package roboyard.ui.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Desktop scroll workaround: forwards mouse-wheel deltas and vertical drags
 * directly to the shared [ScrollState]. `verticalScroll` alone does not
 * deliver wheel/drag scroll reliably inside nested layouts on Desktop.
 */
fun Modifier.desktopScrollable(scrollState: ScrollState): Modifier = this
    .pointerInput(scrollState) {
        awaitEachGesture {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val delta = event.changes.first().scrollDelta.y
                    if (delta != 0f) {
                        scrollState.dispatchRawDelta(delta * 64f)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }
    .pointerInput(scrollState) {
        detectVerticalDragGestures { change, dragAmount ->
            change.consume()
            scrollState.dispatchRawDelta(-dragAmount)
        }
    }

/**
 * Drop-in replacement for `verticalScroll(rememberScrollState())` that also
 * enables Desktop wheel/drag scrolling via [desktopScrollable].
 */
fun Modifier.desktopVerticalScroll(): Modifier = composed {
    val scrollState = rememberScrollState()
    this
        .desktopScrollable(scrollState)
        .verticalScroll(scrollState)
}

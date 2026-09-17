package roboyard.ui.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Registry for the scrollable that is currently pressed by the mouse.
 *
 * On this desktop runtime, Compose does not deliver pointer-move events
 * during a mouse drag to any pointerInput/onPointerEvent handler — the
 * intermediate moves arrive coalesced as a single event at button release.
 * Raw AWT MOUSE_DRAGGED events however arrive continuously, so the desktop
 * main entry installs an AWTEventListener that applies the drag deltas to
 * [activeState] directly. See the AWT listener in desktopMain/Main.kt.
 */
object DesktopDragScroll {
    var activeState: ScrollState? = null
}

/**
 * Desktop scroll workaround: forwards mouse-wheel deltas and pointer drags
 * to the shared [ScrollState]. `verticalScroll` alone does not deliver
 * wheel/drag scroll reliably inside nested layouts on Desktop.
 *
 * Wheel events are applied through a real `scroll {}` session so the scroll
 * mutex marks scrolling in progress. For drags we only track which
 * ScrollState is pressed — the actual per-move scrolling is driven by the
 * AWT-level listener (see [DesktopDragScroll]).
 */
fun Modifier.desktopScrollable(scrollState: ScrollState): Modifier = composed {
    val scope = rememberCoroutineScope()
    this
        .pointerInput(scrollState) {
            awaitEachGesture {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.first().scrollDelta.y
                        if (delta != 0f) {
                            scope.launch { scrollState.scroll { scrollBy(delta * 64f) } }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
        .pointerInput(scrollState) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                DesktopDragScroll.activeState = scrollState
                println("[SCROLL_DRAG] down y=${down.position.y}, registered scroll state")
                // Wait for release — move events are not delivered during
                // drags on this runtime, so the loop only sees the up event.
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.none { it.pressed }) break
                }
                DesktopDragScroll.activeState = null
                println("[SCROLL_DRAG] released, scroll=${scrollState.value}")
            }
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

/**
 * Vertical scrollbar shown next to scrollable content.
 * Real scrollbar on Desktop; touch platforms show none (actuals are no-ops).
 */
@androidx.compose.runtime.Composable
expect fun PlatformVerticalScrollbar(scrollState: ScrollState, modifier: Modifier)

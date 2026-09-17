package roboyard.ui.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformVerticalScrollbar(scrollState: ScrollState, modifier: Modifier) {
    // Touch platforms render no desktop-style scrollbar
}

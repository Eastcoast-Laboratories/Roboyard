package roboyard.ui.compose

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick

/**
 * Shared helpers for Compose desktop UI tests.
 */

/**
 * A fresh profile earns a day-1 login streak, so the streak popup covers the
 * menu on first launch (tap-to-dismiss, like Android). It swallows clicks and
 * hides the Level/Load buttons while visible — tap it away before
 * interacting with the menu.
 */
fun ComposeTestRule.dismissStreakPopupIfVisible() {
    val popup = onAllNodesWithText("Streak", substring = true, ignoreCase = true)
    try {
        waitUntil(timeoutMillis = 10_000) { popup.fetchSemanticsNodes().isNotEmpty() }
    } catch (e: Throwable) {
        return
    }
    popup[0].performClick()
}

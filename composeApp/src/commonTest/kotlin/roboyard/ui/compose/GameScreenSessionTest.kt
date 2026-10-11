package roboyard.ui.compose

import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * UI test for the GameSession-based GameScreen.
 * Starts a random game through the real session and verifies the board
 * renders (i.e. it does not stay stuck on the "Calculating..." state).
 * `waitUntil` throws on timeout, so a passing test means the game UI appeared.
 *
 * The board is located via its fixed English contentDescription
 * ("Game board with WxH cells, ...") — the same anchor the iOS UITest uses —
 * because all counter labels are localized.
 *
 * The Gradle desktopTest task redirects user.home to a per-run sandbox
 * (build/test-profile), so the session starts with a clean profile: no
 * autosave, no history, default preferences.
 *
 * Run: ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.GameScreenSessionTest"
 */
class GameScreenSessionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun awaitGameBoard() {
        try {
            composeRule.waitUntil(timeoutMillis = 60_000) {
                composeRule.onAllNodesWithContentDescription(
                    "Game board with", substring = true
                ).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (t: Throwable) {
            composeRule.onRoot(useUnmergedTree = true).printToLog("GAME_TREE")
            throw t
        }
    }

    @Test
    fun testRandomGameRendersBoard() {
        composeRule.setContent { App() }
        composeRule.dismissStreakPopupIfVisible()

        composeRule.onNodeWithTag("newRandomGameButton").performClick()

        // Wait until map generation + solver finished and the game UI is shown
        awaitGameBoard()
    }

    @Test
    fun testLevelGameRendersBoard() {
        composeRule.setContent { App() }
        composeRule.dismissStreakPopupIfVisible()

        composeRule.onNodeWithTag("levelGameButton").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithTag("levelItem_1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("levelItem_1").performClick()

        awaitGameBoard()
    }
}

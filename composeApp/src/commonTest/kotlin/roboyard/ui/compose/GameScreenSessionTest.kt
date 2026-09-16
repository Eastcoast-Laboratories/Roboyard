package roboyard.ui.compose

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * UI test for the GameSession-based GameScreen.
 * Starts a random game through the real session and verifies the board
 * renders (i.e. it does not stay stuck on the "Calculating..." state).
 * `waitUntil` throws on timeout, so a passing test means the game UI appeared.
 *
 * Run: ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.GameScreenSessionTest"
 */
class GameScreenSessionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testRandomGameRendersBoard() {
        composeRule.setContent { App() }

        composeRule.onNodeWithTag("newRandomGameButton").performClick()

        // Wait until map generation + solver finished and the game UI is shown
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText("Moves:", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testLevelGameRendersBoard() {
        composeRule.setContent { App() }

        composeRule.onNodeWithText("Level Game").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithTag("levelItem_1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("levelItem_1").performClick()

        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText("Moves:", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}

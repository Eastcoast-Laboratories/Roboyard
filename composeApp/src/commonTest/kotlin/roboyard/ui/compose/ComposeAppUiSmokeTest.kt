package roboyard.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * UI smoke test for the ComposeApp.
 * Launches the app, clicks through main screens, and verifies navigation works.
 *
 * Run: ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.ComposeAppUiSmokeTest"
 */
class ComposeAppUiSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testMainMenuVisible() {
        composeRule.setContent { App() }

        // Main menu should show its three primary buttons
        composeRule.onNodeWithText("New Random Game").assertExists()
        composeRule.onNodeWithText("Level Game").assertExists()
        composeRule.onNodeWithText("Load Game").assertExists()
    }

    @Test
    fun testNavigateToLevelSelection() {
        composeRule.setContent { App() }

        composeRule.onNodeWithText("Level Game").performClick()
        composeRule.waitForIdle()

        // Level selection screen should show its title
        composeRule.onNodeWithText("Level Selection").assertExists()
    }

    @Test
    fun testNavigateToCredits() {
        composeRule.setContent { App() }

        // Credits button has text "©"
        composeRule.onNodeWithText("©").performClick()
        composeRule.waitForIdle()

        // Should navigate to Credits screen and show a BACK button
        composeRule.onNodeWithText("BACK").assertExists()
    }

    @Test
    fun testNavigateToCreditsAndBack() {
        composeRule.setContent { App() }

        composeRule.onNodeWithText("©").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("BACK").performClick()
        composeRule.waitForIdle()

        // Should be back at main menu
        composeRule.onNodeWithText("New Random Game").assertExists()
    }

    @Test
    fun testNavigateToSettingsShowsFullSettings() {
        roboyard.logic.core.Preferences.appLanguage = "en"

        composeRule.setContent { App() }

        composeRule.onNodeWithTag("settingsButton").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settingsScreen").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Board Size:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Difficulty Level:", substring = true).assertExists()
        composeRule.onNodeWithText("Num Moves:", substring = true).assertExists()
        composeRule.onNodeWithTag("settingsBackButton").assertIsDisplayed()

        composeRule.onNodeWithTag("settingsBackButton").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("New Random Game").assertExists()
    }
}

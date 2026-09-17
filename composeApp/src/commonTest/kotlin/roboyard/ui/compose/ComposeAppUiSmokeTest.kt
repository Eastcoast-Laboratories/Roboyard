package roboyard.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Before
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

    @Before
    fun setUp() {
        // Assertions check English strings; without a stored preference the
        // provider now falls back to the system locale (de on this machine)
        roboyard.logic.core.Preferences.appLanguage = "en"
    }

    @Test
    fun testMainMenuVisible() {
        composeRule.setContent { App() }

        // Main menu should show its three primary buttons (Android strings: Play / Levels / Load Game)
        composeRule.onNodeWithText("Play").assertExists()
        composeRule.onNodeWithText("Levels").assertExists()
        composeRule.onNodeWithText("Load Game").assertExists()
    }

    @Test
    fun testNavigateToLevelSelection() {
        composeRule.setContent { App() }

        composeRule.onNodeWithText("Levels").performClick()
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

        // Should navigate to Credits screen and show a back button ("← Back")
        composeRule.onNodeWithText("Back", substring = true).assertExists()
    }

    @Test
    fun testNavigateToCreditsAndBack() {
        composeRule.setContent { App() }

        composeRule.onNodeWithText("©").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Back", substring = true).performClick()
        composeRule.waitForIdle()

        // Should be back at main menu
        composeRule.onNodeWithText("Play").assertExists()
    }

    @Test
    fun testNavigateToSettingsShowsFullSettings() {
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
        composeRule.onNodeWithText("Play").assertExists()
    }
}

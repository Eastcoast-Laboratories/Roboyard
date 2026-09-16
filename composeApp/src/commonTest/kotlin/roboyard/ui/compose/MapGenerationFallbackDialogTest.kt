package roboyard.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import roboyard.logic.core.Preferences

class MapGenerationFallbackDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testFallbackDialogShowsMessageAndDismisses() {
        Preferences.appLanguage = "en"

        var dismissed = false
        composeRule.setContent {
            MapGenerationFallbackDialog(
                minMoves = 17,
                maxMoves = 99,
                actualMoves = 5,
                attempts = 1000,
                onDismiss = { dismissed = true }
            )
        }

        composeRule.onNodeWithText("No suitable map found").assertIsDisplayed()
        composeRule.onNodeWithText("at least 17 moves", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("1000 attempts", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("(5 moves)", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitForIdle()
        assertTrue(dismissed)
    }
}

package roboyard.ui.compose

import org.junit.Test
import roboyard.logic.core.Preferences
import roboyard.logic.ui.DesktopStringProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Regression test: strings.json is a pretty-printed JSON object whose first
 * non-whitespace character is '{'. The hand-rolled parser in
 * DesktopStringProvider used to return immediately on that brace (readString
 * rejects non-quote characters), leaving stringsByLocale empty so every
 * getString() fell back to English regardless of the selected language.
 *
 * Run: ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.DesktopStringProviderTest"
 */
class DesktopStringProviderTest {

    @Test
    fun testStringsJsonIsLoadedAndNotEmpty() {
        // app_name is intentionally absent from strings.json; use a key that exists
        assertNotNull(
            DesktopStringProvider.getString("language_settings_label"),
            "strings.json must be loaded — getString must not return null for a known key"
        )
    }

    @Test
    fun testGermanStringsAreReturnedWhenAppLanguageIsDe() {
        val previous = Preferences.appLanguage
        try {
            Preferences.appLanguage = "de"
            assertEquals(
                "Sprache:",
                DesktopStringProvider.getString("language_settings_label"),
                "German string expected when appLanguage is 'de'"
            )
        } finally {
            Preferences.appLanguage = previous
        }
    }
}

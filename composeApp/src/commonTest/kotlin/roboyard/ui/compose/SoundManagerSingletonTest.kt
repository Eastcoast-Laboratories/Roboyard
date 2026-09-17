package roboyard.ui.compose

import org.junit.Test
import roboyard.logic.audio.getSoundManager
import kotlin.test.assertSame

/**
 * Regression test: `getSoundManager()` must return a shared instance.
 * A per-call instance cannot pause/resume or change the volume of the
 * background clip that is actually playing, and settings sliders could
 * never reach the running sound (Android parity: SoundManager.getInstance
 * is a singleton).
 *
 * Run: ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.SoundManagerSingletonTest"
 */
class SoundManagerSingletonTest {

    @Test
    fun testGetSoundManagerReturnsSingleton() {
        assertSame(getSoundManager(), getSoundManager(),
            "getSoundManager() must return the same shared instance")
    }
}

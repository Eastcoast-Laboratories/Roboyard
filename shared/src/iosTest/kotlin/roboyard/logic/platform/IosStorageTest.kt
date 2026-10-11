package roboyard.logic.platform

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import roboyard.logic.core.Preferences

/**
 * IosStorage must return the caller's defaultValue for keys that were never
 * written, like Android SharedPreferences and java.util.prefs do.
 * NSUserDefaults returns 0/false for missing keys, which made a fresh install
 * load robotCount = 0 and complete every random game after the first move.
 *
 * Run on the Mac: ./gradlew :shared:iosSimulatorArm64Test --tests "roboyard.logic.platform.IosStorageTest"
 */
class IosStorageTest {
    private val storage = IosStorage()
    private val keys = listOf("ios_test_int", "ios_test_long", "ios_test_bool")

    @AfterTest
    fun cleanup() = keys.forEach { storage.remove(it) }

    @Test
    fun missingKeys_returnDefaultValue() {
        cleanup()
        assertEquals(3, storage.getInt("ios_test_int", 3))
        assertEquals(42L, storage.getLong("ios_test_long", 42L))
        assertEquals(true, storage.getBoolean("ios_test_bool", true))
    }

    @Test
    fun storedValues_overrideDefault_includingZeroAndFalse() {
        storage.putInt("ios_test_int", 0)
        storage.putLong("ios_test_long", 0L)
        storage.putBoolean("ios_test_bool", false)
        assertEquals(0, storage.getInt("ios_test_int", 3))
        assertEquals(0L, storage.getLong("ios_test_long", 42L))
        assertEquals(false, storage.getBoolean("ios_test_bool", true))
    }

    @Test
    fun freshInstall_robotCountDefaultsToOne() {
        storage.remove("robot_count")
        assertEquals(
            Preferences.DEFAULT_ROBOT_COUNT,
            storage.getInt("robot_count", Preferences.DEFAULT_ROBOT_COUNT)
        )
    }
}

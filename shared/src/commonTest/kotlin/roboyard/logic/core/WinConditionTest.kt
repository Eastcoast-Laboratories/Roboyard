package roboyard.logic.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * GameState.areAllRobotsAtTargets must never report a win while no robot
 * stands on its target. Regression for iOS, where Preferences was never
 * initialized: Preferences.robotCount kept its field value 0, the random game
 * required 0 robots at targets and every first move completed the game.
 *
 * Run: ./gradlew :shared:desktopTest --tests "roboyard.logic.core.WinConditionTest"
 */
class WinConditionTest {
    private val savedRobotCount = Preferences.robotCount

    @AfterTest
    fun restore() {
        Preferences.robotCount = savedRobotCount
    }

    @Test
    fun randomGame_withUninitializedRobotCount_isNotCompleteWhileRobotsAreOffTarget() {
        Preferences.robotCount = 0
        val state = GameState.createRandom()
        assertTrue(state.targets.isNotEmpty(), "random game must have a target")
        assertFalse(state.robots.any { state.isRobotAtTarget(it) }, "fresh random game starts with no robot on its target")
        assertFalse(state.areAllRobotsAtTargets(), "robotCount 0 must not turn a fresh board into a win")
    }

    @Test
    fun levelState_completesOnlyWhenRobotReachesTarget() {
        val state = GameState(8, 8)
        state.addRobot(1, 1, 0)
        state.addTarget(6, 1, 0)
        state.setRobotCount(1)
        assertFalse(state.areAllRobotsAtTargets())
        state.robots.first()!!.x = 6
        assertTrue(state.areAllRobotsAtTargets())
    }
}

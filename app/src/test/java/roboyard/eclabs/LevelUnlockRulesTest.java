package roboyard.eclabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import roboyard.logic.managers.LevelCompletionManager;

public class LevelUnlockRulesTest {

    @Test
    public void firstLevelIsAlwaysUnlocked() {
        assertTrue(LevelCompletionManager.isLevelUnlocked(1, 0));
        assertEquals(0, LevelCompletionManager.getStarsNeededToUnlockLevel(1, 0));
    }

    @Test
    public void nextLevelRequiresOneStarPerCompletedLevel() {
        assertTrue(LevelCompletionManager.isLevelUnlocked(88, 87));
        assertFalse(LevelCompletionManager.isLevelUnlocked(89, 87));
        assertEquals(1, LevelCompletionManager.getStarsNeededToUnlockLevel(89, 87));
    }

    @Test
    public void higherLockedLevelReportsRemainingStars() {
        assertFalse(LevelCompletionManager.isLevelUnlocked(92, 87));
        assertEquals(4, LevelCompletionManager.getStarsNeededToUnlockLevel(92, 87));
    }

    @Test
    public void customLevelsDoNotNeedStars() {
        assertTrue(LevelCompletionManager.isLevelUnlocked(141, 0));
        assertEquals(0, LevelCompletionManager.getStarsNeededToUnlockLevel(141, 0));
    }
}

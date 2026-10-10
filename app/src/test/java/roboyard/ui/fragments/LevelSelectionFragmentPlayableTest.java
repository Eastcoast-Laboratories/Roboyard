package roboyard.ui.fragments;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LevelSelectionFragmentPlayableTest {
    private static final int COMPLETED_LEVEL_WITH_HIGH_REQUIREMENT = 92;
    private static final int STARS_BELOW_REQUIREMENT = 87;
    private static final int FIRST_CUSTOM_LEVEL_ID = 141;

    @Test
    public void completedLevelRemainsPlayableEvenWithoutRequiredStars() {
        assertTrue(LevelSelectionFragment.isLevelPlayable(
                COMPLETED_LEVEL_WITH_HIGH_REQUIREMENT, STARS_BELOW_REQUIREMENT, true));
    }

    @Test
    public void incompleteStandardLevelRequiresEnoughStars() {
        assertFalse(LevelSelectionFragment.isLevelPlayable(
                COMPLETED_LEVEL_WITH_HIGH_REQUIREMENT, STARS_BELOW_REQUIREMENT, false));
    }

    @Test
    public void customLevelIsAlwaysPlayable() {
        assertTrue(LevelSelectionFragment.isLevelPlayable(FIRST_CUSTOM_LEVEL_ID, 0, false));
    }
}

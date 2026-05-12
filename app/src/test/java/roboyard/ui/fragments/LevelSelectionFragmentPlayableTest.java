package roboyard.ui.fragments;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LevelSelectionFragmentPlayableTest {

    @Test
    public void completedLevelRemainsPlayableEvenWithoutRequiredStars() {
        assertTrue(LevelSelectionFragment.isLevelPlayable(92, 87, true));
    }

    @Test
    public void incompleteStandardLevelRequiresEnoughStars() {
        assertFalse(LevelSelectionFragment.isLevelPlayable(92, 87, false));
    }

    @Test
    public void customLevelIsAlwaysPlayable() {
        assertTrue(LevelSelectionFragment.isLevelPlayable(141, 0, false));
    }
}

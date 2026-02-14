package roboyard.eclabs.achievements;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import roboyard.logic.core.Constants;
import roboyard.logic.core.Preferences;

/**
 * Tests for AchievementPopup and unlock listener integration.
 * Tests that achievements trigger the unlock listener correctly.
 * 
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.ui.achievements.AchievementPopupTest
 */
@RunWith(AndroidJUnit4.class)
public class AchievementPopupTest {

    private static final String PREFS_NAME = "roboyard_achievements";

    private Context context;
    private AchievementManager achievementManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Clear achievements before each test
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        // Reset game session flags so achievements can be unlocked
        achievementManager.onNewGameStarted();
    }

    @After
    public void tearDown() {
        achievementManager.setUnlockListener(null);
        achievementManager.resetAll();
    }

    /**
     * Test that the unlock listener is called when an achievement is unlocked.
     */
    @Test
    public void testUnlockListenerIsCalled() {
        final boolean[] listenerCalled = {false};
        final String[] unlockedId = {null};
        
        achievementManager.setUnlockListener(achievement -> {
            listenerCalled[0] = true;
            unlockedId[0] = achievement.getId();
        });
        
        achievementManager.unlock("first_game");
        
        assertTrue("Unlock listener should be called", listenerCalled[0]);
        assertEquals("Unlocked achievement ID should match", "first_game", unlockedId[0]);
    }

    /**
     * Test that the unlock listener is called multiple times for multiple achievements.
     */
    @Test
    public void testUnlockListenerCalledMultipleTimes() {
        final List<String> unlockedIds = new ArrayList<>();
        
        achievementManager.setUnlockListener(achievement -> {
            unlockedIds.add(achievement.getId());
        });
        
        achievementManager.unlock("first_game");
        achievementManager.unlock("level_1_complete");
        achievementManager.unlock("speedrun_under_30s");
        
        assertEquals("Should have 3 unlocked achievements", 3, unlockedIds.size());
        assertTrue("first_game should be in list", unlockedIds.contains("first_game"));
        assertTrue("level_1_complete should be in list", unlockedIds.contains("level_1_complete"));
        assertTrue("speedrun_under_30s should be in list", unlockedIds.contains("speedrun_under_30s"));
    }

    /**
     * Test that completing level 1 triggers multiple achievements via listener.
     */
    @Test
    public void testLevelCompletionTriggersMultipleAchievements() {
        final List<String> unlockedIds = new ArrayList<>();
        
        achievementManager.setUnlockListener(achievement -> {
            unlockedIds.add(achievement.getId());
        });
        
        // Complete level 1 with optimal moves and 3 stars
        achievementManager.onNewGameStarted();
        achievementManager.onLevelCompleted(1, 5, 5, 0, 3, 10000);
        
        // Should trigger: first_game, level_1_complete, 3_star_hard_level
        assertTrue("first_game should be unlocked", unlockedIds.contains("first_game"));
        assertTrue("level_1_complete should be unlocked", unlockedIds.contains("level_1_complete"));
        assertTrue("3_star_hard_level should be unlocked", unlockedIds.contains("3_star_hard_level"));
        
        // Verify at least 3 achievements were unlocked
        assertTrue("At least 3 achievements should be unlocked", unlockedIds.size() >= 3);
    }

    /**
     * Test that the listener is NOT called for already unlocked achievements.
     */
    @Test
    public void testListenerNotCalledForAlreadyUnlocked() {
        final int[] callCount = {0};
        
        achievementManager.setUnlockListener(achievement -> {
            callCount[0]++;
        });
        
        // First unlock
        achievementManager.unlock("first_game");
        assertEquals("Listener should be called once", 1, callCount[0]);
        
        // Try to unlock again
        achievementManager.unlock("first_game");
        assertEquals("Listener should still be called only once", 1, callCount[0]);
    }

    /**
     * Test that random game completion triggers achievements via listener.
     */
    @Test
    public void testRandomGameCompletionTriggersAchievements() {
        final List<String> unlockedIds = new ArrayList<>();
        
        achievementManager.setUnlockListener(achievement -> {
            unlockedIds.add(achievement.getId());
        });
        
        // Complete a random game with 2 targets
        achievementManager.onNewGameStarted();
        boolean isImpossibleMode = Preferences.difficulty == Constants.DIFFICULTY_IMPOSSIBLE;
        achievementManager.onRandomGameCompleted(10, 10, 0, 15000, isImpossibleMode, 4, 2, 2);
        
        // Should trigger: first_game, game_2_targets, speedrun_random_under_20s, perfect_random_games progression
        assertTrue("first_game should be unlocked", unlockedIds.contains("first_game"));
        assertTrue("game_2_targets should be unlocked", unlockedIds.contains("game_2_targets"));
        assertTrue("speedrun_random_under_20s should be unlocked (15s < 20s)", 
                unlockedIds.contains("speedrun_random_under_20s"));
    }

    /**
     * Test that achievement data is correctly passed to the listener.
     */
    @Test
    public void testAchievementDataPassedToListener() {
        final Achievement[] receivedAchievement = {null};
        
        achievementManager.setUnlockListener(achievement -> {
            if (achievement.getId().equals("first_game")) {
                receivedAchievement[0] = achievement;
            }
        });
        
        achievementManager.unlock("first_game");
        
        assertNotNull("Achievement should be passed to listener", receivedAchievement[0]);
        assertEquals("Achievement ID should match", "first_game", receivedAchievement[0].getId());
        assertTrue("Achievement should be marked as unlocked", receivedAchievement[0].isUnlocked());
        assertTrue("Achievement should have a timestamp", receivedAchievement[0].getUnlockedTimestamp() > 0);
    }

    /**
     * Test that setting listener to null doesn't cause crashes.
     */
    @Test
    public void testNullListenerDoesNotCrash() {
        achievementManager.setUnlockListener(null);
        
        // This should not throw any exception
        achievementManager.unlock("first_game");
        
        assertTrue("Achievement should still be unlocked", achievementManager.isUnlocked("first_game"));
    }
}

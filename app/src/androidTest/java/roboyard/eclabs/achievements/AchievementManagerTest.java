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

import java.util.List;

/**
 * Instrumented tests for AchievementManager.
 * Tests achievement unlocking, storage, and event handlers.
 * 
 * Run with: ./gradlew connectedAndroidTest --tests "roboyard.eclabs.achievements.AchievementManagerTest"
 */
@RunWith(AndroidJUnit4.class)
public class AchievementManagerTest {

    private Context context;
    private AchievementManager achievementManager;
    private static final String PREFS_NAME = "roboyard_achievements";

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Clear achievements before each test
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        // Get fresh instance
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
    }

    @After
    public void tearDown() {
        // Clean up after tests
        achievementManager.resetAll();
    }

    // ==================== BASIC FUNCTIONALITY TESTS ====================

    /**
     * Test that all achievements are loaded correctly
     */
    @Test
    public void testAllAchievementsLoaded() {
        List<Achievement> all = achievementManager.getAllAchievements();
        assertNotNull("Achievement list should not be null", all);
        assertEquals("Should have 53 achievements", 53, all.size());
    }

    /**
     * Test that initially no achievements are unlocked
     */
    @Test
    public void testInitiallyNoAchievementsUnlocked() {
        assertEquals("Initially no achievements should be unlocked", 0, achievementManager.getUnlockedCount());
    }

    /**
     * Test basic achievement unlock
     */
    @Test
    public void testUnlockAchievement() {
        boolean result = achievementManager.unlock("first_game");
        assertTrue("First unlock should return true", result);
        assertTrue("Achievement should be unlocked", achievementManager.isUnlocked("first_game"));
        assertEquals("Unlocked count should be 1", 1, achievementManager.getUnlockedCount());
    }

    /**
     * Test that unlocking same achievement twice returns false
     */
    @Test
    public void testDoubleUnlockReturnsFalse() {
        achievementManager.unlock("first_game");
        boolean result = achievementManager.unlock("first_game");
        assertFalse("Second unlock should return false", result);
        assertEquals("Unlocked count should still be 1", 1, achievementManager.getUnlockedCount());
    }

    /**
     * Test unlocking unknown achievement returns false
     */
    @Test
    public void testUnlockUnknownAchievement() {
        boolean result = achievementManager.unlock("unknown_achievement_xyz");
        assertFalse("Unknown achievement unlock should return false", result);
    }

    // ==================== PROGRESSION ACHIEVEMENTS TESTS ====================

    /**
     * Test level completion achievements
     */
    @Test
    public void testLevelCompletionAchievements() {
        // Complete first level
        achievementManager.onLevelCompleted(1, 5, 5, 0, 3, 10000);
        assertTrue("level_1_complete should be unlocked", achievementManager.isUnlocked("level_1_complete"));
        
        // Complete 10 levels
        for (int i = 2; i <= 10; i++) {
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        assertTrue("level_10_complete should be unlocked", achievementManager.isUnlocked("level_10_complete"));
    }

    /**
     * Test perfect solution achievements
     */
    @Test
    public void testPerfectSolutionAchievements() {
        // Complete with optimal moves
        achievementManager.onLevelCompleted(1, 5, 5, 0, 3, 10000);
        assertTrue("perfect_solution_1 should be unlocked", achievementManager.isUnlocked("perfect_solution_1"));
        
        // Complete 10 with optimal moves
        for (int i = 2; i <= 10; i++) {
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        assertTrue("perfect_solutions_10 should be unlocked", achievementManager.isUnlocked("perfect_solutions_10"));
    }

    /**
     * Test speedrun achievements
     */
    @Test
    public void testSpeedrunAchievements() {
        // Complete in under 30 seconds
        achievementManager.onLevelCompleted(1, 5, 5, 0, 3, 25000);
        assertTrue("speedrun_under_30s should be unlocked", achievementManager.isUnlocked("speedrun_under_30s"));
        
        // Complete in under 10 seconds
        achievementManager.onLevelCompleted(2, 5, 5, 0, 3, 8000);
        assertTrue("speedrun_under_10s should be unlocked", achievementManager.isUnlocked("speedrun_under_10s"));
    }

    /**
     * Test 3-star achievements
     */
    @Test
    public void testThreeStarAchievements() {
        // Get 3 stars on a level
        achievementManager.onLevelCompleted(1, 5, 5, 0, 3, 10000);
        assertTrue("3_star_level should be unlocked", achievementManager.isUnlocked("3_star_level"));
        
        // Get 3 stars on 10 levels
        for (int i = 2; i <= 10; i++) {
            achievementManager.onLevelCompleted(i, 5, 5, 0, 3, 10000);
        }
        assertTrue("3_star_10_levels should be unlocked", achievementManager.isUnlocked("3_star_10_levels"));
    }

    /**
     * Test no hints achievements
     */
    @Test
    public void testNoHintsAchievements() {
        // Complete 10 levels without hints
        for (int i = 1; i <= 10; i++) {
            achievementManager.onLevelCompleted(i, 6, 5, 0, 2, 20000);
        }
        assertTrue("no_hints_10 should be unlocked", achievementManager.isUnlocked("no_hints_10"));
    }

    // ==================== RANDOM GAME ACHIEVEMENTS TESTS ====================

    /**
     * Test first game achievement
     */
    @Test
    public void testFirstGameAchievement() {
        achievementManager.onRandomGameCompleted(5, 5, 0, 15000, false, 4, 1, 1);
        assertTrue("first_game should be unlocked", achievementManager.isUnlocked("first_game"));
    }

    /**
     * Test impossible mode achievements
     */
    @Test
    public void testImpossibleModeAchievements() {
        // Complete 1 game in impossible mode
        achievementManager.onRandomGameCompleted(10, 10, 0, 30000, true, 4, 1, 1);
        assertTrue("impossible_mode_1 should be unlocked", achievementManager.isUnlocked("impossible_mode_1"));
        
        // Complete 5 games in impossible mode
        for (int i = 0; i < 4; i++) {
            achievementManager.onRandomGameCompleted(10, 10, 0, 30000, true, 4, 1, 1);
        }
        assertTrue("impossible_mode_5 should be unlocked", achievementManager.isUnlocked("impossible_mode_5"));
    }

    /**
     * Test impossible mode streak achievements
     */
    @Test
    public void testImpossibleModeStreakAchievements() {
        // Complete 5 games in a row with optimal moves in impossible mode
        for (int i = 0; i < 5; i++) {
            achievementManager.onRandomGameCompleted(10, 10, 0, 30000, true, 4, 1, 1);
        }
        assertTrue("impossible_mode_streak_5 should be unlocked", 
                achievementManager.isUnlocked("impossible_mode_streak_5"));
    }

    /**
     * Test solution length achievements (20-30+ moves)
     */
    @Test
    public void testSolutionLengthAchievements() {
        // Test 20 moves
        achievementManager.onRandomGameCompleted(20, 20, 0, 60000, false, 4, 1, 1);
        assertTrue("solution_20_moves should be unlocked", achievementManager.isUnlocked("solution_20_moves"));
        
        // Test 25 moves
        achievementManager.onRandomGameCompleted(25, 25, 0, 90000, false, 4, 1, 1);
        assertTrue("solution_25_moves should be unlocked", achievementManager.isUnlocked("solution_25_moves"));
        
        // Test 30+ moves
        achievementManager.onRandomGameCompleted(35, 35, 0, 120000, false, 4, 1, 1);
        assertTrue("solution_30_plus_moves should be unlocked", achievementManager.isUnlocked("solution_30_plus_moves"));
    }

    /**
     * Test multiple targets achievements
     */
    @Test
    public void testMultipleTargetsAchievements() {
        // 2 targets
        achievementManager.onRandomGameCompleted(10, 10, 0, 30000, false, 4, 2, 2);
        assertTrue("game_2_targets should be unlocked", achievementManager.isUnlocked("game_2_targets"));
        
        // 3 targets
        achievementManager.onRandomGameCompleted(15, 15, 0, 45000, false, 4, 3, 3);
        assertTrue("game_3_targets should be unlocked", achievementManager.isUnlocked("game_3_targets"));
        
        // 4 targets
        achievementManager.onRandomGameCompleted(20, 20, 0, 60000, false, 4, 4, 4);
        assertTrue("game_4_targets should be unlocked", achievementManager.isUnlocked("game_4_targets"));
    }

    /**
     * Test X of Y targets achievements
     */
    @Test
    public void testXofYTargetsAchievements() {
        // 2 of 3 targets
        achievementManager.onRandomGameCompleted(10, 10, 0, 30000, false, 4, 3, 2);
        assertTrue("game_2_of_3_targets should be unlocked", achievementManager.isUnlocked("game_2_of_3_targets"));
        
        // 2 of 4 targets
        achievementManager.onRandomGameCompleted(10, 10, 0, 30000, false, 4, 4, 2);
        assertTrue("game_2_of_4_targets should be unlocked", achievementManager.isUnlocked("game_2_of_4_targets"));
        
        // 3 of 4 targets
        achievementManager.onRandomGameCompleted(15, 15, 0, 45000, false, 4, 4, 3);
        assertTrue("game_3_of_4_targets should be unlocked", achievementManager.isUnlocked("game_3_of_4_targets"));
    }

    /**
     * Test 5 robots achievement
     */
    @Test
    public void testFiveRobotsAchievement() {
        achievementManager.onRandomGameCompleted(10, 10, 0, 30000, false, 5, 1, 1);
        assertTrue("game_5_robots should be unlocked", achievementManager.isUnlocked("game_5_robots"));
    }

    /**
     * Test perfect random games achievements
     */
    @Test
    public void testPerfectRandomGamesAchievements() {
        // Complete 5 random games with optimal moves
        for (int i = 0; i < 5; i++) {
            achievementManager.onRandomGameCompleted(10, 10, 0, 30000, false, 4, 1, 1);
        }
        assertTrue("perfect_random_games_5 should be unlocked", 
                achievementManager.isUnlocked("perfect_random_games_5"));
    }

    /**
     * Test no hints random games achievements
     */
    @Test
    public void testNoHintsRandomGamesAchievements() {
        // Complete 10 random games without hints
        for (int i = 0; i < 10; i++) {
            achievementManager.onRandomGameCompleted(12, 10, 0, 30000, false, 4, 1, 1);
        }
        assertTrue("no_hints_random_10 should be unlocked", 
                achievementManager.isUnlocked("no_hints_random_10"));
    }

    /**
     * Test speed achievements for random games
     */
    @Test
    public void testRandomGameSpeedAchievements() {
        // Under 20 seconds
        achievementManager.onRandomGameCompleted(5, 5, 0, 15000, false, 4, 1, 1);
        assertTrue("speedrun_random_under_20s should be unlocked", 
                achievementManager.isUnlocked("speedrun_random_under_20s"));
        
        // Under 10 seconds
        achievementManager.onRandomGameCompleted(3, 3, 0, 8000, false, 4, 1, 1);
        assertTrue("speedrun_random_under_10s should be unlocked", 
                achievementManager.isUnlocked("speedrun_random_under_10s"));
    }

    /**
     * Test speed streak achievement (5 games under 30s each)
     */
    @Test
    public void testSpeedStreakAchievement() {
        for (int i = 0; i < 5; i++) {
            achievementManager.onRandomGameCompleted(8, 8, 0, 25000, false, 4, 1, 1);
        }
        assertTrue("speedrun_random_5_games_under_30s should be unlocked", 
                achievementManager.isUnlocked("speedrun_random_5_games_under_30s"));
    }

    // ==================== SPECIAL ACHIEVEMENTS TESTS ====================

    /**
     * Test daily login streak achievements
     */
    @Test
    public void testDailyLoginStreakAchievements() {
        achievementManager.onDailyLogin(7);
        assertTrue("daily_login_7 should be unlocked", achievementManager.isUnlocked("daily_login_7"));
        
        achievementManager.onDailyLogin(30);
        assertTrue("daily_login_30 should be unlocked", achievementManager.isUnlocked("daily_login_30"));
    }

    /**
     * Test comeback player achievement
     */
    @Test
    public void testComebackPlayerAchievement() {
        achievementManager.onComebackPlayer(30);
        assertTrue("comeback_player should be unlocked", achievementManager.isUnlocked("comeback_player"));
    }

    /**
     * Test custom level achievements
     */
    @Test
    public void testCustomLevelAchievements() {
        achievementManager.onCustomLevelCreated();
        assertTrue("create_custom_level should be unlocked", achievementManager.isUnlocked("create_custom_level"));
        
        achievementManager.onCustomLevelSolved();
        assertTrue("solve_custom_level should be unlocked", achievementManager.isUnlocked("solve_custom_level"));
        
        achievementManager.onCustomLevelShared();
        assertTrue("share_custom_level should be unlocked", achievementManager.isUnlocked("share_custom_level"));
    }

    /**
     * Test square coverage achievements
     */
    @Test
    public void testSquareCoverageAchievements() {
        achievementManager.onAllSquaresTraversed(true, false);
        assertTrue("traverse_all_squares_1_robot should be unlocked", 
                achievementManager.isUnlocked("traverse_all_squares_1_robot"));
        
        achievementManager.onAllSquaresTraversed(false, true);
        assertTrue("traverse_all_squares_all_robots should be unlocked", 
                achievementManager.isUnlocked("traverse_all_squares_all_robots"));
    }

    // ==================== PERSISTENCE TESTS ====================

    /**
     * Test that achievements persist after reset and reload
     */
    @Test
    public void testAchievementPersistence() {
        // Unlock an achievement
        achievementManager.unlock("first_game");
        assertTrue("Achievement should be unlocked", achievementManager.isUnlocked("first_game"));
        
        // The achievement should persist (we can't easily test reload without recreating the singleton)
        // But we can verify it's stored in SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        assertTrue("Achievement should be stored in prefs", 
                prefs.getBoolean("unlocked_first_game", false));
    }

    /**
     * Test reset all achievements
     */
    @Test
    public void testResetAllAchievements() {
        // Unlock some achievements
        achievementManager.unlock("first_game");
        achievementManager.unlock("level_1_complete");
        assertEquals("Should have 2 unlocked", 2, achievementManager.getUnlockedCount());
        
        // Reset all
        achievementManager.resetAll();
        assertEquals("Should have 0 unlocked after reset", 0, achievementManager.getUnlockedCount());
    }

    // ==================== EDGE CASES ====================

    /**
     * Test that non-optimal moves don't trigger perfect solution achievements
     */
    @Test
    public void testNonOptimalMovesNoAchievement() {
        achievementManager.resetAll();
        // Complete with more moves than optimal
        achievementManager.onLevelCompleted(1, 10, 5, 0, 2, 30000);
        assertFalse("perfect_solution_1 should NOT be unlocked with non-optimal moves", 
                achievementManager.isUnlocked("perfect_solution_1"));
    }

    /**
     * Test that hints used prevents no-hints achievements
     */
    @Test
    public void testHintsUsedNoAchievement() {
        achievementManager.resetAll();
        // Complete 10 levels with hints
        for (int i = 1; i <= 10; i++) {
            achievementManager.onLevelCompleted(i, 5, 5, 1, 2, 20000);
        }
        assertFalse("no_hints_10 should NOT be unlocked when hints were used", 
                achievementManager.isUnlocked("no_hints_10"));
    }

    /**
     * Test impossible mode streak breaks on non-optimal
     */
    @Test
    public void testImpossibleModeStreakBreaks() {
        // Complete 3 games with optimal moves
        for (int i = 0; i < 3; i++) {
            achievementManager.onRandomGameCompleted(10, 10, 0, 30000, true, 4, 1, 1);
        }
        
        // Break streak with non-optimal
        achievementManager.onRandomGameCompleted(12, 10, 0, 30000, true, 4, 1, 1);
        
        // Complete 1 more with optimal
        achievementManager.onRandomGameCompleted(10, 10, 0, 30000, true, 4, 1, 1);
        
        // Streak should be broken, so 5-streak achievement should NOT be unlocked
        assertFalse("impossible_mode_streak_5 should NOT be unlocked after streak break", 
                achievementManager.isUnlocked("impossible_mode_streak_5"));
    }
}

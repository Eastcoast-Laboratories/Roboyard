package roboyard.eclabs.achievements;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.data.LevelSolutionData;

/**
 * Test for the 3_star_hard_level achievement which requires 3 stars on a level with 5+ optimal moves.
 * 
 * This test:
 * 1. Plays through levels using LevelSolutionData to get optimal move counts
 * 2. Counts levels with 5+ optimal moves
 * 3. Simulates completing levels with 3 stars
 * 4. Verifies the 3_star_hard_level achievement is unlocked correctly
 * 
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.achievements.ThreeStarAchievementTest
 */
@RunWith(AndroidJUnit4.class)
public class ThreeStarAchievementTest {

    private static final String TAG = "THREE_STAR_TEST";
    private static final String PREFS_NAME = "roboyard_achievements";
    
    private Context context;
    private AchievementManager achievementManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Clear achievements before each test
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        // Get fresh instance
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        // Reset game session flags so achievements can be unlocked
        achievementManager.onNewGameStarted();
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
    }

    /**
     * Test that plays through the first 100 levels and verifies the 3_star_hard_level achievement
     * is correctly unlocked when completing a level with 5+ optimal moves with 3 stars.
     */
    @Test
    public void testThreeStarAchievementWithRealLevelData() {
        Log.i(TAG, "========== STARTING 3-STAR ACHIEVEMENT TEST ==========");
        
        int levelsWithFivePlusMoves = 0;
        int firstLevelWithFivePlusMoves = -1;
        boolean achievementUnlocked = false;
        
        // Play through first 100 levels
        for (int levelId = 1; levelId <= 100; levelId++) {
            int optimalMoves = LevelSolutionData.getOptimalMoves(levelId);
            
            if (optimalMoves == -1) {
                Log.w(TAG, "Level " + levelId + ": No solution data available");
                continue;
            }
            
            Log.d(TAG, "Level " + levelId + ": " + optimalMoves + " optimal moves");
            
            // Check if this level has 5+ optimal moves
            if (optimalMoves >= 5) {
                levelsWithFivePlusMoves++;
                
                if (firstLevelWithFivePlusMoves == -1) {
                    firstLevelWithFivePlusMoves = levelId;
                }
                
                // Simulate completing this level with 3 stars (optimal moves)
                achievementManager.onNewGameStarted();
            achievementManager.onLevelCompleted(levelId, optimalMoves, optimalMoves, 0, 3, 10000);
                
                // Check if achievement was unlocked
                if (!achievementUnlocked && achievementManager.isUnlocked("3_star_hard_level")) {
                    achievementUnlocked = true;
                    Log.i(TAG, "3_star_hard_level achievement UNLOCKED at level " + levelId + 
                        " (optimal moves: " + optimalMoves + ")");
                }
            } else {
                // Level with less than 5 moves - complete but should NOT unlock 3_star_hard_level
                achievementManager.onNewGameStarted();
            achievementManager.onLevelCompleted(levelId, optimalMoves, optimalMoves, 0, 3, 10000);
            }
        }
        
        Log.i(TAG, "========== TEST SUMMARY ==========");
        Log.i(TAG, "Levels with 5+ moves in first 100: " + levelsWithFivePlusMoves);
        Log.i(TAG, "First level with 5+ moves: " + firstLevelWithFivePlusMoves);
        Log.i(TAG, "3_star_hard_level achievement unlocked: " + achievementUnlocked);
        
        // Verify the achievement was unlocked
        assertTrue("Should have found at least one level with 5+ moves", levelsWithFivePlusMoves > 0);
        assertTrue("3_star_hard_level achievement should be unlocked", achievementUnlocked);
        assertTrue("3_star_hard_level should still be unlocked", achievementManager.isUnlocked("3_star_hard_level"));
        
        Log.i(TAG, "========== TEST PASSED ==========");
    }

    /**
     * Test that 3_star_hard_level is NOT unlocked for levels with less than 5 optimal moves
     */
    @Test
    public void testThreeStarAchievementNotUnlockedForShortLevels() {
        Log.i(TAG, "========== TESTING SHORT LEVELS ==========");
        
        // Find levels with less than 5 optimal moves and complete them with 3 stars
        int shortLevelsCompleted = 0;
        
        for (int levelId = 1; levelId <= 100; levelId++) {
            int optimalMoves = LevelSolutionData.getOptimalMoves(levelId);
            
            if (optimalMoves == -1) continue;
            
            if (optimalMoves < 5) {
                // Complete this short level with 3 stars
                achievementManager.onNewGameStarted();
            achievementManager.onLevelCompleted(levelId, optimalMoves, optimalMoves, 0, 3, 10000);
                shortLevelsCompleted++;
                
                // Verify achievement is NOT unlocked
                assertFalse("3_star_hard_level should NOT be unlocked for level " + levelId + 
                    " with only " + optimalMoves + " moves",
                    achievementManager.isUnlocked("3_star_hard_level"));
            }
        }
        
        Log.i(TAG, "Completed " + shortLevelsCompleted + " short levels without unlocking 3_star_hard_level");
        assertTrue("Should have found at least one short level", shortLevelsCompleted > 0);
        assertFalse("3_star_hard_level should still be locked", achievementManager.isUnlocked("3_star_hard_level"));
        
        Log.i(TAG, "========== TEST PASSED ==========");
    }

    /**
     * Test that 3_star_10_levels counts ALL levels regardless of move count
     */
    @Test
    public void testThreeStar10LevelsCountsAllLevels() {
        Log.i(TAG, "========== TESTING 3_star_10_levels ==========");
        
        // Complete 10 levels with 3 stars (mix of short and long levels)
        int levelsCompleted = 0;
        
        for (int levelId = 1; levelId <= 100 && levelsCompleted < 10; levelId++) {
            int optimalMoves = LevelSolutionData.getOptimalMoves(levelId);
            
            if (optimalMoves == -1) continue;
            
            // Complete with 3 stars
            achievementManager.onNewGameStarted();
            achievementManager.onLevelCompleted(levelId, optimalMoves, optimalMoves, 0, 3, 10000);
            levelsCompleted++;
            
            Log.d(TAG, "Completed level " + levelId + " (" + optimalMoves + " moves), total: " + levelsCompleted);
        }
        
        Log.i(TAG, "Completed " + levelsCompleted + " levels with 3 stars");
        
        // Verify 3_star_10_levels is unlocked
        assertTrue("3_star_10_levels should be unlocked after 10 levels with 3 stars",
            achievementManager.isUnlocked("3_star_10_levels"));
        
        Log.i(TAG, "========== TEST PASSED ==========");
    }

    /**
     * Test that 3_star_10_hard_levels counts only levels with 5+ optimal moves
     */
    @Test
    public void testThreeStar10HardLevelsCountsOnlyHardLevels() {
        Log.i(TAG, "========== TESTING 3_star_10_hard_levels ==========");
        
        // Complete hard levels (5+ moves) with 3 stars until we reach 10
        int hardLevelsCompleted = 0;
        int levelAtWhichUnlocked = -1;
        
        for (int levelId = 1; levelId <= 140; levelId++) {
            int optimalMoves = LevelSolutionData.getOptimalMoves(levelId);
            
            if (optimalMoves == -1) continue;
            
            // Only complete levels with 5+ optimal moves
            if (optimalMoves >= 5) {
                // Complete with 3 stars
                achievementManager.onNewGameStarted();
            achievementManager.onLevelCompleted(levelId, optimalMoves, optimalMoves, 0, 3, 10000);
                hardLevelsCompleted++;
                
                Log.d(TAG, "Completed hard level " + levelId + " (" + optimalMoves + " moves), hard total: " + hardLevelsCompleted);
                
                // Check if achievement was unlocked at exactly 10 hard levels
                if (hardLevelsCompleted == 10 && levelAtWhichUnlocked == -1) {
                    if (achievementManager.isUnlocked("3_star_10_hard_levels")) {
                        levelAtWhichUnlocked = levelId;
                        Log.i(TAG, "3_star_10_hard_levels UNLOCKED at level " + levelId);
                    }
                }
                
                // Stop after 10 hard levels
                if (hardLevelsCompleted >= 10) break;
            }
        }
        
        Log.i(TAG, "Completed " + hardLevelsCompleted + " hard levels with 3 stars");
        Log.i(TAG, "Achievement unlocked at level: " + levelAtWhichUnlocked);
        
        // Verify 3_star_10_hard_levels is unlocked
        assertTrue("Should have completed 10 hard levels", hardLevelsCompleted >= 10);
        assertTrue("3_star_10_hard_levels should be unlocked after 10 hard levels with 3 stars",
            achievementManager.isUnlocked("3_star_10_hard_levels"));
        
        Log.i(TAG, "========== TEST PASSED ==========");
    }

    /**
     * Test that 3_star_10_hard_levels is NOT unlocked by completing only short levels
     */
    @Test
    public void testThreeStar10HardLevelsNotUnlockedByShortLevels() {
        Log.i(TAG, "========== TESTING 3_star_10_hard_levels with short levels ==========");
        
        // Complete only short levels (< 5 moves) with 3 stars
        int shortLevelsCompleted = 0;
        
        for (int levelId = 1; levelId <= 140 && shortLevelsCompleted < 20; levelId++) {
            int optimalMoves = LevelSolutionData.getOptimalMoves(levelId);
            
            if (optimalMoves == -1) continue;
            
            // Only complete levels with < 5 optimal moves
            if (optimalMoves < 5) {
                achievementManager.onNewGameStarted();
            achievementManager.onLevelCompleted(levelId, optimalMoves, optimalMoves, 0, 3, 10000);
                shortLevelsCompleted++;
                
                Log.d(TAG, "Completed short level " + levelId + " (" + optimalMoves + " moves), total: " + shortLevelsCompleted);
            }
        }
        
        Log.i(TAG, "Completed " + shortLevelsCompleted + " short levels with 3 stars");
        
        // Verify 3_star_10_hard_levels is NOT unlocked
        assertFalse("3_star_10_hard_levels should NOT be unlocked by short levels only",
            achievementManager.isUnlocked("3_star_10_hard_levels"));
        
        // But 3_star_10_levels should be unlocked (counts all levels)
        if (shortLevelsCompleted >= 10) {
            assertTrue("3_star_10_levels should be unlocked after 10 levels with 3 stars",
                achievementManager.isUnlocked("3_star_10_levels"));
        }
        
        Log.i(TAG, "========== TEST PASSED ==========");
    }

    /**
     * Test the distribution of levels by optimal move count
     */
    @Test
    public void testLevelMoveDistribution() {
        Log.i(TAG, "========== LEVEL MOVE DISTRIBUTION ==========");
        
        int[] moveCounts = new int[31];
        int totalLevels = 0;
        int levelsWithFivePlus = 0;
        
        for (int levelId = 1; levelId <= 140; levelId++) {
            int optimalMoves = LevelSolutionData.getOptimalMoves(levelId);
            
            if (optimalMoves == -1) {
                Log.w(TAG, "Level " + levelId + ": No data");
                continue;
            }
            
            totalLevels++;
            int bucket = Math.min(optimalMoves, 30);
            moveCounts[bucket]++;
            
            if (optimalMoves >= 5) {
                levelsWithFivePlus++;
            }
        }
        
        Log.i(TAG, "Total levels with data: " + totalLevels);
        Log.i(TAG, "Levels with 5+ moves: " + levelsWithFivePlus);
        
        Log.i(TAG, "Distribution:");
        for (int i = 1; i <= 30; i++) {
            if (moveCounts[i] > 0) {
                String label = (i == 30) ? "30+" : String.valueOf(i);
                Log.i(TAG, "  " + label + " moves: " + moveCounts[i] + " levels");
            }
        }
        
        // Verify we have data for all 140 levels
        assertEquals("Should have data for all 140 levels", 140, totalLevels);
        
        Log.i(TAG, "========== TEST PASSED ==========");
    }
}

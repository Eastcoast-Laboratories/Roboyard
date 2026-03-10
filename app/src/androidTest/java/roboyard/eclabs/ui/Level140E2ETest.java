package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import roboyard.eclabs.R;
import roboyard.ui.achievements.AchievementManager;
import roboyard.logic.core.GameSolution;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;

import timber.log.Timber;

/**
 * E2E test: Complete all 140 levels to unlock 3_star_10_levels, 3_star_50_levels, and 3_star_all_levels achievements
 * Uses the solver solution to execute moves automatically
 *
 * Tags: e2e, level-game, achievement, 3-star, solver, all-levels
 */
@RunWith(AndroidJUnit4.class)
public class Level140E2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private AchievementManager achievementManager;
    private GameStateManager gameStateManager;
    private volatile Boolean levelCompleted = null;
    private Map<Integer, LevelResult> levelResults = new HashMap<>();
    private int totalStarsEarned = 0;
    
    // Test all 140 levels
    private static final int MAX_LEVEL = 140;
    
    /**
     * Stores detailed result for each level
     */
    private static class LevelResult {
        int levelId;
        int starsEarned;
        int playerMoves;
        int optimalMoves;
        int hintsUsed;
        boolean completed;
        String failureReason;
        
        LevelResult(int levelId) {
            this.levelId = levelId;
            this.completed = false;
        }
        
        @Override
        public String toString() {
            if (!completed) {
                return String.format("Level %d: FAILED - %s", levelId, failureReason);
            }
            return String.format("Level %d: %d stars (moves: %d/%d, hints: %d)", 
                    levelId, starsEarned, playerMoves, optimalMoves, hintsUsed);
        }
    }

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        achievementManager.resetAll();
        levelResults.clear();
        totalStarsEarned = 0;
        Timber.d("[UNITTESTS][E2E_140LEVELS] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        achievementManager.resetAll();
        Timber.d("[UNITTESTS][E2E_140LEVELS] ========== TEST FINISHED ==========");
    }

    @Test
    public void testComplete140Levels_UnlocksAllMasteryAchievements() throws InterruptedException {
        Timber.d("[UNITTESTS][E2E_140LEVELS] Starting mastery achievements test");
        Thread.sleep(500);
        
        // Get GameStateManager from activity
        activityRule.getScenario().onActivity(activity -> {
            gameStateManager = activity.getGameStateManager();
        });
        
        Thread.sleep(500);
        
        if (gameStateManager == null) {
            Timber.e("[E2E_140LEVELS] GameStateManager is null!");
            fail("GameStateManager is null");
            return;
        }
        
        // Complete levels to test mastery achievements
        for (int level = 1; level <= MAX_LEVEL; level++) {
            Timber.d("[UNITTESTS][E2E_140LEVELS] ===== Starting Level %d =====", level);
            
            // Print stats before Level 140
            if (level == 140) {
                Timber.d("[UNITTESTS][E2E_140LEVELS] ===== STATS BEFORE LEVEL 140 =====");
                printStatsReport();
            }
            
            LevelResult result = new LevelResult(level);
            levelResults.put(level, result);
            
            // Start level directly via GameStateManager
            final int currentLevel = level;
            activityRule.getScenario().onActivity(activity -> {
                Timber.d("[UNITTESTS][E2E_140LEVELS] Starting level %d via GameStateManager", currentLevel);
                gameStateManager.startLevelGame(currentLevel);
            });
            
            // Wait for level to load and solver to find solution
            Thread.sleep(5000);
            
            // Execute solution moves
            boolean movesExecuted = false;
            try {
                movesExecuted = executeSolutionMoves(level);
            } catch (Exception e) {
                Timber.e(e, "[E2E_140LEVELS] Error executing moves for level %d", level);
                result.failureReason = "Move execution failed: " + e.getMessage();
            }
            
            if (!movesExecuted) {
                result.failureReason = "Solver returned 0 moves (known bug)";
                result.completed = false;
                Timber.w("[E2E_140LEVELS] Level %d: No moves executed, marking as failed", level);
            }
            
            // Wait for level completion
            Thread.sleep(1000);
            
            // Check if level is completed and get stars
            final LevelResult currentResult = result;
            activityRule.getScenario().onActivity(activity -> {
                if (gameStateManager != null) {
                    levelCompleted = gameStateManager.isGameComplete().getValue();
                    // Calculate stars
                    currentResult.playerMoves = gameStateManager.getMoveCount().getValue() != null ? 
                            gameStateManager.getMoveCount().getValue() : 0;
                    currentResult.optimalMoves = 0;
                    GameSolution solution = gameStateManager.getCurrentSolution();
                    if (solution != null && solution.getMoves() != null && solution.getMoves().size() > 0) {
                        currentResult.optimalMoves = solution.getMoves().size();
                    }
                    currentResult.hintsUsed = gameStateManager.getCurrentState().getValue() != null ?
                            gameStateManager.getCurrentState().getValue().getHintCount() : 0;
                    currentResult.starsEarned = gameStateManager.calculateStars(
                            currentResult.playerMoves, currentResult.optimalMoves, currentResult.hintsUsed);
                    currentResult.completed = levelCompleted != null && levelCompleted;
                    
                    Timber.d("[UNITTESTS][E2E_140LEVELS] Level %d completed: %s, stars: %d (moves: %d/%d, hints: %d)", 
                            currentLevel, levelCompleted, currentResult.starsEarned,
                            currentResult.playerMoves, currentResult.optimalMoves, currentResult.hintsUsed);
                }
            });
            
            Thread.sleep(500);
            
            if (levelCompleted == null || !levelCompleted) {
                result.failureReason = "Level not completed";
                Timber.e("[E2E_140LEVELS] Level %d NOT completed!", level);
                // Don't fail immediately, continue and report all failures at the end
            }
            
            // Track stars
            totalStarsEarned += result.starsEarned;
            if (result.starsEarned < 3) {
                Timber.w("[E2E_140LEVELS] ⚠️ Level %d: Only %d stars (moves: %d/%d, hints: %d)", 
                        level, result.starsEarned, result.playerMoves, result.optimalMoves, result.hintsUsed);
            }
            
            // Check achievement status after key milestones
            if (level == 10) {
                Thread.sleep(2000);
                boolean threeStar10 = achievementManager.isUnlocked("3_star_10_levels");
                Timber.d("[UNITTESTS][E2E_140LEVELS] After Level 10: 3_star_10_levels = %s", threeStar10);
            }
            
            if (level == 50) {
                Thread.sleep(2000);
                boolean threeStar50 = achievementManager.isUnlocked("3_star_50_levels");
                Timber.d("[UNITTESTS][E2E_140LEVELS] After Level 50: 3_star_50_levels = %s", threeStar50);
            }
            
            if (level == MAX_LEVEL) {
                Thread.sleep(1000);
                boolean threeStarAll = achievementManager.isUnlocked("3_star_all_levels");
                Timber.d("[UNITTESTS][E2E_140LEVELS] After Level %d: 3_star_all_levels = %s", MAX_LEVEL, threeStarAll);
            }
            
            // No need to click Next Level button - we start levels directly via GameStateManager
            
            // Reset for next level
            levelCompleted = null;
        }
        
        // Wait for achievements to be processed
        Thread.sleep(5000);
        
        // Print final stats
        Timber.d("[UNITTESTS][E2E_140LEVELS] ===== FINAL STATS AFTER ALL LEVELS =====");
        printStatsReport();
        
        // Generate detailed final report
        printFinalReport();
        
        // Final assertions with detailed error messages
        assertTrue("3_star_hard_level should be unlocked", 
                achievementManager.isUnlocked("3_star_hard_level"));
        assertTrue("3_star_10_levels should be unlocked", 
                achievementManager.isUnlocked("3_star_10_levels"));
        assertTrue("3_star_50_levels should be unlocked after completing 50 levels", 
                achievementManager.isUnlocked("3_star_50_levels"));
        assertTrue("3_star_all_levels should be unlocked after completing all 140 levels", 
                achievementManager.isUnlocked("3_star_all_levels"));
        
        // Check for any levels without 3 stars and fail with detailed info
        ArrayList<LevelResult> problemLevels = new ArrayList<>();
        for (LevelResult result : levelResults.values()) {
            if (!result.completed || result.starsEarned < 3) {
                problemLevels.add(result);
            }
        }
        
        if (!problemLevels.isEmpty()) {
            StringBuilder failMessage = new StringBuilder();
            failMessage.append("\n\n========== LEVELS WITHOUT 3 STARS ==========");
            failMessage.append("\nTotal: ").append(problemLevels.size()).append(" levels\n");
            for (LevelResult result : problemLevels) {
                failMessage.append("\n  ").append(result.toString());
            }
            failMessage.append("\n\n============================================\n");
            fail(failMessage.toString());
        }
        
        if (problemLevels.isEmpty()) {
            Timber.d("[UNITTESTS][E2E_140LEVELS] ✓ Test passed: All %d levels completed with 3 stars", MAX_LEVEL);
        } else {
            Timber.d("[UNITTESTS][E2E_140LEVELS] ✗ Test failed: %d levels without 3 stars", problemLevels.size());
        }
    }
    
    /**
     * Execute the solution moves for the current level via TestHelper
     * @return true if moves were executed, false if no solution found
     */
    private boolean executeSolutionMoves(int level) throws InterruptedException {
        return TestHelper.executeSolutionMoves(activityRule, gameStateManager, level, "E2E_140LEVELS");
    }
    
    /**
     * Print stats report for levels completed so far
     */
    private void printStatsReport() {
        int maxStars = levelResults.size() * 3;
        int levelsWithThreeStars = 0;
        int levelsWithTwoStars = 0;
        int levelsWithOneStar = 0;
        int levelsWithZeroStars = 0;
        int failedLevels = 0;
        
        ArrayList<LevelResult> problemLevels = new ArrayList<>();
        
        for (LevelResult result : levelResults.values()) {
            if (!result.completed) {
                failedLevels++;
                problemLevels.add(result);
            } else if (result.starsEarned == 3) {
                levelsWithThreeStars++;
            } else if (result.starsEarned == 2) {
                levelsWithTwoStars++;
                problemLevels.add(result);
            } else if (result.starsEarned == 1) {
                levelsWithOneStar++;
                problemLevels.add(result);
            } else {
                levelsWithZeroStars++;
                problemLevels.add(result);
            }
        }
        
        Timber.d("[UNITTESTS][E2E_140LEVELS] ===== CURRENT STATS =====");
        Timber.d("[UNITTESTS][E2E_140LEVELS] Levels completed: %d", levelResults.size());
        Timber.d("[UNITTESTS][E2E_140LEVELS] Total Stars Earned: %d / %d", totalStarsEarned, maxStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ");
        Timber.d("[UNITTESTS][E2E_140LEVELS] ⭐⭐⭐ 3 stars: %d levels", levelsWithThreeStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ⭐⭐   2 stars: %d levels", levelsWithTwoStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ⭐     1 star:  %d levels", levelsWithOneStar);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ☆     0 stars: %d levels", levelsWithZeroStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ❌    Failed:  %d levels", failedLevels);
        
        if (!problemLevels.isEmpty()) {
            Timber.d("[UNITTESTS][E2E_140LEVELS] ===== PROBLEM LEVELS =====");
            for (LevelResult result : problemLevels) {
                Timber.d("[UNITTESTS][E2E_140LEVELS] %s", result.toString());
            }
        }
        Timber.d("[UNITTESTS][E2E_140LEVELS] ============================");
    }
    
    /**
     * Print detailed final report of all level results
     */
    private void printFinalReport() {
        int maxStars = MAX_LEVEL * 3;
        int levelsWithThreeStars = 0;
        int levelsWithTwoStars = 0;
        int levelsWithOneStar = 0;
        int levelsWithZeroStars = 0;
        int failedLevels = 0;
        
        ArrayList<LevelResult> problemLevels = new ArrayList<>();
        
        for (int i = 1; i <= MAX_LEVEL; i++) {
            LevelResult result = levelResults.get(i);
            if (result == null) {
                failedLevels++;
                continue;
            }
            if (!result.completed) {
                failedLevels++;
                problemLevels.add(result);
            } else if (result.starsEarned == 3) {
                levelsWithThreeStars++;
            } else if (result.starsEarned == 2) {
                levelsWithTwoStars++;
                problemLevels.add(result);
            } else if (result.starsEarned == 1) {
                levelsWithOneStar++;
                problemLevels.add(result);
            } else {
                levelsWithZeroStars++;
                problemLevels.add(result);
            }
        }
        
        Timber.d("[UNITTESTS][E2E_140LEVELS] ===== FINAL REPORT =====");
        Timber.d("[UNITTESTS][E2E_140LEVELS] Levels tested: %d (skipped Level 140 due to OOM)", MAX_LEVEL);
        Timber.d("[UNITTESTS][E2E_140LEVELS] Total Stars Earned: %d / %d", totalStarsEarned, maxStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ");
        Timber.d("[UNITTESTS][E2E_140LEVELS] ⭐⭐⭐ 3 stars: %d levels", levelsWithThreeStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ⭐⭐   2 stars: %d levels", levelsWithTwoStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ⭐     1 star:  %d levels", levelsWithOneStar);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ☆     0 stars: %d levels", levelsWithZeroStars);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ❌    Failed:  %d levels", failedLevels);
        Timber.d("[UNITTESTS][E2E_140LEVELS] ");
        
        if (!problemLevels.isEmpty()) {
            Timber.d("[UNITTESTS][E2E_140LEVELS] ===== PROBLEM LEVELS =====");
            for (LevelResult result : problemLevels) {
                Timber.d("[UNITTESTS][E2E_140LEVELS] %s", result.toString());
            }
        } else {
            Timber.d("[UNITTESTS][E2E_140LEVELS] ✓ All levels completed with 3 stars!");
        }
        
        Timber.d("[UNITTESTS][E2E_140LEVELS] ============================");
    }
}

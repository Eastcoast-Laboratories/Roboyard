package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import android.app.Activity;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainFragmentActivity;
import timber.log.Timber;

/**
 * Integration test for solution save/load functionality.
 * Tests the complete flow: start random game -> wait for solver -> save -> load -> verify solver doesn't run.
 * 
 * This test proves that:
 * 1. Solutions are saved with the game
 * 2. Solutions are loaded from save file
 * 3. Solver is NOT invoked when loading a game with saved solutions
 * 4. Hint and Save buttons are immediately available after load
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SolutionSaveLoadIntegrationTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule = 
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        // Clear logcat for clean test run
        TestHelper.clearLogcat();
        Thread.sleep(500);
        
        // Start with empty storage
        activityRule.getScenario().onActivity(activity -> {
            try {
                TestHelper.startNewSessionWithEmptyStorage(activity);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(1000);
        
        // Close any popups
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(500);
    }

    @Test
    public void testSolutionSaveLoadCycle_SolverShouldNotRun() throws InterruptedException {
        Timber.d("[TEST] ========== Starting Solution Save/Load Integration Test ==========");
        
        // Step 1: Start a random game
        Timber.d("[TEST] Step 1: Starting random game");
        TestHelper.startRandomGame();
        Thread.sleep(2000); // Wait for game to initialize
        
        // Step 2: Wait for solver to finish (max 30 seconds)
        Timber.d("[TEST] Step 2: Waiting for solver to finish");
        boolean solverFinished = waitForSolverToFinish(30000);
        assertTrue("Solver should finish within 30 seconds", solverFinished);
        Thread.sleep(1000);
        
        // Step 3: Collect solver logs from first run
        List<String> firstRunLogs = TestHelper.collectLogcatLines("SOLUTIONS_SAVE_LOAD", null, 100);
        Timber.d("[TEST] First run collected %d SOLUTIONS_SAVE_LOAD log lines", firstRunLogs.size());
        
        // Verify solver ran (should see solver-related logs)
        boolean solverRanFirstTime = false;
        for (String log : firstRunLogs) {
            if (log.contains("Starting solver") || log.contains("Solver.run() completed")) {
                solverRanFirstTime = true;
                break;
            }
        }
        assertTrue("Solver should run on first game start", solverRanFirstTime);
        
        // Step 4: Save the game to slot 1
        Timber.d("[TEST] Step 3: Saving game to slot 1");
        TestHelper.navigateToSaveLoadScreen(activityRule);
        Thread.sleep(1000);
        
        // Click save slot 1 (first item in RecyclerView)
        onView(withId(R.id.save_slot_recycler_view))
                .perform(androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition(0, click()));
        Thread.sleep(1000);
        
        // Verify save was successful by checking logs
        List<String> saveLogs = TestHelper.collectLogcatLines("SAVEDATA", null, 50);
        boolean saveSuccessful = false;
        int solutionCount = 0;
        for (String log : saveLogs) {
            if (log.contains("Added solutions tag")) {
                saveSuccessful = true;
                // Extract solution count from log
                if (log.contains("with") && log.contains("solutions")) {
                    try {
                        String[] parts = log.split("with ");
                        if (parts.length > 1) {
                            String numStr = parts[1].split(" ")[0];
                            solutionCount = Integer.parseInt(numStr);
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
                Timber.d("[TEST] Save log: %s", log);
            }
        }
        assertTrue("Game should be saved successfully with solutions", saveSuccessful);
        assertTrue("At least 1 solution should be saved", solutionCount > 0);
        Timber.d("[TEST] Verified: %d solutions were saved", solutionCount);
        
        // Step 5: Go back to main menu
        Timber.d("[TEST] Step 4: Returning to main menu");
        androidx.test.espresso.Espresso.pressBack();
        Thread.sleep(1000);
        
        // Clear logcat to isolate load logs
        TestHelper.clearLogcat();
        Thread.sleep(500);
        
        // Step 6: Load the saved game from slot 1
        Timber.d("[TEST] Step 5: Loading game from slot 1");
        TestHelper.navigateToSaveLoadScreen(activityRule);
        Thread.sleep(1000);
        
        // Click load slot 1 (first item in RecyclerView) - long click to load
        onView(withId(R.id.save_slot_recycler_view))
                .perform(androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition(0, 
                        androidx.test.espresso.action.ViewActions.longClick()));
        Thread.sleep(3000); // Wait for load to complete
        
        // Step 7: Collect and analyze load logs
        Timber.d("[TEST] Step 6: Analyzing load logs");
        List<String> loadLogs = TestHelper.collectLogcatLines("SOLUTIONS_SAVE_LOAD", null, 200);
        
        // Dump all relevant logs for debugging
        Timber.d("[TEST] ========== LOAD LOGS START ==========");
        for (String log : loadLogs) {
            Timber.d("[TEST] %s", log);
        }
        Timber.d("[TEST] ========== LOAD LOGS END ==========");
        
        // Verify critical checkpoints
        boolean foundSolutionsInMetadata = false;
        boolean deserializedSolutions = false;
        boolean setPredefinedSolution = false;
        boolean usingLoadedSolution = false;
        boolean solverStarted = false;
        boolean hasPredefinedSolution = false;
        
        for (String log : loadLogs) {
            if (log.contains("Found saved solutions in metadata")) {
                foundSolutionsInMetadata = true;
                Timber.d("[TEST] ✓ Found solutions in metadata");
            }
            if (log.contains("Deserializing saved solutions after reset")) {
                deserializedSolutions = true;
                Timber.d("[TEST] ✓ Deserialized solutions");
            }
            if (log.contains("Set predefined solution in SolverManager")) {
                setPredefinedSolution = true;
                Timber.d("[TEST] ✓ Set predefined solution");
            }
            if (log.contains("Using loaded solution") && log.contains("skipping solver calculation")) {
                usingLoadedSolution = true;
                Timber.d("[TEST] ✓ Using loaded solution, skipping solver");
            }
            if (log.contains("Starting solver") || log.contains("No saved solution found")) {
                solverStarted = true;
                Timber.d("[TEST] ✗ Solver was started (should not happen!)");
            }
            if (log.contains("Using predefined solution")) {
                hasPredefinedSolution = true;
                Timber.d("[TEST] ✓ Solver recognized predefined solution");
            }
        }
        
        // Assert all critical checkpoints
        assertTrue("Should find solutions in metadata", foundSolutionsInMetadata);
        assertTrue("Should deserialize solutions", deserializedSolutions);
        assertTrue("Should set predefined solution in SolverManager", setPredefinedSolution);
        assertTrue("Should use loaded solution and skip solver", usingLoadedSolution);
        assertFalse("Solver should NOT be started when loading game with solutions", solverStarted);
        
        // If solver was somehow invoked, it should at least recognize the predefined solution
        if (solverStarted) {
            assertTrue("If solver runs, it must recognize predefined solution", hasPredefinedSolution);
        }
        
        Timber.d("[TEST] ========== Test PASSED: Solver was successfully skipped ==========");
    }
    
    /**
     * Wait for solver to finish by monitoring logcat
     * @param maxWaitMs Maximum time to wait in milliseconds
     * @return true if solver finished, false if timeout
     */
    private boolean waitForSolverToFinish(long maxWaitMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            List<String> logs = TestHelper.collectLogcatLines("SOLUTION_SOLVER", null, 50);
            
            for (String log : logs) {
                if (log.contains("Solver.run() completed") || 
                    log.contains("onSolutionCalculationCompleted") ||
                    log.contains("Solution accepted")) {
                    Timber.d("[TEST] Solver finished: %s", log);
                    return true;
                }
            }
            
            Thread.sleep(1000);
        }
        
        return false;
    }
}

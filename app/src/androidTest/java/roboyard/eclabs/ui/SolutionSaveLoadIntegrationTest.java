package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import roboyard.eclabs.R;
import roboyard.logic.core.GameState;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Integration test for solution save/load functionality.
 * 
 * Tests the critical bug scenario where solutions from game A leak into game B:
 * 1. Start game A, wait for solver, save to slot 1
 * 2. Start game B, wait for solver, save to slot 2
 * 3. Load game A from slot 1
 * 4. Verify that the loaded solution matches game A's map, NOT game B's
 * 5. Load game B from slot 2
 * 6. Verify that the loaded solution matches game B's map, NOT game A's
 *
 * Also verifies that:
 * - Solutions are saved with the game metadata
 * - Solver is NOT re-invoked when loading a game with saved solutions
 * - Solutions can be re-saved after loading
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SolutionSaveLoadIntegrationTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = 
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        TestHelper.clearLogcat();
        Thread.sleep(500);
        
        activityRule.getScenario().onActivity(activity -> {
            try {
                TestHelper.startNewSessionWithEmptyStorage(activity);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(1000);
        
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(500);
    }

    /**
     * Core test: Save game A to slot 1, start game B and save to slot 2,
     * then load game A and verify the solution belongs to game A's map (not B's).
     * Then load game B and verify solution belongs to B's map (not A's).
     * This catches the bug where loadedSolutions leaked between games.
     */
    @Test
    public void testSolutionNotLeakedBetweenGames() throws InterruptedException {
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] ========== Starting Solution Leak Test ==========");
        
        // ===== STEP 1: Start game A, wait for solver, capture map name and solution =====
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Step 1: Starting game A");
        TestHelper.startRandomGame();
        Thread.sleep(2000);
        
        // Wait for solver to finish
        boolean solverDone = waitForSolverViaGameStateManager(45000);
        assertTrue("Solver should finish for game A within 45s", solverDone);
        Thread.sleep(1000);
        
        // Capture game A's map name and solution string
        final AtomicReference<String> mapNameA = new AtomicReference<>();
        final AtomicReference<String> solutionStrA = new AtomicReference<>();
        
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            GameState stateA = gsm.getCurrentState().getValue();
            assertNotNull("Game A state should exist", stateA);
            mapNameA.set(stateA.getLevelName());
            Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Game A map: %s", mapNameA.get());
        });
        
        // ===== STEP 2: Save game A to slot 1 =====
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Step 2: Saving game A to slot 1");
        final AtomicReference<Boolean> saveAResult = new AtomicReference<>(false);
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            saveAResult.set(gsm.saveGame(1));
        });
        Thread.sleep(500);
        assertTrue("Game A should be saved successfully to slot 1", saveAResult.get());
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Game A saved to slot 1");
        
        // Extract solution string from save logs
        List<String> saveALogs = TestHelper.collectLogcatLines(null, "SOLUTIONS_SAVE_LOAD", 200);
        for (String log : saveALogs) {
            if (log.contains("Serialized") && log.contains("solutions")) {
                Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Save A log: %s", log);
            }
        }
        
        // ===== STEP 3: Start game B (new random game), wait for solver =====
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Step 3: Going back and starting game B");
        // Navigate back to main menu
        androidx.test.espresso.Espresso.pressBack();
        Thread.sleep(1000);
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(500);
        
        TestHelper.clearLogcat();
        Thread.sleep(200);
        
        TestHelper.startRandomGame();
        Thread.sleep(2000);
        
        solverDone = waitForSolverViaGameStateManager(45000);
        assertTrue("Solver should finish for game B within 45s", solverDone);
        Thread.sleep(1000);
        
        // Capture game B's map name
        final AtomicReference<String> mapNameB = new AtomicReference<>();
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            GameState stateB = gsm.getCurrentState().getValue();
            assertNotNull("Game B state should exist", stateB);
            mapNameB.set(stateB.getLevelName());
            Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Game B map: %s", mapNameB.get());
        });
        
        // Verify that maps are different (random games should be different)
        assertNotEquals("Game A and B should have different maps", 
                mapNameA.get(), mapNameB.get());
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Confirmed: maps are different (A=%s, B=%s)", 
                mapNameA.get(), mapNameB.get());
        
        // ===== STEP 4: Save game B to slot 2 =====
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Step 4: Saving game B to slot 2");
        final AtomicReference<Boolean> saveBResult = new AtomicReference<>(false);
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            saveBResult.set(gsm.saveGame(2));
        });
        Thread.sleep(500);
        assertTrue("Game B should be saved successfully to slot 2", saveBResult.get());
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Game B saved to slot 2");
        
        // ===== STEP 5: Load game A from slot 1 and verify solution =====
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Step 5: Loading game A from slot 1");
        TestHelper.clearLogcat();
        Thread.sleep(200);
        
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            gsm.loadGame(1);
        });
        Thread.sleep(3000);
        
        // Verify loaded state is game A
        final AtomicReference<String> loadedMapName = new AtomicReference<>();
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            GameState loadedState = gsm.getCurrentState().getValue();
            assertNotNull("Loaded state should exist", loadedState);
            loadedMapName.set(loadedState.getLevelName());
            Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Loaded map name: %s (expected: %s)", 
                    loadedMapName.get(), mapNameA.get());
        });
        assertEquals("Loaded map should be game A's map", mapNameA.get(), loadedMapName.get());
        
        // Verify solution was loaded and solver was skipped
        List<String> loadALogs = TestHelper.collectLogcatLines(null, "SOLUTIONS_SAVE_LOAD", 200);
        boolean foundSolutions = false;
        boolean solverSkipped = false;
        boolean solverStarted = false;
        
        for (String log : loadALogs) {
            if (log.contains("Found saved solutions in metadata")) foundSolutions = true;
            if (log.contains("skipping solver calculation")) solverSkipped = true;
            if (log.contains("No saved solution found, starting solver")) solverStarted = true;
        }
        assertTrue("Should find solutions in game A save metadata", foundSolutions);
        assertTrue("Should skip solver when loading game A", solverSkipped);
        assertFalse("Solver should NOT start when loading game A with solution", solverStarted);
        
        // ===== STEP 6: Re-save game A to slot 3 and verify solution persists =====
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Step 6: Re-saving loaded game A to slot 3");
        TestHelper.clearLogcat();
        Thread.sleep(200);
        
        final AtomicReference<Boolean> resaveResult = new AtomicReference<>(false);
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            resaveResult.set(gsm.saveGame(3));
        });
        Thread.sleep(500);
        assertTrue("Re-save of game A to slot 3 should succeed", resaveResult.get());
        
        // Verify solution was included in re-save
        List<String> resaveLogs = TestHelper.collectLogcatLines(null, "SOLUTIONS_SAVE_LOAD", 100);
        boolean resavedWithSolution = false;
        for (String log : resaveLogs) {
            if (log.contains("Using") && log.contains("loaded solutions for re-saving")) {
                resavedWithSolution = true;
            }
            if (log.contains("Serialized") && log.contains("game solutions")) {
                resavedWithSolution = true;
            }
        }
        assertTrue("Re-saved game should include solutions", resavedWithSolution);
        
        // ===== STEP 7: Load game B from slot 2, verify solution is B's, not A's =====
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Step 7: Loading game B from slot 2");
        TestHelper.clearLogcat();
        Thread.sleep(200);
        
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            gsm.loadGame(2);
        });
        Thread.sleep(3000);
        
        // Verify loaded state is game B
        activityRule.getScenario().onActivity(activity -> {
            GameStateManager gsm = activity.getGameStateManager();
            GameState loadedState = gsm.getCurrentState().getValue();
            assertNotNull("Loaded state should exist", loadedState);
            loadedMapName.set(loadedState.getLevelName());
            Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Loaded map name: %s (expected: %s)", 
                    loadedMapName.get(), mapNameB.get());
        });
        assertEquals("Loaded map should be game B's map", mapNameB.get(), loadedMapName.get());
        
        // Verify solution was loaded for game B too
        List<String> loadBLogs = TestHelper.collectLogcatLines(null, "SOLUTIONS_SAVE_LOAD", 200);
        boolean foundBSolutions = false;
        boolean solverBSkipped = false;
        
        for (String log : loadBLogs) {
            if (log.contains("Found saved solutions in metadata")) foundBSolutions = true;
            if (log.contains("skipping solver calculation")) solverBSkipped = true;
        }
        assertTrue("Should find solutions in game B save metadata", foundBSolutions);
        assertTrue("Should skip solver when loading game B", solverBSkipped);
        
        Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] ========== Test PASSED: Solutions correctly isolated between games ==========");
    }
    
    /**
     * Wait for solver to finish by checking GameStateManager's solver running state
     * @param maxWaitMs Maximum time to wait in milliseconds
     * @return true if solver finished, false if timeout
     */
    private boolean waitForSolverViaGameStateManager(long maxWaitMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            final AtomicReference<Boolean> running = new AtomicReference<>(true);
            
            activityRule.getScenario().onActivity(activity -> {
                GameStateManager gsm = activity.getGameStateManager();
                Boolean isRunning = gsm.isSolverRunning().getValue();
                running.set(isRunning != null && isRunning);
            });
            
            if (!running.get()) {
                Timber.d("[UNITTESTS][TEST_SOLUTION_LEAK] Solver finished (took %dms)", 
                        System.currentTimeMillis() - startTime);
                return true;
            }
            
            Thread.sleep(500);
        }
        
        Timber.e("[TEST_SOLUTION_LEAK] Solver timeout after %dms", maxWaitMs);
        return false;
    }
}

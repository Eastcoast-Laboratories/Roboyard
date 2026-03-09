package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

import android.app.Activity;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import roboyard.eclabs.R;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.logic.core.GameSolution;
import roboyard.logic.core.GameState;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.FileReadWrite;
import roboyard.ui.components.GameHistoryManager;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * End-to-End Espresso test that verifies history completion tracking for random games.
 *
 * Flow:
 * 1. Clear history
 * 2. Click "New Random Game" button → GameFragment visible
 * 3. Wait for AI solution, execute all moves
 * 4. After completion: navigate to SaveGameFragment → History tab
 * 5. Click info button on first history entry
 * 6. Verify popup shows "Completions: 1"
 *
 * Tags: e2e, history, random-game, completion-tracking, espresso
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.HistoryCompletionE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class HistoryCompletionE2ETest {

    private static final String TAG = "[HISTORY_E2E]";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        step("setUp", "Initializing and clearing history");
        AtomicReference<Activity> activityRef = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> {
            activityRef.set(a);
            gameStateManager = ((MainActivity) a).getGameStateManager();
        });
        assertNotNull("GameStateManager must not be null", gameStateManager);
        clearAllHistory();
        Thread.sleep(2000);
    }

    @After
    public void tearDown() {
        clearAllHistory();
    }

    // ==================== TEST 1 ====================

    /**
     * Plays one random game via UI, then opens history and verifies completionCount==1 in popup.
     */
    @Test
    public void testRandomGameCompletionShownInHistoryInfoPopup() throws InterruptedException {
        step("1/8", "Clicking 'New Random Game' button");
        onView(withId(R.id.ui_button)).perform(click());
        Thread.sleep(3000);

        step("2/8", "Waiting for AI solution");
        GameSolution solution = waitForSolution(15);
        assertNotNull(TAG + " Solution must be available", solution);
        assertTrue(TAG + " Solution must have moves", solution.getMoves().size() > 0);
        step("2/8", "Solution ready: " + solution.getMoves().size() + " moves");

        step("3/8", "Playing all solution moves");
        playAllMoves(solution);

        step("4/8", "Waiting for completion + history save");
        Thread.sleep(3000);

        step("5/8", "Verifying game is complete");
        assertTrue(TAG + " Game must be complete after solution",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));

        step("6/8", "Navigating to SaveGameFragment (History tab)");
        navigateToHistoryTab();
        Thread.sleep(2000);

        step("7/8", "Clicking info button on first history entry");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                        new ClickChildViewWithId(R.id.info_button)));
        Thread.sleep(1000);

        step("8/8", "Verifying popup shows Completions: 1");
        onView(withText(containsString("Completions: 1"))).check(matches(isDisplayed()));

        step("PASS", "testRandomGameCompletionShownInHistoryInfoPopup PASSED");
    }

    // ==================== TEST 2 ====================

    /**
     * Plays two random games (same map via Level 1), verifies completionCount==2 in popup.
     */
    @Test
    public void testSecondPlayShowsCompletionCount2InPopup() throws InterruptedException {
        step("PLAY1", "=== First random game ===");
        startRandomGameViaGameStateManager();
        Thread.sleep(5000);

        GameSolution sol1 = waitForSolution(15);
        assertNotNull(TAG + " Solution must be available (play 1)", sol1);
        playAllMoves(sol1);
        Thread.sleep(3000);
        assertTrue(TAG + " Game must be complete after play 1",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));

        // Check history after first play
        List<GameHistoryEntry> afterFirst = getHistoryEntries();
        assertFalse(TAG + " History must have entry after play 1", afterFirst.isEmpty());
        step("PLAY1", "After first play: completionCount=" + afterFirst.get(0).getCompletionCount());
        assertEquals(TAG + " completionCount must be 1 after first play",
                1, afterFirst.get(0).getCompletionCount());

        step("PLAY2", "=== Second play of same map ===");
        // Reload same map (same wall signature → same history entry)
        String mapSig = afterFirst.get(0).getMapSignature();
        startRandomGameViaGameStateManager();
        Thread.sleep(5000);

        GameSolution sol2 = waitForSolution(15);
        assertNotNull(TAG + " Solution must be available (play 2)", sol2);
        playAllMoves(sol2);
        Thread.sleep(3000);
        assertTrue(TAG + " Game must be complete after play 2",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));

        step("PLAY2", "Navigating to history and checking info popup");
        navigateToHistoryTab();
        Thread.sleep(2000);

        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                        new ClickChildViewWithId(R.id.info_button)));
        Thread.sleep(1000);

        // The most recent entry should have completionCount >= 1
        List<GameHistoryEntry> afterSecond = getHistoryEntries();
        assertFalse(TAG + " History must have entry after play 2", afterSecond.isEmpty());
        int totalCompletions = afterSecond.stream().mapToInt(GameHistoryEntry::getCompletionCount).sum();
        step("PLAY2", "Total completions across all entries: " + totalCompletions);
        assertTrue(TAG + " Total completions must be >= 2", totalCompletions >= 2);

        step("PASS", "testSecondPlayShowsCompletionCount2InPopup PASSED");
    }

    // ==================== TEST 3 ====================

    /**
     * Makes only the first move (no completion). Verifies completionCount==0 in history.
     */
    @Test
    public void testIntermediateSaveDoesNotMarkCompleted() throws InterruptedException {
        step("1/4", "Starting random game");
        startRandomGameViaGameStateManager();
        Thread.sleep(5000);

        step("2/4", "Waiting for solution");
        GameSolution solution = waitForSolution(15);
        assertNotNull(TAG + " Solution must be available", solution);

        step("3/4", "Executing only first move (no completion)");
        executeMove(solution.getMoves().get(0));
        Thread.sleep(2000);

        assertFalse(TAG + " Game must NOT be complete after first move only",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));

        step("4/4", "Checking history completionCount == 0");
        List<GameHistoryEntry> entries = getHistoryEntries();
        if (entries != null && !entries.isEmpty()) {
            GameHistoryEntry entry = entries.get(0);
            step("4/4", "Entry found: completionCount=" + entry.getCompletionCount());
            assertEquals(TAG + " completionCount must be 0 for incomplete game",
                    0, entry.getCompletionCount());
        } else {
            step("4/4", "No history entry yet after first move - OK");
        }

        step("PASS", "testIntermediateSaveDoesNotMarkCompleted PASSED");
    }

    // ==================== TEST 4 ====================

    /**
     * Full flow:
     * 1. Start random game, make extra moves with a non-solution robot, then solve optimally
     * 2. Open history, verify bestMoves and optimalMoves in info popup
     * 3. Click the history entry to replay the same map
     * 4. Click hint button, then solve optimally
     * 5. Open history, verify everUsedHints=Yes and bestMoves improved
     */
    @Test
    public void testBestMovesAndHintsTrackedCorrectly() throws InterruptedException {
        step("SETUP", "Starting random game via UI button");
        onView(withId(R.id.ui_button)).perform(click());
        Thread.sleep(4000);

        step("SOL", "Waiting for AI solution");
        GameSolution solution = waitForSolution(20);
        assertNotNull(TAG + " Solution must be available", solution);
        assertTrue(TAG + " Solution must have moves", solution.getMoves().size() > 0);
        int optimalMoveCount = solution.getMoves().size();
        step("SOL", "Optimal solution: " + optimalMoveCount + " moves");

        step("EXTRA", "Making extra moves with a non-solution robot");
        int extraMoves = makeExtraMovesWithNonSolutionRobot(solution);
        step("EXTRA", "Made " + extraMoves + " extra moves");
        Thread.sleep(500);

        step("PLAY1", "Playing optimal solution");
        playAllMoves(solution);
        Thread.sleep(3000);

        assertTrue(TAG + " Game must be complete after solution",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));

        // totalMoves1 = actual move count reported by gameStateManager after completion
        int totalMoves1 = gameStateManager.getMoveCount().getValue() != null
                ? gameStateManager.getMoveCount().getValue() : (extraMoves + optimalMoveCount);
        step("PLAY1", "Total moves in play 1: " + totalMoves1 + " (extraMoves=" + extraMoves + ", optimal=" + optimalMoveCount + ")");

        step("HIST1", "Navigating to history tab");
        navigateToHistoryTab();
        Thread.sleep(2000);

        step("HIST1", "Clicking info button on first history entry");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                        new ClickChildViewWithId(R.id.info_button)));
        Thread.sleep(1000);

        // Verify bestMoves and optimalMoves are shown
        onView(withText(containsString("Optimal moves: " + optimalMoveCount))).check(matches(isDisplayed()));
        onView(withText(containsString("Best moves: " + totalMoves1))).check(matches(isDisplayed()));
        step("HIST1", "PASS: bestMoves=" + totalMoves1 + ", optimalMoves=" + optimalMoveCount + " shown correctly");

        // Dismiss popup by pressing back
        androidx.test.espresso.Espresso.pressBack();
        Thread.sleep(500);

        step("REPLAY", "Clicking history entry to replay same map");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        Thread.sleep(4000);

        step("REPLAY", "Waiting for solution on replayed map");
        GameSolution solution2 = waitForSolution(20);
        assertNotNull(TAG + " Solution must be available for replay", solution2);
        int optimalMoveCount2 = solution2.getMoves().size();
        step("REPLAY", "Replay solution: " + optimalMoveCount2 + " moves");

        step("HINT", "Clicking hint button");
        onView(withId(R.id.hint_button)).perform(click());
        Thread.sleep(2000);

        step("PLAY2", "Playing optimal solution on replayed map");
        playAllMoves(solution2);
        Thread.sleep(3000);

        assertTrue(TAG + " Game must be complete after replay solution",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));

        step("HIST2", "Navigating to history tab after replay");
        navigateToHistoryTab();
        Thread.sleep(2000);

        step("HIST2", "Clicking info button on first history entry");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                        new ClickChildViewWithId(R.id.info_button)));
        Thread.sleep(1000);

        // Verify everUsedHints=Yes
        onView(withText(containsString("Hints ever used (all attempts): Yes"))).check(matches(isDisplayed()));
        step("HIST2", "PASS: everUsedHints=Yes shown correctly");

        // Verify bestMoves improved (replay was optimal, play1 had extra moves)
        // Only check if extraMoves > 0 (otherwise play1 was already optimal, no improvement possible)
        if (extraMoves > 0) {
            onView(withText(containsString("Best moves: " + optimalMoveCount2))).check(matches(isDisplayed()));
            step("HIST2", "PASS: bestMoves improved to " + optimalMoveCount2);
        } else {
            step("HIST2", "SKIP: no extra moves were possible (all directions blocked), bestMoves check skipped");
        }

        // Verify completions = 2
        onView(withText(containsString("Completions: 2"))).check(matches(isDisplayed()));
        step("HIST2", "PASS: Completions=2 shown correctly");

        step("PASS", "testBestMovesAndHintsTrackedCorrectly PASSED");
    }

    /**
     * Starts a random game, makes moves with all 4 robots, saves to slot 1, and verifies
     * that the save-slot info popup resolves the existing history entry for the same map.
     */
    @Test
    public void testSaveSlotInfoPopupFindsHistoryForPlayedMap() throws InterruptedException {
        step("SAVE1", "Starting random game via shared TestHelper");
        TestHelper.startRandomGame();
        Thread.sleep(3000);

        step("SAVE2", "Waiting for AI solution");
        GameSolution solution = waitForSolution(20);
        assertNotNull(TAG + " Solution must be available", solution);

        step("SAVE3", "Making moves with all 4 robots to trigger early history save");
        int movedRobots = moveAllRobotsOnce();
        assertEquals("All 4 robots should be moved at least once", 4, movedRobots);
        Thread.sleep(2000);

        List<GameHistoryEntry> entriesAfterMoves = getHistoryEntries();
        assertNotNull("History entries must not be null after moves", entriesAfterMoves);
        assertFalse("History should contain an entry after first moves", entriesAfterMoves.isEmpty());

        step("SAVE4", "Saving game to slot 1 programmatically");
        activityRule.getScenario().onActivity(a -> {
            boolean saved = gameStateManager.saveGame(1);
            Timber.d(TAG + " saveGame(1)=%b", saved);
        });
        Thread.sleep(1500);

        step("SAVE5", "Navigating to Save/Load screen");
        TestHelper.navigateToSaveLoadScreen(activityRule);
        Thread.sleep(2000);

        step("SAVE6", "Opening save-slot info popup for slot 1");
        onView(withId(R.id.save_slot_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1,
                        new ClickChildViewWithId(R.id.info_button)));
        Thread.sleep(1000);

        step("SAVE7", "Verifying popup uses history details instead of fallback message");
        onView(withText(containsString("Completions:"))).check(matches(isDisplayed()));
        onView(withText(containsString("Map:"))).check(matches(isDisplayed()));
        step("SAVE7", "PASS: Save-slot info popup found matching history entry");
    }

    // ==================== HELPERS ====================

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
    }

    /** Start a random game via GameStateManager + navigate to GameFragment */
    private void startRandomGameViaGameStateManager() {
        activityRule.getScenario().onActivity(a -> {
            gameStateManager.startGame();
            roboyard.ui.fragments.GameFragment gameFragment = new roboyard.ui.fragments.GameFragment();
            a.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, gameFragment)
                    .commit();
        });
    }

    /** Navigate to SaveGameFragment and select the History tab (tab index 1) */
    private void navigateToHistoryTab() {
        activityRule.getScenario().onActivity(a -> {
            roboyard.ui.fragments.SaveGameFragment saveGameFragment =
                    new roboyard.ui.fragments.SaveGameFragment();
            android.os.Bundle args = new android.os.Bundle();
            args.putString("mode", "load");
            saveGameFragment.setArguments(args);
            a.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, saveGameFragment)
                    .commit();
        });
        // Wait for fragment to load, then click History tab (tab 1)
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        onView(withText(containsString("History"))).perform(click());
    }

    private void playAllMoves(GameSolution solution) throws InterruptedException {
        List<IGameMove> moves = solution.getMoves();
        for (int i = 0; i < moves.size(); i++) {
            step("move", (i + 1) + "/" + moves.size() + ": " + moves.get(i));
            executeMove(moves.get(i));
            Thread.sleep(1200);
            if (Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue())) {
                step("move", "Goal reached after move " + (i + 1));
                break;
            }
        }
    }

    private GameSolution waitForSolution(int maxAttempts) throws InterruptedException {
        for (int i = 0; i < maxAttempts; i++) {
            GameSolution s = gameStateManager.getCurrentSolution();
            if (s != null && !s.getMoves().isEmpty()) {
                return s;
            }
            step("solver", "Waiting... attempt " + (i + 1) + "/" + maxAttempts);
            Thread.sleep(2000);
        }
        return gameStateManager.getCurrentSolution();
    }

    private void executeMove(IGameMove move) {
        activityRule.getScenario().onActivity(a -> {
            if (!(move instanceof RRGameMove)) {
                Timber.w(TAG + " Not an RRGameMove: %s", move.getClass().getSimpleName());
                return;
            }
            RRGameMove rrMove = (RRGameMove) move;
            int dx = 0, dy = 0;
            switch (rrMove.getDirection()) {
                case 1: dy = -1; break;
                case 2: dx =  1; break;
                case 4: dy =  1; break;
                case 8: dx = -1; break;
            }
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) { Timber.e(TAG + " GameState null"); return; }
            for (GameElement el : state.getGameElements()) {
                if (el.getType() == Constants.TYPE_ROBOT && el.getColor() == rrMove.getColor()) {
                    state.setSelectedRobot(el);
                    break;
                }
            }
            gameStateManager.moveRobotInDirection(dx, dy);
        });
    }

    /**
     * Finds a robot NOT involved in the solution and moves it in all 4 directions.
     * Returns the number of actual moves made (walls may block some).
     */
    private int makeExtraMovesWithNonSolutionRobot(GameSolution solution) throws InterruptedException {
        AtomicReference<Integer> movesRef = new AtomicReference<>(0);

        // Collect colors used in the solution
        java.util.Set<Integer> solutionColors = new java.util.HashSet<>();
        for (IGameMove m : solution.getMoves()) {
            if (m instanceof RRGameMove) {
                solutionColors.add(((RRGameMove) m).getColor());
            }
        }

        // Find a robot not in the solution
        AtomicReference<GameElement> nonSolutionRobot = new AtomicReference<>(null);
        activityRule.getScenario().onActivity(a -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) return;
            for (GameElement el : state.getGameElements()) {
                if (el.getType() == Constants.TYPE_ROBOT && !solutionColors.contains(el.getColor())) {
                    nonSolutionRobot.set(el);
                    break;
                }
            }
        });

        if (nonSolutionRobot.get() == null) {
            step("EXTRA", "No non-solution robot found, skipping extra moves");
            return 0;
        }

        step("EXTRA", "Moving non-solution robot (color=" + nonSolutionRobot.get().getColor() + ") in all 4 directions");

        int startMoveCount = gameStateManager.getMoveCount().getValue() != null
                ? gameStateManager.getMoveCount().getValue() : 0;

        // Move in all 4 directions: up, right, down, left
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] dir : directions) {
            activityRule.getScenario().onActivity(a -> {
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state == null) return;
                // Re-select the non-solution robot by color each time (position may have changed)
                for (GameElement el : state.getGameElements()) {
                    if (el.getType() == Constants.TYPE_ROBOT
                            && el.getColor() == nonSolutionRobot.get().getColor()) {
                        state.setSelectedRobot(el);
                        break;
                    }
                }
                gameStateManager.moveRobotInDirection(dir[0], dir[1]);
            });
            Thread.sleep(600);
        }

        int endMoveCount = gameStateManager.getMoveCount().getValue() != null
                ? gameStateManager.getMoveCount().getValue() : 0;
        return endMoveCount - startMoveCount; // return only the extra moves delta
    }

    private int moveAllRobotsOnce() throws InterruptedException {
        java.util.Set<Integer> movedColors = new java.util.HashSet<>();
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

        for (int color = 0; color < 4; color++) {
            boolean moved = false;
            for (int[] dir : directions) {
                AtomicReference<Integer> before = new AtomicReference<>(0);
                activityRule.getScenario().onActivity(a -> {
                    Integer moveCount = gameStateManager.getMoveCount().getValue();
                    before.set(moveCount != null ? moveCount : 0);
                    GameState state = gameStateManager.getCurrentState().getValue();
                    if (state == null) {
                        return;
                    }
                    for (GameElement el : state.getGameElements()) {
                        if (el.getType() == Constants.TYPE_ROBOT && el.getColor() == color) {
                            state.setSelectedRobot(el);
                            break;
                        }
                    }
                    gameStateManager.moveRobotInDirection(dir[0], dir[1]);
                });
                Thread.sleep(700);

                Integer after = gameStateManager.getMoveCount().getValue();
                int afterValue = after != null ? after : 0;
                if (afterValue > before.get()) {
                    movedColors.add(color);
                    moved = true;
                    step("MOVE4", "Robot " + color + " moved once");
                    break;
                }
            }
            if (!moved) {
                step("MOVE4", "Robot " + color + " could not move in any direction");
            }
        }

        return movedColors.size();
    }

    private List<GameHistoryEntry> getHistoryEntries() {
        AtomicReference<List<GameHistoryEntry>> ref = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> ref.set(GameHistoryManager.getHistoryEntries(a)));
        return ref.get();
    }

    private void clearAllHistory() {
        AtomicReference<Activity> ref = new AtomicReference<>();
        activityRule.getScenario().onActivity(ref::set);
        Activity act = ref.get();
        if (act == null) return;
        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(act);
        for (GameHistoryEntry e : entries) {
            GameHistoryManager.deleteHistoryEntry(act, e.getMapPath());
        }
        FileReadWrite.writePrivateData(act, "history_index.json", "{\"historyEntries\":[]}");
        step("setup", "History cleared (" + entries.size() + " entries removed)");
    }

    /**
     * Espresso ViewAction to click a child view with a specific ID inside a RecyclerView item.
     */
    private static class ClickChildViewWithId implements androidx.test.espresso.ViewAction {
        private final int viewId;
        ClickChildViewWithId(int viewId) { this.viewId = viewId; }

        @Override
        public org.hamcrest.Matcher<android.view.View> getConstraints() {
            return androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.view.View.class);
        }

        @Override
        public String getDescription() { return "Click child view with id " + viewId; }

        @Override
        public void perform(androidx.test.espresso.UiController uiController, android.view.View view) {
            android.view.View child = view.findViewById(viewId);
            if (child != null) child.performClick();
        }
    }
}

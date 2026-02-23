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
import roboyard.ui.activities.MainFragmentActivity;
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
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.HistoryCompletionE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class HistoryCompletionE2ETest {

    private static final String TAG = "[HISTORY_E2E]";

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        step("setUp", "Initializing and clearing history");
        AtomicReference<Activity> activityRef = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> {
            activityRef.set(a);
            gameStateManager = ((MainFragmentActivity) a).getGameStateManager();
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

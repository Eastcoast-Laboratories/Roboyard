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
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
import roboyard.ui.fragments.LevelSelectionFragment;
import timber.log.Timber;

/**
 * End-to-End test verifying that LevelSelectionFragment shows minimaps and info-buttons
 * for levels that have a history entry.
 *
 * Flow:
 * 1. Clear history
 * 2. Play Level 1 via GameStateManager (solve with solver moves)
 * 3. Verify a history entry for "Level 1" exists with correct boardSize and completionCount
 * 4. Navigate to LevelSelectionFragment
 * 5. Verify the minimap (level_minimap_view) for Level 1 is VISIBLE and has a non-null bitmap
 * 6. Verify the info button (level_info_button) for Level 1 is VISIBLE
 * 7. Click the info button and verify the popup shows "Completions: 1"
 *
 * Tags: e2e, level-screen, minimap, info-button, history, level-game, solver, espresso
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.LevelScreenMinimapE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class LevelScreenMinimapE2ETest {

    private static final String TAG = "[LEVEL_MINIMAP_E2E]";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        step("setUp", "Initializing and clearing history");
        activityRule.getScenario().onActivity(a ->
                gameStateManager = ((MainActivity) a).getGameStateManager());
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
     * Plays Level 1, then navigates to LevelSelectionFragment and verifies that:
     * - level_minimap_view for Level 1 is VISIBLE with a bitmap
     * - level_info_button for Level 1 is VISIBLE
     * - clicking info button shows popup with "Completions: 1"
     */
    @Test
    public void testLevel1MinimapAndInfoButtonVisibleAfterCompletion() throws InterruptedException {
        step("1/7", "Loading Level 1 via GameStateManager");
        loadLevel(1);
        Thread.sleep(5000);

        step("2/7", "Waiting for solver solution");
        GameSolution solution = waitForSolution(15);
        assertNotNull(TAG + " Solution must be available for Level 1", solution);
        assertTrue(TAG + " Solution must have moves", solution.getMoves().size() > 0);
        step("2/7", "Solution ready: " + solution.getMoves().size() + " moves");

        step("3/7", "Playing all solution moves to complete Level 1");
        playAllMoves(solution);
        Thread.sleep(3000);

        step("4/7", "Verifying Level 1 is complete");
        assertTrue(TAG + " Game must be complete after solution",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));

        step("5/7", "Verifying history entry for Level 1 has correct data");
        List<GameHistoryEntry> entries = getHistoryEntries();
        assertFalse(TAG + " History must have at least one entry after Level 1", entries.isEmpty());

        GameHistoryEntry level1Entry = null;
        for (GameHistoryEntry e : entries) {
            if (e.getMapName() != null && e.getMapName().matches("(?i)Level 1")) {
                level1Entry = e;
                break;
            }
        }
        assertNotNull(TAG + " History must have an entry for 'Level 1'", level1Entry);
        step("5/7", "History entry: completionCount=" + level1Entry.getCompletionCount()
                + ", boardSize=" + level1Entry.getBoardSize()
                + ", bestMoves=" + level1Entry.getBestMoves());

        assertEquals(TAG + " completionCount must be 1 after first completion",
                1, level1Entry.getCompletionCount());
        assertTrue(TAG + " bestMoves must be > 0 after completion (was 0 before fix)",
                level1Entry.getBestMoves() > 0);
        assertNotNull(TAG + " boardSize must not be null", level1Entry.getBoardSize());
        assertFalse(TAG + " boardSize must not be empty", level1Entry.getBoardSize().isEmpty());
        assertFalse(TAG + " boardSize must not be default 16x16",
                "16x16".equals(level1Entry.getBoardSize()));

        step("6/7", "Navigating to LevelSelectionFragment");
        navigateToLevelSelectionFragment();
        Thread.sleep(3000);

        step("7/7", "Verifying minimap and info button visible for Level 1 (position 1 in RecyclerView)");
        // Position 0 = "Standard Levels" header, position 1 = Level 1
        verifyMinimapVisibleAtPosition(1);
        verifyInfoButtonVisibleAtPosition(1);

        step("7/7", "Clicking info button on Level 1");
        onView(withId(R.id.level_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1,
                        new ClickChildViewWithId(R.id.level_info_button)));
        Thread.sleep(1000);

        step("7/7", "Verifying popup shows Completions: 1 and Best moves > 0");
        onView(withText(containsString("Completions: 1"))).check(matches(isDisplayed()));
        onView(withText(containsString("Best moves:"))).check(matches(isDisplayed()));

        step("PASS", "testLevel1MinimapAndInfoButtonVisibleAfterCompletion PASSED");
    }

    // ==================== TEST 2 ====================

    /**
     * Verifies that Level 1 with NO history shows level_minimap_view as GONE
     * and level_info_button as GONE (clean state).
     */
    @Test
    public void testLevel1WithoutHistoryShowsNoMinimapNoInfoButton() throws InterruptedException {
        step("1/3", "Ensuring history is empty");
        List<GameHistoryEntry> entries = getHistoryEntries();
        assertTrue(TAG + " History must be empty at start", entries.isEmpty());

        step("2/3", "Navigating to LevelSelectionFragment");
        navigateToLevelSelectionFragment();
        Thread.sleep(3000);

        step("3/3", "Verifying minimap is GONE and info button is GONE for Level 1 (no history)");
        verifyMinimapGoneAtPosition(1);
        verifyInfoButtonGoneAtPosition(1);

        step("PASS", "testLevel1WithoutHistoryShowsNoMinimapNoInfoButton PASSED");
    }

    // ==================== TEST 3 ====================

    /**
     * Verifies boardSize in history is the actual board dimensions, not the hardcoded 16x16.
     * Level 1 uses an 8x8 board (or whatever its actual size is) — must not be "16x16".
     */
    @Test
    public void testHistoryEntryBoardSizeNotHardcoded16x16() throws InterruptedException {
        step("1/4", "Loading and completing Level 1");
        loadLevel(1);
        Thread.sleep(5000);

        GameSolution solution = waitForSolution(15);
        assertNotNull(TAG + " Solution must be available", solution);
        playAllMoves(solution);
        Thread.sleep(3000);

        assertTrue(TAG + " Game must be complete", Boolean.TRUE.equals(
                gameStateManager.isGameComplete().getValue()));

        step("2/4", "Checking boardSize in history");
        List<GameHistoryEntry> entries = getHistoryEntries();
        assertFalse(TAG + " History must not be empty", entries.isEmpty());

        GameHistoryEntry entry = entries.get(0);
        String boardSize = entry.getBoardSize();
        step("3/4", "boardSize in history: '" + boardSize + "'");

        assertNotNull(TAG + " boardSize must not be null", boardSize);
        assertFalse(TAG + " boardSize must not be empty", boardSize.isEmpty());
        assertFalse(TAG + " boardSize must NOT be hardcoded 16x16", "16x16".equals(boardSize));

        step("4/4", "Verifying boardSize matches actual GameState dimensions");
        AtomicReference<String> actualSize = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                actualSize.set(state.getWidth() + "x" + state.getHeight());
            }
        });
        if (actualSize.get() != null) {
            assertEquals(TAG + " boardSize in history must match actual GameState dimensions",
                    actualSize.get(), boardSize);
        }

        step("PASS", "testHistoryEntryBoardSizeNotHardcoded16x16 PASSED");
    }

    // ==================== HELPERS ====================

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
    }

    private void loadLevel(int levelId) {
        activityRule.getScenario().onActivity(a -> {
            gameStateManager.startLevelGame(levelId);
            roboyard.ui.fragments.GameFragment gameFragment = new roboyard.ui.fragments.GameFragment();
            a.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, gameFragment)
                    .commit();
        });
    }

    private void navigateToLevelSelectionFragment() {
        activityRule.getScenario().onActivity(a -> {
            LevelSelectionFragment fragment = new LevelSelectionFragment();
            a.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit();
        });
    }

    private GameSolution waitForSolution(int maxAttempts) throws InterruptedException {
        for (int i = 0; i < maxAttempts; i++) {
            GameSolution s = gameStateManager.getCurrentSolution();
            if (s != null && !s.getMoves().isEmpty()) return s;
            step("solver", "Waiting... attempt " + (i + 1) + "/" + maxAttempts);
            Thread.sleep(2000);
        }
        return gameStateManager.getCurrentSolution();
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

    private void executeMove(IGameMove move) {
        activityRule.getScenario().onActivity(a -> {
            if (!(move instanceof RRGameMove)) return;
            RRGameMove rrMove = (RRGameMove) move;
            int dx = 0, dy = 0;
            switch (rrMove.getDirection()) {
                case 1: dy = -1; break;
                case 2: dx =  1; break;
                case 4: dy =  1; break;
                case 8: dx = -1; break;
            }
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) return;
            for (GameElement el : state.getGameElements()) {
                if (el.getType() == Constants.TYPE_ROBOT && el.getColor() == rrMove.getColor()) {
                    state.setSelectedRobot(el);
                    break;
                }
            }
            gameStateManager.moveRobotInDirection(dx, dy);
        });
    }

    /** Verifies that level_minimap_view at RecyclerView position is VISIBLE with a bitmap. */
    private void verifyMinimapVisibleAtPosition(int position) {
        AtomicBoolean visible = new AtomicBoolean(false);
        AtomicBoolean hasBitmap = new AtomicBoolean(false);
        activityRule.getScenario().onActivity(a -> {
            RecyclerView rv = a.findViewById(R.id.level_recycler_view);
            if (rv == null) {
                Timber.e(TAG + " level_recycler_view not found");
                return;
            }
            RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
            if (vh == null) {
                Timber.e(TAG + " ViewHolder at position %d not found", position);
                return;
            }
            ImageView minimap = vh.itemView.findViewById(R.id.level_minimap_view);
            if (minimap != null) {
                visible.set(minimap.getVisibility() == View.VISIBLE);
                hasBitmap.set(minimap.getDrawable() != null);
                step("verify", "minimap visibility=" + minimap.getVisibility()
                        + " drawable=" + minimap.getDrawable());
            } else {
                Timber.e(TAG + " level_minimap_view not found in item at position %d", position);
            }
        });
        assertTrue(TAG + " level_minimap_view must be VISIBLE at position " + position, visible.get());
        assertTrue(TAG + " level_minimap_view must have a bitmap at position " + position, hasBitmap.get());
    }

    /** Verifies that level_minimap_view at RecyclerView position is GONE. */
    private void verifyMinimapGoneAtPosition(int position) {
        AtomicBoolean gone = new AtomicBoolean(false);
        activityRule.getScenario().onActivity(a -> {
            RecyclerView rv = a.findViewById(R.id.level_recycler_view);
            if (rv == null) return;
            RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
            if (vh == null) return;
            ImageView minimap = vh.itemView.findViewById(R.id.level_minimap_view);
            if (minimap != null) {
                gone.set(minimap.getVisibility() == View.GONE);
            }
        });
        assertTrue(TAG + " level_minimap_view must be GONE at position " + position
                + " when no history exists", gone.get());
    }

    /** Verifies that level_info_button at RecyclerView position is VISIBLE. */
    private void verifyInfoButtonVisibleAtPosition(int position) {
        AtomicBoolean visible = new AtomicBoolean(false);
        activityRule.getScenario().onActivity(a -> {
            RecyclerView rv = a.findViewById(R.id.level_recycler_view);
            if (rv == null) return;
            RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
            if (vh == null) return;
            ImageButton btn = vh.itemView.findViewById(R.id.level_info_button);
            if (btn != null) {
                visible.set(btn.getVisibility() == View.VISIBLE);
                step("verify", "info_button visibility=" + btn.getVisibility());
            }
        });
        assertTrue(TAG + " level_info_button must be VISIBLE at position " + position, visible.get());
    }

    /** Verifies that level_info_button at RecyclerView position is GONE. */
    private void verifyInfoButtonGoneAtPosition(int position) {
        AtomicBoolean gone = new AtomicBoolean(false);
        activityRule.getScenario().onActivity(a -> {
            RecyclerView rv = a.findViewById(R.id.level_recycler_view);
            if (rv == null) return;
            RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
            if (vh == null) return;
            ImageButton btn = vh.itemView.findViewById(R.id.level_info_button);
            if (btn != null) {
                gone.set(btn.getVisibility() == View.GONE);
            }
        });
        assertTrue(TAG + " level_info_button must be GONE at position " + position
                + " when no history exists", gone.get());
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
        public org.hamcrest.Matcher<View> getConstraints() {
            return androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(View.class);
        }

        @Override
        public String getDescription() { return "Click child view with id " + viewId; }

        @Override
        public void perform(androidx.test.espresso.UiController uiController, View view) {
            View child = view.findViewById(viewId);
            if (child != null) child.performClick();
        }
    }
}

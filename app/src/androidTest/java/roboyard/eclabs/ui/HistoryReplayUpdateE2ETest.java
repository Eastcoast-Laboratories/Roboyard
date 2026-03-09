package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import android.app.Activity;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
 * E2E test: Replay a random game from history with fewer moves → bestMoves updates.
 *
 * 1. Play a random game with extra moves (non-optimal)
 * 2. Verify history entry has movesMade > optimal
 * 3. Load game from history
 * 4. Play optimal solution
 * 5. Verify bestMoves was updated to optimal
 * 6. Verify completionCount incremented to 2
 *
 * Tags: e2e, history, random-game, replay, bestMoves, completion-tracking, espresso
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.HistoryReplayUpdateE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class HistoryReplayUpdateE2ETest {

    private static final String TAG = "[HISTORY_REPLAY_E2E]";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        step("SETUP", "Initializing test");
        activityRule.getScenario().onActivity(a -> {
            gameStateManager = ((MainActivity) a).getGameStateManager();
        });
        assertNotNull("GameStateManager must not be null", gameStateManager);

        clearAllHistory();
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(2000);
    }

    @Test
    public void testReplayFromHistoryUpdatesBestMoves() throws InterruptedException {
        // === STEP 1: Play random game with extra (non-optimal) moves ===
        step("1/5", "Starting random game");
        onView(withId(R.id.ui_button)).perform(click());
        Thread.sleep(5000);

        step("1/5", "Waiting for AI solution");
        GameSolution solution = waitForSolution(30);
        assertNotNull(TAG + " Solution must be available", solution);
        assertTrue(TAG + " Solution must have moves", solution.getMoves().size() > 0);
        int optimalMoves = solution.getMoves().size();
        step("1/5", "Optimal solution has " + optimalMoves + " moves");

        // Make an extra move: move the first robot in the solution back and forth
        // to add non-optimal moves before playing the solution
        step("1/5", "Making extra non-optimal moves first");
        IGameMove firstMove = solution.getMoves().get(0);
        RRGameMove rrFirst = (RRGameMove) firstMove;

        // Move a different robot (or same robot in opposite direction) to add extra moves
        // We'll move the first robot in the opposite direction, then back
        int oppositeDir = getOppositeDirection(rrFirst.getDirection());
        executeRawMove(rrFirst.getColor(), oppositeDir);
        Thread.sleep(800);
        // Move it back (towards original position)
        executeRawMove(rrFirst.getColor(), rrFirst.getDirection());
        Thread.sleep(800);

        int extraMovesBefore = 2;
        step("1/5", "Added " + extraMovesBefore + " extra moves, now playing optimal solution");

        // Now play the actual optimal solution
        playAllMoves(solution);
        Thread.sleep(3000);

        assertTrue(TAG + " Game must be complete",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));
        step("1/5", "PASS: Game completed with extra moves");

        // === STEP 2: Verify history entry has non-optimal movesMade ===
        Thread.sleep(2000);
        List<GameHistoryEntry> entriesAfterFirst = getHistoryEntries();
        assertNotNull("History entries must not be null", entriesAfterFirst);
        assertFalse("History must have at least one entry", entriesAfterFirst.isEmpty());

        GameHistoryEntry firstEntry = entriesAfterFirst.get(0);
        String mapPath = firstEntry.getMapPath();
        int firstMovesMade = firstEntry.getMovesMade();
        int firstBestMoves = firstEntry.getBestMoves();
        int firstCompletionCount = firstEntry.getCompletionCount();
        int firstOptimalMoves = firstEntry.getOptimalMoves();

        step("2/5", "After 1st play: movesMade=" + firstMovesMade +
                ", bestMoves=" + firstBestMoves +
                ", completionCount=" + firstCompletionCount +
                ", optimalMoves=" + firstOptimalMoves);

        // movesMade should be > optimalMoves because of extra moves
        assertTrue(TAG + " movesMade (" + firstMovesMade + ") must be > optimal (" + optimalMoves + ") after extra moves",
                firstMovesMade > optimalMoves);
        assertEquals(TAG + " completionCount must be 1", 1, firstCompletionCount);
        step("2/5", "PASS: First completion has non-optimal moves");

        // === STEP 3: Load game from history and replay ===
        step("3/5", "Loading game from history: " + mapPath);
        activityRule.getScenario().onActivity(a -> {
            gameStateManager.loadHistoryEntry(mapPath);
        });
        Thread.sleep(3000);

        // Wait for solver to find solution for replayed game
        step("3/5", "Waiting for AI solution on replayed game");
        GameSolution replaySolution = waitForSolution(30);
        assertNotNull(TAG + " Replay solution must be available", replaySolution);
        int replayOptimalMoves = replaySolution.getMoves().size();
        step("3/5", "Replay solution has " + replayOptimalMoves + " moves");

        // === STEP 4: Play optimal solution on replay ===
        step("4/5", "Playing optimal solution on replayed game");
        playAllMoves(replaySolution);
        Thread.sleep(3000);

        assertTrue(TAG + " Replayed game must be complete",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));
        step("4/5", "PASS: Replay completed with optimal moves");

        // === STEP 5: Verify bestMoves updated ===
        Thread.sleep(2000);
        List<GameHistoryEntry> entriesAfterReplay = getHistoryEntries();
        assertNotNull("History entries must not be null after replay", entriesAfterReplay);
        assertFalse("History must have entries after replay", entriesAfterReplay.isEmpty());

        // Find the same entry by mapPath
        GameHistoryEntry replayEntry = null;
        for (GameHistoryEntry e : entriesAfterReplay) {
            if (e.getMapPath() != null && e.getMapPath().equals(mapPath)) {
                replayEntry = e;
                break;
            }
        }
        // Fallback: use first entry
        if (replayEntry == null) {
            replayEntry = entriesAfterReplay.get(0);
        }

        int replayMovesMade = replayEntry.getMovesMade();
        int replayBestMoves = replayEntry.getBestMoves();
        int replayCompletionCount = replayEntry.getCompletionCount();

        step("5/5", "After replay: movesMade=" + replayMovesMade +
                ", bestMoves=" + replayBestMoves +
                ", completionCount=" + replayCompletionCount +
                ", optimalMoves=" + replayEntry.getOptimalMoves());

        // bestMoves should now be the optimal move count (from replay)
        assertEquals(TAG + " bestMoves must be updated to optimal after replay",
                replayOptimalMoves, replayBestMoves);
        // completionCount should be 2 (first play + replay)
        assertEquals(TAG + " completionCount must be 2 after replay",
                2, replayCompletionCount);
        // movesMade should reflect the latest completion (optimal)
        assertEquals(TAG + " movesMade must reflect latest completion",
                replayOptimalMoves, replayMovesMade);

        step("PASS", "testReplayFromHistoryUpdatesBestMoves PASSED - bestMoves updated correctly");
    }

    // ==================== HELPERS ====================

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
    }

    private int getOppositeDirection(int direction) {
        switch (direction) {
            case 1: return 4; // UP → DOWN
            case 2: return 8; // RIGHT → LEFT
            case 4: return 1; // DOWN → UP
            case 8: return 2; // LEFT → RIGHT
            default: return direction;
        }
    }

    private void executeRawMove(int robotColor, int direction) {
        activityRule.getScenario().onActivity(a -> {
            int dx = 0, dy = 0;
            switch (direction) {
                case 1: dy = -1; break;
                case 2: dx =  1; break;
                case 4: dy =  1; break;
                case 8: dx = -1; break;
            }
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) return;
            for (GameElement el : state.getGameElements()) {
                if (el.getType() == Constants.TYPE_ROBOT && el.getColor() == robotColor) {
                    state.setSelectedRobot(el);
                    break;
                }
            }
            gameStateManager.moveRobotInDirection(dx, dy);
        });
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
}

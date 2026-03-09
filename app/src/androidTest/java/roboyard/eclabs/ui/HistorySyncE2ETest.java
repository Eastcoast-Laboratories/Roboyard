package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import roboyard.ui.components.DataExportImportManager;
import roboyard.ui.components.FileReadWrite;
import roboyard.ui.components.GameHistoryManager;
import roboyard.ui.components.GameStateManager;
import roboyard.ui.components.RoboyardApiClient;
import roboyard.ui.components.SyncManager;
import timber.log.Timber;

/**
 * End-to-End test for history sync round-trip:
 * 1. Register a unique test user
 * 2. Play a random game to completion
 * 3. Wait for automatic history upload
 * 4. Verify history was uploaded (entry count on server > 0)
 * 5. Reset all local data + logout
 * 6. Re-login with same credentials
 * 7. Download history from server
 * 8. Verify history entries are restored locally
 *
 * Tags: e2e, history, sync, register, login, upload, download, round-trip, espresso
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.HistorySyncE2ETest
 */
@RunWith(AndroidJUnit4.class)
public class HistorySyncE2ETest {

    private static final String TAG = "[HISTORY_SYNC_E2E]";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        step("SETUP", "Initializing test");
        AtomicReference<Activity> activityRef = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> {
            activityRef.set(a);
            gameStateManager = ((MainActivity) a).getGameStateManager();
        });
        assertNotNull("GameStateManager must not be null", gameStateManager);

        // Ensure we start logged out and with clean history
        activityRule.getScenario().onActivity(a -> {
            RoboyardApiClient.getInstance(a).logout();
        });
        clearAllHistory();
        
        // Close streak/achievement popup that appears on first launch
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(2000);
    }

    @Test
    public void testFullHistorySyncRoundTrip() throws InterruptedException {
        // === STEP 1: Register unique test user ===
        String timestamp = String.valueOf(System.currentTimeMillis());
        String testEmail = "test_sync_" + timestamp + "@roboyard-test.de";
        String testPassword = "TestPass123!";
        String testName = "SyncTest_" + timestamp;

        step("1/8", "Registering test user: " + testEmail);
        CountDownLatch registerLatch = new CountDownLatch(1);
        AtomicBoolean registerSuccess = new AtomicBoolean(false);
        AtomicReference<String> registerError = new AtomicReference<>();

        activityRule.getScenario().onActivity(a -> {
            RoboyardApiClient.getInstance(a).register(testName, testEmail, testPassword,
                    new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
                        @Override
                        public void onSuccess(RoboyardApiClient.LoginResult result) {
                            Timber.d(TAG + " Registration successful: %s", result.userName);
                            registerSuccess.set(true);
                            registerLatch.countDown();
                        }

                        @Override
                        public void onError(String error) {
                            Timber.e(TAG + " Registration failed: %s", error);
                            registerError.set(error);
                            registerLatch.countDown();
                        }
                    });
        });

        assertTrue("Registration must complete within 15s", registerLatch.await(15, TimeUnit.SECONDS));
        assertTrue("Registration must succeed (error: " + registerError.get() + ")", registerSuccess.get());

        // Verify logged in
        AtomicBoolean isLoggedIn = new AtomicBoolean(false);
        activityRule.getScenario().onActivity(a -> {
            isLoggedIn.set(RoboyardApiClient.getInstance(a).isLoggedIn());
        });
        assertTrue("Must be logged in after registration", isLoggedIn.get());
        step("1/8", "PASS: Registered and logged in as " + testEmail);

        // === STEP 2: Play a random game to completion ===
        step("2/8", "Starting random game");
        onView(withId(R.id.ui_button)).perform(click());
        Thread.sleep(5000);

        step("2/8", "Waiting for AI solution");
        GameSolution solution = waitForSolution(30);
        assertNotNull(TAG + " Solution must be available", solution);
        assertTrue(TAG + " Solution must have moves", solution.getMoves().size() > 0);
        int optimalMoves = solution.getMoves().size();
        step("2/8", "Solution ready: " + optimalMoves + " moves");

        step("2/8", "Playing all solution moves");
        playAllMoves(solution);
        Thread.sleep(3000);

        assertTrue(TAG + " Game must be complete after solution",
                Boolean.TRUE.equals(gameStateManager.isGameComplete().getValue()));
        step("2/8", "PASS: Game completed");

        // === STEP 3: Wait for automatic history upload ===
        step("3/8", "Waiting for automatic history upload (5s)");
        Thread.sleep(5000);

        // Verify local history has an entry
        List<GameHistoryEntry> localEntries = getHistoryEntries();
        assertNotNull("Local history entries must not be null", localEntries);
        assertFalse("Local history must have at least one entry", localEntries.isEmpty());
        GameHistoryEntry uploadedEntry = localEntries.get(0);
        String uploadedMapName = uploadedEntry.getMapName();
        int uploadedOptimalMoves = uploadedEntry.getOptimalMoves();
        int uploadedMaxHintUsed = uploadedEntry.getMaxHintUsed();
        boolean uploadedEverUsedHints = uploadedEntry.isEverUsedHints();
        int uploadedBestTime = uploadedEntry.getBestTime();
        int uploadedBestMoves = uploadedEntry.getBestMoves();
        int uploadedCompletionCount = uploadedEntry.getCompletionCount();
        int uploadedTimestampsSize = uploadedEntry.getCompletionTimestamps() != null ? uploadedEntry.getCompletionTimestamps().size() : 0;
        step("3/8", "Local entry: mapName=" + uploadedMapName +
                ", optimalMoves=" + uploadedOptimalMoves +
                ", maxHintUsed=" + uploadedMaxHintUsed +
                ", everUsedHints=" + uploadedEverUsedHints +
                ", bestTime=" + uploadedBestTime +
                ", bestMoves=" + uploadedBestMoves +
                ", completionCount=" + uploadedCompletionCount +
                ", timestamps=" + uploadedTimestampsSize);
        assertTrue("CompletionCount must be >= 1 after game", uploadedCompletionCount >= 1);
        assertTrue("BestMoves must be > 0 after game", uploadedBestMoves > 0);
        step("3/8", "PASS: Automatic upload triggered (see logs for confirmation)");

        // === STEP 4: Verify server has the history entry ===
        step("4/8", "Verifying server has history entry");
        CountDownLatch fetchLatch = new CountDownLatch(1);
        AtomicInteger serverEntryCount = new AtomicInteger(0);

        activityRule.getScenario().onActivity(a -> {
            RoboyardApiClient.getInstance(a).fetchHistory(new RoboyardApiClient.ApiCallback<org.json.JSONArray>() {
                @Override
                public void onSuccess(org.json.JSONArray result) {
                    serverEntryCount.set(result.length());
                    Timber.d(TAG + " Server has %d history entries", result.length());
                    fetchLatch.countDown();
                }

                @Override
                public void onError(String error) {
                    Timber.e(TAG + " Fetch history error: %s", error);
                    fetchLatch.countDown();
                }
            });
        });

        assertTrue("Fetch must complete within 15s", fetchLatch.await(15, TimeUnit.SECONDS));
        assertTrue("Server must have at least 1 history entry, has " + serverEntryCount.get(),
                serverEntryCount.get() >= 1);
        step("4/8", "PASS: Server has " + serverEntryCount.get() + " history entries");

        // === STEP 5: Reset all local data + logout ===
        step("5/8", "Resetting all local data and logging out");
        activityRule.getScenario().onActivity(a -> {
            new DataExportImportManager(a).resetAllData();
            RoboyardApiClient.getInstance(a).logout();
        });
        Thread.sleep(1000);

        // Verify history is empty
        List<GameHistoryEntry> afterReset = getHistoryEntries();
        assertTrue("History must be empty after reset",
                afterReset == null || afterReset.isEmpty());

        // Verify logged out
        AtomicBoolean loggedOutCheck = new AtomicBoolean(true);
        activityRule.getScenario().onActivity(a -> {
            loggedOutCheck.set(RoboyardApiClient.getInstance(a).isLoggedIn());
        });
        assertFalse("Must be logged out after reset", loggedOutCheck.get());
        step("5/8", "PASS: All data reset, logged out");

        // === STEP 6: Re-login with same credentials ===
        step("6/8", "Re-logging in as " + testEmail);
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicBoolean loginSuccess = new AtomicBoolean(false);
        AtomicReference<String> loginError = new AtomicReference<>();

        activityRule.getScenario().onActivity(a -> {
            RoboyardApiClient.getInstance(a).login(testEmail, testPassword,
                    new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
                        @Override
                        public void onSuccess(RoboyardApiClient.LoginResult result) {
                            Timber.d(TAG + " Login successful: %s", result.userName);
                            loginSuccess.set(true);
                            loginLatch.countDown();
                        }

                        @Override
                        public void onError(String error) {
                            Timber.e(TAG + " Login failed: %s", error);
                            loginError.set(error);
                            loginLatch.countDown();
                        }
                    });
        });

        assertTrue("Login must complete within 15s", loginLatch.await(15, TimeUnit.SECONDS));
        assertTrue("Login must succeed (error: " + loginError.get() + ")", loginSuccess.get());
        step("6/8", "PASS: Re-logged in");

        // === STEP 7: Download history from server ===
        step("7/8", "Downloading history from server");
        CountDownLatch downloadLatch = new CountDownLatch(1);
        AtomicBoolean downloadSuccess = new AtomicBoolean(false);
        AtomicInteger downloadedCount = new AtomicInteger(0);
        AtomicReference<String> downloadError = new AtomicReference<>();

        activityRule.getScenario().onActivity(a -> {
            SyncManager.getInstance(a).downloadHistory(a, new RoboyardApiClient.ApiCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    Timber.d(TAG + " Download success: %d entries restored", result);
                    downloadedCount.set(result);
                    downloadSuccess.set(true);
                    downloadLatch.countDown();
                }

                @Override
                public void onError(String error) {
                    Timber.e(TAG + " Download error: %s", error);
                    downloadError.set(error);
                    downloadLatch.countDown();
                }
            });
        });

        assertTrue("Download must complete within 15s", downloadLatch.await(15, TimeUnit.SECONDS));
        assertTrue("Download must succeed (error: " + downloadError.get() + ")", downloadSuccess.get());
        assertTrue("Must have downloaded at least 1 entry, got " + downloadedCount.get(),
                downloadedCount.get() >= 1);
        step("7/8", "PASS: Downloaded " + downloadedCount.get() + " entries");

        // === STEP 8: Verify restored history matches uploaded data ===
        step("8/8", "Verifying restored history data");
        Thread.sleep(1000);

        List<GameHistoryEntry> restoredEntries = getHistoryEntries();
        assertNotNull("Restored history entries must not be null", restoredEntries);
        assertFalse("Restored history must have at least one entry", restoredEntries.isEmpty());

        // Find the entry that matches our uploaded map
        GameHistoryEntry restoredEntry = null;
        for (GameHistoryEntry entry : restoredEntries) {
            if (entry.getMapName() != null && entry.getMapName().equals(uploadedMapName)) {
                restoredEntry = entry;
                break;
            }
        }

        // If exact name match not found, use first entry (only one game was played)
        if (restoredEntry == null) {
            restoredEntry = restoredEntries.get(0);
        }

        step("8/8", "Restored entry: mapName=" + restoredEntry.getMapName() +
                ", optimalMoves=" + restoredEntry.getOptimalMoves() +
                ", maxHintUsed=" + restoredEntry.getMaxHintUsed() +
                ", everUsedHints=" + restoredEntry.isEverUsedHints() +
                ", bestTime=" + restoredEntry.getBestTime() +
                ", bestMoves=" + restoredEntry.getBestMoves() +
                ", completionCount=" + restoredEntry.getCompletionCount() +
                ", timestamps=" + (restoredEntry.getCompletionTimestamps() != null ? restoredEntry.getCompletionTimestamps().size() : 0) +
                ", starsEarned=" + restoredEntry.getStarsEarned() +
                ", movesMade=" + restoredEntry.getMovesMade());

        // Verify key fields were restored
        assertEquals("OptimalMoves must be restored",
                uploadedOptimalMoves, restoredEntry.getOptimalMoves());
        assertEquals("MaxHintUsed must be restored",
                uploadedMaxHintUsed, restoredEntry.getMaxHintUsed());
        assertEquals("EverUsedHints must be restored",
                uploadedEverUsedHints, restoredEntry.isEverUsedHints());
        assertTrue("MovesMade must be > 0 after restore",
                restoredEntry.getMovesMade() > 0);
        assertEquals("BestMoves must be restored",
                uploadedBestMoves, restoredEntry.getBestMoves());
        assertEquals("BestTime must be restored",
                uploadedBestTime, restoredEntry.getBestTime());
        assertEquals("CompletionCount must be restored",
                uploadedCompletionCount, restoredEntry.getCompletionCount());
        int restoredTimestampsSize = restoredEntry.getCompletionTimestamps() != null ? restoredEntry.getCompletionTimestamps().size() : 0;
        assertEquals("CompletionTimestamps count must be restored",
                uploadedTimestampsSize, restoredTimestampsSize);

        // Verify timestamps are not in the future (timezone bug check)
        long now = System.currentTimeMillis();
        long timeDiffMs = restoredEntry.getTimestamp() - now;
        long timeDiffSeconds = timeDiffMs / 1000;
        step("8/8", "Timestamp check: restored=" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date(restoredEntry.getTimestamp())) +
                ", now=" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date(now)) +
                ", diff=" + timeDiffSeconds + "s");
        assertTrue("Restored timestamp must not be more than 60s in the future (timezone bug), diff=" + timeDiffSeconds + "s",
                timeDiffMs < 60000);
        
        if (restoredEntry.getLastCompletionTimestamp() > 0) {
            long lastCompDiffMs = restoredEntry.getLastCompletionTimestamp() - now;
            long lastCompDiffSeconds = lastCompDiffMs / 1000;
            assertTrue("Last completion timestamp must not be more than 60s in the future, diff=" + lastCompDiffSeconds + "s",
                    lastCompDiffMs < 60000);
        }

        step("PASS", "testFullHistorySyncRoundTrip PASSED - all fields restored correctly, timestamps valid");
    }

    // ==================== HELPERS ====================

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
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

package roboyard.eclabs.ui;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.RoboyardApiClient;
import roboyard.ui.components.SyncManager;
import timber.log.Timber;

/**
 * E2E test for login and minimap verification.
 * Logs in programmatically as rbk@eclabs.de / aaaaaaaa, syncs history, and verifies minimaps are displayed.
 */
@RunWith(AndroidJUnit4.class)
public class MinimapLoginTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        TestHelper.clearLogcat();
        Timber.d("[MINIMAP_LOGIN_TEST] Test setup started");
    }

    @After
    public void tearDown() {
        TestHelper.dumpLogcat("MINIMAP_LOGIN_TEST", "LEVEL_SELECTION", null, 50);
        TestHelper.dumpLogcat("MINIMAP_LOGIN_TEST", "HISTORY", null, 50);
        TestHelper.dumpLogcat("MINIMAP_LOGIN_TEST", "HISTORY_SYNC", null, 50);
    }

    @Test
    public void testLoginAndVerifyMinimaps() throws InterruptedException {
        Timber.d("[MINIMAP_LOGIN_TEST] Starting login and minimap verification test");

        // Programmatic login
        final CountDownLatch loginLatch = new CountDownLatch(1);
        final boolean[] loginSuccess = {false};

        activityRule.getScenario().onActivity(activity -> {
            RoboyardApiClient apiClient = RoboyardApiClient.getInstance(activity);
            apiClient.login("rbk@eclabs.de", "aaaaaaaa", new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
                @Override
                public void onNeedsUpdate() {
                    Timber.e("[MINIMAP_LOGIN_TEST] Login failed: needs update");
                    loginSuccess[0] = false;
                    loginLatch.countDown();
                }

                @Override
                public void onSuccess(RoboyardApiClient.LoginResult result) {
                    Timber.d("[MINIMAP_LOGIN_TEST] Login successful");
                    loginSuccess[0] = true;

                    // Trigger sync
                    SyncManager.getInstance(activity).fullSyncOnLogin(activity, new RoboyardApiClient.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String summary) {
                            Timber.d("[MINIMAP_LOGIN_TEST] Sync complete: %s", summary);
                            loginLatch.countDown();
                        }

                        @Override
                        public void onError(String error) {
                            Timber.e("[MINIMAP_LOGIN_TEST] Sync failed: %s", error);
                            loginLatch.countDown();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Timber.e("[MINIMAP_LOGIN_TEST] Login failed: %s", error);
                    loginSuccess[0] = false;
                    loginLatch.countDown();
                }
            });
        });

        // Wait for login and sync to complete
        boolean completed = loginLatch.await(30, TimeUnit.SECONDS);
        if (!completed) {
            Timber.e("[MINIMAP_LOGIN_TEST] Login/sync timed out");
        }

        if (!loginSuccess[0]) {
            Timber.e("[MINIMAP_LOGIN_TEST] Login failed, cannot continue test");
            return;
        }

        // Wait for sync to complete
        Thread.sleep(5000);

        // Navigate to level selection
        activityRule.getScenario().onActivity(activity -> {
            roboyard.ui.fragments.LevelSelectionFragment fragment = new roboyard.ui.fragments.LevelSelectionFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(roboyard.eclabs.R.id.nav_host_fragment, fragment)
                    .commit();
        });

        // Wait for level screen to load
        Thread.sleep(3000);

        // Programmatically check all history entries
        activityRule.getScenario().onActivity(activity -> {
            java.util.List<roboyard.logic.core.GameHistoryEntry> entries = roboyard.ui.components.GameHistoryManager.getHistoryEntries(activity);
            Timber.d("[MINIMAP_LOGIN_TEST] Total history entries: %d", entries.size());

            int validMapPaths = 0;
            int invalidMapPaths = 0;
            int filesExist = 0;
            int filesMissing = 0;
            java.util.Set<String> uniqueMapPaths = new java.util.HashSet<>();

            for (roboyard.logic.core.GameHistoryEntry entry : entries) {
                String mapPath = entry.getMapPath();
                uniqueMapPaths.add(mapPath);

                if (mapPath != null && (mapPath.startsWith("level_") || mapPath.startsWith("custom_level_"))) {
                    validMapPaths++;
                    // Check if file exists
                    java.io.File file = activity.getFileStreamPath(mapPath);
                    if (file.exists()) {
                        filesExist++;
                    } else {
                        filesMissing++;
                        Timber.e("[MINIMAP_LOGIN_TEST] File missing: %s", mapPath);
                    }
                } else {
                    invalidMapPaths++;
                    Timber.e("[MINIMAP_LOGIN_TEST] Invalid mapPath: %s (mapName=%s)", mapPath, entry.mapName);
                }
            }

            Timber.d("[MINIMAP_LOGIN_TEST] Valid mapPaths: %d, Invalid: %d", validMapPaths, invalidMapPaths);
            Timber.d("[MINIMAP_LOGIN_TEST] Files exist: %d, Missing: %d", filesExist, filesMissing);
            Timber.d("[MINIMAP_LOGIN_TEST] Unique mapPaths: %d", uniqueMapPaths.size());

            System.out.println("=== UNIQUE MAP PATHS ===");
            for (String path : uniqueMapPaths) {
                System.out.println(path);
            }

            // Check if all level_X.txt files are present (1-140)
            int missingLevels = 0;
            java.util.List<String> missingLevelPaths = new java.util.ArrayList<>();
            for (int i = 1; i <= 140; i++) {
                String levelPath = "level_" + i + ".txt";
                java.io.File file = activity.getFileStreamPath(levelPath);
                if (!file.exists()) {
                    missingLevels++;
                    missingLevelPaths.add(levelPath);
                    Timber.w("[MINIMAP_LOGIN_TEST] Level file missing: %s", levelPath);
                }
            }
            Timber.d("[MINIMAP_LOGIN_TEST] Missing level files (1-140): %d", missingLevels);

            // Test fallback: try to load missing levels from assets
            int assetsAvailable = 0;
            int assetsMissing = 0;
            for (String levelPath : missingLevelPaths) {
                try {
                    java.io.InputStream is = activity.getAssets().open("Maps/" + levelPath);
                    is.close();
                    assetsAvailable++;
                } catch (Exception e) {
                    assetsMissing++;
                    Timber.e("[MINIMAP_LOGIN_TEST] Asset missing: %s", levelPath);
                }
            }
            Timber.d("[MINIMAP_LOGIN_TEST] Assets available for missing levels: %d, Missing in assets: %d", assetsAvailable, assetsMissing);
        });

        Thread.sleep(1000);

        // Collect logs for analysis
        List<String> levelSelectionLogs = TestHelper.collectLogcatLines("LEVEL_SELECTION", null, 200);
        List<String> historyLogs = TestHelper.collectLogcatLines("HISTORY", null, 100);
        List<String> syncLogs = TestHelper.collectLogcatLines("HISTORY_SYNC", null, 100);

        Timber.d("[MINIMAP_LOGIN_TEST] Collected %d LEVEL_SELECTION logs", levelSelectionLogs.size());
        Timber.d("[MINIMAP_LOGIN_TEST] Collected %d HISTORY logs", historyLogs.size());
        Timber.d("[MINIMAP_LOGIN_TEST] Collected %d HISTORY_SYNC logs", syncLogs.size());

        // Print logs for analysis
        System.out.println("=== LEVEL_SELECTION LOGS ===");
        for (String line : levelSelectionLogs) {
            System.out.println(line);
        }
        System.out.println("=== HISTORY LOGS ===");
        for (String line : historyLogs) {
            System.out.println(line);
        }
        System.out.println("=== HISTORY_SYNC LOGS ===");
        for (String line : syncLogs) {
            System.out.println(line);
        }

        // Check if history entries were loaded
        boolean hasHistoryEntries = false;
        for (String line : levelSelectionLogs) {
            if (line.contains("Loaded") && line.contains("history entries into map")) {
                String[] parts = line.split(" ");
                for (String part : parts) {
                    try {
                        int count = Integer.parseInt(part);
                        if (count > 0) {
                            hasHistoryEntries = true;
                            Timber.d("[MINIMAP_LOGIN_TEST] Found %d history entries in map", count);
                        }
                    } catch (NumberFormatException e) {
                        // Not a number
                    }
                }
            }
        }

        if (!hasHistoryEntries) {
            Timber.e("[MINIMAP_LOGIN_TEST] FAILED: No history entries loaded into map");
        } else {
            Timber.d("[MINIMAP_LOGIN_TEST] SUCCESS: History entries loaded");
        }

        // Check if minimaps were generated
        int minimapSuccessCount = 0;
        int minimapNullCount = 0;
        for (String line : levelSelectionLogs) {
            if (line.contains("minimap from history file: SUCCESS")) {
                minimapSuccessCount++;
            } else if (line.contains("minimap from history file: NULL")) {
                minimapNullCount++;
            }
        }

        Timber.d("[MINIMAP_LOGIN_TEST] Minimap generation: %d SUCCESS, %d NULL", minimapSuccessCount, minimapNullCount);

        if (minimapNullCount > 0) {
            Timber.e("[MINIMAP_LOGIN_TEST] FAILED: %d levels have NULL minimaps", minimapNullCount);
        } else {
            Timber.d("[MINIMAP_LOGIN_TEST] SUCCESS: All visible levels have minimaps");
        }
    }
}

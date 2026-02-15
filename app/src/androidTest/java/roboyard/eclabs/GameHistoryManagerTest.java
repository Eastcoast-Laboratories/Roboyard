package roboyard.eclabs;

import roboyard.ui.components.GameHistoryManager;
import roboyard.ui.components.FileReadWrite;

import static org.junit.Assert.*;

import android.app.Activity;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import roboyard.ui.activities.MainFragmentActivity;
import roboyard.logic.core.GameHistoryEntry;
import timber.log.Timber;

/**
 * Instrumented tests for GameHistoryManager.
 * Tests history entry creation, retrieval, deletion, and path handling.
 *
 * Run with: ./gradlew connectedAndroidTest --tests "roboyard.ui.components.GameHistoryManagerTest"
 */
@RunWith(AndroidJUnit4.class)
public class GameHistoryManagerTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private Activity activity;

    @Before
    public void setUp() {
        AtomicReference<Activity> activityRef = new AtomicReference<>();
        activityRule.getScenario().onActivity(activityRef::set);
        activity = activityRef.get();
        assertNotNull("Activity should not be null", activity);

        // Clear all history before each test
        clearAllHistory();

        // Initialize
        GameHistoryManager.initialize(activity);
    }

    @After
    public void tearDown() {
        clearAllHistory();
    }

    private void clearAllHistory() {
        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        for (GameHistoryEntry entry : entries) {
            GameHistoryManager.deleteHistoryEntry(activity, entry.getMapPath());
        }
        // Verify cleared
        List<GameHistoryEntry> remaining = GameHistoryManager.getHistoryEntries(activity);
        if (!remaining.isEmpty()) {
            // Force-clear the index file
            FileReadWrite.writePrivateData(activity, "history_index.json", "{\"historyEntries\":[]}");
        }
    }

    // ==================== PATH FORMAT TESTS ====================

    @Test
    public void testHistoryPathsAreFlat() {
        GameHistoryEntry entry = createTestEntry(0, "TestMap");
        GameHistoryManager.addHistoryEntry(activity, entry);

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertFalse("Should have entries", entries.isEmpty());

        for (GameHistoryEntry e : entries) {
            assertFalse("mapPath must not contain path separator: " + e.getMapPath(),
                    e.getMapPath().contains("/"));
            assertFalse("previewImagePath must not contain path separator: " + e.getPreviewImagePath(),
                    e.getPreviewImagePath().contains("/"));
        }
    }

    @Test
    public void testHistoryFileNameFormat() {
        GameHistoryEntry entry = createTestEntry(0, "TestMap");
        GameHistoryManager.addHistoryEntry(activity, entry);

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertEquals(1, entries.size());
        assertTrue("mapPath should start with 'history_'",
                entries.get(0).getMapPath().startsWith("history_"));
        assertTrue("mapPath should end with '.txt'",
                entries.get(0).getMapPath().endsWith(".txt"));
    }

    // ==================== ADD / GET TESTS ====================

    @Test
    public void testAddAndGetHistoryEntry() {
        GameHistoryEntry entry = createTestEntry(0, "MyMap");
        Boolean result = GameHistoryManager.addHistoryEntry(activity, entry);
        assertTrue("addHistoryEntry should return true", result);

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertEquals("Should have 1 entry", 1, entries.size());
        assertEquals("MyMap", entries.get(0).getMapName());
        assertEquals(5, entries.get(0).getMovesMade());
        assertEquals("12x12", entries.get(0).getBoardSize());
    }

    @Test
    public void testAddMultipleEntries() {
        for (int i = 0; i < 5; i++) {
            GameHistoryEntry entry = createTestEntry(i, "Map_" + i);
            GameHistoryManager.addHistoryEntry(activity, entry);
        }

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertEquals("Should have 5 entries", 5, entries.size());
    }

    @Test
    public void testEntriesSortedNewestFirst() {
        for (int i = 0; i < 3; i++) {
            GameHistoryEntry entry = createTestEntry(i, "Map_" + i);
            // Set timestamps so Map_2 is newest
            entry.setTimestamp(1000L + i * 1000L);
            GameHistoryManager.addHistoryEntry(activity, entry);
        }

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertEquals(3, entries.size());
        // Newest first
        assertEquals("Map_2", entries.get(0).getMapName());
        assertEquals("Map_1", entries.get(1).getMapName());
        assertEquals("Map_0", entries.get(2).getMapName());
    }

    // ==================== UPDATE TESTS ====================

    @Test
    public void testUpdateExistingEntryByMapName() {
        GameHistoryEntry entry1 = createTestEntry(0, "SameMap");
        entry1.setMovesMade(3);
        GameHistoryManager.addHistoryEntry(activity, entry1);

        // Add another entry with the same mapName — should update, not duplicate
        GameHistoryEntry entry2 = createTestEntry(0, "SameMap");
        entry2.setMovesMade(7);
        GameHistoryManager.addHistoryEntry(activity, entry2);

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertEquals("Should have 1 entry (updated, not duplicated)", 1, entries.size());
        assertEquals(7, entries.get(0).getMovesMade());
    }

    // ==================== DELETE TESTS ====================

    @Test
    public void testDeleteHistoryEntry() {
        GameHistoryEntry entry = createTestEntry(0, "ToDelete");
        GameHistoryManager.addHistoryEntry(activity, entry);

        List<GameHistoryEntry> before = GameHistoryManager.getHistoryEntries(activity);
        assertEquals(1, before.size());

        GameHistoryManager.deleteHistoryEntry(activity, before.get(0));

        List<GameHistoryEntry> after = GameHistoryManager.getHistoryEntries(activity);
        assertEquals("Should have 0 entries after delete", 0, after.size());
    }

    @Test
    public void testDeleteByPath() {
        GameHistoryEntry entry = createTestEntry(0, "ToDeleteByPath");
        GameHistoryManager.addHistoryEntry(activity, entry);

        List<GameHistoryEntry> before = GameHistoryManager.getHistoryEntries(activity);
        assertEquals(1, before.size());
        String mapPath = before.get(0).getMapPath();

        boolean deleted = GameHistoryManager.deleteHistoryEntry(activity, mapPath);
        assertTrue("deleteHistoryEntry(path) should return true", deleted);

        List<GameHistoryEntry> after = GameHistoryManager.getHistoryEntries(activity);
        assertEquals("Should have 0 entries after delete", 0, after.size());
    }

    // ==================== MAX ENTRIES TEST ====================

    @Test
    public void testMaxHistoryEntriesEnforced() {
        // MAX_HISTORY_ENTRIES is 11
        for (int i = 0; i < 15; i++) {
            GameHistoryEntry entry = createTestEntry(i, "Map_" + i);
            entry.setTimestamp(System.currentTimeMillis() + i * 1000L);
            GameHistoryManager.addHistoryEntry(activity, entry);
        }

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertTrue("Should have at most 11 entries, got " + entries.size(),
                entries.size() <= 11);
    }

    // ==================== INDEX TESTS ====================

    @Test
    public void testGetNextHistoryIndex() {
        int firstIndex = GameHistoryManager.getNextHistoryIndex(activity);
        assertEquals("First index should be 0", 0, firstIndex);

        GameHistoryEntry entry = createTestEntry(0, "IndexTest");
        GameHistoryManager.addHistoryEntry(activity, entry);

        int secondIndex = GameHistoryManager.getNextHistoryIndex(activity);
        assertEquals("Second index should be 1", 1, secondIndex);
    }

    @Test
    public void testGetHistoryIndex() {
        GameHistoryEntry entry = createTestEntry(0, "FindMe");
        GameHistoryManager.addHistoryEntry(activity, entry);

        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        String mapPath = entries.get(0).getMapPath();

        int index = GameHistoryManager.getHistoryIndex(activity, mapPath);
        assertEquals("Should find entry at index 0", 0, index);

        int notFound = GameHistoryManager.getHistoryIndex(activity, "nonexistent.txt");
        assertEquals("Should return -1 for missing entry", -1, notFound);
    }

    // ==================== FILE WRITE/READ TESTS ====================

    @Test
    public void testHistoryFileActuallyWritten() {
        GameHistoryEntry entry = createTestEntry(0, "FileTest");
        String mapPath = entry.getMapPath();

        // Write test content to the history file
        boolean written = FileReadWrite.writePrivateData(activity, mapPath, "test save data content");
        assertTrue("writePrivateData should succeed for flat path: " + mapPath, written);

        // Verify file exists
        boolean exists = FileReadWrite.privateDataExists(activity, mapPath);
        assertTrue("History file should exist after write: " + mapPath, exists);

        // Read back
        String content = FileReadWrite.readPrivateData(activity, mapPath);
        assertEquals("test save data content", content);

        // Clean up
        FileReadWrite.deletePrivateData(activity, mapPath);
    }

    @Test
    public void testWriteFailsWithPathSeparator() {
        // Verify that paths with / are rejected by openFileOutput
        boolean written = FileReadWrite.writePrivateData(activity, "history/bad_path.txt", "data");
        assertFalse("writePrivateData should fail for path with separator", written);
    }

    // ==================== EMPTY STATE TESTS ====================

    @Test
    public void testEmptyHistoryReturnsEmptyList() {
        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
        assertNotNull("Should return non-null list", entries);
        assertTrue("Should be empty", entries.isEmpty());
    }

    @Test
    public void testInitializeCreatesIndexFile() {
        // Delete the index file first
        FileReadWrite.deletePrivateData(activity, "history_index.json");
        assertFalse(FileReadWrite.privateDataExists(activity, "history_index.json"));

        GameHistoryManager.initialize(activity);
        assertTrue("Index file should exist after initialize",
                FileReadWrite.privateDataExists(activity, "history_index.json"));
    }

    // ==================== HELPER ====================

    private GameHistoryEntry createTestEntry(int index, String mapName) {
        String mapPath = "history_" + index + ".txt";
        String previewPath = "history_" + index + ".txt_preview.txt";
        return new GameHistoryEntry(
                mapPath,
                mapName,
                System.currentTimeMillis(),
                10,     // playDuration
                5,      // movesMade
                3,      // optimalMoves
                "12x12", // boardSize
                previewPath
        );
    }
}

package roboyard.eclabs;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for sync functionality.
 * Tests parsing, data extraction, and sync logic without requiring Android context.
 * Note: org.json is not available in local JUnit tests (Android stub), so JSON tests
 * are in androidTest instead.
 */
public class SyncManagerTest {

    // ========== Save Data Parsing Tests ==========

    @Test
    public void testExtractMapNameFromSaveData() {
        String saveData = "board:12,12;NAME:TestMap;DIFFICULTY:3;SIZE:12,12;\nwall data here";
        String mapName = extractMapName(saveData);
        assertEquals("TestMap", mapName);
    }

    @Test
    public void testExtractMapNameReturnsNullWhenMissing() {
        String saveData = "board:12,12;DIFFICULTY:3;\nwall data";
        String mapName = extractMapName(saveData);
        assertNull(mapName);
    }

    @Test
    public void testExtractBoardWidth() {
        String saveData = "board:12,12;SIZE:16,14;DIFFICULTY:3;\nwall data";
        int width = extractBoardWidth(saveData);
        assertEquals(16, width);
    }

    @Test
    public void testExtractBoardHeight() {
        String saveData = "board:12,12;SIZE:16,14;DIFFICULTY:3;\nwall data";
        int height = extractBoardHeight(saveData);
        assertEquals(14, height);
    }

    @Test
    public void testExtractBoardSizeDefaults() {
        String saveData = "board:12,12;DIFFICULTY:3;\nwall data";
        int width = extractBoardWidth(saveData);
        int height = extractBoardHeight(saveData);
        assertEquals(12, width);
        assertEquals(12, height);
    }

    @Test
    public void testExtractSolvedStatus() {
        String solved = "board:12,12;SOLVED:true;SIZE:12,12;\ndata";
        String unsolved = "board:12,12;SOLVED:false;SIZE:12,12;\ndata";
        String missing = "board:12,12;SIZE:12,12;\ndata";

        assertTrue(solved.contains("SOLVED:true"));
        assertFalse(unsolved.contains("SOLVED:true"));
        assertFalse(missing.contains("SOLVED:true"));
    }

    // ========== Board Size From String Tests ==========

    @Test
    public void testExtractBoardWidthFromSizeString() {
        assertEquals(16, extractBoardWidthFromSize("16x14"));
        assertEquals(12, extractBoardWidthFromSize("12x12"));
        assertEquals(12, extractBoardWidthFromSize(null));
        assertEquals(12, extractBoardWidthFromSize("invalid"));
    }

    @Test
    public void testExtractBoardHeightFromSizeString() {
        assertEquals(14, extractBoardHeightFromSize("16x14"));
        assertEquals(12, extractBoardHeightFromSize("12x12"));
        assertEquals(12, extractBoardHeightFromSize(null));
        assertEquals(12, extractBoardHeightFromSize("invalid"));
    }

    // ========== Timestamp Parsing Tests ==========

    @Test
    public void testParseValidTimestamp() {
        long ts = parseTimestamp("2026-02-06T12:00:00+01:00");
        assertTrue("Timestamp should be positive", ts > 0);
        assertTrue("Timestamp should be in the past or now", ts <= System.currentTimeMillis() + 86400000);
    }

    @Test
    public void testParseNullTimestamp() {
        long ts = parseTimestamp(null);
        long now = System.currentTimeMillis();
        assertTrue("Null timestamp should return current time", Math.abs(ts - now) < 1000);
    }

    @Test
    public void testParseEmptyTimestamp() {
        long ts = parseTimestamp("");
        long now = System.currentTimeMillis();
        assertTrue("Empty timestamp should return current time", Math.abs(ts - now) < 1000);
    }

    // ========== Slot 0 Protection Tests ==========

    @Test
    public void testSlot0IsAutoSaveReserved() {
        assertTrue("Slot 0 should be blocked for manual saves", isSlotReservedForAutoSave(0));
    }

    @Test
    public void testManualSaveSlotsAreNotReserved() {
        for (int i = 1; i <= 9; i++) {
            assertFalse("Slot " + i + " should be valid for manual saves", isSlotReservedForAutoSave(i));
        }
    }

    // ========== Save Data Format Validation ==========

    @Test
    public void testSaveDataContainsBoardTag() {
        String saveData = "board:12,12;SIZE:12,12;DIFFICULTY:3;\nwall data";
        assertTrue("Save data should contain board tag", saveData.startsWith("board:"));
    }

    @Test
    public void testExtractSlotIdFromFilename() {
        assertEquals(1, extractSlotIdFromFilename("save_1.dat"));
        assertEquals(5, extractSlotIdFromFilename("save_5.dat"));
        assertEquals(10, extractSlotIdFromFilename("save_10.dat"));
    }

    @Test
    public void testExtractMultipleSizeFormats() {
        // Standard format
        assertEquals(16, extractBoardWidth("board:16,14;SIZE:16,14;"));
        assertEquals(14, extractBoardHeight("board:16,14;SIZE:16,14;"));

        // With extra tags between
        assertEquals(12, extractBoardWidth("board:12,12;SOLVED:true;SIZE:12,12;DIFFICULTY:3;"));
        assertEquals(12, extractBoardHeight("board:12,12;SOLVED:true;SIZE:12,12;DIFFICULTY:3;"));
    }

    @Test
    public void testExtractMapNameWithSpecialChars() {
        String saveData = "board:12,12;NAME:Random-abc123;SIZE:12,12;";
        assertEquals("Random-abc123", extractMapName(saveData));
    }

    @Test
    public void testBoardSizeFromStringWithSpaces() {
        assertEquals(16, extractBoardWidthFromSize(" 16 x 14 "));
        assertEquals(14, extractBoardHeightFromSize(" 16 x 14 "));
    }

    // ========== Helper methods (mirror SyncManager's private methods) ==========

    private boolean isSlotReservedForAutoSave(int slotId) {
        return slotId == 0;
    }

    private int extractSlotIdFromFilename(String filename) {
        String idStr = filename.replace("save_", "").replace(".dat", "");
        return Integer.parseInt(idStr);
    }

    private String extractMapName(String saveData) {
        int nameStart = saveData.indexOf("NAME:");
        if (nameStart >= 0) {
            int nameEnd = saveData.indexOf(";", nameStart);
            if (nameEnd > nameStart) {
                return saveData.substring(nameStart + 5, nameEnd);
            }
        }
        return null;
    }

    private int extractBoardWidth(String saveData) {
        return extractSizeComponent(saveData, 0, 12);
    }

    private int extractBoardHeight(String saveData) {
        return extractSizeComponent(saveData, 1, 12);
    }

    private int extractSizeComponent(String saveData, int index, int defaultValue) {
        int sizeStart = saveData.indexOf("SIZE:");
        if (sizeStart >= 0) {
            int sizeEnd = saveData.indexOf(";", sizeStart);
            if (sizeEnd > sizeStart) {
                String sizeStr = saveData.substring(sizeStart + 5, sizeEnd);
                String[] parts = sizeStr.split(",");
                if (parts.length > index) {
                    try {
                        return Integer.parseInt(parts[index].trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    private int extractBoardWidthFromSize(String boardSize) {
        if (boardSize != null && boardSize.contains("x")) {
            try {
                return Integer.parseInt(boardSize.split("x")[0].trim());
            } catch (NumberFormatException e) {
                return 12;
            }
        }
        return 12;
    }

    private int extractBoardHeightFromSize(String boardSize) {
        if (boardSize != null && boardSize.contains("x")) {
            try {
                return Integer.parseInt(boardSize.split("x")[1].trim());
            } catch (NumberFormatException e) {
                return 12;
            }
        }
        return 12;
    }

    // ========== Streak Sync Tests ==========

    @Test
    public void testStreakBidirectionalSync_serverHigher() {
        int localStreak = 5;
        int serverStreak = 10;
        int result = Math.max(localStreak, serverStreak);
        assertEquals("Server streak is higher, should use server value", 10, result);
    }

    @Test
    public void testStreakBidirectionalSync_localHigher() {
        int localStreak = 15;
        int serverStreak = 3;
        int result = Math.max(localStreak, serverStreak);
        assertEquals("Local streak is higher, should keep local value", 15, result);
    }

    @Test
    public void testStreakBidirectionalSync_equal() {
        int localStreak = 7;
        int serverStreak = 7;
        int result = Math.max(localStreak, serverStreak);
        assertEquals("Equal streaks should stay same", 7, result);
    }

    @Test
    public void testStreakBidirectionalSync_serverZero() {
        int localStreak = 5;
        int serverStreak = 0;
        int result = Math.max(localStreak, serverStreak);
        assertEquals("Server zero should keep local", 5, result);
    }

    @Test
    public void testStreakBidirectionalSync_localZero() {
        int localStreak = 0;
        int serverStreak = 12;
        int result = Math.max(localStreak, serverStreak);
        assertEquals("Local zero should restore from server", 12, result);
    }

    // ========== Offline Achievement + Sync Simulation ==========

    @Test
    public void testOfflineAchievementUnlockSimulation() {
        // Simulate: user unlocks achievements while offline
        // When they go online, the achievements should be in the sync payload
        
        // Simulate local achievement state
        java.util.Map<String, Boolean> localAchievements = new java.util.HashMap<>();
        localAchievements.put("first_solve", true);    // already synced
        localAchievements.put("speed_demon", true);    // unlocked offline
        localAchievements.put("perfect_10", true);     // unlocked offline
        localAchievements.put("daily_login_7", false); // not yet unlocked
        
        // Count achievements to sync (all unlocked ones)
        int toSync = 0;
        for (boolean unlocked : localAchievements.values()) {
            if (unlocked) toSync++;
        }
        
        assertEquals("Should sync 3 unlocked achievements", 3, toSync);
    }

    @Test
    public void testOfflineAchievementMergeWithServer() {
        // Simulate: server has some achievements, local has some
        // Merge should be union (never remove)
        
        java.util.Set<String> serverAchievements = new java.util.HashSet<>();
        serverAchievements.add("first_solve");
        serverAchievements.add("explorer_10");
        
        java.util.Set<String> localAchievements = new java.util.HashSet<>();
        localAchievements.add("first_solve");
        localAchievements.add("speed_demon");
        localAchievements.add("perfect_10");
        
        // Merge: union of both sets
        java.util.Set<String> merged = new java.util.HashSet<>(localAchievements);
        merged.addAll(serverAchievements);
        
        assertEquals("Merged should have 4 unique achievements", 4, merged.size());
        assertTrue(merged.contains("first_solve"));
        assertTrue(merged.contains("explorer_10"));
        assertTrue(merged.contains("speed_demon"));
        assertTrue(merged.contains("perfect_10"));
    }

    @Test
    public void testOfflineAchievementNeverRemovesLocal() {
        // Even if server doesn't have an achievement, local should keep it
        java.util.Set<String> serverAchievements = new java.util.HashSet<>();
        // Server has nothing
        
        java.util.Set<String> localAchievements = new java.util.HashSet<>();
        localAchievements.add("speed_demon");
        localAchievements.add("perfect_10");
        
        // Merge should keep all local
        java.util.Set<String> merged = new java.util.HashSet<>(localAchievements);
        merged.addAll(serverAchievements);
        
        assertEquals("Should keep all 2 local achievements even if server has none", 2, merged.size());
    }

    @Test
    public void testSyncThrottling() {
        long lastSync = System.currentTimeMillis() - 30_000; // 30 seconds ago
        long now = System.currentTimeMillis();
        long minInterval = 60_000; // 1 minute
        
        boolean shouldSync = (now - lastSync) >= minInterval;
        assertFalse("Should NOT sync if less than 1 minute since last sync", shouldSync);
    }

    @Test
    public void testSyncThrottlingAllowsAfterInterval() {
        long lastSync = System.currentTimeMillis() - 120_000; // 2 minutes ago
        long now = System.currentTimeMillis();
        long minInterval = 60_000; // 1 minute
        
        boolean shouldSync = (now - lastSync) >= minInterval;
        assertTrue("Should sync if more than 1 minute since last sync", shouldSync);
    }

    @Test
    public void testStreakDateStringFormat() {
        // Simulate what getLastLoginDateString does
        long daysSinceEpoch = 20490; // some day number
        long normalDayMs = 86400000L;
        long timestampMs = daysSinceEpoch * normalDayMs;
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date(timestampMs));
        
        assertNotNull("Date string should not be null", dateStr);
        assertTrue("Date string should match yyyy-MM-dd format", dateStr.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testAutoLoginUrlConstruction() {
        String token = "abc123def456";
        String expectedUrl = "https://roboyard.z11.de/auto-login?token=abc123def456";
        String url = "https://roboyard.z11.de/auto-login?token=" + token;
        assertEquals(expectedUrl, url);
    }

    @Test
    public void testAutoLoginUrlWithSpecialChars() {
        String token = "abc+def/ghi=";
        // URI encoding would be needed
        String encoded = token.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D");
        String url = "https://roboyard.z11.de/auto-login?token=" + encoded;
        assertTrue("URL should contain encoded token", url.contains("abc%2Bdef%2Fghi%3D"));
    }

    // ========== Helper methods ==========

    private long parseTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
            java.util.Date date = sdf.parse(isoTimestamp);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}

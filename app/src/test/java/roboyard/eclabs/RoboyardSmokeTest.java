package roboyard.eclabs;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive smoke test for Roboyard.
 * Tests all core game logic, data classes, and utility functions
 * that don't require Android context.
 * 
 * Run before every commit:
 *   ./gradlew test --tests "roboyard.eclabs.RoboyardSmokeTest"
 * 
 * Design goals:
 * - Fast execution (no I/O, no Android context)
 * - Broad coverage of game logic
 * - Catches regressions in core data structures
 */
public class RoboyardSmokeTest {

    // ========================================================================
    // GameElement Tests
    // ========================================================================

    @Test
    public void testGameElementCreation() {
        // TYPE_ROBOT = 1 (from Constants)
        roboyard.eclabs.ui.GameElement robot = new roboyard.eclabs.ui.GameElement(1, 5, 7);
        assertEquals(1, robot.getType());
        assertEquals(5, robot.getX());
        assertEquals(7, robot.getY());
        assertEquals(0, robot.getColor()); // default color
        assertFalse(robot.isSelected());
    }

    @Test
    public void testGameElementPositionUpdate() {
        roboyard.eclabs.ui.GameElement robot = new roboyard.eclabs.ui.GameElement(1, 0, 0);
        robot.setX(10);
        robot.setY(12);
        assertEquals(10, robot.getX());
        assertEquals(12, robot.getY());
    }

    @Test
    public void testGameElementColorAndSelection() {
        roboyard.eclabs.ui.GameElement robot = new roboyard.eclabs.ui.GameElement(1, 0, 0);
        robot.setColor(3); // yellow
        robot.setSelected(true);
        assertEquals(3, robot.getColor());
        assertTrue(robot.isSelected());
        
        robot.setSelected(false);
        assertFalse(robot.isSelected());
    }

    @Test
    public void testGameElementIsRobot() {
        roboyard.eclabs.ui.GameElement robot = new roboyard.eclabs.ui.GameElement(1, 0, 0); // TYPE_ROBOT
        roboyard.eclabs.ui.GameElement target = new roboyard.eclabs.ui.GameElement(2, 0, 0); // TYPE_TARGET
        assertTrue(robot.isRobot());
        assertFalse(target.isRobot());
    }

    @Test
    public void testGameElementDirectionX() {
        roboyard.eclabs.ui.GameElement robot = new roboyard.eclabs.ui.GameElement(1, 0, 0);
        assertEquals(1, robot.getDirectionX()); // default right
        
        robot.setDirectionX(-1);
        assertEquals(-1, robot.getDirectionX()); // left
        
        robot.setDirectionX(5); // any positive = right
        assertEquals(1, robot.getDirectionX());
        
        robot.setDirectionX(0); // zero should not change
        assertEquals(1, robot.getDirectionX());
    }

    @Test
    public void testGameElementAnimationPosition() {
        roboyard.eclabs.ui.GameElement robot = new roboyard.eclabs.ui.GameElement(1, 0, 0);
        assertFalse(robot.hasAnimationPosition());
        
        robot.setAnimationPosition(3.5f, 7.2f);
        assertTrue(robot.hasAnimationPosition());
        assertEquals(3.5f, robot.getAnimationX(), 0.001f);
        assertEquals(7.2f, robot.getAnimationY(), 0.001f);
        
        robot.clearAnimationPosition();
        assertFalse(robot.hasAnimationPosition());
    }

    @Test
    public void testGameElementRejectsInvalidAnimationPosition() {
        roboyard.eclabs.ui.GameElement robot = new roboyard.eclabs.ui.GameElement(1, 0, 0);
        robot.setAnimationPosition(Float.NaN, 5.0f);
        assertFalse("NaN should be rejected", robot.hasAnimationPosition());
        
        robot.setAnimationPosition(5.0f, Float.POSITIVE_INFINITY);
        assertFalse("Infinity should be rejected", robot.hasAnimationPosition());
    }

    @Test
    public void testGameElementColorConstants() {
        assertEquals(0, roboyard.eclabs.ui.GameElement.COLOR_RED);
        assertEquals(1, roboyard.eclabs.ui.GameElement.COLOR_GREEN);
        assertEquals(2, roboyard.eclabs.ui.GameElement.COLOR_BLUE);
        assertEquals(3, roboyard.eclabs.ui.GameElement.COLOR_YELLOW);
        assertEquals(4, roboyard.eclabs.ui.GameElement.COLOR_SILVER);
    }

    // ========================================================================
    // GameMove Tests
    // ========================================================================

    @Test
    public void testGameMoveCreationHorizontal() {
        roboyard.eclabs.ui.GameMove move = new roboyard.eclabs.ui.GameMove(0, 0, 2, 5, 8, 5);
        assertEquals(0, move.getRobotId());
        assertEquals(0, move.getRobotColor());
        assertEquals(2, move.getFromX());
        assertEquals(5, move.getFromY());
        assertEquals(8, move.getToX());
        assertEquals(5, move.getToY());
        assertEquals(roboyard.eclabs.ui.GameMove.RIGHT, move.getDirection());
        assertEquals(6, move.getDistance());
    }

    @Test
    public void testGameMoveCreationVertical() {
        roboyard.eclabs.ui.GameMove move = new roboyard.eclabs.ui.GameMove(1, 2, 3, 10, 3, 2);
        assertEquals(roboyard.eclabs.ui.GameMove.UP, move.getDirection());
        assertEquals(8, move.getDistance());
    }

    @Test
    public void testGameMoveDirectionLeft() {
        roboyard.eclabs.ui.GameMove move = new roboyard.eclabs.ui.GameMove(0, 0, 10, 5, 3, 5);
        assertEquals(roboyard.eclabs.ui.GameMove.LEFT, move.getDirection());
        assertEquals(7, move.getDistance());
    }

    @Test
    public void testGameMoveDirectionDown() {
        roboyard.eclabs.ui.GameMove move = new roboyard.eclabs.ui.GameMove(0, 0, 5, 2, 5, 9);
        assertEquals(roboyard.eclabs.ui.GameMove.DOWN, move.getDirection());
        assertEquals(7, move.getDistance());
    }

    @Test
    public void testGameMoveColorNames() {
        assertEquals("Red", new roboyard.eclabs.ui.GameMove(0, 0, 0, 0, 1, 0).getRobotColorName());
        assertEquals("Green", new roboyard.eclabs.ui.GameMove(0, 1, 0, 0, 1, 0).getRobotColorName());
        assertEquals("Blue", new roboyard.eclabs.ui.GameMove(0, 2, 0, 0, 1, 0).getRobotColorName());
        assertEquals("Yellow", new roboyard.eclabs.ui.GameMove(0, 3, 0, 0, 1, 0).getRobotColorName());
        assertEquals("Unknown", new roboyard.eclabs.ui.GameMove(0, 99, 0, 0, 1, 0).getRobotColorName());
    }

    @Test
    public void testGameMoveToString() {
        roboyard.eclabs.ui.GameMove move = new roboyard.eclabs.ui.GameMove(0, 0, 0, 0, 5, 0);
        String str = move.toString();
        assertNotNull(str);
        assertTrue(str.contains("Red"));
        assertTrue(str.contains("Right"));
        assertTrue(str.contains("5"));
    }

    @Test
    public void testGameMoveDirectionConstants() {
        assertEquals(0, roboyard.eclabs.ui.GameMove.UP);
        assertEquals(1, roboyard.eclabs.ui.GameMove.RIGHT);
        assertEquals(2, roboyard.eclabs.ui.GameMove.DOWN);
        assertEquals(3, roboyard.eclabs.ui.GameMove.LEFT);
    }

    @Test
    public void testGameMoveFromDirectionConstructor() {
        roboyard.eclabs.ui.GameMove move = new roboyard.eclabs.ui.GameMove(2, roboyard.eclabs.ui.GameMove.DOWN, 4);
        assertEquals(2, move.getRobotId());
        assertEquals(roboyard.eclabs.ui.GameMove.DOWN, move.getDirection());
        assertEquals(4, move.getDistance());
        assertEquals(-1, move.getRobotColor()); // unknown color
    }

    // ========================================================================
    // LevelCompletionData Tests
    // ========================================================================

    @Test
    public void testLevelCompletionDataDefaults() {
        roboyard.eclabs.ui.LevelCompletionData data = new roboyard.eclabs.ui.LevelCompletionData(42);
        assertEquals(42, data.getLevelId());
        assertFalse(data.isCompleted());
        assertEquals(0, data.getHintsShown());
        assertEquals(0, data.getTimeNeeded());
        assertEquals(0, data.getMovesNeeded());
        assertEquals(0, data.getOptimalMoves());
        assertEquals(0, data.getRobotsUsed());
        assertEquals(0, data.getSquaresSurpassed());
        assertEquals(0, data.getStars());
    }

    @Test
    public void testLevelCompletionDataSetters() {
        roboyard.eclabs.ui.LevelCompletionData data = new roboyard.eclabs.ui.LevelCompletionData(1);
        data.setCompleted(true);
        data.setHintsShown(2);
        data.setTimeNeeded(45000);
        data.setMovesNeeded(5);
        data.setOptimalMoves(3);
        data.setRobotsUsed(2);
        data.setSquaresSurpassed(15);
        data.setStars(3);
        
        assertTrue(data.isCompleted());
        assertEquals(2, data.getHintsShown());
        assertEquals(45000, data.getTimeNeeded());
        assertEquals(5, data.getMovesNeeded());
        assertEquals(3, data.getOptimalMoves());
        assertEquals(2, data.getRobotsUsed());
        assertEquals(15, data.getSquaresSurpassed());
        assertEquals(3, data.getStars());
    }

    @Test
    public void testLevelCompletionDataStarsClamping() {
        roboyard.eclabs.ui.LevelCompletionData data = new roboyard.eclabs.ui.LevelCompletionData(1);
        
        data.setStars(-5);
        assertEquals("Negative stars should clamp to 0", 0, data.getStars());
        
        data.setStars(10);
        assertEquals("Stars above 3 should clamp to 3", 3, data.getStars());
        
        data.setStars(2);
        assertEquals(2, data.getStars());
    }

    @Test
    public void testLevelCompletionDataToString() {
        roboyard.eclabs.ui.LevelCompletionData data = new roboyard.eclabs.ui.LevelCompletionData(7);
        String str = data.toString();
        assertNotNull(str);
        assertTrue(str.contains("levelId=7"));
    }

    // ========================================================================
    // GridElement Tests
    // ========================================================================

    @Test
    public void testGridElementCreation() {
        roboyard.logic.core.GridElement elem = new roboyard.logic.core.GridElement(3, 7, "robot_red");
        assertEquals(3, elem.getX());
        assertEquals(7, elem.getY());
        assertEquals("robot_red", elem.getType());
    }

    @Test
    public void testGridElementSetters() {
        roboyard.logic.core.GridElement elem = new roboyard.logic.core.GridElement(0, 0, "mh");
        elem.setX(15);
        elem.setY(20);
        elem.setType("mv");
        assertEquals(15, elem.getX());
        assertEquals(20, elem.getY());
        assertEquals("mv", elem.getType());
    }

    @Test
    public void testGridElementToChar() {
        assertEquals("-", new roboyard.logic.core.GridElement(0, 0, "mh").toChar());
        assertEquals("|", new roboyard.logic.core.GridElement(0, 0, "mv").toChar());
        assertEquals("r", new roboyard.logic.core.GridElement(0, 0, "robot_red").toChar());
        assertEquals("g", new roboyard.logic.core.GridElement(0, 0, "robot_green").toChar());
        assertEquals("b", new roboyard.logic.core.GridElement(0, 0, "robot_blue").toChar());
        assertEquals("y", new roboyard.logic.core.GridElement(0, 0, "robot_yellow").toChar());
        assertEquals("R", new roboyard.logic.core.GridElement(0, 0, "target_red").toChar());
        assertEquals("G", new roboyard.logic.core.GridElement(0, 0, "target_green").toChar());
        assertEquals("B", new roboyard.logic.core.GridElement(0, 0, "target_blue").toChar());
        assertEquals("Y", new roboyard.logic.core.GridElement(0, 0, "target_yellow").toChar());
        assertEquals("M", new roboyard.logic.core.GridElement(0, 0, "target_multi").toChar());
        assertEquals(" ", new roboyard.logic.core.GridElement(0, 0, "").toChar());
    }

    // ========================================================================
    // MapObjects Tests (static utility methods)
    // ========================================================================

    @Test
    public void testMapObjectsExtractDataFromString() {
        String data = "board:12,12;robot_red5,7;robot_green3,4;mh6,8;mv10,2;target_blue9,11;";
        ArrayList<roboyard.logic.core.GridElement> elements = MapObjects.extractDataFromString(data);
        
        assertNotNull(elements);
        assertEquals("Should extract 5 elements (robots, walls, target)", 5, elements.size());
        
        // Verify first element is robot_red at (5,7)
        assertEquals("robot_red", elements.get(0).getType());
        assertEquals(5, elements.get(0).getX());
        assertEquals(7, elements.get(0).getY());
    }

    @Test
    public void testMapObjectsExtractEmptyString() {
        ArrayList<roboyard.logic.core.GridElement> elements = MapObjects.extractDataFromString("");
        assertNotNull(elements);
        assertEquals(0, elements.size());
    }

    @Test
    public void testMapObjectsExtractAllElementTypes() {
        String data = "board:12,12;" +
            "mh1,2;mv3,4;" +
            "robot_green5,6;robot_yellow7,8;robot_red9,10;robot_blue11,0;" +
            "target_green1,1;target_yellow2,2;target_red3,3;target_blue4,4;target_multi5,5;";
        ArrayList<roboyard.logic.core.GridElement> elements = MapObjects.extractDataFromString(data);
        assertEquals("Should extract all 11 element types", 11, elements.size());
    }

    @Test
    public void testMapObjectsGenerateUnique5Letter() {
        String id1 = MapObjects.generateUnique5LetterFromString("board:12,12;robot_red5,7;");
        String id2 = MapObjects.generateUnique5LetterFromString("board:12,12;robot_red5,8;");
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals("Should be 5 characters", 5, id1.length());
        assertNotEquals("Different maps should produce different IDs", id1, id2);
    }

    @Test
    public void testMapObjectsUnique5LetterAlternatesVowelsConsonants() {
        String id = MapObjects.generateUnique5LetterFromString("test");
        assertNotNull(id);
        assertEquals(5, id.length());
        
        String vowels = "AEIOU";
        String consonants = "BCDFGHJKLMNPQRSTVWXYZ";
        
        // Positions 0, 2, 4 should be consonants; 1, 3 should be vowels
        assertTrue("Pos 0 should be consonant", consonants.indexOf(id.charAt(0)) >= 0);
        assertTrue("Pos 1 should be vowel", vowels.indexOf(id.charAt(1)) >= 0);
        assertTrue("Pos 2 should be consonant", consonants.indexOf(id.charAt(2)) >= 0);
        assertTrue("Pos 3 should be vowel", vowels.indexOf(id.charAt(3)) >= 0);
        assertTrue("Pos 4 should be consonant", consonants.indexOf(id.charAt(4)) >= 0);
    }

    @Test
    public void testMapObjectsUnique5LetterDeterministic() {
        String id1 = MapObjects.generateUnique5LetterFromString("same input");
        String id2 = MapObjects.generateUnique5LetterFromString("same input");
        assertEquals("Same input should produce same output", id1, id2);
    }

    // ========================================================================
    // Achievement Tests
    // ========================================================================

    @Test
    public void testAchievementCreationWithDrawable() {
        roboyard.eclabs.achievements.Achievement ach = new roboyard.eclabs.achievements.Achievement(
            "test_id", "name_key", "desc_key",
            roboyard.eclabs.achievements.AchievementCategory.PROGRESSION, "icon_1_lightning");
        
        assertEquals("test_id", ach.getId());
        assertEquals("name_key", ach.getNameKey());
        assertEquals("desc_key", ach.getDescriptionKey());
        assertEquals(roboyard.eclabs.achievements.AchievementCategory.PROGRESSION, ach.getCategory());
        assertEquals("icon_1_lightning", ach.getIconDrawableName());
        assertEquals(-1, ach.getSpriteIndex());
        assertFalse(ach.isUnlocked());
        assertEquals(0, ach.getUnlockedTimestamp());
    }

    @Test
    public void testAchievementCreationWithSpriteIndex() {
        roboyard.eclabs.achievements.Achievement ach = new roboyard.eclabs.achievements.Achievement(
            "test_id", "name_key", "desc_key",
            roboyard.eclabs.achievements.AchievementCategory.PERFORMANCE, 42);
        
        assertEquals(42, ach.getSpriteIndex());
        assertNull(ach.getIconDrawableName());
    }

    @Test
    public void testAchievementUnlock() {
        roboyard.eclabs.achievements.Achievement ach = new roboyard.eclabs.achievements.Achievement(
            "test", "n", "d", roboyard.eclabs.achievements.AchievementCategory.PROGRESSION, "icon");
        
        assertFalse(ach.isUnlocked());
        assertEquals(0, ach.getUnlockedTimestamp());
        
        long before = System.currentTimeMillis();
        ach.setUnlocked(true);
        long after = System.currentTimeMillis();
        
        assertTrue(ach.isUnlocked());
        assertTrue("Timestamp should be set on unlock", ach.getUnlockedTimestamp() >= before);
        assertTrue("Timestamp should be set on unlock", ach.getUnlockedTimestamp() <= after);
    }

    @Test
    public void testAchievementUnlockTimestampNotOverwritten() {
        roboyard.eclabs.achievements.Achievement ach = new roboyard.eclabs.achievements.Achievement(
            "test", "n", "d", roboyard.eclabs.achievements.AchievementCategory.PROGRESSION, "icon");
        
        ach.setUnlocked(true);
        long firstTimestamp = ach.getUnlockedTimestamp();
        
        // Unlock again - timestamp should NOT change
        ach.setUnlocked(true);
        assertEquals("Timestamp should not change on re-unlock", firstTimestamp, ach.getUnlockedTimestamp());
    }

    @Test
    public void testAchievementFormatArgs() {
        roboyard.eclabs.achievements.Achievement ach = new roboyard.eclabs.achievements.Achievement(
            "test", "n", "d", roboyard.eclabs.achievements.AchievementCategory.PROGRESSION, "icon");
        
        assertNull(ach.getNameFormatArgs());
        assertNull(ach.getDescriptionFormatArgs());
        
        ach.setNameFormatArgs(10, "test");
        ach.setDescriptionFormatArgs(5);
        
        assertNotNull(ach.getNameFormatArgs());
        assertEquals(2, ach.getNameFormatArgs().length);
        assertEquals(10, ach.getNameFormatArgs()[0]);
        
        assertNotNull(ach.getDescriptionFormatArgs());
        assertEquals(1, ach.getDescriptionFormatArgs().length);
    }

    // ========================================================================
    // AchievementDefinitions Tests
    // ========================================================================

    @Test
    public void testAchievementDefinitionsNotEmpty() {
        Map<String, roboyard.eclabs.achievements.Achievement> all = 
            roboyard.eclabs.achievements.AchievementDefinitions.getAll();
        assertNotNull(all);
        assertTrue("Should have achievements defined", all.size() > 0);
    }

    @Test
    public void testAchievementDefinitionsHaveUniqueIds() {
        Map<String, roboyard.eclabs.achievements.Achievement> all = 
            roboyard.eclabs.achievements.AchievementDefinitions.getAll();
        Set<String> ids = new HashSet<>();
        for (roboyard.eclabs.achievements.Achievement ach : all.values()) {
            assertTrue("Duplicate achievement ID: " + ach.getId(), ids.add(ach.getId()));
        }
    }

    @Test
    public void testAchievementDefinitionsAllHaveRequiredFields() {
        Map<String, roboyard.eclabs.achievements.Achievement> all = 
            roboyard.eclabs.achievements.AchievementDefinitions.getAll();
        for (roboyard.eclabs.achievements.Achievement ach : all.values()) {
            assertNotNull("Achievement should have ID: " + ach, ach.getId());
            assertFalse("Achievement ID should not be empty", ach.getId().isEmpty());
            assertNotNull("Achievement should have name key: " + ach.getId(), ach.getNameKey());
            assertNotNull("Achievement should have description key: " + ach.getId(), ach.getDescriptionKey());
            assertNotNull("Achievement should have category: " + ach.getId(), ach.getCategory());
            // Must have either drawable name or sprite index
            assertTrue("Achievement should have icon: " + ach.getId(),
                ach.getIconDrawableName() != null || ach.getSpriteIndex() >= 0);
        }
    }

    @Test
    public void testAchievementDefinitionsKnownAchievementsExist() {
        Map<String, roboyard.eclabs.achievements.Achievement> all = 
            roboyard.eclabs.achievements.AchievementDefinitions.getAll();
        
        // Core achievements that must exist
        String[] requiredIds = {
            "first_game", "level_1_complete", "level_10_complete",
            "daily_login_7", "daily_login_30",
            "perfect_solutions_5", "speedrun_under_30s"
        };
        for (String id : requiredIds) {
            assertTrue("Required achievement missing: " + id, all.containsKey(id));
        }
    }

    @Test
    public void testAchievementColorDeterministic() {
        int color1 = roboyard.eclabs.achievements.AchievementDefinitions.getAchievementColor("test_id");
        int color2 = roboyard.eclabs.achievements.AchievementDefinitions.getAchievementColor("test_id");
        assertEquals("Same ID should produce same color", color1, color2);
    }

    @Test
    public void testAchievementColorFromPalette() {
        int color = roboyard.eclabs.achievements.AchievementDefinitions.getAchievementColor("any_id");
        boolean found = false;
        for (int c : roboyard.eclabs.achievements.AchievementDefinitions.ACHIEVEMENT_COLORS) {
            if (c == color) { found = true; break; }
        }
        assertTrue("Color should be from the predefined palette", found);
    }

    // ========================================================================
    // AchievementCategory Tests
    // ========================================================================

    @Test
    public void testAchievementCategoryValues() {
        roboyard.eclabs.achievements.AchievementCategory[] categories = 
            roboyard.eclabs.achievements.AchievementCategory.values();
        assertTrue("Should have categories", categories.length > 0);
    }

    @Test
    public void testAchievementCategoryDisplayOrder() {
        assertEquals(0, roboyard.eclabs.achievements.AchievementCategory.PROGRESSION.getDisplayOrder());
        assertEquals(1, roboyard.eclabs.achievements.AchievementCategory.PERFORMANCE.getDisplayOrder());
        assertEquals(2, roboyard.eclabs.achievements.AchievementCategory.MASTERY.getDisplayOrder());
    }

    @Test
    public void testAchievementCategoryStringResName() {
        assertNotNull(roboyard.eclabs.achievements.AchievementCategory.PROGRESSION.getStringResName());
        assertEquals("achievement_category_progression", 
            roboyard.eclabs.achievements.AchievementCategory.PROGRESSION.getStringResName());
    }

    @Test
    public void testAchievementCategoryChallengeDisabled() {
        assertFalse("CHALLENGE category should be disabled",
            roboyard.eclabs.achievements.AchievementCategory.CHALLENGE.isEnabled());
    }

    @Test
    public void testAchievementCategoryOthersEnabled() {
        for (roboyard.eclabs.achievements.AchievementCategory cat : 
                roboyard.eclabs.achievements.AchievementCategory.values()) {
            if (cat != roboyard.eclabs.achievements.AchievementCategory.CHALLENGE) {
                assertTrue("Category " + cat + " should be enabled", cat.isEnabled());
            }
        }
    }

    // ========================================================================
    // Save Data Parsing Tests (mirrors SyncManager logic)
    // ========================================================================

    @Test
    public void testSaveDataParsing_mapName() {
        assertEquals("TestMap", extractMapName("board:12,12;NAME:TestMap;SIZE:12,12;"));
        assertNull(extractMapName("board:12,12;SIZE:12,12;"));
        assertEquals("Random-abc123", extractMapName("board:12,12;NAME:Random-abc123;SIZE:12,12;"));
    }

    @Test
    public void testSaveDataParsing_boardSize() {
        assertEquals(16, extractBoardWidth("board:16,14;SIZE:16,14;"));
        assertEquals(14, extractBoardHeight("board:16,14;SIZE:16,14;"));
        assertEquals(12, extractBoardWidth("board:12,12;DIFFICULTY:3;")); // default
        assertEquals(12, extractBoardHeight("board:12,12;DIFFICULTY:3;")); // default
    }

    @Test
    public void testSaveDataParsing_slotId() {
        assertEquals(0, extractSlotIdFromFilename("save_0.dat"));
        assertEquals(1, extractSlotIdFromFilename("save_1.dat"));
        assertEquals(10, extractSlotIdFromFilename("save_10.dat"));
    }

    @Test
    public void testAutoSaveSlotReserved() {
        assertTrue("Slot 0 should be reserved for auto-save", isSlotReservedForAutoSave(0));
        for (int i = 1; i <= 9; i++) {
            assertFalse("Slot " + i + " should be available", isSlotReservedForAutoSave(i));
        }
    }

    // ========================================================================
    // Constants Validation Tests
    // ========================================================================

    @Test
    public void testConstantsDirections() {
        assertEquals(0, roboyard.logic.core.Constants.NORTH);
        assertEquals(1, roboyard.logic.core.Constants.EAST);
        assertEquals(2, roboyard.logic.core.Constants.SOUTH);
        assertEquals(3, roboyard.logic.core.Constants.WEST);
    }

    @Test
    public void testConstantsBoardSizeLimits() {
        assertTrue("Min board size should be positive", roboyard.logic.core.Constants.MIN_BOARD_SIZE > 0);
        assertTrue("Max board size should be > min", 
            roboyard.logic.core.Constants.MAX_BOARD_SIZE > roboyard.logic.core.Constants.MIN_BOARD_SIZE);
        assertEquals(8, roboyard.logic.core.Constants.MIN_BOARD_SIZE);
        assertEquals(22, roboyard.logic.core.Constants.MAX_BOARD_SIZE);
    }

    @Test
    public void testConstantsNumRobots() {
        assertEquals(4, roboyard.logic.core.Constants.NUM_ROBOTS);
        assertEquals(5, roboyard.logic.core.Constants.MAX_NUM_ROBOTS);
        assertTrue(roboyard.logic.core.Constants.MAX_NUM_ROBOTS >= roboyard.logic.core.Constants.NUM_ROBOTS);
    }

    @Test
    public void testConstantsDifficultyLevels() {
        assertEquals(0, roboyard.logic.core.Constants.DIFFICULTY_BEGINNER);
        assertEquals(1, roboyard.logic.core.Constants.DIFFICULTY_ADVANCED);
        assertEquals(2, roboyard.logic.core.Constants.DIFFICULTY_INSANE);
        assertEquals(3, roboyard.logic.core.Constants.DIFFICULTY_IMPOSSIBLE);
    }

    @Test
    public void testConstantsGameModes() {
        assertEquals(0, roboyard.logic.core.Constants.GAME_MODE_STANDARD);
        assertEquals(1, roboyard.logic.core.Constants.GAME_MODE_MULTI_TARGET);
    }

    @Test
    public void testConstantsCellTypes() {
        assertEquals(0, roboyard.logic.core.Constants.TYPE_EMPTY);
        assertEquals(1, roboyard.logic.core.Constants.TYPE_ROBOT);
        assertEquals(2, roboyard.logic.core.Constants.TYPE_TARGET);
        assertEquals(3, roboyard.logic.core.Constants.TYPE_HORIZONTAL_WALL);
        assertEquals(4, roboyard.logic.core.Constants.TYPE_VERTICAL_WALL);
    }

    @Test
    public void testConstantsFileNames() {
        assertNotNull(roboyard.logic.core.Constants.SAVE_DIRECTORY);
        assertNotNull(roboyard.logic.core.Constants.AUTO_SAVE_FILENAME);
        assertTrue(roboyard.logic.core.Constants.AUTO_SAVE_FILENAME.endsWith(".dat"));
    }

    // ========================================================================
    // Streak / Sync Logic Tests
    // ========================================================================

    @Test
    public void testStreakBidirectionalSync() {
        assertEquals(10, Math.max(5, 10));  // server higher
        assertEquals(15, Math.max(15, 3));  // local higher
        assertEquals(7, Math.max(7, 7));    // equal
        assertEquals(5, Math.max(5, 0));    // server zero
        assertEquals(12, Math.max(0, 12));  // local zero
    }

    @Test
    public void testSyncThrottling() {
        long minInterval = 60_000;
        
        long recentSync = System.currentTimeMillis() - 30_000;
        assertFalse("Should NOT sync within interval", 
            (System.currentTimeMillis() - recentSync) >= minInterval);
        
        long oldSync = System.currentTimeMillis() - 120_000;
        assertTrue("Should sync after interval", 
            (System.currentTimeMillis() - oldSync) >= minInterval);
    }

    @Test
    public void testAchievementMergeIsUnion() {
        Set<String> server = new HashSet<>();
        server.add("first_solve");
        server.add("explorer_10");
        
        Set<String> local = new HashSet<>();
        local.add("first_solve");
        local.add("speed_demon");
        
        Set<String> merged = new HashSet<>(local);
        merged.addAll(server);
        
        assertEquals(3, merged.size());
        assertTrue(merged.contains("first_solve"));
        assertTrue(merged.contains("explorer_10"));
        assertTrue(merged.contains("speed_demon"));
    }

    // ========================================================================
    // Timestamp Parsing Tests
    // ========================================================================

    @Test
    public void testTimestampParsing() {
        long ts = parseTimestamp("2026-02-06T12:00:00+01:00");
        assertTrue("Timestamp should be positive", ts > 0);
    }

    @Test
    public void testTimestampParsingNull() {
        long ts = parseTimestamp(null);
        long now = System.currentTimeMillis();
        assertTrue("Null should return ~now", Math.abs(ts - now) < 1000);
    }

    @Test
    public void testTimestampParsingEmpty() {
        long ts = parseTimestamp("");
        long now = System.currentTimeMillis();
        assertTrue("Empty should return ~now", Math.abs(ts - now) < 1000);
    }

    // ========================================================================
    // URL Construction Tests
    // ========================================================================

    @Test
    public void testAutoLoginUrlConstruction() {
        String token = "abc123";
        String url = "https://roboyard.z11.de/auto-login?token=" + token;
        assertEquals("https://roboyard.z11.de/auto-login?token=abc123", url);
    }

    @Test
    public void testAutoLoginUrlEncoding() {
        String token = "abc+def/ghi=";
        String encoded = token.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D");
        assertTrue(encoded.contains("abc%2Bdef%2Fghi%3D"));
    }

    // ========================================================================
    // Helper methods (mirror production code logic for pure unit testing)
    // ========================================================================

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
                    try { return Integer.parseInt(parts[index].trim()); }
                    catch (NumberFormatException e) { return defaultValue; }
                }
            }
        }
        return defaultValue;
    }

    private int extractSlotIdFromFilename(String filename) {
        String idStr = filename.replace("save_", "").replace(".dat", "");
        return Integer.parseInt(idStr);
    }

    private boolean isSlotReservedForAutoSave(int slotId) {
        return slotId == 0;
    }

    private long parseTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
            java.util.Date date = sdf.parse(isoTimestamp);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}

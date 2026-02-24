package roboyard.eclabs;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

import roboyard.logic.core.LevelFormatParser;

/**
 * Unit tests for LevelFormatParser.
 *
 * Verifies parsing and serialization of level format entries including:
 * - Basic entries (board, walls, targets, robots)
 * - Comments (lines starting with #)
 * - Optional line breaks between entries
 * - Legacy format (mh, mv, target_color, robot_color)
 * - Compact format (h, v, t<color>, r<color>)
 * - Empty content and edge cases
 *
 * Tags: level-format, parsing, serialization, comments, legacy-format, compact-format
 */
public class LevelFormatParserTest {

    @Test
    public void testParseBasicEntries() {
        String content = "board:12,14;h0,0;v0,0;";
        List<LevelFormatParser.LevelEntry> entries = LevelFormatParser.parseEntries(content);
        
        assertEquals("Should parse 3 entries", 3, entries.size());
        assertEquals("First entry type should be board", "board", entries.get(0).type);
        assertEquals("Second entry type should be h", "h", entries.get(1).type);
        assertEquals("Third entry type should be v", "v", entries.get(2).type);
    }

    @Test
    public void testParseWithComments() {
        String content = "# Comment\nboard:12,14;\n# Another\nh0,0;";
        List<LevelFormatParser.LevelEntry> entries = LevelFormatParser.parseEntries(content);
        
        assertEquals("Should parse 2 entries (comments ignored)", 2, entries.size());
        assertEquals("First entry should be board", "board", entries.get(0).type);
    }

    @Test
    public void testParseWithLineBreaks() {
        String content = "board:12,14;\nh0,0;\nv0,0;";
        List<LevelFormatParser.LevelEntry> entries = LevelFormatParser.parseEntries(content);
        
        assertEquals("Should parse 3 entries (line breaks optional)", 3, entries.size());
    }

    @Test
    public void testParseEmptyContent() {
        String content = "";
        List<LevelFormatParser.LevelEntry> entries = LevelFormatParser.parseEntries(content);
        
        assertEquals("Should parse 0 entries from empty content", 0, entries.size());
    }

    @Test
    public void testParseOnlyComments() {
        String content = "# Comment 1\n# Comment 2";
        List<LevelFormatParser.LevelEntry> entries = LevelFormatParser.parseEntries(content);
        
        assertEquals("Should parse 0 entries (only comments)", 0, entries.size());
    }

    @Test
    public void testSerializeEntries() {
        List<LevelFormatParser.LevelEntry> entries = new java.util.ArrayList<>();
        entries.add(new LevelFormatParser.LevelEntry("board", ":12,14"));
        entries.add(new LevelFormatParser.LevelEntry("h", "0,0"));
        
        String serialized = LevelFormatParser.serializeEntries(entries);
        
        assertTrue("Should contain board entry", serialized.contains("board:12,14;"));
        assertTrue("Should contain h entry", serialized.contains("h0,0;"));
    }

    @Test
    public void testParseLegacyFormat() {
        String content = "mh0,0;mv0,0;target_blue8,7;robot_red1,5;";
        List<LevelFormatParser.LevelEntry> entries = LevelFormatParser.parseEntries(content);
        
        assertEquals("Should parse 4 legacy format entries", 4, entries.size());
        assertEquals("First entry type should be mh", "mh", entries.get(0).type);
        assertEquals("Second entry type should be mv", "mv", entries.get(1).type);
        assertEquals("Third entry type should be target_blue", "target_blue", entries.get(2).type);
        assertEquals("Fourth entry type should be robot_red", "robot_red", entries.get(3).type);
    }
}

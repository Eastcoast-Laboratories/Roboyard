package roboyard.logic.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Central parser/serializer for level format.
 * Supports:
 * - Comments with # (ignored)
 * - Optional line breaks (can be single line or multiple lines)
 * - Compact format: board:W,H; hX,Y; vX,Y; tcolorX,Y; rcolorX,Y;
 * - Legacy format: mh, mv, target_, robot_ (for backward compatibility)
 * 
 * DRY principle: single source of truth for format parsing/serialization
 */
public class LevelFormatParser {
    
    /**
     * Parse level format string into entries
     * Supports comments (#) and optional line breaks
     * @param content Raw level content (may have comments and line breaks)
     * @return List of entries (type + data pairs)
     */
    public static List<LevelEntry> parseEntries(String content) {
        List<LevelEntry> entries = new ArrayList<>();
        
        // Remove comments (everything after # until end of line)
        String cleaned = removeComments(content);
        
        // Filter out lines that don't contain semicolons (board data lines like "0,0,0,0")
        // and lines that are metadata (WIDTH:, HEIGHT:)
        StringBuilder filteredContent = new StringBuilder();
        for (String line : cleaned.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (!trimmed.contains(";")) continue; // Skip board data lines
            filteredContent.append(trimmed);
        }
        cleaned = filteredContent.toString();
        
        // Parse entries: each entry is type + data + semicolon
        // Examples: h0,0; v1,2; tb8,7; rr1,5; mh0,0; mv1,2; target_blue8,7; robot_red1,5;
        // Pattern: (type)(data);
        // Type can be: h, v, t, r, mh, mv, board, solution, num_moves, target_*, robot_*
        // Data is everything between type and semicolon
        
        int pos = 0;
        while (pos < cleaned.length()) {
            // Find next semicolon
            int semiPos = cleaned.indexOf(';', pos);
            if (semiPos == -1) break;
            
            String entry = cleaned.substring(pos, semiPos);
            if (entry.isEmpty()) {
                pos = semiPos + 1;
                continue;
            }
            
            // Parse type and data from entry
            // Type is the leading letters (including underscores for legacy formats)
            int typeEndPos = 0;
            for (int i = 0; i < entry.length(); i++) {
                char c = entry.charAt(i);
                if (Character.isLetter(c) || c == '_') {
                    typeEndPos = i + 1;
                } else {
                    break;
                }
            }
            
            if (typeEndPos > 0) {
                String type = entry.substring(0, typeEndPos);
                String data = entry.substring(typeEndPos);
                
                if (!type.isEmpty()) {
                    entries.add(new LevelEntry(type, data));
                }
            }
            
            pos = semiPos + 1;
        }
        
        Timber.d("[LEVEL_FORMAT] Parsed %d entries from content", entries.size());
        return entries;
    }
    
    /**
     * Serialize entries back to level format string
     * @param entries List of entries to serialize
     * @return Formatted string (one entry per line for readability)
     */
    public static String serializeEntries(List<LevelEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (LevelEntry entry : entries) {
            sb.append(entry.type).append(entry.data).append(";\n");
        }
        return sb.toString();
    }
    
    /**
     * Remove comments from content (# to end of line)
     */
    private static String removeComments(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) {
                line = line.substring(0, commentPos);
            }
            result.append(line).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Represents a single level format entry (type + data)
     * Examples:
     * - type="board", data="10,10"
     * - type="h", data="0,0" (horizontal wall)
     * - type="v", data="0,0" (vertical wall)
     * - type="tb", data="8,7" (target blue)
     * - type="rr", data="1,5" (robot red)
     * - type="mh", data="0,0" (legacy horizontal wall)
     * - type="mv", data="0,0" (legacy vertical wall)
     * - type="target_blue", data="8,7" (legacy target)
     * - type="robot_red", data="1,5" (legacy robot)
     */
    public static class LevelEntry {
        public String type;
        public String data;
        
        public LevelEntry(String type, String data) {
            this.type = type;
            this.data = data;
        }
        
        @Override
        public String toString() {
            return type + data + ";";
        }
    }
}

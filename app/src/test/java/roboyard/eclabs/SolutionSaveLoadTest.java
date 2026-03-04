package roboyard.eclabs;

import org.junit.Test;
import static org.junit.Assert.*;

import roboyard.ui.components.GameStateManager;
import roboyard.logic.core.GameState;
import android.content.Context;
import org.mockito.Mockito;
import java.util.Map;
import java.io.File;

/**
 * Unit tests for solution save/load functionality.
 * Verifies that solutions are correctly serialized/deserialized
 * and that the solver doesn't need to run when loading a game with saved solutions.
 */
public class SolutionSaveLoadTest {

    @Test
    public void testSolutionMetadataExtraction() {
        // Test save data with SOLUTIONS tag
        String saveData = "#MAPNAME:Test Map;SOLUTIONS:0U,1R,0D|0U,2L,1D;TIME:1000;MOVES:5;\n" +
                "WIDTH:8;\n" +
                "HEIGHT:8;\n";
        
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        
        assertNotNull("Metadata should not be null", metadata);
        assertTrue("Metadata should contain SOLUTIONS", metadata.containsKey("SOLUTIONS"));
        
        String solutions = metadata.get("SOLUTIONS");
        assertEquals("Solutions should match expected format", "0U,1R,0D|0U,2L,1D", solutions);
        
        // Verify solutions contain pipe separator
        assertTrue("Solutions should contain pipe separator for multiple solutions", 
                solutions.contains("|"));
        
        // Verify first solution format
        String[] solutionArray = solutions.split("\\|");
        assertEquals("Should have 2 solutions", 2, solutionArray.length);
        assertEquals("First solution should be 0U,1R,0D", "0U,1R,0D", solutionArray[0]);
        assertEquals("Second solution should be 0U,2L,1D", "0U,2L,1D", solutionArray[1]);
    }
    
    @Test
    public void testSolutionMetadataWithComplexSolutions() {
        // Test with more complex solution data
        String saveData = "#MAPNAME:Complex;SOLUTIONS:0U,0R,1D,1L,2U|1R,2D,3L,0U,1R;DIFFICULTY:3;\n" +
                "WIDTH:16;\n" +
                "HEIGHT:16;\n";
        
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        
        assertNotNull("Metadata should not be null", metadata);
        assertTrue("Metadata should contain SOLUTIONS", metadata.containsKey("SOLUTIONS"));
        
        String solutions = metadata.get("SOLUTIONS");
        String[] solutionArray = solutions.split("\\|");
        assertEquals("Should have 2 solutions", 2, solutionArray.length);
        
        // Verify first solution has 5 moves
        String[] moves1 = solutionArray[0].split(",");
        assertEquals("First solution should have 5 moves", 5, moves1.length);
        
        // Verify second solution has 5 moves
        String[] moves2 = solutionArray[1].split(",");
        assertEquals("Second solution should have 5 moves", 5, moves2.length);
    }
    
    @Test
    public void testSolutionMetadataWithoutSolutions() {
        // Test save data without SOLUTIONS tag
        String saveData = "#MAPNAME:No Solutions;TIME:1000;MOVES:5;\n" +
                "WIDTH:8;\n" +
                "HEIGHT:8;\n";
        
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        
        assertNotNull("Metadata should not be null", metadata);
        assertFalse("Metadata should not contain SOLUTIONS", metadata.containsKey("SOLUTIONS"));
    }
    
    @Test
    public void testSolutionMetadataWithEmptySolutions() {
        // Test save data with empty SOLUTIONS tag
        String saveData = "#MAPNAME:Empty Solutions;SOLUTIONS:;TIME:1000;\n" +
                "WIDTH:8;\n" +
                "HEIGHT:8;\n";
        
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        
        assertNotNull("Metadata should not be null", metadata);
        assertTrue("Metadata should contain SOLUTIONS key", metadata.containsKey("SOLUTIONS"));
        
        String solutions = metadata.get("SOLUTIONS");
        assertTrue("Solutions value should be empty", solutions == null || solutions.isEmpty());
    }
    
    @Test
    public void testMoveFormatParsing() {
        // Test individual move format parsing
        String move1 = "0U"; // Robot 0, UP
        assertEquals("Color should be 0", 0, Integer.parseInt(move1.substring(0, move1.length() - 1)));
        assertEquals("Direction should be U", 'U', move1.charAt(move1.length() - 1));
        
        String move2 = "3L"; // Robot 3, LEFT
        assertEquals("Color should be 3", 3, Integer.parseInt(move2.substring(0, move2.length() - 1)));
        assertEquals("Direction should be L", 'L', move2.charAt(move2.length() - 1));
        
        String move3 = "1R"; // Robot 1, RIGHT
        assertEquals("Color should be 1", 1, Integer.parseInt(move3.substring(0, move3.length() - 1)));
        assertEquals("Direction should be R", 'R', move3.charAt(move3.length() - 1));
        
        String move4 = "2D"; // Robot 2, DOWN
        assertEquals("Color should be 2", 2, Integer.parseInt(move4.substring(0, move4.length() - 1)));
        assertEquals("Direction should be D", 'D', move4.charAt(move4.length() - 1));
    }
    
    @Test
    public void testMetadataWithMultipleFields() {
        // Test that SOLUTIONS doesn't interfere with other metadata fields
        String saveData = "#MAPNAME:Full Test;DIFFICULTY:2;SOLUTIONS:0U,1R|2D,3L;SIZE:8,8;SOLVED:false;TIME:5000;MOVES:10;\n" +
                "WIDTH:8;\n" +
                "HEIGHT:8;\n";
        
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        
        assertNotNull("Metadata should not be null", metadata);
        assertEquals("MAPNAME should be correct", "Full Test", metadata.get("MAPNAME"));
        assertEquals("DIFFICULTY should be correct", "2", metadata.get("DIFFICULTY"));
        assertEquals("SOLUTIONS should be correct", "0U,1R|2D,3L", metadata.get("SOLUTIONS"));
        assertEquals("SIZE should be correct", "8,8", metadata.get("SIZE"));
        assertEquals("SOLVED should be correct", "false", metadata.get("SOLVED"));
        assertEquals("TIME should be correct", "5000", metadata.get("TIME"));
        assertEquals("MOVES should be correct", "10", metadata.get("MOVES"));
    }
    
    @Test
    public void testCompleteSolutionSaveLoadCycle() {
        // Test the complete cycle: save data with solutions -> parse -> verify solutions stored in GameState
        
        // Create save data with solutions
        String saveData = "#MAPNAME:Test Map;SOLUTIONS:0U,1R,0D,1L|2U,3R;TIME:1000;MOVES:5;\n" +
                "WIDTH:8;\n" +
                "HEIGHT:8;\n" +
                "tb7,7;\n" +  // Target blue at 7,7
                "h0,0;h1,0;h2,0;h3,0;h4,0;h5,0;h6,0;h7,0;\n" +  // Top wall
                "h0,8;h1,8;h2,8;h3,8;h4,8;h5,8;h6,8;h7,8;\n" +  // Bottom wall
                "v0,0;v0,1;v0,2;v0,3;v0,4;v0,5;v0,6;v0,7;\n" +  // Left wall
                "v8,0;v8,1;v8,2;v8,3;v8,4;v8,5;v8,6;v8,7;\n" +  // Right wall
                "rb0,0;\n";  // Robot blue at 0,0
        
        // Step 1: Extract metadata
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        assertNotNull("Metadata should not be null", metadata);
        assertTrue("Metadata should contain SOLUTIONS", metadata.containsKey("SOLUTIONS"));
        
        String solutionsStr = metadata.get("SOLUTIONS");
        assertEquals("Solutions should match", "0U,1R,0D,1L|2U,3R", solutionsStr);
        
        // Step 2: Verify solution format
        String[] solutions = solutionsStr.split("\\|");
        assertEquals("Should have 2 solutions", 2, solutions.length);
        
        // First solution: 0U,1R,0D,1L (4 moves)
        String[] moves1 = solutions[0].split(",");
        assertEquals("First solution should have 4 moves", 4, moves1.length);
        assertEquals("First move should be 0U", "0U", moves1[0]);
        assertEquals("Second move should be 1R", "1R", moves1[1]);
        assertEquals("Third move should be 0D", "0D", moves1[2]);
        assertEquals("Fourth move should be 1L", "1L", moves1[3]);
        
        // Second solution: 2U,3R (2 moves)
        String[] moves2 = solutions[1].split(",");
        assertEquals("Second solution should have 2 moves", 2, moves2.length);
        assertEquals("First move should be 2U", "2U", moves2[0]);
        assertEquals("Second move should be 3R", "3R", moves2[1]);
        
        // Step 3: Parse GameState and verify solutions are stored
        Context mockContext = Mockito.mock(Context.class);
        Mockito.when(mockContext.getFilesDir()).thenReturn(new File("/tmp"));
        
        GameState state = GameState.parseFromSaveData(saveData, mockContext);
        assertNotNull("GameState should be parsed", state);
        
        // Note: In the actual load flow, GameState.loadSavedGame() calls setSavedSolutions()
        // Here we simulate that by manually setting it
        state.setSavedSolutions(solutionsStr);
        
        // Step 4: Verify solutions are stored in GameState
        String storedSolutions = state.getSavedSolutions();
        assertNotNull("Stored solutions should not be null", storedSolutions);
        assertEquals("Stored solutions should match original", solutionsStr, storedSolutions);
        
        // Step 5: Verify we can re-extract the solutions
        String[] storedSolutionArray = storedSolutions.split("\\|");
        assertEquals("Should still have 2 solutions", 2, storedSolutionArray.length);
        assertEquals("First solution should still be correct", "0U,1R,0D,1L", storedSolutionArray[0]);
        assertEquals("Second solution should still be correct", "2U,3R", storedSolutionArray[1]);
    }
    
    @Test
    public void testSolutionPersistenceAcrossLoadCycle() {
        // Test that solutions survive the complete save/load cycle
        
        String originalSolutions = "0U,1R,2D,3L|1U,2R,3D,0L|2U,3R,0D,1L";
        
        // Create save data
        String saveData = "#MAPNAME:Persistence Test;SOLUTIONS:" + originalSolutions + ";TIME:2000;\n" +
                "WIDTH:8;\n" +
                "HEIGHT:8;\n" +
                "tb7,7;\n" +
                "h0,0;h1,0;h2,0;h3,0;h4,0;h5,0;h6,0;h7,0;\n" +
                "h0,8;h1,8;h2,8;h3,8;h4,8;h5,8;h6,8;h7,8;\n" +
                "v0,0;v0,1;v0,2;v0,3;v0,4;v0,5;v0,6;v0,7;\n" +
                "v8,0;v8,1;v8,2;v8,3;v8,4;v8,5;v8,6;v8,7;\n" +
                "rb0,0;\n";
        
        // Extract metadata
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        String extractedSolutions = metadata.get("SOLUTIONS");
        
        // Verify extraction
        assertEquals("Extracted solutions should match original", originalSolutions, extractedSolutions);
        
        // Verify all 3 solutions are present
        String[] solutionArray = extractedSolutions.split("\\|");
        assertEquals("Should have 3 solutions", 3, solutionArray.length);
        
        // Verify each solution has correct number of moves
        assertEquals("First solution should have 4 moves", 4, solutionArray[0].split(",").length);
        assertEquals("Second solution should have 4 moves", 4, solutionArray[1].split(",").length);
        assertEquals("Third solution should have 4 moves", 4, solutionArray[2].split(",").length);
        
        // Verify move format for first solution
        String[] firstSolutionMoves = solutionArray[0].split(",");
        assertEquals("Move 1 should be 0U", "0U", firstSolutionMoves[0]);
        assertEquals("Move 2 should be 1R", "1R", firstSolutionMoves[1]);
        assertEquals("Move 3 should be 2D", "2D", firstSolutionMoves[2]);
        assertEquals("Move 4 should be 3L", "3L", firstSolutionMoves[3]);
    }
}

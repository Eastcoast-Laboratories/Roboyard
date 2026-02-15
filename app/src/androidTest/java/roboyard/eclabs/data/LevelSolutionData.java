package roboyard.eclabs.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub class for level solution data used in tests.
 * Run LevelSolutionGeneratorTest to populate with actual data.
 */
public class LevelSolutionData {

    private static final Map<Integer, Integer> optimalMoves = new HashMap<>();

    /**
     * Get the optimal number of moves for a given level.
     * @param levelId The level ID
     * @return The optimal move count, or -1 if not available
     */
    public static int getOptimalMoves(int levelId) {
        Integer moves = optimalMoves.get(levelId);
        return moves != null ? moves : -1;
    }
}

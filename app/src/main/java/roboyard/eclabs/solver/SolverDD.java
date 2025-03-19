package roboyard.eclabs.solver;

import java.util.ArrayList;
import java.util.List;

import driftingdroids.model.Move;
import roboyard.eclabs.GridElement;
import driftingdroids.model.Board;
import driftingdroids.model.Solver;
import driftingdroids.model.Solution;
import roboyard.eclabs.ui.GameMove;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.ricochet.ERRGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.pm.ia.ricochet.RRGetMap;
import roboyard.pm.ia.ricochet.RRPiece;
import android.util.Log;

/**
 * Bridge implementation between Roboyard's solver interface and the DriftingDroids solver.
 * This class is responsible for:
 * 1. Converting Roboyard's game state to DriftingDroids board format
 * 2. Running the DriftingDroids solver to find solutions
 * 3. Converting DriftingDroids solutions back to Roboyard's format
 *
 * @author Pierre Michel
 * @since 08/03/2015
 */
public class SolverDD implements ISolver{
    private static final String TAG = "SolverDD";

    private SolverStatus solverStatus;
    private Solver solver;
    private List<Solution> solutions;
    private final RRPiece[] pieces;
    private Board board;

    public SolverDD(){
        solver = null;
        solverStatus = SolverStatus.idle;
        solutions = null;
        pieces = new RRPiece[4];
        board = null;
    }

    @Override
    public void init(ArrayList<GridElement> elements){
        // Reset solver state
        solver = null;
        solutions = null;
        solverStatus = SolverStatus.idle;
        
        // Initialize new board and solver
        board = RRGetMap.createDDWorld(elements, pieces);
        solver = Solver.createInstance(board);
    }
    
    @Override
    public void init(int[][] boardData) {
        // Convert 2D array to list of GridElements
        ArrayList<GridElement> elements = new ArrayList<>();
        for (int y = 0; y < boardData.length; y++) {
            for (int x = 0; x < boardData[y].length; x++) {
                int cellType = boardData[y][x];
                if (cellType != 0) { // Skip empty cells
                    elements.add(new GridElement(x, y, cellType));
                }
            }
        }
        
        // Call the original init method
        init(elements);
    }

    @Override
    public void run() {
        if(solver == null){
            return;
        }

        solverStatus = SolverStatus.solving;

        try {
            solutions = solver.execute();
            if(solutions.size() != 0){
                Solution solution = solutions.get(0);
                Log.d(TAG, solutions.size() + " solution(s) found; first solution:");
                Log.d(TAG, solution.toString());
                solverStatus = SolverStatus.solved;
            }else{
                solverStatus = SolverStatus.noSolution;
            }
        }catch(InterruptedException e){
            solverStatus = SolverStatus.noSolution;
        }
    }

    @Override
    public SolverStatus getSolverStatus(){
        return this.solverStatus;
    }

    @Override
    public List<Solution> getSolutionList(){
        return this.solutions;
    }

    /**
     * get the solution number num from the list of found different solutions and add all moves to the result
     * @param num number of the solution in the solutions list
     * @return GameSolution with all moves in that solution
     */
    @Override
    public GameSolution getSolution(int num){
        GameSolution s = new GameSolution();
        if (solutions == null || solutions.isEmpty() || num >= solutions.size()) {
            return s; // Return empty solution if no solutions available
        }
        
        Solution solution = solutions.get(num);
        solution.resetMoves();
        Move m = solution.getNextMove();
        while (m != null){
            ERRGameMove mv;
            if(m.getAxis() == 0){
                if(m.getDir() < 0){
                    mv = new RRGameMove(m.getRobot(), ERRGameMove.LEFT, m.getDistance());
                }else{
                    mv = new RRGameMove(m.getRobot(), ERRGameMove.RIGHT, m.getDistance());
                }
            }else{
                if(m.getDir() < 0){
                    mv = new RRGameMove(m.getRobot(), ERRGameMove.UP, m.getDistance());
                }else{
                    mv = new RRGameMove(m.getRobot(), ERRGameMove.DOWN, m.getDistance());
                }
            }
            s.addMove(mv);
            m = solution.getNextMove();
        }
        return s;
    }
    
    /**
     * Get the next move hint for the current board state
     * @param boardData Current board state
     * @return Next move hint or null if no solution is found
     */
    @Override
    public GameMove getNextMove(int[][] boardData) {
        // Initialize the solver with the current board state
        init(boardData);
        
        // Run the solver to find solutions
        solverStatus = SolverStatus.solving;
        try {
            solutions = solver.execute();
            if(solutions != null && !solutions.isEmpty()){
                // Get the first solution
                Solution solution = solutions.get(0);
                solution.resetMoves();
                
                // Get the first move of the solution
                Move m = solution.getNextMove();
                if (m != null) {
                    // Convert DriftingDroids Move to GameMove
                    int robotIndex = m.getRobot();
                    int direction = 0;
                    
                    if(m.getAxis() == 0){ // Horizontal movement
                        direction = (m.getDir() < 0) ? GameMove.LEFT : GameMove.RIGHT;
                    } else { // Vertical movement
                        direction = (m.getDir() < 0) ? GameMove.UP : GameMove.DOWN;
                    }
                    
                    return new GameMove(robotIndex, direction, m.getDistance());
                }
            }
        } catch(InterruptedException e){
            Log.e(TAG, "Solver interrupted", e);
        }
        
        // Return null if no solution found
        return null;
    }

    /**
     * Check if the solution can be reached in one move
     * @return true if the goal can be reached in one move
     */
    @Override
    public boolean isSolution01(){
        if (solutions == null || solutions.isEmpty()) {
            return false;
        }
        
        Solution solution = solutions.get(0);
        solution.resetMoves();
        Move m = solution.getNextMove();
        int moveCount = 0;
        
        while (m != null) {
            moveCount++;
            m = solution.getNextMove();
        }
        
        return moveCount == 1;
    }
}

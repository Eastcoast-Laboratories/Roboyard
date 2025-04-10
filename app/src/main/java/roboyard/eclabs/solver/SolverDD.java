package roboyard.eclabs.solver;

import java.util.ArrayList;
import java.util.List;

import driftingdroids.model.Move;
import roboyard.eclabs.GameManager;
import driftingdroids.model.Board;
import driftingdroids.model.Solver;
import driftingdroids.model.Solution;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GridElement;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.ricochet.ERRGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.pm.ia.ricochet.RRGetMap;
import roboyard.pm.ia.ricochet.RRPiece;
import timber.log.Timber;

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

    private SolverStatus solverStatus;
    private Solver solver;
    private List<Solution> solutions;
    private final RRPiece[] pieces;
    private Board board;

    public SolverDD(){
        solver = null;
        solverStatus = SolverStatus.idle;
        solutions = null;
        pieces = new RRPiece[Constants.NUM_ROBOTS];
        board = null;
    }

    public void init(ArrayList<GridElement> elements){
        // Reset solver state
        solver = null;
        solutions = null;
        solverStatus = SolverStatus.idle;
        
        Timber.d("[SOLUTION SOLVER] SolverDD.init(): Initializing solver with %d grid elements", elements.size());
        
        // Log some sample elements to verify data
        if (elements.size() > 0) {
            Timber.d("[SOLUTION SOLVER] SolverDD.init(): First few elements:");
            for (int i = 0; i < Math.min(5, elements.size()); i++) {
                GridElement element = elements.get(i);
                Timber.d("[SOLUTION SOLVER] Element %d: type=%s, position=(%d,%d)", i, element.getType(), element.getX(), element.getY());
            }
        }
        
        // Initialize new board and solver
        Timber.d("[SOLUTION SOLVER] SolverDD.init(): Creating DD World from elements");
        board = RRGetMap.createDDWorld(elements, pieces);
        
        // Log robot pieces information
        Timber.d("[SOLUTION SOLVER] SolverDD.init(): Robot pieces after initialization:");
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i] != null) {
                Timber.d("[SOLUTION SOLVER] Robot %d: color=%d, position=(%d,%d)", 
                    i, pieces[i].getColor(), pieces[i].getX(), pieces[i].getY());
            } else {
                Timber.d("[SOLUTION SOLVER] Robot %d: null", i);
            }
        }
        
        solver = Solver.createInstance(board);
        Timber.d("[SOLUTION SOLVER] SolverDD.init(): Solver created successfully");
    }

    @Override
    public void run() {

        if(solver == null){
            Timber.d("[SOLUTION SOLVER] SolverDD.run(): solver is null, aborting");
            return;
        }

        // Check if outer walls are complete before running the solver
        if (!outerWallsAreComplete()) {
            Timber.e("[SOLUTION SOLVER] Incomplete outer walls detected! Aborting solver to prevent crash.");
            solverStatus = SolverStatus.missingData;
            return;
        }

        solverStatus = SolverStatus.solving;
        Timber.d("[SOLUTION SOLVER] SolverDD.run(): Starting solver with status %s", solverStatus);

        try {
            Timber.d("[SOLUTION SOLVER] SolverDD.run(): Executing solver");
            solutions = solver.execute();
            Timber.d("[SOLUTION SOLVER] SolverDD.run(): Solver execution complete");
            
            if(solutions.size() != 0){
                Solution solution = solutions.get(0);
                Timber.d("[SOLUTION SOLVER] %d solution(s) found; first solution:", solutions.size());
                Timber.d("[SOLUTION SOLVER] %s", solution.toString());
                solverStatus = SolverStatus.solved;
                Timber.d("[SOLUTION SOLVER] SolverDD.run(): Status set to %s", solverStatus);
            }else{
                Timber.d("[SOLUTION SOLVER] SolverDD.run(): No solutions found");
                solverStatus = SolverStatus.noSolution;
            }
        }catch(InterruptedException e){
            Timber.e(e, "[SOLUTION SOLVER] SolverDD.run(): Solver interrupted");
            solverStatus = SolverStatus.noSolution;
        }
    }

    public SolverStatus getSolverStatus(){
        return this.solverStatus;
    }

    public List<Solution> getSolutionList(){
        return this.solutions;
    }

    /**
     * get the solution number num from the list of found different solutions and add all moves to the result
     * TODO: @param num number of the solution in the solutions list
     * @return GameSolution with all moves in that solution
     */
    public GameSolution getSolution(int num){
        if (solutions == null || num >= solutions.size()) {
            Timber.d("[SOLUTION SOLVER] getSolution(%d): Solutions null or index out of range", num);
            return null;
        }

        Timber.d("[SOLUTION SOLVER] getSolution(%d): Creating GameSolution from DriftingDroids solution", num);
        GameSolution s = new GameSolution();
        Solution solution = solutions.get(num);
        solution.resetMoves();
        Move m = solution.getNextMove();
        int moveCount = 0;
        
        while (m != null){
            moveCount++;
            ERRGameMove mv;
            switch(m.direction){
                case 0:
                    mv = ERRGameMove.UP;
                    break;
                case 1:
                    mv = ERRGameMove.RIGHT;
                    break;
                case 2:
                    mv = ERRGameMove.DOWN;
                    break;
                case 3:
                    mv = ERRGameMove.LEFT;
                    break;
                default:
                    mv = ERRGameMove.NOMOVE;
                    break;
            }
            System.out.print(m.direction+","+pieces[m.robotNumber].getColor()+";");
            s.addMove(new RRGameMove(pieces[m.robotNumber], mv));
            m = solution.getNextMove();
        }
        
        Timber.d("[SOLUTION SOLVER] getSolution(%d): Created GameSolution with %d moves", num, moveCount);
        return s;
    }

    /**
     * Cancel the solver execution and set status to noSolution
     */
    public void cancel() {
        this.solverStatus = SolverStatus.noSolution;
    }

    /**
     * Check if the solution can be reached in one move
     * @return true if the goal can be reached in one move
     */
    public boolean isSolution01() {
        return board != null && board.isSolution01();
    }

    private boolean outerWallsAreComplete() {
        if (board == null) {
            Timber.e("[SOLUTION SOLVER][OUTER WALLS] Cannot check outer walls: board is null");
            return false;
        }
        
        int width = board.width;
        int height = board.height;
        
        Timber.d("[SOLUTION SOLVER][OUTER WALLS] Checking outer walls for board dimensions %d x %d", width, height);
        // Check top border (horizontal walls)
        for (int x = 0; x < width; x++) {
            int position = 0 * width + x; // y=0, first row
            if (!board.isWall(position, Constants.NORTH)) {
                Timber.e("[SOLUTION SOLVER][OUTER WALLS] Missing top wall at x=%d", x);
                return false;
            }
        }
        
        // Check bottom border (horizontal walls)
        for (int x = 0; x < width; x++) {
            int position = (height-1) * width + x; // y=height-1, last row
            if (!board.isWall(position, Constants.SOUTH)) {
                Timber.e("[SOLUTION SOLVER][OUTER WALLS] Missing bottom wall at x=%d", x);
                return false;
            }
        }
        
        // Check left border (vertical walls)
        for (int y = 0; y < height; y++) {
            int position = y * width + 0; // x=0, first column
            if (!board.isWall(position, Constants.WEST)) {
                Timber.e("[SOLUTION SOLVER][OUTER WALLS] Missing left wall at y=%d", y);
                return false;
            }
        }
        
        // Check right border (vertical walls)
        for (int y = 0; y < height; y++) {
            int position = y * width + (width-1); // x=width-1, last column
            if (!board.isWall(position, Constants.EAST)) {
                Timber.e("[SOLUTION SOLVER][OUTER WALLS] Missing right wall at y=%d", y);
                return false;
            }
        }
        
        Timber.d("[SOLUTION SOLVER][OUTER WALLS] All outer walls are present");
        return true;
    }
}

package roboyard.eclabs.solver;


import java.util.ArrayList;
import java.util.List;

import driftingdroids.model.Move;
import roboyard.eclabs.GameManager;
import roboyard.eclabs.GridElement;
import driftingdroids.model.Board;
import driftingdroids.model.Solver;
import driftingdroids.model.Solution;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.ricochet.ERRGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.pm.ia.ricochet.RRGetMap;
import roboyard.pm.ia.ricochet.RRPiece;

/**
 * Created by Pierre on 08/03/2015.
 */
public class SolverDD implements ISolver{

    private SolverStatus solverStatus;
    private Solver solver;
    private List<Solution> solutions;
    private final RRPiece[] pieces;

    public SolverDD(){
        solver = null;
        solverStatus = SolverStatus.idle;
        solutions = null;
        pieces = new RRPiece[4];
    }

    public void init(ArrayList<GridElement> elements){
        // Reset solver state
        solver = null;
        solutions = null;
        solverStatus = SolverStatus.idle;
        
        // Initialize new board and solver
        Board board = RRGetMap.createDDWorld(elements, pieces);
        solver = Solver.createInstance(board);
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
                System.out.println(solutions.size() + " solution(s) found; first solution:");
                System.out.println(solution.toString());
                solverStatus = SolverStatus.solved;
            }else{
                solverStatus = SolverStatus.noSolution;
            }
        }catch(InterruptedException e){
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
        GameSolution s = new GameSolution();
        Solution solution = solutions.get(num);
        solution.resetMoves();
        Move m = solution.getNextMove();
        while (m != null){

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
        System.out.println();
        return s;
    }

}

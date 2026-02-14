package roboyard.logic.core;

import java.util.ArrayList;

/**
 * Represents a solution to a puzzle.
 * A solution consists of a sequence of moves that lead from the initial state
 * to a state where a robot reaches its target position.
 * 
 * This class is used by both the game logic and the solver components to
 * store and manipulate solution sequences.
 *
 * @see roboyard.logic.core.IGameMove
 */
public class GameSolution {
  
  public GameSolution(){
    this.moves = new ArrayList<IGameMove>();
  }

  public void addMove(IGameMove move){
        this.moves.add(move);
    }
  
  public ArrayList<IGameMove> getMoves(){
    return this.moves;
  }
  
  private final ArrayList<IGameMove> moves;
  
}

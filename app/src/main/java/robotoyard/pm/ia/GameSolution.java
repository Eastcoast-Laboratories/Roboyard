package robotoyard.pm.ia;

import java.util.ArrayList;

/**
 *
 * @author usrlocal
 */
public class GameSolution {
  
  public GameSolution(){
    this.moves = new ArrayList<IGameMove>();
  }

  public void addMove(IGameMove move){
        this.moves.add(move);
    }
  
  public void insertMove(IGameMove move){
    this.moves.add(0, move);
  }
  
  public ArrayList<IGameMove> getMoves(){
    return this.moves;
  }
  
  private ArrayList<IGameMove> moves;
  
}

package roboyard.pm.ia;

import java.util.ArrayList;

/**
 *
 * @author Pierre Michel
 */
public abstract class AGameState {
  
  public AGameState(AGameState parentState, IGameMove previousMove){
    this.derivedStates = new ArrayList<>();
    this.parentState = parentState;
    this.previousMove = previousMove;
    if(parentState != null){
      this.depth = parentState.depth+1;
    }
  }
  
  public abstract ArrayList<AGameState> computeDerivedStates(AWorld world);
  public abstract long computeHash(AWorld world);
  
  public AGameState getParentState(){
    return this.parentState;
  }
  
  public IGameMove getPreviousMove(){
    return this.previousMove;
  }
  
  public int getDepth(){
    return this.depth;
  }
  
  public void setDepth(int depth){
    this.depth = depth;
  }
  
  protected final ArrayList<AGameState> derivedStates;
  protected final AGameState parentState;
  protected final IGameMove previousMove;
  protected int depth=1;
  
}

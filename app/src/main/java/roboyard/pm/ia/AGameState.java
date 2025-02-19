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
  
  protected final ArrayList<AGameState> derivedStates;
  protected final AGameState parentState;
  protected final IGameMove previousMove;
  protected int depth=1;
  
}

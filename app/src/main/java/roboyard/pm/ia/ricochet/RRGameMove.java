package roboyard.pm.ia.ricochet;

import roboyard.pm.ia.IGameMove;

/**
 *
 * @author Pierre Michel
 * @author Alain Caillaud
 */
public class RRGameMove implements IGameMove{
  
  public RRGameMove(RRPiece actor, ERRGameMove move){
    this.actor = actor;
    this.move = move;
  }
  
  public int getColor(){
    return this.actor.getColor();
  }
  
  public int getDirection(){
    return this.move.getDirection();
  }
  
  public ERRGameMove getMove(){
    return move;
  }
  
  @Override
  public String toString(){
    return String.format("%d -> %s", this.actor.getId(), this.move.toString());
  }
  
  private final RRPiece actor;
  private final ERRGameMove move;
}

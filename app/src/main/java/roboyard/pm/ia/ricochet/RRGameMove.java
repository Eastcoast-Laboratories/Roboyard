package roboyard.pm.ia.ricochet;

import roboyard.pm.ia.IGameMove;

/**
 * Represents a single move in the game.
 * A move consists of a robot piece and its movement direction.
 * The robot will move in the specified direction until it hits
 * a wall or another robot.
 *
 * This class implements the IGameMove interface and is used by both
 * the game logic and the solver to track robot movements.
 *
 * @author Pierre Michel
 * @author Alain Caillaud
 * @see roboyard.pm.ia.IGameMove
 * @see roboyard.pm.ia.ricochet.RRPiece
 * @see roboyard.pm.ia.ricochet.ERRGameMove
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

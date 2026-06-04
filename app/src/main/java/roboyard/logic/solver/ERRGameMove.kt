package roboyard.pm.ia.ricochet;

/**
 *
 * @author Pierre Michel
 */
public enum ERRGameMove {
  
  NOMOVE(0), UP(1), RIGHT(2), DOWN(4), LEFT(8);
  
  private final int direction;
  
  public int getDirection(){
    return this.direction;
  }
  
  @Override
  public String toString(){
    switch(this.direction){
      case 0:
        return "No Move";
      case 1:
        return "Up";
      case 2:
        return "Right";
      case 4:
        return "Down";
      case 8:
        return "Left";
      default:
        return "Unknown?";
    }
  }
  
  ERRGameMove(int direction){
    this.direction = direction;
  }
  
}

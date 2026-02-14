package roboyard.pm.ia.ricochet;

import roboyard.pm.ia.AGameState;
import roboyard.pm.ia.IGameMove;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Pierre Michel
 */
public class RRGameState extends AGameState {
  
  public RRGameState(AGameState parentState, IGameMove previousMove){
    super(parentState, previousMove);
    if(parentState != null){
      this.allPieces = new ArrayList<>();
      this.mainPieces = new RRPiece[((RRGameState)parentState).mainPieces.length];
      this.secondaryPieces = new RRPiece[((RRGameState)parentState).secondaryPieces.length];
      for(int i=0; i<((RRGameState)parentState).mainPieces.length; i++){
        this.mainPieces[i] = new RRPiece(((RRGameState)parentState).mainPieces[i]);
      }
      for(int i=0; i<((RRGameState)parentState).secondaryPieces.length; i++){
        this.secondaryPieces[i] = new RRPiece(((RRGameState)parentState).secondaryPieces[i]);
      }
      this.allPieces.addAll(Arrays.asList(mainPieces));
      this.allPieces.addAll(Arrays.asList(secondaryPieces));
    }
  }

  @Override
  public String toString(){
    StringBuilder str = new StringBuilder();
    str.append(this.hashCode());
    str.append("\nMain Piece:\n");
    for(RRPiece p : this.mainPieces){
      str.append(String.format("%d -> x:%d, y:%d, color:%d\n", p.hashCode(), p.getX(), p.getY(), p.getColor()));
    }
    str.append("Secondary Pieces:\n");
    for(RRPiece p : this.secondaryPieces){
      str.append(String.format("%d -> x:%d, y:%d, color:%d\n", p.hashCode(), p.getX(), p.getY(), p.getColor()));
    }
    
    return str.toString();
  }
  
  private ArrayList<RRPiece> allPieces;
  private RRPiece[] secondaryPieces;
  private RRPiece[] mainPieces;
  
}

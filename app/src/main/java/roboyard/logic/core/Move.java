package roboyard.eclabs;

/**
 * Created by Alain on 25/03/2015.
 */
public class Move {
    public final GamePiece _p;
    public final int _x;
    public final int _y;
    private int squaresMoved;

    public Move(GamePiece p, int x, int y){
        _p = p;
        _x = x;
        _y = y;
    }

    public void goBack(){
        _p.setX(_x);
        _p.setY(_y);
        _p.setxObjective(_x);
        _p.setyObjective(_y);
    }

    public int getSquaresMoved(){
        return this.squaresMoved;
    }

    public void setSquaresMoved(int moved){
        this.squaresMoved=moved;
    }
}

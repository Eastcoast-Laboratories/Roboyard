package roboyard.eclabs;

import android.graphics.Color;

/**
 * Created by Pierre on 25/02/2015.
 */
public class GamePiece implements IGameObject {
    private int x                   = 0;
    private int y                   = 0;
    private int xObjective          = 0;
    private int yObjective          = 0;
    private int xGrid               = 400;
    private int yGrid               = 500;
    private int xDraw               = 0;
    private int yDraw               = 0;
    private float widthCell         = MainActivity.boardSizeX;
    private float heightCell        = MainActivity.boardSizeY;
    private int radius              = 32;
    private int color               = Color.RED;
    private boolean inMovement      = false;
    private int deltaX              = 0;
    private int deltaY              = 0;
    private int curMoveSquares      = 0;
    // private int numSquaresMoved     = 0;
    private final int initialSpeed        = 16;
    private final int extraSizeForRobotsAndTargets = 1; // robots and targets are 1px larger than the grid and may overlap 1 px
    private final int toleranceForInputManagerTouch = 1000; // virtual circle around robot to touch

    private boolean testIfWon       = true;

    private int image               = 0;

    public void setY(int y) {
        this.y = y;
        deltaY = 0;
    }

    public void setX(int x) {
        this.x = x;
        deltaX = 0;
    }

    public int getColor() {
        return color;
    }

    public int getxObjective() {
        return xObjective;
    }

    public int getyObjective() {
        return yObjective;
    }

    public void setyObjective(int yObjective) {
        this.yObjective = yObjective;
    }

    public void setxObjective(int xObjective) {
        this.xObjective = xObjective;
    }
    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    /**
     * Constructor
     * @param x
     * @param y
     * @param color
     */
    public GamePiece(int x, int y, int color){
        this.x = x;
        this.y = y;
        this.xObjective = x;
        this.yObjective = y;
        this.color = color;
        this.curMoveSquares=0;
        // this.numSquaresMoved=0;

        switch(color)
        {
            case Color.RED:
                image = R.drawable.rr;
                break;
            case Color.YELLOW:
                image = R.drawable.rj;
                break;
            case Color.BLUE:
                image = R.drawable.rb;
                break;
            case Color.GREEN:
                image = R.drawable.rv;
                break;
            default:
                image = 0;
                break;
        }

    }

    public void setGridDimensions(int xGrid, int yGrid, float cellSize){
        this.xGrid = xGrid;
        this.yGrid = yGrid;
        this.widthCell = this.heightCell = cellSize;
        this.radius = (int) (cellSize / 2) + extraSizeForRobotsAndTargets;
    }

    @Override
    public void create(){
    }

    @Override
    public void load(RenderManager renderManager){
    }

    @Override
    public void draw(RenderManager renderManager){
        //renderManager.setColor(this.color);
        //display the piece

        xDraw = (int)(this.xGrid+((this.x+((float)deltaX)/10)+0.5f)*this.widthCell);
        yDraw = (int)(this.yGrid+((this.y+((float)deltaY)/10)+0.5f)*this.heightCell);
        // renderManager.drawCircle(xDraw, yDraw, this.radius);


        renderManager.drawImage(xDraw-this.radius, yDraw-this.radius, xDraw+this.radius, yDraw+this.radius, this.image);
    }

    @Override
    public void update(GameManager gameManager){
        int deltaValue; // movement speed of robots

        //if the piece is not in motion, ...
        if((this.x == this.xObjective) && (this.y == this.yObjective) && (deltaX == 0) && (deltaY == 0)){

//            System.out.println(" GamePiece "+color + " x = "+ x + " y = " + y + " xObj = "+xObjective+ " yObj = "+yObjective + " deltaX = "+deltaX + " deltaY = "+deltaY);
            if(inMovement) {
                ((GridGameScreen)(gameManager.getCurrentScreen())).doMovesInMemory();
            }

            inMovement = false;

            if(testIfWon) {
                ((GridGameScreen)(gameManager.getCurrentScreen())).win(this);
                testIfWon = false;
            }
//            inMovement = false;
            //if there is user input, ...
            InputManager inputManager = gameManager.getInputManager();
            if(inputManager.eventHasOccurred()){
                int xTouch, yTouch, dx, dy;
                xTouch = (int)inputManager.getTouchX();
                yTouch = (int)inputManager.getTouchY();
                dx = xTouch - this.xDraw;
                dy = yTouch - this.yDraw;

                // TODO: if two robots touch, set tolerance to 0
                //if the user touched the piece, ...
                if(dx*dx + dy*dy - toleranceForInputManagerTouch <= this.radius*this.radius && inputManager.downOccurred()){
                    // TODO: enlarge and put in front with this.radius+=1;
                    //display the movement interface
                    ((GridGameScreen)(gameManager.getCurrentScreen())).activateInterface(this, xDraw, yDraw);
                }
            }


        }else{ //otherwise (if the piece must move),
            // TODO: reset if enlarging worked this.radius=32;
            if(inMovement==false){
                // before move
                this.curMoveSquares=Math.abs(this.xObjective-this.x)+Math.abs(this.yObjective-this.y);
                //this.numSquaresMoved+=this.curMoveSquares;
                System.out.println(" start move with "+this.curMoveSquares+" squares");
                ((GridGameScreen)(gameManager.getCurrentScreen())).setCurrentMovedSquares(this.curMoveSquares);
            }
            inMovement = true;
            testIfWon = true;

            deltaValue = initialSpeed; // initial speed of robots

            if(this.x < this.xObjective) {
                for(int i=deltaValue-1; i>0; i--){
                    if(this.x > this.xObjective - (i+1)) deltaValue = i; // slow down
                }
                deltaX += deltaValue;
            } else if(this.x > this.xObjective) {
                for(int i=deltaValue-1; i>0; i--) {
                    if (this.x < this.xObjective + (i+1)) deltaValue = i; // slow down
                }
                deltaX -= deltaValue;
            }
            if(this.y < this.yObjective){
                for(int i=deltaValue-1; i>0; i--) {
                    if (this.y > this.yObjective - (i+1)) deltaValue = i; // slow down
                }
                deltaY += deltaValue;
            } else if(this.y > this.yObjective) {
                for(int i=deltaValue-1; i>0; i--) {
                    if (this.y < this.yObjective + (i+1)) deltaValue = i; // slow down
                }
                deltaY -= deltaValue;
            }

            // TODO: enable faster than speed 9
            if(deltaX > 9) {
                this.x += 1;
                deltaX = 0;
            }
            if(deltaX < -9) {
                this.x -= 1;
                deltaX = 0;
            }
            if(deltaY > 9) {
                this.y += 1;
                deltaY = 0;
            }
            if(deltaY < -9) {
                this.y -= 1;
                deltaY = 0;
            }
        }
    }

    @Override
    public void destroy(){
    }

}

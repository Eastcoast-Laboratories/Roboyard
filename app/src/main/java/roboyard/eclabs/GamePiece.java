package roboyard.eclabs;

import android.graphics.Color;
import android.media.MediaPlayer;

import timber.log.Timber;

/**
 * this class represents a game piece
 */
public class GamePiece implements IGameObject {
    private int x                   = 0;
    private int y                   = 0;
    private int xObjective          = 0;
    private int yObjective          = 0;
    private int xGrid               = 400; // TODO: find out, what this is for. only used in draw method when creating the robots and targets and results in a slightly larger image due to extraSizeForRobotsAndTargets
    private int yGrid               = 500;
    private int xDraw               = 0;
    private int yDraw               = 0;
    private float numSquaresX       = MainActivity.getBoardWidth();
    private float numSquaresY       = MainActivity.getBoardHeight();
    private int radius;
    private int color               = Color.RED;
    private boolean inMovement      = false;
    private int deltaX              = 0;
    private int deltaY              = 0;
    private int curMoveSquares      = 0;
    // private int numSquaresMoved     = 0;
    private final int initialSpeed        = 16;
    private final int extraSizeForRobotsAndTargets = 33; // robots and targets are some percent larger than the grid and may overlap 4 px

    boolean testIfWon       = true;
    
    // Tracks what type of collision last occurred
    private String lastCollisionType = "none"; // none, hit_robot, hit_wall
    
    // Static MediaPlayer to track currently playing sound
    private static MediaPlayer currentSoundPlayer = null;
    private static boolean isSoundPlaying = false;

    /**
     * Check if the piece is currently moving
     * @return true if the piece is in movement
     */
    public boolean isInMovement() {
        return inMovement;
    }

    private int image               = 0;

    private int direction; // 0: up, 1: right, 2: down, 3: left

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

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getDirection(){
        return this.direction;
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

        this.direction = 1; // right

        switch(color)
        {
            case Color.RED:
                image = R.drawable.robot_red_right;
                break;
            case Color.YELLOW:
                image = R.drawable.robot_yellow_right;
                break;
            case Color.BLUE:
                image = R.drawable.robot_blue_right;
                break;
            case Color.GREEN:
                image = R.drawable.robot_green_right;
                break;
            default:
                image = 0;
                break;
        }
    }

    public void setGridDimensions(int xGrid, int yGrid, float cellSize){
        this.xGrid = xGrid;
        this.yGrid = yGrid;
        this.numSquaresX = this.numSquaresY = cellSize;
        this.radius = (int) (cellSize / 2) * (100 + extraSizeForRobotsAndTargets)/100;
    }

    @Override
    public void create(){
    }

    @Override
    public void load(RenderManager renderManager){
    }

    /** Draw the robots on the screen */
    @Override
    public void draw(RenderManager renderManager){
        //renderManager.setColor(this.color);
        //display the piece

        xDraw = (int)(this.xGrid+((this.x+((float)deltaX)/10)+0.5f)*this.numSquaresX); // number of Squares + the movement of the robot +0.5f to round to the nearest integer multiplied by the width of the cell
        yDraw = (int)(this.yGrid+((this.y+((float)deltaY)/10)+0.5f)*this.numSquaresY);
        // renderManager.drawCircle(xDraw, yDraw, this.radius);

        switch (color) {
            case Color.RED:
                image = (this.direction==3) ? R.drawable.robot_red_left : R.drawable.robot_red_right;
                break;
            case Color.YELLOW:
                image = (this.direction==3) ? R.drawable.robot_yellow_left : R.drawable.robot_yellow_right;
                break;
            case Color.BLUE:
                image = (this.direction==3) ? R.drawable.robot_blue_left : R.drawable.robot_blue_right;
                break;
            case Color.GREEN:
                image = (this.direction==3) ? R.drawable.robot_green_left : R.drawable.robot_green_right;
                break;
            default:
                image = 0;
                break;
        }
        renderManager.drawImage(xDraw-this.radius, yDraw-this.radius, xDraw+this.radius, yDraw+this.radius, this.image);
    }

    @Override
    public void update(GameManager gameManager){
        int deltaValue; // movement speed of robots

        //if the piece is not in motion, ...
        if((this.x == this.xObjective) && (this.y == this.yObjective) && (deltaX == 0) && (deltaY == 0)){

//            Timber.d(" GamePiece "+color + " x = "+ x + " y = " + y + " xObj = "+xObjective+ " yObj = "+yObjective + " deltaX = "+deltaX + " deltaY = "+deltaY);
            if(inMovement) {
                ((GridGameScreen)(gameManager.getCurrentScreen())).doMovesInMemory();
                Boolean justWon = false;
                if(testIfWon) {
                    if(((GridGameScreen)(gameManager.getCurrentScreen())).win(this)) {
                        // Play win sound
                        playRobotSound(gameManager, "win");
                        justWon = true;
                        testIfWon = false;  // Only set to false if we actually won
                    }
                }
                // after move check if stopped by wall or robot

                Timber.d(" end move with "+this.curMoveSquares+" squares");
                
                // Play appropriate sound based on collision type
                if (!lastCollisionType.equals("none") && !justWon) {
                    playRobotSound(gameManager, lastCollisionType);
                    // Reset collision type
                    lastCollisionType = "none";
                }
            }
            inMovement = false;

            //if there is user input, ...
            InputManager inputManager = gameManager.getInputManager();
            if(inputManager.eventHasOccurred()){
                int xTouch, yTouch, dx, dy;
                xTouch = (int)inputManager.getTouchX();
                yTouch = (int)inputManager.getTouchY();
                dx = xTouch - this.xDraw;
                dy = yTouch - this.yDraw;

                int thisToleranceForInputManagerTouch;
                // if two robots touch, set tolerance to 0:
                if(((GridGameScreen)(gameManager.getCurrentScreen())).getRobotsTouching()) {
                    thisToleranceForInputManagerTouch = 0;
                } else {
                    // TODO: if any are only 2 squares apart make it 155 else larger
                    thisToleranceForInputManagerTouch = 155 * this.radius;
                }

                //if the user touched the piece, ...
                if(dx*dx + dy*dy - thisToleranceForInputManagerTouch <= this.radius*this.radius && inputManager.downOccurred()){
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
                Timber.d(" start move with "+this.curMoveSquares+" squares");
                ((GridGameScreen)(gameManager.getCurrentScreen())).setCurrentMovedSquares(this.curMoveSquares);
                
                // Play robot movement sound when robot starts moving
                playRobotSound(gameManager, "move");
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
    
    /**
     * Set the type of collision that occurred
     * @param collisionType The type of collision ("robot" or "wall")
     */
    public void setLastCollisionType(String collisionType) {
        this.lastCollisionType = collisionType;
    }
    
    /**
     * Get the type of the last collision
     * @return The last collision type ("none", "robot", or "wall")
     */
    public String getLastCollisionType() {
        return this.lastCollisionType;
    }
    
    /**
     * Plays the robot movement sound effect
     *
     * @param gameManager The game manager instance
     * @param wichSound The type of sound to play ("move", "hit_wall", or "hit_robot")
     */
    private void playRobotSound(GameManager gameManager, String wichSound) {
        try {
            // Get the activity from the game manager
            MainActivity activity = gameManager.getActivity();
            if (activity != null) {
                // Check if sound is enabled in preferences
                Preferences preferences = new Preferences();
                String soundSetting = preferences.getPreferenceValue(activity, "sound");
                if (!soundSetting.equals("off")) {
                    // If a sound is already playing, don't play another one
                    if (isSoundPlaying && currentSoundPlayer != null && currentSoundPlayer.isPlaying()) {
                        Timber.d("Not playing sound %s - another sound is already playing", wichSound);
                        return;
                    }
                    
                    // Stop any previously playing sound
                    if (currentSoundPlayer != null) {
                        try {
                            if (currentSoundPlayer.isPlaying()) {
                                currentSoundPlayer.stop();
                            }
                            currentSoundPlayer.release();
                            currentSoundPlayer = null;
                        } catch (Exception e) {
                            Timber.e(e, "Error stopping previous sound");
                        }
                    }
                    
                    // Create and play the robot sound
                    MediaPlayer mp = null;
                    if(wichSound.equals("move")) {
                        mp = MediaPlayer.create(activity, R.raw.robot_move);
                    } else if (wichSound.equals("hit_wall")) {
                        mp = MediaPlayer.create(activity, R.raw.robot_hit_wall);
                    } else if (wichSound.equals("hit_robot")) {
                        mp = MediaPlayer.create(activity, R.raw.robot_hit_robot);
                    } else if (wichSound.equals("win")) {
                        mp = MediaPlayer.create(activity, R.raw.robot_win);
                    } else if (wichSound.equals("lose")) {
                        // TODO: add sound
                        // mp = MediaPlayer.create(activity, R.raw.robot_lose);
                    } else if (wichSound.equals("none")) {
                        Timber.d("No sound to play");
                        return;
                    }
                    if (mp != null) {
                        // Set the global current player
                        currentSoundPlayer = mp;
                        isSoundPlaying = true;
                        
                        // When playback completes, release the player and reset the flag
                        mp.setOnCompletionListener(mediaPlayer -> {
                            mediaPlayer.release();
                            currentSoundPlayer = null;
                            isSoundPlaying = false;
                        });
                        
                        // Reduce volume to some % of maximum
                        float volume = 0.1f;
                        mp.setVolume(volume, volume);
                        mp.start();
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error playing robot sound");
            isSoundPlaying = false;
            if (currentSoundPlayer != null) {
                try {
                    currentSoundPlayer.release();
                } catch (Exception ex) {
                    // Ignore
                }
                currentSoundPlayer = null;
            }
        }
    }

    @Override
    public void destroy(){
    }

}

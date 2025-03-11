package roboyard.eclabs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import timber.log.Timber;

/**
 * Created by Alain on 04/02/2015.
 */
public class MapGenerator {

    final Random rand;

    // position of the square in the middle of the game board
    int carrePosX; // horizontal position of the top wall of square, starting with 0
    int carrePosY; // vertical position of the left wall of the square

    Boolean targetMustBeInCorner = true; // TODO: only works together with generateNewMapEachTime==true (which is set only in Beginner Mode)
    Boolean allowMulticolorTarget = true;
    static Boolean generateNewMapEachTime = false; // TODO: add option in settings

    // Wall configuration
    int maxWallsInOneVerticalCol = 2;    // Maximum number of walls allowed in one vertical column
    int maxWallsInOneHorizontalRow = 2;  // Maximum number of walls allowed in one horizontal row
    int wallsPerQuadrant;                // Number of walls to place in each quadrant of the board

    Boolean loneWallsAllowed = false; // walls that are not attached in a 90 deg. angle

    public MapGenerator(){
        rand = new Random();

        // Initialize square position based on current board size
        carrePosX = (MainActivity.getBoardWidth()/2)-1;
        carrePosY = (MainActivity.getBoardHeight()/2)-1;

        // Calculate walls per quadrant based on board width
        wallsPerQuadrant = MainActivity.getBoardWidth()/4;  // Default: quarter of board width

        // Check difficulty level
        int level = GridGameScreen.getLevel();
        if(level == 0){ // Difficulty Beginner
            generateNewMapEachTime=true; // generate a new map each time you start a new game
        } else {
            if(level == 1){ // Advanced
                generateNewMapEachTime=true;
            }
            if (generateNewMapEachTime) {
                // TODO: doesn't work if not generateNewMapEachTime because the position is not remembered above restarts with the same map
                // TODO: does not work with the roboyard in the middle, that is not moved to the new random position
                // random position of square in the middle
                // carrePosX=getRandom(3,MainActivity.getBoardWidth()-5);
                // carrePosY=getRandom(3,MainActivity.getBoardHeight()-5);
            }
            allowMulticolorTarget = false;

            maxWallsInOneVerticalCol = 3;
            maxWallsInOneHorizontalRow = 3;
            wallsPerQuadrant = (int) (MainActivity.getBoardWidth()/3.3);

            loneWallsAllowed = true;
        }

        if(level == 2 || level == 3) {
            generateNewMapEachTime=false; // keep the current map over restarts
            // generateNewMapEachTime=true; // DEBUG!!! remove to release
            targetMustBeInCorner = false;

            maxWallsInOneVerticalCol = 5;
            maxWallsInOneHorizontalRow = 5;
            wallsPerQuadrant = (int) (MainActivity.getBoardWidth()/3);
        }
        if(level == DIFFICULTY_IMPOSSIBLE) {
            wallsPerQuadrant = (int) (MainActivity.getBoardWidth()/2.3); // for debug, set to 1.3 with lots of walls
        }
        if (MainActivity.boardSizeX * MainActivity.boardSizeY > 64) {
            // calculate maxWallsInOneVerticalCol and maxWallsInOneHorizontalRow based on board size

        }
        Timber.d("wallsPerQuadrant: " + wallsPerQuadrant + " Board size: " + MainActivity.boardSizeX + "x" + MainActivity.boardSizeY);
    }

    public ArrayList<GridElement> removeGameElementsFromMap(ArrayList<GridElement> data) {
        String[] gameElementTypes = {
                "robot_green", "robot_yellow", "robot_red", "robot_blue", // robots
                "target_green", "target_yellow", "target_red", "target_blue", "target_multi" // targets (cible)
        };
        Iterator<GridElement> iterator = data.iterator();
        while (iterator.hasNext()) {
            GridElement e = iterator.next();
            if(Arrays.asList(gameElementTypes).contains(e.getType())){
                iterator.remove();
            }
        }
        return data;
    }

    public ArrayList<GridElement> translateArraysToMap(int[][] horizontalWalls, int[][] verticalWalls) {
        ArrayList<GridElement> data = new ArrayList<>();

        for(int x=0; x<=MainActivity.getBoardWidth(); x++)
            for(int y=0; y <= MainActivity.getBoardHeight(); y++)
            {
                if(horizontalWalls[x][y]== 1) {
                    // add all horizontal walls
                    data.add(new GridElement(x,y,"mh"));
                }
                if(verticalWalls[x][y]== 1) {
                    // add all vertical walls
                    data.add(new GridElement(x,y,"mv"));
                }
            }
        return data;
    }

    public int getRandom(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    public ArrayList<GridElement> addGameElementsToGameMap(ArrayList<GridElement> data ,int[][] horizontalWalls, int[][]verticalWalls){

        boolean abandon;
        int targetX;
        int targetY;
        Boolean tempTargetMustBeInCorner;

        tempTargetMustBeInCorner = targetMustBeInCorner;
        if(!targetMustBeInCorner && getRandom(0,1) != 1){
            // 50% probability that the target is in a corner
            tempTargetMustBeInCorner=true;
        }
        do{
            abandon = false;
            targetX = getRandom(0, MainActivity.getBoardWidth()-1);
            targetY = getRandom(0, MainActivity.getBoardHeight()-1);

            if(tempTargetMustBeInCorner && horizontalWalls[targetX][targetY] == 0 && horizontalWalls[targetX][targetY+1] == 0)
                abandon = true;
            if(tempTargetMustBeInCorner && verticalWalls[targetX][targetY] == 0 && verticalWalls[targetX+1][targetY] == 0)
                abandon = true;

            if((targetX == carrePosX && targetY == carrePosY)
                    || (targetX == carrePosX && targetY == carrePosY+1)
                    || (targetX == carrePosX+1 && targetY == carrePosY)
                    || (targetX == carrePosX+1 && targetY == carrePosY+1))
                abandon = true; // target was in square

        }while(abandon);

        String[] typesOfTargets = {"target_red", "target_blue", "target_yellow", "target_green", "target_multi"};

        if(allowMulticolorTarget) {
            data.add(new GridElement(targetX, targetY, typesOfTargets[getRandom(0,4)]));
        } else {
            data.add(new GridElement(targetX, targetY, typesOfTargets[getRandom(0,3)]));
        }

        String[] typesOfRobots = {"robot_red", "robot_blue", "robot_yellow", "robot_green"};

        ArrayList<GridElement> robotsTemp = new ArrayList<>();

        int cX;
        int cY;

        for(String currentRobotType : typesOfRobots)
        {
            do {
                abandon = false;
                cX = getRandom(0, MainActivity.getBoardWidth()-1);
                cY = getRandom(0, MainActivity.getBoardHeight()-1);

                for(GridElement robot:robotsTemp) {
                    if (robot.getX() == cX && robot.getY() == cY) {
                        abandon = true;
                        break;
                    }
                }

                if((cX == carrePosX && cY == carrePosY) || (cX == carrePosX && cY == carrePosY+1) || (cX == carrePosX+1 && cY == carrePosY) || (cX == carrePosX+1 && cY == carrePosY+1))
                    abandon = true; // robot was inside square
                if(cX == targetX && cY == targetY)
                    abandon = true; // robot was target

            }while(abandon);
            robotsTemp.add(new GridElement(cX, cY, currentRobotType));
        }
        data.addAll(robotsTemp);

        return data;
    }

    /**
     * generates a new map. The map is divided into four quadrants, like in the game ricochet robots. and walls are evenly distributed among all quadrants.
     * for each quadrant there are 2 walls placed at a right-angle to the each border.
     * @return Arraylist with all grid elements that belong to the map
     */
    public ArrayList<GridElement> getGeneratedGameMap() {
        int[][] horizontalWalls = new int[MainActivity.getBoardWidth()+1][MainActivity.getBoardHeight()+1];
        int[][] verticalWalls = new int[MainActivity.getBoardWidth()+1][MainActivity.getBoardHeight()+1];

        int temp = 0;
        int countX = 0;
        int countY = 0;

        boolean restart;

        do {
            restart = false;

            //We initialize with no walls
            for (int x = 0; x < MainActivity.getBoardWidth(); x++)
                for (int y = 0; y < MainActivity.getBoardHeight(); y++)
                    horizontalWalls[x][y] = verticalWalls[x][y] = 0;

            //Creation of the borders
            for (int x = 0; x < MainActivity.getBoardWidth(); x++) {
                horizontalWalls[x][0] = 1;
                horizontalWalls[x][MainActivity.getBoardHeight()] = 1;
            }
            for (int y = 0; y < MainActivity.getBoardHeight(); y++) {
                verticalWalls[0][y] = 1;
                verticalWalls[MainActivity.getBoardWidth()][y] = 1;
            }

            // right-angled Walls near the left border
            horizontalWalls[0][getRandom(2, 7)] = 1;
            do {
                temp = getRandom(MainActivity.getBoardHeight()/2, MainActivity.getBoardHeight()-1);
            }
            while (horizontalWalls[0][temp - 1] == 1 || horizontalWalls[0][temp] == 1 || horizontalWalls[0][temp + 1] == 1);
            horizontalWalls[0][temp] = 1;

            // right-angled Walls near the right border
            horizontalWalls[MainActivity.getBoardWidth()-1][getRandom(2, 7)] = 1;
            do {
                temp = getRandom(MainActivity.getBoardHeight()/2, MainActivity.getBoardHeight()-1);
            }
            while (horizontalWalls[MainActivity.getBoardWidth()-1][temp - 1] == 1 || horizontalWalls[MainActivity.getBoardWidth()-1][temp] == 1 || horizontalWalls[MainActivity.getBoardWidth()-1][temp + 1] == 1);
            horizontalWalls[MainActivity.getBoardWidth()-1][temp] = 1;

            // right-angled Walls near the top border
            verticalWalls[getRandom(2, MainActivity.getBoardWidth()/2 - 1)][0] = 1;
            do {
                temp = getRandom(MainActivity.getBoardWidth()/2, MainActivity.getBoardWidth()-1);
            }
            while (verticalWalls[temp - 1][0] == 1 || verticalWalls[temp][0] == 1 || verticalWalls[temp + 1][0] == 1);
            verticalWalls[temp][0] = 1;

            // right-angled Walls near the bottom border
            verticalWalls[getRandom(2, MainActivity.getBoardWidth()/2 - 1)][MainActivity.getBoardHeight()-1] = 1;
            do {
                temp = getRandom(8, MainActivity.getBoardWidth()-1);
            }
            while (verticalWalls[temp - 1][MainActivity.getBoardHeight()-1] == 1 || verticalWalls[temp][MainActivity.getBoardHeight()-1] == 1 || verticalWalls[temp + 1][MainActivity.getBoardHeight()-1] == 1);
            verticalWalls[temp][MainActivity.getBoardHeight()-1] = 1;

            //Drawing the middle square (carré)
            horizontalWalls[carrePosX][carrePosY] = horizontalWalls[carrePosX + 1][carrePosY] = 1;
            horizontalWalls[carrePosX][carrePosY+2] = horizontalWalls[carrePosX + 1][carrePosY+2] = 1;
            verticalWalls[carrePosX][carrePosY] = verticalWalls[carrePosX][carrePosY + 1] = 1;
            verticalWalls[carrePosX+2][carrePosY] = verticalWalls[carrePosX+2][carrePosY + 1] = 1;

            // Loop to place walls in each quadrant of the board
            // The board is divided into 4 quadrants, and we try to place an equal number of walls in each
            // Each wall consists of two parts placed at right angles to form an L-shape
            // k determines which quadrant we're placing walls in:
            // k < boardWidth/4:        top-left quadrant
            // k < 2*boardWidth/4:      top-right quadrant
            // k < 3*boardWidth/4:      bottom-left quadrant
            // k < boardWidth:          bottom-right quadrant
            // k >= boardWidth:          anywhere on board (bonus wall)
            for (int k = 0; k < wallsPerQuadrant * 4 + 1; k++) {
                boolean abandon = false;
                int tempX;
                int tempY;
                int tempXv = 0;
                int tempYv = 0;

                long compteLoop1 = 0;
                do {
                    compteLoop1++;
                    abandon = false;

                    //Choice of random coordinates in each quadrant of the game board
                    if (k < MainActivity.getBoardWidth()/4) {
                        // top-left quadrant
                        tempX = getRandom(1, MainActivity.getBoardWidth()/2 -1);
                        tempY = getRandom(1, MainActivity.getBoardHeight()/2 -1);
                    } else if (k < 2*MainActivity.getBoardWidth()/4) {
                        // top-right quadrant
                        tempX = getRandom(MainActivity.getBoardWidth()/2, MainActivity.getBoardWidth()-1);
                        tempY = getRandom(1, MainActivity.getBoardHeight()/2 -1);
                    } else if (k < 3*MainActivity.getBoardWidth()/4) {
                        // bottom-left quadrant
                        tempX = getRandom(1, MainActivity.getBoardWidth()/2 -1);
                        tempY = getRandom(MainActivity.getBoardHeight()/2, MainActivity.getBoardHeight()-1);
                    } else if (k < MainActivity.getBoardWidth()) {
                        // bottom-right quadrant
                        tempX = getRandom(MainActivity.getBoardWidth()/2, MainActivity.getBoardWidth()-1);
                        tempY = getRandom(MainActivity.getBoardHeight()/2, MainActivity.getBoardHeight()-1);
                    } else {
                        // bonus walls
                        tempX = getRandom(1, MainActivity.getBoardWidth()-1);
                        tempY = getRandom(1, MainActivity.getBoardHeight()-1);
                    }

                    if (horizontalWalls[tempX][tempY] == 1 // already chosen
                        || horizontalWalls[tempX - 1][tempY] == 1 // left
                        || horizontalWalls[tempX + 1][tempY] == 1 // right
                        || horizontalWalls[tempX][tempY - 1] == 1 // directly above
                        || horizontalWalls[tempX][tempY + 1] == 1 // directly below
                        ) abandon = true;

                    if (verticalWalls[tempX][tempY] == 1 // already chosen
                        || verticalWalls[tempX + 1][tempY] == 1 // left
                        || verticalWalls[tempX][tempY - 1] == 1 // above
                        || verticalWalls[tempX + 1][tempY - 1] == 1 // diagonal right-above
                        ) abandon = true;

                    if (!abandon) {
                        //We count the number of walls in the same row/column
                        countX = countY = 0;

                        for (int x = 1; x < MainActivity.getBoardWidth()-1; x++) {
                            if (horizontalWalls[x][tempY] == 1)
                                countX++;
                        }

                        for (int y = 1; y < MainActivity.getBoardHeight()-1; y++) {
                            if (horizontalWalls[tempX][y] == 1)
                                countY++;
                        }

                        if (tempY == carrePosY || tempY == carrePosY+2) {
                            countX -= 2;
                        }
                        if (countX >= maxWallsInOneHorizontalRow || countY >= maxWallsInOneVerticalCol) //If there are too many walls in the same row/column, we abandon
                            abandon = true;
                    }

                    if (!abandon) {
                        //Choice of the 2nd wall of the corner being drawn
                        tempXv = tempX + getRandom(0, 1);
                        tempYv = tempY - getRandom(0, 1);

                        //We check that it does not fall on or near existing walls
                        if (verticalWalls[tempXv][tempYv] == 1 || verticalWalls[tempXv - 1][tempYv] == 1 || verticalWalls[tempXv + 1][tempYv] == 1)
                            abandon = true;
                        if (verticalWalls[tempXv][tempYv - 1] == 1 || verticalWalls[tempXv][tempYv + 1] == 1)
                            abandon = true;

                        if (horizontalWalls[tempXv][tempYv] == 1 || horizontalWalls[tempXv - 1][tempYv] == 1)
                            abandon = true;

                        if (horizontalWalls[tempXv][tempYv - 1] == 1 || horizontalWalls[tempXv - 1][tempYv - 1] == 1)
                            abandon = true;

                        if (verticalWalls[tempXv - 1][tempYv - 1] == 1 || verticalWalls[tempXv - 1][tempYv + 1] == 1)
                            abandon = true;

                        if (verticalWalls[tempXv + 1][tempYv + 1] == 1 || verticalWalls[tempXv + 1][tempYv - 1] == 1)
                            abandon = true;

                        if (!abandon) {
                            //We count the number of walls in the same row/column
                            countX = countY = 0;

                            for (int x = 1; x < MainActivity.getBoardWidth()-1; x++) {
                                if (verticalWalls[x][tempYv] == 1)
                                    countX++;
                            }

                            for (int y = 1; y < MainActivity.getBoardHeight()-1; y++) {
                                if (verticalWalls[tempXv][y] == 1)
                                    countY++;
                            }

                            if (tempXv == carrePosX || tempXv == carrePosX+2) {
                                countY -= 2;
                            }
                            if (countX >= maxWallsInOneHorizontalRow || countY >= maxWallsInOneVerticalCol) //If there are too many walls in the same row/column, we abandon
                                abandon = true;
                        }

                    }

                    if (compteLoop1 > 1000) {
                        Timber.d("Wall creation restarted, too many loops");
                        restart = true;
                    }

                } while (abandon && !restart);
                Boolean skiponewall = false;
                if(loneWallsAllowed && getRandom(0, 4) == 1) {
                    skiponewall = true;
                }
                if(skiponewall){
                    if(getRandom(0, 1) == 1){
                        horizontalWalls[tempX][tempY] = 1;
                    }else{
                        verticalWalls[tempXv][tempYv] = 1;
                    }
                }else{
                    horizontalWalls[tempX][tempY] = 1;
                    verticalWalls[tempXv][tempYv] = 1;
                }
            }
        }while(restart);

        ArrayList<GridElement> data = GridGameScreen.getMap();

        if(data == null || generateNewMapEachTime){
            data = translateArraysToMap(horizontalWalls, verticalWalls);
            GridGameScreen.setMap(data);
            generateNewMapEachTime = false;
        } else{
            data = removeGameElementsFromMap(data);
        }

        data = addGameElementsToGameMap(data, horizontalWalls, verticalWalls);

        return data;
    }
}

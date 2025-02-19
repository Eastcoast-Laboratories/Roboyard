package roboyard.pm.ia.ricochet;

import android.graphics.Color;

import driftingdroids.model.Board;
import roboyard.eclabs.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for converting between Roboyard's game elements and DriftingDroids board format.
 * This class serves as the main conversion layer between the two systems, handling:
 * - Grid cell conversion
 * - Robot piece placement
 * - Wall and obstacle mapping
 * - Goal position translation
 *
 * @see driftingdroids.model.Board
 * @see roboyard.eclabs.GridElement
 */
public class RRGetMap {

    /**
     * Create a virtual world in memory for the RR solver (BFS)
     * adds all game elements to that virtual world so the solver can solve it with the current data
     *
     * @param gridElements game elements
     * @param baseState
     * @return
     */
    public static RRWorld createRRWorld(ArrayList<GridElement> gridElements, RRGameState baseState) {
        RRWorld currentWorld = new RRWorld();

        ArrayList<GridElement> robots = new ArrayList<>();
        GridElement cible = null;

        Map<String, Integer> colors = new HashMap<>();



        colors.put("robot_red", Color.RED);
        colors.put("robot_blue", Color.BLUE);
        colors.put("robot_green", Color.GREEN);
        colors.put("robot_yellow", Color.YELLOW);

        colors.put("target_red", Color.RED);
        colors.put("target_blue", Color.BLUE);
        colors.put("target_green", Color.GREEN);
        colors.put("target_yellow", Color.YELLOW);
        colors.put("target_multi", 0);      // no color

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if (myp.getType().equals("mh")) {
                currentWorld.setHorizontalWall(myp.getX(), myp.getY());
            }
            if (myp.getType().equals("mv")) {
                currentWorld.setVerticalWall(myp.getX(), myp.getY());
            }
            if (myp.getType().equals("target_red") || myp.getType().equals("target_green") ||myp.getType().equals("target_blue") ||myp.getType().equals("target_yellow") ||myp.getType().equals("target_multi")) {
                cible = myp;
            }
            if (myp.getType().equals("robot_red") || myp.getType().equals("robot_green") ||myp.getType().equals("robot_blue") ||myp.getType().equals("robot_yellow")) {
                robots.add(myp);
            }
        }


        ArrayList<RRPiece> mainL = new ArrayList<>();
        ArrayList<RRPiece> secondL = new ArrayList<>();

        String[] types = {"target_red","target_blue","target_green","target_yellow"};

        int cpt = 0;

        assert cible != null;
        if(cible.getType().equals("target_multi"))
        {
            for(GridElement robot : robots)
            {
                mainL.add(new RRPiece(robot.getX(), robot.getY(), colors.get(robot.getType()), cpt));
                cpt++;
            }
        }
        else{
            for(String type : types)
            {
                if(cible.getType().equals(type))
                {
                    for(GridElement robot : robots)
                    {
                        if(robot.getType().charAt(6) == cible.getType().charAt(7))
                        {
                            mainL.add(new RRPiece(robot.getX(), robot.getY(), colors.get(robot.getType()), cpt));
                            cpt++;
                        }
                        else
                        {
                            secondL.add(new RRPiece(robot.getX(), robot.getY(), colors.get(robot.getType()), cpt));
                            cpt++;
                        }
                    }
                }
            }
        }

        currentWorld.setObjective(cible.getX(), cible.getY(), colors.get(cible.getType()));


        RRPiece[] mainLA, secLA;
        mainLA = new RRPiece[mainL.size()];
        for(int i=0; i<mainL.size(); i++){
            mainLA[i] = mainL.get(i);
        }

        secLA = new RRPiece[secondL.size()];
        for(int i=0; i<secondL.size(); i++){
            secLA[i] = secondL.get(i);
        }

        baseState.setPieces(mainLA, secLA);

        return currentWorld;
    }

    /**
     * Create a virtual world in memory for the DD solver (IDDFS)
     * adds all game elements to that virtual world so the solver can solve it with the current data
     *
     * @param gridElements game elements
     * @param pieces
     * @return
     */
    public static Board createDDWorld(ArrayList<GridElement> gridElements, RRPiece[] pieces) {

        // this must stay at 16, 16, otherwise the solver finds solutions, that are none (tested with 12x14 and 18x18 and MainActivity.boardSizeX, MainActivity.boardSizeY, MainActivity.numRobots)
        Board board = Board.createBoardFreestyle(null, MainActivity.boardSizeX, MainActivity.boardSizeY, MainActivity.numRobots);
        board.removeGoals();

        // Color mappings for robots and targets
        Map<String, Integer> colors = new HashMap<>();
        Map<String, Integer> colors2 = new HashMap<>();

        // Robot colors for visual representation
        colors2.put("robot_red", Color.RED);
        colors2.put("robot_blue", Color.BLUE);
        colors2.put("robot_green", Color.GREEN);
        colors2.put("robot_yellow", Color.YELLOW);

        // Robot and target indices for game logic
        colors.put("robot_red", 0);
        colors.put("robot_blue", 1);
        colors.put("robot_green", 2);
        colors.put("robot_yellow", 3);

        colors.put("target_red", 0);
        colors.put("target_blue", 1);
        colors.put("target_green", 2);
        colors.put("target_yellow", 3);
        colors.put("target_multi", -1);

        int robotCounter = 0;

        // Process each grid element (walls, targets, robots)
        for (Object element : gridElements) {
            GridElement gridElement = (GridElement) element;
            int x = gridElement.getX();
            int y = gridElement.getY();
            
            // Calculate linear position (y * width + x)
            // This maps 2D coordinates to a unique 1D position
            int position = y * board.width + x;

            String type = gridElement.getType();
            
            // Handle horizontal walls
            if (type.equals("mh")) {
                board.setWall(position, "N", true);
            }
            // Handle vertical walls
            if (type.equals("mv")) {
                board.setWall(position, "W", true);
            }
            // Handle targets (both colored and multi-colored)
            if (type.equals("target_red") || type.equals("target_green") || 
                type.equals("target_blue") || type.equals("target_yellow") || 
                type.equals("target_multi")) {
                board.addGoal(position, colors.get(type), 1);
            }
            // Handle robots of different colors
            if (type.equals("robot_red") || type.equals("robot_green") || 
                type.equals("robot_blue") || type.equals("robot_yellow")) {
                pieces[colors.get(type)] = new RRPiece(x, y, colors2.get(type), robotCounter);
                robotCounter++;
            }
        }

        // Set robot positions on the board
        // Must be exactly 4 robots as the solver expects this
        for(int i = 0; i < 4; i++) {
            int position = pieces[i].getY() * board.width + pieces[i].getX();
            board.setRobot(i, position, false);
        }

        return board;
    }
}

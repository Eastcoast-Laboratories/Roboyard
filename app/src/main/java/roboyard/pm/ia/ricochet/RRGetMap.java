package roboyard.pm.ia.ricochet;

import android.graphics.Color;

import driftingdroids.model.Board;
import roboyard.eclabs.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
*
* @author usrlocal
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

        Board board = Board.createBoardFreestyle(null, 16, 16, 4);
        board.removeGoals();

        Map<String, Integer> colors = new HashMap<>();
        Map<String, Integer> colors2 = new HashMap<>();

        colors2.put("robot_red", Color.RED);
        colors2.put("robot_blue", Color.BLUE);
        colors2.put("robot_green", Color.GREEN);
        colors2.put("robot_yellow", Color.YELLOW);

        colors.put("robot_red", 0);
        colors.put("robot_blue", 1);
        colors.put("robot_green", 2);
        colors.put("robot_yellow", 3);

        colors.put("target_red", 0);
        colors.put("target_blue", 1);
        colors.put("target_green", 2);
        colors.put("target_yellow", 3);
        colors.put("target_multi", -1);

        int cpt = 0;

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if (myp.getType().equals("mh")) {
                board.setWall(myp.getX() | (myp.getY() << 4), "N", true);
            }
            if (myp.getType().equals("mv")) {
                board.setWall(myp.getX() | (myp.getY() << 4), "W", true);
            }
            if (myp.getType().equals("target_red") || myp.getType().equals("target_green") ||myp.getType().equals("target_blue") ||myp.getType().equals("target_yellow") ||myp.getType().equals("target_multi")) {
                board.addGoal(myp.getX()|(myp.getY()<<4), colors.get(myp.getType()), 1);
            }
            if (myp.getType().equals("robot_red") || myp.getType().equals("robot_green") ||myp.getType().equals("robot_blue") ||myp.getType().equals("robot_yellow")) {
                pieces[colors.get(myp.getType())] = new RRPiece(myp.getX(), myp.getY(), colors2.get(myp.getType()), cpt);
                cpt ++;
            }
        }

        for(int i=0; i<4; i++){
            board.setRobot(i, pieces[i].getX()|(pieces[i].getY()<<4), false);
        }

        return board;
    }
}

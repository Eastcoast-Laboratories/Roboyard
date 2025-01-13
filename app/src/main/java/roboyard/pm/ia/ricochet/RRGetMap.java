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



        colors.put("rr", Color.RED);
        colors.put("rb", Color.BLUE);
        colors.put("rv", Color.GREEN);
        colors.put("rj", Color.YELLOW);

        colors.put("cr", Color.RED);
        colors.put("cb", Color.BLUE);
        colors.put("cv", Color.GREEN);
        colors.put("cj", Color.YELLOW);
        colors.put("cm", 0);      // no color

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if (myp.getType().equals("mh")) {
                currentWorld.setHorizontalWall(myp.getX(), myp.getY());
            }
            if (myp.getType().equals("mv")) {
                currentWorld.setVerticalWall(myp.getX(), myp.getY());
            }
            if (myp.getType().equals("cr") || myp.getType().equals("cv") ||myp.getType().equals("cb") ||myp.getType().equals("cj") ||myp.getType().equals("cm")) {
                cible = myp;
            }
            if (myp.getType().equals("rr") || myp.getType().equals("rv") ||myp.getType().equals("rb") ||myp.getType().equals("rj")) {
                robots.add(myp);
            }
        }


        ArrayList<RRPiece> mainL = new ArrayList<>();
        ArrayList<RRPiece> secondL = new ArrayList<>();

        String[] types = {"cr","cb","cv","cj"};

        int cpt = 0;

        assert cible != null;
        if(cible.getType().equals("cm"))
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
                        if(robot.getType().charAt(1) == cible.getType().charAt(1))
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

        colors2.put("rr", Color.RED);
        colors2.put("rb", Color.BLUE);
        colors2.put("rv", Color.GREEN);
        colors2.put("rj", Color.YELLOW);

        colors.put("rr", 0);
        colors.put("rb", 1);
        colors.put("rv", 2);
        colors.put("rj", 3);

        colors.put("cr", 0);
        colors.put("cb", 1);
        colors.put("cv", 2);
        colors.put("cj", 3);
        colors.put("cm", -1);

        int cpt = 0;

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if (myp.getType().equals("mh")) {
                board.setWall(myp.getX() | (myp.getY() << 4), "N", true);
            }
            if (myp.getType().equals("mv")) {
                board.setWall(myp.getX() | (myp.getY() << 4), "W", true);
            }
            if (myp.getType().equals("cr") || myp.getType().equals("cv") ||myp.getType().equals("cb") ||myp.getType().equals("cj") ||myp.getType().equals("cm")) {
                board.addGoal(myp.getX()|(myp.getY()<<4), colors.get(myp.getType()), 1);
            }
            if (myp.getType().equals("rr") || myp.getType().equals("rv") ||myp.getType().equals("rb") ||myp.getType().equals("rj")) {
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

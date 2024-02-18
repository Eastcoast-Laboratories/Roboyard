package roboyard.eclabs.solver;

import java.util.ArrayList;
import java.util.List;

import driftingdroids.model.*;
import roboyard.eclabs.GridElement;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.ricochet.RRGetMap;

/**
 * Created by Pierre on 15/04/2015.
 */
public interface ISolver extends Runnable {

    void init(ArrayList<GridElement> elements);
    void run();
    SolverStatus getSolverStatus();
    GameSolution getSolution(int num);
    List<Solution> getSolutionList();
}

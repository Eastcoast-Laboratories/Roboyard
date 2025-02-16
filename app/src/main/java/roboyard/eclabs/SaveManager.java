package roboyard.eclabs;

import android.app.Activity;

import java.util.HashMap;

/**
 * screen 9: save games
 * Created by Alain on 28/03/2015.
 */
public class SaveManager {

    Activity activity = null;
//    String saveFileManager = "mapsPlayed.txt";
    public SaveManager(Activity activity){
        this.activity = activity;
    }

    public boolean getMapsStateLevel(String mapPath, String fileName)
    {
        return getMapsState(mapPath, fileName, "Maps/");
    }

    public boolean getMapsStateSaved(String mapPath, String fileName)
    {
        return getMapsState(mapPath, fileName, "");
    }

    /**
     * Checks if the map has been played.
     * @param mapPath The path of the map.
     * @param fileName The name of the file.
     * @param folder The folder where the file is located.
     * @return True if the map has been played, false otherwise.
     */
    public boolean getMapsState(String mapPath, String fileName, String folder)
    {
        String playedMaps = FileReadWrite.readPrivateData(activity, fileName);

        if(playedMaps.equals(""))
            return false;

        String[] maps = playedMaps.split("[\\r\\n]+");

        for(String map : maps)
        {
            if(mapPath.equals(folder+map))
            {
                return true;
            }
        }
        return false;
    }

    /** Checks if a map has been solved
     */
    public boolean getMapsSolved(String mapPath, String fileName, String folder) {
        String content = FileReadWrite.readPrivateData(activity, fileName);
        return content != null && !content.equals("") && content.contains(mapPath);
    }

    /** Save map completion data in the form of "mapPath:minMoves:moves:squares:time"
     * @param mapPath Path to the map file
     * @param minMoves Minimum number of moves required to complete
     * @param moves Number of moves taken to complete
     * @param squares Number of squares used
     * @param solveTimeSeconds Time taken to solve the map
     */
    public void saveMapCompletion(String mapPath, int minMoves, int moves, int squares, int solveTimeSeconds) {
        // Save that the map was completed
        FileReadWrite.appendPrivateData(activity, "mapsSolved.txt", mapPath + "\n");

        // Save detailed completion data
        FileReadWrite.appendPrivateData(activity, "mapsStats.txt",
        mapPath + ":"
                + minMoves + ":"
                + moves + ":"
                + squares + ":" +
                + solveTimeSeconds + "\n");
        System.out.println("Map completion data saved:" + mapPath + ":" + minMoves + ":" + moves + ":" + squares + ":" + solveTimeSeconds);
    }

    /**
     * read map completion data in the form of "mapPath:minMoves:moves:squares:time"
     *
     * @param mapPath Path to the map file
     * @return Array of integers containing the completion data: minMoves, moves, squares, time
     */
    public HashMap<String, Integer> getMapLevelData(String mapPath, int level) {
        String fileName = "mapsStats.txt";
        String data = FileReadWrite.readPrivateData(activity, fileName);
        if (data == null || data.equals("")) {
            return null;
        }
        // find map data in data
        String[] lines = data.split("[\\r\\n]+");
        String lineWithMinMoves = "";
        int thisMinMoves = 999999;
        for (String line : lines) {
            if (line.startsWith(mapPath)) {
                String[] parts = line.split(":");
                if (parts.length >= 5) {
                    if(Integer.parseInt(parts[2]) < thisMinMoves)
                    {
                        thisMinMoves = Integer.parseInt(parts[2]);
                        lineWithMinMoves = line;
                    }
                }
            }
        }
        for (String line : lines) {
            if (line.startsWith(mapPath) && line.equals(lineWithMinMoves)) {
                String[] parts = line.split(":");
                if (parts.length >= 5) {
                    HashMap<String, Integer> mapData = new HashMap<>();
                    mapData.put("minMoves", Integer.parseInt(parts[1]));
                    mapData.put("moves", Integer.parseInt(parts[2]));
                    mapData.put("squares", Integer.parseInt(parts[3]));
                    mapData.put("time", Integer.parseInt(parts[4]));
                    System.out.println("DEBUG Map read: " + mapPath + ":" + mapData.get("minMoves") + ":" + mapData.get("moves") + ":" + mapData.get("squares") + ":" + mapData.get("time"));
                    return mapData;
                }
            }
        }
        return null;
    }

    public int getButtonLevels(Boolean played, Boolean up)
    {
        if (played)
        {
            if(up)
                return R.drawable.bt_start_up_played;
            else
                return R.drawable.bt_start_down_played;
        }
        else
        {
            if(up)
                return R.drawable.bt_start_up;
            else
                return R.drawable.bt_start_down;
        }
    }

    public int getButtonSaved(String mapPath, Boolean up)
    {
        if (getMapsStateSaved(mapPath, "mapsSaved.txt"))
        {
            if(up)
                return R.drawable.bt_start_up_saved_used;
            else
                return R.drawable.bt_start_down_saved_used;
        }
        else
        {
            if(up)
                return R.drawable.bt_start_up_saved;
            else
                return R.drawable.bt_start_down_saved;
        }
    }

    public int getButtonAutoSaved(String mapPath, Boolean up)
    {
        if (getMapsStateSaved(mapPath, "mapsSaved.txt"))
        {
            if(up)
                return R.drawable.bt_start_up_played;
            else
                return R.drawable.bt_start_down;
        }
        else
        {
            if(up)
                return R.drawable.bt_start_up_played;
            else
                return R.drawable.bt_start_down;
        }
    }
}

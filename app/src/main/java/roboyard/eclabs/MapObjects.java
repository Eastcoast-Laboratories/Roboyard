package roboyard.eclabs;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapObjects {

    /*
     * Constructor of the class
     */
    public MapObjects(){

    }

    /*
     * Extract the data from the _data string
     * Returns a list of all extracted elements
     * Extracts the data from the _data string
     * Returns a list of all extracted elements
     * @return ArrayList GridElement
     */
    public static ArrayList extractDataFromString(String data)
    {
        int x = 0;
        int y = 0;

        ArrayList<GridElement> elements = new ArrayList<>();

        // r=robot (v=green, j=yellow, red, blue)
        // c=target
        // m=wall (horizontal, vertical)
        List<String> objectTypes = Arrays.asList("mh", "mv", "rv", "rj", "rr", "rb", "cv", "cj", "cr", "cb", "cm");

        // Loop for each type of object
        for(final String objectType: objectTypes) {

            List<String> allMatches = new ArrayList<>();

            // Retrieve all the lines corresponding to the type of object sought
            Matcher m = Pattern.compile(objectType+"\\d+,\\d+;").matcher(data);
            while (m.find()) {
                allMatches.add(m.group());
            }

            for(final String line: allMatches) {
                String[] values = line.split(",");
                // Extract x and y coordinates
                if(values.length>=2) {
                    String valueX = values[0].replaceAll("[^0-9]", "");

                    if (!valueX.equals("")) {
                        x = Integer.decode(valueX);
                    }

                    String valueY = values[1].replaceAll("[^0-9]", "");

                    if (!valueY.equals("")) {
                        y = Integer.decode(valueY);
                    }

                    // Create a GridElement corresponding to the current object and add it to the list
                    GridElement p = new GridElement(x, y, objectType);
                    elements.add(p);
                }
            }
        }
        return elements;
    }

    /*
     * Generate a string containing all the information from the list
     * @param data List of GridElement containing all the content of the map
     * @return String containing all the map information like
     *   mv16,14;
     *   mv16,15;
     *   cj14,6;
     *   rr12,9;
     *   ...
     */
    public static String createStringFromList( ArrayList<GridElement> data)
    {
        StringBuilder content = new StringBuilder();

        // For each element, add a line containing the type as well as the x and y position
        for(GridElement currentElement : data)
        {
            content.append(currentElement.getType()).append(currentElement.getX()).append(",").append(currentElement.getY()).append(";\n");
        }

        return content.toString();
    }
}

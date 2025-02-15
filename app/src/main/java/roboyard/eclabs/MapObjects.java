package roboyard.eclabs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapObjects {

    // Compile patterns once as static fields for reuse
    private static final Pattern BOARD_SIZE_PATTERN = Pattern.compile("board:(\\d+),(\\d+);");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("(\\d+),(\\d+);");

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
     * @return ArrayList<GridElement> List of grid elements
     */
    public static ArrayList<GridElement> extractDataFromString(String data)
    {
        int x = 0;
        int y = 0;

        // First check if the save contains board size information
        Matcher boardSizeMatcher = BOARD_SIZE_PATTERN.matcher(data);
        if (boardSizeMatcher.find()) {
            int boardX = Integer.parseInt(boardSizeMatcher.group(1));
            int boardY = Integer.parseInt(boardSizeMatcher.group(2));
            // Update and persist board size for this game
            MainActivity activity = GridGameScreen.gameManager.getActivity();
            activity.setBoardSize(activity, boardX, boardY);
        }

        ArrayList<GridElement> elements = new ArrayList<>();

        // r=robot (v=green, j=yellow, red, blue)
        // c=target
        // m=wall (horizontal, vertical)
        // v=vertical wall
        // h=horizontal wall
        List<String> objectTypes = Arrays.asList("mh", "mv", // wall (mur)
                "robot_green", "robot_yellow", "robot_red", "robot_blue", // robots
                "target_green", "target_yellow", "target_red", "target_blue", "target_multi"); // targets (cible)

        // Process each line of data once
        // Use negative limit to keep empty strings at end
        String[] lines = data.split(";", -1);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Find matching object type
            String matchedType = null;
            for (String type : objectTypes) {
                if (line.startsWith(type)) {
                    matchedType = type;
                    break;
                }
            }
            
            if (matchedType != null) {
                // Extract coordinates using indexOf instead of regex
                int coordStart = matchedType.length();
                String coords = line.substring(coordStart).trim();
                
                int commaIndex = coords.indexOf(',');
                if (commaIndex > 0) {
                    try {
                        String xStr = coords.substring(0, commaIndex).trim();
                        String yStr = coords.substring(commaIndex + 1).trim();
                        x = Integer.parseInt(xStr);
                        y = Integer.parseInt(yStr);
                        elements.add(new GridElement(x, y, matchedType));
                    } catch (NumberFormatException e) {
                    }
                } else {
                }
            } else {
            }
        }
        
        return elements;
    }

    /*
     * Generate a string containing all the information from the list
     * @param data List of GridElement containing all the content of the map
     * @param shortString Boolean if the string is squeezed into 5 letters
     * @return String containing all the map information like
     *   mv16,14;
     *   mv16,15;
     *   cj14,6;
     *   rr12,9;
     *   ...
     */
    public static String createStringFromList( ArrayList<GridElement> data, boolean shortString)
    {
        StringBuilder content = new StringBuilder();

        // Add board size information
        content.append("board:").append(MainActivity.boardSizeX).append(",").append(MainActivity.boardSizeY).append(";\n");

        // For each element, add a line containing the type as well as the x and y position
        for(GridElement currentElement : data)
        {
            content.append(currentElement.getType()).append(currentElement.getX()).append(",").append(currentElement.getY()).append(";\n");
        }

        String stringContent;
        if (shortString) {
            stringContent = generateUnique5LetterFromString(content.toString());
        }else{
            stringContent = content.toString();
        }

        return stringContent;
    }

    /*
     * set a color for the unique string depending on the value of the string,
     * the color is generated from the hex value of the string
     * - there are only generated light colors
     *
     * @param input The input string
     * @return A light color in hexadecimal format
     */
    public static String generateHexColorFromString(String input) {
        if (input == null || input.isEmpty()) {
            return "#000000";
        }
        
        // Simple and fast hash function
        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = 31 * hash + input.charAt(i);
        }
        
        // Ensure colors are light by setting high base values
        int red = 128 + Math.abs(hash % 128);        // 128-255
        int green = 128 + Math.abs((hash >> 8) % 128);  // 128-255
        int blue = 128 + Math.abs((hash >> 16) % 128);  // 128-255
        
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    /*
     * Generate a unique string from the input string
     * @param input The input string
     * @return A unique 5-letter string altering between vowels and consonants
     */
    public static String generateUnique5LetterFromString(String input) {
        try {
            // Create a SHA-256 message digest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get the hash bytes for the input string
            byte[] hashBytes = digest.digest(input.getBytes());

            // Define vowels and consonants
            char[] vowels = {'A', 'E', 'I', 'O', 'U'};
            char[] consonants = {'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'};

            // Convert the hash bytes to a 5-letter string, alternating between vowels and consonants
            StringBuilder uniqueString = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                // Convert each byte to a positive integer and take modulo 26 to get a letter
                int index = Math.abs(hashBytes[i]) % (i % 2 == 0 ? consonants.length : vowels.length);
                // Map the index to an uppercase letter (ASCII code for 'A' is 65)
                char letter = (i % 2 == 0 ? consonants[index] : vowels[index]);
                // Append the letter to the unique string
                uniqueString.append(letter);
            }
            return uniqueString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Handle NoSuchAlgorithmException if the specified algorithm is not available
            e.printStackTrace();
            return null; // or throw an exception
        }
    }
}

package roboyard.eclabs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        // v=vertical wall
        // h=horizontal wall
        List<String> objectTypes = Arrays.asList("mh", "mv", // wall (mur)
                "rv", "rj", "rr", "rb", // robots
                "cv", "cj", "cr", "cb", "cm"); // targets (cible)
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
        try {
            // Create a SHA-256 message digest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get the hash bytes for the input string
            byte[] hashBytes = digest.digest(input.getBytes());

            // Convert the hash bytes to a 6-letter hexadecimal string
            StringBuilder color = new StringBuilder("#");
            for (int i = 0; i < 6; i++) {
                // Convert each byte to a positive integer and take modulo 16 to get a hexadecimal digit
                int index = Math.abs(hashBytes[i]) % 16;
                // if it is the first, third or 5th digit, the color should be lighter
                if (i % 2 == 0 && index <= 4) {
                    index += 5;
                }
                // Map the index to a hexadecimal digit
                char digit = (char) (index < 10 ? '0' + index : 'A' + index - 10);

                // Append the digit to the color string
                color.append(digit);
            }
            return color.toString();
        } catch (NoSuchAlgorithmException e) {
            // Handle NoSuchAlgorithmException if the specified algorithm is not available
            e.printStackTrace();
            return null; // or throw an exception
        }
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

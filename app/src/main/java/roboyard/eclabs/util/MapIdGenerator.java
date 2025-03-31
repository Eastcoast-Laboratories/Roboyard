package roboyard.eclabs.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import roboyard.eclabs.GridElement;
import timber.log.Timber;

/**
 * Utility class to generate unique 5-letter IDs for maps
 */
public class MapIdGenerator {
    
    /**
     * Generate a unique 5-letter ID from a list of grid elements
     * @param gridElements List of game elements
     * @return A 5-letter unique ID string
     */
    public static String generateUniqueId(ArrayList<GridElement> gridElements) {
        if (gridElements == null || gridElements.isEmpty()) {
            Timber.w("MapIdGenerator: Attempted to generate ID from empty grid elements");
            return "EMPTY";
        }
        
        // Build a string representation of the map for hashing
        StringBuilder mapData = new StringBuilder();
        for (GridElement element : gridElements) {
            mapData.append(element.getType())
                   .append(element.getX())
                   .append(",")
                   .append(element.getY())
                   .append(";");
        }
        
        Timber.d("MapIdGenerator: Generated map data for hashing (%d characters)", mapData.length());
        return generateUnique5LetterFromString(mapData.toString());
    }

    /**
     * Generate a unique 5-letter string from an input string
     * The resulting string alternates between consonants and vowels
     * for better readability and memorability
     * 
     * @param input The input string to hash
     * @return A 5-letter unique ID string
     */
    public static String generateUnique5LetterFromString(String input) {
        try {
            Timber.d("MapIdGenerator: Generating 5-letter ID from input string");
            
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
                // Convert each byte to a positive integer and take modulo of the appropriate array length
                int index = Math.abs(hashBytes[i]) % (i % 2 == 0 ? consonants.length : vowels.length);
                // Map the index to an uppercase letter
                char letter = (i % 2 == 0 ? consonants[index] : vowels[index]);
                // Append the letter to the unique string
                uniqueString.append(letter);
            }
            
            String result = uniqueString.toString();
            Timber.d("MapIdGenerator: Generated unique ID: %s", result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "MapIdGenerator: Failed to generate unique ID, SHA-256 algorithm not available");
            return "ERROR";
        }
    }
}

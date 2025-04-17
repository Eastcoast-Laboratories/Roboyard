package roboyard.eclabs.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import roboyard.eclabs.GameManager;
import roboyard.eclabs.R;
import roboyard.logic.core.GameState;
import roboyard.logic.core.Preferences;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main activity for the game, hosts the fragment-based UI.
 * Acts as the container for all game fragments and provides access to the GameStateManager.
 */
public class MainFragmentActivity extends AppCompatActivity {
    
    private GameStateManager gameStateManager;
    private NavController navController;
    private String deepLinkData = null;

    // Forward to the regular MainActivity's static methods
    public static int getBoardWidth() {
        int width = roboyard.ui.activities.MainActivity.getBoardWidth();
        Timber.d("[BOARD_SIZE_DEBUG] UI MainActivity.getBoardWidth() called, returning: %d", width);
        return width;
    }

    public static int getBoardHeight() {
        int height = roboyard.ui.activities.MainActivity.getBoardHeight();
        Timber.d("[BOARD_SIZE_DEBUG] UI MainActivity.getBoardHeight() called, returning: %d", height);
        return height;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize the static Preferences at app startup
        roboyard.logic.core.Preferences.initialize(getApplicationContext());
        Timber.d("[PREFERENCES] Initialized with robotCount=%d, targetColors=%d", 
                roboyard.logic.core.Preferences.robotCount, 
                roboyard.logic.core.Preferences.targetColors);
        
        // Log the board size at startup
        Timber.d("[BOARD_SIZE_DEBUG] UI MainActivity onCreate - Current board size: %dx%d", 
                 getBoardWidth(), getBoardHeight());
        
        // Initialize the GameStateManager as a ViewModel
        gameStateManager = new ViewModelProvider(this).get(GameStateManager.class);
        
        // Set up the Navigation controller
        NavHostFragment navHostFragment = 
            (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        
        // Set up accessibility services
        setupAccessibility();
        
        applyLanguageSettings();
        
        // Handle deep link intent
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle the new intent, e.g., if the app was already running and a new deep link is clicked
        handleIntent(intent);
    }
    
    /**
     * Handle incoming intents, including deep links
     * @param intent The intent to handle
     */
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            // This is a deep link
            Timber.d("[DEEPLINK] Received deep link: %s", data.toString());
            
            // Extract parameters
            String mapData = data.getQueryParameter("data");
            String mapName = data.getQueryParameter("name");
            String difficultyStr = data.getQueryParameter("difficulty");
            
            // Log the raw map data for debugging
            if (mapData != null) {
                Timber.d("[DEEPLINK_RAW] Raw map data length: %d", mapData.length());
                // Log first 100 chars to see format
                String previewData = mapData.length() > 100 ? mapData.substring(0, 100) + "..." : mapData;
                Timber.d("[DEEPLINK_RAW] Map data preview: %s", previewData);
            } else {
                Timber.e("[DEEPLINK_RAW] Map data is null");
            }
            
            // Parse difficulty if provided
            int difficulty = -1;
            if (difficultyStr != null && !difficultyStr.isEmpty()) {
                try {
                    difficulty = Integer.parseInt(difficultyStr);
                    Timber.d("[DEEPLINK] Parsed difficulty: %d", difficulty);
                } catch (NumberFormatException e) {
                    Timber.e("[DEEPLINK] Invalid difficulty value: %s", difficultyStr);
                }
            }
            
            if (mapData != null && !mapData.isEmpty()) {
                Timber.d("[DEEPLINK] Extracted map data: %s", mapData.substring(0, Math.min(50, mapData.length())));
                deepLinkData = mapData;
                
                // Check if the data is in web format (starts with "name:" or contains "mh" wall markers)
                if (mapData.startsWith("name:") || mapData.contains("mh") || mapData.contains("mv")) {
                    Timber.d("[DEEPLINK_FORMAT] Detected web format, converting to app format");
                    String convertedMapData = convertWebFormatToAppFormat(mapData);
                    Timber.d("[DEEPLINK_CONVERT] Converted map data preview: %s", 
                             convertedMapData.substring(0, Math.min(100, convertedMapData.length())));
                    
                    // Extract map name from web format if not provided as parameter
                    if (mapName == null && mapData.startsWith("name:")) {
                        int endIndex = mapData.indexOf(";");
                        if (endIndex > 5) {
                            mapName = mapData.substring(5, endIndex);
                            Timber.d("[DEEPLINK] Extracted map name from web format: %s", mapName);
                        }
                    }
                    
                    // Process the converted map data
                    processDeepLinkMapData(convertedMapData, mapName, difficulty);
                } else {
                    // Process the map data with the additional parameters
                    processDeepLinkMapData(mapData, mapName, difficulty);
                }
            } else {
                Timber.w("[DEEPLINK] No map data found in deep link");
            }
        }
    }
    
    /**
     * Convert the web format map data to the app format
     * Web format: "name:NAME;num_moves:N;solution:board:W,H;mh0,0;mv0,1;...robot_red10,4;..."
     * App format: The format expected by GameState.parseFromSaveData()
     * 
     * @param webFormatData The map data in web format
     * @return The map data in app format
     */
    private String convertWebFormatToAppFormat(String webFormatData) {
        Timber.d("[DEEPLINK_CONVERT] Converting web format to app format");
        
        StringBuilder appFormat = new StringBuilder();
        String mapName = "Web Map";
        int width = 16;  // Default values
        int height = 16;
        
        // Parse the web format - split by newlines first, then by semicolons
        String[] lines = webFormatData.split("\\n");
        List<String> parts = new ArrayList<>();
        
        // Process each line and add to parts
        for (String line : lines) {
            // Split each line by semicolons and add each part
            String[] lineParts = line.split(";");
            for (String part : lineParts) {
                if (!part.trim().isEmpty()) {
                    parts.add(part.trim());
                }
            }
        }
        
        Timber.d("[DEEPLINK_CONVERT] Parsed %d parts from web format", parts.size());
        
        // Log all parts for debugging
        for (int i = 0; i < Math.min(parts.size(), 40); i++) {
            Timber.d("[DEEPLINK_PARSE] Part %d: %s", i, parts.get(i));
        }
        
        // Extract map name and board dimensions
        for (String part : parts) {
            if (part.startsWith("name:")) {
                mapName = part.substring(5);
                Timber.d("[DEEPLINK_CONVERT] Found map name: %s", mapName);
            } else if (part.contains("board:")) {
                int boardIndex = part.indexOf("board:");
                String dimensionsStr = part.substring(boardIndex + 6);
                String[] dimensions = dimensionsStr.split(",");
                if (dimensions.length == 2) {
                    try {
                        width = Integer.parseInt(dimensions[0]);
                        height = Integer.parseInt(dimensions[1]);
                        Timber.d("[DEEPLINK_CONVERT] Found board dimensions: %dx%d", width, height);
                    } catch (NumberFormatException e) {
                        Timber.e("[DEEPLINK_CONVERT] Error parsing board dimensions: %s", e.getMessage());
                    }
                }
            }
        }
        
        // Start building the app format
        appFormat.append("#MAPNAME:").append(mapName)
                .append(";TIME:0;MOVES:0;UNIQUE_MAP_ID:WEBMP\n");
        
        // Add board dimensions
        appFormat.append("WIDTH:").append(width).append(";\n");
        appFormat.append("HEIGHT:").append(height).append(";\n");
        
        // Create empty board
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x > 0) {
                    appFormat.append(",");
                }
                appFormat.append("0"); // Empty cell
            }
            appFormat.append("\n");
        }
        
        // Process targets - important to add at least one target as the app requires this
        List<String> targetParts = new ArrayList<>();
        for (String part : parts) {
            if (part.startsWith("target_")) {
                targetParts.add(part);
            }
        }
        
        appFormat.append("TARGET_SECTION:\n");
        // If no targets found, add a default one to prevent app errors
        if (targetParts.isEmpty()) {
            Timber.e("[DEEPLINK_CONVERT] No targets found in web format! Adding a default target.");
            appFormat.append("TARGET_SECTION:8,8,0\n");
        } else {
            for (String part : targetParts) {
                try {
                    // Format is now: target_colorX,Y
                    // The expected format is: target_yellow4,15 (color followed directly by coords)
                    Timber.d("[DEEPLINK_TARGET_PARSE] Parsing target: %s", part);
                    
                    // Extract the color part
                    String colorPart = part.substring(7); // Skip "target_"
                    
                    // Find where the color name ends and coordinates begin
                    int digitPos = -1;
                    for (int i = 0; i < colorPart.length(); i++) {
                        if (Character.isDigit(colorPart.charAt(i))) {
                            digitPos = i;
                            break;
                        }
                    }
                    
                    if (digitPos == -1) {
                        Timber.e("[DEEPLINK_TARGET_PARSE] Could not find coordinate digits in: %s", colorPart);
                        continue;
                    }
                    
                    String colorStr = colorPart.substring(0, digitPos);
                    String coordsStr = colorPart.substring(digitPos);
                    
                    Timber.d("[DEEPLINK_TARGET_PARSE] Extracted color: '%s', coords: '%s'", colorStr, coordsStr);
                    
                    String[] coords = coordsStr.split(",");
                    if (coords.length == 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int colorId = getColorId(colorStr);
                        
                        appFormat.append("TARGET_SECTION:")
                                .append(x).append(",")
                                .append(y).append(",")
                                .append(colorId).append("\n");
                        
                        Timber.d("[DEEPLINK_CONVERT] Added target at (%d,%d) with color %d from: %s", 
                                x, y, colorId, part);
                    }
                } catch (Exception e) {
                    Timber.e("[DEEPLINK_CONVERT] Error parsing target: %s - %s", part, e.getMessage());
                }
            }
        }
        
        // Process walls
        appFormat.append("WALLS:\n");
        
        // Parse horizontal walls
        for (String part : parts) {
            if (part.startsWith("mh")) {
                try {
                    // Format: mhX,Y
                    String coordsStr = part.substring(2);
                    String[] coords = coordsStr.split(",");
                    
                    if (coords.length == 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        
                        appFormat.append("H,")
                                .append(x).append(",")
                                .append(y).append("\n");
                        
                        Timber.d("[DEEPLINK_CONVERT] Added horizontal wall at (%d,%d) from: %s", x, y, part);
                    }
                } catch (Exception e) {
                    Timber.e("[DEEPLINK_CONVERT] Error parsing horizontal wall: %s - %s", part, e.getMessage());
                }
            }
        }
        
        // Parse vertical walls
        for (String part : parts) {
            if (part.startsWith("mv")) {
                try {
                    // Format: mvX,Y
                    String coordsStr = part.substring(2);
                    String[] coords = coordsStr.split(",");
                    
                    if (coords.length == 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        
                        appFormat.append("V,")
                                .append(x).append(",")
                                .append(y).append("\n");
                        
                        Timber.d("[DEEPLINK_CONVERT] Added vertical wall at (%d,%d) from: %s", x, y, part);
                    }
                } catch (Exception e) {
                    Timber.e("[DEEPLINK_CONVERT] Error parsing vertical wall: %s - %s", part, e.getMessage());
                }
            }
        }
        
        // Process robots
        List<String> robotParts = new ArrayList<>();
        for (String part : parts) {
            if (part.startsWith("robot_")) {
                robotParts.add(part);
            }
        }
        
        appFormat.append("ROBOTS:\n");
        // If no robots found, add a default one to prevent app errors
        if (robotParts.isEmpty()) {
            Timber.e("[DEEPLINK_CONVERT] No robots found in web format! Adding a default robot.");
            appFormat.append("4,4,0\n");
        } else {
            for (String part : robotParts) {
                try {
                    // Format is now: robot_colorX,Y
                    // The expected format is: robot_red10,4 (color followed directly by coords)
                    Timber.d("[DEEPLINK_ROBOT_PARSE] Parsing robot: %s", part);
                    
                    // Extract the color part
                    String colorPart = part.substring(6); // Skip "robot_"
                    
                    // Find where the color name ends and coordinates begin
                    int digitPos = -1;
                    for (int i = 0; i < colorPart.length(); i++) {
                        if (Character.isDigit(colorPart.charAt(i))) {
                            digitPos = i;
                            break;
                        }
                    }
                    
                    if (digitPos == -1) {
                        Timber.e("[DEEPLINK_ROBOT_PARSE] Could not find coordinate digits in: %s", colorPart);
                        continue;
                    }
                    
                    String colorStr = colorPart.substring(0, digitPos);
                    String coordsStr = colorPart.substring(digitPos);
                    
                    Timber.d("[DEEPLINK_ROBOT_PARSE] Extracted color: '%s', coords: '%s'", colorStr, coordsStr);
                    
                    String[] coords = coordsStr.split(",");
                    if (coords.length == 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int colorId = getColorId(colorStr);
                        
                        appFormat.append(x).append(",")
                                .append(y).append(",")
                                .append(colorId).append("\n");
                        
                        Timber.d("[DEEPLINK_CONVERT] Added robot at (%d,%d) with color %d from: %s", 
                                x, y, colorId, part);
                    }
                } catch (Exception e) {
                    Timber.e("[DEEPLINK_CONVERT] Error parsing robot: %s - %s", part, e.getMessage());
                }
            }
        }
        
        // Add initial positions section (same as robots)
        appFormat.append("INITIAL_POSITIONS:\n");
        if (robotParts.isEmpty()) {
            appFormat.append("4,4,0\n");
        } else {
            for (String part : robotParts) {
                try {
                    // Format is now: robot_colorX,Y
                    // The expected format is: robot_red10,4 (color followed directly by coords)
                    
                    // Extract the color part
                    String colorPart = part.substring(6); // Skip "robot_"
                    
                    // Find where the color name ends and coordinates begin
                    int digitPos = -1;
                    for (int i = 0; i < colorPart.length(); i++) {
                        if (Character.isDigit(colorPart.charAt(i))) {
                            digitPos = i;
                            break;
                        }
                    }
                    
                    if (digitPos == -1) {
                        continue;
                    }
                    
                    String colorStr = colorPart.substring(0, digitPos);
                    String coordsStr = colorPart.substring(digitPos);
                    
                    String[] coords = coordsStr.split(",");
                    if (coords.length == 2) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int colorId = getColorId(colorStr);
                        
                        appFormat.append(x).append(",")
                                .append(y).append(",")
                                .append(colorId).append("\n");
                    }
                } catch (Exception e) {
                    Timber.e("[DEEPLINK_CONVERT] Error parsing robot for initial positions: %s - %s", 
                            part, e.getMessage());
                }
            }
        }
        
        String result = appFormat.toString();
        Timber.d("[DEEPLINK_CONVERT] Conversion complete, generated app format with length: %d", result.length());
        
        // Log a preview of the result
        String preview = result.substring(0, Math.min(200, result.length()));
        Timber.d("[DEEPLINK_CONVERT] Result preview: %s", preview);
        
        return result;
    }
    
    /**
     * Convert color name to color ID
     * @param colorName Color name (red, blue, green, yellow)
     * @return Color ID (0-3)
     */
    private int getColorId(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red": return 0;
            case "pink": return 0;
            case "blue": return 1;
            case "green": return 2;
            case "yellow": return 3;
            case "silver": return 4;
            default: return 0; // Default to red
        }
    }
    
    /**
     * Process the map data received from a deep link
     * @param mapData The serialized map data to process
     * @param mapName Optional custom map name (can be null)
     * @param difficulty Optional difficulty level (use -1 if not specified)
     */
    private void processDeepLinkMapData(String mapData, String mapName, int difficulty) {
        try {
            // Parse the map data into a GameState
            Timber.d("[DEEPLINK_PROCESS] Beginning to parse map data into GameState");
            GameState gameState = GameState.parseFromSaveData(mapData, this);
            
            if (gameState != null) {
                // If we successfully parsed the game state, load it
                Timber.d("[DEEPLINK_PROCESS] Successfully parsed game state: board size=%dx%d, elements=%d", 
                        gameState.getWidth(), gameState.getHeight(), gameState.getGameElements().size());
                
                // Log the types of elements in the game state
                int robotCount = 0;
                int targetCount = 0;
                int wallCount = 0;
                
                for (GameElement element : gameState.getGameElements()) {
                    if (element.getType() == GameElement.TYPE_ROBOT) {
                        robotCount++;
                        Timber.d("[DEEPLINK_ELEMENTS] Robot at (%d,%d) with color %d", 
                                element.getX(), element.getY(), element.getColor());
                    } else if (element.getType() == GameElement.TYPE_TARGET) {
                        targetCount++;
                        Timber.d("[DEEPLINK_ELEMENTS] Target at (%d,%d) with color %d", 
                                element.getX(), element.getY(), element.getColor());
                    } else if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL || element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                        wallCount++;
                    }
                }
                
                Timber.d("[DEEPLINK_ELEMENTS] Game state contains: %d robots, %d targets, %d walls", 
                        robotCount, targetCount, wallCount);
                
                // Override the map name if provided in the deep link
                if (mapName != null && !mapName.isEmpty()) {
                    gameState.setLevelName(mapName);
                    Timber.d("[DEEPLINK_PROCESS] Set custom map name: %s", mapName);
                }
                
                // Override the difficulty if specified in the deep link
                if (difficulty >= 0) {
                    // Store the difficulty for this map
                    gameStateManager.setDeepLinkDifficulty(difficulty);
                    Timber.d("[DEEPLINK_PROCESS] Set map difficulty: %d", difficulty);
                }
                
                // Check which fragment is currently displayed
                if (navController != null && navController.getCurrentDestination() != null) {
                    int currentDestId = navController.getCurrentDestination().getId();
                    Timber.d("[DEEPLINK_NAV] Current destination ID: %d, modernGameFragment ID: %d", 
                            currentDestId, R.id.modernGameFragment);
                }
                
                // Navigate to the game fragment if we're not already there
                if (navController != null && navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getId() != R.id.modernGameFragment) {
                    Timber.d("[DEEPLINK_NAV] Navigating to modern game fragment");
                    navController.navigate(R.id.modernGameFragment);
                } else {
                    Timber.d("[DEEPLINK_NAV] Already in modern game fragment or navigation failed");
                }
                
                // Set the game state in the GameStateManager
                Timber.d("[DEEPLINK_PROCESS] Setting game state in GameStateManager");
                gameStateManager.setGameState(gameState);
            } else {
                Timber.e("[DEEPLINK_PROCESS] Failed to parse map data");
            }
        } catch (Exception e) {
            Timber.e(e, "[DEEPLINK_PROCESS] Error processing map data: %s", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the deep link data if available
     * @return The deep link data or null if none is available
     */
    public String getDeepLinkData() {
        return deepLinkData;
    }
    
    /**
     * Configure accessibility features
     */
    private void setupAccessibility() {
        // Set content descriptions on key elements
        // This is in addition to the content descriptions set in XML and fragment code
    }
    
    /**
     * Get the game state manager
     * @return GameStateManager instance
     */
    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    /**
     * LEGACY COMPATIBILITY METHOD
     * This method is provided for backward compatibility with code that still uses GameManager
     * In the new architecture, we don't use GameManager anymore
     * @return null - GameManager is no longer used
     * @deprecated Use getGameStateManager() instead
     */
    @Deprecated
    public GameManager getGameManager() {
        // Return null as we don't use GameManager anymore
        // Legacy code should be updated to use GameStateManager
        return null;
    }
    
    private void applyLanguageSettings() {
        try {
            // Get saved language setting
            String languageCode = roboyard.logic.core.Preferences.appLanguage;
            Timber.d("ROBOYARD_LANGUAGE: Setting app language on application level: %s", languageCode);
            
            if (languageCode != null && !languageCode.isEmpty()) {
                // Apply language change
                Locale locale = new Locale(languageCode);
                Locale.setDefault(locale);
                
                Resources resources = getResources();
                Configuration config = new Configuration(resources.getConfiguration());
                config.setLocale(locale); // Verwende die neuere Methode statt config.locale = locale
                
                resources.updateConfiguration(config, resources.getDisplayMetrics());
                
                Timber.d("ROBOYARD_LANGUAGE: Successfully applied language %s at application level", languageCode);
            }
        } catch (Exception e) {
            Timber.e(e, "ROBOYARD_LANGUAGE: Error applying language settings at application level");
        }
    }
}

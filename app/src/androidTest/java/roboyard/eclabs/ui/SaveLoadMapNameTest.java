package roboyard.eclabs.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.Map;

import roboyard.eclabs.FileReadWrite;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.MainActivity;
import roboyard.eclabs.MapObjects;

import timber.log.Timber;

/**
 * Test for verifying that saved games correctly maintain the unique map name
 * when saving and loading.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class SaveLoadMapNameTest {

    private static final String TEST_MAP_NAME = "TestMapUniqueName";
    private static final int TEST_SAVE_SLOT = 5; // Use a specific slot for testing
    
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);
    
    private Context context;
    private GameStateManager gameStateManager;
    private GridGameScreen gridGameScreen;
    
    @Before
    public void setup() {
        // Get context
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Initialize test components
        activityScenarioRule.getScenario().onActivity(activity -> {
            // Get or create GameStateManager
            gameStateManager = new GameStateManager(activity);
            
            // Create a test GridGameScreen with a known map name
            gridGameScreen = new GridGameScreen(activity);
            gridGameScreen.setMapName(TEST_MAP_NAME);
            
            // Set up a simple test map
            setupTestMap(gridGameScreen);
        });
    }
    
    @After
    public void cleanup() {
        // Delete the test save if it exists
        activityScenarioRule.getScenario().onActivity(activity -> {
            String savePath = FileReadWrite.getSaveGamePath(activity, TEST_SAVE_SLOT);
            File saveFile = new File(savePath);
            if (saveFile.exists()) {
                saveFile.delete();
            }
        });
    }
    
    /**
     * Sets up a simple test map with some grid elements
     */
    private void setupTestMap(GridGameScreen gameScreen) {
        // Add some basic grid elements for a simple map
        List<GridElement> elements = gameScreen.getGridElements();
        elements.clear();
        
        // Add a robot
        elements.add(new GridElement("robot_red", 3, 3));
        
        // Add a target
        elements.add(new GridElement("target_red", 6, 6));
        
        // Add some walls
        elements.add(new GridElement("mh", 2, 2)); // horizontal wall
        elements.add(new GridElement("mv", 5, 5)); // vertical wall
        
        // Update the grid
        gameScreen.setGridElements(elements);
    }
    
    @Test
    public void testSaveAndLoadWithMapName() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            // 1. Save the game to a specific slot
            boolean saveSuccess = gameStateManager.saveGame(gridGameScreen, TEST_SAVE_SLOT);
            assertTrue("Failed to save game", saveSuccess);
            
            // 2. Read the saved file directly to verify the map name is in the metadata
            String savePath = FileReadWrite.getSaveGamePath(activity, TEST_SAVE_SLOT);
            String saveData = FileReadWrite.loadAbsoluteData(savePath);
            assertNotNull("Save data should not be null", saveData);
            assertTrue("Save data should include metadata", saveData.startsWith("#"));
            
            Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
            assertNotNull("Metadata should not be null", metadata);
            assertEquals("Map name in metadata should match", TEST_MAP_NAME, metadata.get("MAPNAME"));
            
            // 3. Create a new GridGameScreen to load the saved game
            GridGameScreen loadedGameScreen = new GridGameScreen(activity);
            
            // 4. Load the saved game
            boolean loadSuccess = loadedGameScreen.setSavedGame(savePath);
            assertTrue("Failed to load saved game", loadSuccess);
            
            // 5. Verify the map name was preserved
            String loadedMapName = loadedGameScreen.getMapName();
            assertEquals("Map name should be preserved when loading a saved game", 
                TEST_MAP_NAME, loadedMapName);
            
            // 6. Verify grid elements were loaded correctly
            List<GridElement> loadedElements = loadedGameScreen.getGridElements();
            assertNotNull("Loaded elements should not be null", loadedElements);
            assertTrue("Should have loaded at least 4 elements", loadedElements.size() >= 4);
            
            // Log success
            Timber.d("Successfully verified map name preservation in save/load: %s", loadedMapName);
        });
    }
}

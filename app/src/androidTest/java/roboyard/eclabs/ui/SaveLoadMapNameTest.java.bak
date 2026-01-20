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
import java.util.Map;

import roboyard.eclabs.FileReadWrite;
import roboyard.eclabs.MainActivity;

import roboyard.ui.components.GameStateManager;

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

    @Before
    public void setup() {
        // Get context
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Initialize test components
        activityScenarioRule.getScenario().onActivity(activity -> {
            // Get or create GameStateManager
//            gameStateManager = new GameStateManager(context);
            
            // Set up a simple test map
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

    
    @Test
    public void testSaveAndLoadWithMapName() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            // 1. Save the game to a specific slot

            // 2. Read the saved file directly to verify the map name is in the metadata
            String savePath = FileReadWrite.getSaveGamePath(activity, TEST_SAVE_SLOT);
            String saveData = FileReadWrite.loadAbsoluteData(savePath);
            assertNotNull("Save data should not be null", saveData);
            assertTrue("Save data should include metadata", saveData.startsWith("#"));
            
            Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
            assertNotNull("Metadata should not be null", metadata);
            assertEquals("Map name in metadata should match", TEST_MAP_NAME, metadata.get("MAPNAME"));
            
            // 3. Create a new map to load the saved game

            // 4. Load the saved game

            // 5. Verify the map name was preserved
//            assertEquals("Map name should be preserved when loading a saved game",                 TEST_MAP_NAME, loadedMapName);
            
            // 6. Verify grid elements were loaded correctly

            // Log success
//            Timber.d("Successfully verified map name preservation in save/load: %s", loadedMapName);
        });
    }
}

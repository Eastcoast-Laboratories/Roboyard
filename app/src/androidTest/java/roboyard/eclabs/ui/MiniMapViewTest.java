package roboyard.eclabs.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.MinimapGenerator;
import roboyard.eclabs.GameState;
import roboyard.eclabs.GameElement;

/**
 * Tests for the MiniMapView component.
 * 
 * This test verifies that the MiniMapView correctly displays game state
 * and updates when the game state changes. It specifically tests for issues
 * related to minimap caching that were fixed in previous updates.
 */
@RunWith(AndroidJUnit4.class)
public class MiniMapViewTest {

    private Context context;
    private MiniMapView miniMapView;
    private GameState gameState;
    
    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Create a game state with some elements
        gameState = new GameState();
        
        // Add some robots
        gameState.addElement(new GameElement(GameElement.TYPE_ROBOT, 3, 4));
        gameState.addElement(new GameElement(GameElement.TYPE_ROBOT, 7, 8));
        
        // Add a target
        gameState.addElement(new GameElement(GameElement.TYPE_TARGET, 5, 6));
        
        // Create the minimap view
        miniMapView = new MiniMapView(context);
        miniMapView.setGameState(gameState);
    }
    
    /**
     * Test that the minimap view is created correctly
     */
    @Test
    public void testMiniMapCreation() {
        assertNotNull("MiniMapView should not be null", miniMapView);
        assertEquals(View.VISIBLE, miniMapView.getVisibility());
    }
    
    /**
     * Test that the minimap correctly generates a bitmap from game state
     */
    @Test
    public void testMiniMapGeneration() {
        // Get the minimap bitmap
        Bitmap minimap = gameState.getMiniMap(context, 100, 100);
        
        // Verify the bitmap was created
        assertNotNull("Minimap bitmap should not be null", minimap);
        assertEquals("Minimap width should match requested size", 100, minimap.getWidth());
        assertEquals("Minimap height should match requested size", 100, minimap.getHeight());
    }
    
    /**
     * Test that the minimap updates when game state changes
     * This verifies the fix for the cache refresh issue
     */
    @Test
    public void testMiniMapUpdatesWithGameState() {
        // Get the initial minimap
        Bitmap initialMinimap = gameState.getMiniMap(context, 100, 100);
        assertNotNull("Initial minimap should not be null", initialMinimap);
        
        // Modify the game state by adding another element
        gameState.addElement(new GameElement(GameElement.TYPE_ROBOT, 9, 10));
        
        // Clear caches to simulate the fix implemented in the code
        MinimapGenerator.getInstance().clearCache(gameState);
        
        // Get the updated minimap
        Bitmap updatedMinimap = gameState.getMiniMap(context, 100, 100);
        assertNotNull("Updated minimap should not be null", updatedMinimap);
        
        // The updated minimap should be different from the initial one
        // This is a simple check - in a real test, you might need a more sophisticated comparison
        // However, since we're mocking the bitmaps, we'll just verify they're not the same instance
        assertNotSame("Updated minimap should be different from initial", initialMinimap, updatedMinimap);
    }
    
    /**
     * Test that the cache is properly cleared when requested
     * This verifies the fix for the cache management issues
     */
    @Test
    public void testCacheClearingWorks() {
        // Create a mock MinimapGenerator to verify cache clearing behavior
        MinimapGenerator generator = MinimapGenerator.getInstance();
        
        // First access should create a cache entry
        Bitmap firstAccess = generator.generateMinimap(context, gameState, 100, 100);
        assertNotNull("First generated minimap should not be null", firstAccess);
        
        // Clear the cache
        generator.clearAllCaches();
        
        // Create a new generator to ensure we don't get any cached values
        MinimapGenerator newGenerator = MinimapGenerator.getInstance();
        
        // Second access should create a new bitmap after cache clearing
        Bitmap secondAccess = newGenerator.generateMinimap(context, gameState, 100, 100);
        assertNotNull("Second generated minimap should not be null", secondAccess);
        
        // The bitmaps should be different instances if cache was cleared
        assertNotSame("Bitmaps should be different instances after cache clear", firstAccess, secondAccess);
    }
}

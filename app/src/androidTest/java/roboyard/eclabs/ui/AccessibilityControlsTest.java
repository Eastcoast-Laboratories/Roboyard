package roboyard.eclabs.ui;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import roboyard.eclabs.R;
import roboyard.eclabs.ui.GameElement;
import roboyard.eclabs.ui.GameState;
import roboyard.eclabs.ui.GameStateManager;
import roboyard.eclabs.ui.ModernGameFragment;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the accessibility controls for TalkBack support in ModernGameFragment
 */
@RunWith(AndroidJUnit4.class)
public class AccessibilityControlsTest {

    private Context context;
    
    @Mock
    private AccessibilityManager accessibilityManager;
    
    @Mock
    private GameStateManager mockGameStateManager;
    
    @Mock
    private GameState mockGameState;
    
    @Mock
    private GameElement mockRobot;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        context = ApplicationProvider.getApplicationContext();
    }
    
    /**
     * Test that the accessibility controls toggle is visible when TalkBack is enabled
     */
    @Test
    public void testAccessibilityToggleVisibleWithTalkBack() {
        // Mock the AccessibilityManager to return true for isTouchExplorationEnabled
        when(accessibilityManager.isTouchExplorationEnabled()).thenReturn(true);
        
        // Launch the fragment
        FragmentScenario<ModernGameFragment> scenario = FragmentScenario.launchInContainer(
                ModernGameFragment.class, null, R.style.AppTheme, null);
        
        // Inject our mocked AccessibilityManager
        scenario.onFragment(fragment -> {
            try {
                Field accessibilityManagerField = ModernGameFragment.class.getDeclaredField("accessibilityManager");
                accessibilityManagerField.setAccessible(true);
                accessibilityManagerField.set(fragment, accessibilityManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Call onResume to check TalkBack status
            fragment.onResume();
        });
        
        // Verify that the toggle button is visible
        onView(withId(R.id.btn_toggle_accessibility)).check(matches(isDisplayed()));
    }
    
    /**
     * Test that the robot selection info is correctly displayed
     */
    @Test
    public void testRobotSelectionAccessibilityInfo() {
        // Mock the GameElement (robot) with a specific color
        when(mockRobot.getColor()).thenReturn(0); // Red
        when(mockRobot.getX()).thenReturn(3);
        when(mockRobot.getY()).thenReturn(4);
        
        // Launch the fragment
        FragmentScenario<ModernGameFragment> scenario = FragmentScenario.launchInContainer(
                ModernGameFragment.class, null, R.style.AppTheme, null);
        
        // Set up the fragment with mock objects
        scenario.onFragment(fragment -> {
            try {
                // Set up mocked GameStateManager
                Field gameStateManagerField = ModernGameFragment.class.getDeclaredField("gameStateManager");
                gameStateManagerField.setAccessible(true);
                gameStateManagerField.set(fragment, mockGameStateManager);
                
                // Set up accessibility controls visibility
                Field accessibilityControlsVisibleField = ModernGameFragment.class.getDeclaredField("accessibilityControlsVisible");
                accessibilityControlsVisibleField.setAccessible(true);
                accessibilityControlsVisibleField.set(fragment, true);
                
                // Call the method to update robot selection info
                fragment.updateRobotSelectionInfo(mockRobot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Verify that the selected robot info text is correct
        onView(withId(R.id.txt_selected_robot)).check(matches(withText("Selected: Red robot at (3, 4)")));
    }
    
    /**
     * Test that directional button text is correct for a selected robot
     */
    @Test
    public void testDirectionalButtonText() {
        // Mock the GameElement (robot) with a specific color
        when(mockRobot.getColor()).thenReturn(2); // Blue
        
        // Launch the fragment
        FragmentScenario<ModernGameFragment> scenario = FragmentScenario.launchInContainer(
                ModernGameFragment.class, null, R.style.AppTheme, null);
        
        // Set up the fragment with mock objects
        scenario.onFragment(fragment -> {
            try {
                // Set up accessibility controls visibility
                Field accessibilityControlsVisibleField = ModernGameFragment.class.getDeclaredField("accessibilityControlsVisible");
                accessibilityControlsVisibleField.setAccessible(true);
                accessibilityControlsVisibleField.set(fragment, true);
                
                // Call the method to update directional buttons
                fragment.updateDirectionalButtons(mockRobot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Verify that the directional button text is correct
        onView(withId(R.id.btn_move_north)).check(matches(withText("Blue North")));
        onView(withId(R.id.btn_move_south)).check(matches(withText("Blue South")));
        onView(withId(R.id.btn_move_east)).check(matches(withText("Blue East")));
        onView(withId(R.id.btn_move_west)).check(matches(withText("Blue West")));
    }
    
    /**
     * Test the getPositionDescription method in GameGridView
     */
    @Test
    public void testPositionDescription() {
        // Create a GameGridView for testing
        GameGridView gameGridView = new GameGridView(context);
        
        // Mock GameState and GameStateManager
        when(mockGameState.getRobotAt(5, 6)).thenReturn(mockRobot);
        when(mockRobot.getColor()).thenReturn(1); // Green
        when(mockRobot.getX()).thenReturn(5);
        when(mockRobot.getY()).thenReturn(6);
        
        MutableLiveData<GameState> liveData = new MutableLiveData<>();
        liveData.setValue(mockGameState);
        when(mockGameStateManager.getCurrentState()).thenReturn(liveData);
        
        // Set the mocked GameStateManager to the view
        gameGridView.setGameStateManager(mockGameStateManager);
        
        // Use reflection to call the private method
        try {
            Method getPositionDescriptionMethod = GameGridView.class.getDeclaredMethod("getPositionDescription", int.class, int.class);
            getPositionDescriptionMethod.setAccessible(true);
            String description = (String) getPositionDescriptionMethod.invoke(gameGridView, 5, 6);
            
            // Verify the description contains expected information
            assertTrue("Description should mention position", description.contains("Position 5, 6"));
            assertTrue("Description should mention Green robot", description.contains("Green robot"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to test position description: " + e.getMessage());
        }
    }
}

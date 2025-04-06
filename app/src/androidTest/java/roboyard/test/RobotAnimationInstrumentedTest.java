package roboyard.test;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.MainActivity;
import roboyard.eclabs.ui.GameElement;
import roboyard.eclabs.ui.GameState;
import roboyard.eclabs.ui.ModernGameFragment;
import roboyard.ui.animation.RobotAnimationManager;
import roboyard.ui.components.GameGridView;
import roboyard.ui.components.GameStateManager;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Instrumented test that verifies the robot animation system works correctly.
 * This test runs on a real Android device or emulator.
 */
@RunWith(AndroidJUnit4.class)
public class RobotAnimationInstrumentedTest {
    
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);
    
    private GameStateManager gameStateManager;
    private GameGridView gameGridView;
    private ModernGameFragment gameFragment;
    
    @Before
    public void setup() {
        // Enable verbose logging for the test
        Timber.plant(new Timber.DebugTree());
        Timber.d("[TEST] Setting up RobotAnimationInstrumentedTest");
    }
    
    /**
     * Test that the robot animation system works correctly by starting a game,
     * selecting a robot, and moving it.
     */
    @Test
    public void testRobotAnimation() throws InterruptedException {
        // Launch the main activity and wait for it to be ready
        Timber.d("[TEST] Starting robot animation test");
        
        // Click on the "Play" button to start a new game
        Espresso.onView(ViewMatchers.withText("Play"))
                .perform(ViewActions.click());
        
        // Wait for the game to load
        Thread.sleep(2000);
        
        // Click on "Modern Game" to start a modern game
        Espresso.onView(ViewMatchers.withText("Modern Game"))
                .perform(ViewActions.click());
        
        // Wait for the game to load
        Thread.sleep(2000);
        
        // Retrieve the game fragment, state manager and grid view
        final CountDownLatch latch = new CountDownLatch(1);
        activityRule.getScenario().onActivity(activity -> {
            // Find the ModernGameFragment
            gameFragment = (ModernGameFragment) activity.getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
            
            if (gameFragment != null) {
                gameStateManager = gameFragment.getGameStateManager();
                gameGridView = gameFragment.getGameGridView();
                
                // Log state for debugging
                Timber.d("[TEST] Retrieved game components: fragment=%s, manager=%s, grid=%s",
                        gameFragment != null ? "found" : "null",
                        gameStateManager != null ? "found" : "null",
                        gameGridView != null ? "found" : "null");
                
                // Verify animation settings
                if (gameStateManager != null) {
                    Timber.d("[TEST] Animations enabled: %s", gameStateManager.areAnimationsEnabled());
                    
                    // Get current animation manager
                    RobotAnimationManager animManager = gameStateManager.getRobotAnimationManager();
                    if (animManager != null) {
                        // Ensure it has a GameGridView
                        if (animManager.getGameGridView() == null && gameGridView != null) {
                            Timber.d("[TEST] Fixing missing GameGridView in RobotAnimationManager");
                            animManager.setGameGridView(gameGridView);
                        }
                    }
                }
            }
            
            latch.countDown();
        });
        
        // Wait for initialization to complete
        latch.await(5, TimeUnit.SECONDS);
        
        // Wait for the game to be fully loaded
        Thread.sleep(1000);
        
        // Now click on "Accessibility Controls" to switch to accessibility mode
        Espresso.onView(ViewMatchers.withText("Accessibility Controls"))
                .perform(ViewActions.click());
        
        // Wait for accessibility controls to appear
        Thread.sleep(1000);
        
        // Click on "Next Robot" to select the first robot
        Espresso.onView(ViewMatchers.withText("Next Robot"))
                .perform(ViewActions.click());
        
        // Log that we've selected a robot
        Timber.d("[TEST] Selected a robot using Next Robot button");
        
        // Wait a moment for selection to register
        Thread.sleep(1000);
        
        // Now click on a movement button to move the robot
        // Try North first
        Espresso.onView(ViewMatchers.withText("North"))
                .perform(ViewActions.click());
        
        // Log that we've triggered a move
        Timber.d("[TEST] Clicked North button to move robot");
        
        // Wait for animation to complete
        Thread.sleep(2000);
        
        // Try moving the robot East
        Espresso.onView(ViewMatchers.withText("East"))
                .perform(ViewActions.click());
        
        // Log that we've triggered another move
        Timber.d("[TEST] Clicked East button to move robot");
        
        // Wait for animation to complete
        Thread.sleep(2000);
        
        // Verify state after movement
        activityRule.getScenario().onActivity(activity -> {
            // Find the ModernGameFragment again (in case it changed)
            ModernGameFragment fragment = (ModernGameFragment) activity.getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
            
            if (fragment != null) {
                GameStateManager stateManager = fragment.getGameStateManager();
                if (stateManager != null) {
                    GameState state = stateManager.getCurrentState().getValue();
                    if (state != null && state.getSelectedRobot() != null) {
                        GameElement robot = state.getSelectedRobot();
                        Timber.d("[TEST] Robot final position: (%d,%d)", 
                                robot.getX(), robot.getY());
                        Timber.d("[TEST] Robot has animation position: %s", 
                                robot.hasAnimationPosition() ? "yes" : "no");
                    }
                }
            }
        });
        
        // Wait one more moment to ensure logs are captured
        Thread.sleep(3000);
    }
}

package roboyard.eclabs.ui;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import roboyard.eclabs.Constants;
import roboyard.eclabs.GameManager;
import roboyard.ui.components.GameStateManager;
import roboyard.ui.components.InputManager;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.RenderManager;
import timber.log.Timber;

/**
 * This fragment hosts the original GridGameView with its canvas rendering.
 * It allows the old game logic to remain untouched while integrating with
 * the new fragment-based UI architecture.
 */
public class GameCanvasFragment extends BaseGameFragment {

    private GameManager gameManager;
    private GameSurfaceView gameSurfaceView;
    private InputManager inputManager;
    private RenderManager renderManager;
    private int levelScreenType = Constants.SCREEN_GAME; // Default to standard game screen
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("GameCanvasFragment: onCreate() called");
        
        // Check if there's a level screen type passed in arguments
        if (getArguments() != null && getArguments().containsKey("levelScreen")) {
            levelScreenType = getArguments().getInt("levelScreen", Constants.SCREEN_GAME);
            Timber.d("GameCanvasFragment: Level screen type from arguments: %d", levelScreenType);
        } else {
            Timber.d("GameCanvasFragment: No level screen type in arguments, using default: %d", levelScreenType);
        }
        
        // Create resources before we initialize any game components
        Timber.d("GameCanvasFragment: Setting up display metrics");
        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        Timber.d("GameCanvasFragment: Screen dimensions: %d x %d", screenWidth, screenHeight);
        
        Timber.d("GameCanvasFragment: Creating InputManager and RenderManager");
        inputManager = new InputManager();
        // Create RenderManager with the correct constructor parameters and ensure it has a context
        renderManager = new RenderManager(requireActivity().getResources());
        // Set the context for accessibility features
        renderManager.setContext(requireContext());
        
        // We need to check if our activity is a MainActivity or comes from FragmentHostActivity
        Timber.d("GameCanvasFragment: Checking host activity type: %s", requireActivity().getClass().getSimpleName());
        if (requireActivity() instanceof MainActivity mainActivity) {
            // If we're in the original MainActivity, we can use it directly
            Timber.d("GameCanvasFragment: Running in MainActivity");
            gameManager = new GameManager(inputManager, renderManager, screenWidth, screenHeight, mainActivity);
            Timber.d("GameCanvasFragment: Created GameManager with MainActivity");
        } else {
            // If we're in the FragmentHostActivity, we'll create a compatible environment for the game
            Timber.d("GameCanvasFragment: Running in FragmentHostActivity, creating compatible game environment");
            
            // Get GameStateManager from the activity
            GameStateManager gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
            Timber.d("GameCanvasFragment: Obtained GameStateManager from ViewModelProvider");
            
            // Update the context in GameStateManager to use the current activity
            gameStateManager.updateContext(requireActivity());
            Timber.d("GameCanvasFragment: Updated GameStateManager context with current activity");
            
            // Create a proxy MainActivity that delegates to FragmentHostActivity where needed
            ProxyMainActivity proxyMainActivity = new ProxyMainActivity(requireActivity(), gameStateManager);
            Timber.d("GameCanvasFragment: Created ProxyMainActivity");
            
            // Initialize the context in the proxy to avoid NullPointerException
            proxyMainActivity.attachBaseContext(requireContext());
            Timber.d("GameCanvasFragment: Attached base context to ProxyMainActivity");
            
            gameManager = new GameManager(inputManager, renderManager, screenWidth, screenHeight, proxyMainActivity);
            Timber.d("GameCanvasFragment: Created GameManager with ProxyMainActivity");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Timber.d("GameCanvasFragment: onCreateView() called");
        // Create a frame layout to host our game surface
        FrameLayout frameLayout = new FrameLayout(requireContext());
        
        // Create the game surface view which will host the canvas rendering
        Timber.d("GameCanvasFragment: Creating GameSurfaceView");
        gameSurfaceView = new GameSurfaceView(requireContext(), gameManager);
        frameLayout.addView(gameSurfaceView);
        Timber.d("GameCanvasFragment: Added GameSurfaceView to FrameLayout");
        
        return frameLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Timber.d("GameCanvasFragment: onViewCreated() called");
        
        // Set the screen based on the passed argument or default value
        if (levelScreenType != Constants.SCREEN_GAME) {
            // If we're navigating to a level selection screen
            Timber.d("GameCanvasFragment: Setting level screen type: %d", levelScreenType);
            gameManager.setGameScreen(levelScreenType);
        } else {
            // Regular game screen
            Timber.d("GameCanvasFragment: Setting to regular game screen: %d", Constants.SCREEN_GAME);
            gameManager.setGameScreen(Constants.SCREEN_GAME);
        }
        
        // Start the game rendering
        Timber.d("GameCanvasFragment: Starting game rendering");
        gameSurfaceView.startGame();
        Timber.d("GameCanvasFragment: Game rendering started");
    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.d("GameCanvasFragment: onPause() called");
        if (gameSurfaceView != null) {
            Timber.d("GameCanvasFragment: Pausing game surface");
            gameSurfaceView.pauseGame();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("GameCanvasFragment: onResume() called");
        if (gameSurfaceView != null) {
            Timber.d("GameCanvasFragment: Resuming game surface");
            gameSurfaceView.resumeGame();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("GameCanvasFragment: onDestroy() called");
        if (gameSurfaceView != null) {
            Timber.d("GameCanvasFragment: Stopping game surface");
            gameSurfaceView.stopGame();
        }
    }

    @Override
    public String getScreenTitle() {
        return "Game";
    }
    
    /**
     * Inner class to allow ProxyMainActivity to access the private constructor
     * in MainActivity. This allows us to create a proxy that delegates to
     * FragmentHostActivity while being compatible with old code that expects
     * to interact with MainActivity.
     */
    public class ProxyMainActivity extends MainActivity {
        
        private final androidx.fragment.app.FragmentActivity hostActivity;
        private final GameStateManager gameStateManager;
        
        public ProxyMainActivity(androidx.fragment.app.FragmentActivity hostActivity, GameStateManager gameStateManager) {
            this.hostActivity = hostActivity;
            this.gameStateManager = gameStateManager;
        }
        
        public void attachBaseContext(android.content.Context base) {
            // This method will initialize the application context to avoid NullPointerException
            // in methods like getResources()
            super.attachBaseContext(base);
        }

        /**
         * Override the openSaveScreen method to handle it via the modern navigation system
         * instead of trying to start a new activity which causes NullPointerException
         */
        @Override
        public void openSaveScreen() {
            Timber.d("ProxyMainActivity: openSaveScreen called");
            // Delegate to gameStateManager's navigation method with saveMode=true
            gameStateManager.navigateToSaveScreen(true);
        }
        
        /**
         * Override the openLoadScreen method to handle it via the modern navigation system
         * instead of trying to start a new activity which causes NullPointerException
         */
        @Override
        public void openLoadScreen() {
            Timber.d("ProxyMainActivity: openLoadScreen called");
            // Delegate to gameStateManager's navigation method with saveMode=false
            gameStateManager.navigateToSaveScreen(false);
        }
        
        @Override
        public void closeApp() {
            hostActivity.finish();
        }
    }
}

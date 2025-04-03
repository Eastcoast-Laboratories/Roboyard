package roboyard.ui.activities;
import roboyard.SoundService;
import roboyard.logic.core.MapGenerator;
import roboyard.ui.components.RenderManager;
import roboyard.eclabs.Move;
import roboyard.ui.components.InputManager;
import roboyard.eclabs.IGameObject;
import roboyard.ui.components.GamePiece;
import roboyard.ui.components.GameMovementInterface;
import roboyard.eclabs.GameManager;
import roboyard.eclabs.ui.FragmentHostActivity;
import roboyard.eclabs.util.BoardSizeManager;
import android.graphics.Canvas;
import android.app.Activity;
import android.content.Context;
import roboyard.eclabs.R;
import roboyard.ui.components.GamePiece;
import roboyard.ui.components.GameMovementInterface;
import roboyard.logic.core.Preferences;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import roboyard.SoundService;
import roboyard.eclabs.ui.FragmentHostActivity;
import roboyard.ui.components.InputManager;
import timber.log.Timber;

public class MainActivity extends FragmentActivity
        implements TextureView.SurfaceTextureListener {
    private static final boolean DEBUG = true;  // Set this to false for release builds
    private TextureView mTextureView;
    private MainActivity.RenderingThread mThread;
    private int sWidth, sHeight;
    private InputManager inputManager;
    private RenderManager renderManager;
    private GameManager gameManager;

    // Static reference to the application context
    private static Context appContext;
    
    /**
     * Returns the application context.
     * This is used for initializing singletons like AppPreferences when needed.
     * 
     * @return The application context
     */
    public static Context getAppContext() {
        return appContext;
    }

    // Default board sizes
    public static final int DEFAULT_BOARD_SIZE_X = 12;
    public static final int DEFAULT_BOARD_SIZE_Y = 14;
    
    // Minimum and maximum board sizes
    public static final int MIN_BOARD_SIZE = 8; // solver doesn't work below this
    public static final int MAX_BOARD_SIZE = 22; // solver not tested above this
    
    // Current board size - can be changed at runtime
    public static int boardSizeX = DEFAULT_BOARD_SIZE_X;
    public static int boardSizeY = DEFAULT_BOARD_SIZE_Y;
    public static int numRobots = 4;

    private static final int HIGH_FPS_SLEEP = 15;  // ~15 FPS
    private static final int LOW_FPS_SLEEP = 45;  // ~5 FPS / value 100 tested, but thats far too slow reaction on gmi touch
    private boolean touchActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = getApplicationContext();
        
        // Initialize static Preferences
        Preferences.initialize(getApplicationContext());
        
        // Hide the status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_main);
        
        initScreenSize();
        initGameSettings();
        
        // Setup navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // Additional navigation setup if needed
        }
        
        FrameLayout content = new FrameLayout(this);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOpaque(false);

        content.addView(mTextureView, new FrameLayout.LayoutParams(sWidth, sHeight));
        setContentView(content);

        startSound();
        
        // Check if we need to show the load screen after restart
        SharedPreferences prefs = getSharedPreferences("RoboYard", Context.MODE_PRIVATE);
        boolean showLoadScreen = prefs.getBoolean("show_load_screen_on_restart", false);
        final int lastSavedSlot = prefs.getInt("last_saved_slot", -1);
        
        if (showLoadScreen && lastSavedSlot >= 0) {
            // Clear the flag immediately to prevent loops
            prefs.edit().putBoolean("show_load_screen_on_restart", false).apply();
            Timber.d("[MINIMAP] Detected restart flag, navigating to load screen for slot %d", lastSavedSlot);
            
        }
    }
    
    /**
     * Initialize screen size information
     */
    private void initScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        // keep screen on:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Point size = new Point();
        display.getSize(size);
        sWidth = size.x;
        sHeight = size.y;
    }
    
    /**
     * Initialize game settings from preferences
     */
    private void initGameSettings() {
        // Load board size from preferences or use default if not set
        loadBoardSizeFromPreferences(this);
        Timber.d("Initialized with board size: %dx%d", boardSizeX, boardSizeY);
        
        // Load map generation preference
        String newMapSetting = Preferences.getPreferenceValue(this, "newMapEachTime");
        if (newMapSetting == null) {
            newMapSetting = "true"; // Default value
            Preferences.setPreferences(this, "newMapEachTime", newMapSetting);
        }
        MapGenerator.generateNewMapEachTime = newMapSetting.equals("true");
        Timber.d("Initialized generateNewMapEachTime: %s", MapGenerator.generateNewMapEachTime);
        
        // Clear all minimap caches on app start
        Timber.d("Clearing all minimap caches on app start");

        this.inputManager = new InputManager();
        this.renderManager = new RenderManager(getResources());
        this.renderManager.setContext(this); // Set context for accessibility features
        this.gameManager = new GameManager(this.inputManager, this.renderManager, this.sWidth, this.sHeight, this);
    }

    @Override
    public void onBackPressed() {
        this.inputManager.startBack();
    }

    public void closeApp(){
        this.gameManager.destroy();
        this.finish();
        System.exit(0);
    }

    /**
     * relaunches the task, but it does not restart the process, or even the Application object.
     * Therefore any static data, data initialized during creation of the Application, or jni
     * classes remain in their current state and are not reinitialized
     */
    public void restartApp(){
        Context context=getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Timber.d("The app restarts by trigger");
        Runtime.getRuntime().exit(0);
    }

    public void doToast(final CharSequence str, final boolean big){

        this.runOnUiThread(() -> {
            Toast t = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
            if(big) {
               /*
                //* disabled
                // TODO: search for a solution that works with API 30
                LinearLayout l = (LinearLayout) t.getView();
                TextView mtv = (TextView) l.getChildAt(0);
                mtv.setTextSize(18);
                */
               t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            }
            t.show();
        });
        //*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        touchActive = (e.getAction() != MotionEvent.ACTION_UP);
        synchronized (this.inputManager) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    this.inputManager.startDown(e.getX(), e.getY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    this.inputManager.startMove(e.getX(), e.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    this.inputManager.startUp(e.getX(), e.getY());
                    break;
            }
        }
        return true;
    }

    @Override
    public void onResume(){
        startSound();
        super.onResume();
    }

    @Override
    public void onPause(){
        stopSound();
        super.onPause();
        if(mThread != null){
            mThread.interrupt();
            mThread = null;
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mThread != null){
            mThread.interrupt();
            mThread = null;
        }
    }

    @Override
    public void onDestroy(){
        stopSound();
        super.onDestroy();
        if(mThread != null){
            mThread.interrupt();
            mThread = null;
        }
    }

    public void startSound(){
        String soundSetting = Preferences.getPreferenceValue(this, "sound");
        if(!soundSetting.equals("off")) {
            //start service and play music
            startService(new Intent(MainActivity.this, SoundService.class));
        }
    }

    public void stopSound(){
        //stop service and stop music
        stopService(new Intent(MainActivity.this, SoundService.class));
    }

    public void toggleSound(boolean enabled) {
        if (enabled) {
            startSound();
        } else {
            stopSound();
        }
    }

    public void draw(Canvas pCanvas) {
        synchronized (this.renderManager) {
            this.renderManager.setMainTarget(pCanvas);
        }

        synchronized (this.gameManager) {
            this.gameManager.draw();
        }
    }

    public void tick(Canvas pCanvas) {
        try {
            this.draw(pCanvas); //draw all items
            this.gameManager.update(); //update all items
            synchronized(this.inputManager) {
                this.inputManager.resetEvents(); //reset events
            }
        }catch(Exception e){
            //error
            e.getMessage();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mThread = new RenderingThread(mTextureView);
        mThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mThread != null) mThread.stopRendering();
        this.gameManager.destroy();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Ignored
    }

    /**
     * Opens the modern UI settings screen
     * This provides a bridge between the old game logic and the new UI
     */
    public void openSettingsScreen() {
        Intent intent = new Intent(this, FragmentHostActivity.class);
        intent.putExtra("screen", "settings");
        startActivity(intent);
    }
    
    /**
     * Opens the modern UI save game screen
     * This provides a bridge between the old game logic and the new UI
     */
    public void openSaveScreen() {
        Timber.d("[Save button] MainActivity.openSaveScreen called, launching intent");
        
        // Temporarily pause the rendering thread while switching activities
        // Without this, the game canvas might stay in the foreground
        boolean wasRendering = false;
        if (mThread != null) {
            Timber.d("[Save button] Pausing rendering thread");
            wasRendering = true;
            mThread.stopRendering(); // This calls interrupt() and sets mRunning to false
        }
        
        Intent intent = new Intent(this, FragmentHostActivity.class);
        intent.putExtra("screen", "save");
        intent.putExtra("saveMode", true);
        
        // Add flags to bring this activity to front and clear it from stack
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        try {
            Timber.d("[Save button] Starting FragmentHostActivity with flags");
            startActivity(intent);
            
            // Use overridePendingTransition to control the animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            // Move MainActivity to the background to ensure FragmentHostActivity comes to front
            // This is an important step that ensures the rendering canvas doesn't stay on top
            moveTaskToBack(true);
            
            Timber.d("[Save button] MainActivity moved to background");
        } catch (Exception e) {
            Timber.e("[Save button] Error starting FragmentHostActivity: %s", e.getMessage());
            e.printStackTrace();
            
            // Restart the rendering thread if we failed to launch the activity
            if (wasRendering && mTextureView != null && mTextureView.isAvailable()) {
                Timber.d("[Save button] Restarting rendering thread after error");
                mThread = new RenderingThread(mTextureView);
                mThread.start();
            }
        }
    }
    
    /**
     * Opens the modern UI load game screen
     * This provides a bridge between the old game logic and the new UI
     */
    public void openLoadScreen() {
        Timber.d("[Load button] MainActivity.openLoadScreen called, launching intent");
        
        // Temporarily pause the rendering thread while switching activities
        // Without this, the game canvas might stay in the foreground
        boolean wasRendering = false;
        if (mThread != null) {
            Timber.d("[Load button] Pausing rendering thread");
            wasRendering = true;
            mThread.stopRendering(); // This calls interrupt() and sets mRunning to false
        }
        
        Intent intent = new Intent(this, FragmentHostActivity.class);
        intent.putExtra("screen", "save");
        intent.putExtra("saveMode", false);
        
        // Add flags to bring this activity to front and clear it from stack
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        try {
            Timber.d("[Load button] Starting FragmentHostActivity with flags");
            startActivity(intent);
            
            // Use overridePendingTransition to control the animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            // Move MainActivity to the background to ensure FragmentHostActivity comes to front
            // This is an important step that ensures the rendering canvas doesn't stay on top
            moveTaskToBack(true);
            
            Timber.d("[Load button] MainActivity moved to background");
        } catch (Exception e) {
            Timber.e("[Load button] Error starting FragmentHostActivity: %s", e.getMessage());
            e.printStackTrace();
            
            // Restart the rendering thread if we failed to launch the activity
            if (wasRendering && mTextureView != null && mTextureView.isAvailable()) {
                Timber.d("[Load button] Restarting rendering thread after error");
                mThread = new RenderingThread(mTextureView);
                mThread.start();
            }
        }
    }
    
    /**
     * Opens the modern UI help/credits screen
     * This provides a bridge between the old game logic and the new UI
     */
    public void openHelpScreen() {
        Intent intent = new Intent(this, FragmentHostActivity.class);
        intent.putExtra("screen", "help");
        startActivity(intent);
    }

    private boolean needsHighFramerate() {
        if (gameManager == null || gameManager.getCurrentScreen() == null) {
            return false;
        }

        // Always high framerate when touch is active
        if (touchActive) {
            return true;
        }

        // Check if any robot is moving
        for (IGameObject obj : gameManager.getCurrentScreen().getGameObjects()) {
            if (obj instanceof GamePiece piece) {
                if (piece.isInMovement()) {
                    return true;
                }
            }
            // Check if GMI is active
            if (obj instanceof GameMovementInterface gmi) {
                if (gmi.isActive()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the game manager instance
     * @return The current GameManager instance
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /** Thread for rendering the game */
    private class RenderingThread extends Thread {

        private final TextureView mSurface;
        private volatile boolean mRunning = true;

        public RenderingThread(TextureView surface) {
            mSurface = surface;
        }

        @Override
        public void run() {
            while (mRunning && !Thread.interrupted()) {
                final Canvas canvas = mSurface.lockCanvas(null);
                try {
                    if(canvas != null){ tick(canvas); }
                } finally {
                    mSurface.unlockCanvasAndPost(canvas);
                }

                try {
                    // Adjust sleep time based on game state
                    int sleepTime = needsHighFramerate() ? HIGH_FPS_SLEEP : LOW_FPS_SLEEP;
                    Thread.sleep(sleepTime);
                    // Timber.d("tick " + sleepTime + (needsHighFramerate() ? "high" : "low") + " timestamp: " + System.currentTimeMillis());
                } catch (InterruptedException e) {
                    // Interrupted
                }
            }
        }


        void stopRendering() {
            interrupt();
            mRunning = false;
        }
    }

    public void setAndSaveBoardSizeToPreferences(Context context, int x, int y) {
        // Validate board size
        x = Math.max(MIN_BOARD_SIZE, Math.min(x, MAX_BOARD_SIZE));
        y = Math.max(MIN_BOARD_SIZE, Math.min(y, MAX_BOARD_SIZE));
        
        Timber.d("Setting board size to: %dx%d", x, y);
        
        boardSizeX = x;
        boardSizeY = y;
        
        // Save board size to preferences using the existing preferences instance
        Preferences.setPreferences(this, "boardSizeX", String.valueOf(x));
        Preferences.setPreferences(this, "boardSizeY", String.valueOf(y));
        
        // Also update BoardSizeManager to ensure consistency
        try {
            roboyard.eclabs.util.BoardSizeManager boardSizeManager = roboyard.eclabs.util.BoardSizeManager.getInstance(context);
            boardSizeManager.setBoardSize(x, y);
            Timber.d("[BOARD_SIZE_DEBUG] Updated BoardSizeManager with size: %dx%d", x, y);
        } catch (Exception e) {
            Timber.e(e, "[BOARD_SIZE_DEBUG] Error updating BoardSizeManager");
        }
        
        Timber.d("[BOARD_SIZE_DEBUG] Board size saved to preferences: %dx%d", x, y);
    }

    /**
     * Gets the current board width
     * @return Current board width
     */
    public static int getBoardWidth() {
        return boardSizeX;
    }

    /**
     * Gets the current board height
     * @return Current board height
     */
    public static int getBoardHeight() {
        return boardSizeY;
    }

    /**
     * Load board size from preferences or use default if not set
     */
    public void loadBoardSizeFromPreferences(Activity activity) {
        String boardSizeXStr = Preferences.getPreferenceValue(activity, "boardSizeX");
        String boardSizeYStr = Preferences.getPreferenceValue(activity, "boardSizeY");

        Timber.d("Loading board size from preferences: X=%s, Y=%s", boardSizeXStr, boardSizeYStr);

        if (boardSizeXStr != null && !boardSizeXStr.isEmpty()) {
            boardSizeX = Integer.parseInt(boardSizeXStr);
        } else {
            Timber.d("[BOARD_SIZE_DEBUG] MainActivity.loadBoardSizeFromPreferences - Using default board size: %dx%d", DEFAULT_BOARD_SIZE_X, DEFAULT_BOARD_SIZE_Y);
            boardSizeX = DEFAULT_BOARD_SIZE_X;
        }

        if (boardSizeYStr != null && !boardSizeYStr.isEmpty()) {
            boardSizeY = Integer.parseInt(boardSizeYStr);
        } else {
            boardSizeY = DEFAULT_BOARD_SIZE_Y;
        }
        
        // Also sync with BoardSizeManager to ensure consistency
        try {
            roboyard.eclabs.util.BoardSizeManager boardSizeManager = roboyard.eclabs.util.BoardSizeManager.getInstance(activity);
            
            // Only set if necessary (to avoid circular updates)
            if (boardSizeManager.getBoardWidth() != boardSizeX || boardSizeManager.getBoardHeight() != boardSizeY) {
                Timber.d("[BOARD_SIZE_DEBUG] MainActivity.loadBoardSizeFromPreferences - Syncing with BoardSizeManager: %dx%d", boardSizeX, boardSizeY);
                boardSizeManager.setBoardSize(boardSizeX, boardSizeY);
            } else {
                Timber.d("[BOARD_SIZE_DEBUG] MainActivity.loadBoardSizeFromPreferences - BoardSizeManager already in sync: %dx%d", boardSizeX, boardSizeY);
            }
        } catch (Exception e) {
            Timber.e(e, "[BOARD_SIZE_DEBUG] Error syncing with BoardSizeManager");
        }
        
        Timber.d("Board size set to: %dx%d", boardSizeX, boardSizeY);
    }
}
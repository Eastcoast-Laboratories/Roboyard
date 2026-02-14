package roboyard.ui.activities;
import roboyard.eclabs.R;
import roboyard.logic.core.Constants;
import roboyard.logic.core.MapGenerator;
import roboyard.ui.components.RenderManager;
import roboyard.ui.components.InputManager;
import roboyard.logic.core.Preferences;

import android.graphics.Canvas;
import android.content.Context;
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

import timber.log.Timber;

public class MainActivity extends FragmentActivity
        implements TextureView.SurfaceTextureListener {
    private TextureView mTextureView;
    private MainActivity.RenderingThread mThread;
    private int sWidth, sHeight;
    private InputManager inputManager;
    private RenderManager renderManager;

    // Static reference to the application context
    private static Context appContext;

    // Default board sizes
    public static final int DEFAULT_BOARD_SIZE_X = 12;
    public static final int DEFAULT_BOARD_SIZE_Y = 14;
    
    // Minimum and maximum board sizes
    public static final int MIN_BOARD_SIZE = 8; // solver doesn't work below this
    public static final int MAX_BOARD_SIZE = 22; // solver not tested above this
    
    // Current board size - can be changed at runtime
    public static int boardSizeX = DEFAULT_BOARD_SIZE_X;
    public static int boardSizeY = DEFAULT_BOARD_SIZE_Y;
    public static int numRobots = Constants.NUM_ROBOTS;

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
        
        // Setup navigation with proper error handling
        try {
            androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (fragment instanceof NavHostFragment) {
                NavHostFragment navHostFragment = (NavHostFragment) fragment;
                NavController navController = navHostFragment.getNavController();
                // Additional navigation setup if needed
                Timber.d("[NAV] Navigation controller initialized successfully in MainActivity");
            } else {
                Timber.w("[NAV] Fragment with id nav_host_fragment is not a NavHostFragment in MainActivity: %s", 
                        fragment != null ? fragment.getClass().getSimpleName() : "null");
            }
        } catch (ClassCastException e) {
            Timber.e(e, "[NAV] ClassCastException when setting up navigation controller in MainActivity");
        } catch (Exception e) {
            Timber.e(e, "[NAV] Unexpected error when setting up navigation controller in MainActivity");
        }
        
        FrameLayout content = new FrameLayout(this);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOpaque(false);

        content.addView(mTextureView, new FrameLayout.LayoutParams(sWidth, sHeight));
        setContentView(content);
    }
    
    /**
     * Initialize screen size information
     */
    private void initScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        // keep screen on:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        sWidth = size.x;
        sHeight = size.y;
    }
    
    /**
     * Initialize game settings from preferences
     */
    private void initGameSettings() {
        // Use board size directly from Preferences class
        boardSizeX = roboyard.logic.core.Preferences.boardSizeWidth;
        boardSizeY = roboyard.logic.core.Preferences.boardSizeHeight;
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
    }

    @Override
    public void onBackPressed() {
        this.inputManager.startBack();
    }

    public void doToast(final CharSequence str, final boolean big){
        this.runOnUiThread(() -> {
            Toast t = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
            if(big) {
               t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            }
            t.show();
        });
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
        super.onResume();
    }

    @Override
    public void onPause(){
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
        super.onDestroy();
        if(mThread != null){
            mThread.interrupt();
            mThread = null;
        }
    }

    public void draw(Canvas pCanvas) {
        synchronized (this.renderManager) {
            this.renderManager.setMainTarget(pCanvas);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surface, int width, int height) {
        mThread = new RenderingThread(mTextureView);
        mThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surface, int width, int height) {
        // Ignored
    }

    @Override
    public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surface) {
        if (mThread != null) mThread.stopRendering();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surface) {
        // Ignored
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
            // Rendering logic here if needed
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
}
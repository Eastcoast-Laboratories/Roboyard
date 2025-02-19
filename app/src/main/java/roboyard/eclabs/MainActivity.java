package roboyard.eclabs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

import roboyard.SoundService;

public class MainActivity extends Activity
        implements TextureView.SurfaceTextureListener {
    private TextureView mTextureView;
    private MainActivity.RenderingThread mThread;
    private int sWidth, sHeight;
    private InputManager inputManager;
    private RenderManager renderManager;
    private GameManager gameManager;
    private final Preferences preferences = new Preferences();

    // Default board sizes
    public static final int DEFAULT_BOARD_SIZE_X = 14;
    public static final int DEFAULT_BOARD_SIZE_Y = 16;
    
    // Minimum and maximum board sizes
    public static final int MIN_BOARD_SIZE = 12; // solver doesn't work below this
    public static final int MAX_BOARD_SIZE = 16; // solver doesn't work above this
    
    // Current board size - can be changed at runtime
    public static int boardSizeX = DEFAULT_BOARD_SIZE_X;
    public static int boardSizeY = DEFAULT_BOARD_SIZE_Y;
    public static int numRobots = 4;

    public void init() {
        Display display = getWindowManager().getDefaultDisplay();
        // keep screen on:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Point size = new Point();
        display.getSize(size);
        sWidth = size.x;
        sHeight = size.y;
        this.inputManager = new InputManager();
        this.renderManager = new RenderManager(getResources());
        
        // Reset board size to default
        resetBoardSizeToDefault(this);
        
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
        System.out.println("The app restarts by trigger");
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
        String soundSetting = preferences.getPreferenceValue(this, "sound");
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.init();

        FrameLayout content = new FrameLayout(this);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOpaque(false);

        content.addView(mTextureView, new FrameLayout.LayoutParams(sWidth, sHeight));
        setContentView(content);

        startSound();
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
                    Thread.sleep(15);
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

    public void setBoardSize(Context context, int x, int y) {
        // Validate board size
        x = Math.max(MIN_BOARD_SIZE, Math.min(x, MAX_BOARD_SIZE));
        y = Math.max(MIN_BOARD_SIZE, Math.min(y, MAX_BOARD_SIZE));
        
        boardSizeX = x;
        boardSizeY = y;
        // Save board size to preferences
        Preferences prefs = new Preferences();
        prefs.setPreferences(this, "boardSizeX", String.valueOf(x));
        prefs.setPreferences(this, "boardSizeY", String.valueOf(y));
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
     * Reset board size to default values and clear preferences
     */
    public void resetBoardSizeToDefault(Activity activity) {
        boardSizeX = DEFAULT_BOARD_SIZE_X;
        boardSizeY = DEFAULT_BOARD_SIZE_Y;
        preferences.setPreferences(activity, "boardSizeX", String.valueOf(boardSizeX));
        preferences.setPreferences(activity, "boardSizeY", String.valueOf(boardSizeY));
    }
}
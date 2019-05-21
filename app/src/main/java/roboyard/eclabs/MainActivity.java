package roboyard.eclabs;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
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

    // used in GridGameScreen, MapGenerator and both solvers:
    public static int boardSizeX=16; // TODO: crashes on size <12, solver doesn't work on size larger >16
    public static int boardSizeY=16; // TODO: BFS Solver crashes on different sizes of x an y

    public void init() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        sWidth = size.x;
        sHeight = size.y;
        this.inputManager = new InputManager();
        this.renderManager = new RenderManager(getResources());
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

    public void doToast(final CharSequence str, final boolean big){
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast t = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
                if(big) {
                    LinearLayout l = (LinearLayout) t.getView();
                    TextView mtv = (TextView) l.getChildAt(0);
                    mtv.setTextSize(18);
                    t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                }
                t.show();
            }
        });
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
        //start service and play music
        startService(new Intent(MainActivity.this, SoundService.class));
    }
    public void stopSound(){
        //stop service and stop music
        stopService(new Intent(MainActivity.this, SoundService.class));
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
}
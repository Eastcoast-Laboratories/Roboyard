package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import roboyard.eclabs.GameManager;
import timber.log.Timber;

/**
 * Custom SurfaceView that renders the game using the old canvas-based rendering.
 * This creates a bridge between the new fragment-based UI architecture
 * and the original game rendering logic.
 */
public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final GameManager gameManager;
    private GameRenderThread renderThread;

    public GameSurfaceView(Context context, GameManager gameManager) {
        super(context);
        this.gameManager = gameManager;
        getHolder().addCallback(this);
        setFocusable(true); // Make sure we get key events
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Timber.d("GameSurfaceView: surfaceCreated() called");
        renderThread = new GameRenderThread(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Timber.d("GameSurfaceView: surfaceChanged() called with dimensions %dx%d", width, height);
        // The GameManager doesn't have a method to update dimensions after creation
        // So we'll just log them instead
        Timber.d("Surface changed to %dx%d", width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Timber.d("GameSurfaceView: surfaceDestroyed() called");
        stopRenderThread();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Timber.d("GameSurfaceView: onTouchEvent() called with event %s", event);
        // Forward touch events to the game manager's input manager using the correct method
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            gameManager.getInputManager().startDown((int) event.getX(), (int) event.getY());
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            gameManager.getInputManager().startMove((int) event.getX(), (int) event.getY());
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            gameManager.getInputManager().startUp((int) event.getX(), (int) event.getY());
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Start the game rendering
     */
    public void startGame() {
        Timber.d("GameSurfaceView: startGame() called");
        if (renderThread == null) {
            // If no render thread exists, create a new one
            Timber.d("GameSurfaceView: Creating new render thread");
            renderThread = new GameRenderThread(getHolder());
            renderThread.setRunning(true);
            renderThread.start();
            Timber.d("GameSurfaceView: New render thread started");
        } else if (!renderThread.isAlive()) {
            // If thread exists but isn't alive (has terminated), create a new one
            Timber.d("GameSurfaceView: Previous render thread is dead, creating new one");
            renderThread = new GameRenderThread(getHolder());
            renderThread.setRunning(true);
            renderThread.start();
            Timber.d("GameSurfaceView: New render thread started");
        } else {
            // Thread exists and is alive, just make sure it's running
            Timber.d("GameSurfaceView: Render thread already exists and is alive, setting running=true");
            renderThread.setRunning(true);
        }
    }

    /**
     * Pause the game rendering
     */
    public void pauseGame() {
        Timber.d("GameSurfaceView: pauseGame() called");
        if (renderThread != null) {
            renderThread.setRunning(false);
            try {
                renderThread.join(500);
                Timber.d("GameSurfaceView: Render thread stopped on pause");
            } catch (InterruptedException e) {
                Timber.e(e, "GameSurfaceView: Error stopping render thread");
            }
        }
    }

    /**
     * Resume the game rendering
     */
    public void resumeGame() {
        Timber.d("GameSurfaceView: resumeGame() called");
        startGame();
    }

    /**
     * Stop the game rendering
     */
    public void stopGame() {
        Timber.d("GameSurfaceView: stopGame() called");
        stopRenderThread();
    }

    private void stopRenderThread() {
        Timber.d("GameSurfaceView: stopRenderThread() called");
        if (renderThread != null) {
            renderThread.setRunning(false);
            try {
                renderThread.join(1000);
                renderThread = null;
                Timber.d("GameSurfaceView: Render thread fully stopped");
            } catch (InterruptedException e) {
                Timber.e(e, "GameSurfaceView: Error stopping render thread");
            }
        }
    }

    /**
     * Thread responsible for rendering the game
     */
    private class GameRenderThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private boolean running = false;

        public GameRenderThread(SurfaceHolder holder) {
            // Timber.d("GameSurfaceView: GameRenderThread created");
            this.surfaceHolder = holder;
        }

        public void setRunning(boolean running) {
            // Timber.d("GameSurfaceView: setRunning() called with value %b", running);
            this.running = running;
        }

        @Override
        public void run() {
            // Timber.d("GameSurfaceView: Render thread started");
            Canvas canvas;
            while (running) {
                // Timber.d("GameSurfaceView: Render thread loop iteration");
                canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        synchronized (surfaceHolder) {
                            // Timber.d("GameSurfaceView: Locking surface holder for drawing");
                            // Update game state
                            gameManager.update();
                            
                            // Reset input events after processing them
                            gameManager.getInputManager().resetEvents();
                            
                            // Set the canvas as the render target and draw
                            gameManager.getRenderManager().setMainTarget(canvas);
                            gameManager.draw();
                            // Timber.d("GameSurfaceView: Drawing frame completed");
                        }
                    }
                } finally {
                    if (canvas != null) {
                        // Timber.d("GameSurfaceView: Unlocking canvas and posting");
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

                // Control frame rate
                try {
                    // Timber.d("GameSurfaceView: Sleeping for 16ms to control frame rate");
                    Thread.sleep(16); // ~60 FPS
                } catch (InterruptedException e) {
                    Timber.e(e, "GameSurfaceView: Error in render thread sleep");
                }
            }
            // Timber.d("GameSurfaceView: Render thread exiting");
        }
    }
}

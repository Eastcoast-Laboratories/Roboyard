package roboyard.eclabs.util;

import android.os.Handler;
import android.os.Looper;

import timber.log.Timber;

/**
 * A utility class that provides a spinning animation using Braille characters.
 * This is especially useful for accessibility and provides visual feedback
 * for processes that take time, such as calculating solutions.
 */
public class BrailleSpinner {
    // Braille dots pattern for a spinner (rotate clockwise)
    private static final String[] BRAILLE_FRAMES = {
            "⠋", // Frame 1
            "⠙", // Frame 2
            "⠹", // Frame 3
            "⠸", // Frame 4
            "⠼", // Frame 5
            "⠴", // Frame 6
            "⠦", // Frame 7
            "⠧", // Frame 8
            "⠇", // Frame 9
            "⠏"  // Frame 10
    };
    
    private Handler handler;
    private int currentFrame = 0;
    private boolean isSpinning = false;
    private int updateIntervalMs = 100;
    private SpinnerListener spinnerListener;
    private String currentText = "";
    private Runnable runnable;
    
    /**
     * Interface for receiving spinner updates
     */
    public interface SpinnerListener {
        /**
         * Called when the spinner character updates
         * @param spinnerChar The current spinner character
         */
        void onSpinnerUpdate(String spinnerChar);
    }
    
    /**
     * Creates a new BrailleSpinner
     */
    public BrailleSpinner() {
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                updateSpinner();
                if (isSpinning) {
                    handler.postDelayed(this, updateIntervalMs);
                }
            }
        };
    }
    
    /**
     * Sets the spinner update listener
     * @param listener The listener to receive spinner updates
     */
    public void setSpinnerListener(SpinnerListener listener) {
        this.spinnerListener = listener;
    }
    
    /**
     * Sets the update interval for the spinner animation
     * @param intervalMs The interval in milliseconds between frame updates
     */
    public void setUpdateInterval(int intervalMs) {
        this.updateIntervalMs = intervalMs;
    }
    
    /**
     * Starts the spinner animation
     */
    public void start() {
        if (!isSpinning) {
            Timber.d("BrailleSpinner: Starting spinner animation");
            isSpinning = true;
            currentFrame = 0;
            handler.post(runnable);
        }
    }
    
    /**
     * Stops the spinner animation
     */
    public void stop() {
        if (isSpinning) {
            Timber.d("BrailleSpinner: Stopping spinner animation");
            isSpinning = false;
            handler.removeCallbacks(runnable);
            
            // Clear the spinner by sending an empty character
            if (spinnerListener != null) {
                spinnerListener.onSpinnerUpdate("");
            }
        }
    }
    
    /**
     * Updates the spinner and notifies the listener
     */
    private void updateSpinner() {
        if (currentFrame >= BRAILLE_FRAMES.length) {
            currentFrame = 0;
        }
        
        currentText = BRAILLE_FRAMES[currentFrame++];
        
        if (spinnerListener != null) {
            spinnerListener.onSpinnerUpdate(currentText);
        }
    }
    
    /**
     * Gets the current spinner frame.
     * This method supports direct access to the spinner state for legacy code.
     * @return The current spinner character
     */
    public String getCurrentFrame() {
        return currentText;
    }
    
    /**
     * @return Whether the spinner is currently spinning
     */
    public boolean isSpinning() {
        return isSpinning;
    }
}

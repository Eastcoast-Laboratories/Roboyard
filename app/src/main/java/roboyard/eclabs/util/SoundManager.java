package roboyard.eclabs.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;

import timber.log.Timber;
import roboyard.eclabs.R;

/**
 * Manages sound effects for the Modern Game
 */
public class SoundManager {
    private static SoundManager instance;
    private final Context context;
    private MediaPlayer currentPlayer;
    private boolean isSoundPlaying = false;
    
    // Private constructor for singleton
    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Get the singleton instance of SoundManager
     * @param context Application context
     * @return SoundManager instance
     */
    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }
    
    /**
     * Play a sound effect for robot movement
     * @param soundType Type of sound: "move", "hit_wall", "hit_robot", "win", "lose"
     */
    public void playSound(String soundType) {
        Timber.d("[SOUND] Attempting to play sound: %s", soundType);
        // Skip if another sound is playing
        if (isSoundPlaying && currentPlayer != null && currentPlayer.isPlaying()) {
            Timber.d("[SOUND] Not playing sound %s - another sound is already playing", soundType);
            return;
        }
        
        // Stop any previous sound
        stopCurrentSound();
        
        // Create and play the new sound
        int soundResId = 0;
        
        // Get the sound resource ID
        switch (soundType) {
            case "move":
                Timber.d("[SOUND] Selected robot_move sound");
                soundResId = R.raw.robot_move;
                break;
            case "hit_wall":
                Timber.d("[SOUND] Selected robot_hit_wall sound");
                soundResId = R.raw.robot_hit_wall;
                break;
            case "hit_robot":
                Timber.d("[SOUND] Selected robot_hit_robot sound");
                soundResId = R.raw.robot_hit_robot;
                break;
            case "win":
                Timber.d("[SOUND] Selected robot_win sound");
                soundResId = R.raw.robot_win;
                break;
            case "lose":
                Timber.d("[SOUND] Selected robot_hit_wall sound as fallback for lose");
                // We'll use hit_wall sound for now as a fallback
                soundResId = R.raw.robot_hit_wall;
                break;
            case "none":
                Timber.d("[SOUND] No sound to play");
                return;
            default:
                Timber.e("[SOUND] Unknown sound type: %s", soundType);
                return;
        }
        
        if (soundResId != 0) {
            try {
                Timber.d("[SOUND] Creating MediaPlayer for sound ID: %d", soundResId);
                // Create the media player for this sound
                MediaPlayer mp = MediaPlayer.create(context, soundResId);
                if (mp == null) {
                    Timber.e("[SOUND] Failed to create MediaPlayer for sound: %s", soundType);
                    return;
                }
                
                // Set volume to half (0.5) of the maximum for both left and right channels
                mp.setVolume(0.5f, 0.5f);
                Timber.d("[SOUND] Set volume to 50%% for sound: %s", soundType);
                
                // Set the global current player
                currentPlayer = mp;
                isSoundPlaying = true;
                
                // Play the sound
                Timber.d("[SOUND] Starting playback for sound: %s", soundType);
                mp.start();
                
                // When playback completes, release the player and reset the flag
                mp.setOnCompletionListener(mediaPlayer -> {
                    Timber.d("[SOUND] Completed playback for sound: %s", soundType);
                    mediaPlayer.release();
                    currentPlayer = null;
                    isSoundPlaying = false;
                });
            } catch (Exception e) {
                Timber.e(e, "[SOUND] Error playing sound: %s", soundType);
            }
        } else {
            Timber.e("[SOUND] No valid sound resource ID for type: %s", soundType);
        }
    }
    
    /**
     * Stop the currently playing sound
     */
    private void stopCurrentSound() {
        if (currentPlayer != null) {
            try {
                if (currentPlayer.isPlaying()) {
                    currentPlayer.stop();
                }
                currentPlayer.release();
                currentPlayer = null;
                isSoundPlaying = false;
            } catch (Exception e) {
                Timber.e(e, "Error stopping previous sound");
            }
        }
    }
}

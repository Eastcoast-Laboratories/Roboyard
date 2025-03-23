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
    private Context context;
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
        Timber.d("Playing sound: %s", soundType);
        // Skip if another sound is playing
        if (isSoundPlaying && currentPlayer != null && currentPlayer.isPlaying()) {
            Timber.d("Not playing sound %s - another sound is already playing", soundType);
            return;
        }
        
        // Stop any previous sound
        stopCurrentSound();
        
        // Create and play the new sound
        int soundResId = 0;
        
        // Get the sound resource ID
        switch (soundType) {
            case "move":
                soundResId = R.raw.robot_move;
                break;
            case "hit_wall":
                soundResId = R.raw.robot_hit_wall;
                break;
            case "hit_robot":
                soundResId = R.raw.robot_hit_robot;
                break;
            case "win":
                soundResId = R.raw.robot_win;
                break;
            case "lose":
                // We'll use hit_wall sound for now as a fallback
                soundResId = R.raw.robot_hit_wall;
                break;
            case "none":
                Timber.d("No sound to play");
                return;
            default:
                Timber.d("Unknown sound type: %s", soundType);
                return;
        }
        
        if (soundResId != 0) {
            try {
                // Create the media player for this sound
                MediaPlayer mp = new MediaPlayer();
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(soundResId);
                if (afd == null) {
                    Timber.e("Failed to open resource: %d", soundResId);
                    return;
                }
                
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mp.prepare();
                
                // Set the global current player
                currentPlayer = mp;
                isSoundPlaying = true;
                
                // Play the sound
                mp.start();
                
                // When playback completes, release the player and reset the flag
                mp.setOnCompletionListener(mediaPlayer -> {
                    mediaPlayer.release();
                    currentPlayer = null;
                    isSoundPlaying = false;
                });
            } catch (Exception e) {
                Timber.e(e, "Error playing sound: %s", soundType);
            }
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

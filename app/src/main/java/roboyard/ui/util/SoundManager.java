package roboyard.ui.util;

import android.content.Context;
import android.media.MediaPlayer;

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
        playSound(soundType, -1, -1);
    }
    
    /**
     * Play a robot-specific collision sound
     * @param soundType Type of sound: "hit_robot" for robot-specific collision sounds
     * @param attackerRobotId ID of the robot that moved (0-4)
     * @param targetRobotId ID of the robot that was hit (0-4)
     */
    public void playSound(String soundType, int attackerRobotId, int targetRobotId) {
        Timber.d("[SOUND] Attempting to play sound: %s (attacker=%d, target=%d)", soundType, attackerRobotId, targetRobotId);
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
        if ("hit_robot".equals(soundType) && attackerRobotId >= 0 && attackerRobotId <= 4 && targetRobotId >= 0 && targetRobotId <= 4) {
            // Try to play robot-specific collision sound using direct resource ID lookup
            soundResId = getRobotCollisionSoundId(attackerRobotId, targetRobotId);
            if (soundResId != 0) {
                Timber.d("[SOUND] Selected robot-specific collision sound: robot_%d_hits_robot_%d (resourceId=%d)", attackerRobotId, targetRobotId, soundResId);
            } else {
                Timber.w("[SOUND] Robot-specific sound not found for robot_%d_hits_robot_%d, falling back to generic hit_robot", attackerRobotId, targetRobotId);
                soundResId = R.raw.robot_hit_robot;
            }
        } else {
            // Fall back to generic sounds
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
                    Timber.d("[SOUND] Selected robot_hit_robot sound (generic fallback)");
                    soundResId = R.raw.robot_hit_robot;
                    break;
                case "win":
                    Timber.d("[SOUND] Selected robot_win sound");
                    soundResId = R.raw.robot_win;
                    break;
                case "lose":
                    Timber.d("[SOUND] Selected robot_hit_wall sound as fallback for lose");
                    soundResId = R.raw.robot_hit_wall;
                    break;
                case "none":
                    Timber.d("[SOUND] No sound to play");
                    return;
                default:
                    Timber.e("[SOUND] Unknown sound type: %s", soundType);
                    return;
            }
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
     * Get robot collision sound resource ID using direct mapping
     * This avoids reflection issues and ensures sounds are found even if R.raw has issues
     * @param attackerRobotId ID of attacking robot (0-4)
     * @param targetRobotId ID of target robot (0-4)
     * @return Resource ID or 0 if not found
     */
    private int getRobotCollisionSoundId(int attackerRobotId, int targetRobotId) {
        // Direct mapping of robot collision sounds
        // This ensures sounds are found even if reflection fails
        switch (attackerRobotId) {
            case 0:
                switch (targetRobotId) {
                    case 0: return R.raw.robot_0_hits_robot_0;
                    case 1: return R.raw.robot_0_hits_robot_1;
                    case 2: return R.raw.robot_0_hits_robot_2;
                    case 3: return R.raw.robot_0_hits_robot_3;
                    case 4: return R.raw.robot_0_hits_robot_4;
                }
                break;
            case 1:
                switch (targetRobotId) {
                    case 0: return R.raw.robot_1_hits_robot_0;
                    case 1: return R.raw.robot_1_hits_robot_1;
                    case 2: return R.raw.robot_1_hits_robot_2;
                    case 3: return R.raw.robot_1_hits_robot_3;
                    case 4: return R.raw.robot_1_hits_robot_4;
                }
                break;
            case 2:
                switch (targetRobotId) {
                    case 0: return R.raw.robot_2_hits_robot_0;
                    case 1: return R.raw.robot_2_hits_robot_1;
                    case 2: return R.raw.robot_2_hits_robot_2;
                    case 3: return R.raw.robot_2_hits_robot_3;
                    case 4: return R.raw.robot_2_hits_robot_4;
                }
                break;
            case 3:
                switch (targetRobotId) {
                    case 0: return R.raw.robot_3_hits_robot_0;
                    case 1: return R.raw.robot_3_hits_robot_1;
                    case 2: return R.raw.robot_3_hits_robot_2;
                    case 3: return R.raw.robot_3_hits_robot_3;
                    case 4: return R.raw.robot_3_hits_robot_4;
                }
                break;
            case 4:
                switch (targetRobotId) {
                    case 0: return R.raw.robot_4_hits_robot_0;
                    case 1: return R.raw.robot_4_hits_robot_1;
                    case 2: return R.raw.robot_4_hits_robot_2;
                    case 3: return R.raw.robot_4_hits_robot_3;
                    case 4: return R.raw.robot_4_hits_robot_4;
                }
                break;
        }
        Timber.w("[SOUND] No resource ID found for robot_%d_hits_robot_%d", attackerRobotId, targetRobotId);
        return 0;
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

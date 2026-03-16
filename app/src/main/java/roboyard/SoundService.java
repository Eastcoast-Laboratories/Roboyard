package roboyard;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import roboyard.eclabs.R;
import timber.log.Timber;

public class SoundService extends Service {
    public static final String EXTRA_VOLUME = "volume";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_RESUME = "resume";
    private MediaPlayer player;
    private boolean isPaused = false;

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("[SOUND_SERVICE] onBind called");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("[SOUND_SERVICE] ========== onCreate() START ==========");
        Timber.d("[SOUND_SERVICE] Service instance created, player is currently: %s", player == null ? "NULL" : "NOT NULL");
        
        try {
            player = MediaPlayer.create(this, R.raw.singing_bowls_in_a_forest);
            if (player != null) {
                player.setLooping(true);
                Timber.d("[SOUND_SERVICE] MediaPlayer created successfully, looping enabled");
                Timber.d("[SOUND_SERVICE] MediaPlayer state - isPlaying: %b, duration: %d", player.isPlaying(), player.getDuration());
            } else {
                Timber.e("[SOUND_SERVICE] CRITICAL: MediaPlayer.create() returned NULL");
            }
        } catch (Exception e) {
            Timber.e(e, "[SOUND_SERVICE] EXCEPTION in onCreate while creating MediaPlayer");
        }
        
        Timber.d("[SOUND_SERVICE] ========== onCreate() END ==========");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("[SOUND_SERVICE] ========== onStartCommand() START ==========");
        Timber.d("[SOUND_SERVICE] Intent: %s, flags: %d, startId: %d", intent, flags, startId);
        Timber.d("[SOUND_SERVICE] Player state: %s", player == null ? "NULL" : "NOT NULL");
        
        if (player == null) {
            Timber.e("[SOUND_SERVICE] CRITICAL: MediaPlayer is NULL in onStartCommand - stopping service");
            Timber.e("[SOUND_SERVICE] This means onCreate() was not called or MediaPlayer creation failed");
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        // Handle pause/resume actions
        if (intent != null && intent.getAction() != null) {
            if (ACTION_PAUSE.equals(intent.getAction())) {
                if (player.isPlaying()) {
                    player.pause();
                    isPaused = true;
                    Timber.d("[SOUND_SERVICE] Playback PAUSED (app in background)");
                }
                return Service.START_NOT_STICKY;
            } else if (ACTION_RESUME.equals(intent.getAction())) {
                if (isPaused && !player.isPlaying()) {
                    player.start();
                    isPaused = false;
                    Timber.d("[SOUND_SERVICE] Playback RESUMED (app in foreground)");
                }
                return Service.START_NOT_STICKY;
            }
        }

        int volumePercent = 10;
        if (intent != null && intent.hasExtra(EXTRA_VOLUME)) {
            volumePercent = intent.getIntExtra(EXTRA_VOLUME, 10);
            Timber.d("[SOUND_SERVICE] Volume from intent: %d%%", volumePercent);
        } else {
            Timber.w("[SOUND_SERVICE] No volume in intent, using default: %d%%", volumePercent);
        }

        // Convert linear slider (0-100) to logarithmic volume (0.0-1.0)
        float logVolume = roboyard.logic.core.Preferences.getLogarithmicVolume(volumePercent);
        player.setVolume(logVolume, logVolume);
        Timber.d("[SOUND_SERVICE] Volume set - slider: %d%%, logarithmic: %.2f", volumePercent, logVolume);

        // Always restart from beginning so user hears the loudest part for calibration
        player.seekTo(0);
        Timber.d("[SOUND_SERVICE] Seeked to position 0");
        
        if (!player.isPlaying()) {
            try {
                player.start();
                isPaused = false;
                Timber.d("[SOUND_SERVICE] ✓ Playback STARTED successfully");
                Timber.d("[SOUND_SERVICE] MediaPlayer state - isPlaying: %b, position: %d, duration: %d", 
                        player.isPlaying(), player.getCurrentPosition(), player.getDuration());
            } catch (Exception e) {
                Timber.e(e, "[SOUND_SERVICE] EXCEPTION while starting playback");
            }
        } else {
            Timber.d("[SOUND_SERVICE] Player already playing, not starting again");
        }

        Timber.d("[SOUND_SERVICE] ========== onStartCommand() END ==========");
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.d("[SOUND_SERVICE] ========== onDestroy() START ==========");
        if (player != null) {
            Timber.d("[SOUND_SERVICE] Player exists, isPlaying: %b", player.isPlaying());
            if (player.isPlaying()) {
                player.stop();
                Timber.d("[SOUND_SERVICE] Playback stopped");
            }
            player.release();
            player = null;
            Timber.d("[SOUND_SERVICE] MediaPlayer released and set to null");
        } else {
            Timber.w("[SOUND_SERVICE] Player was already null in onDestroy");
        }
        super.onDestroy();
        Timber.d("[SOUND_SERVICE] ========== onDestroy() END ==========");
    }
}
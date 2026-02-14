package roboyard;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import roboyard.eclabs.R;
import timber.log.Timber;

public class SoundService extends Service {
    public static final String EXTRA_VOLUME = "volume";
    private MediaPlayer player;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = MediaPlayer.create(this, R.raw.singing_bowls_in_a_forest);
        if (player != null) {
            player.setLooping(true);
            Timber.d("[SOUND_SERVICE] MediaPlayer created");
        } else {
            Timber.e("[SOUND_SERVICE] Failed to create MediaPlayer");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (player == null) {
            Timber.e("[SOUND_SERVICE] MediaPlayer is null in onStartCommand");
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        int volumePercent = 10;
        if (intent != null && intent.hasExtra(EXTRA_VOLUME)) {
            volumePercent = intent.getIntExtra(EXTRA_VOLUME, 10);
        }

        // Map slider 0-100 to actual volume 0-73%
        float actualVolume = (volumePercent / 100f) * 0.73f;
        player.setVolume(actualVolume, actualVolume);
        Timber.d("[SOUND_SERVICE] Volume slider %d%%, actual %.0f%% (%.2f)", volumePercent, actualVolume * 100, actualVolume);

        // Always restart from beginning so user hears the loudest part for calibration
        player.seekTo(0);
        if (!player.isPlaying()) {
            player.start();
            Timber.d("[SOUND_SERVICE] Playback started");
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
            player = null;
            Timber.d("[SOUND_SERVICE] MediaPlayer released");
        }
        super.onDestroy();
    }
}
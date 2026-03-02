package roboyard.ui.components;

import android.app.Activity;
import android.content.Context;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import timber.log.Timber;
import roboyard.eclabs.BuildConfig;

/**
 * Manager for Google Play Games Services integration.
 * Handles authentication and achievement syncing.
 * 
 * This class is guarded by BuildConfig.ENABLE_PLAY_GAMES flag.
 * Play Store flavor version - GMS fully enabled.
 */
public class PlayGamesManager {
    
    private static final String TAG = "[PLAY_GAMES]";
    private static PlayGamesManager instance;
    
    private final Context context;
    private boolean isInitialized = false;
    private boolean isSignedIn = false;
    
    private PlayGamesManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized PlayGamesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlayGamesManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize Play Games SDK. Call this in Application.onCreate() or MainActivity.onCreate().
     */
    public void initialize() {
        if (!BuildConfig.ENABLE_PLAY_GAMES) {
            Timber.d("%s Play Games disabled in this build", TAG);
            return;
        }
        
        if (isInitialized) {
            Timber.d("%s Already initialized", TAG);
            return;
        }
        
        try {
            PlayGamesSdk.initialize(context);
            isInitialized = true;
            Timber.d("%s SDK initialized successfully", TAG);
        } catch (Exception e) {
            Timber.e(e, "%s Failed to initialize SDK", TAG);
        }
    }
    
    
    /**
     * Unlock an achievement by its local ID.
     * Maps local achievement IDs to Google Play Games achievement IDs.
     * 
     * @param activity The current activity
     * @param localAchievementId The local achievement ID (e.g., "first_game")
     */
    public void unlockAchievement(Activity activity, String localAchievementId) {
        if (!BuildConfig.ENABLE_PLAY_GAMES || !isInitialized || !isSignedIn) {
            Timber.d("%s Cannot unlock achievement %s - not ready (enabled=%b, init=%b, signedIn=%b)", 
                    TAG, localAchievementId, BuildConfig.ENABLE_PLAY_GAMES, isInitialized, isSignedIn);
            return;
        }
        
        roboyard.ui.achievements.AchievementManager achievementManager = 
            roboyard.ui.achievements.AchievementManager.getInstance(context);
        String playGamesId = achievementManager.getPlayGamesAchievementId(localAchievementId);
        if (playGamesId == null || playGamesId.startsWith("REPLACE_")) {
            Timber.w("%s Achievement ID not configured for: %s", TAG, localAchievementId);
            return;
        }
        
        try {
            PlayGames.getAchievementsClient(activity).unlock(playGamesId);
            Timber.d("%s Unlocked achievement: %s -> %s", TAG, localAchievementId, playGamesId);
        } catch (Exception e) {
            Timber.e(e, "%s Failed to unlock achievement: %s", TAG, localAchievementId);
        }
    }
}

package roboyard.ui.components;

import android.app.Activity;
import android.content.Context;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import timber.log.Timber;
import roboyard.eclabs.R;
import roboyard.eclabs.BuildConfig;

/**
 * Manager for Google Play Games Services integration.
 * Handles authentication and achievement syncing.
 * 
 * This class is guarded by BuildConfig.ENABLE_PLAY_GAMES flag.
 * On F-Droid builds, all methods are no-ops.
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
        
        String playGamesId = getPlayGamesAchievementId(localAchievementId);
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
    
    /**
     * Map local achievement ID to Google Play Games achievement ID.
     * 
     * @param localId The local achievement ID
     * @return The Google Play Games achievement ID, or null if not found
     */
    private String getPlayGamesAchievementId(String localId) {
        try {
            int resId = 0;
            switch (localId) {
                case "first_game":
                    resId = R.string.pgs_welcome;
                    break;
                case "level_1_complete":
                    resId = R.string.pgs_first_steps;
                    break;
                case "level_10_complete":
                    resId = R.string.pgs_getting_started;
                    break;
                case "level_50_complete":
                    resId = R.string.pgs_halfway_there;
                    break;
                case "level_140_complete":
                    resId = R.string.pgs_level_master;
                    break;
                case "all_stars_collected":
                    resId = R.string.pgs_star_collector;
                    break;
                case "perfect_solutions_5":
                    resId = R.string.pgs_perfect_mover;
                    break;
                case "perfect_solutions_10":
                    resId = R.string.pgs_precision_player;
                    break;
                case "perfect_solutions_50":
                    resId = R.string.pgs_optimization_expert;
                    break;
                case "speedrun_under_30s":
                    resId = R.string.pgs_quick_thinker;
                    break;
                case "speedrun_under_10s":
                    resId = R.string.pgs_lightning_fast;
                    break;
                case "3_star_hard_level":
                    resId = R.string.pgs_hard_level_star;
                    break;
                case "3_star_10_levels":
                    resId = R.string.pgs_rising_star;
                    break;
                case "3_star_10_hard_levels":
                    resId = R.string.pgs_hard_level_master;
                    break;
                case "3_star_50_levels":
                    resId = R.string.pgs_superstar;
                    break;
                case "3_star_all_levels":
                    resId = R.string.pgs_perfect_master;
                    break;
                case "daily_login_7":
                    resId = R.string.pgs_weekly_player;
                    break;
                case "daily_login_30":
                    resId = R.string.pgs_dedicated_player;
                    break;
                case "comeback_player":
                    resId = R.string.pgs_welcome_back;
                    break;
                case "impossible_mode_1":
                    resId = R.string.pgs_impossible_dream;
                    break;
                case "impossible_mode_5":
                    resId = R.string.pgs_impossible_champion;
                    break;
                case "impossible_mode_streak_5":
                    resId = R.string.pgs_impossible_streak;
                    break;
                case "impossible_mode_streak_10":
                    resId = R.string.pgs_impossible_legend;
                    break;
                case "solution_30_plus_moves":
                    resId = R.string.pgs_30_move_master;
                    break;
                case "play_10_move_games_all_resolutions":
                    resId = R.string.pgs_resolution_explorer_10;
                    break;
                case "play_12_move_games_all_resolutions":
                    resId = R.string.pgs_resolution_explorer_12;
                    break;
                case "play_15_move_games_all_resolutions":
                    resId = R.string.pgs_resolution_explorer_15;
                    break;
                case "game_2_targets":
                    resId = R.string.pgs_double_target;
                    break;
                case "game_3_targets":
                    resId = R.string.pgs_triple_target;
                    break;
                case "game_4_targets":
                    resId = R.string.pgs_quad_target;
                    break;
                case "game_2_of_2_targets":
                    resId = R.string.pgs_2_of_2;
                    break;
                case "game_2_of_3_targets":
                    resId = R.string.pgs_2_of_3;
                    break;
                case "game_2_of_4_targets":
                    resId = R.string.pgs_2_of_4;
                    break;
                case "game_3_of_3_targets":
                    resId = R.string.pgs_3_of_3;
                    break;
                case "game_3_of_4_targets":
                    resId = R.string.pgs_3_of_4;
                    break;
                case "game_4_of_4_targets":
                    resId = R.string.pgs_4_of_4;
                    break;
                case "game_5_robots":
                    resId = R.string.pgs_full_team;
                    break;
                case "gimme_five":
                    resId = R.string.pgs_gimme_five;
                    break;
                case "traverse_all_squares_1_robot":
                    resId = R.string.pgs_solo_explorer;
                    break;
                case "traverse_all_squares_1_robot_goal":
                    resId = R.string.pgs_solo_goal_explorer;
                    break;
                case "traverse_all_squares_all_robots":
                    resId = R.string.pgs_team_explorer;
                    break;
                case "traverse_all_squares_all_robots_goal":
                    resId = R.string.pgs_team_goal_explorer;
                    break;
                case "perfect_random_games_5":
                    resId = R.string.pgs_perfect_5;
                    break;
                case "perfect_random_games_10":
                    resId = R.string.pgs_perfect_10;
                    break;
                case "perfect_random_games_20":
                    resId = R.string.pgs_perfect_20;
                    break;
                case "perfect_random_games_streak_5":
                    resId = R.string.pgs_perfect_streak_5;
                    break;
                case "perfect_random_games_streak_10":
                    resId = R.string.pgs_perfect_streak_10;
                    break;
                case "perfect_random_games_streak_20":
                    resId = R.string.pgs_perfect_streak_20;
                    break;
                case "no_hints_random_10":
                    resId = R.string.pgs_no_help_needed_10;
                    break;
                case "no_hints_random_50":
                    resId = R.string.pgs_no_help_needed_50;
                    break;
                case "no_hints_streak_random_10":
                    resId = R.string.pgs_no_help_streak_10;
                    break;
                case "no_hints_streak_random_50":
                    resId = R.string.pgs_no_help_streak_50;
                    break;
                case "speedrun_random_under_20s":
                    resId = R.string.pgs_speed_demon;
                    break;
                case "speedrun_random_under_10s":
                    resId = R.string.pgs_lightning_speed;
                    break;
                case "speedrun_random_5_games_under_30s":
                    resId = R.string.pgs_speed_streak;
                    break;
                case "solution_18_moves":
                    resId = R.string.pgs_18_move_master;
                    break;
                case "solution_19_moves":
                    resId = R.string.pgs_19_move_master;
                    break;
                case "solution_20_moves":
                    resId = R.string.pgs_20_move_master;
                    break;
                case "solution_21_moves":
                    resId = R.string.pgs_21_move_master;
                    break;
                case "solution_22_moves":
                    resId = R.string.pgs_22_move_master;
                    break;
                case "solution_23_moves":
                    resId = R.string.pgs_23_move_master;
                    break;
                case "solution_24_moves":
                    resId = R.string.pgs_24_move_master;
                    break;
                case "solution_25_moves":
                    resId = R.string.pgs_25_move_master;
                    break;
                case "solution_26_moves":
                    resId = R.string.pgs_26_move_master;
                    break;
                case "solution_27_moves":
                    resId = R.string.pgs_27_move_master;
                    break;
                case "solution_28_moves":
                    resId = R.string.pgs_28_move_master;
                    break;
                case "solution_29_moves":
                    resId = R.string.pgs_29_move_master;
                    break;
                case "perfect_no_hints_random_1":
                    resId = R.string.pgs_perfect_no_help;
                    break;
                default:
                    Timber.w("%s Unknown achievement ID: %s", TAG, localId);
                    return null;
            }
            return context.getString(resId);
        } catch (Exception e) {
            Timber.e(e, "%s Failed to get Play Games ID for: %s", TAG, localId);
            return null;
        }
    }
}

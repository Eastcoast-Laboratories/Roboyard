package roboyard.eclabs.achievements;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Manages achievement unlocking, storage, and retrieval.
 */
public class AchievementManager {
    
    private static final String PREFS_NAME = "roboyard_achievements";
    private static final String KEY_PREFIX_UNLOCKED = "unlocked_";
    private static final String KEY_PREFIX_TIMESTAMP = "timestamp_";
    private static final String KEY_COUNTER_PREFIX = "counter_";
    
    private static AchievementManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Map<String, Achievement> achievements;
    private AchievementUnlockListener unlockListener;
    
    // Counters for tracking progress
    private int levelsCompleted;
    private int perfectSolutions;
    private int noHintLevels;
    private int threeStarLevels;
    private int impossibleModeGames;
    private int impossibleModeStreak;
    private int perfectRandomGames;
    private int noHintRandomGames;
    private int dailyLoginStreak;
    private int speedrunRandomGamesUnder30s;
    
    public interface AchievementUnlockListener {
        void onAchievementUnlocked(Achievement achievement);
    }
    
    private AchievementManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.achievements = AchievementDefinitions.getAll();
        loadState();
    }
    
    public static synchronized AchievementManager getInstance(Context context) {
        if (instance == null) {
            instance = new AchievementManager(context);
        }
        return instance;
    }
    
    public void setUnlockListener(AchievementUnlockListener listener) {
        this.unlockListener = listener;
    }
    
    private void loadState() {
        // Load unlock status for all achievements
        for (Achievement achievement : achievements.values()) {
            boolean unlocked = prefs.getBoolean(KEY_PREFIX_UNLOCKED + achievement.getId(), false);
            long timestamp = prefs.getLong(KEY_PREFIX_TIMESTAMP + achievement.getId(), 0);
            achievement.setUnlocked(unlocked);
            achievement.setUnlockedTimestamp(timestamp);
        }
        
        // Load counters
        levelsCompleted = prefs.getInt(KEY_COUNTER_PREFIX + "levels_completed", 0);
        perfectSolutions = prefs.getInt(KEY_COUNTER_PREFIX + "perfect_solutions", 0);
        noHintLevels = prefs.getInt(KEY_COUNTER_PREFIX + "no_hint_levels", 0);
        threeStarLevels = prefs.getInt(KEY_COUNTER_PREFIX + "three_star_levels", 0);
        impossibleModeGames = prefs.getInt(KEY_COUNTER_PREFIX + "impossible_mode_games", 0);
        impossibleModeStreak = prefs.getInt(KEY_COUNTER_PREFIX + "impossible_mode_streak", 0);
        perfectRandomGames = prefs.getInt(KEY_COUNTER_PREFIX + "perfect_random_games", 0);
        noHintRandomGames = prefs.getInt(KEY_COUNTER_PREFIX + "no_hint_random_games", 0);
        dailyLoginStreak = prefs.getInt(KEY_COUNTER_PREFIX + "daily_login_streak", 0);
        speedrunRandomGamesUnder30s = prefs.getInt(KEY_COUNTER_PREFIX + "speedrun_random_30s", 0);
        
        Timber.d("[ACHIEVEMENTS] Loaded state: %d achievements, %d unlocked", 
            achievements.size(), getUnlockedCount());
    }
    
    private void saveCounter(String key, int value) {
        prefs.edit().putInt(KEY_COUNTER_PREFIX + key, value).apply();
    }
    
    /**
     * Unlock an achievement by ID.
     * @return true if newly unlocked, false if already unlocked
     */
    public boolean unlock(String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) {
            Timber.w("[ACHIEVEMENTS] Unknown achievement: %s", achievementId);
            return false;
        }
        
        if (achievement.isUnlocked()) {
            return false; // Already unlocked
        }
        
        achievement.setUnlocked(true);
        long timestamp = System.currentTimeMillis();
        achievement.setUnlockedTimestamp(timestamp);
        
        // Save to SharedPreferences
        prefs.edit()
            .putBoolean(KEY_PREFIX_UNLOCKED + achievementId, true)
            .putLong(KEY_PREFIX_TIMESTAMP + achievementId, timestamp)
            .apply();
        
        Timber.d("[ACHIEVEMENTS] Unlocked: %s", achievementId);
        
        // Notify listener
        if (unlockListener != null) {
            unlockListener.onAchievementUnlocked(achievement);
        }
        
        return true;
    }
    
    public boolean isUnlocked(String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        return achievement != null && achievement.isUnlocked();
    }
    
    public Achievement getAchievement(String achievementId) {
        return achievements.get(achievementId);
    }
    
    public List<Achievement> getAllAchievements() {
        return new ArrayList<>(achievements.values());
    }
    
    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : achievements.values()) {
            if (achievement.isUnlocked()) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }
    
    public List<Achievement> getLockedAchievements() {
        List<Achievement> locked = new ArrayList<>();
        for (Achievement achievement : achievements.values()) {
            if (!achievement.isUnlocked()) {
                locked.add(achievement);
            }
        }
        return locked;
    }
    
    public int getTotalCount() {
        return achievements.size();
    }
    
    public int getUnlockedCount() {
        int count = 0;
        for (Achievement achievement : achievements.values()) {
            if (achievement.isUnlocked()) {
                count++;
            }
        }
        return count;
    }
    
    // ========== GAME EVENT HANDLERS ==========
    
    /**
     * Called when any game is completed (first game achievement)
     */
    public void onFirstGame() {
        unlock("first_game");
    }
    
    /**
     * Called when a level is completed
     */
    public void onLevelCompleted(int levelId, int playerMoves, int optimalMoves, 
                                  int hintsUsed, int stars, long timeMs) {
        // First game achievement (any game completion)
        unlock("first_game");
        
        levelsCompleted++;
        saveCounter("levels_completed", levelsCompleted);
        
        // Level progression achievements
        if (levelId >= 1) unlock("level_1_complete");
        if (levelsCompleted >= 10) unlock("level_10_complete");
        if (levelsCompleted >= 50) unlock("level_50_complete");
        if (levelsCompleted >= 140) unlock("level_140_complete");
        
        // Perfect solution
        if (playerMoves == optimalMoves) {
            perfectSolutions++;
            saveCounter("perfect_solutions", perfectSolutions);
            unlock("perfect_solution_1");
            if (perfectSolutions >= 10) unlock("perfect_solutions_10");
            if (perfectSolutions >= 50) unlock("perfect_solutions_50");
        }
        
        // No hints
        if (hintsUsed == 0) {
            noHintLevels++;
            saveCounter("no_hint_levels", noHintLevels);
            if (noHintLevels >= 10) unlock("no_hints_10");
            if (noHintLevels >= 50) unlock("no_hints_50");
        }
        
        // 3 stars
        if (stars >= 3) {
            threeStarLevels++;
            saveCounter("three_star_levels", threeStarLevels);
            unlock("3_star_level");
            if (threeStarLevels >= 10) unlock("3_star_10_levels");
            if (threeStarLevels >= 50) unlock("3_star_50_levels");
            if (threeStarLevels >= 140) unlock("3_star_all_levels");
        }
        
        // Speedrun
        if (timeMs < 30000) unlock("speedrun_under_30s");
        if (timeMs < 10000) unlock("speedrun_under_10s");
        
        Timber.d("[ACHIEVEMENTS] Level %d completed: moves=%d/%d, hints=%d, stars=%d, time=%dms",
            levelId, playerMoves, optimalMoves, hintsUsed, stars, timeMs);
    }
    
    /**
     * Called when total stars are updated
     */
    public void onTotalStarsUpdated(int totalStars) {
        if (totalStars >= 420) {
            unlock("all_stars_collected");
        }
    }
    
    /**
     * Called when a random game is completed
     */
    public void onRandomGameCompleted(int playerMoves, int optimalMoves, int hintsUsed, 
                                       long timeMs, boolean isImpossibleMode, int robotCount,
                                       int targetCount, int targetsNeeded) {
        // First game
        onFirstGame();
        
        // Impossible mode
        if (isImpossibleMode) {
            impossibleModeGames++;
            saveCounter("impossible_mode_games", impossibleModeGames);
            unlock("impossible_mode_1");
            if (impossibleModeGames >= 5) unlock("impossible_mode_5");
            
            // Impossible mode streak (perfect solutions)
            if (playerMoves == optimalMoves) {
                impossibleModeStreak++;
                saveCounter("impossible_mode_streak", impossibleModeStreak);
                if (impossibleModeStreak >= 5) unlock("impossible_mode_streak_5");
                if (impossibleModeStreak >= 10) unlock("impossible_mode_streak_10");
            } else {
                impossibleModeStreak = 0;
                saveCounter("impossible_mode_streak", 0);
            }
        }
        
        // Solution length achievements
        if (optimalMoves >= 20 && optimalMoves <= 29) {
            unlock("solution_" + optimalMoves + "_moves");
        } else if (optimalMoves >= 30) {
            unlock("solution_30_plus_moves");
        }
        
        // Multiple targets
        if (targetCount >= 2) unlock("game_2_targets");
        if (targetCount >= 3) unlock("game_3_targets");
        if (targetCount >= 4) unlock("game_4_targets");
        
        // X of Y targets
        if (targetsNeeded == 2 && targetCount == 3) unlock("game_2_of_3_targets");
        if (targetsNeeded == 2 && targetCount == 4) unlock("game_2_of_4_targets");
        if (targetsNeeded == 3 && targetCount == 4) unlock("game_3_of_4_targets");
        
        // Robot count
        if (robotCount >= 5) unlock("game_5_robots");
        
        // Perfect random games
        if (playerMoves == optimalMoves) {
            perfectRandomGames++;
            saveCounter("perfect_random_games", perfectRandomGames);
            if (perfectRandomGames >= 5) unlock("perfect_random_games_5");
            if (perfectRandomGames >= 10) unlock("perfect_random_games_10");
            if (perfectRandomGames >= 20) unlock("perfect_random_games_20");
        }
        
        // No hints random games
        if (hintsUsed == 0) {
            noHintRandomGames++;
            saveCounter("no_hint_random_games", noHintRandomGames);
            if (noHintRandomGames >= 10) unlock("no_hints_random_10");
            if (noHintRandomGames >= 50) unlock("no_hints_random_50");
        }
        
        // Speed achievements
        if (timeMs < 20000) unlock("speedrun_random_under_20s");
        if (timeMs < 10000) unlock("speedrun_random_under_10s");
        if (timeMs < 30000) {
            speedrunRandomGamesUnder30s++;
            saveCounter("speedrun_random_30s", speedrunRandomGamesUnder30s);
            if (speedrunRandomGamesUnder30s >= 5) unlock("speedrun_random_5_games_under_30s");
        }
        
        Timber.d("[ACHIEVEMENTS] Random game completed: moves=%d/%d, hints=%d, time=%dms, impossible=%s, robots=%d, targets=%d/%d",
            playerMoves, optimalMoves, hintsUsed, timeMs, isImpossibleMode, robotCount, targetsNeeded, targetCount);
    }
    
    /**
     * Called when a custom level is created
     */
    public void onCustomLevelCreated() {
        unlock("create_custom_level");
    }
    
    /**
     * Called when a custom level is solved
     */
    public void onCustomLevelSolved() {
        unlock("solve_custom_level");
    }
    
    /**
     * Called when a custom level is shared
     */
    public void onCustomLevelShared() {
        unlock("share_custom_level");
    }
    
    /**
     * Called on daily login
     */
    public void onDailyLogin(int streakDays) {
        dailyLoginStreak = streakDays;
        saveCounter("daily_login_streak", dailyLoginStreak);
        
        if (streakDays >= 7) unlock("daily_login_7");
        if (streakDays >= 30) unlock("daily_login_30");
    }
    
    /**
     * Called when player returns after inactivity
     */
    public void onComebackPlayer(int daysAway) {
        if (daysAway >= 30) {
            unlock("comeback_player");
        }
    }
    
    /**
     * Called when all squares are traversed
     */
    public void onAllSquaresTraversed(boolean byOneRobot, boolean byAllRobots) {
        if (byOneRobot) unlock("traverse_all_squares_1_robot");
        if (byAllRobots) unlock("traverse_all_squares_all_robots");
    }
    
    /**
     * Called when a game is played on a specific resolution
     */
    public void onResolutionPlayed(String resolution, int optimalMoves) {
        // Track resolutions played with specific move counts
        String key10 = "resolution_10_" + resolution;
        String key12 = "resolution_12_" + resolution;
        String key15 = "resolution_15_" + resolution;
        
        if (optimalMoves >= 10) {
            prefs.edit().putBoolean(key10, true).apply();
        }
        if (optimalMoves >= 12) {
            prefs.edit().putBoolean(key12, true).apply();
        }
        if (optimalMoves >= 15) {
            prefs.edit().putBoolean(key15, true).apply();
        }
        
        // Check if all resolutions are covered
        // This would need to know all available resolutions
        // For now, we'll check common ones
        checkResolutionAchievements();
    }
    
    private void checkResolutionAchievements() {
        // Common board sizes
        String[] resolutions = {"8x8", "10x10", "12x12", "14x14", "16x16"};
        
        boolean all10 = true, all12 = true, all15 = true;
        for (String res : resolutions) {
            if (!prefs.getBoolean("resolution_10_" + res, false)) all10 = false;
            if (!prefs.getBoolean("resolution_12_" + res, false)) all12 = false;
            if (!prefs.getBoolean("resolution_15_" + res, false)) all15 = false;
        }
        
        if (all10) unlock("play_10_move_games_all_resolutions");
        if (all12) unlock("play_12_move_games_all_resolutions");
        if (all15) unlock("play_15_move_games_all_resolutions");
    }
    
    /**
     * Reset all achievements (for testing)
     */
    public void resetAll() {
        prefs.edit().clear().apply();
        for (Achievement achievement : achievements.values()) {
            achievement.setUnlocked(false);
            achievement.setUnlockedTimestamp(0);
        }
        levelsCompleted = 0;
        perfectSolutions = 0;
        noHintLevels = 0;
        threeStarLevels = 0;
        impossibleModeGames = 0;
        impossibleModeStreak = 0;
        perfectRandomGames = 0;
        noHintRandomGames = 0;
        dailyLoginStreak = 0;
        speedrunRandomGamesUnder30s = 0;
        
        Timber.d("[ACHIEVEMENTS] All achievements reset");
    }
}

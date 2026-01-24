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
    private int threeStarLevels;
    private int threeStarHardLevels;
    private int impossibleModeGames;
    private int impossibleModeStreak;
    private int perfectRandomGames;
    private int noHintRandomGames;
    private int dailyLoginStreak;
    private int speedrunRandomGamesUnder30s;
    
    // Game session tracking - prevents achievements after game completion until new game starts
    private boolean gameCompleted = false;
    private boolean hintUsedInCurrentGame = false;
    
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
        threeStarLevels = prefs.getInt(KEY_COUNTER_PREFIX + "three_star_levels", 0);
        threeStarHardLevels = prefs.getInt(KEY_COUNTER_PREFIX + "three_star_hard_levels", 0);
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
        // Prevent achievements if game was already completed (robot moved off and back on target)
        if (gameCompleted) {
            Timber.d("[ACHIEVEMENTS] onLevelCompleted ignored - game already completed, waiting for new game");
            return;
        }
        
        // Mark game as completed - no more achievements until new game starts
        gameCompleted = true;
        
        // Log the levelId for debugging
        Timber.d("[ACHIEVEMENTS] onLevelCompleted called: levelId=%d, levelsCompleted=%d->%d, playerMoves=%d, optimalMoves=%d, hintsUsed=%d, stars=%d, time=%dms",
                levelId, levelsCompleted, levelsCompleted + 1, playerMoves, optimalMoves, hintsUsed, stars, timeMs);
        
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
            if (perfectSolutions >= 5) unlock("perfect_solutions_5");
            if (perfectSolutions >= 10) unlock("perfect_solutions_10");
            if (perfectSolutions >= 50) unlock("perfect_solutions_50");
        }
        
        // Note: no_hints_10 and no_hints_50 removed - hints are not allowed in levels
        
        // 3 stars achievements
        if (stars >= 3) {
            threeStarLevels++;
            saveCounter("three_star_levels", threeStarLevels);
            
            // 3_star_hard_level only unlocks for levels with 5+ optimal moves
            if (optimalMoves >= 5) {
                unlock("3_star_hard_level");
                threeStarHardLevels++;
                saveCounter("three_star_hard_levels", threeStarHardLevels);
                if (threeStarHardLevels >= 10) unlock("3_star_10_hard_levels");
            }
            
            // Other 3-star achievements count all levels regardless of move count
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
        // Prevent achievements if game was already completed (robot moved off and back on target)
        if (gameCompleted) {
            Timber.d("[ACHIEVEMENTS] onRandomGameCompleted ignored - game already completed, waiting for new game");
            return;
        }
        
        // Check if hint was used during this game session
        if (hintUsedInCurrentGame) {
            hintsUsed = Math.max(hintsUsed, 1); // Ensure hintsUsed reflects that a hint was used
            Timber.d("[ACHIEVEMENTS] Hint was used during this game session");
        }
        
        // Mark game as completed - no more achievements until new game starts
        gameCompleted = true;
        
        // First game
        onFirstGame();
        
        // Impossible mode - only count if optimal moves >= 17 (to prevent easy games via settings)
        if (isImpossibleMode && optimalMoves >= 17) {
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
            Timber.d("[ACHIEVEMENTS] Impossible mode game counted (optimalMoves=%d >= 17)", optimalMoves);
        } else if (isImpossibleMode) {
            Timber.d("[ACHIEVEMENTS] Impossible mode game NOT counted (optimalMoves=%d < 17)", optimalMoves);
        }
        
        // Solution length achievements (18-29 moves individually, 30+ as one)
        if (optimalMoves >= 18 && optimalMoves <= 29) {
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
        if (targetsNeeded == 4 && targetCount == 4) unlock("game_4_of_4_targets");
        
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
     * Called when a new game starts (level or random game).
     * Resets the game session tracking flags.
     */
    public void onNewGameStarted() {
        gameCompleted = false;
        hintUsedInCurrentGame = false;
        Timber.d("[ACHIEVEMENTS] New game started - session flags reset");
    }
    
    /**
     * For testing only: Disable the gameCompleted check so multiple achievements can be tested in sequence.
     * This should only be used in unit tests.
     */
    public void setTestMode(boolean enabled) {
        if (enabled) {
            gameCompleted = false;
        }
        Timber.d("[ACHIEVEMENTS] Test mode: gameCompleted check disabled");
    }
    
    /**
     * Called when the hint button is pressed during a game.
     * This tracks hint usage for the current game session.
     */
    public void onHintUsed() {
        hintUsedInCurrentGame = true;
        Timber.d("[ACHIEVEMENTS] Hint used in current game session");
    }
    
    /**
     * Check if a hint was used in the current game session.
     */
    public boolean wasHintUsedInCurrentGame() {
        return hintUsedInCurrentGame;
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
     * Called on daily login - only updates streak counter, doesn't unlock achievements
     * Achievements are unlocked when player starts a game (onNewGameStarted)
     */
    public void onDailyLogin(int streakDays) {
        dailyLoginStreak = streakDays;
        saveCounter("daily_login_streak", dailyLoginStreak);
        Timber.d("[ACHIEVEMENT] Daily login recorded - streak: %d days", streakDays);
    }
    
    /**
     * Called when player starts a new game - check and unlock streak achievements
     */
    public void checkAndUnlockStreakAchievements() {
        int streakDays = dailyLoginStreak;
        if (streakDays >= 7) unlock("daily_login_7");
        if (streakDays >= 30) unlock("daily_login_30");
        Timber.d("[ACHIEVEMENT] Checked streak achievements at game start - streak: %d days", streakDays);
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
     * Called when all squares are traversed.
     * 
     * 4 achievements:
     * - traverse_all_squares_1_robot: One robot visits all squares (after goal allowed)
     * - traverse_all_squares_1_robot_goal: One robot visits all squares, goal must be last
     * - traverse_all_squares_all_robots: All robots visit all squares (after goal allowed)
     * - traverse_all_squares_all_robots_goal: All robots visit all squares, goal must be last
     */
    public void onAllSquaresTraversed(boolean oneRobot, boolean oneRobotGoal, 
                                       boolean allRobots, boolean allRobotsGoal) {
        if (oneRobot) unlock("traverse_all_squares_1_robot");
        if (oneRobotGoal) unlock("traverse_all_squares_1_robot_goal");
        if (allRobots) unlock("traverse_all_squares_all_robots");
        if (allRobotsGoal) unlock("traverse_all_squares_all_robots_goal");
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
     * Unlock all achievements (for testing/debug)
     */
    public void unlockAll() {
        for (Achievement achievement : achievements.values()) {
            unlock(achievement.getId());
        }
        Timber.d("[ACHIEVEMENTS] All achievements unlocked");
    }
    
    /**
     * Lock an achievement by ID (for testing/debug)
     */
    public void lock(String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement != null) {
            achievement.setUnlocked(false);
            achievement.setUnlockedTimestamp(0);
            prefs.edit().putBoolean(achievementId, false).apply();
            Timber.d("[ACHIEVEMENTS] Achievement locked: %s", achievementId);
        }
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

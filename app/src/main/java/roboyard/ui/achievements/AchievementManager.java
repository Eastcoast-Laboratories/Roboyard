package roboyard.ui.achievements;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import roboyard.eclabs.BuildConfig;
import roboyard.ui.components.GameHistoryManager;
import roboyard.ui.components.PlayGamesManager;
import roboyard.ui.components.RoboyardApiClient;
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
    private WeakReference<Activity> currentActivity;
    
    // Counters for tracking progress
    private int levelsCompleted;
    private int perfectSolutions;
    private int threeStarLevels;
    private int threeStarHardLevels;
    private int impossibleModeGames;
    private int impossibleModeStreak;
    private int perfectRandomGames;
    private int perfectRandomGamesStreak;
    private int noHintRandomGames;
    private int noHintRandomGamesTotal;
    private int dailyLoginStreak;
    private int speedrunRandomGamesUnder30s = 0;
    private int sameWallsMaxPositions = 0;
    
    // Game session tracking
    private boolean hintUsedInCurrentGame = false;
    
    // Robot touch tracking for gimme_five achievement
    // Stores pairs of robots that have touched each other (e.g., "0-1" means robot 0 touched robot 1)
    private Set<String> robotTouchPairs = new HashSet<>();
    private int currentGameRobotCount = 0;
    
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

    public void setCurrentActivity(Activity activity) {
        this.currentActivity = new WeakReference<>(activity);
    }

    /**
     * Progress snapshot for a counter-based achievement.
     * current == required means the achievement can be / is unlocked.
     */
    public static class AchievementProgress {
        public final int current;
        public final int required;
        public AchievementProgress(int current, int required) {
            this.current = current;
            this.required = required;
        }
        public boolean hasProgress() { return required > 1; }
        public boolean isComplete() { return current >= required; }
    }

    /**
     * Returns progress for counter-based achievements, or null for binary achievements.
     * This is the central method for progress display and auto-unlock at 100%.
     */
    public AchievementProgress getProgress(String achievementId) {
        int uniqueCompletedLevels = getUniqueCompletedLevelCount();
        int uniqueThreeStarLevels = getUniqueThreeStarLevelCount();
        switch (achievementId) {
            // Level progression
            case "level_10_complete":  return new AchievementProgress(uniqueCompletedLevels, 10);
            case "level_50_complete":  return new AchievementProgress(uniqueCompletedLevels, 50);
            case "level_140_complete": return new AchievementProgress(uniqueCompletedLevels, 140);
            // Perfect solutions (levels)
            case "perfect_solutions_5":  return new AchievementProgress(perfectSolutions, 5);
            case "perfect_solutions_10": return new AchievementProgress(perfectSolutions, 10);
            case "perfect_solutions_50": return new AchievementProgress(perfectSolutions, 50);
            // Three-star levels
            case "3_star_10_levels":      return new AchievementProgress(uniqueThreeStarLevels, 10);
            case "3_star_50_levels":      return new AchievementProgress(uniqueThreeStarLevels, 50);
            case "3_star_all_levels":     return new AchievementProgress(uniqueThreeStarLevels, 140);
            case "3_star_10_hard_levels": return new AchievementProgress(threeStarHardLevels, 10);
            // Impossible mode
            case "impossible_mode_5":        return new AchievementProgress(impossibleModeGames, 5);
            case "impossible_mode_10":       return new AchievementProgress(impossibleModeGames, 10);
            case "impossible_mode_streak_5":  return new AchievementProgress(impossibleModeStreak, 5);
            case "impossible_mode_streak_10": return new AchievementProgress(impossibleModeStreak, 10);
            // Perfect random games (cumulative)
            case "perfect_random_games_5":  return new AchievementProgress(perfectRandomGames, 5);
            case "perfect_random_games_10": return new AchievementProgress(perfectRandomGames, 10);
            case "perfect_random_games_20": return new AchievementProgress(perfectRandomGames, 20);
            // Perfect random games streak
            case "perfect_random_games_streak_5":  return new AchievementProgress(perfectRandomGamesStreak, 5);
            case "perfect_random_games_streak_10": return new AchievementProgress(perfectRandomGamesStreak, 10);
            case "perfect_random_games_streak_20": return new AchievementProgress(perfectRandomGamesStreak, 20);
            // No-hints random games (cumulative)
            case "no_hints_random_10": return new AchievementProgress(noHintRandomGamesTotal, 10);
            case "no_hints_random_50": return new AchievementProgress(noHintRandomGamesTotal, 50);
            // No-hints streak
            case "no_hints_streak_random_10": return new AchievementProgress(noHintRandomGames, 10);
            case "no_hints_streak_random_50": return new AchievementProgress(noHintRandomGames, 50);
            // Daily login streak
            case "daily_login_7":  return new AchievementProgress(dailyLoginStreak, 7);
            case "daily_login_30": return new AchievementProgress(dailyLoginStreak, 30);
            // Speedrun streak
            case "speedrun_random_5_games_under_30s": return new AchievementProgress(speedrunRandomGamesUnder30s, 5);
            // Same-walls
            case "same_walls_2":  return new AchievementProgress(sameWallsMaxPositions, 2);
            case "same_walls_10":  return new AchievementProgress(sameWallsMaxPositions, 5);
            case "same_walls_100": return new AchievementProgress(sameWallsMaxPositions, 10);
            // Binary achievements (no progress tracking)
            default: return null;
        }
    }

    /**
     * Unlock an achievement if its counter-based progress is complete.
     * Uses getProgress() as the single source of truth for required thresholds.
     * @return true if newly unlocked
     */
    public boolean unlockIfComplete(String achievementId) {
        AchievementProgress progress = getProgress(achievementId);
        if (progress != null && progress.isComplete()) {
            return unlock(achievementId);
        }
        return false;
    }
    
    
    /**
     * Migrate orphaned SharedPreferences keys written by a previous bug in syncFromServer().
     * The bug wrote keys as "id_unlocked" / "id_timestamp" instead of "unlocked_id" / "timestamp_id".
     * This migrates those keys to the correct format and removes the orphaned ones.
     */
    private void migrateOrphanedSyncKeys() {
        boolean migrated = prefs.getBoolean("orphaned_keys_migrated", false);
        if (migrated) return;
        
        SharedPreferences.Editor editor = prefs.edit();
        int migratedCount = 0;
        
        for (String id : achievements.keySet()) {
            // Check for orphaned keys: id + "_unlocked" (wrong format)
            String wrongUnlockedKey = id + "_unlocked";
            String wrongTimestampKey = id + "_timestamp";
            String correctUnlockedKey = KEY_PREFIX_UNLOCKED + id;
            String correctTimestampKey = KEY_PREFIX_TIMESTAMP + id;
            
            if (prefs.contains(wrongUnlockedKey)) {
                boolean unlocked = prefs.getBoolean(wrongUnlockedKey, false);
                long timestamp = prefs.getLong(wrongTimestampKey, 0);
                
                // Only migrate if the correct key doesn't already have a value
                if (unlocked && !prefs.getBoolean(correctUnlockedKey, false)) {
                    editor.putBoolean(correctUnlockedKey, true);
                    editor.putLong(correctTimestampKey, timestamp);
                    migratedCount++;
                }
                
                // Remove orphaned keys
                editor.remove(wrongUnlockedKey);
                editor.remove(wrongTimestampKey);
            }
        }
        
        editor.putBoolean("orphaned_keys_migrated", true);
        editor.apply();
        
        if (migratedCount > 0) {
            Timber.d("[ACHIEVEMENTS] Migrated %d orphaned sync keys to correct format", migratedCount);
        }
    }
    
    private void loadState() {
        // One-time migration: fix orphaned keys from buggy syncFromServer() 
        // that wrote "id_unlocked" instead of "unlocked_id"
        migrateOrphanedSyncKeys();
        
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
        perfectRandomGamesStreak = prefs.getInt(KEY_COUNTER_PREFIX + "perfect_random_games_streak", 0);
        noHintRandomGames = prefs.getInt(KEY_COUNTER_PREFIX + "no_hint_random_games", 0);
        noHintRandomGamesTotal = prefs.getInt(KEY_COUNTER_PREFIX + "no_hint_random_games_total", 0);
        dailyLoginStreak = prefs.getInt(KEY_COUNTER_PREFIX + "daily_login_streak", 0);
        speedrunRandomGamesUnder30s = prefs.getInt(KEY_COUNTER_PREFIX + "speedrun_random_30s", 0);
        sameWallsMaxPositions = prefs.getInt(KEY_COUNTER_PREFIX + "same_walls_max_positions", 0);
        
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
        
        // Sync to Google Play Games if enabled
        syncToPlayGames(achievementId);
        
        // Sync to roboyard.z11.de server
        syncAfterUnlock();
        
        // Notify listener
        if (unlockListener != null) {
            unlockListener.onAchievementUnlocked(achievement);
        }
        
        return true;
    }
    
    /**
     * Map local achievement ID to Google Play Games achievement ID.
     * Shared between Play and F-Droid flavors.
     * Uses the mapping defined in AchievementDefinitions.
     * 
     * @param localId The local achievement ID
     * @return The Google Play Games achievement ID, or null if not found
     */
    public String getPlayGamesAchievementId(String localId) {
        try {
            String resourceKey = AchievementDefinitions.getPlayGamesResourceKey(localId);
            if (resourceKey == null) {
                Timber.w("[ACHIEVEMENTS] Unknown achievement ID: %s", localId);
                return null;
            }
            
            int resId = context.getResources().getIdentifier(resourceKey, "string", context.getPackageName());
            if (resId == 0) {
                Timber.w("[ACHIEVEMENTS] String resource not found: %s", resourceKey);
                return null;
            }
            
            return context.getString(resId);
        } catch (Exception e) {
            Timber.e(e, "[ACHIEVEMENTS] Failed to get Play Games ID for: %s", localId);
            return null;
        }
    }
    
    /**
     * Sync achievement unlock to Google Play Games Services.
     * Only works if ENABLE_PLAY_GAMES is true and user is signed in.
     */
    private void syncToPlayGames(String achievementId) {
        if (!BuildConfig.ENABLE_PLAY_GAMES) {
            return;
        }
        
        Activity activity = currentActivity != null ? currentActivity.get() : null;
        if (activity == null) {
            Timber.d("[ACHIEVEMENTS] Cannot sync to Play Games - no activity set");
            return;
        }
        
        try {
            PlayGamesManager playGames = PlayGamesManager.getInstance(context);
            playGames.unlockAchievement(activity, achievementId);
            Timber.d("[ACHIEVEMENTS] Synced to Play Games: %s", achievementId);
        } catch (Exception e) {
            Timber.e(e, "[ACHIEVEMENTS] Failed to sync to Play Games: %s", achievementId);
        }
    }
    
    public boolean isUnlocked(String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        return achievement != null && achievement.isUnlocked();
    }
    
    public List<Achievement> getAllAchievements() {
        return new ArrayList<>(achievements.values());
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
        
        // Log the levelId for debugging
        Timber.d("[ACHIEVEMENTS] onLevelCompleted called: levelId=%d, levelsCompleted=%d->%d, playerMoves=%d, optimalMoves=%d, hintsUsed=%d, stars=%d, time=%dms",
                levelId, levelsCompleted, levelsCompleted + 1, playerMoves, optimalMoves, hintsUsed, stars, timeMs);

        int uniqueCompletedLevelsBefore = getUniqueCompletedLevelCount();
        if (uniqueCompletedLevelsBefore <= levelsCompleted) {
            Timber.d("[ACHIEVEMENTS][LEVEL] Skipping duplicate level completion for levelId=%d (history count=%d, stored=%d)",
                    levelId, uniqueCompletedLevelsBefore, levelsCompleted);
            return;
        }
        
        // First game achievement (any game completion)
        unlock("first_game");
        
        levelsCompleted = uniqueCompletedLevelsBefore;
        saveCounter("levels_completed", levelsCompleted);
        
        // Level progression achievements
        if (levelId >= 1) unlock("level_1_complete");
        unlockIfComplete("level_10_complete");
        unlockIfComplete("level_50_complete");
        unlockIfComplete("level_140_complete");
        
        // Perfect solution (optimalMoves must be > 0, i.e. solver result available)
        if (optimalMoves > 0 && playerMoves == optimalMoves) {
            perfectSolutions++;
            saveCounter("perfect_solutions", perfectSolutions);
            Timber.d("[ACHIEVEMENTS][PERFECT] Level %d: perfect solution counted! total=%d (playerMoves=%d == optimalMoves=%d)",
                    levelId, perfectSolutions, playerMoves, optimalMoves);
            unlockIfComplete("perfect_solutions_5");
            unlockIfComplete("perfect_solutions_10");
            unlockIfComplete("perfect_solutions_50");
        } else if (optimalMoves <= 0) {
            Timber.w("[ACHIEVEMENTS][PERFECT] Level %d: optimalMoves=%d (solver not ready?), perfect solution NOT counted!",
                    levelId, optimalMoves);
        } else {
            Timber.d("[ACHIEVEMENTS][PERFECT] Level %d: not perfect (playerMoves=%d, optimalMoves=%d), total=%d",
                    levelId, playerMoves, optimalMoves, perfectSolutions);
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
                unlockIfComplete("3_star_10_hard_levels");
            }
            
            // Other 3-star achievements count all levels regardless of move count
            unlockIfComplete("3_star_10_levels");
            unlockIfComplete("3_star_50_levels");
            unlockIfComplete("3_star_all_levels");
        }
        
        // Speedrun
        if (timeMs < 30000) unlock("speedrun_under_30s");
        if (timeMs < 10000) unlock("speedrun_under_10s");
        
        Timber.d("[ACHIEVEMENTS] Level %d completed: moves=%d/%d, hints=%d, stars=%d, time=%dms",
            levelId, playerMoves, optimalMoves, hintsUsed, stars, timeMs);
    }
    
    
    /**
     * Called when a random game is completed
     * @param isFirstCompletion true if this is the first time this exact map is completed (unique map)
     * @param qualifiesForNoHints true if map qualifies for no-hints achievements (first solve was hint-free)
     * @param wallSignature wall-layout signature for same-walls tracking (may be null)
     */
    public void onRandomGameCompleted(int playerMoves, int optimalMoves, int hintsUsed, 
                                       long timeMs, boolean isImpossibleMode, int robotCount,
                                       int targetCount, int targetsNeeded,
                                       boolean isFirstCompletion, boolean qualifiesForNoHints,
                                       String wallSignature) {
        
        // Check if hint was used during this game session
        if (hintUsedInCurrentGame) {
            hintsUsed = Math.max(hintsUsed, 1); // Ensure hintsUsed reflects that a hint was used
            Timber.d("[ACHIEVEMENTS] Hint was used during this game session");
        }
        
        Timber.d("[ACHIEVEMENTS] onRandomGameCompleted: isFirstCompletion=%b, qualifiesForNoHints=%b, hintsUsed=%d",
                isFirstCompletion, qualifiesForNoHints, hintsUsed);
        
        // First game
        onFirstGame();
        
        // Impossible mode - only count if optimal moves >= 17 AND first completion (unique map)
        if (isImpossibleMode && optimalMoves >= 17 && isFirstCompletion) {
            impossibleModeGames++;
            saveCounter("impossible_mode_games", impossibleModeGames);
            unlock("impossible_mode_1");
            unlockIfComplete("impossible_mode_5");
            unlockIfComplete("impossible_mode_10");
            
            // Impossible mode streak (perfect solutions)
            if (playerMoves == optimalMoves) {
                impossibleModeStreak++;
                saveCounter("impossible_mode_streak", impossibleModeStreak);
                unlockIfComplete("impossible_mode_streak_5");
                unlockIfComplete("impossible_mode_streak_10");
            } else {
                impossibleModeStreak = 0;
                saveCounter("impossible_mode_streak", 0);
            }
            Timber.d("[ACHIEVEMENTS] Impossible mode game counted (optimalMoves=%d >= 17, isFirstCompletion=true)", optimalMoves);
        } else if (isImpossibleMode && optimalMoves >= 17 && !isFirstCompletion) {
            Timber.d("[ACHIEVEMENTS] Impossible mode game NOT counted - map already completed before");
        } else {
            Timber.d("[ACHIEVEMENTS] Impossible mode game NOT counted (optimalMoves=%d < 17), isImpossibleMode=%b", optimalMoves, isImpossibleMode);
        }
        
        // Solution length achievements (18-29 moves individually, 30+ as one)
        // Only unlock on FIRST completion AND without hint usage
        if (isFirstCompletion && hintsUsed == 0) {
            if (optimalMoves >= 18 && optimalMoves <= 29) {
                unlock("solution_" + optimalMoves + "_moves");
            } else if (optimalMoves >= 30) {
                unlock("solution_30_plus_moves");
            }
        } else if (!isFirstCompletion) {
            Timber.d("[ACHIEVEMENTS] Solution length achievements skipped - map already completed before");
        } else {
            Timber.d("[ACHIEVEMENTS] Solution length achievements skipped - hints were used (%d)", hintsUsed);
        }
        
        // Multiple targets
        if (targetCount >= 2) unlock("game_2_targets");
        if (targetCount >= 3) unlock("game_3_targets");
        if (targetCount >= 4) unlock("game_4_targets");
        
        // X of Y targets
        if (targetsNeeded == 2 && targetCount == 2) unlock("game_2_of_2_targets");
        if (targetsNeeded == 2 && targetCount == 3) unlock("game_2_of_3_targets");
        if (targetsNeeded == 2 && targetCount == 4) unlock("game_2_of_4_targets");
        if (targetsNeeded == 3 && targetCount == 3) unlock("game_3_of_3_targets");
        if (targetsNeeded == 3 && targetCount == 4) unlock("game_3_of_4_targets");
        if (targetsNeeded == 4 && targetCount == 4) unlock("game_4_of_4_targets");
        
        // Fun Challenges
        if (robotCount >= 5) unlock("game_5_robots");
        
        // Perfect random games - only count UNIQUE maps (first completion)
        if (playerMoves == optimalMoves && isFirstCompletion) {
            perfectRandomGames++;
            saveCounter("perfect_random_games", perfectRandomGames);
            unlockIfComplete("perfect_random_games_5");
            unlockIfComplete("perfect_random_games_10");
            unlockIfComplete("perfect_random_games_20");
            
            // Perfect random games streak (resets on non-optimal)
            perfectRandomGamesStreak++;
            saveCounter("perfect_random_games_streak", perfectRandomGamesStreak);
            unlockIfComplete("perfect_random_games_streak_5");
            unlockIfComplete("perfect_random_games_streak_10");
            unlockIfComplete("perfect_random_games_streak_20");
            Timber.d("[ACHIEVEMENTS] Perfect game on unique map - total: %d, streak: %d", perfectRandomGames, perfectRandomGamesStreak);
        } else if (playerMoves == optimalMoves && !isFirstCompletion) {
            Timber.d("[ACHIEVEMENTS] Perfect game NOT counted - map already completed before");
        } else {
            // Reset streak when non-optimal
            perfectRandomGamesStreak = 0;
            saveCounter("perfect_random_games_streak", perfectRandomGamesStreak);
            Timber.d("[ACHIEVEMENTS] Non-optimal game - perfect streak reset to 0");
        }
        
        // Perfect solution with no hints (10+ moves optimal) - only on FIRST completion
        if (playerMoves == optimalMoves && qualifiesForNoHints && optimalMoves >= 10 && isFirstCompletion) {
            unlock("perfect_no_hints_random_1");
            Timber.d("[ACHIEVEMENTS] Perfect no hints achievement unlocked - optimal: %d moves, qualifiesForNoHints=true", optimalMoves);
        }
        
        // No hints random games - only count UNIQUE maps with qualifiesForNoHints
        if (qualifiesForNoHints && isFirstCompletion) {
            // Cumulative counter (never resets)
            noHintRandomGamesTotal++;
            saveCounter("no_hint_random_games_total", noHintRandomGamesTotal);
            unlockIfComplete("no_hints_random_10");
            unlockIfComplete("no_hints_random_50");
            
            // Streak counter (resets on hint usage)
            noHintRandomGames++;
            saveCounter("no_hint_random_games", noHintRandomGames);
            unlockIfComplete("no_hints_streak_random_10");
            unlockIfComplete("no_hints_streak_random_50");
            Timber.d("[ACHIEVEMENTS] No hints on unique map - total: %d, streak: %d", noHintRandomGamesTotal, noHintRandomGames);
        } else if (!qualifiesForNoHints) {
            // Reset streak counter when hints were used (on this or previous completion)
            noHintRandomGames = 0;
            saveCounter("no_hint_random_games", noHintRandomGames);
            Timber.d("[ACHIEVEMENTS] Hints used - no_hint streak reset to 0 (total stays: %d)", noHintRandomGamesTotal);
        } else if (!isFirstCompletion) {
            Timber.d("[ACHIEVEMENTS] No hints NOT counted - map already completed before");
        }
        
        // Same-walls achievements: count unique position-signatures sharing the same wall layout
        if (wallSignature != null && !wallSignature.isEmpty() && currentActivity != null) {
            Activity act = currentActivity.get();
            if (act != null) {
                List<roboyard.logic.core.GameHistoryEntry> sameWallEntries =
                        roboyard.ui.components.GameHistoryManager.findByWallSignature(act, wallSignature);
                int uniquePositions = sameWallEntries.size(); // each entry = distinct positionSignature
                Timber.d("[ACHIEVEMENTS] same_walls: wallSig=%s uniquePositions=%d",
                        wallSignature.substring(0, Math.min(30, wallSignature.length())), uniquePositions);
                if (uniquePositions > sameWallsMaxPositions) {
                    sameWallsMaxPositions = uniquePositions;
                    saveCounter("same_walls_max_positions", sameWallsMaxPositions);
                }
                unlockIfComplete("same_walls_2");
                unlockIfComplete("same_walls_10");
                unlockIfComplete("same_walls_100");
            }
        }

        // Speed achievements
        if (timeMs < 20000) unlock("speedrun_random_under_20s");
        if (timeMs < 10000) unlock("speedrun_random_under_10s");
        if (timeMs < 30000) {
            speedrunRandomGamesUnder30s++;
            saveCounter("speedrun_random_30s", speedrunRandomGamesUnder30s);
            unlockIfComplete("speedrun_random_5_games_under_30s");
        }
        
        Timber.d("[ACHIEVEMENTS] Random game completed: moves=%d/%d, hints=%d, time=%dms, impossible=%s, robots=%d, targets=%d/%d",
            playerMoves, optimalMoves, hintsUsed, timeMs, isImpossibleMode, robotCount, targetsNeeded, targetCount);
    }
    
    /**
     * Track robot-to-robot collision for gimme_five achievement.
     * Called when a robot hits another robot (hit_robot sound plays).
     * 
     * @param movingRobotIndex The index of the robot that moved and hit another (0-4)
     * @param hitRobotIndex The index of the robot that was hit (0-4)
     * @param robotCount Total number of robots in the game
     */
    public void onRobotTouched(int movingRobotIndex, int hitRobotIndex, int robotCount) {
        if (movingRobotIndex < 0 || hitRobotIndex < 0 || 
            movingRobotIndex >= robotCount || hitRobotIndex >= robotCount || 
            robotCount < 2 || robotCount > 5) {
            return;
        }
        
        currentGameRobotCount = robotCount;
        
        // Store the touch pair (normalized so 0-1 and 1-0 are the same)
        int minIndex = Math.min(movingRobotIndex, hitRobotIndex);
        int maxIndex = Math.max(movingRobotIndex, hitRobotIndex);
        String touchPair = minIndex + "-" + maxIndex;
        
        boolean isNewTouch = robotTouchPairs.add(touchPair);
        if (isNewTouch) {
            Timber.d("[ACHIEVEMENTS] Robot %d touched robot %d (pair: %s)", movingRobotIndex, hitRobotIndex, touchPair);
        }
        
        // Check if all robots have touched each other
        // For n robots, we need n*(n-1)/2 unique pairs
        int requiredPairs = (robotCount * (robotCount - 1)) / 2;
        
        if (robotTouchPairs.size() >= requiredPairs) {
            unlock("gimme_five");
            Timber.d("[ACHIEVEMENTS] All %d robots have touched each other (%d pairs) - gimme_five unlocked!", 
                    robotCount, robotTouchPairs.size());
        } else {
            Timber.d("[ACHIEVEMENTS] Robot touch progress: %d/%d pairs", robotTouchPairs.size(), requiredPairs);
        }
    }
    
    /**
     * Get current robot touch progress for debugging/UI.
     * @return Array with [currentPairs, requiredPairs]
     */
    public int[] getRobotTouchProgress() {
        int requiredPairs = (currentGameRobotCount * (currentGameRobotCount - 1)) / 2;
        return new int[] { robotTouchPairs.size(), requiredPairs };
    }
    
    /**
     * Reset robot touch tracking for a new game.
     */
    private void resetRobotTouchTracking() {
        robotTouchPairs.clear();
        currentGameRobotCount = 0;
    }
    
    /**
     * Called when a new game starts (level or random game).
     * Resets the game session tracking flags and checks daily login achievements.
     */
    public void onNewGameStarted() {
        hintUsedInCurrentGame = false;
        resetRobotTouchTracking();
        
        // Check daily login achievements
        unlockIfComplete("daily_login_7");
        unlockIfComplete("daily_login_30");
        
        Timber.d("[ACHIEVEMENTS] New game started - session flags reset");
    }
    
    /**
     * For testing only This should only be used in unit tests.
     */
    public void setTestMode(boolean enabled) {
        Timber.d("[ACHIEVEMENTS] Test mode enabled");
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
     * Called when player starts a new game - check and unlock login streak achievements
     */
    public void checkAndUnlockStreakAchievements() {
        int streakDays = dailyLoginStreak;
        unlockIfComplete("daily_login_7");
        unlockIfComplete("daily_login_30");
        Timber.d("[ACHIEVEMENT] Checked Login Streak achievements at game start - streak: %d days", streakDays);
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
        perfectRandomGamesStreak = 0;
        noHintRandomGames = 0;
        noHintRandomGamesTotal = 0;
        dailyLoginStreak = 0;
        speedrunRandomGamesUnder30s = 0;
        sameWallsMaxPositions = 0;
    }

    private int getUniqueCompletedLevelCount() {
        Activity activity = currentActivity != null ? currentActivity.get() : null;
        if (activity == null) {
            return levelsCompleted;
        }
        return GameHistoryManager.getUniqueCompletedLevelCount(activity);
    }

    private int getUniqueThreeStarLevelCount() {
        Activity activity = currentActivity != null ? currentActivity.get() : null;
        if (activity == null) {
            return threeStarLevels;
        }
        return GameHistoryManager.getUniqueThreeStarLevelCount(activity);
    }
    
    /**
     * Sync achievement unlock to the server after unlock.
     * No-op if user is not authenticated.
     */
    // ========== SERVER SYNC ==========
    
    /**
     * Sync all achievements to roboyard.z11.de server.
     * Only syncs if user is logged in.
     */
    public void syncToServer() {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(context);
        if (!apiClient.isLoggedIn()) {
            Timber.d("[ACHIEVEMENT_SYNC] Not logged in, skipping sync");
            return;
        }
        
        try {
            // Build achievements array
            JSONArray achievementsArray = new JSONArray();
            for (Achievement achievement : achievements.values()) {
                JSONObject achievementJson = new JSONObject();
                achievementJson.put("id", achievement.getId());
                achievementJson.put("unlocked", achievement.isUnlocked());
                achievementJson.put("unlocked_timestamp", achievement.getUnlockedTimestamp());
                achievementsArray.put(achievementJson);
            }
            
            // Build stats object
            JSONObject stats = new JSONObject();
            stats.put("total_games_solved", levelsCompleted + perfectRandomGames);
            stats.put("total_games_solved_no_hints", noHintRandomGamesTotal);
            stats.put("total_perfect_solutions", perfectSolutions + perfectRandomGames);
            
            // Include streak data for bidirectional sync
            StreakManager streakManager = StreakManager.getInstance(context);
            stats.put("daily_login_streak", streakManager.getCurrentStreak());
            stats.put("last_streak_date", streakManager.getLastLoginDateString());
            stats.put("longest_streak", streakManager.getLongestStreak());
            stats.put("longest_streak_date", streakManager.getLongestStreakDate());
            
            // Send to server
            apiClient.syncAchievements(achievementsArray, stats, new RoboyardApiClient.ApiCallback<RoboyardApiClient.AchievementSyncResult>() {
                @Override
                public void onSuccess(RoboyardApiClient.AchievementSyncResult result) {
                    Timber.d("[ACHIEVEMENT_SYNC] Sync successful: %d synced, %d new achievements", 
                            result.syncedCount, result.newAchievements);
                }
                
                @Override
                public void onError(String error) {
                    Timber.e("[ACHIEVEMENT_SYNC] Sync failed: %s", error);
                }
            });
            
        } catch (JSONException e) {
            Timber.e(e, "[ACHIEVEMENT_SYNC] Failed to build sync request");
        }
    }
    
    /**
     * Sync achievements to server after unlocking.
     * Called automatically when an achievement is unlocked.
     */
    private void syncAfterUnlock() {
        // Delay sync slightly to batch multiple unlocks
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::syncToServer, 2000);
    }
    
    /**
     * Sync achievements FROM server to local device.
     * Used after login to restore achievements on a new device.
     * Merges server achievements with local ones (union - never removes local achievements).
     * 
     * @param callback Optional callback for sync result
     */
    public void syncFromServer(RoboyardApiClient.ApiCallback<Integer> callback) {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(context);
        if (!apiClient.isLoggedIn()) {
            Timber.d("[ACHIEVEMENT_SYNC_DOWN] Not logged in, skipping download");
            if (callback != null) callback.onError("Not logged in");
            return;
        }
        
        Timber.d("[ACHIEVEMENT_SYNC_DOWN] Starting achievement download from server");
        
        apiClient.fetchAchievements(new RoboyardApiClient.ApiCallback<RoboyardApiClient.AchievementFetchResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.AchievementFetchResult result) {
                int restoredCount = 0;
                
                try {
                    JSONArray serverAchievements = result.achievements;
                    Timber.d("[ACHIEVEMENT_SYNC_DOWN] Received %d achievements from server", serverAchievements.length());
                    
                    for (int i = 0; i < serverAchievements.length(); i++) {
                        JSONObject serverAchievement = serverAchievements.getJSONObject(i);
                        String id = serverAchievement.getString("id");
                        boolean unlocked = serverAchievement.optBoolean("unlocked", false);
                        String unlockedAt = serverAchievement.optString("unlocked_at", null);
                        
                        if (!unlocked) continue;
                        
                        // Check if we have this achievement locally
                        Achievement localAchievement = achievements.get(id);
                        if (localAchievement == null) {
                            Timber.d("[ACHIEVEMENT_SYNC_DOWN] Unknown achievement ID from server: %s", id);
                            continue;
                        }
                        
                        // Only restore if not already unlocked locally
                        if (!localAchievement.isUnlocked()) {
                            long timestamp = 0;
                            if (unlockedAt != null && !unlockedAt.isEmpty()) {
                                try {
                                    // Parse ISO 8601 timestamp
                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
                                    java.util.Date date = sdf.parse(unlockedAt);
                                    if (date != null) {
                                        timestamp = date.getTime();
                                    }
                                } catch (Exception e) {
                                    Timber.w("[ACHIEVEMENT_SYNC_DOWN] Could not parse timestamp for %s: %s", id, unlockedAt);
                                    timestamp = System.currentTimeMillis();
                                }
                            }
                            
                            localAchievement.setUnlocked(true);
                            localAchievement.setUnlockedTimestamp(timestamp > 0 ? timestamp : System.currentTimeMillis());
                            
                            // Save to SharedPreferences
                            prefs.edit()
                                .putBoolean(KEY_PREFIX_UNLOCKED + id, true)
                                .putLong(KEY_PREFIX_TIMESTAMP + id, localAchievement.getUnlockedTimestamp())
                                .apply();
                            
                            restoredCount++;
                            Timber.d("[ACHIEVEMENT_SYNC_DOWN] Restored achievement: %s", id);
                        }
                    }
                    
                    Timber.d("[ACHIEVEMENT_SYNC_DOWN] Download complete: %d achievements restored", restoredCount);
                    
                    // Restore streak data from server (bidirectional)
                    if (result.stats != null) {
                        int serverStreak = result.stats.optInt("daily_login_streak", 0);
                        String serverLastDate = result.stats.optString("last_streak_date", null);
                        StreakManager.getInstance(context).restoreFromServer(serverStreak, serverLastDate);
                    }
                    
                } catch (JSONException e) {
                    Timber.e(e, "[ACHIEVEMENT_SYNC_DOWN] Error parsing server achievements");
                    if (callback != null) callback.onError("Error parsing achievements: " + e.getMessage());
                    return;
                }
                
                if (callback != null) callback.onSuccess(restoredCount);
            }
            
            @Override
            public void onError(String error) {
                Timber.e("[ACHIEVEMENT_SYNC_DOWN] Download failed: %s", error);
                if (callback != null) callback.onError(error);
            }
        });
    }
}

package roboyard.eclabs.achievements;

import java.util.LinkedHashMap;
import java.util.Map;

import roboyard.eclabs.R;

/**
 * Defines all achievements in the game.
 */
public class AchievementDefinitions {
    
    private static Map<String, Achievement> achievements;
    
    public static Map<String, Achievement> getAll() {
        if (achievements == null) {
            achievements = new LinkedHashMap<>();
            initializeAchievements();
        }
        return achievements;
    }
    
    private static void add(Achievement achievement) {
        achievements.put(achievement.getId(), achievement);
    }
    
    private static void initializeAchievements() {
        // ========== PROGRESSION ACHIEVEMENTS ==========
        add(new Achievement("level_1_complete", 
            "achievement_level_1_complete", "achievement_level_1_complete_desc",
            AchievementCategory.PROGRESSION, R.drawable.ic_achievement_level));
        add(new Achievement("level_10_complete", 
            "achievement_level_10_complete", "achievement_level_10_complete_desc",
            AchievementCategory.PROGRESSION, R.drawable.ic_achievement_level));
        add(new Achievement("level_50_complete", 
            "achievement_level_50_complete", "achievement_level_50_complete_desc",
            AchievementCategory.PROGRESSION, R.drawable.ic_achievement_level));
        add(new Achievement("level_140_complete", 
            "achievement_level_140_complete", "achievement_level_140_complete_desc",
            AchievementCategory.PROGRESSION, R.drawable.ic_achievement_platinum));
        add(new Achievement("all_stars_collected", 
            "achievement_all_stars", "achievement_all_stars_desc",
            AchievementCategory.PROGRESSION, R.drawable.ic_achievement_star));
            
        // ========== PERFORMANCE ACHIEVEMENTS ==========
        add(new Achievement("perfect_solution_1", 
            "achievement_perfect_1", "achievement_perfect_1_desc",
            AchievementCategory.PERFORMANCE, R.drawable.ic_achievement_perfect));
        add(new Achievement("perfect_solutions_10", 
            "achievement_perfect_10", "achievement_perfect_10_desc",
            AchievementCategory.PERFORMANCE, R.drawable.ic_achievement_perfect));
        add(new Achievement("perfect_solutions_50", 
            "achievement_perfect_50", "achievement_perfect_50_desc",
            AchievementCategory.PERFORMANCE, R.drawable.ic_achievement_perfect));
        add(new Achievement("speedrun_under_30s", 
            "achievement_speedrun_30s", "achievement_speedrun_30s_desc",
            AchievementCategory.PERFORMANCE, R.drawable.ic_achievement_speed));
        add(new Achievement("speedrun_under_10s", 
            "achievement_speedrun_10s", "achievement_speedrun_10s_desc",
            AchievementCategory.PERFORMANCE, R.drawable.ic_achievement_speed));
            
        // ========== CHALLENGE ACHIEVEMENTS ==========
        add(new Achievement("no_hints_10", 
            "achievement_no_hints_10", "achievement_no_hints_10_desc",
            AchievementCategory.CHALLENGE, R.drawable.ic_achievement_challenge));
        add(new Achievement("no_hints_50", 
            "achievement_no_hints_50", "achievement_no_hints_50_desc",
            AchievementCategory.CHALLENGE, R.drawable.ic_achievement_challenge));
        add(new Achievement("solve_custom_level", 
            "achievement_solve_custom", "achievement_solve_custom_desc",
            AchievementCategory.CHALLENGE, R.drawable.ic_achievement_custom));
        add(new Achievement("create_custom_level", 
            "achievement_create_custom", "achievement_create_custom_desc",
            AchievementCategory.CHALLENGE, R.drawable.ic_achievement_custom));
        add(new Achievement("share_custom_level", 
            "achievement_share_custom", "achievement_share_custom_desc",
            AchievementCategory.CHALLENGE, R.drawable.ic_achievement_share));
            
        // ========== MASTERY ACHIEVEMENTS ==========
        add(new Achievement("3_star_level", 
            "achievement_3_star_1", "achievement_3_star_1_desc",
            AchievementCategory.MASTERY, R.drawable.ic_achievement_star));
        add(new Achievement("3_star_10_levels", 
            "achievement_3_star_10", "achievement_3_star_10_desc",
            AchievementCategory.MASTERY, R.drawable.ic_achievement_star));
        add(new Achievement("3_star_50_levels", 
            "achievement_3_star_50", "achievement_3_star_50_desc",
            AchievementCategory.MASTERY, R.drawable.ic_achievement_star));
        add(new Achievement("3_star_all_levels", 
            "achievement_3_star_all", "achievement_3_star_all_desc",
            AchievementCategory.MASTERY, R.drawable.ic_achievement_gold));
            
        // ========== SPECIAL ACHIEVEMENTS ==========
        add(new Achievement("first_game", 
            "achievement_first_game", "achievement_first_game_desc",
            AchievementCategory.SPECIAL, R.drawable.ic_achievement_first));
        add(new Achievement("daily_login_7", 
            "achievement_streak_7", "achievement_streak_7_desc",
            AchievementCategory.SPECIAL, R.drawable.ic_achievement_streak));
        add(new Achievement("daily_login_30", 
            "achievement_streak_30", "achievement_streak_30_desc",
            AchievementCategory.SPECIAL, R.drawable.ic_achievement_streak));
        add(new Achievement("comeback_player", 
            "achievement_comeback", "achievement_comeback_desc",
            AchievementCategory.SPECIAL, R.drawable.ic_achievement_comeback));
            
        // ========== RANDOM GAME - DIFFICULTY ==========
        add(new Achievement("impossible_mode_1", 
            "achievement_impossible_1", "achievement_impossible_1_desc",
            AchievementCategory.RANDOM_DIFFICULTY, R.drawable.ic_achievement_impossible));
        add(new Achievement("impossible_mode_5", 
            "achievement_impossible_5", "achievement_impossible_5_desc",
            AchievementCategory.RANDOM_DIFFICULTY, R.drawable.ic_achievement_impossible));
        add(new Achievement("impossible_mode_streak_5", 
            "achievement_impossible_streak_5", "achievement_impossible_streak_5_desc",
            AchievementCategory.RANDOM_DIFFICULTY, R.drawable.ic_achievement_impossible));
        add(new Achievement("impossible_mode_streak_10", 
            "achievement_impossible_streak_10", "achievement_impossible_streak_10_desc",
            AchievementCategory.RANDOM_DIFFICULTY, R.drawable.ic_achievement_impossible));
            
        // ========== RANDOM GAME - SOLUTION LENGTH ==========
        for (int moves = 20; moves <= 29; moves++) {
            add(new Achievement("solution_" + moves + "_moves", 
                "achievement_solution_" + moves, "achievement_solution_" + moves + "_desc",
                AchievementCategory.RANDOM_SOLUTION, R.drawable.ic_achievement_solution));
        }
        add(new Achievement("solution_30_plus_moves", 
            "achievement_solution_30_plus", "achievement_solution_30_plus_desc",
            AchievementCategory.RANDOM_SOLUTION, R.drawable.ic_achievement_solution));
            
        // ========== RANDOM GAME - SCREEN RESOLUTIONS ==========
        add(new Achievement("play_10_move_games_all_resolutions", 
            "achievement_resolution_10", "achievement_resolution_10_desc",
            AchievementCategory.RANDOM_RESOLUTION, R.drawable.ic_achievement_resolution));
        add(new Achievement("play_12_move_games_all_resolutions", 
            "achievement_resolution_12", "achievement_resolution_12_desc",
            AchievementCategory.RANDOM_RESOLUTION, R.drawable.ic_achievement_resolution));
        add(new Achievement("play_15_move_games_all_resolutions", 
            "achievement_resolution_15", "achievement_resolution_15_desc",
            AchievementCategory.RANDOM_RESOLUTION, R.drawable.ic_achievement_resolution));
            
        // ========== RANDOM GAME - MULTIPLE TARGETS ==========
        add(new Achievement("game_2_targets", 
            "achievement_2_targets", "achievement_2_targets_desc",
            AchievementCategory.RANDOM_TARGETS, R.drawable.ic_achievement_target));
        add(new Achievement("game_3_targets", 
            "achievement_3_targets", "achievement_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, R.drawable.ic_achievement_target));
        add(new Achievement("game_4_targets", 
            "achievement_4_targets", "achievement_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, R.drawable.ic_achievement_target));
        add(new Achievement("game_2_of_3_targets", 
            "achievement_2_of_3_targets", "achievement_2_of_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, R.drawable.ic_achievement_target));
        add(new Achievement("game_2_of_4_targets", 
            "achievement_2_of_4_targets", "achievement_2_of_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, R.drawable.ic_achievement_target));
        add(new Achievement("game_3_of_4_targets", 
            "achievement_3_of_4_targets", "achievement_3_of_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, R.drawable.ic_achievement_target));
            
        // ========== RANDOM GAME - ROBOT COUNT ==========
        add(new Achievement("game_5_robots", 
            "achievement_5_robots", "achievement_5_robots_desc",
            AchievementCategory.RANDOM_ROBOTS, R.drawable.ic_achievement_robot));
            
        // ========== RANDOM GAME - SQUARE COVERAGE ==========
        add(new Achievement("traverse_all_squares_1_robot", 
            "achievement_traverse_1_robot", "achievement_traverse_1_robot_desc",
            AchievementCategory.RANDOM_COVERAGE, R.drawable.ic_achievement_coverage));
        add(new Achievement("traverse_all_squares_all_robots", 
            "achievement_traverse_all_robots", "achievement_traverse_all_robots_desc",
            AchievementCategory.RANDOM_COVERAGE, R.drawable.ic_achievement_coverage));
            
        // ========== RANDOM GAME - STREAKS & CHALLENGES ==========
        add(new Achievement("perfect_random_games_5", 
            "achievement_perfect_random_5", "achievement_perfect_random_5_desc",
            AchievementCategory.RANDOM_STREAKS, R.drawable.ic_achievement_streak));
        add(new Achievement("perfect_random_games_10", 
            "achievement_perfect_random_10", "achievement_perfect_random_10_desc",
            AchievementCategory.RANDOM_STREAKS, R.drawable.ic_achievement_streak));
        add(new Achievement("perfect_random_games_20", 
            "achievement_perfect_random_20", "achievement_perfect_random_20_desc",
            AchievementCategory.RANDOM_STREAKS, R.drawable.ic_achievement_streak));
        add(new Achievement("no_hints_random_10", 
            "achievement_no_hints_random_10", "achievement_no_hints_random_10_desc",
            AchievementCategory.RANDOM_STREAKS, R.drawable.ic_achievement_challenge));
        add(new Achievement("no_hints_random_50", 
            "achievement_no_hints_random_50", "achievement_no_hints_random_50_desc",
            AchievementCategory.RANDOM_STREAKS, R.drawable.ic_achievement_challenge));
            
        // ========== RANDOM GAME - SPEED ==========
        add(new Achievement("speedrun_random_under_20s", 
            "achievement_speedrun_random_20s", "achievement_speedrun_random_20s_desc",
            AchievementCategory.RANDOM_SPEED, R.drawable.ic_achievement_speed));
        add(new Achievement("speedrun_random_under_10s", 
            "achievement_speedrun_random_10s", "achievement_speedrun_random_10s_desc",
            AchievementCategory.RANDOM_SPEED, R.drawable.ic_achievement_speed));
        add(new Achievement("speedrun_random_5_games_under_30s", 
            "achievement_speedrun_random_5x30s", "achievement_speedrun_random_5x30s_desc",
            AchievementCategory.RANDOM_SPEED, R.drawable.ic_achievement_speed));
    }
}

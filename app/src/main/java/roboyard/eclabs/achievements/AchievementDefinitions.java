package roboyard.eclabs.achievements;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines all achievements in the game.
 * Each achievement uses individual drawable resources from achievements_icons_cropped folder.
 * Icons are named: {number}_{icon_name}.png (e.g., 1_lightning.png, 46_flame.png)
 */
public class AchievementDefinitions {
    
    // Icon resource names for different achievement types
    private static final String ICON_TROPHY_STAR = "icon_9_trophy";           // Trophy with star - progression
    private static final String ICON_MEDAL = "icon_10_gear";                  // Medal - level completion
    private static final String ICON_CHART_UP = "icon_11_chart";              // Chart going up - progress
    private static final String ICON_TROPHY_GOLD = "icon_43_trophy_gold";     // Gold trophy - mastery
    private static final String ICON_STAR = "icon_27_star";                   // Star - stars collected
    private static final String ICON_STARS = "icon_48_stars_gold";            // Multiple stars - all stars
    private static final String ICON_CROWN = "icon_41_crown";                 // Crown - master achievement
    private static final String ICON_DIAMOND_CUP = "icon_45_diamond_blue";    // Diamond cup - platinum
    private static final String ICON_CHECKLIST = "icon_14_checkmark";         // Checklist - completion
    private static final String ICON_SPEEDOMETER = "icon_16_compass";         // Speedometer - speed
    private static final String ICON_LIGHTNING = "icon_1_lightning";          // Lightning - fast
    private static final String ICON_FLAME = "icon_46_flame";                 // Flame - streak/hot
    private static final String ICON_SHIELD_STAR = "icon_27_star";            // Shield with star - perfect
    private static final String ICON_SHIELD_RED = "icon_26_shield_red";       // Shield red - challenge
    private static final String ICON_BRAIN = "icon_29_brain_green";           // Brain - thinking/no hints
    private static final String ICON_LIGHTBULB = "icon_15_lightbulb";         // Lightbulb - idea/custom
    private static final String ICON_TOOLS = "icon_18_wrench";                // Tools - create
    private static final String ICON_NETWORK = "icon_52_network";             // Network - share
    private static final String ICON_HEART_GEAR = "icon_2_heart";             // Heart gear - first game
    private static final String ICON_INFINITY = "icon_32_infinity";           // Infinity - streak
    private static final String ICON_ROBOT = "icon_3_robot";                  // Robot - robot related
    private static final String ICON_ROBOT_YELLOW = "icon_49_armor";          // Yellow robot - 5 robots
    private static final String ICON_TARGET = "icon_8_target";                // Target - targets
    private static final String ICON_TARGET_BLUE = "icon_33_target_blue";     // Blue target - multi targets
    private static final String ICON_PUZZLE = "icon_24_robot_gold";           // Puzzle - solution
    private static final String ICON_TABLET = "icon_35_door";                 // Tablet - resolution
    private static final String ICON_MAP = "icon_36_ring";                    // Map - coverage
    private static final String ICON_SATELLITE = "icon_40_planet";            // Satellite - explorer
    private static final String ICON_SPARKLES = "icon_39_stars";              // Sparkles - special
    private static final String ICON_WREATH = "icon_42_laurel";               // Wreath - achievement
    private static final String ICON_TROPHY_MEDAL = "icon_43_trophy_gold";    // Trophy with medal
    private static final String ICON_POWER = "icon_4_power";                  // Power button - comeback
    private static final String ICON_HOURGLASS = "icon_13_hourglass";         // Hourglass - time
    private static final String ICON_BUILDINGS = "icon_12_bars";              // Buildings - levels
    
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
        add(new Achievement("first_game", 
            "achievement_first_game", "achievement_first_game_desc",
            AchievementCategory.PROGRESSION, ICON_HEART_GEAR));
        add(new Achievement("level_1_complete", 
            "achievement_level_1_complete", "achievement_level_1_complete_desc",
            AchievementCategory.PROGRESSION, ICON_MEDAL));
        add(new Achievement("level_10_complete", 
            "achievement_level_10_complete", "achievement_level_10_complete_desc",
            AchievementCategory.PROGRESSION, ICON_CHART_UP));
        add(new Achievement("level_50_complete", 
            "achievement_level_50_complete", "achievement_level_50_complete_desc",
            AchievementCategory.PROGRESSION, ICON_BUILDINGS));
        add(new Achievement("level_140_complete", 
            "achievement_level_140_complete", "achievement_level_140_complete_desc",
            AchievementCategory.PROGRESSION, ICON_DIAMOND_CUP));
        add(new Achievement("all_stars_collected", 
            "achievement_all_stars", "achievement_all_stars_desc",
            AchievementCategory.PROGRESSION, ICON_STARS));
            
        // ========== PERFORMANCE ACHIEVEMENTS ==========
        add(new Achievement("perfect_solutions_5", 
            "achievement_perfect_5", "achievement_perfect_5_desc",
            AchievementCategory.PERFORMANCE, ICON_SHIELD_STAR));
        add(new Achievement("perfect_solutions_10", 
            "achievement_perfect_10", "achievement_perfect_10_desc",
            AchievementCategory.PERFORMANCE, ICON_TROPHY_STAR));
        add(new Achievement("perfect_solutions_50", 
            "achievement_perfect_50", "achievement_perfect_50_desc",
            AchievementCategory.PERFORMANCE, ICON_CROWN));
        add(new Achievement("speedrun_under_30s", 
            "achievement_speedrun_30s", "achievement_speedrun_30s_desc",
            AchievementCategory.PERFORMANCE, ICON_SPEEDOMETER));
        add(new Achievement("speedrun_under_10s", 
            "achievement_speedrun_10s", "achievement_speedrun_10s_desc",
            AchievementCategory.PERFORMANCE, ICON_LIGHTNING));
            
        // ========== CHALLENGE ACHIEVEMENTS ==========
        // no_hints_10 and no_hints_50 removed - hints are not allowed in levels
        // Custom level achievements - not yet implemented, custom levels feature pending
        // add(new Achievement("solve_custom_level", 
        //     "achievement_solve_custom", "achievement_solve_custom_desc",
        //     AchievementCategory.CHALLENGE, ICON_LIGHTBULB));
        // add(new Achievement("create_custom_level", 
        //     "achievement_create_custom", "achievement_create_custom_desc",
        //     AchievementCategory.CHALLENGE, ICON_TOOLS));
        // add(new Achievement("share_custom_level", 
        //     "achievement_share_custom", "achievement_share_custom_desc",
        //     AchievementCategory.CHALLENGE, ICON_NETWORK));
            
        // ========== MASTERY ACHIEVEMENTS ==========
        add(new Achievement("3_star_hard_level", 
            "achievement_3_star_1", "achievement_3_star_1_desc",
            AchievementCategory.MASTERY, ICON_STAR));
        add(new Achievement("3_star_10_levels", 
            "achievement_3_star_10", "achievement_3_star_10_desc",
            AchievementCategory.MASTERY, ICON_STARS));
        add(new Achievement("3_star_10_hard_levels", 
            "achievement_3_star_10_hard", "achievement_3_star_10_hard_desc",
            AchievementCategory.MASTERY, ICON_STARS));
        add(new Achievement("3_star_50_levels", 
            "achievement_3_star_50", "achievement_3_star_50_desc",
            AchievementCategory.MASTERY, ICON_WREATH));
        add(new Achievement("3_star_all_levels", 
            "achievement_3_star_all", "achievement_3_star_all_desc",
            AchievementCategory.MASTERY, ICON_TROPHY_GOLD));
            
        // ========== SPECIAL ACHIEVEMENTS ==========
        add(new Achievement("daily_login_7", 
            "achievement_streak_7", "achievement_streak_7_desc",
            AchievementCategory.SPECIAL, ICON_FLAME));
        add(new Achievement("daily_login_30", 
            "achievement_streak_30", "achievement_streak_30_desc",
            AchievementCategory.SPECIAL, ICON_INFINITY));
        add(new Achievement("comeback_player", 
            "achievement_comeback", "achievement_comeback_desc",
            AchievementCategory.SPECIAL, ICON_POWER));
            
        // ========== RANDOM GAME - DIFFICULTY ==========
        add(new Achievement("impossible_mode_1", 
            "achievement_impossible_1", "achievement_impossible_1_desc",
            AchievementCategory.RANDOM_DIFFICULTY, ICON_SHIELD_RED));
        add(new Achievement("impossible_mode_5", 
            "achievement_impossible_5", "achievement_impossible_5_desc",
            AchievementCategory.RANDOM_DIFFICULTY, ICON_FLAME));
        add(new Achievement("impossible_mode_streak_5", 
            "achievement_impossible_streak_5", "achievement_impossible_streak_5_desc",
            AchievementCategory.RANDOM_DIFFICULTY, ICON_TROPHY_STAR));
        add(new Achievement("impossible_mode_streak_10", 
            "achievement_impossible_streak_10", "achievement_impossible_streak_10_desc",
            AchievementCategory.RANDOM_DIFFICULTY, ICON_CROWN));
            
        // ========== RANDOM GAME - SOLUTION LENGTH ==========
        // Use different icons for different move counts (18-29)
        String[] solutionIcons = {ICON_CHART_UP, ICON_BUILDINGS,  // 18, 19
                                  ICON_PUZZLE, ICON_CHECKLIST, ICON_CHART_UP, ICON_BUILDINGS, // 20-23
                                  ICON_TROPHY_MEDAL, ICON_TROPHY_STAR, ICON_WREATH, ICON_TROPHY_GOLD, // 24-27
                                  ICON_DIAMOND_CUP, ICON_CROWN}; // 28, 29
        for (int moves = 18; moves <= 29; moves++) {
            add(new Achievement("solution_" + moves + "_moves", 
                "achievement_solution_" + moves, "achievement_solution_" + moves + "_desc",
                AchievementCategory.RANDOM_SOLUTION, solutionIcons[moves - 18]));
        }
        add(new Achievement("solution_30_plus_moves", 
            "achievement_solution_30_plus", "achievement_solution_30_plus_desc",
            AchievementCategory.RANDOM_SOLUTION, ICON_SPARKLES));
            
        // ========== RANDOM GAME - SCREEN RESOLUTIONS ==========
        add(new Achievement("play_10_move_games_all_resolutions", 
            "achievement_resolution_10", "achievement_resolution_10_desc",
            AchievementCategory.RANDOM_RESOLUTION, ICON_TABLET));
        add(new Achievement("play_12_move_games_all_resolutions", 
            "achievement_resolution_12", "achievement_resolution_12_desc",
            AchievementCategory.RANDOM_RESOLUTION, "icon_5_monitor")); // Monitor icon
        add(new Achievement("play_15_move_games_all_resolutions", 
            "achievement_resolution_15", "achievement_resolution_15_desc",
            AchievementCategory.RANDOM_RESOLUTION, "icon_5_monitor")); // Computer monitor
            
        // ========== RANDOM GAME - MULTIPLE TARGETS ==========
        add(new Achievement("game_2_targets", 
            "achievement_2_targets", "achievement_2_targets_desc",
            AchievementCategory.RANDOM_TARGETS, ICON_TARGET));
        add(new Achievement("game_3_targets", 
            "achievement_3_targets", "achievement_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, ICON_TARGET_BLUE));
        add(new Achievement("game_4_targets", 
            "achievement_4_targets", "achievement_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, "icon_51_spiral")); // Pyramid/triangle
        add(new Achievement("game_2_of_2_targets", 
            "achievement_2_of_2_targets", "achievement_2_of_2_targets_desc",
            AchievementCategory.RANDOM_TARGETS, ICON_TARGET_BLUE)); // Target blue
        add(new Achievement("game_2_of_3_targets", 
            "achievement_2_of_3_targets", "achievement_2_of_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, "icon_53_ring_blue")); // Hexagon
        add(new Achievement("game_2_of_4_targets", 
            "achievement_2_of_4_targets", "achievement_2_of_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, "icon_54_cube")); // Cube
        add(new Achievement("game_3_of_3_targets", 
            "achievement_3_of_3_targets", "achievement_3_of_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, ICON_MAP)); // Map
        add(new Achievement("game_3_of_4_targets", 
            "achievement_3_of_4_targets", "achievement_3_of_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, "icon_50_folders")); // Triangle eye
        add(new Achievement("game_4_of_4_targets", 
            "achievement_4_of_4_targets", "achievement_4_of_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, ICON_CROWN)); // Crown for completing all 4
            
        // ========== RANDOM GAME - FUN CHALLENGES ==========
        add(new Achievement("game_5_robots", 
            "achievement_5_robots", "achievement_5_robots_desc",
            AchievementCategory.RANDOM_ROBOTS, ICON_ROBOT_YELLOW));
        add(new Achievement("gimme_five", 
            "achievement_gimme_five", "achievement_gimme_five_desc",
            AchievementCategory.RANDOM_ROBOTS, ICON_SPARKLES));
            
        // ========== RANDOM GAME - SQUARE COVERAGE ==========
        // 4 achievements: with/without goal requirement, 1 robot/all robots
        add(new Achievement("traverse_all_squares_1_robot", 
            "achievement_traverse_1_robot", "achievement_traverse_1_robot_desc",
            AchievementCategory.RANDOM_COVERAGE, ICON_MAP));
        add(new Achievement("traverse_all_squares_1_robot_goal", 
            "achievement_traverse_1_robot_goal", "achievement_traverse_1_robot_goal_desc",
            AchievementCategory.RANDOM_COVERAGE, ICON_TARGET));
        add(new Achievement("traverse_all_squares_all_robots", 
            "achievement_traverse_all_robots", "achievement_traverse_all_robots_desc",
            AchievementCategory.RANDOM_COVERAGE, ICON_SATELLITE));
        add(new Achievement("traverse_all_squares_all_robots_goal", 
            "achievement_traverse_all_robots_goal", "achievement_traverse_all_robots_goal_desc",
            AchievementCategory.RANDOM_COVERAGE, ICON_TARGET_BLUE));
            
        // ========== RANDOM GAME - STREAKS & CHALLENGES ==========
        // Perfect random games (cumulative - no reset)
        add(new Achievement("perfect_random_games_5", 
            "achievement_perfect_random_5", "achievement_perfect_random_5_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_FLAME));
        add(new Achievement("perfect_random_games_10", 
            "achievement_perfect_random_10", "achievement_perfect_random_10_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_INFINITY));
        add(new Achievement("perfect_random_games_20", 
            "achievement_perfect_random_20", "achievement_perfect_random_20_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_CROWN));
        // Perfect random games streak (resets on non-optimal)
        add(new Achievement("perfect_random_games_streak_5", 
            "achievement_perfect_random_streak_5", "achievement_perfect_random_streak_5_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_FLAME));
        add(new Achievement("perfect_random_games_streak_10", 
            "achievement_perfect_random_streak_10", "achievement_perfect_random_streak_10_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_INFINITY));
        add(new Achievement("perfect_random_games_streak_20", 
            "achievement_perfect_random_streak_20", "achievement_perfect_random_streak_20_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_CROWN));
        // No hints random games (cumulative - no reset)
        add(new Achievement("no_hints_random_10", 
            "achievement_no_hints_random_10", "achievement_no_hints_random_10_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_BRAIN));
        add(new Achievement("no_hints_random_50", 
            "achievement_no_hints_random_50", "achievement_no_hints_random_50_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_SHIELD_STAR));
        // No hints random games streak (resets on hint usage)
        add(new Achievement("no_hints_streak_random_10", 
            "achievement_no_hints_streak_random_10", "achievement_no_hints_streak_random_10_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_BRAIN));
        add(new Achievement("no_hints_streak_random_50", 
            "achievement_no_hints_streak_random_50", "achievement_no_hints_streak_random_50_desc",
            AchievementCategory.RANDOM_STREAKS, ICON_SHIELD_STAR));
            
        // ========== RANDOM GAME - SPEED ==========
        add(new Achievement("speedrun_random_under_20s", 
            "achievement_speedrun_random_20s", "achievement_speedrun_random_20s_desc",
            AchievementCategory.RANDOM_SPEED, ICON_HOURGLASS));
        add(new Achievement("speedrun_random_under_10s", 
            "achievement_speedrun_random_10s", "achievement_speedrun_random_10s_desc",
            AchievementCategory.RANDOM_SPEED, ICON_LIGHTNING));
        add(new Achievement("speedrun_random_5_games_under_30s", 
            "achievement_speedrun_random_5x30s", "achievement_speedrun_random_5x30s_desc",
            AchievementCategory.RANDOM_SPEED, "icon_55_cone")); // Rocket
    }
}

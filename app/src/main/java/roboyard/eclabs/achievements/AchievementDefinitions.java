package roboyard.eclabs.achievements;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines all achievements in the game.
 * Each achievement uses a sprite index (0-63) from the achievements_icons_64.png sprite sheet.
 * 
 * Sprite sheet layout (8x8 grid, left-to-right, top-to-bottom):
 * Row 0: 0-7   (lightning, heart gear, robot, power, monitor, key, trophy, target)
 * Row 1: 8-15  (trophy star, medal, chart up, buildings, hourglass, checklist, lightbulb, speedometer)
 * Row 2: 16-23 (trophy red, tools, gear, computer, hand, document, gamepad, puzzle)
 * Row 3: 24-31 (shield gray, shield red, shield star, anchor, brain, list, battery, infinity)
 * Row 4: 32-39 (target blue, magnifier, tablet, circle, map, satellite, sparkles, lamp)
 * Row 5: 40-47 (crown, wreath, trophy medal, trophy gold, diamond cup, flame, star, stars)
 * Row 6: 48-55 (robot yellow, triangle eye, pyramid, network, hexagon, cube, rocket, circle white)
 * Row 7: 56-63 (additional icons)
 */
public class AchievementDefinitions {
    
    // Sprite indices for different achievement types
    private static final int ICON_TROPHY_STAR = 8;      // Trophy with star - progression
    private static final int ICON_MEDAL = 9;            // Medal - level completion
    private static final int ICON_CHART_UP = 10;        // Chart going up - progress
    private static final int ICON_TROPHY_GOLD = 43;     // Gold trophy - mastery
    private static final int ICON_STAR = 46;            // Star - stars collected
    private static final int ICON_STARS = 47;           // Multiple stars - all stars
    private static final int ICON_CROWN = 40;           // Crown - master achievement
    private static final int ICON_DIAMOND_CUP = 44;     // Diamond cup - platinum
    private static final int ICON_CHECKLIST = 13;       // Checklist - completion
    private static final int ICON_SPEEDOMETER = 15;     // Speedometer - speed
    private static final int ICON_LIGHTNING = 0;        // Lightning - fast
    private static final int ICON_FLAME = 45;           // Flame - streak/hot
    private static final int ICON_SHIELD_STAR = 26;     // Shield with star - perfect
    private static final int ICON_SHIELD_RED = 25;      // Shield red - challenge
    private static final int ICON_BRAIN = 28;           // Brain - thinking/no hints
    private static final int ICON_LIGHTBULB = 14;       // Lightbulb - idea/custom
    private static final int ICON_TOOLS = 17;           // Tools - create
    private static final int ICON_NETWORK = 51;         // Network - share
    private static final int ICON_HEART_GEAR = 1;       // Heart gear - first game
    private static final int ICON_INFINITY = 31;        // Infinity - streak
    private static final int ICON_ROBOT = 2;            // Robot - robot related
    private static final int ICON_ROBOT_YELLOW = 48;    // Yellow robot - 5 robots
    private static final int ICON_TARGET = 7;           // Target - targets
    private static final int ICON_TARGET_BLUE = 32;     // Blue target - multi targets
    private static final int ICON_PUZZLE = 23;          // Puzzle - solution
    private static final int ICON_TABLET = 34;          // Tablet - resolution
    private static final int ICON_MAP = 36;             // Map - coverage
    private static final int ICON_SATELLITE = 37;       // Satellite - explorer
    private static final int ICON_SPARKLES = 38;        // Sparkles - special
    private static final int ICON_WREATH = 41;          // Wreath - achievement
    private static final int ICON_TROPHY_MEDAL = 42;    // Trophy with medal
    private static final int ICON_POWER = 3;            // Power button - comeback
    private static final int ICON_HOURGLASS = 12;       // Hourglass - time
    private static final int ICON_BUILDINGS = 11;       // Buildings - levels
    
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
        int[] solutionIcons = {ICON_CHART_UP, ICON_BUILDINGS,  // 18, 19
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
            AchievementCategory.RANDOM_RESOLUTION, 35)); // Monitor icon
        add(new Achievement("play_15_move_games_all_resolutions", 
            "achievement_resolution_15", "achievement_resolution_15_desc",
            AchievementCategory.RANDOM_RESOLUTION, 4)); // Computer monitor
            
        // ========== RANDOM GAME - MULTIPLE TARGETS ==========
        add(new Achievement("game_2_targets", 
            "achievement_2_targets", "achievement_2_targets_desc",
            AchievementCategory.RANDOM_TARGETS, ICON_TARGET));
        add(new Achievement("game_3_targets", 
            "achievement_3_targets", "achievement_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, ICON_TARGET_BLUE));
        add(new Achievement("game_4_targets", 
            "achievement_4_targets", "achievement_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, 50)); // Pyramid/triangle
        add(new Achievement("game_2_of_2_targets", 
            "achievement_2_of_2_targets", "achievement_2_of_2_targets_desc",
            AchievementCategory.RANDOM_TARGETS, 32)); // Target blue
        add(new Achievement("game_2_of_3_targets", 
            "achievement_2_of_3_targets", "achievement_2_of_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, 52)); // Hexagon
        add(new Achievement("game_2_of_4_targets", 
            "achievement_2_of_4_targets", "achievement_2_of_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, 53)); // Cube
        add(new Achievement("game_3_of_3_targets", 
            "achievement_3_of_3_targets", "achievement_3_of_3_targets_desc",
            AchievementCategory.RANDOM_TARGETS, 36)); // Map
        add(new Achievement("game_3_of_4_targets", 
            "achievement_3_of_4_targets", "achievement_3_of_4_targets_desc",
            AchievementCategory.RANDOM_TARGETS, 49)); // Triangle eye
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
            AchievementCategory.RANDOM_SPEED, 54)); // Rocket
    }
}

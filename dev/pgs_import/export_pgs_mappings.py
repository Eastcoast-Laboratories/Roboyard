#!/usr/bin/env python3
"""
PGS Achievement Mappings Export Tool

Exports selected achievements from AchievementDefinitions.java to various formats.
Allows flexible selection of which achievements to include in the export.

Usage:
    python3 export_pgs_mappings.py [--achievements achievement1,achievement2,...]
    python3 export_pgs_mappings.py --category random_robots
    python3 export_pgs_mappings.py --all
"""

import re
import sys
import argparse
from pathlib import Path
from typing import Dict, List, Set

# Achievement categories and their achievements
ACHIEVEMENT_CATEGORIES = {
    'login': ['daily_login_7', 'daily_login_30', 'comeback_player'],
    'progression': ['first_game', 'level_1_complete', 'level_10_complete', 'level_50_complete', 'level_140_complete', 'all_stars_collected'],
    'performance': ['perfect_solutions_5', 'perfect_solutions_10', 'perfect_solutions_50', 'speedrun_under_30s', 'speedrun_under_10s'],
    'mastery': ['3_star_hard_level', '3_star_10_levels', '3_star_10_hard_levels', '3_star_50_levels', '3_star_all_levels'],
    'random_speed': ['speedrun_random_under_20s', 'speedrun_random_under_10s', 'speedrun_random_5_games_under_30s'],
    'random_streaks': ['perfect_random_games_5', 'perfect_random_games_10', 'perfect_random_games_20', 
                       'perfect_random_games_streak_5', 'perfect_random_games_streak_10', 'perfect_random_games_streak_20',
                       'perfect_no_hints_random_1', 'no_hints_random_10', 'no_hints_random_50',
                       'no_hints_streak_random_10', 'no_hints_streak_random_50'],
    'random_difficulty': ['impossible_mode_1', 'impossible_mode_5', 'impossible_mode_streak_5', 'impossible_mode_streak_10'],
    'random_solution': ['solution_18_moves', 'solution_19_moves', 'solution_20_moves', 'solution_21_moves',
                        'solution_22_moves', 'solution_23_moves', 'solution_24_moves', 'solution_25_moves',
                        'solution_26_moves', 'solution_27_moves', 'solution_28_moves', 'solution_29_moves',
                        'solution_30_plus_moves'],
    'random_resolution': ['play_10_move_games_all_resolutions', 'play_12_move_games_all_resolutions', 'play_15_move_games_all_resolutions'],
    'random_targets': ['game_2_targets', 'game_3_targets', 'game_4_targets',
                       'game_2_of_2_targets', 'game_2_of_3_targets', 'game_2_of_4_targets',
                       'game_3_of_3_targets', 'game_3_of_4_targets', 'game_4_of_4_targets'],
    'random_robots': ['game_5_robots', 'gimme_five', 'same_walls_2', 'same_walls_10', 'same_walls_100'],
    'random_coverage': ['traverse_all_squares_1_robot', 'traverse_all_squares_1_robot_goal',
                        'traverse_all_squares_all_robots', 'traverse_all_squares_all_robots_goal'],
}

# PGS Resource Key Mappings
PGS_MAPPINGS = {
    'daily_login_7': 'pgs_weekly_player',
    'daily_login_30': 'pgs_dedicated_player',
    'comeback_player': 'pgs_welcome_back',
    'first_game': 'pgs_welcome',
    'level_1_complete': 'pgs_first_steps',
    'level_10_complete': 'pgs_getting_started',
    'level_50_complete': 'pgs_halfway_there',
    'level_140_complete': 'pgs_level_master',
    'all_stars_collected': 'pgs_star_collector',
    'perfect_solutions_5': 'pgs_perfect_mover',
    'perfect_solutions_10': 'pgs_precision_player',
    'perfect_solutions_50': 'pgs_optimization_expert',
    'speedrun_under_30s': 'pgs_quick_thinker',
    'speedrun_under_10s': 'pgs_lightning_fast',
    '3_star_hard_level': 'pgs_hard_level_star',
    '3_star_10_levels': 'pgs_rising_star',
    '3_star_10_hard_levels': 'pgs_hard_level_master',
    '3_star_50_levels': 'pgs_superstar',
    '3_star_all_levels': 'pgs_perfect_master',
    'speedrun_random_under_20s': 'pgs_speed_demon',
    'speedrun_random_under_10s': 'pgs_lightning_speed',
    'speedrun_random_5_games_under_30s': 'pgs_speed_streak',
    'perfect_random_games_5': 'pgs_perfect_5',
    'perfect_random_games_10': 'pgs_perfect_10',
    'perfect_random_games_20': 'pgs_perfect_20',
    'perfect_random_games_streak_5': 'pgs_perfect_streak_5',
    'perfect_random_games_streak_10': 'pgs_perfect_streak_10',
    'perfect_random_games_streak_20': 'pgs_perfect_streak_20',
    'perfect_no_hints_random_1': 'pgs_perfect_no_help',
    'no_hints_random_10': 'pgs_no_help_needed_10',
    'no_hints_random_50': 'pgs_no_help_needed_50',
    'no_hints_streak_random_10': 'pgs_no_help_streak_10',
    'no_hints_streak_random_50': 'pgs_no_help_streak_50',
    'impossible_mode_1': 'pgs_impossible_dream',
    'impossible_mode_5': 'pgs_impossible_champion',
    'impossible_mode_streak_5': 'pgs_impossible_streak',
    'impossible_mode_streak_10': 'pgs_impossible_legend',
    'solution_18_moves': 'pgs_18_move_master',
    'solution_19_moves': 'pgs_19_move_master',
    'solution_20_moves': 'pgs_20_move_master',
    'solution_21_moves': 'pgs_21_move_master',
    'solution_22_moves': 'pgs_22_move_master',
    'solution_23_moves': 'pgs_23_move_master',
    'solution_24_moves': 'pgs_24_move_master',
    'solution_25_moves': 'pgs_25_move_master',
    'solution_26_moves': 'pgs_26_move_master',
    'solution_27_moves': 'pgs_27_move_master',
    'solution_28_moves': 'pgs_28_move_master',
    'solution_29_moves': 'pgs_29_move_master',
    'solution_30_plus_moves': 'pgs_30_move_master',
    'play_10_move_games_all_resolutions': 'pgs_resolution_explorer_10',
    'play_12_move_games_all_resolutions': 'pgs_resolution_explorer_12',
    'play_15_move_games_all_resolutions': 'pgs_resolution_explorer_15',
    'game_2_targets': 'pgs_double_target',
    'game_3_targets': 'pgs_triple_target',
    'game_4_targets': 'pgs_quad_target',
    'game_2_of_2_targets': 'pgs_2_of_2',
    'game_2_of_3_targets': 'pgs_2_of_3',
    'game_2_of_4_targets': 'pgs_2_of_4',
    'game_3_of_3_targets': 'pgs_3_of_3',
    'game_3_of_4_targets': 'pgs_3_of_4',
    'game_4_of_4_targets': 'pgs_4_of_4',
    'game_5_robots': 'pgs_full_team',
    'gimme_five': 'pgs_gimme_five',
    'same_walls_2': 'pgs_same_walls_2',
    'same_walls_10': 'pgs_same_walls_10',
    'same_walls_100': 'pgs_same_walls_100',
    'traverse_all_squares_1_robot': 'pgs_solo_explorer',
    'traverse_all_squares_1_robot_goal': 'pgs_solo_goal_explorer',
    'traverse_all_squares_all_robots': 'pgs_team_explorer',
    'traverse_all_squares_all_robots_goal': 'pgs_team_goal_explorer',
}

def get_achievements_to_export(args) -> Set[str]:
    """Determine which achievements to export based on arguments."""
    if args.all:
        return set(PGS_MAPPINGS.keys())
    
    achievements = set()
    
    if args.achievements:
        for ach_id in args.achievements.split(','):
            ach_id = ach_id.strip()
            if ach_id in PGS_MAPPINGS:
                achievements.add(ach_id)
            else:
                print(f"Warning: Unknown achievement ID: {ach_id}", file=sys.stderr)
    
    if args.category:
        if args.category in ACHIEVEMENT_CATEGORIES:
            achievements.update(ACHIEVEMENT_CATEGORIES[args.category])
        else:
            print(f"Error: Unknown category: {args.category}", file=sys.stderr)
            print(f"Available categories: {', '.join(ACHIEVEMENT_CATEGORIES.keys())}", file=sys.stderr)
            sys.exit(1)
    
    if not achievements:
        print("Error: No achievements selected. Use --all, --achievements, or --category", file=sys.stderr)
        sys.exit(1)
    
    return achievements

def export_java_switch(achievements: Set[str]) -> str:
    """Export as Java switch statement."""
    lines = ['    private String getPlayGamesAchievementId(String localId) {',
             '        try {',
             '            int resId = 0;',
             '            switch (localId) {']
    
    for ach_id in sorted(achievements):
        resource_key = PGS_MAPPINGS[ach_id]
        lines.append(f'                case "{ach_id}":')
        lines.append(f'                    resId = R.string.{resource_key};')
        lines.append('                    break;')
    
    lines.extend([
        '                default:',
        '                    Timber.w("%s Unknown achievement ID: %s", TAG, localId);',
        '                    return null;',
        '            }',
        '            return context.getString(resId);',
        '        } catch (Exception e) {',
        '            Timber.e(e, "%s Failed to get Play Games ID for: %s", TAG, localId);',
        '            return null;',
        '        }',
        '    }'
    ])
    
    return '\n'.join(lines)

def export_java_map(achievements: Set[str]) -> str:
    """Export as Java LinkedHashMap."""
    lines = ['    private static final Map<String, String> PLAY_GAMES_MAPPINGS = new LinkedHashMap<String, String>() {{']
    
    for ach_id in sorted(achievements):
        resource_key = PGS_MAPPINGS[ach_id]
        lines.append(f'        put("{ach_id}", "{resource_key}");')
    
    lines.append('    }};')
    
    return '\n'.join(lines)

def export_csv(achievements: Set[str]) -> str:
    """Export as CSV format."""
    lines = ['achievement_id,pgs_resource_key']
    
    for ach_id in sorted(achievements):
        resource_key = PGS_MAPPINGS[ach_id]
        lines.append(f'{ach_id},{resource_key}')
    
    return '\n'.join(lines)

def main():
    parser = argparse.ArgumentParser(
        description='Export PGS achievement mappings in various formats',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Examples:
  # Export only new wall achievements
  python3 export_pgs_mappings.py --achievements "same_walls_2,same_walls_10,same_walls_100"
  
  # Export all random_robots category achievements
  python3 export_pgs_mappings.py --category random_robots
  
  # Export all achievements as map
  python3 export_pgs_mappings.py --all --format map
  
  # List available categories
  python3 export_pgs_mappings.py --list-categories
        '''
    )
    
    parser.add_argument('--achievements', help='Comma-separated list of achievement IDs to export')
    parser.add_argument('--category', help='Export all achievements in a category')
    parser.add_argument('--all', action='store_true', help='Export all achievements')
    parser.add_argument('--format', choices=['switch', 'map', 'csv'], default='switch',
                        help='Export format (default: switch)')
    parser.add_argument('--list-categories', action='store_true', help='List available categories')
    parser.add_argument('--output', help='Output file (default: stdout)')
    
    args = parser.parse_args()
    
    if args.list_categories:
        print("Available achievement categories:")
        for category, achievements in sorted(ACHIEVEMENT_CATEGORIES.items()):
            print(f"  {category}: {len(achievements)} achievements")
        return
    
    achievements = get_achievements_to_export(args)
    
    if args.format == 'switch':
        output = export_java_switch(achievements)
    elif args.format == 'map':
        output = export_java_map(achievements)
    elif args.format == 'csv':
        output = export_csv(achievements)
    
    if args.output:
        Path(args.output).write_text(output)
        print(f"Exported {len(achievements)} achievements to {args.output}")
    else:
        print(output)

if __name__ == '__main__':
    main()

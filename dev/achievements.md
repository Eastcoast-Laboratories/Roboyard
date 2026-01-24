# Roboyard Achievements

This document defines all achievements in the game. Keep this file synchronized with the implementation in `AchievementDefinitions.java`.

## Implementation Status

- [x] Achievement data model (`Achievement.java`)
- [x] Achievement categories (`AchievementCategory.java`)
- [x] Achievement definitions (`AchievementDefinitions.java`)
- [x] Achievement manager with unlock logic (`AchievementManager.java`)
- [x] Achievement UI (`AchievementsFragment.java`)
- [x] Achievement unlock popup (`AchievementPopup.java`)
- [x] Achievement triggers in game completion
- [x] Main menu button to view achievements
- [ ] Sidekick sync (Play Store only, guarded by BuildConfig.ENABLE_SIDEKICK)

---

## Achievement Categories

### Progression Achievements (5)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `level_1_complete` | First Steps | Complete Level 1 | ✅ Implemented |
| `level_10_complete` | Getting Started | Complete 10 levels | ✅ Implemented |
| `level_50_complete` | Halfway There | Complete 50 levels | ✅ Implemented |
| `level_140_complete` | Level Master | Complete all 140 levels | ✅ Implemented |
| `all_stars_collected` | Star Collector | Collect all 420 stars | ✅ Implemented |

### Performance Achievements (5)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `perfect_solutions_5` | Perfect Mover | Solve 5 levels with optimal moves | ✅ Implemented |
| `perfect_solutions_10` | Precision Player | Solve 10 levels with optimal moves | ✅ Implemented |
| `perfect_solutions_50` | Optimization Expert | Solve 50 levels with optimal moves | ✅ Implemented |
| `speedrun_under_30s` | Quick Thinker | Complete a level in under 30 seconds | ✅ Implemented |
| `speedrun_under_10s` | Lightning Fast | Complete a level in under 10 seconds | ✅ Implemented |

### Challenge Achievements (0 active, 3 pending)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `solve_custom_level` | Custom Solver | Solve a custom-created level | ❌ Not Implemented (custom levels feature pending) |
| `create_custom_level` | Level Designer | Create and save a custom level | ❌ Not Implemented (custom levels feature pending) |
| `share_custom_level` | Sharing is Caring | Share a custom level with others | ❌ Not Implemented (custom levels feature pending) |

> **Note:** `no_hints_10` and `no_hints_50` were removed because hints are not allowed in levels.

### Mastery Achievements (5)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `3_star_hard_level` | Hard Level Star | Achieve 3 stars on a level with 5+ optimal moves | ✅ Implemented |
| `3_star_10_levels` | Rising Star | Achieve 3 stars on 10 levels | ✅ Implemented |
| `3_star_10_hard_levels` | Hard Level Master | Achieve 3 stars on 10 levels with 5+ optimal moves | ✅ Implemented |
| `3_star_50_levels` | Superstar | Achieve 3 stars on 50 levels | ✅ Implemented |
| `3_star_all_levels` | Perfect Master | Achieve 3 stars on all levels | ✅ Implemented |

### Streak Achievements (4)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `first_game` | Welcome! | Play your first game | ✅ Implemented |
| `daily_login_7` | Weekly Player | Log in 7 days in a row | ✅ Implemented |
| `daily_login_30` | Dedicated Player | Log in 30 days in a row | ✅ Implemented |
| `comeback_player` | Welcome Back! | Return after 30 days of inactivity | ✅ Implemented |

---

## Random Game Achievements

### Difficulty (4)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `impossible_mode_1` | Impossible Dream | Complete 1 game in Impossible mode | ✅ Implemented |
| `impossible_mode_5` | Impossible Champion | Complete 5 games in Impossible mode | ✅ Implemented |
| `impossible_mode_streak_5` | Impossible Streak | Complete 5 games in a row in Impossible mode with optimal moves | ✅ Implemented |
| `impossible_mode_streak_10` | Impossible Legend | Complete 10 games in a row in Impossible mode with optimal moves | ✅ Implemented |

### Solution Length (11)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `solution_18_moves` | 18 Move Master | Complete a game with optimal solution of 18 moves | ✅ Implemented |
| `solution_19_moves` | 19 Move Master | Complete a game with optimal solution of 19 moves | ✅ Implemented |
| `solution_20_moves` | 20 Move Master | Complete a game with optimal solution of 20 moves | ✅ Implemented |
| `solution_21_moves` | 21 Move Master | Complete a game with optimal solution of 21 moves | ✅ Implemented |
| `solution_22_moves` | 22 Move Master | Complete a game with optimal solution of 22 moves | ✅ Implemented |
| `solution_23_moves` | 23 Move Master | Complete a game with optimal solution of 23 moves | ✅ Implemented |
| `solution_24_moves` | 24 Move Master | Complete a game with optimal solution of 24 moves | ✅ Implemented |
| `solution_25_moves` | 25 Move Master | Complete a game with optimal solution of 25 moves | ✅ Implemented |
| `solution_26_moves` | 26 Move Master | Complete a game with optimal solution of 26 moves | ✅ Implemented |
| `solution_27_moves` | 27 Move Master | Complete a game with optimal solution of 27 moves | ✅ Implemented |
| `solution_28_moves` | 28 Move Master | Complete a game with optimal solution of 28 moves | ✅ Implemented |
| `solution_29_moves` | 29 Move Master | Complete a game with optimal solution of 29 moves | ✅ Implemented |
| `solution_30_plus_moves` | 30+ Move Master | Complete a game with optimal solution of 30+ moves | ✅ Implemented |

### Screen Resolutions (3)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|
| `play_10_move_games_all_resolutions` | Resolution Explorer 10 | Play games with 10+ moves on all screen resolutions | ✅ Implemented |
| `play_12_move_games_all_resolutions` | Resolution Explorer 12 | Play games with 12+ moves on all screen resolutions | ✅ Implemented |
| `play_15_move_games_all_resolutions` | Resolution Explorer 15 | Play games with 15+ moves on all screen resolutions | ✅ Implemented |

### Multiple Targets (6)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|
| `game_2_targets` | Double Target | Complete a game with 2 targets | ✅ Implemented |
| `game_3_targets` | Triple Target | Complete a game with 3 targets | ✅ Implemented |
| `game_4_targets` | Quad Target | Complete a game with 4 targets | ✅ Implemented |
| `game_2_of_3_targets` | 2 of 3 | Complete a game where you need 2 out of 3 targets | ✅ Implemented |
| `game_2_of_4_targets` | 2 of 4 | Complete a game where you need 2 out of 4 targets | ✅ Implemented |
| `game_3_of_4_targets` | 3 of 4 | Complete a game where you need 3 out of 4 targets | ✅ Implemented |
| `game_4_of_4_targets` | 4 of 4 | Complete a game where you need all 4 targets | ✅ Implemented |

### Robot Count (1)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `game_5_robots` | Full Team | Complete a game with 5 robots | ✅ Implemented | ✅ |

### Square Coverage (4)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `traverse_all_squares_1_robot` | Solo Explorer | Visit all squares on the board with 1 robot (after goal allowed) | ✅ Implemented | ✅ |
| `traverse_all_squares_1_robot_goal` | Solo Goal Explorer | Visit all squares with 1 robot, reaching the goal last | ✅ Implemented |
| `traverse_all_squares_all_robots` | Team Explorer | Visit all squares on the board with all robots combined (after goal allowed) | ✅ Implemented | ✅ |
| `traverse_all_squares_all_robots_goal` | Team Goal Explorer | Visit all squares with all robots, reaching the goal last | ✅ Implemented | ✅ |

### Streaks & Challenges (5)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `perfect_random_games_5` | Perfect 5 | Complete 5 random games with optimal moves | ✅ Implemented |
| `perfect_random_games_10` | Perfect 10 | Complete 10 random games with optimal moves | ✅ Implemented |
| `perfect_random_games_20` | Perfect 20 | Complete 20 random games with optimal moves | ✅ Implemented |
| `no_hints_random_10` | No Help Needed 10 | Complete 10 random games without using hints in a row (streak) | ✅ Implemented |
| `no_hints_random_50` | No Help Needed 50 | Complete 50 random games without using hints in a row (streak) | ✅ Implemented |

### Speed (3)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `speedrun_random_under_20s` | Speed Demon | Complete a random game in under 20 seconds | ✅ Implemented |
| `speedrun_random_under_10s` | Lightning Speed | Complete a random game in under 10 seconds | ✅ Implemented |
| `speedrun_random_5_games_under_30s` | Speed Streak | Complete 5 random games in under 30 seconds each | ✅ Implemented |

---

## Total: 58 Achievements

| Category | Count |
|----------|-------|
| Progression | 5 |
| Performance | 5 |
| Challenge | 5 |
| Mastery | 4 |
| Special | 4 |
| Random - Difficulty | 4 |
| Random - Solution Length | 11 |
| Random - Screen Resolutions | 3 |
| Random - Multiple Targets | 6 |
| Random - Robot Count | 1 |
| Random - Square Coverage | 2 |
| Random - Streaks | 5 |
| Random - Speed | 3 |
| **Total** | **58** |

Note: The solution length achievements include 20-29 (10 achievements) plus 30+ (1 achievement) = 11 total.

---

## Technical Notes

### Storage
- Achievements are stored in SharedPreferences (`roboyard_achievements`)
- Each achievement has: `unlocked_<id>` (boolean) and `timestamp_<id>` (long)
- Counters for progressive achievements: `counter_<name>` (int)

### Triggers
- Level completion: `AchievementManager.onLevelCompleted()`
- Random game completion: `AchievementManager.onRandomGameCompleted()`
- Custom level events: `onCustomLevelCreated()`, `onCustomLevelSolved()`, `onCustomLevelShared()`
- Daily login: `onDailyLogin()`
- Comeback: `onComebackPlayer()`

### UI
- `AchievementsFragment`: Main achievements screen
- `AchievementPopup`: Unlock notification popup
- Icons: `ic_achievement_*.xml` drawables

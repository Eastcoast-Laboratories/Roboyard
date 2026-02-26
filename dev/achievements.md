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


### Login Streak Achievements (4)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `daily_login_7` | Weekly Player | Log in 7 days in a row | ✅ Implemented |
| `daily_login_30` | Dedicated Player | Log in 30 days in a row | ✅ Implemented |
| `comeback_player` | Welcome Back! | Return after 30 days of inactivity | ✅ Implemented |

### Progression Achievements (5)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `first_game` | Welcome! | Play your first game | ✅ Implemented |
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
| `game_2_of_2_targets` | 2 of 2 | Complete a game where you need 2 out of 2 targets | ✅ Implemented |
| `game_2_of_3_targets` | 2 of 3 | Complete a game where you need 2 out of 3 targets | ✅ Implemented |
| `game_2_of_4_targets` | 2 of 4 | Complete a game where you need 2 out of 4 targets | ✅ Implemented |
| `game_3_of_3_targets` | 3 of 3 | Complete a game where you need 3 out of 3 targets | ✅ Implemented |
| `game_3_of_4_targets` | 3 of 4 | Complete a game where you need 3 out of 4 targets | ✅ Implemented |
| `game_4_of_4_targets` | 4 of 4 | Complete a game where you need all 4 targets | ✅ Implemented |

### Fun Challenges (5)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `game_5_robots` | Full Team | Complete a game with 5 robots | ✅ Implemented | ✅ |
| `gimme_five` | Gimme Five | All Robots must touch each other | ✅ Implemented |
| `same_walls_2` | Déjà Vu | Solve the same wall layout with 2 different robot positions | ✅ Implemented |
| `same_walls_5` | Wall Expert | Solve the same wall layout with 5 different robot positions | ✅ Implemented |
| `same_walls_10` | Wall Master | Solve the same wall layout with 10 different robot positions | ✅ Implemented |

### Square Coverage (4)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `traverse_all_squares_1_robot` | Solo Explorer | Visit all squares on the board with 1 robot (after goal allowed) | ✅ Implemented | ✅ |
| `traverse_all_squares_1_robot_goal` | Solo Goal Explorer | Visit all squares with 1 robot, reaching the goal last | ✅ Implemented |
| `traverse_all_squares_all_robots` | Team Explorer | Visit all squares on the board with all robots combined (after goal allowed) | ✅ Implemented | ✅ |
| `traverse_all_squares_all_robots_goal` | Team Goal Explorer | Visit all squares with all robots, reaching the goal last | ✅ Implemented | ✅ |

### Streaks & Challenges (10)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `perfect_random_games_5` | Perfect 5 | Complete 5 random games with optimal moves (cumulative) | ✅ Implemented | |
| `perfect_random_games_10` | Perfect 10 | Complete 10 random games with optimal moves (cumulative) | ✅ Implemented | |
| `perfect_random_games_20` | Perfect 20 | Complete 20 random games with optimal moves (cumulative) | ✅ Implemented | |
| `perfect_random_games_streak_5` | Perfect Streak 5 | Complete 5 random games with optimal moves in a row | ✅ Implemented | |
| `perfect_random_games_streak_10` | Perfect Streak 10 | Complete 10 random games with optimal moves in a row | ✅ Implemented | |
| `perfect_random_games_streak_20` | Perfect Streak 20 | Complete 20 random games with optimal moves in a row | ✅ Implemented | |
| `perfect_no_hints_random_1` | Perfect No Help | Complete a random game with optimal moves (10+ moves) without using hints | not Implemented | |
| `no_hints_random_10` | No Help Needed 10 | Complete 10 random games without using hints (cumulative) | ✅ Implemented | |
| `no_hints_random_50` | No Help Needed 50 | Complete 50 random games without using hints (cumulative) | ✅ Implemented | |
| `no_hints_streak_random_10` | No Help Streak 10 | Complete 10 random games without using hints in a row | ✅ Implemented | |
| `no_hints_streak_random_50` | No Help Streak 50 | Complete 50 random games without using hints in a row | ✅ Implemented | |

### Speed (3)

| ID | Name | Description | Status | tested |
|----|------|-------------|--------|--------|
| `speedrun_random_under_20s` | Speed Demon | Complete a random game in under 20 seconds | ✅ Implemented |
| `speedrun_random_under_10s` | Lightning Speed | Complete a random game in under 10 seconds | ✅ Implemented |
| `speedrun_random_5_games_under_30s` | Speed Streak | Complete 5 random games in under 30 seconds each | ✅ Implemented |

---

## Total: 63 Achievements

| Category | Count |
|----------|-------|
| Progression | 6 |
| Performance | 5 |
| Challenge | 5 |
| Mastery | 5 |
| Special | 4 |
| Random - Speed | 3 |
| Random - Difficulty | 4 |
| Random - Solution Length | 11 |
| Random - Screen Resolutions | 3 |
| Random - Multiple Targets | 8 |
| Random - Fun Challenges | 5 |
| Random - Square Coverage | 4 |
| Random - Streaks | 5 |
| Random - Same Walls | 3 |

Note: The solution length achievements include 20-29 (10 achievements) plus 30+ (1 achievement) = 11 total.

---

## Unique Map Tracking (Anti-Cheat System)

To prevent achievement farming by repeatedly solving the same map (e.g., loading a savegame), the existing **Game History** (`GameHistoryManager`) has been enhanced to track unique maps permanently.

### Enhanced History Storage

The existing `GameHistoryEntry` class has been extended with:
- **mapSignature**: Unique signature combining walls + positions (for exact map matching)
- **wallSignature**: Signature of wall layout only (for achievements tracking same walls, different positions)
- **positionSignature**: Signature of robot + target positions only
- **completionTimestamps**: List of ALL timestamps when this map was completed
- **completionCount**: How many times this exact map was solved
- **lastCompletionTimestamp**: When the map was most recently solved
- **bestTime**: Fastest completion time for this map
- **bestMoves**: Fewest moves used to solve this map
- **maxHintUsed**: Highest hint index ever viewed for this map (-1 = no hints)
- **solvedWithoutHints**: True only if FIRST completion was without any hints

### Hint Tracking for Achievements

The hint system tracks hint usage permanently per map:
- **Critical hint**: The first normal hint (index 0) reveals which robot colors to use
- Once ANY hint is viewed, the map is **permanently marked** as "solved with hints"
- Even if the same map is later solved without hints, it does NOT qualify for "no hints" achievements
- The `maxHintUsed` field tracks the highest hint index ever viewed
- The `solvedWithoutHints` field is only true if the FIRST completion used no hints

**Methods for hint tracking:**
- `GameHistoryEntry.recordHintUsed(int hintIndex)` - Records hint usage (updates max)
- `GameHistoryEntry.hasUsedHints()` - Returns true if any hint was ever used
- `GameHistoryEntry.qualifiesForNoHintsAchievement()` - Returns true only if first solve was hint-free
- `GameState.recordHintUsed(int hintIndex)` - Tracks hints during current session
- `GameState.getMaxHintUsedThisSession()` - Gets max hint used in current session

**Key changes to GameHistoryManager:**
- Entries are **never deleted** (no MAX_HISTORY_ENTRIES limit)
- Maps are matched by `mapSignature` instead of `mapName`
- Duplicate completions update existing entry via `recordCompletion()` instead of replacing
- Sorted by `lastCompletionTimestamp` (most recently played first)

Storage location: `history_index.json` (existing file, extended format)

### Achievement Conditions by Type

#### Achievements requiring UNIQUE maps (first-time completion only):

| Achievement Type | Condition |
|------------------|-----------|
| **Count-based** (e.g., "Complete 5 games in Impossible mode") | Only counts if map is NEW (not in history) |
| **Speed achievements** (e.g., "Complete in under 10 seconds") | Only triggers on FIRST completion of a map |
| **Solution Length** (e.g., "18 Move Master") | Only triggers on FIRST completion of a map |
| **Perfect solutions** (optimal moves) | Only counts if map is NEW |
| **Streak achievements** | Each map in the streak must be UNIQUE |
| **Resolution Explorer** | Each resolution needs a UNIQUE map |
| **Multiple Targets** | Only triggers on FIRST completion of a map |

#### Achievements NOT requiring unique maps:

| Achievement Type | Reason |
|------------------|--------|
| **Login Streak** | Not map-based |
| **Level progression** (Level 1-140) | Levels are fixed, tracked by level ID |
| **Star collection** | Stars are per-level, tracked by level ID |
| **Square Coverage** | Challenge achievements, can be repeated |
| **Fun Challenges** (Gimme Five, etc.) | Challenge achievements, can be repeated |

### Detailed Achievement Analysis

#### Random Game Achievements - Updated Conditions

**Difficulty (4)** - All require UNIQUE maps:
- `impossible_mode_1`: Map must be NEW
- `impossible_mode_5`: Each of the 5 maps must be UNIQUE
- `impossible_mode_streak_5`: Each of the 5 consecutive maps must be UNIQUE
- `impossible_mode_streak_10`: Each of the 10 consecutive maps must be UNIQUE

**Solution Length (13)** - All require FIRST completion:
- `solution_18_moves` through `solution_30_plus_moves`: Only triggers if this exact map has never been solved before

**Screen Resolutions (3)** - Each resolution needs UNIQUE maps:
- `play_10_move_games_all_resolutions`: For each resolution, the map must be NEW
- `play_12_move_games_all_resolutions`: For each resolution, the map must be NEW
- `play_15_move_games_all_resolutions`: For each resolution, the map must be NEW

**Multiple Targets (9)** - All require FIRST completion:
- `game_2_targets` through `game_4_of_4_targets`: Only triggers if map is NEW

**Streaks & Challenges (11)** - Count-based require UNIQUE maps:
- `perfect_random_games_5/10/20`: Each counted game must be a UNIQUE map
- `perfect_random_games_streak_5/10/20`: Each game in streak must be UNIQUE
- `no_hints_random_10/50`: Each counted game must be a UNIQUE map
- `no_hints_streak_random_10/50`: Each game in streak must be UNIQUE

**Speed (3)** - All require FIRST completion:
- `speedrun_random_under_20s`: Only triggers on FIRST completion of this map
- `speedrun_random_under_10s`: Only triggers on FIRST completion of this map
- `speedrun_random_5_games_under_30s`: Each of the 5 games must be a UNIQUE map

#### Performance Achievements - Updated Conditions

- `perfect_solutions_5/10/50`: Each counted solution must be a UNIQUE map (for random games) or unique level (for levels)
- `speedrun_under_30s`: Only triggers on FIRST completion of this map/level
- `speedrun_under_10s`: Only triggers on FIRST completion of this map/level

### Implementation

```java
// GameHistoryManager.java (enhanced existing class)
public class GameHistoryManager {
    // Check if this is first completion of a map
    public static boolean isFirstCompletion(Activity activity, String mapSignature);
    
    // Find entry by map signature
    public static GameHistoryEntry findByMapSignature(Activity activity, String mapSignature);
    
    // Find entries with same wall layout (different positions)
    public static List<GameHistoryEntry> findByWallSignature(Activity activity, String wallSignature);
    
    // Get total unique maps count
    public static int getUniqueMapCount(Activity activity);
    
    // Get completion count for a specific map
    public static int getCompletionCount(Activity activity, String mapSignature);
}

// GameState.java (new methods)
public class GameState {
    // Generate wall-only signature
    public String generateWallSignature();
    
    // Generate position-only signature (robots + targets)
    public String generatePositionSignature();
    
    // Generate full map signature (walls + positions)
    public String generateMapSignature();
}

// GameHistoryEntry.java (enhanced existing class)
public class GameHistoryEntry {
    // Record a new completion of this map
    public boolean recordCompletion(int completionTime, int moves);
    
    // Check if this is first completion
    public boolean isFirstCompletion();
}
```

### Map Comparison

Two maps are considered IDENTICAL if:
1. Same grid dimensions (width × height)
2. Same robot positions (all robots at same coordinates)
3. Same target positions (all targets at same coordinates with same colors)
4. Same wall positions (all walls at same coordinates)

Note: The order of elements in the comparison does not matter - positions are sorted before comparison.

---

## Technical Notes

### Storage
- Achievements are stored in SharedPreferences (`roboyard_achievements`)
- Each achievement has: `unlocked_<id>` (boolean) and `timestamp_<id>` (long)
- Counters for progressive achievements: `counter_<name>` (int)
- **Map History**: Stored in `map_history.json` (internal storage, not SharedPreferences due to size)

### Triggers
- Level completion: `AchievementManager.onLevelCompleted()`
- Random game completion: `AchievementManager.onRandomGameCompleted()`
- Custom level events: `onCustomLevelCreated()`, `onCustomLevelSolved()`, `onCustomLevelShared()`
- Daily login: `onDailyLogin()`
- Comeback: `onComebackPlayer()`
- **Map history check**: `MapHistoryManager.isFirstCompletion()` called before achievement triggers

### UI
- `AchievementsFragment`: Main achievements screen
- `AchievementPopup`: Unlock notification popup
- Icons: `ic_achievement_*.xml` drawables

### Data Migration
- On first launch after update: No migration needed (history starts empty)
- Existing achievements remain unlocked (grandfather clause)
- New achievements going forward require unique maps

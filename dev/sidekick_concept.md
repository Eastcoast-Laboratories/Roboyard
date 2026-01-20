# Sidekick Integration Concept for Roboyard

## Overview
Google Play Games Sidekick (Beta) is an overlay that presents relevant content and offers directly in the game without requiring players to leave. This document outlines how Roboyard could integrate Sidekick to enhance user engagement and monetization.

## Current Status
- Sidekick (Beta) is available for testing on Android 13+
- Requires Google Play Games Services integration
- Features vary based on integration status and registration

## Distribution Strategy: Single Codebase with Build Flags

### Important Consideration
**Sidekick is ONLY available on Google Play Store.** F-Droid users cannot access Sidekick features since:
- F-Droid doesn't include Google Play Services
- Sidekick requires Google Play Games Services SDK
- F-Droid focuses on open-source, privacy-first distribution

### Recommended Approach: Single Branch with Build Flags

Maintain a single codebase with build flags to conditionally enable/disable Play Store features. All Sidekick-dependent code is wrapped with `BuildConfig.ENABLE_SIDEKICK` constant.

**Build Configuration:**
```gradle
// build.gradle
buildTypes {
    debug {
        buildConfigField "boolean", "ENABLE_SIDEKICK", "true"
    }
    
    release {
        buildConfigField "boolean", "ENABLE_SIDEKICK", "true"
    }
}

flavorDimensions "store"

productFlavors {
    playstore {
        dimension "store"
        buildConfigField "boolean", "ENABLE_SIDEKICK", "true"
        // Include Google Play Services
        implementation 'com.google.android.gms:play-services-games:21.0.0'
    }
    
    fdroid {
        dimension "store"
        buildConfigField "boolean", "ENABLE_SIDEKICK", "false"
        // NO Google Play Services
    }
}
```

**Versioning:**
- All versions use same version number: `X.Y.Z` (whole numbers only)
- No `-fdroid` suffix needed
- Same version across all stores

### Implementation Strategy

**Feature Flag Pattern:**
All Sidekick-dependent code wrapped with `BuildConfig.ENABLE_SIDEKICK`:

```java
// Example: Achievement unlock
public void unlockAchievement(Achievement achievement) {
    // Always store locally
    achievementStorage.save(achievement);
    
    // Conditionally sync to Sidekick
    if (BuildConfig.ENABLE_SIDEKICK) {
        sidekickAchievementSync.syncToSidekick(achievement);
    }
    
    // Always show in-game UI
    showAchievementNotification(achievement);
}

// Example: Streak tracking
public void recordDailyLogin() {
    // Always track locally
    streakManager.incrementStreak();
    
    // Conditionally show leaderboard
    if (BuildConfig.ENABLE_SIDEKICK) {
        showStreakLeaderboard();
    } else {
        showLocalStreakUI();
    }
}

// Example: Initialization
public void initializeFeatures() {
    // Always initialize achievements and streaks
    achievementManager.initialize();
    streakManager.initialize();
    
    // Conditionally initialize Sidekick
    if (BuildConfig.ENABLE_SIDEKICK) {
        sidekickManager.initialize();
        googlePlayGamesManager.initialize();
    }
}
```

**Code Organization:**
```
src/main/java/
├── roboyard/logic/achievements/
│   ├── AchievementManager.java          (SHARED - uses BuildConfig.ENABLE_SIDEKICK)
│   ├── Achievement.java                 (SHARED - data model)
│   ├── AchievementStorage.java          (SHARED - local storage)
│   ├── AchievementUI.java               (SHARED - in-game display)
│   └── SidekickAchievementSync.java     (SHARED - guarded by BuildConfig.ENABLE_SIDEKICK)
│
├── roboyard/logic/streaks/
│   ├── StreakManager.java               (SHARED - uses BuildConfig.ENABLE_SIDEKICK)
│   ├── Streak.java                      (SHARED - data model)
│   ├── StreakStorage.java               (SHARED - local storage)
│   ├── StreakUI.java                    (SHARED - in-game display)
│   ├── SidekickStreakSync.java          (SHARED - guarded by BuildConfig.ENABLE_SIDEKICK)
│   └── StreakLeaderboard.java           (SHARED - guarded by BuildConfig.ENABLE_SIDEKICK)
│
└── roboyard/logic/sidekick/
    ├── SidekickManager.java             (SHARED - guarded by BuildConfig.ENABLE_SIDEKICK)
    └── GooglePlayGamesManager.java      (SHARED - guarded by BuildConfig.ENABLE_SIDEKICK)
```

**Advantages:**
- Single codebase to maintain
- No branch merging complexity
- Easy to toggle features per build flavor
- Same version number for all distributions
- Achievements and streaks work in both versions
- Only Sidekick sync/comparison differs

**Achievement & Streak Flow:**

Play Store (with Sidekick):

*Achievements:*
1. Player unlocks achievement
2. `AchievementManager.unlockAchievement()` called
3. Stored locally in SharedPreferences
4. `SidekickAchievementSync.syncToSidekick()` called
5. Synced to Google Play Games Services
6. Visible in Sidekick overlay + in-game UI
7. Can be compared with other players

*Streaks:*
1. Player logs in daily
2. `StreakManager.recordDailyLogin()` called
3. Streak counter incremented, stored locally
4. `SidekickStreakSync.syncToSidekick()` called
5. Synced to Google Play Games Services
6. Visible in Sidekick overlay + in-game UI
7. Can be compared on leaderboard

F-Droid (without Sidekick):

*Achievements:*
1. Player unlocks achievement
2. `AchievementManager.unlockAchievement()` called
3. Stored locally in SharedPreferences
4. `AchievementStub.syncToSidekick()` called (no-op)
5. Only visible in in-game UI
6. NO comparison with other players
7. NO Sidekick overlay

*Streaks:*
1. Player logs in daily
2. `StreakManager.recordDailyLogin()` called
3. Streak counter incremented, stored locally
4. `StreakStub.syncToSidekick()` called (no-op)
5. Only visible in in-game UI
6. NO comparison on leaderboard
7. NO Sidekick overlay

### Version Management

**Play Store Version:**
- Version: `X.Y.Z` (e.g., 1.0.0)
- Includes all Sidekick features
- Published to Google Play Store

**F-Droid Version:**
- Version: `X.Y.Z-fdroid` (e.g., 1.0.0-fdroid)
- No Sidekick features
- Published to F-Droid repository

### CI/CD Pipeline

**GitHub Actions Workflow:**
```yaml
# .github/workflows/build.yml
jobs:
  build-playstore:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build Play Store APK
        run: ./gradlew assemblePlaystore
      - name: Upload to Play Store
        # Upload to Play Console
        
  build-fdroid:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build F-Droid APK
        run: ./gradlew assembleFdroid
      - name: Upload to F-Droid
        # F-Droid build process
```

### User Communication

**In App:**
- Play Store version: Show Sidekick features prominently
- F-Droid version: Show community website link instead
- Clear messaging about feature availability

**In Store Listings:**
- Play Store: Highlight Sidekick integration
- F-Droid: Highlight open-source, privacy-first approach
- Different feature lists for each platform

### Maintenance Considerations

**Pros:**
- Maximize features for Play Store users
- Maintain open-source integrity for F-Droid
- Clear separation of concerns
- Easy to toggle features per platform
- Achievements work in BOTH versions (feature parity for core gameplay)
- Only Sidekick-specific features differ (sharing/comparison)

**Cons:**
- Dual codebase maintenance (but shared achievement core)
- Different release cycles possible
- Testing overhead (but core achievement logic tested once)
- Documentation needs to cover both versions
- Need to maintain interface contracts for achievement sync

### Recommended Rollout

**Phase 1: Shared Achievement & Streak Core**
- Develop `AchievementManager` and related classes in `src/main/`
- Develop `StreakManager` and related classes in `src/main/`
- Implement local storage (SharedPreferences) for both
- Create in-game UI for achievements and streaks
- Test unlock logic and streak tracking
- This is the foundation for ALL builds

**Phase 2: Sidekick Integration (Guarded by BuildConfig.ENABLE_SIDEKICK)**
- Implement `SidekickAchievementSync` in `src/main/` (guarded by flag)
- Implement `SidekickStreakSync` in `src/main/` (guarded by flag)
- Implement `StreakLeaderboard` in `src/main/` (guarded by flag)
- Implement `SidekickManager` in `src/main/` (guarded by flag)
- Integrate Google Play Games Services (optional dependency)
- Add build flavor configuration for Play Store and F-Droid
- Test with `ENABLE_SIDEKICK=true` for Play Store
- Test with `ENABLE_SIDEKICK=false` for F-Droid

**Phase 3: Build Flavor Configuration**
- Configure `playstore` flavor with `ENABLE_SIDEKICK=true`
- Configure `fdroid` flavor with `ENABLE_SIDEKICK=false`
- Remove Google Play Services from F-Droid flavor
- Verify all `BuildConfig.ENABLE_SIDEKICK` guards work correctly
- Test both flavors build successfully

**Phase 4: Release & Maintenance**
- Release Play Store version with full Sidekick features
- Release F-Droid version with local-only achievements/streaks
- Both use same version number (X.Y.Z)
- Monitor unlock rates and streak patterns across both versions
- Update both versions simultaneously when adding new features
- All code changes go to single `main` branch

## Available Sidekick Features for Roboyard

### 1. Gaming Tools (Immediately Available)
- **Screenshot**: Capture gameplay (note: solution sharing already integrated via roboyard.z11.de)
- **Screen Recording**: Record gameplay for sharing on YouTube/social media
- **YouTube Livestream**: Stream gameplay directly to YouTube
- **Do Not Disturb**: Minimize interruptions during gameplay

**Note on Solution Sharing**: Roboyard already has a community website (roboyard.z11.de) for sharing solutions. Sidekick should **NOT duplicate** this functionality. Instead:
- Direct players to the existing community website for solution sharing
- Use Sidekick only for built-in gaming tools (screenshot, recording, livestream)
- Keep solution sharing centralized on the community platform

### 2. Achievements System (Requires Implementation)
**Status**: Must be implemented before Sidekick can display achievements

#### Achievement Categories:

**Progression Achievements:**
- `level_1_complete` - Complete Level 1
- `level_10_complete` - Complete 10 levels
- `level_50_complete` - Complete 50 levels
- `level_140_complete` - Complete all 140 levels (Platinum)
- `all_stars_collected` - Collect all 420 stars

**Performance Achievements:**
- `perfect_solution_1` - Solve a level with optimal moves
- `perfect_solutions_10` - Solve 10 levels with optimal moves
- `perfect_solutions_50` - Solve 50 levels with optimal moves
- `speedrun_under_30s` - Complete a level in under 30 seconds
- `speedrun_under_10s` - Complete a level in under 10 seconds

**Challenge Achievements:**
- `no_hints_10` - Complete 10 levels without using hints
- `no_hints_50` - Complete 50 levels without using hints
- `solve_custom_level` - Solve a custom-created level
- `create_custom_level` - Create and save a custom level
- `share_custom_level` - Share a custom level with others

**Mastery Achievements:**
- `3_star_level` - Achieve 3 stars on a level
- `3_star_10_levels` - Achieve 3 stars on 10 levels
- `3_star_50_levels` - Achieve 3 stars on 50 levels
- `3_star_all_levels` - Achieve 3 stars on all 140 levels (Gold)

**Special Achievements:**
- `first_game` - Play your first game
- `daily_login_7` - Log in 7 days in a row
- `daily_login_30` - Log in 30 days in a row
- `comeback_player` - Return after 30 days of inactivity

**Random Game Achievements (Difficulty):**
- `impossible_mode_1` - Complete 1 game in Impossible mode
- `impossible_mode_5` - Complete 5 games in Impossible mode
- `impossible_mode_streak_5` - Complete 5 games in a row in Impossible mode with optimal moves
- `impossible_mode_streak_10` - Complete 10 games in a row in Impossible mode with optimal moves

**Random Game Achievements (Solution Length):**
- `solution_20_moves` - Complete a game with exactly 20 moves
- `solution_21_moves` - Complete a game with exactly 21 moves
- `solution_22_moves` - Complete a game with exactly 22 moves
- `solution_25_plus_moves` - Complete a game with 25+ moves
- `solution_30_plus_moves` - Complete a game with 30+ moves

**Random Game Achievements (Screen Resolutions):**
- `play_10_move_games_all_resolutions` - Play games with 10+ moves on all screen resolutions
- `play_12_move_games_all_resolutions` - Play games with 12+ moves on all screen resolutions
- `play_15_move_games_all_resolutions` - Play games with 15+ moves on all screen resolutions

**Random Game Achievements (Multiple Targets):**
- `game_2_targets` - Complete a game with 2 targets
- `game_3_targets` - Complete a game with 3 targets
- `game_4_targets` - Complete a game with 4 targets
- `game_2_of_3_targets` - Complete a game where you need 2 out of 3 targets
- `game_2_of_4_targets` - Complete a game where you need 2 out of 4 targets

**Random Game Achievements (Robot Count):**
- `game_5_robots` - Complete a game with 5 robots
- `game_4_robots_10_times` - Complete 10 games with 4 robots
- `game_5_robots_5_times` - Complete 5 games with 5 robots

**Random Game Achievements (Field Coverage):**
- `traverse_all_fields_1_robot` - Visit all fields on the board with 1 robot before reaching goal
- `traverse_all_fields_all_robots` - Visit all fields on the board with all robots before reaching goal
- `traverse_all_fields_5_games` - Visit all fields in 5 consecutive games

**Random Game Achievements (Streaks & Challenges):**
- `perfect_random_games_5` - Complete 5 random games with optimal moves
- `perfect_random_games_10` - Complete 10 random games with optimal moves
- `perfect_random_games_20` - Complete 20 random games with optimal moves
- `no_hints_random_10` - Complete 10 random games without using hints
- `no_hints_random_50` - Complete 50 random games without using hints

**Random Game Achievements (Speed):**
- `speedrun_random_under_20s` - Complete a random game in under 20 seconds
- `speedrun_random_under_10s` - Complete a random game in under 10 seconds
- `speedrun_random_5_games_under_30s` - Complete 5 random games in under 30 seconds each

### 3. Gaming Streaks (Available)
Track consecutive days of gameplay:
- Daily streak counter
- Streak milestones (7, 14, 30, 60, 100 days)
- Streak rewards (bonus stars, special cosmetics)
- Streak reset notifications

### 4. Play Points Integration (For Registered Developers)
- **Point Redemption**: Allow players to exchange earned points for in-game rewards
- **Play Points Actions**: Special offers and promotions
- **Pass Vouchers**: Discounted access to premium content

### 5. Quest System (For Registered Quest Developers)
**Daily Quests:**
- Complete 3 levels
- Solve 1 level with optimal moves
- Collect 10 stars
- Create 1 custom level

**Weekly Quests:**
- Complete 10 levels
- Solve 5 levels with optimal moves
- Collect 50 stars
- Share 2 custom levels

**Event Quests:**
- Limited-time challenges
- Seasonal events
- Community challenges

### 6. Game Tips (Available Q1 2026)
- Contextual tips during gameplay
- Strategy guides for difficult levels
- Hint system integration
- Tutorial reminders

### 7. Gemini Live (EAP Members Only)
- AI-powered gameplay assistance
- Strategy suggestions
- Real-time problem solving

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
- [ ] Integrate Google Play Games Services
- [ ] Set up Sidekick SDK
- [ ] Implement basic achievement system
- [ ] Test on internal/closed testing track

### Phase 2: Core Features (Weeks 3-4)
- [ ] Implement all achievement categories
- [ ] Set up gaming streaks tracking
- [ ] Integrate Play Points (if applicable)
- [ ] Test achievement unlock triggers

### Phase 3: Advanced Features (Weeks 5-6)
- [ ] Implement quest system
- [ ] Add game tips integration
- [ ] Set up analytics tracking
- [ ] Prepare for Play Store submission

### Phase 4: Optimization (Weeks 7-8)
- [ ] Gather user feedback
- [ ] Optimize achievement unlock rates
- [ ] Fine-tune quest difficulty
- [ ] Prepare for production release

## Technical Requirements

### Dependencies
```gradle
// Google Play Games Services
implementation 'com.google.android.gms:play-services-games:21.0.0'

// Google Play Services Auth
implementation 'com.google.android.gms:play-services-auth:21.0.0'

// Google Play Services Tasks
implementation 'com.google.android.gms:play-services-tasks:18.0.2'
```

### Key Classes to Implement
- `AchievementManager` - Handle achievement unlocking and tracking
- `StreakManager` - Track daily login streaks
- `QuestManager` - Manage daily/weekly quests
- `SidekickIntegration` - Main Sidekick integration point

### Data Persistence
- Store achievement progress in SharedPreferences
- Sync with Google Play Games Services
- Handle offline mode gracefully

## User Experience Flow

### Achievement Unlock Flow
1. Player completes achievement condition
2. `AchievementManager.unlockAchievement()` called
3. Achievement unlocked notification shown
4. Synced to Google Play Games Services
5. Visible in Sidekick overlay

### Streak Tracking Flow
1. Player logs in daily
2. `StreakManager.recordDailyLogin()` called
3. Streak counter incremented
4. Milestone rewards granted if applicable
5. Displayed in Sidekick overlay

### Quest Completion Flow
1. Player completes quest objective
2. `QuestManager.completeQuest()` called
3. Quest reward granted (stars, points, etc.)
4. Next quest auto-generated
5. Visible in Sidekick overlay

## Monetization Opportunities

### Free-to-Play Model
- Achievements and streaks drive engagement
- Daily quests encourage regular play
- Play Points redemption for cosmetics
- Optional ads for bonus rewards

### Premium Features (Future)
- Exclusive achievement badges
- Premium quest tracks
- Cosmetic rewards
- Ad-free experience

## Analytics & Metrics

### Key Metrics to Track
- Achievement unlock rate
- Average streak length
- Quest completion rate
- Play Points redemption rate
- Daily active users (DAU)
- Monthly active users (MAU)
- Retention rates

### Success Criteria
- 40%+ of players unlock first achievement
- Average 7-day streak for engaged players
- 60%+ quest completion rate
- 20%+ increase in DAU after Sidekick launch

## Testing Strategy

### Internal Testing
- Test all achievement unlock conditions
- Verify streak tracking accuracy
- Test offline mode behavior
- Validate Play Points integration

### Closed Testing
- Gather feedback from testers
- Monitor achievement unlock rates
- Identify balance issues
- Refine quest difficulty

### Open Testing
- Wider audience feedback
- Performance monitoring
- Bug identification
- User engagement metrics

## Play Store Submission

### Requirements
- Sidekick SDK properly integrated
- All achievements implemented and tested
- Privacy policy updated
- Terms of service updated
- Compliance with Google Play policies

### Checklist
- [ ] Google Play Games Services configured
- [ ] Sidekick SDK integrated
- [ ] All achievements implemented
- [ ] Streaks system working
- [ ] Quests system functional
- [ ] Analytics tracking enabled
- [ ] Privacy policy updated
- [ ] Terms of service updated
- [ ] Internal testing completed
- [ ] Closed testing completed
- [ ] Ready for production release

## Future Enhancements

### Q1 2026
- Game Tips integration
- Gemini Live (if EAP access granted)
- Advanced analytics

### Q2 2026
- Social features (leaderboards, multiplayer)
- Community challenges
- Seasonal events
- Limited-time achievements

### Q3 2026
- Cross-platform progression
- Cloud save integration
- Advanced Gemini features
- AI-powered recommendations

## References
- [Google Play Games Sidekick Documentation](https://developer.android.com/games/sidekick)
- [Google Play Games Services](https://developers.google.com/games/services)
- [Play Console Help](https://support.google.com/googleplay/android-developer)

## Notes
- Sidekick is currently in Beta - features may change
- Early Access Program (EAP) provides additional features
- Regular updates expected in Q1 2026
- Community feedback should drive feature prioritization

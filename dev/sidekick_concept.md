# Sidekick Integration Concept for Roboyard

## Overview
Google Play Games Sidekick (Beta) is an overlay that presents relevant content and offers directly in the game without requiring players to leave. This document outlines how Roboyard could integrate Sidekick to enhance user engagement and monetization.

## Current Status
- Sidekick (Beta) is available for testing on Android 13+
- Requires Google Play Games Services integration
- Features vary based on integration status and registration

## Distribution Strategy: Play Store vs F-Droid

### Important Consideration
**Sidekick is ONLY available on Google Play Store.** F-Droid users cannot access Sidekick features since:
- F-Droid doesn't include Google Play Services
- Sidekick requires Google Play Games Services SDK
- F-Droid focuses on open-source, privacy-first distribution

### Recommended Approach: Dual Branch Strategy

#### Branch 1: `main-playstore` (Google Play Store)
- Full Sidekick integration
- Google Play Games Services enabled
- Achievements with Sidekick sharing & comparison
- Streaks, quests, Play Points
- All monetization features
- Target: Play Store distribution

#### Branch 2: `main-fdroid` (F-Droid)
- NO Sidekick integration
- NO Google Play Services dependencies
- Achievements collected locally (in-game only)
- NO achievement sharing/comparison
- Streaks tracked locally (in-game only)
- NO streak comparison/leaderboards
- Community features via roboyard.z11.de
- Open-source focus
- Target: F-Droid distribution

**Important**: Achievements AND Streaks are available in BOTH branches, but with different functionality:
- Play Store: Shared via Sidekick, comparable with other players
- F-Droid: Local-only, visible in-game only

### Implementation Strategy

**Build Configuration:**
```gradle
// build.gradle
flavorDimensions "store"

productFlavors {
    playstore {
        dimension "store"
        // Include Google Play Services
        implementation 'com.google.android.gms:play-services-games:21.0.0'
    }
    
    fdroid {
        dimension "store"
        // NO Google Play Services
        // Community features only
    }
}
```

**Feature Flags:**
```java
// In code, check which flavor is being used
if (BuildConfig.FLAVOR.equals("playstore")) {
    // Initialize Sidekick
    initializeSidekick();
} else {
    // F-Droid: use community website only
    showCommunityWebsiteOption();
}
```

**Conditional Compilation:**
- Create separate source sets: `src/playstore/` and `src/fdroid/`
- `AchievementManager` in BOTH flavors (core achievement logic)
- `StreakManager` in BOTH flavors (core streak tracking)
- `SidekickAchievementSync` only in playstore flavor (Sidekick sharing/comparison)
- `SidekickStreakSync` only in playstore flavor (Sidekick streak sharing)
- `SidekickManager` only in playstore flavor
- `CommunityWebsiteManager` in both flavors
- Shared core game logic in `src/main/`

**Architecture for Achievements & Streaks:**
```
src/main/java/
├── roboyard/logic/achievements/
│   ├── AchievementManager.java          (SHARED - both flavors)
│   ├── Achievement.java                 (SHARED - data model)
│   ├── AchievementStorage.java          (SHARED - local storage)
│   └── AchievementUI.java               (SHARED - in-game display)
│
├── roboyard/logic/streaks/
│   ├── StreakManager.java               (SHARED - both flavors)
│   ├── Streak.java                      (SHARED - data model)
│   ├── StreakStorage.java               (SHARED - local storage)
│   └── StreakUI.java                    (SHARED - in-game display)
│
src/playstore/java/
├── roboyard/logic/achievements/
│   ├── SidekickAchievementSync.java     (PLAYSTORE ONLY)
│   └── AchievementComparison.java       (PLAYSTORE ONLY)
│
├── roboyard/logic/streaks/
│   ├── SidekickStreakSync.java          (PLAYSTORE ONLY)
│   └── StreakLeaderboard.java           (PLAYSTORE ONLY)
│
src/fdroid/java/
├── roboyard/logic/achievements/
│   └── AchievementStub.java             (FDROID - no-op implementations)
│
├── roboyard/logic/streaks/
│   └── StreakStub.java                  (FDROID - no-op implementations)
```

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
- This is the foundation for BOTH branches

**Phase 2: Play Store Implementation**
- Create `src/playstore/` flavor with Sidekick integration
- Implement `SidekickAchievementSync` for achievement sharing/comparison
- Implement `SidekickStreakSync` for streak sharing/leaderboard
- Implement `StreakLeaderboard` for streak comparison
- Integrate Google Play Games Services
- Release to Play Store with full Sidekick features
- Gather user feedback

**Phase 3: F-Droid Implementation**
- Create `src/fdroid/` flavor with stub implementations
- Implement `AchievementStub` (no-op for Sidekick)
- Implement `StreakStub` (no-op for Sidekick)
- Remove Google Play Services dependencies
- Release to F-Droid with local-only achievements and streaks
- Ensure feature parity for core gameplay

**Phase 4: Maintenance & Sync**
- Keep achievement and streak core logic in sync between branches
- Update both branches when new achievements/streaks are added
- Monitor unlock rates and streak patterns in both versions
- Gather feedback from both communities

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

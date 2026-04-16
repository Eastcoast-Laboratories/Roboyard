# Streak Workflow (Current State)

## Local Streak Management

**StreakManager.java** - SharedPreferences keys:
- `KEY_CURRENT_STREAK` - current consecutive login days
- `KEY_LAST_LOGIN_DATE` - last login as day-number since epoch
- `KEY_LONGEST_STREAK` - highest streak ever achieved
- `KEY_LONGEST_STREAK_DATE` - date string (yyyy-MM-dd) when longest was achieved

**`recordDailyLogin()`** logic:
- Same day → skip
- Previous day → `currentStreak++`
- Gap > 1 day → `currentStreak = 1` (streak lost, longest preserved)
- First ever → `currentStreak = 1`

## Server Data Storage

**DB table:** `user_achievement_stats`  
**Relevant columns:** `daily_login_streak`, `last_login_date` (date), `last_streak_date` (date), `longest_streak`, `longest_streak_date`

## Sync Flow

### Upload (Android → Server)
Triggered by: achievement unlock, game start  
**`AchievementManager.syncToServer()`** → `POST /api/mobile/achievements/sync`

Sends:
```java
stats.put("daily_login_streak", streakManager.getCurrentStreak());
stats.put("last_login_date", streakManager.getLastLoginDateString());   // date of last login
stats.put("last_streak_date", streakManager.getLastLoginDateString());  // same
stats.put("longest_streak", streakManager.getLongestStreak());
stats.put("longest_streak_date", streakManager.getLongestStreakDate());
```

Server logic (`AchievementService.updateStats()`):
- Checks if `last_login_date` on server is > 1 day old → resets server streak to 0
- Takes `max(server_streak, incoming_streak)`
- Takes `max(longest_streak, incoming_longest_streak)`
- Saves `last_login_date` from client

Response: `{success, synced_count, new_achievements, stats_updated}` — **no stats returned**

### Download (Server → Android)
Triggered by: **login only** (`LoginDialogHelper`)  
**`AchievementManager.syncFromServer()`** → `GET /api/mobile/achievements`

Server returns (via `getUserStats()`):
```json
{
  "daily_login_streak": 1,
  "last_login_date": "2026-04-16",
  "last_streak_date": "2026-04-16",
  "longest_streak": 11,
  "longest_streak_date": "2024-01-15"
}
```

Android processes in `restoreFromServer(serverStreak, serverLastLoginDate)`:
1. Checks if `serverLastDate` is > 1 day old → resets `serverStreak = 1`
2. Takes `max(serverStreak, localStreak)` for current streak
3. Takes `max(maxStreak, localLongest)` for longest streak
4. Updates `AchievementManager.dailyLoginStreak` in sync

## Long Absence + New Device Scenario

```
Old device (months ago):  streak=11, last_login_date=2024-01-15  →  uploaded to server
New device today:
  1. recordDailyLogin()         → local streak = 1
  2. Login → syncFromServer()   → server returns streak=11, last_login_date=2024-01-15
  3. restoreFromServer():
       daysSince = today - 2024-01-15 = 90+
       serverStreak reset to 1
       max(1, 1) = 1  ✅
  4. Result: current=1, longest=11  ✅
```

## Login Sync Order

`LoginDialogHelper` on successful login:
1. `syncFromServer()` — downloads server state, corrects stale streak locally
2. On success → `syncToServer()` — immediately uploads corrected state back to server

This ensures the website shows the correct streak right after login, without waiting for the next game.

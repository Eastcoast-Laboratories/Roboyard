# Streak Workflow (Current State)

## Local Streak Management

**Anforderungen an den Workflow:**

1. **Täglicher Upload**: Jedes Mal wenn der User  oder eine Achievement freischaltet, wird der lokale Streak zum Server hochgeladen (mit `last_login_date` des aktuellen Tages). dazu ist es nicht nötig ein spiel zu spielen

2. **Server-seitiger Upload-Verarbeitung**: Beim Upload prüft der Server ob sein gespeicherter Streak stale ist (basierend auf `last_login_date` oder Fallback auf `last_streak_date`/`updated_at`). Wenn stale, wird er auf 0 zurückgesetzt, sodass der eingehende Wert vom Client gewinnt. Wenn nicht stale, wird `max(server, incoming)` genommen. Der `longest_streak` wird immer mit `max(server, incoming)` aktualisiert und geht nie verloren.

3. **Login auf neuem Gerät**: Nach Login wird der Server-Streak heruntergeladen. Der Client prüft ob der Server-Streak stale ist (basierend auf `last_login_date`). Wenn stale, wird er lokal auf 1 zurückgesetzt. Der `longest_streak` wird vom Server wiederhergestellt (lokal ist es 0, also gewinnt der Server).

4. **Login auf bekanntem Gerät**: Nach Login wird der Server-Streak heruntergeladen und lokal wiederhergestellt. Der lokale Streak wird nicht verändert (Server ist Referenz). Kein Upload nötig. (PROBLEM)

5. **Offline-Spielen**: Wenn der User offline spielt und dann später synct, wird sein lokaler Streak (der durch `recordDailyLogin()` korrekt verwaltet wurde) hochgeladen und überschreibt den alten Server-Streak.
   
6. Wenn ein spieler online sich nicht eingeloggt hat, aber offline weiterspielt, dann soll online bei current streak ein hinswis, wann der "letzter login" war angezeigt werden, da der server nichdt weis, ob der spieler überhaupt noch am leben ist.

7. Alle Streaks beziehen sich immer auf die Zeitzone der Android app.


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

# Crash Analysis: Random Game After App Update

## Problem Description
- App crashes when clicking "Random Game" button after updating from old version
- Crash disappears after clearing app data and reinstalling
- Crash is NOT reproducible with fresh install
- **Root cause: Incompatible settings/preferences from old app version**

## Most Likely Causes (Ranked by Probability)

### 1. **Live Move Counter Feature (NEW in recent version)** ⚠️ HIGH PROBABILITY
**What changed:**
- New preference: `KEY_LIVE_MOVE_COUNTER_ENABLED` (boolean)
- New feature: `LiveSolverManager` with background thread executor
- New cache: `nextMovesCache` (ConcurrentHashMap)
- New executor: `preComputeExecutor` for pre-computation

**Potential crash scenarios:**
```java
// In GameStateManager.java line 356-361
try {
    liveMoveCounterEnabled = prefs.getBoolean(KEY_LIVE_MOVE_COUNTER_ENABLED, DEFAULT_LIVE_MOVE_COUNTER_ENABLED);
} catch (ClassCastException e) {
    // Old version might have stored this as String or Int
    Timber.e("[PREFERENCES] Error loading live move counter enabled: %s", e.getMessage());
    prefs.edit().remove(KEY_LIVE_MOVE_COUNTER_ENABLED).apply();
    liveMoveCounterEnabled = DEFAULT_LIVE_MOVE_COUNTER_ENABLED;
}
```

**Why it crashes:**
- If old version had a preference with same key but different type
- `LiveSolverManager` initialization might fail if context is null
- `preComputeExecutor` might not be initialized properly
- Background threads from old version might still be running

**Fix already in place:**
- `loadCachedValues()` has try-catch for ClassCastException
- BUT: `LiveSolverManager` initialization happens AFTER preferences load
- Crash might occur in `triggerLiveSolver()` if called before manager is ready

### 2. **Hint Auto Move Feature** ⚠️ MEDIUM PROBABILITY
**What changed:**
- New preferences: `KEY_HINT_AUTO_MOVE_ENABLED`, `KEY_HINT_AUTO_MOVE_MODE`
- These are loaded in `loadCachedValues()` with error handling

**Potential crash scenarios:**
```java
// Lines 364-377
try {
    hintAutoMoveEnabled = prefs.getBoolean(KEY_HINT_AUTO_MOVE_ENABLED, DEFAULT_HINT_AUTO_MOVE_ENABLED);
} catch (ClassCastException e) {
    // Handled
}

try {
    hintAutoMoveMode = prefs.getInt(KEY_HINT_AUTO_MOVE_MODE, DEFAULT_HINT_AUTO_MOVE_MODE);
} catch (ClassCastException e) {
    // Handled
}
```

**Why it might crash:**
- Old version might have incompatible hint system state
- Animation system might fail if old hint state is corrupted

### 3. **WallStorage Incompatibility** ⚠️ MEDIUM PROBABILITY
**What changed:**
- WallStorage saves/loads walls per board size
- Uses SharedPreferences with keys like `"walls_8x8"`

**Potential crash scenarios:**
```java
// In WallStorage.java
SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
String wallsData = prefs.getString(key, "");
```

**Why it crashes:**
- Old version might have corrupted wall data
- Board size mismatch between old and new version
- String parsing might fail on old format

### 4. **Board Size Validation** ⚠️ LOW PROBABILITY
**Potential crash scenarios:**
```java
// Lines 252-265
try {
    boardSizeWidth = prefs.getInt(KEY_BOARD_SIZE_WIDTH, DEFAULT_BOARD_SIZE_WIDTH);
} catch (ClassCastException e) {
    // Handled
}

try {
    boardSizeHeight = prefs.getInt(KEY_BOARD_SIZE_HEIGHT, DEFAULT_BOARD_SIZE_HEIGHT);
} catch (ClassCastException e) {
    // Handled
}
```

**Why it might crash:**
- Old version might have invalid board size (e.g., 0, negative, too large)
- Validation might be missing when loading from preferences

### 5. **Sound System Initialization** ⚠️ LOW PROBABILITY
**What changed:**
- New preferences: `KEY_BACKGROUND_SOUND_VOLUME`, `KEY_SOUND_EFFECTS_VOLUME`

**Why it might crash:**
- Sound system might fail to initialize with old volume values
- Audio resources might be missing or incompatible

## Recommended Fixes

### Fix 1: Add Null Check for LiveSolverManager
```java
// In GameStateManager.triggerLiveSolver()
public void triggerLiveSolver() {
    if (!liveMoveCounterEnabled) return;

    GameState state = currentState.getValue();
    if (state == null) return;

    // Don't solve if game is already complete
    if (Boolean.TRUE.equals(isGameComplete.getValue())) {
        liveMoveCounterText.setValue("");
        liveSolverCalculating.setValue(false);
        return;
    }

    // ADD THIS CHECK:
    if (context == null) {
        Timber.e("[LIVE_SOLVER] Context is null, cannot initialize LiveSolverManager");
        liveMoveCounterEnabled = false;
        return;
    }

    // Lazy-init the live solver manager
    if (liveSolverManager == null) {
        try {
            liveSolverManager = new LiveSolverManager();
        } catch (Exception e) {
            Timber.e(e, "[LIVE_SOLVER] Failed to initialize LiveSolverManager");
            liveMoveCounterEnabled = false;
            return;
        }
    }
    
    // ... rest of method
}
```

### Fix 2: Add Validation for Board Size
```java
// In Preferences.loadCachedValues()
try {
    boardSizeWidth = prefs.getInt(KEY_BOARD_SIZE_WIDTH, DEFAULT_BOARD_SIZE_WIDTH);
    // ADD VALIDATION:
    if (boardSizeWidth < 4 || boardSizeWidth > 16) {
        Timber.w("[PREFERENCES] Invalid board width: %d, resetting to default", boardSizeWidth);
        boardSizeWidth = DEFAULT_BOARD_SIZE_WIDTH;
        prefs.edit().putInt(KEY_BOARD_SIZE_WIDTH, boardSizeWidth).apply();
    }
} catch (ClassCastException e) {
    Timber.e("[PREFERENCES] Error loading board width: %s", e.getMessage());
    prefs.edit().remove(KEY_BOARD_SIZE_WIDTH).apply();
    boardSizeWidth = DEFAULT_BOARD_SIZE_WIDTH;
}

try {
    boardSizeHeight = prefs.getInt(KEY_BOARD_SIZE_HEIGHT, DEFAULT_BOARD_SIZE_HEIGHT);
    // ADD VALIDATION:
    if (boardSizeHeight < 4 || boardSizeHeight > 16) {
        Timber.w("[PREFERENCES] Invalid board height: %d, resetting to default", boardSizeHeight);
        boardSizeHeight = DEFAULT_BOARD_SIZE_HEIGHT;
        prefs.edit().putInt(KEY_BOARD_SIZE_HEIGHT, boardSizeHeight).apply();
    }
} catch (ClassCastException e) {
    Timber.e("[PREFERENCES] Error loading board height: %s", e.getMessage());
    prefs.edit().remove(KEY_BOARD_SIZE_HEIGHT).apply();
    boardSizeHeight = DEFAULT_BOARD_SIZE_HEIGHT;
}
```

### Fix 3: Add Try-Catch in startGame()
```java
// In GameStateManager.startGame()
public void startGame() {
    try {
        // Existing startGame() logic
        // ...
    } catch (Exception e) {
        Timber.e(e, "[GAME_START] Critical error starting game, resetting to defaults");
        
        // Reset preferences to defaults
        Preferences.resetToDefaults();
        
        // Try again with clean state
        try {
            // Retry startGame() logic
        } catch (Exception e2) {
            Timber.e(e2, "[GAME_START] Failed to start game even after reset");
            // Show error to user
            throw new RuntimeException("Failed to start game. Please clear app data.", e2);
        }
    }
}
```

### Fix 4: Clear Old Incompatible Preferences on Version Upgrade
```java
// In RoboyardApplication.onCreate() or Preferences.initialize()
public static void initialize(Context context) {
    // ... existing code ...
    
    // Check app version and clear incompatible preferences
    int currentVersion = BuildConfig.VERSION_CODE;
    int savedVersion = prefs.getInt("app_version_code", 0);
    
    if (savedVersion < 116 && currentVersion >= 116) {
        // Version 116 introduced live move counter
        // Clear potentially incompatible preferences
        Timber.w("[PREFERENCES] Upgrading from version %d to %d, clearing incompatible preferences", savedVersion, currentVersion);
        
        prefs.edit()
            .remove(KEY_LIVE_MOVE_COUNTER_ENABLED)
            .remove(KEY_HINT_AUTO_MOVE_ENABLED)
            .remove(KEY_HINT_AUTO_MOVE_MODE)
            .putInt("app_version_code", currentVersion)
            .apply();
    }
    
    // ... rest of initialization ...
}
```

## Testing Strategy

### 1. Simulate Old Version Upgrade
```bash
# Install old APK version
adb install -r roboyard_old_version.apk

# Use app and change settings
# - Enable/disable sound
# - Change board size
# - Play some games

# Install new APK version (without clearing data)
adb install -r roboyard_new_version.apk

# Try to start random game
# - Should NOT crash
# - Should migrate preferences gracefully
```

### 2. Test with Corrupted Preferences
```bash
# Manually corrupt SharedPreferences
adb shell
cd /data/data/de.z11.roboyard/shared_prefs
cat roboyard_preferences.xml

# Edit XML to add invalid values
# - Wrong type for boolean (string instead)
# - Invalid board size (0, -1, 999)
# - Missing required keys

# Restart app and test
```

### 3. Add Crash Reporting
```java
// In GameStateManager or Preferences
try {
    // Critical operation
} catch (Exception e) {
    // Log to Timber with full stack trace
    Timber.e(e, "[CRASH_ANALYSIS] Critical error: %s", e.getMessage());
    
    // Log all preferences for debugging
    Map<String, ?> allPrefs = prefs.getAll();
    for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
        Timber.d("[CRASH_ANALYSIS] Pref: %s = %s (%s)", 
            entry.getKey(), 
            entry.getValue(), 
            entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
    }
    
    // Re-throw or handle gracefully
}
```

## Conclusion

**Most likely cause:** LiveSolverManager initialization failure due to:
1. Old version had incompatible preference with same key
2. Context was null during initialization
3. Background thread executor failed to start
4. Pre-computation cache had corrupted data

**Recommended immediate fix:**
1. Add null checks and try-catch in `triggerLiveSolver()`
2. Add version-based preference migration in `Preferences.initialize()`
3. Add validation for all numeric preferences (board size, volumes, etc.)
4. Add comprehensive error logging for crash analysis

**Long-term solution:**
1. Implement preference migration system with version tracking
2. Add unit tests for preference loading with invalid data
3. Add integration tests for app upgrades
4. Consider using Room database instead of SharedPreferences for complex data

# Code Restructuring: KMP Preparation

## Overview
The `roboyard.logic.*` package is being refactored for Kotlin Multiplatform (KMP) 
compatibility to enable iOS sharing. This involves abstracting Android-specific 
dependencies behind platform-agnostic interfaces.

**Last Updated:** June 10, 2026  
**Status:** 18 of 20 major steps completed

## Completed Tasks ✅

### 1. Misplaced classes moved
| Class | From | To | Status |
|-------|------|----|--------|
| `MinimapGenerator.kt` | `logic.graphics` | `ui.graphics` | ✅ |
| `PlayGamesManager.kt` | `logic.managers` | `platform` | ✅ |

### 2. Trivial replacements
| Task | Files | Status |
|------|-------|--------|
| Color constants (android.graphics.Color → ARGB) | Constants.kt, GameLogic.kt | ✅ |
| Base64 (android.util → java.util) | GameStateManager.kt | ✅ |
| Handler/Looper → Coroutines | ApiClient, AchievementManager, GameStateManager | ✅ |

### 3. PlatformStorage abstraction
| File | Status |
|------|--------|
| `PlatformStorage.kt` interface | ✅ Created |
| `AndroidStorage.kt` implementation | ✅ Created |
| `FileReadWrite.kt` | ✅ Migrated |
| `WallStorage.kt` | ✅ Migrated |
| `StreakManager.kt` | ✅ Migrated |
| `LevelCompletionManager.kt` | ✅ Migrated |
| `SyncManager.kt` | ✅ Migrated |
| `RoboyardApiClient.kt` | ✅ Migrated |
| `PlayGamesManager.kt` | ✅ Already in platform package |
| `AchievementManager.kt` | ✅ Migrated |
| `GameHistoryManager.kt` | ✅ Partially migrated (imports ready) |

### 4. Network abstraction
| Component | Status |
|-----------|--------|
| `NetworkMonitor.kt` interface | ✅ Created |
| `AndroidNetworkMonitor.kt` | ✅ Created |
| SyncManager migrated to NetworkMonitor | ✅ |

### 5. UI abstraction
| Component | Status |
|-----------|--------|
| `UiNotifier.kt` interface | ✅ Created |
| `AndroidUiNotifier.kt` | ✅ Created |
| `StringProvider.kt` interface | ✅ Created |
| `AndroidStringProvider.kt` | ✅ Created |
| AchievementManager UiNotifier | ✅ Updated |
| LevelCompletionManager UiNotifier | ✅ Updated |
| AchievementCategory StringProvider | ✅ Updated |

## Remaining Tasks ⏳

### POSTPONED - Very Complex (requires architectural changes)

1. **GameState.kt** (2,772 lines)
   - **Problem:** Direct `MainActivity.boardSizeX` / `MainActivity.boardSizeY` references
   - **Impact:** GameState reads/writes static fields from MainActivity
   - **Solution:** Introduce BoardSizeProvider interface, inject into GameState

2. **GameStateManager.kt** (5,000+ lines)
   - **Problem:** `AndroidViewModel` with 32× `LiveData`, Toast, ToggleButton, MotionEvent, Bitmap
   - **Impact:** Core game logic tightly coupled to Android UI framework
   - **Solution:** 
     - Split into `GameStateManagerCore` (platform-free, in logic)
     - Create `GameStateManagerViewModel` (thin Android wrapper in ui)
     - Replace LiveData with StateFlow
     - Replace Toast with UiNotifier
     - Abstract all UI references behind interfaces

3. **Preferences.kt** (1,246 lines) - POSTPONED
   - Central preferences manager with many direct SharedPreferences calls
   - Complex due to numerous get/put operations for various types

4. **DataExportImportManager.kt** - POSTPONED
   - Complex file system and SharedPreferences operations
   - Multiple file operations require careful abstraction

## New Platform Interfaces

### PlatformStorage (roboyard.logic.storage)
```kotlin
interface PlatformStorage {
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
    fun clear()
}
```

### NetworkMonitor (roboyard.logic.network)
```kotlin
interface NetworkMonitor {
    fun isNetworkAvailable(): Boolean
}
```

### UiNotifier (roboyard.logic.ui)
```kotlin
fun interface UiNotifier {
    fun showMessage(message: String)
}
```

### StringProvider (roboyard.logic.ui)
```kotlin
fun interface StringProvider {
    fun getString(name: String): String?
}
```

## Build Verification
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"
```

Both must stay green after each step.

## Notes
- The remaining tasks (GameState, GameStateManager split) require significant 
  architectural refactoring and careful testing to maintain game behavior.
- Consider tackling these in a dedicated session with full regression testing.

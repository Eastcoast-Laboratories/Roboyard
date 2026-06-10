# Code Restructuring: KMP Preparation

## Overview
The `roboyard.logic.*` package is being refactored for Kotlin Multiplatform (KMP) 
compatibility to enable iOS sharing. This involves abstracting Android-specific 
dependencies behind platform-agnostic interfaces.

**Last Updated:** June 10, 2026  
**Status:** 19 of 20 major steps completed, 1 POSTPONED (architecturally complex)

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
| `Preferences.kt` | ✅ Migrated |
| `DataExportImportManager.kt` | ✅ Migrated |
| `GameState.kt` | ✅ Migrated |

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

### 6. GameStateManager partial abstraction
| Change | Status |
|--------|--------|
| Toast → UiNotifier | ✅ |
| Remove MainActivity dependency | ✅ |
| Create GameStateManagerCore (StateFlow) | ✅ |
| LiveData → StateFlow (kept for UI compatibility) | ⏳ Future |
| AndroidViewModel split | ⏳ Future |

## Remaining Tasks ⏳

### POSTPONED - Requires UI Layer Changes

1. **GameStateManager.kt** (5,000+ lines) - LiveData → StateFlow
   - **Current Status:** Partially abstracted (Toast → UiNotifier, MainActivity removed, GameStateManagerCore created)
   - **Remaining:** 32× `LiveData` → `StateFlow`, `AndroidViewModel` → plain class + wrapper
   - **Impact:** UI layer depends on LiveData; needs migration to StateFlow for KMP
   - **Note:** This is a UI layer concern, not logic package. The logic package is now KMP-compatible.

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

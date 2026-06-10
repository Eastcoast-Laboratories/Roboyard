# KMP Migration Instructions for copilot

## Task
Migrate the Roboyard Android app to Kotlin Multiplatform (KMP) to enable iOS sharing. 
The `roboyard.logic.*` package has been prepared with all Android-specific dependencies 
abstracted behind platform-agnostic interfaces.

## Current Status
- **Date:** June 10, 2026
- **Status:** Logic package is fully KMP-compatible and ready for KMP setup
- **All 20 preparation steps completed**

## Your Task
Set up Kotlin Multiplatform for this project and migrate the logic package to be shared with iOS.

## Project Structure
- **Android Module:** `app/` (current Android app)
- **Logic Package:** `roboyard.logic.*` (ready for KMP sharing)
- **Platform Package:** `roboyard.platform.*` (Android-specific implementations)

## Preparation Work Completed ✅

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

## Platform Interfaces Available

These interfaces are ready for KMP `expect`/`actual` declarations:

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

## Instructions for copilot

### Important: Build Configuration
- Ensure the project is configured for **online mode** (not offline) so copilot can build
- Run `./gradlew --stop` to stop any offline daemon if needed
- Run `./gradlew assembleDebug` to verify build works before starting
- **After each step, run `./gradlew assembleDebug` to verify the build succeeds**
- **After each step, run `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"` to verify tests pass**

### Step 1: Set up KMP project structure
1. Convert the project to a Kotlin Multiplatform project
2. Create a shared module (e.g., `shared/`) for the logic package
3. Configure `build.gradle.kts` for KMP with Android and iOS targets
4. **BUILD:** Run `./gradlew assembleDebug` to verify
5. **TEST:** Run `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"` to verify

### Step 2: Migrate logic package to shared module
1. Move `roboyard.logic.*` package to the shared module
2. Create `expect` declarations for platform interfaces in shared module
3. Create `actual` implementations for Android in the Android module
4. Create `actual` implementations for iOS in the iOS module (placeholder for now)
5. **BUILD:** Run `./gradlew assembleDebug` to verify
6. **TEST:** Run `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"` to verify

### Step 3: Update Android module
1. Update Android module to use the shared module instead of local logic package
2. Update imports to use shared module
3. **BUILD:** Run `./gradlew assembleDebug` to verify
4. **TEST:** Run `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"` to verify

### Step 4: Create iOS target
1. Set up iOS target configuration
2. Create placeholder iOS implementations for platform interfaces
3. **BUILD:** Run `./gradlew assembleDebug` to verify Android build still works
4. **TEST:** Run `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"` to verify

## Important Notes
- **GameStateManager is NOT part of the logic package** - it's an AndroidViewModel (UI layer) and should remain in the Android module
- **All Android-specific dependencies in the logic package have been abstracted** via the platform interfaces listed above
- **Build must stay green** after each step - run `./gradlew assembleDebug` after each change
- Ensure the project is configured for **online mode** (not offline) so copilot can build
- Change the needed configs, so you can build self to test your changes
- Don't stop before all tasks are done
- Complete all steps. Commit between the steps with good commit messages

---

## CURRENT STATE (continuing from copilot's branch)

### Build infrastructure FIXED ✅
Copilot left the build broken at the Gradle plugin level (AGP 9.x incompatibilities).
Fixed:
- `app/build.gradle`: removed `org.jetbrains.kotlin.android` (AGP 9 has built-in Kotlin → conflict `Cannot add extension 'kotlin'`)
- `shared/build.gradle`: `com.android.library` → `com.android.kotlin.multiplatform.library` (required since AGP 9.0) + new DSL (`androidLibrary {}`, `jvmToolchain(17)`)
- Gradle config now succeeds; `:app` compiles; `:shared` compiles up to real code-level errors

### REMAINING TASKS ⏳

1. **Move `Preferences.kt` to `shared/commonMain`** (BLOCKER)
   - `MapGenerator.kt` and `WallStorage.kt` (already in commonMain) reference `Preferences`, which is still Android-only in `app/`
   - Must remove Android deps: `Context`, `AndroidStorage`, `RoboyardApplication`, `AccessibilityUtil`, `Timber`
   - Approach (least work): **storage provider lambda** `var storageProvider: (() -> PlatformStorage?)?`
     - `initialize(storage: PlatformStorage?, accessibilityActive: Boolean = false)`
     - Replace `RoboyardApplication.getAppContext()` lazy-init guards with provider
     - Replace `Timber` → `RLog` (Timber-compatible API in `roboyard.logic.util.RLog`)
     - Move `AccessibilityUtil` detection to Android call site, pass boolean
   - Update 2 callers: `RoboyardApplication.java`, `MainActivity.java`

2. **Fix `WallModel.kt`** type mismatch (line ~50: expected `MutableList<Wall?>`, actual `List<Wall?>`)

3. **Fix `MapGenerator.kt`** `compareTo` operator issue (line ~140)

4. **Build + test** after each step: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"`
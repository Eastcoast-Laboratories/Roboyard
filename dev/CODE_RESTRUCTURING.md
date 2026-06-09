# Code Restructuring: KMP Preparation

## Overview
The whole `roboyard.logic.*` package is now Kotlin and `Activity` parameters were
abstracted (commit b278c3dd). However, 17 of the logic files still import `android.*`
and cannot be shared with iOS yet. Status as of code analysis (grep for
`^import android` in `roboyard/logic/`).

## Misplaced classes (move out of logic first)
These are not platform-independent logic and should move before abstracting:

| Class | Problem | Target |
|-------|---------|--------|
| `logic.graphics/MinimapGenerator.kt` | Pure rendering (`Bitmap`, `Canvas`, `Paint`) | `roboyard.ui.graphics` |
| `logic.managers/PlayGamesManager.kt` | Google Play Games = Android-only service | `roboyard.platform` or `ui` |
| `logic.managers/GameStateManager.kt` | Is an `AndroidViewModel` with `Toast`, `ToggleButton`, `MotionEvent`, `findNavController`, `Bitmap`, 32× `LiveData`, 35 refs to UI classes | Split: platform-free core stays in logic, ViewModel wrapper → `roboyard.ui` |
| `FileReadWrite.writeBitmap()/readBitmap()` | `Bitmap` I/O inside otherwise abstractable storage class | Extract to UI/platform helper |

## Remaining Android deps to abstract (per theme)

### 1. Storage: `Context` + `SharedPreferences` (biggest cluster, 13 files)
`Preferences.kt`, `WallStorage.kt`, `GameState.kt`, `FileReadWrite.kt`,
`StreakManager.kt`, `AchievementManager.kt`, `AchievementCategory.kt`,
`GameHistoryManager.kt`, `DataExportImportManager.kt`, `LevelCompletionManager.kt`,
`SyncManager.kt`, `RoboyardApiClient.kt`, `PlayGamesManager.kt`
→ Introduce one `PlatformStorage` interface (key-value + file I/O), Android impl
backed by `Context`/`SharedPreferences`. Alternative: `multiplatform-settings` library.

### 2. Network: `HttpURLConnection` (1 file)
`RoboyardApiClient.kt` (SyncManager only uses `ConnectivityManager`)
→ Migrate to Ktor client, or wrap behind an `HttpClient` interface.
`ConnectivityManager` in SyncManager → `NetworkMonitor` interface.

### 3. UI leakage: `Toast` (3 files)
`AchievementManager.kt`, `LevelCompletionManager.kt`, `GameStateManager.kt`
→ The `UiNotifier` pattern already exists in AchievementManager — reuse it everywhere.

### 4. Trivial replacements
- `android.graphics.Color` constants in `Constants.kt`/`GameLogic.kt` → plain ARGB
  Int constants (`0xFFFF0000.toInt()` etc.).
- `android.util.Base64` in `GameStateManager.kt` → `kotlin.io.encoding.Base64`.
- `Handler(Looper.getMainLooper())` (ApiClient, AchievementManager, GameStateManager)
  → `kotlinx.coroutines` `Dispatchers.Main`.
- `android.os.Build` (version strings) → constant/provider.
- `R.string.*` in `AchievementManager.kt`/`GameStateManager.kt` → `StringProvider`
  interface (string resources are platform-specific).

### 5. GameStateManager (do last, biggest)
- `LiveData`/`MutableLiveData` (32×) → `kotlinx.coroutines.flow.StateFlow`.
- `AndroidViewModel` → split into platform-free logic class + thin Android
  `ViewModel` wrapper in `roboyard.ui`.
- UI refs (`GameGridView`, `RobotAnimationManager`, `Activity`, navigation)
  → callback/listener interfaces owned by the UI layer.

## Suggested order (lowest risk first)
1. Move misplaced classes (`MinimapGenerator`, `PlayGamesManager`).
2. Trivial replacements (Color, Base64).
3. `PlatformStorage` abstraction + migrate the 13 storage users one by one.
4. `UiNotifier` reuse for all Toasts; `StringProvider` for resources.
5. Network abstraction (Ktor or interface).
6. `GameStateManager` split (LiveData→StateFlow, ViewModel wrapper).

After each step: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` must stay green.

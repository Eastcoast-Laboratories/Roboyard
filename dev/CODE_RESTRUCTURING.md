# KMP Migration Status for Roboyard

## Current Status
- **Date:** June 16, 2026
- **Status:** Core logic package migrated to KMP (shared/commonMain), DriftingDroids solver converted to Kotlin
- **Build:** ✅ Successful (Android build passing)

## Completed Work ✅

### Core Logic Package (shared/commonMain)
The following core logic classes are now KMP-compatible and shared between Android and iOS:

- **Preferences.kt** - Migrated with storageProvider lambda for KMP compatibility
- **GameLogic.kt** - Timber → RLog, companion object consolidated
- **MapObjects.kt** - SHA-256 ID generation, extractDataFromString
- **LevelFormatParser.kt** - Comment handling for #-prefixed lines
- **LevelCompletionData.kt** - Stars clamping (0-3), toString()
- **WallModel.kt** - Type mismatch fixed (toList → toMutableList)
- **GridElement.kt** - Platform-agnostic
- **GameMove.kt** - Platform-agnostic
- **Constants.kt** - Platform-agnostic
- **GameState.kt** - Platform-agnostic
- **MapGenerator.kt** - Platform-agnostic
- **WallStorage.kt** - Platform-agnostic

### Android-Specific Changes
- **app/build.gradle** - minSdk increased from 21 to 23 (Kermit 2.1.0 requirement)
- **RoboyardApplication.java** - Updated to use new Preferences API
- **MainActivity.java** - Updated to use new Preferences API

### Build Configuration
- **shared/build.gradle** - KMP setup with iosX64, iosArm64, iosSimulatorArm64
- **.gitignore** - Added /shared/build to ignore build artifacts

## Remaining Android-Specific Files (Stay in app/)

The following files have Android-specific dependencies and remain in the Android module:

### UI Layer / Managers
- **GameStateManager.kt** - AndroidViewModel, LiveData (UI-specific)
- **GameHistoryManager.kt** - Context, File, Timber (Android Storage)

### Network
- **RoboyardApiClient.kt** - HttpURLConnection, Context (Android Network)

### Storage
- **FileReadWrite.kt** - Context, File, Bitmap (Android File I/O)

### Achievements
- **AchievementManager.kt** - Activity, Toast, PlayGames (Android Achievements)

## DriftingDroids Solver (Kotlin - Converted, Android Dependencies to Remove)

The DriftingDroids solver package has been converted to Kotlin but still has Android dependencies:

**Location:** `app/src/main/java/driftingdroids/model/`

**Files (all .kt now):**
- Board.kt - Uses android.os.Build, roboyard.ui.activities.MainActivity ❌
- KeyDepthMap.kt - Platform-agnostic ✅
- KeyDepthMapFactory.kt - Platform-agnostic ✅
- KeyDepthMapTrieGeneric.kt - Platform-agnostic ✅
- KeyDepthMapTrieSpecial.kt - Platform-agnostic ✅
- KeyMakerInt.kt - Platform-agnostic ✅
- KeyMakerLong.kt - Platform-agnostic ✅
- L10N.kt - Platform-agnostic ✅
- Logger.kt - Platform-agnostic ✅
- Move.kt - Platform-agnostic ✅
- Solution.kt - Platform-agnostic ✅
- Solver.kt - Platform-agnostic ✅
- SolverIDDFS.kt - Platform-agnostic ✅

**Status:** Converted to Kotlin ✅, but needs Android dependency removal for KMP:
1. ❌ Remove android.os.Build import from Board.kt
2. ❌ Remove roboyard.ui.activities.MainActivity import from Board.kt
3. ❌ Move DriftingDroids package to shared/commonMain/driftingdroids/model/
4. ❌ Update shared/build.gradle to include DriftingDroids package
5. ❌ Test KMP build with DriftingDroids included

## iOS Integration

The shared module is configured for iOS with:
- **iosX64** - iOS Simulator (Intel)
- **iosArm64** - iOS Device (ARM64)
- **iosSimulatorArm64** - iOS Simulator (Apple Silicon)

**iOS-specific implementations (placeholders):**
- **IosStorage.kt** - Currently uses in-memory map, needs UserDefaults/FileManager implementation ❌
- **IosNetworkMonitor.kt** - Currently returns true, needs NWPathMonitor implementation ❌

**Next steps for iOS:**
1. ❌ Remove Android dependencies from DriftingDroids (Board.kt)
2. ❌ Move DriftingDroids to shared/commonMain
3. ❌ Implement IosStorage.kt with platform.Foundation.NSUserDefaults
4. ❌ Implement IosNetworkMonitor.kt with platform.Network.NWPathMonitor
5. ❌ Create iOS Xcode project (SwiftUI/UIKit)
6. ❌ Configure shared module as CocoaPods or SPM dependency
7. ❌ Create iOS UI layer that calls shared logic
8. ❌ Test iOS build and run on simulator
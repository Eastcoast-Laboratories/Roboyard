# KMP Migration Status for Roboyard

## Current Status
- **Date:** June 11, 2026
- **Status:** Core logic package migrated to KMP (shared/commonMain)
- **Build:** ✅ Successful (all 59 smoke tests passing)

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

## DriftingDroids Solver (Java - Not Yet Migrated)

The DriftingDroids solver package is still in Java and has Android dependencies:

**Location:** `app/src/main/java/driftingdroids/model/`

**Files:**
- Board.java - Uses android.util.Log, timber.log.Timber
- KeyDepthMap.java
- KeyDepthMapFactory.java
- KeyDepthMapTrieGeneric.java
- KeyDepthMapTrieSpecial.java
- KeyMakerInt.java
- KeyMakerLong.java
- L10N.java
- Logger.java
- Move.java
- Solution.java
- Solver.java
- SolverIDDFS.java

**Status:** Not yet migrated to Kotlin/KMP. This is a separate solver component that may need:
1. Java → Kotlin migration
2. Android dependency removal (Log → RLog)
3. KMP integration if shared with iOS

## iOS Integration

The shared module is configured for iOS with:
- **iosX64** - iOS Simulator (Intel)
- **iosArm64** - iOS Device (ARM64)
- **iosSimulatorArm64** - iOS Simulator (Apple Silicon)

**iOS-specific implementations:**
- **IosStorage.kt** - UserDefaults/FileManager based storage
- **IosNetworkMonitor.kt** - iOS network monitoring

**Next steps for iOS:**
1. Create iOS Xcode project (SwiftUI/UIKit)
2. Configure shared module as CocoaPods or SPM dependency
3. Complete IosStorage.kt implementation
4. Create iOS UI layer that calls shared logic
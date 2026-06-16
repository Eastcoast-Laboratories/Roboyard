# iOS Integration Guide for Roboyard

This guide explains the current status of iOS integration and how to test the Compose Multiplatform UI on iOS.

## Current Status

### Shared Module (Kotlin Multiplatform)

The shared module is configured for iOS with the following targets:
- **iosX64** - iOS Simulator (Intel)
- **iosArm64** - iOS Device (ARM64)
- **iosSimulatorArm64** - iOS Simulator (Apple Silicon)

**Platform-agnostic (shared/commonMain):**
- Core logic package (GameLogic, MapObjects, LevelFormatParser, etc.)
- DriftingDroids solver package (Board, Solver, Solution, etc.)
- Storage and network interfaces

**iOS-specific (shared/iosMain):**
- **IosStorage.kt** - Implemented with NSUserDefaults and NSFileManager
- **IosNetworkMonitor.kt** - Implemented with NWPathMonitor

### Compose Multiplatform UI (composeApp)

The Compose Multiplatform UI module is configured for iOS with the same targets as the shared module.

**Implemented Screens (commonMain):**
- Main Menu
- Game Board (Canvas rendering, drag gestures, solver hints)
- Level Selection (140 levels)
- Settings (Difficulty, Robot Count, Sound)
- Help (Rules, Tips)
- Credits (Attribution)
- Save/Load (Game states)
- Achievements (8 achievements)

**Note:** iOS targets cannot be built on Linux (Kotlin/Native limitation). Desktop (JVM) target works fully on Linux.

## Testing on Linux (Desktop)

The Compose Multiplatform UI can be fully developed and tested on Linux using the Desktop target.

### Build and Run

```bash
cd /var/www/Roboyard

# Compile Desktop target
./gradlew :composeApp:desktopMainClasses

# Run the application
./gradlew :composeApp:run

# Create distributable
./gradlew :composeApp:createDistributable
```

The distributable is created at: `composeApp/build/compose/binaries/main/app/composeApp/bin/composeApp`

### Verification

- All screens render correctly
- Robot movement via drag gestures works
- Solver hints display correctly
- Navigation between screens works
- Settings toggles function properly

## Testing on iOS (requires macOS)

To test on iOS, you need a macOS machine with Xcode 15.0 or later.

### Build iOS Target

```bash
cd /var/www/Roboyard

# Build iOS framework
./gradlew :composeApp:linkReleaseFrameworkIosArm64

# Build iOS Simulator framework
./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64
```

### Integration Steps (Future)

When ready to integrate with an iOS Xcode project:

1. **Create a new iOS project** in Xcode (SwiftUI or UIKit)

2. **Add the Compose Multiplatform framework**:
   - Build the iOS framework using the commands above
   - Add the framework to your Xcode project
   - Configure the app to use Compose Multiplatform

3. **Initialize the Compose UI**:
   ```swift
   import ComposeApp
   import UIKit
   
   class ViewController: UIViewController {
       override func viewDidLoad() {
           super.viewDidLoad()
           let contentView = MainViewControllerKt.MainViewController()
           addChild(contentView)
           view.addSubview(contentView.view)
           contentView.view.frame = view.bounds
           contentView.didMove(toParent: self)
       }
   }
   ```

## Architecture

```
Roboyard/
├── app/                          # Existing Android app (Fragments)
├── shared/                       # KMP shared logic
│   ├── commonMain/              # Platform-agnostic code
│   ├── iosMain/                 # iOS-specific implementations
│   └── desktopMain/             # Desktop-specific implementations
└── composeApp/                  # Compose Multiplatform UI
    ├── commonMain/              # Shared UI (works on Desktop, iOS, Android)
    ├── iosMain/                 # iOS-specific UI (empty, uses commonMain)
    └── desktopMain/             # Desktop-specific UI (Main.kt entry point)
```

## Notes

- iOS targets are configured but cannot be built on Linux
- Desktop target provides full UI development capability on Linux
- The same Compose UI code will run on iOS once built on macOS

# Concept: Big-Bang Migration – Replace Fragments with One Compose UI

**Strategy: full one-step replacement.** The Android Fragments are removed and replaced by the shared Compose `App()` from `composeApp/commonMain`. No coexistence, no `AndroidView` interop, no intermediate steps still running on Fragments. The working Fragment-based version stays available in git history.

The same Compose UI then runs on **Android, iOS, and Desktop** from a single codebase.

## Why Compose Replaces the Fragments

Compose Multiplatform targets Android natively (Jetpack Compose *is* Android's UI framework). The identical `commonMain` UI runs on:
- **Android** – Jetpack Compose (native)
- **iOS** – Compose via Skia
- **Desktop** – Compose via Skia (JVM)

There is no separate "Android UI" – the shared Compose code *is* the Android UI.

## How the Android UI Currently Works (what must be translated)

The game board is a single custom `View` (`GameGridView`, ~2400 lines) that draws everything onto an Android `Canvas` using `Drawable` PNG assets:

| Element | Android Implementation | Asset(s) |
| --- | --- | --- |
| Grid tiles | `gridTileDrawable`, rotated per tile | `R.drawable.grid_tiles` |
| Center logo | `backgroundLogo` in 2x2 carree | `R.drawable.roboyard` |
| Robots | `drawRobotWithGraphics()`, left/right sprites | `robot_{color}_left/right.png` |
| Walls | `WallRenderer` with horizontal/vertical drawables | `R.drawable.mh`, `R.drawable.mv` |
| Targets | `getTargetDrawable()` per color | `target_{color}.png`, `target_multi.png` |
| Robot paths | `Canvas.drawLine()` with per-color `Paint` | (drawn) |
| Hint arrow | `drawHintArrow()` | (drawn) |
| Animations | `ValueAnimator` scaling robots, move animation | (programmatic) |
| Touch | swipe/drag gestures, robot selection | (programmatic) |
| Accessibility | TalkBack support, focus, move buttons | (programmatic) |

The menu/settings/help screens (`MainMenuFragment`, `SettingsFragment`, etc.) are standard XML layouts in `app/src/main/res/layout/`.

Compose's `Canvas` API (`drawImage`, `drawLine`, `drawRect`, `rotate`) maps almost 1:1 to Android's `Canvas`, so the board migration is a **translation**, not a redesign.

## Foundational Principle: Reuse the Shared Logic (applies to every step)

Game logic, board model, solver, and `GameState` are already in `shared/commonMain` and are **not** rewritten. Throughout the entire migration, the Compose UI:
- Reads state from the same `GameState` / `Board` classes
- Calls the same `SolverIDDFS` for hints
- Uses the real move logic instead of any placeholder

Only the **rendering layer** is migrated, not the logic. This principle underlies all steps below.

## Big-Bang Migration Steps

### 1. Enable `androidTarget()` in composeApp ✅ DONE

`composeApp` now builds for Android via the `com.android.kotlin.multiplatform.library`
plugin with `androidLibrary {}`. The AGP 9.2.1 incompatibility was resolved by bumping
Compose Multiplatform to **1.9.3** (1.8.2 crashed with
`KotlinMultiplatformAndroidComponentsExtension.onVariant`). Verified:
`./gradlew :composeApp:compileAndroidMain` builds cleanly.

### 2. Share the graphic assets ✅ DONE

All robot/wall/target/grid/logo PNGs are copied into
`composeApp/src/commonMain/composeResources/drawable/` and accessed via
`imageResource(Res.drawable.*)`.

### 3. Rewrite the board renderer ✅ DONE

`BoardCanvas` in `GameScreen.kt` draws grid tiles (rotated), the center logo, targets,
walls (`mh`/`mv`) and robots with the shared PNG sprites via `DrawScope.drawImageScaled`.
Verified running on Linux Desktop (`./gradlew :composeApp:run`).

### 4-5. Menu/settings parity + delete legacy UI ⏳ NOT YET

These steps are intentionally **not** executed yet: the Compose menu/level/save-load/
settings screens in `App.kt` are still placeholders (e.g. level selection and save/load
load a random board, settings are not persisted). Deleting the working Fragment UI and
pointing `MainActivity` at `App()` before these screens reach feature parity would leave
the Android app non-functional. Complete step 4 (real screens + persistence + a11y)
before performing the destructive step 5.

---

### Original step list (kept for reference)

### (old) 2. Share the graphic assets

Copy the drawables from `app/src/main/res/drawable/` into Compose Multiplatform resources:

```
composeApp/src/commonMain/composeResources/drawable/
├── robot_{pink,green,blue,yellow,silver}_left.png
├── robot_{pink,green,blue,yellow,silver}_right.png
├── grid_tiles.png
├── roboyard.png        (center logo)
├── mh.png / mv.png     (walls)
└── target_{pink,green,blue,yellow,silver,multi}.png
```

Add `compose.components.resources` and access via `painterResource(Res.drawable.*)`.

### 3. Rewrite the board renderer with full feature parity

Translate `GameGridView.onDraw()` into the Compose `Canvas` in `GameScreen.kt`, including ALL features:

| Android (`Canvas`) | Compose (`DrawScope`) |
| --- | --- |
| `drawable.setBounds(...); drawable.draw(canvas)` | `drawImage(imageBitmap, dstOffset, dstSize)` |
| `canvas.rotate(angle, cx, cy)` | `rotate(angle, pivot) { drawImage(...) }` |
| `canvas.drawLine(x1,y1,x2,y2,paint)` | `drawLine(color, start, end, strokeWidth)` |
| `canvas.drawRect(...)` | `drawRect(color, topLeft, size)` |

Port to `commonMain`:
- `WallRenderer` → `DrawScope.drawWalls(wallModel, cellSize, offset)`
- `getTargetDrawable()` → `when(color)` returning the right `ImageBitmap`
- `drawRobotWithGraphics()` → `DrawScope.drawRobot(robot, sprite, scale)`
- `drawHintArrow()` → `DrawScope.drawHintArrow(...)`
- Robot paths, grid tile rotations, center logo
- Touch/drag gestures → `Modifier.pointerInput { detectDragGestures }`
- Animations (`ValueAnimator`) → `animateFloatAsState` / `Animatable`
- Accessibility/TalkBack → Compose semantics (`Modifier.semantics`, move buttons)

### 4. Rewrite all menu/settings screens with real assets

Replace the placeholder Material3 screens with faithful versions of the Fragment layouts: real logo, backgrounds, button styling/order/labels, using the shared `strings.xml` values.

### 5. Replace the Android entry point and delete the legacy UI

In one step:
- `MainActivity` → `setContent { App() }`
- Delete `app/src/main/java/roboyard/ui/fragments/*`, `GameGridView`, `WallRenderer`, and the XML layouts
- Remove now-unused drawable references from `app`

## Verification

Develop on Linux Desktop, then verify on Android (and iOS on macOS):

```bash
./gradlew :composeApp:run                 # Desktop
./gradlew :composeApp:assembleDebug       # Android (after androidTarget enabled)
```

Verify gameplay, save/load, achievements, settings, levels, accessibility (TalkBack), animations.

## Summary

- One-step Big-Bang: Fragments removed, replaced by the shared Compose `App()`.
- Same Compose UI for Android + iOS + Desktop.
- Main work = translate `GameGridView.onDraw()` + Fragment layouts to Compose `Canvas`/composables with full feature parity, reusing the same PNG assets and the already-shared game logic.
- The working Fragment version remains in git history as the safety net.

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
- Existing Android UI (Fragments) remains untouched
- Compose UI is built in parallel for future migration

# Concept: 1:1 Migration of Android Fragments to Compose Multiplatform

## Important: Current State of Compose Screens

**The current Compose screens are placeholders (dummies), NOT a faithful reproduction of the real Roboyard UI.**

The iOS app will look **exactly** like whatever the shared Compose UI in `composeApp/commonMain` renders. Right now that is generic Material3 styling. To make iOS (and Desktop) look like the real Roboyard Android app, the Compose screens must be rewritten to use the **same graphics, sprites, and buttons** as the Android Fragments.

The good news: **Compose runs identically on Desktop, iOS, and Android.** So once a screen looks correct on Linux Desktop, it will look the same on iOS.

## How the Android UI Actually Works

The Android UI is **not** built from simple Views – the game board is a single custom `View` (`GameGridView`) that draws everything onto an Android `Canvas` using `Drawable` PNG/vector assets:

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

The menu/settings/help screens (`MainMenuFragment`, `SettingsFragment`, etc.) are standard XML layouts in `app/src/main/res/layout/`.

## Migration Strategy

Compose's `Canvas` API (`drawImage`, `drawLine`, `drawRect`, `rotate`) maps almost 1:1 to Android's `Canvas`. The migration is therefore mostly a **translation**, not a redesign.

### Step 1 – Share the graphic assets

Move/copy the drawables from `app/src/main/res/drawable/` into Compose Multiplatform resources:

```
composeApp/src/commonMain/composeResources/drawable/
├── robot_pink_left.png / robot_pink_right.png
├── robot_green_left.png / robot_green_right.png
├── robot_blue_left.png  / robot_blue_right.png
├── robot_yellow_left.png/ robot_yellow_right.png
├── robot_silver_left.png/ robot_silver_right.png
├── grid_tiles.png
├── roboyard.png        (center logo)
├── mh.png / mv.png     (walls)
└── target_{pink,green,blue,yellow,silver,multi}.png
```

Add the `compose.components.resources` dependency in `composeApp/build.gradle` and access them via the generated `Res.drawable.*` accessors:

```kotlin
val robotPink = painterResource(Res.drawable.robot_pink_right)
```

### Step 2 – Rewrite the board renderer (highest priority)

`GameScreen.kt` already uses Compose `Canvas`. Replace the placeholder drawing with the real rendering logic translated from `GameGridView.onDraw()`:

| Android (`Canvas`) | Compose (`DrawScope`) |
| --- | --- |
| `drawable.setBounds(...); drawable.draw(canvas)` | `drawImage(imageBitmap, dstOffset, dstSize)` |
| `canvas.rotate(angle, cx, cy)` | `rotate(angle, pivot) { drawImage(...) }` |
| `canvas.drawLine(x1,y1,x2,y2,paint)` | `drawLine(color, start, end, strokeWidth)` |
| `canvas.drawRect(...)` | `drawRect(color, topLeft, size)` |

Port these helper classes/methods to `commonMain`:
- `WallRenderer` → a Compose function `DrawScope.drawWalls(wallModel, cellSize, offset)`
- `getTargetDrawable()` → a `when(color)` returning the right `ImageBitmap`
- `drawRobotWithGraphics()` → `DrawScope.drawRobot(robot, sprite, scale)`
- `drawHintArrow()` → `DrawScope.drawHintArrow(...)`

The board math (`offsetX/offsetY` centering, `cellSize` calculation in `onMeasure`) translates directly using `size.width/height` in the Compose `Canvas`.

### Step 3 – Port the menu/settings/help screens

These are simpler. The XML layouts become Compose composables, but they must use the **real assets and colors** instead of Material3 defaults:
- Background images / logo from the shared resources
- The same button styling, ordering, and labels as the Fragments
- Use the existing `strings.xml` values (see Step 5)

### Step 4 – Reuse the shared logic (no rewrite needed)

The game logic, board model, solver, and `GameState` are **already** in `shared/commonMain` and work on all platforms. The Compose UI should:
- Read state from the same `GameState` / `Board` classes
- Call the same `SolverIDDFS` for hints
- Call the same move logic instead of the placeholder `moveRobot()` in `GameScreen.kt`

This is the key win: only the **rendering layer** is migrated, not the logic.

### Step 5 – Strings and localization

Move the relevant strings from `app/src/main/res/values*/strings.xml` into Compose Multiplatform string resources (`composeResources/values/strings.xml`) so the same translations work on iOS/Desktop. Access via `stringResource(Res.string.key)`.

### Step 6 – Animations

Replace `ValueAnimator` with Compose `animateFloatAsState` / `Animatable`:
- Robot selection scaling → `animateFloatAsState(if (selected) 1.3f else 1.1f)`
- Robot move animation → `Animatable` interpolating grid position over ~300ms

## Recommended Migration Order

1. **Board rendering** (`GameScreen.kt`) – the visual core, biggest impact
2. **Main Menu** – real logo, backgrounds, button layout
3. **Level Selection** – real level thumbnails/graphics
4. **Settings / Help / Credits / Save-Load / Achievements** – simpler layouts
5. **Animations and polish** – robot scaling, move tweening, hint arrows

## Testing Each Screen on Linux

After migrating each screen, verify on Desktop before assuming it works on iOS:

```bash
./gradlew :composeApp:run
```

Because Compose renders identically across platforms, a screen that looks correct on Linux Desktop will look the same on iOS once built on macOS.

## Summary

- The iOS app shows **exactly** the shared Compose UI – currently dummies.
- Migration = translate `GameGridView.onDraw()` + Fragment layouts to Compose `Canvas`/composables, reusing the same PNG assets and the already-shared game logic.
- Compose `Canvas` maps almost 1:1 to Android `Canvas`, so this is a translation, not a redesign.
- Develop and verify everything on Linux Desktop; iOS gets the same look "for free" once built on macOS.
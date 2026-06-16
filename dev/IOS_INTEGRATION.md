# iOS Integration Guide for Roboyard

This guide explains how to integrate the shared Kotlin Multiplatform (KMP) module into an iOS Xcode project.

## Prerequisites

- macOS with Xcode 15.0 or later
- Xcode Command Line Tools installed
- CocoaPods or Swift Package Manager (SPM)

## Current Status

The shared module is configured for iOS with the following targets:
- **iosX64** - iOS Simulator (Intel)
- **iosArm64** - iOS Device (ARM64)
- **iosSimulatorArm64** - iOS Simulator (Apple Silicon)

### Completed Implementations

**Platform-agnostic (shared/commonMain):**
- Core logic package (GameLogic, MapObjects, LevelFormatParser, etc.)
- DriftingDroids solver package (Board, Solver, Solution, etc.)
- Storage and network interfaces

**iOS-specific (shared/iosMain):**
- **IosStorage.kt** - Implemented with NSUserDefaults and NSFileManager
- **IosNetworkMonitor.kt** - Implemented with NWPathMonitor

## Integration Steps

### Option 1: Using CocoaPods

1. **Create a new iOS project** in Xcode (SwiftUI or UIKit)

2. **Create a Podfile** in the iOS project directory:
```ruby
platform :ios, '15.0'

target 'YourApp' do
  use_frameworks!
  
  # Add the shared module as a local pod
  pod 'RoboyardShared', :path => '../../shared'
end
```

3. **Install CocoaPods** (if not already installed):
```bash
sudo gem install cocoapods
```

4. **Install the pod**:
```bash
cd ios/YourApp
pod install
```

5. **Open the .xcworkspace** file (not .xcodeproj):
```bash
open YourApp.xcworkspace
```

### Option 2: Using Swift Package Manager

1. **Create a new iOS project** in Xcode

2. **Add the shared module as a local package**:
   - File → Add Package Dependencies...
   - Select "Add Local..."
   - Navigate to the `shared` directory
   - Add the package

3. **Select the shared module** in your target's Frameworks & Libraries

## Using the Shared Module in Swift

### Import the Module

```swift
import RoboyardShared
```

### Initialize Storage

```swift
let storage = IosStorage()
let preferences = Preferences(storageProvider: { storage })
```

### Use Game Logic

```swift
let gameLogic = GameLogic(preferences: preferences)
let board = gameLogic.createBoard(width: 12, height: 14, numRobots: 4)
```

### Use DriftingDroids Solver

```swift
let solver = SolverIDDFS(board: board)
let solution = solver.solve(maxDepth: 100)
let moves = solution.movesList
```

### Monitor Network Status

```swift
let networkMonitor = IosNetworkMonitor()
if networkMonitor.isNetworkAvailable() {
    // Network is available
}
```

## Building for iOS

### Build from Xcode

1. Select your target device (iOS Simulator or physical device)
2. Product → Build (⌘B)

### Build from Command Line

```bash
cd ios/YourApp
xcodebuild -workspace YourApp.xcworkspace -scheme YourApp -configuration Debug
```

## Testing

### Unit Tests

Create unit tests in your iOS project to test the shared module:

```swift
import XCTest
import RoboyardShared

class SharedModuleTests: XCTestCase {
    func testStorage() {
        let storage = IosStorage()
        storage.putString(key: "test", value: "hello")
        XCTAssertEqual(storage.getString(key: "test"), "hello")
    }
}
```

### Running Tests

```bash
xcodebuild test -workspace YourApp.xcworkspace -scheme YourApp -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Troubleshooting

### Build Errors

**Error: "Cannot find module 'RoboyardShared'"**
- Ensure the shared module is properly linked in your target
- Check that the pod is installed (CocoaPods) or package is added (SPM)

**Error: "Undefined symbols for architecture arm64"**
- Ensure all iOS targets are built (iosX64, iosArm64, iosSimulatorArm64)
- Check that the shared module is built for the correct architecture

### Runtime Errors

**Error: "Class not found"**
- Ensure the shared module is properly initialized
- Check that all dependencies are linked

## Next Steps

1. Create the iOS UI layer (SwiftUI/UIKit)
2. Implement game board rendering
3. Add touch handling for robot movement
4. Implement level selection
5. Add settings screen
6. Test on iOS Simulator
7. Test on physical iOS device

## Additional Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [CocoaPods Documentation](https://cocoapods.org/)
- [Swift Package Manager Documentation](https://swift.org/package-manager/)

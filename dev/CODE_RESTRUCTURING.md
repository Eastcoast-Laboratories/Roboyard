# Code Restructuring: KMP Preparation

## Overview
The whole `roboyard.logic.*` package is now Kotlin. Before the logic can be shared
with iOS via Kotlin Multiplatform, the remaining Android infrastructure dependencies
must be abstracted.

## TODO: Abstract Android dependencies for KMP
Files depending on `SharedPreferences`, `Context`, `Activity`, `ViewModel`, `LiveData`,
`Bitmap` or `HttpURLConnection` cannot be shared with iOS as-is. Abstract these via
`expect`/`actual` declarations or platform interfaces.

| File | Android deps to abstract |
|------|--------------------------|
| `roboyard.logic.managers/GameStateManager.kt` | `AndroidViewModel`, `Activity`, `LiveData`, `GameGridView`, `RobotAnimationManager` |
| `roboyard.logic.managers/SyncManager.kt` | `HttpURLConnection` |
| `roboyard.logic.network/RoboyardApiClient.kt` | `HttpURLConnection` |
| `roboyard.logic.storage/FileReadWrite.kt` | `Context`, `SharedPreferences` |
| `roboyard.logic.graphics/MinimapGenerator.kt` | `Bitmap`, `Canvas` |
| `roboyard.logic.achievements/AchievementManager.kt` | `Context`, `SharedPreferences` |

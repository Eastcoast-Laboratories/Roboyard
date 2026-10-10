---
name: espresso-testing
description: Rules and helpers for writing Espresso UI tests in the Roboyard project
---

# Espresso Testing in Roboyard

Before writing any new Espresso test, read and analyze `app/src/androidTest/java/roboyard/eclabs/ui/TestHelper.java` to see which reusable methods already exist. Never reinvent existing functionality.

## Rules

- Always use `TestHelper` static methods in new tests instead of writing inline interactions.
- For all settings-related UI interactions, create reusable static methods in `TestHelper.java` rather than duplicating code across tests (e.g. `setDifficulty()`, `setMultiTargetMode()`, `setRobotCount()`).
- Document every new test in `dev/TESTSUITE.md` (name, type, status, description, tags).

## Available TestHelper methods

- `startNewSessionWithEmptyStorage(activity)` — clear all history and start fresh
- `startAndWait8sForPopupClose()` — wait for achievement/streak popup to close
- `startRandomGame()` — click "New Random Game" button
- `startLevelGame(activityRule, levelId)` — start a specific level
- `openDebugScreen()` — navigate to Debug Settings via long press
- `openLevelEditorThroughDebug()` — open Level Editor via Debug Settings
- `openSettingsAndScrollDown()` — open Settings and scroll to bottom
- `navigateToSaveLoadScreen(activityRule)` — navigate to Save/Load screen
- `navigateToHistoryTab()` — switch to History tab in Save/Load
- `closeAchievementPopupIfPresent()` — close achievement popup programmatically

If a needed interaction is missing, add it to `TestHelper` as a reusable static method.

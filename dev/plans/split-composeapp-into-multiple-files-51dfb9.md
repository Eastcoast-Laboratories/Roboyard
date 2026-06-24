# Split ComposeApp App.kt into Multiple Files

Split the large App.kt file (2245 lines) into multiple files following the Main App structure using three initial commits: copy Fragment files from Main App, copy App.kt to all new files, delete unnecessary lines, then fix package declarations and imports.

## Fragment to Screen Mapping

| Main App Fragment | ComposeApp Screen | File Name | Status |
|-------------------|-------------------|-----------|--------|
| MainMenuFragment.java | MainMenuScreen() | MainMenuScreen.kt | To create |
| GameFragment.java | GameScreen() | GameScreen.kt | ✅ Already exists |
| LevelSelectionFragment.java | LevelSelectionScreen() | LevelSelectionScreen.kt | To create |
| SaveGameFragment.java | SaveLoadScreen() | SaveLoadScreen.kt | To create |
| SettingsFragment.java | SettingsScreen() | SettingsScreen.kt | ✅ Already exists |
| CreditsFragment.java | CreditsScreen() | CreditsScreen.kt | To create |
| HelpFragment.java | HelpScreen() | HelpScreen.kt | To create |
| AchievementsFragment.java | AchievementsScreen() | AchievementsScreen.kt | To create |
| DebugSettingsFragment.java | (not in ComposeApp yet) | - | N/A |
| LevelDesignEditorFragment.java | (not in ComposeApp yet) | - | N/A |

## Utility and Component Files

| Function/Component | File Name | Purpose |
|-------------------|-----------|---------|
| gridElementsToBoard, signatures, serialization | BoardUtils.kt | Board utility functions |
| GameInfoCard, CircularButton, CustomButton, MinimapGenerator | UIComponents.kt | Reusable UI components |
| getHistoryEntries, getNextHistoryIndex, validateSaveContainsTargets | SaveLoadScreen.kt | History utilities |
| LoadingScreen() | LoadingScreen.kt | Loading screen |
| Screen enum, App() navigation | App.kt | Main entry point |

## Implementation: Delete unnecessary lines from each file without code changes

### Step 1: Delete unnecessary lines from App.kt
Keep only: Screen enum and App() function (navigation logic)
```bash
git commit -am "refactor: App.kt - keep only Screen enum and App() navigation function"
```

### Step 2: Delete unnecessary lines from MainMenuScreen.kt
Keep only: MainMenuScreen() @Composable function and related imports/variables
```bash
git commit -am "refactor: MainMenuScreen.kt - keep only MainMenuScreen() @Composable function and related imports/variables"
```

### Step 3: Delete unnecessary lines from LevelSelectionScreen.kt
Keep only: LevelSelectionScreen() and LevelItem() @Composable functions and related code
```bash
git commit -am "refactor: LevelSelectionScreen.kt - keep only LevelSelectionScreen() and LevelItem() @Composable functions and related code"
```

### Step 4: Delete unnecessary lines from SaveLoadScreen.kt
Keep only: SaveLoadScreen(), HistoryItem(), HistoryInfoDialog() @Composable functions, plus getHistoryEntries(), getNextHistoryIndex(), validateSaveContainsTargets()
```bash
git commit -am "refactor: SaveLoadScreen.kt - keep only SaveLoadScreen(), HistoryItem(), HistoryInfoDialog() @Composable functions and history utilities (getHistoryEntries, getNextHistoryIndex, validateSaveContainsTargets)"
```

### Step 5: Delete unnecessary lines from SettingsScreen.kt
Keep only: SettingsScreen() @Composable function and related code
```bash
git commit -am "refactor: SettingsScreen.kt - keep only SettingsScreen() @Composable function and related code"
```

### Step 6: Delete unnecessary lines from CreditsScreen.kt
Keep only: CreditsScreen() @Composable function and related code
```bash
git commit -am "refactor: CreditsScreen.kt - keep only CreditsScreen() @Composable function and related code"
```

### Step 7: Delete unnecessary lines from HelpScreen.kt
Keep only: HelpScreen() @Composable function and related code
```bash
git commit -am "refactor: HelpScreen.kt - keep only HelpScreen() @Composable function and related code"
```

### Step 8: Delete unnecessary lines from AchievementsScreen.kt
Keep only: AchievementsScreen() @Composable function and related code
```bash
git commit -am "refactor: AchievementsScreen.kt - keep only AchievementsScreen() @Composable function and related code"
```

### Step 9: Delete unnecessary lines from LoadingScreen.kt
Keep only: LoadingScreen() @Composable function and related code
```bash
git commit -am "refactor: LoadingScreen.kt - keep only LoadingScreen() @Composable function and related code"
```

### Step 10: Delete unnecessary lines from BoardUtils.kt
Keep only: Utility functions (gridElementsToBoard, generateWallSignature, generatePositionSignature, generateMapSignature, generateUnique5LetterFromString, generateMapNameFromSignature, serializeBoardToMainGameFormat, deserializeBoardFromMainGameFormat)
```bash
git commit -am "refactor: BoardUtils.kt - keep only utility functions (gridElementsToBoard, signatures, serialization)"
```

### Step 11: Delete unnecessary lines from UIComponents.kt
Keep only: UI components (GameInfoCard, CircularButton, CustomButton, MinimapGenerator)
```bash
git commit -am "refactor: UIComponents.kt - keep only UI components (GameInfoCard, CircularButton, CustomButton, MinimapGenerator)"
```

### Step 12: Make remaining changes after line deletion (package declarations, imports, etc.)
After deleting unnecessary lines, make the following changes to ensure all files compile correctly:
1. **Fix package declarations**: Ensure all new files have `package roboyard.ui.compose` at the top
2. **Add necessary imports**: Add missing imports in each file (Compose, Material3, etc.)
3. **Remove unused imports**: Clean up any imports that are no longer needed
4. **Ensure function accessibility**: Make sure @Composable functions are accessible from App.kt (remove private if needed)
5. **Update App.kt imports**: Add imports for all new screen files so App() can reference them
6. **Update GameScreen.kt**: Add imports from BoardUtils.kt and UIComponents.kt if functions/components are used there

### Step 13: Compile and verify
Run compilation to ensure all files compile without errors:
```bash
cd /var/www/Roboyard && ./gradlew :composeApp:compileKotlinDesktop
```
Fix any compilation errors by adjusting imports or function visibility.

### Step 14: Commit the final state
```bash
git commit -am "refactor: extract code from App.kt to separate screen files for better maintainability"
```

## Notes
- GameScreen.kt and SettingsScreen.kt already exist separately, only need to verify imports
- Ensure all package declarations match: `package roboyard.ui.compose`
- Update imports in all files after line deletion
- The three initial commits show the progression from Main App structure to extracted content
- This approach preserves code history in git for better traceability

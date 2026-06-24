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
| BaseGameFragment.java | (base class with utilities) | BaseGameUtils.kt.todo | TODO |
| DebugSettingsFragment.java | (not in ComposeApp yet) | DebugSettingsScreen.kt.todo | TODO (done) |
| LevelDesignEditorFragment.java | (not in ComposeApp yet) | LevelDesignEditorScreen.kt.todo | TODO (done) |

## Utility and Component Files

| Function/Component | File Name | Purpose |
|-------------------|-----------|---------|
| gridElementsToBoard, signatures, serialization | BoardUtils.kt | Board utility functions |
| GameInfoCard, CircularButton, CustomButton, MinimapGenerator | UIComponents.kt | Reusable UI components |
| getHistoryEntries, getNextHistoryIndex, validateSaveContainsTargets | SaveLoadScreen.kt | History utilities |
| LoadingScreen() | LoadingScreen.kt | Loading screen |
| Screen enum, App() navigation | App.kt | Main entry point |

## Exact Line Ranges in App.kt (2245 lines total)

| Function/Component | Start Line | End Line | Lines to Keep |
|-------------------|------------|----------|--------------|
| **App.kt (final)** | | | |
| - App() function | 80 | 234 | 80-234 |
| - Screen enum | 236 | 246 | 236-246 |
| **MainMenuScreen.kt** | | | |
| - MainMenuScreen() | 248 | 420 | 248-420 |
| **LevelSelectionScreen.kt** | | | |
| - LevelSelectionScreen() | 437 | 563 | 437-563 |
| - LevelItem() | 565 | 637 | 565-637 |
| **HelpScreen.kt** | | | |
| - HelpScreen() | 639 | 733 | 639-733 |
| **CreditsScreen.kt** | | | |
| - CreditsScreen() | 735 | 826 | 735-826 |
| **BoardUtils.kt** | | | |
| - generateWallSignature() | 828 | 862 | 828-862 |
| - generatePositionSignature() | 864 | 918 | 864-918 |
| - generateMapSignature() | 919 | 922 | 919-922 |
| - generateUnique5LetterFromString() | 931 | 954 | 931-954 |
| - generateMapNameFromSignature() | 961 | 968 | 961-968 |
| - serializeBoardToMainGameFormat() | 974 | 1311 | 974-1311 |
| - deserializeBoardFromMainGameFormat() | 1312-1398 (private) | 1312 | 1398 | 1312-1398 |
| - saveToHistory() | 1399-1400 (private) | 1399 | 1400 | 1399-1400 |
| - getNextHistoryIndex() | 1401-1408 (private) | 1401 | 1408 | 1401-1408 |
| - getHistoryEntries() | 1409-1450 (private) | 1409 | 1450 | 1409-1450 |
| - validateSaveContainsTargets() | 1451-1460 (private) | 1451 | 1460 | 1451-1460 |
| **SaveLoadScreen.kt** | | | |
| - HistoryItem() | 1462 | 1547 | 1462-1547 |
| - SaveLoadScreen() | 1549 | 1751 | 1549-1751 |
| - SaveSlotItem() | 1753 | 1775 | 1753-1775 |
| **DebugSettingsScreen.kt** | | | |
| - DebugSettingsScreen() | 1777 | 1882 | 1777-1882 |
| **LevelDesignEditorScreen.kt** | | | |
| - LevelDesignEditorScreen() | 1884 | 2102 | 1884-2102 |
| **AchievementsScreen.kt** | | | |
| - AchievementsScreen() | 2104 | 2173 | 2104-2173 |
| **LoadingScreen.kt** | | | |
| - LoadingScreen() | 2175 | 2234 | 2175-2234 |
| - AchievementItem() | 2236 | 2244 | 2236-2244 |
| - HistoryInfoDialog() | 2246-2245 (at end) | 2246 | 2245 | 2246-2245 |

## Implementation: Delete unnecessary lines from each file using bash

### Pre-step: Create TODO files for missing screens (DONE ✅)
Copy missing Fragment files as .kt.todo files for future implementation:
- DebugSettingsFragment.java → DebugSettingsScreen.kt.todo ✅
- LevelDesignEditorFragment.java → LevelDesignEditorScreen.kt.todo ✅
- BaseGameFragment.java → BaseGameUtils.kt.todo (to create - contains utilities like minimap generation, language settings, etc.)

### Step 1: Create BaseGameUtils.kt.todo (DONE ✅)
```bash
cd /var/www/Roboyard/composeApp/src/commonMain/kotlin/roboyard/ui/compose && \
cp /var/www/Roboyard/app/src/main/java/roboyard/ui/fragments/BaseGameFragment.java BaseGameUtils.kt.todo
git add .; git commit -am "refactor: create TODO file for BaseGameFragment utilities (minimap, language settings, etc.)"
```

### Step 2: Delete unnecessary lines from App.kt (keep lines 1-14, 80-246)
```bash
cd /var/www/Roboyard/composeApp/src/commonMain/kotlin/roboyard/ui/compose
# Keep package (1), imports (3-14), App() (80-234), Screen enum (236-246)
# Delete lines in reverse order to avoid line number shifts
sed -i '247,2245d; 235d; 15,79d' App.kt
git add App.kt
git commit -am "refactor: App.kt - keep only Screen enum and App() navigation function"
```

### Step 3: Delete unnecessary lines from MainMenuScreen.kt (keep lines 1-14, 248-420)
```bash
# Copy App.kt to MainMenuScreen.kt first
cp App.kt MainMenuScreen.kt
# Keep package (1), imports (3-14), MainMenuScreen() (248-420)
# Delete lines in reverse order to avoid line number shifts
sed -i '421,2245d; 15,247d' MainMenuScreen.kt
git add MainMenuScreen.kt
git commit -am "refactor: MainMenuScreen.kt - keep only MainMenuScreen() @Composable function and related imports/variables"
```

### Step 4: Delete unnecessary lines from LevelSelectionScreen.kt (keep lines 1-14, 437-637)
```bash
# Copy App.kt to LevelSelectionScreen.kt first
cp App.kt LevelSelectionScreen.kt
# Keep package (1), imports (3-14), LevelSelectionScreen() (437-563), LevelItem() (565-637)
# Delete lines in reverse order to avoid line number shifts
sed -i '638,2245d; 564d; 15,436d' LevelSelectionScreen.kt
git add LevelSelectionScreen.kt
git commit -am "refactor: LevelSelectionScreen.kt - keep only LevelSelectionScreen() and LevelItem() @Composable functions and related code"
```

### Step 5: Delete unnecessary lines from HelpScreen.kt (keep lines 1-14, 639-733)
```bash
# Copy App.kt to HelpScreen.kt first
cp App.kt HelpScreen.kt
# Keep package (1), imports (3-14), HelpScreen() (639-733)
# Delete lines in reverse order to avoid line number shifts
sed -i '734,2245d; 15,638d' HelpScreen.kt
git add HelpScreen.kt
git commit -am "refactor: HelpScreen.kt - keep only HelpScreen() @Composable function and related code"
```

### Step 6: Delete unnecessary lines from CreditsScreen.kt (keep lines 1-14, 735-826)
```bash
# Copy App.kt to CreditsScreen.kt first
cp App.kt CreditsScreen.kt
# Keep package (1), imports (3-14), CreditsScreen() (735-826)
# Delete lines in reverse order to avoid line number shifts
sed -i '827,2245d; 15,734d' CreditsScreen.kt
git add CreditsScreen.kt
git commit -am "refactor: CreditsScreen.kt - keep only CreditsScreen() @Composable function and related code"
```

### Step 7: Delete unnecessary lines from BoardUtils.kt (keep lines 1-14, 828-1460)
```bash
# Copy App.kt to BoardUtils.kt first
cp App.kt BoardUtils.kt
# Keep package (1), imports (3-14), utility functions (828-1460)
# Delete lines in reverse order to avoid line number shifts
sed -i '1461,2245d; 15,827d' BoardUtils.kt
git add BoardUtils.kt
git commit -am "refactor: BoardUtils.kt - keep only utility functions (gridElementsToBoard, signatures, serialization)"
```

### Step 8: Delete unnecessary lines from SaveLoadScreen.kt (keep lines 1-14, 1462-1775)
```bash
# Copy App.kt to SaveLoadScreen.kt first
cp App.kt SaveLoadScreen.kt
# Keep package (1), imports (3-14), HistoryItem() (1462-1547), SaveLoadScreen() (1549-1751), SaveSlotItem() (1753-1775)
# Delete lines in reverse order to avoid line number shifts
sed -i '1776,2245d; 1752d; 1548d; 15,1461d' SaveLoadScreen.kt
git add SaveLoadScreen.kt
git commit -am "refactor: SaveLoadScreen.kt - keep only SaveLoadScreen(), HistoryItem(), SaveSlotItem() @Composable functions and history utilities"
```

### Step 9: Delete unnecessary lines from DebugSettingsScreen.kt (keep lines 1-14, 1777-1882)
```bash
# Copy App.kt to DebugSettingsScreen.kt first
cp App.kt DebugSettingsScreen.kt
# Keep package (1), imports (3-14), DebugSettingsScreen() (1777-1882)
# Delete lines in reverse order to avoid line number shifts
sed -i '1883,2245d; 15,1776d' DebugSettingsScreen.kt
git add DebugSettingsScreen.kt
git commit -am "refactor: DebugSettingsScreen.kt - keep only DebugSettingsScreen() @Composable function and related code"
```

### Step 10: Delete unnecessary lines from LevelDesignEditorScreen.kt (keep lines 1-14, 1884-2102)
```bash
# Copy App.kt to LevelDesignEditorScreen.kt first
cp App.kt LevelDesignEditorScreen.kt
# Keep package (1), imports (3-14), LevelDesignEditorScreen() (1884-2102)
# Delete lines in reverse order to avoid line number shifts
sed -i '2103,2245d; 15,1883d' LevelDesignEditorScreen.kt
git add LevelDesignEditorScreen.kt
git commit -am "refactor: LevelDesignEditorScreen.kt - keep only LevelDesignEditorScreen() @Composable function and related code"
```

### Step 11: Delete unnecessary lines from AchievementsScreen.kt (keep lines 1-14, 2104-2173)
```bash
# Copy App.kt to AchievementsScreen.kt first
cp App.kt AchievementsScreen.kt
# Keep package (1), imports (3-14), AchievementsScreen() (2104-2173)
# Delete lines in reverse order to avoid line number shifts
sed -i '2174,2245d; 15,2103d' AchievementsScreen.kt
git add AchievementsScreen.kt
git commit -am "refactor: AchievementsScreen.kt - keep only AchievementsScreen() @Composable function and related code"
```

### Step 12: Delete unnecessary lines from LoadingScreen.kt (keep lines 1-14, 2175-2245)
```bash
# Copy App.kt to LoadingScreen.kt first
cp App.kt LoadingScreen.kt
# Keep package (1), imports (3-14), LoadingScreen() (2175-2234), AchievementItem() (2236-2244), HistoryInfoDialog() (2246-2245)
# Delete lines in reverse order to avoid line number shifts
sed -i '2245d; 2235d; 15,2174d' LoadingScreen.kt
git add LoadingScreen.kt
git commit -am "refactor: LoadingScreen.kt - keep only LoadingScreen(), AchievementItem(), HistoryInfoDialog() @Composable functions and related code"
```

### Step 13: Make remaining changes after line deletion (package declarations, imports, etc.)
After deleting unnecessary lines, make the following changes to ensure all files compile correctly:
1. **Fix package declarations**: Ensure all new files have `package roboyard.ui.compose` at the top
2. **Add necessary imports**: Add missing imports in each file (Compose, Material3, etc.)
3. **Remove unused imports**: Clean up any imports that are no longer needed
4. **Ensure function accessibility**: Make sure @Composable functions are accessible from App.kt (remove private if needed)
5. **Update App.kt imports**: Add imports for all new screen files so App() can reference them
6. **Update GameScreen.kt**: Add imports from BoardUtils.kt and UIComponents.kt if functions/components are used there

### Step 14: Compile and verify
Run compilation to ensure all files compile without errors:
```bash
cd /var/www/Roboyard && ./gradlew :composeApp:compileKotlinDesktop
```
Fix any compilation errors by adjusting imports or function visibility.

### Step 15: Commit the final state
```bash
git commit -am "refactor: extract code from App.kt to separate screen files for better maintainability"
```

## Notes
- GameScreen.kt and SettingsScreen.kt already exist separately, only need to verify imports
- Ensure all package declarations match: `package roboyard.ui.compose`
- Update imports in all files after line deletion
- The three initial commits show the progression from Main App structure to extracted content
- This approach preserves code history in git for better traceability

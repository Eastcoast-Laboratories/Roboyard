# Test Repair Documentation

## Overview
This document tracks test repairs and the root causes of failures, helping identify systemic issues and prevent future regressions.

---

## ReverseMoveUndoBugTest - Level Selection Navigation Issue

**Date:** 2025-03-10  
**Status:** ✅ Repaired & Verified  
**Test Duration:** 5.6s  
**Root Cause:** Level Selection UI redesign changed button layout and IDs

### Problem
Test was failing because it tried to navigate to Level 1 using:
```java
onView(allOf(withId(R.id.level_button), withText("1")))
```

After the Level Selection redesign, the level buttons are now part of a RecyclerView with different IDs and structure.

### Solution
Use `TestHelper.startLevelGame(activityRule, levelId)` instead of manual UI navigation:

```java
@Before
public void setUp() throws InterruptedException {
    TestHelper.closeAchievementPopupIfPresent();
    TestHelper.startLevelGame(activityRule, 1);  // Start Level 1 programmatically
    
    activityRule.getScenario().onActivity(activity -> {
        gameStateManager = activity.getGameStateManager();
    });
    assertNotNull("GameStateManager must not be null", gameStateManager);
}
```

### Why This Works
- `TestHelper.startLevelGame()` uses `GameStateManager.startLevelGame()` directly, bypassing UI navigation
- No dependency on Level Selection button IDs or layout structure
- More robust and faster than UI-based navigation
- Follows DRY principle: reuse existing helper methods instead of duplicating navigation logic

### Lesson Learned
**When UI layouts change (especially RecyclerView-based screens):**
1. ❌ Don't hardcode button IDs or text matchers
2. ✅ Use programmatic navigation via GameStateManager or TestHelper
3. ✅ Update TESTSUITE.md to document which tests use [e] (visible in emulator) vs programmatic navigation
4. ✅ Add a rule to TESTSUITE.md: "Prefer TestHelper methods over UI navigation for robustness"

### Verification
Test executed successfully with the following behavior:
```
Move LEFT:  count=1, robot at (2,8)
Move UP:    count=2, robot at (2,6)
Move DOWN:  count=1, robot at (2,8) ← Undo triggered!
Move RIGHT: count=2, robot at (4,8) ← Robot still movable ✓
```

All assertions passed:
- ✅ Move count decreased after reverse-move undo
- ✅ Robot remained movable after undo
- ✅ Move count increased after subsequent move

### Affected Tests
All Level-based tests now use `TestHelper.startLevelGame()`:
- ✅ `ReverseMoveUndoBugTest` - Fixed and verified
- ✅ `Level1FastE2ETest` - Fixed and verified
- ✅ `Level1SlowE2ETest` - Fixed and verified
- ✅ `Level1WrongE2ETest` - Fixed and verified
- ✅ `Level3E2ETest` - Fixed and verified
- ✅ `Level10E2ETest` - Fixed and verified
- ✅ `Level111DebugTest` - Fixed and verified
- ✅ `LiveMoveCounterE2ETest` - Fixed and verified (4/4 tests)
- ✅ `GameModeMemoryE2ETest` - Fixed and verified (+ app bug fix)

### Prevention
Rule added to TESTSUITE.md:
> **Level Navigation Rule:** Always use `TestHelper.startLevelGame(activityRule, levelId)` for programmatic level start. Only use UI-based level selection if testing the Level Selection screen itself (e.g., `LevelSelectionLandscapeTest`).

---

## DRY: Solution Execution Methods Extracted to TestHelper

**Date:** 2025-03-10  
**Status:** ✅ Completed & All Tests Verified  

### Problem
8 test files contained identical duplicated code for:
- `executeSolutionMoves()` — wait for solver, iterate moves, select robot, execute direction
- `getDirectionX()` / `getDirectionY()` — convert `ERRGameMove` to dx/dy
- `waitForSolution()` — poll GameStateManager for solution
- `selectRobotAndMove()` — find robot by color, set selected, move

### Solution
Extracted all methods into `TestHelper.java` as static helpers:
```java
TestHelper.executeSolutionMoves(activityRule, gameStateManager, levelId, logTag)
TestHelper.selectRobotAndMove(activityRule, gameStateManager, robotColor, dx, dy)
TestHelper.waitForSolution(activityRule, gameStateManager, maxRetries)
TestHelper.completeLevelWithSolver(activityRule, gameStateManager, levelId, logTag)
```

### Tests Refactored (DRY)
- ✅ `Level3E2ETest` — removed ~100 lines
- ✅ `Level10E2ETest` — removed ~100 lines
- ✅ `Level111DebugTest` — removed ~120 lines
- ✅ `Level11With2StarsE2ETest` — removed ~80 lines
- ✅ `Level140E2ETest` — removed ~110 lines
- ✅ `BoardSizeResetLevel2E2ETest` — removed ~90 lines
- ✅ `PerfectRandom5E2ETest` — removed ~70 lines
- ✅ `RandomGame11E2ETest` — removed ~90 lines

### Lesson Learned
**When multiple tests need solver execution:**
1. ✅ Use `TestHelper.executeSolutionMoves()` or `TestHelper.completeLevelWithSolver()`
2. ❌ Don't copy-paste solution execution logic into each test
3. ✅ Keep test-specific logic (wrong moves, star checking) in the test itself

---

## GameModeMemoryE2ETest - AchievementsFragment Back Navigation Bug

**Date:** 2025-03-10  
**Status:** ✅ Repaired & Verified  
**Root Cause:** `AchievementsFragment.navigateToMainMenu()` always navigated to MainMenu, ignoring back stack

### Problem
After completing Level 1 → Achievement popup → "View achievements" → back button, the user landed on MainMenu instead of returning to the Game screen. The `next_level_button` was therefore not visible.

### Solution
Replaced `navigateToMainMenu()` with `navigateBack()` in `AchievementsFragment.java`:
```java
private void navigateBack() {
    boolean popped = requireActivity().getSupportFragmentManager().popBackStackImmediate();
    if (!popped) {
        Timber.d("[ACHIEVEMENTS] Back stack empty, navigating to MainMenu as fallback");
        MainMenuFragment fragment = new MainMenuFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }
}
```

### Why This Works
- `AchievementPopup.navigateToAchievements()` adds the fragment with `addToBackStack(null)`
- `popBackStackImmediate()` returns to the previous fragment (Game or MainMenu)
- Fallback to MainMenu only if back stack is empty (e.g., direct navigation)

### Lesson Learned
**When implementing back navigation in fragments:**
1. ✅ Use `popBackStackImmediate()` to respect back stack
2. ❌ Don't hardcode navigation target (e.g., always MainMenu)
3. ✅ Add fallback for empty back stack

---

## Test Repair Checklist

When repairing a failing test:

- [ ] Identify root cause (UI change, logic bug, timing issue, etc.)
- [ ] Check if TestHelper has a method for this operation
- [ ] If yes: use TestHelper method (DRY principle)
- [ ] If no: add new method to TestHelper
- [ ] Update test with new approach
- [ ] Add logging with `[UNITTESTS][TEST_NAME]` tag
- [ ] Document repair in this file
- [ ] Update TESTSUITE.md with lessons learned
- [ ] Run test until green
- [ ] Check if other tests have the same issue


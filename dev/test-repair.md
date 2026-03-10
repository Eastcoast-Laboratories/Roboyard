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
All Level-based tests should use `TestHelper.startLevelGame()`:
- ✅ `ReverseMoveUndoBugTest` - **Fixed and verified**
- ⚠️ `Level1E2ETest`, `Level1FastE2ETest`, `Level1SlowE2ETest`, `Level1WrongE2ETest`
- ⚠️ `Level3E2ETest`, `Level10E2ETest`, `Level11With2StarsE2ETest`

**Action Required:** Check and update remaining Level tests to use TestHelper.startLevelGame().

### Prevention
Rule added to TESTSUITE.md:
> **Level Navigation Rule:** Always use `TestHelper.startLevelGame(activityRule, levelId)` for programmatic level start. Only use UI-based level selection if testing the Level Selection screen itself (e.g., `LevelSelectionLandscapeTest`).

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


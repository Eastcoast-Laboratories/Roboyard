# Concept: Multiple Targets Support for DriftingDroid Solver (Option 3)

## Problem
DriftingDroid Solver currently only supports **one active goal** (`Board.goal`). For games with multiple targets (e.g., green and yellow robots reaching their 2 targets), the solver cannot find a solution.

**Constraint:** Only different colored robots are allowed

---

## Phase 0: Test-Driven Development Foundation

### 0a. Unit Tests (JUnit - DriftingDroid Core)

**Test File:** `app/src/test/java/roboyard/eclabs/MultipleTargetsSolverTest.java`

**Tests to implement FIRST (before any code changes):**

1. **Single goal (baseline test)** - Ensure we don't break existing functionality
   - Create board with 1 goal (green robot)
   - Verify solver finds solution
   - Compare with original single-goal behavior

2. **Two goals (different robots)** - Core feature test
   - Create board with 2 goals (green robot + yellow robot)
   - Verify solver finds solution reaching both goals
   - Verify solution moves both robots to their targets

3. **Three goals (edge case)** - Scalability test
   - Create board with 3 goals
   - Verify solver handles multiple goals correctly

4. **Same robot, multiple goals (constraint validation)** - Verify constraint enforcement
   - Create board where same robot has 2 goals
   - Verify solver rejects or handles gracefully

**Test Execution Strategy:**

Run tests BEFORE implementation:
```bash
cd /var/www/Roboyard
./gradlew testDebugUnitTest --tests "roboyard.eclabs.MultipleTargetsSolverTest"
```

**Expected Results (before implementation):**
- ✅ `testSingleGoal_GreenRobot` - PASS (baseline)
- ❌ `testTwoGoals_GreenAndYellow` - FAIL (expected, feature not implemented)
- ❌ `testThreeGoals_GreenYellowBlue` - FAIL (expected, feature not implemented)
- ❌ `testSameRobotMultipleGoals_ShouldFail` - FAIL (expected, feature not implemented)

**After implementation:**
- ✅ All tests should PASS
- ✅ No regressions in existing functionality

---

## Important Findings from DriftingDroid Code Analysis

### What `isSolution01()` really does
- **NOT:** Checks if goal is reached
- **YES:** Checks if goal is reachable in **ONE move** (speedup optimization)
- Called in `SolverIDDFS` constructor (line 70)
- Used for heuristic `minimumMovesToGoal` (lines 134-150)

### Where the actual goal check happens
- **`SolverIDDFS.iddfs()`** - Main search loop
- **`SolverIDDFS.search()`** - Recursive search
- Checks: `if (this.goalRobot == lastRobot && this.goalPosition == state[lastRobot])`
- This is the actual solution detection

### Critical code locations
1. **SolverIDDFS Constructor (lines 60-74)**
   - `this.goalPosition = this.board.getGoal().position` (line 67)
   - `this.goalRobot = ...` (line 69)
   - Only **one** goal is considered

2. **SolverIDDFS.iddfs() (line 120)**
   - Main search loop
   - Calls `search()`

3. **SolverIDDFS.search() - Goal check**
   - Checks `if (this.goalRobot == lastRobot && this.goalPosition == state[lastRobot])`
   - Returns Solution when goal is reached

4. **Board.isSolution01() (lines 870-908)**
   - Only speedup optimization
   - Checks if goal is reachable in 1 move
   - **NOT** responsible for actual goal check

---

## Phase 1: Extend DriftingDroid Source (Implementation)

### Architecture Corrections (from code analysis)

**IMPORTANT:** `Board.goal` CANNOT be removed. It is used at ~30+ locations:
- Serialization/deserialization (`toByteArray`, `fromByteArray`)
- Board rotation (`rotateBoard90`)
- Random goal selection (`setGoalRandom`)
- Board string representation (`toString`)
- Goal management (`setGoal`, `addGoal`, `removeGoal`)

**Strategy:** Keep `Board.goal` as-is. Add `activeGoals` as NEW additional field.
The solver uses `Board.goal` as first/primary goal, and checks `activeGoals` for multi-goal mode.

**4 DFS methods need changes** (not just 1):
1. `dfsRecursion()` - standard version (wildcard, solution01, noRebounds)
2. `dfsRecursionFast()` - fast version
3. `dfsLast()` - last depth, standard version
4. `dfsLastFast()` - last depth, fast version

### Changes

#### 1. **Board.java** - ADD activeGoals field (keep goal!)

```java
private Goal goal;                  // the current goal (KEEP - used everywhere)
private List<Goal> activeGoals;     // all active goals for multi-goal mode (NEW)
```

#### 2. **Board.java** - Add new methods

```java
public List<Goal> getActiveGoals() {
    if (this.activeGoals != null && !this.activeGoals.isEmpty()) {
        return this.activeGoals;
    }
    // Single goal mode: return list with just the current goal
    List<Goal> single = new ArrayList<>();
    if (this.goal != null) { single.add(this.goal); }
    return single;
}

public void setActiveGoals(List<Goal> goals) {
    this.activeGoals = new ArrayList<>(goals);
    // Also set primary goal for all existing code that uses Board.goal
    if (!goals.isEmpty()) {
        this.goal = goals.get(0);
    }
}
```

#### 3. **SolverIDDFS.java** - Add fields + extend constructor

New fields:
```java
private final List<Board.Goal> activeGoals;
private final int[] activeGoalPositions;   // positions of all active goals
private final int[] activeGoalRobots;      // robot numbers of all active goals
private final boolean isMultiGoalMode;
```

Constructor addition (after existing goalPosition/goalRobot init):
```java
// Multi-goal support
this.activeGoals = this.board.getActiveGoals();
this.isMultiGoalMode = (this.activeGoals.size() > 1);
this.activeGoalPositions = new int[this.activeGoals.size()];
this.activeGoalRobots = new int[this.activeGoals.size()];
for (int i = 0; i < this.activeGoals.size(); i++) {
    this.activeGoalPositions[i] = this.activeGoals.get(i).position;
    this.activeGoalRobots[i] = this.activeGoals.get(i).robotNumber;
}
```

#### 4. **SolverIDDFS.java** - Multi-goal check method

```java
private boolean isAllGoalsReached(final int[] state) {
    for (int i = 0; i < this.activeGoalPositions.length; i++) {
        int robotIdx = this.activeGoalRobots[i];
        // Account for swapGoalLast: if this robot was swapped to last position
        if (!this.isBoardGoalWildcard && robotIdx == this.board.getGoal().robotNumber) {
            robotIdx = state.length - 1;
        }
        if (state[robotIdx] != this.activeGoalPositions[i]) {
            return false;
        }
    }
    return true;
}
```

#### 5. **SolverIDDFS.java** - Modify all 4 DFS methods

In `dfsLast()` and `dfsLastFast()`: After a robot arrives at the primary goal position,
additionally check if ALL goals are reached via `isAllGoalsReached(state)`.

In `dfsRecursion()` and `dfsRecursionFast()`: No changes needed for goal checking
(they delegate to dfsLast/dfsLastFast for the final move).

---

## Phase 2: Roboyard Integration (RRGetMap.java)

**In `createDDWorld()` - After adding all goals:**
```java
// Collect all goals
List<Goal> allGoals = new ArrayList<>();
for (Object element : gridElements) {
    GridElement gridElement = (GridElement) element;
    String type = gridElement.getType();
    
    if (type.startsWith("target_")) {
        int x = gridElement.getX();
        int y = gridElement.getY();
        int position = y * board.width + x;
        int robotColor = colors.getOrDefault(type, Constants.COLOR_PINK);
        
        // Goal was already added with addGoal()
        // Find the goal object
        for (Goal g : board.getGoals()) {
            if (g.position == position && g.robotNumber == robotColor) {
                allGoals.add(g);
                break;
            }
        }
    }
}

// Set all active goals
if (allGoals.size() > 1) {
    board.setActiveGoals(allGoals);
    Timber.d("[SOLUTION_SOLVER] Set %d active goals for multi-target solving", allGoals.size());
}
```

---

## Feasibility Analysis

### ✅ Highly Feasible Changes

**Board.java modifications:**
- Replace `private Goal goal` with `private List<Goal> activeGoals`
- Add `getActiveGoals()` and `setActiveGoals()` methods
- **Impact:** Minimal, only 2 fields affected
- **Risk:** Low - only internal change

**SolverIDDFS.java modifications:**
- Replace `this.goalPosition` and `this.goalRobot` initialization (lines 67-69)
- Add 3 new fields: `activeGoals`, `completedGoals`, `isMultiGoalMode`
- Modify `dfsLast()` method (line ~348) to check multiple goals
- **Impact:** Localized changes in constructor and one search method
- **Risk:** Medium - core solver logic affected, needs thorough testing

### ⚠️ Potential Issues Found

**1. Other methods using `Board.goal`:**
- `Board.isSolution01()` (line 870-908) - Uses `this.goal.position`
- `Solver.java` - Uses `this.board.getGoal()` (line 78)
- `SolverIDDFS.execute()` (line 106) - Checks `null == this.board.getGoal()`
- **Solution:** Must update all references to use `activeGoals.get(0)` or check list size

**2. Heuristic optimization:**
- `precomputeMinimumMovesToGoal()` (line 134-150) - Uses `this.goalPosition`
- For multi-goal mode: Need to compute minimum moves to ANY goal, not just one
- **Solution:** Modify heuristic to find minimum distance to closest uncompleted goal

**3. State swapping logic:**
- `swapGoalLast()` (line 91-98) - Swaps goal robot to last position
- For multi-goal: Multiple robots need to be "goal robots"
- **Solution:** Keep only first goal robot as "last" for state swapping, or redesign state representation

### 🔴 Critical Issue: State Representation

**Problem:** DriftingDroid uses state array where goal robot is always last position
```java
int[] state = [robot0_pos, robot1_pos, robot2_pos, goalRobot_pos]
```

For multiple goals with different robots:
- Green robot goal at position A
- Yellow robot goal at position B
- Current state swapping assumes only ONE goal robot

**Solution Options:**
1. **Keep first goal as "primary"** - Only swap first goal robot to last, ignore others
   - Simpler implementation
   - May miss optimal solutions
   
2. **Redesign state representation** - Track all goal robots separately
   - Complex refactoring
   - Requires changes to `swapGoalLast()`, `dfsLast()`, `dfsLastFast()`
   - High risk of breaking existing functionality

### Naming Convention (Roboyard-specific)

**Important:** In Roboyard, the red robot is internally PINK (`COLOR_PINK = 0`)
- `robot_red` → `COLOR_PINK` (do not change!)
- `target_red` → `COLOR_PINK` (do not change!)
- DriftingDroid only sees color indices, not names
- **For tests:** Use `robot_green`, `robot_yellow` etc. (not `robot_red`)
- **Note:** Roboyard uses "target" terminology, but DriftingDroid internally uses "Goal"

---

## Effort Estimation

### Realistic Timeline

**Phase 0: Test-Driven Development (TDD Foundation)**
- Write unit tests: 1-2 hours
- Setup test infrastructure: 30 min
- **Subtotal: 1.5-2.5 hours**

**Phase 1: DriftingDroid Core Changes**
- Board.java modifications: 30 min (simple field replacement)
- SolverIDDFS constructor: 30 min (straightforward replacement)
- Update all `Board.goal` references: 1-1.5 hours (scattered across multiple methods)
  - `isSolution01()` (line 870-908)
  - `Solver.java` constructor (line 78)
  - `SolverIDDFS.execute()` (line 106)
- Modify `dfsLast()` goal check: 1 hour (careful implementation)
- **Subtotal: 3-3.5 hours**

**Phase 2: Roboyard Integration**
- RRGetMap.java: 30 min (collect and set goals)
- **Subtotal: 30 min**

**Phase 3: Testing & Debugging**
- Run unit tests: 30 min
- Fix failing tests: 1-2 hours
- Integration testing: 1-2 hours
- Edge case handling: 1 hour
- **Subtotal: 3.5-5.5 hours**

**Total Estimated Effort: 8.5-12 hours**

### Recommended Implementation Strategy

1. **Phase 0 (TDD First):** Write all unit tests BEFORE implementation
   - Effort: 1.5-2.5 hours
   - Ensures clear requirements and fast feedback loop

2. **Phase 1 (Core):** Implement basic multi-goal support
   - Replace `Board.goal` with `activeGoals` list
   - Update all references to use first goal as primary
   - Keep `swapGoalLast()` logic unchanged (only swap first goal robot)
   - Effort: 3-3.5 hours

3. **Phase 2 (Integration):** Connect to Roboyard
   - Collect and set active goals in RRGetMap
   - Effort: 30 min

4. **Phase 3 (Testing):** Run tests and fix issues
   - Execute unit tests
   - Debug failures
   - Verify no regressions
   - Effort: 3.5-5.5 hours

### Critical Success Factors

✅ **Must test thoroughly:**
- Existing single-goal games must still work
- Multi-goal games must find solutions
- No regression in solver performance

⚠️ **Known limitations:**
- State swapping optimization only applies to first goal robot
- Heuristic may not be optimal for all multi-goal scenarios
- Search space grows exponentially with more goals

### Benefits
✅ True multi-goal support (multiple targets in Roboyard)
✅ Finds valid solutions for multi-goal puzzles
✅ Minimal changes to existing single-goal logic
✅ Scalable to 3+ goals (with performance trade-off)
✅ Test-Driven Development ensures quality and regression prevention

### Risks
⚠️ Complex changes to core solver logic
⚠️ High risk of regressions - requires extensive testing
⚠️ Performance degradation with many goals
⚠️ State representation optimization only works for first goal

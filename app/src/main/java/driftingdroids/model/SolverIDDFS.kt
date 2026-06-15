/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011-2025 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package driftingdroids.model

import android.util.Log
import java.util.Arrays
import kotlin.concurrent.Volatile

class SolverIDDFS(board: Board) : Solver(board) {
    private val MAX_DEPTH: Int // maximal depth of search tree to prevent OOM

    private val states: Array<IntArray>
    private val directions: Array<IntArray>
    private val obstacles: Array<IntArray> // initialice in the SolverIDDFS constructor
    private var knownStates: KnownStates? = null
    private val goalPosition: Int
    private val minRobotLast: Int
    private val goalRobot: Int
    private val isSolution01: Boolean
    private val isSolution01NoSpeedup: Boolean
    private val minimumMovesToGoal: IntArray
    private val directionIncrement: IntArray

    // Multi-goal support
    private val isMultiGoalMode: Boolean
    private val activeGoalPositions: IntArray
    private val activeGoalRobots: IntArray

    // Memory monitoring: periodic check inside DFS recursion
    @Volatile
    private var memoryLow = false
    private var recursionCounter = 0
    private val memoryCheckInterval: Int // Check every N recursions (set in constructor)

    // Memory checks use freeBytes = maxMemory - totalMemory + freeMemory, abort if < 25% free
    private var depthLimit = 0


    init {
        // Multi-goal support: determine mode first to calculate correct MAX_DEPTH
        val activeGoals = this.board.getActiveGoals()
        this.isMultiGoalMode = (activeGoals.size > 1)
        this.activeGoalPositions = IntArray(activeGoals.size)
        this.activeGoalRobots = IntArray(activeGoals.size)
        for (i in activeGoals.indices) {
            this.activeGoalPositions[i] = activeGoals.get(i)!!.position
            this.activeGoalRobots[i] = activeGoals.get(i)!!.robotNumber
        }


        // Calculate MAX_DEPTH based on robot count and multi-goal mode
        this.MAX_DEPTH =
            if (this.isMultiGoalMode) getMaxDepthForMultiGoal(board.numRobots) else getMaxDepthForRobots(
                board.numRobots
            )


        // Set memory check interval: every recursion for multi-goal (DFS can allocate 100s MB between checks)
        this.memoryCheckInterval = if (this.isMultiGoalMode) 1 else 1000

        this.obstacles = Array(MAX_DEPTH) { IntArray(board.size) } // Initialize here
        this.initObstacles() // Call after MAX_DEPTH and obstacles are initialized
        this.states = Array(MAX_DEPTH) { IntArray(this.board.robotPositions.size) }
        this.directions = Array(MAX_DEPTH) { IntArray(this.board.robotPositions.size) }
        this.goalPosition = (if (null == this.board.getGoal()) 0 else this.board.getGoal().position)
        this.minRobotLast =
            (if (this.isBoardGoalWildcard) 0 else this.states[0].size - 1) //swapGoalLast
        this.goalRobot =
            (if (this.isBoardGoalWildcard) (if (null == this.board.getGoal()) 0 else this.board.getGoal().robotNumber) else this.minRobotLast) //swapGoalLast
        this.isSolution01 = this.board.isSolution01
        this.isSolution01NoSpeedup =
            (true == this.isSolution01) && ((true == this.isBoardGoalWildcard) || (4 > this.board.numRobots))
        this.minimumMovesToGoal = IntArray(board.size)
        this.directionIncrement = this.board.directionIncrement

        if (this.isMultiGoalMode) {
            Logger.println("[MULTI_GOAL] Multi-goal mode active with " + activeGoals.size + " goals, MAX_DEPTH=" + this.MAX_DEPTH)
            for (i in activeGoals.indices) {
                Logger.println("[MULTI_GOAL]   Goal " + i + ": robot=" + this.activeGoalRobots[i] + " position=" + this.activeGoalPositions[i])
            }
        }
    }


    private fun initObstacles() {
        this.obstacles[0] = IntArray(board.size)
        for (pos in this.obstacles[0].indices) {
            var obstacle = 0
            for (dir in 0..3) {
                if (true == this.boardWalls[dir][pos]) {
                    obstacle = obstacle or (1 shl dir)
                }
            }
            this.obstacles[0][pos] = obstacle
        }
        for (depth in 1..<this.obstacles.size) {
            this.obstacles[depth] = this.obstacles[0].clone()
        }
    }


    @Throws(InterruptedException::class)
    public override fun execute(): List<Solution> {
        val startExecute = System.nanoTime()
        this.lastResultSolutions = ArrayList<Solution>()

        Logger.println("***** " + this.javaClass.getSimpleName() + " *****")
        Logger.println("Options: " + this.getOptionsAsString())
        Logger.println(
            Log.DEBUG,
            "DriftingDroid",
            "[SOLVER_MEMORY] Number of robots: %d, Using MAX_DEPTH: %d",
            board.numRobots,
            this.MAX_DEPTH
        )
        val rtMem = Runtime.getRuntime()
        Logger.println(
            Log.DEBUG,
            "DriftingDroid",
            "[SOLVER_MEMORY] Available memory: %d MB (free=%d total=%d max=%d)",
            (rtMem.maxMemory() - rtMem.totalMemory() + rtMem.freeMemory()) / (1024 * 1024),
            rtMem.freeMemory() / (1024 * 1024),
            rtMem.totalMemory() / (1024 * 1024),
            rtMem.maxMemory() / (1024 * 1024)
        )

        if (null == this.board.getGoal()) {
            Logger.println("no goal is set - nothing to solve!")
        } else {
            this.states[0] = this.board.robotPositions.clone()
            swapGoalLast(this.states[0]) //goal robot is always the last one.
            Arrays.fill(this.directions[0], DIRECTION_NOT_MOVED_YET)
            this.precomputeMinimumMovesToGoal()
            this.knownStates = KnownStates()

            Logger.println("startState=" + this.stateString(this.states[0]))
            Logger.println("solution01=" + this.isSolution01 + "  isSolution01NoSpeedup=" + this.isSolution01NoSpeedup)
            Logger.println("goalWildcard=" + this.isBoardGoalWildcard)
            Logger.println(this.knownStates!!.info)

            this.iddfs()

            if (this.knownStates != null) {
                this.solutionStoredStates = this.knownStates!!.size()
                this.solutionMemoryMegabytes = this.knownStates!!.megaBytesAllocated
                this.knownStates = null //allow garbage collection
            }
        }
        this.sortSolutions()

        this.solutionMilliSeconds = (System.nanoTime() - startExecute) / 1000000L
        return this.lastResultSolutions!!
    }


    private fun precomputeMinimumMovesToGoal() {
        val posToDo = BooleanArray(this.minimumMovesToGoal.size)
        Arrays.fill(this.minimumMovesToGoal, Int.MAX_VALUE)
        this.minimumMovesToGoal[this.goalPosition] = 0
        posToDo[this.goalPosition] = true
        var done = false
        while (false == done) {
            done = true
            for (pos in posToDo.indices) {
                if (true == posToDo[pos]) {
                    posToDo[pos] = false
                    val depth = this.minimumMovesToGoal[pos] + 1
                    var dir = -1
                    for (dirIncr in this.directionIncrement) {
                        var newPos = pos
                        val walls = this.boardWalls[++dir]
                        while (false == walls[newPos]) {    //move the robot until it reaches a wall.
                            newPos += dirIncr //NOTE: we rely on the fact that all boards are surrounded by outer walls.
                            if (depth < this.minimumMovesToGoal[newPos]) {
                                this.minimumMovesToGoal[newPos] = depth
                                posToDo[newPos] = true
                                done = false
                            }
                        }
                    }
                }
            }
        }
    }


    @Throws(InterruptedException::class)
    private fun iddfs() {
        val nanoStart = System.nanoTime()
        val doDfsFast =
            (false == this.isBoardGoalWildcard) && (false == this.isSolution01) && (true == this.optAllowRebounds)
        Logger.println("doDfsFast=" + doDfsFast)
        if (this.isMultiGoalMode) {
            Logger.println("[MULTI_GOAL] Multi-goal mode: MAX_DEPTH limited to " + MAX_DEPTH + " to prevent OOM")
        }

        this.depthLimit = 2
        while (MAX_DEPTH > this.depthLimit) {
            // Check for thread interruption to allow graceful cancellation
            if (Thread.currentThread().isInterrupted()) {
                Logger.println("iddfs: Thread interrupted, stopping solver")
                throw InterruptedException("Solver was cancelled")
            }


            // Reset memory monitoring for this depth level
            this.memoryLow = false
            this.recursionCounter = 0

            val nanoDfs = System.nanoTime()
            try {
                if (doDfsFast) {
                    this.dfsRecursionFast(1, -1, -1, this.states[0])
                } else {
                    this.dfsRecursion(1, -1, -1, this.states[0], this.directions[0])
                }
            } catch (oom: OutOfMemoryError) {
                // Emergency: free knownStates immediately to reclaim memory
                this.knownStates = null
                // Do NOT call System.gc() here - it can trigger GcWatcher.finalize() timeout on Android
                Logger.println("[MEMORY] OOM caught in iddfs at depthLimit=" + this.depthLimit + " - freed knownStates")
                this.memoryLow = true
            }
            val nanoEnd = System.nanoTime()

            val rt = Runtime.getRuntime()
            val memPercent = ((rt.totalMemory() - rt.freeMemory()) * 100.0) / rt.maxMemory()
            val megaBytes =
                if (this.knownStates != null) this.knownStates!!.megaBytesAllocated else 0
            Logger.println(
                "iddfs:  finished depthLimit=" + this.depthLimit +
                        " megaBytes=" + megaBytes +
                        " memory=" + String.format("%.1f", memPercent) + "%" +
                        " time=" + (nanoEnd - nanoDfs) / 1000000L + "ms" +
                        " totalTime=" + (nanoEnd - nanoStart) / 1000000L + "ms"
            )


            // If memory was critically low during DFS, stop searching
            if (this.memoryLow) {
                Logger.println("[MEMORY] Stopping search: memory was critically low during depth " + this.depthLimit)
                break
            }

            if (false == this.lastResultSolutions!!.isEmpty()) {
                break //found solution(s)
            }
            ++this.depthLimit
        }
    }


    // standard version: supports wildcard goal, solution01 special case and option noRebounds
    @Throws(InterruptedException::class)
    private fun dfsRecursion(
        depth: Int,
        prevRobo: Int,
        prevDirBit0: Int,
        oldState: IntArray,
        oldDirs: IntArray
    ) {
        // Periodic memory check (shared counter with dfsRecursionFast)
        if (this.memoryLow) {
            return
        }
        if (++this.recursionCounter >= this.memoryCheckInterval) {
            this.recursionCounter = 0
            if (Thread.currentThread().isInterrupted()) {
                throw InterruptedException("Solver was cancelled")
            }
            val rt = Runtime.getRuntime()
            val freeBytes = rt.maxMemory() - rt.totalMemory() + rt.freeMemory()
            if (freeBytes < rt.maxMemory() / 2) { // abort if less than 50% free
                this.memoryLow = true
                return
            }
        }
        val height = this.depthLimit - depth + 1
        val minMovesToGoal: Int
        if (true == this.isBoardGoalWildcard) {
            var min = Int.MAX_VALUE
            for (pos in oldState) {
                val tmp = this.minimumMovesToGoal[pos]
                if (min > tmp) {
                    min = tmp
                }
            }
            minMovesToGoal = min
        } else {
            minMovesToGoal = this.minimumMovesToGoal[oldState[this.goalRobot]]
        }
        if (minMovesToGoal > height) {
            return  //useless to move any robot: can't reach goal
        }
        val obstacles = this.obstacles[depth]
        val newState = this.states[depth]
        val depth1 = depth + 1
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] or OBSTACLE_ROBOT
        } //set robot positions

        System.arraycopy(oldState, 0, newState, 0, oldState.size)
        val doRecursion = (this.depthLimit > depth1)
        //move all robots
        var robo = 0
        for (oldRoboPos in oldState) {
            val isGoalRobot = (this.goalRobot == robo) || (this.goalRobot < 0)
            if ((minMovesToGoal == height) && (false == isGoalRobot)) {
                ++robo
                continue  //useless to move this robot: can't reach goal
            }
            val oldDir = oldDirs[robo]
            val obstacleInit = obstacles[oldRoboPos]
            var dir = 0
            for (dirIncr in this.directionIncrement) {
                if (((true == this.optAllowRebounds) || ((oldDir != dir) && (oldDir != (dir xor 2)))) // (dir + 2) & 3
                    && ((prevRobo != robo) || (prevDirBit0 != (dir and 1)))
                ) {
                    var newRoboPos = oldRoboPos
                    var obstacle = obstacleInit
                    val wallMask = (1 shl dir)
                    while (0 == (obstacle and wallMask)) {        //move the robot until it reaches a wall or another robot.
                        newRoboPos += dirIncr //NOTE: we rely on the fact that all boards are surrounded
                        obstacle =
                            obstacles[newRoboPos] //by outer walls. without the outer walls we would need
                        if (0 != (obstacle and OBSTACLE_ROBOT)) { //some additional boundary checking here.
                            newRoboPos -= dirIncr
                            break
                        }
                    }
                    //the robot has actually moved
                    //special case (isSolution01): the goal robot has _NOT_ arrived at the goal
                    if ((oldRoboPos != newRoboPos)
                        && ((false == this.isSolution01) || !((this.goalPosition == newRoboPos) && (true == isGoalRobot)))
                    ) {
                        newState[robo] = newRoboPos
                        //special case (isSolution01): we must be able to visit states more than once, so we don't add them to knownStates
                        //the new state is not already known (i.e. stored in knownStates)
                        if (this.isSolution01NoSpeedup || (this.isSolution01 && isGoalRobot) || (this.knownStates!!.add(
                                newState,
                                height
                            ))
                        ) {
                            val newDirs = this.directions[depth]
                            System.arraycopy(oldDirs, 0, newDirs, 0, oldDirs.size)
                            newDirs[robo] = dir
                            if (true == doRecursion) {
                                this.dfsRecursion(depth1, robo, (dir and 1), newState, newDirs)
                            } else {
                                this.dfsLast(depth1, robo, (dir and 1), newState, newDirs)
                            }
                        }
                    }
                }
                ++dir
            }
            newState[robo++] = oldRoboPos
        }
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] xor OBSTACLE_ROBOT
        } //unset robot positions
    }


    // fast version: (false == this.isBoardGoalWildcard) && (false == this.isSolution01) && (true == this.optAllowRebounds)
    @Throws(InterruptedException::class)
    private fun dfsRecursionFast(depth: Int, prevRobo: Int, prevDirBit0: Int, oldState: IntArray) {
        // Periodic memory check: cheap flag test on every call, expensive Runtime check only every N calls
        if (this.memoryLow) {
            return  // Abort this branch - memory is critically low
        }
        if (++this.recursionCounter >= this.memoryCheckInterval) {
            this.recursionCounter = 0
            if (Thread.currentThread().isInterrupted()) {
                throw InterruptedException("Solver was cancelled")
            }
            val rt = Runtime.getRuntime()
            val freeBytes = rt.maxMemory() - rt.totalMemory() + rt.freeMemory()
            if (freeBytes < rt.maxMemory() / 2) { // abort if less than 50% free
                this.memoryLow = true
                return
            }
        }
        val minMovesToGoal = this.minimumMovesToGoal[oldState[this.goalRobot]]
        val height = this.depthLimit - depth + 1
        if (minMovesToGoal > height) {
            return  //useless to move any robot: can't reach goal
        }
        val obstacles = this.obstacles[depth]
        val newState = this.states[depth]
        val depth1 = depth + 1
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] or OBSTACLE_ROBOT
        } //set robot positions

        val doRecursion = (this.depthLimit > depth1)
        System.arraycopy(oldState, 0, newState, 0, oldState.size)
        //move all robots
        var robo = 0
        for (oldRoboPos in oldState) {
            if ((minMovesToGoal == height) && (this.goalRobot != robo)) {
                ++robo //useless to move this robot: can't reach goal
            } else {
                val obstacleInit = obstacles[oldRoboPos]
                var dir = 0
                for (dirIncr in this.directionIncrement) {
                    if ((prevRobo != robo) || (prevDirBit0 != (dir and 1))) {
                        var newRoboPos = oldRoboPos
                        var obstacle = obstacleInit
                        val wallMask = (1 shl dir)
                        while (0 == (obstacle and wallMask)) {        //move the robot until it reaches a wall or another robot.
                            newRoboPos += dirIncr //NOTE: we rely on the fact that all boards are surrounded
                            obstacle =
                                obstacles[newRoboPos] //by outer walls. without the outer walls we would need
                            if (0 != (obstacle and OBSTACLE_ROBOT)) { //some additional boundary checking here.
                                newRoboPos -= dirIncr
                                break
                            }
                        }
                        //the robot has actually moved
                        if (oldRoboPos != newRoboPos) {
                            newState[robo] = newRoboPos
                            //the new state is not already known (i.e. stored in knownStates)
                            if (true == this.knownStates!!.add(newState, height)) {
                                if (true == doRecursion) {
                                    this.dfsRecursionFast(depth1, robo, (dir and 1), newState)
                                } else {
                                    this.dfsLastFast(depth1, robo, (dir and 1), newState)
                                }
                            }
                        }
                    }
                    ++dir
                }
                newState[robo++] = oldRoboPos
            }
        }
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] xor OBSTACLE_ROBOT
        } //unset robot positions
    }


    // standard version: supports wildcard goal, solution01 special case and option noRebounds
    @Throws(InterruptedException::class)
    private fun dfsLast(
        depth: Int,
        prevRobo: Int,
        prevDirBit0: Int,
        oldState: IntArray,
        oldDirs: IntArray
    ) {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }
        val obstacles = this.obstacles[depth]
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] or OBSTACLE_ROBOT
        } //set robot positions

        //move goal robot(s) only
        for (robo in this.minRobotLast..<oldState.size) {
            val oldRoboPos = oldState[robo]
            val oldDir = oldDirs[robo]
            val obstacleInit = obstacles[oldRoboPos]
            var dir = 0
            for (dirIncr in this.directionIncrement) {
                if (((true == this.optAllowRebounds) || ((oldDir != dir) && (oldDir != (dir xor 2)))) // (dir + 2) & 3
                    && ((prevRobo != robo) || (prevDirBit0 != (dir and 1)))
                ) {
                    var newRoboPos = oldRoboPos
                    var obstacle = obstacleInit
                    val wallMask = (1 shl dir)
                    while (0 == (obstacle and wallMask)) {        //move the robot until it reaches a wall or another robot.
                        newRoboPos += dirIncr //NOTE: we rely on the fact that all boards are surrounded
                        obstacle =
                            obstacles[newRoboPos] //by outer walls. without the outer walls we would need
                        if (0 != (obstacle and OBSTACLE_ROBOT)) { //some additional boundary checking here.
                            newRoboPos -= dirIncr
                            break
                        }
                    }
                    //the robot has arrived at the goal
                    if ((this.goalPosition == newRoboPos) && hasPerpendicularMove(
                            depth,
                            robo,
                            dir
                        )
                    ) {
                        System.arraycopy(oldState, 0, this.states[depth], 0, oldState.size)
                        this.states[depth][robo] = newRoboPos
                        this.buildSolution(depth)
                    }
                }
                ++dir
            }
        }
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] xor OBSTACLE_ROBOT
        } //unset robot positions
    }


    // fast version: (false == this.isBoardGoalWildcard) && (false == this.isSolution01) && (true == this.optAllowRebounds)
    @Throws(InterruptedException::class)
    private fun dfsLastFast(depth: Int, prevRobo: Int, prevDirBit0: Int, oldState: IntArray) {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }
        val obstacles = this.obstacles[depth]
        val oldRoboPos = oldState[this.goalRobot]
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] or OBSTACLE_ROBOT
        } //set robot positions

        var dir = 0
        val obstacleInit = obstacles[oldRoboPos]
        //move goal robot only
        for (dirIncr in this.directionIncrement) {
            if ((prevRobo != this.goalRobot) || (prevDirBit0 != (dir and 1))) {
                var newRoboPos = oldRoboPos
                var obstacle = obstacleInit
                val wallMask = (1 shl dir)
                while (0 == (obstacle and wallMask)) {        //move the robot until it reaches a wall or another robot.
                    newRoboPos += dirIncr //NOTE: we rely on the fact that all boards are surrounded
                    obstacle =
                        obstacles[newRoboPos] //by outer walls. without the outer walls we would need
                    if (0 != (obstacle and OBSTACLE_ROBOT)) { //some additional boundary checking here.
                        newRoboPos -= dirIncr
                        break
                    }
                }
                //the robot has arrived at the goal
                if (this.goalPosition == newRoboPos) {
                    System.arraycopy(oldState, 0, this.states[depth], 0, oldState.size)
                    this.states[depth][this.goalRobot] = newRoboPos
                    this.buildSolution(depth)
                }
            }
            ++dir
        }
        for (pos in oldState) {
            obstacles[pos] = obstacles[pos] xor OBSTACLE_ROBOT
        } //unset robot positions
    }


    private fun hasPerpendicularMove(depth: Int, robot: Int, lastDir: Int): Boolean {
        var prevDir = this.directions[0][robot]
        var i = 1
        while (depth > i) {
            val thisDir = this.directions[i][robot]
            if ((((thisDir + 1) and 3) == prevDir) || (((thisDir + 3) and 3) == prevDir)) {
                return true
            }
            prevDir = thisDir
            ++i
        }
        return (((lastDir + 1) and 3) == prevDir) || (((lastDir + 3) and 3) == prevDir)
    }


    private fun buildSolution(depth: Int) {
        // Multi-goal check: verify ALL goals are reached in the final state
        if (this.isMultiGoalMode) {
            val finalState = this.states[depth]
            if (!isAllGoalsReached(finalState)) {
                return  // not all goals reached yet - skip this solution
            }
        }

        var newSolution = Solution(this.board)
        var state0 = this.states[0].clone()
        swapGoalLast(state0)
        for (i in 0..<depth) {
            val state1 = this.states[i + 1].clone()
            swapGoalLast(state1)
            newSolution.add(Move(this.board, state0, state1, i))
            state0 = state1
        }
        newSolution = newSolution.finish()
        Logger.println(
            newSolution.toMovelistString() + " " + newSolution.toString() + " finalState=" + this.stateString(
                states[depth]
            )
        )
        if (false == this.lastResultSolutions!!.contains(newSolution)) {
            this.lastResultSolutions!!.add(newSolution)
        }
    }

    /**
     * Check if all active goals are reached in the given state.
     * 
     * KEY INSIGHT:
     * The solver's existing single-goal search already finds the PRIMARY goal.
     * This method only needs to verify that ALL OTHER goals are ALSO reached in that same state.
     * 
     * Why this works with minimal code changes:
     * 1. The IDDFS algorithm searches for ANY state where the primary goal robot reaches its target
     * 2. When found, buildSolution() is called with that state
     * 3. We simply check: "Does this state also satisfy all other goals?"
     * 4. If not, we skip it (return early) and continue searching
     * 5. The search naturally continues until it finds a state satisfying ALL goals
     * 
     * State uses swapGoalLast format: the primary goal robot is at the last index.
     */
    private fun isAllGoalsReached(state: IntArray): Boolean {
        val primaryGoalRobotNumber =
            if (null == this.board.getGoal()) -1 else this.board.getGoal().robotNumber
        for (i in this.activeGoalPositions.indices) {
            var robotIdx = this.activeGoalRobots[i]
            // Account for swapGoalLast: primary goal robot is swapped to last position
            if (!this.isBoardGoalWildcard && robotIdx == primaryGoalRobotNumber) {
                robotIdx = state.size - 1
            } else if (!this.isBoardGoalWildcard && robotIdx == state.size - 1) {
                // The robot that was originally at last position is now at primary goal robot's position
                robotIdx = primaryGoalRobotNumber
            }
            if (state[robotIdx] != this.activeGoalPositions[i]) {
                return false
            }
        }
        return true
    }


    private inner class KnownStates {
        private val allKeys: AllKeys

        //store the unique keys of all known states
        private abstract inner class AllKeys protected constructor() {
            val theMap: KeyDepthMap

            init {
                this.theMap = KeyDepthMapFactory.newInstance(board)
            }

            abstract fun add(state: IntArray?, depth: Int): Boolean

            val bytesAllocated: Long
                get() = this.theMap.allocatedBytes()

            abstract val info: String
        }

        //store the unique keys of all known states in 32-bit ints
        //supports up to 4 robots with a board size of 256 (16*16)
        private inner class AllKeysInt : AllKeys() {
            private val keyMaker: KeyMakerInt? = KeyMakerInt.createInstance(
                board.numRobots,
                board.sizeNumBits,
                isBoardGoalWildcard,
                isSolution01
            )

            override fun add(state: IntArray?, depth: Int): Boolean {
                val key = this.keyMaker!!.run(state)
                return this.theMap.putIfGreater(key, depth)
            }

            override val info: String
                get() = this.javaClass.getSimpleName() + "," + this.theMap.javaClass.getSimpleName() + "," + (if (null == this.keyMaker) "n/a" else this.keyMaker.javaClass.getSimpleName())
        }

        //store the unique keys of all known states in 64-bit longs
        //supports more than 4 robots and/or board sizes larger than 256
        private inner class AllKeysLong : AllKeys() {
            private val keyMaker: KeyMakerLong? = KeyMakerLong.createInstance(
                board.numRobots,
                board.sizeNumBits,
                isBoardGoalWildcard,
                isSolution01
            )

            override fun add(state: IntArray?, depth: Int): Boolean {
                val key = this.keyMaker!!.run(state)
                return this.theMap.putIfGreater(key, depth)
            }

            override val info: String
                get() = this.javaClass.getSimpleName() + "," + this.theMap.javaClass.getSimpleName() + "," + (if (null == this.keyMaker) "n/a" else this.keyMaker.javaClass.getSimpleName())
        }

        // Deterministic memory limit (Runtime.freeMemory is unreliable on Android ART):
        // - maxBytes: Trie byte limit (70% of heap) - checked every 500 states
        // This ensures the solver stops BEFORE exhausting physical RAM.
        private val maxBytes: Long
        private var stateCount = 0

        init {
            val maxHeap = Runtime.getRuntime().maxMemory()
            // Budget 70% of heap for Trie states
            maxBytes = (maxHeap * 70) / 100
            Logger.println("[MEMORY] KnownStates maxBytes=" + (maxBytes shr 20) + "MB (heap=" + (maxHeap shr 20) + "MB)")
        }

        init {
            this.allKeys =
                if (board.sizeNumBits * (board.numRobots - (if (isSolution01) 1 else 0)) <= 32) AllKeysInt() else AllKeysLong()
        }

        fun add(state: IntArray?, depth: Int): Boolean {
            if (memoryLow) return false
            // Expensive Trie-internal check every 500 states
            if (stateCount > 0 && stateCount % 500 == 0) {
                val allocated = this.allKeys.bytesAllocated
                if (allocated > maxBytes) {
                    Logger.println("[MEMORY] knownStates aborted: Trie " + (allocated shr 20) + "MB > limit " + (maxBytes shr 20) + "MB at " + stateCount + " states")
                    memoryLow = true
                    return false
                }
            }
            try {
                val added = this.allKeys.add(state, depth)
                if (added) stateCount++
                return added
            } catch (oom: OutOfMemoryError) {
                Logger.println("[MEMORY] OOM in knownStates.add() at " + stateCount + " states - aborting search")
                memoryLow = true
                return false
            }
        }

        fun size(): Int {
            return this.allKeys.theMap.size()
        }

        val megaBytesAllocated: Int
            get() = ((this.allKeys.bytesAllocated + (1 shl 20) - 1) shr 20).toInt()
        val info: String
            get() = "KnownStates(" + this.allKeys.info + ")"
    }

    companion object {
        // Lower MAX_DEPTH for 5+ robots to prevent OOM errors
        // The search space grows exponentially with more robots
        private fun getMaxDepthForRobots(numRobots: Int): Int {
            // Scale down max depth based on number of robots to prevent OOM
            if (numRobots >= 5) {
                // Much lower depth for 5+ robots since search space is exponentially larger
                return 24
            } else if (numRobots >= 4) {
                return 64
            } else {
                return 126 // Original MAX_DEPTH for 1-3 robots
            }
        }

        // Calculate max depth for multi-goal mode
        // Multi-goal search space is exponentially larger, but memory check prevents OOM
        // These limits allow finding solutions while memory monitoring prevents crashes
        private fun getMaxDepthForMultiGoal(numRobots: Int): Int {
            if (numRobots >= 5) {
                return 18 // 5+ robots: very large branching factor
            } else if (numRobots >= 4) {
                return 25 // 4 robots: DFS call-stack explodes at depth 15+ on 512MB heap
            } else {
                return 30 // 1-3 robots: smaller branching factor allows deeper search
            }
        }

        private const val DIRECTION_NOT_MOVED_YET = 7
        private val OBSTACLE_ROBOT = (1 shl 4)
    }
}

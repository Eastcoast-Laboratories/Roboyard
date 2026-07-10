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

abstract class Solver protected constructor(board: Board) {
    enum class SOLUTION_MODE(private val modeName: String, private val l10nKey: String) {
        MINIMUM("minimum", "solver.Minimum.text"),
        MAXIMUM("maximum", "solver.Maximum.text");

        override fun toString(): String {
            return L10N.getString(this.l10nKey)
        }

        fun getName(): String {
            return this.modeName
        }
    }

    companion object {
       
        val USE_SLOW_SEARCH_MORE_SOLUTIONS: Boolean

        init {
            var useSlowSearchMoreSolutions = false // TODO: test slow solver with more solutions
            try {
                useSlowSearchMoreSolutions = false // TODO("System.getProperty not available in commonMain")
            } catch (ignored: Exception) {
            }
            USE_SLOW_SEARCH_MORE_SOLUTIONS = useSlowSearchMoreSolutions
        }

        @JvmStatic
        fun createInstance(board: Board): Solver {
            return SolverIDDFS(board)
        }
    }

   
    protected val board: Board
   
    protected val boardWalls: Array<BooleanArray>
   
    protected val boardSizeBitMask: Int
   
    protected val isBoardStateInt32: Boolean
   
    protected val isBoardGoalWildcard: Boolean

   
    protected var optSolutionMode: SOLUTION_MODE = SOLUTION_MODE.MINIMUM
   
    protected var optAllowRebounds: Boolean = true

   
    protected var lastResultSolutions: MutableList<Solution>? = null
   
    protected var solutionMilliSeconds: Long = 0
   
    protected var solutionStoredStates: Int = 0
   
    protected var solutionMemoryMegabytes: Int = 0

    init {
        this.board = board
        this.boardWalls = this.board.walls
        var bitMask = 0
        for (i in 0 until this.board.sizeNumBits) {
            bitMask += bitMask + 1
        }
        this.boardSizeBitMask = bitMask
        this.isBoardStateInt32 = this.board.sizeNumBits * this.board.numRobots <= 32
        this.isBoardGoalWildcard = (null != this.board.getGoal() && this.board.getGoal().robotNumber < 0)
    }

    @Throws(Exception::class)
    abstract fun execute(): List<Solution>

    protected fun stateString(state: IntArray): String {
        val formatter = StringBuilder()
        this.swapGoalLast(state)
        for (i in state) {
            formatter.append(i.toString(16).padStart(2, '0'))
        }
        this.swapGoalLast(state)
        return "0x" + formatter.toString()
    }

    protected fun swapGoalLast(state: IntArray) {
        // swap goal robot and last robot (if goal is not wildcard)
        if (!this.isBoardGoalWildcard) {
            val tmp = state[state.size - 1]
            state[state.size - 1] = state[this.board.getGoal().robotNumber]
            state[this.board.getGoal().robotNumber] = tmp
        }
    }

    protected fun sortSolutions() {
        if (0 == this.lastResultSolutions!!.size) {
            this.lastResultSolutions!!.add(Solution(this.board))
        }
        if (SOLUTION_MODE.MINIMUM == this.optSolutionMode) {
            this.lastResultSolutions!!.sort()
        } else if (SOLUTION_MODE.MAXIMUM == this.optSolutionMode) {
            this.lastResultSolutions!!.sortDescending()
        }
    }

    fun get(): List<Solution> {
        return this.lastResultSolutions!!
    }

    fun setOptionSolutionMode(mode: SOLUTION_MODE) {
        this.optSolutionMode = mode
    }

    fun getOptionSolutionMode(): SOLUTION_MODE {
        return this.optSolutionMode
    }

    fun setOptionAllowRebounds(allowRebounds: Boolean) {
        this.optAllowRebounds = allowRebounds
    }

    fun getOptionAllowRebounds(): Boolean {
        return this.optAllowRebounds
    }

    fun getOptionsAsString(): String {
        return this.optSolutionMode.getName() + " number of robots moved; " +
                (if (this.optAllowRebounds) "with" else "no") + " rebound moves"
    }

    override fun toString(): String {
        val s = StringBuilder()
        s.append("storedStates=").append(this.solutionStoredStates)
        s.append(", time=").append(this.solutionMilliSeconds / 1000.0).append(" seconds")
        return s.toString()
    }
}

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

import java.util.Collections
import java.util.Formatter

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
        @JvmField
        val USE_SLOW_SEARCH_MORE_SOLUTIONS: Boolean

        init {
            var useSlowSearchMoreSolutions = false // TODO: test slow solver with more solutions
            try {
                useSlowSearchMoreSolutions = null != System.getProperty("UseSlowSearchMoreSolutions")
            } catch (ignored: Exception) {
            }
            USE_SLOW_SEARCH_MORE_SOLUTIONS = useSlowSearchMoreSolutions
        }

        @JvmStatic
        fun createInstance(board: Board): Solver {
            return SolverIDDFS(board)
        }
    }

    @JvmField
    protected val board: Board
    @JvmField
    protected val boardWalls: Array<BooleanArray>
    @JvmField
    protected val boardSizeBitMask: Int
    @JvmField
    protected val isBoardStateInt32: Boolean
    @JvmField
    protected val isBoardGoalWildcard: Boolean

    @JvmField
    protected var optSolutionMode: SOLUTION_MODE = SOLUTION_MODE.MINIMUM
    @JvmField
    protected var optAllowRebounds: Boolean = true

    @JvmField
    protected var lastResultSolutions: MutableList<Solution>? = null
    @JvmField
    protected var solutionMilliSeconds: Long = 0
    @JvmField
    protected var solutionStoredStates: Int = 0
    @JvmField
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

    @Throws(InterruptedException::class)
    abstract fun execute(): List<Solution>

    protected fun stateString(state: IntArray): String {
        val formatter = Formatter()
        this.swapGoalLast(state)
        for (i in state) {
            formatter.format("%02x", i)
        }
        this.swapGoalLast(state)
        return "0x" + formatter.out().toString()
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
            Collections.sort(this.lastResultSolutions as List<Solution>)
        } else if (SOLUTION_MODE.MAXIMUM == this.optSolutionMode) {
            Collections.sort(this.lastResultSolutions as List<Solution>, Collections.reverseOrder())
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

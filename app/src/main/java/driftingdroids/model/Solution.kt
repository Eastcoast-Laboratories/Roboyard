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

import java.util.Arrays
import java.util.Deque
import java.util.Formatter
import java.util.LinkedList
import java.util.TreeSet

class Solution(private val board: Board) : Comparable<Solution> {
    private val movesList: MutableList<Move>
    private var moveIndex = 0
    private var numColors = 0
    private var numColorChanges = 0
    private var movedRobots = 0
    private var finalPositions: Long = 0

    init {
        this.movesList = ArrayList<Move>()
    }


    fun add(move: Move?) {
        this.movesList.add(move!!)
    }

    fun size(): Int {
        return this.movesList.size
    }

    val robotsMoved: MutableSet<Int>
        get() {
            val result = TreeSet<Int>() //sorted set
            for (move in this.movesList) {
                result.add(move.robotNumber)
            }
            return result
        }

    val isRebound: Boolean
        get() = this.isRebound(null)

    fun isRebound(queryMove: Move?): Boolean {
        var result = false
        val directions = this.board.robotPositions.clone()
        Arrays.fill(directions, -1)
        for (move in this.movesList) {
            if ((-1 == directions[move.robotNumber]) || (move.direction != (3 and (directions[move.robotNumber] + 2)))) {
                directions[move.robotNumber] = move.direction
            } else {
                if ((queryMove == null) || (queryMove.equals(move))) {
                    result = true
                    break
                }
            }
        }
        return result
    }

    fun resetMoves() {
        this.moveIndex = 0
    }

    val currentMove: Move?
        get() {
            if ((this.moveIndex >= 0) && (this.moveIndex < this.movesList.size)) {
                return this.movesList.get(this.moveIndex)
            } else {
                return null
            }
        }

    val _nextMove: Move?
        get() {
            val result = this.currentMove
            if (null != result) {
                this.moveIndex++
            }
            return result
        }

    fun getNextMove(): Move? {
        return this._nextMove
    }

    val prevMove: Move?
        get() {
            if (this.moveIndex > 0) {
                this.moveIndex--
            }
            return this.currentMove
        }

    val lastMove: Move?
        get() {
            if (this.movesList.size > 0) {
                return this.movesList.get(this.movesList.size - 1)
            } else {
                return null
            }
        }

    fun toMovelistString(): String {
        val s = StringBuilder()
        s.append("solution: size=").append(this.movesList.size).append(" *****")
        if (this.movesList.size > 0) {
            for (mov in this.movesList) {
                s.append(' ')
                s.append(mov.strRobotDirection())
            }
        }
        s.append(" *****")
        return s.toString()
    }

    override fun toString(): String {
        val s = StringBuilder()
        // 1. number of moves
        val f = Formatter(s)
        f.format("%02d", this.size())
        // 2. number of robots moved
        val thisRobotsMoved = this.robotsMoved
        s.append('/').append(thisRobotsMoved.size).append('/')
        // 3. list of robots moved
        for (i in this.board.robotPositions.indices) {
            if (thisRobotsMoved.contains(i)) {
                s.append(Board.ROBOT_COLOR_NAMES_SHORT[i])
            } else {
                s.append('#')
            }
        }
        return s.toString()
    }

    override fun compareTo(other: Solution): Int {
        // 1. compare number of moves
        if (this.size() < other.size()) {
            return -1
        } else if (this.size() > other.size()) {
            return 1
        } else {    //equal number of moves

            // 2. compare number of robots moved

            if (this.numColors < other.numColors) {
                return -1
            } else if (this.numColors > other.numColors) {
                return 1
            } else {    //equal number of robots moved

                // 3. compare the actual robots moved

                if (this.movedRobots < other.movedRobots) {
                    return -1
                } else if (this.movedRobots > other.movedRobots) {
                    return 1
                } else {    //equal robots moved

                    // 4. compare final robot positions

                    if (this.finalPositions < other.finalPositions) {
                        return -1
                    } else if (this.finalPositions > other.finalPositions) {
                        return 1
                    } else {    // equal final positions
                        return 0
                    }
                }
            }
        }
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is Solution) {
            return this.movesList == obj.movesList
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        var result = 0
        for (move in this.movesList) {
            result = 1000003 * result + move.hashCode()
        }
        return result
    }

    // set attributes used for sorting of solutions and swap some moves to minimize color changes
    fun finish(): Solution {
        val colorSolution = this.determineColorChanges()
        // set the attributes used for sorting of solutions
        this.numColorChanges = colorSolution.size
        val robotsMoved = this.robotsMoved
        this.numColors = robotsMoved.size
        this.movedRobots = 0
        for (robo in robotsMoved) {
            this.movedRobots = this.movedRobots or (1 shl (30 - robo))
        }
        this.finalPositions = this.movesList.get(this.movesList.size - 1).newPositions
        // for solution01 the order of moves is important and should not be changed here
        if (false == this.board.isSolution01) {
            this.minimizeColorChanges(colorSolution)
        }
        return this
    }

    // transform Solution to list of lists of moves, grouped by colors
    private fun determineColorChanges(): MutableList<MutableList<Move>> {
        val colorSolution: MutableList<MutableList<Move>> = ArrayList<MutableList<Move>>()
        var moveList = LinkedList<Move>()
        for (move in this.movesList) {
            if ((false == moveList.isEmpty()) && (moveList.getLast().robotNumber != move.robotNumber)) { // color change
                colorSolution.add(moveList)
                moveList = LinkedList<Move>()
            }
            moveList.add(move)
        }
        colorSolution.add(moveList)
        return colorSolution
    }

    // prettify the solution: transpose some moves and thus create longer runs of moves of the same robot color
    private fun minimizeColorChanges(thisSolution: MutableList<MutableList<Move>>) {
        var thisSolution = thisSolution
        val startNano = System.nanoTime()
        if (this.numColors == this.numColorChanges) {
            Logger.println("minimizeColorChanges: no search, already at global minimum " + this.numColorChanges)
            return  // nothing to be minimized here
        }
        val knownSet: MutableSet<MutableList<MutableList<Move>>> =
            HashSet<MutableList<MutableList<Move>>>()
        val todoList: Deque<MutableList<MutableList<Move>>> =
            LinkedList<MutableList<MutableList<Move>>>()
        knownSet.add(thisSolution)
        todoList.addLast(thisSolution)
        search_loop@ while (false == todoList.isEmpty()) {
            thisSolution = todoList.removeFirst()
            // iterate the lists of moves, try to swap adjacent lists
            try_swap_loop@ for (i in 0..<thisSolution.size - 2) {
                var thisMoves: MutableList<Move> = thisSolution.get(i)
                var nextMoves: MutableList<Move> = thisSolution.get(i + 1)
                // check if the lists of moves can be swapped
                for (move1 in thisMoves) {
                    for (move2 in nextMoves) {
                        if (move1.pathMap.containsKey(move2.newPosition) ||
                            move2.pathMap.containsKey(move1.oldPosition)
                        ) {
                            Logger.println("minimizeColorChanges: blocked path  " + move1.toString() + "  " + move2.toString())
                            continue@try_swap_loop  // no swap - blocked path
                        }
                        if ((move1.newPosition == move2.oldPosition - board.directionIncrement[move1.direction]) ||
                            (move2.newPosition == move1.newPosition - board.directionIncrement[move2.direction])
                        ) {
                            Logger.println("minimizeColorChanges: blocker position  " + move1.toString() + "  " + move2.toString())
                            continue@try_swap_loop  // no swap - blocker position
                        }
                    }
                }
                // swap
                val nextSolution: MutableList<MutableList<Move>> =
                    ArrayList<MutableList<Move>>(thisSolution)
                nextSolution.set(i, nextMoves)
                nextSolution.set(i + 1, thisMoves)
                // merge same-colored adjacent lists of moves
                thisMoves = nextSolution.get(0)
                var j = 1
                while (j < nextSolution.size) {
                    nextMoves = nextSolution.get(j)
                    if (thisMoves.get(0).robotNumber == nextMoves.get(0).robotNumber) {
                        thisMoves = LinkedList<Move>(thisMoves)
                        thisMoves.addAll(nextMoves)
                        nextSolution.set(j - 1, thisMoves)
                        nextSolution.removeAt(j--)
                        Logger.println(
                            "minimizeColorChanges: merged " + Board.ROBOT_COLOR_NAMES_LONG[thisMoves.get(
                                0
                            ).robotNumber]
                        )
                    } else {
                        thisMoves = nextMoves
                    }
                    ++j
                }
                // if this is a new minimum of color changes then update the solution
                if (this.numColorChanges > nextSolution.size) {
                    Logger.println("minimizeColorChanges: reduced from " + this.numColorChanges + " to " + nextSolution.size)
                    this.numColorChanges = nextSolution.size
                    this.movesList.clear()
                    var stepNumber = 0
                    for (moves in nextSolution) {
                        for (move in moves) {
                            move.stepNumber = stepNumber++ // re-number moves
                            this.movesList.add(move)
                        }
                    }
                    knownSet.clear()
                    todoList.clear()
                    if (this.numColors == this.numColorChanges) { // global minimum reached
                        Logger.println("minimizeColorChanges: global minimum reached " + this.numColorChanges)
                        break@search_loop  // end of search
                    }
                }
                if (true == knownSet.add(nextSolution)) {
                    todoList.addLast(nextSolution)
                }
            }
        }
        val millis = (System.nanoTime() - startNano) / 1000000L
        Logger.println("minimizeColorChanges: finished after " + millis + " ms.")
    }
}


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

class Move(
    val board: Board,
    oldPositions: IntArray,
    newPositions: IntArray,
    @JvmField var stepNumber: Int
) {
   
    @JvmField val robotNumber: Int
   
    @JvmField val oldPosition: Int
   
    @JvmField val newPosition: Int
   
    @JvmField val direction: Int
   
    @JvmField val pathMap: MutableMap<Int, Int> // key=position, value=PATH
   
    @JvmField val oldPositions: Long // positions of all robots before this move
   
    @JvmField val newPositions: Long // positions of all robots after this move

    companion object {
        @JvmField val PATH_NORTH = 1 shl Board.NORTH
        @JvmField val PATH_EAST = 1 shl Board.EAST
        @JvmField val PATH_SOUTH = 1 shl Board.SOUTH
        @JvmField val PATH_WEST = 1 shl Board.WEST
    }

    init {
        require(oldPositions.size == newPositions.size) { "Move states must have equal robot counts" }
        require(oldPositions.size == board.numRobots) {
            "Move state robot count must match board: state=${oldPositions.size} board=${board.numRobots}"
        }
        val changedRobots = oldPositions.indices.filter { oldPositions[it] != newPositions[it] }
        require(changedRobots.size == 1) { "A move must change exactly one robot, changed=${changedRobots.size}" }
        val robotNum = changedRobots.single()
        val oldPos = oldPositions[robotNum]
        val newPos = newPositions[robotNum]
        require(oldPos in 0 until board.size && newPos in 0 until board.size) {
            "Move positions must be on the board: old=$oldPos new=$newPos size=${board.size}"
        }
        val diffPos = newPos - oldPos
        val direction = board.getDirection(diffPos)
        val aligned = when (direction) {
            Board.EAST, Board.WEST -> oldPos / board.width == newPos / board.width
            Board.NORTH, Board.SOUTH -> oldPos % board.width == newPos % board.width
            else -> false
        }
        require(aligned) { "Move positions must be aligned: old=$oldPos new=$newPos" }
        val posIncr = board.directionIncrement[direction]
        require(diffPos % posIncr == 0) { "Move does not follow direction increment" }
        val stepCount = diffPos / posIncr
        require(stepCount > 0) { "Move must advance at least one cell" }
        this.robotNumber = robotNum
        this.oldPosition = oldPos
        this.newPosition = newPos
        this.direction = direction

        this.pathMap = HashMap()
        val pathStart = 1 shl direction
        val pathEnd = 1 shl board.getDirection(-diffPos)
        this.pathMap[oldPos] = pathStart
        for (step in 1 until stepCount) {
            this.pathMap[oldPos + step * posIncr] = pathStart + pathEnd
        }
        this.pathMap[newPos] = pathEnd

        var oldPosLong = 0L
        for (pos in oldPositions) {
            oldPosLong = (oldPosLong shl board.sizeNumBits) or pos.toLong()
        }
        this.oldPositions = oldPosLong
        var newPosLong = 0L
        for (pos in newPositions) {
            newPosLong = (newPosLong shl board.sizeNumBits) or pos.toLong()
        }
        this.newPositions = newPosLong
    }

    override fun equals(obj: Any?): Boolean {
        if (null == obj || obj !is Move) {
            return false
        }
        val other = obj
        return ((this.stepNumber == other.stepNumber) &&
                (this.robotNumber == other.robotNumber) &&
                (this.oldPosition == other.oldPosition) &&
                (this.newPosition == other.newPosition))
    }

    override fun hashCode(): Int {
        var result = this.stepNumber
        result = 1000003 * result + this.robotNumber
        result = 1000003 * result + this.oldPosition
        result = 1000003 * result + this.newPosition
        return result
    }

    override fun toString(): String {
        return ((this.stepNumber + 1).toString() + ": " + this.strRobotDirection() + " " + this.strOldNewPosition())
    }

    fun strRobotDirection(): String {
        val dir: String
        when (this.pathMap[this.oldPosition]) {
            PATH_NORTH -> dir = "N" // up    / NORTH
            PATH_EAST -> dir = "E" // right / EAST
            PATH_SOUTH -> dir = "S" // down  / SOUTH
            PATH_WEST -> dir = "W" // left  / WEST
            else -> dir = "?"
        }
        return (Board.ROBOT_COLOR_NAMES_SHORT[this.robotNumber] + dir)
    }

    fun strDirectionL10N(): String {
        val dir: String
        when (this.pathMap[this.oldPosition]) {
            PATH_NORTH -> dir = L10N.getString("move.direction.N.text") // up    / NORTH
            PATH_EAST -> dir = L10N.getString("move.direction.E.text") // right / EAST
            PATH_SOUTH -> dir = L10N.getString("move.direction.S.text") // down  / SOUTH
            PATH_WEST -> dir = L10N.getString("move.direction.W.text") // left  / WEST
            else -> dir = "?"
        }
        return dir
    }

    fun strDirectionL10Nlong(): String {
        val dir: String
        when (this.pathMap[this.oldPosition]) {
            PATH_NORTH -> dir = L10N.getString("move.direction.North.text") // up
            PATH_EAST -> dir = L10N.getString("move.direction.East.text") // right
            PATH_SOUTH -> dir = L10N.getString("move.direction.South.text") // down
            PATH_WEST -> dir = L10N.getString("move.direction.West.text") // left
            else -> dir = "???"
        }
        return dir
    }

    fun strOldNewPosition(): String {
        return ("(" + (this.oldPosition % this.board.width) + "," + (this.oldPosition / this.board.width) +
                ") -> (" + (this.newPosition % this.board.width) + "," + (this.newPosition / this.board.width) + ")")
    }
}

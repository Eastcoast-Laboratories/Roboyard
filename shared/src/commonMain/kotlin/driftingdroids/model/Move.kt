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
    @JvmField
    val robotNumber: Int
    @JvmField
    val oldPosition: Int
    @JvmField
    val newPosition: Int
    @JvmField
    val direction: Int
    @JvmField
    val pathMap: MutableMap<Int, Int> // key=position, value=PATH
    @JvmField
    val oldPositions: Long // positions of all robots before this move
    @JvmField
    val newPositions: Long // positions of all robots after this move

    companion object {
        val PATH_NORTH = 1 shl Board.NORTH
        val PATH_EAST = 1 shl Board.EAST
        val PATH_SOUTH = 1 shl Board.SOUTH
        val PATH_WEST = 1 shl Board.WEST
    }

    init {
        var robotNum = 0
        var oldPos = 0
        var newPos = 0
        for (robo in oldPositions.indices) {
            if (oldPositions[robo] != newPositions[robo]) {
                robotNum = robo
                oldPos = oldPositions[robo]
                newPos = newPositions[robo]
                break
            }
        }
        this.robotNumber = robotNum
        this.oldPosition = oldPos
        this.newPosition = newPos

        this.pathMap = HashMap()
        val diffPos = newPos - oldPos
        this.direction = board.getDirection(diffPos)
        val pathStart = 1 shl this.direction
        val pathEnd = 1 shl board.getDirection(-diffPos)
        val posIncr = board.directionIncrement[this.direction]
        var i = oldPos
        this.pathMap[i] = pathStart
        i += posIncr
        while (i != newPos) {
            this.pathMap[i] = pathStart + pathEnd
            i += posIncr
        }
        this.pathMap[i] = pathEnd

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

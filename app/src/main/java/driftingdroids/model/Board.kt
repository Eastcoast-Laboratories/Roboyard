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

import android.os.Build
import roboyard.logic.core.Constants
import roboyard.ui.activities.MainActivity
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.Base64
import java.util.Collections
import java.util.Formatter
import java.util.Random
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.min

/**
 * Board class represents the game board state including walls, robots, and goals.
 * Handles board creation, modification, and game state management.
 */
class Board private constructor(val width: Int, val height: Int, numRobots: Int) {
    val size: Int // width * height
    val sizeNumBits: Int //number of bits required to store any board position (size - 1)

    val directionIncrement: IntArray

    private val quadrants: IntArray // quadrants used for this board (indexes in QUADRANTS) 

    /**
     * Gets wall configuration of the board.
     * @return 2D boolean array representing walls
     */
    val walls: Array<BooleanArray> // [4][width*height] 4 directions
    val goals: MutableList<Goal> // all possible goals on the board
    private val randomGoals: MutableList<Goal?>
    private var goal: Goal? // the current goal
    private var activeGoals: MutableList<Goal?>? = null // all active goals for multi-goal mode

    var robotPositions: IntArray // index=robot, value=position
        private set

    /**
     * Checks if board is freestyle type.
     * @return true if freestyle board, false otherwise
     */
    var isFreestyleBoard: Boolean
        private set

    /**
     * Inner class representing a goal on the board with position, robot, and shape information.
     * Implements Comparable to allow sorting of goals by robot number, shape, and position.
     */
    inner class Goal(val x: Int, val y: Int, val robotNumber: Int, val shape: Int) :
        Comparable<Goal?> {
        val position: Int

        init {
            this.position = x + y * width
        }

        override fun equals(obj: Any?): Boolean {
            if ((null == obj) || obj !is Goal) {
                return false
            }
            val other = obj
            return ((this.x == other.x) &&
                    (this.y == other.y) &&
                    (this.position == other.position) &&
                    (this.robotNumber == other.robotNumber) &&
                    (this.shape == other.shape))
        }

        override fun hashCode(): Int {
            var result = this.x
            result = 1000003 * result + this.y
            result = 1000003 * result + this.position
            result = 1000003 * result + this.robotNumber
            result = 1000003 * result + this.shape
            return result
        }

        // a List<Goal> will be sorted by:
        // 1. robotNumber a.k.a. color
        // 2. shape
        // 3. position
        override fun compareTo(other: Goal): Int {
            val thisColor = (if (this.robotNumber < 0) Int.MAX_VALUE else this.robotNumber)
            val otherColor = (if (other.robotNumber < 0) Int.MAX_VALUE else other.robotNumber)
            if (thisColor < otherColor) {
                return -1 // less
            } else if (thisColor > otherColor) {
                return 1 // greater
            } else { // robotNumbers are equal
                if (this.shape < other.shape) {
                    return -1 // less
                } else if (this.shape > other.shape) {
                    return 1 // greater
                } else { // shapes are equal
                    if (this.position < other.position) {
                        return -1 // less
                    } else if (this.position > other.position) {
                        return 1 // greater
                    } else {
                        return 0 // equal
                    }
                }
            }
        }
    }


    /**
     * Creates a new board with specified dimensions and number of robots.
     * Initializes board state including walls, robots, and goals.
     * @param width Width of the board
     * @param height Height of the board
     * @param numRobots Number of robots to place
     */
    init {
        this.size = width * height
        this.sizeNumBits = 32 - Integer.numberOfLeadingZeros(this.size - 1) //ceil(log2(x))
        this.directionIncrement = IntArray(4)
        this.directionIncrement[NORTH] = -width
        this.directionIncrement[EAST] = 1
        this.directionIncrement[SOUTH] = width
        this.directionIncrement[WEST] = -1
        this.quadrants = IntArray(4)
        this.walls = Array<BooleanArray?>(4) { BooleanArray(width * height) }  //filled with "false"
        this.robotPositions = IntArray(numRobots)
        this.setRobots(numRobots)
        this.goals = ArrayList<Goal>()
        this.randomGoals = ArrayList<Goal?>()
        this.goal = Goal(0, 0, 0, 0) //dummy
        this.isFreestyleBoard = false
    }


    val gameID: String
        /**
         * The game ID string consists of this info:
         * - the 4 board quadrants (4 board pieces, front or back)
         * - how many robots are on board
         * - which one is the active robot (goalRobot)
         * - positions of all robots
         * - position of goal
         * @return ID string of this board configuration
         */
        get() {
            if (this.isFreestyleBoard) {
                return "freestyle"
            }
            val fmt = Formatter()
            val quad01 = (this.getQuadrantNum(0) shl 4) or this.getQuadrantNum(1)
            val quad23 = (this.getQuadrantNum(2) shl 4) or this.getQuadrantNum(3)
            fmt.format("%02X%02X+", quad01, quad23)
            val robos =
                (this.robotPositions.size shl 4) or (if (this.goal!!.robotNumber >= 0) this.goal!!.robotNumber else 0x0f)
            fmt.format("%02X+", robos)
            for (robot in this.robotPositions) {
                fmt.format("%02X", robot)
            }
            fmt.format("+%02X", this.goal!!.position)
            return fmt.toString()
        }


    val gameDump: String
        /**
         * Creates a specific text representation of this Board object. This is printable
         * text which is suitable for copy&paste into e-mails, internet forums etc.
         * 
         * @return a String that represents the state of this Board object.
         */
        get() {
            val data: MutableList<Byte> =
                ArrayList<Byte>()
            // 0. data structure version
            data.add(0.toByte())
            // 1. board size
            putInteger(this.width, data)
            putInteger(this.height, data)
            // 2. robots
            data.add(this.robotPositions.size.toByte())
            for (robot in this.robotPositions) {
                putInteger(robot, data)
            }
            // 3. quadrants
            for (quadrant in this.quadrants) {
                data.add(quadrant.toByte())
            }
            // 4. walls
            for (dir in this.walls.indices) {
                for (pos in this.walls[dir].indices) {
                    data.add(if (this.walls[dir][pos]) 1.toByte() else 0.toByte())
                }
            }
            // 5. list of goals
            putInteger(this.goals.size, data)
            for (goal in this.goals) {  //6 bytes
                putInteger(goal.position, data)
                data.add(goal.robotNumber.toByte())
                data.add(goal.shape.toByte())
            }
            // 6. active goal
            putInteger((if (null == this.goal) -1 else this.goal!!.position), data)
            data.add((if (null == this.goal) 0 else this.goal!!.robotNumber).toByte())
            data.add((if (null == this.goal) 0 else this.goal!!.shape).toByte())
            // 7. isFreestyleBoard
            data.add(if (this.isFreestyleBoard) 1.toByte() else 0.toByte())
            //convert data to String
            val dataArray = ByteArray(data.size)
            var i = 0
            for (dat in data) {
                dataArray[i++] = dat
            }
            val str: String = zipb64(dataArray)
            return str
        }


    /**
     * Rotates the board 90 degrees.
     * @param clockwise If true, rotates clockwise; if false, counterclockwise
     * @return New board instance with rotated configuration
     */
    fun rotate90(clockwise: Boolean): Board {
        val newBoard = Board(this.height, this.width, this.robotPositions.size)
        //quadrants
        for (q in 0..3) {
            newBoard.quadrants[(q + (if (clockwise) 1 else -1)) and 3] = this.quadrants[q]
        }
        //walls
        for (d in 0..3) {
            for (pos in 0..<this.size) {
                newBoard.walls[(d + (if (clockwise) 1 else -1)) and 3][this.rotatePosition90(
                    pos,
                    clockwise
                )] = this.walls[d][pos]
            }
        }
        //robots
        for (i in this.robotPositions.indices) {
            newBoard.robotPositions[i] = this.rotatePosition90(this.robotPositions[i], clockwise)
        }
        //goals
        for (g in this.goals) {
            newBoard.addGoal(this.rotatePosition90(g.position, clockwise), g.robotNumber, g.shape)
        }
        //goal
        newBoard.setGoal(this.rotatePosition90(this.goal!!.position, clockwise))
        return newBoard
    }


    private fun rotatePosition90(pos: Int, clockwise: Boolean): Int {
        val x = pos % this.width
        val y = pos / this.width
        val newx: Int
        val newy: Int
        if (clockwise) {
            newx = this.height - 1 - y
            newy = x
        } else {
            newx = y
            newy = this.width - 1 - x
        }
        return newx + newy * this.height
    }


    private fun transformQuadrantX(qX: Int, qY: Int, qPos: Int): Int {
        //qPos (quadrant target position): 0==NW, 1==NE, 2==SE, 3==SW
        val resultX: Int
        when (qPos) {
            1 -> resultX = this.width - 1 - qY
            2 -> resultX = this.width - 1 - qX
            3 -> resultX = qY
            else -> resultX = qX
        }
        return resultX
    }

    private fun transformQuadrantY(qX: Int, qY: Int, qPos: Int): Int {
        //qPos (quadrant target position): 0==NW, 1==NE, 2==SE, 3==SW
        val resultY: Int
        when (qPos) {
            1 -> resultY = qX
            2 -> resultY = this.height - 1 - qY
            3 -> resultY = this.height - 1 - qX
            else -> resultY = qY
        }
        return resultY
    }

    private fun transformQuadrantPosition(qX: Int, qY: Int, qPos: Int): Int {
        return (this.transformQuadrantX(qX, qY, qPos) + this.transformQuadrantY(
            qX,
            qY,
            qPos
        ) * this.width)
    }


    private fun addQuadrant(qNum: Int, qPos: Int): Board {
        this.quadrants[qPos] = qNum
        val quadrant: Board = QUADRANTS[qNum]
        //qPos (quadrant target position): 0==NW, 1==NE, 2==SE, 3==SW
        var qX: Int
        var qY: Int
        //add walls
        qY = 0
        while (qY < quadrant.height / 2) {
            qX = 0
            while (qX < quadrant.width / 2) {
                for (dir in 0..3) {
                    this.walls[(dir + qPos) and 3][this.transformQuadrantPosition(qX, qY, qPos)] =
                        this.walls[(dir + qPos) and 3][this.transformQuadrantPosition(
                            qX,
                            qY,
                            qPos
                        )] or
                                quadrant.walls[dir][qX + qY * quadrant.width]
                }
                ++qX
            }
            this.walls[(WEST + qPos) and 3][this.transformQuadrantPosition(qX, qY, qPos)] =
                this.walls[(WEST + qPos) and 3][this.transformQuadrantPosition(qX, qY, qPos)] or
                        quadrant.walls[EAST][qX - 1 + qY * quadrant.width]
            ++qY
        }
        qX = 0
        while (qX < quadrant.width / 2) {
            this.walls[(NORTH + qPos) and 3][this.transformQuadrantPosition(qX, qY, qPos)] =
                this.walls[(NORTH + qPos) and 3][this.transformQuadrantPosition(qX, qY, qPos)] or
                        quadrant.walls[SOUTH][qX + (qY - 1) * quadrant.width]
            ++qX
        }
        //add goals
        for (g in quadrant.goals) {
            this.addGoal(
                this.transformQuadrantX(g.x, g.y, qPos),
                this.transformQuadrantY(g.x, g.y, qPos),
                g.robotNumber,
                g.shape
            )
        }
        return this
    }


    val isSolution01: Boolean
        /**
         * Checks if current board configuration is a solution of 0 or 1 move.
         * @return true if solution, false otherwise
         */
        get() {
            for (robo in this.robotPositions.indices) {
                if ((this.goal!!.robotNumber != robo) && (this.goal!!.robotNumber != -1)) {
                    continue  // skip because it's not the goal robot
                }
                val oldRoboPos = this.robotPositions[robo]
                if (this.goal!!.position == oldRoboPos) {
                    return true // already on goal
                }
                var dir = -1
                for (dirIncr in this.directionIncrement) {
                    ++dir
                    var newRoboPos = oldRoboPos
                    var prevRoboPos = oldRoboPos
                    // move the robot until it reaches a wall or another robot.
                    // NOTE: we rely on the fact that all boards are surrounded
                    // by outer walls. without the outer walls we would need
                    // some additional boundary checking here.
                    while (true) {
                        if (true == this.walls[dir][newRoboPos]) { // stopped by wall
                            if (this.goal!!.position == newRoboPos) {
                                return true // one move to goal
                            }
                            break // can't go on
                        }
                        if (true == this.isRobotPos(newRoboPos)) { // stopped by robot
                            if (this.goal!!.position == prevRoboPos) {
                                return true // one move to goal
                            }
                            // go on in this direction
                        }
                        prevRoboPos = newRoboPos
                        newRoboPos += dirIncr
                    }
                }
            }
            return false
        }

    private fun isRobotPos(position: Int): Boolean {
        for (roboPos in this.robotPositions) {
            if (position == roboPos) {
                return true
            }
        }
        return false
    }

    /**
     * Places robots on the board in default positions.
     * @param numRobots Number of robots to place
     */
    fun setRobots(numRobots: Int) {
        this.robotPositions = IntArray(numRobots)
        if (this.isFreestyleBoard) {
            this.setRobotsRandom()
        } else {
            //original board / made out of quadrants
            this.setRobot(0, 14 + 2 * this.width, false) //R
            this.setRobot(1, 1 + 2 * this.width, false) //G
            this.setRobot(2, 13 + 11 * this.width, false) //B
            this.setRobot(3, 15 + 0 * this.width, false) //Y
            this.setRobot(4, 15 + 7 * this.width, false) //S
        }
    }

    /**
     * Places robots randomly on valid board positions.
     * Ensures initial position is not an immediate solution.
     */
    fun setRobotsRandom() {
        do {
            Arrays.fill(this.robotPositions, -1)
            for (i in this.robotPositions.indices) {
                var position: Int
                do {
                    position = RANDOM.nextInt(this.size)
                } while (false == this.setRobot(i, position, false))
            }
        } while (true == this.isSolution01)
    }

    /**
     * Sets robot positions from an array.
     * @param newRobots Array of robot positions
     * @return true if all positions were valid, false otherwise
     */
    fun setRobots(newRobots: IntArray): Boolean {
        if (this.robotPositions.size != newRobots.size) {
            return false
        }
        val backup = this.robotPositions.copyOf(this.robotPositions.size)
        Arrays.fill(this.robotPositions, -1)
        for (i in newRobots.indices) {
            if (!this.setRobot(i, newRobots[i], false)) {    //failed to set a robot
                //undo all changes
                System.arraycopy(backup, 0, this.robotPositions, 0, backup.size)
                return false
            }
        }
        return true
    }

    /**
     * Places a single robot at specified position.
     * @param robot Robot index to place
     * @param position Board position to place robot
     * @param allowSwapRobots If true, allows swapping with other robots
     * @return true if placement successful, false otherwise
     */
    fun setRobot(robot: Int, position: Int, allowSwapRobots: Boolean): Boolean {
        //invalid robot number?
        //impossible position (out of bounds or obstacle)?
        if ((robot < 0) || (robot >= this.robotPositions.size) ||
            (position < 0) || (position >= this.size) ||
            this.isObstacle(position) ||
            ((false == allowSwapRobots) && (this.getRobotNum(position) >= 0) && (this.getRobotNum(
                position
            ) != robot))
        ) {
            return false
        } else {
            //position already occupied by another robot?
            val otherRobot = this.getRobotNum(position)
            val oldPosition = this.robotPositions[robot]
            if ((otherRobot >= 0) && (otherRobot != robot) && (oldPosition >= 0)) {
                this.robotPositions[otherRobot] = oldPosition
            }
            //set this robot's position
            this.robotPositions[robot] = position
            return true
        }
    }

    /**
     * Sets a random goal from available goals.
     * Avoids goals that would make current position a solution.
     */
    fun setGoalRandom() {
        if (this.goals.isEmpty()) {
            this.goal = null
            return
        }
        if (this.randomGoals.isEmpty()) {
            this.randomGoals.addAll(this.goals)
            Collections.shuffle(this.randomGoals, RANDOM)
        }
        this.goal = this.randomGoals.removeAt(0)
        if (this.goal!!.robotNumber >= this.robotPositions.size) {  //goal not usable
            this.setGoalRandom() //recursion
        }
        if (this.isSolution01 && (this.randomGoals.size > 0)) {
            //the resulting board configuration has a solution of 0 or 1 move
            //and there are some other goals available in list randomGoals.
            val goal01 = this.goal
            this.setGoalRandom() //recursion
            this.randomGoals.add(goal01)
        }
    }

    /**
     * Sets active goal at specified position.
     * @param position Position of goal to activate
     * @return true if valid goal exists at position, false otherwise
     */
    fun setGoal(position: Int): Boolean {
        var result = false
        for (g in this.goals) {
            if ((g.position == position) && (g.robotNumber < this.robotPositions.size)) {
                this.goal = g
                result = true
                break
            }
        }
        return result
    }

    /**
     * Adds a new goal to the board.
     * @param pos Position for goal
     * @param robot Robot index for goal (-1 for wildcard)
     * @param shape Shape index for goal
     * @return This board instance for chaining
     */
    fun addGoal(pos: Int, robot: Int, shape: Int): Board {
        this.removeGoal(pos)
        return this.addGoal(pos % this.width, pos / this.width, robot, shape)
    }

    private fun addGoal(x: Int, y: Int, robot: Int, shape: Int): Board {
        val g = Goal(x, y, robot, shape)
        this.goals.add(g)
        if (null == this.goal) {
            this.goal = g
        }
        return this
    }

    /**
     * Removes goal at specified position.
     * @param position Position of goal to remove
     * @return true if goal was removed, false otherwise
     */
    fun removeGoal(position: Int): Boolean {
        var result = false
        val iter = this.goals.iterator()
        while (iter.hasNext()) {
            val g = iter.next()
            if (g.position == position) {
                iter.remove()
                this.randomGoals.remove(g)
                result = true
                if (g == this.goal) {
                    this.setGoalRandom()
                }
            }
        }
        return result
    }

    /**
     * Removes all goals from the board.
     */
    fun removeGoals() {
        this.goals.clear()
        this.goal = null
        this.randomGoals.clear()
    }

    private fun addOuterWalls(): Board {
        return this.setOuterWalls(true)
    }

    private fun removeOuterWalls(): Board {
        return this.setOuterWalls(false)
    }

    private fun setOuterWalls(value: Boolean): Board {
        for (x in 0..<this.width) {
            this.setWall(x, 0, NORTH, value)
            this.setWall(x, this.height - 1, SOUTH, value)
        }
        for (y in 0..<this.height) {
            this.setWall(0, y, WEST, value)
            this.setWall(this.width - 1, y, EAST, value)
        }
        return this
    }

    private fun addWall(x: Int, y: Int, str: String): Board {
        this.setWalls(x, y, str, true)
        return this
    }

    private fun setWalls(x: Int, y: Int, str: String, value: Boolean) {
        if (str.contains("N")) {
            this.setWall(x, y, NORTH, value)
            this.setWall(x, y - 1, SOUTH, value)
        } else if (str.contains("E")) {
            this.setWall(x, y, EAST, value)
            this.setWall(x + 1, y, WEST, value)
        } else if (str.contains("W")) {
            this.setWall(x, y, WEST, value)
            this.setWall(x - 1, y, EAST, value)
        } else if (str.contains("S")) { // S could also be in WEST or EAST
            this.setWall(x, y, SOUTH, value)
            this.setWall(x, y + 1, NORTH, value)
        }
    }

    fun setWall(x: Int, y: Int, direction: Int, value: Boolean) {
        if ((x >= 0) && (x < this.width) && (y >= 0) && (y < this.height)) {
            this.walls[direction][x + y * this.width] = value
        }
    }

    fun setWall(position: Int, direction: String, doSet: Boolean) {
        var direction = direction
        val x = position % this.width
        val y = position / this.width
        if (false == doSet) {
            //prevent removal of outer walls
            if (0 == x) {
                direction = direction.replace('W', ' ')
            }
            if (this.width - 1 == x) {
                direction = direction.replace('E', ' ')
            }
            if (0 == y) {
                direction = direction.replace('N', ' ')
            }
            if (this.height - 1 == y) {
                direction = direction.replace('S', ' ')
            }
        }
        this.setWalls(x, y, direction, doSet)
    }

    fun removeWalls() {
        for (w in this.walls) {
            Arrays.fill(w, false)
        }
        this.addOuterWalls()
    }

    fun isWall(position: Int, direction: Int): Boolean {
        return this.walls[direction][position]
    }

    fun isObstacle(position: Int): Boolean {
        return (this.isWall(position, NORTH) &&
                this.isWall(position, EAST) &&
                this.isWall(position, SOUTH) &&
                this.isWall(position, WEST))
    }

    private fun getRobotNum(position: Int): Int {
        var robotNum = -1 //default: not found
        for (i in this.robotPositions.indices) {
            if (this.robotPositions[i] == position) {
                robotNum = i
                break
            }
        }
        return robotNum
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        val s = StringBuilder()

        //s.append("Board (").append(this.width).append(",").append(this.height)
        //        .append(",").append(this.robots.length).append(")").append('\n');

        // print board graphically
        // horizontal wall = "---", vertical wall = "|",
        // empty cell = ".", robots = "01234", goal = "X"
        var position = 0
        for (y in 0..<this.height) {
            val sWC = StringBuilder() // west walls and cells
            var x = 0
            while (x < this.width) {
                s.append(if (this.isWall(position, NORTH)) " ---" else "    ")
                sWC.append(if (this.isWall(position, WEST)) "| " else "  ")
                val robotNum: Int
                if (isObstacle(position)) {
                    sWC.append('#')
                } else if ((getRobotNum(position).also { robotNum = it }) >= 0) {
                    sWC.append(robotNum)
                } else if (position == this.goal!!.position) {
                    sWC.append('X')
                } else {
                    sWC.append('.')
                }
                sWC.append(' ')
                ++x
                ++position
            }
            s.append(' ').append('\n')
            sWC.append(if (this.isWall(position - 1, EAST)) '|' else ' ')
            s.append(sWC).append('\n')
        }
        var x = 0
        while (x < this.width) {
            s.append(if (this.isWall(position - this.width, SOUTH)) " ---" else "    ")
            ++x
            ++position
        }
        s.append(' ').append('\n')
        //        // print list of wall coordinates and directions
//        s.append("walls:").append('\n');
//        position = 0;
//        for (int y = 0; y < this.height; ++y) {
//            for (int x = 0; x < this.width; ++x, ++position) {
//                String t = "";
//                if (this.isWall(position, NORTH)) t += "N";
//                if (this.isWall(position, EAST )) t += "E";
//                if (this.isWall(position, SOUTH)) t += "S";
//                if (this.isWall(position, WEST )) t += "W";
//                if (! t.equals("")) {
//                    s.append("(").append(x).append(",").append(y).append(") ")
//                            .append(t).append('\n');
//                }
//            }
//        }
        // print list of robot coordinates
        for (i in this.robotPositions.indices) {
            s.append("robot #").append(i)
                .append(" (").append(this.robotPositions[i] % this.width)
                .append(", ").append(this.robotPositions[i] / this.width)
                .append(")").append('\n')
        }
        // print goal coordinates
        s.append("goal #").append(this.goal!!.robotNumber)
            .append(" (").append(this.goal!!.position % this.width)
            .append(", ").append(this.goal!!.position / this.width).append(")")
        return s.toString()
    }

    /**
     * Gets current active goal.
     * @return Active goal object
     */
    fun getGoal(): Goal {
        return this.goal!!
    }

    fun getActiveGoals(): MutableList<Goal?> {
        if (this.activeGoals != null && !this.activeGoals!!.isEmpty()) {
            return this.activeGoals!!
        }
        val single: MutableList<Goal?> = ArrayList<Goal?>()
        if (this.goal != null) {
            single.add(this.goal)
        }
        return single
    }

    fun setActiveGoals(goals: MutableList<Goal>) {
        this.activeGoals = ArrayList<Goal?>(goals)
        if (!goals.isEmpty()) {
            this.goal = goals.get(0)
        }
    }

    /**
     * Gets goal at specified position.
     * @param position Position to check
     * @return Goal at position or null if none exists
     */
    fun getGoalAt(position: Int): Goal? {
        var result: Goal? = null
        for (g in this.goals) {
            if (g.position == position) {
                result = g
                break
            }
        }
        return result
    }

    /**
     * Gets quadrant number at specified position.
     * @param qPos Quadrant position (0=NW, 1=NE, 2=SE, 3=SW)
     * @return Quadrant number (0-15)
     */
    fun getQuadrantNum(qPos: Int): Int { //qPos: 0=NW, 1=NE, 2=SE, 3=SW
        return this.quadrants[qPos]
    }

    /**
     * determine the direction from position "old" to "new".
     * @param diffPos difference of positions (new - old)
     * @return direction (Board.NORTH, Board.EAST, Board.SOUTH or Board.WEST)
     */
    fun getDirection(diffPos: Int): Int {
        val dir: Int
        if (diffPos < 0) {
            if (-diffPos < this.width) {
                dir = WEST
            } else {
                dir = NORTH
            }
        } else {
            if (diffPos < this.width) {
                dir = EAST
            } else {
                dir = SOUTH
            }
        }
        return dir
    }

    val numRobots: Int
        get() = this.robotPositions.size

    /**
     * Marks board as freestyle type.
     */
    fun setFreestyleBoard() {
        this.isFreestyleBoard = true
    }

    companion object {
        var L10N: L10N = L10N()
        val WIDTH_STANDARD: Int = MainActivity.boardSizeX
        const val WIDTH_MIN: Int = 3
        const val WIDTH_MAX: Int = 100
        val HEIGHT_STANDARD: Int = MainActivity.boardSizeY
        const val HEIGHT_MIN: Int = 3
        const val HEIGHT_MAX: Int = 100
        const val SIZE_MAX: Int = 4096 // 12 bits
        const val NUMROBOTS_STANDARD: Int = 4

        val ROBOT_COLOR_NAMES_SHORT: Array<String?> =
            arrayOf<String?>( //also used as part of L10N-keys
                "r", "g", "b", "y", "s"
            )
        val ROBOT_COLOR_NAMES_LONG: Array<String?> =
            arrayOf<String?>( //also used as part of L10N-keys
                "red", "green", "blue", "yellow", "silver"
            )

        const val GOAL_CIRCLE: Int = 0
        const val GOAL_TRIANGLE: Int = 1
        const val GOAL_SQUARE: Int = 2
        const val GOAL_HEXAGON: Int = 3
        const val GOAL_VORTEX: Int = 4
        val GOAL_SHAPE_NAMES: Array<String?> = arrayOf<String?>(
            "circle", "triangle", "square", "hexagon", "vortex"
        )

        val QUADRANT_NAMES: Array<String?> = arrayOf<String?>(
            "1A", "2A", "3A", "4A",
            "1B", "2B", "3B", "4B",
            "1C", "2C", "3C", "4C",
            "1D", "2D", "3D", "4D"
        )
        private val QUADRANTS: Array<Board> = arrayOfNulls<Board>(16)

        init {
            QUADRANTS[0] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1A
                .addWall(1, 0, "E")
                .addWall(4, 1, "NW").addGoal(4, 1, 0, GOAL_CIRCLE) //R
                .addWall(1, 2, "NE").addGoal(1, 2, 1, GOAL_TRIANGLE) //G
                .addWall(6, 3, "SE").addGoal(6, 3, 3, GOAL_HEXAGON) //Y
                .addWall(0, 5, "S")
                .addWall(3, 6, "SW").addGoal(3, 6, 2, GOAL_SQUARE) //B
                .addWall(7, 7, "NESW")
            QUADRANTS[1] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2A
                .addWall(3, 0, "E")
                .addWall(5, 1, "SE").addGoal(5, 1, 1, GOAL_HEXAGON) //G
                .addWall(1, 2, "SW").addGoal(1, 2, 0, GOAL_SQUARE) //R
                .addWall(0, 3, "S")
                .addWall(6, 4, "NW").addGoal(6, 4, 3, GOAL_CIRCLE) //Y
                .addWall(2, 6, "NE").addGoal(2, 6, 2, GOAL_TRIANGLE) //B
                .addWall(7, 7, "NESW")
            QUADRANTS[2] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3A
                .addWall(3, 0, "E")
                .addWall(5, 2, "SE").addGoal(5, 2, 2, GOAL_HEXAGON) //B
                .addWall(0, 4, "S")
                .addWall(2, 4, "NE").addGoal(2, 4, 1, GOAL_CIRCLE) //G
                .addWall(7, 5, "SW").addGoal(7, 5, 0, GOAL_TRIANGLE) //R
                .addWall(1, 6, "NW").addGoal(1, 6, 3, GOAL_SQUARE) //Y
                .addWall(7, 7, "NESW")
            QUADRANTS[3] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4A
                .addWall(3, 0, "E")
                .addWall(6, 1, "SW").addGoal(6, 1, 2, GOAL_CIRCLE) //B
                .addWall(1, 3, "NE").addGoal(1, 3, 3, GOAL_TRIANGLE) //Y
                .addWall(5, 4, "NW").addGoal(5, 4, 1, GOAL_SQUARE) //G
                .addWall(2, 5, "SE").addGoal(2, 5, 0, GOAL_HEXAGON) //R
                .addWall(7, 5, "SE").addGoal(7, 5, -1, GOAL_VORTEX) //W*
                .addWall(0, 6, "S")
                .addWall(7, 7, "NESW")
            QUADRANTS[4] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1B
                .addWall(4, 0, "E")
                .addWall(6, 1, "SE").addGoal(6, 1, 3, GOAL_HEXAGON) //Y
                .addWall(1, 2, "NW").addGoal(1, 2, 1, GOAL_TRIANGLE) //G
                .addWall(0, 5, "S")
                .addWall(6, 5, "NE").addGoal(6, 5, 2, GOAL_SQUARE) //B
                .addWall(3, 6, "SW").addGoal(3, 6, 0, GOAL_CIRCLE) //R
                .addWall(7, 7, "NESW")
            QUADRANTS[5] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2B
                .addWall(4, 0, "E")
                .addWall(2, 1, "NW").addGoal(2, 1, 3, GOAL_CIRCLE) //Y
                .addWall(6, 3, "SW").addGoal(6, 3, 2, GOAL_TRIANGLE) //B
                .addWall(0, 4, "S")
                .addWall(4, 5, "NE").addGoal(4, 5, 0, GOAL_SQUARE) //R
                .addWall(1, 6, "SE").addGoal(1, 6, 1, GOAL_HEXAGON) //G
                .addWall(7, 7, "NESW")
            QUADRANTS[6] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3B
                .addWall(3, 0, "E")
                .addWall(1, 1, "SW").addGoal(1, 1, 0, GOAL_TRIANGLE) //R
                .addWall(6, 2, "NE").addGoal(6, 2, 1, GOAL_CIRCLE) //G
                .addWall(2, 4, "SE").addGoal(2, 4, 2, GOAL_HEXAGON) //B
                .addWall(0, 5, "S")
                .addWall(7, 5, "NW").addGoal(7, 5, 3, GOAL_SQUARE) //Y
                .addWall(7, 7, "NESW")
            QUADRANTS[7] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4B
                .addWall(4, 0, "E")
                .addWall(2, 1, "SE").addGoal(2, 1, 0, GOAL_HEXAGON) //R
                .addWall(1, 3, "SW").addGoal(1, 3, 1, GOAL_SQUARE) //G
                .addWall(0, 4, "S")
                .addWall(6, 4, "NW").addGoal(6, 4, 3, GOAL_TRIANGLE) //Y
                .addWall(5, 6, "NE").addGoal(5, 6, 2, GOAL_CIRCLE) //B
                .addWall(3, 7, "SE").addGoal(3, 7, -1, GOAL_VORTEX) //W*
                .addWall(7, 7, "NESW")
            QUADRANTS[8] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1C
                .addWall(1, 0, "E")
                .addWall(3, 1, "NW").addGoal(3, 1, 1, GOAL_TRIANGLE) //G
                .addWall(6, 3, "SE").addGoal(6, 3, 3, GOAL_HEXAGON) //Y
                .addWall(1, 4, "SW").addGoal(1, 4, 0, GOAL_CIRCLE) //R
                .addWall(0, 6, "S")
                .addWall(4, 6, "NE").addGoal(4, 6, 2, GOAL_SQUARE) //B
                .addWall(7, 7, "NESW")
            QUADRANTS[9] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2C
                .addWall(5, 0, "E")
                .addWall(3, 2, "NW").addGoal(3, 2, 3, GOAL_CIRCLE) //Y
                .addWall(0, 3, "S")
                .addWall(5, 3, "SW").addGoal(5, 3, 2, GOAL_TRIANGLE) //B
                .addWall(2, 4, "NE").addGoal(2, 4, 0, GOAL_SQUARE) //R
                .addWall(4, 5, "SE").addGoal(4, 5, 1, GOAL_HEXAGON) //G
                .addWall(7, 7, "NESW")
            QUADRANTS[10] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3C
                .addWall(1, 0, "E")
                .addWall(4, 1, "NE").addGoal(4, 1, 1, GOAL_CIRCLE) //G
                .addWall(1, 3, "SW").addGoal(1, 3, 0, GOAL_TRIANGLE) //R
                .addWall(0, 5, "S")
                .addWall(5, 5, "NW").addGoal(5, 5, 3, GOAL_SQUARE) //Y
                .addWall(3, 6, "SE").addGoal(3, 6, 2, GOAL_HEXAGON) //B
                .addWall(7, 7, "NESW")
            QUADRANTS[11] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4C
                .addWall(2, 0, "E")
                .addWall(5, 1, "SW").addGoal(5, 1, 2, GOAL_CIRCLE) //B
                .addWall(7, 2, "SE").addGoal(7, 2, -1, GOAL_VORTEX) //W*
                .addWall(0, 3, "S")
                .addWall(3, 4, "SE").addGoal(3, 4, 0, GOAL_HEXAGON) //R
                .addWall(6, 5, "NW").addGoal(6, 5, 1, GOAL_SQUARE) //G
                .addWall(1, 6, "NE").addGoal(1, 6, 3, GOAL_TRIANGLE) //Y
                .addWall(7, 7, "NESW")
            QUADRANTS[12] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1D
                .addWall(5, 0, "E")
                .addWall(1, 3, "NW").addGoal(1, 3, 0, GOAL_CIRCLE) //R
                .addWall(6, 4, "SE").addGoal(6, 4, 3, GOAL_HEXAGON) //Y
                .addWall(0, 5, "S")
                .addWall(2, 6, "NE").addGoal(2, 6, 1, GOAL_TRIANGLE) //G
                .addWall(3, 6, "SW").addGoal(3, 6, 2, GOAL_SQUARE) //B
                .addWall(7, 7, "NESW")
            QUADRANTS[13] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2D
                .addWall(2, 0, "E")
                .addWall(5, 2, "SE").addGoal(5, 2, 1, GOAL_HEXAGON) //G
                .addWall(6, 2, "NW").addGoal(6, 2, 3, GOAL_CIRCLE) //Y
                .addWall(1, 5, "SW").addGoal(1, 5, 0, GOAL_SQUARE) //R
                .addWall(0, 6, "S")
                .addWall(4, 7, "NE").addGoal(4, 7, 2, GOAL_TRIANGLE) //B
                .addWall(7, 7, "NESW")
            QUADRANTS[14] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3D
                .addWall(4, 0, "E")
                .addWall(0, 2, "S")
                .addWall(6, 2, "SE").addGoal(6, 2, 2, GOAL_HEXAGON) //B
                .addWall(2, 4, "NE").addGoal(2, 4, 1, GOAL_CIRCLE) //G
                .addWall(3, 4, "SW").addGoal(3, 4, 0, GOAL_TRIANGLE) //R
                .addWall(5, 6, "NW").addGoal(5, 6, 3, GOAL_SQUARE) //Y
                .addWall(7, 7, "NESW")
            QUADRANTS[15] = Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4D
                .addWall(4, 0, "E")
                .addWall(6, 2, "NW").addGoal(6, 2, 3, GOAL_TRIANGLE) //Y
                .addWall(2, 3, "NE").addGoal(2, 3, 2, GOAL_CIRCLE) //B
                .addWall(3, 3, "SW").addGoal(3, 3, 1, GOAL_SQUARE) //G
                .addWall(1, 5, "SE").addGoal(1, 5, 0, GOAL_HEXAGON) //R
                .addWall(0, 6, "S")
                .addWall(5, 7, "SE").addGoal(5, 7, -1, GOAL_VORTEX) //W*
                .addWall(7, 7, "NESW")
        }

        val NORTH: Int = Constants.NORTH // up
        val EAST: Int = Constants.EAST // right
        val SOUTH: Int = Constants.SOUTH // down
        val WEST: Int = Constants.WEST // left

        private val RANDOM = Random()

        /**
         * Gets a list of all goals in a specified quadrant.
         * @param quadrant Index of the quadrant to get goals from
         * @return List of goals in the quadrant, sorted by robot number, shape, and position
         */
        fun getStaticQuadrantGoals(quadrant: Int): MutableList<Goal?> {
            val result: MutableList<Goal?> = ArrayList<Goal?>(QUADRANTS[quadrant].goals)
            Collections.sort<Goal?>(result)
            return result
        }


        /**
         * Creates an exact copy of an existing board.
         * Copies all board properties including dimensions, robots, walls, goals, and state.
         * @param oldBoard Board to clone
         * @return New board instance that is an exact copy
         */
        fun createClone(oldBoard: Board): Board {
            // 1. board size, numRobots
            val newBoard = Board(oldBoard.width, oldBoard.height, oldBoard.robotPositions.size)
            // 2. robots
            System.arraycopy(
                oldBoard.robotPositions,
                0,
                newBoard.robotPositions,
                0,
                newBoard.robotPositions.size
            )
            // 3. quadrants
            System.arraycopy(oldBoard.quadrants, 0, newBoard.quadrants, 0, newBoard.quadrants.size)
            // 4. walls
            for (i in oldBoard.walls.indices) {
                System.arraycopy(oldBoard.walls[i], 0, newBoard.walls[i], 0, newBoard.walls[i].size)
            }
            // 5. list of goals
            newBoard.goals.clear()
            newBoard.goals.addAll(oldBoard.goals)
            // 6. active goal
            newBoard.goal = oldBoard.goal
            // 7. isFreestyleBoard
            newBoard.isFreestyleBoard = oldBoard.isFreestyleBoard
            return newBoard
        }


        /**
         * Creates a freestyle board with custom dimensions.
         * Can optionally copy properties from an existing board.
         * @param oldBoard Optional board to copy properties from
         * @param width Width of new board
         * @param height Height of new board
         * @param numRobots Number of robots on new board
         * @return New freestyle board instance
         */
        fun createBoardFreestyle(
            oldBoard: Board?,
            width: Int,
            height: Int,
            numRobots: Int
        ): Board? {
            if ((width < WIDTH_MIN) || (height < HEIGHT_MIN) || (width * height > SIZE_MAX)) {
                Logger.println("error in createBoardFreestyle(): invalid parameter: width=" + width + " height=" + height + " size=" + width * height)
                return oldBoard
            }
            val newBoard = Board(width, height, numRobots)
            newBoard.setFreestyleBoard()
            // Reset robot positions to -1 so they can be set correctly later
            // The constructor sets default positions, but for freestyle boards
            // the robots will be positioned by the caller (e.g., RRGetMap.createDDWorld)
            Arrays.fill(newBoard.robotPositions, -1)
            if (null != oldBoard) {
                // copy walls, goals and active goal
                oldBoard.removeOuterWalls()
                newBoard.goal = null
                for (y in 0..<min(newBoard.height, oldBoard.height)) {
                    var newPos = y * newBoard.width
                    var oldPos = y * oldBoard.width
                    var x = 0
                    while (x < min(newBoard.width, oldBoard.width)) {
                        for (d in newBoard.walls.indices) {
                            newBoard.walls[d][newPos] = oldBoard.walls[d][oldPos]
                        }
                        val oldGoal = oldBoard.getGoalAt(oldPos)
                        if (null != oldGoal) {
                            newBoard.addGoal(newPos, oldGoal.robotNumber, oldGoal.shape)
                            if (oldGoal == oldBoard.getGoal()) {
                                newBoard.setGoal(newPos)
                            }
                        }
                        ++x
                        ++newPos
                        ++oldPos
                    }
                }
                if (null == newBoard.goal) {
                    newBoard.setGoalRandom()
                }
                oldBoard.addOuterWalls()
                // copy robots
                Arrays.fill(newBoard.robotPositions, -1)
                for (robot in 0..<min(newBoard.robotPositions.size, oldBoard.robotPositions.size)) {
                    val oldX = oldBoard.robotPositions[robot] % oldBoard.width
                    val oldY = oldBoard.robotPositions[robot] / oldBoard.width
                    val newPos = oldX + oldY * newBoard.width
                    newBoard.setRobot(robot, newPos, false)
                }
                // copy of some robot didn't succeed, set it on lowest possible position
                for (robot in newBoard.robotPositions.indices) {
                    if (0 > newBoard.robotPositions[robot]) {
                        for (pos in 0..<newBoard.size) {
                            if (true == newBoard.setRobot(robot, pos, false)) {
                                break // setRobot succeeded
                            }
                        }
                    }
                }
            }
            newBoard.addOuterWalls()
            return newBoard
        }


        /**
         * Creates a standard board by combining four quadrants.
         * @param quadrantNW Northwest quadrant index
         * @param quadrantNE Northeast quadrant index
         * @param quadrantSE Southeast quadrant index
         * @param quadrantSW Southwest quadrant index
         * @param numRobots Number of robots to place
         * @return New board composed of specified quadrants
         */
        fun createBoardQuadrants(
            quadrantNW: Int,
            quadrantNE: Int,
            quadrantSE: Int,
            quadrantSW: Int,
            numRobots: Int
        ): Board {
            val b = Board(WIDTH_STANDARD, HEIGHT_STANDARD, numRobots)
            //add walls and goals
            b.addQuadrant(quadrantNW, 0)
            b.addQuadrant(quadrantNE, 1)
            b.addQuadrant(quadrantSE, 2)
            b.addQuadrant(quadrantSW, 3)
            b.addOuterWalls()
            //place the robots ->  done by constructor / setRobots(num)
            //choose a goal
            b.setGoalRandom()
            return b
        }


        /**
         * Creates a board with randomly selected quadrants.
         * @param numRobots Number of robots to place
         * @return New board with random quadrant configuration
         */
        fun createBoardRandom(numRobots: Int): Board {
            val indexList = ArrayList<Int?>()
            for (i in 0..3) {
                indexList.add(i)
            }
            Collections.shuffle(indexList, RANDOM)
            return createBoardQuadrants(
                indexList.get(0)!! + RANDOM.nextInt(3 + 1) * 4,
                indexList.get(1)!! + RANDOM.nextInt(3 + 1) * 4,
                indexList.get(2)!! + RANDOM.nextInt(3 + 1) * 4,
                indexList.get(3)!! + RANDOM.nextInt(3 + 1) * 4,
                numRobots
            )
        }


        /**
         * Creates a board from a game ID string.
         * Game ID encodes quadrants, robots, and goal information.
         * @param idStr Game ID string to decode
         * @return New board based on game ID, or null if invalid
         */
        fun createBoardGameID(idStr: String): Board? {
            var result: Board? = null
            var index = 0
            try {
                //example game ID: 0765+41+2E21BD0F+1C
                val q0 = idStr.get(index++).toString().toInt(16)
                val q1 = idStr.get(index++).toString().toInt(16)
                val q2 = idStr.get(index++).toString().toInt(16)
                val q3 = idStr.get(index++).toString().toInt(16)
                val qMax: Int = QUADRANTS.size - 1
                require(!((q0 > qMax) || (q1 > qMax) || (q2 > qMax) || (q3 > qMax))) { "quadrant numbers out of range" }
                require(idStr.get(index++) == '+') { "missing '+' at index=" + (index - 1) }
                val numRobots = idStr.get(index++).toString().toInt(16)
                var goalRobot = idStr.get(index++).toString().toInt(16)
                if (goalRobot == 0x0f) {
                    goalRobot = -1
                }
                require(!((numRobots > ROBOT_COLOR_NAMES_SHORT.size) || (goalRobot >= numRobots))) { "robot numbers out of range" }
                require(idStr.get(index++) == '+') { "missing '+' at index=" + (index - 1) }
                val robotPositions = IntArray(numRobots)
                for (i in 0..<numRobots) {
                    var str = idStr.get(index++).toString()
                    str += idStr.get(index++).toString()
                    robotPositions[i] = str.toInt(16)
                }
                require(idStr.get(index++) == '+') { "missing '+' at index=" + (index - 1) }
                var str = idStr.get(index++).toString()
                str += idStr.get(index).toString()
                val goalPosition = str.toInt(16)
                result = createBoardQuadrants(q0, q1, q2, q3, numRobots)
                val successRobots = result.setRobots(robotPositions)
                val successGoal = result.setGoal(goalPosition)
                require(!(!successRobots || !successGoal || (result.goal!!.robotNumber != goalRobot))) { "robots or goal position are not valid" }
            } catch (e: Exception) {
                Logger.println("error while parsing fingerprint(" + idStr + ") :  " + e.toString())
                result = null
            }
            return result
        }


        /**
         * Creates a new Board object based on the state information contained in the input string.
         * 
         * @param dump a String that represents the state of a Board object.
         * @return a new Board object.
         */
        fun createBoardGameDump(dump: String): Board? {
            val data: ByteArray? = unb64unzip(dump.replace("\\s".toRegex(), "")) //remove whitespace
            if (null == data) {
                return null //invalid input String
            }
            var didx = 0
            // 0. data structure version
            val version = data[didx++].toInt()
            if (0 != version) {
                return null //unknown data structure version
            }
            // 1. board size
            val width: Int = getInteger(data, didx)
            didx += 4
            val height: Int = getInteger(data, didx)
            didx += 4
            // 2. robots
            val numRobots = 0xff and data[didx++].toInt()
            val board = Board(width, height, numRobots)
            run {
                var i = 0
                while (numRobots > i) {
                    board.setRobot(i, Companion.getInteger(data, didx), true)
                    didx += 4
                    ++i
                }
            }
            // 3. quadrants
            run {
                var i = 0
                while (board.quadrants.size > i) {
                    board.quadrants[i] = 0xff and data[didx++].toInt()
                    ++i
                }
            }
            // 4. walls
            for (dir in board.walls.indices) {
                for (pos in board.walls[dir].indices) {
                    board.walls[dir][pos] = (0 != data[didx++].toInt())
                }
            }
            // 5. list of goals
            val numGoals: Int = getInteger(data, didx)
            didx += 4
            var i = 0
            while (numGoals > i) {
                val pos: Int = getInteger(data, didx)
                didx += 4
                val robot = 0xff and data[didx++].toInt()
                val shape = 0xff and data[didx++].toInt()
                board.addGoal(pos, (if (255 == robot) -1 else robot), shape)
                ++i
            }
            // 6. active goal
            val pos: Int = getInteger(data, didx)
            didx += 4
            val robot = 0xff and data[didx++].toInt()
            val shape = 0xff and data[didx++].toInt()
            if (-1 == pos) {
                board.goal = null
            } else {
                val setGoalResult = board.setGoal(pos)
                val goal = board.getGoal()
                if ((false == setGoalResult) || (goal.position != pos) || (goal.robotNumber != (if (255 == robot) -1 else robot)) || (goal.shape != shape)) {
                    return null //invalid active goal
                }
            }
            // 7. isFreestyleBoard
            board.isFreestyleBoard = (0 != data[didx].toInt())
            return board
        }


        private fun putInteger(value: Int, data: MutableList<Byte>) {
            data.add((0xff and (value shr 24)).toByte())
            data.add((0xff and (value shr 16)).toByte())
            data.add((0xff and (value shr 8)).toByte())
            data.add((0xff and value).toByte())
        }


        private fun getInteger(data: ByteArray, didx: Int): Int {
            var didx = didx
            var result = data[didx++].toInt()
            result = (result shl 8) or (0xff and data[didx++].toInt())
            result = (result shl 8) or (0xff and data[didx++].toInt())
            result = (result shl 8) or (0xff and data[didx].toInt())
            return result
        }


        private fun zipb64(input: ByteArray): String {
            //store uncompressed length
            val zipOutput = ByteArray(input.size * 2 + 128) //large enough?!
            var i = 0
            var rshift = 24
            while (i < 4) {
                zipOutput[i] = ((input.size ushr rshift) and 0xff).toByte()
                rshift -= 8
                i++
            }
            //zip/deflate data
            val zip = Deflater(9)
            zip.setInput(input)
            zip.finish()
            val zipOutLen =
                4 + zip.deflate(zipOutput, 4, zipOutput.size - 4) //skip uncompressed length
            //encode base64
            val b64Input = zipOutput.copyOf(zipOutLen)
            val b64Output: String
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b64Output = Base64.getEncoder().encodeToString(b64Input)
            } else {
                b64Output =
                    android.util.Base64.encodeToString(b64Input, android.util.Base64.DEFAULT)
            }
            //compute CRC of encoded data
            val crc32 = CRC32()
            crc32.update(b64Output.toByteArray(StandardCharsets.UTF_8))
            val crc32Value = crc32.getValue()
            val crc32String = Formatter().format("%08X", crc32Value).toString()
            //build output string:  starts and ends with "!", to be split at "!"
            val result = "!DriftingDroids_game!" + crc32String + "!" + b64Output + "!"
            return result
        }


        private fun unb64unzip(input: String): ByteArray? {
            var result: ByteArray? = null
            try {
                //split input string and first validation
                val inputSplit: Array<String?> =
                    input.split("!".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                require(
                    !((4 != inputSplit.size) || (inputSplit[1] != "DriftingDroids_game") ||
                            ('!' != input.get(0)) || ('!' != input.get(input.length - 1)))
                ) { "input string has wrong format" }
                //validate data
                val b64crc = inputSplit[2]!!.toLong(16) //throws NumberFormatException
                val crc32 = CRC32()
                crc32.update(inputSplit[3]!!.toByteArray(StandardCharsets.UTF_8))
                require(crc32.getValue() == b64crc) { "data CRC mismatch" }
                //parse base64 string
                val b64Output: ByteArray
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    b64Output =
                        Base64.getDecoder().decode(inputSplit[3]) //throws IllegalArgumentException
                } else {
                    b64Output =
                        android.util.Base64.decode(inputSplit[3], android.util.Base64.DEFAULT)
                }
                //unzip/inflate data
                var unzipLen = 0
                for (i in 0..3) {
                    unzipLen = (unzipLen shl 8) or (0xff and b64Output[i].toInt())
                }
                result = ByteArray(unzipLen)
                val unzip = Inflater()
                unzip.setInput(b64Output, 4, b64Output.size - 4)
                val unzipLenActual = unzip.inflate(result) //throws DataFormatException
                //validate unzip
                require(unzipLen == unzipLenActual) { "uncompressed data length mismatch" }
            } catch (e: Exception) {
                Logger.println("error in unb64unzip: " + e.toString())
                result = null
            }
            return result
        }


        fun getColorLongL10N(color: Int): String {
            if (color < 0) {    //wildcard
                return L10N.getString("board.color.wildcard.text")
            } else {
                return L10N.getString("board.color." + ROBOT_COLOR_NAMES_LONG[color] + ".text")
            }
        }

        fun getColorShortL10N(color: Int): String {
            if (color < 0) {    //wildcard
                return L10N.getString("board.color.w.text")
            } else {
                return L10N.getString("board.color." + ROBOT_COLOR_NAMES_SHORT[color] + ".text")
            }
        }

        fun getGoalShapeL10N(shape: Int): String {
            return L10N.getString("board.shape." + GOAL_SHAPE_NAMES[shape] + ".text")
        }
    }
}

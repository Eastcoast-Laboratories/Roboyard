package roboyard.logic.solver


/**
 * Represents a robot piece in the game.
 * Each piece has a position (x,y) and a color.
 * 
 * This class implements Comparable to allow sorting pieces by their
 * position and color, which is used for state hashing and comparison.
 * 
 * @author Pierre Michel
 * @see RRGameState
 * 
 * @see RRGameMove
 */
class RRPiece : Comparable<RRPiece?> {
    constructor() {
        this.x = 0
        this.y = 0
        this.color = 0
        this.id = 0
    }

    constructor(p: RRPiece) {
        this.x = p.x
        this.y = p.y
        this.color = p.color
        this.id = p.id
    }

    constructor(x: Int, y: Int, color: Int, id: Int) {
        this.x = x
        this.y = y
        this.color = color
        this.id = id
    }

    override fun compareTo(other: RRPiece?): Int {
        if (other == null) return 1
        val a = this.x + this.y
        val b = other.x + other.y
        if (a == b) {
            return 0
        } else if (a > b) {
            return 1
        } else {
            return -1
        }
    }

    var x: Int
    var y: Int
    var color: Int
    val id: Int
}

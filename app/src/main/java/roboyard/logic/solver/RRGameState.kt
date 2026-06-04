package roboyard.logic.solver

import roboyard.logic.core.AGameState
import roboyard.logic.core.IGameMove

/**
 * 
 * @author Pierre Michel
 */
class RRGameState(parentState: AGameState?, previousMove: IGameMove?) :
    AGameState(parentState, previousMove) {
    override fun toString(): String {
        val str = StringBuilder()
        str.append(this.hashCode())
        str.append("\nMain Piece:\n")
        for (p in this.mainPieces) {
            if (p != null) {
                str.append(
                    String.format(
                        "%d -> x:%d, y:%d, color:%d\n",
                        p.hashCode(),
                        p.x,
                        p.y,
                        p.color
                    )
                )
            }
        }
        str.append("Secondary Pieces:\n")
        for (p in this.secondaryPieces) {
            if (p != null) {
                str.append(
                    String.format(
                        "%d -> x:%d, y:%d, color:%d\n",
                        p.hashCode(),
                        p.x,
                        p.y,
                        p.color
                    )
                )
            }
        }

        return str.toString()
    }

    private var allPieces: ArrayList<RRPiece?>? = null
    private var secondaryPieces: Array<RRPiece?> = emptyArray()
    private var mainPieces: Array<RRPiece?> = emptyArray()

    init {
        if (parentState != null) {
            val ps = parentState as RRGameState
            this.allPieces = ArrayList<RRPiece?>()
            this.mainPieces = arrayOfNulls<RRPiece>(ps.mainPieces.size)
            this.secondaryPieces = arrayOfNulls<RRPiece>(ps.secondaryPieces.size)
            for (i in ps.mainPieces.indices) {
                if (ps.mainPieces[i] != null) {
                    this.mainPieces[i] = RRPiece(ps.mainPieces[i]!!)
                }
            }
            for (i in ps.secondaryPieces.indices) {
                if (ps.secondaryPieces[i] != null) {
                    this.secondaryPieces[i] = RRPiece(ps.secondaryPieces[i]!!)
                }
            }
            this.allPieces!!.addAll(this.mainPieces)
            this.allPieces!!.addAll(this.secondaryPieces)
        }
    }
}

package roboyard.logic.core

import java.util.ArrayList

/**
 * 
 * @author Pierre Michel
 */
abstract class AGameState(
    protected val parentState: AGameState?,
    protected val previousMove: IGameMove?
) {
    protected val derivedStates: ArrayList<AGameState?>
    protected var depth: Int = 1

    init {
        this.derivedStates = ArrayList<AGameState?>()
        if (parentState != null) {
            this.depth = parentState.depth + 1
        }
    }
}

package roboyard.logic.core

/**
 * Represents a solution to a puzzle.
 * A solution consists of a sequence of moves that lead from the initial state
 * to a state where a robot reaches its target position.
 * 
 * This class is used by both the game logic and the solver components to
 * store and manipulate solution sequences.
 * 
 * @see IGameMove
 */
class GameSolution {
    fun addMove(move: IGameMove?) {
        this.moves.add(move)
    }

    @JvmField
    val moves: ArrayList<IGameMove?>

    init {
        this.moves = ArrayList<IGameMove?>()
    }
}

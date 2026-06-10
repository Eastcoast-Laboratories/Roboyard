package roboyard.logic.solver

/**
 * Created by Pierre on 08/03/2015.
 */
enum class SolverStatus(val isFinished: Boolean, val code: Int) {
    idle(false, 0),
    solving(false, 1),
    solved(true, 0),
    missingData(true, 1),
    noSolution(true, 2);

    override fun toString(): String {
        val ret = ""
        if (this.isFinished) {
            ret + "Finished: "
        } else {
            ret + "Not Finished: "
        }
        ret + "" + this.code
        return ret
    }
}
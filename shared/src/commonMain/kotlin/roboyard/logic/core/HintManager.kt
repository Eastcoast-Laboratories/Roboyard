package roboyard.logic.core

import driftingdroids.model.Board
import driftingdroids.model.Solution
import roboyard.logic.ui.StringProvider
import roboyard.logic.util.RLog

/**
 * Data class containing everything the UI needs to display a hint.
 * The UI layer (Android/Compose) only needs to render this — no hint logic in the UI.
 */
data class HintDisplayData(
    /** The hint text to display (includes number, arrows, abbreviated history) */
    val text: String,
    /** Robot color index for background color (-1 = default blue) */
    val robotColorForBackground: Int,
    /** Whether this is a pre-hint (not a move hint) */
    val isPreHint: Boolean,
    /** Robot color index to select on the board (-1 = none) */
    val robotColorToSelect: Int,
    /** Direction for hint arrow on board (-1 = none) */
    val hintArrowDirection: Int
)

/**
 * Shared hint manager — extracts ALL hint display logic from Android GameFragment.
 * Both Android and ComposeApp use this class for identical hint behavior.
 *
 * The UI layer only needs to:
 * 1. Call initialize() when a new game starts
 * 2. Call getHintForDisplay() to get the current hint data
 * 3. Call nextHint() / prevHint() for navigation
 * 4. Render the HintDisplayData (text + background color + optional arrow)
 *
 * Uses StringProvider for localization (reads from strings.xml on Android,
 * strings.json on Desktop).
 */
class HintManager(private val stringProvider: StringProvider? = null) {

    private val log = RLog.tag("HintManager")

    companion object {
        /** Number of fixed pre-hints (exact solution, involved robots, move first) */
        const val NUM_FIXED_PRE_HINTS = 3

        /** Maximum hints for levels 1-10 */
        const val MAX_HINTS_UP_TO_LEVEL_10 = 4

        /** Level threshold for no hints */
        const val LEVEL_10_THRESHOLD = 10

        /** Maximum hint history shown in hint text */
        const val MAX_HINT_HISTORY = 6
    }

    /** Number of random pre-hints (2-4) */
    private var numPreHints: Int = 2

    /** Current hint step (includes pre-hints and regular hints) */
    private var currentHintStep: Int = 0

    /** Whether we're showing pre-hints */
    private var showingPreHints: Boolean = true

    /** Total pre-hint steps (random pre-hints + fixed pre-hints). Matches Android numPreHints + NUM_FIXED_PRE_HINTS. */
    fun getTotalPreHintSteps(): Int = numPreHints + NUM_FIXED_PRE_HINTS

    /** True when the current step is the "exact solution" fixed pre-hint (matches Android currentHintStep == numPreHints). */
    fun isExactSolutionHintStep(): Boolean = showingPreHints && currentHintStep == numPreHints

    /** True when the current step is a regular (per-move) hint. */
    fun isRegularHintStep(): Boolean = currentHintStep >= numPreHints + NUM_FIXED_PRE_HINTS

    /** The solution to show hints for */
    private var solution: Solution? = null

    /**
     * Check if the hint manager has a solution set.
     */
    fun hasSolution(): Boolean = solution != null || testMoves != null

    /**
     * Get the moves list from solution or testMoves.
     */
    private fun getMoves(): List<Pair<Int, Int>>? {
        testMoves?.let { return it }
        solution?.getMovesList()?.let { moves ->
            return moves.map { Pair(it.robotNumber, it.direction) }
        }
        return null
    }

    /** Total moves in the solution */
    private var totalMoves: Int = 0

    /** Whether hints are restricted (level games >10) */
    private var hintsRestricted: Boolean = false

    /** Maximum hints allowed (for level games 1-10) */
    private var maxHintsAllowed: Int = Int.MAX_VALUE

    /**
     * Initialize the hint manager for a new game.
     * @param solution The solver solution
     * @param isLevelGame Whether this is a level game
     * @param levelId Level ID (for hint restrictions)
     */
    fun initialize(solution: Solution?, isLevelGame: Boolean, levelId: Int) {
        this.solution = solution
        this.totalMoves = solution?.size() ?: 0
        this.numPreHints = (2..4).random()
        this.currentHintStep = 0
        this.showingPreHints = true

        applyLevelRestrictions(isLevelGame, levelId)

        log.d("[HINT_SYSTEM] Initialized: numPreHints=$numPreHints, totalMoves=$totalMoves, isLevelGame=$isLevelGame, levelId=$levelId, restricted=$hintsRestricted")
    }

    /**
     * Initialize with a list of (robotNumber, direction) moves — e.g. from a
     * GameSolution produced by the shared GameSession solver path.
     */
    fun initialize(moves: List<Pair<Int, Int>>, isLevelGame: Boolean, levelId: Int) {
        this.testMoves = moves
        this.totalMoves = moves.size
        this.numPreHints = (2..4).random()
        this.currentHintStep = 0
        this.showingPreHints = true
        this.solution = null // Use testMoves instead

        applyLevelRestrictions(isLevelGame, levelId)

        log.d("[HINT_SYSTEM] Initialized from move list: numPreHints=$numPreHints, totalMoves=$totalMoves, isLevelGame=$isLevelGame, levelId=$levelId, restricted=$hintsRestricted")
    }

    /** Test-only: initialize with a list of (robotNumber, direction) moves. */
    internal fun initializeForTest(moves: List<Pair<Int, Int>>, isLevelGame: Boolean, levelId: Int) {
        this.testMoves = moves
        this.totalMoves = moves.size
        this.numPreHints = 3 // Fixed for deterministic tests
        this.currentHintStep = 0
        this.showingPreHints = true
        this.solution = null // Use testMoves instead

        applyLevelRestrictions(isLevelGame, levelId)
    }

    /** Level hint restrictions (matches Android). */
    private fun applyLevelRestrictions(isLevelGame: Boolean, levelId: Int) {
        if (isLevelGame && levelId > LEVEL_10_THRESHOLD) {
            hintsRestricted = true
            maxHintsAllowed = 0
        } else if (isLevelGame && levelId <= LEVEL_10_THRESHOLD) {
            hintsRestricted = false
            maxHintsAllowed = MAX_HINTS_UP_TO_LEVEL_10
        } else {
            hintsRestricted = false
            maxHintsAllowed = Int.MAX_VALUE
        }
    }

    /** Test-only moves list (used when solution is null but testMoves is set) */
    private var testMoves: List<Pair<Int, Int>>? = null

    /**
     * Get the total number of possible hints (pre-hints + regular hints).
     */
    fun getTotalPossibleHints(): Int {
        return numPreHints + NUM_FIXED_PRE_HINTS + (getMoves()?.size ?: 0)
    }

    /**
     * Get the current hint step.
     */
    fun getCurrentHintStep(): Int = currentHintStep

    /**
     * Check if hints are restricted for this game.
     */
    fun isHintsRestricted(): Boolean = hintsRestricted

    /**
     * Get the maximum hints allowed.
     */
    fun getMaxHintsAllowed(): Int = maxHintsAllowed

    /**
     * Get the display hint number (1-based).
     */
    fun getDisplayHintNumber(): Int = currentHintStep + 1

    /**
     * Check if we're showing pre-hints.
     */
    fun isShowingPreHints(): Boolean = showingPreHints

    /**
     * Get the current hint as display data. This is the MAIN method the UI calls.
     * Extracts the complete showPreHint/showNormalHint logic from Android.
     * @return HintDisplayData or null if no hint available
     */
    fun getHintForDisplay(): HintDisplayData? {
        val moves = getMoves() ?: return null
        if (moves.isEmpty()) return null

        return if (showingPreHints && currentHintStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
            showPreHint(moves, totalMoves, currentHintStep)
        } else {
            val normalHintIndex = currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS)
            showNormalHint(moves, totalMoves, normalHintIndex)
        }
    }

    /**
     * Shows a pre-hint. Extracted from Android GameFragment.showPreHint.
     * Returns HintDisplayData with text and robot color for background.
     */
    private fun showPreHint(moves: List<Pair<Int, Int>>, totalMoves: Int, currentHintStep: Int): HintDisplayData {
        log.d("[HINT_SYSTEM] Showing pre-hint #${currentHintStep + 1} (total pre-hints: $numPreHints + $NUM_FIXED_PRE_HINTS)")

        val totalPreHints = numPreHints + NUM_FIXED_PRE_HINTS
        val preHintText: String
        var robotColorForBackground = -1

        when {
            // Regular pre-hints: "less than X" (decreasing offset)
            currentHintStep < numPreHints -> {
                val offset = numPreHints - currentHintStep
                val hintValue = totalMoves + offset
                preHintText = if (hintValue <= 1) {
                    stringProvider?.getString("pre_hint_less_than_1") ?: "You found a better solution than the A.I.!"
                } else {
                    stringProvider?.getString("pre_hint_less_than_x", hintValue)
                        ?: "The A.I. found a solution in less than $hintValue moves"
                }
                log.d("[HINT_SYSTEM] Showing regular pre-hint ${currentHintStep + 1}/$numPreHints: less than $hintValue moves")
            }
            // First fixed pre-hint: exact solution length
            currentHintStep == numPreHints -> {
                preHintText = stringProvider?.getString("pre_hint_exact_solution", totalMoves)
                    ?: "The A.I. found a solution in $totalMoves moves"
                log.d("[HINT_SYSTEM] Showing exact solution length: $totalMoves moves")
            }
            // Second fixed pre-hint: involved robot colors
            currentHintStep == numPreHints + 1 -> {
                val displayHintNumber = currentHintStep + 1
                val totalPossibleHints = numPreHints + NUM_FIXED_PRE_HINTS + moves.size
                val sb = StringBuilder()
                sb.append(displayHintNumber).append("/").append(totalPossibleHints).append(": ")
                sb.append(stringProvider?.getString("pre_hint_involved_robots") ?: "Move the").append(" ")

                // Analyze ALL moves to see which robots are involved
                val robotsInvolved = mutableListOf<String>()
                val uniqueColors = mutableListOf<Int>()
                for (move in moves) {
                    val colorName = getRobotColorName(move.first)
                    if (!robotsInvolved.contains(colorName)) {
                        robotsInvolved.add(colorName)
                        uniqueColors.add(move.first)
                    }
                }

                // Format the list with commas and "and" (matches Android)
                val andWord = stringProvider?.getString("and") ?: "and"
                for (i in robotsInvolved.indices) {
                    if (i == robotsInvolved.size - 1 && robotsInvolved.size > 1) {
                        sb.append(andWord).append(" ").append(robotsInvolved[i])
                        sb.append(if (robotsInvolved.size > 1) " robots" else " robot")
                    } else if (i == robotsInvolved.size - 1) {
                        sb.append(robotsInvolved[i])
                        sb.append(" robot")
                    } else if (i == robotsInvolved.size - 2) {
                        sb.append(robotsInvolved[i]).append(" ")
                    } else {
                        sb.append(robotsInvolved[i]).append(", ")
                    }
                }

                preHintText = sb.toString()
                log.d("[HINT_SYSTEM] Showing involved robot colors: $preHintText")

                // Only use color if exactly one robot is involved
                if (uniqueColors.size == 1) {
                    robotColorForBackground = uniqueColors[0]
                }
            }
            // Last fixed pre-hint: which robot to move first
            currentHintStep == numPreHints + 2 -> {
                if (moves.isNotEmpty()) {
                    val firstMove = moves[0]
                    val robotColorName = getRobotColorNameDative(firstMove.first)
                    preHintText = stringProvider?.getString("pre_hint_first_move", robotColorName)
                        ?: "Move the $robotColorName robot first"
                    robotColorForBackground = firstMove.first
                    log.d("[HINT_SYSTEM] Showing which robot to move first: $robotColorName")
                } else {
                    preHintText = stringProvider?.getString("no_solution_found") ?: "No solution found"
                }
            }
            // Fallback
            else -> {
                preHintText = stringProvider?.getString("pre_hint_ready") ?: "Ready to show step-by-step hints"
            }
        }

        // Return robot color to select for "move X first" pre-hint
        val robotColorToSelect = if (currentHintStep == numPreHints + 2 && moves.isNotEmpty()) {
            moves[0].first
        } else -1

        return HintDisplayData(
            text = preHintText,
            robotColorForBackground = robotColorForBackground,
            isPreHint = true,
            robotColorToSelect = robotColorToSelect,
            hintArrowDirection = -1
        )
    }

    /**
     * Shows a normal hint. Extracted from Android GameFragment.showNormalHint.
     * Returns HintDisplayData with text, robot color, and direction arrow.
     */
    private fun showNormalHint(moves: List<Pair<Int, Int>>, totalMoves: Int, hintIndex: Int): HintDisplayData {
        log.d("[HINT_SYSTEM] showNormalHint called with hintIndex: $hintIndex")

        // Validate hint index
        if (hintIndex < 0 || hintIndex >= totalMoves) {
            log.e("[HINT_SYSTEM] Invalid hint index: $hintIndex (total moves: $totalMoves)")
            return HintDisplayData(
                text = stringProvider?.getString("all_hints_shown") ?: "All hints have been shown",
                robotColorForBackground = -1,
                isPreHint = false,
                robotColorToSelect = -1,
                hintArrowDirection = -1
            )
        }

        return try {
            val move = moves[hintIndex]
            val robotColorName = getRobotColorName(move.first)
            val directionArrow = getDirectionArrow(move.second)
            val displayHintNumber = hintIndex + 1

            log.d("[HINT_SYSTEM] Robot color: ${move.first}, Direction: ${move.second}")

            val sb = StringBuilder()
            sb.append(displayHintNumber).append(". ")

            if (hintIndex == 0) {
                // First hint: "ColorName ↑"
                sb.append(robotColorName).append(" ").append(directionArrow)
                log.d("[HINT_SYSTEM] First hint format: ${sb.toString()}")
            } else {
                // Subsequent hints: abbreviated previous moves + current move
                val startIndex = maxOf(0, hintIndex - MAX_HINT_HISTORY)
                if (startIndex > 0) {
                    sb.append("...,")
                }
                var lastColorAbbrev: String? = null
                for (i in startIndex until hintIndex) {
                    if (i >= moves.size) break
                    val prevMove = moves[i]
                    val prevColorAbbrev = getColorAbbreviation(getRobotColorName(prevMove.first))
                    val prevArrow = getDirectionArrow(prevMove.second)
                    // Only add color abbreviation if color changed
                    if (lastColorAbbrev == null || prevColorAbbrev != lastColorAbbrev) {
                        sb.append(prevColorAbbrev)
                    }
                    sb.append(prevArrow)
                    lastColorAbbrev = prevColorAbbrev
                    if (i < hintIndex - 1) {
                        sb.append(",")
                    }
                }
                sb.append(", ").append(robotColorName).append(" ").append(directionArrow)
                log.d("[HINT_SYSTEM] Subsequent hint format: ${sb.toString()}")
            }

            HintDisplayData(
                text = sb.toString(),
                robotColorForBackground = move.first,
                isPreHint = false,
                robotColorToSelect = move.first,
                hintArrowDirection = move.second
            )
        } catch (e: Exception) {
            log.e("[HINT_SYSTEM] Error displaying normal hint #${hintIndex + 1}: ${e.message}")
            HintDisplayData(
                text = stringProvider?.getString("error_displaying_hint") ?: "Error displaying hint",
                robotColorForBackground = -1,
                isPreHint = false,
                robotColorToSelect = -1,
                hintArrowDirection = -1
            )
        }
    }

    /**
     * Advance to the next hint.
     * @return true if there is a next hint
     */
    fun nextHint(): Boolean {
        val total = getTotalPossibleHints()
        if (currentHintStep >= total - 1) return false
        // Check level restrictions
        if (maxHintsAllowed != Int.MAX_VALUE) {
            val regularHintsShown = currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) + 1
            if (regularHintsShown >= maxHintsAllowed && currentHintStep >= numPreHints + NUM_FIXED_PRE_HINTS) return false
        }
        currentHintStep++
        showingPreHints = currentHintStep < numPreHints + NUM_FIXED_PRE_HINTS
        log.d("[HINT_SYSTEM] Next hint: step=$currentHintStep, showingPreHints=$showingPreHints")
        return true
    }

    /**
     * Go to the previous hint.
     * @return true if there is a previous hint
     */
    fun prevHint(): Boolean {
        if (currentHintStep <= 0) return false
        currentHintStep--
        showingPreHints = currentHintStep < numPreHints + NUM_FIXED_PRE_HINTS
        log.d("[HINT_SYSTEM] Prev hint: step=$currentHintStep, showingPreHints=$showingPreHints")
        return true
    }

    /**
     * Check if there is a next hint.
     */
    fun hasNextHint(): Boolean {
        val total = getTotalPossibleHints()
        if (currentHintStep >= total - 1) return false
        if (maxHintsAllowed != Int.MAX_VALUE) {
            val regularHintsShown = currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) + 1
            if (regularHintsShown >= maxHintsAllowed && currentHintStep >= numPreHints + NUM_FIXED_PRE_HINTS) return false
        }
        return true
    }

    /**
     * Check if there is a previous hint.
     */
    fun hasPrevHint(): Boolean = currentHintStep > 0

    /**
     * Get the current regular hint (robot color + direction).
     * @return Pair of (robotColorIndex, direction) or null if not a regular hint step
     */
    fun getRegularHint(): Pair<Int, Int>? {
        val regularHintIndex = currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS)
        if (regularHintIndex < 0) return null
        val moves = getMoves() ?: return null
        if (regularHintIndex >= moves.size) return null
        return moves[regularHintIndex]
    }

    /**
     * Get direction arrow symbol (matches Android getDirectionArrow).
     */
    private fun getDirectionArrow(direction: Int): String {
        return when (direction) {
            Board.NORTH -> "↑"
            Board.SOUTH -> "↓"
            Board.EAST -> "→"
            Board.WEST -> "←"
            else -> "?"
        }
    }

    /**
     * Get color abbreviation (first letter, or 2 letters on conflict).
     * Matches Android getColorAbbreviation.
     */
    private fun getColorAbbreviation(colorName: String): String {
        if (colorName.isEmpty()) return "?"
        // Get all color names to check for conflicts
        val allColors = listOf(
            stringProvider?.getString("color_pink") ?: "Pink",
            stringProvider?.getString("color_blue") ?: "Blue",
            stringProvider?.getString("color_green") ?: "Green",
            stringProvider?.getString("color_yellow") ?: "Yellow",
            stringProvider?.getString("color_silver") ?: "Silver",
            stringProvider?.getString("color_red") ?: "Red",
            stringProvider?.getString("color_brown") ?: "Brown",
            stringProvider?.getString("color_orange") ?: "Orange",
            stringProvider?.getString("color_white") ?: "White"
        )
        val firstLetter = colorName.first().uppercaseChar()
        // Check if any other color starts with the same letter
        val conflicting = allColors.filter { it.isNotEmpty() && it.first().uppercaseChar() == firstLetter }
        return if (conflicting.size > 1 && colorName.length >= 2) {
            // Conflict: use 2-letter abbreviation
            colorName.first().uppercaseChar() + colorName.substring(1, 2).lowercase()
        } else {
            // No conflict: use 1-letter abbreviation
            firstLetter.toString()
        }
    }

    /**
     * Get localized robot color name (nominative form).
     * Uses StringProvider if available, falls back to English.
     */
    private fun getRobotColorName(color: Int): String {
        val key = when (color) {
            0 -> "color_pink"
            1 -> "color_green"
            2 -> "color_blue"
            3 -> "color_yellow"
            4 -> "color_silver"
            5 -> "color_red"
            6 -> "color_brown"
            7 -> "color_orange"
            8 -> "color_white"
            else -> return "robot $color"
        }
        return stringProvider?.getString(key) ?: when (color) {
            0 -> "Pink"
            1 -> "Green"
            2 -> "Blue"
            3 -> "Yellow"
            4 -> "Silver"
            5 -> "Red"
            6 -> "Brown"
            7 -> "Orange"
            8 -> "White"
            else -> "robot $color"
        }
    }

    /**
     * Get localized robot color name (dative form, used in "move the X robot first").
     * Uses StringProvider if available, falls back to nominative form.
     */
    private fun getRobotColorNameDative(color: Int): String {
        val key = when (color) {
            0 -> "color_pink_dative"
            1 -> "color_green_dative"
            2 -> "color_blue_dative"
            3 -> "color_yellow_dative"
            4 -> "color_silver_dative"
            5 -> "color_red_dative"
            6 -> "color_brown_dative"
            7 -> "color_orange_dative"
            8 -> "color_white_dative"
            else -> return getRobotColorName(color)
        }
        return stringProvider?.getString(key) ?: getRobotColorName(color)
    }

    /**
     * Reset only the hint step (used when toggling hints OFF then ON).
     * Keeps the solution so hints can be shown again without re-solving.
     */
    fun resetStep() {
        currentHintStep = 0
        showingPreHints = true
    }

    /**
     * Reset the hint manager for a new game.
     */
    fun reset() {
        currentHintStep = 0
        showingPreHints = true
        solution = null
        testMoves = null
        totalMoves = 0
    }
}

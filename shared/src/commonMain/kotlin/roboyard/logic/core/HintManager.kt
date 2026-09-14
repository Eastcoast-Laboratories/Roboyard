package roboyard.logic.core

import driftingdroids.model.Board
import driftingdroids.model.Solution
import roboyard.logic.ui.StringProvider
import roboyard.logic.util.RLog

/**
 * Shared hint manager for pre-hints and regular hints.
 * Matches Android GameFragment hint system:
 * - Pre-hints: 2-4 random "less than X" + 3 fixed (exact, involved robots, move first)
 * - Regular hints: each move with color + direction
 * - Hint numbering: displayHintNumber/totalPossibleHints
 * - Prev/next navigation
 *
 * Both Android and ComposeApp use this class for consistent hint behavior.
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

        /** Maximum hint history shown in hint text */
        const val MAX_HINT_HISTORY = 6
    }

    /** Number of random pre-hints (2-4) */
    private var numPreHints: Int = 2

    /** Current hint step (includes pre-hints and regular hints) */
    private var currentHintStep: Int = 0

    /** Whether we're showing pre-hints */
    private var showingPreHints: Boolean = true

    /** The solution to show hints for */
    private var solution: Solution? = null

    /** Total moves in the solution */
    private var totalMoves: Int = 0

    /** Whether hints are restricted (level games >10) */
    private var hintsRestricted: Boolean = false

    /** Maximum hints allowed (for level games 1-10) */
    private var maxHintsAllowed: Int = Int.MAX_VALUE

    /** History of shown hints for display */
    private val hintHistory: MutableList<String> = mutableListOf()

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
        this.hintHistory.clear()

        // Level hint restrictions
        if (isLevelGame && levelId > 10) {
            hintsRestricted = true
            maxHintsAllowed = 0
        } else if (isLevelGame && levelId <= 10) {
            hintsRestricted = false
            maxHintsAllowed = MAX_HINTS_UP_TO_LEVEL_10
        } else {
            hintsRestricted = false
            maxHintsAllowed = Int.MAX_VALUE
        }

        log.d("[HINT_SYSTEM] Initialized: numPreHints=$numPreHints, totalMoves=$totalMoves, isLevelGame=$isLevelGame, levelId=$levelId, restricted=$hintsRestricted")
    }

    /**
     * Get the total number of possible hints (pre-hints + regular hints).
     */
    fun getTotalPossibleHints(): Int {
        return numPreHints + NUM_FIXED_PRE_HINTS + (solution?.size() ?: 0)
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
     * Get the current pre-hint text.
     * @return Pre-hint text, or null if not a pre-hint step
     */
    fun getPreHintText(): String? {
        if (currentHintStep >= numPreHints + NUM_FIXED_PRE_HINTS) return null

        val totalPreHints = numPreHints + NUM_FIXED_PRE_HINTS

        return when {
            // Regular pre-hints: "less than X" (decreasing offset)
            currentHintStep < numPreHints -> {
                val offset = numPreHints - currentHintStep
                val hintValue = totalMoves + offset
                if (hintValue <= 1) {
                    stringProvider?.getString("pre_hint_less_than_1") ?: "You found a better solution than the A.I.!"
                } else {
                    stringProvider?.getString("pre_hint_less_than_x", hintValue)
                        ?: "The A.I. found a solution in less than $hintValue moves"
                }
            }
            // First fixed pre-hint: exact solution length
            currentHintStep == numPreHints -> {
                stringProvider?.getString("pre_hint_exact_solution", totalMoves)
                    ?: "The A.I. found a solution in $totalMoves moves"
            }
            // Second fixed pre-hint: involved robots
            currentHintStep == numPreHints + 1 -> {
                val robots = getInvolvedRobots()
                val prefix = stringProvider?.getString("pre_hint_involved_robots") ?: "Move the"
                val andWord = stringProvider?.getString("and") ?: "and"
                val robotList = formatRobotList(robots, andWord)
                "$prefix $robotList"
            }
            // Third fixed pre-hint: move first robot
            currentHintStep == numPreHints + 2 -> {
                val firstMove = solution?.getMovesList()?.firstOrNull()
                if (firstMove != null) {
                    val robotColor = getRobotColorNameDative(firstMove.robotNumber)
                    stringProvider?.getString("pre_hint_first_move", robotColor)
                        ?: "Move the $robotColor robot first"
                } else {
                    stringProvider?.getString("no_solution_found") ?: "No solution found"
                }
            }
            else -> null
        }
    }

    /**
     * Get the current regular hint (move hint).
     * @return Pair of (robotColorIndex, direction) or null if not a regular hint step
     */
    fun getRegularHint(): Pair<Int, Int>? {
        val regularHintIndex = currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS)
        if (regularHintIndex < 0) return null
        val moves = solution?.getMovesList() ?: return null
        if (regularHintIndex >= moves.size) return null

        val move = moves[regularHintIndex]
        return Pair(move.robotNumber, move.direction)
    }

    /**
     * Get the regular hint text.
     * @return Hint text like "Move red robot UP"
     */
    fun getRegularHintText(): String? {
        val hint = getRegularHint() ?: return null
        val colorName = getRobotColorName(hint.first)
        val directionArrow = getDirectionArrow(hint.second)
        val hintIndex = currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS)

        // Build hint text with abbreviated history (matches Android format)
        val sb = StringBuilder()
        sb.append(getDisplayHintNumber()).append(". ")

        if (hintIndex == 0) {
            // First hint: just show "ColorName ↑"
            sb.append(colorName).append(" ").append(directionArrow)
        } else {
            // Subsequent hints: show abbreviated previous moves + current move
            val startIndex = maxOf(0, hintIndex - MAX_HINT_HISTORY)
            if (startIndex > 0) {
                sb.append("...,")
            }
            var lastColorAbbrev: String? = null
            for (i in startIndex until hintIndex) {
                val moves = solution?.getMovesList() ?: break
                if (i >= moves.size) break
                val move = moves[i]
                val prevColorAbbrev = getColorAbbreviation(getRobotColorName(move.robotNumber))
                val prevArrow = getDirectionArrow(move.direction)
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
            sb.append(", ").append(colorName).append(" ").append(directionArrow)
        }

        return sb.toString()
    }

    /**
     * Get the full hint text with numbering.
     * @return Formatted hint text like "3. P↑, G→, B↑" (matches Android format)
     */
    fun getFullHintText(): String? {
        val hintText = getPreHintText() ?: getRegularHintText() ?: return null
        return hintText
    }

    /**
     * Advance to the next hint.
     * @return true if there is a next hint
     */
    fun nextHint(): Boolean {
        val total = getTotalPossibleHints()
        if (currentHintStep >= total - 1) return false
        if (currentHintStep >= maxHintsAllowed + numPreHints + NUM_FIXED_PRE_HINTS - 1 && maxHintsAllowed != Int.MAX_VALUE) {
            // Check if we've reached the max hints for level games
            val regularHintsShown = currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) + 1
            if (regularHintsShown >= maxHintsAllowed) return false
        }
        currentHintStep++
        showingPreHints = currentHintStep < numPreHints + NUM_FIXED_PRE_HINTS

        // Add to hint history
        val hintText = getFullHintText()
        if (hintText != null) {
            hintHistory.add(hintText)
            if (hintHistory.size > MAX_HINT_HISTORY) {
                hintHistory.removeAt(0)
            }
        }

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
     * Get the hint history (last MAX_HINT_HISTORY hints).
     */
    fun getHintHistory(): List<String> = hintHistory.toList()

    /**
     * Get the involved robot colors from the solution.
     */
    private fun getInvolvedRobots(): List<String> {
        val moves = solution?.getMovesList() ?: return emptyList()
        val robots = mutableSetOf<String>()
        for (move in moves) {
            robots.add(getRobotColorName(move.robotNumber))
        }
        return robots.toList()
    }

    /**
     * Get localized robot color name (nominative form).
     * Uses StringProvider if available, falls back to English.
     */
    private fun getRobotColorName(color: Int): String {
        val key = when (color) {
            0 -> "color_red"
            1 -> "color_green"
            2 -> "color_blue"
            3 -> "color_yellow"
            4 -> "color_silver"
            5 -> "color_pink"
            6 -> "color_brown"
            7 -> "color_orange"
            8 -> "color_white"
            else -> return "robot $color"
        }
        return stringProvider?.getString(key) ?: when (color) {
            0 -> "red"
            1 -> "green"
            2 -> "blue"
            3 -> "yellow"
            4 -> "silver"
            else -> "robot $color"
        }
    }

    /**
     * Get localized robot color name (dative form, used in "move the X robot first").
     * Uses StringProvider if available, falls back to nominative form.
     */
    private fun getRobotColorNameDative(color: Int): String {
        val key = when (color) {
            0 -> "color_red_dative"
            1 -> "color_green_dative"
            2 -> "color_blue_dative"
            3 -> "color_yellow_dative"
            4 -> "color_silver_dative"
            5 -> "color_pink_dative"
            6 -> "color_brown_dative"
            7 -> "color_orange_dative"
            8 -> "color_white_dative"
            else -> return getRobotColorName(color)
        }
        return stringProvider?.getString(key) ?: getRobotColorName(color)
    }

    /**
     * Format a list of robot colors for the "involved robots" pre-hint.
     * Matches Android GameFragment format: "red, blue and green" (with localized "and").
     */
    private fun formatRobotList(robots: List<String>, andWord: String): String {
        return when {
            robots.isEmpty() -> ""
            robots.size == 1 -> robots[0]
            robots.size == 2 -> "${robots[0]} $andWord ${robots[1]}"
            else -> robots.dropLast(1).joinToString(", ") + " $andWord ${robots.last()}"
        }
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
     * Get localized direction name (for regular hints).
     * Uses StringProvider if available, falls back to English.
     */
    private fun getDirectionName(direction: Int): String {
        val key = when (direction) {
            Board.NORTH -> "direction_up"
            Board.SOUTH -> "direction_down"
            Board.EAST -> "direction_right"
            Board.WEST -> "direction_left"
            else -> return "unknown"
        }
        return stringProvider?.getString(key) ?: when (direction) {
            Board.NORTH -> "UP"
            Board.SOUTH -> "DOWN"
            Board.EAST -> "RIGHT"
            Board.WEST -> "LEFT"
            else -> "unknown"
        }
    }

    /**
     * Reset the hint manager for a new game.
     */
    fun reset() {
        currentHintStep = 0
        showingPreHints = true
        hintHistory.clear()
        solution = null
        totalMoves = 0
    }
}

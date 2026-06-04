package roboyard.logic.core

import timber.log.Timber
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Generates wall patterns for the level editor.
 * Provides 10 different generation modes for varied board layouts.
 */
class WallPatternGenerator(private val width: Int, private val height: Int) {
    private val rand = Random()

    // Wall arrays: [x][y], 1 = wall present
    private lateinit var hWalls: Array<IntArray?> // horizontal walls
    private lateinit var vWalls: Array<IntArray?> // vertical walls

    /**
     * Generate walls for the given pattern mode.
     * Returns a GameState with border walls, center carree, and pattern walls.
     * Robots and targets are NOT added — the editor handles those separately.
     */
    fun generate(pattern: Int): GameState {
        hWalls = Array<IntArray?>(width + 1) { IntArray(height + 1) }
        vWalls = Array<IntArray?>(width + 1) { IntArray(height + 1) }

        addBorderWalls()
        addCenterCarree()

        when (pattern) {
            PATTERN_CLASSIC -> generateClassic()
            PATTERN_SPIRAL -> generateSpiral()
            PATTERN_ROOMS -> generateRooms()
            PATTERN_MAZE -> generateMaze()
            PATTERN_DIAGONAL -> generateDiagonal()
            PATTERN_SYMMETRIC -> generateSymmetric()
            PATTERN_CORRIDORS -> generateCorridors()
            PATTERN_ISLANDS -> generateIslands()
            PATTERN_BORDER_HEAVY -> generateBorderHeavy()
            PATTERN_SCATTER -> generateScatter()
            else -> generateClassic()
        }

        Timber.d("[WALL_PATTERN] Generated pattern %d for %dx%d board", pattern, width, height)
        return buildGameState()
    }

    // ── helpers ──────────────────────────────────────────────────────────
    private fun addBorderWalls() {
        for (x in 0..<width) {
            hWalls[x]!![0] = 1
            hWalls[x]!![height] = 1
        }
        for (y in 0..<height) {
            vWalls[0]!![y] = 1
            vWalls[width]!![y] = 1
        }
    }

    private fun addCenterCarree() {
        val cx = width / 2 - 1
        val cy = height / 2 - 1
        hWalls[cx]!![cy] = 1
        hWalls[cx + 1]!![cy] = 1
        hWalls[cx]!![cy + 2] = 1
        hWalls[cx + 1]!![cy + 2] = 1
        vWalls[cx]!![cy] = 1
        vWalls[cx]!![cy + 1] = 1
        vWalls[cx + 2]!![cy] = 1
        vWalls[cx + 2]!![cy + 1] = 1
    }

    private fun inCarree(x: Int, y: Int): Boolean {
        val cx = width / 2 - 1
        val cy = height / 2 - 1
        return x >= cx && x <= cx + 1 && y >= cy && y <= cy + 1
    }

    private fun safeH(x: Int, y: Int): Boolean {
        return x >= 0 && x < width && y >= 0 && y <= height && hWalls[x]!![y] == 0
    }

    private fun safeV(x: Int, y: Int): Boolean {
        return x >= 0 && x <= width && y >= 0 && y < height && vWalls[x]!![y] == 0
    }

    private fun placeH(x: Int, y: Int) {
        if (safeH(x, y) && !inCarree(x, y)) hWalls[x]!![y] = 1
    }

    private fun placeV(x: Int, y: Int) {
        if (safeV(x, y) && !inCarree(x, y)) vWalls[x]!![y] = 1
    }

    private fun rng(min: Int, max: Int): Int {
        if (min >= max) return min
        return min + rand.nextInt(max - min + 1)
    }

    private fun buildGameState(): GameState {
        val state = GameState(width, height)
        for (x in 0..width) {
            for (y in 0..height) {
                if (hWalls[x]!![y] == 1 && x < width) {
                    state.addHorizontalWall(x, y)
                }
                if (vWalls[x]!![y] == 1 && y < height) {
                    state.addVerticalWall(x, y)
                }
            }
        }
        return state
    }

    // ── Pattern 0: Classic (L-shaped wall pairs in quadrants) ───────────
    private fun generateClassic() {
        val wallsPerQuadrant = width / 4
        val quadrantBounds = arrayOf<IntArray>(
            intArrayOf(1, 1, width / 2 - 1, height / 2 - 1),
            intArrayOf(width / 2, 1, width - 2, height / 2 - 1),
            intArrayOf(1, height / 2, width / 2 - 1, height - 2),
            intArrayOf(width / 2, height / 2, width - 2, height - 2)
        )

        for (bounds in quadrantBounds) {
            for (i in 0..<wallsPerQuadrant) {
                for (attempt in 0..49) {
                    val x = rng(bounds[0], bounds[2])
                    val y = rng(bounds[1], bounds[3])
                    if (inCarree(x, y)) continue
                    if (hWalls[x]!![y] == 1) continue

                    val vx = x + rng(0, 1)
                    val vy = y - rng(0, 1)
                    if (safeH(x, y) && safeV(vx, vy) && !inCarree(vx, vy)) {
                        hWalls[x]!![y] = 1
                        vWalls[vx]!![vy] = 1
                        break
                    }
                }
            }
        }
    }

    // ── Pattern 1: Spiral ───────────────────────────────────────────────
    private fun generateSpiral() {
        var left = 2
        var right = width - 3
        var top = 2
        var bottom = height - 3
        var direction = 0 // 0=right, 1=down, 2=left, 3=up

        while (left < right && top < bottom) {
            val gapPos: Int
            when (direction) {
                0 -> {
                    gapPos = rng(left + 1, right - 1)
                    var x = left
                    while (x <= right) {
                        if (x != gapPos) placeH(x, top)
                        x++
                    }
                    top += 2
                }

                1 -> {
                    gapPos = rng(top + 1, bottom - 1)
                    var y = top
                    while (y <= bottom) {
                        if (y != gapPos) placeV(right, y)
                        y++
                    }
                    right -= 2
                }

                2 -> {
                    gapPos = rng(left + 1, right - 1)
                    var x = left
                    while (x <= right) {
                        if (x != gapPos) placeH(x, bottom)
                        x++
                    }
                    bottom -= 2
                }

                3 -> {
                    gapPos = rng(top + 1, bottom - 1)
                    var y = top
                    while (y <= bottom) {
                        if (y != gapPos) placeV(left, y)
                        y++
                    }
                    left += 2
                }
            }
            direction = (direction + 1) % 4
        }
    }

    // ── Pattern 2: Rooms ────────────────────────────────────────────────
    private fun generateRooms() {
        // Divide board into a grid of rooms with doorways
        val roomsX = max(2, width / 5)
        val roomsY = max(2, height / 5)
        val roomW = width / roomsX
        val roomH = height / roomsY

        for (rx in 1..<roomsX) {
            val wallX = rx * roomW
            val doorY = rng(0, roomsY - 1) * roomH + rng(1, roomH - 1)
            for (y in 1..<height - 1) {
                if (abs(y - doorY) > 0) {
                    placeV(wallX, y)
                }
            }
        }

        for (ry in 1..<roomsY) {
            val wallY = ry * roomH
            val doorX = rng(0, roomsX - 1) * roomW + rng(1, roomW - 1)
            for (x in 1..<width - 1) {
                if (abs(x - doorX) > 0) {
                    placeH(x, wallY)
                }
            }
        }

        // Add a few random L-walls inside rooms for interest
        for (i in 0..<width / 2) {
            val x = rng(2, width - 3)
            val y = rng(2, height - 3)
            if (!inCarree(x, y) && safeH(x, y)) {
                placeH(x, y)
                if (rand.nextBoolean()) placeV(x, y)
                else placeV(x + 1, y)
            }
        }
    }

    // ── Pattern 3: Maze ─────────────────────────────────────────────────
    private fun generateMaze() {
        // Recursive backtracker maze where every board cell is a maze node.
        // Wall between cell (x,y) and (x+1,y) = vWalls[x+1][y]
        // Wall between cell (x,y) and (x,y+1) = hWalls[x][y+1]
        // After carving, every cell is reachable from every other cell.

        val cx2 = width / 2 - 1
        val cy2 = height / 2 - 1

        // Place ALL interior walls (border walls are already set by addBorderWalls)
        for (x in 1..<width) {
            for (y in 0..<height) {
                vWalls[x]!![y] = 1 // vertical wall between (x-1,y) and (x,y)
            }
        }
        for (x in 0..<width) {
            for (y in 1..<height) {
                hWalls[x]!![y] = 1 // horizontal wall between (x,y-1) and (x,y)
            }
        }

        // Recursive backtracker: visit every cell exactly once, removing walls
        val visited = Array<BooleanArray?>(width) { BooleanArray(height) }
        // Mark center carree cells as visited so maze carves around them
        var x = cx2
        while (x <= cx2 + 1 && x < width) {
            var y = cy2
            while (y <= cy2 + 1 && y < height) {
                visited[x]!![y] = true
                y++
            }
            x++
        }

        val stack = ArrayList<IntArray>()
        // Start from (0,0)
        visited[0]!![0] = true
        stack.add(intArrayOf(0, 0))

        val dirs = arrayOf<IntArray>(
            intArrayOf(0, -1),
            intArrayOf(1, 0),
            intArrayOf(0, 1),
            intArrayOf(-1, 0)
        )

        while (!stack.isEmpty()) {
            val cur = stack.get(stack.size - 1)
            val cx = cur[0]
            val cy = cur[1]

            // Collect unvisited neighbors
            val neighbors = ArrayList<IntArray>()
            for (d in dirs) {
                val nx = cx + d[0]
                val ny = cy + d[1]
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && !visited[nx]!![ny]) {
                    neighbors.add(intArrayOf(nx, ny, d[0], d[1]))
                }
            }

            if (neighbors.isEmpty()) {
                stack.removeAt(stack.size - 1)
            } else {
                val chosen = neighbors.get(rand.nextInt(neighbors.size))
                val nx = chosen[0]
                val ny = chosen[1]
                val dx = chosen[2]
                val dy = chosen[3]
                visited[nx]!![ny] = true

                // Remove wall between (cx,cy) and (nx,ny)
                if (dx == 1) {
                    // Moving right → remove vWalls[cx+1][cy]
                    vWalls[cx + 1]!![cy] = 0
                } else if (dx == -1) {
                    // Moving left → remove vWalls[cx][cy]
                    vWalls[cx]!![cy] = 0
                } else if (dy == 1) {
                    // Moving down → remove hWalls[cx][cy+1]
                    hWalls[cx]!![cy + 1] = 0
                } else if (dy == -1) {
                    // Moving up → remove hWalls[cx][cy]
                    hWalls[cx]!![cy] = 0
                }

                stack.add(intArrayOf(nx, ny))
            }
        }

        // Open a few extra walls to create loops (makes it less of a perfect maze,
        // more interesting for Roboyard where robots slide until hitting a wall)
        val extraOpenings = (width + height) / 2
        for (i in 0..<extraOpenings) {
            val x = rng(1, width - 1)
            val y = rng(1, height - 1)
            if (!inCarree(x, y)) {
                if (rand.nextBoolean() && hWalls[x]!![y] == 1) {
                    hWalls[x]!![y] = 0
                } else if (vWalls[x]!![y] == 1) {
                    vWalls[x]!![y] = 0
                }
            }
        }

        Timber.d(
            "[WALL_PATTERN] Maze generated: %dx%d cells, %d extra openings",
            width,
            height,
            extraOpenings
        )
    }

    // ── Pattern 4: Diagonal ─────────────────────────────────────────────
    private fun generateDiagonal() {
        // Create diagonal lines of walls from corners
        val numLines = rng(3, 6)

        for (line in 0..<numLines) {
            val startX: Int
            val startY: Int
            val dx: Int
            val dy: Int
            when (line % 4) {
                0 -> {
                    startX = rng(1, 3)
                    startY = rng(1, 3)
                    dx = 1
                    dy = 1
                }

                1 -> {
                    startX = width - rng(2, 4)
                    startY = rng(1, 3)
                    dx = -1
                    dy = 1
                }

                2 -> {
                    startX = rng(1, 3)
                    startY = height - rng(2, 4)
                    dx = 1
                    dy = -1
                }

                else -> {
                    startX = width - rng(2, 4)
                    startY = height - rng(2, 4)
                    dx = -1
                    dy = -1
                }
            }

            var x = startX
            var y = startY
            val len = rng(3, min(width, height) / 2)
            for (i in 0..<len) {
                if (x < 1 || x >= width - 1 || y < 1 || y >= height - 1) break
                if (!inCarree(x, y)) {
                    // Alternate between horizontal and vertical to create staircase
                    if (i % 2 == 0) placeH(x, y)
                    else placeV(x, y)
                }
                x += dx
                y += dy
            }
        }

        // Fill with some random L-walls
        for (i in 0..<width / 3) {
            val x = rng(2, width - 3)
            val y = rng(2, height - 3)
            if (!inCarree(x, y) && safeH(x, y)) {
                placeH(x, y)
                placeV(x + rng(0, 1), y)
            }
        }
    }

    // ── Pattern 5: Symmetric (4-fold mirror) ────────────────────────────
    private fun generateSymmetric() {
        val halfW = width / 2
        val halfH = height / 2
        val wallCount = rng(width / 2, width)

        for (i in 0..<wallCount) {
            val x = rng(1, halfW - 1)
            val y = rng(1, halfH - 1)
            val horiz = rand.nextBoolean()

            if (horiz) {
                placeH(x, y)
                placeH(width - 1 - x, y)
                placeH(x, height - y)
                placeH(width - 1 - x, height - y)
            } else {
                placeV(x, y)
                placeV(width - x, y)
                placeV(x, height - 1 - y)
                placeV(width - x, height - 1 - y)
            }
        }
    }

    // ── Pattern 6: Corridors ────────────────────────────────────────────
    private fun generateCorridors() {
        // Long horizontal and vertical wall lines with gaps
        val numCorridors = rng(3, 6)

        for (c in 0..<numCorridors) {
            val horiz = rand.nextBoolean()
            if (horiz) {
                val y = rng(2, height - 3)
                val gapX = rng(2, width - 3)
                val gapWidth = rng(1, 3)
                for (x in 1..<width - 1) {
                    if (x < gapX || x >= gapX + gapWidth) {
                        placeH(x, y)
                    }
                }
            } else {
                val x = rng(2, width - 3)
                val gapY = rng(2, height - 3)
                val gapHeight = rng(1, 3)
                for (y in 1..<height - 1) {
                    if (y < gapY || y >= gapY + gapHeight) {
                        placeV(x, y)
                    }
                }
            }
        }

        // Add some perpendicular stubs
        for (i in 0..<width / 3) {
            val x = rng(2, width - 3)
            val y = rng(2, height - 3)
            if (!inCarree(x, y)) {
                if (rand.nextBoolean()) placeH(x, y)
                else placeV(x, y)
            }
        }
    }

    // ── Pattern 7: Islands ──────────────────────────────────────────────
    private fun generateIslands() {
        // Many small wall clusters spread evenly across the board
        // Use a grid-based approach to ensure good coverage
        val gridStep = 3
        val numIslands = rng(width * height / 12, width * height / 8)

        for (i in 0..<numIslands) {
            val cx = rng(1, width - 2)
            val cy = rng(1, height - 2)
            if (inCarree(cx, cy)) continue

            val size = rng(1, 2)
            // Create a small L-shaped or T-shaped cluster
            for (dx in 0..size) {
                for (dy in 0..size) {
                    val wx = cx + dx
                    val wy = cy + dy
                    if (wx >= 0 && wx < width && wy >= 0 && wy < height && !inCarree(wx, wy)) {
                        if (rand.nextInt(3) > 0) placeH(wx, wy)
                        if (rand.nextInt(3) > 0) placeV(wx, wy)
                    }
                }
            }
        }

        // Fill sparse areas with extra single walls
        var x = 1
        while (x < width - 1) {
            var y = 1
            while (y < height - 1) {
                if (!inCarree(x, y) && hWalls[x]!![y] == 0 && vWalls[x]!![y] == 0) {
                    if (rand.nextInt(3) == 0) {
                        if (rand.nextBoolean()) placeH(x, y)
                        else placeV(x, y)
                    }
                }
                y += gridStep
            }
            x += gridStep
        }
    }

    // ── Pattern 8: Border Heavy ─────────────────────────────────────────
    private fun generateBorderHeavy() {
        // Many walls near the edges, fewer in the center
        // Extended range: 0 to width/height (includes edge positions)
        val margin = max(3, width / 3)
        val wallCount = (width + height) * 2

        for (i in 0..<wallCount) {
            val x: Int
            val y: Int
            // 70% chance near border, 30% chance anywhere
            if (rand.nextInt(10) < 7) {
                // Near a border — range includes edge cells (0 and max)
                when (rand.nextInt(4)) {
                    0 -> {
                        x = rng(0, margin)
                        y = rng(0, height - 1)
                    }

                    1 -> {
                        x = rng(width - 1 - margin, width - 1)
                        y = rng(0, height - 1)
                    }

                    2 -> {
                        x = rng(0, width - 1)
                        y = rng(0, margin)
                    }

                    else -> {
                        x = rng(0, width - 1)
                        y = rng(height - 1 - margin, height - 1)
                    }
                }
            } else {
                x = rng(0, width - 1)
                y = rng(0, height - 1)
            }

            if (!inCarree(x, y)) {
                if (rand.nextBoolean()) placeH(x, y)
                else placeV(x, y)
            }
        }
    }

    // ── Pattern 9: Scatter (random walls everywhere) ────────────────────
    private fun generateScatter() {
        // Random walls everywhere, including edge positions
        val wallCount = rng(width + height, (width + height) * 2)

        for (i in 0..<wallCount) {
            val x = rng(0, width - 1)
            val y = rng(0, height - 1)
            if (!inCarree(x, y)) {
                if (rand.nextBoolean()) placeH(x, y)
                else placeV(x, y)
            }
        }
    }

    companion object {
        const val PATTERN_CLASSIC: Int = 0
        const val PATTERN_SPIRAL: Int = 1
        const val PATTERN_ROOMS: Int = 2
        const val PATTERN_MAZE: Int = 3
        const val PATTERN_DIAGONAL: Int = 4
        const val PATTERN_SYMMETRIC: Int = 5
        const val PATTERN_CORRIDORS: Int = 6
        const val PATTERN_ISLANDS: Int = 7
        const val PATTERN_BORDER_HEAVY: Int = 8
        const val PATTERN_SCATTER: Int = 9

        const val PATTERN_COUNT: Int = 10

        // ── Border Stubs (perpendicular walls on outer edges) ─────────────
        /**
         * Remove existing border stubs and generate new ones on a GameState.
         * Places 2-3 perpendicular walls per side, not too close to corners (min 2 cells away).
         * Top/bottom borders get vertical stubs, left/right borders get horizontal stubs.
         */
        @JvmStatic
        fun generateBorderStubs(state: GameState) {
            val w = state.width
            val h = state.height
            val r = Random()
            val minCornerDist = 2

            // Remove existing border stubs (perpendicular walls touching the border)
            val toRemove: MutableList<GameElement> = ArrayList<GameElement>()
            for (el in state.gameElements) {
                if (el.type == GameElement.TYPE_VERTICAL_WALL) {
                    // Vertical wall on top border (y=0) or bottom border (y=h-1)
                    if (el.y == 0 || el.y == h - 1) {
                        // Don't remove corner border walls (x=0 or x=w)
                        if (el.x > 0 && el.x < w) {
                            toRemove.add(el)
                        }
                    }
                } else if (el.type == GameElement.TYPE_HORIZONTAL_WALL) {
                    // Horizontal wall on left border (x=0) or right border (x=w-1)
                    if (el.x == 0 || el.x == w - 1) {
                        // Don't remove corner border walls (y=0 or y=h)
                        if (el.y > 0 && el.y < h) {
                            toRemove.add(el)
                        }
                    }
                }
            }
            // Clean up board[][] for removed stubs
            for (el in toRemove) {
                state.setCellType(el.x, el.y, 0)
            }
            state.gameElements.removeAll(toRemove)

            Timber.d(
                "[BORDER_STUBS] Removed %d old border stubs from %dx%d board",
                toRemove.size,
                w,
                h
            )

            // Generate new stubs per side
            generateStubsForSide(state, r, w, h, minCornerDist)
        }

        private fun generateStubsForSide(
            state: GameState,
            r: Random,
            w: Int,
            h: Int,
            minDist: Int
        ) {
            // Top border: vertical walls at y=0, x varies
            placeStubsOnEdge(state, r, minDist, w - 1 - minDist, true, 0, w, h)
            // Bottom border: vertical walls at y=h, x varies
            placeStubsOnEdge(state, r, minDist, w - 1 - minDist, true, h, w, h)
            // Left border: horizontal walls at x=0, y varies
            placeStubsOnEdge(state, r, minDist, h - 1 - minDist, false, 0, w, h)
            // Right border: horizontal walls at x=w, y varies
            placeStubsOnEdge(state, r, minDist, h - 1 - minDist, false, w, w, h)
        }

        private fun placeStubsOnEdge(
            state: GameState, r: Random, rangeMin: Int, rangeMax: Int,
            isHorizontalEdge: Boolean, fixedCoord: Int, boardW: Int, boardH: Int
        ) {
            if (rangeMin >= rangeMax) return
            val count = 2 + r.nextInt(2) // 2 or 3 stubs
            val minSpacing = 2
            val positions = ArrayList<Int?>()

            for (i in 0..<count) {
                for (attempt in 0..29) {
                    val pos = rangeMin + r.nextInt(rangeMax - rangeMin + 1)
                    var tooClose = false
                    for (existing in positions) {
                        if (abs(pos - existing!!) < minSpacing) {
                            tooClose = true
                            break
                        }
                    }
                    if (!tooClose) {
                        positions.add(pos)
                        break
                    }
                }
            }

            for (pos in positions) {
                if (isHorizontalEdge) {
                    // Horizontal edge (top/bottom) → place vertical wall stub
                    state.addVerticalWall(pos!!, if (fixedCoord == 0) 0 else boardH - 1)
                } else {
                    // Vertical edge (left/right) → place horizontal wall stub
                    state.addHorizontalWall(if (fixedCoord == 0) 0 else boardW - 1, pos!!)
                }
            }

            Timber.d(
                "[BORDER_STUBS] Placed %d stubs on %s edge at %d",
                positions.size, if (isHorizontalEdge) "horizontal" else "vertical", fixedCoord
            )
        }
    }
}

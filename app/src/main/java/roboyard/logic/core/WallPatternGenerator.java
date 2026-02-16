package roboyard.logic.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

/**
 * Generates wall patterns for the level editor.
 * Provides 10 different generation modes for varied board layouts.
 */
public class WallPatternGenerator {

    public static final int PATTERN_CLASSIC = 0;
    public static final int PATTERN_SPIRAL = 1;
    public static final int PATTERN_ROOMS = 2;
    public static final int PATTERN_MAZE = 3;
    public static final int PATTERN_DIAGONAL = 4;
    public static final int PATTERN_SYMMETRIC = 5;
    public static final int PATTERN_CORRIDORS = 6;
    public static final int PATTERN_ISLANDS = 7;
    public static final int PATTERN_BORDER_HEAVY = 8;
    public static final int PATTERN_SCATTER = 9;

    public static final int PATTERN_COUNT = 10;

    private final Random rand = new Random();
    private final int width;
    private final int height;

    // Wall arrays: [x][y], 1 = wall present
    private int[][] hWalls; // horizontal walls
    private int[][] vWalls; // vertical walls

    public WallPatternGenerator(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Generate walls for the given pattern mode.
     * Returns a GameState with border walls, center carree, and pattern walls.
     * Robots and targets are NOT added — the editor handles those separately.
     */
    public GameState generate(int pattern) {
        hWalls = new int[width + 1][height + 1];
        vWalls = new int[width + 1][height + 1];

        addBorderWalls();
        addCenterCarree();

        switch (pattern) {
            case PATTERN_CLASSIC:    generateClassic();     break;
            case PATTERN_SPIRAL:     generateSpiral();      break;
            case PATTERN_ROOMS:      generateRooms();       break;
            case PATTERN_MAZE:       generateMaze();        break;
            case PATTERN_DIAGONAL:   generateDiagonal();    break;
            case PATTERN_SYMMETRIC:  generateSymmetric();   break;
            case PATTERN_CORRIDORS:  generateCorridors();   break;
            case PATTERN_ISLANDS:    generateIslands();     break;
            case PATTERN_BORDER_HEAVY: generateBorderHeavy(); break;
            case PATTERN_SCATTER:    generateScatter();     break;
            default:                 generateClassic();     break;
        }

        Timber.d("[WALL_PATTERN] Generated pattern %d for %dx%d board", pattern, width, height);
        return buildGameState();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void addBorderWalls() {
        for (int x = 0; x < width; x++) {
            hWalls[x][0] = 1;
            hWalls[x][height] = 1;
        }
        for (int y = 0; y < height; y++) {
            vWalls[0][y] = 1;
            vWalls[width][y] = 1;
        }
    }

    private void addCenterCarree() {
        int cx = width / 2 - 1;
        int cy = height / 2 - 1;
        hWalls[cx][cy] = 1;     hWalls[cx + 1][cy] = 1;
        hWalls[cx][cy + 2] = 1; hWalls[cx + 1][cy + 2] = 1;
        vWalls[cx][cy] = 1;     vWalls[cx][cy + 1] = 1;
        vWalls[cx + 2][cy] = 1; vWalls[cx + 2][cy + 1] = 1;
    }

    private boolean inCarree(int x, int y) {
        int cx = width / 2 - 1;
        int cy = height / 2 - 1;
        return x >= cx && x <= cx + 1 && y >= cy && y <= cy + 1;
    }

    private boolean safeH(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y <= height && hWalls[x][y] == 0;
    }

    private boolean safeV(int x, int y) {
        return x >= 0 && x <= width && y >= 0 && y < height && vWalls[x][y] == 0;
    }

    private void placeH(int x, int y) {
        if (safeH(x, y) && !inCarree(x, y)) hWalls[x][y] = 1;
    }

    private void placeV(int x, int y) {
        if (safeV(x, y) && !inCarree(x, y)) vWalls[x][y] = 1;
    }

    private int rng(int min, int max) {
        if (min >= max) return min;
        return min + rand.nextInt(max - min + 1);
    }

    private GameState buildGameState() {
        GameState state = new GameState(width, height);
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                if (hWalls[x][y] == 1 && x < width) {
                    state.addHorizontalWall(x, y);
                }
                if (vWalls[x][y] == 1 && y < height) {
                    state.addVerticalWall(x, y);
                }
            }
        }
        return state;
    }

    // ── Border Stubs (perpendicular walls on outer edges) ─────────────

    /**
     * Remove existing border stubs and generate new ones on a GameState.
     * Places 2-3 perpendicular walls per side, not too close to corners (min 2 cells away).
     * Top/bottom borders get vertical stubs, left/right borders get horizontal stubs.
     */
    public static void generateBorderStubs(GameState state) {
        int w = state.getWidth();
        int h = state.getHeight();
        Random r = new Random();
        int minCornerDist = 2;

        // Remove existing border stubs (perpendicular walls touching the border)
        List<GameElement> toRemove = new ArrayList<>();
        for (GameElement el : state.getGameElements()) {
            if (el.getType() == GameElement.TYPE_VERTICAL_WALL) {
                // Vertical wall on top border (y=0) or bottom border (y=h-1)
                if (el.getY() == 0 || el.getY() == h - 1) {
                    // Don't remove corner border walls (x=0 or x=w)
                    if (el.getX() > 0 && el.getX() < w) {
                        toRemove.add(el);
                    }
                }
            } else if (el.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                // Horizontal wall on left border (x=0) or right border (x=w-1)
                if (el.getX() == 0 || el.getX() == w - 1) {
                    // Don't remove corner border walls (y=0 or y=h)
                    if (el.getY() > 0 && el.getY() < h) {
                        toRemove.add(el);
                    }
                }
            }
        }
        // Clean up board[][] for removed stubs
        for (GameElement el : toRemove) {
            state.setCellType(el.getX(), el.getY(), 0);
        }
        state.getGameElements().removeAll(toRemove);

        Timber.d("[BORDER_STUBS] Removed %d old border stubs from %dx%d board", toRemove.size(), w, h);

        // Generate new stubs per side
        generateStubsForSide(state, r, w, h, minCornerDist);
    }

    private static void generateStubsForSide(GameState state, Random r, int w, int h, int minDist) {
        // Top border: vertical walls at y=0, x varies
        placeStubsOnEdge(state, r, minDist, w - 1 - minDist, true, 0, w, h);
        // Bottom border: vertical walls at y=h, x varies
        placeStubsOnEdge(state, r, minDist, w - 1 - minDist, true, h, w, h);
        // Left border: horizontal walls at x=0, y varies
        placeStubsOnEdge(state, r, minDist, h - 1 - minDist, false, 0, w, h);
        // Right border: horizontal walls at x=w, y varies
        placeStubsOnEdge(state, r, minDist, h - 1 - minDist, false, w, w, h);
    }

    private static void placeStubsOnEdge(GameState state, Random r, int rangeMin, int rangeMax,
                                           boolean isHorizontalEdge, int fixedCoord, int boardW, int boardH) {
        if (rangeMin >= rangeMax) return;
        int count = 2 + r.nextInt(2); // 2 or 3 stubs
        int minSpacing = 2;
        ArrayList<Integer> positions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 30; attempt++) {
                int pos = rangeMin + r.nextInt(rangeMax - rangeMin + 1);
                boolean tooClose = false;
                for (int existing : positions) {
                    if (Math.abs(pos - existing) < minSpacing) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    positions.add(pos);
                    break;
                }
            }
        }

        for (int pos : positions) {
            if (isHorizontalEdge) {
                // Horizontal edge (top/bottom) → place vertical wall stub
                state.addVerticalWall(pos, fixedCoord == 0 ? 0 : boardH - 1);
            } else {
                // Vertical edge (left/right) → place horizontal wall stub
                state.addHorizontalWall(fixedCoord == 0 ? 0 : boardW - 1, pos);
            }
        }

        Timber.d("[BORDER_STUBS] Placed %d stubs on %s edge at %d",
                positions.size(), isHorizontalEdge ? "horizontal" : "vertical", fixedCoord);
    }

    // ── Pattern 0: Classic (L-shaped wall pairs in quadrants) ───────────

    private void generateClassic() {
        int wallsPerQuadrant = width / 4;
        int[][] quadrantBounds = {
            {1, 1, width / 2 - 1, height / 2 - 1},
            {width / 2, 1, width - 2, height / 2 - 1},
            {1, height / 2, width / 2 - 1, height - 2},
            {width / 2, height / 2, width - 2, height - 2}
        };

        for (int[] bounds : quadrantBounds) {
            for (int i = 0; i < wallsPerQuadrant; i++) {
                for (int attempt = 0; attempt < 50; attempt++) {
                    int x = rng(bounds[0], bounds[2]);
                    int y = rng(bounds[1], bounds[3]);
                    if (inCarree(x, y)) continue;
                    if (hWalls[x][y] == 1) continue;

                    int vx = x + rng(0, 1);
                    int vy = y - rng(0, 1);
                    if (safeH(x, y) && safeV(vx, vy) && !inCarree(vx, vy)) {
                        hWalls[x][y] = 1;
                        vWalls[vx][vy] = 1;
                        break;
                    }
                }
            }
        }
    }

    // ── Pattern 1: Spiral ───────────────────────────────────────────────

    private void generateSpiral() {
        int left = 2, right = width - 3, top = 2, bottom = height - 3;
        boolean horizontal = true;
        int direction = 0; // 0=right, 1=down, 2=left, 3=up

        while (left < right && top < bottom) {
            int gapPos;
            switch (direction) {
                case 0: // top wall going right
                    gapPos = rng(left + 1, right - 1);
                    for (int x = left; x <= right; x++) {
                        if (x != gapPos) placeH(x, top);
                    }
                    top += 2;
                    break;
                case 1: // right wall going down
                    gapPos = rng(top + 1, bottom - 1);
                    for (int y = top; y <= bottom; y++) {
                        if (y != gapPos) placeV(right, y);
                    }
                    right -= 2;
                    break;
                case 2: // bottom wall going left
                    gapPos = rng(left + 1, right - 1);
                    for (int x = left; x <= right; x++) {
                        if (x != gapPos) placeH(x, bottom);
                    }
                    bottom -= 2;
                    break;
                case 3: // left wall going up
                    gapPos = rng(top + 1, bottom - 1);
                    for (int y = top; y <= bottom; y++) {
                        if (y != gapPos) placeV(left, y);
                    }
                    left += 2;
                    break;
            }
            direction = (direction + 1) % 4;
        }
    }

    // ── Pattern 2: Rooms ────────────────────────────────────────────────

    private void generateRooms() {
        // Divide board into a grid of rooms with doorways
        int roomsX = Math.max(2, width / 5);
        int roomsY = Math.max(2, height / 5);
        int roomW = width / roomsX;
        int roomH = height / roomsY;

        for (int rx = 1; rx < roomsX; rx++) {
            int wallX = rx * roomW;
            int doorY = rng(0, roomsY - 1) * roomH + rng(1, roomH - 1);
            for (int y = 1; y < height - 1; y++) {
                if (Math.abs(y - doorY) > 0) {
                    placeV(wallX, y);
                }
            }
        }

        for (int ry = 1; ry < roomsY; ry++) {
            int wallY = ry * roomH;
            int doorX = rng(0, roomsX - 1) * roomW + rng(1, roomW - 1);
            for (int x = 1; x < width - 1; x++) {
                if (Math.abs(x - doorX) > 0) {
                    placeH(x, wallY);
                }
            }
        }

        // Add a few random L-walls inside rooms for interest
        for (int i = 0; i < width / 2; i++) {
            int x = rng(2, width - 3);
            int y = rng(2, height - 3);
            if (!inCarree(x, y) && safeH(x, y)) {
                placeH(x, y);
                if (rand.nextBoolean()) placeV(x, y);
                else placeV(x + 1, y);
            }
        }
    }

    // ── Pattern 3: Maze ─────────────────────────────────────────────────

    private void generateMaze() {
        // Simple recursive-backtracker-inspired maze with wider passages
        int step = 2;
        boolean[][] visited = new boolean[width][height];
        ArrayList<int[]> stack = new ArrayList<>();

        int startX = 1, startY = 1;
        visited[startX][startY] = true;
        stack.add(new int[]{startX, startY});

        int[][] dirs = {{0, -step}, {step, 0}, {0, step}, {-step, 0}};

        while (!stack.isEmpty()) {
            int[] current = stack.get(stack.size() - 1);
            int cx = current[0], cy = current[1];

            // Find unvisited neighbors
            ArrayList<int[]> neighbors = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (nx >= 1 && nx < width - 1 && ny >= 1 && ny < height - 1 && !visited[nx][ny]) {
                    neighbors.add(new int[]{nx, ny, d[0], d[1]});
                }
            }

            if (neighbors.isEmpty()) {
                stack.remove(stack.size() - 1);
            } else {
                int[] chosen = neighbors.get(rand.nextInt(neighbors.size()));
                int nx = chosen[0], ny = chosen[1], dx = chosen[2], dy = chosen[3];
                visited[nx][ny] = true;

                // Place wall segments on the sides of the passage (not blocking it)
                if (dx != 0) {
                    // Moving horizontally — place vertical walls above/below passage
                    int midX = cx + dx / 2;
                    // Randomly place a wall segment perpendicular to movement
                    if (rand.nextBoolean()) placeH(midX, cy);
                } else {
                    // Moving vertically — place horizontal walls left/right of passage
                    int midY = cy + dy / 2;
                    if (rand.nextBoolean()) placeV(cx, midY);
                }

                stack.add(new int[]{nx, ny});
            }
        }

        // Add extra L-walls for complexity
        for (int i = 0; i < width / 3; i++) {
            int x = rng(2, width - 3);
            int y = rng(2, height - 3);
            if (!inCarree(x, y)) {
                placeH(x, y);
                placeV(x + rng(0, 1), y - rng(0, 1));
            }
        }
    }

    // ── Pattern 4: Diagonal ─────────────────────────────────────────────

    private void generateDiagonal() {
        // Create diagonal lines of walls from corners
        int numLines = rng(3, 6);

        for (int line = 0; line < numLines; line++) {
            int startX, startY, dx, dy;
            switch (line % 4) {
                case 0: startX = rng(1, 3); startY = rng(1, 3); dx = 1; dy = 1; break;
                case 1: startX = width - rng(2, 4); startY = rng(1, 3); dx = -1; dy = 1; break;
                case 2: startX = rng(1, 3); startY = height - rng(2, 4); dx = 1; dy = -1; break;
                default: startX = width - rng(2, 4); startY = height - rng(2, 4); dx = -1; dy = -1; break;
            }

            int x = startX, y = startY;
            int len = rng(3, Math.min(width, height) / 2);
            for (int i = 0; i < len; i++) {
                if (x < 1 || x >= width - 1 || y < 1 || y >= height - 1) break;
                if (!inCarree(x, y)) {
                    // Alternate between horizontal and vertical to create staircase
                    if (i % 2 == 0) placeH(x, y);
                    else placeV(x, y);
                }
                x += dx;
                y += dy;
            }
        }

        // Fill with some random L-walls
        for (int i = 0; i < width / 3; i++) {
            int x = rng(2, width - 3);
            int y = rng(2, height - 3);
            if (!inCarree(x, y) && safeH(x, y)) {
                placeH(x, y);
                placeV(x + rng(0, 1), y);
            }
        }
    }

    // ── Pattern 5: Symmetric (4-fold mirror) ────────────────────────────

    private void generateSymmetric() {
        int halfW = width / 2;
        int halfH = height / 2;
        int wallCount = rng(width / 2, width);

        for (int i = 0; i < wallCount; i++) {
            int x = rng(1, halfW - 1);
            int y = rng(1, halfH - 1);
            boolean horiz = rand.nextBoolean();

            if (horiz) {
                placeH(x, y);
                placeH(width - 1 - x, y);
                placeH(x, height - y);
                placeH(width - 1 - x, height - y);
            } else {
                placeV(x, y);
                placeV(width - x, y);
                placeV(x, height - 1 - y);
                placeV(width - x, height - 1 - y);
            }
        }
    }

    // ── Pattern 6: Corridors ────────────────────────────────────────────

    private void generateCorridors() {
        // Long horizontal and vertical wall lines with gaps
        int numCorridors = rng(3, 6);

        for (int c = 0; c < numCorridors; c++) {
            boolean horiz = rand.nextBoolean();
            if (horiz) {
                int y = rng(2, height - 3);
                int gapX = rng(2, width - 3);
                int gapWidth = rng(1, 3);
                for (int x = 1; x < width - 1; x++) {
                    if (x < gapX || x >= gapX + gapWidth) {
                        placeH(x, y);
                    }
                }
            } else {
                int x = rng(2, width - 3);
                int gapY = rng(2, height - 3);
                int gapHeight = rng(1, 3);
                for (int y = 1; y < height - 1; y++) {
                    if (y < gapY || y >= gapY + gapHeight) {
                        placeV(x, y);
                    }
                }
            }
        }

        // Add some perpendicular stubs
        for (int i = 0; i < width / 3; i++) {
            int x = rng(2, width - 3);
            int y = rng(2, height - 3);
            if (!inCarree(x, y)) {
                if (rand.nextBoolean()) placeH(x, y);
                else placeV(x, y);
            }
        }
    }

    // ── Pattern 7: Islands ──────────────────────────────────────────────

    private void generateIslands() {
        // Small isolated wall clusters scattered across the board
        int numIslands = rng(4, 8);

        for (int i = 0; i < numIslands; i++) {
            int cx = rng(2, width - 3);
            int cy = rng(2, height - 3);
            if (inCarree(cx, cy)) continue;

            int size = rng(1, 3);
            // Create a small cluster of walls around (cx, cy)
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    int wx = cx + dx;
                    int wy = cy + dy;
                    if (wx < width - 1 && wy < height - 1 && !inCarree(wx, wy)) {
                        if (rand.nextBoolean()) placeH(wx, wy);
                        if (rand.nextBoolean()) placeV(wx, wy);
                    }
                }
            }
        }
    }

    // ── Pattern 8: Border Heavy ─────────────────────────────────────────

    private void generateBorderHeavy() {
        // Many walls near the edges, fewer in the center
        int margin = Math.max(2, width / 4);

        for (int i = 0; i < width * 2; i++) {
            int x, y;
            // 70% chance near border, 30% chance anywhere
            if (rand.nextInt(10) < 7) {
                // Near a border
                switch (rand.nextInt(4)) {
                    case 0: x = rng(1, margin); y = rng(1, height - 2); break;
                    case 1: x = rng(width - margin - 1, width - 2); y = rng(1, height - 2); break;
                    case 2: x = rng(1, width - 2); y = rng(1, margin); break;
                    default: x = rng(1, width - 2); y = rng(height - margin - 1, height - 2); break;
                }
            } else {
                x = rng(1, width - 2);
                y = rng(1, height - 2);
            }

            if (!inCarree(x, y)) {
                if (rand.nextBoolean()) placeH(x, y);
                else placeV(x, y);
            }
        }
    }

    // ── Pattern 9: Scatter (random walls everywhere) ────────────────────

    private void generateScatter() {
        int wallCount = rng(width, width * 2);

        for (int i = 0; i < wallCount; i++) {
            int x = rng(1, width - 2);
            int y = rng(1, height - 2);
            if (!inCarree(x, y)) {
                if (rand.nextBoolean()) placeH(x, y);
                else placeV(x, y);
            }
        }
    }
}

# Wall Rendering Unification Concept

## Problem Statement

Currently, the Roboyard codebase has two separate systems for handling walls:

1. **Visual Rendering (GameGridView.java)**: Handles drawing walls on the screen for the player to see
2. **Logical Representation (RRGetMap.java)**: Creates a virtual representation of walls for the solver

This separation creates potential inconsistencies, particularly with scaling issues where visual walls appear to extend beyond the board edge when the game board is scaled down.

## Proposed Solution

### 1. Unified Wall Model

Create a single source of truth for wall data with these components:

```
┌─────────────────────┐
│   WallModel Class   │
├─────────────────────┤
│ - Wall positions    │
│ - Wall types        │
│ - Board dimensions  │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  WallRenderer Class │
├─────────────────────┤
│ - Rendering logic   │
│ - Scaling handling  │
└─────────────────────┘
```

### 2. Implementation Plan

1. **Create a WallModel class**:
   - Responsible for storing wall data (position, type)
   - Provides methods to query walls at specific positions
   - Handles edge cases consistently

2. **Create a WallRenderer class**:
   - Takes a WallModel as input
   - Handles visual rendering with proper scaling
   - Ensures walls stay within board boundaries when scaled

3. **Modify GameGridView**:
   - Use WallRenderer for drawing
   - Pass appropriate scaling information

4. **Modify RRGetMap**:
   - Use the same WallModel for creating the solver's virtual world
   - Eliminate duplicate wall processing logic

## Implementation Details

### Wall Class

```java
public class Wall {
    private final int x;
    private final int y;
    private final WallType type;
    
    public Wall(int x, int y, WallType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    // Getters
}

public enum WallType {
    HORIZONTAL,
    VERTICAL
}
```

### WallModel Class

```java
public class WallModel {
    private final List<Wall> walls = new ArrayList<>();
    private final int boardWidth;
    private final int boardHeight;
    
    public WallModel(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
    }
    
    public void addWall(int x, int y, WallType type) {
        walls.add(new Wall(x, y, type));
    }
    
    public List<Wall> getWalls() {
        return Collections.unmodifiableList(walls);
    }
    
    public boolean hasWallAt(int x, int y, WallType type) {
        // Logic to check if a wall exists at position
    }
    
    // Methods to convert between game elements and wall model
    public static WallModel fromGameElements(List<GameElement> elements, int width, int height) {
        // Conversion logic
    }
}
```

### WallRenderer Class

```java
public class WallRenderer {
    private final WallModel model;
    private final float cellSize;
    private final Drawable horizontalWallDrawable;
    private final Drawable verticalWallDrawable;
    private static float WALL_THICKNESS_FACTOR = 0.675f;
    private static float WALL_OFFSET_FACTOR = 0.3f;
    
    public WallRenderer(WallModel model, float cellSize, 
                        Drawable horizontalWall, Drawable verticalWall) {
        this.model = model;
        this.cellSize = cellSize;
        this.horizontalWallDrawable = horizontalWall;
        this.verticalWallDrawable = verticalWall;
    }
    
    public void drawWalls(Canvas canvas, float offsetX, float offsetY) {
        for (Wall wall : model.getWalls()) {
            if (wall.getType() == WallType.HORIZONTAL) {
                drawHorizontalWall(canvas, wall, offsetX, offsetY);
            } else {
                drawVerticalWall(canvas, wall, offsetX, offsetY);
            }
        }
    }
    
    private void drawHorizontalWall(Canvas canvas, Wall wall, float offsetX, float offsetY) {
        // Drawing logic with proper scaling
        // Ensure wall stays within board boundaries
    }
    
    private void drawVerticalWall(Canvas canvas, Wall wall, float offsetX, float offsetY) {
        // Drawing logic with proper scaling
        // Ensure wall stays within board boundaries
    }
}
```

## Integration

### In GameGridView

```java
@Override
protected void onDraw(Canvas canvas) {
    // Create wall model from game state
    WallModel wallModel = WallModel.fromGameElements(
        gameStateManager.getCurrentState().getGameElements(),
        gridWidth, gridHeight
    );
    
    // Create renderer with current cell size
    WallRenderer renderer = new WallRenderer(
        wallModel, cellSize, wallHorizontal, wallVertical
    );
    
    // Draw background and grid
    // ...
    
    // Draw walls using renderer
    renderer.drawWalls(canvas, offsetX, offsetY);
    
    // Draw other game elements
    // ...
}
```

### In RRGetMap

```java
public static Board createDDWorld(ArrayList<GridElement> gridElements, RRPiece[] pieces) {
    // Create board
    // ...
    
    // Create wall model from grid elements
    WallModel wallModel = WallModel.fromGameElements(gridElements, boardWidth, boardHeight);
    
    // Apply walls to board using wall model
    for (Wall wall : wallModel.getWalls()) {
        int position = wall.getY() * board.width + wall.getX();
        
        if (wall.getType() == WallType.HORIZONTAL) {
            board.setWall(position, "N", true);
        } else {
            board.setWall(position, "W", true);
        }
    }
    
    // Handle other elements
    // ...
}
```

## Benefits of This Approach

1. **Single Source of Truth**: Wall data is defined in one place
2. **Consistent Rendering**: Visual representation matches logical representation
3. **Proper Scaling**: Walls will scale correctly with the board
4. **Easier Maintenance**: Changes to wall behavior only need to be made in one place
5. **Better Separation of Concerns**: Model (data) is separated from rendering (presentation)

## Implementation Timeline

1. Create Wall and WallType classes
2. Create WallModel class
3. Create WallRenderer class
4. Integrate with GameGridView
5. Integrate with RRGetMap
6. Test and verify scaling behavior

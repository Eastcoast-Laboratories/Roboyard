# Wall Data Format Difference: Main Game vs ComposeApp

## Main Game Data Format

Walls are stored as GameElements.

Each wall is a separate GameElement with:
- `type = TYPE_HORIZONTAL_WALL` or `TYPE_VERTICAL_WALL`
- `x` and `y` position

Example: A horizontal wall at (5, 7) is a GameElement with:
- `type = TYPE_HORIZONTAL_WALL`
- `x = 5`
- `y = 7`

Walls are independent of cells - they are stored "between" cells.

## ComposeApp Data Format

Walls are stored as directions in the Board object.

`board.walls[direction][position]` is a 2D array where:
- `direction` can be 0=NORTH, 1=EAST, 2=SOUTH, 3=WEST
- `position` is the cell index (x + y * width)

Example: A SOUTH wall at position 100 is:
- `board.walls[2][100] = true`

Walls are bound to cells - each cell can have walls in 4 directions.

## The Difference

**Main Game:** Walls are independent objects with coordinates.

**ComposeApp:** Walls are properties of cells (hasWall in direction X).

## Serialization Format

The Main Game serializes walls in the format:
- `hX,Y;` - horizontal wall at (x, y)
- `vX,Y;` - vertical wall at (x, y)

According to the Main Game's movement validation:
- `hX,Y;` blocks movement from (x, y) to (x, y+1) (SOUTH)
- `vX,Y;` blocks movement from (x-1, y) to (x, y) (EAST)

## Mapping Between Formats

When serializing from ComposeApp to Main Game format:
- SOUTH wall at (x, y) → `hX,Y;`
- EAST wall at (x, y) → `vX,Y;`

When deserializing from Main Game format to ComposeApp:
- `hX,Y;` → SOUTH wall at (x, y)
- `vX,Y;` → EAST wall at (x, y)

## Important Note

In the ComposeApp, walls are stored per cell. However, when a wall is placed between two cells, it should be stored as a wall in both directions for both cells to maintain consistency with the Main Game's behavior.

For example:
- A horizontal wall between (x, y) and (x, y+1) should be:
  - SOUTH wall at (x, y)
  - NORTH wall at (x, y+1)
- A vertical wall between (x, y) and (x+1, y) should be:
  - EAST wall at (x, y)
  - WEST wall at (x+1, y)

This is how the `gridElementsToBoard` function works in the ComposeApp when converting from MapGenerator output.

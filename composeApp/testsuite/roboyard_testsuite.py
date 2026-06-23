#!/usr/bin/env python3
"""
Roboyard-specific Test Suite
Functions specific to Roboyard app testing (robot movement, log parsing, etc.)
"""

from testsuite.test_suite import WINDOW_BOUNDS, drag_from_to

def move_robot(robot_color, direction, robot_positions, board_width, board_height, duration=0.5):
    """Move a robot by dragging from its current position to target position.
    
    Parameters:
    - robot_color: Robot color index (0=blue, 1=green, 2=red, 3=yellow)
    - direction: Direction to move ('UP', 'DOWN', 'LEFT', 'RIGHT')
    - robot_positions: List of (x, y) tuples for all robots
    - board_width: Board width in cells
    - board_height: Board height in cells
    - duration: Drag duration in seconds
    
    Returns True if move was executed successfully
    """
    if robot_color >= len(robot_positions):
        print(f"[ROBOYARD_TEST_SUITE] ERROR: Robot color {robot_color} out of range (have {len(robot_positions)} robots)")
        return False
    
    # Get robot position
    robot_x, robot_y = robot_positions[robot_color]
    
    # Calculate target position based on direction
    target_x, target_y = robot_x, robot_y
    if direction == 'UP':
        target_y -= 1
    elif direction == 'DOWN':
        target_y += 1
    elif direction == 'LEFT':
        target_x -= 1
    elif direction == 'RIGHT':
        target_x += 1
    else:
        print(f"[ROBOYARD_TEST_SUITE] ERROR: Invalid direction '{direction}'")
        return False
    
    # Check if target is within board bounds
    if target_x < 0 or target_x >= board_width or target_y < 0 or target_y >= board_height:
        print(f"[ROBOYARD_TEST_SUITE] ERROR: Target position ({target_x}, {target_y}) out of board bounds")
        return False
    
    # Convert board coordinates to screen coordinates
    # Board is displayed in the center of the window, need to calculate cell size
    if WINDOW_BOUNDS is None:
        print("[ROBOYARD_TEST_SUITE] ERROR: Window bounds not set")
        return False
    
    # Calculate board display area (assume board takes up most of the window, with some margins)
    margin_x = WINDOW_BOUNDS['width'] * 0.05  # 5% margin
    margin_y = WINDOW_BOUNDS['height'] * 0.1  # 10% margin
    board_display_width = WINDOW_BOUNDS['width'] - 2 * margin_x
    board_display_height = WINDOW_BOUNDS['height'] - 2 * margin_y
    
    cell_width = board_display_width / board_width
    cell_height = board_display_height / board_height
    
    # Calculate screen coordinates (center of each cell)
    screen_start_x = WINDOW_BOUNDS['x'] + margin_x
    screen_start_y = WINDOW_BOUNDS['y'] + margin_y
    
    from_x = screen_start_x + robot_x * cell_width + cell_width / 2
    from_y = screen_start_y + robot_y * cell_height + cell_height / 2
    to_x = screen_start_x + target_x * cell_width + cell_width / 2
    to_y = screen_start_y + target_y * cell_height + cell_height / 2
    
    print(f"[ROBOYARD_TEST_SUITE] Moving robot {robot_color} {direction}: from ({robot_x}, {robot_y}) to ({target_x}, {target_y})")
    print(f"[ROBOYARD_TEST_SUITE] Screen coordinates: from ({from_x:.0f}, {from_y:.0f}) to ({to_x:.0f}, {to_y:.0f})")
    
    # Execute drag
    return drag_from_to(int(from_x), int(from_y), int(to_x), int(to_y), f"Robot {robot_color} {direction}", duration)

def parse_solution_from_log(log_file=None):
    """Parse solver solution from app log file.
    
    Returns list of (robot_color, direction) tuples, or None if not found.
    Format: "solution: size=N ***** [moves] *****"
    Moves format: "bN" (blue north), "rE" (red east), etc.
    """
    from testsuite.test_suite import APP_LOG_FILE
    if log_file is None:
        log_file = APP_LOG_FILE
    try:
        with open(log_file, 'r') as f:
            content = f.read()
        
        # Find solution line
        import re
        solution_pattern = r"solution: size=\d+ \*{5} (.+?) \*{5}"
        match = re.search(solution_pattern, content)
        
        if not match:
            print("[ROBOYARD_TEST_SUITE] ERROR: Solution not found in log")
            return None
        
        moves_str = match.group(1)
        print(f"[ROBOYARD_TEST_SUITE] Found solution moves: {moves_str}")
        
        # Parse moves
        moves = []
        # Move format: 2 characters (color + direction)
        for i in range(0, len(moves_str), 2):
            if i + 1 >= len(moves_str):
                break
            
            color_char = moves_str[i].lower()
            direction_char = moves_str[i + 1].upper()
            
            # Map color char to robot color index (matches LevelLoader.parseColorChar)
            color_map = {'b': 0, 'g': 1, 'r': 2, 'y': 3, 's': 4}
            robot_color = color_map.get(color_char, 0)
            
            # Map direction char to direction string
            direction_map = {'N': 'UP', 'S': 'DOWN', 'W': 'LEFT', 'E': 'RIGHT'}
            direction = direction_map.get(direction_char, 'UP')
            
            moves.append((robot_color, direction))
            print(f"[ROBOYARD_TEST_SUITE] Parsed move: robot {robot_color} ({color_char}) {direction} ({direction_char})")
        
        print(f"[ROBOYARD_TEST_SUITE] Total moves parsed: {len(moves)}")
        return moves
        
    except Exception as e:
        print(f"[ROBOYARD_TEST_SUITE] ERROR parsing solution from log: {e}")
        return None

def parse_robot_positions_from_log(log_file=None):
    """Parse robot start positions from app log file.
    
    Returns list of (x, y) tuples for all robots, or None if not found.
    Format: "Robot positions: [x1,y1, x2,y2, ...]"
    """
    from testsuite.test_suite import APP_LOG_FILE
    if log_file is None:
        log_file = APP_LOG_FILE
    try:
        with open(log_file, 'r') as f:
            content = f.read()
        
        # Find robot positions line
        import re
        positions_pattern = r"Robot positions: \[(.+?)\]"
        match = re.search(positions_pattern, content)
        
        if not match:
            print("[ROBOYARD_TEST_SUITE] ERROR: Robot positions not found in log")
            return None
        
        positions_str = match.group(1)
        print(f"[ROBOYARD_TEST_SUITE] Found robot positions: {positions_str}")
        
        # Parse positions
        positions = []
        pos_pairs = positions_str.split(', ')
        for i in range(0, len(pos_pairs), 2):
            if i + 1 >= len(pos_pairs):
                break
            x = int(pos_pairs[i])
            y = int(pos_pairs[i + 1])
            positions.append((x, y))
        
        print(f"[ROBOYARD_TEST_SUITE] Total robot positions parsed: {len(positions)}")
        return positions
        
    except Exception as e:
        print(f"[ROBOYARD_TEST_SUITE] ERROR parsing robot positions from log: {e}")
        return None

def parse_preferences_from_log(log_file=None):
    """Parse preferences from app log file (DRY - like the rest of the app).
    
    Returns dict with board_width, board_height, robot_count, target_colors, or None if not found.
    Format: "Cached values loaded - robotCount: X, targetColors: Y, difficulty: Z, boardSize: WxH"
    """
    from testsuite.test_suite import APP_LOG_FILE
    if log_file is None:
        log_file = APP_LOG_FILE
    try:
        with open(log_file, 'r') as f:
            content = f.read()
        
        # Find preferences line
        import re
        prefs_pattern = r"Cached values loaded - robotCount: (\d+), targetColors: (\d+), difficulty: (\d+), boardSize: (\d+)x(\d+)"
        match = re.search(prefs_pattern, content)
        
        if not match:
            print("[ROBOYARD_TEST_SUITE] ERROR: Preferences not found in log")
            return None
        
        robot_count = int(match.group(1))
        target_colors = int(match.group(2))
        difficulty = int(match.group(3))
        board_width = int(match.group(4))
        board_height = int(match.group(5))
        
        print(f"[ROBOYARD_TEST_SUITE] Found preferences: robotCount={robot_count}, targetColors={target_colors}, difficulty={difficulty}, boardSize={board_width}x{board_height}")
        
        return {
            'board_width': board_width,
            'board_height': board_height,
            'robot_count': robot_count,
            'target_colors': target_colors,
            'difficulty': difficulty
        }
        
    except Exception as e:
        print(f"[ROBOYARD_TEST_SUITE] ERROR parsing preferences from log: {e}")
        return None

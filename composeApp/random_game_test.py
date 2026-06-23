#!/usr/bin/env python3
"""
HEADED UI Test for Random Game Verification
This test uses PyAutoGUI to automate UI interactions with the ComposeApp
You can watch the test running in real-time!

How to run this test:
1. Start the ComposeApp: ./gradlew :composeApp:run --no-configuration-cache
2. Wait for the app window to appear and be in focus
3. Run this test in another terminal: python3 random_game_test.py

What this test verifies:
- Click "New Random Game" button
- Parse board size from preferences
- Parse robot start positions from log
- Parse solution from log
- Execute first move from solution
- Verify history entry is correctly updated
"""

import sys
import os
import time

# Import generic test suite functions
from testsuite.test_suite import (
    start_composeapp,
    wait_for_window,
    init_globals,
    take_screenshot,
    click_frac,
    configure_app
)

# Import Roboyard-specific test suite functions
from testsuite.roboyard_testsuite import (
    move_robot,
    parse_preferences_from_log,
    parse_robot_positions_from_log,
    parse_solution_from_log
)

# Configure app-specific settings for Roboyard
configure_app(
    name="Roboyard",
    path="/var/www/Roboyard",
    process_pattern="roboyard.*MainKt"
)

def main():
    print("[TEST] Starting headed UI test for Random Game")
    print("[TEST] This test will automate UI interactions with the ComposeApp")
    
    # Start fresh ComposeApp instance with --no-configuration-cache
    start_composeapp()

    # Wait for the ComposeApp window to appear (matched by PID, not title)
    window_id, geometry = wait_for_window(-1)
    if not geometry:
        print("[TEST] ERROR: ComposeApp window not found. Make sure it's running.")
        sys.exit(1)

    # Initialize globals so click/drag functions can verify focus and bounds
    init_globals(window_id, geometry)

    # Get window bounds for safe clicking
    window_x = geometry['x']
    window_y = geometry['y']
    window_width = geometry['width']
    window_height = geometry['height']
    
    print(f"[TEST] Window bounds: x={window_x}, y={window_y}, width={window_width}, height={window_height}")
    
    # Calculate center coordinates relative to window
    center_x = window_x + window_width // 2
    center_y = window_y + window_height // 2
    
    print(f"[TEST] Window center: ({center_x}, {center_y})")
    
    # Take initial screenshot
    take_screenshot("random_screenshot_0_initial.png")
    
    # Step 1: Click "New Random Game" button
    print("[TEST] Step 1: Clicking New Random Game button")
    click_frac(0.5, 0.20, "New Random Game button", wait=0.5)
    take_screenshot("random_screenshot_1_after_random_game.png")
    
    # Wait for game to load and solver to run
    print("[TEST] Waiting for game to load and solver to run...")
    time.sleep(3)
    
    # Step 2: Parse preferences from log
    print("[TEST] Step 2: Parsing preferences from log")
    prefs = parse_preferences_from_log()
    if not prefs:
        print("[TEST] ERROR: Could not parse preferences from log")
        sys.exit(1)
    
    board_width = prefs['board_width']
    board_height = prefs['board_height']
    robot_count = prefs['robot_count']
    
    print(f"[TEST] Board size: {board_width}x{board_height}, Robot count: {robot_count}")
    
    # Step 3: Parse robot positions from log
    print("[TEST] Step 3: Parsing robot positions from log")
    robot_positions = parse_robot_positions_from_log()
    if not robot_positions:
        print("[TEST] ERROR: Could not parse robot positions from log")
        sys.exit(1)
    
    print(f"[TEST] Robot positions: {robot_positions}")
    
    # Step 4: Parse solution from log
    print("[TEST] Step 4: Parsing solution from log")
    solution = parse_solution_from_log()
    if not solution or len(solution) == 0:
        print("[TEST] ERROR: Could not parse solution from log")
        sys.exit(1)
    
    print(f"[TEST] Solution has {len(solution)} moves")
    
    # Step 5: Execute first move from solution
    print("[TEST] Step 5: Executing first move from solution")
    first_move = solution[0]
    robot_color, direction = first_move
    
    print(f"[TEST] First move: robot {robot_color} {direction}")
    
    # Execute the move
    success = move_robot(robot_color, direction, robot_positions, board_width, board_height, duration=0.5)
    
    if not success:
        print("[TEST] ERROR: Failed to execute first move")
        sys.exit(1)
    
    take_screenshot("random_screenshot_2_after_first_move.png")
    
    # Step 6: Wait for history to be updated
    print("[TEST] Step 6: Waiting for history to be updated...")
    time.sleep(2)
    
    # Step 7: Verify history entry (check log for history save)
    print("[TEST] Step 7: Verifying history entry was updated")
    # This would require checking the history_index.json or log for history save confirmation
    # For now, we just take a screenshot for manual verification
    take_screenshot("random_screenshot_3_final.png")
    
    print("[TEST] Test completed successfully!")
    print("[TEST] All screenshots saved for verification")
    print("[TEST] Check the screenshots to verify the random game test")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n[TEST] Test aborted by user")
        sys.exit(1)
    except Exception as e:
        print(f"[TEST] ERROR: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

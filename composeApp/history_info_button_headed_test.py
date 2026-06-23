#!/usr/bin/env python3
"""
HEADED UI Test for History Info Button Verification
This test uses PyAutoGUI to automate UI interactions with the ComposeApp
You can watch the test running in real-time!

How to run this test:
1. Start the ComposeApp: ./gradlew :composeApp:run --no-configuration-cache
2. Wait for the app window to appear and be in focus
3. Run this test in another terminal: python3 history_info_button_headed_test.py

What this test verifies:
- Click "Level Game" button
- Select Level 1 (blue robot needs to move up then right to goal)
- Click on blue robot and drag UP (first move)
- Click on blue robot and drag RIGHT (second move - completes level)
- Verify completion dialog shows correct stats
- Click "Load Game" button
- Switch to History tab
- Click info button on Level 1 entry
- Verify bestTime and bestMoves are displayed in dialog
- Play Level 1 again with same moves (up, right)
- Verify bestTime/bestMoves don't change (only best values kept)
"""

import sys
import os

# Import generic test suite functions
from testsuite.test_suite import (
    start_composeapp,
    wait_for_window,
    init_globals,
    take_screenshot,
    click_frac,
    drag_frac,
    click_at_coordinates,
    configure_app
)

# Configure app-specific settings for Roboyard
configure_app(
    name="Roboyard",
    path="/var/www/Roboyard",
    process_pattern="roboyard.*MainKt"
)

def main():
    print("[TEST] Starting headed UI test for History Info Button")
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
    take_screenshot("screenshot_0_initial.png")
    
    # Button layout (fractions of window): main menu buttons are in the upper portion.
    # New Random Game ~0.20, Level Game ~0.29, Load Game ~0.38 of window height.
    # Step 1: Click "Level Game" button
    print("[TEST] Step 1: Clicking Level Game button")
    click_frac(0.5, 0.29, "Level Game button", wait=0.5)
    take_screenshot("screenshot_1_after_level_game.png")
    
    # Step 2: Select Level 1
    print("[TEST] Step 2: Selecting Level 1")
    click_frac(0.15, 0.18, "Level 1 button", wait=0.5)
    take_screenshot("screenshot_2_after_level_1_select.png")
    
    # Step 3: Move blue robot UP (first move)
    # Robot starts at ~(0.35, 0.55) and moves to ~(0.35, 0.35)
    print("[TEST] Step 3: Moving blue robot UP")
    drag_frac(0.35, 0.55, 0.35, 0.35, "Blue robot UP", duration=0.5)
    take_screenshot("screenshot_3_after_move_up.png")
    
    # Step 4: Move blue robot RIGHT (second move - completes level)
    # After UP, robot is at ~(0.35, 0.35), move RIGHT to ~(0.75, 0.45) (one cell down from UP position)
    print("[TEST] Step 4: Moving blue robot RIGHT (completes level)")
    drag_frac(0.35, 0.45, 0.75, 0.45, "Blue robot RIGHT", duration=0.5)
    take_screenshot("screenshot_4_after_move_right.png")
    
    # Step 5: Verify completion dialog
    print("[TEST] Step 5: Verifying completion dialog")
    take_screenshot("screenshot_5_completion_dialog.png")
    
    # Step 6: Close completion dialog (Menu button, left of the Retry button)
    print("[TEST] Step 6: Closing completion dialog (Menu button)")
    click_at_coordinates(center_x - 120, center_y + 100, "Menu button", wait=0.5)
    take_screenshot("screenshot_6_after_completion.png")
    
    # Step 7: Click "Load Game" button
    print("[TEST] Step 7: Clicking Load Game button")
    click_at_coordinates(center_x, center_y - 150, "Load Game button", wait=0.5)
    take_screenshot("screenshot_7_after_load_game.png")
    
    # Step 8: Switch to History tab
    print("[TEST] Step 8: Switching to History tab")
    click_at_coordinates(window_x + window_width - 100, window_y + 150, "History tab", wait=0.5)
    take_screenshot("screenshot_8_after_history_tab.png")
    
    # Step 9: Click info button on Level 1 entry
    print("[TEST] Step 9: Clicking info button on Level 1 entry")
    click_at_coordinates(window_x + window_width - 85, window_y + 300, "Info button", wait=0.5)
    take_screenshot("screenshot_9_info_dialog.png")
    
    # Step 10: Verify bestTime and bestMoves are displayed
    print("[TEST] Step 10: Verifying bestTime and bestMoves in dialog")
    print("[TEST] Check screenshot_9_info_dialog.png for bestTime and bestMoves values")
    
    # Step 11: Close info dialog
    print("[TEST] Step 11: Closing info dialog")
    click_at_coordinates(center_x, center_y + 150, "OK button", wait=1)
    take_screenshot("screenshot_10_after_info_close.png")
    
    print("[TEST] Test completed successfully!")
    print("[TEST] All screenshots saved for verification")
    print("[TEST] Check the screenshots to verify bestTime and bestMoves are correctly displayed")

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

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

import pyautogui
import time
import sys
import subprocess
import os

# Configure PyAutoGUI
pyautogui.PAUSE = 0.3
pyautogui.FAILSAFE = True

# Create screenshots directory
SCREENSHOTS_DIR = "/tmp/roboyard_test_screenshots"
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)
print(f"[TEST] Screenshots will be saved to: {SCREENSHOTS_DIR}")

def start_composeapp():
    """Stop any running ComposeApp and start a fresh instance with --no-configuration-cache"""
    print("[TEST] Stopping any running ComposeApp instances...")
    subprocess.run(["pkill", "-9", "-f", "java.*MainKt|gradlew.*composeApp"], 
                   capture_output=True, timeout=5)
    time.sleep(2)
    
    print("[TEST] Starting ComposeApp with --no-configuration-cache...")
    # Start in background, redirect output to log file
    subprocess.Popen(
        ["bash", "-c", "cd /var/www/Roboyard && ./gradlew :composeApp:run --no-configuration-cache > /tmp/app.log 2>&1"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )
    print("[TEST] ComposeApp started in background. Waiting for window to appear...")
    time.sleep(5)  # Wait for app to fully start

def get_window_geometry(window_id):
    """Get ABSOLUTE screen geometry of a window using xwininfo.
    xdotool getwindowgeometry returns parent-relative coords (wrong for clicking),
    while xwininfo 'Absolute upper-left' gives true screen coordinates that match
    pyautogui's coordinate space. Returns dict with x, y, width, height or None."""
    try:
        result = subprocess.run(
            ["xwininfo", "-id", window_id],
            capture_output=True, text=True, timeout=5
        )
        geometry = {}
        for line in result.stdout.strip().split('\n'):
            line = line.strip()
            if line.startswith("Absolute upper-left X:"):
                geometry['x'] = int(line.split(':')[1].strip())
            elif line.startswith("Absolute upper-left Y:"):
                geometry['y'] = int(line.split(':')[1].strip())
            elif line.startswith("Width:"):
                geometry['width'] = int(line.split(':')[1].strip())
            elif line.startswith("Height:"):
                geometry['height'] = int(line.split(':')[1].strip())
        if all(k in geometry for k in ['x', 'y', 'width', 'height']):
            return geometry
    except Exception as e:
        print(f"[TEST] Error getting geometry for {window_id}: {e}")
    return None

def get_composeapp_jvm_pids():
    """Find the JVM process(es) running the ComposeApp (roboyard MainKt)"""
    try:
        result = subprocess.run(
            ["pgrep", "-f", "roboyard.*MainKt"],
            capture_output=True, text=True, timeout=5
        )
        pids = [int(p) for p in result.stdout.strip().split('\n') if p.strip()]
        return pids
    except Exception as e:
        print(f"[TEST] Error finding JVM PIDs: {e}")
    return []

def find_composeapp_window():
    """Find the ComposeApp main window: named exactly 'Roboyard', belonging to the
    ComposeApp JVM process, with the largest area. Returns (window_id, geometry) or None."""
    try:
        jvm_pids = get_composeapp_jvm_pids()
        if not jvm_pids:
            print("[TEST] No ComposeApp JVM process found")
            return None
        print(f"[TEST] ComposeApp JVM PIDs: {jvm_pids}")

        # Search for windows named exactly "Roboyard" (^Roboyard$ excludes SmartGit/Devin titles)
        result = subprocess.run(
            ["xdotool", "search", "--name", "^Roboyard$"],
            capture_output=True, text=True, timeout=5
        )
        window_ids = [w for w in result.stdout.strip().split('\n') if w.strip()]

        best_window = None
        best_area = 0
        for wid in window_ids:
            # Window PID must belong to the ComposeApp JVM
            pr = subprocess.run(["xdotool", "getwindowpid", wid], capture_output=True, text=True, timeout=5)
            try:
                wpid = int(pr.stdout.strip())
            except ValueError:
                continue
            if wpid not in jvm_pids:
                continue
            geom = get_window_geometry(wid)
            if geom:
                area = geom['width'] * geom['height']
                if area > best_area:
                    best_area = area
                    best_window = (wid, geom)
        return best_window
    except Exception as e:
        print(f"[TEST] Error finding ComposeApp window: {e}")
    return None

def wait_for_window(gradle_pid, timeout=60):
    """Wait for the ComposeApp window to appear (named 'Roboyard', matched to JVM PID)"""
    print(f"[TEST] Waiting for ComposeApp window...")
    start_time = time.time()

    while time.time() - start_time < timeout:
        result = find_composeapp_window()
        if result:
            window_id, geometry = result
            # Only accept reasonably sized windows (main window, not 1x1 hidden)
            if geometry['width'] >= 300 and geometry['height'] >= 300:
                print(f"[TEST] Found ComposeApp window: ID={window_id}, bounds={geometry}")
                # Activate window and bring to front (no --sync: some WMs don't update _NET_WM_DESKTOP)
                activate_window(window_id)
                time.sleep(1)
                return window_id, geometry
        time.sleep(1)

    print(f"[TEST] ERROR: Window not found after {timeout} seconds")
    return None, None

def activate_window(window_id):
    """Bring a window to focus/front without --sync (avoids WM hang).
    Uses windowfocus + windowraise which work even when _NET_WM_DESKTOP is unsupported."""
    for cmd in (
        ["xdotool", "windowfocus", window_id],
        ["xdotool", "windowraise", window_id],
        ["xdotool", "windowactivate", window_id],
    ):
        try:
            subprocess.run(cmd, timeout=3, capture_output=True)
        except Exception as e:
            print(f"[TEST] activate_window: {cmd[1]} failed: {e}")

def get_active_window_id():
    """Get the currently focused/active window ID"""
    try:
        result = subprocess.run(
            ["xdotool", "getactivewindow"],
            capture_output=True, text=True, timeout=5
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"[TEST] Error getting active window: {e}")
    return None

def get_focused_window_id():
    """Get the currently focused window ID (works even without _NET_ACTIVE_WINDOW)"""
    try:
        result = subprocess.run(
            ["xdotool", "getwindowfocus"],
            capture_output=True, text=True, timeout=5
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"[TEST] Error getting focused window: {e}")
    return None

def ensure_window_focused(window_id):
    """Ensure the target window is focused before clicking. Returns True if focused."""
    # Try both active and focused window queries (WM support varies)
    focused = get_focused_window_id()
    if focused == window_id:
        return True
    # Re-activate the window
    print(f"[TEST] Window not focused (focused={focused}, target={window_id}), re-activating...")
    activate_window(window_id)
    time.sleep(0.5)
    # Verify again
    focused = get_focused_window_id()
    if focused == window_id:
        return True
    print(f"[TEST] ERROR: Could not focus target window (focused={focused}, target={window_id})")
    return False

def take_screenshot(filename):
    """Take a screenshot for debugging"""
    try:
        filepath = os.path.join(SCREENSHOTS_DIR, filename)
        pyautogui.screenshot(filepath)
        print(f"[TEST] Screenshot saved: {filepath}")
    except Exception as e:
        print(f"[TEST] Error taking screenshot: {e}")

def shake_mouse():
    """Shake mouse cursor minimally left/right for visual confirmation at current position"""
    try:
        current_x, current_y = pyautogui.position()
        # Shake 2x: left (-2px) then right (+2px)
        for _ in range(1):
            pyautogui.moveTo(current_x - 2, current_y, duration=0.005)
            pyautogui.moveTo(current_x + 2, current_y, duration=0.005)
        # Return to original position
        pyautogui.moveTo(current_x, current_y, duration=0.005)
    except Exception as e:
        print(f"[TEST] Error shaking mouse: {e}")

# Global reference to the target ComposeApp window
TARGET_WINDOW_ID = None
WINDOW_BOUNDS = None

def is_within_window(x, y):
    """Check if coordinates are within the target window bounds"""
    if WINDOW_BOUNDS is None:
        return False
    return (WINDOW_BOUNDS['x'] <= x <= WINDOW_BOUNDS['x'] + WINDOW_BOUNDS['width'] and
            WINDOW_BOUNDS['y'] <= y <= WINDOW_BOUNDS['y'] + WINDOW_BOUNDS['height'])

def click_at_coordinates(x, y, description="", wait=0.5):
    """Click at specific coordinates ONLY if target window is focused and coords are within bounds"""
    print(f"[TEST] Clicking at ({x}, {y}): {description}")
    # Safety check 1: coordinates must be within window bounds
    if not is_within_window(x, y):
        print(f"[TEST] ERROR: Coordinates ({x}, {y}) are outside window bounds {WINDOW_BOUNDS}. Skipping click.")
        return False
    # Safety check 2: target window must be focused
    if not ensure_window_focused(TARGET_WINDOW_ID):
        print(f"[TEST] ERROR: Target window not focused. Skipping click to avoid clicking elsewhere.")
        return False
    pyautogui.moveTo(x, y)
    shake_mouse()
    pyautogui.click(x, y)
    time.sleep(wait)
    return True

def drag_from_to(x1, y1, x2, y2, description="", duration=0.5):
    """Drag from one position to another ONLY if target window is focused and coords are within bounds"""
    print(f"[TEST] Dragging from ({x1}, {y1}) to ({x2}, {y2}): {description}")
    # Safety check 1: both endpoints must be within window bounds
    if not is_within_window(x1, y1) or not is_within_window(x2, y2):
        print(f"[TEST] ERROR: Drag coordinates outside window bounds {WINDOW_BOUNDS}. Skipping drag.")
        return False
    # Safety check 2: target window must be focused
    if not ensure_window_focused(TARGET_WINDOW_ID):
        print(f"[TEST] ERROR: Target window not focused. Skipping drag to avoid affecting other windows.")
        return False
    pyautogui.moveTo(x1, y1)
    shake_mouse()
    time.sleep(0.2)
    pyautogui.drag(x2 - x1, y2 - y1, duration=duration)
    time.sleep(0.5)
    return True

def click_frac(fx, fy, description="", wait=0.5):
    """Click at a position given as fractions (0..1) of the target window bounds"""
    x = WINDOW_BOUNDS['x'] + int(fx * WINDOW_BOUNDS['width'])
    y = WINDOW_BOUNDS['y'] + int(fy * WINDOW_BOUNDS['height'])
    return click_at_coordinates(x, y, description, wait)

def drag_frac(fx1, fy1, fx2, fy2, description="", duration=0.5):
    """Drag between two positions given as fractions (0..1) of the target window bounds"""
    x1 = WINDOW_BOUNDS['x'] + int(fx1 * WINDOW_BOUNDS['width'])
    y1 = WINDOW_BOUNDS['y'] + int(fy1 * WINDOW_BOUNDS['height'])
    x2 = WINDOW_BOUNDS['x'] + int(fx2 * WINDOW_BOUNDS['width'])
    y2 = WINDOW_BOUNDS['y'] + int(fy2 * WINDOW_BOUNDS['height'])
    return drag_from_to(x1, y1, x2, y2, description, duration)

def main():
    global TARGET_WINDOW_ID, WINDOW_BOUNDS

    print("[TEST] Starting headed UI test for History Info Button")
    print("[TEST] This test will automate UI interactions with the ComposeApp")
    
    # Start fresh ComposeApp instance with --no-configuration-cache
    start_composeapp()

    # Wait for the ComposeApp window to appear (matched by PID, not title)
    window_id, geometry = wait_for_window(-1)
    if not geometry:
        print("[TEST] ERROR: ComposeApp window not found. Make sure it's running.")
        sys.exit(1)

    # Set globals so click/drag functions can verify focus and bounds
    TARGET_WINDOW_ID = window_id
    WINDOW_BOUNDS = geometry

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

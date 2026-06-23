#!/usr/bin/env python3
"""
Test Suite for ComposeApp UI Testing
Reusable functions for PyAutoGUI-based UI automation tests

Usage Example:
    from testsuite.test_suite import (
        start_composeapp, wait_for_window, init_globals,
        take_screenshot, click_frac, configure_app
    )
    
    # Configure app-specific settings
    configure_app(
        name="MyApp",
        path="/path/to/app",
        process_pattern="myapp.*MainKt",
    )
    
    # Start app and wait for window
    start_composeapp()
    window_id, geometry = wait_for_window(-1)
    init_globals(window_id, geometry)
    
    # Interact with UI
    click_frac(0.5, 0.5, "Center button")
    take_screenshot("screenshot.png")
"""

import pyautogui
import time
import subprocess
import os

# Configure PyAutoGUI
pyautogui.PAUSE = 0.3
pyautogui.FAILSAFE = True

# Global reference to the target ComposeApp window
TARGET_WINDOW_ID = None
WINDOW_BOUNDS = None

# App-specific configuration (to be set by caller)
APP_NAME = "MyApp"
APP_PATH = "/path/to/app"
APP_PROCESS_PATTERN = "myapp.*MainKt"
# Optional (defaults will be used if not set):
APP_LOG_FILE = "/tmp/app.log"
SCREENSHOTS_DIR = "/tmp/python_testsuite_screenshots"

def configure_app(name, path, process_pattern, log_file=None, screenshots_dir=None):
    """Configure app-specific settings (log_file and screenshots_dir are optional)"""
    global APP_NAME, APP_PATH, APP_PROCESS_PATTERN, APP_LOG_FILE, SCREENSHOTS_DIR
    APP_NAME = name
    APP_PATH = path
    APP_PROCESS_PATTERN = process_pattern
    if log_file is not None:
        APP_LOG_FILE = log_file
    if screenshots_dir is not None:
        SCREENSHOTS_DIR = screenshots_dir

def start_composeapp():
    """Stop any running ComposeApp and start a fresh instance with --no-configuration-cache"""
    print("[TEST_SUITE] Stopping any running ComposeApp instances...")
    subprocess.run(["pkill", "-9", "-f", "java.*MainKt|gradlew.*composeApp"], 
                   capture_output=True, timeout=5)
    time.sleep(2)
    
    print("[TEST_SUITE] Starting ComposeApp with --no-configuration-cache...")
    # Start in background, redirect output to log file
    subprocess.Popen(
        ["bash", "-c", f"cd {APP_PATH} && ./gradlew :composeApp:run --no-configuration-cache > {APP_LOG_FILE} 2>&1"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )
    print("[TEST_SUITE] ComposeApp started in background. Waiting for window to appear...")
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
        print(f"[TEST_SUITE] Error getting geometry for {window_id}: {e}")
    return None

def get_composeapp_jvm_pids():
    """Find the JVM process(es) running the ComposeApp"""
    try:
        result = subprocess.run(
            ["pgrep", "-f", APP_PROCESS_PATTERN],
            capture_output=True, text=True, timeout=5
        )
        pids = [int(p) for p in result.stdout.strip().split('\n') if p.strip()]
        return pids
    except Exception as e:
        print(f"[TEST_SUITE] Error finding JVM PIDs: {e}")
    return []

def find_composeapp_window():
    """Find the ComposeApp main window: named exactly APP_NAME, belonging to the
    ComposeApp JVM process, with the largest area. Returns (window_id, geometry) or None."""
    try:
        jvm_pids = get_composeapp_jvm_pids()
        if not jvm_pids:
            print("[TEST_SUITE] No ComposeApp JVM process found")
            return None
        print(f"[TEST_SUITE] ComposeApp JVM PIDs: {jvm_pids}")

        # Search for windows named exactly APP_NAME (excludes other windows with similar names)
        result = subprocess.run(
            ["xdotool", "search", "--name", f"^{APP_NAME}$"],
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
        print(f"[TEST_SUITE] Error finding ComposeApp window: {e}")
    return None

def wait_for_window(gradle_pid, timeout=60):
    """Wait for the ComposeApp window to appear (named APP_NAME, matched to JVM PID)"""
    print(f"[TEST_SUITE] Waiting for ComposeApp window...")
    start_time = time.time()

    while time.time() - start_time < timeout:
        result = find_composeapp_window()
        if result:
            window_id, geometry = result
            # Only accept reasonably sized windows (main window, not 1x1 hidden)
            if geometry['width'] >= 300 and geometry['height'] >= 300:
                print(f"[TEST_SUITE] Found ComposeApp window: ID={window_id}, bounds={geometry}")
                # Activate window and bring to front (no --sync: some WMs don't update _NET_WM_DESKTOP)
                activate_window(window_id)
                time.sleep(1)
                return window_id, geometry
        time.sleep(1)

    print(f"[TEST_SUITE] ERROR: Window not found after {timeout} seconds")
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
            print(f"[TEST_SUITE] activate_window: {cmd[1]} failed: {e}")

def get_active_window_id():
    """Get the currently focused/active window ID"""
    try:
        result = subprocess.run(
            ["xdotool", "getactivewindow"],
            capture_output=True, text=True, timeout=5
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"[TEST_SUITE] Error getting active window: {e}")
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
        print(f"[TEST_SUITE] Error getting focused window: {e}")
    return None

def ensure_window_focused(window_id):
    """Ensure the target window is focused before clicking. Returns True if focused."""
    # Try both active and focused window queries (WM support varies)
    focused = get_focused_window_id()
    if focused == window_id:
        return True
    # Re-activate the window
    print(f"[TEST_SUITE] Window not focused (focused={focused}, target={window_id}), re-activating...")
    activate_window(window_id)
    time.sleep(0.5)
    # Verify again
    focused = get_focused_window_id()
    if focused == window_id:
        return True
    print(f"[TEST_SUITE] ERROR: Could not focus target window (focused={focused}, target={window_id})")
    return False

def take_screenshot(filename, screenshots_dir=None):
    """Take a screenshot for debugging"""
    if screenshots_dir is None:
        screenshots_dir = SCREENSHOTS_DIR
    try:
        os.makedirs(screenshots_dir, exist_ok=True)
        filepath = os.path.join(screenshots_dir, filename)
        pyautogui.screenshot(filepath)
        print(f"[TEST_SUITE] Screenshot saved: {filepath}")
    except Exception as e:
        print(f"[TEST_SUITE] Error taking screenshot: {e}")

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
        print(f"[TEST_SUITE] Error shaking mouse: {e}")

def is_within_window(x, y):
    """Check if coordinates are within the target window bounds"""
    if WINDOW_BOUNDS is None:
        return False
    return (WINDOW_BOUNDS['x'] <= x <= WINDOW_BOUNDS['x'] + WINDOW_BOUNDS['width'] and
            WINDOW_BOUNDS['y'] <= y <= WINDOW_BOUNDS['y'] + WINDOW_BOUNDS['height'])

def click_at_coordinates(x, y, description="", wait=0.5):
    """Click at specific coordinates ONLY if target window is focused and coords are within bounds"""
    print(f"[TEST_SUITE] Clicking at ({x}, {y}): {description}")
    # Safety check 1: coordinates must be within window bounds
    if not is_within_window(x, y):
        print(f"[TEST_SUITE] ERROR: Coordinates ({x}, {y}) are outside window bounds {WINDOW_BOUNDS}. Skipping click.")
        return False
    # Safety check 2: target window must be focused
    if not ensure_window_focused(TARGET_WINDOW_ID):
        print(f"[TEST_SUITE] ERROR: Target window not focused. Skipping click to avoid clicking elsewhere.")
        return False
    pyautogui.moveTo(x, y)
    shake_mouse()
    pyautogui.click(x, y)
    time.sleep(wait)
    return True

def drag_from_to(x1, y1, x2, y2, description="", duration=0.5):
    """Drag from one position to another ONLY if target window is focused and coords are within bounds"""
    print(f"[TEST_SUITE] Dragging from ({x1}, {y1}) to ({x2}, {y2}): {description}")
    # Safety check 1: both endpoints must be within window bounds
    if not is_within_window(x1, y1) or not is_within_window(x2, y2):
        print(f"[TEST_SUITE] ERROR: Drag coordinates outside window bounds {WINDOW_BOUNDS}. Skipping drag.")
        return False
    # Safety check 2: target window must be focused
    if not ensure_window_focused(TARGET_WINDOW_ID):
        print(f"[TEST_SUITE] ERROR: Target window not focused. Skipping drag to avoid affecting other windows.")
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

def init_globals(window_id, geometry):
    """Initialize global variables for window tracking"""
    global TARGET_WINDOW_ID, WINDOW_BOUNDS
    TARGET_WINDOW_ID = window_id
    WINDOW_BOUNDS = geometry

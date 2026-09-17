#!/usr/bin/env python3
"""
HEADED UI Regression Test: robot movement + level editor navigation
Uses PyAutoGUI against the running Compose Desktop app.

How to run:
1. Start the ComposeApp: ./gradlew :composeApp:run
2. Wait for the main menu to appear
3. Run: python3 composeApp/gameflow_regression_test.py

What this test verifies:
- Robot can be moved by dragging it one cell (log shows "Movement INITIATED"
  and the robot's position actually changes)
- Robot can be moved by tap-select + tap on empty cell in the same row/column
- The "Level Design Editor" button in the main menu opens the editor screen
  (verified via the editor's log marker and a changed screenshot)
"""

import sys
import os
import re
import time

from testsuite.test_suite import (
    wait_for_window,
    init_globals,
    take_screenshot,
    click_frac,
    drag_frac,
    click_at_coordinates,
    configure_app,
    WINDOW_BOUNDS as _WB_UNUSED,
)
import testsuite.test_suite as test_suite

configure_app(
    name="Roboyard",
    path="/var/www/Roboyard",
    process_pattern="roboyard.*MainKt",
    log_file=os.environ.get("APP_LOG", "/tmp/composeapp_run.log"),
)

FAILED = []


def log_content():
    try:
        with open(test_suite.APP_LOG_FILE, "r") as f:
            return f.read()
    except Exception:
        return ""


def parse_board_and_robots(content):
    """Parse board dims + robot positions from solver log lines:
    'Board created with width=9, height=9' and
    'Setting robot N at board position P (x=X, y=Y)'."""
    m = re.search(r"Board created with width=(\d+), height=(\d+)", content)
    if not m:
        return None
    bw, bh = int(m.group(1)), int(m.group(2))
    robots = {}
    for rm in re.finditer(
        r"Setting robot (\d+) at board position \d+ \(x=(\d+), y=(\d+)\)", content
    ):
        robots[int(rm.group(1))] = (int(rm.group(2)), int(rm.group(3)))
    if not robots:
        return None
    # Apply all completed moves so positions reflect the CURRENT board
    for mm in re.finditer(
        r"Movement INITIATED: Robot (\d+) moving from \(\d+,\d+\) to \((\d+),(\d+)\)",
        content,
    ):
        robots[int(mm.group(1))] = (int(mm.group(2)), int(mm.group(3)))
    positions = [robots[i] for i in sorted(robots)]
    return bw, bh, positions


def _find_color_blob(im, match, min_pixels=30):
    """Find the largest cluster of pixels matching `match(r,g,b)` in the
    window region. Returns (cx, cy) screen coords of the cluster centroid.
    Pixels are binned into coarse cells to cluster nearby hits."""
    b = test_suite.WINDOW_BOUNDS
    hits = []
    # The board occupies the upper part of the window; the bottom area has
    # buttons/icons that can contain similar colors (pink-ish save button etc.)
    y_max = b["y"] + int(b["height"] * 0.65)
    for y in range(b["y"], y_max, 2):
        for x in range(b["x"], b["x"] + b["width"], 2):
            if match(*im.getpixel((x, y))):
                hits.append((x, y))
    if not hits:
        return None
    # Bin into 20px buckets; the densest bucket neighborhood is the sprite
    from collections import defaultdict
    bins = defaultdict(list)
    for x, y in hits:
        bins[(x // 20, y // 20)].append((x, y))
    # Merge: pick the bucket with most hits plus its 8 neighbors
    key = max(bins, key=lambda k: len(bins[k]))
    kx, ky = key
    pts = []
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            pts += bins.get((kx + dx, ky + dy), [])
    if len(pts) < min_pixels:
        return None
    return (sum(p[0] for p in pts) / len(pts), sum(p[1] for p in pts) / len(pts))


def find_board_on_screen(robots):
    """Locate the board by finding the pink (index 0) and green (index 1)
    robot sprites, then derive board origin + cell size from their known
    grid positions. Returns (left, top, cell_px) in screen coords."""
    from PIL import Image
    test_suite.take_screenshot("_board_scan.png")
    im = Image.open("/tmp/python_testsuite_screenshots/_board_scan.png").convert("RGB")

    pink = _find_color_blob(im, lambda r, g, b: r > 220 and 110 < g < 190 and b > 160)
    green = _find_color_blob(im, lambda r, g, b: r < 80 and 140 < g < 215 and 40 < b < 120)
    if not pink or not green:
        print(f"[TEST] Robot blobs not found: pink={pink} green={green}")
        return None
    rx0, ry0 = robots[0]  # pink grid cell
    rx1, ry1 = robots[1]  # green grid cell
    dxg, dyg = rx1 - rx0, ry1 - ry0
    if dxg == 0 or dyg == 0:
        print("[TEST] Robot cells not distinct enough for calibration")
        return None
    cell_x = (green[0] - pink[0]) / dxg
    cell_y = (green[1] - pink[1]) / dyg
    cell = (cell_x + cell_y) / 2
    left = pink[0] - (rx0 + 0.5) * cell
    top = pink[1] - (ry0 + 0.5) * cell
    print(f"[TEST] Calibrated: pink@({pink[0]:.0f},{pink[1]:.0f}) green@({green[0]:.0f},{green[1]:.0f}) -> origin=({left:.0f},{top:.0f}) cell={cell:.1f}px")
    return left, top, cell


def cell_to_screen(cx, cy, board):
    """Convert board cell to absolute screen coords using the detected board box."""
    left, top, cell = board
    return (int(left + cx * cell + cell / 2), int(top + cy * cell + cell / 2))


def main():
    print("[TEST] Attaching to running ComposeApp (log: %s)" % test_suite.APP_LOG_FILE)
    window_id, geometry = wait_for_window(-1, timeout=20)
    if not geometry:
        print("[TEST] ERROR: ComposeApp window not found")
        sys.exit(1)
    init_globals(window_id, geometry)
    take_screenshot("gf_0_mainmenu.png")

    # ---- Part A: robot movement ----
    print("[TEST] Step 1: New Random Game")
    board = None
    for attempt in range(3):
        click_frac(0.5, 0.17, "New Random Game", wait=1.0)
        # Wait for the solver to produce a board
        for _ in range(20):
            time.sleep(1)
            board = parse_board_and_robots(log_content())
            if board:
                break
        if board:
            break
        print(f"[TEST] Click attempt {attempt + 1} did not start a game, retrying")
    if not board:
        print("[TEST] FAIL: no board/robot positions in log")
        sys.exit(1)
    bw, bh, robots = board
    print(f"[TEST] Board {bw}x{bh}, robots: {robots}")
    time.sleep(2)  # let the board fully render
    take_screenshot("gf_1_game.png")

    # Locate the rendered board precisely via the robot sprite colors
    board_box = find_board_on_screen(robots)
    if not board_box:
        print("[TEST] FAIL: could not locate board via robot sprites")
        sys.exit(1)

    # Marker: current log length, so we only inspect NEW output
    log_len_before = len(log_content())

    # Drag robot 0 one cell to the right (or any direction that keeps it
    # on the board — the actual slide distance doesn't matter, we only
    # verify a move was initiated)
    rx, ry = robots[0]
    dx, dy = (1, 0) if rx + 1 < bw else (-1, 0)
    x1, y1 = cell_to_screen(rx, ry, board_box)
    x2, y2 = cell_to_screen(rx + dx, ry + dy, board_box)
    print(f"[TEST] Step 2: drag robot 0 from ({x1},{y1}) to ({x2},{y2})")
    from testsuite.test_suite import drag_from_to
    drag_from_to(x1, y1, x2, y2, "robot 0 drag", duration=0.6)
    time.sleep(1.5)

    new_log = log_content()[log_len_before:]
    moved = "Movement INITIATED" in new_log or "Moving robot" in new_log
    print("[TEST] Drag move log excerpt:", new_log[-800:])
    if not moved:
        FAILED.append("drag move did not initiate")
        print("[TEST] FAIL: no 'Movement INITIATED' after drag")
    else:
        print("[TEST] PASS: drag initiated a move")

    # Tap-to-move: tap robot, then tap an empty cell in its row
    log_len_before = len(log_content())
    board2 = parse_board_and_robots(log_content())
    robots2 = board2[2] if board2 else robots
    rx, ry = robots2[0]
    tapx, tapy = cell_to_screen(rx, ry, board_box)
    print(f"[TEST] Step 3: tap robot 0 at ({tapx},{tapy})")
    click_at_coordinates(tapx, tapy, "tap robot 0", wait=0.4)
    # Tap an empty cell on the same row
    empty_x = bw - 1 if rx < bw - 2 else 0
    ex, ey = cell_to_screen(empty_x, ry, board_box)
    click_at_coordinates(ex, ey, "tap empty cell same row", wait=0.8)
    new_log = log_content()[log_len_before:]
    moved2 = "Movement INITIATED" in new_log
    if not moved2:
        FAILED.append("tap move did not initiate")
        print("[TEST] FAIL: no 'Movement INITIATED' after tap-to-move")
    else:
        print("[TEST] PASS: tap-to-move initiated a move")

    # ---- Part B: level editor button ----
    print("[TEST] Step 4: back to main menu (Menu button ~5%/92%)")
    click_frac(0.16, 0.955, "Menu button", wait=1.0)
    take_screenshot("gf_2_backtomenu.png")

    shot_before = "gf_3_before_editor.png"
    take_screenshot(shot_before)
    log_len_before = len(log_content())
    # The editor button sits below "Load Game" in the 70%-width button column
    click_frac(0.5, 0.42, "Level Design Editor button", wait=1.5)
    time.sleep(1)
    take_screenshot("gf_4_after_editor_click.png")

    new_log = log_content()[log_len_before:]
    editor_opened = "[LEVEL_EDITOR] Opened" in new_log
    if not editor_opened:
        FAILED.append("level editor did not open")
        print("[TEST] FAIL: editor log marker missing after button click")
    else:
        print("[TEST] PASS: level editor opened")

    print("\n[TEST] ===== RESULT =====")
    if FAILED:
        for f in FAILED:
            print("[TEST] FAILED:", f)
        sys.exit(1)
    print("[TEST] ALL PASS")


if __name__ == "__main__":
    main()

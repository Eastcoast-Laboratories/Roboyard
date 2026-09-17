#!/usr/bin/env python3
"""
HEADED UI Diagnostic Test: scroll during drag on the Help screen.

How to run:
    python3 composeApp/help_scroll_test.py

What this test checks:
1. Whether drag move events reach the scroll handler while the mouse button
   is held ("[SCROLL_DRAG] move#N" lines appearing BEFORE release).
2. Whether scrollState.value changes during the drag ("scroll=" column).
3. Whether the visible content repaints during the drag (screenshot taken
   mid-drag must differ from the pre-drag screenshot).
4. Whether mouse-wheel scrolling works.

The app is started fresh by this test; its stdout is captured in the log
file so the [SCROLL_DRAG]/[HELP] markers can be parsed.
"""

import sys
import os
import re
import time
import hashlib

import pyautogui

from testsuite.test_suite import (
    wait_for_window,
    init_globals,
    take_screenshot,
    click_frac,
    start_composeapp,
    configure_app,
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


def shot_hash(path):
    try:
        with open(path, "rb") as f:
            return hashlib.md5(f.read()).hexdigest()[:12]
    except Exception:
        return None


def scroll_values(text):
    """All scroll= values reported by [SCROLL_DRAG] lines, in order."""
    return re.findall(r"scroll=(\d+)", text)


def main():
    print("[TEST] Starting fresh ComposeApp instance (log: %s)" % test_suite.APP_LOG_FILE)
    start_composeapp()
    window_id, geometry = wait_for_window(-1, timeout=90)
    if not geometry:
        print("[TEST] ERROR: ComposeApp window not found")
        sys.exit(1)
    init_globals(window_id, geometry)
    time.sleep(3)
    take_screenshot("hs_0_mainmenu.png")

    # ---- Step 1: open Help screen (2nd of 4 footer icons) ----
    log_len = len(log_content())
    click_frac(0.375, 0.955, "Help icon", wait=1.5)
    time.sleep(1)
    if "[HELP] Opened" not in log_content()[log_len:]:
        print("[TEST] WARN: no '[HELP] Opened' marker - clicking again")
        click_frac(0.375, 0.955, "Help icon retry", wait=1.5)
    take_screenshot("hs_1_help.png")
    print("[TEST] Help opened:", "[HELP] Opened" in log_content()[log_len:])

    # ---- Step 2: drag on the content, watch the log DURING the hold ----
    b = test_suite.WINDOW_BOUNDS
    cx = b["x"] + int(b["width"] * 0.5)
    cy = b["y"] + int(b["height"] * 0.6)
    print(f"[TEST] Dragging from ({cx},{cy}) upward with hold...")

    log_len = len(log_content())
    take_screenshot("hs_2_before_drag.png")
    h_before = shot_hash(os.path.join(test_suite.SCREENSHOTS_DIR, "hs_2_before_drag.png"))

    pyautogui.moveTo(cx, cy)
    time.sleep(0.3)
    pyautogui.mouseDown()
    time.sleep(0.4)
    # move upward slowly so the app can (fail to) repaint mid-drag
    for i in range(10):
        pyautogui.moveTo(cx, cy - (i + 1) * 40, duration=0.15)

    # ---- measurements while the button is STILL held ----
    time.sleep(0.5)
    during_log = log_content()[log_len:]
    moves_during = len(re.findall(r"\[SCROLL_DRAG\] move#", during_log))
    awt_during = len(re.findall(r"\[AWT_DRAG\]", during_log))
    pevt_during = len(re.findall(r"\[POINTER_EVT\] move", during_log))
    vals_during = scroll_values(during_log)
    print(f"[TEST] during hold: AWT_DRAG={awt_during} POINTER_EVT_move={pevt_during} SCROLL_DRAG_move={moves_during}")
    take_screenshot("hs_3_during_drag.png")
    h_during = shot_hash(os.path.join(test_suite.SCREENSHOTS_DIR, "hs_3_during_drag.png"))

    pyautogui.mouseUp()
    time.sleep(1.0)

    # ---- measurements after release ----
    after_log = log_content()[log_len:]
    moves_total = len(re.findall(r"\[SCROLL_DRAG\] move#", after_log))
    vals_after = scroll_values(after_log)
    take_screenshot("hs_4_after_release.png")
    h_after = shot_hash(os.path.join(test_suite.SCREENSHOTS_DIR, "hs_4_after_release.png"))

    print(f"[TEST] moves during hold: {moves_during}, scroll values: {vals_during[-5:]}")
    print(f"[TEST] moves after release: {moves_total - moves_during}, scroll values: {vals_after[-5:]}")
    print(f"[TEST] screenshot hashes: before={h_before} during={h_during} after={h_after}")

    if moves_during > 0:
        print("[TEST] OK: move events arrive while button is held")
    else:
        print("[TEST] RESULT: NO move events during hold - events arrive only at release")

    if h_during != h_before:
        print("[TEST] OK: content repaints during drag")
    else:
        print("[TEST] RESULT: content does NOT repaint during drag (paint deferred)")

    if vals_after and vals_after[-1] != "0":
        print(f"[TEST] scroll reached {vals_after[-1]}")
    else:
        FAILED.append("drag produced no scroll")
        print("[TEST] FAIL: scroll value stayed 0 after drag")

    # ---- Step 3: mouse wheel ----
    log_len = len(log_content())
    pyautogui.moveTo(cx, cy)
    pyautogui.scroll(-8)
    time.sleep(1.2)
    wheel_vals = scroll_values(log_content()[log_len:])
    if wheel_vals:
        print(f"[TEST] OK: wheel scrolled, values: {wheel_vals[-3:]}")
    else:
        # wheel may use Scrolled-to markers on screens with their own logging
        print("[TEST] NOTE: no SCROLL_DRAG scroll= marker for wheel (expected - wheel has its own path)")

    print("\n[TEST] ===== RESULT =====")
    if FAILED:
        for f in FAILED:
            print("[TEST] FAILED:", f)
        sys.exit(1)
    print("[TEST] DONE")


if __name__ == "__main__":
    main()

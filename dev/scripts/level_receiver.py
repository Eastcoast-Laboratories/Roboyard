#!/usr/bin/env python3
"""
Level Receiver — lightweight HTTP server for the in-game Level Editor.

Receives level data via HTTP POST from the Android emulator and writes
it directly into the project's Maps directory (sourcecode).

Usage:
  1. start the receiver:
    python3 dev/scripts/level_receiver.py
  2. in the app, go to Level Design Editor and click "Export Level" button

Listens on: http://0.0.0.0:8787
Android emulator reaches this via: http://10.0.2.2:8787

Endpoints:
  GET  /ping        — health check (used by app to show/hide button)
  POST /save-level  — save level data to Maps directory
       Body: {"level_id": 42, "level_data": "board:12,14;\\nmh0,0;\\n..."}
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import os

MAPS_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "../../app/src/main/assets/Maps"))


class LevelReceiver(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == "/ping":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ok"}).encode())
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path != "/save-level":
            self.send_error(404)
            return

        content_length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(content_length))

        level_id = body.get("level_id")
        level_data = body.get("level_data", "")

        if not level_id or not level_data:
            self.send_response(400)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(
                {"error": "Missing level_id or level_data"}).encode())
            return

        filepath = os.path.join(MAPS_DIR, f"level_{level_id}.txt")
        existed = os.path.exists(filepath)

        with open(filepath, "w") as f:
            f.write(level_data)

        action = "overwritten" if existed else "created"
        print(f"✓ Level {level_id} {action}: {filepath}")

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({
            "success": True,
            "message": f"Level {level_id} {action}",
            "path": filepath
        }).encode())

    def log_message(self, format, *args):
        # Prefix log lines for easy grep
        print(f"[RECEIVER] {args[0]}")


if __name__ == "__main__":
    server = HTTPServer(("0.0.0.0", 8787), LevelReceiver)
    print(f"Level receiver listening on http://0.0.0.0:8787")
    print(f"Maps directory: {os.path.abspath(MAPS_DIR)}")
    print(f"Waiting for connections from Android emulator (10.0.2.2)...")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down.")
        server.server_close()

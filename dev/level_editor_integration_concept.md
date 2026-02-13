# Level Editor → Sourcecode Integration Concept

## Goal
Enable developers to save an edited level from the **in-game Level Editor** (running in the Android emulator) directly into the project sourcecode on the same dev machine — with a single button click.

## Current Workflow
1. Developer edits level in the in-game Level Editor
2. Clicks "Export" → sees level text in a dialog
3. Copies text to clipboard
4. Manually creates/overwrites `level_X.txt` in `/app/src/main/assets/Maps/`
5. Rebuilds app

## Proposed Workflow
1. Developer edits level in the in-game Level Editor
2. Clicks **"Save to Sourcecode"** button
3. App checks if receiver is reachable
    - only show the button if receiver is reachable
    - ignore the button if receiver is not reachable (log to console)
4. App sends level data via HTTP POST to `http://10.0.2.2:8787/save-level`
   (10.0.2.2 = host machine from Android emulator)
5. A lightweight local receiver script writes the file to the Maps directory
5. Done — developer rebuilds app in Android Studio
---

## Architecture

```
┌─────────────────────────────────┐
│  Android Emulator               │
│  In-Game Level Editor           │
│                                 │
│  [Save to Sourcecode] button    │
│         │                       │
│         │ HTTP POST             │
│         │ http://10.0.2.2:8787  │
│         │ /save-level           │
└─────────┼───────────────────────┘
          │
          ▼
┌─────────────────────────────────┐
│  Dev Machine (localhost:8787)   │
│  level_receiver.py              │
│                                 │
│  Receives POST with:            │
│  - level_id (int)               │
│  - level_data (string)          │
│                                 │
│  Writes to:                     │
│  .../assets/Maps/level_X.txt    │
└─────────────────────────────────┘
```

**Key insight:** Android emulator can reach the host machine at `10.0.2.2`. No SSH, no network config, no Laravel needed.

---

## Implementation

 
### 1. Local Receiver Script (Python, ~40 lines)

File: `dev/scripts/level_receiver.py`

```python
#!/usr/bin/env python3
"""
Lightweight HTTP server that receives level data from the in-game editor
and writes it to the project's Maps directory.

Usage: python3 dev/scripts/level_receiver.py
Listens on: http://localhost:8787
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import os

MAPS_DIR = os.path.join(os.path.dirname(__file__), 
    "../../app/src/main/assets/Maps")

class LevelReceiver(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/save-level":
            self.send_error(404)
            return
        
        content_length = int(self.headers["Content-Length"])
        body = json.loads(self.rfile.read(content_length))
        
        level_id = body.get("level_id")
        level_data = body.get("level_data", "")
        
        if not level_id or not level_data:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b'{"error": "Missing level_id or level_data"}')
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

if __name__ == "__main__":
    server = HTTPServer(("0.0.0.0", 8787), LevelReceiver)
    print(f"Level receiver listening on http://localhost:8787")
    print(f"Maps directory: {os.path.abspath(MAPS_DIR)}")
    server.serve_forever()
```

### 2. Android App Changes (LevelDesignEditorFragment)

Add a "Save to Sourcecode" button in the export dialog that sends the level data via HTTP POST:

```java
// In showLevelText() dialog, add a new button:
Button saveToSourceButton = new Button(requireContext());
saveToSourceButton.setText("💾 Save to Sourcecode");
saveToSourceButton.setOnClickListener(v -> {
    saveToSourcecode(currentLevelId, levelText);
});
container.addView(saveToSourceButton);
```

```java
private void saveToSourcecode(int levelId, String levelText) {
    // 10.0.2.2 = host machine from Android emulator
    String url = "http://10.0.2.2:8787/save-level";
    
    new Thread(() -> {
        try {
            URL endpoint = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            JSONObject json = new JSONObject();
            json.put("level_id", levelId);
            json.put("level_data", levelText);
            
            conn.getOutputStream().write(json.toString().getBytes());
            
            int responseCode = conn.getResponseCode();
            String response = new String(conn.getInputStream().readAllBytes());
            
            requireActivity().runOnUiThread(() -> {
                if (responseCode == 200) {
                    Toast.makeText(requireContext(), 
                        "✓ Level " + levelId + " saved to sourcecode!", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), 
                        "✗ Save failed: " + response, 
                        Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), 
                    "✗ Connection failed. Is level_receiver.py running?", 
                    Toast.LENGTH_LONG).show();
            });
            Timber.e(e, "[EDITOR_EXPORT] Failed to save to sourcecode");
        }
    }).start();
}
```

### 3. Network Security Config

Add `10.0.2.2` to the network security config to allow cleartext HTTP in debug builds:

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

---

## Developer Workflow

### Setup (once)
```bash
# Start the receiver in a terminal
python3 dev/scripts/level_receiver.py
```

### Daily use
1. Open app in emulator → Level Editor
2. Edit level (walls, robots, target)
3. Click "Export" → dialog opens
3. Click **"Save to Sourcecode"**
4. Terminal shows: `✓ Level 42 overwritten: .../Maps/level_42.txt`
5. Rebuild app (or use file watcher for auto-rebuild)

---

## Security

- **Only works locally** — 10.0.2.2 is only reachable from the emulator
- **Only in debug builds** — cleartext HTTP is restricted to debug
- **No authentication needed** — it's localhost-to-localhost
- The receiver script only writes to the Maps directory
- The receiver script validates level_id and level_data

---


## Future Enhancements

- **Level validation**: Receiver validates level format before saving

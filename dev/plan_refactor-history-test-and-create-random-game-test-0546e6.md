# Refactor History Test and Create Random Game Test

## Zusammenfassung
Extrahiere eine wiederverwendbare Testsuite aus dem history_info_button_headed_test.py und erstelle einen neuen Test für Random Games, der Roboter-Positionen dynamisch aus dem Log berechnet.

## Schritte

### 1. Testsuite extrahieren
Erstelle `test_suite.py` mit allgemeinen Funktionen:
- `start_composeapp()` - App starten
- `get_window_geometry()` - Fenster-Geometrie abrufen
- `get_composeapp_jvm_pids()` - JVM PIDs finden
- `find_composeapp_window()` - Fenster finden
- `wait_for_window()` - Auf Fenster warten
- `activate_window()` - Fenster aktivieren
- `get_active_window_id()` - Aktives Fenster abrufen
- `get_focused_window_id()` - Fokussiertes Fenster abrufen
- `ensure_window_focused()` - Sicherstellen, dass Fenster fokussiert ist
- `take_screenshot()` - Screenshot machen
- `shake_mouse()` - Maus schütteln
- `is_within_window()` - Prüfen, ob Koordinaten im Fenster liegen
- `click_at_coordinates()` - Bei Koordinaten klicken
- `drag_from_to()` - Drag von zu
- `click_frac()` - Bei Fraktion klicken
- `drag_frac()` - Drag bei Fraktion

### 2. History Test refaktorieren
Passe `history_info_button_headed_test.py` an:
- Importiere Funktionen aus `test_suite.py`
- Entferne duplizierte Funktionen
- Behalte nur testspezifische Logik (Level 1 spezifische Koordinaten und Schritte)

### 3. move_robot Funktion implementieren
Erstelle eine Funktion, die Roboter-Positionen dynamisch berechnet:
- Parameter: `robot_color` (0-@Constants.kt#L84 MAX_NUM_ROBOTS), `direction` (UP/DOWN/LEFT/RIGHT), `robot_positions` (Liste von (x, y) Koordinaten), `board_width`, `board_height`
- Berechne Screen-Koordinaten für Roboter basierend auf Board-Größe und Position
- Berechne Ziel-Koordinaten basierend auf Richtung und hindernissen in der richtung (mauer oder anderer robot)
- Führe Drag-Operation aus

### 4. Log-Parser für Lösung implementieren
Erstelle Funktion zum Parsen der Lösung aus dem App-Log:
- Parse `/tmp/app.log` für Solver-Lösung
- Format: `solution: size=N ***** [moves] *****`
- Moves sind im Format: `bN` (blue north), `rE` (red east), etc.
- Konvertiere Moves in (robot_color, direction) Paare

### 5. Preferences-Storage-Reader implementieren
Erstelle Funktion zum Lesen der Preferences direkt aus Storage (DRY - wie im restlichen Code):
- Lese `boardSizeX` und `boardSizeY` aus Storage (wie Preferences.boardSizeWidth/Height)
- Lese `robotCount` aus Storage (wie Preferences.robotCount)
- Lese `targetColors` aus Storage (wie Preferences.targetColors)
- Verwende denselben Storage-Mechanismus wie der Rest der App

### 6. Random Game Test erstellen
Erstelle `random_game_test.py`:
- Starte Random Game (New Random Game Button)
- Parse Board-Größe aus Preferences
- Parse Roboter-Startpositionen aus Log (Format: `Robot positions: [x1,y1, x2,y2, ...]`)
- Parse Lösung aus Log
- Extrahiere ersten Move aus Lösung
- Führe ersten Move mit `move_robot` aus
- Verifiziere History-Eintrag wird korrekt aktualisiert

## Annahmen
- Roboter-Anzahl: Immer 4 (außer Deep Link Games, noch nicht implementiert)
- Roboter-Farben: 0=blue, 1=green, 2=red, 3=yellow (entspricht LevelLoader.parseColorChar)
- Board-Größe: Wird aus Preferences geladen (z.B. 12x14)

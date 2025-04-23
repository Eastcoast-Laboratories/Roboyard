#!/bin/bash

# Pfade definieren
BUILD_GRADLE="app/build.gradle"
BASE_DIR="fastlane/metadata/android"
CHANGELOG_MD="CHANGELOG.md"
CHANGELOG_DE_MD="CHANGELOG_de.md"
PLAYSTORE_DIR="fastlane/metadata/android/playstore"

# Version aus build.gradle extrahieren
VERSION_NAME=$(grep versionName $BUILD_GRADLE | awk -F\" '{print $2}')

# Aktuelles Datum
CURRENT_DATE=$(date +"%Y-%m-%d")

# Play Store Zeichenlimit für Changelogs (pro Sprache)
PLAYSTORE_CHAR_LIMIT=500

# Funktion zum Prüfen der Zeichenlänge und Warnen bei Überschreitung
check_length() {
  local content="$1"
  local locale="$2"
  # Zähle nur die tatsächlichen Changelog-Einträge, nicht die XML-Tags
  local content_without_tags=$(echo "$content" | grep -v "^<.*>$")
  local chars=$(echo "$content_without_tags" | wc -c)
  local limit=$PLAYSTORE_CHAR_LIMIT
  
  echo "Changelog für $locale: $chars Zeichen"
  
  if [ $chars -gt $limit ]; then
    local over=$((chars - limit))
    echo "⚠️ WARNUNG: Changelog für $locale ist um $over Zeichen zu lang! (Maximum: $limit)"
    return 1
  else
    local remaining=$((limit - chars))
    echo "✔️ OK: Noch $remaining Zeichen verfügbar."
    return 0
  fi
}

# Deutsche Änderungen definieren
DE_CHANGES=$(cat << EOF
- Mehrsprachigkeit hinzugefügt: Deutsch, Spanisch, Französisch, Chinesisch und Koreanisch
- Automatische Auswahl des Roboters in der Zielfarbe beim Spielstart
- Verbesserte Barrierefreiheit:
  - Steuerungselemente im Accessibility-Modus bleiben jetzt immer sichtbar
  - Fehler bei der Erkennung von Wandpositionen in der Sprachausgabe behoben
  - mehr Sprachausgaben
- Fehlende Außenwände im Süden und Osten wurden behoben
- Roboterpfade werden beim Start eines neuen Spiels zurückgesetzt
- Squares Moved wird bei neuem Spiel zurückgesetzt
- Roboteranimationen sind jetzt schneller
- Den nächsten Hint nur anzeigen, wenn der Roboter den passenden Zug gemacht hat
- Verhindern von Karten mit Lösung in nur einem Zug
- Neuer Multi-Target Mode (beta) in Settings
- Im Save Screen wird nun die Kartengröße und der Spielfortschritt gespeichert.
- Im Fortgeschrittenen Modus werden nun Lösungen mit mindestens 6-10 Zügen erwartet
- Share button: Man kann Karten mit Lösungen teilen
- Man kann das Spiel von externen Seiten starten mit dem URL Schema 'roboyard://'
EOF
)

# Englische Übersetzung
EN_CHANGES=$(cat << EOF
- Added German, Spanish, French, Chinese and Korean translation
- Automatically select the robot that matches the target color at game start
- Improved accessibility:
  - Controls remain visible in accessibility mode
  - Fixed incorrect wall position detection in screen reader announcements
  - Added more announcements
- Fixed missing outer walls (south and east)
- Robot paths are now cleared when starting a new game
- Squares Moved is reset when starting a new game
- Faster robot animations
- only advance hint if the suggested move is made
- Prevent maps with solution one move
- Added Multi-Target Mode (beta) in Settings with radio buttons.
- Added board size and completion status to save files.
- advanced mode now requires solutions with at least 6-10 moves
- Improved Sharing: Added support for sharing maps with solutions
- Added support for deep links: You can now open games via the URL scheme 'roboyard://'
EOF
)

# Play Store has a limit of 500 characters
PLAYSTORE_DE_DE=$(cat << EOF
<de-DE>
- Mehrsprachigkeit hinzugefügt: Deutsch, Spanisch, Französisch, Chinesisch und Koreanisch
- Automatische Auswahl des Roboters in der Zielfarbe beim Spielstart
- Verbesserte Barrierefreiheit und Fehlerbehebungen
- Fehlende Außenwände im Süden und Osten wurden behoben
- Zurücksetzen von Roboterpfaden und Zugzähler bei neuem Spiel
- Schnellere Roboteranimationen
- Neuer Multi-Target Mode (beta) in den Einstellungen
- Verbesserter Teilen-Modus und Deep-Link-Unterstützung
</de-DE>
EOF
)

PLAYSTORE_EN_GB=$(cat << EOF
<en-GB>
- Added German, Spanish, French, Chinese and Korean translation
- Automatically select the robot that matches the target color at game start
- Improved accessibility features and fixed bugs
- Fixed missing outer walls (south and east)
- Robot paths and Squares Moved are now reset when starting a new game
- Faster robot animations
- Added Multi-Target Mode (beta) in Settings
- Added board size and completion status to save files
- Improved Sharing and deep links support
</en-GB>
EOF
)

# Fastlane-Changelogs erstellen
# Deutsches Changelog
DE_CHANGELOG_FILE="$BASE_DIR/de/changelogs/${VERSION_NAME}.txt"
mkdir -p "$(dirname "$DE_CHANGELOG_FILE")"
echo "$DE_CHANGES" > "$DE_CHANGELOG_FILE"

# Englisches Changelog
EN_CHANGELOG_FILE="$BASE_DIR/en-US/changelogs/${VERSION_NAME}.txt"
mkdir -p "$(dirname "$EN_CHANGELOG_FILE")"
echo "$EN_CHANGES" > "$EN_CHANGELOG_FILE"

# Play Store spezifische Changelogs erstellen
mkdir -p "$PLAYSTORE_DIR"
PLAYSTORE_CHANGELOG_FILE="$PLAYSTORE_DIR/changelog_${VERSION_NAME}.txt"
echo "# Play Store Changelogs für Version $VERSION_NAME" > "$PLAYSTORE_CHANGELOG_FILE"
echo "" >> "$PLAYSTORE_CHANGELOG_FILE"
echo "$PLAYSTORE_EN_GB" >> "$PLAYSTORE_CHANGELOG_FILE"
echo "" >> "$PLAYSTORE_CHANGELOG_FILE"
echo "$PLAYSTORE_DE_DE" >> "$PLAYSTORE_CHANGELOG_FILE"

# Überprüfen der Zeichenlänge für Play Store Changelogs
echo ""
echo "Prüfe Zeichenlänge der Play Store Changelogs:"
echo "-----------------------------------------------"
check_length "$PLAYSTORE_EN_GB" "en-GB"
echo ""
check_length "$PLAYSTORE_DE_DE" "de-DE"
echo "-----------------------------------------------"

# Original-Texte zur Referenz prüfen
echo ""
echo "Zum Vergleich - Zeichenlänge der vollständigen Änderungen:"
echo "-----------------------------------------------"
# Erstelle temporäre Files mit nur dem Text der Einträge (ohne XML-Tags)
TEMP_EN=$(mktemp)
echo "$EN_CHANGES" > "$TEMP_EN"
TEMP_DE=$(mktemp)
echo "$DE_CHANGES" > "$TEMP_DE"
check_length "$(cat $TEMP_EN)" "Vollständiges EN"
echo ""
check_length "$(cat $TEMP_DE)" "Vollständiges DE"
rm "$TEMP_EN" "$TEMP_DE"
echo "-----------------------------------------------"

# CHANGELOG.md aktualisieren (nur Englisch)
if [ ! -f "$CHANGELOG_MD" ]; then
  echo "# Changelog" > "$CHANGELOG_MD"
  echo "=========" >> "$CHANGELOG_MD"
  echo "" >> "$CHANGELOG_MD"
else
  # Temporäre Datei erstellen
  TEMP_FILE=$(mktemp)
  
  # Die ersten 3 Zeilen beibehalten (Titel und Trennlinie)
  head -n 3 "$CHANGELOG_MD" > "$TEMP_FILE"
  
  # Neue Einträge nach der Trennlinie hinzufügen
  echo "" >> "$TEMP_FILE"
  echo "## Version ${VERSION_NAME} (${CURRENT_DATE})" >> "$TEMP_FILE"
  echo "$EN_CHANGES" >> "$TEMP_FILE"
  echo "" >> "$TEMP_FILE"
  
  # Restlichen Inhalt anhängen, aber die ersten 3 Zeilen überspringen
  tail -n +4 "$CHANGELOG_MD" >> "$TEMP_FILE"
  
  # Temporäre Datei in die Originaldatei verschieben
  mv "$TEMP_FILE" "$CHANGELOG_MD"
fi

# CHANGELOG_de.md aktualisieren (nur Deutsch)
if [ ! -f "$CHANGELOG_DE_MD" ]; then
  echo "# Changelog" > "$CHANGELOG_DE_MD"
  echo "=========" >> "$CHANGELOG_DE_MD"
  echo "" >> "$CHANGELOG_DE_MD"
else
  # Temporäre Datei erstellen
  TEMP_FILE=$(mktemp)
  
  # Die ersten 3 Zeilen beibehalten (Titel und Trennlinie)
  head -n 3 "$CHANGELOG_DE_MD" > "$TEMP_FILE"
  
  # Neue Einträge nach der Trennlinie hinzufügen
  echo "" >> "$TEMP_FILE"
  echo "## Version ${VERSION_NAME} (${CURRENT_DATE})" >> "$TEMP_FILE"
  echo "$DE_CHANGES" >> "$TEMP_FILE"
  echo "" >> "$TEMP_FILE"
  
  # Restlichen Inhalt anhängen, aber die ersten 3 Zeilen überspringen
  tail -n +4 "$CHANGELOG_DE_MD" >> "$TEMP_FILE"
  
  # Temporäre Datei in die Originaldatei verschieben
  mv "$TEMP_FILE" "$CHANGELOG_DE_MD"
fi

echo "Changelogs erfolgreich aktualisiert!"
echo "Fastlane-Changelogs: de/${VERSION_NAME}.txt und en-US/${VERSION_NAME}.txt"
echo "Play Store Changelogs: $PLAYSTORE_CHANGELOG_FILE"
echo ""
echo "Play Store Changelogs können so kopiert werden:"
echo "cat $PLAYSTORE_CHANGELOG_FILE"

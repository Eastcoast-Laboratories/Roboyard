#!/bin/bash

# Pfade definieren
BUILD_GRADLE="app/build.gradle"
BASE_DIR="fastlane/metadata/android"
CHANGELOG_MD="CHANGELOG.md"
CHANGELOG_DE_MD="CHANGELOG_de.md"

# Version aus build.gradle extrahieren
VERSION_NAME=$(grep versionName $BUILD_GRADLE | awk -F\" '{print $2}')

# Aktuelles Datum
CURRENT_DATE=$(date +"%Y-%m-%d")

# Deutsche Änderungen definieren
DE_CHANGES=$(cat << EOF
- Überarbeitete Benutzeroberfläche mit klareren Icons und besserer Lesbarkeit
- Barrierefreiheit für blinde Spieler
- Screenreader-Optimierungen & Sprachhinweise
- Button zum Wechseln zwischen Robotern mit Audio-Feedback und Screenreader-Unterstuetzung
EOF
)

# Englische Übersetzung
EN_CHANGES=$(cat << EOF
- Improved user interface with clearer icons and better readability
- Accessibility for blind players
- Screenreader optimizations & language hints
- Button to switch between robots with audio feedback and screenreader support
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

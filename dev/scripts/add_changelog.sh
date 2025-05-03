#!/bin/bash

# Paths
BUILD_GRADLE="app/build.gradle"
BASE_DIR="fastlane/metadata/android"
CHANGELOG_MD="CHANGELOG.md"
CHANGELOG_DE_MD="CHANGELOG_de.md"
PLAYSTORE_DIR="fastlane/metadata/android/playstore"

# Get version from build.gradle
VERSION_NAME=$(grep versionName $BUILD_GRADLE | awk -F\" '{print $2}')

# Get current date
CURRENT_DATE=$(date +"%Y-%m-%d")

# Play Store Zeichenlimit für Changelogs (pro Sprache)
PLAYSTORE_CHAR_LIMIT=500

# Function to check length and warn if exceeded
check_length() {
  local content="$1"
  local locale="$2"
  # Count only the actual changelog entries, not the XML tags
  local content_without_tags=$(echo "$content" | grep -v "^<.*>$")
  local chars=$(echo "$content_without_tags" | wc -c)
  local limit=$PLAYSTORE_CHAR_LIMIT
  
  echo "Changelog for $locale: $chars characters"
  
  if [ $chars -gt $limit ]; then
    local over=$((chars - limit))
    echo "⚠️ WARNING: Changelog for $locale is $over characters too long! (Maximum: $limit)"
    return 1
  else
    local remaining=$((limit - chars))
    echo "✔️ OK: $remaining characters remaining."
    return 0
  fi
}

# German Changelog
DE_CHANGES=$(cat << EOF
- Fullscreen Option in Settings
EOF
)

# English Changelog
EN_CHANGES=$(cat << EOF
- Fullscreen toggle option in settings
- enhance swipe-to-move: continuous swiping
- allow 5 Robots when opening external Maps
EOF
)

# Play Store has a limit of 500 characters
PLAYSTORE_DE_DE=$(cat << EOF
<de-DE>
- Fullscreen Option in Settings
</de-DE>
EOF
)

PLAYSTORE_EN_GB=$(cat << EOF
<en-GB>
- Fullscreen toggle option in settings
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
echo "$PLAYSTORE_DE_DE" >> "$PLAYSTORE_CHANGELOG_FILE"

# check length of Play Store Changelogs
echo ""
echo "Prüfe Zeichenlänge der Play Store Changelogs:"
echo "-----------------------------------------------"
check_length "$PLAYSTORE_EN_GB" "en-GB"
echo ""
check_length "$PLAYSTORE_DE_DE" "de-DE"
echo "-----------------------------------------------"

# Original-Text for Reference check length
echo ""
echo "For Reference - check length of full changes:"
echo "-----------------------------------------------"
# create temporary files with only the text of the entries (without XML tags)
TEMP_EN=$(mktemp)
echo "$EN_CHANGES" > "$TEMP_EN"
TEMP_DE=$(mktemp)
echo "$DE_CHANGES" > "$TEMP_DE"
check_length "$(cat $TEMP_EN)" "Full EN"
echo ""
check_length "$(cat $TEMP_DE)" "Full DE"
rm "$TEMP_EN" "$TEMP_DE"
echo "-----------------------------------------------"

# CHANGELOG.md aktualisieren (nur Englisch)
if [ ! -f "$CHANGELOG_MD" ]; then
  echo "# Changelog" > "$CHANGELOG_MD"
  echo "=========" >> "$CHANGELOG_MD"
  echo "" >> "$CHANGELOG_MD"
else
  # create temporary file
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
  # create temporary file
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

echo "Changelogs successfully updated!"
echo "Fastlane-Changelogs: de/${VERSION_NAME}.txt und en-US/${VERSION_NAME}.txt"
echo "Play Store Changelogs: $PLAYSTORE_CHANGELOG_FILE"
echo ""
echo "Play Store Changelogs can be copied like this:"
echo "cat $PLAYSTORE_CHANGELOG_FILE"

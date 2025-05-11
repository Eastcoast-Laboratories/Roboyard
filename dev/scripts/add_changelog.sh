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
- Verbesserte Accessibility-Übersetzungen
- Alle Koordinaten in der Accessibility-Announcement von 1,1 bis 8,8 statt von 0,0 bis 7,7
- Sound-Einstellung funktioniert jetzt korrekt
- Gewinnbedingung bei mehreren Zielen prüft jetzt korrekt, ob die gewählte Anzahl an Robotern auf ihren Zielen steht
- Mehrfarben-Ziele wiederhergestellt im Beginner Mode
EOF
)

# English Changelog
EN_CHANGES=$(cat << EOF
- Enhanced accessibility translations
- Make all accessibility coordinate announcements from 1,1 to 8,8 instead of 0,0 to 7,7
- Fixed sound settings to properly respect user preferences
- Fixed win condition to correctly check if the selected number of robots are at their targets
- Freenable multi-color targets in Beginner mode
EOF
)

# Play Store has a limit of 500 characters
PLAYSTORE_DE_DE=$(cat << EOF
<de-DE>
</de-DE>
EOF
)

PLAYSTORE_EN_GB=$(cat << EOF
<en-GB>
</en-GB>
EOF
)

# If PLAYSTORE sections are empty, use content from DE_CHANGES and EN_CHANGES
if [ "$(echo "$PLAYSTORE_DE_DE" | grep -v '^<.*>$' | tr -d '\n' | tr -d ' ')" = "" ]; then
  PLAYSTORE_DE_DE=$(cat << EOF
<de-DE>
$DE_CHANGES
</de-DE>
EOF
)
  echo "Using DE_CHANGES for PLAYSTORE_DE_DE as it was empty"
fi

if [ "$(echo "$PLAYSTORE_EN_GB" | grep -v '^<.*>$' | tr -d '\n' | tr -d ' ')" = "" ]; then
  PLAYSTORE_EN_GB=$(cat << EOF
<en-GB>
$EN_CHANGES
</en-GB>
EOF
)
  echo "Using EN_CHANGES for PLAYSTORE_EN_GB as it was empty"
fi

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
echo "check length of Play Store Changelogs:"
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

# CHANGELOG.md update (only English)
if [ ! -f "$CHANGELOG_MD" ]; then
  echo "# Changelog" > "$CHANGELOG_MD"
  echo "=========" >> "$CHANGELOG_MD"
  echo "" >> "$CHANGELOG_MD"
else
  # create temporary file
  TEMP_FILE=$(mktemp)
  
  # Check if the current version already exists in the changelog
  if grep -q "## Version ${VERSION_NAME}" "$CHANGELOG_MD"; then
    # Version exists, update it instead of adding a new entry
    echo "Updating existing entry for Version ${VERSION_NAME} in CHANGELOG.md"
    
    # Get line number of the version header
    VERSION_LINE=$(grep -n "## Version ${VERSION_NAME}" "$CHANGELOG_MD" | cut -d ':' -f1)
    
    # Copy everything before the version header (excluding the version line itself)
    head -n $((VERSION_LINE - 1)) "$CHANGELOG_MD" > "$TEMP_FILE"
    
    # Update the version header with current date
    echo "## Version ${VERSION_NAME} (${CURRENT_DATE})" >> "$TEMP_FILE"
    
    # Add the updated changes
    echo "$EN_CHANGES" >> "$TEMP_FILE"
    echo "" >> "$TEMP_FILE"
    
    # Find the next version header or end of file
    NEXT_VERSION_LINE=$(tail -n +$((VERSION_LINE + 1)) "$CHANGELOG_MD" | grep -n "^## Version" | head -n 1 | cut -d ':' -f1)
    
    if [ -n "$NEXT_VERSION_LINE" ]; then
      # If there is a next version, add everything from that line onwards
      NEXT_VERSION_LINE=$((VERSION_LINE + NEXT_VERSION_LINE))
      tail -n +$NEXT_VERSION_LINE "$CHANGELOG_MD" >> "$TEMP_FILE"
    fi
  else
    # Version doesn't exist, add a new entry
    # keep first 3 lines (title and separator)
    head -n 3 "$CHANGELOG_MD" > "$TEMP_FILE"
    
    # add new entry after separator
    echo "" >> "$TEMP_FILE"
    echo "## Version ${VERSION_NAME} (${CURRENT_DATE})" >> "$TEMP_FILE"
    echo "$EN_CHANGES" >> "$TEMP_FILE"
    echo "" >> "$TEMP_FILE"
    
    # append rest of original file, but skip first 3 lines
    tail -n +4 "$CHANGELOG_MD" >> "$TEMP_FILE"
  fi
  
  # move temporary file back to original file
  mv "$TEMP_FILE" "$CHANGELOG_MD"
fi

# CHANGELOG_de.md update (only German)
if [ ! -f "$CHANGELOG_DE_MD" ]; then
  echo "# Changelog" > "$CHANGELOG_DE_MD"
  echo "=========" >> "$CHANGELOG_DE_MD"
else
  # create temporary file
  TEMP_FILE=$(mktemp)
  
  # Check if the current version already exists in the changelog
  if grep -q "## Version ${VERSION_NAME}" "$CHANGELOG_DE_MD"; then
    # Version exists, update it instead of adding a new entry
    echo "Updating existing entry for Version ${VERSION_NAME} in CHANGELOG_de.md"
    
    # Get line number of the version header
    VERSION_LINE=$(grep -n "## Version ${VERSION_NAME}" "$CHANGELOG_DE_MD" | cut -d ':' -f1)
    
    # Copy everything before the version header (excluding the version line itself)
    head -n $((VERSION_LINE - 1)) "$CHANGELOG_DE_MD" > "$TEMP_FILE"
    
    # Update the version header with current date
    echo "## Version ${VERSION_NAME} (${CURRENT_DATE})" >> "$TEMP_FILE"
    
    # Add the updated changes
    echo "$DE_CHANGES" >> "$TEMP_FILE"
    echo "" >> "$TEMP_FILE"
    
    # Find the next version header or end of file
    NEXT_VERSION_LINE=$(tail -n +$((VERSION_LINE + 1)) "$CHANGELOG_DE_MD" | grep -n "^## Version" | head -n 1 | cut -d ':' -f1)
    
    if [ -n "$NEXT_VERSION_LINE" ]; then
      # If there is a next version, add everything from that line onwards
      NEXT_VERSION_LINE=$((VERSION_LINE + NEXT_VERSION_LINE))
      tail -n +$NEXT_VERSION_LINE "$CHANGELOG_DE_MD" >> "$TEMP_FILE"
    fi
  else
    # Version doesn't exist, add a new entry
    # keep first 3 lines (title and separator)
    head -n 3 "$CHANGELOG_DE_MD" > "$TEMP_FILE"
    
    # add new entry after separator
    echo "" >> "$TEMP_FILE"
    echo "## Version ${VERSION_NAME} (${CURRENT_DATE})" >> "$TEMP_FILE"
    echo "$DE_CHANGES" >> "$TEMP_FILE"
    echo "" >> "$TEMP_FILE"
    
    # append rest of original file, but skip first 3 lines
    tail -n +4 "$CHANGELOG_DE_MD" >> "$TEMP_FILE"
  fi
  
  # move temporary file back to original file
  mv "$TEMP_FILE" "$CHANGELOG_DE_MD"
fi

echo "Changelogs successfully updated!"
echo "Fastlane-Changelogs: de/${VERSION_NAME}.txt und en-US/${VERSION_NAME}.txt"
echo "Play Store Changelogs: $PLAYSTORE_CHANGELOG_FILE"
echo ""
echo "Play Store Changelogs can be copied like this:"
echo "cat $PLAYSTORE_CHANGELOG_FILE"

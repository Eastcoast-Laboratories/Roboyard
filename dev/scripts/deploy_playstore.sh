#!/bin/bash
#
# Deploy script for Google Play Store
# Prepares all necessary files for Play Games Services and builds the release APK
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PGS_IMPORT_DIR="$PROJECT_ROOT/dev/pgs_import"

echo "=============================================="
echo "  Roboyard Play Store Deployment Script"
echo "=============================================="
echo ""

# Step 1: Generate PGS import files
echo "Step 1: Generating Play Games Services import files..."
python3 "$SCRIPT_DIR/generate_pgs_import.py"
echo ""

# Step 2: Create ZIP for Play Console import
echo "Step 2: Creating achievements.zip for Play Console import..."
cd "$PGS_IMPORT_DIR"
rm -f achievements.zip
zip achievements.zip AchievementsMetadata.csv
echo "  Created: $PGS_IMPORT_DIR/achievements.zip"
echo ""

# Step 3: Build release APK
echo "Step 3: Building release APK..."
cd "$PROJECT_ROOT"
./gradlew clean assembleRelease

# Find the APK
APK_PATH=$(find "$PROJECT_ROOT/app/build/outputs/apk/release" -name "*.apk" | head -1)
if [ -n "$APK_PATH" ]; then
    echo "  Release APK: $APK_PATH"
else
    echo "  Warning: Release APK not found"
fi
echo ""

# Step 4: Summary
echo "=============================================="
echo "  Deployment Preparation Complete!"
echo "=============================================="
echo ""
echo "Files created:"
echo "  - $PGS_IMPORT_DIR/achievements.zip"
echo "  - $PGS_IMPORT_DIR/AchievementsMetadata.csv"
if [ -n "$APK_PATH" ]; then
    echo "  - $APK_PATH"
fi
echo ""
echo "Next steps:"
echo "  1. Upload achievements.zip to Play Console:"
echo "     Grow > Play Games Services > Achievements > Import"
echo ""
echo "  2. After import, copy each Achievement ID from Play Console"
echo "     and replace REPLACE_WITH_ACHIEVEMENT_ID in:"
echo "     app/src/main/res/values/games-ids.xml"
echo ""
echo "  3. Also set your App ID in games-ids.xml:"
echo "     <string name=\"games_app_id\">YOUR_APP_ID</string>"
echo ""
echo "  4. Rebuild and upload APK to Play Console"
echo ""
echo "  5. Publish Play Games Services (separate from app publish!)"
echo ""

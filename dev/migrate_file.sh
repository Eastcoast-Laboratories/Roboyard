#!/bin/bash

# Migration Script for Roboyard
# This script moves a file from roboyard.eclabs to a specific target package

# Check if filename parameter is provided
if [ $# -lt 1 ]; then
    echo "Usage: 
    cp $0 /var/tmp/$0
    /var/tmp/$0 <FileName> [TargetPackage]
    cp /var/tmp/$0 /var/www/Roboyard/dev/migrate_file.sh"
    echo "Example: cp $0 /var/tmp/$0; /var/tmp/$0 GameMovementInterface.java roboyard.ui.components; cp /var/tmp/$0 /var/www/Roboyard/dev/migrate_file.sh"
    exit 1
fi

# Get filename from parameter
FILENAME="$1"
# Default target package is roboyard.ui.components
TARGET_PACKAGE="${2:-roboyard.ui.components}"

PROJECT_DIR="/var/www/Roboyard"
cd "$PROJECT_DIR" || exit 1

echo "Starting Migration Script for $FILENAME..."

# Reset to a clean state
echo "Resetting to latest commit..."
git reset --hard HEAD
git clean -f
echo "Git reset completed."

# Create target directories - parse package into directory structure
TARGET_DIR="app/src/main/java/$(echo $TARGET_PACKAGE | tr '.' '/')"
echo "Creating target directory: $TARGET_DIR"
mkdir -p "$TARGET_DIR"

# Prepare source and target paths
SOURCE="app/src/main/java/roboyard/eclabs/$FILENAME"
TARGET="$TARGET_DIR/$FILENAME"

if [ -f "$SOURCE" ]; then
    echo "Moving $FILENAME to $TARGET_PACKAGE..."
    
    # Use git mv to preserve file history
    git mv "$SOURCE" "$TARGET"
    
    # Update package declaration
    sed -i "1s/package roboyard\.eclabs;/package $TARGET_PACKAGE;/" "$TARGET"
    
    echo "Moved $SOURCE to $TARGET"
    
    # Add imports for common eclabs classes
    echo "Adding necessary imports..."
    
    # Create an array of potential class dependencies
    CLASSES=("GameManager" "InputManager" "RenderManager" "IGameObject" "Constants"
             "GamePiece" "GridGameScreen" "GameScreen" "GameButton" "GameDropdown"
             "Game*" "AccessibilityUtil")
    
    # Add package import for Android R class if R is referenced
    if grep -q "R\.drawable" "$TARGET" || grep -q "R\.id" "$TARGET" || grep -q "R\.string" "$TARGET"; then
        echo "Adding import for Android resources..."
        sed -i '/^package/a import roboyard.eclabs.R;' "$TARGET"
    fi
    
    # Add imports for all referenced classes
    for CLASS in "${CLASSES[@]}"; do
        # Handle AccessibilityUtil specially since it's in a subpackage
        if [ "$CLASS" = "AccessibilityUtil" ]; then
            if grep -q "AccessibilityUtil" "$TARGET"; then
                # Create AccessibilityUtil if it doesn't exist
                if [ ! -f "app/src/main/java/roboyard/eclabs/util/AccessibilityUtil.java" ]; then
                    echo "Creating AccessibilityUtil class..."
                    mkdir -p app/src/main/java/roboyard/eclabs/util
                    cat > app/src/main/java/roboyard/eclabs/util/AccessibilityUtil.java << 'EOF'
package roboyard.eclabs.util;

import android.content.Context;
import android.view.accessibility.AccessibilityManager;

public class AccessibilityUtil {
    public static boolean isScreenReaderActive(Context context) {
        if (context == null) return false;
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled() && am.isTouchExplorationEnabled();
    }
    
    public static void announceForAccessibility(Context context, String message) {
        // Implementation would go here
    }
}
EOF
                fi
                sed -i '/^package/a import roboyard.eclabs.util.AccessibilityUtil;' "$TARGET"
            fi
        # For GameDropdown, check if it's already been moved and import from the right place
        elif [ "$CLASS" = "GameDropdown" ]; then
            if grep -q "GameDropdown" "$TARGET"; then
                if [ -f "app/src/main/java/roboyard/ui/components/GameDropdown.java" ]; then
                    sed -i '/^package/a import roboyard.ui.components.GameDropdown;' "$TARGET"
                else
                    sed -i '/^package/a import roboyard.eclabs.GameDropdown;' "$TARGET"
                fi
            fi
        # For wildcard classes, we need to use grep differently
        elif [[ "$CLASS" == *"*"* ]]; then
            # Remove the * for grep
            CLASS_PREFIX=$(echo "$CLASS" | sed 's/\*//')
            # Find all classes matching the pattern
            for MATCHING_CLASS in $(grep -l "class $CLASS_PREFIX" app/src/main/java/roboyard/eclabs/*.java 2>/dev/null | sed 's/.*\///;s/\.java//');
            do
                if grep -q "$MATCHING_CLASS" "$TARGET"; then
                    sed -i "/^package/a import roboyard.eclabs.$MATCHING_CLASS;" "$TARGET"
                fi
            done
        # Normal class check
        else
            if grep -q "$CLASS" "$TARGET"; then
                # Check if the class has been moved to UI components
                if [ -f "app/src/main/java/roboyard/ui/components/$CLASS.java" ]; then
                    sed -i "/^package/a import roboyard.ui.components.$CLASS;" "$TARGET"
                else
                    sed -i "/^package/a import roboyard.eclabs.$CLASS;" "$TARGET"
                fi
            fi
        fi
    done
    
    # Update any files that might be importing this class
    echo "Updating imports in other files..."
    BASE_FILENAME=$(basename "$FILENAME" .java)
    find app/src/main/java -name "*.java" -type f | grep -v "$TARGET" | xargs grep -l "import roboyard.eclabs.$BASE_FILENAME" 2>/dev/null | while read -r file; do
        echo "Updating imports in $file"
        sed -i "s/import roboyard\.eclabs\.$BASE_FILENAME;/import $TARGET_PACKAGE.$BASE_FILENAME;/g" "$file"
    done
    
    # Also find and update any files with instanceof checks that might not have imports
    find app/src/main/java -name "*.java" -type f | grep -v "$TARGET" | xargs grep -l "instanceof $BASE_FILENAME" 2>/dev/null | while read -r file; do
        # Only add import if the file doesn't already import the class
        if ! grep -q "import .*$BASE_FILENAME" "$file"; then
            echo "Adding import to $file for instanceof check"
            sed -i "/^package/a import $TARGET_PACKAGE.$BASE_FILENAME;" "$file"
        fi
    done
    
    # Update migration strategy document
    echo "Updating migration strategy document..."
    if [ -f "dev/Code_Migration_Strategy.md" ]; then
        ESCAPED_FILENAME="$(echo $FILENAME | sed 's/\./\\./g')"
        sed -i "s/|\`$ESCAPED_FILENAME\`.*| Move to.*| \`$TARGET_PACKAGE\` |.*/|\`$FILENAME\`         | (done) UI components | \`$TARGET_PACKAGE\` | Migrated successfully|/g" "dev/Code_Migration_Strategy.md"
    fi
else
    echo "Source file $SOURCE not found"
    exit 1
fi

# Build to check for errors
echo "Running gradle build to check for compilation errors..."
./gradlew clean
./gradlew assembleDebug

echo "Migration completed. Check the build results for any errors to address."

# stage all
git add .
echo "Code Migration: move $FILENAME to $TARGET_PACKAGE" > .git/COMMIT_EDITMSG

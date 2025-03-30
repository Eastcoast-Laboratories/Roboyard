#!/bin/bash

# Simple Migration Script for Roboyard
# This script focuses on moving only GameDropdown.java

PROJECT_DIR="/var/www/Roboyard"
cd "$PROJECT_DIR" || exit 1

echo "Starting Simple Migration Script for GameDropdown.java..."

# Reset to a clean state
echo "Resetting to latest commit..."
git reset --hard HEAD
git clean -f
echo "Git reset completed."

# Create target directories
echo "Creating target directories..."
mkdir -p app/src/main/java/roboyard/ui/components
mkdir -p app/src/main/java/roboyard/eclabs/util

# Create AccessibilityUtil if it doesn't exist
if [ ! -f "app/src/main/java/roboyard/eclabs/util/AccessibilityUtil.java" ]; then
    echo "Creating AccessibilityUtil class..."
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

# Move GameDropdown.java to UI components
echo "Moving GameDropdown.java to UI components..."
SOURCE="app/src/main/java/roboyard/eclabs/GameDropdown.java"
TARGET="app/src/main/java/roboyard/ui/components/GameDropdown.java"

if [ -f "$SOURCE" ]; then
    # Use git mv to preserve file history
    git mv "$SOURCE" "$TARGET"
    
    # Update package declaration
    sed -i '1s/package roboyard\.eclabs;/package roboyard.ui.components;/' "$TARGET"
    
    echo "Moved $SOURCE to $TARGET"
    
    # Add missing imports
    echo "Adding necessary imports to GameDropdown.java..."
    sed -i '/^package/a import roboyard.eclabs.Constants;\nimport roboyard.eclabs.GameManager;\nimport roboyard.eclabs.InputManager;\nimport roboyard.eclabs.RenderManager;\nimport roboyard.eclabs.IGameObject;\nimport roboyard.eclabs.util.AccessibilityUtil;' "$TARGET"
    
    # Update any files that might be importing GameDropdown
    echo "Updating imports in other files..."
    find app/src/main/java -name "*.java" -type f | grep -v "$TARGET" | xargs grep -l "import roboyard.eclabs.GameDropdown" 2>/dev/null | while read -r file; do
        echo "Updating imports in $file"
        sed -i 's/import roboyard\.eclabs\.GameDropdown;/import roboyard.ui.components.GameDropdown;/g' "$file"
    done
    
    # Update migration strategy document
    echo "Updating migration strategy document..."
    if [ -f "dev/Code_Migration_Strategy.md" ]; then
        sed -i 's/|`GameDropdown.java`         | Move to UI components | `roboyard.ui.components` | Android Spinner adapter dependency/|`GameDropdown.java`         | Moved | `roboyard.ui.components` | Android Spinner adapter dependency/g' "dev/Code_Migration_Strategy.md"
    fi
else
    echo "Source file $SOURCE not found"
fi

# Build to check for errors
echo "Running gradle build to check for compilation errors..."
./gradlew clean
./gradlew assembleDebug

echo "Simple migration completed. Check the build results for any errors to address."

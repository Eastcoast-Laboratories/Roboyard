#!/bin/bash

# Migration Script for Roboyard
# This script moves a file from roboyard.eclabs to a specific target package

# Check if filename parameter is provided
if [ $# -lt 1 ]; then
    echo "Usage: 
    cp $0 /var/tmp/$0
    /var/tmp/$0 <FileName> [TargetPackage]
    cp /var/tmp/$0 /var/www/Roboyard/dev/migrate_file.sh"
    echo "Example: cp $0 /var/tmp/$0; /var/tmp/$0 GameMovementInterface.java roboyard.ui.components; cp $0 /var/www/Roboyard/dev/migrate_file.sh"
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
BASE_FILENAME=$(basename "$FILENAME" .java)

# First, find all files that reference this class to later update them
echo "Finding all references to $BASE_FILENAME..."
REFERENCING_FILES=$(find app/src/main/java -name "*.java" -type f -exec grep -l "[^a-zA-Z0-9_]$BASE_FILENAME[^a-zA-Z0-9_]" {} \; | sort -u)

if [ -f "$SOURCE" ]; then
    echo "Moving $FILENAME to $TARGET_PACKAGE..."
    
    # Use git mv to preserve file history
    git mv "$SOURCE" "$TARGET"
    
    # Update package declaration
    sed -i "1s/package roboyard\.eclabs;/package $TARGET_PACKAGE;/" "$TARGET"
    
    echo "Moved $SOURCE to $TARGET"
    
    # Add imports for common eclabs classes
    echo "Adding necessary imports..."
    
    # First identify which classes are needed by the moved file
    NEEDED_CLASSES=$(grep -o "[A-Z][a-zA-Z0-9_]*" "$TARGET" | sort -u)
    
    # Add package import for Android R class if R is referenced
    if grep -q "R\.drawable" "$TARGET" || grep -q "R\.id" "$TARGET" || grep -q "R\.string" "$TARGET"; then
        echo "Adding import for Android resources..."
        sed -i '/^package/a import roboyard.eclabs.R;' "$TARGET"
    fi
    
    # Import required Android classes
    if grep -q "Context\|Activity\|MediaPlayer\|Canvas" "$TARGET"; then
        echo "Adding imports for Android classes..."
        if grep -q "Context" "$TARGET"; then
            sed -i '/^package/a import android.content.Context;' "$TARGET"
        fi
        if grep -q "Activity" "$TARGET"; then
            sed -i '/^package/a import android.app.Activity;' "$TARGET"
        fi
        if grep -q "MediaPlayer" "$TARGET"; then
            sed -i '/^package/a import android.media.MediaPlayer;' "$TARGET"
        fi
        if grep -q "Canvas" "$TARGET"; then
            sed -i '/^package/a import android.graphics.Canvas;' "$TARGET"
        fi
    fi
    
    # Add imports for static references from MainActivity
    if grep -q "MainActivity\.get" "$TARGET"; then
        echo "Adding import for MainActivity (static reference)..."
        sed -i '/^package/a import roboyard.eclabs.MainActivity;' "$TARGET"
    fi
    
    # Loop over all possible dependent classes
    for CLASS in $NEEDED_CLASSES; do
        # Skip primitive types, keywords, and standard classes
        if [[ "$CLASS" == "int" || "$CLASS" == "boolean" || "$CLASS" == "void" || 
              "$CLASS" == "float" || "$CLASS" == "double" || "$CLASS" == "String" || 
              "$CLASS" == "Override" || "$CLASS" == "final" || "$CLASS" == "static" || 
              "$CLASS" == "public" || "$CLASS" == "private" || "$CLASS" == "protected" ]]; then
            continue
        fi
        
        # Check if the class is in the roboyard codebase
        POTENTIAL_FILES=$(find app/src/main/java -path "*/roboyard/*" -name "${CLASS}.java")
        
        if [ -n "$POTENTIAL_FILES" ]; then
            # Class found in codebase, check for usage in the file
            if grep -q "[^a-zA-Z0-9_\.]$CLASS[^a-zA-Z0-9_]" "$TARGET"; then
                # Find the package for this class
                CLASS_PATH=$(echo "$POTENTIAL_FILES" | head -1)
                CLASS_PACKAGE=$(grep -m1 "^package" "$CLASS_PATH" | sed 's/package \([^;]*\);.*/\1/')
                
                # Don't add import for classes in the same package
                if [ "$CLASS_PACKAGE" != "$TARGET_PACKAGE" ]; then
                    echo "Adding import for $CLASS (from $CLASS_PACKAGE)..."
                    sed -i "/^package/a import $CLASS_PACKAGE.$CLASS;" "$TARGET"
                fi
            fi
        else 
            # Special handling for known classes that might not be found
            # Add your most common dependencies here for easy future reference
            case "$CLASS" in
                "GameManager"|"InputManager"|"RenderManager"|"IGameObject"|"Constants")
                    if grep -q "[^a-zA-Z0-9_\.]$CLASS[^a-zA-Z0-9_]" "$TARGET"; then
                        echo "Adding import for common class $CLASS..."
                        sed -i "/^package/a import roboyard.eclabs.$CLASS;" "$TARGET"
                    fi
                    ;;
                "MainActivity")
                    if grep -q "[^a-zA-Z0-9_\.]$CLASS[^a-zA-Z0-9_]" "$TARGET"; then
                        echo "Adding import for $CLASS..."
                        sed -i "/^package/a import roboyard.eclabs.$CLASS;" "$TARGET"
                    fi
                    ;;
                "Preferences")
                    if grep -q "[^a-zA-Z0-9_\.]$CLASS[^a-zA-Z0-9_]" "$TARGET"; then
                        echo "Adding import for $CLASS..."
                        sed -i "/^package/a import roboyard.eclabs.$CLASS;" "$TARGET"
                    fi
                    ;;
                "AccessibilityUtil")
                    if grep -q "[^a-zA-Z0-9_\.]$CLASS[^a-zA-Z0-9_]" "$TARGET"; then
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
                    ;;
            esac
        fi
    done
    
    # Special handling for static references
    echo "Checking for static references to $BASE_FILENAME..."
    for file in $(find app/src/main/java -name "*.java" -type f | grep -v "$TARGET"); do
        # Check for static references to the class (ClassName.method or ClassName.field)
        if grep -q "$BASE_FILENAME\." "$file"; then
            echo "Found static reference to $BASE_FILENAME in $file"
            
            # Add import if not already present
            if ! grep -q "import.*$BASE_FILENAME" "$file"; then
                echo "Adding import for static reference in $file"
                sed -i "/^package/a import $TARGET_PACKAGE.$BASE_FILENAME;" "$file"
            fi
        fi
    done
    
    # Special handling for instanceof pattern matching (Java 14+)
    echo "Checking for instanceof pattern matching with $BASE_FILENAME..."
    find app/src/main/java -name "*.java" -type f | xargs grep -l "instanceof $BASE_FILENAME [a-zA-Z]" 2>/dev/null | while read -r file; do
        echo "Found instanceof pattern matching with $BASE_FILENAME in $file"
        
        # Add import if not already present
        if ! grep -q "import.*$BASE_FILENAME" "$file"; then
            echo "Adding import for instanceof pattern matching in $file"
            sed -i "/^package/a import $TARGET_PACKAGE.$BASE_FILENAME;" "$file"
        fi
    done
    
    # Enhanced parameter type handling
    echo "Checking for class used as method parameter type..."
    POTENTIAL_METHOD_PARAMS=$(find app/src/main/java -name "*.java" -type f | grep -v "$TARGET")
    for file in $POTENTIAL_METHOD_PARAMS; do
        # Check if the file contains method declarations with the class as a parameter
        if grep -q "\b$BASE_FILENAME\b[[:space:]]\+[a-zA-Z0-9_]\+" "$file"; then
            echo "Found $BASE_FILENAME used as parameter type in $file"
            
            # Add import if not already present
            if ! grep -q "import.*$BASE_FILENAME" "$file"; then
                echo "Adding import for parameter type in $file"
                sed -i "/^package/a import $TARGET_PACKAGE.$BASE_FILENAME;" "$file"
            fi
        fi
    done
    
    # Now add imports to specific files that we know need them
    for known_file in "app/src/main/java/roboyard/eclabs/GridGameScreen.java" "app/src/main/java/roboyard/logic/core/Move.java" "app/src/main/java/roboyard/eclabs/MainActivity.java" "app/src/main/java/roboyard/eclabs/GameManager.java" "app/src/main/java/roboyard/eclabs/MapObjects.java"; do
        if [ -f "$known_file" ]; then
            echo "Adding import to known dependency: $known_file"
            if ! grep -q "import.*$BASE_FILENAME" "$known_file"; then
                sed -i "/^package/a import $TARGET_PACKAGE.$BASE_FILENAME;" "$known_file"
            fi
        fi
    done
    
    # Check all import statements in all files and update them
    echo "Checking import statements in all files..."
    find app/src/main/java -name "*.java" -type f | xargs grep -l "import roboyard.eclabs.$BASE_FILENAME" 2>/dev/null | while read -r file; do
        echo "Updating import in $file"
        sed -i "s/import roboyard\.eclabs\.$BASE_FILENAME;/import $TARGET_PACKAGE.$BASE_FILENAME;/g" "$file"
    done
    
    # Now check for direct class references without imports and add them
    echo "Checking class references without imports..."
    for file in $(find app/src/main/java -name "*.java" -type f | grep -v "$TARGET"); do
        # Check for any direct reference to the class
        if grep -q "[^a-zA-Z0-9_\.]$BASE_FILENAME[ ;()]|instanceof $BASE_FILENAME" "$file"; then
            # Only add import if the file doesn't already have any import for this class
            if ! grep -q "import .*$BASE_FILENAME" "$file"; then
                echo "Adding import to $file for direct class reference"
                sed -i "/^package/a import $TARGET_PACKAGE.$BASE_FILENAME;" "$file"
            fi
        fi
    done
    
    # Add automatic access modifier handling for package-private fields/methods
    echo "Checking for potential package access issues..."
    # First identify all package-private static members in classes that might need public access
    POTENTIAL_FILES=$(find app/src/main/java -name "*.java" -type f -not -path "*/$TARGET_PACKAGE/*")
    for file in $POTENTIAL_FILES; do
        # Get the classname of the file
        CLASS_NAME=$(basename "$file" .java)
        
        # Check if the migrated file accesses static members of this class that might be package-private
        if grep -q "$CLASS_NAME\.\w\+" "$TARGET"; then
            # Look for package-private static fields or methods in that file
            PACKAGE_PRIVATE_STATIC=$(grep -oE "static [^p][^u][^b][^l][^i][^c].*[a-zA-Z0-9_]+\s*[=(]" "$file" | sed 's/.*\s\([a-zA-Z0-9_]\+\)\s*[=(].*/\1/')
            
            if [ -n "$PACKAGE_PRIVATE_STATIC" ]; then
                echo "Found potential package-private static members in $file that may need to be made public"
                
                # For each package-private static member, check if it's accessed by the migrated file
                for member in $PACKAGE_PRIVATE_STATIC; do
                    if grep -q "$CLASS_NAME\.$member" "$TARGET"; then
                        echo "Making $member in $CLASS_NAME public for cross-package access"
                        # Replace static <type> with public static <type>
                        sed -i "s/static\s\+\([a-zA-Z0-9_.<>]\+\)\s\+$member/public static \1 $member/g" "$file"
                    fi
                done
            fi
        fi
    done
    
    # Update migration strategy document
    echo "\nUpdating migration strategy document..."
    if [ -f "dev/Code_Migration_Strategy.md" ]; then
        ESCAPED_FILENAME=$(echo "$FILENAME" | sed 's/\./\\./g')
        
        # Get the existing description from the file
        DESCRIPTION=$(grep -E "\|\`$ESCAPED_FILENAME\`" "dev/Code_Migration_Strategy.md" | sed -E 's/.*\| ([^|]*)\|$/\1/')
        
        # If no description found, use default
        if [ -z "$DESCRIPTION" ]; then
            DESCRIPTION=" Migrated successfully"
        fi
        
        # Update the migration strategy document, preserving the description
        sed -i "s/|\`$ESCAPED_FILENAME\`.*| Move to.*| \`$TARGET_PACKAGE\` |.*/|\`$FILENAME\`         | (done) UI components | \`$TARGET_PACKAGE\` |$DESCRIPTION|/g" "dev/Code_Migration_Strategy.md"
    fi
else
    echo "Source file $SOURCE not found"
    exit 1
fi

# Build to check for errors
echo "\nRunning gradle build to check for compilation errors..."
./gradlew clean
./gradlew assembleDebug

echo "\nMigration completed. Check the build results for any errors to address."

# stage all
git add .
echo "Code Migration: move $FILENAME to $TARGET_PACKAGE" > .git/COMMIT_EDITMSG

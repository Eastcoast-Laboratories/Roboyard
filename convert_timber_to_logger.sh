#!/bin/bash

# Script to convert Timber logging calls to Logger.println in driftingdroids directory
# This preserves the diagnostic information while adapting to the driftingdroids logging system

echo "Converting Timber logs to Logger.println in driftingdroids directory..."

# Find all Java files containing Timber in the driftingdroids directory
files=$(find app/src/main/java/driftingdroids -name "*.java" -type f -exec grep -l "Timber" {} \;)

if [ -z "$files" ]; then
    echo "No files found containing Timber logging calls."
    exit 0
fi

echo "Found files containing Timber logging calls:"
echo "$files"
echo ""

# Process each file
for file in $files; do
    echo "Processing $file..."
    
    # First, add import for Log if not already present
    grep -q "import android.util.Log;" "$file" || sed -i '1,/^import/s/^import/import android.util.Log;\nimport/' "$file"
    
    # Convert Timber.d() calls to Logger.println() with Log.DEBUG level
    sed -i 's/Timber\.d(\(.*\));/Logger.println(Log.DEBUG, "DriftingDroid", \1);/g' "$file"
    
    # Convert Timber.e() calls to Logger.println() with Log.ERROR level
    sed -i 's/Timber\.e(\(.*\));/Logger.println(Log.ERROR, "DriftingDroid", \1);/g' "$file"
    
    # Convert Timber.i() calls to Logger.println() with Log.INFO level
    sed -i 's/Timber\.i(\(.*\));/Logger.println(Log.INFO, "DriftingDroid", \1);/g' "$file"
    
    # Convert Timber.w() calls to Logger.println() with Log.WARN level
    sed -i 's/Timber\.w(\(.*\));/Logger.println(Log.WARN, "DriftingDroid", \1);/g' "$file"
    
    # Special case for simple string logs (no formatting)
    # This handles cases where Timber is used with just a string (no formatting)
    sed -i 's/Logger\.println(Log\.[A-Z]*, "DriftingDroid", "\([^%]*\)");/Logger.println("\1");/g' "$file"
    
    echo "Done processing $file"
done

echo ""
echo "Conversion complete. Please check the files for any remaining issues."
echo "Note: You may need to check for complex Timber usage patterns that weren't handled by this script."

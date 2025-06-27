#!/bin/bash

# Script to remove unused strings from all language files in Roboyard
# Usage: bash remove_unused_strings_from_all.sh [--dry-run] [--verbose]

# Path settings
SOURCE_DIR="/var/www/Roboyard"
STRINGS_DIR="$SOURCE_DIR/app/src/main/res"
LOG_DIR="$SOURCE_DIR/dev/logs"
TMP_DIR=$(mktemp -d)

# Parse command line arguments
DRY_RUN=false
VERBOSE=false

for arg in "$@"; do
  case $arg in
    --dry-run)
      DRY_RUN=true
      echo "Running in DRY RUN mode - no files will be modified"
      ;;
    --verbose)
      VERBOSE=true
      ;;
  esac
done

# Only create log directory if not in dry run mode
if [ "$DRY_RUN" = false ]; then
  mkdir -p "$LOG_DIR"
fi

# Cleanup function
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

# Step 1: First identify unused strings
echo "Step 1: Identifying unused strings..."

# Extract all string names from the English strings.xml
EN_STRINGS_FILE="$STRINGS_DIR/values/strings.xml"
grep -o 'name="[^"]*"' "$EN_STRINGS_FILE" | sed 's/name="//;s/"$//' | sort > "$TMP_DIR/all_strings.txt"
TOTAL_STRINGS=$(wc -l < "$TMP_DIR/all_strings.txt")
echo "Found $TOTAL_STRINGS strings in English strings.xml"

# Get list of all Java and XML files (excluding values/strings.xml files)
find "$SOURCE_DIR" -name "*.java" -o -name "*.xml" | grep -v "/values.*/strings\.xml" > "$TMP_DIR/source_files.txt"
TOTAL_FILES=$(wc -l < "$TMP_DIR/source_files.txt")
echo "Searching through $TOTAL_FILES source files for string usage..."

# Initialize unused strings file
UNUSED_STRINGS_FILE="$TMP_DIR/unused_strings.txt"
> "$UNUSED_STRINGS_FILE"

# Check each string for usage
echo "Checking each string for usage..."
PROGRESS_WIDTH=50
COUNT=0

while read -r string; do
  # Update progress
  COUNT=$((COUNT + 1))
  PERCENT=$((COUNT * 100 / TOTAL_STRINGS))
  FILLED_WIDTH=$((PERCENT * PROGRESS_WIDTH / 100))
  EMPTY_WIDTH=$((PROGRESS_WIDTH - FILLED_WIDTH))
  
  # Print progress bar
  BAR=$(printf "[%-${FILLED_WIDTH}s%-${EMPTY_WIDTH}s] %d%%" "$(printf '#%.0s' $(seq 1 $FILLED_WIDTH))" "$(printf ' %.0s' $(seq 1 $EMPTY_WIDTH))" "$PERCENT")
  printf "\r%s" "$BAR"
  
  # Special case for some strings that are referenced by convention
  if [[ "$string" == "app_name" || "$string" == "app_version" ]]; then
    continue
  fi
  
  # Search in all source files except strings.xml files
  # Exclude build outputs, generated files, and analysis directories
  DIRS_TO_EXCLUDE="--exclude-dir=build --exclude-dir=.gradle --exclude-dir=dev/translation_stats --exclude-dir=dev/translation_analysis --exclude-dir=dev/logs"
  
  # First check for Java/Kotlin/XML specific patterns in source files only
  if ! grep -q "R\.string\.${string}[),;]\|@string/${string}\|getString(.*${string}\|getResources.*.getString(.*${string}" \
     --exclude="strings.xml" $DIRS_TO_EXCLUDE -r "$SOURCE_DIR/app/src"; then
    
    # Then do a more thorough search for the raw string name in source code
    if ! grep -q "[^a-zA-Z0-9_]${string}[^a-zA-Z0-9_]\|\"${string}\"" \
       --exclude="strings.xml" $DIRS_TO_EXCLUDE -r "$SOURCE_DIR/app/src"; then
      
      # As a last check, search for exact string name in the gradle files (different pattern)
      if ! grep -q "${string}" --include="*.gradle" --include="*.properties" -r "$SOURCE_DIR"; then
        # String is not used anywhere in the actual source code
        echo "$string" >> "$UNUSED_STRINGS_FILE"
        
        if [ "$VERBOSE" = true ]; then
          echo "Found potentially unused string: $string"
        fi
      fi
    fi
  fi
done < "$TMP_DIR/all_strings.txt"

# Clear progress line
echo ""

# Count unused strings
UNUSED_COUNT=$(wc -l < "$UNUSED_STRINGS_FILE")
echo "Found $UNUSED_COUNT potentially unused strings"

# Step 2: Process each language file
echo -e "\nStep 2: Removing unused strings from all language files..."

# Get all values directories (contains the strings.xml files)
find "$STRINGS_DIR" -type d -name "values*" | sort > "$TMP_DIR/values_dirs.txt"

# Process each values directory
while read -r values_dir; do
  # Extract language code from directory name
  if [[ "$values_dir" == "$STRINGS_DIR/values" ]]; then
    LANG="en"
  else
    LANG=$(echo "$values_dir" | sed -E 's|.*/values-([a-zA-Z]+).*|\1|')
  fi
  
  STRINGS_XML="$values_dir/strings.xml"
  LOG_FILE="$LOG_DIR/remove_strings_${LANG}.log"
  
  echo "Processing $LANG strings file: $STRINGS_XML"
  if [ "$DRY_RUN" = false ]; then
    echo "Removed strings will be logged to: $LOG_FILE"
  else
    echo "[DRY RUN] No changes will be made to the file"
  fi
  
  # Create temp file for modified content
  TEMP_STRINGS="$TMP_DIR/strings_${LANG}.xml"
  
  # Initialize log file if not in dry run mode
  if [ "$DRY_RUN" = false ]; then
    echo "# Removed unused strings from $STRINGS_XML on $(date)" > "$LOG_FILE"
    echo "# Total strings removed: 0" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
  else
    # For dry run, just create a temporary log
    LOG_FILE="$TMP_DIR/dry_run_${LANG}.log"
    echo "# [DRY RUN] Strings that would be removed from $STRINGS_XML" > "$LOG_FILE"
    echo "" >> "$LOG_FILE"
  fi
  
  # Check if strings.xml exists
  if [ ! -f "$STRINGS_XML" ]; then
    echo "  Warning: $STRINGS_XML does not exist, skipping"
    continue
  fi
  
  # Count initial strings
  INITIAL_COUNT=$(grep -c '<string name=' "$STRINGS_XML")
  
  # Process the file
  REMOVED_COUNT=0
  
  # Create a clean copy of the file
  cp "$STRINGS_XML" "$TEMP_STRINGS"
  
  # Remove each unused string
  while read -r string_name; do
    # Check if this string exists in this language file
    if grep -q "<string name=\"$string_name\"" "$TEMP_STRINGS"; then
      # Get the entire string entry for logging
      STRING_ENTRY=$(grep -n "<string name=\"$string_name\"" "$TEMP_STRINGS" -A 1 | head -2)
      LINE_NUM=$(echo "$STRING_ENTRY" | head -1 | cut -d- -f1)
      
      # Log the removal
      echo "Removed line $LINE_NUM: $STRING_ENTRY" >> "$LOG_FILE"
      
      # Remove the string from the temporary file
      sed -i "/<string name=\"$string_name\"/,/<\/string>/d" "$TEMP_STRINGS"
      
      REMOVED_COUNT=$((REMOVED_COUNT + 1))
    fi
  done < "$UNUSED_STRINGS_FILE"
  
  # Update the log with the final count
  sed -i "s/# Total strings removed: 0/# Total strings removed: $REMOVED_COUNT/" "$LOG_FILE"
  
  # Only update the file if changes were made and we're not in dry-run mode
  if [ $REMOVED_COUNT -gt 0 ]; then
    if [ "$DRY_RUN" = false ]; then
      # Backup the original file
      cp "$STRINGS_XML" "${STRINGS_XML}.bak"
      
      # Replace with the new content
      cp "$TEMP_STRINGS" "$STRINGS_XML"
      
      # Count final strings
      FINAL_COUNT=$(grep -c '<string name=' "$STRINGS_XML")
      
      echo "  Removed $REMOVED_COUNT unused strings. Before: $INITIAL_COUNT, After: $FINAL_COUNT"
      echo "  Original file backed up to ${STRINGS_XML}.bak"
    else
      echo "  [DRY RUN] Would remove $REMOVED_COUNT unused strings from $STRINGS_XML"
      if [ "$VERBOSE" = true ]; then
        echo "  Strings that would be removed:"
        cat "$LOG_FILE" | grep -o "<string name=\".*\"" | sed 's/<string name="//;s/".*$//' | sort
      fi
    fi
  else
    echo "  No unused strings found in this file"
  fi
done < "$TMP_DIR/values_dirs.txt"

if [ "$UNUSED_COUNT" -gt 0 ]; then
  cp "$UNUSED_STRINGS_FILE" "./unused_strings.txt"
  echo -e "\nList of potentially unused strings saved to ./unused_strings.txt"
fi

if [ "$DRY_RUN" = true ]; then
  echo -e "\nDRY RUN completed. No files were modified."
  echo "Run without --dry-run to actually remove the strings."
else
  echo -e "\nAll done! Unused strings have been removed from all language files."
  echo "Check the logs in $LOG_DIR for details of what was removed from each file."
fi

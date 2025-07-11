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
UNUSED_STRINGS_FILE="$LOG_DIR/unused_strings.txt"

# Only create the file if not in dry run mode or create it in temp dir if in dry run mode
if [ "$DRY_RUN" = false ]; then
  > "$UNUSED_STRINGS_FILE"
else
  UNUSED_STRINGS_FILE="$TMP_DIR/unused_strings.txt"
  > "$UNUSED_STRINGS_FILE"
fi

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
      # Check if the string has comment="keep" attribute - if so, skip it
      if grep -q "<string name=\"$string_name\".*comment=\"keep\"" "$TEMP_STRINGS"; then
        if [ "$VERBOSE" = true ]; then
          echo "  Preserving string with comment=\"keep\": $string_name"
        fi
        continue
      fi
      
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
  # If in dry run mode, copy from temp directory to log directory
  if [ "$DRY_RUN" = true ]; then
    if [ ! -d "$LOG_DIR" ]; then
      mkdir -p "$LOG_DIR"
    fi
    cp "$UNUSED_STRINGS_FILE" "$LOG_DIR/unused_strings.txt"
  fi
  
  echo -e "\nList of potentially unused strings saved to $LOG_DIR/unused_strings.txt"
fi

# Step 3: Extract and compare with Lint data
echo -e "\nStep 3: Extracting Android Lint unused strings data..."

# Define paths for lint data
LINT_XML="$SOURCE_DIR/app/build/reports/lint-results-debug.xml"
LINT_UNUSED_FILE="$LOG_DIR/lint_unused_strings.txt"
LINT_COMPARISON_FILE="$LOG_DIR/lint_comparison.txt"

if [ ! -f "$LINT_XML" ]; then
  echo "Warning: Lint XML report not found at $LINT_XML"
  echo "Run Android Lint first to generate the report."
else
  echo "Found lint report at $LINT_XML"
  
  # Extract unused strings from Lint XML report
  echo "Extracting unused strings from Lint report..."
  grep -o 'message="The resource `R.string[^`]*' "$LINT_XML" | \
    sed 's/message="The resource `R.string.\([^`]*\).*/\1/' | \
    sort > "$LINT_UNUSED_FILE"
  
  # Count unused strings found by Lint
  LINT_UNUSED_COUNT=$(wc -l < "$LINT_UNUSED_FILE")
  echo "Found $LINT_UNUSED_COUNT unused strings reported by Android Lint"
  
  # Compare with our results
  echo "Comparing with script's findings..."
  
  # Create simple lint comparison files with only the differing strings
  # Strings only in script (not in lint)
  comm -23 "$UNUSED_STRINGS_FILE" "$LINT_UNUSED_FILE" > "$LINT_COMPARISON_FILE"
  
  # Count strings for summary output
  SCRIPT_ONLY_COUNT=$(wc -l < "$LINT_COMPARISON_FILE")
  LINT_ONLY_COUNT=$(comm -13 "$UNUSED_STRINGS_FILE" "$LINT_UNUSED_FILE" | wc -l)
  BOTH_COUNT=$(comm -12 "$UNUSED_STRINGS_FILE" "$LINT_UNUSED_FILE" | wc -l)

  echo "Comparison statistics:"
  echo "* Script found $UNUSED_COUNT unused strings"
  echo "* Android Lint found $LINT_UNUSED_COUNT unused strings"
  echo "* Strings found by both tools: $BOTH_COUNT"
  echo "* Strings only found by script: $SCRIPT_ONLY_COUNT"
  echo "* Strings only found by Lint: $LINT_ONLY_COUNT"
  
  echo "Differing strings saved to $LINT_COMPARISON_FILE"
fi

# Final messages
if [ "$DRY_RUN" = true ]; then
  echo -e "\nDRY RUN completed. No files were modified."
  echo "Run without --dry-run to actually remove the strings."
else
  echo -e "\nAll done! Unused strings have been removed from all language files."
  echo "Check the logs in $LOG_DIR for details of what was removed from each file."
fi

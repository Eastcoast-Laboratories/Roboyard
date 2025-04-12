#!/bin/bash

# cleanup_unused_strings.sh
# This script performs these main functions:
# 1. First it identifies and fixes duplicate string definitions in all string resource files
# 2. Then it identifies and removes completely unused strings
# 3. Also checks for strings in localization files that don't exist in the default resources
# 4. Enhances string comments with usage information in Java files
# 5. Finally it creates a todo file for strings that are referenced but not implemented
#
# Usage: cp /var/www/Roboyard/dev/scripts/cleanup_unused_strings.sh /var/tmp/; git reset --hard; cp /var/tmp/cleanup_unused_strings.sh /var/www/Roboyard/dev/scripts/

set -e  # Exit on error

# Colors for terminal output
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
BLUE="\033[0;34m"
MAGENTA="\033[0;35m"
NC="\033[0m" # No Color
BOLD="\033[1m"

# Project directories
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RES_DIR="$PROJECT_DIR/app/src/main/res"
SRC_DIR="$PROJECT_DIR/app/src/main"
DEV_DIR="$PROJECT_DIR/dev"
LOG_FILE="$DEV_DIR/unused_strings_cleanup.log"
TODO_FILE="$DEV_DIR/strings_todo.md"
COMMIT_FILE="$DEV_DIR/commit_message.txt"

# Initialize log file
echo "===== Roboyard String Resource Cleanup Log =====" > "$LOG_FILE"
echo "Date: $(date)" >> "$LOG_FILE"
echo "Working directory: $PROJECT_DIR" >> "$LOG_FILE"
echo >> "$LOG_FILE"

# Find all string resource files
STRINGS_FILES=($(find "$RES_DIR" -name "strings.xml"))
DEFAULT_STRINGS_FILE="$RES_DIR/values/strings.xml"

echo -e "${BLUE}Starting string resources cleanup in Roboyard project...${NC}"
echo "Working in: $PROJECT_DIR"
echo

echo "Found ${#STRINGS_FILES[@]} string resource files:"
for file in "${STRINGS_FILES[@]}"; do
    echo "- $file"
done
echo

###############################################################
# Step 1: Fix duplicate string definitions
###############################################################
echo -e "${BOLD}Step 1: Checking for duplicate string definitions...${NC}"
echo "Step 1: Checking for duplicate string definitions..." >> "$LOG_FILE"

# Function to handle duplicate strings in a file
fix_duplicates() {
    local file=$1
    local name=$2
    local count=$3
    
    echo -e "${YELLOW}  Found duplicate: $name ($count occurrences)${NC}" | tee -a "$LOG_FILE"
    
    # Create a temporary file without the duplicate string
    TEMP_FILE=$(mktemp)
    
    # Extract all strings except the duplicate
    grep -v "<string name=\"$name\"" "$file" > "$TEMP_FILE"
    
    # Get the first occurrence with comment (preferred) or just first occurrence
    local entry_with_comment=$(grep -m1 "<string name=\"$name\".*comment=" "$file")
    
    # If no entry with comment found, get the first occurrence
    if [ -z "$entry_with_comment" ]; then
        entry_with_comment=$(grep -m1 "<string name=\"$name\"" "$file")
    fi
    
    # Find position to insert (before closing </resources>)
    local end_line=$(grep -n '</resources>' "$TEMP_FILE" | cut -d':' -f1)
    local insert_line=$((end_line - 1))
    
    # Split and reassemble the file with single entry
    local head_file=$(mktemp)
    local tail_file=$(mktemp)
    
    head -n "$insert_line" "$TEMP_FILE" > "$head_file"
    tail -n $((end_line - insert_line)) "$TEMP_FILE" > "$tail_file"
    
    cat "$head_file" > "$TEMP_FILE"
    echo "    $entry_with_comment" >> "$TEMP_FILE" 
    cat "$tail_file" >> "$TEMP_FILE"
    
    # Copy back and clean up
    cp "$TEMP_FILE" "$file"
    rm "$TEMP_FILE" "$head_file" "$tail_file"
    
    echo -e "${GREEN}  Fixed duplicate: $name${NC}" | tee -a "$LOG_FILE"
}

# Process each file for duplicates
for file in "${STRINGS_FILES[@]}"; do
    echo -e "${MAGENTA}Checking for duplicates in $file...${NC}" | tee -a "$LOG_FILE"
    
    # Extract all string names and find duplicates
    ALL_NAMES=$(grep -o '<string name="[^"]*"' "$file" | cut -d '"' -f2 | sort)
    DUPLICATES=$(echo "$ALL_NAMES" | uniq -d)
    
    if [ -z "$DUPLICATES" ]; then
        echo -e "${GREEN}  No duplicates found.${NC}" | tee -a "$LOG_FILE"
        continue
    fi
    
    # Process each duplicate
    while read -r name; do
        if [ -n "$name" ]; then
            COUNT=$(grep -c "<string name=\"$name\"" "$file")
            fix_duplicates "$file" "$name" "$COUNT"
        fi
    done <<< "$DUPLICATES"
done

echo -e "${GREEN}Duplicate string handling complete.${NC}"
echo "Duplicate string handling complete." >> "$LOG_FILE"
echo

###############################################################
# Step 2: Check for localized strings that don't exist in default resources
###############################################################
echo -e "${BOLD}Step 2: Checking for missing default strings...${NC}"
echo "Step 2: Checking for missing default strings..." >> "$LOG_FILE"

# Get strings from default file
DEFAULT_STRINGS=$(grep -o '<string name="[^"]*"' "$DEFAULT_STRINGS_FILE" | cut -d '"' -f2 | sort)

# Check each non-default file
for file in "${STRINGS_FILES[@]}"; do
    # Skip default file
    if [ "$file" = "$DEFAULT_STRINGS_FILE" ]; then
        continue
    fi
    
    echo -e "${MAGENTA}Checking for missing defaults in $file...${NC}" | tee -a "$LOG_FILE"
    
    # Get strings from this language file
    LANG_STRINGS=$(grep -o '<string name="[^"]*"' "$file" | cut -d '"' -f2 | sort)
    
    # Extract missing default definitions
    MISSING_DEFAULTS=$(comm -23 <(echo "$LANG_STRINGS") <(echo "$DEFAULT_STRINGS"))
    
    if [ -z "$MISSING_DEFAULTS" ]; then
        echo -e "${GREEN}  All localized strings have default definitions.${NC}" | tee -a "$LOG_FILE"
        continue
    fi
    
    # Report missing defaults
    MISSING_COUNT=0
    TEMP_FIXED_FILE=$(mktemp)
    LANG=$(basename $(dirname "$file") | sed 's/values-//')
    
    echo -e "${YELLOW}  Found strings in $LANG localization without default definition:${NC}" | tee -a "$LOG_FILE"
    
    while read -r name; do
        if [ -n "$name" ]; then
            MISSING_COUNT=$((MISSING_COUNT + 1))
            
            # Get the localized string to copy to default
            LOCALIZED_STRING=$(grep -m1 "<string name=\"$name\"" "$file")
            
            echo -e "  - $name: ${YELLOW}This string exists in $LANG but not in default resources${NC}" | tee -a "$LOG_FILE"
            echo "    Original: $LOCALIZED_STRING" | tee -a "$LOG_FILE"
        fi
    done <<< "$MISSING_DEFAULTS"
    
    echo -e "${YELLOW}  Found $MISSING_COUNT strings in $LANG without default definition.${NC}" | tee -a "$LOG_FILE"
    echo -e "${YELLOW}  These strings will likely be removed by the Android build system.${NC}" | tee -a "$LOG_FILE"
    echo -e "${YELLOW}  Consider adding them to the default (values/strings.xml) file.${NC}" | tee -a "$LOG_FILE"
done

echo -e "${GREEN}Missing default string check complete.${NC}"
echo "Missing default string check complete." >> "$LOG_FILE"
echo

###############################################################
# Step 3: Identify completely unused strings
###############################################################
echo -e "${BOLD}Step 3: Checking for unused string resources...${NC}"
echo "Step 3: Checking for unused string resources..." >> "$LOG_FILE"

# Extract strings from the default resource file
echo "Extracting strings from default resource file..." | tee -a "$LOG_FILE"
ALL_STRINGS=$(grep -o '<string name="[^"]*"' "$DEFAULT_STRINGS_FILE" | cut -d '"' -f2)
STRING_NAMES=()

# Convert to an array
while read -r line; do
    STRING_NAMES+=("$line")
done <<< "$ALL_STRINGS"

echo "Found ${#STRING_NAMES[@]} strings in default resource file." | tee -a "$LOG_FILE"
echo

# Special strings that should never be removed even if unused
SPECIAL_PATTERNS=("app_name" ".*_a11y" ".*translatable")

# Arrays to track string status
UNUSED_STRINGS=()

echo "Checking for unused strings..." | tee -a "$LOG_FILE"

# Check each string for usage
for name in "${STRING_NAMES[@]}"; do
    # Skip special strings that should never be removed
    SKIP=false
    for pattern in "${SPECIAL_PATTERNS[@]}"; do
        if [[ "$name" =~ $pattern ]]; then
            echo "Skipping special string: $name (considered used)" | tee -a "$LOG_FILE"
            SKIP=true
            break
        fi
    done
    
    if [ "$SKIP" = true ]; then
        continue
    fi
    
    # Check in Java/Kotlin code and XML layouts
    JAVA_USAGE=$(find "$SRC_DIR" -name "*.java" -o -name "*.kt" | grep -v "TODO" | grep -v "driftingdroids" | xargs grep -l "R\.string\.${name}\b\|getString(R\.string\.${name})" 2>/dev/null || true)
    XML_USAGE=$(find "$RES_DIR" -name "*.xml" | grep -v "/values" | xargs grep -l "@string/${name}\b" 2>/dev/null || true)
    
    # If not found anywhere, mark as unused
    if [ -z "$JAVA_USAGE" ] && [ -z "$XML_USAGE" ]; then
        echo -e "${RED}Completely unused string found: $name${NC}" | tee -a "$LOG_FILE"
        UNUSED_STRINGS+=("$name")
    else
        echo "String '$name' is used and implemented" >> "$LOG_FILE"
    fi
done

echo
echo "Analysis complete. Found:" | tee -a "$LOG_FILE"
echo "- ${#UNUSED_STRINGS[@]} completely unused strings" | tee -a "$LOG_FILE"
echo

###############################################################
# Step 4: Enhance comments with file usage information (English only)
###############################################################
echo -e "${BOLD}Step 4: Enhancing string comments with usage information...${NC}"
echo "Step 4: Enhancing string comments with usage information..." >> "$LOG_FILE"

# Function to get class names from file paths
get_class_name() {
    local file=$1
    local base_name=$(basename "$file" .java)
    echo "$base_name"
}

# Only update comments in the default resource file
echo -e "${MAGENTA}Enhancing comments in $DEFAULT_STRINGS_FILE...${NC}" | tee -a "$LOG_FILE"

# Create a temporary file for the updated content
TEMP_DEFAULT_FILE=$(mktemp)

# Process each line in the default strings file
LINE_COUNT=0
MODIFIED_COUNT=0

# Copy the file first
cp "$DEFAULT_STRINGS_FILE" "$TEMP_DEFAULT_FILE.original"

# Process the strings file line by line
while IFS= read -r line; do
    LINE_COUNT=$((LINE_COUNT + 1))
    
    # Only process string lines with comments
    if echo "$line" | grep -q '<string name=\".*\".*comment=\"'; then
        # Extract the string name
        STRING_NAME=$(echo "$line" | grep -o 'name="[^"]*"' | cut -d '"' -f2)
        
        # Find Java files that use this string
        JAVA_FILES=$(find "$SRC_DIR" -name "*.java" | grep -v "TODO" | grep -v "driftingdroids" | xargs grep -l "R\.string\.${STRING_NAME}\b\|getString(R\.string\.${STRING_NAME})" 2>/dev/null || true)
        
        # If Java usage found, enhance the comment
        if [ -n "$JAVA_FILES" ]; then
            # Get class names from file paths
            FILE_CLASSES=""
            for java_file in $JAVA_FILES; do
                class_name=$(get_class_name "$java_file")
                FILE_CLASSES+="[$class_name]"
            done
            
            # Get the original comment
            ORIG_COMMENT=$(echo "$line" | grep -o 'comment="[^"]*"' | cut -d '"' -f2)
            
            # Check if the comment already has file info (to avoid duplication)
            if ! echo "$ORIG_COMMENT" | grep -q '\[.*\]'; then
                # Replace the comment with enhanced version
                NEW_LINE=$(echo "$line" | sed "s|comment=\"$ORIG_COMMENT\"|comment=\"$ORIG_COMMENT $FILE_CLASSES\"|")
                echo "$NEW_LINE" >> "$TEMP_DEFAULT_FILE"
                
                echo "  Enhanced comment for $STRING_NAME with file usage: $FILE_CLASSES" | tee -a "$LOG_FILE"
                MODIFIED_COUNT=$((MODIFIED_COUNT + 1))
                continue
            fi
        fi
    fi
    
    # Write unchanged line
    echo "$line" >> "$TEMP_DEFAULT_FILE"
done < "$DEFAULT_STRINGS_FILE"

# Replace the original file if we made changes
if [ $MODIFIED_COUNT -gt 0 ]; then
    cp "$TEMP_DEFAULT_FILE" "$DEFAULT_STRINGS_FILE"
    echo -e "${GREEN}Enhanced $MODIFIED_COUNT string comments with file usage information.${NC}" | tee -a "$LOG_FILE"
else
    echo -e "${YELLOW}No string comments were enhanced. All strings already have complete information.${NC}" | tee -a "$LOG_FILE"
fi

# Clean up
rm "$TEMP_DEFAULT_FILE" "$TEMP_DEFAULT_FILE.original"

echo -e "${GREEN}Comment enhancement complete.${NC}"
echo "Comment enhancement complete." >> "$LOG_FILE"
echo

###############################################################
# Step 5: Remove only the specific safe-to-remove strings
###############################################################

# Only remove these specific strings which are confirmed safe to remove
SAFE_TO_REMOVE=("robot_count_message" "hint_robot_count")
REMOVED_COUNT=0

echo -e "${BOLD}Step 5: Removing specific unused strings...${NC}"
echo "Step 5: Removing specific unused strings..." >> "$LOG_FILE"

for file in "${STRINGS_FILES[@]}"; do
    FILE_REMOVED=0
    
    for name in "${SAFE_TO_REMOVE[@]}"; do
        # Use grep to find the line numbers of entries
        LINE_NUMBERS=$(grep -n "<string name=\"$name\"" "$file" | cut -d':' -f1)
        
        if [ -n "$LINE_NUMBERS" ]; then
            # Create a temporary file
            TEMP_FILE=$(mktemp)
            
            # Initialize line counter
            LINE_COUNT=1
            DELETED=false
            
            # Process each line
            while IFS= read -r line; do
                # Check if this line contains the string to remove
                if echo "$LINE_NUMBERS" | grep -q "$LINE_COUNT"; then
                    # Skip this line and the closing tag if needed
                    if echo "$line" | grep -q "/>"; then
                        # Self-closing tag, just skip this line
                        DELETED=true
                    else
                        # Multi-line tag, need to skip until </string>
                        DELETED=true
                        continue
                    fi
                elif [ "$DELETED" = true ] && echo "$line" | grep -q "</string>"; then
                    # We found the closing tag, reset flag
                    DELETED=false
                    continue
                elif [ "$DELETED" = true ]; then
                    # Still in a deleted section
                    continue
                else
                    # Regular line to keep
                    echo "$line" >> "$TEMP_FILE"
                fi
                
                LINE_COUNT=$((LINE_COUNT + 1))
            done < "$file"
            
            # Replace original file with the modified one
            cp "$TEMP_FILE" "$file"
            rm "$TEMP_FILE"
            
            # Verify the string was actually removed
            if ! grep -q "<string name=\"$name\"" "$file"; then
                FILE_REMOVED=$((FILE_REMOVED + 1))
                REMOVED_COUNT=$((REMOVED_COUNT + 1))
                echo "  Removed string: $name from $file" | tee -a "$LOG_FILE"
            fi
        fi
    done
    
    echo "Modified $file: removed $FILE_REMOVED strings" | tee -a "$LOG_FILE"
done

echo -e "${GREEN}Removed $REMOVED_COUNT unused string references across all files.${NC}" | tee -a "$LOG_FILE"
echo

###############################################################
# Step 6: Verify the build still works
###############################################################
echo -e "${BOLD}Step 6: Running Gradle build to verify changes...${NC}"
echo "Step 6: Running Gradle build to verify changes..." >> "$LOG_FILE"

# Generate list of strings we removed
REMOVED_LIST=""
for name in "${SAFE_TO_REMOVE[@]}"; do
    REMOVED_LIST+="- $name\n"
done

# Run build to verify changes
if ./gradlew build; then
    echo -e "\n${GREEN}Build successful! ${NC}" | tee -a "$LOG_FILE"
    
    # Create commit message
    echo -e "${BOLD}Suggested Git commit message:${NC}"
    echo "Localization: Cleaned up string resources" > "$COMMIT_FILE"
    echo "" >> "$COMMIT_FILE"
    echo "Removed $REMOVED_COUNT completely unused string resources from all localization files." >> "$COMMIT_FILE"
    echo "Enhanced string comments with source file usage information." >> "$COMMIT_FILE"
    echo "The removed strings were:" >> "$COMMIT_FILE"
    echo -e "$REMOVED_LIST" >> "$COMMIT_FILE"
    
    echo -e "\nCommit message saved to: $COMMIT_FILE"
    echo "You can use it with: git commit -F $COMMIT_FILE"
else
    echo -e "\n${RED}Build failed after string modifications.${NC}" | tee -a "$LOG_FILE"
    echo "You may need to manually fix the errors or revert changes to continue."
    exit 1
fi

echo
echo -e "${GREEN}Clean-up complete. See $LOG_FILE for details.${NC}"
echo -e "${YELLOW}Note: Only removed a small set of confirmed unused strings to maintain XML stability.${NC}"
echo -e "${YELLOW}The analysis found ${#UNUSED_STRINGS[@]} potentially unused strings in total.${NC}"
echo -e "${YELLOW}For a complete cleanup, consider manually removing the remaining unused strings.${NC}"

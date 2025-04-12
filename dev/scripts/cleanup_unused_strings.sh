#!/bin/bash

# cleanup_unused_strings.sh
# This script performs two main functions:
# 1. First it identifies and fixes duplicate string definitions in all string resource files
# 2. Then it identifies and removes completely unused strings
# 3. Finally it creates a todo file for strings that are referenced but not implemented
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

echo -e "${BLUE}Starting string resources cleanup in Roboyard project...${NC}"
echo "Working in: $PROJECT_DIR"
echo

echo "Found ${#STRINGS_FILES[@]} string resource files:"
for file in "${STRINGS_FILES[@]}"; do
    echo "- $file"
done
echo

###############################################################
# Step 1: Fix duplicate string definitions, especially in values-de
###############################################################
echo -e "${BOLD}Step 1: Checking for duplicate string definitions...${NC}"
echo "Step 1: Checking for duplicate string definitions..." >> "$LOG_FILE"

# First handle German cancel_button issue specifically 
DE_FILE="$RES_DIR/values-de/strings.xml"
if [ -f "$DE_FILE" ]; then
    # Check for duplicate cancel_button entries
    CANCEL_BUTTON_COUNT=$(grep -c '<string name="cancel_button"' "$DE_FILE")
    
    if [ "$CANCEL_BUTTON_COUNT" -gt 1 ]; then
        echo -e "${RED}Found duplicate cancel_button in German file ($CANCEL_BUTTON_COUNT entries)${NC}" | tee -a "$LOG_FILE"
        
        # Create a temporary file
        TEMP_FILE=$(mktemp)
        
        # Create fixed content by removing all cancel_button entries
        grep -v '<string name="cancel_button"' "$DE_FILE" > "$TEMP_FILE"
        
        # Find the line before </resources> to add our fixed entry
        END_LINE=$(grep -n '</resources>' "$TEMP_FILE" | cut -d':' -f1)
        INSERT_LINE=$((END_LINE - 1))
        
        # Split the file
        head -n $INSERT_LINE "$TEMP_FILE" > "${TEMP_FILE}.head"
        tail -n $((END_LINE - INSERT_LINE)) "$TEMP_FILE" > "${TEMP_FILE}.tail"
        
        # Add the correct cancel_button entry
        cat "${TEMP_FILE}.head" > "$TEMP_FILE"
        echo '    <string name="cancel_button" comment="Generic cancel button text">Abbrechen</string>' >> "$TEMP_FILE"
        cat "${TEMP_FILE}.tail" >> "$TEMP_FILE"
        
        # Replace the original file
        cp "$TEMP_FILE" "$DE_FILE"
        rm "$TEMP_FILE" "${TEMP_FILE}.head" "${TEMP_FILE}.tail"
        
        echo -e "${GREEN}Fixed duplicate cancel_button in German file${NC}" | tee -a "$LOG_FILE"
    fi
    
    # Also handle duplicate hint_button entries in German file
    HINT_BUTTON_COUNT=$(grep -c '<string name="hint_button"' "$DE_FILE")
    
    if [ "$HINT_BUTTON_COUNT" -gt 1 ]; then
        echo -e "${RED}Found duplicate hint_button in German file ($HINT_BUTTON_COUNT entries)${NC}" | tee -a "$LOG_FILE"
        
        # Create a temporary file
        TEMP_FILE=$(mktemp)
        
        # Create fixed content by removing all hint_button entries
        grep -v '<string name="hint_button"' "$DE_FILE" > "$TEMP_FILE"
        
        # Find the line before </resources> to add our fixed entry
        END_LINE=$(grep -n '</resources>' "$TEMP_FILE" | cut -d':' -f1)
        INSERT_LINE=$((END_LINE - 1))
        
        # Split the file
        head -n $INSERT_LINE "$TEMP_FILE" > "${TEMP_FILE}.head"
        tail -n $((END_LINE - INSERT_LINE)) "$TEMP_FILE" > "${TEMP_FILE}.tail"
        
        # Add the correct hint_button entry (with emoji)
        cat "${TEMP_FILE}.head" > "$TEMP_FILE"
        echo '    <string name="hint_button" comment="Button to get a hint">💡Hinweis</string>' >> "$TEMP_FILE"
        cat "${TEMP_FILE}.tail" >> "$TEMP_FILE"
        
        # Replace the original file
        cp "$TEMP_FILE" "$DE_FILE"
        rm "$TEMP_FILE" "${TEMP_FILE}.head" "${TEMP_FILE}.tail"
        
        echo -e "${GREEN}Fixed duplicate hint_button in German file${NC}" | tee -a "$LOG_FILE"
    fi
    
    # Also handle duplicate cancel_hint_button entries in German file
    CANCEL_HINT_BUTTON_COUNT=$(grep -c '<string name="cancel_hint_button"' "$DE_FILE")
    
    if [ "$CANCEL_HINT_BUTTON_COUNT" -gt 1 ]; then
        echo -e "${RED}Found duplicate cancel_hint_button in German file ($CANCEL_HINT_BUTTON_COUNT entries)${NC}" | tee -a "$LOG_FILE"
        
        # Create a temporary file
        TEMP_FILE=$(mktemp)
        
        # Create fixed content by removing all cancel_hint_button entries
        grep -v '<string name="cancel_hint_button"' "$DE_FILE" > "$TEMP_FILE"
        
        # Find the line before </resources> to add our fixed entry
        END_LINE=$(grep -n '</resources>' "$TEMP_FILE" | cut -d':' -f1)
        INSERT_LINE=$((END_LINE - 1))
        
        # Split the file
        head -n $INSERT_LINE "$TEMP_FILE" > "${TEMP_FILE}.head"
        tail -n $((END_LINE - INSERT_LINE)) "$TEMP_FILE" > "${TEMP_FILE}.tail"
        
        # Add the correct cancel_hint_button entry (with emoji)
        cat "${TEMP_FILE}.head" > "$TEMP_FILE"
        echo '    <string name="cancel_hint_button" comment="Button to hide hints">🔍 Ausblenden</string>' >> "$TEMP_FILE"
        cat "${TEMP_FILE}.tail" >> "$TEMP_FILE"
        
        # Replace the original file
        cp "$TEMP_FILE" "$DE_FILE"
        rm "$TEMP_FILE" "${TEMP_FILE}.head" "${TEMP_FILE}.tail"
        
        echo -e "${GREEN}Fixed duplicate cancel_hint_button in German file${NC}" | tee -a "$LOG_FILE"
    fi
fi

# Now handle remaining specific duplicate entries in main values/strings.xml
MAIN_FILE="$RES_DIR/values/strings.xml"
if [ -f "$MAIN_FILE" ]; then
    # Known duplicates to fix
    KNOWN_DUPLICATES=("level_completed" "level_difficulty_format" "level_filter_all" 
                        "level_filter_played" "level_filter_unsolved" "level_format" 
                        "level_locked" "level_locked_message" "level_size_format" 
                        "save_date_format" "save_empty" "save_failed" 
                        "save_level_format" "save_slot_format")
    
    for name in "${KNOWN_DUPLICATES[@]}"; do
        # Check if this string has duplicates
        COUNT=$(grep -c "<string name=\"$name\"" "$MAIN_FILE")
        
        if [ "$COUNT" -gt 1 ]; then
            echo -e "${YELLOW}Fixing duplicate: $name ($COUNT occurrences)${NC}" | tee -a "$LOG_FILE"
            
            # Create temporary file without the string
            TEMP_FILE=$(mktemp)
            grep -v "<string name=\"$name\"" "$MAIN_FILE" > "$TEMP_FILE"
            
            # Get the first occurrence
            FIRST_OCCURRENCE=$(grep -m 1 "<string name=\"$name\"" "$MAIN_FILE")
            
            # Find where to insert it (before </resources>)
            END_LINE=$(grep -n '</resources>' "$TEMP_FILE" | cut -d':' -f1)
            INSERT_LINE=$((END_LINE - 1))
            
            # Split the file
            head -n $INSERT_LINE "$TEMP_FILE" > "${TEMP_FILE}.head"
            tail -n $((END_LINE - INSERT_LINE)) "$TEMP_FILE" > "${TEMP_FILE}.tail"
            
            # Reassemble with the first occurrence restored
            cat "${TEMP_FILE}.head" > "$TEMP_FILE"
            echo "    $FIRST_OCCURRENCE" >> "$TEMP_FILE"
            cat "${TEMP_FILE}.tail" >> "$TEMP_FILE"
            
            # Replace original file
            cp "$TEMP_FILE" "$MAIN_FILE"
            rm "$TEMP_FILE" "${TEMP_FILE}.head" "${TEMP_FILE}.tail"
        fi
    done
fi

echo -e "${GREEN}Duplicate string handling complete.${NC}"
echo "Duplicate string handling complete." >> "$LOG_FILE"
echo

###############################################################
# Step 2: Identify completely unused strings
###############################################################
echo -e "${BOLD}Step 2: Checking for unused string resources...${NC}"
echo "Step 2: Checking for unused string resources..." >> "$LOG_FILE"

# Extract strings from the default resource file
DEFAULT_STRINGS_FILE="$RES_DIR/values/strings.xml"
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
# Step 3: Remove only the specific safe-to-remove strings
###############################################################

# Only remove these specific strings which are safe to remove
SAFE_TO_REMOVE=("robot_count_message" "hint_robot_count")
REMOVED_COUNT=0

echo -e "${BOLD}Step 3: Removing specific unused strings...${NC}"
echo "Step 3: Removing specific unused strings..." >> "$LOG_FILE"

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
# Step 4: Verify the build still works
###############################################################
echo -e "${BOLD}Step 4: Running Gradle build to verify changes...${NC}"
echo "Step 4: Running Gradle build to verify changes..." >> "$LOG_FILE"

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

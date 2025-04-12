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

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RES_DIR="$PROJECT_DIR/app/src/main/res"
SRC_DIR="$PROJECT_DIR/app/src/main"
DEV_DIR="$PROJECT_DIR/dev"
LOG_FILE="$DEV_DIR/unused_strings_cleanup.log"
TODO_FILE="$DEV_DIR/strings_todo.md"

# Initialize log file
echo "String Resources Cleanup Log - $(date)" > "$LOG_FILE"
echo "===================================" >> "$LOG_FILE"

echo -e "${BOLD}Starting string resources cleanup in Roboyard project...${NC}"
echo "Working in: $PROJECT_DIR"
echo 

# Function to extract string names from a strings.xml file
extract_string_names() {
    grep -o 'name="[^"]*"' "$1" | sed 's/name="\([^"]*\)"/\1/'
}

# Find all strings.xml files
STRINGS_FILES=($(find "$RES_DIR" -name "strings.xml"))
echo -e "${BOLD}Found ${#STRINGS_FILES[@]} string resource files:${NC}"
for file in "${STRINGS_FILES[@]}"; do
    echo "- $file"
done
echo 

# Step 1: Check for and fix duplicate string definitions
echo -e "${BOLD}Step 1: Checking for duplicate string definitions...${NC}"
DUPE_COUNT=0
DUPE_FIXED=0

for file in "${STRINGS_FILES[@]}"; do
    echo -e "${MAGENTA}Checking for duplicates in $file...${NC}"
    FILE_DUPES=0
    
    # Get all unique string names in this file
    UNIQUE_NAMES=($(extract_string_names "$file" | sort | uniq))
    
    for name in "${UNIQUE_NAMES[@]}"; do
        # Count occurrences of this string name
        COUNT=$(grep -c "<string name=\"$name\"" "$file" || true)
        
        if [ "$COUNT" -gt 1 ]; then
            DUPE_COUNT=$((DUPE_COUNT + 1))
            FILE_DUPES=$((FILE_DUPES + 1))
            echo -e "${RED}  Duplicate string found: $name appears $COUNT times${NC}"
            echo "Duplicate string found in $file: $name appears $COUNT times" >> "$LOG_FILE"
            
            # Keep only the version with a comment if available, otherwise keep the first occurrence
            if grep -q "<string name=\"$name\" comment" "$file"; then
                # Keep the version with a comment and remove duplicates without comments
                echo "  Keeping version with comment, removing others" >> "$LOG_FILE"
                
                # Create a temporary file with all content except the duplicated string
                TEMPFILE="$(mktemp)"
                # More robust duplicate removal - use XML parser or line-by-line processing
                # First, write all lines up to first occurrence of the string
                FIRST_LINE=$(grep -n "<string name=\"$name\"" "$file" | head -1 | cut -d ':' -f1)
                head -n $((FIRST_LINE-1)) "$file" > "$TEMPFILE"
                
                # Add the first occurrence of the string
                grep -m 1 "<string name=\"$name\"" "$file" >> "$TEMPFILE"
                
                # Add all lines after the last occurrence
                LAST_LINE=$(grep -n "<string name=\"$name\"" "$file" | tail -1 | cut -d ':' -f1)
                tail -n +$((LAST_LINE+1)) "$file" >> "$TEMPFILE"
                
                # Make sure we keep the XML structure intact
                if ! grep -q "</resources>" "$TEMPFILE"; then
                    echo "</resources>" >> "$TEMPFILE"
                fi
                
                # Copy back to original file
                cp "$TEMPFILE" "$file"
                rm "$TEMPFILE"
                DUPE_FIXED=$((DUPE_FIXED + 1))
            else
                # Keep only the first occurrence
                echo "  Keeping first occurrence, removing others" >> "$LOG_FILE"
                
                # Create a temporary file with all content except the duplicated string
                TEMPFILE="$(mktemp)"
                # More robust duplicate removal - use XML parser or line-by-line processing
                # First, write all lines up to first occurrence of the string
                FIRST_LINE=$(grep -n "<string name=\"$name\"" "$file" | head -1 | cut -d ':' -f1)
                head -n $((FIRST_LINE-1)) "$file" > "$TEMPFILE"
                
                # Add the first occurrence of the string
                grep -m 1 "<string name=\"$name\"" "$file" >> "$TEMPFILE"
                
                # Add all lines after the last occurrence
                LAST_LINE=$(grep -n "<string name=\"$name\"" "$file" | tail -1 | cut -d ':' -f1)
                tail -n +$((LAST_LINE+1)) "$file" >> "$TEMPFILE"
                
                # Make sure we keep the XML structure intact
                if ! grep -q "</resources>" "$TEMPFILE"; then
                    echo "</resources>" >> "$TEMPFILE"
                fi
                
                # Copy back to original file
                cp "$TEMPFILE" "$file"
                rm "$TEMPFILE"
                DUPE_FIXED=$((DUPE_FIXED + 1))
            fi
        fi
    done
    
    if [ "$FILE_DUPES" -eq 0 ]; then
        echo -e "${GREEN}  No duplicates found.${NC}"
    else
        echo -e "${YELLOW}  Fixed $FILE_DUPES duplicate strings in this file.${NC}"
    fi
done

echo 
if [ "$DUPE_COUNT" -gt 0 ]; then
    echo -e "${BOLD}Fixed $DUPE_FIXED/$DUPE_COUNT duplicate string definitions.${NC}"
else
    echo -e "${GREEN}${BOLD}No duplicate string definitions found.${NC}"
fi
echo 

# Step 2: Find and remove completely unused strings
echo -e "${BOLD}Step 2: Checking for unused string resources...${NC}"

# Extract strings from default strings.xml again in case we fixed duplicates
DEFAULT_STRINGS_FILE="$RES_DIR/values/strings.xml"
echo -e "${BOLD}Extracting strings from default resource file...${NC}"
STRING_NAMES=($(extract_string_names "$DEFAULT_STRINGS_FILE"))
echo "Found ${#STRING_NAMES[@]} strings in default resource file."
echo 

TOTAL_UNUSED=0
COMPLETELY_UNUSED=0
REFERENCED_NOT_IMPLEMENTED=0
UNUSED_STRINGS=()
TODO_STRINGS=()

echo -e "${BOLD}Checking for unused strings...${NC}"

# Initialize todo file
echo "# Strings Todo List" > "$TODO_FILE"
echo "Generated on $(date)" >> "$TODO_FILE"
echo "" >> "$TODO_FILE"
echo "## Completely Unused Strings" >> "$TODO_FILE"
echo "These strings are not referenced anywhere in the codebase and can be safely removed:" >> "$TODO_FILE"
echo "" >> "$TODO_FILE"

# Only search in Java files that are part of the project, excluding TODOs and driftingdroids
JAVA_SRC_FILES=$(find "/var/www/Roboyard/app/src/main" -name "*.java" | grep -v "TODO" | grep -v "driftingdroids")

# Check each string to see if it's used in code
for string_name in "${STRING_NAMES[@]}"; do
    # Skip strings with special patterns (app name, launcher names, etc.)
    if [[ "$string_name" == "app_name" || \
          "$string_name" == "launcher_name" || \
          "$string_name" == *"_a11y" || \
          "$string_name" == *"translatable"* ]]; then
        echo -e "${YELLOW}Skipping special string: $string_name (considered used)${NC}"
        echo "Skipping special string: $string_name (considered used)" >> "$LOG_FILE"
        continue
    fi
    
    # Search for the string in layout XML files (but not in values directories)
    XML_USAGE=$(find "$SRC_DIR/res" -name "*.xml" | grep -v "/values" | xargs grep -l "@string/${string_name}\b" 2>/dev/null || true)
    
    # Search for the string in Java files
    JAVA_USAGE=$(echo "$JAVA_SRC_FILES" | xargs grep -l "R\.string\.${string_name}\b\|getString(R\.string\.${string_name})\|getString(R\.string\.${string_name}," 2>/dev/null || true)
    
    if [ -n "$XML_USAGE" ] || [ -n "$JAVA_USAGE" ]; then
        # String is referenced somewhere in the code
        IMPLEMENTED_USAGE=$(grep -q -r "<string name=\"${string_name}\"" "$RES_DIR" && echo "1" || echo "0")
        
        if [ "$IMPLEMENTED_USAGE" == "1" ]; then
            echo -e "${GREEN}String '$string_name' is used and implemented${NC}"
        else
            echo -e "${BLUE}String '$string_name' is referenced in code but not implemented yet${NC}"
            echo "String '$string_name' is referenced in code but not implemented yet" >> "$LOG_FILE"
            TODO_STRINGS+=("$string_name")
            REFERENCED_NOT_IMPLEMENTED=$((REFERENCED_NOT_IMPLEMENTED + 1))
            TOTAL_UNUSED=$((TOTAL_UNUSED + 1))
        fi
    else
        # String is not referenced anywhere
        echo -e "${RED}Completely unused string found: $string_name${NC}"
        echo "Completely unused string found: $string_name" >> "$LOG_FILE"
        UNUSED_STRINGS+=("$string_name")
        COMPLETELY_UNUSED=$((COMPLETELY_UNUSED + 1))
        TOTAL_UNUSED=$((TOTAL_UNUSED + 1))
    fi
done

# Add completely unused strings to the todo file
for unused_string in "${UNUSED_STRINGS[@]}"; do
    # Get the string value from the default strings.xml file
    STRING_VALUE=$(grep -A1 "<string name=\"$unused_string\"" "$DEFAULT_STRINGS_FILE" | tail -n1 | sed 's/^[ \t]*//;s/[ \t]*$//' | sed 's/<\/string>//')
    echo "- \`$unused_string\`: $STRING_VALUE" >> "$TODO_FILE"
done

# Add section for strings referenced but not implemented
echo "" >> "$TODO_FILE"
echo "## Strings Referenced But Not Implemented" >> "$TODO_FILE"
echo "These strings are referenced in the code but are not yet implemented in all locale files:" >> "$TODO_FILE"
echo "" >> "$TODO_FILE"

for todo_string in "${TODO_STRINGS[@]}"; do
    # Find where this string is referenced (only in project Java files, excluding TODOs and driftingdroids)
    REFERENCES=$(echo "$JAVA_SRC_FILES" | xargs grep -l "R\.string\.${todo_string}\b\|getString(R\.string\.${todo_string})\|getString(R\.string\.${todo_string}," 2>/dev/null || true)
    XML_REFS=$(find "$SRC_DIR/res" -name "*.xml" | grep -v "/values" | xargs grep -l "@string/${todo_string}\b" 2>/dev/null || true)
    
    # Format the references to be more readable (just file paths)
    if [ -n "$REFERENCES" ]; then
        FORMATTED_REFS=$(echo "$REFERENCES" | sed 's/.*\///' | sort | uniq | sed 's/^/  - /')
        echo "- \`$todo_string\` is referenced in:" >> "$TODO_FILE"
        echo "$FORMATTED_REFS" >> "$TODO_FILE"
    fi
    
    if [ -n "$XML_REFS" ]; then
        XML_FORMATTED=$(echo "$XML_REFS" | sed 's/.*\///' | sort | uniq | sed 's/^/  - /')
        if [ -z "$REFERENCES" ]; then
            echo "- \`$todo_string\` is referenced in:" >> "$TODO_FILE"
        fi
        echo "$XML_FORMATTED" >> "$TODO_FILE"
    fi
done

echo 
echo -e "${BOLD}Analysis complete. Found:${NC}"
echo -e "- ${RED}$COMPLETELY_UNUSED completely unused strings${NC}"
echo -e "- ${BLUE}$REFERENCED_NOT_IMPLEMENTED strings referenced but not implemented${NC}"
echo -e "${BOLD}Total: $TOTAL_UNUSED strings to review${NC}"

# If there are completely unused strings, remove them from all strings.xml files
if [ $COMPLETELY_UNUSED -gt 0 ]; then
    echo -e "\n${BOLD}Step 3: Removing completely unused strings from resource files...${NC}"
    REMOVED_COUNT=0
    
    for file in "${STRINGS_FILES[@]}"; do
        MODIFIED=false
        FILE_REMOVED=0
        
        for unused_string in "${UNUSED_STRINGS[@]}"; do
            # Check if this string exists in this file
            if grep -q "<string name=\"$unused_string\"" "$file"; then
                # If found, remove the string element
                echo "Removing string '$unused_string' from $file" >> "$LOG_FILE"
                
                # Create a temporary file without the unused string
                TEMPFILE="$(mktemp)"
                grep -v "<string name=\"$unused_string\"" "$file" > "$TEMPFILE"
                
                # Make sure we keep the XML structure intact
                if ! grep -q "</resources>" "$TEMPFILE"; then
                    echo "</resources>" >> "$TEMPFILE"
                fi
                
                # Copy back to original file
                cp "$TEMPFILE" "$file"
                rm "$TEMPFILE"
                
                MODIFIED=true
                REMOVED_COUNT=$((REMOVED_COUNT + 1))
                FILE_REMOVED=$((FILE_REMOVED + 1))
            fi
        done
        
        if [ "$MODIFIED" = true ]; then
            echo -e "${MAGENTA}Modified $file: removed $FILE_REMOVED strings${NC}"
        else
            echo -e "${GREEN}No changes needed in: $file${NC}"
        fi
    done
    
    echo -e "${GREEN}Successfully removed $REMOVED_COUNT unused string references across all files.${NC}"
else
    echo -e "${GREEN}No completely unused strings to remove.${NC}"
fi

# Running Gradle build to ensure everything still compiles
echo -e "\n${BOLD}Step 4: Running Gradle build to verify changes...${NC}"
cd "$PROJECT_DIR"
./gradlew build

BUILD_RESULT=$?
if [ $BUILD_RESULT -eq 0 ]; then
    echo -e "\n${GREEN}Build successful!${NC}"
    
    # Generate a git commit message suggestion
    COMMIT_MSG="Localization: Cleaned up string resources\n\n"
    if [ $DUPE_COUNT -gt 0 ]; then
        COMMIT_MSG+="Fixed $DUPE_FIXED duplicate string definitions.\n\n"
    fi
    
    if [ $COMPLETELY_UNUSED -gt 0 ]; then
        COMMIT_MSG+="Removed $COMPLETELY_UNUSED completely unused string resources from all localization files.\n"
        COMMIT_MSG+="The removed strings were:\n"
        
        for unused_string in "${UNUSED_STRINGS[@]}"; do
            COMMIT_MSG+="- $unused_string\n"
        done
    fi
    
    if [ $REFERENCED_NOT_IMPLEMENTED -gt 0 ]; then
        COMMIT_MSG+="\nAlso identified $REFERENCED_NOT_IMPLEMENTED strings that are referenced in code but not implemented.\n"
        COMMIT_MSG+="See $TODO_FILE for details."
    fi
    
    echo -e "\n${BOLD}Suggested Git commit message:${NC}"
    echo -e "${YELLOW}$COMMIT_MSG${NC}"
    
    # Write the commit message to a file for easy use
    echo -e "$COMMIT_MSG" > "$DEV_DIR/commit_message.txt"
    echo "Commit message suggestion saved to: $DEV_DIR/commit_message.txt"
    echo "You can use it with: git commit -F $DEV_DIR/commit_message.txt"
    
    echo -e "\n${BOLD}Clean-up complete. See $LOG_FILE for details and $TODO_FILE for the todo list.${NC}"
else
    echo -e "\n${RED}Build failed! You may need to review your changes.${NC}"
    echo "Error details can be found in the Gradle output above."
    echo "Clean-up completed but build failed. Check the build output for errors." >> "$LOG_FILE"
fi

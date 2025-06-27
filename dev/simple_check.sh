#!/bin/bash

# Super simple translation check script
# For Roboyard project

# Output directory
OUTPUT_DIR="/var/www/Roboyard/dev/translation_stats"
mkdir -p "$OUTPUT_DIR"

# Main report file
REPORT="$OUTPUT_DIR/summary.md"
echo "# Roboyard Translation Status" > "$REPORT"
echo "Generated: $(date)" >> "$REPORT"
echo "" >> "$REPORT"
echo "| Language | Total Strings | Missing Strings | Completion % |" >> "$REPORT"
echo "|----------|---------------|----------------|-------------|" >> "$REPORT"

# Extract English strings as reference
echo "Extracting English strings..."
grep -o 'name="[^"]*"' "/var/www/Roboyard/app/src/main/res/values/strings.xml" | \
    sed 's/name="//;s/"$//' > "$OUTPUT_DIR/en_strings.txt"
sort "$OUTPUT_DIR/en_strings.txt" > "$OUTPUT_DIR/en_sorted.txt"
EN_COUNT=$(wc -l < "$OUTPUT_DIR/en_sorted.txt")
echo "Found $EN_COUNT English strings"

# Languages to check
LANGUAGES=("de" "es" "fr" "ko" "zh")

# Check each language
for LANG in "${LANGUAGES[@]}"; do
    echo "Checking $LANG..."
    
    # Extract strings for this language
    grep -o 'name="[^"]*"' "/var/www/Roboyard/app/src/main/res/values-$LANG/strings.xml" | \
        sed 's/name="//;s/"$//' > "$OUTPUT_DIR/${LANG}_strings.txt"
    sort "$OUTPUT_DIR/${LANG}_strings.txt" > "$OUTPUT_DIR/${LANG}_sorted.txt"
    LANG_COUNT=$(wc -l < "$OUTPUT_DIR/${LANG}_sorted.txt")
    
    # Find missing strings
    comm -23 "$OUTPUT_DIR/en_sorted.txt" "$OUTPUT_DIR/${LANG}_sorted.txt" > "$OUTPUT_DIR/${LANG}_missing.txt"
    MISSING_COUNT=$(wc -l < "$OUTPUT_DIR/${LANG}_missing.txt")
    
    # Calculate completion percentage
    COMPLETION=$(( (LANG_COUNT * 100) / EN_COUNT ))
    
    # Add to summary table
    echo "| $LANG | $LANG_COUNT | $MISSING_COUNT | ${COMPLETION}% |" >> "$REPORT"
    
    # Create individual report
    echo "# Missing translations for $LANG" > "$OUTPUT_DIR/${LANG}_report.md"
    echo "* Total missing: $MISSING_COUNT out of $EN_COUNT" >> "$OUTPUT_DIR/${LANG}_report.md"
    echo "* Completion: ${COMPLETION}%" >> "$OUTPUT_DIR/${LANG}_report.md"
    echo "" >> "$OUTPUT_DIR/${LANG}_report.md"
    echo "## Missing string keys:" >> "$OUTPUT_DIR/${LANG}_report.md"
    echo '```' >> "$OUTPUT_DIR/${LANG}_report.md"
    cat "$OUTPUT_DIR/${LANG}_missing.txt" >> "$OUTPUT_DIR/${LANG}_report.md"
    echo '```' >> "$OUTPUT_DIR/${LANG}_report.md"
done

echo ""
echo "Analysis complete! Check $OUTPUT_DIR for results."
echo "Summary report: $REPORT"

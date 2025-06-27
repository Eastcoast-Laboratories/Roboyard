#!/bin/bash

# Simplified script to analyze missing translations in Roboyard
# Usage: bash check_translations.sh

# Create output directory
OUTPUT_DIR="/var/www/Roboyard/dev/translation_analysis"
mkdir -p "$OUTPUT_DIR"

# Extract all string names from English file
echo "Extracting strings from English reference file..."
grep -o 'name="[^"]*"' /var/www/Roboyard/app/src/main/res/values/strings.xml | \
  sed 's/name="//;s/"$//' | sort > "$OUTPUT_DIR/en_strings.txt"
EN_COUNT=$(wc -l < "$OUTPUT_DIR/en_strings.txt")
echo "Found $EN_COUNT English strings."

# Define languages to check
LANGUAGES=("de" "es" "fr" "ko" "zh")

# Create markdown report header
REPORT="$OUTPUT_DIR/translation_report.md"
echo "# Roboyard Translation Status" > "$REPORT"
echo "Generated: $(date)" >> "$REPORT"
echo "" >> "$REPORT"
echo "## Summary" >> "$REPORT"
echo "" >> "$REPORT"
echo "| Language | Available | Missing | Completion % |" >> "$REPORT"
echo "|----------|-----------|---------|-------------|" >> "$REPORT"

# Check each language
for LANG in "${LANGUAGES[@]}"; do
  echo "Analyzing $LANG..."
  
  # Extract strings for this language
  LANG_FILE="$OUTPUT_DIR/${LANG}_strings.txt"
  grep -o 'name="[^"]*"' "/var/www/Roboyard/app/src/main/res/values-$LANG/strings.xml" | \
    sed 's/name="//;s/"$//' | sort > "$LANG_FILE"
  
  # Count strings
  LANG_COUNT=$(wc -l < "$LANG_FILE")
  
  # Find missing strings
  MISSING_FILE="$OUTPUT_DIR/${LANG}_missing.txt"
  comm -23 "$OUTPUT_DIR/en_strings.txt" "$LANG_FILE" > "$MISSING_FILE"
  MISSING_COUNT=$(wc -l < "$MISSING_FILE")
  
  # Calculate completion percentage
  COMPLETION=$(( (LANG_COUNT * 100) / EN_COUNT ))
  
  # Add to markdown report
  echo "| $LANG | $LANG_COUNT | $MISSING_COUNT | ${COMPLETION}% |" >> "$REPORT"
  
  # Create detailed report for this language
  LANG_REPORT="$OUTPUT_DIR/${LANG}_report.md"
  echo "# Missing Translations for $LANG" > "$LANG_REPORT"
  echo "Total: $MISSING_COUNT missing strings out of $EN_COUNT" >> "$LANG_REPORT"
  echo "" >> "$LANG_REPORT"
  echo "## All Missing Strings" >> "$LANG_REPORT"
  echo '```' >> "$LANG_REPORT"
  cat "$MISSING_FILE" >> "$LANG_REPORT"
  echo '```' >> "$LANG_REPORT"
done

echo ""
echo "Analysis complete. Reports are available in $OUTPUT_DIR/"
echo "Main report: $REPORT"
echo "Individual language reports can be found in the same directory."
    
    # JSON für diese Kategorie
    if [ "$first_cat" = true ]; then
      first_cat=false
    else
      echo "        }," >> "$output_dir/translation_status.json"
    fi
    
    echo "        \"$category\": {" >> "$output_dir/translation_status.json"
    echo "          \"count\": $cat_count," >> "$output_dir/translation_status.json"
    echo "          \"strings\": [" >> "$output_dir/translation_status.json"
    
    # Markdown für diese Kategorie
    echo "### $category: $cat_count strings" >> "$output_dir/report.md"
    
    # Liste die ersten 15 fehlenden Strings in dieser Kategorie
    if [ $cat_count -gt 0 ]; then
      echo -e "```" >> "$output_dir/report.md"
      
      # Für JSON
      first_string=true
      while IFS= read -r string; do
        if [ "$first_string" = true ]; then
          first_string=false
        else
          echo "            ," >> "$output_dir/translation_status.json"
        fi
        echo "            \"$string\"" >> "$output_dir/translation_status.json"
      done < <(grep -E "$pattern" "$output_dir/${lang}_missing.txt" | head -15)
      
      # Für Markdown
      grep -E "$pattern" "$output_dir/${lang}_missing.txt" | head -15 >> "$output_dir/report.md"
      
      if [ $cat_count -gt 15 ]; then
        echo "... and $(($cat_count - 15)) more" >> "$output_dir/report.md"
      fi
      
      echo "```" >> "$output_dir/report.md"
    else
      echo "None" >> "$output_dir/report.md"
      echo "            " >> "$output_dir/translation_status.json"
    fi
    
    echo "          ]" >> "$output_dir/translation_status.json"
  done
  
  echo "        }" >> "$output_dir/translation_status.json"
  echo "      }" >> "$output_dir/translation_status.json"
  
  # Erstelle einzelne Dateien mit den fehlenden Strings für jede Sprache
  echo "# Missing strings for $lang" > "$output_dir/${lang}_missing_strings.md"
  echo -e "\n## All missing strings ($missing_count)\n" >> "$output_dir/${lang}_missing_strings.md"
  echo "```" >> "$output_dir/${lang}_missing_strings.md"
  cat "$output_dir/${lang}_missing.txt" >> "$output_dir/${lang}_missing_strings.md"
  echo "```" >> "$output_dir/${lang}_missing_strings.md"
done

# JSON abschließen
echo "    }" >> "$output_dir/translation_status.json"
echo "  ]" >> "$output_dir/translation_status.json"
echo "}" >> "$output_dir/translation_status.json"

# Füge weitere Informationen zum Bericht hinzu
cat << EOF >> "$output_dir/report.md"

## Using This Report

1. **Priority Languages**: Focus on languages with lowest completion percentages
2. **Priority Categories**: Consider UI and game messages first for better user experience
3. **Implementation**: Copy strings from English source and get them professionally translated
4. **Verification**: Re-run this script after adding new translations

## Translation Standards

- Always escape apostrophes (') with a backslash (\\') in XML strings
- Maintain consistent terminology across languages
- Follow platform-specific localization guidelines
- Test UI layout with translations to avoid text overflow issues
EOF

echo -e "\nAnalysis complete. Reports saved to $output_dir/"
echo "Main report: $output_dir/report.md"

#!/bin/bash
# Quick Stringlate Check - Fast overview of missing translations

echo "🌍 Stringlate Quick Check - Roboyard"
echo "=================================="
echo ""

cd /var/www/Roboyard

echo "📊 Main Languages Status:"
python3 dev/scripts/stringlate_summary.py > /tmp/stringlate_temp.txt
grep -A 10 "Language Summary" /tmp/stringlate_temp.txt | tail -7
echo ""

echo "🔥 Top 10 Priority Missing Strings:"
grep -A 20 "Priority Missing Strings" /tmp/stringlate_temp.txt | grep -E "^\-.*missing in [0-9]+" | head -10
echo ""

echo "📈 Total Missing: $(python3 -c "
import sys
sys.path.append('dev/scripts')
from stringlate_summary import StringlateSummaryReporter
from pathlib import Path
reporter = StringlateSummaryReporter(Path('.'))
print(sum(len(strings) for strings in reporter.analyze_missing_strings().values()))
") strings"
echo ""

echo "📁 Full reports available:"
echo "  - dev/stringlate_summary.md (main languages only)"
echo "  - dev/stringlate_report.md (all languages)"

rm -f /tmp/stringlate_temp.txt

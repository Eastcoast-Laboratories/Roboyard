#!/usr/bin/env python3
"""
Stringlate Summary Report - Focused on main languages only
"""

import os
import re
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

class StringlateSummaryReporter:
    def __init__(self, project_root):
        self.project_root = Path(project_root)
        self.res_dir = self.project_root / "app/src/main/res"
        self.main_languages = ["en", "de", "es", "fr", "ja", "ko", "zh", "pt-rBR"]
        self.languages = self._discover_main_languages()
        self.base_strings = self._load_base_strings()
        
    def _discover_main_languages(self):
        """Discover main language directories."""
        languages = {}
        for item in self.res_dir.iterdir():
            if item.is_dir() and item.name.startswith("values"):
                if item.name == "values":
                    languages["en"] = item
                else:
                    # Extract language code from values-xx or values-xx-rYY
                    match = re.match(r'values-(\w+)', item.name)
                    if match:
                        lang_code = match.group(1)
                        # Map pt-rBR to pt for consistency
                        if lang_code == "pt-rBR":
                            lang_code = "pt-rBR"
                        if lang_code in self.main_languages:
                            languages[lang_code] = item
        return languages
    
    def _load_strings_from_file(self, strings_file):
        """Load strings from XML file."""
        strings = {}
        try:
            tree = ET.parse(strings_file)
            root = tree.getroot()
            
            for string_elem in root.findall('string'):
                name = string_elem.get('name')
                if name and not string_elem.get('translatable') == 'false':
                    strings[name] = string_elem.text or ''
        except Exception as e:
            print(f"Error parsing {strings_file}: {e}")
        return strings
    
    def _load_base_strings(self):
        """Load base English strings."""
        base_file = self.languages.get("en") / "strings.xml"
        return self._load_strings_from_file(base_file)
    
    def analyze_missing_strings(self):
        """Analyze missing strings across main languages."""
        missing_report = defaultdict(lambda: defaultdict(list))
        
        for lang_code, lang_dir in self.languages.items():
            if lang_code == "en":
                continue  # Skip base language
                
            strings_file = lang_dir / "strings.xml"
            if not strings_file.exists():
                continue
                
            lang_strings = self._load_strings_from_file(strings_file)
            
            # Find missing strings
            for string_name in self.base_strings:
                if string_name not in lang_strings:
                    missing_report[lang_code][string_name] = self.base_strings[string_name]
        
        return missing_report
    
    def generate_summary_report(self):
        """Generate focused summary report."""
        missing = self.analyze_missing_strings()
        
        report = []
        report.append("# Stringlate Summary Report - Main Languages")
        report.append("=" * 60)
        report.append(f"Project: {self.project_root.name}")
        report.append(f"Base Language: English")
        report.append(f"Main Languages: {len(self.languages)}")
        report.append(f"Base Strings: {len(self.base_strings)}")
        report.append("")
        
        # Language summary
        report.append("## Language Summary")
        report.append("")
        
        total_missing = 0
        language_names = {
            "de": "Deutsch (German)",
            "es": "Español (Spanish)", 
            "fr": "Français (French)",
            "ja": "日本語 (Japanese)",
            "ko": "한국어 (Korean)",
            "zh": "中文 (Chinese)",
            "pt-rBR": "Português (Brazilian Portuguese)"
        }
        
        for lang_code in sorted(self.languages.keys()):
            if lang_code == "en":
                continue
                
            missing_count = len(missing.get(lang_code, {}))
            total_missing += missing_count
            completion = ((len(self.base_strings) - missing_count) / len(self.base_strings)) * 100
            
            lang_name = language_names.get(lang_code, lang_code.upper())
            status = "✅ Complete" if missing_count == 0 else f"⚠️ {missing_count} missing ({completion:.1f}% complete)"
            report.append(f"- **{lang_name}**: {status}")
        
        report.append("")
        report.append(f"**Total Missing Strings: {total_missing}**")
        report.append("")
        
        # Priority missing strings (missing in multiple languages)
        report.append("## Priority Missing Strings (Missing in Multiple Languages)")
        report.append("")
        
        # Count missing frequency
        missing_frequency = defaultdict(int)
        for lang_strings in missing.values():
            for string_name in lang_strings:
                missing_frequency[string_name] += 1
        
        # Show strings missing in 3+ languages
        most_missing = [(name, count) for name, count in missing_frequency.items() if count >= 3]
        most_missing.sort(key=lambda x: x[1], reverse=True)
        
        if most_missing:
            for string_name, count in most_missing[:20]:
                english_text = self.base_strings.get(string_name, "Unknown")
                missing_langs = []
                for lang_code in missing:
                    if string_name in missing[lang_code]:
                        missing_langs.append(lang_code)
                
                report.append(f"- `{string_name}` (missing in {count}/{len(self.languages)-1} languages: {', '.join(missing_langs)})")
                report.append(f"  - English: `{english_text[:100]}{'...' if len(english_text) > 100 else ''}`")
                report.append("")
        else:
            report.append("No strings missing in 3+ languages. Great progress!")
            report.append("")
        
        # Language-specific critical missing strings
        report.append("## Language-Specific Missing Strings (Top 5 per language)")
        report.append("")
        
        for lang_code in sorted(missing.keys()):
            if not missing[lang_code]:
                continue
                
            lang_name = language_names.get(lang_code, lang_code.upper())
            report.append(f"### {lang_name} (Top 5 missing)")
            report.append("")
            
            # Show first 5 missing strings for this language
            missing_strings = list(missing[lang_code].items())[:5]
            for string_name, english_text in missing_strings:
                report.append(f"- `{string_name}`")
                report.append(f"  - English: `{english_text[:80]}{'...' if len(english_text) > 80 else ''}`")
                report.append("")
        
        return "\n".join(report)

def main():
    """Main function."""
    project_root = Path(__file__).parent.parent.parent  # Go up to project root
    reporter = StringlateSummaryReporter(project_root)
    
    report = reporter.generate_summary_report()
    
    # Save report
    report_file = project_root / "dev" / "stringlate_summary.md"
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"Stringlate summary report generated: {report_file}")
    missing_total = sum(len(strings) for strings in reporter.analyze_missing_strings().values())
    print(f"Total missing strings in main languages: {missing_total}")

if __name__ == "__main__":
    main()

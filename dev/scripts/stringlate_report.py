#!/usr/bin/env python3
"""
Stringlate Report Generator
Analyzes missing strings across all language files and generates a comprehensive report.
"""

import os
import re
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

class StringlateReporter:
    def __init__(self, project_root):
        self.project_root = Path(project_root)
        self.res_dir = self.project_root / "app/src/main/res"
        self.languages = self._discover_languages()
        self.base_strings = self._load_base_strings()
        
    def _discover_languages(self):
        """Discover all language directories in res folder."""
        languages = {}
        for item in self.res_dir.iterdir():
            if item.is_dir() and item.name.startswith("values"):
                if item.name == "values":
                    languages["en"] = item  # Default English
                else:
                    # Extract language code from values-xx or values-xx-rYY
                    match = re.match(r'values-(\w+)', item.name)
                    if match:
                        lang_code = match.group(1)
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
        """Analyze missing strings across all languages."""
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
    
    def analyze_extra_strings(self):
        """Analyze strings that exist in other languages but not in base."""
        extra_report = defaultdict(lambda: defaultdict(list))
        
        for lang_code, lang_dir in self.languages.items():
            if lang_code == "en":
                continue  # Skip base language
                
            strings_file = lang_dir / "strings.xml"
            if not strings_file.exists():
                continue
                
            lang_strings = self._load_strings_from_file(strings_file)
            
            # Find extra strings
            for string_name in lang_strings:
                if string_name not in self.base_strings:
                    extra_report[lang_code][string_name] = lang_strings[string_name]
        
        return extra_report
    
    def generate_report(self):
        """Generate comprehensive missing strings report."""
        missing = self.analyze_missing_strings()
        extra = self.analyze_extra_strings()
        
        report = []
        report.append("# Stringlate Missing Strings Report")
        report.append("=" * 50)
        report.append(f"Project: {self.project_root.name}")
        report.append(f"Base Language: English (values/strings.xml)")
        report.append(f"Total Languages: {len(self.languages)}")
        report.append(f"Base Strings: {len(self.base_strings)}")
        report.append("")
        
        # Language summary
        report.append("## Language Summary")
        report.append("")
        for lang_code in sorted(self.languages.keys()):
            if lang_code == "en":
                continue
            missing_count = len(missing.get(lang_code, {}))
            extra_count = len(extra.get(lang_code, {}))
            status = "✅ Complete" if missing_count == 0 else f"⚠️ {missing_count} missing"
            report.append(f"- **{lang_code}**: {status} ({missing_count} missing, {extra_count} extra)")
        report.append("")
        
        # Detailed missing strings
        if missing:
            report.append("## Missing Strings by Language")
            report.append("")
            
            for lang_code in sorted(missing.keys()):
                if not missing[lang_code]:
                    continue
                    
                report.append(f"### {lang_code.upper()} ({len(missing[lang_code])} missing)")
                report.append("")
                
                for string_name in sorted(missing[lang_code].keys()):
                    english_text = missing[lang_code][string_name]
                    report.append(f"- `{string_name}`")
                    report.append(f"  - English: `{english_text}`")
                    report.append("")
        
        # Extra strings (strings in other languages but not in base)
        if extra:
            report.append("## Extra Strings (Not in Base)")
            report.append("")
            
            for lang_code in sorted(extra.keys()):
                if not extra[lang_code]:
                    continue
                    
                report.append(f"### {lang_code.upper()} ({len(extra[lang_code])} extra)")
                report.append("")
                
                for string_name in sorted(extra[lang_code].keys()):
                    local_text = extra[lang_code][string_name]
                    report.append(f"- `{string_name}`")
                    report.append(f"  - {lang_code}: `{local_text}`")
                    report.append("")
        
        # Statistics
        report.append("## Statistics")
        report.append("")
        total_missing = sum(len(strings) for strings in missing.values())
        total_extra = sum(len(strings) for strings in extra.values())
        
        report.append(f"- Total Missing Strings: {total_missing}")
        report.append(f"- Total Extra Strings: {total_extra}")
        report.append(f"- Languages with Missing: {len([lang for lang in missing if missing[lang]])}")
        report.append(f"- Languages with Extra: {len([lang for lang in extra if extra[lang]])}")
        report.append("")
        
        # Recommendations
        report.append("## Recommendations")
        report.append("")
        
        if total_missing > 0:
            report.append("### Priority Missing Strings")
            report.append("")
            
            # Count missing frequency
            missing_frequency = defaultdict(int)
            for lang_strings in missing.values():
                for string_name in lang_strings:
                    missing_frequency[string_name] += 1
            
            # Show most missing strings
            most_missing = sorted(missing_frequency.items(), key=lambda x: x[1], reverse=True)
            for string_name, count in most_missing[:10]:
                english_text = self.base_strings.get(string_name, "Unknown")
                report.append(f"- `{string_name}` (missing in {count}/{len(self.languages)-1} languages)")
                report.append(f"  - English: `{english_text}`")
                report.append("")
        
        return "\n".join(report)

def main():
    """Main function."""
    project_root = Path(__file__).parent.parent.parent  # Go up to project root
    reporter = StringlateReporter(project_root)
    
    report = reporter.generate_report()
    
    # Save report
    report_file = project_root / "dev" / "stringlate_report.md"
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"Stringlate report generated: {report_file}")
    print(f"Total missing strings: {sum(len(strings) for strings in reporter.analyze_missing_strings().values())}")

if __name__ == "__main__":
    main()

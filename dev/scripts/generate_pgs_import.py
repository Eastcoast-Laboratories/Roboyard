#!/usr/bin/env python3
"""
Generate Google Play Games Services Achievement Import Files

This script generates the CSV files needed for bulk-importing achievements
into Google Play Console. It reads the achievement definitions from the
Android app and creates:

1. AchievementsMetadata.csv - Achievement metadata (name, description, points, etc.)
2. AchievementsLocalizations.csv - Localized names and descriptions
3. AchievementsIconMappings.csv - Icon file mappings
4. games-ids.xml - Android resource file with placeholder IDs

Usage:
    python3 generate_pgs_import.py

Output:
    Creates files in dev/pgs_import/ directory, ready to be zipped and uploaded
    to Google Play Console.

After running:
    1. cd dev/pgs_import
    2. zip -r achievements_import.zip *.csv *.png
    3. Upload to Play Console > Play Games Services > Achievements > Import
"""

import os
import re
import xml.etree.ElementTree as ET
from pathlib import Path

# Project paths
SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
STRINGS_FILE = PROJECT_ROOT / "app/src/main/res/values/strings.xml"
DEFINITIONS_FILE = PROJECT_ROOT / "app/src/main/java/roboyard/eclabs/achievements/AchievementDefinitions.java"
OUTPUT_DIR = SCRIPT_DIR.parent / "pgs_import"
GAMES_IDS_FILE = PROJECT_ROOT / "app/src/main/res/values/games-ids.xml"

# Achievement points mapping by category
POINTS_BY_CATEGORY = {
    "LOGIN_STREAK": 20,
    "PROGRESSION": 10,
    "PERFORMANCE": 15,
    "CHALLENGE": 20,
    "MASTERY": 25,
    "RANDOM_DIFFICULTY": 30,
    "RANDOM_SOLUTION": 15,
    "RANDOM_RESOLUTION": 20,
    "RANDOM_TARGETS": 15,
    "RANDOM_ROBOTS": 20,
    "RANDOM_COVERAGE": 25,
    "RANDOM_STREAKS": 20,
    "RANDOM_SPEED": 15,
}

def parse_strings_xml():
    """Parse strings.xml to get achievement names and descriptions."""
    strings = {}
    tree = ET.parse(STRINGS_FILE)
    root = tree.getroot()
    
    for string_elem in root.findall("string"):
        name = string_elem.get("name")
        if name and (name.startswith("achievement_") or name.startswith("pgs_")):
            # Handle escaped apostrophes
            text = string_elem.text or ""
            text = text.replace("\\'", "'")
            strings[name] = text
    
    return strings

def parse_achievement_definitions():
    """Parse AchievementDefinitions.java to extract achievement IDs and categories."""
    achievements = []
    
    with open(DEFINITIONS_FILE, 'r') as f:
        content = f.read()
    
    # Match: add(new Achievement("id", "title_key", "desc_key", Category.XXX, icon));
    pattern = r'add\(new Achievement\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*AchievementCategory\.(\w+),\s*\w+\)\);'
    
    for match in re.finditer(pattern, content):
        achievement_id = match.group(1)
        title_key = match.group(2)
        desc_key = match.group(3)
        category = match.group(4)
        
        achievements.append({
            "id": achievement_id,
            "title_key": title_key,
            "desc_key": desc_key,
            "category": category,
        })
    
    # Also match loop-generated achievements (solution_X_moves)
    # These are generated in a loop, so we need to handle them specially
    for moves in range(18, 30):
        achievements.append({
            "id": f"solution_{moves}_moves",
            "title_key": f"achievement_solution_{moves}",
            "desc_key": f"achievement_solution_{moves}_desc",
            "category": "RANDOM_SOLUTION",
        })
    
    # Remove duplicates (keep first occurrence)
    seen = set()
    unique_achievements = []
    for a in achievements:
        if a["id"] not in seen:
            seen.add(a["id"])
            unique_achievements.append(a)
    
    return unique_achievements

def generate_metadata_csv(achievements, strings):
    """Generate AchievementsMetadata.csv"""
    lines = []
    
    for i, ach in enumerate(achievements):
        name = strings.get(ach["title_key"], ach["id"].replace("_", " ").title())
        desc = strings.get(ach["desc_key"], "")
        
        # Remove commas from name and description (CSV requirement)
        name = name.replace(",", " -")
        desc = desc.replace(",", " -")
        
        # CSV format: Name,Description,Incremental Value,Steps Needed,Initial State,Points,List Order
        incremental = "False"
        steps = ""
        initial_state = "Revealed"
        points = POINTS_BY_CATEGORY.get(ach["category"], 10)
        # Ensure points is multiple of 5 and between 5-200
        points = max(5, min(200, (points // 5) * 5))
        list_order = i + 1
        
        lines.append(f"{name},{desc},{incremental},{steps},{initial_state},{points},{list_order}")
    
    return "\n".join(lines)

def generate_localizations_csv(achievements, strings):
    """Generate AchievementsLocalizations.csv for German translations."""
    # For now, we only have English strings in the default locale
    # German translations would need to be added from strings-de.xml
    # This is a placeholder for future localization
    return ""

def generate_icon_mappings_csv(achievements):
    """Generate AchievementsIconMappings.csv"""
    lines = []
    
    # For now, use a default icon for all achievements
    # You can replace this with individual icons later
    for ach in achievements:
        name = ach["id"].replace("_", " ").title()
        # Use a generic icon name - you'll need to provide actual icon files
        icon_name = f"achievement_{ach['id']}.png"
        lines.append(f"{name},{icon_name}")
    
    return "\n".join(lines)

def generate_games_ids_xml(achievements):
    """Generate games-ids.xml with all achievement IDs."""
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<resources>',
        '    <!--',
        '    Google Play Games Services Configuration',
        '    ',
        '    IMPORTANT: Replace REPLACE_WITH_ACHIEVEMENT_ID with actual IDs from Google Play Console!',
        '    ',
        '    After importing achievements via CSV:',
        '    1. Go to Play Console > Play Games Services > Achievements',
        '    2. Click on each achievement to see its ID',
        '    3. Copy the ID and replace the placeholder below',
        '    ',
        '    See dev/google_play_games_setup.md for detailed instructions.',
        '    -->',
        '    ',
        '    <!-- Google Play Games Services App ID -->',
        '    <string name="games_app_id" translatable="false">REPLACE_WITH_YOUR_APP_ID</string>',
        '    ',
        '    <!-- Achievement IDs (pgs_ prefix to avoid conflicts with local string names) -->',
    ]
    
    for ach in achievements:
        lines.append(f'    <string name="pgs_{ach["id"]}" translatable="false">REPLACE_WITH_ACHIEVEMENT_ID</string>')
    
    lines.append('</resources>')
    
    return "\n".join(lines)

def generate_playgames_manager_mapping(achievements):
    """Generate the switch statement for PlayGamesManager.java"""
    lines = ['    private String getPlayGamesAchievementId(String localId) {',
             '        try {',
             '            int resId = 0;',
             '            switch (localId) {']
    
    for ach in achievements:
        lines.append(f'                case "{ach["id"]}":')
        lines.append(f'                    resId = R.string.pgs_{ach["id"]};')
        lines.append('                    break;')
    
    lines.extend([
        '                default:',
        '                    Timber.w("%s Unknown achievement ID: %s", TAG, localId);',
        '                    return null;',
        '            }',
        '            return context.getString(resId);',
        '        } catch (Exception e) {',
        '            Timber.e(e, "%s Failed to get Play Games ID for: %s", TAG, localId);',
        '            return null;',
        '        }',
        '    }',
    ])
    
    return "\n".join(lines)

def main():
    print("Generating Google Play Games Services import files...")
    
    # Create output directory
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    # Parse source files
    print("Parsing strings.xml...")
    strings = parse_strings_xml()
    print(f"  Found {len(strings)} achievement-related strings")
    
    print("Parsing AchievementDefinitions.java...")
    achievements = parse_achievement_definitions()
    print(f"  Found {len(achievements)} achievements")
    
    # Generate CSV files
    print("\nGenerating CSV files...")
    
    metadata = generate_metadata_csv(achievements, strings)
    metadata_file = OUTPUT_DIR / "AchievementsMetadata.csv"
    with open(metadata_file, 'w') as f:
        f.write(metadata)
    print(f"  Created {metadata_file}")
    
    # Icon mappings (optional - you need to provide actual icons)
    # icon_mappings = generate_icon_mappings_csv(achievements)
    # icon_file = OUTPUT_DIR / "AchievementsIconMappings.csv"
    # with open(icon_file, 'w') as f:
    #     f.write(icon_mappings)
    # print(f"  Created {icon_file}")
    
    # Generate games-ids.xml
    print("\nGenerating games-ids.xml...")
    games_ids = generate_games_ids_xml(achievements)
    with open(GAMES_IDS_FILE, 'w') as f:
        f.write(games_ids)
    print(f"  Updated {GAMES_IDS_FILE}")
    
    # Generate PlayGamesManager mapping code
    print("\nGenerating PlayGamesManager switch statement...")
    mapping_code = generate_playgames_manager_mapping(achievements)
    mapping_file = OUTPUT_DIR / "PlayGamesManager_mapping.java.txt"
    with open(mapping_file, 'w') as f:
        f.write(mapping_code)
    print(f"  Created {mapping_file}")
    
    # Summary
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    print(f"Total achievements: {len(achievements)}")
    print(f"\nFiles created in {OUTPUT_DIR}:")
    print("  - AchievementsMetadata.csv (upload to Play Console)")
    print("  - PlayGamesManager_mapping.java.txt (copy to PlayGamesManager.java)")
    print(f"\nUpdated: {GAMES_IDS_FILE}")
    print("\nNext steps:")
    print("1. Create a ZIP file: cd dev/pgs_import && zip achievements.zip AchievementsMetadata.csv")
    print("2. Upload to Play Console > Play Games Services > Achievements > Import")
    print("3. After import, copy each achievement ID from Play Console")
    print("4. Replace REPLACE_WITH_ACHIEVEMENT_ID in games-ids.xml")
    print("5. Copy the switch statement from PlayGamesManager_mapping.java.txt")
    print("   to PlayGamesManager.java")

if __name__ == "__main__":
    main()

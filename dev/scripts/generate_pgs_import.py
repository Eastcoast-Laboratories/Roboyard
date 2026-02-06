#!/usr/bin/env python3
"""
Generate Google Play Games Services Achievement Import Files

This script generates the CSV files needed for bulk-importing achievements
into Google Play Console. It reads the achievement definitions from the
Android app's AchievementDefinitions.java and resolves names/descriptions
from strings.xml.

Output (in dev/pgs_import/):
  - AchievementsMetadata.csv
  - games-ids.xml (placeholder IDs for Android resources)
  - PlayGamesManager_mapping.java.txt
  - achievements_import.zip (ready to upload)

Usage:
    python3 generate_pgs_import.py              # Export all achievements
    python3 generate_pgs_import.py --limit 2   # Export only 2 random achievements (for testing)
"""

import os
import re
import shutil
import sys
import random
import xml.etree.ElementTree as ET
import zipfile
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
    "SPECIAL": 20,
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

def parse_strings_xml(lang="en"):
    """Parse strings.xml to get achievement names and descriptions.
    
    Args:
        lang: Language code ("en" for English, "de" for German, etc.)
    """
    strings = {}
    
    if lang == "en":
        file_path = STRINGS_FILE
    else:
        # Go up to res directory, then into values-{lang}
        file_path = STRINGS_FILE.parent.parent / f"values-{lang}" / "strings.xml"
    
    if not file_path.exists():
        return strings
    
    tree = ET.parse(file_path)
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
    """Parse AchievementDefinitions.java to extract all achievement IDs, categories, and icons.
    
    Handles both:
    - Direct add() calls with constant or string-literal icon params
    - Loop-generated achievements (solution_X_moves)
    """
    achievements = []
    icon_constants = {}  # Map ICON_* constants to actual icon names
    
    with open(DEFINITIONS_FILE, 'r') as f:
        content = f.read()
    
    # First, extract icon constant definitions
    icon_pattern = r'private static final String (ICON_\w+)\s*=\s*"([^"]+)"'
    for match in re.finditer(icon_pattern, content):
        const_name = match.group(1)
        icon_name = match.group(2)
        icon_constants[const_name] = icon_name
    
    # Remove block comments (/* ... */) and line comments (// ...)
    content_no_comments = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    # Keep commented-out add() calls excluded (lines starting with //)
    lines = []
    for line in content_no_comments.split('\n'):
        stripped = line.strip()
        if stripped.startswith('//'):
            continue
        lines.append(line)
    content_clean = '\n'.join(lines)
    
    # Match add(new Achievement("id", "title_key", "desc_key", AchievementCategory.XXX, <icon>));
    # <icon> can be a constant name (ICON_FLAME) or a string literal ("icon_55_cone")
    pattern = r'add\(new Achievement\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*AchievementCategory\.(\w+)\s*,\s*(?:"([^"]*)"|(\w+))\s*\)\)'
    
    for match in re.finditer(pattern, content_clean):
        achievement_id = match.group(1)
        title_key = match.group(2)
        desc_key = match.group(3)
        category = match.group(4)
        icon_literal = match.group(5)  # String literal like "icon_55_cone"
        icon_constant = match.group(6)  # Constant name like ICON_FLAME
        
        # Resolve icon name
        if icon_literal:
            icon_name = icon_literal
        elif icon_constant in icon_constants:
            icon_name = icon_constants[icon_constant]
        else:
            icon_name = icon_constant  # Fallback to constant name
        
        achievements.append({
            "id": achievement_id,
            "title_key": title_key,
            "desc_key": desc_key,
            "category": category,
            "icon": icon_name,
        })
    
    # Detect loop-generated achievements: for (int moves = X; moves <= Y; moves++)
    loop_pattern = r'for\s*\(int\s+moves\s*=\s*(\d+);\s*moves\s*<=\s*(\d+);'
    loop_match = re.search(loop_pattern, content_clean)
    if loop_match:
        start = int(loop_match.group(1))
        end = int(loop_match.group(2))
        # Find the add() inside the loop body
        loop_add = re.search(
            r'for\s*\(int\s+moves.*?\{[^}]*add\(new Achievement\([^)]*"([^"]*?)"\s*\+\s*moves\s*\+\s*"([^"]*?)"\s*,\s*"([^"]*?)"\s*\+\s*moves\s*,\s*"([^"]*?)"\s*\+\s*moves\s*\+\s*"([^"]*?)"',
            content_clean, re.DOTALL
        )
        if loop_add:
            id_prefix = loop_add.group(1)
            id_suffix = loop_add.group(2)
            title_prefix = loop_add.group(3)
            desc_prefix = loop_add.group(4)
            desc_suffix = loop_add.group(5)
            for moves in range(start, end + 1):
                aid = f"{id_prefix}{moves}{id_suffix}"
                # Only add if not already found by the direct pattern
                if not any(a["id"] == aid for a in achievements):
                    achievements.append({
                        "id": aid,
                        "title_key": f"{title_prefix}{moves}",
                        "desc_key": f"{desc_prefix}{moves}{desc_suffix}",
                        "category": "RANDOM_SOLUTION",
                        "icon": "icon_24_robot_gold",  # Default icon for solution achievements
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

def generate_localizations_csv(achievements, strings_en, strings_de):
    """Generate AchievementsLocalizations.csv with translations.
    
    Format per Google Play Console:
    Name, Localized name, Localized description, locale
    
    Note: If no locale is specified, Google Play Console uses the default game language.
    This file can be empty or omitted if using only the default language.
    """
    lines = []
    
    # Return empty CSV - use AchievementsMetadata.csv as default language
    # Translations can be added later via Play Console
    
    return "\n".join(lines)

def generate_icon_mappings_csv(achievements, strings_en):
    """Generate AchievementsIconsMappings.csv
    
    Format: Achievement Name (from metadata), icon filename
    Maps each achievement to its corresponding PNG icon file.
    Converts icon names from AchievementDefinitions.java (icon_46_flame) to actual files (46_flame.png)
    """
    lines = []
    
    for ach in achievements:
        # Use the achievement name from strings (same as in metadata)
        name = strings_en.get(ach["title_key"], ach["id"].replace("_", " ").title())
        
        # Remove commas (CSV requirement)
        name = name.replace(",", " -")
        
        # Convert icon name from "icon_46_flame" to "46_flame.png"
        # Remove "icon_" prefix if present
        icon_base = ach['icon']
        if icon_base.startswith('icon_'):
            icon_base = icon_base[5:]  # Remove "icon_" prefix
        icon_name = f"{icon_base}.png"
        
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

def validate_achievements(achievements, strings):
    """Validate that all achievements have matching strings."""
    missing = []
    for ach in achievements:
        if ach["title_key"] not in strings:
            missing.append(f"  MISSING title: {ach['title_key']} (for {ach['id']})")
        if ach["desc_key"] not in strings:
            missing.append(f"  MISSING desc:  {ach['desc_key']} (for {ach['id']})")
    return missing


def copy_achievement_icons(output_dir, achievements):
    """Copy achievement icon PNG files to output directory and resize to 512x512."""
    icons_source_dir = PROJECT_ROOT / "app/src/main/res/drawable/achievements_icons"
    
    if not icons_source_dir.exists():
        print(f"  WARNING: Icons directory not found: {icons_source_dir}")
        return []
    
    try:
        from PIL import Image
    except ImportError:
        print(f"  WARNING: PIL not available, copying icons without resizing")
        copied = []
        for ach in achievements:
            icon_name = ach['icon']
            if icon_name.startswith('icon_'):
                icon_name = icon_name[5:]
            icon_file = icons_source_dir / f"{icon_name}.png"
            
            if icon_file.exists():
                dest_file = output_dir / icon_file.name
                shutil.copy2(icon_file, dest_file)
                copied.append(icon_file.name)
        return copied
    
    copied = []
    for ach in achievements:
        icon_name = ach['icon']
        if icon_name.startswith('icon_'):
            icon_name = icon_name[5:]
        icon_file = icons_source_dir / f"{icon_name}.png"
        
        if icon_file.exists():
            dest_file = output_dir / icon_file.name
            
            # Resize to 512x512 for Google Play Console (only in output dir, not original)
            try:
                img = Image.open(icon_file)
                img_resized = img.resize((512, 512), Image.Resampling.LANCZOS)
                img_resized.save(dest_file, 'PNG', quality=95)
            except Exception as e:
                # Fallback: just copy if resize fails
                shutil.copy2(icon_file, dest_file)
            
            copied.append(icon_file.name)
    
    return copied


def create_zip(output_dir, zip_name="achievements_import.zip"):
    """Create a ZIP file containing only CSV files and PNG icons.
    
    Excludes .txt and other files.
    """
    zip_path = output_dir / zip_name
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        for f in sorted(output_dir.iterdir()):
            # Only include CSV and PNG files
            if f.name != zip_name and f.is_file() and f.suffix in ['.csv', '.png']:
                zf.write(f, f.name)
    return zip_path


def main():
    # Parse command line arguments
    limit = None
    if len(sys.argv) > 1:
        if sys.argv[1] == "--limit" and len(sys.argv) > 2:
            try:
                limit = int(sys.argv[2])
            except ValueError:
                print(f"Error: --limit requires an integer argument")
                sys.exit(1)
        else:
            print(f"Usage: python3 generate_pgs_import.py [--limit N]")
            sys.exit(1)
    
    print("Generating Google Play Games Services import files...\n")
    
    # Parse strings
    print("Parsing strings.xml (English)...")
    strings_en = parse_strings_xml("en")
    print(f"  Found {len(strings_en)} achievement-related strings")
    
    print("Parsing strings-de.xml (German)...")
    strings_de = parse_strings_xml("de")
    print(f"  Found {len(strings_de)} German achievement strings")
    
    # Parse achievements
    print("Parsing AchievementDefinitions.java...")
    achievements = parse_achievement_definitions()
    print(f"  Found {len(achievements)} achievements")
    
    # Apply limit if specified
    if limit:
        random.shuffle(achievements)
        achievements = achievements[:limit]
        print(f"  Limited to {limit} random achievements for testing")
    
    # Validate
    missing = validate_achievements(achievements, strings_en)
    if missing:
        print(f"\n  WARNING: {len(missing)} missing string(s):")
        for m in missing:
            print(m)
        print()
    
    # Print all found achievements for verification
    print("\nAchievements found:")
    categories = {}
    for ach in achievements:
        cat = ach["category"]
        categories.setdefault(cat, [])
        categories[cat].append(ach)
    for cat, achs in categories.items():
        name_str = strings_en.get(achs[0]["title_key"], achs[0]["id"])
        print(f"  {cat} ({len(achs)}): {name_str}, ...")
    
    # Generate CSV files
    print("\nGenerating CSV files...")
    
    metadata = generate_metadata_csv(achievements, strings_en)
    metadata_file = OUTPUT_DIR / "AchievementsMetadata.csv"
    with open(metadata_file, 'w') as f:
        f.write(metadata)
    print(f"  Created {metadata_file}")
    
    # Generate localizations CSV
    print("\nGenerating AchievementsLocalizations.csv...")
    localizations = generate_localizations_csv(achievements, strings_en, strings_de)
    localizations_file = OUTPUT_DIR / "AchievementsLocalizations.csv"
    with open(localizations_file, 'w') as f:
        f.write(localizations)
    print(f"  Created {localizations_file}")
    
    # Generate icon mappings CSV
    print("\nGenerating AchievementsIconsMappings.csv...")
    icon_mappings = generate_icon_mappings_csv(achievements, strings_en)
    icon_mappings_file = OUTPUT_DIR / "AchievementsIconsMappings.csv"
    with open(icon_mappings_file, 'w') as f:
        f.write(icon_mappings)
    print(f"  Created {icon_mappings_file}")
    
    # Copy achievement icons
    print("\nCopying achievement icons...")
    copied_icons = copy_achievement_icons(OUTPUT_DIR, achievements)
    print(f"  Copied {len(copied_icons)} icon files")
    
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
    
    # Create ZIP (only CSV + PNG)
    print("\nCreating ZIP archive (CSV + PNG only)...")
    zip_path = create_zip(OUTPUT_DIR)
    print(f"  Created {zip_path}")
    
    # Summary
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    print(f"Total achievements: {len(achievements)}")
    print(f"\nZIP contents (dev/pgs_import/achievements_import.zip):")
    print(f"  - AchievementsMetadata.csv (68 achievements - default language)")
    print(f"  - AchievementsIconsMappings.csv (achievement → icon mappings)")
    print(f"  - {len(copied_icons)} achievement icon PNG files")
    print(f"\nOther files (not in ZIP):")
    print("  - PlayGamesManager_mapping.java.txt")
    print(f"\nUpdated: {GAMES_IDS_FILE}")
    print("\nNext steps:")
    print("1. Upload achievements_import.zip to Play Console > Play Games Services > Achievements > Import")
    print("2. After import, copy each achievement ID from Play Console")
    print("3. Replace REPLACE_WITH_ACHIEVEMENT_ID in games-ids.xml")
    print("4. Copy the switch statement from PlayGamesManager_mapping.java.txt")
    print("   to PlayGamesManager.java")

if __name__ == "__main__":
    import shutil
    main()

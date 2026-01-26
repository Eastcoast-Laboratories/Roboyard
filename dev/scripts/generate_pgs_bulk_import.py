#!/usr/bin/env python3
"""
Generate bulk import files for Google Play Games Services achievements.
Creates AchievementsMetadata.csv, AchievementsLocalizations.csv, 
AchievementsIconsMappings.csv and packages them into a ZIP file.
"""

import csv
import os
import zipfile
from pathlib import Path

# Achievement data with German translations
ACHIEVEMENTS = [
    ("Welcome!", "Play your first game", "Willkommen!", "Spiele dein erstes Spiel"),
    ("First Steps", "Complete Level 1", "Erste Schritte", "Vervollständige Level 1"),
    ("Getting Started", "Complete 10 levels", "Erste Schritte", "Vervollständige 10 Level"),
    ("Halfway There", "Complete 50 levels", "Halbwegs dort", "Vervollständige 50 Level"),
    ("Level Master", "Complete all 140 levels", "Level-Meister", "Vervollständige alle 140 Level"),
    ("Star Collector", "Collect all 420 stars", "Stern-Sammler", "Sammle alle 420 Sterne"),
    ("Perfect Mover", "Solve 5 levels with optimal moves", "Perfekter Zug", "Löse 5 Level mit optimalen Zügen"),
    ("Precision Player", "Solve 10 levels with optimal moves", "Präzisions-Spieler", "Löse 10 Level mit optimalen Zügen"),
    ("Optimization Expert", "Solve 50 levels with optimal moves", "Optimierungs-Experte", "Löse 50 Level mit optimalen Zügen"),
    ("Quick Thinker", "Complete a level in under 30 seconds", "Schnelldenker", "Vervollständige ein Level in unter 30 Sekunden"),
    ("Lightning Fast", "Complete a level in under 10 seconds", "Blitzschnell", "Vervollständige ein Level in unter 10 Sekunden"),
    ("Hard Level Star", "Achieve 3 stars on a level with 5+ optimal moves", "Schwierigkeits-Stern", "Erreiche 3 Sterne auf einem Level mit 5+ optimalen Zügen"),
    ("Rising Star", "Achieve 3 stars on 10 levels", "Aufstrebender Stern", "Erreiche 3 Sterne auf 10 Leveln"),
    ("Hard Level Master", "Achieve 3 stars on 10 levels with 5+ optimal moves", "Schwierigkeits-Meister", "Erreiche 3 Sterne auf 10 Leveln mit 5+ optimalen Zügen"),
    ("Superstar", "Achieve 3 stars on 50 levels", "Superstar", "Erreiche 3 Sterne auf 50 Leveln"),
    ("Perfect Master", "Achieve 3 stars on all 140 levels", "Perfekter Meister", "Erreiche 3 Sterne auf allen 140 Leveln"),
    ("Weekly Player", "Log in 7 days in a row", "Wöchentlicher Spieler", "Melde dich 7 Tage hintereinander an"),
    ("Dedicated Player", "Log in 30 days in a row", "Engagierter Spieler", "Melde dich 30 Tage hintereinander an"),
    ("Welcome Back!", "Return after 30 days of inactivity", "Willkommen zurück!", "Kehre nach 30 Tagen Inaktivität zurück"),
    ("Impossible Dream", "Complete 1 game in Impossible mode", "Unmöglicher Traum", "Vervollständige 1 Spiel im unmöglichen Modus"),
    ("Impossible Champion", "Complete 5 games in Impossible mode", "Unmöglicher Champion", "Vervollständige 5 Spiele im unmöglichen Modus"),
    ("Impossible Streak", "Complete 5 games in a row in Impossible mode with optimal moves", "Unmögliche Serie", "Vervollständige 5 Spiele hintereinander im unmöglichen Modus mit optimalen Zügen"),
    ("Impossible Legend", "Complete 10 games in a row in Impossible mode with optimal moves", "Unmögliche Legende", "Vervollständige 10 Spiele hintereinander im unmöglichen Modus mit optimalen Zügen"),
    ("18 Move Master", "Complete a game with optimal solution of 18 moves", "18-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 18 Zügen"),
    ("19 Move Master", "Complete a game with optimal solution of 19 moves", "19-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 19 Zügen"),
    ("20 Move Master", "Complete a game with optimal solution of 20 moves", "20-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 20 Zügen"),
    ("21 Move Master", "Complete a game with optimal solution of 21 moves", "21-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 21 Zügen"),
    ("22 Move Master", "Complete a game with optimal solution of 22 moves", "22-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 22 Zügen"),
    ("23 Move Master", "Complete a game with optimal solution of 23 moves", "23-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 23 Zügen"),
    ("24 Move Master", "Complete a game with optimal solution of 24 moves", "24-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 24 Zügen"),
    ("25 Move Master", "Complete a game with optimal solution of 25 moves", "25-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 25 Zügen"),
    ("26 Move Master", "Complete a game with optimal solution of 26 moves", "26-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 26 Zügen"),
    ("27 Move Master", "Complete a game with optimal solution of 27 moves", "27-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 27 Zügen"),
    ("28 Move Master", "Complete a game with optimal solution of 28 moves", "28-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 28 Zügen"),
    ("29 Move Master", "Complete a game with optimal solution of 29 moves", "29-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 29 Zügen"),
    ("30+ Move Master", "Complete a game with optimal solution of 30+ moves", "30+-Zug-Meister", "Vervollständige ein Spiel mit optimaler Lösung von 30+ Zügen"),
    ("Resolution Explorer 10", "Play games with 10+ moves on all screen resolutions", "Auflösungs-Erkunder 10", "Spiele Spiele mit 10+ Zügen auf allen Bildschirmauflösungen"),
    ("Resolution Explorer 12", "Play games with 12+ moves on all screen resolutions", "Auflösungs-Erkunder 12", "Spiele Spiele mit 12+ Zügen auf allen Bildschirmauflösungen"),
    ("Resolution Explorer 15", "Play games with 15+ moves on all screen resolutions", "Auflösungs-Erkunder 15", "Spiele Spiele mit 15+ Zügen auf allen Bildschirmauflösungen"),
    ("Double Target", "Complete a game with 2 targets", "Doppeltes Ziel", "Vervollständige ein Spiel mit 2 Zielen"),
    ("Triple Target", "Complete a game with 3 targets", "Dreifaches Ziel", "Vervollständige ein Spiel mit 3 Zielen"),
    ("Quad Target", "Complete a game with 4 targets", "Vierfaches Ziel", "Vervollständige ein Spiel mit 4 Zielen"),
    ("2 of 2", "Complete a game where you need 2 out of 2 targets", "2 von 2", "Vervollständige ein Spiel, bei dem du 2 von 2 Zielen brauchst"),
    ("2 of 3", "Complete a game where you need 2 out of 3 targets", "2 von 3", "Vervollständige ein Spiel, bei dem du 2 von 3 Zielen brauchst"),
    ("2 of 4", "Complete a game where you need 2 out of 4 targets", "2 von 4", "Vervollständige ein Spiel, bei dem du 2 von 4 Zielen brauchst"),
    ("3 of 3", "Complete a game where you need 3 out of 3 targets", "3 von 3", "Vervollständige ein Spiel, bei dem du 3 von 3 Zielen brauchst"),
    ("3 of 4", "Complete a game where you need 3 out of 4 targets", "3 von 4", "Vervollständige ein Spiel, bei dem du 3 von 4 Zielen brauchst"),
    ("4 of 4", "Complete a game where you need all 4 targets", "4 von 4", "Vervollständige ein Spiel, bei dem du alle 4 Ziele brauchst"),
    ("Full Team", "Complete a game with 5 robots", "Volles Team", "Vervollständige ein Spiel mit 5 Robotern"),
    ("Gimme Five", "All robots must touch each other", "Gib mir Fünf", "Alle Roboter müssen sich gegenseitig berühren"),
    ("Solo Explorer", "Visit all squares on the board with 1 robot", "Solo-Erkunder", "Besuche alle Felder auf dem Brett mit 1 Roboter"),
    ("Solo Goal Explorer", "Visit all squares on the board with 1 robot in goal mode", "Solo-Ziel-Erkunder", "Besuche alle Felder auf dem Brett mit 1 Roboter im Ziel-Modus"),
    ("Team Explorer", "Visit all squares on the board with all robots", "Team-Erkunder", "Besuche alle Felder auf dem Brett mit allen Robotern"),
    ("Team Goal Explorer", "Visit all squares on the board with all robots in goal mode", "Team-Ziel-Erkunder", "Besuche alle Felder auf dem Brett mit allen Robotern im Ziel-Modus"),
    ("Perfect 5", "Solve 5 random games with optimal moves", "Perfekt 5", "Löse 5 Zufallsspiele mit optimalen Zügen"),
    ("Perfect 10", "Solve 10 random games with optimal moves", "Perfekt 10", "Löse 10 Zufallsspiele mit optimalen Zügen"),
    ("Perfect 20", "Solve 20 random games with optimal moves", "Perfekt 20", "Löse 20 Zufallsspiele mit optimalen Zügen"),
    ("Perfect Streak 5", "Solve 5 random games in a row with optimal moves", "Perfekte Serie 5", "Löse 5 Zufallsspiele hintereinander mit optimalen Zügen"),
    ("Perfect Streak 10", "Solve 10 random games in a row with optimal moves", "Perfekte Serie 10", "Löse 10 Zufallsspiele hintereinander mit optimalen Zügen"),
    ("Perfect Streak 20", "Solve 20 random games in a row with optimal moves", "Perfekte Serie 20", "Löse 20 Zufallsspiele hintereinander mit optimalen Zügen"),
    ("No Help Needed 10", "Complete 10 random games without hints", "Keine Hilfe nötig 10", "Vervollständige 10 Zufallsspiele ohne Hinweise"),
    ("No Help Needed 50", "Complete 50 random games without hints", "Keine Hilfe nötig 50", "Vervollständige 50 Zufallsspiele ohne Hinweise"),
    ("No Help Streak 10", "Complete 10 random games in a row without hints", "Keine-Hilfe-Serie 10", "Vervollständige 10 Zufallsspiele hintereinander ohne Hinweise"),
    ("No Help Streak 50", "Complete 50 random games in a row without hints", "Keine-Hilfe-Serie 50", "Vervollständige 50 Zufallsspiele hintereinander ohne Hinweise"),
    ("Speed Demon", "Complete a random game in under 20 seconds", "Geschwindigkeits-Dämon", "Vervollständige ein Zufallsspiel in unter 20 Sekunden"),
    ("Lightning Speed", "Complete a random game in under 10 seconds", "Blitzgeschwindigkeit", "Vervollständige ein Zufallsspiel in unter 10 Sekunden"),
    ("Speed Streak", "Complete 5 random games in a row in under 30 seconds each", "Geschwindigkeits-Serie", "Vervollständige 5 Zufallsspiele hintereinander in unter 30 Sekunden pro Spiel"),
]

def generate_metadata_csv(output_path):
    """Generate AchievementsMetadata.csv"""
    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        for idx, (en_name, en_desc, _, _) in enumerate(ACHIEVEMENTS, 1):
            # Name, Description, Incremental Value, Steps Needed, Initial State, Points, List Order
            writer.writerow([en_name, en_desc, "False", "", "Revealed", "10", str(idx)])

def generate_localizations_csv(output_path):
    """Generate AchievementsLocalizations.csv with German translations"""
    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        for en_name, en_desc, de_name, de_desc in ACHIEVEMENTS:
            # German translations
            writer.writerow([en_name, de_name, de_desc, "de"])

def generate_icon_mappings_csv(output_path):
    """Generate AchievementsIcons
Mappings.csv"""
    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        for en_name, _, _, _ in ACHIEVEMENTS:
            # Use a generic icon name - in production, you'd map specific icons
            icon_name = f"{en_name.lower().replace(' ', '_')}.png"
            writer.writerow([en_name, icon_name])

def create_bulk_import_zip(output_dir):
    """Create a ZIP file with all necessary CSV files for bulk import"""
    metadata_path = os.path.join(output_dir, "AchievementsMetadata.csv")
    localizations_path = os.path.join(output_dir, "AchievementsLocalizations.csv")
    icon_mappings_path = os.path.join(output_dir, "AchievementsIcons
Mappings.csv")
    zip_path = os.path.join(output_dir, "achievements_bulk_import.zip")
    
    # Generate CSV files
    generate_metadata_csv(metadata_path)
    generate_localizations_csv(localizations_path)
    generate_icon_mappings_csv(icon_mappings_path)
    
    # Create ZIP file
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        zipf.write(metadata_path, arcname="AchievementsMetadata.csv")
        zipf.write(localizations_path, arcname="AchievementsLocalizations.csv")
        zipf.write(icon_mappings_path, arcname="AchievementsIcons
    Mappings.csv")
    
    print(f"✅ Generated bulk import files:")
    print(f"   - AchievementsMetadata.csv")
    print(f"   - AchievementsLocalizations.csv")
    print(f"   - AchievementsIcons
Mappings.csv")
    print(f"   - achievements_bulk_import.zip")
    print(f"\n📦 ZIP file ready for upload: {zip_path}")
    print(f"\n📋 Next steps:")
    print(f"   1. Go to Play Console > Grow > Play Games Services > Achievements")
    print(f"   2. Click 'Import achievements'")
    print(f"   3. Upload: {zip_path}")
    print(f"   4. Review and save as draft")
    print(f"   5. Publish Play Games Services")

if __name__ == "__main__":
    output_dir = Path(__file__).parent.parent / "pgs_import"
    output_dir.mkdir(parents=True, exist_ok=True)
    create_bulk_import_zip(str(output_dir))

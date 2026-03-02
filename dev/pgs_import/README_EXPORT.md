# PGS Achievement Mappings Export Tool

Flexibles Export-Tool für Google Play Games Achievement-Mappings aus `AchievementDefinitions.java`.

## Verwendung

### Neue Wall-Achievements exportieren (nur diese 3)
```bash
python3 export_pgs_mappings.py --achievements "same_walls_2,same_walls_10,same_walls_100"
```

### Alle Achievements einer Kategorie exportieren
```bash
# Alle random_robots Achievements
python3 export_pgs_mappings.py --category random_robots

# Alle progression Achievements
python3 export_pgs_mappings.py --category progression
```

### Alle Achievements exportieren
```bash
python3 export_pgs_mappings.py --all
```

## Export-Formate

### Java Switch-Statement (Standard)
```bash
python3 export_pgs_mappings.py --achievements "same_walls_2" --format switch
```

Ausgabe:
```java
private String getPlayGamesAchievementId(String localId) {
    try {
        int resId = 0;
        switch (localId) {
            case "same_walls_2":
                resId = R.string.pgs_same_walls_2;
                break;
            // ...
        }
        return context.getString(resId);
    } catch (Exception e) {
        // ...
    }
}
```

### Java LinkedHashMap
```bash
python3 export_pgs_mappings.py --category random_robots --format map
```

Ausgabe:
```java
private static final Map<String, String> PLAY_GAMES_MAPPINGS = new LinkedHashMap<String, String>() {{
    put("game_5_robots", "pgs_full_team");
    put("gimme_five", "pgs_gimme_five");
    put("same_walls_2", "pgs_same_walls_2");
    // ...
}};
```

### CSV-Format
```bash
python3 export_pgs_mappings.py --all --format csv
```

Ausgabe:
```csv
achievement_id,pgs_resource_key
daily_login_7,pgs_weekly_player
daily_login_30,pgs_dedicated_player
// ...
```

## Verfügbare Kategorien

```bash
python3 export_pgs_mappings.py --list-categories
```

Ausgabe:
```
Available achievement categories:
  login: 3 achievements
  progression: 6 achievements
  performance: 5 achievements
  mastery: 5 achievements
  random_speed: 3 achievements
  random_streaks: 11 achievements
  random_difficulty: 4 achievements
  random_solution: 13 achievements
  random_resolution: 3 achievements
  random_targets: 9 achievements
  random_robots: 5 achievements
  random_coverage: 4 achievements
```

## Ausgabe in Datei speichern

```bash
python3 export_pgs_mappings.py --achievements "same_walls_2,same_walls_10,same_walls_100" \
  --format switch \
  --output new_wall_achievements.txt
```

## Beispiele

### Nur neue Wall-Achievements als Switch-Statement
```bash
python3 export_pgs_mappings.py \
  --achievements "same_walls_2,same_walls_10,same_walls_100" \
  --format switch \
  --output wall_achievements_switch.txt
```

### Alle random_robots Achievements als Map
```bash
python3 export_pgs_mappings.py \
  --category random_robots \
  --format map \
  --output random_robots_map.txt
```

### Mehrere Achievements manuell auswählen
```bash
python3 export_pgs_mappings.py \
  --achievements "same_walls_2,game_5_robots,gimme_five" \
  --format csv \
  --output selected_achievements.csv
```

## Workflow

1. **Neue Achievements in AchievementDefinitions.java hinzufügen**
   - Füge Achievement-Definition hinzu
   - Füge Mapping in `PLAY_GAMES_MAPPINGS` hinzu

2. **Exportieren mit diesem Tool**
   ```bash
   python3 export_pgs_mappings.py --achievements "new_achievement_id" --format switch
   ```

3. **In Google Play Games Console importieren**
   - Kopiere die exportierte Switch-Statement oder Map
   - Importiere in Play Games Console

## Dateistruktur

```
dev/pgs_import/
├── export_pgs_mappings.py          # Dieses Export-Tool
├── README_EXPORT.md                # Diese Dokumentation
├── PlayGamesManager_mapping.java.txt # Alte Export-Datei (veraltet)
├── AchievementsMetadata.csv        # Alte Metadaten
└── achievements_import.zip         # Alte Import-Datei
```

## Hinweise

- Das Tool liest die Mappings aus dem Python-Skript selbst (nicht aus der Java-Datei)
- Bei neuen Achievements müssen beide Orte aktualisiert werden:
  1. `AchievementDefinitions.java` - Java-Mapping
  2. `export_pgs_mappings.py` - Python-Export-Mappings
- Das Tool validiert Achievement-IDs und warnt vor unbekannten IDs

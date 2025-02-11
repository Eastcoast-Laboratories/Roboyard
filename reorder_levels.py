#!/usr/bin/env python3
import os
import shutil

# Level die an den Anfang sollen
move_to_start = [10, 13, 15, 16, 26, 29, 30, 31, 38, 45, 47, 50, 53, 57, 61, 62, 65, 66, 68, 71, 72, 73, 75, 76, 77, 80, 83, 87, 108, 109, 113, 118, 119, 122, 126, 134, 136, 137, 138, 140]

# Level die ans Ende sollen
move_to_end = [1, 9, 11, 14, 41, 55, 70, 82, 85, 86, 90, 94, 95, 97, 100, 104, 107, 116, 120, 125, 127, 131]

# Alle anderen Level
all_levels = set(range(1, 141))  # 1-140
remaining = sorted(list(all_levels - set(move_to_start) - set(move_to_end)))

# Neue Reihenfolge erstellen
new_order = move_to_start + remaining + move_to_end

# Maps neu anordnen
maps_dir = "app/src/main/assets/Maps"
temp_dir = "app/src/main/assets/Maps_temp"

for new_num, old_num in enumerate(new_order, 1):
    src = f"{temp_dir}/level_{old_num}.txt"
    dst = f"{maps_dir}/level_{new_num}.txt"
    shutil.copy2(src, dst)

# Temp-Verzeichnis löschen
shutil.rmtree(temp_dir)

print("Level-Maps wurden neu angeordnet!")

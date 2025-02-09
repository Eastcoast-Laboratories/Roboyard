#!/usr/bin/env python3
import random
import os
import subprocess
from concurrent.futures import ProcessPoolExecutor

def get_board_size():
    """Get board size"""
    return (12, 14)  # Standard-Größe für alle Level

def generate_map():
    """Generate a map with walls, robots and target"""
    width, height = get_board_size()
    
    # Initialize wall arrays
    horizontal_walls = [[0] * (height + 1) for _ in range(width + 1)]
    vertical_walls = [[0] * (height + 1) for _ in range(width + 1)]
    
    # Add border walls
    for x in range(width):
        horizontal_walls[x][0] = 1  # Top wall
        horizontal_walls[x][height] = 1  # Bottom wall
    for y in range(height):
        vertical_walls[0][y] = 1  # Left wall
        vertical_walls[width][y] = 1  # Right wall
    
    # Carré in der Mitte definieren (2x2 Felder)
    center_x = width // 2 - 1  # Linke Kante des Carrés
    center_y = height // 2 - 1  # Obere Kante des Carrés
    
    # Debug-Ausgabe der Carré-Position
    print(f"Carré Position: x={center_x},{center_x+1} y={center_y},{center_y+1}")
    
    # Horizontale Wände um das Carré (oben und unten)
    horizontal_walls[center_x][center_y] = 1      # Oben links
    horizontal_walls[center_x+1][center_y] = 1    # Oben rechts
    horizontal_walls[center_x][center_y+2] = 1    # Unten links
    horizontal_walls[center_x+1][center_y+2] = 1  # Unten rechts
    
    # Vertikale Wände um das Carré (links und rechts)
    vertical_walls[center_x][center_y] = 1      # Links oben
    vertical_walls[center_x][center_y+1] = 1    # Links unten
    vertical_walls[center_x+2][center_y] = 1    # Rechts oben
    vertical_walls[center_x+2][center_y+1] = 1  # Rechts unten
    
    # Zähle die bereits platzierten Wände
    wall_count = 8  # 8 Wände fürs Carré
    max_walls = 50
    
    # Verteile die restlichen Wände
    remaining_walls = max_walls - wall_count
    
    # 40% der Wände sollen den Rand berühren (mehr Randwände)
    edge_walls = int(remaining_walls * 0.4)
    inner_walls = remaining_walls - edge_walls
    
    # Platziere Wände am Rand
    attempts = 0
    while edge_walls > 0 and attempts < 1000:
        attempts += 1
        if random.random() < 0.5:  # Horizontale Wand
            x = random.randint(1, width-2)
            y = random.choice([1, height-1])  # Nur nahe am oberen/unteren Rand
            if not horizontal_walls[x][y]:
                horizontal_walls[x][y] = 1
                # Füge mit 50% Chance eine senkrechte Wand hinzu
                if random.random() < 0.5:
                    if y == 1 and not vertical_walls[x][0]:
                        vertical_walls[x][0] = 1
                        edge_walls -= 1
                    elif y == height-1 and not vertical_walls[x][height-1]:
                        vertical_walls[x][height-1] = 1
                        edge_walls -= 1
                edge_walls -= 1
                wall_count += 1
        else:  # Vertikale Wand
            x = random.choice([1, width-1])  # Nur nahe am linken/rechten Rand
            y = random.randint(1, height-2)
            if not vertical_walls[x][y]:
                vertical_walls[x][y] = 1
                # Füge mit 50% Chance eine horizontale Wand hinzu
                if random.random() < 0.5:
                    if x == 1 and not horizontal_walls[0][y]:
                        horizontal_walls[0][y] = 1
                        edge_walls -= 1
                    elif x == width-1 and not horizontal_walls[width-1][y]:
                        horizontal_walls[width-1][y] = 1
                        edge_walls -= 1
                edge_walls -= 1
                wall_count += 1
    
    # Platziere die restlichen Wände im Inneren
    attempts = 0
    while wall_count < max_walls and attempts < 1000:
        attempts += 1
        if random.random() < 0.5:  # Horizontale Wand
            x = random.randint(1, width-2)
            y = random.randint(2, height-2)  # Nicht direkt am Rand
            # Überspringe das Carré
            if (y == center_y or y == center_y+2) and \
               (x == center_x or x == center_x+1):
                continue
            if not horizontal_walls[x][y]:
                horizontal_walls[x][y] = 1
                wall_count += 1
        else:  # Vertikale Wand
            x = random.randint(2, width-2)  # Nicht direkt am Rand
            y = random.randint(1, height-2)
            # Überspringe das Carré
            if (x == center_x or x == center_x+2) and \
               (y == center_y or y == center_y+1):
                continue
            if not vertical_walls[x][y]:
                vertical_walls[x][y] = 1
                wall_count += 1
    
    # Convert walls to map format
    result = [f"board:{width},{height};"]
    
    # Add horizontal walls
    for x in range(width):
        for y in range(height + 1):
            if horizontal_walls[x][y]:
                result.append(f"mh{x},{y};")
    
    # Add vertical walls
    for x in range(width + 1):
        for y in range(height):
            if vertical_walls[x][y]:
                result.append(f"mv{x},{y};")
    
    # Add target (nicht im Carré)
    while True:
        x = random.randint(1, width-2)
        y = random.randint(1, height-2)
        if not (x in [center_x, center_x+1] and y in [center_y, center_y+1]):
            break
    target_type = random.choice(['target_red', 'target_blue', 'target_yellow', 'target_green'])
    result.append(f"{target_type}{x},{y};")
    
    # Add robots (nicht im Carré)
    robot_positions = []
    for robot_type in ['robot_red', 'robot_blue', 'robot_yellow', 'robot_green']:
        while True:
            x = random.randint(1, width-2)
            y = random.randint(1, height-2)
            if (x,y) not in robot_positions and \
               not (x in [center_x, center_x+1] and y in [center_y, center_y+1]):
                robot_positions.append((x,y))
                result.append(f"{robot_type}{x},{y};")
                break
    
    return "\n".join(result)

def solve_level(map_str):
    """Use the Java solver to determine number of moves needed"""
    try:
        # print(f"Testing map:\n{map_str}")
        result = subprocess.run(
            ["java", "-cp", ".", "GeneratorLevelSolver"],  # Use the generator solver
            input=map_str,
            capture_output=True,
            text=True,
            cwd="tools"
        )
        moves = int(result.stdout.strip())
        return moves
    except Exception as e:
        print(f"Error in solve_level: {e}")
        return -1

def save_map(map_str, difficulty, number):
    """Save the map to a file"""
    directory = f"app/src/main/assets/Maps/{difficulty}"
    os.makedirs(directory, exist_ok=True)
    filename = f"{directory}/generatedMap_{number}.txt"
    print(f"Saving {filename}")
    with open(filename, "w") as f:
        f.write(map_str)
    return filename

def get_difficulty(moves):
    """Determine difficulty based on number of moves"""
    if moves <= 6:  # Einfachere Beginner-Level
        return "beginner"
    elif moves <= 10:  # Angepasste Schwierigkeitsgrade
        return "intermediate"
    elif moves <= 14:
        return "advanced"
    else:
        return "expert"

def main():
    # Zähler für generierte Level pro Schwierigkeit
    counts = {
        "beginner": 0,
        "intermediate": 0,
        "advanced": 0,
        "expert": 0
    }
    
    # Ziel: 35 Level pro Schwierigkeit
    target_count = 35
    
    # Temporäres Verzeichnis für neue Level
    temp_dir = "app/src/main/assets/Maps/temp"
    os.makedirs(temp_dir, exist_ok=True)
    
    # Starte Background-Monitor
    subprocess.Popen(
        ['bash', '-c', '''
        while true; do 
            clear
            echo "=== $(date) ==="
            echo "Generiere Level..."
            echo "----------------"
            for d in beginner intermediate advanced expert; do
                count=$(ls app/src/main/assets/Maps/$d/generatedMap_* 2>/dev/null | wc -l)
                printf "%-12s: %2d/35 (%3d%%)\n" "$d" "$count" $((count * 100 / 35))
            done
            echo "----------------"
            sleep 2
        done
        '''],
        cwd=os.getcwd()
    )
    
    while min(counts.values()) < target_count:
        # Generiere ein Level
        map_str = generate_map()
        
        # Teste wie viele Züge nötig sind
        moves = solve_level(map_str)
        
        if moves == -1:
            print("Level konnte nicht gelöst werden, überspringe...")
            continue
            
        # Bestimme Schwierigkeit
        difficulty = get_difficulty(moves)
        
        # Wenn wir noch Level dieser Schwierigkeit brauchen, speichere es
        if counts[difficulty] < target_count:
            save_map(map_str, difficulty, counts[difficulty])
            counts[difficulty] += 1
        else:
            print(f"Überspringe {difficulty} Level, haben schon genug")
    
    print("\nFertig! Generierte Level:")
    for diff, count in counts.items():
        print(f"{diff}: {count}")

if __name__ == "__main__":
    main()

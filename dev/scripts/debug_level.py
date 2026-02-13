#!/usr/bin/env python3
"""
Level Analysis & Debug Tool for Roboyard

Analyzes level files for:
- Unreachable areas (cells no robot can reach)
- Trapped robots (robots stuck in small isolated regions)
- Target-robot overlaps (target on same position as robot)
- Center carré detection (normal 2x2 center square is ignored)

Usage:
  Single level debug:
    python3 dev/scripts/debug_level.py <level_file>
    python3 dev/scripts/debug_level.py app/src/main/assets/Maps/level_9.txt

  Full report (all levels):
    python3 dev/scripts/debug_level.py --report
    python3 dev/scripts/debug_level.py --report /path/to/Maps/

  Output report to file:
    python3 dev/scripts/debug_level.py --report > dev/level_analysis_report.md
"""

import sys
import re
from collections import deque

def parse_level(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    board_size = None
    hedges_h = set()
    hedges_v = set()
    robots = []
    target = None
    
    for line in lines:
        line = line.strip().rstrip(';')
        if not line:
            continue
        
        if line.startswith('board:'):
            parts = line.split(':')[1].split(',')
            board_size = (int(parts[0]), int(parts[1]))
        elif line.startswith('mh'):
            parts = line[2:].split(',')
            hedges_h.add((int(parts[0]), int(parts[1])))
        elif line.startswith('mv'):
            parts = line[2:].split(',')
            hedges_v.add((int(parts[0]), int(parts[1])))
        elif line.startswith('robot_'):
            match = re.match(r'robot_([a-z]+)(\d+),(\d+)', line)
            if match:
                x = int(match.group(2))
                y = int(match.group(3))
                robots.append((x, y))
        elif line.startswith('target_'):
            match = re.match(r'target_([a-z]+)(\d+),(\d+)', line)
            if match:
                x = int(match.group(2))
                y = int(match.group(3))
                target = (x, y)
    
    return board_size, hedges_h, hedges_v, robots, target

def can_move(x, y, dx, dy, board_size, hedges_h, hedges_v):
    """Check if movement is blocked by hedges
    
    Wall semantics (from rendering code):
    - mh at (x,y) = horizontal wall at TOP edge of cell (x,y)
      → blocks movement between (x, y-1) and (x, y)
    - mv at (x,y) = vertical wall at LEFT edge of cell (x,y)
      → blocks movement between (x-1, y) and (x, y)
    
    Boundary walls (x=0, y=0, x=width, y=height) form the board border.
    """
    width, height = board_size
    nx, ny = x + dx, y + dy
    
    # Check board boundaries
    if nx < 0 or nx >= width or ny < 0 or ny >= height:
        return False
    
    if dy == -1:  # moving up: (x,y) -> (x,y-1)
        # Blocked by mh at (x, y) — wall at top edge of current cell
        if (x, y) in hedges_h:
            return False
    elif dy == 1:  # moving down: (x,y) -> (x,y+1)
        # Blocked by mh at (x, y+1) — wall at top edge of destination cell
        if (x, ny) in hedges_h:
            return False
    elif dx == -1:  # moving left: (x,y) -> (x-1,y)
        # Blocked by mv at (x, y) — wall at left edge of current cell
        if (x, y) in hedges_v:
            return False
    elif dx == 1:  # moving right: (x,y) -> (x+1,y)
        # Blocked by mv at (x+1, y) — wall at left edge of destination cell
        if (nx, y) in hedges_v:
            return False
    
    return True

def find_reachable_from(start, board_size, hedges_h, hedges_v):
    """Find all cells reachable from a single starting position"""
    reachable = set()
    queue = deque()
    queue.append(start)
    reachable.add(start)
    
    while queue:
        x, y = queue.popleft()
        for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
            if can_move(x, y, dx, dy, board_size, hedges_h, hedges_v):
                nx, ny = x + dx, y + dy
                if (nx, ny) not in reachable:
                    reachable.add((nx, ny))
                    queue.append((nx, ny))
    return reachable

def find_reachable(board_size, hedges_h, hedges_v, robots):
    """Find all cells reachable from any robot position"""
    reachable = set()
    for robot in robots:
        reachable |= find_reachable_from(robot, board_size, hedges_h, hedges_v)
    return reachable

def is_carre_cell(x, y, board_width, board_height):
    """Check if cell is part of the center carré (2x2 square in the middle)"""
    center_x = board_width // 2 - 1
    center_y = board_height // 2 - 1
    return (x in [center_x, center_x + 1] and 
            y in [center_y, center_y + 1])

def find_unreachable_regions(board_size, unreachable_cells):
    """Find connected components among unreachable cells"""
    if not unreachable_cells:
        return []
    
    remaining = set(unreachable_cells)
    regions = []
    
    while remaining:
        # BFS from an arbitrary unreachable cell
        start = next(iter(remaining))
        region = set()
        queue = deque([start])
        region.add(start)
        
        while queue:
            x, y = queue.popleft()
            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = x + dx, y + dy
                if (nx, ny) in remaining and (nx, ny) not in region:
                    region.add((nx, ny))
                    queue.append((nx, ny))
        
        regions.append(sorted(region))
        remaining -= region
    
    return regions

def get_carre_cells(board_size):
    """Get the set of cells that form the center carré"""
    width, height = board_size
    center_x = width // 2 - 1
    center_y = height // 2 - 1
    return {
        (center_x, center_y),
        (center_x + 1, center_y),
        (center_x, center_y + 1),
        (center_x + 1, center_y + 1)
    }

def analyze_all_levels(maps_dir):
    """Analyze all levels and return structured results"""
    import os, glob
    
    results = []
    level_files = sorted(glob.glob(os.path.join(maps_dir, "level_*.txt")),
                         key=lambda f: int(os.path.basename(f).replace("level_", "").replace(".txt", "")))
    
    for level_file in level_files:
        level_num = int(os.path.basename(level_file).replace("level_", "").replace(".txt", ""))
        board_size, hedges_h, hedges_v, robots, target = parse_level(level_file)
        
        if not board_size:
            continue
        
        width, height = board_size
        all_cells = set((x, y) for x in range(width) for y in range(height))
        
        # Check if any robot is trapped (can only reach a small part of the board)
        trapped_robots = []
        robot_reach = {}
        robot_reach_sets = {}
        max_reach = 0
        for robot in robots:
            reach = find_reachable_from(robot, board_size, hedges_h, hedges_v)
            robot_reach[robot] = len(reach)
            robot_reach_sets[robot] = reach
            if len(reach) > max_reach:
                max_reach = len(reach)
        
        free_robots = []
        for robot in robots:
            if robot_reach[robot] < max_reach * 0.5:
                trapped_robots.append((robot, robot_reach[robot]))
            else:
                free_robots.append(robot)
        
        # Calculate reachable from free robots only (trapped robot regions count as unreachable)
        reachable = find_reachable(board_size, hedges_h, hedges_v, free_robots if free_robots else robots)
        unreachable = all_cells - reachable
        
        regions = find_unreachable_regions(board_size, unreachable)
        
        # Check if target is reachable from free robots
        target_reachable = target in reachable if target else None
        
        results.append({
            'level': level_num,
            'board': f"{width}x{height}",
            'total_cells': width * height,
            'unreachable_count': len(unreachable),
            'regions': regions,
            'region_count': len(regions),
            'target_reachable': target_reachable,
            'trapped_robots': trapped_robots,
            'robots': robots,
            'target': target,
        })
    
    return results

def check_target_duplicates(maps_dir, start_level=90):
    """Check if any target overlaps with a robot position"""
    import os, glob
    
    issues = []
    level_files = sorted(glob.glob(os.path.join(maps_dir, "level_*.txt")),
                         key=lambda f: int(os.path.basename(f).replace("level_", "").replace(".txt", "")))
    
    for level_file in level_files:
        level_num = int(os.path.basename(level_file).replace("level_", "").replace(".txt", ""))
        if level_num < start_level:
            continue
        
        board_size, hedges_h, hedges_v, robots, target = parse_level(level_file)
        if target and target in robots:
            issues.append((level_num, target))
    
    return issues

if __name__ == '__main__':
    import os
    
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    
    if sys.argv[1] == '--report':
        maps_dir = sys.argv[2] if len(sys.argv) > 2 else "/var/www/Roboyard/app/src/main/assets/Maps"
        
        results = analyze_all_levels(maps_dir)
        target_issues = check_target_duplicates(maps_dir, 90)
        
        # Generate markdown report
        from datetime import datetime
        now = datetime.now().strftime("%d.%m.%Y %H:%M")
        
        print(f"# Level Analysis Report")
        print(f"")
        print(f"Generated: {now}")
        print(f"")
        print(f"## 1. Target Duplicate Analysis (Levels 90+)")
        print(f"")
        if target_issues:
            print(f"**{len(target_issues)} issues found:**")
            for level_num, pos in target_issues:
                print(f"- Level {level_num}: Target at ({pos[0]},{pos[1]}) overlaps with robot")
        else:
            print(f"✓ No target-robot overlaps found in levels 90+")
        
        print(f"")
        print(f"---")
        print(f"")
        print(f"## 2. Unreachable Areas Analysis (All Levels)")
        print(f"")
        
        # Filter out carré cells from each region
        levels_with_issues = []
        for r in results:
            board_dims = (int(r['board'].split('x')[0]), int(r['board'].split('x')[1]))
            carre = get_carre_cells(board_dims)
            
            # Remove carré cells from each region, keep non-empty regions
            filtered_regions = []
            for reg in r['regions']:
                non_carre = [cell for cell in reg if tuple(cell) not in carre]
                if non_carre:
                    filtered_regions.append(non_carre)
            
            if filtered_regions:
                non_carre_cells = sum(len(reg) for reg in filtered_regions)
                r['filtered_unreachable'] = non_carre_cells
                r['filtered_regions'] = filtered_regions
                r['filtered_region_count'] = len(filtered_regions)
                levels_with_issues.append(r)
            else:
                r['filtered_unreachable'] = 0
                r['filtered_regions'] = []
                r['filtered_region_count'] = 0
        
        levels_with_trapped = [r for r in results if r['trapped_robots']]
        levels_target_unreachable = [r for r in results if r['target_reachable'] == False]
        
        print(f"### Summary")
        print(f"- **Total levels analyzed:** {len(results)}")
        print(f"- **Levels with anomalies** (excluding normal carré): {len(levels_with_issues)}")
        print(f"- **Levels with trapped robots:** {len(levels_with_trapped)}")
        print(f"- **Levels with unreachable target:** {len(levels_target_unreachable)}")
        print(f"")
        print(f"*Note: The 2×2 center carré (4 cells) is normal and ignored in this analysis.*")
        print(f"")
        
        # Critical issues first
        if levels_with_trapped:
            print(f"### ⚠ Critical: Trapped Robots")
            print(f"")
            for r in levels_with_trapped:
                for pos, reach in r['trapped_robots']:
                    print(f"- **Level {r['level']}** ({r['board']}): Robot at ({pos[0]},{pos[1]}) can only reach {reach} cells")
            print(f"")
        
        if levels_target_unreachable:
            print(f"### ⚠ Critical: Unreachable Targets")
            print(f"")
            for r in levels_target_unreachable:
                print(f"- **Level {r['level']}** ({r['board']}): Target at {r['target']} is unreachable")
            print(f"")
        
        # Detailed table (only levels with issues)
        if levels_with_issues:
            print(f"### Levels with Anomalies (excluding normal carré)")
            print(f"")
            print(f"| Level | Board | Unreachable | Regions | Cells per Region |")
            print(f"|------:|-------|------------:|--------:|:-----------------|")
            
            for r in levels_with_issues:
                region_sizes = ", ".join(str(len(reg)) for reg in r['filtered_regions'])
                print(f"| {r['level']:>5} | {r['board']:<5} | {r['filtered_unreachable']:>11} | {r['filtered_region_count']:>7} | {region_sizes:<17} |")
        else:
            print(f"### ✓ All Levels Normal")
            print(f"")
            print(f"All levels only have the normal center carré as unreachable area.")
        
        print(f"")
        print(f"---")
        print(f"Report generated: {now}")
    
    else:
        # Single file debug mode
        filename = sys.argv[1]
        board_size, hedges_h, hedges_v, robots, target = parse_level(filename)
        
        width, height = board_size
        print(f"Board size: {width}×{height} = {width*height} cells")
        print(f"Robots: {robots}")
        print(f"Target: {target}")
        print(f"Horizontal hedges: {len(hedges_h)}")
        print(f"Vertical hedges: {len(hedges_v)}")
        print()
        
        # Per-robot reachability
        max_reach = 0
        robot_reaches = {}
        for robot in robots:
            reach = find_reachable_from(robot, board_size, hedges_h, hedges_v)
            robot_reaches[robot] = reach
            if len(reach) > max_reach:
                max_reach = len(reach)
            print(f"Robot at {robot}: can reach {len(reach)} cells")
        
        print()
        
        # Identify trapped robots
        free_robots = []
        trapped = []
        for robot in robots:
            if len(robot_reaches[robot]) < max_reach * 0.5:
                trapped.append(robot)
                print(f"⚠ TRAPPED: Robot at {robot} (only {len(robot_reaches[robot])} cells)")
            else:
                free_robots.append(robot)
        
        if not trapped:
            print("No trapped robots")
        
        print()
        
        # Unreachable from free robots
        reachable = find_reachable(board_size, hedges_h, hedges_v, free_robots if free_robots else robots)
        all_cells = set((x, y) for x in range(width) for y in range(height))
        unreachable = all_cells - reachable
        
        regions = find_unreachable_regions(board_size, unreachable)
        
        print(f"Reachable from free robots: {len(reachable)}")
        print(f"Unreachable cells: {len(unreachable)}")
        print(f"Unreachable regions: {len(regions)}")
        for i, region in enumerate(regions):
            print(f"  Region {i+1} ({len(region)} cells): {region}")
        print()

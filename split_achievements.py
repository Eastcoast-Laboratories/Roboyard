#!/usr/bin/env python3
from PIL import Image
import os

# Icon names in order (8x8 grid = 64 icons)
# Row 1
icon_names = [
    "1_lightning",      # Lightning/electricity
    "2_heart",          # Heart with gear
    "3_robot",          # Robot head
    "4_power",          # Power button
    "5_monitor",        # Monitor/screen
    "6_key",            # Key
    "7_flag",           # Flag/trophy
    "8_target",         # Target/crosshair
    
    # Row 2
    "9_trophy",         # Trophy/cup
    "10_gear",          # Gear/cog
    "11_chart",         # Bar chart
    "12_bars",          # Stacked bars
    "13_hourglass",     # Hourglass/timer
    "14_checkmark",     # Checkmark
    "15_lightbulb",     # Lightbulb
    "16_compass",       # Compass/gauge
    
    # Row 3
    "17_trophy",        # Trophy (red)
    "18_wrench",        # Wrench/tools
    "19_circle",        # Circle/target
    "20_circuit",       # Circuit board
    "21_brain",         # Brain
    "22_dice",          # Dice
    "23_gamepad",       # Game controller
    "24_robot_gold",    # Gold robot
    
    # Row 4
    "25_shield",        # Shield
    "26_shield_red",    # Red shield
    "27_star",          # Star
    "28_anchor",        # Anchor
    "29_brain_green",   # Green brain
    "30_storage",       # Storage/cabinet
    "31_storage_green", # Green storage
    "32_infinity",      # Infinity symbol
    
    # Row 5
    "33_target_blue",   # Blue target
    "34_magnifier",     # Magnifying glass
    "35_door",          # Door
    "36_ring",          # Ring/circle
    "37_blocks",        # Blocks/tetris
    "38_diamond",       # Diamond pattern
    "39_stars",         # Stars (purple)
    "40_planet",        # Planet
    
    # Row 6
    "41_crown",         # Crown
    "42_laurel",        # Laurel wreath
    "43_trophy_gold",   # Gold trophy
    "44_trophy_gold2",  # Gold trophy 2
    "45_diamond_blue",  # Blue diamond
    "46_flame",         # Flame/fire
    "47_water",         # Water/wave
    "48_stars_gold",    # Gold stars
    
    # Row 7
    "49_armor",         # Armor/robot
    "50_folders",       # Folders
    "51_spiral",        # Spiral
    "52_network",       # Network/nodes
    "53_ring_blue",     # Blue ring
    "54_cube",          # 3D cube
    "55_cone",          # Cone
    "56_circle_gray",   # Gray circle
    
    # Row 8 (duplicates from row 6-7, likely for reference)
    "57_crown",         # Crown (duplicate)
    "58_laurel",        # Laurel (duplicate)
    "59_trophy_gold",   # Trophy (duplicate)
    "60_trophy_gold2",  # Trophy (duplicate)
    "61_diamond_blue",  # Diamond (duplicate)
    "62_flame",         # Flame (duplicate)
    "63_water",         # Water (duplicate)
    "64_stars_gold",    # Stars (duplicate)
]

# Open the image
img_path = "/var/www/Roboyard/app/src/main/res/drawable/achievements_icons_64.png"
img = Image.open(img_path)

# Get image dimensions
width, height = img.size
print(f"Image size: {width}x{height}")

# Calculate icon size (should be 64x64 for each icon in an 8x8 grid)
icon_width = width // 8
icon_height = height // 8
print(f"Icon size: {icon_width}x{icon_height}")

# Create output directory
output_dir = "/var/www/Roboyard/app/src/main/res/drawable/achievements_icons"
os.makedirs(output_dir, exist_ok=True)

# Split the image and save each icon
for idx, name in enumerate(icon_names):
    row = idx // 8
    col = idx % 8
    
    # Calculate crop box
    left = col * icon_width
    top = row * icon_height
    right = left + icon_width
    bottom = top + icon_height
    
    # Crop the icon
    icon = img.crop((left, top, right, bottom))
    
    # Save the icon
    output_path = os.path.join(output_dir, f"{name}.png")
    icon.save(output_path)
    print(f"Saved: {name}.png")

print(f"\nAll {len(icon_names)} icons extracted successfully!")

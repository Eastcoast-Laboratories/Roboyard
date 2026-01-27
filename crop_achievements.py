#!/usr/bin/env python3
from PIL import Image
import os

input_dir = "/var/www/Roboyard/app/src/main/res/drawable/achievements_icons"
output_dir = "/var/www/Roboyard/app/src/main/res/drawable/achievements_icons_cropped"

os.makedirs(output_dir, exist_ok=True)

# Get all PNG files
icon_files = sorted([f for f in os.listdir(input_dir) if f.endswith('.png')])

for icon_file in icon_files:
    input_path = os.path.join(input_dir, icon_file)
    output_path = os.path.join(output_dir, icon_file)
    
    # Open image
    img = Image.open(input_path)
    
    # Convert to RGBA if not already
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    
    # Get bounding box of non-transparent content
    bbox = img.getextrema()
    
    # Get alpha channel
    alpha = img.split()[-1]
    bbox = alpha.getbbox()
    
    if bbox:
        # Crop to the bounding box
        cropped = img.crop(bbox)
        cropped.save(output_path)
        original_size = img.size
        cropped_size = cropped.size
        print(f"✓ {icon_file}: {original_size} → {cropped_size}")
    else:
        # Image is completely transparent
        print(f"⚠ {icon_file}: Completely transparent, skipping")

print(f"\nAll icons cropped successfully!")

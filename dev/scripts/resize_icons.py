#!/usr/bin/env python3
"""
Resize achievement icons to 512x512 pixels for Google Play Console.

Google Play Console requires all achievement icons to be exactly 512x512 pixels.
This script resizes all PNG icons in the achievements_icons directory.

Usage:
    python3 resize_icons.py
"""

from PIL import Image
from pathlib import Path

ICONS_DIR = Path(__file__).parent.parent.parent / "app/src/main/res/drawable/achievements_icons"
TARGET_SIZE = (512, 512)

def resize_icons():
    """Resize all PNG icons to 512x512 pixels."""
    if not ICONS_DIR.exists():
        print(f"Error: Icons directory not found: {ICONS_DIR}")
        return False
    
    png_files = sorted(ICONS_DIR.glob("*.png"))
    if not png_files:
        print(f"Error: No PNG files found in {ICONS_DIR}")
        return False
    
    print(f"Resizing {len(png_files)} icons to {TARGET_SIZE[0]}x{TARGET_SIZE[1]}...\n")
    
    resized_count = 0
    for png_file in png_files:
        try:
            img = Image.open(png_file)
            original_size = img.size
            
            # Resize using high-quality resampling
            img_resized = img.resize(TARGET_SIZE, Image.Resampling.LANCZOS)
            img_resized.save(png_file, 'PNG', quality=95)
            
            resized_count += 1
            print(f"✓ {png_file.name}: {original_size} → {TARGET_SIZE}")
        except Exception as e:
            print(f"✗ {png_file.name}: Error - {e}")
    
    print(f"\nResized {resized_count}/{len(png_files)} icons successfully")
    return resized_count == len(png_files)

if __name__ == "__main__":
    success = resize_icons()
    exit(0 if success else 1)

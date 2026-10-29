from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "public" / "assets" / "wisps-sprite-sheet.png"
COLORS = ("pink", "green", "blue", "yellow", "silver")

sheet = Image.open(SOURCE).convert("RGBA")
cell_width = sheet.width // 8
cell_height = sheet.height // 5

for row, color in enumerate(COLORS):
    top = row * cell_height
    bottom = sheet.height if row == len(COLORS) - 1 else (row + 1) * cell_height
    for animation, first_column in (("idle", 0), ("move", 4)):
        strip = sheet.crop((first_column * cell_width, top, (first_column + 4) * cell_width, bottom))
        strip.save(ROOT / "public" / "assets" / f"wisp_{color}_{animation}.png", optimize=True)

from math import pi, sin
from pathlib import Path

from PIL import Image, ImageEnhance


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "public" / "crypt-crawler-splash.png"
OUTPUT = ROOT / "public" / "crypt-crawler-splash.gif"
SIZE = (640, 360)
FRAME_COUNT = 16

source = Image.open(SOURCE).convert("RGB")
base_scale = max(SIZE[0] / source.width, SIZE[1] / source.height)
palette_source = source.resize(SIZE, Image.Resampling.LANCZOS).quantize(colors=80)
frames: list[Image.Image] = []

for index in range(FRAME_COUNT):
    phase = 2 * pi * index / FRAME_COUNT
    zoom = 1.015 + 0.015 * sin(phase)
    width = round(source.width * base_scale * zoom)
    height = round(source.height * base_scale * zoom)
    frame = source.resize((width, height), Image.Resampling.LANCZOS)
    left = max(0, (width - SIZE[0]) // 2 + round(4 * sin(phase)))
    top = max(0, (height - SIZE[1]) // 2 + round(2 * sin(phase + pi / 2)))
    frame = frame.crop((left, top, left + SIZE[0], top + SIZE[1]))
    frame = ImageEnhance.Brightness(frame).enhance(1 + 0.025 * sin(phase))
    frames.append(frame.quantize(palette=palette_source, dither=Image.Dither.FLOYDSTEINBERG))

frames[0].save(
    OUTPUT,
    save_all=True,
    append_images=frames[1:],
    duration=150,
    loop=0,
    optimize=True,
    disposal=2,
)

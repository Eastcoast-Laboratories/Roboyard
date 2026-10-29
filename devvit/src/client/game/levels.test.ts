import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { parseLevel } from './engine';

const levelsDirectory = join(process.cwd(), 'public', 'levels');

describe('bundled Arcane Wisps levels', () => {
  it('parses all 140 levels with valid board positions', () => {
    const files = readdirSync(levelsDirectory).filter((name) => /^level_\d+\.txt$/.test(name));
    expect(files).toHaveLength(140);

    for (const file of files) {
      const board = parseLevel(readFileSync(join(levelsDirectory, file), 'utf8'));
      expect(board.robots).toHaveLength(4);
      expect(board.goals.length).toBeGreaterThan(0);
      for (const piece of [...board.robots, ...board.goals]) {
        expect(piece.x, `${file}: x`).toBeGreaterThanOrEqual(0);
        expect(piece.x, `${file}: x`).toBeLessThan(board.width);
        expect(piece.y, `${file}: y`).toBeGreaterThanOrEqual(0);
        expect(piece.y, `${file}: y`).toBeLessThan(board.height);
      }
    }
  });
});

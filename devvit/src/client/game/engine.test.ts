import { describe, expect, it } from 'vitest';
import { isSolved, moveRobot, parseLevel } from './engine';

const level = `
  board:5,5;
  h0,0; h1,0; h2,0; h3,0; h4,0;
  h0,5; h1,5; h2,5; h3,5; h4,5;
  v0,0; v0,1; v0,2; v0,3; v0,4;
  v5,0; v5,1; v5,2; v5,3; v5,4;
  v3,0;
  tg2,0;
  rg1,4; rr4,4;
`;

describe('Roboyard simulation', () => {
  it('parses the Kotlin level format', () => {
    const board = parseLevel(level);
    expect(board.width).toBe(5);
    expect(board.robots[0]).toMatchObject({ color: 'pink', x: 4, y: 4 });
    expect(board.robots[1]).toMatchObject({ color: 'green', x: 1, y: 4 });
    expect(board.goals[0]).toMatchObject({ color: 'green', x: 2, y: 0 });
  });

  it('slides until a wall blocks the route', () => {
    const moved = moveRobot(parseLevel(level), 'green', 'up');
    expect(moved?.robots.find((robot) => robot.color === 'green')).toMatchObject({ x: 1, y: 0 });
  });

  it('stops before another character', () => {
    const board = parseLevel(level);
    board.robots = [
      { color: 'pink', x: 1, y: 2 },
      { color: 'green', x: 3, y: 2 },
    ];
    const moved = moveRobot(board, 'pink', 'right');
    expect(moved?.robots[0]).toMatchObject({ x: 2, y: 2 });
  });

  it('recognizes a matching destination', () => {
    const board = parseLevel(level);
    board.robots.find((robot) => robot.color === 'green')!.x = 2;
    board.robots.find((robot) => robot.color === 'green')!.y = 0;
    expect(isSolved(board)).toBe(true);
  });
});

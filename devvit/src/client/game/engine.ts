import { COLORS, type Board, type Direction, type GameSnapshot, type Goal, type Robot, type RobotColor } from './types';

const colorCodes: Record<string, RobotColor | 'multi'> = {
  r: 'pink',
  g: 'green',
  b: 'blue',
  y: 'yellow',
  s: 'silver',
  m: 'multi',
};

const key = (x: number, y: number): string => `${x},${y}`;

export function parseLevel(source: string): Board {
  let width = 14;
  let height = 14;
  const horizontalWalls = new Set<string>();
  const verticalWalls = new Set<string>();
  const robots: Robot[] = [];
  const goals: Goal[] = [];

  const entries = source
    .split(/;|\n/)
    .map((entry) => entry.replace(/#.*/, '').trim())
    .filter(Boolean);

  for (const entry of entries) {
    const boardMatch = entry.match(/^board:(\d+)\s*,\s*(\d+)$/);
    if (boardMatch) {
      width = Number(boardMatch[1]);
      height = Number(boardMatch[2]);
      continue;
    }

    const match = entry.match(/^([a-z]+):?(\d+)\s*,\s*(\d+)$/i);
    if (!match) continue;
    const [, token, xText, yText] = match;
    const x = Number(xText);
    const y = Number(yText);

    if (token === 'h' || token === 'mh') horizontalWalls.add(key(x, y));
    else if (token === 'v' || token === 'mv') verticalWalls.add(key(x, y));
    else if (token.startsWith('r')) {
      const color = colorCodes[token.at(-1) ?? ''];
      if (color && color !== 'multi') robots.push({ color, x, y });
    } else if (token.startsWith('t')) {
      const color = colorCodes[token.at(-1) ?? ''];
      if (color) goals.push({ color, x, y });
    }
  }

  robots.sort((a, b) => COLORS.indexOf(a.color) - COLORS.indexOf(b.color));
  if (!robots.length || !goals.length) throw new Error('Level must contain robots and at least one destination.');
  return { width, height, horizontalWalls, verticalWalls, robots, goals };
}

function wallBlocks(board: Board, x: number, y: number, direction: Direction): boolean {
  if (direction === 'up') return y === 0 || board.horizontalWalls.has(key(x, y));
  if (direction === 'down') return y === board.height - 1 || board.horizontalWalls.has(key(x, y + 1));
  if (direction === 'left') return x === 0 || board.verticalWalls.has(key(x, y));
  return x === board.width - 1 || board.verticalWalls.has(key(x + 1, y));
}

export function moveRobot(board: Board, color: RobotColor, direction: Direction): Board | null {
  const index = board.robots.findIndex((robot) => robot.color === color);
  if (index < 0) return null;
  const delta = direction === 'up' ? [0, -1] : direction === 'right' ? [1, 0] : direction === 'down' ? [0, 1] : [-1, 0];
  const robots = board.robots.map((robot) => ({ ...robot }));
  const robot = robots[index];
  const start = key(robot.x, robot.y);

  while (!wallBlocks(board, robot.x, robot.y, direction)) {
    const nextX = robot.x + delta[0];
    const nextY = robot.y + delta[1];
    if (robots.some((other, otherIndex) => otherIndex !== index && other.x === nextX && other.y === nextY)) break;
    robot.x = nextX;
    robot.y = nextY;
  }

  return key(robot.x, robot.y) === start ? null : { ...board, robots };
}

export function isSolved(board: Board): boolean {
  const matched = board.robots.filter((robot) =>
    board.goals.some((goal) => goal.x === robot.x && goal.y === robot.y && (goal.color === robot.color || goal.color === 'multi'))
  ).length;
  return matched >= Math.min(board.robots.length, board.goals.length);
}

export function snapshot(board: Board, selected: RobotColor): GameSnapshot {
  return { robots: board.robots.map((robot) => ({ ...robot })), selected };
}

export function restore(board: Board, state: GameSnapshot): Board {
  return { ...board, robots: state.robots.map((robot) => ({ ...robot })) };
}

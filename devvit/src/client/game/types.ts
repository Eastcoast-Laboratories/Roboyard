export const COLORS = ['pink', 'green', 'blue', 'yellow', 'silver'] as const;
export type RobotColor = (typeof COLORS)[number];
export type Direction = 'up' | 'right' | 'down' | 'left';

export type Position = { x: number; y: number };
export type Robot = Position & { color: RobotColor };
export type Goal = Position & { color: RobotColor | 'multi' };

export type Board = {
  width: number;
  height: number;
  horizontalWalls: Set<string>;
  verticalWalls: Set<string>;
  robots: Robot[];
  goals: Goal[];
};

export type GameSnapshot = {
  robots: Robot[];
  selected: RobotColor;
};

import { context } from '@devvit/web/client';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { isSolved, moveRobot, parseLevel, restore, snapshot } from './game/engine';
import { COLORS, type Board, type Direction, type GameSnapshot, type RobotColor } from './game/types';

const ASSET = './assets/';
const directionGlyph: Record<Direction, string> = { up: '▲', right: '▶', down: '▼', left: '◀' };

function asset(name: string): string {
  return `${ASSET}${name}`;
}

export function App() {
  const [level, setLevel] = useState(() => Math.min(140, Math.max(1, Number(localStorage.getItem('crypt-crawler-level') ?? localStorage.getItem('roboyard-level')) || 1)));
  const [board, setBoard] = useState<Board | null>(null);
  const [initial, setInitial] = useState<Board | null>(null);
  const [selected, setSelected] = useState<RobotColor>('pink');
  const [history, setHistory] = useState<GameSnapshot[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setError('');
    fetch(`./levels/level_${level}.txt`)
      .then((response) => {
        if (!response.ok) throw new Error(`Level ${level} is unavailable.`);
        return response.text();
      })
      .then((source) => {
        if (!active) return;
        const next = parseLevel(source);
        setBoard(next);
        setInitial(next);
        setSelected(next.robots[0]?.color ?? 'pink');
        setHistory([]);
        localStorage.setItem('crypt-crawler-level', String(level));
      })
      .catch((reason: unknown) => active && setError(reason instanceof Error ? reason.message : 'Could not load level.'));
    return () => {
      active = false;
    };
  }, [level]);

  const solved = useMemo(() => (board ? isSolved(board) : false), [board]);
  const move = useCallback((direction: Direction) => {
    setBoard((current) => {
      if (!current || solved) return current;
      const next = moveRobot(current, selected, direction);
      if (!next) return current;
      setHistory((items) => [...items, snapshot(current, selected)]);
      return next;
    });
  }, [selected, solved]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const direction: Direction | undefined =
        event.key === 'ArrowUp' || event.key.toLowerCase() === 'w' ? 'up' :
        event.key === 'ArrowRight' || event.key.toLowerCase() === 'd' ? 'right' :
        event.key === 'ArrowDown' || event.key.toLowerCase() === 's' ? 'down' :
        event.key === 'ArrowLeft' || event.key.toLowerCase() === 'a' ? 'left' : undefined;
      if (direction) {
        event.preventDefault();
        move(direction);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [move]);

  const undo = () => {
    if (!board || !history.length) return;
    const previous = history.at(-1)!;
    setBoard(restore(board, previous));
    setSelected(previous.selected);
    setHistory((items) => items.slice(0, -1));
  };

  const reset = () => {
    if (!initial) return;
    setBoard({ ...initial, robots: initial.robots.map((robot) => ({ ...robot })) });
    setHistory([]);
  };

  if (error) return <main className="shell"><section className="message"><h1>Crypt sealed</h1><p>{error}</p></section></main>;
  if (!board) return <main className="shell"><section className="message"><h1>Opening the crypt…</h1></section></main>;

  return (
    <main className="shell">
      <header className="hero">
        <div>
          <span className="eyebrow">Crypt Crawler</span>
          <h1>Slide. Ricochet. Awaken.</h1>
          <p>Guide each skeleton to its matching summoning circle.</p>
        </div>
        <div className="community">{context?.subredditName ? `r/${context.subredditName}` : 'Devvit edition'}</div>
      </header>

      <section className="game-card">
        <div className="toolbar">
          <button aria-label="Previous level" onClick={() => setLevel((value) => Math.max(1, value - 1))} disabled={level === 1}>‹</button>
          <div><strong>Level {level}</strong><span>{history.length} moves</span></div>
          <button aria-label="Next level" onClick={() => setLevel((value) => Math.min(140, value + 1))} disabled={level === 140}>›</button>
        </div>

        <div className="play-area">
          <BoardView board={board} selected={selected} onSelect={setSelected} />
          <aside className="controls">
            <div className="roster" aria-label="Choose a skeleton">
              {COLORS.filter((color) => board.robots.some((robot) => robot.color === color)).map((color) => (
                <button key={color} className={selected === color ? `roster-button selected ${color}` : `roster-button ${color}`} onClick={() => setSelected(color)} aria-pressed={selected === color}>
                  <img src={asset(`robot_${color}_right.png`)} alt="" /><span>{color}</span>
                </button>
              ))}
            </div>
            <div className="dpad" aria-label="Movement controls">
              {(['up', 'left', 'down', 'right'] as Direction[]).map((direction) => (
                <button key={direction} className={direction} onClick={() => move(direction)} aria-label={`Move ${selected} ${direction}`}>{directionGlyph[direction]}</button>
              ))}
            </div>
            <div className="actions">
              <button onClick={undo} disabled={!history.length}>Undo</button>
              <button onClick={reset}>Reset</button>
            </div>
            <p className="hint">Select a skeleton, then use the arrows, WASD, or direction pad.</p>
          </aside>
        </div>
      </section>

      {solved && (
        <div className="victory" role="dialog" aria-modal="true" aria-label="Level complete">
          <div className="victory-card">
            <img src={asset('center-emblem.png')} alt="" />
            <span className="eyebrow">Ritual complete</span>
            <h2>Level {level} awakened</h2>
            <p>Completed in {history.length} moves.</p>
            <button onClick={() => setLevel((value) => Math.min(140, value + 1))}>{level === 140 ? 'Play again' : 'Next level'}</button>
          </div>
        </div>
      )}
    </main>
  );
}

function BoardView({ board, selected, onSelect }: { board: Board; selected: RobotColor; onSelect: (color: RobotColor) => void }) {
  const cells = Array.from({ length: board.width * board.height }, (_, index) => ({ x: index % board.width, y: Math.floor(index / board.width) }));
  return (
    <div className="board-frame">
      <div className="board" style={{ gridTemplateColumns: `repeat(${board.width}, 1fr)`, gridTemplateRows: `repeat(${board.height}, 1fr)` }}>
        {cells.map(({ x, y }) => {
          const goal = board.goals.find((item) => item.x === x && item.y === y);
          const robot = board.robots.find((item) => item.x === x && item.y === y);
          const wallTop = board.horizontalWalls.has(`${x},${y}`);
          const wallBottom = board.horizontalWalls.has(`${x},${y + 1}`);
          const wallLeft = board.verticalWalls.has(`${x},${y}`);
          const wallRight = board.verticalWalls.has(`${x + 1},${y}`);
          return (
            <button key={`${x},${y}`} className="cell" onClick={() => robot && onSelect(robot.color)} aria-label={robot ? `${robot.color} skeleton at column ${x + 1}, row ${y + 1}` : `Empty cell ${x + 1}, ${y + 1}`}>
              {goal && <img className="goal" src={asset(`target_${goal.color}.png`)} alt="" />}
              {robot && <img className={selected === robot.color ? 'robot selected-robot' : 'robot'} src={asset(`robot_${robot.color}_right.png`)} alt="" />}
              {wallTop && <img className="wall wall-horizontal wall-top" src={asset('wall-horizontal.png')} alt="" />}
              {wallBottom && <img className="wall wall-horizontal wall-bottom" src={asset('wall-horizontal.png')} alt="" />}
              {wallLeft && <img className="wall wall-vertical wall-left" src={asset('wall-vertical.png')} alt="" />}
              {wallRight && <img className="wall wall-vertical wall-right" src={asset('wall-vertical.png')} alt="" />}
            </button>
          );
        })}
        <img className="center-emblem" src={asset('center-emblem.png')} alt="" />
      </div>
    </div>
  );
}

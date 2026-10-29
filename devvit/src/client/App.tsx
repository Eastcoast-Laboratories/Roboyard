import { context } from '@devvit/web/client';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { isSolved, moveRobot, parseLevel, restore, snapshot } from './game/engine';
import { COLORS, type Board, type Direction, type GameSnapshot, type RobotColor } from './game/types';

const ASSET = './assets/';
const MOVE_LIMIT = 30;

type Motion = { color: RobotColor; direction: Direction } | null;

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
  const [motion, setMotion] = useState<Motion>(null);
  const [showGuide, setShowGuide] = useState(() => localStorage.getItem('arcane-wisps-guide-seen') !== 'yes');
  const motionTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    let active = true;
    fetch(`./levels/level_${level}.txt`)
      .then((response) => {
        if (!response.ok) throw new Error(`Level ${level} is unavailable.`);
        return response.text();
      })
      .then((source) => {
        if (!active) return;
        const next = parseLevel(source);
        setError('');
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
  const movesLeft = MOVE_LIMIT - history.length;
  const outOfMoves = movesLeft <= 0 && !solved;
  const move = useCallback((direction: Direction) => {
    setBoard((current) => {
      if (!current || solved || history.length >= MOVE_LIMIT) return current;
      const next = moveRobot(current, selected, direction);
      if (!next) return current;
      setHistory((items) => [...items, snapshot(current, selected)]);
      setMotion({ color: selected, direction });
      window.clearTimeout(motionTimer.current);
      motionTimer.current = window.setTimeout(() => setMotion(null), 420);
      return next;
    });
  }, [history.length, selected, solved]);

  useEffect(() => () => window.clearTimeout(motionTimer.current), []);

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

  const cycleWisp = () => {
    if (!board) return;
    const available = COLORS.filter((color) => board.robots.some((robot) => robot.color === color));
    const next = available[(available.indexOf(selected) + 1) % available.length];
    if (next) setSelected(next);
  };

  const closeGuide = () => {
    localStorage.setItem('arcane-wisps-guide-seen', 'yes');
    setShowGuide(false);
  };

  if (error) return <main className="shell"><section className="message"><h1>The ruins are sealed</h1><p>{error}</p></section></main>;
  if (!board) return <main className="shell"><section className="message"><h1>Calling the wisps…</h1></section></main>;

  return (
    <main className="shell">
      <header className="hero">
        <div>
          <span className="eyebrow">Arcane Wisps</span>
          <h1>Slide. Ricochet. Return.</h1>
          <p>Guide every wisp into a matching rune portal before your moves run out.</p>
        </div>
        <div className="community">{context?.subredditName ? `r/${context.subredditName}` : 'Devvit edition'}</div>
      </header>

      <section className="game-card">
        <div className="toolbar">
          <button aria-label="Previous level" onClick={() => setLevel((value) => Math.max(1, value - 1))} disabled={level === 1}>‹</button>
          <div><strong>Level {level}</strong><span className={movesLeft <= 5 ? 'moves-low' : ''}>{movesLeft} moves left</span></div>
          <button aria-label="Next level" onClick={() => setLevel((value) => Math.min(140, value + 1))} disabled={level === 140}>›</button>
        </div>

        <div className="play-area">
          <BoardView board={board} selected={selected} motion={motion} onSelect={setSelected} onMove={move} />
          <aside className="controls">
            <div className="selected-wisp">
              <Wisp color={selected} selected />
              <div><span>Selected wisp</span><strong>{selected}</strong></div>
            </div>
            <button className="switch-wisp" onClick={cycleWisp}>Switch wisp</button>
            <div className="actions">
              <button onClick={undo} disabled={!history.length}>Undo</button>
              <button onClick={reset}>Reset</button>
            </div>
            <button className="how-to" onClick={() => setShowGuide(true)}>How to play</button>
            <p className="hint">Tap a wisp to select it. Swipe or drag across the board to move.</p>
          </aside>
        </div>
      </section>

      {solved && (
        <div className="victory" role="dialog" aria-modal="true" aria-label="Level complete">
          <div className="victory-card">
            <Wisp color={selected} selected />
            <span className="eyebrow">All wisps returned</span>
            <h2>Level {level} restored</h2>
            <p>Completed in {history.length} moves.</p>
            <button onClick={() => setLevel((value) => Math.min(140, value + 1))}>{level === 140 ? 'Play again' : 'Next level'}</button>
          </div>
        </div>
      )}

      {outOfMoves && (
        <div className="victory" role="dialog" aria-modal="true" aria-label="Out of moves">
          <div className="victory-card">
            <span className="eyebrow">The portals faded</span>
            <h2>Out of moves</h2>
            <p>Undo your last move or restart the puzzle and try a shorter route.</p>
            <div className="dialog-actions"><button onClick={undo}>Undo</button><button onClick={reset}>Restart</button></div>
          </div>
        </div>
      )}

      {showGuide && <GameGuide onClose={closeGuide} />}
    </main>
  );
}

function Wisp({ color, selected = false, moving = false, direction = 'right' }: { color: RobotColor; selected?: boolean; moving?: boolean; direction?: Direction }) {
  return <span className={`wisp wisp-${color} ${moving ? 'wisp-moving' : 'wisp-idle'} wisp-${direction}${selected ? ' selected-wisp-sprite' : ''}`} aria-hidden="true" />;
}

function BoardView({ board, selected, motion, onSelect, onMove }: { board: Board; selected: RobotColor; motion: Motion; onSelect: (color: RobotColor) => void; onMove: (direction: Direction) => void }) {
  const cells = Array.from({ length: board.width * board.height }, (_, index) => ({ x: index % board.width, y: Math.floor(index / board.width) }));
  const dragStart = useRef<{ x: number; y: number } | null>(null);
  const finishDrag = (x: number, y: number) => {
    const start = dragStart.current;
    dragStart.current = null;
    if (!start) return;
    const dx = x - start.x;
    const dy = y - start.y;
    if (Math.max(Math.abs(dx), Math.abs(dy)) < 18) return;
    onMove(Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 'right' : 'left') : (dy > 0 ? 'down' : 'up'));
  };
  return (
    <div className="board-frame">
      <div
        className="board"
        style={{ gridTemplateColumns: `repeat(${board.width}, 1fr)`, gridTemplateRows: `repeat(${board.height}, 1fr)` }}
        onPointerDown={(event) => { dragStart.current = { x: event.clientX, y: event.clientY }; }}
        onPointerUp={(event) => finishDrag(event.clientX, event.clientY)}
        onPointerCancel={() => { dragStart.current = null; }}
      >
        {cells.map(({ x, y }) => {
          const goal = board.goals.find((item) => item.x === x && item.y === y);
          const robot = board.robots.find((item) => item.x === x && item.y === y);
          const wallTop = board.horizontalWalls.has(`${x},${y}`);
          const wallBottom = board.horizontalWalls.has(`${x},${y + 1}`);
          const wallLeft = board.verticalWalls.has(`${x},${y}`);
          const wallRight = board.verticalWalls.has(`${x + 1},${y}`);
          return (
            <button key={`${x},${y}`} className="cell" onClick={() => robot && onSelect(robot.color)} aria-label={robot ? `${robot.color} wisp at column ${x + 1}, row ${y + 1}` : `Empty cell ${x + 1}, ${y + 1}`}>
              {goal && <img className="goal" src={asset(`target_${goal.color}.png`)} alt="" />}
              {robot && <Wisp color={robot.color} selected={selected === robot.color} moving={motion?.color === robot.color} direction={motion?.color === robot.color ? motion.direction : 'right'} />}
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

function GameGuide({ onClose }: { onClose: () => void }) {
  return (
    <div className="guide" role="dialog" aria-modal="true" aria-labelledby="guide-title">
      <div className="guide-card">
        <span className="eyebrow">Your objective</span>
        <h2 id="guide-title">Return every wisp</h2>
        <p>Move each colored wisp onto a matching rune portal before the move counter reaches zero.</p>
        <ol>
          <li><strong>Select</strong><span>Tap a wisp on the board, or use Switch wisp.</span></li>
          <li><strong>Move</strong><span>Swipe or drag across the board. Keyboard players can use arrows or WASD.</span></li>
          <li><strong>Plan</strong><span>Wisps slide until a wall or another wisp stops them. Every successful slide costs one move.</span></li>
        </ol>
        <button onClick={onClose}>Start puzzle</button>
      </div>
    </div>
  );
}

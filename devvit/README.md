# Arcane Wisps for Devvit

A React and TypeScript sliding puzzle game for Reddit's Devvit Web runtime.

## Development

```bash
npm install
npm run validate
npm run dev
```

The browser preview runs without Reddit credentials. To test inside Reddit, authenticate with `npm run login` or `npx devvit login`, then run `npm run playtest` from this directory.

The game includes all 140 built-in levels. Progress uses browser-local storage; gameplay simulation remains independent of React in `src/client/game/engine.ts`.

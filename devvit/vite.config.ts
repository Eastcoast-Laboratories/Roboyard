import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    outDir: 'dist/client',
    emptyOutDir: true,
    sourcemap: true,
  },
  test: {
    environment: 'node',
  },
});

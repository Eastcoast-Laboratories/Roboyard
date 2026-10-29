import './styles.css';

import { requestExpandedMode } from '@devvit/web/client';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

export const Splash = () => (
  <main className="splash">
    <div className="splash__content">
      <img className="splash__art" src="/arcane-wisps-splash.gif" alt="Arcane Wisps racing through enchanted ruins" />
      <p>Return every wisp to its matching rune portal before your moves run out.</p>
      <button type="button" onClick={(event) => requestExpandedMode(event.nativeEvent, 'game')}>
        Enter the crypt
      </button>
    </div>
  </main>
);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Splash />
  </StrictMode>
);

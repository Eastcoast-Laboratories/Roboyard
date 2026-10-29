import './styles.css';

import { requestExpandedMode } from '@devvit/web/client';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

export const Splash = () => (
  <main className="splash">
    <div className="splash__content">
      <img className="splash__art" src="/crypt-crawler-splash.gif" alt="Crypt Crawler skeleton heroes racing through a haunted dungeon" />
      <p>Guide each skeleton to its matching summoning circle.</p>
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

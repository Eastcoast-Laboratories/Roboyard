import './styles.css';

import { requestExpandedMode } from '@devvit/web/client';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

export const Splash = () => (
  <main className="splash">
    <div className="splash__content">
      <img src="/assets/robot_silver_left.png" alt="A chibi skeleton adventurer" />
      <div>
        <span className="eyebrow">Crypt Crawler</span>
        <h1>Slide. Ricochet. Awaken.</h1>
        <p>Guide each skeleton to its matching summoning circle.</p>
        <button type="button" onClick={(event) => requestExpandedMode(event.nativeEvent, 'game')}>
          Enter the crypt
        </button>
      </div>
    </div>
  </main>
);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Splash />
  </StrictMode>
);

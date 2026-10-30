// Screenshot: capture splash and game screens of the built client (dist/client)
// Run:   node tools/capture-screenshots.cjs [baseUrl] [outDir]
//        defaults: http://127.0.0.1:8185 and ./screenshots
// Needs the playwright package (npm i -D playwright) and an installed browser
// (npx playwright install chromium, or PLAYWRIGHT_BROWSERS_PATH=<shared cache>).
let chromium;
try {
  ({ chromium } = require('playwright'));
} catch {
  console.error('playwright package not found — run: npm i -D playwright');
  process.exit(1);
}

const BASE = process.argv[2] || 'http://127.0.0.1:8185';
const OUT = process.argv[3] || 'screenshots';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 480, height: 720 } });

  page.on('console', (m) => console.log(`[BROWSER:${m.type()}]`, m.text()));
  page.on('pageerror', (e) => console.log('[BROWSER:pageerror]', e.message));

  await page.goto(`${BASE}/splash.html`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.splash__art', { timeout: 10000 });
  await page.waitForTimeout(800);
  await page.screenshot({ path: `${OUT}/arcane-wisps-splash.png` });
  console.log('splash ok');

  await page.goto(`${BASE}/game.html`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${OUT}/arcane-wisps-guide.png` });
  console.log('guide ok');

  const start = page.getByRole('button', { name: /start puzzle/i });
  if (await start.isVisible().catch(() => false)) {
    await start.click();
    await page.waitForTimeout(800);
  }
  await page.screenshot({ path: `${OUT}/arcane-wisps-game.png` });
  console.log('game ok');

  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });

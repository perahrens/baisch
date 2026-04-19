// @ts-check
/**
 * Tutorial walkthrough test.
 * Walks through every tutorial step, screenshots each one, and logs observations.
 *
 * Coordinate system (GameScreen layout):
 *   Canvas: 450×850 browser pixels.
 *   scale = min(450/450, 850/800) = 1.0  →  25px letterbox bars at top and bottom.
 *
 *   gameStage  – square play area (450×450 logical units):
 *     browserY = 475 − gameStageY   (game y=0 bottom ↔ browser y=475)
 *
 *   handStage  – hand area (450×350 logical units, below the game board):
 *     browserY = 825 − handStageY   (hand y=0 bottom ↔ browser y=825)
 *
 *   overlayStage – full screen (450×800):
 *     browserY = 825 − overlayY     (overlay y=0 bottom ↔ browser y=825)
 *
 *   MenuScreen / TutorialSelectScreen use a different (full-height) viewport; use gc() there.
 *
 * Card sprite dimensions: 200×277 px, scaleFactor=0.22
 *   defWidth  = 200 × 0.22 = 44   (W)
 *   defHeight = 277 × 0.22 = 61   (H)
 */

const { test, expect } = require('@playwright/test');

const GAME_W = 450;
const GAME_H = 800;

/** MenuScreen / overlay legacy helper (uses full-height scale, close enough for overlay buttons). */
function toScreen(box, gameX, gameY) {
  const scaleX = box.width / GAME_W;
  const scaleY = box.height / GAME_H;
  return {
    x: box.x + gameX * scaleX,
    y: box.y + (GAME_H - gameY) * scaleY,
  };
}

async function gc(page, box, gameX, gameY, label = '') {
  const { x, y } = toScreen(box, gameX, gameY);
  if (label) console.log(`  [click] ${label} overlay(${gameX},${gameY}) → browser(${x.toFixed(0)},${y.toFixed(0)})`);
  await page.mouse.click(x, y);
}

/**
 * Click in gameStage coordinates (0–450 x, 0–450 y, y=0 at bottom).
 * browserY = 475 − gameStageY
 */
async function cgame(page, gx, gy, label = '') {
  const px = gx;
  const py = 475 - gy;
  if (label) console.log(`  [click] ${label} game(${gx},${gy}) → browser(${px},${py})`);
  await page.mouse.click(px, py);
}

/**
 * Click in handStage coordinates (0–450 x, 0–350 y, y=0 at bottom).
 * browserY = 825 − handStageY
 */
async function chand(page, hx, hy, label = '') {
  const px = hx;
  const py = 825 - hy;
  if (label) console.log(`  [click] ${label} hand(${hx},${hy}) → browser(${px},${py})`);
  await page.mouse.click(px, py);
}

async function shot(page, label) {
  const slug = label.replace(/[^a-z0-9]+/gi, '_').toLowerCase();
  const path = `test-results/tutorial_${slug}.png`;
  await page.screenshot({ path, fullPage: false });
  console.log(`  [shot] ${label} → ${path}`);
}

test.setTimeout(300_000); // 5 min – allows many waitForTimeout calls

test('tutorial full walkthrough', async ({ page }) => {
  await page.setViewportSize({ width: 450, height: 850 });
  page.on('pageerror', (err) => console.error('[pageerror]', err.message));

  // Override window.prompt so Gdx.input.getTextInput() returns immediately with a name.
  // This prevents the blocking native dialog from ever appearing.
  await page.addInitScript(() => {
    window.prompt = (_msg, _default) => 'TestPlayer';
  });

  // ── Load ──────────────────────────────────────────────────────────────────
  await page.goto('/');
  const canvas = page.locator('#embed-html canvas');
  await expect(canvas).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(3000);
  await shot(page, '01_loaded');

  const box = await canvas.boundingBox();
  console.log(`Canvas: ${JSON.stringify(box)}`);

  // ── Enter name ────────────────────────────────────────────────────────────
  // "Enter your name" button: position = (cx - w/2, 0.3*800) → center ≈ (225, 0.3*800 + h/2)
  // Default button height ~36, doubled for "Ready"=72, enterName uses getHeight()=72 but wait —
  // enterNameButton uses button.getHeight() which is already 2× default. Center ≈ (225, 276)
  await gc(page, box, 225, 276, 'name button');
  // GWT renders getTextInput() as an HTML overlay dialog (not a native browser dialog).
  // Fill the text input. LibGDX canvas intercepts pointer events, so use evaluate()
  // to dispatch a synthetic click directly on the OK button.
  const nameInput = page.locator('input[type="text"]').first();
  await nameInput.waitFor({ state: 'visible', timeout: 5000 });
  await nameInput.fill('TestPlayer');
  const clicked = await page.evaluate(() => {
    const buttons = document.querySelectorAll('button.gwt-Button');
    for (const btn of buttons) {
      if (btn.textContent.trim() === 'OK') {
        btn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
        return true;
      }
    }
    return false;
  });
  console.log('[ok-click]', clicked);
  await page.waitForTimeout(2500);
  await shot(page, '02_after_name');

  // ── Tutorial button ───────────────────────────────────────────────────────
  // Tutorial btn: x = margin + btnW + gap = 16 + 134 + 8 = 158, center x = 225
  // y = 0.06*800 = 48, btnH = button.getHeight() ≈ 72 (doubled), center y ≈ 48 + 36 = 84
  await gc(page, box, 225, 84, 'Tutorial button');
  await page.waitForTimeout(2000);
  await shot(page, '03_tutorial_select_screen');

  // ── TutorialSelectScreen – click "Basic Tutorial" ─────────────────────────
  // It's a scrollable list. First item is likely near top, approximately y=680
  await gc(page, box, 225, 690, 'Basic Tutorial item');
  await page.waitForTimeout(5000);
  await shot(page, '04_game_started');

  // ── INTRO overlay (step 0) ─────────────────────────────────────────────────
  // "Let's go!" button is inside a full-screen overlay Table, centered.
  // Table has outer.center().pad(20), button at roughly y=270 game coords
  await gc(page, box, 225, 295, 'Let\'s go button');
  await page.waitForTimeout(1000);
  await shot(page, '05_after_intro');

  // ═══════════════════════════════════════════════════════════════════════════
  // All positions below use EXACT card dimensions (defWidth=44, defHeight=61):
  //   sprite 200×277 px × scaleFactor 0.22 = 44×61 game units.
  //
  // gameStage (450×450): player 0 positions
  //   King   center: gameStage(225, 31)       → browser(225, 444)
  //   Def 1  center: gameStage(181, 92)       → browser(181, 383)   [225-W, H+H/2]
  //   Def 2  center: gameStage(225, 92)       → browser(225, 383)
  //   Def 3  center: gameStage(269, 92)       → browser(269, 383)   [225+W, H+H/2]
  //
  // Harvest decks (45°-rotated in center of gameStage):
  //   Deck 1: gameStage center ≈(232, 267)   → browser(232, 208)
  //
  // Tutorial Bot (player 1, visual slot 1 – left side, rotated −90°):
  //   Def slot 1 center: gameStage(100, 217)  → browser(100, 258)
  //
  // handStage (450×350): hand cards
  //   Card 0  center: handStage(44, 286)      → browser(44, 539)   [x=W, y=225+H]
  //   Finish turn:    handStage(383, 15)      → browser(383, 810)
  // ═══════════════════════════════════════════════════════════════════════════

  // ── Step 1: TAKE_DEF_FIRST – tap an occupied defense slot ─────────────────
  // Two steps: (1) click defense slot to SELECT it; (2) click empty hand-area
  // background to TAKE the selected card back to hand.
  // Empty hand-area background: hand x≈380 (no cards there), y≈200 center area.
  await cgame(page, 181, 92, 'select def slot 1');
  await page.waitForTimeout(300);
  await chand(page, 380, 200, 'hand bg → take def');
  await page.waitForTimeout(1500);
  await shot(page, '06_after_take_def');

  // ── Step 2: SELECT – tap a hand card ──────────────────────────────────────
  await chand(page, 44, 286, 'hand card 0');
  await page.waitForTimeout(1000);
  await shot(page, '07_hand_card_selected');

  // ── Step 3: INFO_SYMBOLS blocking overlay ─────────────────────────────────
  await gc(page, box, 225, 295, 'Got it (symbols)');
  await page.waitForTimeout(800);
  await shot(page, '08_after_symbols_info');

  // ── Step 4: PLUNDER – tap a harvest deck (card already selected from step 2)
  // Deck 1 center in gameStage: (232, 267) → browser(232, 208)
  await cgame(page, 232, 267, 'harvest deck');
  await page.waitForTimeout(2500);
  await shot(page, '09_after_plunder');

  // ── Step 5: INFO_PLUNDER ──────────────────────────────────────────────────
  // The plunder result ("FAILED. Tap to continue.") is a gameStage overlay and
  // MUST be dismissed by clicking inside gameStage (browser Y 25-475), not the
  // hand area. After it is tapped the tutorial advances to INFO_PLUNDER whose
  // "Got it" button sits lower (~Y 601) because the body text is very long.
  await cgame(page, 225, 200, 'dismiss plunder result');
  await page.waitForTimeout(800);  // wait for INFO_PLUNDER overlay to build
  console.log('  [click] Got it (plunder) browser(225,601)');
  await page.mouse.click(225, 601);
  await page.waitForTimeout(800);
  await shot(page, '10_after_plunder_info');

  // ── Step 6: INFO_JOKER ────────────────────────────────────────────────────
  // INFO_JOKER body text is slightly shorter than INFO_PLUNDER, button ≈ Y 555.
  console.log('  [click] Got it (joker) browser(225,555)');
  await page.mouse.click(225, 555);
  await page.waitForTimeout(800);
  await shot(page, '11_after_joker_info');

  // ── Step 7: DEFENSE – select hand card → place in empty shield slot ────────
  await chand(page, 44, 286, 'hand card for defense');
  await page.waitForTimeout(800);
  await shot(page, '12a_selected_for_defense');
  await cgame(page, 181, 92, 'shield slot 1');
  await page.waitForTimeout(1500);
  await shot(page, '12b_defense_placed');

  // ── Step 8: ENDTURN – click "Finish turn" ─────────────────────────────────
  // finishTurnButton is in handStage at setPosition(450−btnWidth, 0).
  // Button center ≈ handStage(383, 15) → browser(383, 810)
  await chand(page, 383, 15, 'Finish turn');
  await page.waitForTimeout(1500);
  await shot(page, '13_after_finish_turn');

  // ── Step 9: WAITING – bot plays ───────────────────────────────────────────
  await page.waitForTimeout(8000);
  await shot(page, '14_after_bot_turn');

  // ── Step 10: INFO_EXPOSE ──────────────────────────────────────────────────
  // INFO_EXPOSE body is medium length; button ≈ Y 541.
  console.log('  [click] Got it (expose) browser(225,541)');
  await page.mouse.click(225, 541);
  await page.waitForTimeout(800);
  await shot(page, '15_after_expose_info');

  // ── Step 11: INFO_KING ────────────────────────────────────────────────────
  // INFO_KING body is similar length to INFO_JOKER; button ≈ Y 558.
  console.log('  [click] Got it (king info) browser(225,558)');
  await page.mouse.click(225, 558);
  await page.waitForTimeout(800);
  await shot(page, '16_after_king_info');

  // ── Step 12: TAKE_DEF_ALL – click hand bg to take all defense cards at once ──
  // During TAKE_DEF_ALL the game bypasses both the per-turn limit AND the
  // selection requirement, so a single tap on the hand background suffices to
  // take every occupied defense slot back to hand.
  await chand(page, 380, 200, 'hand bg → take all defs');
  await page.waitForTimeout(700);
  await shot(page, '17_all_defs_taken');

  // ── Step 13: KING_ATTACK ──────────────────────────────────────────────────
  // Select own king (center: gameStage(225, 31) → browser(225, 444))
  await cgame(page, 225, 31, 'king card');
  await page.waitForTimeout(800);
  await shot(page, '18a_king_selected');
  // Attack Tutorial Bot's defense slot 1 (gameStage(100, 217) → browser(100, 258))
  await cgame(page, 100, 217, 'enemy def slot');
  await page.waitForTimeout(2500);
  await shot(page, '18b_after_king_attack');

  // ── Step 14: COMPLETE ─────────────────────────────────────────────────────
  await page.waitForTimeout(1000);
  await shot(page, '19_complete');

  console.log('=== Done. Screenshots in test-results/ ===');
});

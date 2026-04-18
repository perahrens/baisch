// @ts-check
const { test, expect } = require('@playwright/test');

test('page loads and canvas appears', async ({ page }) => {
  await page.goto('/');

  // Title should be set
  await expect(page).toHaveTitle(/Baisch/i);

  // The container div must be present immediately
  await expect(page.locator('#embed-html')).toBeVisible();

  // Wait for the GWT bootstrap to create the canvas element
  const canvas = page.locator('#embed-html canvas');
  await expect(canvas).toBeVisible({ timeout: 30_000 });
});

test('no JS errors during load', async ({ page }) => {
  const errors = [];
  page.on('pageerror', (err) => errors.push(err.message));

  await page.goto('/');
  // Wait for the canvas so we know the full bootstrap ran
  await expect(page.locator('#embed-html canvas')).toBeVisible({ timeout: 30_000 });

  expect(errors, `JS errors on load: ${errors.join('; ')}`).toHaveLength(0);
});

test('canvas appears on mobile viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');

  await expect(page.locator('#embed-html')).toBeVisible();
  await expect(page.locator('#embed-html canvas')).toBeVisible({ timeout: 30_000 });
});

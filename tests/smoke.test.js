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

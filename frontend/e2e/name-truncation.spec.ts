import { test, expect } from '@playwright/test';
import { login, apiLogin, apiCreateWorld, apiDeleteWorld, uniqueName } from './support';

// The map name overflow/tooltip fix lives in a CSS rule shared by every
// sidebar list (.article-link), not just Maps. Spot-checks one more of
// those pages (Campaigns) to confirm the fix actually is shared, not
// coincidentally only wired up for Maps.

let worldId: string;
const LONG_NAME = 'A Campaign With An Extremely Long Name That Cannot Possibly Fit In The Sidebar';

test.beforeEach(async ({ page, request }) => {
  const token = await apiLogin(request);
  const world = await apiCreateWorld(request, token, uniqueName('Truncation Test World'));
  worldId = world.id;

  await request.post(`/api/worlds/${worldId}/campaigns`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { name: LONG_NAME },
  });

  await login(page);
  await page.goto(`/worlds/${worldId}/campaigns`);
});

test.afterEach(async ({ request }) => {
  const token = await apiLogin(request);
  await apiDeleteWorld(request, token, worldId);
});

test('long campaign name truncates and shows a real tooltip on hover', async ({ page }) => {
  const nameSpan = page.locator('.article-link span').filter({ hasText: 'A Campaign With' });

  const overflowing = await nameSpan.evaluate((el) => el.scrollWidth > el.clientWidth);
  expect(overflowing).toBe(true);

  await nameSpan.hover();
  const tooltip = page.locator('[data-slot="tooltip-content"]');
  await expect(tooltip).toBeVisible();
  await expect(tooltip).toHaveText(LONG_NAME);
});

import { test, expect } from '@playwright/test';
import { login, apiLogin, apiCreateWorld, apiDeleteWorld, uniqueName } from './support';

// Regression test for: a long map name overflowed its sidebar row instead
// of truncating with an ellipsis + hover tooltip.
//
// Creating a map normally requires uploading an image (MapRequest.mediaId is
// required); that's product behavior worth its own coverage later, but it's
// not what this bug is about. Instead we upload a 1x1 pixel PNG via the API
// to get a real mediaId cheaply, and focus the test on name overflow.

let worldId: string;
const LONG_NAME =
  'The Sprawling Northern Reaches of the Old Empire, Beyond the Frostbound Wastes and Ashen Hills';

const ONE_PX_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
  'base64',
);

test.beforeEach(async ({ page, request }) => {
  const token = await apiLogin(request);
  const world = await apiCreateWorld(request, token, uniqueName('Map Overflow Test World'));
  worldId = world.id;

  const mediaRes = await request.post(`/api/worlds/${worldId}/media`, {
    headers: { Authorization: `Bearer ${token}` },
    multipart: { file: { name: 'pixel.png', mimeType: 'image/png', buffer: ONE_PX_PNG } },
  });
  const media = await mediaRes.json();

  await request.post(`/api/worlds/${worldId}/maps`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { name: LONG_NAME, mediaId: media.id },
  });

  await login(page);
  await page.goto(`/worlds/${worldId}/maps`);
});

test.afterEach(async ({ request }) => {
  const token = await apiLogin(request);
  await apiDeleteWorld(request, token, worldId);
});

test('long map name truncates with an ellipsis and shows the full name on hover', async ({ page }) => {
  const nameSpan = page.getByTestId('map-name').filter({ hasText: 'The Sprawling Northern' });
  await expect(nameSpan).toBeVisible();

  await expect(nameSpan).toHaveAttribute('title', LONG_NAME);

  const { overflowsContainer, textOverflow, whiteSpace } = await nameSpan.evaluate((el) => ({
    overflowsContainer: el.scrollWidth > el.clientWidth,
    textOverflow: getComputedStyle(el).textOverflow,
    whiteSpace: getComputedStyle(el).whiteSpace,
  }));

  expect(textOverflow).toBe('ellipsis');
  expect(whiteSpace).toBe('nowrap');
  // The whole point: the element's rendered box must not be wider than its
  // container, i.e. the name is genuinely clipped, not just styled to clip.
  expect(overflowsContainer).toBe(true);

  const link = page.locator('.article-link').filter({ has: nameSpan });
  const [linkBox, sidebarBox] = await Promise.all([
    link.boundingBox(),
    page.locator('.wiki-sidebar').boundingBox(),
  ]);
  expect(linkBox).not.toBeNull();
  expect(sidebarBox).not.toBeNull();
  if (linkBox && sidebarBox) {
    expect(linkBox.width).toBeLessThanOrEqual(sidebarBox.width + 1);
  }
});

import { test, expect } from '@playwright/test';
import { login, apiLogin, apiCreateWorld, apiDeleteWorld, uniqueName } from './support';

// Exercises the shared MarkdownEditor (ADR-0054) via the Handouts screen,
// which wires it up with no extra fields in the way. Covers two reported
// bugs: formatting not actually rendering while editing, and the toolbar
// giving no indication of which formatting is currently active.

let worldId: string;

test.beforeEach(async ({ page, request }) => {
  const token = await apiLogin(request);
  const world = await apiCreateWorld(request, token, uniqueName('MD Editor Test World'));
  worldId = world.id;
  await login(page);
  await page.goto(`/worlds/${worldId}/handouts`);
  await page.getByTestId('new-handout-button').click();
  await page.getByTestId('handout-title-input').fill('Formatting check');
  await page.getByTestId('md-content').locator('.ProseMirror').click();
  await page.waitForTimeout(400); // let Milkdown's async editor init finish
});

test.afterEach(async ({ request }) => {
  const token = await apiLogin(request);
  await apiDeleteWorld(request, token, worldId);
});

test('bold text actually renders bold, and the button reflects it', async ({ page }) => {
  const content = page.getByTestId('md-content');
  const boldBtn = page.getByTestId('md-toolbar-bold');

  await expect(boldBtn).toHaveAttribute('aria-pressed', 'false');

  await boldBtn.click();
  await expect(boldBtn).toHaveAttribute('aria-pressed', 'true');
  await page.keyboard.type('this is bold');

  await expect(content.locator('strong', { hasText: 'this is bold' })).toBeVisible();

  await boldBtn.click();
  await expect(boldBtn).toHaveAttribute('aria-pressed', 'false');
  await page.keyboard.type(' and this is not');
  await expect(content.locator('strong', { hasText: 'and this is not' })).toHaveCount(0);
});

test('italic text actually renders italic, and the button reflects it', async ({ page }) => {
  const content = page.getByTestId('md-content');
  const italicBtn = page.getByTestId('md-toolbar-italic');

  await italicBtn.click();
  await expect(italicBtn).toHaveAttribute('aria-pressed', 'true');
  await page.keyboard.type('slanted');

  await expect(content.locator('em', { hasText: 'slanted' })).toBeVisible();
});

test('H2 actually renders as a heading, and the button reflects it', async ({ page }) => {
  const content = page.getByTestId('md-content');
  const h2Btn = page.getByTestId('md-toolbar-h2');

  await page.keyboard.type('normal paragraph');
  await page.keyboard.press('Enter');
  await h2Btn.click();
  await expect(h2Btn).toHaveAttribute('aria-pressed', 'true');
  await page.keyboard.type('Section Title');

  const heading = content.locator('h2', { hasText: 'Section Title' });
  await expect(heading).toBeVisible();
  const [headingSize, bodySize] = await Promise.all([
    heading.evaluate((el) => parseFloat(getComputedStyle(el).fontSize)),
    content.locator('p', { hasText: 'normal paragraph' }).evaluate((el) => parseFloat(getComputedStyle(el).fontSize)),
  ]);
  expect(headingSize).toBeGreaterThan(bodySize);
});

test('bullet list renders as a real, visibly-marked list, and toggles off', async ({ page }) => {
  const content = page.getByTestId('md-content');
  const listBtn = page.getByTestId('md-toolbar-bullet-list');

  await listBtn.click();
  await expect(listBtn).toHaveAttribute('aria-pressed', 'true');
  await page.keyboard.type('first item');

  const list = content.locator('ul');
  const item = list.locator('li', { hasText: 'first item' });
  await expect(item).toBeVisible();
  // A <ul> with list-style: none (Tailwind's preflight default) still
  // matches the DOM query above but is indistinguishable from a plain
  // paragraph — the bug was exactly that. Marker must actually be visible.
  await expect(list).not.toHaveCSS('list-style-type', 'none');

  // The button must be a real toggle: clicking it again while still inside
  // the list item turns it back into a plain paragraph.
  await listBtn.click();
  await expect(listBtn).toHaveAttribute('aria-pressed', 'false');
  await expect(content.locator('ul li', { hasText: 'first item' })).toHaveCount(0);
  await expect(content.locator('p', { hasText: 'first item' })).toBeVisible();
});

test('toolbar buttons un-press when the cursor leaves the formatted text', async ({ page }) => {
  const content = page.getByTestId('md-content');
  const boldBtn = page.getByTestId('md-toolbar-bold');

  await page.keyboard.type('plain start ');
  await boldBtn.click();
  await page.keyboard.type('bold middle');
  await boldBtn.click();
  await page.keyboard.type(' plain end');

  // Click into the leading plain text, away from the bold span.
  await content.locator('.ProseMirror').click({ position: { x: 4, y: 8 } });
  await page.keyboard.press('Home');
  await expect(boldBtn).toHaveAttribute('aria-pressed', 'false');
});

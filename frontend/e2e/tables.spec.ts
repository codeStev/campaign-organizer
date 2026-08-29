import { test, expect } from '@playwright/test';
import { login, apiLogin, apiCreateWorld, apiDeleteWorld, uniqueName } from './support';

// Regression test for: the roll table/card deck editor's Save button gave
// no feedback at all, making it hard to tell whether a save went through.

let worldId: string;

test.beforeEach(async ({ page, request }) => {
  const token = await apiLogin(request);
  const world = await apiCreateWorld(request, token, uniqueName('Table Save Test World'));
  worldId = world.id;
  await login(page);
  await page.goto(`/worlds/${worldId}/tables`);
});

test.afterEach(async ({ request }) => {
  const token = await apiLogin(request);
  await apiDeleteWorld(request, token, worldId);
});

test('creating a table saves successfully and the button settles back to normal', async ({ page }) => {
  await page.getByTestId('new-table-button').click();
  await page.getByTestId('table-title-input').fill('Random Encounters');
  await page.getByTestId('table-dice-input').fill('1d6');
  await page.getByTestId('table-entry-body-0').fill('A wandering merchant');

  const saveBtn = page.getByTestId('table-save-button');
  await expect(saveBtn).toHaveText(/create table/i);

  await saveBtn.click();
  // Proves the save actually round-tripped, not just that the label changed.
  await page.waitForURL(/\/tables\/table\/[^/]+$/, { timeout: 5000 });
  await expect(saveBtn).toBeEnabled({ timeout: 3000 });
  await expect(saveBtn).toHaveText(/save table/i, { timeout: 3000 });
});

test('save button shows a disabled "Saving…" state while the request is in flight', async ({ page }) => {
  await page.getByTestId('new-table-button').click();
  await page.getByTestId('table-title-input').fill('Slow Save Table');
  await page.getByTestId('table-dice-input').fill('1d4');
  await page.getByTestId('table-entry-body-0').fill('Something happens');

  // Delay the create request so the transient "Saving…" state is
  // deterministically observable instead of racing a timer.
  await page.route('**/api/worlds/*/roll-tables', async (route) => {
    await new Promise((r) => setTimeout(r, 800));
    await route.continue();
  });

  const saveBtn = page.getByTestId('table-save-button');
  await saveBtn.click();
  await expect(saveBtn).toHaveText(/saving/i);
  await expect(saveBtn).toBeDisabled();

  await expect(saveBtn).toBeEnabled({ timeout: 5000 });
  await expect(saveBtn).not.toHaveText(/saving/i);
});

// Regression test for: markdown in a table entry (e.g. a bullet list) had no
// styling at all in the printed output — same root cause as the preview-pane
// bug, just a different render site (PrintView.tsx uses the same shared CSS).
test('a markdown bullet list in a table entry is visibly marked in the print output', async ({ page }) => {
  await page.getByTestId('new-table-button').click();
  await page.getByTestId('table-title-input').fill('Print Styling Table');
  await page.getByTestId('table-dice-input').fill('1d4');
  await page.getByTestId('table-entry-body-0').fill('- printed item one\n- printed item two');

  await page.getByTestId('table-save-button').click();
  await page.waitForURL(/\/tables\/table\/[^/]+$/, { timeout: 5000 });

  const [popup] = await Promise.all([
    page.waitForEvent('popup'),
    page.getByTestId('table-print-button').click(),
  ]);
  await popup.waitForLoadState();

  const printedList = popup.locator('ul').filter({ hasText: 'printed item one' });
  await expect(printedList).toBeVisible();
  await expect(printedList).not.toHaveCSS('list-style-type', 'none');
});

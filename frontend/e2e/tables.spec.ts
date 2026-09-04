import { test, expect, APIRequestContext } from '@playwright/test';
import { login, apiLogin, apiCreateWorld, apiDeleteWorld, uniqueName } from './support';

async function apiCreateTable(
  request: APIRequestContext,
  token: string,
  worldId: string,
  body: unknown,
): Promise<{ id: string; title: string }> {
  const res = await request.post(`/api/worlds/${worldId}/roll-tables`, {
    headers: { Authorization: `Bearer ${token}` },
    data: body,
  });
  return res.json();
}

// Regression test for: the roll table/card deck editor's Save button gave
// no feedback at all, making it hard to tell whether a save went through.

let worldId: string;

test.beforeEach(async ({ page, request }) => {
  const token = await apiLogin(request);
  const world = await apiCreateWorld(request, token, uniqueName('Table Save Test World'));
  worldId = world.id;
  await login(page);
  await page.goto(`/next/worlds/${worldId}/tables`);
});

// Table/deck creation now goes through CategoryTree's "+" menu on the
// Uncategorised leaf (a fresh world has no other categories yet).
async function newTable(page: import('@playwright/test').Page) {
  await page.getByTitle('New…').click();
  await page.getByRole('menuitem', { name: '+ New roll table' }).click();
}

test.afterEach(async ({ request }) => {
  const token = await apiLogin(request);
  await apiDeleteWorld(request, token, worldId);
});

test('creating a table saves successfully and the button settles back to normal', async ({ page }) => {
  await newTable(page);
  await page.getByTestId('table-title-input').fill('Random Encounters');
  await page.getByTestId('table-dice-input').fill('1d6');
  await page.getByTestId('table-entry-body-0').fill('A wandering merchant');

  const saveBtn = page.getByTestId('table-save-button');
  await expect(saveBtn).toHaveText(/create table/i);

  await saveBtn.click();
  // Proves the save actually round-tripped, not just that the label changed.
  await page.waitForURL(/\/tables\/table\/[^/]+$/, { timeout: 5000 });

  // Saving lands on the read preview; reopening the editor shows it's now
  // treated as an existing table (not stuck offering to create a duplicate).
  await expect(page.getByRole('heading', { name: /Random Encounters/ })).toBeVisible();
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await expect(saveBtn).toBeEnabled({ timeout: 3000 });
  await expect(saveBtn).toHaveText(/save table/i, { timeout: 3000 });
});

// Regression test for: the button's own "Saved" state reverts almost
// immediately (fast round-trips), which is easy to miss entirely — a toast
// is meant to be the feedback that actually lingers long enough to notice.
test('saving shows a toast that lingers, not just an instant button flip', async ({ page }) => {
  await newTable(page);
  await page.getByTestId('table-title-input').fill('Toast Lingers Table');
  await page.getByTestId('table-dice-input').fill('1d4');
  await page.getByTestId('table-entry-body-0').fill('Something happens');

  await page.getByTestId('table-save-button').click();

  const toast = page.locator('[data-sonner-toast]').filter({ hasText: 'Toast Lingers Table' });
  await expect(toast).toBeVisible();
  await expect(toast).toContainText(/saved/i);
  // Still there well after the button's own "Saved" state (1.5s) has reverted.
  await page.waitForTimeout(1800);
  await expect(toast).toBeVisible();
});

test('save button shows a disabled "Saving…" state while the request is in flight', async ({ page }) => {
  await newTable(page);
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

  // Once the (delayed) request resolves, saving lands on the read preview.
  await expect(page.getByRole('button', { name: 'Edit', exact: true })).toBeVisible({ timeout: 5000 });
});

// Regression test for: markdown in a table entry (e.g. a bullet list) had no
// styling at all in the printed output — same root cause as the preview-pane
// bug, just a different render site (PrintView.tsx uses the same shared CSS).
test('a markdown bullet list in a table entry is visibly marked in the print output', async ({ page }) => {
  await newTable(page);
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

// Regression test for: a printed table/deck appended every chained
// table/deck as a separate section, but nothing on the originating row said
// which section a given result actually led to.
test('a chained table is labeled on the row that leads to it, in print', async ({ page, request }) => {
  const token = await apiLogin(request);
  const subTable = await apiCreateTable(request, token, worldId, {
    title: 'Sub Table',
    diceExpression: '1d4',
    entries: [{ minResult: null, maxResult: null, body: 'A sub-result' }],
  });
  const mainTable = await apiCreateTable(request, token, worldId, {
    title: 'Main Table',
    diceExpression: '1d4',
    entries: [
      {
        minResult: null,
        maxResult: null,
        body: 'Roll on the sub table',
        nestedTableIds: [subTable.id],
      },
    ],
  });

  await page.goto(`/next/worlds/${worldId}/tables/table/${mainTable.id}`);

  const [popup] = await Promise.all([
    page.waitForEvent('popup'),
    page.getByTestId('table-print-button').click(),
  ]);
  await popup.waitForLoadState();

  const mainRow = popup.locator('tr').filter({ hasText: 'Roll on the sub table' });
  await expect(mainRow.locator('.print-chain-note')).toHaveText(/Sub Table/);
  await expect(popup.getByRole('heading', { name: 'Sub Table' })).toBeVisible();
});

// Regression test for: every delete button in the app fired immediately on
// click, with no way to back out of a misclick.
test('deleting a table asks for confirmation, and Cancel backs out safely', async ({ page, request }) => {
  const token = await apiLogin(request);
  const table = await apiCreateTable(request, token, worldId, {
    title: 'Delete Me Table',
    diceExpression: '1d4',
    entries: [{ minResult: null, maxResult: null, body: 'An outcome' }],
  });

  await page.goto(`/next/worlds/${worldId}/tables/table/${table.id}`);
  // Delete lives in the editor, not the read preview — open it first.
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  const deleteTrigger = page.getByRole('button', { name: 'Delete', exact: true });

  await deleteTrigger.click();
  const dialog = page.getByRole('alertdialog');
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('Delete Me Table');

  // Cancel must not delete anything.
  await dialog.getByRole('button', { name: 'Cancel' }).click();
  await expect(dialog).not.toBeVisible();
  await page.reload();
  // Deep-linked to this table's own route, so the category tree (ADR-0105)
  // auto-expands its "Uncategorised" bucket to reveal the active entity.
  await expect(page.locator('.category-tree-article').filter({ hasText: 'Delete Me Table' })).toBeVisible();

  // Confirming does delete it.
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await deleteTrigger.click();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(page.getByRole('alertdialog')).not.toBeVisible();
  await expect(page.locator('.category-tree-article').filter({ hasText: 'Delete Me Table' })).toHaveCount(0);
});

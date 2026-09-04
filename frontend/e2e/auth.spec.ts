import { test, expect } from '@playwright/test';
import { login } from './support';

test('wrong password shows an error and does not log in', async ({ page }) => {
  await page.goto('/');
  await page.getByTestId('login-password').fill('definitely-wrong');
  await page.getByTestId('login-submit').click();
  await expect(page.locator('.error')).toContainText(/wrong password/i);
  await expect(page).toHaveURL(/\/$|\/login/);
});

test('correct password logs in and reaches the worlds list', async ({ page }) => {
  await login(page);
  await expect(page).toHaveURL(/\/next\/worlds/);
  await expect(page.getByRole('heading', { name: 'Worlds' })).toBeVisible();
});

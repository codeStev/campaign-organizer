import { APIRequestContext, Page } from '@playwright/test';

const PASSWORD = process.env.APP_PASSWORD ?? 'changeme';

/** Logs in through the real UI, the way a user would. */
export async function login(page: Page) {
  await page.goto('/');
  await page.getByTestId('login-password').fill(PASSWORD);
  await page.getByTestId('login-submit').click();
  await page.waitForURL('**/worlds');
}

/**
 * Direct API helpers for fast test-data setup, so specs can create the
 * world/article/etc. they need without driving forms that aren't the thing
 * under test. Mirrors scripts/seed-from-obsidian.mjs's auth pattern.
 */
export async function apiLogin(request: APIRequestContext): Promise<string> {
  const res = await request.post('/api/auth/login', { data: { password: PASSWORD } });
  const { token } = await res.json();
  return token;
}

export async function apiCreateWorld(
  request: APIRequestContext,
  token: string,
  name: string,
): Promise<{ id: string; name: string }> {
  const res = await request.post('/api/worlds', {
    headers: { Authorization: `Bearer ${token}` },
    data: { name },
  });
  return res.json();
}

export async function apiDeleteWorld(request: APIRequestContext, token: string, worldId: string) {
  await request.delete(`/api/worlds/${worldId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

/** A per-run-unique name so parallel/repeat runs don't collide on title uniqueness. */
export function uniqueName(prefix: string): string {
  return `${prefix} ${Date.now()}-${Math.floor(Math.random() * 1000)}`;
}

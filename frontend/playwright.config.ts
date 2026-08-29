import { defineConfig, devices } from '@playwright/test';

/**
 * E2E tests drive the app as a browser would: against an already-running
 * stack (docker compose up, or `npm run dev` + backend), not a mocked one.
 * They assume a single-user local instance (ADR-0005) and freely create
 * throwaway worlds/articles rather than resetting a shared database.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3001',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});

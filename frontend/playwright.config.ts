import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: 'e2e',
  workers: 1,
  timeout: 60_000,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:8080',
    ...devices['Desktop Chrome'],
    trace: 'retain-on-failure',
  },
});

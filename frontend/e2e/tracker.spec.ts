import { expect, test } from '@playwright/test';

test('applies a suggested interview stage and confirms No response separately', async ({ page }) => {
  const created = await page.request.post('/api/roles', {
    data: { company: 'Playwright', title: 'Final interview example', status: 'APPLIED' },
  });
  expect(created.status()).toBe(201);
  const { id } = await created.json();
  const rolePath = '/roles/' + id;
  await page.goto(rolePath);
  await page.getByRole('link', { name: 'Add interview' }).click();
  await page.getByLabel('Interview title').fill('Final conversation');
  await page.getByLabel('Type', { exact: true }).selectOption('FINAL');
  await page.getByLabel('Status', { exact: true }).selectOption('SCHEDULED');
  const date = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
  await page.getByLabel('Start time').fill(date + 'T14:00');
  await page.getByRole('button', { name: 'Save interview' }).click();
  await expect(page.getByLabel('Status', { exact: true })).toHaveValue('APPLIED');
  await expect(page.getByText('In progress / Final interview', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Update progress', exact: true }).click();
  await expect(page.getByLabel('Status', { exact: true })).toHaveValue('IN_PROGRESS');
  await expect(page.getByLabel('Stage', { exact: true })).toHaveValue('FINAL_INTERVIEW');

  await page.goto(rolePath + '?review=no-response');
  await expect(page.getByLabel('Status', { exact: true })).toHaveValue('IN_PROGRESS');
  await expect(page.getByLabel('Outcome')).toHaveValue('NO_RESPONSE');
  await page.getByRole('button', { name: 'Confirm completion' }).click();
  await expect(page.getByLabel('Role progress')).toContainText('Completed');
  await expect(page.getByLabel('Role progress')).toContainText('No response');
});

test('tracks parallel work, resolves a stale edit, and exports the tracker data', async ({ page, context }) => {
  const company = 'Playwright ' + Date.now();
  const interviewDay = new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10);

  await page.goto('/');
  await expect(page).toHaveURL(/\/today$/);
  await expect(page.getByRole('heading', { name: 'Today', exact: true })).toBeVisible();
  await expect(page.getByRole('region', { name: 'To do', exact: true })).toBeVisible();
  await page.getByRole('navigation').getByRole('link', { name: 'Add role' }).click();
  await page.getByLabel('Company', { exact: true }).fill(company);
  await page.getByLabel('Role title').fill('Platform engineer');
  await page.getByLabel('Starting status').selectOption('APPLIED');
  await expect(page.getByLabel('Stage', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Save role' }).click();
  await expect(page.getByRole('heading', { name: 'Platform engineer' })).toBeVisible();
  const roleUrl = page.url();

  await page.getByLabel('Status', { exact: true }).selectOption('IN_PROGRESS');
  await page.getByLabel('Stage', { exact: true }).selectOption('ASSIGNMENT_PENDING');
  await page.getByRole('button', { name: 'Save progress' }).click();
  await expect(page.getByLabel('Role progress')).toContainText('Assignment pending');

  await page.getByRole('link', { name: 'Add interview' }).click();
  await page.getByLabel('Interview title').fill('Technical discussion');
  await page.getByLabel('Type', { exact: true }).selectOption('TECHNICAL');
  await page.getByLabel('Status', { exact: true }).selectOption('SCHEDULED');
  await page.getByLabel('Start time').fill(interviewDay + 'T14:00');
  await page.getByLabel('Preparation notes').fill('Prepare architecture walkthrough');
  await page.getByLabel('Interview notes').fill('Discussed testing');
  await page.getByLabel('Feedback').fill('Strong testing experience');
  await page.getByRole('button', { name: 'Add interviewer' }).click();
  await page.getByRole('button', { name: 'Add interviewer' }).click();
  await page.getByRole('group', { name: 'Interviewer 1' }).getByLabel('Name').fill('Alex');
  await page.getByRole('group', { name: 'Interviewer 2' }).getByLabel('Name').fill('Taylor');
  await page.getByRole('button', { name: 'Save interview' }).click();
  await expect(page).toHaveURL(roleUrl);
  await expect(page.getByText('In progress / Interview scheduled', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Keep current progress' }).click();
  await expect(page.getByLabel('Role progress')).toContainText('Assignment pending');

  await page.getByRole('link', { name: 'Add assignment' }).click();
  await page.getByLabel('Assignment title').fill('Build a small API');
  await expect(page.getByLabel('Due date')).toHaveAttribute('required', '');
  await page.getByLabel('Due date').fill(interviewDay);
  await page.getByLabel('Instructions', { exact: true }).fill('Implement a status endpoint');
  await page.getByLabel('Work link').fill('https://example.com/repository');
  await page.getByLabel('Instructions link').fill('https://example.com/task');
  await page.getByLabel('Notes', { exact: true }).fill('Use Java');
  await page.getByRole('button', { name: 'Save assignment' }).click();
  await expect(page).toHaveURL(roleUrl);
  await page.getByRole('navigation').getByRole('link', { name: 'Today' }).click();
  const upcoming = page.getByRole('region', { name: 'Upcoming' }).getByRole('listitem').filter({ hasText: company });
  await expect(upcoming.getByRole('link', { name: 'Technical discussion' })).toBeVisible();
  await expect(upcoming.getByRole('link', { name: 'Build a small API' })).toBeVisible();

  await upcoming.getByRole('link', { name: 'Build a small API' }).click();
  await page.getByLabel('Status', { exact: true }).selectOption('SUBMITTED');
  await expect(page.getByLabel('Submission URL')).toHaveCount(0);
  await expect(page.getByLabel('Work link')).toHaveValue('https://example.com/repository');
  await page.getByRole('button', { name: 'Save assignment' }).click();
  await expect(page).toHaveURL(roleUrl);
  await page.getByRole('link', { name: 'Build a small API' }).first().click();
  await expect(page.getByLabel('Submission time')).not.toHaveValue('');
  await expect(page.getByLabel('Instructions', { exact: true })).toHaveValue('Implement a status endpoint');
  await page.getByLabel('Feedback').fill('Reviewer approved the design');
  await page.getByRole('button', { name: 'Save assignment' }).click();
  await expect(page).toHaveURL(roleUrl);

  await page.getByRole('button', { name: 'Set follow-up' }).click();
  await page.getByLabel('Action', { exact: true }).fill('Email next steps');
  await page.getByLabel('Due date').fill(interviewDay);
  await page.getByRole('button', { name: 'Save follow-up' }).click();
  await expect(page.getByText('Email next steps')).toBeVisible();
  await page.getByRole('navigation').getByRole('link', { name: 'Today' }).click();
  const followUp = upcoming.filter({ hasText: 'Email next steps' });
  await followUp.getByRole('button', { name: 'Complete follow-up' }).click();
  await expect(followUp).toHaveCount(0);
  await expect(upcoming.getByRole('link', { name: 'Build a small API' })).toHaveCount(0);

  await page.goto(roleUrl);
  await expect(page.getByText('Follow-up completed', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Complete role' }).click();
  await page.getByLabel('Outcome').selectOption('ACCEPTED');
  await page.getByRole('button', { name: 'Confirm completion' }).click();
  await expect(page.getByLabel('Role progress')).toContainText('Accepted');
  await page.getByRole('navigation').getByRole('link', { name: 'Today' }).click();
  await expect(upcoming.getByRole('link', { name: 'Technical discussion' })).toHaveCount(0);
  await page.goto(roleUrl);
  await page.getByRole('button', { name: 'Reopen role' }).click();
  await expect(page.getByLabel('Role progress')).toContainText('Saved');

  const older = await context.newPage();
  await older.goto(roleUrl);
  await older.getByRole('button', { name: 'Edit details' }).click();
  await older.getByLabel('Role notes').fill('Older draft');
  await page.getByRole('button', { name: 'Edit details' }).click();
  await page.getByLabel('Role notes').fill('Latest saved note');
  await page.getByRole('button', { name: 'Save details' }).click();
  await expect(page.getByText('Latest saved note', { exact: true })).toBeVisible();
  const conflict = older.waitForResponse((response) => response.request().method() === 'PATCH' && response.status() === 409);
  await older.getByRole('button', { name: 'Save details' }).click();
  await conflict;
  await expect(older.getByRole('region', { name: 'Latest saved version' })).toContainText('Latest saved note');
  await expect(older.getByLabel('Role notes')).toHaveValue('Older draft');
  await older.close();

  await page.getByRole('navigation').getByRole('link', { name: 'Settings' }).click();
  const exported = page.waitForEvent('download');
  await page.getByRole('link', { name: 'Download JSON' }).click();
  const download = await exported;
  expect(download.suggestedFilename()).toBe('job-search-tracker.json');
  const stream = await download.createReadStream();
  const chunks: Buffer[] = [];
  for await (const chunk of stream) chunks.push(chunk);
  const snapshot = JSON.parse(Buffer.concat(chunks).toString('utf8'));
  const role = snapshot.roles.find((item: { company: string }) => item.company === company);
  expect(snapshot.schemaVersion).toBe(3);
  expect(role.status).toBe('SAVED');
  expect(role.notes).toBe('Latest saved note');
  expect(role.interviews[0].participants.map((person: { name: string }) => person.name)).toEqual(['Alex', 'Taylor']);
  expect(role.assignments[0].feedback).toBe('Reviewer approved the design');
  expect(role.assignments[0].instructionsUrl).toBe('https://example.com/task');
  expect(role.assignments[0].notes).toBe('Use Java');
  expect(role.timeline.map((event: { kind: string }) => event.kind))
    .toEqual(expect.arrayContaining(['FOLLOW_UP_COMPLETED', 'ROLE_COMPLETED', 'ROLE_REOPENED']));

  const missingAsset = await page.request.get('/assets/missing.js');
  const missingApi = await page.request.get('/api/missing');
  expect(missingAsset.status()).toBe(404);
  expect(missingApi.status()).toBe(404);
  expect(missingApi.headers()['content-type']).toContain('application/problem+json');
});

test('saves light and dark themes across reloads', async ({ page }, testInfo) => {
  await page.goto('/settings');
  await page.getByLabel('Theme', { exact: true }).selectOption('DARK');
  await page.getByLabel('Tracker time zone').fill('Europe/Warsaw');
  await page.getByRole('button', { name: 'Save settings', exact: true }).click();
  await expect(page.getByRole('status')).toHaveText('Settings saved.');
  await expect(page.locator('html')).toHaveCSS('background-color', 'rgb(17, 26, 22)');
  await page.reload();
  await expect(page.getByLabel('Theme', { exact: true })).toHaveValue('DARK');
  await expect(page.getByLabel('Tracker time zone')).toHaveValue('Europe/Warsaw');
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  await page.screenshot({ path: testInfo.outputPath('settings-dark.png'), fullPage: true });
  await page.getByRole('navigation').getByRole('link', { name: 'Today' }).click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  await page.screenshot({ path: testInfo.outputPath('today-dark.png'), fullPage: true });
  await page.getByRole('navigation').getByRole('link', { name: 'Settings' }).click();
  await page.getByLabel('Theme', { exact: true }).selectOption('LIGHT');
  await page.getByRole('button', { name: 'Save settings', exact: true }).click();
  await expect(page.locator('html')).toHaveCSS('background-color', 'rgb(245, 247, 243)');
  await page.reload();
  await expect(page.getByLabel('Theme', { exact: true })).toHaveValue('LIGHT');
  await page.screenshot({ path: testInfo.outputPath('settings-light.png'), fullPage: true });
});

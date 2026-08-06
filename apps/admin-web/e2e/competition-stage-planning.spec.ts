import { expect, test, type Page } from '@playwright/test';
import {
  getStages,
  prepareDraftFixture,
  saveStages,
  stageE2EEnabled,
  stageE2EEventId,
} from './support/competitionStageFixture';
import { installLocalCompetitionStageFixture } from './support/localCompetitionStageFixture';

const localFixtureEnabled = process.env.STAGE_E2E_LOCAL_FIXTURE === '1';

async function loginThroughUi(page: Page, options: {
  tenantLabel: string;
  username: string;
  password: string;
  captchaDistance: number;
}) {
  await page.goto('/competition/stages');
  const alreadyLoaded = page.getByTestId('stage-planning-toolbar');
  const loginForm = page.locator('button[type="submit"]');
  if (!(await alreadyLoaded.isVisible().catch(() => false))) {
    await page.locator('.ant-select').first().click();
    await page.getByText(options.tenantLabel, { exact: true }).click();
    await page.getByLabel('账号').fill(options.username);
    await page.getByLabel('密码').fill(options.password);
    await loginForm.click();

    await expect(page.locator('.slider-captcha__bg')).toBeVisible();
    const handle = page.locator('.slider-captcha__handle');
    await expect(handle).toBeVisible();
    const box = await handle.boundingBox();
    if (!box) throw new Error('captcha slider handle is not measurable');
    const startX = box.x + box.width / 2;
    const startY = box.y + box.height / 2;
    await handle.dispatchEvent('mousedown', { bubbles: true, clientX: startX, clientY: startY });
    await page.mouse.move(startX + options.captchaDistance, startY, { steps: 12 });
    await page.evaluate(({ x, y }) => document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: x, clientY: y })), {
      x: startX + options.captchaDistance,
      y: startY,
    });
  }
  await expect(alreadyLoaded).toBeVisible();
}

async function dragToCanvas(page: Page, sourceTestId: string, xOffset: number, yOffset = 260) {
  const source = page.getByTestId(sourceTestId);
  const target = page.getByTestId('stage-planning-canvas');
  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();
  if (!sourceBox || !targetBox) throw new Error(`cannot measure drag ${sourceTestId} -> stage-planning-canvas`);
  await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2);
  await page.mouse.down();
  await page.mouse.move(targetBox.x + Math.min(xOffset, targetBox.width - 40), targetBox.y + Math.min(yOffset, targetBox.height - 40), { steps: 8 });
  await page.mouse.up();
}

async function fillDateRangePicker(page: Page, testId: string, startAt: string, endAt: string) {
  const inputs = page.locator(`[data-testid="${testId}"]`);
  await expect(inputs).toHaveCount(2);
  await inputs.nth(0).fill(startAt);
  await inputs.nth(0).press('Enter');
  await inputs.nth(1).fill(endAt);
  await inputs.nth(1).press('Enter');
}

async function connectHandles(page: Page, sourceTestId: string, targetTestId: string) {
  await page.getByTestId(sourceTestId).scrollIntoViewIfNeeded();
  await page.getByTestId(targetTestId).scrollIntoViewIfNeeded();
  const source = await page.getByTestId(sourceTestId).boundingBox();
  const target = await page.getByTestId(targetTestId).boundingBox();
  if (!source || !target) throw new Error(`cannot measure connection ${sourceTestId} -> ${targetTestId}`);
  const start = { x: source.x + source.width / 2, y: source.y + source.height / 2 };
  const end = { x: target.x + target.width / 2, y: target.y + target.height / 2 };
  await page.mouse.move(start.x, start.y);
  await page.mouse.down();
  await page.mouse.move(end.x, end.y, { steps: 12 });
  await page.mouse.up();
}

async function exerciseStageCanvas(page: Page, assertSaved: () => Promise<Array<{ stageType: string; sortOrder: number }>>) {
  await page.reload();
  await expect(page.getByTestId('stage-node-preliminary')).toBeVisible();

  // Remove the fixture's initial node through the real canvas keyboard path.
  await page.getByTestId('stage-node-preliminary').click();
  await page.keyboard.press('Delete');
  await expect(page.getByTestId('stage-node-preliminary')).toHaveCount(0);

  await dragToCanvas(page, 'stage-library-preliminary', 180, 90);
  await dragToCanvas(page, 'stage-library-semifinal', 180, 250);
  await dragToCanvas(page, 'stage-library-final', 180, 410);
  await expect(page.getByTestId('stage-node-preliminary')).toBeVisible();
  await expect(page.getByTestId('stage-node-semifinal')).toBeVisible();
  await expect(page.getByTestId('stage-node-final')).toBeVisible();

  await page.getByTestId('stage-node-preliminary').click({ button: 'right' });
  const openContextMenu = page.locator('.ant-dropdown:not(.ant-dropdown-hidden)');
  await expect(openContextMenu).toHaveCount(1);
  await expect(openContextMenu.getByText('理论配置', { exact: true })).toBeVisible();
  await expect(openContextMenu.locator('li.ant-dropdown-menu-submenu-disabled')).toHaveCount(3);
  await page.getByTestId('stage-node-final').click({ button: 'right' });
  await expect(page.getByTestId('stage-node-final')).toHaveClass(/is-selected/);
  await expect(page.getByTestId('stage-inspector')).toContainText('决赛');
  await expect(openContextMenu).toHaveCount(1);
  await expect(openContextMenu.getByText('理论配置', { exact: true })).toBeVisible();
  await expect(openContextMenu.locator('li.ant-dropdown-menu-submenu-disabled')).toHaveCount(3);
  await page.keyboard.press('Escape');
  await expect(openContextMenu).toHaveCount(0);

  await dragToCanvas(page, 'stage-node-semifinal', 360, 300);
  // Normalize the drop positions before selecting nodes again; this keeps the
  // real browser path deterministic even when fitView scales the canvas.
  await page.getByTestId('stage-auto-layout').click();
  await page.getByTestId('stage-fit-view').click();
  await page.waitForTimeout(250);
  await connectHandles(page, 'stage-handle-preliminary-source', 'stage-handle-semifinal-target');
  await connectHandles(page, 'stage-handle-semifinal-source', 'stage-handle-final-target');

  await page.locator('.react-flow__edge-path').nth(1).click({ force: true });
  await expect(page.getByTestId('stage-edge-note-editor')).toBeVisible();
  await page.getByTestId('stage-edge-note-editor').fill('晋级名单进入半决赛');

  const descriptions: Array<[string, string]> = [
    ['preliminary', '完成初赛筛选'],
    ['semifinal', '完成半决赛晋级'],
    ['final', '完成决赛排名'],
  ];
  for (const [stageType, description] of descriptions) {
    await page.getByTestId(`stage-node-${stageType}`).click();
    await page.getByTestId('stage-inspector-description').fill(description);
  }

  const windows: Array<[string, string, string]> = [
    ['preliminary', '2026-08-04 09:00:00', '2026-08-04 10:00:00'],
    ['semifinal', '2026-08-04 10:00:00', '2026-08-04 11:00:00'],
    ['final', '2026-08-04 11:00:00', '2026-08-04 12:00:00'],
  ];
  for (const [stageType, startAt, endAt] of windows) {
    await page.getByTestId(`stage-node-${stageType}`).click();
    await fillDateRangePicker(page, 'stage-inspector-time-range', startAt, endAt);
  }

  await page.getByTestId('stage-auto-layout').click();
  await page.getByTestId('stage-validate').click();
  await expect(page.getByTestId('stage-validation-success')).toBeVisible();
  const saveResponsePromise = page.waitForResponse(
    (response) => response.request().method() === 'PUT' && response.url().includes('/stages/draft'),
    { timeout: 5000 },
  );
  await page.getByTestId('stage-save-draft').click();
  await saveResponsePromise;
  await expect(page.getByTestId('stage-dirty-indicator')).toHaveCount(0);

  const saved = await assertSaved();
  expect(saved.map((stage) => stage.stageType)).toEqual(['preliminary', 'semifinal', 'final']);
  expect(saved.map((stage) => stage.sortOrder)).toEqual([20, 40, 50]);

  await page.reload();
  await expect(page.getByTestId('stage-node-preliminary')).toBeVisible();
  await expect(page.getByTestId('stage-node-semifinal')).toBeVisible();
  await expect(page.getByTestId('stage-node-final')).toBeVisible();
}

test('real stage canvas workflow persists only business stage fields', async ({ page }) => {
  test.skip(!stageE2EEnabled(), 'set STAGE_E2E_EVENT_ID/TENANT_CODE/USERNAME/PASSWORD/CAPTCHA_X for the safe real fixture');
  const eventId = stageE2EEventId();
  await loginThroughUi(page, {
    tenantLabel: process.env.STAGE_E2E_TENANT_LABEL || process.env.STAGE_E2E_TENANT_CODE!,
    username: process.env.STAGE_E2E_USERNAME!,
    password: process.env.STAGE_E2E_PASSWORD!,
    captchaDistance: Number(process.env.STAGE_E2E_CAPTCHA_X),
  });
  const fixture = await prepareDraftFixture(page, eventId);
  try {
    await exerciseStageCanvas(page, () => getStages(page, eventId));
  } finally {
    await saveStages(page, eventId, fixture.restoreStages);
  }
});

test('local safe fixture exercises the real browser login and canvas click chain', async ({ page }) => {
  test.skip(!localFixtureEnabled, 'set STAGE_E2E_LOCAL_FIXTURE=1 to run the isolated browser fixture');
  const fixture = await installLocalCompetitionStageFixture(page);
  await loginThroughUi(page, {
    tenantLabel: '本地测试租户',
    username: 'fixture-admin',
    password: 'fixture-password',
    captchaDistance: 40,
  });
  await exerciseStageCanvas(page, async () => fixture.getStages());
});

/**
 * 标签页"刷新标签页"用例（用户报告：点击后没有反应）。
 *
 * 定性：重挂载链路（refreshCounters → 面板 key）一直是通的，但 react-query 缓存
 * 瞬时回填 + 后台静默重取让刷新在数据没变化时零可见反馈。修复后断言三件事：
 * ① 点击 ⟳ / 右键菜单"刷新标签页"后，面板列表接口重新请求（重挂载 → refetchOnMount）；
 * ② 面板 DOM 真实重挂载（刷新前埋的探针属性应随节点替换而消失）；
 * ③ 可见反馈：加载遮罩出现并收起、容器滚回顶部。
 * 另覆盖覆盖态标签（教材大纲编辑页）的刷新——修复前它是彻底的 no-op。
 *
 * 运行：STAGE_E2E_MANAGE_SERVER=1 npx playwright test e2e/tab-refresh.spec.ts
 */
import { expect, test, type Page } from '@playwright/test';

const TAB_KEY = 'textbook_project';
const PROJECT_ID = '2101516042824790018';

const user = {
  id: 1000,
  tenantId: 10159,
  username: 'admin',
  displayName: '平台管理员',
  userType: 'admin',
  roles: ['PLATFORM_ADMIN'],
  permissions: ['textbook:project:view', 'textbook:project:manage', 'textbook:project:export'],
};

const menuTree = [
  {
    id: 1, parentId: 0, menuType: 'directory', code: 'zhi_neng_ke_cheng_cai_liao',
    name: '智能课程材料管理', path: '', component: '', permissionCode: '', icon: '',
    sortOrder: 1,
    children: [
      {
        id: 2, parentId: 1, menuType: 'menu', code: TAB_KEY,
        name: '教材生成管理', path: '/jiao-cai-sheng-cheng',
        component: 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookProjectPage',
        permissionCode: 'textbook:project:view', icon: 'BookOutlined', sortOrder: 5,
        children: [],
      },
    ],
  },
];

/** 50 行让内容超出视口，才能验证"滚回顶部"；id 全程字符串拼接（雪花 id 超 JS 安全整数，禁用数字运算） */
const projectRows = Array.from({ length: 50 }, (_, i) => ({
  id: i === 0 ? PROJECT_ID : `${PROJECT_ID.slice(0, -2)}${String(10 + i).padStart(2, '0')}`,
  workTypeTitle: `工种${i + 1}`, direction: '电解',
  levelCode: 5, trainingHours: 80, totalWords: 120000, status: 'draft',
  updatedAt: '2026-10-23 10:00:00', createdAt: '2026-10-23 10:00:00',
}));

async function installFixture(page: Page) {
  const ok = (data: unknown) => ({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ code: '0', message: 'OK', data, traceId: 'tab-refresh-fixture' }),
  });
  const transparentPng =
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';
  let listRequests = 0;
  let projectRequests = 0;
  let outlineRequests = 0;

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace(/^\/api/, '');
    const method = request.method();

    if (path === '/auth/tenants') return route.fulfill(ok([{ code: 'demo', name: '千铝工投' }]));
    if (path === '/auth/captcha/generate') {
      return route.fulfill(ok({
        captchaId: 'captcha-1', bgImage: `data:image/png;base64,${transparentPng}`,
        sliderImage: `data:image/png;base64,${transparentPng}`, sliderY: 40,
      }));
    }
    if (path === '/auth/captcha/verify') {
      return route.fulfill(ok({ success: true, captchaToken: 'captcha-token', message: null }));
    }
    if (path === '/auth/login' && method === 'POST') {
      return route.fulfill(ok({
        accessToken: 'tab-refresh-fixture-token', refreshToken: 'r', expiresIn: 3600,
        tenantId: 10159, user,
      }));
    }
    if (path === '/auth/me') return route.fulfill(ok(user));
    if (path === '/auth/menus') return route.fulfill(ok(menuTree));
    if (path === '/textbook/projects' && method === 'GET') {
      listRequests += 1;
      return route.fulfill(ok({ items: projectRows, page: 1, pageSize: 20, total: projectRows.length }));
    }
    if (path === `/textbook/projects/${PROJECT_ID}` && method === 'GET') {
      projectRequests += 1;
      return route.fulfill(ok(projectRows[0]));
    }
    if (path === `/textbook/projects/${PROJECT_ID}/outline` && method === 'GET') {
      outlineRequests += 1;
      return route.fulfill(ok([]));
    }
    return route.continue();
  });

  return {
    getListRequests: () => listRequests,
    getProjectRequests: () => projectRequests,
    getOutlineRequests: () => outlineRequests,
  };
}

/** 走真实登录 UI（验证码接口已打桩成功），照 coursewareFixture.loginThroughUi */
async function loginThroughUi(page: Page) {
  await page.goto('/');
  const submitBtn = page.locator('button[type="submit"]');
  await submitBtn.waitFor({ state: 'visible', timeout: 15_000 });
  await page.locator('.ant-select').first().click();
  await page.locator('.ant-select-item-option-content', { hasText: '千铝工投' }).first().click();
  await page.getByLabel('账号').fill('admin');
  await page.getByLabel('密码').fill('Admin@123456');
  await submitBtn.click();
  const handle = page.locator('.slider-captcha__handle');
  if (await handle.isVisible().catch(() => false)) {
    const box = await handle.boundingBox();
    if (box) {
      const startX = box.x + box.width / 2;
      const startY = box.y + box.height / 2;
      await handle.dispatchEvent('mousedown', { bubbles: true, clientX: startX, clientY: startY });
      await page.mouse.move(startX + 60, startY, { steps: 8 });
      await page.evaluate(({ x, y }) =>
        document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: x, clientY: y })),
        { x: startX + 60, y: startY });
    }
  }
}

async function openTextbookTab(page: Page) {
  await page.locator('.app-menu .ant-menu-title-content', { hasText: '智能课程材料管理' }).first().click();
  await page.locator('.app-menu .ant-menu-title-content', { hasText: '教材生成管理' }).first().click();
  await expect(page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="${TAB_KEY}"]`))
    .toHaveClass(/ant-tabs-tab-active/);
  await expect(page.locator(`.app-tab-pane[data-tab-key="${TAB_KEY}"] .ant-table-tbody`)).toBeVisible();
}

/** 在面板表格节点上埋探针属性；重挂载后节点被替换，属性应消失 */
async function plantRemountProbe(page: Page) {
  await page.evaluate((key) => {
    const tbody = document.querySelector(`.app-tab-pane[data-tab-key="${key}"] .ant-table-tbody`);
    tbody?.setAttribute('data-remount-probe', '1');
  }, TAB_KEY);
}

async function expectRemounted(page: Page) {
  await expect.poll(async () =>
    page.evaluate((key) => {
      const tbody = document.querySelector(`.app-tab-pane[data-tab-key="${key}"] .ant-table-tbody`);
      return tbody?.getAttribute('data-remount-probe');
    }, TAB_KEY),
  ).toBe(null);
}

/** 断言刷新反馈闭环：遮罩出现 → 收起（数据平息） */
async function expectRefreshOverlayCycle(page: Page) {
  const overlay = page.locator('.app-tab-pane-refreshing');
  await expect(overlay).toBeVisible();
  await expect(overlay).toBeHidden({ timeout: 10_000 });
}

test.describe('标签页刷新', () => {
  test('⟳ 图标点击：重发请求 + 重挂载 + 遮罩反馈', async ({ page }) => {
    const fixture = await installFixture(page);
    await loginThroughUi(page);
    await openTextbookTab(page);
    const before = fixture.getListRequests();
    expect(before).toBeGreaterThan(0);

    await plantRemountProbe(page);
    const refreshIcon = page.locator(
      `.app-tabbar-strip .ant-tabs-tab[data-node-key="${TAB_KEY}"] .app-tab-refresh`,
    );
    await expect(refreshIcon).toBeVisible();
    await refreshIcon.click();

    await expectRefreshOverlayCycle(page);
    await expect.poll(() => fixture.getListRequests()).toBeGreaterThan(before);
    await expectRemounted(page);
  });

  test('右键菜单"刷新标签页"：重发请求 + 重挂载', async ({ page }) => {
    const fixture = await installFixture(page);
    await loginThroughUi(page);
    await openTextbookTab(page);
    const before = fixture.getListRequests();

    await plantRemountProbe(page);
    await page
      .locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="${TAB_KEY}"] .app-tab-label`)
      .click({ button: 'right' });
    await page.locator('.ant-dropdown-menu').getByText('刷新标签页').click();

    await expect.poll(() => fixture.getListRequests()).toBeGreaterThan(before);
    await expectRemounted(page);
  });

  test('刷新滚回顶部（F5 语义）', async ({ page }) => {
    await installFixture(page);
    await loginThroughUi(page);
    await openTextbookTab(page);

    await page.locator('.app-content').evaluate((el) => { el.scrollTop = 800; });
    await expect.poll(async () =>
      page.locator('.app-content').evaluate((el) => el.scrollTop),
    ).toBeGreaterThan(0);

    await page.locator(
      `.app-tabbar-strip .ant-tabs-tab[data-node-key="${TAB_KEY}"] .app-tab-refresh`,
    ).click();

    await expect.poll(async () =>
      page.locator('.app-content').evaluate((el) => el.scrollTop),
    ).toBe(0);
  });

  test('覆盖态标签（大纲编辑页）刷新：重发请求 + 遮罩反馈', async ({ page }) => {
    const fixture = await installFixture(page);
    await loginThroughUi(page);
    await openTextbookTab(page);

    await page.locator(`.app-tab-pane[data-tab-key="${TAB_KEY}"] .ant-table-tbody`)
      .getByRole('button', { name: '大纲' })
      .first()
      .click();
    await expect.poll(() => new URL(page.url()).pathname, { timeout: 10_000 })
      .toBe(`/textbook/outline/${PROJECT_ID}`);
    await expect(page.locator('.app-tabbar-strip .ant-tabs-tab'))
      .toHaveCount(2); // 固定标签 + 覆盖态教材标签
    const projectBefore = fixture.getProjectRequests();
    const outlineBefore = fixture.getOutlineRequests();
    // 大纲页是 React.lazy：等它挂载并发出详情/大纲请求后再记录基线
    await expect.poll(() => fixture.getProjectRequests(), { timeout: 15_000 }).toBeGreaterThan(projectBefore);
    await expect.poll(() => fixture.getOutlineRequests(), { timeout: 15_000 }).toBeGreaterThan(outlineBefore);
    const projectBase = fixture.getProjectRequests();
    const outlineBase = fixture.getOutlineRequests();
    expect(projectBase).toBeGreaterThan(0);

    await page.locator(
      `.app-tabbar-strip .ant-tabs-tab[data-node-key="${TAB_KEY}"] .app-tab-refresh`,
    ).click();

    await expectRefreshOverlayCycle(page);
    await expect.poll(() => fixture.getProjectRequests()).toBeGreaterThan(projectBase);
    await expect.poll(() => fixture.getOutlineRequests()).toBeGreaterThan(outlineBase);
  });
});

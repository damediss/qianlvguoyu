/**
 * admin-web 标签页外壳 e2e（《admin-web标签页外壳改造方案》§5.3 E2E-01~08）。
 *
 * 运行前提（与 competition-stage-planning.spec.ts 同口径）：
 *   STAGE_E2E_BASE_URL 指向已登录态的 dev server（默认 http://127.0.0.1:5173），
 *   且浏览器已持有平台登录 cookie/token —— 本 spec 不覆盖登录流程。
 *   本地起服务：STAGE_E2E_MANAGE_SERVER=1 npx playwright test e2e/adminTabs.spec.ts
 *
 * 用例映射：
 *   E2E-01 主流程（开 3 标签→中键关 1→右键关其他）
 *   E2E-02 刷新恢复（F5 后标签/激活位/持久化还原）
 *   E2E-03 直达（粘贴菜单 path → 自动开标签）
 *   E2E-04 竞赛并存（竞赛 URL 直达 → 覆盖态 → 点其他菜单出标签）
 *   E2E-05 覆盖态回退（竞赛工作台 → 知识库 → 点回竞赛标签）
 *   E2E-07 浏览器前进/后退在标签间切换
 *   E2E-08 登出清空（跳过：涉及真实登出会污染共享登录态，见用例内注释）
 *   E2E-06 拖拽排序：以 store 级 UT-20 覆盖排序逻辑，浏览器拖拽交互依赖
 *          dnd-kit PointerSensor 真实指针事件，headless 下不稳定，标注 manual。
 */
import { expect, test, type Page } from '@playwright/test';

const MENU_NAMES = {
  knowledge: '管理概览',
  knowledgeFix: '知识归属修正',
  systemUser: '用户管理',
} as const;

const PATHS = {
  knowledge: '/knowledge',
  knowledgeFix: '/question-bank/knowledge-fix',
  systemUser: '/system/users',
  competitionEvents: '/competition/events',
} as const;

/** 打开侧栏菜单（antd inline 菜单：先展开父级再点叶子） */
async function clickMenuItem(page: Page, name: string) {
  const item = page.locator(`.app-menu .ant-menu-title-content`, { hasText: name }).first();
  await item.click();
}

async function expectTabCount(page: Page, count: number) {
  await expect(page.locator('.app-tabbar-strip .ant-tabs-tab')).toHaveCount(count);
}

async function expectActiveTab(page: Page, key: string) {
  await expect(page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="${key}"]`)).toHaveClass(/ant-tabs-tab-active/);
}

test.describe('admin-web 标签页外壳', () => {
  test('E2E-01 主流程：开 3 个标签 → 中键关 1 个 → 右键关闭其他', async ({ page }) => {
    await page.goto(PATHS.knowledge);
    await expect(page.locator('.app-tabbar-strip')).toBeVisible();
    await expectTabCount(page, 1);

    await clickMenuItem(page, MENU_NAMES.knowledgeFix);
    await expectActiveTab(page, 'knowledge_fix');
    await clickMenuItem(page, MENU_NAMES.systemUser);
    await expectActiveTab(page, 'system_user');
    await expectTabCount(page, 3);

    // 中键关闭 knowledge_fix 标签
    const fixTabText = page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="knowledge_fix"] .app-tab-label`);
    await fixTabText.click({ button: 'middle' });
    await expectTabCount(page, 2);
    await expect(page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="knowledge_fix"]`)).toHaveCount(0);

    // 右键 knowledge_fix 重新打开后：关闭其他
    await clickMenuItem(page, MENU_NAMES.knowledgeFix);
    await expectTabCount(page, 3);
    const fixLabel = page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="knowledge_fix"] .app-tab-label`);
    await fixLabel.click({ button: 'right' });
    await page.getByText('关闭其他标签页').click();
    await expectTabCount(page, 2);
    await expectActiveTab(page, 'knowledge_fix');
  });

  test('E2E-02 刷新恢复：F5 后标签列表、激活位还原', async ({ page }) => {
    await page.goto(PATHS.knowledge);
    await clickMenuItem(page, MENU_NAMES.knowledgeFix);
    await clickMenuItem(page, MENU_NAMES.systemUser);
    await expectActiveTab(page, 'system_user');

    await page.reload();
    await expect(page.locator('.app-tabbar-strip')).toBeVisible();
    await expectTabCount(page, 3);
    await expectActiveTab(page, 'system_user');
  });

  test('E2E-03 直达：粘贴菜单 path URL → 自动开对应标签', async ({ page }) => {
    await page.goto(PATHS.knowledge);
    await expectTabCount(page, 1);
    await page.goto(PATHS.systemUser);
    await expect(page.locator('.app-tabbar-strip')).toBeVisible();
    await expectActiveTab(page, 'system_user');
    await expectTabCount(page, 2);
  });

  test('E2E-04 竞赛并存：竞赛 URL 直达 → 覆盖态 → 点其他菜单出标签', async ({ page }) => {
    await page.goto(PATHS.competitionEvents);
    // 覆盖态 shell 仍渲染标签栏，且竞赛菜单标签存在
    await expect(page.locator('.app-tabbar-strip')).toBeVisible();
    await expect(page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="competition_event"]`)).toBeVisible();
    // 从覆盖态点击普通菜单 → 退出覆盖 + 打开对应标签
    await clickMenuItem(page, MENU_NAMES.knowledgeFix);
    await expectActiveTab(page, 'knowledge_fix');
    expect(new URL(page.url()).pathname).toBe(PATHS.knowledgeFix);
  });

  test('E2E-05 覆盖态回退：竞赛 → 知识库 → 点回竞赛标签 → URL 级状态恢复', async ({ page }) => {
    await page.goto(PATHS.competitionEvents);
    await expect(page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="competition_event"]`)).toBeVisible();
    await clickMenuItem(page, MENU_NAMES.knowledgeFix);
    await expectActiveTab(page, 'knowledge_fix');
    // 点击携带 overridePath 的竞赛标签 → pushState+popstate → 覆盖态恢复
    await page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="competition_event"] .app-tab-label`).click();
    await expect(page.locator(`.app-tabbar-strip .ant-tabs-tab[data-node-key="competition_event"]`)).toHaveClass(/ant-tabs-tab-active/);
    expect(new URL(page.url()).pathname).toBe(PATHS.competitionEvents);
  });

  test('E2E-07 浏览器前进/后退在标签间切换', async ({ page }) => {
    await page.goto(PATHS.knowledge);
    await clickMenuItem(page, MENU_NAMES.knowledgeFix);
    await clickMenuItem(page, MENU_NAMES.systemUser);
    await expectActiveTab(page, 'system_user');
    await page.goBack();
    await expectActiveTab(page, 'knowledge_fix');
    await page.goForward();
    await expectActiveTab(page, 'system_user');
  });

  // E2E-08（登出清空标签）与 E2E-06（浏览器真实拖拽）按方案 §5.5/5.6 归人工/抽查项：
  // 登出会清掉共享登录态、拖拽依赖真实指针事件，不在本自动化 spec 内执行。
});

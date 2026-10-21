/**
 * 教材生成 e2e 主链路（《职业标准驱动教材生成·一期实施设计》v1.0 §4.4）。
 *
 * 运行：STAGE_E2E_MANAGE_SERVER=1 npx playwright test --config playwright.chrome.config.ts e2e/textbook-outline.spec.ts
 *
 * 说明：本 spec 用 page.route 模拟 SaaS 后端全部 /api/textbook/** 接口 + 登录态/菜单
 * （照 zhenti-past-papers.spec.ts 打桩模式），真实渲染 TextbookProjectPage / TextbookOutlinePage
 * （含隐藏路由 shell 覆盖态）。fixture 形状对齐 src/types/textbook.ts，数据手写（铝电解工×槽操作工×五级）。
 */
import { expect, test, type Page, type Route } from '@playwright/test';

// ------------------------------------------------------------------
// fixture（形状对齐 src/types/textbook.ts；雪花 ID 为字符串）
// ------------------------------------------------------------------

/** 铝电解工 parse-preview：10 方向、五级 40 学时、槽操作工方向映射 [1,2,12]（v1.0 §2.1 对拍基准） */
const PREVIEW = {
  workTypeTitle: '铝电解工',
  seriesNo: '6-17-02-02',
  standardVersion: '2020',
  directions: [
    '槽操作工',
    '吸出',
    '多功能机组',
    '物料输送',
    '计算机监控',
    '熔铸',
    '炭极',
    '分析检测',
    '筑炉',
    '综合',
  ],
  directionLevels: {
    槽操作工: [5],
    吸出: [5, 4],
    多功能机组: [5, 4],
    物料输送: [5, 4],
    计算机监控: [5, 4, 3],
    熔铸: [5, 4, 3],
    炭极: [5, 4, 3],
    分析检测: [5, 4, 3],
    筑炉: [5, 4, 3, 2],
    综合: [5, 4, 3, 2, 1],
  },
  trainingHoursByLevel: { 5: 40, 4: 36, 3: 32, 2: 28, 1: 24 },
  guessedFunctionNos: [1, 2, 12],
  warnings: [],
};

const BALANCED = { chapterSum: 100000, totalWords: 100000, balanced: true, unbalancedChapters: [] };
const UNBALANCED = {
  chapterSum: 103000,
  totalWords: 100000,
  balanced: false,
  unbalancedChapters: ['职业道德'],
};

type Project = {
  id: string;
  workTypeTitle: string;
  seriesNo: string;
  standardVersion: string;
  fileId: string;
  direction: string;
  levelCode: number;
  functionNos: number[];
  trainingHours: number;
  wordsPerHour: number;
  totalWords: number;
  status: string;
  parseWarnings: string | null;
  updatedAt: string;
  summary?: unknown;
};

function project(overrides: Partial<Project> = {}): Project {
  return {
    id: '8001',
    workTypeTitle: '铝电解工',
    seriesNo: '6-17-02-02',
    standardVersion: '2020',
    fileId: '6001',
    direction: '槽操作工',
    levelCode: 5,
    functionNos: [1, 2, 12],
    trainingHours: 40,
    wordsPerHour: 2500,
    totalWords: 100000,
    status: 'outlined',
    parseWarnings: null,
    updatedAt: '2026-08-21 10:00:00',
    ...overrides,
  };
}

const PROJECT_8001 = project();
const PROJECT_8002 = project({ id: '8002', updatedAt: '2026-08-21 11:00:00', summary: BALANCED });

/**
 * 五级/槽操作工大纲（扁平）：
 * 篇 = 基本要求/工作要求；章 = 职业道德(2.1,5%)、基础知识(2.2,10%)、工作准备(1,15%)、
 * 铝电解槽操作(2,40%)、职业功能12(12,30%)——只含方向映射 [1,2,12] 的章，无其他方向职业功能。
 * 章字数合计 5000+10000+15000+40000+30000 = 100000 = totalWords（配比守恒）。
 */
const OUTLINE = [
  { id: '100', parentId: '0', nodeLevel: 1, dimension: null, sourceNo: null, titleRaw: '基本要求', title: '基本要求', weight: null, targetWords: null, wordsManual: 0, sortNo: 1 },
  { id: '101', parentId: '0', nodeLevel: 1, dimension: null, sourceNo: null, titleRaw: '工作要求', title: '工作要求', weight: null, targetWords: null, wordsManual: 0, sortNo: 2 },
  { id: '200', parentId: '100', nodeLevel: 2, dimension: null, sourceNo: '2.1', titleRaw: '职业道德', title: '职业道德', weight: 5, targetWords: 5000, wordsManual: 0, sortNo: 1 },
  { id: '201', parentId: '100', nodeLevel: 2, dimension: null, sourceNo: '2.2', titleRaw: '基础知识', title: '基础知识', weight: 10, targetWords: 10000, wordsManual: 0, sortNo: 2 },
  { id: '202', parentId: '101', nodeLevel: 2, dimension: null, sourceNo: '1', titleRaw: '工作准备', title: '工作准备', weight: 15, targetWords: 15000, wordsManual: 0, sortNo: 1 },
  { id: '203', parentId: '101', nodeLevel: 2, dimension: null, sourceNo: '2', titleRaw: '铝电解槽操作', title: '铝电解槽操作', weight: 40, targetWords: 40000, wordsManual: 0, sortNo: 2 },
  { id: '204', parentId: '101', nodeLevel: 2, dimension: null, sourceNo: '12', titleRaw: '职业功能12', title: '职业功能12', weight: 30, targetWords: 30000, wordsManual: 0, sortNo: 3 },
  { id: '300', parentId: '200', nodeLevel: 3, dimension: null, sourceNo: '2.1.1', titleRaw: '职业道德基本知识', title: '职业道德基本知识', weight: null, targetWords: 3000, wordsManual: 0, sortNo: 1 },
  { id: '301', parentId: '200', nodeLevel: 3, dimension: null, sourceNo: '2.1.2', titleRaw: '职业守则', title: '职业守则', weight: null, targetWords: 2000, wordsManual: 0, sortNo: 2 },
  { id: '302', parentId: '201', nodeLevel: 3, dimension: null, sourceNo: '2.2.1', titleRaw: '安全文明生产知识', title: '安全文明生产知识', weight: null, targetWords: 4000, wordsManual: 0, sortNo: 1 },
  { id: '303', parentId: '202', nodeLevel: 3, dimension: null, sourceNo: '1.1', titleRaw: '操作前准备', title: '操作前准备', weight: null, targetWords: 8000, wordsManual: 0, sortNo: 1 },
  { id: '304', parentId: '203', nodeLevel: 3, dimension: null, sourceNo: '2.1', titleRaw: '槽况判断', title: '槽况判断', weight: null, targetWords: 15000, wordsManual: 0, sortNo: 1 },
  { id: '310', parentId: '304', nodeLevel: 4, dimension: 'theory', sourceNo: '2.1.1.1', titleRaw: '电解工艺参数', title: '电解工艺参数', weight: null, targetWords: null, wordsManual: 0, sortNo: 1 },
  { id: '311', parentId: '304', nodeLevel: 4, dimension: 'skill', sourceNo: '2.1.1.2', titleRaw: '换极操作', title: '换极操作', weight: null, targetWords: null, wordsManual: 0, sortNo: 2 },
];

const MENU = [
  {
    id: 1,
    parentId: 0,
    menuType: 'directory',
    code: 'zhi_neng_ke_cheng_cai_liao_guan_li',
    name: '智能课程材料管理',
    path: '/zhi-neng-ke-cheng-cai-liao-guan-li',
    component: '',
    permissionCode: '',
    icon: '',
    sortOrder: 1,
    children: [
      {
        id: 2,
        parentId: 1,
        menuType: 'page',
        code: 'textbook_project',
        name: '教材生成管理',
        path: '/jiao-cai-sheng-cheng',
        component: 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookProjectPage',
        permissionCode: 'textbook:project:view',
        icon: 'BookOutlined',
        sortOrder: 5,
        children: [],
      },
    ],
  },
];

function ok(data: unknown) {
  return { code: '0', message: 'OK', data, traceId: 'e2e' };
}

// ------------------------------------------------------------------
// 后端打桩：登录态/菜单 + 全部 /api/textbook/**
// ------------------------------------------------------------------

async function mockBackend(page: Page, state: { putCount: number }) {
  const ctx = page.context();

  await ctx.route('**/api/auth/me', (route: Route) =>
    route.fulfill({
      json: ok({
        id: 1,
        tenantId: 10159,
        username: 'admin',
        displayName: '管理员',
        userType: 'admin',
        roles: ['PLATFORM_ADMIN'],
        permissions: ['textbook:project:view', 'textbook:project:manage', 'textbook:project:export'],
      }),
    }),
  );
  await ctx.route('**/api/auth/menus', (route) => route.fulfill({ json: ok(MENU) }));
  // 登录表单用到的租户列表（后端不在线，打桩避免代理报错噪声）
  await ctx.route('**/api/auth/tenants', (route) => route.fulfill({ json: ok([]) }));

  // 全部 /api/textbook/** 单点分发（按 path+method；比多条 pattern 省去 playwright 路由优先级烦恼）
  await ctx.route('**/api/textbook/**', (route: Route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();

    // 新建向导：同步解析预览（multipart，不落库）
    if (path.endsWith('/projects/parse-preview')) {
      return route.fulfill({ json: ok(PREVIEW) });
    }
    // 导出：blob 下载（UTF-8 文件名，照 writeFile 口径）
    if (path.endsWith('/export')) {
      return route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'text/markdown',
          'Content-Disposition': `attachment; filename*=UTF-8''${encodeURIComponent('教材目录_铝电解工_槽操作工_五级-初级工_20260821.md')}`,
        },
        body: '# 铝电解工-槽操作工（五级/初级工）教材目录\n',
      });
    }
    if (path.endsWith('/projects/rebuild') || /\/projects\/\d+\/rebuild$/.test(path)) {
      return route.fulfill({ json: ok(BALANCED) });
    }
    if (/\/projects\/[^/]+\/outline$/.test(path)) {
      return route.fulfill({ json: ok(OUTLINE) });
    }
    // 列表 / 创建
    if (path === '/api/textbook/projects') {
      if (method === 'POST') {
        return route.fulfill({ json: ok(PROJECT_8002) });
      }
      return route.fulfill({
        json: ok({ items: [PROJECT_8001], page: 1, pageSize: 20, total: 1 }),
      });
    }
    // 详情 / 编辑 / 删除
    if (/\/projects\/[^/]+$/.test(path)) {
      if (method === 'GET') {
        return route.fulfill({ json: ok({ ...PROJECT_8002, summary: BALANCED }) });
      }
      if (method === 'PUT') {
        return route.fulfill({ json: ok(PROJECT_8002) });
      }
      return route.fulfill({ json: ok(null) });
    }
    // 大纲节点：改字数/排序（返回合计校验）；增删
    if (/\/outline\/nodes\/[^/]+$/.test(path)) {
      if (method === 'PUT') {
        state.putCount += 1;
        // 第 1 次改字数 → 不平衡（红）；第 2 次（恢复/再调整）→ 平衡（绿），验证校验条双向翻转
        return route.fulfill({ json: ok(state.putCount % 2 === 1 ? UNBALANCED : BALANCED) });
      }
      return route.fulfill({ json: ok(null) });
    }
    if (path === '/api/textbook/outline/nodes') {
      return route.fulfill({ json: ok(OUTLINE[OUTLINE.length - 1]) });
    }
    return route.fulfill({ status: 404, json: ok(null) });
  });
}

async function openPage(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('guoyu_admin_access_token', 'e2e-token');
    localStorage.setItem('guoyu_admin_tenant_id', '10159');
  });
  await page.goto('/jiao-cai-sheng-cheng');
  await expect(page.getByText('教材生成管理').first()).toBeVisible();
}

// ------------------------------------------------------------------
// 主链路：§4.4 一条（列表 → 新建向导 → 大纲 → 改字数合计校验 → 导出）
// ------------------------------------------------------------------

test('教材生成主链路：列表 → 新建向导 → 大纲编辑（改字数/合计校验）→ 导出 Markdown', async ({ page }) => {
  test.setTimeout(120_000);
  const state = { putCount: 0 };
  await mockBackend(page, state);
  await openPage(page);

  // ① 列表页渲染：分页返回 1 条「铝电解工-槽操作工-五级」工程
  const row = page.locator('.ant-table-row', { hasText: '铝电解工' }).first();
  await expect(row).toContainText('槽操作工');
  await expect(row).toContainText('五级/初级工');
  await expect(row).toContainText('100000');
  await expect(row).toContainText('大纲已生成');

  // ② 新建向导：上传 docx → parse-preview 回填 → 选方向×等级 → 提交创建
  await page.getByRole('button', { name: /新建教材工程/ }).click();
  const dialog = page.locator('.ant-modal').filter({ has: page.getByText('新建教材工程') });
  await dialog.locator('input[type="file"]').setInputFiles({
    name: '铝电解工.docx',
    mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    buffer: Buffer.from('e2e-docx-bytes'),
  });
  // parse-preview 回填：职业名称/编码/版本
  await expect(dialog.getByText('6-17-02-02')).toBeVisible();
  await expect(dialog.getByText('2020')).toBeVisible();

  await dialog.getByRole('button', { name: '下一步', exact: true }).click();
  // 选方向：等级受 directionLevels 约束（槽操作工仅五级）、学时回填 40、职业功能预选 [1,2,12]
  await dialog.locator('.ant-select').first().click();
  await page.locator('.ant-select-item-option[title="槽操作工"]').click();
  await expect(dialog.locator('.ant-select').nth(1)).toContainText('五级/初级工');
  // 职业功能多选预选 guessedFunctionNos [1,2,12]（第 3 个 select 为 functionNos）
  await expect(dialog.locator('.ant-select').nth(2).locator('.ant-select-selection-item')).toHaveCount(3);

  await dialog.getByRole('button', { name: '下一步', exact: true }).click();
  // 确认信息：学时 40 × 系数 2500 = 总字数 100000
  await expect(dialog.getByText('预计总字数')).toBeVisible();
  await expect(dialog.getByText('100000', { exact: true })).toBeVisible();

  await dialog.getByRole('button', { name: /创建并生成大纲/ }).click();

  // ③ 创建成功进入大纲页（隐藏路由覆盖态）：骨架章集合正确、无其他方向职业功能
  await expect(page).toHaveURL(/\/textbook\/outline\/8002/);
  await expect(page.getByText('铝电解工（槽操作工 · 五级/初级工）')).toBeVisible();
  await expect(page.getByText('合计校验通过')).toBeVisible();
  for (const chapter of ['职业道德', '基础知识', '工作准备', '铝电解槽操作', '职业功能12']) {
    await expect(page.getByText(chapter, { exact: true })).toBeVisible();
  }
  // 点级维度 Tag + 无其他方向职业功能章（吸出=功能3 / 多功能机组=功能4 不属于 [1,2,12]）
  await expect(page.getByText('理论').first()).toBeVisible();
  await expect(page.getByText('技能').first()).toBeVisible();
  await expect(page.getByText('铝液吸出')).toHaveCount(0);
  await expect(page.getByText('多功能机组操作')).toHaveCount(0);

  // ④ 行内改节字数：第 1 次 PUT 返回不平衡 → 校验条变红并列出章节
  const sectionRow = page.locator('tr', { hasText: '职业道德基本知识' });
  const wordInput = sectionRow.locator('input').first();
  await expect(wordInput).toHaveValue('3000');
  await wordInput.fill('6000');
  await wordInput.blur();
  await expect(page.getByText(/合计不平衡/)).toBeVisible();
  await expect(page.getByText(/不平衡章节：职业道德/)).toBeVisible();

  // 再改一次（mock 返回 balanced=true）→ 校验条恢复绿色
  const otherSection = page.locator('tr', { hasText: '安全文明生产知识' });
  const otherInput = otherSection.locator('input').first();
  await otherInput.fill('4500');
  await otherInput.blur();
  await expect(page.getByText('合计校验通过')).toBeVisible();

  // ⑤ 导出 Markdown：blob 下载，文件名来自 Content-Disposition
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: /导\s*出/ }).click();
  await page.locator('.ant-dropdown-menu-item', { hasText: 'Markdown' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('教材目录_铝电解工_槽操作工');
});

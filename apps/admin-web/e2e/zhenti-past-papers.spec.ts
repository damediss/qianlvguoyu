/**
 * 历年真题管理端 e2e（方案 §5.4 三条关键路径）。
 *
 * 运行：STAGE_E2E_MANAGE_SERVER=1 npx playwright test --config playwright.chrome.config.ts e2e/zhenti-past-papers.spec.ts
 *
 * 说明：本 spec 用 page.route 模拟 SaaS 后端（登录态/考试项目/真题接口/F1 文件流），
 * 但真实渲染管理端页面与 public/pdf-viewer/viewer.html，验证前端交互与 viewer 可用性。
 */
import { expect, test, type Page, type Route } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

type Record = {
  id: string;
  examId: string;
  title: string;
  year: string | null;
  fileSize: number;
  sort: number;
  status: number;
  metaPending: number;
  createdBy: string | null;
  createdById: string | null;
  createdAt: string;
};

type ImportRow = {
  fileName: string;
  ok: boolean;
  id?: string;
  title?: string;
  year?: string | null;
  reason?: string;
  pending?: boolean;
};

const PDF = readFileSync(join(process.cwd(), 'e2e', 'fixtures', 'sample.pdf'));

const MENU = [
  {
    id: 1,
    parentId: 0,
    menuType: 'directory',
    code: 'ti_sheng_kao_shi_guan_li',
    name: '提升/考试管理',
    path: '/ti-sheng-kao-shi-guan-li',
    component: '',
    permissionCode: '',
    icon: '',
    sortOrder: 1,
    children: [
      {
        id: 2,
        parentId: 1,
        menuType: 'page',
        code: 'exam_past_papers',
        name: '历年真题管理',
        path: '/exam/past-papers',
        component: 'exam/PastPaperManagePage',
        permissionCode: 'exam:zhenti:view',
        icon: '',
        sortOrder: 2,
        children: [],
      },
    ],
  },
];

function ok(data: unknown) {
  return { code: '0', message: 'OK', data, traceId: 'e2e' };
}

function baseRecord(overrides: Partial<Record> = {}): Record {
  return {
    id: '9001',
    examId: '100',
    title: '铝电解工（铝及铝合金熔铸工）五级理论科目',
    year: '2024',
    fileSize: 1835008,
    sort: 0,
    status: 1,
    metaPending: 0,
    createdBy: '张三',
    createdById: '1',
    createdAt: '2026-10-18 10:00:00',
    ...overrides,
  };
}

async function mockBackend(page: Page, state: { records: Record[] }) {
  // 使用 context 级路由：预览是 popup（新 page），page 级路由不会继承
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
        permissions: ['exam:zhenti:view', 'exam:zhenti:manage'],
      }),
    }),
  );
  await ctx.route('**/api/auth/menus', (route) => route.fulfill({ json: ok(MENU) }));
  await ctx.route('**/api/training/exam-lists**', (route) =>
    route.fulfill({
      json: ok({ items: [{ id: 100, title: '铝电解工五级' }], total: 1, page: 1, pageSize: 20 }),
    }),
  );
  await ctx.route('**/api/appzhenti/admin/list', (route) =>
    route.fulfill({
      json: ok({
        records: state.records,
        total: state.records.length,
        page: 1,
        size: 20,
        years: ['2024'],
      }),
    }),
  );
  await ctx.route('**/api/appzhenti/list', (route) =>
    route.fulfill({
      json: ok({
        records: state.records
          .filter((item) => item.status === 1)
          .map((item) => ({ id: item.id, title: item.title, year: item.year, fileSize: item.fileSize })),
        total: state.records.length,
        page: 1,
        size: state.records.length,
      }),
    }),
  );
  await ctx.route('**/api/appzhenti/admin/upload', async (route) => {
    const record = baseRecord({ id: '9001' });
    state.records = [record];
    await route.fulfill({ json: ok({ id: record.id, title: record.title, year: record.year, metaPending: 0 }) });
  });
  await ctx.route('**/api/appzhenti/admin/preview', async (route) => {
    const fileUrl = `/api/appzhenti/file/9001?sign=e2e-sign&t=9999999999`;
    await route.fulfill({
      json: ok({
        viewerUrl: `/pdf-viewer/viewer.html?file=${encodeURIComponent(fileUrl)}`,
        title: '铝电解工（铝及铝合金熔铸工）五级理论科目',
      }),
    });
  });
  await ctx.route('**/api/appzhenti/file/**', (route) =>
    route.fulfill({ status: 200, contentType: 'application/pdf', body: PDF }),
  );
  await ctx.route('**/api/appzhenti/admin/batchImport', (route) =>
    route.fulfill({
      json: ok({
        mode: 'sync',
        results: [
          { fileName: '铝电解工五级理论科目-2024.pdf', ok: true, id: '9101', title: '铝电解工五级理论科目', year: '2024' },
          { fileName: '铝电解工五级实操科目-2023.pdf', ok: true, id: '9102', title: '铝电解工五级实操科目', year: '2023' },
          {
            fileName: '无年份真题.pdf',
            ok: false,
            pending: true,
            id: '9103',
            title: '无年份真题',
            year: null,
            reason: '无法从文件名解析年份或工种等级，请补录标题与年份',
          },
        ] satisfies ImportRow[],
      }),
    }),
  );
  await ctx.route('**/api/appzhenti/admin/update', async (route) => {
    const payload = route.request().postDataJSON() as { id: string; title?: string; year?: string };
    state.records = state.records.map((item) =>
      item.id === payload.id
        ? { ...item, title: payload.title ?? item.title, year: payload.year ?? item.year, metaPending: 0 }
        : item,
    );
    await route.fulfill({ json: ok({ id: payload.id, title: payload.title, year: payload.year, metaPending: 0 }) });
  });
  await ctx.route('**/api/appzhenti/admin/delete', async (route) => {
    const payload = route.request().postDataJSON() as { id: string };
    state.records = state.records.filter((item) => item.id !== payload.id);
    await route.fulfill({ json: ok(null) });
  });
  await ctx.route('**/api/appzhenti/admin/importLog', (route) =>
    route.fulfill({
      json: ok({ importId: '77', status: 'done', total: 3, finished: 3, results: [] }),
    }),
  );
}

async function openPage(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('guoyu_admin_access_token', 'e2e-token');
    localStorage.setItem('guoyu_admin_tenant_id', '10159');
  });
  await page.goto('/exam/past-papers');
  await expect(page.getByText('历年真题管理').first()).toBeVisible();
}

async function selectExam(page: Page) {
  await page.locator('.ant-select').first().click();
  await page.locator('.ant-select-item-option[title="铝电解工五级"]').click();
}

test.describe('历年真题管理', () => {
  test('路径1：上传 PDF → 列表可见 → 预览打开 viewer（无下载/打印）', async ({ page }) => {
    const state = { records: [] as Record[] };
    await mockBackend(page, state);
    await openPage(page);

    // 未选考试项目：空态 + 操作禁用
    await expect(page.getByText('请先选择考试项目')).toBeVisible();
    state.records = [baseRecord()];
    await selectExam(page);
    await expect(page.locator('.ant-table-row').first()).toContainText('铝电解工（铝及铝合金熔铸工）五级理论科目');

    await page.getByRole('button', { name: /上传 PDF/ }).click();
    await page.locator('.ant-modal input[type="file"]').setInputFiles({
      name: '铝电解工（铝及铝合金熔铸工）五级理论科目-2024.pdf',
      mimeType: 'application/pdf',
      buffer: PDF,
    });
    await page.locator('.ant-modal').getByRole('button', { name: /^上\s*传$/ }).click();
    await expect(page.getByText('上传成功')).toBeVisible();
    await expect(page.locator('.ant-table-row').first()).toContainText('铝电解工（铝及铝合金熔铸工）五级理论科目');

    const popupPromise = page.waitForEvent('popup');
    await page.getByRole('button', { name: /预\s*览/ }).click();
    const viewer = await popupPromise;
    await expect(viewer.locator('#toolbar')).toBeVisible();
    await expect(viewer.locator('#pageInfo')).not.toHaveText('- / -', { timeout: 30000 });

    // 仅在线预览：页面内无下载/打印入口
    expect(await viewer.getByText('下载').count()).toBe(0);
    expect(await viewer.getByText('打印').count()).toBe(0);
    await viewer.close();
  });

  test('路径2：批量导入 2 成功 1 解析失败 → 失败行补录重试成功', async ({ page }) => {
    const state = { records: [] as Record[] };
    await mockBackend(page, state);
    await openPage(page);
    state.records = [baseRecord()];
    await page.locator('.ant-select').first().click();
    await page.locator('.ant-select-item-option[title="铝电解工五级"]').click();

    await page.getByRole('button', { name: /批量导入/ }).click();
    await page.locator('.ant-modal input[type="file"]').setInputFiles({
      name: '真题批次.zip',
      mimeType: 'application/zip',
      buffer: Buffer.from('PK\u0003\u0004e2e'),
    });
    await page.getByRole('button', { name: '开始导入' }).click();

    await expect(page.getByText('铝电解工五级理论科目-2024.pdf')).toBeVisible();
    await expect(page.getByText('无年份真题.pdf')).toBeVisible();
    await expect(page.getByText('待补录')).toBeVisible();
    await expect(page.getByText('成功')).toHaveCount(2);

    const yearInput = page.locator('.ant-modal input[placeholder="年份"]');
    await yearInput.fill('2024');
    await page.getByRole('button', { name: '补录后重试' }).click();

    await expect(page.getByText('待补录')).toHaveCount(0);
    await expect(page.getByText('成功')).toHaveCount(3);
  });

  test('路径3：删除（二次确认）→ 列表移除 → App A1 不再返回', async ({ page }) => {
    const state = { records: [baseRecord()] };
    await mockBackend(page, state);
    await openPage(page);

    await page.locator('.ant-select').first().click();
    await page.locator('.ant-select-item-option[title="铝电解工五级"]').click();
    await expect(page.locator('.ant-table-row').first()).toContainText('铝电解工（铝及铝合金熔铸工）五级理论科目');

    await page.getByRole('button', { name: /删\s*除/ }).click();
    await expect(page.getByText('删除后学员端不可见，且不可恢复（软删保留 90 天）')).toBeVisible();
    await page.locator('.ant-popconfirm-buttons .ant-btn-dangerous').click();

    await expect(page.getByText('该考试项目暂未导入真题')).toBeVisible();

    const appRecords = await page.evaluate(async () => {
      const response = await fetch('/api/appzhenti/list', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Tenant-Id': '10159',
          Authorization: 'Bearer e2e',
        },
        body: JSON.stringify({ examId: 100 }),
      });
      const payload = await response.json();
      return payload.data.records as unknown[];
    });
    expect(appRecords).toHaveLength(0);
  });
});

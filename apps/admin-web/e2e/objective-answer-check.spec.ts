/**
 * 客观题答案 AI 检查 e2e（方案 §16.4）。
 *
 * 运行：STAGE_E2E_MANAGE_SERVER=1 npx playwright test --config playwright.chrome.config.ts \
 *   e2e/objective-answer-check.spec.ts
 *
 * 用 context 级 route 模拟后端（登录态/菜单/筛选项/试题/预览/批次/明细/复核/导出），
 * 真实渲染管理端页面，验证菜单入口、选择→预览→创建→结果→详情→复核→导出主链路，
 * 以及无 submit 权限时创建入口不可见。
 */
import { expect, test, type Page, type Route } from '@playwright/test';

const BATCH_ID = '2100000000000000001';
const ITEM_ID = '2100000000000000002';

function ok(data: unknown) {
  return { code: '0', message: 'OK', data, traceId: 'e2e' };
}

const ALL_PERMISSIONS = [
  'exam:objective-answer-check:view',
  'exam:objective-answer-check:submit',
  'exam:objective-answer-check:review',
  'exam:objective-answer-check:export',
];

const MENU = [
  {
    id: 1,
    parentId: 0,
    menuType: 'directory',
    code: 'ke_cheng_guan_li',
    name: '课程管理',
    path: '/ke-cheng-guan-li',
    component: '',
    permissionCode: '',
    icon: '',
    sortOrder: 1,
    children: [
      {
        id: 2,
        parentId: 1,
        menuType: 'directory',
        code: 'zi_yuan_guan_li',
        name: '资源管理',
        path: '/resources',
        component: '',
        permissionCode: '',
        icon: '',
        sortOrder: 2,
        children: [
          {
            id: 3,
            parentId: 2,
            menuType: 'page',
            code: 'objective_answer_check',
            name: '客观题答案AI检查',
            path: '/question-bank/resources/objective-answer-check',
            component: 'question_bank/ObjectiveAnswerCheckPage',
            permissionCode: 'exam:objective-answer-check:view',
            icon: '',
            sortOrder: 6,
            children: [],
          },
        ],
      },
    ],
  },
];

const QUESTION = {
  detailId: 101,
  questionType: 'single',
  questionTypeLabel: '单选题',
  stemPreview: '海洋是什么颜色的？',
  originalAnswer: 'B. 蓝色',
  source: 0,
  sourceLabel: '内部',
  workTypeId: '143',
  workTypeTitle: '铝及铝合金熔铸工',
  levelCode: 4,
  levelLabel: '四级/中级工',
  courseId: 9,
  courseTitle: '铝电解工中级',
  invalidReason: null,
  contentQuality: 'clean',
};

const QUESTION_MULTI = {
  ...QUESTION,
  detailId: 102,
  questionType: 'multi',
  questionTypeLabel: '多选题',
  stemPreview: '以下哪些属于正确项？',
  originalAnswer: 'A. 甲；B. 乙',
};

const QUESTION_JUDGE = {
  ...QUESTION,
  detailId: 103,
  questionType: 'judge',
  questionTypeLabel: '判断题',
  stemPreview: '地球比太阳大',
  originalAnswer: '错误',
};


const ITEM = {
  id: ITEM_ID,
  batchId: BATCH_ID,
  detailId: 101,
  questionType: 'single',
  questionTypeLabel: '单选题',
  stemPreview: '海洋是什么颜色的？',
  originalAnswer: 'B. 蓝色',
  aiAnswer: 'C. 1.2E+3',
  resultCode: 'mismatch_high',
  contentQuality: 'suspect',
  contentWarnings: [
    {
      ruleCode: 'EXCEL_SCIENTIFIC_NOTATION_SUSPECT',
      severity: 'suspect',
      location: 'option',
      optionId: 'opt_2',
      matchedText: '1.2E+3',
      suggestedText: '1200',
      message: '该处疑似被 Excel 转成了科学计数法，请核对原始数值',
    },
  ],
  needsReview: true,
  consistent: false,
  referenceConfidence: 0.06,
  aiConfidence: 0.91,
  minMargin: 0.78,
  decisionWarning: null,
  retryable: false,
  attempts: 1,
  version: 3,
  reviewStatus: 'unreviewed',
  reviewNote: null,
  checkedAt: '2026-10-28T10:01:00',
  updatedAt: '2026-10-28T10:01:00',
};

const BATCH = {
  id: BATCH_ID,
  status: 'done',
  selectionMode: 'ids',
  filterSummary: '铝及铝合金熔铸工 / 四级/中级工',
  total: 3,
  done: 3,
  pending: 0,
  processing: 0,
  consistentHigh: 0,
  mismatchHigh: 1,
  lowConfidence: 0,
  consistentHigh: 2,
  consistentHigh: 2,
  invalid: 0,
  failed: 0,
  contentSuspect: 1,
  contentInvalid: 0,
  expectedCalls: 1,
  callCount: 1,
  inputTokensExact: 100,
  inputTokensEstimated: 0,
  usageEstimatedCalls: 0,
  outputTokens: 0,
  modelId: 'ateve-jev-v1',
  promptVersion: 'objective-answer-v1',
  confidenceThreshold: 0.8,
  marginThreshold: 0.2,
  createdBy: '1',
  createdByName: '管理员',
  createdAt: '2026-10-28T10:00:00',
  startedAt: '2026-10-28T10:00:01',
  finishedAt: '2026-10-28T10:05:00',
  failReason: null,
  canPause: false,
  canResume: false,
  canRetryFailed: true,
};

const DETAIL = {
  item: ITEM,
  sourceLabel: '内部',
  workTypeTitle: '铝及铝合金熔铸工',
  courseTitle: '铝电解工中级',
  levelCode: 4,
  knowledgePath: '基本要求/安全知识',
  contentSnapshot: '<p>海洋是什么颜色的？</p>',
  stemText: '海洋是什么颜色的？',
  resolveSnapshot: '人工解析不发送给 AI',
  options: [
    {
      optionId: 'opt_1',
      label: 'B',
      text: '蓝色',
      originalCorrect: true,
      aiCorrect: false,
      choice: null,
      confidence: 0.91,
      referenceProbability: 0.06,
      top1Probability: 0.88,
      top2Probability: 0.06,
      margin: 0.82,
    },
    {
      optionId: 'opt_2',
      label: 'C',
      text: '1.2E+3',
      originalCorrect: false,
      aiCorrect: true,
      choice: 'opt_2',
      confidence: 0.91,
      referenceProbability: 0.06,
      top1Probability: 0.88,
      top2Probability: 0.06,
      margin: 0.82,
    },
  ],
  calls: [],
  sourceChanged: false,
  modelId: 'ateve-jev-v1',
  promptVersion: 'objective-answer-v1',
  requestedModel: 'ateve-jev-v1',
  responseModel: 'ateve-jev-v1',
  confidenceDefinition: 'maximum candidate probability',
  failReason: null,
};

async function mockBackend(page: Page, permissions: string[]) {
  const ctx = page.context();
  await page.addInitScript(() => {
    localStorage.setItem('guoyu_admin_access_token', 'e2e-token');
    localStorage.setItem('guoyu_admin_tenant_id', '10159');
  });
  await ctx.route('**/api/auth/me', (route: Route) =>
    route.fulfill({
      json: ok({
        id: 1,
        tenantId: 10159,
        username: 'admin',
        displayName: '管理员',
        userType: 'admin',
        roles: ['PLATFORM_ADMIN'],
        permissions,
      }),
    }),
  );
  await ctx.route('**/api/auth/menus', (route) => route.fulfill({ json: ok(MENU) }));
  await ctx.route('**/api/exam/objective-answer-check/filter-options**', (route) =>
    route.fulfill({
      json: ok({
        workTypes: [
          { id: '143', title: '铝及铝合金熔铸工', path: '铝电解工 / 铝及铝合金熔铸工' },
        ],
        levels: [{ code: 4, label: '四级/中级工' }],
        sources: [
          { value: 0, label: '内部' },
          { value: 1, label: '外部' },
          { value: 2, label: 'AI 生成' },
          { value: 3, label: '赛前复习' },
        ],
        types: [
          { value: 'single', label: '单选题' },
          { value: 'multi', label: '多选题' },
          { value: 'judge', label: '判断题' },
        ],
      }),
    }),
  );
  await ctx.route('**/api/exam/objective-answer-check/questions/page', (route) => {
    const body = route.request().postDataJSON() as { page: number };
    const items = body.page === 2 ? [QUESTION_JUDGE] : [QUESTION, QUESTION_MULTI];
    route.fulfill({ json: ok({ items, page: body.page, pageSize: 20, total: 21 }) });
  });
  await ctx.route('**/api/exam/objective-answer-check/selection/preview', (route) =>
    route.fulfill({
      json: ok({
        selectionCount: 3,
        overLimit: false,
        maxBatchSize: 2000,
        validCount: 3,
        invalidCount: 0,
        suspectContentCount: 1,
        invalidContentCount: 0,
        expectedCalls: 1,
        typeCounts: { single: 1, multi: 1, judge: 1 },
        invalidReasonCounts: [],
        modelId: 'ateve-jev-v1',
        confidenceThreshold: 0.8,
        marginThreshold: 0.2,
      }),
    }),
  );
  await ctx.route('**/api/exam/objective-answer-check/batches', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({ json: ok({ batchId: BATCH_ID }) });
    } else {
      await route.fulfill({ json: ok({ items: [BATCH], page: 1, pageSize: 10, total: 1 }) });
    }
  });
  await ctx.route('**/api/exam/objective-answer-check/batches/current', (route) =>
    route.fulfill({ json: ok(null) }),
  );
  await ctx.route(`**/api/exam/objective-answer-check/batches/${BATCH_ID}`, (route) =>
    route.fulfill({ json: ok(BATCH) }),
  );
  await ctx.route(
    `**/api/exam/objective-answer-check/batches/${BATCH_ID}/items?**`,
    (route) => route.fulfill({ json: ok({ items: [ITEM], page: 1, pageSize: 20, total: 1 }) }),
  );
  await ctx.route(
    `**/api/exam/objective-answer-check/batches/${BATCH_ID}/items/${ITEM_ID}`,
    (route) => route.fulfill({ json: ok(DETAIL) }),
  );
  await ctx.route(
    `**/api/exam/objective-answer-check/batches/${BATCH_ID}/items/${ITEM_ID}/review`,
    async (route) => {
      const body = route.request().postDataJSON();
      expect(body.expectedVersion).toBe(3);
      expect(body.reviewStatus).toBe('needs_edit');
      await route.fulfill({ json: ok(null) });
    },
  );
  await ctx.route(
    `**/api/exam/objective-answer-check/batches/${BATCH_ID}/export**`,
    (route) =>
      route.fulfill({
        status: 200,
        headers: {
          'Content-Disposition':
            "attachment; filename=\"check.xlsx\"; filename*=UTF-8''%E8%80%83%E6%A0%B8.xlsx",
          'Content-Type':
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        },
        body: Buffer.from('PK-e2e-xlsx'),
      }),
  );
}

test('管理员从侧栏进入，选择→预览→创建→结果→详情→复核→导出', async ({ page }) => {
  await mockBackend(page, ALL_PERMISSIONS);
  await page.goto('/');

  await page.getByText('课程管理').first().click();
  await page.getByText('资源管理').first().click();
  await page.getByText('客观题答案AI检查').first().click();

  await expect(page.getByText('AI 检查用于快速筛查，不能替代专家审定；检查不会修改题库答案。'))
    .toBeVisible();
  await expect(page.getByText('海洋是什么颜色的？')).toBeVisible();
  await expect(page.getByText('B. 蓝色')).toBeVisible();

  const pageOneChecks = page.locator('.ant-table-tbody .ant-checkbox-input');
  await pageOneChecks.nth(0).check();
  await pageOneChecks.nth(1).check();
  await expect(page.getByText('已逐题勾选 2 道')).toBeVisible();
  await page.locator('.ant-pagination-item-2').click();
  await expect(page.getByText('地球比太阳大')).toBeVisible();
  await page.locator('.ant-table-tbody .ant-checkbox-input').first().check();
  await expect(page.getByText('已逐题勾选 3 道')).toBeVisible();

  await page.getByRole('button', { name: '预览并开始 AI 检查' }).click();
  await expect(page.getByText('确认开始 AI 检查')).toBeVisible();
  await expect(page.getByText(/可直接检查/)).toBeVisible();
  await page.getByRole('button', { name: '开始检查' }).click();

  await expect(page.getByText(/批次 2100000000000000001/)).toBeVisible();
  await expect(page.getByText('高置信不一致').first()).toBeVisible();
  await expect(page.getByText('内容格式待核')).toBeVisible();

  await page.getByRole('button', { name: /详\s*情/ }).first().click();
  await expect(page.getByText('原解析（仅供人工查看，未发送给 AI）')).toBeVisible();
  await expect(page.getByText('人工解析不发送给 AI')).toBeVisible();
  await expect(page.getByText('该处疑似被 Excel 转成了科学计数法，请核对原始数值')).toBeVisible();
  await expect(page.getByText('1200')).toBeVisible();
  await expect(page.getByText(/采纳 AI 答案/)).toHaveCount(0);

  await page.getByRole('button', { name: '人工复核' }).click();
  await page.getByLabel('需要修改').check();
  await page.getByRole('button', { name: '保存意见' }).click();
  await expect(page.getByText('复核意见已保存')).toBeVisible();
  await page.locator('.ant-drawer-close').click();
  await expect(page.locator('.ant-drawer-content')).toBeHidden();

  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: /导\s*出/ }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('.xlsx');
});

test('无 submit 权限时创建入口不可见，直接请求仍由后端 403 兜底', async ({ page }) => {
  await mockBackend(page, ['exam:objective-answer-check:view']);
  await page.goto('/question-bank/resources/objective-answer-check');

  await expect(page.getByText('海洋是什么颜色的？')).toBeVisible();
  await expect(page.getByRole('button', { name: '预览并开始 AI 检查' })).toHaveCount(0);
});

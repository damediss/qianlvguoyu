/**
 * AI 课件生成管理 EU 级 E2E(方案 §15.5 EU-01~EU-08):route 全 mock 夹具 + 真实 UI 交互。
 * 运行:npm run test:e2e:courseware(或 STAGE_E2E_MANAGE_SERVER=1 自动起 dev server)
 */
import { expect, test, type Page } from '@playwright/test';
import { installCoursewareFixture, loginThroughUi } from './support/coursewareFixture';

// 内置 headless shell 未下载时统一走系统 Chrome(既有本地走查口径);
// ffmpeg 未安装,关闭录像(截图/文本断言不依赖)
test.use({ channel: 'chrome', video: 'off', trace: 'off' });

const OUTLINE_PAGE_4_ID_SUFFIX = '1003'; // 第 4 页(第 1 章内容页)

async function loginAndEnter(page: Page) {
  await loginThroughUi(page);
  // EU-01:侧栏出现 智能课程材料管理 > AI课件生成管理
  const parent = page.getByText('智能课程材料管理', { exact: true }).first();
  await expect(parent).toBeVisible();
  await parent.click();
  const item = page.getByText('AI课件生成管理', { exact: true });
  await expect(item).toBeVisible();
  await item.click();
  await expect(page.getByRole('heading', { name: 'AI 课件生成' })).toBeVisible();
}

test.describe('AI课件生成管理 EU', () => {
  test('EU-01 菜单可见并可进入页面', async ({ page }) => {
    await installCoursewareFixture(page);
    await loginAndEnter(page);
    await expect(page.getByTestId('courseware-create-btn')).toBeVisible();
  });

  test('EU-02 创建课件 → 阶段推进(正在整理素材)→ 大纲按章分组', async ({ page }) => {
    await installCoursewareFixture(page);
    await loginAndEnter(page);

    await page.getByTestId('courseware-create-btn').click();
    await page.setInputFiles('.ant-upload input[type="file"]', {
      name: '热处理标准.docx', mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      buffer: Buffer.from('PK-fixture-docx'),
    });
    await page.getByRole('button', { name: '开始生成大纲' }).click();

    // 阶段文案推进:正在解析文档/正在整理素材
    await expect(page.getByText(/正在解析文档|正在整理素材|正在生成大纲/).first()).toBeVisible();
    // 大纲就绪:按章分组(章标题平铺)
    await expect(page.getByText('确认课件大纲', { exact: false }).first()).toBeVisible({ timeout: 30000 });
    await expect(page.getByText('第 1 章:热处理基础')).toBeVisible();
    await expect(page.getByText('第 2 章:工艺流程')).toBeVisible();
    await expect(page.getByText('第 3 章:质量检验')).toBeVisible();
    // 固定页标识
    await expect(page.getByText('复习思考题')).toBeVisible();
  });

  test('EU-03 大纲编辑(改标题/增页/删页/调序)并确认生成', async ({ page }) => {
    const fixture = await installCoursewareFixture(page);
    await loginAndEnter(page);
    await page.getByTestId('courseware-create-btn').click();
    await page.setInputFiles('.ant-upload input[type="file"]', {
      name: '热处理标准.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      buffer: Buffer.from('PK-fixture-docx'),
    });
    await page.getByRole('button', { name: '开始生成大纲' }).click();
    await expect(page.getByText('确认课件大纲', { exact: false }).first()).toBeVisible({ timeout: 30000 });

    // 改标题(第 1 章内容页)
    const titleInput = page.getByTestId(`outline-title-21015160428247${OUTLINE_PAGE_4_ID_SUFFIX}`);
    await titleInput.fill('淬火温度决定硬度上限(改)');
    await titleInput.blur();
    await expect(page.getByText('已保存').first()).toBeVisible();

    // 本章加页 → 删除
    await page.getByTestId('chapter-add-1').click();
    const newCard = page.getByTestId('outline-title-210151604282472009');
    await expect(newCard).toBeVisible();
    await page.getByTestId('outline-delete-210151604282472009').click();
    await page.getByRole('button', { name: '确 定' }).click();
    await expect(newCard).toHaveCount(0);

    // 调序:第 1 章内容页下移一位
    const card = page.getByTestId('outline-card-210151604282471003');
    await card.getByRole('button').nth(1).click(); // ArrowDown

    // 确认生成 → 进度视图
    await page.getByTestId('outline-confirm-btn').click();
    await expect(page.getByText('页面与导出', { exact: false })).toBeVisible();
    expect(fixture.getExportCalls()).toBe(0);
  });

  test('EU-04 生成进度推进至全部终态后轮询停止', async ({ page }) => {
    const fixture = await installCoursewareFixture(page);
    await loginAndEnter(page);
    await page.getByTestId('courseware-create-btn').click();
    await page.setInputFiles('.ant-upload input[type="file"]', {
      name: '热处理标准.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      buffer: Buffer.from('PK-fixture-docx'),
    });
    await page.getByRole('button', { name: '开始生成大纲' }).click();
    await expect(page.getByText('确认课件大纲', { exact: false }).first()).toBeVisible({ timeout: 30000 });
    await page.getByTestId('outline-confirm-btn').click();

    // 全部页终态(9 个"已完成")
    await expect(page.getByText('已完成')).toHaveCount(9, { timeout: 30000 });
    // 终态后等待 4.2s(> 3s 轮询间隔),断言无新增 progress 请求
    await page.waitForTimeout(4200);
    expect(fixture.getProgressCallsAfterTerminal()).toBeLessThanOrEqual(1);
  });

  test('EU-05 失败页重试恢复', async ({ page }) => {
    await installCoursewareFixture(page, { failSlide8: true });
    await loginAndEnter(page);
    await page.getByTestId('courseware-create-btn').click();
    await page.setInputFiles('.ant-upload input[type="file"]', {
      name: '热处理标准.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      buffer: Buffer.from('PK-fixture-docx'),
    });
    await page.getByRole('button', { name: '开始生成大纲' }).click();
    await expect(page.getByText('确认课件大纲', { exact: false }).first()).toBeVisible({ timeout: 30000 });
    await page.getByTestId('outline-confirm-btn').click();

    // 第 8 页失败,出现重试按钮
    const retryBtn = page.getByTestId('slide-retry-210151604282473008');
    await expect(retryBtn).toBeVisible({ timeout: 30000 });
    await retryBtn.click();
    await expect(page.getByText('已重新生成该页')).toBeVisible();
    // 下一轮刷新后该页恢复已完成
    await expect(page.getByTestId('slide-view-210151604282473008')).toBeVisible();
  });

  test('EU-06 导出 PPTX 触发中文文件名下载', async ({ page }) => {
    const fixture = await installCoursewareFixture(page);
    await loginAndEnter(page);
    await page.getByTestId('courseware-create-btn').click();
    await page.setInputFiles('.ant-upload input[type="file"]', {
      name: '热处理标准.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      buffer: Buffer.from('PK-fixture-docx'),
    });
    await page.getByRole('button', { name: '开始生成大纲' }).click();
    await expect(page.getByText('确认课件大纲', { exact: false }).first()).toBeVisible({ timeout: 30000 });
    await page.getByTestId('outline-confirm-btn').click();
    await expect(page.getByText('已完成')).toHaveCount(9, { timeout: 30000 });

    const exportBtn = page.getByTestId('courseware-export-btn');
    await expect(exportBtn).toBeVisible();
    const downloadPromise = page.waitForEvent('download');
    await exportBtn.click();
    // warning 透出弹窗(antd info 弹窗默认按钮文案"知道了")
    const dialog = page.getByRole('dialog', { name: /导出成功,以下建议可在 PowerPoint 中检查后调整/ });
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('第 4 页 要点偏多,建议检查版面')).toBeVisible();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe('热处理工艺培训.pptx');
    await dialog.getByRole('button', { name: /知道了|确 定|确定/ }).click();
    expect(fixture.getExportCalls()).toBe(1);
  });

  test('EU-07 无权限账号菜单不可见', async ({ page }) => {
    await installCoursewareFixture(page, { noPermission: true });
    await loginThroughUi(page);
    await expect(page.getByText('AI课件生成管理')).toHaveCount(0);
  });

  test('EU-08 雪花 id 字符串全链路(查看页明细用精确保留的大 id)', async ({ page }) => {
    await installCoursewareFixture(page);
    await loginAndEnter(page);
    await page.getByTestId('courseware-create-btn').click();
    await page.setInputFiles('.ant-upload input[type="file"]', {
      name: '热处理标准.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      buffer: Buffer.from('PK-fixture-docx'),
    });
    await page.getByRole('button', { name: '开始生成大纲' }).click();
    await expect(page.getByText('确认课件大纲', { exact: false }).first()).toBeVisible({ timeout: 30000 });
    await page.getByTestId('outline-confirm-btn').click();
    await expect(page.getByText('已完成')).toHaveCount(9, { timeout: 30000 });

    // 查看第 3 页明细(抽屉):该页 id 为 19 位大数字字符串,若精度丢失会 404/不渲染
    await page.getByTestId('slide-view-210151604282473003').click();
    await expect(page.getByText('学习提示')).toBeVisible();
    await expect(page.getByText('本页重点是结论,易错点是概念辨析')).toBeVisible();
  });
});

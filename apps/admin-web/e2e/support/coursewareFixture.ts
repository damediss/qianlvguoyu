/**
 * AI 课件生成 EU 级 E2E 夹具(方案 §15.5):route 拦截全 mock,内存状态机模拟
 * 解析→整理素材→大纲→确认→页级生成→导出 全链路,不依赖真实后端。
 * 雪花 id 用大数字字符串(EU-08 精度口径)。
 */
import type { Page } from '@playwright/test';

const PID = '2101516042824790018';
const FID = '2101516042824790100';

type OutlinePage = {
  id: string; chapterNo: number; chapterTitle: string | null; position: number;
  title: string; objective: string; keyPoints: string[]; pageRole: string;
  layoutId: string; sourceRefs: string[]; fixed: boolean;
};

type Slide = {
  id: string; position: number; title: string; pageRole: string;
  status: 'pending' | 'generating' | 'ready' | 'failed'; error: string | null;
};

interface FixtureOptions {
  /** 无权限模式(EU-07):用户无 courseware 权限码,菜单不下发 */
  noPermission?: boolean;
  /** 让第 8 页首次生成失败(EU-05) */
  failSlide8?: boolean;
}

function outlineSeed(): OutlinePage[] {
  const pages: OutlinePage[] = [];
  const push = (p: Omit<OutlinePage, 'id' | 'position'>) =>
    pages.push({ ...p, id: `${PID.slice(0, 14)}${1000 + pages.length}`, position: pages.length + 1 });
  push({ chapterNo: 0, chapterTitle: null, title: '热处理工艺培训', objective: '',
    keyPoints: [], pageRole: 'cover', layoutId: 'cover', sourceRefs: [], fixed: true });
  push({ chapterNo: 0, chapterTitle: null, title: '目录', objective: '',
    keyPoints: ['热处理基础', '工艺流程', '质量检验'], pageRole: 'toc', layoutId: 'toc',
    sourceRefs: [], fixed: true });
  const chapters: Array<[number, string]> = [[1, '热处理基础'], [2, '工艺流程'], [3, '质量检验']];
  for (const [no, name] of chapters) {
    push({ chapterNo: no, chapterTitle: name, title: name, objective: '',
      keyPoints: [], pageRole: 'section', layoutId: 'section', sourceRefs: ['S1:1'], fixed: false });
    push({ chapterNo: no, chapterTitle: name, title: `${name}的核心结论页`,
      objective: '讲清楚', keyPoints: ['要点一内容充实具体', '要点二内容充实具体', '要点三内容充实具体'],
      pageRole: 'content', layoutId: 'content_bullets', sourceRefs: ['S1:1'], fixed: false });
  }
  push({ chapterNo: 0, chapterTitle: null, title: '复习思考题', objective: '',
    keyPoints: ['淬火与回火的区别是什么?', '为什么冷却速度影响硬度?', '抽检比例是多少?'],
    pageRole: 'review', layoutId: 'summary', sourceRefs: [], fixed: true });
  return pages;
}

export async function installCoursewareFixture(page: Page, options: FixtureOptions = {}) {
  const outline = outlineSeed();
  const slides: Slide[] = [];
  let projectStatus = 'parsing';
  let outlinePolls = 0;
  let genPolls = 0;
  let projectCreated = false;
  let confirmed = false;
  let failApplied = false;
  let terminalProgressCallsAtStop = -1;
  let progressCallsAfterTerminal = 0;
  let exportCalls = 0;

  const user = {
    id: 1000,
    tenantId: 10159,
    username: 'admin',
    displayName: '平台管理员',
    userType: 'admin',
    roles: ['PLATFORM_ADMIN'],
    permissions: options.noPermission ? [] : [
      'courseware:project:list', 'courseware:project:create', 'courseware:project:confirm',
      'courseware:project:generate', 'courseware:project:retry', 'courseware:project:export',
    ],
  };

  const menuTree = options.noPermission
    ? [{ id: 1, parentId: 0, menuType: 'directory', code: 'zhi_neng_ke_cheng_cai_liao',
        name: '智能课程材料管理', path: '', component: '', permissionCode: '', icon: '',
        sortOrder: 1, children: [] }]
    : [{ id: 1, parentId: 0, menuType: 'directory', code: 'zhi_neng_ke_cheng_cai_liao',
        name: '智能课程材料管理', path: '', component: '', permissionCode: '', icon: '',
        sortOrder: 1, children: [
          { id: 2, parentId: 1, menuType: 'menu', code: 'ai_courseware_gen',
            name: 'AI课件生成管理', path: '/ai-ke-jian-sheng-cheng',
            component: 'zhi_neng_ke_cheng_cai_liao_guan_li/AiCoursewarePage',
            permissionCode: 'courseware:project:list', icon: 'FilePptOutlined', sortOrder: 4,
            children: [] },
        ] }];

  const ok = (data: unknown) => ({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ code: '0', message: 'OK', data, traceId: 'courseware-fixture' }),
  });

  const projectVO = () => ({
    id: PID, title: '热处理工艺培训', audience: '学员自学', contentDensity: 'medium',
    status: projectStatus,
    totalPages: slides.length, readyPages: slides.filter((s) => s.status === 'ready').length,
    failedPages: slides.filter((s) => s.status === 'failed').length,
    error: null, createdAt: '2026-09-20 10:00:00',
  });

  const transparentPng = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/api/, '');
    const method = request.method();

    // ── 认证 ──
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
        accessToken: 'courseware-fixture-token', refreshToken: 'r', expiresIn: 3600,
        tenantId: 10159, user,
      }));
    }
    if (path === '/auth/me') return route.fulfill(ok(user));
    if (path === '/auth/menus') return route.fulfill(ok(menuTree));

    // ── 文件上传/下载 ──
    if (path === '/files' && method === 'POST') {
      return route.fulfill(ok({
        id: FID, originalName: '热处理标准.docx', sizeBytes: 12345,
        contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        accessPolicy: 'private', url: '',
      }));
    }
    if (path === `/files/${FID}` && method === 'GET') {
      return route.fulfill(ok({ id: FID, originalName: '热处理工艺培训.pptx', accessPolicy: 'private', url: '' }));
    }
    if (path === `/files/${FID}/download`) {
      return route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
          'Content-Disposition': "attachment; filename*=UTF-8''%E7%83%AD%E5%A4%84%E7%90%86%E5%B7%A5%E8%89%BA%E5%9F%B9%E8%AE%AD.pptx",
        },
        body: Buffer.from('PK-fixture-pptx-bytes'),
      });
    }

    // ── 课件项目 ──
    if (path === '/courseware/projects' && method === 'POST') {
      projectCreated = true;
      projectStatus = 'parsing';
      outlinePolls = 0;
      return route.fulfill(ok(projectVO()));
    }
    if (path === '/courseware/projects/page') {
      return route.fulfill(ok({
        items: projectCreated ? [projectVO()] : [], page: 1, pageSize: 20,
        total: projectCreated ? 1 : 0,
      }));
    }
    const projMatch = path.match(/^\/courseware\/projects\/(\d+)$/);
    if (projMatch && method === 'GET') {
      // 大纲前阶段按轮询推进:parsing → digesting → outline_generating → outline_ready
      if (projectCreated && !confirmed) {
        outlinePolls += 1;
        projectStatus = outlinePolls <= 1 ? 'parsing'
          : outlinePolls === 2 ? 'digesting'
          : outlinePolls === 3 ? 'outline_generating' : 'outline_ready';
      }
      return route.fulfill(ok(projectVO()));
    }
    if (path === `/courseware/projects/${PID}/outline` && method === 'GET') {
      return route.fulfill(ok(outline));
    }
    if (path === `/courseware/projects/${PID}/outline/pages` && method === 'POST') {
      const body = request.postDataJSON() as { chapterNo: number; title: string; keyPoints?: string[] };
      const chapterTitle = outline.find((p) => p.chapterNo === body.chapterNo)?.chapterTitle ?? null;
      const insertAfter = Math.max(...outline.filter((p) => p.chapterNo === body.chapterNo)
        .map((p) => p.position));
      outline.forEach((p) => { if (p.position >= insertAfter + 1) p.position += 1; });
      const created: OutlinePage = {
        id: `${PID.slice(0, 14)}${2000 + outline.length}`, chapterNo: body.chapterNo,
        chapterTitle, position: insertAfter + 1, title: body.title,
        objective: body.keyPoints?.join('') ?? '', keyPoints: body.keyPoints ?? [],
        pageRole: 'content', layoutId: 'content_bullets', sourceRefs: [], fixed: false,
      };
      outline.push(created);
      outline.sort((a, b) => a.position - b.position);
      return route.fulfill(ok(created));
    }
    const pageMatch = path.match(/^\/courseware\/outline\/pages\/(\d+)$/);
    if (pageMatch && method === 'PUT') {
      const body = request.postDataJSON() as Partial<OutlinePage>;
      const target = outline.find((p) => p.id === pageMatch[1]);
      if (target) Object.assign(target, body);
      return route.fulfill(ok(target));
    }
    if (pageMatch && method === 'DELETE') {
      const idx = outline.findIndex((p) => p.id === pageMatch[1]);
      if (idx >= 0) outline.splice(idx, 1);
      outline.forEach((p, i) => { p.position = i + 1; });
      return route.fulfill(ok(null));
    }
    if (path === `/courseware/projects/${PID}/outline/reorder` && method === 'POST') {
      const body = request.postDataJSON() as { orderedIds: string[] };
      body.orderedIds.forEach((id, i) => {
        const target = outline.find((p) => p.id === id);
        if (target) target.position = i + 1;
      });
      outline.sort((a, b) => a.position - b.position);
      return route.fulfill(ok(null));
    }
    if (path === `/courseware/projects/${PID}/confirm` && method === 'POST') {
      confirmed = true;
      projectStatus = 'generating';
      slides.length = 0;
      outline.forEach((p) => slides.push({
        id: `${PID.slice(0, 14)}${3000 + p.position}`, position: p.position,
        title: p.title, pageRole: p.pageRole, status: 'pending', error: null,
      }));
      return route.fulfill(ok(null));
    }
    if (path === `/courseware/projects/${PID}/progress`) {
      if (confirmed && !['ready', 'partial'].includes(projectStatus)) {
        genPolls += 1;
        const readyUpTo = genPolls * 3;
        slides.forEach((s, i) => {
          if (i < readyUpTo) {
            s.status = 'ready';
          } else if (i < readyUpTo + 2) {
            s.status = 'generating';
          }
        });
        if (options.failSlide8 && !failApplied && slides.length >= 8 && genPolls >= 2) {
          failApplied = true;
          slides[7].status = 'failed';
          slides[7].error = '页面生成失败,请重试';
        }
        const noneRunning = slides.every((s) => s.status === 'ready' || s.status === 'failed');
        if (noneRunning && slides.every((s) => s.status === 'ready')) {
          projectStatus = 'ready';
        } else if (noneRunning && slides.some((s) => s.status === 'failed')) {
          projectStatus = 'partial';
        }
      }
      if (['ready', 'partial'].includes(projectStatus)) {
        if (terminalProgressCallsAtStop < 0) {
          terminalProgressCallsAtStop = 0;
        } else {
          progressCallsAfterTerminal += 1;
        }
      }
      return route.fulfill(ok({
        status: projectStatus, totalPages: slides.length,
        readyPages: slides.filter((s) => s.status === 'ready').length,
        failedPages: slides.filter((s) => s.status === 'failed').length,
        error: projectStatus === 'partial' ? '部分页面生成失败,可在页列表单独重试' : null,
        slides,
      }));
    }
    const retryMatch = path.match(/^\/courseware\/slides\/(\d+)\/retry$/);
    if (retryMatch && method === 'POST') {
      const slide = slides.find((s) => s.id === retryMatch[1]);
      if (slide) {
        slide.status = 'ready';
        slide.error = null;
      }
      if (slides.every((s) => s.status === 'ready')) {
        projectStatus = 'ready';
      }
      return route.fulfill(ok(null));
    }
    const slideMatch = path.match(/^\/courseware\/slides\/(\d+)$/);
    if (slideMatch && method === 'GET') {
      const slide = slides.find((s) => s.id === slideMatch[1]);
      return route.fulfill(ok({
        id: slide?.id, position: slide?.position, title: slide?.title,
        pageRole: slide?.pageRole, layoutId: 'content_bullets',
        blocks: [
          { slotId: 'title', type: 'text', text: slide?.title },
          { slotId: 'body', type: 'bullets',
            items: ['要点一内容充实具体', '要点二内容充实具体', '要点三内容充实具体'] },
        ],
        speakerNotes: '本页重点是结论,易错点是概念辨析',
        issues: [], status: slide?.status, error: slide?.error, revision: 1,
      }));
    }
    if (path === `/courseware/projects/${PID}/issues`) {
      return route.fulfill(ok([]));
    }
    if (path === `/courseware/projects/${PID}/export` && method === 'POST') {
      exportCalls += 1;
      return route.fulfill(ok({
        blocked: false, issues: [], fileId: FID,
        warnings: [{ severity: 'warning', code: 'capacity', slotId: 'body', message: '第 4 页 要点偏多,建议检查版面' }],
      }));
    }
    if (path === `/courseware/projects/${PID}/cancel` && method === 'POST') {
      projectStatus = 'cancelled';
      return route.fulfill(ok(null));
    }

    return route.continue();
  });

  return {
    projectId: PID,
    getProgressCallsAfterTerminal: () => progressCallsAfterTerminal,
    getExportCalls: () => exportCalls,
  };
}

/** 走真实登录 UI(验证码接口已打桩成功) */
export async function loginThroughUi(page: Page) {
  await page.goto('/ai-ke-jian-sheng-cheng');
  const menuVisible = page.getByText('AI课件生成管理');
  if (await menuVisible.isVisible().catch(() => false)) {
    return;
  }
  const submitBtn = page.locator('button[type="submit"]');
  if (await submitBtn.isVisible().catch(() => false)) {
    await page.locator('.ant-select').first().click();
    // 租户下拉项(避免与已选中显示文案 strict 冲突)
    await page.locator('.ant-select-item-option-content', { hasText: '千铝工投' }).first().click();
    await page.getByLabel('账号').fill('admin');
    await page.getByLabel('密码').fill('Admin@123456');
    await submitBtn.click();
    // 滑块验证码(接口已打桩成功)
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
}

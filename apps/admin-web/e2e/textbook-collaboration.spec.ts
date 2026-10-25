/**
 * 教材编写协同（二期）e2e 主链路 —— 《职业标准驱动教材生成·二期编写协同实施设计》v1.1 §12.4。
 *
 * 运行：npm run test:e2e:textbook-collaboration（内部 playwright.chrome.config.ts）
 *
 * 全接口 mock（照 textbook-outline.spec.ts 打桩模式）：
 * 1. admin 打开已 reviewed 工程，将章 A 分给 writer1/reviewer2、章 B 分给 writer2/reviewer2
 * 2. writer1 登录只看到章 A；编写两节、上传一张图片、提交
 * 3. writer2 不能直接访问章 A；reviewer2 打回第一节并通过第二节
 * 4. writer1 看到意见，修改重交；reviewer2 通过；章 A 完成
 * 5. admin 查看进度，未完成章 B 时不能完成工程；补齐后完成并下载 final docx
 * 6. 附加短链路：重开工程 + 撤销通过后，旧 baseVersion 保存出现冲突弹窗，服务端内容未被覆盖
 */
import { expect, test, type Page, type Route } from '@playwright/test';

// ------------------------------------------------------------------
// 内存态：工程 / 章 / 节 / 分配 / 正文 / 快照 / 审定记录 / 资产
// ------------------------------------------------------------------

interface SectionState {
  sectionId: string;
  title: string;
  sourceNo: string;
  sortNo: number;
  targetWords: number;
}

interface ChapterState {
  chapterId: string;
  title: string;
  sourceNo: string;
  targetWords: number;
  sections: SectionState[];
}

interface AssignmentState {
  chapterId: string;
  writerId: string;
  reviewerId: string;
  dueDate: string | null;
  instruction: string | null;
  active: boolean;
  taskStatus: string;
  assignmentVersion: number;
  assignedByName: string;
  assignedAt: string;
}

interface ContentState {
  sectionId: string;
  chapterId: string;
  contentMd: string;
  contentStatus: 'draft' | 'submitted' | 'approved' | 'returned';
  version: number;
  submittedBy: string | null;
  submittedByName: string | null;
  submittedAt: string | null;
  submitRevision: number | null;
}

interface VersionState {
  id: string;
  sectionId: string;
  snapshotNo: number;
  snapshotType: 'submit' | 'before_restore';
  submitRevision: number | null;
  contentMd: string;
  wordCount: number;
  createdAt: string;
  createdByName: string;
}

interface ReviewRecordState {
  sectionId: string;
  action: 'submit' | 'approve' | 'return' | 'reopen';
  submitRevision: number;
  comment: string | null;
  actorName: string;
  createdAt: string;
}

const USERS: Record<string, { id: string; username: string; displayName: string; permissions: string[]; roles: string[] }> = {
  'token-admin': {
    id: '1', username: 'admin', displayName: '管理员',
    permissions: ['textbook:project:view', 'textbook:project:manage', 'textbook:project:export'],
    roles: ['PLATFORM_ADMIN'],
  },
  'token-writer1': {
    id: '101', username: 'zhang', displayName: '张一',
    permissions: ['textbook:task:view', 'textbook:content:write'],
    roles: ['TEXTBOOK_WRITER'],
  },
  'token-writer2': {
    id: '102', username: 'wang', displayName: '王二',
    permissions: ['textbook:task:view', 'textbook:content:write'],
    roles: ['TEXTBOOK_WRITER'],
  },
  'token-reviewer2': {
    id: '202', username: 'li', displayName: '李审',
    permissions: ['textbook:review:view', 'textbook:content:review'],
    roles: ['TEXTBOOK_REVIEWER'],
  },
};

function menuFor(token: string) {
  const u = USERS[token]!;
  const base = [
    {
      id: 1, parentId: 0, menuType: 'directory', code: 'zhi_neng_ke_cheng_cai_liao_guan_li',
      name: '智能课程材料管理', path: '/zhi-neng-ke-cheng-cai-liao-guan-li', component: '',
      permissionCode: '', icon: '', sortOrder: 1, children: [] as unknown[],
    },
  ];
  if (u.roles.includes('PLATFORM_ADMIN')) {
    base[0].children = [
      {
        id: 2, parentId: 1, menuType: 'page', code: 'textbook_project', name: '教材生成管理',
        path: '/jiao-cai-sheng-cheng', component: 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookProjectPage',
        permissionCode: 'textbook:project:view', icon: 'BookOutlined', sortOrder: 5, children: [],
      },
    ];
  }
  if (u.roles.includes('TEXTBOOK_WRITER')) {
    base[0].children = [
      {
        id: 3, parentId: 1, menuType: 'page', code: 'textbook_my_writing', name: '我的编写任务',
        path: '/wo-de-bian-xie', component: 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookMyTasksPage',
        permissionCode: 'textbook:task:view', icon: 'EditOutlined', sortOrder: 6, children: [],
      },
    ];
  }
  if (u.roles.includes('TEXTBOOK_REVIEWER')) {
    base[0].children = [
      {
        id: 4, parentId: 1, menuType: 'page', code: 'textbook_my_review', name: '我的审定任务',
        path: '/wo-de-shen-ding', component: 'zhi_neng_ke_cheng_cai_liao_guan_li/TextbookMyTasksPage',
        permissionCode: 'textbook:review:view', icon: 'AuditOutlined', sortOrder: 7, children: [],
      },
    ];
  }
  return base;
}

function createState() {
  return {
    projectStatus: 'reviewed' as string,
    chapters: [
      {
        chapterId: '9001', title: '铝电解槽操作', sourceNo: '2', targetWords: 40000,
        sections: [
          { sectionId: '9101', title: '槽况判断', sourceNo: '2.1', sortNo: 1, targetWords: 15000 },
          { sectionId: '9102', title: '更换阳极', sourceNo: '2.2', sortNo: 2, targetWords: 15000 },
        ],
      },
      {
        chapterId: '9002', title: '工作准备', sourceNo: '1', targetWords: 15000,
        sections: [{ sectionId: '9201', title: '操作前准备', sourceNo: '1.1', sortNo: 1, targetWords: 8000 }],
      },
    ] as ChapterState[],
    assignments: new Map<string, AssignmentState>(),
    contents: new Map<string, ContentState>(),
    versions: [] as VersionState[],
    reviewRecords: [] as ReviewRecordState[],
    assetSeq: 9900,
  };
}

function wordCountOf(md: string) {
  return md.replace(/\s/g, '').length;
}

function refreshTaskStatus(s: ReturnType<typeof createState>, chapterId: string) {
  const chapter = s.chapters.find((c) => c.chapterId === chapterId)!;
  const a = s.assignments.get(chapterId);
  if (!a || !a.active) return;
  const contents = chapter.sections.map((sec) => s.contents.get(sec.sectionId));
  const sectionCount = chapter.sections.length;
  const approvedCount = contents.filter((c) => c?.contentStatus === 'approved').length;
  const submittedCount = contents.filter((c) => c?.contentStatus === 'submitted').length;
  const returnedCount = contents.filter((c) => c?.contentStatus === 'returned').length;
  const startedCount = contents.filter((c) => c && (c.contentMd.trim().length > 0 || c.contentStatus !== 'draft')).length;
  a.sectionCount = sectionCount;
  a.approvedCount = approvedCount;
  a.submittedCount = submittedCount;
  a.returnedCount = returnedCount;
  a.draftCount = contents.filter((c) => c?.contentStatus === 'draft').length;
  a.taskStatus =
    approvedCount === sectionCount && sectionCount > 0
      ? 'approved'
      : returnedCount > 0
        ? 'returned'
        : submittedCount + approvedCount === sectionCount && sectionCount > 0
          ? 'reviewing'
          : startedCount > 0
            ? 'writing'
            : 'assigned';
}

// assignment 概览字段由 refreshTaskStatus 维护（接口合并补充到 AssignmentState）
interface AssignmentState {
  sectionCount: number;
  approvedCount: number;
  submittedCount: number;
  returnedCount: number;
  draftCount: number;
}

function ok(data: unknown) {
  return { code: '0', message: 'OK', data, traceId: 'e2e' };
}

function assignmentVO(a: AssignmentState) {
  const usersById = Object.fromEntries(Object.values(USERS).map((u) => [u.id, u]));
  return {
    chapterId: a.chapterId,
    writerId: a.writerId,
    writerName: usersById[a.writerId]?.displayName ?? a.writerId,
    reviewerId: a.reviewerId,
    reviewerName: usersById[a.reviewerId]?.displayName ?? a.reviewerId,
    dueDate: a.dueDate,
    instruction: a.instruction,
    active: a.active,
    taskStatus: a.taskStatus,
    assignmentVersion: a.assignmentVersion,
    sectionCount: a.sectionCount,
    approvedCount: a.approvedCount,
    submittedCount: a.submittedCount,
    returnedCount: a.returnedCount,
    draftCount: a.draftCount,
    untouchedCount: Math.max(0, a.sectionCount - a.approvedCount - a.submittedCount - a.returnedCount - a.draftCount),
    assignedByName: a.assignedByName,
    assignedAt: a.assignedAt,
  };
}

function contentExistsVO(s: ReturnType<typeof createState>, sec: SectionState, chapter: ChapterState) {
  const c = s.contents.get(sec.sectionId);
  return {
    sectionId: sec.sectionId,
    title: sec.title,
    sourceNo: sec.sourceNo,
    sortNo: sec.sortNo,
    contentExists: !!c,
    contentStatus: c?.contentStatus ?? null,
    wordCount: c ? wordCountOf(c.contentMd) : 0,
    targetWords: sec.targetWords,
    version: c?.version ?? 0,
    submittedAt: c?.submittedAt ?? null,
    latestReviewRecord: [...s.reviewRecords].reverse().find((r) => r.sectionId === sec.sectionId) ?? null,
  };
}

function sectionContentVO(s: ReturnType<typeof createState>, sectionId: string, token: string) {
  const user = USERS[token]!;
  const chapter = s.chapters.find((c) => c.sections.some((x) => x.sectionId === sectionId))!;
  const sec = chapter.sections.find((x) => x.sectionId === sectionId)!;
  const c = s.contents.get(sectionId);
  const a = s.assignments.get(chapter.chapterId);
  const isWriter = a?.active && a.writerId === user.id && user.permissions.includes('textbook:content:write');
  const isReviewer = a?.active && a.reviewerId === user.id && user.permissions.includes('textbook:content:review');
  const editableStatus = c == null || c.contentStatus === 'draft' || c.contentStatus === 'returned';
  const canWrite = !!(isWriter && editableStatus && s.projectStatus === 'writing');
  const readOnlyReason = !canWrite
    ? c?.contentStatus === 'submitted'
      ? `本节已提交，等待${a ? assignmentVO(a).reviewerName : '审定人'}审定`
      : c?.contentStatus === 'approved'
        ? '本节已通过审定'
        : s.projectStatus !== 'writing'
          ? '工程已完成编写，只读'
          : '当前用户无编写权限'
    : null;
  const relations = [isWriter ? 'writer' : '', isReviewer ? 'reviewer' : ''].filter(Boolean);
  return {
    sectionId,
    chapterId: chapter.chapterId,
    projectId: '8001',
    sectionTitle: sec.title,
    chapterTitle: chapter.title,
    targetWords: sec.targetWords,
    contentExists: !!c,
    contentMd: c?.contentMd ?? '',
    contentStatus: c?.contentStatus ?? null,
    wordCount: c ? wordCountOf(c.contentMd) : 0,
    version: c?.version ?? 0,
    relations,
    canWrite,
    canReview: !!(isReviewer && s.projectStatus === 'writing'),
    readOnly: !canWrite,
    readOnlyReason,
    checkReport: null,
    assets: [],
    submittedBy: c?.submittedBy ?? null,
    submittedByName: c?.submittedByName ?? null,
    submittedAt: c?.submittedAt ?? null,
    submitRevision: c?.contentStatus === 'submitted' || c?.contentStatus === 'approved' ? c?.submitRevision : null,
    currentSnapshotId: null,
    reviewedByName: null,
    reviewedAt: null,
    latestReviewRecord: [...s.reviewRecords].reverse().find((r) => r.sectionId === sectionId) ?? null,
  };
}

function workspaceVO(s: ReturnType<typeof createState>, chapterId: string, token: string) {
  const user = USERS[token]!;
  const chapter = s.chapters.find((c) => c.chapterId === chapterId)!;
  const a = s.assignments.get(chapterId);
  const isWriter = a?.active && a.writerId === user.id;
  const isReviewer = a?.active && a.reviewerId === user.id;
  const allowed =
    user.permissions.includes('textbook:project:view') || (a?.active && (isWriter || isReviewer));
  if (!allowed) return null;
  const relations = [isWriter ? 'writer' : '', isReviewer ? 'reviewer' : ''].filter(Boolean);
  return {
    projectId: '8001',
    projectStatus: s.projectStatus,
    workTypeTitle: '铝电解工',
    direction: '槽操作工',
    levelCode: 5,
    chapterId,
    chapterTitle: chapter.title,
    chapterSourceNo: chapter.sourceNo,
    assignment: a && a.active ? assignmentVO(a) : null,
    relations,
    canWrite: !!(isWriter && s.projectStatus === 'writing'),
    canReview: !!(isReviewer && s.projectStatus === 'writing'),
    readOnlyReason: s.projectStatus === 'done' ? '工程已完成编写，只读' : null,
    sections: [...chapter.sections]
      .sort((x, y) => x.sortNo - y.sortNo)
      .map((sec) => {
        const c = s.contents.get(sec.sectionId);
        return {
          ...contentExistsVO(s, sec, chapter),
          submittedByName: c?.submittedByName ?? null,
          submitRevision:
            c && (c.contentStatus === 'submitted' || c.contentStatus === 'approved') ? c.submitRevision : null,
          currentSnapshotId: null,
          points: [],
        };
      }),
  };
}

function completionOf(s: ReturnType<typeof createState>) {
  const blockers: { code: string; message: string }[] = [];
  if (s.chapters.length === 0) blockers.push({ code: 'NO_CHAPTER', message: '工程没有任何章' });
  for (const chapter of s.chapters) {
    const a = s.assignments.get(chapter.chapterId);
    if (!a || !a.active) blockers.push({ code: 'CHAPTER_UNASSIGNED', message: `「${chapter.title}」尚未分配主笔/审定人` });
    if (chapter.sections.length === 0) blockers.push({ code: 'EMPTY_CHAPTER', message: `「${chapter.title}」章下没有节` });
    for (const sec of chapter.sections) {
      const c = s.contents.get(sec.sectionId);
      if (!c || c.contentStatus !== 'approved') {
        blockers.push({ code: 'SECTION_NOT_APPROVED', message: `「${sec.title}」尚未通过` });
      }
      if (c?.contentMd.includes('[待专家补充]')) {
        blockers.push({ code: 'PLACEHOLDER_PRESENT', message: `「${sec.title}」仍有待专家补充占位` });
      }
    }
  }
  return { canComplete: blockers.length === 0, blockers };
}

function collaborationVO(s: ReturnType<typeof createState>) {
  const sections = s.chapters.flatMap((c) => c.sections);
  const approved = sections.filter((sec) => s.contents.get(sec.sectionId)?.contentStatus === 'approved').length;
  return {
    project: { id: '8001', status: s.projectStatus, workTypeTitle: '铝电解工', direction: '槽操作工', levelCode: 5 },
    outlineSummary: null,
    progress: {
      chapterCount: s.chapters.length,
      assignedChapterCount: [...s.assignments.values()].filter((a) => a.active).length,
      sectionCount: sections.length,
      approvedSectionCount: approved,
      actualWords: sections.reduce((sum, sec) => sum + (s.contents.get(sec.sectionId) ? wordCountOf(s.contents.get(sec.sectionId)!.contentMd) : 0), 0),
      targetWords: 55000,
    },
    completion: completionOf(s),
    chapters: s.chapters.map((chapter) => ({
      chapterId: chapter.chapterId,
      title: chapter.title,
      sourceNo: chapter.sourceNo,
      targetWords: chapter.targetWords,
      assignment: (() => {
        const a = s.assignments.get(chapter.chapterId);
        return a && a.active ? assignmentVO(a) : null;
      })(),
      sections: [...chapter.sections]
        .sort((x, y) => x.sortNo - y.sortNo)
        .map((sec) => contentExistsVO(s, sec, chapter)),
    })),
  };
}

function taskVO(s: ReturnType<typeof createState>, a: AssignmentState) {
  const chapter = s.chapters.find((c) => c.chapterId === a.chapterId)!;
  const actualWords = chapter.sections.reduce(
    (sum, sec) => sum + (s.contents.get(sec.sectionId) ? wordCountOf(s.contents.get(sec.sectionId)!.contentMd) : 0),
    0,
  );
  const latest = Math.max(
    0,
    ...chapter.sections.map((sec) => (s.contents.get(sec.sectionId)?.submittedAt ? Date.parse(s.contents.get(sec.sectionId)!.submittedAt!) : 0)),
  );
  return {
    ...assignmentVO(a),
    projectId: '8001',
    workTypeTitle: '铝电解工',
    direction: '槽操作工',
    levelCode: 5,
    chapterTitle: chapter.title,
    chapterSourceNo: chapter.sourceNo,
    projectStatus: s.projectStatus,
    latestSectionUpdatedAt: latest > 0 ? '2026-10-24 11:00:00' : null,
    actualWords,
    targetWords: chapter.targetWords,
    overdue: false,
  };
}

// ------------------------------------------------------------------
// 后端打桩
// ------------------------------------------------------------------

async function mockBackend(context: import('@playwright/test').BrowserContext, s: ReturnType<typeof createState>) {
  const currentToken = (route: Route) =>
    (route.request().headers()['authorization'] || '').replace('Bearer ', '') || 'token-admin';

  await context.route('**/api/auth/me', (route) => {
    const token = currentToken(route);
    const u = USERS[token] ?? USERS['token-admin'];
    return route.fulfill({
      json: ok({ id: Number(u.id), tenantId: 10159, username: u.username, displayName: u.displayName, userType: 'admin', roles: u.roles, permissions: u.permissions }),
    });
  });
  await context.route('**/api/auth/menus', (route) => route.fulfill({ json: ok(menuFor(currentToken(route))) }));
  await context.route('**/api/auth/tenants', (route) => route.fulfill({ json: ok([]) }));

  await context.route('**/api/textbook/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();
    const token = currentToken(route);
    const user = USERS[token] ?? USERS['token-admin'];

    const json = async () => route.request().postDataJSON() as Record<string, unknown>;

    // ---- 一期工程列表/详情（admin 入口用） ----
    if (path === '/api/textbook/projects' && method === 'GET') {
      return route.fulfill({
        json: ok({
          items: [
            {
              id: '8001', workTypeTitle: '铝电解工', seriesNo: '6-17-02-02', standardVersion: '2020', fileId: '6001',
              direction: '槽操作工', levelCode: 5, functionNos: [1, 2], trainingHours: 40, wordsPerHour: 2500,
              totalWords: 100000, status: s.projectStatus, parseWarnings: null, updatedAt: '2026-10-24 10:00:00',
              chapterCount: s.chapters.length,
              assignedChapterCount: [...s.assignments.values()].filter((a) => a.active).length,
              sectionCount: s.chapters.flatMap((c) => c.sections).length,
              approvedSectionCount: s.chapters.flatMap((c) => c.sections).filter((sec) => s.contents.get(sec.sectionId)?.contentStatus === 'approved').length,
              hasContent: s.contents.size > 0,
              structureLocked: s.contents.size > 0 || [...s.assignments.values()].some((a) => a.active),
            },
          ],
          page: 1, pageSize: 20, total: 1,
        }),
      });
    }

    // ---- C1 候选人（按角色过滤有权限的启用用户，不含手机号） ----
    if (path === '/api/textbook/collaborators') {
      const role = url.searchParams.get('role') || 'writer';
      const items =
        role === 'writer'
          ? ['token-writer1', 'token-writer2'].map((t) => ({ id: USERS[t].id, displayName: USERS[t].displayName, username: USERS[t].username, orgName: '电解车间' }))
          : [{ id: USERS['token-reviewer2'].id, displayName: USERS['token-reviewer2'].displayName, username: USERS['token-reviewer2'].username, orgName: '质检科' }];
      return route.fulfill({ json: ok({ items, page: 1, pageSize: 20, total: items.length }) });
    }

    // ---- C2 编写管理 ----
    if (/\/projects\/8001\/collaboration$/.test(path) && method === 'GET') {
      return route.fulfill({ json: ok(collaborationVO(s)) });
    }

    // ---- C3 批量分配/改派 ----
    if (/\/projects\/8001\/assignments$/.test(path) && method === 'PUT') {
      if (!user.permissions.includes('textbook:project:manage')) {
        return route.fulfill({ status: 403, json: ok(null) });
      }
      const body = await json();
      const items = body.items as { chapterId: string; writerId: string; reviewerId: string; dueDate?: string; instruction?: string; assignmentVersion: number | null }[];
      for (const item of items) {
        const chapter = s.chapters.find((c) => c.chapterId === item.chapterId);
        if (!chapter || chapter.sections.length === 0) {
          return route.fulfill({ status: 400, json: { code: '400', message: `章 ${item.chapterId} 不存在或无节`, data: null, traceId: 'e2e' } });
        }
        if (item.writerId === item.reviewerId) {
          return route.fulfill({ status: 400, json: { code: '400', message: '主笔与审定人不能是同一人', data: null, traceId: 'e2e' } });
        }
        const existing = s.assignments.get(item.chapterId);
        if (existing && item.assignmentVersion !== existing.assignmentVersion) {
          return route.fulfill({
            status: 409,
            json: { code: '409', message: '分配版本冲突', data: { reason: 'ASSIGNMENT_VERSION_CONFLICT', currentAssignment: assignmentVO(existing) }, traceId: 'e2e' },
          });
        }
        s.assignments.set(item.chapterId, {
          chapterId: item.chapterId,
          writerId: item.writerId,
          reviewerId: item.reviewerId,
          dueDate: item.dueDate ?? null,
          instruction: item.instruction ?? null,
          active: true,
          taskStatus: 'assigned',
          assignmentVersion: (existing?.assignmentVersion ?? 0) + 1,
          assignedByName: user.displayName,
          assignedAt: '2026-10-24 10:30:00',
          sectionCount: chapter.sections.length,
          approvedCount: 0, submittedCount: 0, returnedCount: 0, draftCount: 0,
        });
        refreshTaskStatus(s, item.chapterId);
      }
      // 首个有效分配 → writing
      if (s.projectStatus === 'reviewed') s.projectStatus = 'writing';
      return route.fulfill({ json: ok(items.map((i) => assignmentVO(s.assignments.get(i.chapterId)!))) });
    }

    // ---- C5/C6 完成/重开 ----
    if (/\/projects\/8001\/complete$/.test(path) && method === 'POST') {
      const completion = completionOf(s);
      if (!completion.canComplete) {
        return route.fulfill({ status: 409, json: { code: '409', message: '尚不满足完成条件', data: { completion }, traceId: 'e2e' } });
      }
      s.projectStatus = 'done';
      return route.fulfill({ json: ok({ id: '8001', status: 'done' }) });
    }
    if (/\/projects\/8001\/reopen$/.test(path) && method === 'POST') {
      s.projectStatus = 'writing';
      return route.fulfill({ json: ok({ id: '8001', status: 'writing' }) });
    }

    // ---- T1~T4 我的任务 ----
    if (path === '/api/textbook/my-writing-tasks' && method === 'GET') {
      const items = [...s.assignments.values()].filter((a) => a.active && a.writerId === user.id).map((a) => taskVO(s, a));
      return route.fulfill({ json: ok({ items, page: 1, pageSize: 20, total: items.length }) });
    }
    if (path === '/api/textbook/my-writing-tasks/summary') {
      const mine = [...s.assignments.values()].filter((a) => a.active && a.writerId === user.id);
      return route.fulfill({
        json: ok({
          assignedCount: mine.filter((a) => a.taskStatus === 'assigned').length,
          writingCount: mine.filter((a) => a.taskStatus === 'writing').length,
          pendingReviewCount: mine.filter((a) => a.submittedCount > 0).length,
          returnedCount: mine.filter((a) => a.returnedCount > 0).length,
          approvedCount: mine.filter((a) => a.taskStatus === 'approved').length,
        }),
      });
    }
    if (path === '/api/textbook/my-review-tasks' && method === 'GET') {
      const status = url.searchParams.get('status');
      let items = [...s.assignments.values()].filter((a) => a.active && a.reviewerId === user.id).map((a) => taskVO(s, a));
      if (status === 'pending') items = items.filter((t) => t.submittedCount > 0);
      return route.fulfill({ json: ok({ items, page: 1, pageSize: 20, total: items.length }) });
    }
    if (path === '/api/textbook/my-review-tasks/summary') {
      const mine = [...s.assignments.values()].filter((a) => a.active && a.reviewerId === user.id);
      return route.fulfill({
        json: ok({
          pendingCount: mine.filter((a) => a.submittedCount > 0).length,
          returnedCount: mine.filter((a) => a.returnedCount > 0).length,
          partialApprovedCount: mine.filter((a) => a.approvedCount > 0 && a.taskStatus !== 'approved').length,
          approvedCount: mine.filter((a) => a.taskStatus === 'approved').length,
        }),
      });
    }

    // ---- W1 工作台 ----
    const wsMatch = path.match(/\/textbook\/chapters\/([^/]+)\/workspace$/);
    if (wsMatch && method === 'GET') {
      const vo = workspaceVO(s, wsMatch[1], token);
      if (!vo) return route.fulfill({ status: 404, json: { code: '404', message: '章节不存在或无权访问', data: null, traceId: 'e2e' } });
      return route.fulfill({ json: ok(vo) });
    }

    // ---- W2/W3 正文 ----
    const contentMatch = path.match(/\/textbook\/sections\/([^/]+)\/content$/);
    if (contentMatch) {
      const sectionId = contentMatch[1];
      if (method === 'GET') {
        return route.fulfill({ json: ok(sectionContentVO(s, sectionId, token)) });
      }
      if (method === 'PUT') {
        const body = await json();
        const contentMd = String(body.contentMd ?? '');
        const baseVersion = Number(body.baseVersion ?? 0);
        const current = s.contents.get(sectionId);
        const chapter = s.chapters.find((c) => c.sections.some((x) => x.sectionId === sectionId))!;
        const a = s.assignments.get(chapter.chapterId);
        const isWriter = a?.active && a.writerId === user.id && user.permissions.includes('textbook:content:write');
        if (!isWriter || s.projectStatus !== 'writing') {
          return route.fulfill({ status: 404, json: { code: '404', message: '章节不存在或无权访问', data: null, traceId: 'e2e' } });
        }
        if (current && (current.contentStatus === 'submitted' || current.contentStatus === 'approved')) {
          return route.fulfill({ status: 409, json: { code: '409', message: '当前状态不允许保存', data: { reason: 'VERSION_CONFLICT', currentVersion: current.version, currentContentMd: current.contentMd, currentUpdatedAt: '2026-10-24 12:00:00', currentUpdatedByName: '其他人' }, traceId: 'e2e' } });
        }
        if ((current?.version ?? 0) !== baseVersion) {
          // 旧 baseVersion：409 冲突，不覆盖先保存内容（§7.6）
          return route.fulfill({
            status: 409,
            json: {
              code: '409', message: '内容版本冲突',
              data: { reason: 'VERSION_CONFLICT', currentVersion: current!.version, currentContentMd: current!.contentMd, currentUpdatedAt: '2026-10-24 12:00:00', currentUpdatedByName: '其他人' },
              traceId: 'e2e',
            },
          });
        }
        if (!current) {
          s.contents.set(sectionId, { sectionId, chapterId: chapter.chapterId, contentMd, contentStatus: 'draft', version: 1, submittedBy: null, submittedByName: null, submittedAt: null, submitRevision: null });
        } else {
          current.contentMd = contentMd;
          current.version += 1;
        }
        refreshTaskStatus(s, chapter.chapterId);
        return route.fulfill({ json: ok(sectionContentVO(s, sectionId, token)) });
      }
    }

    // ---- W4 提交 ----
    const submitMatch = path.match(/\/textbook\/sections\/([^/]+)\/submit$/);
    if (submitMatch && method === 'POST') {
      const sectionId = submitMatch[1];
      const body = await json();
      const current = s.contents.get(sectionId);
      const chapter = s.chapters.find((c) => c.sections.some((x) => x.sectionId === sectionId))!;
      const a = s.assignments.get(chapter.chapterId);
      if (!current || !a || a.writerId !== user.id) {
        return route.fulfill({ status: 404, json: { code: '404', message: '章节不存在或无权访问', data: null, traceId: 'e2e' } });
      }
      if (Number(body.baseVersion) !== current.version) {
        return route.fulfill({ status: 409, json: { code: '409', message: '内容版本冲突', data: { reason: 'VERSION_CONFLICT', currentVersion: current.version, currentContentMd: current.contentMd, currentUpdatedAt: null, currentUpdatedByName: null }, traceId: 'e2e' } });
      }
      if (current.contentMd.replace(/[\s#!\[\]()*>`-]/g, '').length === 0) {
        return route.fulfill({ status: 400, json: { code: '400', message: '正文为空，不能提交', data: null, traceId: 'e2e' } });
      }
      const nextRevision = (current.submitRevision ?? 0) + 1;
      current.contentStatus = 'submitted';
      current.version += 1;
      current.submittedBy = user.id;
      current.submittedByName = user.displayName;
      current.submittedAt = '2026-10-24 11:00:00';
      current.submitRevision = nextRevision;
      s.versions.push({
        id: `9500${s.versions.length + 1}`,
        sectionId,
        snapshotNo: s.versions.filter((v) => v.sectionId === sectionId).length + 1,
        snapshotType: 'submit',
        submitRevision: nextRevision,
        contentMd: current.contentMd,
        wordCount: wordCountOf(current.contentMd),
        createdAt: '2026-10-24 11:00:00',
        createdByName: user.displayName,
      });
      s.reviewRecords.push({ sectionId, action: 'submit', submitRevision: nextRevision, comment: null, actorName: user.displayName, createdAt: '2026-10-24 11:00:00' });
      refreshTaskStatus(s, chapter.chapterId);
      return route.fulfill({ json: ok(sectionContentVO(s, sectionId, token)) });
    }

    // ---- W5 通过/打回/撤销 ----
    const reviewMatch = path.match(/\/textbook\/sections\/([^/]+)\/review$/);
    if (reviewMatch && method === 'POST') {
      const sectionId = reviewMatch[1];
      const body = await json();
      const action = String(body.action);
      const comment = body.comment == null ? null : String(body.comment);
      const current = s.contents.get(sectionId);
      const chapter = s.chapters.find((c) => c.sections.some((x) => x.sectionId === sectionId))!;
      const a = s.assignments.get(chapter.chapterId);
      if (!current || !a || a.reviewerId !== user.id) {
        return route.fulfill({ status: 404, json: { code: '404', message: '章节不存在或无权访问', data: null, traceId: 'e2e' } });
      }
      if (Number(body.baseVersion) !== current.version) {
        return route.fulfill({ status: 409, json: { code: '409', message: '内容版本冲突', data: { reason: 'VERSION_CONFLICT', currentVersion: current.version, currentContentMd: current.contentMd, currentUpdatedAt: null, currentUpdatedByName: null }, traceId: 'e2e' } });
      }
      if ((action === 'return' || action === 'reopen') && (!comment || comment.trim().length === 0)) {
        return route.fulfill({ status: 400, json: { code: '400', message: '打回/撤销必须填写意见', data: null, traceId: 'e2e' } });
      }
      if (action === 'approve' && current.contentStatus !== 'submitted') {
        return route.fulfill({ status: 409, json: { code: '409', message: '当前状态不能通过', data: null, traceId: 'e2e' } });
      }
      if (action === 'return' && current.contentStatus !== 'submitted') {
        return route.fulfill({ status: 409, json: { code: '409', message: '当前状态不能打回', data: null, traceId: 'e2e' } });
      }
      if (action === 'reopen' && current.contentStatus !== 'approved') {
        return route.fulfill({ status: 409, json: { code: '409', message: '当前状态不能撤销通过', data: null, traceId: 'e2e' } });
      }
      current.contentStatus = action === 'approve' ? 'approved' : 'returned';
      current.version += 1;
      s.reviewRecords.push({ sectionId, action, submitRevision: current.submitRevision ?? 1, comment, actorName: user.displayName, createdAt: '2026-10-24 12:00:00' });
      refreshTaskStatus(s, chapter.chapterId);
      return route.fulfill({ json: ok(sectionContentVO(s, sectionId, token)) });
    }

    // ---- W6 时间线 ----
    const reviewsMatch = path.match(/\/textbook\/sections\/([^/]+)\/reviews$/);
    if (reviewsMatch && method === 'GET') {
      const items = [...s.reviewRecords].reverse().filter((r) => r.sectionId === reviewsMatch[1]);
      return route.fulfill({ json: ok({ items: items.map((r, i) => ({ id: String(i), ...r })), page: 1, pageSize: 20, total: items.length }) });
    }

    // ---- W7/W8 快照 ----
    const versionsMatch = path.match(/\/textbook\/sections\/([^/]+)\/versions$/);
    if (versionsMatch && method === 'GET') {
      const items = s.versions.filter((v) => v.sectionId === versionsMatch[1]).sort((x, y) => y.snapshotNo - x.snapshotNo);
      return route.fulfill({
        json: ok({
          items: items.map(({ contentMd: _contentMd, ...rest }) => rest),
          page: 1, pageSize: 20, total: items.length,
        }),
      });
    }
    const versionDetail = path.match(/\/textbook\/section-versions\/([^/]+)$/);
    if (versionDetail && method === 'GET') {
      const v = s.versions.find((x) => x.id === versionDetail[1]);
      if (!v) return route.fulfill({ status: 404, json: ok(null) });
      return route.fulfill({ json: ok(v) });
    }

    // ---- W9 恢复 ----
    const restoreMatch = path.match(/\/textbook\/sections\/([^/]+)\/restore$/);
    if (restoreMatch && method === 'POST') {
      const sectionId = restoreMatch[1];
      const body = await json();
      const current = s.contents.get(sectionId);
      const snap = s.versions.find((v) => v.id === body.snapshotId && v.sectionId === sectionId);
      const chapter = s.chapters.find((c) => c.sections.some((x) => x.sectionId === sectionId))!;
      const a = s.assignments.get(chapter.chapterId);
      if (!current || !snap || !a || a.writerId !== user.id) {
        return route.fulfill({ status: 404, json: ok(null) });
      }
      if (Number(body.baseVersion) !== current.version) {
        return route.fulfill({ status: 409, json: { code: '409', message: '内容版本冲突', data: { reason: 'VERSION_CONFLICT', currentVersion: current.version, currentContentMd: current.contentMd, currentUpdatedAt: null, currentUpdatedByName: null }, traceId: 'e2e' } });
      }
      s.versions.push({
        id: `9500${s.versions.length + 1}`,
        sectionId,
        snapshotNo: s.versions.filter((v) => v.sectionId === sectionId).length + 1,
        snapshotType: 'before_restore',
        submitRevision: null,
        contentMd: current.contentMd,
        wordCount: wordCountOf(current.contentMd),
        createdAt: '2026-10-24 13:00:00',
        createdByName: user.displayName,
      });
      current.contentMd = snap.contentMd;
      current.version += 1;
      refreshTaskStatus(s, chapter.chapterId);
      return route.fulfill({ json: ok(sectionContentVO(s, sectionId, token)) });
    }

    // ---- A1/A2 图片 ----
    const assetUpload = path.match(/\/textbook\/sections\/([^/]+)\/assets$/);
    if (assetUpload && method === 'POST') {
      const sectionId = assetUpload[1];
      const chapter = s.chapters.find((c) => c.sections.some((x) => x.sectionId === sectionId))!;
      const a = s.assignments.get(chapter.chapterId);
      if (!a || a.writerId !== user.id) {
        return route.fulfill({ status: 404, json: ok(null) });
      }
      s.assetSeq += 1;
      return route.fulfill({ json: ok({ assetId: String(s.assetSeq), sourceUri: `textbook-asset://${s.assetSeq}` }) });
    }
    const assetContent = path.match(/\/textbook\/sections\/([^/]+)\/assets\/([^/]+)\/content$/);
    if (assetContent && method === 'GET') {
      const png = Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
        'base64',
      );
      return route.fulfill({ body: png, headers: { 'Content-Type': 'image/png', 'Cache-Control': 'private, no-store' } });
    }

    // ---- E1~E4 docx 导出 ----
    const exportSection = path.match(/\/textbook\/sections\/([^/]+)\/export-docx$/);
    const exportChapter = path.match(/\/textbook\/chapters\/([^/]+)\/export-docx$/);
    const exportBook = path.match(/\/projects\/8001\/export-book$/);
    if (exportSection || exportChapter || exportBook) {
      const name = exportBook
        ? `教材正文_铝电解工_槽操作工_五级-初级工_${url.searchParams.get('mode') === 'final' ? 'final' : 'review'}_20261024.docx`
        : '教材节_导出.docx';
      return route.fulfill({
        headers: {
          'Content-Type': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
          'Content-Disposition': `attachment; filename*=UTF-8''${encodeURIComponent(name)}`,
        },
        body: Buffer.from('e2e-docx-bytes'),
      });
    }

    return route.fulfill({ status: 404, json: ok(null) });
  });
}

async function openAs(page: Page, token: string, path: string) {
  await page.goto('/');
  await page.evaluate((t) => {
    localStorage.setItem('guoyu_admin_access_token', t);
    localStorage.setItem('guoyu_admin_tenant_id', '10159');
  }, token);
  await page.goto(path);
}

test('教材编写协同主链路：分配 → 编写/图片/提交 → 打回/通过 → 重交 → 完成导出 → 冲突短链路', async ({ page, context }) => {
  test.setTimeout(300_000);
  const s = createState();
  await mockBackend(context, s);

  // ================================================================
  // ① admin：reviewed 工程 → 编写管理 → 章 A/章 B 分配
  // ================================================================
  await openAs(page, 'token-admin', '/jiao-cai-sheng-cheng');
  await expect(page.getByText('教材生成管理').first()).toBeVisible();
  const row = page.locator('.ant-table-row', { hasText: '铝电解工' }).first();
  await expect(row).toContainText('大纲已评审');

  await row.getByRole('button', { name: '编写管理' }).click();
  await expect(page).toHaveURL(/\/textbook\/collaboration\/8001/);
  await expect(page.getByText('铝电解槽操作').first()).toBeVisible();

  async function assignChapter(chapterTitle: string, writerName: string, reviewerName: string) {
    const chapterRow = page.locator('.ant-table-row', { hasText: chapterTitle }).first();
    await chapterRow.getByRole('button', { name: /^(分\s*配|改\s*派)$/ }).click();
    const modal = page.locator('.ant-modal:visible');
    await modal.locator('.ant-select').nth(0).click();
    await page.locator('.ant-select-item-option:visible', { hasText: writerName }).first().click();
    await modal.locator('.ant-select').nth(1).click();
    await page.locator('.ant-select-item-option:visible', { hasText: reviewerName }).first().click();
    await modal.getByRole('button', { name: /下一步/ }).click();
    await modal.getByRole('button', { name: /确认提交/ }).click();
    await expect(modal).toBeHidden({ timeout: 10_000 });
  }

  await assignChapter('铝电解槽操作', '张一', '李审');
  await assignChapter('工作准备', '王二', '李审');
  // 两章均显示主笔，工程进入编写中
  await expect(page.locator('.ant-table-row', { hasText: '铝电解槽操作' }).first()).toContainText('张一');
  await expect(page.locator('.ant-table-row', { hasText: '工作准备' }).first()).toContainText('王二');
  await expect(page.getByText('编写中').first()).toBeVisible();

  // ================================================================
  // ② writer1：只看到章 A；编写两节、上传图片、提交
  // ================================================================
  await openAs(page, 'token-writer1', '/wo-de-bian-xie');
  await expect(page.getByText('我的编写任务').first()).toBeVisible();
  await expect(page.locator('.ant-table-row').filter({ hasText: '铝电解槽操作' })).toHaveCount(1);
  await expect(page.locator('.ant-table-row').filter({ hasText: '工作准备' })).toHaveCount(0); // writer2 的章不可见

  await page.locator('.ant-table-row', { hasText: '铝电解槽操作' }).getByRole('button', { name: '开始编写' }).click();
  await expect(page).toHaveURL(/\/textbook\/write\/9001/);
  await expect(page.getByRole('button', { name: '节 槽况判断' })).toBeVisible();

  const editor = page.locator('.cm-content');
  await editor.click();
  await editor.fill('槽况判断正文：电解温度、分子比与水平的状态判断方法。');
  await page.getByRole('button', { name: '保存草稿' }).click();
  await expect(page.getByLabel('保存状态')).toContainText(/已(保存|于)/, { timeout: 10_000 });

  // 上传一张图片（仅 A1，正文出现 textbook-asset:// URI）
  const uploadPromise = page.waitForRequest((r) => /\/textbook\/sections\/9101\/assets$/.test(r.url()) && r.method() === 'POST');
  await page.getByRole('button', { name: /上传图片/ }).click();
  await page.setInputFiles('input[type="file"]', {
    name: '槽况图.png',
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
      'base64',
    ),
  });
  const uploadReq = await uploadPromise;
  expect(uploadReq.url()).toContain('/textbook/sections/9101/assets');
  await page.getByRole('button', { name: '保存草稿' }).click();
  await expect(page.getByLabel('保存状态')).toContainText(/已(保存|于)/, { timeout: 10_000 });
  // 编辑器内容含内部资产 URI（无外链图片）
  await expect.poll(async () => await editor.textContent()).toContain('textbook-asset://9901');

  // 提交第一节
  await page.getByRole('button', { name: /提交审定/ }).first().click();
  await page.locator('.ant-popover:visible').getByRole('button', { name: /提交审定/ }).click();
  await expect(page.getByText(/本节已提交，等待李审审定/)).toBeVisible({ timeout: 10_000 });

  // 第二节：编写并提交
  await page.getByRole('button', { name: '节 更换阳极' }).click();
  await editor.click();
  await editor.fill('更换阳极正文：残极拔出与新极安装的完整步骤。');
  await page.getByRole('button', { name: '保存草稿' }).click();
  await expect(page.getByLabel('保存状态')).toContainText(/已(保存|于)/, { timeout: 10_000 });
  await page.getByRole('button', { name: /提交审定/ }).first().click();
  await page.locator('.ant-popover:visible').getByRole('button', { name: /提交审定/ }).click();
  await expect(page.getByText(/本节已提交，等待李审审定/)).toBeVisible({ timeout: 10_000 });

  // ================================================================
  // ③ writer2 不能直接访问章 A；reviewer2 打回第一节、通过第二节
  // ================================================================
  const pageW2 = await context.newPage();
  await openAs(pageW2, 'token-writer2', '/textbook/write/9001');
  await expect(pageW2.getByText('章节不存在或无权访问')).toBeVisible({ timeout: 15_000 });
  await pageW2.close();

  await openAs(page, 'token-reviewer2', '/wo-de-shen-ding');
  await expect(page.getByText('我的审定任务').first()).toBeVisible();
  await expect(page.locator('.ant-table-row').filter({ hasText: '铝电解槽操作' })).toHaveCount(1);
  await page.locator('.ant-table-row', { hasText: '铝电解槽操作' }).getByRole('button', { name: '去审定' }).click();
  await expect(page).toHaveURL(/\/textbook\/review\/9001/);
  await expect(page.locator('.md-editor-preview-wrapper, .md-editor-preview').first()).toContainText('槽况判断正文');

  // 打回第一节（意见必填）
  await page.getByRole('button', { name: /^打\s*回$/ }).click();
  await page.locator('.ant-modal textarea').fill('缺少安全注意事项，请补充后重交');
  await page.getByRole('button', { name: '确认打回' }).click();
  await expect(page.getByText(/缺少安全注意事项/).first()).toBeVisible({ timeout: 10_000 });

  // 通过第二节
  await page.getByRole('button', { name: '节 更换阳极' }).click();
  await expect(page.locator('.md-editor-preview-wrapper, .md-editor-preview').first()).toContainText('更换阳极正文');
  await page.getByRole('button', { name: /^通\s*过$/ }).click();
  await page.getByRole('button', { name: '确认通过' }).click();
  await expect(page.getByText(/撤销通过/).first()).toBeVisible({ timeout: 10_000 });

  // ================================================================
  // ④ writer1 看到意见 → 修改重交（第 2 次提交）；reviewer2 通过 → 章 A 完成
  // ================================================================
  await openAs(page, 'token-writer1', '/wo-de-bian-xie');
  await expect(page.locator('.ant-table-row', { hasText: '铝电解槽操作' }).first()).toContainText('有打回');
  await page.locator('.ant-table-row', { hasText: '铝电解槽操作' }).getByRole('button', { name: '修改打回' }).click();
  await expect(page).toHaveURL(/\/textbook\/write\/9001/);
  await expect(page.getByText(/缺少安全注意事项/).first()).toBeVisible();

  await editor.click();
  await editor.fill('槽况判断正文（修订）：状态判断方法与安全注意事项。');
  await page.getByRole('button', { name: '保存草稿' }).click();
  await expect(page.getByLabel('保存状态')).toContainText(/已(保存|于)/, { timeout: 10_000 });
  await page.getByRole('button', { name: /提交审定/ }).first().click();
  await page.locator('.ant-popover:visible').getByRole('button', { name: /提交审定/ }).click();
  await expect(page.getByText(/本节已提交，等待李审审定/)).toBeVisible({ timeout: 10_000 });

  await openAs(page, 'token-reviewer2', '/wo-de-shen-ding');
  await page.locator('.ant-table-row', { hasText: '铝电解槽操作' }).getByRole('button', { name: '去审定' }).click();
  await expect(page).toHaveURL(/\/textbook\/review\/9001/);
  await expect(page.locator('.md-editor-preview-wrapper, .md-editor-preview').first()).toContainText('槽况判断正文（修订）');
  await page.getByRole('button', { name: /^通\s*过$/ }).click();
  await page.getByRole('button', { name: '确认通过' }).click();
  await expect(page.getByText(/撤销通过/).first()).toBeVisible({ timeout: 10_000 });
  // 章 A 全部通过
  await expect(page.getByText('已通过 2 节').first()).toBeVisible({ timeout: 10_000 });

  // ================================================================
  // ⑤ admin：未完成章 B 时不能完成工程；补齐后完成并下载 final docx
  // ================================================================
  await openAs(page, 'token-admin', '/textbook/collaboration/8001');
  await expect(page.getByRole('button', { name: '完成编写' })).toBeDisabled();
  await expect(page.getByText(/「操作前准备」尚未通过/)).toBeVisible();

  // writer2 补齐章 B
  await openAs(page, 'token-writer2', '/wo-de-bian-xie');
  await page.locator('.ant-table-row', { hasText: '工作准备' }).getByRole('button', { name: '开始编写' }).click();
  await expect(page).toHaveURL(/\/textbook\/write\/9002/);
  await editor.click();
  await editor.fill('操作前准备正文：工器具、劳保用品与点检要求。');
  await page.getByRole('button', { name: '保存草稿' }).click();
  await expect(page.getByLabel('保存状态')).toContainText(/已(保存|于)/, { timeout: 10_000 });
  await page.getByRole('button', { name: /提交审定/ }).first().click();
  await page.locator('.ant-popover:visible').getByRole('button', { name: /提交审定/ }).click();
  await expect(page.getByText(/本节已提交，等待李审审定/)).toBeVisible({ timeout: 10_000 });

  await openAs(page, 'token-reviewer2', '/wo-de-shen-ding');
  await page.locator('.ant-table-row', { hasText: '工作准备' }).getByRole('button', { name: '去审定' }).click();
  await expect(page).toHaveURL(/\/textbook\/review\/9002/);
  await page.getByRole('button', { name: /^通\s*过$/ }).click();
  await page.getByRole('button', { name: '确认通过' }).click();
  await expect(page.getByText(/撤销通过/).first()).toBeVisible({ timeout: 10_000 });

  // admin 完成工程 + 正式导出
  await openAs(page, 'token-admin', '/textbook/collaboration/8001');
  await expect(page.getByRole('button', { name: '完成编写' })).toBeEnabled({ timeout: 15_000 });
  await page.getByRole('button', { name: '完成编写' }).click();
  await page.locator('.ant-popover:visible').getByRole('button', { name: /完成编写/ }).click();
  await expect(page.getByText('编写完成').first()).toBeVisible({ timeout: 10_000 });

  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: '正式导出' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('教材正文_铝电解工');
  expect(download.suggestedFilename()).toContain('final');

  // ================================================================
  // ⑥ 冲突短链路：重开 → 撤销通过 → 两个窗口旧 baseVersion 保存 409，服务端内容未被覆盖
  // ================================================================
  await page.getByRole('button', { name: /重新打开/ }).click();
  await page.locator('.ant-popover:visible').getByRole('button', { name: /重新打开/ }).click();
  await expect(page.getByText('编写中').first()).toBeVisible({ timeout: 10_000 });

  // reviewer2 撤销 9101 通过 → returned
  await openAs(page, 'token-reviewer2', '/textbook/review/9001');
  await page.getByRole('button', { name: '节 槽况判断' }).click();
  await page.getByRole('button', { name: /撤销通过/ }).click();
  await page.locator('.ant-modal textarea').fill('需要补充最新槽控参数');
  await page.getByRole('button', { name: '确认撤销' }).click();
  await expect(page.getByText(/已打回/).first()).toBeVisible({ timeout: 10_000 });

  // writer1 两个窗口打开同一节：窗口 1 保存成功后，窗口 2 用旧 baseVersion 保存 → 409 冲突弹窗
  await openAs(page, 'token-writer1', '/textbook/write/9001');
  await expect(page.getByRole('button', { name: '节 槽况判断' })).toBeVisible();
  const pageB = await context.newPage();
  await openAs(pageB, 'token-writer1', '/textbook/write/9001');

  // 窗口 A：改内容并保存成功
  await page.getByRole('button', { name: '节 槽况判断' }).click();
  await editor.click();
  await editor.fill('窗口A的修改内容');
  await page.getByRole('button', { name: '保存草稿' }).click();
  await expect(page.getByLabel('保存状态')).toContainText(/已(保存|于)/, { timeout: 10_000 });

  // 窗口 B（仍是旧版本）：改内容保存 → 409 冲突弹窗，不自动覆盖
  const editorB = pageB.locator('.cm-content');
  await pageB.getByRole('button', { name: '节 槽况判断' }).click();
  await editorB.click();
  await editorB.fill('窗口B的修改内容');
  await pageB.getByRole('button', { name: '保存草稿' }).click();
  await expect(pageB.getByText('保存冲突：服务端已有更新版本').first()).toBeVisible({ timeout: 15_000 });
  await expect(pageB.getByLabel('保存状态')).toContainText('冲突');
  // 选择继续编辑（不覆盖）；刷新后服务端内容仍是窗口 A 的（returned 节为可编辑态，从编辑器断言）
  await pageB.getByRole('button', { name: '继续编辑（保留我的内容）' }).click();
  await pageB.reload();
  await pageB.getByRole('button', { name: '节 槽况判断' }).click();
  await expect
    .poll(async () => await pageB.locator('.cm-content').textContent(), { timeout: 15_000 })
    .toContain('窗口A的修改内容');
  await pageB.close();
});

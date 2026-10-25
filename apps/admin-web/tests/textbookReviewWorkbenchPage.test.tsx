/**
 * TextbookReviewWorkbenchPage 测试 —— 《二期编写协同实施设计》§12.3
 * 用例：通过/打回；意见必填；撤销通过；只读正文；下一待审定节。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TextbookReviewWorkbenchPage } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/TextbookReviewWorkbenchPage';

// md-editor-rt 桩：审定工作台只读渲染 MdPreview
vi.mock('md-editor-rt', () => ({
  MdPreview: (props: { value?: string }) => <div data-testid="md-preview">{props.value}</div>,
}));

const api = vi.hoisted(() => ({
  pageCollaborators: vi.fn(),
  getCollaboration: vi.fn(),
  saveAssignments: vi.fn(),
  cancelAssignment: vi.fn(),
  completeProject: vi.fn(),
  reopenProject: vi.fn(),
  pageMyWritingTasks: vi.fn(),
  pageMyReviewTasks: vi.fn(),
  getWritingTaskSummary: vi.fn(),
  getReviewTaskSummary: vi.fn(),
  getChapterWorkspace: vi.fn(),
  getSectionContent: vi.fn(),
  saveSectionContent: vi.fn(),
  submitSectionContent: vi.fn(),
  reviewSectionContent: vi.fn(),
  pageSectionReviews: vi.fn(),
  pageSectionVersions: vi.fn(),
  getSectionVersion: vi.fn(),
  restoreSectionVersion: vi.fn(),
  uploadSectionAsset: vi.fn(),
  getSectionAssetContent: vi.fn(),
  deleteSectionAsset: vi.fn(),
  exportSectionDocx: vi.fn(),
  exportChapterDocx: vi.fn(),
  exportBookDocx: vi.fn(),
}));

vi.mock('../src/api/textbookCollaboration', () => api);

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (selector: (s: { hasPermission: (code: string) => boolean }) => unknown) =>
    selector({ hasPermission: () => true }),
}));

const ASSIGNMENT = {
  chapterId: '9001',
  writerId: '101',
  writerName: '张工',
  reviewerId: '202',
  reviewerName: '李工',
  dueDate: null,
  instruction: '结合现场操作写清步骤',
  active: true,
  taskStatus: 'reviewing',
  assignmentVersion: 1,
  sectionCount: 3,
  approvedCount: 1,
  submittedCount: 2,
  returnedCount: 0,
  draftCount: 0,
  untouchedCount: 0,
  assignedByName: '王教研',
  assignedAt: '2026-10-24 10:00:00',
};

const SECTIONS = [
  {
    sectionId: '9101',
    title: '更换阳极',
    sourceNo: '2.2',
    sortNo: 1,
    targetWords: 5000,
    contentExists: true,
    contentStatus: 'submitted' as const,
    wordCount: 4800,
    version: 4,
    submittedAt: '2026-10-24 11:00:00',
    submittedByName: '张工',
    submitRevision: 1,
    currentSnapshotId: '9501',
    latestReviewRecord: { action: 'submit' as const, submitRevision: 1, comment: null, actorName: '张工', createdAt: '2026-10-24 11:00:00' },
    points: [],
  },
  {
    sectionId: '9102',
    title: '槽况判断',
    sourceNo: '2.1',
    sortNo: 2,
    targetWords: 5000,
    contentExists: true,
    contentStatus: 'submitted' as const,
    wordCount: 1400,
    version: 5,
    submittedAt: '2026-10-24 12:00:00',
    submittedByName: '张工',
    submitRevision: 2,
    currentSnapshotId: '9502',
    latestReviewRecord: { action: 'submit' as const, submitRevision: 2, comment: null, actorName: '张工', createdAt: '2026-10-24 12:00:00' },
    points: [],
  },
  {
    sectionId: '9103',
    title: '抬母线',
    sourceNo: '2.3',
    sortNo: 3,
    targetWords: 3000,
    contentExists: true,
    contentStatus: 'approved' as const,
    wordCount: 2900,
    version: 8,
    submittedAt: '2026-10-23 09:00:00',
    submittedByName: '张工',
    submitRevision: 1,
    currentSnapshotId: '9503',
    latestReviewRecord: { action: 'approve' as const, submitRevision: 1, comment: '通过', actorName: '李工', createdAt: '2026-10-23 10:00:00' },
    points: [],
  },
];

const WORKSPACE = {
  projectId: '8001',
  projectStatus: 'writing',
  workTypeTitle: '铝电解工',
  direction: '槽操作工',
  levelCode: 5,
  chapterId: '9001',
  chapterTitle: '铝电解槽操作',
  chapterSourceNo: '2',
  assignment: ASSIGNMENT,
  relations: ['reviewer'],
  canWrite: false,
  canReview: true,
  readOnlyReason: null,
  sections: SECTIONS,
};

function w2(sectionId: string, overrides: Record<string, unknown> = {}) {
  const meta = SECTIONS.find((s) => s.sectionId === sectionId)!;
  return {
    sectionId,
    chapterId: '9001',
    projectId: '8001',
    sectionTitle: meta.title,
    chapterTitle: '铝电解槽操作',
    targetWords: meta.targetWords,
    contentExists: true,
    contentMd: `# ${meta.title}正文`,
    contentStatus: meta.contentStatus,
    wordCount: meta.wordCount,
    version: meta.version,
    relations: ['reviewer'],
    canWrite: false,
    canReview: true,
    readOnly: false,
    readOnlyReason: null,
    checkReport: null,
    assets: [],
    submittedBy: '101',
    submittedByName: '张工',
    submittedAt: meta.submittedAt,
    submitRevision: meta.submitRevision,
    currentSnapshotId: meta.currentSnapshotId,
    reviewedByName: meta.contentStatus === 'approved' ? '李工' : null,
    reviewedAt: null,
    latestReviewRecord: meta.latestReviewRecord,
    ...overrides,
  };
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <TextbookReviewWorkbenchPage chapterId="9001" />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

function clickSection(title: string) {
  fireEvent.click(screen.getByRole('button', { name: `节 ${title}` }));
}

async function fillModalComment(text: string) {
  const modal = document.querySelector('.ant-modal') as HTMLElement;
  const textarea = modal.querySelector('textarea') as HTMLTextAreaElement;
  fireEvent.change(textarea, { target: { value: text } });
}

describe('TextbookReviewWorkbenchPage', () => {
  beforeEach(() => {
    api.getChapterWorkspace.mockReset().mockResolvedValue(WORKSPACE);
    api.getSectionContent.mockReset().mockImplementation(async (id: string) => w2(id));
    api.reviewSectionContent.mockReset().mockImplementation(async (id: string, req: { action: string; baseVersion: number }) =>
      w2(id, {
        contentStatus: req.action === 'approve' ? 'approved' : 'returned',
        version: req.baseVersion + 1,
      }),
    );
    api.pageSectionReviews.mockReset().mockResolvedValue({
      items: [
        { id: 'r1', action: 'submit', submitRevision: 1, comment: null, actorName: '张工', createdAt: '2026-10-24 11:00:00' },
      ],
      page: 1,
      pageSize: 20,
      total: 1,
    });
    api.exportSectionDocx.mockReset();
  });

  it('只读正文：默认选中第一个待审定节，无编辑入口，展示提交信息与主操作', async () => {
    renderPage();
    expect(await screen.findByTestId('md-preview')).toBeInTheDocument();
    expect(screen.getByText('# 更换阳极正文')).toBeInTheDocument();
    // 审定人不能修改 Markdown：无 textarea/编辑器
    expect(document.querySelector('textarea')).toBeNull();
    // 提交信息 + 主操作平铺
    expect(screen.getByText('主笔提交人 张工')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /通\s*过/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /打\s*回/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '导出本节' })).toBeInTheDocument();
    // 下一待审定节按钮（还有一个 submitted 的槽况判断）
    expect(screen.getByRole('button', { name: /下一待审定节：槽况判断/ })).toBeInTheDocument();
  }, 30000);

  it('通过：意见选填，确认后调 W5 approve（带 baseVersion）', async () => {
    renderPage();
    await screen.findByTestId('md-preview');
    await userEvent.click(screen.getByRole('button', { name: /通\s*过/ }));
    await fillModalComment('内容完整，同意');
    await userEvent.click(screen.getByRole('button', { name: '确认通过' }));
    await waitFor(() =>
      expect(api.reviewSectionContent).toHaveBeenCalledWith('9101', {
        action: 'approve',
        comment: '内容完整，同意',
        baseVersion: 4,
      }),
    );
  }, 30000);

  it('打回意见必填：空意见不提交，填后提交 return', async () => {
    renderPage();
    await screen.findByTestId('md-preview');
    await userEvent.click(screen.getByRole('button', { name: /打\s*回/ }));

    // 空意见：校验失败，不调接口
    await userEvent.click(screen.getByRole('button', { name: '确认打回' }));
    expect(await screen.findByText('打回必须填写意见')).toBeInTheDocument();
    expect(api.reviewSectionContent).not.toHaveBeenCalled();

    // 填意见后提交
    await fillModalComment('缺少安全注意事项，请补充后重交');
    await userEvent.click(screen.getByRole('button', { name: '确认打回' }));
    await waitFor(() =>
      expect(api.reviewSectionContent).toHaveBeenCalledWith('9101', {
        action: 'return',
        comment: '缺少安全注意事项，请补充后重交',
        baseVersion: 4,
      }),
    );
  }, 30000);

  it('撤销通过：仅 approved 节显示，原因必填后调 reopen', async () => {
    renderPage();
    await screen.findByTestId('md-preview');
    // 默认节（submitted）不显示撤销通过
    expect(screen.queryByRole('button', { name: /撤销通过/ })).toBeNull();

    clickSection('抬母线');
    await waitFor(() => expect(screen.getByTestId('md-preview')).toHaveTextContent('# 抬母线正文'));
    await userEvent.click(screen.getByRole('button', { name: /撤销通过/ }));

    // 原因必填
    await userEvent.click(screen.getByRole('button', { name: '确认撤销' }));
    expect(await screen.findByText('撤销通过必须填写原因')).toBeInTheDocument();
    expect(api.reviewSectionContent).not.toHaveBeenCalled();

    await fillModalComment('发现遗漏引用，撤销后请补充');
    await userEvent.click(screen.getByRole('button', { name: '确认撤销' }));
    await waitFor(() =>
      expect(api.reviewSectionContent).toHaveBeenCalledWith('9103', {
        action: 'reopen',
        comment: '发现遗漏引用，撤销后请补充',
        baseVersion: 8,
      }),
    );
  }, 30000);

  it('下一待审定节：点击直接切到下一个 submitted 节，按钮指向其余待审定节', async () => {
    renderPage();
    await screen.findByTestId('md-preview');
    expect(screen.getByTestId('md-preview')).toHaveTextContent('# 更换阳极正文');
    await userEvent.click(screen.getByRole('button', { name: /下一待审定节：槽况判断/ }));
    await waitFor(() => expect(screen.getByTestId('md-preview')).toHaveTextContent('# 槽况判断正文'));
    // 切到 9102 后：9101 仍是待审定，按钮指向更换阳极
    expect(screen.getByRole('button', { name: /下一待审定节：更换阳极/ })).toBeInTheDocument();
  }, 30000);
});

/**
 * TextbookWritingWorkbenchPage 测试 —— 《二期编写协同实施设计》§12.3
 * 用例：自动保存防抖与串行（fake timers，含 30s 兜底）；Ctrl+S；离开 dirty 保护；
 *      409 冲突不覆盖；submitted/approved 只读；打回后可编辑；提交流程。
 *
 * md-editor-rt 以 textarea/div 桩替换（CodeMirror 在 jsdom 过重）；
 * API mock 用 vi.hoisted；慢用例显式超时。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TextbookWritingWorkbenchPage } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/TextbookWritingWorkbenchPage';

// ---- md-editor-rt 桩：编辑器 → 受控 textarea，预览 → div（vi.mock 工厂被提升，桩必须定义在工厂内部） ----
vi.mock('md-editor-rt', async () => {
  const React = await import('react');
  const MdEditorStub = React.forwardRef(function MdEditorStub(props: {
    value?: string;
    onChange?: (v: string) => void;
  }, ref: React.Ref<unknown>) {
    void ref;
    return (
      <textarea
        aria-label="md-editor"
        value={props.value ?? ''}
        onChange={(e) => props.onChange?.(e.target.value)}
      />
    );
  });
  return {
    MdEditor: MdEditorStub,
    MdPreview: (props: { value?: string }) => <div data-testid="md-preview">{props.value}</div>,
  };
});

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
  useAuthStore: (selector: (s: { hasPermission: (code: string) => boolean; currentUser?: { username: string } }) => unknown) =>
    selector({ hasPermission: () => true, currentUser: { username: 'zhangsan' } }),
}));

const ASSIGNMENT = {
  chapterId: '9001',
  writerId: '101',
  writerName: '张工',
  reviewerId: '202',
  reviewerName: '李工',
  dueDate: '2026-11-15',
  instruction: '结合现场操作写清步骤',
  active: true,
  taskStatus: 'writing',
  assignmentVersion: 1,
  sectionCount: 3,
  approvedCount: 0,
  submittedCount: 1,
  returnedCount: 1,
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
    contentStatus: 'draft' as const,
    wordCount: 4800,
    version: 3,
    submittedAt: null,
    submittedByName: null,
    submitRevision: null,
    currentSnapshotId: null,
    latestReviewRecord: null,
    points: [
      { pointId: '9201', titleRaw: '换极表知识', dimension: 'theory' as const, sortNo: 1 },
    ],
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
    submittedAt: '2026-10-24 11:00:00',
    submittedByName: '张工',
    submitRevision: 1,
    currentSnapshotId: '9501',
    latestReviewRecord: { action: 'submit' as const, submitRevision: 1, comment: null, actorName: '张工', createdAt: '2026-10-24 11:00:00' },
    points: [],
  },
  {
    sectionId: '9103',
    title: '抬母线',
    sourceNo: '2.3',
    sortNo: 3,
    targetWords: 3000,
    contentExists: true,
    contentStatus: 'returned' as const,
    wordCount: 900,
    version: 7,
    submittedAt: '2026-10-23 09:00:00',
    submittedByName: '张工',
    submitRevision: 2,
    currentSnapshotId: '9502',
    latestReviewRecord: { action: 'return' as const, submitRevision: 2, comment: '缺少安全注意事项', actorName: '李工', createdAt: '2026-10-23 10:00:00' },
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
  relations: ['writer'],
  canWrite: true,
  canReview: false,
  readOnlyReason: null,
  sections: SECTIONS,
};

function w2(sectionId: string, overrides: Record<string, unknown> = {}) {
  const meta = SECTIONS.find((s) => s.sectionId === sectionId)!;
  const editable = meta.contentStatus === 'draft' || meta.contentStatus === 'returned';
  return {
    sectionId,
    chapterId: '9001',
    projectId: '8001',
    sectionTitle: meta.title,
    chapterTitle: '铝电解槽操作',
    targetWords: meta.targetWords,
    contentExists: true,
    contentMd:
      sectionId === '9101' ? '# 初稿内容' : sectionId === '9102' ? '# 已提交内容' : '# 被打回的内容',
    contentStatus: meta.contentStatus,
    wordCount: meta.wordCount,
    version: meta.version,
    relations: ['writer'],
    canWrite: editable,
    canReview: false,
    readOnly: !editable,
    readOnlyReason: editable ? null : '本节已提交，等待李工审定',
    checkReport: null,
    assets: [],
    submittedBy: '101',
    submittedByName: '张工',
    submittedAt: meta.submittedAt,
    submitRevision: meta.submitRevision,
    currentSnapshotId: meta.currentSnapshotId,
    reviewedByName: null,
    reviewedAt: null,
    latestReviewRecord: meta.latestReviewRecord,
    ...overrides,
  };
}

function conflict409() {
  return {
    response: {
      status: 409,
      data: {
        code: '409',
        message: '内容版本冲突',
        data: {
          reason: 'VERSION_CONFLICT',
          currentVersion: 6,
          currentContentMd: '# 服务端内容',
          currentUpdatedAt: '2026-10-24 12:00:00',
          currentUpdatedByName: '其他人',
        },
      },
    },
  };
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <TextbookWritingWorkbenchPage chapterId="9001" />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

function clickSection(title: string) {
  fireEvent.click(screen.getByRole('button', { name: `节 ${title}` }));
}

describe('TextbookWritingWorkbenchPage', () => {
  beforeEach(() => {
    vi.useRealTimers();
    api.getChapterWorkspace.mockReset().mockResolvedValue(WORKSPACE);
    api.getSectionContent.mockReset().mockImplementation(async (id: string) => w2(id));
    api.saveSectionContent.mockReset().mockImplementation(async (_id: string, req: { contentMd: string }) =>
      w2('9101', { contentMd: req.contentMd, version: 99 }),
    );
    api.submitSectionContent.mockReset().mockImplementation(async (id: string, baseVersion: number) =>
      w2(id, { contentStatus: 'submitted', canWrite: false, readOnly: true, readOnlyReason: '本节已提交，等待李工审定', version: baseVersion + 1 }),
    );
    api.exportSectionDocx.mockReset();
    api.pageSectionVersions.mockReset().mockResolvedValue({ items: [], page: 1, pageSize: 20, total: 0 });
  });

  afterEach(() => {
    vi.useRealTimers();
    localStorage.clear();
  });

  it('默认选中第一个未完成节（9101 draft），左栏显示章信息与节状态', async () => {
    renderPage();
    const editor = await screen.findByLabelText('md-editor');
    expect(editor).toHaveValue('# 初稿内容');
    expect(screen.getByText('总进度：0/3 节已通过')).toBeInTheDocument();
    expect(screen.getByText(/审定人 李工/)).toBeInTheDocument();
    expect(screen.getByText(/结合现场操作写清步骤/)).toBeInTheDocument();
    // 左栏三个节均可见（打回节有红点 Tag）
    expect(screen.getByRole('button', { name: '节 更换阳极' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '节 槽况判断' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '节 抬母线' })).toBeInTheDocument();
    expect(screen.getByText('打回')).toBeInTheDocument();
  }, 30000);

  it('自动保存防抖与串行：3 秒防抖触发；保存未返回期间不并发，返回后串行补发最新内容', async () => {
    renderPage();
    const editor = await screen.findByLabelText('md-editor');

    vi.useFakeTimers();
    let resolveFirst!: (v: unknown) => void;
    const firstPending = new Promise((r) => {
      resolveFirst = r;
    });
    api.saveSectionContent.mockReset();
    api.saveSectionContent.mockReturnValueOnce(firstPending);
    api.saveSectionContent.mockReturnValueOnce(Promise.resolve(w2('9101', { contentMd: '# 第二版', version: 4 })));

    fireEvent.change(editor, { target: { value: '# 第一版' } });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2999);
    });
    expect(api.saveSectionContent).not.toHaveBeenCalled();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1);
    });
    expect(api.saveSectionContent).toHaveBeenCalledTimes(1);
    expect(api.saveSectionContent).toHaveBeenCalledWith('9101', { contentMd: '# 第一版', baseVersion: 3 });

    // 第一个保存尚未返回：继续输入并再过 3 秒 → 串行，不并发
    fireEvent.change(editor, { target: { value: '# 第二版' } });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(api.saveSectionContent).toHaveBeenCalledTimes(1);

    // 第一个返回 → 串行补发第二次（带最新内容 + 新 baseVersion）
    await act(async () => {
      resolveFirst(w2('9101', { contentMd: '# 第一版', version: 4 }));
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(api.saveSectionContent).toHaveBeenCalledTimes(2);
    expect(api.saveSectionContent).toHaveBeenLastCalledWith('9101', { contentMd: '# 第二版', baseVersion: 4 });
    vi.useRealTimers();
  }, 30000);

  it('连续输入时 30 秒兜底保存一次（防抖一直被重置也强制落盘）', async () => {
    renderPage();
    const editor = await screen.findByLabelText('md-editor');

    vi.useFakeTimers();
    api.saveSectionContent.mockReset();
    api.saveSectionContent.mockImplementation(async (_id: string, req: { contentMd: string }) =>
      w2('9101', { contentMd: req.contentMd, version: 99 }),
    );

    for (let i = 0; i < 10; i++) {
      fireEvent.change(editor, { target: { value: `# 内容${i}` } });
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2900);
      });
    }
    expect(api.saveSectionContent).not.toHaveBeenCalled(); // 3 秒防抖不断被重置

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1001);
    });
    expect(api.saveSectionContent).toHaveBeenCalledTimes(1); // 30 秒兜底到点
    expect(api.saveSectionContent).toHaveBeenCalledWith('9101', { contentMd: '# 内容9', baseVersion: 3 });
    vi.useRealTimers();
  }, 30000);

  it('Ctrl+S 立即手动保存', async () => {
    renderPage();
    const editor = await screen.findByLabelText('md-editor');
    fireEvent.change(editor, { target: { value: '# 手动保存' } });
    fireEvent.keyDown(window, { key: 's', ctrlKey: true });
    await waitFor(() =>
      expect(api.saveSectionContent).toHaveBeenCalledWith('9101', { contentMd: '# 手动保存', baseVersion: 3 }),
    );
  }, 30000);

  it('离开 dirty 保护：保存失败弹「继续留下 / 放弃」，选择留下不切节', async () => {
    renderPage();
    const editor = await screen.findByLabelText('md-editor');

    vi.useFakeTimers();
    api.saveSectionContent.mockReset();
    api.saveSectionContent.mockRejectedValue({ message: 'network down' });

    fireEvent.change(editor, { target: { value: '# 未保存内容' } });
    clickSection('抬母线');

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    // fake timers 下 findByText 的 waitFor 会挂起，直接同步断言（Modal 已在微任务后渲染；标题在 aria 与可见节点各出现一次）
    expect(screen.getAllByText('当前内容尚未保存成功').length).toBeGreaterThanOrEqual(1);
    fireEvent.click(screen.getByRole('button', { name: '继续留下' }));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    // 未切换：仍在编辑 9101，内容保留（当前节高亮）
    expect(screen.getByLabelText('md-editor')).toHaveValue('# 未保存内容');
    expect(screen.getByRole('button', { name: '节 更换阳极' })).toHaveStyle({ background: '#e6f4ff' });
    vi.useRealTimers();
  }, 30000);

  it('409 冲突不覆盖：弹服务端版本信息；「加载服务端最新」前本地内容保持', async () => {
    renderPage();
    const editor = await screen.findByLabelText('md-editor');

    vi.useFakeTimers();
    api.saveSectionContent.mockReset();
    api.saveSectionContent.mockRejectedValueOnce(conflict409());

    fireEvent.change(editor, { target: { value: '# 我的版本' } });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });

    expect(screen.getAllByText('保存冲突：服务端已有更新版本').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/服务端版本 v6/)).toBeInTheDocument();
    expect(screen.getByText(/更新人：其他人/)).toBeInTheDocument();
    // 不自动覆盖：编辑器仍是本地内容
    expect(screen.getByLabelText('md-editor')).toHaveValue('# 我的版本');

    fireEvent.click(screen.getByRole('button', { name: '下载我的草稿为 .md' }));
    // 下载后仍不覆盖
    expect(screen.getByLabelText('md-editor')).toHaveValue('# 我的版本');

    fireEvent.click(screen.getByRole('button', { name: '加载服务端最新' }));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(screen.getByLabelText('md-editor')).toHaveValue('# 服务端内容');
    vi.useRealTimers();
  }, 30000);

  it('submitted 只读：编辑器替换为只读预览，无保存/提交按钮，显示只读原因', async () => {
    renderPage();
    await screen.findByLabelText('md-editor');
    clickSection('槽况判断');
    expect(await screen.findByTestId('md-preview')).toBeInTheDocument();
    expect(screen.getByText('# 已提交内容')).toBeInTheDocument();
    expect(screen.queryByLabelText('md-editor')).toBeNull();
    expect(screen.queryByRole('button', { name: '保存草稿' })).toBeNull();
    expect(screen.queryByRole('button', { name: /提交审定/ })).toBeNull();
    expect(screen.getByText('本节已提交，等待李工审定')).toBeInTheDocument();
    // 最近审定意见（右栏）
    expect(screen.getByText('提交审定（第 1 次提交）')).toBeInTheDocument();
  }, 30000);

  it('打回后可编辑：returned 节显示打回意见并可继续输入', async () => {
    renderPage();
    await screen.findByLabelText('md-editor');
    clickSection('抬母线');
    // 切换是异步（confirmLeave 微任务）：初始拿到的是旧节编辑器，等新编辑器内容就位（重新查询避免拿到已卸载元素）
    await waitFor(() => expect(screen.getByLabelText('md-editor')).toHaveValue('# 被打回的内容'));
    const editor = screen.getByLabelText('md-editor');
    expect(screen.getAllByText(/缺少安全注意事项/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: '保存草稿' })).toBeInTheDocument();
    fireEvent.change(editor, { target: { value: '# 补充安全注意事项' } });
    expect(screen.getByLabelText('md-editor')).toHaveValue('# 补充安全注意事项');
  }, 30000);

  it('提交流程：先保存最新草稿，再以保存后的 baseVersion 提交', async () => {
    renderPage();
    const editor = await screen.findByLabelText('md-editor');
    fireEvent.change(editor, { target: { value: '# 定稿' } });

    await userEvent.click(screen.getByRole('button', { name: /提交审定/ }));
    // Popconfirm 确认（okText=提交审定，位于弹出层内；与主按钮同名，故限定在 popover 内查找）
    const popover = await waitFor(() => {
      const el = document.querySelector('.ant-popover');
      expect(el).toBeTruthy();
      return el as HTMLElement;
    });
    await userEvent.click(within(popover).getByRole('button', { name: /提交审定/ }));

    await waitFor(() => expect(api.saveSectionContent).toHaveBeenCalledWith('9101', { contentMd: '# 定稿', baseVersion: 3 }));
    await waitFor(() => expect(api.submitSectionContent).toHaveBeenCalledWith('9101', 99));
    // 提交成功后切只读
    expect(await screen.findByText('本节已提交，等待李工审定')).toBeInTheDocument();
    expect(screen.queryByLabelText('md-editor')).toBeNull();
  }, 30000);
});

/**
 * TextbookMyTasksPage 测试 —— 《二期编写协同实施设计》§12.3
 * 用例：编写/审定两种模式；筛选参数；逾期；空态；只显示 API 返回的本人任务。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TextbookMyTasksPage } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/TextbookMyTasksPage';

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

function task(overrides: Record<string, unknown> = {}) {
  return {
    chapterId: '9001',
    writerId: '101',
    writerName: '张三',
    reviewerId: '202',
    reviewerName: '李四',
    dueDate: '2026-11-15',
    instruction: null,
    active: true,
    taskStatus: 'writing',
    assignmentVersion: 1,
    sectionCount: 4,
    approvedCount: 1,
    submittedCount: 1,
    returnedCount: 0,
    draftCount: 1,
    untouchedCount: 1,
    assignedByName: '王教研',
    assignedAt: '2026-10-24 10:00:00',
    projectId: '8001',
    workTypeTitle: '铝电解工',
    direction: '槽操作工',
    levelCode: 5,
    chapterTitle: '铝电解槽操作',
    chapterSourceNo: '2',
    projectStatus: 'writing',
    latestSectionUpdatedAt: '2026-10-24 11:00:00',
    actualWords: 6200,
    targetWords: 30000,
    overdue: false,
    ...overrides,
  };
}

function renderPage(mode: 'writing' | 'review' = 'writing') {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <TextbookMyTasksPage mode={mode} />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

describe('TextbookMyTasksPage', () => {
  beforeEach(() => {
    api.pageMyWritingTasks.mockReset().mockResolvedValue({
      items: [task(), task({ chapterId: '9002', chapterTitle: '工作准备', taskStatus: 'returned', overdue: true, dueDate: '2026-09-01' })],
      page: 1,
      pageSize: 20,
      total: 2,
    });
    api.pageMyReviewTasks.mockReset().mockResolvedValue({
      items: [task({ chapterId: '9003', chapterTitle: '职业道德', taskStatus: 'reviewing', submittedCount: 2 })],
      page: 1,
      pageSize: 20,
      total: 1,
    });
    api.getWritingTaskSummary.mockReset().mockResolvedValue({
      assignedCount: 2,
      writingCount: 3,
      pendingReviewCount: 1,
      returnedCount: 4,
      approvedCount: 5,
    });
    api.getReviewTaskSummary.mockReset().mockResolvedValue({
      pendingCount: 2,
      returnedCount: 1,
      partialApprovedCount: 3,
      approvedCount: 4,
    });
  });

  it('编写模式：T1 列表 + T3 统计卡（全量口径，不等于当前页条数）', async () => {
    renderPage('writing');
    expect(await screen.findByText('我的编写任务')).toBeInTheDocument();
    // 只显示 API 返回的本人任务（9001/9002 两章，无伪造行）
    expect(await screen.findByText(/铝电解槽操作/)).toBeInTheDocument();
    expect(screen.getByText(/工作准备/)).toBeInTheDocument();
    expect(screen.queryByText(/职业道德/)).toBeNull();
    expect(screen.getByText('共 2 条')).toBeInTheDocument();
    // 统计卡数字来自 T3（相交口径，非当前页合计：2+3+1+4+5 ≠ 列表 total 2）
    expect(screen.getByTestId('summary-assigned')).toHaveTextContent('2');
    expect(screen.getByTestId('summary-writing')).toHaveTextContent('3');
    expect(screen.getByTestId('summary-pendingReview')).toHaveTextContent('1');
    expect(screen.getByTestId('summary-returned')).toHaveTextContent('4');
    expect(screen.getByTestId('summary-approved')).toHaveTextContent('5');
    await waitFor(() => expect(api.getWritingTaskSummary).toHaveBeenCalled());
    await waitFor(() => expect(api.pageMyWritingTasks).toHaveBeenCalledWith(expect.objectContaining({ page: 1, pageSize: 20 })));
  }, 30000);

  it('审定模式：T2 默认 status=pending + T4 统计卡 4 张', async () => {
    renderPage('review');
    expect(await screen.findByText('我的审定任务')).toBeInTheDocument();
    await waitFor(() =>
      expect(api.pageMyReviewTasks).toHaveBeenCalledWith(expect.objectContaining({ status: 'pending' })),
    );
    expect(screen.getByText(/职业道德/)).toBeInTheDocument();
    expect(screen.getByText('2 节待审定')).toBeInTheDocument();
    expect(screen.getByTestId('summary-pending')).toHaveTextContent('2');
    expect(screen.getByTestId('summary-partial')).toHaveTextContent('3');
    await waitFor(() => expect(api.getReviewTaskSummary).toHaveBeenCalled());
  }, 30000);

  it('筛选参数：状态/关键字/逾期切换反映到 API 入参', async () => {
    renderPage('writing');
    await screen.findByText(/铝电解槽操作/);

    // 状态筛选
    fireEvent.mouseDown(document.querySelector('.ant-select-selector')!);
    const option = await screen.findByText('已打回', { selector: '.ant-select-item-option-content' });
    await userEvent.click(option.closest('.ant-select-item-option')!);
    await waitFor(() =>
      expect(api.pageMyWritingTasks).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'returned', page: 1, pageSize: 20 }),
      ),
    );

    // 关键字
    const keywordInput = screen.getByPlaceholderText('教材/章名称');
    await userEvent.type(keywordInput, '阳极');
    await waitFor(() =>
      expect(api.pageMyWritingTasks).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'returned', keyword: '阳极' }),
      ),
    );

    // 只看逾期
    await userEvent.click(screen.getByRole('button', { name: /只看逾期/ }));
    await waitFor(() =>
      expect(api.pageMyWritingTasks).toHaveBeenLastCalledWith(
        expect.objectContaining({ overdueOnly: true, keyword: '阳极', status: 'returned' }),
      ),
    );
  }, 30000);

  it('逾期：overdue=true 行显示红字「已逾期」', async () => {
    renderPage('writing');
    const row = await screen.findByText(/工作准备/);
    expect(row.closest('tr')).toHaveTextContent('已逾期');
    // 未逾期行不显示
    expect(screen.getByText(/铝电解槽操作/).closest('tr')).not.toHaveTextContent('已逾期');
  }, 30000);

  it('空态：无任务与筛选无结果区分', async () => {
    api.pageMyWritingTasks.mockResolvedValue({ items: [], page: 1, pageSize: 20, total: 0 });
    renderPage('writing');
    expect(await screen.findByText('还没有分配编写任务')).toBeInTheDocument();

    // 设置筛选后仍无结果 → 当前筛选无结果
    await userEvent.click(screen.getByRole('button', { name: /只看逾期/ }));
    expect(await screen.findByText('当前筛选无结果')).toBeInTheDocument();
  }, 30000);

  it('操作列：writing 章显示「继续编写」，点击进入 /textbook/write/:chapterId', async () => {
    renderPage('writing');
    const row = (await screen.findByText(/铝电解槽操作/)).closest('tr') as HTMLElement;
    const pushStateSpy = vi.spyOn(window.history, 'pushState').mockImplementation(() => {});
    const dispatchSpy = vi.spyOn(window, 'dispatchEvent').mockImplementation(() => true);
    await userEvent.click(within(row).getByRole('button', { name: '继续编写' }));
    expect(pushStateSpy).toHaveBeenCalledWith({}, '', '/textbook/write/9001');
    pushStateSpy.mockRestore();
    dispatchSpy.mockRestore();
  }, 30000);
});

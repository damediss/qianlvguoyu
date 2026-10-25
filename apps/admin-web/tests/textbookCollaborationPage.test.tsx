/**
 * TextbookCollaborationPage 测试 —— 《二期编写协同实施设计》§12.3
 * 用例：章/节进度渲染；分配候选人加载；批量分配提交；改派提示；完成条件不足禁用；manage 权限显隐。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TextbookCollaborationPage } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/TextbookCollaborationPage';

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

let permittedCodes: string[] = [];

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (selector: (s: { hasPermission: (code: string) => boolean }) => unknown) =>
    selector({ hasPermission: (code: string) => permittedCodes.includes(code) }),
}));

const WRITERS = {
  items: [
    { id: '101', displayName: '张工', username: 'zhang', orgName: '电解车间' },
    { id: '102', displayName: '王工', username: 'wang', orgName: '铸造车间' },
  ],
  page: 1,
  pageSize: 20,
  total: 2,
};

const REVIEWERS = {
  items: [{ id: '202', displayName: '李工', username: 'li', orgName: '质检科' }],
  page: 1,
  pageSize: 20,
  total: 1,
};

const BASE_ASSIGNMENT = {
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
  sectionCount: 4,
  approvedCount: 1,
  submittedCount: 1,
  returnedCount: 0,
  draftCount: 1,
  untouchedCount: 1,
  assignedByName: '王教研',
  assignedAt: '2026-10-24 10:00:00',
};

function collab(overrides: Record<string, unknown> = {}) {
  return {
    project: { id: '8001', status: 'writing', workTypeTitle: '铝电解工', direction: '槽操作工', levelCode: 5 },
    outlineSummary: { chapterSum: 100000, totalWords: 100000, balanced: true },
    progress: {
      chapterCount: 2,
      assignedChapterCount: 1,
      sectionCount: 8,
      approvedSectionCount: 1,
      actualWords: 6200,
      targetWords: 100000,
    },
    completion: { canComplete: false, blockers: [{ code: 'SECTION_NOT_APPROVED', chapterId: '9001', sectionId: '9101', message: '「更换阳极」尚未通过' }] },
    chapters: [
      {
        chapterId: '9001',
        title: '铝电解槽操作',
        sourceNo: '2',
        targetWords: 50000,
        assignment: BASE_ASSIGNMENT,
        sections: [
          {
            sectionId: '9101',
            title: '更换阳极',
            sourceNo: '2.2',
            sortNo: 2,
            contentExists: true,
            contentStatus: 'submitted',
            wordCount: 4800,
            targetWords: 5000,
            version: 4,
            submittedAt: '2026-10-24 11:00:00',
            latestReviewRecord: null,
          },
          {
            sectionId: '9102',
            title: '槽况判断',
            sourceNo: '2.1',
            sortNo: 1,
            contentExists: true,
            contentStatus: 'approved',
            wordCount: 1400,
            targetWords: 5000,
            version: 6,
            submittedAt: '2026-10-23 11:00:00',
            latestReviewRecord: { action: 'approve', submitRevision: 1, comment: '可以', actorName: '李工', createdAt: '2026-10-23 12:00:00' },
          },
        ],
      },
      {
        chapterId: '9002',
        title: '工作准备',
        sourceNo: '1',
        targetWords: 15000,
        assignment: null,
        sections: [
          {
            sectionId: '9201',
            title: '操作前准备',
            sourceNo: '1.1',
            sortNo: 1,
            contentExists: false,
            contentStatus: null,
            wordCount: 0,
            targetWords: 8000,
            version: 0,
            submittedAt: null,
            latestReviewRecord: null,
          },
        ],
      },
    ],
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
        <TextbookCollaborationPage projectId="8001" />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

async function openAssignModalForRow(rowText: string) {
  const row = screen.getByText(new RegExp(rowText)).closest('tr') as HTMLElement;
  // 行内按钮：未分配章为「分 配」、已分配章为「改 派」（antd 两字按钮自动加空格），排除「取消分配」
  await userEvent.click(within(row).getByRole('button', { name: /^(分\s*配|改\s*派)$/ }));
  await screen.findAllByText('输入姓名/用户名搜索');
}

async function pickSelect(selectIndex: number, optionText: string | RegExp) {
  // Modal 内第 N 个 Select（0=主笔，1=审定人）
  const modal = document.querySelector('.ant-modal');
  const selects = modal?.querySelectorAll('.ant-select');
  expect(selects?.length).toBeGreaterThanOrEqual(selectIndex + 1);
  fireEvent.mouseDown(selects![selectIndex].querySelector('.ant-select-selector')!);
  const option = await screen.findByText(optionText, { selector: '.ant-select-item-option-content' });
  // antd option 内容节点：按文本找最近的 option
  const optionEl = option.closest('.ant-select-item-option') as HTMLElement;
  await userEvent.click(optionEl);
}

describe('TextbookCollaborationPage', () => {
  beforeEach(() => {
    permittedCodes = ['textbook:project:view', 'textbook:project:manage', 'textbook:project:export'];
    api.getCollaboration.mockReset().mockResolvedValue(collab());
    api.pageCollaborators.mockReset().mockImplementation(async (params: { role: string }) =>
      params.role === 'writer' ? WRITERS : REVIEWERS,
    );
    api.saveAssignments.mockReset().mockResolvedValue([BASE_ASSIGNMENT]);
    api.cancelAssignment.mockReset().mockResolvedValue({ ...BASE_ASSIGNMENT, active: false });
    api.completeProject.mockReset().mockResolvedValue({ id: '8001', status: 'done' });
    api.reopenProject.mockReset().mockResolvedValue({ id: '8001', status: 'writing' });
    api.getSectionContent.mockReset();
    api.exportChapterDocx.mockReset();
    api.exportBookDocx.mockReset();
  });

  it('章/节进度渲染：总进度、章分配率、章行进度与展开节状态', async () => {
    renderPage();
    expect(await screen.findByText(/铝电解槽操作/)).toBeInTheDocument();
    // 顶部进度概览
    expect(screen.getByText('1/8')).toBeInTheDocument(); // 节通过率
    expect(screen.getByText('1/2')).toBeInTheDocument(); // 章分配率
    // 章行：主笔/审定人/进度
    expect(screen.getByText('张工')).toBeInTheDocument();
    expect(screen.getByText('李工')).toBeInTheDocument();
    // 完成条件未满足 + blockers
    expect(screen.getByText('「更换阳极」尚未通过')).toBeInTheDocument();

    // 展开第一章查看节
    const chapterRow = screen.getByText(/铝电解槽操作/).closest('tr') as HTMLElement;
    await userEvent.click(within(chapterRow).getByRole('button', { name: /展开/ }));
    // 展开节渲染（「更换阳极」同时出现在 blocker 提示里，用 getAllByText）
    expect((await screen.findAllByText(/更换阳极/)).length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText(/槽况判断/)).toBeInTheDocument();
    expect(screen.getByText('1/4')).toBeInTheDocument(); // 章进度 approved/section
  }, 30000);

  it('分配候选人加载：打开弹窗时按角色预载 C1', async () => {
    renderPage();
    await screen.findByText(/工作准备/);
    await openAssignModalForRow('工作准备');
    await waitFor(() =>
      expect(api.pageCollaborators).toHaveBeenCalledWith(
        expect.objectContaining({ role: 'writer', page: 1, pageSize: 20 }),
      ),
    );
    await waitFor(() =>
      expect(api.pageCollaborators).toHaveBeenCalledWith(
        expect.objectContaining({ role: 'reviewer', page: 1, pageSize: 20 }),
      ),
    );
    // 无手机号字段、候选人按「姓名（用户名）/组织」展示
    await pickSelect(0, /张工/);
    expect(document.querySelector('.ant-modal')?.textContent).toContain('zhang');
  }, 30000);

  it('批量分配提交：勾选两章后一次 PUT items=2', async () => {
    renderPage();
    await screen.findByText(/工作准备/);

    // 勾选两个章行（tbody 行内 checkbox，排除表头全选）
    const checkboxes = document.querySelectorAll('tbody .ant-table-selection-column .ant-checkbox-input');
    expect(checkboxes.length).toBe(2);
    await userEvent.click(checkboxes[0]);
    await userEvent.click(checkboxes[1]);

    await userEvent.click(screen.getByRole('button', { name: /批量分配/ }));
    await screen.findByText('批量分配 2 章');
    await pickSelect(0, /张工/);
    await pickSelect(1, /李工/);

    // 下一步 → 确认页列出将影响章数 → 提交
    await userEvent.click(screen.getByRole('button', { name: /下一步/ }));
    expect(await screen.findByText('2 章')).toBeInTheDocument();
    expect(screen.getByText(/铝电解槽操作、工作准备/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /确认提交/ }));

    await waitFor(() => expect(api.saveAssignments).toHaveBeenCalledTimes(1));
    const arg = api.saveAssignments.mock.calls[0];
    expect(arg[0]).toBe('8001');
    expect(arg[1]).toHaveLength(2);
    expect(arg[1][0]).toMatchObject({ chapterId: '9001', writerId: '101', reviewerId: '202', assignmentVersion: 1 });
    expect(arg[1][1]).toMatchObject({ chapterId: '9002', writerId: '101', reviewerId: '202', assignmentVersion: null });
  }, 30000);

  it('改派提示：已有提交/通过内容的章提示「改派不改变已提交/已通过状态」', async () => {
    renderPage();
    await screen.findByText(/铝电解槽操作/);
    await openAssignModalForRow('铝电解槽操作');
    expect(await screen.findByText('改派不改变已提交/已通过状态')).toBeInTheDocument();
    // 已存在的分配带当前 assignmentVersion
    await pickSelect(0, /王工/);
    await pickSelect(1, /李工/);
    await userEvent.click(screen.getByRole('button', { name: /下一步/ }));
    await userEvent.click(await screen.findByRole('button', { name: /确认提交/ }));
    await waitFor(() =>
      expect(api.saveAssignments).toHaveBeenCalledWith('8001', [
        expect.objectContaining({ chapterId: '9001', writerId: '102', assignmentVersion: 1 }),
      ]),
    );
  }, 30000);

  it('完成条件不足禁用：完成编写 disabled + blockers 展示；满足后可点击并调 C5', async () => {
    const { unmount } = renderPage();
    await screen.findByText(/铝电解槽操作/);
    const completeBtn = screen.getByRole('button', { name: '完成编写' });
    expect(completeBtn).toBeDisabled();
    expect(screen.getByText('「更换阳极」尚未通过')).toBeInTheDocument();
    unmount();

    // 满足条件：可点击，Popconfirm 确认后调 C5
    api.getCollaboration.mockResolvedValue(
      collab({
        project: { id: '8001', status: 'writing', workTypeTitle: '铝电解工', direction: '槽操作工', levelCode: 5 },
        completion: { canComplete: true, blockers: [] },
      }),
    );
    renderPage();
    const enabledBtn = await screen.findByRole('button', { name: '完成编写' });
    expect(enabledBtn).toBeEnabled();
    await userEvent.click(enabledBtn);
    expect(await screen.findByText(/确认完成编写/)).toBeInTheDocument();
    // Popconfirm 确认按钮（okText=完成编写，位于弹出层内）
    const popover = document.querySelector('.ant-popover') as HTMLElement;
    await userEvent.click(within(popover).getByRole('button', { name: /完成编写/ }));
    await waitFor(() => expect(api.completeProject).toHaveBeenCalledWith('8001'));
  }, 30000);

  it('manage 权限显隐：仅 view 时分配/取消分配/批量分配/完成编写均不出现', async () => {
    permittedCodes = ['textbook:project:view', 'textbook:project:export'];
    renderPage();
    await screen.findByText(/铝电解槽操作/);
    expect(screen.queryByRole('button', { name: /^(分\s*配|改\s*派)$/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /取消分配/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /批量分配/ })).toBeNull();
    expect(screen.queryByRole('button', { name: '完成编写' })).toBeNull();
    // 导出审阅稿仍可见（project:export）
    expect(screen.getByRole('button', { name: /导出审阅稿/ })).toBeInTheDocument();
  }, 30000);
});

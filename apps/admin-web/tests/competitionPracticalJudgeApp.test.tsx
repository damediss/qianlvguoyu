import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import {
  completeCompetitionPracticalJudgeSession,
  confirmCompetitionPracticalJudgeStart,
  createCompetitionPracticalRealtimeTicket,
  listCompetitionPracticalJudgeTasks,
  saveCompetitionPracticalJudgeTask,
  selectCompetitionPracticalJudgeActiveStage,
  startCompetitionPracticalJudgeSession,
} from '../src/api/competition';
import { CompetitionPracticalJudgeApp } from '../src/pages/competition/CompetitionPracticalJudgeApp';
import type { CompetitionPracticalJudgeTask } from '../src/types/competition';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: Record<string, unknown>) => unknown) => select({
    token: 'judge-token',
    tenantId: '10159',
    currentUser: { id: '88', displayName: '裁判一号' },
    clearAuth: vi.fn(),
  }),
}));

vi.mock('../src/api/auth', () => ({ logout: vi.fn() }));

vi.mock('../src/api/competition', () => ({
  completeCompetitionPracticalJudgeSession: vi.fn(),
  confirmCompetitionPracticalJudgeStart: vi.fn(),
  createCompetitionPracticalRealtimeTicket: vi.fn(),
  listCompetitionPracticalJudgeTasks: vi.fn(),
  openCompetitionPracticalJudgeTask: vi.fn(),
  saveCompetitionPracticalJudgeTask: vi.fn(),
  selectCompetitionPracticalJudgeActiveStage: vi.fn(),
  startCompetitionPracticalJudgeSession: vi.fn(),
}));

const judgeTask: CompetitionPracticalJudgeTask = {
  assignmentId: '501',
  sessionId: '301',
  eventId: '20',
  eventName: '技能大赛',
  stageId: '10',
  stageName: '初赛',
  workTypeName: '电工',
  levelName: '高级工',
  contestantMemberId: '2001',
  contestantNo: 'C001',
  contestantName: '选手甲',
  refereeMemberId: '1001',
  workstationCode: 'P-01',
  status: 'assigned',
  assessmentSheetId: '900',
  scoreSheetId: '901',
  scoreSheetStatus: 'draft',
  totalScore: '20',
  judgeRole: 'leader',
  startStatus: 'started',
  confirmedJudges: 3,
  requiredJudges: 3,
  currentJudgeConfirmed: true,
  activeStageId: '10',
  assessmentSheet: {
    id: '900',
    eventId: '20',
    stageId: '10',
    eventWorkTypeId: '30',
    eventWorkTypeLevelId: '40',
    paperSnapshotId: '500',
    versionNo: 1,
    sheetCode: 'P1-500',
    title: '竞赛实操考评表',
    questionCount: 1,
    totalScore: '20',
    status: 'published',
    sheet: {
      questions: [{
        paperQuestionId: '7001',
        sortOrder: 1,
        title: '设备启动与检查',
        score: '20',
        scoringMethod: 'deductive',
        detail: {
          assessmentRequirementsHtml: '<p>按规程完成启动检查</p>',
          candidatePrepareHtml: '<p>佩戴安全防护用品</p>',
          examRoomPrepareHtml: '<p>检查设备供电</p>',
        },
        scoreItems: [{
          itemNo: 1,
          itemName: '启动前检查',
          criterion: '漏检一项扣0.5分',
          maxScore: 20,
          maxDeduction: 20,
          deductionStep: 0.5,
          scoringMethod: 'deductive',
        }],
      }],
    },
  },
};

describe('竞赛实操裁判 WebView', () => {
  it('复用考评员端平板布局和大按钮扣分交互，并自动保存草稿', async () => {
    const user = userEvent.setup();
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks).mockResolvedValue([judgeTask]);
    vi.mocked(saveCompetitionPracticalJudgeTask).mockResolvedValue(judgeTask);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { container } = render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('裁判一号，请独立完成本人评分')).toBeInTheDocument();
    expect(await screen.findByText('按规程完成启动检查')).toBeInTheDocument();
    expect(container.querySelector('.assessor-tablet-main')).toBeInTheDocument();
    expect(container.querySelector('.assessor-task-list')).toBeInTheDocument();
    expect(container.querySelector('.assessor-question-nav')).toBeInTheDocument();
    expect(container.querySelector('.assessor-submit-bar')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /扣 0.5 分/ }));
    expect(screen.getAllByText('19.5 / 20 分')).toHaveLength(3);

    await waitFor(() => expect(saveCompetitionPracticalJudgeTask).toHaveBeenCalledWith(
      '501',
      [expect.objectContaining({
        paperQuestionId: '7001',
        paperItemNo: 1,
        score: 19.5,
        deduction: 0.5,
      })],
      false,
    ));
    expect(screen.getByRole('button', { name: /提交本题/ })).toBeInTheDocument();
  });

  it('三台裁判设备分别确认后，仅组长显示并执行开考操作', async () => {
    const user = userEvent.setup();
    const waitingTask = {
      ...judgeTask,
      scoreSheetId: null,
      judgeRole: 'leader' as const,
      startStatus: 'waiting' as const,
      confirmedJudges: 2,
      currentJudgeConfirmed: false,
    };
    const readyTask = {
      ...waitingTask,
      startStatus: 'ready' as const,
      confirmedJudges: 3,
      currentJudgeConfirmed: true,
    };
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks).mockResolvedValue([waitingTask]);
    vi.mocked(confirmCompetitionPracticalJudgeStart).mockResolvedValue(readyTask);
    vi.mocked(startCompetitionPracticalJudgeSession).mockResolvedValue({
      ...readyTask,
      startStatus: 'started',
      status: 'in_progress',
    });

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('裁判确认 2/3')).toBeInTheDocument();
    expect(screen.getByText('裁判组组长')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '等待三台设备确认' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: '我已核对选手与试卷，确认就绪' }));
    await waitFor(() => expect(confirmCompetitionPracticalJudgeStart).toHaveBeenCalledWith('501'));
    await user.click(await screen.findByRole('button', { name: '组长开始考试' }));
    await waitFor(() => expect(startCompetitionPracticalJudgeSession).toHaveBeenCalledWith('501'));
  });

  it('组员完成确认后仍无权执行组长开考', async () => {
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks).mockResolvedValue([{
      ...judgeTask,
      scoreSheetId: null,
      judgeRole: 'member',
      startStatus: 'ready',
      confirmedJudges: 3,
      currentJudgeConfirmed: true,
    }]);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('裁判组组员')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '组长开始考试' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '本设备已确认' })).toBeDisabled();
  });

  it('三名裁判逐题提交完成后，仅组长可以完成考试并提交实操成绩', async () => {
    const user = userEvent.setup();
    const awaitingTask: CompetitionPracticalJudgeTask = {
      ...judgeTask,
      status: 'completed',
      scoreSheetStatus: 'submitted_locked',
      startStatus: 'awaiting_leader',
    };
    const completedTask: CompetitionPracticalJudgeTask = {
      ...awaitingTask,
      startStatus: 'completed',
    };
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks).mockResolvedValue([awaitingTask]);
    vi.mocked(completeCompetitionPracticalJudgeSession).mockResolvedValue(completedTask);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('三名裁判均已提交，请完成考试')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '完成考试' }));
    expect((await screen.findAllByText('确认完成考试并提交实操成绩？')).length).toBeGreaterThan(0);
    const confirmButtons = screen.getAllByRole('button', { name: '完成考试' });
    await user.click(confirmButtons[confirmButtons.length - 1]);
    await waitFor(() => expect(completeCompetitionPracticalJudgeSession).toHaveBeenCalledWith('501'));
  });

  it('分差异常时组长端提前显示原因并禁止完成考试', async () => {
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks).mockResolvedValue([{
      ...judgeTask,
      status: 'completed',
      scoreSheetStatus: 'submitted_locked',
      startStatus: 'awaiting_leader',
      scoreDifferenceLimit: '10',
      actualScoreDifference: '15',
      scoreDifferenceStatus: 'exceeded',
    }]);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('裁判评分分差异常：15 分，允许 10 分')).toBeInTheDocument();
    expect(screen.getByText(/请在实操比赛控制台核对三名裁判原始分/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '完成考试' })).toBeDisabled();
  });

  it('自动保存和提交只针对当前试题，不会把未操作题目一并提交', async () => {
    const user = userEvent.setup();
    const twoQuestionTask: CompetitionPracticalJudgeTask = {
      ...judgeTask,
      assessmentSheet: {
        ...judgeTask.assessmentSheet,
        questionCount: 2,
        totalScore: '40',
        sheet: {
          questions: [
            judgeTask.assessmentSheet.sheet.questions![0],
            {
              paperQuestionId: '7002',
              sortOrder: 2,
              title: '故障判断',
              score: '20',
              scoringMethod: 'deductive',
              scoreItems: [{
                itemNo: 1,
                itemName: '判断步骤',
                criterion: '漏判一项扣0.5分',
                maxScore: 20,
                maxDeduction: 20,
                deductionStep: 0.5,
                scoringMethod: 'deductive',
              }],
            },
          ],
        },
      },
    };
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks).mockResolvedValue([twoQuestionTask]);
    vi.mocked(saveCompetitionPracticalJudgeTask).mockResolvedValue(twoQuestionTask);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    await user.click(await screen.findByRole('button', { name: /扣 0.5 分/ }));
    await waitFor(() => expect(saveCompetitionPracticalJudgeTask).toHaveBeenCalled());
    const firstSave = vi.mocked(saveCompetitionPracticalJudgeTask).mock.calls.at(-1);
    expect(firstSave?.[1]).toEqual([expect.objectContaining({ paperQuestionId: '7001' })]);
    expect(firstSave?.[1]).toHaveLength(1);
    expect(screen.getAllByText('评分中').length).toBeGreaterThan(0);
  });

  it('组长可在上栏下拉切换赛段任务，组员只展示组长选中的赛段', async () => {
    const user = userEvent.setup();
    const finalTask: CompetitionPracticalJudgeTask = {
      ...judgeTask,
      assignmentId: '601',
      sessionId: '401',
      stageId: '11',
      stageName: '决赛',
      contestantMemberId: '2002',
      contestantNo: 'C002',
      contestantName: '选手乙',
      activeStageId: '10',
    };
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks).mockResolvedValue([judgeTask, finalTask]);
    vi.mocked(selectCompetitionPracticalJudgeActiveStage).mockImplementation(async (stageId) => ([
      { ...judgeTask, activeStageId: String(stageId) },
      { ...finalTask, activeStageId: String(stageId) },
    ]));

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    expect(await screen.findByTestId('judge-stage-switch')).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.getAllByText('选手甲').length).toBeGreaterThan(0);
    expect(screen.queryByText('选手乙')).not.toBeInTheDocument();

    await user.click(screen.getByRole('combobox'));
    await user.click(await screen.findByText('技能大赛 · 电工 · 高级工 · 决赛'));
    await waitFor(() => expect(selectCompetitionPracticalJudgeActiveStage).toHaveBeenCalledWith('11'));
    expect((await screen.findAllByText('选手乙')).length).toBeGreaterThan(0);
    expect(screen.queryByText('选手甲')).not.toBeInTheDocument();
  });

  it('组员端只展示组长当前赛段，并随刷新同步切换', async () => {
    const user = userEvent.setup();
    const memberTask: CompetitionPracticalJudgeTask = {
      ...judgeTask,
      judgeRole: 'member',
      activeStageId: '10',
    };
    const memberFinalTask: CompetitionPracticalJudgeTask = {
      ...judgeTask,
      assignmentId: '601',
      sessionId: '401',
      stageId: '11',
      stageName: '决赛',
      contestantMemberId: '2002',
      contestantNo: 'C002',
      contestantName: '选手乙',
      judgeRole: 'member',
      activeStageId: '11',
    };
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip websocket'));
    vi.mocked(listCompetitionPracticalJudgeTasks)
      .mockResolvedValueOnce([memberTask, { ...memberFinalTask, activeStageId: '10' }])
      .mockResolvedValue([memberFinalTask]);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalJudgeApp />
      </QueryClientProvider>,
    );

    expect(await screen.findByTestId('judge-stage-current')).toBeInTheDocument();
    expect(screen.queryByTestId('judge-stage-switch')).not.toBeInTheDocument();
    expect(screen.getAllByText('选手甲').length).toBeGreaterThan(0);
    expect(screen.queryByText('选手乙')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '刷新评分任务' }));
    expect((await screen.findAllByText('选手乙')).length).toBeGreaterThan(0);
    expect(screen.queryByText('选手甲')).not.toBeInTheDocument();
    expect(screen.getAllByText('技能大赛 · 电工 · 高级工 · 决赛').length).toBeGreaterThan(0);
  });
});

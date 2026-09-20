import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import {
  confirmCompetitionPracticalJudgeStart,
  createCompetitionPracticalRealtimeTicket,
  listCompetitionPracticalJudgeTasks,
  saveCompetitionPracticalJudgeTask,
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
  confirmCompetitionPracticalJudgeStart: vi.fn(),
  createCompetitionPracticalRealtimeTicket: vi.fn(),
  listCompetitionPracticalJudgeTasks: vi.fn(),
  openCompetitionPracticalJudgeTask: vi.fn(),
  saveCompetitionPracticalJudgeTask: vi.fn(),
  startCompetitionPracticalJudgeSession: vi.fn(),
}));

const judgeTask: CompetitionPracticalJudgeTask = {
  assignmentId: '501',
  sessionId: '301',
  contestantMemberId: '2001',
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
});

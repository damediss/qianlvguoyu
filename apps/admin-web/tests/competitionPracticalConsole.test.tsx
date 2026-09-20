import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import {
  createCompetitionPracticalRealtimeTicket,
  getCompetitionPracticalConsole,
  listCompetitionEventWorkTypeLevels,
  pageCompetitionStages,
} from '../src/api/competition';
import {
  CompetitionPracticalConsolePage,
  groupConsoleContestants,
} from '../src/pages/competition/CompetitionPracticalConsolePage';
import type { CompetitionPracticalConsoleTask } from '../src/types/competition';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: {
    permissionCodes: string[];
    token?: string;
    tenantId?: string;
  }) => unknown) => select({
    permissionCodes: [
      'competition:practical-console:view',
      'competition:practical-console:manage',
    ],
    token: 'token',
    tenantId: '10159',
  }),
}));

vi.mock('../src/api/competition', () => ({
  createCompetitionPracticalRealtimeTicket: vi.fn(),
  getCompetitionPracticalConsole: vi.fn(),
  initializeCompetitionPracticalConsole: vi.fn(),
  listCompetitionEventWorkTypeLevels: vi.fn(),
  pageCompetitionStages: vi.fn(),
}));

function task(partial: Partial<CompetitionPracticalConsoleTask>): CompetitionPracticalConsoleTask {
  return {
    assignmentId: '501',
    sessionId: '300',
    contestantMemberId: '500',
    refereeMemberId: '1001',
    assessmentSheetId: '900',
    workstationCode: 'P-1',
    taskStatus: 'assigned',
    scoreSheetStatus: 'submitted_locked',
    totalScore: '100.0',
    contestantNo: 'C-001',
    contestantName: '张三',
    refereeName: '认定测试001',
    assessmentSheetTitle: '实操考评表',
    questions: [
      { paperQuestionId: '1', sortOrder: 1, title: '试题1', maxScore: '35', score: '35', status: 'submitted_locked' },
      { paperQuestionId: '2', sortOrder: 2, title: '试题2', maxScore: '35', score: '35', status: 'submitted_locked' },
    ],
    ...partial,
  };
}

describe('实操比赛控制台选手展开卡片', () => {
  it('按选手聚合三名裁判任务', () => {
    const rows = groupConsoleContestants([
      task({ assignmentId: '501', refereeMemberId: '1001', refereeName: '认定测试001' }),
      task({ assignmentId: '502', refereeMemberId: '1002', refereeName: '认定测试002' }),
      task({ assignmentId: '503', refereeMemberId: '1003', refereeName: '认定测试003' }),
    ]);
    expect(rows).toHaveLength(1);
    expect(rows[0].contestantName).toBe('张三');
    expect(rows[0].submittedSheets).toBe(3);
    expect(rows[0].totalQuestions).toBe(2);
    expect(rows[0].completedQuestions).toBe(2);
    expect(rows[0].referees).toHaveLength(3);
    expect(rows[0].status).toBe('completed');
    expect(rows[0].totalScore).toBe(100);
  });

  it('评分中的草稿分数不算试题完成', () => {
    const scoringQuestion = (id: string): NonNullable<CompetitionPracticalConsoleTask['questions']>[number] => ({
      paperQuestionId: id,
      sortOrder: Number(id),
      title: `试题${id}`,
      maxScore: '35',
      score: '35',
      status: 'scoring',
    });
    const rows = groupConsoleContestants([
      task({
        assignmentId: '501',
        refereeMemberId: '1001',
        scoreSheetStatus: 'draft',
        totalScore: '70',
        questions: [scoringQuestion('1'), scoringQuestion('2')],
      }),
      task({
        assignmentId: '502',
        refereeMemberId: '1002',
        scoreSheetStatus: 'draft',
        totalScore: '70',
        questions: [scoringQuestion('1'), scoringQuestion('2')],
      }),
      task({
        assignmentId: '503',
        refereeMemberId: '1003',
        scoreSheetStatus: 'draft',
        totalScore: '70',
        questions: [scoringQuestion('1'), scoringQuestion('2')],
      }),
    ]);
    expect(rows[0].submittedSheets).toBe(0);
    expect(rows[0].completedQuestions).toBe(0);
    expect(rows[0].status).toBe('scoring');
    expect(rows[0].totalScore).toBeNull();
  });

  it('列表按选手展示，展开后显示三张裁判评分卡', async () => {
    vi.mocked(createCompetitionPracticalRealtimeTicket).mockRejectedValue(new Error('skip ws'));
    vi.mocked(listCompetitionEventWorkTypeLevels).mockResolvedValue([
      { id: '40', levelCode: '5', levelName: '初级工' },
    ] as never);
    vi.mocked(pageCompetitionStages).mockResolvedValue({
      items: [{ id: '10', name: '资格赛', status: 'initialized' }],
      total: 1,
      page: 1,
      size: 500,
    } as never);
    vi.mocked(getCompetitionPracticalConsole).mockResolvedValue({
      planId: '200',
      eventId: '20',
      stageId: '10',
      eventWorkTypeId: '30',
      eventWorkTypeLevelId: '40',
      sessionCount: 1,
      contestantCount: 1,
      scoringTaskCount: 3,
      openedSheetCount: 3,
      submittedSheetCount: 3,
      status: 'initialized',
      tasks: [
        task({ assignmentId: '501', refereeMemberId: '1001', refereeName: '认定测试001' }),
        task({ assignmentId: '502', refereeMemberId: '1002', refereeName: '认定测试002' }),
        task({ assignmentId: '503', refereeMemberId: '1003', refereeName: '认定测试003' }),
      ],
    } as never);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalConsolePage
          eventId="20"
          eventWorkTypeId="30"
          eventWorkTypeLevelCode="5"
          initialStageId="10"
        />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('张三')).toBeInTheDocument();
    expect(screen.getByText('选手已提交 1 / 1 人')).toBeInTheDocument();
    expect(screen.queryByText('裁判')).not.toBeInTheDocument();
    expect(await screen.findByText('认定测试001')).toBeInTheDocument();
    expect(screen.getByText('认定测试002')).toBeInTheDocument();
    expect(screen.getByText('认定测试003')).toBeInTheDocument();
    expect(screen.getAllByText('试题1').length).toBeGreaterThan(0);
  });
});

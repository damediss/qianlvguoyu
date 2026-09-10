import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { message } from 'antd';
import { CompetitionFormalExamConsolePage } from '../src/pages/competition/CompetitionFormalExamConsolePage';
import { CompetitionFormalExamSessionPanel } from '../src/pages/competition/CompetitionFormalExamSessionPanel';
import {
  getCompetitionFormalCandidateExam,
  getCompetitionFormalCandidateResult,
  getCompetitionFormalExamSession,
  getCompetitionFormalExamSessionByStage,
  getCompetitionFormalExamSessionPreflight,
  pageCompetitionStages,
  pageCompetitionFormalCandidateExams,
  startCompetitionFormalExamSession,
  forceSubmitCompetitionFormalCandidateExam,
  forceOfflineCompetitionFormalCandidateExam,
  scoreCompetitionFormalExamSession,
} from '../src/api/competition';
import type { CompetitionFormalExamSession } from '../src/types/competition';

const auth = {
  permissionCodes: ['competition:exam-session:force-submit'],
};

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: { permissionCodes: string[] }) => unknown) => select(auth),
}));

vi.mock('../src/api/competition', () => ({
  pageCompetitionFormalCandidateExams: vi.fn().mockResolvedValue({ items: [], page: 1, pageSize: 20, total: 0 }),
  pageCompetitionStages: vi.fn(),
  getCompetitionFormalExamSessionByStage: vi.fn(),
  getCompetitionFormalExamSessionPreflight: vi.fn().mockResolvedValue({ ready: true, checks: [] }),
  getCompetitionFormalExamSession: vi.fn(),
  startCompetitionFormalExamSession: vi.fn(),
  forceSubmitCompetitionFormalCandidateExam: vi.fn(),
  forceOfflineCompetitionFormalCandidateExam: vi.fn(),
  createFormalExamConsoleRealtimeTicket: vi.fn().mockResolvedValue({
    ticket: 'console-ticket',
    expiresIn: 45,
    wsPath: '/api/ws/competition/formal-exam/console',
    heartbeatSeconds: 20,
  }),
  getCompetitionFormalCandidateExam: vi.fn().mockResolvedValue({
    id: '1', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
    candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
    paperCode: 'A', paperName: 'A卷', status: 'waiting_enter', answeredCount: 0, totalQuestionCount: 100,
  }),
  getCompetitionFormalCandidateResult: vi.fn(),
  scoreCompetitionFormalExamSession: vi.fn().mockResolvedValue({}),
}));

const session: CompetitionFormalExamSession = {
  id: '9',
  eventId: '20',
  eventName: '技能大赛',
  stageId: '10',
  stageName: '初赛',
  eventWorkTypeId: '30',
  workTypeName: '电工',
  eventWorkTypeLevelId: '40',
  levelCode: '3',
  levelName: '高级工',
  examType: 'theory',
  status: 'ready',
  durationSeconds: 5400,
  candidateTotal: 2,
  submittedTotal: 0,
  waitingEnterCount: 2,
  readyCount: 0,
  runningCount: 0,
  submittedCount: 0,
  autoSubmittedCount: 0,
  forcedSubmittedCount: 0,
  paperCount: 2,
  papers: [
    {
      id: '11', sourcePaperId: '500', paperCode: 'A', paperName: 'A卷',
      snapshotVersion: 1, questionCount: 100, candidateCount: 1,
    },
    {
      id: '12', sourcePaperId: '501', paperCode: 'B', paperName: 'B卷',
      snapshotVersion: 1, questionCount: 100, candidateCount: 1,
    },
  ],
};

function renderPanel(current: CompetitionFormalExamSession | null = session) {
  if (current) {
    vi.mocked(getCompetitionFormalExamSession).mockResolvedValue(current);
  }
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    client,
    ...render(
      <QueryClientProvider client={client}>
        <CompetitionFormalExamSessionPanel
          open
          loading={false}
          session={current}
          onClose={() => undefined}
        />
      </QueryClientProvider>,
    ),
  };
}

describe('正式比赛控制台', () => {
  afterEach(() => {
    message.destroy();
    auth.permissionCodes = ['competition:exam-session:force-submit'];
  });
  it('shows session status, paper distribution and backend preflight checks', async () => {
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true,
      checks: [
        { code: 'SESSION_STATUS', passed: true, message: '正式比赛实例已准备完成' },
        { code: 'CANDIDATE_COUNT', passed: true, message: '2 名选手考试实例完整' },
      ],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [{
        id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
        candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
        paperCode: 'A', paperName: 'A卷', status: 'waiting_enter', answeredCount: 0, totalQuestionCount: 100,
      }],
      page: 1, pageSize: 20, total: 1,
    });
    renderPanel();
    expect(await screen.findByText('理论考试 · 正式比赛控制台')).toBeInTheDocument();
    expect(screen.getAllByText('准备完成').length).toBeGreaterThan(0);
    expect(screen.getByText('技能大赛')).toBeInTheDocument();
    expect(await screen.findByText('正式比赛实例已准备完成')).toBeInTheDocument();
    expect(screen.getByText('当前状态：可以进入选手候考阶段')).toBeInTheDocument();
    expect(await screen.findByText('张三')).toBeInTheDocument();
    expect(screen.getByText('0 / 100')).toBeInTheDocument();
    expect(screen.getAllByText(/A卷/).length).toBeGreaterThan(0);
  });

  it('shows failed preflight and cancelled banner without inventing a start action', async () => {
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: false,
      checks: [
        { code: 'SESSION_STATUS', passed: false, message: '本场正式比赛已取消' },
      ],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [], page: 1, pageSize: 20, total: 0,
    });
    renderPanel({ ...session, status: 'cancelled' });
    expect(await screen.findByText('本场正式比赛已取消')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /开始比赛/ })).not.toBeInTheDocument();
  });

  it('filters candidates by keyword, paper and status, and opens runtime detail', { timeout: 15_000 }, async () => {
    const user = userEvent.setup();
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true,
      checks: [{ code: 'SESSION_STATUS', passed: true, message: '正式比赛实例已准备完成' }],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [{
        id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
        candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
        paperCode: 'A', paperName: 'A卷', status: 'waiting_enter', answeredCount: 0, totalQuestionCount: 100,
      }],
      page: 1, pageSize: 20, total: 1,
    });
    vi.mocked(getCompetitionFormalCandidateExam).mockResolvedValue({
      id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
      candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
      paperCode: 'A', paperName: 'A卷', status: 'waiting_enter', answeredCount: 0, totalQuestionCount: 100,
    });
    renderPanel();
    await screen.findByText('张三');
    await user.type(screen.getByPlaceholderText('搜索编号或姓名'), '张三');
    await user.keyboard('{Enter}');
    await waitFor(() => expect(pageCompetitionFormalCandidateExams).toHaveBeenCalledWith('9', expect.objectContaining({
      keyword: '张三',
    })));
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '试卷筛选' }));
    await user.click(await screen.findByText((content, element) => (
      element?.classList.contains('ant-select-item-option-content') && content === 'A卷 · A卷'
    )));
    await waitFor(() => expect(pageCompetitionFormalCandidateExams).toHaveBeenCalledWith('9', expect.objectContaining({
      paperSnapshotId: '11',
    })));
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '考试状态' }));
    await user.click(await screen.findByText((content, element) => (
      element?.classList.contains('ant-select-item-option-content') && content === '待进入'
    )));
    await waitFor(() => expect(pageCompetitionFormalCandidateExams).toHaveBeenCalledWith('9', expect.objectContaining({
      status: 'waiting_enter',
    })));
    await user.click(screen.getByRole('button', { name: '查看' }));
    expect(await screen.findByText('contestantProfileId')).toBeInTheDocument();
    expect(screen.getByText('CandidateExam ID')).toBeInTheDocument();
    expect(screen.queryByText(/正确答案|评分/)).not.toBeInTheDocument();
  });

  it('does not refetch session by id on first open when parent already provided it', async () => {
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true,
      checks: [{ code: 'SESSION_STATUS', passed: true, message: '正式比赛实例已准备完成' }],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [], page: 1, pageSize: 20, total: 0,
    });
    renderPanel({
      ...session,
      waitingEnterCount: 47,
      readyCount: 1,
    });
    expect(await screen.findByText('已候考')).toBeInTheDocument();
    expect(getCompetitionFormalExamSession).not.toHaveBeenCalled();
  });

  it('shows waitingEnterCount and readyCount from the live session refresh', async () => {
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true,
      checks: [{ code: 'SESSION_STATUS', passed: true, message: '正式比赛实例已准备完成' }],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [], page: 1, pageSize: 20, total: 0,
    });
    renderPanel({
      ...session,
      waitingEnterCount: 47,
      readyCount: 1,
    });
    expect(await screen.findByText('已候考')).toBeInTheDocument();
    expect(document.querySelector('.competition-formal-exam-session-panel__stat-waiting')).toHaveTextContent('47');
    expect(document.querySelector('.competition-formal-exam-session-panel__stat-ready')).toHaveTextContent('1');
  });

  it('shows start button when ready and preflight passes, and disables it without ready candidates', async () => {
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true,
      checks: [{ code: 'SESSION_STATUS', passed: true, message: '正式比赛实例已准备完成' }],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [], page: 1, pageSize: 20, total: 0,
    });
    renderPanel(session);
    expect(await screen.findByRole('button', { name: '开始比赛' })).toBeDisabled();
  });

  it('confirms waiting-enter count then starts and refreshes running state', async () => {
    const user = userEvent.setup();
    const readySession = {
      ...session,
      waitingEnterCount: 2,
      readyCount: 46,
      candidateTotal: 48,
    };
    const runningSession = {
      ...readySession,
      status: 'running' as const,
      waitingEnterCount: 2,
      readyCount: 0,
      runningCount: 46,
      actualStartTime: '2026-09-08 09:00:00',
      deadlineTime: '2026-09-08 10:30:00',
    };
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true,
      checks: [{ code: 'SESSION_STATUS', passed: true, message: '正式比赛实例已准备完成' }],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [], page: 1, pageSize: 20, total: 0,
    });
    renderPanel(readySession);
    let live = readySession;
    vi.mocked(getCompetitionFormalExamSession).mockImplementation(async () => live);
    vi.mocked(startCompetitionFormalExamSession).mockImplementation(async () => {
      live = runningSession;
      return runningSession;
    });
    await user.click(await screen.findByRole('button', { name: '开始比赛' }));
    expect(await screen.findByText('当前仍有 2 名选手未进入候考。')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '确认开始' }));
    await waitFor(() => expect(startCompetitionFormalExamSession).toHaveBeenCalledWith('9', {
      confirmWaitingCandidates: true,
    }));
    expect(await screen.findByText('实际开始：09:00:00')).toBeInTheDocument();
    expect(screen.getByText('预计截止：10:30:00')).toBeInTheDocument();
    expect(screen.getAllByText('比赛中').length).toBeGreaterThan(0);
  });

  it('shows force-submit only for running candidates with permission', async () => {
    auth.permissionCodes = ['competition:exam-session:force-submit'];
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true,
      checks: [],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [
        {
          id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
          candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'running', answeredCount: 12, totalQuestionCount: 100,
        },
        {
          id: '102', examSessionId: '9', candidateId: '101', contestantProfileId: '101',
          candidateNo: '002', candidateName: '李四', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'waiting_enter', answeredCount: 0, totalQuestionCount: 100,
        },
        {
          id: '103', examSessionId: '9', candidateId: '102', contestantProfileId: '102',
          candidateNo: '003', candidateName: '王五', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'submitted', answeredCount: 20, totalQuestionCount: 100,
        },
        {
          id: '104', examSessionId: '9', candidateId: '103', contestantProfileId: '103',
          candidateNo: '004', candidateName: '赵六', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'auto_submitted', answeredCount: 18, totalQuestionCount: 100,
        },
        {
          id: '105', examSessionId: '9', candidateId: '104', contestantProfileId: '104',
          candidateNo: '005', candidateName: '钱七', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'forced_submitted', answeredCount: 9, totalQuestionCount: 100,
        },
      ],
      page: 1, pageSize: 20, total: 5,
    });
    renderPanel({
      ...session,
      status: 'running',
      waitingEnterCount: 1,
      readyCount: 0,
      runningCount: 1,
      submittedCount: 1,
      autoSubmittedCount: 1,
      forcedSubmittedCount: 1,
    });
    expect(await screen.findByText('张三')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: '强制交卷' })).toHaveLength(1);
    expect(screen.getAllByText('自动交卷').length).toBeGreaterThan(0);
    expect(screen.getAllByText('强制交卷').length).toBeGreaterThan(0);
  });

  it('shows absent stats, finished banner and finish blockers without start or force', async () => {
    auth.permissionCodes = ['competition:exam-session:force-submit'];
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: false,
      checks: [{ code: 'SESSION_STATUS', passed: false, message: '本场正式比赛已结束，不能再进入候考' }],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [{
        id: '106', examSessionId: '9', candidateId: '105', contestantProfileId: '105',
        candidateNo: '006', candidateName: '周八', paperSnapshotId: '11', sourcePaperId: '500',
        paperCode: 'A', paperName: 'A卷', status: 'absent', answeredCount: 0, totalQuestionCount: 100,
        absentTime: '2026-09-08T10:30:00',
        absentReason: '考试截止前未进入本场考试',
      }],
      page: 1, pageSize: 20, total: 1,
    });
    renderPanel({
      ...session,
      status: 'finished',
      waitingEnterCount: 0,
      readyCount: 0,
      runningCount: 0,
      submittedCount: 1,
      autoSubmittedCount: 1,
      forcedSubmittedCount: 1,
      absentCount: 1,
      actualStartTime: '2026-09-08T09:00:00',
      actualEndTime: '2026-09-08T10:30:00',
    });
    expect(await screen.findByText('本场正式比赛已结束')).toBeInTheDocument();
    expect(screen.getAllByText('已结束').length).toBeGreaterThan(0);
    expect(screen.getByText('实际结束：10:30:00')).toBeInTheDocument();
    expect(screen.getAllByText('缺考').length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: '开始比赛' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '强制交卷' })).not.toBeInTheDocument();
    expect(screen.queryByText('开考前检查未通过')).not.toBeInTheDocument();
    expect(await screen.findByText('周八')).toBeInTheDocument();
  });

  it('shows ready finish blocker after deadline while session stays running', async () => {
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [{
        id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
        candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
        paperCode: 'A', paperName: 'A卷', status: 'ready', answeredCount: 0, totalQuestionCount: 100,
      }],
      page: 1, pageSize: 20, total: 1,
    });
    renderPanel({
      ...session,
      status: 'running',
      waitingEnterCount: 0,
      readyCount: 1,
      runningCount: 0,
      submittedCount: 1,
      finishEligible: false,
      finishBlockers: [{
        code: 'READY_CANDIDATE_REMAINS',
        count: 1,
        message: '存在 1 名选手处于异常 READY 状态，需要人工处理后才能结束本场比赛',
      }],
    });
    expect(await screen.findByText('场次结束受阻')).toBeInTheDocument();
    expect(screen.getByText(/存在 1 名选手处于异常 READY 状态/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '开始比赛' })).not.toBeInTheDocument();
  });

  it('requires a force-submit reason before confirming', async () => {
    const user = userEvent.setup();
    auth.permissionCodes = ['competition:exam-session:force-submit'];
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true, checks: [],
    });
    vi.mocked(forceSubmitCompetitionFormalCandidateExam).mockResolvedValue({
      id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
      candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
      paperCode: 'A', paperName: 'A卷', status: 'forced_submitted', answeredCount: 12, totalQuestionCount: 100,
      submitType: 'forced_submitted',
      forceSubmitReason: '现场设备异常，裁判确认结束考试',
      forceSubmittedBy: '1000',
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [{
        id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
        candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
        paperCode: 'A', paperName: 'A卷', status: 'running', answeredCount: 12, totalQuestionCount: 100,
      }],
      page: 1, pageSize: 20, total: 1,
    });
    renderPanel({ ...session, status: 'running', runningCount: 1 });
    await user.click(await screen.findByRole('button', { name: '强制交卷' }));
    expect(await screen.findByText(/强制交卷将立即锁定服务器已经接收的答案/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '确认强制交卷' })).toBeDisabled();
    expect(forceSubmitCompetitionFormalCandidateExam).not.toHaveBeenCalled();
    await user.type(screen.getByLabelText('强制交卷原因'), '现场设备异常，裁判确认结束考试');
    expect(screen.getByRole('button', { name: '确认强制交卷' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: '确认强制交卷' }));
    await waitFor(() => expect(forceSubmitCompetitionFormalCandidateExam).toHaveBeenCalledWith('9', '101', {
      reason: '现场设备异常，裁判确认结束考试',
    }));
  });

  it('shows evict for logged-in candidates and returns them to the login page', async () => {
    const user = userEvent.setup();
    auth.permissionCodes = ['competition:exam-session:force-offline'];
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true, checks: [],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [
        {
          id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
          candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'submitted', answeredCount: 20, totalQuestionCount: 100,
        },
        {
          id: '102', examSessionId: '9', candidateId: '101', contestantProfileId: '101',
          candidateNo: '002', candidateName: '李四', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'waiting_enter', answeredCount: 0, totalQuestionCount: 100,
        },
      ],
      page: 1, pageSize: 20, total: 2,
    });
    vi.mocked(forceOfflineCompetitionFormalCandidateExam).mockResolvedValue(undefined);
    renderPanel({ ...session, status: 'running', waitingEnterCount: 1, submittedCount: 1 });
    expect(await screen.findByText('张三')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: '清退' })).toHaveLength(2);
    expect(screen.queryByRole('button', { name: '强制交卷' })).not.toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: '清退' })[0]);
    expect(await screen.findByText(/清退只会强制该选手考试账号下线/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '确认清退' }));
    await waitFor(() => expect(forceOfflineCompetitionFormalCandidateExam).toHaveBeenCalledWith('9', '101'));
  });

  it('hides force-submit without the dedicated permission', async () => {
    auth.permissionCodes = ['competition:exam-session:view'];
    vi.mocked(getCompetitionFormalExamSessionPreflight).mockResolvedValue({
      ready: true, checks: [],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [{
        id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
        candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
        paperCode: 'A', paperName: 'A卷', status: 'running', answeredCount: 12, totalQuestionCount: 100,
      }],
      page: 1, pageSize: 20, total: 1,
    });
    renderPanel({ ...session, status: 'running', runningCount: 1 });
    expect(await screen.findByText('张三')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '强制交卷' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '清退' })).not.toBeInTheDocument();
  });

  it('shows pending scoring status after the session is finished', async () => {
    auth.permissionCodes = ['competition:exam-session:score'];
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [],
      page: 1, pageSize: 20, total: 0,
    });
    renderPanel({
      ...session,
      status: 'finished',
      scoringStatus: 'not_started',
      waitingEnterCount: 0,
      readyCount: 0,
      runningCount: 0,
    });
    expect(await screen.findByText('本场正式比赛已结束')).toBeInTheDocument();
    expect(screen.getAllByText('已结束').length).toBeGreaterThan(0);
    expect(screen.getAllByText('待评分').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: '重新执行评分' })).toBeInTheDocument();
  });

  it('shows scoring in progress without a retry button', async () => {
    auth.permissionCodes = ['competition:exam-session:score'];
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [],
      page: 1, pageSize: 20, total: 0,
    });
    renderPanel({
      ...session,
      status: 'finished',
      scoringStatus: 'scoring',
      waitingEnterCount: 0,
    });
    expect(await screen.findAllByText('评分中')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: '重新执行评分' })).not.toBeInTheDocument();
  });

  it('shows scored and absent results after scoring completes', async () => {
    auth.permissionCodes = ['competition:exam-session:view'];
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [
        {
          id: '101', examSessionId: '9', candidateId: '100', contestantProfileId: '100',
          candidateNo: '001', candidateName: '张三', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'submitted', answeredCount: 98, totalQuestionCount: 100,
          resultStatus: 'scored', totalScore: '86.50', fullScore: '100.00',
        },
        {
          id: '106', examSessionId: '9', candidateId: '105', contestantProfileId: '105',
          candidateNo: '006', candidateName: '周八', paperSnapshotId: '11', sourcePaperId: '500',
          paperCode: 'A', paperName: 'A卷', status: 'absent', answeredCount: 0, totalQuestionCount: 100,
          resultStatus: 'absent', totalScore: null, fullScore: null,
        },
      ],
      page: 1, pageSize: 20, total: 2,
    });
    renderPanel({
      ...session,
      status: 'finished',
      scoringStatus: 'completed',
      waitingEnterCount: 0,
      scoredCount: 1,
      scoringAbsentCount: 1,
      scoringFailedCount: 0,
    });
    expect((await screen.findAllByText('评分完成')).length).toBeGreaterThan(0);
    expect(await screen.findByText('张三')).toBeInTheDocument();
    expect(await screen.findByText('86.50 / 100.00')).toBeInTheDocument();
    expect(screen.getAllByText('缺考').length).toBeGreaterThan(0);
    expect(screen.queryByText('0.00 / 100.00')).not.toBeInTheDocument();
    expect(screen.getByText('已评分')).toBeInTheDocument();
    expect(screen.getByText('缺考结果')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '重新执行评分' })).not.toBeInTheDocument();
  });

  it('shows failed scoring and retries only with score permission', async () => {
    const user = userEvent.setup();
    auth.permissionCodes = ['competition:exam-session:score'];
    vi.mocked(scoreCompetitionFormalExamSession).mockResolvedValue({
      ...session,
      status: 'finished',
      scoringStatus: 'completed',
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [],
      page: 1, pageSize: 20, total: 0,
    });
    renderPanel({
      ...session,
      status: 'finished',
      scoringStatus: 'failed',
      scoringError: '正式题目标准答案格式异常',
      scoringFailedCount: 1,
    });
    expect((await screen.findAllByText('评分失败')).length).toBeGreaterThan(0);
    expect(screen.getByText('正式题目标准答案格式异常')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '重新执行评分' }));
    await waitFor(() => expect(scoreCompetitionFormalExamSession).toHaveBeenCalledWith('9'));
  });

  it('hides scoring retry without the dedicated permission', async () => {
    auth.permissionCodes = ['competition:exam-session:view'];
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [],
      page: 1, pageSize: 20, total: 0,
    });
    renderPanel({
      ...session,
      status: 'finished',
      scoringStatus: 'failed',
    });
    expect((await screen.findAllByText('评分失败')).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: '重新执行评分' })).not.toBeInTheDocument();
  });

  it('shows absent result detail as no-score instead of zero', async () => {
    const user = userEvent.setup();
    auth.permissionCodes = ['competition:exam-session:view'];
    vi.mocked(getCompetitionFormalCandidateExam).mockResolvedValue({
      id: '106', examSessionId: '9', candidateId: '105', contestantProfileId: '105',
      candidateNo: '006', candidateName: '周八', paperSnapshotId: '11', sourcePaperId: '500',
      paperCode: 'A', paperName: 'A卷', status: 'absent', answeredCount: 0, totalQuestionCount: 100,
      resultStatus: 'absent',
    });
    vi.mocked(getCompetitionFormalCandidateResult).mockResolvedValue({
      id: '2001', examSessionId: '9', candidateExamId: '106',
      resultStatus: 'absent', totalScore: null, fullScore: null, scoreVersion: 1, questionScores: [],
    });
    vi.mocked(pageCompetitionFormalCandidateExams).mockResolvedValue({
      items: [{
        id: '106', examSessionId: '9', candidateId: '105', contestantProfileId: '105',
        candidateNo: '006', candidateName: '周八', paperSnapshotId: '11', sourcePaperId: '500',
        paperCode: 'A', paperName: 'A卷', status: 'absent', answeredCount: 0, totalQuestionCount: 100,
        resultStatus: 'absent',
      }],
      page: 1, pageSize: 20, total: 1,
    });
    renderPanel({
      ...session,
      status: 'finished',
      scoringStatus: 'completed',
      waitingEnterCount: 0,
    });
    await user.click(await screen.findByRole('button', { name: '查看' }));
    expect(await screen.findByText('正式成绩')).toBeInTheDocument();
    expect(screen.getByText('结果')).toBeInTheDocument();
    expect(screen.getAllByText('缺考').length).toBeGreaterThan(0);
    expect(screen.queryByText('0.00')).not.toBeInTheDocument();
  });

  it('renders the formal console as an embedded page module instead of a modal', async () => {
    auth.permissionCodes = ['competition:exam-session:view'];
    vi.mocked(pageCompetitionStages).mockResolvedValue({
      items: [{
        id: '10', eventId: '20', name: '初赛', stageType: 'preliminary', sortOrder: 1,
        eventWorkTypeId: '30', eventWorkTypeLevelCode: '3',
      }],
      page: 1, pageSize: 500, total: 1,
    } as never);
    vi.mocked(getCompetitionFormalExamSessionByStage).mockResolvedValue(session);
    vi.mocked(getCompetitionFormalExamSession).mockResolvedValue(session);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionFormalExamConsolePage
          eventId="20"
          eventWorkTypeId="30"
          eventWorkTypeLevelCode="3"
        />
      </QueryClientProvider>,
    );
    expect(await screen.findByText('当前控制范围：初赛 · 等级3')).toBeInTheDocument();
    expect(await screen.findByText('理论考试 · 正式比赛')).toBeInTheDocument();
    expect(document.querySelector('.competition-formal-exam-session-panel--embedded')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(pageCompetitionStages).toHaveBeenCalledWith('20', expect.objectContaining({
      eventWorkTypeId: '30',
      eventWorkTypeLevelCode: '3',
    }));
    client.clear();
  });

  it('does not load unscoped stages until work type and level are selected', async () => {
    auth.permissionCodes = ['competition:exam-session:view'];
    vi.mocked(pageCompetitionStages).mockClear();
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionFormalExamConsolePage eventId="20" />
      </QueryClientProvider>,
    );
    expect(await screen.findByText(
      '请先选择工种和技能等级。正式比赛只读取该工种+等级下的赛段，只刷新赛事列表不会带出已配置的赛段。',
    )).toBeInTheDocument();
    expect(pageCompetitionStages).not.toHaveBeenCalled();
    client.clear();
  });

  it('explains empty scoped stages instead of treating the event as unconfigured', async () => {
    auth.permissionCodes = ['competition:exam-session:view'];
    vi.mocked(pageCompetitionStages).mockResolvedValue({
      items: [],
      page: 1, pageSize: 500, total: 0,
    } as never);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionFormalExamConsolePage
          eventId="20"
          eventWorkTypeId="30"
          eventWorkTypeLevelCode="3"
        />
      </QueryClientProvider>,
    );
    expect(await screen.findByText(
      '当前工种等级下暂无可控制的赛段。请确认已在「赛段编排」配置该工种等级的赛段，再点「刷新赛段」。',
    )).toBeInTheDocument();
    client.clear();
  });
});

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FormalExamPlayerApp } from '../src/pages/formalExam/FormalExamPlayerApp';
import {
  FormalExamApiError,
  enterFormalExam,
  getFormalExamPlayerContext,
  getFormalExamPlayerPaper,
  getFormalExamPlayerStatus,
  loginFormalExamPlayer,
  saveFormalExamAnswer,
  submitFormalExamPlayer,
  syncFormalExamAnswers,
  type FormalExamPlayerContext,
  type FormalExamPlayerPaper,
} from '../src/api/formalExamPlayer';
import { useFormalExamPlayerStore } from '../src/stores/formalExamPlayerStore';
import { formalExamLocalAnswerRepository } from '../src/pages/formalExam/idb/FormalExamLocalAnswerRepository';
import { FORMAL_EXAM_IDB_NAME } from '../src/pages/formalExam/idb/formalExamLocalAnswerTypes';
import type { LocalFormalExamAnswer } from '../src/pages/formalExam/idb/formalExamLocalAnswerTypes';

const realtimeTestState = vi.hoisted(() => ({
  options: undefined as Parameters<typeof import('../src/pages/formalExam/FormalExamRealtimeClient').startFormalExamRealtime>[0] | undefined,
  stop: vi.fn(),
  reconnect: vi.fn(),
}));

vi.mock('sanitize-html', () => ({
  default: (html: string) => html,
}));

vi.mock('../src/api/formalExamPlayer', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/api/formalExamPlayer')>();
  return {
    ...actual,
    loginFormalExamPlayer: vi.fn(),
    getFormalExamPlayerContext: vi.fn(),
    getFormalExamPlayerStatus: vi.fn(),
    enterFormalExam: vi.fn(),
    logoutFormalExamPlayer: vi.fn(),
    getFormalExamPlayerPaper: vi.fn(),
    saveFormalExamAnswer: vi.fn(),
    syncFormalExamAnswers: vi.fn(),
    submitFormalExamPlayer: vi.fn(),
    createFormalExamRealtimeTicket: vi.fn(),
    ackFormalExamRealtimeCommand: vi.fn(),
  };
});

vi.mock('../src/pages/formalExam/FormalExamRealtimeClient', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/pages/formalExam/FormalExamRealtimeClient')>();
  return {
    ...actual,
    startFormalExamRealtime: vi.fn((options: Parameters<typeof actual.startFormalExamRealtime>[0]) => {
      realtimeTestState.options = options;
      return {
        stop: realtimeTestState.stop,
        reconnect: realtimeTestState.reconnect,
      };
    }),
  };
});

const waitingContext = {
  examSession: {
    id: '900000000000001',
    status: 'ready',
    examType: 'theory',
    durationSeconds: 5400,
  },
  candidateExam: {
    id: '900000000000101',
    status: 'waiting_enter',
    candidateId: '8001',
    candidateNo: '001',
    candidateName: '张三',
  },
  competition: {
    eventName: '技能大赛',
    stageName: '初赛',
    tradeName: '铝电解工',
    skillLevelName: '高级工',
  },
  serverTime: '2026-09-08T09:00:00',
};

const readyContext = {
  ...waitingContext,
  candidateExam: {
    ...waitingContext.candidateExam,
    status: 'ready',
  },
};

const runningContext = {
  ...waitingContext,
  examSession: {
    ...waitingContext.examSession,
    status: 'running',
    actualStartTime: '2026-09-08 09:00:00',
    deadlineTime: '2026-09-08 10:30:00',
  },
  candidateExam: {
    ...waitingContext.candidateExam,
    status: 'running',
  },
  serverTime: '2026-09-08 09:00:08',
};

const submittedContext = {
  ...runningContext,
  candidateExam: {
    ...runningContext.candidateExam,
    status: 'submitted',
    submitTime: '2026-09-08 09:26:18',
  },
  serverTime: '2026-09-08 09:26:18',
};

const autoSubmittedContext = {
  ...runningContext,
  candidateExam: {
    ...runningContext.candidateExam,
    status: 'auto_submitted',
    submitTime: '2026-09-08 10:30:00',
  },
  serverTime: '2026-09-08 10:30:04',
};

const forcedSubmittedContext = {
  ...runningContext,
  candidateExam: {
    ...runningContext.candidateExam,
    status: 'forced_submitted',
    submitTime: '2026-09-08 09:40:12',
  },
  serverTime: '2026-09-08 09:40:12',
};

const absentContext = {
  ...waitingContext,
  examSession: {
    ...runningContext.examSession,
    status: 'finished',
    actualStartTime: '2026-09-08 09:00:00',
    deadlineTime: '2026-09-08 10:30:00',
  },
  candidateExam: {
    ...waitingContext.candidateExam,
    status: 'absent',
  },
  serverTime: '2026-09-08 10:30:08',
};

const finishedSubmittedContext = {
  ...submittedContext,
  examSession: {
    ...submittedContext.examSession,
    status: 'finished',
  },
};

function statusOf(context: FormalExamPlayerContext) {
  return {
    examSession: context.examSession,
    candidateExam: {
      id: context.candidateExam.id,
      status: context.candidateExam.status,
      submitTime: context.candidateExam.submitTime,
    },
    serverTime: context.serverTime,
    runtimeIssue: context.runtimeIssue,
  };
}

const blockedContext = {
  ...waitingContext,
  examSession: {
    ...waitingContext.examSession,
    status: 'running',
    actualStartTime: '2026-09-08 09:00:00',
    deadlineTime: '2026-09-08 10:30:00',
  },
};

const samplePaper: FormalExamPlayerPaper = {
  session: {
    id: '900000000000001',
    status: 'running',
    serverTime: '2026-09-08 09:00:08',
    actualStartTime: '2026-09-08 09:00:00',
    deadlineTime: '2026-09-08 10:30:00',
    durationSeconds: 5400,
  },
  candidate: {
    candidateNo: '001',
    candidateName: '张三',
  },
  paper: {
    questionCount: 3,
    totalScore: 100,
    answeredCount: 0,
  },
  questions: [
    {
      questionSnapshotId: '9301',
      order: 1,
      type: 'single',
      stem: '单选题干',
      options: [
        { optionId: '9301:A', label: 'A', content: '选项甲' },
        { optionId: '9301:B', label: 'B', content: '选项乙' },
      ],
    },
    {
      questionSnapshotId: '9302',
      order: 2,
      type: 'multi',
      stem: '多选题干',
      options: [
        { optionId: '9302:A', label: 'A', content: '多选甲' },
        { optionId: '9302:B', label: 'B', content: '多选乙' },
      ],
    },
    {
      questionSnapshotId: '9303',
      order: 3,
      type: 'fill',
      stem: '填空题干',
    },
  ],
  answers: [],
};

function acceptedSave(questionSnapshotId: string, version: number, answer: unknown) {
  return {
    questionSnapshotId,
    version,
    isAnswered: true,
    marked: false,
    savedAt: '2026-09-08T09:00:10',
    answeredCount: 1,
    serverTime: '2026-09-08T09:00:10',
    accepted: true,
    status: 'SYNCED',
    answer,
  };
}

function acceptedSync(body: { answers: Array<{ questionSnapshotId: string; version: number; answer?: unknown; marked?: boolean }> }) {
  return {
    results: body.answers.map((item) => acceptedSave(item.questionSnapshotId, item.version, item.answer)),
    serverTime: '2026-09-08T09:00:10',
  };
}

async function resetFormalExamIdb() {
  await formalExamLocalAnswerRepository.close();
  await new Promise<void>((resolve) => {
    const request = indexedDB.deleteDatabase(FORMAL_EXAM_IDB_NAME);
    request.onsuccess = () => resolve();
    request.onerror = () => resolve();
    request.onblocked = () => resolve();
  });
}

async function seedLocal(partial: Partial<LocalFormalExamAnswer>) {
  await formalExamLocalAnswerRepository.upsert({
    sessionId: '900000000000001',
    candidateExamId: '900000000000101',
    questionSnapshotId: '9301',
    answer: { selectedOptionIds: ['9301:B'] },
    marked: false,
    version: 4,
    serverVersion: 1,
    syncStatus: 'PENDING',
    updatedAt: '2026-09-08T09:01:00.000Z',
    ...partial,
  });
}

function renderApp() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, networkMode: 'always' } },
  });
  return render(
    <QueryClientProvider client={client}>
      <FormalExamPlayerApp />
    </QueryClientProvider>,
  );
}

function loginAsPlayer() {
  useFormalExamPlayerStore.getState().setSession({
    token: 'formal-token',
    tenantId: '1',
    playerId: '77',
    accessSessionId: '55',
    generation: 1,
    deviceId: 'pad-test',
    examSessionId: '900000000000001',
  });
}

describe('正式比赛选手端', () => {
  beforeEach(async () => {
    window.history.replaceState({}, '', '/formal-exam?sessionId=900000000000001');
    window.localStorage.clear();
    await resetFormalExamIdb();
    Object.defineProperty(window.navigator, 'onLine', { configurable: true, get: () => true });
    useFormalExamPlayerStore.setState({
      token: undefined,
      tenantId: undefined,
      playerId: undefined,
      accessSessionId: undefined,
      generation: undefined,
      examSessionId: undefined,
    });
    vi.mocked(loginFormalExamPlayer).mockReset();
    vi.mocked(getFormalExamPlayerContext).mockReset();
    vi.mocked(getFormalExamPlayerStatus).mockReset();
    vi.mocked(enterFormalExam).mockReset();
    vi.mocked(getFormalExamPlayerPaper).mockReset();
    vi.mocked(saveFormalExamAnswer).mockReset();
    vi.mocked(syncFormalExamAnswers).mockReset();
    vi.mocked(submitFormalExamPlayer).mockReset();
    vi.mocked(getFormalExamPlayerPaper).mockResolvedValue(samplePaper);
    vi.mocked(syncFormalExamAnswers).mockImplementation(async (_sessionId, body) => acceptedSync(body));
    vi.mocked(saveFormalExamAnswer).mockImplementation(async (_sessionId, questionSnapshotId, body) =>
      acceptedSave(questionSnapshotId, body.version, body.answer));
    vi.mocked(submitFormalExamPlayer).mockResolvedValue({
      candidateExamStatus: 'submitted',
      submitTime: '2026-09-08 09:26:18',
      answeredCount: 3,
      totalQuestionCount: 3,
      unansweredCount: 0,
      serverTime: '2026-09-08 09:26:18',
    });
    realtimeTestState.options = undefined;
    realtimeTestState.stop.mockReset();
    realtimeTestState.reconnect.mockReset();
    vi.mocked(getFormalExamPlayerStatus).mockImplementation(async () => {
      const results = vi.mocked(getFormalExamPlayerContext).mock.results
        .filter((item) => item.type === 'return');
      const last = results.at(-1)?.value;
      if (!last) {
        throw new FormalExamApiError('status unavailable', undefined, 500);
      }
      return statusOf(await last as FormalExamPlayerContext);
    });
  });

  it('logs in and shows identity confirmation without paper secrets', async () => {
    const user = userEvent.setup();
    vi.mocked(loginFormalExamPlayer).mockResolvedValue({
      playerId: '77',
      tenantId: '1',
      accessToken: 'formal-token',
      tokenType: 'Bearer',
      expiresIn: 259200,
      accessSessionId: '55',
      generation: 1,
      deviceId: 'pad-test',
      examSessionId: '900000000000001',
      candidateExamId: '900000000000101',
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(waitingContext);
    renderApp();
    await user.type(screen.getByPlaceholderText('请输入手机号'), '13800000000');
    await user.type(screen.getByPlaceholderText('请输入密码'), 'secret12');
    await user.click(screen.getByRole('button', { name: '登录' }));
    expect(await screen.findByText('姓名：张三')).toBeInTheDocument();
    expect(loginFormalExamPlayer).toHaveBeenCalledWith(
      '13800000000',
      'secret12',
      '900000000000001',
      expect.any(String),
    );
    expect(useFormalExamPlayerStore.getState().accessSessionId).toBe('55');
    expect(useFormalExamPlayerStore.getState().generation).toBe(1);
    expect(window.localStorage.getItem('formal-exam-device-id')).toBeTruthy();
    expect(screen.getByText('选手编号：001')).toBeInTheDocument();
    expect(screen.getByText('工种：铝电解工')).toBeInTheDocument();
    expect(screen.getByText('考试时长：90 分钟')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '确认进入比赛' })).toBeInTheDocument();
    expect(screen.queryByText(/A卷|paperSnapshot|正确答案/)).not.toBeInTheDocument();
    expect(getFormalExamPlayerContext).toHaveBeenCalledWith('900000000000001');
  });

  it('shows a hotspot-friendly message when login cannot reach the server', async () => {
    const user = userEvent.setup();
    vi.mocked(loginFormalExamPlayer).mockRejectedValue(new FormalExamApiError(
      '无法连接考试服务器。请用本机热点网关打开本页（Windows 热点一般为 http://192.168.137.1:8080/formal-exam），不要使用 localhost 或电脑网线/Wi-Fi 的 IP。',
    ));
    renderApp();
    await user.type(screen.getByPlaceholderText('请输入手机号'), '13800138010');
    await user.type(screen.getByPlaceholderText('请输入密码'), '100101');
    await user.click(screen.getByRole('button', { name: '登录' }));
    expect(await screen.findByText(/无法连接考试服务器/)).toBeInTheDocument();
  });

  it('enters the waiting room after confirming identity', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(waitingContext);
    vi.mocked(enterFormalExam).mockResolvedValue(readyContext);
    renderApp();
    await screen.findByRole('button', { name: '确认进入比赛' });
    await user.click(screen.getByRole('button', { name: '确认进入比赛' }));
    expect(await screen.findByText('当前状态：等待开考')).toBeInTheDocument();
    expect(screen.getByText('✓ 已进入比赛')).toBeInTheDocument();
    expect(enterFormalExam).toHaveBeenCalledWith('900000000000001');
  });

  it('restores the waiting room from server READY status after reload', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(readyContext);
    renderApp();
    expect(await screen.findByText('当前状态：等待开考')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '确认进入比赛' })).not.toBeInTheDocument();
    await waitFor(() => expect(getFormalExamPlayerContext).toHaveBeenCalledWith('900000000000001'));
  });

  it('switches from waiting to the answering page when context becomes running', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(readyContext);
    vi.mocked(getFormalExamPlayerStatus)
      .mockResolvedValueOnce(statusOf(readyContext))
      .mockResolvedValue(statusOf(runningContext));
    renderApp();
    expect(await screen.findByText('当前状态：等待开考')).toBeInTheDocument();
    await waitFor(() => expect(getFormalExamPlayerStatus).toHaveBeenCalled());
    window.dispatchEvent(new Event('focus'));
    expect(await screen.findByText('单选题干')).toBeInTheDocument();
    expect(screen.getByText(/剩余/)).toBeInTheDocument();
    expect(screen.getByText('张三 001')).toBeInTheDocument();
    expect(screen.queryByText('正在准备考试内容……')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '交卷' })).toBeInTheDocument();
    expect(getFormalExamPlayerPaper).toHaveBeenCalledWith('900000000000001');
  });

  it('restores the answering page from server RUNNING status after reload', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    expect(await screen.findByText('单选题干')).toBeInTheDocument();
    expect(screen.queryByText('当前状态：等待开考')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '确认进入比赛' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '交卷' })).toBeInTheDocument();
  });

  it('does not show enter when session already running and candidate still waiting', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(blockedContext);
    renderApp();
    expect(await screen.findByText('本场比赛已经开始。')).toBeInTheDocument();
    expect(screen.getByText('您尚未完成赛前候考进入，请联系现场工作人员处理。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '确认进入比赛' })).not.toBeInTheDocument();
  });

  it('returns to the login page after staff evicts the exam session', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockRejectedValue(
      new FormalExamApiError('考试会话无效或已下线', '401', 401),
    );
    renderApp();
    expect(await screen.findByText('请使用选手账号登录')).toBeInTheDocument();
    expect(await screen.findByText('考试账号已被工作人员清退，请重新登录')).toBeInTheDocument();
    expect(useFormalExamPlayerStore.getState().token).toBeUndefined();
    expect(screen.getByRole('button', { name: '登录' })).toBeInTheDocument();
    await waitFor(() => expect(window.location.search).not.toContain('sessionId'));
  });

  it('returns to the login page when a later status poll reports the account was kicked', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(readyContext);
    vi.mocked(getFormalExamPlayerStatus).mockRejectedValue(
      new FormalExamApiError('考试会话无效或已下线', '401', 401),
    );
    renderApp();
    expect(await screen.findByText('考试账号已被工作人员清退，请重新登录')).toBeInTheDocument();
    expect(useFormalExamPlayerStore.getState().token).toBeUndefined();
    expect(screen.getByRole('button', { name: '登录' })).toBeInTheDocument();
  });

  it('lets a pad without sessionId log in and bind the contestant to the current exam', async () => {
    const user = userEvent.setup();
    window.history.replaceState({}, '', '/formal-exam');
    vi.mocked(loginFormalExamPlayer).mockResolvedValue({
      playerId: '77',
      tenantId: '1',
      accessToken: 'formal-token',
      tokenType: 'Bearer',
      expiresIn: 259200,
      accessSessionId: '55',
      generation: 1,
      deviceId: 'pad-test',
      examSessionId: '900000000000001',
      candidateExamId: '900000000000101',
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(waitingContext);
    renderApp();
    expect(screen.getByText('请使用选手账号登录')).toBeInTheDocument();
    expect(screen.queryByText('缺少比赛场次，请联系工作人员')).not.toBeInTheDocument();
    await user.type(screen.getByPlaceholderText('请输入手机号'), '13800000000');
    await user.type(screen.getByPlaceholderText('请输入密码'), 'secret12');
    await user.click(screen.getByRole('button', { name: '登录' }));
    expect(loginFormalExamPlayer).toHaveBeenCalledWith(
      '13800000000',
      'secret12',
      undefined,
      expect.any(String),
    );
    expect(await screen.findByText('姓名：张三')).toBeInTheDocument();
    expect(getFormalExamPlayerContext).toHaveBeenCalledWith('900000000000001');
    expect(useFormalExamPlayerStore.getState().examSessionId).toBe('900000000000001');
    expect(window.location.search).toContain('sessionId=900000000000001');
  });

  it('binds the next contestant to their own exam after the pad stays on the previous session url', async () => {
    const user = userEvent.setup();
    window.history.replaceState({}, '', '/formal-exam?sessionId=800000000000001');
    vi.mocked(loginFormalExamPlayer).mockResolvedValue({
      playerId: '77',
      tenantId: '1',
      accessToken: 'formal-token',
      tokenType: 'Bearer',
      expiresIn: 259200,
      accessSessionId: '55',
      generation: 1,
      deviceId: 'pad-test',
      examSessionId: '900000000000001',
      candidateExamId: '900000000000101',
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(waitingContext);
    renderApp();
    await user.type(screen.getByPlaceholderText('请输入手机号'), '13800000000');
    await user.type(screen.getByPlaceholderText('请输入密码'), 'secret12');
    await user.click(screen.getByRole('button', { name: '登录' }));
    expect(loginFormalExamPlayer).toHaveBeenCalledWith(
      '13800000000',
      'secret12',
      '800000000000001',
      expect.any(String),
    );
    expect(await screen.findByText('姓名：张三')).toBeInTheDocument();
    expect(getFormalExamPlayerContext).toHaveBeenCalledWith('900000000000001');
    expect(useFormalExamPlayerStore.getState().examSessionId).toBe('900000000000001');
    expect(window.location.search).toContain('sessionId=900000000000001');
  });

  it('writes IndexedDB before requesting the server and shows SAVED after sync', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    let resolveSync: ((value: ReturnType<typeof acceptedSync>) => void) | undefined;
    vi.mocked(syncFormalExamAnswers).mockImplementation(
      () => new Promise((resolve) => {
        resolveSync = resolve;
      }),
    );
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: /选项甲/ }));
    await waitFor(async () => {
      const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
      expect(row?.syncStatus === 'PENDING' || row?.syncStatus === 'SYNCING').toBe(true);
      expect(row?.version).toBe(1);
      expect(row?.answer.selectedOptionIds).toEqual(['9301:A']);
    });
    await waitFor(() => expect(syncFormalExamAnswers).toHaveBeenCalled());
    const request = vi.mocked(syncFormalExamAnswers).mock.calls[0][1];
    expect(request.answers[0]).toMatchObject({
      questionSnapshotId: '9301',
      version: 1,
    });
    expect(request.answers[0].answer?.selectedOptionIds).toEqual(['9301:A']);
    resolveSync?.(acceptedSync(request));
    expect(await screen.findByText('✓ 已保存')).toBeInTheDocument();
    const synced = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(synced?.syncStatus).toBe('SYNCED');
  });

  it('keeps the local answer as pending when the sync API fails', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(syncFormalExamAnswers).mockRejectedValue(new FormalExamApiError('网络错误', '500', 500));
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: /选项甲/ }));
    expect(await screen.findByText('同步失败，等待重试')).toBeInTheDocument();
    expect(screen.queryByText('✓ 已保存')).not.toBeInTheDocument();
    const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(row?.answer.selectedOptionIds).toEqual(['9301:A']);
    expect(row?.syncStatus).toBe('ERROR');
  });

  it('restores only successfully saved answers after reload', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(getFormalExamPlayerPaper).mockResolvedValue({
      ...samplePaper,
      paper: { ...samplePaper.paper, answeredCount: 1 },
      answers: [{
        questionSnapshotId: '9301',
        answer: { selectedOptionIds: ['9301:B'] },
        marked: true,
        version: 2,
        isAnswered: true,
        savedAt: '2026-09-08T09:01:00',
      }],
    });
    renderApp();
    expect(await screen.findByText('单选题干')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /选项乙/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /选项甲/ })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: '取消标记' })).toBeInTheDocument();
    expect(syncFormalExamAnswers).not.toHaveBeenCalled();
  });

  it('keeps unsynced IndexedDB answers after reload instead of using older server answers', async () => {
    loginAsPlayer();
    await seedLocal({
      version: 4,
      serverVersion: 1,
      syncStatus: 'PENDING',
      answer: { selectedOptionIds: ['9301:B'] },
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(getFormalExamPlayerPaper).mockResolvedValue({
      ...samplePaper,
      answers: [{
        questionSnapshotId: '9301',
        answer: { selectedOptionIds: ['9301:A'] },
        marked: false,
        version: 1,
        isAnswered: true,
        savedAt: '2026-09-08T09:00:10',
      }],
    });
    renderApp();
    expect(await screen.findByText('单选题干')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /选项乙/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /选项甲/ })).toHaveAttribute('aria-pressed', 'false');
    await waitFor(() => expect(syncFormalExamAnswers).toHaveBeenCalled());
    const request = vi.mocked(syncFormalExamAnswers).mock.calls[0][1];
    expect(request.answers[0].version).toBe(4);
  });

  it('adopts a newer server answer over an older local copy', async () => {
    loginAsPlayer();
    await seedLocal({
      version: 1,
      serverVersion: 1,
      syncStatus: 'SYNCED',
      answer: { selectedOptionIds: ['9301:A'] },
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(getFormalExamPlayerPaper).mockResolvedValue({
      ...samplePaper,
      answers: [{
        questionSnapshotId: '9301',
        answer: { selectedOptionIds: ['9301:B'] },
        marked: false,
        version: 3,
        isAnswered: true,
        savedAt: '2026-09-08T09:02:00',
      }],
    });
    renderApp();
    expect(await screen.findByText('单选题干')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /选项乙/ })).toHaveAttribute('aria-pressed', 'true');
    const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(row?.version).toBe(3);
    expect(row?.syncStatus).toBe('SYNCED');
  });

  it('does not merge another candidateExam local answers on the same pad', async () => {
    loginAsPlayer();
    await seedLocal({
      candidateExamId: '900000000000202',
      version: 9,
      syncStatus: 'PENDING',
      answer: { selectedOptionIds: ['9301:B'] },
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    expect(await screen.findByText('单选题干')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /选项甲/ })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: /选项乙/ })).toHaveAttribute('aria-pressed', 'false');
    const other = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000202', '9301');
    expect(other?.version).toBe(9);
  });

  it('keeps answering offline and does not show server SAVED', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    await screen.findByText('单选题干');
    Object.defineProperty(window.navigator, 'onLine', { configurable: true, get: () => false });
    window.dispatchEvent(new Event('offline'));
    await user.click(screen.getByRole('button', { name: /选项甲/ }));
    expect(await screen.findByText('已保存到本机，待同步')).toBeInTheDocument();
    expect(screen.getByText(/网络异常 · 答案已保存在本机/)).toBeInTheDocument();
    expect(screen.queryByText('✓ 已保存')).not.toBeInTheDocument();
    expect(syncFormalExamAnswers).not.toHaveBeenCalled();
    const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(row?.syncStatus).toBe('PENDING');
  });

  it('auto syncs pending answers when the browser fires online', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    await screen.findByText('单选题干');
    Object.defineProperty(window.navigator, 'onLine', { configurable: true, get: () => false });
    window.dispatchEvent(new Event('offline'));
    await user.click(screen.getByRole('button', { name: /选项甲/ }));
    await screen.findByText('已保存到本机，待同步');
    Object.defineProperty(window.navigator, 'onLine', { configurable: true, get: () => true });
    window.dispatchEvent(new Event('online'));
    expect(await screen.findByText('✓ 已保存')).toBeInTheDocument();
    expect(syncFormalExamAnswers).toHaveBeenCalled();
  });

  it('ignores a stale sync response after a newer local version exists', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    const resolvers: Array<(value: ReturnType<typeof acceptedSync>) => void> = [];
    vi.mocked(syncFormalExamAnswers).mockImplementation(
      () => new Promise((resolve) => {
        resolvers.push(resolve);
      }),
    );
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: /选项甲/ }));
    await waitFor(() => expect(resolvers.length).toBeGreaterThanOrEqual(1));
    await user.click(screen.getByRole('button', { name: /选项乙/ }));
    await waitFor(async () => {
      const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
      expect(row?.version).toBe(2);
    });
    resolvers[0](acceptedSync({
      answers: [{ questionSnapshotId: '9301', version: 1, answer: { selectedOptionIds: ['9301:A'] } }],
    }));
    await waitFor(() => expect(resolvers.length).toBeGreaterThanOrEqual(2));
    resolvers[1](acceptedSync({
      answers: [{ questionSnapshotId: '9301', version: 2, answer: { selectedOptionIds: ['9301:B'] } }],
    }));
    expect(await screen.findByText('✓ 已保存')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /选项乙/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /选项甲/ })).toHaveAttribute('aria-pressed', 'false');
    const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(row?.version).toBe(2);
    expect(row?.syncStatus).toBe('SYNCED');
    expect(row?.answer.selectedOptionIds).toEqual(['9301:B']);
  });

  it('jumps to a question from the answer card', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: '答题卡' }));
    await user.click(await screen.findByRole('button', { name: '3' }));
    expect(await screen.findByText('填空题干')).toBeInTheDocument();
  });

  it('locks answering after the deadline without showing submit', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue({
      ...runningContext,
      serverTime: '2026-09-08 10:30:00',
      examSession: {
        ...runningContext.examSession,
        deadlineTime: '2026-09-08 10:30:00',
      },
    });
    renderApp();
    expect((await screen.findAllByText('考试时间已到，请等待系统处理')).length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /选项甲/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: '交卷' })).toBeInTheDocument();
  });

  it('flushes fill answers to IndexedDB when jumping to another question', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: '答题卡' }));
    await user.click(await screen.findByRole('button', { name: '3' }));
    expect(await screen.findByText('填空题干')).toBeInTheDocument();
    await user.type(screen.getByPlaceholderText('请输入答案'), '氧化铝');
    await user.click(screen.getByRole('button', { name: '上一题' }));
    await waitFor(async () => {
      const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9303');
      expect(row?.answer.blanks).toEqual(['氧化铝']);
      expect(row?.version).toBeGreaterThanOrEqual(1);
    });
  });

  it('shows a reconnect hint when paper cannot be loaded', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(getFormalExamPlayerPaper).mockRejectedValue(new FormalExamApiError('Network Error'));
    renderApp();
    expect(await screen.findByText('暂时无法连接考试服务器，请保持应用开启并等待网络恢复')).toBeInTheDocument();
  });

  it('flushes pending answers then submits after confirmation', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    await seedLocal({
      version: 2,
      serverVersion: 1,
      syncStatus: 'PENDING',
      answer: { selectedOptionIds: ['9301:A'] },
    });
    await seedLocal({
      questionSnapshotId: '9302',
      version: 1,
      serverVersion: 1,
      syncStatus: 'SYNCED',
      answer: { selectedOptionIds: ['9302:A', '9302:B'] },
    });
    await seedLocal({
      questionSnapshotId: '9303',
      version: 1,
      serverVersion: 1,
      syncStatus: 'SYNCED',
      answer: { blanks: ['氧化铝'] },
    });
    const resolvers: Array<() => void> = [];
    vi.mocked(syncFormalExamAnswers).mockImplementation(
      (_sessionId, body) => new Promise((resolve) => {
        resolvers.push(() => resolve(acceptedSync(body)));
      }),
    );
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(submitFormalExamPlayer).mockResolvedValue({
      candidateExamStatus: 'submitted',
      submitTime: '2026-09-08 09:26:18',
      answeredCount: 3,
      totalQuestionCount: 3,
      unansweredCount: 0,
      serverTime: '2026-09-08 09:26:18',
    });
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: '交卷' }));
    expect(await screen.findByText('正在同步最后的答案…')).toBeInTheDocument();
    expect(submitFormalExamPlayer).not.toHaveBeenCalled();
    await waitFor(() => {
      resolvers.splice(0).forEach((resolve) => resolve());
      expect(screen.getByText('确认提交试卷？')).toBeInTheDocument();
    });
    expect(screen.getByText('总题数：3')).toBeInTheDocument();
    expect(screen.getByText('已答：3')).toBeInTheDocument();
    expect(screen.getByText('未答：0')).toBeInTheDocument();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(submittedContext);
    await user.click(screen.getByRole('button', { name: '确认交卷' }));
    expect(await screen.findByText('试卷提交成功')).toBeInTheDocument();
    expect(screen.getByText('提交时间：09:26:18')).toBeInTheDocument();
    expect(screen.queryByText('单选题干')).not.toBeInTheDocument();
    expect(submitFormalExamPlayer).toHaveBeenCalledTimes(1);
    const body = vi.mocked(submitFormalExamPlayer).mock.calls[0][1];
    expect(body.confirmUnanswered).toBe(false);
    expect(body.answerVersions).toEqual([
      { questionSnapshotId: '9301', version: 2 },
      { questionSnapshotId: '9302', version: 1 },
      { questionSnapshotId: '9303', version: 1 },
    ]);
    const local = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(local?.syncStatus).toBe('SYNCED');
  });

  it('does not submit when pending answers cannot reach the server', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    await seedLocal({
      version: 3,
      serverVersion: 1,
      syncStatus: 'PENDING',
      answer: { selectedOptionIds: ['9301:B'] },
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(syncFormalExamAnswers).mockRejectedValue(new FormalExamApiError('网络错误', '500', 500));
    renderApp();
    await screen.findByText('单选题干');
    Object.defineProperty(window.navigator, 'onLine', { configurable: true, get: () => false });
    window.dispatchEvent(new Event('offline'));
    await user.click(screen.getByRole('button', { name: '交卷' }));
    expect(await screen.findByText(/答案已经保存在本机/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '确认交卷' })).not.toBeInTheDocument();
    expect(submitFormalExamPlayer).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: '返回答题' }));
    expect(screen.getByRole('button', { name: /选项甲/ })).toBeEnabled();
    const row = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(row?.syncStatus === 'PENDING' || row?.syncStatus === 'ERROR').toBe(true);
  });

  it('blocks submit when a local conflict exists', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    await seedLocal({
      version: 4,
      serverVersion: 4,
      syncStatus: 'CONFLICT',
      lastError: 'VERSION_PAYLOAD_CONFLICT',
      answer: { selectedOptionIds: ['9301:B'] },
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: '交卷' }));
    expect(await screen.findByText('当前有答案同步冲突，暂时无法交卷。请联系现场工作人员处理。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '确认交卷' })).not.toBeInTheDocument();
    expect(submitFormalExamPlayer).not.toHaveBeenCalled();
  });

  it('asks to confirm unanswered questions before submit', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    await seedLocal({
      version: 1,
      serverVersion: 1,
      syncStatus: 'SYNCED',
      answer: { selectedOptionIds: ['9301:A'] },
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: '交卷' }));
    expect(await screen.findByText('当前还有 2 道题未作答。')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '继续答题' }));
    expect(screen.queryByRole('button', { name: '确认交卷' })).not.toBeInTheDocument();
    expect(submitFormalExamPlayer).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: '交卷' }));
    expect(await screen.findByText('当前还有 2 道题未作答。')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '确认交卷' }));
    await waitFor(() => expect(submitFormalExamPlayer).toHaveBeenCalled());
    expect(vi.mocked(submitFormalExamPlayer).mock.calls[0][1].confirmUnanswered).toBe(true);
  });

  it('recovers to the complete page when submit succeeds but the response is lost', async () => {
    const user = userEvent.setup();
    loginAsPlayer();
    await seedLocal({
      version: 1,
      serverVersion: 1,
      syncStatus: 'SYNCED',
      answer: { selectedOptionIds: ['9301:A'] },
    });
    await seedLocal({
      questionSnapshotId: '9302',
      version: 1,
      serverVersion: 1,
      syncStatus: 'SYNCED',
      answer: { selectedOptionIds: ['9302:A'] },
    });
    await seedLocal({
      questionSnapshotId: '9303',
      version: 1,
      serverVersion: 1,
      syncStatus: 'SYNCED',
      answer: { blanks: ['氧化铝'] },
    });
    vi.mocked(getFormalExamPlayerContext)
      .mockResolvedValueOnce(runningContext)
      .mockResolvedValue(submittedContext);
    vi.mocked(submitFormalExamPlayer).mockRejectedValue(new FormalExamApiError('Network Error', '500', 500));
    renderApp();
    await screen.findByText('单选题干');
    await user.click(screen.getByRole('button', { name: '交卷' }));
    expect(await screen.findByText('确认提交试卷？')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '确认交卷' }));
    expect(await screen.findByText('试卷提交成功')).toBeInTheDocument();
    expect(screen.queryByText('单选题干')).not.toBeInTheDocument();
  });

  it('restores the complete page from SUBMITTED context after reload', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(submittedContext);
    renderApp();
    expect(await screen.findByText('试卷提交成功')).toBeInTheDocument();
    expect(screen.getByText('姓名：张三')).toBeInTheDocument();
    expect(screen.getByText('选手编号：001')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '交卷' })).not.toBeInTheDocument();
    expect(getFormalExamPlayerPaper).not.toHaveBeenCalled();
  });

  it('does not poll context or status after a submitted terminal screen', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(submittedContext);
    renderApp();
    expect(await screen.findByText('试卷提交成功')).toBeInTheDocument();
    const contextCalls = vi.mocked(getFormalExamPlayerContext).mock.calls.length;
    const statusCalls = vi.mocked(getFormalExamPlayerStatus).mock.calls.length;
    await new Promise((resolve) => setTimeout(resolve, 80));
    expect(vi.mocked(getFormalExamPlayerContext).mock.calls.length).toBe(contextCalls);
    expect(vi.mocked(getFormalExamPlayerStatus).mock.calls.length).toBe(statusCalls);
    expect(contextCalls).toBe(1);
    expect(statusCalls).toBe(0);
  });

  it('keeps the realtime control channel active after a candidate submits', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(submittedContext);
    renderApp();
    expect(await screen.findByText('试卷提交成功')).toBeInTheDocument();
    expect(realtimeTestState.options?.isActive()).toBe(true);
    expect(realtimeTestState.stop).not.toHaveBeenCalled();
  });

  it('shows the auto-submit complete page and does not start answer sync', async () => {
    loginAsPlayer();
    await seedLocal({
      version: 5,
      serverVersion: 4,
      syncStatus: 'PENDING',
      answer: { selectedOptionIds: ['9301:B'] },
    });
    await seedLocal({
      questionSnapshotId: '9302',
      version: 2,
      serverVersion: 1,
      syncStatus: 'ERROR',
      lastError: 'NETWORK',
      answer: { selectedOptionIds: ['9302:A'] },
    });
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(autoSubmittedContext);
    renderApp();
    expect(await screen.findByText('考试时间已到')).toBeInTheDocument();
    expect(screen.getByText('系统已自动提交试卷')).toBeInTheDocument();
    expect(screen.getByText('提交时间：10:30:00')).toBeInTheDocument();
    expect(await screen.findByText(/本设备仍保留 2 道未同步的本地作答记录/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '交卷' })).not.toBeInTheDocument();
    expect(getFormalExamPlayerPaper).not.toHaveBeenCalled();
    expect(syncFormalExamAnswers).not.toHaveBeenCalled();
    const pending = await formalExamLocalAnswerRepository.get('900000000000001', '900000000000101', '9301');
    expect(pending?.syncStatus).toBe('PENDING');
    expect(pending?.version).toBe(5);
  });

  it('shows the forced-submit complete page without staff reason', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(forcedSubmittedContext);
    renderApp();
    expect(await screen.findByText('本场考试已由工作人员结束提交')).toBeInTheDocument();
    expect(screen.getByText('请联系现场工作人员')).toBeInTheDocument();
    expect(screen.getByText('提交时间：09:40:12')).toBeInTheDocument();
    expect(screen.queryByText(/forceSubmitReason|操作人员|现场设备/)).not.toBeInTheDocument();
    expect(getFormalExamPlayerPaper).not.toHaveBeenCalled();
    expect(syncFormalExamAnswers).not.toHaveBeenCalled();
  });

  it('leaves answering when status becomes AUTO_SUBMITTED and stops sync', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(getFormalExamPlayerStatus)
      .mockResolvedValueOnce(statusOf(runningContext))
      .mockResolvedValue(statusOf(autoSubmittedContext));
    renderApp();
    await screen.findByText('单选题干');
    window.dispatchEvent(new Event('focus'));
    expect(await screen.findByText('考试时间已到')).toBeInTheDocument();
    expect(screen.queryByText('单选题干')).not.toBeInTheDocument();
    const syncCalls = vi.mocked(syncFormalExamAnswers).mock.calls.length;
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(vi.mocked(syncFormalExamAnswers).mock.calls.length).toBe(syncCalls);
  });

  it('leaves answering when status becomes FORCED_SUBMITTED', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(runningContext);
    vi.mocked(getFormalExamPlayerStatus)
      .mockResolvedValueOnce(statusOf(runningContext))
      .mockResolvedValue(statusOf(forcedSubmittedContext));
    renderApp();
    await screen.findByText('单选题干');
    window.dispatchEvent(new Event('focus'));
    expect(await screen.findByText('本场考试已由工作人员结束提交')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '交卷' })).not.toBeInTheDocument();
  });

  it('shows the absent page after deadline without paper or enter', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(absentContext);
    renderApp();
    expect(await screen.findByText('本场考试已结束')).toBeInTheDocument();
    expect(screen.getByText('您未在规定时间内进入考试，')).toBeInTheDocument();
    expect(screen.getByText('系统已记录为缺考。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '确认进入比赛' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '交卷' })).not.toBeInTheDocument();
    expect(screen.queryByText('试卷提交成功')).not.toBeInTheDocument();
    expect(getFormalExamPlayerPaper).not.toHaveBeenCalled();
    expect(syncFormalExamAnswers).not.toHaveBeenCalled();
  });

  it('keeps the personal submitted page after the session is finished', async () => {
    loginAsPlayer();
    vi.mocked(getFormalExamPlayerContext).mockResolvedValue(finishedSubmittedContext);
    renderApp();
    expect(await screen.findByText('试卷提交成功')).toBeInTheDocument();
    expect(screen.queryByText('系统已记录为缺考。')).not.toBeInTheDocument();
    expect(getFormalExamPlayerPaper).not.toHaveBeenCalled();
  });
});

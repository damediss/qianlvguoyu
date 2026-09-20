import {
  CheckCircleOutlined,
  CloudOutlined,
  CloudSyncOutlined,
  LeftOutlined,
  LogoutOutlined,
  MinusOutlined,
  PlusOutlined,
  ReloadOutlined,
  WifiOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Empty, Input, List, Modal, Space, Spin, Tag, Typography, message } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { logout } from '../../api/auth';
import {
  confirmCompetitionPracticalJudgeStart,
  createCompetitionPracticalRealtimeTicket,
  listCompetitionPracticalJudgeTasks,
  openCompetitionPracticalJudgeTask,
  saveCompetitionPracticalJudgeTask,
  startCompetitionPracticalJudgeSession,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionPracticalJudgeTask,
  CompetitionPracticalScoreEntryRequest,
} from '../../types/competition';
import '../assessor/TabletAssessorApp.css';

type ScoreItem = {
  itemNo: number;
  itemName?: string;
  criterion?: string;
  elementHtml?: string;
  standardHtml?: string;
  maxScore: number;
  maxDeduction?: number | null;
  deductionStep?: number | null;
  scoringMethod?: string | null;
};

type PracticalQuestion = {
  paperQuestionId: string | number;
  sortOrder: number;
  title: string;
  score: number;
  scoringMethod?: string | null;
  scoreItems?: unknown;
  detail?: unknown;
};

type ScoreValue = {
  score: number;
  deduction: number;
  remark: string;
};

type ScoreRow = {
  question: PracticalQuestion;
  item: ScoreItem;
  key: string;
};

type SaveVariables = {
  entries: CompetitionPracticalScoreEntryRequest[];
  fingerprint: string;
  submit: boolean;
};

type SyncState = 'saved' | 'syncing' | 'offline' | 'error';

function plain(value?: string | null) {
  return (value ?? '')
    .replace(/<br\s*\/?\s*>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return value != null && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, unknown>
    : undefined;
}

function detailText(detail: unknown, ...keys: string[]) {
  const record = asRecord(detail);
  for (const key of keys) {
    const value = record?.[key];
    if (typeof value === 'string' && plain(value)) return plain(value);
  }
  return '';
}

function formatScore(value?: number | null) {
  const score = Number(value ?? 0);
  return Number.isInteger(score) ? String(score) : score.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}

function taskStatusText(task: CompetitionPracticalJudgeTask) {
  if (task.scoreSheetStatus === 'submitted_locked') return '已提交';
  if (task.startStatus === 'waiting') return task.currentJudgeConfirmed ? '等待其他裁判' : '待确认';
  if (task.startStatus === 'ready') return task.judgeRole === 'leader' ? '待组长开考' : '等待组长开考';
  if (task.startStatus === 'completed') return '已完成';
  if (task.scoreSheetStatus === 'unlocked_for_rescore') return '已退回';
  if (task.scoreSheetStatus === 'draft') return '评分中';
  return '待评分';
}

function questionProgressOf(task: CompetitionPracticalJudgeTask | undefined, paperQuestionId?: string | number) {
  if (!task || paperQuestionId == null) return undefined;
  return task.questionProgress?.find((item) => String(item.paperQuestionId) === String(paperQuestionId));
}

function questionStatusOf(task: CompetitionPracticalJudgeTask | undefined, paperQuestionId?: string | number) {
  if (!task) return 'scoring';
  if (task.scoreSheetStatus === 'submitted_locked') return 'submitted';
  const progress = questionProgressOf(task, paperQuestionId);
  if (progress?.status) return progress.status;
  if (task.scoreSheetStatus === 'unlocked_for_rescore') return 'returned';
  return 'scoring';
}

function questionStatusText(status: string) {
  return { submitted: '已提交', scoring: '评分中', returned: '已退回', unopened: '待评分' }[status] || status;
}

function syncText(state: SyncState) {
  return {
    saved: '已同步',
    syncing: '同步中',
    offline: '已保存到本机',
    error: '同步失败',
  }[state];
}

function errorMessage(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
    || (error as { message?: string })?.message
    || fallback;
}

function rowDefault(item: ScoreItem): ScoreValue {
  const additive = item.scoringMethod === 'additive';
  return {
    score: additive ? 0 : Number(item.maxScore || 0),
    deduction: 0,
    remark: '',
  };
}

function localDraftKey(userId: string | number | undefined, assignmentId: string | number) {
  return `competition-practical-judge-draft:${userId ?? 'anonymous'}:${assignmentId}`;
}

export function CompetitionPracticalJudgeApp() {
  const token = useAuthStore((state) => state.token);
  const tenantId = useAuthStore((state) => state.tenantId);
  const currentUser = useAuthStore((state) => state.currentUser);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string>();
  const [activeQuestionId, setActiveQuestionId] = useState<string>();
  const [scores, setScores] = useState<Record<string, ScoreValue>>({});
  const [online, setOnline] = useState(navigator.onLine);
  const [realtimeConnected, setRealtimeConnected] = useState(false);
  const [syncState, setSyncState] = useState<SyncState>('saved');
  const [dirty, setDirty] = useState(false);
  const hydratedAssignmentRef = useRef<string>();
  const openedAssignmentsRef = useRef(new Set<string>());
  const payloadFingerprintRef = useRef('');

  const tasksQuery = useQuery({
    queryKey: ['competition-practical-judge-tasks'],
    queryFn: listCompetitionPracticalJudgeTasks,
    refetchInterval: online ? 15_000 : false,
    staleTime: 0,
  });
  const tasks = tasksQuery.data ?? [];
  const selected = useMemo(
    () => tasks.find((item) => String(item.assignmentId) === selectedId),
    [selectedId, tasks],
  );
  const questions = useMemo(
    () => (selected?.assessmentSheet?.sheet.questions ?? []) as PracticalQuestion[],
    [selected?.assessmentSheet?.sheet.questions],
  );
  const rows = useMemo<ScoreRow[]>(() => questions.flatMap((question) => {
    const items = Array.isArray(question.scoreItems) ? question.scoreItems as ScoreItem[] : [];
    return items.map((item) => ({
      question,
      item: { ...item, scoringMethod: item.scoringMethod || question.scoringMethod || 'deductive' },
      key: `${question.paperQuestionId}:${item.itemNo}`,
    }));
  }), [questions]);
  const activeQuestion = useMemo(
    () => questions.find((question) => String(question.paperQuestionId) === activeQuestionId) || questions[0],
    [activeQuestionId, questions],
  );
  const activeRows = useMemo(
    () => rows.filter((row) => row.question.paperQuestionId === activeQuestion?.paperQuestionId),
    [activeQuestion?.paperQuestionId, rows],
  );
  const locked = selected?.scoreSheetStatus === 'submitted_locked';
  const scoringStarted = selected?.startStatus === 'started';
  const sessionCompleted = selected?.startStatus === 'completed';
  const startSessionReady = selected?.startStatus === 'ready';
  const isGroupLeader = selected?.judgeRole === 'leader';
  const activeQuestionStatus = questionStatusOf(selected, activeQuestion?.paperQuestionId);
  const questionLocked = activeQuestionStatus === 'submitted';

  useEffect(() => {
    if (!selectedId && tasks.length) setSelectedId(String(tasks[0].assignmentId));
  }, [selectedId, tasks]);

  useEffect(() => {
    if (!selected) return;
    const assignmentId = String(selected.assignmentId);
    if (hydratedAssignmentRef.current === assignmentId) return;
    hydratedAssignmentRef.current = assignmentId;
    const initial: Record<string, ScoreValue> = {};
    rows.forEach(({ key, item, question }) => {
      const progress = questionProgressOf(selected, question.paperQuestionId);
      const savedEntry = progress?.entries?.find((entry) => Number(entry.paperItemNo) === Number(item.itemNo));
      if (savedEntry && Number.isFinite(Number(savedEntry.score)) && Number.isFinite(Number(savedEntry.deduction))) {
        initial[key] = {
          score: Number(savedEntry.score),
          deduction: Number(savedEntry.deduction),
          remark: savedEntry.remark || '',
        };
        return;
      }
      initial[key] = rowDefault(item);
    });
    try {
      const local = JSON.parse(localStorage.getItem(localDraftKey(currentUser?.id, assignmentId)) || '{}') as Record<string, ScoreValue>;
      rows.forEach(({ key, item, question }) => {
        if (questionStatusOf(selected, question.paperQuestionId) === 'submitted') return;
        const saved = local[key];
        if (saved && Number.isFinite(Number(saved.score)) && Number.isFinite(Number(saved.deduction))) {
          initial[key] = { ...rowDefault(item), ...saved };
        }
      });
    } catch {
      localStorage.removeItem(localDraftKey(currentUser?.id, assignmentId));
    }
    setScores(initial);
    setActiveQuestionId(questions[0] ? String(questions[0].paperQuestionId) : undefined);
    setDirty(false);
    setSyncState('saved');
    payloadFingerprintRef.current = '';
  }, [currentUser?.id, questions, rows, selected]);

  useEffect(() => {
    if (!selected || !scoringStarted || selected.scoreSheetId) return;
    const assignmentId = String(selected.assignmentId);
    if (openedAssignmentsRef.current.has(assignmentId)) return;
    openedAssignmentsRef.current.add(assignmentId);
    void openCompetitionPracticalJudgeTask(selected.assignmentId)
      .then(() => queryClient.invalidateQueries({ queryKey: ['competition-practical-judge-tasks'] }))
      .catch((error) => {
        openedAssignmentsRef.current.delete(assignmentId);
        message.error(errorMessage(error, '评分任务打开失败'));
      });
  }, [queryClient, scoringStarted, selected]);

  useEffect(() => {
    const onOnline = () => {
      setOnline(true);
      setSyncState((current) => current === 'offline' ? 'syncing' : current);
    };
    const onOffline = () => {
      setOnline(false);
      setRealtimeConnected(false);
      setSyncState('offline');
    };
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, []);

  useEffect(() => {
    if (!token || !tenantId || !online) return;
    let socket: WebSocket | undefined;
    let disposed = false;
    createCompetitionPracticalRealtimeTicket().then((ticket) => {
      if (disposed) return;
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const query = new URLSearchParams({ ticket, tenantId: String(tenantId) });
      socket = new WebSocket(`${protocol}//${window.location.host}/api/ws/competition-practical?${query}`);
      socket.onopen = () => setRealtimeConnected(true);
      socket.onerror = () => setRealtimeConnected(false);
      socket.onclose = () => setRealtimeConnected(false);
      socket.onmessage = () => void queryClient.invalidateQueries({
        queryKey: ['competition-practical-judge-tasks'],
      });
    }).catch(() => setRealtimeConnected(false));
    return () => {
      disposed = true;
      socket?.close();
    };
  }, [online, queryClient, tenantId, token]);

  const entries = useMemo<CompetitionPracticalScoreEntryRequest[]>(() => activeRows.map(({ question, item, key }) => {
    const value = scores[key] ?? rowDefault(item);
    return {
      paperQuestionId: question.paperQuestionId,
      paperItemNo: item.itemNo,
      score: value.score,
      deduction: value.deduction,
      remark: value.remark || undefined,
    };
  }), [activeRows, scores]);
  const payloadFingerprint = useMemo(() => JSON.stringify(entries), [entries]);
  payloadFingerprintRef.current = payloadFingerprint;

  const saveMutation = useMutation({
    mutationFn: ({ entries: nextEntries, submit }: SaveVariables) =>
      saveCompetitionPracticalJudgeTask(selected!.assignmentId, nextEntries, submit),
    onSuccess: (result, variables) => {
      if (variables.submit) {
        setDirty(false);
        setSyncState('saved');
        if (result.scoreSheetStatus === 'submitted_locked') {
          localStorage.removeItem(localDraftKey(currentUser?.id, result.assignmentId));
          message.success('本题已提交，本场评分已锁定');
        } else {
          message.success('本题评分已提交');
        }
      } else if (variables.fingerprint === payloadFingerprintRef.current) {
        setDirty(false);
        setSyncState('saved');
      }
      queryClient.setQueryData<CompetitionPracticalJudgeTask[]>(
        ['competition-practical-judge-tasks'],
        (current) => (current ?? []).map((task) => (
          String(task.assignmentId) === String(result.assignmentId) ? result : task
        )),
      );
    },
    onError: (error, variables) => {
      setSyncState(online ? 'error' : 'offline');
      if (variables.submit) message.error(errorMessage(error, '评分提交失败'));
    },
  });

  const confirmStartMutation = useMutation({
    mutationFn: () => confirmCompetitionPracticalJudgeStart(selected!.assignmentId),
    onSuccess: (result) => {
      patchTaskCache(queryClient, result);
      message.success(result.startStatus === 'ready'
        ? '三名裁判均已确认，请等待组长开始考试'
        : '已确认选手与试卷，正在等待其他裁判');
    },
    onError: (error) => message.error(errorMessage(error, '开考确认失败，请刷新后重试')),
  });

  const startSessionMutation = useMutation({
    mutationFn: () => startCompetitionPracticalJudgeSession(selected!.assignmentId),
    onSuccess: (result) => {
      patchTaskCache(queryClient, result);
      void queryClient.invalidateQueries({ queryKey: ['competition-practical-judge-tasks'] });
      message.success('考试已开始，三台裁判设备已同步解锁评分');
    },
    onError: (error) => message.error(errorMessage(error, '开始考试失败，请检查三名裁判是否均已确认')),
  });

  useEffect(() => {
    if (!dirty || !selected) return;
    localStorage.setItem(localDraftKey(currentUser?.id, selected.assignmentId), JSON.stringify(scores));
  }, [currentUser?.id, dirty, scores, selected]);

  useEffect(() => {
    if (!dirty || !selected || !scoringStarted || questionLocked || locked || !activeRows.length) return;
    if (!online) {
      setSyncState('offline');
      return;
    }
    setSyncState('syncing');
    const timer = window.setTimeout(() => {
      saveMutation.mutate({ entries, fingerprint: payloadFingerprint, submit: false });
    }, 900);
    return () => window.clearTimeout(timer);
  // saveMutation is intentionally omitted: its object identity changes across renders.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dirty, entries, locked, online, payloadFingerprint, activeRows.length, questionLocked, scoringStarted, selected?.assignmentId]);

  const activeQuestionScore = activeRows.reduce(
    (sum, row) => sum + (scores[row.key]?.score ?? rowDefault(row.item).score),
    0,
  );
  const totalScore = questions.reduce((sum, question) => {
    if (question.paperQuestionId === activeQuestion?.paperQuestionId) {
      return sum + activeQuestionScore;
    }
    const progress = questionProgressOf(selected, question.paperQuestionId);
    return sum + Number(progress?.score ?? 0);
  }, 0);
  const submittedQuestionCount = questions.filter((question) => (
    questionStatusOf(selected, question.paperQuestionId) === 'submitted'
  )).length;
  const fullScore = Number(selected?.assessmentSheet?.totalScore ?? 0);
  const activeQuestionFullScore = activeRows.reduce((sum, row) => sum + Number(row.item.maxScore || 0), 0);

  function selectQuestion(nextId: string) {
    if (nextId === String(activeQuestion?.paperQuestionId ?? '')) return;
    if (dirty && selected && scoringStarted && !questionLocked && online && entries.length) {
      saveMutation.mutate({
        entries,
        fingerprint: payloadFingerprint,
        submit: false,
      });
    }
    setDirty(false);
    setActiveQuestionId(nextId);
  }

  function updateScore(key: string, item: ScoreItem, next: Partial<ScoreValue>) {
    setScores((current) => ({
      ...current,
      [key]: { ...(current[key] ?? rowDefault(item)), ...next },
    }));
    setDirty(true);
    setSyncState(online ? 'syncing' : 'offline');
  }

  function adjust(row: ScoreRow, delta: number) {
    const current = scores[row.key] ?? rowDefault(row.item);
    const maxScore = Number(row.item.maxScore || 0);
    if (row.item.scoringMethod === 'additive') {
      const score = Math.min(maxScore, Math.max(0, current.score + delta));
      updateScore(row.key, row.item, { score: Number(score.toFixed(2)), deduction: 0 });
      return;
    }
    const maxDeduction = Number(row.item.maxDeduction ?? maxScore);
    const deduction = Math.min(maxDeduction, Math.max(0, current.deduction + delta));
    updateScore(row.key, row.item, {
      deduction: Number(deduction.toFixed(2)),
      score: Number(Math.max(0, maxScore - deduction).toFixed(2)),
    });
  }

  async function handleLogout() {
    try {
      await logout();
    } catch {
      // 后端退出失败时仍清理本机登录态。
    } finally {
      clearAuth();
      window.location.assign('/competition-practical-judge');
    }
  }

  const detail = activeQuestion?.detail;
  const questionContent = detailText(detail, 'assessmentRequirementsHtml', 'operationContent') || '无补充题目说明';
  const candidatePrepare = detailText(detail, 'candidatePrepareHtml', 'studentPrepare') || '-';
  const equipmentPrepare = detailText(detail, 'examRoomPrepareHtml', 'equipmentPrepare') || '-';

  return (
    <div className="assessor-tablet-shell">
      <header className="assessor-tablet-header">
        <div>
          <Typography.Title level={3}>竞赛实操裁判</Typography.Title>
          <Typography.Text>{currentUser?.displayName || '裁判员'}，请独立完成本人评分</Typography.Text>
        </div>
        <Space wrap>
          <Tag icon={online ? <WifiOutlined /> : <CloudOutlined />} color={online ? 'success' : 'warning'}>
            {online ? '在线' : '短时断网'}
          </Tag>
          <Tag icon={<CloudSyncOutlined />} color={syncState === 'error' ? 'error' : 'processing'}>
            {syncText(syncState)}
          </Tag>
          <Tag color={realtimeConnected ? 'success' : 'default'}>
            {realtimeConnected ? '实时连接' : '轮询同步'}
          </Tag>
          <Button icon={<LogoutOutlined />} onClick={handleLogout}>切换账号</Button>
        </Space>
      </header>

      <main className={`assessor-tablet-main ${selected ? 'has-selection' : ''}`}>
        <aside className="assessor-task-list">
          <div className="assessor-task-list-title">
            <Typography.Title level={4}>当前竞赛任务</Typography.Title>
            <Button
              size="large"
              aria-label="刷新评分任务"
              icon={<ReloadOutlined />}
              onClick={() => tasksQuery.refetch()}
              disabled={!online}
            />
          </div>
          <Spin spinning={tasksQuery.isFetching}>
            <List
              dataSource={tasks}
              locale={{ emptyText: <Empty description="暂无评分任务" /> }}
              renderItem={(task) => {
                const status = taskStatusText(task);
                return (
                  <List.Item
                    className={String(task.assignmentId) === selectedId ? 'selected' : ''}
                    onClick={() => {
                      hydratedAssignmentRef.current = undefined;
                      setSelectedId(String(task.assignmentId));
                    }}
                  >
                    <div className="assessor-task-card">
                      <Space wrap>
                        <Tag color={status === '已提交' ? 'success' : status === '已退回' ? 'error' : 'blue'}>
                          {status}
                        </Tag>
                        <Tag color={task.judgeRole === 'leader' ? 'gold' : 'default'}>
                          {task.judgeRole === 'leader' ? '组长' : '组员'}
                        </Tag>
                        <strong>选手 {task.contestantMemberId}</strong>
                        <span>{task.workstationCode ? `工位 ${task.workstationCode}` : ''}</span>
                      </Space>
                      <Typography.Text>{task.assessmentSheet?.title || '实操考评表'}</Typography.Text>
                      <small>
                        {task.assessmentSheet?.questionCount || 0} 题 · {formatScore(Number(task.totalScore ?? 0))}/{formatScore(Number(task.assessmentSheet?.totalScore ?? 0))} 分
                      </small>
                    </div>
                  </List.Item>
                );
              }}
            />
          </Spin>
        </aside>

        <section className="assessor-score-workspace">
          {!selected ? <Empty description="请选择评分任务" /> : !questions.length ? (
            <Empty description="当前考评表暂无评分题目" />
          ) : activeQuestion ? (
            <>
              <Button className="assessor-mobile-back" icon={<LeftOutlined />} onClick={() => setSelectedId(undefined)}>
                返回任务
              </Button>

              <div className="assessor-candidate-bar">
                <div>
                  <Typography.Text type="secondary">当前选手</Typography.Text>
                  <Typography.Title level={3}>选手 {selected.contestantMemberId}</Typography.Title>
                  <Typography.Text>
                    {selected.workstationCode ? `工位 ${selected.workstationCode}` : '工位未设置'} / {selected.assessmentSheet.title}
                  </Typography.Text>
                </div>
                <div className="assessor-exam-stats">
                  <Tag color={isGroupLeader ? 'gold' : 'blue'}>
                    {isGroupLeader ? '裁判组组长' : '裁判组组员'}
                  </Tag>
                  <div className="assessor-live-score">
                    <span>整场实时得分</span>
                    <strong>{formatScore(totalScore)}<small>/{formatScore(fullScore)}</small></strong>
                  </div>
                  <div>
                    <span>题目提交</span>
                    <strong>{submittedQuestionCount}/{questions.length}</strong>
                  </div>
                </div>
              </div>

              {!online && (
                <Alert
                  type="warning"
                  showIcon
                  message="当前网络不可用"
                  description="评分内容会保留在当前页面，恢复联网后自动同步；联网前不能提交。"
                />
              )}
              {syncState === 'error' && (
                <Alert
                  type="error"
                  showIcon
                  message="评分草稿同步失败"
                  description="请检查网络后重试，或点击刷新按钮重新加载任务。"
                  action={<Button onClick={() => setDirty(true)}>重新同步</Button>}
                />
              )}

              {!scoringStarted && !sessionCompleted && (
                <div className="assessor-start-gate">
                  <Typography.Title level={3}>统一开考确认</Typography.Title>
                  <Typography.Paragraph>
                    请三名裁判分别在各自平板核对当前选手、工位和试卷；全部确认后，仅组长可以统一开考。
                  </Typography.Paragraph>
                  <div className="assessor-start-steps">
                    <Tag color={startSessionReady ? 'success' : 'processing'}>
                      裁判确认 {selected.confirmedJudges}/{selected.requiredJudges}
                    </Tag>
                    <Tag color="success">试卷已匹配</Tag>
                    <Tag color={isGroupLeader ? 'gold' : 'blue'}>
                      {isGroupLeader ? '当前设备：组长' : '当前设备：组员'}
                    </Tag>
                  </div>
                  {isGroupLeader && (
                    <Button
                      type="primary"
                      size="large"
                      disabled={!startSessionReady || !online}
                      loading={startSessionMutation.isPending}
                      onClick={() => startSessionMutation.mutate()}
                    >
                      {startSessionReady ? '组长开始考试' : '等待三台设备确认'}
                    </Button>
                  )}
                  {selected.currentJudgeConfirmed ? (
                    <Button size="large" disabled>本设备已确认</Button>
                  ) : (
                    <Button
                      size="large"
                      disabled={!online}
                      loading={confirmStartMutation.isPending}
                      onClick={() => confirmStartMutation.mutate()}
                    >
                      我已核对选手与试卷，确认就绪
                    </Button>
                  )}
                </div>
              )}

              <div className="assessor-question-nav">
                {questions.map((question, index) => {
                  const active = question.paperQuestionId === activeQuestion.paperQuestionId;
                  const status = questionStatusOf(selected, question.paperQuestionId);
                  const questionRows = rows.filter((row) => row.question.paperQuestionId === question.paperQuestionId);
                  const questionScore = active
                    ? activeQuestionScore
                    : Number(questionProgressOf(selected, question.paperQuestionId)?.score ?? 0);
                  const questionFullScore = questionRows.reduce((sum, row) => sum + Number(row.item.maxScore || 0), 0);
                  return (
                    <button
                      key={String(question.paperQuestionId)}
                      type="button"
                      className={active ? 'active' : ''}
                      onClick={() => selectQuestion(String(question.paperQuestionId))}
                    >
                      <span>试题 {index + 1}</span>
                      <strong>{plain(question.title) || `实操题 ${index + 1}`}</strong>
                      <small>{formatScore(questionScore)} / {formatScore(questionFullScore)} 分</small>
                      <Tag color={status === 'submitted' ? 'success' : status === 'returned' ? 'error' : 'blue'}>
                        {questionStatusText(status)}
                      </Tag>
                    </button>
                  );
                })}
              </div>

              <div className="assessor-question-card">
                <div className="assessor-question-title-row">
                  <Typography.Title level={3}>{plain(activeQuestion.title) || '实操题'}</Typography.Title>
                  <div className="assessor-question-score">
                    <span>本题得分</span>
                    <strong>{formatScore(activeQuestionScore)} / {formatScore(activeQuestionFullScore)}</strong>
                  </div>
                </div>
                <Typography.Paragraph style={{ whiteSpace: 'pre-line' }}>{questionContent}</Typography.Paragraph>
                <div className="assessor-prepare-grid">
                  <div><strong>考生准备</strong><span>{candidatePrepare}</span></div>
                  <div><strong>设备设施</strong><span>{equipmentPrepare}</span></div>
                </div>
              </div>

              <div className="assessor-score-items">
                {activeRows.map((row) => {
                  const value = scores[row.key] ?? rowDefault(row.item);
                  const step = Number(row.item.deductionStep ?? 0.5);
                  const additive = row.item.scoringMethod === 'additive';
                  const maxAdjustment = additive
                    ? Number(row.item.maxScore || 0)
                    : Number(row.item.maxDeduction ?? row.item.maxScore ?? 0);
                  const currentAdjustment = additive ? value.score : value.deduction;
                  return (
                    <article key={row.key} className="assessor-score-item">
                      <div className="assessor-score-item-head">
                        <div>
                          <span className="assessor-item-no">{row.item.itemNo}</span>
                          <Typography.Title level={4}>{plain(row.item.itemName || row.item.elementHtml) || `评分项 ${row.item.itemNo}`}</Typography.Title>
                        </div>
                        <strong>{formatScore(value.score)} / {formatScore(Number(row.item.maxScore))} 分</strong>
                      </div>
                      <Typography.Paragraph style={{ whiteSpace: 'pre-line' }}>
                        {plain(row.item.criterion || row.item.standardHtml) || '无补充评分标准'}
                      </Typography.Paragraph>
                      <div className="assessor-deduction-control">
                        <Button
                          icon={<MinusOutlined />}
                          disabled={!scoringStarted || sessionCompleted || questionLocked || currentAdjustment <= 0}
                          onClick={() => adjust(row, -step)}
                        >
                          {additive ? '减' : '撤销'} {formatScore(step)} 分
                        </Button>
                        <div>
                          <span>{additive ? '当前得分' : '当前扣分'}</span>
                          <strong>{formatScore(currentAdjustment)}</strong>
                        </div>
                        <Button
                          danger={!additive}
                          type="primary"
                          icon={<PlusOutlined />}
                          disabled={!scoringStarted || sessionCompleted || questionLocked || currentAdjustment >= maxAdjustment}
                          onClick={() => adjust(row, step)}
                        >
                          {additive ? '加' : '扣'} {formatScore(step)} 分
                        </Button>
                      </div>
                      <Input
                        size="large"
                        value={value.remark}
                        disabled={!scoringStarted || sessionCompleted || questionLocked}
                        onChange={(event) => updateScore(row.key, row.item, { remark: event.target.value })}
                        placeholder="扣分说明（选填）"
                      />
                    </article>
                  );
                })}
              </div>

              <footer className="assessor-submit-bar">
                <div>
                  <span>整场合计</span>
                  <strong>{formatScore(totalScore)} / {formatScore(fullScore)} 分</strong>
                </div>
                <Button
                  type="primary"
                  size="large"
                  icon={<CheckCircleOutlined />}
                  disabled={!scoringStarted || sessionCompleted || questionLocked || !online || !activeRows.length || syncState === 'syncing'}
                  loading={saveMutation.isPending}
                  onClick={() => Modal.confirm({
                    title: '确认提交本题评分？',
                    content: '提交后本题将锁定，只有管理员退回后才能重新评分。',
                    okText: '确认提交',
                    cancelText: '继续检查',
                    onOk: () => saveMutation.mutateAsync({
                      entries,
                      fingerprint: payloadFingerprint,
                      submit: true,
                    }),
                  })}
                >
                  {questionLocked ? '本题已提交' : online ? '提交本题' : '联网后提交'}
                </Button>
              </footer>
            </>
          ) : <Empty description="评分任务加载失败" />}
        </section>
      </main>
    </div>
  );
}

function patchTaskCache(
  queryClient: ReturnType<typeof useQueryClient>,
  task: CompetitionPracticalJudgeTask,
) {
  queryClient.setQueryData<CompetitionPracticalJudgeTask[]>(
    ['competition-practical-judge-tasks'],
    (current) => (current ?? []).map((item) => (
      String(item.assignmentId) === String(task.assignmentId) ? task : item
    )),
  );
}

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
import {
  Alert,
  Button,
  Empty,
  Input,
  List,
  Modal,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  completeMyPracticalSession,
  confirmMyPracticalStart,
  createPracticalWebSocketTicket,
  getMyAssessorExam,
  getMyAssessorWorkbenchContext,
  listMyAssessorExams,
  saveMyAssessorScoringDraft,
  startMyPracticalSession,
  submitMyAssessorScoringTask,
  switchMyAssessorWorkbenchTask,
} from '../../api/practicalScoring';
import { logout } from '../../api/auth';
import { useAuthStore } from '../../stores/authStore';
import type {
  AssessorExamDetail,
  AssessorExamListItem,
  AssessorWorkbenchContext,
  AssessorScoringTaskDetail,
  PracticalSaveScorePayload,
  PracticalStartSession,
} from '../../types/practicalScoring';
import {
  assessorDraftKey,
  deleteAssessorDraft,
  loadAssessorDraft,
  saveAssessorDraft,
} from '../../utils/assessorDraftDb';
import './TabletAssessorApp.css';

const FULL_SCORE = 100;

export function TabletAssessorApp() {
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((state) => state.currentUser);
  const token = useAuthStore((state) => state.token);
  const tenantId = useAuthStore((state) => state.tenantId);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [selectedExamKey, setSelectedExamKey] = useState<string>();
  const [activeTaskId, setActiveTaskId] = useState<string>();
  const [deductions, setDeductions] = useState<Record<number, number>>({});
  const [remarks, setRemarks] = useState<Record<number, string>>({});
  const [note, setNote] = useState('');
  const [serverVersion, setServerVersion] = useState(0);
  const [online, setOnline] = useState(navigator.onLine);
  const [syncState, setSyncState] = useState<'saved' | 'syncing' | 'offline' | 'conflict'>('saved');
  const hydratedTaskRef = useRef<string>();
  const previousWorkbenchTaskRef = useRef<string>();
  const syncTimerRef = useRef<number>();
  const skipNextDraftSyncRef = useRef(false);

  const workbenchContextQuery = useQuery({
    queryKey: ['my-practical-scoring-workbench-context'],
    queryFn: () => getMyAssessorWorkbenchContext(),
    refetchInterval: online ? 10_000 : false,
    staleTime: 0,
  });

  const examsQuery = useQuery({
    queryKey: ['my-practical-scoring-exams', workbenchContextQuery.data?.activeTaskId],
    queryFn: () => listMyAssessorExams(),
    enabled: workbenchContextQuery.isSuccess,
    refetchInterval: online ? 30_000 : false,
  });

  const switchWorkbenchTaskMutation = useMutation({
    mutationFn: (taskId: string) => switchMyAssessorWorkbenchTask(taskId),
    onSuccess: (context) => {
      queryClient.setQueryData<AssessorWorkbenchContext>(['my-practical-scoring-workbench-context'], context);
      setSelectedExamKey(undefined);
      setActiveTaskId(undefined);
      hydratedTaskRef.current = undefined;
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exam'] });
      message.success('已切换当前认定任务');
    },
  });

  const selectedExam = useMemo(
    () => (examsQuery.data || []).find((exam) => examKey(exam) === selectedExamKey),
    [examsQuery.data, selectedExamKey],
  );

  const examQuery = useQuery({
    queryKey: ['my-practical-scoring-exam', selectedExam?.taskId, selectedExam?.candidateId],
    queryFn: () => getMyAssessorExam(selectedExam!.taskId, selectedExam!.candidateId),
    enabled: Boolean(selectedExam),
    // 评分面板需要及时感知管理端的退回/状态变化，否则考评员会停留在旧的评分快照上
    refetchInterval: online ? 10_000 : false,
    staleTime: 0,
  });

  const activeDetail = useMemo(() => {
    const questions = examQuery.data?.questions || [];
    return questions.find((question) => question.task.id === activeTaskId) || questions[0];
  }, [activeTaskId, examQuery.data]);

  useEffect(() => {
    if (!selectedExamKey && examsQuery.data?.length) {
      setSelectedExamKey(examKey(examsQuery.data[0]));
    }
  }, [examsQuery.data, selectedExamKey]);

  useEffect(() => {
    const currentTaskId = workbenchContextQuery.data?.activeTaskId;
    if (!currentTaskId) return;
    const previousTaskId = previousWorkbenchTaskRef.current;
    previousWorkbenchTaskRef.current = currentTaskId;
    if (previousTaskId && previousTaskId !== currentTaskId) {
      hydratedTaskRef.current = undefined;
      setSelectedExamKey(undefined);
      setActiveTaskId(undefined);
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exam'] });
      if (!workbenchContextQuery.data?.leader) {
        message.info('组长已切换当前认定任务，评分列表已同步更新');
      }
    }
  }, [queryClient, workbenchContextQuery.data?.activeTaskId, workbenchContextQuery.data?.leader]);

  useEffect(() => {
    const questions = examQuery.data?.questions || [];
    if (!questions.length) {
      setActiveTaskId(undefined);
      return;
    }
    if (!activeTaskId || !questions.some((question) => question.task.id === activeTaskId)) {
      hydratedTaskRef.current = undefined;
      setActiveTaskId(questions[0].task.id);
    }
  }, [activeTaskId, examQuery.data]);

  useEffect(() => {
    const onOnline = () => {
      setOnline(true);
      setSyncState('syncing');
    };
    const onOffline = () => {
      setOnline(false);
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
    const detail = activeDetail;
    if (!detail || !currentUser) return;
    // 退回/重新编辑会让 task.id 不变但 revision、version、status 发生变化，
    // 必须以这三者组合作为水合去重键，否则考评员面板会停留在旧分数 / 旧版本上，
    // 导致退回后无法重新编辑或提交时出现版本冲突。
    const hydrateKey = `${detail.task.id}@${detail.task.revision}@${detail.task.version}@${detail.task.status}`;
    if (hydratedTaskRef.current === hydrateKey) return;
    hydratedTaskRef.current = hydrateKey;
    const key = assessorDraftKey(currentUser.id, detail.task.id, detail.task.revision);
    loadAssessorDraft(key).then((local) => {
      const source = local && local.version >= detail.task.version ? local : payloadFromDetail(detail);
      skipNextDraftSyncRef.current = true;
      setDeductions(Object.fromEntries(source.items.map((item) => [item.itemNo, item.deductedScore])));
      setRemarks(Object.fromEntries(source.items.map((item) => [item.itemNo, item.remark || ''])));
      setNote(source.note || '');
      setServerVersion(Math.max(detail.task.version, source.version));
      setSyncState(local && !online ? 'offline' : 'saved');
    }).catch(() => {
      const source = payloadFromDetail(detail);
      skipNextDraftSyncRef.current = true;
      setDeductions(Object.fromEntries(source.items.map((item) => [item.itemNo, item.deductedScore])));
      setRemarks(Object.fromEntries(source.items.map((item) => [item.itemNo, item.remark || ''])));
      setNote(source.note || '');
      setServerVersion(detail.task.version);
    });
  }, [activeDetail, currentUser, online]);

  const payload = useMemo<PracticalSaveScorePayload | undefined>(() => {
    if (!activeDetail) return undefined;
    return {
      version: serverVersion,
      items: activeDetail.scoreItems.map((item) => ({
        scoreItemId: item.id,
        itemNo: item.itemNo,
        deductedScore: deductions[item.itemNo] ?? item.deductedScore ?? 0,
        remark: remarks[item.itemNo] || undefined,
      })),
      note: note || undefined,
    };
  }, [activeDetail, deductions, note, remarks, serverVersion]);

  const draftMutation = useMutation({
    mutationFn: ({ taskId, value }: { taskId: string; value: PracticalSaveScorePayload }) =>
      saveMyAssessorScoringDraft(taskId, value),
    onSuccess: (detail) => {
      setServerVersion(detail.task.version);
      setSyncState('saved');
      patchQuestionCache(queryClient, examQuery.data, detail);
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
    },
    onError: (error: { response?: { status?: number }; message?: string }) => {
      const conflict = error?.response?.status === 409 || error?.message?.includes('其他设备');
      setSyncState(conflict ? 'conflict' : 'offline');
    },
  });

  const confirmStartMutation = useMutation({
    mutationFn: () => confirmMyPracticalStart(activeDetail!.task.id),
    onSuccess: (startSession) => {
      patchStartSessionCache(queryClient, examQuery.data, startSession);
      message.success(startSession.status === 'started' ? '全员已确认，统一开始' : '已确认，正在等待其他人员');
    },
  });

  const startSessionMutation = useMutation({
    mutationFn: () => startMyPracticalSession(activeDetail!.task.id),
    onSuccess: (startSession) => {
      patchStartSessionCache(queryClient, examQuery.data, startSession);
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
      message.success('考试已开始，评分界面已同步解锁');
    },
  });

  const completeSessionMutation = useMutation({
    mutationFn: () => completeMyPracticalSession(activeDetail!.task.id),
    onSuccess: (startSession) => {
      patchStartSessionCache(queryClient, examQuery.data, startSession);
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
      message.success('考试已完成，评分数据已锁定');
    },
    onError: (error: unknown) => {
      message.error(errorMessage(error, '完成考试失败，请检查评分状态'));
    },
  });

  useEffect(() => {
    const exam = examQuery.data?.exam;
    if (!exam || !token || !tenantId || !online || ['started', 'completed'].includes(exam.startSession?.status || '')) {
      return;
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    let socket: WebSocket | undefined;
    let disposed = false;
    createPracticalWebSocketTicket().then((ticket) => {
      if (disposed) return;
      const params = new URLSearchParams({
        ticket,
        tenantId: String(tenantId),
        taskId: exam.taskId,
        candidateId: exam.candidateId,
      });
      socket = new WebSocket(`${protocol}//${window.location.host}/api/ws/practical-scoring?${params}`);
      socket.onmessage = (event) => {
        const startSession = JSON.parse(event.data);
        patchStartSessionCache(queryClient, examQuery.data, startSession);
        if (startSession.status === 'started') {
          message.success('考生与三名考评员均已确认，现在统一开始评分');
        }
      };
    });
    return () => {
      disposed = true;
      socket?.close();
    };
  }, [examQuery.data, online, queryClient, tenantId, token]);

  useEffect(() => {
    if (!payload || !activeDetail || !currentUser) return;
    if (skipNextDraftSyncRef.current) {
      skipNextDraftSyncRef.current = false;
      return;
    }
    if (activeDetail.task.status === 'submitted'
        || activeDetail.startSession?.status !== 'started') return;
    const key = assessorDraftKey(currentUser.id, activeDetail.task.id, activeDetail.task.revision);
    saveAssessorDraft({
      ...payload,
      key,
      taskId: activeDetail.task.id,
      revision: activeDetail.task.revision,
      savedAt: Date.now(),
    }).catch(() => message.warning('本机草稿保存失败，请勿关闭页面'));
    if (syncTimerRef.current) window.clearTimeout(syncTimerRef.current);
    if (!online || syncState === 'conflict') {
      setSyncState(online ? 'conflict' : 'offline');
      return;
    }
    setSyncState('syncing');
    syncTimerRef.current = window.setTimeout(() => {
      draftMutation.mutate({ taskId: activeDetail.task.id, value: payload });
    }, 900);
    return () => {
      if (syncTimerRef.current) window.clearTimeout(syncTimerRef.current);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deductions, remarks, note, online]);

  useEffect(() => {
    if (online && syncState === 'offline' && payload && activeDetail) {
      setSyncState('syncing');
      draftMutation.mutate({ taskId: activeDetail.task.id, value: payload });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [online]);

  const submitMutation = useMutation({
    mutationFn: () => submitMyAssessorScoringTask(activeDetail!.task.id, payload!),
    onSuccess: async (detail) => {
      if (currentUser) {
        await deleteAssessorDraft(assessorDraftKey(currentUser.id, detail.task.id, detail.task.revision));
      }
      message.success('本题评分已提交并锁定');
      patchQuestionCache(queryClient, examQuery.data, detail);
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
      queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exam', detail.task.taskId, detail.task.candidateId] });
      setSyncState('saved');
    },
  });

  const scoringStarted = activeDetail?.startSession?.status === 'started';
  const sessionCompleted = activeDetail?.startSession?.status === 'completed';
  // ready 表示可开考；waiting 且考评员已满确认时同样视为可开考，
  // 兼容历史 waiting 会话（后端会在开始时把 waiting 提升为 started）
  const startSession =
    activeDetail?.startSession?.status === 'ready'
    || (activeDetail?.startSession?.status === 'waiting'
      && activeDetail?.startSession?.confirmedAssessors === activeDetail?.startSession?.requiredAssessors);
  const startSessionReady = Boolean(startSession);
  const isGroupLeader = Boolean(activeDetail?.task.leader);
  const examReviewRequired = Boolean(examQuery.data?.exam.reviewRequired);
  const questionTotal = activeDetail ? questionLiveScore(activeDetail, deductions) : 0;
  const examTotal = examLiveScore(examQuery.data, activeDetail?.task.id, questionTotal);
  const fullScore = FULL_SCORE;
  const ownSubmittedQuestions = examQuery.data?.questions.filter((question) => question.task.status === 'submitted').length ?? 0;
  const allOwnQuestionsSubmitted = Boolean(examQuery.data?.questions.length)
    && ownSubmittedQuestions === examQuery.data?.questions.length;

  function adjust(itemNo: number, delta: number, max: number) {
    setDeductions((current) => {
      const next = Math.min(max, Math.max(0, (current[itemNo] || 0) + delta));
      return { ...current, [itemNo]: Number(next.toFixed(2)) };
    });
  }

  async function handleLogout() {
    try {
      await logout();
    } catch {
      // 后端退出失败时仍清理本机登录态
    } finally {
      clearAuth();
      window.location.assign('/assessor-scoring');
    }
  }

  function switchWorkbenchTask(taskId: string) {
    const task = workbenchContextQuery.data?.switchableTasks.find(
      (item) => item.taskId === workbenchContextQuery.data?.activeTaskId,
    );
    const execute = () => switchWorkbenchTaskMutation.mutate(taskId);
    if (task && task.startedSessionCount > 0) {
      Modal.confirm({
        title: '当前任务仍有进行中的考试',
        content: `该任务有 ${task.startedSessionCount} 个已开考未完成场次。切换不会中断评分，确认后可继续在其他任务操作。`,
        okText: '仍要切换',
        cancelText: '留在当前任务',
        onOk: execute,
      });
      return;
    }
    execute();
  }

  return (
    <div className="assessor-tablet-shell">
      <header className="assessor-tablet-header">
        <div>
          <Typography.Title level={3}>实操考评</Typography.Title>
          <Typography.Text>{currentUser?.displayName || '考评员'}，请独立完成本人评分</Typography.Text>
        </div>
        <Space>
          {workbenchContextQuery.data?.leader ? (
            <Select
              aria-label="当前认定任务"
              style={{ minWidth: 250 }}
              value={workbenchContextQuery.data.activeTaskId}
              loading={workbenchContextQuery.isFetching}
              disabled={switchWorkbenchTaskMutation.isPending}
              onChange={switchWorkbenchTask}
              options={workbenchContextQuery.data.switchableTasks.map((task) => ({
                value: task.taskId,
                label: [task.taskNo, task.taskName].filter(Boolean).join(' ') || `认定任务 ${task.taskId}`,
              }))}
            />
          ) : workbenchContextQuery.data?.leaderAssessorName ? (
            <Tag color="blue">跟随组长：{workbenchContextQuery.data.leaderAssessorName}</Tag>
          ) : null}
          <Tag icon={online ? <WifiOutlined /> : <CloudOutlined />} color={online ? 'success' : 'warning'}>
            {online ? '在线' : '短时断网'}
          </Tag>
          <Tag icon={<CloudSyncOutlined />} color={syncState === 'conflict' ? 'error' : 'processing'}>
            {syncText(syncState)}
          </Tag>
          <Button icon={<LogoutOutlined />} onClick={handleLogout}>切换账号</Button>
        </Space>
      </header>

      <main className={`assessor-tablet-main ${selectedExam ? 'has-selection' : ''}`}>
        <aside className="assessor-task-list">
          <div className="assessor-task-list-title">
            <Typography.Title level={4}>当前认定任务</Typography.Title>
            <Button
              size="large"
              icon={<ReloadOutlined />}
              onClick={() => examsQuery.refetch()}
              disabled={!online}
            />
          </div>
          <Spin spinning={examsQuery.isFetching}>
            <List
              dataSource={examsQuery.data || []}
              locale={{ emptyText: <Empty description="暂无考试任务" /> }}
              renderItem={(exam) => (
                <List.Item
                  className={examKey(exam) === selectedExamKey ? 'selected' : ''}
                  onClick={() => {
                    hydratedTaskRef.current = undefined;
                    setActiveTaskId(undefined);
                    setSelectedExamKey(examKey(exam));
                  }}
                >
                  <div className="assessor-task-card">
                    <Space wrap>
                      <Tag color={exam.status === 'submitted' ? 'success' : exam.status === 'returned' ? 'error' : 'blue'}>
                        {statusText(exam.status)}
                      </Tag>
                      {exam.reviewRequired ? <Tag color="error">分差异常</Tag> : null}
                      <strong>{exam.candidateName}</strong>
                      <span>{exam.candidateNo || ''}</span>
                    </Space>
                    <Typography.Text>{exam.workTypeName || '-'} / {exam.levelName || '-'}</Typography.Text>
                    <small>{exam.submittedQuestions}/{exam.totalQuestions} 题已提交 · {exam.currentScore || 0}/{FULL_SCORE} 分</small>
                  </div>
                </List.Item>
              )}
            />
          </Spin>
        </aside>

        <section className="assessor-score-workspace">
          {!selectedExam ? <Empty description="请选择考试任务" /> : !examQuery.data && examQuery.isFetching ? <Spin size="large" /> : activeDetail && examQuery.data ? (
            <>
              <Button className="assessor-mobile-back" icon={<LeftOutlined />} onClick={() => setSelectedExamKey(undefined)}>
                返回任务
              </Button>

              <div className="assessor-candidate-bar">
                <div>
                  <Typography.Text type="secondary">当前考生</Typography.Text>
                  <Typography.Title level={3}>{examQuery.data.exam.candidateName}</Typography.Title>
                  <Typography.Text>
                    {examQuery.data.exam.candidateNo || '-'} / {examQuery.data.exam.workTypeName || '-'} / {examQuery.data.exam.levelName || '-'}
                  </Typography.Text>
                </div>
                <div className="assessor-exam-stats">
                  <Tag color={isGroupLeader ? 'gold' : 'blue'}>
                    {isGroupLeader ? '组长端' : '组员端'}
                  </Tag>
                  <div className="assessor-live-score">
                    <span>整场实时得分</span>
                    <strong>{formatScore(examTotal)}<small>/{fullScore}</small></strong>
                  </div>
                  <div>
                    <span>题目提交</span>
                    <strong>{ownSubmittedQuestions}/{examQuery.data.questions.length}</strong>
                  </div>
                </div>
              </div>

              {activeDetail.task.status === 'returned' && (
                <Alert type="warning" showIcon message="管理员已退回，请重新评分" description={activeDetail.returnReason} />
              )}
              {isGroupLeader && examReviewRequired && (
                <Alert
                  type="error"
                  showIcon
                  message="成绩分差异常，暂不能完成考试"
                  description="三名考评员给出的成绩偏差超过10分，请在实操管理中修改异常分数并重新提交。修正后本页会自动刷新状态。"
                />
              )}
              {syncState === 'conflict' && (
                <Alert
                  type="error"
                  showIcon
                  message="该题已在其他设备更新"
                  description="系统已停止自动覆盖。请重新加载服务端版本后继续。"
                  action={<Button onClick={() => window.location.reload()}>重新加载</Button>}
                />
              )}
              {!scoringStarted && activeDetail.startSession && (
                <div className="assessor-start-gate">
                  <Typography.Title level={3}>统一开考确认</Typography.Title>
                  <div className="assessor-start-steps">
                    <Tag color={activeDetail.startSession.confirmedAssessors === activeDetail.startSession.requiredAssessors ? 'success' : 'processing'}>
                      考评员 {activeDetail.startSession.confirmedAssessors}/{activeDetail.startSession.requiredAssessors}
                    </Tag>
                    <Tag color="success">试卷已匹配</Tag>
                  </div>
                  <Button
                    type="primary"
                    size="large"
                    style={{ display: isGroupLeader ? undefined : 'none' }}
                    disabled={!startSessionReady}
                    loading={startSessionMutation.isPending}
                    onClick={() => startSessionMutation.mutate()}
                  >
                    {startSessionReady ? '组长开始考试' : '等待全部确认'}
                  </Button>
                  {activeDetail.startSession.currentAssessorConfirmed ? (
                    <Button size="large" disabled>
                      已确认
                    </Button>
                  ) : null}
                  <Button
                    size="large"
                    style={{ display: activeDetail.startSession.currentAssessorConfirmed ? 'none' : undefined }}
                    disabled={activeDetail.startSession.currentAssessorConfirmed}
                    loading={confirmStartMutation.isPending}
                    onClick={() => confirmStartMutation.mutate()}
                  >
                    我已核对考生与试卷，确认就绪
                  </Button>
                </div>
              )}

              <div className="assessor-question-nav">
                {examQuery.data.questions.map((question, index) => {
                  const active = question.task.id === activeDetail.task.id;
                  const score = active ? questionTotal : (question.task.totalScore || 0);
                  return (
                    <button
                      key={question.task.id}
                      type="button"
                      className={active ? 'active' : ''}
                      onClick={() => {
                        hydratedTaskRef.current = undefined;
                        setActiveTaskId(question.task.id);
                      }}
                    >
                      <span>试题 {index + 1}</span>
                      <strong>{shortTitle(question.task.questionTitle)}</strong>
                      <small>{formatScore(score)} / {formatScore(questionMaxScore(question))} 分</small>
                      <Tag color={question.task.status === 'submitted' ? 'success' : question.task.status === 'returned' ? 'error' : 'blue'}>
                        {statusText(question.task.status)}
                      </Tag>
                    </button>
                  );
                })}
              </div>

              <div className="assessor-question-card">
                <div className="assessor-question-title-row">
                  <Typography.Title level={3}>{activeDetail.task.questionTitle}</Typography.Title>
                  <div className="assessor-question-score">
                    <span>本题得分</span>
                    <strong>{formatScore(questionTotal)} / {formatScore(questionMaxScore(activeDetail))}</strong>
                  </div>
                </div>
                <Typography.Paragraph>{activeDetail.questionContent || '无补充题目说明'}</Typography.Paragraph>
                <div className="assessor-prepare-grid">
                  <div><strong>考生准备</strong><span>{activeDetail.studentPrepare || '-'}</span></div>
                  <div><strong>设备设施</strong><span>{activeDetail.equipmentPrepare || '-'}</span></div>
                </div>
              </div>

              <div className="assessor-score-items">
                {activeDetail.scoreItems.map((item) => {
                  const deducted = deductions[item.itemNo] ?? item.deductedScore ?? 0;
                  const awarded = Math.max(0, item.maxScore - deducted);
                  return (
                    <article key={item.id} className="assessor-score-item">
                      <div className="assessor-score-item-head">
                        <div>
                          <span className="assessor-item-no">{item.itemNo}</span>
                          <Typography.Title level={4}>{item.itemName}</Typography.Title>
                        </div>
                        <strong>{formatScore(awarded)} / {formatScore(item.maxScore)} 分</strong>
                      </div>
                      <Typography.Paragraph>{item.criterion}</Typography.Paragraph>
                      <div className="assessor-deduction-control">
                        <Button
                          icon={<MinusOutlined />}
                          disabled={!scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted' || deducted <= 0}
                          onClick={() => adjust(item.itemNo, -item.deductionStep, item.maxDeduction)}
                        >
                          撤销 {formatScore(item.deductionStep)} 分
                        </Button>
                        <div><span>当前扣分</span><strong>{formatScore(deducted)}</strong></div>
                        <Button
                          danger
                          type="primary"
                          icon={<PlusOutlined />}
                          disabled={!scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted' || deducted >= item.maxDeduction}
                          onClick={() => adjust(item.itemNo, item.deductionStep, item.maxDeduction)}
                        >
                          扣 {formatScore(item.deductionStep)} 分
                        </Button>
                      </div>
                      <Input
                        size="large"
                        value={remarks[item.itemNo]}
                        disabled={!scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted'}
                        onChange={(event) => setRemarks((current) => ({ ...current, [item.itemNo]: event.target.value }))}
                        placeholder="扣分说明（选填）"
                      />
                    </article>
                  );
                })}
              </div>

              <div className="assessor-note">
                <Typography.Text strong>本题考评说明</Typography.Text>
                <Input.TextArea
                  rows={3}
                  size="large"
                  value={note}
                  disabled={!scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted'}
                  onChange={(event) => setNote(event.target.value)}
                  placeholder="可记录现场情况，仅管理人员可查看"
                />
              </div>

              <footer className="assessor-submit-bar">
                <div>
                  <span>整场合计</span>
                  <strong>{formatScore(examTotal)} / {fullScore} 分</strong>
                </div>
                <Button
                  type="primary"
                  size="large"
                  icon={<CheckCircleOutlined />}
                  disabled={!scoringStarted || sessionCompleted || !online || syncState === 'syncing'
                    || syncState === 'conflict' || activeDetail.task.status === 'submitted'}
                  loading={submitMutation.isPending}
                  onClick={() => Modal.confirm({
                    title: '确认提交本题评分？',
                    content: '提交后本题将锁定，只有管理员退回后才能重新评分。',
                    okText: '确认提交',
                    cancelText: '继续检查',
                    onOk: () => submitMutation.mutateAsync(),
                  })}
                >
                  {activeDetail.task.status === 'submitted' ? '本题已提交' : online ? '提交本题' : '联网后提交'}
                </Button>
                <Button
                  size="large"
                  disabled={!scoringStarted || !allOwnQuestionsSubmitted || sessionCompleted || !isGroupLeader || examReviewRequired}
                  loading={completeSessionMutation.isPending}
                  onClick={() => Modal.confirm({
                    title: '确认完成本场考试？',
                    content: '系统会校验三名考评员是否全部提交，完成后锁定评分并生成汇总记录。',
                    okText: '完成考试',
                    cancelText: '再检查一下',
                    onOk: () => completeSessionMutation.mutateAsync(),
                  })}
                >
                  {sessionCompleted
                    ? '考试已完成'
                    : examReviewRequired
                      ? '等待实操管理修正异常'
                      : !isGroupLeader && allOwnQuestionsSubmitted
                        ? '等待组长完成考试'
                        : '完成考试'}
                </Button>
              </footer>
            </>
          ) : <Empty description="考试任务加载失败" />}
        </section>
      </main>
    </div>
  );
}

function payloadFromDetail(detail: AssessorScoringTaskDetail): PracticalSaveScorePayload {
  return {
    version: detail.task.version,
    items: detail.scoreItems.map((item) => ({
      scoreItemId: item.id,
      itemNo: item.itemNo,
      deductedScore: item.deductedScore || 0,
      remark: item.remark,
    })),
    note: detail.progressNote,
  };
}

function patchQuestionCache(
  queryClient: ReturnType<typeof useQueryClient>,
  exam: AssessorExamDetail | undefined,
  detail: AssessorScoringTaskDetail,
) {
  if (!exam) return;
  queryClient.setQueryData<AssessorExamDetail>(
    ['my-practical-scoring-exam', detail.task.taskId, detail.task.candidateId],
    {
      ...exam,
      questions: exam.questions.map((question) => question.task.id === detail.task.id ? detail : question),
    },
  );
}

function patchStartSessionCache(
  queryClient: ReturnType<typeof useQueryClient>,
  exam: AssessorExamDetail | undefined,
  startSession: AssessorScoringTaskDetail['startSession'],
) {
  if (!exam || !startSession) return;
  queryClient.setQueryData<AssessorExamDetail>(
    ['my-practical-scoring-exam', exam.exam.taskId, exam.exam.candidateId],
    {
      ...exam,
      exam: { ...exam.exam, startSession: mergeStartSession(exam.exam.startSession, startSession) },
      questions: exam.questions.map((question) => ({
        ...question,
        startSession: mergeStartSession(question.startSession, startSession),
      })),
    },
  );
}

function mergeStartSession(
  current: PracticalStartSession | undefined,
  incoming: PracticalStartSession,
) {
  return current
    ? { ...incoming, currentAssessorConfirmed: current.currentAssessorConfirmed || incoming.currentAssessorConfirmed }
    : incoming;
}

function examKey(exam: AssessorExamListItem) {
  return `${exam.taskId}:${exam.candidateId}:${exam.paperId}`;
}

function questionLiveScore(detail: AssessorScoringTaskDetail, deductions: Record<number, number>) {
  return detail.scoreItems.reduce(
    (sum, item) => sum + Math.max(0, item.maxScore - (deductions[item.itemNo] ?? item.deductedScore ?? 0)),
    0,
  );
}

function examLiveScore(exam: AssessorExamDetail | undefined, activeTaskId: string | undefined, activeScore: number) {
  if (!exam) return 0;
  return exam.questions.reduce((sum, question) => (
    sum + (question.task.id === activeTaskId ? activeScore : (question.task.totalScore || 0))
  ), 0);
}

function questionMaxScore(detail: AssessorScoringTaskDetail) {
  return detail.scoreItems.reduce((sum, item) => sum + item.maxScore, 0);
}

function shortTitle(title?: string) {
  if (!title) return '-';
  return title.replace(/^试题\s*\d+[、.．]?\s*/, '').replace(/[（(]\s*\d+(?:\.\d+)?\s*分\s*[）)]/, '').trim();
}

function formatScore(value?: number) {
  if (value == null) return '0';
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, '');
}

function statusText(status: string) {
  return { assigned: '待评分', draft: '评分中', submitted: '已提交', returned: '已退回' }[status] || status;
}

function syncText(status: string) {
  return { saved: '已同步', syncing: '同步中', offline: '已保存到本机', conflict: '版本冲突' }[status] || status;
}

function errorMessage(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
    || (error as { message?: string })?.message
    || fallback;
}

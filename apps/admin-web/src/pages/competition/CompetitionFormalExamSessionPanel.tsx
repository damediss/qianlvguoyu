import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import dayjs from 'dayjs';
import {
  getCompetitionFormalCandidateExam,
  getCompetitionFormalCandidateResult,
  getCompetitionFormalExamSession,
  getCompetitionFormalExamSessionPreflight,
  pageCompetitionFormalCandidateExams,
  startCompetitionFormalExamSession,
  forceSubmitCompetitionFormalCandidateExam,
  forceOfflineCompetitionFormalCandidateExam,
  scoreCompetitionFormalExamSession,
  fetchFormalExamConsoleRealtimeEvents,
} from '../../api/competition';
import type { PageResponse } from '../../types/api';
import type {
  CompetitionFormalCandidateExam,
  CompetitionFormalExamSession,
  CompetitionId,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import {
  FORMAL_EXAM_CONNECTION_LABELS,
  FORMAL_EXAM_CONSOLE_DISCONNECT_BANNER,
  applyFormalExamConsoleEnvelope,
  formalExamConsoleCandidatesQueryKey,
  formalExamConsolePollIntervalMs,
  formalExamConsoleSessionQueryKey,
  resolveFormalExamConnectionState,
  startFormalExamConsoleRealtime,
  type FormalExamConsoleRealtimeHandle,
} from './FormalExamConsoleRealtime';
import './CompetitionFormalExamSessionPanel.css';

const sessionStatusMeta: Record<string, { label: string; color: string }> = {
  preparing: { label: '准备中', color: 'processing' },
  ready: { label: '准备完成', color: 'success' },
  running: { label: '比赛中', color: 'blue' },
  finished: { label: '已结束', color: 'default' },
  cancelled: { label: '已取消', color: 'error' },
};

const candidateStatusMeta: Record<string, { label: string; color: string }> = {
  waiting_enter: { label: '待进入', color: 'gold' },
  ready: { label: '已候考', color: 'blue' },
  running: { label: '考试中', color: 'processing' },
  submitted: { label: '已交卷', color: 'success' },
  auto_submitted: { label: '自动交卷', color: 'orange' },
  forced_submitted: { label: '强制交卷', color: 'red' },
  absent: { label: '缺考', color: 'default' },
};

const scoringStatusMeta: Record<string, { label: string; color: string }> = {
  not_started: { label: '待评分', color: 'gold' },
  scoring: { label: '评分中', color: 'processing' },
  completed: { label: '评分完成', color: 'success' },
  failed: { label: '评分失败', color: 'error' },
};

const questionTypeLabel: Record<string, string> = {
  single: '单选',
  multi: '多选',
  judge: '判断',
  fill: '填空',
};

function formatScore(value?: string | number | null) {
  if (value == null || value === '') return '--';
  const number = Number(value);
  if (Number.isNaN(number)) return String(value);
  return number.toFixed(2);
}

function candidateScoreText(row: CompetitionFormalCandidateExam) {
  if (row.status === 'absent' || row.resultStatus === 'absent') {
    return '缺考';
  }
  if (row.resultStatus === 'scored' && row.totalScore != null && row.fullScore != null) {
    return `${formatScore(row.totalScore)} / ${formatScore(row.fullScore)}`;
  }
  return '--';
}

const candidateStatusOptions = Object.entries(candidateStatusMeta).map(([value, meta]) => ({
  value,
  label: meta.label,
}));

function durationLabel(seconds?: number | null) {
  if (!seconds || seconds <= 0) return '-';
  return `${Math.round(seconds / 60)} 分钟`;
}

function examTypeLabel(value?: string | null) {
  if (value === 'theory') return '理论考试';
  if (value === 'practical') return '实操考试';
  if (value === 'simulation') return '仿真考试';
  return value || '-';
}

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--';
}

function formatClockTime(value?: string | null) {
  if (!value) return '--';
  const normalized = value.includes('T') ? value : value.replace(' ', 'T');
  const parsed = dayjs(normalized);
  return parsed.isValid() ? parsed.format('HH:mm:ss') : '--';
}

function paperLabel(code?: string | null, name?: string | null) {
  const codeText = code ? `${code}卷` : '-';
  return name ? `${codeText} · ${name}` : codeText;
}

function sessionBanner(status?: string) {
  if (status === 'preparing') {
    return { type: 'warning' as const, message: '正式比赛数据仍在准备中' };
  }
  if (status === 'cancelled') {
    return { type: 'error' as const, message: '本场正式比赛已取消' };
  }
  if (status === 'finished') {
    return { type: 'info' as const, message: '本场正式比赛已结束' };
  }
  return null;
}

export function CompetitionFormalExamSessionPanel({
  open,
  loading,
  session,
  error,
  onClose,
  embedded = false,
}: {
  open: boolean;
  loading: boolean;
  session: CompetitionFormalExamSession | null;
  error?: string | null;
  onClose: () => void;
  embedded?: boolean;
}) {
  const [keyword, setKeyword] = useState('');
  const [paperSnapshotId, setPaperSnapshotId] = useState<string>();
  const [status, setStatus] = useState<string>();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [detailId, setDetailId] = useState<CompetitionId | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [forceTarget, setForceTarget] = useState<CompetitionFormalCandidateExam | null>(null);
  const [forceReason, setForceReason] = useState('');
  const [evictTarget, setEvictTarget] = useState<CompetitionFormalCandidateExam | null>(null);
  const [realtimeState, setRealtimeState] = useState<'idle' | 'connected' | 'disconnected'>('idle');
  const queryClient = useQueryClient();
  const realtimeRef = useRef<FormalExamConsoleRealtimeHandle | null>(null);
  const applyStateRef = useRef({ seen: new Set<string>(), lastSeq: null as number | null });
  const pageIdsRef = useRef<string[]>([]);
  const scopeRef = useRef({
    sessionId: session?.id as CompetitionId | undefined,
    keyword,
    paperSnapshotId,
    status,
    page,
    pageSize,
  });
  const canForceSubmit = useAuthStore((state) =>
    state.permissionCodes.includes('competition:exam-session:force-submit'));
  const canForceOffline = useAuthStore((state) =>
    state.permissionCodes.includes('competition:exam-session:force-offline'));
  const canScore = useAuthStore((state) =>
    state.permissionCodes.includes('competition:exam-session:score'));
  const pollMs = useMemo(
    () => formalExamConsolePollIntervalMs(realtimeState === 'connected'),
    [realtimeState],
  );

  const liveSessionQuery = useQuery({
    queryKey: formalExamConsoleSessionQueryKey(session?.id),
    queryFn: () => getCompetitionFormalExamSession(session!.id),
    enabled: open && Boolean(session?.id),
    initialData: session ?? undefined,
    staleTime: 4000,
    refetchInterval: pollMs,
    refetchIntervalInBackground: false,
  });
  const currentSession = liveSessionQuery.data ?? session;
  const preflightQuery = useQuery({
    queryKey: ['competition-formal-exam-session-preflight', session?.id],
    queryFn: () => getCompetitionFormalExamSessionPreflight(session!.id),
    enabled: open && Boolean(session?.id) && (liveSessionQuery.data?.status ?? session?.status) === 'ready',
  });
  const candidatesQuery = useQuery({
    queryKey: formalExamConsoleCandidatesQueryKey(
      session?.id,
      keyword,
      paperSnapshotId,
      status,
      page,
      pageSize,
    ),
    queryFn: () => pageCompetitionFormalCandidateExams(session!.id, {
      page,
      pageSize,
      ...(keyword ? { keyword } : {}),
      ...(paperSnapshotId ? { paperSnapshotId } : {}),
      ...(status ? { status } : {}),
    }),
    enabled: open && Boolean(session?.id),
    refetchInterval: pollMs,
    refetchIntervalInBackground: false,
  });
  const detailQuery = useQuery({
    queryKey: ['competition-formal-candidate-exam', detailId],
    queryFn: () => getCompetitionFormalCandidateExam(detailId!),
    enabled: open && Boolean(detailId),
  });
  const resultQuery = useQuery({
    queryKey: ['competition-formal-candidate-result', detailId],
    queryFn: () => getCompetitionFormalCandidateResult(detailId!),
    enabled: open && Boolean(detailId)
      && currentSession?.status === 'finished'
      && (currentSession.scoringStatus === 'completed' || currentSession.scoringStatus === 'failed'),
    retry: false,
  });
  const startMutation = useMutation({
    mutationFn: () => startCompetitionFormalExamSession(String(currentSession!.id), {
      confirmWaitingCandidates: (currentSession?.waitingEnterCount ?? 0) > 0,
    }),
    onSuccess: async (data) => {
      setConfirmOpen(false);
      queryClient.setQueryData(['competition-formal-exam-session', session?.id], data);
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session', session?.id],
      });
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session-candidates', session?.id],
      });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '开始比赛失败');
    },
  });
  const forceSubmitMutation = useMutation({
    mutationFn: () => forceSubmitCompetitionFormalCandidateExam(
      String(currentSession!.id),
      String(forceTarget!.id),
      { reason: forceReason.trim() },
    ),
    onSuccess: async () => {
      message.success('已强制交卷');
      setForceTarget(null);
      setForceReason('');
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session', session?.id],
      });
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session-candidates', session?.id],
      });
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-candidate-exam'],
      });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '强制交卷失败');
    },
  });
  const forceOfflineMutation = useMutation({
    mutationFn: () => forceOfflineCompetitionFormalCandidateExam(
      String(currentSession!.id),
      String(evictTarget!.id),
    ),
    onSuccess: async () => {
      message.success('已清退该选手考试账号，平板将回到登录页');
      setEvictTarget(null);
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session', session?.id],
      });
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session-candidates', session?.id],
      });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '清退失败');
    },
  });
  const scoreMutation = useMutation({
    mutationFn: () => scoreCompetitionFormalExamSession(String(currentSession!.id)),
    onSuccess: async () => {
      message.success('评分任务已执行');
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session', session?.id],
      });
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-exam-session-candidates', session?.id],
      });
      await queryClient.invalidateQueries({
        queryKey: ['competition-formal-candidate-result'],
      });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '评分失败');
    },
  });

  pageIdsRef.current = (candidatesQuery.data?.items || []).map((row) => String(row.id));
  scopeRef.current = {
    sessionId: session?.id,
    keyword,
    paperSnapshotId,
    status,
    page,
    pageSize,
  };

  useEffect(() => {
    if (!open || !session?.id) {
      realtimeRef.current?.stop();
      realtimeRef.current = null;
      setRealtimeState('idle');
      return undefined;
    }
    const sessionId = session.id;
    applyStateRef.current = { seen: new Set<string>(), lastSeq: null };
    const handle = startFormalExamConsoleRealtime({
      examSessionId: String(sessionId),
      isActive: () => true,
      getPageCandidateIds: () => pageIdsRef.current,
      fetchCatchUp: (afterSeq) => fetchFormalExamConsoleRealtimeEvents(sessionId, { afterSeq, limit: 200 }),
      onConnected: () => setRealtimeState('connected'),
      onDisconnected: () => setRealtimeState('disconnected'),
      onPageRefresh: () => {
        applyStateRef.current.lastSeq = null;
        void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
        void queryClient.invalidateQueries({
          queryKey: ['competition-formal-exam-session-candidates', sessionId],
        });
      },
      onOutOfOrder: () => {
        void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
        void queryClient.invalidateQueries({
          queryKey: ['competition-formal-exam-session-candidates', sessionId],
        });
      },
      onSequenceCollision: () => {
        void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
        void queryClient.invalidateQueries({
          queryKey: ['competition-formal-exam-session-candidates', sessionId],
        });
      },
      onEnvelope: (envelope) => {
        const scope = scopeRef.current;
        const sessionKey = formalExamConsoleSessionQueryKey(scope.sessionId);
        const candidatesKey = formalExamConsoleCandidatesQueryKey(
          scope.sessionId,
          scope.keyword,
          scope.paperSnapshotId,
          scope.status,
          scope.page,
          scope.pageSize,
        );
        const result = applyFormalExamConsoleEnvelope({
          envelope,
          seen: applyStateRef.current.seen,
          lastSeq: applyStateRef.current.lastSeq,
          pageCandidateIds: pageIdsRef.current,
          session: queryClient.getQueryData<CompetitionFormalExamSession>(sessionKey),
          page: queryClient.getQueryData<PageResponse<CompetitionFormalCandidateExam>>(candidatesKey),
        });
        applyStateRef.current.lastSeq = result.lastSeq;
        if (result.invalidate) {
          void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
          void queryClient.invalidateQueries({
            queryKey: ['competition-formal-exam-session-candidates', sessionId],
          });
          return;
        }
        if (result.session) {
          queryClient.setQueryData(sessionKey, result.session);
        }
        if (result.page) {
          queryClient.setQueryData(candidatesKey, result.page);
        }
      },
    });
    realtimeRef.current = handle;
    return () => {
      handle.stop();
      realtimeRef.current = null;
    };
  }, [open, queryClient, session?.id]);

  useEffect(() => {
    realtimeRef.current?.subscribePage(pageIdsRef.current);
  }, [candidatesQuery.data]);

  const sessionStatus = sessionStatusMeta[currentSession?.status || ''] || {
    label: currentSession?.status || '-',
    color: 'default',
  };
  const banner = sessionBanner(currentSession?.status);
  const paperOptions = useMemo(
    () => (currentSession?.papers || []).map((paper) => ({
      value: String(paper.id),
      label: paperLabel(paper.paperCode, paper.paperName),
    })),
    [currentSession?.papers],
  );
  const failedChecks = (preflightQuery.data?.checks || []).filter((item) => !item.passed);

  const resetPanelState = () => {
    setKeyword('');
    setPaperSnapshotId(undefined);
    setStatus(undefined);
    setPage(1);
    setDetailId(null);
    setConfirmOpen(false);
    setForceTarget(null);
    setForceReason('');
  };

  const content = (
    <>
      {loading ? <Spin /> : null}
      {error ? <Alert type="error" showIcon message={error} /> : null}
      {!loading && !error && !session ? (
        <Empty description="尚未生成正式比赛运行实例" />
      ) : null}
      {session && currentSession && (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {banner ? <Alert type={banner.type} showIcon message={banner.message} /> : null}
          {realtimeState === 'disconnected' ? (
            <Alert type="warning" showIcon message={FORMAL_EXAM_CONSOLE_DISCONNECT_BANNER} />
          ) : null}
          {currentSession.status === 'running' && (currentSession.finishBlockers?.length ?? 0) > 0 ? (
            <Alert
              type="error"
              showIcon
              message="场次结束受阻"
              description={
                <ul className="competition-formal-exam-session-panel__fail-list">
                  {currentSession.finishBlockers?.map((item) => (
                    <li key={item.code}>{item.message}</li>
                  ))}
                </ul>
              }
            />
          ) : null}
          {currentSession.status === 'running'
            && currentSession.finishEligible
            && !(currentSession.finishBlockers?.length) ? (
            <Alert type="info" showIcon message="考试已截止，系统正在归档并结束本场比赛" />
          ) : null}
          {currentSession.status === 'ready' && preflightQuery.data && !preflightQuery.data.ready ? (
            <Alert
              type="error"
              showIcon
              message="开考前检查未通过"
              description={
                <ul className="competition-formal-exam-session-panel__fail-list">
                  {failedChecks.map((item) => (
                    <li key={item.code}>{item.message}</li>
                  ))}
                </ul>
              }
            />
          ) : null}

          <div className="competition-formal-exam-session-panel__hero">
            <div>
              <Typography.Title level={4}>理论考试 · 正式比赛</Typography.Title>
              <Typography.Text type="secondary">
                {currentSession.eventName || '-'} / {currentSession.stageName || '-'}
              </Typography.Text>
              {currentSession.status === 'running' || currentSession.status === 'finished' ? (
                <div className="competition-formal-exam-session-panel__times">
                  <span>实际开始：{formatClockTime(currentSession.actualStartTime)}</span>
                  {currentSession.status === 'finished' ? (
                    <span>实际结束：{formatClockTime(currentSession.actualEndTime)}</span>
                  ) : (
                    <span>预计截止：{formatClockTime(currentSession.deadlineTime)}</span>
                  )}
                </div>
              ) : null}
            </div>
            <div className="competition-formal-exam-session-panel__hero-actions">
              <Tag color={sessionStatus.color}>{sessionStatus.label}</Tag>
              {currentSession.status === 'finished' ? (
                <Tag color={(scoringStatusMeta[currentSession.scoringStatus || 'not_started'] || scoringStatusMeta.not_started).color}>
                  {(scoringStatusMeta[currentSession.scoringStatus || 'not_started'] || scoringStatusMeta.not_started).label}
                </Tag>
              ) : null}
              {currentSession.status === 'finished'
                && canScore
                && (currentSession.scoringStatus === 'failed' || currentSession.scoringStatus === 'not_started') ? (
                <Button
                  autoInsertSpace={false}
                  loading={scoreMutation.isPending}
                  onClick={() => scoreMutation.mutate()}
                >
                  重新执行评分
                </Button>
              ) : null}
              {currentSession.status === 'ready' && preflightQuery.data?.ready ? (
                <Tooltip
                  title={(currentSession.readyCount ?? 0) === 0 ? '当前没有已进入候考的选手' : ''}
                >
                  <span>
                    <Button
                      type="primary"
                      autoInsertSpace={false}
                      disabled={(currentSession.readyCount ?? 0) === 0 || startMutation.isPending}
                      loading={startMutation.isPending}
                      onClick={() => setConfirmOpen(true)}
                    >
                      开始比赛
                    </Button>
                  </span>
                </Tooltip>
              ) : null}
            </div>
          </div>

          <Descriptions size="small" column={3} bordered>
            <Descriptions.Item label="赛事">{currentSession.eventName || '-'}</Descriptions.Item>
            <Descriptions.Item label="赛段">{currentSession.stageName || '-'}</Descriptions.Item>
            <Descriptions.Item label="工种">{currentSession.workTypeName || '-'}</Descriptions.Item>
            <Descriptions.Item label="技能等级">
              {currentSession.levelName ? `${currentSession.levelName}（${currentSession.levelCode || '-'}）` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="考试类型">{examTypeLabel(currentSession.examType)}</Descriptions.Item>
            <Descriptions.Item label="ExamSession ID">
              <Typography.Text copyable={{ text: String(currentSession.id) }}>{String(currentSession.id)}</Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="平板端地址" span={3}>
              <Typography.Text
                copyable={{ text: 'http://192.168.137.1:8080/formal-exam' }}
              >
                http://192.168.137.1:8080/formal-exam
              </Typography.Text>
              <div>
                <Typography.Text type="secondary">
                  平板 App 打开固定入口即可，不必随竞赛更换 sessionId。选手登录后进入当前可参加的理论考试。请连本机热点，不要用 localhost。
                </Typography.Text>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="比赛状态">
              <Tag color={sessionStatus.color}>{sessionStatus.label}</Tag>
            </Descriptions.Item>
            {currentSession.status === 'finished' ? (
              <Descriptions.Item label="评分状态">
                <Tag color={(scoringStatusMeta[currentSession.scoringStatus || 'not_started'] || scoringStatusMeta.not_started).color}>
                  {(scoringStatusMeta[currentSession.scoringStatus || 'not_started'] || scoringStatusMeta.not_started).label}
                </Tag>
              </Descriptions.Item>
            ) : null}
            <Descriptions.Item label="考试时长">{durationLabel(currentSession.durationSeconds)}</Descriptions.Item>
            <Descriptions.Item label="计划开始时间">{formatDateTime(currentSession.plannedStartTime)}</Descriptions.Item>
            <Descriptions.Item label="实际开始时间">{formatDateTime(currentSession.actualStartTime)}</Descriptions.Item>
            <Descriptions.Item label="实际结束时间">{formatDateTime(currentSession.actualEndTime)}</Descriptions.Item>
            <Descriptions.Item label="选手总人数">{currentSession.candidateTotal ?? 0}</Descriptions.Item>
            <Descriptions.Item label="试卷套数">{currentSession.paperCount ?? 0}</Descriptions.Item>
          </Descriptions>

          <div className="competition-formal-exam-session-panel__stats">
            <Card size="small" className="competition-formal-exam-session-panel__stat-waiting">
              <Statistic title="待进入" value={currentSession.waitingEnterCount ?? 0} suffix="人" />
            </Card>
            <Card size="small" className="competition-formal-exam-session-panel__stat-ready">
              <Statistic title="已候考" value={currentSession.readyCount ?? 0} suffix="人" />
            </Card>
            <Card size="small"><Statistic title="考试中" value={currentSession.runningCount ?? 0} suffix="人" /></Card>
            <Card size="small"><Statistic title="已交卷" value={currentSession.submittedCount ?? 0} suffix="人" /></Card>
            <Card size="small"><Statistic title="自动交卷" value={currentSession.autoSubmittedCount ?? 0} suffix="人" /></Card>
            <Card size="small"><Statistic title="强制交卷" value={currentSession.forcedSubmittedCount ?? 0} suffix="人" /></Card>
            <Card size="small"><Statistic title="缺考" value={currentSession.absentCount ?? 0} suffix="人" /></Card>
            {currentSession.status === 'finished' ? (
              <>
                <Card size="small"><Statistic title="已评分" value={currentSession.scoredCount ?? 0} suffix="人" /></Card>
                <Card size="small"><Statistic title="缺考结果" value={currentSession.scoringAbsentCount ?? 0} suffix="人" /></Card>
                <Card size="small"><Statistic title="评分失败" value={currentSession.scoringFailedCount ?? 0} suffix="人" /></Card>
              </>
            ) : null}
          </div>
          {currentSession.status === 'finished' && currentSession.scoringStatus === 'failed' && currentSession.scoringError ? (
            <Alert type="error" showIcon message={currentSession.scoringError} />
          ) : null}

          {currentSession.status === 'ready' ? (
          <Card size="small" title="开考前检查" loading={preflightQuery.isFetching}>
            {preflightQuery.isError ? (
              <Alert type="error" showIcon message="开考前检查加载失败" />
            ) : (
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                {(preflightQuery.data?.checks || []).map((item) => (
                  <div key={item.code} className="competition-formal-exam-session-panel__check">
                    {item.passed
                      ? <CheckCircleOutlined className="competition-formal-exam-session-panel__check-ok" />
                      : <CloseCircleOutlined className="competition-formal-exam-session-panel__check-fail" />}
                    <Typography.Text>{item.message}</Typography.Text>
                  </div>
                ))}
                {preflightQuery.data?.ready ? (
                  <Typography.Text type="success">当前状态：可以进入选手候考阶段</Typography.Text>
                ) : null}
              </Space>
            )}
          </Card>
          ) : null}

          <Card size="small" title="试卷分布">
            <div className="competition-formal-exam-session-panel__papers">
              {(currentSession.papers || []).map((paper) => (
                <Card key={String(paper.id)} size="small">
                  <Statistic
                    title={paperLabel(paper.paperCode, paper.paperName)}
                    value={paper.candidateCount}
                    suffix="人"
                  />
                </Card>
              ))}
              {!currentSession.papers?.length ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无正式试卷快照" /> : null}
            </div>
          </Card>

          <Card size="small" title="选手">
            <div className="competition-formal-exam-session-panel__toolbar">
              <Input.Search
                allowClear
                aria-label="搜索编号或姓名"
                placeholder="搜索编号或姓名"
                style={{ width: 240 }}
                onSearch={(value) => { setKeyword(value.trim()); setPage(1); }}
              />
              <Select
                allowClear
                aria-label="试卷筛选"
                placeholder="试卷筛选"
                style={{ width: 220 }}
                value={paperSnapshotId}
                options={paperOptions}
                virtual={false}
                onChange={(value) => { setPaperSnapshotId(value); setPage(1); }}
              />
              <Select
                allowClear
                aria-label="考试状态"
                placeholder="考试状态"
                style={{ width: 160 }}
                value={status}
                options={candidateStatusOptions}
                virtual={false}
                onChange={(value) => { setStatus(value); setPage(1); }}
              />
            </div>
            <Table<CompetitionFormalCandidateExam>
              rowKey={(row) => String(row.id)}
              size="small"
              loading={candidatesQuery.isFetching}
              dataSource={candidatesQuery.data?.items || []}
              locale={{ emptyText: '尚未生成选手考试实例' }}
              pagination={{
                current: page,
                pageSize,
                total: candidatesQuery.data?.total || 0,
                showSizeChanger: false,
                onChange: (nextPage, nextSize) => {
                  setPage(nextPage);
                  setPageSize(nextSize);
                },
              }}
              columns={[
                { title: '编号', dataIndex: 'candidateNo', width: 110, render: (value) => value || '-' },
                { title: '姓名', dataIndex: 'candidateName', width: 110, render: (value) => value || '-' },
                {
                  title: '试卷',
                  width: 160,
                  render: (_, row) => paperLabel(row.paperCode, row.paperName),
                },
                {
                  title: '状态',
                  dataIndex: 'status',
                  width: 180,
                  render: (value: string, row: CompetitionFormalCandidateExam) => {
                    const meta = candidateStatusMeta[value] || { label: value, color: 'default' };
                    const state = row.connectionState || resolveFormalExamConnectionState({
                      candidateStatus: row.status,
                      accessSessionStatus: row.accessSessionStatus,
                      lastSeenAt: row.lastSeenAt,
                    });
                    const connection = FORMAL_EXAM_CONNECTION_LABELS[state] || { label: state, color: 'default' };
                    return (
                      <Space size={4} wrap>
                        <Tag color={meta.color}>{meta.label}</Tag>
                        <Tag color={connection.color}>{connection.label}</Tag>
                      </Space>
                    );
                  },
                },
                { title: '进入时间', dataIndex: 'enterTime', width: 160, render: formatDateTime },
                { title: '开始时间', dataIndex: 'startTime', width: 160, render: formatDateTime },
                { title: '交卷时间', dataIndex: 'submitTime', width: 160, render: formatDateTime },
                {
                  title: '缺考确认时间',
                  dataIndex: 'absentTime',
                  width: 160,
                  render: formatDateTime,
                },
                {
                  title: '答题进度',
                  width: 110,
                  render: (_, row) => `${row.answeredCount ?? 0} / ${row.totalQuestionCount ?? 0}`,
                },
                {
                  title: '成绩',
                  width: 130,
                  render: (_: unknown, row: CompetitionFormalCandidateExam) => (
                    currentSession.status === 'finished' ? candidateScoreText(row) : '--'
                  ),
                },
                {
                  title: '操作',
                  width: 220,
                  render: (_, row) => (
                    <Space size={0}>
                      <Button type="link" size="small" onClick={() => setDetailId(row.id)}>查看</Button>
                      {canForceOffline ? (
                        <Button
                          type="link"
                          size="small"
                          onClick={() => setEvictTarget(row)}
                        >
                          清退
                        </Button>
                      ) : null}
                      {canForceSubmit && currentSession.status === 'running' && row.status === 'running' ? (
                        <Button
                          type="link"
                          size="small"
                          danger
                          onClick={() => {
                            setForceReason('');
                            setForceTarget(row);
                          }}
                        >
                          强制交卷
                        </Button>
                      ) : null}
                    </Space>
                  ),
                },
              ]}
            />
          </Card>
        </Space>
      )}

      <Modal
        title="开始比赛"
        open={confirmOpen}
        okText="确认开始"
        cancelText="取消"
        confirmLoading={startMutation.isPending}
        onCancel={() => setConfirmOpen(false)}
        onOk={() => startMutation.mutate()}
        destroyOnHidden
        transitionName=""
        maskTransitionName=""
      >
        {(currentSession?.waitingEnterCount ?? 0) > 0 ? (
          <Space direction="vertical" size={8}>
            <Typography.Paragraph style={{ marginBottom: 0 }}>
              当前仍有 {currentSession?.waitingEnterCount} 名选手未进入候考。
            </Typography.Paragraph>
            <Typography.Paragraph style={{ marginBottom: 0 }}>
              已候考：{currentSession?.readyCount ?? 0} 人；未进入：{currentSession?.waitingEnterCount ?? 0} 人。
            </Typography.Paragraph>
            <Typography.Paragraph style={{ marginBottom: 0 }}>
              开始比赛后，这些选手将无法通过普通候考入口自行进入本场考试。
            </Typography.Paragraph>
            <Typography.Paragraph style={{ marginBottom: 0 }}>是否确认开始比赛？</Typography.Paragraph>
          </Space>
        ) : (
          <Space direction="vertical" size={8}>
            <Typography.Paragraph style={{ marginBottom: 0 }}>
              本场共有 {currentSession?.readyCount ?? 0} 名选手已进入候考。
            </Typography.Paragraph>
            <Typography.Paragraph style={{ marginBottom: 0 }}>确认开始理论考试？</Typography.Paragraph>
            <Typography.Paragraph style={{ marginBottom: 0 }}>开始后将以服务器时间统一计时。</Typography.Paragraph>
          </Space>
        )}
      </Modal>

      <Modal
        title="强制交卷"
        open={Boolean(forceTarget)}
        okText="确认强制交卷"
        cancelText="取消"
        okButtonProps={{ disabled: forceReason.trim().length < 1 || forceReason.trim().length > 500 }}
        confirmLoading={forceSubmitMutation.isPending}
        onCancel={() => {
          setForceTarget(null);
          setForceReason('');
        }}
        onOk={() => {
          if (forceReason.trim().length < 1 || forceReason.trim().length > 500) return;
          forceSubmitMutation.mutate();
        }}
        destroyOnHidden
        transitionName=""
        maskTransitionName=""
      >
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            姓名：{forceTarget?.candidateName || '-'}
          </Typography.Paragraph>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            选手编号：{forceTarget?.candidateNo || '-'}
          </Typography.Paragraph>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            当前答题进度：{forceTarget?.answeredCount ?? 0} / {forceTarget?.totalQuestionCount ?? 0}
          </Typography.Paragraph>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            考试状态：{candidateStatusMeta[forceTarget?.status || '']?.label || forceTarget?.status || '-'}
          </Typography.Paragraph>
          <Alert
            type="warning"
            showIcon
            message="强制交卷将立即锁定服务器已经接收的答案。如果该选手设备当前存在尚未同步的本地答案，这些答案不会自动计入正式试卷。确认继续吗？"
          />
          <Typography.Paragraph style={{ marginBottom: 0 }}>强制交卷原因（必填，1～500 字）</Typography.Paragraph>
          <Input.TextArea
            aria-label="强制交卷原因"
            value={forceReason}
            maxLength={500}
            rows={4}
            placeholder="例如：设备违规操作 / 选手申请提前结束 / 现场异常处理"
            onChange={(event) => setForceReason(event.target.value)}
          />
        </Space>
      </Modal>

      <Modal
        title="清退考试账号"
        open={Boolean(evictTarget)}
        okText="确认清退"
        cancelText="取消"
        confirmLoading={forceOfflineMutation.isPending}
        onCancel={() => setEvictTarget(null)}
        onOk={() => forceOfflineMutation.mutate()}
        destroyOnHidden
        transitionName=""
        maskTransitionName=""
      >
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            姓名：{evictTarget?.candidateName || '-'}
          </Typography.Paragraph>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            选手编号：{evictTarget?.candidateNo || '-'}
          </Typography.Paragraph>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            考试状态：{candidateStatusMeta[evictTarget?.status || '']?.label || evictTarget?.status || '-'}
          </Typography.Paragraph>
          <Alert
            type="info"
            showIcon
            message="清退只会强制该选手考试账号下线，平板将回到登录页。不会交卷，也不会作废已答题目。下一场选手可在同一平板直接登录，不必更换考试地址。"
          />
        </Space>
      </Modal>

      <Drawer
        title="选手正式考试"
        open={Boolean(detailId)}
        width={560}
        onClose={() => setDetailId(null)}
        destroyOnHidden
        getContainer={false}
      >
        {detailQuery.isFetching ? <Spin /> : null}
        {detailQuery.isError ? <Alert type="error" showIcon message="选手考试实例加载失败" /> : null}
        {detailQuery.data ? (
          <Descriptions size="small" column={1} bordered>
            <Descriptions.Item label="姓名">{detailQuery.data.candidateName || '-'}</Descriptions.Item>
            <Descriptions.Item label="编号">{detailQuery.data.candidateNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="contestantProfileId">
              <Typography.Text copyable={{ text: String(detailQuery.data.contestantProfileId) }}>
                {String(detailQuery.data.contestantProfileId)}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="CandidateExam ID">
              <Typography.Text copyable={{ text: String(detailQuery.data.id) }}>
                {String(detailQuery.data.id)}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {candidateStatusMeta[detailQuery.data.status]?.label || detailQuery.data.status}
            </Descriptions.Item>
            <Descriptions.Item label="正式试卷名称">{detailQuery.data.paperName || '-'}</Descriptions.Item>
            <Descriptions.Item label="正式试卷代码">{detailQuery.data.paperCode || '-'}</Descriptions.Item>
            <Descriptions.Item label="paperSnapshotId">
              <Typography.Text copyable={{ text: String(detailQuery.data.paperSnapshotId) }}>
                {String(detailQuery.data.paperSnapshotId)}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="sourcePaperId">
              <Typography.Text copyable={{ text: String(detailQuery.data.sourcePaperId) }}>
                {String(detailQuery.data.sourcePaperId)}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="题目数量">{detailQuery.data.totalQuestionCount ?? 0}</Descriptions.Item>
            <Descriptions.Item label="进入时间">{formatDateTime(detailQuery.data.enterTime)}</Descriptions.Item>
            <Descriptions.Item label="开始时间">{formatDateTime(detailQuery.data.startTime)}</Descriptions.Item>
            <Descriptions.Item label="交卷时间">{formatDateTime(detailQuery.data.submitTime)}</Descriptions.Item>
            {detailQuery.data.status === 'absent' ? (
              <>
                <Descriptions.Item label="缺考确认时间">
                  {formatDateTime(detailQuery.data.absentTime)}
                </Descriptions.Item>
                <Descriptions.Item label="缺考原因">
                  {detailQuery.data.absentReason || '-'}
                </Descriptions.Item>
              </>
            ) : null}
            <Descriptions.Item label="交卷类型">
              {detailQuery.data.submitType
                ? (candidateStatusMeta[detailQuery.data.submitType]?.label || detailQuery.data.submitType)
                : '-'}
            </Descriptions.Item>
            {detailQuery.data.status === 'forced_submitted' ? (
              <>
                <Descriptions.Item label="强制交卷时间">
                  {formatDateTime(detailQuery.data.submitTime)}
                </Descriptions.Item>
                <Descriptions.Item label="操作人员">
                  {detailQuery.data.forceSubmittedBy == null
                    ? '-'
                    : String(detailQuery.data.forceSubmittedBy)}
                </Descriptions.Item>
                <Descriptions.Item label="强制交卷原因">
                  {detailQuery.data.forceSubmitReason || '-'}
                </Descriptions.Item>
              </>
            ) : null}
            <Descriptions.Item label="最后活动时间">{formatDateTime(detailQuery.data.lastActiveTime)}</Descriptions.Item>
          </Descriptions>
        ) : null}
        {detailQuery.data ? (
          <div style={{ marginTop: 16 }}>
            <Typography.Title level={5}>正式成绩</Typography.Title>
            {detailQuery.data.status === 'absent' || resultQuery.data?.resultStatus === 'absent' ? (
              <Descriptions size="small" column={1} bordered>
                <Descriptions.Item label="结果">缺考</Descriptions.Item>
                <Descriptions.Item label="成绩">--</Descriptions.Item>
              </Descriptions>
            ) : resultQuery.data?.resultStatus === 'scored' ? (
              <>
                <Descriptions size="small" column={1} bordered>
                  <Descriptions.Item label="总分">{formatScore(resultQuery.data.totalScore)}</Descriptions.Item>
                  <Descriptions.Item label="满分">{formatScore(resultQuery.data.fullScore)}</Descriptions.Item>
                  <Descriptions.Item label="题目数">{resultQuery.data.questionCount ?? '--'}</Descriptions.Item>
                  <Descriptions.Item label="已答">{resultQuery.data.answeredCount ?? '--'}</Descriptions.Item>
                  <Descriptions.Item label="未答">{resultQuery.data.unansweredCount ?? '--'}</Descriptions.Item>
                  <Descriptions.Item label="正确">{resultQuery.data.correctCount ?? '--'}</Descriptions.Item>
                  <Descriptions.Item label="错误">{resultQuery.data.wrongCount ?? '--'}</Descriptions.Item>
                  <Descriptions.Item label="评分时间">{formatDateTime(resultQuery.data.scoredTime)}</Descriptions.Item>
                  <Descriptions.Item label="评分版本">{resultQuery.data.scoreVersion ?? '--'}</Descriptions.Item>
                </Descriptions>
                {(resultQuery.data.questionScores || []).length > 0 ? (
                  <Table
                    style={{ marginTop: 12 }}
                    size="small"
                    rowKey={(row) => String(row.questionSnapshotId)}
                    pagination={false}
                    dataSource={resultQuery.data.questionScores}
                    columns={[
                      { title: '题号', dataIndex: 'questionOrder', width: 60 },
                      {
                        title: '题型',
                        dataIndex: 'questionType',
                        width: 70,
                        render: (value: string) => questionTypeLabel[value] || value,
                      },
                      {
                        title: '作答',
                        width: 70,
                        render: (_, row) => (row.answered ? '已答' : '未答'),
                      },
                      {
                        title: '对错',
                        width: 70,
                        render: (_, row) => (row.answered ? (row.correct ? '正确' : '错误') : '--'),
                      },
                      {
                        title: '得分',
                        width: 90,
                        render: (_, row) => `${formatScore(row.awardedScore)} / ${formatScore(row.maxScore)}`,
                      },
                    ]}
                  />
                ) : null}
              </>
            ) : (
              <Typography.Text type="secondary">正式成绩尚未生成</Typography.Text>
            )}
          </div>
        ) : null}
      </Drawer>
    </>
  );

  if (embedded) {
    return <div className="competition-formal-exam-session-panel competition-formal-exam-session-panel--embedded">{content}</div>;
  }

  return (
    <Modal
      title="理论考试 · 正式比赛控制台"
      open={open}
      onCancel={() => {
        setDetailId(null);
        onClose();
      }}
      footer={null}
      width={1120}
      destroyOnHidden
      transitionName=""
      maskTransitionName=""
      className="competition-formal-exam-session-panel"
      afterOpenChange={(visible) => {
        if (visible) return;
        resetPanelState();
      }}
    >
      {content}
    </Modal>
  );
}

import { PlayCircleOutlined, ReloadOutlined, SettingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  Modal,
  Progress,
  Result,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState, type Key } from 'react';
import {
  getCompetitionPracticalConsole,
  createCompetitionPracticalRealtimeTicket,
  initializeCompetitionPracticalConsole,
  listCompetitionEventWorkTypeLevels,
  pageCompetitionStages,
  returnCompetitionPracticalScoreForRescore,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionId,
  CompetitionPracticalConsoleQuestion,
  CompetitionPracticalConsoleTask,
  CompetitionStage,
} from '../../types/competition';
import './practical-console-expand.css';

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  if (error && typeof error === 'object' && 'response' in error) {
    const msg = (error as { response?: { data?: { message?: string } } }).response?.data?.message;
    if (typeof msg === 'string' && msg.trim()) return msg;
  }
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

function formatScore(value?: number | string | null) {
  if (value === undefined || value === null || value === '') return undefined;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric.toFixed(1) : undefined;
}

function sheetStatusMeta(status?: string | null) {
  if (status === 'submitted_locked' || status === 'submitted') return { color: 'success', text: '已提交' };
  if (status === 'draft' || status === 'scoring') return { color: 'processing', text: '评分中' };
  if (status === 'unlocked_for_rescore' || status === 'returned') return { color: 'error', text: '已退回' };
  if (status === 'unopened' || !status) return { color: 'default', text: '未打开' };
  return { color: 'processing', text: status };
}

type ContestantRow = {
  key: string;
  contestantMemberId: string;
  contestantNo?: string | null;
  contestantName?: string | null;
  workstationCode?: string | null;
  submittedSheets: number;
  totalSheets: number;
  completedQuestions: number;
  totalQuestions: number;
  totalScore?: number | null;
  scoreDifferenceLimit?: number | null;
  actualScoreDifference?: number | null;
  scoreDifferenceStatus: 'waiting' | 'normal' | 'exceeded' | 'unrestricted' | string;
  status: 'completed' | 'awaiting_leader' | 'abnormal' | 'scoring';
  referees: CompetitionPracticalConsoleTask[];
};

export function groupConsoleContestants(tasks: CompetitionPracticalConsoleTask[]): ContestantRow[] {
  const groups = new Map<string, CompetitionPracticalConsoleTask[]>();
  for (const task of tasks) {
    const contestantId = idValue(task.contestantMemberId);
    const current = groups.get(contestantId) ?? [];
    current.push(task);
    groups.set(contestantId, current);
  }
  return Array.from(groups.entries()).map(([contestantMemberId, referees]) => {
    const ordered = [...referees].sort((left, right) => (
      idValue(left.refereeMemberId).localeCompare(idValue(right.refereeMemberId))
    ));
    const submittedSheets = ordered.filter((item) => item.scoreSheetStatus === 'submitted_locked').length;
    const questionCount = Math.max(0, ...ordered.map((item) => item.questions?.length ?? 0));
    const completedQuestions = questionCount
      ? Array.from({ length: questionCount }, (_, index) => index).filter((index) => (
        ordered.length > 0 && ordered.every((item) => questionSubmitted(item.questions?.[index], item.scoreSheetStatus))
      )).length
      : 0;
    const allSubmitted = ordered.length > 0 && submittedSheets === ordered.length;
    const totalScore = allSubmitted && ordered.every((item) => item.totalScore != null && item.totalScore !== '')
      ? ordered.reduce((sum, item) => sum + Number(item.totalScore), 0) / ordered.length
      : null;
    const scoreDifferenceStatus = ordered[0]?.scoreDifferenceStatus ?? 'waiting';
    const practicalScoreCompleted = ordered.some((item) => item.practicalScoreCompleted);
    const status = scoreDifferenceStatus === 'exceeded' ? 'abnormal'
      : practicalScoreCompleted && allSubmitted ? 'completed'
      : allSubmitted ? 'awaiting_leader' : 'scoring';
    return {
      key: contestantMemberId,
      contestantMemberId,
      contestantNo: ordered[0]?.contestantNo,
      contestantName: ordered[0]?.contestantName,
      workstationCode: ordered[0]?.workstationCode,
      submittedSheets,
      totalSheets: ordered.length,
      completedQuestions,
      totalQuestions: questionCount,
      totalScore,
      scoreDifferenceLimit: ordered[0]?.scoreDifferenceLimit == null
        ? null : Number(ordered[0].scoreDifferenceLimit),
      actualScoreDifference: ordered[0]?.actualScoreDifference == null
        ? null : Number(ordered[0].actualScoreDifference),
      scoreDifferenceStatus,
      status,
      referees: ordered,
    };
  });
}

export function CompetitionPracticalConsolePage({
  eventId,
  eventWorkTypeId,
  eventWorkTypeLevelCode,
  initialStageId,
}: {
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelCode: string;
  initialStageId?: string;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const token = useAuthStore((state) => state.token);
  const tenantId = useAuthStore((state) => state.tenantId);
  const canView = permissions.includes('competition:practical-console:view');
  const canManage = permissions.includes('competition:practical-console:manage');
  const queryClient = useQueryClient();
  const [selectedStageId, setSelectedStageId] = useState<string | undefined>(initialStageId);
  const [expandedRowKeys, setExpandedRowKeys] = useState<Key[]>([]);
  const [returnTask, setReturnTask] = useState<CompetitionPracticalConsoleTask | null>(null);
  const [returnReason, setReturnReason] = useState('');
  const levelsQuery = useQuery({
    queryKey: ['competition-practical-console-levels', eventId, eventWorkTypeId],
    queryFn: () => listCompetitionEventWorkTypeLevels(eventId, eventWorkTypeId),
    enabled: canView,
  });
  const levelId = idValue(levelsQuery.data?.find(
    (item) => item.levelCode === eventWorkTypeLevelCode,
  )?.id);
  const stagesQuery = useQuery({
    queryKey: ['competition-practical-console-stages', eventId, eventWorkTypeId, eventWorkTypeLevelCode],
    queryFn: () => pageCompetitionStages(eventId, {
      page: 1,
      pageSize: 500,
      eventWorkTypeId,
      eventWorkTypeLevelCode,
    }),
    enabled: canView,
  });
  const stages = stagesQuery.data?.items ?? [];
  useEffect(() => {
    if (!stages.length) return setSelectedStageId(undefined);
    if (!selectedStageId || !stages.some((item) => idValue(item.id) === selectedStageId)) {
      setSelectedStageId(initialStageId && stages.some((item) => idValue(item.id) === initialStageId)
        ? initialStageId : idValue(stages[0].id));
    }
  }, [initialStageId, selectedStageId, stages]);
  const selectedStage = useMemo(
    () => stages.find((item) => idValue(item.id) === selectedStageId),
    [selectedStageId, stages],
  );
  const consoleQuery = useQuery({
    queryKey: ['competition-practical-console', selectedStageId, levelId],
    queryFn: () => getCompetitionPracticalConsole(selectedStageId!, levelId),
    enabled: canView && Boolean(selectedStageId && levelId),
    refetchInterval: import.meta.env.MODE === 'test' || !(canView && selectedStageId && levelId)
      ? false
      : 4_000,
    staleTime: 0,
  });
  const initializeMutation = useMutation({
    mutationFn: () => initializeCompetitionPracticalConsole(selectedStageId!, levelId),
    onSuccess: (data) => {
      queryClient.setQueryData(['competition-practical-console', selectedStageId, levelId], data);
      message.success('实操正式比赛已初始化，裁判、选手和考评表已生成评分任务');
    },
    onError: (error) => message.error(errorText(error, '初始化失败，请核对选手、裁判和考评表配置')),
  });
  const returnMutation = useMutation({
    mutationFn: () => returnCompetitionPracticalScoreForRescore(
      returnTask!.assignmentId,
      returnReason.trim(),
    ),
    onSuccess: () => {
      message.success('已退回该裁判重评，分差状态将在重新提交后自动更新');
      setReturnTask(null);
      setReturnReason('');
      void queryClient.invalidateQueries({ queryKey: ['competition-practical-console'] });
    },
    onError: (error) => message.error(errorText(error, '退回重评失败，请刷新后重试')),
  });
  useEffect(() => {
    if (!token || !tenantId) return;
    if (import.meta.env.MODE === 'test') return;
    let socket: WebSocket | undefined;
    let disposed = false;
    let retryTimer: number | undefined;
    let refetchTimer: number | undefined;
    let attempt = 0;

    function connect() {
      createCompetitionPracticalRealtimeTicket().then((ticket) => {
        if (disposed) return;
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const query = new URLSearchParams({ ticket, tenantId: String(tenantId) });
        socket = new WebSocket(`${protocol}//${window.location.host}/api/ws/competition-practical?${query}`);
        socket.onmessage = () => {
          if (refetchTimer != null) window.clearTimeout(refetchTimer);
          refetchTimer = window.setTimeout(() => {
            void queryClient.refetchQueries({ queryKey: ['competition-practical-console'] });
          }, 50);
        };
        socket.onclose = () => {
          if (disposed) return;
          const delay = Math.min(8_000, 400 * 2 ** attempt);
          attempt += 1;
          retryTimer = window.setTimeout(connect, delay);
        };
      }).catch(() => {
        if (disposed) return;
        retryTimer = window.setTimeout(connect, 2_000);
      });
    }

    connect();
    return () => {
      disposed = true;
      if (retryTimer != null) window.clearTimeout(retryTimer);
      if (refetchTimer != null) window.clearTimeout(refetchTimer);
      socket?.close();
    };
  }, [queryClient, tenantId, token]);

  const contestantRows = useMemo(
    () => groupConsoleContestants(consoleQuery.data?.tasks ?? []),
    [consoleQuery.data?.tasks],
  );
  const contestantKeys = contestantRows.map((row) => row.key).join(',');
  useEffect(() => {
    const keys = contestantKeys ? contestantKeys.split(',') : [];
    setExpandedRowKeys((current) => {
      if (keys.length === 1) return keys;
      return current.filter((key) => keys.includes(String(key)));
    });
  }, [contestantKeys]);

  const submittedContestantCount = contestantRows.filter(
    (row) => row.totalSheets > 0 && row.submittedSheets === row.totalSheets,
  ).length;
  const abnormalContestants = contestantRows.filter((row) => row.status === 'abnormal');

  if (!canView) return <Result status="403" title="暂无实操比赛控制台查看权限" />;
  const state = consoleQuery.data;
  const contestantColumns: ColumnsType<ContestantRow> = [
    { title: '工位', dataIndex: 'workstationCode', width: 90, render: (value) => value || '-' },
    { title: '选手编号', dataIndex: 'contestantNo', width: 160, render: (value, row) => value || row.contestantMemberId },
    { title: '选手', dataIndex: 'contestantName', width: 140, render: (value, row) => value || row.contestantMemberId },
    {
      title: '评分提交',
      width: 180,
      render: (_, row) => (
        <Progress
          percent={row.totalSheets ? Math.round(row.submittedSheets * 100 / row.totalSheets) : 0}
          format={() => `${row.submittedSheets}/${row.totalSheets}`}
        />
      ),
    },
    {
      title: '试题完成',
      width: 120,
      render: (_, row) => (row.totalQuestions ? `${row.completedQuestions}/${row.totalQuestions}` : '-'),
    },
    {
      title: '实操总分',
      width: 130,
      render: (_, row) => formatScore(row.totalScore) ?? '待三人齐交',
    },
    {
      title: '分差预警',
      width: 170,
      render: (_, row) => {
        if (row.scoreDifferenceStatus === 'exceeded') {
          return <Tag color="error">分差 {formatScore(row.actualScoreDifference)} ＞ {formatScore(row.scoreDifferenceLimit)}</Tag>;
        }
        if (row.scoreDifferenceStatus === 'normal') {
          return <Tag color="success">分差 {formatScore(row.actualScoreDifference)} / {formatScore(row.scoreDifferenceLimit)}</Tag>;
        }
        if (row.scoreDifferenceStatus === 'unrestricted') return <Tag>未限制</Tag>;
        return <Tag>待至少 2 人提交</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: ContestantRow['status']) => (
        <Tag color={value === 'completed' ? 'success' : value === 'abnormal' ? 'error' : 'processing'}>
          {value === 'completed' ? '已完成'
            : value === 'abnormal' ? '分差异常'
            : value === 'awaiting_leader' ? '待组长确认' : '评分中'}
        </Tag>
      ),
    },
  ];
  return <Space direction="vertical" size={16} style={{ width: '100%', marginTop: 16 }}>
    <Card size="small" bordered={false}>
      <Space wrap>
        <SettingOutlined />
        <Typography.Text type="secondary">选择赛段</Typography.Text>
        <Select
          value={selectedStageId}
          style={{ width: 320 }}
          loading={stagesQuery.isFetching}
          placeholder="请选择需要控制的实操赛段"
          options={stages.map((stage: CompetitionStage) => ({ value: idValue(stage.id), label: stage.name }))}
          onChange={setSelectedStageId}
        />
        <Button icon={<ReloadOutlined />} onClick={() => { void stagesQuery.refetch(); void consoleQuery.refetch(); }}>刷新</Button>
        {state && <Tag color={state.scoringTaskCount > 0 ? 'success' : 'warning'}>
          {state.scoringTaskCount > 0 ? '已初始化' : '未初始化'}
        </Tag>}
      </Space>
    </Card>
    {!selectedStage ? <Card><Empty description="当前工种等级下暂无赛段" /></Card> : null}
    {consoleQuery.isError ? <Alert type="error" showIcon message={errorText(consoleQuery.error, '控制台状态加载失败')} /> : null}
    {selectedStage && state ? <Card
      title={<Space><span>{selectedStage.name} · 实操评分任务</span><Tag>{state.status}</Tag></Space>}
      extra={state.scoringTaskCount === 0 && canManage ? <Button
        type="primary"
        icon={<PlayCircleOutlined />}
        loading={initializeMutation.isPending}
        onClick={() => initializeMutation.mutate()}
      >初始化正式比赛</Button> : null}
    >
      {state.status === 'not_initialized' ? <Alert
        type="warning"
        showIcon
        message="当前实操比赛尚未初始化"
        description="点击“初始化正式比赛”后，系统会校验已确认的赛段编排、选手试卷与考评表、三名裁判配置，并自动准备比赛运行数据。"
      /> : null}
      <Space size={36} wrap style={{ marginTop: 16 }}>
        <Statistic title="比赛场次" value={state.sessionCount} />
        <Statistic title="参赛选手" value={state.contestantCount} />
        <Statistic title="评分任务" value={state.scoringTaskCount} />
        <Statistic title="已打开评分单" value={state.openedSheetCount} />
        <Statistic title="已提交评分单" value={state.submittedSheetCount} />
      </Space>
      {state.scoringTaskCount > 0 ? <Alert
        style={{ marginTop: 20 }}
        type="success"
        showIcon
        message="评分任务已冻结"
        description="每条任务均保存裁判、选手、考评表的明确关联。裁判端提交后，控制台统计会即时刷新。"
      /> : null}
      {abnormalContestants.length > 0 ? <Alert
        style={{ marginTop: 16 }}
        type="error"
        showIcon
        message={`${abnormalContestants.length} 名选手存在裁判评分分差异常`}
        description="请展开异常选手，核对各裁判原始分，并将需要复核的裁判评分退回重评。分差恢复到题目允许范围内之前，组长无法完成考试。"
      /> : null}
      {state.scoringTaskCount > 0 ? <>
        <Space size={24} style={{ margin: '20px 0 16px' }}>
          <Progress
            type="circle"
            size={92}
            percent={contestantRows.length
              ? Math.round(submittedContestantCount * 100 / contestantRows.length)
              : 0}
          />
          <Typography.Text>
            选手已提交 {submittedContestantCount} / {contestantRows.length} 人
          </Typography.Text>
        </Space>
        <Table<ContestantRow>
          rowKey="key"
          size="middle"
          pagination={contestantRows.length > 20 ? { pageSize: 20 } : false}
          dataSource={contestantRows}
          columns={contestantColumns}
          scroll={{ x: 1090 }}
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: (keys) => setExpandedRowKeys([...keys]),
            expandedRowRender: (row) => <RefereeBreakdown
              referees={row.referees}
              scoreDifferenceExceeded={row.scoreDifferenceStatus === 'exceeded'}
              canManage={canManage}
              onReturn={(task) => {
                setReturnTask(task);
                setReturnReason('');
              }}
            />,
            rowExpandable: (row) => row.referees.length > 0,
            expandRowByClick: true,
          }}
        />
      </> : null}
    </Card> : null}
    <Modal
      open={Boolean(returnTask)}
      title="退回裁判评分重评"
      okText="确认退回"
      cancelText="取消"
      okButtonProps={{ disabled: !returnReason.trim() }}
      confirmLoading={returnMutation.isPending}
      onCancel={() => {
        if (returnMutation.isPending) return;
        setReturnTask(null);
        setReturnReason('');
      }}
      onOk={() => returnMutation.mutate()}
    >
      <Typography.Paragraph>
        将退回裁判“{returnTask?.refereeName || idValue(returnTask?.refereeMemberId)}”的评分（当前总分：{formatScore(returnTask?.totalScore) ?? '-'}）。
        该裁判重新提交后系统会自动复核分差。
      </Typography.Paragraph>
      <Input.TextArea
        rows={3}
        maxLength={200}
        showCount
        value={returnReason}
        placeholder="请填写退回原因，例如：与其他裁判分差过大，请复核第 2 题评分"
        onChange={(event) => setReturnReason(event.target.value)}
      />
    </Modal>
  </Space>;
}

function RefereeBreakdown({
  referees,
  scoreDifferenceExceeded,
  canManage,
  onReturn,
}: {
  referees: CompetitionPracticalConsoleTask[];
  scoreDifferenceExceeded: boolean;
  canManage: boolean;
  onReturn: (task: CompetitionPracticalConsoleTask) => void;
}) {
  const padded: Array<CompetitionPracticalConsoleTask | null> = [...referees];
  while (padded.length < 3) padded.push(null);
  const submittedScores = referees
    .filter((item) => item.scoreSheetStatus === 'submitted_locked' && item.totalScore != null)
    .map((item) => Number(item.totalScore));
  const minScore = submittedScores.length ? Math.min(...submittedScores) : undefined;
  const maxScore = submittedScores.length ? Math.max(...submittedScores) : undefined;

  return (
    <div className="practical-candidate-assessors">
      {padded.map((referee, index) => {
        if (!referee) {
          return (
            <div
              key={`empty-${index}`}
              className="practical-candidate-assessor practical-candidate-assessor--empty"
            >
              该位置暂无裁判
            </div>
          );
        }
        const questions = [...(referee.questions ?? [])].sort((left, right) => left.sortOrder - right.sortOrder);
        const submittedQuestions = questions.filter((item) => questionSubmitted(item, referee.scoreSheetStatus)).length;
        const totalQuestions = questions.length;
        const sheetMeta = sheetStatusMeta(referee.scoreSheetStatus);
        const edgeScore = scoreDifferenceExceeded
          && referee.totalScore != null
          && [minScore, maxScore].includes(Number(referee.totalScore));
        return (
          <div key={idValue(referee.assignmentId)} className={`practical-candidate-assessor${edgeScore ? ' practical-candidate-assessor--difference-edge' : ''}`}>
            <div className="practical-assessor-head">
              <span className="assessor-name">{referee.refereeName || idValue(referee.refereeMemberId)}</span>
              <Tag color={sheetMeta.color}>{sheetMeta.text}</Tag>
            </div>
            <div className="practical-assessor-progress">
              <Progress
                percent={totalQuestions ? Math.round((submittedQuestions * 100) / totalQuestions) : (referee.scoreSheetStatus === 'submitted_locked' ? 100 : 0)}
                size="small"
                style={{ flex: 1, margin: 0 }}
              />
              <span>{totalQuestions ? `${submittedQuestions}/${totalQuestions} 已提交` : sheetMeta.text}</span>
            </div>
            <div className="practical-assessor-tasks">
              {questions.length === 0 ? (
                <Typography.Text type="secondary">暂无试题明细</Typography.Text>
              ) : questions.map((question) => (
                <QuestionRow
                  key={`${idValue(referee.assignmentId)}-${question.paperQuestionId ?? question.sortOrder}`}
                  question={question}
                  sheetStatus={referee.scoreSheetStatus}
                />
              ))}
            </div>
            <div className="practical-assessor-total">
              <span>合计分</span>
              <strong>{formatScore(referee.totalScore) ?? '-'}</strong>
            </div>
            {scoreDifferenceExceeded && canManage && referee.scoreSheetStatus === 'submitted_locked' ? (
              <Button danger onClick={() => onReturn(referee)}>退回重评</Button>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

function QuestionRow({
  question,
  sheetStatus,
}: {
  question: CompetitionPracticalConsoleQuestion;
  sheetStatus?: string | null;
}) {
  const status = sheetStatusMeta(question.status || sheetStatus);
  return (
    <div className="practical-assessor-task">
      <span className="task-title">{questionTitle(question)}</span>
      <span className="task-score">{formatScore(question.score) ?? '-'}</span>
      <Tag color={status.color}>{status.text}</Tag>
    </div>
  );
}

function questionSubmitted(
  question?: CompetitionPracticalConsoleQuestion | null,
  sheetStatus?: string | null,
) {
  const status = question?.status || sheetStatus;
  return status === 'submitted' || status === 'submitted_locked';
}

function questionTitle(question: CompetitionPracticalConsoleQuestion) {
  const prefix = `试题${question.sortOrder}`;
  const title = (question.title || '').trim();
  if (!title) return prefix;
  if (title === prefix || title.startsWith(`${prefix}、`) || title.startsWith(`${prefix} `)) return title;
  return `${prefix}、${title}`;
}

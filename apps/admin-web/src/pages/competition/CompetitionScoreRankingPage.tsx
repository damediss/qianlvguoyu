import {
  ArrowDownOutlined,
  ArrowRightOutlined,
  ArrowUpOutlined,
  BookOutlined,
  CalculatorOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  FilterOutlined,
  InfoCircleOutlined,
  LockOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined,
  TeamOutlined,
  TrophyFilled,
  TrophyOutlined,
  UploadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Col,
  Divider,
  Drawer,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Progress,
  Result,
  Row,
  Segmented,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  Upload,
  Checkbox,
  type TableColumnsType,
  message,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import {
  confirmCompetitionRankingSnapshot,
  confirmCompetitionSimulationImport,
  calculateCompetitionTotalScore,
  confirmCompetitionSubjectScoreImport,
  downloadCompetitionSimulationImportErrors,
  downloadCompetitionSubjectScoreImportErrors,
  downloadCompetitionSubjectScoreTemplate,
  getCompetitionSubjectScoreImport,
  getCompetitionEvent,
  downloadCompetitionFormalCandidateAnswerSheet,
  downloadCompetitionFormalCandidateAnswerSheets,
  getCompetitionFormalCandidateAnswerSheet,
  getCompetitionFormalCandidateResult,
  getCompetitionScoringReadiness,
  getCompetitionSimulationImport,
  getCompetitionStageWorkspace,
  generateCompetitionRankingSnapshot,
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  listCompetitionScoreRules,
  pageCompetitionFormalScoringTasks,
  pageCompetitionPlayers,
  pageCompetitionRankingEntries,
  pageCompetitionRankingSnapshots,
  pageCompetitionScoreSources,
  pageCompetitionStages,
  pageCompetitionTotalScoreSnapshots,
  publishCompetitionRankingSnapshot,
  retryCompetitionFormalScoringTask,
  uploadCompetitionSimulationScores,
  uploadCompetitionSubjectScores,
} from '../../api/competition';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionFormalCandidateAnswerSheet,
  CompetitionFormalCandidateAnswerSheetQuestion,
  CompetitionFormalCandidateResult,
  CompetitionFormalScoringTask,
  CompetitionId,
  CompetitionPlayer,
  CompetitionRankingCandidate,
  CompetitionRankingEntry,
  CompetitionRankingSnapshot,
  CompetitionScoreConfirmationStatus,
  CompetitionScoreRule,
  CompetitionScoreSource,
  CompetitionScoreSourceType,
  CompetitionSubjectScoreImportBatch,
  CompetitionStageContent,
  CompetitionTotalScoreSnapshot,
} from '../../types/competition';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import {
  buildRankingCandidates,
  enabledScoreSourceTypes,
  findCalculationReadyContestants,
  rankingEntryPageItems,
  stageContentFormulaReady,
} from './competitionScoreUtils';
import './CompetitionEventManagementPage.css';
import './competitionScoreRanking.css';

type ScoreStatus =
  | 'not_started'
  | 'pending_generation'
  | 'pending_input'
  | 'scoring'
  | 'pending_review'
  | 'confirmed'
  | 'locked'
  | 'absent'
  | 'abnormal'
  | 'arbitrating';

interface SubjectConfig {
  type: CompetitionScoreSourceType;
  label: string;
  shortLabel: string;
  color: string;
  icon: ReactNode;
  weight?: number;
  providerCode?: string | null;
}

interface ScoreCell {
  value?: number | string | null;
  status: ScoreStatus;
  source?: CompetitionScoreSource;
  task?: CompetitionFormalScoringTask;
}

interface ScoreOverviewRow {
  key: string;
  contestantMemberId: CompetitionId;
  player?: CompetitionPlayer;
  candidateName: string;
  candidateNo: string;
  teamName: string;
  workTypeName: string;
  levelName: string;
  rank?: number | null;
  total?: CompetitionTotalScoreSnapshot;
  rankEntry?: CompetitionRankingEntry;
  sourceByType: Partial<
    Record<CompetitionScoreSourceType, CompetitionScoreSource>
  >;
  subjectCells: Record<CompetitionScoreSourceType, ScoreCell>;
  overallStatus: ScoreStatus;
}

interface RankingDraftRow extends CompetitionRankingCandidate {
  key: string;
  totalScoreSnapshot?: CompetitionTotalScoreSnapshot;
}

const sourceMeta: Record<
  CompetitionScoreSourceType,
  {
    label: string;
    shortLabel: string;
    color: string;
    icon: ReactNode;
    scoreField: keyof CompetitionTotalScoreSnapshot;
  }
> = {
  theory: {
    label: '理论考试',
    shortLabel: '理论',
    color: '#1677ff',
    icon: <BookOutlined />,
    scoreField: 'theoryScore',
  },
  practical: {
    label: '实操考评',
    shortLabel: '实操',
    color: '#fa8c16',
    icon: <SettingOutlined />,
    scoreField: 'practicalScore',
  },
  simulation: {
    label: '仿真操作',
    shortLabel: '仿真',
    color: '#722ed1',
    icon: <ExperimentOutlined />,
    scoreField: 'simulationScore',
  },
};

const statusMeta: Record<
  ScoreStatus,
  { label: string; color: string; icon: ReactNode }
> = {
  not_started: {
    label: '未开始',
    color: 'default',
    icon: <ClockCircleOutlined />,
  },
  pending_generation: {
    label: '待生成',
    color: 'processing',
    icon: <ClockCircleOutlined />,
  },
  pending_input: {
    label: '待录入',
    color: 'warning',
    icon: <ClockCircleOutlined />,
  },
  scoring: {
    label: '评分中',
    color: 'processing',
    icon: <ClockCircleOutlined />,
  },
  pending_review: {
    label: '待审核',
    color: 'warning',
    icon: <WarningOutlined />,
  },
  confirmed: { label: '已确认', color: 'success', icon: <CheckCircleFilled /> },
  locked: { label: '已锁定', color: 'blue', icon: <LockOutlined /> },
  absent: { label: '缺考', color: 'default', icon: <CloseCircleOutlined /> },
  abnormal: { label: '异常', color: 'error', icon: <WarningOutlined /> },
  arbitrating: {
    label: '仲裁中',
    color: 'purple',
    icon: <InfoCircleOutlined />,
  },
};

const confirmationMeta: Record<
  CompetitionScoreConfirmationStatus,
  { label: string; color: string }
> = {
  pending: { label: '待确认', color: 'warning' },
  confirmed: { label: '已确认', color: 'success' },
  voided: { label: '已作废', color: 'default' },
};

function idValue(value?: CompetitionId | null) {
  return value === undefined || value === null ? '-' : String(value);
}

function scoreNumber(value?: number | string | null) {
  if (value === undefined || value === null || value === '') return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function scoreValue(value?: number | string | null) {
  const parsed = scoreNumber(value);
  return parsed === undefined ? '-' : parsed.toFixed(2);
}

function compareScoreValues(
  left?: number | string | null,
  right?: number | string | null,
) {
  const leftScore = scoreNumber(left);
  const rightScore = scoreNumber(right);
  if (leftScore === undefined) return rightScore === undefined ? 0 : 1;
  if (rightScore === undefined) return -1;
  return leftScore - rightScore;
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function statusTag(status: ScoreStatus, compact = false) {
  const meta = statusMeta[status];
  return (
    <Tag
      className={compact ? 'competition-score-status-tag--compact' : undefined}
      color={meta.color}
      icon={meta.icon}
    >
      {meta.label}
    </Tag>
  );
}

function confirmationTag(status: CompetitionScoreConfirmationStatus) {
  const meta = confirmationMeta[status];
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}

function sanitizeAnswerSheetFileStem(value?: string | null) {
  const cleaned = (value || '')
    .replace(/[\\/:*?"<>|\u0000-\u001F\u007F]/g, '')
    .trim()
    .replace(/[. ]+$/g, '');
  const stem = cleaned.slice(0, 80).replace(/[. ]+$/g, '');
  if (/^(con|prn|aux|nul|com[1-9]|lpt[1-9])$/i.test(stem)) return `${stem}_`;
  return stem;
}

function answerSheetDocxName(
  candidateName?: string | null,
  candidateNo?: string | null,
) {
  const name = sanitizeAnswerSheetFileStem(candidateName);
  const no = sanitizeAnswerSheetFileStem(candidateNo);
  return `${name || no || '答题卡'}.docx`;
}

function csvCell(value: unknown) {
  const text = value === undefined || value === null ? '' : String(value);
  return `"${text.replace(/"/g, '""')}"`;
}

function enabledSubjectConfigs(
  contents: CompetitionStageContent[] | undefined,
  rule: CompetitionScoreRule | undefined,
  sources: CompetitionScoreSource[],
  totals: CompetitionTotalScoreSnapshot[],
): SubjectConfig[] {
  const configured = contents
    ? contents
        .filter((item) => item.status === 'enabled')
        .sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0))
        .map((item) => item.contentType)
        .filter((type, index, all) => all.indexOf(type) === index)
    : [];

  let types = configured;
  if (!contents) {
    const inferred = new Set<CompetitionScoreSourceType>();
    sources.forEach((source) => inferred.add(source.sourceType));
    totals.forEach((total) => {
      if (total.theoryScore !== undefined && total.theoryScore !== null)
        inferred.add('theory');
      if (total.practicalScore !== undefined && total.practicalScore !== null)
        inferred.add('practical');
      if (total.simulationScore !== undefined && total.simulationScore !== null)
        inferred.add('simulation');
    });
    if (rule?.theoryWeight !== undefined && rule?.theoryWeight !== null)
      inferred.add('theory');
    if (rule?.practicalWeight !== undefined && Number(rule.practicalWeight) > 0)
      inferred.add('practical');
    if (
      rule?.simulationWeight !== undefined &&
      Number(rule.simulationWeight) > 0
    )
      inferred.add('simulation');
    types = (
      ['theory', 'practical', 'simulation'] as CompetitionScoreSourceType[]
    ).filter((type) => inferred.has(type));
  }

  const contentByType = new Map(
    (contents || [])
      .filter((item) => item.status === 'enabled')
      .map((item) => [item.contentType, item]),
  );
  const hasSeparateRuleWeights =
    rule?.simulationWeight != null || rule?.practicalWeight != null;
  return types.map((type) => {
    const meta = sourceMeta[type];
    const configuredWeight = contentByType.get(type)?.weight;
    let weight = scoreNumber(configuredWeight);
    if (weight === undefined && rule) {
      if (type === 'theory') weight = scoreNumber(rule.theoryWeight);
      if (type === 'simulation' && hasSeparateRuleWeights)
        weight = scoreNumber(rule.simulationWeight);
      if (type === 'practical' && hasSeparateRuleWeights)
        weight = scoreNumber(rule.practicalWeight);
    }
    return {
      type,
      label: meta.label,
      shortLabel: meta.shortLabel,
      color: meta.color,
      icon: meta.icon,
      weight,
      providerCode: contentByType.get(type)?.providerCode,
    };
  });
}

function currentSources(sources: CompetitionScoreSource[]) {
  const result = new Map<
    string,
    Partial<Record<CompetitionScoreSourceType, CompetitionScoreSource>>
  >();
  sources.forEach((source) => {
    const key = idValue(source.contestantMemberId);
    const current = result.get(key) || {};
    const previous = current[source.sourceType];
    if (
      !previous ||
      source.currentFlag === 1 ||
      (previous.currentFlag !== 1 && source.revisionNo >= previous.revisionNo)
    ) {
      current[source.sourceType] = source;
      result.set(key, current);
    }
  });
  return result;
}

function currentTotals(totals: CompetitionTotalScoreSnapshot[]) {
  const result = new Map<string, CompetitionTotalScoreSnapshot>();
  totals.forEach((total) => {
    const key = idValue(total.contestantMemberId);
    const previous = result.get(key);
    if (
      !previous ||
      total.currentFlag === 1 ||
      (previous.currentFlag !== 1 &&
        total.calculationVersion >= previous.calculationVersion)
    ) {
      result.set(key, total);
    }
  });
  return result;
}

function subjectCell(
  type: CompetitionScoreSourceType,
  source: CompetitionScoreSource | undefined,
  task: CompetitionFormalScoringTask | undefined,
  absent: boolean,
): ScoreCell {
  if (absent) return { status: 'absent' };
  if (source?.confirmationStatus === 'voided')
    return { source, status: 'abnormal' };
  if (source?.confirmationStatus === 'confirmed')
    return { source, value: source.rawScore, status: 'confirmed' };
  if (source?.confirmationStatus === 'pending')
    return { source, value: source.rawScore, status: 'pending_review' };
  if (task?.taskStatus === 'failed') return { task, status: 'abnormal' };
  if (task && ['processing', 'retry'].includes(task.taskStatus))
    return { task, status: 'scoring' };
  if (task?.taskStatus === 'pending')
    return { task, status: 'pending_generation' };
  return { status: type === 'theory' ? 'pending_generation' : 'pending_input' };
}

function overallStatus(
  cells: ScoreCell[],
  total: CompetitionTotalScoreSnapshot | undefined,
  absent: boolean,
): ScoreStatus {
  if (absent) return 'absent';
  if (cells.some((cell) => cell.status === 'abnormal')) return 'abnormal';
  if (cells.some((cell) => cell.status === 'arbitrating')) return 'arbitrating';
  if (cells.some((cell) => cell.status === 'pending_review'))
    return 'pending_review';
  if (total?.confirmationStatus === 'confirmed') return 'confirmed';
  if (total?.confirmationStatus === 'pending') return 'pending_review';
  if (cells.some((cell) => cell.status === 'scoring')) return 'scoring';
  if (cells.some((cell) => cell.status === 'pending_input'))
    return 'pending_input';
  if (cells.some((cell) => cell.status === 'pending_generation'))
    return 'pending_generation';
  return 'not_started';
}

function latestFormalScoringTask(
  tasks: CompetitionFormalScoringTask[],
  memberId: string,
) {
  let selected: CompetitionFormalScoringTask | undefined;
  tasks.forEach((task) => {
    if (!task.candidateExamId || idValue(task.contestantMemberId) !== memberId) return;
    const newerRevision = !selected || task.resultRevisionNo > selected.resultRevisionNo;
    const sameRevisionCompleted =
      selected
      && task.resultRevisionNo === selected.resultRevisionNo
      && task.taskStatus === 'completed'
      && selected.taskStatus !== 'completed';
    if (newerRevision || sameRevisionCompleted) selected = task;
  });
  return selected;
}

function buildOverviewRows(
  players: CompetitionPlayer[],
  sources: CompetitionScoreSource[],
  totals: CompetitionTotalScoreSnapshot[],
  entries: CompetitionRankingEntry[],
  tasks: CompetitionFormalScoringTask[],
  subjects: SubjectConfig[],
): ScoreOverviewRow[] {
  const playerByMember = new Map<string, CompetitionPlayer>();
  players.forEach((player) => {
    const key = player.memberId || player.playerId || player.registrationId;
    if (key != null) playerByMember.set(idValue(key), player);
  });
  const sourceMap = currentSources(sources);
  const totalMap = currentTotals(totals);
  const entryMap = new Map(
    entries.map((entry) => [idValue(entry.contestantMemberId), entry]),
  );
  const taskMap = new Map<string, CompetitionFormalScoringTask>();
  tasks.forEach((task) => {
    if (!task.contestantMemberId) return;
    const key = idValue(task.contestantMemberId);
    const previous = taskMap.get(key);
    if (!previous || task.resultRevisionNo >= previous.resultRevisionNo)
      taskMap.set(key, task);
  });
  const ids = new Set<string>([
    ...playerByMember.keys(),
    ...sourceMap.keys(),
    ...totalMap.keys(),
    ...entryMap.keys(),
    ...taskMap.keys(),
  ]);
  return [...ids].map((memberId) => {
    const player = playerByMember.get(memberId);
    const sourceByType = sourceMap.get(memberId) || {};
    const task = taskMap.get(memberId);
    const rankEntry = entryMap.get(memberId);
    const total = totalMap.get(memberId);
    const absent =
      rankEntry?.contestantStatus === 'absent' ||
      task?.candidateStatus === 'absent' ||
      task?.candidateStatus === 'provisional_absent';
    const subjectCells = subjects.reduce<
      Record<CompetitionScoreSourceType, ScoreCell>
    >(
      (result, subject) => {
        result[subject.type] = subjectCell(
          subject.type,
          sourceByType[subject.type],
          subject.type === 'theory' ? task : undefined,
          absent,
        );
        return result;
      },
      {} as Record<CompetitionScoreSourceType, ScoreCell>,
    );
    const candidateName =
      player?.name || task?.candidateName || `选手 ${memberId}`;
    return {
      key: memberId,
      contestantMemberId: memberId,
      player,
      candidateName,
      candidateNo:
        task?.candidateNo ||
        (player?.registrationId != null ? idValue(player.registrationId) : '-'),
      teamName: player?.orgName || '-',
      workTypeName: player?.eventWorkTypeName || '-',
      levelName:
        player?.eventWorkTypeLevelName ||
        player?.eventWorkTypeLevelCode ||
        player?.level ||
        '-',
      rank: rankEntry?.displayRank,
      total,
      rankEntry,
      sourceByType,
      subjectCells,
      overallStatus: overallStatus(Object.values(subjectCells), total, absent),
    };
  });
}

function ScoreCellView({
  cell,
  onClick,
}: {
  cell: ScoreCell;
  onClick: () => void;
}) {
  const meta = statusMeta[cell.status];
  return (
    <button type="button" className="competition-score-cell" onClick={onClick}>
      <span
        className={`competition-score-cell__value competition-score-cell__value--${cell.status}`}
      >
        {cell.value === undefined || cell.value === null
          ? '—'
          : scoreValue(cell.value)}
      </span>
      <Tag color={meta.color}>{meta.label}</Tag>
    </button>
  );
}

function answerSheetStatus(
  question: CompetitionFormalCandidateAnswerSheetQuestion,
): 'correct' | 'wrong' | 'unanswered' {
  if (!question.answered) return 'unanswered';
  return question.correct ? 'correct' : 'wrong';
}

function selectedAnswerLabels(
  question: CompetitionFormalCandidateAnswerSheetQuestion,
) {
  const selectedIds = question.candidateAnswer?.selectedOptionIds || [];
  return selectedIds.map((optionId) => {
    const option = question.options?.find((item) => item.optionId === optionId);
    return option ? `${option.label}. ${option.content}` : optionId;
  });
}

function questionTypeLabel(type: string) {
  const labels: Record<string, string> = {
    single: '单选题',
    multi: '多选题',
    judge: '判断题',
    fill: '填空题',
  };
  return labels[type] || type || '题目';
}

export function CompetitionScoreRankingPage({
  eventId,
  onNavigate,
  embedded = false,
}: {
  eventId: string;
  onNavigate: (path: string) => void;
  embedded?: boolean;
}) {
  const paneActive = usePaneActive();
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canViewEvent = permissions.includes('competition:event:view');
  const canViewScores = permissions.includes('competition:score:view');
  const canCalculate = permissions.includes('competition:score:calculate');
  const canViewRanking = permissions.includes('competition:ranking:view');
  const canGenerateRanking = permissions.includes(
    'competition:ranking:generate',
  );
  const canReviewRanking = permissions.includes('competition:score:review');
  const canPublishRanking = permissions.includes('competition:ranking:publish');
  const canImportSimulation = permissions.includes('competition:score:import');
  const canRetryScoring = permissions.includes('competition:score-task:retry');
  const canViewPlayers = permissions.includes('competition:player:view');
  const canReadStagePlan = permissions.includes('competition:stage-plan:view');
  const canReadDimensions =
    permissions.includes('competition:stage:view') &&
    permissions.includes('competition:event-work-type:view') &&
    permissions.includes('competition:score-rule:view');
  const canReadReadiness = permissions.includes(
    'competition:scoring-readiness:view',
  );
  const [stageId, setStageId] = useState<string>();
  // 原始工种 state（可能短暂持有跨赛事的旧 id）；下游一律使用下方派生的 effectiveEventWorkTypeId
  const [rawEventWorkTypeId, setEventWorkTypeId] = useState<string>();
  const [eventWorkTypeLevelId, setEventWorkTypeLevelId] = useState<string>();
  const [scoreRuleId, setScoreRuleId] = useState<string>();
  const [activeTab, setActiveTab] = useState('overview');
  const [workbenchSubject, setWorkbenchSubject] =
    useState<CompetitionScoreSourceType>();
  const [selectedMemberId, setSelectedMemberId] = useState<string>();
  const [answerSheetOpen, setAnswerSheetOpen] = useState(false);
  const [exportingAnswerSheetId, setExportingAnswerSheetId] = useState<string>();
  const [exportingAnswerSheets, setExportingAnswerSheets] = useState(false);
  const [selectedAnswerQuestionId, setSelectedAnswerQuestionId] =
    useState<string>();
  const [selectedSnapshotId, setSelectedSnapshotId] = useState<string>();
  const [simulationBatchId, setSimulationBatchId] = useState<string>();
  const [subjectBatchIds, setSubjectBatchIds] = useState<
    Partial<Record<'practical' | 'simulation', string>>
  >({});
  const [importSubject, setImportSubject] = useState<
    'practical' | 'simulation'
  >('practical');
  const [rankingDraftOpen, setRankingDraftOpen] = useState(false);
  const [rankingDraftRows, setRankingDraftRows] = useState<RankingDraftRow[]>(
    [],
  );
  const [generationReason, setGenerationReason] = useState('');
  const [absentContestantId, setAbsentContestantId] = useState('');
  const [rankingReviewed, setRankingReviewed] = useState(false);
  const [orgFilter, setOrgFilter] = useState<string>();
  const [statusFilter, setStatusFilter] = useState<ScoreStatus>();
  const [keyword, setKeyword] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
  const queryClient = useQueryClient();

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canViewEvent,
  });
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canViewEvent && canReadDimensions,
  });
  // 渲染期派生有效工种：跨赛事/URL 参数混合时，未在当前赛事工种列表中的 id 当帧钳为 undefined，
  // 全部查询/操作以此值为准，混合帧内不会发出请求（effect 事后纠偏存在一帧竞态）
  const eventWorkTypeId = rawEventWorkTypeId
    && (workTypesQuery.data || []).some((item) => idValue(item.id) === rawEventWorkTypeId)
    ? rawEventWorkTypeId
    : undefined;
  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', eventId, eventWorkTypeId],
    queryFn: () =>
      listCompetitionEventWorkTypeLevels(eventId, eventWorkTypeId!),
    enabled: canViewEvent && canReadDimensions && Boolean(eventWorkTypeId),
  });
  const levelRequired = (levelsQuery.data?.length || 0) > 0;
  const selectedLevelCode = levelsQuery.data?.find(
    (level) => idValue(level.id) === eventWorkTypeLevelId,
  )?.levelCode;
  const stagesQuery = useQuery({
    queryKey: [
      'competition-stages',
      eventId,
      eventWorkTypeId,
      selectedLevelCode,
    ],
    queryFn: () =>
      pageCompetitionStages(eventId, {
        page: 1,
        pageSize: 500,
        eventWorkTypeId,
        eventWorkTypeLevelCode: selectedLevelCode,
      }),
    enabled:
      canViewEvent &&
      canReadDimensions &&
      Boolean(eventWorkTypeId) &&
      !levelsQuery.isLoading &&
      (!levelRequired || Boolean(selectedLevelCode)),
  });
  const scopeReady = Boolean(
    stageId &&
    eventWorkTypeId &&
    !levelsQuery.isLoading &&
    (!levelRequired || eventWorkTypeLevelId),
  );
  const rulesQuery = useQuery({
    queryKey: [
      'competition-score-rules',
      eventId,
      stageId,
      eventWorkTypeId,
      eventWorkTypeLevelId,
    ],
    queryFn: () =>
      listCompetitionScoreRules(eventId, {
        stageId,
        eventWorkTypeId,
        eventWorkTypeLevelId,
      }),
    enabled:
      canReadDimensions &&
      Boolean(stageId && eventWorkTypeId) &&
      !levelsQuery.isLoading &&
      (!levelRequired || Boolean(eventWorkTypeLevelId)),
  });
  const stageWorkspaceQuery = useQuery({
    queryKey: ['competition-stage-workspace', stageId],
    queryFn: () => getCompetitionStageWorkspace(stageId!),
    enabled: canReadStagePlan && Boolean(stageId),
    retry: false,
  });

  useEffect(() => {
    const stages = stagesQuery.data?.items || [];
    if (!stages.some((stage) => idValue(stage.id) === stageId)) {
      setStageId(stages[0] ? idValue(stages[0].id) : undefined);
      setScoreRuleId(undefined);
      setSelectedSnapshotId(undefined);
    }
  }, [stageId, stagesQuery.data?.items]);
  useEffect(() => {
    if (!rawEventWorkTypeId && workTypesQuery.data?.length)
      setEventWorkTypeId(idValue(workTypesQuery.data[0].id));
  }, [rawEventWorkTypeId, workTypesQuery.data]);
  useEffect(() => {
    const levels = levelsQuery.data || [];
    if (!levels.some((level) => idValue(level.id) === eventWorkTypeLevelId))
      setEventWorkTypeLevelId(levels[0] ? idValue(levels[0].id) : undefined);
  }, [eventWorkTypeLevelId, levelsQuery.data]);
  useEffect(() => {
    const publishedRules = (rulesQuery.data || []).filter(
      (rule) => rule.status === 'published',
    );
    const preferredRule =
      publishedRules.find(
        (rule) =>
          eventWorkTypeLevelId &&
          idValue(rule.eventWorkTypeLevelId) === eventWorkTypeLevelId,
      ) ||
      publishedRules.find((rule) => !rule.eventWorkTypeLevelId) ||
      publishedRules[0];
    const preferredRuleId = preferredRule
      ? idValue(preferredRule.id)
      : undefined;
    if (preferredRuleId !== scoreRuleId) setScoreRuleId(preferredRuleId);
  }, [eventWorkTypeLevelId, rulesQuery.data, scoreRuleId]);

  const readinessQuery = useQuery({
    queryKey: [
      'competition-scoring-readiness',
      eventId,
      stageId,
      eventWorkTypeId,
      eventWorkTypeLevelId,
    ],
    queryFn: () =>
      getCompetitionScoringReadiness(eventId, {
        stageId,
        eventWorkTypeId,
        eventWorkTypeLevelId,
      }),
    enabled: canReadReadiness && scopeReady,
  });
  const theoryOnly = Boolean(
    stageWorkspaceQuery.data?.contents?.some(
      (content) => content.status === 'enabled' && content.contentType === 'theory',
    ) &&
      stageWorkspaceQuery.data?.contents
        ?.filter((content) => content.status === 'enabled')
        .every((content) => content.contentType === 'theory'),
  );
  const usesStageContentFormula =
    !scoreRuleId &&
    (stageContentFormulaReady(stageWorkspaceQuery.data?.contents) ||
      Boolean(
        readinessQuery.data?.ready &&
          !readinessQuery.data.publishedScoreRuleVersionId,
      ));
  const contextReady =
    scopeReady && (usesStageContentFormula || Boolean(scoreRuleId));
  const scoreSourcesQuery = useQuery({
    queryKey: [
      'competition-score-sources',
      eventId,
      stageId,
      eventWorkTypeId,
      eventWorkTypeLevelId,
    ],
    queryFn: () =>
      pageCompetitionScoreSources(eventId, {
        page: 1,
        pageSize: 500,
        stageId: stageId!,
        eventWorkTypeId: eventWorkTypeId!,
        eventWorkTypeLevelId,
      }),
    enabled: canViewScores && scopeReady,
  });
  const totalSnapshotsQuery = useQuery({
    queryKey: [
      'competition-total-score-snapshots',
      eventId,
      stageId,
      eventWorkTypeId,
      eventWorkTypeLevelId,
      scoreRuleId,
    ],
    queryFn: () =>
      pageCompetitionTotalScoreSnapshots(eventId, {
        page: 1,
        pageSize: 500,
        stageId: stageId!,
        eventWorkTypeId: eventWorkTypeId!,
        eventWorkTypeLevelId,
        scoreRuleId,
      }),
    enabled: canViewScores && contextReady,
  });
  const rankingSnapshotsQuery = useQuery({
    queryKey: [
      'competition-ranking-snapshots',
      eventId,
      stageId,
      eventWorkTypeId,
      eventWorkTypeLevelId,
      scoreRuleId,
    ],
    queryFn: () =>
      pageCompetitionRankingSnapshots(eventId, {
        page: 1,
        pageSize: 500,
        stageId: stageId!,
        eventWorkTypeId: eventWorkTypeId!,
        eventWorkTypeLevelId,
        scoreRuleId,
      }),
    enabled: canViewRanking && contextReady,
  });
  const rankingEntriesQuery = useQuery({
    queryKey: ['competition-ranking-entries', selectedSnapshotId],
    queryFn: () =>
      pageCompetitionRankingEntries(selectedSnapshotId!, {
        page: 1,
        pageSize: 500,
      }),
    enabled: canViewRanking && Boolean(selectedSnapshotId),
  });
  const playersQuery = useQuery({
    queryKey: ['competition-score-players', eventId, eventWorkTypeId],
    queryFn: () =>
      pageCompetitionPlayers({
        page: 1,
        pageSize: 500,
        eventId,
        eventWorkTypeId,
      }),
    enabled: canViewPlayers && Boolean(eventWorkTypeId),
  });
  const scoringTasksQuery = useQuery({
    queryKey: [
      'competition-formal-scoring-tasks',
      eventId,
      stageId,
      eventWorkTypeId,
      eventWorkTypeLevelId,
    ],
    queryFn: () =>
      pageCompetitionFormalScoringTasks({
        page: 1,
        pageSize: 500,
        eventId,
        stageId,
        eventWorkTypeId,
        eventWorkTypeLevelId,
      }),
    enabled: canViewScores && Boolean(stageId && eventWorkTypeId),
    refetchInterval: (query) =>
      query.state.data?.items.some((task) =>
        ['pending', 'processing', 'retry'].includes(task.taskStatus),
      )
        ? 1000
        : false,
  });
  const simulationBatchQuery = useQuery({
    queryKey: ['competition-simulation-import', simulationBatchId],
    queryFn: () => getCompetitionSimulationImport(simulationBatchId!),
    enabled: Boolean(simulationBatchId),
    refetchInterval: (query) =>
      paneActive &&
      ['parsing', 'confirming'].includes(query.state.data?.batchStatus || '')
        ? 1000
        : false,
  });
  const practicalImportQuery = useQuery({
    queryKey: ['competition-subject-score-import', subjectBatchIds.practical],
    queryFn: () => getCompetitionSubjectScoreImport(subjectBatchIds.practical!),
    enabled: Boolean(subjectBatchIds.practical),
    refetchInterval: (query) =>
      paneActive &&
      ['parsing', 'confirming'].includes(query.state.data?.batchStatus || '')
        ? 1000
        : false,
  });
  const simulationIdCardImportQuery = useQuery({
    queryKey: ['competition-subject-score-import', subjectBatchIds.simulation],
    queryFn: () => getCompetitionSubjectScoreImport(subjectBatchIds.simulation!),
    enabled: Boolean(subjectBatchIds.simulation),
    refetchInterval: (query) =>
      paneActive &&
      ['parsing', 'confirming'].includes(query.state.data?.batchStatus || '')
        ? 1000
        : false,
  });

  const selectedRule = useMemo(
    () =>
      (rulesQuery.data || []).find((rule) => idValue(rule.id) === scoreRuleId),
    [rulesQuery.data, scoreRuleId],
  );
  const scoreSources = scoreSourcesQuery.data?.items || [];
  const totalSnapshots = totalSnapshotsQuery.data?.items || [];
  const subjects = useMemo(
    () =>
      enabledSubjectConfigs(
        stageWorkspaceQuery.data?.contents,
        selectedRule,
        scoreSources,
        totalSnapshots,
      ),
    [
      scoreSources,
      selectedRule,
      stageWorkspaceQuery.data?.contents,
      totalSnapshots,
    ],
  );
  const selectedRuleSources = useMemo(
    () =>
      scoreSources.filter(
        (source) => idValue(source.scoreRuleId) === scoreRuleId,
      ),
    [scoreRuleId, scoreSources],
  );
  const scoringSources = useMemo(
    () =>
      usesStageContentFormula
        ? scoreSources.filter((source) => !source.scoreRuleId)
        : selectedRuleSources,
    [scoreSources, selectedRuleSources, usesStageContentFormula],
  );
  const requiredSourceTypes = useMemo(
    () =>
      enabledScoreSourceTypes(stageWorkspaceQuery.data?.contents).length
        ? enabledScoreSourceTypes(stageWorkspaceQuery.data?.contents)
        : undefined,
    [stageWorkspaceQuery.data?.contents],
  );
  const calculationReadyContestants = useMemo(
    () =>
      findCalculationReadyContestants(
        scoringSources,
        requiredSourceTypes,
      ),
    [requiredSourceTypes, scoringSources],
  );
  const players = useMemo(
    () =>
      (playersQuery.data?.items || []).filter(
        (player) =>
          !eventWorkTypeLevelId ||
          idValue(player.eventWorkTypeLevelId) === eventWorkTypeLevelId,
      ),
    [eventWorkTypeLevelId, playersQuery.data?.items],
  );
  const rankingSnapshots = rankingSnapshotsQuery.data?.items || [];
  const activeSnapshot = useMemo(
    () =>
      rankingSnapshots.find((snapshot) => snapshot.status === 'published') ||
      rankingSnapshots.find((snapshot) => snapshot.status === 'confirmed') ||
      rankingSnapshots.find((snapshot) => snapshot.status === 'draft'),
    [rankingSnapshots],
  );
  useEffect(() => {
    if (!activeSnapshot) setSelectedSnapshotId(undefined);
    else if (
      !selectedSnapshotId ||
      !rankingSnapshots.some(
        (snapshot) => idValue(snapshot.id) === selectedSnapshotId,
      )
    )
      setSelectedSnapshotId(idValue(activeSnapshot.id));
  }, [activeSnapshot, rankingSnapshots, selectedSnapshotId]);
  const rankingEntries = rankingEntryPageItems(rankingEntriesQuery.data);
  const scoringTasks = scoringTasksQuery.data?.items || [];
  const scoringTaskStateKey = useMemo(
    () =>
      scoringTasks
        .map((task) =>
          [
            idValue(task.id),
            task.taskStatus,
            task.attemptCount,
            task.completedAt || '',
          ].join(':'),
        )
        .sort()
        .join('|'),
    [scoringTasks],
  );
  const previousScoringTaskStateKey = useRef<string>();
  useEffect(() => {
    if (!scoringTasks.length || !scoringTaskStateKey) return;
    if (previousScoringTaskStateKey.current === scoringTaskStateKey) return;
    previousScoringTaskStateKey.current = scoringTaskStateKey;
    if (!scoringTasks.some((task) => task.taskStatus === 'completed')) return;
    void Promise.all([
      queryClient.invalidateQueries({
        queryKey: ['competition-score-sources', eventId],
      }),
      queryClient.invalidateQueries({
        queryKey: ['competition-total-score-snapshots', eventId],
      }),
      queryClient.invalidateQueries({
        queryKey: ['competition-ranking-snapshots', eventId],
      }),
    ]);
  }, [eventId, queryClient, scoringTaskStateKey, scoringTasks]);
  const overviewRows = useMemo(
    () =>
      buildOverviewRows(
        players,
        scoreSources,
        totalSnapshots,
        rankingEntries,
        scoringTasks,
        subjects,
      ),
    [
      players,
      rankingEntries,
      scoreSources,
      scoringTasks,
      subjects,
      totalSnapshots,
    ],
  );
  const orgOptions = useMemo(
    () => [
      ...new Map(
        overviewRows
          .filter((row) => row.teamName !== '-')
          .map((row) => [
            row.teamName,
            { value: row.teamName, label: row.teamName },
          ]),
      ).values(),
    ],
    [overviewRows],
  );
  const filteredRows = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return overviewRows.filter(
      (row) =>
        (!normalized ||
          row.candidateName.toLowerCase().includes(normalized) ||
          row.candidateNo.toLowerCase().includes(normalized) ||
          idValue(row.contestantMemberId).includes(normalized)) &&
        (!orgFilter || row.teamName === orgFilter) &&
        (!statusFilter || row.overallStatus === statusFilter),
    );
  }, [keyword, orgFilter, overviewRows, statusFilter]);
  const selectedRow = overviewRows.find((row) => row.key === selectedMemberId);
  const selectedTask = selectedRow
    ? latestFormalScoringTask(scoringTasks, selectedRow.key)
    : undefined;
  const formalResultQuery = useQuery<CompetitionFormalCandidateResult | null>({
    queryKey: [
      'competition-formal-candidate-result',
      selectedTask?.candidateExamId,
    ],
    queryFn: () =>
      getCompetitionFormalCandidateResult(selectedTask!.candidateExamId),
    enabled: canViewScores && Boolean(selectedTask?.candidateExamId),
    retry: false,
  });
  const answerSheetQuery = useQuery<CompetitionFormalCandidateAnswerSheet>({
    queryKey: [
      'competition-formal-candidate-answer-sheet',
      selectedTask?.candidateExamId,
    ],
    queryFn: () =>
      getCompetitionFormalCandidateAnswerSheet(selectedTask!.candidateExamId),
    enabled: answerSheetOpen && Boolean(selectedTask?.candidateExamId),
    retry: false,
  });
  const statistics = useMemo(() => {
    const absent = overviewRows.filter(
      (row) => row.overallStatus === 'absent',
    ).length;
    const abnormal = overviewRows.filter((row) =>
      ['abnormal', 'arbitrating'].includes(row.overallStatus),
    ).length;
    const pendingReview = overviewRows.filter(
      (row) => row.overallStatus === 'pending_review',
    ).length;
    const complete = overviewRows.filter(
      (row) =>
        row.total?.currentFlag === 1 &&
        row.total.confirmationStatus === 'confirmed',
    ).length;
    const participantCount = playersQuery.data?.total || overviewRows.length;
    const eligibleCount = Math.max(0, participantCount - absent);
    return {
      participantCount,
      eligibleCount,
      complete,
      absent,
      abnormal,
      pendingReview,
      completionPercent: eligibleCount
        ? Math.min(100, Math.round((complete / eligibleCount) * 100))
        : participantCount
          ? 100
          : 0,
    };
  }, [overviewRows, playersQuery.data?.total]);

  const confirmRankingMutation = useMutation({
    mutationFn: (snapshotId: CompetitionId) =>
      confirmCompetitionRankingSnapshot(snapshotId),
    onSuccess: () => {
      message.success('排名草稿已复核确认');
      queryClient.invalidateQueries({
        queryKey: ['competition-ranking-snapshots', eventId],
      });
    },
  });
  const publishRankingMutation = useMutation({
    mutationFn: ({
      snapshotId,
      correctionReason,
    }: {
      snapshotId: CompetitionId;
      correctionReason?: string;
    }) => publishCompetitionRankingSnapshot(snapshotId, correctionReason),
    onSuccess: () => {
      message.success('排名已发布，选手现在可以查看本人结果');
      queryClient.invalidateQueries({
        queryKey: ['competition-ranking-snapshots', eventId],
      });
    },
  });
  const calculateMutation = useMutation({
    mutationFn: async () =>
      Promise.all(
        calculationReadyContestants.map((contestantMemberId) =>
          calculateCompetitionTotalScore(eventId, {
            stageId: stageId!,
            eventWorkTypeId: eventWorkTypeId!,
            eventWorkTypeLevelId,
            contestantMemberId,
            ...(scoreRuleId ? { scoreRuleId } : {}),
          }),
        ),
      ),
    onSuccess: (results) => {
      message.success(`已生成 ${results.length} 条总分快照`);
      queryClient.invalidateQueries({
        queryKey: ['competition-total-score-snapshots', eventId],
      });
      queryClient.invalidateQueries({
        queryKey: ['competition-ranking-snapshots', eventId],
      });
    },
  });
  const autoCalculatedKey = useRef<string>();
  useEffect(() => {
    if (!usesStageContentFormula || !canCalculate || !contextReady) return;
    if (readinessQuery.data?.ready === false) return;
    if (calculationReadyContestants.length === 0) return;
    if (!totalSnapshotsQuery.isFetched || totalSnapshotsQuery.isFetching) return;
    if (
      totalSnapshots.some(
        (snapshot) =>
          snapshot.currentFlag === 1 &&
          snapshot.confirmationStatus === 'confirmed',
      )
    ) {
      return;
    }
    const key = [
      eventId,
      stageId,
      eventWorkTypeId,
      eventWorkTypeLevelId,
      ...calculationReadyContestants,
    ].join(':');
    if (autoCalculatedKey.current === key) return;
    autoCalculatedKey.current = key;
    calculateMutation.mutate();
  }, [
    calculationReadyContestants,
    canCalculate,
    contextReady,
    eventId,
    eventWorkTypeId,
    eventWorkTypeLevelId,
    readinessQuery.data?.ready,
    stageId,
    totalSnapshots,
    totalSnapshotsQuery.isFetched,
    totalSnapshotsQuery.isFetching,
    usesStageContentFormula,
  ]);
  const generateRankingMutation = useMutation({
    mutationFn: () =>
      generateCompetitionRankingSnapshot(eventId, {
        stageId: stageId!,
        eventWorkTypeId: eventWorkTypeId!,
        eventWorkTypeLevelId,
        scoreRuleId: scoreRuleId!,
        generationReason: generationReason.trim() || undefined,
        candidates: rankingDraftRows.map(
          ({
            contestantMemberId,
            totalScoreSnapshotId,
            tieKey,
            contestantStatus,
          }) => ({
            contestantMemberId,
            totalScoreSnapshotId,
            tieKey:
              contestantStatus === 'completed' ? tieKey?.trim() : undefined,
            contestantStatus,
          }),
        ),
      }),
    onSuccess: (result) => {
      message.success(`排名快照 V${result.snapshot.snapshotVersion} 已生成`);
      setRankingDraftOpen(false);
      setSelectedSnapshotId(idValue(result.snapshot.id));
      setActiveTab('trace');
      queryClient.invalidateQueries({
        queryKey: ['competition-ranking-snapshots', eventId],
      });
      queryClient.setQueryData(
        ['competition-ranking-entries', idValue(result.snapshot.id)],
        {
          items: result.entries,
          page: 1,
          pageSize: 500,
          total: result.entries.length,
        },
      );
    },
  });
  const retryTaskMutation = useMutation({
    mutationFn: (taskId: CompetitionId) =>
      retryCompetitionFormalScoringTask(taskId, '管理员人工重试'),
    onSuccess: () => {
      message.success('判分任务已重新入队');
      scoringTasksQuery.refetch();
    },
  });
  const simulationUploadMutation = useMutation({
    mutationFn: (file: File) =>
      uploadCompetitionSimulationScores(
        {
          eventId,
          stageId: stageId!,
          eventWorkTypeId: eventWorkTypeId!,
          eventWorkTypeLevelId,
          scoreRuleId: scoreRuleId!,
        },
        file,
      ),
    onSuccess: (batch) => {
      setSimulationBatchId(idValue(batch.id));
      message.success('文件已接收，正在后台预检');
    },
  });
  const simulationConfirmMutation = useMutation({
    mutationFn: (correctionReason?: string) =>
      confirmCompetitionSimulationImport(simulationBatchId!, correctionReason),
    onSuccess: () => {
      message.success('仿真成绩已确认并进入自动计算链路');
      queryClient.invalidateQueries({
        queryKey: ['competition-score-sources', eventId],
      });
      queryClient.invalidateQueries({
        queryKey: ['competition-total-score-snapshots', eventId],
      });
      queryClient.invalidateQueries({
        queryKey: ['competition-ranking-snapshots', eventId],
      });
      simulationBatchQuery.refetch();
    },
  });

  const requestSimulationConfirm = () => {
    const replacingPublished = rankingSnapshots.some(
      (snapshot) => snapshot.status === 'published',
    );
    if (!replacingPublished) {
      simulationConfirmMutation.mutate(undefined);
      return;
    }
    let reason = '';
    Modal.confirm({
      title: '填写已发布成绩更正原因',
      content: (
        <Input.TextArea
          rows={3}
          maxLength={500}
          onChange={(event) => {
            reason = event.target.value;
          }}
        />
      ),
      okText: '确认更正导入',
      onOk: async () => {
        if (!reason.trim()) {
          message.error('已发布范围更正必须填写原因');
          return Promise.reject(new Error('correction reason required'));
        }
        await simulationConfirmMutation.mutateAsync(reason.trim());
      },
    });
  };
  const subjectUploadMutation = useMutation({
    mutationFn: ({
      sourceType,
      file,
    }: {
      sourceType: 'practical' | 'simulation';
      file: File;
    }) =>
      uploadCompetitionSubjectScores(
        {
          eventId,
          stageId: stageId!,
          eventWorkTypeId: eventWorkTypeId!,
          eventWorkTypeLevelId,
          sourceType,
        },
        file,
      ),
    onSuccess: (batch, variables) => {
      setSubjectBatchIds((current) => ({
        ...current,
        [variables.sourceType]: idValue(batch.id),
      }));
      message.success('文件已接收，正在校验；通过后会写入成绩并计算综合分');
    },
  });
  const subjectConfirmMutation = useMutation({
    mutationFn: ({
      batchId,
      correctionReason,
    }: {
      batchId: CompetitionId;
      correctionReason?: string;
    }) => confirmCompetitionSubjectScoreImport(batchId, correctionReason),
    onSuccess: () => {
      message.success('成绩已写入，正在按赛段内容权重计算综合成绩');
      queryClient.invalidateQueries({
        queryKey: ['competition-score-sources', eventId],
      });
      queryClient.invalidateQueries({
        queryKey: ['competition-total-score-snapshots', eventId],
      });
      queryClient.invalidateQueries({
        queryKey: ['competition-ranking-snapshots', eventId],
      });
    },
  });
  const requestSubjectConfirm = (batchId: CompetitionId) => {
    const replacingPublished = rankingSnapshots.some(
      (snapshot) => snapshot.status === 'published',
    );
    if (!replacingPublished) {
      subjectConfirmMutation.mutate({ batchId });
      return;
    }
    let reason = '';
    Modal.confirm({
      title: '填写已发布成绩更正原因',
      content: (
        <Input.TextArea
          rows={3}
          maxLength={500}
          onChange={(event) => {
            reason = event.target.value;
          }}
        />
      ),
      okText: '确认更正导入',
      onOk: async () => {
        if (!reason.trim()) {
          message.error('已发布范围更正必须填写原因');
          return Promise.reject(new Error('correction reason required'));
        }
        await subjectConfirmMutation.mutateAsync({
          batchId,
          correctionReason: reason.trim(),
        });
      },
    });
  };
  const autoConfirmedBatches = useRef(new Set<string>());
  const completedImportBatches = useRef(new Set<string>());
  useEffect(() => {
    const batches = [
      practicalImportQuery.data,
      simulationIdCardImportQuery.data,
    ];
    for (const batch of batches) {
      if (!batch?.id) continue;
      const batchKey = idValue(batch.id);
      const canCommit =
        batch.batchStatus === 'ready' ||
        (batch.batchStatus === 'failed' &&
          batch.errorRows === 0 &&
          batch.confirmedRows < batch.validRows);
      if (canCommit && !autoConfirmedBatches.current.has(batchKey)) {
        const replacingPublished = rankingSnapshots.some(
          (snapshot) => snapshot.status === 'published',
        );
        if (replacingPublished) continue;
        autoConfirmedBatches.current.add(batchKey);
        subjectConfirmMutation.mutate({ batchId: batch.id });
      }
      if (
        batch.batchStatus === 'completed' &&
        !completedImportBatches.current.has(batchKey)
      ) {
        completedImportBatches.current.add(batchKey);
        void queryClient.invalidateQueries({
          queryKey: ['competition-score-sources', eventId],
        });
        void queryClient.invalidateQueries({
          queryKey: ['competition-total-score-snapshots', eventId],
        });
        void queryClient.invalidateQueries({
          queryKey: ['competition-ranking-snapshots', eventId],
        });
      }
    }
  }, [
    eventId,
    practicalImportQuery.data,
    queryClient,
    rankingSnapshots,
    simulationIdCardImportQuery.data,
    subjectConfirmMutation,
  ]);
  const downloadSubjectTemplate = async (
    sourceType: 'practical' | 'simulation',
  ) => {
    try {
      downloadBlob(
        await downloadCompetitionSubjectScoreTemplate(sourceType),
        sourceType === 'practical'
          ? '实操成绩导入模板.xlsx'
          : '仿真成绩导入模板.xlsx',
      );
    } catch {
      message.error('导入模板下载失败');
    }
  };
  const requestRankingPublish = (snapshotId: CompetitionId) => {
    const replacingPublished = rankingSnapshots.some(
      (snapshot) =>
        snapshot.status === 'published' &&
        idValue(snapshot.id) !== idValue(snapshotId),
    );
    if (!replacingPublished) {
      publishRankingMutation.mutate({ snapshotId });
      return;
    }
    let reason = '';
    Modal.confirm({
      title: '填写排名更正发布原因',
      content: (
        <Input.TextArea
          rows={3}
          maxLength={500}
          onChange={(event) => {
            reason = event.target.value;
          }}
        />
      ),
      okText: '发布并替代旧版本',
      onOk: async () => {
        if (!reason.trim()) {
          message.error('更正发布必须填写原因');
          return Promise.reject(new Error('correction reason required'));
        }
        await publishRankingMutation.mutateAsync({
          snapshotId,
          correctionReason: reason.trim(),
        });
      },
    });
  };
  const openRankingDraft = () => {
    const snapshots = totalSnapshotsQuery.data?.items || [];
    const byId = new Map(
      snapshots.map((snapshot) => [idValue(snapshot.id), snapshot]),
    );
    setRankingDraftRows(
      buildRankingCandidates(snapshots).map((candidate) => ({
        ...candidate,
        key: `completed-${idValue(candidate.contestantMemberId)}`,
        totalScoreSnapshot: byId.get(idValue(candidate.totalScoreSnapshotId)),
      })),
    );
    setGenerationReason('');
    setAbsentContestantId('');
    setRankingReviewed(false);
    setRankingDraftOpen(true);
  };
  const addAbsentContestant = () => {
    const contestantId = absentContestantId.trim();
    if (!contestantId) return;
    if (
      rankingDraftRows.some(
        (row) => idValue(row.contestantMemberId) === contestantId,
      )
    ) {
      message.warning('该选手已在候选列表中');
      return;
    }
    setRankingDraftRows((rows) => [
      ...rows,
      {
        key: `absent-${contestantId}`,
        contestantMemberId: contestantId,
        contestantStatus: 'absent',
      },
    ]);
    setAbsentContestantId('');
    setRankingReviewed(false);
  };
  const moveRankingRow = (index: number, offset: number) => {
    const target = index + offset;
    if (target < 0 || target >= rankingDraftRows.length) return;
    setRankingDraftRows((rows) => {
      const next = [...rows];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
    setRankingReviewed(false);
  };
  const completedTieKeysValid = rankingDraftRows.every(
    (row) => row.contestantStatus === 'absent' || Boolean(row.tieKey?.trim()),
  );
  const exportOverview = (rowsToExport = filteredRows) => {
    const header = [
      '排名',
      '选手姓名',
      '参赛编号',
      '代表队',
      '工种',
      '等级',
      ...subjects.map((subject) => subject.label),
      '综合成绩',
      '状态',
    ];
    const rows = rowsToExport.map((row) => [
      row.rank ?? '',
      row.candidateName,
      row.candidateNo,
      row.teamName,
      row.workTypeName,
      row.levelName,
      ...subjects.map((subject) => {
        const cell = row.subjectCells[subject.type];
        return cell.status === 'absent'
          ? '缺考'
          : cell.value == null
            ? statusMeta[cell.status].label
            : scoreValue(cell.value);
      }),
      row.total?.confirmationStatus === 'confirmed'
        ? scoreValue(row.total.totalScore)
        : statusMeta[row.overallStatus].label,
      statusMeta[row.overallStatus].label,
    ]);
    const csv = [header, ...rows]
      .map((line) => line.map(csvCell).join(','))
      .join('\r\n');
    downloadBlob(
      new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }),
      `成绩汇总-${eventId}.csv`,
    );
  };
  const refreshAll = () => {
    // refetch() 会绕过 enabled 门：scope 未就绪时变量为 undefined，axios 丢弃后变成裸请求（后端 500）。
    // 未就绪时整体拦截并提示，与查询自动触发的门控保持同一口径
    if (!scopeReady) {
      message.warning('当前赛事尚未选定赛段、工种或参赛等级，请先在上方完成选择再刷新');
      return;
    }
    void scoreSourcesQuery.refetch();
    void totalSnapshotsQuery.refetch();
    if (contextReady) void rankingSnapshotsQuery.refetch();
    void playersQuery.refetch();
    void scoringTasksQuery.refetch();
  };

  const sourceColumns: ProColumns<CompetitionScoreSource>[] = [
    {
      title: '选手',
      width: 160,
      fixed: 'left',
      render: (_, row) => (
        <Button
          type="link"
          className="competition-score-table-link"
          onClick={() => setSelectedMemberId(idValue(row.contestantMemberId))}
        >
          {overviewRows.find(
            (item) =>
              idValue(item.contestantMemberId) ===
              idValue(row.contestantMemberId),
          )?.candidateName || idValue(row.contestantMemberId)}
        </Button>
      ),
    },
    {
      title: '科目',
      dataIndex: 'sourceType',
      width: 110,
      render: (_, row) => (
        <Tag color={sourceMeta[row.sourceType].color}>
          {sourceMeta[row.sourceType].shortLabel}
        </Tag>
      ),
    },
    {
      title: '原始分',
      dataIndex: 'rawScore',
      width: 100,
      render: (_, row) => (
        <Typography.Text strong>{scoreValue(row.rawScore)}</Typography.Text>
      ),
    },
    {
      title: '状态',
      dataIndex: 'confirmationStatus',
      width: 105,
      render: (_, row) => confirmationTag(row.confirmationStatus),
    },
    {
      title: '修订',
      dataIndex: 'revisionNo',
      width: 72,
      render: (_, row) => `V${row.revisionNo}`,
    },
    {
      title: '当前有效',
      dataIndex: 'currentFlag',
      width: 95,
      render: (_, row) =>
        row.currentFlag === 1 ? (
          <Tag color="success">当前</Tag>
        ) : (
          <Tag>历史</Tag>
        ),
    },
    {
      title: '来源系统',
      dataIndex: 'sourceSystem',
      width: 145,
      ellipsis: true,
    },
    {
      title: '来源流水',
      dataIndex: 'sourceReference',
      width: 190,
      ellipsis: true,
      copyable: true,
    },
    {
      title: '取得时间',
      dataIndex: 'achievedAt',
      width: 165,
      render: (_, row) => formatDate(row.achievedAt),
    },
    {
      title: '确认时间',
      dataIndex: 'confirmedAt',
      width: 165,
      render: (_, row) => formatDate(row.confirmedAt),
    },
    {
      title: '操作人',
      dataIndex: 'createdBy',
      width: 110,
      render: (_, row) => row.createdBy || row.confirmedBy || '系统',
    },
  ];
  const totalColumns: ProColumns<CompetitionTotalScoreSnapshot>[] = [
    {
      title: '选手',
      width: 160,
      fixed: 'left',
      render: (_, row) => (
        <Button
          type="link"
          className="competition-score-table-link"
          onClick={() => setSelectedMemberId(idValue(row.contestantMemberId))}
        >
          {overviewRows.find(
            (item) =>
              idValue(item.contestantMemberId) ===
              idValue(row.contestantMemberId),
          )?.candidateName || idValue(row.contestantMemberId)}
        </Button>
      ),
    },
    ...subjects.map((subject) => ({
      title: subject.shortLabel,
      dataIndex: sourceMeta[subject.type].scoreField,
      width: 95,
      render: (_: unknown, row: CompetitionTotalScoreSnapshot) =>
        scoreValue(
          row[sourceMeta[subject.type].scoreField] as
            number | string | null | undefined,
        ),
    })),
    ...(subjects.some((subject) => subject.type === 'simulation') &&
    subjects.some((subject) => subject.type === 'practical')
      ? [
          {
            title: '仿真实操组合',
            dataIndex: 'simulationPracticalScore',
            width: 120,
            render: (_: unknown, row: CompetitionTotalScoreSnapshot) =>
              scoreValue(row.simulationPracticalScore),
          },
        ]
      : []),
    {
      title: '综合成绩',
      dataIndex: 'totalScore',
      width: 110,
      render: (_: unknown, row: CompetitionTotalScoreSnapshot) => (
        <Typography.Text strong>
          {row.confirmationStatus === 'confirmed'
            ? scoreValue(row.totalScore)
            : '—'}
        </Typography.Text>
      ),
    },
    {
      title: '状态',
      dataIndex: 'confirmationStatus',
      width: 100,
      render: (_, row) => confirmationTag(row.confirmationStatus),
    },
    {
      title: '版本',
      dataIndex: 'calculationVersion',
      width: 70,
      render: (_, row) => `V${row.calculationVersion}`,
    },
    {
      title: '计算时间',
      dataIndex: 'calculatedAt',
      width: 165,
      render: (_, row) => formatDate(row.calculatedAt),
    },
  ];
  const snapshotColumns: ProColumns<CompetitionRankingSnapshot>[] = [
    {
      title: '版本',
      dataIndex: 'snapshotVersion',
      width: 80,
      render: (_, row) => `V${row.snapshotVersion}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (_, row) => (
        <Tag
          color={
            row.status === 'published'
              ? 'success'
              : row.status === 'confirmed'
                ? 'processing'
                : row.status === 'superseded'
                  ? 'default'
                  : 'warning'
          }
        >
          {
            (
              {
                draft: '草稿',
                confirmed: '已确认',
                published: '已发布',
                superseded: '已替代',
              } as Record<string, string>
            )[row.status]
          }
        </Tag>
      ),
    },
    { title: '生成说明', dataIndex: 'generationReason', ellipsis: true },
    {
      title: '生成时间',
      dataIndex: 'generatedAt',
      width: 165,
      render: (_, row) => formatDate(row.generatedAt),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 260,
      render: (_, row) => (
        <Space size={2}>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => {
              setSelectedSnapshotId(idValue(row.id));
              setActiveTab('trace');
            }}
          >
            查看榜单
          </Button>
          {row.status === 'draft' && canReviewRanking && (
            <Popconfirm
              title="确认成绩输入和同分排序无误？"
              onConfirm={() => confirmRankingMutation.mutate(row.id)}
            >
              <Button type="link">复核确认</Button>
            </Popconfirm>
          )}
          {row.status === 'confirmed' && canPublishRanking && (
            <Popconfirm
              title="发布后选手将看到本人结果，确认发布？"
              onConfirm={() => requestRankingPublish(row.id)}
            >
              <Button type="link">发布</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];
  const entryColumns: ProColumns<CompetitionRankingEntry>[] = [
    {
      title: '名次',
      dataIndex: 'displayRank',
      width: 75,
      render: (_, row) => row.displayRank ?? '—',
    },
    {
      title: '选手',
      width: 160,
      render: (_, row) =>
        overviewRows.find(
          (item) =>
            idValue(item.contestantMemberId) ===
            idValue(row.contestantMemberId),
        )?.candidateName || idValue(row.contestantMemberId),
    },
    {
      title: '综合成绩',
      dataIndex: 'totalScore',
      width: 110,
      render: (_, row) =>
        row.contestantStatus === 'absent' ? '—' : scoreValue(row.totalScore),
    },
    {
      title: '并列',
      dataIndex: 'tiedFlag',
      width: 75,
      render: (_, row) =>
        row.tiedFlag === 1 ? <Tag color="processing">并列</Tag> : '—',
    },
    {
      title: '选手状态',
      dataIndex: 'contestantStatus',
      width: 100,
      render: (_, row) =>
        row.contestantStatus === 'absent'
          ? statusTag('absent')
          : statusTag('confirmed'),
    },
    {
      title: '排序键摘要',
      dataIndex: 'tieKey',
      ellipsis: true,
      copyable: true,
    },
  ];

  if (!canViewEvent || (!canViewScores && !canViewRanking))
    return (
      <Result
        status="403"
        title="暂无成绩与排名查看权限"
        subTitle="服务端权限仍是最终裁决，请联系管理员配置赛事成绩权限。"
      />
    );
  if (!canReadDimensions)
    return (
      <Result
        status="403"
        title="赛事上下文权限不完整"
        subTitle="查看成绩还需要赛段、赛事工种和成绩规则查看权限。"
      />
    );
  if (eventQuery.isLoading || stagesQuery.isLoading || workTypesQuery.isLoading)
    return (
      <div className="competition-score-loading">
        <Progress type="circle" percent={65} showInfo={false} />
        <Typography.Text type="secondary">正在载入成绩工作台…</Typography.Text>
      </div>
    );
  if (!eventQuery.data)
    return (
      <Result
        status="404"
        title="赛事不存在或无法访问"
        extra={
          <Button onClick={() => onNavigate('/competition/events')}>
            返回赛事列表
          </Button>
        }
      />
    );

  const contextLabel = `${stagesQuery.data?.items.find((stage) => idValue(stage.id) === stageId)?.name || '未选择赛段'} · ${workTypesQuery.data?.find((item) => idValue(item.id) === eventWorkTypeId)?.displayName || '未选择工种'}${eventWorkTypeLevelId ? ` · ${levelsQuery.data?.find((level) => idValue(level.id) === eventWorkTypeLevelId)?.levelName || ''}` : ''}`;
  const formulaParts = subjects
    .map(
      (subject) =>
        `${subject.shortLabel}${subject.weight != null ? ` ${subject.weight}%` : ''}`,
    )
    .filter(Boolean);
  const canPublish =
    statistics.participantCount > 0 &&
    statistics.complete >= statistics.eligibleCount &&
    activeSnapshot?.status === 'confirmed';
  const answerSheetQuestions = answerSheetQuery.data?.questions || [];
  const activeAnswerQuestion =
    answerSheetQuestions.find(
      (question) =>
        idValue(question.questionSnapshotId) === selectedAnswerQuestionId,
    ) || answerSheetQuestions[0];
  const answerSheetCorrectCount = answerSheetQuestions.filter(
    (question) => answerSheetStatus(question) === 'correct',
  ).length;
  const answerSheetWrongCount = answerSheetQuestions.filter(
    (question) => answerSheetStatus(question) === 'wrong',
  ).length;
  const answerSheetUnansweredCount = answerSheetQuestions.filter(
    (question) => answerSheetStatus(question) === 'unanswered',
  ).length;

  const overviewColumns: TableColumnsType<ScoreOverviewRow> = [
    {
      title: '排名',
      key: 'rank',
      width: 70,
      fixed: 'left',
      render: (_, row) =>
        row.rank ? (
          <span
            className={`competition-score-rank competition-score-rank--${row.rank <= 3 ? row.rank : 'other'}`}
          >
            {row.rank}
          </span>
        ) : (
          <span className="competition-score-muted">—</span>
        ),
    },
    {
      title: '选手',
      key: 'candidate',
      width: 190,
      fixed: 'left',
      render: (_, row) => (
        <button
          type="button"
          className="competition-score-player"
          onClick={() => setSelectedMemberId(row.key)}
        >
          <Avatar size={30} className="competition-score-player__avatar">
            {row.candidateName.slice(0, 1)}
          </Avatar>
          <span>
            <strong>{row.candidateName}</strong>
            <small>
              {row.candidateNo === '-'
                ? `成员 ${row.contestantMemberId}`
                : row.candidateNo}
            </small>
          </span>
        </button>
      ),
    },
    ...subjects.map((subject) => ({
      title: (
        <span
          className="competition-score-column-title"
          style={{ color: subject.color }}
        >
          <span>{subject.icon}</span>
          {subject.shortLabel}
        </span>
      ),
      key: subject.type,
      width: 130,
      render: (_: unknown, row: ScoreOverviewRow) => (
        <ScoreCellView
          cell={row.subjectCells[subject.type]}
          onClick={() => {
            setSelectedMemberId(row.key);
            setWorkbenchSubject(subject.type);
            setActiveTab('workbench');
          }}
        />
      ),
    })),
    {
      title: '综合成绩',
      key: 'total',
      width: 130,
      className: 'competition-score-total-column',
      defaultSortOrder: 'descend',
      sorter: (left, right) =>
        compareScoreValues(
          left.total?.confirmationStatus === 'confirmed'
            ? left.total.totalScore
            : undefined,
          right.total?.confirmationStatus === 'confirmed'
            ? right.total.totalScore
            : undefined,
        ),
      render: (_: unknown, row: ScoreOverviewRow) => (
        <div className="competition-score-total-cell">
          <strong>
            {row.total?.confirmationStatus === 'confirmed'
              ? scoreValue(row.total.totalScore)
              : '—'}
          </strong>
          <small>
            {row.total?.confirmationStatus === 'confirmed'
              ? '已计算'
              : statusMeta[row.overallStatus].label}
          </small>
        </div>
      ),
    },
    {
      title: '代表队',
      dataIndex: 'teamName',
      width: 150,
      ellipsis: true,
      render: (value) => (
        <span className="competition-score-secondary">
          <TeamOutlined /> {value}
        </span>
      ),
    },
    { title: '工种', dataIndex: 'workTypeName', width: 130, ellipsis: true },
    { title: '等级', dataIndex: 'levelName', width: 110, ellipsis: true },
    {
      title: '状态',
      key: 'status',
      width: 112,
      render: (_: unknown, row: ScoreOverviewRow) =>
        statusTag(row.overallStatus),
    },
    {
      title: '操作',
      key: 'actions',
      width: 92,
      fixed: 'right',
      render: (_: unknown, row: ScoreOverviewRow) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => setSelectedMemberId(row.key)}
        >
          详情
        </Button>
      ),
    },
  ];

  const exportOneAnswerSheet = async (
    candidateExamId: CompetitionId,
    candidateName?: string | null,
    candidateNo?: string | null,
  ) => {
    const key = idValue(candidateExamId);
    setExportingAnswerSheetId(key);
    try {
      const blob = await downloadCompetitionFormalCandidateAnswerSheet(
        candidateExamId,
      );
      downloadBlob(blob, answerSheetDocxName(candidateName, candidateNo));
    } catch {
      // 错误提示由 apiClient 拦截器统一弹出
    } finally {
      setExportingAnswerSheetId((current) => (current === key ? undefined : current));
    }
  };

  const exportTheoryAnswerSheets = async (rows: ScoreOverviewRow[]) => {
    const ready: Array<{ id: CompetitionId; name: string; no: string }> = [];
    const seen = new Set<string>();
    rows.forEach((row) => {
      const task = latestFormalScoringTask(scoringTasks, row.key);
      const examId = task?.candidateExamId;
      if (!examId || task?.taskStatus !== 'completed') return;
      const key = idValue(examId);
      if (seen.has(key)) return;
      seen.add(key);
      ready.push({ id: examId, name: row.candidateName, no: row.candidateNo });
    });
    if (ready.length === 0) {
      message.warning('当前没有已完成判分的理论答题卡');
      return;
    }
    if (ready.length > 200) {
      message.warning('一次最多导出 200 份答题卡，请缩小筛选范围');
      return;
    }
    setExportingAnswerSheets(true);
    try {
      if (ready.length === 1) {
        const only = ready[0];
        const blob = await downloadCompetitionFormalCandidateAnswerSheet(only.id);
        downloadBlob(blob, answerSheetDocxName(only.name, only.no));
      } else {
        const blob = await downloadCompetitionFormalCandidateAnswerSheets(
          ready.map((item) => item.id),
        );
        downloadBlob(blob, '理论考试答题卡.zip');
      }
      const skipped = rows.length - ready.length;
      message.success(
        skipped > 0
          ? `已导出 ${ready.length} 份答题卡，${skipped} 人尚未完成判分`
          : `已导出 ${ready.length} 份答题卡`,
      );
    } catch {
      // 错误提示由 apiClient 拦截器统一弹出
    } finally {
      setExportingAnswerSheets(false);
    }
  };

  const workbenchColumns: TableColumnsType<ScoreOverviewRow> = (() => {
    const activeType = workbenchSubject || subjects[0]?.type;
    return [
      {
        title: '选手',
        key: 'candidate',
        width: 190,
        fixed: 'left',
        render: (_value, row) => (
          <button
            type="button"
            className="competition-score-player"
            onClick={() => setSelectedMemberId(row.key)}
          >
            <Avatar size={30} className="competition-score-player__avatar">
              {row.candidateName.slice(0, 1)}
            </Avatar>
            <span>
              <strong>{row.candidateName}</strong>
              <small>{row.candidateNo}</small>
            </span>
          </button>
        ),
      },
      { title: '代表队', dataIndex: 'teamName', width: 150, ellipsis: true },
      {
        title: '成绩',
        width: 110,
        defaultSortOrder: 'descend',
        sorter: (left, right) =>
          compareScoreValues(
            activeType ? left.subjectCells[activeType]?.value : undefined,
            activeType ? right.subjectCells[activeType]?.value : undefined,
          ),
        render: (_value, row) =>
          activeType ? (
            <ScoreCellView
              cell={row.subjectCells[activeType]}
              onClick={() => setSelectedMemberId(row.key)}
            />
          ) : (
            '—'
          ),
      },
      {
        title: '来源系统',
        width: 160,
        render: (_value, row) =>
          activeType
            ? row.subjectCells[activeType]?.source?.sourceSystem ||
              (activeType === 'theory' ? '正式理论考试' : '待接入')
            : '—',
      },
      {
        title: '更新时间',
        width: 160,
        render: (_value, row) =>
          activeType
            ? formatDate(
                row.subjectCells[activeType]?.source?.confirmedAt ||
                  row.subjectCells[activeType]?.source?.achievedAt,
              )
            : '—',
      },
      {
        title: '操作',
        width: activeType === 'theory' ? 180 : 90,
        render: (_value, row) => {
          const task = latestFormalScoringTask(scoringTasks, row.key);
          const examId = task?.candidateExamId;
          return (
            <Space size={0}>
              <Button type="link" onClick={() => setSelectedMemberId(row.key)}>
                详情
              </Button>
              {activeType === 'theory' && (
                <Button
                  type="link"
                  icon={<DownloadOutlined />}
                  disabled={!examId || task?.taskStatus !== 'completed'}
                  loading={
                    Boolean(examId) &&
                    exportingAnswerSheetId === idValue(examId)
                  }
                  onClick={() => {
                    if (!examId) return;
                    void exportOneAnswerSheet(
                      examId,
                      row.candidateName,
                      row.candidateNo,
                    );
                  }}
                >
                  导出答题卡
                </Button>
              )}
            </Space>
          );
        },
      },
    ];
  })();

  const workbenchItems = subjects.map((subject) => {
    const subjectRows = filteredRows;
    const confirmedCount = subjectRows.filter(
      (row) => row.subjectCells[subject.type]?.status === 'confirmed',
    ).length;
    const reviewCount = subjectRows.filter((row) =>
      ['pending_review', 'abnormal', 'arbitrating'].includes(
        row.subjectCells[subject.type]?.status || '',
      ),
    ).length;
    return {
      key: subject.type,
      label: (
        <span
          className="competition-score-tab-label"
          style={{ color: subject.color }}
        >
          {subject.icon}
          {subject.shortLabel}
          <Badge count={reviewCount || undefined} size="small" />
        </span>
      ),
      children: (
        <div className="competition-score-subject-panel">
          <Row gutter={[12, 12]} className="competition-score-subject-stats">
            <Col xs={24} sm={8}>
              <Card size="small">
                <Statistic
                  title="已确认"
                  value={confirmedCount}
                  suffix={`/ ${subjectRows.length}`}
                  valueStyle={{ color: '#1677ff' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card size="small">
                <Statistic
                  title="待处理"
                  value={Math.max(0, subjectRows.length - confirmedCount)}
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card size="small">
                <Statistic
                  title="异常 / 待审核"
                  value={reviewCount}
                  valueStyle={{ color: reviewCount ? '#d4380d' : '#52c41a' }}
                />
              </Card>
            </Col>
          </Row>
          {subject.type === 'simulation' && usesStageContentFormula && (
            <Alert
              className="competition-score-subject-alert"
              type="info"
              showIcon
              message="仿真成绩按身份证号导入"
              description="模板列为姓名、身份证号、仿真成绩。校验通过后会自动写入，并按赛段内容权重计算综合成绩。"
            />
          )}
          {subject.type === 'simulation' && !usesStageContentFormula && (
            <Alert
              className="competition-score-subject-alert"
              type="info"
              showIcon
              message="仿真成绩支持批量导入"
              description="文件先预检，确认后才写入统一成绩来源并触发综合计算。"
            />
          )}
          {subject.type === 'practical' && usesStageContentFormula && (
            <Alert
              className="competition-score-subject-alert"
              type="info"
              showIcon
              message="实操成绩按身份证号导入"
              description="模板列为姓名、身份证号、实操成绩。校验通过后会自动写入，并按赛段内容权重计算综合成绩。"
            />
          )}
          {subject.type === 'practical' && !usesStageContentFormula && (
            <Alert
              className="competition-score-subject-alert"
              type="warning"
              showIcon
              message="实操成绩由裁判评分链路提供"
              description="本页展示结果与复核状态，裁判评分继续沿用现有实操工作台。"
            />
          )}
          {subject.type === 'theory' && (
            <Alert
              className="competition-score-subject-alert"
              type="success"
              showIcon
              message="理论成绩自动回写"
              description="理论成绩来自正式考试自动评分。已完成判分的选手可导出答题卡，每人一份 Word 文档，文件名使用选手姓名。"
              action={
                <Button
                  icon={<DownloadOutlined />}
                  loading={exportingAnswerSheets}
                  onClick={() => void exportTheoryAnswerSheets(subjectRows)}
                >
                  导出答题卡
                </Button>
              }
            />
          )}
          <Table<ScoreOverviewRow>
            rowKey="key"
            size="middle"
            dataSource={subjectRows}
            loading={
              scoreSourcesQuery.isFetching || totalSnapshotsQuery.isFetching
            }
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 人`,
            }}
            scroll={{ x: 900 }}
            columns={workbenchColumns}
          />
        </div>
      ),
    };
  });

  const renderSubjectImportAlert = (
    sourceType: 'practical' | 'simulation',
    batch?: CompetitionSubjectScoreImportBatch,
  ) =>
    batch ? (
      <Alert
        className="competition-score-import-alert"
        type={
          batch.batchStatus === 'invalid' || batch.batchStatus === 'failed'
            ? 'error'
            : 'info'
        }
        showIcon
        message={`${sourceMeta[sourceType].shortLabel}成绩：${
          {
            parsing: '正在校验身份证号',
            ready: '校验通过，正在写入成绩',
            invalid: '校验未通过，尚未写入成绩',
            confirming: '正在写入成绩并计算综合分',
            completed: '已写入成绩，综合分按已齐备的科目计算',
            failed: '导入失败',
          }[batch.batchStatus]
        }`}
        description={`总行数 ${batch.totalRows}，有效 ${batch.validRows}，错误 ${batch.errorRows}，已确认 ${batch.confirmedRows}${batch.errorMessage ? `；${batch.errorMessage}` : ''}`}
        action={
          <Space>
            {batch.errorRows > 0 && (
              <Button
                icon={<DownloadOutlined />}
                onClick={async () => {
                  try {
                    downloadBlob(
                      await downloadCompetitionSubjectScoreImportErrors(batch.id),
                      `${sourceMeta[sourceType].shortLabel}成绩导入错误明细.xlsx`,
                    );
                  } catch {
                    message.error('错误明细下载失败');
                  }
                }}
              >
                下载错误明细
              </Button>
            )}
            {(batch.batchStatus === 'ready' ||
              (batch.batchStatus === 'failed' &&
                batch.errorRows === 0 &&
                batch.confirmedRows < batch.validRows)) && (
              <Button
                type="primary"
                loading={subjectConfirmMutation.isPending}
                onClick={() => requestSubjectConfirm(batch.id)}
              >
                确认导入
              </Button>
            )}
          </Space>
        }
      />
    ) : null;

  const importableSubjects = (['practical', 'simulation'] as const).filter(
    (sourceType) => subjects.some((subject) => subject.type === sourceType),
  );
  const activeImportSubject = importableSubjects.includes(importSubject)
    ? importSubject
    : importableSubjects[0];
  const activeImportBatch =
    activeImportSubject === 'practical'
      ? practicalImportQuery.data
      : activeImportSubject === 'simulation'
        ? simulationIdCardImportQuery.data
        : undefined;

  const content = (
    <div className="competition-score-page">
      <Card bordered={false} className="competition-score-scope-card">
        <div className="competition-score-scope-card__head">
          <div>
            <Typography.Text className="competition-score-eyebrow">
              SCORE CONTROL CENTER
            </Typography.Text>
            <Typography.Title level={3}>成绩管理</Typography.Title>
            <Typography.Text type="secondary">
              {contextLabel} · 科目、成绩状态与排名统一视图
            </Typography.Text>
          </div>
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={refreshAll}>
              刷新数据
            </Button>
            <Button
              icon={<DownloadOutlined />}
              onClick={() => exportOverview()}
              disabled={!filteredRows.length}
            >
              导出成绩
            </Button>
            <Button
              icon={<DownloadOutlined />}
              onClick={() =>
                exportOverview(
                  filteredRows.filter((row) =>
                    selectedRowKeys.includes(row.key),
                  ),
                )
              }
              disabled={!selectedRowKeys.length}
            >
              导出选中 ({selectedRowKeys.length})
            </Button>
            <Button
              icon={<CalculatorOutlined />}
              onClick={() => setActiveTab('summary')}
            >
              汇总 / 发布
            </Button>
            {canCalculate && !theoryOnly && (
              <Button
                icon={<CalculatorOutlined />}
                loading={calculateMutation.isPending}
                disabled={
                  !contextReady ||
                  calculationReadyContestants.length === 0 ||
                  readinessQuery.data?.ready === false
                }
                onClick={() => calculateMutation.mutate()}
              >
                计算总分 ({calculationReadyContestants.length})
              </Button>
            )}
            {canImportSimulation &&
              !usesStageContentFormula &&
              Boolean(scoreRuleId) &&
              subjects.some((subject) => subject.type === 'simulation') && (
                <Upload
                  accept=".xlsx,.xls"
                  showUploadList={false}
                  beforeUpload={(file) => {
                    simulationUploadMutation.mutate(file);
                    return false;
                  }}
                >
                  <Button
                    type="primary"
                    icon={<UploadOutlined />}
                    loading={simulationUploadMutation.isPending}
                  >
                    导入仿真
                  </Button>
                </Upload>
              )}
            {usesStageContentFormula &&
              canImportSimulation &&
              contextReady &&
              importableSubjects.length > 0 &&
              activeImportSubject && (
                <Space size={8}>
                  <Segmented
                    value={activeImportSubject}
                    options={importableSubjects.map((sourceType) => ({
                      value: sourceType,
                      label: sourceMeta[sourceType].shortLabel,
                    }))}
                    onChange={(value) => {
                      const sourceType = value as 'practical' | 'simulation';
                      setImportSubject(sourceType);
                      setWorkbenchSubject(sourceType);
                    }}
                  />
                  <Button
                    icon={<DownloadOutlined />}
                    onClick={() => void downloadSubjectTemplate(activeImportSubject)}
                  >
                    下载模板
                  </Button>
                  <Upload
                    accept=".xlsx,.xls"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      subjectUploadMutation.mutate({
                        sourceType: activeImportSubject,
                        file,
                      });
                      return false;
                    }}
                  >
                    <Button
                      type="primary"
                      icon={<UploadOutlined />}
                      loading={subjectUploadMutation.isPending}
                    >
                      导入成绩
                    </Button>
                  </Upload>
                </Space>
              )}
          </Space>
        </div>
        <div className="competition-score-scope-selectors">
          <Select
            value={stageId}
            placeholder="选择赛段"
            options={(stagesQuery.data?.items || []).map((stage) => ({
              value: idValue(stage.id),
              label: stage.name,
            }))}
            onChange={(value) => {
              setStageId(value);
              setScoreRuleId(undefined);
              setSelectedSnapshotId(undefined);
              setSelectedMemberId(undefined);
              setSubjectBatchIds({});
            }}
          />
          <Select
            value={eventWorkTypeId}
            placeholder="选择工种"
            options={(workTypesQuery.data || []).map((workType) => ({
              value: idValue(workType.id),
              label:
                workType.displayName ||
                workType.workTypeName ||
                idValue(workType.id),
            }))}
            onChange={(value) => {
              setEventWorkTypeId(value);
              setEventWorkTypeLevelId(undefined);
              setScoreRuleId(undefined);
              setSelectedSnapshotId(undefined);
              setSelectedMemberId(undefined);
              setSubjectBatchIds({});
            }}
          />
          {levelRequired && (
            <Select
              value={eventWorkTypeLevelId}
              placeholder="选择等级"
              loading={levelsQuery.isLoading}
              options={(levelsQuery.data || []).map((level) => ({
                value: idValue(level.id),
                label: level.levelName || level.levelCode,
              }))}
              onChange={(value) => {
                setEventWorkTypeLevelId(value);
                setScoreRuleId(undefined);
                setSelectedSnapshotId(undefined);
                setSubjectBatchIds({});
              }}
            />
          )}
          {usesStageContentFormula ? (
            <Tag color="blue">
              {theoryOnly ? '理论题目分值直接计分' : '赛段内容权重计分'}
            </Tag>
          ) : (
            <Select
              value={scoreRuleId}
              placeholder="选择已发布规则"
              loading={rulesQuery.isLoading}
              options={(rulesQuery.data || [])
                .filter((rule) => rule.status === 'published')
                .map((rule) => ({
                  value: idValue(rule.id),
                  label: `V${rule.ruleVersion} · ${rule.scoringSystem}`,
                }))}
              onChange={(value) => {
                setScoreRuleId(value);
                setSelectedSnapshotId(undefined);
              }}
            />
          )}
        </div>
        <div className="competition-score-filter-row">
          <Input
            prefix={<SearchOutlined />}
            allowClear
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索选手姓名、编号或成员 ID"
          />
          <Select
            allowClear
            value={orgFilter}
            options={orgOptions}
            onChange={setOrgFilter}
            placeholder="代表队"
          />
          <Select
            allowClear
            value={statusFilter}
            options={Object.entries(statusMeta).map(([value, meta]) => ({
              value,
              label: meta.label,
            }))}
            onChange={(value) =>
              setStatusFilter(value as ScoreStatus | undefined)
            }
            placeholder="成绩状态"
          />
          <Button
            icon={<FilterOutlined />}
            onClick={() => {
              setKeyword('');
              setOrgFilter(undefined);
              setStatusFilter(undefined);
            }}
          >
            清除筛选
          </Button>
        </div>
      </Card>
      {activeImportSubject &&
        renderSubjectImportAlert(activeImportSubject, activeImportBatch)}
      {canReadReadiness && readinessQuery.data && (
        <Alert
          className="competition-score-readiness"
          type={readinessQuery.data.ready ? 'success' : 'warning'}
          showIcon
          message={
            readinessQuery.data.ready ? '成绩域已就绪' : '成绩域尚未就绪'
          }
          description={
            readinessQuery.data.checks
              .filter((check) => !check.passed)
              .map((check) => check.message)
              .join('；') ||
            (theoryOnly
              ? '理论赛段按题目快照分值直接计分，无需配置计分规则。'
              : usesStageContentFormula
                ? '综合成绩按赛段编排中的内容权重计算。'
                : '赛段、工种、资格选手和已发布规则版本均已通过校验。')
          }
        />
      )}
      {stageWorkspaceQuery.isError && (
        <Alert
          className="competition-score-readiness"
          type="info"
          showIcon
          message="未读取到赛段内容配置"
          description="当前按已有成绩来源推断科目；建议授予赛段执行方案查看权限，以确保科目显示与赛段配置一致。"
        />
      )}
      <Tabs
        className="competition-score-main-tabs"
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'overview',
            label: (
              <span>
                <TrophyFilled /> 综合成绩
              </span>
            ),
            children: (
              <Card
                bordered={false}
                className="competition-score-overview-card"
              >
                <div className="competition-score-overview-head">
                  <div>
                    <Typography.Title level={4}>综合成绩表</Typography.Title>
                    <Typography.Text type="secondary">
                      共 {filteredRows.length} 名选手 · 当前范围动态生成{' '}
                      {subjects.length} 个科目列
                    </Typography.Text>
                  </div>
                  <Space size={8} wrap>
                    <Tag color="blue">
                      {subjects
                        .map((subject) => subject.shortLabel)
                        .join(' / ') || '未配置科目'}
                    </Tag>
                    <Tooltip title="点击选手或成绩单元格，可查看成绩详情与修订轨迹">
                      <InfoCircleOutlined />
                    </Tooltip>
                  </Space>
                </div>
                <Table<ScoreOverviewRow>
                  rowKey="key"
                  size="middle"
                  dataSource={filteredRows}
                  rowSelection={{
                    selectedRowKeys,
                    onChange: (keys) =>
                      setSelectedRowKeys(keys.map((key) => String(key))),
                  }}
                  loading={
                    scoreSourcesQuery.isFetching ||
                    totalSnapshotsQuery.isFetching ||
                    rankingEntriesQuery.isFetching
                  }
                  columns={overviewColumns}
                  scroll={{ x: 1500 }}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 人`,
                  }}
                  rowClassName={(row) =>
                    row.overallStatus === 'absent'
                      ? 'competition-score-row--absent'
                      : ''
                  }
                  locale={{
                    emptyText: (
                      <Empty
                        image={<FileSearchOutlined />}
                        description={
                          contextReady
                            ? '当前范围暂无成绩记录'
                            : '请选择完整赛事范围和已发布规则'
                        }
                      />
                    ),
                  }}
                />
              </Card>
            ),
          },
          {
            key: 'workbench',
            label: (
              <span>
                <SettingOutlined /> 科目工作台
              </span>
            ),
            children: subjects.length ? (
              <Card
                bordered={false}
                className="competition-score-workbench-card"
              >
                <div className="competition-score-workbench-head">
                  <div>
                    <Typography.Title level={4}>
                      科目成绩工作台
                    </Typography.Title>
                    <Typography.Text type="secondary">
                      仅展示当前赛段已启用的科目，成绩值与业务状态分开呈现。
                    </Typography.Text>
                  </div>
                  {subjects.some((subject) => subject.type === 'simulation') &&
                    canImportSimulation &&
                    !usesStageContentFormula &&
                    Boolean(scoreRuleId) && (
                      <Upload
                        accept=".xlsx,.xls"
                        showUploadList={false}
                        beforeUpload={(file) => {
                          simulationUploadMutation.mutate(file);
                          return false;
                        }}
                      >
                        <Button
                          icon={<UploadOutlined />}
                          loading={simulationUploadMutation.isPending}
                        >
                          导入仿真成绩
                        </Button>
                      </Upload>
                    )}
                </div>
                <Tabs
                  activeKey={workbenchSubject || subjects[0].type}
                  onChange={(key) => {
                    const sourceType = key as CompetitionScoreSourceType;
                    setWorkbenchSubject(sourceType);
                    if (sourceType === 'practical' || sourceType === 'simulation') {
                      setImportSubject(sourceType);
                    }
                  }}
                  items={workbenchItems}
                />
              </Card>
            ) : (
              <Card bordered={false} className="competition-score-empty-card">
                <Empty
                  image={<FileSearchOutlined />}
                  description="当前赛段还没有启用成绩科目"
                />
                <Typography.Paragraph type="secondary">
                  请在赛段编排中启用理论、实操或仿真内容，成绩工作台会按配置自动生成科目页签。
                </Typography.Paragraph>
              </Card>
            ),
          },
          {
            key: 'summary',
            label: (
              <span>
                <CalculatorOutlined /> 汇总发布{' '}
                {activeSnapshot?.status === 'confirmed' ? (
                  <Badge status="processing" />
                ) : null}
              </span>
            ),
            children: (
              <div className="competition-score-summary">
                <Row gutter={[12, 12]} className="competition-score-statistics">
                  <Col xs={12} md={6}>
                    <Card bordered={false}>
                      <Statistic
                        title="参赛人数"
                        value={statistics.participantCount}
                        prefix={<TeamOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card bordered={false}>
                      <Statistic
                        title="成绩完整"
                        value={statistics.complete}
                        suffix={`/ ${statistics.eligibleCount}`}
                        prefix={<CheckCircleFilled />}
                        valueStyle={{ color: '#1677ff' }}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card bordered={false}>
                      <Statistic
                        title="缺考人数"
                        value={statistics.absent}
                        prefix={<CloseCircleOutlined />}
                        valueStyle={{ color: '#8c8c8c' }}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card bordered={false}>
                      <Statistic
                        title="异常 / 待审核"
                        value={statistics.abnormal + statistics.pendingReview}
                        prefix={<WarningOutlined />}
                        valueStyle={{
                          color:
                            statistics.abnormal + statistics.pendingReview
                              ? '#d4380d'
                              : '#52c41a',
                        }}
                      />
                    </Card>
                  </Col>
                </Row>
                <Card
                  bordered={false}
                  className="competition-score-publish-card"
                >
                  <div className="competition-score-publish-card__head">
                    <div>
                      <Typography.Title level={4}>
                        汇总与发布中心
                      </Typography.Title>
                      <Typography.Text type="secondary">
                        发布前完成成绩完整性、锁定状态和排名快照复核。
                      </Typography.Text>
                    </div>
                    <Space>
                      <Progress
                        type="circle"
                        percent={statistics.completionPercent}
                        size={54}
                        status={
                          statistics.completionPercent === 100
                            ? 'success'
                            : 'active'
                        }
                      />
                      <div>
                        <Typography.Text strong>成绩完整度</Typography.Text>
                        <br />
                        <Typography.Text type="secondary">
                          {statistics.complete} / {statistics.eligibleCount}
                        </Typography.Text>
                      </div>
                    </Space>
                  </div>
                  <Divider />
                  <Row gutter={[24, 18]}>
                    <Col xs={24} lg={14}>
                      <div className="competition-score-check-list">
                        <Typography.Text strong>发布前检查</Typography.Text>
                        {[
                          statistics.complete >= statistics.eligibleCount
                            ? '所有应排名选手均已有已确认总分'
                            : `仍有 ${Math.max(0, statistics.eligibleCount - statistics.complete)} 名选手缺少已确认总分`,
                          statistics.absent
                            ? `${statistics.absent} 名缺考选手将保留在榜单但不占名次`
                            : '没有缺考选手',
                          statistics.abnormal
                            ? `${statistics.abnormal} 名选手存在异常成绩，需要处理后再发布`
                            : '未发现异常成绩',
                          activeSnapshot?.status === 'confirmed' ||
                          activeSnapshot?.status === 'published'
                            ? '当前排名快照已复核确认'
                            : '尚未有已复核确认的排名快照',
                        ].map((text) => {
                          const warn =
                            text.includes('仍有') ||
                            text.includes('存在异常') ||
                            text.includes('尚未');
                          return (
                            <div
                              key={text}
                              className="competition-score-check-item"
                            >
                              <span className={warn ? 'is-warn' : 'is-pass'}>
                                {warn ? (
                                  <WarningOutlined />
                                ) : (
                                  <CheckCircleFilled />
                                )}
                              </span>
                              {text}
                            </div>
                          );
                        })}
                      </div>
                    </Col>
                    <Col xs={24} lg={10}>
                      <div className="competition-score-rule-card">
                        <Typography.Text strong>
                          当前综合计分规则
                        </Typography.Text>
                        <div className="competition-score-rule-card__formula">
                          {formulaParts.length ? (
                            formulaParts.map((part) => (
                              <Tag key={part} color="blue">
                                {part}
                              </Tag>
                            ))
                          ) : (
                            <Tag>等待规则配置</Tag>
                          )}
                        </div>
                        <Typography.Text type="secondary">
                          {theoryOnly
                            ? '理论成绩来自题目快照分值和答案判定，判分完成后自动形成综合成绩与排名草稿。'
                            : usesStageContentFormula
                              ? '综合成绩按赛段内容权重计算。已确认的科目分按编排权重合成后自动形成排名草稿。'
                              : '最终综合成绩由服务端按已发布规则计算，前端只展示结果。'}
                        </Typography.Text>
                        <div className="competition-score-rule-card__scope">
                          <LockOutlined /> 当前范围：{contextLabel}
                        </div>
                      </div>
                    </Col>
                  </Row>
                  <div className="competition-score-publish-actions">
                    <Button
                      icon={<ArrowRightOutlined />}
                      onClick={() => setActiveTab('overview')}
                    >
                      返回综合成绩
                    </Button>
                    {canGenerateRanking && !usesStageContentFormula && (
                      <Button
                        icon={<TrophyOutlined />}
                        disabled={
                          !contextReady ||
                          !totalSnapshots.some(
                            (snapshot) =>
                              snapshot.currentFlag === 1 &&
                              snapshot.confirmationStatus === 'confirmed',
                          )
                        }
                        onClick={openRankingDraft}
                      >
                        生成排名快照
                      </Button>
                    )}
                    {activeSnapshot && (
                      <Button
                        type="primary"
                        disabled={
                          activeSnapshot.status === 'published' || !canPublish
                        }
                        onClick={() => {
                          if (
                            activeSnapshot.status === 'draft' &&
                            canReviewRanking
                          )
                            confirmRankingMutation.mutate(activeSnapshot.id);
                          else if (
                            activeSnapshot.status === 'confirmed' &&
                            canPublishRanking
                          )
                            requestRankingPublish(activeSnapshot.id);
                        }}
                      >
                        {activeSnapshot.status === 'published'
                          ? '已发布'
                          : activeSnapshot.status === 'confirmed'
                            ? '发布排名'
                            : '先复核排名'}
                      </Button>
                    )}
                  </div>
                </Card>
                {canViewRanking && (
                  <Card
                    bordered={false}
                    className="competition-score-trace-card"
                    title={
                      <Space>
                        <TrophyFilled />
                        排名版本
                      </Space>
                    }
                    extra={
                      <Typography.Text type="secondary">
                        排名快照不可变，发布更正会生成新版本
                      </Typography.Text>
                    }
                  >
                    <ProTable<CompetitionRankingSnapshot>
                      rowKey={(row) => idValue(row.id)}
                      search={false}
                      options={false}
                      loading={rankingSnapshotsQuery.isFetching}
                      dataSource={rankingSnapshots}
                      columns={snapshotColumns}
                      pagination={{
                        pageSize: 8,
                        showSizeChanger: false,
                        showTotal: (total) => `共 ${total} 个版本`,
                      }}
                    />
                  </Card>
                )}
                {simulationBatchQuery.data && (
                  <Alert
                    className="competition-score-import-alert"
                    type={
                      simulationBatchQuery.data.batchStatus === 'invalid' ||
                      simulationBatchQuery.data.batchStatus === 'failed'
                        ? 'error'
                        : 'info'
                    }
                    showIcon
                    message={`仿真导入：${simulationBatchQuery.data.batchStatus}`}
                    description={`总行数 ${simulationBatchQuery.data.totalRows}，有效 ${simulationBatchQuery.data.validRows}，错误 ${simulationBatchQuery.data.errorRows}，已确认 ${simulationBatchQuery.data.confirmedRows}${simulationBatchQuery.data.errorMessage ? `；${simulationBatchQuery.data.errorMessage}` : ''}`}
                    action={
                      <Space>
                        {simulationBatchQuery.data.errorRows > 0 && (
                          <Button
                            icon={<DownloadOutlined />}
                            onClick={async () => {
                              try {
                                downloadBlob(
                                  await downloadCompetitionSimulationImportErrors(
                                    simulationBatchQuery.data!.id,
                                  ),
                                  '仿真成绩导入错误明细.xlsx',
                                );
                              } catch {
                                message.error('错误明细下载失败');
                              }
                            }}
                          >
                            下载错误明细
                          </Button>
                        )}
                        {(simulationBatchQuery.data.batchStatus === 'ready' ||
                          (simulationBatchQuery.data.batchStatus === 'failed' &&
                            simulationBatchQuery.data.errorRows === 0 &&
                            simulationBatchQuery.data.confirmedRows <
                              simulationBatchQuery.data.validRows)) && (
                          <Button
                            type="primary"
                            loading={simulationConfirmMutation.isPending}
                            onClick={requestSimulationConfirm}
                          >
                            确认导入
                          </Button>
                        )}
                      </Space>
                    }
                  />
                )}
                {renderSubjectImportAlert(
                  'practical',
                  practicalImportQuery.data,
                )}
                {renderSubjectImportAlert(
                  'simulation',
                  simulationIdCardImportQuery.data,
                )}
              </div>
            ),
          },
          {
            key: 'trace',
            label: (
              <span>
                <FileSearchOutlined /> 数据追踪
              </span>
            ),
            children: (
              <div className="competition-score-trace">
                {canViewScores && (
                  <Card
                    bordered={false}
                    className="competition-score-trace-card"
                    title={
                      <Space>
                        <FileSearchOutlined />
                        成绩来源修订
                      </Space>
                    }
                    extra={
                      <Typography.Text type="secondary">
                        保留每次来源修订，当前有效版本以绿色标记
                      </Typography.Text>
                    }
                  >
                    <ProTable<CompetitionScoreSource>
                      rowKey={(row) => idValue(row.id)}
                      search={false}
                      options={false}
                      loading={scoreSourcesQuery.isFetching}
                      dataSource={scoreSources}
                      columns={sourceColumns}
                      scroll={{ x: 1300 }}
                      pagination={{
                        defaultPageSize: 10,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条修订`,
                      }}
                    />
                  </Card>
                )}
                {canViewScores && (
                  <Card
                    bordered={false}
                    className="competition-score-trace-card"
                    title={
                      <Space>
                        <CalculatorOutlined />
                        综合成绩计算快照
                      </Space>
                    }
                    extra={
                      <Typography.Text type="secondary">
                        重算生成新版本，不覆盖历史快照
                      </Typography.Text>
                    }
                  >
                    <ProTable<CompetitionTotalScoreSnapshot>
                      rowKey={(row) => idValue(row.id)}
                      search={false}
                      options={false}
                      loading={totalSnapshotsQuery.isFetching}
                      dataSource={totalSnapshots}
                      columns={totalColumns}
                      scroll={{ x: 1100 }}
                      pagination={{
                        defaultPageSize: 10,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 个快照`,
                      }}
                    />
                  </Card>
                )}
                {canViewScores && (
                  <Card
                    bordered={false}
                    className="competition-score-trace-card"
                    title={
                      <Space>
                        <ClockCircleOutlined />
                        理论判分任务
                      </Space>
                    }
                    extra={
                      <Typography.Text type="secondary">
                        异步任务状态自动刷新
                      </Typography.Text>
                    }
                  >
                    <ProTable<CompetitionFormalScoringTask>
                      rowKey={(row) => idValue(row.id)}
                      search={false}
                      options={false}
                      loading={scoringTasksQuery.isFetching}
                      dataSource={scoringTasks}
                      columns={[
                        {
                          title: '选手',
                          dataIndex: 'candidateName',
                          width: 150,
                        },
                        {
                          title: '准考证号',
                          dataIndex: 'candidateNo',
                          width: 150,
                        },
                        {
                          title: '结果修订',
                          dataIndex: 'resultRevisionNo',
                          width: 90,
                          render: (_, row) => `V${row.resultRevisionNo}`,
                        },
                        {
                          title: '状态',
                          dataIndex: 'taskStatus',
                          width: 100,
                          render: (_, row) =>
                            row.taskStatus === 'completed' ? (
                              <Tag color="success">已完成</Tag>
                            ) : row.taskStatus === 'failed' ? (
                              <Tag color="error">失败</Tag>
                            ) : (
                              <Tag color="processing">处理中</Tag>
                            ),
                        },
                        {
                          title: '失败原因',
                          dataIndex: 'lastError',
                          ellipsis: true,
                        },
                        {
                          title: '完成时间',
                          dataIndex: 'completedAt',
                          width: 165,
                          render: (_, row) => formatDate(row.completedAt),
                        },
                        {
                          title: '操作',
                          width: 80,
                          render: (_, row) =>
                            row.taskStatus === 'failed' && canRetryScoring ? (
                              <Button
                                type="link"
                                onClick={() => retryTaskMutation.mutate(row.id)}
                              >
                                重试
                              </Button>
                            ) : (
                              '—'
                            ),
                        },
                      ]}
                      pagination={{ pageSize: 10 }}
                    />
                  </Card>
                )}
                {canViewRanking && (
                  <Card
                    bordered={false}
                    className="competition-score-trace-card"
                    title={
                      <Space>
                        <TrophyFilled />
                        排名条目
                      </Space>
                    }
                  >
                    <ProTable<CompetitionRankingEntry>
                      rowKey={(row) => idValue(row.id)}
                      search={false}
                      options={false}
                      loading={rankingEntriesQuery.isFetching}
                      dataSource={rankingEntries}
                      columns={entryColumns}
                      pagination={{ pageSize: 10 }}
                    />
                  </Card>
                )}
              </div>
            ),
          },
        ]}
      />
      <Drawer
        title="选手成绩详情"
        open={Boolean(selectedRow)}
        width={560}
        onClose={() => {
          setSelectedMemberId(undefined);
          setAnswerSheetOpen(false);
        }}
        destroyOnClose
      >
        {selectedRow && (
          <div className="competition-score-detail">
            <div className="competition-score-detail__hero">
              <Avatar size={48}>{selectedRow.candidateName.slice(0, 1)}</Avatar>
              <div>
                <Typography.Title level={4}>
                  {selectedRow.candidateName}
                </Typography.Title>
                <Typography.Text type="secondary">
                  {selectedRow.candidateNo} · {selectedRow.teamName}
                </Typography.Text>
              </div>
              <div className="competition-score-detail__total">
                <Typography.Text type="secondary">综合成绩</Typography.Text>
                <strong>
                  {selectedRow.total?.confirmationStatus === 'confirmed'
                    ? scoreValue(selectedRow.total.totalScore)
                    : '—'}
                </strong>
                {statusTag(selectedRow.overallStatus, true)}
              </div>
            </div>
            <div className="competition-score-detail__section-head">
              <Typography.Title level={5}>科目成绩</Typography.Title>
            </div>
            <div className="competition-score-detail__subjects">
              {subjects.map((subject) => {
                const cell = selectedRow.subjectCells[subject.type];
                const source = cell?.source;
                const finalValue = selectedRow.total
                  ? (selectedRow.total[sourceMeta[subject.type].scoreField] as
                      number | string | null | undefined)
                  : source?.rawScore;
                return (
                  <Card
                    key={subject.type}
                    size="small"
                    className="competition-score-detail__subject"
                  >
                    <div className="competition-score-detail__subject-row">
                      <span style={{ color: subject.color }}>
                        {subject.icon} {subject.label}
                      </span>
                      <strong>{scoreValue(finalValue ?? cell?.value)}</strong>
                    </div>
                    <div className="competition-score-detail__subject-actions">
                      {statusTag(cell?.status || 'not_started', true)}
                      {subject.type === 'theory' && selectedTask && (
                        <>
                          <Button
                            size="small"
                            icon={<EyeOutlined />}
                            loading={formalResultQuery.isFetching}
                            disabled={
                              formalResultQuery.isLoading ||
                              !formalResultQuery.data?.questionScores?.length
                            }
                            onClick={() => {
                              setSelectedAnswerQuestionId(undefined);
                              setAnswerSheetOpen(true);
                            }}
                          >
                            查看答题卡
                          </Button>
                          <Button
                            size="small"
                            icon={<DownloadOutlined />}
                            disabled={
                              formalResultQuery.isLoading ||
                              !formalResultQuery.data?.questionScores?.length
                            }
                            loading={
                              exportingAnswerSheetId ===
                              idValue(selectedTask.candidateExamId)
                            }
                            onClick={() =>
                              void exportOneAnswerSheet(
                                selectedTask.candidateExamId,
                                selectedRow.candidateName,
                                selectedRow.candidateNo,
                              )
                            }
                          >
                            导出答题卡
                          </Button>
                        </>
                      )}
                    </div>
                  </Card>
                );
              })}
            </div>
          </div>
        )}
      </Drawer>
      <Modal
        className="competition-answer-sheet-modal"
        title={
          <Space>
            <FileSearchOutlined />
            <span>查看答题卡</span>
            {answerSheetQuery.data && (
              <Typography.Text type="secondary">
                {answerSheetQuery.data.candidateName} ·{' '}
                {answerSheetQuery.data.candidateNo}
              </Typography.Text>
            )}
          </Space>
        }
        open={answerSheetOpen}
        width="calc(100vw - 48px)"
        footer={
          answerSheetQuery.data ? (
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              loading={
                exportingAnswerSheetId ===
                idValue(answerSheetQuery.data.candidateExamId)
              }
              onClick={() =>
                void exportOneAnswerSheet(
                  answerSheetQuery.data!.candidateExamId,
                  answerSheetQuery.data!.candidateName,
                  answerSheetQuery.data!.candidateNo,
                )
              }
            >
              导出 Word
            </Button>
          ) : null
        }
        destroyOnClose
        onCancel={() => setAnswerSheetOpen(false)}
      >
        {answerSheetQuery.isLoading ? (
          <div className="competition-answer-sheet__loading">
            <Spin tip="正在加载答题卡…" />
          </div>
        ) : answerSheetQuery.isError ? (
          <Result
            status="error"
            title="答题卡加载失败"
            subTitle={
              answerSheetQuery.error instanceof Error
                ? answerSheetQuery.error.message
                : '请稍后重试'
            }
            extra={
              <Button onClick={() => answerSheetQuery.refetch()}>重新加载</Button>
            }
          />
        ) : answerSheetQuery.data && activeAnswerQuestion ? (
          <div className="competition-answer-sheet">
            <Card size="small" className="competition-answer-sheet__summary">
              <Space wrap>
                <Tag color="success">正确 {answerSheetCorrectCount}</Tag>
                <Tag color="error">错误 {answerSheetWrongCount}</Tag>
                <Tag>未作答 {answerSheetUnansweredCount}</Tag>
              </Space>
              <Typography.Text type="secondary">
                点击左侧题号查看该题作答内容
              </Typography.Text>
            </Card>
            <Row gutter={[16, 16]}>
              <Col xs={24} md={8}>
                <Card
                  size="small"
                  title={`题目导航（${answerSheetQuestions.length} 题）`}
                  className="competition-answer-sheet__navigation"
                >
                  <div className="competition-answer-sheet__question-grid">
                    {answerSheetQuestions.map((question) => {
                      const status = answerSheetStatus(question);
                      const active =
                        idValue(question.questionSnapshotId) ===
                        idValue(activeAnswerQuestion.questionSnapshotId);
                      return (
                        <Button
                          key={idValue(question.questionSnapshotId)}
                          className={`competition-answer-sheet__question-number competition-answer-sheet__question-number--${status}${active ? ' is-active' : ''}`}
                          aria-label={`第 ${question.questionOrder} 题${status === 'correct' ? '，正确' : status === 'wrong' ? '，错误' : '，未作答'}`}
                          aria-pressed={active}
                          onClick={() =>
                            setSelectedAnswerQuestionId(
                              idValue(question.questionSnapshotId),
                            )
                          }
                        >
                          {question.questionOrder}
                        </Button>
                      );
                    })}
                  </div>
                  <Divider />
                  <Space wrap size={[12, 8]}>
                    <span className="competition-answer-sheet__legend">
                      <i className="competition-answer-sheet__legend-dot competition-answer-sheet__legend-dot--correct" />
                      正确
                    </span>
                    <span className="competition-answer-sheet__legend">
                      <i className="competition-answer-sheet__legend-dot competition-answer-sheet__legend-dot--wrong" />
                      错误
                    </span>
                    <span className="competition-answer-sheet__legend">
                      <i className="competition-answer-sheet__legend-dot competition-answer-sheet__legend-dot--unanswered" />
                      未作答
                    </span>
                  </Space>
                </Card>
              </Col>
              <Col xs={24} md={16}>
                <Card
                  size="small"
                  className="competition-answer-sheet__question-detail"
                  title={
                    <Space wrap>
                      <span>第 {activeAnswerQuestion.questionOrder} 题</span>
                      <Tag>{questionTypeLabel(activeAnswerQuestion.questionType)}</Tag>
                      <Tag
                        color={
                          answerSheetStatus(activeAnswerQuestion) === 'correct'
                            ? 'success'
                            : answerSheetStatus(activeAnswerQuestion) === 'wrong'
                              ? 'error'
                              : 'default'
                        }
                      >
                        {answerSheetStatus(activeAnswerQuestion) === 'correct'
                          ? '正确'
                          : answerSheetStatus(activeAnswerQuestion) === 'wrong'
                            ? '错误'
                            : '未作答'}
                      </Tag>
                    </Space>
                  }
                >
                  <Typography.Paragraph className="competition-answer-sheet__stem">
                    {activeAnswerQuestion.stem || '（题目内容为空）'}
                  </Typography.Paragraph>
                  {activeAnswerQuestion.options.length > 0 ? (
                    <div className="competition-answer-sheet__options">
                      {activeAnswerQuestion.options.map((option) => {
                        const selected =
                          activeAnswerQuestion.candidateAnswer.selectedOptionIds?.includes(
                            option.optionId,
                          ) || false;
                        return (
                          <div
                            key={option.optionId}
                            className={`competition-answer-sheet__option${selected ? ' is-selected' : ''}`}
                          >
                            <strong>{option.label}</strong>
                            <Typography.Text>{option.content}</Typography.Text>
                            {selected && <Tag color="processing">选手选择</Tag>}
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div className="competition-answer-sheet__fill-answers">
                      <Typography.Text type="secondary">选手答案</Typography.Text>
                      {activeAnswerQuestion.candidateAnswer.blanks?.length ? (
                        activeAnswerQuestion.candidateAnswer.blanks.map(
                          (answer, index) => (
                            <Typography.Paragraph key={`blank-${index}`}>
                              第 {index + 1} 空：{answer || '（未填写）'}
                            </Typography.Paragraph>
                          ),
                        )
                      ) : (
                        <Typography.Paragraph>
                          {selectedAnswerLabels(activeAnswerQuestion).join('、') ||
                            (activeAnswerQuestion.answered ? '已作答' : '未作答')}
                        </Typography.Paragraph>
                      )}
                    </div>
                  )}
                  {activeAnswerQuestion.options.length > 0 && (
                    <div className="competition-answer-sheet__answer-summary">
                      <Typography.Text type="secondary">选手答案</Typography.Text>
                      <Typography.Paragraph>
                        {selectedAnswerLabels(activeAnswerQuestion).join('、') ||
                          (activeAnswerQuestion.answered ? '已作答' : '未作答')}
                      </Typography.Paragraph>
                    </div>
                  )}
                </Card>
              </Col>
            </Row>
          </div>
        ) : (
          <Empty description="暂无答题卡内容" />
        )}
      </Modal>
      <Modal
        title="核对并生成排名快照"
        open={rankingDraftOpen}
        width={1120}
        destroyOnClose
        confirmLoading={generateRankingMutation.isPending}
        okText="生成不可变快照"
        okButtonProps={{
          disabled:
            rankingDraftRows.length === 0 ||
            !completedTieKeysValid ||
            !rankingReviewed,
        }}
        onCancel={() => setRankingDraftOpen(false)}
        onOk={() => generateRankingMutation.mutate()}
      >
        <Alert
          type="warning"
          showIcon
          message="排名顺序与同分键由已发布规则决定"
          description="系统只按当前总分生成初始顺序，不猜测二级排序规则；请核对完赛选手顺序并填写完整排序键。缺考选手保留展示但不占名次。"
        />
        <Space className="competition-ranking-draft-actions" wrap>
          <Input
            value={absentContestantId}
            placeholder="输入缺考选手成员 ID"
            onChange={(event) => setAbsentContestantId(event.target.value)}
            onPressEnter={addAbsentContestant}
            style={{ width: 260 }}
          />
          <Button icon={<PlusOutlined />} onClick={addAbsentContestant}>
            加入缺考选手
          </Button>
          <Input
            value={generationReason}
            placeholder="生成原因（可选）"
            onChange={(event) => setGenerationReason(event.target.value)}
            style={{ width: 360 }}
          />
        </Space>
        <ProTable<RankingDraftRow>
          rowKey="key"
          search={false}
          options={false}
          pagination={false}
          dataSource={rankingDraftRows}
          scroll={{ y: 420 }}
          columns={[
            {
              title: '顺序',
              width: 65,
              render: (_value, _row, index) => index + 1,
            },
            {
              title: '选手成员 ID',
              dataIndex: 'contestantMemberId',
              width: 170,
            },
            {
              title: '状态',
              width: 80,
              render: (_value, row) =>
                row.contestantStatus === 'absent' ? (
                  <Tag>缺考</Tag>
                ) : (
                  <Tag color="green">完赛</Tag>
                ),
            },
            {
              title: '总分',
              width: 80,
              render: (_value, row) =>
                scoreValue(row.totalScoreSnapshot?.totalScore),
            },
            {
              title: '理论',
              width: 75,
              render: (_value, row) =>
                scoreValue(row.totalScoreSnapshot?.theoryScore),
            },
            {
              title: '仿真',
              width: 75,
              render: (_value, row) =>
                scoreValue(row.totalScoreSnapshot?.simulationScore),
            },
            {
              title: '实操',
              width: 75,
              render: (_value, row) =>
                scoreValue(row.totalScoreSnapshot?.practicalScore),
            },
            {
              title: '完整排序键',
              width: 250,
              render: (_value, row) =>
                row.contestantStatus === 'absent' ? (
                  '-'
                ) : (
                  <Input
                    value={row.tieKey}
                    status={row.tieKey?.trim() ? undefined : 'error'}
                    placeholder="按本场规则填写，完全并列者相同"
                    onChange={(event) => {
                      const tieKey = event.target.value;
                      setRankingDraftRows((rows) =>
                        rows.map((item) =>
                          item.key === row.key ? { ...item, tieKey } : item,
                        ),
                      );
                      setRankingReviewed(false);
                    }}
                  />
                ),
            },
            {
              title: '调整',
              width: 115,
              render: (_value, row, index) => (
                <Space size={2}>
                  <Button
                    size="small"
                    icon={<ArrowUpOutlined />}
                    disabled={index === 0}
                    onClick={() => moveRankingRow(index, -1)}
                  />
                  <Button
                    size="small"
                    icon={<ArrowDownOutlined />}
                    disabled={index === rankingDraftRows.length - 1}
                    onClick={() => moveRankingRow(index, 1)}
                  />
                  {row.contestantStatus === 'absent' && (
                    <Button
                      danger
                      size="small"
                      onClick={() =>
                        setRankingDraftRows((rows) =>
                          rows.filter((item) => item.key !== row.key),
                        )
                      }
                    >
                      移除
                    </Button>
                  )}
                </Space>
              ),
            },
          ]}
        />
        <Checkbox
          checked={rankingReviewed}
          disabled={!completedTieKeysValid || rankingDraftRows.length === 0}
          onChange={(event) => setRankingReviewed(event.target.checked)}
        >
          我已按本场已确认规则核对候选顺序和所有完赛选手的完整排序键，并确认缺考选手不占名次
        </Checkbox>
      </Modal>
    </div>
  );
  return embedded ? (
    content
  ) : (
    <CompetitionEventFrame
      event={eventQuery.data}
      activeSection="scores"
      onNavigate={onNavigate}
    >
      {content}
    </CompetitionEventFrame>
  );
}

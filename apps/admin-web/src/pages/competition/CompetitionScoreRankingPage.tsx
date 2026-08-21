import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  CalculatorOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  TrophyOutlined,
} from '@ant-design/icons';
import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Input,
  Modal,
  Result,
  Select,
  Space,
  Spin,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import {
  calculateCompetitionTotalScore,
  generateCompetitionRankingSnapshot,
  getCompetitionEvent,
  getCompetitionScoringReadiness,
  listCompetitionEventWorkTypes,
  pageCompetitionRankingEntries,
  listCompetitionScoreRules,
  pageCompetitionRankingSnapshots,
  pageCompetitionScoreSources,
  pageCompetitionStages,
  pageCompetitionTotalScoreSnapshots,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionId,
  CompetitionRankingCandidate,
  CompetitionRankingEntry,
  CompetitionRankingSnapshot,
  CompetitionScoreConfirmationStatus,
  CompetitionScoreSource,
  CompetitionScoreSourceType,
  CompetitionTotalScoreSnapshot,
} from '../../types/competition';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import {
  buildRankingCandidates,
  findCalculationReadyContestants,
  rankingEntryPageItems,
} from './competitionScoreUtils';
import './CompetitionEventManagementPage.css';

interface RankingDraftRow extends CompetitionRankingCandidate {
  key: string;
  totalScoreSnapshot?: CompetitionTotalScoreSnapshot;
}

const sourceMeta: Record<CompetitionScoreSourceType, { label: string; color: string }> = {
  theory: { label: '理论', color: 'blue' },
  simulation: { label: '仿真', color: 'purple' },
  practical: { label: '实操', color: 'orange' },
};

const confirmationMeta: Record<CompetitionScoreConfirmationStatus, { label: string; color: string }> = {
  pending: { label: '待确认', color: 'warning' },
  confirmed: { label: '已确认', color: 'success' },
  voided: { label: '已作废', color: 'default' },
};

function idValue(value?: CompetitionId | null) {
  return value === undefined || value === null ? '-' : String(value);
}

function scoreValue(value?: number | string | null) {
  if (value === undefined || value === null || value === '') return '-';
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed.toFixed(2) : String(value);
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function confirmationTag(status: CompetitionScoreConfirmationStatus) {
  const meta = confirmationMeta[status];
  return <Tag color={meta.color}>{meta.label}</Tag>;
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
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canViewEvent = permissions.includes('competition:event:view');
  const canViewScores = permissions.includes('competition:score:view');
  const canCalculate = permissions.includes('competition:score:calculate');
  const canViewRanking = permissions.includes('competition:ranking:view');
  const canGenerateRanking = permissions.includes('competition:ranking:generate');
  const canReadDimensions = permissions.includes('competition:stage:view')
    && permissions.includes('competition:event-work-type:view')
    && permissions.includes('competition:score-rule:view');
  const canReadReadiness = permissions.includes('competition:scoring-readiness:view');
  const [stageId, setStageId] = useState<string>();
  const [eventWorkTypeId, setEventWorkTypeId] = useState<string>();
  const [scoreRuleId, setScoreRuleId] = useState<string>();
  const [activeTab, setActiveTab] = useState('sources');
  const [selectedSnapshotId, setSelectedSnapshotId] = useState<string>();
  const [rankingDraftOpen, setRankingDraftOpen] = useState(false);
  const [rankingDraftRows, setRankingDraftRows] = useState<RankingDraftRow[]>([]);
  const [generationReason, setGenerationReason] = useState('');
  const [absentContestantId, setAbsentContestantId] = useState('');
  const [rankingReviewed, setRankingReviewed] = useState(false);
  const queryClient = useQueryClient();
  const contextReady = Boolean(stageId && eventWorkTypeId && scoreRuleId);

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canViewEvent,
  });
  const stagesQuery = useQuery({
    queryKey: ['competition-stages', eventId],
    queryFn: () => pageCompetitionStages(eventId, { page: 1, pageSize: 500 }),
    enabled: canViewEvent && canReadDimensions,
  });
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canViewEvent && canReadDimensions,
  });
  const rulesQuery = useQuery({
    queryKey: ['competition-score-rules', eventId, stageId, eventWorkTypeId],
    queryFn: () => listCompetitionScoreRules(eventId, { stageId, eventWorkTypeId }),
    enabled: canReadDimensions && Boolean(stageId && eventWorkTypeId),
  });

  useEffect(() => {
    if (!stageId && stagesQuery.data?.items.length) {
      setStageId(idValue(stagesQuery.data.items[0].id));
    }
  }, [stageId, stagesQuery.data?.items]);
  useEffect(() => {
    if (!eventWorkTypeId && workTypesQuery.data?.length) {
      setEventWorkTypeId(idValue(workTypesQuery.data[0].id));
    }
  }, [eventWorkTypeId, workTypesQuery.data]);
  useEffect(() => {
    const publishedRules = (rulesQuery.data || []).filter((rule) => rule.status === 'published');
    if (!publishedRules.some((rule) => idValue(rule.id) === scoreRuleId)) {
      setScoreRuleId(publishedRules[0] ? idValue(publishedRules[0].id) : undefined);
    }
  }, [rulesQuery.data, scoreRuleId]);

  const readinessQuery = useQuery({
    queryKey: ['competition-scoring-readiness', eventId, stageId, eventWorkTypeId],
    queryFn: () => getCompetitionScoringReadiness(eventId, { stageId, eventWorkTypeId }),
    enabled: canReadReadiness && Boolean(stageId && eventWorkTypeId),
  });
  const scoreSourcesQuery = useQuery({
    queryKey: ['competition-score-sources', eventId, stageId, eventWorkTypeId],
    queryFn: () => pageCompetitionScoreSources(eventId, {
      page: 1,
      pageSize: 500,
      stageId: stageId!,
      eventWorkTypeId: eventWorkTypeId!,
    }),
    enabled: canViewScores && contextReady,
  });
  const totalSnapshotsQuery = useQuery({
    queryKey: ['competition-total-score-snapshots', eventId, stageId, eventWorkTypeId, scoreRuleId],
    queryFn: () => pageCompetitionTotalScoreSnapshots(eventId, {
      page: 1,
      pageSize: 500,
      stageId: stageId!,
      eventWorkTypeId: eventWorkTypeId!,
      scoreRuleId,
    }),
    enabled: canViewScores && contextReady,
  });
  const rankingSnapshotsQuery = useQuery({
    queryKey: ['competition-ranking-snapshots', eventId, stageId, eventWorkTypeId, scoreRuleId],
    queryFn: () => pageCompetitionRankingSnapshots(eventId, {
      page: 1,
      pageSize: 500,
      stageId: stageId!,
      eventWorkTypeId: eventWorkTypeId!,
      scoreRuleId,
    }),
    enabled: canViewRanking && contextReady,
  });
  const rankingEntriesQuery = useQuery({
    queryKey: ['competition-ranking-entries', selectedSnapshotId],
    queryFn: () => pageCompetitionRankingEntries(
      selectedSnapshotId!,
      { page: 1, pageSize: 500 },
    ),
    enabled: canViewRanking && Boolean(selectedSnapshotId),
  });

  const selectedRuleSources = useMemo(
    () => (scoreSourcesQuery.data?.items || []).filter((source) => idValue(source.scoreRuleId) === scoreRuleId),
    [scoreRuleId, scoreSourcesQuery.data?.items],
  );
  const calculationReadyContestants = useMemo(
    () => findCalculationReadyContestants(selectedRuleSources),
    [selectedRuleSources],
  );

  const calculateMutation = useMutation({
    mutationFn: async () => Promise.all(calculationReadyContestants.map((contestantMemberId) =>
      calculateCompetitionTotalScore(eventId, {
        stageId: stageId!,
        eventWorkTypeId: eventWorkTypeId!,
        contestantMemberId,
        scoreRuleId: scoreRuleId!,
      }))),
    onSuccess: (results) => {
      message.success(`已生成 ${results.length} 条总分快照`);
      queryClient.invalidateQueries({ queryKey: ['competition-total-score-snapshots', eventId] });
    },
  });

  const generateRankingMutation = useMutation({
    mutationFn: () => generateCompetitionRankingSnapshot(eventId, {
      stageId: stageId!,
      eventWorkTypeId: eventWorkTypeId!,
      scoreRuleId: scoreRuleId!,
      generationReason: generationReason.trim() || undefined,
      candidates: rankingDraftRows.map(({ contestantMemberId, totalScoreSnapshotId, tieKey, contestantStatus }) => ({
        contestantMemberId,
        totalScoreSnapshotId,
        tieKey: contestantStatus === 'completed' ? tieKey?.trim() : undefined,
        contestantStatus,
      })),
    }),
    onSuccess: (result) => {
      message.success(`排名快照 V${result.snapshot.snapshotVersion} 已生成`);
      setRankingDraftOpen(false);
      setSelectedSnapshotId(idValue(result.snapshot.id));
      setActiveTab('rankings');
      queryClient.invalidateQueries({ queryKey: ['competition-ranking-snapshots', eventId] });
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

  const openRankingDraft = () => {
    const snapshots = totalSnapshotsQuery.data?.items || [];
    const candidates = buildRankingCandidates(snapshots);
    const byId = new Map(snapshots.map((snapshot) => [idValue(snapshot.id), snapshot]));
    setRankingDraftRows(candidates.map((candidate) => ({
      ...candidate,
      key: `completed-${idValue(candidate.contestantMemberId)}`,
      totalScoreSnapshot: byId.get(idValue(candidate.totalScoreSnapshotId)),
    })));
    setGenerationReason('');
    setAbsentContestantId('');
    setRankingReviewed(false);
    setRankingDraftOpen(true);
  };

  const addAbsentContestant = () => {
    const contestantId = absentContestantId.trim();
    if (!contestantId) return;
    if (rankingDraftRows.some((row) => idValue(row.contestantMemberId) === contestantId)) {
      message.warning('该选手已在候选列表中');
      return;
    }
    setRankingDraftRows((rows) => [...rows, {
      key: `absent-${contestantId}`,
      contestantMemberId: contestantId,
      contestantStatus: 'absent',
    }]);
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

  const completedTieKeysValid = rankingDraftRows.every((row) =>
    row.contestantStatus === 'absent' || Boolean(row.tieKey?.trim()));

  const sourceColumns: ProColumns<CompetitionScoreSource>[] = [
    { title: '选手成员 ID', dataIndex: 'contestantMemberId', width: 170, copyable: true },
    {
      title: '来源',
      dataIndex: 'sourceType',
      width: 90,
      render: (_, row) => <Tag color={sourceMeta[row.sourceType].color}>{sourceMeta[row.sourceType].label}</Tag>,
    },
    { title: '原始分', dataIndex: 'rawScore', width: 100, render: (_, row) => scoreValue(row.rawScore) },
    {
      title: '确认状态',
      dataIndex: 'confirmationStatus',
      width: 105,
      render: (_, row) => confirmationTag(row.confirmationStatus),
    },
    { title: '修订', dataIndex: 'revisionNo', width: 70, render: (_, row) => `V${row.revisionNo}` },
    { title: '当前有效', dataIndex: 'currentFlag', width: 90, render: (_, row) => row.currentFlag === 1 ? <Tag color="green">是</Tag> : <Tag>否</Tag> },
    { title: '来源系统', dataIndex: 'sourceSystem', width: 130, ellipsis: true },
    { title: '来源流水', dataIndex: 'sourceReference', width: 180, ellipsis: true, copyable: true },
    { title: '取得时间', dataIndex: 'achievedAt', width: 165, render: (_, row) => formatDate(row.achievedAt) },
    { title: '确认时间', dataIndex: 'confirmedAt', width: 165, render: (_, row) => formatDate(row.confirmedAt) },
  ];

  const totalColumns: ProColumns<CompetitionTotalScoreSnapshot>[] = [
    { title: '选手成员 ID', dataIndex: 'contestantMemberId', width: 170, copyable: true, fixed: 'left' },
    { title: '理论', dataIndex: 'theoryScore', width: 85, render: (_, row) => scoreValue(row.theoryScore) },
    { title: '仿真', dataIndex: 'simulationScore', width: 85, render: (_, row) => scoreValue(row.simulationScore) },
    { title: '实操', dataIndex: 'practicalScore', width: 85, render: (_, row) => scoreValue(row.practicalScore) },
    { title: '组合分', dataIndex: 'simulationPracticalScore', width: 90, render: (_, row) => scoreValue(row.simulationPracticalScore) },
    { title: '总分', dataIndex: 'totalScore', width: 100, render: (_, row) => <Typography.Text strong>{scoreValue(row.totalScore)}</Typography.Text> },
    { title: '理论权重', dataIndex: 'theoryWeight', width: 100, render: (_, row) => `${scoreValue(row.theoryWeight)}%` },
    { title: '组合权重', dataIndex: 'simulationPracticalWeight', width: 100, render: (_, row) => `${scoreValue(row.simulationPracticalWeight)}%` },
    { title: '计算版本', dataIndex: 'calculationVersion', width: 90, render: (_, row) => `V${row.calculationVersion}` },
    { title: '状态', dataIndex: 'confirmationStatus', width: 100, render: (_, row) => confirmationTag(row.confirmationStatus) },
    { title: '当前有效', dataIndex: 'currentFlag', width: 90, render: (_, row) => row.currentFlag === 1 ? <Tag color="green">是</Tag> : <Tag>否</Tag> },
    { title: '计算时间', dataIndex: 'calculatedAt', width: 165, render: (_, row) => formatDate(row.calculatedAt) },
  ];

  const snapshotColumns: ProColumns<CompetitionRankingSnapshot>[] = [
    { title: '快照版本', dataIndex: 'snapshotVersion', width: 100, render: (_, row) => `V${row.snapshotVersion}` },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (_, row) => {
        const meta = {
          draft: { label: '草稿', color: 'default' },
          confirmed: { label: '已确认', color: 'blue' },
          published: { label: '已发布', color: 'green' },
        }[row.status];
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    { title: '生成原因', dataIndex: 'generationReason', ellipsis: true },
    { title: '生成时间', dataIndex: 'generatedAt', width: 165, render: (_, row) => formatDate(row.generatedAt) },
    {
      title: '操作',
      valueType: 'option',
      width: 110,
      render: (_, row) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => setSelectedSnapshotId(idValue(row.id))}>
          查看榜单
        </Button>
      ),
    },
  ];

  const entryColumns: ProColumns<CompetitionRankingEntry>[] = [
    { title: '名次', dataIndex: 'displayRank', width: 80, render: (_, row) => row.displayRank ?? '-' },
    { title: '选手成员 ID', dataIndex: 'contestantMemberId', width: 180, copyable: true },
    { title: '总分', dataIndex: 'totalScore', width: 100, render: (_, row) => scoreValue(row.totalScore) },
    { title: '并列', dataIndex: 'tiedFlag', width: 80, render: (_, row) => row.tiedFlag === 1 ? <Tag color="blue">并列</Tag> : '-' },
    { title: '选手状态', dataIndex: 'contestantStatus', width: 100, render: (_, row) => row.contestantStatus === 'absent' ? <Tag color="default">缺考</Tag> : <Tag color="green">完赛</Tag> },
    { title: '完整排序键', dataIndex: 'tieKey', ellipsis: true, copyable: true },
  ];

  if (!canViewEvent || (!canViewScores && !canViewRanking)) {
    return <Result status="403" title="暂无成绩与排名查看权限" subTitle="服务端权限仍是最终裁决，请联系管理员配置赛事成绩权限。" />;
  }
  if (!canReadDimensions) {
    return <Result status="403" title="赛事上下文权限不完整" subTitle="查看成绩还需要赛段、赛事工种和成绩规则查看权限。" />;
  }
  if (eventQuery.isLoading || stagesQuery.isLoading || workTypesQuery.isLoading) {
    return <Spin className="competition-loading" />;
  }
  if (!eventQuery.data) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回赛事列表</Button>} />;
  }

  const content = (
    <>
      <Card bordered={false} className="competition-filter-card competition-score-context">
        <div className="competition-filter-card__head">
          <div>
            <Typography.Title level={4}>成绩计算范围</Typography.Title>
            <Typography.Text type="secondary">所有成绩、总分和排名均按赛事 + 赛段 + 工种 + 已发布规则版本隔离。</Typography.Text>
          </div>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              scoreSourcesQuery.refetch();
              totalSnapshotsQuery.refetch();
              rankingSnapshotsQuery.refetch();
            }}
          >
            刷新
          </Button>
        </div>
        <Space wrap size={12}>
          <Select
            value={stageId}
            placeholder="选择赛段"
            style={{ width: 220 }}
            options={(stagesQuery.data?.items || []).map((stage) => ({ value: idValue(stage.id), label: stage.name }))}
            onChange={(value) => { setStageId(value); setScoreRuleId(undefined); setSelectedSnapshotId(undefined); }}
          />
          <Select
            value={eventWorkTypeId}
            placeholder="选择工种"
            style={{ width: 240 }}
            options={(workTypesQuery.data || []).map((workType) => ({
              value: idValue(workType.id),
              label: workType.displayName || workType.workTypeName || idValue(workType.id),
            }))}
            onChange={(value) => { setEventWorkTypeId(value); setScoreRuleId(undefined); setSelectedSnapshotId(undefined); }}
          />
          <Select
            value={scoreRuleId}
            placeholder="选择已发布规则"
            loading={rulesQuery.isLoading}
            style={{ width: 280 }}
            options={(rulesQuery.data || []).filter((rule) => rule.status === 'published').map((rule) => ({
              value: idValue(rule.id),
              label: `V${rule.ruleVersion} · ${rule.scoringSystem} · ${scoreValue(rule.theoryWeight)}%/${scoreValue(rule.simulationPracticalWeight)}%`,
            }))}
            onChange={(value) => { setScoreRuleId(value); setSelectedSnapshotId(undefined); }}
          />
        </Space>
      </Card>

      {canReadReadiness && readinessQuery.data && (
        <Alert
          className="competition-score-readiness"
          type={readinessQuery.data.ready ? 'success' : 'warning'}
          showIcon
          message={readinessQuery.data.ready ? '成绩域已就绪' : '成绩域尚未就绪'}
          description={readinessQuery.data.checks.filter((check) => !check.passed).map((check) => check.message).join('；') || '赛段、工种、资格选手和已发布规则版本均已通过校验。'}
        />
      )}

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          canViewScores ? {
            key: 'sources',
            label: '成绩来源',
            children: (
              <Card bordered={false} className="competition-table-card">
                <ProTable<CompetitionScoreSource>
                  rowKey={(row) => idValue(row.id)}
                  search={false}
                  options={false}
                  loading={scoreSourcesQuery.isFetching}
                  dataSource={scoreSourcesQuery.data?.items || []}
                  columns={sourceColumns}
                  scroll={{ x: 1300 }}
                  pagination={{ defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: (total) => `共 ${total} 条来源修订` }}
                  headerTitle={`当前规则共有 ${selectedRuleSources.length} 条来源修订`}
                  toolBarRender={() => canCalculate ? [
                    <Button
                      key="calculate"
                      type="primary"
                      icon={<CalculatorOutlined />}
                      loading={calculateMutation.isPending}
                      disabled={!contextReady || calculationReadyContestants.length === 0 || readinessQuery.data?.ready === false}
                      onClick={() => calculateMutation.mutate()}
                    >
                      计算就绪选手总分（{calculationReadyContestants.length}）
                    </Button>,
                  ] : []}
                />
              </Card>
            ),
          } : undefined,
          canViewScores ? {
            key: 'totals',
            label: '总分快照',
            children: (
              <Card bordered={false} className="competition-table-card">
                <ProTable<CompetitionTotalScoreSnapshot>
                  rowKey={(row) => idValue(row.id)}
                  search={false}
                  options={false}
                  loading={totalSnapshotsQuery.isFetching}
                  dataSource={totalSnapshotsQuery.data?.items || []}
                  columns={totalColumns}
                  scroll={{ x: 1300 }}
                  pagination={{ defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: (total) => `共 ${total} 条计算快照` }}
                  headerTitle="总分重算生成新版本，不覆盖历史快照"
                />
              </Card>
            ),
          } : undefined,
          canViewRanking ? {
            key: 'rankings',
            label: '排名快照',
            children: (
              <>
                <Card bordered={false} className="competition-table-card">
                  <ProTable<CompetitionRankingSnapshot>
                    rowKey={(row) => idValue(row.id)}
                    search={false}
                    options={false}
                    loading={rankingSnapshotsQuery.isFetching}
                    dataSource={rankingSnapshotsQuery.data?.items || []}
                    columns={snapshotColumns}
                    pagination={{ pageSize: 8, showSizeChanger: false, showTotal: (total) => `共 ${total} 个快照版本` }}
                    headerTitle="不可变排名快照"
                    toolBarRender={() => canGenerateRanking ? [
                      <Button
                        key="generate"
                        type="primary"
                        icon={<TrophyOutlined />}
                        disabled={!contextReady || !(totalSnapshotsQuery.data?.items || []).some((row) => row.currentFlag === 1 && row.confirmationStatus === 'confirmed')}
                        onClick={openRankingDraft}
                      >
                        生成排名快照
                      </Button>,
                    ] : []}
                  />
                </Card>
                {selectedSnapshotId && (
                  <Card bordered={false} className="competition-table-card">
                    <ProTable<CompetitionRankingEntry>
                      rowKey={(row) => idValue(row.id)}
                      search={false}
                      options={false}
                      loading={rankingEntriesQuery.isFetching}
                      dataSource={rankingEntryPageItems(rankingEntriesQuery.data)}
                      columns={entryColumns}
                      pagination={false}
                      headerTitle={`榜单明细 · 快照 ${selectedSnapshotId}`}
                    />
                  </Card>
                )}
              </>
            ),
          } : undefined,
        ].filter(Boolean) as NonNullable<Parameters<typeof Tabs>[0]['items']>}
      />

      <Modal
        title="核对并生成排名快照"
        open={rankingDraftOpen}
        width={1120}
        destroyOnClose
        confirmLoading={generateRankingMutation.isPending}
        okText="生成不可变快照"
        okButtonProps={{
          disabled: rankingDraftRows.length === 0 || !completedTieKeysValid || !rankingReviewed,
        }}
        onCancel={() => setRankingDraftOpen(false)}
        onOk={() => generateRankingMutation.mutate()}
      >
        <Alert
          type="warning"
          showIcon
          message="初始顺序只按总分降序，系统不会在前端猜测未冻结的二级排序规则"
          description="请按本场已确认规则调整完赛选手顺序，并为每名完赛选手填写完整排序键。完整排序字段完全相同的选手填写相同键，后端据此执行 1334；缺考选手保留展示但不占名次。"
        />
        <Space className="competition-ranking-draft-actions" wrap>
          <Input
            value={absentContestantId}
            placeholder="输入缺考选手成员 ID"
            onChange={(event) => setAbsentContestantId(event.target.value)}
            onPressEnter={addAbsentContestant}
            style={{ width: 260 }}
          />
          <Button icon={<PlusOutlined />} onClick={addAbsentContestant}>加入缺考选手</Button>
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
            { title: '顺序', width: 65, render: (_, __, index) => index + 1 },
            { title: '选手成员 ID', dataIndex: 'contestantMemberId', width: 170 },
            { title: '状态', width: 80, render: (_, row) => row.contestantStatus === 'absent' ? <Tag>缺考</Tag> : <Tag color="green">完赛</Tag> },
            { title: '总分', width: 80, render: (_, row) => scoreValue(row.totalScoreSnapshot?.totalScore) },
            { title: '理论', width: 75, render: (_, row) => scoreValue(row.totalScoreSnapshot?.theoryScore) },
            { title: '仿真', width: 75, render: (_, row) => scoreValue(row.totalScoreSnapshot?.simulationScore) },
            { title: '实操', width: 75, render: (_, row) => scoreValue(row.totalScoreSnapshot?.practicalScore) },
            {
              title: '完整排序键',
              width: 250,
              render: (_, row) => row.contestantStatus === 'absent' ? '-' : (
                <Input
                  value={row.tieKey}
                  status={row.tieKey?.trim() ? undefined : 'error'}
                  placeholder="按本场规则填写，完全并列者相同"
                  onChange={(event) => {
                    const tieKey = event.target.value;
                    setRankingDraftRows((rows) => rows.map((item) => item.key === row.key ? { ...item, tieKey } : item));
                    setRankingReviewed(false);
                  }}
                />
              ),
            },
            {
              title: '调整',
              width: 115,
              render: (_, row, index) => (
                <Space size={2}>
                  <Button size="small" icon={<ArrowUpOutlined />} disabled={index === 0} onClick={() => moveRankingRow(index, -1)} />
                  <Button size="small" icon={<ArrowDownOutlined />} disabled={index === rankingDraftRows.length - 1} onClick={() => moveRankingRow(index, 1)} />
                  {row.contestantStatus === 'absent' && (
                    <Button danger size="small" onClick={() => setRankingDraftRows((rows) => rows.filter((item) => item.key !== row.key))}>移除</Button>
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
    </>
  );
  return embedded ? content : <CompetitionEventFrame event={eventQuery.data} activeSection="scores" onNavigate={onNavigate}>{content}</CompetitionEventFrame>;
}

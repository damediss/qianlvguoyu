import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  ThunderboltOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Empty,
  Input,
  List,
  Modal,
  Progress,
  Result,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import {
  autoAllocateCompetitionStagePaperAssignments,
  confirmCompetitionStagePaperAssignments,
  getCompetitionStagePaperAssignmentSummary,
  listCompetitionStagePaperAssignmentPlayers,
  saveCompetitionStagePaperAssignments,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionId,
  CompetitionStagePaperAssignmentContentType,
  CompetitionStagePaperAssignmentItem,
  CompetitionStagePaperAssignmentPlayer,
  CompetitionStagePaperAssignmentSummary,
} from '../../types/competition';
import './CompetitionPlayerPaperAssignmentPage.css';

const UNASSIGNED = '__unassigned__';

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

function playerKey(player: CompetitionStagePaperAssignmentPlayer) {
  return idValue(player.contestantProfileId);
}

function paperLabel(paper?: { paperCode?: string | null; title?: string | null }) {
  if (!paper) return '未分配';
  return paper.title ? `${paper.paperCode}卷 · ${paper.title}` : `${paper.paperCode}卷`;
}

function percentValue(value: number | string | undefined) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function CompetitionPlayerPaperAssignmentPage({
  stageId,
  onDirtyChange,
  contentType = 'theory',
}: {
  stageId: CompetitionId;
  onDirtyChange?: (dirty: boolean) => void;
  contentType?: CompetitionStagePaperAssignmentContentType;
}) {
  const assignmentContentType = contentType;
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:stage-paper-assignment:view');
  const canManage = permissions.includes('competition:stage-paper-assignment:manage');
  const canConfirm = permissions.includes('competition:stage-paper-assignment:confirm');
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('table');
  const [keyword, setKeyword] = useState('');
  const [orgId, setOrgId] = useState<string>();
  const [paperSnapshotId, setPaperSnapshotId] = useState<string>();
  const [assignmentStatus, setAssignmentStatus] = useState<'assigned' | 'unassigned'>();
  const [page, setPage] = useState(1);
  const [selectedProfileIds, setSelectedProfileIds] = useState<string[]>([]);
  const [batchPaperId, setBatchPaperId] = useState<string>();
  const [visualPaperId, setVisualPaperId] = useState<string>();
  const [visualSearch, setVisualSearch] = useState('');
  const [visualSelectedIds, setVisualSelectedIds] = useState<string[]>([]);
  const [draftAssignments, setDraftAssignments] = useState<Record<string, string | null>>({});
  const [baselineAssignments, setBaselineAssignments] = useState<Record<string, string | null>>({});

  const summaryQuery = useQuery({
    queryKey: ['competition-stage-paper-assignment-summary', stageId, assignmentContentType],
    queryFn: () => assignmentContentType === 'practical'
      ? getCompetitionStagePaperAssignmentSummary(stageId, assignmentContentType)
      : getCompetitionStagePaperAssignmentSummary(stageId),
    enabled: canView,
  });
  const visualizationQuery = useQuery({
    queryKey: ['competition-stage-paper-assignment-visualization', stageId, assignmentContentType],
    queryFn: () => assignmentContentType === 'practical'
      ? listCompetitionStagePaperAssignmentPlayers(stageId, assignmentContentType)
      : listCompetitionStagePaperAssignmentPlayers(stageId),
    enabled: canView,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
  const locked = Boolean(summaryQuery.data?.formalExamLocked);
  const canManageEffective = canManage && !locked;

  useEffect(() => {
    if (!visualizationQuery.data) return;
    const next: Record<string, string | null> = {};
    visualizationQuery.data.forEach((player) => {
      next[playerKey(player)] = idValue(player.paperSnapshotId) || null;
    });
    setBaselineAssignments(next);
    setDraftAssignments(next);
    setSelectedProfileIds([]);
    setVisualSelectedIds([]);
  }, [assignmentContentType, stageId, visualizationQuery.data]);

  const papers = summaryQuery.data?.papers || [];
  const paperById = useMemo(
    () => new Map(papers.map((paper) => [idValue(paper.id), paper])),
    [papers],
  );
  const paperOptions = useMemo(
    () => papers.map((paper) => ({
      label: `${paper.paperCode}卷${paper.title ? ` · ${paper.title}` : ''}`,
      value: idValue(paper.id),
    })),
    [papers],
  );

  useEffect(() => {
    if (!papers.length) {
      setVisualPaperId(undefined);
      return;
    }
    if (!visualPaperId || !papers.some((paper) => idValue(paper.id) === visualPaperId)) {
      setVisualPaperId(idValue(papers[0].id));
    }
  }, [papers, visualPaperId]);

  const effectivePlayers = useMemo(() => {
    return (visualizationQuery.data || []).map((player) => {
      const key = playerKey(player);
      const hasDraft = Object.prototype.hasOwnProperty.call(draftAssignments, key);
      const nextPaperId = hasDraft ? draftAssignments[key] : idValue(player.paperSnapshotId) || null;
      const paper = nextPaperId ? paperById.get(nextPaperId) : undefined;
      return {
        ...player,
        paperSnapshotId: nextPaperId,
        paperCode: paper?.paperCode || null,
        paperTitle: paper?.title || null,
        assignmentStatus: nextPaperId ? 'assigned' : 'unassigned',
      } as CompetitionStagePaperAssignmentPlayer;
    });
  }, [draftAssignments, paperById, visualizationQuery.data]);

  const effectivePlayerById = useMemo(
    () => new Map(effectivePlayers.map((player) => [playerKey(player), player])),
    [effectivePlayers],
  );

  const effectiveSummary = useMemo<CompetitionStagePaperAssignmentSummary | undefined>(() => {
    const source = summaryQuery.data;
    if (!source) return undefined;
    const counts = new Map<string, number>();
    let assignedCount = 0;
    effectivePlayers.forEach((player) => {
      const paperId = idValue(player.paperSnapshotId);
      if (paperId) {
        assignedCount += 1;
        counts.set(paperId, (counts.get(paperId) || 0) + 1);
      }
    });
    const eligibleCount = effectivePlayers.length || source.eligibleCount;
    return {
      ...source,
      assignedCount,
      unassignedCount: Math.max(0, eligibleCount - assignedCount),
      confirmed: source.confirmed && assignedCount === eligibleCount,
      papers: source.papers.map((paper) => {
        const assigned = counts.get(idValue(paper.id)) || 0;
        return {
          ...paper,
          assignedCount: assigned,
          percentage: eligibleCount ? Math.round((assigned / eligibleCount) * 10000) / 100 : 0,
        };
      }),
    };
  }, [effectivePlayers, summaryQuery.data]);

  const dirty = useMemo(() => {
    const keys = new Set([...Object.keys(baselineAssignments), ...Object.keys(draftAssignments)]);
    return [...keys].some((key) => (baselineAssignments[key] || null) !== (draftAssignments[key] || null));
  }, [baselineAssignments, draftAssignments]);

  useEffect(() => { onDirtyChange?.(dirty); }, [dirty, onDirtyChange]);

  const organizationOptions = useMemo(() => {
    const byId = new Map<string, string>();
    effectivePlayers.forEach((player) => {
      const value = idValue(player.orgId);
      if (value) byId.set(value, player.orgName || value);
    });
    return [...byId.entries()]
      .sort((left, right) => left[1].localeCompare(right[1], 'zh-CN'))
      .map(([value, label]) => ({ value, label }));
  }, [effectivePlayers]);

  const assignmentItems = (): CompetitionStagePaperAssignmentItem[] => effectivePlayers
    .filter((player) => (baselineAssignments[playerKey(player)] || null) !== (idValue(player.paperSnapshotId) || null))
    .map((player) => ({
    contestantProfileId: player.contestantProfileId,
    paperSnapshotId: idValue(player.paperSnapshotId) || null,
  }));

  const acceptLocalChanges = (items: CompetitionStagePaperAssignmentItem[]) => {
    const next = { ...draftAssignments };
    items.forEach((item) => {
      next[idValue(item.contestantProfileId)] = idValue(item.paperSnapshotId) || null;
    });
    setDraftAssignments(next);
  };

  const invalidateAssignmentQueries = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['competition-stage-paper-assignment-summary', stageId, assignmentContentType] }),
      queryClient.invalidateQueries({ queryKey: ['competition-stage-paper-assignment-players', stageId, assignmentContentType] }),
      queryClient.invalidateQueries({ queryKey: ['competition-stage-paper-assignment-visualization', stageId, assignmentContentType] }),
    ]);
  };

  const autoAllocateMutation = useMutation({
    mutationFn: () => assignmentContentType === 'practical'
      ? autoAllocateCompetitionStagePaperAssignments(stageId, assignmentContentType)
      : autoAllocateCompetitionStagePaperAssignments(stageId),
    onSuccess: async () => {
      await invalidateAssignmentQueries();
      message.success('已按平均随机策略重新分配试卷');
    },
    onError: (error) => message.error(errorText(error, '自动分配失败，请刷新后重试')),
  });
  const saveMutation = useMutation({
    mutationFn: () => assignmentContentType === 'practical'
      ? saveCompetitionStagePaperAssignments(stageId, assignmentItems(), assignmentContentType)
      : saveCompetitionStagePaperAssignments(stageId, assignmentItems()),
    onSuccess: async () => {
      const next = { ...draftAssignments };
      setBaselineAssignments(next);
      await invalidateAssignmentQueries();
      message.success('选手试卷配置已保存');
    },
    onError: (error) => message.error(errorText(error, '保存试卷配置失败，请刷新后重试')),
  });
  const confirmMutation = useMutation({
    mutationFn: () => assignmentContentType === 'practical'
      ? confirmCompetitionStagePaperAssignments(stageId, assignmentItems(), assignmentContentType)
      : confirmCompetitionStagePaperAssignments(stageId, assignmentItems()),
    onSuccess: async () => {
      const next = { ...draftAssignments };
      setBaselineAssignments(next);
      await invalidateAssignmentQueries();
      message.success('选手试卷配置已确认');
    },
    onError: (error) => message.error(errorText(error, '确认试卷配置失败，请先检查未分配选手')),
  });
  const tableRows = effectivePlayers.filter((player) => {
    const search = keyword.trim().toLowerCase();
    return (!search || [player.name, player.contestantNo]
      .some((value) => value?.toLowerCase().includes(search)))
      && (!orgId || idValue(player.orgId) === orgId)
      && (!paperSnapshotId || idValue(player.paperSnapshotId) === paperSnapshotId)
      && (!assignmentStatus || player.assignmentStatus === assignmentStatus);
  });
  const tableColumns: ColumnsType<CompetitionStagePaperAssignmentPlayer> = [
    { title: '姓名', dataIndex: 'name', width: 110, render: (value) => value || '-' },
    { title: '选手编号', width: 150, dataIndex: 'contestantNo', render: (value) => value || '-' },
    { title: '所属单位', dataIndex: 'orgName', ellipsis: true, render: (value) => value || '-' },
    { title: '工种', dataIndex: 'workTypeName', width: 140, render: (value) => value || '-' },
    {
      title: '技能等级', width: 135,
      render: (_, row) => row.levelName ? `${row.levelName}（${row.levelCode || '-'}）` : row.rosterLevel || '-',
    },
    {
      title: '当前试卷', width: 180,
      render: (_, row) => row.paperSnapshotId
        ? <Tag color="purple">{paperLabel(paperById.get(idValue(row.paperSnapshotId)))}</Tag>
        : <Tag>未分配</Tag>,
    },
    {
      title: '分配状态', width: 100,
      render: (_, row) => row.assignmentStatus === 'assigned'
        ? <Badge status="success" text="已分配" />
        : <Badge status="warning" text="未分配" />,
    },
    ...(assignmentContentType === 'practical' ? [{
      title: '考评表', width: 160,
      render: (_: unknown, row: CompetitionStagePaperAssignmentPlayer) => row.assessmentSheetId
        ? <Tag color="orange">{row.assessmentSheetCode || '已同步绑定'}</Tag>
        : <Tag color="warning">待保存同步</Tag>,
    }] : []),
    {
      title: '操作', width: 220,
      render: (_, row) => canManageEffective ? (
        <Select
          size="small"
          style={{ width: 190 }}
          value={idValue(row.paperSnapshotId) || UNASSIGNED}
          options={[{ label: '未分配', value: UNASSIGNED }, ...paperOptions]}
          onChange={(value) => acceptLocalChanges([{
            contestantProfileId: row.contestantProfileId,
            paperSnapshotId: value === UNASSIGNED ? null : value,
          }])}
        />
      ) : <Typography.Text type="secondary">已锁定</Typography.Text>,
    },
  ];

  const selectBatchPaper = () => {
    if (!batchPaperId) {
      message.warning('请选择目标试卷或取消分配');
      return;
    }
    if (!selectedProfileIds.length) {
      message.warning('请先勾选需要调整的选手');
      return;
    }
    const items = selectedProfileIds
      .map((profileId) => effectivePlayerById.get(profileId))
      .filter((player): player is CompetitionStagePaperAssignmentPlayer => Boolean(player))
      .map((player) => ({
        contestantProfileId: player.contestantProfileId,
        paperSnapshotId: batchPaperId === UNASSIGNED ? null : batchPaperId,
      }));
    if (!items.length) return;
    acceptLocalChanges(items);
    setSelectedProfileIds([]);
    setBatchPaperId(undefined);
  };

  const currentVisualPaperPlayers = effectivePlayers.filter(
    (player) => idValue(player.paperSnapshotId) === visualPaperId,
  );
  const currentVisualPaperPlayerIds = new Set(currentVisualPaperPlayers.map(playerKey));
  const visualKeyword = visualSearch.trim().toLowerCase();
  const visualLeftPlayers = effectivePlayers.filter((player) => {
    if (currentVisualPaperPlayerIds.has(playerKey(player))) return false;
    if (!visualKeyword) return true;
    return [player.name, player.contestantNo, player.orgName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(visualKeyword));
  });
  const visualRightPlayers = currentVisualPaperPlayers.filter((player) => {
    if (!visualKeyword) return true;
    return [player.name, player.contestantNo, player.orgName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(visualKeyword));
  });

  const toggleVisualPlayer = (profileId: string) => {
    setVisualSelectedIds((current) => current.includes(profileId)
      ? current.filter((id) => id !== profileId)
      : [...current, profileId]);
  };
  const moveVisualPlayers = (direction: 'in' | 'out') => {
    const source = direction === 'in' ? visualLeftPlayers : visualRightPlayers;
    const sourceIds = new Set(source.map(playerKey));
    const selected = visualSelectedIds.filter((id) => sourceIds.has(id));
    if (!selected.length) {
      message.warning(direction === 'in' ? '请先从左侧选择选手' : '请先从右侧选择选手');
      return;
    }
    acceptLocalChanges(selected.map((profileId) => ({
      contestantProfileId: profileId,
      paperSnapshotId: direction === 'in' ? visualPaperId : null,
    })));
    setVisualSelectedIds([]);
  };

  if (!canView) return <Result status="403" title="暂无选手试卷配置查看权限" />;
  if (summaryQuery.isLoading || visualizationQuery.isLoading) return <Spin className="competition-loading" />;
  if (summaryQuery.isError || visualizationQuery.isError) {
    return <Result status="error" title="选手试卷配置加载失败" extra={<Button onClick={() => { summaryQuery.refetch(); visualizationQuery.refetch(); }}>重新加载</Button>} />;
  }

  const summary = effectiveSummary;
  const autoAllocate = () => Modal.confirm({
    title: '重新自动分配试卷？',
    content: '该操作会覆盖当前赛段的现有分卷结果，未保存的人工调整也会被覆盖。',
    okText: '重新分配',
    cancelText: '取消',
    onOk: () => autoAllocateMutation.mutateAsync(),
  });
  const confirmAssignment = () => Modal.confirm({
    title: '确认选手试卷配置？',
    content: '确认将保存当前分卷结果并标记为已确认；后续调整保存后需重新确认。',
    okText: '确认配置',
    cancelText: '取消',
    onOk: () => confirmMutation.mutateAsync(),
  });

  return (
    <div className="competition-paper-assignment-page">
      <div className="competition-paper-assignment-page__header">
        <div>
          <Typography.Title level={4}>
            {assignmentContentType === 'practical' ? '实操选手配置 · 试卷分配' : '选手配置 · 试卷分配'}
          </Typography.Title>
          <Typography.Text type="secondary">
            {assignmentContentType === 'practical'
              ? '为当前赛段符合工种和技能等级要求的选手分配已绑定实操试卷；保存时系统同步绑定该试卷的考评表。'
              : '为当前赛段符合工种和技能等级要求的选手分配已配置试卷；已导入待激活选手也会出现在列表中。'}
          </Typography.Text>
        </div>
        <Space>
          {summary?.confirmed && !dirty && <Tag color="success" icon={<CheckCircleOutlined />}>已确认</Tag>}
          <Button icon={<ReloadOutlined />} onClick={() => {
            if (dirty) Modal.confirm({ title: '放弃未保存的调整并刷新？', onOk: invalidateAssignmentQueries });
            else void invalidateAssignmentQueries();
          }}>刷新</Button>
        </Space>
      </div>

      {summary && summary.eligibleCount === 0 && (
        <Alert
          style={{ marginBottom: 16 }}
          type="info"
          showIcon
          message="当前赛段暂无选手"
          description="选手管理中已导入的名单会出现在这里，包括待激活选手。若仍为空，请确认该选手的赛事工种、技能等级与当前赛段一致，并先在竞赛计划中发布赛事以便选手激活。"
        />
      )}
      {locked && (
        <Alert
          style={{ marginBottom: 16 }}
          type="warning"
          showIcon
          message="正式比赛数据已生成，当前试卷分配已锁定。"
          description={assignmentContentType === 'practical'
            ? '正式实操数据将使用生成时的运行快照。如需核对选手试卷，请从竞赛菜单进入实操比赛控制台。'
            : '正式比赛将使用生成时的运行快照。如需核对选手试卷，请从竞赛菜单进入理论比赛控制台。'}
        />
      )}
      {summary && (
        <>
          <Card size="small" className="competition-paper-assignment-page__scope">
            <Space wrap split={<Typography.Text type="secondary">/</Typography.Text>}>
              <Typography.Text strong>{summary.stageName}</Typography.Text>
              <Typography.Text>{summary.workTypeName}</Typography.Text>
              <Typography.Text>{summary.levelName}（{summary.levelCode}）</Typography.Text>
            </Space>
          </Card>
          <div className="competition-paper-assignment-page__stats">
            <Card size="small"><Statistic title="符合条件选手" value={summary.eligibleCount} suffix="人" /></Card>
            <Card size="small"><Statistic title="当前配置试卷" value={summary.paperCount} suffix="套" /></Card>
            <Card size="small"><Statistic title="已分配" value={summary.assignedCount} suffix="人" valueStyle={{ color: '#389e0d' }} /></Card>
            <Card size="small"><Statistic title="未分配" value={summary.unassignedCount} suffix="人" valueStyle={{ color: summary.unassignedCount ? '#d46b08' : '#389e0d' }} /></Card>
          </div>
          <Card
            size="small"
            title="分配策略"
            extra={canManageEffective && <Button type="primary" icon={<ThunderboltOutlined />} loading={autoAllocateMutation.isPending} disabled={!summary.paperCount} onClick={autoAllocate}>重新自动分配</Button>}
          >
            <div className="competition-paper-assignment-page__strategy">
              <RadioStrategy />
              <Typography.Text type="secondary">按当前试卷数量平均分配，先随机打散选手，人数不能整除时相邻试卷最多相差 1 人。</Typography.Text>
            </div>
          </Card>
          <Card size="small" title="分配概览" className="competition-paper-assignment-page__overview">
            <div className="competition-paper-assignment-page__paper-grid">
              {(summary.papers || []).map((paper) => (
                <Card key={idValue(paper.id)} size="small" className="competition-paper-assignment-page__paper-card">
                  <div className="competition-paper-assignment-page__paper-card-title">
                    <Typography.Text strong>{paper.paperCode}卷</Typography.Text>
                    <Tag color="purple">v{paper.versionNo}</Tag>
                  </div>
                  <Typography.Text type="secondary" ellipsis>{paper.title || '未命名试卷'}</Typography.Text>
                  <div className="competition-paper-assignment-page__paper-card-count">{paper.assignedCount} <small>人</small></div>
                  <Progress percent={percentValue(paper.percentage)} size="small" format={(value) => `${value}%`} />
                </Card>
              ))}
              {!summary.papers.length && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前赛段尚未配置试卷" />}
            </div>
          </Card>
        </>
      )}

      <Card size="small" className="competition-paper-assignment-page__workspace">
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'table',
              label: <span><UserSwitchOutlined /> 选手列表</span>,
              children: (
                <>
                  <div className="competition-paper-assignment-page__toolbar">
                    <Input
                      allowClear
                      prefix={<SearchOutlined />}
                      placeholder="搜索姓名或选手编号"
                      value={keyword}
                      style={{ width: 250 }}
                      onChange={(event) => { setKeyword(event.target.value); setPage(1); }}
                    />
                    <Select allowClear placeholder="所属单位" style={{ width: 180 }} value={orgId} options={organizationOptions} onChange={(value) => { setOrgId(value); setPage(1); }} />
                    <Select allowClear placeholder="当前试卷" style={{ width: 180 }} value={paperSnapshotId} options={paperOptions} onChange={(value) => { setPaperSnapshotId(value); setPage(1); }} />
                    <Select allowClear placeholder="分配状态" style={{ width: 140 }} value={assignmentStatus} options={[{ value: 'assigned', label: '已分配' }, { value: 'unassigned', label: '未分配' }]} onChange={(value) => { setAssignmentStatus(value); setPage(1); }} />
                    {canManageEffective && (
                    <Space.Compact>
                      <Select placeholder="批量分配到" style={{ width: 180 }} value={batchPaperId} options={[{ label: '取消分配', value: UNASSIGNED }, ...paperOptions]} onChange={setBatchPaperId} />
                      <Button disabled={!selectedProfileIds.length} onClick={selectBatchPaper}>批量调整</Button>
                    </Space.Compact>
                    )}
                  </div>
                  <Table<CompetitionStagePaperAssignmentPlayer>
                    rowKey={(row) => playerKey(row)}
                    size="small"
                    scroll={{ x: 1250 }}
                    loading={visualizationQuery.isFetching}
                    dataSource={tableRows}
                    columns={tableColumns}
                    rowSelection={canManageEffective ? {
                      selectedRowKeys: selectedProfileIds,
                      preserveSelectedRowKeys: true,
                      onChange: (keys) => setSelectedProfileIds(keys.map(String)),
                    } : undefined}
                    pagination={{
                      current: page,
                      pageSize: 20,
                      total: tableRows.length,
                      showSizeChanger: false,
                      onChange: setPage,
                    }}
                    locale={{ emptyText: '暂无选手。已导入待激活选手应显示在此，请核对工种、等级，并确认赛事已发布。' }}
                  />
                </>
              ),
            },
            {
              key: 'visual',
              label: <span><UserSwitchOutlined /> 可视化调整</span>,
              children: (
                <div className="competition-paper-assignment-page__visual">
                  <div className="competition-paper-assignment-page__visual-tabs">
                    <Typography.Text type="secondary">当前调整试卷：</Typography.Text>
                    {papers.map((paper) => (
                      <Button
                        key={idValue(paper.id)}
                        size="small"
                        type={visualPaperId === idValue(paper.id) ? 'primary' : 'default'}
                        onClick={() => { setVisualPaperId(idValue(paper.id)); setVisualSelectedIds([]); }}
                      >{paper.paperCode}卷</Button>
                    ))}
                  </div>
                  {!papers.length ? <Empty description="请先完成当前赛段试卷配置" /> : (
                    <div className="competition-paper-assignment-page__visual-grid">
                      <VisualPlayerList
                        title="选手列表"
                        description="选择后移入当前试卷"
                        players={visualLeftPlayers}
                        selectedIds={visualSelectedIds}
                        onToggle={toggleVisualPlayer}
                        search={visualSearch}
                        onSearch={setVisualSearch}
                        disabled={!canManageEffective}
                      />
                      {canManageEffective && (
                      <div className="competition-paper-assignment-page__visual-actions">
                        <Button type="primary" icon={<ArrowRightOutlined />} onClick={() => moveVisualPlayers('in')}>移入</Button>
                        <Button icon={<ArrowLeftOutlined />} onClick={() => moveVisualPlayers('out')}>移出</Button>
                      </div>
                      )}
                      <VisualPlayerList
                        title={paperLabel(visualPaperId ? paperById.get(visualPaperId) : undefined)}
                        description={`当前 ${currentVisualPaperPlayers.length} 人`}
                        players={visualRightPlayers}
                        selectedIds={visualSelectedIds}
                        onToggle={toggleVisualPlayer}
                        emptyDescription="当前试卷暂无选手"
                        disabled={!canManageEffective}
                      />
                    </div>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>

      <div className="competition-paper-assignment-page__footer">
        <Typography.Text type={dirty ? 'warning' : 'secondary'}>{dirty ? '有未保存的人工调整' : '当前配置已保存'}</Typography.Text>
        <Space>
          {!locked && (
            <>
              <Button icon={<SaveOutlined />} disabled={!canManageEffective || !dirty} loading={saveMutation.isPending} onClick={() => saveMutation.mutate()}>保存</Button>
              <Button type="primary" icon={<CheckCircleOutlined />} disabled={!canManageEffective || !canConfirm || !summary || !summary.eligibleCount || summary.unassignedCount > 0} loading={confirmMutation.isPending} onClick={confirmAssignment}>确认配置</Button>
            </>
          )}
        </Space>
      </div>
    </div>
  );
}

function RadioStrategy() {
  return (
    <div className="competition-paper-assignment-page__radio-strategy">
      <span className="competition-paper-assignment-page__radio-dot" />
      <Typography.Text strong>平均随机分配</Typography.Text>
    </div>
  );
}

function VisualPlayerList({
  title,
  description,
  players,
  selectedIds,
  onToggle,
  search,
  onSearch,
  emptyDescription = '暂无选手',
  disabled = false,
}: {
  title: string;
  description: string;
  players: CompetitionStagePaperAssignmentPlayer[];
  selectedIds: string[];
  onToggle: (profileId: string) => void;
  search?: string;
  onSearch?: (value: string) => void;
  emptyDescription?: string;
  disabled?: boolean;
}) {
  return (
    <Card size="small" className="competition-paper-assignment-page__visual-card" title={title} extra={<Typography.Text type="secondary">{description}</Typography.Text>}>
      {onSearch && <Input allowClear size="small" prefix={<SearchOutlined />} placeholder="筛选选手" value={search} onChange={(event) => onSearch(event.target.value)} style={{ marginBottom: 8 }} />}
      <List
        size="small"
        bordered
        className="competition-paper-assignment-page__visual-list"
        dataSource={players}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyDescription} /> }}
        renderItem={(player) => {
          const key = playerKey(player);
          return (
            <List.Item
              onClick={() => { if (!disabled) onToggle(key); }}
              className="competition-paper-assignment-page__visual-item"
            >
              {!disabled && (
                <Checkbox checked={selectedIds.includes(key)} onChange={() => onToggle(key)} onClick={(event) => event.stopPropagation()} />
              )}
              <div className="competition-paper-assignment-page__visual-item-main">
                <Typography.Text ellipsis>{player.name || '-'}</Typography.Text>
                <Typography.Text type="secondary" ellipsis>{player.contestantNo || '-'} · {player.orgName || '-'}</Typography.Text>
              </div>
              {player.paperCode && <Tag color="purple">{player.paperCode}卷</Tag>}
            </List.Item>
          );
        }}
      />
    </Card>
  );
}

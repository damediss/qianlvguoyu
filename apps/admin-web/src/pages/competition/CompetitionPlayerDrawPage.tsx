import {
  DeleteOutlined,
  DownloadOutlined,
  OrderedListOutlined,
  ReloadOutlined,
  SendOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Modal,
  Result,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
  pageCompetitionStages,
} from '../../api/competition';
import {
  createCompetitionPlayerDrawTask,
  deleteCompetitionPlayerDrawTask,
  downloadCompetitionPlayerDrawRoster,
  getCompetitionPlayerDrawTask,
  listCompetitionPlayerDrawCandidates,
  pageCompetitionPlayerDrawTasks,
  saveCompetitionPlayerDrawTaskSelection,
} from '../../api/competitionPlayerDraw';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { useAuthStore } from '../../stores/authStore';
import { competitionEventLabel } from '../../utils/competitionEventLabel';
import type { CompetitionEvent, CompetitionId, CompetitionStage } from '../../types/competition';
import type { PageResponse } from '../../types/api';
import type {
  CompetitionPlayerDrawCandidate,
  CompetitionPlayerDrawSlot,
  CompetitionPlayerDrawTask,
} from '../../types/competitionPlayerDraw';
import './CompetitionEventManagementPage.css';

type DrawMode = 'distribution' | 'management';

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  const responseMessage = (error as { response?: { data?: { message?: unknown } } })
    ?.response?.data?.message;
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage;
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function taskStatus(status: string, playerCount: number, drawnCount: number) {
  if (status === 'created') return <Tag color="warning">待分发</Tag>;
  if (status === 'distributed') return <Tag color="processing">待选手抽签</Tag>;
  if (status === 'drawing') return <Tag color="gold">抽签中 {drawnCount}/{playerCount}</Tag>;
  if (status === 'drawn') return <Tag color="success">已完成</Tag>;
  return <Tag>{status || '-'}</Tag>;
}

export function DrawTaskWorkspace({
  mode,
  stageId,
  stageName,
}: {
  mode: DrawMode;
  stageId: string;
  stageName?: string;
}) {
  const paneActive = usePaneActive();
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-draw:view');
  const canCreate = permissions.includes('competition:player-draw:create');
  const canExport = permissions.includes('competition:player-draw:export');
  const canDelete = permissions.includes('competition:player-draw:delete');
  const queryClient = useQueryClient();
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [taskPage, setTaskPage] = useState(1);
  const [activeTaskId, setActiveTaskId] = useState<string>();
  const [detailOpen, setDetailOpen] = useState(false);

  useEffect(() => {
    setSelectedIds([]);
    setTaskPage(1);
    setDetailOpen(false);
    setActiveTaskId(undefined);
  }, [stageId]);

  const candidatesQuery = useQuery({
    queryKey: ['competition-player-draw-candidates', stageId],
    queryFn: () => listCompetitionPlayerDrawCandidates(stageId),
    enabled: canView && mode === 'distribution' && Boolean(stageId),
  });
  const tasksQuery = useQuery({
    queryKey: ['competition-player-draw-tasks', stageId, taskPage],
    queryFn: () => pageCompetitionPlayerDrawTasks(stageId, { page: taskPage, pageSize: 10 }),
    enabled: canView && mode === 'management' && Boolean(stageId),
    refetchInterval: paneActive && mode === 'management' ? 3000 : false,
  });
  const detailQuery = useQuery({
    queryKey: ['competition-player-draw-task', activeTaskId],
    queryFn: () => getCompetitionPlayerDrawTask(activeTaskId!),
    enabled: canView && Boolean(activeTaskId && detailOpen),
    refetchInterval: paneActive && detailOpen ? 3000 : false,
  });

  const distributeMutation = useMutation({
    mutationFn: async () => {
      if (!selectedIds.length) throw new Error('请至少选择一名选手');
      const task = await createCompetitionPlayerDrawTask(stageId);
      try {
        return await saveCompetitionPlayerDrawTaskSelection(task.id, {
          contestantProfileIds: selectedIds,
        });
      } catch (error) {
        try {
          await deleteCompetitionPlayerDrawTask(task.id);
        } catch {
          // 原错误更能说明分发失败原因；空任务可在抽签管理中删除。
        }
        throw error;
      }
    },
    onSuccess: (task) => {
      setSelectedIds([]);
      void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-tasks', stageId] });
      message.success(`已向 ${task.playerCount} 名选手下发抽签任务`);
    },
    onError: (error) => message.error(errorText(error, '抽签任务下发失败')),
  });
  const deleteMutation = useMutation({
    mutationFn: deleteCompetitionPlayerDrawTask,
    onSuccess: () => {
      setDetailOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-tasks', stageId] });
      message.success('抽签任务已删除');
    },
    onError: (error) => message.error(errorText(error, '抽签任务删除失败')),
  });
  const exportMutation = useMutation({
    mutationFn: downloadCompetitionPlayerDrawRoster,
    onSuccess: (blob, taskId) => downloadBlob(blob, `抽签名单-${taskId}.xlsx`),
    onError: (error) => message.error(errorText(error, '抽签名单导出失败')),
  });

  const candidates = candidatesQuery.data || [];
  const candidateColumns = useMemo(() => [
    { title: '选手姓名', dataIndex: 'name', width: 140, render: (value: string) => value || '-' },
    { title: '选手编号', dataIndex: 'contestantNo', width: 140, render: (value: string) => value || '-' },
    { title: '所属企业', dataIndex: 'orgName', width: 220, render: (value: string) => value || '-' },
    { title: '赛事工种', dataIndex: 'workTypeName', width: 180, render: (value: string) => value || '-' },
    {
      title: '技能等级', dataIndex: 'levelName', width: 150,
      render: (value: string, record: CompetitionPlayerDrawCandidate) => (
        value || record.levelCode || record.rosterLevel || '-'
      ),
    },
    {
      title: 'App 状态', dataIndex: 'playerId', width: 120,
      render: (value?: string | null) => value
        ? <Tag color="success">可接收</Tag>
        : <Tag color="warning">未激活</Tag>,
    },
  ], []);

  const detail = detailQuery.data;
  useEffect(() => {
    if (!detail) return;
    queryClient.setQueriesData<PageResponse<CompetitionPlayerDrawTask>>(
      { queryKey: ['competition-player-draw-tasks', stageId] },
      (page) => page ? {
        ...page,
        items: page.items.map((task) => idValue(task.id) === idValue(detail.id)
          ? { ...task, status: detail.status, drawnCount: detail.drawnCount }
          : task),
      } : page,
    );
  }, [detail, queryClient, stageId]);
  const slots = useMemo(() => [...(detail?.slots || [])].sort((left, right) => {
    if (left.appearanceOrder == null && right.appearanceOrder != null) return -1;
    if (left.appearanceOrder != null && right.appearanceOrder == null) return 1;
    return (left.appearanceOrder || left.drawOrder) - (right.appearanceOrder || right.drawOrder);
  }), [detail?.slots]);
  const slotColumns = [
    { title: '选手姓名', dataIndex: 'name', width: 140, render: (value?: string) => value || '-' },
    { title: '选手编号', dataIndex: 'contestantNo', width: 140, render: (value?: string) => value || '-' },
    { title: '所属企业', dataIndex: 'orgName', width: 220, render: (value?: string) => value || '-' },
    { title: '手机号', dataIndex: 'mobile', width: 150, render: (value?: string) => value || '-' },
    {
      title: '抽签状态', dataIndex: 'appearanceOrder', width: 120,
      render: (value?: number | null) => value == null
        ? <Tag>未抽签</Tag>
        : <Tag color="success">已抽签</Tag>,
    },
    {
      title: '所抽号码', dataIndex: 'appearanceOrder', width: 120,
      render: (value?: number | null) => value == null ? '-' : <Typography.Text strong>{value}</Typography.Text>,
    },
  ];

  if (!canView) return <Result status="403" title="暂无抽签任务查看权限" />;
  if (mode === 'distribution') {
    const selectableCandidates = candidates.filter((candidate) => Boolean(candidate.playerId));
    return (
      <Card bordered={false}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message={`当前赛段：${stageName || stageId}`}
            description="选择选手后即下发到 App 的“赛事任务”。每名选手只能抽一次，号码范围为 1 至所选人数。"
          />
          {candidatesQuery.isError && (
            <Alert type="error" showIcon message={errorText(candidatesQuery.error, '选手加载失败')} />
          )}
          <Space wrap>
            <Button
              onClick={() => setSelectedIds(selectableCandidates.map((item) => idValue(item.contestantProfileId)))}
              disabled={!selectableCandidates.length}
            >全选可接收选手</Button>
            <Button onClick={() => setSelectedIds([])} disabled={!selectedIds.length}>清空选择</Button>
            {canCreate && (
              <Button
                type="primary"
                icon={<SendOutlined />}
                loading={distributeMutation.isPending}
                disabled={!selectedIds.length}
                onClick={() => Modal.confirm({
                  title: '确认下发抽签任务？',
                  content: `将向 ${selectedIds.length} 名选手下发任务，号码范围为 1–${selectedIds.length}。`,
                  okText: '确认下发',
                  cancelText: '取消',
                  onOk: () => distributeMutation.mutateAsync(),
                })}
              >下发抽签任务（{selectedIds.length}）</Button>
            )}
          </Space>
          <Table<CompetitionPlayerDrawCandidate>
            rowKey={(record) => idValue(record.contestantProfileId)}
            loading={candidatesQuery.isFetching}
            dataSource={candidates}
            columns={candidateColumns}
            rowSelection={{
              selectedRowKeys: selectedIds,
              getCheckboxProps: (record) => ({ disabled: !record.playerId, name: record.name || undefined }),
              onChange: (keys) => setSelectedIds(keys.map(String)),
            }}
            pagination={{ defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: [10, 20, 50] }}
            scroll={{ x: 980 }}
            locale={{ emptyText: '当前赛段暂无可分发选手' }}
          />
        </Space>
      </Card>
    );
  }

  const taskColumns = [
    { title: '赛段', dataIndex: 'stageName', width: 180, render: (value?: string) => value || '-' },
    { title: '选手数', dataIndex: 'playerCount', width: 90 },
    {
      title: '进度', key: 'progress', width: 100,
      render: (_: unknown, record: CompetitionPlayerDrawTask) => `${record.drawnCount || 0}/${record.playerCount || 0}`,
    },
    {
      title: '任务状态', dataIndex: 'status', width: 160,
      render: (value: string, record: CompetitionPlayerDrawTask) => (
        taskStatus(value, record.playerCount, record.drawnCount || 0)
      ),
    },
    {
      title: '创建时间', dataIndex: 'createdAt', width: 180,
      render: (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '-',
    },
    {
      title: '操作', key: 'actions', width: 260,
      render: (_: unknown, record: CompetitionPlayerDrawTask) => (
        <Space>
          <Button type="link" onClick={() => {
            setActiveTaskId(idValue(record.id));
            setDetailOpen(true);
          }}>查看状态</Button>
          {record.status === 'drawn' && canExport && (
            <Button type="link" icon={<DownloadOutlined />} onClick={() => exportMutation.mutate(record.id)}>
              导出名单
            </Button>
          )}
          {canDelete && record.status !== 'drawing' && (
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              onClick={() => Modal.confirm({
                title: '删除抽签任务？',
                content: '删除后选手 App 中将不再显示该任务。',
                okText: '删除',
                okButtonProps: { danger: true },
                cancelText: '取消',
                onOk: () => deleteMutation.mutateAsync(idValue(record.id)),
              })}
            >删除</Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card bordered={false}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message={`当前赛段：${stageName || stageId}`}
          description="任务进度与明细每 3 秒自动刷新，可及时查看选手抽签状态和所抽号码。"
        />
        {tasksQuery.isError && (
          <Alert type="error" showIcon message={errorText(tasksQuery.error, '抽签任务加载失败')} />
        )}
        <Table<CompetitionPlayerDrawTask>
          rowKey={(record) => idValue(record.id)}
          loading={tasksQuery.isFetching}
          dataSource={tasksQuery.data?.items || []}
          columns={taskColumns}
          pagination={{
            current: taskPage,
            pageSize: 10,
            total: tasksQuery.data?.total || 0,
            showSizeChanger: false,
            onChange: setTaskPage,
          }}
          scroll={{ x: 900 }}
          locale={{ emptyText: '当前赛段暂无抽签任务' }}
        />
      </Space>
      <Modal
        title={`${detail?.stageName || stageName || '抽签任务'} · 选手抽签状态`}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        width={920}
        footer={(
          <Space>
            <Button onClick={() => detailQuery.refetch()} loading={detailQuery.isFetching}>刷新</Button>
            {detail?.status === 'drawn' && activeTaskId && canExport && (
              <Button type="primary" icon={<DownloadOutlined />} onClick={() => exportMutation.mutate(activeTaskId)}>
                导出抽签名单
              </Button>
            )}
            <Button onClick={() => setDetailOpen(false)}>关闭</Button>
          </Space>
        )}
      >
        {detailQuery.isLoading ? <Spin /> : detailQuery.isError
          ? <Result status="error" title="抽签状态加载失败" />
          : (
            <Table<CompetitionPlayerDrawSlot>
              rowKey={(record) => idValue(record.contestantProfileId)}
              dataSource={slots}
              columns={slotColumns}
              pagination={{ defaultPageSize: 10, showSizeChanger: true }}
              scroll={{ x: 900 }}
            />
          )}
      </Modal>
    </Card>
  );
}

function CompetitionPlayerDrawModulePage({ mode }: { mode: DrawMode }) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-draw:view');
  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'player-draw', mode],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];
  const [selectedEventId, setSelectedEventId] = useState<string>();
  const [selectedWorkTypeId, setSelectedWorkTypeId] = useState<string>();
  const [selectedLevelCode, setSelectedLevelCode] = useState<string>();
  const [selectedStageId, setSelectedStageId] = useState<string>();

  useEffect(() => {
    if (!events.length) return setSelectedEventId(undefined);
    if (!selectedEventId || !events.some((event) => idValue(event.id) === selectedEventId)) {
      setSelectedEventId(idValue(events[0].id));
    }
  }, [events, selectedEventId]);
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', selectedEventId, 'player-draw', mode],
    queryFn: () => listCompetitionEventWorkTypes(selectedEventId!),
    enabled: canView && Boolean(selectedEventId),
  });
  const workTypeOptions = useMemo(() => (workTypesQuery.data || [])
    .filter((item) => item.status === 'active')
    .map((item) => ({
      value: idValue(item.id),
      label: item.displayName || item.workTypeName || idValue(item.workTypeId),
    })), [workTypesQuery.data]);
  useEffect(() => {
    if (!workTypesQuery.data) return;
    if (!workTypeOptions.some((item) => item.value === selectedWorkTypeId)) {
      setSelectedWorkTypeId(workTypeOptions[0]?.value);
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
    }
  }, [selectedWorkTypeId, workTypeOptions, workTypesQuery.data]);

  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', selectedEventId, selectedWorkTypeId, 'player-draw', mode],
    queryFn: () => listCompetitionEventWorkTypeLevels(selectedEventId!, selectedWorkTypeId!),
    enabled: canView && Boolean(selectedEventId && selectedWorkTypeId),
  });
  const levelOptions = useMemo(() => (levelsQuery.data || []).map((item) => ({
    value: item.levelCode,
    label: item.levelName ? `${item.levelName}（${item.levelCode}）` : item.levelCode,
  })), [levelsQuery.data]);
  useEffect(() => {
    if (!levelsQuery.data) return;
    if (!levelOptions.some((item) => item.value === selectedLevelCode)) {
      setSelectedLevelCode(levelOptions[0]?.value);
      setSelectedStageId(undefined);
    }
  }, [levelOptions, levelsQuery.data, selectedLevelCode]);

  const stagesQuery = useQuery({
    queryKey: ['competition-stages', 'player-draw', mode, selectedEventId, selectedWorkTypeId, selectedLevelCode],
    queryFn: () => pageCompetitionStages(selectedEventId!, {
      page: 1,
      pageSize: 100,
      eventWorkTypeId: selectedWorkTypeId,
      eventWorkTypeLevelCode: selectedLevelCode,
    }),
    enabled: canView && Boolean(selectedEventId && selectedWorkTypeId && selectedLevelCode),
  });
  const stages = stagesQuery.data?.items || [];
  useEffect(() => {
    if (!stagesQuery.data) return;
    if (!stages.some((stage) => idValue(stage.id) === selectedStageId)) {
      setSelectedStageId(stages[0] ? idValue(stages[0].id) : undefined);
    }
  }, [selectedStageId, stages, stagesQuery.data]);
  const selectedStage = stages.find((stage) => idValue(stage.id) === selectedStageId);

  if (!canView) return <Result status="403" title="暂无抽签任务查看权限" />;
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventsQuery.isError) return <Result status="error" title="赛事任务加载失败" />;

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}>
            {mode === 'distribution' ? <SendOutlined /> : <OrderedListOutlined />}
            {' '}{mode === 'distribution' ? '分发任务' : '抽签管理'}
          </Typography.Title>
          <Typography.Paragraph type="secondary">
            {mode === 'distribution'
              ? '按赛事、工种、等级和赛段筛选选手，将抽签任务下发到选手 App。'
              : '查看各抽签任务的完成状态、选手所抽号码并导出名单。'}
          </Typography.Paragraph>
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => {
          void eventsQuery.refetch();
          if (selectedEventId) void workTypesQuery.refetch();
          if (selectedEventId && selectedWorkTypeId) void levelsQuery.refetch();
          if (selectedEventId && selectedWorkTypeId && selectedLevelCode) void stagesQuery.refetch();
        }}>刷新</Button>
      </section>
      <Card bordered={false} className="competition-filter-card competition-filter-card--compact">
        <Space align="center" size={12} wrap style={{ width: '100%' }}>
          <Typography.Text type="secondary">赛事</Typography.Text>
          <Select
            showSearch optionFilterProp="label" value={selectedEventId}
            options={events.map((event: CompetitionEvent) => ({
              value: idValue(event.id), label: competitionEventLabel(event),
            }))}
            onChange={(value) => {
              setSelectedEventId(value); setSelectedWorkTypeId(undefined);
              setSelectedLevelCode(undefined); setSelectedStageId(undefined);
            }}
            placeholder="选择赛事" style={{ width: 280, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">工种</Typography.Text>
          <Select
            showSearch optionFilterProp="label" value={selectedWorkTypeId}
            options={workTypeOptions} loading={workTypesQuery.isFetching}
            onChange={(value) => {
              setSelectedWorkTypeId(value); setSelectedLevelCode(undefined); setSelectedStageId(undefined);
            }}
            placeholder="选择工种" style={{ width: 220, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">等级</Typography.Text>
          <Select
            showSearch optionFilterProp="label" value={selectedLevelCode}
            options={levelOptions} loading={levelsQuery.isFetching}
            onChange={(value) => { setSelectedLevelCode(value); setSelectedStageId(undefined); }}
            placeholder="选择等级" style={{ width: 180, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">赛段</Typography.Text>
          <Select
            showSearch optionFilterProp="label" value={selectedStageId}
            options={stages.map((stage: CompetitionStage) => ({
              value: idValue(stage.id), label: `${stage.sortOrder}. ${stage.name}`,
            }))}
            loading={stagesQuery.isFetching} onChange={setSelectedStageId}
            placeholder="选择赛段" style={{ width: 220, maxWidth: '100%' }}
          />
        </Space>
      </Card>
      {(workTypesQuery.isError || levelsQuery.isError || stagesQuery.isError) && (
        <Alert type="error" showIcon message="筛选条件加载失败，请刷新后重试" />
      )}
      {selectedStage && (
        <DrawTaskWorkspace
          key={`${mode}-${idValue(selectedStage.id)}`}
          mode={mode}
          stageId={idValue(selectedStage.id)}
          stageName={selectedStage.name}
        />
      )}
    </div>
  );
}

export function CompetitionPlayerDrawDistributionPage() {
  return <CompetitionPlayerDrawModulePage mode="distribution" />;
}

export function CompetitionPlayerDrawManagementPage() {
  return <CompetitionPlayerDrawModulePage mode="management" />;
}

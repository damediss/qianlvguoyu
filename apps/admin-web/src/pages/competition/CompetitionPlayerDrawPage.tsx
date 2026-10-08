import {
  DeleteOutlined,
  DownloadOutlined,
  OrderedListOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Modal,
  Result,
  Select,
  Space,
  Spin,
  Steps,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
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
  drawNextCompetitionPlayer,
  generateCompetitionPlayerDrawOrder,
  getCompetitionPlayerDrawTask,
  listCompetitionPlayerDrawCandidates,
  pageCompetitionPlayerDrawTasks,
  saveCompetitionPlayerDrawTaskSelection,
} from '../../api/competitionPlayerDraw';
import type { CompetitionEvent, CompetitionId, CompetitionStage } from '../../types/competition';
import type {
  CompetitionPlayerDrawCandidate,
  CompetitionPlayerDrawSlot,
  CompetitionPlayerDrawTask,
} from '../../types/competitionPlayerDraw';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';

interface CompetitionPlayerDrawPageProps {
  stageId: string;
  stageName?: string;
  showTitle?: boolean;
}

const MAX_DRAW_PLAYER_COUNT = 5000;

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  const responseMessage = (error as {
    response?: { data?: { message?: unknown } };
  })?.response?.data?.message;
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

function taskStatus(status: string, playerCount: number) {
  if (status === 'created' && playerCount === 0) return <Tag color="warning">待选择选手</Tag>;
  if (status === 'created') return <Tag color="processing">待生成抽签顺序</Tag>;
  if (status === 'ordered') return <Tag color="blue">待逐个抽签</Tag>;
  if (status === 'drawing') return <Tag color="gold">抽签进行中</Tag>;
  if (status === 'drawn') return <Tag color="success">已完成</Tag>;
  return <Tag>{status || '-'}</Tag>;
}

function slotKey(slot: CompetitionPlayerDrawSlot) {
  return `${idValue(slot.contestantProfileId)}-${slot.drawOrder}`;
}

export function CompetitionPlayerDrawPage({
  stageId,
  stageName,
  showTitle = true,
}: CompetitionPlayerDrawPageProps) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-draw:view');
  const canCreate = permissions.includes('competition:player-draw:create');
  const canDraw = permissions.includes('competition:player-draw:draw');
  const canExport = permissions.includes('competition:player-draw:export');
  const canDelete = permissions.includes('competition:player-draw:delete');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [taskPage, setTaskPage] = useState(1);
  const [wizardOpen, setWizardOpen] = useState(false);
  const [wizardStep, setWizardStep] = useState(0);
  const [createdTaskId, setCreatedTaskId] = useState<string>();
  const [savedPlayerCount, setSavedPlayerCount] = useState(0);
  const [activeTaskId, setActiveTaskId] = useState<string>();
  const [deskOpen, setDeskOpen] = useState(false);
  const [lastDrawn, setLastDrawn] = useState<CompetitionPlayerDrawSlot>();
  const drawnIdsRef = useRef<Set<string>>(new Set());
  const queryClient = useQueryClient();

  const candidatesQuery = useQuery({
    queryKey: ['competition-player-draw-candidates', stageId],
    queryFn: () => listCompetitionPlayerDrawCandidates(stageId),
    enabled: canView && Boolean(stageId) && wizardOpen,
  });
  const candidates = candidatesQuery.data || [];
  const tasksQuery = useQuery({
    queryKey: ['competition-player-draw-tasks', stageId, taskPage],
    queryFn: () => pageCompetitionPlayerDrawTasks(stageId, { page: taskPage, pageSize: 10 }),
    enabled: canView && Boolean(stageId),
  });
  const taskDetailQuery = useQuery({
    queryKey: ['competition-player-draw-task', activeTaskId],
    queryFn: () => getCompetitionPlayerDrawTask(activeTaskId!),
    enabled: Boolean(activeTaskId && deskOpen),
  });

  const countMatches = selectedIds.length >= 1 && selectedIds.length <= MAX_DRAW_PLAYER_COUNT;
  const detail = taskDetailQuery.data;
  const detailSlots = useMemo(() => {
    const slots = [...(detail?.slots || [])];
    slots.sort((left, right) => (
      detail?.status === 'drawn'
        ? (left.appearanceOrder || 0) - (right.appearanceOrder || 0)
        : left.drawOrder - right.drawOrder
    ));
    return slots;
  }, [detail?.slots, detail?.status]);
  const nextSlot = detailSlots.find((slot) => slot.appearanceOrder == null);

  useEffect(() => {
    drawnIdsRef.current = new Set(
      (detail?.slots || [])
        .filter((slot) => slot.appearanceOrder != null)
        .map((slot) => idValue(slot.contestantProfileId)),
    );
  }, [detail?.slots]);

  const rememberTask = (task: CompetitionPlayerDrawTask) => {
    const taskId = idValue(task.id);
    queryClient.setQueryData(['competition-player-draw-task', taskId], task);
    void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-tasks', stageId] });
    return taskId;
  };

  const createMutation = useMutation({
    mutationFn: () => {
      if (!stageId) throw new Error('请先选择赛事工种、等级和赛段');
      return createCompetitionPlayerDrawTask(stageId);
    },
    onSuccess: (task) => {
      setCreatedTaskId(rememberTask(task));
      setSavedPlayerCount(task.playerCount || 0);
      setSelectedIds([]);
      setWizardStep(1);
      message.success('抽签任务已创建，请选择选手');
    },
    onError: (error) => message.error(errorText(error, '创建抽签任务失败')),
  });

  const selectionMutation = useMutation({
    mutationFn: (contestantProfileIds: string[]) => {
      if (!createdTaskId) throw new Error('抽签任务尚未创建');
      return saveCompetitionPlayerDrawTaskSelection(createdTaskId, { contestantProfileIds });
    },
    onSuccess: (task) => {
      rememberTask(task);
      setSavedPlayerCount(task.playerCount || 0);
      setWizardStep(2);
      message.success('选手已保存，请确认后生成抽签顺序');
    },
    onError: (error) => message.error(errorText(error, '保存选手失败')),
  });

  const orderMutation = useMutation({
    mutationFn: generateCompetitionPlayerDrawOrder,
    onSuccess: (task) => {
      const taskId = rememberTask(task);
      setActiveTaskId(taskId);
      setLastDrawn(undefined);
      setWizardOpen(false);
      setDeskOpen(true);
      message.success('抽签顺序已生成，请按顺序逐个抽出场顺序');
    },
    onError: (error) => message.error(errorText(error, '生成抽签顺序失败')),
  });

  const drawMutation = useMutation({
    mutationFn: drawNextCompetitionPlayer,
    onSuccess: (task) => {
      const previous = drawnIdsRef.current;
      const drawn = (task.slots || []).find((slot) => (
        slot.appearanceOrder != null && !previous.has(idValue(slot.contestantProfileId))
      ));
      setLastDrawn(drawn);
      rememberTask(task);
      if (drawn) {
        message.success(`${drawn.name || '选手'} 抽到出场顺序 ${drawn.appearanceOrder}`);
      }
    },
    onError: (error) => message.error(errorText(error, '抽签失败')),
  });

  const downloadMutation = useMutation({
    mutationFn: downloadCompetitionPlayerDrawRoster,
    onSuccess: (blob, taskId) => downloadBlob(blob, `抽签出场名单-${taskId}.xlsx`),
    onError: (error) => message.error(errorText(error, '导出名单失败')),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCompetitionPlayerDrawTask,
    onSuccess: (_data, taskId) => {
      message.success('抽签任务已删除');
      if (idValue(taskId) === activeTaskId) setDeskOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-tasks', stageId] });
    },
    onError: (error) => message.error(errorText(error, '删除抽签任务失败')),
  });

  const candidateColumns = useMemo(() => [
    { title: '选手姓名', dataIndex: 'name', width: 140, render: (value: string) => value || '-' },
    { title: '所属企业', dataIndex: 'orgName', width: 220, render: (value: string) => value || '-' },
    { title: '赛事工种', dataIndex: 'workTypeName', width: 180, render: (value: string) => value || '-' },
    {
      title: '技能等级',
      dataIndex: 'levelName',
      width: 150,
      render: (value: string, record: CompetitionPlayerDrawCandidate) => value || record.levelCode || record.rosterLevel || '-',
    },
  ], []);

  const slotColumns = useMemo(() => [
    { title: '抽签顺序', dataIndex: 'drawOrder', width: 110, render: (value: number) => `第 ${value} 位` },
    {
      title: '出场顺序',
      dataIndex: 'appearanceOrder',
      width: 110,
      render: (value?: number | null) => (value == null ? '-' : `第 ${value} 位`),
    },
    { title: '选手姓名', dataIndex: 'name', width: 140, render: (value?: string | null) => value || '-' },
    { title: '所属企业', dataIndex: 'orgName', width: 220, render: (value?: string | null) => value || '-' },
    { title: '手机号', dataIndex: 'mobile', width: 160, render: (value?: string | null) => value || '-' },
    {
      title: '状态',
      key: 'drawState',
      width: 110,
      render: (_: unknown, record: CompetitionPlayerDrawSlot) => (
        record.appearanceOrder == null
          ? <Tag color={nextSlot && slotKey(nextSlot) === slotKey(record) ? 'processing' : 'default'}>
            {nextSlot && slotKey(nextSlot) === slotKey(record) ? '当前抽签' : '等待'}
          </Tag>
          : <Tag color="success">已抽出</Tag>
      ),
    },
  ], [nextSlot]);

  const openDesk = (taskId: CompetitionId) => {
    setActiveTaskId(idValue(taskId));
    setLastDrawn(undefined);
    setDeskOpen(true);
  };

  const confirmDelete = (taskId: CompetitionId, status: string) => {
    const content = status === 'drawn'
      ? '该任务已完成抽签。删除后出场名单将从任务列表中移除。确认删除吗？'
      : status === 'ordered'
        ? '抽签顺序已生成，但尚未逐个抽签。删除后需要重新创建任务。确认删除吗？'
        : '删除后，该未执行的抽签任务将从任务列表中移除。确认删除吗？';
    Modal.confirm({
      title: '删除抽签任务',
      content,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(idValue(taskId)),
    });
  };

  const taskColumns = [
    { title: '选手数', dataIndex: 'playerCount', width: 90 },
    {
      title: '抽签进度',
      key: 'progress',
      width: 120,
      render: (_: unknown, record: CompetitionPlayerDrawTask) => (
        record.status === 'created' ? '-' : `${record.drawnCount || 0}/${record.playerCount || 0}`
      ),
    },
    {
      title: '任务状态',
      dataIndex: 'status',
      width: 150,
      render: (value: string, record: CompetitionPlayerDrawTask) => taskStatus(value, record.playerCount),
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 180, render: (value: string) => value ? value.replace('T', ' ').slice(0, 16) : '-' },
    {
      title: '操作',
      key: 'actions',
      width: 400,
      render: (_: unknown, record: CompetitionPlayerDrawTask) => (
        <Space>
          {record.status === 'created' && record.playerCount === 0 && canCreate && (
            <Button type="link" onClick={() => resumeTask(record)}>选择选手</Button>
          )}
          {record.status === 'created' && record.playerCount > 0 && canDraw && (
            <Button type="link" onClick={() => resumeTask(record)}>生成抽签顺序</Button>
          )}
          {(record.status === 'ordered' || record.status === 'drawing') && canDraw && (
            <Button type="link" onClick={() => openDesk(record.id)}>继续抽签</Button>
          )}
          {record.status === 'drawn' && canView && (
            <Button type="link" onClick={() => openDesk(record.id)}>查看名单</Button>
          )}
          {record.status === 'drawn' && canExport && (
            <Button
              type="link"
              icon={<DownloadOutlined />}
              loading={downloadMutation.isPending && downloadMutation.variables === record.id}
              onClick={() => downloadMutation.mutate(record.id)}
            >导出 Excel</Button>
          )}
          {(record.status === 'created' || record.status === 'ordered' || record.status === 'drawn') && canDelete && (
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              loading={deleteMutation.isPending && deleteMutation.variables === idValue(record.id)}
              onClick={() => confirmDelete(record.id, record.status)}
            >删除</Button>
          )}
        </Space>
      ),
    },
  ];

  const selectedCandidates = useMemo(() => {
    const candidateById = new Map(
      candidates.map((candidate) => [idValue(candidate.contestantProfileId), candidate]),
    );
    return selectedIds
      .map((id) => candidateById.get(id))
      .filter((candidate): candidate is CompetitionPlayerDrawCandidate => Boolean(candidate));
  }, [candidates, selectedIds]);

  const openWizard = () => {
    setCreatedTaskId(undefined);
    setSavedPlayerCount(0);
    setSelectedIds([]);
    setWizardStep(0);
    setWizardOpen(true);
  };

  const resumeTask = (task: CompetitionPlayerDrawTask) => {
    setCreatedTaskId(idValue(task.id));
    setSavedPlayerCount(task.playerCount);
    setSelectedIds([]);
    setWizardStep(task.playerCount === 0 ? 1 : 2);
    setWizardOpen(true);
  };

  const closeWizard = () => {
    setWizardOpen(false);
    setWizardStep(0);
  };

  if (!canView) return <Result status="403" title="暂无抽签排序查看权限" />;
  return (
    <div className="competition-page" style={{ padding: 16 }}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
          <div>
            {showTitle && <Typography.Title level={4} style={{ margin: 0 }}><OrderedListOutlined /> 抽签排序</Typography.Title>}
            <Typography.Text type="secondary">赛段：{stageName || stageId} · 先定抽签顺序，再逐个抽出场顺序</Typography.Text>
          </div>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                void tasksQuery.refetch();
                void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-candidates', stageId] });
              }}
            >刷新</Button>
            {canCreate && <Button type="primary" onClick={openWizard}>创建抽签任务</Button>}
          </Space>
        </div>

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
            onChange: (page) => setTaskPage(page),
          }}
          locale={{ emptyText: '当前赛段还没有抽签任务' }}
          scroll={{ x: 980 }}
        />
      </Space>

      <Modal
        title={`抽签任务 · ${['创建抽签任务', '选择选手', '生成抽签顺序'][wizardStep]}`}
        open={wizardOpen}
        onCancel={closeWizard}
        width={920}
        centered
        destroyOnClose
        styles={{ body: { maxHeight: 'calc(100vh - 220px)', overflowY: 'auto' } }}
        footer={(
          <Space>
            {wizardStep < 2 && <Button onClick={closeWizard}>取消</Button>}
            {wizardStep === 0 && (
              <Button type="primary" loading={createMutation.isPending} onClick={() => createMutation.mutate()}>
                创建任务并选择选手
              </Button>
            )}
            {wizardStep === 1 && (
              <Button
                type="primary"
                loading={selectionMutation.isPending}
                disabled={!countMatches || !canCreate}
                onClick={() => selectionMutation.mutate(selectedIds)}
              >保存选手并确认</Button>
            )}
            {wizardStep === 2 && (
              <>
                <Button onClick={closeWizard}>稍后处理</Button>
                {canDraw && (
                  <Button
                    type="primary"
                    loading={orderMutation.isPending}
                    disabled={!createdTaskId}
                    onClick={() => createdTaskId && orderMutation.mutate(createdTaskId)}
                  >生成抽签顺序</Button>
                )}
              </>
            )}
          </Space>
        )}
      >
        <Steps
          current={wizardStep}
          items={[
            { title: '创建抽签任务' },
            { title: '选择选手' },
            { title: '生成抽签顺序' },
          ]}
          style={{ marginBottom: 24 }}
        />
        {wizardStep === 0 && (
          <Alert
            type="info"
            showIcon
            message={`当前赛段：${stageName || stageId}`}
            description="抽签分两次执行。第一轮为已选选手生成随机抽签顺序，决定谁先抽；第二轮按这个顺序逐个选手抽签，每次抽出一个尚未占用的出场顺序。"
          />
        )}
        {wizardStep === 1 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            {candidatesQuery.isError && (
              <Alert type="error" showIcon message={errorText(candidatesQuery.error, '当前赛段选手加载失败')} />
            )}
            <Alert
              type={countMatches ? 'success' : 'warning'}
              showIcon
              message={`当前赛段选手 ${candidates.length} 人；已选 ${selectedIds.length} 人；允许选择 1–${MAX_DRAW_PLAYER_COUNT} 人。`}
              description={countMatches
                ? '人数校验通过。确认后将进入第一轮，生成这些选手的随机抽签顺序。'
                : candidates.length === 0
                  ? '当前赛段暂无可抽签选手。'
                  : `请选择 1 至 ${MAX_DRAW_PLAYER_COUNT} 名选手。`}
            />
            <Space wrap>
              <Button
                disabled={!candidates.length || candidatesQuery.isFetching}
                onClick={() => setSelectedIds(candidates.map((item) => idValue(item.contestantProfileId)))}
              >全选当前赛段选手</Button>
              <Button disabled={!selectedIds.length} onClick={() => setSelectedIds([])}>清空选择</Button>
            </Space>
            <Table<CompetitionPlayerDrawCandidate>
              rowKey={(record) => idValue(record.contestantProfileId)}
              loading={candidatesQuery.isFetching}
              dataSource={candidates}
              columns={candidateColumns}
              rowSelection={{
                selectedRowKeys: selectedIds,
                preserveSelectedRowKeys: true,
                onChange: (keys) => setSelectedIds(keys.map(String)),
              }}
              pagination={{ pageSize: 8, showSizeChanger: true, pageSizeOptions: [8, 20, 50] }}
              scroll={{ x: 900, y: 360 }}
              locale={{ emptyText: '当前赛段暂无可抽签选手' }}
            />
          </Space>
        )}
        {wizardStep === 2 && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type="success"
              showIcon
              message="选手已保存，等待第一轮抽签"
              description={`任务 ${createdTaskId || ''} 已选择 ${savedPlayerCount} 名选手。确认后只生成抽签顺序，不会同时分配出场顺序。`}
            />
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="所属赛段">{stageName || stageId}</Descriptions.Item>
              <Descriptions.Item label="选手人数">{savedPlayerCount} 人</Descriptions.Item>
              <Descriptions.Item label="第一轮" span={2}>随机打乱选手，得到抽签顺序</Descriptions.Item>
              <Descriptions.Item label="第二轮" span={2}>按抽签顺序逐个抽取尚未占用的出场顺序</Descriptions.Item>
            </Descriptions>
            {selectedCandidates.length > 0 ? (
              <>
                <Typography.Text strong>已选选手</Typography.Text>
                <Table<CompetitionPlayerDrawCandidate>
                  rowKey={(record) => idValue(record.contestantProfileId)}
                  size="small"
                  dataSource={selectedCandidates}
                  columns={candidateColumns}
                  pagination={{ pageSize: 8, showSizeChanger: true, pageSizeOptions: [8, 20, 50] }}
                  scroll={{ x: 900, y: 300 }}
                />
              </>
            ) : (
              <Typography.Text type="secondary">
                选手名单已保存在任务中（{savedPlayerCount} 人），生成抽签顺序时将再次校验赛段成员。
              </Typography.Text>
            )}
          </Space>
        )}
      </Modal>

      <Modal
        title={detail?.status === 'drawn'
          ? `${detail.stageName || stageName || '抽签'} · 出场名单`
          : `${detail?.stageName || stageName || '抽签'} · 逐个抽签`}
        open={deskOpen}
        onCancel={() => setDeskOpen(false)}
        width={980}
        centered
        styles={{ body: { maxHeight: 'calc(100vh - 220px)', overflowY: 'auto' } }}
        footer={(
          <Space>
            <Button onClick={() => setDeskOpen(false)}>关闭</Button>
            {detail?.status === 'drawn' && activeTaskId && canDelete && (
              <Button
                danger
                icon={<DeleteOutlined />}
                loading={deleteMutation.isPending}
                onClick={() => confirmDelete(activeTaskId, 'drawn')}
              >删除</Button>
            )}
            {detail?.status === 'drawn' && activeTaskId && canExport && (
              <Button
                type="primary"
                icon={<DownloadOutlined />}
                loading={downloadMutation.isPending}
                onClick={() => downloadMutation.mutate(activeTaskId)}
              >导出 Excel</Button>
            )}
            {detail && detail.status !== 'drawn' && nextSlot && canDraw && (
              <Button
                type="primary"
                loading={drawMutation.isPending}
                onClick={() => activeTaskId && drawMutation.mutate(activeTaskId)}
              >为{nextSlot.name || '当前选手'}抽取出场顺序</Button>
            )}
          </Space>
        )}
      >
        {taskDetailQuery.isLoading ? <Spin /> : taskDetailQuery.isError
          ? <Result status="error" title="抽签结果加载失败" extra={<Button onClick={() => taskDetailQuery.refetch()}>重新加载</Button>} />
          : (
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Steps
                size="small"
                current={detail?.status === 'drawn' ? 2 : 1}
                items={[
                  { title: '抽签顺序已生成' },
                  { title: '逐个抽出场顺序' },
                  { title: '完成' },
                ]}
              />
              {lastDrawn && (
                <Alert
                  type="success"
                  showIcon
                  message={`${lastDrawn.name || '选手'} 抽到出场顺序第 ${lastDrawn.appearanceOrder} 位`}
                  description={`该选手的抽签顺序是第 ${lastDrawn.drawOrder} 位。`}
                />
              )}
              {detail && (
                <Alert
                  type={detail.status === 'drawn' ? 'success' : 'info'}
                  showIcon
                  message={detail.status === 'drawn'
                    ? `全部 ${detail.playerCount} 名选手已完成抽签。名单按出场顺序排列。`
                    : `已抽 ${detail.drawnCount || 0}/${detail.playerCount} 人。下一位抽签：第 ${nextSlot?.drawOrder || '-'} 位 ${nextSlot?.name || ''}`}
                />
              )}
              <Table<CompetitionPlayerDrawSlot>
                rowKey={slotKey}
                dataSource={detailSlots}
                columns={slotColumns}
                pagination={{ pageSize: 8, showSizeChanger: true }}
                scroll={{ x: 900 }}
                rowClassName={(record) => (
                  nextSlot && slotKey(nextSlot) === slotKey(record) ? 'ant-table-row-selected' : ''
                )}
              />
            </Space>
          )}
      </Modal>
    </div>
  );
}

export function CompetitionPlayerDrawManagementPage() {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-draw:view');
  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'player-draw'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];
  const [selectedEventId, setSelectedEventId] = useState<string>();
  const [selectedWorkTypeId, setSelectedWorkTypeId] = useState<string>();
  const [selectedLevelCode, setSelectedLevelCode] = useState<string>();
  const [selectedStageId, setSelectedStageId] = useState<string>();

  useEffect(() => {
    if (!events.length) {
      setSelectedEventId(undefined);
      return;
    }
    if (!selectedEventId || !events.some((event) => idValue(event.id) === selectedEventId)) {
      setSelectedEventId(idValue(events[0].id));
    }
  }, [events, selectedEventId]);

  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', selectedEventId, 'player-draw'],
    queryFn: () => listCompetitionEventWorkTypes(selectedEventId!),
    enabled: canView && Boolean(selectedEventId),
  });
  const workTypeOptions = useMemo(() => (
    (workTypesQuery.data || [])
      .filter((workType) => workType.status === 'active')
      .map((workType) => ({
        value: idValue(workType.id),
        label: workType.displayName || workType.workTypeName || idValue(workType.workTypeId),
      }))
  ), [workTypesQuery.data]);

  useEffect(() => {
    if (!workTypesQuery.data) return;
    if (!workTypeOptions.length) {
      setSelectedWorkTypeId(undefined);
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
      return;
    }
    if (!selectedWorkTypeId || !workTypeOptions.some((item) => item.value === selectedWorkTypeId)) {
      setSelectedWorkTypeId(workTypeOptions[0].value);
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
    }
  }, [selectedWorkTypeId, workTypeOptions, workTypesQuery.data]);

  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', selectedEventId, selectedWorkTypeId, 'player-draw'],
    queryFn: () => listCompetitionEventWorkTypeLevels(selectedEventId!, selectedWorkTypeId!),
    enabled: canView && Boolean(selectedEventId && selectedWorkTypeId),
  });
  const levelOptions = useMemo(() => (
    (levelsQuery.data || []).map((level) => ({
      value: level.levelCode,
      label: level.levelName ? `${level.levelName}（${level.levelCode}）` : level.levelCode,
    }))
  ), [levelsQuery.data]);

  useEffect(() => {
    if (!levelsQuery.data || !selectedWorkTypeId) return;
    if (!levelOptions.length) {
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
      return;
    }
    if (!selectedLevelCode || !levelOptions.some((item) => item.value === selectedLevelCode)) {
      setSelectedLevelCode(levelOptions[0].value);
      setSelectedStageId(undefined);
    }
  }, [levelOptions, levelsQuery.data, selectedLevelCode, selectedWorkTypeId]);

  const stagesQuery = useQuery({
    queryKey: ['competition-stages', 'player-draw', selectedEventId, selectedWorkTypeId, selectedLevelCode],
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
    const stageIds = stages.map((stage) => idValue(stage.id));
    if (!selectedStageId || !stageIds.includes(selectedStageId)) {
      setSelectedStageId(stageIds[0]);
    }
  }, [selectedStageId, stages, stagesQuery.data]);

  const selectedStage = stages.find((stage) => idValue(stage.id) === selectedStageId);

  if (!canView) return <Result status="403" title="暂无抽签排序查看权限" />;
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventsQuery.isError) {
    return <Result status="error" title="赛事任务加载失败" extra={<Button onClick={() => eventsQuery.refetch()}>重新加载</Button>} />;
  }

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}><OrderedListOutlined /> 抽签排序</Typography.Title>
          <Typography.Paragraph type="secondary">
            按赛事工种、技能等级和赛段选择选手。第一轮生成随机抽签顺序，第二轮按该顺序逐个抽出场顺序。
            {selectedStage ? ` 当前赛段：${selectedStage.name}` : ''}
          </Typography.Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            void eventsQuery.refetch();
            if (selectedEventId) void workTypesQuery.refetch();
            if (selectedEventId && selectedWorkTypeId) void levelsQuery.refetch();
            if (selectedEventId && selectedWorkTypeId && selectedLevelCode) {
              void stagesQuery.refetch();
            }
          }}
        >刷新</Button>
      </section>

      <Card bordered={false} className="competition-filter-card competition-filter-card--compact">
        <Space align="center" size={12} wrap style={{ width: '100%' }}>
          <Typography.Text type="secondary">赛事任务</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedEventId}
            loading={eventsQuery.isFetching}
            disabled={!events.length}
            options={events.map((event: CompetitionEvent) => ({
              value: idValue(event.id),
              label: `${event.code} · ${event.name}`,
            }))}
            onChange={(value) => {
              setSelectedEventId(value);
              setSelectedWorkTypeId(undefined);
              setSelectedLevelCode(undefined);
              setSelectedStageId(undefined);
            }}
            placeholder="选择赛事任务"
            style={{ width: 280, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">工种</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedWorkTypeId}
            loading={workTypesQuery.isFetching}
            disabled={!selectedEventId || (!workTypesQuery.isFetching && workTypeOptions.length === 0)}
            options={workTypeOptions}
            onChange={(value) => {
              setSelectedWorkTypeId(value);
              setSelectedLevelCode(undefined);
              setSelectedStageId(undefined);
            }}
            placeholder="选择工种"
            style={{ width: 220, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">等级</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedLevelCode}
            loading={levelsQuery.isFetching}
            disabled={!selectedWorkTypeId || levelOptions.length === 0}
            options={levelOptions}
            onChange={(value) => {
              setSelectedLevelCode(value);
              setSelectedStageId(undefined);
            }}
            placeholder="选择等级"
            style={{ width: 180, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">赛段</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedStageId}
            loading={stagesQuery.isFetching}
            disabled={!selectedLevelCode || stages.length === 0}
            options={stages.map((stage: CompetitionStage) => ({
              value: idValue(stage.id),
              label: `${stage.sortOrder}. ${stage.name}`,
            }))}
            onChange={setSelectedStageId}
            placeholder="选择赛段"
            style={{ width: 220, maxWidth: '100%' }}
          />
        </Space>
      </Card>

      {workTypesQuery.isError && <Alert type="error" showIcon message={errorText(workTypesQuery.error, '赛事工种加载失败')} />}
      {levelsQuery.isError && <Alert type="error" showIcon message={errorText(levelsQuery.error, '工种等级加载失败')} />}
      {stagesQuery.isError && <Alert type="error" showIcon message={errorText(stagesQuery.error, '赛段加载失败')} />}
      {!events.length && <Alert type="info" showIcon message="当前没有可用赛事任务" />}
      {!workTypesQuery.isFetching && selectedEventId && workTypeOptions.length === 0 && (
        <Alert type="info" showIcon message="当前赛事暂无可用工种" />
      )}
      {!levelsQuery.isFetching && selectedWorkTypeId && levelOptions.length === 0 && (
        <Alert type="info" showIcon message="当前工种暂无已配置等级" />
      )}
      {!stagesQuery.isFetching && selectedLevelCode && stagesQuery.isSuccess && stages.length === 0 && (
        <Alert type="info" showIcon message="当前工种和等级下暂无赛段，请先完成赛段编排" />
      )}
      {selectedStage && (
        <CompetitionPlayerDrawPage
          key={idValue(selectedStage.id)}
          stageId={idValue(selectedStage.id)}
          stageName={selectedStage.name}
          showTitle={false}
        />
      )}
    </div>
  );
}

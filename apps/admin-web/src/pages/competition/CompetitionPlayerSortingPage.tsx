import {
  DeleteOutlined,
  DownloadOutlined,
  ReloadOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Card,
  Alert,
  Button,
  Descriptions,
  Form,
  InputNumber,
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
import { useEffect, useMemo, useState } from 'react';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
  pageCompetitionStages,
} from '../../api/competition';
import {
  createCompetitionPlayerSortingTask,
  deleteCompetitionPlayerSortingTask,
  downloadCompetitionPlayerSortingRoster,
  getCompetitionPlayerSortingTask,
  listCompetitionPlayerSortingCandidates,
  pageCompetitionPlayerSortingTasks,
  randomizeCompetitionPlayerSortingTask,
  saveCompetitionPlayerSortingTaskSelection,
} from '../../api/competitionPlayerSorting';
import type { CompetitionEvent, CompetitionId, CompetitionStage } from '../../types/competition';
import type {
  CompetitionPlayerSortingAssignment,
  CompetitionPlayerSortingCandidate,
  CompetitionPlayerSortingTask,
} from '../../types/competitionPlayerSorting';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';

interface SortingFormValues {
  groupCount?: number;
  workstationCount?: number;
  playersPerGroup?: number;
}

interface SortingConfiguration {
  groupCount: number;
  workstationCount: number;
  playersPerGroup: number;
}

interface CompetitionPlayerSortingPageProps {
  stageId: string;
  stageName?: string;
  showTitle?: boolean;
}

const MAX_SORTING_PLAYER_COUNT = 5000;

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
  return status === 'sorted'
    ? <Tag color="success">已随机排序</Tag>
    : <Tag color="processing">待随机排序</Tag>;
}

function groupSizePlan(playerCount: number, groupCount: number) {
  if (playerCount < 1 || groupCount < 1) return [];
  const baseSize = Math.floor(playerCount / groupCount);
  const largerGroupCount = playerCount % groupCount;
  return Array.from(
    { length: groupCount },
    (_, index) => baseSize + (index < largerGroupCount ? 1 : 0),
  );
}

function maximumSortingPlayerCount(groupCount: number, playersPerGroup: number) {
  return Math.min(groupCount * playersPerGroup, MAX_SORTING_PLAYER_COUNT);
}

export function CompetitionPlayerSortingPage({
  stageId,
  stageName,
  showTitle = true,
}: CompetitionPlayerSortingPageProps) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-sorting:view');
  const canCreate = permissions.includes('competition:player-sorting:create');
  const canRandomize = permissions.includes('competition:player-sorting:randomize');
  const canExport = permissions.includes('competition:player-sorting:export');
  const canDelete = permissions.includes('competition:player-sorting:delete');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [taskPage, setTaskPage] = useState(1);
  const [wizardOpen, setWizardOpen] = useState(false);
  const [wizardStep, setWizardStep] = useState(0);
  const [configuration, setConfiguration] = useState<SortingConfiguration>();
  const [createdTaskId, setCreatedTaskId] = useState<string>();
  const [savedPlayerCount, setSavedPlayerCount] = useState(0);
  const [activeTaskId, setActiveTaskId] = useState<string>();
  const [resultOpen, setResultOpen] = useState(false);
  const [form] = Form.useForm<SortingFormValues>();
  const queryClient = useQueryClient();

  const candidatesQuery = useQuery({
    queryKey: ['competition-player-sorting-candidates', stageId],
    queryFn: () => listCompetitionPlayerSortingCandidates(stageId!),
    enabled: canView && Boolean(stageId) && wizardOpen,
  });
  const candidates = candidatesQuery.data || [];
  const tasksQuery = useQuery({
    queryKey: ['competition-player-sorting-tasks', stageId, taskPage],
    queryFn: () => pageCompetitionPlayerSortingTasks(stageId!, { page: taskPage, pageSize: 10 }),
    enabled: canView && Boolean(stageId),
  });
  const taskDetailQuery = useQuery({
    queryKey: ['competition-player-sorting-task', activeTaskId],
    queryFn: () => getCompetitionPlayerSortingTask(activeTaskId!),
    enabled: Boolean(activeTaskId && resultOpen),
  });

  const groupCount = Form.useWatch('groupCount', form);
  const workstationCount = Form.useWatch('workstationCount', form);
  const playersPerGroup = Form.useWatch('playersPerGroup', form);
  const effectiveGroupCount = configuration?.groupCount ?? Number(groupCount || 0);
  const effectiveWorkstationCount = configuration?.workstationCount ?? Number(workstationCount || 0);
  const effectivePlayersPerGroup = configuration?.playersPerGroup ?? Number(playersPerGroup || 0);
  const minimumPlayerCount = effectiveGroupCount;
  const maximumPlayerCount = maximumSortingPlayerCount(effectiveGroupCount, effectivePlayersPerGroup);
  const countMatches = minimumPlayerCount > 0 && effectivePlayersPerGroup > 0
    && selectedIds.length >= minimumPlayerCount
    && selectedIds.length <= maximumPlayerCount;
  const candidateCountInsufficient = candidatesQuery.isSuccess
    && candidates.length < minimumPlayerCount;
  const enoughWorkstations = effectiveGroupCount > 0
    && effectiveWorkstationCount >= effectiveGroupCount;

  const createMutation = useMutation({
    mutationFn: (taskConfiguration: SortingConfiguration) => {
      if (!stageId) throw new Error('请先在选手管理中选择赛事工种、等级和赛段');
      return createCompetitionPlayerSortingTask(stageId, {
        ...taskConfiguration,
        contestantProfileIds: [],
      });
    },
    onSuccess: (task) => {
      const taskId = idValue(task.id);
      setCreatedTaskId(taskId);
      setSavedPlayerCount(0);
      setWizardStep(1);
      message.success('排序任务已创建，请选择参加排序的选手');
      void queryClient.invalidateQueries({ queryKey: ['competition-player-sorting-tasks', stageId] });
    },
    onError: (error) => message.error(errorText(error, '创建排序任务失败')),
  });

  const selectionMutation = useMutation({
    mutationFn: (contestantProfileIds: string[]) => {
      if (!createdTaskId) throw new Error('请先创建排序任务');
      return saveCompetitionPlayerSortingTaskSelection(createdTaskId, { contestantProfileIds });
    },
    onSuccess: (task) => {
      setSavedPlayerCount(task.playerCount);
      setWizardStep(2);
      message.success('选手名单已保存，请确认后执行随机分组');
      void queryClient.invalidateQueries({ queryKey: ['competition-player-sorting-tasks', stageId] });
    },
    onError: (error) => message.error(errorText(error, '保存选手名单失败')),
  });

  const randomizeMutation = useMutation({
    mutationFn: randomizeCompetitionPlayerSortingTask,
    onSuccess: (task) => {
      const taskId = idValue(task.id);
      message.success('随机分组排序已完成');
      queryClient.setQueryData(['competition-player-sorting-task', taskId], task);
      void queryClient.invalidateQueries({ queryKey: ['competition-player-sorting-tasks', stageId] });
      setActiveTaskId(taskId);
      setWizardOpen(false);
      setResultOpen(true);
    },
    onError: (error) => message.error(errorText(error, '随机排序失败')),
  });

  const downloadMutation = useMutation({
    mutationFn: downloadCompetitionPlayerSortingRoster,
    onSuccess: (blob) => {
      downloadBlob(blob, `${stageName || '赛段'}-实操出场名单.xlsx`);
      message.success('实操出场名单已下载');
    },
    onError: (error) => message.error(errorText(error, '下载名单失败')),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCompetitionPlayerSortingTask,
    onSuccess: () => {
      message.success('排序任务已删除');
      void queryClient.invalidateQueries({ queryKey: ['competition-player-sorting-tasks', stageId] });
    },
    onError: (error) => message.error(errorText(error, '删除排序任务失败')),
  });

  const candidateColumns = useMemo(() => [
    { title: '准考证号', dataIndex: 'contestantNo', width: 170, render: (value: string) => value || '-' },
    { title: '选手姓名', dataIndex: 'name', width: 140, render: (value: string) => value || '-' },
    { title: '所属企业', dataIndex: 'orgName', width: 220, render: (value: string) => value || '-' },
    { title: '赛事工种', dataIndex: 'workTypeName', width: 180, render: (value: string) => value || '-' },
    {
      title: '技能等级',
      dataIndex: 'levelName',
      width: 150,
      render: (value: string, record: CompetitionPlayerSortingCandidate) => value || record.levelCode || record.rosterLevel || '-',
    },
  ], []);

  const assignmentColumns = useMemo(() => [
    { title: '抽签组别', dataIndex: 'groupNo', width: 130, render: (value: number) => `第${value}组` },
    { title: '组内顺序', dataIndex: 'groupOrder', width: 110 },
    { title: '对应工位', dataIndex: 'workstation', width: 240 },
    { title: '准考证号', dataIndex: 'contestantNo', width: 180, render: (value: string) => value || '-' },
    { title: '考生姓名', dataIndex: 'name', width: 140, render: (value: string) => value || '-' },
  ], []);

  const taskColumns = [
    { title: '组数', dataIndex: 'groupCount', width: 70 },
    { title: '工位数', dataIndex: 'workstationCount', width: 80 },
    { title: '每组人数上限', dataIndex: 'playersPerGroup', width: 105 },
    { title: '选手数', dataIndex: 'playerCount', width: 80 },
    {
      title: '任务状态',
      dataIndex: 'status',
      width: 120,
      render: (value: string, record: CompetitionPlayerSortingTask) => taskStatus(value, record.playerCount),
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 180, render: (value: string) => value ? value.replace('T', ' ').slice(0, 16) : '-' },
    {
      title: '操作',
      key: 'actions',
      width: 300,
      render: (_: unknown, record: CompetitionPlayerSortingTask) => (
        <Space>
          {record.status === 'created' && record.playerCount === 0 && canCreate && (
            <Button
              type="link"
              onClick={() => resumeTask(record)}
            >选择选手</Button>
          )}
          {record.status === 'created' && record.playerCount > 0 && canRandomize && (
            <Button type="link" onClick={() => resumeTask(record)}>确认执行</Button>
          )}
          {record.status === 'sorted' && canView && (
            <Button type="link" onClick={() => { setActiveTaskId(idValue(record.id)); setResultOpen(true); }}>
              查看名单
            </Button>
          )}
          {record.status === 'sorted' && canExport && (
            <Button
              type="link"
              icon={<DownloadOutlined />}
              loading={downloadMutation.isPending && downloadMutation.variables === record.id}
              onClick={() => downloadMutation.mutate(record.id)}
            >导出 Excel</Button>
          )}
          {record.status === 'created' && canDelete && (
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              loading={deleteMutation.isPending && deleteMutation.variables === idValue(record.id)}
              onClick={() => Modal.confirm({
                title: '删除排序任务',
                content: '删除后，该未执行的排序任务将从任务列表中移除。确认删除吗？',
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

  const selectedCandidates = useMemo(() => {
    const candidateById = new Map(
      candidates.map((candidate) => [idValue(candidate.contestantProfileId), candidate]),
    );
    return selectedIds
      .map((id) => candidateById.get(id))
      .filter((candidate): candidate is CompetitionPlayerSortingCandidate => Boolean(candidate));
  }, [candidates, selectedIds]);

  const openWizard = () => {
    form.resetFields();
    setConfiguration(undefined);
    setCreatedTaskId(undefined);
    setSavedPlayerCount(0);
    setSelectedIds([]);
    setWizardStep(0);
    setWizardOpen(true);
  };

  const resumeTask = (task: CompetitionPlayerSortingTask) => {
    setConfiguration({
      groupCount: task.groupCount,
      workstationCount: task.workstationCount,
      playersPerGroup: task.playersPerGroup,
    });
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

  const createTaskAndOpenSelection = async () => {
    try {
      const values = await form.validateFields();
      const nextConfiguration = {
        groupCount: Number(values.groupCount),
        workstationCount: Number(values.workstationCount),
        playersPerGroup: Number(values.playersPerGroup),
      };
      setConfiguration(nextConfiguration);
      createMutation.mutate(nextConfiguration);
    } catch {
      // Ant Design displays the field validation messages in the form.
    }
  };

  const saveSelectionAndReview = () => {
    if (!countMatches || !enoughWorkstations) return;
    selectionMutation.mutate(selectedIds);
  };

  if (!canView) return <Result status="403" title="暂无排序分组查看权限" />;
  return (
    <div className="competition-page" style={{ padding: 16 }}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
          <div>
            {showTitle && <Typography.Title level={4} style={{ margin: 0 }}><TeamOutlined /> 排序分组</Typography.Title>}
            <Typography.Text type="secondary">赛段：{stageName || stageId} · 历史任务与名单</Typography.Text>
          </div>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                void tasksQuery.refetch();
                void queryClient.invalidateQueries({ queryKey: ['competition-player-sorting-candidates', stageId] });
              }}
            >刷新</Button>
            {canCreate && <Button type="primary" onClick={openWizard}>创建排序任务</Button>}
          </Space>
        </div>

        {tasksQuery.isError && (
          <Alert type="error" showIcon message={errorText(tasksQuery.error, '排序任务加载失败')} />
        )}
        <Table<CompetitionPlayerSortingTask>
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
          locale={{ emptyText: '当前赛段还没有排序任务' }}
          scroll={{ x: 950 }}
        />
      </Space>

      <Modal
        title={`排序任务 · ${['创建排序任务', '选择选手', '确认并执行'][wizardStep]}`}
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
              <Button
                type="primary"
                loading={createMutation.isPending}
                disabled={!enoughWorkstations}
                onClick={() => void createTaskAndOpenSelection()}
              >
                创建任务并选择选手
              </Button>
            )}
            {wizardStep === 1 && (
              <Button
                type="primary"
                loading={selectionMutation.isPending}
                disabled={!countMatches || !enoughWorkstations || !canCreate}
                onClick={saveSelectionAndReview}
              >保存选手并确认</Button>
            )}
            {wizardStep === 2 && (
              <>
                <Button onClick={closeWizard}>稍后处理</Button>
                {canRandomize && (
                  <Button
                    type="primary"
                    loading={randomizeMutation.isPending}
                    disabled={!createdTaskId}
                    onClick={() => createdTaskId && randomizeMutation.mutate(createdTaskId)}
                  >确认并执行随机分组</Button>
                )}
              </>
            )}
          </Space>
        )}
      >
        <Steps
          current={wizardStep}
          items={[
            { title: '创建排序任务' },
            { title: '选择选手' },
            { title: '确认并执行' },
          ]}
          style={{ marginBottom: 24 }}
        />

        {wizardStep === 0 && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              message={`当前赛段：${stageName || stageId}`}
              description="设置组数、工位数和每组人数上限。选手至少与组数相同以保证每组有人；人数不整除时，各组人数尽量均衡，差不超过 1 人。"
            />
            <Form<SortingFormValues> form={form} layout="vertical" requiredMark>
              <Space size={20} wrap align="start">
                <Form.Item name="groupCount" label="组数量" rules={[{ required: true, message: '请输入组数量' }]}>
                  <InputNumber min={1} max={200} precision={0} placeholder="例如 8" style={{ width: 180 }} />
                </Form.Item>
                <Form.Item name="workstationCount" label="工位数量" rules={[{ required: true, message: '请输入工位数量' }]}>
                  <InputNumber min={1} max={5000} precision={0} placeholder="例如 16" style={{ width: 180 }} />
                </Form.Item>
                <Form.Item name="playersPerGroup" label="每组人数上限" rules={[{ required: true, message: '请输入每组人数上限' }]}>
                  <InputNumber min={1} max={5000} precision={0} placeholder="例如 5" style={{ width: 180 }} />
                </Form.Item>
              </Space>
            </Form>
            {groupCount && workstationCount && workstationCount < groupCount && (
              <Alert type="warning" showIcon message="工位数量不能少于组数，请保证每组至少有一个工位。" />
            )}
            <Typography.Text type="secondary">
              工位会按组顺序尽量均匀分配；每个赛段分别创建和保存排序任务，单次最多选择 5000 名选手。
            </Typography.Text>
          </Space>
        )}

        {wizardStep === 1 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            {candidatesQuery.isError && (
              <Alert type="error" showIcon message={errorText(candidatesQuery.error, '当前赛段选手加载失败')} />
            )}
            <Alert
              type={countMatches ? 'success' : 'warning'}
              showIcon
              message={`当前赛段选手 ${candidates.length} 人；已选 ${selectedIds.length} 人；允许选择 ${minimumPlayerCount}–${maximumPlayerCount} 人。`}
              description={countMatches
                ? '人数校验通过。系统会随机打乱选手，再均衡分组，各组人数相差不超过 1 人。'
                : candidateCountInsufficient
                  ? `当前赛段只有 ${candidates.length} 名候选，少于组数 ${minimumPlayerCount}。请降低组数后重新创建任务，或先补充赛段选手。`
                  : selectedIds.length < minimumPlayerCount
                    ? `至少选择 ${minimumPlayerCount} 人，保证每组至少 1 人。`
                    : `最多选择 ${maximumPlayerCount} 人（每组不超过 ${effectivePlayersPerGroup} 人，单次上限 5000 人）。`}
            />
            {groupCount && workstationCount && workstationCount < groupCount && (
              <Alert type="warning" showIcon message="工位数量不能少于组数。请返回上一步调整任务配置。" />
            )}
            <Space wrap>
              <Button
                disabled={!candidates.length || candidatesQuery.isFetching}
                onClick={() => setSelectedIds(candidates.map((item) => idValue(item.contestantProfileId)))}
              >全选当前赛段选手</Button>
              <Button disabled={!selectedIds.length} onClick={() => setSelectedIds([])}>清空选择</Button>
            </Space>
            <Table<CompetitionPlayerSortingCandidate>
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

        {wizardStep === 2 && configuration && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type="success"
              showIcon
              message="任务配置与选手人数已核对"
              description={`已创建并保存任务 ${createdTaskId || ''}。确认后将对选手进行随机排序，并按组分配工位。`}
            />
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="所属赛段">{stageName || stageId}</Descriptions.Item>
              <Descriptions.Item label="选手人数">{savedPlayerCount} 人</Descriptions.Item>
              <Descriptions.Item label="组数量">{configuration.groupCount} 组</Descriptions.Item>
              <Descriptions.Item label="工位数量">{configuration.workstationCount} 个</Descriptions.Item>
              <Descriptions.Item label="每组人数上限">{configuration.playersPerGroup} 人</Descriptions.Item>
              <Descriptions.Item label="分组人数校验">
                已选 {savedPlayerCount} 人；允许 {configuration.groupCount}–{maximumSortingPlayerCount(configuration.groupCount, configuration.playersPerGroup)} 人，通过
              </Descriptions.Item>
              <Descriptions.Item label="预计各组人数" span={2}>
                {groupSizePlan(savedPlayerCount, configuration.groupCount)
                  .map((size, index) => `第${index + 1}组 ${size}人`).join('；')}
              </Descriptions.Item>
            </Descriptions>
            {selectedCandidates.length > 0 ? (
              <>
                <Typography.Text strong>已选选手</Typography.Text>
                <Table<CompetitionPlayerSortingCandidate>
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
                选手名单已保存在任务中（{savedPlayerCount} 人），执行时将再次校验赛段成员。
              </Typography.Text>
            )}
          </Space>
        )}
      </Modal>

      <Modal
        title={taskDetailQuery.data?.stageName ? `${taskDetailQuery.data.stageName} · 实操出场名单` : '实操出场名单'}
        open={resultOpen}
        onCancel={() => setResultOpen(false)}
        width={920}
        footer={(
          <Space>
            <Button onClick={() => setResultOpen(false)}>关闭</Button>
            {activeTaskId && canExport && (
              <Button
                type="primary"
                icon={<DownloadOutlined />}
                loading={downloadMutation.isPending}
                onClick={() => downloadMutation.mutate(activeTaskId)}
              >导出 Excel</Button>
            )}
          </Space>
        )}
      >
        {taskDetailQuery.isLoading ? <Spin /> : taskDetailQuery.isError
          ? <Result status="error" title="排序结果加载失败" extra={<Button onClick={() => taskDetailQuery.refetch()}>重新加载</Button>} />
          : (
            <>
              {taskDetailQuery.data && (
                <Alert style={{ marginBottom: 12 }} type="success" showIcon
                  message={`共 ${taskDetailQuery.data.playerCount} 人，${taskDetailQuery.data.groupCount} 组（${groupSizePlan(taskDetailQuery.data.playerCount, taskDetailQuery.data.groupCount).map((size, index) => `第${index + 1}组${size}人`).join('、')}）；每组人数上限 ${taskDetailQuery.data.playersPerGroup} 人；工位总数 ${taskDetailQuery.data.workstationCount}。`} />
              )}
              <Table<CompetitionPlayerSortingAssignment>
                rowKey={(record) => `${record.groupNo}-${record.groupOrder}-${record.contestantProfileId}`}
                dataSource={taskDetailQuery.data?.assignments || []}
                columns={assignmentColumns}
                pagination={{ pageSize: 10, showSizeChanger: true }}
                scroll={{ x: 800 }}
              />
            </>
          )}
      </Modal>
    </div>
  );
}

export function CompetitionPlayerSortingManagementPage() {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-sorting:view');
  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'player-sorting'],
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
    queryKey: ['competition-event-work-types', selectedEventId, 'player-sorting'],
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
    queryKey: ['competition-event-work-type-levels', selectedEventId, selectedWorkTypeId, 'player-sorting'],
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
    queryKey: ['competition-stages', 'player-sorting', selectedEventId, selectedWorkTypeId, selectedLevelCode],
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

  if (!canView) return <Result status="403" title="暂无排序分组查看权限" />;
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventsQuery.isError) {
    return <Result status="error" title="赛事任务加载失败" extra={<Button onClick={() => eventsQuery.refetch()}>重新加载</Button>} />;
  }

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}><TeamOutlined /> 排序分组</Typography.Title>
          <Typography.Paragraph type="secondary">
            按赛事工种、技能等级和赛段查看并维护实操出场排序任务。
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
        <CompetitionPlayerSortingPage
          key={idValue(selectedStage.id)}
          stageId={idValue(selectedStage.id)}
          stageName={selectedStage.name}
          showTitle={false}
        />
      )}
    </div>
  );
}

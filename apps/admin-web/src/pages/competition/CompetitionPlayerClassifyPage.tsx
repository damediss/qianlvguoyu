import {
  AppstoreOutlined,
  DeleteOutlined,
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Input,
  Modal,
  Pagination,
  Popconfirm,
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
import dayjs from 'dayjs';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
  pageCompetitionStages,
} from '../../api/competition';
import {
  createCompetitionPlayerClassifyTask,
  deleteCompetitionPlayerClassifyTask,
  downloadCompetitionPlayerClassifyResult,
  getCompetitionPlayerClassifyTask,
  listCompetitionPlayerClassifyCandidates,
  pageCompetitionPlayerClassifyTasks,
} from '../../api/competitionPlayerClassify';
import type { CompetitionEvent, CompetitionId, CompetitionStage } from '../../types/competition';
import type {
  CompetitionPlayerClassifyAssignment,
  CompetitionPlayerClassifyCandidate,
  CompetitionPlayerClassifyTask,
} from '../../types/competitionPlayerClassify';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
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

function errorText(error: unknown, fallback: string) {
  const responseMessage = (error as {
    response?: { data?: { message?: unknown } };
  })?.response?.data?.message;
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage;
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

/** 与后端 CompetitionPlayerClassifyPlan 一致：不能整除时，靠前的类型各多 1 人。 */
function balancedSizes(playerCount: number, typeCount: number) {
  if (playerCount < 1 || typeCount < 1 || typeCount > playerCount) return [];
  const base = Math.floor(playerCount / typeCount);
  const extra = playerCount % typeCount;
  return Array.from({ length: typeCount }, (_, index) => base + (index < extra ? 1 : 0));
}

function allocationText(typeNames: string[], sizes: number[]) {
  return typeNames.map((name, index) => `${name} ${sizes[index] ?? 0} 人`).join('，');
}

interface CompetitionPlayerClassifyPageProps {
  stageId: string;
  stageName?: string;
}

function CompetitionPlayerClassifyPage({ stageId, stageName }: CompetitionPlayerClassifyPageProps) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canCreate = permissions.includes('competition:player-classify:create');
  const canDelete = permissions.includes('competition:player-classify:delete');
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [wizardOpen, setWizardOpen] = useState(false);
  const [wizardStep, setWizardStep] = useState(0);
  const [typeDraft, setTypeDraft] = useState('');
  const [typeNames, setTypeNames] = useState<string[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [keyword, setKeyword] = useState('');
  const [viewingId, setViewingId] = useState<string>();
  const [typePage, setTypePage] = useState(1);

  const tasksQuery = useQuery({
    queryKey: ['competition-player-classify-tasks', stageId, page, pageSize],
    queryFn: () => pageCompetitionPlayerClassifyTasks(stageId, { page, pageSize }),
  });
  const candidatesQuery = useQuery({
    queryKey: ['competition-player-classify-candidates', stageId],
    queryFn: () => listCompetitionPlayerClassifyCandidates(stageId),
    enabled: wizardOpen,
  });
  const detailQuery = useQuery({
    queryKey: ['competition-player-classify-task', viewingId],
    queryFn: () => getCompetitionPlayerClassifyTask(viewingId!),
    enabled: Boolean(viewingId),
  });

  const candidates = candidatesQuery.data || [];
  const filteredCandidates = useMemo(() => {
    const text = keyword.trim().toLowerCase();
    if (!text) return candidates;
    return candidates.filter((item) => (
      [item.name, item.orgName, item.contestantNo, item.workTypeName, item.levelName]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(text))
    ));
  }, [candidates, keyword]);
  const sizes = balancedSizes(selectedIds.length, typeNames.length);

  const resetWizard = () => {
    setWizardStep(0);
    setTypeDraft('');
    setTypeNames([]);
    setSelectedIds([]);
    setKeyword('');
  };

  const addType = () => {
    const name = typeDraft.trim();
    if (!name) {
      message.warning('请填写分类名称');
      return;
    }
    if (name.length > 32) {
      message.warning('分类名称不能超过 32 个字');
      return;
    }
    if (typeNames.includes(name)) {
      message.warning(`分类名称不能重复：${name}`);
      return;
    }
    if (typeNames.length >= 20) {
      message.warning('分类最多 20 个');
      return;
    }
    setTypeNames((current) => [...current, name]);
    setTypeDraft('');
  };

  const classifyMutation = useMutation({
    mutationFn: () => createCompetitionPlayerClassifyTask(stageId, {
      typeNames,
      contestantProfileIds: selectedIds,
    }),
    onSuccess: (task) => {
      message.success('已按类型随机均分选手');
      queryClient.setQueryData(['competition-player-classify-task', idValue(task.id)], task);
      void queryClient.invalidateQueries({ queryKey: ['competition-player-classify-tasks', stageId] });
      setWizardOpen(false);
      resetWizard();
      setTypePage(1);
      setViewingId(idValue(task.id));
    },
    onError: (error) => message.error(errorText(error, '排序分类失败')),
  });

  const downloadMutation = useMutation({
    mutationFn: downloadCompetitionPlayerClassifyResult,
    onSuccess: (blob) => {
      downloadBlob(blob, `${stageName || '赛段'}-排序分类.xlsx`);
      message.success('分类结果已导出');
    },
    onError: (error) => message.error(errorText(error, '导出分类结果失败')),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCompetitionPlayerClassifyTask,
    onSuccess: () => {
      message.success('已删除排序分类');
      void queryClient.invalidateQueries({ queryKey: ['competition-player-classify-tasks', stageId] });
    },
    onError: (error) => message.error(errorText(error, '删除失败')),
  });

  const detail = detailQuery.data;
  const detailTypes = detail?.typeNames || [];
  const currentTypeName = detailTypes[typePage - 1];
  const currentAssignments = (detail?.assignments || [])
    .filter((item) => item.typeIndex === typePage)
    .slice()
    .sort((left, right) => left.orderInType - right.orderInType);

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Alert
        type="info"
        showIcon
        message="人数不能被类型数整除时，按类型填写顺序，靠前的类型各多 1 人。"
        description="例如 32 名选手、3 个类型，结果是 11、11、10。任意两个类型最多相差 1 人。类型数不能多于选手人数，避免出现空类型。"
      />
      <Card
        bordered={false}
        title={stageName ? `${stageName} · 排序分类` : '排序分类'}
        extra={canCreate ? (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { resetWizard(); setWizardOpen(true); }}>
            新建分类
          </Button>
        ) : null}
      >
        {tasksQuery.isError && (
          <Alert type="error" showIcon message={errorText(tasksQuery.error, '排序分类加载失败')} />
        )}
        <Table<CompetitionPlayerClassifyTask>
          rowKey={(record) => idValue(record.id)}
          loading={tasksQuery.isFetching}
          dataSource={tasksQuery.data?.items || []}
          pagination={{
            current: page,
            pageSize,
            total: tasksQuery.data?.total || 0,
            showSizeChanger: true,
            onChange: (nextPage, nextSize) => {
              setPage(nextPage);
              setPageSize(nextSize);
            },
          }}
          locale={{ emptyText: '当前赛段还没有排序分类' }}
          columns={[
            {
              title: '分类',
              key: 'types',
              render: (_, record) => (record.typeNames || []).map((name, index) => (
                <Tag key={`${name}-${index}`}>{name} {record.typeSizes?.[index] ?? '-'} 人</Tag>
              )),
            },
            { title: '选手人数', dataIndex: 'playerCount', width: 100 },
            {
              title: '创建时间',
              dataIndex: 'createdAt',
              width: 170,
              render: (value?: string | null) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'),
            },
            {
              title: '操作',
              key: 'actions',
              width: 220,
              render: (_, record) => (
                <Space>
                  <Button
                    type="link"
                    onClick={() => {
                      setTypePage(1);
                      setViewingId(idValue(record.id));
                    }}
                  >查看</Button>
                  <Button
                    type="link"
                    icon={<DownloadOutlined />}
                    loading={downloadMutation.isPending}
                    onClick={() => downloadMutation.mutate(idValue(record.id))}
                  >导出</Button>
                  {canDelete && (
                    <Popconfirm
                      title="删除这条排序分类？"
                      okText="删除"
                      cancelText="取消"
                      onConfirm={() => deleteMutation.mutate(idValue(record.id))}
                    >
                      <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
                    </Popconfirm>
                  )}
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title="新建排序分类"
        open={wizardOpen}
        width={980}
        destroyOnClose
        onCancel={() => { setWizardOpen(false); resetWizard(); }}
        footer={(
          <Space>
            <Button onClick={() => { setWizardOpen(false); resetWizard(); }}>取消</Button>
            {wizardStep > 0 && (
              <Button onClick={() => setWizardStep((current) => current - 1)}>上一步</Button>
            )}
            {wizardStep < 2 && (
              <Button
                type="primary"
                disabled={wizardStep === 0 ? typeNames.length < 1 : selectedIds.length < typeNames.length}
                onClick={() => setWizardStep((current) => current + 1)}
              >下一步</Button>
            )}
            {wizardStep === 2 && (
              <Button
                type="primary"
                loading={classifyMutation.isPending}
                disabled={sizes.length !== typeNames.length}
                onClick={() => classifyMutation.mutate()}
              >确认并随机分类</Button>
            )}
          </Space>
        )}
      >
        <Steps
          current={wizardStep}
          items={[{ title: '新增类型' }, { title: '选择选手' }, { title: '确认均分' }]}
          style={{ marginBottom: 24 }}
        />
        {wizardStep === 0 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              message="类型名称由业务人员自定义填写，例如“焦粒启槽”“突发事件异常处理”。"
            />
            <Space wrap>
              <Input
                value={typeDraft}
                maxLength={32}
                placeholder="分类名称"
                style={{ width: 280 }}
                onChange={(event) => setTypeDraft(event.target.value)}
                onPressEnter={addType}
              />
              <Button icon={<PlusOutlined />} onClick={addType}>添加类型</Button>
            </Space>
            <div>
              {typeNames.length === 0 && <Typography.Text type="secondary">还没有类型</Typography.Text>}
              {typeNames.map((name) => (
                <Tag
                  key={name}
                  closable
                  onClose={(event) => {
                    event.preventDefault();
                    setTypeNames((current) => current.filter((item) => item !== name));
                  }}
                >{name}</Tag>
              ))}
            </div>
          </Space>
        )}
        {wizardStep === 1 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            {candidatesQuery.isError && (
              <Alert type="error" showIcon message={errorText(candidatesQuery.error, '当前赛段选手加载失败')} />
            )}
            <Alert
              type={selectedIds.length >= typeNames.length ? 'success' : 'warning'}
              showIcon
              message={`已选 ${selectedIds.length} 人，当前 ${typeNames.length} 个类型。`}
              description={selectedIds.length < typeNames.length
                ? '选手人数需要不少于类型数量。'
                : `预计分配：${allocationText(typeNames, sizes)}`}
            />
            <Space wrap>
              <Input.Search
                allowClear
                placeholder="搜索姓名、单位或选手编号"
                style={{ width: 280 }}
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
              <Button
                disabled={!candidates.length}
                onClick={() => setSelectedIds(candidates.map((item) => idValue(item.contestantProfileId)))}
              >全选当前赛段选手</Button>
              <Button disabled={!selectedIds.length} onClick={() => setSelectedIds([])}>清空选择</Button>
            </Space>
            <Table<CompetitionPlayerClassifyCandidate>
              rowKey={(record) => idValue(record.contestantProfileId)}
              loading={candidatesQuery.isFetching}
              dataSource={filteredCandidates}
              pagination={{ defaultPageSize: 8, showSizeChanger: true, pageSizeOptions: [8, 20, 50] }}
              scroll={{ x: 860, y: 360 }}
              locale={{ emptyText: '当前赛段暂无可分类选手' }}
              rowSelection={{
                selectedRowKeys: selectedIds,
                preserveSelectedRowKeys: true,
                onChange: (keys) => setSelectedIds(keys.map(String)),
              }}
              columns={[
                { title: '选手编号', dataIndex: 'contestantNo', width: 120, render: (value?: string | null) => value || '-' },
                { title: '姓名', dataIndex: 'name', width: 120, render: (value?: string | null) => value || '-' },
                { title: '参赛公司', dataIndex: 'orgName', render: (value?: string | null) => value || '-' },
                { title: '工种', dataIndex: 'workTypeName', width: 160, render: (value?: string | null) => value || '-' },
                { title: '等级', dataIndex: 'levelName', width: 120, render: (value?: string | null) => value || '-' },
              ]}
            />
          </Space>
        )}
        {wizardStep === 2 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type="success"
              showIcon
              message={allocationText(typeNames, sizes)}
              description={selectedIds.length % typeNames.length === 0
                ? '人数可以被类型数整除，每个类型人数相同。确认后会先打乱选手，再按这个人数切分，类型内顺序即为随机出场顺序。'
                : `多出的 ${selectedIds.length % typeNames.length} 人按类型填写顺序分给靠前的类型，每个类型最多比其他类型多 1 人。确认后会先打乱选手，再按这个人数切分。`}
            />
            {typeNames.map((name, index) => (
              <Card key={name} size="small" title={`${name} · ${sizes[index] ?? 0} 人`} />
            ))}
          </Space>
        )}
      </Modal>

      <Modal
        title="分类结果"
        open={Boolean(viewingId)}
        width={880}
        footer={null}
        onCancel={() => setViewingId(undefined)}
      >
        {detailQuery.isError && (
          <Alert type="error" showIcon message={errorText(detailQuery.error, '分类结果加载失败')} />
        )}
        {detailQuery.isLoading && <Spin />}
        {detail && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              message={allocationText(detailTypes, detail.typeSizes || [])}
              description="每次只展示一个类型，避免不同分类的选手混在一起。类型内顺序是随机打乱后的出场顺序。"
            />
            <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {currentTypeName || '分类'} · {currentAssignments.length} 人
              </Typography.Title>
              <Space>
                <Button
                  icon={<DownloadOutlined />}
                  loading={downloadMutation.isPending}
                  onClick={() => viewingId && downloadMutation.mutate(viewingId)}
                >导出 Excel</Button>
                <Pagination
                  simple
                  current={typePage}
                  pageSize={1}
                  total={Math.max(detailTypes.length, 1)}
                  onChange={setTypePage}
                />
              </Space>
            </Space>
            <Table<CompetitionPlayerClassifyAssignment>
              rowKey={(record) => idValue(record.contestantProfileId)}
              dataSource={currentAssignments}
              pagination={false}
              locale={{ emptyText: '这个类型没有选手' }}
              columns={[
                { title: '类型内顺序', dataIndex: 'orderInType', width: 120 },
                { title: '姓名', dataIndex: 'name', width: 160, render: (value?: string | null) => value || '-' },
                { title: '参赛公司', dataIndex: 'orgName', render: (value?: string | null) => value || '-' },
              ]}
            />
          </Space>
        )}
      </Modal>
    </Space>
  );
}

export function CompetitionPlayerClassifyManagementPage() {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-classify:view');
  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'player-classify'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];
  const [selectedEventId, setSelectedEventId] = useState<string>();
  const [rawSelectedWorkTypeId, setSelectedWorkTypeId] = useState<string>();
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
    queryKey: ['competition-event-work-types', selectedEventId, 'player-classify'],
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
  const selectedWorkTypeId = rawSelectedWorkTypeId
    && workTypeOptions.some((item) => item.value === rawSelectedWorkTypeId)
    ? rawSelectedWorkTypeId
    : undefined;

  useEffect(() => {
    if (!workTypesQuery.data) return;
    if (!workTypeOptions.length) {
      setSelectedWorkTypeId(undefined);
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
      return;
    }
    if (!rawSelectedWorkTypeId || !workTypeOptions.some((item) => item.value === rawSelectedWorkTypeId)) {
      setSelectedWorkTypeId(workTypeOptions[0].value);
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
    }
  }, [rawSelectedWorkTypeId, workTypeOptions, workTypesQuery.data]);

  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', selectedEventId, selectedWorkTypeId, 'player-classify'],
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
    queryKey: ['competition-stages', 'player-classify', selectedEventId, selectedWorkTypeId, selectedLevelCode],
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

  if (!canView) return <Result status="403" title="暂无排序分类查看权限" />;
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventsQuery.isError) {
    return <Result status="error" title="赛事任务加载失败" extra={<Button onClick={() => eventsQuery.refetch()}>重新加载</Button>} />;
  }

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}><AppstoreOutlined /> 排序分类</Typography.Title>
          <Typography.Paragraph type="secondary">
            自定义分类后选择选手，随机打乱并按类型均分。
            {selectedStage ? ` 当前赛段：${selectedStage.name}` : ''}
          </Typography.Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            void eventsQuery.refetch();
            if (selectedEventId) void workTypesQuery.refetch();
            if (selectedEventId && selectedWorkTypeId) void levelsQuery.refetch();
            if (selectedEventId && selectedWorkTypeId && selectedLevelCode) void stagesQuery.refetch();
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
        <CompetitionPlayerClassifyPage
          key={idValue(selectedStage.id)}
          stageId={idValue(selectedStage.id)}
          stageName={selectedStage.name}
        />
      )}
    </div>
  );
}

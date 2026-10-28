import { ReloadOutlined, SendOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Empty,
  Pagination,
  Result,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
  Modal,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { TableRowSelection } from 'antd/es/table/interface';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  dispatchCompetitionPrePracticeTask,
  listCompetitionPrePracticePushOrganizations,
  pageCompetitionPrePracticeAvailableTasks,
  pageCompetitionPrePracticeDispatches,
  pageCompetitionPrePracticePushContestants,
} from '../../api/competitionPrePractice';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
} from '../../api/competition';
import type {
  CompetitionPrePracticeDispatch,
  CompetitionPrePracticePushContestant,
  CompetitionPrePracticeTask,
} from '../../types/competitionPrePractice';
import { useAuthStore } from '../../stores/authStore';
import { competitionEventLabel } from '../../utils/competitionEventLabel';
import './CompetitionPlayerPrePracticePushPage.css';

function dateTime(value?: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

export function CompetitionPlayerPrePracticePushPage() {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-pre-practice:view');
  const canPush = permissions.includes('competition:player-pre-practice:push');
  const queryClient = useQueryClient();
  const [eventId, setEventId] = useState<string>();
  const [eventWorkTypeId, setEventWorkTypeId] = useState<string>();
  const [levelCode, setLevelCode] = useState<string>();
  const [orgId, setOrgId] = useState<string>();
  const [taskId, setTaskId] = useState<string>();
  const [candidatePage, setCandidatePage] = useState(1);
  const [historyPage, setHistoryPage] = useState(1);
  const [selectedProfileIds, setSelectedProfileIds] = useState<string[]>([]);
  const [selectAllMatching, setSelectAllMatching] = useState(false);

  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'pre-practice-push'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];

  useEffect(() => {
    if (!eventId && events.length) setEventId(String(events[0].id));
  }, [eventId, events]);

  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId, 'pre-practice-push'],
    queryFn: () => listCompetitionEventWorkTypes(eventId!),
    enabled: canView && Boolean(eventId),
  });
  const workTypes = (workTypesQuery.data || []).filter((item) => item.status === 'active');
  const effectiveEventWorkTypeId = workTypes.some((item) => String(item.id) === eventWorkTypeId)
    ? eventWorkTypeId : undefined;

  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', eventId, effectiveEventWorkTypeId, 'pre-practice-push'],
    queryFn: () => listCompetitionEventWorkTypeLevels(eventId!, effectiveEventWorkTypeId!),
    enabled: canView && Boolean(eventId && effectiveEventWorkTypeId),
  });
  const levels = levelsQuery.data || [];
  const effectiveLevelCode = levels.some((item) => item.levelCode === levelCode) ? levelCode : undefined;
  const scopeReady = Boolean(eventId && effectiveEventWorkTypeId && effectiveLevelCode);

  useEffect(() => {
    setOrgId(undefined);
    setTaskId(undefined);
    setCandidatePage(1);
    setHistoryPage(1);
    setSelectedProfileIds([]);
    setSelectAllMatching(false);
  }, [eventId, effectiveEventWorkTypeId, effectiveLevelCode]);

  useEffect(() => {
    setCandidatePage(1);
    setSelectedProfileIds([]);
    setSelectAllMatching(false);
  }, [orgId]);

  const baseScope = useMemo(() => ({
    eventId: eventId || '',
    eventWorkTypeId: effectiveEventWorkTypeId || '',
    levelCode: effectiveLevelCode || '',
  }), [eventId, effectiveEventWorkTypeId, effectiveLevelCode]);

  const organizationsQuery = useQuery({
    queryKey: ['competition-pre-practice-push-organizations', baseScope],
    queryFn: () => listCompetitionPrePracticePushOrganizations(baseScope),
    enabled: canView && scopeReady,
  });
  const organizations = (organizationsQuery.data || []).filter((item) => item.id);

  const contestantsQuery = useQuery({
    queryKey: ['competition-pre-practice-push-contestants', baseScope, orgId, candidatePage],
    queryFn: () => pageCompetitionPrePracticePushContestants({
      ...baseScope,
      orgId,
      page: candidatePage,
      pageSize: 20,
    }),
    enabled: canView && scopeReady,
  });
  const contestants = contestantsQuery.data?.items || [];
  const candidateTotal = contestantsQuery.data?.total || 0;

  const tasksQuery = useQuery({
    queryKey: ['competition-pre-practice-available-tasks', baseScope],
    queryFn: () => pageCompetitionPrePracticeAvailableTasks({ ...baseScope, page: 1, pageSize: 500 }),
    enabled: canView && scopeReady,
  });
  const availableTasks = tasksQuery.data?.items || [];

  const historyQuery = useQuery({
    queryKey: ['competition-pre-practice-dispatches', baseScope, historyPage],
    queryFn: () => pageCompetitionPrePracticeDispatches({
      ...baseScope,
      page: historyPage,
      pageSize: 20,
    }),
    enabled: canView && scopeReady,
  });

  const dispatchMutation = useMutation({
    mutationFn: dispatchCompetitionPrePracticeTask,
    onSuccess: async (batch) => {
      message.success(`任务已推送至 ${batch.recipientCount} 名选手的赛事任务`);
      setSelectedProfileIds([]);
      setSelectAllMatching(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['competition-pre-practice-dispatches'] }),
        queryClient.invalidateQueries({ queryKey: ['competition-pre-practice-available-tasks'] }),
        queryClient.invalidateQueries({ queryKey: ['competition-pre-practice-tasks'] }),
      ]);
    },
    onError: (error) => message.error(errorMessage(error, '推送任务失败')),
  });

  const historyColumns: ColumnsType<CompetitionPrePracticeDispatch> = [
    { title: '任务名称', dataIndex: 'taskTitle', width: 220, ellipsis: true },
    {
      title: '赛事范围',
      width: 300,
      render: (_, row) => (
        <Space direction="vertical" size={1}>
          <span>{row.eventName}</span>
          <Typography.Text type="secondary">
            {row.eventWorkTypeName} · {row.eventWorkTypeLevelName} · {row.stageName}
          </Typography.Text>
        </Space>
      ),
    },
    { title: '推送人数', dataIndex: 'recipientCount', width: 100, render: (value: number) => `${value} 人` },
    { title: '推送时间', dataIndex: 'pushedAt', width: 180, render: (value: string) => dateTime(value) },
  ];

  const contestantColumns: ColumnsType<CompetitionPrePracticePushContestant> = [
    { title: '选手编号', dataIndex: 'contestantNo', width: 160, render: (value?: string) => value || '-' },
    { title: '姓名', dataIndex: 'contestantName', width: 180 },
    { title: '单位', dataIndex: 'orgName', ellipsis: true, render: (value?: string) => value || '未填写单位' },
    { title: '选手端账号', dataIndex: 'playerId', width: 130, render: () => <Tag color="green">已激活</Tag> },
  ];

  const selectedCount = selectAllMatching ? candidateTotal : selectedProfileIds.length;
  const rowSelection: TableRowSelection<CompetitionPrePracticePushContestant> = {
    preserveSelectedRowKeys: true,
    selectedRowKeys: selectAllMatching
      ? contestants.map((row) => row.profileId)
      : selectedProfileIds,
    getCheckboxProps: () => ({ disabled: selectAllMatching }),
    onChange: (keys) => {
      if (selectAllMatching) return;
      const nextKeys = keys.map(String);
      if (nextKeys.length > 1000) {
        message.warning('单次手动选择最多 1000 名选手，请使用全选筛选结果或按单位筛选。');
        setSelectedProfileIds(nextKeys.slice(0, 1000));
        return;
      }
      setSelectedProfileIds(nextKeys);
    },
  };

  const startDispatch = () => {
    if (!scopeReady || !taskId) {
      message.warning('请选择赛事、工种、等级和已生成任务');
      return;
    }
    if (!selectAllMatching && selectedProfileIds.length === 0) {
      message.warning('请至少选择一名选手');
      return;
    }
    const task = availableTasks.find((item) => item.id === taskId);
    const payload = {
      taskId,
      ...baseScope,
      orgId,
      selectAllMatching,
      contestantProfileIds: selectAllMatching ? [] : selectedProfileIds,
    };
    Modal.confirm({
      title: '确认推送赛前练习任务',
      content: `将“${task?.title || '赛前练习任务'}”推送给 ${selectedCount} 名选手。推送后任务会显示在选手 APP 的“赛事任务”中，同一任务后续仍可再次推送。`,
      okText: '确认推送',
      cancelText: '取消',
      onOk: () => dispatchMutation.mutateAsync(payload),
    });
  };

  if (!canView) return <Result status="403" title="无权访问" subTitle="当前账号没有赛前练习查看权限。" />;

  return (
    <div className="competition-pre-practice-push-page">
      <div className="competition-pre-practice-push-page__heading">
        <div>
          <Typography.Title level={3}>赛前练习推送管理</Typography.Title>
          <Typography.Paragraph type="secondary">
            按赛事范围筛选已激活选手，可按单位推送。每次推送都会生成独立批次，选手 APP 在“赛事任务”中分别接收。
          </Typography.Paragraph>
        </div>
      </div>

      <Card bordered={false} title="推送范围" className="competition-pre-practice-push-page__card">
        <div className="competition-pre-practice-push-page__filters">
          <div>
            <Typography.Text className="competition-pre-practice-push-page__label">赛事</Typography.Text>
            <Select
              showSearch
              optionFilterProp="label"
              value={eventId}
              placeholder="选择赛事"
              loading={eventsQuery.isFetching}
              options={events.map((event) => ({ value: String(event.id), label: competitionEventLabel(event) }))}
              onChange={(value) => {
                setEventId(value);
                setEventWorkTypeId(undefined);
                setLevelCode(undefined);
              }}
            />
          </div>
          <div>
            <Typography.Text className="competition-pre-practice-push-page__label">赛事工种</Typography.Text>
            <Select
              showSearch
              optionFilterProp="label"
              value={effectiveEventWorkTypeId}
              placeholder="选择赛事工种"
              loading={workTypesQuery.isFetching}
              disabled={!eventId}
              options={workTypes.map((item) => ({
                value: String(item.id),
                label: item.displayName || item.workTypeName || String(item.id),
              }))}
              onChange={(value) => {
                setEventWorkTypeId(value);
                setLevelCode(undefined);
              }}
            />
          </div>
          <div>
            <Typography.Text className="competition-pre-practice-push-page__label">参赛等级</Typography.Text>
            <Select
              value={effectiveLevelCode}
              placeholder="选择参赛等级"
              loading={levelsQuery.isFetching}
              disabled={!effectiveEventWorkTypeId}
              options={levels.map((item) => ({
                value: item.levelCode,
                label: item.levelName || item.levelCode,
              }))}
              onChange={setLevelCode}
            />
          </div>
          <div>
            <Typography.Text className="competition-pre-practice-push-page__label">单位筛选</Typography.Text>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              value={orgId}
              placeholder="全部单位"
              loading={organizationsQuery.isFetching}
              disabled={!scopeReady}
              options={organizations.map((item) => ({ value: String(item.id), label: item.name }))}
              onChange={setOrgId}
            />
          </div>
        </div>

        <div className="competition-pre-practice-push-page__task-row">
          <div>
            <Typography.Text className="competition-pre-practice-push-page__label">已生成任务</Typography.Text>
            <Select
              showSearch
              optionFilterProp="label"
              value={taskId}
              placeholder={scopeReady ? '选择当前赛事范围下已生成的任务' : '请先选择赛事、工种和等级'}
              loading={tasksQuery.isFetching}
              disabled={!scopeReady || availableTasks.length === 0}
              options={availableTasks.map((task: CompetitionPrePracticeTask) => ({
                value: task.id,
                label: `${task.title} · ${task.stageName} · ${task.questionCount} 题`,
              }))}
              onChange={setTaskId}
            />
          </div>
          {scopeReady && availableTasks.length === 0 && !tasksQuery.isFetching ? (
            <Typography.Text type="secondary">当前范围暂无已生成任务，请先完成试题配置。</Typography.Text>
          ) : null}
        </div>
        {scopeReady && availableTasks.length ? (
          <Typography.Paragraph type="secondary" className="competition-pre-practice-push-page__tip">
            任务按当前赛事、工种和等级筛选；赛段信息显示在任务名称后。推送只建立选手端任务收件记录，本阶段不开放题目和作答。
          </Typography.Paragraph>
        ) : null}
      </Card>

      <Card
        bordered={false}
        title={(
          <Space>
            <span>选手名单</span>
            <Tag>{candidateTotal} 人</Tag>
            {selectedCount ? <Tag color="blue">已选 {selectedCount} 人</Tag> : null}
          </Space>
        )}
        className="competition-pre-practice-push-page__card"
        extra={(
          <Space wrap>
            {selectAllMatching ? (
              <Button onClick={() => setSelectAllMatching(false)}>清除全选</Button>
            ) : (
              <Button
                disabled={!scopeReady || candidateTotal === 0 || candidateTotal > 5000}
                onClick={() => {
                  setSelectedProfileIds([]);
                  setSelectAllMatching(true);
                }}
              >
                全选筛选结果
              </Button>
            )}
            {!selectAllMatching && selectedProfileIds.length ? (
              <Button onClick={() => setSelectedProfileIds([])}>清除已选</Button>
            ) : null}
            <Button
              icon={<ReloadOutlined />}
              onClick={() => void contestantsQuery.refetch()}
              disabled={!scopeReady}
            >
              刷新名单
            </Button>
            <Button
              type="primary"
              icon={<SendOutlined />}
              loading={dispatchMutation.isPending}
              disabled={!canPush || !scopeReady || !taskId || selectedCount === 0}
              onClick={startDispatch}
            >
              推送到赛事任务
            </Button>
          </Space>
        )}
      >
        {!canPush ? (
          <Alert type="warning" showIcon message="当前账号只有查看权限，不能发起任务推送。" style={{ marginBottom: 16 }} />
        ) : null}
        {scopeReady && candidateTotal > 5000 ? (
          <Alert
            type="warning"
            showIcon
            message="当前筛选结果超过单批推送上限"
            description="全选推送上限为 5000 名选手，请先按单位筛选；也可以手动选择最多 1000 名选手。"
            style={{ marginBottom: 16 }}
          />
        ) : null}
        {!scopeReady ? (
          <Empty description="请选择赛事、赛事工种和参赛等级" />
        ) : (
          <>
            <Table<CompetitionPrePracticePushContestant>
              rowKey="profileId"
              size="middle"
              columns={contestantColumns}
              dataSource={contestants}
              loading={contestantsQuery.isFetching}
              rowSelection={rowSelection}
              pagination={false}
              locale={{ emptyText: <Empty description="当前筛选范围没有可接收任务的已激活选手" /> }}
            />
            <div className="competition-pre-practice-push-page__pagination">
              <Pagination
                current={candidatePage}
                pageSize={20}
                total={candidateTotal}
                showTotal={(total) => `共 ${total} 名选手`}
                showSizeChanger={false}
                onChange={(page) => setCandidatePage(page)}
              />
            </div>
          </>
        )}
      </Card>

      <Card bordered={false} title="推送批次记录" className="competition-pre-practice-push-page__card">
        <Table<CompetitionPrePracticeDispatch>
          rowKey="id"
          size="middle"
          columns={historyColumns}
          dataSource={historyQuery.data?.items || []}
          loading={historyQuery.isFetching}
          pagination={false}
          locale={{ emptyText: <Empty description="当前赛事范围还没有推送记录" /> }}
        />
        <div className="competition-pre-practice-push-page__pagination">
          <Pagination
            current={historyPage}
            pageSize={20}
            total={historyQuery.data?.total || 0}
            showTotal={(total) => `共 ${total} 个推送批次`}
            showSizeChanger={false}
            onChange={setHistoryPage}
          />
        </div>
      </Card>
    </div>
  );
}

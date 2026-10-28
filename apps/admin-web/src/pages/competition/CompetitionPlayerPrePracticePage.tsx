import {
  ReloadOutlined,
  SendOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Result,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  createCompetitionPrePracticeTask,
  getCompetitionPrePracticeQuestionCounts,
  pageCompetitionPrePracticeTasks,
} from '../../api/competitionPrePractice';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  listCompetitionWorkTypeStages,
  pageCompetitionEvents,
} from '../../api/competition';
import { treeWorkTypes } from '../../api/exam';
import type { ExamWorkType } from '../../types/exam';
import type {
  CompetitionPrePracticeQuestionTypeCount,
  CompetitionPrePracticeTask,
} from '../../types/competitionPrePractice';
import { useAuthStore } from '../../stores/authStore';
import { competitionEventLabel } from '../../utils/competitionEventLabel';
import './CompetitionEventManagementPage.css';

const COURSE_LEVEL_OPTIONS = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

interface SourceOption {
  value: string;
  label: string;
}

interface ConfigurationFormValues {
  eventId?: string;
  eventWorkTypeId?: string;
  eventWorkTypeLevelCode?: string;
  stageId?: string;
  title?: string;
  sourceWorkTypeIds?: string[];
  sourceLevelCodes?: number[];
  quotas?: Record<string, number | undefined>;
}

function leafWorkTypes(nodes: ExamWorkType[]) {
  const options: SourceOption[] = [];
  const visit = (items: ExamWorkType[], parents: string[]) => {
    items.forEach((item) => {
      if (item.status !== 1 || !item.id) return;
      const path = [...parents, item.title || String(item.id)];
      if (item.children?.length) {
        visit(item.children, path);
      } else {
        options.push({ value: String(item.id), label: path.join(' / ') });
      }
    });
  };
  visit(nodes || [], []);
  return options;
}

function errorMessage(error: unknown, fallback: string) {
  const responseMessage = (error as { response?: { data?: { message?: unknown } } })
    ?.response?.data?.message;
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage;
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

function taskStatusTag(task: CompetitionPrePracticeTask) {
  if (task.deliveryStatus === 'not_pushed') return <Tag color="blue">已配置 · 待推送</Tag>;
  if (task.deliveryStatus === 'pushed') return <Tag color="green">已推送 · 可再次推送</Tag>;
  return <Tag>{task.status || '已配置'}</Tag>;
}

export function CompetitionPlayerPrePracticePage() {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-pre-practice:view');
  const canCreate = permissions.includes('competition:player-pre-practice:create');
  const canReadQuestionBank = permissions.includes('exam:work-type:view')
    && permissions.includes('exam:course:view')
    && permissions.includes('exam:question:view');
  const queryClient = useQueryClient();
  const [form] = Form.useForm<ConfigurationFormValues>();
  const [taskEventFilter, setTaskEventFilter] = useState<string>();
  const [taskPage, setTaskPage] = useState(1);

  const eventId = Form.useWatch('eventId', form);
  const eventWorkTypeId = Form.useWatch('eventWorkTypeId', form);
  const eventWorkTypeLevelCode = Form.useWatch('eventWorkTypeLevelCode', form);
  const sourceWorkTypeIds = Form.useWatch('sourceWorkTypeIds', form) || [];
  const sourceLevelCodes = Form.useWatch('sourceLevelCodes', form) || [];
  const quotaValues = Form.useWatch('quotas', form) || {};

  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'pre-practice'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];

  useEffect(() => {
    if (!eventId && events.length) form.setFieldValue('eventId', String(events[0].id));
  }, [eventId, events, form]);

  const eventWorkTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId, 'pre-practice'],
    queryFn: () => listCompetitionEventWorkTypes(eventId!),
    enabled: canView && Boolean(eventId),
  });
  const eventWorkTypes = (eventWorkTypesQuery.data || []).filter((item) => item.status === 'active');
  const targetWorkTypeIsValid = eventWorkTypes.some((item) => String(item.id) === eventWorkTypeId);
  const effectiveEventWorkTypeId = targetWorkTypeIsValid ? eventWorkTypeId : undefined;

  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', eventId, effectiveEventWorkTypeId, 'pre-practice'],
    queryFn: () => listCompetitionEventWorkTypeLevels(eventId!, effectiveEventWorkTypeId!),
    enabled: canView && Boolean(eventId && effectiveEventWorkTypeId),
  });
  const levels = levelsQuery.data || [];
  const effectiveLevelCode = levels.some((item) => item.levelCode === eventWorkTypeLevelCode)
    ? eventWorkTypeLevelCode
    : undefined;

  const stagesQuery = useQuery({
    queryKey: [
      'competition-event-work-type-stages', eventId, effectiveEventWorkTypeId,
      effectiveLevelCode, 'pre-practice',
    ],
    queryFn: () => listCompetitionWorkTypeStages(eventId!, effectiveEventWorkTypeId!, effectiveLevelCode),
    enabled: canView && Boolean(eventId && effectiveEventWorkTypeId && effectiveLevelCode),
  });
  const stages = (stagesQuery.data || []).filter((item) => item.enabled === 1);

  const workTypeTreeQuery = useQuery({
    queryKey: ['exam-work-types', 'pre-practice'],
    queryFn: treeWorkTypes,
    enabled: canView && canReadQuestionBank,
  });
  const sourceWorkTypeOptions = useMemo(
    () => leafWorkTypes(workTypeTreeQuery.data || []),
    [workTypeTreeQuery.data],
  );

  const sourceScope = useMemo(() => ({
    sourceWorkTypeIds: [...sourceWorkTypeIds].sort(),
    sourceLevelCodes: [...sourceLevelCodes].sort((left, right) => right - left),
  }), [sourceWorkTypeIds, sourceLevelCodes]);
  const countsQuery = useQuery({
    queryKey: ['competition-pre-practice-question-counts', sourceScope],
    queryFn: () => getCompetitionPrePracticeQuestionCounts(sourceScope),
    enabled: canView && canReadQuestionBank
      && sourceScope.sourceWorkTypeIds.length > 0 && sourceScope.sourceLevelCodes.length > 0,
  });
  const questionCounts = countsQuery.data || [];

  useEffect(() => {
    if (!questionCounts.length) return;
    const current = form.getFieldValue('quotas') || {};
    const next = { ...current };
    questionCounts.forEach((item) => {
      if (next[item.questionType] == null) next[item.questionType] = 0;
    });
    form.setFieldValue('quotas', next);
  }, [form, questionCounts]);

  const tasksQuery = useQuery({
    queryKey: ['competition-pre-practice-tasks', taskEventFilter, taskPage],
    queryFn: () => pageCompetitionPrePracticeTasks({
      page: taskPage,
      pageSize: 20,
      eventId: taskEventFilter,
    }),
    enabled: canView,
  });

  const createMutation = useMutation({
    mutationFn: createCompetitionPrePracticeTask,
    onSuccess: async () => {
      message.success('赛前练习任务已生成，当前状态为待推送');
      await queryClient.invalidateQueries({ queryKey: ['competition-pre-practice-tasks'] });
      const selectedEventId = form.getFieldValue('eventId');
      form.resetFields();
      form.setFieldValue('eventId', selectedEventId);
    },
    onError: (error) => message.error(errorMessage(error, '生成赛前练习任务失败')),
  });

  const totalConfiguredCount = Object.values(quotaValues)
    .reduce<number>((sum, value) => sum + (typeof value === 'number' ? value : 0), 0);
  const sourceOptionsReady = sourceScope.sourceWorkTypeIds.length > 0
    && sourceScope.sourceLevelCodes.length > 0;

  const taskColumns: ColumnsType<CompetitionPrePracticeTask> = [
    {
      title: '任务名称',
      dataIndex: 'title',
      width: 230,
      render: (value: string) => <Typography.Text strong>{value}</Typography.Text>,
    },
    {
      title: '赛事范围',
      width: 290,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Typography.Text>{row.eventName}</Typography.Text>
          <Typography.Text type="secondary">
            {row.eventWorkTypeName} · {row.eventWorkTypeLevelName}
          </Typography.Text>
          <Typography.Text type="secondary">{row.stageName}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '题库范围',
      render: (_, row) => (
        <Space size={[4, 4]} wrap>
          {(row.sourceScope || []).map((scope) => (
            <Tag key={`${scope.workTypeId}:${scope.levelCode}`}>
              {scope.workTypeName} · {scope.levelName}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '题型配额',
      width: 250,
      render: (_, row) => (
        <Space size={[4, 4]} wrap>
          {(row.questionTypeCounts || []).filter((item) => item.questionCount > 0).map((item) => (
            <Tag key={item.questionType}>{item.questionTypeName} {item.questionCount}</Tag>
          ))}
        </Space>
      ),
    },
    { title: '总题数', dataIndex: 'questionCount', width: 90, align: 'right' },
    { title: '状态', width: 140, render: (_, row) => taskStatusTag(row) },
    {
      title: '生成时间',
      dataIndex: 'createdAt',
      width: 150,
      render: (value?: string) => value?.replace('T', ' ').slice(0, 16) || '—',
    },
  ];

  if (!canView) return <Result status="403" title="暂无赛前练习查看权限" />;
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}>赛前练习 · 试题配置</Typography.Title>
          <Typography.Paragraph type="secondary">
            按赛事工种、等级和赛段创建练习任务，从课程题库按题型抽取已审核试题。任务会保存试题快照，供后续选手端推送使用。
          </Typography.Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            void eventsQuery.refetch();
            void tasksQuery.refetch();
            if (sourceOptionsReady) void countsQuery.refetch();
          }}
        >刷新</Button>
      </section>

      {eventsQuery.isError ? (
        <Alert type="error" showIcon message="赛事列表加载失败" description={errorMessage(eventsQuery.error, '请刷新后重试')} />
      ) : null}

      {canCreate ? (
        <Card bordered={false} title="创建赛前练习任务" style={{ marginBottom: 16 }}>
          {!canReadQuestionBank ? (
            <Alert
              type="warning"
              showIcon
              message="当前账号缺少课程题库查看权限"
              description="请联系管理员开通工种、课程和试题查看权限后，再配置课程题库资源。"
            />
          ) : (
            <Form
              form={form}
              layout="vertical"
              onFinish={(values) => {
                if (!values.eventId || !values.eventWorkTypeId
                  || !values.eventWorkTypeLevelCode || !values.stageId) {
                  message.warning('请先完成赛事、工种、等级和赛段选择');
                  return;
                }
                if (!sourceScope.sourceWorkTypeIds.length || !sourceScope.sourceLevelCodes.length) {
                  message.warning('请至少选择一个题库工种和等级');
                  return;
                }
                if (totalConfiguredCount <= 0) {
                  message.warning('请至少配置一道试题');
                  return;
                }
                const event = events.find((item) => String(item.id) === values.eventId);
                const targetWorkType = eventWorkTypes.find((item) => String(item.id) === values.eventWorkTypeId);
                const targetLevel = levels.find((item) => item.levelCode === values.eventWorkTypeLevelCode);
                const targetStage = stages.find((item) => String(item.stageId) === values.stageId);
                const title = values.title?.trim()
                  || `${event ? competitionEventLabel(event) : '赛事'} · ${targetWorkType?.displayName || '工种'} · ${targetLevel?.levelName || values.eventWorkTypeLevelCode} · ${targetStage?.stageName || '赛段'}赛前练习`;
                const questionTypeQuotas = questionCounts.map((item) => ({
                  questionType: item.questionType,
                  questionCount: Math.max(0, Number(values.quotas?.[item.questionType] || 0)),
                }));
                createMutation.mutate({
                  eventId: values.eventId,
                  eventWorkTypeId: values.eventWorkTypeId,
                  eventWorkTypeLevelCode: values.eventWorkTypeLevelCode,
                  stageId: values.stageId,
                  title,
                  sourceScope,
                  questionTypeQuotas,
                });
              }}
            >
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, minmax(160px, 1fr))', gap: 12 }}>
                <Form.Item name="eventId" label="选择赛事" rules={[{ required: true, message: '请选择赛事' }]}>
                  <Select
                    showSearch
                    optionFilterProp="label"
                    placeholder="选择赛事"
                    options={events.map((event) => ({ value: String(event.id), label: competitionEventLabel(event) }))}
                    onChange={() => form.setFieldsValue({
                      eventWorkTypeId: undefined,
                      eventWorkTypeLevelCode: undefined,
                      stageId: undefined,
                    })}
                  />
                </Form.Item>
                <Form.Item name="eventWorkTypeId" label="赛事工种" rules={[{ required: true, message: '请选择赛事工种' }]}>
                  <Select
                    showSearch
                    optionFilterProp="label"
                    loading={eventWorkTypesQuery.isFetching}
                    disabled={!eventId || eventWorkTypes.length === 0}
                    placeholder="选择赛事工种"
                    options={eventWorkTypes.map((item) => ({
                      value: String(item.id),
                      label: item.displayName || item.workTypeName || String(item.id),
                    }))}
                    onChange={() => form.setFieldsValue({ eventWorkTypeLevelCode: undefined, stageId: undefined })}
                  />
                </Form.Item>
                <Form.Item name="eventWorkTypeLevelCode" label="参赛等级" rules={[{ required: true, message: '请选择等级' }]}>
                  <Select
                    loading={levelsQuery.isFetching}
                    disabled={!effectiveEventWorkTypeId || levels.length === 0}
                    placeholder="选择参赛等级"
                    options={levels.map((item) => ({
                      value: item.levelCode,
                      label: item.levelName || item.levelCode,
                    }))}
                    onChange={() => form.setFieldValue('stageId', undefined)}
                  />
                </Form.Item>
                <Form.Item name="stageId" label="赛段" rules={[{ required: true, message: '请选择赛段' }]}>
                  <Select
                    loading={stagesQuery.isFetching}
                    disabled={!effectiveLevelCode || stages.length === 0}
                    placeholder="选择当前等级已启用的赛段"
                    options={stages.map((item) => ({ value: String(item.stageId), label: item.stageName }))}
                  />
                </Form.Item>
              </div>

              <Form.Item name="title" label="任务名称">
                <Input maxLength={255} placeholder="留空时按赛事、工种、等级和赛段自动生成" />
              </Form.Item>

              <Typography.Title level={5} style={{ marginTop: 4 }}>课程题库范围</Typography.Title>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(240px, 1fr))', gap: 12 }}>
                <Form.Item name="sourceWorkTypeIds" label="题库工种（可多选）" rules={[{ required: true, message: '请选择题库工种' }]}>
                  <Select
                    mode="multiple"
                    showSearch
                    optionFilterProp="label"
                    loading={workTypeTreeQuery.isFetching}
                    placeholder="选择课程题库工种"
                    options={sourceWorkTypeOptions}
                    maxTagCount="responsive"
                  />
                </Form.Item>
                <Form.Item name="sourceLevelCodes" label="题库等级（可多选）" rules={[{ required: true, message: '请选择题库等级' }]}>
                  <Select
                    mode="multiple"
                    placeholder="选择课程题库等级"
                    options={COURSE_LEVEL_OPTIONS}
                    maxTagCount="responsive"
                  />
                </Form.Item>
              </div>
              <Typography.Paragraph type="secondary" style={{ marginTop: -12 }}>
                题库范围按已选工种与等级组合查询；只统计课程题库中已启用且终审通过的试题。
              </Typography.Paragraph>

              {sourceOptionsReady ? (
                <>
                  {countsQuery.isError ? (
                    <Alert
                      type="error"
                      showIcon
                      style={{ marginBottom: 12 }}
                      message="可用题量加载失败"
                      description={errorMessage(countsQuery.error, '请调整题库范围或刷新后重试')}
                    />
                  ) : null}
                  <Table<CompetitionPrePracticeQuestionTypeCount>
                    rowKey="questionType"
                    size="small"
                    loading={countsQuery.isFetching}
                    pagination={false}
                    dataSource={questionCounts}
                    columns={[
                      { title: '题型', dataIndex: 'questionTypeName' },
                      { title: '可用数量', dataIndex: 'availableCount', width: 140, align: 'right' },
                      {
                        title: '配置数量',
                        width: 180,
                        render: (_, item) => (
                          <Form.Item name={['quotas', item.questionType]} style={{ marginBottom: 0 }}>
                            <InputNumber
                              min={0}
                              max={item.availableCount}
                              precision={0}
                              disabled={item.availableCount <= 0 || countsQuery.isFetching}
                              style={{ width: 140 }}
                              placeholder="0"
                            />
                          </Form.Item>
                        ),
                      },
                    ]}
                  />
                  <Space style={{ marginTop: 12 }}>
                    <Typography.Text type="secondary">本任务配置试题</Typography.Text>
                    <Typography.Text strong>{totalConfiguredCount} 道</Typography.Text>
                  </Space>
                </>
              ) : (
                <Alert type="info" showIcon message="请先选择题库工种和等级，再配置各题型数量" />
              )}

              <Space style={{ marginTop: 20 }}>
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  htmlType="submit"
                  loading={createMutation.isPending}
                  disabled={!canCreate || !canReadQuestionBank || !questionCounts.length || countsQuery.isError}
                >生成赛前练习任务</Button>
                <Typography.Text type="secondary">本次只生成后台任务与题目快照，选手端推送后续接入。</Typography.Text>
              </Space>
            </Form>
          )}
        </Card>
      ) : (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="当前账号仅可查看赛前练习任务"
        />
      )}

      <Card
        bordered={false}
        title="已生成任务"
        extra={(
          <Space wrap>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="按赛事筛选"
              style={{ width: 300, maxWidth: '100%' }}
              value={taskEventFilter}
              options={events.map((event) => ({ value: String(event.id), label: competitionEventLabel(event) }))}
              onChange={(value) => {
                setTaskPage(1);
                setTaskEventFilter(value);
              }}
            />
            <Button icon={<ReloadOutlined />} onClick={() => void tasksQuery.refetch()}>刷新列表</Button>
          </Space>
        )}
      >
        <Table<CompetitionPrePracticeTask>
          rowKey="id"
          loading={tasksQuery.isLoading || tasksQuery.isFetching}
          dataSource={tasksQuery.data?.items || []}
          columns={taskColumns}
          scroll={{ x: 1280 }}
          locale={{ emptyText: '暂无赛前练习任务，请先完成试题配置' }}
          pagination={{
            current: taskPage,
            pageSize: 20,
            total: tasksQuery.data?.total || 0,
            showSizeChanger: false,
            onChange: (page) => setTaskPage(page),
          }}
        />
      </Card>
    </div>
  );
}

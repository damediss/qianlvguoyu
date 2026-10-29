import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Result,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import type { Dayjs } from 'dayjs';
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
import './CompetitionPlayerPrePracticePage.css';

const COURSE_LEVEL_OPTIONS = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

const QUESTION_TYPE_TAG_COLORS: Record<string, string> = {
  single: 'blue',
  multi: 'purple',
  judge: 'volcano',
  answer: 'cyan',
  cal: 'green',
  essay: 'magenta',
  analysis: 'orange',
  explain: 'geekblue',
  fill: 'lime',
  short: 'gold',
  drafting: 'red',
};

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
  effectiveTimeEnabled?: boolean;
  effectiveStartAt?: Dayjs;
  effectiveEndAt?: Dayjs;
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
  if (task.deliveryStatus === 'not_pushed') return <Tag color="blue">待推送</Tag>;
  if (task.deliveryStatus === 'pushed') return <Tag color="green">已推送</Tag>;
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
  const effectiveTimeEnabled = Form.useWatch('effectiveTimeEnabled', form) === true;
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
      width: 180,
      ellipsis: { showTitle: false },
      render: (value: string) => (
        <Typography.Text
          strong
          ellipsis={{ tooltip: value }}
          style={{ display: 'block', maxWidth: '100%' }}
          tabIndex={0}
          aria-label={value}
        >
          {value}
        </Typography.Text>
      ),
    },
    {
      title: '开放时间',
      width: 220,
      render: (_, row) => row.effectiveTimeEnabled
        ? (
          <Space direction="vertical" size={2}>
            <Typography.Text>{row.effectiveStartAt?.replace('T', ' ').slice(0, 16) || '—'}</Typography.Text>
            <Typography.Text type="secondary">
              至 {row.effectiveEndAt?.replace('T', ' ').slice(0, 16) || '—'}
            </Typography.Text>
          </Space>
        )
        : <Typography.Text type="secondary">推送后立即开放（不限时间）</Typography.Text>,
    },
    {
      title: '赛事范围',
      width: 220,
      render: (_, row) => (
        <Space direction="vertical" size={2} className="competition-pre-practice-task-event">
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
      width: 210,
      render: (_, row) => (
        <Space
          size={[4, 4]}
          wrap
          className="competition-pre-practice-task-tags"
        >
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
      width: 230,
      render: (_, row) => (
        <Space
          size={[4, 4]}
          wrap
          className="competition-pre-practice-task-tags"
        >
          {(row.questionTypeCounts || []).filter((item) => item.questionCount > 0).map((item) => (
            <Tag key={item.questionType} color={QUESTION_TYPE_TAG_COLORS[item.questionType] || 'default'}>
              {item.questionTypeName} {item.questionCount}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '总题数',
      dataIndex: 'questionCount',
      width: 80,
      align: 'center',
      onHeaderCell: () => ({ style: { textAlign: 'left' } }),
    },
    {
      title: '状态',
      width: 100,
      align: 'center',
      onHeaderCell: () => ({ style: { textAlign: 'left' } }),
      render: (_, row) => taskStatusTag(row),
    },
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
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={eventsQuery.isFetching
            || eventWorkTypesQuery.isFetching
            || levelsQuery.isFetching
            || stagesQuery.isFetching
            || workTypeTreeQuery.isFetching
            || tasksQuery.isFetching
            || countsQuery.isFetching}
          onClick={() => {
            void eventsQuery.refetch();
            void workTypeTreeQuery.refetch();
            if (eventId) void eventWorkTypesQuery.refetch();
            if (eventId && effectiveEventWorkTypeId) void levelsQuery.refetch();
            if (eventId && effectiveEventWorkTypeId && effectiveLevelCode) void stagesQuery.refetch();
            void tasksQuery.refetch();
            if (sourceOptionsReady) void countsQuery.refetch();
          }}
        >刷新</Button>
      </section>

      {eventsQuery.isError ? (
        <Alert type="error" showIcon message="赛事列表加载失败" description={errorMessage(eventsQuery.error, '请刷新后重试')} />
      ) : null}

      {canCreate ? (
        <Card
          bordered={false}
          className="competition-pre-practice-card"
          title="创建赛前练习任务"
          style={{ marginBottom: 16 }}
        >
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
              className="competition-pre-practice-form"
              initialValues={{ effectiveTimeEnabled: false }}
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
                if (values.effectiveTimeEnabled
                  && (!values.effectiveStartAt || !values.effectiveEndAt
                    || !values.effectiveEndAt.isAfter(values.effectiveStartAt))) {
                  message.warning('请填写有效的开始时间和截止时间，截止时间须晚于开始时间');
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
                  effectiveTimeEnabled: values.effectiveTimeEnabled === true,
                  effectiveStartAt: values.effectiveTimeEnabled
                    ? values.effectiveStartAt?.format('YYYY-MM-DDTHH:mm:ss')
                    : undefined,
                  effectiveEndAt: values.effectiveTimeEnabled
                    ? values.effectiveEndAt?.format('YYYY-MM-DDTHH:mm:ss')
                    : undefined,
                  sourceScope,
                  questionTypeQuotas,
                });
              }}
            >
              <section className="competition-pre-practice-target" aria-labelledby="pre-practice-target-heading">
                <div className="competition-pre-practice-section-heading">
                  <Typography.Title
                    id="pre-practice-target-heading"
                    level={5}
                    className="competition-pre-practice-section-title"
                  >
                    赛事范围
                  </Typography.Title>
                </div>
                <div className="competition-pre-practice-basic-grid">
                  <Form.Item name="eventId" label="选择赛事" rules={[{ required: true, message: '请选择赛事' }]}>
                    <Select
                      showSearch
                      optionFilterProp="label"
                      loading={eventsQuery.isFetching}
                      placeholder="选择赛事"
                      options={events.map((event) => ({ value: String(event.id), label: competitionEventLabel(event) }))}
                      onChange={() => form.setFieldsValue({
                        eventWorkTypeId: undefined,
                        eventWorkTypeLevelCode: undefined,
                        stageId: undefined,
                      })}
                    />
                  </Form.Item>
                  <Form.Item
                    name="eventWorkTypeId"
                    label="赛事工种"
                    rules={[{ required: true, message: '请选择赛事工种' }]}
                    extra={eventWorkTypesQuery.isError
                      ? '赛事工种加载失败，请点击页面顶部刷新重试。'
                      : !eventId
                        ? '请先选择赛事。'
                        : !eventWorkTypesQuery.isFetching && eventWorkTypes.length === 0
                          ? '当前赛事尚未配置可用工种。'
                          : undefined}
                  >
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
                  <Form.Item
                    name="eventWorkTypeLevelCode"
                    label="参赛等级"
                    rules={[{ required: true, message: '请选择等级' }]}
                    extra={!effectiveEventWorkTypeId
                      ? '请先选择赛事工种。'
                      : levelsQuery.isError
                        ? '参赛等级加载失败，请点击页面顶部刷新重试。'
                        : !levelsQuery.isFetching && levels.length === 0
                          ? '当前工种暂无可选参赛等级。'
                          : undefined}
                  >
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
                  <Form.Item
                    name="stageId"
                    label="赛段"
                    rules={[{ required: true, message: '请选择赛段' }]}
                    extra={!effectiveLevelCode
                      ? '请先选择参赛等级。'
                      : stagesQuery.isError
                        ? '赛段加载失败，请点击页面顶部刷新重试。'
                        : !stagesQuery.isFetching && stages.length === 0
                          ? '当前等级暂无已启用赛段。'
                          : undefined}
                  >
                    <Select
                      loading={stagesQuery.isFetching}
                      disabled={!effectiveLevelCode || stages.length === 0}
                      placeholder="选择当前等级已启用的赛段"
                      options={stages.map((item) => ({ value: String(item.stageId), label: item.stageName }))}
                    />
                  </Form.Item>
                </div>
              </section>
              <div className="competition-pre-practice-task-config">
                <Form.Item
                  name="title"
                  label="任务名称（选填）"
                  className="competition-pre-practice-task-name"
                >
                  <Input maxLength={255} placeholder="例如：2026 年度竞赛赛前练习" />
                </Form.Item>

                <section className="competition-pre-practice-time-setting" aria-labelledby="pre-practice-time-heading">
                  <div className="competition-pre-practice-time-setting__header">
                    <div className="competition-pre-practice-time-setting__copy">
                      <Typography.Text id="pre-practice-time-heading" strong>
                        限制任务开放时间
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        关闭后任务推送即可开始，默认不设置截止时间。
                      </Typography.Text>
                    </div>
                    <Form.Item name="effectiveTimeEnabled" valuePropName="checked" noStyle>
                      <Switch aria-label="限制任务开放时间" />
                    </Form.Item>
                  </div>
                  {effectiveTimeEnabled ? (
                    <div className="competition-pre-practice-time-setting__range">
                      <Form.Item
                        name="effectiveStartAt"
                        label="开始时间"
                        rules={[{ required: true, message: '请选择开始时间' }]}
                      >
                        <DatePicker
                          showTime
                          format="YYYY/MM/DD HH:mm"
                          placeholder="选择开始时间"
                          style={{ width: '100%' }}
                        />
                      </Form.Item>
                      <Form.Item
                        name="effectiveEndAt"
                        label="截止时间"
                        dependencies={['effectiveStartAt']}
                        rules={[
                          { required: true, message: '请选择截止时间' },
                          {
                            validator: (_, value?: Dayjs) => {
                              const startAt = form.getFieldValue('effectiveStartAt') as Dayjs | undefined;
                              if (!value || !startAt || value.isAfter(startAt)) return Promise.resolve();
                              return Promise.reject(new Error('截止时间必须晚于开始时间'));
                            },
                          },
                        ]}
                      >
                        <DatePicker
                          showTime
                          format="YYYY/MM/DD HH:mm"
                          placeholder="选择截止时间"
                          style={{ width: '100%' }}
                        />
                      </Form.Item>
                    </div>
                  ) : null}
                </section>
              </div>

              <section className="competition-pre-practice-scope" aria-labelledby="pre-practice-scope-heading">
                <div className="competition-pre-practice-section-heading">
                  <Typography.Title
                    id="pre-practice-scope-heading"
                    level={5}
                    className="competition-pre-practice-section-title"
                  >
                    课程题库范围
                  </Typography.Title>
                </div>
                <div className="competition-pre-practice-scope-grid">
                  <Form.Item
                    name="sourceWorkTypeIds"
                    label="题库工种（可多选）"
                    rules={[{ required: true, message: '请选择题库工种' }]}
                    extra={workTypeTreeQuery.isError
                      ? '课程题库工种加载失败，请点击页面顶部刷新重试。'
                      : workTypeTreeQuery.isSuccess && sourceWorkTypeOptions.length === 0
                        ? '当前没有可选的课程题库工种。'
                        : undefined}
                  >
                    <Select
                      mode="multiple"
                      showSearch
                      optionFilterProp="label"
                      loading={workTypeTreeQuery.isFetching}
                      disabled={workTypeTreeQuery.isError || (workTypeTreeQuery.isSuccess && sourceWorkTypeOptions.length === 0)}
                      placeholder="选择课程题库工种"
                      options={sourceWorkTypeOptions}
                      maxTagCount="responsive"
                    />
                  </Form.Item>
                  <Form.Item name="sourceLevelCodes" label="题库等级（可多选）" rules={[{ required: true, message: '请选择题库等级' }]}>
                    <Checkbox.Group
                      className="competition-pre-practice-level-options"
                      options={COURSE_LEVEL_OPTIONS}
                    />
                  </Form.Item>
                </div>
              </section>
              {sourceOptionsReady ? (
                <section
                  className="competition-pre-practice-question-counts"
                  aria-labelledby="pre-practice-counts-heading"
                >
                  <div className="competition-pre-practice-section-heading">
                    <Typography.Title
                      id="pre-practice-counts-heading"
                      level={5}
                      className="competition-pre-practice-section-title"
                    >
                      题型数量
                    </Typography.Title>
                  </div>
                  {countsQuery.isError ? (
                    <Alert
                      type="error"
                      showIcon
                      className="competition-pre-practice-question-counts__error"
                      message="可用题量加载失败"
                      description={errorMessage(countsQuery.error, '请调整题库范围或刷新后重试')}
                    />
                  ) : null}
                  {countsQuery.isSuccess && questionCounts.length === 0 ? (
                    <Alert
                      type="info"
                      showIcon
                      className="competition-pre-practice-question-counts__empty"
                      message="当前题库范围没有可用试题"
                      description="请调整题库工种或等级，或联系题库管理员补充已审核试题。"
                    />
                  ) : null}
                  <Table<CompetitionPrePracticeQuestionTypeCount>
                    rowKey="questionType"
                    size="small"
                    loading={countsQuery.isFetching}
                    pagination={false}
                    dataSource={questionCounts}
                    scroll={{ x: 520 }}
                    locale={{
                      emptyText: countsQuery.isError
                        ? '暂时无法获取题量，请刷新后重试'
                        : '当前题库范围没有可用试题',
                    }}
                    columns={[
                      { title: '题型', dataIndex: 'questionTypeName' },
                      {
                        title: '可用数量',
                        dataIndex: 'availableCount',
                        width: 140,
                        align: 'right',
                        render: (value: number) => `${value} 道`,
                      },
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
                              aria-label={`配置${item.questionTypeName}数量`}
                            />
                          </Form.Item>
                        ),
                      },
                    ]}
                  />
                  <Space
                    className="competition-pre-practice-question-counts__summary"
                    role="status"
                    aria-live="polite"
                    wrap
                  >
                    <Typography.Text type="secondary">本任务配置试题</Typography.Text>
                    <Typography.Text strong>{totalConfiguredCount} 道</Typography.Text>
                    {countsQuery.isSuccess && questionCounts.length > 0 && totalConfiguredCount === 0 ? (
                      <Typography.Text type="warning">请至少配置 1 道题后生成任务。</Typography.Text>
                    ) : null}
                  </Space>
                </section>
              ) : (
                <Alert
                  type="info"
                  showIcon
                  className="competition-pre-practice-form__hint"
                  message="请先选择题库工种和等级，再配置各题型数量"
                />
              )}

              <div className="competition-pre-practice-form__actions">
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  htmlType="submit"
                  loading={createMutation.isPending}
                  disabled={
                    !canCreate
                    || !canReadQuestionBank
                    || !questionCounts.length
                    || countsQuery.isError
                    || totalConfiguredCount <= 0
                  }
                >
                  生成赛前练习任务
                </Button>
              </div>
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
        className="competition-pre-practice-card"
        extra={(
          <Space wrap>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="按赛事筛选"
              loading={eventsQuery.isFetching}
              style={{ width: 300, maxWidth: '100%' }}
              value={taskEventFilter}
              options={events.map((event) => ({ value: String(event.id), label: competitionEventLabel(event) }))}
              onChange={(value) => {
                setTaskPage(1);
                setTaskEventFilter(value);
              }}
            />
            <Button
              icon={<ReloadOutlined />}
              loading={tasksQuery.isFetching}
              onClick={() => void tasksQuery.refetch()}
            >
              刷新列表
            </Button>
          </Space>
        )}
      >
        {tasksQuery.isError ? (
          <Alert
            type="error"
            showIcon
            className="competition-pre-practice-tasks__error"
            message={tasksQuery.data ? '任务列表刷新失败，当前仍显示上次加载的数据' : '任务列表加载失败'}
            description={errorMessage(tasksQuery.error, '请重试加载任务列表')}
            action={(
              <Button size="small" onClick={() => void tasksQuery.refetch()} loading={tasksQuery.isFetching}>
                重试
              </Button>
            )}
          />
        ) : null}
        <Table<CompetitionPrePracticeTask>
          className="competition-pre-practice-tasks-table"
          rowKey="id"
          loading={tasksQuery.isLoading || tasksQuery.isFetching}
          dataSource={tasksQuery.data?.items || []}
          columns={taskColumns}
          tableLayout="fixed"
          scroll={{ x: 1390 }}
          locale={{
            emptyText: tasksQuery.isError
              ? '暂时无法获取任务列表，请重试'
              : '暂无赛前练习任务，请先完成试题配置',
          }}
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

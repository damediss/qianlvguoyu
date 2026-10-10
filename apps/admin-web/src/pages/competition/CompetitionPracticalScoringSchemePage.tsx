import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  DeleteOutlined,
  EyeOutlined,
  PlusOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import type { Key } from 'react';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
} from '../../api/competition';
import {
  createCompetitionMakeExamPlan,
  deleteCompetitionMakeExamPlan,
  getCompetitionMakeExamPlan,
  listCompetitionMakeExamSnapshots,
  pageCompetitionMakeExamPlans,
  pageCompetitionQuestions,
  precheckCompetitionMakeExamPlan,
  previewCompetitionMakeExamSnapshot,
  publishCompetitionMakeExamPlan,
  updateCompetitionMakeExamPlan,
} from '../../api/competitionQuestion';
import type {
  CompetitionId,
  CompetitionEvent,
  CompetitionEventWorkType,
  CompetitionEventWorkTypeLevel,
} from '../../types/competition';
import type {
  CompetitionMakeExamPlan,
  CompetitionMakeExamPlanRequest,
  CompetitionMakeExamPlanPrecheck,
  CompetitionMakeExamSnapshot,
  CompetitionQuestion,
} from '../../types/competitionQuestion';
import { CompetitionPracticalSnapshotPaperPreview } from './CompetitionPracticalSnapshotPaperPreview';

interface SchemeFormValues {
  title: string;
  comments?: string;
}

function idFromUrl() {
  return new URLSearchParams(window.location.search).get('eventId') || '';
}

function readableError(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

function plainText(value?: string | null) {
  return (value ?? '').replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim();
}

function scoreOf(question?: CompetitionQuestion) {
  return Number(question?.practicalDetail?.totalScore ?? 0);
}

function durationOf(question?: CompetitionQuestion) {
  return Number(question?.practicalDetail?.durationMinutes ?? 0);
}

function scoreMethodLabel(value?: string | null) {
  return value === 'additive' ? '加分制' : value === 'deductive' ? '扣分制' : '未配置';
}

export function CompetitionPracticalScoringSchemePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<SchemeFormValues>();
  const [eventId, setEventId] = useState(idFromUrl);
  const [workTypeId, setWorkTypeId] = useState('');
  const [levelId, setLevelId] = useState('');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [editing, setEditing] = useState<CompetitionMakeExamPlan | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [precheck, setPrecheck] = useState<CompetitionMakeExamPlanPrecheck | null>(null);
  const [previewPlanId, setPreviewPlanId] = useState<string | null>(null);
  const [previewSnapshotId, setPreviewSnapshotId] = useState<string | null>(null);

  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'practical-scoring-scheme'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }),
  });
  const events = eventsQuery.data?.items ?? [];
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', 'practical-scoring-scheme', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: Boolean(eventId),
  });
  const workTypes = workTypesQuery.data ?? [];
  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', 'practical-scoring-scheme', eventId, workTypeId],
    queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId),
    enabled: Boolean(eventId && workTypeId),
  });
  const levels = levelsQuery.data ?? [];
  const questionsQuery = useQuery({
    queryKey: ['competition-practical-scheme-source-projects', eventId, workTypeId, levelId],
    queryFn: () => pageCompetitionQuestions(eventId, {
      page: 1,
      pageSize: 500,
      eventWorkTypeId: workTypeId,
      eventWorkTypeLevelId: levelId,
      contentType: 'practical',
      questionType: 'practical',
    }),
    enabled: Boolean(eventId && workTypeId && levelId),
  });
  const questions = questionsQuery.data?.items ?? [];
  const questionById = useMemo(() => new Map(questions.map(question => [String(question.id), question])), [questions]);
  const selectedQuestions = useMemo(
    () => selectedIds.map(id => questionById.get(id)).filter((question): question is CompetitionQuestion => Boolean(question)),
    [questionById, selectedIds],
  );
  const scoreTotalCents = selectedQuestions.reduce((total, question) => total + Math.round(scoreOf(question) * 100), 0);
  const scoreTotal = scoreTotalCents / 100;
  const durationTotal = selectedQuestions.reduce((total, question) => total + durationOf(question), 0);
  const plansQuery = useQuery({
    queryKey: ['competition-make-plans', 'practical-scoring-scheme', eventId, workTypeId, levelId],
    queryFn: () => pageCompetitionMakeExamPlans(eventId, {
      page: 1,
      pageSize: 100,
      eventWorkTypeId: workTypeId,
      eventWorkTypeLevelId: levelId,
      contentType: 'practical',
      simpleMode: true,
    }),
    enabled: Boolean(eventId && workTypeId && levelId),
  });
  const snapshotsQuery = useQuery({
    queryKey: ['competition-make-plan-snapshots', eventId, previewPlanId],
    queryFn: () => listCompetitionMakeExamSnapshots(eventId, previewPlanId!),
    enabled: Boolean(eventId && previewPlanId),
  });
  const previewQuery = useQuery({
    queryKey: ['competition-make-snapshot-preview', eventId, previewSnapshotId],
    queryFn: () => previewCompetitionMakeExamSnapshot(eventId, previewSnapshotId!),
    enabled: Boolean(eventId && previewSnapshotId),
  });

  useEffect(() => { if (!eventId && events[0]) setEventId(String(events[0].id)); }, [eventId, events]);
  useEffect(() => { if (!workTypeId && workTypes[0]) setWorkTypeId(String(workTypes[0].id)); }, [workTypeId, workTypes]);
  useEffect(() => { if (!levelId && levels[0]) setLevelId(String(levels[0].id)); }, [levelId, levels]);
  useEffect(() => {
    if (snapshotsQuery.data?.length && !snapshotsQuery.data.some(item => item.id === previewSnapshotId)) {
      setPreviewSnapshotId(snapshotsQuery.data[0].id);
    }
  }, [previewSnapshotId, snapshotsQuery.data]);

  const buildRequest = (values: SchemeFormValues): CompetitionMakeExamPlanRequest => ({
    eventWorkTypeId: workTypeId,
    eventWorkTypeLevelId: levelId,
    contentType: 'practical',
    title: values.title.trim(),
    examType: 'PRACTICAL_SCORING_SCHEME',
    durationMinutes: durationTotal,
    totalScore: 100,
    passScore: 0,
    comments: values.comments?.trim(),
    details: [],
    paperCount: 1,
    practicalQuestionIds: selectedIds,
  });

  const openNew = (source?: CompetitionMakeExamPlan) => {
    const sourceIds = source?.practicalQuestionIds?.map(String) ?? [];
    setEditing(null);
    setSelectedIds(sourceIds);
    setPrecheck(null);
    form.resetFields();
    form.setFieldsValue({
      title: source ? `${source.title}（新版本）` : '实操评分方案',
      comments: source?.comments ?? undefined,
    });
    setFormOpen(true);
  };

  const openEdit = async (record: CompetitionMakeExamPlan) => {
    try {
      const detail = await getCompetitionMakeExamPlan(eventId, record.id);
      setEditing(detail);
      setSelectedIds((detail.practicalQuestionIds ?? []).map(String));
      setPrecheck(null);
      form.setFieldsValue({ title: detail.title, comments: detail.comments ?? undefined });
      setFormOpen(true);
    } catch (error) {
      message.error(readableError(error, '读取评分方案失败'));
    }
  };

  const precheckMutation = useMutation({
    mutationFn: async (values: SchemeFormValues) => precheckCompetitionMakeExamPlan(eventId, buildRequest(values)),
    onSuccess: result => {
      setPrecheck(result);
      if (result.ready) message.success('方案配置完整，可以保存');
      else message.error(result.errors.join('；') || '方案校验未通过');
    },
    onError: error => message.error(readableError(error, '评分方案校验失败')),
  });
  const saveMutation = useMutation({
    mutationFn: async (values: SchemeFormValues) => {
      const body = buildRequest(values);
      const result = await precheckCompetitionMakeExamPlan(eventId, body);
      setPrecheck(result);
      if (!result.ready) throw new Error(result.errors.join('；') || '评分方案校验未通过');
      return editing
        ? updateCompetitionMakeExamPlan(eventId, editing.id, body)
        : createCompetitionMakeExamPlan(eventId, body);
    },
    onSuccess: async () => {
      message.success(editing ? '评分方案草稿已更新' : '评分方案草稿已创建');
      setFormOpen(false);
      setEditing(null);
      setSelectedIds([]);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['competition-make-plans', 'practical-scoring-scheme'] });
    },
    onError: error => message.error(readableError(error, '评分方案保存失败')),
  });
  const publishMutation = useMutation({
    mutationFn: (planId: string) => publishCompetitionMakeExamPlan(eventId, planId),
    onSuccess: async () => {
      message.success('评分方案已发布，发布快照不可修改');
      await queryClient.invalidateQueries({ queryKey: ['competition-make-plans', 'practical-scoring-scheme'] });
    },
    onError: error => message.error(readableError(error, '评分方案发布失败')),
  });
  const deleteMutation = useMutation({
    mutationFn: (planId: string) => deleteCompetitionMakeExamPlan(eventId, planId),
    onSuccess: async () => {
      message.success('评分方案草稿已删除');
      await queryClient.invalidateQueries({ queryKey: ['competition-make-plans', 'practical-scoring-scheme'] });
    },
    onError: error => message.error(readableError(error, '评分方案删除失败')),
  });

  const moveSelected = (questionId: string, offset: -1 | 1) => {
    const index = selectedIds.indexOf(questionId);
    const target = index + offset;
    if (index < 0 || target < 0 || target >= selectedIds.length) return;
    const next = [...selectedIds];
    [next[index], next[target]] = [next[target], next[index]];
    setSelectedIds(next);
    setPrecheck(null);
  };

  const selectedRowSelection = {
    selectedRowKeys: selectedIds,
    preserveSelectedRowKeys: true,
    onChange: (keys: Key[]) => {
      const nextKeys = keys.map(String);
      if (nextKeys.length > 50) {
        message.warning('一个评分方案最多选择50个实操项目');
        return;
      }
      const retained = selectedIds.filter(id => nextKeys.includes(id));
      setSelectedIds([...retained, ...nextKeys.filter(id => !retained.includes(id))]);
      setPrecheck(null);
    },
    getCheckboxProps: (question: CompetitionQuestion) => ({
      disabled: question.status !== 'enabled' || scoreOf(question) <= 0 || durationOf(question) <= 0,
    }),
  };

  const flattenElements = (question: CompetitionQuestion) => (question.practicalDetail?.assessmentContents ?? [])
    .flatMap(content => (content.elements ?? []).map(element => ({
      key: `${content.id}-${element.id}`,
      content: plainText(content.contentHtml),
      element: plainText(element.elementHtml),
      standard: plainText(element.standardHtml),
      itemName: element.itemName,
      maxScore: element.maxScore,
      maxDeduction: element.maxDeduction,
      deductionStep: element.deductionStep,
      allocationEnabled: element.allocationEnabled,
    })));

  const planRows = plansQuery.data?.items ?? [];
  const currentEvent = events.find(event => String(event.id) === eventId);
  const currentWorkType = workTypes.find(workType => String(workType.id) === workTypeId);
  const currentLevel = levels.find(level => String(level.id) === levelId);

  return (
    <Card
      title="实操评分方案"
      extra={<Button type="primary" icon={<PlusOutlined />} disabled={!eventId || !workTypeId || !levelId} onClick={() => openNew()}>新建方案</Button>}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="先从当前赛事工种等级下的实操题库选择项目，再按顺序组成一份评分方案。"
        description="方案按所选项目的原始分值汇总，合计必须为100分，不做比例换算。保存后生成草稿快照；发布后作为不可变版本供赛段运行规则绑定。考试时长、启停方式和时间加扣分仍在赛段运行规则中配置。"
      />
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          showSearch
          optionFilterProp="label"
          style={{ width: 260 }}
          placeholder="选择赛事"
          value={eventId || undefined}
          loading={eventsQuery.isLoading}
          options={events.map((event: CompetitionEvent) => ({ value: String(event.id), label: `${event.name}（${event.code}）` }))}
          onChange={value => { setEventId(value); setWorkTypeId(''); setLevelId(''); setSelectedIds([]); setPrecheck(null); }}
        />
        <Select
          style={{ width: 230 }}
          placeholder="选择工种"
          value={workTypeId || undefined}
          loading={workTypesQuery.isLoading}
          options={workTypes.map((item: CompetitionEventWorkType) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) }))}
          onChange={value => { setWorkTypeId(value); setLevelId(''); setSelectedIds([]); setPrecheck(null); }}
        />
        <Select
          style={{ width: 190 }}
          placeholder="选择等级"
          value={levelId || undefined}
          loading={levelsQuery.isLoading}
          options={levels.map((item: CompetitionEventWorkTypeLevel) => ({ value: String(item.id), label: `${item.levelName}（${item.levelCode}）` }))}
          onChange={value => { setLevelId(value); setSelectedIds([]); setPrecheck(null); }}
        />
      </Space>
      {currentEvent && currentWorkType && currentLevel && (
        <Typography.Paragraph type="secondary">
          当前范围：{currentEvent.name} / {currentWorkType.displayName || currentWorkType.workTypeName || currentWorkType.workTypeId} / {currentLevel.levelName}
        </Typography.Paragraph>
      )}
      <Table<CompetitionMakeExamPlan>
        rowKey="id"
        loading={plansQuery.isLoading || plansQuery.isFetching}
        dataSource={planRows}
        pagination={{ pageSize: 20, showSizeChanger: false }}
        scroll={{ x: 'max-content' }}
        columns={[
          { title: '方案名称', dataIndex: 'title', ellipsis: true },
          { title: '实操项目数', dataIndex: 'practicalQuestionIds', width: 110, render: (ids?: CompetitionId[]) => ids?.length ?? 0 },
          { title: '方案总分', dataIndex: 'totalScore', width: 100, render: (value: string | number) => `${value} 分` },
          { title: '参考时长', dataIndex: 'durationMinutes', width: 110, render: (value: number) => `${value} 分钟` },
          { title: '版本', dataIndex: 'planVersion', width: 90, render: (value: number) => `v${value}` },
          { title: '状态', dataIndex: 'status', width: 100, render: (value: string) => <Tag color={value === 'published' ? 'green' : 'gold'}>{value === 'published' ? '已发布' : value === 'draft' ? '草稿' : value}</Tag> },
          {
            title: '操作',
            key: 'actions',
            width: 330,
            fixed: 'right' as const,
            render: (_: unknown, record: CompetitionMakeExamPlan) => (
              <Space wrap size={[8, 0]}>
                <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => { setPreviewPlanId(record.id); setPreviewSnapshotId(null); }}>预览</Button>
                {record.status === 'draft' && <Button type="link" size="small" onClick={() => void openEdit(record)}>编辑</Button>}
                {record.status === 'draft' && <Popconfirm title="发布后方案快照不可修改，并可供匹配赛段使用。确认发布？" onConfirm={() => publishMutation.mutate(record.id)}><Button type="link" size="small" icon={<SendOutlined />}>发布</Button></Popconfirm>}
                {record.status === 'published' && <Button type="link" size="small" onClick={() => openNew(record)}>复制新版本</Button>}
                {record.status === 'draft' && <Popconfirm title="确认删除此评分方案草稿？" onConfirm={() => deleteMutation.mutate(record.id)}><Button type="link" danger size="small" icon={<DeleteOutlined />}>删除</Button></Popconfirm>}
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title={`${editing ? '编辑' : '新建'}实操评分方案`}
        open={formOpen}
        onCancel={() => setFormOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={saveMutation.isPending}
        destroyOnClose
        width={1100}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={values => saveMutation.mutate(values)}
          onValuesChange={() => setPrecheck(null)}
        >
          <Space wrap align="start">
            <Form.Item name="title" label="方案名称" rules={[{ required: true, whitespace: true, message: '请输入方案名称' }]}>
              <Input maxLength={100} style={{ width: 360 }} />
            </Form.Item>
            <Form.Item label="方案总分"><Input value="100 分（固定）" readOnly style={{ width: 180 }} /></Form.Item>
            <Form.Item label="所选项目参考时长"><Input value={`${durationTotal} 分钟`} readOnly style={{ width: 180 }} /></Form.Item>
          </Space>
          <Typography.Title level={5}>选择实操项目</Typography.Title>
          <Table<CompetitionQuestion>
            rowKey="id"
            size="small"
            loading={questionsQuery.isLoading || questionsQuery.isFetching}
            dataSource={questions}
            rowSelection={selectedRowSelection}
            pagination={{ pageSize: 8, showSizeChanger: false }}
            expandable={{
              expandedRowRender: question => (
                <Table
                  size="small"
                  rowKey="key"
                  pagination={false}
                  dataSource={flattenElements(question)}
                  columns={[
                    { title: '考评内容', dataIndex: 'content', width: 180, ellipsis: true },
                    { title: '评分项', dataIndex: 'itemName', width: 150, render: (value: string | undefined, row: { element: string }) => value || row.element || '-' },
                    { title: '评分标准', dataIndex: 'standard', ellipsis: true },
                    { title: '配分', dataIndex: 'maxScore', width: 80 },
                    { title: '最大扣分', dataIndex: 'maxDeduction', width: 90 },
                    { title: '扣分步长', dataIndex: 'deductionStep', width: 90 },
                    { title: '参与评分', dataIndex: 'allocationEnabled', width: 90, render: (value: boolean) => value ? '是' : '否' },
                  ]}
                />
              ),
            }}
            columns={[
              { title: '实操项目', dataIndex: 'stem', ellipsis: true },
              { title: '原始分值', width: 100, render: (_: unknown, row: CompetitionQuestion) => `${scoreOf(row)} 分` },
              { title: '参考时长', width: 100, render: (_: unknown, row: CompetitionQuestion) => `${durationOf(row)} 分钟` },
              { title: '评分方式', width: 100, render: (_: unknown, row: CompetitionQuestion) => scoreMethodLabel(row.practicalDetail?.scoringMethod) },
              { title: '评分项', width: 90, render: (_: unknown, row: CompetitionQuestion) => row.scoreItems?.length ?? 0 },
              { title: '状态', dataIndex: 'status', width: 90, render: (value: string) => <Tag color={value === 'enabled' ? 'green' : 'default'}>{value === 'enabled' ? '启用' : '停用'}</Tag> },
            ]}
          />
          <Space style={{ marginTop: 16, marginBottom: 8 }}>
            <Typography.Text strong>方案组成顺序</Typography.Text>
            <Typography.Text type="secondary">调整后的顺序会固化到发布快照中。</Typography.Text>
          </Space>
          <Table<CompetitionQuestion>
            rowKey="id"
            size="small"
            pagination={false}
            dataSource={selectedQuestions}
            locale={{ emptyText: '请先从上方选择一个或多个实操项目' }}
            columns={[
              { title: '顺序', width: 70, render: (_: unknown, row: CompetitionQuestion) => selectedIds.indexOf(row.id) + 1 },
              { title: '实操项目', dataIndex: 'stem', ellipsis: true },
              { title: '原始分值', width: 100, render: (_: unknown, row: CompetitionQuestion) => `${scoreOf(row)} 分` },
              { title: '参考时长', width: 100, render: (_: unknown, row: CompetitionQuestion) => `${durationOf(row)} 分钟` },
              {
                title: '操作',
                width: 130,
                render: (_: unknown, row: CompetitionQuestion) => {
                  const index = selectedIds.indexOf(row.id);
                  return <Space>
                    <Button aria-label="上移" size="small" icon={<ArrowUpOutlined />} disabled={index <= 0} onClick={() => moveSelected(row.id, -1)} />
                    <Button aria-label="下移" size="small" icon={<ArrowDownOutlined />} disabled={index < 0 || index >= selectedIds.length - 1} onClick={() => moveSelected(row.id, 1)} />
                    <Button aria-label="移除" size="small" danger icon={<DeleteOutlined />} onClick={() => { setSelectedIds(selectedIds.filter(id => id !== row.id)); setPrecheck(null); }} />
                  </Space>;
                },
              },
            ]}
          />
          <Descriptions size="small" bordered column={3} style={{ marginTop: 12, marginBottom: 12 }}>
            <Descriptions.Item label="项目数量">{selectedIds.length}</Descriptions.Item>
            <Descriptions.Item label="所选原始分值合计">
              <Typography.Text type={scoreTotalCents === 10000 ? 'success' : 'danger'}>{scoreTotal} / 100 分</Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="参考时长合计">{durationTotal} 分钟</Descriptions.Item>
          </Descriptions>
          {selectedIds.length > 0 && scoreTotalCents !== 10000 && (
            <Alert type="warning" showIcon style={{ marginBottom: 12 }} message="所选项目原始分值必须刚好合计100分；方案不会自动缩放项目分值。" />
          )}
          {precheck && <Alert
            type={precheck.ready ? 'success' : 'error'}
            showIcon
            style={{ marginBottom: 12 }}
            message={precheck.ready ? '方案校验通过' : precheck.errors.join('；') || '方案校验未通过'}
          />}
          <Space direction="vertical" style={{ width: '100%' }}>
            <Button
              loading={precheckMutation.isPending}
              disabled={!selectedIds.length || scoreTotalCents !== 10000 || !durationTotal}
              onClick={() => form.validateFields().then(values => precheckMutation.mutate(values)).catch(() => undefined)}
            >校验方案</Button>
            <Form.Item name="comments" label="备注" style={{ marginBottom: 0 }}><Input.TextArea rows={2} maxLength={500} /></Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal
        title="实操评分方案快照预览"
        open={Boolean(previewPlanId)}
        onCancel={() => { setPreviewPlanId(null); setPreviewSnapshotId(null); }}
        footer={null}
        width={1000}
        styles={{ body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto', padding: 0 } }}
      >
        {snapshotsQuery.isLoading ? <Spin /> : snapshotsQuery.data ? <>
          <Space wrap style={{ padding: '16px 20px 0' }}>
            {snapshotsQuery.data.map((snapshot: CompetitionMakeExamSnapshot) => (
              <Button key={snapshot.id} type={snapshot.id === previewSnapshotId ? 'primary' : 'default'} onClick={() => setPreviewSnapshotId(snapshot.id)}>
                {snapshot.paperCode} 卷 / v{snapshot.versionNo} <Tag>{snapshot.status}</Tag>
              </Button>
            ))}
          </Space>
          {previewQuery.isLoading ? <Spin /> : previewQuery.data
            ? <CompetitionPracticalSnapshotPaperPreview snapshot={previewQuery.data} />
            : <Alert type="warning" message="无法加载评分方案快照" />}
        </> : <Alert type="warning" message="暂无快照" />}
      </Modal>
    </Card>
  );
}

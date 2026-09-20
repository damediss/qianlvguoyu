import { DeleteOutlined, EyeOutlined, EditOutlined, PlusOutlined, SendOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import { useEffect, useState } from 'react';
import {
  createCompetitionMakeExamPlan,
  deleteCompetitionMakeExamPlan,
  getCompetitionMakeExamPlan,
  listCompetitionMakeExamSnapshots,
  pageCompetitionMakeExamPlans,
  previewCompetitionMakeExamSnapshot,
  publishCompetitionMakeExamPlan,
  updateCompetitionMakeExamPlan,
  precheckCompetitionMakeExamPlan,
  pageCompetitionQuestions,
} from '../../api/competitionQuestion';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents } from '../../api/competition';
import { getCompetitionQuestionBlueprintTree, pageCompetitionQuestionBlueprints } from '../../api/competitionQuestionBlueprint';
import type { CompetitionEvent, CompetitionEventWorkType, CompetitionEventWorkTypeLevel } from '../../types/competition';
import type { CompetitionMakeExamPlan, CompetitionMakeExamPlanRequest, CompetitionQuestion, CompetitionQuestionContentType, CompetitionQuestionType } from '../../types/competitionQuestion';
import { CompetitionPracticalSnapshotPaperPreview } from './CompetitionPracticalSnapshotPaperPreview';

interface PlanFormValues {
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  practicalQuestionId?: string;
  blueprintId?: string;
  title: string;
  examType?: string;
  durationMinutes: number;
  totalScore: number;
  passScore: number;
  paperCount?: number;
  randomSeed?: string;
  comments?: string;
  details: Array<{
    questionType: CompetitionQuestionType;
    questionCount: number;
    point: number;
    difficultyStart?: number;
    difficultyEnd?: number;
    sortOrder: number;
  }>;
}

const typeLabels: Record<string, string> = {
  single: '单选题',
  multi: '多选题',
  judge: '判断题',
  fill: '填空题',
  practical: '实操题',
};

function getEventIdFromUrl() {
  return new URLSearchParams(window.location.search).get('eventId') || '';
}

export function CompetitionMakeExamPlanPage() {
  const queryClient = useQueryClient();
  const [contentType, setContentType] = useState<CompetitionQuestionContentType>('theory');
  const [eventId, setEventId] = useState(getEventIdFromUrl);
  const [workTypeId, setWorkTypeId] = useState('');
  const [levelId, setLevelId] = useState('');
  const [editing, setEditing] = useState<CompetitionMakeExamPlan | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [previewPlanId, setPreviewPlanId] = useState<string | null>(null);
  const [previewSnapshotId, setPreviewSnapshotId] = useState<string | null>(null);
  const [theoryPrecheck, setTheoryPrecheck] = useState<Awaited<ReturnType<typeof precheckCompetitionMakeExamPlan>> | null>(null);
  const [practicalPrecheck, setPracticalPrecheck] = useState<Awaited<ReturnType<typeof precheckCompetitionMakeExamPlan>> | null>(null);
  const [form] = Form.useForm<PlanFormValues>();

  const eventsQuery = useQuery({ queryKey: ['competition-events', 'make-plan'], queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }) });
  const events = eventsQuery.data?.items ?? [];
  const workTypesQuery = useQuery({ queryKey: ['competition-event-work-types', 'make-plan', eventId], queryFn: () => listCompetitionEventWorkTypes(eventId), enabled: Boolean(eventId) });
  const workTypes = workTypesQuery.data ?? [];
  const levelsQuery = useQuery({ queryKey: ['competition-event-levels', 'make-plan', eventId, workTypeId], queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId), enabled: Boolean(eventId && workTypeId) });
  const levels = levelsQuery.data ?? [];
  const blueprintQuery = useQuery({
    queryKey: ['competition-plan-active-blueprint', eventId, workTypeId, levelId, contentType],
    queryFn: () => pageCompetitionQuestionBlueprints(eventId, {
      page: 1, pageSize: 20, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType, status: 'active',
    }),
    enabled: Boolean(eventId && workTypeId && levelId),
  });
  const activeBlueprint = blueprintQuery.data?.items?.[0];
  const selectedBlueprintId = editing ? editing.blueprintId || undefined : activeBlueprint?.id;
  const blueprintTreeQuery = useQuery({
    queryKey: ['competition-plan-blueprint-tree', eventId, selectedBlueprintId],
    queryFn: () => getCompetitionQuestionBlueprintTree(eventId, selectedBlueprintId!),
    enabled: Boolean(eventId && selectedBlueprintId),
  });
  const practicalQuestionsQuery = useQuery({
    queryKey: ['competition-practical-question-options', eventId, workTypeId, levelId, selectedBlueprintId],
    queryFn: () => pageCompetitionQuestions(eventId, {
      page: 1, pageSize: 500, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId,
      contentType: 'practical', questionType: 'practical',
      blueprintId: undefined,
    }),
    enabled: contentType === 'practical' && !selectedBlueprintId && Boolean(eventId && workTypeId && levelId),
  });
  const practicalQuestions = practicalQuestionsQuery.data?.items ?? [];
  const plansQuery = useQuery({
    queryKey: ['competition-make-plans', eventId, workTypeId, levelId, contentType],
    queryFn: () => pageCompetitionMakeExamPlans(eventId, { page: 1, pageSize: 100, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType }),
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

  const buildRequest = (values: PlanFormValues): CompetitionMakeExamPlanRequest => {
    const requestBlueprintId = values.blueprintId || editing?.blueprintId || (!editing ? activeBlueprint?.id : undefined);
    if (contentType === 'practical') {
      const selected = practicalQuestions.find((question) => question.id === values.practicalQuestionId);
      const practicalDetail = selected?.practicalDetail;
      const blueprintPractical = Boolean(requestBlueprintId);
      return {
        eventWorkTypeId: values.eventWorkTypeId,
        eventWorkTypeLevelId: values.eventWorkTypeLevelId,
        practicalQuestionId: blueprintPractical ? undefined : values.practicalQuestionId,
        blueprintId: requestBlueprintId,
        contentType,
        title: values.title,
        examType: values.examType,
        durationMinutes: Number(values.durationMinutes),
        totalScore: Number(values.totalScore),
        passScore: values.passScore,
        paperCount: Number(values.paperCount || 1),
        randomSeed: values.randomSeed,
        comments: values.comments,
        details: blueprintPractical
          ? []
          : [{ questionType: 'practical', questionCount: 1, point: Number(practicalDetail?.totalScore || values.totalScore), sortOrder: 1 }],
      };
    }
    // Do not spread form values here: an old form instance may still contain
    // practical topicRules after a tab switch, but theory requests must never
    // send them to the server.
    return {
      eventWorkTypeId: values.eventWorkTypeId,
      eventWorkTypeLevelId: values.eventWorkTypeLevelId,
      blueprintId: requestBlueprintId,
      contentType,
      title: values.title,
      examType: values.examType,
      durationMinutes: Number(values.durationMinutes),
      totalScore: Number(values.totalScore),
      passScore: values.passScore,
      paperCount: Number(values.paperCount || 1),
      randomSeed: values.randomSeed,
      comments: values.comments,
      details: values.details,
    };
  };

  const precheckMutation = useMutation({
    mutationFn: (values: PlanFormValues) => precheckCompetitionMakeExamPlan(eventId, buildRequest(values)),
    onSuccess: (result) => {
      if (contentType === 'practical') setPracticalPrecheck(result);
      else setTheoryPrecheck(result);
    },
    onError: (error) => { message.error(error instanceof Error ? error.message : '组卷预检失败'); },
  });

  const saveMutation = useMutation({
    mutationFn: async (values: PlanFormValues) => {
      const body = buildRequest(values);
      const precheck = await precheckCompetitionMakeExamPlan(eventId, body);
      if (contentType === 'theory') setTheoryPrecheck(precheck);
      else setPracticalPrecheck(precheck);
      if (!precheck.ready) {
        const shortages = precheck.topics
          .filter((topic) => topic.shortage > 0)
          .map((topic) => `${topic.path} 缺 ${topic.shortage} 题`);
        const detailShortages = precheck.details
          .filter((detail) => detail.shortage > 0)
          .map((detail) => `${typeLabels[detail.questionType] || detail.questionType} 缺 ${detail.shortage} 题`);
        const practicalShortages = (precheck.practicalAllocations || [])
          .filter((item) => item.candidateQuestionCount <= 0)
          .map((item) => `${item.path} 缺少可用完整实操题`);
        const reason = [...precheck.errors, ...shortages, ...detailShortages, ...practicalShortages].join('；') || '候选题不足，请调整细目表或题库';
        throw new Error(`组卷预检未通过：${reason}`);
      }
      return editing ? updateCompetitionMakeExamPlan(eventId, editing.id, body) : createCompetitionMakeExamPlan(eventId, body);
    },
    onSuccess: () => {
      message.success(editing ? '组卷计划已更新并生成新草稿快照' : '组卷计划已保存并生成草稿快照');
      setFormOpen(false);
      setEditing(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-make-plans'] });
    },
    onError: (error) => { message.error(error instanceof Error ? error.message : '组卷预检或保存失败'); },
  });
  const publishMutation = useMutation({
    mutationFn: (planId: string) => publishCompetitionMakeExamPlan(eventId, planId),
    onSuccess: () => { message.success('组卷计划及快照已发布'); queryClient.invalidateQueries({ queryKey: ['competition-make-plans'] }); },
  });
  const deleteMutation = useMutation({
    mutationFn: (planId: string) => deleteCompetitionMakeExamPlan(eventId, planId),
    onSuccess: () => { message.success('组卷计划已删除'); queryClient.invalidateQueries({ queryKey: ['competition-make-plans'] }); },
  });

  const openCreate = () => {
    setEditing(null);
    setTheoryPrecheck(null);
    setPracticalPrecheck(null);
    form.resetFields();
    const firstPractical = contentType === 'practical' ? practicalQuestions[0] : undefined;
    const defaultDetails = contentType === 'theory'
      ? [{ questionType: 'single' as CompetitionQuestionType, questionCount: 10, point: 10, sortOrder: 1 }]
      : selectedBlueprintId ? [] : [{ questionType: 'practical' as CompetitionQuestionType, questionCount: 1, point: 100, sortOrder: 1 }];
    form.setFieldsValue({
      eventWorkTypeId: workTypeId,
      eventWorkTypeLevelId: levelId,
      durationMinutes: 90,
      totalScore: 100,
      passScore: 60,
      paperCount: contentType === 'practical' ? 1 : 1,
      details: defaultDetails,
      practicalQuestionId: firstPractical?.id,
      blueprintId: activeBlueprint?.id,
      randomSeed: undefined,
    });
    if (firstPractical?.practicalDetail) {
      form.setFieldsValue({
        durationMinutes: firstPractical.practicalDetail.durationMinutes || 60,
        totalScore: Number(firstPractical.practicalDetail.totalScore || 100),
      });
    }
    setFormOpen(true);
  };
  const openEdit = async (record: CompetitionMakeExamPlan) => {
    const detail = await getCompetitionMakeExamPlan(eventId, record.id);
    setEditing(detail);
    setTheoryPrecheck(null);
    setPracticalPrecheck(null);
    form.setFieldsValue({
      eventWorkTypeId: detail.eventWorkTypeId,
      eventWorkTypeLevelId: detail.eventWorkTypeLevelId,
      practicalQuestionId: detail.practicalQuestionId || undefined,
      // Do not silently migrate a legacy plan to the currently active
      // blueprint when it is opened for review/editing.
      blueprintId: detail.blueprintId || undefined,
      title: detail.title,
      examType: detail.examType || undefined,
      durationMinutes: detail.durationMinutes,
      totalScore: Number(detail.totalScore),
      passScore: Number(detail.passScore),
      paperCount: detail.paperCount || 1,
      randomSeed: detail.randomSeed || undefined,
      comments: detail.comments || undefined,
      details: detail.details.map(item => ({
        questionType: item.questionType,
        questionCount: item.questionCount,
        point: Number(item.point),
        difficultyStart: item.difficultyStart || undefined,
        difficultyEnd: item.difficultyEnd || undefined,
        sortOrder: item.sortOrder,
      })),
    });
    if (contentType === 'practical' && detail.practicalQuestionId) {
      const selected = practicalQuestions.find((question) => question.id === detail.practicalQuestionId);
      if (selected?.practicalDetail) {
        form.setFieldsValue({
          durationMinutes: selected.practicalDetail.durationMinutes || detail.durationMinutes,
          totalScore: Number(selected.practicalDetail.totalScore || detail.totalScore),
        });
      }
    }
    setFormOpen(true);
  };

  const rows = plansQuery.data?.items ?? [];
  const questionTypeOptions = contentType === 'theory'
    ? ['single', 'multi', 'judge', 'fill'].map(value => ({ value, label: typeLabels[value] }))
    : [{ value: 'practical', label: '实操题' }];

  return (
    <Card
      title="竞赛组卷计划"
      extra={<Button type="primary" icon={<PlusOutlined />} disabled={!eventId || !workTypeId || !levelId} onClick={openCreate}>新建计划</Button>}
    >
      <Alert type="info" showIcon style={{ marginBottom: 16 }} message={contentType === 'practical'
        ? '实操题量由细目表的层级选考关系决定：下级为必考时每个下级各抽1题；一级必考、二级任选一项时，每个一级只形成1个题位，再从该一级的二级范围中任选一个并抽1道完整题。'
        : '理论组卷按组卷规则和细目表三级权重自动抽题；理论分布不可手工修改。'} />
      {contentType === 'theory' && !activeBlueprint && eventId && workTypeId && levelId && <Alert type="warning" showIcon style={{ marginBottom: 16 }} message="当前赛事工种等级没有启用的理论细目表；理论自动组卷需要先导入并启用版本。" />}
      <Tabs activeKey={contentType} onChange={(value) => {
        setContentType(value as CompetitionQuestionContentType);
        setEditing(null);
        setTheoryPrecheck(null);
        setPracticalPrecheck(null);
      }} items={[{ key: 'theory', label: '理论组卷' }, { key: 'practical', label: '实操组卷' }]} />
      <Space wrap style={{ marginBottom: 16 }}>
        <Select showSearch optionFilterProp="label" style={{ width: 250 }} placeholder="选择赛事" value={eventId || undefined}
          options={events.map((event: CompetitionEvent) => ({ value: String(event.id), label: `${event.name}（${event.code}）` }))}
          onChange={(value) => { setEventId(value); setWorkTypeId(''); setLevelId(''); }} />
        <Select style={{ width: 220 }} placeholder="选择工种" value={workTypeId || undefined}
          options={workTypes.map((item: CompetitionEventWorkType) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) }))}
          onChange={(value) => { setWorkTypeId(value); setLevelId(''); }} />
        <Select style={{ width: 180 }} placeholder="选择等级" value={levelId || undefined}
          options={levels.map((item: CompetitionEventWorkTypeLevel) => ({ value: String(item.id), label: `${item.levelName}（${item.levelCode}）` }))}
          onChange={setLevelId} />
      </Space>
      <Table<CompetitionMakeExamPlan> rowKey="id" loading={plansQuery.isLoading || plansQuery.isFetching} dataSource={rows} pagination={{ pageSize: 20, showSizeChanger: false }} scroll={{ x: 'max-content' }}
        columns={[
          { title: '计划标题', dataIndex: 'title', ellipsis: true },
          { title: '类型', dataIndex: 'contentType', width: 90, render: (value: string) => value === 'theory' ? '理论' : '实操' },
          { title: '时长', dataIndex: 'durationMinutes', width: 90, render: (value: number) => `${value} 分钟` },
          { title: '总分', dataIndex: 'totalScore', width: 80 },
          { title: '及格分', dataIndex: 'passScore', width: 90 },
          { title: '快照', dataIndex: 'paperCount', width: 90, render: (value: number) => `${value} 套` },
          { title: '状态', dataIndex: 'status', width: 100, render: (value: string) => <Tag color={value === 'published' ? 'green' : 'gold'}>{value === 'published' ? '已发布' : value === 'draft' ? '草稿' : value}</Tag> },
          { title: '操作', key: 'actions', width: 250, fixed: 'right' as const, render: (_: unknown, record: CompetitionMakeExamPlan) => (
            <Space wrap size={[8, 0]} style={{ maxWidth: '100%' }}>
              <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => { setPreviewPlanId(record.id); setPreviewSnapshotId(null); }}>快照预览</Button>
              {record.status === 'draft' && <Button type="link" size="small" icon={<EditOutlined />} onClick={() => void openEdit(record)}>编辑</Button>}
              {record.status === 'draft' && <Popconfirm title="发布后快照将允许赛段使用，确认发布？" onConfirm={() => publishMutation.mutate(record.id)}><Button type="link" size="small" icon={<SendOutlined />}>发布</Button></Popconfirm>}
              {record.status === 'draft' && <Popconfirm title="确认删除此组卷计划？" onConfirm={() => deleteMutation.mutate(record.id)}><Button type="link" danger size="small" icon={<DeleteOutlined />}>删除</Button></Popconfirm>}
            </Space>
          ) },
        ]}
      />

      <Modal title={editing ? '编辑竞赛组卷计划' : '新建竞赛组卷计划'} open={formOpen} onCancel={() => setFormOpen(false)} onOk={() => form.submit()} confirmLoading={saveMutation.isPending} destroyOnClose width={860}>
        <Form form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
          <Form.Item name="eventWorkTypeId" hidden><Input /></Form.Item>
          <Form.Item name="eventWorkTypeLevelId" hidden><Input /></Form.Item>
          <Form.Item name="randomSeed" hidden><Input /></Form.Item>
          <Form.Item name="blueprintId" label="细目表版本" rules={[{ required: contentType === 'theory' || Boolean(editing?.blueprintId), message: '请选择已启用的细目表版本' }]}>
            <Select
              disabled={Boolean(editing) || (!activeBlueprint && !editing)}
              options={(() => {
                const blueprint = selectedBlueprintId && blueprintTreeQuery.data?.blueprint?.id === selectedBlueprintId
                  ? blueprintTreeQuery.data.blueprint
                  : activeBlueprint;
                return blueprint ? [{ value: blueprint.id, label: `v${blueprint.versionNo} ${blueprint.title}` }] : [];
              })()}
              placeholder={editing?.blueprintId ? '沿用原细目表版本' : '请先启用理论/实操细目表'} />
          </Form.Item>
          {contentType === 'practical' && selectedBlueprintId && <>
            <Space style={{ marginTop: 4, marginBottom: 8 }}>
              <Typography.Text strong>实操自动组卷结构预览</Typography.Text>
              <Button
                loading={precheckMutation.isPending}
                onClick={() => form.validateFields().then(values => precheckMutation.mutate(values)).catch(() => undefined)}
              >预检并查看结构</Button>
            </Space>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
              系统按层级关系生成题位：下级必考项分别成题；“一级必考、二级任选一项”则一级只成1题，并在该一级的二级候选中任选一个范围。最终题位使用对应必考范围的整卷比重，任选候选的权重不会相加；试题量仅作为题库储备提示。
            </Typography.Paragraph>
            {practicalPrecheck && <>
              {practicalPrecheck.errors.length > 0 && <Alert type="error" showIcon style={{ marginBottom: 8 }} message={practicalPrecheck.errors.join('；')} />}
              {practicalPrecheck.warnings.length > 0 && <Alert type="warning" showIcon style={{ marginBottom: 8 }} message={practicalPrecheck.warnings.join('；')} />}
              <Table
                size="small"
                rowKey="selectionGroup"
                pagination={false}
                dataSource={practicalPrecheck.practicalAllocations || []}
                expandable={{
                  expandedRowRender: (row) => <Table
                    size="small"
                    rowKey="blueprintNodeId"
                    pagination={false}
                    dataSource={row.candidateNodes}
                    columns={[
                      { title: '候选结构', dataIndex: 'path', ellipsis: true },
                      { title: '候选权重', dataIndex: 'scoreWeight', width: 90, render: (value: number | string) => `${(Number(value) * 100).toFixed(2)}%` },
                      { title: '末级考评点', dataIndex: 'assessmentPointIds', width: 110, render: (value: string[]) => value?.length || 0 },
                      { title: '可用完整实操题', dataIndex: 'candidateQuestionCount', width: 120 },
                      { title: '状态', dataIndex: 'selected', width: 70, render: (value: boolean) => value ? <Tag color="green">已选</Tag> : '-' },
                    ]}
                  />,
                }}
                columns={[
                  { title: '选择组', dataIndex: 'selectionGroup', width: 150 },
                  { title: '模式', dataIndex: 'selectionMode', width: 100, render: (value: string) => value === 'REQUIRED' ? '必考' : '任选一项' },
                  { title: '最终结构', dataIndex: 'path', ellipsis: true },
                  { title: '整组权重', dataIndex: 'scoreWeight', width: 90, render: (value: number | string) => `${(Number(value) * 100).toFixed(2)}%` },
                  { title: '目标分值', dataIndex: 'targetScore', width: 90 },
                  { title: '可用题数', dataIndex: 'candidateQuestionCount', width: 90 },
                  { title: '参考时长', dataIndex: 'referenceDurationMinutes', width: 100, render: (value: number | null) => value == null ? '-' : `${value} 分钟` },
                  { title: '已选题目', dataIndex: 'selectedQuestionId', width: 110, render: (value: string | null) => value || '待生成' },
                ]}
              />
              <Typography.Text type="secondary">最终题量：{practicalPrecheck.practicalQuestionCount ?? practicalPrecheck.practicalAllocations?.length ?? 0} 题；所选题目参考时长合计：{practicalPrecheck.practicalReferenceDurationMinutes ?? 0} 分钟。</Typography.Text>
            </>}
          </>}
          <Space wrap align="start">
            <Form.Item name="title" label="计划标题" rules={[{ required: true }]}><Input style={{ width: 300 }} /></Form.Item>
            <Form.Item name="examType" label="考试类型"><Input style={{ width: 180 }} placeholder="例如：初赛理论" /></Form.Item>
            {contentType === 'practical' && !activeBlueprint && <Form.Item name="practicalQuestionId" label="实操题" rules={[{ required: true, message: '请选择实操题' }]}>
              <Select
                showSearch optionFilterProp="label" style={{ width: 360 }} loading={practicalQuestionsQuery.isLoading}
                placeholder="选择当前赛事/工种/等级下的启用实操题"
                options={practicalQuestions.map((question: CompetitionQuestion) => ({ value: question.id, label: `${question.sortOrder}. ${question.stem}` }))}
                onChange={(value) => {
                  const selected = practicalQuestions.find((question) => question.id === value);
                  if (selected?.practicalDetail) {
                    form.setFieldsValue({
                      durationMinutes: selected.practicalDetail.durationMinutes || 60,
                      totalScore: Number(selected.practicalDetail.totalScore || 100),
                    });
                  }
                }}
              />
            </Form.Item>}
            {contentType === 'practical' && <Form.Item name="paperCount" label="生成套数" rules={[{ required: true }]}><InputNumber min={1} max={50} precision={0} /></Form.Item>}
            <Form.Item name="durationMinutes" label="考试时长（分钟）" rules={[{ required: true, type: 'number', min: 1 }]} extra={contentType === 'practical' ? '计划时长独立配置；题目参考时长合计超出时仅提示' : undefined}><InputNumber min={1} precision={0} /></Form.Item>
            <Form.Item name="totalScore" label="计划总分（每套卷）" rules={[{ required: true, type: 'number', min: 0.01 }]} extra={contentType === 'practical' ? '系统按最终必考题位或其任选组对应的整卷比重精确分配分值' : '每套卷实际题目分值合计必须等于此值'}><InputNumber min={0.01} precision={2} /></Form.Item>
            <Form.Item name="passScore" label="及格分" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
          </Space>
          {contentType === 'theory' && <><Typography.Text strong>组卷规则（题型、题量和单题分值）</Typography.Text>
           <Form.List name="details">
            {(fields, { add, remove }) => <>
              {fields.map((field, index) => <Space key={field.key} align="start" style={{ display: 'flex', marginTop: 12 }}>
                <Form.Item {...field} name={[field.name, 'sortOrder']} label={index === 0 ? '序号' : undefined} rules={[{ required: true }]}><InputNumber min={1} /></Form.Item>
                <Form.Item {...field} name={[field.name, 'questionType']} label={index === 0 ? '题型' : undefined} rules={[{ required: true }]}><Select style={{ width: 130 }} options={questionTypeOptions} /></Form.Item>
                <Form.Item {...field} name={[field.name, 'questionCount']} label={index === 0 ? '题量' : undefined} rules={[{ required: true }]}><InputNumber min={1} /></Form.Item>
                <Form.Item {...field} name={[field.name, 'point']} label={index === 0 ? '单题分值' : undefined} rules={[{ required: true }]}><InputNumber min={0.01} /></Form.Item>
                <Form.Item {...field} name={[field.name, 'difficultyStart']} label={index === 0 ? '难度起' : undefined}><InputNumber min={1} max={5} placeholder="不限" /></Form.Item>
                <Form.Item {...field} name={[field.name, 'difficultyEnd']} label={index === 0 ? '难度止' : undefined}><InputNumber min={1} max={5} placeholder="不限" /></Form.Item>
                <Button danger type="link" style={{ marginTop: index === 0 ? 30 : 0 }} onClick={() => remove(field.name)}>删除</Button>
              </Space>)}
              {contentType === 'theory' && <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({ questionType: 'single', questionCount: 1, point: 1, sortOrder: fields.length + 1 })}>增加题型</Button>}
            </>}
           </Form.List>
           {selectedBlueprintId && <>
             <Space style={{ marginTop: 16, marginBottom: 8 }}>
               <Typography.Text strong>理论抽题分布（根据细目表三级权重自动计算）</Typography.Text>
               <Button
                 loading={precheckMutation.isPending}
                 onClick={() => form.validateFields().then(values => precheckMutation.mutate(values)).catch(() => undefined)}
               >预检并查看自动分配</Button>
             </Space>
             <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
               您只需配置题型、题量、单题分值和难度。系统将按照细目表三级权重自动分配题目，理论分布不可手工修改。
             </Typography.Paragraph>
             {theoryPrecheck && <>
               {theoryPrecheck.errors.length > 0 && <Alert type="error" showIcon style={{ marginBottom: 8 }} message={theoryPrecheck.errors.join('；')} />}
               {theoryPrecheck.warnings.length > 0 && <Alert type="warning" showIcon style={{ marginBottom: 8 }} message={theoryPrecheck.warnings.join('；')} />}
               <Table
                 size="small"
                 rowKey="blueprintNodeId"
                 pagination={false}
                 dataSource={theoryPrecheck.theoryAllocations}
                 expandable={{
                   expandedRowRender: (row) => <Table
                     size="small"
                      rowKey="ruleIndex"
                     pagination={false}
                     dataSource={row.details}
                     columns={[
                       { title: '规则', dataIndex: 'sortOrder', width: 70 },
                       { title: '题型', dataIndex: 'questionType', width: 90, render: (value: string) => typeLabels[value] || value },
                        { title: '难度', render: (_: unknown, cell: { difficultyStart?: number | null; difficultyEnd?: number | null }) => cell.difficultyStart == null && cell.difficultyEnd == null ? '不限' : `${cell.difficultyStart ?? ''}-${cell.difficultyEnd ?? ''}` },
                       { title: '自动题量', dataIndex: 'required', width: 90 },
                       { title: '可用题量', dataIndex: 'available', width: 90 },
                       { title: '缺口', dataIndex: 'shortage', width: 70, render: (value: number) => value > 0 ? <Typography.Text type="danger">{value}</Typography.Text> : value },
                     ]}
                   />,
                 }}
                 columns={[
                   { title: '三级考评范围', dataIndex: 'path', ellipsis: true },
                   { title: '权重', dataIndex: 'weight', width: 80, render: (value: number | string) => `${(Number(value) * 100).toFixed(2)}%` },
                   { title: '自动题量', dataIndex: 'questionCount', width: 90 },
                   { title: '可用题量', dataIndex: 'available', width: 90 },
                   { title: '缺口', dataIndex: 'shortage', width: 70, render: (value: number) => value > 0 ? <Typography.Text type="danger">{value}</Typography.Text> : value },
                 ]}
               />
             </>}
           </>}
           </>}
          <Form.Item name="comments" label="备注" style={{ marginTop: 16 }}><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="组卷快照预览" open={Boolean(previewPlanId)} onCancel={() => { setPreviewPlanId(null); setPreviewSnapshotId(null); }} footer={null} width={1000} styles={{ body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto', padding: 0 } }}>
        {snapshotsQuery.isLoading ? <Spin /> : snapshotsQuery.data ? <>
          <Space wrap style={{ padding: '16px 20px 0', marginBottom: 0 }}>{snapshotsQuery.data.map(snapshot => <Button key={snapshot.id} type={snapshot.id === previewSnapshotId ? 'primary' : 'default'} onClick={() => setPreviewSnapshotId(snapshot.id)}>{snapshot.paperCode} 卷 / v{snapshot.versionNo} <Tag>{snapshot.status}</Tag></Button>)}</Space>
          {previewQuery.isLoading ? <Spin /> : previewQuery.data ? <>
            {previewQuery.data.contentType === 'practical' ? <CompetitionPracticalSnapshotPaperPreview snapshot={previewQuery.data} /> : <>
              <Descriptions column={4} size="small" style={{ marginBottom: 16 }}>
                <Descriptions.Item label="试卷">{previewQuery.data.paperCode} 卷</Descriptions.Item>
                <Descriptions.Item label="题量">{previewQuery.data.questionCount}</Descriptions.Item>
                <Descriptions.Item label="总分">{previewQuery.data.totalScore}</Descriptions.Item>
                <Descriptions.Item label="时长">{previewQuery.data.durationMinutes} 分钟</Descriptions.Item>
              </Descriptions>
              <Table size="small" rowKey="id" pagination={{ pageSize: 20 }} dataSource={previewQuery.data.questions} columns={[
                { title: '题号', dataIndex: 'sortOrder', width: 70 },
                { title: '题型', dataIndex: 'questionType', width: 90, render: (value: string) => typeLabels[value] || value },
                { title: '题干/操作内容', dataIndex: 'stem', ellipsis: true, render: (value: string, row) => <Typography.Text ellipsis={{ tooltip: true }}>{value || row.operationContent}</Typography.Text> },
                { title: '分值', dataIndex: 'score', width: 80 },
                { title: '答案（管理预览）', dataIndex: 'referenceAnswer', width: 180, render: (value: string, row) => value || row.fillAnswer || '-' },
              ]} />
            </>}
          </> : <Alert type="warning" message="无法加载快照" />}
        </> : <Alert type="warning" message="暂无快照" />}
      </Modal>
    </Card>
  );
}

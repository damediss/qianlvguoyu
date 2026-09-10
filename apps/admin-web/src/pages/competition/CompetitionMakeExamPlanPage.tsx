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
} from '../../api/competitionQuestion';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents } from '../../api/competition';
import type { CompetitionEvent, CompetitionEventWorkType, CompetitionEventWorkTypeLevel } from '../../types/competition';
import type { CompetitionMakeExamPlan, CompetitionMakeExamPlanRequest, CompetitionQuestionContentType, CompetitionQuestionType } from '../../types/competitionQuestion';

interface PlanFormValues {
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  title: string;
  examType?: string;
  durationMinutes: number;
  totalScore: number;
  passScore: number;
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
  const [form] = Form.useForm<PlanFormValues>();

  const eventsQuery = useQuery({ queryKey: ['competition-events', 'make-plan'], queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }) });
  const events = eventsQuery.data?.items ?? [];
  const workTypesQuery = useQuery({ queryKey: ['competition-event-work-types', 'make-plan', eventId], queryFn: () => listCompetitionEventWorkTypes(eventId), enabled: Boolean(eventId) });
  const workTypes = workTypesQuery.data ?? [];
  const levelsQuery = useQuery({ queryKey: ['competition-event-levels', 'make-plan', eventId, workTypeId], queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId), enabled: Boolean(eventId && workTypeId) });
  const levels = levelsQuery.data ?? [];
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

  const saveMutation = useMutation({
    mutationFn: (values: PlanFormValues) => {
      const body: CompetitionMakeExamPlanRequest = { ...values, contentType, details: values.details };
      return editing ? updateCompetitionMakeExamPlan(eventId, editing.id, body) : createCompetitionMakeExamPlan(eventId, body);
    },
    onSuccess: () => {
      message.success(editing ? '组卷计划已更新并生成新草稿快照' : '组卷计划已保存并生成草稿快照');
      setFormOpen(false);
      setEditing(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-make-plans'] });
    },
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
    form.resetFields();
    form.setFieldsValue({
      eventWorkTypeId: workTypeId,
      eventWorkTypeLevelId: levelId,
      durationMinutes: 90,
      totalScore: contentType === 'theory' ? 100 : 100,
      passScore: 60,
      details: contentType === 'theory'
        ? [{ questionType: 'single', questionCount: 10, point: 10, sortOrder: 1 }]
        : [{ questionType: 'practical', questionCount: 1, point: 100, sortOrder: 1 }],
    });
    setFormOpen(true);
  };
  const openEdit = async (record: CompetitionMakeExamPlan) => {
    const detail = await getCompetitionMakeExamPlan(eventId, record.id);
    setEditing(detail);
    form.setFieldsValue({
      eventWorkTypeId: detail.eventWorkTypeId,
      eventWorkTypeLevelId: detail.eventWorkTypeLevelId,
      title: detail.title,
      examType: detail.examType || undefined,
      durationMinutes: detail.durationMinutes,
      totalScore: Number(detail.totalScore),
      passScore: Number(detail.passScore),
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
      <Alert type="info" showIcon style={{ marginBottom: 16 }} message="每个组卷计划只生成一套独立试卷；按组卷规则的题型、题量和单题分值抽题并生成快照，规则合计必须等于计划总分。需要 A/B 或更多套试卷时，请分别创建多个计划，发布后在赛段编排中选择。" />
      <Tabs activeKey={contentType} onChange={(value) => { setContentType(value as CompetitionQuestionContentType); setEditing(null); }} items={[{ key: 'theory', label: '理论组卷' }, { key: 'practical', label: '实操组卷' }]} />
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
          <Space wrap align="start">
            <Form.Item name="title" label="计划标题" rules={[{ required: true }]}><Input style={{ width: 300 }} /></Form.Item>
            <Form.Item name="examType" label="考试类型"><Input style={{ width: 180 }} placeholder="例如：初赛理论" /></Form.Item>
            <Form.Item name="durationMinutes" label="考试时长（分钟）" rules={[{ required: true }]}><InputNumber min={1} /></Form.Item>
            <Form.Item name="totalScore" label="计划总分（每套卷）" rules={[{ required: true }]} extra="每套卷实际题目分值合计必须等于此值"><InputNumber min={0.01} /></Form.Item>
            <Form.Item name="passScore" label="及格分" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
          </Space>
          <Typography.Text strong>组卷规则（题型、题量和单题分值）</Typography.Text>
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
          <Form.Item name="comments" label="备注" style={{ marginTop: 16 }}><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="组卷快照预览" open={Boolean(previewPlanId)} onCancel={() => { setPreviewPlanId(null); setPreviewSnapshotId(null); }} footer={null} width={900}>
        {snapshotsQuery.isLoading ? <Spin /> : snapshotsQuery.data ? <>
          <Space wrap style={{ marginBottom: 12 }}>{snapshotsQuery.data.map(snapshot => <Button key={snapshot.id} type={snapshot.id === previewSnapshotId ? 'primary' : 'default'} onClick={() => setPreviewSnapshotId(snapshot.id)}>{snapshot.paperCode} 卷 / v{snapshot.versionNo} <Tag>{snapshot.status}</Tag></Button>)}</Space>
          {previewQuery.isLoading ? <Spin /> : previewQuery.data ? <>
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
          </> : <Alert type="warning" message="无法加载快照" />}
        </> : <Alert type="warning" message="暂无快照" />}
      </Modal>
    </Card>
  );
}

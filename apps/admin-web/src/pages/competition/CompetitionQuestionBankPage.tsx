import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { UploadProps } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import {
  createCompetitionQuestion,
  deleteCompetitionQuestion,
  downloadCompetitionQuestionTemplate,
  getCompetitionQuestion,
  getCompetitionQuestionImportBatch,
  importCompetitionQuestions,
  pageCompetitionQuestions,
  updateCompetitionQuestion,
} from '../../api/competitionQuestion';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
} from '../../api/competition';
import type { CompetitionEvent, CompetitionEventWorkType, CompetitionEventWorkTypeLevel } from '../../types/competition';
import type {
  CompetitionQuestion,
  CompetitionQuestionContentType,
  CompetitionQuestionRequest,
  CompetitionQuestionType,
} from '../../types/competitionQuestion';

interface Props {
  contentType: CompetitionQuestionContentType;
}

const OPTION_KEYS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'] as const;
type OptionKey = typeof OPTION_KEYS[number];
type QuestionOptions = Partial<Record<OptionKey, string>>;

interface QuestionFormValues {
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  questionType: CompetitionQuestionType;
  stem: string;
  referenceAnswer?: string;
  fillAnswer?: string;
  options?: QuestionOptions;
  difficulty?: number;
  status?: string;
  operationContent?: string;
  studentPrepare?: string;
  equipmentPrepare?: string;
  scoreItemsJson?: string;
}

const typeLabels: Record<string, string> = {
  single: '单选题',
  multi: '多选题',
  judge: '判断题',
  fill: '填空题',
  practical: '实操题',
};

const typeColors: Record<string, string> = {
  single: 'blue',
  multi: 'purple',
  judge: 'orange',
  fill: 'cyan',
  practical: 'green',
};

const judgeAnswerOptions = [
  { value: 'T', label: 'T（正确）' },
  { value: 'F', label: 'F（错误）' },
];

function questionTypeTag(value?: string | null) {
  const type = value?.trim().toLowerCase() || '';
  return <Tag color={typeColors[type] || 'default'}>{typeLabels[type] || value || '-'}</Tag>;
}

function parseOptionsJson(optionsJson?: string | null): QuestionOptions {
  if (!optionsJson) return {};
  try {
    const parsed: unknown = JSON.parse(optionsJson);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {};
    const source = parsed as Record<string, unknown>;
    return OPTION_KEYS.reduce<QuestionOptions>((options, key) => {
      if (typeof source[key] === 'string') options[key] = source[key] as string;
      return options;
    }, {});
  } catch {
    return {};
  }
}

function isChoiceQuestion(questionType?: CompetitionQuestionType) {
  return questionType === 'single' || questionType === 'multi';
}

function serializeOptions(questionType: CompetitionQuestionType, options?: QuestionOptions) {
  if (!isChoiceQuestion(questionType)) return undefined;
  const entries = OPTION_KEYS
    .map((key) => [key, options?.[key]?.trim() || ''] as const)
    .filter(([, value]) => value.length > 0);
  return entries.length > 0 ? JSON.stringify(Object.fromEntries(entries)) : undefined;
}

function getEventIdFromUrl() {
  return new URLSearchParams(window.location.search).get('eventId') || '';
}

export function CompetitionQuestionBankPage({ contentType }: Props) {
  const queryClient = useQueryClient();
  const [eventId, setEventId] = useState(getEventIdFromUrl);
  const [workTypeId, setWorkTypeId] = useState('');
  const [levelId, setLevelId] = useState('');
  const [keyword, setKeyword] = useState('');
  const [questionType, setQuestionType] = useState<CompetitionQuestionType | undefined>();
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState<CompetitionQuestion | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [previewing, setPreviewing] = useState<string | null>(null);
  const [batchId, setBatchId] = useState<string | null>(null);
  const [form] = Form.useForm<QuestionFormValues>();
  const selectedQuestionType = Form.useWatch('questionType', form);
  const effectiveQuestionType = selectedQuestionType || (contentType === 'theory' ? 'single' : 'practical');

  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'question-bank'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }),
  });
  const events = eventsQuery.data?.items ?? [];
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: Boolean(eventId),
  });
  const workTypes = workTypesQuery.data ?? [];
  const levelsQuery = useQuery({
    queryKey: ['competition-event-levels', eventId, workTypeId],
    queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId),
    enabled: Boolean(eventId && workTypeId),
  });
  const levels = levelsQuery.data ?? [];
  const questionsQuery = useQuery({
    queryKey: ['competition-questions', eventId, workTypeId, levelId, contentType, questionType, keyword, page],
    queryFn: () => pageCompetitionQuestions(eventId, {
      page,
      pageSize: 20,
      eventWorkTypeId: workTypeId,
      eventWorkTypeLevelId: levelId,
      contentType,
      questionType,
      keyword: keyword || undefined,
    }),
    enabled: Boolean(eventId && workTypeId && levelId),
  });
  const previewQuery = useQuery({
    queryKey: ['competition-question', eventId, previewing],
    queryFn: () => getCompetitionQuestion(eventId, previewing!),
    enabled: Boolean(eventId && previewing),
  });
  const batchQuery = useQuery({
    queryKey: ['competition-question-import-batch', eventId, batchId],
    queryFn: () => getCompetitionQuestionImportBatch(eventId, batchId!),
    enabled: Boolean(eventId && batchId),
    refetchInterval: batchId ? 1500 : false,
  });

  useEffect(() => {
    if (!eventId && events[0]) setEventId(String(events[0].id));
  }, [eventId, events]);

  useEffect(() => {
    if (!workTypeId && workTypes[0]) setWorkTypeId(String(workTypes[0].id));
  }, [workTypeId, workTypes]);

  useEffect(() => {
    if (!levelId && levels[0]) setLevelId(String(levels[0].id));
  }, [levelId, levels]);

  useEffect(() => {
    if (batchQuery.data?.status === 'succeeded') {
      queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
    }
  }, [batchQuery.data?.status, queryClient]);

  const saveMutation = useMutation({
    mutationFn: async (values: QuestionFormValues) => {
      const questionType = values.questionType;
      const referenceAnswer = isChoiceQuestion(questionType) || questionType === 'judge'
        ? values.referenceAnswer?.trim().toUpperCase()
        : undefined;
      const body: CompetitionQuestionRequest = {
        eventWorkTypeId: values.eventWorkTypeId,
        eventWorkTypeLevelId: values.eventWorkTypeLevelId,
        contentType,
        questionType,
        stem: values.stem,
        referenceAnswer,
        fillAnswer: questionType === 'fill' ? values.fillAnswer?.trim() : undefined,
        optionsJson: serializeOptions(questionType, values.options),
        difficulty: values.difficulty,
        status: values.status,
      };
      if (contentType === 'practical') {
        body.practicalDetail = {
          operationContent: values.operationContent,
          studentPrepare: values.studentPrepare,
          equipmentPrepare: values.equipmentPrepare,
        };
        try {
          body.scoreItems = values.scoreItemsJson ? JSON.parse(values.scoreItemsJson) : [];
        } catch {
          throw new Error('评分项 JSON 格式不正确');
        }
      }
      return editing
        ? updateCompetitionQuestion(eventId, editing.id, body)
        : createCompetitionQuestion(eventId, body);
    },
    onSuccess: () => {
      message.success(editing ? '竞赛试题已更新' : '竞赛试题已创建');
      setEditing(null);
      setFormOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCompetitionQuestion(eventId, id),
    onSuccess: () => {
      message.success('竞赛试题已删除');
      queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
    },
  });
  const importMutation = useMutation({
    mutationFn: (file: File) => importCompetitionQuestions(eventId, {
      eventWorkTypeId: workTypeId,
      eventWorkTypeLevelId: levelId,
      contentType,
    }, file),
    onSuccess: (batch) => {
      setBatchId(batch.id);
      message.success('文件已接收，正在后台校验');
    },
  });

  const openEdit = (record: CompetitionQuestion) => {
    setEditing(record);
    setFormOpen(true);
    form.setFieldsValue({
      eventWorkTypeId: record.eventWorkTypeId,
      eventWorkTypeLevelId: record.eventWorkTypeLevelId,
      questionType: record.questionType,
      stem: record.stem,
      referenceAnswer: record.questionType === 'judge'
        ? record.referenceAnswer?.trim().toUpperCase() || undefined
        : record.referenceAnswer || undefined,
      fillAnswer: record.fillAnswer || undefined,
      options: parseOptionsJson(record.optionsJson),
      difficulty: record.difficulty || undefined,
      status: record.status,
      operationContent: record.operationContent || undefined,
      studentPrepare: record.studentPrepare || undefined,
      equipmentPrepare: record.equipmentPrepare || undefined,
      scoreItemsJson: record.scoreItems?.length ? JSON.stringify(record.scoreItems.map(item => ({
        itemNo: item.itemNo,
        itemName: item.itemName,
        criterion: item.criterion,
        maxScore: item.maxScore,
        maxDeduction: item.maxDeduction,
        deductionStep: item.deductionStep,
      })), null, 2) : undefined,
    });
  };

  const handleQuestionTypeChange = (value: CompetitionQuestionType) => {
    form.setFieldsValue({
      questionType: value,
      referenceAnswer: undefined,
      fillAnswer: undefined,
      options: undefined,
    });
  };

  const uploadProps: UploadProps = {
    accept: '.xlsx,.xls',
    showUploadList: false,
    beforeUpload: (file) => {
      importMutation.mutate(file);
      return Upload.LIST_IGNORE;
    },
  };
  const typeOptions = useMemo(() => contentType === 'theory'
    ? ['single', 'multi', 'judge', 'fill'].map(value => ({ value, label: typeLabels[value] }))
    : [{ value: 'practical', label: '实操题' }], [contentType]);
  const data = questionsQuery.data;

  const downloadTemplate = async () => {
    const blob = await downloadCompetitionQuestionTemplate(contentType);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = contentType === 'theory' ? '竞赛理论试题模板.xlsx' : '竞赛实操试题模板.xlsx';
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const answerRules = (type: CompetitionQuestionType) => {
    if (type === 'judge') {
      return [
        { required: true, message: '请输入参考答案' },
        { pattern: /^[TF]$/i, message: '判断题参考答案只能是 T 或 F' },
      ];
    }
    if (type === 'single' || type === 'multi') {
      return [
        { required: true, message: '请输入参考答案' },
        {
          validator: (_rule: unknown, value: unknown) => {
            const letters = String(value ?? '').toUpperCase().replace(/[^A-H]/g, '');
            const options = form.getFieldValue('options') as QuestionOptions | undefined;
            const selected = letters.split('');
            if (type === 'single' && letters.length !== 1) {
              return Promise.reject(new Error('单选题参考答案只能填写一个 A-H 选项'));
            }
            if (type === 'multi' && letters.length < 2) {
              return Promise.reject(new Error('多选题参考答案至少填写两个 A-H 选项'));
            }
            if (new Set(selected).size !== selected.length) {
              return Promise.reject(new Error('参考答案不能重复选择同一选项'));
            }
            if (selected.some((key) => !options?.[key as OptionKey]?.trim())) {
              return Promise.reject(new Error('参考答案对应的选项必须填写内容'));
            }
            return Promise.resolve();
          },
        },
      ];
    }
    return [{ required: true, message: '请输入回答' }];
  };

  const renderTheoryAnswerFields = () => {
    if (isChoiceQuestion(effectiveQuestionType)) {
      return (
        <>
          <Typography.Text strong>选项</Typography.Text>
          <Row gutter={[12, 0]} style={{ marginTop: 8 }}>
            {OPTION_KEYS.map((key) => (
              <Col span={12} key={key}>
                <Form.Item name={['options', key]} label={`选项 ${key}`}>
                  <Input placeholder={`请输入选项 ${key}`} />
                </Form.Item>
              </Col>
            ))}
          </Row>
          <Form.Item
            name="referenceAnswer"
            label="参考答案"
            dependencies={['options']}
            rules={answerRules(effectiveQuestionType)}
            extra={effectiveQuestionType === 'single' ? '请输入一个选项字母，例如 A' : '请输入两个或以上选项字母，例如 AC'}
          >
            <Input placeholder={effectiveQuestionType === 'single' ? '例如：A' : '例如：AC'} />
          </Form.Item>
        </>
      );
    }
    if (effectiveQuestionType === 'judge') {
      return (
        <Form.Item
          name="referenceAnswer"
          label="答案选项"
          rules={answerRules(effectiveQuestionType)}
          extra="请选择 T（正确）或 F（错误）"
        >
          <Select placeholder="请选择 T 或 F" options={judgeAnswerOptions} />
        </Form.Item>
      );
    }
    return (
      <Form.Item name="fillAnswer" label="回答" rules={answerRules('fill')}>
        <Input placeholder="请输入填空题回答" />
      </Form.Item>
    );
  };

  return (
    <Card
      title={`竞赛${contentType === 'theory' ? '理论' : '实操'}试题`}
      extra={(
        <Space wrap>
          <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载导入模板</Button>
          <Upload {...uploadProps} disabled={!eventId || !workTypeId || !levelId || importMutation.isPending}>
            <Button type="primary" icon={<UploadOutlined />}>导入试题</Button>
          </Upload>
          <Button icon={<PlusOutlined />} onClick={() => { setEditing(null); form.resetFields(); form.setFieldsValue({
            eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId,
            questionType: contentType === 'theory' ? 'single' : 'practical', status: 'enabled', difficulty: 2,
          }); setFormOpen(true); }}>新增试题</Button>
        </Space>
      )}
    >
      <Alert type="info" showIcon style={{ marginBottom: 16 }} message="竞赛题库与课程题库完全独立；题目只维护内容、答案和实操评分项，单题分值在组卷计划中统一设置；导入按赛事、工种、等级和题型序号整批校验，发现重复或格式错误时不会写入任何题目。" />
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          showSearch optionFilterProp="label" style={{ width: 240 }} placeholder="选择赛事"
          value={eventId || undefined}
          options={events.map((event: CompetitionEvent) => ({ value: String(event.id), label: `${event.name}（${event.code}）` }))}
          onChange={(value) => { setEventId(value); setWorkTypeId(''); setLevelId(''); setPage(1); }}
        />
        <Select
          style={{ width: 210 }} placeholder="选择工种" value={workTypeId || undefined}
          options={workTypes.map((item: CompetitionEventWorkType) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) }))}
          onChange={(value) => { setWorkTypeId(value); setLevelId(''); setPage(1); }}
        />
        <Select
          style={{ width: 180 }} placeholder="选择等级" value={levelId || undefined}
          options={levels.map((item: CompetitionEventWorkTypeLevel) => ({ value: String(item.id), label: `${item.levelName}（${item.levelCode}）` }))}
          onChange={(value) => { setLevelId(value); setPage(1); }}
        />
        <Select allowClear style={{ width: 140 }} placeholder="题型" value={questionType} options={typeOptions}
          onChange={(value) => { setQuestionType(value); setPage(1); }} />
        <Input.Search allowClear placeholder="搜索题干" style={{ width: 240 }} value={keyword}
          onChange={(event) => setKeyword(event.target.value)} onSearch={() => setPage(1)} />
      </Space>
      <Table<CompetitionQuestion>
        rowKey="id" loading={questionsQuery.isLoading || questionsQuery.isFetching}
        dataSource={data?.items ?? []}
        pagination={{ current: data?.page ?? page, pageSize: data?.pageSize ?? 20, total: data?.total ?? 0, onChange: setPage, showSizeChanger: false }}
        columns={[
          { title: '序号', dataIndex: 'sortOrder', width: 80 },
          { title: '题型', dataIndex: 'questionType', width: 100, render: questionTypeTag },
          { title: '题干', dataIndex: 'stem', ellipsis: true, render: (value: string) => <Typography.Text ellipsis={{ tooltip: true }}>{value}</Typography.Text> },
          { title: '难度', dataIndex: 'difficulty', width: 90, render: (value: number | null) => value ? ['-', '简单', '中等', '困难', '较难', '极难'][value] || value : '-' },
          { title: '状态', dataIndex: 'status', width: 90, render: (value: string) => value === 'enabled' ? <Tag color="green">启用</Tag> : <Tag>停用</Tag> },
          { title: '操作', key: 'actions', width: 170, render: (_: unknown, record: CompetitionQuestion) => (
            <Space size={0} style={{ whiteSpace: 'nowrap' }}>
              <Button type="link" size="small" style={{ paddingInline: 0 }} icon={<EyeOutlined />} onClick={() => setPreviewing(record.id)}>预览</Button>
              <Button type="link" size="small" style={{ paddingInline: 0 }} icon={<EditOutlined />} onClick={() => openEdit(record)}>编辑</Button>
              <Popconfirm title="确认删除此竞赛试题？" onConfirm={() => deleteMutation.mutate(record.id)}>
                <Button type="link" danger size="small" style={{ paddingInline: 0 }} icon={<DeleteOutlined />}>删除</Button>
              </Popconfirm>
            </Space>
          ) },
        ]}
      />

      <Modal title={editing ? '编辑竞赛试题' : '新增竞赛试题'} open={formOpen}
        onCancel={() => { setEditing(null); setFormOpen(false); form.resetFields(); }} onOk={() => form.submit()} confirmLoading={saveMutation.isPending} destroyOnClose width={760}>
        <Form form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
          <Space wrap align="start">
            <Form.Item name="eventWorkTypeId" label="工种" rules={[{ required: true }]}><Select style={{ width: 220 }} options={workTypes.map(item => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) }))} /></Form.Item>
            <Form.Item name="eventWorkTypeLevelId" label="等级" rules={[{ required: true }]}><Select style={{ width: 170 }} options={levels.map(item => ({ value: String(item.id), label: item.levelName }))} /></Form.Item>
            <Form.Item name="questionType" label="题型" rules={[{ required: true }]}><Select style={{ width: 130 }} options={typeOptions} onChange={(value) => handleQuestionTypeChange(value as CompetitionQuestionType)} /></Form.Item>
            <Form.Item
              label="序号"
              extra={editing ? '按创建顺序生成，编辑时不可修改' : '保存后按创建顺序自动生成'}
            >
              <Input disabled value={editing ? String(editing.sortOrder) : undefined} placeholder="自动生成" />
            </Form.Item>
            <Form.Item name="difficulty" label="难度"><InputNumber min={1} max={5} /></Form.Item>
          </Space>
          <Form.Item name="stem" label="题干" rules={[{ required: true }]}><Input.TextArea rows={4} /></Form.Item>
          {contentType === 'theory' ? renderTheoryAnswerFields() : <>
            <Form.Item name="operationContent" label="操作内容"><Input.TextArea rows={3} /></Form.Item>
            <Space wrap align="start"><Form.Item name="studentPrepare" label="考生准备"><Input.TextArea rows={2} style={{ width: 340 }} /></Form.Item><Form.Item name="equipmentPrepare" label="设备准备"><Input.TextArea rows={2} style={{ width: 340 }} /></Form.Item></Space>
            <Form.Item name="scoreItemsJson" label="评分项 JSON" rules={[{ required: true }]} extra="每项包含 itemNo、itemName、criterion、maxScore、maxDeduction、deductionStep；题目满分在组卷计划中统一设置"><Input.TextArea rows={7} placeholder={'[{"itemNo":1,"itemName":"评分要素","criterion":"评分标准","maxScore":100,"maxDeduction":100,"deductionStep":1}]'} /></Form.Item>
          </>}
          <Form.Item name="status" label="状态"><Select options={[{ value: 'enabled', label: '启用' }, { value: 'disabled', label: '停用' }]} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="竞赛试题预览" open={Boolean(previewing)} onCancel={() => setPreviewing(null)} footer={null} width={720}>
        {previewQuery.isLoading ? <Spin /> : previewQuery.data ? <>
          <Descriptions column={3} size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="题号">{previewQuery.data.sortOrder}</Descriptions.Item>
            <Descriptions.Item label="题型">{questionTypeTag(previewQuery.data.questionType)}</Descriptions.Item>
          </Descriptions>
          <Typography.Paragraph>{previewQuery.data.stem}</Typography.Paragraph>
          {previewQuery.data.optionsJson && <Typography.Paragraph type="secondary">选项：{previewQuery.data.optionsJson}</Typography.Paragraph>}
          {contentType === 'practical' && <>
            <Typography.Paragraph><b>操作内容：</b>{previewQuery.data.operationContent || '-'}</Typography.Paragraph>
            <Typography.Paragraph><b>考生准备：</b>{previewQuery.data.studentPrepare || '-'}</Typography.Paragraph>
            <Typography.Paragraph><b>设备准备：</b>{previewQuery.data.equipmentPrepare || '-'}</Typography.Paragraph>
            <Table size="small" rowKey="id" pagination={false} dataSource={previewQuery.data.scoreItems} columns={[{ title: '序号', dataIndex: 'itemNo' }, { title: '评分要素', dataIndex: 'itemName' }, { title: '评分标准', dataIndex: 'criterion' }, { title: '最大分值', dataIndex: 'maxScore' }, { title: '最大扣分', dataIndex: 'maxDeduction' }, { title: '步长', dataIndex: 'deductionStep' }]} />
          </>}
        </> : <Alert type="warning" message="无法加载题目" />}
      </Modal>

      <Modal title="导入进度" open={Boolean(batchId)} onCancel={() => setBatchId(null)} footer={null}>
        {batchQuery.data ? <>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Typography.Text>文件：{batchQuery.data.originalFilename}</Typography.Text>
            <Typography.Text>状态：{batchQuery.data.status === 'processing' ? '解析中' : batchQuery.data.status === 'succeeded' ? '导入成功' : batchQuery.data.status === 'failed' ? '导入失败' : '排队中'}</Typography.Text>
            <Typography.Text>总数：{batchQuery.data.totalCount}，成功：{batchQuery.data.successCount}，失败：{batchQuery.data.failureCount}</Typography.Text>
            {batchQuery.data.errorDetail && <Alert type="error" showIcon message="错误明细" description={batchQuery.data.errorDetail} />}
          </Space>
        </> : <Spin />}
      </Modal>
    </Card>
  );
}

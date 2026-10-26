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
  batchBindCompetitionQuestions,
  deleteCompetitionQuestion,
  downloadCompetitionQuestionTemplate,
  getCompetitionQuestion,
  getCompetitionQuestionImportBatch,
  importCompetitionQuestions,
  importCompetitionSimplePracticalDetails,
  importCompetitionSimpleTheoryQuestions,
  pageCompetitionQuestions,
  updateCompetitionQuestion,
} from '../../api/competitionQuestion';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
} from '../../api/competition';
import { getCompetitionQuestionBlueprintTree, pageCompetitionQuestionBlueprints } from '../../api/competitionQuestionBlueprint';
import type { CompetitionEvent, CompetitionEventWorkType, CompetitionEventWorkTypeLevel } from '../../types/competition';
import { SimplePracticalQuestionCreateModal } from './SimplePracticalQuestionCreateModal';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { competitionEventLabel } from '../../utils/competitionEventLabel';
import type {
  CompetitionQuestion,
  CompetitionQuestionContentType,
  CompetitionQuestionRequest,
  CompetitionQuestionType,
} from '../../types/competitionQuestion';
import { PracticalQuestionEditorModal } from './PracticalQuestionEditorModal';

interface Props {
  contentType: CompetitionQuestionContentType;
  simpleMode?: boolean;
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
  blueprintId?: string;
  blueprintNodeId?: string;
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

function plainText(value?: string | null) {
  return value?.replace(/<br\s*\/?\s*>/gi, ' ').replace(/<[^>]*>/g, ' ').replace(/&nbsp;/gi, ' ').replace(/\s+/g, ' ').trim() || '-';
}

function practicalScoringMethodLabel(value?: string | null) {
  return value === 'additive' ? '加分制' : value === 'deductive' ? '扣分制' : value || '-';
}

export function CompetitionQuestionBankPage({ contentType, simpleMode = false }: Props) {
  const queryClient = useQueryClient();
  const paneActive = usePaneActive(); // 标签页不可见时暂停导入批次轮询（§3.6）
  const [eventId, setEventId] = useState(getEventIdFromUrl);
  const [workTypeId, setWorkTypeId] = useState('');
  const [levelId, setLevelId] = useState('');
  const [keyword, setKeyword] = useState('');
  const [selectedSheet, setSelectedSheet] = useState('');
  const [questionType, setQuestionType] = useState<CompetitionQuestionType | undefined>();
  const [bindingStatus, setBindingStatus] = useState<string | undefined>();
  const [blueprintNodeId, setBlueprintNodeId] = useState<string | undefined>();
  const [selectedQuestionIds, setSelectedQuestionIds] = useState<string[]>([]);
  const [bindingOpen, setBindingOpen] = useState(false);
  const [bindingTargetNodeId, setBindingTargetNodeId] = useState<string>();
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState<CompetitionQuestion | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [simplePracticalCreateOpen, setSimplePracticalCreateOpen] = useState(false);
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
  const blueprintQuery = useQuery({
    queryKey: ['competition-active-blueprint', eventId, workTypeId, levelId, contentType],
    queryFn: () => pageCompetitionQuestionBlueprints(eventId, {
      page: 1, pageSize: 20, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType, status: 'active',
    }),
    enabled: !simpleMode && Boolean(eventId && workTypeId && levelId),
  });
  const activeBlueprint = simpleMode ? undefined : blueprintQuery.data?.items?.[0];
  const blueprintTreeQuery = useQuery({
    queryKey: ['competition-active-blueprint-tree', eventId, activeBlueprint?.id],
    queryFn: () => getCompetitionQuestionBlueprintTree(eventId, activeBlueprint!.id),
    enabled: Boolean(eventId && activeBlueprint?.id && formOpen),
  });
  const blueprintNodes = useMemo(() => {
    const rows: Array<{ value: string; label: string }> = [];
    const walk = (nodes: NonNullable<typeof blueprintTreeQuery.data>['tree'], prefix = '') => nodes.forEach(node => {
      const path = prefix ? `${prefix} / ${node.name}` : node.name;
      if (!node.children?.length) rows.push({ value: node.id, label: `${node.fullCode} ${path}` });
      else walk(node.children, path);
    });
    if (blueprintTreeQuery.data?.tree) walk(blueprintTreeQuery.data.tree);
    return rows;
  }, [blueprintTreeQuery.data]);
  const questionsQuery = useQuery({
    queryKey: ['competition-questions', eventId, workTypeId, levelId, contentType, questionType, keyword, bindingStatus, blueprintNodeId, activeBlueprint?.id, simpleMode, page],
    queryFn: () => pageCompetitionQuestions(eventId, {
      page,
      pageSize: 20,
      eventWorkTypeId: workTypeId,
      eventWorkTypeLevelId: levelId,
      contentType,
      questionType,
      keyword: keyword || undefined,
      blueprintId: activeBlueprint?.id,
      blueprintNodeId,
      bindingStatus,
      unboundOnly: simpleMode || undefined,
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
    refetchInterval: paneActive && batchId ? 1500 : false,
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
      // Historical questions may remain unbound and editable.  Do not send
      // the current active blueprint implicitly during an edit; only a newly
      // selected node creates/replaces a binding.  New questions still carry
      // the required blueprint/node from the form.
      if (!editing) {
        body.blueprintId = values.blueprintId;
        body.blueprintNodeId = values.blueprintNodeId;
      } else if (values.blueprintNodeId && values.blueprintNodeId !== editing.blueprintNodeId) {
        body.blueprintId = activeBlueprint?.id;
        body.blueprintNodeId = values.blueprintNodeId;
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
    mutationFn: (file: File) => simpleMode
      ? contentType === 'practical'
        ? importCompetitionSimplePracticalDetails(eventId, {
          eventWorkTypeId: workTypeId,
          eventWorkTypeLevelId: levelId,
        }, file)
        : importCompetitionSimpleTheoryQuestions(eventId, {
        eventWorkTypeId: workTypeId,
        eventWorkTypeLevelId: levelId,
      }, file)
      : importCompetitionQuestions(eventId, {
        eventWorkTypeId: workTypeId,
        eventWorkTypeLevelId: levelId,
        contentType,
        blueprintId: activeBlueprint!.id,
        selectedSheet: selectedSheet.trim() || undefined,
      }, file),
    onSuccess: (batch) => {
      setBatchId(batch.id);
      message.success('文件已接收，正在后台校验');
    },
  });
  const bindingMutation = useMutation({
    mutationFn: () => batchBindCompetitionQuestions(eventId, {
      blueprintId: activeBlueprint!.id,
      blueprintNodeId: bindingTargetNodeId!,
      questionIds: selectedQuestionIds,
    }),
    onSuccess: () => {
      message.success(`已绑定 ${selectedQuestionIds.length} 道历史试题`);
      setSelectedQuestionIds([]);
      setBindingTargetNodeId(undefined);
      setBindingOpen(false);
      queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
      queryClient.invalidateQueries({ queryKey: ['competition-blueprint-coverage'] });
    },
  });

  const openEdit = (record: CompetitionQuestion) => {
    if (contentType === 'practical') {
      setEditing(record);
      setFormOpen(true);
      return;
    }
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
      blueprintId: record.blueprintId || undefined,
      blueprintNodeId: record.blueprintNodeId || undefined,
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
    const blob = await downloadCompetitionQuestionTemplate(contentType, simpleMode);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = simpleMode
      ? contentType === 'practical' ? '竞赛简易实操评分明细表模板.xlsx' : '竞赛简易理论试题导入模板.xlsx'
      : contentType === 'theory' ? '竞赛理论试题模板.xlsx' : '竞赛实操试题模板.xlsx';
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
      title={simpleMode
        ? contentType === 'practical' ? '简易实操评分项' : '简易理论试题'
        : `竞赛${contentType === 'theory' ? '理论' : '实操'}试题`}
      extra={(
        <Space wrap>
          <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载导入模板</Button>
          {contentType === 'practical' && !simpleMode && <Input allowClear value={selectedSheet} onChange={(event) => setSelectedSheet(event.target.value)} placeholder="实操工作表（可选）" style={{ width: 170 }} />}
          {!simpleMode && <Button disabled={!activeBlueprint || selectedQuestionIds.length === 0} onClick={() => setBindingOpen(true)}>批量绑定考评点</Button>}
          {simpleMode && contentType === 'practical' && <Button icon={<PlusOutlined />} disabled={!eventId || !workTypeId || !levelId} onClick={() => setSimplePracticalCreateOpen(true)}>新增项目</Button>}
          <Upload {...uploadProps} disabled={!eventId || !workTypeId || !levelId || (!simpleMode && !activeBlueprint) || importMutation.isPending}>
            <Button type="primary" icon={<UploadOutlined />}>{simpleMode
              ? contentType === 'practical' ? '导入评分明细表' : '批量导入试题'
              : '导入试题'}</Button>
          </Upload>
          {!simpleMode && <Button icon={<PlusOutlined />} disabled={!activeBlueprint} onClick={() => { setEditing(null); form.resetFields(); form.setFieldsValue({
            eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId,
            questionType: contentType === 'theory' ? 'single' : 'practical', status: 'enabled', difficulty: 2,
            blueprintId: activeBlueprint?.id,
          }); setFormOpen(true); }}>新增试题</Button>}
        </Space>
      )}
    >
      {simpleMode
        ? <Alert type="info" showIcon style={{ marginBottom: 16 }} message={contentType === 'practical'
          ? '实操评分项按赛事、工种和等级管理，无需配置细目表。每次导入一个考核项目，上传包含评分明细的 Excel 表。'
          : '简易理论试题按赛事、工种和等级管理，导入时无需配置细目表。当前支持单选、多选、判断和填空题。'} />
        : !activeBlueprint && eventId && workTypeId && levelId
        ? <Alert type="warning" showIcon style={{ marginBottom: 16 }} message="当前范围没有启用的竞赛细目表，请先导入并启用理论/实操细目表；未绑定题目不会进入自动组卷。" />
        : <Alert type="info" showIcon style={{ marginBottom: 16 }} message="竞赛题库与课程题库完全独立；题库导入必须绑定当前启用细目表的末级考评点，历史未绑定题目仍可编辑但不会进入自动组卷。" />}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          showSearch optionFilterProp="label" style={{ width: 240 }} placeholder="选择赛事"
          value={eventId || undefined}
          options={events.map((event: CompetitionEvent) => ({ value: String(event.id), label: competitionEventLabel(event) }))}
          onChange={(value) => { setEventId(value); setWorkTypeId(''); setLevelId(''); setBindingStatus(undefined); setBlueprintNodeId(undefined); setSelectedQuestionIds([]); setPage(1); }}
        />
        <Select
          style={{ width: 210 }} placeholder="选择工种" value={workTypeId || undefined}
          options={workTypes.map((item: CompetitionEventWorkType) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) }))}
          onChange={(value) => { setWorkTypeId(value); setLevelId(''); setBindingStatus(undefined); setBlueprintNodeId(undefined); setSelectedQuestionIds([]); setPage(1); }}
        />
        <Select
          style={{ width: 180 }} placeholder="选择等级" value={levelId || undefined}
          options={levels.map((item: CompetitionEventWorkTypeLevel) => ({ value: String(item.id), label: `${item.levelName}（${item.levelCode}）` }))}
          onChange={(value) => { setLevelId(value); setBindingStatus(undefined); setBlueprintNodeId(undefined); setSelectedQuestionIds([]); setPage(1); }}
        />
        {!(simpleMode && contentType === 'practical') && <Select allowClear style={{ width: 140 }} placeholder="题型" value={questionType} options={typeOptions}
          onChange={(value) => { setQuestionType(value); setPage(1); }} />}
        {!simpleMode && <Select allowClear style={{ width: 130 }} placeholder="绑定状态" value={bindingStatus} disabled={!activeBlueprint}
          options={[{ value: 'bound', label: '已绑定' }, { value: 'unbound', label: '未绑定' }]}
          onChange={(value) => { setBindingStatus(value); setPage(1); }} />}
        {!simpleMode && <Select allowClear showSearch optionFilterProp="label" style={{ width: 260 }} placeholder="考评点" value={blueprintNodeId} disabled={!activeBlueprint}
          options={blueprintNodes} onChange={(value) => { setBlueprintNodeId(value); setPage(1); }} />}
        <Input.Search allowClear placeholder={simpleMode && contentType === 'practical' ? '搜索考核项目' : '搜索题干'} style={{ width: 240 }} value={keyword}
          onChange={(event) => setKeyword(event.target.value)} onSearch={() => setPage(1)} />
      </Space>
      <Table<CompetitionQuestion>
        rowKey="id" loading={questionsQuery.isLoading || questionsQuery.isFetching}
        rowSelection={simpleMode ? undefined : { selectedRowKeys: selectedQuestionIds, onChange: (keys) => setSelectedQuestionIds(keys.map(String)) }}
        dataSource={data?.items ?? []}
        pagination={{ current: data?.page ?? page, pageSize: data?.pageSize ?? 20, total: data?.total ?? 0, onChange: setPage, showSizeChanger: false }}
        columns={simpleMode && contentType === 'practical' ? [
          { title: '考核项目', dataIndex: 'stem', ellipsis: true, render: (value: string) => <Typography.Text ellipsis={{ tooltip: true }}>{value}</Typography.Text> },
          { title: '考核内容', width: 260, render: (_: unknown, record: CompetitionQuestion) => {
            const content = record.practicalDetail?.assessmentContents?.map(item => plainText(item.contentHtml)).filter(item => item !== '-') ?? [];
            return <Typography.Text ellipsis={{ tooltip: content.join('；') }}>{content.join('；') || plainText(record.operationContent)}</Typography.Text>;
          } },
          { title: '评分项', width: 90, render: (_: unknown, record: CompetitionQuestion) => record.scoreItems?.length ?? 0 },
          { title: '计分方式', width: 100, render: (_: unknown, record: CompetitionQuestion) => practicalScoringMethodLabel(record.practicalDetail?.scoringMethod) },
          { title: '总分', width: 90, render: (_: unknown, record: CompetitionQuestion) => record.practicalDetail?.totalScore ?? record.scoreItems?.reduce((total, item) => total + Number(item.maxScore || 0), 0) ?? '-' },
          { title: '状态', dataIndex: 'status', width: 90, render: (value: string) => value === 'enabled' ? <Tag color="green">启用</Tag> : <Tag>停用</Tag> },
          { title: '操作', key: 'actions', width: 180, render: (_: unknown, record: CompetitionQuestion) => (
            <Space size={0} style={{ whiteSpace: 'nowrap' }}>
              <Button type="link" size="small" style={{ paddingInline: 0 }} icon={<EyeOutlined />} onClick={() => setPreviewing(record.id)}>查看评分明细</Button>
              <Popconfirm title="确认删除此实操评分项？" onConfirm={() => deleteMutation.mutate(record.id)}>
                <Button type="link" danger size="small" style={{ paddingInline: 0 }} icon={<DeleteOutlined />}>删除</Button>
              </Popconfirm>
            </Space>
          ) },
        ] : [
          { title: '序号', dataIndex: 'sortOrder', width: 80 },
          { title: '题型', dataIndex: 'questionType', width: 100, render: questionTypeTag },
          { title: '题干', dataIndex: 'stem', ellipsis: true, render: (value: string) => <Typography.Text ellipsis={{ tooltip: true }}>{value}</Typography.Text> },
          { title: '难度', dataIndex: 'difficulty', width: 90, render: (value: number | null) => value ? ['-', '简单', '中等', '困难', '较难', '极难'][value] || value : '-' },
          ...(!simpleMode ? [{ title: '绑定状态', dataIndex: 'bindingStatus', width: 100, render: (value: string) => value === 'bound' ? <Tag color="green">已绑定</Tag> : <Tag color="orange">未绑定</Tag> }] : []),
          { title: '状态', dataIndex: 'status', width: 90, render: (value: string) => value === 'enabled' ? <Tag color="green">启用</Tag> : <Tag>停用</Tag> },
          { title: '操作', key: 'actions', width: 170, render: (_: unknown, record: CompetitionQuestion) => (
            <Space size={0} style={{ whiteSpace: 'nowrap' }}>
              <Button type="link" size="small" style={{ paddingInline: 0 }} icon={<EyeOutlined />} onClick={() => setPreviewing(record.id)}>预览</Button>
              {!simpleMode && <Button type="link" size="small" style={{ paddingInline: 0 }} icon={<EditOutlined />} onClick={() => openEdit(record)}>编辑</Button>}
              <Popconfirm title="确认删除此竞赛试题？" onConfirm={() => deleteMutation.mutate(record.id)}>
                <Button type="link" danger size="small" style={{ paddingInline: 0 }} icon={<DeleteOutlined />}>删除</Button>
              </Popconfirm>
            </Space>
          ) },
        ]}
      />

      <Modal
        title={`批量绑定 ${selectedQuestionIds.length} 道历史试题`}
        open={bindingOpen}
        onCancel={() => { setBindingOpen(false); setBindingTargetNodeId(undefined); }}
        onOk={() => bindingMutation.mutate()}
        okButtonProps={{ disabled: !bindingTargetNodeId }}
        confirmLoading={bindingMutation.isPending}
      >
        <Alert type="info" showIcon message="只能选择当前启用细目表的末级考评点；绑定后题目才会进入新的自动组卷。" style={{ marginBottom: 12 }} />
        <Select showSearch optionFilterProp="label" style={{ width: '100%' }} value={bindingTargetNodeId} options={blueprintNodes} onChange={setBindingTargetNodeId} placeholder="选择末级考评点" />
      </Modal>

      {contentType === 'practical' && (
        <PracticalQuestionEditorModal
          open={formOpen}
          eventId={eventId}
          workTypeId={workTypeId}
          levelId={levelId}
          workTypes={workTypes}
          levels={levels}
          editing={editing}
          blueprintId={activeBlueprint?.id}
          blueprintNodes={blueprintNodes}
          onClose={() => { setEditing(null); setFormOpen(false); form.resetFields(); }}
          onSaved={() => { setEditing(null); setFormOpen(false); form.resetFields(); queryClient.invalidateQueries({ queryKey: ['competition-questions'] }); }}
        />
      )}

      {simpleMode && contentType === 'practical' && (
        <SimplePracticalQuestionCreateModal
          open={simplePracticalCreateOpen}
          eventId={eventId}
          workTypeId={workTypeId}
          levelId={levelId}
          onClose={() => setSimplePracticalCreateOpen(false)}
          onSaved={() => {
            setSimplePracticalCreateOpen(false);
            queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
          }}
        />
      )}

      {contentType === 'theory' && <Modal title={editing ? '编辑竞赛试题' : '新增竞赛试题'} open={formOpen}
        onCancel={() => { setEditing(null); setFormOpen(false); form.resetFields(); }} onOk={() => form.submit()} confirmLoading={saveMutation.isPending} destroyOnClose width={760}>
        <Form form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
          <Space wrap align="start">
            <Form.Item name="eventWorkTypeId" label="工种" rules={[{ required: true }]}><Select style={{ width: 220 }} options={workTypes.map(item => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) }))} /></Form.Item>
            <Form.Item name="eventWorkTypeLevelId" label="等级" rules={[{ required: true }]}><Select style={{ width: 170 }} options={levels.map(item => ({ value: String(item.id), label: item.levelName }))} /></Form.Item>
            <Form.Item name="blueprintId" hidden><Input /></Form.Item>
            <Form.Item name="questionType" label="题型" rules={[{ required: true }]}><Select style={{ width: 130 }} options={typeOptions} onChange={(value) => handleQuestionTypeChange(value as CompetitionQuestionType)} /></Form.Item>
            {activeBlueprint && <Form.Item name="blueprintNodeId" label="考评点" rules={[{ required: !editing, message: '请选择末级考评点' }]}><Select allowClear showSearch optionFilterProp="label" style={{ width: 300 }} options={blueprintNodes} placeholder={editing ? '如需绑定可选择末级考评点' : '选择细目表末级考评点'} /></Form.Item>}
            <Form.Item
              label="序号"
              extra={editing ? '按创建顺序生成，编辑时不可修改' : '保存后按创建顺序自动生成'}
            >
              <Input disabled value={editing ? String(editing.sortOrder) : undefined} placeholder="自动生成" />
            </Form.Item>
            <Form.Item name="difficulty" label="难度"><InputNumber min={1} max={5} /></Form.Item>
          </Space>
          <Form.Item name="stem" label="题干" rules={[{ required: true }]}><Input.TextArea rows={4} /></Form.Item>
          {renderTheoryAnswerFields()}
          <Form.Item name="status" label="状态"><Select options={[{ value: 'enabled', label: '启用' }, { value: 'disabled', label: '停用' }]} /></Form.Item>
        </Form>
      </Modal>}

      <Modal title={simpleMode && contentType === 'practical' ? '实操评分明细' : '竞赛试题预览'} open={Boolean(previewing)} onCancel={() => setPreviewing(null)} footer={null} width={simpleMode && contentType === 'practical' ? 1120 : 720}>
        {previewQuery.isLoading ? <Spin /> : previewQuery.data ? <>
          <Descriptions column={3} size="small" style={{ marginBottom: 16 }}>
            {!simpleMode && <Descriptions.Item label="题号">{previewQuery.data.sortOrder}</Descriptions.Item>}
            {!simpleMode && <Descriptions.Item label="题型">{questionTypeTag(previewQuery.data.questionType)}</Descriptions.Item>}
            {simpleMode && contentType === 'practical' && <Descriptions.Item label="考核项目">{previewQuery.data.stem}</Descriptions.Item>}
            {simpleMode && contentType === 'practical' && <Descriptions.Item label="计分方式">{practicalScoringMethodLabel(previewQuery.data.practicalDetail?.scoringMethod)}</Descriptions.Item>}
            {simpleMode && contentType === 'practical' && <Descriptions.Item label="总分">{previewQuery.data.practicalDetail?.totalScore ?? '-'}</Descriptions.Item>}
          </Descriptions>
          {!simpleMode && <Typography.Paragraph>{previewQuery.data.stem}</Typography.Paragraph>}
          {previewQuery.data.optionsJson && <Typography.Paragraph type="secondary">选项：{previewQuery.data.optionsJson}</Typography.Paragraph>}
          {contentType === 'practical' && <>
            {simpleMode ? <Table size="small" rowKey="id" pagination={false}
              dataSource={previewQuery.data.practicalDetail?.assessmentContents?.flatMap(content => content.elements.map(element => ({
                ...element,
                assessmentContent: plainText(content.contentHtml),
                standard: element.standardHtml,
                scoringMethod: previewQuery.data!.practicalDetail?.scoringMethod,
                stepText: element.elementHtml || String(element.elementOrder ?? ''),
              }))) ?? []}
              columns={[
                { title: '考核项目', dataIndex: 'stem', render: () => previewQuery.data!.stem },
                { title: '考核内容', dataIndex: 'assessmentContent' },
                { title: '评分项', dataIndex: 'itemName' },
                { title: '评分标准', dataIndex: 'standard', render: (value: string) => plainText(value) },
                { title: '计分方式', dataIndex: 'scoringMethod', render: (value: string) => practicalScoringMethodLabel(value) },
                { title: '步骤', dataIndex: 'stepText', render: (value: string) => plainText(value) },
                { title: '分值', dataIndex: 'maxScore' },
              ]} /> : <>
            <Typography.Paragraph><b>操作内容：</b>{previewQuery.data.operationContent || '-'}</Typography.Paragraph>
            <Typography.Paragraph><b>考生准备：</b>{previewQuery.data.studentPrepare || '-'}</Typography.Paragraph>
            <Typography.Paragraph><b>设备准备：</b>{previewQuery.data.equipmentPrepare || '-'}</Typography.Paragraph>
            <Table size="small" rowKey="id" pagination={false} dataSource={previewQuery.data.scoreItems} columns={[{ title: '序号', dataIndex: 'itemNo' }, { title: '评分要素', dataIndex: 'itemName' }, { title: '评分标准', dataIndex: 'criterion' }, { title: '最大分值', dataIndex: 'maxScore' }, { title: '最大扣分', dataIndex: 'maxDeduction' }, { title: '步长', dataIndex: 'deductionStep' }]} />
            </>}
          </>}
        </> : <Alert type="warning" message="无法加载题目" />}
      </Modal>

      <Modal title="导入进度" open={Boolean(batchId)} onCancel={() => setBatchId(null)} footer={null}>
        {batchQuery.data ? <>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Typography.Text>文件：{batchQuery.data.originalFilename}</Typography.Text>
            {batchQuery.data.selectedSheet && <Typography.Text>工作表：{batchQuery.data.selectedSheet}</Typography.Text>}
            <Typography.Text>状态：{batchQuery.data.status === 'processing' ? '解析中' : batchQuery.data.status === 'succeeded' ? '导入成功' : batchQuery.data.status === 'failed' ? '导入失败' : '排队中'}</Typography.Text>
            <Typography.Text>总数：{batchQuery.data.totalCount}，成功：{batchQuery.data.successCount}，失败：{batchQuery.data.failureCount}</Typography.Text>
            {batchQuery.data.metrics && (
              <Alert type="info" showIcon message="导入耗时"
                description={`总耗时 ${(batchQuery.data.metrics.totalMs / 1000).toFixed(1)}s；解析 ${(batchQuery.data.metrics.excelParseMs / 1000).toFixed(1)}s；校验 ${(batchQuery.data.metrics.validationMs / 1000).toFixed(1)}s；落库 ${(batchQuery.data.metrics.persistMs / 1000).toFixed(1)}s；提交 ${(batchQuery.data.metrics.commitMs / 1000).toFixed(1)}s。题目批次 ${batchQuery.data.metrics.questionBatchCount}，绑定批次 ${batchQuery.data.metrics.bindingBatchCount}，SQL ${batchQuery.data.metrics.totalSqlCount}，平均每批 ${batchQuery.data.metrics.avgBatchWriteMs ?? 0}ms`} />
            )}
            {batchQuery.data.status === 'failed' && batchQuery.data.errorDetail && <Alert type="error" showIcon message="错误明细" description={batchQuery.data.errorDetail} />}
          </Space>
        </> : <Spin />}
      </Modal>
    </Card>
  );
}

export function CompetitionSimplePracticalQuestionPage() {
  return <CompetitionQuestionBankPage contentType="practical" simpleMode />;
}

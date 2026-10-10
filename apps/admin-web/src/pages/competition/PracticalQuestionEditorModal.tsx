import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  DeleteOutlined,
  EyeOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Row,
  Select,
  Space,
  Steps,
  Tabs,
  Table,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import {
  createCompetitionQuestion,
  getCompetitionQuestion,
  updateCompetitionQuestion,
} from '../../api/competitionQuestion';
import type { CompetitionEventWorkType, CompetitionEventWorkTypeLevel } from '../../types/competition';
import type {
  CompetitionPracticalAssessmentContent,
  CompetitionPracticalAssessmentContentRequest,
  CompetitionPracticalAssessmentElement,
  CompetitionPracticalAssessmentElementRequest,
  CompetitionPracticalQuestionDetail,
  CompetitionPracticalQuestionDetailRequestV2,
  CompetitionPracticalScoringMethod,
  CompetitionQuestion,
  CompetitionQuestionRequest,
} from '../../types/competitionQuestion';
import type { CompetitionId } from '../../types/competition';
import { RichTextEditor, RichTextPreview } from '../../components/RichTextEditor';
import './PracticalQuestionEditorModal.css';

interface Props {
  open: boolean;
  eventId: CompetitionId;
  workTypeId: CompetitionId;
  levelId: CompetitionId;
  workTypes: CompetitionEventWorkType[];
  levels: CompetitionEventWorkTypeLevel[];
  editing: CompetitionQuestion | null;
  blueprintId?: string;
  blueprintNodes?: Array<{ value: string; label: string }>;
  onClose: () => void;
  onSaved: () => void;
}

type EditorElement = {
  key: string;
  elementOrder: number;
  elementHtml: string;
  allocationEnabled: boolean;
  maxScore?: number;
  standardHtml: string;
  maxDeduction?: number;
  deductionStep?: number;
};
type EditorContent = {
  key: string;
  sortOrder: number;
  contentHtml: string;
  elements: EditorElement[];
};

interface BasicValues {
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  stem: string;
  durationMinutes: number;
  scoreDifference: number;
  totalScore: number;
  scoringMethod: CompetitionPracticalScoringMethod;
  status: string;
  blueprintId?: string;
  blueprintNodeId?: string;
}

interface PreviewRow {
  key: string;
  contentKey: string;
  contentHtml: string;
  contentTotal: number;
  contentRowSpan: number;
  contentFirst: boolean;
  elementHtml: string;
  maxScore?: number;
  standardHtml: string;
  maxDeduction?: number;
  deductionStep?: number;
  scoringMethod: CompetitionPracticalScoringMethod;
  allocationEnabled: boolean;
}

const emptyElement = (elementOrder = 1): EditorElement => ({
  key: `element-${Date.now()}-${Math.random()}`,
  elementOrder,
  elementHtml: '<p></p>',
  allocationEnabled: true,
  maxScore: undefined,
  standardHtml: '<p></p>',
  maxDeduction: undefined,
  deductionStep: undefined,
});

const emptyContent = (sortOrder = 1): EditorContent => ({
  key: `content-${Date.now()}-${Math.random()}`,
  sortOrder,
  contentHtml: '<p></p>',
  elements: [emptyElement()],
});

function htmlText(value?: string | null) {
  return (value || '').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
}

function nextKey(prefix: string, id?: string | null) {
  return `${prefix}-${id || ''}-${Date.now()}-${Math.random()}`;
}

function fromDetail(question: CompetitionQuestion, detail?: CompetitionPracticalQuestionDetail | null): EditorContent[] {
  const contents = detail?.assessmentContents || [];
  if (contents.length > 0) {
    return contents.map((content, contentIndex) => ({
      key: nextKey('content', content.id),
      sortOrder: content.sortOrder || contentIndex + 1,
      contentHtml: content.contentHtml || '<p></p>',
      elements: (content.elements || []).map((element, elementIndex) => ({
        key: nextKey('element', element.id),
        elementOrder: element.elementOrder || elementIndex + 1,
        elementHtml: element.elementHtml || '<p></p>',
        allocationEnabled: element.allocationEnabled !== false,
        maxScore: element.maxScore == null ? undefined : Number(element.maxScore),
        standardHtml: element.standardHtml || '<p></p>',
        maxDeduction: element.maxDeduction == null ? undefined : Number(element.maxDeduction),
        deductionStep: element.deductionStep == null ? undefined : Number(element.deductionStep),
      })),
    }));
  }
  const legacy = (question.scoreItems || []).map((item, index) => ({
    key: nextKey('element', item.id),
    elementOrder: index + 1,
    elementHtml: item.itemName ? `<p>${item.itemName}</p>` : '<p></p>',
    allocationEnabled: true,
    maxScore: item.maxScore == null ? undefined : Number(item.maxScore),
    standardHtml: item.criterion ? `<p>${item.criterion}</p>` : '<p></p>',
    maxDeduction: item.maxDeduction == null ? undefined : Number(item.maxDeduction),
    deductionStep: item.deductionStep == null ? undefined : Number(item.deductionStep),
  }));
  return [{
    ...emptyContent(),
    contentHtml: detail?.assessmentRequirementsHtml || question.operationContent || '<p>默认考核内容</p>',
    elements: legacy.length > 0 ? legacy : [emptyElement()],
  }];
}

function normalizeContents(contents: EditorContent[]): EditorContent[] {
  return contents.map((content, contentIndex) => ({
    ...content,
    sortOrder: contentIndex + 1,
    elements: content.elements.map((element, elementIndex) => ({ ...element, elementOrder: elementIndex + 1 })),
  }));
}

function contentAllocatedTotal(content: EditorContent) {
  return content.elements.reduce(
    (sum, element) => sum + (element.allocationEnabled ? Number(element.maxScore || 0) : 0),
    0,
  );
}

function buildPreviewRows(contents: EditorContent[], values: BasicValues): PreviewRow[] {
  const rows: PreviewRow[] = [];
  for (const content of contents) {
    const elements = content.elements.length > 0 ? content.elements : [emptyElement()];
    const contentTotal = elements.reduce((sum, element) => sum + (element.allocationEnabled ? Number(element.maxScore || 0) : 0), 0);
    elements.forEach((element, index) => rows.push({
      key: `${content.key}-${element.key}`,
      contentKey: content.key,
      contentHtml: content.contentHtml,
      contentTotal,
      contentRowSpan: elements.length,
      contentFirst: index === 0,
      elementHtml: element.elementHtml,
      maxScore: element.allocationEnabled ? element.maxScore : undefined,
      standardHtml: element.standardHtml,
      maxDeduction: element.allocationEnabled && values.scoringMethod === 'deductive' ? element.maxDeduction : undefined,
      deductionStep: element.allocationEnabled && values.scoringMethod === 'deductive' ? element.deductionStep : undefined,
      scoringMethod: values.scoringMethod,
      allocationEnabled: element.allocationEnabled,
    }));
  }
  return rows;
}

export function PracticalScoreTablePreview({ contents, values }: { contents: EditorContent[]; values: BasicValues }) {
  const rows = useMemo(() => buildPreviewRows(contents, values), [contents, values]);
  const columns: ColumnsType<PreviewRow> = [
    {
      title: '考核内容', dataIndex: 'contentHtml', width: 180,
      render: (value, row) => row.contentFirst ? { children: <RichTextPreview value={value} />, props: { rowSpan: row.contentRowSpan } } : { children: null, props: { rowSpan: 0 } },
    },
    {
      title: '总分', dataIndex: 'contentTotal', width: 70, align: 'center',
      render: (value, row) => row.contentFirst ? { children: value.toFixed(2).replace(/\.00$/, ''), props: { rowSpan: row.contentRowSpan } } : { children: null, props: { rowSpan: 0 } },
    },
    { title: '评分要素', dataIndex: 'elementHtml', width: 190, render: (value) => <RichTextPreview value={value} /> },
    { title: '配分', dataIndex: 'maxScore', width: 70, align: 'center', render: (value, row) => row.allocationEnabled ? value : null },
    { title: '评分标准', dataIndex: 'standardHtml', width: 360, render: (value) => <RichTextPreview value={value} /> },
    { title: '最大扣分', dataIndex: 'maxDeduction', width: 90, align: 'center', render: (value, row) => row.scoringMethod === 'deductive' && row.allocationEnabled ? value : null },
    { title: '步长', dataIndex: 'deductionStep', width: 70, align: 'center', render: (value, row) => row.scoringMethod === 'deductive' && row.allocationEnabled ? value : null },
    { title: '评分方式', dataIndex: 'scoringMethod', width: 90, align: 'center', render: (value) => value === 'additive' ? '加分' : '扣分' },
  ];
  return <Table<PreviewRow> bordered size="small" pagination={false} rowKey="key" dataSource={rows} columns={columns} scroll={{ x: 1120 }} />;
}

export function PracticalQuestionEditorModal({
  open, eventId, workTypeId, levelId, workTypes, levels, editing, onClose, onSaved,
  blueprintId, blueprintNodes,
}: Props) {
  const [form] = Form.useForm<BasicValues>();
  const [activeTab, setActiveTab] = useState('requirements');
  const [step, setStep] = useState(0);
  const [contents, setContents] = useState<EditorContent[]>([emptyContent()]);
  const [assessmentRequirementsHtml, setAssessmentRequirementsHtml] = useState('');
  const [examRoomPrepareHtml, setExamRoomPrepareHtml] = useState('');
  const [candidatePrepareHtml, setCandidatePrepareHtml] = useState('');
  const [negativeItemsHtml, setNegativeItemsHtml] = useState('');
  const [questionMaterialsHtml, setQuestionMaterialsHtml] = useState('');
  const [previewOpen, setPreviewOpen] = useState(false);

  const detailQuery = useQuery({
    queryKey: ['competition-practical-question-editor', eventId, editing?.id],
    queryFn: () => getCompetitionQuestion(eventId, editing!.id),
    enabled: open && Boolean(editing?.id),
  });

  useEffect(() => {
    if (!open) return;
    setStep(0);
    setActiveTab('requirements');
    setPreviewOpen(false);
    if (!editing) {
      form.resetFields();
      form.setFieldsValue({
        eventWorkTypeId: String(workTypeId), eventWorkTypeLevelId: String(levelId),
        durationMinutes: 60, scoreDifference: 10, totalScore: 100,
        scoringMethod: 'deductive', status: 'enabled', stem: '',
        blueprintId, blueprintNodeId: undefined,
      });
      setContents([emptyContent()]);
      setAssessmentRequirementsHtml(''); setExamRoomPrepareHtml(''); setCandidatePrepareHtml('');
      setNegativeItemsHtml(''); setQuestionMaterialsHtml('');
    }
  }, [editing, form, levelId, open, workTypeId]);

  useEffect(() => {
    const question = detailQuery.data;
    if (!open || !editing || !question) return;
    const detail = question.practicalDetail;
    form.setFieldsValue({
      eventWorkTypeId: question.eventWorkTypeId,
      eventWorkTypeLevelId: question.eventWorkTypeLevelId,
      stem: question.stem,
      durationMinutes: detail?.durationMinutes || 60,
      scoreDifference: detail?.scoreDifference == null ? 10 : Number(detail.scoreDifference),
      totalScore: detail?.totalScore == null ? 100 : Number(detail.totalScore),
      scoringMethod: detail?.scoringMethod === 'additive' ? 'additive' : 'deductive',
      status: question.status,
      blueprintNodeId: question.blueprintNodeId || undefined,
    });
    setContents(fromDetail(question, detail));
    setAssessmentRequirementsHtml(detail?.assessmentRequirementsHtml || question.operationContent || '');
    setExamRoomPrepareHtml(detail?.examRoomPrepareHtml || question.equipmentPrepare || '');
    setCandidatePrepareHtml(detail?.candidatePrepareHtml || question.studentPrepare || '');
    setNegativeItemsHtml(detail?.negativeItemsHtml || '');
    setQuestionMaterialsHtml(detail?.questionMaterialsHtml || '');
  }, [detailQuery.data, editing, form, open]);

  const saveMutation = useMutation({
    mutationFn: async (values: BasicValues) => {
      const normalizedContents = normalizeContents(contents);
      const allocatedTotal = normalizedContents.reduce((sum, content) => sum + content.elements.reduce(
        (inner, element) => inner + (element.allocationEnabled ? Number(element.maxScore || 0) : 0), 0), 0);
      if (normalizedContents.some((content) => !htmlText(content.contentHtml))) throw new Error('请完善所有考核内容');
      if (normalizedContents.some((content) => content.elements.length === 0 || content.elements.some((element) => !htmlText(element.elementHtml) || !htmlText(element.standardHtml)))) {
        throw new Error('请完善所有评分要素和评分标准');
      }
      if (Number(values.durationMinutes) <= 0 || !Number.isInteger(Number(values.durationMinutes))) {
        throw new Error('考试时间必须为正整数分钟');
      }
      if (Number(values.totalScore) <= 0 || Number(values.totalScore) > 100) {
        throw new Error('试题总分必须大于0且不超过100分');
      }
      if (Number(values.scoreDifference) < 0 || Number(values.scoreDifference) > Number(values.totalScore)) {
        throw new Error('评分差值必须在0到试题总分之间');
      }
      if (Math.abs(allocatedTotal - Number(values.totalScore)) > 1e-8) throw new Error(`配分合计 ${allocatedTotal} 分，必须等于试题总分 ${values.totalScore} 分`);
      if (normalizedContents.some((content) => content.elements.some((element) => element.allocationEnabled && (element.maxScore == null || element.maxScore <= 0 || element.maxScore > 100)))) {
        throw new Error('最大配分必须大于0且不超过100分');
      }
      if (values.scoringMethod === 'deductive' && normalizedContents.some((content) => content.elements.some((element) => element.allocationEnabled && (element.maxDeduction == null || element.deductionStep == null || element.maxDeduction < 0 || element.maxDeduction > Number(element.maxScore) || element.deductionStep <= 0 || Math.abs((Number(element.maxDeduction) / Number(element.deductionStep)) - Math.round(Number(element.maxDeduction) / Number(element.deductionStep))) > 1e-8)))) {
        throw new Error('扣分制的最大扣分和步长设置无效');
      }
      if (values.scoringMethod === 'additive' && normalizedContents.some((content) => content.elements.some((element) => element.maxDeduction != null || element.deductionStep != null))) {
        throw new Error('加分制不需要填写最大扣分和步长');
      }
      const practicalDetail: CompetitionPracticalQuestionDetailRequestV2 = {
        durationMinutes: Number(values.durationMinutes),
        scoreDifference: Number(values.scoreDifference),
        totalScore: Number(values.totalScore),
        scoringMethod: values.scoringMethod,
        assessmentRequirementsHtml,
        examRoomPrepareHtml,
        candidatePrepareHtml,
        negativeItemsHtml,
        questionMaterialsHtml,
        assessmentContents: normalizedContents.map(({ key: _key, ...content }) => ({
          ...content,
          elements: content.elements.map(({ key: _elementKey, ...element }) => element),
        })),
      };
      const body: CompetitionQuestionRequest = {
        eventWorkTypeId: values.eventWorkTypeId,
        eventWorkTypeLevelId: values.eventWorkTypeLevelId,
        contentType: 'practical', questionType: 'practical', stem: values.stem,
        status: values.status, practicalDetail,
      };
      // Keep legacy/unbound questions editable.  Binding is sent for a new
      // question, or only when an editor explicitly picks a different node;
      // otherwise an archived historical binding is left untouched.
      if (!editing) {
        body.blueprintId = blueprintId;
        body.blueprintNodeId = values.blueprintNodeId;
      } else if (values.blueprintNodeId && values.blueprintNodeId !== editing.blueprintNodeId) {
        body.blueprintId = blueprintId;
        body.blueprintNodeId = values.blueprintNodeId;
      }
      return editing ? updateCompetitionQuestion(eventId, editing.id, body) : createCompetitionQuestion(eventId, body);
    },
    onSuccess: () => { message.success(editing ? '竞赛实操试题已更新' : '竞赛实操试题已创建'); onSaved(); },
    onError: (error: Error) => message.error(error.message || '保存失败'),
  });

  const values = Form.useWatch([], form) as BasicValues | undefined;
  const currentValues: BasicValues = values || {
    eventWorkTypeId: String(workTypeId), eventWorkTypeLevelId: String(levelId), stem: '',
    durationMinutes: 60, scoreDifference: 10, totalScore: 100, scoringMethod: 'deductive', status: 'enabled',
  };

  const updateContent = (key: string, updater: (content: EditorContent) => EditorContent) => {
    setContents((current) => normalizeContents(current.map((content) => content.key === key ? updater(content) : content)));
  };
  const updateElement = (contentKey: string, elementKey: string, updater: (element: EditorElement) => EditorElement) => {
    updateContent(contentKey, (content) => ({ ...content, elements: content.elements.map((element) => element.key === elementKey ? updater(element) : element) }));
  };
  const moveContent = (index: number, direction: -1 | 1) => {
    setContents((current) => {
      const next = [...current]; const target = index + direction;
      if (target < 0 || target >= next.length) return current;
      [next[index], next[target]] = [next[target], next[index]];
      return normalizeContents(next);
    });
  };
  const moveElement = (contentKey: string, index: number, direction: -1 | 1) => {
    updateContent(contentKey, (content) => {
      const elements = [...content.elements]; const target = index + direction;
      if (target < 0 || target >= elements.length) return content;
      [elements[index], elements[target]] = [elements[target], elements[index]];
      return { ...content, elements };
    });
  };

  const renderContentsStep = () => (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      <Alert type="info" showIcon message="第一步：增加考核内容。考核内容是评分表中左侧合并单元格的分组标题。" />
      {contents.map((content, index) => (
        <Card key={content.key} size="small" title={<Space>考核内容 {index + 1}<Typography.Text type="secondary">总分：{contentAllocatedTotal(content).toFixed(2).replace(/\.00$/, '')} 分</Typography.Text></Space>} extra={(
          <Space size={0}>
            <Button type="text" size="small" icon={<ArrowUpOutlined />} disabled={index === 0} onClick={() => moveContent(index, -1)} />
            <Button type="text" size="small" icon={<ArrowDownOutlined />} disabled={index === contents.length - 1} onClick={() => moveContent(index, 1)} />
            <Button type="text" danger size="small" icon={<DeleteOutlined />} disabled={contents.length <= 1} onClick={() => setContents((current) => normalizeContents(current.filter((item) => item.key !== content.key)))} />
          </Space>
        )}>
          <RichTextEditor minHeight={120} value={content.contentHtml} onChange={(value) => updateContent(content.key, (item) => ({ ...item, contentHtml: value }))} placeholder="请输入考核内容" />
        </Card>
      ))}
      <Button type="dashed" block icon={<PlusOutlined />} onClick={() => setContents((current) => [...current, emptyContent(current.length + 1)])}>增加考核内容</Button>
    </Space>
  );

  const renderElementsStep = () => (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      <Alert type="info" showIcon message="第二步：录入评分要素。未配分要素保留在预览表中，但不计入试题总分。" />
      {contents.map((content, contentIndex) => (
        <Card key={content.key} size="small" title={<Space>考核内容 {contentIndex + 1}<Typography.Text type="secondary">总分：{contentAllocatedTotal(content).toFixed(2).replace(/\.00$/, '')} 分</Typography.Text><RichTextPreview value={content.contentHtml} className="practical-question-editor__inline-preview" /></Space>}>
          <Space direction="vertical" style={{ width: '100%' }}>
            {content.elements.map((element, index) => (
              <Card key={element.key} type="inner" size="small" title={`评分要素 ${index + 1}`} extra={(
                <Space size={0}>
                  <Button type="text" size="small" icon={<ArrowUpOutlined />} disabled={index === 0} onClick={() => moveElement(content.key, index, -1)} />
                  <Button type="text" size="small" icon={<ArrowDownOutlined />} disabled={index === content.elements.length - 1} onClick={() => moveElement(content.key, index, 1)} />
                  <Button type="text" danger size="small" icon={<DeleteOutlined />} disabled={content.elements.length <= 1} onClick={() => updateContent(content.key, (item) => ({ ...item, elements: item.elements.filter((candidate) => candidate.key !== element.key) }))} />
                </Space>
              )}>
                <RichTextEditor minHeight={100} value={element.elementHtml} onChange={(value) => updateElement(content.key, element.key, (item) => ({ ...item, elementHtml: value }))} placeholder="请输入评分要素" />
                <Row gutter={16} align="middle" style={{ marginTop: 12 }}>
                  <Col flex="none"><Checkbox checked={element.allocationEnabled} onChange={(event) => updateElement(content.key, element.key, (item) => ({ ...item, allocationEnabled: event.target.checked, maxScore: event.target.checked ? item.maxScore : undefined, maxDeduction: event.target.checked ? item.maxDeduction : undefined, deductionStep: event.target.checked ? item.deductionStep : undefined }))}>是否配分</Checkbox></Col>
                  {element.allocationEnabled && <Col flex="220px"><Space>最大配分<InputNumber min={0.01} max={100} precision={2} value={element.maxScore} onChange={(value) => updateElement(content.key, element.key, (item) => ({ ...item, maxScore: value == null ? undefined : Number(value) }))} />分</Space></Col>}
                </Row>
              </Card>
            ))}
            <Button type="dashed" icon={<PlusOutlined />} onClick={() => updateContent(content.key, (item) => ({ ...item, elements: [...item.elements, emptyElement(item.elements.length + 1)] }))}>增加评分要素</Button>
          </Space>
        </Card>
      ))}
    </Space>
  );

  const renderStandardsStep = () => (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      <Alert type="info" showIcon message={currentValues.scoringMethod === 'deductive' ? '第三步：录入评分标准、最大扣分和步长。最大扣分必须不超过该要素配分，且能被步长整除。' : '第三步：录入评分标准。当前为加分制，不需要填写最大扣分和步长。'} />
      {contents.map((content, contentIndex) => (
        <Card key={content.key} size="small" title={`考核内容 ${contentIndex + 1}`}>
          {content.elements.map((element, index) => (
            <Card key={element.key} type="inner" size="small" title={`评分要素 ${index + 1}`} style={{ marginBottom: 10 }}>
              <RichTextEditor minHeight={110} value={element.standardHtml} onChange={(value) => updateElement(content.key, element.key, (item) => ({ ...item, standardHtml: value }))} placeholder="请输入评分标准" />
              {element.allocationEnabled && currentValues.scoringMethod === 'deductive' && (
                <Row gutter={16} style={{ marginTop: 12 }}>
                  <Col><Space>最大扣分<InputNumber min={0} max={element.maxScore || 100} precision={2} value={element.maxDeduction} onChange={(value) => updateElement(content.key, element.key, (item) => ({ ...item, maxDeduction: value == null ? undefined : Number(value) }))} />分</Space></Col>
                  <Col><Space>步长<InputNumber min={0.01} max={element.maxDeduction || element.maxScore || 100} precision={2} value={element.deductionStep} onChange={(value) => updateElement(content.key, element.key, (item) => ({ ...item, deductionStep: value == null ? undefined : Number(value) }))} />分</Space></Col>
                </Row>
              )}
            </Card>
          ))}
        </Card>
      ))}
    </Space>
  );

  const tabItems = [
    { key: 'requirements', label: '考核要求', children: <RichTextEditor minHeight={240} value={assessmentRequirementsHtml} onChange={setAssessmentRequirementsHtml} placeholder="请输入考核要求" /> },
    { key: 'room', label: '考场准备', children: <RichTextEditor minHeight={240} value={examRoomPrepareHtml} onChange={setExamRoomPrepareHtml} placeholder="请输入考场准备" /> },
    { key: 'candidate', label: '考生准备', children: <RichTextEditor minHeight={240} value={candidatePrepareHtml} onChange={setCandidatePrepareHtml} placeholder="请输入考生准备" /> },
    { key: 'scoring', label: '评分标准表', children: <div className="practical-question-editor__scoring-tab"><Space direction="vertical" style={{ width: '100%' }} size={14}><Space wrap><Steps current={step} size="small" items={[{ title: '考核内容' }, { title: '评分要素' }, { title: '评分标准' }]} /><Button icon={<EyeOutlined />} onClick={() => setPreviewOpen(true)}>预览评分表</Button></Space>{step === 0 ? renderContentsStep() : step === 1 ? renderElementsStep() : renderStandardsStep()}<div className="practical-question-editor__step-actions"><Button disabled={step === 0} onClick={() => setStep((current) => current - 1)}>上一步</Button><Button type="primary" disabled={step === 2} onClick={() => setStep((current) => current + 1)}>下一步</Button></div></Space></div> },
    { key: 'negative', label: '否定项', children: <RichTextEditor minHeight={240} value={negativeItemsHtml} onChange={setNegativeItemsHtml} placeholder="请输入否定项" /> },
    { key: 'materials', label: '试题材料', children: <RichTextEditor minHeight={240} value={questionMaterialsHtml} onChange={setQuestionMaterialsHtml} placeholder="请输入试题材料" /> },
  ];

  return (
    <>
      <Modal
        title={editing ? '编辑实操试题' : '新增实操试题'} open={open} onCancel={onClose}
        onOk={() => form.submit()} confirmLoading={saveMutation.isPending} destroyOnClose width="min(1280px, 96vw)"
        styles={{ body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto' } }}
      >
        {detailQuery.isLoading && editing ? <Typography.Text>正在加载试题详情…</Typography.Text> : (
          <Form form={form} layout="vertical" onFinish={(formValues) => saveMutation.mutate(formValues)}>
            <Row gutter={16}>
              <Col xs={24} lg={8}><Form.Item name="eventWorkTypeId" label="工种" rules={[{ required: true }]}><Select disabled options={workTypes.map((item) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) }))} /></Form.Item></Col>
              <Col xs={24} lg={6}><Form.Item name="eventWorkTypeLevelId" label="等级" rules={[{ required: true }]}><Select disabled options={levels.map((item) => ({ value: String(item.id), label: item.levelName }))} /></Form.Item></Col>
              <Col xs={24} lg={10}><Form.Item name="stem" label="试题名称" rules={[{ required: true, whitespace: true, message: '请输入试题名称' }]}><Input maxLength={255} placeholder="请输入试题名称" /></Form.Item></Col>
              {blueprintId && <Col xs={24} lg={10}><Form.Item name="blueprintNodeId" label="考评点" rules={[{ required: !editing, message: '请选择末级考评点' }]}><Select allowClear showSearch optionFilterProp="label" options={blueprintNodes} placeholder={editing ? '如需绑定可选择末级考评点' : '选择细目表末级考评点'} /></Form.Item></Col>}
            </Row>
            <Row gutter={16}>
              <Col xs={24} sm={12} lg={6}><Form.Item name="durationMinutes" label="考试时间（分钟）" rules={[{ required: true, type: 'number', min: 1, message: '请输入正整数分钟' }, { validator: (_rule, value) => Number.isInteger(Number(value)) && Number(value) > 0 ? Promise.resolve() : Promise.reject(new Error('请输入正整数分钟')) }]}><InputNumber precision={0} min={1} style={{ width: '100%' }} /></Form.Item></Col>
              <Col xs={24} sm={12} lg={6}><Form.Item name="scoreDifference" label="评分差值" dependencies={['totalScore']} rules={[{ required: true, type: 'number', min: 0, message: '评分差值不能小于0' }, { validator: (_rule, value) => { const total = Number(form.getFieldValue('totalScore')); return Number(value) >= 0 && Number(value) <= total ? Promise.resolve() : Promise.reject(new Error('评分差值不能超过试题总分')); } }]}><InputNumber min={0} precision={2} style={{ width: '100%' }} /></Form.Item></Col>
              <Col xs={24} sm={12} lg={6}><Form.Item name="totalScore" label="试题总分" rules={[{ required: true, type: 'number', min: 0.01, max: 100, message: '总分必须在0到100之间' }]}><InputNumber min={0.01} max={100} precision={2} style={{ width: '100%' }} /></Form.Item></Col>
              <Col xs={24} sm={12} lg={6}><Form.Item name="scoringMethod" label="评分方式" rules={[{ required: true }]}><Radio.Group options={[{ label: '加分制', value: 'additive' }, { label: '扣分制', value: 'deductive' }]} onChange={(event) => { if (event.target.value === 'additive') setContents((current) => current.map((content) => ({ ...content, elements: content.elements.map((element) => ({ ...element, maxDeduction: undefined, deductionStep: undefined })) }))); }} /></Form.Item></Col>
            </Row>
            <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
            <Form.Item name="status" label="状态" hidden><Select options={[{ value: 'enabled', label: '启用' }, { value: 'disabled', label: '停用' }]} /></Form.Item>
          </Form>
        )}
      </Modal>
      <Modal title="查看评分表" open={previewOpen} onCancel={() => setPreviewOpen(false)} footer={null} width="min(1500px, 98vw)" styles={{ body: { maxHeight: 'calc(100vh - 150px)', overflowY: 'auto' } }}>
        <Typography.Title level={5}>{currentValues.stem || '实操试题评分表'}</Typography.Title>
        <PracticalScoreTablePreview contents={contents} values={currentValues} />
      </Modal>
    </>
  );
}

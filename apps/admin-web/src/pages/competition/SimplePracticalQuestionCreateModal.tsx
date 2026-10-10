import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Row,
  Space,
  Typography,
  message,
} from 'antd';
import { useEffect } from 'react';
import { createCompetitionSimplePracticalQuestion } from '../../api/competitionQuestion';
import type { CompetitionId } from '../../types/competition';
import type { CompetitionQuestionRequest, CompetitionPracticalScoringMethod } from '../../types/competitionQuestion';

interface Props {
  open: boolean;
  eventId: CompetitionId;
  workTypeId: CompetitionId;
  levelId: CompetitionId;
  onClose: () => void;
  onSaved: () => void;
}

interface ScoreRowValues {
  assessmentContent?: string;
  itemName?: string;
  standard?: string;
  stepText?: string;
  score?: number;
}

interface FormValues {
  projectName: string;
  scoringMethod: CompetitionPracticalScoringMethod;
  items: ScoreRowValues[];
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/\r?\n/g, '<br/>');
}

function toParagraph(value: string) {
  return `<p>${escapeHtml(value.trim())}</p>`;
}

function formatScore(value: number) {
  return Number(value.toFixed(2)).toString();
}

export function SimplePracticalQuestionCreateModal({ open, eventId, workTypeId, levelId, onClose, onSaved }: Props) {
  const [form] = Form.useForm<FormValues>();
  const scoringMethod = Form.useWatch('scoringMethod', form) || 'deductive';
  const items = Form.useWatch('items', form) ?? [];
  const totalScore = items.reduce((total, item) => total + Number(item?.score || 0), 0);

  useEffect(() => {
    if (open) {
      form.setFieldsValue({
        projectName: '',
        scoringMethod: 'deductive',
        items: [{ assessmentContent: '', itemName: '', standard: '', stepText: '', score: undefined }],
      });
    }
  }, [form, open]);

  const saveMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const projectName = values.projectName.trim();
      const method = values.scoringMethod;
      const validRows = values.items.map((row) => ({
        assessmentContent: row.assessmentContent?.trim() || '',
        itemName: row.itemName?.trim() || '',
        standard: row.standard?.trim() || '',
        stepText: row.stepText?.trim() || '',
        score: Number(row.score),
      }));
      if (!projectName) throw new Error('请输入考核项目');
      if (validRows.length === 0) throw new Error('请至少添加一个评分项');
      if (validRows.some((row) => !row.assessmentContent || !row.itemName || !row.standard || !Number.isFinite(row.score) || row.score <= 0)) {
        throw new Error('请填写每行的考核内容、评分项、评分标准和正数分值');
      }
      if (method === 'deductive' && validRows.some((row) => {
        const step = Number(row.stepText);
        return !row.stepText || !Number.isFinite(step) || step <= 0
          || Math.abs((row.score / step) - Math.round(row.score / step)) > 1e-8;
      })) {
        throw new Error('扣分制的步骤必须填写正数扣分步长，且能整除对应评分项分值');
      }
      const itemTotal = validRows.reduce((total, row) => total + row.score, 0);
      if (itemTotal <= 0 || itemTotal > 100) throw new Error('项目总分必须大于0且不超过100分');

      const contents = new Map<string, typeof validRows>();
      validRows.forEach((row) => {
        const current = contents.get(row.assessmentContent) ?? [];
        current.push(row);
        contents.set(row.assessmentContent, current);
      });
      const body: CompetitionQuestionRequest = {
        eventWorkTypeId: workTypeId,
        eventWorkTypeLevelId: levelId,
        contentType: 'practical',
        questionType: 'practical',
        stem: projectName,
        status: 'enabled',
        practicalDetail: {
          durationMinutes: 60,
          scoreDifference: Math.min(itemTotal, 10),
          totalScore: itemTotal,
          scoringMethod: method,
          assessmentContents: Array.from(contents.entries()).map(([content, rows], contentIndex) => ({
            sortOrder: contentIndex + 1,
            contentHtml: toParagraph(content),
            elements: rows.map((row, rowIndex) => ({
              elementOrder: rowIndex + 1,
              elementHtml: toParagraph(row.stepText),
              itemName: row.itemName,
              allocationEnabled: true,
              maxScore: row.score,
              standardHtml: toParagraph(row.standard),
              maxDeduction: method === 'deductive' ? row.score : undefined,
              deductionStep: method === 'deductive' ? Number(row.stepText) : undefined,
            })),
          })),
        },
      };
      return createCompetitionSimplePracticalQuestion(eventId, body);
    },
    onSuccess: () => {
      message.success('实操评分项已新增');
      form.resetFields();
      onSaved();
    },
    onError: (error: Error) => message.error(error.message || '新增失败'),
  });

  const close = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title="新增实操评分项"
      open={open}
      onCancel={close}
      onOk={() => form.submit()}
      confirmLoading={saveMutation.isPending}
      destroyOnClose
      width="min(1280px, 96vw)"
      styles={{ body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto' } }}
    >
      <Form form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="一次新增一个考核项目，可添加多行评分项；相同考核内容会自动合并。"
        />
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item name="projectName" label="考核项目" rules={[{ required: true, whitespace: true, message: '请输入考核项目' }]}>
              <Input maxLength={255} placeholder="请输入考核项目名称" />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="scoringMethod" label="计分方式" rules={[{ required: true, message: '请选择计分方式' }]}>
              <Radio.Group options={[{ label: '加分制', value: 'additive' }, { label: '扣分制', value: 'deductive' }]} />
            </Form.Item>
          </Col>
        </Row>

        <Typography.Text strong>评分项明细</Typography.Text>
        <Form.List name="items">
          {(fields, { add, remove }) => (
            <Space direction="vertical" size={8} style={{ display: 'flex', marginTop: 8 }}>
              <Row gutter={8} style={{ paddingInline: 8 }}>
                <Col span={5}><Typography.Text type="secondary">考核内容</Typography.Text></Col>
                <Col span={4}><Typography.Text type="secondary">评分项</Typography.Text></Col>
                <Col span={5}><Typography.Text type="secondary">评分标准</Typography.Text></Col>
                <Col span={4}><Typography.Text type="secondary">步骤</Typography.Text></Col>
                <Col span={3}><Typography.Text type="secondary">分值</Typography.Text></Col>
                <Col span={3}><Typography.Text type="secondary">操作</Typography.Text></Col>
              </Row>
              {fields.map((field, index) => (
                <Card key={field.key} size="small" styles={{ body: { padding: 8 } }}>
                  <Row gutter={8} align="top">
                    <Col xs={24} md={5}>
                      <Form.Item name={[field.name, 'assessmentContent']} rules={[{ required: true, whitespace: true, message: '请填写考核内容' }]} style={{ marginBottom: 0 }}>
                        <Input.TextArea autoSize={{ minRows: 1 }} placeholder="考核内容" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={4}>
                      <Form.Item name={[field.name, 'itemName']} rules={[{ required: true, whitespace: true, message: '请填写评分项' }]} style={{ marginBottom: 0 }}>
                        <Input.TextArea autoSize={{ minRows: 1 }} maxLength={255} placeholder="评分项" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={5}>
                      <Form.Item name={[field.name, 'standard']} rules={[{ required: true, whitespace: true, message: '请填写评分标准' }]} style={{ marginBottom: 0 }}>
                        <Input.TextArea autoSize={{ minRows: 1 }} placeholder="评分标准" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={4}>
                      <Form.Item
                        name={[field.name, 'stepText']}
                        rules={scoringMethod === 'deductive' ? [{ required: true, whitespace: true, message: '请填写扣分步长' }] : []}
                        style={{ marginBottom: 0 }}
                      >
                        <Input placeholder={scoringMethod === 'deductive' ? '正数扣分步长' : '步骤说明（选填）'} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={3}>
                      <Form.Item name={[field.name, 'score']} rules={[{ required: true, type: 'number', min: 0.01, message: '请输入正数分值' }]} style={{ marginBottom: 0 }}>
                        <InputNumber min={0.01} max={100} precision={2} style={{ width: '100%' }} placeholder="分值" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={3}>
                      <Button danger type="text" icon={<DeleteOutlined />} disabled={fields.length <= 1} onClick={() => remove(field.name)}>
                        删除
                      </Button>
                    </Col>
                  </Row>
                  <Typography.Text type="secondary">第 {index + 1} 项</Typography.Text>
                </Card>
              ))}
              <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({ assessmentContent: '', itemName: '', standard: '', stepText: '', score: undefined })}>
                添加评分项
              </Button>
              <div style={{ textAlign: 'right' }}>
                <Typography.Text strong>项目总分：{formatScore(totalScore)} 分</Typography.Text>
              </div>
            </Space>
          )}
        </Form.List>
      </Form>
    </Modal>
  );
}

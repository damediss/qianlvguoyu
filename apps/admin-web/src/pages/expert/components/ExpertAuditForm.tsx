// 专家端 3 页面共享的"审核录入"表单
// 根据 taskType 切换字段:
//   exam      - 8 选项 + 答案 + 重点
//   examshicao - 评分标准 JSON + 考场准备 + 考生准备
//   examsub   - 答案 + 是否计算题 + 图片(可选)
//
// 内部维护 item index,逐题审核;全部提交后回调 onComplete()

import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Form,
  Input,
  Radio,
  Space,
  Tag,
  message,
} from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import type { TaskAuditType } from '../../../types/exam';
import type { ExpertAuditFormItem } from '../../../types/expert';

const OPTION_KEYS = ['examA', 'examB', 'examC', 'examD', 'examE', 'examF', 'examG', 'examH'] as const;
const OPTION_LABELS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];

export interface ExpertAuditFormProps {
  taskType: TaskAuditType;
  items: ExpertAuditFormItem[];
  // 提交某道题,返回 Promise<isdone>: 1=全部完成,0=还有题
  onSubmit: (item: ExpertAuditFormItem, values: Record<string, unknown>) => Promise<{ isdone: number; count: number; total: number }>;
  onComplete?: () => void;
}

// 把 values 转成 ExpertTaskSubmitItem 格式
function buildSubmitValues(
  taskType: TaskAuditType,
  item: ExpertAuditFormItem,
  raw: Record<string, unknown>,
): Record<string, unknown> {
  switch (taskType) {
    case 'exam':
      return {
        examId: item.id,
        examContent: raw.examContent,
        examAnswer: raw.examAnswer,
        isblod: raw.isblod ? 1 : 0,
        examA: raw.examA,
        examB: raw.examB,
        examC: raw.examC,
        examD: raw.examD,
        examE: raw.examE,
        examF: raw.examF,
        examG: raw.examG,
        examH: raw.examH,
      };
    case 'examshicao':
      return {
        examId: item.id,
        content: raw.content,
        pingfen: raw.pingfen,
        kaochang: raw.kaochang,
        kaosheng: raw.kaosheng,
      };
    case 'examsub':
      return {
        examId: item.id,
        content: raw.content,
        answer: raw.answer,
        isJisuan: raw.isJisuan ? 1 : 0,
        pic: raw.pic,
      };
  }
}

export function ExpertAuditForm({ taskType, items, onSubmit, onComplete }: ExpertAuditFormProps) {
  const [idx, setIdx] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [submittedIds, setSubmittedIds] = useState<Set<number>>(new Set());
  const [form] = Form.useForm();
  const item = items[idx];

  // 切到下一题时重置表单
  useEffect(() => {
    if (item) {
      const init: Record<string, unknown> = {};
      if (taskType === 'exam') {
        init.examContent = item.examContent;
        init.examAnswer = item.examAnswer;
        init.isblod = item.isblod === 1;
        OPTION_KEYS.forEach((k) => {
          init[k] = (item as any)[k];
        });
      } else if (taskType === 'examshicao') {
        init.content = item.content;
        init.pingfen = item.pingfen;
        init.kaochang = item.kaochang;
        init.kaosheng = item.kaosheng;
      } else if (taskType === 'examsub') {
        init.content = item.content;
        init.answer = item.answer;
        init.isJisuan = item.isJisuan === 1;
        init.pic = item.pic;
      }
      form.setFieldsValue(init);
    }
  }, [idx, item, taskType, form]);

  if (!item) {
    return <Alert type="info" message="暂无审核数据" showIcon />;
  }

  const total = items.length;
  const done = submittedIds.size;

  const handleSubmit = async () => {
    if (submitting) return;
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload = buildSubmitValues(taskType, item, values);
      const res = await onSubmit(item, payload);
      setSubmitting(false);
      setSubmittedIds((prev) => new Set(prev).add(item.id));
      if (res.isdone === 1) {
        message.success('审核完成');
        onComplete?.();
        return;
      }
      message.success(`已提交 ${res.count}/${res.total}`);
      if (idx < items.length - 1) {
        setIdx(idx + 1);
      }
    } catch (e: any) {
      setSubmitting(false);
      if (e?.errorFields) {
        message.warning('请填写必填项');
      } else {
        message.error(e?.message || '提交失败');
      }
    }
  };

  const goPrev = () => idx > 0 && setIdx(idx - 1);
  const goNext = () => idx < items.length - 1 && setIdx(idx + 1);

  return (
    <div>
      {/* 进度条 */}
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <Tag color="blue">第 {idx + 1} / {total} 题</Tag>
          <Tag color="green">已完成 {done}</Tag>
        </Space>
        <Space>
          <Button size="small" icon={<LeftOutlined />} disabled={idx === 0} onClick={goPrev}>上一题</Button>
          <Button size="small" icon={<RightOutlined />} disabled={idx >= items.length - 1} onClick={goNext}>下一题</Button>
        </Space>
      </div>

      <Form form={form} layout="vertical" preserve={false}>
        {taskType === 'exam' && (
          <>
            <Form.Item label="题目类型">
              <Input value={item.examType} disabled />
            </Form.Item>
            <Form.Item label="题干" name="examContent" rules={[{ required: true }]}>
              <Input.TextArea rows={3} />
            </Form.Item>
            {/* 8 个选项,只渲染非空的 */}
            {OPTION_KEYS.map((k, i) => {
              const label = OPTION_LABELS[i];
              if (!(item as any)[k]) return null;
              return (
                <Form.Item key={k} label={`选项 ${label}`} name={k}>
                  <Input />
                </Form.Item>
              );
            })}
            <Form.Item label="参考答案" name="examAnswer" rules={[{ required: true }]}>
              {item.examType === '判断题' ? (
                <Radio.Group
                  options={[
                    { label: '对 (A)', value: 'A' },
                    { label: '错 (B)', value: 'B' },
                  ]}
                />
              ) : (
                <Input />
              )}
            </Form.Item>
            <Form.Item label="是否重点审核" name="isblod" valuePropName="checked">
              <Radio.Group
                options={[
                  { label: '是', value: true },
                  { label: '否', value: false },
                ]}
              />
            </Form.Item>
          </>
        )}

        {taskType === 'examshicao' && (
          <>
            <Form.Item label="考评点">
              <Input value={item.jianding} disabled />
            </Form.Item>
            <Form.Item label="题目内容" name="content" rules={[{ required: true }]}>
              <Input.TextArea rows={3} />
            </Form.Item>
            <Form.Item
              label="评分标准 (JSON 格式)"
              name="pingfen"
              rules={[{ required: true, message: '请填写评分标准' }]}
            >
              <Input.TextArea rows={6} placeholder='例如: {"分值": 100, "评分项": [{"name": "操作规范", "score": 30}]}' />
            </Form.Item>
            <Form.Item label="考场准备" name="kaochang">
              <Input.TextArea rows={2} />
            </Form.Item>
            <Form.Item label="考生准备" name="kaosheng">
              <Input.TextArea rows={2} />
            </Form.Item>
          </>
        )}

        {taskType === 'examsub' && (
          <>
            <Form.Item label="知识点">
              <Input value={item.zhishidian} disabled />
            </Form.Item>
            <Form.Item label="题目内容" name="content" rules={[{ required: true }]}>
              <Input.TextArea rows={3} />
            </Form.Item>
            <Form.Item label="参考答案" name="answer" rules={[{ required: true }]}>
              <Input.TextArea rows={5} />
            </Form.Item>
            <Form.Item label="是否计算题" name="isJisuan" valuePropName="checked">
              <Radio.Group
                options={[
                  { label: '是', value: true },
                  { label: '否', value: false },
                ]}
              />
            </Form.Item>
            <Form.Item label="图片 (可选)" name="pic">
              <Input placeholder="图片 URL,可留空" />
            </Form.Item>
          </>
        )}

        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Button
            type="primary"
            loading={submitting}
            onClick={handleSubmit}
            disabled={submittedIds.has(item.id)}
          >
            {submittedIds.has(item.id) ? '已提交' : '提交'}
          </Button>
        </div>
      </Form>
    </div>
  );
}

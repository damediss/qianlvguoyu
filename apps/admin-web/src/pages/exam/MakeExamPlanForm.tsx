import type { ReactNode } from 'react';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Switch,
  Typography,
} from 'antd';
import type { FormInstance } from 'antd/es/form';
import { useQuery } from '@tanstack/react-query';
import { listKnowledgeRates } from '../../api/exam';
import type { ExamMakeExamPlanCreateReq, ExamKnowledgeRate } from '../../types/exam';
import { DIFFICULTY_MAP } from '../../types/exam';

const { Title } = Typography;

export const MAKE_EXAM_PLAN_QUESTION_TYPES = [
  { label: '单选题', value: 'single' },
  { label: '多选题', value: 'multi' },
  { label: '判断题', value: 'judge' },
  { label: '填空题', value: 'fill' },
  { label: '简答题', value: 'essay' },
  { label: '论述题', value: 'explain' },
  { label: '计算题', value: 'cal' },
  { label: '简答题(答)', value: 'answer' },
  { label: '分析题', value: 'analysis' },
  { label: '简单填空', value: 'simple_fill' },
];

/** 难度下拉选项：只显示中文档位，值域 0~4（与 DIFFICULTY_MAP 一致，后端按数值区间抽题） */
export const MAKE_EXAM_PLAN_DIFFICULTY_OPTIONS = [0, 1, 2, 3, 4]
  .map((value) => ({ label: DIFFICULTY_MAP[value], value }));

export type MakeExamPlanCourseOption = { id: number; title: string };

export type MakeExamPlanFormProps = {
  form: FormInstance;
  courses: MakeExamPlanCourseOption[];
  /** 锁定考试类型（跟班模拟考仅理论=0） */
  examTypeLocked?: number;
  /** 科目下拉空态提示 */
  coursePlaceholder?: string;
  /** 表单底部扩展区（如跟班挂接选项） */
  children?: ReactNode;
};

/** 将表单值转为与题库「添加组卷」一致的创建请求体 */
export function buildMakeExamPlanCreatePayload(values: Record<string, unknown>): ExamMakeExamPlanCreateReq {
  const isAll = values.isAll as number | undefined;
  return {
    title: String(values.title ?? ''),
    courseId: Number(values.courseId),
    examType: Number(values.examType),
    rateId: (values.rateId as number | undefined) || undefined,
    type: values.type as number | undefined,
    time: values.time as number | undefined,
    isComputer: values.isComputer ? 1 : 0,
    isPrint: values.isPrint ? 1 : 0,
    openRate: values.openRate as number | undefined,
    totalPoint: values.totalPoint as number | undefined,
    passPoint: values.passPoint as number | undefined,
    isAll,
    comments: (values.comments as string | undefined) || undefined,
    detailList: isAll === 1 ? undefined : (values.detailList as ExamMakeExamPlanCreateReq['detailList']),
  };
}

/**
 * 题库「组卷计划」添加/编辑表单（与 MakeExamPlanPage 弹窗字段一致）。
 * 题库页与跟班「添加组卷」共用，禁止另写第二套字段。
 */
export function MakeExamPlanForm({
  form,
  courses,
  examTypeLocked,
  coursePlaceholder,
  children,
}: MakeExamPlanFormProps) {
  const watchedCourseId = Form.useWatch('courseId', form) as number | undefined;

  const rateQuery = useQuery({
    queryKey: ['knowledge-rates-list', watchedCourseId],
    queryFn: () => listKnowledgeRates(watchedCourseId),
    enabled: watchedCourseId != null && watchedCourseId > 0,
  });
  const rates = (rateQuery.data || []) as ExamKnowledgeRate[];

  return (
    <Form
      form={form}
      layout="horizontal"
      labelCol={{ flex: '80px' }}
      wrapperCol={{ flex: 1 }}
      labelAlign="right"
    >
      <Form.Item name="title" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
        <Input />
      </Form.Item>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
        <Form.Item name="courseId" label="科目" rules={[{ required: true, message: '请选择科目' }]}>
          <Select
            placeholder={coursePlaceholder || '选择科目'}
            options={courses.map((c) => ({ label: c.title, value: c.id }))}
            showSearch
            optionFilterProp="label"
          />
        </Form.Item>
        <Form.Item name="examType" label="考试类型" rules={[{ required: true }]}>
          <Select
            disabled={examTypeLocked != null}
            options={[
              { label: '理论', value: 0 },
              { label: '实操', value: 1 },
            ]}
          />
        </Form.Item>
        <Form.Item name="time" label="时长(分钟)">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="rateId" label="知识比重">
          <Select
            allowClear
            placeholder="选择比重(可选)"
            options={rates.map((r) => ({ label: r.title, value: r.id }))}
          />
        </Form.Item>
        <Form.Item name="totalPoint" label="总分">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="passPoint" label="及格分">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="isComputer" label="机考" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="isPrint" label="打印" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="isAll" label="全选模式">
          <Select
            options={[
              { label: '全选(自动统计)', value: 1 },
              { label: '自定义', value: 0 },
            ]}
          />
        </Form.Item>
      </div>
      <Form.Item name="comments" label="备注">
        <Input.TextArea rows={2} />
      </Form.Item>

      <Form.Item noStyle shouldUpdate={(prev, cur) => prev.isAll !== cur.isAll}>
        {({ getFieldValue }) =>
          getFieldValue('isAll') !== 1 ? (
            <>
              <Title level={5}>题型配置</Title>
              <Form.List name="detailList">
                {(fields, { add, remove }) => (
                  <>
                    {fields.map((field) => (
                      <Card
                        key={field.key}
                        size="small"
                        style={{ marginBottom: 8 }}
                        extra={
                          <MinusCircleOutlined
                            onClick={() => remove(field.name)}
                            style={{ color: '#ff4d4f' }}
                          />
                        }
                      >
                        <div
                          style={{
                            display: 'grid',
                            gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr 1fr 1fr',
                            gap: 12,
                            alignItems: 'end',
                          }}
                        >
                          <Form.Item
                            name={[field.name, 'type']}
                            label="题型"
                            rules={[{ required: true }]}
                            style={{ marginBottom: 0 }}
                          >
                            <Select options={MAKE_EXAM_PLAN_QUESTION_TYPES} />
                          </Form.Item>
                          <Form.Item
                            name={[field.name, 'num']}
                            label="数量"
                            rules={[{ required: true }]}
                            style={{ marginBottom: 0 }}
                          >
                            <InputNumber min={1} style={{ width: '100%' }} />
                          </Form.Item>
                          <Form.Item
                            name={[field.name, 'point']}
                            label="每题分值"
                            style={{ marginBottom: 0 }}
                          >
                            <InputNumber min={0} style={{ width: '100%' }} />
                          </Form.Item>
                          <Form.Item
                            name={[field.name, 'difficultyStart']}
                            label="难度起"
                            style={{ marginBottom: 0 }}
                          >
                            <Select
                              options={MAKE_EXAM_PLAN_DIFFICULTY_OPTIONS}
                              placeholder="不限"
                              allowClear
                            />
                          </Form.Item>
                          <Form.Item
                            name={[field.name, 'difficultyEnd']}
                            label="难度止"
                            style={{ marginBottom: 0 }}
                          >
                            <Select
                              options={MAKE_EXAM_PLAN_DIFFICULTY_OPTIONS}
                              placeholder="不限"
                              allowClear
                            />
                          </Form.Item>
                          <Form.Item
                            name={[field.name, 'startPoint']}
                            label="分值起"
                            style={{ marginBottom: 0 }}
                          >
                            <InputNumber min={0} style={{ width: '100%' }} />
                          </Form.Item>
                          <Form.Item
                            name={[field.name, 'endPoint']}
                            label="分值止"
                            style={{ marginBottom: 0 }}
                          >
                            <InputNumber min={0} style={{ width: '100%' }} />
                          </Form.Item>
                        </div>
                      </Card>
                    ))}
                    <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                      添加题型
                    </Button>
                  </>
                )}
              </Form.List>
            </>
          ) : null
        }
      </Form.Item>
      {children}
    </Form>
  );
}

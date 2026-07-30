import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  List,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
  Popconfirm,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import {
  pageMakeExamPlans,
  createMakeExamPlan,
  updateMakeExamPlan,
  deleteMakeExamPlan,
  listKnowledgeRates,
} from '../../api/exam';
import { pageCourses } from '../../api/exam';
import type {
  ExamMakeExamPlan,
  ExamMakeExamPlanCreateReq,
  ExamMakeExamPlanDetailReq,
  ExamKnowledgeRate,
  ExamCourse,
} from '../../types/exam';

const { Title } = Typography;

const EXAM_TYPE_MAP: Record<number, string> = { 0: '理论', 1: '实操' };

const QUESTION_TYPES = [
  { label: '单选题', value: 'single' },
  { label: '多选题', value: 'multiple' },
  { label: '判断题', value: 'judge' },
  { label: '填空题', value: 'fill' },
  { label: '简答题', value: 'essay' },
  { label: '论述题', value: 'explain' },
  { label: '计算题', value: 'cal' },
  { label: '简答题(答)', value: 'answer' },
  { label: '分析题', value: 'analysis' },
  { label: '简单填空', value: 'simple_fill' },
];

export function MakeExamPlanPage() {
  const queryClient = useQueryClient();
  const [selectedCourseId, setSelectedCourseId] = useState<number | undefined>();
  const [examType, setExamType] = useState<number>(0);
  const [query, setQuery] = useState({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  // 科目列表
  const courseQuery = useQuery({
    queryKey: ['exam-courses-all'],
    queryFn: () => pageCourses({ page: 1, pageSize: 200 }),
  });

  // 知识比重列表(下拉用)
  const rateQuery = useQuery({
    queryKey: ['knowledge-rates-list', selectedCourseId],
    queryFn: () => listKnowledgeRates(selectedCourseId),
    enabled: !!selectedCourseId,
  });

  // 组卷计划分页
  const listQuery = useQuery({
    queryKey: ['make-exam-plans', { ...query, courseId: selectedCourseId, examType }],
    queryFn: () =>
      pageMakeExamPlans({ ...query, courseId: selectedCourseId, examType }),
  });

  const createMutation = useMutation({
    mutationFn: createMakeExamPlan,
    onSuccess: () => {
      message.success('组卷计划已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['make-exam-plans'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExamMakeExamPlanCreateReq }) =>
      updateMakeExamPlan(id, payload),
    onSuccess: () => {
      message.success('组卷计划已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['make-exam-plans'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteMakeExamPlan,
    onSuccess: () => {
      message.success('组卷计划已删除');
      queryClient.invalidateQueries({ queryKey: ['make-exam-plans'] });
    },
  });

  const handleAdd = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ examType, courseId: selectedCourseId, isAll: 0, isComputer: 1, isPrint: 0 });
    setModalOpen(true);
  }, [form, examType, selectedCourseId]);

  const handleEdit = useCallback(
    (record: ExamMakeExamPlan) => {
      setEditingId(record.id);
      form.setFieldsValue({
        ...record,
        isComputer: record.isComputer === 1,
        isPrint: record.isPrint === 1,
        detailList: record.planDetailList?.map((d) => ({
          type: d.type,
          num: d.num,
          point: d.point,
          startPoint: d.startPoint,
          endPoint: d.endPoint,
          difficultyStart: d.difficultyStart,
          difficultyEnd: d.difficultyEnd,
        })),
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const payload: ExamMakeExamPlanCreateReq = {
        ...values,
        isComputer: values.isComputer ? 1 : 0,
        isPrint: values.isPrint ? 1 : 0,
      };
      if (editingId) {
        updateMutation.mutate({ id: editingId, payload });
      } else {
        createMutation.mutate(payload);
      }
    } catch {
      // validation error
    }
  }, [form, editingId, createMutation, updateMutation]);

  const columns: ColumnsType<ExamMakeExamPlan> = [
    { title: '序号', width: 60, render: (_, __, i) => i + 1 },
    { title: '名称', dataIndex: 'title', width: 180 },
    { title: '类型', dataIndex: 'examType', width: 80, render: (v: number) => EXAM_TYPE_MAP[v] || v },
    { title: '时长(分)', dataIndex: 'time', width: 80 },
    {
      title: '机考', dataIndex: 'isComputer', width: 70,
      render: (v: number) => (v === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: '打印', dataIndex: 'isPrint', width: 70,
      render: (v: number) => (v === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    { title: '总分', dataIndex: 'totalPoint', width: 70 },
    { title: '及格分', dataIndex: 'passPoint', width: 80 },
    { title: '备注', dataIndex: 'comments', width: 120, ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作', width: 120, fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMutation.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const courses = courseQuery.data?.items || [];
  const rates = rateQuery.data || [];

  return (
    <div style={{ display: 'flex', gap: 16, height: '100%' }}>
      {/* 左侧科目筛选 */}
      <Card style={{ width: 260, flexShrink: 0 }} title="科目筛选" size="small">
        <Input
          placeholder="搜索科目"
          prefix={<SearchOutlined />}
          style={{ marginBottom: 12 }}
          onChange={(e) => {
            const val = e.target.value;
            // local filter could be added
          }}
        />
        <List
          dataSource={courses}
          renderItem={(item: ExamCourse) => (
            <List.Item
              style={{
                cursor: 'pointer',
                background: selectedCourseId === item.id ? '#e6f7ff' : undefined,
                padding: '8px 12px',
              }}
              onClick={() => {
                setSelectedCourseId(item.id);
                setQuery({ ...query, page: 1 });
              }}
            >
              {item.title}
            </List.Item>
          )}
        />
        {selectedCourseId && (
          <Button
            type="link"
            size="small"
            onClick={() => setSelectedCourseId(undefined)}
            style={{ marginTop: 8 }}
          >
            清除筛选
          </Button>
        )}
      </Card>

      {/* 右侧主区域 */}
      <div style={{ flex: 1 }}>
        <Tabs
          activeKey={String(examType)}
          onChange={(k) => {
            setExamType(Number(k));
            setQuery({ ...query, page: 1 });
          }}
          items={[
            { key: '0', label: '理论组卷' },
            { key: '1', label: '实操组卷' },
          ]}
          style={{ marginBottom: 0 }}
        />
        <Space style={{ marginBottom: 12 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => listQuery.refetch()}
          >
            刷新
          </Button>
        </Space>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={listQuery.data?.items || []}
          loading={listQuery.isLoading}
          scroll={{ x: 1200 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: listQuery.data?.total || 0,
            showSizeChanger: true,
            onChange: (p, ps) => setQuery({ page: p, pageSize: ps }),
          }}
        />
      </div>

      {/* 弹窗表单 */}
      <Modal
        title={editingId ? '编辑组卷计划' : '添加组卷计划'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        width={800}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="courseId" label="科目" rules={[{ required: true }]}>
              <Select
                placeholder="选择科目"
                options={courses.map((c: ExamCourse) => ({ label: c.title, value: c.id }))}
                showSearch
                optionFilterProp="label"
              />
            </Form.Item>
            <Form.Item name="examType" label="考试类型" rules={[{ required: true }]}>
              <Select
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
                options={rates.map((r: ExamKnowledgeRate) => ({ label: r.title, value: r.id }))}
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

          {/* 题型配置(自定义模式时显示) */}
          <Form.Item noStyle shouldUpdate={(prev, cur) => prev.isAll !== cur.isAll}>
            {({ getFieldValue }) =>
              getFieldValue('isAll') !== 1 && (
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
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
                              <Form.Item name={[field.name, 'type']} label="题型" rules={[{ required: true }]}>
                                <Select options={QUESTION_TYPES} />
                              </Form.Item>
                              <Form.Item name={[field.name, 'num']} label="数量" rules={[{ required: true }]}>
                                <InputNumber min={1} style={{ width: '100%' }} />
                              </Form.Item>
                              <Form.Item name={[field.name, 'point']} label="每题分值">
                                <InputNumber min={0} style={{ width: '100%' }} />
                              </Form.Item>
                              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4 }}>
                                <Form.Item name={[field.name, 'difficultyStart']} label="难度起">
                                  <InputNumber min={0} max={4} style={{ width: '100%' }} />
                                </Form.Item>
                                <Form.Item name={[field.name, 'difficultyEnd']} label="难度止">
                                  <InputNumber min={0} max={4} style={{ width: '100%' }} />
                                </Form.Item>
                              </div>
                              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4 }}>
                                <Form.Item name={[field.name, 'startPoint']} label="分值起">
                                  <InputNumber min={0} style={{ width: '100%' }} />
                                </Form.Item>
                                <Form.Item name={[field.name, 'endPoint']} label="分值止">
                                  <InputNumber min={0} style={{ width: '100%' }} />
                                </Form.Item>
                              </div>
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
              )
            }
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

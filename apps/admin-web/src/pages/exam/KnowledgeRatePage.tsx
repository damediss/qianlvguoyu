import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  TreeSelect,
  Typography,
  message,
  Popconfirm,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import {
  pageKnowledgeRates,
  createKnowledgeRate,
  updateKnowledgeRate,
  deleteKnowledgeRate,
  pageCourses,
  treeKnowledgeStructures,
} from '../../api/exam';
import type {
  ExamKnowledgeRate,
  ExamKnowledgeRateCreateReq,
  ExamCourse,
  ExamKnowledgeStructure,
} from '../../types/exam';

export function KnowledgeRatePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, courseId: undefined as number | undefined });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['knowledge-rates', query],
    queryFn: () => pageKnowledgeRates(query),
  });

  const courseQuery = useQuery({
    queryKey: ['exam-courses-all'],
    queryFn: () => pageCourses({ page: 1, pageSize: 200 }),
  });

  // 知识点树 (用于 TreeSelect)
  const knowledgeQuery = useQuery({
    queryKey: ['knowledge-structures-tree', query.courseId],
    queryFn: () => treeKnowledgeStructures(query.courseId),
  });

  const createMutation = useMutation({
    mutationFn: createKnowledgeRate,
    onSuccess: () => {
      message.success('知识比重已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['knowledge-rates'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExamKnowledgeRateCreateReq }) =>
      updateKnowledgeRate(id, payload),
    onSuccess: () => {
      message.success('知识比重已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['knowledge-rates'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteKnowledgeRate,
    onSuccess: () => {
      message.success('知识比重已删除');
      queryClient.invalidateQueries({ queryKey: ['knowledge-rates'] });
    },
  });

  const handleAdd = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    setModalOpen(true);
  }, [form]);

  const handleEdit = useCallback(
    async (record: ExamKnowledgeRate) => {
      setEditingId(record.id);
      // 加载详情
      const { getKnowledgeRate } = await import('../../api/exam');
      const detail = await getKnowledgeRate(record.id);
      form.setFieldsValue({
        title: detail.title,
        courseId: detail.courseId,
        type: detail.type,
        isStandard: detail.isStandard,
        comments: detail.comments,
        detailList: detail.detailList?.map((d) => ({
          knowledgeId: d.knowledgeId,
          rate: d.rate,
        })),
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const payload: ExamKnowledgeRateCreateReq = { ...values };
      if (editingId) {
        updateMutation.mutate({ id: editingId, payload });
      } else {
        createMutation.mutate(payload);
      }
    } catch {
      // validation error
    }
  }, [form, editingId, createMutation, updateMutation]);

  // Convert tree data for TreeSelect (already in tree shape)
  const toTreeSelectData = (nodes: ExamKnowledgeStructure[]): any[] =>
    nodes.map((n) => ({
      title: n.title,
      value: n.id,
      children: n.children?.length ? toTreeSelectData(n.children) : undefined,
    }));

  const knowledgeTreeData = toTreeSelectData(knowledgeQuery.data || []);
  const courses = courseQuery.data?.items || [];

  const columns: ColumnsType<ExamKnowledgeRate> = [
    { title: '序号', width: 60, render: (_, __, i) => i + 1 },
    { title: '名称', dataIndex: 'title', width: 180 },
    {
      title: '科目', dataIndex: 'courseId', width: 150,
      render: (v: number) => {
        const c = courses.find((c: ExamCourse) => c.id === v);
        return c?.title || v;
      },
    },
    {
      title: '是否标准', dataIndex: 'isStandard', width: 90,
      render: (v: number) => (v === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    { title: '排序号', dataIndex: 'sortNumber', width: 80 },
    { title: '备注', dataIndex: 'comments', width: 150, ellipsis: true },
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

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <Select
          placeholder="筛选科目"
          allowClear
          style={{ width: 200 }}
          value={query.courseId}
          onChange={(v) => setQuery({ ...query, courseId: v, page: 1 })}
          options={courses.map((c: ExamCourse) => ({ label: c.title, value: c.id }))}
          showSearch
          optionFilterProp="label"
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          添加
        </Button>
        <Button icon={<ReloadOutlined />} onClick={() => listQuery.refetch()}>
          刷新
        </Button>
      </Space>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={listQuery.data?.items || []}
        loading={listQuery.isLoading}
        scroll={{ x: 1000 }}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total: listQuery.data?.total || 0,
          showSizeChanger: true,
          onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }),
        }}
      />

      <Modal
        title={editingId ? '编辑知识比重' : '添加知识比重'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        width={700}
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
            <Form.Item name="isStandard" label="是否标准">
              <Select
                options={[
                  { label: '是', value: 1 },
                  { label: '否', value: 0 },
                ]}
              />
            </Form.Item>
          </div>
          <Form.Item name="comments" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>

          <Typography.Title level={5}>知识点比重配置</Typography.Title>
          <Form.List name="detailList">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Card key={field.key} size="small" style={{ marginBottom: 8 }}>
                    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                      <Form.Item
                        name={[field.name, 'knowledgeId']}
                        label="知识点"
                        rules={[{ required: true }]}
                        style={{ flex: 1, marginBottom: 0 }}
                      >
                        <TreeSelect
                          treeData={knowledgeTreeData}
                          placeholder="选择知识点"
                          treeDefaultExpandAll
                          showSearch
                          treeNodeFilterProp="title"
                        />
                      </Form.Item>
                      <Form.Item
                        name={[field.name, 'rate']}
                        label="百分比(%)"
                        rules={[{ required: true }]}
                        style={{ width: 120, marginBottom: 0 }}
                      >
                        <InputNumber min={0} max={100} style={{ width: '100%' }} />
                      </Form.Item>
                      <MinusCircleOutlined
                        onClick={() => remove(field.name)}
                        style={{ color: '#ff4d4f', marginTop: 36 }}
                      />
                    </div>
                  </Card>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加知识点比重
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  );
}

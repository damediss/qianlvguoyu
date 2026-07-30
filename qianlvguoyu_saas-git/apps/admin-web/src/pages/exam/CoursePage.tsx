import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ReadOutlined,
} from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  TreeSelect,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import {
  pageCourses,
  treeWorkTypes,
  createCourse,
  updateCourse,
  deleteCourse,
} from '../../api/exam';
import type { ExamCourse, ExamCourseCreateReq } from '../../types/exam';
import type { ExamWorkType } from '../../types/exam';

const LEVEL_OPTIONS = [
  { label: '初级', value: 0 },
  { label: '中级', value: 1 },
  { label: '高级', value: 2 },
  { label: '技师', value: 3 },
  { label: '高级技师', value: 4 },
];

const LEVEL_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '初级', color: 'green' },
  1: { label: '中级', color: 'blue' },
  2: { label: '高级', color: 'orange' },
  3: { label: '技师', color: 'purple' },
  4: { label: '高级技师', color: 'red' },
};

function buildTreeSelectData(nodes: ExamWorkType[]): any[] {
  return nodes.map((node) => ({
    title: node.title,
    value: node.id,
    children: node.children ? buildTreeSelectData(node.children) : undefined,
  }));
}

export function CoursePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, title: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['exam-courses', query],
    queryFn: () => pageCourses(query),
  });

  const treeQuery = useQuery({
    queryKey: ['exam-work-types-tree'],
    queryFn: treeWorkTypes,
  });

  const createMutation = useMutation({
    mutationFn: createCourse,
    onSuccess: () => {
      message.success('科目已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExamCourseCreateReq }) =>
      updateCourse(id, payload),
    onSuccess: () => {
      message.success('科目已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCourse,
    onSuccess: () => {
      message.success('科目已删除');
      queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: true, sortNumber: 1, isGeneral: false });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (record: ExamCourse) => {
      setEditingId(record.id);
      form.setFieldsValue({
        title: record.title,
        workTypeId: record.workTypeId || undefined,
        level: record.level,
        seriesNoGroup: record.seriesNoGroup || '',
        seriesNo: record.seriesNo || '',
        userInChargeName: record.userInChargeName || '',
        lastLevelGetYear: record.lastLevelGetYear,
        workYearLimit: record.workYearLimit,
        minLevel: record.minLevel,
        minLevelYear: record.minLevelYear,
        startAge: record.startAge,
        isGeneral: record.isGeneral === 1,
        examCondition: record.examCondition || '',
        comments: record.comments || '',
        status: record.status === 1,
        sortNumber: record.sortNumber,
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(() => {
    form.validateFields().then((values) => {
      const payload: ExamCourseCreateReq = {
        title: values.title,
        workTypeId: values.workTypeId || undefined,
        level: values.level ?? 0,
        seriesNoGroup: values.seriesNoGroup || undefined,
        seriesNo: values.seriesNo || undefined,
        userInChargeName: values.userInChargeName || undefined,
        lastLevelGetYear: values.lastLevelGetYear ?? 0,
        workYearLimit: values.workYearLimit,
        minLevel: values.minLevel,
        minLevelYear: values.minLevelYear,
        startAge: values.startAge ?? 0,
        isGeneral: values.isGeneral ? 1 : 0,
        examCondition: values.examCondition || undefined,
        comments: values.comments || undefined,
        status: values.status ? 1 : 0,
        sortNumber: values.sortNumber ?? 1,
      };
      if (editingId) {
        updateMutation.mutate({ id: editingId, payload });
      } else {
        createMutation.mutate(payload);
      }
    });
  }, [form, editingId, createMutation, updateMutation]);

  const handleDelete = useCallback(
    (record: ExamCourse) => {
      Modal.confirm({
        title: `确认删除「${record.title}」？`,
        content: '删除后可在数据库中恢复（逻辑删除）。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteMutation.mutate(record.id),
      });
    },
    [deleteMutation],
  );

  const treeData = treeQuery.data || [];
  const workTypeTreeData = buildTreeSelectData(treeData);

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;

  const columns = [
    {
      title: '科目名称',
      dataIndex: 'title',
      render: (text: string) => (
        <Typography.Text strong>{text}</Typography.Text>
      ),
    },
    { title: '科目编码', dataIndex: 'seriesNo', width: 140 },
    {
      title: '级别',
      dataIndex: 'level',
      width: 100,
      render: (v: number) => {
        const info = LEVEL_MAP[v];
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{v}</Tag>;
      },
    },
    { title: '拥有者', dataIndex: 'userInChargeName', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: number) =>
        v === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    { title: '排序号', dataIndex: 'sortNumber', width: 80 },
    {
      title: '操作',
      width: 120,
      render: (_: any, record: ExamCourse) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><ReadOutlined /></span>
            <Typography.Title level={1}>科目管理</Typography.Title>
          </div>
          <Typography.Text>
            维护科目信息，支持按工种关联、等级分类、编码和条件管理。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
            }}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增科目
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Space style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索科目名称、编码、拥有者"
            allowClear
            style={{ width: 300 }}
            value={query.title}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, title: e.target.value, page: 1 }))
            }
            onPressEnter={() => queryClient.invalidateQueries({ queryKey: ['exam-courses'] })}
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['exam-courses'] })}
          >
            搜索
          </Button>
        </Space>
        <Table
          rowKey="id"
          size="middle"
          loading={listQuery.isLoading}
          dataSource={items}
          columns={columns}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, pageSize) =>
              setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>

      <Modal
        title={editingId ? '编辑科目' : '新增科目'}
        open={modalOpen}
        width={640}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="title"
            label="科目名称"
            rules={[{ required: true, message: '请输入科目名称' }]}
          >
            <Input placeholder="请输入科目名称" maxLength={255} />
          </Form.Item>
          <Form.Item name="workTypeId" label="关联工种">
            <TreeSelect
              allowClear
              showSearch
              treeNodeFilterProp="title"
              placeholder="不选则为独立科目"
              treeData={workTypeTreeData}
              treeDefaultExpandAll
            />
          </Form.Item>
          <Form.Item name="level" label="级别">
            <Select allowClear placeholder="请选择级别" options={LEVEL_OPTIONS} />
          </Form.Item>
          <Form.Item name="seriesNoGroup" label="科目编码(编组)">
            <Input placeholder="请输入科目编码编组" maxLength={200} />
          </Form.Item>
          <Form.Item name="seriesNo" label="科目编码">
            <Input placeholder="请输入科目编码" maxLength={200} />
          </Form.Item>
          <Form.Item name="userInChargeName" label="拥有者名称">
            <Input placeholder="请输入拥有者名称" maxLength={255} />
          </Form.Item>
          <Space size={12}>
            <Form.Item name="workYearLimit" label="工作年限要求">
              <InputNumber min={0} max={99} style={{ width: 160 }} />
            </Form.Item>
            <Form.Item name="startAge" label="开始年龄">
              <InputNumber min={0} max={99} style={{ width: 160 }} />
            </Form.Item>
            <Form.Item name="lastLevelGetYear" label="上级获得年份">
              <InputNumber min={0} max={99} style={{ width: 160 }} />
            </Form.Item>
          </Space>
          <Space size={12}>
            <Form.Item name="minLevel" label="最低证书要求">
              <InputNumber min={0} max={4} style={{ width: 160 }} />
            </Form.Item>
            <Form.Item name="minLevelYear" label="最低证书获得年">
              <InputNumber min={0} max={99} style={{ width: 160 }} />
            </Form.Item>
          </Space>
          <Form.Item name="examCondition" label="考试条件">
            <Input.TextArea placeholder="请输入考试条件" rows={3} />
          </Form.Item>
          <Form.Item name="comments" label="描述">
            <Input placeholder="请输入描述" maxLength={255} />
          </Form.Item>
          <Space size={12}>
            <Form.Item name="status" label="状态" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
            <Form.Item name="isGeneral" label="通用资源" valuePropName="checked">
              <Switch checkedChildren="是" unCheckedChildren="否" />
            </Form.Item>
          </Space>
          <Form.Item name="sortNumber" label="排序号">
            <InputNumber min={1} max={9999} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
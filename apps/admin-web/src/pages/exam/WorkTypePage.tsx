import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ApartmentOutlined,
  UnorderedListOutlined,
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
  pageWorkTypes,
  treeWorkTypes,
  createWorkType,
  updateWorkType,
  deleteWorkType,
} from '../../api/exam';
import type { ExamWorkType, ExamWorkTypeCreateReq } from '../../types/exam';

function buildTreeSelectData(nodes: ExamWorkType[]): any[] {
  return nodes.map((node) => ({
    title: node.title,
    value: node.id,
    children: node.children ? buildTreeSelectData(node.children) : undefined,
  }));
}

function filterSelfAndDescendants(nodes: ExamWorkType[], excludeId: number): any[] {
  return nodes
    .filter((n) => n.id !== excludeId)
    .map((n) => ({
      ...n,
      children: n.children ? filterSelfAndDescendants(n.children, excludeId) : undefined,
    }));
}

export function WorkTypePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, title: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [viewMode, setViewMode] = useState<'table' | 'tree'>('tree');
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['exam-work-types', query],
    queryFn: () => pageWorkTypes(query),
  });

  const treeQuery = useQuery({
    queryKey: ['exam-work-types-tree'],
    queryFn: treeWorkTypes,
  });

  const createMutation = useMutation({
    mutationFn: createWorkType,
    onSuccess: () => {
      message.success('工种已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
      queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExamWorkTypeCreateReq }) =>
      updateWorkType(id, payload),
    onSuccess: () => {
      message.success('工种已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
      queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteWorkType,
    onSuccess: () => {
      message.success('工种已删除（含子级）');
      queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
      queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: true, sortNumber: 1 });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (record: ExamWorkType) => {
      setEditingId(record.id);
      form.setFieldsValue({
        title: record.title,
        seriesNo: record.seriesNo || '',
        levelsNo: record.levelsNo || '',
        parentId: record.parentId || undefined,
        status: record.status === 1,
        sortNumber: record.sortNumber,
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(() => {
    form.validateFields().then((values) => {
      const payload: ExamWorkTypeCreateReq = {
        title: values.title,
        seriesNo: values.seriesNo || undefined,
        levelsNo: values.levelsNo || undefined,
        parentId: values.parentId || undefined,
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
    (record: ExamWorkType) => {
      Modal.confirm({
        title: `确认删除「${record.title}」？`,
        content: '将同时删除该工种下的所有子级工种。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteMutation.mutate(record.id),
      });
    },
    [deleteMutation],
  );

  const treeData = treeQuery.data || [];
  const allTreeData = buildTreeSelectData(treeData);

  const parentTreeData = editingId
    ? buildTreeSelectData(
        filterSelfAndDescendants(
          JSON.parse(JSON.stringify(treeData)),
          editingId,
        ),
      )
    : allTreeData;

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;

  const columns = [
    {
      title: '序号',
      width: 105,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '工种名称',
      dataIndex: 'title',
      render: (text: string) => (
        <Typography.Text strong>{text}</Typography.Text>
      ),
    },
    { title: '职业编码', dataIndex: 'seriesNo', width: 140 },
    { title: '级别', dataIndex: 'levelsNo', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (v: number) =>
        v === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    { title: '排序号', dataIndex: 'sortNumber', width: 90 },
    {
      title: '操作',
      width: 120,
      render: (_: any, record: ExamWorkType) => (
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
            <span className="page-title-icon"><ApartmentOutlined /></span>
            <Typography.Title level={1}>工种管理</Typography.Title>
          </div>
          <Typography.Text>
            维护工种的树形结构，支持多级分类、职业编码和级别管理。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
              queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
            }}
          >
            刷新
          </Button>
          <Select
            value={viewMode}
            onChange={setViewMode}
            style={{ width: 110 }}
            options={[
              { label: '树形视图', value: 'tree' },
              { label: '列表视图', value: 'table' },
            ]}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增工种
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        {viewMode === 'table' ? (
          <>
            <Space style={{ marginBottom: 16 }}>
              <Input
                prefix={<SearchOutlined />}
                placeholder="搜索工种名称、编码、级别"
                allowClear
                style={{ width: 300 }}
                value={query.title}
                onChange={(e) =>
                  setQuery((prev) => ({ ...prev, title: e.target.value, page: 1 }))
                }
                onPressEnter={() => queryClient.invalidateQueries({ queryKey: ['exam-work-types'] })}
              />
              <Button
                icon={<SearchOutlined />}
                onClick={() => queryClient.invalidateQueries({ queryKey: ['exam-work-types'] })}
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
          </>
        ) : (
          <Table
            rowKey="id"
            size="middle"
            loading={treeQuery.isLoading}
            dataSource={treeData}
            columns={columns}
            pagination={false}
            expandable={{ defaultExpandAllRows: true }}
          />
        )}
      </div>

      <Modal
        title={editingId ? '编辑工种' : '新增工种'}
        open={modalOpen}
        width={520}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="title"
            label="工种名称"
            rules={[{ required: true, message: '请输入工种名称' }]}
          >
            <Input placeholder="请输入工种名称" maxLength={255} />
          </Form.Item>
          <Form.Item name="seriesNo" label="职业编码">
            <Input placeholder="请输入职业编码" maxLength={200} />
          </Form.Item>
          <Form.Item name="levelsNo" label="级别">
            <Input placeholder="请输入级别" maxLength={200} />
          </Form.Item>
          <Form.Item name="parentId" label="上级工种">
            <TreeSelect
              allowClear
              showSearch
              treeNodeFilterProp="title"
              placeholder="不选则为顶级工种"
              treeData={parentTreeData}
              treeDefaultExpandAll
            />
          </Form.Item>
          <Form.Item name="status" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
          <Form.Item name="sortNumber" label="排序号">
            <InputNumber min={1} max={9999} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

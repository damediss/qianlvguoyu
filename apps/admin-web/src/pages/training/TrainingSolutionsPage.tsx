import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined, FilterOutlined, ClearOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainSolution, TrainSolutionQuery, TrainSolutionCreateReq } from '../../types/training';

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '审批中', color: 'processing' },
  2: { label: '已通过', color: 'success' },
  3: { label: '已驳回', color: 'error' },
};

export function TrainingSolutionsPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainSolutionCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainSolutionQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-solutions', query],
    queryFn: () => trainApi.pageSolutions(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainSolutionCreateReq) => trainApi.createSolution(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-solutions'] }); },
    onError: () => message.error('创建失败'),
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainSolutionCreateReq }) => trainApi.updateSolution(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-solutions'] }); },
    onError: () => message.error('更新失败'),
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSolution(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-solutions'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainSolution) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const handleSubmit = async () => {
    const v = await form.validateFields();
    editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
  };
  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });
  const handleReset = () => { searchForm.resetFields(); setQuery({ page: 1, pageSize: 20 }); };

  const columns: ColumnsType<TrainSolution> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '方案标题', dataIndex: 'title', width: 200, ellipsis: true },
    { title: '企业', dataIndex: 'companyName', width: 150, ellipsis: true },
    { title: '培训内容', dataIndex: 'trainContent', width: 200, ellipsis: true },
    { title: '培训方式', dataIndex: 'trainMethod', width: 120 },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => { const s = STATUS_MAP[v] ?? { label: '未知', color: 'default' }; return <Tag color={s.color}>{s.label}</Tag>; },
    },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', key: 'act', width: 120, fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(r)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(r.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>培训方案</Typography.Title>
          <Typography.Text type="secondary">管理培训方案，支持方案制定、审批和状态跟踪</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline" style={{ flexWrap: 'wrap', gap: 8 }}>
          <Form.Item name="keyword" style={{ flex: '1 1 220px', minWidth: 180 }}>
            <Input placeholder="搜索方案标题、企业..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<FilterOutlined />} onClick={handleSearch}>查询</Button>
              <Button icon={<ClearOutlined />} onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-solutions'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增方案</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 1000 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑方案' : '新增方案'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnClose width={800}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={12}><Form.Item name="title" label="方案标题" rules={[{ required: true }]}><Input placeholder="请输入方案标题" /></Form.Item></Col>
            <Col span={12}><Form.Item name="companyName" label="企业名称"><Input placeholder="请输入企业" /></Form.Item></Col>
            <Col span={8}>
              <Form.Item name="status" label="状态">
                <Select allowClear placeholder="选择状态" options={Object.entries(STATUS_MAP).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
              </Form.Item>
            </Col>
            <Col span={8}><Form.Item name="trainMethod" label="培训方式"><Input placeholder="线上/线下/混合" /></Form.Item></Col>
            <Col span={8}><Form.Item name="course" label="课程"><Input placeholder="课程名称" /></Form.Item></Col>
            <Col span={24}><Form.Item name="trainContent" label="培训内容"><Input.TextArea rows={2} placeholder="培训内容描述" /></Form.Item></Col>
            <Col span={24}><Form.Item name="mainObjectives" label="主要目标"><Input.TextArea rows={2} placeholder="培训主要目标" /></Form.Item></Col>
            <Col span={24}><Form.Item name="mainInitiatives" label="主要措施"><Input.TextArea rows={2} placeholder="主要措施" /></Form.Item></Col>
            <Col span={24}><Form.Item name="teacherIntro" label="师资介绍"><Input.TextArea rows={2} placeholder="师资介绍" /></Form.Item></Col>
            <Col span={12}><Form.Item name="rejectReason" label="驳回原因"><Input.TextArea rows={2} /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

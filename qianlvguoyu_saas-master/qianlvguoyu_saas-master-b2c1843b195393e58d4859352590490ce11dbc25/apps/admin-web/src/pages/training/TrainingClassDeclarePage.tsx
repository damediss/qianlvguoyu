import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined, FilterOutlined, ClearOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainDeclare, TrainDeclareQuery, TrainDeclareCreateReq } from '../../types/training';

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '审批中', color: 'processing' },
  2: { label: '已通过', color: 'success' },
  3: { label: '已驳回', color: 'error' },
};

export function TrainingClassDeclarePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainDeclareCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainDeclareQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-declares', query],
    queryFn: () => trainApi.pageDeclares(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainDeclareCreateReq) => trainApi.createDeclare(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-declares'] }); },
    onError: () => message.error('创建失败'),
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainDeclareCreateReq }) => trainApi.updateDeclare(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-declares'] }); },
    onError: () => message.error('更新失败'),
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteDeclare(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-declares'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainDeclare) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const handleSubmit = async () => {
    const v = await form.validateFields();
    editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
  };
  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });
  const handleReset = () => { searchForm.resetFields(); setQuery({ page: 1, pageSize: 20 }); };

  const columns: ColumnsType<TrainDeclare> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '申报标题', dataIndex: 'title', width: 200, ellipsis: true },
    { title: '城市', dataIndex: 'cityName', width: 120 },
    { title: '关联培训ID', dataIndex: 'trainId', width: 100 },
    {
      title: '状态', dataIndex: 'status', width: 100,
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
          <Typography.Title level={4}>开班申报</Typography.Title>
          <Typography.Text type="secondary">管理开班申报信息，支持申报创建、审批和状态跟踪</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline" style={{ flexWrap: 'wrap', gap: 8 }}>
          <Form.Item name="keyword" style={{ flex: '1 1 220px', minWidth: 180 }}>
            <Input placeholder="搜索申报标题、城市..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} />
          </Form.Item>
          <Form.Item name="status">
            <Select allowClear placeholder="状态" style={{ width: 120 }}
              options={Object.entries(STATUS_MAP).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
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
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-declares'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增申报</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 800 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑申报' : '新增申报'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnClose width={600}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={24}><Form.Item name="title" label="申报标题" rules={[{ required: true }]}><Input placeholder="请输入申报标题" /></Form.Item></Col>
            <Col span={12}><Form.Item name="trainId" label="关联培训ID"><InputNumber style={{ width: '100%' }} placeholder="培训ID" /></Form.Item></Col>
            <Col span={12}><Form.Item name="cityName" label="城市"><Input placeholder="请输入城市" /></Form.Item></Col>
            <Col span={12}><Form.Item name="cityId" label="城市ID"><InputNumber style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select allowClear placeholder="请选择状态" options={Object.entries(STATUS_MAP).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
              </Form.Item>
            </Col>
            <Col span={12}><Form.Item name="sortNumber" label="排序号"><InputNumber min={0} style={{ width: '100%' }} placeholder="数字越大越靠前" /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

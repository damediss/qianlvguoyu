import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainSummary, TrainSummaryQuery, TrainSummaryCreateReq } from '../../types/training';

export function TrainingSummaryPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainSummaryCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainSummaryQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-summaries', query],
    queryFn: () => trainApi.pageSummaries(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainSummaryCreateReq) => trainApi.createSummary(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-summaries'] }); },
    onError: () => message.error('创建失败'),
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainSummaryCreateReq }) => trainApi.updateSummary(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-summaries'] }); },
    onError: () => message.error('更新失败'),
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSummary(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-summaries'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainSummary) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const handleSubmit = async () => {
    const v = await form.validateFields();
    editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
  };
  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });

  const columns: ColumnsType<TrainSummary> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '标题', dataIndex: 'title', width: 200, ellipsis: true },
    { title: '作者', dataIndex: 'author', width: 120 },
    { title: '标签', dataIndex: 'tags', width: 120, render: (v: string) => v ? v.split(',').map((t: string) => <Tag key={t}>{t.trim()}</Tag>) : null },
    { title: '状态', dataIndex: 'status', width: 100, render: (v: number) => v === 1 ? <Tag color="success">已发布</Tag> : <Tag>草稿</Tag> },
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
          <Typography.Title level={4}>经验萃取</Typography.Title>
          <Typography.Text type="secondary">管理培训经验总结与知识萃取，支持经验文档的创建、编辑和发布</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="keyword">
            <Input placeholder="搜索标题、标签..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} style={{ width: 280 }} />
          </Form.Item>
          <Form.Item name="status">
            <Select allowClear placeholder="状态" style={{ width: 120 }}
              options={[{ value: 0, label: '草稿' }, { value: 1, label: '已发布' }]} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSearch}>查询</Button>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-summaries'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增经验</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 800 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑经验' : '新增经验'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnClose width={700}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={24}><Form.Item name="title" label="标题" rules={[{ required: true }]}><Input placeholder="请输入标题" /></Form.Item></Col>
            <Col span={12}><Form.Item name="author" label="作者"><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={12}><Form.Item name="tags" label="标签"><Input placeholder="多个标签用逗号分隔" /></Form.Item></Col>
            <Col span={12}><Form.Item name="status" label="状态"><Select allowClear placeholder="请选择" options={[{ value: 0, label: '草稿' }, { value: 1, label: '已发布' }]} /></Form.Item></Col>
            <Col span={24}><Form.Item name="content" label="内容"><Input.TextArea rows={6} placeholder="请输入经验总结内容" /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

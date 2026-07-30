import { useState } from 'react';
import { Button, Card, Col, Form, Input, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, Upload, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, UploadOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainCertificate, TrainCertificateQuery, TrainCertificateCreateReq } from '../../types/training';

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '未颁发', color: 'default' },
  1: { label: '已颁发', color: 'success' },
  2: { label: '已作废', color: 'error' },
};

export function TrainingCertificateTemplatePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainCertificateCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainCertificateQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-certificates', query],
    queryFn: () => trainApi.pageCertificates(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainCertificateCreateReq) => trainApi.createCertificate(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-certificates'] }); },
    onError: () => message.error('创建失败'),
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainCertificateCreateReq }) => trainApi.updateCertificate(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-certificates'] }); },
    onError: () => message.error('更新失败'),
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteCertificate(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-certificates'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainCertificate) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const handleSubmit = async () => {
    const v = await form.validateFields();
    editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
  };
  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });

  const columns: ColumnsType<TrainCertificate> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '模板名称', dataIndex: 'name', width: 200, ellipsis: true },
    { title: '类型', dataIndex: 'type', width: 100 },
    { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
    { title: '模板文件', dataIndex: 'templateFile', width: 150, ellipsis: true,
      render: (v: string) => v ? <a href={v} target="_blank" rel="noopener noreferrer">查看文件</a> : <Tag>无</Tag> },
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
          <Typography.Title level={4}>证书模板</Typography.Title>
          <Typography.Text type="secondary">管理培训证书模板，支持模板创建、编辑和文件上传</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="keyword">
            <Input placeholder="搜索模板名称..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} style={{ width: 240 }} />
          </Form.Item>
          <Form.Item name="type">
            <Select allowClear placeholder="模板类型" style={{ width: 120 }}
              options={[{ value: '结业', label: '结业证书' }, { value: '资格', label: '资格证书' }, { value: '培训', label: '培训证明' }]} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSearch}>查询</Button>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-certificates'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增模板</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 900 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑证书模板' : '新增证书模板'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnHidden width={600}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={24}><Form.Item name="name" label="模板名称" rules={[{ required: true }]}><Input placeholder="请输入模板名称" /></Form.Item></Col>
            <Col span={12}>
              <Form.Item name="type" label="模板类型" rules={[{ required: true }]}>
                <Select allowClear placeholder="请选择" options={[{ value: '结业', label: '结业证书' }, { value: '资格', label: '资格证书' }, { value: '培训', label: '培训证明' }]} />
              </Form.Item>
            </Col>
            <Col span={12}><Form.Item name="status" label="状态"><Select allowClear placeholder="请选择" options={[{ value: 0, label: '未颁发' }, { value: 1, label: '已颁发' }]} /></Form.Item></Col>
            <Col span={24}><Form.Item name="description" label="描述"><Input.TextArea rows={2} placeholder="请输入模板描述" /></Form.Item></Col>
            <Col span={24}>
              <Form.Item name="templateFile" label="模板文件">
                <Upload maxCount={1} beforeUpload={() => false}>
                  <Button icon={<UploadOutlined />}>上传模板文件</Button>
                </Upload>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

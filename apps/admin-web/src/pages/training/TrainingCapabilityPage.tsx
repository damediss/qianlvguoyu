import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Table, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainUserAbility, TrainUserAbilityQuery, TrainUserAbilityCreateReq } from '../../types/training';

export function TrainingCapabilityPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainUserAbilityCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainUserAbilityQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-user-abilities', query],
    queryFn: () => trainApi.pageUserAbilities(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainUserAbilityCreateReq) => trainApi.createUserAbility(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] }); },
    onError: () => message.error('创建失败'),
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainUserAbilityCreateReq }) => trainApi.updateUserAbility(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] }); },
    onError: () => message.error('更新失败'),
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteUserAbility(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainUserAbility) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const handleSubmit = async () => {
    const v = await form.validateFields();
    editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
  };
  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });

  const columns: ColumnsType<TrainUserAbility> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '用户姓名', dataIndex: 'userName', width: 120 },
    { title: '技能名称', dataIndex: 'skillName', width: 150, ellipsis: true },
    { title: '技能等级', dataIndex: 'skillLevel', width: 100 },
    { title: '证书编号', dataIndex: 'certificateNo', width: 150 },
    { title: '颁发机构', dataIndex: 'issuingAuthority', width: 150 },
    { title: '有效期', dataIndex: 'expireDate', width: 120 },
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
          <Typography.Title level={4}>能力矩阵</Typography.Title>
          <Typography.Text type="secondary">管理学员技能能力信息，支持技能等级、证书等数据维护</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="keyword">
            <Input placeholder="搜索用户姓名、技能..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} style={{ width: 240 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSearch}>查询</Button>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增能力</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 900 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑能力' : '新增能力'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnHidden width={600}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={12}><Form.Item name="userName" label="用户姓名" rules={[{ required: true }]}><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={12}><Form.Item name="skillName" label="技能名称" rules={[{ required: true }]}><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={12}><Form.Item name="skillLevel" label="技能等级"><InputNumber min={1} max={10} style={{ width: '100%' }} placeholder="1-10" /></Form.Item></Col>
            <Col span={12}><Form.Item name="certificateNo" label="证书编号"><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={12}><Form.Item name="issuingAuthority" label="颁发机构"><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={12}><Form.Item name="expireDate" label="有效期"><Input placeholder="YYYY-MM-DD" /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

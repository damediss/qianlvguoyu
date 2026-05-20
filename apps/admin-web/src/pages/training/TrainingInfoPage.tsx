import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined, FilterOutlined, ClearOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainInfoList, TrainInfoListQuery, TrainInfoListCreateReq } from '../../types/training';

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待开始', color: 'default' },
  1: { label: '进行中', color: 'processing' },
  2: { label: '已结束', color: 'success' },
  3: { label: '已取消', color: 'error' },
};

const PASS_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'default' },
  1: { label: '已通过', color: 'success' },
  2: { label: '已驳回', color: 'error' },
};

export function TrainingInfoPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainInfoListCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainInfoListQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-info', query],
    queryFn: () => trainApi.pageInfoLists(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainInfoListCreateReq) => trainApi.createInfoList(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
    onError: () => message.error('创建失败'),
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainInfoListCreateReq }) => trainApi.updateInfoList(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
    onError: () => message.error('更新失败'),
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteInfoList(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainInfoList) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const handleSubmit = async () => {
    const v = await form.validateFields();
    editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
  };
  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });
  const handleReset = () => { searchForm.resetFields(); setQuery({ page: 1, pageSize: 20 }); };

  const columns: ColumnsType<TrainInfoList> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '培训标题', dataIndex: 'title', width: 180, ellipsis: true },
    { title: '企业', dataIndex: 'companyName', width: 140, ellipsis: true },
    { title: '工种ID', dataIndex: 'workTypeId', width: 80 },
    { title: '人数', dataIndex: 'num', width: 70, align: 'center' },
    { title: '岗位', dataIndex: 'position', width: 100 },
    {
      title: '审核状态', dataIndex: 'passStatus', width: 100,
      render: (v: number) => { const s = PASS_STATUS_MAP[v] ?? { label: '未知', color: 'default' }; return <Tag color={s.color}>{s.label}</Tag>; },
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => { const s = STATUS_MAP[v] ?? { label: '未知', color: 'default' }; return <Tag color={s.color}>{s.label}</Tag>; },
    },
    { title: '开始日期', dataIndex: 'startDate', width: 110 },
    { title: '结束日期', dataIndex: 'endDate', width: 110 },
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
          <Typography.Title level={4}>培训信息</Typography.Title>
          <Typography.Text type="secondary">管理培训信息，跟踪培训项目状态和进度</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline" style={{ flexWrap: 'wrap', gap: 8 }}>
          <Form.Item name="keyword" style={{ flex: '1 1 220px', minWidth: 180 }}>
            <Input placeholder="搜索培训标题、企业..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} />
          </Form.Item>
          <Form.Item name="status">
            <Select allowClear placeholder="培训状态" style={{ width: 120 }}
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
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-info'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增培训</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 1200 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑培训' : '新增培训'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnClose width={800}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={14}><Form.Item name="title" label="培训标题" rules={[{ required: true }]}><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={10}><Form.Item name="trainCompanyName" label="培训机构"><Input placeholder="培训机构名称" /></Form.Item></Col>
            <Col span={8}><Form.Item name="companyName" label="企业名称"><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={8}><Form.Item name="companyAddress" label="企业地址"><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={8}><Form.Item name="num" label="人数"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={6}><Form.Item name="position" label="岗位"><Input placeholder="岗位" /></Form.Item></Col>
            <Col span={6}><Form.Item name="workTypeId" label="工种ID"><InputNumber style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={6}><Form.Item name="courseNum" label="课程数"><InputNumber style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={6}><Form.Item name="point" label="积分"><InputNumber style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={6}><Form.Item name="startDate" label="开始日期"><Input placeholder="2024-01-01" /></Form.Item></Col>
            <Col span={6}><Form.Item name="endDate" label="结束日期"><Input placeholder="2024-12-31" /></Form.Item></Col>
            <Col span={6}>
              <Form.Item name="status" label="状态">
                <Select allowClear placeholder="状态" options={Object.entries(STATUS_MAP).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="passStatus" label="审核状态">
                <Select allowClear placeholder="审核" options={Object.entries(PASS_STATUS_MAP).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
              </Form.Item>
            </Col>
            <Col span={24}><Form.Item name="intro" label="简介"><Input.TextArea rows={2} placeholder="培训简介" /></Form.Item></Col>
            <Col span={12}><Form.Item name="provinceName" label="省"><Input /></Form.Item></Col>
            <Col span={12}><Form.Item name="cityName" label="市"><Input /></Form.Item></Col>
            <Col span={12}><Form.Item name="inChargeUsername" label="负责人"><Input placeholder="负责人" /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined, FilterOutlined, ClearOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainNeed, TrainNeedQuery, TrainNeedCreateReq } from '../../types/training';

export function TrainingNeedsPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainNeedCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainNeedQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-needs', query],
    queryFn: () => trainApi.pageNeeds(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainNeedCreateReq) => trainApi.createNeed(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-needs'] }); },
    onError: () => message.error('创建失败'),
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainNeedCreateReq }) => trainApi.updateNeed(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-needs'] }); },
    onError: () => message.error('更新失败'),
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteNeed(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-needs'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainNeed) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const handleSubmit = async () => {
    const v = await form.validateFields();
    editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
  };
  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });
  const handleReset = () => { searchForm.resetFields(); setQuery({ page: 1, pageSize: 20 }); };

  const columns: ColumnsType<TrainNeed> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '企业', dataIndex: 'orgName', width: 150, ellipsis: true },
    { title: '培训对象', dataIndex: 'trainObject', width: 100 },
    { title: '岗位', dataIndex: 'position', width: 100 },
    { title: '人数', dataIndex: 'num', width: 70, align: 'center' },
    { title: '工种', dataIndex: 'workType', width: 100 },
    { title: '等级', dataIndex: 'level', width: 80 },
    { title: '培训日期', dataIndex: 'trainDate', width: 110 },
    { title: '预算', dataIndex: 'budget', width: 100 },
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
          <Typography.Title level={4}>需求管理</Typography.Title>
          <Typography.Text type="secondary">管理企业培训需求，支持需求录入、查询和维护</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline" style={{ flexWrap: 'wrap', gap: 8 }}>
          <Form.Item name="keyword" style={{ flex: '1 1 220px', minWidth: 180 }}>
            <Input placeholder="搜索企业、工种、岗位..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} />
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
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-needs'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增需求</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 1100 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑需求' : '新增需求'} open={modalOpen} onOk={handleSubmit} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnClose width={800}>
        <Form form={form} layout="vertical">
          <Row gutter={24}>
            <Col span={12}><Form.Item name="orgName" label="企业名称"><Input placeholder="请输入" /></Form.Item></Col>
            <Col span={12}><Form.Item name="orgId" label="企业ID"><InputNumber style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={8}><Form.Item name="trainObject" label="培训对象"><Input placeholder="如：在岗职工" /></Form.Item></Col>
            <Col span={8}><Form.Item name="position" label="岗位"><Input placeholder="岗位名称" /></Form.Item></Col>
            <Col span={8}><Form.Item name="num" label="人数"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={8}><Form.Item name="workType" label="工种"><Input placeholder="工种" /></Form.Item></Col>
            <Col span={8}>
              <Form.Item name="level" label="等级">
                <Select allowClear placeholder="选择等级" options={['初级','中级','高级','技师','高级技师'].map(v => ({ value: v, label: v }))} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="method" label="培训方式">
                <Select allowClear placeholder="选择方式" options={['线上','线下','混合'].map(v => ({ value: v, label: v }))} />
              </Form.Item>
            </Col>
            <Col span={12}><Form.Item name="target" label="培训目标"><Input placeholder="培训目标" /></Form.Item></Col>
            <Col span={6}><Form.Item name="trainDate" label="培训日期"><Input placeholder="2024-06-01" /></Form.Item></Col>
            <Col span={6}><Form.Item name="budget" label="预算"><Input placeholder="预算" /></Form.Item></Col>
            <Col span={8}><Form.Item name="address" label="培训地点"><Input placeholder="地点" /></Form.Item></Col>
            <Col span={8}><Form.Item name="needExam" label="需考试"><Select allowClear placeholder="选择" options={[{value:0,label:'否'},{value:1,label:'是'}]} /></Form.Item></Col>
            <Col span={8}><Form.Item name="needPaper" label="需试卷"><Select allowClear placeholder="选择" options={[{value:0,label:'否'},{value:1,label:'是'}]} /></Form.Item></Col>
            <Col span={24}><Form.Item name="teacherRequire" label="师资要求"><Input.TextArea rows={2} placeholder="师资要求描述" /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

import { useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tabs, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined, FilterOutlined, ClearOutlined, UserAddOutlined, TeamOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainClass, TrainClassQuery, TrainClassCreateReq, TrainPeople, TrainPeopleQuery, TrainPeopleCreateReq, AssignClassReq } from '../../types/training';

/* ==================== 班级管理 ==================== */
function ClassList() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainClassCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainClassQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignClassId, setAssignClassId] = useState<number | null>(null);
  const [assignForm] = Form.useForm<AssignClassReq>();

  const { data, isLoading } = useQuery({
    queryKey: ['training-classes', query],
    queryFn: () => trainApi.pageClasses(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainClassCreateReq) => trainApi.createClass(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-classes'] }); },
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainClassCreateReq }) => trainApi.updateClass(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-classes'] }); },
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteClass(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-classes'] }); },
  });
  const assignMu = useMutation({
    mutationFn: (p: AssignClassReq) => trainApi.batchAssignClass(p),
    onSuccess: () => { message.success('分配成功'); setAssignOpen(false); assignForm.resetFields(); queryClient.invalidateQueries({ queryKey: ['training-classes'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainClass) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
  const openAssign = (id: number) => { setAssignClassId(id); assignForm.resetFields(); setAssignOpen(true); };

  const columns: ColumnsType<TrainClass> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '班级名称', dataIndex: 'className', width: 150 },
    { title: '关联培训ID', dataIndex: 'trainId', width: 100 },
    { title: '用户ID', dataIndex: 'userId', width: 80 },
    { title: '是否班委', dataIndex: 'isCommittee', width: 90, render: (v: number) => v === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag> },
    { title: '是否组长', dataIndex: 'isLeader', width: 90, render: (v: number) => v === 1 ? <Tag color="green">是</Tag> : <Tag>否</Tag> },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', key: 'act', width: 200, fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(r)}>编辑</Button>
          <Button type="link" size="small" icon={<UserAddOutlined />} onClick={() => openAssign(r.id)}>分配学员</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(r.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline" style={{ flexWrap: 'wrap', gap: 8 }}>
          <Form.Item name="keyword" style={{ flex: '1 1 200px', minWidth: 160 }}>
            <Input placeholder="搜索班级名称..." prefix={<SearchOutlined />} allowClear />
          </Form.Item>
          <Form.Item name="trainId"><InputNumber placeholder="培训ID" style={{ width: 120 }} /></Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<FilterOutlined />} onClick={() => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() })}>查询</Button>
              <Button icon={<ClearOutlined />} onClick={() => { searchForm.resetFields(); setQuery({ page: 1, pageSize: 20 }); }}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-classes'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增班级</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 900 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑班级' : '新增班级'} open={modalOpen} onOk={async () => { const v = await form.validateFields(); editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v); }} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnClose width={500}>
        <Form form={form} layout="vertical">
          <Form.Item name="className" label="班级名称" rules={[{ required: true }]}><Input placeholder="请输入班级名称" /></Form.Item>
          <Form.Item name="trainId" label="关联培训ID"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="userId" label="用户ID"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="isCommittee" label="是否班委"><Select allowClear options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} /></Form.Item>
          <Form.Item name="isLeader" label="是否组长"><Select allowClear options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="分配学员" open={assignOpen} onOk={() => assignMu.mutate({ classId: assignClassId!, userIds: assignForm.getFieldValue('userIds') || [] })}
        onCancel={() => { setAssignOpen(false); assignForm.resetFields(); }} destroyOnClose width={500}>
        <Form form={assignForm} layout="vertical">
          <Form.Item name="userIds" label="学员ID列表（逗号分隔）" rules={[{ required: true }]}>
            <Input placeholder="例如: 101,102,103" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

/* ==================== 人员管理 ==================== */
function PeopleList() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainPeopleCreateReq>();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<TrainPeopleQuery>({ page: 1, pageSize: 20 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['training-people', query],
    queryFn: () => trainApi.pagePeople(query),
  });

  const createMu = useMutation({
    mutationFn: (p: TrainPeopleCreateReq) => trainApi.createPeople(p),
    onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-people'] }); },
  });
  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainPeopleCreateReq }) => trainApi.updatePeople(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-people'] }); },
  });
  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deletePeople(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-people'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
  const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
  const openEdit = (r: TrainPeople) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };

  const columns: ColumnsType<TrainPeople> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '姓名', dataIndex: 'realname', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 130 },
    { title: '身份证', dataIndex: 'idCard', width: 170, ellipsis: true },
    { title: '关联培训', dataIndex: 'trainId', width: 90 },
    { title: '关联班级', dataIndex: 'trainClassId', width: 90 },
    { title: '类型', dataIndex: 'type', width: 80 },
    { title: '是否组长', dataIndex: 'isLeader', width: 90, render: (v: number) => v === 1 ? <Tag color="green">是</Tag> : <Tag>否</Tag> },
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
    <div>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline" style={{ flexWrap: 'wrap', gap: 8 }}>
          <Form.Item name="keyword" style={{ flex: '1 1 200px', minWidth: 160 }}>
            <Input placeholder="搜索姓名、手机号..." prefix={<SearchOutlined />} allowClear />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<FilterOutlined />} onClick={() => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() })}>查询</Button>
              <Button icon={<ClearOutlined />} onClick={() => { searchForm.resetFields(); setQuery({ page: 1, pageSize: 20 }); }}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['training-people'] })}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增人员</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={data?.items ?? []}
          scroll={{ x: 1100 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title={editingId ? '编辑人员' : '新增人员'} open={modalOpen} onOk={async () => { const v = await form.validateFields(); editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v); }} onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending} destroyOnClose width={600}>
        <Form form={form} layout="vertical">
          <Form.Item name="realname" label="姓名" rules={[{ required: true }]}><Input placeholder="请输入姓名" /></Form.Item>
          <Form.Item name="phone" label="手机号"><Input placeholder="请输入手机号" /></Form.Item>
          <Form.Item name="idCard" label="身份证"><Input placeholder="请输入身份证号" /></Form.Item>
          <Form.Item name="trainId" label="关联培训ID"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="trainClassId" label="关联班级ID"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="type" label="人员类型"><Input placeholder="学员/教师/管理员" /></Form.Item>
          <Form.Item name="isLeader" label="是否组长"><Select allowClear options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} /></Form.Item>
          <Form.Item name="isCommittee" label="是否班委"><Select allowClear options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

/* ==================== 主页面 ==================== */
export function TrainingClassPeoplePage() {
  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>班级人员</Typography.Title>
          <Typography.Text type="secondary">管理培训班级和学员，支持分班、分配学员和维护人员信息</Typography.Text>
        </div>
      </section>
      <Tabs defaultActiveKey="classes" items={[
        { key: 'classes', label: (<span><TeamOutlined /> 班级管理</span>), children: <ClassList /> },
        { key: 'people', label: (<span><UserAddOutlined /> 人员管理</span>), children: <PeopleList /> },
      ]} />
    </div>
  );
}

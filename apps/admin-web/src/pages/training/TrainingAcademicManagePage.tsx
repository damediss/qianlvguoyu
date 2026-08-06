import { useState } from 'react';
import {
  Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message,
} from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainTeacherBank, TrainTeacherBankCreateReq } from '../../types/training';

const STATUS_OPTIONS = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' },
];

const GENDER_OPTIONS = [
  { value: 1, label: '男' },
  { value: 0, label: '女' },
];

export function TrainingAcademicManagePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainTeacherBankCreateReq>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TrainTeacherBank | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['teacher-banks', page, keyword],
    queryFn: () => trainApi.pageTeacherBanks({ page, pageSize: 20, keyword: keyword || undefined }),
  });

  const createMu = useMutation({
    mutationFn: (payload: TrainTeacherBankCreateReq) => trainApi.createTeacherBank(payload),
    onSuccess: () => {
      message.success('添加成功');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['teacher-banks'] });
    },
    onError: (e: Error) => message.error(e.message || '添加失败'),
  });

  const updateMu = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: TrainTeacherBankCreateReq }) =>
      trainApi.updateTeacherBank(id, payload),
    onSuccess: () => {
      message.success('保存成功');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['teacher-banks'] });
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteTeacherBank(id),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['teacher-banks'] });
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 1 });
    setModalOpen(true);
  };

  const openEdit = (row: TrainTeacherBank) => {
    setEditing(row);
    form.setFieldsValue({
      name: row.name,
      phone: row.phone,
      idCardNumber: row.idCardNumber || row.idCard,
      company: row.company,
      gender: row.gender,
      education: row.education,
      major: row.major,
      status: row.status ?? 1,
      address: row.address,
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const v = await form.validateFields();
    if (editing?.id != null) {
      updateMu.mutate({ id: editing.id, payload: v });
    } else {
      createMu.mutate(v);
    }
  };

  const columns: ColumnsType<TrainTeacherBank> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 130, render: (v?: string) => v || '—' },
    {
      title: '身份证号',
      width: 180,
      render: (_, row) => row.idCardNumber || row.idCard || '—',
    },
    { title: '工作单位', dataIndex: 'company', ellipsis: true, render: (v?: string) => v || '—'
    },
    {
      title: '性别',
      dataIndex: 'gender',
      width: 70,
      render: (v?: number) => (v === 1 ? '男' : v === 0 ? '女' : '—'),
    },
    { title: '学历', dataIndex: 'education', width: 90, render: (v?: string) => v || '—' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v?: number) => (v === 0 ? '停用' : '启用'),
    },
    {
      title: '操作',
      key: 'act',
      width: 180,
      fixed: 'right',
      render: (_, row) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>
            修改
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(row.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>教务管理</Typography.Title>
          <Typography.Text type="secondary">
            师资/教务人员库（train_teacher_bank），供开班准备与班级教务导入使用
          </Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="姓名/手机/身份证搜索"
            prefix={<SearchOutlined />}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={() => {
              setPage(1);
              void refetch();
            }}
            style={{ width: 240 }}
            allowClear
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() => {
              setPage(1);
              void refetch();
            }}
          >
            查询
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            添加
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => void refetch()}>
            刷新
          </Button>
        </Space>
      </Card>

      <Card size="small">
        <Table
          rowKey="id"
          loading={isLoading}
          columns={columns}
          dataSource={data?.items ?? []}
          scroll={{ x: 1100 }}
          pagination={{
            current: page,
            pageSize: 20,
            total: data?.total ?? 0,
            onChange: setPage,
            showTotal: (t) => `共 ${t} 人`,
          }}
        />
      </Card>

      <Modal
        title={editing ? '修改教务人员' : '添加教务人员'}
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          setEditing(null);
        }}
        onOk={() => void submit()}
        confirmLoading={createMu.isPending || updateMu.isPending}
        destroyOnHidden
        width={560}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input maxLength={11} />
          </Form.Item>
          <Form.Item name="idCardNumber" label="身份证号">
            <Input maxLength={18} />
          </Form.Item>
          <Form.Item name="company" label="工作单位">
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="gender" label="性别">
            <Select allowClear options={GENDER_OPTIONS} placeholder="请选择" />
          </Form.Item>
          <Form.Item name="education" label="学历">
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="major" label="专业">
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="address" label="地址">
            <Input maxLength={500} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

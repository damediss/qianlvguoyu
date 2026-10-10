import { useMemo, useState } from 'react';
import {
  Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, BankOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { SystemCompany, SystemCompanySaveReq } from '../../types/training';

const STATUS_OPTIONS = [
  { value: 'enabled', label: '启用' },
  { value: 'disabled', label: '禁用' },
];

export function EnterpriseManagePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<SystemCompanySaveReq>();
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<SystemCompany | null>(null);

  const { data = [], isLoading, refetch } = useQuery({
    queryKey: ['system-companies', searchKeyword],
    queryFn: () => trainApi.listSystemCompanies(searchKeyword || undefined),
  });

  const createMu = useMutation({
    mutationFn: (payload: SystemCompanySaveReq) => trainApi.createSystemCompany(payload),
    onSuccess: () => {
      message.success('创建成功');
      closeModal();
      void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
      void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const updateMu = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: SystemCompanySaveReq }) =>
      trainApi.updateSystemCompany(id, payload),
    onSuccess: () => {
      message.success('更新成功');
      closeModal();
      void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
      void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
    },
    onError: (e: Error) => message.error(e.message || '更新失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSystemCompany(id),
    onSuccess: () => {
      message.success('删除成功');
      void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
      void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
    form.resetFields();
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ sortOrder: 0, status: 'enabled' });
    setModalOpen(true);
  };

  const openEdit = (row: SystemCompany) => {
    setEditing(row);
    form.setFieldsValue({
      name: row.name,
      sortOrder: row.sortOrder ?? 0,
      status: row.status || 'enabled',
    });
    setModalOpen(true);
  };

  const columns: ColumnsType<SystemCompany> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 90 },
      { title: '编码', dataIndex: 'code', width: 180 },
      { title: '企业名称', dataIndex: 'name', ellipsis: true },
      { title: '排序', dataIndex: 'sortOrder', width: 80, align: 'center' },
      {
        title: '状态',
        dataIndex: 'status',
        width: 90,
        align: 'center',
        render: (v: string) =>
          v === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>禁用</Tag>,
      },
      {
        title: '操作',
        key: 'act',
        width: 140,
        fixed: 'right',
        render: (_, row) => (
          <Space size="small">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>
              编辑
            </Button>
            <Popconfirm title="确定删除该企业？" onConfirm={() => deleteMu.mutate(row.id)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          </Space>
        ),
      },
    ],
    [deleteMu],
  );

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>
            <BankOutlined style={{ marginRight: 8 }} />
            企业管理
          </Typography.Title>
          <Typography.Text type="secondary">
            维护系统企业（培训关联企业），供调研关联、开班、审核等场景选用
          </Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="按名称或编码搜索"
            prefix={<SearchOutlined />}
            value={keyword}
            allowClear
            style={{ width: 240 }}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={() => setSearchKeyword(keyword.trim())}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => setSearchKeyword(keyword.trim())}
          >
            搜索
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增企业
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
            刷新
          </Button>
        </Space>
      </Card>

      <Card size="small">
        <Table
          rowKey="id"
          loading={isLoading}
          columns={columns}
          dataSource={data}
          scroll={{ x: 800 }}
          pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        />
      </Card>

      <Modal
        title={editing ? '编辑企业' : '新增企业'}
        open={modalOpen}
        onCancel={closeModal}
        onOk={() => form.submit()}
        confirmLoading={createMu.isPending || updateMu.isPending}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => {
            if (editing) {
              updateMu.mutate({ id: editing.id, payload: values });
            } else {
              createMu.mutate(values);
            }
          }}
        >
          <Form.Item
            name="name"
            label="企业名称"
            rules={[{ required: true, message: '请输入企业名称' }]}
          >
            <Input maxLength={128} placeholder="请输入企业名称" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

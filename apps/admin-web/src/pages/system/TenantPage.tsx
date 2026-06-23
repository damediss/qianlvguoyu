import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import {
  pageTenants,
  createTenant,
  updateTenant,
  deleteTenant,
} from '../../api/system';
import type { TenantVO, TenantCreateReq } from '../../types/system';

export function TenantPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', status: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['system-tenants', query],
    queryFn: () => pageTenants(query),
  });

  const createMutation = useMutation({
    mutationFn: createTenant,
    onSuccess: () => {
      message.success('租户已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-tenants'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: TenantCreateReq }) =>
      updateTenant(id, payload as Parameters<typeof updateTenant>[1]),
    onSuccess: () => {
      message.success('租户已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-tenants'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteTenant,
    onSuccess: () => {
      message.success('租户已删除');
      queryClient.invalidateQueries({ queryKey: ['system-tenants'] });
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 'active' });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (record: TenantVO) => {
      setEditingId(record.id);
      form.setFieldsValue({
        code: record.code,
        name: record.name,
        status: record.status,
        contactName: record.contactName,
        contactPhone: record.contactPhone,
        remark: record.remark,
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(() => {
    form.validateFields().then((values) => {
      if (editingId) {
        updateMutation.mutate({ id: editingId, payload: values });
      } else {
        createMutation.mutate(values);
      }
    });
  }, [form, editingId, createMutation, updateMutation]);

  const handleDelete = useCallback(
    (record: TenantVO) => {
      Modal.confirm({
        title: `确认删除租户「${record.name}」？`,
        content: '删除租户将影响该租户下的所有数据和用户，此操作不可恢复。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteMutation.mutate(record.id),
      });
    },
    [deleteMutation],
  );

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;

  const columns = [
    {
      title: '序号',
      width: 80,
      render: (_: unknown, __: unknown, index: number) => index + 1,
    },
    {
      title: '租户编码',
      dataIndex: 'code',
      width: 150,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '租户名称',
      dataIndex: 'name',
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '联系人',
      dataIndex: 'contactName',
      width: 120,
      render: (v: string) => v || '-',
    },
    {
      title: '联系电话',
      dataIndex: 'contactPhone',
      width: 140,
      render: (v: string) => v || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v: string) => {
        const map: Record<string, { color: string; text: string }> = {
          active: { color: 'success', text: '正常' },
          suspended: { color: 'warning', text: '挂起' },
          disabled: { color: 'error', text: '停用' },
        };
        const item = map[v] || { color: 'default', text: v };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (v: string) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      width: 140,
      render: (_: unknown, record: TenantVO) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><TeamOutlined /></span>
            <Typography.Title level={1}>租户管理</Typography.Title>
          </div>
          <Typography.Text>
            管理平台中的所有租户，包括创建、状态控制和联系信息维护。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-tenants'] })}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增租户
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Space style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索租户名称或编码"
            allowClear
            style={{ width: 280 }}
            value={query.keyword}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 }))
            }
            onPressEnter={() => queryClient.invalidateQueries({ queryKey: ['system-tenants'] })}
          />
          <Select
            placeholder="状态"
            allowClear
            style={{ width: 120 }}
            value={query.status || undefined}
            onChange={(v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 }))}
            options={[
              { label: '正常', value: 'active' },
              { label: '挂起', value: 'suspended' },
              { label: '停用', value: 'disabled' },
            ]}
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-tenants'] })}
          >
            搜索
          </Button>
        </Space>
        <Table
          rowKey="id"
          size="middle"
          loading={listQuery.isLoading}
          dataSource={items}
          columns={columns}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, pageSize) =>
              setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>

      <Modal
        title={editingId ? '编辑租户' : '新增租户'}
        open={modalOpen}
        width={560}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="code"
            label="租户编码"
            rules={[{ required: true, message: '请输入租户编码' }]}
          >
            <Input
              placeholder="请输入唯一租户编码（如 acme_corp）"
              maxLength={64}
              disabled={!!editingId}
            />
          </Form.Item>
          <Form.Item
            name="name"
            label="租户名称"
            rules={[{ required: true, message: '请输入租户名称' }]}
          >
            <Input placeholder="请输入租户名称" maxLength={128} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { label: '正常', value: 'active' },
                { label: '挂起', value: 'suspended' },
                { label: '停用', value: 'disabled' },
              ]}
            />
          </Form.Item>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item name="contactName" label="联系人" style={{ flex: 1 }}>
              <Input placeholder="联系人姓名" maxLength={64} />
            </Form.Item>
            <Form.Item name="contactPhone" label="联系电话" style={{ flex: 1 }}>
              <Input placeholder="联系人电话" maxLength={32} />
            </Form.Item>
          </Space>
          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="备注信息（可选）" maxLength={512} rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

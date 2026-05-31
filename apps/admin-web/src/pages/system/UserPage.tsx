import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UserOutlined,
  KeyOutlined,
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
  TreeSelect,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import {
  pageUsers,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
  getOrgTree,
  listAllPosts,
  pageRoles,
} from '../../api/system';
import type { OrgTreeVO, PostVO, RoleVO, UserVO } from '../../types/system';

function buildOrgTreeSelect(nodes: OrgTreeVO[]): any[] {
  return nodes.map((node) => ({
    title: node.name,
    value: node.id,
    children: node.children?.length ? buildOrgTreeSelect(node.children) : undefined,
  }));
}

export function UserPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', status: '', userType: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [pwdModalOpen, setPwdModalOpen] = useState(false);
  const [pwdUserId, setPwdUserId] = useState<number | null>(null);
  const [form] = Form.useForm();
  const [pwdForm] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['system-users', query],
    queryFn: () => pageUsers(query),
  });

  const orgQuery = useQuery({
    queryKey: ['system-orgs-tree'],
    queryFn: getOrgTree,
  });

  const postQuery = useQuery({
    queryKey: ['system-posts-all'],
    queryFn: listAllPosts,
  });

  const roleQuery = useQuery({
    queryKey: ['system-roles-all'],
    queryFn: () => pageRoles({ page: 1, pageSize: 200 }),
  });

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      message.success('用户已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-users'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Parameters<typeof updateUser>[1] }) =>
      updateUser(id, payload),
    onSuccess: () => {
      message.success('用户已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-users'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteUser,
    onSuccess: () => {
      message.success('用户已删除');
      queryClient.invalidateQueries({ queryKey: ['system-users'] });
    },
  });

  const resetPwdMutation = useMutation({
    mutationFn: ({ id, password }: { id: number; password: string }) =>
      resetPassword(id, { newPassword: password }),
    onSuccess: () => {
      message.success('密码已重置');
      setPwdModalOpen(false);
      setPwdUserId(null);
      pwdForm.resetFields();
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 'active', userType: 'normal', roleIds: [] });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (record: UserVO) => {
      setEditingId(record.id);
      form.setFieldsValue({
        orgId: record.orgId || undefined,
        postId: record.postId || undefined,
        username: record.username,
        displayName: record.displayName,
        userType: record.userType,
        phone: record.phone,
        email: record.email,
        status: record.status,
        roleIds: record.roleIds || [],
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
    (record: UserVO) => {
      Modal.confirm({
        title: `确认删除用户「${record.displayName}」？`,
        content: '删除后不可恢复。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteMutation.mutate(record.id),
      });
    },
    [deleteMutation],
  );

  const handleResetPassword = useCallback(() => {
    pwdForm.validateFields().then((values) => {
      if (pwdUserId) {
        resetPwdMutation.mutate({ id: pwdUserId, password: values.newPassword });
      }
    });
  }, [pwdForm, pwdUserId, resetPwdMutation]);

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;
  const orgTreeData = buildOrgTreeSelect(orgQuery.data || []);
  const posts = postQuery.data || [];
  const roles: RoleVO[] = roleQuery.data?.items || [];

  const columns = [
    {
      title: '序号',
      width: 70,
      render: (_: unknown, __: unknown, index: number) => index + 1,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      width: 130,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '姓名',
      dataIndex: 'displayName',
      width: 120,
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '用户类型',
      dataIndex: 'userType',
      width: 100,
      render: (v: string) => {
        const map: Record<string, string> = {
          platform_admin: '平台管理员',
          tenant_admin: '租户管理员',
          normal: '普通用户',
        };
        return map[v] || v;
      },
    },
    {
      title: '岗位',
      dataIndex: 'postName',
      width: 120,
      render: (v: string) => v || '-',
    },
    {
      title: '角色',
      dataIndex: 'roleCodes',
      width: 180,
      render: (codes: string[]) =>
        codes?.length ? (
          <Space size={4} wrap>
            {codes.map((c) => (
              <Tag key={c} color="blue">{c}</Tag>
            ))}
          </Space>
        ) : (
          '-'
        ),
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      width: 130,
      render: (v: string) => v || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: string) => {
        const map: Record<string, { color: string; text: string }> = {
          active: { color: 'success', text: '正常' },
          locked: { color: 'warning', text: '锁定' },
          disabled: { color: 'error', text: '停用' },
        };
        const item = map[v] || { color: 'default', text: v };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '最后登录',
      dataIndex: 'lastLoginAt',
      width: 160,
      render: (v: string) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      width: 200,
      render: (_: unknown, record: UserVO) => (
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
            icon={<KeyOutlined />}
            onClick={() => {
              setPwdUserId(record.id);
              pwdForm.resetFields();
              setPwdModalOpen(true);
            }}
          >
            重置
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
            <span className="page-title-icon"><UserOutlined /></span>
            <Typography.Title level={1}>用户管理</Typography.Title>
          </div>
          <Typography.Text>
            管理平台用户账号，包括创建、角色分配、岗位关联和密码管理。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-users'] })}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增用户
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索用户名、姓名、手机号"
            allowClear
            style={{ width: 280 }}
            value={query.keyword}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 }))
            }
            onPressEnter={() => queryClient.invalidateQueries({ queryKey: ['system-users'] })}
          />
          <Select
            placeholder="用户类型"
            allowClear
            style={{ width: 140 }}
            value={query.userType || undefined}
            onChange={(v) => setQuery((prev) => ({ ...prev, userType: v || '', page: 1 }))}
            options={[
              { label: '平台管理员', value: 'platform_admin' },
              { label: '租户管理员', value: 'tenant_admin' },
              { label: '普通用户', value: 'normal' },
            ]}
          />
          <Select
            placeholder="状态"
            allowClear
            style={{ width: 120 }}
            value={query.status || undefined}
            onChange={(v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 }))}
            options={[
              { label: '正常', value: 'active' },
              { label: '锁定', value: 'locked' },
              { label: '停用', value: 'disabled' },
            ]}
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-users'] })}
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
          scroll={{ x: 1300 }}
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

      {/* User modal */}
      <Modal
        title={editingId ? '编辑用户' : '新增用户'}
        open={modalOpen}
        width={600}
        onOk={handleSubmit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item
              name="username"
              label="用户名"
              rules={[{ required: true, message: '请输入用户名' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="登录用户名" maxLength={64} disabled={!!editingId} />
            </Form.Item>
            <Form.Item
              name="displayName"
              label="姓名"
              rules={[{ required: true, message: '请输入姓名' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="显示姓名" maxLength={64} />
            </Form.Item>
          </Space>
          {!editingId && (
            <Form.Item
              name="password"
              label="密码"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码至少8位' },
              ]}
            >
              <Input.Password placeholder="请输入密码（至少8位）" maxLength={128} />
            </Form.Item>
          )}
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item
              name="userType"
              label="用户类型"
              rules={[{ required: true, message: '请选择用户类型' }]}
              style={{ flex: 1 }}
            >
              <Select
                options={[
                  { label: '平台管理员', value: 'platform_admin' },
                  { label: '租户管理员', value: 'tenant_admin' },
                  { label: '普通用户', value: 'normal' },
                ]}
              />
            </Form.Item>
            <Form.Item name="status" label="状态" style={{ flex: 1 }}>
              <Select
                options={[
                  { label: '正常', value: 'active' },
                  { label: '锁定', value: 'locked' },
                  { label: '停用', value: 'disabled' },
                ]}
              />
            </Form.Item>
          </Space>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item name="orgId" label="所属组织" style={{ flex: 1 }}>
              <TreeSelect
                placeholder="选择所属组织"
                allowClear
                treeDefaultExpandAll
                treeData={orgTreeData}
              />
            </Form.Item>
            <Form.Item name="postId" label="岗位" style={{ flex: 1 }}>
              <Select
                placeholder="选择岗位"
                allowClear
                options={posts.map((p: PostVO) => ({ label: p.name, value: p.id }))}
              />
            </Form.Item>
          </Space>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item name="phone" label="手机号" style={{ flex: 1 }}>
              <Input placeholder="手机号" maxLength={32} />
            </Form.Item>
            <Form.Item name="email" label="邮箱" style={{ flex: 1 }}>
              <Input placeholder="邮箱地址" maxLength={128} />
            </Form.Item>
          </Space>
          <Form.Item name="roleIds" label="角色">
            <Select
              mode="multiple"
              placeholder="选择角色（可多选）"
              options={roles.map((r: RoleVO) => ({ label: `${r.name} (${r.code})`, value: r.id }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Reset password modal */}
      <Modal
        title="重置密码"
        open={pwdModalOpen}
        width={400}
        onOk={handleResetPassword}
        onCancel={() => {
          setPwdModalOpen(false);
          setPwdUserId(null);
          pwdForm.resetFields();
        }}
        confirmLoading={resetPwdMutation.isPending}
        destroyOnClose
      >
        <Form form={pwdForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码至少8位' },
            ]}
          >
            <Input.Password placeholder="请输入新密码（至少8位）" maxLength={128} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

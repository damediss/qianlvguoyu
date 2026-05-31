import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import {
  Button,
  Checkbox,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tree,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo } from 'react';
import {
  pageRoles,
  createRole,
  updateRole,
  deleteRole,
  getRole,
  bindRolePermissions,
  getMenuTree,
  getPermissionMatrix,
} from '../../api/system';
import type { RoleVO, MenuTreeVO, PermissionMatrixVO } from '../../types/system';

function buildMenuCheckTree(
  menus: MenuTreeVO[],
  checkedIds: Set<number>,
): { data: any[]; checkedKeys: number[] } {
  const checkedKeys: number[] = [];

  function walk(nodes: MenuTreeVO[]): any[] {
    return nodes.map((node) => {
      if (checkedIds.has(node.id)) {
        checkedKeys.push(node.id);
      }
      return {
        key: node.id,
        title: node.name,
        children: node.children?.length ? walk(node.children) : undefined,
      };
    });
  }

  const data = walk(menus);
  return { data, checkedKeys };
}

export function RolePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', roleType: '', status: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [permDrawerOpen, setPermDrawerOpen] = useState(false);
  const [permRoleId, setPermRoleId] = useState<number | null>(null);
  const [permRoleDetail, setPermRoleDetail] = useState<RoleVO | null>(null);
  const [checkedMenuIds, setCheckedMenuIds] = useState<number[]>([]);
  const [checkedPermIds, setCheckedPermIds] = useState<Set<number>>(new Set());
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['system-roles', query],
    queryFn: () => pageRoles(query),
  });

  const menuTreeQuery = useQuery({
    queryKey: ['system-menus-tree'],
    queryFn: getMenuTree,
  });

  const permMatrixQuery = useQuery({
    queryKey: ['system-perm-matrix'],
    queryFn: getPermissionMatrix,
  });

  const createMutation = useMutation({
    mutationFn: createRole,
    onSuccess: () => {
      message.success('角色已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-roles'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Parameters<typeof updateRole>[1] }) =>
      updateRole(id, payload),
    onSuccess: () => {
      message.success('角色已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-roles'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteRole,
    onSuccess: () => {
      message.success('角色已删除');
      queryClient.invalidateQueries({ queryKey: ['system-roles'] });
    },
  });

  const bindMutation = useMutation({
    mutationFn: () =>
      bindRolePermissions(permRoleId!, {
        menuIds: checkedMenuIds,
        permissionIds: Array.from(checkedPermIds),
      }),
    onSuccess: () => {
      message.success('权限绑定已保存');
      setPermDrawerOpen(false);
      setPermRoleId(null);
      setPermRoleDetail(null);
      queryClient.invalidateQueries({ queryKey: ['system-roles'] });
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 'enabled', roleType: 'custom' });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (record: RoleVO) => {
      setEditingId(record.id);
      form.setFieldsValue({
        code: record.code,
        name: record.name,
        roleType: record.roleType,
        status: record.status,
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
    (record: RoleVO) => {
      Modal.confirm({
        title: `确认删除角色「${record.name}」？`,
        content: '删除后不可恢复，已绑定该角色的用户将失去对应权限。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteMutation.mutate(record.id),
      });
    },
    [deleteMutation],
  );

  const openPermDrawer = useCallback(
    async (record: RoleVO) => {
      setPermRoleId(record.id);
      try {
        const detail = await getRole(record.id);
        setPermRoleDetail(detail);
        setCheckedMenuIds(detail.menuIds || []);
        setCheckedPermIds(new Set(detail.permissionIds || []));
      } catch {
        setCheckedMenuIds([]);
        setCheckedPermIds(new Set());
      }
      setPermDrawerOpen(true);
    },
    [],
  );

  const handleMenuCheck = useCallback((keys: any) => {
    setCheckedMenuIds(keys as number[]);
  }, []);

  const handlePermToggle = useCallback((permId: number, checked: boolean) => {
    setCheckedPermIds((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(permId);
      } else {
        next.delete(permId);
      }
      return next;
    });
  }, []);

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;
  const menuTree = menuTreeQuery.data || [];
  const permMatrix = permMatrixQuery.data || [];

  const { data: menuCheckTreeData } = useMemo(
    () => buildMenuCheckTree(menuTree, new Set(checkedMenuIds)),
    [menuTree, checkedMenuIds],
  );

  const columns = [
    {
      title: '序号',
      width: 70,
      render: (_: unknown, __: unknown, index: number) => index + 1,
    },
    {
      title: '角色编码',
      dataIndex: 'code',
      width: 160,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '角色名称',
      dataIndex: 'name',
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '角色类型',
      dataIndex: 'roleType',
      width: 110,
      render: (v: string) => {
        const map: Record<string, { color: string; text: string }> = {
          platform: { color: 'purple', text: '平台角色' },
          tenant: { color: 'blue', text: '租户角色' },
          custom: { color: 'green', text: '自定义' },
        };
        const item = map[v] || { color: 'default', text: v };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: string) =>
        v === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
      render: (v: string) => v || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (v: string) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      width: 220,
      render: (_: unknown, record: RoleVO) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<SafetyOutlined />}
            onClick={() => openPermDrawer(record)}
          >
            权限
          </Button>
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
            <span className="page-title-icon"><SafetyOutlined /></span>
            <Typography.Title level={1}>角色管理</Typography.Title>
          </div>
          <Typography.Text>
            管理系统角色及其权限绑定，控制不同角色的菜单和功能访问权限。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-roles'] })}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增角色
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索角色名称或编码"
            allowClear
            style={{ width: 260 }}
            value={query.keyword}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 }))
            }
            onPressEnter={() => queryClient.invalidateQueries({ queryKey: ['system-roles'] })}
          />
          <Select
            placeholder="角色类型"
            allowClear
            style={{ width: 130 }}
            value={query.roleType || undefined}
            onChange={(v) => setQuery((prev) => ({ ...prev, roleType: v || '', page: 1 }))}
            options={[
              { label: '平台角色', value: 'platform' },
              { label: '租户角色', value: 'tenant' },
              { label: '自定义', value: 'custom' },
            ]}
          />
          <Select
            placeholder="状态"
            allowClear
            style={{ width: 120 }}
            value={query.status || undefined}
            onChange={(v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 }))}
            options={[
              { label: '启用', value: 'enabled' },
              { label: '停用', value: 'disabled' },
            ]}
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-roles'] })}
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

      {/* Role form modal */}
      <Modal
        title={editingId ? '编辑角色' : '新增角色'}
        open={modalOpen}
        width={520}
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
              name="code"
              label="角色编码"
              rules={[{ required: true, message: '请输入角色编码' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="如 PLATFORM_ADMIN" maxLength={64} disabled={!!editingId} />
            </Form.Item>
            <Form.Item
              name="name"
              label="角色名称"
              rules={[{ required: true, message: '请输入角色名称' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="如 平台管理员" maxLength={64} />
            </Form.Item>
          </Space>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item
              name="roleType"
              label="角色类型"
              rules={[{ required: true, message: '请选择角色类型' }]}
              style={{ flex: 1 }}
            >
              <Select
                options={[
                  { label: '平台角色', value: 'platform' },
                  { label: '租户角色', value: 'tenant' },
                  { label: '自定义', value: 'custom' },
                ]}
              />
            </Form.Item>
            <Form.Item name="status" label="状态" style={{ flex: 1 }}>
              <Select
                options={[
                  { label: '启用', value: 'enabled' },
                  { label: '停用', value: 'disabled' },
                ]}
              />
            </Form.Item>
          </Space>
          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="备注信息（可选）" maxLength={512} rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Permission binding drawer */}
      <Drawer
        title={permRoleDetail ? `权限配置 - ${permRoleDetail.name}` : '权限配置'}
        open={permDrawerOpen}
        width={640}
        onClose={() => {
          setPermDrawerOpen(false);
          setPermRoleId(null);
          setPermRoleDetail(null);
        }}
        extra={
          <Button
            type="primary"
            loading={bindMutation.isPending}
            onClick={() => bindMutation.mutate()}
          >
            保存权限
          </Button>
        }
      >
        <div style={{ marginBottom: 24 }}>
          <Typography.Title level={5} style={{ marginBottom: 12 }}>
            菜单权限
          </Typography.Title>
          <div
            style={{
              border: '1px solid #e2e8f0',
              borderRadius: 8,
              padding: 12,
              maxHeight: 280,
              overflow: 'auto',
            }}
          >
            {menuCheckTreeData.length > 0 ? (
              <Tree
                checkable
                defaultExpandAll
                checkedKeys={checkedMenuIds}
                onCheck={handleMenuCheck}
                treeData={menuCheckTreeData}
              />
            ) : (
              <Typography.Text type="secondary">暂无菜单数据</Typography.Text>
            )}
          </div>
        </div>

        <div>
          <Typography.Title level={5} style={{ marginBottom: 12 }}>
            功能权限
          </Typography.Title>
          {permMatrix.length > 0 ? (
            permMatrix.map((group: PermissionMatrixVO) => (
              <div
                key={group.menuId}
                style={{
                  marginBottom: 16,
                  border: '1px solid #e2e8f0',
                  borderRadius: 8,
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    padding: '8px 14px',
                    background: '#f8fafc',
                    borderBottom: '1px solid #e2e8f0',
                    fontWeight: 600,
                    fontSize: 13,
                    color: '#475569',
                  }}
                >
                  {group.menuName}
                </div>
                <div style={{ padding: '8px 14px' }}>
                  <Space wrap>
                    {group.permissions.map((perm) => (
                      <Checkbox
                        key={perm.id}
                        checked={checkedPermIds.has(perm.id)}
                        onChange={(e) => handlePermToggle(perm.id, e.target.checked)}
                      >
                        {perm.name}
                      </Checkbox>
                    ))}
                  </Space>
                </div>
              </div>
            ))
          ) : (
            <Typography.Text type="secondary">暂无权限数据</Typography.Text>
          )}
        </div>
      </Drawer>
    </div>
  );
}

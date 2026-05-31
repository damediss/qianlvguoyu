import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  TreeSelect,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import {
  getMenuTree,
  createMenu,
  updateMenu,
  deleteMenu,
} from '../../api/system';
import type { MenuTreeVO, MenuCreateReq } from '../../types/system';

function buildTreeSelectData(nodes: MenuTreeVO[], excludeId?: number): any[] {
  return nodes
    .filter((n) => !excludeId || n.id !== excludeId)
    .map((node) => ({
      title: node.name,
      value: node.id,
      children: node.children?.length
        ? buildTreeSelectData(node.children, excludeId)
        : undefined,
    }));
}

const MENU_TYPE_OPTIONS = [
  { label: '目录', value: 'directory' },
  { label: '菜单', value: 'menu' },
  { label: '按钮', value: 'button' },
];

const MENU_TYPE_TAG: Record<string, { color: string; text: string }> = {
  directory: { color: 'blue', text: '目录' },
  menu: { color: 'green', text: '菜单' },
  button: { color: 'orange', text: '按钮' },
};

export function MenuPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const treeQuery = useQuery({
    queryKey: ['system-menus-tree'],
    queryFn: getMenuTree,
  });

  const createMutation = useMutation({
    mutationFn: createMenu,
    onSuccess: () => {
      message.success('菜单已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: MenuCreateReq }) =>
      updateMenu(id, payload),
    onSuccess: () => {
      message.success('菜单已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteMenu,
    onSuccess: () => {
      message.success('菜单已删除');
      queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
    },
  });

  const openCreate = useCallback(
    (parentId?: number) => {
      setEditingId(null);
      form.resetFields();
      form.setFieldsValue({
        parentId: parentId ?? 0,
        menuType: 'menu',
        sortOrder: 0,
        visible: 1,
        status: 'enabled',
      });
      setModalOpen(true);
    },
    [form],
  );

  const openEdit = useCallback(
    (record: MenuTreeVO) => {
      setEditingId(record.id);
      form.setFieldsValue({
        parentId: record.parentId,
        menuType: record.menuType,
        code: record.code,
        name: record.name,
        path: record.path,
        component: record.component,
        permissionCode: record.permissionCode,
        icon: record.icon,
        sortOrder: record.sortOrder,
        visible: record.visible,
        status: record.status,
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
    (record: MenuTreeVO) => {
      Modal.confirm({
        title: `确认删除菜单「${record.name}」？`,
        content: '将同时删除该菜单下的所有子级。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteMutation.mutate(record.id),
      });
    },
    [deleteMutation],
  );

  const treeData = treeQuery.data || [];
  const treeSelectData = buildTreeSelectData(treeData, editingId || undefined);

  const columns = [
    {
      title: '菜单名称',
      dataIndex: 'name',
      width: 220,
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '类型',
      dataIndex: 'menuType',
      width: 80,
      render: (v: string) => {
        const item = MENU_TYPE_TAG[v] || { color: 'default', text: v };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '编码',
      dataIndex: 'code',
      width: 160,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '路径',
      dataIndex: 'path',
      width: 160,
      ellipsis: true,
      render: (v: string) => v || '-',
    },
    {
      title: '权限码',
      dataIndex: 'permissionCode',
      width: 140,
      ellipsis: true,
      render: (v: string) => v ? <Typography.Text code>{v}</Typography.Text> : '-',
    },
    {
      title: '图标',
      dataIndex: 'icon',
      width: 80,
      render: (v: string) => v || '-',
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 70,
    },
    {
      title: '可见',
      dataIndex: 'visible',
      width: 70,
      render: (v: number) => (v === 1 ? <Tag color="success">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: string) =>
        v === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '操作',
      width: 200,
      render: (_: unknown, record: MenuTreeVO) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => openCreate(record.id)}
          >
            子级
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
            <span className="page-title-icon"><AppstoreOutlined /></span>
            <Typography.Title level={1}>菜单管理</Typography.Title>
          </div>
          <Typography.Text>
            维护系统菜单的树形结构，包括目录、页面菜单和操作按钮。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] })}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate(0)}>
            新增菜单
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Table
          rowKey="id"
          size="middle"
          loading={treeQuery.isLoading}
          dataSource={treeData}
          columns={columns}
          pagination={false}
          expandable={{ defaultExpandAllRows: true }}
        />
      </div>

      <Modal
        title={editingId ? '编辑菜单' : '新增菜单'}
        open={modalOpen}
        width={580}
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
          <Form.Item name="parentId" label="上级菜单">
            <TreeSelect
              placeholder="选择上级菜单（不选则为顶级）"
              allowClear
              treeDefaultExpandAll
              treeData={[
                { title: '顶级菜单', value: 0, children: treeSelectData },
              ]}
            />
          </Form.Item>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item
              name="menuType"
              label="菜单类型"
              rules={[{ required: true, message: '请选择菜单类型' }]}
              style={{ flex: 1 }}
            >
              <Select options={MENU_TYPE_OPTIONS} />
            </Form.Item>
            <Form.Item
              name="code"
              label="菜单编码"
              rules={[{ required: true, message: '请输入菜单编码' }]}
              style={{ flex: 1 }}
            >
              <Input placeholder="如 system_user" maxLength={64} disabled={!!editingId} />
            </Form.Item>
          </Space>
          <Form.Item
            name="name"
            label="菜单名称"
            rules={[{ required: true, message: '请输入菜单名称' }]}
          >
            <Input placeholder="请输入菜单名称" maxLength={64} />
          </Form.Item>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item name="path" label="路由路径" style={{ flex: 1 }}>
              <Input placeholder="如 /system/users" maxLength={255} />
            </Form.Item>
            <Form.Item name="component" label="组件路径" style={{ flex: 1 }}>
              <Input placeholder="如 system/UserPage" maxLength={255} />
            </Form.Item>
          </Space>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item name="permissionCode" label="权限码" style={{ flex: 1 }}>
              <Input placeholder="如 user:view" maxLength={128} />
            </Form.Item>
            <Form.Item name="icon" label="图标" style={{ flex: 1 }}>
              <Input placeholder="图标名称" maxLength={64} />
            </Form.Item>
          </Space>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item name="sortOrder" label="排序" style={{ flex: 1 }}>
              <InputNumber min={0} max={9999} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="visible" label="是否可见" valuePropName="checked" style={{ flex: 1 }}>
              <Switch checkedChildren="可见" unCheckedChildren="隐藏" />
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
        </Form>
      </Modal>
    </div>
  );
}

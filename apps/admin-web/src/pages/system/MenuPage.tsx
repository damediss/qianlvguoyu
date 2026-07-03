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
import { useState, useCallback, useRef } from 'react';
import {
  getMenuTree,
  createMenu,
  updateMenu,
  deleteMenu,
  batchUpdateMenuVisible,
} from '../../api/system';
import type { MenuTreeVO, MenuUpdateReq } from '../../types/system';
import { IconPicker } from '../../components/IconPicker';
import { usePinyinAutoGen } from '../../hooks/usePinyinAutoGen';
import { iconFromName } from '../../utils/icons';

/** ID type that handles both safe integers and large snowflake IDs */
type Id = number | string;

function buildTreeSelectData(nodes: MenuTreeVO[], excludeId?: Id): any[] {
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

/** Collect all descendant IDs from a tree node. */
function collectDescendantIds(nodes: MenuTreeVO[], targetId: Id): Id[] {
  const findNode = (list: MenuTreeVO[]): MenuTreeVO | null => {
    for (const node of list) {
      if (node.id === targetId) return node;
      if (node.children?.length) {
        const found = findNode(node.children);
        if (found) return found;
      }
    }
    return null;
  };

  const collectIds = (node: MenuTreeVO): Id[] => {
    const ids: Id[] = [];
    if (node.children?.length) {
      for (const child of node.children) {
        ids.push(child.id);
        ids.push(...collectIds(child));
      }
    }
    return ids;
  };

  const targetNode = findNode(nodes);
  if (!targetNode) return [];
  return collectIds(targetNode);
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
  const [editingId, setEditingId] = useState<Id | null>(null);
  const [form] = Form.useForm();
  const menuType = Form.useWatch('menuType', form);
  const originalVisibleRef = useRef<number | null>(null);

  const treeQuery = useQuery({
    queryKey: ['system-menus-tree'],
    queryFn: getMenuTree,
  });

  const treeData = treeQuery.data || [];

  const { onNameChange } = usePinyinAutoGen(form, treeData);

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
    mutationFn: ({ id, payload }: { id: Id; payload: MenuUpdateReq }) =>
      updateMenu(id, payload),
    onSuccess: () => {
      message.success('菜单已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
    },
  });

  const batchVisibleMutation = useMutation({
    mutationFn: batchUpdateMenuVisible,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (input: number | string | { id: number | string; cascade: boolean }) => {
      if (typeof input === 'object' && 'cascade' in input) {
        return deleteMenu(input.id, { cascade: input.cascade });
      }
      return deleteMenu(input as number | string);
    },
    onSuccess: (data, variables) => {
      const isCascade =
        typeof variables === 'object' && 'cascade' in variables && variables.cascade;
      if (isCascade) {
        const count = typeof data === 'number' ? data : 1;
        message.success(`已级联删除 ${count} 个菜单`);
      } else {
        message.success('菜单已删除');
      }
      queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
    },
  });

  const openCreate = useCallback(
    (parentId?: Id) => {
      setEditingId(null);
      originalVisibleRef.current = null;
      form.resetFields();
      form.setFieldsValue({
        parentId: parentId ?? 0,
        menuType: 'menu',
        sortOrder: 0,
        visible: 1,
        expanded: 1,
        status: 'enabled',
      });
      setModalOpen(true);
    },
    [form],
  );

  const openEdit = useCallback(
    (record: MenuTreeVO) => {
      setEditingId(record.id);
      originalVisibleRef.current = record.visible;
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
        expanded: record.expanded ?? 1,
        status: record.status,
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(() => {
    form.validateFields().then((values) => {
      // Switch produces boolean; backend expects Integer (0/1)
      const newVisible = typeof values.visible === 'boolean' ? (values.visible ? 1 : 0) : values.visible;
      const newExpanded = typeof values.expanded === 'boolean' ? (values.expanded ? 1 : 0) : values.expanded;
      const payload = { ...values, visible: newVisible, expanded: newExpanded };

      // Check if we need to cascade visibility for directory type
      if (
        editingId &&
        values.menuType === 'directory' &&
        originalVisibleRef.current !== null &&
        newVisible !== originalVisibleRef.current
      ) {
        const descendantIds = collectDescendantIds(treeData, editingId);
        if (descendantIds.length > 0) {
          Modal.confirm({
            title: '级联修改可见性',
            content: `此操作将同时修改 ${descendantIds.length} 个子菜单的可见性，是否继续？`,
            okText: '确认',
            cancelText: '取消',
            onOk: () => {
              Promise.all([
                updateMutation.mutateAsync({ id: editingId, payload }),
                batchVisibleMutation.mutateAsync({ ids: descendantIds, visible: newVisible }),
              ]).then(() => {
                message.success(`已更新菜单及 ${descendantIds.length} 个子菜单的可见性`);
                setModalOpen(false);
                setEditingId(null);
                form.resetFields();
                queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
              });
            },
          });
          return;
        }
      }

      if (editingId) {
        updateMutation.mutate({ id: editingId, payload });
      } else {
        createMutation.mutate(payload);
      }
    });
  }, [form, editingId, treeData, createMutation, updateMutation, batchVisibleMutation, queryClient]);

  const handleDelete = useCallback(
    (record: MenuTreeVO) => {
      const descendantIds = collectDescendantIds(treeData, record.id);
      const hasChildren = descendantIds.length > 0;

      if (!hasChildren) {
        // 叶子菜单：直接走默认 delete
        Modal.confirm({
          title: `确认删除菜单「${record.name}」？`,
          content: '该菜单为叶子节点，无子菜单。',
          okText: '确认删除',
          okType: 'danger',
          cancelText: '取消',
          onOk: () => deleteMutation.mutate(record.id),
        });
        return;
      }

      // 父菜单：弹级联确认，传 cascade=true
      const names = descendantIds
        .slice(0, 5)
        .map((id) => {
          const findName = (list: MenuTreeVO[]): string | null => {
            for (const n of list) {
              if (n.id === id) return n.name;
              if (n.children?.length) {
                const r = findName(n.children);
                if (r) return r;
              }
            }
            return null;
          };
          return findName(treeData) || `#${id}`;
        });
      const moreCount = descendantIds.length - names.length;
      const namePreview = names.join('、') + (moreCount > 0 ? ` 等 ${moreCount} 个` : '');

      Modal.confirm({
        title: `级联删除菜单「${record.name}」？`,
        content: (
          <div>
            <p style={{ marginBottom: 8, color: '#dc2626', fontWeight: 500 }}>
              该菜单下还有 {descendantIds.length} 个子菜单，删除后将级联清空。
            </p>
            <p style={{ marginBottom: 8, color: '#475569' }}>子菜单：{namePreview}</p>
            <p style={{ marginBottom: 0, color: '#94a3b8', fontSize: 12 }}>
              同时会解除所有角色对该菜单及其子菜单的绑定，操作不可恢复。
            </p>
          </div>
        ),
        okText: '级联删除',
        okType: 'danger',
        cancelText: '取消',
        width: 480,
        onOk: () => deleteMutation.mutate({ id: record.id, cascade: true } as any),
      });
    },
    [deleteMutation, treeData],
  );

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
      render: (v: string) => v ? <span title={v}>{iconFromName(v) || v}</span> : '-',
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
          originalVisibleRef.current = null;
          form.resetFields();
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnHidden
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
              name="name"
              label="菜单名称"
              rules={[{ required: true, message: '请输入菜单名称' }]}
              style={{ flex: 1 }}
            >
              <Input
                placeholder="请输入菜单名称"
                maxLength={64}
                onChange={(e) => {
                  // Auto-generate fields only in create mode
                  if (!editingId) {
                    onNameChange(e.target.value);
                  }
                }}
              />
            </Form.Item>
          </Space>
          <Form.Item
            name="code"
            label="菜单编码"
            rules={[{ required: true, message: '请输入菜单编码' }]}
          >
            <Input placeholder="如 system_user" maxLength={64} disabled={!!editingId} />
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
              <IconPicker placeholder="选择图标" />
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
            {menuType === 'directory' && (
              <Form.Item name="expanded" label="默认展开" valuePropName="checked" style={{ flex: 1 }}>
                <Switch checkedChildren="是" unCheckedChildren="否" />
              </Form.Item>
            )}
          </Space>
        </Form>
      </Modal>
    </div>
  );
}

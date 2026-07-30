import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  BankOutlined,
} from '@ant-design/icons';
import {
  Button,
  Empty,
  Form,
  Input,
  InputNumber,
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
import { useState, useCallback } from 'react';
import {
  getOrgTree,
  createOrg,
  updateOrg,
  deleteOrg,
} from '../../api/system';
import type { OrgTreeVO, OrgCreateReq } from '../../types/system';

function buildTreeData(nodes: OrgTreeVO[]): any[] {
  return nodes.map((node) => ({
    key: node.id,
    title: node.name,
    code: node.code,
    orgType: node.orgType,
    sortOrder: node.sortOrder,
    status: node.status,
    parentId: node.parentId,
    children: node.children?.length ? buildTreeData(node.children) : undefined,
  }));
}

function buildTreeSelectData(nodes: OrgTreeVO[], excludeId?: number): any[] {
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

function flattenTreeForSelect(nodes: OrgTreeVO[]): OrgTreeVO[] {
  const result: OrgTreeVO[] = [];
  function walk(list: OrgTreeVO[]) {
    for (const n of list) {
      result.push(n);
      if (n.children?.length) walk(n.children);
    }
  }
  walk(nodes);
  return result;
}

export function OrgPage() {
  const queryClient = useQueryClient();
  const [selectedOrg, setSelectedOrg] = useState<OrgTreeVO | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const treeQuery = useQuery({
    queryKey: ['system-orgs-tree'],
    queryFn: getOrgTree,
  });

  const createMutation = useMutation({
    mutationFn: createOrg,
    onSuccess: () => {
      message.success('组织已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: OrgCreateReq }) =>
      updateOrg(id, payload),
    onSuccess: () => {
      message.success('组织已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      setSelectedOrg(null);
      queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteOrg,
    onSuccess: () => {
      message.success('组织已删除');
      setSelectedOrg(null);
      queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] });
    },
  });

  const openCreate = useCallback(
    (parentId?: number) => {
      setEditingId(null);
      form.resetFields();
      form.setFieldsValue({
        parentId: parentId ?? 0,
        orgType: 'department',
        status: 'enabled',
        sortOrder: 0,
      });
      setModalOpen(true);
    },
    [form],
  );

  const openEdit = useCallback(
    (record: OrgTreeVO) => {
      setEditingId(record.id);
      form.setFieldsValue({
        parentId: record.parentId,
        orgType: record.orgType,
        code: record.code,
        name: record.name,
        sortOrder: record.sortOrder,
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
    (record: OrgTreeVO) => {
      Modal.confirm({
        title: `确认删除组织「${record.name}」？`,
        content: '将同时删除该组织下的所有子级组织。',
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteMutation.mutate(record.id),
      });
    },
    [deleteMutation],
  );

  const treeData = treeQuery.data || [];
  const antTreeData = buildTreeData(treeData);

  const tableColumns = [
    {
      title: '组织名称',
      dataIndex: 'name',
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '编码',
      dataIndex: 'code',
      width: 140,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '类型',
      dataIndex: 'orgType',
      width: 100,
      render: (v: string) => {
        const map: Record<string, { color: string; text: string }> = {
          company: { color: 'blue', text: '公司' },
          department: { color: 'green', text: '部门' },
          team: { color: 'orange', text: '小组' },
        };
        const item = map[v] || { color: 'default', text: v };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 70,
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
      width: 180,
      render: (_: unknown, record: OrgTreeVO) => (
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
            <span className="page-title-icon"><BankOutlined /></span>
            <Typography.Title level={1}>组织管理</Typography.Title>
          </div>
          <Typography.Text>
            维护公司组织架构的树形结构，包括公司、部门和小组。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] })}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate(0)}>
            新增组织
          </Button>
        </Space>
      </section>

      <div style={{ display: 'grid', gridTemplateColumns: '280px 1fr', gap: 16, padding: '0 24px' }}>
        {/* Left: Tree navigation */}
        <div
          style={{
            border: '1px solid #e2e8f0',
            borderRadius: 10,
            padding: 16,
            background: '#fff',
            minHeight: 400,
          }}
        >
          <Typography.Text strong style={{ display: 'block', marginBottom: 12, fontSize: 14 }}>
            组织架构
          </Typography.Text>
          {antTreeData.length > 0 ? (
            <Tree
              treeData={antTreeData}
              defaultExpandAll
              selectedKeys={selectedOrg ? [selectedOrg.id] : []}
              onSelect={(keys, info) => {
                if (keys.length > 0) {
                  setSelectedOrg(info.node as unknown as OrgTreeVO);
                }
              }}
            />
          ) : (
            <Empty description="暂无组织数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          )}
        </div>

        {/* Right: Detail table */}
        <div
          style={{
            border: '1px solid #e2e8f0',
            borderRadius: 10,
            padding: 16,
            background: '#fff',
            minHeight: 400,
          }}
        >
          {selectedOrg ? (
            <>
              <div style={{ marginBottom: 16 }}>
                <Typography.Title level={4} style={{ margin: 0 }}>
                  {selectedOrg.name}
                </Typography.Title>
                <Typography.Text type="secondary">
                  编码: {selectedOrg.code} | 类型: {selectedOrg.orgType}
                </Typography.Text>
              </div>
              <Table
                rowKey="id"
                size="small"
                dataSource={selectedOrg.children || []}
                columns={tableColumns}
                pagination={false}
                expandable={{ defaultExpandAllRows: true }}
              />
            </>
          ) : (
            <div style={{ display: 'grid', placeItems: 'center', minHeight: 300 }}>
              <Empty description="请在左侧选择一个组织节点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </div>
          )}
        </div>
      </div>

      <Modal
        title={editingId ? '编辑组织' : '新增组织'}
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
          <Form.Item name="parentId" label="上级组织">
            <Input type="hidden" />
            <Select
              placeholder="选择上级组织（不选则为顶级）"
              allowClear
              treeDefaultExpandAll
              options={[
                { label: '顶级组织', value: 0 },
                ...buildTreeSelectData(treeData, editingId || undefined),
              ]}
              showSearch
              filterOption={(input, option) =>
                String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </Form.Item>
          <Form.Item
            name="orgType"
            label="组织类型"
            rules={[{ required: true, message: '请选择组织类型' }]}
          >
            <Select
              options={[
                { label: '公司', value: 'company' },
                { label: '部门', value: 'department' },
                { label: '小组', value: 'team' },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="code"
            label="组织编码"
            rules={[{ required: true, message: '请输入组织编码' }]}
          >
            <Input placeholder="请输入唯一编码" maxLength={64} disabled={!!editingId} />
          </Form.Item>
          <Form.Item
            name="name"
            label="组织名称"
            rules={[{ required: true, message: '请输入组织名称' }]}
          >
            <Input placeholder="请输入组织名称" maxLength={128} />
          </Form.Item>
          <Space size={16} style={{ width: '100%' }}>
            <Form.Item name="sortOrder" label="排序" style={{ flex: 1 }}>
              <InputNumber min={0} max={9999} style={{ width: '100%' }} />
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

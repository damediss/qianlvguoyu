import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  BookOutlined,
} from '@ant-design/icons';
import {
  Button,
  Drawer,
  Form,
  Input,
  InputNumber,
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
  pageDicts,
  createDict,
  updateDict,
  getDictItems,
  createDictItem,
  updateDictItem,
} from '../../api/system';
import type { DictVO, DictItemVO } from '../../types/system';

export function DictPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', status: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [drawerDict, setDrawerDict] = useState<DictVO | null>(null);
  const [itemModalOpen, setItemModalOpen] = useState(false);
  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [form] = Form.useForm();
  const [itemForm] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['system-dicts', query],
    queryFn: () => pageDicts(query),
  });

  const itemsQuery = useQuery({
    queryKey: ['system-dict-items', drawerDict?.code],
    queryFn: () => getDictItems(drawerDict!.code),
    enabled: !!drawerDict,
  });

  const createMutation = useMutation({
    mutationFn: createDict,
    onSuccess: () => {
      message.success('字典已创建');
      setModalOpen(false);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-dicts'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Parameters<typeof updateDict>[1] }) =>
      updateDict(id, payload),
    onSuccess: () => {
      message.success('字典已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-dicts'] });
    },
  });

  const createItemMutation = useMutation({
    mutationFn: (payload: Parameters<typeof createDictItem>[1]) =>
      createDictItem(drawerDict!.id, payload),
    onSuccess: () => {
      message.success('字典项已创建');
      setItemModalOpen(false);
      itemForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-dict-items'] });
    },
  });

  const updateItemMutation = useMutation({
    mutationFn: ({ itemId, payload }: { itemId: number; payload: Parameters<typeof updateDictItem>[1] }) =>
      updateDictItem(itemId, payload),
    onSuccess: () => {
      message.success('字典项已更新');
      setItemModalOpen(false);
      setEditingItemId(null);
      itemForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['system-dict-items'] });
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 'enabled' });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (record: DictVO) => {
      setEditingId(record.id);
      form.setFieldsValue({
        code: record.code,
        name: record.name,
        value: record.value,
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

  const openItemCreate = useCallback(() => {
    setEditingItemId(null);
    itemForm.resetFields();
    itemForm.setFieldsValue({ status: 'enabled', sortOrder: 0 });
    setItemModalOpen(true);
  }, [itemForm]);

  const openItemEdit = useCallback(
    (record: DictItemVO) => {
      setEditingItemId(record.id);
      itemForm.setFieldsValue({
        itemValue: record.itemValue,
        itemLabel: record.itemLabel,
        sortOrder: record.sortOrder,
        status: record.status,
      });
      setItemModalOpen(true);
    },
    [itemForm],
  );

  const handleItemSubmit = useCallback(() => {
    itemForm.validateFields().then((values) => {
      if (editingItemId) {
        updateItemMutation.mutate({ itemId: editingItemId, payload: values });
      } else {
        createItemMutation.mutate(values);
      }
    });
  }, [itemForm, editingItemId, createItemMutation, updateItemMutation]);

  const dictItems = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;
  const detailItems = itemsQuery.data || [];

  const columns = [
    {
      title: '序号',
      width: 80,
      render: (_: unknown, __: unknown, index: number) => index + 1,
    },
    {
      title: '字典编码',
      dataIndex: 'code',
      width: 180,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '字典名称',
      dataIndex: 'name',
      width: 150,
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '字典值',
      dataIndex: 'value',
      ellipsis: true,
      render: (text: string) => text ? <Typography.Text>{text}</Typography.Text> : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v: string) =>
        v === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (v: string) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      width: 200,
      render: (_: unknown, record: DictVO) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<BookOutlined />}
            onClick={() => setDrawerDict(record)}
          >
            字典项
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          >
            编辑
          </Button>
        </Space>
      ),
    },
  ];

  const itemColumns = [
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 70,
    },
    {
      title: '值',
      dataIndex: 'itemValue',
      ellipsis: { showTitle: false },
      render: (text: string) => (
        <Typography.Text code ellipsis={{ tooltip: text }}>
          {text}
        </Typography.Text>
      ),
    },
    {
      title: '标签',
      dataIndex: 'itemLabel',
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
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
      width: 80,
      render: (_: unknown, record: DictItemVO) => (
        <Button
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={() => openItemEdit(record)}
        >
          编辑
        </Button>
      ),
    },
  ];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><BookOutlined /></span>
            <Typography.Title level={1}>字典管理</Typography.Title>
          </div>
          <Typography.Text>
            维护系统数据字典及其选项值，用于下拉选择、枚举值等场景。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-dicts'] })}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增字典
          </Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Space style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索字典名称或编码"
            allowClear
            style={{ width: 280 }}
            value={query.keyword}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 }))
            }
            onPressEnter={() => queryClient.invalidateQueries({ queryKey: ['system-dicts'] })}
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
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-dicts'] })}
          >
            搜索
          </Button>
        </Space>
        <Table
          rowKey="id"
          size="middle"
          loading={listQuery.isLoading}
          dataSource={dictItems}
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

      {/* Dict modal */}
      <Modal
        title={editingId ? '编辑字典' : '新增字典'}
        open={modalOpen}
        width={480}
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
            label="字典编码"
            rules={[{ required: true, message: '请输入字典编码' }]}
          >
            <Input placeholder="如 gender, education" maxLength={64} disabled={!!editingId} />
          </Form.Item>
          <Form.Item
            name="name"
            label="字典名称"
            rules={[{ required: true, message: '请输入字典名称' }]}
          >
            <Input placeholder="如 性别, 学历" maxLength={64} />
          </Form.Item>
          <Form.Item
            name="value"
            label="字典值"
          >
            <Input placeholder="编码对应的值" maxLength={1000} allowClear />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { label: '启用', value: 'enabled' },
                { label: '停用', value: 'disabled' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Dict items drawer */}
      <Drawer
        title={drawerDict ? `字典项 - ${drawerDict.name}` : '字典项'}
        open={!!drawerDict}
        width={560}
        onClose={() => setDrawerDict(null)}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openItemCreate}>
            新增字典项
          </Button>
        }
      >
        <Table
          rowKey="id"
          size="small"
          loading={itemsQuery.isLoading}
          dataSource={detailItems}
          columns={itemColumns}
          pagination={false}
        />
      </Drawer>

      {/* Dict item modal */}
      <Modal
        title={editingItemId ? '编辑字典项' : '新增字典项'}
        open={itemModalOpen}
        width={440}
        onOk={handleItemSubmit}
        onCancel={() => {
          setItemModalOpen(false);
          setEditingItemId(null);
          itemForm.resetFields();
        }}
        confirmLoading={createItemMutation.isPending || updateItemMutation.isPending}
        destroyOnHidden
      >
        <Form form={itemForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="itemValue"
            label="值"
            rules={[{ required: true, message: '请输入字典项值' }]}
          >
            <Input.TextArea
              placeholder="存储值（如 male, female）"
              showCount
              rows={5}
              disabled={!!editingItemId}
            />
          </Form.Item>
          <Form.Item
            name="itemLabel"
            label="标签"
            rules={[{ required: true, message: '请输入显示标签' }, { max: 512, message: '最多 512 字' }]}
          >
            <Input.TextArea
              placeholder="显示文本（如 男, 女；申报条件请填全文）"
              maxLength={512}
              showCount
              rows={4}
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} max={9999} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { label: '启用', value: 'enabled' },
                { label: '停用', value: 'disabled' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

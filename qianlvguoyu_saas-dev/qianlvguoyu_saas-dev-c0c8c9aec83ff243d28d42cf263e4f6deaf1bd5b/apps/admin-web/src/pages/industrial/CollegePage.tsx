import {
  BankOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  Button,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import {
  createIndustrialCollege,
  deleteIndustrialCollege,
  getIndustrialCollege,
  getIndustrialCollegeStats,
  pageIndustrialColleges,
  updateIndustrialCollege,
} from '../../api/industrial';
import { PermissionGate } from '../../directives/permission';
import type { IndustrialCollege, IndustrialCollegePayload } from '../../types/industrial';

const statusOptions = [
  { label: '启用', value: 'enabled' },
  { label: '停用', value: 'disabled' },
];

export function CollegePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: '', tag: undefined, status: undefined });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [detailId, setDetailId] = useState<string | null>(null);
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['industrial-colleges', query],
    queryFn: () => pageIndustrialColleges(query),
  });

  const statsQuery = useQuery({
    queryKey: ['industrial-college-stats'],
    queryFn: getIndustrialCollegeStats,
  });

  const detailQuery = useQuery({
    queryKey: ['industrial-college-detail', detailId],
    queryFn: () => getIndustrialCollege(detailId!),
    enabled: detailId !== null,
  });

  const tagOptions = useMemo(
    () => (statsQuery.data?.byTag || []).map((item) => ({ label: item.tag, value: item.tag })),
    [statsQuery.data],
  );

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['industrial-colleges'] });
    queryClient.invalidateQueries({ queryKey: ['industrial-college-stats'] });
  }, [queryClient]);

  const createMutation = useMutation({
    mutationFn: createIndustrialCollege,
    onSuccess: () => {
      message.success('园区学院已新增');
      setModalOpen(false);
      form.resetFields();
      refresh();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: IndustrialCollegePayload }) =>
      updateIndustrialCollege(id, payload),
    onSuccess: () => {
      message.success('园区学院已更新');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      refresh();
      if (detailId) {
        queryClient.invalidateQueries({ queryKey: ['industrial-college-detail', detailId] });
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteIndustrialCollege,
    onSuccess: () => {
      message.success('园区学院已删除');
      setDetailId(null);
      refresh();
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 'enabled', sortOrder: 100 });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (record: IndustrialCollege) => {
      setEditingId(record.id);
      form.setFieldsValue({
        name: record.name,
        tag: record.tag,
        businessScope: record.businessScope,
        description: record.description,
        trainingQualification: record.trainingQualification,
        certificationQualification: record.certificationQualification,
        address: record.address,
        phone: record.phone,
        status: record.status,
        sortOrder: record.sortOrder,
      });
      setModalOpen(true);
    },
    [form],
  );

  const handleSubmit = useCallback(() => {
    form.validateFields().then((values) => {
      const payload: IndustrialCollegePayload = {
        name: values.name,
        tag: values.tag || undefined,
        businessScope: values.businessScope || undefined,
        description: values.description || undefined,
        trainingQualification: values.trainingQualification || undefined,
        certificationQualification: values.certificationQualification || undefined,
        address: values.address || undefined,
        phone: values.phone || undefined,
        status: values.status,
        sortOrder: values.sortOrder ?? 100,
      };
      if (editingId) {
        updateMutation.mutate({ id: editingId, payload });
      } else {
        createMutation.mutate(payload);
      }
    });
  }, [createMutation, editingId, form, updateMutation]);

  const handleDelete = useCallback(
    (record: IndustrialCollege) => {
      Modal.confirm({
        title: `确认删除“${record.name}”？`,
        content: '删除后该园区学院将不再出现在维护列表中。',
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
  const stats = statsQuery.data;

  const columns = [
    {
      title: '机构名称',
      dataIndex: 'name',
      width: 220,
      render: (text: string, record: IndustrialCollege) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => setDetailId(record.id)}>
          {text}
        </Button>
      ),
    },
    { title: '类型', dataIndex: 'tag', width: 120, render: (value: string) => value || '-' },
    { title: '经营范围', dataIndex: 'businessScope', ellipsis: true },
    { title: '地址', dataIndex: 'address', ellipsis: true },
    { title: '联系电话', dataIndex: 'phone', width: 140, render: (value: string) => value || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value: string) => value === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    { title: '排序', dataIndex: 'sortOrder', width: 80 },
    {
      title: '操作',
      width: 190,
      render: (_: unknown, record: IndustrialCollege) => (
        <Space size={4}>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setDetailId(record.id)}>
            详情
          </Button>
          <PermissionGate code="industrial:college:update">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          </PermissionGate>
          <PermissionGate code="industrial:college:delete">
            <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
              删除
            </Button>
          </PermissionGate>
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
            <Typography.Title level={1}>园区学院</Typography.Title>
          </div>
          <Typography.Text>维护园区学院机构、资质、经营范围和联系方式。</Typography.Text>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
          <PermissionGate code="industrial:college:create">
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增</Button>
          </PermissionGate>
        </Space>
      </section>

      <div style={{ padding: '0 24px 24px' }}>
        <Space wrap size={16} style={{ marginBottom: 16 }}>
          <Statistic title="总机构数" value={stats?.total ?? 0} />
          <Statistic title="近30天新增" value={stats?.recentCreated ?? 0} />
          <Statistic title="类型数" value={stats?.byTag?.length ?? 0} />
        </Space>

        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索机构、范围、地址、电话"
            allowClear
            style={{ width: 280 }}
            value={query.keyword}
            onChange={(e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 }))}
            onPressEnter={refresh}
          />
          <Select
            allowClear
            placeholder="类型"
            style={{ width: 160 }}
            value={query.tag}
            options={tagOptions}
            onChange={(tag) => setQuery((prev) => ({ ...prev, tag, page: 1 }))}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={query.status}
            options={statusOptions}
            onChange={(status) => setQuery((prev) => ({ ...prev, status, page: 1 }))}
          />
          <Button icon={<SearchOutlined />} onClick={refresh}>搜索</Button>
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
            showTotal: (count) => `共 ${count} 条`,
            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>

      <Modal
        title={editingId ? '编辑园区学院' : '新增园区学院'}
        open={modalOpen}
        width={760}
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
          <Form.Item name="name" label="机构名称" rules={[{ required: true, message: '请输入机构名称' }]}>
            <Input maxLength={200} placeholder="请输入机构名称" />
          </Form.Item>
          <Space align="start" style={{ width: '100%' }} size={16}>
            <Form.Item name="tag" label="类型" style={{ flex: 1 }}>
              <Input maxLength={80} placeholder="如：本科院校、职业院校" />
            </Form.Item>
            <Form.Item name="phone" label="联系电话" style={{ flex: 1 }}>
              <Input maxLength={50} placeholder="请输入联系电话" />
            </Form.Item>
          </Space>
          <Form.Item name="address" label="地址">
            <Input maxLength={300} placeholder="请输入地址" />
          </Form.Item>
          <Form.Item name="businessScope" label="经营范围">
            <Input.TextArea rows={3} maxLength={1000} showCount />
          </Form.Item>
          <Form.Item name="description" label="简介">
            <Input.TextArea rows={3} maxLength={2000} showCount />
          </Form.Item>
          <Form.Item name="trainingQualification" label="培训资质">
            <Input.TextArea rows={2} maxLength={1000} showCount />
          </Form.Item>
          <Form.Item name="certificationQualification" label="认定资质">
            <Input.TextArea rows={2} maxLength={1000} showCount />
          </Form.Item>
          <Space align="start" style={{ width: '100%' }} size={16}>
            <Form.Item name="status" label="状态" style={{ flex: 1 }}>
              <Select options={statusOptions} />
            </Form.Item>
            <Form.Item name="sortOrder" label="排序" style={{ flex: 1 }}>
              <InputNumber min={1} max={9999} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Drawer
        title="园区学院详情"
        width={640}
        open={detailId !== null}
        onClose={() => setDetailId(null)}
        extra={
          detailQuery.data ? (
            <Space>
              <PermissionGate code="industrial:college:update">
                <Button icon={<EditOutlined />} onClick={() => openEdit(detailQuery.data!)}>编辑</Button>
              </PermissionGate>
              <PermissionGate code="industrial:college:delete">
                <Button danger icon={<DeleteOutlined />} onClick={() => handleDelete(detailQuery.data!)}>删除</Button>
              </PermissionGate>
            </Space>
          ) : null
        }
      >
        <Descriptions column={1} bordered size="middle">
          <Descriptions.Item label="机构名称">{detailQuery.data?.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="类型">{detailQuery.data?.tag || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {detailQuery.data?.status === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="经营范围">{detailQuery.data?.businessScope || '-'}</Descriptions.Item>
          <Descriptions.Item label="简介">{detailQuery.data?.description || '-'}</Descriptions.Item>
          <Descriptions.Item label="培训资质">{detailQuery.data?.trainingQualification || '-'}</Descriptions.Item>
          <Descriptions.Item label="认定资质">{detailQuery.data?.certificationQualification || '-'}</Descriptions.Item>
          <Descriptions.Item label="地址">{detailQuery.data?.address || '-'}</Descriptions.Item>
          <Descriptions.Item label="联系电话">{detailQuery.data?.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="排序">{detailQuery.data?.sortOrder ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Drawer>
    </div>
  );
}

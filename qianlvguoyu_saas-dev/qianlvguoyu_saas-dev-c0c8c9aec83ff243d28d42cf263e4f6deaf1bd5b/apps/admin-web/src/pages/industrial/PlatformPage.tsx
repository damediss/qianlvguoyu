import {
  AuditOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { Button, DatePicker, Form, Input, InputNumber, Modal, Space, Table, Tag, Typography, message } from 'antd';
import type { TableColumnsType } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useCallback, useMemo, useState } from 'react';
import {
  createIndustrialPlatformItem,
  deleteIndustrialPlatformItem,
  pageIndustrialPlatformItems,
  updateIndustrialPlatformItem,
} from '../../api/industrial';
import { PermissionGate } from '../../directives/permission';
import type {
  IndustrialPlatformCategory,
  IndustrialPlatformItem,
  IndustrialPlatformPayload,
} from '../../types/industrial';

const configs = {
  master: {
    title: '技能大师工作室',
    desc: '维护技能大师工作室列表与团队人数数据。',
    nameLabel: '工作室名称',
    numsLabel: '团队人数',
    permission: 'industrial:platform-master',
    date: false,
    nums: true,
    state: false,
  },
  study: {
    title: '学习型班组',
    desc: '维护培训班组列表与评价基础数据。',
    nameLabel: '班组名称',
    numsLabel: '人数',
    permission: 'industrial:platform-study',
    date: false,
    nums: true,
    state: false,
  },
  match: {
    title: '竞赛',
    desc: '维护竞赛列表与成绩管理基础数据。',
    nameLabel: '竞赛名称',
    permission: 'industrial:platform-match',
    date: true,
    nums: false,
    state: false,
  },
  apprentice: {
    title: '新型学徒',
    desc: '维护新型学徒开班与评价数据列表。',
    nameLabel: '名称',
    numsLabel: '人数',
    permission: 'industrial:platform-apprentice',
    date: true,
    nums: true,
    state: false,
  },
  comment: {
    title: '自主评价',
    desc: '维护开班申报与自主评价数据。',
    nameLabel: '评价项目',
    permission: 'industrial:platform-comment',
    date: true,
    nums: false,
    state: true,
  },
} satisfies Record<IndustrialPlatformCategory, {
  title: string;
  desc: string;
  nameLabel: string;
  numsLabel?: string;
  permission: string;
  date: boolean;
  nums: boolean;
  state: boolean;
}>;

interface PlatformPageProps {
  category: IndustrialPlatformCategory;
}

export function PlatformPage({ category }: PlatformPageProps) {
  const config = configs[category];
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: '', company: '', state: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['industrial-platform', category, query],
    queryFn: () => pageIndustrialPlatformItems(category, query),
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['industrial-platform', category] });
  }, [category, queryClient]);

  const createMutation = useMutation({
    mutationFn: (payload: IndustrialPlatformPayload) => createIndustrialPlatformItem(category, payload),
    onSuccess: () => {
      message.success(`${config.title}已新增`);
      setModalOpen(false);
      form.resetFields();
      refresh();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: IndustrialPlatformPayload }) =>
      updateIndustrialPlatformItem(category, id, payload),
    onSuccess: () => {
      message.success(`${config.title}已更新`);
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      refresh();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteIndustrialPlatformItem(category, id),
    onSuccess: () => {
      message.success(`${config.title}已删除`);
      refresh();
    },
  });

  const openCreate = useCallback(() => {
    setEditingId(null);
    form.resetFields();
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback((record: IndustrialPlatformItem) => {
    setEditingId(record.id);
    form.setFieldsValue({
      company: record.company,
      name: record.name,
      nums: record.nums,
      date: record.date ? dayjs(record.date) : undefined,
      state: record.state,
    });
    setModalOpen(true);
  }, [form]);

  const submit = useCallback(() => {
    form.validateFields().then((values) => {
      const payload: IndustrialPlatformPayload = {
        company: values.company,
        name: values.name,
        nums: values.nums,
        date: values.date ? values.date.format('YYYY-MM-DD') : undefined,
        state: values.state || undefined,
      };
      if (editingId) {
        updateMutation.mutate({ id: editingId, payload });
      } else {
        createMutation.mutate(payload);
      }
    });
  }, [createMutation, editingId, form, updateMutation]);

  const handleDelete = useCallback((record: IndustrialPlatformItem) => {
    Modal.confirm({
      title: `确认删除“${record.name}”？`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => deleteMutation.mutate(record.id),
    });
  }, [deleteMutation]);

  const columns = useMemo(() => {
    const result: TableColumnsType<IndustrialPlatformItem> = [];
    if (config.date) {
      result.push({ title: category === 'match' ? '日期' : '创建日期', dataIndex: 'date', width: 140 });
    }
    result.push(
      { title: config.nameLabel, dataIndex: 'name', ellipsis: true },
      { title: category === 'comment' ? '企业' : '企业名称', dataIndex: 'company', width: 220, ellipsis: true },
    );
    if (config.nums) {
      result.push({ title: config.numsLabel, dataIndex: 'nums', width: 110 });
    }
    if (config.state) {
      result.push({
        title: '状态',
        dataIndex: 'state',
        width: 120,
        render: (value: string) => value ? <Tag color="processing">{value}</Tag> : '-',
      });
    }
    result.push({
      title: '操作',
      width: 150,
      render: (_: unknown, record: IndustrialPlatformItem) => (
        <Space size={4}>
          <PermissionGate code={`${config.permission}:update`}>
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          </PermissionGate>
          <PermissionGate code={`${config.permission}:delete`}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
              删除
            </Button>
          </PermissionGate>
        </Space>
      ),
    });
    return result;
  }, [category, config, handleDelete, openEdit]);

  const items = listQuery.data?.items || [];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon">{category === 'comment' ? <AuditOutlined /> : <TeamOutlined />}</span>
            <Typography.Title level={1}>{config.title}</Typography.Title>
          </div>
          <Typography.Text>{config.desc}</Typography.Text>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
          <PermissionGate code={`${config.permission}:create`}>
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增</Button>
          </PermissionGate>
        </Space>
      </section>

      <div style={{ padding: '0 24px 24px' }}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder={`搜索${config.nameLabel}、企业`}
            allowClear
            style={{ width: 280 }}
            value={query.keyword}
            onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value, page: 1 }))}
            onPressEnter={refresh}
          />
          {config.state ? (
            <Input
              placeholder="状态"
              allowClear
              style={{ width: 160 }}
              value={query.state}
              onChange={(event) => setQuery((prev) => ({ ...prev, state: event.target.value, page: 1 }))}
            />
          ) : null}
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
            total: listQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (count) => `共 ${count} 条`,
            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>

      <Modal
        title={editingId ? `编辑${config.title}` : `新增${config.title}`}
        open={modalOpen}
        onOk={submit}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
          form.resetFields();
        }}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          {config.date ? (
            <Form.Item name="date" label={category === 'match' ? '日期' : '创建日期'} rules={[{ required: true, message: '请选择日期' }]}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          ) : null}
          <Form.Item name="company" label={category === 'comment' ? '企业' : '企业名称'} rules={[{ required: true, message: '请输入企业名称' }]}>
            <Input maxLength={200} />
          </Form.Item>
          <Form.Item name="name" label={config.nameLabel} rules={[{ required: true, message: `请输入${config.nameLabel}` }]}>
            <Input maxLength={200} />
          </Form.Item>
          {config.nums ? (
            <Form.Item name="nums" label={config.numsLabel} rules={[{ required: true, message: `请输入${config.numsLabel}` }]}>
              <InputNumber min={0} max={999999} style={{ width: '100%' }} />
            </Form.Item>
          ) : null}
          {config.state ? (
            <Form.Item name="state" label="状态" rules={[{ required: true, message: '请输入状态' }]}>
              <Input maxLength={80} />
            </Form.Item>
          ) : null}
        </Form>
      </Modal>
    </div>
  );
}

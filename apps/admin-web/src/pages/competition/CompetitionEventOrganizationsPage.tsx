import {
  ArrowLeftOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Result,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  addCompetitionEventOrganization,
  getCompetitionEvent,
  pageCompetitionEventOrganizations,
  updateCompetitionEventOrganizationStatus,
} from '../../api/competition';
import type {
  CompetitionEventOrg,
  CompetitionEventOrgCreateRequest,
  CompetitionEventOrgPageQuery,
  CompetitionEventOrgStatus,
  CompetitionEventOrgStatusUpdateRequest,
  CompetitionId,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';

interface OrganizationFormValues {
  orgId: string;
  quota?: number;
  importPermission: number;
}

interface StatusFormValues {
  status: Exclude<CompetitionEventOrgStatus, 'invited'>;
}

const statusOptions: Array<{ label: string; value: CompetitionEventOrgStatus }> = [
  { label: '已邀请', value: 'invited' },
  { label: '生效中', value: 'active' },
  { label: '已暂停', value: 'suspended' },
  { label: '已关闭', value: 'closed' },
];

const editableStatusOptions: Array<{ label: string; value: StatusFormValues['status'] }> = [
  { label: '生效中', value: 'active' },
  { label: '已暂停', value: 'suspended' },
  { label: '已关闭', value: 'closed' },
];

const statusMeta: Record<CompetitionEventOrgStatus, { label: string; color: string }> = {
  invited: { label: '已邀请', color: 'default' },
  active: { label: '生效中', color: 'success' },
  suspended: { label: '已暂停', color: 'warning' },
  closed: { label: '已关闭', color: 'error' },
};

function idValue(id: CompetitionId) {
  return String(id);
}

function statusTag(status?: CompetitionEventOrgStatus) {
  const meta = status ? statusMeta[status] : undefined;
  return <Tag color={meta?.color}>{meta?.label || status || '-'}</Tag>;
}

export function CompetitionEventOrganizationsPage({
  eventId,
  onNavigate,
}: {
  eventId: string;
  onNavigate: (path: string) => void;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:event:view')
    && permissions.includes('competition:event-org:view');
  const canManage = permissions.includes('competition:event-org:manage');
  const [query, setQuery] = useState<CompetitionEventOrgPageQuery>({ page: 1, pageSize: 10 });
  const [addOpen, setAddOpen] = useState(false);
  const [editingOrganization, setEditingOrganization] = useState<CompetitionEventOrg>();
  const [addForm] = Form.useForm<OrganizationFormValues>();
  const [statusForm] = Form.useForm<StatusFormValues>();
  const queryClient = useQueryClient();

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canView,
  });

  const organizationsQuery = useQuery({
    queryKey: ['competition-event-organizations', eventId, query],
    queryFn: () => pageCompetitionEventOrganizations(eventId, query),
    enabled: canView,
  });

  const addMutation = useMutation({
    mutationFn: (payload: CompetitionEventOrgCreateRequest) =>
      addCompetitionEventOrganization(eventId, payload),
    onSuccess: () => {
      message.success('参与企业添加成功');
      setAddOpen(false);
      addForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-event-organizations', eventId] });
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ orgId, payload }: { orgId: CompetitionId; payload: CompetitionEventOrgStatusUpdateRequest }) =>
      updateCompetitionEventOrganizationStatus(eventId, orgId, payload),
    onSuccess: () => {
      message.success('参与状态已更新');
      setEditingOrganization(undefined);
      statusForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-event-organizations', eventId] });
    },
  });

  useEffect(() => {
    if (!editingOrganization) return;
    statusForm.setFieldsValue({
      status: editingOrganization.status === 'invited' ? 'active' : editingOrganization.status,
    });
  }, [editingOrganization, statusForm]);

  const columns = useMemo(() => [
    {
      title: '企业名称',
      dataIndex: 'orgName',
      render: (value: string | null | undefined, record: CompetitionEventOrg) => value || idValue(record.orgId),
    },
    {
      title: '企业编码',
      key: 'orgCode',
      width: 180,
      render: (_: unknown, record: CompetitionEventOrg) => record.orgCode || idValue(record.orgId),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value: CompetitionEventOrgStatus) => statusTag(value),
    },
    {
      title: '参赛名额',
      dataIndex: 'quota',
      width: 100,
      render: (value: number | null | undefined) => value ?? '-',
    },
    {
      title: '名单导入权限',
      dataIndex: 'importPermission',
      width: 150,
      render: (value: number | null | undefined) => (value === 1 ? '允许' : '不允许'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 120,
      render: (_: unknown, record: CompetitionEventOrg) => canManage ? (
        <Button
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={() => setEditingOrganization(record)}
        >
          修改状态
        </Button>
      ) : null,
    },
  ], [canManage]);

  if (!canView) {
    return <Result status="403" title="暂无赛事查看权限" subTitle="请联系管理员申请 competition:event:view 权限。" />;
  }
  if (eventQuery.isLoading || organizationsQuery.isLoading) {
    return <Spin className="competition-loading" />;
  }
  if (eventQuery.isError || !eventQuery.data) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回赛事列表</Button>} />;
  }
  if (organizationsQuery.isError) {
    return <Result status="error" title="参与企业加载失败" extra={<Button onClick={() => organizationsQuery.refetch()}>重新加载</Button>} />;
  }

  const event = eventQuery.data;
  const canAdd = canManage && event.scope === 'platform';
  const organizations = organizationsQuery.data?.items || [];

  const handleAdd = (values: OrganizationFormValues) => {
    const orgId = values.orgId.trim();
    if (!orgId) {
      message.error('请输入企业 ID');
      return;
    }
    addMutation.mutate({
      orgId,
      quota: values.quota,
      importPermission: values.importPermission,
    });
  };

  const handleStatusUpdate = (values: StatusFormValues) => {
    if (!editingOrganization) return;
    statusMutation.mutate({
      orgId: editingOrganization.orgId,
      payload: values,
    });
  };

  return (
    <CompetitionEventFrame event={event} activeSection="organizations" onNavigate={onNavigate}>
      <Card bordered={false} className="competition-filter-card">
        <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
          <Space wrap>
            <Select
              allowClear
              placeholder="按状态筛选"
              prefix={<SearchOutlined />}
              options={statusOptions}
              value={query.status}
              onChange={(status: CompetitionEventOrgStatus | undefined) =>
                setQuery((current) => ({ ...current, page: 1, status }))}
              style={{ width: 190 }}
            />
            <Button type="primary" ghost icon={<SearchOutlined />} onClick={() => organizationsQuery.refetch()}>
              查询
            </Button>
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => organizationsQuery.refetch()}>刷新</Button>
            {canAdd && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>
                添加参与企业
              </Button>
            )}
          </Space>
        </Space>
      </Card>

      <Card bordered={false} className="competition-table-card">
        <Table<CompetitionEventOrg>
          rowKey={(record) => idValue(record.id)}
          loading={organizationsQuery.isFetching}
          dataSource={organizations}
          columns={columns}
          scroll={{ x: 900 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: organizationsQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 家企业`,
            onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })),
          }}
        />
      </Card>

      <Modal
        title="添加参与企业"
        open={addOpen}
        destroyOnClose
        confirmLoading={addMutation.isPending}
        onCancel={() => { setAddOpen(false); addForm.resetFields(); }}
        onOk={() => addForm.submit()}
      >
        <Form<OrganizationFormValues>
          form={addForm}
          layout="vertical"
          initialValues={{ importPermission: 0 }}
          onFinish={handleAdd}
          requiredMark="optional"
        >
          <Form.Item name="orgId" label="企业 ID" rules={[{ required: true, whitespace: true, message: '请输入企业 ID' }]}>
            <Input placeholder="请输入企业 ID" />
          </Form.Item>
          <Form.Item name="quota" label="参赛名额" rules={[{ type: 'number', min: 0, message: '参赛名额不能小于 0' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="不限制可留空" />
          </Form.Item>
          <Form.Item name="importPermission" label="名单导入权限" rules={[{ required: true, message: '请选择名单导入权限' }]}>
            <Select options={[{ label: '不允许', value: 0 }, { label: '允许', value: 1 }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`修改「${editingOrganization?.orgName || idValue(editingOrganization?.orgId || '')}」参与状态`}
        open={Boolean(editingOrganization)}
        destroyOnClose
        confirmLoading={statusMutation.isPending}
        onCancel={() => { setEditingOrganization(undefined); statusForm.resetFields(); }}
        onOk={() => statusForm.submit()}
      >
        <Form<StatusFormValues> form={statusForm} layout="vertical" onFinish={handleStatusUpdate}>
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择参与状态' }]}>
            <Select options={editableStatusOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </CompetitionEventFrame>
  );
}

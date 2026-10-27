import {
  ArrowLeftOutlined,
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
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import {
  addCompetitionEventOrganization,
  getCompetitionEvent,
  pageCompetitionEventOrganizations,
  updateCompetitionEventOrganizationImportPermission,
  updateCompetitionEventOrganizationStatus,
} from '../../api/competition';
import type {
  CompetitionEventOrg,
  CompetitionEventOrgCreateRequest,
  CompetitionEventOrgImportPermissionUpdateRequest,
  CompetitionEventOrgPageQuery,
  CompetitionEventOrgStatus,
  CompetitionEventOrgStatusUpdateRequest,
  CompetitionId,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';

interface OrganizationFormValues {
  orgName: string;
  quota?: number;
  importPermission: number;
}

type EditableCompetitionEventOrgStatus = Extract<CompetitionEventOrgStatus, 'active' | 'suspended'>;

const statusOptions: Array<{ label: string; value: EditableCompetitionEventOrgStatus }> = [
  { label: '已生效', value: 'active' },
  { label: '已暂停', value: 'suspended' },
];

const importPermissionOptions: Array<{ label: string; value: 0 | 1 }> = [
  { label: '拒绝', value: 0 },
  { label: '允许', value: 1 },
];

const statusMeta: Record<CompetitionEventOrgStatus, { label: string; color: string }> = {
  invited: { label: '已邀请', color: 'default' },
  active: { label: '已生效', color: 'success' },
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

function editableStatusValue(status: CompetitionEventOrgStatus): EditableCompetitionEventOrgStatus {
  return status === 'suspended' ? 'suspended' : 'active';
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
  const [orgCodeDetail, setOrgCodeDetail] = useState<{ orgName: string; orgCode: string } | null>(null);
  const [updatingStatusOrgId, setUpdatingStatusOrgId] = useState<CompetitionId>();
  const [updatingPermissionOrgId, setUpdatingPermissionOrgId] = useState<CompetitionId>();
  const [addForm] = Form.useForm<OrganizationFormValues>();
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
    onMutate: ({ orgId }) => setUpdatingStatusOrgId(orgId),
    onSuccess: () => {
      message.success('参与状态已更新');
      queryClient.invalidateQueries({ queryKey: ['competition-event-organizations', eventId] });
    },
    onSettled: () => setUpdatingStatusOrgId(undefined),
  });

  const importPermissionMutation = useMutation({
    mutationFn: ({ orgId, payload }: {
      orgId: CompetitionId;
      payload: CompetitionEventOrgImportPermissionUpdateRequest;
    }) => updateCompetitionEventOrganizationImportPermission(eventId, orgId, payload),
    onMutate: ({ orgId }) => setUpdatingPermissionOrgId(orgId),
    onSuccess: () => {
      message.success('名单导入权限已更新');
      queryClient.invalidateQueries({ queryKey: ['competition-event-organizations', eventId] });
    },
    onSettled: () => setUpdatingPermissionOrgId(undefined),
  });

  const columns = useMemo(() => [
    {
      title: '企业名称',
      dataIndex: 'orgName',
      align: 'left',
      render: (value: string | null | undefined, record: CompetitionEventOrg) => value || idValue(record.orgId),
    },
    {
      title: '企业编码',
      key: 'orgCode',
      align: 'left',
      width: 72,
      render: (_: unknown, record: CompetitionEventOrg) => (
        <Button
          type="link"
          size="small"
          aria-label={`查看${record.orgName || idValue(record.orgId)}的企业编码详情`}
          onClick={() => setOrgCodeDetail({
            orgName: record.orgName || idValue(record.orgId),
            orgCode: record.orgCode || idValue(record.orgId),
          })}
        >
          详细
        </Button>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      align: 'left',
      width: 140,
      render: (value: CompetitionEventOrgStatus, record: CompetitionEventOrg) => canManage ? (
        <Select<EditableCompetitionEventOrgStatus>
          size="small"
          value={editableStatusValue(value)}
          options={statusOptions}
          loading={updatingStatusOrgId === record.orgId}
          onChange={(status) => statusMutation.mutate({
            orgId: record.orgId,
            payload: { status },
          })}
          style={{ width: 120 }}
        />
      ) : statusTag(value),
    },
    {
      title: '参赛名额',
      dataIndex: 'quota',
      align: 'left',
      width: 100,
      render: (value: number | null | undefined) => value ?? '-',
    },
    {
      title: '名单导入权限',
      dataIndex: 'importPermission',
      align: 'left',
      width: 150,
      render: (value: number | null | undefined, record: CompetitionEventOrg) => canManage ? (
        <Select<0 | 1>
          size="small"
          value={value === 1 ? 1 : 0}
          options={importPermissionOptions}
          loading={updatingPermissionOrgId === record.orgId}
          onChange={(importPermission) => importPermissionMutation.mutate({
            orgId: record.orgId,
            payload: { importPermission },
          })}
          style={{ width: 100 }}
        />
      ) : (value === 1 ? '允许' : '拒绝'),
    },
  ], [canManage, importPermissionMutation, statusMutation, updatingPermissionOrgId, updatingStatusOrgId]);

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
    if (!values.orgName?.trim()) {
      message.error('请输入企业名称');
      return;
    }
    addMutation.mutate({
      orgName: values.orgName.trim(),
      quota: values.quota,
      importPermission: values.importPermission ?? 1,
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
              value={query.status === 'active' || query.status === 'suspended' ? query.status : undefined}
              onChange={(status: EditableCompetitionEventOrgStatus | undefined) =>
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
          tableLayout="fixed"
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
        title="企业编码详情"
        open={Boolean(orgCodeDetail)}
        centered
        footer={null}
        destroyOnClose
        onCancel={() => setOrgCodeDetail(null)}
      >
        {orgCodeDetail && (
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <Typography.Text type="secondary">企业名称</Typography.Text>
            <Typography.Text>{orgCodeDetail.orgName}</Typography.Text>
            <Typography.Text type="secondary">企业编码</Typography.Text>
            <Typography.Paragraph
              code
              copyable={{ text: orgCodeDetail.orgCode }}
              style={{ marginBottom: 0, overflowWrap: 'anywhere' }}
            >
              {orgCodeDetail.orgCode}
            </Typography.Paragraph>
          </Space>
        )}
      </Modal>

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
          initialValues={{ importPermission: 1 }}
          onFinish={handleAdd}
          requiredMark="optional"
        >
          <Form.Item name="orgName" label="企业名称" rules={[{ required: true, whitespace: true, max: 128, message: '请输入企业名称' }]}>
            <Input showCount maxLength={128} placeholder="请输入参与企业名称" />
          </Form.Item>
          <Form.Item name="quota" label="参赛名额" rules={[{ type: 'number', min: 0, message: '参赛名额不能小于 0' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="不限制可留空" />
          </Form.Item>
          <Form.Item name="importPermission" label="名单导入权限" rules={[{ required: true, message: '请选择名单导入权限' }]}>
            <Select options={importPermissionOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </CompetitionEventFrame>
  );
}

import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
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
import { useEffect, useMemo, useState } from 'react';
import {
  createCompetitionPlayer,
  deleteCompetitionPlayer,
  listCompetitionEventWorkTypes,
  pageCompetitionEventOrganizations,
  pageCompetitionEvents,
  pageCompetitionPlayers,
  updateCompetitionPlayer,
} from '../../api/competition';
import type {
  CompetitionEvent,
  CompetitionId,
  CompetitionPlayer,
  CompetitionPlayerCreateRequest,
  CompetitionPlayerUpdateRequest,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';

interface CompetitionPlayerFormValues {
  orgId: string;
  eventWorkTypeId: string;
  employeeNo: string;
  name: string;
  idCardNo?: string;
  mobile?: string;
  level?: string;
}

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
}

function statusTag(status?: string | null) {
  const meta: Record<string, { label: string; color: string }> = {
    pending_audit: { label: '待审核', color: 'warning' },
    eligible_unactivated: { label: '待激活', color: 'processing' },
    eligible_activated: { label: '已激活', color: 'success' },
    rejected: { label: '已驳回', color: 'error' },
    withdrawn: { label: '已撤回', color: 'default' },
  };
  const value = status ? meta[status] : undefined;
  return <Tag color={value?.color}>{value?.label || status || '-'}</Tag>;
}

export function CompetitionPlayerManagementPage({
  onNavigate,
}: {
  onNavigate: (path: string) => void;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player:view');
  const canCreate = permissions.includes('competition:player:create');
  const canUpdate = permissions.includes('competition:player:update');
  const canDelete = permissions.includes('competition:player:delete');
  const canViewOrganizations = permissions.includes('competition:event-org:view');
  const [selectedEventId, setSelectedEventId] = useState<string>();
  const [selectedOrgId, setSelectedOrgId] = useState<string>();
  const [selectedWorkTypeId, setSelectedWorkTypeId] = useState<string>();
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: undefined as string | undefined });
  const [editing, setEditing] = useState<CompetitionPlayer>();
  const [formOpen, setFormOpen] = useState(false);
  const [form] = Form.useForm<CompetitionPlayerFormValues>();
  const queryClient = useQueryClient();

  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'players'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];

  useEffect(() => {
    if (!selectedEventId && events.length) setSelectedEventId(String(events[0].id));
  }, [events, selectedEventId]);

  const organizationsQuery = useQuery({
    queryKey: ['competition-event-organizations', selectedEventId, 'active'],
    queryFn: () => pageCompetitionEventOrganizations(selectedEventId!, { page: 1, pageSize: 500, status: 'active' }),
    enabled: canView && canViewOrganizations && Boolean(selectedEventId),
  });
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', selectedEventId],
    queryFn: () => listCompetitionEventWorkTypes(selectedEventId!),
    enabled: canView && Boolean(selectedEventId),
  });

  const orgOptions = useMemo(() => (
    (organizationsQuery.data?.items || []).map((item) => ({
      value: idValue(item.orgId),
      label: item.orgName || idValue(item.orgId),
    }))
  ), [organizationsQuery.data?.items]);
  const workTypeOptions = useMemo(() => (
    (workTypesQuery.data || [])
      .filter((item) => item.status === 'active')
      .map((item) => ({ value: idValue(item.id), label: item.displayName || item.workTypeName || idValue(item.workTypeId) }))
  ), [workTypesQuery.data]);

  useEffect(() => {
    if (selectedOrgId && !orgOptions.some((item) => item.value === selectedOrgId)) setSelectedOrgId(undefined);
  }, [orgOptions, selectedOrgId]);
  useEffect(() => {
    if (selectedWorkTypeId && !workTypeOptions.some((item) => item.value === selectedWorkTypeId)) setSelectedWorkTypeId(undefined);
  }, [selectedWorkTypeId, workTypeOptions]);

  const playersQuery = useQuery({
    queryKey: ['competition-players', selectedEventId, selectedOrgId, selectedWorkTypeId, query],
    queryFn: () => pageCompetitionPlayers({
      page: query.page,
      pageSize: query.pageSize,
      eventId: selectedEventId,
      orgId: selectedOrgId,
      eventWorkTypeId: selectedWorkTypeId,
      keyword: query.keyword,
    }),
    enabled: canView && Boolean(selectedEventId),
  });

  const saveMutation = useMutation({
    mutationFn: (values: CompetitionPlayerFormValues) => {
      if (!selectedEventId) throw new Error('请选择赛事任务');
      if (editing) {
        const payload: CompetitionPlayerUpdateRequest = {
          orgId: values.orgId,
          eventWorkTypeId: values.eventWorkTypeId,
          employeeNo: values.employeeNo,
          name: values.name,
          idCardNo: values.idCardNo || undefined,
          mobile: values.mobile || undefined,
          level: values.level || undefined,
        };
        return updateCompetitionPlayer(editing.registrationId, payload);
      }
      const payload: CompetitionPlayerCreateRequest = {
        eventId: selectedEventId,
        orgId: values.orgId,
        eventWorkTypeId: values.eventWorkTypeId,
        employeeNo: values.employeeNo,
        name: values.name,
        idCardNo: values.idCardNo || '',
        mobile: values.mobile || '',
        level: values.level || undefined,
      };
      return createCompetitionPlayer(payload);
    },
    onSuccess: () => {
      message.success(editing ? '选手信息已更新' : '选手已新增');
      setFormOpen(false);
      setEditing(undefined);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-players'] });
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '保存选手失败'),
  });

  const deleteMutation = useMutation({
    mutationFn: (registrationId: CompetitionId) => deleteCompetitionPlayer(registrationId),
    onSuccess: () => {
      message.success('选手已删除');
      queryClient.invalidateQueries({ queryKey: ['competition-players'] });
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '删除选手失败'),
  });

  const openCreate = () => {
    setEditing(undefined);
    form.resetFields();
    form.setFieldsValue({ orgId: selectedOrgId, eventWorkTypeId: selectedWorkTypeId });
    setFormOpen(true);
  };
  const openEdit = (record: CompetitionPlayer) => {
    setEditing(record);
    form.setFieldsValue({
      orgId: idValue(record.orgId),
      eventWorkTypeId: idValue(record.eventWorkTypeId),
      employeeNo: record.employeeNo || '',
      name: record.name || '',
      level: record.level || undefined,
      idCardNo: undefined,
      mobile: undefined,
    });
    setFormOpen(true);
  };

  const columns = [
    { title: '赛事任务', dataIndex: 'eventName', width: 180, ellipsis: true },
    { title: '企业', dataIndex: 'orgName', width: 170, ellipsis: true },
    { title: '工种', dataIndex: 'eventWorkTypeName', width: 140 },
    { title: '工号', dataIndex: 'employeeNo', width: 130 },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '身份证号', dataIndex: 'idCardNo', width: 150 },
    { title: '手机号', dataIndex: 'mobile', width: 130 },
    { title: '等级', dataIndex: 'level', width: 90, render: (value: string | null) => value || '-' },
    { title: '参赛状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '操作', key: 'actions', fixed: 'right' as const, width: 140,
      render: (_: unknown, record: CompetitionPlayer) => (
        <Space size={0}>
          {canUpdate && <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>编辑</Button>}
          {canDelete && <Button type="link" danger size="small" icon={<DeleteOutlined />} loading={deleteMutation.isPending && deleteMutation.variables === record.registrationId} onClick={() => Modal.confirm({ title: '确认删除该选手？', content: '删除后将从本赛事选手名单中移除，选手身份不会被删除。', okText: '删除', okButtonProps: { danger: true }, onOk: () => deleteMutation.mutate(record.registrationId) })}>删除</Button>}
        </Space>
      ),
    },
  ];

  if (!canView) return <Result status="403" title="暂无选手管理权限" />;
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventsQuery.isError) return <Result status="error" title="赛事任务加载失败" />;

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}><TeamOutlined /> 选手管理</Typography.Title>
          <Typography.Paragraph type="secondary">按赛事任务划分查看选手，支持按企业、赛事工种筛选，并维护选手基础信息。</Typography.Paragraph>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => { void eventsQuery.refetch(); void playersQuery.refetch(); }}>刷新</Button>
          {canCreate && <Button type="primary" icon={<PlusOutlined />} disabled={!selectedEventId} onClick={openCreate}>新增选手</Button>}
        </Space>
      </section>

      <Card bordered={false} className="competition-filter-card">
        <Space wrap>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedEventId}
            options={events.map((event: CompetitionEvent) => ({ value: idValue(event.id), label: `${event.code} · ${event.name}` }))}
            onChange={(value) => { setSelectedEventId(value); setQuery((current) => ({ ...current, page: 1 })); setSelectedOrgId(undefined); setSelectedWorkTypeId(undefined); }}
            placeholder="选择赛事任务"
            style={{ width: 280 }}
          />
          <Select allowClear showSearch optionFilterProp="label" value={selectedOrgId} options={orgOptions} onChange={(value) => { setSelectedOrgId(value); setQuery((current) => ({ ...current, page: 1 })); }} placeholder="按企业筛选" style={{ width: 220 }} loading={organizationsQuery.isLoading} />
          <Select allowClear showSearch optionFilterProp="label" value={selectedWorkTypeId} options={workTypeOptions} onChange={(value) => { setSelectedWorkTypeId(value); setQuery((current) => ({ ...current, page: 1 })); }} placeholder="按工种筛选" style={{ width: 200 }} loading={workTypesQuery.isLoading} />
          <Input allowClear prefix={<SearchOutlined />} placeholder="工号或姓名" value={keyword} onChange={(event) => setKeyword(event.target.value)} onPressEnter={() => setQuery((current) => ({ ...current, page: 1, keyword: keyword.trim() || undefined }))} style={{ width: 220 }} />
          <Button type="primary" ghost icon={<SearchOutlined />} onClick={() => setQuery((current) => ({ ...current, page: 1, keyword: keyword.trim() || undefined }))}>查询</Button>
        </Space>
      </Card>

      <Card bordered={false} className="competition-table-card">
        <Table<CompetitionPlayer>
          rowKey={(record) => idValue(record.registrationId)}
          loading={playersQuery.isFetching}
          dataSource={playersQuery.data?.items || []}
          columns={columns}
          scroll={{ x: 1500 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: playersQuery.data?.total || 0, showSizeChanger: true, onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })) }}
        />
      </Card>

      <Modal
        title={editing ? '编辑选手' : '新增选手'}
        open={formOpen}
        destroyOnClose
        width={620}
        confirmLoading={saveMutation.isPending}
        onCancel={() => { setFormOpen(false); setEditing(undefined); form.resetFields(); }}
        onOk={() => form.submit()}
      >
        <Form<CompetitionPlayerFormValues> form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
          <Form.Item label="赛事任务"><Input value={events.find((event) => idValue(event.id) === selectedEventId)?.name || selectedEventId} disabled /></Form.Item>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="orgId" label="企业" rules={[{ required: true, message: '请选择企业' }]} style={{ flex: 1 }}><Select options={orgOptions} showSearch optionFilterProp="label" placeholder="请选择企业" /></Form.Item>
            <Form.Item name="eventWorkTypeId" label="工种" rules={[{ required: true, message: '请选择赛事工种' }]} style={{ flex: 1 }}><Select options={workTypeOptions} showSearch optionFilterProp="label" placeholder="请选择工种" /></Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="employeeNo" label="工号" rules={[{ required: true, whitespace: true, message: '请输入工号' }]} style={{ flex: 1 }}><Input maxLength={64} /></Form.Item>
            <Form.Item name="name" label="姓名" rules={[{ required: true, whitespace: true, message: '请输入姓名' }]} style={{ flex: 1 }}><Input maxLength={128} /></Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="idCardNo" label="身份证号" rules={[{ required: !editing, message: '请输入身份证号' }]} extra={editing ? '不修改请留空' : undefined} style={{ flex: 1 }}><Input maxLength={18} /></Form.Item>
            <Form.Item name="mobile" label="手机号" rules={[{ required: !editing, message: '请输入手机号' }]} extra={editing ? '不修改请留空' : undefined} style={{ flex: 1 }}><Input maxLength={11} /></Form.Item>
          </Space>
          <Form.Item name="level" label="等级"><Input maxLength={32} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

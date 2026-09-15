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
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  createCompetitionPlayer,
  deleteCompetitionPlayer,
  getCompetitionPlayerForEdit,
  listCompetitionEventWorkTypeLevels,
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
  CompetitionPlayerWorkTypeRequest,
  CompetitionPlayerUpdateRequest,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';

interface CompetitionPlayerFormValues {
  orgId: string;
  name: string;
  idCardNo?: string;
  mobile?: string;
}

interface WorkTypeSelection {
  eventWorkTypeId: string;
  eventWorkTypeLevelId?: string;
}

const idCardWeights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
const idCardCheckCodes = '10X98765432';

function normalizeIdCardNo(value?: string) {
  const normalized = value?.trim().toUpperCase().replace(/\s/g, '');
  return normalized || undefined;
}

function normalizeMobile(value?: string) {
  const normalized = value?.trim().replace(/\s/g, '');
  return normalized || undefined;
}

function isValidIdCardNo(value?: string) {
  const normalized = normalizeIdCardNo(value);
  if (!normalized || !/^\d{17}[0-9X]$/.test(normalized)) return false;
  const checksum = idCardWeights.reduce(
    (sum, weight, index) => sum + Number(normalized[index]) * weight,
    0,
  );
  return idCardCheckCodes[checksum % 11] === normalized[17];
}

function isValidMobile(value?: string) {
  const normalized = normalizeMobile(value);
  return Boolean(normalized && /^1[3-9]\d{9}$/.test(normalized));
}

function errorText(error: unknown, fallback: string) {
  const responseMessage = (error as {
    response?: { data?: { message?: unknown } };
  })?.response?.data?.message;
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage;
  return error instanceof Error && error.message.trim() ? error.message : fallback;
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

export function CompetitionPlayerManagementPage() {
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
  const [editLoadingId, setEditLoadingId] = useState<CompetitionId>();
  const [formOpen, setFormOpen] = useState(false);
  const [selectedWorkTypes, setSelectedWorkTypes] = useState<WorkTypeSelection[]>([]);
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

  const workTypeLevelQueries = useQueries({
    queries: workTypeOptions.map((workType) => ({
      queryKey: ['competition-event-work-type-levels', selectedEventId, workType.value],
      queryFn: () => listCompetitionEventWorkTypeLevels(selectedEventId!, workType.value),
      enabled: canView && Boolean(selectedEventId),
    })),
  });

  const levelsByWorkType = useMemo(() => {
    const result: Record<string, Array<{ value: string; label: string }>> = {};
    workTypeOptions.forEach((workType, index) => {
      const levels = (workTypeLevelQueries[index]?.data || []) as Array<{
        id: CompetitionId;
        levelCode: string;
        levelName: string;
      }>;
      result[workType.value] = levels.map((level) => ({
        value: idValue(level.id),
        label: level.levelName ? `${level.levelName}（${level.levelCode}）` : level.levelCode,
      }));
    });
    return result;
  }, [workTypeLevelQueries, workTypeOptions]);

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
      if (!selectedWorkTypes.length) throw new Error('请至少选择一个赛事工种');
      if (selectedWorkTypes.some((selection) => !selection.eventWorkTypeLevelId)) {
        throw new Error('请为每个赛事工种选择对应等级');
      }
      const firstWorkType = selectedWorkTypes[0];
      const firstLevel = levelsByWorkType[firstWorkType.eventWorkTypeId]?.find(
        (level) => level.value === firstWorkType.eventWorkTypeLevelId,
      );
      const workTypes: CompetitionPlayerWorkTypeRequest[] = selectedWorkTypes.map((selection) => ({
        eventWorkTypeId: selection.eventWorkTypeId,
        eventWorkTypeLevelId: selection.eventWorkTypeLevelId,
      }));
      if (editing) {
        const idCardNo = normalizeIdCardNo(values.idCardNo);
        const mobile = normalizeMobile(values.mobile);
          const payload: CompetitionPlayerUpdateRequest = {
          orgId: values.orgId,
          eventWorkTypeId: firstWorkType.eventWorkTypeId,
          name: values.name,
          idCardNo,
          mobile,
          level: firstLevel?.label,
          workTypes,
        };
        return updateCompetitionPlayer(editing.registrationId, payload);
      }
      const idCardNo = normalizeIdCardNo(values.idCardNo);
      const mobile = normalizeMobile(values.mobile);
      const payload: CompetitionPlayerCreateRequest = {
        eventId: selectedEventId,
        orgId: values.orgId,
        eventWorkTypeId: firstWorkType.eventWorkTypeId,
        name: values.name,
        idCardNo: idCardNo || '',
        mobile: mobile || '',
        level: firstLevel?.label,
        workTypes,
      };
      return createCompetitionPlayer(payload);
    },
    onSuccess: () => {
      message.success(editing ? '选手信息已更新' : '选手已新增');
      setFormOpen(false);
      setEditing(undefined);
      setSelectedWorkTypes([]);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-players'] });
    },
    onError: (error) => message.error(errorText(error, '保存选手失败')),
  });

  const deleteMutation = useMutation({
    mutationFn: (registrationId: CompetitionId) => deleteCompetitionPlayer(registrationId),
    onSuccess: () => {
      message.success('选手已删除');
      queryClient.invalidateQueries({ queryKey: ['competition-players'] });
    },
    onError: (error) => message.error(errorText(error, '删除选手失败')),
  });

  const openCreate = () => {
    setEditing(undefined);
    form.resetFields();
    setSelectedWorkTypes(selectedWorkTypeId
      ? [{ eventWorkTypeId: selectedWorkTypeId }]
      : []);
    form.setFieldsValue({ orgId: selectedOrgId });
    setFormOpen(true);
  };
  const openEdit = async (record: CompetitionPlayer) => {
    setEditLoadingId(record.registrationId);
    try {
      const detail = await getCompetitionPlayerForEdit(record.registrationId);
      setEditing(detail);
      const detailWorkTypes = detail.workTypes && detail.workTypes.length
        ? detail.workTypes.map((workType) => ({
          eventWorkTypeId: idValue(workType.eventWorkTypeId),
          eventWorkTypeLevelId: workType.eventWorkTypeLevelId == null
            ? undefined : idValue(workType.eventWorkTypeLevelId),
        }))
        : detail.eventWorkTypeId
          ? [{
            eventWorkTypeId: idValue(detail.eventWorkTypeId),
            eventWorkTypeLevelId: detail.eventWorkTypeLevelId == null
              ? undefined : idValue(detail.eventWorkTypeLevelId),
          }]
          : [];
      setSelectedWorkTypes(detailWorkTypes);
      form.setFieldsValue({
        orgId: idValue(detail.orgId),
        name: detail.name || '',
        idCardNo: detail.idCardNo && detail.idCardNo !== '-' ? detail.idCardNo : '',
        mobile: detail.mobile && detail.mobile !== '-' ? detail.mobile : '',
      });
      setFormOpen(true);
    } catch (error) {
      message.error(errorText(error, '选手详情加载失败'));
    } finally {
      setEditLoadingId(undefined);
    }
  };

  const columns = [
    { title: '赛事任务', dataIndex: 'eventName', width: 180, ellipsis: true },
    { title: '企业', dataIndex: 'orgName', width: 170, ellipsis: true },
    {
      title: '工种 / 等级', key: 'workTypes', width: 260,
      render: (_: unknown, record: CompetitionPlayer) => {
        const workTypes = record.workTypes?.length
          ? record.workTypes
          : record.eventWorkTypeId
            ? [{
              eventWorkTypeId: record.eventWorkTypeId,
              eventWorkTypeName: record.eventWorkTypeName,
              levelName: record.eventWorkTypeLevelName || record.level,
              level: record.level,
            }]
            : [];
        return <Space wrap size={[4, 4]}>{workTypes.map((workType) => (
          <Tag key={idValue(workType.eventWorkTypeId)}>
            {workType.eventWorkTypeName || idValue(workType.eventWorkTypeId)}
            {workType.levelName || workType.level ? ` · ${workType.levelName || workType.level}` : ''}
          </Tag>
        ))}</Space>;
      },
    },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '身份证号', dataIndex: 'idCardNo', width: 150 },
    { title: '手机号', dataIndex: 'mobile', width: 130 },
    { title: '参赛状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '操作', key: 'actions', fixed: 'right' as const, width: 140,
      render: (_: unknown, record: CompetitionPlayer) => (
        <Space size={0}>
          {canUpdate && <Button type="link" size="small" icon={<EditOutlined />} loading={editLoadingId === record.registrationId} disabled={editLoadingId != null} onClick={() => { void openEdit(record); }}>编辑</Button>}
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
            onChange={(value) => { setSelectedEventId(value); setQuery((current) => ({ ...current, page: 1 })); setSelectedOrgId(undefined); setSelectedWorkTypeId(undefined); setSelectedWorkTypes([]); }}
            placeholder="选择赛事任务"
            style={{ width: 280 }}
          />
          <Select allowClear showSearch optionFilterProp="label" value={selectedOrgId} options={orgOptions} onChange={(value) => { setSelectedOrgId(value); setQuery((current) => ({ ...current, page: 1 })); }} placeholder="按企业筛选" style={{ width: 220 }} loading={organizationsQuery.isLoading} />
          <Select allowClear showSearch optionFilterProp="label" value={selectedWorkTypeId} options={workTypeOptions} onChange={(value) => { setSelectedWorkTypeId(value); setQuery((current) => ({ ...current, page: 1 })); }} placeholder="按工种筛选" style={{ width: 200 }} loading={workTypesQuery.isLoading} />
          <Input allowClear prefix={<SearchOutlined />} placeholder="姓名" value={keyword} onChange={(event) => setKeyword(event.target.value)} onPressEnter={() => setQuery((current) => ({ ...current, page: 1, keyword: keyword.trim() || undefined }))} style={{ width: 220 }} />
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
        width={700}
        confirmLoading={saveMutation.isPending}
        onCancel={() => { setFormOpen(false); setEditing(undefined); setSelectedWorkTypes([]); form.resetFields(); }}
        onOk={() => form.submit()}
      >
        <Form<CompetitionPlayerFormValues> form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
          <Form.Item label="赛事任务"><Input value={events.find((event) => idValue(event.id) === selectedEventId)?.name || selectedEventId} disabled /></Form.Item>
          <Form.Item name="orgId" label="企业" rules={[{ required: true, message: '请选择企业' }]}>
            <Select options={orgOptions} showSearch optionFilterProp="label" placeholder="请选择企业" />
          </Form.Item>
          <Form.Item
            label="报名工种"
            required
            help="可选择多个工种；每个工种必须单独选择其对应等级。"
            validateStatus={selectedWorkTypes.length > 0 && selectedWorkTypes.every((item) => item.eventWorkTypeLevelId) ? undefined : 'warning'}
          >
            <Select
              mode="multiple"
              value={selectedWorkTypes.map((item) => item.eventWorkTypeId)}
              options={workTypeOptions}
              showSearch
              optionFilterProp="label"
              placeholder="请选择一个或多个赛事工种"
              onChange={(values: string[]) => {
                setSelectedWorkTypes((current) => values.map((value) => (
                  current.find((item) => item.eventWorkTypeId === value)
                    || { eventWorkTypeId: value }
                )));
              }}
            />
            <Space direction="vertical" size={8} style={{ width: '100%', marginTop: 12 }}>
              {selectedWorkTypes.map((selection) => {
                const workType = workTypeOptions.find((item) => item.value === selection.eventWorkTypeId);
                const levels = levelsByWorkType[selection.eventWorkTypeId] || [];
                const loading = workTypeLevelQueries[workTypeOptions.findIndex(
                  (item) => item.value === selection.eventWorkTypeId,
                )]?.isLoading;
                return (
                  <Space key={selection.eventWorkTypeId} style={{ display: 'flex' }} align="center">
                    <Typography.Text style={{ minWidth: 150 }}>{workType?.label || selection.eventWorkTypeId}</Typography.Text>
                    <Select
                      value={selection.eventWorkTypeLevelId}
                      options={levels}
                      loading={loading}
                      disabled={!levels.length}
                      allowClear
                      placeholder={levels.length ? '选择对应等级' : '该工种暂无已配置等级'}
                      style={{ minWidth: 250 }}
                      onChange={(value) => setSelectedWorkTypes((current) => current.map((item) => (
                        item.eventWorkTypeId === selection.eventWorkTypeId
                          ? { ...item, eventWorkTypeLevelId: value }
                          : item
                      )))}
                    />
                  </Space>
                );
              })}
            </Space>
          </Form.Item>
          <div className="competition-player-identity-fields">
            <Form.Item name="name" label="姓名" rules={[{ required: true, whitespace: true, message: '请输入姓名' }]}>
              <Input maxLength={128} />
            </Form.Item>
            <Form.Item
              name="idCardNo"
              label="身份证号"
              rules={[
                { required: !editing, whitespace: !editing, message: '请输入身份证号' },
                {
                  validator: (_, value?: string) => (
                    !value?.trim() || isValidIdCardNo(value)
                      ? Promise.resolve()
                      : Promise.reject(new Error('请输入有效的18位身份证号'))
                  ),
                },
              ]}
              extra={editing ? '请核对完整信息；不修改请保持原值' : undefined}
            >
              <Input maxLength={18} />
            </Form.Item>
            <Form.Item
              name="mobile"
              label="手机号"
              rules={[
                { required: !editing, whitespace: !editing, message: '请输入手机号' },
                {
                  validator: (_, value?: string) => (
                    !value?.trim() || isValidMobile(value)
                      ? Promise.resolve()
                      : Promise.reject(new Error('请输入有效的11位手机号'))
                  ),
                },
              ]}
              extra={editing ? '请核对完整信息；不修改请保持原值' : undefined}
            >
              <Input maxLength={11} inputMode="numeric" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
}

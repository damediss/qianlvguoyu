import {
  ArrowLeftOutlined,
  CalendarOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  CopyOutlined,
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SaveOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
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
import dayjs, { type Dayjs } from 'dayjs';
import QRCode from 'qrcode';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  createCompetitionEvent,
  createCompetitionEventWorkType,
  getCompetitionEvent,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
  updateCompetitionEventWorkType,
  updateCompetitionEvent,
} from '../../api/competition';
import { treeWorkTypes } from '../../api/exam';
import type {
  CompetitionEvent,
  CompetitionEventCreateRequest,
  CompetitionEventPageQuery,
  CompetitionEventScope,
  CompetitionEventStatus,
  CompetitionEventWorkType,
  CompetitionEventUpdateRequest,
  CompetitionId,
} from '../../types/competition';
import type { ExamWorkType } from '../../types/exam';
import { useAuthStore } from '../../stores/authStore';
import { useNavigationBlocker } from '../../hooks/useUnsavedChangesBlocker';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import { CompetitionEventOrganizationsPage } from './CompetitionEventOrganizationsPage';
import { CompetitionRosterImportPage } from './CompetitionRosterImportPage';
import { CompetitionPlayerManagementPage } from './CompetitionPlayerManagementPage';
import { CompetitionNoticePage } from './CompetitionNoticePage';
import { CompetitionScoreRankingPage } from './CompetitionScoreRankingPage';
import { CompetitionTheoryExamPage } from './CompetitionTheoryExamPage';
import { CompetitionEventModulePage, type CompetitionEventModule } from './CompetitionEventModulePage';

type CompetitionWorkspaceRoute = {
  mode: 'organizations' | 'rosters' | 'notices' | 'theory-exam' | 'scores';
  eventId: string;
};

type EventRoute =
  | { mode: 'list' | 'create'; eventId?: never }
  | { mode: 'detail' | 'edit'; eventId: string }
  | CompetitionWorkspaceRoute
  | { mode: 'players' }
  | { mode: 'module'; module: CompetitionEventModule };

interface EventFormValues {
  name: string;
  code: string;
  scope: CompetitionEventScope;
  startAt?: Dayjs;
  endAt?: Dayjs;
  registrationAuditRequired: number;
  description?: string;
}

const scopeOptions = [
  { label: '平台赛事', value: 'platform' },
  { label: '企业内部赛', value: 'enterprise_internal' },
];

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
  { label: '已关闭', value: 'closed' },
  { label: '已归档', value: 'archived' },
];

const statusMeta: Record<CompetitionEventStatus, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  published: { label: '已发布', color: 'success' },
  closed: { label: '已关闭', color: 'warning' },
  archived: { label: '已归档', color: 'error' },
};

function eventRoute(): EventRoute {
  const modulePathMap: Record<string, CompetitionEventModule> = {
    '/competition/stages': 'stages',
    '/competition/notices': 'notices',
    '/competition/theory-exams': 'theory-exam',
    '/competition/scores': 'scores',
  };
  const module = modulePathMap[window.location.pathname.replace(/\/$/, '')];
  if (module) return { mode: 'module', module };
  if (window.location.pathname.replace(/\/$/, '') === '/competition/players') {
    return { mode: 'players' };
  }
  const theoryExamMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/theory-exam\/?$/);
  if (theoryExamMatch?.[1]) {
    return { mode: 'theory-exam', eventId: decodeURIComponent(theoryExamMatch[1]) };
  }
  const scoresMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/scores\/?$/);
  if (scoresMatch?.[1]) {
    return { mode: 'scores', eventId: decodeURIComponent(scoresMatch[1]) };
  }
  const noticesMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/notices\/?$/);
  if (noticesMatch?.[1]) {
    return { mode: 'notices', eventId: decodeURIComponent(noticesMatch[1]) };
  }
  const rostersMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/rosters\/?$/);
  if (rostersMatch?.[1]) {
    return { mode: 'rosters', eventId: decodeURIComponent(rostersMatch[1]) };
  }
  const organizationsMatch = window.location.pathname.match(/^\/competition\/events\/([^/]+)\/organizations\/?$/);
  if (organizationsMatch?.[1]) {
    return { mode: 'organizations', eventId: decodeURIComponent(organizationsMatch[1]) };
  }
  const match = window.location.pathname.match(/^\/competition\/events(?:\/([^/]+))?(?:\/(edit))?\/?$/);
  if (!match?.[1]) {
    return { mode: 'list' };
  }
  if (match[1] === 'new') {
    return { mode: 'create' };
  }
  const eventId = match[1];
  return { mode: match[2] ? 'edit' : 'detail', eventId: decodeURIComponent(eventId) };
}

function idValue(id: CompetitionId) {
  return String(id);
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function scopeLabel(scope?: string) {
  return scopeOptions.find((item) => item.value === scope)?.label || scope || '-';
}

function statusTag(status?: CompetitionEventStatus) {
  const meta = status ? statusMeta[status] : undefined;
  return <Tag color={meta?.color}>{meta?.label || status || '-'}</Tag>;
}

function dateTimeValue(value?: string | null) {
  return value ? dayjs(value) : undefined;
}

function toDateTime(value?: Dayjs) {
  return value?.format('YYYY-MM-DDTHH:mm:ss');
}

async function copyText(value: string, successText: string) {
  try {
    await navigator.clipboard.writeText(value);
    message.success(successText);
  } catch {
    message.error('复制失败，请手动复制');
  }
}

function playerEntryQrPayload(entryCode: string) {
  return `qianlvguoyu-player://competition/activate?entry=${encodeURIComponent(entryCode)}`;
}

function downloadDataUrl(dataUrl: string, fileName: string) {
  const link = document.createElement('a');
  link.href = dataUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

export function CompetitionEventManagementPage() {
  const [route, setRoute] = useState<EventRoute>(eventRoute);
  const { confirmNavigation } = useNavigationBlocker();
  const currentPathRef = useRef(window.location.pathname);

  useEffect(() => {
    const onPopState = () => {
      const nextPath = window.location.pathname;
      void confirmNavigation().then((allowed) => {
        if (!allowed) {
          window.history.pushState({}, '', currentPathRef.current);
          setRoute(eventRoute());
          return;
        }
        currentPathRef.current = nextPath;
        setRoute(eventRoute());
      });
    };
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, [confirmNavigation]);

  const navigate = async (path: string) => {
    if (!(await confirmNavigation())) return;
    window.history.pushState({}, '', path);
    currentPathRef.current = path;
    setRoute(eventRoute());
  };

  if (route.mode === 'list') {
    return <CompetitionEventListPage onNavigate={navigate} />;
  }
  if (route.mode === 'create') {
    return <CompetitionEventListPage onNavigate={navigate} initialCreateOpen />;
  }
  if (route.mode === 'edit') {
    return <CompetitionEventListPage onNavigate={navigate} initialEditEventId={route.eventId} />;
  }
  if (route.mode === 'detail') {
    return <CompetitionEventListPage onNavigate={navigate} initialDetailEventId={route.eventId} />;
  }
  if (route.mode === 'module') {
    return <CompetitionEventModulePage module={route.module} onNavigate={navigate} />;
  }
  if (route.mode === 'players') {
    return <CompetitionPlayerManagementPage onNavigate={navigate} />;
  }
  if (route.mode === 'notices' || route.mode === 'theory-exam' || route.mode === 'scores') {
    return <CompetitionEventModulePage module={route.mode} onNavigate={navigate} initialEventId={route.eventId} />;
  }
  if (route.mode === 'organizations' || route.mode === 'rosters') {
    return <CompetitionEventListPage onNavigate={navigate} initialWorkspaceRoute={route} />;
  }
  return null;
}

function CompetitionEventListPage({
  onNavigate,
  initialCreateOpen = false,
  initialEditEventId,
  initialDetailEventId,
  initialWorkspaceRoute,
}: {
  onNavigate: (path: string) => void;
  initialCreateOpen?: boolean;
  initialEditEventId?: string;
  initialDetailEventId?: string;
  initialWorkspaceRoute?: CompetitionWorkspaceRoute;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:event:view');
  const canCreate = permissions.includes('competition:event:create');
  const [keywordInput, setKeywordInput] = useState('');
  const [query, setQuery] = useState<CompetitionEventPageQuery>({ page: 1, pageSize: 10 });
  const [createOpen, setCreateOpen] = useState(initialCreateOpen);
  const [editingEventId, setEditingEventId] = useState<string | undefined>(initialEditEventId);
  const [detailEventId, setDetailEventId] = useState<string | undefined>(initialDetailEventId);
  const [workspaceRoute, setWorkspaceRoute] = useState(initialWorkspaceRoute);
  const queryClient = useQueryClient();

  useEffect(() => {
    setCreateOpen(initialCreateOpen);
    setEditingEventId(initialEditEventId);
    setDetailEventId(initialDetailEventId);
    setWorkspaceRoute(initialWorkspaceRoute);
  }, [initialCreateOpen, initialDetailEventId, initialEditEventId, initialWorkspaceRoute]);

  const listQuery = useQuery({
    queryKey: ['competition-events', query],
    queryFn: () => pageCompetitionEvents(query),
    enabled: canView,
  });

  const events = listQuery.data?.items || [];
  const columns = useMemo(() => [
    {
      title: '名称',
      dataIndex: 'name',
      render: (value: string, record: CompetitionEvent) => (
        <Button type="link" className="competition-event-name" onClick={() => setDetailEventId(idValue(record.id))}>
          {value}
        </Button>
      ),
    },
    { title: '编码', dataIndex: 'code', width: 180 },
    { title: '范围', dataIndex: 'scope', width: 130, render: scopeLabel },
    { title: '状态', dataIndex: 'status', width: 110, render: statusTag },
    { title: '开始时间', dataIndex: 'startAt', width: 170, render: formatDate },
    { title: '结束时间', dataIndex: 'endAt', width: 170, render: formatDate },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_: unknown, record: CompetitionEvent) => (
        <Space size={0}>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setDetailEventId(idValue(record.id))}>
            详情
          </Button>
        </Space>
      ),
    },
  ], [onNavigate]);

  const closeFormModal = () => {
    setCreateOpen(false);
    setEditingEventId(undefined);
  };

  const closeDetailModal = () => setDetailEventId(undefined);

  const closeActiveModal = () => {
    closeFormModal();
    closeDetailModal();
    setWorkspaceRoute(undefined);
  };

  const handleFormNavigate = (path: string) => {
    closeActiveModal();
    onNavigate(path);
  };

  const handleDetailNavigate = (path: string) => {
    onNavigate(path);
  };

  const modalOpen = createOpen || Boolean(editingEventId) || Boolean(detailEventId) || Boolean(workspaceRoute);

  if (!canView) {
    return <Result status="403" title="暂无赛事查看权限" subTitle="请联系管理员申请 competition:event:view 权限。" />;
  }

  return (
    <div className="competition-page">
      <div className="competition-list-actions">
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['competition-events'] })}>
            刷新
          </Button>
          {canCreate && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
              创建赛事
            </Button>
          )}
        </Space>
      </div>

      <Card bordered={false} className="competition-filter-card competition-filter-card--workspace">
        <div className="competition-filter-card__head">
          <div>
            <Typography.Title level={4}>计划列表</Typography.Title>
            <Typography.Text type="secondary">点击计划名称进入赛事工作台</Typography.Text>
          </div>
          <Tag color="blue"><SafetyCertificateOutlined /> 权限已按菜单控制</Tag>
        </div>
        <Space wrap className="competition-filter-card__body">
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="搜索赛事名称或编码"
            value={keywordInput}
            onChange={(event) => setKeywordInput(event.target.value)}
            onPressEnter={() => setQuery((current) => ({ ...current, page: 1, keyword: keywordInput.trim() || undefined }))}
            style={{ width: 280 }}
          />
          <Select
            allowClear
            placeholder="按状态筛选"
            options={statusOptions}
            value={query.status}
            onChange={(status: CompetitionEventStatus | undefined) => setQuery((current) => ({ ...current, page: 1, status }))}
            style={{ width: 150 }}
          />
          <Button type="primary" ghost icon={<SearchOutlined />} onClick={() => setQuery((current) => ({ ...current, page: 1, keyword: keywordInput.trim() || undefined }))}>
            搜索
          </Button>
        </Space>
      </Card>

      <Card bordered={false} className="competition-table-card competition-plan-table-card">
        <Table<CompetitionEvent>
          rowKey={(record) => idValue(record.id)}
          loading={listQuery.isLoading}
          dataSource={events}
          columns={columns}
          scroll={{ x: 1050 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: listQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })),
          }}
        />
      </Card>

      <Modal
        title={editingEventId ? '编辑竞赛计划' : detailEventId ? '赛事详情' : workspaceRoute ? competitionWorkspaceTitle(workspaceRoute.mode) : '创建竞赛计划'}
        open={modalOpen}
        centered
        width={workspaceRoute?.mode === 'scores' || workspaceRoute?.mode === 'theory-exam' ? 1320 : 920}
        footer={null}
        destroyOnClose
        className="competition-create-modal"
        onCancel={closeActiveModal}
      >
        {detailEventId ? (
          <CompetitionEventDetailPage eventId={detailEventId} onNavigate={handleDetailNavigate} inModal />
        ) : workspaceRoute ? (
          <div className="competition-detail-modal-content">
            <CompetitionEventWorkspaceModalContent route={workspaceRoute} onNavigate={handleDetailNavigate} />
          </div>
        ) : (
          <CompetitionEventFormPage
            eventId={editingEventId}
            onNavigate={handleFormNavigate}
            inModal
            onClose={closeFormModal}
          />
        )}
      </Modal>
    </div>
  );
}

function competitionWorkspaceTitle(mode: CompetitionWorkspaceRoute['mode']) {
      return {
        organizations: '参与企业',
        rosters: '名单导入',
        notices: '公告设置',
        'theory-exam': '理论考试',
        scores: '成绩与排名',
      }[mode];
}

function CompetitionEventWorkspaceModalContent({
  route,
  onNavigate,
}: {
  route: CompetitionWorkspaceRoute;
  onNavigate: (path: string) => void;
}) {
  if (route.mode === 'organizations') return <CompetitionEventOrganizationsPage eventId={route.eventId} onNavigate={onNavigate} />;
  if (route.mode === 'rosters') return <CompetitionRosterImportPage eventId={route.eventId} onNavigate={onNavigate} />;
  if (route.mode === 'theory-exam') return <CompetitionTheoryExamPage eventId={route.eventId} onNavigate={onNavigate} />;
  if (route.mode === 'scores') return <CompetitionScoreRankingPage eventId={route.eventId} onNavigate={onNavigate} />;
  return <CompetitionNoticePage eventId={route.eventId} onNavigate={onNavigate} />;
}

function CompetitionEventFormPage({
  eventId,
  onNavigate,
  inModal = false,
  onClose,
}: {
  eventId?: string;
  onNavigate: (path: string) => void;
  inModal?: boolean;
  onClose?: () => void;
}) {
  const isEdit = Boolean(eventId);
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:event:view');
  const canCreate = permissions.includes('competition:event:create');
  const canUpdate = permissions.includes('competition:event:update');
  const [form] = Form.useForm<EventFormValues>();
  const queryClient = useQueryClient();

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId!),
    enabled: canView && isEdit,
  });

  useEffect(() => {
    if (!eventQuery.data) return;
    form.setFieldsValue({
      name: eventQuery.data.name,
      code: eventQuery.data.code,
      scope: eventQuery.data.scope,
      startAt: dateTimeValue(eventQuery.data.startAt),
      endAt: dateTimeValue(eventQuery.data.endAt),
      registrationAuditRequired: eventQuery.data.registrationAuditRequired,
      description: eventQuery.data.description || undefined,
    });
  }, [eventQuery.data, form]);

  const createMutation = useMutation({
    mutationFn: createCompetitionEvent,
    onSuccess: (event) => {
      message.success('赛事草稿已创建');
      queryClient.invalidateQueries({ queryKey: ['competition-events'] });
      onNavigate(`/competition/events/${event.id}`);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: CompetitionId; payload: CompetitionEventUpdateRequest }) => updateCompetitionEvent(id, payload),
    onSuccess: (event) => {
      message.success('赛事草稿已保存');
      queryClient.setQueryData(['competition-event', event.id], event);
      queryClient.invalidateQueries({ queryKey: ['competition-events'] });
      onNavigate(`/competition/events/${event.id}`);
    },
  });

  if (!canView || (isEdit && eventQuery.isLoading)) {
    return canView ? <Spin className="competition-loading" /> : <Result status="403" title="暂无赛事查看权限" />;
  }
  if (eventQuery.isError) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回列表</Button>} />;
  }
  if (isEdit && eventQuery.data?.status !== 'draft') {
    return (
      <Result
        status="warning"
        title="已发布赛事不可编辑"
        subTitle="只有 draft 状态赛事允许修改。"
        extra={<Button onClick={() => onNavigate(`/competition/events/${eventId}`)}>查看详情</Button>}
      />
    );
  }
  if (!isEdit && !canCreate) {
    return <Result status="403" title="暂无赛事创建权限" />;
  }
  if (isEdit && !canUpdate) {
    return <Result status="403" title="暂无赛事编辑权限" />;
  }

  const handleFinish = (values: EventFormValues) => {
    if (values.startAt && values.endAt && values.endAt.isBefore(values.startAt)) {
      message.error('结束时间不能早于开始时间');
      return;
    }
    if (isEdit) {
      const payload: CompetitionEventUpdateRequest = {
        name: values.name.trim(),
        startAt: toDateTime(values.startAt),
        endAt: toDateTime(values.endAt),
        registrationAuditRequired: values.registrationAuditRequired,
        description: values.description?.trim() || undefined,
      };
      updateMutation.mutate({ id: eventId!, payload });
      return;
    }
    const payload: CompetitionEventCreateRequest = {
      name: values.name.trim(),
      code: values.code.trim(),
      scope: values.scope,
      startAt: toDateTime(values.startAt),
      endAt: toDateTime(values.endAt),
      registrationAuditRequired: values.registrationAuditRequired,
      description: values.description?.trim() || undefined,
    };
    createMutation.mutate(payload);
  };

  const formContent = (
    <>
      <Card bordered={false} className="competition-form-card">
        <div className="competition-form-card__intro">
          <div className="competition-section-icon"><FileTextOutlined /></div>
          <div>
            <Typography.Title level={4}>填写竞赛计划基础信息</Typography.Title>
          </div>
        </div>
        <Form<EventFormValues>
          form={form}
          layout="vertical"
          initialValues={{ scope: 'platform', registrationAuditRequired: 0 }}
          onFinish={handleFinish}
          requiredMark="optional"
        >
          <div className="competition-form-grid">
            <Form.Item name="name" label="赛事名称" rules={[{ required: true, whitespace: true, message: '请输入赛事名称' }, { max: 128, message: '名称不能超过128个字符' }]}>
              <Input placeholder="请输入赛事名称" maxLength={128} showCount />
            </Form.Item>
            <Form.Item name="code" label="赛事编码" rules={[{ required: true, whitespace: true, message: '请输入赛事编码' }, { max: 64, message: '编码不能超过64个字符' }]}>
              <Input placeholder="请输入唯一赛事编码" maxLength={64} disabled={isEdit} showCount />
            </Form.Item>
            <Form.Item name="scope" label="赛事范围" rules={[{ required: true, message: '请选择赛事范围' }]}>
              <Select options={scopeOptions} disabled={isEdit} />
            </Form.Item>
            <Form.Item name="registrationAuditRequired" label="报名资格审核">
              <Select options={[{ label: '不需要审核', value: 0 }, { label: '需要审核', value: 1 }]} />
            </Form.Item>
            <Form.Item name="startAt" label="开始时间">
              <DatePicker showTime style={{ width: '100%' }} placeholder="请选择开始时间" />
            </Form.Item>
            <Form.Item
              name="endAt"
              label="结束时间"
              dependencies={['startAt']}
              rules={[({ getFieldValue }) => ({
                validator(_, value: Dayjs | undefined) {
                  const startAt = getFieldValue('startAt') as Dayjs | undefined;
                  if (!value || !startAt || !value.isBefore(startAt)) return Promise.resolve();
                  return Promise.reject(new Error('结束时间不能早于开始时间'));
                },
              })]}
            >
              <DatePicker showTime style={{ width: '100%' }} placeholder="请选择结束时间" />
            </Form.Item>
          </div>
          <Form.Item name="description" label="赛事描述" rules={[{ max: 2000, message: '描述不能超过2000个字符' }]}>
            <Input.TextArea rows={5} maxLength={2000} showCount placeholder="请输入赛事描述" />
          </Form.Item>
          <Space>
            <Button onClick={() => (inModal ? onClose?.() : onNavigate('/competition/events'))}>取消</Button>
            <Button type="primary" htmlType="submit" loading={createMutation.isPending || updateMutation.isPending}>
              保存草稿
            </Button>
          </Space>
        </Form>
      </Card>
    </>
  );

  if (inModal) {
    return <div className="competition-form-modal-content">{formContent}</div>;
  }

  return (
    <div className="competition-page">
      <section className="competition-page-heading competition-form-hero">
        <div>
          <Button size="large" icon={<ArrowLeftOutlined />} onClick={() => onNavigate('/competition/events')}>
            返回
          </Button>
          <Typography.Title level={2}>{isEdit ? '编辑竞赛计划' : '创建竞赛计划'}</Typography.Title>
        </div>
      </section>
      {formContent}
    </div>
  );
}

function CompetitionEventDetailPage({
  eventId,
  onNavigate,
  inModal = false,
}: {
  eventId: string;
  onNavigate: (path: string) => void;
  inModal?: boolean;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:event:view');
  const canUpdate = permissions.includes('competition:event:update');
  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canView,
  });
  const entryCode = eventQuery.data?.playerEntryCode || '';
  const [entryQrDataUrl, setEntryQrDataUrl] = useState('');

  useEffect(() => {
    let disposed = false;
    if (!entryCode) {
      setEntryQrDataUrl('');
      return () => { disposed = true; };
    }
    QRCode.toDataURL(playerEntryQrPayload(entryCode), {
      width: 360,
      margin: 2,
      errorCorrectionLevel: 'M',
    }).then((dataUrl) => {
      if (!disposed) setEntryQrDataUrl(dataUrl);
    }).catch(() => {
      if (!disposed) setEntryQrDataUrl('');
    });
    return () => { disposed = true; };
  }, [entryCode]);
  if (!canView) return <Result status="403" title="暂无赛事查看权限" />;
  if (eventQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventQuery.isError || !eventQuery.data) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回列表</Button>} />;
  }

  const event = eventQuery.data;
  const isDraft = event.status === 'draft';
  const workspaceActions = (
    <Space wrap>
      {isDraft && canUpdate && (
        <Button icon={<EditOutlined />} onClick={() => onNavigate(`/competition/events/${event.id}/edit`)}>编辑计划</Button>
      )}
    </Space>
  );

  const detailContent = (
    <CompetitionEventFrame
      event={event}
      activeSection="overview"
      onNavigate={onNavigate}
      actions={workspaceActions}
    >
      <section className="competition-detail-grid competition-detail-grid--single">
        <Card bordered={false} className="competition-detail-card competition-detail-card--main">
          <div className="competition-card-heading">
            <div><Typography.Title level={4}>竞赛计划</Typography.Title><Typography.Text type="secondary">基础信息与当前版本</Typography.Text></div>
            <Tag className="competition-status-tag" color={isDraft ? 'orange' : 'blue'}>{isDraft ? '待完善' : '运营中'}</Tag>
          </div>
          <Descriptions column={{ xs: 1, sm: 2 }}>
            <Descriptions.Item label="计划名称">{event.name}</Descriptions.Item>
            <Descriptions.Item label="计划编码">{event.code}</Descriptions.Item>
            {event.status === 'published' && (
              <Descriptions.Item label="选手端赛事入口码" span={2}>
                {event.playerEntryCode ? (
                  <Space size={8} wrap>
                    <Typography.Text code copyable={{ text: event.playerEntryCode, tooltips: ['复制入口码', '已复制'] }}>
                      {event.playerEntryCode}
                    </Typography.Text>
                    <Button
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => void copyText(event.playerEntryCode!, '选手端入口码已复制')}
                    >
                      复制入口码
                    </Button>
                    <Typography.Text type="secondary">用于打开选手端 H5 的 entry 参数</Typography.Text>
                  </Space>
                ) : (
                  <Typography.Text type="warning">入口码未生成，请联系管理员核查发布记录。</Typography.Text>
                )}
              </Descriptions.Item>
            )}
            {event.status === 'published' && entryCode && (
              <Descriptions.Item label="赛事激活二维码" span={2}>
                <Space size={16} align="start" wrap>
                  {entryQrDataUrl ? (
                    <img className="competition-entry-qr" src={entryQrDataUrl} alt={`${event.name} 选手端激活二维码`} />
                  ) : <Spin size="small" />}
                  <div className="competition-entry-qr__actions">
                    <Typography.Text>请在选手端 App 首页点击“扫一扫”后扫描</Typography.Text>
                    <Typography.Text type="secondary">二维码不含选手个人信息，仅指向本赛事激活入口。</Typography.Text>
                    <Button
                      size="small"
                      icon={<DownloadOutlined />}
                      disabled={!entryQrDataUrl}
                      onClick={() => downloadDataUrl(entryQrDataUrl, `${event.code}-选手端赛事二维码.png`)}
                    >
                      下载二维码
                    </Button>
                  </div>
                </Space>
              </Descriptions.Item>
            )}
            <Descriptions.Item label="开始时间">{formatDate(event.startAt)}</Descriptions.Item>
            <Descriptions.Item label="结束时间">{formatDate(event.endAt)}</Descriptions.Item>
            <Descriptions.Item label="所属组织">{event.ownerOrgId ? idValue(event.ownerOrgId) : '平台'}</Descriptions.Item>
            <Descriptions.Item label="赛事 ID">{idValue(event.id)}</Descriptions.Item>
            <Descriptions.Item label="计划说明" span={2}>{event.description || '暂未填写计划说明'}</Descriptions.Item>
          </Descriptions>
        </Card>

        <CompetitionEventWorkTypeCard
          eventId={idValue(event.id)}
          canView={permissions.includes('competition:event-work-type:view')}
          canManage={permissions.includes('competition:event-work-type:manage')}
        />

      </section>

      <Card bordered={false} className="competition-detail-card competition-audit-card">
        <div className="competition-card-heading">
          <div><Typography.Title level={4}>计划留痕</Typography.Title><Typography.Text type="secondary">关键操作与版本信息</Typography.Text></div>
          <FileTextOutlined className="competition-card-heading__icon" />
        </div>
        <div className="competition-audit-line"><span className="competition-audit-dot" /><div><strong>当前状态：{statusMeta[event.status].label}</strong><small>最后更新时间：{formatDate(event.updatedAt)}</small></div></div>
        <div className="competition-audit-line"><span className="competition-audit-dot competition-audit-dot--muted" /><div><strong>计划已创建</strong><small>创建时间：{formatDate(event.createdAt)} · 创建人：{event.createdBy ? idValue(event.createdBy) : '系统'}</small></div></div>
      </Card>
    </CompetitionEventFrame>
  );

  return inModal ? <div className="competition-detail-modal-content">{detailContent}</div> : detailContent;
}

type CompetitionWorkTypeOption = {
  value: string;
  label: string;
  title: string;
  disabled?: boolean;
};

function flattenExamWorkTypes(
  nodes: ExamWorkType[],
): Array<{ id: number; label: string; title: string; status: number }> {
  return nodes.flatMap((node) => {
    if (node.children?.length) {
      return flattenExamWorkTypes(node.children);
    }
    return [{ id: node.id, label: node.title, title: node.title, status: node.status }];
  });
}

function CompetitionEventWorkTypeCard({
  eventId,
  canView,
  canManage,
}: {
  eventId: string;
  canView: boolean;
  canManage: boolean;
}) {
  const eventWorkTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canView,
  });
  const sourceWorkTypesQuery = useQuery({
    queryKey: ['exam-work-types-tree'],
    queryFn: treeWorkTypes,
    enabled: canView && canManage,
  });
  const queryClient = useQueryClient();
  const [selectedWorkTypeIds, setSelectedWorkTypeIds] = useState<string[]>([]);

  useEffect(() => {
    if (eventWorkTypesQuery.data) {
      setSelectedWorkTypeIds(
        eventWorkTypesQuery.data
          .filter((item) => item.status === 'active')
          .map((item) => idValue(item.workTypeId)),
      );
    }
  }, [eventWorkTypesQuery.data]);

  const sourceOptions = useMemo<CompetitionWorkTypeOption[]>(() => {
    const options = new Map<string, CompetitionWorkTypeOption>();
    flattenExamWorkTypes(sourceWorkTypesQuery.data || []).forEach((item) => {
      options.set(String(item.id), {
        value: String(item.id),
        label: item.status === 1 ? item.label : `${item.label}（已停用）`,
        title: item.title,
        disabled: item.status !== 1,
      });
    });
    return Array.from(options.values());
  }, [sourceWorkTypesQuery.data]);

  const options = useMemo<CompetitionWorkTypeOption[]>(() => {
    const optionMap = new Map(sourceOptions.map((option) => [option.value, option]));
    (eventWorkTypesQuery.data || [])
      .filter((item) => item.status === 'active')
      .forEach((item) => {
        const value = idValue(item.workTypeId);
        if (!optionMap.has(value)) {
          optionMap.set(value, {
            value,
            label: `${item.displayName || item.workTypeName || value}（工种已不可用）`,
            title: item.displayName || item.workTypeName || value,
            disabled: true,
          });
        }
      });
    return Array.from(optionMap.values());
  }, [eventWorkTypesQuery.data, sourceOptions]);

  const optionById = useMemo(
    () => new Map(options.map((option) => [option.value, option])),
    [options],
  );
  const activeEventWorkTypes = useMemo(
    () => (eventWorkTypesQuery.data || []).filter((item) => item.status === 'active'),
    [eventWorkTypesQuery.data],
  );
  const isDirty = useMemo(() => {
    const selected = [...selectedWorkTypeIds].sort();
    const persisted = activeEventWorkTypes.map((item) => idValue(item.workTypeId)).sort();
    return selected.length !== persisted.length || selected.some((id, index) => id !== persisted[index]);
  }, [activeEventWorkTypes, selectedWorkTypeIds]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const existingByWorkTypeId = new Map<string, CompetitionEventWorkType>();
      (eventWorkTypesQuery.data || []).forEach((item) => {
        existingByWorkTypeId.set(idValue(item.workTypeId), item);
      });

      for (const [index, workTypeId] of selectedWorkTypeIds.entries()) {
        const option = optionById.get(workTypeId);
        const existing = existingByWorkTypeId.get(workTypeId);
        if (!option || option.disabled) continue;

        const payload = {
          workTypeId: Number(workTypeId),
          displayName: existing?.displayName || option.title,
          status: 'active',
          sortOrder: index,
        };
        if (!existing) {
          await createCompetitionEventWorkType(eventId, payload);
        } else if (
          existing.status !== 'active'
          || existing.sortOrder !== index
          || existing.displayName !== payload.displayName
        ) {
          await updateCompetitionEventWorkType(eventId, existing.id, payload);
        }
      }

      for (const existing of eventWorkTypesQuery.data || []) {
        if (existing.status !== 'active' || selectedWorkTypeIds.includes(idValue(existing.workTypeId))) continue;
        await updateCompetitionEventWorkType(eventId, existing.id, {
          workTypeId: Number(existing.workTypeId),
          displayName: existing.displayName || existing.workTypeName || undefined,
          status: 'inactive',
          sortOrder: existing.sortOrder,
        });
      }
    },
    onSuccess: () => {
      message.success('参赛工种已保存');
      queryClient.invalidateQueries({ queryKey: ['competition-event-work-types', eventId] });
    },
  });

  return (
    <Card bordered={false} className="competition-detail-card competition-event-work-type-card">
      <div className="competition-card-heading">
        <div>
          <Typography.Title level={4}>参赛工种</Typography.Title>
          <Typography.Text type="secondary">
            从课程管理的工种管理中选择本赛事参赛工种，支持多选
          </Typography.Text>
        </div>
      </div>

      {!canView ? (
        <Typography.Text type="secondary">暂无参赛工种查看权限</Typography.Text>
      ) : eventWorkTypesQuery.isLoading || (canManage && sourceWorkTypesQuery.isLoading) ? (
        <Spin size="small" />
      ) : eventWorkTypesQuery.isError ? (
        <Typography.Text type="danger">参赛工种加载失败，请刷新重试</Typography.Text>
      ) : canManage ? (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Select
            mode="multiple"
            allowClear
            showSearch
            optionFilterProp="label"
            style={{ width: '100%' }}
            placeholder="请选择参赛工种（支持多选）"
            value={selectedWorkTypeIds}
            options={options}
            loading={sourceWorkTypesQuery.isFetching}
            onChange={(values) => setSelectedWorkTypeIds(values.map(String))}
          />
          <Space>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={saveMutation.isPending}
              disabled={!isDirty || sourceWorkTypesQuery.isError}
              onClick={() => saveMutation.mutate()}
            >
              保存参赛工种
            </Button>
            <Typography.Text type="secondary">
              赛事绑定只影响本赛事，不会修改课程管理中的工种
            </Typography.Text>
          </Space>
        </Space>
      ) : (
        <Space wrap>
          {activeEventWorkTypes.length ? activeEventWorkTypes.map((item) => (
            <Tag key={item.id}>{item.displayName || item.workTypeName || idValue(item.workTypeId)}</Tag>
          )) : <Typography.Text type="secondary">暂未设置参赛工种</Typography.Text>}
        </Space>
      )}
    </Card>
  );
}

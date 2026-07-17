import {
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  SendOutlined,
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
import dayjs from 'dayjs';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  createCompetitionNotice,
  getCompetitionEvent,
  getCompetitionNotice,
  pageCompetitionNotices,
  publishCompetitionNotice,
  updateCompetitionNotice,
} from '../../api/competition';
import type {
  CompetitionId,
  CompetitionNotice,
  CompetitionNoticeCreateRequest,
  CompetitionNoticePageQuery,
  CompetitionNoticeStatus,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';

type NoticeMode = 'create' | 'edit' | 'view';

interface NoticeFormValues {
  title: string;
  content: string;
  version: number;
}

const statusOptions: Array<{ label: string; value: CompetitionNoticeStatus }> = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
];

const statusMeta: Record<CompetitionNoticeStatus, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  published: { label: '已发布', color: 'success' },
};

function idValue(id: CompetitionId) {
  return String(id);
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

export function CompetitionNoticePage({
  eventId,
  onNavigate,
}: {
  eventId: string;
  onNavigate: (path: string) => void;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:notice:view');
  const canManage = permissions.includes('competition:notice:publish');
  const [query, setQuery] = useState<CompetitionNoticePageQuery>({ page: 1, pageSize: 10 });
  const [noticeMode, setNoticeMode] = useState<NoticeMode>();
  const [selectedNoticeId, setSelectedNoticeId] = useState<CompetitionId>();
  const [noticeForm] = Form.useForm<NoticeFormValues>();
  const queryClient = useQueryClient();

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canView,
  });

  const noticesQuery = useQuery({
    queryKey: ['competition-notices', eventId, query],
    queryFn: () => pageCompetitionNotices(eventId, query),
    enabled: canView,
  });

  const noticeDetailQuery = useQuery({
    queryKey: ['competition-notice', selectedNoticeId],
    queryFn: () => getCompetitionNotice(selectedNoticeId!),
    enabled: canView && Boolean(selectedNoticeId),
  });

  useEffect(() => {
    const notice = noticeDetailQuery.data;
    if (!notice || !selectedNoticeId) return;
    noticeForm.setFieldsValue({
      title: notice.title,
      content: notice.content,
      version: notice.version,
    });
  }, [noticeDetailQuery.data, noticeForm, selectedNoticeId]);

  const createMutation = useMutation({
    mutationFn: (payload: CompetitionNoticeCreateRequest) => createCompetitionNotice(eventId, payload),
    onSuccess: () => {
      message.success('公告创建成功');
      closeNoticeModal();
      queryClient.invalidateQueries({ queryKey: ['competition-notices', eventId] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ noticeId, payload }: { noticeId: CompetitionId; payload: CompetitionNoticeCreateRequest }) =>
      updateCompetitionNotice(noticeId, payload),
    onSuccess: () => {
      message.success('公告已保存');
      closeNoticeModal();
      queryClient.invalidateQueries({ queryKey: ['competition-notices', eventId] });
    },
  });

  const publishMutation = useMutation({
    mutationFn: (noticeId: CompetitionId) => publishCompetitionNotice(noticeId),
    onSuccess: () => {
      message.success('公告已发布');
      queryClient.invalidateQueries({ queryKey: ['competition-notices', eventId] });
    },
  });

  const closeNoticeModal = () => {
    setNoticeMode(undefined);
    setSelectedNoticeId(undefined);
    noticeForm.resetFields();
  };

  const openCreate = () => {
    noticeForm.resetFields();
    noticeForm.setFieldsValue({ version: 1 });
    setSelectedNoticeId(undefined);
    setNoticeMode('create');
  };

  const openNotice = (notice: CompetitionNotice, mode: NoticeMode) => {
    noticeForm.setFieldsValue({
      title: notice.title,
      content: notice.content,
      version: notice.version,
    });
    setSelectedNoticeId(notice.id);
    setNoticeMode(mode);
  };

  const publish = (notice: CompetitionNotice) => {
    if (!canManage || notice.status !== 'draft') return;
    Modal.confirm({
      title: '发布公告？',
      content: `公告「${notice.title}」发布后将不可修改。`,
      okText: '发布',
      cancelText: '取消',
      onOk: () => publishMutation.mutateAsync(notice.id),
    });
  };

  const columns = useMemo(() => [
    {
      title: '标题',
      dataIndex: 'title',
      render: (value: string, record: CompetitionNotice) => (
        <Button type="link" className="competition-event-name" onClick={() => openNotice(record, 'view')}>
          {value}
        </Button>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value: CompetitionNoticeStatus) => <Tag color={statusMeta[value]?.color}>{statusMeta[value]?.label || value}</Tag>,
    },
    { title: '版本', dataIndex: 'version', width: 100 },
    { title: '创建时间', dataIndex: 'createdAt', width: 175, render: formatDate },
    { title: '发布时间', dataIndex: 'publishedAt', width: 175, render: formatDate },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      render: (_: unknown, record: CompetitionNotice) => (
        <Space size={0}>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => openNotice(record, 'view')}>查看</Button>
          {canManage && record.status === 'draft' && (
            <>
              <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openNotice(record, 'edit')}>编辑</Button>
              <Button type="link" size="small" icon={<SendOutlined />} onClick={() => publish(record)}>发布</Button>
            </>
          )}
        </Space>
      ),
    },
  ], [canManage]);

  if (!canView) {
    return <Result status="403" title="暂无公告查看权限" subTitle="请联系管理员申请 competition:notice:view 权限。" />;
  }
  if (eventQuery.isLoading || noticesQuery.isLoading) {
    return <Spin className="competition-loading" />;
  }
  if (eventQuery.isError || !eventQuery.data) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回计划</Button>} />;
  }
  if (noticesQuery.isError) {
    return <Result status="error" title="公告加载失败" extra={<Button onClick={() => noticesQuery.refetch()}>重新加载</Button>} />;
  }

  const submitNotice = (values: NoticeFormValues) => {
    const payload: CompetitionNoticeCreateRequest = {
      title: values.title.trim(),
      content: values.content.trim(),
      version: values.version,
    };
    if (noticeMode === 'edit' && selectedNoticeId) {
      updateMutation.mutate({ noticeId: selectedNoticeId, payload });
    } else if (noticeMode === 'create') {
      createMutation.mutate(payload);
    }
  };

  const isViewOnly = noticeMode === 'view';
  const modalTitle = noticeMode === 'create' ? '创建公告' : noticeMode === 'edit' ? '编辑公告' : '查看公告';

  return (
    <CompetitionEventFrame event={eventQuery.data} activeSection="notices" onNavigate={onNavigate}>
      <Card bordered={false} className="competition-filter-card">
        <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
          <Space wrap>
            <Select
              allowClear
              placeholder="按状态筛选"
              prefix={<SearchOutlined />}
              options={statusOptions}
              value={query.status}
              onChange={(status: CompetitionNoticeStatus | undefined) => setQuery((current) => ({ ...current, page: 1, status }))}
              style={{ width: 180 }}
            />
            <Button type="primary" ghost icon={<SearchOutlined />} onClick={() => noticesQuery.refetch()}>查询</Button>
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => noticesQuery.refetch()}>刷新</Button>
            {canManage && <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>创建公告</Button>}
          </Space>
        </Space>
      </Card>

      <Card bordered={false} className="competition-table-card">
        <Table<CompetitionNotice>
          rowKey={(record) => idValue(record.id)}
          loading={noticesQuery.isFetching}
          dataSource={noticesQuery.data?.items || []}
          columns={columns}
          scroll={{ x: 950 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: noticesQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条公告`,
            onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })),
          }}
        />
      </Card>

      <Modal
        title={modalTitle}
        open={Boolean(noticeMode)}
        destroyOnClose
        confirmLoading={createMutation.isPending || updateMutation.isPending || noticeDetailQuery.isFetching}
        onCancel={closeNoticeModal}
        onOk={isViewOnly ? closeNoticeModal : () => noticeForm.submit()}
        okText={isViewOnly ? '关闭' : '保存'}
        cancelButtonProps={{ style: isViewOnly ? { display: 'none' } : undefined }}
      >
        <Form<NoticeFormValues>
          form={noticeForm}
          layout="vertical"
          onFinish={submitNotice}
          disabled={isViewOnly}
          requiredMark="optional"
        >
          <Form.Item name="title" label="标题" rules={[{ required: true, whitespace: true, message: '请输入通知标题' }]}>
            <Input maxLength={256} showCount placeholder="请输入通知标题" />
          </Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true, whitespace: true, message: '请输入通知内容' }]}>
            <Input.TextArea rows={10} maxLength={16000} showCount placeholder="请输入通知内容" />
          </Form.Item>
          <Form.Item name="version" label="版本" rules={[{ required: true, type: 'number', min: 1, message: '版本必须是大于等于 1 的整数' }]}>
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </CompetitionEventFrame>
  );
}

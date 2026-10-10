import {
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { Button, Card, Modal, Result, Select, Space, Spin, Table, Tag, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import {
  getCompetitionEvent,
  pageCompetitionNotices,
  publishCompetitionNotice,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionId,
  CompetitionNotice,
  CompetitionNoticeContentType,
  CompetitionNoticePageQuery,
  CompetitionNoticeStatus,
} from '../../types/competition';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import {
  CompetitionNoticeEditorDrawer,
  contentTypeMeta,
} from './CompetitionNoticeEditorDrawer';

type NoticeMode = 'create' | 'edit' | 'view';

const statusOptions: Array<{ label: string; value: CompetitionNoticeStatus }> = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
];

const statusMeta: Record<CompetitionNoticeStatus, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  published: { label: '已发布', color: 'success' },
};

const contentTypeOptions = Object.entries(contentTypeMeta).map(([value, meta]) => ({
  label: meta.label,
  value: value as CompetitionNoticeContentType,
}));

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

export function CompetitionNoticePage({
  eventId,
  onNavigate,
  embedded = false,
}: {
  eventId: string;
  onNavigate: (path: string) => void;
  embedded?: boolean;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:notice:view');
  const canManage = permissions.includes('competition:notice:publish');
  const [query, setQuery] = useState<CompetitionNoticePageQuery>({ page: 1, pageSize: 10 });
  const [noticeMode, setNoticeMode] = useState<NoticeMode>();
  const [selectedNoticeId, setSelectedNoticeId] = useState<CompetitionId>();
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

  const publishMutation = useMutation({
    mutationFn: (noticeId: CompetitionId) => publishCompetitionNotice(noticeId),
    onSuccess: () => {
      message.success('公告已发布');
      queryClient.invalidateQueries({ queryKey: ['competition-notices', eventId] });
    },
  });

  const openNotice = (notice: CompetitionNotice, mode: NoticeMode) => {
    setSelectedNoticeId(notice.id);
    setNoticeMode(mode);
  };

  const publish = (notice: CompetitionNotice) => {
    if (!canManage || notice.status !== 'draft') return;
    const type = notice.contentType || 'markdown';
    const pdf = notice.assets?.find((asset) => asset.assetType === 'pdf');
    Modal.confirm({
      title: '发布公告？',
      content: (
        <div>
          <p>公告「{notice.title}」发布后将不可修改。</p>
          <p>发布类型：{contentTypeMeta[type].label}</p>
          {pdf && <p>赛事文件：{pdf.originalName}</p>}
        </div>
      ),
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
      title: '类型',
      dataIndex: 'contentType',
      width: 140,
      render: (value?: CompetitionNoticeContentType) => {
        const type = value || 'markdown';
        return <Tag color={contentTypeMeta[type].color}>{contentTypeMeta[type].label}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: CompetitionNoticeStatus) => (
        <Tag color={statusMeta[value]?.color}>{statusMeta[value]?.label || value}</Tag>
      ),
    },
    { title: '版本', dataIndex: 'version', width: 80 },
    { title: '创建时间', dataIndex: 'createdAt', width: 175, render: formatDate },
    { title: '发布时间', dataIndex: 'publishedAt', width: 175, render: formatDate },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      fixed: 'right' as const,
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
  if (eventQuery.isLoading || noticesQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventQuery.isError || !eventQuery.data) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回计划</Button>} />;
  }
  if (noticesQuery.isError) {
    return <Result status="error" title="公告加载失败" extra={<Button onClick={() => noticesQuery.refetch()}>重新加载</Button>} />;
  }

  const content = (
    <>
      <Card bordered={false} className="competition-filter-card">
        <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
          <Space wrap>
            <Select
              allowClear
              placeholder="按状态筛选"
              options={statusOptions}
              value={query.status}
              onChange={(status: CompetitionNoticeStatus | undefined) =>
                setQuery((current) => ({ ...current, page: 1, status }))}
              style={{ width: 150 }}
            />
            <Select
              allowClear
              placeholder="按类型筛选"
              options={contentTypeOptions}
              value={query.contentType}
              onChange={(contentType: CompetitionNoticeContentType | undefined) =>
                setQuery((current) => ({ ...current, page: 1, contentType }))}
              style={{ width: 180 }}
            />
            <Button type="primary" ghost icon={<SearchOutlined />} onClick={() => noticesQuery.refetch()}>查询</Button>
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => noticesQuery.refetch()}>刷新</Button>
            {canManage && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  setSelectedNoticeId(undefined);
                  setNoticeMode('create');
                }}
              >
                创建公告
              </Button>
            )}
          </Space>
        </Space>
      </Card>

      <Card bordered={false} className="competition-table-card">
        <Table<CompetitionNotice>
          rowKey={(record) => String(record.id)}
          loading={noticesQuery.isFetching}
          dataSource={noticesQuery.data?.items || []}
          columns={columns}
          scroll={{ x: 1080 }}
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

      <CompetitionNoticeEditorDrawer
        eventId={eventId}
        mode={noticeMode}
        noticeId={selectedNoticeId}
        onClose={() => {
          setNoticeMode(undefined);
          setSelectedNoticeId(undefined);
        }}
        onSaved={() => queryClient.invalidateQueries({ queryKey: ['competition-notices', eventId] })}
      />
    </>
  );
  return embedded ? content : <CompetitionEventFrame event={eventQuery.data} activeSection="notices" onNavigate={onNavigate}>{content}</CompetitionEventFrame>;
}

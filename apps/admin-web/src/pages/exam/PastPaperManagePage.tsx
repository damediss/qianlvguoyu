import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  ImportOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { deleteZhenti, pageZhentis, previewZhenti, updateZhenti } from '../../api/exam-zhenti';
import { pageExams } from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import type { ZhentiItem } from '../../types/examZhenti';
import { ZhentiEditModal } from './components/ZhentiEditModal';
import { ZhentiImportModal } from './components/ZhentiImportModal';
import { ZhentiUploadModal } from './components/ZhentiUploadModal';
import {
  buildZhentiQuery,
  extractErrorMessage,
  formatFileSize,
  zhentiStatusTag,
  type StatusFilter,
} from './pastPaperUtils';

export function PastPaperManagePage() {
  const queryClient = useQueryClient();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const canView = hasPermission('exam:zhenti:view');
  const canManage = hasPermission('exam:zhenti:manage');

  const [examId, setExamId] = useState<string | undefined>();
  const [keyword, setKeyword] = useState('');
  const [year, setYear] = useState<string | undefined>();
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [uploadOpen, setUploadOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [editing, setEditing] = useState<ZhentiItem | null>(null);
  const [editFocus, setEditFocus] = useState<'title' | 'year'>('title');

  const examOptionsQuery = useQuery({
    queryKey: ['zhenti-exam-options'],
    queryFn: () => pageExams({ page: 1, pageSize: 200 }),
  });
  const examOptions = examOptionsQuery.data?.items ?? [];
  const selectedExam = examOptions.find((item) => String(item.id) === examId);
  const examName = selectedExam?.title;

  const query = useMemo(
    () => buildZhentiQuery({ examId, page, pageSize, keyword, year, statusFilter }),
    [examId, page, pageSize, keyword, year, statusFilter],
  );

  const listQuery = useQuery({
    queryKey: ['zhenti-list', query],
    queryFn: () => pageZhentis(query!),
    enabled: Boolean(query),
  });

  const reload = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: ['zhenti-list'] });
  }, [queryClient]);

  const handlePreview = async (record: ZhentiItem) => {
    const tab = window.open('', '_blank');
    try {
      const result = await previewZhenti(record.id);
      if (tab) {
        tab.location.href = result.viewerUrl;
      } else {
        window.location.href = result.viewerUrl;
      }
    } catch (err) {
      tab?.close();
      message.error(extractErrorMessage(err, '打开预览失败，请重试'));
    }
  };

  const handleToggleStatus = async (record: ZhentiItem) => {
    try {
      await updateZhenti({ id: record.id, status: record.status === 1 ? 0 : 1 });
      message.success(record.status === 1 ? '已下架' : '已上架');
      reload();
    } catch (err) {
      message.error(extractErrorMessage(err, '操作失败，请重试'));
    }
  };

  const handleDelete = async (record: ZhentiItem) => {
    try {
      await deleteZhenti(record.id);
      message.success('已删除');
      reload();
    } catch (err) {
      message.error(extractErrorMessage(err, '删除失败，请重试'));
    }
  };

  const openEdit = (record: ZhentiItem, focus: 'title' | 'year') => {
    setEditing(record);
    setEditFocus(focus);
  };

  const columns: ColumnsType<ZhentiItem> = [
    {
      title: '年份',
      dataIndex: 'year',
      width: 90,
      render: (value: string | null) => value || '-',
    },
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: { showTitle: false },
      render: (value: string) => (
        <Tooltip title={value} placement="topLeft">
          <span>{value}</span>
        </Tooltip>
      ),
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      width: 110,
      render: (value: number) => formatFileSize(value),
    },
    {
      title: '状态',
      width: 90,
      render: (_, record) => {
        const tag = zhentiStatusTag(record);
        return (
          <Tooltip title={record.metaPending === 1 ? '缺年份或工种等级，请补录' : ''}>
            <Tag color={tag.color}>{tag.text}</Tag>
          </Tooltip>
        );
      },
    },
    {
      title: '上传人',
      dataIndex: 'createdBy',
      width: 110,
      render: (value: string | null) => value || '-',
    },
    {
      title: '上传时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (value: string) => value || '-',
    },
    {
      title: '操作',
      width: 330,
      fixed: 'right',
      render: (_, record) => (
        <Space size={0} wrap>
          {canView && (
            <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handlePreview(record)}>
              预览
            </Button>
          )}
          {canManage && (
            <>
              <Button type="link" size="small" onClick={() => openEdit(record, 'title')}>
                重命名
              </Button>
              <Button type="link" size="small" onClick={() => openEdit(record, 'year')}>
                改年份
              </Button>
              {record.metaPending === 1 && (
                <Button type="link" size="small" onClick={() => openEdit(record, 'year')}>
                  补录
                </Button>
              )}
              <Button type="link" size="small" onClick={() => handleToggleStatus(record)}>
                {record.status === 1 ? '下架' : '上架'}
              </Button>
              <Popconfirm
                title="删除后学员端不可见，且不可恢复（软删保留 90 天）"
                okText="删除"
                cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={() => handleDelete(record)}
              >
                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ];

  const emptyText = examId ? '该考试项目暂未导入真题' : '请先选择考试项目';

  return (
    <Card
      title="历年真题管理"
      extra={
        <Space>
          <Button
            type="primary"
            icon={<UploadOutlined />}
            disabled={!examId || !canManage}
            onClick={() => setUploadOpen(true)}
          >
            上传 PDF
          </Button>
          <Button
            icon={<ImportOutlined />}
            disabled={!examId || !canManage}
            onClick={() => setImportOpen(true)}
          >
            批量导入（zip）
          </Button>
          <Button icon={<ReloadOutlined />} onClick={reload} disabled={!query}>
            刷新
          </Button>
        </Space>
      }
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Space wrap>
          <Select
            showSearch
            allowClear
            placeholder="选择考试项目（必选）"
            style={{ width: 320 }}
            loading={examOptionsQuery.isLoading}
            value={examId}
            optionFilterProp="label"
            options={examOptions.map((item) => ({
              value: String(item.id),
              label: item.title || `考试项目 ${item.id}`,
            }))}
            onChange={(value) => {
              setExamId(value);
              setYear(undefined);
              setPage(1);
            }}
          />
          <Input.Search
            allowClear
            placeholder="搜索标题"
            style={{ width: 220 }}
            onSearch={(value) => {
              setKeyword(value);
              setPage(1);
            }}
          />
          <Select
            allowClear
            placeholder="年份"
            style={{ width: 120 }}
            value={year}
            disabled={!examId}
            options={(listQuery.data?.years ?? []).map((item) => ({ value: item, label: item }))}
            onChange={(value) => {
              setYear(value);
              setPage(1);
            }}
          />
          <Select<StatusFilter>
            style={{ width: 130 }}
            value={statusFilter}
            options={[
              { value: 'all', label: '全部状态' },
              { value: '1', label: '上架' },
              { value: '0', label: '下架' },
              { value: 'pending', label: '待完善' },
            ]}
            onChange={(value) => {
              setStatusFilter(value);
              setPage(1);
            }}
          />
        </Space>

        {listQuery.isError && (
          <Alert
            type="error"
            showIcon
            message="加载失败，请重试"
            action={
              <Button size="small" onClick={() => listQuery.refetch()}>
                重试
              </Button>
            }
          />
        )}

        <Table<ZhentiItem>
          rowKey="id"
          size="middle"
          loading={listQuery.isLoading || listQuery.isFetching}
          columns={columns}
          dataSource={query ? listQuery.data?.records ?? [] : []}
          scroll={{ x: 1180 }}
          locale={{ emptyText: <Empty description={emptyText} image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          pagination={
            query
              ? {
                  current: page,
                  pageSize,
                  total: listQuery.data?.total ?? 0,
                  showSizeChanger: true,
                  showTotal: (total) => `共 ${total} 条`,
                  onChange: (nextPage, nextSize) => {
                    setPage(nextPage);
                    setPageSize(nextSize);
                  },
                }
              : false
          }
        />
        {!canManage && (
          <Typography.Text type="secondary">
            当前账号仅有查看权限，上传、编辑、上下架与删除不可用。
          </Typography.Text>
        )}
      </Space>

      <ZhentiUploadModal
        open={uploadOpen}
        examId={examId}
        examName={examName}
        onCancel={() => setUploadOpen(false)}
        onSuccess={() => {
          setUploadOpen(false);
          reload();
        }}
      />
      <ZhentiImportModal
        open={importOpen}
        examId={examId}
        examName={examName}
        onCancel={() => setImportOpen(false)}
        onSuccess={reload}
      />
      <ZhentiEditModal
        open={Boolean(editing)}
        record={editing}
        focusField={editFocus}
        onCancel={() => setEditing(null)}
        onSuccess={() => {
          setEditing(null);
          reload();
        }}
      />
    </Card>
  );
}

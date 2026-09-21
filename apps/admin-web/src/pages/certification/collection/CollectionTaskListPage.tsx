import {
  DownloadOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  SearchOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { TableColumnsType } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import {
  exportQrCodes,
  generateQrCodes,
  getCollectionTasks,
  initializeCollection,
} from '../../../api/certificationPracticalCollection';
import { PermissionGate } from '../../../directives/permission';
import type { CollectionTaskVO } from '../../../types/certificationPracticalCollection';
import {
  formatCollectionTaskStatus,
  pendingCandidateCount,
  resolveCollectionTaskStatus,
  type CollectionTaskStatus,
} from '../../../utils/collectionTask';
import { CollectionExceptionPage } from './CollectionExceptionPage';
import './CollectionTaskListPage.css';

const { RangePicker } = DatePicker;

type CollectionTaskStatusFilter = 'all' | CollectionTaskStatus;

const statusOptions: Array<{ label: string; value: CollectionTaskStatusFilter }> = [
  { label: '全部', value: 'all' },
  { label: '待采集', value: 'pending' },
  { label: '采集中', value: 'collecting' },
  { label: '已完成', value: 'completed' },
  { label: '有异常', value: 'exception' },
];

function toNumber(value: unknown) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function pendingCount(task: CollectionTaskVO) {
  return pendingCandidateCount(task);
}

function taskStatus(task: CollectionTaskVO): CollectionTaskStatus {
  return resolveCollectionTaskStatus(task);
}

function statusTag(task: CollectionTaskVO) {
  const status = formatCollectionTaskStatus(task);
  return <Tag color={status.color}>{status.label}</Tag>;
}

function taskManageUrl(task: CollectionTaskVO) {
  const baseUrl = `/certification/practical-collection/${encodeURIComponent(String(task.taskId))}/manage`;
  if (!task.examSessionId) {
    return baseUrl;
  }
  return `${baseUrl}?examSessionId=${encodeURIComponent(String(task.examSessionId))}`;
}

function openManagePage(task: CollectionTaskVO) {
  window.location.assign(taskManageUrl(task));
}

function taskDateInRange(task: CollectionTaskVO, dateRange: [string, string] | null) {
  if (!dateRange || !dateRange[0] || !dateRange[1]) {
    return true;
  }
  if (!task.examDate) {
    return false;
  }
  const examDate = String(task.examDate);
  return examDate >= dateRange[0] && examDate <= dateRange[1];
}

export function CollectionTaskListPage() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<CollectionTaskStatusFilter>('all');
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);
  const [selectedTask, setSelectedTask] = useState<CollectionTaskVO | null>(null);
  const [exceptionTask, setExceptionTask] = useState<CollectionTaskVO | null>(null);

  const tasksQuery = useQuery({
    queryKey: ['practical-collection-tasks', keyword],
    queryFn: () => getCollectionTasks({ page: 1, pageSize: 100, keyword: keyword || undefined }),
  });

  const invalidateTasks = () => queryClient.invalidateQueries({ queryKey: ['practical-collection-tasks'] });

  const initializeMutation = useMutation({
    mutationFn: (task: CollectionTaskVO) => initializeCollection(task.taskId, task.examSessionId),
    onSuccess: (result) => {
      message.success(result.message || '采集状态已初始化');
      invalidateTasks();
    },
  });

  const generateQrMutation = useMutation({
    mutationFn: (task: CollectionTaskVO) => generateQrCodes(task.taskId, task.examSessionId),
    onSuccess: (result) => {
      message.success(result.message || '二维码 token 已生成');
      invalidateTasks();
    },
  });

  const exportMutation = useMutation({
    mutationFn: (task: CollectionTaskVO) => exportQrCodes(task.taskId, task.examSessionId),
    onSuccess: (result) => {
      const messageText = typeof result === 'object' && result && 'message' in result
        ? String((result as { message?: unknown }).message)
        : '二维码导出能力后续完善';
      message.info(messageText);
    },
  });

  const tasks = tasksQuery.data?.items || [];
  const filteredTasks = useMemo(() => tasks.filter((task) => {
    if (statusFilter !== 'all' && taskStatus(task) !== statusFilter) {
      return false;
    }
    return taskDateInRange(task, dateRange);
  }), [dateRange, statusFilter, tasks]);

  const columns: TableColumnsType<CollectionTaskVO> = [
    {
      title: '任务名称',
      dataIndex: 'taskName',
      width: 200,
      fixed: 'left',
      render: (value: string) => <Typography.Text strong>{value || '-'}</Typography.Text>,
    },
    {
      title: '场次名称',
      dataIndex: 'sessionName',
      width: 180,
      render: (value: string) => <Typography.Text strong>{value || '-'}</Typography.Text>,
    },
    {
      title: '任务编号',
      dataIndex: 'taskNo',
      width: 150,
      render: (value: string) => value || '-',
    },
    {
      title: '企业',
      dataIndex: 'projectName',
      width: 180,
      render: (value: string) => value || '-',
    },
    {
      title: '工种/等级',
      width: 160,
      render: (_, task) => task.workTypeLevel || `${task.workType || '-'} / ${task.level || '-'}`,
    },
    {
      title: '认定日期',
      dataIndex: 'examDate',
      width: 120,
      sorter: (a, b) => {
        const left = a.examDate || '';
        const right = b.examDate || '';
        if (!left && !right) {
          return 0;
        }
        if (!left) {
          return 1;
        }
        if (!right) {
          return -1;
        }
        return left.localeCompare(right);
      },
      render: (value: string) => value || '-',
    },
    {
      title: '总人数',
      dataIndex: 'candidateTotal',
      width: 72,
      align: 'right',
      render: toNumber,
    },
    {
      title: '已完成',
      dataIndex: 'completedCount',
      width: 72,
      align: 'right',
      render: toNumber,
    },
    {
      title: '待采集',
      width: 72,
      align: 'right',
      render: (_, task) => pendingCount(task),
    },
    {
      title: '异常',
      dataIndex: 'exceptionCount',
      width: 72,
      align: 'right',
      render: (value: number) => {
        const count = toNumber(value);
        return count > 0 ? <Typography.Text type="danger">{count}</Typography.Text> : count;
      },
    },
    {
      title: '状态',
      width: 92,
      render: (_, task) => statusTag(task),
    },
    {
      title: '操作',
      width: 104,
      fixed: 'right',
      render: (_, task) => (
        <Space size={4}>
          <Tooltip title="采集管理">
            <Button
              type="primary"
              size="small"
              shape="circle"
              icon={<ToolOutlined />}
              aria-label="采集管理"
              onClick={(event) => {
                event.stopPropagation();
                openManagePage(task);
              }}
            />
          </Tooltip>
          <Tooltip title="详情">
            <Button
              size="small"
              shape="circle"
              icon={<EyeOutlined />}
              aria-label="详情"
              onClick={(event) => {
                event.stopPropagation();
                setSelectedTask(task);
              }}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  const visibleColumns = columns.filter(
    (column) => !('dataIndex' in column && ['taskNo', 'projectName'].includes(String(column.dataIndex))),
  );
  const drawerTask = selectedTask;

  return (
    <PermissionGate code="certification.practical.collection.view" fallback={<Empty description="暂无信息采集访问权限" />}>
      <div className="knowledge-page collection-task-page">
        <div className="page-heading knowledge-heading collection-task-heading">
          <div>
            <div className="page-title-line">
              <div className="page-title-icon"><QrcodeOutlined /></div>
              <div>
                <Typography.Title level={1}>信息采集</Typography.Title>
                <Typography.Text type="secondary">按考试场次查看现场信息采集任务，并进入 PC 采集管理台</Typography.Text>
              </div>
            </div>
          </div>
          <Space wrap className="collection-task-toolbar">
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索任务名称 / 编号 / 企业"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              style={{ width: 260 }}
            />
            <Select
              value={statusFilter}
              options={statusOptions}
              onChange={setStatusFilter}
              style={{ width: 128 }}
            />
            <RangePicker
              allowClear
              onChange={(_, dateStrings) => {
                const [start, end] = dateStrings;
                setDateRange(start && end ? [start, end] : null);
              }}
            />
            <Button icon={<ReloadOutlined />} loading={tasksQuery.isFetching} onClick={() => tasksQuery.refetch()}>
              刷新
            </Button>
          </Space>
        </div>

        <Table<CollectionTaskVO>
          className="collection-task-table"
          rowKey={(task) => String(task.examSessionId || task.taskId)}
          columns={visibleColumns}
          dataSource={filteredTasks}
          loading={tasksQuery.isFetching}
          pagination={{ defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: (total) => `共 ${total} 个任务` }}
          scroll={{ x: 1200 }}
          locale={{ emptyText: <Empty description="暂无可采集任务" /> }}
          onRow={(task) => ({
            onClick: () => setSelectedTask(task),
          })}
        />

        <Drawer
          title="任务详情"
          open={Boolean(drawerTask)}
          width={520}
          destroyOnHidden
          onClose={() => setSelectedTask(null)}
        >
          {drawerTask ? (
            <Space direction="vertical" size={18} className="collection-task-drawer">
              <div>
                <Typography.Title level={4}>{drawerTask.taskName}</Typography.Title>
                <Space wrap>
                  {statusTag(drawerTask)}
                  <Typography.Text type="secondary">{drawerTask.taskNo || '-'}</Typography.Text>
                </Space>
              </div>

              <Descriptions title="任务基础信息" column={1} bordered size="small">
                <Descriptions.Item label="任务名称">{drawerTask.taskName || '-'}</Descriptions.Item>
                <Descriptions.Item label="场次名称">{drawerTask.sessionName || '-'}</Descriptions.Item>
                <Descriptions.Item label="任务编号">{drawerTask.taskNo || '-'}</Descriptions.Item>
                <Descriptions.Item label="企业">{drawerTask.projectName || '-'}</Descriptions.Item>
                <Descriptions.Item label="工种/等级">
                  {drawerTask.workTypeLevel || `${drawerTask.workType || '-'} / ${drawerTask.level || '-'}`}
                </Descriptions.Item>
                <Descriptions.Item label="认定日期">{drawerTask.examDate || '-'}</Descriptions.Item>
              </Descriptions>

              <div>
                <Typography.Title level={5}>采集统计</Typography.Title>
                <div className="collection-drawer-stats">
                  <div><span>总人数</span><strong>{toNumber(drawerTask.candidateTotal)}</strong></div>
                  <div><span>已发放</span><strong>{toNumber(drawerTask.qrIssuedCount)}</strong></div>
                  <div><span>已到场</span><strong>{toNumber(drawerTask.arrivedCount)}</strong></div>
                  <div><span>已入队</span><strong>{toNumber(drawerTask.queuedCount)}</strong></div>
                  <div><span>采集中</span><strong>{toNumber(drawerTask.collectingCount)}</strong></div>
                  <div><span>已完成</span><strong>{toNumber(drawerTask.completedCount)}</strong></div>
                  <div><span>待采集</span><strong>{pendingCount(drawerTask)}</strong></div>
                  <div><span>异常</span><strong>{toNumber(drawerTask.exceptionCount)}</strong></div>
                </div>
              </div>

              <Alert
                type="info"
                showIcon
                message="平板端入口信息"
                description="平板端现场采集页用于现场扫码、指纹采集和电子签名。PC 首页不直接进入平板端页面，请从 PC 管理台查看或生成平板端入口。"
              />

              <div>
                <Typography.Title level={5}>最近操作 / 提示</Typography.Title>
                <Typography.Paragraph type="secondary">
                  当前首页仅展示任务概况。管理、监控、调度和异常处理将在 PC 采集管理台中继续承接。
                </Typography.Paragraph>
              </div>

              <div>
                <Typography.Title level={5}>快捷操作</Typography.Title>
                <Space wrap>
                  <Button type="primary" icon={<PlayCircleOutlined />} onClick={() => openManagePage(drawerTask)}>
                    进入 PC 采集管理台
                  </Button>
                  <Button
                    loading={initializeMutation.isPending}
                    onClick={() => initializeMutation.mutate(drawerTask)}
                  >
                    初始化采集状态
                  </Button>
                  <Button
                    icon={<QrcodeOutlined />}
                    loading={generateQrMutation.isPending}
                    onClick={() => generateQrMutation.mutate(drawerTask)}
                  >
                    生成二维码
                  </Button>
                  <Button
                    icon={<DownloadOutlined />}
                    loading={exportMutation.isPending}
                    onClick={() => exportMutation.mutate(drawerTask)}
                  >
                    二维码导出
                  </Button>
                  <Button icon={<ExclamationCircleOutlined />} onClick={() => setExceptionTask(drawerTask)}>
                    查看异常记录
                  </Button>
                </Space>
              </div>
            </Space>
          ) : null}
        </Drawer>

        <Modal
          title={exceptionTask ? `${exceptionTask.taskName} · 异常记录` : '异常记录'}
          open={Boolean(exceptionTask)}
          width={920}
          footer={null}
          destroyOnHidden
          onCancel={() => setExceptionTask(null)}
        >
          <CollectionExceptionPage
            taskId={exceptionTask?.taskId}
            examSessionId={exceptionTask?.examSessionId}
          />
        </Modal>
      </div>
    </PermissionGate>
  );
}

import {
  DownloadOutlined,
  ExclamationCircleOutlined,
  PlayCircleOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { Button, Card, Empty, Input, Modal, Space, Statistic, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  exportQrCodes,
  generateQrCodes,
  getCollectionTasks,
  initializeCollection,
} from '../../../api/certificationPracticalCollection';
import { PermissionGate } from '../../../directives/permission';
import type { CollectionTaskVO } from '../../../types/certificationPracticalCollection';
import { CollectionExceptionPage } from './CollectionExceptionPage';
import './CollectionTaskListPage.css';

export function CollectionTaskListPage() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [exceptionTask, setExceptionTask] = useState<CollectionTaskVO | null>(null);

  const tasksQuery = useQuery({
    queryKey: ['practical-collection-tasks', keyword],
    queryFn: () => getCollectionTasks({ page: 1, pageSize: 100, keyword: keyword || undefined }),
  });

  const invalidateTasks = () => queryClient.invalidateQueries({ queryKey: ['practical-collection-tasks'] });

  const initializeMutation = useMutation({
    mutationFn: initializeCollection,
    onSuccess: (result) => {
      message.success(result.message || '采集状态已初始化');
      invalidateTasks();
    },
  });

  const generateQrMutation = useMutation({
    mutationFn: generateQrCodes,
    onSuccess: (result) => {
      message.success(result.message || '二维码 token 已生成');
      invalidateTasks();
    },
  });

  const exportMutation = useMutation({
    mutationFn: exportQrCodes,
    onSuccess: (result) => {
      const messageText = typeof result === 'object' && result && 'message' in result
        ? String((result as { message?: unknown }).message)
        : '二维码导出能力后续完善';
      message.info(messageText);
    },
  });

  const tasks = tasksQuery.data?.items || [];

  return (
    <PermissionGate code="certification.practical.collection.view" fallback={<Empty description="暂无实操采集访问权限" />}>
      <div className="knowledge-page">
        <div className="page-heading knowledge-heading">
          <div>
            <div className="page-title-line">
              <div className="page-title-icon"><QrcodeOutlined /></div>
              <div>
                <Typography.Title level={1}>实操信息采集</Typography.Title>
                <Typography.Text type="secondary">现场前置信息采集、二维码发放、队列和异常处理入口</Typography.Text>
              </div>
            </div>
          </div>
          <Space>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索任务名称 / 编号"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              style={{ width: 260 }}
            />
            <Button icon={<ReloadOutlined />} loading={tasksQuery.isFetching} onClick={() => tasksQuery.refetch()}>
              刷新
            </Button>
          </Space>
        </div>

        <div className="collection-task-grid">
          {tasks.map((task) => (
            <Card
              key={task.taskId}
              className="collection-task-card"
              title={task.taskName}
              extra={<Typography.Text type="secondary">{task.examDate || '-'}</Typography.Text>}
            >
              <Typography.Paragraph type="secondary">
                {task.taskNo || '-'} · {task.projectName || '认定任务'} · {task.workType || '-'} / {task.level || '-'}
              </Typography.Paragraph>
              <div className="collection-task-stats">
                <Statistic title="总人数" value={task.candidateTotal} />
                <Statistic title="已发放" value={task.qrIssuedCount} />
                <Statistic title="已到场" value={task.arrivedCount} />
                <Statistic title="已入队" value={task.queuedCount} />
                <Statistic title="采集中" value={task.collectingCount} />
                <Statistic title="已完成" value={task.completedCount} />
                <Statistic title="待补采" value={task.pendingRetryCount} />
                <Statistic title="异常" value={task.exceptionCount} />
              </div>
              <Space wrap>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={() => window.open(`/practical-collection/workbench/${task.taskId}`, '_blank')}
                >
                  进入采集工作台
                </Button>
                <Button
                  loading={initializeMutation.isPending}
                  onClick={() => initializeMutation.mutate(task.taskId)}
                >
                  初始化采集状态
                </Button>
                <Button
                  icon={<QrcodeOutlined />}
                  loading={generateQrMutation.isPending}
                  onClick={() => generateQrMutation.mutate(task.taskId)}
                >
                  生成二维码
                </Button>
                <Button
                  icon={<DownloadOutlined />}
                  loading={exportMutation.isPending}
                  onClick={() => exportMutation.mutate(task.taskId)}
                >
                  二维码导出
                </Button>
                <Button icon={<ExclamationCircleOutlined />} onClick={() => setExceptionTask(task)}>
                  查看异常记录
                </Button>
              </Space>
            </Card>
          ))}
        </div>
        {!tasks.length && !tasksQuery.isFetching ? <Empty description="暂无可采集任务" /> : null}
        <Modal
          title={exceptionTask ? `${exceptionTask.taskName} · 异常记录` : '异常记录'}
          open={Boolean(exceptionTask)}
          width={920}
          footer={null}
          destroyOnHidden
          onCancel={() => setExceptionTask(null)}
        >
          <CollectionExceptionPage taskId={exceptionTask?.taskId} />
        </Modal>
      </div>
    </PermissionGate>
  );
}

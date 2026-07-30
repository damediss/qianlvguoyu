import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Empty, Input, Modal, Space, Spin, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { fetchCollectionTasks } from '../../../services/practicalCollection/tabletCollectionService';
import type { CollectionTaskVO, SnowflakeId } from '../../../types/certificationPracticalCollection';
import { formatCollectionTaskStatus } from '../../../utils/collectionTask';

interface CollectionTaskSwitchModalProps {
  open: boolean;
  currentTaskId?: SnowflakeId | null;
  forceSelect?: boolean;
  switching?: boolean;
  onSelect: (taskId: SnowflakeId) => void;
  onClose?: () => void;
}

function isSameTask(left?: SnowflakeId | null, right?: SnowflakeId | null) {
  return left !== undefined && left !== null && right !== undefined && right !== null
    && String(left) === String(right);
}

export function CollectionTaskSwitchModal({
  open,
  currentTaskId,
  forceSelect = false,
  switching = false,
  onSelect,
  onClose,
}: CollectionTaskSwitchModalProps) {
  const [keyword, setKeyword] = useState('');

  const tasksQuery = useQuery({
    queryKey: ['tablet-collection-tasks', keyword],
    queryFn: () => fetchCollectionTasks(keyword),
    enabled: open,
  });

  const tasks = useMemo(() => tasksQuery.data || [], [tasksQuery.data]);

  return (
    <Modal
      title="切换采集任务"
      open={open}
      width={760}
      footer={null}
      destroyOnHidden
      closable={!forceSelect}
      maskClosable={!forceSelect}
      onCancel={forceSelect ? undefined : onClose}
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="搜索任务名称 / 编号"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onPressEnter={() => tasksQuery.refetch()}
        />
        <div className="collection-task-switch-toolbar">
          <Typography.Text type="secondary">
            请选择要进入的采集任务，切换后将刷新考生名单与采集状态。
          </Typography.Text>
          <Button
            icon={<ReloadOutlined />}
            loading={tasksQuery.isFetching}
            onClick={() => tasksQuery.refetch()}
          >
            刷新列表
          </Button>
        </div>
        <div className="collection-task-switch-list">
          {tasksQuery.isLoading ? (
            <div className="collection-task-switch-loading"><Spin /></div>
          ) : null}
          {!tasksQuery.isLoading && tasks.length === 0 ? (
            <Empty description="暂无可切换的采集任务" />
          ) : null}
          {tasks.map((task) => {
            const status = formatCollectionTaskStatus(task);
            const isCurrent = isSameTask(task.taskId, currentTaskId);
            return (
              <button
                key={String(task.taskId)}
                type="button"
                className={`collection-task-switch-item${isCurrent ? ' is-current' : ''}`}
                disabled={switching}
                onClick={() => onSelect(task.taskId)}
              >
                <div className="collection-task-switch-item-main">
                  <div className="collection-task-switch-item-title">
                    <strong>{task.taskName}</strong>
                    {isCurrent ? <Tag color="blue">当前任务</Tag> : null}
                    <Tag color={status.color}>{status.label}</Tag>
                  </div>
                  <div className="collection-task-switch-item-meta">
                    <span>任务编号：{task.taskNo || '-'}</span>
                    <span>日期：{task.examDate || '-'}</span>
                    <span>考生：{task.candidateTotal} 人</span>
                    <span>已完成：{task.completedCount} 人</span>
                  </div>
                  {(task.workTypeLevel || task.workType || task.level) ? (
                    <div className="collection-task-switch-item-extra">
                      {task.workTypeLevel || [task.workType, task.level].filter(Boolean).join(' · ')}
                    </div>
                  ) : null}
                </div>
              </button>
            );
          })}
        </div>
      </Space>
    </Modal>
  );
}

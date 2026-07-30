import { SwapOutlined } from '@ant-design/icons';
import { Button, Typography } from 'antd';
import type { CollectionTaskVO } from '../../../types/certificationPracticalCollection';

interface CollectionTaskSelectorBarProps {
  task?: CollectionTaskVO | null;
  loading?: boolean;
  onSwitchClick: () => void;
}

export function CollectionTaskSelectorBar({
  task,
  loading,
  onSwitchClick,
}: CollectionTaskSelectorBarProps) {
  return (
    <div
      className="collection-task-selector-bar"
      role="button"
      tabIndex={0}
      onClick={onSwitchClick}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onSwitchClick();
        }
      }}
    >
      <Typography.Text className="collection-task-selector-kicker" type="secondary">
        平板信息采集端
      </Typography.Text>
      <Typography.Title level={4} className="collection-task-selector-name">
        当前任务：{task?.taskName || (loading ? '加载中…' : '未选择任务')}
      </Typography.Title>
      <div className="collection-task-selector-meta">
        <span>任务编号：{task?.taskNo || '-'}</span>
        <span>日期：{task?.examDate || '-'}</span>
        {task ? <span>考生：{task.candidateTotal} 人</span> : null}
      </div>
      <Button
        className="collection-task-switch-button"
        icon={<SwapOutlined />}
        loading={loading}
        onClick={(event) => {
          event.stopPropagation();
          onSwitchClick();
        }}
      >
        切换任务
      </Button>
    </div>
  );
}

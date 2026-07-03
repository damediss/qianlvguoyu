import { ClockCircleOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { Button, Empty, List, Popconfirm, Space, Tag, Typography } from 'antd';
import type { QueueItemVO } from '../../../types/certificationPracticalCollection';

interface CollectionQueuePanelProps {
  queue: QueueItemVO[];
  loading?: boolean;
  onStartNext: () => void;
  onRemove: (queue: QueueItemVO) => void;
  onViewDetail: (queue: QueueItemVO) => void;
}

export function CollectionQueuePanel({
  queue,
  loading,
  onStartNext,
  onRemove,
  onViewDetail,
}: CollectionQueuePanelProps) {
  return (
    <div className="collection-panel queue-panel">
      <div className="collection-panel-header">
        <Typography.Title level={4}>现场动态队列</Typography.Title>
        <Button type="primary" icon={<PlayCircleOutlined />} loading={loading} onClick={onStartNext}>
          开始下一位
        </Button>
      </div>
      <List
        dataSource={queue}
        locale={{ emptyText: <Empty description="当前暂无排队考生" /> }}
        renderItem={(item) => (
          <List.Item className="touch-list-item queue-item">
            <div className="queue-number">#{item.queueNo}</div>
            <div className="touch-list-main">
              <strong>{item.name || '-'}</strong>
              <span>后四位 {item.idCardLast4 || '-'}</span>
              <span><ClockCircleOutlined /> {item.joinedAt ? new Date(item.joinedAt).toLocaleTimeString() : '-'}</span>
            </div>
            <Space wrap>
              <Tag color={item.queueStatus === 'COLLECTING' ? 'processing' : 'default'}>
                {item.queueStatusLabel || item.queueStatus}
              </Tag>
              <Button onClick={() => onViewDetail(item)}>详情</Button>
              <Popconfirm
                title="确认移出队列？"
                okText="移出"
                cancelText="取消"
                onConfirm={() => onRemove(item)}
              >
                <Button danger disabled={item.queueStatus === 'COLLECTING'}>移出</Button>
              </Popconfirm>
            </Space>
          </List.Item>
        )}
      />
    </div>
  );
}

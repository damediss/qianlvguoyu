import { CloudOutlined, SafetyCertificateOutlined, WifiOutlined } from '@ant-design/icons';
import { Button, Space, Tag, Typography } from 'antd';
import type { FingerprintDeviceStatus } from '../../../services/fingerprint/FingerprintAdapter';
import type { CollectionStatsVO, CollectionTaskVO } from '../../../types/certificationPracticalCollection';

interface CollectionStatsBarProps {
  task?: CollectionTaskVO;
  stats?: CollectionStatsVO;
  deviceStatus?: FingerprintDeviceStatus;
  online: boolean;
  onCheckDevice?: () => void;
  checkingDevice?: boolean;
}

export function CollectionStatsBar({
  task,
  stats,
  deviceStatus,
  online,
  onCheckDevice,
  checkingDevice,
}: CollectionStatsBarProps) {
  const items = [
    ['总人数', stats?.candidateTotal ?? task?.candidateTotal ?? 0],
    ['已到场', stats?.arrivedCount ?? task?.arrivedCount ?? 0],
    ['已入队', stats?.queuedCount ?? task?.queuedCount ?? 0],
    ['采集中', stats?.collectingCount ?? task?.collectingCount ?? 0],
    ['已完成', stats?.completedCount ?? task?.completedCount ?? 0],
    ['待补采', stats?.pendingRetryCount ?? task?.pendingRetryCount ?? 0],
    ['异常', stats?.exceptionCount ?? task?.exceptionCount ?? 0],
  ] as const;

  const deviceColor = deviceStatus?.connected ? 'success' : deviceStatus?.status === 'ERROR' ? 'error' : 'warning';

  return (
    <div className="collection-stats-bar">
      <div className="collection-task-title">
        <Typography.Title level={3}>{task?.taskName || '实操信息采集工作台'}</Typography.Title>
        <Typography.Text>{task?.taskNo || '-'} · {task?.projectName || '认定任务'} · {task?.examDate || '-'}</Typography.Text>
      </div>
      <div className="collection-stat-strip">
        {items.map(([label, value]) => (
          <div className="collection-stat-cell" key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
      <Space className="collection-device-strip" size={8} wrap>
        <Tag icon={<WifiOutlined />} color={online ? 'success' : 'error'}>
          {online ? '网络在线' : '网络离线'}
        </Tag>
        <Tag icon={<SafetyCertificateOutlined />} color={deviceColor}>
          {deviceStatus?.message || '指纹设备未检测'}
        </Tag>
        <Button icon={<CloudOutlined />} loading={checkingDevice} onClick={onCheckDevice}>
          检测设备
        </Button>
      </Space>
    </div>
  );
}

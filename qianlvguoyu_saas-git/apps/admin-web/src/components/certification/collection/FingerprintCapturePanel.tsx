import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Button, Space, Tag, Typography } from 'antd';
import type { FingerprintDeviceStatus } from '../../../services/fingerprint/FingerprintAdapter';

interface FingerprintCapturePanelProps {
  status?: FingerprintDeviceStatus;
  loading?: boolean;
  onCheckDevice: () => void;
}

export function FingerprintCapturePanel({ status, loading, onCheckDevice }: FingerprintCapturePanelProps) {
  return (
    <div className="fingerprint-capture-panel">
      <Space direction="vertical" size={8}>
        <Typography.Text strong>指纹设备</Typography.Text>
        <Tag color={status?.connected ? 'success' : 'warning'}>
          {status?.message || '未检测'}
        </Tag>
      </Space>
      <Button icon={<SafetyCertificateOutlined />} loading={loading} onClick={onCheckDevice}>
        检测指纹设备
      </Button>
    </div>
  );
}

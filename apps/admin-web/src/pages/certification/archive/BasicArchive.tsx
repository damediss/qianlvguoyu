import { Descriptions, Empty, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getArchiveBasic } from '../../../api/certificationArchive';
import {
  dateTimeText,
  formatDeviceSnapshot,
  formatEvaluatorSnapshot,
  formatExamStaffSnapshot,
  formatRoomSnapshot,
  formatScopeSnapshot,
} from './shared';

export function BasicArchive({ archiveId }: { archiveId?: string }) {
  const query = useQuery({
    queryKey: ['certification-archive-basic', archiveId],
    queryFn: () => getArchiveBasic(archiveId!),
    enabled: !!archiveId,
  });

  if (!archiveId) {
    return <Empty description="请选择已创建归档记录的任务" />;
  }
  if (!query.data) {
    return <Empty description={query.isLoading ? '加载中' : '尚未生成基础信息快照'} />;
  }

  const snapshot = query.data;
  return (
    <Descriptions bordered column={2} size="small">
      <Descriptions.Item label="归档时间">{dateTimeText(snapshot.createdAt)}</Descriptions.Item>
      <Descriptions.Item label="认定完成时间">{dateTimeText(snapshot.completedAt)}</Descriptions.Item>
      <Descriptions.Item label="认定人数">{snapshot.candidateCount}</Descriptions.Item>
      <Descriptions.Item label="快照记录">{snapshot.id}</Descriptions.Item>
      <Descriptions.Item label="认定范围" span={2}>
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{formatScopeSnapshot(snapshot.scopeJson)}</Typography.Paragraph>
      </Descriptions.Item>
      <Descriptions.Item label="考务配置" span={2}>
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{formatExamStaffSnapshot(snapshot.examStaffJson)}</Typography.Paragraph>
      </Descriptions.Item>
      <Descriptions.Item label="设备配置" span={2}>
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{formatDeviceSnapshot(snapshot.deviceJson)}</Typography.Paragraph>
      </Descriptions.Item>
      <Descriptions.Item label="考场配置" span={2}>
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{formatRoomSnapshot(snapshot.roomJson)}</Typography.Paragraph>
      </Descriptions.Item>
      <Descriptions.Item label="考评员配置" span={2}>
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{formatEvaluatorSnapshot(snapshot.evaluatorJson)}</Typography.Paragraph>
      </Descriptions.Item>
    </Descriptions>
  );
}

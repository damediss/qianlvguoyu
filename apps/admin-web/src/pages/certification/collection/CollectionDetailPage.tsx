import { Descriptions, Empty, List, Space, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getCollectionDetail } from '../../../api/certificationPracticalCollection';
import type { SnowflakeId } from '../../../types/certificationPracticalCollection';

interface CollectionDetailPageProps {
  taskId?: SnowflakeId;
  candidateId?: SnowflakeId;
}

export function CollectionDetailPage({ taskId, candidateId }: CollectionDetailPageProps) {
  const detailQuery = useQuery({
    queryKey: ['practical-collection-detail', taskId, candidateId],
    queryFn: () => getCollectionDetail(taskId!, candidateId!),
    enabled: Boolean(taskId && candidateId),
  });

  const detail = detailQuery.data;
  if (!taskId || !candidateId) {
    return <Empty description="请选择考生查看详情" />;
  }

  if (!detail) {
    return <Empty description={detailQuery.isFetching ? '正在加载详情' : '暂无详情'} />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <div>
        <Typography.Title level={4}>{detail.name}</Typography.Title>
        <Typography.Text type="secondary">身份证后四位：{detail.idCardLast4 || '-'}</Typography.Text>
      </div>
      <Descriptions bordered column={2} size="small">
        <Descriptions.Item label="单位">{detail.unitName || '-'}</Descriptions.Item>
        <Descriptions.Item label="工种">{detail.workType || '-'}</Descriptions.Item>
        <Descriptions.Item label="等级">{detail.level || '-'}</Descriptions.Item>
        <Descriptions.Item label="准考证号">{detail.admissionNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="整体状态">{detail.overallStatusLabel || detail.overallStatus}</Descriptions.Item>
        <Descriptions.Item label="队列状态">{detail.queueStatusLabel || detail.queueStatus}</Descriptions.Item>
        <Descriptions.Item label="指纹状态">{detail.fingerprintStatusLabel || detail.fingerprintStatus}</Descriptions.Item>
        <Descriptions.Item label="签名状态">{detail.signatureStatusLabel || detail.signatureStatus}</Descriptions.Item>
      </Descriptions>
      <Descriptions bordered column={1} size="small" title="最新采集记录">
        <Descriptions.Item label="最新指纹">
          {detail.latestFingerprint
            ? `${detail.latestFingerprint.collectStatusLabel || detail.latestFingerprint.collectStatus} · ${detail.latestFingerprint.collectedAt}`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="最新签名">
          {detail.latestSignature
            ? `${detail.latestSignature.signerName} · ${detail.latestSignature.signedAt}`
            : '-'}
        </Descriptions.Item>
      </Descriptions>
      <List
        header={<strong>异常记录</strong>}
        dataSource={detail.exceptions || []}
        locale={{ emptyText: <Empty description="暂无异常记录" /> }}
        renderItem={(item) => (
          <List.Item>
            <Space wrap>
              <Tag color="red">{item.exceptionTypeLabel || item.exceptionType}</Tag>
              <span>{item.reason}</span>
              <span>{item.handledAt || item.createdAt || '-'}</span>
            </Space>
          </List.Item>
        )}
      />
    </Space>
  );
}

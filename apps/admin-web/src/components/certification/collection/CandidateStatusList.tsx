import { Badge, Empty, List, Tag, Typography } from 'antd';
import type { CandidateCollectionItemVO, CollectionOverallStatus } from '../../../types/certificationPracticalCollection';
import { CollectionOverallStatusText } from '../../../types/certificationPracticalCollection';

interface CandidateStatusListProps {
  candidates: CandidateCollectionItemVO[];
  onSelect: (candidate: CandidateCollectionItemVO) => void;
}

const groupOrder: CollectionOverallStatus[] = [
  'COLLECTING',
  'QUEUED',
  'ARRIVED',
  'PENDING_RETRY',
  'INFO_ERROR',
  'EXCEPTION_RELEASED',
  'COMPLETED',
  'NOT_ARRIVED',
  'ISSUED',
  'NOT_ISSUED',
  'CANCELLED',
];

function colorFor(status: CollectionOverallStatus) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'COLLECTING' || status === 'EXCEPTION_RELEASED') return 'processing';
  if (status === 'PENDING_RETRY') return 'warning';
  if (status === 'INFO_ERROR') return 'error';
  return 'default';
}

export function CandidateStatusList({ candidates, onSelect }: CandidateStatusListProps) {
  const sorted = [...candidates].sort((left, right) => (
    groupOrder.indexOf(left.overallStatus) - groupOrder.indexOf(right.overallStatus)
  ));

  return (
    <div className="collection-panel candidate-status-list">
      <div className="collection-panel-header">
        <Typography.Title level={4}>考生状态</Typography.Title>
        <Badge count={candidates.length} color="#1677ff" />
      </div>
      <List
        dataSource={sorted}
        locale={{ emptyText: <Empty description="暂无考生" /> }}
        renderItem={(candidate) => (
          <List.Item className="touch-list-item" onClick={() => onSelect(candidate)}>
            <div className="touch-list-main">
              <strong>{candidate.name}</strong>
              <span>后四位 {candidate.idCardLast4 || '-'}</span>
              <span>{candidate.unitName || '-'}</span>
            </div>
            <Tag color={colorFor(candidate.overallStatus)}>
              {candidate.overallStatusLabel || CollectionOverallStatusText[candidate.overallStatus]}
            </Tag>
          </List.Item>
        )}
      />
    </div>
  );
}

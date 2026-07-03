import { ExclamationCircleOutlined, IdcardOutlined } from '@ant-design/icons';
import { Button, Descriptions, Modal, Space, Tag, Typography } from 'antd';
import type { CandidateMatchVO, CollectionMatchMethod } from '../../../types/certificationPracticalCollection';
import { CollectionMatchMethodText, CollectionOverallStatusText } from '../../../types/certificationPracticalCollection';

interface CandidateConfirmCardProps {
  open: boolean;
  candidate?: CandidateMatchVO | null;
  loading?: boolean;
  onCancel: () => void;
  onJoinQueue: (candidate: CandidateMatchVO) => void;
  onCollectNow: (candidate: CandidateMatchVO) => void;
  onViewDetail: (candidate: CandidateMatchVO) => void;
  onInfoError: (candidate: CandidateMatchVO) => void;
}

function statusText(candidate: CandidateMatchVO) {
  return candidate.currentStatusLabel
    || CollectionOverallStatusText[candidate.currentStatus]
    || candidate.currentStatus;
}

export function CandidateConfirmCard({
  open,
  candidate,
  loading,
  onCancel,
  onJoinQueue,
  onCollectNow,
  onViewDetail,
  onInfoError,
}: CandidateConfirmCardProps) {
  const disabled = !candidate || candidate.currentStatus === 'COMPLETED' || candidate.currentStatus === 'INFO_ERROR';
  const matchMethod = candidate?.matchMethod as CollectionMatchMethod | undefined;

  return (
    <Modal
      title="考生身份确认"
      open={open}
      onCancel={onCancel}
      footer={null}
      width={680}
      destroyOnHidden
    >
      {candidate ? (
        <div className="candidate-confirm-card">
          <div className="candidate-confirm-header">
            <div className="candidate-avatar"><IdcardOutlined /></div>
            <div>
              <Typography.Title level={3}>{candidate.name}</Typography.Title>
              <Space wrap>
                <Tag color="blue">状态：{statusText(candidate)}</Tag>
                {candidate.alreadyQueued ? <Tag color="processing">队列号 {candidate.queueNo || '-'}</Tag> : <Tag>未入队</Tag>}
                {matchMethod ? <Tag>{CollectionMatchMethodText[matchMethod] || matchMethod}</Tag> : null}
              </Space>
            </div>
          </div>
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="身份证后四位">{candidate.idCardLast4 || '-'}</Descriptions.Item>
            <Descriptions.Item label="脱敏证件号">{candidate.maskedIdCardNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="单位">{candidate.unitName || '-'}</Descriptions.Item>
            <Descriptions.Item label="工种">{candidate.workType || '-'}</Descriptions.Item>
            <Descriptions.Item label="等级">{candidate.level || '-'}</Descriptions.Item>
            <Descriptions.Item label="准考证号">{candidate.admissionNo || '-'}</Descriptions.Item>
          </Descriptions>
          {candidate.currentStatus === 'COMPLETED' ? (
            <div className="candidate-confirm-warning">
              <ExclamationCircleOutlined /> 已完成考生不能重复入队或重新采集。
            </div>
          ) : null}
          <Space className="candidate-confirm-actions" wrap>
            <Button
              type="primary"
              loading={loading}
              disabled={disabled || candidate.alreadyQueued}
              onClick={() => onJoinQueue(candidate)}
            >
              确认并加入队列
            </Button>
            <Button
              loading={loading}
              disabled={disabled}
              onClick={() => onCollectNow(candidate)}
            >
              确认并立即采集
            </Button>
            <Button onClick={() => onViewDetail(candidate)}>查看详情</Button>
            <Button danger onClick={() => onInfoError(candidate)}>
              不是本人 / 信息有误
            </Button>
          </Space>
        </div>
      ) : null}
    </Modal>
  );
}

import { CheckCircleOutlined, SafetyCertificateOutlined, WarningOutlined } from '@ant-design/icons';
import { Button, Descriptions, Empty, Space, Tag, Typography, message } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { submitFingerprint } from '../../../api/certificationPracticalCollection';
import { getFingerprintAdapter } from '../../../services/fingerprint/FingerprintAdapterFactory';
import { isFingerprintCaptureCancelled } from '../../../services/fingerprint/AndroidFingerprintAdapter';
import { formatBackendDateTime } from '../../../utils/dateTime';
import { SignaturePad } from './SignaturePad';
import type {
  CandidateCollectionDetailVO,
  CandidateMatchVO,
  SnowflakeId,
} from '../../../types/certificationPracticalCollection';

interface CurrentCandidateCollectionPanelProps {
  candidate?: CandidateCollectionDetailVO | null;
  taskId: SnowflakeId;
  loading?: boolean;
  onRefresh: () => void;
  onComplete: () => void;
  onPendingRetry: () => void;
  onInfoError: () => void;
  onExceptionRelease: () => void;
}

function asMatchCandidate(candidate: CandidateCollectionDetailVO): CandidateMatchVO {
  return {
    candidateId: candidate.candidateId,
    taskId: candidate.taskId,
    name: candidate.name,
    idCardLast4: candidate.idCardLast4,
    maskedIdCardNo: candidate.maskedIdCardNo,
    unitName: candidate.unitName,
    workType: candidate.workType,
    level: candidate.level,
    admissionNo: candidate.admissionNo,
    avatarFileId: candidate.avatarFileId,
    currentStatus: candidate.overallStatus,
    currentStatusLabel: candidate.overallStatusLabel,
    alreadyQueued: Boolean(candidate.currentQueue),
    queueNo: candidate.queueNo,
  };
}

function getSignatureDisabledReason(candidate: CandidateCollectionDetailVO): string | undefined {
  if (candidate.signatureStatus === 'EXCEPTION_RELEASED') {
    return '签名已异常放行';
  }
  if (candidate.signatureStatus === 'DONE' || candidate.latestSignature) {
    return '签名已提交，本阶段不支持重复签名覆盖';
  }
  if (candidate.overallStatus === 'COMPLETED') {
    return '考生采集已完成，不允许重复签名';
  }
  if (candidate.overallStatus !== 'COLLECTING') {
    return '只有采集中考生可以签名';
  }
  return undefined;
}

export function CurrentCandidateCollectionPanel({
  candidate,
  taskId,
  loading,
  onRefresh,
  onComplete,
  onPendingRetry,
  onInfoError,
  onExceptionRelease,
}: CurrentCandidateCollectionPanelProps) {
  const fingerprintMutation = useMutation({
    mutationFn: async (detail: CandidateCollectionDetailVO) => {
      const adapter = getFingerprintAdapter();
      const result = await adapter.startCapture(asMatchCandidate(detail));
      if (!result.success) {
        throw new Error(result.message || '指纹采集失败，请重试');
      }
      return submitFingerprint({
        taskId,
        candidateId: detail.candidateId,
        sessionId: detail.sessionId,
        deviceSn: result.deviceSn,
        deviceModel: result.deviceModel,
        deviceVendorId: result.deviceVendorId,
        deviceProductId: result.deviceProductId,
        sdkDeviceType: result.sdkDeviceType,
        adapterType: result.adapterType,
        collectStatus: 'SUCCESS',
        qualityScore: result.qualityScore,
        durationMs: result.durationMs,
        errorCode: undefined,
        exceptionReason: undefined,
        collectedAt: formatBackendDateTime(result.collectedAt),
        fingerprintImageFileId: result.fingerprintImageFileId,
        fingerprintImageHash: result.fingerprintImageHash,
        fingerprintImageFormat: result.fingerprintImageFormat,
        fingerprintImageCapturedAt: result.fingerprintImageFileId
          ? formatBackendDateTime(result.fingerprintImageCapturedAt || result.collectedAt)
          : undefined,
        requestId: result.requestId,
      });
    },
    onSuccess: () => {
      message.success('指纹采集记录已提交');
      onRefresh();
    },
    onError: (error) => {
      if (isFingerprintCaptureCancelled(error)) {
        message.info('已取消指纹采集');
        return;
      }
      message.error(error instanceof Error ? error.message : '指纹采集失败，请检查设备后重试');
    },
  });

  if (!candidate) {
    return (
      <div className="collection-panel current-candidate-panel empty-current-candidate">
        <Empty description="暂无当前采集考生，请从队列开始下一位" />
      </div>
    );
  }

  const canOperate = candidate.overallStatus === 'COLLECTING';
  const signatureDisabledReason = getSignatureDisabledReason(candidate);

  return (
    <div className="collection-panel current-candidate-panel">
      <div className="current-candidate-hero">
        <div>
          <Typography.Text type="secondary">当前采集对象</Typography.Text>
          <Typography.Title level={2}>{candidate.name}</Typography.Title>
          <Space wrap>
            <Tag color="processing">{candidate.overallStatusLabel || candidate.overallStatus}</Tag>
            <Tag>队列号 {candidate.queueNo || '-'}</Tag>
            <Tag>后四位 {candidate.idCardLast4 || '-'}</Tag>
          </Space>
        </div>
      </div>

      <Descriptions className="current-candidate-desc" column={2} size="small" bordered>
        <Descriptions.Item label="单位">{candidate.unitName || '-'}</Descriptions.Item>
        <Descriptions.Item label="工种">{candidate.workType || '-'}</Descriptions.Item>
        <Descriptions.Item label="等级">{candidate.level || '-'}</Descriptions.Item>
        <Descriptions.Item label="准考证号">{candidate.admissionNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="指纹状态">{candidate.fingerprintStatusLabel || candidate.fingerprintStatus}</Descriptions.Item>
        <Descriptions.Item label="签名状态">{candidate.signatureStatusLabel || candidate.signatureStatus}</Descriptions.Item>
      </Descriptions>

      <SignaturePad
        taskId={taskId}
        candidate={candidate}
        disabled={Boolean(signatureDisabledReason)}
        disabledReason={signatureDisabledReason}
        onSubmitted={onRefresh}
      />

      <div className="current-candidate-actions">
        <Button
          icon={<SafetyCertificateOutlined />}
          loading={fingerprintMutation.isPending}
          disabled={!canOperate}
          onClick={() => fingerprintMutation.mutate(candidate)}
        >
          采集指纹
        </Button>
        <Button
          type="primary"
          icon={<CheckCircleOutlined />}
          loading={loading}
          disabled={!canOperate}
          onClick={onComplete}
        >
          完成采集
        </Button>
        <Button loading={loading} disabled={!canOperate} onClick={onPendingRetry}>
          待补采
        </Button>
        <Button loading={loading} disabled={!canOperate} onClick={onInfoError}>
          信息异常
        </Button>
        <Button danger icon={<WarningOutlined />} loading={loading} disabled={!canOperate} onClick={onExceptionRelease}>
          异常放行
        </Button>
      </div>
    </div>
  );
}

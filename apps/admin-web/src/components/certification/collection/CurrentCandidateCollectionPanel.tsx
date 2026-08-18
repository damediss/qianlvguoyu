import {
  CheckCircleOutlined,
  CloseOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { Button, Descriptions, Empty, Input, Modal, Space, Typography, message } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { completeCollection, markInfoError, submitFingerprint } from '../../../api/certificationPracticalCollection';
import { getFingerprintAdapter } from '../../../services/fingerprint/FingerprintAdapterFactory';
import { isFingerprintCaptureCancelled } from '../../../services/fingerprint/AndroidFingerprintAdapter';
import { formatBackendDateTime } from '../../../utils/dateTime';
import { SignaturePad } from './SignaturePad';
import type {
  CandidateCollectionContext,
  CandidateCollectionDetailVO,
  CandidateMatchVO,
  SnowflakeId,
} from '../../../types/certificationPracticalCollection';

interface CurrentCandidateCollectionPanelProps {
  candidate?: CandidateCollectionDetailVO | null;
  collectionContext?: CandidateCollectionContext | null;
  taskId: SnowflakeId;
  onRefresh: () => void;
  onCancelCurrent: () => void;
  onClearCurrent?: () => void;
  onAfterStatusChanged?: () => void;
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
    return '签名状态异常';
  }
  if (candidate.signatureStatus === 'DONE' || candidate.latestSignature) {
    return '签名已提交，本阶段不支持重复签名覆盖';
  }
  if (candidate.overallStatus === 'COMPLETED') {
    return '考生采集已完成，不允许重复签名';
  }
  if (candidate.overallStatus !== 'COLLECTING') {
    return '请先确认当前考生';
  }
  return undefined;
}

function getFingerprintStatusText(candidate: CandidateCollectionDetailVO) {
  if (candidate.latestFingerprint?.fingerprintImageFileId || candidate.fingerprintStatus === 'DONE') {
    return '已采集';
  }
  if (candidate.fingerprintStatus === 'FAILED' || candidate.fingerprintStatus === 'EXCEPTION_RELEASED') {
    return '异常';
  }
  return '未完成';
}

function getSignatureStatusText(candidate: CandidateCollectionDetailVO) {
  if (candidate.latestSignature?.signatureFileId || candidate.signatureStatus === 'DONE') {
    return '已签名';
  }
  if (candidate.signatureStatus === 'FAILED' || candidate.signatureStatus === 'EXCEPTION_RELEASED') {
    return '异常';
  }
  return '未完成';
}

export function CurrentCandidateCollectionPanel({
  candidate,
  taskId,
  onRefresh,
  onCancelCurrent,
  onClearCurrent,
  onAfterStatusChanged,
}: CurrentCandidateCollectionPanelProps) {
  const [infoErrorOpen, setInfoErrorOpen] = useState(false);
  const [infoErrorReason, setInfoErrorReason] = useState('');

  const clearCurrentAfterSave = () => {
    if (onClearCurrent) {
      onClearCurrent();
      return;
    }
    onCancelCurrent();
  };

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

  const completeMutation = useMutation({
    mutationFn: () => {
      if (!candidate) {
        throw new Error('请先确认当前考生');
      }
      return completeCollection({
        taskId,
        candidateId: candidate.candidateId,
        sessionId: candidate.sessionId,
      });
    },
    onSuccess: (result) => {
      message.success(result.message || '采集已完成');
      clearCurrentAfterSave();
      onAfterStatusChanged?.();
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '提交失败，请稍后重试');
    },
  });

  const infoErrorMutation = useMutation({
    mutationFn: (reason: string) => {
      if (!candidate) {
        throw new Error('请先确认当前考生');
      }
      return markInfoError({
        taskId,
        candidateId: candidate.candidateId,
        reason,
      });
    },
    onSuccess: () => {
      message.success('已标记异常');
      setInfoErrorOpen(false);
      setInfoErrorReason('');
      clearCurrentAfterSave();
      onAfterStatusChanged?.();
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '标记异常失败，请稍后重试');
    },
  });

  if (!candidate) {
    return (
      <div className="collection-panel current-candidate-panel empty-current-candidate">
        <Empty
          description={(
            <Space direction="vertical" size={4}>
              <Typography.Text>等待确认考生</Typography.Text>
              <Typography.Text type="secondary">请扫码或从考生列表选择后开始采集</Typography.Text>
            </Space>
          )}
        />
      </div>
    );
  }

  const canOperate = candidate.overallStatus === 'COLLECTING';
  const signatureDisabledReason = getSignatureDisabledReason(candidate);
  const fingerprintButtonText = candidate.latestFingerprint?.fingerprintImageFileId ? '重新采集指纹' : '指纹采集';
  const panelLoading = completeMutation.isPending || infoErrorMutation.isPending;

  const handleComplete = () => {
    if (!candidate) {
      message.warning('请先确认当前考生');
      return;
    }
    if (!candidate.latestFingerprint?.fingerprintImageFileId || !candidate.latestSignature?.signatureFileId) {
      message.warning('请先完成指纹采集和电子签名');
      return;
    }
    completeMutation.mutate();
  };

  const handleSubmitInfoError = () => {
    const reason = infoErrorReason.trim();
    if (!reason) {
      message.warning('请填写异常原因');
      return;
    }
    infoErrorMutation.mutate(reason);
  };

  return (
    <div className="collection-panel current-candidate-panel">
      <div className="current-candidate-hero">
        <Typography.Text type="secondary">当前采集对象</Typography.Text>
        <Typography.Title level={2}>{candidate.name}</Typography.Title>
      </div>

      <Descriptions className="current-candidate-desc" column={2} size="small" bordered>
        <Descriptions.Item label="身份证号">{candidate.maskedIdCardNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="准考证号">{candidate.admissionNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="指纹状态">{getFingerprintStatusText(candidate)}</Descriptions.Item>
        <Descriptions.Item label="电子签名">{getSignatureStatusText(candidate)}</Descriptions.Item>
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
          {fingerprintButtonText}
        </Button>
        <Button
          type="primary"
          icon={<CheckCircleOutlined />}
          loading={completeMutation.isPending}
          disabled={!canOperate}
          onClick={handleComplete}
        >
          提交完成并清空当前考生
        </Button>
        <Button
          danger
          icon={<WarningOutlined />}
          loading={panelLoading}
          disabled={!canOperate}
          onClick={() => {
            setInfoErrorReason('');
            setInfoErrorOpen(true);
          }}
        >
          标记异常
        </Button>
        <Button icon={<CloseOutlined />} onClick={onCancelCurrent}>
          取消当前
        </Button>
      </div>

      <Modal
        title="标记异常"
        open={infoErrorOpen}
        okText="确认异常"
        cancelText="取消"
        confirmLoading={infoErrorMutation.isPending}
        okButtonProps={{ danger: true }}
        onCancel={() => {
          setInfoErrorOpen(false);
          setInfoErrorReason('');
        }}
        onOk={handleSubmitInfoError}
        destroyOnHidden
      >
        <Input.TextArea
          rows={4}
          maxLength={1000}
          showCount
          value={infoErrorReason}
          placeholder="请输入异常原因"
          onChange={(event) => setInfoErrorReason(event.target.value)}
        />
      </Modal>
    </div>
  );
}

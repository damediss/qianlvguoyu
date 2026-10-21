import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CheckCircleOutlined, CloseOutlined, SafetyCertificateOutlined, WarningOutlined, } from '@ant-design/icons';
import { Button, Descriptions, Empty, Input, Modal, Space, Typography, message } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { completeCollection, markInfoError, submitFingerprint } from '../../../api/certificationPracticalCollection';
import { getFingerprintAdapter } from '../../../services/fingerprint/FingerprintAdapterFactory';
import { isFingerprintCaptureCancelled } from '../../../services/fingerprint/AndroidFingerprintAdapter';
import { formatBackendDateTime } from '../../../utils/dateTime';
import { SignaturePad } from './SignaturePad';
function asMatchCandidate(candidate) {
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
function getSignatureDisabledReason(candidate) {
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
function getFingerprintStatusText(candidate) {
    if (candidate.latestFingerprint?.fingerprintImageFileId || candidate.fingerprintStatus === 'DONE') {
        return '已采集';
    }
    if (candidate.fingerprintStatus === 'FAILED' || candidate.fingerprintStatus === 'EXCEPTION_RELEASED') {
        return '异常';
    }
    return '未完成';
}
function getSignatureStatusText(candidate) {
    if (candidate.latestSignature?.signatureFileId || candidate.signatureStatus === 'DONE') {
        return '已签名';
    }
    if (candidate.signatureStatus === 'FAILED' || candidate.signatureStatus === 'EXCEPTION_RELEASED') {
        return '异常';
    }
    return '未完成';
}
export function CurrentCandidateCollectionPanel({ candidate, taskId, onRefresh, onCancelCurrent, onClearCurrent, onAfterStatusChanged, }) {
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
        mutationFn: async (detail) => {
            const adapter = getFingerprintAdapter();
            const result = await adapter.startCapture(asMatchCandidate(detail));
            if (!result.success) {
                throw new Error(result.message || '指纹采集失败，请重试');
            }
            return submitFingerprint({
                taskId,
                candidateId: detail.candidateId,
                sessionId: detail.sessionId,
                examSessionId: detail.examSessionId,
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
                examSessionId: candidate.examSessionId,
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
        mutationFn: (reason) => {
            if (!candidate) {
                throw new Error('请先确认当前考生');
            }
            return markInfoError({
                taskId,
                examSessionId: candidate.examSessionId,
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
        return (_jsx("div", { className: "collection-panel current-candidate-panel empty-current-candidate", children: _jsx(Empty, { description: (_jsxs(Space, { direction: "vertical", size: 4, children: [_jsx(Typography.Text, { children: "\u7B49\u5F85\u786E\u8BA4\u8003\u751F" }), _jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u626B\u7801\u6216\u4ECE\u8003\u751F\u5217\u8868\u9009\u62E9\u540E\u5F00\u59CB\u91C7\u96C6" })] })) }) }));
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
    return (_jsxs("div", { className: "collection-panel current-candidate-panel", children: [_jsxs("div", { className: "current-candidate-hero", children: [_jsx(Typography.Text, { type: "secondary", children: "\u5F53\u524D\u91C7\u96C6\u5BF9\u8C61" }), _jsx(Typography.Title, { level: 2, children: candidate.name })] }), _jsxs(Descriptions, { className: "current-candidate-desc", column: 2, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u8EAB\u4EFD\u8BC1\u53F7", children: candidate.maskedIdCardNo || '-' }), _jsx(Descriptions.Item, { label: "\u51C6\u8003\u8BC1\u53F7", children: candidate.admissionNo || '-' }), _jsx(Descriptions.Item, { label: "\u6307\u7EB9\u72B6\u6001", children: getFingerprintStatusText(candidate) }), _jsx(Descriptions.Item, { label: "\u7535\u5B50\u7B7E\u540D", children: getSignatureStatusText(candidate) })] }), _jsx(SignaturePad, { taskId: taskId, candidate: candidate, disabled: Boolean(signatureDisabledReason), disabledReason: signatureDisabledReason, onSubmitted: onRefresh }), _jsxs("div", { className: "current-candidate-actions", children: [_jsx(Button, { icon: _jsx(SafetyCertificateOutlined, {}), loading: fingerprintMutation.isPending, disabled: !canOperate, onClick: () => fingerprintMutation.mutate(candidate), children: fingerprintButtonText }), _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), loading: completeMutation.isPending, disabled: !canOperate, onClick: handleComplete, children: "\u63D0\u4EA4\u5B8C\u6210\u5E76\u6E05\u7A7A\u5F53\u524D\u8003\u751F" }), _jsx(Button, { danger: true, icon: _jsx(WarningOutlined, {}), loading: panelLoading, disabled: !canOperate, onClick: () => {
                            setInfoErrorReason('');
                            setInfoErrorOpen(true);
                        }, children: "\u6807\u8BB0\u5F02\u5E38" }), _jsx(Button, { icon: _jsx(CloseOutlined, {}), onClick: onCancelCurrent, children: "\u53D6\u6D88\u5F53\u524D" })] }), _jsx(Modal, { title: "\u6807\u8BB0\u5F02\u5E38", open: infoErrorOpen, okText: "\u786E\u8BA4\u5F02\u5E38", cancelText: "\u53D6\u6D88", confirmLoading: infoErrorMutation.isPending, okButtonProps: { danger: true }, onCancel: () => {
                    setInfoErrorOpen(false);
                    setInfoErrorReason('');
                }, onOk: handleSubmitInfoError, destroyOnHidden: true, children: _jsx(Input.TextArea, { rows: 4, maxLength: 1000, showCount: true, value: infoErrorReason, placeholder: "\u8BF7\u8F93\u5165\u5F02\u5E38\u539F\u56E0", onChange: (event) => setInfoErrorReason(event.target.value) }) })] }));
}

import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { PlayCircleOutlined, ReloadOutlined, SafetyCertificateOutlined, ScanOutlined, } from '@ant-design/icons';
import { Alert, Button, Card, Descriptions, Input, Modal, Space, Spin, Tag, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { getCollectionDetail, getCollectionWorkbench, openCandidateCollection, cancelCurrentCollection, scanMatchCurrent, } from '../../api/certificationPracticalCollection';
import { CollectionTaskSelectorBar } from '../../components/certification/collection/CollectionTaskSelectorBar';
import { CollectionTaskSwitchModal } from '../../components/certification/collection/CollectionTaskSwitchModal';
import { CurrentCandidateCollectionPanel } from '../../components/certification/collection/CurrentCandidateCollectionPanel';
import { getFingerprintAdapter } from '../../services/fingerprint/FingerprintAdapterFactory';
import { getScanAdapter, hasAndroidScanBridge } from '../../services/scan/ScanAdapterFactory';
import { parseCollectionToken } from '../../services/scan/scanToken';
import { parseScanQRCodeResult } from '../../services/scan/parseScanQRCodeResult';
import { hasCollectedCollectionData, normalizeActiveCollectionCandidate, toCandidateCollectionContext, } from '../../utils/collectionCandidate';
import { CollectionOverallStatusText } from '../../types/certificationPracticalCollection';
const tabletStatusText = {
    CURRENT: '当前',
    COMPLETED: '已完成',
    PENDING: '未采集',
    EXCEPTION: '异常',
    RELEASED: '已放行',
};
const tabletStatusColor = {
    CURRENT: 'blue',
    COMPLETED: 'green',
    PENDING: 'default',
    EXCEPTION: 'red',
    RELEASED: 'blue',
};
function isSameId(left, right) {
    return left !== undefined && left !== null && right !== undefined && right !== null && String(left) === String(right);
}
function isSameOptionalId(left, right) {
    if ((left === undefined || left === null) && (right === undefined || right === null)) {
        return true;
    }
    return isSameId(left, right);
}
function getTabletCandidateStatus(candidate, currentCandidateId) {
    if (isSameId(candidate.candidateId, currentCandidateId)) {
        return 'CURRENT';
    }
    if (candidate.overallStatus === 'COMPLETED') {
        return 'COMPLETED';
    }
    if (candidate.overallStatus === 'EXCEPTION_RELEASED' || candidate.exceptionStatus === 'RELEASED') {
        return 'RELEASED';
    }
    if (candidate.overallStatus === 'PENDING_RETRY'
        || candidate.overallStatus === 'INFO_ERROR'
        || candidate.fingerprintStatus === 'FAILED'
        || candidate.signatureStatus === 'FAILED'
        || (candidate.exceptionStatus && candidate.exceptionStatus !== 'NONE')) {
        return 'EXCEPTION';
    }
    if (candidate.overallStatus === 'COLLECTING') {
        return 'CURRENT';
    }
    return 'PENDING';
}
function configuredScanMode() {
    return String(import.meta.env.VITE_SCAN_MODE || 'MANUAL').toUpperCase();
}
export function CollectionWorkbenchPage({ taskId, examSessionId, onTaskChange, taskSelectorVisible: taskSelectorVisibleProp, onTaskSelectorVisibleChange, }) {
    const queryClient = useQueryClient();
    const [online, setOnline] = useState(navigator.onLine);
    const [deviceStatus, setDeviceStatus] = useState();
    const [activeCandidate, setActiveCandidate] = useState(null);
    const [activeCollectionContext, setActiveCollectionContext] = useState(null);
    const [candidateKeyword, setCandidateKeyword] = useState('');
    const [debugScanOpen, setDebugScanOpen] = useState(false);
    const [debugScanToken, setDebugScanToken] = useState('');
    const [internalTaskSelectorVisible, setInternalTaskSelectorVisible] = useState(false);
    const [taskSwitching, setTaskSwitching] = useState(false);
    const taskSelectorVisible = taskSelectorVisibleProp ?? internalTaskSelectorVisible;
    const setTaskSelectorVisible = onTaskSelectorVisibleChange ?? setInternalTaskSelectorVisible;
    const workbenchQuery = useQuery({
        queryKey: ['practical-collection-workbench', taskId, examSessionId],
        queryFn: () => getCollectionWorkbench(taskId, examSessionId),
        refetchInterval: online ? 10000 : false,
    });
    const refreshWorkbench = () => {
        queryClient.invalidateQueries({ queryKey: ['practical-collection-workbench', taskId, examSessionId] });
    };
    const deviceMutation = useMutation({
        mutationFn: () => getFingerprintAdapter().checkDevice(),
        onSuccess: setDeviceStatus,
    });
    const setCurrentCollectionCandidate = (candidate, entrySource) => {
        const normalized = normalizeActiveCollectionCandidate(candidate);
        setActiveCandidate(normalized);
        setActiveCollectionContext(toCandidateCollectionContext(normalized, entrySource));
    };
    const clearCurrentCandidate = () => {
        setActiveCandidate(null);
        setActiveCollectionContext(null);
    };
    const releaseCurrentCollectingCandidate = async (candidate) => {
        if (candidate.overallStatus !== 'COLLECTING') {
            clearCurrentCandidate();
            return;
        }
        try {
            await cancelCurrentCollection(taskId, candidate.candidateId, examSessionId);
            clearCurrentCandidate();
            refreshWorkbench();
            message.success('已取消当前采集');
        }
        catch (error) {
            message.error(error instanceof Error ? error.message : '取消当前采集失败，请稍后重试');
            throw error;
        }
    };
    const handleCancelCurrent = () => {
        if (!activeCandidate) {
            return;
        }
        if (activeCandidate.overallStatus === 'COLLECTING') {
            Modal.confirm({
                title: '取消当前采集？',
                content: hasCollectedCollectionData(activeCandidate)
                    ? '取消后将释放该考生的「采集中」占用，已提交的指纹/签名记录会保留，可稍后再继续采集。'
                    : '取消后将释放该考生的「采集中」占用，可切换到其他考生开始采集。',
                okText: '确认取消',
                cancelText: '继续采集',
                onOk: () => releaseCurrentCollectingCandidate(activeCandidate),
            });
            return;
        }
        if (hasCollectedCollectionData(activeCandidate)) {
            Modal.confirm({
                title: '取消当前采集？',
                content: '当前考生已有采集记录，取消后仅清空本机当前考生，不会删除已提交记录。',
                okText: '确认取消',
                cancelText: '继续采集',
                onOk: clearCurrentCandidate,
            });
            return;
        }
        clearCurrentCandidate();
    };
    useEffect(() => {
        deviceMutation.mutate();
        const onOnline = () => setOnline(true);
        const onOffline = () => setOnline(false);
        window.addEventListener('online', onOnline);
        window.addEventListener('offline', onOffline);
        return () => {
            window.removeEventListener('online', onOnline);
            window.removeEventListener('offline', onOffline);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);
    const refreshActiveCandidate = async () => {
        refreshWorkbench();
        if (!activeCandidate) {
            return;
        }
        try {
            const detail = await getCollectionDetail(taskId, activeCandidate.candidateId, examSessionId);
            setCurrentCollectionCandidate(detail, activeCollectionContext?.entrySource || 'QR_SCAN');
        }
        catch {
            message.warning('当前考生信息刷新失败，请稍后重试');
        }
    };
    const scanMatchMutation = useMutation({
        mutationFn: async (manualToken) => {
            let token = parseCollectionToken(manualToken);
            if (!token) {
                const scanResult = await getScanAdapter().startScan();
                if (!scanResult.success) {
                    if (scanResult.code === 'CANCELLED') {
                        return null;
                    }
                    if (scanResult.code === 'PERMISSION_DENIED') {
                        throw new Error('未授予摄像头权限，请在系统设置中开启后重试');
                    }
                    throw new Error(scanResult.message || '扫码失败，请重试');
                }
                const parsed = scanResult.parsed || parseScanQRCodeResult(scanResult.rawText);
                token = parseCollectionToken(scanResult.token || parsed.token || scanResult.rawText);
            }
            if (!token) {
                throw new Error('未识别到有效的二维码 token');
            }
            return scanMatchCurrent(taskId, token, examSessionId);
        },
        onSuccess: (result) => {
            if (!result) {
                return;
            }
            setDebugScanOpen(false);
            setDebugScanToken('');
            if (result.alreadyCompleted) {
                Modal.confirm({
                    title: '该考生已完成采集',
                    content: '该考生已完成采集，是否查看采集信息？系统默认不会覆盖已完成的采集数据。',
                    okText: '查看采集信息',
                    cancelText: '取消',
                    onOk: () => {
                        setCurrentCollectionCandidate(result.currentCandidate, 'QR_SCAN');
                        refreshWorkbench();
                    },
                });
                return;
            }
            setCurrentCollectionCandidate(result.currentCandidate, 'QR_SCAN');
            message.success(`已匹配考生：${result.currentCandidate.name}`);
            refreshWorkbench();
        },
        onError: (error) => {
            const errorMessage = error instanceof Error ? error.message : '扫码匹配失败，请重试';
            if (errorMessage.includes('当前已有正在采集')) {
                Modal.warning({
                    title: '扫码入口冲突',
                    content: errorMessage,
                    okText: '我知道了',
                });
                return;
            }
            if (errorMessage.includes('不属于当前任务')
                || errorMessage.includes('采集二维码无效')) {
                message.error('该考生不属于当前采集任务，请切换任务后重试');
                return;
            }
            if (errorMessage.includes('NOT_FOUND')) {
                message.error('未匹配到考生，请确认二维码是否正确');
                return;
            }
            message.error(errorMessage);
        },
    });
    const openCandidateMutation = useMutation({
        mutationFn: (candidate) => openCandidateCollection(taskId, candidate.candidateId, examSessionId),
        onSuccess: (detail) => {
            setCurrentCollectionCandidate(detail, 'CANDIDATE_ROW');
            message.success(`已进入考生采集：${detail.name}`);
            refreshWorkbench();
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '打开考生采集失败，请稍后重试');
        },
    });
    const workbench = workbenchQuery.data;
    const currentCandidateId = activeCandidate?.candidateId;
    const deviceColor = deviceStatus?.connected ? 'success' : deviceStatus?.status === 'ERROR' ? 'error' : 'warning';
    const filteredCandidates = useMemo(() => {
        const keyword = candidateKeyword.trim().toLowerCase();
        if (!keyword) {
            return workbench?.candidates || [];
        }
        return (workbench?.candidates || []).filter((candidate) => [
            candidate.name,
            candidate.admissionNo,
            candidate.unitName,
            candidate.workType,
            candidate.maskedIdCardNo,
        ].some((value) => String(value || '').toLowerCase().includes(keyword)));
    }, [candidateKeyword, workbench?.candidates]);
    const openScanEntry = () => {
        if (configuredScanMode() === 'MOCK' || hasAndroidScanBridge()) {
            scanMatchMutation.mutate(undefined);
            return;
        }
        setDebugScanToken('');
        setDebugScanOpen(true);
    };
    const handleScanClick = () => {
        const currentCollectingCandidate = getCurrentCollectingCandidate();
        if (currentCollectingCandidate) {
            showCollectingConflict(currentCollectingCandidate);
            return;
        }
        const needsSwitchConfirm = activeCandidate
            && activeCandidate.overallStatus !== 'COMPLETED'
            && hasCollectedCollectionData(activeCandidate);
        if (needsSwitchConfirm) {
            Modal.confirm({
                title: '切换考生确认',
                content: '当前考生采集尚未完成，切换后可能丢失未提交数据，是否确认切换？',
                okText: '确认切换',
                cancelText: '继续当前采集',
                onOk: openScanEntry,
            });
            return;
        }
        openScanEntry();
    };
    const handleDebugScanSubmit = () => {
        const token = parseCollectionToken(debugScanToken);
        if (!token) {
            message.warning('请输入二维码 token');
            return;
        }
        scanMatchMutation.mutate(token);
    };
    const getCurrentCollectingCandidate = () => {
        if (activeCandidate?.overallStatus === 'COLLECTING') {
            return activeCandidate;
        }
        if (workbench?.currentCandidate?.overallStatus === 'COLLECTING') {
            return workbench.currentCandidate;
        }
        return (workbench?.candidates || []).find((candidate) => candidate.overallStatus === 'COLLECTING') || null;
    };
    const showCollectingConflict = (current, selected) => {
        Modal.warning({
            title: '当前已有采集中考生',
            content: (_jsxs(Descriptions, { column: 1, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u5F53\u524D\u8003\u751F", children: current.name || '-' }), _jsx(Descriptions.Item, { label: "\u5F53\u524D\u8BC1\u4EF6\u53F7", children: current.maskedIdCardNo || (current.idCardLast4 ? `后四位 ${current.idCardLast4}` : '-') }), selected ? (_jsx(Descriptions.Item, { label: "\u672C\u6B21\u9009\u62E9", children: selected.name || '-' })) : null, _jsx(Descriptions.Item, { label: "\u5904\u7406\u65B9\u5F0F", children: "\u8BF7\u5148\u5B8C\u6210\u3001\u53D6\u6D88\u5F53\u524D\u6216\u6807\u8BB0\u5F02\u5E38\u5F53\u524D\u8003\u751F\uFF0C\u5237\u65B0\u786E\u8BA4\u73B0\u573A\u72B6\u6001\u540E\u518D\u5207\u6362\u5230\u5176\u4ED6\u8003\u751F\u3002" })] })),
            okText: '我知道了',
        });
    };
    const validateCandidateEntry = (candidate) => {
        if (candidate.collectionEligible === false) {
            Modal.warning({
                title: '资料未满足信息采集准入条件',
                content: `该考生的信息收集尚未完成。${candidate.missingDocumentCodes?.length ? `缺少：${candidate.missingDocumentCodes.join('、')}。` : ''}${candidate.invalidDocumentCodes?.length ? `资料异常：${candidate.invalidDocumentCodes.join('、')}。` : ''}请先前往“认定筹备—信息收集”补充资料。`,
                okText: '我知道了',
            });
            return false;
        }
        const currentCollectingCandidate = getCurrentCollectingCandidate();
        if (currentCollectingCandidate && !isSameId(currentCollectingCandidate.candidateId, candidate.candidateId)) {
            showCollectingConflict(currentCollectingCandidate, candidate);
            return false;
        }
        return true;
    };
    const confirmOpenCandidateCollection = (candidate) => {
        if (!validateCandidateEntry(candidate)) {
            return;
        }
        Modal.confirm({
            title: '确认进入考生采集',
            content: (_jsxs(Descriptions, { column: 1, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u8003\u751F\u59D3\u540D", children: candidate.name || '-' }), _jsx(Descriptions.Item, { label: "\u8BC1\u4EF6\u53F7", children: candidate.maskedIdCardNo || (candidate.idCardLast4 ? `后四位 ${candidate.idCardLast4}` : '-') }), _jsx(Descriptions.Item, { label: "\u5F53\u524D\u4EFB\u52A1", children: workbench?.task.taskName || '-' }), _jsx(Descriptions.Item, { label: "\u91C7\u96C6\u72B6\u6001", children: candidate.overallStatusLabel || CollectionOverallStatusText[candidate.overallStatus] })] })),
            okText: candidate.overallStatus === 'COMPLETED' ? '查看/继续采集' : '开始采集',
            cancelText: '取消',
            onOk: () => openCandidateMutation.mutateAsync(candidate),
        });
    };
    const performTaskSwitch = (nextTask) => {
        const nextTaskId = nextTask.taskId;
        if (isSameId(nextTaskId, taskId) && isSameOptionalId(nextTask.examSessionId, examSessionId)) {
            setTaskSelectorVisible(false);
            return;
        }
        setTaskSwitching(true);
        clearCurrentCandidate();
        setCandidateKeyword('');
        setDebugScanOpen(false);
        setDebugScanToken('');
        onTaskChange(nextTask);
        setTaskSelectorVisible(false);
        setTaskSwitching(false);
        message.success('已切换采集任务');
    };
    const handleTaskSwitchRequest = (nextTask) => {
        const nextTaskId = nextTask.taskId;
        if (isSameId(nextTaskId, taskId) && isSameOptionalId(nextTask.examSessionId, examSessionId)) {
            setTaskSelectorVisible(false);
            return;
        }
        if (activeCandidate && hasCollectedCollectionData(activeCandidate)) {
            Modal.confirm({
                title: '切换任务确认',
                content: '当前考生已有未提交的采集数据，切换任务后本机临时状态将清空，是否继续？',
                okText: '确认切换',
                cancelText: '继续当前采集',
                onOk: () => performTaskSwitch(nextTask),
            });
            return;
        }
        if (activeCandidate) {
            Modal.confirm({
                title: '切换任务确认',
                content: '切换任务后将清空当前选中的考生，是否继续？',
                okText: '确认切换',
                cancelText: '取消',
                onOk: () => performTaskSwitch(nextTask),
            });
            return;
        }
        performTaskSwitch(nextTask);
    };
    return (_jsx("div", { className: "practical-collection-shell", children: _jsxs("div", { className: "collection-workbench", children: [_jsxs("div", { className: "collection-tablet-topline", children: [_jsx(CollectionTaskSelectorBar, { task: workbench?.task, loading: workbenchQuery.isLoading, onSwitchClick: () => setTaskSelectorVisible(true) }), _jsxs(Space, { className: "collection-tablet-status-strip", size: 8, wrap: true, children: [_jsx(Tag, { color: online ? 'success' : 'error', children: online ? '网络在线' : '网络离线' }), _jsx(Tag, { icon: _jsx(SafetyCertificateOutlined, {}), color: deviceColor, children: deviceStatus?.message || '指纹设备未检测' }), _jsx(Button, { loading: deviceMutation.isPending, onClick: () => deviceMutation.mutate(), children: "\u68C0\u6D4B\u8BBE\u5907" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: workbenchQuery.isFetching, onClick: () => workbenchQuery.refetch(), children: "\u5237\u65B0" })] })] }), workbenchQuery.isLoading ? (_jsx("div", { className: "collection-loading", children: _jsx(Spin, { size: "large" }) })) : null, workbenchQuery.isError ? (_jsx(Alert, { type: "error", showIcon: true, message: "\u5DE5\u4F5C\u53F0\u6570\u636E\u52A0\u8F7D\u5931\u8D25\uFF0C\u8BF7\u68C0\u67E5\u4EFB\u52A1\u548C\u7F51\u7EDC\u540E\u91CD\u8BD5" })) : null, workbench ? (_jsxs("div", { className: "collection-tablet-layout", children: [_jsx("div", { className: "collection-tablet-main", children: _jsx(CurrentCandidateCollectionPanel, { taskId: taskId, candidate: activeCandidate, collectionContext: activeCollectionContext, onRefresh: refreshActiveCandidate, onCancelCurrent: handleCancelCurrent, onClearCurrent: clearCurrentCandidate, onAfterStatusChanged: refreshWorkbench }) }), _jsxs("div", { className: "collection-tablet-side", children: [_jsxs(Card, { className: "tablet-scan-card", title: "\u5FEB\u901F\u626B\u7801\u5165\u53E3", bordered: false, children: [_jsxs(Typography.Paragraph, { type: "secondary", className: "tablet-scan-hint", children: ["\u626B\u7801\u5339\u914D\u4EC5\u5728\u5F53\u524D\u4EFB\u52A1\u300C", workbench.task.taskName, "\u300D\u5185\u751F\u6548"] }), _jsx(Button, { block: true, type: "primary", size: "large", icon: _jsx(ScanOutlined, {}), className: "tablet-scan-button", loading: scanMatchMutation.isPending, onClick: handleScanClick, children: "\u626B\u7801\u5339\u914D\u8003\u751F" })] }), _jsxs(Card, { className: "tablet-candidate-list-card", title: "\u5F53\u524D\u4EFB\u52A1\u8003\u751F\u540D\u5355", bordered: false, extra: _jsxs(Tag, { children: [filteredCandidates.length, "/", workbench.candidates.length] }), children: [_jsx(Input, { allowClear: true, value: candidateKeyword, placeholder: "\u7B5B\u9009\u59D3\u540D / \u51C6\u8003\u8BC1\u53F7", onChange: (event) => setCandidateKeyword(event.target.value) }), _jsxs("div", { className: "tablet-candidate-list", children: [filteredCandidates.map((candidate) => {
                                                    const status = getTabletCandidateStatus(candidate, currentCandidateId);
                                                    const statusLabel = status === 'CURRENT' && candidate.overallStatus === 'COLLECTING'
                                                        ? '采集中'
                                                        : tabletStatusText[status];
                                                    return (_jsxs("div", { className: "tablet-candidate-item", role: "button", tabIndex: 0, onClick: () => confirmOpenCandidateCollection(candidate), onKeyDown: (event) => {
                                                            if (event.key === 'Enter' || event.key === ' ') {
                                                                event.preventDefault();
                                                                confirmOpenCandidateCollection(candidate);
                                                            }
                                                        }, children: [_jsxs("div", { className: "tablet-candidate-item-main", children: [_jsx("strong", { children: candidate.name }), _jsxs("span", { children: [candidate.admissionNo || '无准考证号', candidate.maskedIdCardNo ? ` · ${candidate.maskedIdCardNo}` : ''] }), _jsxs("span", { children: [candidate.unitName || '未填写单位', candidate.workType ? ` · ${candidate.workType}` : '', candidate.level ? ` · ${candidate.level}` : ''] })] }), _jsxs(Space, { size: 8, children: [_jsx(Tag, { color: tabletStatusColor[status], children: statusLabel }), candidate.collectionEligible !== true ? (_jsxs(Tag, { color: "orange", children: ["\u8D44\u6599\uFF1A", candidate.documentStatus === 'INVALID' ? '资料异常' : '待补充'] })) : null, _jsx(Button, { size: "large", type: candidate.overallStatus === 'COMPLETED' ? 'default' : 'primary', icon: _jsx(PlayCircleOutlined, {}), loading: openCandidateMutation.isPending, disabled: candidate.collectionEligible === false, onClick: (event) => {
                                                                            event.stopPropagation();
                                                                            confirmOpenCandidateCollection(candidate);
                                                                        }, children: candidate.overallStatus === 'COMPLETED' ? '查看/继续' : '采集' })] })] }, String(candidate.candidateId)));
                                                }), filteredCandidates.length === 0 ? (_jsx("div", { className: "tablet-candidate-empty", children: "\u6682\u65E0\u5339\u914D\u8003\u751F" })) : null] })] })] })] })) : null, _jsx(CollectionTaskSwitchModal, { open: taskSelectorVisible, currentTaskId: taskId, currentExamSessionId: examSessionId, switching: taskSwitching, onSelect: handleTaskSwitchRequest, onClose: () => setTaskSelectorVisible(false) }), _jsxs(Modal, { title: "\u8C03\u8BD5\u7528\uFF1A\u4E8C\u7EF4\u7801 token \u8F93\u5165", open: debugScanOpen, okText: "\u786E\u8BA4\u5339\u914D", cancelText: "\u53D6\u6D88", confirmLoading: scanMatchMutation.isPending, onCancel: () => {
                        setDebugScanOpen(false);
                        setDebugScanToken('');
                    }, onOk: handleDebugScanSubmit, destroyOnHidden: true, children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u4E3A\u6D4F\u89C8\u5668\u8C03\u8BD5\u73AF\u5883\uFF0C\u672A\u68C0\u6D4B\u5230 Android \u626B\u7801\u6865\u63A5\u3002\u53EF\u624B\u52A8\u8F93\u5165 token \u8FDB\u884C\u8054\u8C03\u3002", className: "tablet-debug-scan-alert" }), _jsx(Input, { autoFocus: true, value: debugScanToken, placeholder: "\u8BF7\u8F93\u5165\u6216\u7C98\u8D34\u4E8C\u7EF4\u7801 token", onChange: (event) => setDebugScanToken(event.target.value), onPressEnter: handleDebugScanSubmit })] })] }) }));
}

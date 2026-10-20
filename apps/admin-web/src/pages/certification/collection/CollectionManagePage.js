import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ArrowLeftOutlined, ClusterOutlined, CopyOutlined, DownloadOutlined, ExclamationCircleOutlined, EyeOutlined, LinkOutlined, MonitorOutlined, PlayCircleOutlined, QrcodeOutlined, ReloadOutlined, SearchOutlined, WarningOutlined, } from '@ant-design/icons';
import { Alert, Button, Card, Descriptions, Drawer, Empty, Input, List, Modal, Select, Space, Statistic, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { downloadUrlFile } from '../../../api/files';
import { cancelCurrentCollection, exceptionRelease, exportCollectionForms, exportCollectionPdf, exportQrCodes, generateQrCodes, getCollectionDetail, getCollectionExceptions, getCollectionWorkbench, markInfoError, openCandidateCollection, } from '../../../api/certificationPracticalCollection';
import { CurrentCandidateCollectionPanel } from '../../../components/certification/collection/CurrentCandidateCollectionPanel';
import { useNavStore } from '../../../stores/navStore';
import { normalizeActiveCollectionCandidate, toCandidateCollectionContext, } from '../../../utils/collectionCandidate';
import { CollectionExceptionStatusText, CollectionOverallStatusText, CollectionQueueStatusText, FingerprintCollectStatusText, FingerprintStatusText, SignatureStatusText, } from '../../../types/certificationPracticalCollection';
import { CollectionExceptionPage } from './CollectionExceptionPage';
import { ExceptionReleaseModal } from '../../../components/certification/collection/ExceptionReleaseModal';
import { InfoErrorModal } from '../../../components/certification/collection/InfoErrorModal';
import './CollectionTaskListPage.css';
const overallFilterOptions = [
    { label: '全部状态', value: 'all' },
    { label: '未发放', value: 'NOT_ISSUED' },
    { label: '已发放', value: 'ISSUED' },
    { label: '未到场', value: 'NOT_ARRIVED' },
    { label: '已到场', value: 'ARRIVED' },
    { label: '已入队', value: 'QUEUED' },
    { label: '采集中', value: 'COLLECTING' },
    { label: '待补采', value: 'PENDING_RETRY' },
    { label: '异常', value: 'INFO_ERROR' },
    { label: '已放行', value: 'EXCEPTION_RELEASED' },
    { label: '已完成', value: 'COMPLETED' },
    { label: '已取消', value: 'CANCELLED' },
];
const exceptionFilterOptions = [
    { label: '全部异常', value: 'all' },
    { label: '有异常', value: 'hasException' },
    { label: '待补采', value: 'PENDING_RETRY' },
    { label: '异常', value: 'INFO_ERROR' },
    { label: '已放行', value: 'RELEASED' },
    { label: '无异常', value: 'NONE' },
];
const SignatureRecordStatusText = {
    ACTIVE: '有效',
    REVOKED: '已撤销',
};
function toNumber(value) {
    const numberValue = Number(value || 0);
    return Number.isFinite(numberValue) ? numberValue : 0;
}
function formatDateTime(value) {
    if (!value)
        return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return value;
    return date.toLocaleString();
}
function shortHash(value) {
    if (!value)
        return '-';
    if (value.length <= 18)
        return value;
    return `${value.slice(0, 10)}...${value.slice(-6)}`;
}
function statusColor(value) {
    if (!value)
        return 'default';
    if (['COMPLETED', 'DONE', 'SUCCESS', 'ACTIVE'].includes(value))
        return 'green';
    if (['COLLECTING', 'CAPTURING', 'SIGNING', 'ARRIVED', 'QUEUED', 'ISSUED', 'EXCEPTION_RELEASED', 'RELEASED'].includes(value))
        return 'blue';
    if (['PENDING_RETRY'].includes(value))
        return 'orange';
    if (['INFO_ERROR', 'FAILED'].includes(value))
        return 'red';
    return 'default';
}
function isReleasedCandidate(candidate) {
    return candidate.overallStatus === 'EXCEPTION_RELEASED' || candidate.exceptionStatus === 'RELEASED';
}
function canSelectForExport(candidate) {
    const abnormal = candidate.overallStatus === 'INFO_ERROR';
    return (candidate.collectionEligible === true || abnormal)
        && (candidate.overallStatus === 'COMPLETED'
            || abnormal
            || isReleasedCandidate(candidate));
}
function exportDisabledTitle(candidate) {
    const abnormal = candidate.overallStatus === 'INFO_ERROR';
    if (candidate.collectionEligible !== true && !abnormal) {
        return '资料未完成，不能生成采集表';
    }
    if (candidate.overallStatus === 'COMPLETED'
        || abnormal
        || isReleasedCandidate(candidate)) {
        return undefined;
    }
    return '仅已完成、已放行或异常状态的考生可生成采集表';
}
function statusTag(value, label, textMap) {
    const text = label || (value ? textMap?.[value] || value : '-');
    return _jsx(Tag, { color: statusColor(value), children: text });
}
function normalizeStats(workbench) {
    const stats = workbench?.stats;
    const task = workbench?.task;
    const candidates = workbench?.candidates || [];
    const candidateTotal = toNumber(stats?.candidateTotal || task?.candidateTotal || candidates.length);
    const completedCount = toNumber(stats?.completedCount || task?.completedCount);
    const exceptionCount = toNumber(stats?.exceptionCount || task?.exceptionCount);
    return {
        candidateTotal,
        qrIssuedCount: toNumber(stats?.qrIssuedCount || task?.qrIssuedCount),
        arrivedCount: toNumber(stats?.arrivedCount || task?.arrivedCount),
        queuedCount: toNumber(stats?.queuedCount || task?.queuedCount),
        collectingCount: toNumber(stats?.collectingCount || task?.collectingCount),
        completedCount,
        pendingRetryCount: toNumber(stats?.pendingRetryCount || task?.pendingRetryCount),
        infoErrorCount: toNumber(stats?.infoErrorCount),
        exceptionCount,
        pendingCollectCount: Math.max(candidateTotal - completedCount - exceptionCount, 0),
    };
}
function candidateMatchesKeyword(candidate, keyword) {
    const text = keyword.trim().toLowerCase();
    if (!text)
        return true;
    return [
        candidate.name,
        candidate.admissionNo,
        candidate.maskedIdCardNo,
        candidate.idCardLast4,
        candidate.unitName,
    ].some((value) => String(value || '').toLowerCase().includes(text));
}
function candidateMatchesException(candidate, filter) {
    if (filter === 'all')
        return true;
    if (filter === 'hasException')
        return candidate.exceptionStatus !== 'NONE';
    return candidate.exceptionStatus === filter;
}
function fallbackText(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value);
}
function isSameId(left, right) {
    return left !== undefined && left !== null && right !== undefined && right !== null && String(left) === String(right);
}
async function copyText(text) {
    if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
        return;
    }
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
}
export function CollectionManagePage({ taskId, examSessionId }) {
    const queryClient = useQueryClient();
    const setSelectedKey = useNavStore((state) => state.setSelectedKey);
    const [keyword, setKeyword] = useState('');
    const [overallFilter, setOverallFilter] = useState('all');
    const [exceptionFilter, setExceptionFilter] = useState('all');
    const [detailCandidate, setDetailCandidate] = useState(null);
    const [infoErrorCandidate, setInfoErrorCandidate] = useState(null);
    const [releaseCandidate, setReleaseCandidate] = useState(null);
    const [collectionCandidate, setCollectionCandidate] = useState(null);
    const [collectionContext, setCollectionContext] = useState(null);
    const [exceptionModalOpen, setExceptionModalOpen] = useState(false);
    const [selectedCandidateIds, setSelectedCandidateIds] = useState([]);
    const workbenchQuery = useQuery({
        queryKey: ['practical-collection-workbench', taskId, examSessionId],
        queryFn: () => getCollectionWorkbench(taskId, examSessionId),
        enabled: Boolean(taskId),
    });
    const exceptionsQuery = useQuery({
        queryKey: ['practical-collection-exceptions', taskId, examSessionId, 'manage-overview'],
        queryFn: () => getCollectionExceptions({ page: 1, pageSize: 5, taskId, examSessionId }),
        enabled: Boolean(taskId),
    });
    const detailQuery = useQuery({
        queryKey: ['practical-collection-detail', taskId, examSessionId, detailCandidate?.candidateId],
        queryFn: () => getCollectionDetail(taskId, detailCandidate.candidateId, examSessionId),
        enabled: Boolean(taskId && detailCandidate?.candidateId),
    });
    const setCurrentCollectionCandidate = (candidate, entrySource) => {
        const normalized = normalizeActiveCollectionCandidate(candidate);
        setCollectionCandidate(normalized);
        setCollectionContext(toCandidateCollectionContext(normalized, entrySource));
    };
    const clearCollectionCandidate = () => {
        setCollectionCandidate(null);
        setCollectionContext(null);
    };
    const releaseCurrentCollectingCandidate = async (candidate) => {
        if (candidate.overallStatus !== 'COLLECTING') {
            clearCollectionCandidate();
            return;
        }
        try {
            await cancelCurrentCollection(taskId, candidate.candidateId, examSessionId);
            clearCollectionCandidate();
            refreshAll();
            message.success('已取消当前采集');
        }
        catch (error) {
            message.error(error instanceof Error ? error.message : '取消当前采集失败，请稍后重试');
            throw error;
        }
    };
    const handleCancelCurrent = () => {
        if (!collectionCandidate) {
            return;
        }
        if (collectionCandidate.overallStatus === 'COLLECTING') {
            Modal.confirm({
                title: '取消当前采集？',
                content: '取消后将释放该考生的「采集中」占用，已提交的指纹/签名记录会保留，可稍后再继续采集。',
                okText: '确认取消',
                cancelText: '继续采集',
                onOk: () => releaseCurrentCollectingCandidate(collectionCandidate),
            });
            return;
        }
        clearCollectionCandidate();
    };
    const refreshAll = () => {
        queryClient.invalidateQueries({ queryKey: ['practical-collection-workbench', taskId, examSessionId] });
        queryClient.invalidateQueries({ queryKey: ['practical-collection-exceptions', taskId, examSessionId] });
        if (detailCandidate) {
            queryClient.invalidateQueries({ queryKey: ['practical-collection-detail', taskId, detailCandidate.candidateId] });
        }
        if (collectionCandidate) {
            queryClient.invalidateQueries({ queryKey: ['practical-collection-detail', taskId, collectionCandidate.candidateId] });
        }
    };
    const refreshCollectionCandidate = async () => {
        refreshAll();
        if (!collectionCandidate) {
            return;
        }
        try {
            const detail = await getCollectionDetail(taskId, collectionCandidate.candidateId, examSessionId);
            setCurrentCollectionCandidate(detail, collectionContext?.entrySource || 'CANDIDATE_ROW');
        }
        catch {
            message.warning('当前考生采集数据刷新失败，请稍后重试');
        }
    };
    const generateQrMutation = useMutation({
        mutationFn: () => generateQrCodes(taskId, examSessionId),
        onSuccess: (result) => {
            message.success(result.message || '二维码 token 已生成');
            refreshAll();
        },
    });
    const exportQrMutation = useMutation({
        mutationFn: () => exportQrCodes(taskId, examSessionId),
        onSuccess: (result) => {
            message.success(`二维码已导出：${result.filename}`);
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '二维码导出失败，请稍后重试');
        },
    });
    const exportCollectionFormsMutation = useMutation({
        mutationFn: (candidateIds) => exportCollectionForms(taskId, candidateIds, examSessionId),
        onSuccess: (result) => {
            message.success(`采集表已生成：${result.filename}`);
            setSelectedCandidateIds([]);
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '采集表生成失败，请补齐资料后重试');
        },
    });
    const exportCollectionPdfMutation = useMutation({
        mutationFn: (candidateIds) => exportCollectionPdf(taskId, candidateIds, examSessionId),
        onSuccess: async (result) => {
            try {
                await downloadUrlFile(result.downloadUrl, result.fileName);
                message.success(`打印版 PDF 已生成：${result.fileName}`);
                setSelectedCandidateIds([]);
            }
            catch (error) {
                message.error(error instanceof Error ? error.message : 'PDF 下载失败，请稍后重试');
            }
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '打印版 PDF 生成失败，请补齐资料后重试');
        },
    });
    const markInfoErrorMutation = useMutation({
        mutationFn: (reason) => markInfoError({
            taskId: taskId,
            examSessionId,
            candidateId: infoErrorCandidate.candidateId,
            reason,
        }),
        onSuccess: () => {
            message.success('已标记异常');
            setInfoErrorCandidate(null);
            refreshAll();
        },
    });
    const releaseMutation = useMutation({
        mutationFn: (values) => exceptionRelease({
            ...values,
            examSessionId,
        }),
        onSuccess: () => {
            message.success('免考放行已记录');
            setReleaseCandidate(null);
            refreshAll();
        },
    });
    const openCandidateMutation = useMutation({
        mutationFn: (candidate) => openCandidateCollection(taskId, candidate.candidateId, examSessionId),
        onSuccess: (detail) => {
            setCurrentCollectionCandidate(detail, 'CANDIDATE_ROW');
            message.success(`已进入考生采集：${detail.name}`);
            refreshAll();
        },
        onError: (error) => {
            const errorMessage = error instanceof Error ? error.message : '打开考生采集失败，请稍后重试';
            if (errorMessage.includes('当前已有正在采集')) {
                Modal.warning({
                    title: '列表入口冲突',
                    content: errorMessage,
                    okText: '我知道了',
                });
                return;
            }
            message.error(errorMessage);
        },
    });
    const workbench = workbenchQuery.data;
    const task = workbench?.task;
    const stats = normalizeStats(workbench);
    const candidates = workbench?.candidates || [];
    const recentExceptions = exceptionsQuery.data?.items || [];
    const tabletUrl = taskId
        ? `${window.location.origin}/practical-collection/workbench/${encodeURIComponent(taskId)}${examSessionId ? `?examSessionId=${encodeURIComponent(examSessionId)}` : ''}`
        : '';
    const lastUpdated = workbenchQuery.dataUpdatedAt ? formatDateTime(new Date(workbenchQuery.dataUpdatedAt).toISOString()) : '-';
    const filteredCandidates = useMemo(() => candidates.filter((candidate) => {
        if (!candidateMatchesKeyword(candidate, keyword))
            return false;
        if (overallFilter !== 'all' && candidate.overallStatus !== overallFilter)
            return false;
        return candidateMatchesException(candidate, exceptionFilter);
    }), [candidates, exceptionFilter, keyword, overallFilter]);
    const goBackToList = () => {
        setSelectedKey('certification_practical_collection');
        window.location.assign('/certification/practical-collection');
    };
    const handleCopyTabletUrl = () => {
        if (!tabletUrl) {
            message.warning('缺少任务 ID，无法生成平板端链接');
            return;
        }
        copyText(tabletUrl)
            .then(() => message.success('平板端链接已复制'))
            .catch(() => message.error('复制失败，请手动复制链接'));
    };
    const getCurrentCollectingCandidate = () => {
        if (collectionCandidate?.overallStatus === 'COLLECTING') {
            return collectionCandidate;
        }
        if (workbench?.currentCandidate?.overallStatus === 'COLLECTING') {
            return workbench.currentCandidate;
        }
        return candidates.find((candidate) => candidate.overallStatus === 'COLLECTING') || null;
    };
    const showCollectingConflict = (current, selected) => {
        Modal.warning({
            title: '当前已有采集中考生',
            content: (_jsxs(Descriptions, { column: 1, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u5F53\u524D\u8003\u751F", children: current.name || '-' }), _jsx(Descriptions.Item, { label: "\u5F53\u524D\u8BC1\u4EF6\u53F7", children: current.maskedIdCardNo || (current.idCardLast4 ? `后四位 ${current.idCardLast4}` : '-') }), _jsx(Descriptions.Item, { label: "\u672C\u6B21\u9009\u62E9", children: selected.name || '-' }), _jsx(Descriptions.Item, { label: "\u5904\u7406\u65B9\u5F0F", children: "\u8BF7\u5148\u5B8C\u6210\u3001\u53D6\u6D88\u5F53\u524D\u6216\u6807\u8BB0\u5F02\u5E38\u5F53\u524D\u8003\u751F\uFF0C\u5237\u65B0\u786E\u8BA4\u73B0\u573A\u72B6\u6001\u540E\u518D\u5207\u6362\u5230\u5176\u4ED6\u8003\u751F\u3002" })] })),
            okText: '我知道了',
        });
    };
    const validateCandidateEntry = (candidate) => {
        if (candidate.collectionEligible === false) {
            Modal.warning({
                title: '资料未满足信息采集准入条件',
                content: (_jsxs("div", { children: [_jsx("div", { children: "\u8BE5\u8003\u751F\u7684\u4FE1\u606F\u6536\u96C6\u5C1A\u672A\u5B8C\u6210\uFF0C\u6682\u4E0D\u80FD\u8FDB\u5165\u5B9E\u64CD\u4FE1\u606F\u91C7\u96C6\u3002" }), candidate.missingDocumentCodes?.length ? _jsxs("div", { children: ["\u7F3A\u5C11\u8D44\u6599\uFF1A", candidate.missingDocumentCodes.join('、')] }) : null, candidate.invalidDocumentCodes?.length ? _jsxs("div", { children: ["\u8D44\u6599\u5F02\u5E38\uFF1A", candidate.invalidDocumentCodes.join('、')] }) : null, _jsx("div", { children: "\u8BF7\u5148\u524D\u5F80\u201C\u8BA4\u5B9A\u7B79\u5907\u2014\u4FE1\u606F\u6536\u96C6\u201D\u8865\u5145\u8D44\u6599\u3002" })] })),
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
            content: (_jsxs(Descriptions, { column: 1, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u8003\u751F\u59D3\u540D", children: candidate.name || '-' }), _jsx(Descriptions.Item, { label: "\u8BC1\u4EF6\u53F7", children: candidate.maskedIdCardNo || (candidate.idCardLast4 ? `后四位 ${candidate.idCardLast4}` : '-') }), _jsx(Descriptions.Item, { label: "\u5F53\u524D\u4EFB\u52A1", children: task?.taskName || '-' }), _jsx(Descriptions.Item, { label: "\u91C7\u96C6\u72B6\u6001", children: candidate.overallStatusLabel || CollectionOverallStatusText[candidate.overallStatus] })] })),
            okText: candidate.overallStatus === 'COMPLETED' ? '查看/继续采集' : '开始采集',
            cancelText: '取消',
            onOk: () => openCandidateMutation.mutateAsync(candidate),
        });
    };
    const candidateColumns = [
        {
            title: '姓名',
            dataIndex: 'name',
            width: 110,
            fixed: 'left',
            render: (value) => _jsx(Typography.Text, { strong: true, children: value || '-' }),
        },
        {
            title: '考号',
            dataIndex: 'admissionNo',
            width: 140,
            render: fallbackText,
        },
        {
            title: '身份证号',
            width: 150,
            render: (_, candidate) => candidate.maskedIdCardNo || (candidate.idCardLast4 ? `后四位 ${candidate.idCardLast4}` : '-'),
        },
        {
            title: '单位',
            dataIndex: 'unitName',
            width: 150,
            render: fallbackText,
        },
        {
            title: '工种/等级',
            width: 170,
            render: (_, candidate) => `${candidate.workType || '-'} / ${candidate.level || '-'}`,
        },
        {
            title: '采集状态',
            dataIndex: 'overallStatus',
            width: 120,
            render: (value, candidate) => (statusTag(value, candidate.overallStatusLabel, CollectionOverallStatusText)),
        },
        {
            title: '资料状态',
            dataIndex: 'documentStatus',
            width: 120,
            render: (_value, candidate) => {
                const complete = candidate.collectionEligible === true;
                const label = complete ? '已完成' : candidate.documentStatus === 'INVALID' ? '资料异常' : '待补充';
                return (_jsxs(Space, { direction: "vertical", size: 0, children: [_jsx(Tag, { color: complete ? 'green' : candidate.documentStatus === 'INVALID' ? 'red' : 'orange', children: label }), !complete && candidate.missingDocumentCodes?.length ? (_jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["\u7F3A\u5C11\uFF1A", candidate.missingDocumentCodes.join('、')] })) : null, !complete && candidate.invalidDocumentCodes?.length ? (_jsxs(Typography.Text, { type: "danger", style: { fontSize: 12 }, children: ["\u5F02\u5E38\uFF1A", candidate.invalidDocumentCodes.join('、')] })) : null] }));
            },
        },
        {
            title: '指纹状态',
            dataIndex: 'fingerprintStatus',
            width: 110,
            render: (value, candidate) => (statusTag(value, candidate.fingerprintStatusLabel, FingerprintStatusText)),
        },
        {
            title: '签名状态',
            dataIndex: 'signatureStatus',
            width: 110,
            render: (value, candidate) => (statusTag(value, candidate.signatureStatusLabel, SignatureStatusText)),
        },
        {
            title: '队列状态',
            dataIndex: 'queueStatus',
            width: 110,
            render: (value, candidate) => (statusTag(value, candidate.queueStatusLabel, CollectionQueueStatusText)),
        },
        {
            title: '异常状态',
            dataIndex: 'exceptionStatus',
            width: 110,
            render: (value, candidate) => (statusTag(value, candidate.exceptionStatusLabel, CollectionExceptionStatusText)),
        },
        {
            title: '操作',
            width: 260,
            fixed: 'right',
            render: (_, candidate) => (_jsxs(Space, { size: 4, className: "collection-manage-row-actions", children: [_jsx(Button, { size: "small", type: candidate.overallStatus === 'COMPLETED' ? 'default' : 'primary', icon: _jsx(PlayCircleOutlined, {}), loading: openCandidateMutation.isPending, onClick: (event) => {
                            event.stopPropagation();
                            confirmOpenCandidateCollection(candidate);
                        }, children: candidate.overallStatus === 'COMPLETED' ? '查看/继续' : '采集' }), _jsx(Button, { size: "small", icon: _jsx(EyeOutlined, {}), onClick: (event) => {
                            event.stopPropagation();
                            setDetailCandidate(candidate);
                        }, children: "\u8BE6\u60C5" }), _jsx(Button, { size: "small", danger: true, icon: _jsx(WarningOutlined, {}), disabled: candidate.overallStatus === 'COMPLETED', title: candidate.overallStatus === 'COMPLETED' ? '采集已完成，不能标记异常' : undefined, onClick: (event) => {
                            event.stopPropagation();
                            setInfoErrorCandidate(candidate);
                        }, children: "\u5F02\u5E38" }), _jsx(Button, { size: "small", disabled: candidate.overallStatus === 'CANCELLED'
                            || candidate.overallStatus === 'EXCEPTION_RELEASED', title: candidate.overallStatus === 'CANCELLED'
                            ? '已取消的考生不能免考放行'
                            : candidate.overallStatus === 'EXCEPTION_RELEASED'
                                ? '该考生已放行'
                                : undefined, onClick: (event) => {
                            event.stopPropagation();
                            setReleaseCandidate(candidate);
                        }, children: "\u514D\u8003\u653E\u884C" })] })),
        },
    ];
    if (!taskId) {
        return (_jsxs("div", { className: "knowledge-page collection-manage-page", children: [_jsx(Alert, { type: "warning", showIcon: true, message: "\u4EFB\u52A1 ID \u7F3A\u5931", description: "\u65E0\u6CD5\u6253\u5F00 PC \u91C7\u96C6\u7BA1\u7406\u53F0\uFF0C\u8BF7\u8FD4\u56DE\u4FE1\u606F\u91C7\u96C6\u9996\u9875\u540E\u91CD\u65B0\u8FDB\u5165\u3002" }), _jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: goBackToList, children: "\u8FD4\u56DE\u4FE1\u606F\u91C7\u96C6\u9996\u9875" })] }));
    }
    const detail = detailQuery.data;
    return (_jsxs("div", { className: "knowledge-page collection-manage-page", children: [_jsxs("div", { className: "page-heading knowledge-heading collection-manage-heading", children: [_jsx("div", { children: _jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(ClusterOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u4FE1\u606F\u91C7\u96C6\u7BA1\u7406\u53F0" }), _jsxs(Typography.Text, { type: "secondary", children: [task?.taskName || '正在加载任务', " \u00B7 \u5F53\u524D\u4EFB\u52A1 ID\uFF1A", taskId] })] })] }) }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: goBackToList, children: "\u8FD4\u56DE\u4FE1\u606F\u91C7\u96C6\u9996\u9875" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: workbenchQuery.isFetching || exceptionsQuery.isFetching, onClick: refreshAll, children: "\u5237\u65B0" })] })] }), workbenchQuery.isError ? (_jsx(Alert, { type: "error", showIcon: true, message: "\u7BA1\u7406\u53F0\u6570\u636E\u52A0\u8F7D\u5931\u8D25\uFF0C\u8BF7\u68C0\u67E5\u4EFB\u52A1\u548C\u7F51\u7EDC\u540E\u91CD\u8BD5" })) : null, _jsx(Card, { className: "collection-manage-task-card", loading: workbenchQuery.isLoading, children: _jsxs(Descriptions, { column: { xs: 1, sm: 2, lg: 5 }, size: "small", children: [_jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u540D\u79F0", children: task?.taskName || '-' }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7F16\u53F7", children: task?.taskNo || '-' }), _jsx(Descriptions.Item, { label: "\u4F01\u4E1A", children: task?.projectName || '-' }), _jsx(Descriptions.Item, { label: "\u5DE5\u79CD/\u7B49\u7EA7", children: task?.workTypeLevel || `${task?.workType || '-'} / ${task?.level || '-'}` }), _jsx(Descriptions.Item, { label: "\u8BA4\u5B9A\u65E5\u671F", children: task?.examDate || '-' })] }) }), _jsxs("div", { className: "collection-manage-stats", children: [_jsx(Card, { children: _jsx(Statistic, { title: "\u603B\u4EBA\u6570", value: stats.candidateTotal }) }), _jsx(Card, { children: _jsx(Statistic, { title: "\u5DF2\u53D1\u653E", value: stats.qrIssuedCount }) }), _jsx(Card, { children: _jsx(Statistic, { title: "\u5DF2\u5230\u573A", value: stats.arrivedCount }) }), _jsx(Card, { children: _jsx(Statistic, { title: "\u5DF2\u5165\u961F", value: stats.queuedCount }) }), _jsx(Card, { children: _jsx(Statistic, { title: "\u91C7\u96C6\u4E2D", value: stats.collectingCount }) }), _jsx(Card, { children: _jsx(Statistic, { title: "\u5DF2\u5B8C\u6210", value: stats.completedCount }) }), _jsx(Card, { children: _jsx(Statistic, { title: "\u5F85\u91C7\u96C6", value: stats.pendingCollectCount }) }), _jsx(Card, { children: _jsx(Statistic, { title: "\u5F02\u5E38", value: stats.exceptionCount, valueStyle: { color: stats.exceptionCount ? '#cf1322' : undefined } }) })] }), _jsxs("div", { className: "collection-manage-grid", children: [_jsxs(Card, { className: "collection-manage-candidate-card", title: "\u5F53\u524D\u4EFB\u52A1\u8003\u751F\u72B6\u6001", extra: _jsxs(Typography.Text, { type: "secondary", children: ["\u5171 ", filteredCandidates.length, " / ", candidates.length, " \u4EBA"] }), children: [_jsxs(Space, { wrap: true, className: "collection-manage-table-toolbar", children: [_jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u59D3\u540D / \u8003\u53F7 / \u8EAB\u4EFD\u8BC1 / \u5355\u4F4D", value: keyword, onChange: (event) => setKeyword(event.target.value), style: { width: 280 } }), _jsx(Select, { value: overallFilter, options: overallFilterOptions, onChange: setOverallFilter, style: { width: 140 } }), _jsx(Select, { value: exceptionFilter, options: exceptionFilterOptions, onChange: setExceptionFilter, style: { width: 140 } }), _jsx(Button, { type: "primary", icon: _jsx(DownloadOutlined, {}), disabled: !selectedCandidateIds.length, loading: exportCollectionFormsMutation.isPending, onClick: () => {
                                            if (!selectedCandidateIds.length) {
                                                message.warning('请先选择需要生成采集表的考生。');
                                                return;
                                            }
                                            exportCollectionFormsMutation.mutate(selectedCandidateIds);
                                        }, children: "\u751F\u6210 Excel" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), disabled: !selectedCandidateIds.length, loading: exportCollectionPdfMutation.isPending, onClick: () => {
                                            if (!selectedCandidateIds.length) {
                                                message.warning('请先选择需要生成打印版 PDF 的考生。');
                                                return;
                                            }
                                            exportCollectionPdfMutation.mutate(selectedCandidateIds);
                                        }, children: "\u751F\u6210\u6253\u5370\u7248 PDF" })] }), _jsx(Table, { rowKey: (candidate) => String(candidate.candidateId), columns: candidateColumns, dataSource: filteredCandidates, loading: workbenchQuery.isFetching, pagination: { defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: (total) => `共 ${total} 名考生` }, scroll: { x: 1440 }, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u8003\u751F" }) }, rowSelection: {
                                    selectedRowKeys: selectedCandidateIds.map(String),
                                    onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
                                    getCheckboxProps: (candidate) => ({
                                        disabled: !canSelectForExport(candidate),
                                        title: exportDisabledTitle(candidate),
                                    }),
                                }, onRow: (candidate) => ({
                                    onClick: () => confirmOpenCandidateCollection(candidate),
                                }) })] }), _jsxs(Space, { direction: "vertical", size: 12, className: "collection-manage-side", children: [_jsx(Card, { title: _jsxs(Space, { children: [_jsx(MonitorOutlined, {}), "\u73B0\u573A\u76D1\u63A7"] }), children: _jsxs(Descriptions, { column: 1, size: "small", children: [_jsx(Descriptions.Item, { label: "\u5F53\u524D\u91C7\u96C6\u8003\u751F", children: workbench?.currentCandidate?.name || '暂无' }), _jsx(Descriptions.Item, { label: "\u5F53\u524D\u961F\u5217\u4EBA\u6570", children: workbench?.queue?.length || 0 }), _jsx(Descriptions.Item, { label: "\u5E73\u677F\u5728\u7EBF\u72B6\u6001", children: "\u5F85\u63A5\u5165" }), _jsx(Descriptions.Item, { label: "\u6700\u8FD1\u66F4\u65B0\u65F6\u95F4", children: lastUpdated })] }) }), _jsxs(Card, { title: _jsxs(Space, { children: [_jsx(QrcodeOutlined, {}), "\u5E73\u677F\u7AEF\u5165\u53E3\u7BA1\u7406"] }), children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u8BE5\u9875\u9762\u7528\u4E8E\u5E73\u677F\u7AEF\u73B0\u573A\u91C7\u96C6\uFF0CPC \u7AEF\u4EC5\u7528\u4E8E\u8C03\u8BD5\u6216\u9884\u89C8\u3002", className: "collection-manage-inline-alert" }), _jsx(Typography.Paragraph, { copyable: { text: tabletUrl }, className: "collection-tablet-url", children: tabletUrl || '-' }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(CopyOutlined, {}), onClick: handleCopyTabletUrl, children: "\u590D\u5236\u5E73\u677F\u7AEF\u94FE\u63A5" }), _jsx(Button, { icon: _jsx(LinkOutlined, {}), onClick: () => window.open(tabletUrl, '_blank', 'noopener,noreferrer'), children: "\u6253\u5F00\u5E73\u677F\u7AEF\u9875\u9762\uFF08\u8C03\u8BD5\uFF09" }), _jsx(Button, { icon: _jsx(QrcodeOutlined, {}), loading: generateQrMutation.isPending, onClick: () => generateQrMutation.mutate(), children: "\u751F\u6210\u4E8C\u7EF4\u7801" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exportQrMutation.isPending, onClick: () => exportQrMutation.mutate(), children: "\u4E8C\u7EF4\u7801\u5BFC\u51FA" })] })] }), _jsxs(Card, { title: _jsxs(Space, { children: [_jsx(ExclamationCircleOutlined, {}), "\u5F02\u5E38\u6982\u89C8"] }), extra: _jsx(Button, { type: "link", onClick: () => setExceptionModalOpen(true), children: "\u67E5\u770B\u5F02\u5E38\u8BB0\u5F55" }), children: [_jsxs("div", { className: "collection-exception-summary", children: [_jsx(Statistic, { title: "\u5F02\u5E38\u4EBA\u6570", value: stats.exceptionCount }), _jsx(Statistic, { title: "\u5F85\u8865\u91C7\u4EBA\u6570", value: stats.pendingRetryCount }), _jsx(Statistic, { title: "\u4FE1\u606F\u5F02\u5E38\u4EBA\u6570", value: stats.infoErrorCount })] }), _jsx(List, { size: "small", loading: exceptionsQuery.isFetching, dataSource: recentExceptions, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u6700\u8FD1\u5F02\u5E38" }) }, renderItem: (item) => (_jsx(List.Item, { children: _jsxs(Space, { direction: "vertical", size: 2, children: [_jsxs(Space, { wrap: true, children: [_jsx(Typography.Text, { strong: true, children: item.candidateName || `考生 ${item.candidateId}` }), statusTag(item.exceptionStatus, item.exceptionStatusLabel, CollectionExceptionStatusText)] }), _jsx(Typography.Text, { type: "secondary", children: item.reason || '-' })] }) })) })] })] })] }), _jsx(Drawer, { title: "\u8003\u751F\u8BE6\u60C5", open: Boolean(detailCandidate), width: 620, destroyOnHidden: true, onClose: () => setDetailCandidate(null), children: detailCandidate ? (_jsxs(Space, { direction: "vertical", size: 16, className: "collection-detail-drawer", children: [_jsxs(Descriptions, { title: "\u57FA\u7840\u4FE1\u606F", column: 1, bordered: true, size: "small", children: [_jsx(Descriptions.Item, { label: "\u8003\u751F ID", children: fallbackText(detail?.candidateId || detailCandidate.candidateId) }), _jsx(Descriptions.Item, { label: "\u59D3\u540D", children: detail?.name || detailCandidate.name || '-' }), _jsx(Descriptions.Item, { label: "\u8003\u53F7", children: detail?.admissionNo || detailCandidate.admissionNo || '-' }), _jsx(Descriptions.Item, { label: "\u8EAB\u4EFD\u8BC1\u53F7", children: detail?.maskedIdCardNo || detailCandidate.maskedIdCardNo || (detailCandidate.idCardLast4 ? `后四位 ${detailCandidate.idCardLast4}` : '-') }), _jsx(Descriptions.Item, { label: "\u5355\u4F4D", children: detail?.unitName || detailCandidate.unitName || '-' }), _jsxs(Descriptions.Item, { label: "\u5DE5\u79CD/\u7B49\u7EA7", children: [detail?.workType || detailCandidate.workType || '-', " / ", detail?.level || detailCandidate.level || '-'] })] }), _jsxs(Descriptions, { title: "\u91C7\u96C6\u72B6\u6001", column: 1, bordered: true, size: "small", children: [_jsx(Descriptions.Item, { label: "\u91C7\u96C6\u72B6\u6001", children: statusTag(detail?.overallStatus || detailCandidate.overallStatus, detail?.overallStatusLabel || detailCandidate.overallStatusLabel, CollectionOverallStatusText) }), _jsx(Descriptions.Item, { label: "\u6307\u7EB9\u72B6\u6001", children: statusTag(detail?.fingerprintStatus || detailCandidate.fingerprintStatus, detail?.fingerprintStatusLabel || detailCandidate.fingerprintStatusLabel, FingerprintStatusText) }), _jsx(Descriptions.Item, { label: "\u7B7E\u540D\u72B6\u6001", children: statusTag(detail?.signatureStatus || detailCandidate.signatureStatus, detail?.signatureStatusLabel || detailCandidate.signatureStatusLabel, SignatureStatusText) }), _jsx(Descriptions.Item, { label: "\u961F\u5217\u72B6\u6001", children: statusTag(detail?.queueStatus || detailCandidate.queueStatus, detail?.queueStatusLabel || detailCandidate.queueStatusLabel, CollectionQueueStatusText) }), _jsx(Descriptions.Item, { label: "\u5F02\u5E38\u72B6\u6001", children: statusTag(detail?.exceptionStatus || detailCandidate.exceptionStatus, detail?.exceptionStatusLabel || detailCandidate.exceptionStatusLabel, CollectionExceptionStatusText) })] }), _jsxs(Descriptions, { title: "\u6307\u7EB9\u4FE1\u606F", column: 1, bordered: true, size: "small", children: [_jsx(Descriptions.Item, { label: "\u6700\u8FD1\u6307\u7EB9", children: detail?.latestFingerprint
                                        ? statusTag(detail.latestFingerprint.collectStatus, detail.latestFingerprint.collectStatusLabel, FingerprintCollectStatusText)
                                        : '-' }), _jsx(Descriptions.Item, { label: "\u8D28\u91CF\u5206", children: fallbackText(detail?.latestFingerprint?.qualityScore) }), _jsx(Descriptions.Item, { label: "\u8BBE\u5907", children: detail?.latestFingerprint?.deviceModel || detail?.latestFingerprint?.deviceSn || '-' }), _jsx(Descriptions.Item, { label: "\u91C7\u96C6\u65F6\u95F4", children: formatDateTime(detail?.latestFingerprint?.collectedAt) }), _jsx(Descriptions.Item, { label: "\u6307\u7EB9\u6587\u4EF6 ID", children: fallbackText(detail?.latestFingerprint?.fingerprintImageFileId) })] }), _jsxs(Descriptions, { title: "\u7B7E\u540D\u4FE1\u606F", column: 1, bordered: true, size: "small", children: [_jsx(Descriptions.Item, { label: "\u7B7E\u540D\u4EBA", children: detail?.latestSignature?.signerName || '-' }), _jsx(Descriptions.Item, { label: "\u7B7E\u540D\u72B6\u6001", children: detail?.latestSignature
                                        ? statusTag(detail.latestSignature.recordStatus, detail.latestSignature.recordStatusLabel, SignatureRecordStatusText)
                                        : '-' }), _jsx(Descriptions.Item, { label: "\u7B7E\u540D\u6587\u4EF6 ID", children: fallbackText(detail?.latestSignature?.signatureFileId) }), _jsx(Descriptions.Item, { label: "\u7B7E\u540D Hash", children: shortHash(detail?.latestSignature?.signatureHash) }), _jsx(Descriptions.Item, { label: "\u7B7E\u540D\u65F6\u95F4", children: formatDateTime(detail?.latestSignature?.signedAt) })] }), _jsxs(Descriptions, { title: "\u65F6\u95F4\u4FE1\u606F", column: 1, bordered: true, size: "small", children: [_jsx(Descriptions.Item, { label: "\u5230\u573A\u65F6\u95F4", children: formatDateTime(detail?.arrivedAt) }), _jsx(Descriptions.Item, { label: "\u5F00\u59CB\u91C7\u96C6\u65F6\u95F4", children: formatDateTime(detail?.collectingStartedAt) }), _jsx(Descriptions.Item, { label: "\u5B8C\u6210\u65F6\u95F4", children: formatDateTime(detail?.completedAt) })] }), _jsx(List, { header: _jsx("strong", { children: "\u5F02\u5E38\u8BB0\u5F55" }), loading: detailQuery.isFetching, dataSource: detail?.exceptions || [], locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u5F02\u5E38\u8BB0\u5F55" }) }, renderItem: (item) => (_jsx(List.Item, { children: _jsxs(Space, { direction: "vertical", size: 2, children: [_jsxs(Space, { wrap: true, children: [statusTag(item.exceptionStatus, item.exceptionStatusLabel, CollectionExceptionStatusText), _jsx(Tag, { children: item.exceptionTypeLabel || item.exceptionType })] }), _jsx(Typography.Text, { type: "secondary", children: item.reason || '-' }), _jsx(Typography.Text, { type: "secondary", children: formatDateTime(item.handledAt || item.createdAt) })] }) })) })] })) : null }), _jsx(Drawer, { className: "collection-candidate-drawer", title: collectionCandidate ? `${collectionCandidate.name} · 考生采集` : '考生采集', open: Boolean(collectionCandidate), width: 960, destroyOnHidden: true, onClose: clearCollectionCandidate, children: collectionCandidate ? (_jsx(CurrentCandidateCollectionPanel, { taskId: taskId, candidate: collectionCandidate, collectionContext: collectionContext, onRefresh: refreshCollectionCandidate, onCancelCurrent: handleCancelCurrent, onClearCurrent: clearCollectionCandidate, onAfterStatusChanged: refreshAll })) : null }), _jsx(InfoErrorModal, { open: Boolean(infoErrorCandidate), loading: markInfoErrorMutation.isPending, onCancel: () => setInfoErrorCandidate(null), onSubmit: (reason) => markInfoErrorMutation.mutate(reason) }), _jsx(ExceptionReleaseModal, { open: Boolean(releaseCandidate), loading: releaseMutation.isPending, onCancel: () => setReleaseCandidate(null), onSubmit: (values) => {
                    if (!releaseCandidate)
                        return;
                    releaseMutation.mutate({
                        taskId,
                        candidateId: releaseCandidate.candidateId,
                        exceptionType: values.exceptionType,
                        releaseScope: 'ALL',
                        reason: values.reason,
                    });
                } }), _jsx(Modal, { title: task ? `${task.taskName} · 异常记录` : '异常记录', open: exceptionModalOpen, width: 920, footer: null, destroyOnHidden: true, onCancel: () => setExceptionModalOpen(false), children: _jsx(CollectionExceptionPage, { taskId: taskId, examSessionId: examSessionId }) })] }));
}

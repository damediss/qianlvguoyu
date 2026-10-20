import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Descriptions, Empty, List, Space, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getCollectionDetail } from '../../../api/certificationPracticalCollection';
export function CollectionDetailPage({ taskId, candidateId }) {
    const detailQuery = useQuery({
        queryKey: ['practical-collection-detail', taskId, candidateId],
        queryFn: () => getCollectionDetail(taskId, candidateId),
        enabled: Boolean(taskId && candidateId),
    });
    const detail = detailQuery.data;
    if (!taskId || !candidateId) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u8003\u751F\u67E5\u770B\u8BE6\u60C5" });
    }
    if (!detail) {
        return _jsx(Empty, { description: detailQuery.isFetching ? '正在加载详情' : '暂无详情' });
    }
    return (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: detail.name }), _jsxs(Typography.Text, { type: "secondary", children: ["\u8EAB\u4EFD\u8BC1\u540E\u56DB\u4F4D\uFF1A", detail.idCardLast4 || '-'] })] }), _jsxs(Descriptions, { bordered: true, column: 2, size: "small", children: [_jsx(Descriptions.Item, { label: "\u5355\u4F4D", children: detail.unitName || '-' }), _jsx(Descriptions.Item, { label: "\u5DE5\u79CD", children: detail.workType || '-' }), _jsx(Descriptions.Item, { label: "\u7B49\u7EA7", children: detail.level || '-' }), _jsx(Descriptions.Item, { label: "\u51C6\u8003\u8BC1\u53F7", children: detail.admissionNo || '-' }), _jsx(Descriptions.Item, { label: "\u6574\u4F53\u72B6\u6001", children: detail.overallStatusLabel || detail.overallStatus }), _jsx(Descriptions.Item, { label: "\u961F\u5217\u72B6\u6001", children: detail.queueStatusLabel || detail.queueStatus }), _jsx(Descriptions.Item, { label: "\u6307\u7EB9\u72B6\u6001", children: detail.fingerprintStatusLabel || detail.fingerprintStatus }), _jsx(Descriptions.Item, { label: "\u7B7E\u540D\u72B6\u6001", children: detail.signatureStatusLabel || detail.signatureStatus })] }), _jsxs(Descriptions, { bordered: true, column: 1, size: "small", title: "\u6700\u65B0\u91C7\u96C6\u8BB0\u5F55", children: [_jsx(Descriptions.Item, { label: "\u6700\u65B0\u6307\u7EB9", children: detail.latestFingerprint
                            ? `${detail.latestFingerprint.collectStatusLabel || detail.latestFingerprint.collectStatus} · ${detail.latestFingerprint.collectedAt}`
                            : '-' }), _jsx(Descriptions.Item, { label: "\u6700\u65B0\u7B7E\u540D", children: detail.latestSignature
                            ? `${detail.latestSignature.signerName} · ${detail.latestSignature.signedAt}`
                            : '-' })] }), _jsx(List, { header: _jsx("strong", { children: "\u5F02\u5E38\u8BB0\u5F55" }), dataSource: detail.exceptions || [], locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u5F02\u5E38\u8BB0\u5F55" }) }, renderItem: (item) => (_jsx(List.Item, { children: _jsxs(Space, { wrap: true, children: [_jsx(Tag, { color: "red", children: item.exceptionTypeLabel || item.exceptionType }), _jsx("span", { children: item.reason }), _jsx("span", { children: item.handledAt || item.createdAt || '-' })] }) })) })] }));
}

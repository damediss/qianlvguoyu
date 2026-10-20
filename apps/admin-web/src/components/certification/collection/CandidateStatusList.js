import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Badge, Empty, List, Tag, Typography } from 'antd';
import { CollectionOverallStatusText } from '../../../types/certificationPracticalCollection';
const groupOrder = [
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
function colorFor(status) {
    if (status === 'COMPLETED')
        return 'success';
    if (status === 'COLLECTING' || status === 'EXCEPTION_RELEASED')
        return 'processing';
    if (status === 'PENDING_RETRY')
        return 'warning';
    if (status === 'INFO_ERROR')
        return 'error';
    return 'default';
}
export function CandidateStatusList({ candidates, onSelect }) {
    const sorted = [...candidates].sort((left, right) => (groupOrder.indexOf(left.overallStatus) - groupOrder.indexOf(right.overallStatus)));
    return (_jsxs("div", { className: "collection-panel candidate-status-list", children: [_jsxs("div", { className: "collection-panel-header", children: [_jsx(Typography.Title, { level: 4, children: "\u8003\u751F\u72B6\u6001" }), _jsx(Badge, { count: candidates.length, color: "#1677ff" })] }), _jsx(List, { dataSource: sorted, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u8003\u751F" }) }, renderItem: (candidate) => (_jsxs(List.Item, { className: "touch-list-item", onClick: () => onSelect(candidate), children: [_jsxs("div", { className: "touch-list-main", children: [_jsx("strong", { children: candidate.name }), _jsxs("span", { children: ["\u540E\u56DB\u4F4D ", candidate.idCardLast4 || '-'] }), _jsx("span", { children: candidate.unitName || '-' })] }), _jsx(Tag, { color: colorFor(candidate.overallStatus), children: candidate.overallStatusLabel || CollectionOverallStatusText[candidate.overallStatus] })] })) })] }));
}

import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Button, Progress, Space, Table, Tag, Tooltip } from 'antd';
import { STATUS_TAG } from './constants';
const LEVEL_TEXT = {
    1: '一级/高级技师', 2: '二级/技师', 3: '三级/高级工', 4: '四级/中级工', 5: '五级/初级工',
};
const statusTag = (b) => {
    const t = STATUS_TAG[b.status] ?? { color: 'default', text: b.status };
    const animCls = b.status === 'pending' ? 'xmb-status-waiting'
        : b.status === 'running' ? 'xmb-status-running' : undefined;
    const tag = _jsx(Tag, { color: t.color, className: animCls, children: t.text });
    // 失败原因挂在状态徽标上（自动补跑耗尽等关键信息不再丢失）
    return b.status === 'failed' && b.failReason
        ? _jsx(Tooltip, { title: b.failReason, overlayStyle: { maxWidth: 420 }, children: tag })
        : tag;
};
const columns = (p) => [
    {
        title: '批次', dataIndex: 'id', key: 'id', width: 140,
        render: (id, b) => {
            const time = b.startedAt ?? b.createdAt ?? b.finishedAt;
            return (_jsxs("div", { style: { lineHeight: 1.4 }, children: [_jsx("div", { children: time ? new Date(time).toLocaleString('zh-CN', { hour12: false }).slice(5, 16) : '-' }), _jsxs("div", { style: { color: '#999', fontSize: 12 }, children: ["\u6279\u6B21\u2026", id.slice(-8)] })] }));
        },
    },
    {
        title: '工种/等级', key: 'workType', width: 190, ellipsis: true,
        render: (_, b) => `${p.workTypeName(b.workTypeId)} · ${b.level ? LEVEL_TEXT[b.level] ?? `${b.level}级` : '-'}`,
    },
    { title: '模型', dataIndex: 'modelId', key: 'modelId', width: 120, ellipsis: true },
    { title: '状态', key: 'status', width: 100, render: (_, b) => statusTag(b) },
    {
        title: '生成进度', key: 'progress', width: 220,
        render: (_, b) => {
            const pct = b.totalPoints > 0 ? Math.floor((b.donePoints / b.totalPoints) * 100) : 0;
            const live = b.status === 'running' || b.status === 'pending';
            return (_jsxs("div", { style: { lineHeight: 1.5 }, children: [_jsx(Progress, { percent: pct, size: "small", status: live ? 'active' : 'normal', format: () => `${b.donePoints}/${b.totalPoints} 点` }), _jsxs("span", { style: { fontSize: 12, color: '#666' }, children: ["\u5DF2\u4EA7\u9898 ", b.totalQuestions, " \u9898", (b.subjectiveCount ?? 0) > 0 ? `（主观 ${b.subjectiveCount}）` : ''] })] }));
        },
    },
    {
        title: '审核进度', key: 'review', width: 190,
        render: (_, b) => (_jsxs("span", { style: { fontSize: 12 }, children: [_jsxs("span", { style: { color: '#389e0d' }, children: ["\u901A\u8FC7 ", b.approvedCount ?? 0] }), ' · ', _jsxs("span", { style: { color: '#d46b08' }, children: ["\u5F85\u5BA1 ", b.pendingReviewCount ?? 0] }), ' · ', _jsxs("span", { style: { color: '#cf1322' }, children: ["\u9A73\u56DE ", b.rejectedCount ?? 0] })] })),
    },
    {
        title: '入库', key: 'publish', width: 80,
        render: (_, b) => ((b.publishedCount ?? 0) > 0
            ? _jsxs(Tag, { color: "success", children: ["\u5DF2\u5165\u5E93 ", b.publishedCount] })
            : _jsx(Tag, { children: "\u672A\u5165\u5E93" })),
    },
    {
        title: '操作', key: 'op', width: 290, fixed: 'right',
        render: (_, b) => (_jsxs(Space, { size: 4, wrap: true, children: [_jsx(Button, { size: "small", type: "primary", onClick: () => p.onOpenDraft(b), children: "\u8349\u7A3F" }), b.status === 'running' && (_jsx(Button, { size: "small", onClick: () => p.onAction('pause', b), children: "\u6682\u505C" })), b.status === 'paused' && (_jsx(Button, { size: "small", onClick: () => p.onAction('resume', b), children: "\u7EE7\u7EED" })), b.status === 'failed' && (_jsx(Button, { size: "small", onClick: () => p.onAction('retry', b), children: "\u91CD\u8BD5\u5931\u8D25" })), b.status === 'done' && b.donePoints < b.totalPoints && (_jsx(Button, { size: "small", onClick: () => p.onAction('retry', b), children: "\u8865\u8DD1\u7F3A\u53E3" })), _jsx(Button, { size: "small", loading: p.publishingBatchId === b.id, onClick: () => p.onAction('publish', b), children: "\u5165\u5E93" }), _jsx(Button, { size: "small", loading: p.exportingBatchId === b.id, onClick: () => p.onAction('export', b), children: "\u5BFC\u51FA\u8BD5\u9898\u8868" })] })),
    },
];
const GenBatchOverviewTable = (props) => {
    const { activeItems, historyItems, historyLoading, historyPage, historyPageSize, historyTotal, onHistoryPageChange, flashBatchId } = props;
    const rowCls = (b) => (flashBatchId === b.id ? 'gen-row-flash' : '');
    return (_jsxs(_Fragment, { children: [_jsxs("div", { className: "gen-section-title", children: ["\u8FDB\u884C\u4E2D\u6279\u6B21", _jsx("span", { style: { color: '#888', fontWeight: 400, fontSize: 12 }, children: "\uFF08\u7B49\u5F85/\u751F\u6210\u4E2D\u7684\u6279\u6B21\u7F6E\u9876\u663E\u793A\uFF0C\u8FDB\u5EA6\u5B9E\u65F6\u5237\u65B0\uFF09" })] }), _jsx(Table, { rowKey: "id", size: "small", rowClassName: rowCls, dataSource: activeItems, columns: columns(props), pagination: false, locale: { emptyText: '当前没有进行中的批次' }, scroll: { x: 1280 } }), _jsx("div", { className: "gen-section-title", style: { marginTop: 16 }, children: "\u5386\u53F2\u6279\u6B21" }), _jsx(Table, { rowKey: "id", size: "small", rowClassName: rowCls, loading: historyLoading, dataSource: historyItems, columns: columns(props), scroll: { x: 1280 }, pagination: {
                    current: historyPage,
                    pageSize: historyPageSize,
                    total: historyTotal,
                    showSizeChanger: true,
                    pageSizeOptions: [10, 20, 50],
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: (p, s) => onHistoryPageChange(p, s),
                } })] }));
};
export default GenBatchOverviewTable;

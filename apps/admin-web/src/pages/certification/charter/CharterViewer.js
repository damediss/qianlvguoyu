import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Card, Descriptions, Empty, Space, Tag, Typography } from 'antd';
function safeParse(document) {
    const raw = document?.contentJson || document?.contentText;
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw);
    }
    catch {
        return null;
    }
}
function statusTag(value) {
    const label = value === 'running' ? '进行中' : value === 'pending_review' ? '待审核' : value === 'completed' ? '已完成' : value || '-';
    const color = value === 'running' ? 'processing' : value === 'completed' ? 'success' : value === 'pending_review' ? 'warning' : 'default';
    return _jsx(Tag, { color: color, children: label });
}
export function CharterViewer({ task, document, }) {
    const draft = safeParse(document);
    if (!task) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u4EFB\u52A1\u540E\u67E5\u770B\u4EFB\u52A1\u7B56\u5212\u4EFB\u52A1\u4E66" });
    }
    if (!draft) {
        return (_jsx(Card, { children: _jsx(Empty, { description: "\u6682\u65E0\u4EFB\u52A1\u7B56\u5212\u4EFB\u52A1\u4E66\u5185\u5BB9\uFF08\u53EF\u70B9\u51FB\u201C\u7F16\u8F91\u201D\u5F00\u59CB\u586B\u5199\uFF09" }) }));
    }
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(Card, { styles: { body: { paddingBottom: 8 } }, title: "\u57FA\u672C\u4FE1\u606F", size: "small", children: _jsxs(Descriptions, { column: 3, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u540D\u79F0", children: draft.taskName || task.taskName }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7F16\u53F7", children: draft.taskNo || task.taskNo }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7ECF\u7406", children: draft.taskManagerName || task.taskManagerName }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u72B6\u6001", children: statusTag(draft.taskStatus || task.status) }), _jsxs(Descriptions.Item, { label: "\u7AE0\u7A0B\u7248\u672C", children: ["V", document?.versionNo || 1] }), _jsx(Descriptions.Item, { label: "\u6700\u540E\u4FDD\u5B58", children: document?.lastSavedAt || '-' })] }) }), _jsx(Card, { title: "\u4EFB\u52A1\u80CC\u666F\u4E0E\u76EE\u7684", size: "small", children: _jsx(Typography.Paragraph, { style: { marginBottom: 0, whiteSpace: 'pre-wrap' }, children: draft.background || '-' }) }), _jsx(Card, { title: "\u4EFB\u52A1\u603B\u4F53\u76EE\u6807", size: "small", children: _jsxs(Descriptions, { column: 1, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u8D28\u91CF\u76EE\u6807", children: draft.goals?.quality || '-' }), _jsx(Descriptions.Item, { label: "\u5DE5\u671F\u76EE\u6807", children: draft.goals?.schedule || '-' }), _jsx(Descriptions.Item, { label: "\u8D39\u7528\u76EE\u6807", children: draft.goals?.cost || '-' }), _jsx(Descriptions.Item, { label: "\u4EA4\u4ED8\u76EE\u6807", children: draft.goals?.delivery || '-' })] }) }), _jsx(Card, { title: "\u8282\u70B9\u76EE\u6807", size: "small", children: _jsx(Space, { direction: "vertical", style: { width: '100%' }, size: 8, children: (draft.milestones || []).map((m, idx) => (_jsxs("div", { style: {
                            border: '1px solid #e5e7eb',
                            borderRadius: 8,
                            padding: 12,
                            background: m.enabled ? '#f6ffed' : '#f8fafc',
                        }, children: [_jsxs(Space, { align: "baseline", size: 10, style: { width: '100%', justifyContent: 'space-between' }, children: [_jsxs(Typography.Text, { strong: true, children: [idx + 1, ". ", m.title || '-'] }), _jsx(Tag, { color: m.enabled ? 'success' : 'default', children: m.enabled ? '启用' : '未启用' })] }), m.desc ? (_jsx(Typography.Paragraph, { style: { margin: '6px 0 0', whiteSpace: 'pre-wrap' }, children: m.desc })) : null] }, `${idx}-${m.title}`))) }) })] }));
}

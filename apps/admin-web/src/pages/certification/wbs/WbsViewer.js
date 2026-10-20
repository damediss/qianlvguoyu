import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EditOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, Empty, Select, Typography } from 'antd';
import { useMemo, useState } from 'react';
import { PermissionGate } from '../../../directives/permission';
import { formatEstimateHours, formatManpowerInput, resolveCategoryName, summarizeWbsItems, wbsCategoryOptions, } from './constants';
const colors = {
    primary: '#1890ff',
    primaryStrong: '#096dd9',
    primarySoft: '#e6f7ff',
    success: '#52c41a',
    successSoft: '#f6ffed',
    text: '#1f2937',
    textMuted: '#6b7280',
    border: '#e5e7eb',
};
const containerStyle = {
    maxWidth: 1200,
    margin: '0 auto',
    background: '#fff',
    borderRadius: 12,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    overflow: 'hidden',
    border: '1px solid #eef2f7',
};
const headerStyle = {
    background: `linear-gradient(135deg, ${colors.primary} 0%, ${colors.primaryStrong} 100%)`,
    color: '#fff',
    padding: 32,
};
const metaSectionStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(5, 1fr)',
    borderBottom: `1px solid ${colors.border}`,
};
const metaItemStyle = {
    padding: '20px 24px',
    borderRight: `1px solid ${colors.border}`,
};
const toolbarStyle = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    flexWrap: 'wrap',
    padding: '20px 32px',
    background: '#f8fafc',
    borderBottom: `1px solid ${colors.border}`,
};
const bodyStyle = {
    padding: 32,
};
const kpiGridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
    gap: 20,
    marginBottom: 32,
};
const kpiCardStyle = {
    background: '#f8fafc',
    borderRadius: 10,
    padding: 24,
    textAlign: 'center',
    border: `1px solid ${colors.border}`,
};
const filterSectionStyle = {
    background: '#f8fafc',
    borderRadius: 10,
    padding: 20,
    marginBottom: 24,
};
const tableSectionStyle = {
    background: '#fff',
    borderRadius: 10,
    border: `1px solid ${colors.border}`,
    overflow: 'hidden',
};
const tableHeaderStyle = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '20px 24px',
    borderBottom: `1px solid ${colors.border}`,
};
function dash(value) {
    if (value === null || value === undefined || value === '') {
        return '-';
    }
    return String(value);
}
function documentDraftLabel(document) {
    if (document?.status === 'submitted') {
        return '已提交';
    }
    if (document?.status === 'approved') {
        return '已通过';
    }
    if (document?.status === 'rejected') {
        return '已驳回';
    }
    if (document?.lastSavedAt) {
        return `已保存 ${document.lastSavedAt}`;
    }
    return '未保存草稿';
}
function taskStatusLabel(task) {
    if (task?.statusName) {
        return task.statusName;
    }
    const map = {
        in_progress: '进行中',
        pending_review: '待审核',
        rejected: '已驳回',
        completed: '已完成',
    };
    return task?.status ? map[task.status] || task.status : '-';
}
function CategoryBadge({ name }) {
    const label = name || '-';
    const style = {
        display: 'inline-block',
        padding: '4px 10px',
        borderRadius: 4,
        fontSize: 12,
        fontWeight: 500,
    };
    if (label === '规划') {
        return _jsx("span", { style: { ...style, background: colors.primarySoft, color: colors.primary }, children: label });
    }
    if (label === '执行') {
        return _jsx("span", { style: { ...style, background: colors.successSoft, color: colors.success }, children: label });
    }
    if (label === '审核') {
        return _jsx("span", { style: { ...style, background: '#f3e8ff', color: '#9333ea' }, children: label });
    }
    return _jsx("span", { style: { ...style, background: '#f3f4f6', color: colors.textMuted }, children: label });
}
export function WbsViewer({ task, document, items, lockedForEdit, saveLoading, onEdit, onSaveDraft, }) {
    const [categoryFilter, setCategoryFilter] = useState('all');
    const filteredItems = useMemo(() => {
        if (categoryFilter === 'all') {
            return items;
        }
        return items.filter((item) => resolveCategoryName(item.categoryName, item.categoryCode) === categoryFilter);
    }, [categoryFilter, items]);
    const summary = useMemo(() => summarizeWbsItems(filteredItems), [filteredItems]);
    if (!task) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u4EFB\u52A1\u540E\u67E5\u770B\u4EFB\u52A1 WBS" });
    }
    return (_jsxs("div", { style: containerStyle, children: [_jsxs("div", { style: headerStyle, children: [_jsx(Typography.Title, { level: 2, style: { margin: 0, color: '#fff', fontSize: 24 }, children: "\u4EFB\u52A1 WBS \u8868" }), _jsx(Typography.Paragraph, { style: { margin: '8px 0 0', color: 'rgba(255,255,255,0.9)', fontSize: 14 }, children: "\u5F53\u524D\u4E3A\u67E5\u770B\u6001\u9875\u9762\uFF0C\u7528\u4E8E\u67E5\u770B\u4EFB\u52A1\u5206\u89E3\u3001\u5DE5\u65F6\u3001\u8D44\u6E90\u548C\u8D39\u7528\u4F30\u7B97\uFF0C\u5E76\u53EF\u8FDB\u5165\u7F16\u8F91\u9875\u9762\u7EE7\u7EED\u8C03\u6574\u4EFB\u52A1\u5185\u5BB9" })] }), _jsx("div", { style: metaSectionStyle, children: [
                    { label: '任务名称', value: task.taskName },
                    { label: '任务编号', value: task.taskNo },
                    { label: '任务经理', value: task.taskManagerName },
                    { label: '当前状态', value: taskStatusLabel(task) },
                    { label: '草稿状态', value: documentDraftLabel(document) },
                ].map((item, index, arr) => (_jsxs("div", { style: {
                        ...metaItemStyle,
                        borderRight: index === arr.length - 1 ? 'none' : metaItemStyle.borderRight,
                    }, children: [_jsx("span", { style: { display: 'block', fontSize: 12, color: colors.textMuted, marginBottom: 6 }, children: item.label }), _jsx("strong", { style: { fontSize: 15, color: colors.text, fontWeight: 600 }, children: dash(item.value) })] }, item.label))) }), _jsx("div", { style: toolbarStyle, children: _jsxs("div", { style: { display: 'flex', gap: 12, flexWrap: 'wrap' }, children: [onEdit ? (_jsx(PermissionGate, { code: "certification.project.wbs.edit", children: _jsx(Button, { type: "primary", icon: _jsx(EditOutlined, {}), disabled: lockedForEdit, onClick: onEdit, children: "\u7F16\u8F91\u8868\u5355" }) })) : null, onSaveDraft ? (_jsx(PermissionGate, { code: "certification.project.wbs.edit", children: _jsx(Button, { icon: _jsx(SaveOutlined, {}), disabled: lockedForEdit, loading: saveLoading, onClick: onSaveDraft, children: "\u4FDD\u5B58\u8349\u7A3F" }) })) : null] }) }), _jsxs("div", { style: bodyStyle, children: [_jsxs("div", { style: kpiGridStyle, children: [_jsxs("div", { style: kpiCardStyle, children: [_jsx("div", { style: { fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }, children: summary.count }), _jsx("div", { style: { fontSize: 13, color: colors.textMuted }, children: "\u5F53\u524D\u7B5B\u9009\u4EFB\u52A1\u6570" })] }), _jsxs("div", { style: kpiCardStyle, children: [_jsxs("div", { style: { fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }, children: [summary.totalHours, "h"] }), _jsx("div", { style: { fontSize: 13, color: colors.textMuted }, children: "\u9884\u8BA1\u603B\u5DE5\u65F6" })] }), _jsxs("div", { style: kpiCardStyle, children: [_jsxs("div", { style: { fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }, children: [summary.totalDays, "\u5929"] }), _jsx("div", { style: { fontSize: 13, color: colors.textMuted }, children: "\u9884\u8BA1\u603B\u5DE5\u671F" })] }), _jsxs("div", { style: kpiCardStyle, children: [_jsxs("div", { style: { fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }, children: ["\uFFE5", summary.totalCost] }), _jsx("div", { style: { fontSize: 13, color: colors.textMuted }, children: "\u8D39\u7528\u4F30\u7B97" })] })] }), _jsx("div", { style: filterSectionStyle, children: _jsxs("div", { style: { display: 'flex', flexDirection: 'column', gap: 6, maxWidth: 220 }, children: [_jsx("label", { style: { fontSize: 13, color: colors.textMuted }, children: "\u6309\u4EFB\u52A1\u5206\u7C7B\u7B5B\u9009" }), _jsx(Select, { value: categoryFilter, onChange: setCategoryFilter, options: [
                                        { label: '全部', value: 'all' },
                                        ...wbsCategoryOptions.map((item) => ({ label: item.label, value: item.name })),
                                    ] })] }) }), _jsxs("div", { style: tableSectionStyle, children: [_jsxs("div", { style: tableHeaderStyle, children: [_jsx(Typography.Title, { level: 5, style: { margin: 0, fontSize: 16, color: colors.text }, children: "\u5DE5\u4F5C\u5206\u89E3\u7ED3\u6784" }), _jsx("span", { style: { color: colors.textMuted, fontSize: 14 }, children: "\u652F\u6301\u6309\u4EFB\u52A1\u5206\u7C7B\u7B5B\u9009" })] }), _jsx("div", { style: { overflowX: 'auto' }, children: _jsxs("table", { style: { width: '100%', borderCollapse: 'collapse', fontSize: 14 }, children: [_jsx("thead", { children: _jsx("tr", { children: ['序号', '任务分类', '任务名称', '包含活动', '工时估算', '人力投入', '其他资源投入', '费用估算(元)', '工期（天）'].map((title) => (_jsx("th", { style: {
                                                        background: '#f8fafc',
                                                        padding: '14px 16px',
                                                        textAlign: 'left',
                                                        fontWeight: 600,
                                                        color: colors.text,
                                                        borderBottom: `1px solid ${colors.border}`,
                                                        whiteSpace: 'nowrap',
                                                    }, children: title }, title))) }) }), _jsx("tbody", { children: filteredItems.length === 0 ? (_jsx("tr", { children: _jsx("td", { colSpan: 9, style: { padding: 32, textAlign: 'center', color: colors.textMuted }, children: "\u6682\u65E0 WBS \u6570\u636E\uFF0C\u53EF\u70B9\u51FB\u300C\u7F16\u8F91\u8868\u5355\u300D\u5F00\u59CB\u586B\u5199" }) })) : (filteredItems.map((row, index) => (_jsxs("tr", { children: [_jsx("td", { style: cellStyle, children: index + 1 }), _jsx("td", { style: cellStyle, children: _jsx(CategoryBadge, { name: resolveCategoryName(row.categoryName, row.categoryCode) }) }), _jsx("td", { style: { ...cellStyle, color: colors.text }, children: dash(row.itemName) }), _jsx("td", { style: cellStyle, children: dash(row.activityDescription) }), _jsx("td", { style: cellStyle, children: formatEstimateHours(row.estimateHours) }), _jsx("td", { style: cellStyle, children: formatManpowerInput(row) }), _jsx("td", { style: cellStyle, children: dash(row.resourceInput) }), _jsx("td", { style: cellStyle, children: row.costAmount == null ? '-' : row.costAmount }), _jsx("td", { style: cellStyle, children: row.durationDays == null ? '-' : row.durationDays })] }, row.id || `${row.wbsCode}-${index}`)))) })] }) })] })] })] }));
}
const cellStyle = {
    padding: '14px 16px',
    borderBottom: `1px solid ${colors.border}`,
    color: colors.textMuted,
};

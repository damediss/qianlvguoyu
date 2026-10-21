import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EditOutlined } from '@ant-design/icons';
import { Button, Card, Checkbox, Empty, Progress, Space, Table, Tag, Typography } from 'antd';
import { PermissionGate } from '../../../directives/permission';
import { buildExamInfoFromTask, certificationTypeLabel, formatDayLabel, formatPreparationDays, memberNameById, scheduleStatusLabel, summarizeExamPeopleCount, summarizeScheduleItems, } from './constants';
const containerStyle = {
    maxWidth: 1200,
    margin: '0 auto',
    borderRadius: 12,
    overflow: 'hidden',
    background: '#fff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    border: '1px solid #eef2f7',
};
const headerStyle = {
    padding: 32,
    color: '#fff',
    background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
};
const metaGridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(5, minmax(0, 1fr))',
    borderBottom: '1px solid #e5e7eb',
};
const metaItemStyle = {
    padding: '20px 24px',
    borderRight: '1px solid #e5e7eb',
};
const bodyStyle = {
    padding: 32,
};
function draftLabel(document) {
    if (!document?.lastSavedAt) {
        return '未保存草稿';
    }
    if (document.status === 'submitted') {
        return '已提交审核';
    }
    if (document.status === 'approved') {
        return '审核已通过';
    }
    return '已保存草稿';
}
export function ScheduleViewer({ task, document, examInfos, items, members, lockedForEdit, onEdit, onToggleCompleted, }) {
    if (!task) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u4EFB\u52A1\u540E\u67E5\u770B\u8FDB\u5EA6\u8BA1\u5212" });
    }
    const rows = examInfos && examInfos.length > 0 ? examInfos : buildExamInfoFromTask(task);
    const summary = summarizeScheduleItems(items);
    const prepDays = formatPreparationDays(task.examDate, task.planStartDate);
    const externalId = items[0]?.externalContactMemberId;
    const ownerId = items[0]?.certificationOwnerMemberId;
    const primaryExam = rows[0];
    const totalPeople = summarizeExamPeopleCount(rows);
    const columns = [
        { title: '认定状态', width: 100, render: () => scheduleStatusLabel(task.status, task.statusName) },
        { title: '认定类型', width: 180, render: (_, row) => certificationTypeLabel(row.certificationType) },
        { title: '任务名称', width: 180, render: () => task.taskName },
        { title: '工种', dataIndex: 'workTypeName', width: 90, render: (value) => value || '-' },
        { title: '等级', dataIndex: 'levelName', width: 80, render: (value) => value || '-' },
        { title: '认定人数', dataIndex: 'plannedPeopleCount', width: 90, render: (value) => value ?? '-' },
        { title: '考试日期', width: 110, render: () => task.examDate || '-' },
        { title: '启动日期', width: 110, render: () => task.planStartDate || '-' },
        { title: '时长', width: 80, render: () => prepDays },
        { title: '对外联系人', width: 110, render: () => memberNameById(members, externalId) },
        { title: '认定负责人', width: 110, render: () => memberNameById(members, ownerId) },
    ];
    const toggleItem = (index, completed) => {
        if (!onToggleCompleted || lockedForEdit) {
            return;
        }
        onToggleCompleted(items.map((row, i) => (i === index ? { ...row, completed } : row)));
    };
    return (_jsxs("div", { style: containerStyle, children: [_jsxs("div", { style: headerStyle, children: [_jsx(Typography.Title, { level: 2, style: { margin: 0, color: '#fff' }, children: "\u4EFB\u52A1\u8FDB\u5EA6\u8BA1\u5212\u8868" }), _jsx(Typography.Paragraph, { style: { margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }, children: "\u5F53\u524D\u4E3A\u67E5\u770B\u6001\u9875\u9762\uFF0C\u7528\u4E8E\u67E5\u770B\u8003\u8BD5\u65E5\u671F\u3001\u542F\u52A8\u65E5\u671F\u3001\u51C6\u5907\u65F6\u95F4\u4E0E\u5173\u952E\u5B89\u6392\uFF0C\u5E76\u53EF\u8FDB\u5165\u7F16\u8F91\u9875\u8C03\u6574\u65F6\u95F4\u8BA1\u5212" })] }), _jsx("div", { style: metaGridStyle, children: [
                    { label: '任务名称', value: task.taskName },
                    { label: '任务编号', value: task.taskNo },
                    { label: '任务经理', value: task.taskManagerName },
                    { label: '当前状态', value: scheduleStatusLabel(task.status, task.statusName) },
                    { label: '草稿状态', value: draftLabel(document) },
                ].map((item, index, list) => (_jsxs("div", { style: { ...metaItemStyle, borderRight: index === list.length - 1 ? 'none' : metaItemStyle.borderRight }, children: [_jsx(Typography.Text, { type: "secondary", style: { display: 'block', fontSize: 12, marginBottom: 6 }, children: item.label }), _jsx(Typography.Text, { strong: true, children: item.value })] }, item.label))) }), _jsxs("div", { style: bodyStyle, children: [_jsxs("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', gap: 16, marginBottom: 24 }, children: [_jsxs(Card, { size: "small", style: { textAlign: 'center' }, children: [_jsxs("div", { style: { fontSize: 22, fontWeight: 700, color: '#1890ff' }, children: [certificationTypeLabel(primaryExam?.certificationType || task.certificationType), rows.length > 1 ? ` 等${rows.length}项` : ''] }), _jsx(Typography.Text, { type: "secondary", children: "\u8BA4\u5B9A\u7C7B\u578B" })] }), _jsxs(Card, { size: "small", style: { textAlign: 'center' }, children: [_jsx("div", { style: { fontSize: 22, fontWeight: 700, color: '#1890ff' }, children: task.examDate || '-' }), _jsx(Typography.Text, { type: "secondary", children: "\u8003\u8BD5\u65E5\u671F" })] }), _jsxs(Card, { size: "small", style: { textAlign: 'center' }, children: [_jsx("div", { style: { fontSize: 22, fontWeight: 700, color: '#1890ff' }, children: task.planStartDate || '-' }), _jsx(Typography.Text, { type: "secondary", children: "\u542F\u52A8\u65E5\u671F" })] }), _jsxs(Card, { size: "small", style: { textAlign: 'center' }, children: [_jsx("div", { style: { fontSize: 22, fontWeight: 700, color: '#1890ff' }, children: prepDays }), _jsx(Typography.Text, { type: "secondary", children: "\u51C6\u5907\u65F6\u95F4" })] })] }), _jsxs(Card, { size: "small", style: { marginBottom: 24, background: '#f8fafc' }, children: [_jsxs(Space, { style: { width: '100%', justifyContent: 'space-between', marginBottom: 8 }, children: [_jsx(Typography.Text, { strong: true, children: "\u51C6\u5907\u4E8B\u9879\u5B8C\u6210\u60C5\u51B5" }), _jsxs(Typography.Text, { type: "secondary", children: [summary.completed, "/", summary.total, " \u51C6\u5907\u4E8B\u9879\u5DF2\u5B8C\u6210"] })] }), _jsx(Progress, { percent: summary.percent, showInfo: false, strokeColor: { from: '#1890ff', to: '#60a5fa' } })] }), _jsx(Card, { size: "small", title: "\u8FDB\u5EA6\u603B\u8868", extra: _jsx(Typography.Text, { type: "secondary", children: "\u5B57\u6BB5\u6765\u6E90\uFF1A\u8BA4\u5B9A\u72B6\u6001 / \u8BA4\u5B9A\u7C7B\u578B / \u4EFB\u52A1\u540D\u79F0 / \u8003\u8BD5\u65E5\u671F / \u542F\u52A8\u65E5\u671F / \u65F6\u957F / \u51C6\u5907\u65F6\u95F4" }), style: { marginBottom: 24 }, children: _jsx(Table, { rowKey: (row, index) => row.id || `exam-summary-${index}`, size: "small", pagination: false, dataSource: rows, locale: { emptyText: '暂无考试信息' }, columns: columns }) }), rows.length > 1 ? (_jsx(Card, { size: "small", style: { marginBottom: 24 }, children: _jsxs(Typography.Text, { type: "secondary", children: ["\u5408\u8BA1\u8BA4\u5B9A\u4EBA\u6570\uFF1A", totalPeople] }) })) : null, _jsx(Card, { size: "small", title: "\u51C6\u5907\u671F\u5173\u952E\u5B89\u6392", extra: _jsx(Typography.Text, { type: "secondary", children: "\u652F\u6301\u52FE\u9009\u5B8C\u6210\u60C5\u51B5\u5E76\u56DE\u5199\u5DE5\u4F5C\u53F0" }), children: _jsx(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: items.length === 0 ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u5173\u952E\u5B89\u6392\uFF0C\u8BF7\u70B9\u51FB\u300C\u7F16\u8F91\u8868\u5355\u300D\u8865\u5145" })) : (items.map((item, index) => {
                                const checked = Boolean(item.completed);
                                return (_jsxs("label", { style: {
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 12,
                                        padding: 16,
                                        borderRadius: 8,
                                        border: `1px solid ${checked ? '#b7eb8f' : '#e5e7eb'}`,
                                        background: checked ? '#f6ffed' : '#f8fafc',
                                        cursor: lockedForEdit || !onToggleCompleted ? 'default' : 'pointer',
                                    }, children: [_jsx(Checkbox, { checked: checked, disabled: lockedForEdit || !onToggleCompleted, onChange: (event) => toggleItem(index, event.target.checked) }), _jsxs("span", { style: { flex: 1 }, children: [_jsx(Tag, { color: "blue", style: { marginRight: 8 }, children: formatDayLabel(item) }), item.itemName] })] }, item.id || `schedule-${index}`));
                            })) }) }), onEdit ? (_jsx("div", { style: { marginTop: 20 }, children: _jsx(PermissionGate, { code: "certification.project.schedule.edit", children: _jsx(Button, { icon: _jsx(EditOutlined, {}), disabled: lockedForEdit, onClick: onEdit, children: "\u7F16\u8F91\u8868\u5355" }) }) })) : null] })] }));
}

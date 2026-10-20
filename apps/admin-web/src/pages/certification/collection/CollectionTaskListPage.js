import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DownloadOutlined, ExclamationCircleOutlined, EyeOutlined, PlayCircleOutlined, QrcodeOutlined, ReloadOutlined, SearchOutlined, ToolOutlined, } from '@ant-design/icons';
import { Alert, Button, DatePicker, Descriptions, Drawer, Empty, Input, Modal, Select, Space, Table, Tag, Tooltip, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { exportQrCodes, generateQrCodes, getCollectionTasks, initializeCollection, } from '../../../api/certificationPracticalCollection';
import { PermissionGate } from '../../../directives/permission';
import { formatCollectionTaskStatus, pendingCandidateCount, resolveCollectionTaskStatus, } from '../../../utils/collectionTask';
import { CollectionExceptionPage } from './CollectionExceptionPage';
import './CollectionTaskListPage.css';
const { RangePicker } = DatePicker;
const statusOptions = [
    { label: '全部', value: 'all' },
    { label: '待采集', value: 'pending' },
    { label: '采集中', value: 'collecting' },
    { label: '已完成', value: 'completed' },
    { label: '有异常', value: 'exception' },
];
function toNumber(value) {
    const numberValue = Number(value || 0);
    return Number.isFinite(numberValue) ? numberValue : 0;
}
function pendingCount(task) {
    return pendingCandidateCount(task);
}
function taskStatus(task) {
    return resolveCollectionTaskStatus(task);
}
function statusTag(task) {
    const status = formatCollectionTaskStatus(task);
    return _jsx(Tag, { color: status.color, children: status.label });
}
function taskManageUrl(task) {
    const baseUrl = `/certification/practical-collection/${encodeURIComponent(String(task.taskId))}/manage`;
    if (!task.examSessionId) {
        return baseUrl;
    }
    return `${baseUrl}?examSessionId=${encodeURIComponent(String(task.examSessionId))}`;
}
function openManagePage(task) {
    window.location.assign(taskManageUrl(task));
}
function taskDateInRange(task, dateRange) {
    if (!dateRange || !dateRange[0] || !dateRange[1]) {
        return true;
    }
    if (!task.examDate) {
        return false;
    }
    const examDate = String(task.examDate);
    return examDate >= dateRange[0] && examDate <= dateRange[1];
}
export function CollectionTaskListPage() {
    const queryClient = useQueryClient();
    const [keyword, setKeyword] = useState('');
    const [statusFilter, setStatusFilter] = useState('all');
    const [dateRange, setDateRange] = useState(null);
    const [selectedTask, setSelectedTask] = useState(null);
    const [exceptionTask, setExceptionTask] = useState(null);
    const tasksQuery = useQuery({
        queryKey: ['practical-collection-tasks', keyword],
        queryFn: () => getCollectionTasks({ page: 1, pageSize: 100, keyword: keyword || undefined }),
    });
    const invalidateTasks = () => queryClient.invalidateQueries({ queryKey: ['practical-collection-tasks'] });
    const initializeMutation = useMutation({
        mutationFn: (task) => initializeCollection(task.taskId, task.examSessionId),
        onSuccess: (result) => {
            message.success(result.message || '采集状态已初始化');
            invalidateTasks();
        },
    });
    const generateQrMutation = useMutation({
        mutationFn: (task) => generateQrCodes(task.taskId, task.examSessionId),
        onSuccess: (result) => {
            message.success(result.message || '二维码 token 已生成');
            invalidateTasks();
        },
    });
    const exportMutation = useMutation({
        mutationFn: (task) => exportQrCodes(task.taskId, task.examSessionId),
        onSuccess: (result) => {
            const messageText = typeof result === 'object' && result && 'message' in result
                ? String(result.message)
                : '二维码导出能力后续完善';
            message.info(messageText);
        },
    });
    const tasks = tasksQuery.data?.items || [];
    const filteredTasks = useMemo(() => tasks.filter((task) => {
        if (statusFilter !== 'all' && taskStatus(task) !== statusFilter) {
            return false;
        }
        return taskDateInRange(task, dateRange);
    }), [dateRange, statusFilter, tasks]);
    const columns = [
        {
            title: '任务名称',
            dataIndex: 'taskName',
            width: 200,
            fixed: 'left',
            render: (value) => _jsx(Typography.Text, { strong: true, children: value || '-' }),
        },
        {
            title: '场次名称',
            dataIndex: 'sessionName',
            width: 180,
            render: (value) => _jsx(Typography.Text, { strong: true, children: value || '-' }),
        },
        {
            title: '任务编号',
            dataIndex: 'taskNo',
            width: 150,
            render: (value) => value || '-',
        },
        {
            title: '企业',
            dataIndex: 'projectName',
            width: 180,
            render: (value) => value || '-',
        },
        {
            title: '工种/等级',
            width: 160,
            render: (_, task) => task.workTypeLevel || `${task.workType || '-'} / ${task.level || '-'}`,
        },
        {
            title: '认定日期',
            dataIndex: 'examDate',
            width: 120,
            sorter: (a, b) => {
                const left = a.examDate || '';
                const right = b.examDate || '';
                if (!left && !right) {
                    return 0;
                }
                if (!left) {
                    return 1;
                }
                if (!right) {
                    return -1;
                }
                return left.localeCompare(right);
            },
            render: (value) => value || '-',
        },
        {
            title: '总人数',
            dataIndex: 'candidateTotal',
            width: 72,
            align: 'right',
            render: toNumber,
        },
        {
            title: '已完成',
            dataIndex: 'completedCount',
            width: 72,
            align: 'right',
            render: toNumber,
        },
        {
            title: '待采集',
            width: 72,
            align: 'right',
            render: (_, task) => pendingCount(task),
        },
        {
            title: '异常',
            dataIndex: 'exceptionCount',
            width: 72,
            align: 'right',
            render: (value) => {
                const count = toNumber(value);
                return count > 0 ? _jsx(Typography.Text, { type: "danger", children: count }) : count;
            },
        },
        {
            title: '状态',
            width: 92,
            render: (_, task) => statusTag(task),
        },
        {
            title: '操作',
            width: 104,
            fixed: 'right',
            render: (_, task) => (_jsxs(Space, { size: 4, children: [_jsx(Tooltip, { title: "\u91C7\u96C6\u7BA1\u7406", children: _jsx(Button, { type: "primary", size: "small", shape: "circle", icon: _jsx(ToolOutlined, {}), "aria-label": "\u91C7\u96C6\u7BA1\u7406", onClick: (event) => {
                                event.stopPropagation();
                                openManagePage(task);
                            } }) }), _jsx(Tooltip, { title: "\u8BE6\u60C5", children: _jsx(Button, { size: "small", shape: "circle", icon: _jsx(EyeOutlined, {}), "aria-label": "\u8BE6\u60C5", onClick: (event) => {
                                event.stopPropagation();
                                setSelectedTask(task);
                            } }) })] })),
        },
    ];
    const visibleColumns = columns.filter((column) => !('dataIndex' in column && ['taskNo', 'projectName'].includes(String(column.dataIndex))));
    const drawerTask = selectedTask;
    return (_jsx(PermissionGate, { code: "certification.practical.collection.view", fallback: _jsx(Empty, { description: "\u6682\u65E0\u4FE1\u606F\u91C7\u96C6\u8BBF\u95EE\u6743\u9650" }), children: _jsxs("div", { className: "knowledge-page collection-task-page", children: [_jsxs("div", { className: "page-heading knowledge-heading collection-task-heading", children: [_jsx("div", { children: _jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(QrcodeOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u4FE1\u606F\u91C7\u96C6" }), _jsx(Typography.Text, { type: "secondary", children: "\u6309\u8003\u8BD5\u573A\u6B21\u67E5\u770B\u73B0\u573A\u4FE1\u606F\u91C7\u96C6\u4EFB\u52A1\uFF0C\u5E76\u8FDB\u5165 PC \u91C7\u96C6\u7BA1\u7406\u53F0" })] })] }) }), _jsxs(Space, { wrap: true, className: "collection-task-toolbar", children: [_jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u4EFB\u52A1\u540D\u79F0 / \u7F16\u53F7 / \u4F01\u4E1A", value: keyword, onChange: (event) => setKeyword(event.target.value), style: { width: 260 } }), _jsx(Select, { value: statusFilter, options: statusOptions, onChange: setStatusFilter, style: { width: 128 } }), _jsx(RangePicker, { allowClear: true, onChange: (_, dateStrings) => {
                                        const [start, end] = dateStrings;
                                        setDateRange(start && end ? [start, end] : null);
                                    } }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: tasksQuery.isFetching, onClick: () => tasksQuery.refetch(), children: "\u5237\u65B0" })] })] }), _jsx(Table, { className: "collection-task-table", rowKey: (task) => String(task.examSessionId || task.taskId), columns: visibleColumns, dataSource: filteredTasks, loading: tasksQuery.isFetching, pagination: { defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: (total) => `共 ${total} 个任务` }, scroll: { x: 1200 }, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u53EF\u91C7\u96C6\u4EFB\u52A1" }) }, onRow: (task) => ({
                        onClick: () => setSelectedTask(task),
                    }) }), _jsx(Drawer, { title: "\u4EFB\u52A1\u8BE6\u60C5", open: Boolean(drawerTask), width: 520, destroyOnHidden: true, onClose: () => setSelectedTask(null), children: drawerTask ? (_jsxs(Space, { direction: "vertical", size: 18, className: "collection-task-drawer", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: drawerTask.taskName }), _jsxs(Space, { wrap: true, children: [statusTag(drawerTask), _jsx(Typography.Text, { type: "secondary", children: drawerTask.taskNo || '-' })] })] }), _jsxs(Descriptions, { title: "\u4EFB\u52A1\u57FA\u7840\u4FE1\u606F", column: 1, bordered: true, size: "small", children: [_jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u540D\u79F0", children: drawerTask.taskName || '-' }), _jsx(Descriptions.Item, { label: "\u573A\u6B21\u540D\u79F0", children: drawerTask.sessionName || '-' }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7F16\u53F7", children: drawerTask.taskNo || '-' }), _jsx(Descriptions.Item, { label: "\u4F01\u4E1A", children: drawerTask.projectName || '-' }), _jsx(Descriptions.Item, { label: "\u5DE5\u79CD/\u7B49\u7EA7", children: drawerTask.workTypeLevel || `${drawerTask.workType || '-'} / ${drawerTask.level || '-'}` }), _jsx(Descriptions.Item, { label: "\u8BA4\u5B9A\u65E5\u671F", children: drawerTask.examDate || '-' })] }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 5, children: "\u91C7\u96C6\u7EDF\u8BA1" }), _jsxs("div", { className: "collection-drawer-stats", children: [_jsxs("div", { children: [_jsx("span", { children: "\u603B\u4EBA\u6570" }), _jsx("strong", { children: toNumber(drawerTask.candidateTotal) })] }), _jsxs("div", { children: [_jsx("span", { children: "\u5DF2\u53D1\u653E" }), _jsx("strong", { children: toNumber(drawerTask.qrIssuedCount) })] }), _jsxs("div", { children: [_jsx("span", { children: "\u5DF2\u5230\u573A" }), _jsx("strong", { children: toNumber(drawerTask.arrivedCount) })] }), _jsxs("div", { children: [_jsx("span", { children: "\u5DF2\u5165\u961F" }), _jsx("strong", { children: toNumber(drawerTask.queuedCount) })] }), _jsxs("div", { children: [_jsx("span", { children: "\u91C7\u96C6\u4E2D" }), _jsx("strong", { children: toNumber(drawerTask.collectingCount) })] }), _jsxs("div", { children: [_jsx("span", { children: "\u5DF2\u5B8C\u6210" }), _jsx("strong", { children: toNumber(drawerTask.completedCount) })] }), _jsxs("div", { children: [_jsx("span", { children: "\u5F85\u91C7\u96C6" }), _jsx("strong", { children: pendingCount(drawerTask) })] }), _jsxs("div", { children: [_jsx("span", { children: "\u5F02\u5E38" }), _jsx("strong", { children: toNumber(drawerTask.exceptionCount) })] })] })] }), _jsx(Alert, { type: "info", showIcon: true, message: "\u5E73\u677F\u7AEF\u5165\u53E3\u4FE1\u606F", description: "\u5E73\u677F\u7AEF\u73B0\u573A\u91C7\u96C6\u9875\u7528\u4E8E\u73B0\u573A\u626B\u7801\u3001\u6307\u7EB9\u91C7\u96C6\u548C\u7535\u5B50\u7B7E\u540D\u3002PC \u9996\u9875\u4E0D\u76F4\u63A5\u8FDB\u5165\u5E73\u677F\u7AEF\u9875\u9762\uFF0C\u8BF7\u4ECE PC \u7BA1\u7406\u53F0\u67E5\u770B\u6216\u751F\u6210\u5E73\u677F\u7AEF\u5165\u53E3\u3002" }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 5, children: "\u6700\u8FD1\u64CD\u4F5C / \u63D0\u793A" }), _jsx(Typography.Paragraph, { type: "secondary", children: "\u5F53\u524D\u9996\u9875\u4EC5\u5C55\u793A\u4EFB\u52A1\u6982\u51B5\u3002\u7BA1\u7406\u3001\u76D1\u63A7\u3001\u8C03\u5EA6\u548C\u5F02\u5E38\u5904\u7406\u5C06\u5728 PC \u91C7\u96C6\u7BA1\u7406\u53F0\u4E2D\u7EE7\u7EED\u627F\u63A5\u3002" })] }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 5, children: "\u5FEB\u6377\u64CD\u4F5C" }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(PlayCircleOutlined, {}), onClick: () => openManagePage(drawerTask), children: "\u8FDB\u5165 PC \u91C7\u96C6\u7BA1\u7406\u53F0" }), _jsx(Button, { loading: initializeMutation.isPending, onClick: () => initializeMutation.mutate(drawerTask), children: "\u521D\u59CB\u5316\u91C7\u96C6\u72B6\u6001" }), _jsx(Button, { icon: _jsx(QrcodeOutlined, {}), loading: generateQrMutation.isPending, onClick: () => generateQrMutation.mutate(drawerTask), children: "\u751F\u6210\u4E8C\u7EF4\u7801" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exportMutation.isPending, onClick: () => exportMutation.mutate(drawerTask), children: "\u4E8C\u7EF4\u7801\u5BFC\u51FA" }), _jsx(Button, { icon: _jsx(ExclamationCircleOutlined, {}), onClick: () => setExceptionTask(drawerTask), children: "\u67E5\u770B\u5F02\u5E38\u8BB0\u5F55" })] })] })] })) : null }), _jsx(Modal, { title: exceptionTask ? `${exceptionTask.taskName} · 异常记录` : '异常记录', open: Boolean(exceptionTask), width: 920, footer: null, destroyOnHidden: true, onCancel: () => setExceptionTask(null), children: _jsx(CollectionExceptionPage, { taskId: exceptionTask?.taskId, examSessionId: exceptionTask?.examSessionId }) })] }) }));
}

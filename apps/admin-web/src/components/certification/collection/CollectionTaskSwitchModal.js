import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Empty, Input, Modal, Space, Spin, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { fetchCollectionTasks } from '../../../services/practicalCollection/tabletCollectionService';
import { formatCollectionTaskStatus } from '../../../utils/collectionTask';
function isSameTask(left, right) {
    return left !== undefined && left !== null && right !== undefined && right !== null
        && String(left) === String(right);
}
export function CollectionTaskSwitchModal({ open, currentTaskId, currentExamSessionId, forceSelect = false, switching = false, onSelect, onClose, }) {
    const [keyword, setKeyword] = useState('');
    const tasksQuery = useQuery({
        queryKey: ['tablet-collection-tasks', keyword],
        queryFn: () => fetchCollectionTasks(keyword),
        enabled: open,
    });
    const tasks = useMemo(() => tasksQuery.data || [], [tasksQuery.data]);
    return (_jsx(Modal, { title: "\u5207\u6362\u91C7\u96C6\u4EFB\u52A1", open: open, width: 760, footer: null, destroyOnHidden: true, closable: !forceSelect, maskClosable: !forceSelect, onCancel: forceSelect ? undefined : onClose, children: _jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u4EFB\u52A1\u540D\u79F0 / \u7F16\u53F7", value: keyword, onChange: (event) => setKeyword(event.target.value), onPressEnter: () => tasksQuery.refetch() }), _jsxs("div", { className: "collection-task-switch-toolbar", children: [_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8981\u8FDB\u5165\u7684\u91C7\u96C6\u4EFB\u52A1\uFF0C\u5207\u6362\u540E\u5C06\u5237\u65B0\u8003\u751F\u540D\u5355\u4E0E\u91C7\u96C6\u72B6\u6001\u3002" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: tasksQuery.isFetching, onClick: () => tasksQuery.refetch(), children: "\u5237\u65B0\u5217\u8868" })] }), _jsxs("div", { className: "collection-task-switch-list", children: [tasksQuery.isLoading ? (_jsx("div", { className: "collection-task-switch-loading", children: _jsx(Spin, {}) })) : null, !tasksQuery.isLoading && tasks.length === 0 ? (_jsx(Empty, { description: "\u6682\u65E0\u53EF\u5207\u6362\u7684\u91C7\u96C6\u4EFB\u52A1" })) : null, tasks.map((task) => {
                            const status = formatCollectionTaskStatus(task);
                            const sameSession = task.examSessionId == null && currentExamSessionId == null
                                ? true
                                : isSameTask(task.examSessionId, currentExamSessionId);
                            const isCurrent = isSameTask(task.taskId, currentTaskId) && sameSession;
                            return (_jsx("button", { type: "button", className: `collection-task-switch-item${isCurrent ? ' is-current' : ''}`, disabled: switching, onClick: () => onSelect(task), children: _jsxs("div", { className: "collection-task-switch-item-main", children: [_jsxs("div", { className: "collection-task-switch-item-title", children: [_jsx("strong", { children: task.taskName }), isCurrent ? _jsx(Tag, { color: "blue", children: "\u5F53\u524D\u4EFB\u52A1" }) : null, _jsx(Tag, { color: status.color, children: status.label })] }), _jsxs("div", { className: "collection-task-switch-item-meta", children: [_jsxs("span", { children: ["\u4EFB\u52A1\u7F16\u53F7\uFF1A", task.taskNo || '-'] }), _jsxs("span", { children: ["\u573A\u6B21\uFF1A", task.sessionName || '-'] }), _jsxs("span", { children: ["\u65E5\u671F\uFF1A", task.examDate || '-'] }), _jsxs("span", { children: ["\u8003\u751F\uFF1A", task.candidateTotal, " \u4EBA"] }), _jsxs("span", { children: ["\u5DF2\u5B8C\u6210\uFF1A", task.completedCount, " \u4EBA"] })] }), (task.workTypeLevel || task.workType || task.level) ? (_jsx("div", { className: "collection-task-switch-item-extra", children: task.workTypeLevel || [task.workType, task.level].filter(Boolean).join(' · ') })) : null] }) }, `${String(task.taskId)}:${String(task.examSessionId || 'legacy')}`));
                        })] })] }) }));
}

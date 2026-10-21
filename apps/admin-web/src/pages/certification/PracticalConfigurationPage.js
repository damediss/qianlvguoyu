import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Empty, Select, Space, Typography } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { pagePreparationTasks } from '../../api/certificationPreparation';
import { PermissionGate } from '../../directives/permission';
import { PracticalScoringConfig } from './preparation/PracticalScoringConfig';
export function PracticalConfigurationPage() {
    const queryClient = useQueryClient();
    const [selectedTaskId, setSelectedTaskId] = useState();
    const tasksQuery = useQuery({
        queryKey: ['certification-practical-config-tasks'],
        queryFn: () => pagePreparationTasks({ page: 1, pageSize: 200, currentPhase: 'preparation' }),
    });
    const tasks = tasksQuery.data?.items || [];
    useEffect(() => {
        if (!selectedTaskId && tasks.length > 0)
            setSelectedTaskId(tasks[0].id);
        if (selectedTaskId && tasks.length > 0 && !tasks.some((task) => task.id === selectedTaskId)) {
            setSelectedTaskId(tasks[0].id);
        }
    }, [selectedTaskId, tasks]);
    const selectedTask = useMemo(() => tasks.find((task) => task.id === selectedTaskId), [selectedTaskId, tasks]);
    const taskLabel = (task) => `${task.taskNo} · ${task.taskName} · ${task.workTypeName || '-'} / ${task.levelName || '-'}`;
    return (_jsx(PermissionGate, { code: "certification.practical.progress", fallback: _jsx(Empty, { description: "\u6682\u65E0\u5B9E\u64CD\u7BA1\u7406\u6743\u9650" }), children: _jsxs("div", { className: "knowledge-page", children: [_jsxs("div", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u5B9E\u64CD\u7BA1\u7406" }), _jsx(Typography.Text, { type: "secondary", children: "\u76D1\u7BA1\u5B9E\u64CD\u8003\u8BD5\u5F00\u8003\u3001\u8BC4\u5206\u63D0\u4EA4\u548C\u5B8C\u6210\u8FDB\u5EA6" })] }), _jsx(Space, { children: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    queryClient.invalidateQueries({ queryKey: ['certification-practical-config-tasks'] });
                                    queryClient.invalidateQueries({ queryKey: ['practical-scoring-progress', selectedTaskId] });
                                }, children: "\u5237\u65B0" }) })] }), _jsxs(Card, { title: "\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1", style: { marginBottom: 16 }, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", loading: tasksQuery.isFetching, value: selectedTaskId, placeholder: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1", style: { width: 'min(720px, 100%)' }, options: tasks.map((task) => ({ value: task.id, label: taskLabel(task) })), onChange: setSelectedTaskId }), selectedTask ? (_jsxs(Typography.Paragraph, { type: "secondary", style: { marginTop: 12, marginBottom: 0 }, children: ["\u5F53\u524D\u4EFB\u52A1\uFF1A", taskLabel(selectedTask)] })) : null] }), _jsx(PracticalScoringConfig, { taskId: selectedTaskId, showSetup: false, showTemplate: false, showProgress: true })] }) }));
}

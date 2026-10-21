import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckSquareOutlined, DatabaseOutlined, DownOutlined, FileProtectOutlined, ReloadOutlined, SafetyCertificateOutlined, ScheduleOutlined, TeamOutlined, UpOutlined, } from '@ant-design/icons';
import { Button, Card, Empty, Select, Space, Tabs, Tag, Typography } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { pagePreparationTasks } from '../../api/certificationPreparation';
import { PermissionGate } from '../../directives/permission';
import { AssessorConfig } from './preparation/AssessorConfig';
import { CandidateImport } from './preparation/CandidateImport';
import { ChecklistReview } from './preparation/ChecklistReview';
import { EquipmentConfig } from './preparation/EquipmentConfig';
import { ExamSiteConfig } from './preparation/ExamSiteConfig';
import { ExamStaffConfig } from './preparation/ExamStaffConfig';
import { ExamSessionConfig } from './preparation/ExamSessionConfig';
import { PreparationWorkbench } from './preparation/PreparationWorkbench';
import { PracticalScoringConfig } from './preparation/PracticalScoringConfig';
import { DefenseScoringConfig } from './preparation/DefenseScoringConfig';
import './PreparationPage.css';
const MORE_TAB_KEYS = ['staff', 'equipment', 'sites', 'checklist'];
const MORE_TAB_PANE_KEY = '__more__';
export function PreparationPage() {
    const queryClient = useQueryClient();
    // 认定筹备模块：固定按任务阶段 = preparation 过滤
    const query = { page: 1, pageSize: 500, currentPhase: 'preparation' };
    const [selectedTaskId, setSelectedTaskId] = useState();
    const [activeTab, setActiveTab] = useState('workbench');
    const [collapsedTabs, setCollapsedTabs] = useState(false);
    const [moreSelectOpen, setMoreSelectOpen] = useState(false);
    const tasksQuery = useQuery({
        queryKey: ['certification-preparation-tasks', query],
        queryFn: () => pagePreparationTasks(query),
    });
    const tasks = tasksQuery.data?.items || [];
    useEffect(() => {
        if (!selectedTaskId && tasks.length > 0) {
            setSelectedTaskId(tasks[0].id);
        }
        if (selectedTaskId && tasks.length > 0 && !tasks.some((task) => task.id === selectedTaskId)) {
            setSelectedTaskId(tasks[0].id);
        }
        if (selectedTaskId && !tasksQuery.isFetching && tasks.length === 0) {
            setSelectedTaskId(undefined);
        }
    }, [selectedTaskId, tasks, tasksQuery.isFetching]);
    const selectedTask = useMemo(() => tasks.find((task) => task.id === selectedTaskId), [selectedTaskId, tasks]);
    const taskOptions = useMemo(() => tasks.map((task) => ({
        value: task.id,
        label: [task.taskNo, task.taskName].filter(Boolean).join(' · '),
    })), [tasks]);
    const refreshAll = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation'] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-tasks'] });
        if (selectedTaskId) {
            queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', selectedTaskId] });
            queryClient.invalidateQueries({ queryKey: ['practical-scoring-setup', selectedTaskId] });
            queryClient.invalidateQueries({ queryKey: ['defense-scoring-setup', selectedTaskId] });
        }
    };
    const tabItems = [
        {
            key: 'workbench',
            label: '筹备工作台',
            icon: _jsx(SafetyCertificateOutlined, {}),
            children: (_jsx(PreparationWorkbench, { taskId: selectedTaskId, task: selectedTask, onChanged: refreshAll })),
        },
        {
            key: 'collection',
            label: '考生导入',
            icon: _jsx(DatabaseOutlined, {}),
            children: _jsx(CandidateImport, { taskId: selectedTaskId, onChanged: refreshAll }),
        },
        {
            key: 'sessions',
            label: '考试场次',
            icon: _jsx(ScheduleOutlined, {}),
            children: _jsx(ExamSessionConfig, { taskId: selectedTaskId, onChanged: refreshAll }),
        },
        {
            key: 'staff',
            label: '考务配置',
            icon: _jsx(TeamOutlined, {}),
            children: _jsx(ExamStaffConfig, { taskId: selectedTaskId, onChanged: refreshAll }),
        },
        {
            key: 'equipment',
            label: '配置设备',
            icon: _jsx(DatabaseOutlined, {}),
            children: _jsx(EquipmentConfig, { taskId: selectedTaskId, onChanged: refreshAll }),
        },
        {
            key: 'sites',
            label: '考场配置',
            icon: _jsx(ScheduleOutlined, {}),
            children: _jsx(ExamSiteConfig, { taskId: selectedTaskId, onChanged: refreshAll }),
        },
        {
            key: 'assessors',
            label: '考评员配置',
            icon: _jsx(TeamOutlined, {}),
            children: _jsx(AssessorConfig, { taskId: selectedTaskId, task: selectedTask, onChanged: refreshAll }),
        },
        {
            key: 'scoring',
            label: '实操配置',
            icon: _jsx(CheckSquareOutlined, {}),
            children: _jsx(PracticalScoringConfig, { taskId: selectedTaskId, showProgress: false }),
        },
        {
            key: 'defense',
            label: '综合答辩配置',
            icon: _jsx(FileProtectOutlined, {}),
            children: _jsx(DefenseScoringConfig, { taskId: selectedTaskId }),
        },
        {
            key: 'checklist',
            label: '清单核查',
            icon: _jsx(CheckSquareOutlined, {}),
            children: _jsx(ChecklistReview, { taskId: selectedTaskId, onChanged: refreshAll }),
        },
    ];
    const tabOrder = [
        'workbench',
        'collection',
        'sessions',
        'assessors',
        'scoring',
        'defense',
        'staff',
        'equipment',
        'sites',
        'checklist',
    ];
    const orderedTabItems = [...tabItems].sort((a, b) => tabOrder.indexOf(a.key) - tabOrder.indexOf(b.key));
    const moreTabKeySet = new Set(MORE_TAB_KEYS);
    const isMoreTab = moreTabKeySet.has(activeTab);
    const moreTabItems = MORE_TAB_KEYS
        .map((key) => orderedTabItems.find((item) => item.key === key))
        .filter((item) => Boolean(item));
    const primaryTabItems = orderedTabItems.filter((item) => !moreTabKeySet.has(item.key));
    const activeMoreItem = moreTabItems.find((item) => item.key === activeTab);
    const visibleTabItems = [
        ...primaryTabItems,
        {
            key: MORE_TAB_PANE_KEY,
            label: (_jsx(Select, { className: "preparation-more-select", placeholder: "\u66F4\u591A", value: isMoreTab ? activeTab : undefined, open: moreSelectOpen, options: moreTabItems.map((item) => ({
                    value: item.key,
                    label: (_jsxs(Space, { size: 6, children: [item.icon, item.label] })),
                })), variant: "borderless", suffixIcon: _jsx(DownOutlined, {}), popupMatchSelectWidth: false, getPopupContainer: () => document.body, onOpenChange: setMoreSelectOpen, onClick: (event) => {
                    event.stopPropagation();
                    setMoreSelectOpen(true);
                }, onMouseDown: (event) => event.stopPropagation(), onChange: (key) => {
                    setActiveTab(key);
                    setMoreSelectOpen(false);
                } })),
            children: activeMoreItem?.children ?? _jsx(Empty, { description: "\u8BF7\u4ECE\u300C\u66F4\u591A\u300D\u4E2D\u9009\u62E9\u6A21\u5757" }),
        },
    ];
    return (_jsx(PermissionGate, { code: "certification.preparation.task.view", fallback: _jsx(Empty, { description: "\u6682\u65E0\u8BA4\u5B9A\u7B79\u5907\u8BBF\u95EE\u6743\u9650" }), children: _jsxs("div", { className: "knowledge-page", children: [_jsxs("div", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(SafetyCertificateOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u8BA4\u5B9A\u7B79\u5907" }), _jsx(Typography.Text, { type: "secondary", children: "\u627F\u63A5\u8BA4\u5B9A\u4EFB\u52A1\uFF0C\u5B8C\u6210\u4FE1\u606F\u6536\u96C6\u3001\u8003\u573A\u548C\u8003\u8BC4\u5458\u51C6\u5907" })] })] }), selectedTask && (_jsxs(Space, { wrap: true, children: [_jsx(Tag, { color: "processing", children: selectedTask.taskNo }), _jsx(Typography.Text, { strong: true, children: selectedTask.taskName }), _jsxs(Typography.Text, { type: "secondary", children: [selectedTask.workTypeName || '-', " / ", selectedTask.levelName || '-'] })] }))] }), _jsx(Space, { children: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refreshAll, children: "\u5237\u65B0" }) })] }), _jsx(Card, { className: "preparation-task-selector", size: "small", title: "\u5F53\u524D\u8BA4\u5B9A\u4EFB\u52A1", extra: _jsxs(Typography.Text, { type: "secondary", children: ["\u5171 ", tasksQuery.data?.total || 0, " \u9879"] }), children: _jsxs(Space, { wrap: true, size: [12, 8], style: { width: '100%' }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u4EFB\u52A1" }), _jsx(Select, { showSearch: true, allowClear: true, value: selectedTaskId, loading: tasksQuery.isFetching, disabled: !tasksQuery.isFetching && tasks.length === 0, placeholder: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1", optionFilterProp: "label", options: taskOptions, onChange: (value) => setSelectedTaskId(value || undefined), style: { width: 'min(100%, 520px)' } })] }) }), _jsx(Card, { title: "\u7B79\u5907\u8D44\u6599\u4E0E\u5DE5\u4F5C\u53F0", styles: {
                        body: collapsedTabs
                            ? { display: 'none', padding: 0 }
                            : { paddingTop: 12 },
                    }, extra: (_jsx(Button, { type: "text", icon: collapsedTabs ? _jsx(DownOutlined, {}) : _jsx(UpOutlined, {}), onClick: () => setCollapsedTabs((prev) => !prev), children: collapsedTabs ? '展开' : '折叠' })), children: !collapsedTabs ? (_jsx(_Fragment, { children: _jsx(Tabs, { className: "preparation-workbench-tabs", activeKey: isMoreTab ? MORE_TAB_PANE_KEY : activeTab, onChange: (key) => {
                                if (key === MORE_TAB_PANE_KEY) {
                                    setMoreSelectOpen(true);
                                    return;
                                }
                                setMoreSelectOpen(false);
                                setActiveTab(key);
                            }, items: visibleTabItems }) })) : null })] }) }));
}

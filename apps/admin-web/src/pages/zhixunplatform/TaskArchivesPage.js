import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { message, Modal } from 'antd';
import { ArrowLeftOutlined, PlusOutlined, SearchOutlined, RedoOutlined, ExportOutlined, EyeOutlined, EditOutlined, UserSwitchOutlined, FileTextOutlined, FileProtectOutlined, AuditOutlined, ToolOutlined, SafetyCertificateOutlined, FileAddOutlined, TrophyOutlined, DollarOutlined, FolderOpenOutlined, DeleteOutlined, ExclamationCircleOutlined, InboxOutlined, CloseOutlined, NodeIndexOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TaskArchivesPage.css';
// ============================================================
// 常量
// ============================================================
const STORAGE_KEY = 'zhixun_tasks';
const CONFIG_TOTAL = 9;
const CONFIG_LABELS = {
    humanTeacher: '师资与教务',
    humanStudent: '参训学员',
    machine: '机构与场地',
    materialCourse: '课程与课件',
    materialCost: '预算与费用',
    lawSchedule: '分班排课',
    lawMode: '培养方案',
    lawRule: '业务规则',
    environment: '开班与合规',
};
const DEFAULT_TASKS = [
    { id: 1, name: '2026年度安全培训计划编制', owner: '张明', deadline: '2026-06-15', priority: '高', enterprise: '广西建工集团', occupation: '安全员', skillLevel: '高级', region: '南宁', projectType: '安全培训', status: '进行中', planCount: 120, startDate: '2026-03-01', endDate: '2026-06-30' },
    { id: 2, name: '电工高级班认定材料整理', owner: '李芳', deadline: '2026-05-25', priority: '高', enterprise: '南宁供电局', occupation: '电工', skillLevel: '高级', region: '南宁', projectType: '资质认证', status: '进行中', planCount: 42 },
    { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', deadline: '2026-05-30', priority: '中', enterprise: '柳州钢铁有限公司', occupation: '焊工', skillLevel: '中级', region: '柳州', projectType: '新员工培训', status: '草稿', planCount: 36 },
    { id: 4, name: '焊工中级班排课协调', owner: '赵丽', deadline: '2026-05-22', priority: '中', enterprise: '桂林旅游股份公司', occupation: '焊工', skillLevel: '中级', region: '桂林', projectType: '资质认证', status: '待审批', planCount: 28 },
    { id: 5, name: '培训档案年度归档', owner: '陈伟', deadline: '2026-06-30', priority: '低', enterprise: '梧州港务集团', occupation: '起重工', skillLevel: '初级', region: '梧州', projectType: '管理研修', status: '草稿', planCount: 15 },
];
const PRIORITY_CLASS = { '高': 'priority-high', '中': 'priority-medium', '低': 'priority-low' };
const STATUS_CLASS = { '进行中': 'status-active', '草稿': 'status-draft', '待审批': 'status-pending', '已完成': 'status-done' };
function taskCode(id) {
    return 'TASK-' + String(id).padStart(4, '0');
}
function isOverdue(task) {
    if (!task.deadline || task.status === '已完成')
        return false;
    return new Date(task.deadline) < new Date(new Date().toDateString());
}
function getConfigProgress(task) {
    const cfg = task.taskConfig || task.fiveM;
    if (!cfg)
        return { done: 0, total: CONFIG_TOTAL };
    const done = Object.values(cfg).filter((c) => c && c.configured).length;
    return { done, total: CONFIG_TOTAL };
}
function loadTasks() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (raw)
            return JSON.parse(raw);
    }
    catch { /* ignore */ }
    return null;
}
function saveTasks(tasks) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks));
}
// ============================================================
// 主页面
// ============================================================
export function TaskArchivesPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const setEditingTaskId = useNavStore((s) => s.setEditingTaskId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [tasks, setTasks] = useState(() => {
        return loadTasks() || DEFAULT_TASKS;
    });
    const [quickFilter, setQuickFilter] = useState('all');
    const [searchText, setSearchText] = useState('');
    const [filterStatus, setFilterStatus] = useState('');
    const [filterPriority, setFilterPriority] = useState('');
    const [filterOccupation, setFilterOccupation] = useState('');
    const [filterRegion, setFilterRegion] = useState('');
    const [drawerTask, setDrawerTask] = useState(null);
    // localStorage 持久化：每次 tasks 变化时写入
    useEffect(() => {
        saveTasks(tasks);
    }, [tasks]);
    // URL ?id= 打开详情
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const idParam = params.get('id');
        if (idParam) {
            const id = parseInt(idParam);
            const task = tasks.find((t) => t.id === id);
            if (task)
                setDrawerTask(task);
        }
    }, [tasks]);
    const stats = useMemo(() => {
        return {
            all: tasks.length,
            active: tasks.filter((t) => t.status === '进行中').length,
            draft: tasks.filter((t) => ['草稿', '待审批'].includes(t.status)).length,
            overdue: tasks.filter((t) => isOverdue(t)).length,
        };
    }, [tasks]);
    const filteredTasks = useMemo(() => {
        return tasks.filter((t) => {
            if (quickFilter === '进行中' && t.status !== '进行中')
                return false;
            if (quickFilter === 'draft' && !['草稿', '待审批'].includes(t.status))
                return false;
            if (quickFilter === 'overdue' && !isOverdue(t))
                return false;
            if (filterStatus && t.status !== filterStatus)
                return false;
            if (filterPriority && t.priority !== filterPriority)
                return false;
            if (filterOccupation && t.occupation !== filterOccupation)
                return false;
            if (filterRegion && t.region !== filterRegion)
                return false;
            if (searchText) {
                const hay = [t.name, t.enterprise, t.owner, t.occupation, taskCode(t.id)].join(' ').toLowerCase();
                if (!hay.includes(searchText.toLowerCase()))
                    return false;
            }
            return true;
        });
    }, [tasks, quickFilter, searchText, filterStatus, filterPriority, filterOccupation, filterRegion]);
    const resetFilters = useCallback(() => {
        setSearchText('');
        setFilterStatus('');
        setFilterPriority('');
        setFilterOccupation('');
        setFilterRegion('');
        setQuickFilter('all');
    }, []);
    const handleDelete = useCallback((id) => {
        Modal.confirm({
            title: '确定删除该任务？',
            onOk: () => {
                setTasks((prev) => prev.filter((t) => t.id !== id));
                message.success('任务已删除');
            },
        });
    }, []);
    const handleEditFromDrawer = useCallback(() => {
        if (drawerTask) {
            setEditingTaskId(drawerTask.id);
            navigateTo('zhixun-create-task');
        }
    }, [drawerTask, setEditingTaskId, navigateTo]);
    const handleProcessFromDrawer = useCallback(() => {
        if (drawerTask) {
            setCurrentProjectId(drawerTask.id);
            navigateTo('zhixun-training-implementation');
        }
    }, [drawerTask, setCurrentProjectId, navigateTo]);
    const navigateProject = useCallback((task, key) => {
        setCurrentProjectId(task.id);
        navigateTo(key);
    }, [navigateTo, setCurrentProjectId]);
    const taskActions = [
        { title: '查看', icon: _jsx(EyeOutlined, {}), onClick: (task) => setDrawerTask(task) },
        { title: '编辑', icon: _jsx(EditOutlined, {}), onClick: (task) => { setEditingTaskId(task.id); navigateTo('zhixun-create-task'); } },
        { title: '人才识别', icon: _jsx(UserSwitchOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-talent-identification') },
        { title: '初步方案', icon: _jsx(FileTextOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-preliminary-plan') },
        { title: '最终方案', icon: _jsx(FileProtectOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-final-plan') },
        { title: '开班申报', icon: _jsx(AuditOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-class-declaration') },
        { title: '培训实施', icon: _jsx(ToolOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-training-implementation') },
        { title: '认定准备', icon: _jsx(SafetyCertificateOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-certification') },
        { title: '申报服务', icon: _jsx(FileAddOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-declaration-service') },
        { title: '培训结业', icon: _jsx(TrophyOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-graduation') },
        { title: '项目回款', icon: _jsx(DollarOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-refund') },
        { title: '资料档案', icon: _jsx(FolderOpenOutlined, {}), onClick: (task) => navigateProject(task, 'zhixun-material-archives') },
        { title: '删除', icon: _jsx(DeleteOutlined, {}), danger: true, onClick: (task) => handleDelete(task.id) },
    ];
    return (_jsxs("div", { className: "task-archives-page", children: [_jsxs("header", { className: "ta-topbar", children: [_jsxs("div", { className: "ta-topbar-left", children: [_jsxs("a", { className: "ta-back-link", onClick: () => navigateTo('zhixun-task-management'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u4EFB\u52A1\u7BA1\u7406"] }), _jsxs("div", { children: [_jsx("h1", { className: "ta-title", children: "\u4EFB\u52A1\u6863\u6848" }), _jsx("span", { className: "ta-sub", children: "\u57F9\u8BAD\u4EFB\u52A1\u53F0\u8D26 \u00B7 \u67E5\u8BE2\u3001\u7B5B\u9009\u4E0E\u8BE6\u60C5" })] })] }), _jsxs("div", { className: "ta-topbar-actions", children: [_jsxs("button", { type: "button", className: "ta-btn ta-btn-secondary", onClick: () => navigateTo('zhixun-material-archives'), children: [_jsx(FolderOpenOutlined, {}), " \u8D44\u6599\u6863\u6848"] }), _jsxs("button", { type: "button", className: "ta-btn ta-btn-primary", onClick: () => navigateTo('zhixun-create-task'), children: [_jsx(PlusOutlined, {}), "\u65B0\u5EFA\u4EFB\u52A1"] })] })] }), _jsxs("main", { className: "ta-page", children: [_jsxs("div", { className: "ta-stat-row", children: [_jsxs("div", { className: `ta-stat-card${quickFilter === 'all' ? ' active' : ''}`, onClick: () => setQuickFilter('all'), children: [_jsx("div", { className: "ta-stat-val", children: stats.all }), _jsx("div", { className: "ta-stat-lbl", children: "\u5168\u90E8\u4EFB\u52A1" })] }), _jsxs("div", { className: `ta-stat-card${quickFilter === '进行中' ? ' active' : ''}`, onClick: () => setQuickFilter('进行中'), children: [_jsx("div", { className: "ta-stat-val", children: stats.active }), _jsx("div", { className: "ta-stat-lbl", children: "\u8FDB\u884C\u4E2D" })] }), _jsxs("div", { className: `ta-stat-card${quickFilter === 'draft' ? ' active' : ''}`, onClick: () => setQuickFilter('draft'), children: [_jsx("div", { className: "ta-stat-val", children: stats.draft }), _jsx("div", { className: "ta-stat-lbl", children: "\u8349\u7A3F / \u5F85\u5BA1\u6279" })] }), _jsxs("div", { className: `ta-stat-card${quickFilter === 'overdue' ? ' active' : ''}`, onClick: () => setQuickFilter('overdue'), children: [_jsx("div", { className: "ta-stat-val", children: stats.overdue }), _jsx("div", { className: "ta-stat-lbl", children: "\u5DF2\u903E\u671F" })] })] }), _jsxs("div", { className: "ta-toolbar", children: [_jsxs("div", { className: "ta-search-box", children: [_jsx(SearchOutlined, {}), _jsx("input", { type: "text", placeholder: "\u641C\u7D22\u4EFB\u52A1\u540D\u79F0\u3001\u4F01\u4E1A\u3001\u8D1F\u8D23\u4EBA\u2026", value: searchText, onChange: (e) => setSearchText(e.target.value) })] }), _jsxs("select", { className: "ta-filter-select", value: filterStatus, onChange: (e) => setFilterStatus(e.target.value), children: [_jsx("option", { value: "", children: "\u5168\u90E8\u72B6\u6001" }), _jsx("option", { value: "\u8FDB\u884C\u4E2D", children: "\u8FDB\u884C\u4E2D" }), _jsx("option", { value: "\u8349\u7A3F", children: "\u8349\u7A3F" }), _jsx("option", { value: "\u5F85\u5BA1\u6279", children: "\u5F85\u5BA1\u6279" }), _jsx("option", { value: "\u5DF2\u5B8C\u6210", children: "\u5DF2\u5B8C\u6210" })] }), _jsxs("select", { className: "ta-filter-select", value: filterPriority, onChange: (e) => setFilterPriority(e.target.value), children: [_jsx("option", { value: "", children: "\u5168\u90E8\u4F18\u5148\u7EA7" }), _jsx("option", { value: "\u9AD8", children: "\u9AD8" }), _jsx("option", { value: "\u4E2D", children: "\u4E2D" }), _jsx("option", { value: "\u4F4E", children: "\u4F4E" })] }), _jsxs("select", { className: "ta-filter-select", value: filterOccupation, onChange: (e) => setFilterOccupation(e.target.value), children: [_jsx("option", { value: "", children: "\u5168\u90E8\u5DE5\u79CD" }), _jsx("option", { value: "\u7535\u5DE5", children: "\u7535\u5DE5" }), _jsx("option", { value: "\u710A\u5DE5", children: "\u710A\u5DE5" }), _jsx("option", { value: "\u8D77\u91CD\u5DE5", children: "\u8D77\u91CD\u5DE5" }), _jsx("option", { value: "\u5B89\u5168\u5458", children: "\u5B89\u5168\u5458" })] }), _jsxs("select", { className: "ta-filter-select", value: filterRegion, onChange: (e) => setFilterRegion(e.target.value), children: [_jsx("option", { value: "", children: "\u5168\u90E8\u533A\u57DF" }), _jsx("option", { value: "\u5357\u5B81", children: "\u5357\u5B81" }), _jsx("option", { value: "\u6842\u6797", children: "\u6842\u6797" }), _jsx("option", { value: "\u67F3\u5DDE", children: "\u67F3\u5DDE" }), _jsx("option", { value: "\u7389\u6797", children: "\u7389\u6797" }), _jsx("option", { value: "\u68A7\u5DDE", children: "\u68A7\u5DDE" })] }), _jsxs("button", { type: "button", className: "ta-btn ta-btn-secondary", onClick: resetFilters, children: [_jsx(RedoOutlined, {}), " \u91CD\u7F6E"] }), _jsxs("button", { type: "button", className: "ta-btn ta-btn-secondary", onClick: () => message.info(`导出演示：共 ${filteredTasks.length} 条任务`), children: [_jsx(ExportOutlined, {}), " \u5BFC\u51FA"] })] }), _jsx("div", { className: "ta-card", children: filteredTasks.length > 0 ? (_jsx("div", { className: "ta-table-wrap", children: _jsxs("table", { className: "ta-table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "\u4EFB\u52A1" }), _jsx("th", { children: "\u4F01\u4E1A" }), _jsx("th", { children: "\u5DE5\u79CD / \u7B49\u7EA7" }), _jsx("th", { children: "\u533A\u57DF" }), _jsx("th", { children: "\u8D1F\u8D23\u4EBA" }), _jsx("th", { children: "\u622A\u6B62\u65E5\u671F" }), _jsx("th", { children: "\u4F18\u5148\u7EA7" }), _jsx("th", { children: "\u72B6\u6001" }), _jsx("th", { children: "\u914D\u7F6E" }), _jsx("th", { children: "\u64CD\u4F5C" })] }) }), _jsx("tbody", { children: filteredTasks.map((task) => {
                                            const prog = getConfigProgress(task);
                                            const pct = Math.round((prog.done / prog.total) * 100);
                                            const overdue = isOverdue(task);
                                            return (_jsxs("tr", { className: "clickable", onClick: () => setDrawerTask(task), children: [_jsxs("td", { children: [_jsx("div", { className: "task-name", children: task.name }), _jsx("div", { className: "task-code", children: taskCode(task.id) })] }), _jsx("td", { children: task.enterprise || '—' }), _jsxs("td", { children: [task.occupation || '—', task.skillLevel ? ' · ' + task.skillLevel : ''] }), _jsx("td", { children: task.region || '—' }), _jsx("td", { children: task.owner }), _jsxs("td", { className: overdue ? 'overdue-text' : '', children: [task.deadline || '—', overdue && _jsx(ExclamationCircleOutlined, { title: "\u5DF2\u903E\u671F" })] }), _jsx("td", { children: _jsx("span", { className: `priority-badge ${PRIORITY_CLASS[task.priority] || 'priority-medium'}`, children: task.priority }) }), _jsx("td", { children: _jsx("span", { className: `status-badge ${STATUS_CLASS[task.status || '草稿'] || 'status-draft'}`, children: task.status || '草稿' }) }), _jsxs("td", { className: "progress-wrap", children: [_jsxs("span", { className: "progress-text", children: [prog.done, "/", prog.total] }), _jsx("div", { className: "progress-bar", children: _jsx("div", { className: "progress-fill", style: { width: `${pct}%` } }) })] }), _jsx("td", { children: _jsx("div", { className: "action-btns", onClick: (e) => e.stopPropagation(), children: taskActions.map((action) => (_jsx("button", { type: "button", className: `action-btn${action.danger ? ' delete' : ''}`, title: action.title, onClick: () => action.onClick(task), children: action.icon }, action.title))) }) })] }, task.id));
                                        }) })] }) })) : (_jsxs("div", { className: "ta-empty", children: [_jsx(InboxOutlined, {}), _jsx("p", { children: "\u6682\u65E0\u7B26\u5408\u6761\u4EF6\u7684\u4EFB\u52A1" }), _jsx("button", { type: "button", className: "ta-btn ta-btn-primary", onClick: () => navigateTo('zhixun-create-task'), children: "\u521B\u5EFA\u7B2C\u4E00\u4E2A\u4EFB\u52A1" })] })) })] }), _jsx("div", { className: `ta-drawer-overlay${drawerTask ? ' show' : ''}`, onClick: (e) => {
                    if (e.currentTarget === e.target)
                        setDrawerTask(null);
                }, children: _jsx("aside", { className: "ta-drawer", "aria-hidden": !drawerTask, children: drawerTask && (_jsxs(_Fragment, { children: [_jsxs("div", { className: "ta-drawer-head", children: [_jsxs("div", { children: [_jsx("h2", { children: drawerTask.name }), _jsx("span", { className: "task-code", children: taskCode(drawerTask.id) })] }), _jsx("button", { type: "button", className: "ta-drawer-close", onClick: () => setDrawerTask(null), children: _jsx(CloseOutlined, {}) })] }), _jsxs("div", { className: "ta-drawer-body", children: [_jsxs("div", { className: "ta-detail-section", children: [_jsx("h4", { children: "\u57FA\u672C\u4FE1\u606F" }), _jsxs("div", { className: "ta-detail-grid", children: [_jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u8D1F\u8D23\u4EBA" }), _jsx("span", { children: drawerTask.owner })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u622A\u6B62\u65E5\u671F" }), _jsx("span", { children: drawerTask.deadline || '—' })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u4F18\u5148\u7EA7" }), _jsx("span", { children: drawerTask.priority })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u72B6\u6001" }), _jsx("span", { children: drawerTask.status || '草稿' })] })] }), drawerTask.description && _jsx("p", { className: "ta-detail-description", children: drawerTask.description })] }), _jsxs("div", { className: "ta-detail-section", children: [_jsx("h4", { children: "\u9879\u76EE\u4FE1\u606F" }), _jsxs("div", { className: "ta-detail-grid", children: [_jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u59D4\u6258\u4F01\u4E1A" }), _jsx("span", { children: drawerTask.enterprise || '—' })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u6240\u5C5E\u533A\u57DF" }), _jsx("span", { children: drawerTask.region || '—' })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u57F9\u8BAD\u5DE5\u79CD" }), _jsxs("span", { children: [drawerTask.occupation || '—', " \u00B7 ", drawerTask.skillLevel || '—'] })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u9879\u76EE\u7C7B\u578B" }), _jsx("span", { children: drawerTask.projectType || '—' })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u8BA1\u5212\u4EBA\u6570" }), _jsxs("span", { children: [drawerTask.planCount || '—', " \u4EBA"] })] }), _jsxs("div", { className: "ta-detail-item", children: [_jsx("label", { children: "\u8BA1\u5212\u5468\u671F" }), _jsxs("span", { children: [drawerTask.startDate || '—', " ~ ", drawerTask.endDate || '—'] })] })] })] }), _jsxs("div", { className: "ta-detail-section", children: [_jsxs("h4", { children: ["\u4EFB\u52A1\u914D\u7F6E\uFF08", getConfigProgress(drawerTask).done, "/", getConfigProgress(drawerTask).total, "\uFF09"] }), _jsx("div", { className: "ta-config-tags", children: Object.keys(CONFIG_LABELS).map((key) => {
                                                    const cfg = (drawerTask.taskConfig || drawerTask.fiveM)?.[key];
                                                    const done = cfg && cfg.configured;
                                                    return (_jsxs("span", { className: `ta-config-tag${done ? ' done' : ''}`, children: [CONFIG_LABELS[key], done ? ' ✓' : ''] }, key));
                                                }) })] })] }), _jsxs("div", { className: "ta-drawer-actions", children: [_jsxs("button", { type: "button", className: "ta-btn ta-btn-primary", onClick: handleEditFromDrawer, children: [_jsx(EditOutlined, {}), " \u7F16\u8F91\u4EFB\u52A1"] }), _jsxs("button", { type: "button", className: "ta-btn ta-btn-secondary", onClick: handleProcessFromDrawer, children: [_jsx(NodeIndexOutlined, {}), " \u57F9\u8BAD\u6D41\u7A0B"] })] })] })) }) })] }));
}

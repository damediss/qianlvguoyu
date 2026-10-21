import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, Input, Button, Table, Modal, Tabs, message } from 'antd';
import { ArrowLeftOutlined, FolderOpenOutlined, UnorderedListOutlined, ToolOutlined, EyeOutlined, CheckCircleOutlined, VideoCameraOutlined, FileImageOutlined, EditOutlined, ExclamationCircleOutlined, SaveOutlined, InfoCircleOutlined, CalendarOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TrainingImplementationPage.css';
// ============================================================
// 常量
// ============================================================
const DEFAULT_TASKS = [
    { id: 1, name: '2026年度安全培训计划编制', owner: '张明', enterprise: '广西建工集团', occupation: '安全员', planCount: 120 },
    { id: 2, name: '电工高级班认定材料整理', owner: '李芳', enterprise: '南宁供电局', occupation: '电工', planCount: 42 },
    { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', enterprise: '柳州钢铁有限公司', occupation: '焊工', planCount: 36 },
    { id: 4, name: '焊工中级班排课协调', owner: '赵丽', enterprise: '桂林旅游股份公司', occupation: '焊工', planCount: 28 },
    { id: 5, name: '培训档案年度归档', owner: '陈伟', enterprise: '梧州港务集团', occupation: '起重工', planCount: 15 },
];
function todayStr() {
    return new Date().toISOString().slice(0, 10);
}
// ============================================================
// Mock 数据
// ============================================================
function buildMockSessions(classId, _studentCount) {
    return [
        { id: classId * 1000 + 1, date: '2026-07-01', courseName: '安全生产基础知识', timeRange: '09:00-12:00', expected: 30, present: 30, photos: 0, videos: 0, checked: true },
        { id: classId * 1000 + 2, date: '2026-07-02', courseName: '安全操作规程', timeRange: '09:00-12:00', expected: 30, present: 29, photos: 0, videos: 0, checked: true },
        { id: classId * 1000 + 3, date: '2026-07-03', courseName: '应急救援演练', timeRange: '14:00-17:00', expected: 30, present: 28, photos: 0, videos: 0, checked: true },
        { id: classId * 1000 + 4, date: '2026-07-08', courseName: '安全法律法规', timeRange: '09:00-12:00', expected: 30, present: 30, photos: 0, videos: 0, checked: false },
        { id: classId * 1000 + 5, date: '2026-07-10', courseName: '事故案例分析', timeRange: '09:00-12:00', expected: 30, present: 30, photos: 0, videos: 0, checked: false },
    ];
}
function buildMockRecords(projectId) {
    const task = DEFAULT_TASKS.find((t) => t.id === projectId);
    if (!task)
        return [];
    const s1 = buildMockSessions(1, 30);
    const s2 = buildMockSessions(2, 25);
    return [
        {
            classId: 1, className: `${task.occupation || '电工'}高级工1班`, studentCount: 30,
            progress: 60, attendanceRate: 96.7, status: '正常',
            sessions: s1, evidenceCount: { photos: 2, videos: 1 },
        },
        {
            classId: 2, className: `${task.occupation || '电工'}中级工1班`, studentCount: 25,
            progress: 40, attendanceRate: 92, status: '正常',
            sessions: s2, evidenceCount: { photos: 1, videos: 0 },
        },
    ];
}
// ============================================================
// 主页面
// ============================================================
export function TrainingImplementationPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [records, setRecords] = useState([]);
    const [filterClass, setFilterClass] = useState('');
    const [logs, setLogs] = useState([]);
    const [exceptions, setExceptions] = useState([]);
    const [logIdCounter, setLogIdCounter] = useState(1);
    const [excIdCounter, setExcIdCounter] = useState(1);
    const [activeTab, setActiveTab] = useState('overview');
    // modals
    const [detailModal, setDetailModal] = useState(null);
    const [logModalOpen, setLogModalOpen] = useState(false);
    const [excModalOpen, setExcModalOpen] = useState(false);
    const [logDate, setLogDate] = useState(todayStr());
    const [logClassId, setLogClassId] = useState();
    const [logContent, setLogContent] = useState('');
    const [excDate, setExcDate] = useState(todayStr());
    const [excClassId, setExcClassId] = useState();
    const [excStudent, setExcStudent] = useState('');
    const [excType, setExcType] = useState('缺勤');
    const [excDesc, setExcDesc] = useState('');
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    useEffect(() => {
        if (selectedProjectId) {
            setRecords(buildMockRecords(+selectedProjectId));
            setLogs([]);
            setExceptions([]);
            setLogIdCounter(1);
            setExcIdCounter(1);
        }
        else {
            setRecords([]);
        }
    }, [selectedProjectId]);
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
        setFilterClass('');
    };
    const filteredRecords = useMemo(() => {
        if (!filterClass)
            return records;
        return records.filter((r) => String(r.classId) === filterClass);
    }, [records, filterClass]);
    const stats = useMemo(() => {
        const list = records;
        const att = list.length ? Math.round(list.reduce((s, r) => s + (r.attendanceRate || 0), 0) / list.length * 10) / 10 : 0;
        const prog = list.length ? Math.round(list.reduce((s, r) => s + (r.progress || 0), 0) / list.length) : 0;
        const pendingExc = exceptions.filter((e) => !e.resolved).length;
        return { classes: list.length, attendance: att, progress: prog, exceptions: pendingExc };
    }, [records, exceptions]);
    const refreshMetrics = useCallback((r) => {
        const sessions = r.sessions || [];
        let totalExp = 0, totalPres = 0;
        sessions.forEach((s) => { totalExp += s.expected || 0; totalPres += s.present ?? s.expected; });
        r.attendanceRate = totalExp ? Math.round((totalPres / totalExp) * 1000) / 10 : 0;
        const done = sessions.filter((s) => s.checked || (s.present >= (s.expected || 0) * 0.8)).length;
        r.progress = sessions.length ? Math.round((done / sessions.length) * 100) : 0;
        r.status = r.attendanceRate < 80 ? '异常' : (r.attendanceRate < 90 ? '关注' : '正常');
    }, []);
    const updateRecords = useCallback((updater) => {
        setRecords((prev) => {
            const next = updater(prev.map((r) => ({ ...r, sessions: r.sessions.map((s) => ({ ...s })) })));
            next.forEach(refreshMetrics);
            return next;
        });
    }, [refreshMetrics]);
    const handleBatchCheckIn = () => {
        const today = todayStr();
        let n = 0;
        updateRecords((prev) => prev.map((r) => {
            r.sessions.forEach((s) => {
                if (s.date === today) {
                    s.checked = true;
                    s.present = s.expected;
                    n++;
                }
            });
            return r;
        }));
        message.success(n ? `已为 ${n} 个今日课次完成批量签到` : '今日无待签到课次');
    };
    const handleCheckIn = (classId) => {
        const today = todayStr();
        updateRecords((prev) => prev.map((r) => {
            if (r.classId !== classId)
                return r;
            const session = r.sessions.find((s) => s.date === today) || r.sessions[0];
            if (session) {
                session.checked = true;
                session.present = session.expected;
            }
            return r;
        }));
        const r = records.find((x) => x.classId === classId);
        message.success(`已为「${r?.className || ''}」完成课次签到`);
    };
    const handleUploadPhoto = () => {
        const target = filteredRecords[0] || records[0];
        if (!target) {
            message.warning('请先选择项目并确保有班级');
            return;
        }
        updateRecords((prev) => prev.map((r) => {
            if (r.classId !== target.classId)
                return r;
            r.evidenceCount.photos += 1;
            return r;
        }));
        message.success('课堂照片上传成功（演示）');
    };
    const handleUploadVideo = () => {
        const target = filteredRecords[0] || records[0];
        if (!target) {
            message.warning('请先选择项目并确保有班级');
            return;
        }
        updateRecords((prev) => prev.map((r) => {
            if (r.classId !== target.classId)
                return r;
            r.evidenceCount.videos += 1;
            return r;
        }));
        message.success('课堂视频上传成功（演示）');
    };
    const handleAddLog = () => {
        if (!logClassId || !logContent.trim()) {
            message.warning('请填写完整信息');
            return;
        }
        const r = records.find((x) => x.classId === logClassId);
        const entry = {
            id: logIdCounter, classId: logClassId, className: r?.className || '—',
            date: logDate, author: '班主任', content: logContent.trim(),
        };
        setLogIdCounter((c) => c + 1);
        setLogs((prev) => [entry, ...prev]);
        setLogContent('');
        setLogModalOpen(false);
        message.success('跟班日志已保存');
    };
    const handleAddException = () => {
        if (!excClassId) {
            message.warning('请选择班级');
            return;
        }
        const r = records.find((x) => x.classId === excClassId);
        const entry = {
            id: excIdCounter, classId: excClassId, className: r?.className || '—',
            date: excDate, student: excStudent.trim(), type: excType, desc: excDesc.trim(), resolved: false,
        };
        setExcIdCounter((c) => c + 1);
        setExceptions((prev) => [entry, ...prev]);
        if (r) {
            updateRecords((prev) => prev.map((x) => (x.classId === r.classId ? { ...x, status: '关注' } : x)));
        }
        setExcStudent('');
        setExcDesc('');
        setExcModalOpen(false);
        message.success('异常已登记');
    };
    const handleResolveExc = (id) => {
        setExceptions((prev) => prev.map((e) => (e.id === id ? { ...e, resolved: true } : e)));
        message.success('异常已标记为已处理');
    };
    const filteredLogs = useMemo(() => {
        let list = [...logs].sort((a, b) => b.date.localeCompare(a.date));
        if (filterClass)
            list = list.filter((l) => String(l.classId) === filterClass);
        return list;
    }, [logs, filterClass]);
    const filteredExc = useMemo(() => {
        let list = [...exceptions].sort((a, b) => b.date.localeCompare(a.date));
        if (filterClass)
            list = list.filter((e) => String(e.classId) === filterClass);
        return list;
    }, [exceptions, filterClass]);
    // 班级概览列
    const classColumns = [
        { title: '班级', dataIndex: 'className', render: (v) => _jsx("strong", { children: v }) },
        { title: '学员', dataIndex: 'studentCount', render: (v) => `${v} 人` },
        {
            title: '课程进度', key: 'progress',
            render: (_, r) => (_jsxs("div", { style: { display: 'flex', alignItems: 'center', gap: 8 }, children: [_jsx("div", { className: "tpx-progress-bar", children: _jsx("div", { className: "tpx-progress-fill", style: { width: `${r.progress || 0}%` } }) }), _jsxs("span", { style: { fontSize: 13, fontWeight: 600 }, children: [r.progress || 0, "%"] })] })),
        },
        { title: '签到率', dataIndex: 'attendanceRate', render: (v) => `${v || 0}%` },
        { title: '留痕', key: 'evidence', render: (_, r) => { const ev = r.evidenceCount || { photos: 0, videos: 0 }; return `${ev.photos}图 ${ev.videos}视频`; } },
        { title: '状态', dataIndex: 'status', render: (v) => _jsx("span", { className: `status-badge ${v === '异常' ? 'st-danger' : v === '关注' ? 'st-warn' : 'st-normal'}`, children: v || '正常' }) },
        {
            title: '操作', key: 'actions',
            render: (_, r) => (_jsxs("div", { className: "action-btns", onClick: (e) => e.stopPropagation(), children: [_jsx("button", { className: "action-btn", title: "\u8BE6\u60C5", onClick: () => setDetailModal(r), children: _jsx(EyeOutlined, {}) }), _jsx("button", { className: "action-btn", title: "\u7B7E\u5230", onClick: () => handleCheckIn(r.classId), children: _jsx(CheckCircleOutlined, {}) })] })),
        },
    ];
    return (_jsxs("div", { className: "tpx-page", children: [_jsxs("header", { className: "tpx-topbar", children: [_jsxs("div", { className: "tpx-topbar-left", children: [_jsxs("a", { className: "tpx-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "tpx-title", children: "\u57F9\u8BAD\u5B9E\u65BD" }), _jsx("span", { className: "tpx-sub", children: currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 5 / 9` : '按项目跟踪培训过程 · 步骤 5 / 9' })] })] }), _jsxs("span", { className: "tpx-process-badge", children: [_jsx(ToolOutlined, {}), " \u2026 \u2192 \u57F9\u8BAD\u5B9E\u65BD \u2192 \u8BA4\u5B9A\u51C6\u5907 \u2192 \u2026"] })] }), _jsxs("main", { className: "tpx-page-body", children: [_jsxs("div", { className: "tpx-card", children: [_jsxs("div", { className: "tpx-card-head", children: [_jsxs("div", { className: "tpx-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "tpx-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE\uFF08\u57F9\u8BAD\u9488\u5BF9\u5DF2\u5F00\u73ED\u73ED\u7EA7\uFF09" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && (_jsxs("div", { className: "tpx-project-banner", children: [_jsxs("div", { className: "tpx-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "tpx-banner-item", children: [_jsx("label", { children: "\u57F9\u8BAD\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "tpx-banner-item", children: [_jsx("label", { children: "\u5728\u57F9\u73ED\u7EA7" }), _jsxs("strong", { children: [records.length, " \u4E2A"] })] }), _jsxs("div", { className: "tpx-banner-item", children: [_jsx("label", { children: "\u5DE5\u79CD" }), _jsx("strong", { children: currentProject.occupation || '—' })] }), _jsxs("div", { className: "tpx-banner-item", children: [_jsx("label", { children: "\u8D1F\u8D23\u4EBA" }), _jsx("strong", { children: currentProject.owner || '—' })] })] }))] }), !currentProject ? (_jsxs("div", { className: "tpx-empty", children: [_jsx(InfoCircleOutlined, { style: { fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u518D\u7BA1\u7406\u5404\u73ED\u7EA7\u57F9\u8BAD\u5B9E\u65BD\u4E0E\u8003\u52E4\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u9700\u5148\u5B8C\u6210 \u5F00\u73ED\uFF1B\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u8FDB\u5165\u3002" })] })) : records.length === 0 ? (_jsxs("div", { className: "tpx-alert warn", children: [_jsx(ExclamationCircleOutlined, {}), _jsx("div", { children: "\u672C\u9879\u76EE\u5C1A\u65E0\u5728\u57F9\u73ED\u7EA7\uFF0C\u8BF7\u5148 \u5F00\u73ED\u3002" })] })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "tpx-stat-row", children: [_jsxs("div", { className: "tpx-stat-card", children: [_jsx("div", { className: "tpx-stat-val", children: stats.classes }), _jsx("div", { className: "tpx-stat-lbl", children: "\u5728\u57F9\u73ED\u7EA7" })] }), _jsxs("div", { className: "tpx-stat-card", children: [_jsxs("div", { className: "tpx-stat-val success", children: [stats.attendance, "%"] }), _jsx("div", { className: "tpx-stat-lbl", children: "\u5E73\u5747\u7B7E\u5230\u7387" })] }), _jsxs("div", { className: "tpx-stat-card", children: [_jsxs("div", { className: "tpx-stat-val", children: [stats.progress, "%"] }), _jsx("div", { className: "tpx-stat-lbl", children: "\u8BFE\u7A0B\u8FDB\u5EA6" })] }), _jsxs("div", { className: "tpx-stat-card", children: [_jsx("div", { className: "tpx-stat-val warn", children: stats.exceptions }), _jsx("div", { className: "tpx-stat-lbl", children: "\u5F85\u5904\u7406\u5F02\u5E38" })] })] }), _jsxs("div", { className: "tpx-card", children: [_jsxs("div", { className: "tpx-card-head", children: [_jsxs("div", { className: "tpx-card-head-title", children: [_jsx(ToolOutlined, {}), " \u57F9\u8BAD\u8FC7\u7A0B"] }), _jsx("span", { className: "tpx-hint", children: "\u8003\u52E4\u7BA1\u7406 \u00B7 \u8FC7\u7A0B\u7559\u75D5 \u00B7 \u8DDF\u73ED\u65E5\u5FD7 \u00B7 \u5F02\u5E38\u5904\u7406" })] }), _jsx("div", { className: "tpx-toolbar", children: _jsxs("div", { className: "tpx-form-group", style: { minWidth: 180, margin: 0 }, children: [_jsx("label", { children: "\u7B5B\u9009\u73ED\u7EA7" }), _jsx(Select, { placeholder: "\u5168\u90E8\u73ED\u7EA7", value: filterClass || undefined, onChange: (v) => setFilterClass(v || ''), allowClear: true, style: { width: '100%' }, options: records.map((r) => ({ value: String(r.classId), label: r.className })) })] }) }), _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                                            {
                                                key: 'overview', label: '班级概览',
                                                children: (_jsx(Table, { dataSource: filteredRecords, columns: classColumns, rowKey: "classId", pagination: false, locale: { emptyText: '暂无在培班级' } })),
                                            },
                                            {
                                                key: 'attendance', label: '课次考勤',
                                                children: (_jsxs("div", { children: [_jsx("div", { className: "tpx-toolbar", children: _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), onClick: handleBatchCheckIn, children: "\u4ECA\u65E5\u8BFE\u6B21\u6279\u91CF\u7B7E\u5230" }) }), filteredRecords.length === 0 ? _jsx("p", { style: { color: '#64748B' }, children: "\u6682\u65E0\u8BFE\u6B21" }) : (filteredRecords.map((r) => {
                                                            const sessions = [...r.sessions].sort((a, b) => (a.date + a.timeRange).localeCompare(b.date + b.timeRange));
                                                            return (_jsxs("div", { className: "tpx-att-card", children: [_jsx("strong", { children: r.className }), sessions.map((s) => (_jsxs("div", { className: "tpx-att-row", children: [_jsx("span", { style: { minWidth: 100 }, children: s.date }), _jsxs("span", { style: { flex: 1 }, children: [s.courseName, " \u00B7 ", s.timeRange] }), _jsxs("span", { children: ["\u5E94\u5230 ", s.expected, " / \u5B9E\u5230", ' ', _jsx(Input, { size: "small", type: "number", min: 0, max: s.expected, value: s.present, style: { width: 56 }, onChange: (e) => {
                                                                                            updateRecords((prev) => prev.map((x) => {
                                                                                                if (x.classId !== r.classId)
                                                                                                    return x;
                                                                                                const sess = x.sessions.find((ss) => ss.id === s.id);
                                                                                                if (sess)
                                                                                                    sess.present = parseInt(e.target.value) || 0;
                                                                                                return x;
                                                                                            }));
                                                                                        } })] }), _jsxs("label", { className: "tpx-check-label", children: [_jsx("input", { type: "checkbox", checked: s.checked, onChange: (e) => {
                                                                                            updateRecords((prev) => prev.map((x) => {
                                                                                                if (x.classId !== r.classId)
                                                                                                    return x;
                                                                                                const sess = x.sessions.find((ss) => ss.id === s.id);
                                                                                                if (sess)
                                                                                                    sess.checked = e.target.checked;
                                                                                                return x;
                                                                                            }));
                                                                                        } }), ' ', "\u5DF2\u7B7E\u5230"] })] }, s.id)))] }, r.classId));
                                                        }))] })),
                                            },
                                            {
                                                key: 'evidence', label: '过程留痕',
                                                children: (_jsxs("div", { children: [_jsxs("div", { className: "tpx-toolbar", children: [_jsx(Button, { icon: _jsx(FileImageOutlined, {}), onClick: handleUploadPhoto, children: "\u4E0A\u4F20\u8BFE\u5802\u7167\u7247" }), _jsx(Button, { icon: _jsx(VideoCameraOutlined, {}), onClick: handleUploadVideo, children: "\u4E0A\u4F20\u8BFE\u5802\u89C6\u9891" })] }), (() => {
                                                            const totalP = filteredRecords.reduce((s, r) => s + (r.evidenceCount?.photos || 0), 0);
                                                            const totalV = filteredRecords.reduce((s, r) => s + (r.evidenceCount?.videos || 0), 0);
                                                            if (!totalP && !totalV) {
                                                                return _jsx("p", { style: { color: '#64748B' }, children: "\u6682\u65E0\u7559\u75D5\uFF0C\u8BF7\u4E0A\u4F20\u8BFE\u5802\u7167\u7247\u6216\u89C6\u9891\uFF08\u5BF9\u5E94\u9700\u6C42\uFF1A\u7B7E\u5230\u8868\u3001\u56FE\u7247\u3001\u89C6\u9891\u4E0A\u4F20\uFF09" });
                                                            }
                                                            return (_jsxs("div", { children: [_jsxs("p", { style: { marginBottom: 12, fontSize: 14 }, children: ["\u7D2F\u8BA1\uFF1A", totalP, " \u5F20\u7167\u7247\uFF0C", totalV, " \u4E2A\u89C6\u9891\uFF08\u6F14\u793A\uFF09"] }), _jsx("div", { className: "tpx-evidence-grid", children: filteredRecords.map((r) => {
                                                                            const n = (r.evidenceCount?.photos || 0) + (r.evidenceCount?.videos || 0);
                                                                            const items = [];
                                                                            for (let i = 0; i < Math.min(n, 3); i++) {
                                                                                const isVid = i >= (r.evidenceCount?.photos || 0);
                                                                                items.push(_jsxs("div", { className: "tpx-evidence-thumb", children: [isVid ? _jsx(VideoCameraOutlined, { style: { fontSize: 28, color: '#2A6DFF' } }) : _jsx(FileImageOutlined, { style: { fontSize: 28, color: '#2A6DFF' } }), _jsx("span", { children: r.className })] }, i));
                                                                            }
                                                                            return items;
                                                                        }) })] }));
                                                        })()] })),
                                            },
                                            {
                                                key: 'logs', label: '跟班日志',
                                                children: (_jsxs("div", { children: [_jsx("div", { className: "tpx-toolbar", children: _jsx(Button, { type: "primary", icon: _jsx(EditOutlined, {}), onClick: () => { setLogDate(todayStr()); setLogModalOpen(true); }, children: "\u65B0\u589E\u8DDF\u73ED\u65E5\u5FD7" }) }), filteredLogs.length === 0 ? _jsx("p", { style: { color: '#64748B' }, children: "\u6682\u65E0\u8DDF\u73ED\u65E5\u5FD7" }) : (filteredLogs.map((l) => (_jsxs("div", { className: "tpx-log-item", children: [_jsxs("div", { className: "tpx-log-meta", children: [l.date, " \u00B7 ", l.className, " \u00B7 ", l.author] }), l.content] }, l.id))))] })),
                                            },
                                            {
                                                key: 'exceptions', label: '异常处理',
                                                children: (_jsxs("div", { children: [_jsx("div", { className: "tpx-toolbar", children: _jsx(Button, { type: "primary", icon: _jsx(ExclamationCircleOutlined, {}), onClick: () => { setExcDate(todayStr()); setExcModalOpen(true); }, children: "\u767B\u8BB0\u5F02\u5E38" }) }), filteredExc.length === 0 ? _jsx("p", { style: { color: '#64748B' }, children: "\u6682\u65E0\u5F02\u5E38\u8BB0\u5F55" }) : (filteredExc.map((e) => (_jsxs("div", { className: "tpx-exc-item", children: [_jsxs("div", { className: "tpx-exc-meta", children: [e.date, " \u00B7 ", e.className, " \u00B7 ", e.type, " ", e.student ? '· ' + e.student : '', e.resolved
                                                                            ? _jsx("span", { className: "status-badge st-normal", children: "\u5DF2\u5904\u7406" })
                                                                            : _jsx("span", { className: "status-badge st-warn", children: "\u5F85\u5904\u7406" })] }), e.desc || '—', !e.resolved && (_jsx(Button, { size: "small", style: { marginTop: 8 }, onClick: () => handleResolveExc(e.id), children: "\u6807\u8BB0\u5DF2\u5904\u7406" }))] }, e.id))))] })),
                                            },
                                        ] })] }), _jsxs("div", { className: "tpx-footer-actions", children: [_jsx(Button, { icon: _jsx(CalendarOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-class-declaration'); }, children: "\u8FD4\u56DE\u5F00\u73ED" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), style: { background: '#10B981', borderColor: '#10B981' }, onClick: () => {
                                            if (!currentProject) {
                                                message.warning('请先选择项目');
                                                return;
                                            }
                                            message.success(`项目「${currentProject.name}」培训过程已保存，进入认定环节`);
                                            setCurrentProjectId(currentProject.id);
                                            navigateTo('zhixun-certification');
                                        }, children: "\u4FDD\u5B58\u57F9\u8BAD\u8FDB\u5EA6" })] }), _jsx(Modal, { open: !!detailModal, onCancel: () => setDetailModal(null), title: detailModal?.className + ' · 培训详情', footer: _jsx(Button, { onClick: () => setDetailModal(null), children: "\u5173\u95ED" }), children: detailModal && (_jsxs("div", { children: [_jsxs("p", { children: [_jsx("strong", { children: "\u5B66\u5458\uFF1A" }), detailModal.studentCount, " \u4EBA \u00B7 ", _jsx("strong", { children: "\u8FDB\u5EA6\uFF1A" }), detailModal.progress, "% \u00B7 ", _jsx("strong", { children: "\u7B7E\u5230\u7387\uFF1A" }), detailModal.attendanceRate, "%"] }), _jsxs("p", { style: { marginTop: 12 }, children: [_jsx("strong", { children: "\u8BFE\u6B21\uFF1A" }), (detailModal.sessions || []).length, " \u8282"] }), _jsxs("p", { style: { marginTop: 8 }, children: [_jsx("strong", { children: "\u7559\u75D5\uFF1A" }), detailModal.evidenceCount?.photos || 0, " \u7167\u7247 / ", detailModal.evidenceCount?.videos || 0, " \u89C6\u9891"] }), _jsxs("p", { style: { marginTop: 8 }, children: [_jsx("strong", { children: "\u72B6\u6001\uFF1A" }), detailModal.status] })] })) }), _jsx(Modal, { open: logModalOpen, onCancel: () => setLogModalOpen(false), title: "\u65B0\u589E\u8DDF\u73ED\u65E5\u5FD7", footer: _jsxs("div", { style: { display: 'flex', gap: 10, justifyContent: 'flex-end' }, children: [_jsx(Button, { onClick: () => setLogModalOpen(false), children: "\u53D6\u6D88" }), _jsx(Button, { type: "primary", onClick: handleAddLog, children: "\u4FDD\u5B58" })] }), children: _jsx("div", { className: "tpx-modal-form", children: _jsxs("div", { className: "tpx-form-grid", children: [_jsxs("div", { className: "tpx-form-group", children: [_jsx("label", { children: "\u73ED\u7EA7" }), _jsx(Select, { value: logClassId, onChange: setLogClassId, style: { width: '100%' }, placeholder: "\u9009\u62E9\u73ED\u7EA7", options: records.map((r) => ({ value: r.classId, label: r.className })) })] }), _jsxs("div", { className: "tpx-form-group", children: [_jsx("label", { children: "\u65E5\u671F" }), _jsx(Input, { type: "date", value: logDate, onChange: (e) => setLogDate(e.target.value) })] }), _jsxs("div", { className: "tpx-form-group", style: { gridColumn: '1/-1' }, children: [_jsx("label", { children: "\u65E5\u5FD7\u5185\u5BB9" }), _jsx(Input.TextArea, { rows: 3, value: logContent, onChange: (e) => setLogContent(e.target.value), placeholder: "\u8003\u52E4\u60C5\u51B5\u3001\u8BFE\u5802\u8868\u73B0\u3001\u5F02\u5E38\u5B66\u5458\u6C9F\u901A\u7B49" })] })] }) }) }), _jsx(Modal, { open: excModalOpen, onCancel: () => setExcModalOpen(false), title: "\u767B\u8BB0\u57F9\u8BAD\u5F02\u5E38", footer: _jsxs("div", { style: { display: 'flex', gap: 10, justifyContent: 'flex-end' }, children: [_jsx(Button, { onClick: () => setExcModalOpen(false), children: "\u53D6\u6D88" }), _jsx(Button, { type: "primary", onClick: handleAddException, children: "\u4FDD\u5B58" })] }), children: _jsx("div", { className: "tpx-modal-form", children: _jsxs("div", { className: "tpx-form-grid", children: [_jsxs("div", { className: "tpx-form-group", children: [_jsx("label", { children: "\u73ED\u7EA7" }), _jsx(Select, { value: excClassId, onChange: setExcClassId, style: { width: '100%' }, placeholder: "\u9009\u62E9\u73ED\u7EA7", options: records.map((r) => ({ value: r.classId, label: r.className })) })] }), _jsxs("div", { className: "tpx-form-group", children: [_jsx("label", { children: "\u65E5\u671F" }), _jsx(Input, { type: "date", value: excDate, onChange: (e) => setExcDate(e.target.value) })] }), _jsxs("div", { className: "tpx-form-group", children: [_jsx("label", { children: "\u5B66\u5458\u59D3\u540D" }), _jsx(Input, { placeholder: "\u53EF\u9009", value: excStudent, onChange: (e) => setExcStudent(e.target.value) })] }), _jsxs("div", { className: "tpx-form-group", children: [_jsx("label", { children: "\u5F02\u5E38\u7C7B\u578B" }), _jsx(Select, { value: excType, onChange: setExcType, style: { width: '100%' }, options: ['缺勤', '迟到', '课堂违纪', '伤病请假', '其他'].map((v) => ({ value: v, label: v })) })] }), _jsxs("div", { className: "tpx-form-group", style: { gridColumn: '1/-1' }, children: [_jsx("label", { children: "\u8BF4\u660E" }), _jsx(Input.TextArea, { rows: 3, value: excDesc, onChange: (e) => setExcDesc(e.target.value) })] })] }) }) })] }))] })] }));
}

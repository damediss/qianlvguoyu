import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, Button, Table, Tabs, message } from 'antd';
import { ArrowLeftOutlined, FolderOpenOutlined, UnorderedListOutlined, TrophyOutlined, BarChartOutlined, FileZipOutlined, BellOutlined, FileAddOutlined, NodeIndexOutlined, InfoCircleOutlined, ExclamationCircleOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './GraduationPage.css';
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
const FLOW_LABELS = ['结课统计', '材料整理', '结课通知', '证书归档'];
const FLOW_ICONS = [BarChartOutlined, FolderOpenOutlined, BellOutlined, FileZipOutlined];
function todayStr() {
    return new Date().toISOString().slice(0, 10);
}
// ============================================================
// Mock 数据
// ============================================================
const MOCK_STUDENTS = [
    { studentId: 1, name: '张建国', classId: 1, className: '安全员高级工1班' },
    { studentId: 2, name: '李秀英', classId: 1, className: '安全员高级工1班' },
    { studentId: 3, name: '王志强', classId: 1, className: '安全员高级工1班' },
    { studentId: 4, name: '陈丽华', classId: 1, className: '安全员高级工1班' },
    { studentId: 5, name: '刘明辉', classId: 1, className: '安全员高级工1班' },
    { studentId: 6, name: '赵红梅', classId: 2, className: '安全员中级工1班' },
    { studentId: 7, name: '周建华', classId: 2, className: '安全员中级工1班' },
    { studentId: 8, name: '吴桂芳', classId: 2, className: '安全员中级工1班' },
];
function buildMockGraduates(projectId) {
    const task = DEFAULT_TASKS.find((t) => t.id === projectId);
    if (!task)
        return [];
    const cls1 = `${task.occupation || '电工'}高级工1班`;
    const cls2 = `${task.occupation || '电工'}中级工1班`;
    return MOCK_STUDENTS.map((s, i) => {
        const isIssued = i < 5;
        return {
            studentId: s.studentId,
            name: s.name,
            classId: s.classId,
            className: s.classId === 1 ? cls1 : cls2,
            rendingResult: '通过',
            graduateCertStatus: isIssued ? '已发证' : '待发证',
            graduateCertNo: isIssued ? `JY${projectId}${String(s.studentId).padStart(4, '0')}` : '',
            graduateDate: isIssued ? '2026-08-01' : '',
            archived: isIssued,
            notified: isIssued,
        };
    });
}
// ============================================================
// 主页面
// ============================================================
export function GraduationPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [graduates, setGraduates] = useState([]);
    const [statsDone, setStatsDone] = useState(false);
    const [packageReady, setPackageReady] = useState(false);
    const [notified, setNotified] = useState(false);
    const [activeTab, setActiveTab] = useState('class');
    const [filterClass, setFilterClass] = useState('');
    const [filterCert, setFilterCert] = useState('');
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    useEffect(() => {
        if (selectedProjectId) {
            setGraduates(buildMockGraduates(+selectedProjectId));
            setStatsDone(false);
            setPackageReady(false);
            setNotified(false);
        }
        else {
            setGraduates([]);
        }
    }, [selectedProjectId]);
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
        setFilterClass('');
        setFilterCert('');
    };
    const updateGraduates = useCallback((updater) => {
        setGraduates((prev) => updater(prev.map((g) => ({ ...g }))));
    }, []);
    const classSummaries = useMemo(() => {
        const map = {};
        graduates.forEach((g) => {
            if (!map[g.classId]) {
                map[g.classId] = { classId: g.classId, className: g.className, total: 0, issued: 0, pending: 0, graduateDate: g.graduateDate || '' };
            }
            const c = map[g.classId];
            c.total++;
            if (g.graduateCertStatus === '已发证')
                c.issued++;
            else
                c.pending++;
            if (g.graduateDate)
                c.graduateDate = g.graduateDate;
        });
        return Object.values(map);
    }, [graduates]);
    const stats = useMemo(() => {
        const total = graduates.length;
        const issued = graduates.filter((g) => g.graduateCertStatus === '已发证').length;
        const pending = total - issued;
        const archived = graduates.filter((g) => g.archived).length;
        return { total, issued, pending, archived };
    }, [graduates]);
    const flowStep = useMemo(() => {
        const arcCount = graduates.filter((g) => g.archived).length;
        if (arcCount === graduates.length && graduates.length)
            return 4;
        if (graduates.every((g) => g.graduateCertStatus === '已发证') && graduates.length)
            return 3;
        if (notified)
            return 2;
        if (packageReady || statsDone)
            return 1;
        return 0;
    }, [graduates, notified, packageReady, statsDone]);
    const classNames = useMemo(() => [...new Set(graduates.map((g) => g.className))], [graduates]);
    const filteredGraduates = useMemo(() => {
        return graduates.filter((g) => {
            if (filterClass && g.className !== filterClass)
                return false;
            if (filterCert && g.graduateCertStatus !== filterCert)
                return false;
            return true;
        });
    }, [graduates, filterClass, filterCert]);
    const handleStats = () => {
        setStatsDone(true);
        const issued = graduates.filter((g) => g.graduateCertStatus === '已发证').length;
        message.success(`结课统计完成：应结业 ${graduates.length} 人，已发证 ${issued} 人，待发 ${graduates.length - issued} 人`);
    };
    const handlePackage = () => { setPackageReady(true); message.success('结课材料包已整理（含考勤、过程留痕、考核与获证汇总）'); };
    const handleNotify = () => {
        setNotified(true);
        updateGraduates((prev) => prev.map((g) => ({ ...g, notified: true })));
        message.success(`已向企业与学员发送结课通知（${graduates.length} 人）`);
    };
    const handleBatchCert = () => {
        const pending = graduates.filter((g) => g.graduateCertStatus !== '已发证');
        if (!pending.length) {
            message.warning('暂无待发证学员');
            return;
        }
        const today = todayStr();
        updateGraduates((prev) => prev.map((g) => {
            if (g.graduateCertStatus === '已发证')
                return g;
            return { ...g, graduateCertStatus: '已发证', graduateCertNo: `JY${selectedProjectId}${String(g.studentId).padStart(4, '0')}`, graduateDate: today };
        }));
        message.success(`已为 ${pending.length} 名学员发放结业证书`);
    };
    const handleArchive = () => {
        const issued = graduates.filter((g) => g.graduateCertStatus === '已发证');
        if (!issued.length) {
            message.warning('请先发放结业证');
            return;
        }
        updateGraduates((prev) => prev.map((g) => (g.graduateCertStatus === '已发证' ? { ...g, archived: true } : g)));
        message.success(`已归档 ${issued.length} 名学员培训档案`);
    };
    const handleIssueOne = (studentId) => {
        updateGraduates((prev) => prev.map((g) => {
            if (g.studentId !== studentId || g.graduateCertStatus === '已发证')
                return g;
            return { ...g, graduateCertStatus: '已发证', graduateCertNo: `JY${selectedProjectId}${String(g.studentId).padStart(4, '0')}`, graduateDate: todayStr() };
        }));
    };
    const classColumns = [
        { title: '班级', dataIndex: 'className', render: (v) => _jsx("strong", { children: v }) },
        { title: '应结业', dataIndex: 'total' },
        { title: '已发证', dataIndex: 'issued' },
        { title: '待发', dataIndex: 'pending' },
        { title: '结课日期', dataIndex: 'graduateDate', render: (v) => v || '—' },
        { title: '材料包', key: 'package', render: () => _jsx("span", { className: `status-badge ${packageReady ? 'st-done' : 'st-draft'}`, children: packageReady ? '已整理' : '待整理' }) },
    ];
    const studentColumns = [
        { title: '姓名', dataIndex: 'name', render: (v) => _jsx("strong", { children: v }) },
        { title: '班级', dataIndex: 'className' },
        { title: '认定结果', dataIndex: 'rendingResult' },
        {
            title: '结业证', key: 'cert',
            render: (_, r) => (_jsxs("div", { children: [_jsx("span", { className: `status-badge ${r.graduateCertStatus === '已发证' ? 'st-done' : 'st-pending'}`, children: r.graduateCertStatus }), r.graduateCertNo && _jsx("br", {}), r.graduateCertNo && _jsx("small", { children: r.graduateCertNo })] })),
        },
        { title: '结业日期', dataIndex: 'graduateDate', render: (v) => v || '—' },
        { title: '档案', dataIndex: 'archived', render: (v) => _jsx("span", { className: `status-badge ${v ? 'st-done' : 'st-draft'}`, children: v ? '已归档' : '未归档' }) },
        {
            title: '操作', key: 'actions',
            render: (_, r) => (_jsx(Button, { size: "small", disabled: r.graduateCertStatus === '已发证', onClick: () => handleIssueOne(r.studentId), children: r.graduateCertStatus === '已发证' ? '已发证' : '发证' })),
        },
    ];
    return (_jsxs("div", { className: "jy-page", children: [_jsxs("header", { className: "jy-topbar", children: [_jsxs("div", { className: "jy-topbar-left", children: [_jsxs("a", { className: "jy-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "jy-title", children: "\u57F9\u8BAD\u7ED3\u4E1A" }), _jsx("span", { className: "jy-sub", children: currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 8 / 9` : '培训结业与档案归档 · 步骤 8 / 9' })] })] }), _jsxs("span", { className: "jy-process-badge", children: [_jsx(TrophyOutlined, {}), " \u2026 \u2192 \u57F9\u8BAD\u7ED3\u4E1A \u2192 \u9879\u76EE\u56DE\u6B3E \u2192 \u2026"] })] }), _jsxs("main", { className: "jy-page-body", children: [_jsxs("div", { className: "jy-card", children: [_jsxs("div", { className: "jy-card-head", children: [_jsxs("div", { className: "jy-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "jy-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && (_jsxs("div", { className: "jy-project-banner", children: [_jsxs("div", { className: "jy-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "jy-banner-item", children: [_jsx("label", { children: "\u57F9\u8BAD\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "jy-banner-item", children: [_jsx("label", { children: "\u5E94\u7ED3\u4E1A" }), _jsxs("strong", { children: [graduates.length, " \u4EBA"] })] }), _jsxs("div", { className: "jy-banner-item", children: [_jsx("label", { children: "\u5DF2\u53D1\u8BC1" }), _jsxs("strong", { children: [graduates.filter((g) => g.graduateCertStatus === '已发证').length, " \u4EBA"] })] }), _jsxs("div", { className: "jy-banner-item", children: [_jsx("label", { children: "\u7ED3\u8BFE\u6750\u6599" }), _jsx("strong", { children: packageReady ? '已整理' : '未整理' })] })] }))] }), !currentProject ? (_jsxs("div", { className: "jy-empty", children: [_jsx(InfoCircleOutlined, { style: { fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u518D\u529E\u7406\u7ED3\u4E1A\u7EDF\u8BA1\u3001\u8BC1\u4E66\u53D1\u653E\u4E0E\u6863\u6848\u5F52\u6863\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u5EFA\u8BAE\u5148\u5B8C\u6210\u57F9\u8BAD\u4E0E\u8BA4\u5B9A\uFF1B\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u8FDB\u5165\u3002" })] })) : graduates.length === 0 ? (_jsxs("div", { className: "jy-alert warn", children: [_jsx(ExclamationCircleOutlined, {}), _jsx("div", { children: "\u672C\u9879\u76EE\u5C1A\u65E0\u5B66\u5458\u6570\u636E\uFF0C\u8BF7\u5148\u5B8C\u6210 \u5206\u73ED\u3002" })] })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "jy-stat-row", children: [_jsxs("div", { className: "jy-stat-card", children: [_jsx("div", { className: "jy-stat-val", children: stats.total }), _jsx("div", { className: "jy-stat-lbl", children: "\u5E94\u7ED3\u4E1A\u4EBA\u6570" })] }), _jsxs("div", { className: "jy-stat-card", children: [_jsx("div", { className: "jy-stat-val success", children: stats.issued }), _jsx("div", { className: "jy-stat-lbl", children: "\u8BC1\u4E66\u5DF2\u53D1" })] }), _jsxs("div", { className: "jy-stat-card", children: [_jsx("div", { className: "jy-stat-val warn", children: stats.pending }), _jsx("div", { className: "jy-stat-lbl", children: "\u5F85\u53D1\u8BC1" })] }), _jsxs("div", { className: "jy-stat-card", children: [_jsx("div", { className: "jy-stat-val", children: stats.archived }), _jsx("div", { className: "jy-stat-lbl", children: "\u5DF2\u5F52\u6863" })] })] }), _jsxs("div", { className: "jy-card", children: [_jsxs("div", { className: "jy-card-head", children: [_jsxs("div", { className: "jy-card-head-title", children: [_jsx(NodeIndexOutlined, {}), " \u7ED3\u4E1A\u6D41\u7A0B"] }), _jsx("span", { className: "jy-hint", children: "\u7ED3\u8BFE\u7EDF\u8BA1 \u2192 \u6750\u6599\u6574\u7406 \u2192 \u7ED3\u8BFE\u901A\u77E5 \u2192 \u8BC1\u4E66/\u6863\u6848\u5F52\u6863" })] }), _jsx("div", { className: "jy-flow-steps", children: FLOW_LABELS.map((label, i) => {
                                            const Icon = FLOW_ICONS[i];
                                            let cls = 'jy-flow-step';
                                            if (i < flowStep)
                                                cls += ' done';
                                            else if (i === flowStep)
                                                cls += ' active';
                                            return (_jsxs("div", { className: cls, children: [_jsx(Icon, { style: { display: 'block', fontSize: 18, marginBottom: 6 } }), label] }, i));
                                        }) }), _jsxs("div", { className: "jy-toolbar", children: [_jsx(Button, { type: "primary", icon: _jsx(BarChartOutlined, {}), onClick: handleStats, children: "\u7ED3\u8BFE\u7EDF\u8BA1" }), _jsx(Button, { icon: _jsx(FileZipOutlined, {}), onClick: handlePackage, children: "\u6574\u7406\u7ED3\u8BFE\u6750\u6599\u5305" }), _jsx(Button, { icon: _jsx(BellOutlined, {}), onClick: handleNotify, children: "\u53D1\u9001\u7ED3\u8BFE\u901A\u77E5" }), _jsx(Button, { style: { background: '#10B981', color: '#fff', borderColor: '#10B981' }, icon: _jsx(TrophyOutlined, {}), onClick: handleBatchCert, children: "\u6279\u91CF\u53D1\u653E\u7ED3\u4E1A\u8BC1" }), _jsx(Button, { icon: _jsx(FolderOpenOutlined, {}), onClick: handleArchive, children: "\u5B66\u5458\u6863\u6848\u5F52\u6863" })] }), _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                                            {
                                                key: 'class', label: '班级汇总',
                                                children: (_jsx(Table, { dataSource: classSummaries, columns: classColumns, rowKey: "classId", pagination: false, locale: { emptyText: '暂无班级' } })),
                                            },
                                            {
                                                key: 'student', label: '学员明细',
                                                children: (_jsxs("div", { children: [_jsxs("div", { className: "jy-filter-bar", children: [_jsxs("div", { className: "jy-form-group", style: { minWidth: 160, margin: 0 }, children: [_jsx("label", { children: "\u73ED\u7EA7" }), _jsx(Select, { placeholder: "\u5168\u90E8", value: filterClass || undefined, onChange: (v) => setFilterClass(v || ''), allowClear: true, style: { width: '100%' }, options: classNames.map((n) => ({ value: n, label: n })) })] }), _jsxs("div", { className: "jy-form-group", style: { minWidth: 160, margin: 0 }, children: [_jsx("label", { children: "\u8BC1\u4E66\u72B6\u6001" }), _jsx(Select, { placeholder: "\u5168\u90E8", value: filterCert || undefined, onChange: (v) => setFilterCert(v || ''), allowClear: true, style: { width: '100%' }, options: ['待发证', '已发证'].map((v) => ({ value: v, label: v })) })] })] }), _jsx(Table, { dataSource: filteredGraduates, columns: studentColumns, rowKey: "studentId", pagination: false, locale: { emptyText: '暂无学员' } })] })),
                                            },
                                        ] })] }), _jsxs("div", { className: "jy-footer-actions", children: [_jsx(Button, { icon: _jsx(FileAddOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-declaration-service'); }, children: "\u8FD4\u56DE\u7533\u62A5" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { type: "primary", style: { background: '#10B981', borderColor: '#10B981' }, icon: _jsx(ArrowLeftOutlined, { style: { transform: 'rotate(180deg)' } }), onClick: () => {
                                            if (!currentProject) {
                                                message.warning('请先选择项目');
                                                return;
                                            }
                                            setCurrentProjectId(+selectedProjectId);
                                            navigateTo('zhixun-refund');
                                        }, children: "\u5B8C\u6210\u57F9\u8BAD\u7ED3\u4E1A\uFF0C\u8FDB\u5165\u9879\u76EE\u56DE\u6B3E" })] })] }))] })] }));
}

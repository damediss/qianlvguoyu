import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback } from 'react';
import { Select, Input, Button, Table, Tabs, message, Progress } from 'antd';
import { ArrowLeftOutlined, DatabaseOutlined, FilterOutlined, UnorderedListOutlined, SyncOutlined, CloudUploadOutlined, AuditOutlined, FileZipOutlined, TrophyOutlined, FilePdfOutlined, FileExcelOutlined, FileWordOutlined, SafetyCertificateOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './MaterialArchivesPage.css';
// ============================================================
// 常量
// ============================================================
const DEFAULT_TASKS = [
    { id: 1, name: '2026年度安全培训计划编制', owner: '张明', deadline: '2026-06-15', priority: '高', enterprise: '广西建工集团', occupation: '安全员', skillLevel: '高级', region: '南宁', projectType: '安全培训', status: '进行中', planCount: 120, startDate: '2026-03-01', endDate: '2026-06-30' },
    { id: 2, name: '电工高级班认定材料整理', owner: '李芳', deadline: '2026-05-25', priority: '高', enterprise: '南宁供电局', occupation: '电工', skillLevel: '高级', region: '南宁', projectType: '资质认证', status: '进行中', planCount: 42 },
    { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', deadline: '2026-05-30', priority: '中', enterprise: '柳州钢铁有限公司', occupation: '焊工', skillLevel: '中级', region: '柳州', projectType: '新员工培训', status: '草稿', planCount: 36 },
    { id: 4, name: '焊工中级班排课协调', owner: '赵丽', deadline: '2026-05-22', priority: '中', enterprise: '桂林旅游股份公司', occupation: '焊工', skillLevel: '中级', region: '桂林', projectType: '资质认证', status: '待审批', planCount: 28 },
    { id: 5, name: '培训档案年度归档', owner: '陈伟', deadline: '2026-06-30', priority: '低', enterprise: '梧州港务集团', occupation: '起重工', skillLevel: '初级', region: '梧州', projectType: '管理研修', status: '草稿', planCount: 15 },
];
const CATEGORIES = ['协议合同', '培训课件', '过程留痕', '认定证书', '补贴申报', '结业归档', '结算收尾'];
const STAGES = ['立项', '人才识别', '初步方案', '最终方案', '开班申报', '培训实施', '认定准备', '申报服务', '培训结业', '项目回款'];
const APPROVAL_NODES = ['培训部归档', '总经理审批', '资料中心入库'];
function todayStr() {
    return new Date().toISOString().slice(0, 10);
}
// ============================================================
// 同步流程资料
// ============================================================
function syncDocsFromFlow(task) {
    const pid = String(task.id);
    const docs = [];
    const add = (name, category, stage, version, status, type) => {
        docs.push({ id: pid + '-' + stage + '-' + docs.length, name, category, stage, version: version || 'v1', status, date: todayStr(), type, projectId: task.id, enterprise: task.enterprise, projectName: task.name });
    };
    add(task.name + '-培训服务合同.pdf', '协议合同', '立项', 'v1', '已归档', 'pdf');
    add(task.name + '-项目立项表.xlsx', '协议合同', '立项', 'v1', '待归档', 'xls');
    add('学员名册与分班表.xlsx', '过程留痕', '分班', 'v1', '已生成', 'xls');
    add('班级编制说明.docx', '过程留痕', '分班', 'v1', '待归档', 'doc');
    add('课程排课表.xlsx', '过程留痕', '排课', 'v1', '已生成', 'xls');
    add('开班仪式材料包.zip', '过程留痕', '开班', 'v1', '已生成', 'pdf');
    add('考勤汇总表.xlsx', '过程留痕', '培训', 'v1', '已生成', 'xls');
    add('课堂留痕证据目录.pdf', '过程留痕', '培训', 'v1', '待归档', 'pdf');
    add('技能认定获证花名册.xlsx', '认定证书', '认定', 'v1', '已生成', 'xls');
    add('补贴申报材料包.pdf', '补贴申报', '申报', 'v1', '待归档', 'pdf');
    add('拨款进度跟踪表.xlsx', '补贴申报', '申报', 'v1', '待归档', 'xls');
    add('结业证明与档案目录.pdf', '结业归档', '结业', 'v1', '待归档', 'cert');
    add('项目结算单.pdf', '结算收尾', '回款', 'v1', '待归档', 'pdf');
    return docs;
}
// ============================================================
// 文件图标
// ============================================================
function FileTypeIcon({ type }) {
    const iconMap = {
        ppt: { icon: _jsx(FilePdfOutlined, {}), cls: 'file-ppt' },
        pdf: { icon: _jsx(FilePdfOutlined, {}), cls: 'file-pdf' },
        doc: { icon: _jsx(FileWordOutlined, {}), cls: 'file-doc' },
        cert: { icon: _jsx(SafetyCertificateOutlined, {}), cls: 'file-cert' },
        xls: { icon: _jsx(FileExcelOutlined, {}), cls: 'file-xls' },
    };
    const { icon, cls } = iconMap[type] || iconMap.pdf;
    return _jsx("span", { className: `file-icon ${cls}`, children: icon });
}
// ============================================================
// 主页面
// ============================================================
export function MaterialArchivesPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [projectMetas, setProjectMetas] = useState({});
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [filterCategory, setFilterCategory] = useState('');
    const [filterStage, setFilterStage] = useState('');
    const [searchKeyword, setSearchKeyword] = useState('');
    const [activeTab, setActiveTab] = useState('list');
    const getProjectMeta = useCallback((projectId) => {
        const key = String(projectId);
        if (!projectMetas[key]) {
            const task = DEFAULT_TASKS.find((t) => t.id === projectId);
            const docs = task ? syncDocsFromFlow(task) : [];
            const meta = {
                projectId,
                documents: docs,
                reviewDone: false,
                archived: false,
                approvalStatus: '草稿',
                approvalStep: 0,
                physicalLocation: '',
                archiveDate: '',
            };
            setProjectMetas((prev) => ({ ...prev, [key]: meta }));
            return meta;
        }
        return projectMetas[key];
    }, [projectMetas]);
    const updateProjectMeta = useCallback((projectId, updater) => {
        setProjectMetas((prev) => {
            const key = String(projectId);
            const current = prev[key] || getProjectMeta(projectId);
            return { ...prev, [key]: updater(current) };
        });
    }, [getProjectMeta]);
    // 全部文档
    const allDocs = useMemo(() => {
        const docs = [];
        DEFAULT_TASKS.forEach((t) => {
            const pid = String(t.id);
            let meta = projectMetas[pid];
            if (!meta) {
                meta = { projectId: t.id, documents: syncDocsFromFlow(t), reviewDone: false, archived: false, approvalStatus: '草稿', approvalStep: 0, physicalLocation: '', archiveDate: '' };
            }
            if (!meta.documents.length) {
                meta.documents = syncDocsFromFlow(t);
            }
            meta.documents.forEach((d) => {
                d.projectId = t.id;
                d.enterprise = t.enterprise;
                d.projectName = t.name;
                docs.push(d);
            });
        });
        return docs;
    }, [projectMetas]);
    const filteredDocs = useMemo(() => {
        return allDocs.filter((d) => {
            if (selectedProjectId && String(d.projectId) !== selectedProjectId)
                return false;
            if (filterCategory && d.category !== filterCategory)
                return false;
            if (filterStage && d.stage !== filterStage)
                return false;
            if (searchKeyword) {
                const hay = [d.name, d.enterprise || '', d.projectName || ''].join(' ').toLowerCase();
                if (!hay.includes(searchKeyword.toLowerCase()))
                    return false;
            }
            return true;
        });
    }, [allDocs, selectedProjectId, filterCategory, filterStage, searchKeyword]);
    // 统计
    const stats = useMemo(() => {
        const archivedProjects = DEFAULT_TASKS.filter((t) => {
            const m = projectMetas[String(t.id)];
            return m?.archived || m?.approvalStatus === '已入库';
        }).length;
        const pending = allDocs.filter((d) => d.status !== '已归档').length;
        return { projects: DEFAULT_TASKS.length, docs: allDocs.length, pending, archived: archivedProjects };
    }, [allDocs, projectMetas]);
    // 选中项目
    const selectedProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    const selectedMeta = selectedProjectId ? projectMetas[selectedProjectId] : null;
    const handleSyncDocs = () => {
        const targets = selectedProjectId ? [+selectedProjectId] : DEFAULT_TASKS.map((t) => t.id);
        targets.forEach((id) => {
            const task = DEFAULT_TASKS.find((t) => t.id === id);
            if (task) {
                updateProjectMeta(id, (prev) => ({ ...prev, documents: syncDocsFromFlow(task) }));
            }
        });
        message.success('已从培训流程各环节同步资料清单');
    };
    const handleReview = () => {
        if (!selectedProjectId) {
            message.warning('请先选择项目');
            return;
        }
        updateProjectMeta(+selectedProjectId, (prev) => ({ ...prev, reviewDone: true }));
        message.success('归档审查通过，资料完整度符合要求');
    };
    const handlePackArchive = () => {
        if (!selectedProjectId) {
            message.warning('请先选择项目');
            return;
        }
        updateProjectMeta(+selectedProjectId, (prev) => {
            if (!prev.reviewDone) {
                message.warning('请先完成归档审查');
                return prev;
            }
            return { ...prev, documents: prev.documents.map((d) => ({ ...d, status: '已归档' })), archiveDate: todayStr() };
        });
        message.success('项目全生命周期资料已打包电子归档');
    };
    const handleSubmitArchive = () => {
        if (!selectedProjectId) {
            message.warning('请先选择项目');
            return;
        }
        updateProjectMeta(+selectedProjectId, (prev) => {
            if (prev.approvalStatus === '草稿') {
                if (!prev.archiveDate) {
                    message.warning('请先电子归档打包');
                    return prev;
                }
                return { ...prev, approvalStatus: '审批中', approvalStep: 1 };
            }
            else if (prev.approvalStatus === '审批中') {
                const newStep = prev.approvalStep + 1;
                if (newStep >= APPROVAL_NODES.length)
                    return { ...prev, approvalStatus: '已入库', archived: true, approvalStep: newStep };
                return { ...prev, approvalStep: newStep };
            }
            return prev;
        });
        message.success('归档审批进度已更新');
    };
    const handleMarkArchived = (docId) => {
        if (!selectedProjectId)
            return;
        updateProjectMeta(+selectedProjectId, (prev) => ({
            ...prev,
            documents: prev.documents.map((d) => (d.id === docId ? { ...d, status: '已归档' } : d)),
        }));
    };
    // ---- 项目列表列 ----
    const projectColumns = [
        {
            title: '企业 · 项目',
            key: 'project',
            render: (_, t) => (_jsxs("div", { children: [_jsx("strong", { children: t.enterprise || '—' }), _jsx("br", {}), _jsx("small", { children: t.name })] })),
        },
        {
            title: '档案完整度',
            key: 'completeness',
            render: (_, t) => {
                const docs = allDocs.filter((d) => d.projectId === t.id);
                const total = docs.length || 1;
                const done = docs.filter((d) => d.status === '已归档').length;
                const pct = Math.round((done / total) * 100);
                return (_jsxs("div", { children: [_jsxs("div", { style: { fontSize: 13 }, children: [pct, "%"] }), _jsx(Progress, { percent: pct, showInfo: false, size: "small" })] }));
            },
        },
        {
            title: '条目数',
            key: 'count',
            render: (_, t) => allDocs.filter((d) => d.projectId === t.id).length,
        },
        {
            title: '归档状态',
            key: 'archiveStatus',
            render: (_, t) => {
                const m = projectMetas[String(t.id)];
                const st = m?.archived ? '已入库' : m?.approvalStatus === '审批中' ? '审批中' : '待归档';
                const cls = m?.archived ? 'st-archived' : m?.approvalStatus === '审批中' ? 'st-pending' : 'st-draft';
                return _jsx("span", { className: `status-badge ${cls}`, children: st });
            },
        },
        {
            title: '操作',
            key: 'actions',
            render: (_, t) => (_jsx(Button, { size: "small", onClick: () => {
                    setSelectedProjectId(String(t.id));
                    setActiveTab('business');
                }, children: "\u67E5\u770B\u6863\u6848" })),
        },
    ];
    // ---- 业务档案列 ----
    const docColumns = [
        {
            title: '资料名称',
            key: 'name',
            render: (_, d) => (_jsxs("div", { children: [_jsx(FileTypeIcon, { type: d.type }), _jsx("strong", { children: d.name }), d.projectName && _jsx("br", {}), d.projectName && _jsxs("small", { style: { color: '#64748B' }, children: [d.enterprise, " \u00B7 ", d.projectName] })] })),
        },
        { title: '分类', dataIndex: 'category' },
        { title: '阶段', dataIndex: 'stage' },
        { title: '版本', dataIndex: 'version' },
        {
            title: '归档状态',
            dataIndex: 'status',
            render: (v) => {
                const cls = v === '已归档' ? 'st-done' : v === '已生成' ? 'st-pending' : 'st-draft';
                return _jsx("span", { className: `status-badge ${cls}`, children: v });
            },
        },
        {
            title: '操作',
            key: 'actions',
            render: (_, d) => (_jsxs("div", { style: { display: 'flex', gap: 8 }, children: [_jsx(Button, { size: "small", onClick: () => message.info('下载演示：' + d.name), children: "\u4E0B\u8F7D" }), _jsx(Button, { size: "small", onClick: () => handleMarkArchived(d.id), children: "\u6807\u8BB0\u5F52\u6863" })] })),
        },
    ];
    // ---- 数据台账 ----
    const ledgerItems = useMemo(() => {
        const projects = selectedProjectId ? DEFAULT_TASKS.filter((t) => String(t.id) === selectedProjectId) : DEFAULT_TASKS;
        const items = [];
        projects.forEach((t) => {
            items.push({ label: t.name + ' · 班级数', val: 0 }, { label: '参训学员', val: '0 人' }, { label: '培训课次', val: '0 次' }, { label: '认定通过', val: '0 人' }, { label: '补贴申报', val: '—' }, { label: '结业归档', val: '未完成' }, { label: '回款状态', val: '—' });
        });
        return items;
    }, [selectedProjectId]);
    // ---- 归档审批 ----
    const approvalTrack = useMemo(() => {
        if (!selectedProjectId || !selectedMeta)
            return null;
        const step = selectedMeta.approvalStep || 0;
        return APPROVAL_NODES.map((name, i) => {
            let cls = 'approval-node';
            if (selectedMeta.archived || selectedMeta.approvalStatus === '已入库')
                cls += ' done';
            else if (i < step)
                cls += ' done';
            else if (i === step && selectedMeta.approvalStatus === '审批中')
                cls += ' active';
            return _jsx("div", { className: cls, children: name }, name);
        });
    }, [selectedProjectId, selectedMeta]);
    return (_jsxs("div", { className: "material-archives-page", children: [_jsxs("header", { className: "ma-topbar", children: [_jsxs("div", { className: "ma-topbar-left", children: [_jsxs("a", { className: "ma-back-link", onClick: () => navigateTo('zhixun-project-archives'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u9879\u76EE\u6863\u6848"] }), _jsxs("div", { children: [_jsx("h1", { className: "ma-title", children: "\u8D44\u6599\u6863\u6848" }), _jsx("span", { className: "ma-sub", children: selectedProject ? `${selectedProject.enterprise} · ${selectedProject.name} · 资料档案` : '多项目资料归档 · 项目-班级-阶段-版本' })] })] }), _jsxs("span", { className: "ma-badge", children: [_jsx(DatabaseOutlined, {}), " \u5217\u8868 \u00B7 \u4E1A\u52A1\u6863\u6848 \u00B7 \u6570\u636E\u53F0\u8D26"] })] }), _jsxs("main", { className: "ma-page", children: [_jsxs("div", { className: "ma-card", children: [_jsxs("div", { className: "ma-card-head", children: [_jsxs("div", { className: "ma-card-head-title", children: [_jsx(FilterOutlined, {}), " \u68C0\u7D22\u4E0E\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "ma-filter-bar", children: [_jsxs("div", { className: "ma-filter-item", children: [_jsx("label", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), _jsx(Select, { placeholder: "\u5168\u90E8\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: (v) => { setSelectedProjectId(v || ''); setCurrentProjectId(v ? +v : null); }, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), _jsxs("div", { className: "ma-filter-item", children: [_jsx("label", { children: "\u8D44\u6599\u5206\u7C7B" }), _jsx(Select, { placeholder: "\u5168\u90E8\u5206\u7C7B", value: filterCategory || undefined, onChange: (v) => setFilterCategory(v || ''), allowClear: true, style: { width: '100%' }, options: CATEGORIES.map((v) => ({ value: v, label: v })) })] }), _jsxs("div", { className: "ma-filter-item", children: [_jsx("label", { children: "\u6D41\u7A0B\u9636\u6BB5" }), _jsx(Select, { placeholder: "\u5168\u90E8\u9636\u6BB5", value: filterStage || undefined, onChange: (v) => setFilterStage(v || ''), allowClear: true, style: { width: '100%' }, options: STAGES.map((v) => ({ value: v, label: v })) })] }), _jsxs("div", { className: "ma-filter-item", children: [_jsx("label", { children: "\u5173\u952E\u8BCD" }), _jsx(Input, { placeholder: "\u6587\u4EF6\u540D\u3001\u4F01\u4E1A\u540D\u2026", value: searchKeyword, onChange: (e) => setSearchKeyword(e.target.value), allowClear: true })] })] }), selectedProject && (_jsxs("div", { className: "ma-project-banner", children: [_jsxs("div", { className: "ma-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: selectedProject.enterprise || '—' })] }), _jsxs("div", { className: "ma-banner-item", children: [_jsx("label", { children: "\u9879\u76EE" }), _jsx("strong", { children: selectedProject.name || '—' })] }), _jsxs("div", { className: "ma-banner-item", children: [_jsx("label", { children: "\u6863\u6848\u6761\u76EE" }), _jsxs("strong", { children: [allDocs.filter((d) => String(d.projectId) === selectedProjectId).length, " \u4EFD"] })] }), _jsxs("div", { className: "ma-banner-item", children: [_jsx("label", { children: "\u5F52\u6863\u72B6\u6001" }), _jsx("strong", { children: selectedMeta?.approvalStatus || '草稿' })] })] }))] }), _jsxs("div", { className: "ma-stat-row", children: [_jsxs("div", { className: "ma-stat-card", children: [_jsx("div", { className: "ma-stat-val", children: stats.projects }), _jsx("div", { className: "ma-stat-lbl", children: "\u5728\u7BA1\u9879\u76EE" })] }), _jsxs("div", { className: "ma-stat-card", children: [_jsx("div", { className: "ma-stat-val success", children: stats.docs }), _jsx("div", { className: "ma-stat-lbl", children: "\u6863\u6848\u6761\u76EE" })] }), _jsxs("div", { className: "ma-stat-card", children: [_jsx("div", { className: "ma-stat-val warn", children: stats.pending }), _jsx("div", { className: "ma-stat-lbl", children: "\u5F85\u5F52\u6863" })] }), _jsxs("div", { className: "ma-stat-card", children: [_jsx("div", { className: "ma-stat-val", children: stats.archived }), _jsx("div", { className: "ma-stat-lbl", children: "\u5DF2\u5F52\u6863\u9879\u76EE" })] })] }), _jsx("div", { className: "ma-card", children: _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                                {
                                    key: 'list',
                                    label: '项目列表',
                                    children: (_jsx(Table, { dataSource: DEFAULT_TASKS, columns: projectColumns, rowKey: "id", pagination: false, locale: { emptyText: '暂无培训项目，请先在任务档案创建' } })),
                                },
                                {
                                    key: 'business',
                                    label: '业务档案',
                                    children: (_jsxs(_Fragment, { children: [_jsxs("div", { className: "ma-toolbar", children: [_jsx(Button, { type: "primary", size: "small", icon: _jsx(SyncOutlined, {}), onClick: handleSyncDocs, children: "\u540C\u6B65\u6D41\u7A0B\u8D44\u6599" }), _jsx(Button, { size: "small", icon: _jsx(CloudUploadOutlined, {}), onClick: () => message.info('上传资料功能演示'), children: "\u4E0A\u4F20\u8D44\u6599" }), _jsx(Button, { size: "small", icon: _jsx(AuditOutlined, {}), onClick: handleReview, children: "\u5F52\u6863\u5BA1\u67E5" })] }), _jsx(Table, { dataSource: filteredDocs, columns: docColumns, rowKey: "id", pagination: false, locale: { emptyText: '暂无资料，请选择项目或点击「同步流程资料」' } })] })),
                                },
                                {
                                    key: 'ledger',
                                    label: '数据台账',
                                    children: (_jsxs(_Fragment, { children: [_jsx("p", { style: { fontSize: 13, color: '#64748B', marginBottom: 16 }, children: "\u6C47\u603B\u5404\u57F9\u8BAD\u6D41\u7A0B\u73AF\u8282\u4EA7\u751F\u7684\u7ED3\u6784\u5316\u6570\u636E\uFF08\u4EBA\u624D\u8BC6\u522B\u3001\u65B9\u6848\u3001\u5F00\u73ED\u7533\u62A5\u3001\u57F9\u8BAD\u5B9E\u65BD\u3001\u8BA4\u5B9A\u51C6\u5907\u3001\u7533\u62A5\u670D\u52A1\u3001\u57F9\u8BAD\u7ED3\u4E1A\u3001\u9879\u76EE\u56DE\u6B3E\uFF09\u3002" }), ledgerItems.length ? (_jsx("div", { className: "ma-ledger-grid", children: ledgerItems.map((it, i) => (_jsxs("div", { className: "ma-ledger-item", children: [_jsx("label", { children: it.label }), _jsx("strong", { children: it.val })] }, i))) })) : (_jsx("p", { style: { color: '#64748B' }, children: "\u8BF7\u9009\u62E9\u9879\u76EE\u67E5\u770B\u6570\u636E\u53F0\u8D26" }))] })),
                                },
                                {
                                    key: 'archive',
                                    label: '归档审批',
                                    children: (_jsxs(_Fragment, { children: [_jsx("p", { style: { fontSize: 13, color: '#64748B', marginBottom: 16 }, children: "\u6309 MD \u6D41\u7A0B\uFF1A\u57F9\u8BAD\u90E8\u8D44\u6599\u5F52\u6863 \u2192 \u603B\u7ECF\u7406\u5F52\u6863\u5BA1\u6279 \u2192 \u7535\u5B50\u5F52\u6863\u81F3\u8D44\u6599\u4E2D\u5FC3\u3002" }), _jsxs("div", { className: "ma-toolbar", children: [_jsx(Button, { type: "primary", size: "small", icon: _jsx(FileZipOutlined, {}), onClick: handlePackArchive, children: "\u7535\u5B50\u5F52\u6863\u6253\u5305" }), _jsx(Button, { size: "small", className: "btn-success", icon: _jsx(TrophyOutlined, {}), onClick: handleSubmitArchive, children: "\u63D0\u4EA4\u5F52\u6863\u5BA1\u6279" })] }), _jsxs("div", { className: "ma-form-group", style: { maxWidth: 400, marginBottom: 16 }, children: [_jsx("label", { children: "\u7EB8\u8D28\u6863\u6848\u5B58\u653E\u4F4D\u7F6E\uFF08\u53EF\u9009\uFF09" }), _jsx(Input, { placeholder: "\u5982\uFF1A\u6863\u6848\u5BA4 A-12 \u67DC", value: selectedMeta?.physicalLocation || '', onChange: (e) => {
                                                            if (!selectedProjectId)
                                                                return;
                                                            updateProjectMeta(+selectedProjectId, (prev) => ({ ...prev, physicalLocation: e.target.value }));
                                                        } })] }), _jsx("div", { className: "ma-approval-track", children: approvalTrack || _jsx("p", { style: { color: '#64748B', fontSize: 14 }, children: "\u8BF7\u5148\u9009\u62E9\u5177\u4F53\u9879\u76EE\u4EE5\u529E\u7406\u5F52\u6863\u5BA1\u6279" }) }), selectedMeta?.archiveDate && (_jsxs("div", { className: "ma-archive-summary", children: [_jsx("strong", { children: "\u7535\u5B50\u5F52\u6863\u5305" }), " \u5DF2\u4E8E ", selectedMeta.archiveDate, " \u6253\u5305\u5165\u5E93\u3002\u5B58\u653E\u4F4D\u7F6E\uFF1A", selectedMeta.physicalLocation || '未登记'] }))] })),
                                },
                            ] }) })] })] }));
}

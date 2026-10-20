import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback } from 'react';
import { Input, Select, Button, Table, Modal, message } from 'antd';
import { ArrowLeftOutlined, UserSwitchOutlined, InfoCircleOutlined, FileExcelOutlined, EyeOutlined, EditOutlined, AuditOutlined, SendOutlined, CloudUploadOutlined, CloseOutlined, ThunderboltOutlined, CheckOutlined, ArrowRightOutlined, UnorderedListOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TalentIdentificationPage.css';
// ============================================================
// 模拟数据
// ============================================================
const DEFAULT_COMPANIES = [
    { id: 'c1', name: '南南铝业股份有限公司', unidentified: 286, pending: 42, confirmed: 897, status: '已审核' },
    { id: 'c2', name: '华锡集团', unidentified: 156, pending: 28, confirmed: 521, status: '已确认' },
    { id: 'c3', name: '金海不锈钢', unidentified: 98, pending: 15, confirmed: 368, status: '未审核' },
    { id: 'c4', name: '柳州钢铁有限公司', unidentified: 45, pending: 12, confirmed: 210, status: '已确认' },
    { id: 'c5', name: '南宁供电局', unidentified: 32, pending: 8, confirmed: 156, status: '已审核' },
];
const DEFAULT_TASKS = [
    { id: 1, name: '2026年度安全培训计划编制', enterprise: '广西建工集团', planCount: 120 },
    { id: 2, name: '电工高级班认定材料整理', enterprise: '南宁供电局', planCount: 42 },
    { id: 3, name: 'Q2培训项目回款跟进', enterprise: '柳州钢铁有限公司', planCount: 36 },
    { id: 4, name: '焊工中级班排课协调', enterprise: '桂林旅游股份公司', planCount: 28 },
    { id: 5, name: '培训档案年度归档', enterprise: '梧州港务集团', planCount: 15 },
];
const NAMES = ['张伟', '李娜', '王强', '赵丽', '刘洋', '陈静', '杨帆', '周敏'];
const DEPTS = ['生产一部', '生产二部', '质检部', '设备科'];
const OCCS = ['电工', '焊工', '起重工', '安全员'];
const LEVELS = ['初级', '中级', '高级', '技师'];
function genPersons(company) {
    if (company.persons && company.persons.length)
        return company.persons;
    const n = Math.min(company.unidentified + company.pending + company.confirmed, 12) || 8;
    const persons = [];
    for (let i = 0; i < n; i++) {
        let st = '已确认';
        if (i < company.unidentified)
            st = '未识别';
        else if (i < company.unidentified + company.pending)
            st = '待确认';
        persons.push({
            id: i + 1,
            name: NAMES[i % NAMES.length] + (i > 7 ? i : ''),
            code: 'GH' + String(1000 + i),
            dept: DEPTS[i % DEPTS.length],
            occ: OCCS[i % OCCS.length],
            level: LEVELS[i % LEVELS.length],
            status: st,
        });
    }
    company.persons = persons;
    return persons;
}
function statusClass(st) {
    if (st === '已审核')
        return 'st-audited';
    if (st === '已确认')
        return 'st-confirmed';
    return 'st-pending';
}
// ============================================================
// 主页面
// ============================================================
export function TalentIdentificationPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [companies, setCompanies] = useState(() => {
        // 从 URL 参数 / navStore 匹配项目
        const list = [...DEFAULT_COMPANIES];
        if (currentProjectId) {
            const task = DEFAULT_TASKS.find((t) => t.id === currentProjectId);
            if (task && !list.some((c) => c.name === task.enterprise)) {
                list.unshift({
                    id: 't' + currentProjectId,
                    name: task.enterprise || task.name,
                    unidentified: Math.floor((task.planCount || 30) * 0.3),
                    pending: Math.floor((task.planCount || 30) * 0.15),
                    confirmed: Math.floor((task.planCount || 30) * 0.5),
                    status: '未审核',
                });
            }
        }
        return list;
    });
    const [filterStatus, setFilterStatus] = useState('');
    const [searchKeyword, setSearchKeyword] = useState(() => {
        if (currentProjectId) {
            const task = DEFAULT_TASKS.find((t) => t.id === currentProjectId);
            return task ? (task.enterprise || task.name) : '';
        }
        return '';
    });
    const [currentUnitId, setCurrentUnitId] = useState(null);
    const [personFilter, setPersonFilter] = useState('all');
    const [importModalOpen, setImportModalOpen] = useState(false);
    const [importUnit, setImportUnit] = useState('');
    const [importFileName, setImportFileName] = useState('');
    const currentCompany = currentUnitId ? companies.find((c) => c.id === currentUnitId) : null;
    const projectTask = currentProjectId ? DEFAULT_TASKS.find((t) => t.id === currentProjectId) : null;
    // 统计
    const stats = useMemo(() => ({
        count: companies.length,
        unidentified: companies.reduce((s, c) => s + c.unidentified, 0),
        pending: companies.reduce((s, c) => s + c.pending, 0),
        confirmed: companies.reduce((s, c) => s + c.confirmed, 0),
    }), [companies]);
    // 筛选单位
    const filteredCompanies = useMemo(() => companies.filter((c) => {
        if (filterStatus && c.status !== filterStatus)
            return false;
        if (searchKeyword && !c.name.toLowerCase().includes(searchKeyword.toLowerCase()))
            return false;
        return true;
    }), [companies, filterStatus, searchKeyword]);
    // 人员列表
    const persons = useMemo(() => {
        if (!currentCompany)
            return [];
        const p = genPersons(currentCompany);
        if (personFilter === 'all')
            return p;
        if (personFilter === 'unidentified')
            return p.filter((x) => x.status === '未识别');
        if (personFilter === 'pending')
            return p.filter((x) => x.status === '待确认');
        return p.filter((x) => x.status === '已确认');
    }, [currentCompany, personFilter]);
    const updateCompany = useCallback((id, updater) => {
        setCompanies((prev) => prev.map((c) => {
            if (c.id !== id)
                return c;
            const clone = { ...c, persons: c.persons ? [...c.persons] : undefined };
            updater(clone);
            return clone;
        }));
    }, []);
    // 操作
    const handleIdentify = (personId) => {
        if (!currentUnitId)
            return;
        updateCompany(currentUnitId, (c) => {
            const p = c.persons?.find((x) => x.id === personId);
            if (p && p.status === '未识别') {
                p.status = '待确认';
                c.unidentified = Math.max(0, c.unidentified - 1);
                c.pending++;
            }
        });
    };
    const handleConfirmPerson = (personId) => {
        if (!currentUnitId)
            return;
        updateCompany(currentUnitId, (c) => {
            const p = c.persons?.find((x) => x.id === personId);
            if (p && p.status === '待确认') {
                p.status = '已确认';
                c.pending = Math.max(0, c.pending - 1);
                c.confirmed++;
            }
        });
    };
    const handleBatchIdentify = () => {
        if (!currentUnitId)
            return;
        updateCompany(currentUnitId, (c) => {
            let n = 0;
            c.persons?.forEach((p) => { if (p.status === '未识别') {
                p.status = '待确认';
                n++;
            } });
            c.unidentified = Math.max(0, c.unidentified - n);
            c.pending += n;
        });
        message.success('已批量识别，进入待确认');
    };
    const handleAudit = (id) => {
        updateCompany(id, (c) => { c.status = '已审核'; });
        message.success('已审核通过');
    };
    const handleSendConfirm = () => {
        if (!currentUnitId)
            return;
        updateCompany(currentUnitId, (c) => { c.status = '已确认'; });
        message.success(`「${currentCompany?.name}」识别结果已送企业确认`);
    };
    const handleAuditDetail = () => {
        if (!currentUnitId)
            return;
        updateCompany(currentUnitId, (c) => { c.status = '已审核'; });
        message.success(`「${currentCompany?.name}」审核通过`);
    };
    const handleImportConfirm = () => {
        if (!importUnit) {
            message.warning('请选择单位');
            return;
        }
        if (!importFileName) {
            message.warning('请上传 Excel 文件');
            return;
        }
        updateCompany(importUnit, (c) => {
            const add = Math.floor(Math.random() * 30) + 20;
            c.unidentified += add;
            c.persons = undefined; // 重置人员缓存
        });
        message.success(`已导入 ${Math.floor(Math.random() * 30) + 20} 条人员数据（演示）`);
        setImportModalOpen(false);
        setImportFileName('');
        setImportUnit('');
    };
    const handleNextStep = () => {
        if (companies.some((c) => c.unidentified > 0)) {
            message.warning('仍有未识别人员');
        }
        navigateTo('zhixun-task-archives'); // 暂跳任务档案，后续替换为初步方案
    };
    // ---- 单位列表列 ----
    const companyColumns = [
        { title: '单位名称', dataIndex: 'name', render: (v) => _jsx("strong", { children: v }) },
        { title: '未识别人员', dataIndex: 'unidentified', render: (v) => _jsx("span", { style: { color: '#EF4444', fontWeight: 600 }, children: v }) },
        { title: '待确认数量', dataIndex: 'pending', render: (v) => _jsx("span", { style: { color: '#F59E0B', fontWeight: 600 }, children: v }) },
        { title: '已确认数量', dataIndex: 'confirmed', render: (v) => _jsx("span", { style: { color: '#10B981', fontWeight: 600 }, children: v }) },
        {
            title: '状态', dataIndex: 'status',
            render: (v) => _jsx("span", { className: `status-badge ${statusClass(v)}`, children: v }),
        },
        {
            title: '操作', key: 'actions',
            render: (_, c) => (_jsxs("div", { className: "ti-actions", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => setCurrentUnitId(c.id), children: "\u67E5\u770B\u8BE6\u60C5" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => message.info(`编辑单位「${c.name}」信息（演示）`), style: { color: '#F59E0B' }, children: "\u7F16\u8F91" }), c.status === '未审核' && (_jsx(Button, { type: "link", size: "small", icon: _jsx(AuditOutlined, {}), onClick: () => handleAudit(c.id), children: "\u5BA1\u6838" })), _jsx(Button, { type: "link", size: "small", icon: _jsx(SendOutlined, {}), onClick: () => message.info(`已向「${c.name}」发送企业确认通知（演示）`), children: "\u9001\u4F01\u4E1A\u786E\u8BA4" })] })),
        },
    ];
    // ---- 人员列表列 ----
    const personColumns = [
        { title: '姓名', dataIndex: 'name', render: (v) => _jsx("strong", { children: v }) },
        { title: '工号', dataIndex: 'code' },
        { title: '部门', dataIndex: 'dept' },
        { title: '工种', dataIndex: 'occ' },
        { title: '技能等级', dataIndex: 'level' },
        {
            title: '识别状态', dataIndex: 'status',
            render: (v) => {
                const cls = v === '已确认' ? 'st-audited' : v === '待确认' ? 'st-confirmed' : 'st-pending';
                return _jsx("span", { className: `status-badge ${cls}`, children: v });
            },
        },
        {
            title: '操作', key: 'actions',
            render: (_, p) => (_jsxs(_Fragment, { children: [p.status === '未识别' && _jsx(Button, { type: "link", size: "small", onClick: () => handleIdentify(p.id), children: "\u8BC6\u522B" }), p.status === '待确认' && _jsx(Button, { type: "link", size: "small", onClick: () => handleConfirmPerson(p.id), children: "\u786E\u8BA4" })] })),
        },
    ];
    return (_jsxs("div", { className: "talent-id-page", children: [_jsxs("header", { className: "ti-topbar", children: [_jsxs("div", { className: "ti-topbar-left", children: [_jsxs("a", { className: "ti-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "ti-title", children: "\u4EBA\u624D\u8BC6\u522B" }), _jsx("span", { className: "ti-sub", children: projectTask ? `${projectTask.enterprise || projectTask.name} · 企业人才数据识别与确认 · 步骤 1 / 9` : '企业人才数据识别与确认 · 步骤 1 / 9' })] })] }), _jsxs("span", { className: "ti-process-badge", children: [_jsx(UserSwitchOutlined, {}), " \u4EBA\u624D\u8BC6\u522B \u2192 \u521D\u6B65\u65B9\u6848 \u2192 \u2026"] })] }), _jsxs("main", { className: "ti-page", children: [!currentUnitId && (_jsxs(_Fragment, { children: [_jsxs("div", { className: "ti-flow-hint", children: [_jsx(InfoCircleOutlined, { style: { color: '#2A6DFF' } }), ' ', "\u4E1A\u52A1\u6D41\u7A0B\uFF1A", _jsx("strong", { children: "Excel\u5BFC\u5165\u4EBA\u5458\u6570\u636E \u2192 \u7CFB\u7EDF\u6280\u80FD\u8BC6\u522B \u2192 \u57F9\u8BAD\u90E8\u5BA1\u6838 \u2192 \u4F01\u4E1A\u786E\u8BA4 \u2192 \u8FDB\u5165\u521D\u6B65\u65B9\u6848" })] }), _jsxs("div", { className: "ti-stat-row", children: [_jsxs("div", { className: "ti-stat-card identify", children: [_jsx("div", { className: "ti-stat-val", children: stats.count }), _jsx("div", { className: "ti-stat-lbl", children: "\u53C2\u4E0E\u8BC6\u522B\u5355\u4F4D" })] }), _jsxs("div", { className: "ti-stat-card danger", children: [_jsx("div", { className: "ti-stat-val red", children: stats.unidentified }), _jsx("div", { className: "ti-stat-lbl", children: "\u672A\u8BC6\u522B\u4EBA\u5458" })] }), _jsxs("div", { className: "ti-stat-card warn", children: [_jsx("div", { className: "ti-stat-val yellow", children: stats.pending }), _jsx("div", { className: "ti-stat-lbl", children: "\u5F85\u786E\u8BA4\u6570\u91CF" })] }), _jsxs("div", { className: "ti-stat-card success", children: [_jsx("div", { className: "ti-stat-val green", children: stats.confirmed }), _jsx("div", { className: "ti-stat-lbl", children: "\u5DF2\u786E\u8BA4\u6570\u91CF" })] })] }), _jsxs("div", { className: "ti-card", children: [_jsxs("div", { className: "ti-toolbar", children: [_jsxs("div", { className: "ti-toolbar-left", children: [_jsxs("div", { className: "ti-form-inline", children: [_jsx("label", { children: "\u72B6\u6001" }), _jsx(Select, { placeholder: "\u5168\u90E8\u72B6\u6001", value: filterStatus || undefined, onChange: (v) => setFilterStatus(v || ''), allowClear: true, style: { width: 130 }, options: ['未审核', '已确认', '已审核'].map((v) => ({ value: v, label: v })) })] }), _jsx(Input, { placeholder: "\u641C\u7D22\u5355\u4F4D\u540D\u79F0\u2026", value: searchKeyword, onChange: (e) => setSearchKeyword(e.target.value), allowClear: true, style: { width: 220 } })] }), _jsx(Button, { className: "btn-identify", icon: _jsx(FileExcelOutlined, {}), onClick: () => setImportModalOpen(true), children: "Excel \u5BFC\u5165" })] }), _jsx(Table, { dataSource: filteredCompanies, columns: companyColumns, rowKey: "id", pagination: false, locale: { emptyText: '暂无单位数据，请先 Excel 导入或从任务档案创建项目' } }), _jsxs("div", { className: "ti-footer-actions", children: [_jsx(Button, { icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" }), _jsx(Button, { type: "primary", icon: _jsx(ArrowRightOutlined, {}), onClick: handleNextStep, children: "\u5B8C\u6210\u8BC6\u522B\uFF0C\u8FDB\u5165\u521D\u6B65\u65B9\u6848" })] })] })] })), currentUnitId && currentCompany && (_jsxs("div", { className: "ti-card", children: [_jsxs("div", { className: "ti-detail-header", children: [_jsxs("h2", { children: [currentCompany.name, " \u00B7 \u4EBA\u5458\u8BC6\u522B\u660E\u7EC6"] }), _jsx(Button, { size: "small", icon: _jsx(ArrowLeftOutlined, {}), onClick: () => setCurrentUnitId(null), children: "\u8FD4\u56DE\u5355\u4F4D\u5217\u8868" })] }), _jsx("div", { className: "ti-filter-tabs", children: [
                                    { key: 'all', label: '全部' },
                                    { key: 'unidentified', label: '未识别' },
                                    { key: 'pending', label: '待确认' },
                                    { key: 'confirmed', label: '已确认' },
                                ].map((tab) => (_jsx("button", { className: `ti-filter-tab${personFilter === tab.key ? ' active' : ''}`, onClick: () => setPersonFilter(tab.key), children: tab.label }, tab.key))) }), _jsx(Table, { dataSource: persons, columns: personColumns, rowKey: "id", pagination: false, locale: { emptyText: '暂无人员' } }), _jsxs("div", { className: "ti-footer-actions", children: [_jsx(Button, { className: "btn-identify", size: "small", icon: _jsx(ThunderboltOutlined, {}), onClick: handleBatchIdentify, children: "\u6279\u91CF\u667A\u80FD\u8BC6\u522B" }), _jsx(Button, { size: "small", icon: _jsx(SendOutlined, {}), onClick: handleSendConfirm, children: "\u9001\u4F01\u4E1A\u786E\u8BA4" }), _jsx(Button, { type: "primary", size: "small", icon: _jsx(CheckOutlined, {}), onClick: handleAuditDetail, children: "\u5BA1\u6838\u901A\u8FC7" })] })] }))] }), _jsxs(Modal, { open: importModalOpen, onCancel: () => { setImportModalOpen(false); setImportFileName(''); setImportUnit(''); }, title: _jsxs(_Fragment, { children: [_jsx(FileExcelOutlined, { style: { color: '#F18F01' } }), " Excel \u5BFC\u5165"] }), footer: _jsxs(_Fragment, { children: [_jsx(Button, { onClick: () => { setImportModalOpen(false); setImportFileName(''); setImportUnit(''); }, children: "\u53D6\u6D88" }), _jsx(Button, { className: "btn-identify", onClick: handleImportConfirm, children: "\u786E\u8BA4\u5BFC\u5165" })] }), children: [_jsxs("div", { className: "ti-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u5355\u4F4D / \u4F01\u4E1A" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u5355\u4F4D", value: importUnit || undefined, onChange: (v) => setImportUnit(v || ''), style: { width: '100%' }, options: companies.map((c) => ({ value: c.id, label: c.name })) })] }), _jsxs("div", { className: "ti-form-group", children: [_jsx("label", { children: "\u4E0A\u4F20 Excel \u6587\u4EF6" }), _jsxs("div", { className: "ti-upload-zone", onClick: () => {
                                    const input = document.createElement('input');
                                    input.type = 'file';
                                    input.accept = '.xlsx,.xls';
                                    input.onchange = (e) => {
                                        const f = e.target.files?.[0];
                                        if (f)
                                            setImportFileName(f.name);
                                    };
                                    input.click();
                                }, children: [_jsx(CloudUploadOutlined, { style: { fontSize: 36, color: '#F18F01' } }), _jsx("p", { children: "\u70B9\u51FB\u4E0A\u4F20 Excel \u6587\u4EF6" }), _jsx("p", { style: { fontSize: 12, color: '#64748B', marginTop: 6 }, children: "\u652F\u6301 .xlsx / .xls \u683C\u5F0F" })] }), importFileName && (_jsxs("div", { className: "ti-file-preview show", children: [_jsx("span", { children: importFileName }), _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(CloseOutlined, {}), onClick: () => setImportFileName('') })] }))] })] })] }));
}

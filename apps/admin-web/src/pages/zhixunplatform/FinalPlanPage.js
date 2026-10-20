import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, Input, Button, Table, Tabs, InputNumber, message } from 'antd';
import { ArrowLeftOutlined, ArrowRightOutlined, InfoCircleOutlined, FolderOpenOutlined, UnorderedListOutlined, FileTextOutlined, FileProtectOutlined, CheckCircleOutlined, LockOutlined, PlusOutlined, DeleteOutlined, BellOutlined, SafetyCertificateOutlined, NodeIndexOutlined, CalendarOutlined, ApartmentOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './FinalPlanPage.css';
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
const INSTITUTES = ['广西机电技师学院', '柳州职业技能培训中心', '华南技能培训学校'];
function todayStr() {
    return new Date().toISOString().slice(0, 10);
}
function defaultFinal() {
    return {
        refVersion: 'V1.0',
        refMode: '—',
        refYears: '5 年（2026 起）',
        enterpriseConfirm: '待确认',
        enterpriseNote: '',
        finalizeDate: todayStr(),
        finalStatus: '编制中',
        firstOpenDate: '',
        firstOpenOcc: '',
        planClassCount: 1,
        classSize: 30,
        finalVenue: '',
        occFinal: [],
        resources: [],
        locked: false,
        lockedVer: '',
        lockedAt: '',
    };
}
function buildInitialData(project) {
    const occs = [
        { id: 1, occ: project.occupation || '电工', level: '高级工', planCount: 40, identified: 32, batches: 2, form: '理实一体', quarter: 'Q2', finalCount: 40, firstBatch: true, institute: INSTITUTES[0], confirmed: false },
        { id: 2, occ: '焊工', level: '技师', planCount: 24, identified: 18, batches: 1, form: '集中培训', quarter: 'Q3', finalCount: 24, firstBatch: true, institute: INSTITUTES[0], confirmed: false },
    ];
    const resources = [
        { id: 1, type: '培训机构', name: INSTITUTES[0], content: '签订培训协议及授课承诺书', date: '', owner: '培训部', status: '待落实' },
        { id: 2, type: '师资', name: '陈教授', content: '首期主讲教师安排确认', date: '', owner: '培训机构', status: '待落实' },
        { id: 3, type: '场地', name: '总部培训中心', content: '场地预订与设备检查', date: '', owner: '培训部', status: '待落实' },
        { id: 4, type: '课件', name: '课程部课包', content: '课件审批通过', date: '', owner: '课程部', status: '审批中' },
    ];
    return {
        refVersion: 'V1.0',
        refMode: '新型学徒制',
        refYears: `5 年（2026 起）`,
        enterpriseConfirm: '待确认',
        enterpriseNote: '',
        finalizeDate: todayStr(),
        finalStatus: '编制中',
        firstOpenDate: '',
        firstOpenOcc: `${project.occupation || '电工'} 高级工`,
        planClassCount: 1,
        classSize: 30,
        finalVenue: '总部培训中心',
        occFinal: occs,
        resources,
        locked: false,
        lockedVer: '',
        lockedAt: '',
    };
}
// ============================================================
// 主页面
// ============================================================
export function FinalPlanPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [plan, setPlan] = useState(defaultFinal);
    const [resIdCounter, setResIdCounter] = useState(5);
    const [activeTab, setActiveTab] = useState('finalize');
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    useEffect(() => {
        if (!currentProject) {
            setPlan(defaultFinal());
            return;
        }
        setPlan(buildInitialData(currentProject));
        setResIdCounter(5);
    }, [selectedProjectId]);
    const updatePlan = useCallback((updater) => {
        setPlan((prev) => {
            const next = { ...prev, occFinal: prev.occFinal.map((o) => ({ ...o })), resources: prev.resources.map((r) => ({ ...r })) };
            updater(next);
            return next;
        });
    }, []);
    const stats = useMemo(() => {
        const occ = plan.occFinal.length;
        const confirmed = plan.occFinal.filter((o) => o.confirmed).length;
        const resOk = plan.resources.filter((r) => r.status === '已落实').length;
        const resTotal = plan.resources.length;
        const pct = resTotal ? Math.round(resOk / resTotal * 100) : 0;
        return { occ, confirmed, pct, locked: plan.locked };
    }, [plan]);
    const readiness = useMemo(() => {
        return [
            { label: '企业已确认方案', ok: plan.enterpriseConfirm === '已确认' },
            { label: '工种等级终稿已全部确认', ok: plan.occFinal.length > 0 && plan.occFinal.every((o) => o.confirmed) },
            { label: '首期开班日期已确定', ok: !!plan.firstOpenDate },
            { label: '培训场地已落实', ok: plan.resources.some((r) => r.type === '场地' && r.status === '已落实') },
            { label: '师资已安排确认', ok: plan.resources.some((r) => r.type === '师资' && r.status === '已落实') },
            { label: '最终方案已锁定', ok: plan.locked },
        ];
    }, [plan]);
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
    };
    const handleConfirmOcc = (idx) => {
        updatePlan((p) => { p.occFinal[idx].confirmed = true; });
    };
    const handleAddResource = () => {
        if (plan.locked)
            return;
        const newId = resIdCounter;
        setResIdCounter(newId + 1);
        updatePlan((p) => { p.resources.push({ id: newId, type: '其他', name: '', content: '', date: '', owner: '培训部', status: '待落实' }); });
    };
    const handleDelResource = (id) => {
        updatePlan((p) => { p.resources = p.resources.filter((r) => r.id !== id); });
    };
    const handleLockFinal = () => {
        const notOk = readiness.filter((r) => !r.ok).length;
        if (notOk > 2 && !confirm(`仍有 ${notOk} 项未就绪，是否仍锁定最终方案？`))
            return;
        updatePlan((p) => {
            p.locked = true;
            p.lockedVer = p.refVersion + '-FINAL';
            p.lockedAt = new Date().toLocaleString('zh-CN');
            p.finalStatus = '已锁定';
        });
        message.success('最终方案已锁定，可进入开班申报环节');
    };
    // ---- 工种表格列 ----
    const occColumns = [
        { title: '工种', dataIndex: 'occ', render: (v) => v || '—' },
        { title: '等级', dataIndex: 'level', render: (v) => v || '—' },
        { title: '定稿人数', dataIndex: 'finalCount', render: (v, _, i) => _jsx(InputNumber, { size: "small", min: 0, value: v, style: { width: 80 }, disabled: plan.locked, onChange: (val) => updatePlan((p) => { p.occFinal[i].finalCount = val || 0; }) }) },
        { title: '首期开班', dataIndex: 'firstBatch', render: (v, _, i) => _jsx(Select, { size: "small", value: String(v), style: { width: 70 }, disabled: plan.locked, onChange: (val) => updatePlan((p) => { p.occFinal[i].firstBatch = val === 'true'; }), options: [{ value: 'true', label: '是' }, { value: 'false', label: '否' }] }) },
        { title: '培训机构', dataIndex: 'institute', render: (v, _, i) => _jsx(Select, { size: "small", value: v, style: { width: 180 }, disabled: plan.locked, onChange: (val) => updatePlan((p) => { p.occFinal[i].institute = val; }), options: INSTITUTES.map((ins) => ({ value: ins, label: ins })) }) },
        { title: '确认', dataIndex: 'confirmed', render: (v) => _jsx("span", { className: `status-badge ${v ? 'st-ok' : 'st-warn'}`, children: v ? '已确认' : '待确认' }) },
        { title: '操作', key: 'actions', render: (_, r, i) => plan.locked ? '—' : _jsx(Button, { type: "link", size: "small", disabled: r.confirmed, onClick: () => handleConfirmOcc(i), children: "\u786E\u8BA4" }) },
    ];
    // ---- 资源表格列 ----
    const resColumns = [
        { title: '资源类型', dataIndex: 'type', render: (v, _, i) => _jsx(Select, { size: "small", value: v, style: { width: 100 }, disabled: plan.locked, onChange: (val) => updatePlan((p) => { p.resources[i].type = val; }), options: ['培训机构', '师资', '场地', '课件', '设备', '其他'].map((t) => ({ value: t, label: t })) }) },
        { title: '名称/机构', dataIndex: 'name', render: (v, _, i) => _jsx(Input, { size: "small", value: v, disabled: plan.locked, onChange: (e) => updatePlan((p) => { p.resources[i].name = e.target.value; }) }) },
        { title: '落实内容', dataIndex: 'content', render: (v, _, i) => _jsx(Input, { size: "small", value: v, disabled: plan.locked, onChange: (e) => updatePlan((p) => { p.resources[i].content = e.target.value; }) }) },
        { title: '负责人', dataIndex: 'owner', render: (v, _, i) => _jsx(Input, { size: "small", value: v, disabled: plan.locked, onChange: (e) => updatePlan((p) => { p.resources[i].owner = e.target.value; }) }) },
        { title: '状态', dataIndex: 'status', render: (v, _, i) => _jsx(Select, { size: "small", value: v, style: { width: 90 }, disabled: plan.locked, onChange: (val) => updatePlan((p) => { p.resources[i].status = val; }), options: ['待落实', '落实中', '已落实', '审批中'].map((s) => ({ value: s, label: s })) }) },
        { title: '操作', key: 'actions', render: (_, r) => plan.locked ? '—' : _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelResource(r.id), children: "\u5220\u9664" }) },
    ];
    return (_jsxs("div", { className: "fp-page-container", children: [_jsxs("header", { className: "fp-topbar", children: [_jsxs("div", { className: "fp-topbar-left", children: [_jsxs("a", { className: "fp-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "fp-title", children: "\u6700\u7EC8\u65B9\u6848" }), _jsx("span", { className: "fp-sub", children: currentProject ? `${currentProject.enterprise} · 方案定稿 · 步骤 3 / 9` : '方案定稿 · 分班排课定稿 · 步骤 3 / 9' })] })] }), _jsxs("span", { className: "fp-process-badge", children: [_jsx(FileProtectOutlined, {}), " \u521D\u6B65\u65B9\u6848 \u2192 \u6700\u7EC8\u65B9\u6848 \u2192 \u5F00\u73ED\u7533\u62A5"] })] }), _jsxs("main", { className: "fp-page", children: [_jsxs("div", { className: "fp-card", children: [_jsxs("div", { className: "fp-card-head", children: [_jsxs("div", { className: "fp-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE\uFF08\u627F\u63A5\u521D\u6B65\u65B9\u6848\uFF0C\u5B8C\u6210\u5B9A\u7A3F\u4E0E\u8D44\u6E90\u786E\u8BA4\uFF09" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && (_jsxs("div", { className: "fp-project-banner", children: [_jsxs("div", { className: "fp-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "fp-banner-item", children: [_jsx("label", { children: "\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "fp-banner-item", children: [_jsx("label", { children: "\u521D\u6B65\u65B9\u6848" }), _jsx("strong", { children: plan.refVersion })] }), _jsxs("div", { className: "fp-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A\u786E\u8BA4" }), _jsx("strong", { children: plan.enterpriseConfirm })] }), _jsxs("div", { className: "fp-banner-item", children: [_jsx("label", { children: "\u6700\u7EC8\u72B6\u6001" }), _jsx("strong", { children: plan.finalStatus })] }), _jsxs("div", { className: "fp-banner-item", children: [_jsx("label", { children: "\u8D1F\u8D23\u4EBA" }), _jsx("strong", { children: currentProject.owner || '—' })] })] }))] }), !currentProject ? (_jsxs("div", { className: "fp-empty", children: [_jsx(FileProtectOutlined, { style: { fontSize: 48, color: '#E2E8F0', marginBottom: 16 } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u5B8C\u6210\u6700\u7EC8\u65B9\u6848\u5B9A\u7A3F\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u9700\u5148\u5B8C\u6210 \u521D\u6B65\u65B9\u6848\uFF1B\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u8FDB\u5165\u3002" })] })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "fp-flow-hint", children: [_jsx(InfoCircleOutlined, { style: { color: '#059669' } }), ' ', "\u4F9D\u636E\u9700\u6C42\u89C4\u683C\uFF1A", _jsx("strong", { children: "\u65B9\u6848\u5B9A\u7A3F\u786E\u8BA4 \u2192 \u5206\u73ED\u5B9A\u7A3F \u2192 \u6392\u8BFE\u5B9A\u7A3F\u4E0E\u53D1\u5E03\u6700\u7EC8\u8BFE\u8868 \u2192 \u7248\u672C\u9501\u5B9A" })] }), _jsxs("div", { className: "fp-stat-row", children: [_jsxs("div", { className: "fp-stat-card final", children: [_jsx("div", { className: "fp-stat-val", children: stats.occ }), _jsx("div", { className: "fp-stat-lbl", children: "\u5B9A\u7A3F\u5DE5\u79CD\u6570" })] }), _jsxs("div", { className: "fp-stat-card", children: [_jsxs("div", { className: "fp-stat-val", children: [stats.confirmed, "/", stats.occ] }), _jsx("div", { className: "fp-stat-lbl", children: "\u5DE5\u79CD\u5DF2\u786E\u8BA4" })] }), _jsxs("div", { className: "fp-stat-card warn", children: [_jsxs("div", { className: "fp-stat-val", children: [stats.pct, "%"] }), _jsx("div", { className: "fp-stat-lbl", children: "\u8D44\u6E90\u843D\u5B9E\u7387" })] }), _jsxs("div", { className: `fp-stat-card ${plan.locked ? 'final' : 'danger'}`, children: [_jsx("div", { className: "fp-stat-val", children: plan.locked ? '已锁' : '未锁' }), _jsx("div", { className: "fp-stat-lbl", children: "\u65B9\u6848\u9501\u5B9A" })] })] }), _jsx("div", { className: "fp-card", children: _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                                        {
                                            key: 'finalize', label: '方案定稿',
                                            children: (_jsxs("div", { children: [plan.locked && (_jsxs("div", { className: "fp-locked-banner", children: [_jsx(LockOutlined, { style: { color: '#059669' } }), _jsxs("span", { children: ["\u6700\u7EC8\u65B9\u6848\u5DF2\u9501\u5B9A \u00B7 \u7248\u672C ", _jsx("strong", { children: plan.lockedVer }), " \u00B7 \u9501\u5B9A\u65F6\u95F4 ", plan.lockedAt] })] })), _jsxs("div", { className: "fp-form-grid", children: [_jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u5F15\u7528\u521D\u6B65\u65B9\u6848\u7248\u672C" }), _jsx(Input, { value: plan.refVersion, disabled: true })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u57F9\u517B\u6A21\u5F0F" }), _jsx(Input, { value: plan.refMode, disabled: true })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u89C4\u5212\u5468\u671F" }), _jsx(Input, { value: plan.refYears, disabled: true })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u4F01\u4E1A\u786E\u8BA4\u72B6\u6001" }), _jsx(Select, { value: plan.enterpriseConfirm, style: { width: '100%' }, disabled: plan.locked, onChange: (v) => updatePlan((p) => { p.enterpriseConfirm = v; }), options: ['待确认', '已确认', '有异议'].map((s) => ({ value: s, label: s })) })] }), _jsxs("div", { className: "fp-form-group full", children: [_jsx("label", { children: "\u4F01\u4E1A\u786E\u8BA4\u610F\u89C1" }), _jsx(Input.TextArea, { value: plan.enterpriseNote, rows: 3, disabled: plan.locked, placeholder: "\u4F01\u4E1A\u6CD5\u52A1/\u57F9\u8BAD\u8D1F\u8D23\u4EBA\u53CD\u9988\u610F\u89C1\u2026", onChange: (e) => updatePlan((p) => { p.enterpriseNote = e.target.value; }) })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u5B9A\u7A3F\u65E5\u671F" }), _jsx(Input, { type: "date", value: plan.finalizeDate, disabled: plan.locked, onChange: (e) => updatePlan((p) => { p.finalizeDate = e.target.value; }) })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u6700\u7EC8\u65B9\u6848\u72B6\u6001" }), _jsx(Select, { value: plan.finalStatus, style: { width: '100%' }, disabled: plan.locked, onChange: (v) => updatePlan((p) => { p.finalStatus = v; }), options: ['编制中', '待锁定', '已锁定'].map((s) => ({ value: s, label: s })) })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u8BA1\u5212\u9996\u671F\u5F00\u73ED\u65E5\u671F" }), _jsx(Input, { type: "date", value: plan.firstOpenDate, disabled: plan.locked, onChange: (e) => updatePlan((p) => { p.firstOpenDate = e.target.value; }) })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u9996\u671F\u5F00\u73ED\u5DE5\u79CD" }), _jsx(Input, { value: plan.firstOpenOcc, disabled: plan.locked, placeholder: "\u5982\uFF1A\u7535\u5DE5 \u9AD8\u7EA7\u5DE5", onChange: (e) => updatePlan((p) => { p.firstOpenOcc = e.target.value; }) })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u8BA1\u5212\u73ED\u7EA7\u6570" }), _jsx(InputNumber, { min: 1, value: plan.planClassCount, style: { width: '100%' }, disabled: plan.locked, onChange: (v) => updatePlan((p) => { p.planClassCount = v || 1; }) })] }), _jsxs("div", { className: "fp-form-group", children: [_jsx("label", { children: "\u6BCF\u671F\u89C4\u6A21\uFF08\u4EBA/\u73ED\uFF09" }), _jsx(InputNumber, { min: 10, value: plan.classSize, style: { width: '100%' }, disabled: plan.locked, onChange: (v) => updatePlan((p) => { p.classSize = v || 30; }) })] }), _jsxs("div", { className: "fp-form-group full", children: [_jsx("label", { children: "\u4E0A\u8BFE\u5730\u70B9\uFF08\u7EC8\u7A3F\uFF09" }), _jsx(Input, { value: plan.finalVenue, disabled: plan.locked, placeholder: "\u4F01\u4E1A\u786E\u8BA4\u540E\u7684\u57F9\u8BAD\u573A\u5730", onChange: (e) => updatePlan((p) => { p.finalVenue = e.target.value; }) })] })] }), _jsxs("div", { className: "fp-card", children: [_jsx("div", { className: "fp-card-head", children: _jsxs("div", { className: "fp-card-head-title", children: [_jsx(CheckCircleOutlined, {}), " \u5DE5\u79CD\u7B49\u7EA7\u7EC8\u7A3F\u786E\u8BA4"] }) }), _jsx(Table, { dataSource: plan.occFinal, columns: occColumns, rowKey: "id", pagination: false, locale: { emptyText: '请先在初步方案中维护工种规划' } })] }), _jsxs("div", { className: "fp-card", children: [_jsxs("div", { className: "fp-card-head", children: [_jsxs("div", { className: "fp-card-head-title", children: [_jsx(SafetyCertificateOutlined, {}), " \u57F9\u8BAD\u8D44\u6E90\u843D\u5B9E\u8868"] }), _jsx(Button, { size: "small", icon: _jsx(PlusOutlined, {}), disabled: plan.locked, onClick: handleAddResource, children: "\u65B0\u589E\u8D44\u6E90\u9879" })] }), _jsx(Table, { dataSource: plan.resources, columns: resColumns, rowKey: "id", pagination: false, locale: { emptyText: '暂无资源项' } })] }), _jsxs("div", { className: "fp-card", children: [_jsx("div", { className: "fp-card-head", children: _jsxs("div", { className: "fp-card-head-title", children: [_jsx(CheckCircleOutlined, {}), " \u5F00\u73ED\u5C31\u7EEA\u68C0\u67E5\u6E05\u5355"] }) }), _jsx("ul", { className: "fp-checklist", children: readiness.map((it, i) => (_jsxs("li", { children: [_jsx("span", { children: it.label }), _jsx("span", { className: `status-badge ${it.ok ? 'st-ok' : 'st-no'}`, children: it.ok ? '已完成' : '未完成' })] }, i))) }), _jsxs("div", { className: "fp-toolbar", children: [_jsx(Button, { className: "btn-final", icon: _jsx(LockOutlined, {}), onClick: handleLockFinal, style: { display: plan.locked ? 'none' : undefined }, children: "\u9501\u5B9A\u6700\u7EC8\u65B9\u6848" }), _jsx(Button, { type: "primary", icon: _jsx(FileTextOutlined, {}), onClick: () => message.success('已生成培养方案文档'), children: "\u751F\u6210\u57F9\u517B\u65B9\u6848\u6587\u6863" }), _jsx(Button, { icon: _jsx(BellOutlined, {}), onClick: () => message.success('已将开班安排反馈企业'), children: "\u53CD\u9988\u4F01\u4E1A\u5F00\u73ED\u5B89\u6392" })] })] })] })),
                                        },
                                        {
                                            key: 'scheduling', label: '分班排课定稿',
                                            children: (_jsxs("div", { children: [_jsxs("div", { className: "fp-card-head", children: [_jsxs("div", { className: "fp-card-head-title", children: [_jsx(NodeIndexOutlined, {}), " \u5206\u73ED\u6392\u8BFE\uFF08\u5B9A\u7A3F\uFF09"] }), _jsx("span", { className: "fp-hint", children: "\u786E\u8BA4\u5206\u73ED\u5B9A\u7A3F\u5E76\u53D1\u5E03\u6700\u7EC8\u8BFE\u8868" })] }), _jsx("p", { style: { fontSize: 14, color: '#64748B', lineHeight: 1.8, marginBottom: 20 }, children: "\u5B8C\u6210\u5206\u73ED\u5B9A\u7A3F \u2192 \u6392\u8BFE\u4E0E\u51B2\u7A81\u68C0\u6D4B \u2192 \u53D1\u5E03\u6700\u7EC8\u8BFE\u8868\u3002" }), _jsxs("div", { className: "fp-sub-stat-row", children: [_jsxs("div", { className: "fp-sub-stat", children: [_jsx("div", { className: "fp-sub-stat-v", children: "0" }), _jsx("div", { className: "fp-sub-stat-l", children: "\u5F85\u5206\u73ED" })] }), _jsxs("div", { className: "fp-sub-stat", children: [_jsx("div", { className: "fp-sub-stat-v", children: "0" }), _jsx("div", { className: "fp-sub-stat-l", children: "\u73ED\u7EA7" })] }), _jsxs("div", { className: "fp-sub-stat", children: [_jsx("div", { className: "fp-sub-stat-v", children: "0" }), _jsx("div", { className: "fp-sub-stat-l", children: "\u8BFE\u7A0B" })] }), _jsxs("div", { className: "fp-sub-stat", children: [_jsx("div", { className: "fp-sub-stat-v", children: "\u2014" }), _jsx("div", { className: "fp-sub-stat-l", children: "\u8BFE\u8868" })] })] }), _jsxs("div", { className: "fp-toolbar", children: [_jsx(Button, { className: "btn-final", icon: _jsx(ApartmentOutlined, {}), children: "\u534F\u540C\u5DE5\u4F5C\u53F0\uFF08\u5B9A\u7A3F\uFF09" }), _jsx(Button, { type: "primary", icon: _jsx(NodeIndexOutlined, {}), children: "\u5206\u73ED\u7BA1\u7406" }), _jsx(Button, { type: "primary", icon: _jsx(CalendarOutlined, {}), children: "\u6392\u8BFE\u7BA1\u7406" })] })] })),
                                        },
                                    ] }) }), _jsxs("div", { className: "fp-footer-actions", children: [_jsx(Button, { icon: _jsx(FileTextOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-preliminary-plan'); }, children: "\u8FD4\u56DE\u521D\u6B65\u65B9\u6848" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { className: "btn-final", icon: _jsx(ArrowRightOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-class-declaration'); }, children: "\u5B8C\u6210\u6700\u7EC8\u65B9\u6848\uFF0C\u8FDB\u5165\u5F00\u73ED\u7533\u62A5" })] })] }))] })] }));
}

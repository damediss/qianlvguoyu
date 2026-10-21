import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, Input, Button, Table, Tabs, InputNumber, message } from 'antd';
import { ArrowLeftOutlined, FileTextOutlined, InfoCircleOutlined, FolderOpenOutlined, UnorderedListOutlined, SaveOutlined, PlusOutlined, DeleteOutlined, SyncOutlined, CalendarOutlined, UserSwitchOutlined, ArrowRightOutlined, ThunderboltOutlined, NodeIndexOutlined, ApartmentOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './PreliminaryPlanPage.css';
// ============================================================
// 模拟数据
// ============================================================
const DEFAULT_TASKS = [
    { id: 1, name: '2026年度安全培训计划编制', owner: '张明', enterprise: '广西建工集团', occupation: '安全员', planCount: 120 },
    { id: 2, name: '电工高级班认定材料整理', owner: '李芳', enterprise: '南宁供电局', occupation: '电工', planCount: 42 },
    { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', enterprise: '柳州钢铁有限公司', occupation: '焊工', planCount: 36 },
    { id: 4, name: '焊工中级班排课协调', owner: '赵丽', enterprise: '桂林旅游股份公司', occupation: '焊工', planCount: 28 },
    { id: 5, name: '培训档案年度归档', owner: '陈伟', enterprise: '梧州港务集团', occupation: '起重工', planCount: 15 },
];
const OCCUPATIONS = ['电工', '焊工', '起重工', '安全员', '架子工'];
const LEVELS = ['初级', '中级', '高级', '高级工', '技师', '高级技师'];
const TRAIN_FORMS = ['集中培训', '理实一体', '线上+线下', '企业内训'];
function defaultPlan() {
    const y = new Date().getFullYear();
    return {
        planYears: 5, trainMode: '新型学徒制', startYear: y, planStatus: '草案',
        planGoal: '', planVenue: '总部培训中心', occupations: [], annuals: [],
    };
}
// ============================================================
// 主页面
// ============================================================
export function PreliminaryPlanPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [plan, setPlan] = useState(defaultPlan);
    const [activeTab, setActiveTab] = useState('basic');
    const [occIdCounter, setOccIdCounter] = useState(1);
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    // 初始化 / 切换项目
    useEffect(() => {
        if (!currentProject) {
            setPlan(defaultPlan());
            return;
        }
        const newPlan = defaultPlan();
        const occ1 = { id: 1, occ: currentProject.occupation || '电工', level: '高级工', planCount: 40, identified: 32, batches: 2, form: '理实一体', quarter: 'Q2' };
        const occ2 = { id: 2, occ: '焊工', level: '技师', planCount: 24, identified: 18, batches: 1, form: '集中培训', quarter: 'Q3' };
        newPlan.occupations = [occ1, occ2];
        newPlan.planGoal = `基于「${currentProject.enterprise || '企业'}」人才识别结果，制定 ${newPlan.startYear}–${newPlan.startYear + 4} 年技能人才梯队培养规划，覆盖${currentProject.occupation || '主要工种'}等岗位技能提升与认定衔接。`;
        setOccIdCounter(3);
        newPlan.annuals = buildAnnuals(newPlan);
        setPlan(newPlan);
    }, [selectedProjectId]);
    const readForm = useCallback(() => {
        // 表单值由 Ant Design Form 管理，这里只是占位
    }, []);
    const buildAnnuals = useCallback((p) => {
        const years = p.planYears || 5;
        const start = p.startYear || new Date().getFullYear();
        const total = p.occupations.reduce((s, o) => s + (o.planCount || 0), 0);
        const perYear = Math.ceil(total / years) || 0;
        const annuals = [];
        for (let i = 0; i < years; i++) {
            const y = start + i;
            const occs = p.occupations.map((o) => o.occ).filter(Boolean).slice(0, 2).join('、') || '—';
            annuals.push({ year: y, classes: Math.max(1, Math.ceil((p.occupations.length || 1) / years)), trainees: perYear, focus: occs, budget: (perYear * 0.35).toFixed(1), note: i === 0 ? '启动年' : '' });
        }
        return annuals;
    }, []);
    const updatePlan = useCallback((updater) => {
        setPlan((prev) => { const next = { ...prev, occupations: [...prev.occupations], annuals: [...prev.annuals] }; updater(next); return next; });
    }, []);
    // 统计
    const stats = useMemo(() => ({
        occCount: plan.occupations.length,
        total: plan.occupations.reduce((s, o) => s + (o.planCount || 0), 0),
        identified: plan.occupations.reduce((s, o) => s + (o.identified || 0), 0),
        years: plan.planYears || 5,
    }), [plan]);
    const handleGenDraft = () => {
        if (!currentProject)
            return;
        updatePlan((p) => {
            const newId = occIdCounter;
            p.occupations = [
                { id: newId, occ: currentProject.occupation || '电工', level: '高级工', planCount: Math.max(30, currentProject.planCount || 40), identified: 32, batches: 2, form: '理实一体', quarter: 'Q2' },
                { id: newId + 1, occ: '安全员', level: '中级', planCount: 20, identified: 15, batches: 1, form: '集中培训', quarter: 'Q1' },
            ];
            setOccIdCounter(newId + 2);
            p.planGoal = `【智能草案】${currentProject.enterprise || '企业'} ${p.startYear} 年起 ${p.planYears} 年规划：以${currentProject.occupation || '电工'}为主体，分 ${p.planYears} 年梯次培养。`;
            p.annuals = buildAnnuals(p);
        });
        message.success('已根据人才识别数据生成规划草案');
    };
    const handleSaveBasic = () => {
        updatePlan((p) => { p.annuals = buildAnnuals(p); });
        message.success('方案概要已保存');
    };
    const handleAddOcc = () => {
        const newId = occIdCounter;
        setOccIdCounter(newId + 1);
        updatePlan((p) => { p.occupations.push({ id: newId, occ: '', level: '中级', planCount: 0, identified: 0, batches: 1, form: '集中培训', quarter: 'Q1' }); });
    };
    const handleDelOcc = (id) => {
        updatePlan((p) => { p.occupations = p.occupations.filter((o) => o.id !== id); });
    };
    const handleSyncAnnual = () => {
        updatePlan((p) => { p.annuals = buildAnnuals(p); });
        message.success('已按工种规划汇总年度计划');
    };
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
    };
    // ---- 工种表格列 ----
    const occColumns = [
        { title: '工种', dataIndex: 'occ', render: (v, _, i) => _jsx(Input, { size: "small", value: v, onChange: (e) => updatePlan((p) => { p.occupations[i].occ = e.target.value; }) }) },
        { title: '等级', dataIndex: 'level', render: (v, _, i) => _jsx(Select, { size: "small", value: v, style: { width: 90 }, onChange: (val) => updatePlan((p) => { p.occupations[i].level = val; }), options: LEVELS.map((l) => ({ value: l, label: l })) }) },
        { title: '规划人数', dataIndex: 'planCount', render: (v, _, i) => _jsx(InputNumber, { size: "small", min: 0, value: v, style: { width: 80 }, onChange: (val) => updatePlan((p) => { p.occupations[i].planCount = val || 0; }) }) },
        { title: '已识别人数', dataIndex: 'identified', render: (v, _, i) => _jsx(InputNumber, { size: "small", min: 0, value: v, style: { width: 80 }, onChange: (val) => updatePlan((p) => { p.occupations[i].identified = val || 0; }) }) },
        { title: '计划批次', dataIndex: 'batches', render: (v, _, i) => _jsx(InputNumber, { size: "small", min: 1, value: v, style: { width: 70 }, onChange: (val) => updatePlan((p) => { p.occupations[i].batches = val || 1; }) }) },
        { title: '培训形式', dataIndex: 'form', render: (v, _, i) => _jsx(Select, { size: "small", value: v, style: { width: 110 }, onChange: (val) => updatePlan((p) => { p.occupations[i].form = val; }), options: TRAIN_FORMS.map((f) => ({ value: f, label: f })) }) },
        { title: '计划开班', dataIndex: 'quarter', render: (v, _, i) => _jsx(Input, { size: "small", value: v, style: { width: 70 }, placeholder: "Q1-Q4", onChange: (e) => updatePlan((p) => { p.occupations[i].quarter = e.target.value; }) }) },
        { title: '操作', key: 'actions', render: (_, r) => _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelOcc(r.id), children: "\u5220\u9664" }) },
    ];
    // ---- 年度计划列 ----
    const annualColumns = [
        { title: '年度', dataIndex: 'year', render: (v) => _jsx("strong", { children: v }) },
        { title: '计划开班数', dataIndex: 'classes', render: (v, _, i) => _jsx(InputNumber, { size: "small", min: 0, value: v, onChange: (val) => updatePlan((p) => { p.annuals[i].classes = val || 0; }) }) },
        { title: '计划参训人次', dataIndex: 'trainees', render: (v, _, i) => _jsx(InputNumber, { size: "small", min: 0, value: v, onChange: (val) => updatePlan((p) => { p.annuals[i].trainees = val || 0; }) }) },
        { title: '重点工种', dataIndex: 'focus', render: (v, _, i) => _jsx(Input, { size: "small", value: v, onChange: (e) => updatePlan((p) => { p.annuals[i].focus = e.target.value; }) }) },
        { title: '预算口径（万元）', dataIndex: 'budget', render: (v, _, i) => _jsx(InputNumber, { size: "small", min: 0, step: 0.1, value: parseFloat(v), onChange: (val) => updatePlan((p) => { p.annuals[i].budget = String(val ?? 0); }) }) },
        { title: '备注', dataIndex: 'note', render: (v, _, i) => _jsx(Input, { size: "small", value: v, onChange: (e) => updatePlan((p) => { p.annuals[i].note = e.target.value; }) }) },
    ];
    return (_jsxs("div", { className: "prelim-plan-page", children: [_jsxs("header", { className: "pp-topbar", children: [_jsxs("div", { className: "pp-topbar-left", children: [_jsxs("a", { className: "pp-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "pp-title", children: "\u521D\u6B65\u65B9\u6848" }), _jsx("span", { className: "pp-sub", children: currentProject ? `${currentProject.enterprise} · 培训规划草案 · 步骤 2 / 9` : '规划草案 · 分班排课草案 · 步骤 2 / 9' })] })] }), _jsxs("span", { className: "pp-process-badge", children: [_jsx(FileTextOutlined, {}), " \u4EBA\u624D\u8BC6\u522B \u2192 \u521D\u6B65\u65B9\u6848 \u2192 \u6700\u7EC8\u65B9\u6848"] })] }), _jsxs("main", { className: "pp-page", children: [_jsxs("div", { className: "pp-card", children: [_jsxs("div", { className: "pp-card-head", children: [_jsxs("div", { className: "pp-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "pp-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE\uFF08\u57FA\u4E8E\u4EBA\u624D\u8BC6\u522B\u7ED3\u679C\u7F16\u5236 3\u20135 \u5E74\u89C4\u5212\u8349\u6848\uFF09" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && (_jsxs("div", { className: "pp-project-banner", children: [_jsxs("div", { className: "pp-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "pp-banner-item", children: [_jsx("label", { children: "\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "pp-banner-item", children: [_jsx("label", { children: "\u4E3B\u5DE5\u79CD" }), _jsx("strong", { children: currentProject.occupation || '—' })] }), _jsxs("div", { className: "pp-banner-item", children: [_jsx("label", { children: "\u5DF2\u786E\u8BA4\u4EBA\u624D" }), _jsx("strong", { children: "32 \u4EBA" })] }), _jsxs("div", { className: "pp-banner-item", children: [_jsx("label", { children: "\u65B9\u6848\u72B6\u6001" }), _jsx("strong", { children: plan.planStatus })] }), _jsxs("div", { className: "pp-banner-item", children: [_jsx("label", { children: "\u8D1F\u8D23\u4EBA" }), _jsx("strong", { children: currentProject.owner || '—' })] })] }))] }), !currentProject ? (_jsxs("div", { className: "pp-empty", children: [_jsx(FileTextOutlined, { style: { fontSize: 48, color: '#E2E8F0', marginBottom: 16 } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u518D\u7F16\u5236\u521D\u6B65\u57F9\u8BAD\u89C4\u5212\u65B9\u6848\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u5E26\u9879\u76EE\u53C2\u6570\u8FDB\u5165\u3002" })] })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "pp-flow-hint", children: [_jsx(InfoCircleOutlined, { style: { color: '#2A6DFF' } }), ' ', "\u4F9D\u636E\u9700\u6C42\u89C4\u683C\uFF1A", _jsx("strong", { children: "\u89C4\u5212\u8349\u6848 \u2192 \u5206\u73ED\u89C4\u5219/\u667A\u80FD\u5206\u73ED \u2192 \u6392\u8BFE\u5B89\u6392/\u51B2\u7A81\u68C0\u6D4B" }), "\uFF08\u6CD5\u00B7\u65E5\u7A0B\u8BA1\u5212\uFF1A\u5BFC\u5165\u540D\u5355 \u2192 \u667A\u80FD\u5206\u73ED \u2192 \u6392\u8BFE \u2192 \u68C0\u6D4B\u51B2\u7A81\uFF09"] }), _jsxs("div", { className: "pp-stat-row", children: [_jsxs("div", { className: "pp-stat-card plan", children: [_jsx("div", { className: "pp-stat-val", children: stats.occCount }), _jsx("div", { className: "pp-stat-lbl", children: "\u89C4\u5212\u5DE5\u79CD\u6570" })] }), _jsxs("div", { className: "pp-stat-card", children: [_jsx("div", { className: "pp-stat-val", children: stats.total }), _jsx("div", { className: "pp-stat-lbl", children: "\u89C4\u5212\u53C2\u8BAD\u4EBA\u6B21" })] }), _jsxs("div", { className: "pp-stat-card success", children: [_jsx("div", { className: "pp-stat-val", children: stats.identified }), _jsx("div", { className: "pp-stat-lbl", children: "\u5DF2\u8BC6\u522B\u4EBA\u6570\u8854\u63A5" })] }), _jsxs("div", { className: "pp-stat-card warn", children: [_jsx("div", { className: "pp-stat-val", children: stats.years }), _jsx("div", { className: "pp-stat-lbl", children: "\u89C4\u5212\u5468\u671F\uFF08\u5E74\uFF09" })] })] }), _jsx("div", { className: "pp-card", children: _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                                        {
                                            key: 'basic', label: '方案概要',
                                            children: (_jsxs("div", { children: [_jsxs("div", { className: "pp-card-head", children: [_jsxs("div", { className: "pp-card-head-title", children: [_jsx(FileTextOutlined, {}), " \u89C4\u5212\u65B9\u6848\u6982\u8981"] }), _jsx("span", { className: "pp-hint", children: "\u57F9\u8BAD\u8BA1\u5212\u5236\u5B9A\uFF1A\u5DE5\u79CD\u3001\u7B49\u7EA7\u3001\u65F6\u95F4\u3001\u5730\u70B9\u3001\u57F9\u517B\u6A21\u5F0F" })] }), _jsxs("div", { className: "pp-form-grid", children: [_jsxs("div", { className: "pp-form-group", children: [_jsx("label", { children: "\u89C4\u5212\u5468\u671F" }), _jsx(Select, { value: plan.planYears, style: { width: '100%' }, onChange: (v) => updatePlan((p) => { p.planYears = v; }), options: [3, 4, 5].map((n) => ({ value: n, label: `${n} 年` })) })] }), _jsxs("div", { className: "pp-form-group", children: [_jsx("label", { children: "\u57F9\u517B\u6A21\u5F0F" }), _jsx(Select, { value: plan.trainMode, style: { width: '100%' }, onChange: (v) => updatePlan((p) => { p.trainMode = v; }), options: ['新型学徒制', '岗前培训', '技能提升', '企业内训'].map((m) => ({ value: m, label: m })) })] }), _jsxs("div", { className: "pp-form-group", children: [_jsx("label", { children: "\u89C4\u5212\u8D77\u59CB\u5E74\u5EA6" }), _jsx(InputNumber, { value: plan.startYear, style: { width: '100%' }, min: 2024, max: 2035, onChange: (v) => updatePlan((p) => { p.startYear = v || new Date().getFullYear(); }) })] }), _jsxs("div", { className: "pp-form-group", children: [_jsx("label", { children: "\u65B9\u6848\u72B6\u6001" }), _jsx(Select, { value: plan.planStatus, style: { width: '100%' }, onChange: (v) => updatePlan((p) => { p.planStatus = v; }), options: ['草案', '编制中', '已定稿'].map((s) => ({ value: s, label: s })) })] }), _jsxs("div", { className: "pp-form-group full", children: [_jsx("label", { children: "\u89C4\u5212\u76EE\u6807\u6458\u8981" }), _jsx(Input.TextArea, { value: plan.planGoal, rows: 4, onChange: (e) => updatePlan((p) => { p.planGoal = e.target.value; }), placeholder: "\u6839\u636E\u4EBA\u624D\u8BC6\u522B\u7ED3\u679C\uFF0C\u63CF\u8FF0\u4F01\u4E1A\u6280\u80FD\u4EBA\u624D\u68AF\u961F\u5EFA\u8BBE\u76EE\u6807\u2026" })] }), _jsxs("div", { className: "pp-form-group full", children: [_jsx("label", { children: "\u4E3B\u8981\u57F9\u8BAD\u5730\u70B9" }), _jsx(Input, { value: plan.planVenue, onChange: (e) => updatePlan((p) => { p.planVenue = e.target.value; }), placeholder: "\u5982\uFF1A\u603B\u90E8\u57F9\u8BAD\u4E2D\u5FC3\u3001\u534E\u4E1C\u5B9E\u8BAD\u57FA\u5730" })] })] }), _jsxs("div", { className: "pp-toolbar", children: [_jsx(Button, { className: "btn-plan", icon: _jsx(ThunderboltOutlined, {}), onClick: handleGenDraft, children: "\u667A\u80FD\u751F\u6210\u89C4\u5212\u8349\u6848" }), _jsx(Button, { icon: _jsx(SaveOutlined, {}), onClick: handleSaveBasic, children: "\u4FDD\u5B58\u6982\u8981" })] })] })),
                                        },
                                        {
                                            key: 'occupation', label: '工种等级规划',
                                            children: (_jsxs(_Fragment, { children: [_jsxs("div", { className: "pp-card-head", children: [_jsxs("div", { className: "pp-card-head-title", children: [_jsx(FileTextOutlined, {}), " \u5DE5\u79CD\u7B49\u7EA7\u89C4\u5212\u8868"] }), _jsx(Button, { size: "small", icon: _jsx(PlusOutlined, {}), onClick: handleAddOcc, children: "\u65B0\u589E\u5DE5\u79CD" })] }), _jsx("p", { style: { fontSize: 13, color: '#64748B', marginBottom: 16 }, children: "\u5BF9\u63A5\u4EBA\u624D\u8BC6\u522B\u9700\u6C42\u6E05\u5355\uFF0C\u660E\u786E\u5404\u5DE5\u79CD\u7B49\u7EA7\u53C2\u8BAD\u89C4\u6A21\u4E0E\u6279\u6B21\u5B89\u6392\u3002" }), _jsx(Table, { dataSource: plan.occupations, columns: occColumns, rowKey: "id", pagination: false, locale: { emptyText: '暂无工种规划，请新增或智能生成草案' } }), _jsxs("div", { className: "pp-card", style: { marginTop: 24 }, children: [_jsx("div", { className: "pp-card-head", children: _jsxs("div", { className: "pp-card-head-title", children: [_jsx(UserSwitchOutlined, {}), " \u5173\u8054\u4EBA\u624D\u8BC6\u522B\u6570\u636E"] }) }), _jsxs("div", { style: { fontSize: 14, lineHeight: 1.8, color: '#64748B' }, children: [_jsxs("p", { children: ["\u4F01\u4E1A ", _jsx("strong", { children: currentProject?.enterprise || '—' }), " \u4EBA\u624D\u8BC6\u522B\u6C47\u603B\uFF1A"] }), _jsxs("ul", { style: { margin: '8px 0 0 20px' }, children: [_jsxs("li", { children: ["\u5DF2\u786E\u8BA4 ", _jsx("strong", { style: { color: '#10B981' }, children: stats.identified }), " \u4EBA"] }), _jsxs("li", { children: ["\u5F85\u786E\u8BA4 ", _jsx("strong", { style: { color: '#F59E0B' }, children: "8" }), " \u4EBA"] }), _jsxs("li", { children: ["\u672A\u8BC6\u522B ", _jsx("strong", { style: { color: '#EF4444' }, children: "4" }), " \u4EBA"] })] }), _jsx("p", { style: { marginTop: 12 }, children: _jsx("a", { onClick: () => { setCurrentProjectId(currentProject?.id || null); navigateTo('zhixun-talent-identification'); }, style: { color: '#2A6DFF', cursor: 'pointer' }, children: "\u67E5\u770B\u4EBA\u624D\u8BC6\u522B\u660E\u7EC6 \u2192" }) })] })] })] })),
                                        },
                                        {
                                            key: 'annual', label: '年度培训计划',
                                            children: (_jsxs(_Fragment, { children: [_jsxs("div", { className: "pp-card-head", children: [_jsxs("div", { className: "pp-card-head-title", children: [_jsx(CalendarOutlined, {}), " \u5E74\u5EA6\u57F9\u8BAD\u8BA1\u5212"] }), _jsx(Button, { size: "small", icon: _jsx(SyncOutlined, {}), onClick: handleSyncAnnual, children: "\u6309\u5DE5\u79CD\u6C47\u603B" })] }), _jsx(Table, { dataSource: plan.annuals, columns: annualColumns, rowKey: "year", pagination: false, locale: { emptyText: '请先维护工种规划，再按工种汇总年度计划' } })] })),
                                        },
                                        {
                                            key: 'scheduling', label: '分班排课工作台',
                                            children: (_jsxs("div", { children: [_jsxs("div", { className: "pp-card-head", children: [_jsxs("div", { className: "pp-card-head-title", children: [_jsx(NodeIndexOutlined, {}), " \u5206\u73ED\u6392\u8BFE\uFF08\u8349\u6848\uFF09"] }), _jsx("span", { className: "pp-hint", children: "\u5B8C\u6574\u529F\u80FD\u8BF7\u8FDB\u5165\u534F\u540C\u5DE5\u4F5C\u53F0" })] }), _jsx("p", { style: { fontSize: 14, color: '#64748B', lineHeight: 1.8, marginBottom: 20 }, children: "\u5BFC\u5165\u5B66\u5458 \u2192 \u5206\u73ED\u89C4\u5219 \u2192 \u667A\u80FD\u5206\u73ED \u2192 \u6392\u8BFE\u5B89\u6392 \u2192 \u51B2\u7A81\u68C0\u6D4B \u2192 \u4FDD\u5B58\u8BFE\u8868\u8349\u6848\u3002" }), _jsxs("div", { className: "pp-sub-stat-row", children: [_jsxs("div", { className: "pp-sub-stat", children: [_jsx("div", { className: "pp-sub-stat-v", children: "0" }), _jsx("div", { className: "pp-sub-stat-l", children: "\u5F85\u5206\u73ED" })] }), _jsxs("div", { className: "pp-sub-stat", children: [_jsx("div", { className: "pp-sub-stat-v", children: "0" }), _jsx("div", { className: "pp-sub-stat-l", children: "\u73ED\u7EA7" })] }), _jsxs("div", { className: "pp-sub-stat", children: [_jsx("div", { className: "pp-sub-stat-v", children: "0" }), _jsx("div", { className: "pp-sub-stat-l", children: "\u8BFE\u7A0B" })] }), _jsxs("div", { className: "pp-sub-stat", children: [_jsx("div", { className: "pp-sub-stat-v", children: "\u2014" }), _jsx("div", { className: "pp-sub-stat-l", children: "\u8BFE\u8868" })] })] }), _jsxs("div", { className: "pp-toolbar", children: [_jsx(Button, { className: "btn-plan", icon: _jsx(ApartmentOutlined, {}), children: "\u534F\u540C\u5DE5\u4F5C\u53F0" }), _jsx(Button, { type: "primary", icon: _jsx(NodeIndexOutlined, {}), children: "\u5206\u73ED\u7BA1\u7406" }), _jsx(Button, { type: "primary", icon: _jsx(CalendarOutlined, {}), children: "\u6392\u8BFE\u7BA1\u7406" })] })] })),
                                        },
                                    ] }) }), _jsxs("div", { className: "pp-footer-actions", children: [_jsx(Button, { icon: _jsx(UserSwitchOutlined, {}), onClick: () => { setCurrentProjectId(currentProject?.id || null); navigateTo('zhixun-talent-identification'); }, children: "\u8FD4\u56DE\u4EBA\u624D\u8BC6\u522B" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { type: "primary", icon: _jsx(ArrowRightOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-final-plan'); }, children: "\u5B8C\u6210\u521D\u6B65\u65B9\u6848\uFF0C\u8FDB\u5165\u6700\u7EC8\u65B9\u6848" })] })] }))] })] }));
}

import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, Input, Button, Table, Modal, message } from 'antd';
import { ArrowLeftOutlined, FolderOpenOutlined, UnorderedListOutlined, NodeIndexOutlined, FileZipOutlined, CloudUploadOutlined, BellOutlined, EditOutlined, CalendarOutlined, ArrowRightOutlined, ExclamationCircleOutlined, CheckCircleOutlined, InfoCircleOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './ClassDeclarationPage.css';
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
const VENUES = ['总部培训中心', '华东实训基地A区', '华南技能中心', '柳州培训基地', '桂林教学点'];
const MATERIAL_KEYS = [
    { key: 'application', label: '开班申请表' },
    { key: 'roster', label: '开班花名册' },
    { key: 'commitment', label: '开班承诺书' },
    { key: 'schedule', label: '已发布课表' },
    { key: 'venue', label: '场地确认单' },
];
const FLOW_LABELS = ['开班申请', '承诺书签署', '人社提交', '审批通过', '正式开班'];
const FLOW_ICONS = [FileZipOutlined, EditOutlined, CloudUploadOutlined, CheckCircleOutlined, ArrowRightOutlined];
function statusClass(status) {
    const map = {
        '待申请': 'st-draft',
        '材料准备中': 'st-prep',
        '已提交人社': 'st-submit',
        '审批中': 'st-review',
        '已批准': 'st-approved',
        '正式开班': 'st-open',
    };
    return map[status] || 'st-draft';
}
function flowIndexForStatus(status) {
    const order = ['待申请', '材料准备中', '已提交人社', '审批中', '已批准', '正式开班'];
    const i = order.indexOf(status);
    if (i <= 0)
        return 0;
    if (i === 1)
        return 1;
    if (i === 2)
        return 2;
    if (i === 3)
        return 3;
    if (i === 4)
        return 4;
    return 5;
}
// ============================================================
// Mock 班级数据生成
// ============================================================
function buildMockRecords(projectId) {
    const task = DEFAULT_TASKS.find((t) => t.id === projectId);
    if (!task)
        return [];
    return [
        {
            classId: 1, className: `${task.occupation || '电工'}高级工1班`, studentCount: 30,
            openDate: '2026-07-01', venue: VENUES[0], ceremonyTime: '09:00',
            headTeacher: task.owner, status: '材料准备中',
            materials: { application: true, roster: true, commitment: false, schedule: true, venue: false },
            commitmentSigned: false, ceremonyDone: false, remark: '', rsbSubmittedAt: null, enterpriseNotified: false,
        },
        {
            classId: 2, className: `${task.occupation || '电工'}中级工1班`, studentCount: 25,
            openDate: '2026-07-15', venue: VENUES[1], ceremonyTime: '10:00',
            headTeacher: '李老师', status: '待申请',
            materials: { application: false, roster: false, commitment: false, schedule: false, venue: false },
            commitmentSigned: false, ceremonyDone: false, remark: '', rsbSubmittedAt: null, enterpriseNotified: false,
        },
    ];
}
function materialsProgress(m) {
    const total = MATERIAL_KEYS.length;
    const done = MATERIAL_KEYS.filter((k) => m[k.key]).length;
    return `${done}/${total}`;
}
// ============================================================
// 主页面
// ============================================================
export function ClassDeclarationPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [records, setRecords] = useState([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    // modal form state
    const [mOpenDate, setMOpenDate] = useState('');
    const [mCeremonyTime, setMCeremonyTime] = useState('09:00');
    const [mVenue, setMVenue] = useState(VENUES[0]);
    const [mHeadTeacher, setMHeadTeacher] = useState('');
    const [mStatus, setMStatus] = useState('待申请');
    const [mCommitment, setMCommitment] = useState(false);
    const [mCeremony, setMCeremony] = useState(false);
    const [mRemark, setMRemark] = useState('');
    const [mMaterials, setMMaterials] = useState({});
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    useEffect(() => {
        if (selectedProjectId) {
            setRecords(buildMockRecords(+selectedProjectId));
        }
        else {
            setRecords([]);
        }
    }, [selectedProjectId]);
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
    };
    const stats = useMemo(() => {
        const total = records.length;
        const pending = records.filter((r) => r.status === '审批中' || r.status === '已提交人社').length;
        const opened = records.filter((r) => r.status === '正式开班').length;
        const ceremony = records.filter((r) => r.ceremonyDone).length;
        return { total, pending, opened, ceremony };
    }, [records]);
    const flowAvg = useMemo(() => {
        if (!records.length)
            return 0;
        return Math.round(records.reduce((s, r) => s + flowIndexForStatus(r.status), 0) / records.length);
    }, [records]);
    const openModal = useCallback((classId) => {
        const r = records.find((x) => x.classId === classId);
        if (!r)
            return;
        setEditingId(classId);
        setMOpenDate(r.openDate || '');
        setMCeremonyTime(r.ceremonyTime || '09:00');
        setMVenue(r.venue || VENUES[0]);
        setMHeadTeacher(r.headTeacher || '');
        setMStatus(r.status);
        setMCommitment(!!r.commitmentSigned);
        setMCeremony(!!r.ceremonyDone);
        setMRemark(r.remark || '');
        setMMaterials({ ...r.materials });
        setModalOpen(true);
    }, [records]);
    const saveModal = useCallback(() => {
        if (editingId == null)
            return;
        setRecords((prev) => prev.map((r) => r.classId === editingId
            ? {
                ...r,
                openDate: mOpenDate,
                ceremonyTime: mCeremonyTime,
                venue: mVenue,
                headTeacher: mHeadTeacher,
                status: mStatus,
                materials: { ...mMaterials },
                commitmentSigned: mCommitment,
                ceremonyDone: mCeremony,
                remark: mRemark,
            }
            : r));
        setModalOpen(false);
        message.success('班级开班信息已保存');
    }, [editingId, mOpenDate, mCeremonyTime, mVenue, mHeadTeacher, mStatus, mMaterials, mCommitment, mCeremony, mRemark]);
    const allMaterialsReady = useCallback((r) => {
        return MATERIAL_KEYS.every((k) => r.materials[k.key]);
    }, []);
    const handleGenMaterials = () => {
        if (!records.length) {
            message.warning('暂无班级');
            return;
        }
        setRecords((prev) => prev.map((r) => {
            const m = { ...r.materials };
            MATERIAL_KEYS.forEach((k) => { m[k.key] = true; });
            return { ...r, materials: m, status: r.status === '待申请' ? '材料准备中' : r.status };
        }));
        message.success('已为本项目全部班级勾选开班材料清单');
    };
    const submitToRsb = (targets, single) => {
        const ready = targets.filter((r) => allMaterialsReady(r));
        const notReady = targets.filter((r) => !allMaterialsReady(r));
        if (!ready.length) {
            message.warning('请先完善开班材料清单（申请表、花名册、承诺书、课表、场地确认单）');
            return;
        }
        setRecords((prev) => prev.map((r) => {
            if (!ready.find((x) => x.classId === r.classId))
                return r;
            return { ...r, status: '已提交人社', rsbSubmittedAt: new Date().toISOString().slice(0, 10) };
        }));
        message.success(single ? `班级「${ready[0].className}」开班材料已提交人社系统` : `已向人社系统提交 ${ready.length} 个班级的开班材料`);
    };
    const handleNotifyEnterprise = () => {
        if (!currentProject)
            return;
        setRecords((prev) => prev.map((r) => ({ ...r, enterpriseNotified: true })));
        message.success(`已向企业「${currentProject.enterprise}」发送开班时间与场地确认通知`);
    };
    const columns = [
        { title: '班级', dataIndex: 'className', render: (v) => _jsx("strong", { children: v }) },
        { title: '学员数', dataIndex: 'studentCount', render: (v) => `${v} 人` },
        { title: '开班日期', dataIndex: 'openDate', render: (v) => v || '—' },
        { title: '场地', dataIndex: 'venue', render: (v) => v || '—' },
        { title: '仪式时间', dataIndex: 'ceremonyTime', render: (v) => v || '—' },
        { title: '材料', key: 'materials', render: (_, r) => materialsProgress(r.materials || {}) },
        { title: '状态', dataIndex: 'status', render: (v) => _jsx("span", { className: `status-badge ${statusClass(v)}`, children: v }) },
        {
            title: '操作', key: 'actions',
            render: (_, r) => (_jsxs("div", { className: "action-btns", onClick: (e) => e.stopPropagation(), children: [_jsx("button", { className: "action-btn", title: "\u7F16\u8F91", onClick: () => openModal(r.classId), children: _jsx(EditOutlined, {}) }), _jsx("button", { className: "action-btn", title: "\u63D0\u4EA4\u4EBA\u793E", onClick: () => submitToRsb([r], true), children: _jsx(CloudUploadOutlined, {}) })] })),
        },
    ];
    return (_jsxs("div", { className: "kb-page", children: [_jsxs("header", { className: "kb-topbar", children: [_jsxs("div", { className: "kb-topbar-left", children: [_jsxs("a", { className: "kb-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "kb-title", children: "\u5F00\u73ED\u7533\u62A5" }), _jsx("span", { className: "kb-sub", children: currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 4 / 9` : '按项目办理开班申报 · 步骤 4 / 9' })] })] }), _jsxs("span", { className: "kb-process-badge", children: [_jsx(CheckCircleOutlined, {}), " \u2026 \u2192 \u5F00\u73ED\u7533\u62A5 \u2192 \u57F9\u8BAD\u5B9E\u65BD \u2192 \u2026"] })] }), _jsxs("main", { className: "kb-page-body", children: [_jsxs("div", { className: "kb-card", children: [_jsxs("div", { className: "kb-card-head", children: [_jsxs("div", { className: "kb-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "kb-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE\uFF08\u5F00\u73ED\u9488\u5BF9\u8BE5\u9879\u76EE\u5DF2\u6392\u8BFE\u73ED\u7EA7\uFF09" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && (_jsxs("div", { className: "kb-project-banner", children: [_jsxs("div", { className: "kb-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "kb-banner-item", children: [_jsx("label", { children: "\u57F9\u8BAD\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "kb-banner-item", children: [_jsx("label", { children: "\u5DE5\u79CD" }), _jsx("strong", { children: currentProject.occupation || '—' })] }), _jsxs("div", { className: "kb-banner-item", children: [_jsx("label", { children: "\u73ED\u7EA7\u6570" }), _jsxs("strong", { children: [records.length, " \u4E2A"] })] }), _jsxs("div", { className: "kb-banner-item", children: [_jsx("label", { children: "\u8D1F\u8D23\u4EBA" }), _jsx("strong", { children: currentProject.owner || '—' })] })] }))] }), !currentProject ? (_jsxs("div", { className: "kb-empty", children: [_jsx(InfoCircleOutlined, { style: { fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u518D\u529E\u7406\u5404\u73ED\u7EA7\u5F00\u73ED\u7533\u8BF7\u4E0E\u4EEA\u5F0F\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u9700\u5148\u5B8C\u6210 \u4EBA\u624D\u8BC6\u522B\u3001\u6700\u7EC8\u65B9\u6848\uFF1B\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u8FDB\u5165\u3002" })] })) : records.length === 0 ? (_jsxs("div", { className: "kb-alert warn", children: [_jsx(ExclamationCircleOutlined, {}), _jsx("div", { children: "\u672C\u9879\u76EE\u5C1A\u65E0\u73ED\u7EA7\uFF0C\u8BF7\u5148 \u4EBA\u624D\u8BC6\u522B \u5E76\u5B8C\u6210 \u6700\u7EC8\u65B9\u6848\u3002" })] })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "kb-stat-row", children: [_jsxs("div", { className: "kb-stat-card", children: [_jsx("div", { className: "kb-stat-val", children: stats.total }), _jsx("div", { className: "kb-stat-lbl", children: "\u5F85\u529E\u73ED\u7EA7" })] }), _jsxs("div", { className: "kb-stat-card", children: [_jsx("div", { className: "kb-stat-val warn", children: stats.pending }), _jsx("div", { className: "kb-stat-lbl", children: "\u5BA1\u6279\u4E2D" })] }), _jsxs("div", { className: "kb-stat-card", children: [_jsx("div", { className: "kb-stat-val success", children: stats.opened }), _jsx("div", { className: "kb-stat-lbl", children: "\u5DF2\u6B63\u5F0F\u5F00\u73ED" })] }), _jsxs("div", { className: "kb-stat-card", children: [_jsx("div", { className: "kb-stat-val", children: stats.ceremony }), _jsx("div", { className: "kb-stat-lbl", children: "\u4EEA\u5F0F\u5DF2\u5B8C\u6210" })] })] }), _jsxs("div", { className: "kb-card", children: [_jsxs("div", { className: "kb-card-head", children: [_jsxs("div", { className: "kb-card-head-title", children: [_jsx(NodeIndexOutlined, {}), " \u5F00\u73ED\u6D41\u7A0B"] }), _jsx("span", { className: "kb-hint", children: "\u5F00\u73ED\u7533\u8BF7 \u2192 \u627F\u8BFA\u4E66 \u2192 \u4EBA\u793E\u63D0\u4EA4 \u2192 \u5BA1\u6279\u901A\u8FC7 \u2192 \u6B63\u5F0F\u5F00\u73ED" })] }), _jsx("div", { className: "kb-flow-steps", children: FLOW_LABELS.map((label, i) => {
                                            const Icon = FLOW_ICONS[i];
                                            let cls = 'kb-flow-step';
                                            if (i < flowAvg)
                                                cls += ' done';
                                            else if (i === flowAvg)
                                                cls += ' active';
                                            return (_jsxs("div", { className: cls, children: [_jsx(Icon, { style: { display: 'block', fontSize: 18, marginBottom: 6 } }), label] }, i));
                                        }) }), _jsxs("div", { className: "kb-toolbar", children: [_jsx(Button, { type: "primary", icon: _jsx(FileZipOutlined, {}), onClick: handleGenMaterials, children: "\u6279\u91CF\u751F\u6210\u5F00\u73ED\u6750\u6599\u5305" }), _jsx(Button, { icon: _jsx(CloudUploadOutlined, {}), onClick: () => submitToRsb(records, false), children: "\u63D0\u4EA4\u4EBA\u793E\u7CFB\u7EDF" }), _jsx(Button, { icon: _jsx(BellOutlined, {}), onClick: handleNotifyEnterprise, children: "\u901A\u77E5\u4F01\u4E1A\u786E\u8BA4" })] })] }), _jsxs("div", { className: "kb-card", children: [_jsx("div", { className: "kb-card-head", children: _jsxs("div", { className: "kb-card-head-title", children: [_jsx(CalendarOutlined, {}), " \u73ED\u7EA7\u5F00\u73ED\u53F0\u8D26"] }) }), _jsx(Table, { dataSource: records, columns: columns, rowKey: "classId", pagination: false, locale: { emptyText: '暂无班级，请先分班排课' } })] }), _jsxs("div", { className: "kb-footer-actions", children: [_jsx(Button, { icon: _jsx(CalendarOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-final-plan'); }, children: "\u8FD4\u56DE\u6700\u7EC8\u65B9\u6848" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { type: "primary", icon: _jsx(ArrowRightOutlined, {}), style: { background: '#10B981', borderColor: '#10B981' }, onClick: () => {
                                            if (!currentProject) {
                                                message.warning('请先选择项目');
                                                return;
                                            }
                                            message.success(`项目「${currentProject.name}」开班信息已保存，进入培训环节`);
                                            setCurrentProjectId(currentProject.id);
                                            navigateTo('zhixun-training-implementation');
                                        }, children: "\u5B8C\u6210\u5F00\u73ED\u7533\u62A5\uFF0C\u8FDB\u5165\u57F9\u8BAD\u5B9E\u65BD" })] }), _jsx(Modal, { open: modalOpen, onCancel: () => setModalOpen(false), title: "\u73ED\u7EA7\u5F00\u73ED\u4FE1\u606F", footer: _jsxs("div", { style: { display: 'flex', gap: 10, justifyContent: 'flex-end' }, children: [_jsx(Button, { onClick: () => setModalOpen(false), children: "\u53D6\u6D88" }), _jsx(Button, { type: "primary", onClick: saveModal, children: "\u4FDD\u5B58" })] }), width: 560, children: _jsxs("div", { className: "kb-modal-form", children: [_jsxs("div", { className: "kb-form-group-full", children: [_jsx("label", { children: "\u73ED\u7EA7\u540D\u79F0" }), _jsx(Input, { disabled: true, value: records.find((r) => r.classId === editingId)?.className || '' })] }), _jsxs("div", { className: "kb-form-grid", children: [_jsxs("div", { className: "kb-form-group", children: [_jsx("label", { children: "\u8BA1\u5212\u5F00\u73ED\u65E5\u671F" }), _jsx(Input, { type: "date", value: mOpenDate, onChange: (e) => setMOpenDate(e.target.value) })] }), _jsxs("div", { className: "kb-form-group", children: [_jsx("label", { children: "\u5F00\u73ED\u4EEA\u5F0F\u65F6\u95F4" }), _jsx(Input, { type: "time", value: mCeremonyTime, onChange: (e) => setMCeremonyTime(e.target.value) })] }), _jsxs("div", { className: "kb-form-group", children: [_jsx("label", { children: "\u57F9\u8BAD\u573A\u5730" }), _jsx(Select, { value: mVenue, onChange: setMVenue, style: { width: '100%' }, options: VENUES.map((v) => ({ value: v, label: v })) })] }), _jsxs("div", { className: "kb-form-group", children: [_jsx("label", { children: "\u73ED\u4E3B\u4EFB" }), _jsx(Input, { placeholder: "\u59D3\u540D", value: mHeadTeacher, onChange: (e) => setMHeadTeacher(e.target.value) })] }), _jsxs("div", { className: "kb-form-group", children: [_jsx("label", { children: "\u5F00\u73ED\u72B6\u6001" }), _jsx(Select, { value: mStatus, onChange: setMStatus, style: { width: '100%' }, options: ['待申请', '材料准备中', '已提交人社', '审批中', '已批准', '正式开班'].map((s) => ({ value: s, label: s })) })] })] }), _jsxs("div", { className: "kb-form-group-full", style: { marginTop: 16 }, children: [_jsx("label", { children: "\u5F00\u73ED\u6750\u6599\u6E05\u5355" }), _jsx("div", { className: "kb-material-grid", children: MATERIAL_KEYS.map((mk) => (_jsxs("label", { className: `kb-material-item${mMaterials[mk.key] ? ' done' : ''}`, children: [_jsx("input", { type: "checkbox", checked: mMaterials[mk.key] || false, onChange: (e) => setMMaterials((prev) => ({ ...prev, [mk.key]: e.target.checked })) }), mk.label] }, mk.key))) })] }), _jsxs("div", { className: "kb-check-row", children: [_jsx("input", { type: "checkbox", checked: mCommitment, onChange: (e) => setMCommitment(e.target.checked) }), _jsx("label", { children: "\u4F01\u4E1A/\u673A\u6784\u5DF2\u7B7E\u7F72\u5F00\u73ED\u627F\u8BFA\u4E66" })] }), _jsxs("div", { className: "kb-check-row", children: [_jsx("input", { type: "checkbox", checked: mCeremony, onChange: (e) => setMCeremony(e.target.checked) }), _jsx("label", { children: "\u5F00\u73ED\u4EEA\u5F0F\u5DF2\u5B8C\u6210\uFF085\u201310 \u5206\u949F\uFF09" })] }), _jsxs("div", { className: "kb-form-group-full", style: { marginTop: 16 }, children: [_jsx("label", { children: "\u5907\u6CE8" }), _jsx(Input.TextArea, { placeholder: "\u573A\u5730\u786E\u8BA4\u3001\u4F01\u4E1A\u53C2\u4E0E\u60C5\u51B5\u7B49", value: mRemark, onChange: (e) => setMRemark(e.target.value), rows: 3 })] })] }) })] }))] })] }));
}

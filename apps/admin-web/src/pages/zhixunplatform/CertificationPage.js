import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, Input, Button, Table, Modal, message } from 'antd';
import { ArrowLeftOutlined, FolderOpenOutlined, UnorderedListOutlined, SafetyCertificateOutlined, CheckCircleOutlined, EditOutlined, FileExcelOutlined, CloudUploadOutlined, TrophyOutlined, NodeIndexOutlined, InfoCircleOutlined, ExclamationCircleOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './CertificationPage.css';
// ============================================================
// 常量
// ============================================================
const DEFAULT_TASKS = [
    { id: 1, name: '2026年度安全培训计划编制', owner: '张明', enterprise: '广西建工集团', occupation: '安全员', skillLevel: '高级', planCount: 120 },
    { id: 2, name: '电工高级班认定材料整理', owner: '李芳', enterprise: '南宁供电局', occupation: '电工', skillLevel: '高级', planCount: 42 },
    { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', enterprise: '柳州钢铁有限公司', occupation: '焊工', skillLevel: '中级', planCount: 36 },
    { id: 4, name: '焊工中级班排课协调', owner: '赵丽', enterprise: '桂林旅游股份公司', occupation: '焊工', skillLevel: '中级', planCount: 28 },
    { id: 5, name: '培训档案年度归档', owner: '陈伟', enterprise: '梧州港务集团', occupation: '起重工', skillLevel: '初级', planCount: 15 },
];
const FLOW_LABELS = ['条件审核', '理论/实操', '获证花名册', '人社提交', '证书发放'];
const FLOW_ICONS = [CheckCircleOutlined, EditOutlined, FileExcelOutlined, CloudUploadOutlined, TrophyOutlined];
const PASS_SCORE = 60;
function computeResult(s) {
    if (s.conditionOk === false)
        return '未通过';
    if (s.theoryScore == null || s.practicalScore == null)
        return '待考';
    if (s.theoryScore >= PASS_SCORE && s.practicalScore >= PASS_SCORE)
        return '通过';
    return '未通过';
}
// ============================================================
// Mock 数据
// ============================================================
const MOCK_STUDENTS = [
    { studentId: 1, name: '张建国' },
    { studentId: 2, name: '李秀英' },
    { studentId: 3, name: '王志强' },
    { studentId: 4, name: '陈丽华' },
    { studentId: 5, name: '刘明辉' },
    { studentId: 6, name: '赵红梅' },
    { studentId: 7, name: '周建华' },
    { studentId: 8, name: '吴桂芳' },
    { studentId: 9, name: '黄伟民' },
    { studentId: 10, name: '孙晓燕' },
    { studentId: 11, name: '钱国栋' },
    { studentId: 12, name: '杨玉兰' },
];
function buildMockCandidates(projectId) {
    const task = DEFAULT_TASKS.find((t) => t.id === projectId);
    if (!task)
        return [];
    const cls1 = `${task.occupation || '电工'}高级工1班`;
    const cls2 = `${task.occupation || '电工'}中级工1班`;
    const list = [];
    MOCK_STUDENTS.slice(0, 8).forEach((s, i) => {
        const theory = i < 4 ? 55 + Math.floor(Math.random() * 45) : null;
        const practical = i < 4 ? 55 + Math.floor(Math.random() * 45) : null;
        const cond = i < 6 ? true : null;
        list.push({
            studentId: s.studentId, name: s.name,
            classId: i < 5 ? 1 : 2, className: i < 5 ? cls1 : cls2,
            occupation: task.occupation || '—',
            conditionOk: cond, theoryScore: theory, practicalScore: practical,
            result: '待考', rsbStatus: '未提交', certNo: '', certStatus: '未发证',
        });
    });
    list.forEach((c) => { c.result = computeResult(c); });
    return list;
}
// ============================================================
// 主页面
// ============================================================
export function CertificationPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [candidates, setCandidates] = useState([]);
    const [filterClass, setFilterClass] = useState('');
    const [filterResult, setFilterResult] = useState('');
    const [examModalOpen, setExamModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [theoryScore, setTheoryScore] = useState('');
    const [practicalScore, setPracticalScore] = useState('');
    const [conditionVal, setConditionVal] = useState('');
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    useEffect(() => {
        if (selectedProjectId) {
            setCandidates(buildMockCandidates(+selectedProjectId));
        }
        else {
            setCandidates([]);
        }
    }, [selectedProjectId]);
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
        setFilterClass('');
        setFilterResult('');
    };
    const classNames = useMemo(() => [...new Set(candidates.map((c) => c.className))], [candidates]);
    const filteredList = useMemo(() => {
        return candidates.filter((s) => {
            if (filterClass && s.className !== filterClass)
                return false;
            if (filterResult && s.result !== filterResult)
                return false;
            return true;
        });
    }, [candidates, filterClass, filterResult]);
    const stats = useMemo(() => {
        const total = candidates.length;
        const pending = candidates.filter((s) => s.result === '待考').length;
        const passed = candidates.filter((s) => s.result === '通过').length;
        const rate = total ? Math.round((passed / total) * 1000) / 10 : 0;
        const certified = candidates.filter((s) => s.certStatus === '已发证').length;
        return { total, pending, passRate: rate, certified };
    }, [candidates]);
    const flowStep = useMemo(() => {
        if (!candidates.length)
            return 0;
        const cond = candidates.filter((s) => s.conditionOk !== null).length / candidates.length;
        const exam = candidates.filter((s) => s.result !== '待考').length / candidates.length;
        const pass = candidates.filter((s) => s.result === '通过').length / candidates.length;
        const rsb = candidates.filter((s) => s.rsbStatus !== '未提交').length / candidates.length;
        const cert = candidates.filter((s) => s.certStatus === '已发证').length / candidates.length;
        if (cert > 0.5)
            return 5;
        if (rsb > 0.3)
            return 4;
        if (pass > 0.3)
            return 3;
        if (exam > 0.5)
            return 2;
        if (cond > 0.5)
            return 1;
        return 0;
    }, [candidates]);
    const updateCandidates = useCallback((updater) => {
        setCandidates((prev) => {
            const next = updater(prev.map((c) => ({ ...c })));
            next.forEach((c) => { c.result = computeResult(c); });
            return next;
        });
    }, []);
    const handleCheckCondition = () => {
        if (!candidates.length) {
            message.warning('暂无学员');
            return;
        }
        updateCandidates((prev) => prev.map((s) => ({ ...s, conditionOk: s.conditionOk === null ? Math.random() > 0.08 : s.conditionOk })));
        message.success('已完成批量认定条件审核');
    };
    const handleBatchExam = () => {
        const eligible = candidates.filter((s) => s.conditionOk !== false);
        if (!eligible.length) {
            message.warning('请先完成条件审核');
            return;
        }
        updateCandidates((prev) => prev.map((s) => {
            if (s.result !== '待考' || s.conditionOk === false)
                return s;
            return { ...s, theoryScore: 55 + Math.floor(Math.random() * 45), practicalScore: 55 + Math.floor(Math.random() * 45) };
        }));
        message.success(`已为 ${eligible.length} 名符合条件学员录入考核成绩`);
    };
    const handleGenRoster = () => {
        const passed = candidates.filter((s) => s.result === '通过');
        if (!passed.length) {
            message.warning('暂无通过学员，无法生成花名册');
            return;
        }
        message.success(`已生成获证花名册：${passed.length} 人`);
    };
    const handleSubmitRsb = () => {
        const passed = candidates.filter((s) => s.result === '通过' && s.rsbStatus === '未提交');
        if (!passed.length) {
            message.warning('暂无待提交人社的通过学员');
            return;
        }
        updateCandidates((prev) => prev.map((s) => {
            if (s.result === '通过' && s.rsbStatus === '未提交')
                return { ...s, rsbStatus: '审批中' };
            return s;
        }));
        message.success(`已向人社系统提交 ${passed.length} 人认定材料`);
    };
    const handleIssueCert = () => {
        const ready = candidates.filter((s) => s.result === '通过' && (s.rsbStatus === '审批中' || s.rsbStatus === '已通过'));
        if (!ready.length) {
            message.warning('暂无待发证学员');
            return;
        }
        updateCandidates((prev) => prev.map((s, i) => {
            if (s.result === '通过' && (s.rsbStatus === '审批中' || s.rsbStatus === '已通过')) {
                return { ...s, rsbStatus: '已通过', certStatus: '已发证', certNo: 'GX' + (selectedProjectId || '0') + String(s.studentId).slice(-4) + (1000 + i) };
            }
            return s;
        }));
        message.success(`已为 ${ready.length} 名学员登记证书发放`);
    };
    const openExamModal = (studentId) => {
        const s = candidates.find((x) => x.studentId === studentId);
        if (!s)
            return;
        setEditingId(studentId);
        setTheoryScore(s.theoryScore != null ? String(s.theoryScore) : '');
        setPracticalScore(s.practicalScore != null ? String(s.practicalScore) : '');
        setConditionVal(s.conditionOk === true ? 'true' : s.conditionOk === false ? 'false' : '');
        setExamModalOpen(true);
    };
    const saveExamModal = () => {
        if (editingId == null)
            return;
        updateCandidates((prev) => prev.map((s) => {
            if (s.studentId !== editingId)
                return s;
            return {
                ...s,
                conditionOk: conditionVal === '' ? null : conditionVal === 'true',
                theoryScore: theoryScore === '' ? null : parseInt(theoryScore, 10),
                practicalScore: practicalScore === '' ? null : parseInt(practicalScore, 10),
            };
        }));
        setExamModalOpen(false);
        message.success('考核成绩已保存');
    };
    const columns = [
        { title: '姓名', dataIndex: 'name', render: (v) => _jsx("strong", { children: v }) },
        { title: '班级', dataIndex: 'className', render: (v) => v || '—' },
        { title: '工种', dataIndex: 'occupation', render: (v) => v || '—' },
        {
            title: '条件', dataIndex: 'conditionOk',
            render: (v) => {
                if (v === true)
                    return _jsx("span", { className: "status-badge st-pass", children: "\u7B26\u5408" });
                if (v === false)
                    return _jsx("span", { className: "status-badge st-fail", children: "\u4E0D\u7B26\u5408" });
                return _jsx("span", { className: "status-badge st-pending", children: "\u5F85\u5BA1\u6838" });
            },
        },
        { title: '理论', dataIndex: 'theoryScore', render: (v) => v != null ? v : '—' },
        { title: '实操', dataIndex: 'practicalScore', render: (v) => v != null ? v : '—' },
        {
            title: '结果', dataIndex: 'result',
            render: (v) => {
                if (v === '通过')
                    return _jsx("span", { className: "status-badge st-pass", children: "\u901A\u8FC7" });
                if (v === '未通过')
                    return _jsx("span", { className: "status-badge st-fail", children: "\u672A\u901A\u8FC7" });
                return _jsx("span", { className: "status-badge st-pending", children: "\u5F85\u8003" });
            },
        },
        {
            title: '人社', dataIndex: 'rsbStatus',
            render: (v) => {
                const map = { '未提交': 'st-pending', '已提交': 'st-rsb', '审批中': 'st-rsb', '已通过': 'st-pass' };
                return _jsx("span", { className: `status-badge ${map[v] || 'st-pending'}`, children: v });
            },
        },
        {
            title: '证书', key: 'cert',
            render: (_, r) => (_jsxs("div", { children: [r.certStatus === '已发证' ? _jsx("span", { className: "status-badge st-cert", children: "\u5DF2\u53D1\u8BC1" }) : _jsx("span", { className: "status-badge st-pending", children: "\u672A\u53D1\u8BC1" }), r.certNo && _jsx("br", {}), r.certNo && _jsx("small", { children: r.certNo })] })),
        },
        {
            title: '操作', key: 'actions',
            render: (_, r) => (_jsx("div", { className: "action-btns", onClick: (e) => e.stopPropagation(), children: _jsx("button", { className: "action-btn", title: "\u5F55\u5165\u6210\u7EE9", onClick: () => openExamModal(r.studentId), children: _jsx(EditOutlined, {}) }) })),
        },
    ];
    return (_jsxs("div", { className: "rd-page", children: [_jsxs("header", { className: "rd-topbar", children: [_jsxs("div", { className: "rd-topbar-left", children: [_jsxs("a", { className: "rd-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "rd-title", children: "\u8BA4\u5B9A\u51C6\u5907" }), _jsx("span", { className: "rd-sub", children: currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 6 / 9` : '按项目组织认定准备与获证 · 步骤 6 / 9' })] })] }), _jsxs("span", { className: "rd-process-badge", children: [_jsx(SafetyCertificateOutlined, {}), " \u2026 \u2192 \u8BA4\u5B9A\u51C6\u5907 \u2192 \u7533\u62A5\u670D\u52A1 \u2192 \u2026"] })] }), _jsxs("main", { className: "rd-page-body", children: [_jsxs("div", { className: "rd-card", children: [_jsxs("div", { className: "rd-card-head", children: [_jsxs("div", { className: "rd-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "rd-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE\uFF08\u8BA4\u5B9A\u9488\u5BF9\u8BE5\u9879\u76EE\u5DF2\u5206\u73ED\u5B66\u5458\uFF09" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && (_jsxs("div", { className: "rd-project-banner", children: [_jsxs("div", { className: "rd-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "rd-banner-item", children: [_jsx("label", { children: "\u57F9\u8BAD\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "rd-banner-item", children: [_jsx("label", { children: "\u5DE5\u79CD / \u7B49\u7EA7" }), _jsxs("strong", { children: [currentProject.occupation || '—', currentProject.skillLevel ? ' · ' + currentProject.skillLevel : ''] })] }), _jsxs("div", { className: "rd-banner-item", children: [_jsx("label", { children: "\u53C2\u8003\u4EBA\u6570" }), _jsxs("strong", { children: [candidates.length, " \u4EBA"] })] }), _jsxs("div", { className: "rd-banner-item", children: [_jsx("label", { children: "\u5DF2\u901A\u8FC7" }), _jsxs("strong", { children: [candidates.filter((s) => s.result === '通过').length, " \u4EBA"] })] })] }))] }), !currentProject ? (_jsxs("div", { className: "rd-empty", children: [_jsx(InfoCircleOutlined, { style: { fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u518D\u529E\u7406\u6280\u80FD\u7B49\u7EA7\u8BA4\u5B9A\u4E0E\u83B7\u8BC1\u82B1\u540D\u518C\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u9700\u5148\u5B8C\u6210 \u4EBA\u624D\u8BC6\u522B\u3001\u57F9\u8BAD\uFF1B\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u8FDB\u5165\u3002" })] })) : candidates.length === 0 ? (_jsxs("div", { className: "rd-alert warn", children: [_jsx(ExclamationCircleOutlined, {}), _jsx("div", { children: "\u672C\u9879\u76EE\u5C1A\u65E0\u5DF2\u5206\u73ED\u5B66\u5458\uFF0C\u8BF7\u5148 \u5206\u73ED \u5E76\u5B8C\u6210\u57F9\u8BAD\u3002" })] })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "rd-stat-row", children: [_jsxs("div", { className: "rd-stat-card", children: [_jsx("div", { className: "rd-stat-val", children: stats.total }), _jsx("div", { className: "rd-stat-lbl", children: "\u53C2\u8003\u4EBA\u6570" })] }), _jsxs("div", { className: "rd-stat-card", children: [_jsx("div", { className: "rd-stat-val warn", children: stats.pending }), _jsx("div", { className: "rd-stat-lbl", children: "\u5F85\u8BA4\u5B9A" })] }), _jsxs("div", { className: "rd-stat-card", children: [_jsxs("div", { className: "rd-stat-val success", children: [stats.passRate, "%"] }), _jsx("div", { className: "rd-stat-lbl", children: "\u901A\u8FC7\u7387" })] }), _jsxs("div", { className: "rd-stat-card", children: [_jsx("div", { className: "rd-stat-val", children: stats.certified }), _jsx("div", { className: "rd-stat-lbl", children: "\u5DF2\u83B7\u8BC1" })] })] }), _jsxs("div", { className: "rd-card", children: [_jsxs("div", { className: "rd-card-head", children: [_jsxs("div", { className: "rd-card-head-title", children: [_jsx(NodeIndexOutlined, {}), " \u8BA4\u5B9A\u6D41\u7A0B"] }), _jsx("span", { className: "rd-hint", children: "\u6761\u4EF6\u5BA1\u6838 \u2192 \u7406\u8BBA/\u5B9E\u64CD\u8003\u6838 \u2192 \u82B1\u540D\u518C \u2192 \u4EBA\u793E\u63D0\u4EA4 \u2192 \u8BC1\u4E66\u53D1\u653E" })] }), _jsx("div", { className: "rd-flow-steps", children: FLOW_LABELS.map((label, i) => {
                                            const Icon = FLOW_ICONS[i];
                                            let cls = 'rd-flow-step';
                                            if (i < flowStep)
                                                cls += ' done';
                                            else if (i === flowStep)
                                                cls += ' active';
                                            return (_jsxs("div", { className: cls, children: [_jsx(Icon, { style: { display: 'block', fontSize: 18, marginBottom: 6 } }), label] }, i));
                                        }) }), _jsxs("div", { className: "rd-toolbar", children: [_jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), onClick: handleCheckCondition, children: "\u6279\u91CF\u6761\u4EF6\u5BA1\u6838" }), _jsx(Button, { icon: _jsx(EditOutlined, {}), onClick: handleBatchExam, children: "\u6279\u91CF\u5F55\u5165\u8003\u6838\u6210\u7EE9" }), _jsx(Button, { icon: _jsx(FileExcelOutlined, {}), onClick: handleGenRoster, children: "\u751F\u6210\u83B7\u8BC1\u82B1\u540D\u518C" }), _jsx(Button, { icon: _jsx(CloudUploadOutlined, {}), onClick: handleSubmitRsb, children: "\u63D0\u4EA4\u4EBA\u793E\u7CFB\u7EDF" }), _jsx(Button, { style: { background: '#10B981', color: '#fff', borderColor: '#10B981' }, icon: _jsx(TrophyOutlined, {}), onClick: handleIssueCert, children: "\u8BC1\u4E66\u53D1\u653E\u767B\u8BB0" })] }), _jsxs("div", { className: "rd-filter-bar", children: [_jsxs("div", { className: "rd-form-group", style: { minWidth: 160, margin: 0 }, children: [_jsx("label", { children: "\u73ED\u7EA7" }), _jsx(Select, { placeholder: "\u5168\u90E8", value: filterClass || undefined, onChange: (v) => setFilterClass(v || ''), allowClear: true, style: { width: '100%' }, options: classNames.map((n) => ({ value: n, label: n })) })] }), _jsxs("div", { className: "rd-form-group", style: { minWidth: 160, margin: 0 }, children: [_jsx("label", { children: "\u8003\u6838\u7ED3\u679C" }), _jsx(Select, { placeholder: "\u5168\u90E8", value: filterResult || undefined, onChange: (v) => setFilterResult(v || ''), allowClear: true, style: { width: '100%' }, options: ['待考', '通过', '未通过'].map((v) => ({ value: v, label: v })) })] })] }), _jsx(Table, { dataSource: filteredList, columns: columns, rowKey: "studentId", pagination: false, locale: { emptyText: '暂无学员，请先分班' } })] }), _jsxs("div", { className: "rd-footer-actions", children: [_jsx(Button, { icon: _jsx(SafetyCertificateOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-training-implementation'); }, children: "\u8FD4\u56DE\u57F9\u8BAD" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { type: "primary", style: { background: '#10B981', borderColor: '#10B981' }, icon: _jsx(ArrowLeftOutlined, { style: { transform: 'rotate(180deg)' } }), onClick: () => {
                                            if (!currentProject) {
                                                message.warning('请先选择项目');
                                                return;
                                            }
                                            message.success(`项目「${currentProject.name}」认定信息已保存，进入申报环节`);
                                            setCurrentProjectId(currentProject.id);
                                            navigateTo('zhixun-declaration-service');
                                        }, children: "\u5B8C\u6210\u8BA4\u5B9A\u51C6\u5907\uFF0C\u8FDB\u5165\u7533\u62A5\u670D\u52A1" })] }), _jsxs(Modal, { open: examModalOpen, onCancel: () => setExamModalOpen(false), title: "\u5F55\u5165\u8003\u6838\u6210\u7EE9", footer: _jsxs("div", { style: { display: 'flex', gap: 10, justifyContent: 'flex-end' }, children: [_jsx(Button, { onClick: () => setExamModalOpen(false), children: "\u53D6\u6D88" }), _jsx(Button, { type: "primary", onClick: saveExamModal, children: "\u4FDD\u5B58" })] }), children: [_jsxs("p", { style: { marginBottom: 16, fontWeight: 600 }, children: [candidates.find((x) => x.studentId === editingId)?.name, " \u00B7 ", candidates.find((x) => x.studentId === editingId)?.className] }), _jsxs("div", { className: "rd-form-grid", children: [_jsxs("div", { className: "rd-form-group", children: [_jsx("label", { children: "\u7406\u8BBA\u6210\u7EE9" }), _jsx(Input, { type: "number", min: 0, max: 100, placeholder: "0-100", value: theoryScore, onChange: (e) => setTheoryScore(e.target.value) })] }), _jsxs("div", { className: "rd-form-group", children: [_jsx("label", { children: "\u5B9E\u64CD\u6210\u7EE9" }), _jsx(Input, { type: "number", min: 0, max: 100, placeholder: "0-100", value: practicalScore, onChange: (e) => setPracticalScore(e.target.value) })] }), _jsxs("div", { className: "rd-form-group", style: { gridColumn: '1/-1' }, children: [_jsx("label", { children: "\u8BA4\u5B9A\u6761\u4EF6\u7B26\u5408" }), _jsx(Select, { value: conditionVal || undefined, onChange: (v) => setConditionVal(v || ''), style: { width: '100%' }, options: [{ value: 'true', label: '符合' }, { value: 'false', label: '不符合' }, { value: '', label: '待审核' }] })] })] })] })] }))] })] }));
}

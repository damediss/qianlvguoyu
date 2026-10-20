import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, Button, Table, InputNumber, message } from 'antd';
import { ArrowLeftOutlined, FolderOpenOutlined, UnorderedListOutlined, FileAddOutlined, FileZipOutlined, CheckCircleOutlined, CloudUploadOutlined, DollarOutlined, SafetyCertificateOutlined, NodeIndexOutlined, InfoCircleOutlined, ExclamationCircleOutlined, FileTextOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './DeclarationServicePage.css';
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
const FLOW_LABELS = ['政策模板', '材料包', '人社提交', '拨款跟踪'];
const FLOW_ICONS = [FileTextOutlined, FolderOpenOutlined, CloudUploadOutlined, DollarOutlined];
const POLICY_OPTIONS = ['企业职工岗位技能培训补贴', '职业技能提升行动补贴', '鉴定考核补贴'];
const DEFAULT_MATERIALS = [
    { key: 'plan', name: '培训计划备案表', stage: '项目立项', status: '草稿', submitDate: '', ready: false },
    { key: 'roster', name: '学员名册及考勤', stage: '培训执行', status: '草稿', submitDate: '', ready: false },
    { key: 'cert', name: '获证花名册', stage: '认定', status: '草稿', submitDate: '', ready: false },
    { key: 'subsidy', name: '补贴申报材料包', stage: '补贴申报', status: '草稿', submitDate: '', ready: false },
    { key: 'contract', name: '培训协议及承诺书', stage: '协议签订', status: '草稿', submitDate: '', ready: false },
    { key: 'evidence', name: '过程留痕证据链目录', stage: '培训执行', status: '草稿', submitDate: '', ready: false },
    { key: 'graduate', name: '结业证明申请', stage: '结业', status: '草稿', submitDate: '', ready: false },
];
function todayStr() {
    return new Date().toISOString().slice(0, 10);
}
function statusClass(st) {
    if (st === '已提交' || st === '已通过')
        return 'st-pass';
    if (st === '审核中' || st === '已齐')
        return 'st-submit';
    if (st === '待补充' || st === '已退回')
        return 'st-reject';
    if (st === '草稿')
        return 'st-draft';
    return 'st-ready';
}
// ============================================================
// Mock 数据
// ============================================================
function getMockCertifiedCount(projectId) {
    return 8;
}
function initBundle(projectId) {
    const certCount = getMockCertifiedCount(projectId);
    const materials = DEFAULT_MATERIALS.map((m) => ({ ...m }));
    if (certCount > 0) {
        const cert = materials.find((m) => m.key === 'cert');
        if (cert) {
            cert.ready = true;
            cert.status = '已齐';
        }
    }
    return {
        projectId,
        policyTemplate: POLICY_OPTIONS[0],
        subsidyAmount: (certCount || 40) * 1200,
        materials,
        submitStatus: '草稿',
        submitDate: '',
        refundStatus: '未拨款',
        rejectReason: '',
    };
}
function flowStep(bundle) {
    if (bundle.submitStatus === '已通过' || bundle.refundStatus === '已拨款')
        return 4;
    if (bundle.submitStatus === '审核中' || bundle.submitStatus === '已提交')
        return 3;
    const ready = bundle.materials.filter((m) => m.ready).length;
    if (ready === bundle.materials.length)
        return 2;
    if (bundle.policyTemplate)
        return 1;
    return 0;
}
// ============================================================
// 主页面
// ============================================================
export function DeclarationServicePage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [bundle, setBundle] = useState(null);
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    const certCount = selectedProjectId ? getMockCertifiedCount(+selectedProjectId) : 0;
    useEffect(() => {
        if (selectedProjectId) {
            setBundle(initBundle(+selectedProjectId));
        }
        else {
            setBundle(null);
        }
    }, [selectedProjectId]);
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
    };
    const updateBundle = useCallback((updater) => {
        setBundle((prev) => {
            if (!prev)
                return prev;
            const next = { ...prev, materials: prev.materials.map((m) => ({ ...m })) };
            updater(next);
            return next;
        });
    }, []);
    const stats = useMemo(() => {
        if (!bundle)
            return { ready: 0, total: 0, submitStatus: '草稿', certCount: 0, refund: '—' };
        const ready = bundle.materials.filter((m) => m.ready).length;
        return { ready, total: bundle.materials.length, submitStatus: bundle.submitStatus, certCount, refund: bundle.refundStatus };
    }, [bundle, certCount]);
    const step = bundle ? flowStep(bundle) : 0;
    const handleGenPackage = () => {
        updateBundle((b) => {
            b.materials.forEach((m) => { m.ready = true; m.status = m.status === '草稿' ? '已齐' : m.status; });
        });
        message.success('已根据政策模板生成本项目申报材料包');
    };
    const handleVerify = () => {
        if (!bundle)
            return;
        const notReady = bundle.materials.filter((m) => !m.ready);
        if (notReady.length) {
            message.warning(`尚有 ${notReady.length} 项材料未齐备：${notReady.map((m) => m.name).join('、')}`);
            return;
        }
        message.success('补贴申报材料核验清单已全部通过');
    };
    const handleSubmitRsb = () => {
        if (!bundle)
            return;
        const notReady = bundle.materials.filter((m) => !m.ready);
        if (notReady.length) {
            message.warning('请先齐备全部材料后再提交');
            return;
        }
        const today = todayStr();
        updateBundle((b) => {
            b.submitStatus = '审核中';
            b.submitDate = today;
            b.materials.forEach((m) => { if (m.ready) {
                m.status = '已提交';
                m.submitDate = today;
            } });
        });
        message.success('培训及补贴材料已提交人社系统，进入审核中');
    };
    const handleTrackRefund = () => {
        if (!bundle)
            return;
        if (bundle.submitStatus !== '审核中' && bundle.submitStatus !== '已通过') {
            message.warning('请先完成人社材料提交');
            return;
        }
        if (bundle.submitStatus === '审核中') {
            updateBundle((b) => { b.submitStatus = '已通过'; b.refundStatus = '拨款中'; });
        }
        else if (bundle.refundStatus === '拨款中') {
            updateBundle((b) => { b.refundStatus = '已拨款'; });
            message.success('拨款已到账');
        }
        else {
            message.info('当前拨款状态：' + bundle.refundStatus);
        }
    };
    const handleToggleReady = (key) => {
        updateBundle((b) => {
            const m = b.materials.find((x) => x.key === key);
            if (m) {
                m.ready = !m.ready;
                m.status = m.ready ? '已齐' : '草稿';
            }
        });
    };
    const materialColumns = [
        { title: '材料名称', dataIndex: 'name', render: (v) => _jsx("strong", { children: v }) },
        { title: '所属环节', dataIndex: 'stage' },
        { title: '状态', dataIndex: 'status', render: (v) => _jsx("span", { className: `status-badge ${statusClass(v)}`, children: v }) },
        { title: '提交日期', dataIndex: 'submitDate', render: (v) => v || '—' },
        {
            title: '操作', key: 'actions',
            render: (_, r) => (_jsx(Button, { size: "small", onClick: () => handleToggleReady(r.key), children: r.ready ? '标记草稿' : '标记已齐' })),
        },
    ];
    return (_jsxs("div", { className: "sb-page", children: [_jsxs("header", { className: "sb-topbar", children: [_jsxs("div", { className: "sb-topbar-left", children: [_jsxs("a", { className: "sb-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "sb-title", children: "\u7533\u62A5\u670D\u52A1" }), _jsx("span", { className: "sb-sub", children: currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 7 / 9` : '补贴申报服务 · 步骤 7 / 9' })] })] }), _jsxs("span", { className: "sb-process-badge", children: [_jsx(FileAddOutlined, {}), " \u2026 \u2192 \u7533\u62A5\u670D\u52A1 \u2192 \u57F9\u8BAD\u7ED3\u4E1A \u2192 \u2026"] })] }), _jsxs("main", { className: "sb-page-body", children: [_jsxs("div", { className: "sb-card", children: [_jsxs("div", { className: "sb-card-head", children: [_jsxs("div", { className: "sb-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "sb-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && bundle && (_jsxs("div", { className: "sb-project-banner", children: [_jsxs("div", { className: "sb-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "sb-banner-item", children: [_jsx("label", { children: "\u57F9\u8BAD\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "sb-banner-item", children: [_jsx("label", { children: "\u653F\u7B56\u6A21\u677F" }), _jsx("strong", { children: bundle.policyTemplate || '—' })] }), _jsxs("div", { className: "sb-banner-item", children: [_jsx("label", { children: "\u83B7\u8BC1\u4EBA\u6570" }), _jsxs("strong", { children: [certCount, " \u4EBA"] })] }), _jsxs("div", { className: "sb-banner-item", children: [_jsx("label", { children: "\u7533\u62A5\u72B6\u6001" }), _jsx("strong", { children: bundle.submitStatus })] })] }))] }), !currentProject ? (_jsxs("div", { className: "sb-empty", children: [_jsx(InfoCircleOutlined, { style: { fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u518D\u51C6\u5907\u5E76\u63D0\u4EA4\u57F9\u8BAD/\u8865\u8D34\u7533\u62A5\u6750\u6599\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u5EFA\u8BAE\u5148\u5B8C\u6210 \u8BA4\u5B9A \u83B7\u8BC1\u82B1\u540D\u518C\uFF1B\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u8FDB\u5165\u3002" })] })) : bundle && (_jsxs(_Fragment, { children: [_jsxs("div", { className: "sb-stat-row", children: [_jsxs("div", { className: "sb-stat-card", children: [_jsxs("div", { className: "sb-stat-val", children: [stats.ready, "/", stats.total] }), _jsx("div", { className: "sb-stat-lbl", children: "\u6750\u6599\u9F50\u5907" })] }), _jsxs("div", { className: "sb-stat-card", children: [_jsx("div", { className: `sb-stat-val${stats.submitStatus === '草稿' ? ' warn' : ' success'}`, children: stats.submitStatus }), _jsx("div", { className: "sb-stat-lbl", children: "\u7533\u62A5\u72B6\u6001" })] }), _jsxs("div", { className: "sb-stat-card", children: [_jsx("div", { className: "sb-stat-val success", children: stats.certCount }), _jsx("div", { className: "sb-stat-lbl", children: "\u83B7\u8BC1\u4EBA\u6570" })] }), _jsxs("div", { className: "sb-stat-card", children: [_jsx("div", { className: "sb-stat-val", children: stats.refund }), _jsx("div", { className: "sb-stat-lbl", children: "\u62E8\u6B3E\u72B6\u6001" })] })] }), _jsxs("div", { className: "sb-card", children: [_jsxs("div", { className: "sb-card-head", children: [_jsxs("div", { className: "sb-card-head-title", children: [_jsx(NodeIndexOutlined, {}), " \u7533\u62A5\u6D41\u7A0B"] }), _jsx("span", { className: "sb-hint", children: "\u653F\u7B56\u6A21\u677F \u2192 \u751F\u6210\u6750\u6599\u5305 \u2192 \u63D0\u4EA4\u4EBA\u793E \u2192 \u62E8\u6B3E\u8DDF\u8E2A" })] }), _jsx("div", { className: "sb-flow-steps", children: FLOW_LABELS.map((label, i) => {
                                            const Icon = FLOW_ICONS[i];
                                            let cls = 'sb-flow-step';
                                            if (i < step)
                                                cls += ' done';
                                            else if (i === step)
                                                cls += ' active';
                                            return (_jsxs("div", { className: cls, children: [_jsx(Icon, { style: { display: 'block', fontSize: 18, marginBottom: 6 } }), label] }, i));
                                        }) }), _jsxs("div", { className: "sb-form-grid", children: [_jsxs("div", { className: "sb-form-group", children: [_jsx("label", { children: "\u8865\u8D34\u653F\u7B56\u6A21\u677F" }), _jsx(Select, { value: bundle.policyTemplate, style: { width: '100%' }, onChange: (v) => updateBundle((b) => { b.policyTemplate = v; }), options: POLICY_OPTIONS.map((p) => ({ value: p, label: p })) })] }), _jsxs("div", { className: "sb-form-group", children: [_jsx("label", { children: "\u9884\u8BA1\u8865\u8D34\u91D1\u989D\uFF08\u5143\uFF09" }), _jsx(InputNumber, { min: 0, placeholder: "\u6839\u636E\u653F\u7B56\u6D4B\u7B97", value: bundle.subsidyAmount, style: { width: '100%' }, onChange: (v) => updateBundle((b) => { b.subsidyAmount = v || 0; }) })] })] }), _jsxs("div", { className: "sb-toolbar", children: [_jsx(Button, { type: "primary", icon: _jsx(FileZipOutlined, {}), onClick: handleGenPackage, children: "\u751F\u6210\u7533\u62A5\u6750\u6599\u5305" }), _jsx(Button, { icon: _jsx(CheckCircleOutlined, {}), onClick: handleVerify, children: "\u6750\u6599\u6838\u9A8C\u6E05\u5355" }), _jsx(Button, { icon: _jsx(CloudUploadOutlined, {}), onClick: handleSubmitRsb, children: "\u63D0\u4EA4\u4EBA\u793E\u7CFB\u7EDF" }), _jsx(Button, { style: { background: '#10B981', color: '#fff', borderColor: '#10B981' }, icon: _jsx(DollarOutlined, {}), onClick: handleTrackRefund, children: "\u62E8\u6B3E\u8FDB\u5EA6\u767B\u8BB0" })] }), bundle.submitStatus === '已退回' && bundle.rejectReason && (_jsxs("div", { className: "sb-reject-box", children: [_jsx(ExclamationCircleOutlined, {}), " ", _jsx("strong", { children: "\u9000\u56DE\u539F\u56E0\uFF1A" }), bundle.rejectReason] })), _jsx(Table, { dataSource: bundle.materials, columns: materialColumns, rowKey: "key", pagination: false, locale: { emptyText: '暂无材料' } })] }), _jsxs("div", { className: "sb-footer-actions", children: [_jsx(Button, { icon: _jsx(SafetyCertificateOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-certification'); }, children: "\u8FD4\u56DE\u8BA4\u5B9A" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { type: "primary", style: { background: '#10B981', borderColor: '#10B981' }, icon: _jsx(ArrowLeftOutlined, { style: { transform: 'rotate(180deg)' } }), onClick: () => {
                                            if (!currentProject) {
                                                message.warning('请先选择项目');
                                                return;
                                            }
                                            setCurrentProjectId(+selectedProjectId);
                                            navigateTo('zhixun-graduation');
                                        }, children: "\u5B8C\u6210\u7533\u62A5\u670D\u52A1\uFF0C\u8FDB\u5165\u57F9\u8BAD\u7ED3\u4E1A" })] })] }))] })] }));
}

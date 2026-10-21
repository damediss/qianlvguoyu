import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, useCallback, useEffect } from 'react';
import { Select, InputNumber, Input, Button, Table, Tabs, message } from 'antd';
import { ArrowLeftOutlined, DollarOutlined, FolderOpenOutlined, UnorderedListOutlined, NodeIndexOutlined, CheckCircleOutlined, FileTextOutlined, AuditOutlined, TrophyOutlined, InfoCircleOutlined, ExclamationCircleOutlined, CalendarOutlined, PayCircleOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './RefundPage.css';
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
const FLOW_LABELS = ['回款计划', '收款登记', '项目结算', '审批结案'];
const APPROVAL_NODES = ['培训部提交', '财务复核', '总经理审批', '企业确认'];
function todayStr() {
    return new Date().toISOString().slice(0, 10);
}
function formatWan(yuan) {
    return (yuan / 10000).toFixed(2);
}
function agingLabel(dueDate) {
    const due = new Date(dueDate);
    const today = new Date(todayStr());
    const days = Math.floor((today.getTime() - due.getTime()) / 86400000);
    if (days < 0)
        return `剩余 ${-days} 天`;
    if (days === 0)
        return '今日到期';
    if (days <= 30)
        return `${days} 天`;
    if (days <= 90)
        return '31-90天';
    return '90天以上';
}
function planStatusClass(st) {
    if (st === '已回款')
        return 'st-done';
    if (st === '逾期')
        return 'st-overdue';
    if (st === '部分回款')
        return 'st-partial';
    return 'st-pending';
}
// ============================================================
// Mock 数据
// ============================================================
function getMockSubsidy(projectId) {
    return { refundStatus: '拨款中', subsidyAmount: 48000 };
}
function defaultContractAmount(planCount) {
    return (planCount || 40) * 45000;
}
function initData(projectId, planCount) {
    const contract = defaultContractAmount(planCount);
    const shenbao = getMockSubsidy(projectId);
    return {
        projectId,
        contractAmount: contract,
        planGenerated: false,
        paymentPlan: [],
        payments: [],
        orgSharePct: 70,
        settlement: {
            generated: false,
            orgAmount: 0,
            enterpriseAmount: 0,
            subsidyAmount: shenbao.subsidyAmount || 0,
            approvalStatus: '草稿',
            approvalStep: 0,
        },
        projectSummary: '',
        projectClosed: false,
    };
}
// ============================================================
// 主页面
// ============================================================
export function RefundPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const currentProjectId = useNavStore((s) => s.currentProjectId);
    const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
    const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
    const [data, setData] = useState(null);
    const [activeTab, setActiveTab] = useState('plan');
    const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
    useEffect(() => {
        if (selectedProjectId) {
            setData(initData(+selectedProjectId, currentProject?.planCount || 40));
        }
        else {
            setData(null);
        }
    }, [selectedProjectId]);
    const handleProjectChange = (v) => {
        setSelectedProjectId(v);
        setCurrentProjectId(v ? +v : null);
        setActiveTab('plan');
    };
    const shenbao = useMemo(() => getMockSubsidy(selectedProjectId ? +selectedProjectId : 0), [selectedProjectId]);
    const totalReceived = useCallback(() => {
        if (!data)
            return 0;
        return data.payments.reduce((s, p) => s + (p.amount || 0), 0);
    }, [data]);
    const syncPlanStatus = useCallback((d) => {
        if (!d.paymentPlan.length)
            return;
        const received = d.payments.reduce((s, p) => s + (p.amount || 0), 0);
        let allocated = 0;
        d.paymentPlan.forEach((plan) => {
            const planAmt = plan.amount || 0;
            if (allocated + planAmt <= received) {
                plan.status = '已回款';
                allocated += planAmt;
            }
            else if (allocated < received) {
                plan.status = '部分回款';
                allocated = received;
            }
            else {
                const due = new Date(plan.dueDate);
                const today = new Date(todayStr());
                plan.status = due < today ? '逾期' : '待回款';
            }
        });
    }, []);
    const updateData = useCallback((updater) => {
        setData((prev) => {
            if (!prev)
                return prev;
            const next = { ...prev, paymentPlan: prev.paymentPlan.map((p) => ({ ...p })), payments: prev.payments.map((p) => ({ ...p })), settlement: { ...prev.settlement } };
            updater(next);
            syncPlanStatus(next);
            return next;
        });
    }, [syncPlanStatus]);
    const stats = useMemo(() => {
        if (!data)
            return { contract: 0, received: 0, pending: 0, rate: 0 };
        const received = totalReceived();
        const contract = data.contractAmount || 0;
        const pending = Math.max(0, contract - received);
        const rate = contract ? Math.round((received / contract) * 100) : 0;
        return { contract, received, pending, rate };
    }, [data, totalReceived]);
    const flowStep = useMemo(() => {
        if (!data)
            return 0;
        if (data.projectClosed || data.settlement?.approvalStatus === '已结案')
            return 4;
        if (data.settlement?.approvalStatus === '审批中' || data.settlement?.approvalStatus === '已通过')
            return 3;
        if (data.settlement?.generated)
            return 2;
        if (data.payments?.length || data.planGenerated)
            return 1;
        return 0;
    }, [data]);
    const overduePlans = useMemo(() => (data?.paymentPlan || []).filter((p) => p.status === '逾期'), [data]);
    const handleGenPlan = () => {
        if (!data)
            return;
        const contract = data.contractAmount;
        if (!contract) {
            message.warning('请填写培训合同金额');
            return;
        }
        const p1 = Math.round(contract * 0.4);
        const p2 = Math.round(contract * 0.4);
        const p3 = contract - p1 - p2;
        const base = new Date();
        const addDays = (d) => {
            const x = new Date(base);
            x.setDate(x.getDate() + d);
            return x.toISOString().slice(0, 10);
        };
        updateData((d) => {
            d.contractAmount = contract;
            d.paymentPlan = [
                { phase: '首期（40%）', dueDate: addDays(0), amount: p1, status: '待回款' },
                { phase: '中期（40%）', dueDate: addDays(60), amount: p2, status: '待回款' },
                { phase: '尾款（20%）', dueDate: addDays(120), amount: p3, status: '待回款' },
            ];
            d.planGenerated = true;
        });
        message.success('已生成三期回款计划');
    };
    const handleRegisterPay = () => {
        if (!data || !data.planGenerated) {
            message.warning('请先生成回款计划');
            return;
        }
        const pending = data.paymentPlan.find((p) => p.status !== '已回款');
        const phase = pending?.phase || '补充收款';
        const amount = pending?.amount || 0;
        updateData((d) => {
            d.payments.push({
                id: Date.now(),
                date: todayStr(),
                amount,
                phase,
                method: '银行转账',
                remark: '企业付款',
            });
        });
        message.success(`已登记收款 ${amount.toLocaleString()} 元`);
    };
    const handleGenSettlement = () => {
        if (!data)
            return;
        const contract = data.contractAmount;
        if (!contract) {
            message.warning('请填写合同金额');
            return;
        }
        const received = data.payments.reduce((s, p) => s + (p.amount || 0), 0);
        const subsidy = shenbao?.subsidyAmount || 0;
        const totalSettle = received + (shenbao?.refundStatus === '已拨款' ? subsidy : 0);
        const orgPct = data.orgSharePct;
        updateData((d) => {
            d.orgSharePct = orgPct;
            d.settlement = {
                generated: true,
                orgAmount: Math.round(totalSettle * orgPct / 100),
                enterpriseAmount: Math.round(totalSettle * (100 - orgPct) / 100),
                subsidyAmount: subsidy,
                approvalStatus: '草稿',
                approvalStep: 0,
            };
        });
        message.success('项目结算单已生成');
    };
    const handleSubmitApproval = () => {
        if (!data || !data.settlement?.generated) {
            message.warning('请先生成结算单');
            return;
        }
        updateData((d) => {
            const s = d.settlement;
            if (s.approvalStatus === '草稿') {
                s.approvalStatus = '审批中';
                s.approvalStep = 1;
                message.success('结算单已提交，进入财务复核');
            }
            else if (s.approvalStatus === '审批中') {
                if (s.approvalStep < APPROVAL_NODES.length) {
                    s.approvalStep++;
                    if (s.approvalStep >= APPROVAL_NODES.length) {
                        s.approvalStatus = '已通过';
                        message.success('结算审批已全部通过');
                    }
                    else {
                        message.success(`当前节点：${APPROVAL_NODES[s.approvalStep]}`);
                    }
                }
            }
            else if (s.approvalStatus === '已通过') {
                s.approvalStatus = '已结案';
                d.projectClosed = true;
                message.success('项目已结案，全流程完成');
            }
        });
    };
    const planColumns = [
        { title: '期次', dataIndex: 'phase', render: (v) => _jsx("strong", { children: v }) },
        { title: '计划回款日', dataIndex: 'dueDate' },
        { title: '计划金额（元）', dataIndex: 'amount', render: (v) => (v || 0).toLocaleString() },
        { title: '账龄', dataIndex: 'dueDate', render: (v) => agingLabel(v) },
        { title: '状态', dataIndex: 'status', render: (v) => _jsx("span", { className: `status-badge ${planStatusClass(v)}`, children: v }) },
    ];
    const paymentColumns = [
        { title: '收款日期', dataIndex: 'date' },
        { title: '金额（元）', dataIndex: 'amount', render: (v) => (v || 0).toLocaleString() },
        { title: '对应期次', dataIndex: 'phase' },
        { title: '方式', dataIndex: 'method' },
        { title: '备注', dataIndex: 'remark', render: (v) => v || '—' },
    ];
    const tabItems = [
        {
            key: 'plan', label: '回款计划',
            children: (_jsx(Table, { dataSource: data?.paymentPlan || [], columns: planColumns, rowKey: "phase", pagination: false, locale: { emptyText: '请点击「生成回款计划」' } })),
        },
        {
            key: 'payment', label: '收款明细',
            children: (_jsx(Table, { dataSource: data?.payments || [], columns: paymentColumns, rowKey: "id", pagination: false, locale: { emptyText: '暂无收款记录' } })),
        },
        {
            key: 'settlement', label: '项目结算',
            children: (_jsx("div", { children: !data?.settlement?.generated ? (_jsx("div", { className: "hk-settlement-empty", children: "\u8BF7\u5148\u751F\u6210\u9879\u76EE\u7ED3\u7B97\u5355" })) : (_jsxs("div", { children: [_jsxs("div", { className: "hk-settlement-box", children: [_jsxs("div", { className: "hk-settlement-row", children: [_jsx("span", { children: "\u57F9\u8BAD\u5408\u540C\u603B\u989D" }), _jsxs("span", { children: [data.contractAmount.toLocaleString(), " \u5143"] })] }), _jsxs("div", { className: "hk-settlement-row", children: [_jsx("span", { children: "\u7D2F\u8BA1\u5DF2\u56DE\u6B3E" }), _jsxs("span", { children: [totalReceived().toLocaleString(), " \u5143"] })] }), _jsxs("div", { className: "hk-settlement-row", children: [_jsx("span", { children: "\u8865\u8D34\u5230\u8D26\uFF08\u9884\u8BA1\uFF09" }), _jsxs("span", { children: [(data.settlement.subsidyAmount || 0).toLocaleString(), " \u5143"] })] }), _jsxs("div", { className: "hk-settlement-row", children: [_jsxs("span", { children: ["\u673A\u6784\u5206\u6210\uFF08", data.orgSharePct, "%\uFF09"] }), _jsxs("span", { children: [data.settlement.orgAmount.toLocaleString(), " \u5143"] })] }), _jsxs("div", { className: "hk-settlement-row", children: [_jsx("span", { children: "\u4F01\u4E1A\u7ED3\u7B97\u4EFD\u989D" }), _jsxs("span", { children: [data.settlement.enterpriseAmount.toLocaleString(), " \u5143"] })] }), _jsxs("div", { className: "hk-settlement-row hk-settlement-total", children: [_jsx("span", { children: "\u7ED3\u7B97\u5355\u72B6\u6001" }), _jsx("span", { children: data.settlement.approvalStatus })] })] }), _jsx("div", { className: "hk-approval-track", children: APPROVAL_NODES.map((name, i) => {
                                const s = data.settlement;
                                let cls = 'hk-approval-node';
                                if (i < s.approvalStep)
                                    cls += ' done';
                                else if (i === s.approvalStep && s.approvalStatus === '审批中')
                                    cls += ' active';
                                else if (s.approvalStatus === '已结案' || s.approvalStatus === '已通过')
                                    cls += ' done';
                                return _jsx("div", { className: cls, children: name }, i);
                            }) }), _jsxs("div", { className: "hk-form-group", style: { marginTop: 20 }, children: [_jsx("label", { children: "\u9879\u76EE\u603B\u7ED3\u6458\u8981" }), _jsx(Input.TextArea, { rows: 3, placeholder: "\u586B\u5199\u9879\u76EE\u7ED3\u7B97\u8BF4\u660E\u3001\u7ECF\u9A8C\u6C89\u6DC0\u8981\u70B9\uFF08\u53EF\u9009\uFF09", value: data.projectSummary, onChange: (e) => updateData((d) => { d.projectSummary = e.target.value; }) })] })] })) })),
        },
    ];
    return (_jsxs("div", { className: "hk-page", children: [_jsxs("header", { className: "hk-topbar", children: [_jsxs("div", { className: "hk-topbar-left", children: [_jsxs("a", { className: "hk-back-link", onClick: () => navigateTo('zhixun-training-process'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u57F9\u8BAD\u6D41\u7A0B"] }), _jsxs("div", { children: [_jsx("h1", { className: "hk-title", children: "\u9879\u76EE\u56DE\u6B3E" }), _jsx("span", { className: "hk-sub", children: currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 9 / 9` : '合同回款与项目结算 · 步骤 9 / 9' })] })] }), _jsxs("span", { className: "hk-process-badge", children: [_jsx(DollarOutlined, {}), " \u2026 \u2192 \u7ED3\u4E1A \u2192 \u56DE\u6B3E \u2713"] })] }), _jsxs("main", { className: "hk-page-body", children: [_jsxs("div", { className: "hk-card", children: [_jsxs("div", { className: "hk-card-head", children: [_jsxs("div", { className: "hk-card-head-title", children: [_jsx(FolderOpenOutlined, {}), " \u57F9\u8BAD\u9879\u76EE"] }), _jsx(Button, { size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => navigateTo('zhixun-task-archives'), children: "\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { className: "hk-form-group", children: [_jsx("label", { children: "\u9009\u62E9\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u2026", value: selectedProjectId || undefined, onChange: handleProjectChange, allowClear: true, style: { width: '100%' }, options: DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` })) })] }), currentProject && data && (_jsxs("div", { className: "hk-project-banner", children: [_jsxs("div", { className: "hk-banner-item", children: [_jsx("label", { children: "\u4F01\u4E1A" }), _jsx("strong", { children: currentProject.enterprise || '—' })] }), _jsxs("div", { className: "hk-banner-item", children: [_jsx("label", { children: "\u57F9\u8BAD\u9879\u76EE" }), _jsx("strong", { children: currentProject.name || '—' })] }), _jsxs("div", { className: "hk-banner-item", children: [_jsx("label", { children: "\u5408\u540C\u91D1\u989D" }), _jsxs("strong", { children: [formatWan(data.contractAmount), " \u4E07"] })] }), _jsxs("div", { className: "hk-banner-item", children: [_jsx("label", { children: "\u5DF2\u56DE\u6B3E" }), _jsxs("strong", { children: [formatWan(totalReceived()), " \u4E07"] })] }), _jsxs("div", { className: "hk-banner-item", children: [_jsx("label", { children: "\u7ED3\u7B97\u72B6\u6001" }), _jsx("strong", { children: data.settlement?.approvalStatus || '草稿' })] })] }))] }), !currentProject ? (_jsxs("div", { className: "hk-empty", children: [_jsx(InfoCircleOutlined, { style: { fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' } }), _jsxs("p", { children: ["\u8BF7\u5148\u9009\u62E9", _jsx("strong", { children: "\u4F01\u4E1A\u57F9\u8BAD\u9879\u76EE" }), "\uFF0C\u518D\u7BA1\u7406\u56DE\u6B3E\u8BA1\u5212\u3001\u6536\u6B3E\u767B\u8BB0\u4E0E\u9879\u76EE\u7ED3\u7B97\u3002"] }), _jsx("p", { style: { marginTop: 8, fontSize: 13 }, children: "\u5EFA\u8BAE\u5148\u5B8C\u6210 \u7ED3\u4E1A \u4E0E \u7533\u62A5\uFF1B\u4E5F\u53EF\u4ECE\u4EFB\u52A1\u6863\u6848\u8FDB\u5165\u3002" })] })) : data && (_jsxs(_Fragment, { children: [overduePlans.length > 0 && (_jsxs("div", { className: "hk-alert warn", children: [_jsx(ExclamationCircleOutlined, {}), _jsxs("span", { children: ["\u6709 ", overduePlans.length, " \u671F\u56DE\u6B3E\u8BA1\u5212\u5DF2\u903E\u671F\uFF0C\u8BF7\u8DDF\u8FDB\u4F01\u4E1A\u4ED8\u6B3E\u6216\u66F4\u65B0\u6536\u6B3E\u767B\u8BB0\u3002"] })] })), _jsxs("div", { className: "hk-stat-row", children: [_jsxs("div", { className: "hk-stat-card", children: [_jsx("div", { className: "hk-stat-val", children: formatWan(stats.contract) }), _jsx("div", { className: "hk-stat-lbl", children: "\u5408\u540C\u91D1\u989D\uFF08\u4E07\uFF09" })] }), _jsxs("div", { className: "hk-stat-card", children: [_jsx("div", { className: "hk-stat-val success", children: formatWan(stats.received) }), _jsx("div", { className: "hk-stat-lbl", children: "\u5DF2\u56DE\u6B3E\uFF08\u4E07\uFF09" })] }), _jsxs("div", { className: "hk-stat-card", children: [_jsx("div", { className: "hk-stat-val warn", children: formatWan(stats.pending) }), _jsx("div", { className: "hk-stat-lbl", children: "\u5F85\u56DE\u6B3E\uFF08\u4E07\uFF09" })] }), _jsxs("div", { className: "hk-stat-card", children: [_jsxs("div", { className: `hk-stat-val${stats.rate >= 100 ? ' success' : stats.rate < 50 ? ' warn' : ''}`, children: [stats.rate, "%"] }), _jsx("div", { className: "hk-stat-lbl", children: "\u56DE\u6B3E\u7387" })] })] }), _jsxs("div", { className: "hk-card", children: [_jsxs("div", { className: "hk-card-head", children: [_jsxs("div", { className: "hk-card-head-title", children: [_jsx(NodeIndexOutlined, {}), " \u56DE\u6B3E\u4E0E\u7ED3\u7B97\u6D41\u7A0B"] }), _jsx("span", { className: "hk-hint", children: "\u56DE\u6B3E\u8BA1\u5212 \u2192 \u6536\u6B3E\u767B\u8BB0 \u2192 \u9879\u76EE\u7ED3\u7B97 \u2192 \u5BA1\u6279\u7ED3\u6848" })] }), _jsx("div", { className: "hk-flow-steps", children: FLOW_LABELS.map((label, i) => {
                                            const flowIcons = [CalendarOutlined, PayCircleOutlined, FileTextOutlined, AuditOutlined];
                                            const Icon = flowIcons[i];
                                            let cls = 'hk-flow-step';
                                            if (i < flowStep)
                                                cls += ' done';
                                            else if (i === flowStep)
                                                cls += ' active';
                                            return (_jsxs("div", { className: cls, children: [_jsx(Icon, { style: { display: 'block', fontSize: 18, marginBottom: 6 } }), label] }, i));
                                        }) }), _jsxs("div", { className: "hk-form-grid", children: [_jsxs("div", { className: "hk-form-group", children: [_jsx("label", { children: "\u57F9\u8BAD\u5408\u540C\u91D1\u989D\uFF08\u5143\uFF09" }), _jsx(InputNumber, { min: 0, placeholder: "\u4F01\u4E1A\u57F9\u8BAD\u670D\u52A1\u5408\u540C\u603B\u989D", value: data.contractAmount, style: { width: '100%' }, onChange: (v) => updateData((d) => { d.contractAmount = v || 0; }) })] }), _jsxs("div", { className: "hk-form-group", children: [_jsx("label", { children: "\u8865\u8D34\u62E8\u6B3E\u72B6\u6001\uFF08\u6765\u81EA\u7533\u62A5\uFF09" }), _jsx(Input, { value: shenbao?.refundStatus || '未申报', readOnly: true })] }), _jsxs("div", { className: "hk-form-group", children: [_jsx("label", { children: "\u9884\u8BA1\u8865\u8D34\u91D1\u989D\uFF08\u5143\uFF09" }), _jsx(InputNumber, { value: shenbao?.subsidyAmount || data.settlement?.subsidyAmount || 0, readOnly: true, style: { width: '100%' } })] }), _jsxs("div", { className: "hk-form-group", children: [_jsx("label", { children: "\u673A\u6784\u5206\u6210\u6BD4\u4F8B\uFF08%\uFF09" }), _jsx(InputNumber, { min: 0, max: 100, value: data.orgSharePct, style: { width: '100%' }, onChange: (v) => updateData((d) => { d.orgSharePct = v || 70; }) })] })] }), _jsxs("div", { className: "hk-toolbar", children: [_jsx(Button, { type: "primary", icon: _jsx(CalendarOutlined, {}), onClick: handleGenPlan, children: "\u751F\u6210\u56DE\u6B3E\u8BA1\u5212" }), _jsx(Button, { icon: _jsx(PayCircleOutlined, {}), onClick: handleRegisterPay, children: "\u767B\u8BB0\u6536\u6B3E" }), _jsx(Button, { icon: _jsx(FileTextOutlined, {}), onClick: handleGenSettlement, children: "\u751F\u6210\u7ED3\u7B97\u5355" }), _jsx(Button, { style: { background: '#10B981', color: '#fff', borderColor: '#10B981' }, icon: _jsx(AuditOutlined, {}), onClick: handleSubmitApproval, children: "\u63D0\u4EA4\u7ED3\u7B97\u5BA1\u6279" })] }), _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: tabItems })] }), _jsxs("div", { className: "hk-footer-actions", children: [_jsx(Button, { icon: _jsx(TrophyOutlined, {}), onClick: () => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-graduation'); }, children: "\u8FD4\u56DE\u7ED3\u4E1A" }), _jsx(Button, { onClick: () => navigateTo('zhixun-training-process'), children: "\u4FDD\u5B58\u5E76\u8FD4\u56DE\u6D41\u7A0B" }), _jsx(Button, { type: "primary", style: { background: '#10B981', borderColor: '#10B981' }, icon: _jsx(CheckCircleOutlined, {}), onClick: () => {
                                            if (!currentProject) {
                                                message.warning('请先选择项目');
                                                return;
                                            }
                                            navigateTo('zhixun-training-process');
                                        }, children: "\u5B8C\u6210\u5168\u6D41\u7A0B" })] })] }))] })] }));
}

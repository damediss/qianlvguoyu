import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Empty, Input, Progress, Result, Row, Space, Spin, Table, Tag, Typography, } from 'antd';
import { BankOutlined, DashboardOutlined, ReloadOutlined, WarningOutlined, } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { useEffect, useMemo, useState } from 'react';
import * as trainApi from '../../api/training';
import { PermissionGate } from '../../directives/permission';
import { usePaneActive, usePaneActiveRef } from '../../layouts/TabKeepAlive';
import { ReviewSyncPanel } from './ReviewSyncPanel';
import './TrainingDashboardPage.css';
const STAGE_COLORS = {
    待开班: '#faad14',
    培训中: '#1890ff',
    已结业: '#52c41a',
    pending: '#faad14',
    in_progress: '#1890ff',
    reviewing: '#722ed1',
    completed: '#52c41a',
};
function resolveColor(stage) {
    return STAGE_COLORS[stage] ?? '#8c8c8c';
}
function resolveErrorMessage(error) {
    return error instanceof Error ? error.message : '数据加载失败，请稍后重试';
}
function resolveStatusTag(status) {
    const normalized = status?.toLowerCase() ?? '';
    if (normalized === 'danger' || normalized === 'error' || normalized === 'critical' || status === '预警') {
        return _jsx(Tag, { color: "red", children: "\u9884\u8B66" });
    }
    if (normalized === 'warning' || status === '关注') {
        return _jsx(Tag, { color: "orange", children: "\u6CE8\u610F" });
    }
    if (normalized === 'success' || normalized === 'normal' || normalized === 'ok' || status === '正常') {
        return _jsx(Tag, { color: "green", children: "\u6B63\u5E38" });
    }
    return _jsx(Tag, { children: status || '未知' });
}
function formatRate(value) {
    const n = Number(value ?? 0);
    return Number.isFinite(n) ? n.toFixed(1) : '0.0';
}
function stageTag(stage) {
    const color = stage === '已结业' ? 'green' : stage === '培训中' ? 'blue' : 'gold';
    return _jsx(Tag, { color: color, children: stage || '待开班' });
}
const LIVE_INTERVAL_MS = 10_000;
const liveQueryOptions = (paneActive) => ({
    // 标签页不可见时暂停 10s 轮询（§3.6）
    refetchInterval: paneActive ? LIVE_INTERVAL_MS : false,
    refetchIntervalInBackground: false,
    refetchOnWindowFocus: true,
    staleTime: 0,
});
export function TrainingDashboardPage() {
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询与时钟 tick（§3.6）
    const paneActiveRef = usePaneActiveRef(paneActive);
    const [companyKeyword, setCompanyKeyword] = useState('');
    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        const timer = window.setInterval(() => {
            // 标签不可见时计时继续、跳过 setState（§3.6 乙组口径：渲染类 tick 省 CPU）
            if (paneActiveRef.current)
                setNow(Date.now());
        }, 1000);
        return () => window.clearInterval(timer);
    }, []);
    const dashboardQuery = useQuery({
        queryKey: ['training-dashboard'],
        queryFn: () => trainApi.getDashboard(),
        ...liveQueryOptions(paneActive),
    });
    const milestonesQuery = useQuery({
        queryKey: ['training-dashboard-milestones'],
        queryFn: () => trainApi.getDashboardMilestones(),
        ...liveQueryOptions(paneActive),
    });
    const stageQuery = useQuery({
        queryKey: ['training-dashboard-stage'],
        queryFn: () => trainApi.getDashboardStageDistribution(),
        ...liveQueryOptions(paneActive),
    });
    const handleRefresh = () => {
        void dashboardQuery.refetch();
        void milestonesQuery.refetch();
        void stageQuery.refetch();
    };
    const partialErrors = [];
    if (milestonesQuery.isError) {
        partialErrors.push(`项目里程碑：${resolveErrorMessage(milestonesQuery.error)}`);
    }
    if (stageQuery.isError) {
        partialErrors.push(`阶段分布：${resolveErrorMessage(stageQuery.error)}`);
    }
    const data = dashboardQuery.data;
    const summary = data?.summary;
    const monthlyTrends = data?.monthlyTrends ?? [];
    const alerts = data?.alerts ?? [];
    const modules = data?.modules ?? [];
    const companies = data?.companies ?? [];
    const filteredCompanies = useMemo(() => {
        const kw = companyKeyword.trim().toLowerCase();
        const list = kw
            ? companies.filter((c) => (c.companyName || '').toLowerCase().includes(kw))
            : [...companies];
        const rank = (c) => {
            if (c.inProgressCount > 0 || c.activeClassCount > 0)
                return 0;
            if (c.trainingCount > 0 || c.studentCount > 0 || c.classCount > 0)
                return 1;
            return 2;
        };
        return [...list].sort((a, b) => {
            const byRank = rank(a) - rank(b);
            if (byRank !== 0)
                return byRank;
            if (b.inProgressCount !== a.inProgressCount)
                return b.inProgressCount - a.inProgressCount;
            if (b.studentCount !== a.studentCount)
                return b.studentCount - a.studentCount;
            if (b.trainingCount !== a.trainingCount)
                return b.trainingCount - a.trainingCount;
            return (a.companyName || '').localeCompare(b.companyName || '', 'zh');
        });
    }, [companies, companyKeyword]);
    const trendOption = useMemo(() => ({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 16, top: 24, bottom: 32 },
        xAxis: {
            type: 'category',
            data: monthlyTrends.map((m) => m.month),
            axisLine: { lineStyle: { color: '#d9d9d9' } },
        },
        yAxis: { type: 'value', name: '方案数', minInterval: 1 },
        series: [
            {
                name: '新增方案',
                type: 'line',
                smooth: true,
                data: monthlyTrends.map((m) => m.count),
                areaStyle: { color: 'rgba(47,107,255,0.16)' },
                lineStyle: { color: '#2f6bff' },
                itemStyle: { color: '#2f6bff' },
            },
        ],
    }), [monthlyTrends]);
    const stageEntries = useMemo(() => (stageQuery.isError ? [] : Object.entries(stageQuery.data ?? {})), [stageQuery.data, stageQuery.isError]);
    const stageOption = useMemo(() => ({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [
            {
                type: 'pie',
                radius: ['42%', '68%'],
                center: ['50%', '44%'],
                data: stageEntries.map(([name, value]) => ({ name, value })),
                label: { formatter: '{b}\n{c}项' },
                color: stageEntries.map(([name]) => resolveColor(name)),
            },
        ],
    }), [stageEntries]);
    const companyBarOption = useMemo(() => {
        const top = [...filteredCompanies]
            .sort((a, b) => b.studentCount - a.studentCount)
            .slice(0, 10)
            .reverse();
        return {
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            grid: { left: 16, right: 36, top: 12, bottom: 24, containLabel: true },
            xAxis: { type: 'value', name: '学员数', minInterval: 1 },
            yAxis: {
                type: 'category',
                data: top.map((c) => c.companyName),
                axisLabel: { width: 100, overflow: 'truncate' },
            },
            series: [{
                    type: 'bar',
                    data: top.map((c) => c.studentCount),
                    itemStyle: { color: '#2f6bff', borderRadius: [0, 5, 5, 0] },
                    label: { show: true, position: 'right' },
                }],
        };
    }, [filteredCompanies]);
    const alertColumns = [
        { title: '指标', dataIndex: 'indicator', width: 140 },
        { title: '当前值', dataIndex: 'currentValue', width: 100 },
        { title: '阈值', dataIndex: 'threshold', width: 100 },
        { title: '趋势', dataIndex: 'trend', width: 100 },
        { title: '状态', dataIndex: 'status', width: 90, render: resolveStatusTag },
        { title: '建议', dataIndex: 'suggestion', ellipsis: true },
    ];
    const milestoneColumns = [
        { title: '项目/方案', dataIndex: 'projectName', ellipsis: true },
        { title: '所属企业', dataIndex: 'companyName', width: 150, ellipsis: true },
        { title: '当前阶段', dataIndex: 'currentStage', width: 100 },
        {
            title: '进度',
            dataIndex: 'progress',
            width: 140,
            render: (v) => (_jsx(Progress, { percent: v ?? 0, size: "small", status: v >= 100 ? 'success' : v >= 40 ? 'active' : 'normal' })),
        },
    ];
    const companyColumns = [
        {
            title: '企业名称',
            dataIndex: 'companyName',
            ellipsis: true,
            render: (name, row) => (_jsxs(Space, { children: [_jsx(BankOutlined, { style: { color: '#2f6bff' } }), _jsx("span", { children: name }), row.trainingCount === 0 && row.projectCount === 0 ? _jsx(Tag, { children: "\u672A\u5F00\u73ED" }) : null] })),
        },
        { title: '方案', dataIndex: 'projectCount', width: 70, align: 'right' },
        { title: '开班', dataIndex: 'trainingCount', width: 70, align: 'right' },
        {
            title: '阶段',
            width: 180,
            render: (_, row) => (_jsxs(Space, { size: 4, wrap: true, children: [_jsxs(Tag, { color: "gold", children: ["\u5F85", row.pendingCount] }), _jsxs(Tag, { color: "blue", children: ["\u57F9", row.inProgressCount] }), _jsxs(Tag, { color: "green", children: ["\u7ED3", row.completedCount] })] })),
        },
        { title: '班级', dataIndex: 'classCount', width: 70, align: 'right' },
        { title: '在培班', dataIndex: 'activeClassCount', width: 80, align: 'right' },
        { title: '学员', dataIndex: 'studentCount', width: 70, align: 'right' },
        { title: '考试', dataIndex: 'examCount', width: 70, align: 'right' },
        { title: '证书', dataIndex: 'issuedCertCount', width: 70, align: 'right' },
        { title: '补贴', dataIndex: 'subsidyCount', width: 70, align: 'right' },
    ];
    const trainingColumns = [
        { title: '培训名称', dataIndex: 'title', ellipsis: true },
        { title: '阶段', dataIndex: 'stage', width: 90, render: (v) => stageTag(v) },
        {
            title: '培训周期',
            width: 180,
            render: (_, row) => {
                const start = row.startDate || '-';
                const end = row.endDate || '-';
                return `${start} ~ ${end}`;
            },
        },
        { title: '班级', dataIndex: 'classCount', width: 70, align: 'right' },
        { title: '学员', dataIndex: 'studentCount', width: 70, align: 'right' },
        { title: '考试', dataIndex: 'examCount', width: 70, align: 'right' },
        { title: '证书', dataIndex: 'issuedCertCount', width: 70, align: 'right' },
    ];
    const rateCards = summary
        ? [
            { title: '计划执行率', value: summary.planExecutionRate, foot: '已结业方案 / 已立项方案' },
            { title: '培训完成率', value: summary.trainingCompletionRate, foot: '已结业开班 / 全部开班' },
            { title: '资料完整率', value: summary.materialSubmissionRate, foot: '已关联学员 / 花名册' },
            { title: '证书发放率', value: summary.certificationRate, foot: '已发证书 / 学员数' },
            { title: '补贴审批率', value: summary.subsidyApprovalRate, foot: '已通过预算 / 预算总数' },
        ]
        : [];
    const renderBody = () => {
        if (dashboardQuery.isLoading && !dashboardQuery.data) {
            return _jsx(Spin, { size: "large", style: { display: 'block', margin: 120 } });
        }
        if (dashboardQuery.isError) {
            return (_jsx(Result, { status: "error", title: "\u57F9\u8BAD\u6570\u636E\u9762\u677F\u52A0\u8F7D\u5931\u8D25", subTitle: resolveErrorMessage(dashboardQuery.error), extra: _jsx(Button, { type: "primary", onClick: handleRefresh, children: "\u91CD\u8BD5" }) }));
        }
        if (!summary) {
            return _jsx(Empty, { description: "\u6682\u65E0\u770B\u677F\u6570\u636E" });
        }
        return (_jsxs(_Fragment, { children: [partialErrors.length > 0 && (_jsx(Alert, { type: "warning", showIcon: true, message: "\u90E8\u5206\u6570\u636E\u52A0\u8F7D\u5931\u8D25", description: _jsx("ul", { style: { margin: 0, paddingLeft: 20 }, children: partialErrors.map((item) => (_jsx("li", { children: item }, item))) }), action: _jsx(Button, { size: "small", onClick: handleRefresh, children: "\u91CD\u8BD5" }) })), _jsx("div", { className: "training-dashboard-kpis", children: rateCards.map((k) => (_jsxs("div", { className: "training-dashboard-kpi", children: [_jsx("div", { className: "training-dashboard-kpi-label", children: k.title }), _jsxs("div", { className: "training-dashboard-kpi-value", children: [formatRate(k.value), "%"] }), _jsx("div", { className: "training-dashboard-kpi-foot", children: k.foot })] }, k.title))) }), _jsx(ReviewSyncPanel, {}), _jsx(Card, { title: "\u57F9\u8BAD\u5168\u6D41\u7A0B\u6A21\u5757\u6982\u89C8", size: "small", children: _jsx("div", { className: "training-dashboard-modules", children: (modules.length > 0
                            ? modules
                            : [
                                { code: 'projects', name: '方案立项', count: summary.totalProjects, hint: '已立项方案' },
                                { code: 'students', name: '学员花名册', count: summary.totalStudents, hint: '在册学员' },
                                { code: 'certs', name: '资质证书', count: summary.totalCertificates, hint: '论文/专利等' },
                                { code: 'budgets', name: '预算条目', count: summary.totalBudgets, hint: '预算总数' },
                            ]).map((m) => (_jsxs("div", { className: "training-dashboard-module", children: [_jsx("div", { className: "training-dashboard-module-title", children: _jsx("span", { children: m.name }) }), _jsx("div", { className: "training-dashboard-module-value", children: m.count }), _jsx("div", { className: "training-dashboard-module-hint", children: m.hint })] }, m.code))) }) }), _jsxs(Row, { gutter: 16, children: [_jsx(Col, { xs: 12, sm: 8, md: 6, lg: 4, children: _jsxs(Card, { size: "small", children: [_jsx(Typography.Text, { type: "secondary", children: "\u4F01\u4E1A\u603B\u6570" }), _jsx("div", { style: { fontSize: 22, fontWeight: 700 }, children: summary.totalCompanies ?? companies.length })] }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, lg: 4, children: _jsxs(Card, { size: "small", children: [_jsx(Typography.Text, { type: "secondary", children: "\u53C2\u8BAD\u4F01\u4E1A" }), _jsx("div", { style: { fontSize: 22, fontWeight: 700 }, children: summary.activeCompanies ?? 0 })] }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, lg: 4, children: _jsxs(Card, { size: "small", children: [_jsx(Typography.Text, { type: "secondary", children: "\u5728\u57F9\u5F00\u73ED" }), _jsx("div", { style: { fontSize: 22, fontWeight: 700 }, children: summary.inProgressTrainings ?? 0 })] }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, lg: 4, children: _jsxs(Card, { size: "small", children: [_jsx(Typography.Text, { type: "secondary", children: "\u73ED\u7EA7\u603B\u6570" }), _jsx("div", { style: { fontSize: 22, fontWeight: 700 }, children: summary.totalClasses ?? 0 })] }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, lg: 4, children: _jsxs(Card, { size: "small", children: [_jsx(Typography.Text, { type: "secondary", children: "\u5B66\u5458\u603B\u6570" }), _jsx("div", { style: { fontSize: 22, fontWeight: 700 }, children: summary.totalStudents ?? 0 })] }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, lg: 4, children: _jsxs(Card, { size: "small", children: [_jsx(Typography.Text, { type: "secondary", children: "\u5DF2\u53D1\u8BC1\u4E66" }), _jsx("div", { style: { fontSize: 22, fontWeight: 700 }, children: summary.totalIssuedCerts ?? 0 })] }) })] }), _jsx(Card, { title: _jsxs(Space, { children: [_jsx(BankOutlined, {}), "\u4F01\u4E1A\u57F9\u8BAD\u660E\u7EC6", _jsxs(Typography.Text, { type: "secondary", children: ["\u5171 ", filteredCompanies.length, " \u5BB6"] })] }), extra: _jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u4F01\u4E1A", style: { width: 220 }, onSearch: setCompanyKeyword, onChange: (e) => {
                            if (!e.target.value)
                                setCompanyKeyword('');
                        } }), children: _jsx(Table, { rowKey: (r) => String(r.companyId), size: "middle", columns: companyColumns, dataSource: filteredCompanies, pagination: { pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 家企业` }, locale: { emptyText: '暂无企业培训数据' }, expandable: {
                            expandedRowRender: (row) => row.trainings?.length ? (_jsx(Table, { rowKey: (r) => String(r.trainInfoId), size: "small", columns: trainingColumns, dataSource: row.trainings, pagination: false })) : (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u8BE5\u4F01\u4E1A\u6682\u65E0\u5F00\u73ED\u57F9\u8BAD" })),
                            rowExpandable: () => true,
                        } }) }), _jsxs("div", { className: "training-dashboard-chart-grid", children: [_jsx(Card, { title: "\u8FD1 6 \u4E2A\u6708\u65B9\u6848\u7ACB\u9879\u8D8B\u52BF", children: monthlyTrends.length > 0 ? (_jsx(ReactECharts, { option: trendOption, className: "training-dashboard-chart" })) : (_jsx(Empty, { description: "\u6682\u65E0\u8D8B\u52BF\u6570\u636E" })) }), _jsx(Card, { title: "\u5F00\u73ED\u9636\u6BB5\u5206\u5E03", loading: stageQuery.isLoading, children: stageQuery.isError ? (_jsx(Empty, { description: resolveErrorMessage(stageQuery.error) })) : stageEntries.length > 0 ? (_jsx(ReactECharts, { option: stageOption, className: "training-dashboard-chart" })) : (_jsx(Empty, { description: "\u6682\u65E0\u9636\u6BB5\u6570\u636E" })) })] }), _jsx(Card, { title: "\u4F01\u4E1A\u5B66\u5458\u89C4\u6A21 Top10", children: filteredCompanies.some((c) => c.studentCount > 0) ? (_jsx(ReactECharts, { option: companyBarOption, className: "training-dashboard-chart" })) : (_jsx(Empty, { description: "\u6682\u65E0\u4F01\u4E1A\u5B66\u5458\u6570\u636E" })) }), _jsxs(Row, { gutter: 16, children: [_jsx(Col, { span: 14, children: _jsx(Card, { title: _jsxs(Space, { children: [_jsx(WarningOutlined, { style: { color: '#fa8c16' } }), "\u98CE\u9669\u9884\u8B66"] }), children: _jsx(Table, { rowKey: (_, i) => `${_.indicator}-${i}`, size: "middle", columns: alertColumns, dataSource: alerts, pagination: false, locale: { emptyText: '暂无预警' } }) }) }), _jsx(Col, { span: 10, children: _jsx(Card, { title: "\u9879\u76EE\u91CC\u7A0B\u7891", loading: milestonesQuery.isLoading, children: milestonesQuery.isError ? (_jsx(Empty, { description: resolveErrorMessage(milestonesQuery.error) })) : (_jsx(Table, { rowKey: (r) => String(r.projectId), size: "middle", columns: milestoneColumns, dataSource: milestonesQuery.data ?? [], pagination: false, scroll: { y: 320 }, locale: { emptyText: '暂无进行中的项目' } })) }) })] })] }));
    };
    return (_jsx(PermissionGate, { code: "train:dashboard:view", fallback: _jsx(Empty, { description: "\u6682\u65E0\u57F9\u8BAD\u6570\u636E\u9762\u677F\u8BBF\u95EE\u6743\u9650" }), children: _jsxs("div", { className: "training-dashboard", children: [_jsxs("section", { className: "page-heading", children: [_jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(DashboardOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u57F9\u8BAD\u7BA1\u7406\u6570\u636E\u9A7E\u9A76\u8231" }), _jsx(Typography.Text, { type: "secondary", children: "\u8986\u76D6\u4F01\u4E1A\u3001\u65B9\u6848\u7ACB\u9879\u3001\u5F00\u73ED\u8DDF\u73ED\u3001\u8003\u8BD5\u53D1\u8BC1\u3001\u8865\u8D34\u7533\u62A5\uFF0C\u9875\u9762\u6253\u5F00\u65F6\u6BCF 10 \u79D2\u81EA\u52A8\u5237\u65B0" })] })] }), _jsxs(Space, { children: [_jsxs("span", { className: "training-dashboard-live", children: [_jsx("span", { className: "training-dashboard-live-dot" }), "\u5B9E\u65F6", dashboardQuery.dataUpdatedAt
                                            ? ` · ${Math.max(0, Math.floor((now - dashboardQuery.dataUpdatedAt) / 1000))} 秒前更新`
                                            : ''] }), _jsx(Button, { type: "link", icon: _jsx(ReloadOutlined, {}), onClick: handleRefresh, children: "\u7ACB\u5373\u5237\u65B0" })] })] }), renderBody()] }) }));
}

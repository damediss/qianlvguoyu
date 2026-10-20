import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { AlertOutlined, CheckCircleOutlined, DashboardOutlined, FileDoneOutlined, ReloadOutlined, TeamOutlined, } from '@ant-design/icons';
import { Button, Card, DatePicker, Empty, Form, Progress, Select, Space, Table, Tag, Typography, } from 'antd';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import ReactECharts from 'echarts-for-react';
import { useMemo, useState } from 'react';
import { getCertificationDashboard } from '../../api/certificationDashboard';
import { WeKnoraWidget } from '../../components/ai/WeKnoraWidget';
import { PermissionGate } from '../../directives/permission';
import './CertificationDataPanelPage.css';
const phaseOptions = [
    { label: '任务创建阶段', value: 'task_creation' },
    { label: '认定筹备阶段', value: 'preparation' },
    { label: '认定归档阶段', value: 'archive' },
    { label: '认定完成', value: 'certification_completed' },
];
const statusOptions = [
    { label: '进行中', value: 'in_progress' },
    { label: '待审核', value: 'pending_review' },
    { label: '已驳回', value: 'rejected' },
    { label: '已完成', value: 'completed' },
];
const dateModeOptions = [
    { label: '按创建日期', value: 'created' },
    { label: '按考试日期', value: 'exam' },
    { label: '按归档日期', value: 'archive' },
];
const phaseColors = ['#2f6bff', '#7c5cff', '#f59e0b', '#10b981'];
export function CertificationDataPanelPage() {
    const [form] = Form.useForm();
    const [query, setQuery] = useState(() => {
        const endDate = dayjs();
        return {
            startDate: endDate.subtract(11, 'month').startOf('month').format('YYYY-MM-DD'),
            endDate: endDate.format('YYYY-MM-DD'),
            dateMode: 'created',
        };
    });
    const dashboardQuery = useQuery({
        queryKey: ['certification-dashboard', query],
        queryFn: () => getCertificationDashboard(query),
    });
    const dashboard = dashboardQuery.data;
    const applyFilters = (values) => {
        setQuery({
            startDate: values.dateRange?.[0]?.format('YYYY-MM-DD'),
            endDate: values.dateRange?.[1]?.format('YYYY-MM-DD'),
            dateMode: values.dateMode || 'created',
            currentPhase: values.currentPhase,
            status: values.status,
            certificationType: values.certificationType,
            workTypeName: values.workTypeName,
            levelName: values.levelName,
        });
    };
    const resetFilters = () => {
        const endDate = dayjs();
        const dateRange = [endDate.subtract(11, 'month').startOf('month'), endDate];
        form.setFieldsValue({ dateRange, dateMode: 'created' });
        setQuery({
            startDate: dateRange[0].format('YYYY-MM-DD'),
            endDate: dateRange[1].format('YYYY-MM-DD'),
            dateMode: 'created',
        });
    };
    const phaseOption = useMemo(() => ({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        color: phaseColors,
        series: [{
                type: 'pie',
                radius: ['42%', '68%'],
                center: ['50%', '44%'],
                label: { formatter: '{b}\n{c}项' },
                data: (dashboard?.phaseDistribution || []).map((item) => ({
                    name: item.name,
                    value: item.taskCount,
                })),
            }],
    }), [dashboard?.phaseDistribution]);
    const workTypeOption = useMemo(() => {
        const values = (dashboard?.workTypeDistribution || []).slice(0, 10).reverse();
        return {
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            grid: { left: 16, right: 36, top: 12, bottom: 24, containLabel: true },
            xAxis: { type: 'value', name: '人数' },
            yAxis: { type: 'category', data: values.map((item) => item.name) },
            series: [{
                    type: 'bar',
                    data: values.map((item) => item.peopleCount),
                    itemStyle: { color: '#2f6bff', borderRadius: [0, 5, 5, 0] },
                    label: { show: true, position: 'right' },
                }],
        };
    }, [dashboard?.workTypeDistribution]);
    const workTypeLevelOption = useMemo(() => {
        const source = dashboard?.workTypeLevelDistribution || [];
        const workTypes = [...new Set(source.map((item) => item.workTypeName))].slice(0, 10);
        const levels = [...new Set(source.map((item) => item.levelName))];
        return {
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            legend: { bottom: 0 },
            grid: { left: 16, right: 24, top: 16, bottom: 56, containLabel: true },
            xAxis: { type: 'category', data: workTypes, axisLabel: { rotate: 20 } },
            yAxis: { type: 'value', name: '人数' },
            series: levels.map((level) => ({
                name: level,
                type: 'bar',
                stack: 'people',
                data: workTypes.map((workType) => (source.find((item) => item.workTypeName === workType && item.levelName === level)?.peopleCount || 0)),
            })),
        };
    }, [dashboard?.workTypeLevelDistribution]);
    const trendOption = useMemo(() => ({
        tooltip: { trigger: 'axis' },
        legend: { bottom: 0 },
        grid: { left: 16, right: 24, top: 16, bottom: 56, containLabel: true },
        xAxis: {
            type: 'category',
            data: (dashboard?.monthlyTrends || []).map((item) => item.month),
        },
        yAxis: [
            { type: 'value', name: '任务数' },
            { type: 'value', name: '归档人数' },
        ],
        series: [
            {
                name: '新增任务',
                type: 'bar',
                data: (dashboard?.monthlyTrends || []).map((item) => item.createdTaskCount),
                itemStyle: { color: '#93b4ff' },
            },
            {
                name: '归档任务',
                type: 'line',
                smooth: true,
                data: (dashboard?.monthlyTrends || []).map((item) => item.archivedTaskCount),
                itemStyle: { color: '#10b981' },
            },
            {
                name: '归档人数',
                type: 'line',
                smooth: true,
                yAxisIndex: 1,
                data: (dashboard?.monthlyTrends || []).map((item) => item.archivedPeopleCount),
                itemStyle: { color: '#f59e0b' },
            },
        ],
    }), [dashboard?.monthlyTrends]);
    const alertColumns = [
        { title: '任务编号', dataIndex: 'taskNo', width: 150, render: (value) => value || '-' },
        { title: '任务名称', dataIndex: 'taskName', width: 220, ellipsis: true, render: (value) => value || '-' },
        {
            title: '级别',
            dataIndex: 'level',
            width: 90,
            render: (value) => _jsx(Tag, { color: value === 'error' || value === 'high' ? 'error' : 'warning', children: value === 'error' || value === 'high' ? '高' : '关注' }),
        },
        { title: '提醒内容', dataIndex: 'description', ellipsis: true },
        { title: '计划日期', dataIndex: 'dueDate', width: 120, render: (value) => value || '-' },
    ];
    return (_jsx(PermissionGate, { code: "certification.dashboard.view", fallback: _jsx(Empty, { description: "\u6682\u65E0\u8BA4\u5B9A\u6570\u636E\u9762\u677F\u8BBF\u95EE\u6743\u9650" }), children: _jsxs("div", { className: "knowledge-page cert-dashboard", children: [_jsxs("div", { className: "page-heading knowledge-heading", children: [_jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(DashboardOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u8BA4\u5B9A\u6570\u636E\u9762\u677F" }), _jsx(Typography.Text, { type: "secondary", children: "\u6C47\u603B\u8BA4\u5B9A\u4EFB\u52A1\u3001\u5BF9\u8C61\u3001\u8D44\u6E90\u3001\u98CE\u9669\u4E0E\u5F52\u6863\u5408\u89C4\u6570\u636E" })] })] }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: dashboardQuery.isFetching, onClick: () => dashboardQuery.refetch(), children: "\u5237\u65B0" })] }), _jsx(Card, { className: "dashboard-filter-card", title: "\u7EDF\u8BA1\u7B5B\u9009", size: "small", children: _jsxs(Form, { form: form, layout: "inline", initialValues: {
                            dateRange: [dayjs(query.startDate), dayjs(query.endDate)],
                            dateMode: 'created',
                        }, onFinish: applyFilters, children: [_jsx(Form.Item, { name: "dateRange", label: "\u65E5\u671F\u8303\u56F4", children: _jsx(DatePicker.RangePicker, { allowClear: false }) }), _jsx(Form.Item, { name: "dateMode", label: "\u65E5\u671F\u53E3\u5F84", children: _jsx(Select, { style: { width: 130 }, options: dateModeOptions }) }), _jsx(Form.Item, { name: "currentPhase", label: "\u9636\u6BB5", children: _jsx(Select, { allowClear: true, style: { width: 150 }, options: phaseOptions }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { allowClear: true, style: { width: 120 }, options: statusOptions }) }), _jsx(Form.Item, { name: "certificationType", label: "\u8BA4\u5B9A\u7C7B\u578B", children: _jsx(Select, { allowClear: true, style: { width: 150 }, options: (dashboard?.filterOptions.certificationTypes || []).map((value) => ({ label: value, value })) }) }), _jsx(Form.Item, { name: "workTypeName", label: "\u5DE5\u79CD", children: _jsx(Select, { allowClear: true, showSearch: true, style: { width: 150 }, options: (dashboard?.filterOptions.workTypes || []).map((value) => ({ label: value, value })) }) }), _jsx(Form.Item, { name: "levelName", label: "\u7B49\u7EA7", children: _jsx(Select, { allowClear: true, style: { width: 120 }, options: (dashboard?.filterOptions.levels || []).map((value) => ({ label: value, value })) }) }), _jsx(Form.Item, { children: _jsxs(Space, { children: [_jsx(Button, { type: "primary", htmlType: "submit", children: "\u67E5\u8BE2" }), _jsx(Button, { onClick: resetFilters, children: "\u91CD\u7F6E" })] }) })] }) }), _jsxs("div", { className: "cert-dashboard-kpis", children: [_jsx(KpiCard, { label: "\u8BA4\u5B9A\u4EFB\u52A1\u603B\u6570", value: dashboard?.summary.totalTasks, foot: `进行中 ${dashboard?.summary.inProgressTasks || 0} 项` }), _jsx(KpiCard, { label: "\u5DF2\u5B8C\u6210\u4EFB\u52A1", value: dashboard?.summary.completedTasks, foot: `完成率 ${dashboard?.summary.taskCompletionRate || 0}%` }), _jsx(KpiCard, { label: "\u8BA1\u5212\u8BA4\u5B9A\u4EBA\u6570", value: dashboard?.summary.plannedPeopleCount, foot: `实际 ${dashboard?.summary.actualPeopleCount || 0} 人` }), _jsx(KpiCard, { label: "\u903E\u671F\u4EFB\u52A1", value: dashboard?.summary.overdueTasks, foot: `平均周期 ${dashboard?.summary.averageCycleDays || 0} 天`, danger: true }), _jsx(KpiCard, { label: "\u5DF2\u5F52\u6863\u4EFB\u52A1", value: dashboard?.summary.archivedTasks, foot: `待归档 ${dashboard?.summary.pendingArchiveTasks || 0} 项` }), _jsx(KpiCard, { label: "\u5F52\u6863\u5B8C\u6210\u7387", value: `${dashboard?.summary.archiveCompletionRate || 0}%`, foot: `材料完整率 ${dashboard?.summary.taskMaterialCompletionRate || 0}%` })] }), _jsxs("div", { className: "cert-dashboard-chart-grid", children: [_jsx(Card, { title: "\u4EFB\u52A1\u9636\u6BB5\u5206\u5E03", size: "small", children: _jsx(ReactECharts, { option: phaseOption, className: "cert-dashboard-chart" }) }), _jsx(Card, { title: "\u5DE5\u79CD\u8BA4\u5B9A\u4EBA\u6570\u6392\u540D", size: "small", children: _jsx(ReactECharts, { option: workTypeOption, className: "cert-dashboard-chart" }) }), _jsx(Card, { title: "\u5DE5\u79CD \u00D7 \u7B49\u7EA7\u4EBA\u6570", size: "small", children: _jsx(ReactECharts, { option: workTypeLevelOption, className: "cert-dashboard-chart" }) }), _jsx(Card, { title: "\u6708\u5EA6\u4EFB\u52A1\u4E0E\u5F52\u6863\u8D8B\u52BF", size: "small", children: _jsx(ReactECharts, { option: trendOption, className: "cert-dashboard-chart" }) })] }), _jsx(Card, { title: _jsxs(Space, { children: [_jsx(TeamOutlined, {}), "\u8D44\u6E90\u72B6\u6001"] }), size: "small", children: _jsx("div", { className: "cert-dashboard-resource-grid", children: (dashboard?.resources || []).map((resource) => (_jsxs("div", { className: "cert-dashboard-resource", children: [_jsxs("div", { className: "cert-dashboard-resource-title", children: [_jsx("span", { children: resource.name }), _jsxs(Tag, { color: resource.available > 0 ? 'success' : 'default', children: ["\u53EF\u7528 ", resource.available] })] }), _jsx("div", { className: "cert-dashboard-resource-total", children: resource.total }), _jsx(Typography.Text, { type: "secondary", children: "\u8D44\u6E90\u603B\u91CF" }), _jsxs("div", { className: "cert-dashboard-resource-meta", children: [_jsxs("span", { children: ["\u5360\u7528/\u975E\u7A7A\u95F2\uFF1A", resource.busy] }), _jsxs("span", { children: ["\u505C\u7528\uFF1A", resource.disabled] }), _jsxs("span", { children: ["30\u5929\u5185\u5230\u671F\uFF1A", resource.expiring] }), _jsx("span", { children: resource.capacity > 0 ? `可用容量：${resource.capacity}` : '容量：-' })] })] }, resource.code))) }) }), _jsx(Card, { title: _jsxs(Space, { children: [_jsx(CheckCircleOutlined, {}), "\u8D28\u91CF\u4E0E\u5408\u89C4"] }), size: "small", children: _jsxs("div", { className: "cert-dashboard-resource-grid", children: [_jsx(QualityCard, { label: "\u68C0\u67E5\u6E05\u5355\u5B8C\u6210\u7387", value: dashboard?.quality.checklistCompletionRate || 0 }), _jsx(QualityCard, { label: "\u8003\u751F\u6750\u6599\u5B8C\u6574\u7387", value: dashboard?.quality.candidateMaterialCompletionRate || 0 }), _jsx(KpiCard, { label: "\u672A\u5173\u95ED\u98CE\u9669", value: dashboard?.quality.openRiskCount, foot: `高风险任务 ${dashboard?.quality.highRiskTaskCount || 0} 项`, danger: true }), _jsx(KpiCard, { label: "\u5BFC\u5165\u5F02\u5E38", value: dashboard?.quality.importErrorCount, foot: "\u8003\u751F\u4E0E\u5F52\u6863\u6750\u6599\u5BFC\u5165\u5F02\u5E38", danger: true })] }) }), _jsxs("div", { className: "cert-dashboard-chart-grid", children: [_jsx(Card, { title: _jsxs(Space, { children: [_jsx(AlertOutlined, {}), "\u98CE\u9669\u4EFB\u52A1"] }), size: "small", children: _jsx(Table, { rowKey: (record) => `${record.taskId}-${record.alertType}-${record.description}`, columns: alertColumns, dataSource: dashboard?.riskAlerts || [], loading: dashboardQuery.isLoading, pagination: false, size: "small", scroll: { x: 760 } }) }), _jsx(Card, { title: _jsxs(Space, { children: [_jsx(FileDoneOutlined, {}), "\u5F52\u6863\u4E0E\u5408\u89C4\u63D0\u9192"] }), size: "small", children: _jsx(Table, { rowKey: (record) => `${record.taskId}-${record.alertType}-${record.description}`, columns: alertColumns, dataSource: dashboard?.complianceAlerts || [], loading: dashboardQuery.isLoading, pagination: false, size: "small", scroll: { x: 760 } }) })] }), _jsx(WeKnoraWidget, {})] }) }));
}
function KpiCard({ label, value, foot, danger = false, }) {
    return (_jsxs("div", { className: "cert-dashboard-kpi", children: [_jsx("div", { className: "cert-dashboard-kpi-label", children: label }), _jsx("div", { className: "cert-dashboard-kpi-value", style: danger ? { color: '#dc2626' } : undefined, children: value ?? 0 }), _jsx("div", { className: "cert-dashboard-kpi-foot", children: foot })] }));
}
function QualityCard({ label, value }) {
    return (_jsxs("div", { className: "cert-dashboard-resource", children: [_jsx("div", { className: "cert-dashboard-resource-title", children: label }), _jsx(Progress, { type: "dashboard", percent: value, size: 110 })] }));
}

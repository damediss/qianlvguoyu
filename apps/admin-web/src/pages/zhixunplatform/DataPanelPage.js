import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useRef, useState, useCallback } from 'react';
import { Typography, Table } from 'antd';
import { FolderOpenOutlined, TeamOutlined, DollarOutlined, ReadOutlined, SafetyCertificateOutlined, LineChartOutlined, ArrowUpOutlined, BarChartOutlined, PieChartOutlined, EnvironmentOutlined, TableOutlined, BankOutlined, ApartmentOutlined, BlockOutlined, LaptopOutlined, WarningOutlined, UserOutlined, SettingOutlined, BookOutlined, BulbOutlined, HomeOutlined, } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import * as echarts from 'echarts/core';
import { BarChart, PieChart as Pie, LineChart, ScatterChart, GaugeChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { WeKnoraWidget } from '../../components/ai/WeKnoraWidget';
import './DataPanelPage.css';
echarts.use([BarChart, Pie, LineChart, ScatterChart, GaugeChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);
// ============================================================
// 模拟数据
// ============================================================
const KPI_DATA = [
    { label: '总培训项目数', value: '186', trend: '+12% 同比', up: true, icon: FolderOpenOutlined, bg: '#E8F0FF', ic: '#2A6DFF' },
    { label: '参与总人次', value: '12,458', trend: '+8.3% 同比', up: true, icon: TeamOutlined, bg: '#ECFDF5', ic: '#10B981' },
    { label: '回款总额（万元）', value: '2,860', trend: '+15.2% 同比', up: true, icon: DollarOutlined, bg: '#FFF7ED', ic: '#F59E0B' },
    { label: '进行中班次', value: '24', trend: '3个本周开班', up: true, icon: ReadOutlined, bg: '#F5F3FF', ic: '#8B5CF6' },
    { label: '待认定人数', value: '342', trend: '本月需完成', up: null, icon: SafetyCertificateOutlined, bg: '#FEF2F2', ic: '#EF4444' },
    { label: '结业率', value: '94.6%', trend: '+2.1% 环比', up: true, icon: LineChartOutlined, bg: '#E0F2FE', ic: '#0284C7' },
];
const OCCUPATIONS = [
    { name: '电工', count: 128 }, { name: '焊工', count: 94 }, { name: '起重工', count: 46 },
    { name: '架子工', count: 33 }, { name: '安全员', count: 87 },
];
const TALENT_TYPES = [
    { name: '技能型人才', value: 36 }, { name: '管理型人才', value: 17 },
    { name: '技术型人才', value: 31 }, { name: '复合型', value: 16 },
];
const REGIONS = [
    { name: '南宁', pct: 32 }, { name: '桂林', pct: 18 }, { name: '柳州', pct: 15 },
    { name: '玉林', pct: 12 }, { name: '梧州', pct: 10 }, { name: '其他', pct: 13 },
];
const REGION_OCC = {
    cities: ['南宁', '桂林', '柳州', '玉林', '梧州'],
    occs: ['电工', '焊工', '起重工', '架子工', '安全员'],
    data: [
        [50, 30, 15, 11, 22], [23, 17, 8, 6, 14], [20, 14, 7, 5, 12],
        [16, 11, 6, 4, 10], [14, 10, 5, 3, 9],
    ],
};
const ENTERPRISE_LEVEL = {
    types: ['国有企业', '民营企业'], levels: ['初级', '中级', '高级', '专家级'],
    data: [[2050, 2387, 1705, 678], [2537, 1691, 1128, 282]],
};
const PROJECT_TYPES = [
    { name: '新员工培训', items: 42, people: 2100 }, { name: '晋升培训', items: 28, people: 1680 },
    { name: '资质认证', items: 56, people: 4200 }, { name: '安全培训', items: 38, people: 2280 },
    { name: '管理研修', items: 22, people: 1198 },
];
const EDUCATION = [
    { name: '大专及以下', value: 39 }, { name: '本科', value: 41 },
    { name: '硕士', value: 15 }, { name: '博士及其他', value: 5 },
];
const AGE_GROUPS = [
    { name: '20-30岁', value: 3240 }, { name: '31-40岁', value: 4580 },
    { name: '41-50岁', value: 3120 }, { name: '51岁以上', value: 1518 },
];
const YEAR_TREND = { years: ['2024', '2025', '2026'], values: [142, 168, 186] };
const SENIOR_PCT = [
    { name: '电工', pct: 28 }, { name: '焊工', pct: 22 }, { name: '起重工', pct: 35 },
    { name: '架子工', pct: 18 }, { name: '安全员', pct: 31 },
];
const RESOURCES = [
    { key: 'man', title: '人 · 人力资源', icon: UserOutlined, color: '#2A6DFF', items: ['专职讲师 35人', '兼职讲师 42人', '助教/班主任 26人'], util: '讲师利用率 87%' },
    { key: 'machine', title: '机 · 设备机器', icon: SettingOutlined, color: '#8B5CF6', items: ['实训设备 420台', '完好率 96%', '电工实训台/焊机/起重机模拟器等'], util: '' },
    { key: 'material', title: '料 · 教材物料', icon: BookOutlined, color: '#10B981', items: ['线上课程 128门', '实体教材发放 5600册', '课件季度更新率 92%'], util: '' },
    { key: 'method', title: '法 · 教学方法', icon: BulbOutlined, color: '#F59E0B', items: ['案例教学 40%', '实操模拟 35%', '理论讲授 25%'], util: '' },
    { key: 'env', title: '环 · 环境场地', icon: HomeOutlined, color: '#06B6D4', items: ['实训基地 3个', '总面积 12000㎡', '教室利用率 82%', '环境满意度 4.6/5'], util: '' },
];
const PLATFORM_ROWS = [
    { platform: '线上学习平台', team: '电工提升班', grad: 96, pass: 91, score: 86.5, cert: 89 },
    { platform: '线上学习平台', team: '安全管理研修班', grad: 94, pass: 88, score: 84.0, cert: 85 },
    { platform: '线下实训平台', team: '焊工特训班', grad: 98, pass: 96, score: 90.2, cert: 94 },
    { platform: '线下实训平台', team: '起重工实操班', grad: 92, pass: 87, score: 82.5, cert: 83 },
    { platform: '混合平台', team: '复合型人才班', grad: 97, pass: 93, score: 88.0, cert: 91 },
];
const PROJECTS = [
    { name: '2026年度电工高压特种作业认证培训', type: '资质认证', period: '2026.03-2026.06', risk: 'mid', milestones: [100, 75, 50], quality: '实操合格率 92%（目标95%）', cost: '预算58万 / 已用32万（55%）', riskText: '实操设备老化故障率上升', control: '增加备用设备与每日点检' },
    { name: '全区建筑架子工安全轮训（春季）', type: '安全培训', period: '2026.04-2026.07', risk: 'low', milestones: [100, 70, 70], quality: '理论均分86 / 安全规范91', cost: '预算35万 / 已用28万', riskText: '部分地市报名不足', control: '线上同步直播参训' },
    { name: '智能制造管理研修班', type: '管理研修', period: '2026.05-2026.09', risk: 'low', milestones: [100, 100, 85], quality: '往期满意度4.8/5', cost: '预算120万 / 已用65万', riskText: '高层管理者时间冲突出勤率下降', control: '录制课程+补学机制' },
    { name: '焊工技师晋升认证项目', type: '晋升培训', period: '2026.02-2026.08', risk: 'high', milestones: [100, 70, 70], quality: '实操通过率88%（预期92%）', cost: '预算72万 / 已用56万（材料超支8%）', riskText: '焊材浪费严重', control: '定额领料+奖惩制度' },
];
const RISK_MATRIX = [
    { name: '电工认证', impact: 3, prob: 2 }, { name: '架子工轮训', impact: 2, prob: 2 },
    { name: '管理研修', impact: 2, prob: 3 }, { name: '焊工晋升', impact: 4, prob: 3 },
];
const riskLabelMap = { high: '高风险', mid: '中风险', low: '低风险' };
// 导航配置
const NAV_ITEMS = [
    { key: 'sec-talent', label: '人才数据', icon: TeamOutlined, color: '#2A6DFF' },
    { key: 'sec-resource', label: '资源数据', icon: BlockOutlined, color: '#8B5CF6' },
    { key: 'sec-platform', label: '平台数据', icon: LaptopOutlined, color: '#10B981' },
    { key: 'sec-project', label: '项目数据', icon: WarningOutlined, color: '#F59E0B' },
];
// ============================================================
// 公共样式
// ============================================================
const S = {
    kpiIcon: (bg, ic) => ({ width: 44, height: 44, borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, background: bg, color: ic }),
    kpiVal: { fontSize: 28, fontWeight: 700, letterSpacing: '-.5px' },
    kpiLabel: { fontSize: 13, color: '#64748b', marginTop: 4 },
    kpiTrend: (up) => ({ fontSize: 12, fontWeight: 600, marginTop: 8, display: 'flex', alignItems: 'center', gap: 4, color: up === true ? '#10b981' : '#64748b' }),
    kpiTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 },
    sectionHead: { display: 'flex', alignItems: 'center', gap: 12, marginBottom: 22, paddingBottom: 16, borderBottom: '2px solid #f5f7fa' },
    iconWrap: (bg, ic) => ({ width: 48, height: 48, borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, background: bg, color: ic }),
    subCardTitle: { fontSize: 14, fontWeight: 600, marginBottom: 14, color: '#1a2332', display: 'flex', alignItems: 'center', gap: 8 },
    chart: { width: '100%', height: 320, minHeight: 260 },
    chartSm: { width: '100%', height: 240, minHeight: 200 },
    chartXs: { width: '100%', height: 200, minHeight: 160 },
    bigNum: { fontSize: 26, fontWeight: 700, color: '#2A6DFF' },
    subText: { fontSize: 12, color: '#64748b', marginTop: 6 },
    resCardTitle: { fontSize: 15, margin: '0 0 12px', display: 'flex', alignItems: 'center', gap: 8 },
    riskTag: (risk) => ({ display: 'inline-block', padding: '3px 10px', borderRadius: 6, fontSize: 11, fontWeight: 600, marginLeft: 8, background: { high: '#fee2e2', mid: '#fef3c7', low: '#d1fae5' }[risk], color: { high: '#dc2626', mid: '#d97706', low: '#059669' }[risk] }),
    projMeta: { display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '10px 20px', fontSize: 13, marginBottom: 12 },
    milestoneBar: { display: 'flex', gap: 4, margin: '10px 0' },
    riskDesc: { fontSize: 12, color: '#64748b', padding: 10, background: '#fff', borderRadius: 8, marginTop: 8 },
    progressRow: { marginBottom: 12 },
    progressLabel: { fontSize: 12, display: 'flex', justifyContent: 'space-between', marginBottom: 4, color: '#64748b' },
    progressBar: { height: 8, background: '#e2e8f0', borderRadius: 4, overflow: 'hidden' },
    progressFill: (pct) => ({ display: 'block', height: '100%', background: 'linear-gradient(90deg, #2A6DFF, #5B8FFF)', borderRadius: 4, width: `${pct}%` }),
    pctCell: { display: 'flex', alignItems: 'center', gap: 8 },
    pctBar: { flex: 1, height: 6, background: '#e2e8f0', borderRadius: 3, maxWidth: 80 },
    pctFill: (v) => ({ display: 'block', height: '100%', background: '#2A6DFF', borderRadius: 3, width: `${v}%` }),
    mb20: { marginBottom: 20 },
    mt20: { marginTop: 20 },
};
// ============================================================
// ECharts 配置
// ============================================================
const chartOccupationOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: { type: 'value', name: '人数' },
    yAxis: { type: 'category', data: OCCUPATIONS.map((o) => o.name) },
    series: [{ type: 'bar', data: OCCUPATIONS.map((o) => o.count), itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#5b8fff' }, { offset: 1, color: '#2A6DFF' }]) }, label: { show: true, position: 'right', formatter: '{c}人' } }],
};
const chartTalentTypeOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}% ({d}%)' },
    legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['42%', '68%'], center: ['50%', '45%'], data: TALENT_TYPES.map((t) => ({ name: t.name, value: t.value })), label: { formatter: '{b}\n{d}%' }, itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 } }],
    color: ['#2A6DFF', '#10B981', '#F59E0B', '#8B5CF6'],
};
const chartRegionOption = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{ type: 'pie', radius: '65%', center: ['40%', '50%'], data: REGIONS.map((r) => ({ name: r.name, value: r.pct })), label: { formatter: '{b} {c}%' }, emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,.2)' } } }],
    color: ['#2A6DFF', '#5b8fff', '#10B981', '#F59E0B', '#8B5CF6', '#94A3B8'],
};
const chartYearTrendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['培训项目数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: YEAR_TREND.years },
    yAxis: { type: 'value', name: '项' },
    series: [{ name: '培训项目数', type: 'line', smooth: true, data: YEAR_TREND.values, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(42,109,255,.3)' }, { offset: 1, color: 'rgba(42,109,255,.02)' }]) }, itemStyle: { color: '#2A6DFF' }, lineStyle: { width: 3 } }],
};
const chartEnterpriseLevelOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ENTERPRISE_LEVEL.levels, bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: ENTERPRISE_LEVEL.types },
    yAxis: { type: 'value', name: '人数' },
    series: ENTERPRISE_LEVEL.levels.map((lv, i) => ({ name: lv, type: 'bar', stack: 'total', data: ENTERPRISE_LEVEL.data.map((row) => row[i]), emphasis: { focus: 'series' } })),
    color: ['#94A3B8', '#5b8fff', '#2A6DFF', '#1e40af'],
};
const chartProjectTypeOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['项目数', '人才人次'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: PROJECT_TYPES.map((p) => p.name), axisLabel: { rotate: 20 } },
    yAxis: [{ type: 'value', name: '项目数' }, { type: 'value', name: '人次', position: 'right' }],
    series: [
        { name: '项目数', type: 'bar', data: PROJECT_TYPES.map((p) => p.items), itemStyle: { color: '#2A6DFF' } },
        { name: '人才人次', type: 'line', yAxisIndex: 1, data: PROJECT_TYPES.map((p) => p.people), itemStyle: { color: '#10B981' }, lineStyle: { width: 3 } },
    ],
};
const chartEducationOption = {
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '60%', data: EDUCATION.map((e) => ({ name: e.name, value: e.value })), label: { formatter: '{b} {c}%' } }],
    color: ['#2A6DFF', '#5b8fff', '#10B981', '#8B5CF6'],
};
const chartAgeOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: AGE_GROUPS.map((a) => a.name) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: AGE_GROUPS.map((a) => a.value), itemStyle: { color: '#10B981', borderRadius: [6, 6, 0, 0] } }],
};
const chartTeacherUtilOption = {
    tooltip: { formatter: '{b}: {c}%' },
    series: [{ type: 'gauge', startAngle: 200, endAngle: -20, min: 0, max: 100, progress: { show: true, width: 14 }, axisLine: { lineStyle: { width: 14 } }, detail: { valueAnimation: true, formatter: '{value}%', fontSize: 22, offsetCenter: [0, '20%'] }, data: [{ value: 87, name: '讲师利用率' }] }],
};
const chartMethodOption = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '65%'], data: [{ name: '案例教学', value: 40 }, { name: '实操模拟', value: 35 }, { name: '理论讲授', value: 25 }], label: { formatter: '{b} {d}%' } }],
    color: ['#2A6DFF', '#10B981', '#F59E0B'],
};
function makePlatformCompareOption() {
    const online = PLATFORM_ROWS.filter((r) => r.platform.includes('线上'));
    const offline = PLATFORM_ROWS.filter((r) => r.platform.includes('线下'));
    const mix = PLATFORM_ROWS.filter((r) => r.platform.includes('混合'));
    const avgG = (arr, k) => (arr.length ? Math.round((arr.reduce((s, r) => s + r[k], 0) / arr.length) * 10) / 10 : 0);
    return {
        tooltip: { trigger: 'axis' },
        legend: { data: ['结业率', '考核通过率', '平均成绩', '证书获取率'], bottom: 0 },
        grid: { left: '3%', right: '4%', bottom: '18%', containLabel: true },
        xAxis: { type: 'category', data: ['线上学习平台', '线下实训平台', '混合平台'] },
        yAxis: { type: 'value' },
        series: [
            { name: '结业率', type: 'bar', data: [avgG(online, 'grad'), avgG(offline, 'grad'), avgG(mix, 'grad')] },
            { name: '考核通过率', type: 'bar', data: [avgG(online, 'pass'), avgG(offline, 'pass'), avgG(mix, 'pass')] },
            { name: '平均成绩', type: 'bar', data: [avgG(online, 'score'), avgG(offline, 'score'), avgG(mix, 'score')] },
            { name: '证书获取率', type: 'bar', data: [avgG(online, 'cert'), avgG(offline, 'cert'), avgG(mix, 'cert')] },
        ],
        color: ['#2A6DFF', '#10B981', '#F59E0B', '#8B5CF6'],
    };
}
const chartRiskMatrixOption = {
    tooltip: { formatter: (p) => `${p.data?.[2] || ''}<br/>影响:${p.data?.[0] || 0} 概率:${p.data?.[1] || 0}` },
    grid: { left: '12%', right: '8%', bottom: '12%', top: '8%' },
    xAxis: { type: 'value', name: '发生概率', min: 0, max: 5, splitLine: { show: true } },
    yAxis: { type: 'value', name: '影响程度', min: 0, max: 5, splitLine: { show: true } },
    series: [{
            type: 'scatter', symbolSize: (v) => (v[0] + v[1]) * 8,
            data: RISK_MATRIX.map((r) => ({ value: [r.prob, r.impact, r.name], itemStyle: { color: (r.prob + r.impact) >= 6 ? '#ef4444' : (r.prob + r.impact) >= 4 ? '#f59e0b' : '#10b981' } })),
            label: { show: true, formatter: (p) => p.data?.[2], position: 'top', fontSize: 11 },
        }],
};
// ============================================================
// 各地市×工种表格
// ============================================================
function RegionOccTable() {
    const { cities, occs, data } = REGION_OCC;
    const columns = [
        { title: '地市', dataIndex: 'city', key: 'city', fixed: 'left', width: 80, render: (v) => _jsx("strong", { children: v }) },
        ...occs.map((o) => ({ title: o, dataIndex: o, key: o, width: 80 })),
        { title: '合计', dataIndex: 'total', key: 'total', width: 80, render: (v) => _jsx("strong", { children: v }) },
    ];
    const tableData = cities.map((city, i) => {
        const row = { city };
        occs.forEach((o, j) => { row[o] = data[i][j]; });
        row.total = data[i].reduce((a, b) => a + b, 0);
        return row;
    });
    return _jsx(Table, { columns: columns, dataSource: tableData, rowKey: "city", pagination: false, size: "small", bordered: true });
}
// ============================================================
// 主页面
// ============================================================
export function DataPanelPage() {
    const chartRefs = useRef([]);
    const [activeNav, setActiveNav] = useState('sec-talent');
    // 滚动监听：自动高亮当前可视板块
    useEffect(() => {
        const ids = NAV_ITEMS.map((n) => n.key);
        const observer = new IntersectionObserver((entries) => {
            const visible = entries.filter((e) => e.isIntersecting);
            if (visible.length > 0) {
                setActiveNav(visible[0].target.id);
            }
        }, { rootMargin: '-80px 0px -60% 0px' });
        ids.forEach((id) => {
            const el = document.getElementById(id);
            if (el)
                observer.observe(el);
        });
        return () => observer.disconnect();
    }, []);
    // 点击导航平滑滚动
    const scrollToSection = useCallback((id) => {
        setActiveNav(id);
        document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, []);
    useEffect(() => {
        const timer = setTimeout(() => chartRefs.current.forEach((c) => c?.getEchartsInstance()?.resize()), 350);
        return () => clearTimeout(timer);
    }, []);
    return (_jsxs("div", { className: "data-panel-page", children: [_jsx("div", { className: "dp-kpi-grid", children: KPI_DATA.map((k, i) => (_jsxs("div", { className: "dp-kpi-card", children: [_jsx("div", { style: S.kpiTop, children: _jsx("div", { style: S.kpiIcon(k.bg, k.ic), children: _jsx(k.icon, {}) }) }), _jsx("div", { style: S.kpiVal, children: k.value }), _jsx("div", { style: S.kpiLabel, children: k.label }), _jsxs("div", { style: S.kpiTrend(k.up), children: [k.up === true && _jsx(ArrowUpOutlined, {}), " ", k.trend] })] }, i))) }), _jsx("nav", { className: "dp-nav-bar", children: NAV_ITEMS.map((item) => (_jsxs("button", { className: `dp-nav-btn${activeNav === item.key ? ' active' : ''}`, onClick: () => scrollToSection(item.key), children: [_jsx(item.icon, { style: { color: activeNav === item.key ? '#fff' : item.color } }), item.label] }, item.key))) }), _jsxs("section", { id: "sec-talent", className: "dp-section", children: [_jsxs("div", { style: S.sectionHead, children: [_jsx("div", { style: S.iconWrap('#E8F0FF', '#2A6DFF'), children: _jsx(TeamOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u4EBA\u624D\u6570\u636E\u677F\u5757" }), _jsx(Typography.Text, { type: "secondary", children: "\u533A\u57DF\u3001\u4F01\u4E1A\u3001\u9879\u76EE\u3001\u804C\u4E1A\u3001\u5DE5\u79CD\u3001\u7EA7\u522B\u591A\u7EF4\u5EA6\u4EBA\u624D\u753B\u50CF" })] })] }), _jsxs("div", { className: "dp-grid-23", style: S.mb20, children: [_jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(BarChartOutlined, {}), " \u804C\u4E1A\u5DE5\u79CD\u5206\u5E03"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[0] = e; }, option: chartOccupationOption, style: S.chart })] }), _jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(PieChartOutlined, {}), " \u4EBA\u624D\u7C7B\u578B\u7ED3\u6784"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[1] = e; }, option: chartTalentTypeOption, style: S.chartSm })] })] }), _jsxs("div", { className: "dp-grid-2", style: S.mb20, children: [_jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(EnvironmentOutlined, {}), " \u5730\u57DF\u5206\u5E03\uFF08\u5E7F\u897F\uFF09"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[2] = e; }, option: chartRegionOption, style: S.chartSm })] }), _jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(LineChartOutlined, {}), " \u57F9\u8BAD\u9879\u76EE\u6570\u5E74\u5EA6\u5BF9\u6BD4"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[3] = e; }, option: chartYearTrendOption, style: S.chartSm })] })] }), _jsxs("div", { className: "dp-sub-card", style: S.mb20, children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(TableOutlined, {}), " \u5404\u5730\u5E02 \u00D7 \u5DE5\u79CD\u4EBA\u624D\u6570\u91CF"] }), _jsx(RegionOccTable, {})] }), _jsxs("div", { className: "dp-grid-2", style: S.mb20, children: [_jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(BankOutlined, {}), " \u4F01\u4E1A\u6027\u8D28 \u00D7 \u4EBA\u624D\u7EA7\u522B"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[4] = e; }, option: chartEnterpriseLevelOption, style: S.chart })] }), _jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(ApartmentOutlined, {}), " \u9879\u76EE\u7C7B\u578B\u4EBA\u624D\u4EBA\u6B21"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[5] = e; }, option: chartProjectTypeOption, style: S.chart })] })] }), _jsxs("div", { className: "dp-grid-3", children: [_jsxs("div", { className: "dp-sub-card", children: [_jsx("div", { style: S.subCardTitle, children: "\u5B66\u5386\u7ED3\u6784" }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[6] = e; }, option: chartEducationOption, style: S.chartXs })] }), _jsxs("div", { className: "dp-sub-card", children: [_jsx("div", { style: S.subCardTitle, children: "\u5E74\u9F84\u5206\u5E03" }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[7] = e; }, option: chartAgeOption, style: S.chartXs })] }), _jsxs("div", { className: "dp-sub-card", children: [_jsx("div", { style: S.subCardTitle, children: "\u5404\u5DE5\u79CD\u9AD8\u7EA7\u4EBA\u624D\u5360\u6BD4" }), _jsx("div", { style: { marginTop: 8 }, children: SENIOR_PCT.map((s, i) => (_jsxs("div", { style: S.progressRow, children: [_jsxs("label", { style: S.progressLabel, children: [_jsx("span", { children: s.name }), _jsxs("span", { children: [s.pct, "%"] })] }), _jsx("div", { style: S.progressBar, children: _jsx("span", { style: S.progressFill(s.pct) }) })] }, i))) })] })] })] }), _jsxs("section", { id: "sec-resource", className: "dp-section", children: [_jsxs("div", { style: S.sectionHead, children: [_jsx("div", { style: S.iconWrap('#F5F3FF', '#8B5CF6'), children: _jsx(BlockOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u8D44\u6E90\u6570\u636E\u677F\u5757" }), _jsx(Typography.Text, { type: "secondary", children: "\u4EBA\u673A\u6599\u6CD5\u73AF\u4E94\u7C7B\u57F9\u8BAD\u8D44\u6E90\u72B6\u6001\u4E0E\u5229\u7528\u7387" })] })] }), _jsx("div", { className: "dp-resource-grid", children: RESOURCES.map((r) => (_jsxs("div", { className: "dp-res-card", style: { borderLeft: `4px solid ${r.color}` }, children: [_jsxs("h4", { style: S.resCardTitle, children: [_jsx(r.icon, { style: { color: r.color } }), " ", r.title] }), _jsx("div", { style: S.bigNum, children: r.items[0].split(' ').slice(-1)[0] || '—' }), r.items.slice(1).map((t, j) => _jsx("div", { style: S.subText, children: t }, j)), r.util && _jsx("div", { style: { ...S.subText, marginTop: 10, fontWeight: 600, color: '#2A6DFF' }, children: r.util })] }, r.key))) }), _jsxs("div", { className: "dp-grid-2", style: S.mt20, children: [_jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(ReadOutlined, {}), " \u8BB2\u5E08\u5229\u7528\u7387"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[8] = e; }, option: chartTeacherUtilOption, style: S.chartSm })] }), _jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(BookOutlined, {}), " \u6559\u5B66\u65B9\u6CD5\u5206\u5E03"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[9] = e; }, option: chartMethodOption, style: S.chartSm })] })] })] }), _jsxs("section", { id: "sec-platform", className: "dp-section", children: [_jsxs("div", { style: S.sectionHead, children: [_jsx("div", { style: S.iconWrap('#ECFDF5', '#10B981'), children: _jsx(LaptopOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u5E73\u53F0\u6570\u636E\u677F\u5757" }), _jsx(Typography.Text, { type: "secondary", children: "\u4E0D\u540C\u5E73\u53F0\u3001\u4E0D\u540C\u57F9\u8BAD\u73ED\u7EC4\u6210\u679C\u6307\u6807\u5BF9\u6BD4" })] })] }), _jsxs("div", { className: "dp-sub-card", style: S.mb20, children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(TableOutlined, {}), " \u5E73\u53F0 \u00D7 \u73ED\u7EC4\u6210\u679C\u660E\u7EC6"] }), _jsx(Table, { dataSource: PLATFORM_ROWS.map((r, i) => ({ ...r, key: i })), pagination: false, size: "small", bordered: true, columns: [
                                    { title: '平台', dataIndex: 'platform', key: 'platform' },
                                    { title: '班组名称', dataIndex: 'team', key: 'team', render: (v) => _jsx("strong", { children: v }) },
                                    { title: '结业率', dataIndex: 'grad', key: 'grad', render: (v) => _jsxs("div", { style: S.pctCell, children: [v, "%", _jsx("div", { style: S.pctBar, children: _jsx("span", { style: S.pctFill(v) }) })] }) },
                                    { title: '考核通过率', dataIndex: 'pass', key: 'pass', render: (v) => _jsxs("div", { style: S.pctCell, children: [v, "%", _jsx("div", { style: S.pctBar, children: _jsx("span", { style: S.pctFill(v) }) })] }) },
                                    { title: '平均成绩', dataIndex: 'score', key: 'score' },
                                    { title: '证书获取率', dataIndex: 'cert', key: 'cert', render: (v) => _jsxs("div", { style: S.pctCell, children: [v, "%", _jsx("div", { style: S.pctBar, children: _jsx("span", { style: S.pctFill(v) }) })] }) },
                                ] })] }), _jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(BarChartOutlined, {}), " \u5404\u5E73\u53F0\u5E73\u5747\u6210\u679C\u5BF9\u6BD4"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                    chartRefs.current[10] = e; }, option: makePlatformCompareOption(), style: S.chart })] })] }), _jsxs("section", { id: "sec-project", className: "dp-section", children: [_jsxs("div", { style: S.sectionHead, children: [_jsx("div", { style: S.iconWrap('#FFF7ED', '#F59E0B'), children: _jsx(WarningOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u9879\u76EE\u6570\u636E\u677F\u5757" }), _jsx(Typography.Text, { type: "secondary", children: "\u91CD\u70B9\u9879\u76EE\u91CC\u7A0B\u3001\u8D28\u91CF\u3001\u6210\u672C\u4E0E\u98CE\u9669\u70B9\u7BA1\u63A7" })] })] }), _jsxs("div", { className: "dp-grid-32", style: S.mb20, children: [_jsx("div", { children: PROJECTS.map((p, i) => {
                                    const ms = p.milestones.map((v, j) => {
                                        const bg = v >= 100 ? '#10b981' : `linear-gradient(90deg, #10b981 ${v}%, #e2e8f0 ${v}%)`;
                                        return _jsx("div", { style: { flex: 1, height: 6, borderRadius: 3, background: bg, overflow: 'hidden', position: 'relative' }, title: ['课程开发', '培训实施', '考试筹备'][j] + ` ${v}%` }, j);
                                    });
                                    return (_jsxs("div", { className: "dp-project-card", children: [_jsxs("h4", { style: { fontSize: 15, fontWeight: 600, margin: '0 0 12px' }, children: [p.name, _jsx("span", { style: S.riskTag(p.risk), children: riskLabelMap[p.risk] })] }), _jsxs("div", { style: S.projMeta, children: [_jsxs("div", { children: [_jsx("span", { style: { color: '#64748b' }, children: "\u7C7B\u578B\uFF1A" }), p.type] }), _jsxs("div", { children: [_jsx("span", { style: { color: '#64748b' }, children: "\u5468\u671F\uFF1A" }), p.period] }), _jsxs("div", { children: [_jsx("span", { style: { color: '#64748b' }, children: "\u8D28\u91CF\uFF1A" }), p.quality] }), _jsxs("div", { children: [_jsx("span", { style: { color: '#64748b' }, children: "\u6210\u672C\uFF1A" }), p.cost] })] }), _jsx("div", { style: { fontSize: 12, color: '#64748b', marginBottom: 4 }, children: "\u91CC\u7A0B\u7891\u8FDB\u5EA6" }), _jsx("div", { style: S.milestoneBar, children: ms }), _jsxs("div", { style: S.riskDesc, children: [_jsx("strong", { children: "\u98CE\u9669\uFF1A" }), p.riskText, " \u00B7 ", _jsx("strong", { children: "\u7BA1\u63A7\uFF1A" }), p.control] })] }, i));
                                }) }), _jsxs("div", { className: "dp-sub-card", children: [_jsxs("div", { style: S.subCardTitle, children: [_jsx(WarningOutlined, {}), " \u9879\u76EE\u98CE\u9669\u77E9\u9635\u6982\u89C8"] }), _jsx(ReactECharts, { ref: (e) => { if (e)
                                            chartRefs.current[11] = e; }, option: chartRiskMatrixOption, style: S.chart })] })] })] }), _jsx("div", { style: { textAlign: 'center', fontSize: 12, color: '#64748b', marginTop: 12 }, children: "\u6570\u636E\u66F4\u65B0\u81F3 2026-05 \u00B7 \u6F14\u793A\u6570\u636E \u00B7 ECharts \u4EA4\u4E92\u56FE\u8868" }), _jsx(WeKnoraWidget, {})] }));
}

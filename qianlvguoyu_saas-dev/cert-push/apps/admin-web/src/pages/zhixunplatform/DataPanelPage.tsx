import { useEffect, useRef, useState, useCallback } from 'react';
import { Typography, Table, Anchor } from 'antd';
import {
  FolderOpenOutlined,
  TeamOutlined,
  DollarOutlined,
  ReadOutlined,
  SafetyCertificateOutlined,
  LineChartOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  PieChartOutlined,
  EnvironmentOutlined,
  TableOutlined,
  BankOutlined,
  ApartmentOutlined,
  BlockOutlined,
  LaptopOutlined,
  WarningOutlined,
  UserOutlined,
  SettingOutlined,
  BookOutlined,
  BulbOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import * as echarts from 'echarts/core';
import { BarChart, PieChart as Pie, LineChart, ScatterChart, GaugeChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
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
  { name: '2026年度电工高压特种作业认证培训', type: '资质认证', period: '2026.03-2026.06', risk: 'mid' as const, milestones: [100, 75, 50], quality: '实操合格率 92%（目标95%）', cost: '预算58万 / 已用32万（55%）', riskText: '实操设备老化故障率上升', control: '增加备用设备与每日点检' },
  { name: '全区建筑架子工安全轮训（春季）', type: '安全培训', period: '2026.04-2026.07', risk: 'low' as const, milestones: [100, 70, 70], quality: '理论均分86 / 安全规范91', cost: '预算35万 / 已用28万', riskText: '部分地市报名不足', control: '线上同步直播参训' },
  { name: '智能制造管理研修班', type: '管理研修', period: '2026.05-2026.09', risk: 'low' as const, milestones: [100, 100, 85], quality: '往期满意度4.8/5', cost: '预算120万 / 已用65万', riskText: '高层管理者时间冲突出勤率下降', control: '录制课程+补学机制' },
  { name: '焊工技师晋升认证项目', type: '晋升培训', period: '2026.02-2026.08', risk: 'high' as const, milestones: [100, 70, 70], quality: '实操通过率88%（预期92%）', cost: '预算72万 / 已用56万（材料超支8%）', riskText: '焊材浪费严重', control: '定额领料+奖惩制度' },
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
  kpiIcon: (bg: string, ic: string) => ({ width: 44, height: 44, borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, background: bg, color: ic }) as React.CSSProperties,
  kpiVal: { fontSize: 28, fontWeight: 700, letterSpacing: '-.5px' } as React.CSSProperties,
  kpiLabel: { fontSize: 13, color: '#64748b', marginTop: 4 } as React.CSSProperties,
  kpiTrend: (up: boolean | null) => ({ fontSize: 12, fontWeight: 600, marginTop: 8, display: 'flex', alignItems: 'center', gap: 4, color: up === true ? '#10b981' : '#64748b' }) as React.CSSProperties,
  kpiTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 } as React.CSSProperties,
  sectionHead: { display: 'flex', alignItems: 'center', gap: 12, marginBottom: 22, paddingBottom: 16, borderBottom: '2px solid #f5f7fa' } as React.CSSProperties,
  iconWrap: (bg: string, ic: string) => ({ width: 48, height: 48, borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, background: bg, color: ic }) as React.CSSProperties,
  subCardTitle: { fontSize: 14, fontWeight: 600, marginBottom: 14, color: '#1a2332', display: 'flex', alignItems: 'center', gap: 8 } as React.CSSProperties,
  chart: { width: '100%', height: 320, minHeight: 260 } as React.CSSProperties,
  chartSm: { width: '100%', height: 240, minHeight: 200 } as React.CSSProperties,
  chartXs: { width: '100%', height: 200, minHeight: 160 } as React.CSSProperties,
  bigNum: { fontSize: 26, fontWeight: 700, color: '#2A6DFF' } as React.CSSProperties,
  subText: { fontSize: 12, color: '#64748b', marginTop: 6 } as React.CSSProperties,
  resCardTitle: { fontSize: 15, margin: '0 0 12px', display: 'flex', alignItems: 'center', gap: 8 } as React.CSSProperties,
  riskTag: (risk: string) => ({ display: 'inline-block', padding: '3px 10px', borderRadius: 6, fontSize: 11, fontWeight: 600, marginLeft: 8, background: { high: '#fee2e2', mid: '#fef3c7', low: '#d1fae5' }[risk], color: { high: '#dc2626', mid: '#d97706', low: '#059669' }[risk] }) as React.CSSProperties,
  projMeta: { display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '10px 20px', fontSize: 13, marginBottom: 12 } as React.CSSProperties,
  milestoneBar: { display: 'flex', gap: 4, margin: '10px 0' } as React.CSSProperties,
  riskDesc: { fontSize: 12, color: '#64748b', padding: 10, background: '#fff', borderRadius: 8, marginTop: 8 } as React.CSSProperties,
  progressRow: { marginBottom: 12 } as React.CSSProperties,
  progressLabel: { fontSize: 12, display: 'flex', justifyContent: 'space-between', marginBottom: 4, color: '#64748b' } as React.CSSProperties,
  progressBar: { height: 8, background: '#e2e8f0', borderRadius: 4, overflow: 'hidden' } as React.CSSProperties,
  progressFill: (pct: number) => ({ display: 'block', height: '100%', background: 'linear-gradient(90deg, #2A6DFF, #5B8FFF)', borderRadius: 4, width: `${pct}%` }) as React.CSSProperties,
  pctCell: { display: 'flex', alignItems: 'center', gap: 8 } as React.CSSProperties,
  pctBar: { flex: 1, height: 6, background: '#e2e8f0', borderRadius: 3, maxWidth: 80 } as React.CSSProperties,
  pctFill: (v: number) => ({ display: 'block', height: '100%', background: '#2A6DFF', borderRadius: 3, width: `${v}%` }) as React.CSSProperties,
  mb20: { marginBottom: 20 } as React.CSSProperties,
  mt20: { marginTop: 20 } as React.CSSProperties,
};

// ============================================================
// ECharts 配置
// ============================================================
const chartOccupationOption = {
  tooltip: { trigger: 'axis' as const, axisPointer: { type: 'shadow' as const } },
  grid: { left: '3%', right: '8%', bottom: '3%', top: '8%', containLabel: true },
  xAxis: { type: 'value' as const, name: '人数' },
  yAxis: { type: 'category' as const, data: OCCUPATIONS.map((o) => o.name) },
  series: [{ type: 'bar', data: OCCUPATIONS.map((o) => o.count), itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#5b8fff' }, { offset: 1, color: '#2A6DFF' }]) }, label: { show: true, position: 'right' as const, formatter: '{c}人' } }],
};

const chartTalentTypeOption = {
  tooltip: { trigger: 'item' as const, formatter: '{b}: {c}% ({d}%)' },
  legend: { bottom: 0 },
  series: [{ type: 'pie', radius: ['42%', '68%'], center: ['50%', '45%'], data: TALENT_TYPES.map((t) => ({ name: t.name, value: t.value })), label: { formatter: '{b}\n{d}%' }, itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 } }],
  color: ['#2A6DFF', '#10B981', '#F59E0B', '#8B5CF6'],
};

const chartRegionOption = {
  tooltip: { trigger: 'item' as const },
  legend: { orient: 'vertical' as const, right: 10, top: 'center' },
  series: [{ type: 'pie', radius: '65%', center: ['40%', '50%'], data: REGIONS.map((r) => ({ name: r.name, value: r.pct })), label: { formatter: '{b} {c}%' }, emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,.2)' } } }],
  color: ['#2A6DFF', '#5b8fff', '#10B981', '#F59E0B', '#8B5CF6', '#94A3B8'],
};

const chartYearTrendOption = {
  tooltip: { trigger: 'axis' as const },
  legend: { data: ['培训项目数'] },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category' as const, data: YEAR_TREND.years },
  yAxis: { type: 'value' as const, name: '项' },
  series: [{ name: '培训项目数', type: 'line', smooth: true, data: YEAR_TREND.values, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(42,109,255,.3)' }, { offset: 1, color: 'rgba(42,109,255,.02)' }]) }, itemStyle: { color: '#2A6DFF' }, lineStyle: { width: 3 } }],
};

const chartEnterpriseLevelOption = {
  tooltip: { trigger: 'axis' as const, axisPointer: { type: 'shadow' as const } },
  legend: { data: ENTERPRISE_LEVEL.levels, bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  xAxis: { type: 'category' as const, data: ENTERPRISE_LEVEL.types },
  yAxis: { type: 'value' as const, name: '人数' },
  series: ENTERPRISE_LEVEL.levels.map((lv, i) => ({ name: lv, type: 'bar' as const, stack: 'total', data: ENTERPRISE_LEVEL.data.map((row) => row[i]), emphasis: { focus: 'series' as const } })),
  color: ['#94A3B8', '#5b8fff', '#2A6DFF', '#1e40af'],
};

const chartProjectTypeOption = {
  tooltip: { trigger: 'axis' as const },
  legend: { data: ['项目数', '人才人次'] },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category' as const, data: PROJECT_TYPES.map((p) => p.name), axisLabel: { rotate: 20 } },
  yAxis: [{ type: 'value' as const, name: '项目数' }, { type: 'value' as const, name: '人次', position: 'right' as const }],
  series: [
    { name: '项目数', type: 'bar', data: PROJECT_TYPES.map((p) => p.items), itemStyle: { color: '#2A6DFF' } },
    { name: '人才人次', type: 'line', yAxisIndex: 1, data: PROJECT_TYPES.map((p) => p.people), itemStyle: { color: '#10B981' }, lineStyle: { width: 3 } },
  ],
};

const chartEducationOption = {
  tooltip: { trigger: 'item' as const },
  series: [{ type: 'pie', radius: '60%', data: EDUCATION.map((e) => ({ name: e.name, value: e.value })), label: { formatter: '{b} {c}%' } }],
  color: ['#2A6DFF', '#5b8fff', '#10B981', '#8B5CF6'],
};

const chartAgeOption = {
  tooltip: { trigger: 'axis' as const },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category' as const, data: AGE_GROUPS.map((a) => a.name) },
  yAxis: { type: 'value' as const },
  series: [{ type: 'bar', data: AGE_GROUPS.map((a) => a.value), itemStyle: { color: '#10B981', borderRadius: [6, 6, 0, 0] } }],
};

const chartTeacherUtilOption = {
  tooltip: { formatter: '{b}: {c}%' },
  series: [{ type: 'gauge', startAngle: 200, endAngle: -20, min: 0, max: 100, progress: { show: true, width: 14 }, axisLine: { lineStyle: { width: 14 } }, detail: { valueAnimation: true, formatter: '{value}%', fontSize: 22, offsetCenter: [0, '20%'] }, data: [{ value: 87, name: '讲师利用率' }] }],
};

const chartMethodOption = {
  tooltip: { trigger: 'item' as const },
  legend: { bottom: 0 },
  series: [{ type: 'pie', radius: ['40%', '65%'], data: [{ name: '案例教学', value: 40 }, { name: '实操模拟', value: 35 }, { name: '理论讲授', value: 25 }], label: { formatter: '{b} {d}%' } }],
  color: ['#2A6DFF', '#10B981', '#F59E0B'],
};

function makePlatformCompareOption() {
  const online = PLATFORM_ROWS.filter((r) => r.platform.includes('线上'));
  const offline = PLATFORM_ROWS.filter((r) => r.platform.includes('线下'));
  const mix = PLATFORM_ROWS.filter((r) => r.platform.includes('混合'));
  const avgG = (arr: typeof PLATFORM_ROWS, k: 'grad' | 'pass' | 'score' | 'cert') => (arr.length ? Math.round((arr.reduce((s, r) => s + r[k], 0) / arr.length) * 10) / 10 : 0);
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { data: ['结业率', '考核通过率', '平均成绩', '证书获取率'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '18%', containLabel: true },
    xAxis: { type: 'category' as const, data: ['线上学习平台', '线下实训平台', '混合平台'] },
    yAxis: { type: 'value' as const },
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
  tooltip: { formatter: (p: any) => `${p.data?.[2] || ''}<br/>影响:${p.data?.[0] || 0} 概率:${p.data?.[1] || 0}` },
  grid: { left: '12%', right: '8%', bottom: '12%', top: '8%' },
  xAxis: { type: 'value' as const, name: '发生概率', min: 0, max: 5, splitLine: { show: true } },
  yAxis: { type: 'value' as const, name: '影响程度', min: 0, max: 5, splitLine: { show: true } },
  series: [{
    type: 'scatter', symbolSize: (v: number[]) => (v[0] + v[1]) * 8,
    data: RISK_MATRIX.map((r) => ({ value: [r.prob, r.impact, r.name], itemStyle: { color: (r.prob + r.impact) >= 6 ? '#ef4444' : (r.prob + r.impact) >= 4 ? '#f59e0b' : '#10b981' } })),
    label: { show: true, formatter: (p: any) => p.data?.[2], position: 'top' as const, fontSize: 11 },
  }],
};

// ============================================================
// 各地市×工种表格
// ============================================================
function RegionOccTable() {
  const { cities, occs, data } = REGION_OCC;
  const columns = [
    { title: '地市', dataIndex: 'city', key: 'city', fixed: 'left' as const, width: 80, render: (v: string) => <strong>{v}</strong> },
    ...occs.map((o) => ({ title: o, dataIndex: o, key: o, width: 80 })),
    { title: '合计', dataIndex: 'total', key: 'total', width: 80, render: (v: number) => <strong>{v}</strong> },
  ];
  const tableData = cities.map((city, i) => {
    const row: Record<string, string | number> = { city };
    occs.forEach((o, j) => { row[o] = data[i][j]; });
    row.total = data[i].reduce((a: number, b: number) => a + b, 0);
    return row;
  });
  return <Table columns={columns} dataSource={tableData} rowKey="city" pagination={false} size="small" bordered />;
}

// ============================================================
// 主页面
// ============================================================
export function DataPanelPage() {
  const chartRefs = useRef<ReactECharts[]>([]);
  const [activeNav, setActiveNav] = useState('sec-talent');

  // 滚动监听：自动高亮当前可视板块
  useEffect(() => {
    const ids = NAV_ITEMS.map((n) => n.key);
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries.filter((e) => e.isIntersecting);
        if (visible.length > 0) {
          setActiveNav(visible[0].target.id);
        }
      },
      { rootMargin: '-80px 0px -60% 0px' },
    );
    ids.forEach((id) => {
      const el = document.getElementById(id);
      if (el) observer.observe(el);
    });
    return () => observer.disconnect();
  }, []);

  // 点击导航平滑滚动
  const scrollToSection = useCallback((id: string) => {
    setActiveNav(id);
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => chartRefs.current.forEach((c) => c?.getEchartsInstance()?.resize()), 350);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="data-panel-page">
      {/* KPI 指标卡片 */}
      <div className="dp-kpi-grid">
        {KPI_DATA.map((k, i) => (
          <div key={i} className="dp-kpi-card">
            <div style={S.kpiTop}>
              <div style={S.kpiIcon(k.bg, k.ic)}><k.icon /></div>
            </div>
            <div style={S.kpiVal}>{k.value}</div>
            <div style={S.kpiLabel}>{k.label}</div>
            <div style={S.kpiTrend(k.up)}>
              {k.up === true && <ArrowUpOutlined />} {k.trend}
            </div>
          </div>
        ))}
      </div>

      {/* 悬浮导航栏 */}
      <nav className="dp-nav-bar">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.key}
            className={`dp-nav-btn${activeNav === item.key ? ' active' : ''}`}
            onClick={() => scrollToSection(item.key)}
          >
            <item.icon style={{ color: activeNav === item.key ? '#fff' : item.color }} />
            {item.label}
          </button>
        ))}
      </nav>

      {/* 人才数据板块 */}
      <section id="sec-talent" className="dp-section">
        <div style={S.sectionHead}>
          <div style={S.iconWrap('#E8F0FF', '#2A6DFF')}><TeamOutlined /></div>
          <div>
            <Typography.Title level={4} style={{ margin: 0 }}>人才数据板块</Typography.Title>
            <Typography.Text type="secondary">区域、企业、项目、职业、工种、级别多维度人才画像</Typography.Text>
          </div>
        </div>

        <div className="dp-grid-23" style={S.mb20}>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><BarChartOutlined /> 职业工种分布</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[0] = e; }} option={chartOccupationOption} style={S.chart} />
          </div>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><PieChartOutlined /> 人才类型结构</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[1] = e; }} option={chartTalentTypeOption} style={S.chartSm} />
          </div>
        </div>

        <div className="dp-grid-2" style={S.mb20}>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><EnvironmentOutlined /> 地域分布（广西）</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[2] = e; }} option={chartRegionOption} style={S.chartSm} />
          </div>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><LineChartOutlined /> 培训项目数年度对比</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[3] = e; }} option={chartYearTrendOption} style={S.chartSm} />
          </div>
        </div>

        <div className="dp-sub-card" style={S.mb20}>
          <div style={S.subCardTitle}><TableOutlined /> 各地市 × 工种人才数量</div>
          <RegionOccTable />
        </div>

        <div className="dp-grid-2" style={S.mb20}>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><BankOutlined /> 企业性质 × 人才级别</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[4] = e; }} option={chartEnterpriseLevelOption} style={S.chart} />
          </div>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><ApartmentOutlined /> 项目类型人才人次</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[5] = e; }} option={chartProjectTypeOption} style={S.chart} />
          </div>
        </div>

        <div className="dp-grid-3">
          <div className="dp-sub-card">
            <div style={S.subCardTitle}>学历结构</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[6] = e; }} option={chartEducationOption} style={S.chartXs} />
          </div>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}>年龄分布</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[7] = e; }} option={chartAgeOption} style={S.chartXs} />
          </div>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}>各工种高级人才占比</div>
            <div style={{ marginTop: 8 }}>
              {SENIOR_PCT.map((s, i) => (
                <div key={i} style={S.progressRow}>
                  <label style={S.progressLabel}><span>{s.name}</span><span>{s.pct}%</span></label>
                  <div style={S.progressBar}><span style={S.progressFill(s.pct)} /></div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* 资源数据板块 */}
      <section id="sec-resource" className="dp-section">
        <div style={S.sectionHead}>
          <div style={S.iconWrap('#F5F3FF', '#8B5CF6')}><BlockOutlined /></div>
          <div>
            <Typography.Title level={4} style={{ margin: 0 }}>资源数据板块</Typography.Title>
            <Typography.Text type="secondary">人机料法环五类培训资源状态与利用率</Typography.Text>
          </div>
        </div>

        <div className="dp-resource-grid">
          {RESOURCES.map((r) => (
            <div key={r.key} className="dp-res-card" style={{ borderLeft: `4px solid ${r.color}` }}>
              <h4 style={S.resCardTitle}><r.icon style={{ color: r.color }} /> {r.title}</h4>
              <div style={S.bigNum}>{r.items[0].split(' ').slice(-1)[0] || '—'}</div>
              {r.items.slice(1).map((t, j) => <div key={j} style={S.subText}>{t}</div>)}
              {r.util && <div style={{ ...S.subText, marginTop: 10, fontWeight: 600, color: '#2A6DFF' }}>{r.util}</div>}
            </div>
          ))}
        </div>

        <div className="dp-grid-2" style={S.mt20}>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><ReadOutlined /> 讲师利用率</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[8] = e; }} option={chartTeacherUtilOption} style={S.chartSm} />
          </div>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><BookOutlined /> 教学方法分布</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[9] = e; }} option={chartMethodOption} style={S.chartSm} />
          </div>
        </div>
      </section>

      {/* 平台数据板块 */}
      <section id="sec-platform" className="dp-section">
        <div style={S.sectionHead}>
          <div style={S.iconWrap('#ECFDF5', '#10B981')}><LaptopOutlined /></div>
          <div>
            <Typography.Title level={4} style={{ margin: 0 }}>平台数据板块</Typography.Title>
            <Typography.Text type="secondary">不同平台、不同培训班组成果指标对比</Typography.Text>
          </div>
        </div>

        <div className="dp-sub-card" style={S.mb20}>
          <div style={S.subCardTitle}><TableOutlined /> 平台 × 班组成果明细</div>
          <Table dataSource={PLATFORM_ROWS.map((r, i) => ({ ...r, key: i }))} pagination={false} size="small" bordered
            columns={[
              { title: '平台', dataIndex: 'platform', key: 'platform' },
              { title: '班组名称', dataIndex: 'team', key: 'team', render: (v: string) => <strong>{v}</strong> },
              { title: '结业率', dataIndex: 'grad', key: 'grad', render: (v: number) => <div style={S.pctCell}>{v}%<div style={S.pctBar}><span style={S.pctFill(v)} /></div></div> },
              { title: '考核通过率', dataIndex: 'pass', key: 'pass', render: (v: number) => <div style={S.pctCell}>{v}%<div style={S.pctBar}><span style={S.pctFill(v)} /></div></div> },
              { title: '平均成绩', dataIndex: 'score', key: 'score' },
              { title: '证书获取率', dataIndex: 'cert', key: 'cert', render: (v: number) => <div style={S.pctCell}>{v}%<div style={S.pctBar}><span style={S.pctFill(v)} /></div></div> },
            ]}
          />
        </div>

        <div className="dp-sub-card">
          <div style={S.subCardTitle}><BarChartOutlined /> 各平台平均成果对比</div>
          <ReactECharts ref={(e) => { if (e) chartRefs.current[10] = e; }} option={makePlatformCompareOption()} style={S.chart} />
        </div>
      </section>

      {/* 项目数据板块 */}
      <section id="sec-project" className="dp-section">
        <div style={S.sectionHead}>
          <div style={S.iconWrap('#FFF7ED', '#F59E0B')}><WarningOutlined /></div>
          <div>
            <Typography.Title level={4} style={{ margin: 0 }}>项目数据板块</Typography.Title>
            <Typography.Text type="secondary">重点项目里程、质量、成本与风险点管控</Typography.Text>
          </div>
        </div>

        <div className="dp-grid-32" style={S.mb20}>
          <div>
            {PROJECTS.map((p, i) => {
              const ms = p.milestones.map((v, j) => {
                const bg = v >= 100 ? '#10b981' : `linear-gradient(90deg, #10b981 ${v}%, #e2e8f0 ${v}%)`;
                return <div key={j} style={{ flex: 1, height: 6, borderRadius: 3, background: bg, overflow: 'hidden', position: 'relative' }} title={['课程开发', '培训实施', '考试筹备'][j] + ` ${v}%`} />;
              });
              return (
                <div key={i} className="dp-project-card">
                  <h4 style={{ fontSize: 15, fontWeight: 600, margin: '0 0 12px' }}>
                    {p.name}<span style={S.riskTag(p.risk)}>{riskLabelMap[p.risk]}</span>
                  </h4>
                  <div style={S.projMeta}>
                    <div><span style={{ color: '#64748b' }}>类型：</span>{p.type}</div>
                    <div><span style={{ color: '#64748b' }}>周期：</span>{p.period}</div>
                    <div><span style={{ color: '#64748b' }}>质量：</span>{p.quality}</div>
                    <div><span style={{ color: '#64748b' }}>成本：</span>{p.cost}</div>
                  </div>
                  <div style={{ fontSize: 12, color: '#64748b', marginBottom: 4 }}>里程碑进度</div>
                  <div style={S.milestoneBar}>{ms}</div>
                  <div style={S.riskDesc}><strong>风险：</strong>{p.riskText} · <strong>管控：</strong>{p.control}</div>
                </div>
              );
            })}
          </div>
          <div className="dp-sub-card">
            <div style={S.subCardTitle}><WarningOutlined /> 项目风险矩阵概览</div>
            <ReactECharts ref={(e) => { if (e) chartRefs.current[11] = e; }} option={chartRiskMatrixOption} style={S.chart} />
          </div>
        </div>
      </section>

      <div style={{ textAlign: 'center', fontSize: 12, color: '#64748b', marginTop: 12 }}>
        数据更新至 2026-05 · 演示数据 · ECharts 交互图表
      </div>
    </div>
  );
}

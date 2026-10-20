import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import ReactECharts from 'echarts-for-react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Space, Spin, Tag, Tooltip, Typography } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';
import { getReviewSyncPanel } from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import { useNavStore } from '../../stores/navStore';
import { usePaneActive } from '../../layouts/TabKeepAlive';
/** 红绿灯阈值（方案 §4.3，集中在前端常量便于调） */
const ACTIVE_RATE_RED = 40; // 7日活跃率 <40% 标红
const PASS_RATE_RED = 40; // 模考及格率 <40% 标红
const PASS_RATE_GREEN = 60; // ≥60% 标绿
const MOCK_MIN_SAMPLE = 10; // 模考完成 <10 套显示"--"（样本不足）
const POLL_INTERVAL_MS = 60_000;
/** 明细页=考试计划列表（行内「AI同步学统计」入口）；SPA 内容区按 selectedKey 渲染，不走 URL */
const DETAIL_MENU_KEY = 'kao_shi_ji_hua';
function formatWan(value) {
    if (value == null)
        return '--';
    return value >= 10000 ? `${(value / 10000).toFixed(1)}万` : String(value);
}
function formatRate(value) {
    return value == null ? '--' : `${Number(value).toFixed(1)}%`;
}
function rateTag(value, redBelow, redAbove, greenAtOrAbove) {
    if (value == null)
        return null;
    if (redAbove != null && value > redAbove)
        return _jsx(Tag, { color: "red", children: "\u504F\u9AD8" });
    if (redBelow != null && value < redBelow)
        return _jsx(Tag, { color: "red", children: "\u504F\u4F4E" });
    if (greenAtOrAbove != null && value >= greenAtOrAbove)
        return _jsx(Tag, { color: "green", children: "\u6B63\u5E38" });
    return null;
}
function Kpi({ label, value, foot, tag }) {
    return (_jsxs("div", { className: "review-sync-kpi", children: [_jsx("div", { className: "review-sync-kpi-label", children: label }), _jsxs("div", { className: "review-sync-kpi-value", children: [value, tag ? _jsx("span", { style: { marginLeft: 8 }, children: tag }) : null] }), foot ? _jsx("div", { className: "review-sync-kpi-foot", children: foot }) : null] }));
}
function GroupTitle({ children }) {
    return _jsx("div", { className: "review-sync-group-title", children: children });
}
/** 运营数据面板「同步练习」分区（方案 v0.1 §4）：一次拿全，60s 轮询，局部降级 */
export function ReviewSyncPanel() {
    const hasPerm = useAuthStore((s) => s.hasPermission);
    const canViewStats = hasPerm('exam:review-sync:stats:view');
    const setSelectedKey = useNavStore((s) => s.setSelectedKey);
    const paneActive = usePaneActive(); // 标签页不可见时暂停 60s 轮询（§3.6）
    const panelQuery = useQuery({
        queryKey: ['training-dashboard-review-sync-panel'],
        queryFn: () => getReviewSyncPanel(30),
        refetchInterval: paneActive ? POLL_INTERVAL_MS : false,
        refetchIntervalInBackground: false,
        refetchOnWindowFocus: true,
        staleTime: 0,
    });
    if (panelQuery.isLoading && !panelQuery.data) {
        return (_jsx(Card, { size: "small", title: "\u540C\u6B65\u7EC3\u4E60\uFF08AI \u540C\u6B65\u5B66\uFF09", children: _jsx(Spin, { size: "small", style: { display: 'block', margin: 24 } }) }));
    }
    if (panelQuery.isError || !panelQuery.data) {
        return (_jsx(Alert, { type: "warning", showIcon: true, message: "\u540C\u6B65\u7EC3\u4E60\u7EDF\u8BA1\u52A0\u8F7D\u5931\u8D25", action: _jsx(Button, { size: "small", onClick: () => void panelQuery.refetch(), children: "\u91CD\u8BD5" }) }));
    }
    const { scale, effort, effect, trend30d, wrongTrend30d, weakTop5 } = panelQuery.data;
    const mockSampleEnough = effect.mockDone >= MOCK_MIN_SAMPLE;
    const trendOption = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['作答数', '活跃学员', '正确率'], top: 0 },
        grid: { left: 48, right: 48, top: 32, bottom: 24 },
        xAxis: { type: 'category', data: trend30d.map((p) => p.date.slice(5)) },
        yAxis: [
            { type: 'value', name: '题数/人数' },
            { type: 'value', name: '正确率%', max: 100, min: 0 },
        ],
        series: [
            { name: '作答数', type: 'bar', data: trend30d.map((p) => p.answers), itemStyle: { color: '#7fb3ff' } },
            { name: '活跃学员', type: 'line', smooth: true, data: trend30d.map((p) => p.activeUsers), itemStyle: { color: '#52c41a' } },
            { name: '正确率', type: 'line', yAxisIndex: 1, smooth: true, data: trend30d.map((p) => p.correctRate), itemStyle: { color: '#722ed1' } },
        ],
    };
    const wrongOption = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['新增错题', '消灭错题'], top: 0 },
        grid: { left: 48, right: 24, top: 32, bottom: 24 },
        xAxis: { type: 'category', data: wrongTrend30d.map((p) => p.date.slice(5)) },
        yAxis: { type: 'value' },
        series: [
            { name: '消灭错题', type: 'bar', stack: 'w', data: wrongTrend30d.map((p) => p.removedCnt), itemStyle: { color: '#52c41a' } },
            { name: '新增错题', type: 'bar', stack: 'w', data: wrongTrend30d.map((p) => p.newCnt), itemStyle: { color: '#ff4d4f' } },
        ],
    };
    const weakOption = {
        tooltip: { trigger: 'axis', valueFormatter: (v) => `${v}%` },
        grid: { left: 120, right: 48, top: 8, bottom: 24 },
        xAxis: { type: 'value', max: 100 },
        yAxis: { type: 'category', data: [...weakTop5].reverse().map((w) => w.title) },
        series: [{
                type: 'bar',
                data: [...weakTop5].reverse().map((w) => w.correctRate),
                itemStyle: { color: '#ff7875' },
                label: { show: true, position: 'right', formatter: (p) => (p.value == null ? '--' : `${p.value}%`) },
            }],
    };
    const detailLink = canViewStats ? (_jsx(Button, { type: "link", size: "small", icon: _jsx(LineChartOutlined, {}), onClick: () => setSelectedKey(DETAIL_MENU_KEY), children: "\u67E5\u770B\u660E\u7EC6" })) : (_jsx(Tooltip, { title: "\u9700\u8003\u8BD5\u7BA1\u7406\u6743\u9650", children: _jsx(Button, { type: "link", size: "small", icon: _jsx(LineChartOutlined, {}), disabled: true, children: "\u67E5\u770B\u660E\u7EC6" }) }));
    return (_jsxs(Card, { size: "small", title: _jsxs(Space, { children: ["\u540C\u6B65\u7EC3\u4E60\uFF08AI \u540C\u6B65\u5B66\uFF09", _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u8FD130\u5929\u53E3\u5F84" })] }), extra: detailLink, children: [_jsx(GroupTitle, { children: "\u89C4\u6A21" }), _jsxs("div", { className: "review-sync-kpi-row", children: [_jsx(Kpi, { label: "\u8BA1\u5212\u5B66\u5458", value: formatWan(scale.totalPlans), foot: `已完成 ${scale.completed}` }), _jsx(Kpi, { label: "\u8FDB\u884C\u4E2D", value: formatWan(scale.active) }), _jsx(Kpi, { label: "\u8FD17\u65E5\u65B0\u589E\u8BA1\u5212", value: formatWan(scale.newPlans7d), foot: "\u542B\u4ECA\u5929" }), _jsx(Kpi, { label: "\u8FD17\u65E5\u6D3B\u8DC3", value: formatWan(scale.active7d), tag: rateTag(scale.activeRate7d, ACTIVE_RATE_RED), foot: `7日活跃率 ${formatRate(scale.activeRate7d)}` }), _jsx(Kpi, { label: "\u7D2F\u8BA1\u4E2D\u65AD", value: `${scale.interruptPlans} 人次` })] }), _jsx(GroupTitle, { children: "\u6295\u5165" }), _jsxs("div", { className: "review-sync-kpi-row", children: [_jsx(Kpi, { label: "\u7D2F\u8BA1\u4F5C\u7B54", value: formatWan(effort.totalAnswers) }), _jsx(Kpi, { label: "\u4EBA\u5747\u4F5C\u7B54", value: effort.avgAnswersPerPlan == null ? '--' : `${effort.avgAnswersPerPlan} 题` }), _jsx(Kpi, { label: "\u4EFB\u52A1\u5B8C\u6210\u7387", value: formatRate(effort.dayCompletionRate) }), _jsx(Kpi, { label: "\u4ECA\u65E5\u4F5C\u7B54", value: formatWan(effort.todayAnswers), foot: `活跃 ${effort.todayActive} 人` })] }), _jsx(GroupTitle, { children: "\u6548\u679C" }), _jsxs("div", { className: "review-sync-kpi-row", children: [_jsx(Kpi, { label: "\u6574\u4F53\u6B63\u786E\u7387", value: formatRate(effect.correctRate) }), _jsx(Kpi, { label: "\u7D2F\u8BA1\u6D88\u706D\u9519\u9898", value: formatWan(effect.removedWrongs), foot: `在册 红${formatWan(effect.redWrongs)} / 黄${formatWan(effect.yellowWrongs)}` }), _jsx(Kpi, { label: "\u6A21\u8003\u5B8C\u6210", value: mockSampleEnough ? `${effect.mockDone} 套` : `${effect.mockDone} 套`, foot: `平均分 ${effect.mockAvgScore == null ? '--' : effect.mockAvgScore}` }), _jsx(Kpi, { label: "\u6A21\u8003\u53CA\u683C\u7387", value: mockSampleEnough ? formatRate(effect.mockPassRate) : '--', tag: mockSampleEnough ? rateTag(effect.mockPassRate, PASS_RATE_RED, undefined, PASS_RATE_GREEN) : null, foot: mockSampleEnough ? undefined : '样本不足' })] }), _jsxs("div", { className: "training-dashboard-chart-grid", style: { marginTop: 12 }, children: [_jsx(Card, { size: "small", title: "\u8FD130\u5929\u6D3B\u8DC3\u4E0E\u6B63\u786E\u7387", children: _jsx(ReactECharts, { option: trendOption, style: { height: 280 } }) }), _jsx(Card, { size: "small", title: "\u9519\u9898\u65B0\u589E vs \u6D88\u706D", children: _jsx(ReactECharts, { option: wrongOption, style: { height: 280 } }) })] }), _jsx(Card, { size: "small", title: "\u8584\u5F31\u8003\u6838\u9879\u76EE Top5\uFF08L1\uFF0C\u6B63\u786E\u7387\u6700\u4F4E\uFF0C\u8FD130\u5929\u4F5C\u7B54\u226550\u6B21\u53C2\u8BC4\uFF09", style: { marginTop: 12 }, children: weakTop5.length === 0 ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u6EE1\u8DB3\u53C2\u8BC4\u95E8\u69DB\u7684\u6570\u636E" })) : (_jsx(ReactECharts, { option: weakOption, style: { height: Math.max(160, weakTop5.length * 40) } })) })] }));
}

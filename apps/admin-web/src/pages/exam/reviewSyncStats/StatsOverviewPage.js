import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * AI 同步学统计总览页 —— 《AI同步学统计功能设计方案》v1.1 §6.2
 * 隐藏路由：/training/exam-lists/:examId/review-sync-stats（考试管理列表页行内按钮进入）
 */
import { useMemo, useState } from 'react';
import { Alert, Button, Card, Col, DatePicker, Empty, Input, Row, Select, Space, Statistic, Table, Tag, Typography, message, } from 'antd';
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { pageExams } from '../../../api/training';
import { useAuthStore } from '../../../stores/authStore';
import { downloadBlob, exportStudents, fetchDailyTrend, fetchKnowledgeMastery, fetchStatsOverview, fetchStudents, fetchWrongTrend, } from './api';
const RANGE_OPTIONS = [
    { value: 7, label: '近 7 天' },
    { value: 14, label: '近 14 天' },
    { value: 30, label: '近 30 天' },
];
const STAGE_NAMES = { 1: '地毯式扫荡', 2: '定向爆破', 3: '仿真模考' };
const STATUS_TAGS = {
    active: { color: 'processing', text: '进行中' },
    completed: { color: 'success', text: '已完成' },
    abandoned: { color: 'default', text: '已放弃' },
};
/** 与 main.tsx 约定一致的路由跳转（pushState + popstate 事件） */
function navigateTo(path) {
    window.history.pushState({}, '', path);
    window.dispatchEvent(new PopStateEvent('popstate'));
}
export function StatsOverviewPage({ examId, embedded = false, enabled = true, examTitle, }) {
    const hasPerm = useAuthStore((s) => s.hasPermission);
    const canExport = hasPerm('exam:review-sync:stats:export');
    const canView = hasPerm('exam:review-sync:stats:view');
    const active = enabled && examId > 0 && canView;
    const [rangeDays, setRangeDays] = useState(14);
    const [customRange, setCustomRange] = useState(null);
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [exporting, setExporting] = useState(false);
    const { from, to } = useMemo(() => {
        if (customRange) {
            return {
                from: customRange[0].format('YYYY-MM-DD'),
                to: customRange[1].format('YYYY-MM-DD'),
            };
        }
        return {
            from: dayjs().subtract(rangeDays - 1, 'day').format('YYYY-MM-DD'),
            to: dayjs().format('YYYY-MM-DD'),
        };
    }, [rangeDays, customRange]);
    // 考试计划下拉（独立页用；嵌入模式不拉全量计划）
    const examsQuery = useQuery({
        queryKey: ['review-sync-stats-exam-options'],
        queryFn: () => pageExams({ page: 1, pageSize: 200 }),
        enabled: !embedded && active,
    });
    const examOptions = (examsQuery.data?.items ?? []).map((e) => ({
        value: e.id,
        label: e.title ?? `计划#${e.id}`,
    }));
    const currentExamTitle = examTitle
        || examOptions.find((o) => o.value === examId)?.label
        || `计划#${examId}`;
    const query = { from, to, examId };
    const overviewQuery = useQuery({
        queryKey: ['review-sync-stats', 'overview', query],
        queryFn: () => fetchStatsOverview(query),
        enabled: active,
    });
    const trendQuery = useQuery({
        queryKey: ['review-sync-stats', 'daily-trend', query],
        queryFn: () => fetchDailyTrend(query),
        enabled: active,
    });
    const wrongQuery = useQuery({
        queryKey: ['review-sync-stats', 'wrong-trend', query],
        queryFn: () => fetchWrongTrend(query),
        enabled: active,
    });
    const knowledgeQuery = useQuery({
        queryKey: ['review-sync-stats', 'knowledge', examId],
        queryFn: () => fetchKnowledgeMastery(examId),
        enabled: active,
    });
    const studentsQuery = useQuery({
        queryKey: ['review-sync-stats', 'students', examId, keyword, page, pageSize],
        queryFn: () => fetchStudents({ page, pageSize, examId, keyword: keyword || undefined }),
        enabled: active,
    });
    const overview = overviewQuery.data;
    const trendSeries = trendQuery.data?.series ?? [];
    const wrongTrend = wrongQuery.data;
    // ------------------------------------------------------------------
    // 图表 option
    // ------------------------------------------------------------------
    const trendOption = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['作答数', '活跃学员', '正确率'], top: 0, left: 'center' },
        grid: { left: 48, right: 52, top: 44, bottom: 28 },
        xAxis: { type: 'category', data: trendSeries.map((p) => p.day.slice(5)) },
        yAxis: [
            { type: 'value' },
            { type: 'value', max: 100, min: 0, axisLabel: { formatter: '{value}%' } },
        ],
        series: [
            { name: '作答数', type: 'bar', barMaxWidth: 36, data: trendSeries.map((p) => p.answers), itemStyle: { color: '#73a9fa' } },
            { name: '活跃学员', type: 'line', data: trendSeries.map((p) => p.activeUsers), itemStyle: { color: '#50b66f' } },
            {
                name: '正确率', type: 'line', yAxisIndex: 1,
                data: trendSeries.map((p) => p.accuracy), itemStyle: { color: '#e7916c' },
            },
        ],
    };
    const wrongDist = wrongTrend?.distribution;
    const wrongDonutOption = {
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, left: 'center', itemWidth: 12, itemHeight: 12, textStyle: { fontSize: 12 } },
        series: [{
                type: 'pie',
                radius: ['38%', '60%'],
                center: ['50%', '42%'],
                data: [
                    { name: '红牌', value: wrongDist?.red ?? 0, itemStyle: { color: '#e55744' } },
                    { name: '黄牌', value: wrongDist?.yellow ?? 0, itemStyle: { color: '#e7916c' } },
                    { name: '已掌握', value: wrongDist?.green ?? 0, itemStyle: { color: '#50b66f' } },
                    { name: '加星(顽固)', value: wrongDist?.star ?? 0, itemStyle: { color: '#b03a2e' } },
                ],
            }],
    };
    const newByDay = new Map((wrongTrend?.dailyNew ?? []).map((p) => [p.day, p.cnt]));
    const removedByDay = new Map((wrongTrend?.dailyRemoved ?? []).map((p) => [p.day, p.cnt]));
    const wrongDays = Array.from(new Set([...newByDay.keys(), ...removedByDay.keys()])).sort();
    const wrongDailyOption = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['新增错题', '消灭错题'], top: 0, left: 'center' },
        grid: { left: 40, right: 16, top: 36, bottom: 24 },
        xAxis: { type: 'category', data: wrongDays.map((d) => d.slice(5)) },
        yAxis: { type: 'value' },
        series: [
            { name: '新增错题', type: 'bar', stack: 'w', barMaxWidth: 24, data: wrongDays.map((d) => newByDay.get(d) ?? 0), itemStyle: { color: '#e55744' } },
            { name: '消灭错题', type: 'bar', stack: 'w', barMaxWidth: 24, data: wrongDays.map((d) => -(removedByDay.get(d) ?? 0)), itemStyle: { color: '#50b66f' } },
        ],
    };
    const l1List = knowledgeQuery.data?.l1 ?? [];
    const masteryOption = {
        tooltip: { trigger: 'axis' },
        grid: { left: 140, right: 40, top: 20, bottom: 28 },
        xAxis: { type: 'value', max: 100, name: '正确率%' },
        yAxis: { type: 'category', data: l1List.map((k) => k.title).reverse() },
        series: [{
                type: 'bar',
                data: l1List.map((k) => ({
                    value: k.accuracy,
                    itemStyle: { color: k.accuracy < 60 ? '#e55744' : k.accuracy < 80 ? '#e7916c' : '#50b66f' },
                })).reverse(),
                label: { show: true, position: 'right', formatter: '{c}%' },
            }],
    };
    // ------------------------------------------------------------------
    // 学员明细表
    // ------------------------------------------------------------------
    const columns = [
        { title: '学员', dataIndex: 'realname', width: 100 },
        { title: '手机号', dataIndex: 'phone', width: 120, render: (v) => v ?? '-' },
        { title: '考试计划', dataIndex: 'examTitle', ellipsis: true },
        {
            title: '阶段/进度', key: 'stage', width: 170,
            render: (_, r) => (_jsxs("span", { children: [_jsx(Tag, { color: "blue", children: STAGE_NAMES[r.stage] ?? `阶段${r.stage}` }), "\u7B2C", r.dayIndex, "/", r.totalDays, "\u5929"] })),
        },
        { title: '累计练题', dataIndex: 'answers', width: 90, sorter: (a, b) => a.answers - b.answers },
        {
            title: '正确率', dataIndex: 'accuracy', width: 90,
            render: (v) => (_jsxs("span", { style: { color: v < 60 ? '#e55744' : v < 80 ? '#e7916c' : '#50b66f' }, children: [v, "%"] })),
        },
        {
            title: '错题(红/黄/加星)', key: 'wrong', width: 140,
            render: (_, r) => (_jsxs("span", { children: [_jsxs(Tag, { color: "#e55744", children: ["\u7EA2", r.redCnt] }), _jsxs(Tag, { color: "#e7916c", children: ["\u9EC4", r.yellowCnt] }), r.starCnt > 0 && _jsxs(Tag, { color: "#b03a2e", children: ["\u2605", r.starCnt] })] })),
        },
        { title: '最近活跃', dataIndex: 'lastActiveDate', width: 110, render: (v) => v ?? '-' },
        {
            title: '状态', dataIndex: 'status', width: 90,
            render: (v) => {
                const tag = STATUS_TAGS[v] ?? { color: 'default', text: v };
                return _jsx(Tag, { color: tag.color, children: tag.text });
            },
        },
        {
            title: '操作', key: 'act', width: 80, fixed: 'right',
            render: (_, r) => (_jsx(Button, { type: "link", size: "small", onClick: () => navigateTo(`/training/exam-lists/${r.examId}/review-sync-stats/student/${r.planId}`), children: "\u8BE6\u60C5" })),
        },
    ];
    const onExport = async () => {
        setExporting(true);
        try {
            const blob = await exportStudents({ examId, keyword: keyword || undefined });
            downloadBlob(blob, `AI同步学学员明细_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`);
            message.success('导出成功');
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '导出失败');
        }
        finally {
            setExporting(false);
        }
    };
    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------
    if (!canView) {
        return (_jsx(Alert, { type: "warning", showIcon: true, message: "\u65E0\u540C\u6B65\u5B66\u7EDF\u8BA1\u67E5\u770B\u6743\u9650", description: "\u9700\u8981\u6743\u9650\u7801 exam:review-sync:stats:view" }));
    }
    return (_jsxs("div", { style: { padding: embedded ? 0 : 16 }, children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [!embedded && (_jsxs(_Fragment, { children: [_jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: () => navigateTo('/training/exam-list'), children: "\u8FD4\u56DE\u8003\u8BD5\u7BA1\u7406" }), _jsx(Typography.Text, { strong: true, style: { fontSize: 16 }, children: "AI \u540C\u6B65\u5B66\u7EDF\u8BA1" }), _jsx("span", { children: "\u5F53\u524D\u8BA1\u5212\uFF1A" }), _jsx(Select, { style: { minWidth: 280 }, value: examId, options: examOptions, loading: examsQuery.isLoading, showSearch: true, optionFilterProp: "label", onChange: (v) => navigateTo(`/training/exam-lists/${v}/review-sync-stats`) })] })), embedded && (_jsxs(Typography.Text, { strong: true, style: { fontSize: 15 }, children: ["\u540C\u6B65\u5B66\u5B66\u60C5 \u00B7 ", currentExamTitle] })), _jsx(Select, { style: { width: 120 }, value: customRange ? undefined : rangeDays, options: RANGE_OPTIONS, placeholder: "\u81EA\u5B9A\u4E49", onChange: (v) => { setRangeDays(v); setCustomRange(null); setPage(1); } }), _jsx(DatePicker.RangePicker, { value: customRange, onChange: (v) => {
                            setCustomRange(v && v[0] && v[1] ? [v[0], v[1]] : null);
                            setPage(1);
                        } })] }), _jsxs(Row, { gutter: 16, style: { marginBottom: 16 }, children: [_jsx(Col, { span: 4, children: _jsx(Card, { children: _jsx(Statistic, { title: "\u8BA1\u5212\u5B66\u5458", value: overview?.studentCount ?? 0, suffix: "\u4EBA" }) }) }), _jsx(Col, { span: 4, children: _jsx(Card, { children: _jsx(Statistic, { title: "\u4ECA\u65E5\u6D3B\u8DC3", value: overview?.todayActiveUsers ?? 0, suffix: "\u4EBA" }) }) }), _jsx(Col, { span: 4, children: _jsx(Card, { children: _jsx(Statistic, { title: "\u4ECA\u65E5\u4F5C\u7B54", value: overview?.todayAnswers ?? 0, suffix: "\u9898" }) }) }), _jsx(Col, { span: 4, children: _jsx(Card, { children: _jsx(Statistic, { title: "\u7D2F\u8BA1\u4F5C\u7B54", value: overview?.totalAnswers ?? 0, suffix: "\u9898" }) }) }), _jsx(Col, { span: 4, children: _jsx(Card, { children: _jsx(Statistic, { title: "\u6574\u4F53\u6B63\u786E\u7387", value: overview?.overallAccuracy ?? 0, suffix: "%" }) }) }), _jsx(Col, { span: 4, children: _jsx(Card, { children: _jsx(Statistic, { title: "\u5728\u518C\u9519\u9898(\u7EA2/\u9EC4/\u52A0\u661F)", value: overview?.wrong.active ?? 0, suffix: `红${overview?.wrong.red ?? 0} 黄${overview?.wrong.yellow ?? 0} ★${overview?.wrong.star ?? 0}` }) }) })] }), _jsxs(Row, { gutter: 16, style: { marginBottom: 16 }, children: [_jsx(Col, { span: 14, children: _jsx(Card, { title: "\u6BCF\u65E5\u4F7F\u7528\u8D8B\u52BF", size: "small", children: trendSeries.length === 0
                                ? _jsx(Empty, { description: "\u6240\u9009\u8303\u56F4\u5185\u6682\u65E0 AI \u540C\u6B65\u7EC3\u4E60\u6570\u636E" })
                                : _jsx(ReactECharts, { option: trendOption, style: { height: 300 } }) }) }), _jsxs(Col, { span: 10, children: [_jsx(Card, { title: "\u9519\u9898\u724C\u7EA7\u5206\u5E03", size: "small", style: { marginBottom: 8 }, children: (wrongDist?.active ?? 0) + (wrongDist?.green ?? 0) === 0
                                    ? _jsx(Empty, { description: "\u6682\u65E0\u9519\u9898" })
                                    : _jsx(ReactECharts, { option: wrongDonutOption, style: { height: 190 } }) }), _jsx(Card, { title: "\u6BCF\u65E5\u65B0\u589E/\u6D88\u706D\u9519\u9898", size: "small", children: wrongDays.length === 0
                                    ? _jsx(Empty, { description: "\u6682\u65E0\u9519\u9898\u53D8\u52A8" })
                                    : _jsx(ReactECharts, { option: wrongDailyOption, style: { height: 190 } }) })] })] }), _jsx(Card, { title: "\u8003\u6838\u9879\u76EE\u638C\u63E1\u5EA6\uFF08L1\uFF09", size: "small", style: { marginBottom: 16 }, children: l1List.length === 0
                    ? _jsx(Empty, { description: "\u6682\u65E0\u4F5C\u7B54\u6570\u636E" })
                    : _jsx(ReactECharts, { option: masteryOption, style: { height: Math.max(220, l1List.length * 40) } }) }), _jsx(Card, { title: "\u5B66\u5458\u660E\u7EC6", size: "small", extra: (_jsxs(Space, { children: [_jsx(Input.Search, { placeholder: "\u5B66\u5458\u59D3\u540D/\u624B\u673A\u53F7", allowClear: true, style: { width: 200 }, onSearch: (v) => { setKeyword(v.trim()); setPage(1); } }), canExport && (_jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exporting, onClick: onExport, children: "\u5BFC\u51FA Excel" }))] })), children: _jsx(Table, { rowKey: "planId", size: "small", loading: studentsQuery.isLoading, columns: columns, dataSource: studentsQuery.data?.items ?? [], scroll: { x: 1200 }, pagination: {
                        current: page,
                        pageSize,
                        total: studentsQuery.data?.total ?? 0,
                        showSizeChanger: true,
                        showTotal: (t) => `共 ${t} 条`,
                        onChange: (p, ps) => {
                            if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                                setPageSize(ps);
                                setPage(1);
                            }
                            else {
                                setPage(p);
                            }
                        },
                    } }) })] }));
}

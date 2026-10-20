import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Alert, Button, Col, DatePicker, Input, Modal, Row, Space, Spin, Statistic, Switch, Table, Tabs, Typography, message, } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
const { RangePicker } = DatePicker;
const { Text } = Typography;
function fmtRate(v) {
    if (v == null)
        return '-';
    return `${v}%`;
}
function fmtTime(v) {
    if (!v)
        return '-';
    return String(v).replace('T', ' ').slice(0, 19);
}
/** 班级刷题学情面板（跟班 Tab / Modal 共用） */
export function ClassPracticeAnalyticsPanel({ classId, enabled = true }) {
    const [range, setRange] = useState([
        dayjs().subtract(29, 'day'),
        dayjs(),
    ]);
    const [inScopeOnly, setInScopeOnly] = useState(true);
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [exporting, setExporting] = useState(false);
    const active = enabled && classId != null;
    const queryParams = useMemo(() => ({
        from: range[0].format('YYYY-MM-DD'),
        to: range[1].format('YYYY-MM-DD'),
        inScopeOnly,
    }), [range, inScopeOnly]);
    const handleExport = async (allTime) => {
        if (classId == null)
            return;
        setExporting(true);
        try {
            await trainApi.exportClassPracticeAnalytics(classId, {
                ...queryParams,
                allTime,
            });
            message.success(allTime ? '已导出全部刷题数据' : '已导出当前区间刷题数据');
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '导出失败');
        }
        finally {
            setExporting(false);
        }
    };
    const summaryQuery = useQuery({
        queryKey: ['class-practice-analytics-summary', classId, queryParams],
        queryFn: () => trainApi.getClassPracticeAnalyticsSummary(classId, queryParams),
        enabled: active,
    });
    const studentsQuery = useQuery({
        queryKey: ['class-practice-analytics-students', classId, queryParams, keyword, page, pageSize],
        queryFn: () => trainApi.getClassPracticeAnalyticsStudents(classId, {
            ...queryParams,
            keyword: keyword.trim() || undefined,
            page,
            pageSize,
        }),
        enabled: active,
    });
    const knowledgesQuery = useQuery({
        queryKey: ['class-practice-analytics-knowledges', classId, queryParams],
        queryFn: () => trainApi.getClassPracticeAnalyticsKnowledges(classId, {
            ...queryParams,
            limit: 50,
        }),
        enabled: active,
    });
    const dailyQuery = useQuery({
        queryKey: ['class-practice-analytics-daily', classId, queryParams],
        queryFn: () => trainApi.getClassPracticeAnalyticsDaily(classId, queryParams),
        enabled: active,
    });
    const summary = summaryQuery.data;
    const studentColumns = [
        { title: '姓名', dataIndex: 'realname', width: 100, ellipsis: true },
        { title: '手机', dataIndex: 'phone', width: 120 },
        {
            title: 'App 账号',
            dataIndex: 'hasAccount',
            width: 90,
            render: (v) => (v ? '已开通' : '未开通'),
        },
        { title: '答题数', dataIndex: 'answerCount', width: 80, align: 'right' },
        { title: '正确数', dataIndex: 'rightCount', width: 80, align: 'right' },
        {
            title: '正确率',
            dataIndex: 'accuracyRate',
            width: 90,
            align: 'right',
            render: (v) => fmtRate(v),
        },
        { title: '练习次数', dataIndex: 'paperCount', width: 90, align: 'right' },
        { title: '练习天数', dataIndex: 'practiceDays', width: 90, align: 'right' },
        {
            title: '最近练习',
            dataIndex: 'lastPracticeAt',
            width: 160,
            render: (v) => fmtTime(v),
        },
        {
            title: '未练天数',
            dataIndex: 'idleDays',
            width: 90,
            align: 'right',
            render: (v) => (v == null ? '-' : v),
        },
    ];
    const knowledgeColumns = [
        { title: '知识点', dataIndex: 'title', ellipsis: true },
        { title: '答题数', dataIndex: 'answerCount', width: 90, align: 'right' },
        { title: '错误数', dataIndex: 'wrongCount', width: 90, align: 'right' },
        {
            title: '错误率',
            dataIndex: 'wrongRate',
            width: 90,
            align: 'right',
            render: (v) => fmtRate(v),
        },
        { title: '覆盖学员', dataIndex: 'userCount', width: 100, align: 'right' },
    ];
    const dailyColumns = [
        { title: '日期', dataIndex: 'bizDate', width: 120 },
        { title: '答题数', dataIndex: 'answerCount', width: 90, align: 'right' },
        { title: '正确数', dataIndex: 'rightCount', width: 90, align: 'right' },
        {
            title: '正确率',
            dataIndex: 'accuracyRate',
            width: 90,
            align: 'right',
            render: (v) => fmtRate(v),
        },
        { title: '练习人数', dataIndex: 'userCount', width: 100, align: 'right' },
        { title: '练习次数', dataIndex: 'paperCount', width: 100, align: 'right' },
    ];
    return (_jsxs("div", { children: [_jsxs(Space, { wrap: true, style: { marginBottom: 16 }, children: [_jsx(RangePicker, { value: range, allowClear: false, onChange: (vals) => {
                            if (vals?.[0] && vals?.[1]) {
                                setRange([vals[0], vals[1]]);
                                setPage(1);
                            }
                        } }), _jsxs(Space, { children: [_jsx(Text, { type: "secondary", children: "\u4EC5\u7EDF\u8BA1\u7EC3\u4E60\u8303\u56F4\u5185" }), _jsx(Switch, { checked: inScopeOnly, onChange: (v) => {
                                    setInScopeOnly(v);
                                    setPage(1);
                                } })] }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exporting, onClick: () => handleExport(false), children: "\u5BFC\u51FA\u5F53\u524D\u533A\u95F4" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exporting, onClick: () => handleExport(true), children: "\u5BFC\u51FA\u5168\u90E8" })] }), summaryQuery.isLoading ? (_jsx("div", { style: { textAlign: 'center', padding: 40 }, children: _jsx(Spin, {}) })) : (_jsxs(_Fragment, { children: [summary?.hint ? (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: summary.hint })) : null, _jsxs(Row, { gutter: [16, 16], style: { marginBottom: 16 }, children: [_jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u5728\u518C\u5B66\u5458", value: summary?.rosterCount ?? 0 }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u5DF2\u5F00 App", value: summary?.withAccountCount ?? 0 }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u6709\u5237\u9898\u4EBA\u6570", value: summary?.activeUserCount ?? 0 }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u53C2\u4E0E\u7387", value: fmtRate(summary?.participationRate) }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u7B54\u9898\u603B\u6570", value: summary?.answerCount ?? 0 }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u6B63\u786E\u7387", value: fmtRate(summary?.accuracyRate) }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u4EBA\u5747\u7B54\u9898", value: summary?.avgAnswersPerActive ?? 0 }) }), _jsx(Col, { xs: 12, sm: 8, md: 6, children: _jsx(Statistic, { title: "\u8FD17\u65E5\u6D3B\u8DC3", value: summary?.activeUserCount7d ?? 0 }) })] })] })), _jsx(Tabs, { items: [
                    {
                        key: 'students',
                        label: '学员明细',
                        children: (_jsxs(_Fragment, { children: [_jsx(Input.Search, { allowClear: true, placeholder: "\u59D3\u540D / \u624B\u673A / \u8EAB\u4EFD\u8BC1", style: { width: 280, marginBottom: 12 }, onSearch: (v) => {
                                        setKeyword(v);
                                        setPage(1);
                                    } }), _jsx(Table, { size: "small", rowKey: "peopleId", loading: studentsQuery.isLoading, columns: studentColumns, dataSource: studentsQuery.data?.items ?? [], scroll: { x: 1100 }, pagination: {
                                        current: page,
                                        pageSize,
                                        total: studentsQuery.data?.total ?? 0,
                                        showSizeChanger: true,
                                        onChange: (p, ps) => {
                                            if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                                                setPageSize(ps);
                                                setPage(1);
                                            }
                                            else {
                                                setPage(p);
                                            }
                                        },
                                    } })] })),
                    },
                    {
                        key: 'knowledges',
                        label: '薄弱知识点',
                        children: (_jsx(Table, { size: "small", rowKey: "knowledgeId", loading: knowledgesQuery.isLoading, columns: knowledgeColumns, dataSource: knowledgesQuery.data?.items ?? [], pagination: false, locale: { emptyText: '暂无知识点刷题数据' } })),
                    },
                    {
                        key: 'daily',
                        label: '每日刷题',
                        children: (_jsx(Table, { size: "small", rowKey: "bizDate", loading: dailyQuery.isLoading, columns: dailyColumns, dataSource: dailyQuery.data?.items ?? [], pagination: false, locale: { emptyText: '所选日期内暂无刷题记录' } })),
                    },
                ] })] }));
}
export function ClassPracticeAnalyticsModal({ open, onClose, classId, classTitle }) {
    return (_jsx(Modal, { title: classTitle ? `刷题学情：${classTitle}` : '班级刷题学情', open: open, onCancel: onClose, width: 1080, footer: null, destroyOnHidden: true, children: _jsx(ClassPracticeAnalyticsPanel, { classId: classId, enabled: open }) }));
}

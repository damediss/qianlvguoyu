import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Card, Col, Empty, Input, Pagination, Row, Space, Spin, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import { FollowClassWorkbench } from './follow/FollowClassWorkbench';
import './TrainingFollowClassPage.css';
/** 班级工种等级：5=五级 … 1=一级（与开班准备一致） */
const CLASS_LEVEL_LABELS = {
    5: '五级/初级工',
    4: '四级/中级工',
    3: '三级/高级工',
    2: '二级/技师',
    1: '一级/高级技师',
};
function formatClassLevel(level) {
    if (level == null || Number.isNaN(Number(level)))
        return '';
    const n = Number(level);
    return CLASS_LEVEL_LABELS[n] ?? `${n}级`;
}
function formatWorkTypeWithLevel(row) {
    const workType = row.work_type_title?.trim() || '';
    const level = formatClassLevel(row.work_type_level);
    if (workType && level)
        return `${workType}（${level}）`;
    if (workType)
        return workType;
    if (level)
        return level;
    return '—';
}
export function TrainingFollowClassPage() {
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [activeClass, setActiveClass] = useState(null);
    const { data, isLoading } = useQuery({
        queryKey: ['follow-class-list', { page, keyword }],
        queryFn: () => trainApi.pageInfoClassList({
            page,
            pageSize: 20,
            keyword: keyword.trim() || undefined,
        }),
        enabled: !activeClass,
    });
    const rows = useMemo(() => data?.list ?? [], [data?.list]);
    if (activeClass) {
        return (_jsx("div", { className: "training-list-container training-follow-class-page", children: _jsx(FollowClassWorkbench, { classItem: activeClass, onBack: () => setActiveClass(null) }) }));
    }
    return (_jsxs("div", { className: "training-list-container training-follow-class-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { marginBottom: 4 }, children: "\u8DDF\u73ED" }), _jsx(Typography.Text, { type: "secondary", children: "\u5F00\u73ED\u540E\u8FC7\u7A0B\u7BA1\u63A7\uFF1A\u9009\u62E9\u73ED\u7EA7\u8FDB\u5165\u5DE5\u4F5C\u53F0\uFF08\u8003\u52E4\u3001\u89D2\u8272\u3001\u65E5\u5FD7\u7B49\uFF09" })] }) }), _jsx(TrainingProcessSteps, { current: 5 }), _jsxs(Card, { size: "small", title: "\u5728\u57F9\u73ED\u7EA7", extra: _jsx(Space, { children: _jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u73ED\u7EA7 / \u4F01\u4E1A / \u5DE5\u79CD", style: { width: 260 }, onSearch: (v) => {
                            setKeyword(v);
                            setPage(1);
                        } }) }), children: [_jsx(Spin, { spinning: isLoading, children: rows.length === 0 && !isLoading ? (_jsx(Empty, { description: "\u6682\u65E0\u73ED\u7EA7\u6570\u636E" })) : (_jsx(Row, { gutter: [16, 16], className: "follow-class-card-grid", children: rows.map((row) => (_jsx(Col, { xs: 24, sm: 12, md: 8, lg: 6, children: _jsx("button", { type: "button", className: "follow-class-card", onClick: () => setActiveClass(row), children: _jsxs("div", { className: "follow-class-card-body", children: [_jsx(Typography.Text, { className: "follow-class-card-title", children: row.title || '未命名班级' }), _jsx(Typography.Text, { type: "secondary", className: "follow-class-card-worktype", ellipsis: { tooltip: formatWorkTypeWithLevel(row) }, children: formatWorkTypeWithLevel(row) }), _jsx(Typography.Text, { type: "secondary", className: "follow-class-card-company", ellipsis: { tooltip: row.company_name || '—' }, children: row.company_name || '—' }), _jsxs(Typography.Text, { type: "secondary", className: "follow-class-card-meta", children: [row.nums ?? 0, " \u4EBA"] })] }) }) }, String(row.id)))) })) }), (data?.total ?? 0) > 0 && (_jsx("div", { className: "follow-class-card-pagination", children: _jsx(Pagination, { current: page, pageSize: 20, total: data?.total ?? 0, showTotal: (total) => `共 ${total} 个班级`, onChange: setPage, showSizeChanger: false }) }))] })] }));
}

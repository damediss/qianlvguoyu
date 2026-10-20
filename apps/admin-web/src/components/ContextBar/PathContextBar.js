import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CloseOutlined, EnvironmentOutlined, FilterOutlined, BarChartOutlined } from '@ant-design/icons';
import { Card, Space, Tag, Typography } from 'antd';
const { Text } = Typography;
const SOURCE_LABEL = {
    '0': '内部',
    '1': '外部',
    '3': '赛前复习',
};
function renderChip(label, value, onRemove) {
    const inner = (_jsxs("span", { children: [label, ": ", _jsx(Text, { strong: true, children: value })] }));
    if (!onRemove)
        return _jsx(Tag, { children: inner });
    return (_jsx(Tag, { closable: true, onClose: onRemove, closeIcon: _jsx(CloseOutlined, {}), children: inner }));
}
export function PathContextBar({ breadcrumb, stats, filters, onFilterRemove }) {
    const pathNodes = [breadcrumb.workType, breadcrumb.course, breadcrumb.knowledge].filter((s) => Boolean(s));
    const sourceLabel = filters.source !== undefined ? SOURCE_LABEL[String(filters.source)] ?? String(filters.source) : undefined;
    const typeLabel = filters.type ?? undefined;
    const difficultyLabel = filters.difficulty ?? undefined;
    const hasAnything = pathNodes.length > 0
        || stats.total > 0
        || filters.source !== undefined
        || filters.type !== undefined
        || filters.difficulty !== undefined;
    if (!hasAnything)
        return null;
    return (_jsx(Card, { size: "small", style: { background: '#fafafa' }, children: _jsxs(Space, { size: "middle", wrap: true, children: [pathNodes.length > 0 && (_jsxs(Space, { size: 4, wrap: true, children: [_jsxs(Text, { type: "secondary", children: [_jsx(EnvironmentOutlined, {}), " \u5F53\u524D\u8303\u56F4:"] }), pathNodes.map((node, idx) => {
                            const isLast = idx === pathNodes.length - 1;
                            return (_jsxs(Space, { size: 4, children: [_jsx(Tag, { color: isLast ? 'blue' : 'default', children: node }), !isLast && _jsx(Text, { type: "secondary", children: "\u203A" })] }, `${node}-${idx}`));
                        })] })), pathNodes.length > 0 && _jsx(Divider, {}), _jsxs(Space, { size: "small", children: [_jsxs(Text, { type: "secondary", children: [_jsx(BarChartOutlined, {}), " \u9898\u76EE\u7EDF\u8BA1"] }), _jsxs(Text, { children: ["\u5171 ", _jsx(Text, { strong: true, children: stats.total }), " \u9898 \u00B7 \u5185\u90E8", ' ', _jsx(Text, { strong: true, children: stats.internal }), " \u00B7 \u5916\u90E8", ' ', _jsx(Text, { strong: true, children: stats.external }), stats.preCompetition !== undefined && (_jsxs(_Fragment, { children: [' ', "\u00B7 \u8D5B\u524D\u590D\u4E60", ' ', _jsx(Text, { strong: true, children: stats.preCompetition })] })), ' ', "\u00B7 \u5DF2\u9009", ' ', _jsx(Text, { strong: true, style: { color: stats.selected > 0 ? '#1677ff' : undefined }, children: stats.selected })] })] }), (filters.source !== undefined || filters.type !== undefined || filters.difficulty !== undefined) && (_jsx(Divider, {})), (filters.source !== undefined
                    || filters.type !== undefined
                    || filters.difficulty !== undefined) && (_jsxs(Space, { size: 4, wrap: true, children: [_jsxs(Text, { type: "secondary", children: [_jsx(FilterOutlined, {}), " \u7B5B\u9009"] }), filters.source !== undefined && sourceLabel
                            ? renderChip('来源', sourceLabel, () => onFilterRemove('source'))
                            : null, filters.type !== undefined && typeLabel
                            ? renderChip('题型', typeLabel, () => onFilterRemove('type'))
                            : null, filters.difficulty !== undefined && difficultyLabel
                            ? renderChip('难度', difficultyLabel, () => onFilterRemove('difficulty'))
                            : null] }))] }) }));
}
function Divider() {
    return (_jsx("span", { "aria-hidden": true, style: {
            display: 'inline-block',
            width: 1,
            height: 18,
            background: '#f0f0f0',
            margin: '0 4px',
        } }));
}

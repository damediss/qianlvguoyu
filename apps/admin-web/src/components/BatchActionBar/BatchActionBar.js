import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Space, Typography } from 'antd';
const { Text } = Typography;
/**
 * 批量操作条:有选中行时出现在表格上方。
 *
 * 视觉:浅蓝底 (#e6f4ff) + 虚线蓝色下边线。
 * 父组件需要在 count > 0 时才挂载(组件本身只负责视觉呈现)。
 */
export function BatchActionBar({ count, onClear, actions }) {
    return (_jsxs("div", { style: {
            background: '#e6f4ff',
            border: '1px dashed #91caff',
            borderRadius: 6,
            padding: '8px 12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 8,
        }, children: [_jsxs(Space, { size: "middle", wrap: true, children: [_jsxs(Text, { children: ["\u5DF2\u9009 ", _jsx(Text, { strong: true, style: { color: '#1677ff' }, children: count }), " \u9898"] }), actions.map((a, idx) => (_jsxs(Space, { size: 4, children: [idx > 0 && _jsx(Text, { type: "secondary", children: "|" }), _jsx(Button, { type: "link", size: "small", danger: a.danger, onClick: a.onClick, style: { padding: 0 }, children: a.label })] }, a.key)))] }), _jsx(Button, { type: "link", size: "small", onClick: onClear, children: "\u53D6\u6D88\u9009\u62E9" })] }));
}

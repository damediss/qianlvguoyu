import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { SwapOutlined } from '@ant-design/icons';
import { Button, Typography } from 'antd';
export function CollectionTaskSelectorBar({ task, loading, onSwitchClick, }) {
    return (_jsxs("div", { className: "collection-task-selector-bar", role: "button", tabIndex: 0, onClick: onSwitchClick, onKeyDown: (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                onSwitchClick();
            }
        }, children: [_jsx(Typography.Text, { className: "collection-task-selector-kicker", type: "secondary", children: "\u5E73\u677F\u4FE1\u606F\u91C7\u96C6\u7AEF" }), _jsxs(Typography.Title, { level: 4, className: "collection-task-selector-name", children: ["\u5F53\u524D\u4EFB\u52A1\uFF1A", task?.taskName || (loading ? '加载中…' : '未选择任务')] }), _jsxs("div", { className: "collection-task-selector-meta", children: [_jsxs("span", { children: ["\u4EFB\u52A1\u7F16\u53F7\uFF1A", task?.taskNo || '-'] }), _jsxs("span", { children: ["\u65E5\u671F\uFF1A", task?.examDate || '-'] }), task ? _jsxs("span", { children: ["\u8003\u751F\uFF1A", task.candidateTotal, " \u4EBA"] }) : null] }), _jsx(Button, { className: "collection-task-switch-button", icon: _jsx(SwapOutlined, {}), loading: loading, onClick: (event) => {
                    event.stopPropagation();
                    onSwitchClick();
                }, children: "\u5207\u6362\u4EFB\u52A1" })] }));
}

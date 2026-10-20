import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Alert, List, Tag, Typography } from 'antd';
export function StageValidationPanel({ issues, onSelectIssue }) {
    const errors = issues.filter((issue) => issue.level === 'error');
    const warnings = issues.filter((issue) => issue.level === 'warning');
    return (_jsxs("div", { className: "stage-validation-panel", "data-testid": "stage-validation-panel", children: [_jsx("div", { className: "stage-validation-panel__summary", children: _jsx(SpaceSummary, { errors: errors.length, warnings: warnings.length }) }), issues.length === 0 ? (_jsx(Alert, { type: "success", showIcon: true, message: "\u672A\u53D1\u73B0\u7F16\u6392\u95EE\u9898", "data-testid": "stage-validation-success" })) : (_jsx(List, { size: "small", dataSource: issues, renderItem: (issue) => (_jsx(List.Item, { children: _jsxs("button", { type: "button", className: "stage-validation-item", onClick: () => onSelectIssue(issue), "data-testid": `stage-validation-${issue.code}`, children: [_jsx(Tag, { color: issue.level === 'error' ? 'error' : 'warning', children: issue.level === 'error' ? '阻断' : '警告' }), _jsx("span", { children: issue.message })] }) })) }))] }));
}
function SpaceSummary({ errors, warnings }) {
    return _jsxs(Typography.Text, { type: "secondary", children: ["\u963B\u65AD ", errors, " \u9879 \u00B7 \u8B66\u544A ", warnings, " \u9879"] });
}

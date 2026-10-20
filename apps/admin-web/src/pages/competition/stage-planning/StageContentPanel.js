import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { BookOutlined, ExperimentOutlined, SettingOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Checkbox, Divider, InputNumber, Progress, Space, Tag, Typography } from 'antd';
import * as React from 'react';
import './stageContentPanel.css';
export const STAGE_CONTENT_OPTIONS = [
    {
        code: 'theory',
        label: '理论考试',
        icon: _jsx(BookOutlined, {}),
        color: '#1677ff',
    },
    {
        code: 'simulation',
        label: '仿真操作',
        icon: _jsx(ExperimentOutlined, {}),
        color: '#722ed1',
        providerHint: '成绩在成绩与排名中按身份证号导入',
    },
    {
        code: 'practical',
        label: '实操考评',
        icon: _jsx(SettingOutlined, {}),
        color: '#fa8c16',
    },
];
export function stageContentTypeLabel(code) {
    return STAGE_CONTENT_OPTIONS.find((item) => item.code === code)?.label || code;
}
function isEnabledContent(content) {
    return !('status' in content) || content.status === 'enabled';
}
export function selectedContentTypes(contents) {
    return contents
        .filter(isEnabledContent)
        .map((item) => item.contentType)
        .filter((code, index, values) => values.indexOf(code) === index);
}
export function weightsFromContents(contents) {
    return contents.reduce((weights, content) => {
        if (isEnabledContent(content)) {
            weights[content.contentType] = Number(content.weight || 0);
        }
        return weights;
    }, { theory: 0, simulation: 0, practical: 0 });
}
export function weightTotal(weights) {
    return Number((weights.theory + weights.simulation + weights.practical).toFixed(2));
}
export function stageContentConfigsAreValid(contents) {
    const selected = contents.map((item) => item.contentType);
    const weights = contents.reduce((result, item) => {
        result[item.contentType] = item.weight;
        return result;
    }, { theory: 0, simulation: 0, practical: 0 });
    if (selected.length === 0)
        return false;
    return selected.every((code) => weights[code] > 0)
        && STAGE_CONTENT_OPTIONS.every((option) => selected.includes(option.code) || weights[option.code] === 0)
        && weightTotal(weights) === 100;
}
export function StageContentPanel({ stage, contents, contentEditable, contentSaving, onSave, onChange, compact = false, showActions = true, }) {
    const [selected, setSelected] = React.useState(() => selectedContentTypes(contents));
    const [weights, setWeights] = React.useState(() => weightsFromContents(contents));
    const [dirty, setDirty] = React.useState(false);
    const total = weightTotal(weights);
    const currentConfigs = () => selected.map((contentType) => ({ contentType, weight: weights[contentType] }));
    const validWeights = stageContentConfigsAreValid(currentConfigs());
    React.useEffect(() => {
        setSelected(selectedContentTypes(contents));
        setWeights(weightsFromContents(contents));
        setDirty(false);
    }, [contents]);
    const changeContent = (code, checked) => {
        const nextSelected = selected.includes(code) === checked
            ? selected
            : checked
                ? [...selected, code]
                : selected.filter((item) => item !== code);
        const nextWeights = { ...weights, [code]: checked ? weights[code] : 0 };
        setSelected(nextSelected);
        setWeights(nextWeights);
        onChange?.(nextSelected.map((contentType) => ({ contentType, weight: nextWeights[contentType] })));
        setDirty(true);
    };
    const changeWeight = (code, value) => {
        const nextWeights = { ...weights, [code]: value ?? 0 };
        setWeights(nextWeights);
        onChange?.(selected.map((contentType) => ({ contentType, weight: nextWeights[contentType] })));
        setDirty(true);
    };
    const resetFromServer = () => {
        setSelected(selectedContentTypes(contents));
        setWeights(weightsFromContents(contents));
        setDirty(false);
    };
    return (_jsxs("div", { className: `stage-content-panel${compact ? ' stage-content-panel--compact' : ''}`, "data-testid": "stage-content-panel", children: [_jsxs("div", { className: "stage-content-panel__header", children: [_jsx("div", { children: _jsx(Typography.Title, { level: 5, style: { margin: 0 }, children: "\u8D5B\u6BB5\u5185\u5BB9" }) }), _jsx(Space, { wrap: true, children: !contentEditable && _jsx(Tag, { children: "\u5F53\u524D\u8D5B\u6BB5\u53EA\u8BFB" }) })] }), _jsx("div", { className: "stage-content-panel__options", children: STAGE_CONTENT_OPTIONS.map((option) => {
                    const checked = selected.includes(option.code);
                    return (_jsxs(Card, { size: "small", hoverable: contentEditable, className: `stage-content-option${checked ? ' stage-content-option--selected' : ''}`, style: checked ? { borderColor: option.color, boxShadow: `0 0 0 2px ${option.color}18` } : undefined, onClick: () => contentEditable && changeContent(option.code, !checked), children: [_jsxs("div", { className: "stage-content-option__topline", children: [_jsx(Checkbox, { checked: checked, disabled: !contentEditable, onChange: (event) => changeContent(option.code, event.target.checked), onClick: (event) => event.stopPropagation() }), _jsx("span", { className: "stage-content-option__icon", style: { color: option.color }, children: option.icon }), _jsx(Typography.Text, { strong: true, children: option.label }), checked && _jsx(Tag, { color: "success", children: "\u5DF2\u542F\u7528" })] }), option.providerHint && _jsx("div", { className: "stage-content-option__hint", children: option.providerHint })] }, option.code));
                }) }), selected.includes('simulation') && (_jsx(Alert, { type: "info", showIcon: true, message: "\u4EFF\u771F\u6210\u7EE9\u901A\u8FC7 Excel \u5BFC\u5165", description: "\u52FE\u9009\u4EFF\u771F\u5E76\u4FDD\u5B58\u6743\u91CD\u540E\u53EF\u4EE5\u786E\u8BA4\u8D5B\u6BB5\u7F16\u6392\u3002\u4EFF\u771F\u5206\u6570\u5728\u6210\u7EE9\u4E0E\u6392\u540D\u4E2D\u6309\u8EAB\u4EFD\u8BC1\u53F7\u5BFC\u5165\uFF0C\u518D\u6309\u5185\u5BB9\u6743\u91CD\u8BA1\u5165\u7EFC\u5408\u6210\u7EE9\u3002" })), (selected.includes('practical') || selected.includes('simulation')) && validWeights && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5B9E\u64CD\u4E0E\u4EFF\u771F\u6210\u7EE9\u6765\u6E90", description: `保存权重后，在成绩与排名中按身份证号导入 ${[
                    selected.includes('practical') ? '实操' : '',
                    selected.includes('simulation') ? '仿真' : '',
                ].filter(Boolean).join('、')} xlsx。系统按身份证匹配选手分数，再按这里的内容权重计算综合成绩与排名。理论成绩仍来自正式考试。` })), _jsx(Divider, {}), _jsxs("div", { className: "stage-content-panel__weight-head", children: [_jsx("div", { children: _jsx(Typography.Title, { level: 5, style: { margin: 0 }, children: "\u5185\u5BB9\u6743\u91CD\u5360\u6BD4" }) }), _jsxs(Space, { size: 16, align: "center", children: [_jsx(Progress, { type: "circle", percent: Math.min(total, 100), size: 46, status: total === 100 ? 'success' : total > 100 ? 'exception' : 'normal', format: () => `${total}%` }), _jsxs("div", { children: [_jsxs(Typography.Text, { type: total === 100 ? 'success' : 'danger', strong: true, children: ["\u6743\u91CD\u5408\u8BA1 ", total, "%"] }), _jsx("div", { children: _jsx(Typography.Text, { type: "secondary", children: validWeights ? '配置有效' : '待完善配置' }) })] })] })] }), selected.length === 0 && (_jsx(Alert, { type: "warning", showIcon: true, message: "\u8BF7\u81F3\u5C11\u542F\u7528\u4E00\u9879\u6BD4\u8D5B\u5185\u5BB9\u3002" })), _jsx("div", { className: "stage-content-weight-list", children: STAGE_CONTENT_OPTIONS.map((option) => {
                    const enabled = selected.includes(option.code);
                    return (_jsxs("div", { className: `stage-content-weight-row${enabled ? '' : ' stage-content-weight-row--disabled'}`, children: [_jsxs("div", { className: "stage-content-weight-row__label", children: [_jsx("span", { className: "stage-content-option__icon", style: { color: option.color }, children: option.icon }), _jsx(Typography.Text, { strong: enabled, children: option.label }), !enabled && _jsx(Typography.Text, { type: "secondary", children: "\u672A\u542F\u7528" })] }), _jsx(InputNumber, { min: 0, max: 100, precision: 2, step: 5, value: weights[option.code], disabled: !enabled || !contentEditable, onChange: (value) => changeWeight(option.code, value), addonAfter: "%", style: { width: 150 }, "data-testid": `stage-content-weight-${option.code}` })] }, option.code));
                }) }), selected.length > 0 && total !== 100 && (_jsx(Alert, { type: "error", showIcon: true, message: `当前权重合计为 ${total}%，还差 ${total > 100 ? total - 100 : 100 - total}%。` })), showActions && (_jsx("div", { className: "stage-content-panel__actions", children: _jsxs(Space, { children: [contentEditable && onSave && (_jsx(Button, { type: "primary", loading: contentSaving, disabled: !dirty || selected.length === 0 || !validWeights, onClick: () => onSave(currentConfigs()), "data-testid": "stage-content-save", children: "\u4FDD\u5B58\u8D5B\u6BB5\u914D\u7F6E" })), dirty && _jsx(Button, { onClick: resetFromServer, children: "\u64A4\u9500\u4FEE\u6539" })] }) }))] }));
}

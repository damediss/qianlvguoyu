import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Typography } from 'antd';
import { STAGE_DEFINITIONS, stageTypeDisplayOrder } from '../../../types/competitionStagePlanning';
import './stagePlanning.css';
export function StageNodeLibrary({ usedTypes, editable }) {
    return (_jsxs("aside", { className: "stage-planning-library", "data-testid": "stage-node-library", children: [_jsx(Typography.Title, { level: 5, children: "\u8282\u70B9\u5E93" }), _jsx(Typography.Paragraph, { type: "secondary", className: "stage-planning-library__hint", children: "\u6BCF\u79CD\u9884\u8BBE\u8D5B\u6BB5\u5F53\u524D\u5DE5\u79CD\u5F53\u524D\u7B49\u7EA7\u6700\u591A\u914D\u7F6E\u4E00\u6B21\u3002" }), _jsx("div", { className: "stage-planning-library__items", children: STAGE_DEFINITIONS.map((definition) => {
                    const used = usedTypes.includes(definition.stageType);
                    const disabled = !editable || used;
                    const onDragStart = (event) => {
                        if (disabled) {
                            event.preventDefault();
                            return;
                        }
                        event.dataTransfer.effectAllowed = 'move';
                        event.dataTransfer.setData('application/x-competition-stage-type', definition.stageType);
                    };
                    return (_jsxs("div", { className: `stage-library-item${disabled ? ' is-disabled' : ''}`, draggable: !disabled, onDragStart: onDragStart, "data-testid": `stage-library-${definition.stageType}`, "aria-disabled": disabled, children: [_jsx("span", { className: "stage-library-item__order", children: stageTypeDisplayOrder(definition.stageType) }), _jsxs("span", { children: [_jsx("strong", { children: definition.name }), _jsx("small", { children: used ? '已添加' : '拖入画布' })] })] }, definition.stageType));
                }) }), !editable && _jsx(Typography.Text, { type: "warning", children: "\u5F53\u524D\u7F16\u6392\u4F5C\u7528\u57DF\u53EA\u8BFB\uFF0C\u8282\u70B9\u5E93\u4E0D\u53EF\u7F16\u8F91\u3002" })] }));
}

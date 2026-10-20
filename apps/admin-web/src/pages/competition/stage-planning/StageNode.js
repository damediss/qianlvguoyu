import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Handle, Position } from '@xyflow/react';
import { Tag } from 'antd';
import { stageStatusLabel, stageTypeDefinition, stageTypeDisplayOrder } from '../../../types/competitionStagePlanning';
export function StageNode({ id, data, selected, stagePlanConfirmed = false, }) {
    if (data.kind === 'marker') {
        return (_jsxs("div", { className: "stage-marker-node", "data-testid": `stage-marker-${data.marker}`, children: [_jsx("span", { className: "stage-marker-node__dot" }), _jsx("span", { children: data.label })] }));
    }
    const definition = stageTypeDefinition(data.stageType);
    const displayOrder = stageTypeDisplayOrder(data.stageType);
    return (_jsxs("div", { className: `stage-canvas-node${selected ? ' is-selected' : ''}`, "data-testid": `stage-node-${data.stageType}`, "data-stage-id": data.stageId ? String(data.stageId) : undefined, children: [_jsx(Handle, { id: `${id}-target`, type: "target", position: Position.Left, "data-testid": `stage-handle-${data.stageType}-target` }), _jsxs("div", { className: "stage-canvas-node__header", children: [_jsx("span", { className: "stage-canvas-node__index", children: displayOrder ?? data.sortOrder }), _jsx("strong", { children: definition?.name || data.name }), _jsx(Tag, { children: stageStatusLabel(data.status, stagePlanConfirmed) })] }), _jsxs("div", { className: "stage-canvas-node__body", children: [_jsx("span", { children: data.startAt && data.endAt ? `${data.startAt.replace('T', ' ').slice(0, 16)} — ${data.endAt.replace('T', ' ').slice(0, 16)}` : '时间待补充' }), _jsx("span", { className: "stage-canvas-node__description", children: data.description || '暂无说明' })] }), _jsx(Handle, { id: `${id}-source`, type: "source", position: Position.Right, "data-testid": `stage-handle-${data.stageType}-source` })] }));
}

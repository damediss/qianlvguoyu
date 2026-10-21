import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { BaseEdge, EdgeLabelRenderer, getBezierPath } from '@xyflow/react';
export function StageEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }) {
    const [path, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition });
    return (_jsxs(_Fragment, { children: [_jsx(BaseEdge, { id: id, path: path, markerEnd: "url(#stage-edge-arrow)", className: `${data?.business ? 'stage-edge' : 'stage-edge stage-edge--marker'}${selected ? ' is-selected' : ''}`, "data-testid": `stage-edge-${id.replace(/^edge:/, '')}` }), data?.business && data.note && (_jsx(EdgeLabelRenderer, { children: _jsx("div", { className: `stage-edge-label${selected ? ' is-selected' : ''}`, style: { transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)` }, "data-testid": `stage-edge-note-${id.replace(/^edge:/, '')}`, children: data.note }) }))] }));
}

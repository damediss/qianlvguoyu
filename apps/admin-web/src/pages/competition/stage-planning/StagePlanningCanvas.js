import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Background, Controls, MiniMap, ReactFlow, ReactFlowProvider, useReactFlow, } from '@xyflow/react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import '@xyflow/react/dist/style.css';
import { StageEdge as StageEdgeComponent } from './StageEdge';
import { StageContextMenu } from './StageContextMenu';
import { StageNode as StageNodeComponent } from './StageNode';
import './stagePlanning.css';
const edgeTypes = { stage: StageEdgeComponent };
function StagePlanningCanvasInner({ nodes, edges, editable, stagePlanConfirmed, onNodesChange, onEdgesChange, onConnect, onSelectionChange, onAddStage, onStageContextMenu, onStageContentTypeClick, onStageCandidateClick, onStageRefereeClick, candidateEnabled, refereeEnabled, stageContentTypesByNodeId, onFitViewReady }) {
    const { screenToFlowPosition, fitView } = useReactFlow();
    const [contextMenu, setContextMenu] = useState(null);
    const nodeTypes = useMemo(() => ({
        stage: (props) => _jsx(StageNodeComponent, { ...props, stagePlanConfirmed: stagePlanConfirmed }),
        marker: (props) => _jsx(StageNodeComponent, { ...props, stagePlanConfirmed: stagePlanConfirmed }),
    }), [stagePlanConfirmed]);
    const closeContextMenu = useCallback(() => setContextMenu(null), []);
    const handleNodeContextMenu = useCallback((event, node) => {
        if (node.data.kind !== 'stage')
            return;
        event.preventDefault();
        event.stopPropagation();
        onStageContextMenu(node.id);
        setContextMenu({
            nodeId: node.id,
            position: { x: event.clientX, y: event.clientY },
        });
    }, [onStageContextMenu]);
    const contextMenuNode = contextMenu
        ? nodes.find((node) => node.id === contextMenu.nodeId)
        : undefined;
    const enabledContentTypes = contextMenuNode?.data.kind === 'stage'
        ? contextMenuNode.data.contents?.map((content) => content.contentType)
            ?? stageContentTypesByNodeId?.[contextMenuNode.id]
            ?? []
        : [];
    const onDrop = (event) => {
        event.preventDefault();
        const stageType = event.dataTransfer.getData('application/x-competition-stage-type');
        if (!stageType || !editable)
            return;
        onAddStage(stageType, screenToFlowPosition({ x: event.clientX, y: event.clientY }));
    };
    useEffect(() => {
        onFitViewReady(() => fitView({ padding: 0.2, duration: 180 }));
    }, [fitView, onFitViewReady]);
    return (_jsxs("div", { className: "stage-planning-canvas", "data-testid": "stage-planning-canvas", onDrop: onDrop, onDragOver: (event) => event.preventDefault(), children: [_jsx("svg", { className: "stage-planning-svg-defs", "aria-hidden": "true", children: _jsx("defs", { children: _jsx("marker", { id: "stage-edge-arrow", markerWidth: "10", markerHeight: "10", refX: "8", refY: "3", orient: "auto", markerUnits: "strokeWidth", children: _jsx("path", { d: "M0,0 L0,6 L9,3 z", fill: "currentColor" }) }) }) }), _jsxs(ReactFlow, { nodes: nodes, edges: edges, nodeTypes: nodeTypes, edgeTypes: edgeTypes, onNodesChange: onNodesChange, onEdgesChange: onEdgesChange, onConnect: onConnect, onSelectionChange: onSelectionChange, onNodeContextMenu: handleNodeContextMenu, onPaneClick: closeContextMenu, onPaneContextMenu: (event) => {
                    event.preventDefault();
                    closeContextMenu();
                }, onMoveStart: closeContextMenu, nodesDraggable: editable, nodesConnectable: editable, edgesReconnectable: editable, elementsSelectable: true, deleteKeyCode: editable ? ['Backspace', 'Delete'] : null, fitView: true, fitViewOptions: { padding: 0.2 }, selectionOnDrag: false, panOnDrag: true, zoomOnScroll: true, proOptions: { hideAttribution: true }, children: [_jsx(Background, { gap: 24, size: 1, color: "#d9e2ec" }), _jsx(Controls, { showInteractive: false }), _jsx(MiniMap, { pannable: true, zoomable: true, nodeColor: (node) => node.type === 'marker' ? '#94a3b8' : '#1677ff' })] }), contextMenu && (_jsx(StageContextMenu, { position: contextMenu.position, enabledContentTypes: enabledContentTypes, candidateEnabled: candidateEnabled, refereeEnabled: refereeEnabled, onClose: closeContextMenu, onContentTypeClick: (contentType, practicalTab) => {
                    const nodeId = contextMenu.nodeId;
                    closeContextMenu();
                    onStageContentTypeClick(nodeId, contentType, practicalTab);
                }, onCandidateClick: (contentType) => {
                    const nodeId = contextMenu.nodeId;
                    closeContextMenu();
                    onStageCandidateClick?.(nodeId, contentType);
                }, onRefereeClick: () => {
                    const nodeId = contextMenu.nodeId;
                    closeContextMenu();
                    onStageRefereeClick?.(nodeId);
                } }))] }));
}
export function StagePlanningCanvas(props) {
    return (_jsx(ReactFlowProvider, { children: _jsx(StagePlanningCanvasInner, { ...props }) }));
}

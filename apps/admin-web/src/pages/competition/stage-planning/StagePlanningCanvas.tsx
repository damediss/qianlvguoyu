import {
  Background,
  Controls,
  MiniMap,
  ReactFlow,
  ReactFlowProvider,
  useReactFlow,
  type Connection,
  type EdgeChange,
  type NodeChange,
  type NodeMouseHandler,
  type NodeProps,
  type OnSelectionChangeParams,
} from '@xyflow/react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import '@xyflow/react/dist/style.css';
import type { CompetitionStageContentType } from '../../../types/competition';
import type { StageEdge, StageNode, StageType } from '../../../types/competitionStagePlanning';
import { StageEdge as StageEdgeComponent } from './StageEdge';
import { StageContextMenu } from './StageContextMenu';
import { StageNode as StageNodeComponent } from './StageNode';
import './stagePlanning.css';

const edgeTypes = { stage: StageEdgeComponent };

interface StagePlanningCanvasProps {
  nodes: StageNode[];
  edges: StageEdge[];
  editable: boolean;
  stagePlanConfirmed: boolean;
  onNodesChange: (changes: NodeChange<StageNode>[]) => void;
  onEdgesChange: (changes: EdgeChange<StageEdge>[]) => void;
  onConnect: (connection: Connection) => void;
  onSelectionChange: (selection: OnSelectionChangeParams) => void;
  onAddStage: (stageType: StageType, position: { x: number; y: number }) => void;
  onStageContextMenu: (nodeId: string) => void;
  onStageContentTypeClick: (nodeId: string, contentType: CompetitionStageContentType) => void;
  onStageCandidateClick?: (nodeId: string) => void;
  candidateEnabled?: boolean;
  stageContentTypesByNodeId?: Readonly<Record<string, readonly CompetitionStageContentType[]>>;
  onFitViewReady: (fitView: () => void) => void;
}

function StagePlanningCanvasInner({ nodes, edges, editable, stagePlanConfirmed, onNodesChange, onEdgesChange, onConnect, onSelectionChange, onAddStage, onStageContextMenu, onStageContentTypeClick, onStageCandidateClick, candidateEnabled, stageContentTypesByNodeId, onFitViewReady }: StagePlanningCanvasProps) {
  const { screenToFlowPosition, fitView } = useReactFlow<StageNode, StageEdge>();
  const [contextMenu, setContextMenu] = useState<{
    nodeId: string;
    position: { x: number; y: number };
  } | null>(null);
  const nodeTypes = useMemo(() => ({
    stage: (props: NodeProps<StageNode>) => <StageNodeComponent {...props} stagePlanConfirmed={stagePlanConfirmed} />,
    marker: (props: NodeProps<StageNode>) => <StageNodeComponent {...props} stagePlanConfirmed={stagePlanConfirmed} />,
  }), [stagePlanConfirmed]);

  const closeContextMenu = useCallback(() => setContextMenu(null), []);
  const handleNodeContextMenu: NodeMouseHandler<StageNode> = useCallback((event, node) => {
    if (node.data.kind !== 'stage') return;
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

  const onDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    const stageType = event.dataTransfer.getData('application/x-competition-stage-type') as StageType;
    if (!stageType || !editable) return;
    onAddStage(stageType, screenToFlowPosition({ x: event.clientX, y: event.clientY }));
  };

  useEffect(() => {
    onFitViewReady(() => fitView({ padding: 0.2, duration: 180 }));
  }, [fitView, onFitViewReady]);

  return (
    <div className="stage-planning-canvas" data-testid="stage-planning-canvas" onDrop={onDrop} onDragOver={(event) => event.preventDefault()}>
      <svg className="stage-planning-svg-defs" aria-hidden="true">
        <defs>
          <marker id="stage-edge-arrow" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
            <path d="M0,0 L0,6 L9,3 z" fill="currentColor" />
          </marker>
        </defs>
      </svg>
      <ReactFlow<StageNode, StageEdge>
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onSelectionChange={onSelectionChange}
        onNodeContextMenu={handleNodeContextMenu}
        onPaneClick={closeContextMenu}
        onPaneContextMenu={(event) => {
          event.preventDefault();
          closeContextMenu();
        }}
        onMoveStart={closeContextMenu}
        nodesDraggable={editable}
        nodesConnectable={editable}
        edgesReconnectable={editable}
        elementsSelectable
        deleteKeyCode={editable ? ['Backspace', 'Delete'] : null}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        selectionOnDrag={false}
        panOnDrag
        zoomOnScroll
        proOptions={{ hideAttribution: true }}
      >
        <Background gap={24} size={1} color="#d9e2ec" />
        <Controls showInteractive={false} />
        <MiniMap pannable zoomable nodeColor={(node) => node.type === 'marker' ? '#94a3b8' : '#1677ff'} />
      </ReactFlow>
      {contextMenu && (
        <StageContextMenu
          position={contextMenu.position}
          enabledContentTypes={enabledContentTypes}
          candidateEnabled={candidateEnabled}
          onClose={closeContextMenu}
          onContentTypeClick={(contentType) => {
            const nodeId = contextMenu.nodeId;
            closeContextMenu();
            onStageContentTypeClick(nodeId, contentType);
          }}
          onCandidateClick={() => {
            const nodeId = contextMenu.nodeId;
            closeContextMenu();
            onStageCandidateClick?.(nodeId);
          }}
        />
      )}
    </div>
  );
}

export function StagePlanningCanvas(props: StagePlanningCanvasProps) {
  return (
    <ReactFlowProvider>
      <StagePlanningCanvasInner {...props} />
    </ReactFlowProvider>
  );
}

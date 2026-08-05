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
  type NodeProps,
  type OnSelectionChangeParams,
} from '@xyflow/react';
import { useEffect, useMemo } from 'react';
import '@xyflow/react/dist/style.css';
import type { StageEdge, StageNode, StageType } from '../../../types/competitionStagePlanning';
import { StageEdge as StageEdgeComponent } from './StageEdge';
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
  onFitViewReady: (fitView: () => void) => void;
}

function StagePlanningCanvasInner({ nodes, edges, editable, stagePlanConfirmed, onNodesChange, onEdgesChange, onConnect, onSelectionChange, onAddStage, onFitViewReady }: StagePlanningCanvasProps) {
  const { screenToFlowPosition, fitView } = useReactFlow<StageNode, StageEdge>();
  const nodeTypes = useMemo(() => ({
    stage: (props: NodeProps<StageNode>) => <StageNodeComponent {...props} stagePlanConfirmed={stagePlanConfirmed} />,
    marker: (props: NodeProps<StageNode>) => <StageNodeComponent {...props} stagePlanConfirmed={stagePlanConfirmed} />,
  }), [stagePlanConfirmed]);

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

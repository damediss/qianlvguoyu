import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  type Connection,
  type EdgeChange,
  type NodeChange,
  type OnSelectionChangeParams,
} from '@xyflow/react';
import type { CompetitionId, CompetitionStage } from '../../../types/competition';
import {
  autoLayoutStageGraph,
  buildStageSavePayload,
  canConnectStage,
  cleanStageNotes,
  toStageGraph,
  updateStageNodeData,
  validateStageGraph,
} from './stageGraphUtils';
import type {
  StageBusinessNodeData,
  StageEdge,
  StageGraphSelection,
  StageNode,
  StageType,
  StageValidationIssue,
} from '../../../types/competitionStagePlanning';
import {
  stageEdgeId,
  stageNodeId,
  stageNodePosition,
  stageNoteKey,
  stageTypeDefinition,
} from '../../../types/competitionStagePlanning';

function stagesSignature(stages: CompetitionStage[]) {
  return JSON.stringify(stages.map((stage) => ({
    id: String(stage.id),
    stageType: stage.stageType,
    sortOrder: stage.sortOrder,
    startAt: stage.startAt,
    endAt: stage.endAt,
    status: stage.status,
    description: stage.description,
  })));
}

function edgeNotes(nodes: StageNode[], edges: StageEdge[]) {
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const result: Record<string, string> = {};
  edges.filter((edge) => edge.data?.business).forEach((edge) => {
    const source = nodeMap.get(edge.source);
    const target = nodeMap.get(edge.target);
    if (source?.data.kind === 'stage' && target?.data.kind === 'stage') {
      const key = stageNoteKey(source.data.stageType, target.data.stageType);
      if (edge.data?.note) result[key] = edge.data.note;
    }
  });
  return result;
}

export function useStagePlanningDraft({
  eventId,
  serverStages,
  stageCatalog = serverStages,
  initialNotes,
  editable,
  eventStartAt,
  eventEndAt,
}: {
  eventId: string;
  serverStages: CompetitionStage[];
  stageCatalog?: CompetitionStage[];
  initialNotes: Record<string, string>;
  editable: boolean;
  eventStartAt?: string | null;
  eventEndAt?: string | null;
}) {
  const signature = stagesSignature(serverStages);
  const [nodes, setNodes] = useState<StageNode[]>([]);
  const [edges, setEdges] = useState<StageEdge[]>([]);
  const [baselineStages, setBaselineStages] = useState<CompetitionStage[]>(serverStages);
  const [baselineNotes, setBaselineNotes] = useState<Record<string, string>>(initialNotes);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [issues, setIssues] = useState<StageValidationIssue[]>([]);
  const [selection, setSelection] = useState<StageGraphSelection>({});
  const baselineSignatureRef = useRef('');
  const eventIdRef = useRef(eventId);

  const resetFromServer = useCallback((stages: CompetitionStage[], notes: Record<string, string>) => {
    const graph = toStageGraph(stages, notes);
    setNodes(graph.nodes);
    setEdges(graph.edges);
    setBaselineStages(stages);
    setBaselineNotes(notes);
    setDirty(false);
    setIssues([]);
    setSelection({});
    baselineSignatureRef.current = stagesSignature(stages);
  }, []);

  useEffect(() => {
    const eventChanged = eventIdRef.current !== eventId;
    if (eventChanged || (!dirty && baselineSignatureRef.current !== signature)) {
      eventIdRef.current = eventId;
      resetFromServer(serverStages, initialNotes);
    }
  }, [dirty, eventId, initialNotes, resetFromServer, serverStages, signature]);

  const markDirty = useCallback(() => {
    if (editable) setDirty(true);
  }, [editable]);

  const onNodesChange = useCallback((changes: NodeChange<StageNode>[]) => {
    setNodes((current) => {
      const next = applyNodeChanges<StageNode>(changes, current);
      const removed = new Set(changes.filter((change) => change.type === 'remove').map((change) => change.id));
      if (removed.size > 0) {
        setEdges((currentEdges) => currentEdges.filter((edge) => !removed.has(edge.source) && !removed.has(edge.target)));
      }
      if (changes.some((change) => change.type === 'add'
        || change.type === 'remove'
        || change.type === 'replace'
        || change.type === 'position')) markDirty();
      return next;
    });
  }, [markDirty]);

  const onEdgesChange = useCallback((changes: EdgeChange<StageEdge>[]) => {
    setEdges((current) => applyEdgeChanges<StageEdge>(changes, current));
    if (changes.some((change) => change.type !== 'select')) markDirty();
  }, [markDirty]);

  const onConnect = useCallback((connection: Connection) => {
    if (!editable) return;
    setNodes((currentNodes) => {
      setEdges((currentEdges) => {
        if (!canConnectStage(connection, currentNodes, currentEdges)) return currentEdges;
        const source = currentNodes.find((node) => node.id === connection.source);
        const target = currentNodes.find((node) => node.id === connection.target);
        if (source?.data.kind !== 'stage' || target?.data.kind !== 'stage') return currentEdges;
        const nextEdge: StageEdge = {
          id: stageEdgeId(source.data.stageType, target.data.stageType),
          source: connection.source,
          target: connection.target,
          sourceHandle: connection.sourceHandle,
          targetHandle: connection.targetHandle,
          type: 'stage',
          data: { business: true, note: '' },
        };
        markDirty();
        return addEdge(nextEdge, currentEdges) as StageEdge[];
      });
      return currentNodes;
    });
  }, [editable, markDirty]);

  const onSelectionChange = useCallback((next: OnSelectionChangeParams) => {
    setSelection({ nodeId: next.nodes[0]?.id, edgeId: next.edges[0]?.id });
  }, []);

  const addStage = useCallback((stageType: StageType, position?: { x: number; y: number }) => {
    if (!editable || nodes.some((node) => node.data.kind === 'stage' && node.data.stageType === stageType)) return;
    const definition = stageTypeDefinition(stageType);
    if (!definition) return;
    const catalogStage = stageCatalog.find((stage) => stage.stageType === stageType);
    const stageNode: StageNode = {
      id: stageNodeId(stageType),
      type: 'stage',
      position: position || stageNodePosition(nodes.filter((node) => node.data.kind === 'stage').length),
      data: {
        kind: 'stage',
        stageType,
        stageId: catalogStage?.id,
        name: definition.name,
        sortOrder: definition.sortOrder,
        status: catalogStage?.status ?? 'draft',
        startAt: catalogStage?.startAt ?? null,
        endAt: catalogStage?.endAt ?? null,
        description: catalogStage?.description ?? '',
      },
    };
    setNodes((current) => [...current, stageNode]);
    markDirty();
  }, [editable, markDirty, nodes, stageCatalog]);

  const updateNode = useCallback((nodeId: string, patch: Partial<StageBusinessNodeData>) => {
    if (!editable) return;
    setNodes((current) => current.map((node) => updateStageNodeData(node, node.id === nodeId ? patch : {})));
    markDirty();
  }, [editable, markDirty]);

  const updateEdge = useCallback((edgeId: string, patch: { note?: string }) => {
    if (!editable) return;
    setEdges((current) => current.map((edge) => edge.id === edgeId
      ? { ...edge, data: { business: edge.data?.business ?? true, ...patch } }
      : edge));
    markDirty();
  }, [editable, markDirty]);

  const autoLayout = useCallback(() => {
    if (!editable) return;
    setNodes((current) => autoLayoutStageGraph(current));
    markDirty();
  }, [editable, markDirty]);

  const validate = useCallback(() => {
    const result = validateStageGraph(nodes, edges, eventStartAt, eventEndAt);
    setIssues(result);
    return result;
  }, [edges, eventEndAt, eventStartAt, nodes]);

  const discard = useCallback(() => {
    resetFromServer(baselineStages, baselineNotes);
  }, [baselineNotes, baselineStages, resetFromServer]);

  const acceptSavedStages = useCallback((savedStages: CompetitionStage[], savedNotes: Record<string, string>) => {
    const currentPositions = new Map(nodes.map((node) => [node.id, node.position]));
    const currentContents = new Map<string, StageBusinessNodeData['contents']>(
      nodes
        .filter((node) => node.data.kind === 'stage' && node.data.contents !== undefined)
        .map((node) => [node.data.stageType, node.data.contents] as [string, StageBusinessNodeData['contents']]),
    );
    const graph = toStageGraph(savedStages, savedNotes);
    setNodes(graph.nodes.map((node) => currentPositions.has(node.id)
      ? {
        ...node,
        position: currentPositions.get(node.id)!,
        data: node.data.kind === 'stage' && currentContents.has(node.data.stageType)
          ? { ...node.data, contents: currentContents.get(node.data.stageType) }
          : node.data,
      }
      : node));
    setEdges(graph.edges);
    setBaselineStages(savedStages);
    setBaselineNotes(savedNotes);
    baselineSignatureRef.current = stagesSignature(savedStages);
    setDirty(false);
    setSaving(false);
    setIssues([]);
  }, [nodes]);

  const selectedNode = useMemo(() => nodes.find((node) => node.id === selection.nodeId), [nodes, selection.nodeId]);
  const selectedEdge = useMemo(() => edges.find((edge) => edge.id === selection.edgeId), [edges, selection.edgeId]);
  const usedTypes = useMemo(() => nodes.flatMap((node) => node.data.kind === 'stage' ? [node.data.stageType] : []), [nodes]);
  const payload = useMemo(() => buildStageSavePayload(nodes), [nodes]);
  const currentNotes = useMemo(() => cleanStageNotes(nodes, edges, edgeNotes(nodes, edges)), [edges, nodes]);

  return {
    nodes,
    edges,
    baselineStages,
    dirty,
    saving,
    issues,
    selection,
    selectedNode,
    selectedEdge,
    usedTypes,
    payload,
    currentNotes,
    setSaving,
    setSelection,
    onNodesChange,
    onEdgesChange,
    onConnect,
    onSelectionChange,
    addStage,
    updateNode,
    updateEdge,
    autoLayout,
    validate,
    discard,
    acceptSavedStages,
  };
}

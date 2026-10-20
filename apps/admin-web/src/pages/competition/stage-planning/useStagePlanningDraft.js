import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { addEdge, applyEdgeChanges, applyNodeChanges, } from '@xyflow/react';
import { autoLayoutStageGraph, buildStageSavePayload, canConnectStage, cleanStageNotes, toStageGraph, updateStageNodeData, validateStageGraph, } from './stageGraphUtils';
import { stageEdgeId, stageNodeId, stageNodePosition, stageNoteKey, stageTypeDefinition, } from '../../../types/competitionStagePlanning';
function stagesSignature(stages) {
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
function edgeNotes(nodes, edges) {
    const nodeMap = new Map(nodes.map((node) => [node.id, node]));
    const result = {};
    edges.filter((edge) => edge.data?.business).forEach((edge) => {
        const source = nodeMap.get(edge.source);
        const target = nodeMap.get(edge.target);
        if (source?.data.kind === 'stage' && target?.data.kind === 'stage') {
            const key = stageNoteKey(source.data.stageType, target.data.stageType);
            if (edge.data?.note)
                result[key] = edge.data.note;
        }
    });
    return result;
}
export function useStagePlanningDraft({ eventId, serverStages, stageCatalog = serverStages, initialNotes, editable, eventStartAt, eventEndAt, }) {
    const signature = stagesSignature(serverStages);
    const [nodes, setNodes] = useState([]);
    const [edges, setEdges] = useState([]);
    const [baselineStages, setBaselineStages] = useState(serverStages);
    const [baselineNotes, setBaselineNotes] = useState(initialNotes);
    const [dirty, setDirty] = useState(false);
    const [saving, setSaving] = useState(false);
    const [issues, setIssues] = useState([]);
    const [selection, setSelection] = useState({});
    const baselineSignatureRef = useRef('');
    const eventIdRef = useRef(eventId);
    const resetFromServer = useCallback((stages, notes) => {
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
        if (editable)
            setDirty(true);
    }, [editable]);
    const onNodesChange = useCallback((changes) => {
        setNodes((current) => {
            const next = applyNodeChanges(changes, current);
            const removed = new Set(changes.filter((change) => change.type === 'remove').map((change) => change.id));
            if (removed.size > 0) {
                setEdges((currentEdges) => currentEdges.filter((edge) => !removed.has(edge.source) && !removed.has(edge.target)));
            }
            if (changes.some((change) => change.type === 'add'
                || change.type === 'remove'
                || change.type === 'replace'
                || change.type === 'position'))
                markDirty();
            return next;
        });
    }, [markDirty]);
    const onEdgesChange = useCallback((changes) => {
        setEdges((current) => applyEdgeChanges(changes, current));
        if (changes.some((change) => change.type !== 'select'))
            markDirty();
    }, [markDirty]);
    const onConnect = useCallback((connection) => {
        if (!editable)
            return;
        setNodes((currentNodes) => {
            setEdges((currentEdges) => {
                if (!canConnectStage(connection, currentNodes, currentEdges))
                    return currentEdges;
                const source = currentNodes.find((node) => node.id === connection.source);
                const target = currentNodes.find((node) => node.id === connection.target);
                if (source?.data.kind !== 'stage' || target?.data.kind !== 'stage')
                    return currentEdges;
                const nextEdge = {
                    id: stageEdgeId(source.data.stageType, target.data.stageType),
                    source: connection.source,
                    target: connection.target,
                    sourceHandle: connection.sourceHandle,
                    targetHandle: connection.targetHandle,
                    type: 'stage',
                    data: { business: true, note: '' },
                };
                markDirty();
                return addEdge(nextEdge, currentEdges);
            });
            return currentNodes;
        });
    }, [editable, markDirty]);
    const onSelectionChange = useCallback((next) => {
        setSelection({ nodeId: next.nodes[0]?.id, edgeId: next.edges[0]?.id });
    }, []);
    const selectNode = useCallback((nodeId) => {
        setSelection({ nodeId });
        setNodes((current) => current.map((node) => ({
            ...node,
            selected: node.id === nodeId,
        })));
        setEdges((current) => current.map((edge) => ({
            ...edge,
            selected: false,
        })));
    }, []);
    const addStage = useCallback((stageType, position) => {
        if (!editable || nodes.some((node) => node.data.kind === 'stage' && node.data.stageType === stageType))
            return;
        const definition = stageTypeDefinition(stageType);
        if (!definition)
            return;
        const catalogStage = stageCatalog.find((stage) => stage.stageType === stageType);
        const stageNode = {
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
    const updateNode = useCallback((nodeId, patch) => {
        if (!editable)
            return;
        setNodes((current) => current.map((node) => updateStageNodeData(node, node.id === nodeId ? patch : {})));
        markDirty();
    }, [editable, markDirty]);
    const updateEdge = useCallback((edgeId, patch) => {
        if (!editable)
            return;
        setEdges((current) => current.map((edge) => edge.id === edgeId
            ? { ...edge, data: { business: edge.data?.business ?? true, ...patch } }
            : edge));
        markDirty();
    }, [editable, markDirty]);
    const autoLayout = useCallback(() => {
        if (!editable)
            return;
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
    const acceptSavedStages = useCallback((savedStages, savedNotes) => {
        const currentPositions = new Map(nodes.map((node) => [node.id, node.position]));
        const currentContents = new Map(nodes
            .filter((node) => node.data.kind === 'stage' && node.data.contents !== undefined)
            .map((node) => [node.data.stageType, node.data.contents]));
        const graph = toStageGraph(savedStages, savedNotes);
        setNodes(graph.nodes.map((node) => currentPositions.has(node.id)
            ? {
                ...node,
                position: currentPositions.get(node.id),
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
        selectNode,
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

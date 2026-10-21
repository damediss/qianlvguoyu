import dayjs from 'dayjs';
import { isBusinessStageNode, stageEdgeId, stageNodeId, stageNodePosition, stageNoteKey, stageTypeDefinition, stageTypeSortOrder, toStageBusinessNode, } from '../../../types/competitionStagePlanning';
function businessNodes(nodes) {
    return nodes
        .filter(isBusinessStageNode)
        .sort((a, b) => a.data.sortOrder - b.data.sortOrder);
}
function businessEdges(edges) {
    return edges.filter((edge) => edge.data?.business !== false);
}
function nodeById(nodes) {
    return new Map(nodes.map((node) => [node.id, node]));
}
export function deriveBusinessEdges(nodes, notes = {}) {
    const ordered = businessNodes(nodes);
    return ordered.slice(0, -1).map((source, index) => {
        const target = ordered[index + 1];
        const sourceType = source.data.stageType;
        const targetType = target.data.stageType;
        return {
            id: stageEdgeId(sourceType, targetType),
            source: source.id,
            target: target.id,
            type: 'stage',
            data: {
                business: true,
                note: notes[stageNoteKey(sourceType, targetType)] || '',
            },
        };
    });
}
export function toStageGraph(stages, notes = {}) {
    const ordered = [...stages]
        .filter((stage) => Boolean(stageTypeDefinition(stage.stageType)))
        .sort((a, b) => a.sortOrder - b.sortOrder);
    const stageNodes = ordered.map((stage, index) => toStageBusinessNode(stage, index));
    const startNode = {
        id: 'marker:start',
        type: 'marker',
        position: { x: -120, y: 180 },
        data: { kind: 'marker', marker: 'start', label: '赛事开始' },
        selectable: false,
        draggable: false,
    };
    const endNode = {
        id: 'marker:end',
        type: 'marker',
        position: stageNodePosition(stageNodes.length),
        data: { kind: 'marker', marker: 'end', label: '赛事结束' },
        selectable: false,
        draggable: false,
    };
    const edges = [];
    if (stageNodes.length > 0) {
        edges.push({
            id: `edge:marker-start:${stageNodes[0].id}`,
            source: startNode.id,
            target: stageNodes[0].id,
            type: 'stage',
            data: { business: false },
        });
        edges.push(...deriveBusinessEdges(stageNodes, notes));
        edges.push({
            id: `edge:${stageNodes[stageNodes.length - 1].id}:marker-end`,
            source: stageNodes[stageNodes.length - 1].id,
            target: endNode.id,
            type: 'stage',
            data: { business: false },
        });
    }
    return { nodes: [startNode, ...stageNodes, endNode], edges };
}
export function autoLayoutStageGraph(nodes) {
    const ordered = businessNodes(nodes);
    const positionById = new Map(ordered.map((node, index) => [node.id, stageNodePosition(index)]));
    return nodes.map((node) => {
        if (node.id === 'marker:start')
            return { ...node, position: { x: -120, y: 180 } };
        if (node.id === 'marker:end')
            return { ...node, position: stageNodePosition(ordered.length) };
        const position = positionById.get(node.id);
        return position ? { ...node, position } : node;
    });
}
export function buildStageSavePayload(nodes) {
    return {
        stages: businessNodes(nodes).map((node) => {
            const data = node.data;
            const item = {
                stageType: data.stageType,
            };
            if (data.stageId !== undefined && data.stageId !== null)
                item.id = String(data.stageId);
            if (data.startAt)
                item.startAt = data.startAt;
            if (data.endAt)
                item.endAt = data.endAt;
            if (data.description?.trim())
                item.description = data.description.trim();
            if (data.contents !== undefined)
                item.contents = data.contents;
            return item;
        }),
    };
}
export function canConnectStage(connection, nodes, edges) {
    const map = nodeById(nodes);
    const source = map.get(connection.source);
    const target = map.get(connection.target);
    if (!source || !target || !isBusinessStageNode(source) || !isBusinessStageNode(target))
        return false;
    if (source.id === target.id)
        return false;
    if (stageTypeSortOrder(source.data.stageType) >= stageTypeSortOrder(target.data.stageType))
        return false;
    if (businessEdges(edges).some((edge) => edge.source === source.id && edge.target === target.id))
        return false;
    const outgoing = businessEdges(edges).filter((edge) => edge.source === source.id);
    const incoming = businessEdges(edges).filter((edge) => edge.target === target.id);
    return outgoing.length === 0 && incoming.length === 0;
}
export function cleanStageNotes(nodes, edges, notes) {
    const map = nodeById(nodes);
    const cleaned = {};
    businessEdges(edges).forEach((edge) => {
        const source = map.get(edge.source);
        const target = map.get(edge.target);
        if (source && target && isBusinessStageNode(source) && isBusinessStageNode(target)) {
            const key = stageNoteKey(source.data.stageType, target.data.stageType);
            if (notes[key])
                cleaned[key] = notes[key];
        }
    });
    return cleaned;
}
function issue(code, level, targetType, message, targetId) {
    return { code, level, targetType, message, targetId };
}
export function validateStageGraph(nodes, edges, eventStartAt, eventEndAt) {
    const issues = [];
    const stages = businessNodes(nodes);
    const stageEdges = businessEdges(edges);
    const eventStart = eventStartAt ? dayjs(eventStartAt) : undefined;
    const eventEnd = eventEndAt ? dayjs(eventEndAt) : undefined;
    const hasEventStart = eventStart?.isValid() === true;
    const hasEventEnd = eventEnd?.isValid() === true;
    if (stages.length === 0) {
        issues.push(issue('STAGE_REQUIRED', 'error', 'canvas', '至少配置一个业务赛段'));
        return issues;
    }
    const seenTypes = new Set();
    const map = nodeById(nodes);
    stages.forEach((node) => {
        if (seenTypes.has(node.data.stageType)) {
            issues.push(issue('STAGE_TYPE_DUPLICATE', 'error', 'node', '同一赛事不能重复配置同一预设赛段', node.id));
        }
        seenTypes.add(node.data.stageType);
        const hasStart = Boolean(node.data.startAt);
        const hasEnd = Boolean(node.data.endAt);
        if (!hasStart && !hasEnd) {
            issues.push(issue('STAGE_TIME_MISSING', 'warning', 'node', '赛段尚未填写起止时间', node.id));
        }
        else if (!hasStart || !hasEnd) {
            issues.push(issue('STAGE_TIME_PAIR_INCOMPLETE', 'error', 'node', '开始和结束时间必须同时填写', node.id));
        }
        else if (node.data.endAt <= node.data.startAt) {
            issues.push(issue('STAGE_TIME_RANGE_INVALID', 'error', 'node', '结束时间必须晚于开始时间', node.id));
        }
        if (hasEventStart && hasStart && dayjs(node.data.startAt).isBefore(eventStart)) {
            issues.push(issue('STAGE_TIME_OUTSIDE_EVENT', 'error', 'node', '赛段开始时间不能早于赛事开始时间', node.id));
        }
        if (hasEventEnd && hasEnd && dayjs(node.data.endAt).isAfter(eventEnd)) {
            issues.push(issue('STAGE_TIME_OUTSIDE_EVENT', 'error', 'node', '赛段结束时间不能晚于赛事结束时间', node.id));
        }
        if (!node.data.description?.trim()) {
            issues.push(issue('STAGE_DESCRIPTION_EMPTY', 'warning', 'node', '赛段说明为空', node.id));
        }
    });
    const edgeKeys = new Set();
    const outgoing = new Map();
    const incoming = new Map();
    stageEdges.forEach((edge) => {
        const source = map.get(edge.source);
        const target = map.get(edge.target);
        if (!source || !target || !isBusinessStageNode(source) || !isBusinessStageNode(target)) {
            issues.push(issue('STAGE_EDGE_UNKNOWN_NODE', 'error', 'edge', '连线必须连接当前已选业务赛段', edge.id));
            return;
        }
        const key = `${edge.source}->${edge.target}`;
        if (edgeKeys.has(key)) {
            issues.push(issue('STAGE_EDGE_EXTRA', 'error', 'edge', '存在重复业务连线', edge.id));
        }
        edgeKeys.add(key);
        if (edge.source === edge.target) {
            issues.push(issue('STAGE_EDGE_SELF', 'error', 'edge', '不允许连接节点自身', edge.id));
        }
        if (source.data.sortOrder >= target.data.sortOrder) {
            issues.push(issue('STAGE_EDGE_REVERSE', 'error', 'edge', '连线必须从较早赛段指向较晚赛段', edge.id));
        }
        outgoing.set(edge.source, (outgoing.get(edge.source) || 0) + 1);
        incoming.set(edge.target, (incoming.get(edge.target) || 0) + 1);
    });
    outgoing.forEach((count, nodeId) => {
        if (count > 1)
            issues.push(issue('STAGE_EDGE_BRANCH', 'error', 'node', '一个赛段不能分支到多个后续赛段', nodeId));
    });
    incoming.forEach((count, nodeId) => {
        if (count > 1)
            issues.push(issue('STAGE_EDGE_MERGE', 'error', 'node', '一个赛段不能汇聚多个前置赛段', nodeId));
    });
    const expectedEdges = deriveBusinessEdges(stages).map((edge) => `${edge.source}->${edge.target}`);
    expectedEdges.forEach((key) => {
        if (!edgeKeys.has(key))
            issues.push(issue('STAGE_EDGE_MISSING', 'error', 'canvas', '赛段之间存在断链', key));
    });
    stageEdges.forEach((edge) => {
        const key = `${edge.source}->${edge.target}`;
        if (!expectedEdges.includes(key)) {
            issues.push(issue('STAGE_EDGE_EXTRA', 'error', 'edge', '连线必须连接排序后的相邻赛段', edge.id));
        }
    });
    if (stageEdges.length > Math.max(0, stages.length - 1)) {
        issues.push(issue('STAGE_EDGE_EXTRA', 'error', 'canvas', '业务连线数量不能超过赛段数量减一'));
    }
    const visiting = new Set();
    const visited = new Set();
    const visit = (nodeId) => {
        if (visiting.has(nodeId))
            return true;
        if (visited.has(nodeId))
            return false;
        visiting.add(nodeId);
        const cycle = stageEdges.filter((edge) => edge.source === nodeId).some((edge) => visit(edge.target));
        visiting.delete(nodeId);
        visited.add(nodeId);
        return cycle;
    };
    if (stages.some((node) => visit(node.id))) {
        issues.push(issue('STAGE_EDGE_CYCLE', 'error', 'canvas', '业务连线不能形成环路'));
    }
    const windows = stages
        .filter((node) => node.data.startAt && node.data.endAt && node.data.endAt > node.data.startAt)
        .map((node) => ({ node, startAt: node.data.startAt, endAt: node.data.endAt }));
    for (let index = 1; index < windows.length; index += 1) {
        const previous = windows[index - 1];
        const current = windows[index];
        if (current.startAt < previous.startAt) {
            issues.push(issue('STAGE_TIME_ORDER_INVALID', 'error', 'node', '赛段时间必须按预设顺序递增', current.node.id));
        }
        if (current.startAt < previous.endAt) {
            issues.push(issue('STAGE_TIME_OVERLAP', 'error', 'node', '相邻赛段时间不能重叠', current.node.id));
        }
    }
    return issues;
}
export function updateStageNodeData(node, patch) {
    if (!isBusinessStageNode(node))
        return node;
    return { ...node, data: { ...node.data, ...patch } };
}
export function stageTypesFromNodes(nodes) {
    return businessNodes(nodes).map((node) => node.data.stageType);
}
export function graphStageIds(nodes) {
    return businessNodes(nodes).map((node) => node.data.stageId).filter((id) => id !== undefined);
}
export function stageData(node) {
    return isBusinessStageNode(node) ? node.data : undefined;
}
export function stableNodeId(stageType) {
    return stageNodeId(stageType);
}

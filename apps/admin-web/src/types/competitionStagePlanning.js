export const STAGE_DEFINITIONS = [
    { stageType: 'qualification', name: '资格赛', sortOrder: 10 },
    { stageType: 'preliminary', name: '初赛', sortOrder: 20 },
    { stageType: 'quarterfinal', name: '复赛', sortOrder: 30 },
    { stageType: 'semifinal', name: '半决赛', sortOrder: 40 },
    { stageType: 'final', name: '决赛', sortOrder: 50 },
];
export function isBusinessStageNode(node) {
    return node.data.kind === 'stage';
}
export function stageTypeDefinition(stageType) {
    return STAGE_DEFINITIONS.find((item) => item.stageType === stageType);
}
export function stageTypeSortOrder(stageType) {
    return stageTypeDefinition(stageType)?.sortOrder ?? Number.MAX_SAFE_INTEGER;
}
export function stageTypeDisplayOrder(stageType) {
    const index = STAGE_DEFINITIONS.findIndex((item) => item.stageType === stageType);
    return index >= 0 ? index + 1 : undefined;
}
export function stageStatusLabel(status, stagePlanConfirmed = false) {
    if (stagePlanConfirmed && status === 'draft')
        return '已锁定';
    return {
        draft: '草稿',
        configuring: '配置中',
        ready_locked: '已锁定',
        in_progress: '进行中',
        closed: '已关闭',
        settled: '已结算',
        result_published: '结果已发布',
    }[status];
}
export function stageNodeId(stageType) {
    return `stage:${stageType}`;
}
export function stageEdgeId(sourceType, targetType) {
    return `edge:${sourceType}:${targetType}`;
}
export function stageNoteKey(sourceType, targetType) {
    return `${sourceType}->${targetType}`;
}
export function stageNodePosition(index) {
    return { x: 120 + index * 260, y: 180 };
}
export function toStageBusinessNode(stage, index) {
    const definition = stageTypeDefinition(stage.stageType);
    if (!definition) {
        throw new Error(`未知赛段类型：${stage.stageType}`);
    }
    return {
        id: stageNodeId(definition.stageType),
        type: 'stage',
        position: stageNodePosition(index),
        data: {
            kind: 'stage',
            stageType: definition.stageType,
            stageId: stage.id,
            name: definition.name,
            sortOrder: definition.sortOrder,
            status: stage.status,
            startAt: stage.startAt,
            endAt: stage.endAt,
            description: stage.description,
        },
    };
}

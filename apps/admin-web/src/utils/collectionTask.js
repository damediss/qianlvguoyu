export const CollectionTaskStatusText = {
    pending: '待采集',
    collecting: '采集中',
    completed: '已完成',
    exception: '有异常',
};
export const CollectionTaskStatusColor = {
    pending: undefined,
    collecting: 'blue',
    completed: 'green',
    exception: 'orange',
};
function toNumber(value) {
    const numberValue = Number(value || 0);
    return Number.isFinite(numberValue) ? numberValue : 0;
}
export function pendingCandidateCount(task) {
    return Math.max(toNumber(task.candidateTotal) - toNumber(task.completedCount) - toNumber(task.exceptionCount), 0);
}
export function resolveCollectionTaskStatus(task) {
    const total = toNumber(task.candidateTotal);
    const completed = toNumber(task.completedCount);
    const exception = toNumber(task.exceptionCount);
    if (exception > 0) {
        return 'exception';
    }
    if (completed >= total && total > 0) {
        return 'completed';
    }
    if (toNumber(task.collectingCount) > 0
        || toNumber(task.queuedCount) > 0
        || toNumber(task.arrivedCount) > 0) {
        return 'collecting';
    }
    return 'pending';
}
export function formatCollectionTaskStatus(task) {
    const status = resolveCollectionTaskStatus(task);
    return {
        status,
        label: CollectionTaskStatusText[status],
        color: CollectionTaskStatusColor[status],
    };
}

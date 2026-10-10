import type { CollectionTaskVO } from '../types/certificationPracticalCollection';

export type CollectionTaskStatus = 'pending' | 'collecting' | 'completed' | 'exception';

export const CollectionTaskStatusText: Record<CollectionTaskStatus, string> = {
  pending: '待采集',
  collecting: '采集中',
  completed: '已完成',
  exception: '有异常',
};

export const CollectionTaskStatusColor: Record<CollectionTaskStatus, string | undefined> = {
  pending: undefined,
  collecting: 'blue',
  completed: 'green',
  exception: 'orange',
};

function toNumber(value: unknown) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

export function pendingCandidateCount(task: CollectionTaskVO) {
  return Math.max(
    toNumber(task.candidateTotal) - toNumber(task.completedCount) - toNumber(task.exceptionCount),
    0,
  );
}

export function resolveCollectionTaskStatus(task: CollectionTaskVO): CollectionTaskStatus {
  const total = toNumber(task.candidateTotal);
  const completed = toNumber(task.completedCount);
  const exception = toNumber(task.exceptionCount);
  if (exception > 0) {
    return 'exception';
  }
  if (completed >= total && total > 0) {
    return 'completed';
  }
  if (
    toNumber(task.collectingCount) > 0
    || toNumber(task.queuedCount) > 0
    || toNumber(task.arrivedCount) > 0
  ) {
    return 'collecting';
  }
  return 'pending';
}

export function formatCollectionTaskStatus(task: CollectionTaskVO) {
  const status = resolveCollectionTaskStatus(task);
  return {
    status,
    label: CollectionTaskStatusText[status],
    color: CollectionTaskStatusColor[status],
  };
}

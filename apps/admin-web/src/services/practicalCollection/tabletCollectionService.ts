import { getCollectionTasks, getCollectionWorkbench } from '../../api/certificationPracticalCollection';
import type { CollectionTaskVO, SnowflakeId } from '../../types/certificationPracticalCollection';

export async function fetchCollectionTasks(keyword?: string): Promise<CollectionTaskVO[]> {
  const response = await getCollectionTasks({
    page: 1,
    pageSize: 100,
    keyword: keyword?.trim() || undefined,
  });
  return response.items || [];
}

export async function fetchCollectionTaskDetail(taskId: SnowflakeId) {
  const workbench = await getCollectionWorkbench(taskId);
  return workbench.task;
}

export async function fetchTaskCandidates(taskId: SnowflakeId) {
  const workbench = await getCollectionWorkbench(taskId);
  return workbench.candidates;
}

export function buildTabletWorkbenchPath(taskId: SnowflakeId): string {
  return `/practical-collection/workbench/${encodeURIComponent(String(taskId))}`;
}

export function switchCollectionTask(taskId: SnowflakeId): void {
  window.history.replaceState(null, '', buildTabletWorkbenchPath(taskId));
}

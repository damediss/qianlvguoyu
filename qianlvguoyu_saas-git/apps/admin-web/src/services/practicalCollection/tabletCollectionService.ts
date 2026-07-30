import { getCollectionTasks, getCollectionWorkbench } from '../../api/certificationPracticalCollection';
import type { CollectionTaskVO, SnowflakeId } from '../../types/certificationPracticalCollection';

const LAST_COLLECTION_TASK_ID_KEY = 'guoyu.practical-collection.last-task-id';

export function readLastCollectionTaskId(): string | undefined {
  try {
    const value = localStorage.getItem(LAST_COLLECTION_TASK_ID_KEY)?.trim();
    return value || undefined;
  } catch {
    return undefined;
  }
}

export function writeLastCollectionTaskId(taskId: SnowflakeId): void {
  try {
    localStorage.setItem(LAST_COLLECTION_TASK_ID_KEY, String(taskId));
  } catch {
    // ignore storage failures on restricted WebView profiles
  }
}

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
  writeLastCollectionTaskId(taskId);
  window.history.replaceState(null, '', buildTabletWorkbenchPath(taskId));
}

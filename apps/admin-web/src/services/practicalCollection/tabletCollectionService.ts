import { getCollectionTasks, getCollectionWorkbench } from '../../api/certificationPracticalCollection';
import type { CollectionTaskVO, SnowflakeId } from '../../types/certificationPracticalCollection';

const LAST_COLLECTION_TASK_ID_KEY = 'guoyu.practical-collection.last-task-id';
const LAST_COLLECTION_EXAM_SESSION_ID_KEY = 'guoyu.practical-collection.last-exam-session-id';

export interface CollectionWorkbenchScope {
  taskId: string;
  examSessionId?: string;
}

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

export function readLastCollectionScope(): CollectionWorkbenchScope | undefined {
  const taskId = readLastCollectionTaskId();
  if (!taskId) return undefined;
  try {
    const examSessionId = localStorage.getItem(LAST_COLLECTION_EXAM_SESSION_ID_KEY)?.trim() || undefined;
    return { taskId, examSessionId };
  } catch {
    return { taskId };
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

export async function fetchCollectionTaskDetail(taskId: SnowflakeId, examSessionId?: SnowflakeId) {
  const workbench = await getCollectionWorkbench(taskId, examSessionId);
  return workbench.task;
}

export async function fetchTaskCandidates(taskId: SnowflakeId, examSessionId?: SnowflakeId) {
  const workbench = await getCollectionWorkbench(taskId, examSessionId);
  return workbench.candidates;
}

export function buildTabletWorkbenchPath(
  taskId: SnowflakeId,
  examSessionId?: SnowflakeId,
): string {
  const path = `/practical-collection/workbench/${encodeURIComponent(String(taskId))}`;
  return examSessionId === undefined || examSessionId === null
    ? path
    : `${path}?examSessionId=${encodeURIComponent(String(examSessionId))}`;
}

export function switchCollectionTask(taskId: SnowflakeId, examSessionId?: SnowflakeId): void {
  writeLastCollectionTaskId(taskId);
  try {
    if (examSessionId === undefined || examSessionId === null) {
      localStorage.removeItem(LAST_COLLECTION_EXAM_SESSION_ID_KEY);
    } else {
      localStorage.setItem(LAST_COLLECTION_EXAM_SESSION_ID_KEY, String(examSessionId));
    }
  } catch {
    // ignore storage failures on restricted WebView profiles
  }
  window.history.replaceState(null, '', buildTabletWorkbenchPath(taskId, examSessionId));
}

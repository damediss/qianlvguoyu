import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type { CompetitionId } from '../types/competition';
import type {
  CompetitionPlayerSortingCandidate,
  CompetitionPlayerSortingGroupsRequest,
  CompetitionPlayerSortingReveal,
  CompetitionPlayerSortingRolesRequest,
  CompetitionPlayerSortingTask,
  CompetitionPlayerSortingTaskCreateRequest,
  CompetitionPlayerSortingTaskSelectionRequest,
  CompetitionPlayerSortingWorkstationsRequest,
} from '../types/competitionPlayerSorting';

export function listCompetitionPlayerSortingCandidates(stageId: CompetitionId) {
  return unwrap<CompetitionPlayerSortingCandidate[]>(
    apiClient.get(`/competition/player-sorting/stages/${stageId}/candidates`),
  );
}

export function pageCompetitionPlayerSortingTasks(
  stageId: CompetitionId,
  params: { page: number; pageSize: number },
) {
  return unwrap<PageResponse<CompetitionPlayerSortingTask>>(
    apiClient.get(`/competition/player-sorting/stages/${stageId}/tasks`, { params }),
  );
}

export function createCompetitionPlayerSortingTask(
  stageId: CompetitionId,
  payload: CompetitionPlayerSortingTaskCreateRequest,
) {
  return unwrap<CompetitionPlayerSortingTask>(
    apiClient.post(`/competition/player-sorting/stages/${stageId}/tasks`, payload),
  );
}

export function deleteCompetitionPlayerSortingTask(taskId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/player-sorting/tasks/${taskId}`));
}

export function saveCompetitionPlayerSortingTaskSelection(
  taskId: CompetitionId,
  payload: CompetitionPlayerSortingTaskSelectionRequest,
) {
  return unwrap<CompetitionPlayerSortingTask>(
    apiClient.put(`/competition/player-sorting/tasks/${taskId}/selection`, payload),
  );
}

export function saveCompetitionPlayerSortingGroups(
  taskId: CompetitionId,
  payload: CompetitionPlayerSortingGroupsRequest,
) {
  return unwrap<CompetitionPlayerSortingTask>(
    apiClient.put(`/competition/player-sorting/tasks/${taskId}/groups`, payload),
  );
}

export function saveCompetitionPlayerSortingRoles(
  taskId: CompetitionId,
  payload: CompetitionPlayerSortingRolesRequest,
) {
  return unwrap<CompetitionPlayerSortingTask>(
    apiClient.put(`/competition/player-sorting/tasks/${taskId}/roles`, payload),
  );
}

export function saveCompetitionPlayerSortingWorkstations(
  taskId: CompetitionId,
  payload: CompetitionPlayerSortingWorkstationsRequest,
) {
  return unwrap<CompetitionPlayerSortingTask>(
    apiClient.put(`/competition/player-sorting/tasks/${taskId}/workstations`, payload),
  );
}

export function drawCompetitionPlayerSortingOrder(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerSortingReveal>(
    apiClient.post(`/competition/player-sorting/tasks/${taskId}/draw-order`),
  );
}

export function drawCompetitionPlayerSortingWorkstations(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerSortingReveal>(
    apiClient.post(`/competition/player-sorting/tasks/${taskId}/draw-workstations`),
  );
}

export function randomizeCompetitionPlayerSortingTask(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerSortingTask>(
    apiClient.post(`/competition/player-sorting/tasks/${taskId}/randomize`),
  );
}

export function getCompetitionPlayerSortingTask(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerSortingTask>(
    apiClient.get(`/competition/player-sorting/tasks/${taskId}`),
  );
}

export async function downloadCompetitionPlayerSortingRoster(taskId: CompetitionId): Promise<Blob> {
  return (await apiClient.get(`/competition/player-sorting/tasks/${taskId}/roster.xlsx`, {
    responseType: 'blob',
    timeout: 120000,
  })).data as Blob;
}

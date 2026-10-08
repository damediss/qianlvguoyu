import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type { CompetitionId } from '../types/competition';
import type {
  CompetitionPlayerDrawCandidate,
  CompetitionPlayerDrawTask,
  CompetitionPlayerDrawTaskSelectionRequest,
} from '../types/competitionPlayerDraw';

export function listCompetitionPlayerDrawCandidates(stageId: CompetitionId) {
  return unwrap<CompetitionPlayerDrawCandidate[]>(
    apiClient.get(`/competition/player-draw/stages/${stageId}/candidates`),
  );
}

export function pageCompetitionPlayerDrawTasks(
  stageId: CompetitionId,
  params: { page: number; pageSize: number },
) {
  return unwrap<PageResponse<CompetitionPlayerDrawTask>>(
    apiClient.get(`/competition/player-draw/stages/${stageId}/tasks`, { params }),
  );
}

export function createCompetitionPlayerDrawTask(stageId: CompetitionId) {
  return unwrap<CompetitionPlayerDrawTask>(
    apiClient.post(`/competition/player-draw/stages/${stageId}/tasks`),
  );
}

export function deleteCompetitionPlayerDrawTask(taskId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/player-draw/tasks/${taskId}`));
}

export function saveCompetitionPlayerDrawTaskSelection(
  taskId: CompetitionId,
  payload: CompetitionPlayerDrawTaskSelectionRequest,
) {
  return unwrap<CompetitionPlayerDrawTask>(
    apiClient.put(`/competition/player-draw/tasks/${taskId}/selection`, payload),
  );
}

export function generateCompetitionPlayerDrawOrder(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerDrawTask>(
    apiClient.post(`/competition/player-draw/tasks/${taskId}/draw-order`),
  );
}

export function drawNextCompetitionPlayer(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerDrawTask>(
    apiClient.post(`/competition/player-draw/tasks/${taskId}/draw-next`),
  );
}

export function getCompetitionPlayerDrawTask(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerDrawTask>(
    apiClient.get(`/competition/player-draw/tasks/${taskId}`),
  );
}

export async function downloadCompetitionPlayerDrawRoster(taskId: CompetitionId): Promise<Blob> {
  return (await apiClient.get(`/competition/player-draw/tasks/${taskId}/roster.xlsx`, {
    responseType: 'blob',
    timeout: 120000,
  })).data as Blob;
}

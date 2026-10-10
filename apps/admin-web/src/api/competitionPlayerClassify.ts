import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type { CompetitionId } from '../types/competition';
import type {
  CompetitionPlayerClassifyCandidate,
  CompetitionPlayerClassifyCreateRequest,
  CompetitionPlayerClassifyTask,
} from '../types/competitionPlayerClassify';

export function listCompetitionPlayerClassifyCandidates(stageId: CompetitionId) {
  return unwrap<CompetitionPlayerClassifyCandidate[]>(
    apiClient.get(`/competition/player-classify/stages/${stageId}/candidates`),
  );
}

export function pageCompetitionPlayerClassifyTasks(
  stageId: CompetitionId,
  params: { page: number; pageSize: number },
) {
  return unwrap<PageResponse<CompetitionPlayerClassifyTask>>(
    apiClient.get(`/competition/player-classify/stages/${stageId}/tasks`, { params }),
  );
}

export function getCompetitionPlayerClassifyTask(taskId: CompetitionId) {
  return unwrap<CompetitionPlayerClassifyTask>(
    apiClient.get(`/competition/player-classify/tasks/${taskId}`),
  );
}

export function createCompetitionPlayerClassifyTask(
  stageId: CompetitionId,
  payload: CompetitionPlayerClassifyCreateRequest,
) {
  return unwrap<CompetitionPlayerClassifyTask>(
    apiClient.post(`/competition/player-classify/stages/${stageId}/tasks`, payload),
  );
}

export function deleteCompetitionPlayerClassifyTask(taskId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/player-classify/tasks/${taskId}`));
}

export async function downloadCompetitionPlayerClassifyResult(taskId: CompetitionId): Promise<Blob> {
  return (await apiClient.get(`/competition/player-classify/tasks/${taskId}/result.xlsx`, {
    responseType: 'blob',
    timeout: 120000,
  })).data as Blob;
}

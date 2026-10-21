import { apiClient, unwrap } from './client';
export function listCompetitionPlayerClassifyCandidates(stageId) {
    return unwrap(apiClient.get(`/competition/player-classify/stages/${stageId}/candidates`));
}
export function pageCompetitionPlayerClassifyTasks(stageId, params) {
    return unwrap(apiClient.get(`/competition/player-classify/stages/${stageId}/tasks`, { params }));
}
export function getCompetitionPlayerClassifyTask(taskId) {
    return unwrap(apiClient.get(`/competition/player-classify/tasks/${taskId}`));
}
export function createCompetitionPlayerClassifyTask(stageId, payload) {
    return unwrap(apiClient.post(`/competition/player-classify/stages/${stageId}/tasks`, payload));
}
export function deleteCompetitionPlayerClassifyTask(taskId) {
    return unwrap(apiClient.delete(`/competition/player-classify/tasks/${taskId}`));
}
export async function downloadCompetitionPlayerClassifyResult(taskId) {
    return (await apiClient.get(`/competition/player-classify/tasks/${taskId}/result.xlsx`, {
        responseType: 'blob',
        timeout: 120000,
    })).data;
}

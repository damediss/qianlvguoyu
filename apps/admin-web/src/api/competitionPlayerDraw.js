import { apiClient, unwrap } from './client';
export function listCompetitionPlayerDrawCandidates(stageId) {
    return unwrap(apiClient.get(`/competition/player-draw/stages/${stageId}/candidates`));
}
export function pageCompetitionPlayerDrawTasks(stageId, params) {
    return unwrap(apiClient.get(`/competition/player-draw/stages/${stageId}/tasks`, { params }));
}
export function createCompetitionPlayerDrawTask(stageId) {
    return unwrap(apiClient.post(`/competition/player-draw/stages/${stageId}/tasks`));
}
export function deleteCompetitionPlayerDrawTask(taskId) {
    return unwrap(apiClient.delete(`/competition/player-draw/tasks/${taskId}`));
}
export function saveCompetitionPlayerDrawTaskSelection(taskId, payload) {
    return unwrap(apiClient.put(`/competition/player-draw/tasks/${taskId}/selection`, payload));
}
export function generateCompetitionPlayerDrawOrder(taskId) {
    return unwrap(apiClient.post(`/competition/player-draw/tasks/${taskId}/draw-order`));
}
export function drawNextCompetitionPlayer(taskId) {
    return unwrap(apiClient.post(`/competition/player-draw/tasks/${taskId}/draw-next`));
}
export function getCompetitionPlayerDrawTask(taskId) {
    return unwrap(apiClient.get(`/competition/player-draw/tasks/${taskId}`));
}
export async function downloadCompetitionPlayerDrawRoster(taskId) {
    return (await apiClient.get(`/competition/player-draw/tasks/${taskId}/roster.xlsx`, {
        responseType: 'blob',
        timeout: 120000,
    })).data;
}

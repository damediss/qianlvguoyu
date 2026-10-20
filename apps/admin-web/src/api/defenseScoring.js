import { apiClient, unwrap } from './client';
const adminBase = '/certification/defense-scoring';
const assessorBase = '/certification/assessor-defense';
export function getDefenseScoringSetup(taskId) {
    return unwrap(apiClient.get(`${adminBase}/tasks/${taskId}/setup`));
}
export function saveDefenseScoringConfig(taskId, groupId) {
    return unwrap(apiClient.put(`${adminBase}/tasks/${taskId}/config`, { groupId }));
}
export function matchDefenseReports(taskId, files) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/reports/match`, { files }));
}
export function bindDefenseCandidateReport(taskId, candidateId, payload) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/report`, payload));
}
export function deleteDefenseCandidateReport(taskId, candidateId) {
    return unwrap(apiClient.delete(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/report`));
}
export function listMyDefenseReports() {
    return unwrap(apiClient.get(`${assessorBase}/reports`));
}
export function getMyDefenseReport(reportId) {
    return unwrap(apiClient.get(`${assessorBase}/reports/${reportId}`));
}
export function getDefenseScoreSheets(reportId) {
    return unwrap(apiClient.get(`${assessorBase}/reports/${reportId}/score-sheets`));
}
export function saveDefenseScoreSheet(reportId, sheetType, payload) {
    return unwrap(apiClient.put(`${assessorBase}/reports/${reportId}/score-sheets/${sheetType}`, payload));
}
export function submitDefenseScoreSheet(reportId, sheetType, payload) {
    return unwrap(apiClient.post(`${assessorBase}/reports/${reportId}/score-sheets/${sheetType}/submit`, payload));
}
export function getDefenseProgress(taskId) {
    return unwrap(apiClient.get(`${adminBase}/tasks/${taskId}/progress`));
}
export function completeDefenseTask(taskId) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/complete`));
}
export function markDefenseCandidateAbsent(taskId, candidateId) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/absent`));
}
export function exportDefenseSummary(taskId) {
    return apiClient.get(`${adminBase}/tasks/${taskId}/summary/export`, {
        responseType: 'blob',
    });
}

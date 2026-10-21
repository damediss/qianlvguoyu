import { apiClient, unwrap } from './client';
const adminBase = '/certification/practical-scoring';
const assessorBase = '/certification/assessor-scoring';
export function getPracticalScoringSetup(taskId) {
    return unwrap(apiClient.get(`${adminBase}/tasks/${taskId}/setup`));
}
export function savePracticalScoringConfig(taskId, payload) {
    return unwrap(apiClient.put(`${adminBase}/tasks/${taskId}/config`, payload));
}
export function savePracticalQuestionTemplate(paperId, questionId, items) {
    return unwrap(apiClient.put(`${adminBase}/papers/${paperId}/questions/${questionId}/template`, items));
}
export function savePracticalQuestion(paperId, questionId, payload) {
    return unwrap(apiClient.put(`${adminBase}/papers/${paperId}/questions/${questionId}`, payload));
}
export function generatePracticalScoringTasks(taskId) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/generate`));
}
export function linkPracticalCandidateTemplate(taskId, candidateId, payload) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/link`, payload));
}
export function batchLinkPracticalCandidateTemplates(taskId, payload) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/candidates/batch-link`, payload));
}
export function cancelPracticalCandidateGeneratedTasks(taskId, candidateId) {
    return unwrap(apiClient.delete(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/generated-tasks`));
}
export function importPracticalPaper(taskId, file) {
    const formData = new FormData();
    formData.append('file', file);
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/papers/import`, formData));
}
export function getPracticalScoringProgress(taskId) {
    return unwrap(apiClient.get(`${adminBase}/tasks/${taskId}/progress`));
}
export function completePracticalScoringTask(taskId) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/complete`));
}
export function requestPracticalCompleteSign(taskId) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/complete-sign/request`));
}
export function submitMyPracticalCompleteSign(taskId, payload) {
    return unwrap(apiClient.post(`${assessorBase}/tasks/${taskId}/complete-sign`, payload));
}
export function exportPracticalSummary(taskId, candidateIds) {
    return apiClient.get(`${adminBase}/tasks/${taskId}/summary/export`, {
        params: { candidateIds: candidateIds.join(',') },
        responseType: 'blob',
    });
}
export function returnPracticalScoringTask(assessorTaskId, reason) {
    return unwrap(apiClient.post(`${adminBase}/assessor-tasks/${assessorTaskId}/return`, { reason }));
}
export function getPracticalScoringTaskDetail(assessorTaskId) {
    return unwrap(apiClient.get(`${adminBase}/assessor-tasks/${assessorTaskId}`));
}
export function adjustPracticalScoringTaskScore(assessorTaskId, payload) {
    return unwrap(apiClient.put(`${adminBase}/assessor-tasks/${assessorTaskId}/score`, payload));
}
export function confirmPracticalCandidate(taskId, candidateId) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/confirm`));
}
export function markPracticalCandidateAbsent(taskId, candidateId) {
    return unwrap(apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/absent`));
}
export function listMyAssessorScoringTasks(status) {
    return unwrap(apiClient.get(`${assessorBase}/tasks`, { params: { status } }));
}
export function getMyAssessorWorkbenchContext() {
    return unwrap(apiClient.get(`${assessorBase}/workbench-context`));
}
export function switchMyAssessorWorkbenchTask(taskId) {
    return unwrap(apiClient.put(`${assessorBase}/workbench-context`, { taskId }));
}
export function listMyAssessorExams(status) {
    return unwrap(apiClient.get(`${assessorBase}/exams`, { params: { status } }));
}
export function getMyAssessorExam(taskId, candidateId) {
    return unwrap(apiClient.get(`${assessorBase}/exams/${taskId}/candidates/${candidateId}`));
}
export function getMyAssessorScoringTask(taskId) {
    return unwrap(apiClient.get(`${assessorBase}/tasks/${taskId}`));
}
export function confirmMyPracticalStart(taskId) {
    return unwrap(apiClient.post(`${assessorBase}/tasks/${taskId}/confirm-start`));
}
export function startMyPracticalSession(taskId) {
    return unwrap(apiClient.post(`${assessorBase}/tasks/${taskId}/start-session`));
}
export function completeMyPracticalSession(taskId) {
    return unwrap(apiClient.post(`${assessorBase}/tasks/${taskId}/complete-session`));
}
export function createPracticalWebSocketTicket() {
    return unwrap(apiClient.post(`${assessorBase}/websocket-ticket`));
}
export function saveMyAssessorScoringDraft(taskId, payload) {
    return unwrap(apiClient.put(`${assessorBase}/tasks/${taskId}/draft`, payload));
}
export function submitMyAssessorScoringTask(taskId, payload) {
    return unwrap(apiClient.post(`${assessorBase}/tasks/${taskId}/submit`, payload));
}

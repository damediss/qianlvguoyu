import { apiClient, unwrap } from './client';
export function pageCertificationTasks(params) {
    return unwrap(apiClient.get('/certification/projects/tasks', { params }));
}
export function getCertificationTask(id) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${id}`));
}
export function createCertificationTask(payload) {
    return unwrap(apiClient.post('/certification/projects/tasks', payload));
}
export function updateCertificationTask(id, payload) {
    return unwrap(apiClient.put(`/certification/projects/tasks/${id}`, payload));
}
export function deleteCertificationTask(id) {
    return unwrap(apiClient.delete(`/certification/projects/tasks/${id}`));
}
export function getCertificationWorkbench(taskId) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/workbench`));
}
export function getCertificationFlow(taskId) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/flow`));
}
export function listCertificationDocuments(taskId) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/documents`));
}
export function getCertificationDocument(taskId, documentType) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/documents/${documentType}`));
}
export function saveCertificationDocument(taskId, documentType, payload) {
    return unwrap(apiClient.put(`/certification/projects/tasks/${taskId}/documents/${documentType}`, payload));
}
export function submitCertificationDocument(taskId, documentType) {
    return unwrap(apiClient.post(`/certification/projects/tasks/${taskId}/documents/${documentType}/submit`));
}
export function initialReviewCertificationDocument(taskId, documentType, payload) {
    return unwrap(apiClient.post(`/certification/projects/tasks/${taskId}/documents/${documentType}/initial-review`, payload));
}
export function finalReviewCertificationDocument(taskId, documentType, payload) {
    return unwrap(apiClient.post(`/certification/projects/tasks/${taskId}/documents/${documentType}/final-review`, payload));
}
export function getCertificationDocumentWorkflow(taskId, documentType) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/documents/${documentType}/workflow`));
}
export function listCertificationMembers(taskId) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/members`));
}
export function listCertificationMemberUsers() {
    return unwrap(apiClient.get('/certification/projects/member-users'));
}
export function saveCertificationMembers(taskId, payload) {
    return unwrap(apiClient.put(`/certification/projects/tasks/${taskId}/members`, payload));
}
export function listCertificationWbsItems(taskId) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/wbs`));
}
export function saveCertificationWbsItems(taskId, payload) {
    return unwrap(apiClient.put(`/certification/projects/tasks/${taskId}/wbs`, payload));
}
export function listCertificationScheduleItems(taskId) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/schedule`));
}
export function saveCertificationScheduleItems(taskId, payload) {
    return unwrap(apiClient.put(`/certification/projects/tasks/${taskId}/schedule`, payload));
}
export function listCertificationRisks(taskId) {
    return unwrap(apiClient.get(`/certification/projects/tasks/${taskId}/risks`));
}
export function saveCertificationRisks(taskId, payload) {
    return unwrap(apiClient.put(`/certification/projects/tasks/${taskId}/risks`, payload));
}
export function toggleCertificationRiskStatus(taskId, riskId, payload) {
    return unwrap(apiClient.put(`/certification/projects/tasks/${taskId}/risks/${riskId}/status`, payload));
}

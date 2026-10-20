import { apiClient, unwrap } from './client';
const base = '/certification/preparation';
export function pagePreparationTasks(params) {
    return unwrap(apiClient.get(`${base}/tasks`, { params }));
}
export function getPreparationTaskOverview(taskId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/overview`));
}
export function getPreparationCompleteCheck(taskId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/complete-check`));
}
export function completePreparation(taskId) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/complete`));
}
export function importPreparationCandidates(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/candidate-imports`, payload));
}
export function importPreparationCandidatesExcel(taskId, taskScopeId, file, remark, examSessionIds, defaultParticipation) {
    const formData = new FormData();
    formData.append('taskScopeId', taskScopeId);
    formData.append('file', file);
    if (remark)
        formData.append('remark', remark);
    examSessionIds?.forEach((sessionId) => formData.append('examSessionIds', sessionId));
    if (defaultParticipation)
        formData.append('defaultParticipation', defaultParticipation);
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/candidate-imports/xlsx`, formData));
}
const examSessionBase = '/certification';
export function listCertificationExamSessions(taskId) {
    return unwrap(apiClient.get(`${examSessionBase}/tasks/${taskId}/exam-sessions`));
}
export function createCertificationExamSession(taskId, payload) {
    return unwrap(apiClient.post(`${examSessionBase}/tasks/${taskId}/exam-sessions`, payload));
}
export function updateCertificationExamSession(sessionId, payload) {
    return unwrap(apiClient.put(`${examSessionBase}/exam-sessions/${sessionId}`, payload));
}
export function deleteCertificationExamSession(sessionId) {
    return unwrap(apiClient.delete(`${examSessionBase}/exam-sessions/${sessionId}`));
}
export function listCertificationExamSessionCandidates(sessionId) {
    return unwrap(apiClient.get(`${examSessionBase}/exam-sessions/${sessionId}/candidates`));
}
export function assignCertificationExamSessionCandidates(sessionId, payload) {
    return unwrap(apiClient.put(`${examSessionBase}/exam-sessions/${sessionId}/candidates`, payload));
}
export function completeCertificationExamSessionCollectionConfig(sessionId) {
    return unwrap(apiClient.post(`${examSessionBase}/exam-sessions/${sessionId}/collection-config/complete`));
}
export function listCandidateExamSessions(candidateId) {
    return unwrap(apiClient.get(`${examSessionBase}/candidates/${candidateId}/exam-sessions`));
}
export function updateCandidateExamSession(candidateId, sessionId, payload) {
    return unwrap(apiClient.put(`${examSessionBase}/candidates/${candidateId}/exam-sessions/${sessionId}`, payload));
}
export function downloadPreparationCandidateExcelTemplate() {
    return apiClient.get(`${base}/candidate-import-template`, { responseType: 'blob' });
}
export function generatePreparationImportBatchMaterials(taskId, batchId, remark) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/candidate-imports/${batchId}/application-materials`, undefined, { params: { remark } }));
}
export function pageCandidateStaging(taskId, params) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/candidate-staging`, { params }));
}
export function promoteCandidateStaging(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/candidate-staging/promote`, payload));
}
export function generatePreparationCandidateDocument(taskId, candidateId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/candidates/${candidateId}/documents/generate`, payload));
}
export function pagePreparationImportErrors(taskId, batchId, params) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/candidate-imports/${batchId}/errors`, { params }));
}
export function pagePreparationCandidates(taskId, params) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/candidates`, { params }));
}
export async function listAllPreparationCandidates(taskId, params = {}) {
    const pageSize = 200;
    const first = await pagePreparationCandidates(taskId, { ...params, page: 1, pageSize });
    const items = [...(first.items || [])];
    const pageCount = Math.ceil((first.total || 0) / pageSize);
    for (let page = 2; page <= pageCount; page += 1) {
        const next = await pagePreparationCandidates(taskId, { ...params, page, pageSize });
        items.push(...(next.items || []));
    }
    return { ...first, items };
}
export function createPreparationCandidate(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/candidates`, payload));
}
export function getPreparationCandidateApplicationSource(taskId, candidateId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/candidates/${candidateId}/application-source`));
}
export function updatePreparationCandidate(taskId, candidateId, payload) {
    return unwrap(apiClient.put(`${base}/tasks/${taskId}/candidates/${candidateId}`, payload));
}
export function deletePreparationCandidate(taskId, candidateId) {
    return unwrap(apiClient.delete(`${base}/tasks/${taskId}/candidates/${candidateId}`));
}
export function pagePreparationCandidateDocuments(taskId, params) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/candidate-documents`, { params }));
}
export function uploadPreparationCandidateDocument(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/candidate-documents`, payload));
}
export function deletePreparationCandidateDocument(taskId, documentId) {
    return unwrap(apiClient.delete(`${base}/tasks/${taskId}/candidate-documents/${documentId}`));
}
export function reviewPreparationCandidateDocument(taskId, documentId, payload) {
    return unwrap(apiClient.put(`${base}/tasks/${taskId}/candidate-documents/${documentId}/review`, payload));
}
export function pagePreparationMaterials(taskId, params) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/application-materials`, { params }));
}
export function createPreparationMaterial(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/application-materials`, payload));
}
export function generatePreparationMaterials(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/application-materials/generate`, payload));
}
export function getPreparationMaterialBatch(taskId, batchId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/application-materials/${batchId}`));
}
export function pagePreparationExamSites(params) {
    return unwrap(apiClient.get(`${base}/exam-sites`, { params }));
}
export function createPreparationExamSite(payload) {
    return unwrap(apiClient.post(`${base}/exam-sites`, payload));
}
export function updatePreparationExamSite(siteId, payload) {
    return unwrap(apiClient.put(`${base}/exam-sites/${siteId}`, payload));
}
export function listPreparationTaskExamSites(taskId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/exam-sites`));
}
export function assignPreparationExamSite(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/exam-sites`, payload));
}
export function removePreparationTaskExamSite(taskId, taskSiteId) {
    return unwrap(apiClient.delete(`${base}/tasks/${taskId}/exam-sites/${taskSiteId}`));
}
export function listPreparationTaskExamStaff(taskId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/exam-staff`));
}
export function assignPreparationExamStaff(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/exam-staff`, payload));
}
export function removePreparationTaskExamStaff(taskId, taskStaffId) {
    return unwrap(apiClient.delete(`${base}/tasks/${taskId}/exam-staff/${taskStaffId}`));
}
export function appointPreparationTaskExamStaffLeader(taskId, taskStaffId) {
    return unwrap(apiClient.put(`${base}/tasks/${taskId}/exam-staff/${taskStaffId}/leader`));
}
export function updatePreparationTaskExamStaffRole(taskId, taskStaffId, payload) {
    return unwrap(apiClient.put(`${base}/tasks/${taskId}/exam-staff/${taskStaffId}/role`, payload));
}
export function getPreparationChecklist(taskId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/checklist`));
}
export function updatePreparationChecklist(taskId, payload) {
    return unwrap(apiClient.put(`${base}/tasks/${taskId}/checklist`, payload));
}
export function listPreparationTaskEquipment(taskId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/equipment`));
}
export function assignPreparationTaskEquipment(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/equipment`, payload));
}
export function bindPreparationTaskEquipment(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/equipment/bind`, payload));
}
export function removePreparationTaskEquipment(taskId, taskEquipmentId) {
    return unwrap(apiClient.delete(`${base}/tasks/${taskId}/equipment/${taskEquipmentId}`));
}
export function pagePreparationAssessors(params) {
    return unwrap(apiClient.get(`${base}/assessors`, { params }));
}
export function createPreparationAssessor(payload) {
    return unwrap(apiClient.post(`${base}/assessors`, payload));
}
export function updatePreparationAssessor(assessorId, payload) {
    return unwrap(apiClient.put(`${base}/assessors/${assessorId}`, payload));
}
export function listPreparationAssessorGroups(taskId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/assessor-groups`));
}
export function createPreparationAssessorGroup(taskId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/assessor-groups`, payload));
}
export function updatePreparationAssessorGroup(taskId, groupId, payload) {
    return unwrap(apiClient.put(`${base}/tasks/${taskId}/assessor-groups/${groupId}`, payload));
}
export function deletePreparationAssessorGroup(taskId, groupId) {
    return unwrap(apiClient.delete(`${base}/tasks/${taskId}/assessor-groups/${groupId}`));
}
export function listPreparationGroupMembers(taskId, groupId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/assessor-groups/${groupId}/members`));
}
export function addPreparationGroupMember(taskId, groupId, payload) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/assessor-groups/${groupId}/members`, payload));
}
export function removePreparationGroupMember(taskId, groupId, memberId) {
    return unwrap(apiClient.delete(`${base}/tasks/${taskId}/assessor-groups/${groupId}/members/${memberId}`));
}
export function listPreparationAssessorGroupWorktypes(taskId, groupId) {
    return unwrap(apiClient.get(`${base}/tasks/${taskId}/assessor-group-worktypes`, { params: { groupId } }));
}

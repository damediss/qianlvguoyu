import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  PreparationAssessor,
  PreparationAssessorDispatchPayload,
  PreparationAssessorGroup,
  PreparationAssessorGroupPayload,
  PreparationAssessorPayload,
  PreparationAssessorQuery,
  PreparationAssessorTask,
  PreparationCandidate,
  PreparationCandidateDocument,
  PreparationCandidateDocumentPayload,
  PreparationCandidateImportPayload,
  PreparationCandidatePayload,
  PreparationCandidateQuery,
  PreparationCandidateStaging,
  PreparationCandidateStagingPromotePayload,
  PreparationCandidateStagingPromoteResult,
  PreparationCandidateStagingQuery,
  PreparationDocumentGeneratePayload,
  PreparationChecklist,
  PreparationChecklistUpdatePayload,
  PreparationCompleteCheck,
  PreparationDocumentQuery,
  PreparationDocumentReviewPayload,
  PreparationExamSite,
  PreparationExamSitePayload,
  PreparationExamSiteQuery,
  PreparationImportBatch,
  PreparationImportError,
  PreparationMaterialBatch,
  PreparationMaterialGeneratePayload,
  PreparationMaterialItem,
  PreparationMaterialItemPayload,
  PreparationMaterialQuery,
  PreparationTaskAssessor,
  PreparationTaskAssessorPayload,
  PreparationTaskEquipment,
  PreparationTaskEquipmentPayload,
  PreparationTaskExamSite,
  PreparationTaskExamSitePayload,
  PreparationTaskExamStaff,
  PreparationTaskExamStaffPayload,
  PreparationTaskOverview,
  PreparationTaskQuery,
  PreparationPageQuery,
} from '../types/certificationPreparation';

const base = '/certification/preparation';

export function pagePreparationTasks(params: PreparationTaskQuery) {
  return unwrap<PageResponse<PreparationTaskOverview>>(apiClient.get(`${base}/tasks`, { params }));
}

export function getPreparationTaskOverview(taskId: string) {
  return unwrap<PreparationTaskOverview>(apiClient.get(`${base}/tasks/${taskId}/overview`));
}

export function getPreparationCompleteCheck(taskId: string) {
  return unwrap<PreparationCompleteCheck>(apiClient.get(`${base}/tasks/${taskId}/complete-check`));
}

export function completePreparation(taskId: string) {
  return unwrap<PreparationCompleteCheck>(apiClient.post(`${base}/tasks/${taskId}/complete`));
}

export function importPreparationCandidates(taskId: string, payload: PreparationCandidateImportPayload) {
  return unwrap<PreparationImportBatch>(apiClient.post(`${base}/tasks/${taskId}/candidate-imports`, payload));
}

export function pageCandidateStaging(taskId: string, params: PreparationCandidateStagingQuery) {
  return unwrap<PageResponse<PreparationCandidateStaging>>(apiClient.get(`${base}/tasks/${taskId}/candidate-staging`, { params }));
}

export function promoteCandidateStaging(taskId: string, payload: PreparationCandidateStagingPromotePayload) {
  return unwrap<PreparationCandidateStagingPromoteResult>(
    apiClient.post(`${base}/tasks/${taskId}/candidate-staging/promote`, payload),
  );
}

export function generatePreparationCandidateDocument(
  taskId: string,
  candidateId: string,
  payload: PreparationDocumentGeneratePayload,
) {
  return unwrap<PreparationCandidateDocument>(
    apiClient.post(`${base}/tasks/${taskId}/candidates/${candidateId}/documents/generate`, payload),
  );
}

export function pagePreparationImportErrors(taskId: string, batchId: string, params: PreparationPageQuery) {
  return unwrap<PageResponse<PreparationImportError>>(
    apiClient.get(`${base}/tasks/${taskId}/candidate-imports/${batchId}/errors`, { params }),
  );
}

export function pagePreparationCandidates(taskId: string, params: PreparationCandidateQuery) {
  return unwrap<PageResponse<PreparationCandidate>>(apiClient.get(`${base}/tasks/${taskId}/candidates`, { params }));
}

export async function listAllPreparationCandidates(taskId: string, params: Omit<PreparationCandidateQuery, 'page' | 'pageSize'> = {}) {
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

export function createPreparationCandidate(taskId: string, payload: PreparationCandidatePayload) {
  return unwrap<PreparationCandidate>(apiClient.post(`${base}/tasks/${taskId}/candidates`, payload));
}

export function updatePreparationCandidate(taskId: string, candidateId: string, payload: PreparationCandidatePayload) {
  return unwrap<PreparationCandidate>(apiClient.put(`${base}/tasks/${taskId}/candidates/${candidateId}`, payload));
}

export function deletePreparationCandidate(taskId: string, candidateId: string) {
  return unwrap<void>(apiClient.delete(`${base}/tasks/${taskId}/candidates/${candidateId}`));
}

export function pagePreparationCandidateDocuments(taskId: string, params: PreparationDocumentQuery) {
  return unwrap<PageResponse<PreparationCandidateDocument>>(
    apiClient.get(`${base}/tasks/${taskId}/candidate-documents`, { params }),
  );
}

export function uploadPreparationCandidateDocument(taskId: string, payload: PreparationCandidateDocumentPayload) {
  return unwrap<PreparationCandidateDocument>(apiClient.post(`${base}/tasks/${taskId}/candidate-documents`, payload));
}

export function deletePreparationCandidateDocument(taskId: string, documentId: string) {
  return unwrap<void>(apiClient.delete(`${base}/tasks/${taskId}/candidate-documents/${documentId}`));
}

export function reviewPreparationCandidateDocument(
  taskId: string,
  documentId: string,
  payload: PreparationDocumentReviewPayload,
) {
  return unwrap<PreparationCandidateDocument>(
    apiClient.put(`${base}/tasks/${taskId}/candidate-documents/${documentId}/review`, payload),
  );
}

export function pagePreparationMaterials(taskId: string, params: PreparationMaterialQuery) {
  return unwrap<PageResponse<PreparationMaterialBatch>>(
    apiClient.get(`${base}/tasks/${taskId}/application-materials`, { params }),
  );
}

export function createPreparationMaterial(taskId: string, payload: PreparationMaterialItemPayload) {
  return unwrap<PreparationMaterialItem>(apiClient.post(`${base}/tasks/${taskId}/application-materials`, payload));
}

export function generatePreparationMaterials(taskId: string, payload: PreparationMaterialGeneratePayload) {
  return unwrap<PreparationMaterialBatch>(apiClient.post(`${base}/tasks/${taskId}/application-materials/generate`, payload));
}

export function getPreparationMaterialBatch(taskId: string, batchId: string) {
  return unwrap<PreparationMaterialBatch>(apiClient.get(`${base}/tasks/${taskId}/application-materials/${batchId}`));
}

export function pagePreparationExamSites(params: PreparationExamSiteQuery) {
  return unwrap<PageResponse<PreparationExamSite>>(apiClient.get(`${base}/exam-sites`, { params }));
}

export function createPreparationExamSite(payload: PreparationExamSitePayload) {
  return unwrap<PreparationExamSite>(apiClient.post(`${base}/exam-sites`, payload));
}

export function updatePreparationExamSite(siteId: string, payload: PreparationExamSitePayload) {
  return unwrap<PreparationExamSite>(apiClient.put(`${base}/exam-sites/${siteId}`, payload));
}

export function listPreparationTaskExamSites(taskId: string) {
  return unwrap<PreparationTaskExamSite[]>(apiClient.get(`${base}/tasks/${taskId}/exam-sites`));
}

export function assignPreparationExamSite(taskId: string, payload: PreparationTaskExamSitePayload) {
  return unwrap<PreparationTaskExamSite>(apiClient.post(`${base}/tasks/${taskId}/exam-sites`, payload));
}

export function removePreparationTaskExamSite(taskId: string, taskSiteId: string) {
  return unwrap<void>(apiClient.delete(`${base}/tasks/${taskId}/exam-sites/${taskSiteId}`));
}

export function listPreparationTaskExamStaff(taskId: string) {
  return unwrap<PreparationTaskExamStaff[]>(apiClient.get(`${base}/tasks/${taskId}/exam-staff`));
}

export function assignPreparationExamStaff(taskId: string, payload: PreparationTaskExamStaffPayload) {
  return unwrap<PreparationTaskExamStaff>(apiClient.post(`${base}/tasks/${taskId}/exam-staff`, payload));
}

export function removePreparationTaskExamStaff(taskId: string, taskStaffId: string) {
  return unwrap<void>(apiClient.delete(`${base}/tasks/${taskId}/exam-staff/${taskStaffId}`));
}

export function appointPreparationTaskExamStaffLeader(taskId: string, taskStaffId: string) {
  return unwrap<PreparationTaskExamStaff>(apiClient.put(`${base}/tasks/${taskId}/exam-staff/${taskStaffId}/leader`));
}

export function updatePreparationTaskExamStaffRole(
  taskId: string,
  taskStaffId: string,
  payload: Pick<PreparationTaskExamStaffPayload, 'taskRole' | 'roleName'>,
) {
  return unwrap<PreparationTaskExamStaff>(
    apiClient.put(`${base}/tasks/${taskId}/exam-staff/${taskStaffId}/role`, payload),
  );
}

export function getPreparationChecklist(taskId: string) {
  return unwrap<PreparationChecklist>(apiClient.get(`${base}/tasks/${taskId}/checklist`));
}

export function updatePreparationChecklist(taskId: string, payload: PreparationChecklistUpdatePayload) {
  return unwrap<PreparationChecklist>(apiClient.put(`${base}/tasks/${taskId}/checklist`, payload));
}

export function listPreparationTaskEquipment(taskId: string) {
  return unwrap<PreparationTaskEquipment[]>(apiClient.get(`${base}/tasks/${taskId}/equipment`));
}

export function assignPreparationTaskEquipment(taskId: string, payload: PreparationTaskEquipmentPayload) {
  return unwrap<PreparationTaskEquipment>(apiClient.post(`${base}/tasks/${taskId}/equipment`, payload));
}

export function bindPreparationTaskEquipment(taskId: string, payload: PreparationTaskEquipmentPayload) {
  return unwrap<PreparationTaskEquipment>(apiClient.post(`${base}/tasks/${taskId}/equipment/bind`, payload));
}

export function removePreparationTaskEquipment(taskId: string, taskEquipmentId: string) {
  return unwrap<void>(apiClient.delete(`${base}/tasks/${taskId}/equipment/${taskEquipmentId}`));
}

export function pagePreparationAssessors(params: PreparationAssessorQuery) {
  return unwrap<PageResponse<PreparationAssessor>>(apiClient.get(`${base}/assessors`, { params }));
}

export function createPreparationAssessor(payload: PreparationAssessorPayload) {
  return unwrap<PreparationAssessor>(apiClient.post(`${base}/assessors`, payload));
}

export function updatePreparationAssessor(assessorId: string, payload: PreparationAssessorPayload) {
  return unwrap<PreparationAssessor>(apiClient.put(`${base}/assessors/${assessorId}`, payload));
}

export function listPreparationAssessorGroups(taskId: string) {
  return unwrap<PreparationAssessorGroup[]>(apiClient.get(`${base}/tasks/${taskId}/assessor-groups`));
}

export function createPreparationAssessorGroup(taskId: string, payload: PreparationAssessorGroupPayload) {
  return unwrap<PreparationAssessorGroup>(apiClient.post(`${base}/tasks/${taskId}/assessor-groups`, payload));
}

export function updatePreparationAssessorGroup(taskId: string, groupId: string, payload: PreparationAssessorGroupPayload) {
  return unwrap<PreparationAssessorGroup>(apiClient.put(`${base}/tasks/${taskId}/assessor-groups/${groupId}`, payload));
}

export function deletePreparationAssessorGroup(taskId: string, groupId: string) {
  return unwrap<void>(apiClient.delete(`${base}/tasks/${taskId}/assessor-groups/${groupId}`));
}

export function listPreparationGroupMembers(taskId: string, groupId: string) {
  return unwrap<PreparationTaskAssessor[]>(apiClient.get(`${base}/tasks/${taskId}/assessor-groups/${groupId}/members`));
}

export function addPreparationGroupMember(taskId: string, groupId: string, payload: PreparationTaskAssessorPayload) {
  return unwrap<PreparationTaskAssessor>(
    apiClient.post(`${base}/tasks/${taskId}/assessor-groups/${groupId}/members`, payload),
  );
}

export function removePreparationGroupMember(taskId: string, groupId: string, memberId: string) {
  return unwrap<void>(apiClient.delete(`${base}/tasks/${taskId}/assessor-groups/${groupId}/members/${memberId}`));
}

export function dispatchPreparationAssessorGroup(
  taskId: string,
  groupId: string,
  payload: PreparationAssessorDispatchPayload,
) {
  return unwrap<PreparationAssessorTask[]>(
    apiClient.post(`${base}/tasks/${taskId}/assessor-groups/${groupId}/dispatch`, payload),
  );
}


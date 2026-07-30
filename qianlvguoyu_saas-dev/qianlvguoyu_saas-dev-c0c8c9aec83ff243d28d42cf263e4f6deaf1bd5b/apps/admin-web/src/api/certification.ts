import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  CertificationDocument,
  CertificationDocumentPayload,
  CertificationDocumentReviewPayload,
  CertificationDocumentType,
  CertificationDocumentWorkflow,
  CertificationMember,
  CertificationPhase,
  CertificationRisk,
  CertificationRiskStatusPayload,
  CertificationScheduleItem,
  CertificationTask,
  CertificationTaskPayload,
  CertificationTaskQuery,
  CertificationWbsItem,
  CertificationWorkbench,
} from '../types/certification';

export function pageCertificationTasks(params: CertificationTaskQuery) {
  return unwrap<PageResponse<CertificationTask>>(apiClient.get('/certification/projects/tasks', { params }));
}

export function getCertificationTask(id: string) {
  return unwrap<CertificationTask>(apiClient.get(`/certification/projects/tasks/${id}`));
}

export function createCertificationTask(payload: CertificationTaskPayload) {
  return unwrap<CertificationTask>(apiClient.post('/certification/projects/tasks', payload));
}

export function updateCertificationTask(id: string, payload: CertificationTaskPayload) {
  return unwrap<CertificationTask>(apiClient.put(`/certification/projects/tasks/${id}`, payload));
}

export function deleteCertificationTask(id: string) {
  return unwrap<void>(apiClient.delete(`/certification/projects/tasks/${id}`));
}

export function getCertificationWorkbench(taskId: string) {
  return unwrap<CertificationWorkbench>(apiClient.get(`/certification/projects/tasks/${taskId}/workbench`));
}

export function getCertificationFlow(taskId: string) {
  return unwrap<CertificationPhase[]>(apiClient.get(`/certification/projects/tasks/${taskId}/flow`));
}

export function listCertificationDocuments(taskId: string) {
  return unwrap<CertificationDocument[]>(apiClient.get(`/certification/projects/tasks/${taskId}/documents`));
}

export function getCertificationDocument(taskId: string, documentType: CertificationDocumentType) {
  return unwrap<CertificationDocument>(apiClient.get(`/certification/projects/tasks/${taskId}/documents/${documentType}`));
}

export function saveCertificationDocument(
  taskId: string,
  documentType: CertificationDocumentType,
  payload: CertificationDocumentPayload,
) {
  return unwrap<CertificationDocument>(
    apiClient.put(`/certification/projects/tasks/${taskId}/documents/${documentType}`, payload),
  );
}

export function submitCertificationDocument(taskId: string, documentType: CertificationDocumentType) {
  return unwrap<CertificationDocument>(
    apiClient.post(`/certification/projects/tasks/${taskId}/documents/${documentType}/submit`),
  );
}

export function initialReviewCertificationDocument(
  taskId: string,
  documentType: CertificationDocumentType,
  payload: CertificationDocumentReviewPayload,
) {
  return unwrap<CertificationDocument>(
    apiClient.post(`/certification/projects/tasks/${taskId}/documents/${documentType}/initial-review`, payload),
  );
}

export function finalReviewCertificationDocument(
  taskId: string,
  documentType: CertificationDocumentType,
  payload: CertificationDocumentReviewPayload,
) {
  return unwrap<CertificationDocument>(
    apiClient.post(`/certification/projects/tasks/${taskId}/documents/${documentType}/final-review`, payload),
  );
}

export function getCertificationDocumentWorkflow(taskId: string, documentType: CertificationDocumentType) {
  return unwrap<CertificationDocumentWorkflow>(
    apiClient.get(`/certification/projects/tasks/${taskId}/documents/${documentType}/workflow`),
  );
}

export function listCertificationMembers(taskId: string) {
  return unwrap<CertificationMember[]>(apiClient.get(`/certification/projects/tasks/${taskId}/members`));
}

export function saveCertificationMembers(taskId: string, payload: CertificationMember[]) {
  return unwrap<CertificationMember[]>(apiClient.put(`/certification/projects/tasks/${taskId}/members`, payload));
}

export function listCertificationWbsItems(taskId: string) {
  return unwrap<CertificationWbsItem[]>(apiClient.get(`/certification/projects/tasks/${taskId}/wbs`));
}

export function saveCertificationWbsItems(taskId: string, payload: CertificationWbsItem[]) {
  return unwrap<CertificationWbsItem[]>(apiClient.put(`/certification/projects/tasks/${taskId}/wbs`, payload));
}

export function listCertificationScheduleItems(taskId: string) {
  return unwrap<CertificationScheduleItem[]>(apiClient.get(`/certification/projects/tasks/${taskId}/schedule`));
}

export function saveCertificationScheduleItems(taskId: string, payload: CertificationScheduleItem[]) {
  return unwrap<CertificationScheduleItem[]>(apiClient.put(`/certification/projects/tasks/${taskId}/schedule`, payload));
}

export function listCertificationRisks(taskId: string) {
  return unwrap<CertificationRisk[]>(apiClient.get(`/certification/projects/tasks/${taskId}/risks`));
}

export function saveCertificationRisks(taskId: string, payload: CertificationRisk[]) {
  return unwrap<CertificationRisk[]>(apiClient.put(`/certification/projects/tasks/${taskId}/risks`, payload));
}

export function toggleCertificationRiskStatus(taskId: string, riskId: string, payload: CertificationRiskStatusPayload) {
  return unwrap<CertificationRisk>(
    apiClient.put(`/certification/projects/tasks/${taskId}/risks/${riskId}/status`, payload),
  );
}

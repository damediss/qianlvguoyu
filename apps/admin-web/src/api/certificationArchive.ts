import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  ArchivePageQuery,
  CertificationArchiveBasicSnapshot,
  CertificationArchiveCandidate,
  CertificationArchiveCompleteCheck,
  CertificationArchiveDetail,
  CertificationArchiveImportBatch,
  CertificationArchiveImportError,
  CertificationArchiveMaterial,
  CertificationArchiveRecord,
  CertificationArchiveTask,
} from '../types/certificationArchive';

const base = '/certification/archive';

export function pageAvailableArchiveTasks(params: ArchivePageQuery) {
  return unwrap<PageResponse<CertificationArchiveTask>>(apiClient.get(`${base}/tasks/available`, { params }));
}

export function pageArchiveRecords(params: ArchivePageQuery) {
  return unwrap<PageResponse<CertificationArchiveRecord>>(apiClient.get(`${base}/records`, { params }));
}

export function getArchiveDetail(archiveId: string) {
  return unwrap<CertificationArchiveDetail>(apiClient.get(`${base}/records/${archiveId}`));
}

export function archiveBasic(taskId: string) {
  return unwrap<CertificationArchiveBasicSnapshot>(apiClient.post(`${base}/tasks/${taskId}/basic`));
}

export function getArchiveBasic(archiveId: string) {
  return unwrap<CertificationArchiveBasicSnapshot>(apiClient.get(`${base}/records/${archiveId}/basic`));
}

export function listArchiveMaterials(archiveId: string) {
  return unwrap<CertificationArchiveMaterial[]>(apiClient.get(`${base}/records/${archiveId}/materials`));
}

export function uploadArchiveMaterial(archiveId: string, payload: FormData) {
  return unwrap<CertificationArchiveMaterial>(apiClient.post(`${base}/records/${archiveId}/materials`, payload));
}

export function deleteArchiveMaterial(archiveId: string, materialId: string) {
  return unwrap<void>(apiClient.delete(`${base}/records/${archiveId}/materials/${materialId}`));
}

export function importArchiveCandidateMaterials(archiveId: string, payload: FormData) {
  return unwrap<CertificationArchiveImportBatch>(apiClient.post(`${base}/records/${archiveId}/candidate-materials/import`, payload));
}

export function pageArchiveImportBatches(params: ArchivePageQuery) {
  return unwrap<PageResponse<CertificationArchiveImportBatch>>(apiClient.get(`${base}/import-batches`, { params }));
}

export function pageArchiveImportErrors(batchId: string, params: ArchivePageQuery) {
  return unwrap<PageResponse<CertificationArchiveImportError>>(apiClient.get(`${base}/import-batches/${batchId}/errors`, { params }));
}

export function confirmArchiveImportBatch(batchId: string) {
  return unwrap<CertificationArchiveImportBatch>(apiClient.post(`${base}/import-batches/${batchId}/confirm`));
}

export function cancelArchiveImportBatch(batchId: string) {
  return unwrap<CertificationArchiveImportBatch>(apiClient.post(`${base}/import-batches/${batchId}/cancel`));
}

export function pageArchiveCandidates(archiveId: string, params: ArchivePageQuery) {
  return unwrap<PageResponse<CertificationArchiveCandidate>>(apiClient.get(`${base}/records/${archiveId}/candidates`, { params }));
}

export function getArchiveCompleteCheck(archiveId: string) {
  return unwrap<CertificationArchiveCompleteCheck>(apiClient.get(`${base}/records/${archiveId}/complete-check`));
}

export function confirmArchiveRecord(archiveId: string, allowErrors = false) {
  return unwrap<CertificationArchiveCompleteCheck>(apiClient.post(`${base}/records/${archiveId}/confirm`, { allowErrors }));
}

export async function downloadArchiveFile(fileId: string, fileName: string) {
  const response = await apiClient.get(`${base}/files/${fileId}/download`, { responseType: 'blob' });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || 'archive-file';
  link.click();
  URL.revokeObjectURL(url);
}

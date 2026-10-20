import { apiClient, unwrap } from './client';
const base = '/certification/archive';
export function pageAvailableArchiveTasks(params) {
    return unwrap(apiClient.get(`${base}/tasks/available`, { params }));
}
export function pageArchiveRecords(params) {
    return unwrap(apiClient.get(`${base}/records`, { params }));
}
export function getArchiveDetail(archiveId) {
    return unwrap(apiClient.get(`${base}/records/${archiveId}`));
}
export function archiveBasic(taskId) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/basic`));
}
export function getArchiveBasic(archiveId) {
    return unwrap(apiClient.get(`${base}/records/${archiveId}/basic`));
}
export function listArchiveMaterials(archiveId) {
    return unwrap(apiClient.get(`${base}/records/${archiveId}/materials`));
}
export function uploadArchiveMaterial(archiveId, payload) {
    return unwrap(apiClient.post(`${base}/records/${archiveId}/materials`, payload));
}
export function deleteArchiveMaterial(archiveId, materialId) {
    return unwrap(apiClient.delete(`${base}/records/${archiveId}/materials/${materialId}`));
}
export function importArchiveCandidateMaterials(archiveId, payload) {
    return unwrap(apiClient.post(`${base}/records/${archiveId}/candidate-materials/import`, payload));
}
export function pageArchiveImportBatches(params) {
    return unwrap(apiClient.get(`${base}/import-batches`, { params }));
}
export function pageArchiveImportErrors(batchId, params) {
    return unwrap(apiClient.get(`${base}/import-batches/${batchId}/errors`, { params }));
}
export function confirmArchiveImportBatch(batchId) {
    return unwrap(apiClient.post(`${base}/import-batches/${batchId}/confirm`));
}
export function cancelArchiveImportBatch(batchId) {
    return unwrap(apiClient.post(`${base}/import-batches/${batchId}/cancel`));
}
export function pageArchiveCandidates(archiveId, params) {
    return unwrap(apiClient.get(`${base}/records/${archiveId}/candidates`, { params }));
}
export function getArchiveCompleteCheck(archiveId) {
    return unwrap(apiClient.get(`${base}/records/${archiveId}/complete-check`));
}
export function confirmArchiveRecord(archiveId, allowErrors = false) {
    return unwrap(apiClient.post(`${base}/records/${archiveId}/confirm`, { allowErrors }));
}
export async function downloadArchiveFile(fileId, fileName) {
    const response = await apiClient.get(`${base}/files/${fileId}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName || 'archive-file';
    link.click();
    URL.revokeObjectURL(url);
}

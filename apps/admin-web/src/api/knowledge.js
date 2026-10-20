import { apiClient, unwrap } from './client';
import { uploadPlatformFile } from './files';
export function pageSpaces(params) {
    return unwrap(apiClient.get('/knowledge/spaces', { params }));
}
export function createSpace(payload) {
    return unwrap(apiClient.post('/knowledge/spaces', payload));
}
export function updateSpace(id, payload) {
    return unwrap(apiClient.put(`/knowledge/spaces/${id}`, payload));
}
export function deleteSpace(id) {
    return unwrap(apiClient.delete(`/knowledge/spaces/${id}`));
}
export function pageDocuments(spaceId, params) {
    return unwrap(apiClient.get(`/knowledge/spaces/${spaceId}/documents`, { params }));
}
export function ingestDocument(spaceId, payload) {
    return unwrap(apiClient.post(`/knowledge/spaces/${spaceId}/documents`, payload));
}
export function searchKnowledge(payload) {
    return unwrap(apiClient.post('/knowledge/search', payload));
}
export function createAnswerDraft(payload) {
    const { question, spaceIds, categoryCode, topK } = payload;
    return unwrap(apiClient.post('/knowledge/answer-drafts', { question, spaceIds, categoryCode, topK }));
}
export function uploadFile(file, accessPolicy = 'private') {
    return uploadPlatformFile(file, accessPolicy);
}

import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  FileVO,
  KnowledgeAnswerDraft,
  KnowledgeDocument,
  KnowledgeSearchResult,
  KnowledgeSpace,
} from '../types/knowledge';

export interface SpaceQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  scopeType?: string;
  status?: string;
}

export interface SpacePayload {
  code: string;
  name: string;
  scopeType: string;
  ownerUserId?: number;
  categoryCode?: string;
  description?: string;
  status: string;
  visibleRoleCodes: string[];
}

export interface DocumentPayload {
  fileId: number;
  title: string;
  categoryCode?: string;
  sourceType: string;
  accessScope: string;
  sensitiveLevel: string;
  visibleUserIds?: number[];
  visibleRoleCodes?: string[];
  rawText?: string;
}

export interface SearchPayload {
  query: string;
  spaceIds?: number[];
  categoryCode?: string;
  topK?: number;
}

export function pageSpaces(params: SpaceQuery) {
  return unwrap<PageResponse<KnowledgeSpace>>(apiClient.get('/knowledge/spaces', { params }));
}

export function createSpace(payload: SpacePayload) {
  return unwrap<KnowledgeSpace>(apiClient.post('/knowledge/spaces', payload));
}

export function updateSpace(id: number, payload: Omit<SpacePayload, 'code' | 'scopeType' | 'ownerUserId'>) {
  return unwrap<KnowledgeSpace>(apiClient.put(`/knowledge/spaces/${id}`, payload));
}

export function deleteSpace(id: number) {
  return unwrap<void>(apiClient.delete(`/knowledge/spaces/${id}`));
}

export function pageDocuments(spaceId: number, params: { page?: number; pageSize?: number }) {
  return unwrap<PageResponse<KnowledgeDocument>>(apiClient.get(`/knowledge/spaces/${spaceId}/documents`, { params }));
}

export function ingestDocument(spaceId: number, payload: DocumentPayload) {
  return unwrap<KnowledgeDocument>(apiClient.post(`/knowledge/spaces/${spaceId}/documents`, payload));
}

export function searchKnowledge(payload: SearchPayload) {
  return unwrap<KnowledgeSearchResult[]>(apiClient.post('/knowledge/search', payload));
}

export function createAnswerDraft(payload: SearchPayload & { question: string }) {
  const { question, spaceIds, categoryCode, topK } = payload;
  return unwrap<KnowledgeAnswerDraft>(apiClient.post('/knowledge/answer-drafts', { question, spaceIds, categoryCode, topK }));
}

export async function uploadFile(file: File, accessPolicy = 'private') {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('accessPolicy', accessPolicy);
  return unwrap<FileVO>(apiClient.post('/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }));
}

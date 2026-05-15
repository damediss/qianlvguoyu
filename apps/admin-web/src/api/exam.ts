import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  ExamWorkType,
  ExamWorkTypeCreateReq,
  ExamWorkTypeUpdateReq,
  ExamWorkTypeQuery,
} from '../types/exam';

export function pageWorkTypes(params: ExamWorkTypeQuery) {
  return unwrap<PageResponse<ExamWorkType>>(apiClient.get('/exam/work-types', { params }));
}

export function treeWorkTypes() {
  return unwrap<ExamWorkType[]>(apiClient.get('/exam/work-types/tree'));
}

export function getWorkType(id: number) {
  return unwrap<ExamWorkType>(apiClient.get(`/exam/work-types/${id}`));
}

export function createWorkType(payload: ExamWorkTypeCreateReq) {
  return unwrap<ExamWorkType>(apiClient.post('/exam/work-types', payload));
}

export function updateWorkType(id: number, payload: ExamWorkTypeUpdateReq) {
  return unwrap<ExamWorkType>(apiClient.put(`/exam/work-types/${id}`, payload));
}

export function deleteWorkType(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/work-types/${id}`));
}

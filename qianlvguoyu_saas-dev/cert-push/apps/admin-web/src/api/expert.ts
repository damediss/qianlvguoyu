import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  ExpertTask,
  ExpertTaskQuery,
  ExpertAuditItem,
  ExpertSubmitReq,
  XimuListResponse,
  ExamshicaoListResponse,
} from '../types/expert';

export function pageStartTasks(params: ExpertTaskQuery) {
  return unwrap<PageResponse<ExpertTask>>(apiClient.get('/expert/start-tasks', { params }));
}

export function pageEndTasks(params: ExpertTaskQuery) {
  return unwrap<PageResponse<ExpertTask>>(apiClient.get('/expert/end-tasks', { params }));
}

export function getExamList(taskId: number, role: number) {
  return unwrap<ExpertAuditItem[]>(apiClient.get('/expert/exam-list', { params: { taskId, role } }));
}

export function submitExam(payload: ExpertSubmitReq) {
  return unwrap<{ count: number; total: number; isdone: number }>(apiClient.post('/expert/exam-submit', payload));
}

export function getExamshicaoList(taskId: number, role: number) {
  return unwrap<ExamshicaoListResponse>(apiClient.get('/expert/examshicao-list', { params: { taskId, role } }));
}

export function submitExamshicao(payload: ExpertSubmitReq) {
  return unwrap<{ count: number; total: number; isdone: number }>(apiClient.post('/expert/examshicao-submit', payload));
}

export function getExamsubList(taskId: number, role: number) {
  return unwrap<ExpertAuditItem[]>(apiClient.get('/expert/examsub-list', { params: { taskId, role } }));
}

export function submitExamsub(payload: ExpertSubmitReq) {
  return unwrap<{ count: number; total: number; isdone: number }>(apiClient.post('/expert/examsub-submit', payload));
}

export function getXimuList(taskId: number, role: number) {
  return unwrap<XimuListResponse>(apiClient.get('/expert/ximu-list', { params: { taskId, role } }));
}

export function submitXimu(payload: ExpertSubmitReq) {
  return unwrap<{ count: number; total: number; isdone: number }>(apiClient.post('/expert/ximu-submit', payload));
}

import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  ExpertTask,
  ExpertTaskQuery,
  ExpertAuditItem,
  ExpertSubmitReq,
  XimuListResponse,
  ExpertMyTaskVO,
  ExpertMyTaskQuery,
  ExpertTaskDetailVO,
  ExpertTaskSubmitReq,
  ExpertTaskSubmitResp,
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
  return unwrap<ExpertAuditItem[]>(apiClient.get('/expert/examshicao-list', { params: { taskId, role } }));
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

// ---- 题库审核指派 - 专家端(理论/实操/主观) ----
// 3 套 × 3 方法 = 9 个函数
// URL 前缀:
//   /expert/task-exams         理论题
//   /expert/task-examshicaos   实操题
//   /expert/task-examsubs      主观题

// 理论题 - exam
export function pageMyTaskExams(params: ExpertMyTaskQuery) {
  return unwrap<PageResponse<ExpertMyTaskVO>>(apiClient.get('/expert/task-exams', { params }));
}
export function getExpertTaskExamDetail(id: number) {
  return unwrap<ExpertTaskDetailVO>(apiClient.get(`/expert/task-exams/${id}`));
}
export function submitExpertTaskExam(id: number, payload: ExpertTaskSubmitReq) {
  return unwrap<ExpertTaskSubmitResp>(apiClient.post(`/expert/task-exams/${id}/submit`, payload));
}

// 实操题 - examshicao
export function pageMyTaskExamshicaos(params: ExpertMyTaskQuery) {
  return unwrap<PageResponse<ExpertMyTaskVO>>(apiClient.get('/expert/task-examshicaos', { params }));
}
export function getExpertTaskExamshicaoDetail(id: number) {
  return unwrap<ExpertTaskDetailVO>(apiClient.get(`/expert/task-examshicaos/${id}`));
}
export function submitExpertTaskExamshicao(id: number, payload: ExpertTaskSubmitReq) {
  return unwrap<ExpertTaskSubmitResp>(apiClient.post(`/expert/task-examshicaos/${id}/submit`, payload));
}

// 主观题 - examsub
export function pageMyTaskExamsubs(params: ExpertMyTaskQuery) {
  return unwrap<PageResponse<ExpertMyTaskVO>>(apiClient.get('/expert/task-examsubs', { params }));
}
export function getExpertTaskExamsubDetail(id: number) {
  return unwrap<ExpertTaskDetailVO>(apiClient.get(`/expert/task-examsubs/${id}`));
}
export function submitExpertTaskExamsub(id: number, payload: ExpertTaskSubmitReq) {
  return unwrap<ExpertTaskSubmitResp>(apiClient.post(`/expert/task-examsubs/${id}/submit`, payload));
}

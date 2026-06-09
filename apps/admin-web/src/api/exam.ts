import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  ExamWorkType,
  ExamWorkTypeCreateReq,
  ExamWorkTypeUpdateReq,
  ExamWorkTypeQuery,
  ExamCourse,
  ExamCourseCreateReq,
  ExamCourseUpdateReq,
  ExamCourseQuery,
  ExamUserAuthCourse,
  ExamUserAuthCourseCreateReq,
  ExamUserAuthCourseQuery,
  ExamTaskXimu,
  ExamTaskXimuDetail,
  ExamTaskXimuDaily,
  ExamTaskXimuQuery,
  ExamTaskXimuAssignExpertReq,
  PublicData,
  TaskAuditVO,
  TaskAuditQuery,
  TaskAuditDetailItem,
  TaskAuditDaily,
  TaskAuditAssignExpertReq,
  ExamKnowledgeStructure,
  ExamKnowledgeStructureCreateReq,
  ExamKnowledgeStructureUpdateReq,
  ExamExamDetail,
  ExamExamDetailCreateReq,
  ExamExamDetailUpdateReq,
  ExamExamDetailQuery,
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

export function pageCourses(params: ExamCourseQuery) {
  return unwrap<PageResponse<ExamCourse>>(apiClient.get('/exam/courses', { params }));
}

export function getCourse(id: number) {
  return unwrap<ExamCourse>(apiClient.get(`/exam/courses/${id}`));
}

export function createCourse(payload: ExamCourseCreateReq) {
  return unwrap<ExamCourse>(apiClient.post('/exam/courses', payload));
}

export function updateCourse(id: number, payload: ExamCourseUpdateReq) {
  return unwrap<ExamCourse>(apiClient.put(`/exam/courses/${id}`, payload));
}

export function deleteCourse(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/courses/${id}`));
}

export function pageUserAuthCourses(params: ExamUserAuthCourseQuery) {
  return unwrap<PageResponse<ExamUserAuthCourse>>(apiClient.get('/exam/user-auth-courses', { params }));
}

export function getUserAuthCourse(realname: string, idCard: string) {
  return unwrap<ExamUserAuthCourse>(apiClient.get('/exam/user-auth-courses/detail', {
    params: { realname, idCard },
  }));
}

export function createUserAuthCourse(payload: ExamUserAuthCourseCreateReq) {
  return unwrap<ExamUserAuthCourse>(apiClient.post('/exam/user-auth-courses', payload));
}

export function updateUserAuthCourse(realname: string, idCard: string, payload: ExamUserAuthCourseCreateReq) {
  return unwrap<ExamUserAuthCourse>(apiClient.put(
    `/exam/user-auth-courses/${encodeURIComponent(realname)}/${encodeURIComponent(idCard)}`,
    payload,
  ));
}

export function deleteUserAuthCourse(realname: string, idCard: string) {
  return unwrap<void>(apiClient.delete(
    `/exam/user-auth-courses/${encodeURIComponent(realname)}/${encodeURIComponent(idCard)}`,
  ));
}

// ---- Exercise task ximu ----
export function pageTaskXimus(params: ExamTaskXimuQuery) {
  return unwrap<PageResponse<ExamTaskXimu>>(apiClient.get('/exam/task-ximus', { params }));
}

export function getTaskXimuDetails(taskId: number) {
  return unwrap<ExamTaskXimuDetail[]>(apiClient.get(`/exam/task-ximus/${taskId}/details`));
}

export function getTaskXimuDaily(taskId: number, role: number) {
  return unwrap<ExamTaskXimuDaily[]>(apiClient.get(`/exam/task-ximus/${taskId}/daily`, { params: { role } }));
}

export function importTaskXimu(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/task-ximus/import', formData));
}

export function assignTaskXimuExpert(taskId: number, payload: ExamTaskXimuAssignExpertReq) {
  return unwrap<void>(apiClient.post(`/exam/task-ximus/${taskId}/assign-expert`, payload));
}

export function deleteTaskXimu(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/task-ximus/${id}`));
}

export function exportTaskXimu(id: number) {
  return apiClient.get(`/exam/task-ximus/${id}/export`, { responseType: 'blob' });
}

export function getPublicData() {
  return unwrap<PublicData>(apiClient.get('/exam/public-data'));
}

// ---- 题库审核指派 - 管理端(理论/实操/主观) ----
// 理论题(exam) / 实操(examshicao) / 主观(examsub) 各自一套 6 个函数
// 共 18 个函数,后台 URL 前缀:
//   /exam/task-exams         /exam/task-examshicaos         /exam/task-examsubs
//   /exam/task-exams/{id}    /exam/task-examshicaos/{id}    /exam/task-examsubs/{id}

// 理论题 - exam
export function pageTaskExamAudits(params: TaskAuditQuery) {
  return unwrap<PageResponse<TaskAuditVO>>(apiClient.get('/exam/task-exams', { params }));
}
export function getTaskExamAuditDetails(taskId: number) {
  return unwrap<TaskAuditDetailItem[]>(apiClient.get(`/exam/task-exams/${taskId}/details`));
}
export function deleteTaskExamAudit(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/task-exams/${id}`));
}
export function assignTaskExamAuditExpert(taskId: number, payload: TaskAuditAssignExpertReq) {
  return unwrap<void>(apiClient.post(`/exam/task-exams/${taskId}/assign-expert`, payload));
}
export function getTaskExamAuditDaily(taskId: number, role: number) {
  return unwrap<TaskAuditDaily[]>(apiClient.get(`/exam/task-exams/${taskId}/daily`, { params: { role } }));
}
export function exportTaskExamAudit(id: number) {
  return apiClient.get(`/exam/task-exams/${id}/export`, { responseType: 'blob' });
}

// 实操题 - examshicao
export function pageTaskExamshicaoAudits(params: TaskAuditQuery) {
  return unwrap<PageResponse<TaskAuditVO>>(apiClient.get('/exam/task-examshicaos', { params }));
}
export function getTaskExamshicaoAuditDetails(taskId: number) {
  return unwrap<TaskAuditDetailItem[]>(apiClient.get(`/exam/task-examshicaos/${taskId}/details`));
}
export function deleteTaskExamshicaoAudit(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/task-examshicaos/${id}`));
}
export function assignTaskExamshicaoAuditExpert(taskId: number, payload: TaskAuditAssignExpertReq) {
  return unwrap<void>(apiClient.post(`/exam/task-examshicaos/${taskId}/assign-expert`, payload));
}
export function getTaskExamshicaoAuditDaily(taskId: number, role: number) {
  return unwrap<TaskAuditDaily[]>(apiClient.get(`/exam/task-examshicaos/${taskId}/daily`, { params: { role } }));
}
export function exportTaskExamshicaoAudit(id: number) {
  return apiClient.get(`/exam/task-examshicaos/${id}/export`, { responseType: 'blob' });
}

// 主观题 - examsub
export function pageTaskExamsubAudits(params: TaskAuditQuery) {
  return unwrap<PageResponse<TaskAuditVO>>(apiClient.get('/exam/task-examsubs', { params }));
}
export function getTaskExamsubAuditDetails(taskId: number) {
  return unwrap<TaskAuditDetailItem[]>(apiClient.get(`/exam/task-examsubs/${taskId}/details`));
}
export function deleteTaskExamsubAudit(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/task-examsubs/${id}`));
}
export function assignTaskExamsubAuditExpert(taskId: number, payload: TaskAuditAssignExpertReq) {
  return unwrap<void>(apiClient.post(`/exam/task-examsubs/${taskId}/assign-expert`, payload));
}
export function getTaskExamsubAuditDaily(taskId: number, role: number) {
  return unwrap<TaskAuditDaily[]>(apiClient.get(`/exam/task-examsubs/${taskId}/daily`, { params: { role } }));
}
export function exportTaskExamsubAudit(id: number) {
  return apiClient.get(`/exam/task-examsubs/${id}/export`, { responseType: 'blob' });
}

// ---- 题库审核指派 - 导入(理论/实操/主观 3 端点) ----
// 服务端 @PostMapping 接 MultipartFile + workTypeId(Integer) + level(Integer)
// 权限: task:exam-audit:import / task:examshicao-audit:import / task:examsub-audit:import
// 返回 Integer:成功导入的题数

export function importTaskExam(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/task-exams/import', formData));
}

export function importTaskExamshicao(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/task-examshicaos/import', formData));
}

export function importTaskExamsub(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/task-examsubs/import', formData));
}

// ---- 试题管理 — 知识点 ----
export function treeKnowledgeStructures(courseId?: number, type?: number) {
  return unwrap<ExamKnowledgeStructure[]>(
    apiClient.get('/exam/knowledge-structures/tree', { params: { courseId, type } }),
  );
}

export function createKnowledgeStructure(payload: ExamKnowledgeStructureCreateReq) {
  return unwrap<ExamKnowledgeStructure>(apiClient.post('/exam/knowledge-structures', payload));
}

export function updateKnowledgeStructure(id: number, payload: ExamKnowledgeStructureUpdateReq) {
  return unwrap<ExamKnowledgeStructure>(apiClient.put(`/exam/knowledge-structures/${id}`, payload));
}

export function deleteKnowledgeStructure(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/knowledge-structures/${id}`));
}

export function batchImportKnowledgeStructures(courseId: number | undefined, type: number | undefined, lines: string[]) {
  return unwrap<number>(apiClient.post('/exam/knowledge-structures/batch', lines, {
    params: { courseId, type },
  }));
}

// ---- 试题管理 — 试题详情 ----
export function pageExamDetails(params: ExamExamDetailQuery) {
  return unwrap<PageResponse<ExamExamDetail>>(apiClient.get('/exam/exam-details', { params }));
}

export function getExamDetail(id: number) {
  return unwrap<ExamExamDetail>(apiClient.get(`/exam/exam-details/${id}`));
}

export function createExamDetail(payload: ExamExamDetailCreateReq) {
  return unwrap<ExamExamDetail>(apiClient.post('/exam/exam-details', payload));
}

export function updateExamDetail(id: number, payload: ExamExamDetailUpdateReq) {
  return unwrap<ExamExamDetail>(apiClient.put(`/exam/exam-details/${id}`, payload));
}

export function deleteExamDetail(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/exam-details/${id}`));
}

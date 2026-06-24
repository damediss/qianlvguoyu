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
  QuestionBankTreeNodeVO,
  ExamExamDetail,
  ExamExamDetailCreateReq,
  ExamExamDetailUpdateReq,
  ExamExamDetailQuery,
  CompanyQuestionCreateReq,
  CompanyQuestionUpdateReq,
  CompanyQuestionQuery,
  ExamMakeExamPlan,
  ExamMakeExamPlanCreateReq,
  ExamMakeExamPlanQuery,
  ExamKnowledgeRate,
  ExamKnowledgeRateCreateReq,
  ExamKnowledgeRateQuery,
  ExamPaperDetail,
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

/** Excel 导入知识点: FormData 包含 file + courseId + type */
export function importKnowledgeStructureExcel(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/knowledge-structures/import-excel', formData));
}

/** 导出知识点:返回 blob 流 */
export function exportKnowledgeStructures(courseId?: number, type?: number) {
  return apiClient.get('/exam/knowledge-structures/export', {
    params: { courseId, type },
    responseType: 'blob',
  });
}

/** 批量删除知识点 */
export function batchDeleteKnowledgeStructures(ids: number[]) {
  return unwrap<number>(apiClient.post('/exam/knowledge-structures/batch-delete', ids));
}

/**
 * 题库分类导航聚合树:工种→课程→知识点 L0~L3 一次性返回,每节点带
 * questionCount(子树递归题数)。替代前端三次串行 lazy-load。
 * 权限: exam:question:view
 */
export function getQuestionBankTree() {
  return unwrap<QuestionBankTreeNodeVO[]>(apiClient.get('/exam/question-bank-tree'));
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

/** 批量删除试题,后端返回成功删除的数量 */
export function batchDeleteExamDetails(ids: number[]) {
  return unwrap<number>(apiClient.post('/exam/exam-details/batch-delete', ids));
}

/** 导入试题: FormData 包含 file + courseId + source */
export function importExamDetails(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/exam-details/import', formData));
}

/** 导出试题:返回 blob 流 */
export function exportExamDetails(courseId: number) {
  return apiClient.get('/exam/exam-details/export', {
    params: { courseId },
    responseType: 'blob',
  });
}

// ---- 公司试题管理 ----
export function pageCompanyQuestions(params: CompanyQuestionQuery) {
  return unwrap<PageResponse<ExamExamDetail>>(apiClient.get('/exam/company-questions', { params }));
}

export function createCompanyQuestion(payload: CompanyQuestionCreateReq) {
  return unwrap<ExamExamDetail>(apiClient.post('/exam/company-questions', payload));
}

export function updateCompanyQuestion(id: number, payload: CompanyQuestionUpdateReq) {
  return unwrap<ExamExamDetail>(apiClient.put(`/exam/company-questions/${id}`, payload));
}

export function deleteCompanyQuestion(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/company-questions/${id}`));
}

export function batchDeleteCompanyQuestions(ids: number[]) {
  return unwrap<number>(apiClient.post('/exam/company-questions/batch-delete', ids));
}

/** 导入公司试题: FormData 包含 file + orgId + orgName + source */
export function importCompanyQuestions(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/company-questions/import', formData));
}

/** 导出公司试题:返回 blob 流,orgId 为空时导出全部公司试题 */
export function exportCompanyQuestions(orgId?: number) {
  return apiClient.get('/exam/company-questions/export', {
    params: { orgId },
    responseType: 'blob',
  });
}

// ==================== 组卷计划 ====================

export function pageMakeExamPlans(params: ExamMakeExamPlanQuery) {
  return unwrap<PageResponse<ExamMakeExamPlan>>(apiClient.get('/exam/make-exam-plans', { params }));
}

export function listMakeExamPlans(courseId?: number, examType?: number) {
  return unwrap<ExamMakeExamPlan[]>(apiClient.get('/exam/make-exam-plans/list', { params: { courseId, examType } }));
}

export function getMakeExamPlan(id: number) {
  return unwrap<ExamMakeExamPlan>(apiClient.get(`/exam/make-exam-plans/${id}`));
}

export function createMakeExamPlan(payload: ExamMakeExamPlanCreateReq) {
  return unwrap<ExamMakeExamPlan>(apiClient.post('/exam/make-exam-plans', payload));
}

export function updateMakeExamPlan(id: number, payload: ExamMakeExamPlanCreateReq) {
  return unwrap<ExamMakeExamPlan>(apiClient.put(`/exam/make-exam-plans/${id}`, payload));
}

export function deleteMakeExamPlan(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/make-exam-plans/${id}`));
}

// ==================== 知识比重 ====================

export function pageKnowledgeRates(params: ExamKnowledgeRateQuery) {
  return unwrap<PageResponse<ExamKnowledgeRate>>(apiClient.get('/exam/knowledge-rates', { params }));
}

export function listKnowledgeRates(courseId?: number) {
  return unwrap<ExamKnowledgeRate[]>(apiClient.get('/exam/knowledge-rates/list', { params: { courseId } }));
}

export function getKnowledgeRate(id: number) {
  return unwrap<ExamKnowledgeRate>(apiClient.get(`/exam/knowledge-rates/${id}`));
}

export function createKnowledgeRate(payload: ExamKnowledgeRateCreateReq) {
  return unwrap<ExamKnowledgeRate>(apiClient.post('/exam/knowledge-rates', payload));
}

export function updateKnowledgeRate(id: number, payload: ExamKnowledgeRateCreateReq) {
  return unwrap<ExamKnowledgeRate>(apiClient.put(`/exam/knowledge-rates/${id}`, payload));
}

export function deleteKnowledgeRate(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/knowledge-rates/${id}`));
}

// ==================== 试卷 ====================

export function getPaperDetails(paperId: number) {
  return unwrap<ExamPaperDetail[]>(apiClient.get(`/exam/papers/${paperId}/details`));
}

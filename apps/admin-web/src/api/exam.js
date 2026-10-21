import { apiClient, unwrap } from './client';
export function pageWorkTypes(params) {
    return unwrap(apiClient.get('/exam/work-types', { params }));
}
export function treeWorkTypes() {
    return unwrap(apiClient.get('/exam/work-types/tree'));
}
export function getWorkType(id) {
    return unwrap(apiClient.get(`/exam/work-types/${id}`));
}
export function createWorkType(payload) {
    return unwrap(apiClient.post('/exam/work-types', payload));
}
export function updateWorkType(id, payload) {
    return unwrap(apiClient.put(`/exam/work-types/${id}`, payload));
}
export function deleteWorkType(id) {
    return unwrap(apiClient.delete(`/exam/work-types/${id}`));
}
export function pageCourses(params) {
    return unwrap(apiClient.get('/exam/courses', { params }));
}
export function getCourse(id) {
    return unwrap(apiClient.get(`/exam/courses/${id}`));
}
export function createCourse(payload) {
    return unwrap(apiClient.post('/exam/courses', payload));
}
export function updateCourse(id, payload) {
    return unwrap(apiClient.put(`/exam/courses/${id}`, payload));
}
export function deleteCourse(id) {
    return unwrap(apiClient.delete(`/exam/courses/${id}`));
}
export function pageUserAuthCourses(params) {
    return unwrap(apiClient.get('/exam/user-auth-courses', { params }));
}
export function getUserAuthCourse(realname, idCard) {
    return unwrap(apiClient.get('/exam/user-auth-courses/detail', {
        params: { realname, idCard },
    }));
}
export function createUserAuthCourse(payload) {
    return unwrap(apiClient.post('/exam/user-auth-courses', payload));
}
export function updateUserAuthCourse(realname, idCard, payload) {
    return unwrap(apiClient.put(`/exam/user-auth-courses/${encodeURIComponent(realname)}/${encodeURIComponent(idCard)}`, payload));
}
export function deleteUserAuthCourse(realname, idCard) {
    return unwrap(apiClient.delete(`/exam/user-auth-courses/${encodeURIComponent(realname)}/${encodeURIComponent(idCard)}`));
}
// ---- Exercise task ximu ----
export function pageTaskXimus(params) {
    return unwrap(apiClient.get('/exam/task-ximus', { params }));
}
export function getTaskXimuDetails(taskId) {
    return unwrap(apiClient.get(`/exam/task-ximus/${taskId}/details`));
}
export function getTaskXimuDaily(taskId, role) {
    return unwrap(apiClient.get(`/exam/task-ximus/${taskId}/daily`, { params: { role } }));
}
export function importTaskXimu(formData) {
    return unwrap(apiClient.post('/exam/task-ximus/import', formData));
}
export function assignTaskXimuExpert(taskId, payload) {
    return unwrap(apiClient.post(`/exam/task-ximus/${taskId}/assign-expert`, payload));
}
export function deleteTaskXimu(id) {
    return unwrap(apiClient.delete(`/exam/task-ximus/${id}`));
}
export function exportTaskXimu(id) {
    return apiClient.get(`/exam/task-ximus/${id}/export`, { responseType: 'blob' });
}
export function getPublicData() {
    return unwrap(apiClient.get('/exam/public-data'));
}
// ---- 题库审核指派 - 管理端(理论/实操/主观) ----
// 理论题(exam) / 实操(examshicao) / 主观(examsub) 各自一套 6 个函数
// 共 18 个函数,后台 URL 前缀:
//   /exam/task-exams         /exam/task-examshicaos         /exam/task-examsubs
//   /exam/task-exams/{id}    /exam/task-examshicaos/{id}    /exam/task-examsubs/{id}
// 理论题 - exam
export function pageTaskExamAudits(params) {
    return unwrap(apiClient.get('/exam/task-exams', { params }));
}
export function getTaskExamAuditDetails(taskId) {
    return unwrap(apiClient.get(`/exam/task-exams/${taskId}/details`));
}
export function deleteTaskExamAudit(id) {
    return unwrap(apiClient.delete(`/exam/task-exams/${id}`));
}
export function assignTaskExamAuditExpert(taskId, payload) {
    return unwrap(apiClient.post(`/exam/task-exams/${taskId}/assign-expert`, payload));
}
export function getTaskExamAuditDaily(taskId, role) {
    return unwrap(apiClient.get(`/exam/task-exams/${taskId}/daily`, { params: { role } }));
}
export function exportTaskExamAudit(id) {
    return apiClient.get(`/exam/task-exams/${id}/export`, { responseType: 'blob' });
}
// 实操题 - examshicao
export function pageTaskExamshicaoAudits(params) {
    return unwrap(apiClient.get('/exam/task-examshicaos', { params }));
}
export function getTaskExamshicaoAuditDetails(taskId) {
    return unwrap(apiClient.get(`/exam/task-examshicaos/${taskId}/details`));
}
export function deleteTaskExamshicaoAudit(id) {
    return unwrap(apiClient.delete(`/exam/task-examshicaos/${id}`));
}
export function assignTaskExamshicaoAuditExpert(taskId, payload) {
    return unwrap(apiClient.post(`/exam/task-examshicaos/${taskId}/assign-expert`, payload));
}
export function getTaskExamshicaoAuditDaily(taskId, role) {
    return unwrap(apiClient.get(`/exam/task-examshicaos/${taskId}/daily`, { params: { role } }));
}
export function exportTaskExamshicaoAudit(id) {
    return apiClient.get(`/exam/task-examshicaos/${id}/export`, { responseType: 'blob' });
}
// 主观题 - examsub
export function pageTaskExamsubAudits(params) {
    return unwrap(apiClient.get('/exam/task-examsubs', { params }));
}
export function getTaskExamsubAuditDetails(taskId) {
    return unwrap(apiClient.get(`/exam/task-examsubs/${taskId}/details`));
}
export function deleteTaskExamsubAudit(id) {
    return unwrap(apiClient.delete(`/exam/task-examsubs/${id}`));
}
export function assignTaskExamsubAuditExpert(taskId, payload) {
    return unwrap(apiClient.post(`/exam/task-examsubs/${taskId}/assign-expert`, payload));
}
export function getTaskExamsubAuditDaily(taskId, role) {
    return unwrap(apiClient.get(`/exam/task-examsubs/${taskId}/daily`, { params: { role } }));
}
export function exportTaskExamsubAudit(id) {
    return apiClient.get(`/exam/task-examsubs/${id}/export`, { responseType: 'blob' });
}
// ---- 题库审核指派 - 导入(理论/实操/主观 3 端点) ----
// 服务端 @PostMapping 接 MultipartFile + workTypeId(Integer) + level(Integer)
// 权限: task:exam-audit:import / task:examshicao-audit:import / task:examsub-audit:import
// 返回 Integer:成功导入的题数
export function importTaskExam(formData) {
    return unwrap(apiClient.post('/exam/task-exams/import', formData));
}
export function importTaskExamshicao(formData) {
    return unwrap(apiClient.post('/exam/task-examshicaos/import', formData));
}
export function importTaskExamsub(formData) {
    return unwrap(apiClient.post('/exam/task-examsubs/import', formData));
}
// ---- 试题管理 — 知识点 ----
export function treeKnowledgeStructures(courseId, type) {
    return unwrap(apiClient.get('/exam/knowledge-structures/tree', { params: { courseId, type } }));
}
export function createKnowledgeStructure(payload) {
    return unwrap(apiClient.post('/exam/knowledge-structures', payload));
}
export function updateKnowledgeStructure(id, payload) {
    return unwrap(apiClient.put(`/exam/knowledge-structures/${id}`, payload));
}
export function deleteKnowledgeStructure(id) {
    return unwrap(apiClient.delete(`/exam/knowledge-structures/${id}`));
}
export function batchImportKnowledgeStructures(courseId, type, lines) {
    return unwrap(apiClient.post('/exam/knowledge-structures/batch', lines, {
        params: { courseId, type },
    }));
}
/** Excel 导入知识点: FormData 包含 file + courseId + type */
export function importKnowledgeStructureExcel(formData) {
    return unwrap(apiClient.post('/exam/knowledge-structures/import-excel', formData));
}
/** 导出知识点:返回 blob 流 */
export function exportKnowledgeStructures(courseId, type) {
    return apiClient.get('/exam/knowledge-structures/export', {
        params: { courseId, type },
        responseType: 'blob',
    });
}
/** 批量删除知识点 */
export function batchDeleteKnowledgeStructures(ids) {
    return unwrap(apiClient.post('/exam/knowledge-structures/batch-delete', ids));
}
/**
 * 题库分类导航聚合树:工种→课程→知识点 L0~L3 一次性返回,每节点带
 * questionCount(子树递归题数)。替代前端三次串行 lazy-load。
 * 权限: exam:question:view
 */
export function getQuestionBankTree() {
    return unwrap(apiClient.get('/exam/question-bank-tree'));
}
// ---- 试题管理 — 试题详情 ----
export function pageExamDetails(params) {
    return unwrap(apiClient.get('/exam/exam-details', { params }));
}
export function getExamDetail(id) {
    return unwrap(apiClient.get(`/exam/exam-details/${id}`));
}
export function createExamDetail(payload) {
    return unwrap(apiClient.post('/exam/exam-details', payload));
}
export function updateExamDetail(id, payload) {
    return unwrap(apiClient.put(`/exam/exam-details/${id}`, payload));
}
export function deleteExamDetail(id) {
    return unwrap(apiClient.delete(`/exam/exam-details/${id}`));
}
/** 批量删除试题,后端返回成功删除的数量 */
export function batchDeleteExamDetails(ids) {
    return unwrap(apiClient.post('/exam/exam-details/batch-delete', ids));
}
/** 导入试题: FormData 包含 file + courseId + source */
export function importExamDetails(formData) {
    return unwrap(apiClient.post('/exam/exam-details/import', formData));
}
/** 导出试题:返回 blob 流;传入 ids 时仅导出勾选的试题(勾选可跨页),否则导出整个科目 */
export function exportExamDetails(courseId, ids) {
    return apiClient.get('/exam/exam-details/export', {
        params: {
            courseId,
            ...(ids && ids.length > 0 ? { ids } : {}),
        },
        responseType: 'blob',
    });
}
// ---- 公司试题管理 ----
export function pageCompanyQuestions(params) {
    return unwrap(apiClient.get('/exam/company-questions', { params }));
}
export function createCompanyQuestion(payload) {
    return unwrap(apiClient.post('/exam/company-questions', payload));
}
export function updateCompanyQuestion(id, payload) {
    return unwrap(apiClient.put(`/exam/company-questions/${id}`, payload));
}
export function deleteCompanyQuestion(id) {
    return unwrap(apiClient.delete(`/exam/company-questions/${id}`));
}
export function batchDeleteCompanyQuestions(ids) {
    return unwrap(apiClient.post('/exam/company-questions/batch-delete', ids));
}
/** 导入公司试题: FormData 包含 file + orgId + orgName + source */
export function importCompanyQuestions(formData) {
    return unwrap(apiClient.post('/exam/company-questions/import', formData));
}
/** 导出公司试题:返回 blob 流,orgId 为空时导出全部公司试题 */
export function exportCompanyQuestions(orgId) {
    return apiClient.get('/exam/company-questions/export', {
        params: { orgId },
        responseType: 'blob',
    });
}
// ==================== 组卷计划 ====================
export function pageMakeExamPlans(params) {
    return unwrap(apiClient.get('/exam/make-exam-plans', { params }));
}
export function listMakeExamPlans(courseId, examType) {
    return unwrap(apiClient.get('/exam/make-exam-plans/list', { params: { courseId, examType } }));
}
export function getMakeExamPlan(id) {
    return unwrap(apiClient.get(`/exam/make-exam-plans/${id}`));
}
export function createMakeExamPlan(payload) {
    return unwrap(apiClient.post('/exam/make-exam-plans', payload));
}
export function updateMakeExamPlan(id, payload) {
    return unwrap(apiClient.put(`/exam/make-exam-plans/${id}`, payload));
}
export function deleteMakeExamPlan(id) {
    return unwrap(apiClient.delete(`/exam/make-exam-plans/${id}`));
}
// ==================== 知识比重 ====================
export function pageKnowledgeRates(params) {
    return unwrap(apiClient.get('/exam/knowledge-rates', { params }));
}
export function listKnowledgeRates(courseId) {
    return unwrap(apiClient.get('/exam/knowledge-rates/list', { params: { courseId } }));
}
export function getKnowledgeRate(id) {
    return unwrap(apiClient.get(`/exam/knowledge-rates/${id}`));
}
export function createKnowledgeRate(payload) {
    return unwrap(apiClient.post('/exam/knowledge-rates', payload));
}
export function updateKnowledgeRate(id, payload) {
    return unwrap(apiClient.put(`/exam/knowledge-rates/${id}`, payload));
}
export function deleteKnowledgeRate(id) {
    return unwrap(apiClient.delete(`/exam/knowledge-rates/${id}`));
}
// ==================== 试卷 ====================
export function getPaperDetails(paperId) {
    return unwrap(apiClient.get(`/exam/papers/${paperId}/details`));
}
// ==================== FAQ 转换 ====================
export function submitFaqConversion(payload) {
    return unwrap(apiClient.post('/exam/faq/conversion/submit', payload));
}
export function listFaqBatches(params) {
    return unwrap(apiClient.get('/exam/faq/conversion/batches', { params }));
}
export function getFaqBatchProgress(batchId) {
    return unwrap(apiClient.get(`/exam/faq/conversion/batches/${batchId}/progress`));
}
export function pageFaqRecords(params) {
    return unwrap(apiClient.get('/exam/faq/conversion/records', { params }));
}
export function getFaqRecordDetail(id) {
    return unwrap(apiClient.get(`/exam/faq/conversion/records/${id}`));
}
export function pauseFaqBatch(batchId) {
    return unwrap(apiClient.post(`/exam/faq/conversion/batches/${batchId}/pause`));
}
export function resumeFaqBatch(batchId) {
    return unwrap(apiClient.post(`/exam/faq/conversion/batches/${batchId}/resume`));
}
export function retryFaqBatch(batchId) {
    return unwrap(apiClient.post(`/exam/faq/conversion/batches/${batchId}/retry`));
}
export async function exportFaqBatch(batchId) {
    return (await apiClient.get(`/exam/faq/conversion/batches/${batchId}/export`, { responseType: 'blob' })).data;
}
export function listEnabledFaqModels() {
    return unwrap(apiClient.get('/exam/faq/conversion/models/enabled'));
}
// ---- LLM 模型配置 ----
export function pageFaqModelConfigs(params) {
    return unwrap(apiClient.get('/exam/faq/model-config/page', { params }));
}
export function getFaqModelConfig(id) {
    return unwrap(apiClient.get(`/exam/faq/model-config/${id}`));
}
export function createFaqModelConfig(payload) {
    return unwrap(apiClient.post('/exam/faq/model-config', payload));
}
export function updateFaqModelConfig(id, payload) {
    return unwrap(apiClient.put(`/exam/faq/model-config/${id}`, payload));
}
export function deleteFaqModelConfig(id) {
    return unwrap(apiClient.delete(`/exam/faq/model-config/${id}`));
}
// ---- WeKnora 配置 ----
export function getFaqWeknoraConfig() {
    return unwrap(apiClient.get('/exam/faq/weknora-config'));
}
export function saveFaqWeknoraConfig(payload) {
    return unwrap(apiClient.post('/exam/faq/weknora-config', payload));
}

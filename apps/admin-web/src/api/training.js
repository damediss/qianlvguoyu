import { apiClient, unwrap } from './client';
import { fetchFileBlob } from './files';
import { parseContentDispositionFilename } from '../utils/download';
const BASE = '/training';
// ==================== Needs (培训需求) ====================
export function pageNeeds(params) {
    return unwrap(apiClient.get(`${BASE}/needs`, { params }));
}
export function getNeed(id) {
    return unwrap(apiClient.get(`${BASE}/needs/${id}`));
}
export function createNeed(payload) {
    return unwrap(apiClient.post(`${BASE}/needs`, payload));
}
export function updateNeed(id, payload) {
    return unwrap(apiClient.put(`${BASE}/needs/${id}`, payload));
}
export function deleteNeed(id) {
    return unwrap(apiClient.delete(`${BASE}/needs/${id}`));
}
// ==================== Solutions (培训方案) ====================
export function pageSolutions(params) {
    return unwrap(apiClient.get(`${BASE}/solutions`, { params }));
}
export function getSolution(id) {
    return unwrap(apiClient.get(`${BASE}/solutions/${id}`));
}
export function createSolution(payload) {
    return unwrap(apiClient.post(`${BASE}/solutions`, payload));
}
export function updateSolution(id, payload) {
    return unwrap(apiClient.put(`${BASE}/solutions/${id}`, payload));
}
export function deleteSolution(id) {
    return unwrap(apiClient.delete(`${BASE}/solutions/${id}`));
}
// ==================== Requests (培训请求) ====================
export function pageRequests(params) {
    return unwrap(apiClient.get(`${BASE}/requests`, { params }));
}
export function getRequest(id) {
    return unwrap(apiClient.get(`${BASE}/requests/${id}`));
}
export function createRequest(payload) {
    return unwrap(apiClient.post(`${BASE}/requests`, payload));
}
export function updateRequest(id, payload) {
    return unwrap(apiClient.put(`${BASE}/requests/${id}`, payload));
}
export function deleteRequest(id) {
    return unwrap(apiClient.delete(`${BASE}/requests/${id}`));
}
// ==================== Declares (培训申报) ====================
export function pageDeclares(params) {
    return unwrap(apiClient.get(`${BASE}/declares`, { params }));
}
export function getDeclare(id) {
    return unwrap(apiClient.get(`${BASE}/declares/${id}`));
}
export function createDeclare(payload) {
    return unwrap(apiClient.post(`${BASE}/declares`, payload));
}
export function updateDeclare(id, payload) {
    return unwrap(apiClient.put(`${BASE}/declares/${id}`, payload));
}
export function deleteDeclare(id) {
    return unwrap(apiClient.delete(`${BASE}/declares/${id}`));
}
// ==================== People List (培训人员) ====================
export function pagePeople(params) {
    return unwrap(apiClient.get(`${BASE}/people-lists`, { params }));
}
export function getPeople(id) {
    return unwrap(apiClient.get(`${BASE}/people-lists/${id}`));
}
export function createPeople(payload) {
    return unwrap(apiClient.post(`${BASE}/people-lists`, payload));
}
export function updatePeople(id, payload) {
    return unwrap(apiClient.put(`${BASE}/people-lists/${id}`, payload));
}
export function deletePeople(id) {
    return unwrap(apiClient.delete(`${BASE}/people-lists/${id}`));
}
export function batchAuditPeople(payload) {
    return unwrap(apiClient.put(`${BASE}/people-lists/batch-audit`, payload));
}
// ==================== Class List (班级管理) ====================
export function pageClasses(params) {
    return unwrap(apiClient.get(`${BASE}/class-lists`, { params }));
}
export function getClass(id) {
    return unwrap(apiClient.get(`${BASE}/class-lists/${id}`));
}
export function createClass(payload) {
    return unwrap(apiClient.post(`${BASE}/class-lists`, payload));
}
export function updateClass(id, payload) {
    return unwrap(apiClient.put(`${BASE}/class-lists/${id}`, payload));
}
export function deleteClass(id) {
    return unwrap(apiClient.delete(`${BASE}/class-lists/${id}`));
}
export function batchAssignClass(payload) {
    return unwrap(apiClient.post(`${BASE}/class-lists/batch-assign`, payload));
}
// ==================== Courses (课程/课时) ====================
export function pageCourses(params) {
    return unwrap(apiClient.get(`${BASE}/courses`, { params }));
}
export function getCourse(id) {
    return unwrap(apiClient.get(`${BASE}/courses/${id}`));
}
export function createCourse(payload) {
    return unwrap(apiClient.post(`${BASE}/courses`, payload));
}
export function updateCourse(id, payload) {
    return unwrap(apiClient.put(`${BASE}/courses/${id}`, payload));
}
export function deleteCourse(id) {
    return unwrap(apiClient.delete(`${BASE}/courses/${id}`));
}
// Kejians (课件)
export function pageKejians(params) {
    return unwrap(apiClient.get(`${BASE}/course-kejians`, { params }));
}
export function getKejian(id) {
    return unwrap(apiClient.get(`${BASE}/course-kejians/${id}`));
}
export function createKejian(payload) {
    return unwrap(apiClient.post(`${BASE}/course-kejians`, payload));
}
export function updateKejian(id, payload) {
    return unwrap(apiClient.put(`${BASE}/course-kejians/${id}`, payload));
}
export function deleteKejian(id) {
    return unwrap(apiClient.delete(`${BASE}/course-kejians/${id}`));
}
// ==================== Resources (资源管理) ====================
export function pageResources(params) {
    return unwrap(apiClient.get(`${BASE}/resources`, { params }));
}
export function getResource(id) {
    return unwrap(apiClient.get(`${BASE}/resources/${id}`));
}
export function createResource(payload) {
    return unwrap(apiClient.post(`${BASE}/resources`, payload));
}
export function updateResource(id, payload) {
    return unwrap(apiClient.put(`${BASE}/resources/${id}`, payload));
}
export function deleteResource(id) {
    return unwrap(apiClient.delete(`${BASE}/resources/${id}`));
}
// ==================== Task List (任务管理) ====================
export function pageTasks(params) {
    return unwrap(apiClient.get(`${BASE}/task-lists`, { params }));
}
export function getTask(id) {
    return unwrap(apiClient.get(`${BASE}/task-lists/${id}`));
}
export function createTask(payload) {
    return unwrap(apiClient.post(`${BASE}/task-lists`, payload));
}
export function updateTask(id, payload) {
    return unwrap(apiClient.put(`${BASE}/task-lists/${id}`, payload));
}
export function deleteTask(id) {
    return unwrap(apiClient.delete(`${BASE}/task-lists/${id}`));
}
// Task Infos (培训配置)
export function pageTaskInfos(params) {
    return unwrap(apiClient.get(`${BASE}/task-infos`, { params }));
}
export function getTaskInfo(id) {
    return unwrap(apiClient.get(`${BASE}/task-infos/${id}`));
}
export function createTaskInfo(payload) {
    return unwrap(apiClient.post(`${BASE}/task-infos`, payload));
}
export function updateTaskInfo(id, payload) {
    return unwrap(apiClient.put(`${BASE}/task-infos/${id}`, payload));
}
export function deleteTaskInfo(id) {
    return unwrap(apiClient.delete(`${BASE}/task-infos/${id}`));
}
export function pageExams(params) {
    return unwrap(apiClient.get(`${BASE}/exam-lists`, { params }));
}
export function getExam(id) {
    return unwrap(apiClient.get(`${BASE}/exam-lists/${id}`));
}
export function createExam(payload) {
    return unwrap(apiClient.post(`${BASE}/exam-lists`, payload));
}
export function updateExam(id, payload) {
    return unwrap(apiClient.put(`${BASE}/exam-lists/${id}`, payload));
}
export function deleteExam(id) {
    return unwrap(apiClient.delete(`${BASE}/exam-lists/${id}`));
}
// ----- 考生管理 -----
export function pageExamPeople(examId, params) {
    return unwrap(apiClient.get(`${BASE}/exam-lists/${examId}/people-grades`, { params }));
}
export function updateExamPeopleGrade(id, body) {
    return unwrap(apiClient.put(`${BASE}/exam-lists/people-grades/${id}`, body));
}
export function importExamPeopleByIdCard(examId, idCardNumbers) {
    return unwrap(apiClient.post(`${BASE}/exam-lists/${examId}/import-people`, { idCardNumbers }));
}
export function importExamPeopleFromTrain(examId, trainId) {
    return unwrap(apiClient.post(`${BASE}/exam-lists/${examId}/import-from-train/${trainId}`));
}
export function batchDeleteExamPeople(ids) {
    return unwrap(apiClient.post(`${BASE}/exam-lists/people/batch-delete`, { ids }));
}
// Exam Results (考试结果)
export function pageExamResults(params) {
    return unwrap(apiClient.get(`${BASE}/exam-results`, { params }));
}
export function getExamResult(id) {
    return unwrap(apiClient.get(`${BASE}/exam-results/${id}`));
}
export function createExamResult(payload) {
    return unwrap(apiClient.post(`${BASE}/exam-results`, payload));
}
export function updateExamResult(id, payload) {
    return unwrap(apiClient.put(`${BASE}/exam-results/${id}`, payload));
}
export function deleteExamResult(id) {
    return unwrap(apiClient.delete(`${BASE}/exam-results/${id}`));
}
// ==================== Protocols (协议管理) ====================
export function pageProtocols(params) {
    return unwrap(apiClient.get(`${BASE}/protocols`, { params }));
}
export function getProtocol(id) {
    return unwrap(apiClient.get(`${BASE}/protocols/${id}`));
}
export function createProtocol(payload) {
    return unwrap(apiClient.post(`${BASE}/protocols`, payload));
}
export function updateProtocol(id, payload) {
    return unwrap(apiClient.put(`${BASE}/protocols/${id}`, payload));
}
export function deleteProtocol(id) {
    return unwrap(apiClient.delete(`${BASE}/protocols/${id}`));
}
// ==================== Projects (项目管理) ====================
export function pageProjects(params) {
    return unwrap(apiClient.get(`${BASE}/projects`, { params }));
}
export function getProject(id) {
    return unwrap(apiClient.get(`${BASE}/projects/${id}`));
}
export function createProject(payload) {
    return unwrap(apiClient.post(`${BASE}/projects`, payload));
}
export function updateProject(id, payload) {
    return unwrap(apiClient.put(`${BASE}/projects/${id}`, payload));
}
export function deleteProject(id) {
    return unwrap(apiClient.delete(`${BASE}/projects/${id}`));
}
// ==================== Project Tables (十大管理表) ====================
export function pageProjectTables(params) {
    return unwrap(apiClient.get(`${BASE}/project-tables`, { params }));
}
export function getProjectTable(id) {
    return unwrap(apiClient.get(`${BASE}/project-tables/${id}`));
}
export function createProjectTable(payload) {
    return unwrap(apiClient.post(`${BASE}/project-tables`, payload));
}
export function updateProjectTable(id, payload) {
    return unwrap(apiClient.put(`${BASE}/project-tables/${id}`, payload));
}
export function deleteProjectTable(id) {
    return unwrap(apiClient.delete(`${BASE}/project-tables/${id}`));
}
// ==================== Dashboard (仪表盘) ====================
export function getDashboard() {
    return unwrap(apiClient.get(`${BASE}/dashboard`));
}
/** 同步练习（AI 同步学）统计面板：一次拿全，默认近 30 天 */
export function getReviewSyncPanel(days = 30) {
    return unwrap(apiClient.get(`${BASE}/dashboard/review-sync`, { params: { days } }));
}
export function getDashboardMilestones() {
    return unwrap(apiClient.get(`${BASE}/dashboard/milestones`));
}
export function getDashboardStageDistribution() {
    return unwrap(apiClient.get(`${BASE}/dashboard/stage-distribution`));
}
export function getDashboardCompanies() {
    return unwrap(apiClient.get(`${BASE}/dashboard/companies`));
}
// ==================== Talent Needs (人才数据管理) ====================
export function pageTalentNeeds(params) {
    return unwrap(apiClient.get(`${BASE}/talent-needs`, { params }));
}
export function getTalentNeed(id) {
    return unwrap(apiClient.get(`${BASE}/talent-needs/${id}`));
}
export function createTalentNeed(payload) {
    return unwrap(apiClient.post(`${BASE}/talent-needs`, payload));
}
export function updateTalentNeed(id, payload) {
    return unwrap(apiClient.put(`${BASE}/talent-needs/${id}`, payload));
}
export function deleteTalentNeed(id) {
    return unwrap(apiClient.delete(`${BASE}/talent-needs/${id}`));
}
// ==================== Platform Users (平台用户) ====================
export function pagePlatformUsers(params) {
    return unwrap(apiClient.get(`${BASE}/platform-users`, { params }));
}
export function getPlatformUser(id) {
    return unwrap(apiClient.get(`${BASE}/platform-users/${id}`));
}
export function createPlatformUser(payload) {
    return unwrap(apiClient.post(`${BASE}/platform-users`, payload));
}
export function updatePlatformUser(id, payload) {
    return unwrap(apiClient.put(`${BASE}/platform-users/${id}`, payload));
}
export function deletePlatformUser(id) {
    return unwrap(apiClient.delete(`${BASE}/platform-users/${id}`));
}
// ==================== Teacher Bank (师资管理) ====================
export function pageTeacherBanks(params) {
    return unwrap(apiClient.get(`${BASE}/teacher-banks`, { params }));
}
export function getTeacherBank(id) {
    return unwrap(apiClient.get(`${BASE}/teacher-banks/${id}`));
}
export function createTeacherBank(payload) {
    return unwrap(apiClient.post(`${BASE}/teacher-banks`, payload));
}
export function updateTeacherBank(id, payload) {
    return unwrap(apiClient.put(`${BASE}/teacher-banks/${id}`, payload));
}
export function deleteTeacherBank(id) {
    return unwrap(apiClient.delete(`${BASE}/teacher-banks/${id}`));
}
export async function downloadTeacherBankImportTemplate() {
    const response = await apiClient.get(`${BASE}/teacher-banks/import/template`, {
        responseType: 'blob',
    });
    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response) || '教师信息导入模板.xlsx';
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}
export function importTeacherBanks(file) {
    const form = new FormData();
    form.append('file', file);
    return unwrap(apiClient.post(`${BASE}/teacher-banks/import`, form));
}
export function migrateTeacherBanksFromLegacy(file, migrateImages = true) {
    const form = new FormData();
    form.append('file', file);
    return unwrap(apiClient.post(`${BASE}/teacher-banks/migrate-from-legacy`, form, {
        params: { migrateImages },
        timeout: 600000,
    }));
}
export function bindTeacherBankFile(id, fileType, fileId) {
    return unwrap(apiClient.put(`${BASE}/teacher-banks/${id}/files/${encodeURIComponent(fileType)}`, {
        fileId,
    }));
}
export function clearTeacherBankFile(id, fileType) {
    return unwrap(apiClient.delete(`${BASE}/teacher-banks/${id}/files/${encodeURIComponent(fileType)}`));
}
// ==================== User Info (用户信息) ====================
export function pageUserInfos(params) {
    return unwrap(apiClient.get(`${BASE}/user-infos`, { params }));
}
export function getUserInfo(id) {
    return unwrap(apiClient.get(`${BASE}/user-infos/${id}`));
}
export function createUserInfo(payload) {
    return unwrap(apiClient.post(`${BASE}/user-infos`, payload));
}
export function updateUserInfo(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-infos/${id}`, payload));
}
export function deleteUserInfo(id) {
    return unwrap(apiClient.delete(`${BASE}/user-infos/${id}`));
}
// ==================== Budgets (预算费用管理) ====================
export function pageBudgets(params) {
    return unwrap(apiClient.get(`${BASE}/budgets`, { params }));
}
export function getBudget(id) {
    return unwrap(apiClient.get(`${BASE}/budgets/${id}`));
}
export function createBudget(payload) {
    return unwrap(apiClient.post(`${BASE}/budgets`, payload));
}
export function updateBudget(id, payload) {
    return unwrap(apiClient.put(`${BASE}/budgets/${id}`, payload));
}
export function deleteBudget(id) {
    return unwrap(apiClient.delete(`${BASE}/budgets/${id}`));
}
// ==================== File Lists (文件管理) ====================
export function pageFileLists(params) {
    return unwrap(apiClient.get(`${BASE}/file-lists`, { params }));
}
export function getFileList(id) {
    return unwrap(apiClient.get(`${BASE}/file-lists/${id}`));
}
export function createFileList(payload) {
    return unwrap(apiClient.post(`${BASE}/file-lists`, payload));
}
export function updateFileList(id, payload) {
    return unwrap(apiClient.put(`${BASE}/file-lists/${id}`, payload));
}
export function deleteFileList(id) {
    return unwrap(apiClient.delete(`${BASE}/file-lists/${id}`));
}
// ==================== File Details (文件明细) ====================
export function pageFileDetails(params) {
    return unwrap(apiClient.get(`${BASE}/file-details`, { params }));
}
export function getFileDetail(id) {
    return unwrap(apiClient.get(`${BASE}/file-details/${id}`));
}
export function createFileDetail(payload) {
    return unwrap(apiClient.post(`${BASE}/file-details`, payload));
}
export function updateFileDetail(id, payload) {
    return unwrap(apiClient.put(`${BASE}/file-details/${id}`, payload));
}
export function deleteFileDetail(id) {
    return unwrap(apiClient.delete(`${BASE}/file-details/${id}`));
}
// ==================== Info Lists (培训信息) ====================
export function pageInfoLists(params) {
    const { limit, pageSize, page, ...rest } = params;
    return unwrap(apiClient.get(`${BASE}/info-lists`, {
        params: {
            ...rest,
            page,
            pageSize: pageSize ?? limit ?? 10,
        },
    }));
}
export function getInfoList(id) {
    return unwrap(apiClient.get(`${BASE}/info-lists/${id}`));
}
export function createInfoList(payload) {
    return unwrap(apiClient.post(`${BASE}/info-lists`, payload));
}
export function updateInfoList(id, payload) {
    return unwrap(apiClient.put(`${BASE}/info-lists/${id}`, payload));
}
export function deleteInfoList(id) {
    return unwrap(apiClient.delete(`${BASE}/info-lists/${id}`));
}
// ==================== Info Classes (培训班级信息) ====================
export function pageInfoClasses(params) {
    return unwrap(apiClient.get(`${BASE}/info-classes`, { params }));
}
export function getInfoClass(id) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${id}`));
}
export function createInfoClass(payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes`, payload));
}
export function updateInfoClass(id, payload) {
    return unwrap(apiClient.put(`${BASE}/info-classes/${id}`, payload));
}
export function deleteInfoClass(id) {
    return unwrap(apiClient.delete(`${BASE}/info-classes/${id}`));
}
// ==================== Task Info Details (任务培训配置详情) ====================
export function pageTaskInfoDetails(params) {
    return unwrap(apiClient.get(`${BASE}/task-info-details`, { params }));
}
export function getTaskInfoDetail(id) {
    return unwrap(apiClient.get(`${BASE}/task-info-details/${id}`));
}
export function createTaskInfoDetail(payload) {
    return unwrap(apiClient.post(`${BASE}/task-info-details`, payload));
}
export function updateTaskInfoDetail(id, payload) {
    return unwrap(apiClient.put(`${BASE}/task-info-details/${id}`, payload));
}
export function deleteTaskInfoDetail(id) {
    return unwrap(apiClient.delete(`${BASE}/task-info-details/${id}`));
}
// ==================== Task Logs (任务日志) ====================
export function pageTaskLogs(params) {
    return unwrap(apiClient.get(`${BASE}/task-logs`, { params }));
}
export function getTaskLog(id) {
    return unwrap(apiClient.get(`${BASE}/task-logs/${id}`));
}
export function createTaskLog(payload) {
    return unwrap(apiClient.post(`${BASE}/task-logs`, payload));
}
export function updateTaskLog(id, payload) {
    return unwrap(apiClient.put(`${BASE}/task-logs/${id}`, payload));
}
export function deleteTaskLog(id) {
    return unwrap(apiClient.delete(`${BASE}/task-logs/${id}`));
}
// ==================== User Learn Logs (用户学习记录) ====================
export function pageUserLearnLogs(params) {
    return unwrap(apiClient.get(`${BASE}/user-learn-logs`, { params }));
}
export function getUserLearnLog(id) {
    return unwrap(apiClient.get(`${BASE}/user-learn-logs/${id}`));
}
export function createUserLearnLog(payload) {
    return unwrap(apiClient.post(`${BASE}/user-learn-logs`, payload));
}
export function updateUserLearnLog(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-learn-logs/${id}`, payload));
}
export function deleteUserLearnLog(id) {
    return unwrap(apiClient.delete(`${BASE}/user-learn-logs/${id}`));
}
// ==================== User Course Plans (用户选课计划) ====================
export function pageUserCoursePlans(params) {
    return unwrap(apiClient.get(`${BASE}/user-course-plans`, { params }));
}
export function getUserCoursePlan(id) {
    return unwrap(apiClient.get(`${BASE}/user-course-plans/${id}`));
}
export function createUserCoursePlan(payload) {
    return unwrap(apiClient.post(`${BASE}/user-course-plans`, payload));
}
export function updateUserCoursePlan(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-course-plans/${id}`, payload));
}
export function deleteUserCoursePlan(id) {
    return unwrap(apiClient.delete(`${BASE}/user-course-plans/${id}`));
}
// ==================== User Course Papers (用户课程证书) ====================
export function pageUserCoursePapers(params) {
    return unwrap(apiClient.get(`${BASE}/user-course-papers`, { params }));
}
export function getUserCoursePaper(id) {
    return unwrap(apiClient.get(`${BASE}/user-course-papers/${id}`));
}
export function createUserCoursePaper(payload) {
    return unwrap(apiClient.post(`${BASE}/user-course-papers`, payload));
}
export function updateUserCoursePaper(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-course-papers/${id}`, payload));
}
export function deleteUserCoursePaper(id) {
    return unwrap(apiClient.delete(`${BASE}/user-course-papers/${id}`));
}
// ==================== User Papers (用户证书) ====================
export function pageUserPapers(params) {
    return unwrap(apiClient.get(`${BASE}/user-papers`, { params }));
}
export function getUserPaper(id) {
    return unwrap(apiClient.get(`${BASE}/user-papers/${id}`));
}
export function createUserPaper(payload) {
    return unwrap(apiClient.post(`${BASE}/user-papers`, payload));
}
export function updateUserPaper(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-papers/${id}`, payload));
}
export function deleteUserPaper(id) {
    return unwrap(apiClient.delete(`${BASE}/user-papers/${id}`));
}
// ==================== User Patents (用户专利) ====================
export function pageUserPatents(params) {
    return unwrap(apiClient.get(`${BASE}/user-patents`, { params }));
}
export function getUserPatent(id) {
    return unwrap(apiClient.get(`${BASE}/user-patents/${id}`));
}
export function createUserPatent(payload) {
    return unwrap(apiClient.post(`${BASE}/user-patents`, payload));
}
export function updateUserPatent(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-patents/${id}`, payload));
}
export function deleteUserPatent(id) {
    return unwrap(apiClient.delete(`${BASE}/user-patents/${id}`));
}
// ==================== User Exam Logs (用户考试日志) ====================
export function pageUserExamLogs(params) {
    return unwrap(apiClient.get(`${BASE}/user-exam-logs`, { params }));
}
export function getUserExamLog(id) {
    return unwrap(apiClient.get(`${BASE}/user-exam-logs/${id}`));
}
export function createUserExamLog(payload) {
    return unwrap(apiClient.post(`${BASE}/user-exam-logs`, payload));
}
export function updateUserExamLog(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-exam-logs/${id}`, payload));
}
export function deleteUserExamLog(id) {
    return unwrap(apiClient.delete(`${BASE}/user-exam-logs/${id}`));
}
// ==================== User Abilities (用户能力) ====================
export function pageUserAbilities(params) {
    return unwrap(apiClient.get(`${BASE}/user-abilities`, { params }));
}
export function getUserAbility(id) {
    return unwrap(apiClient.get(`${BASE}/user-abilities/${id}`));
}
export function createUserAbility(payload) {
    return unwrap(apiClient.post(`${BASE}/user-abilities`, payload));
}
export function updateUserAbility(id, payload) {
    return unwrap(apiClient.put(`${BASE}/user-abilities/${id}`, payload));
}
export function deleteUserAbility(id) {
    return unwrap(apiClient.delete(`${BASE}/user-abilities/${id}`));
}
// ==================== Summaries (培训总结/经验萃取) ====================
export function pageSummaries(params) {
    return unwrap(apiClient.get(`${BASE}/summaries`, { params }));
}
export function getSummary(id) {
    return unwrap(apiClient.get(`${BASE}/summaries/${id}`));
}
export function createSummary(payload) {
    return unwrap(apiClient.post(`${BASE}/summaries`, payload));
}
export function updateSummary(id, payload) {
    return unwrap(apiClient.put(`${BASE}/summaries/${id}`, payload));
}
export function deleteSummary(id) {
    return unwrap(apiClient.delete(`${BASE}/summaries/${id}`));
}
// ==================== Certificates (证书管理) ====================
export function pageCertificates(params) {
    return unwrap(apiClient.get(`${BASE}/certificates`, { params }));
}
export function getCertificate(id) {
    return unwrap(apiClient.get(`${BASE}/certificates/${id}`));
}
export function createCertificate(payload) {
    return unwrap(apiClient.post(`${BASE}/certificates`, payload));
}
export function updateCertificate(id, payload) {
    return unwrap(apiClient.put(`${BASE}/certificates/${id}`, payload));
}
export function deleteCertificate(id) {
    return unwrap(apiClient.delete(`${BASE}/certificates/${id}`));
}
// ==================== Demand Train (企业培训需求) ====================
export function pageDemandTrains(params) {
    return unwrap(apiClient.get(`${BASE}/demand-trains`, { params }));
}
export function getDemandTrain(id) {
    return unwrap(apiClient.get(`${BASE}/demand-trains/${id}`));
}
export function createDemandTrain(payload) {
    return unwrap(apiClient.post(`${BASE}/demand-trains`, payload));
}
export function updateDemandTrain(id, payload) {
    return unwrap(apiClient.put(`${BASE}/demand-trains/${id}`, payload));
}
export function deleteDemandTrain(id) {
    return unwrap(apiClient.delete(`${BASE}/demand-trains/${id}`));
}
// ==================== Demand Employee (企业人才需求) ====================
export function pageDemandEmployees(params) {
    return unwrap(apiClient.get(`${BASE}/demand-employees`, { params }));
}
export function getDemandEmployee(id) {
    return unwrap(apiClient.get(`${BASE}/demand-employees/${id}`));
}
export function createDemandEmployee(payload) {
    return unwrap(apiClient.post(`${BASE}/demand-employees`, payload));
}
export function updateDemandEmployee(id, payload) {
    return unwrap(apiClient.put(`${BASE}/demand-employees/${id}`, payload));
}
export function deleteDemandEmployee(id) {
    return unwrap(apiClient.delete(`${BASE}/demand-employees/${id}`));
}
export function batchDeleteDemandEmployees(ids) {
    return unwrap(apiClient.post(`${BASE}/demand-employees/batch-delete`, ids));
}
/** 待处理需求列表，del=0 正常数据，del=1 回收站 */
export function pageDemandPendings(params) {
    return unwrap(apiClient.get(`${BASE}/demand-trains/pendings`, { params }));
}
/** 公共数据（企业/工种/申报条件等），tags 可选 ['company','worktype','traininfo','kejian','condition'] */
export function getDemandPublicData(tags) {
    return unwrap(apiClient.get(`${BASE}/demand-trains/public-data`, { params: tags ? { tags } : {} }));
}
/** 需求转培训开班 */
export function convertDemandToTraining(payload) {
    return unwrap(apiClient.post(`${BASE}/demand-trains/convert`, payload));
}
// ==================== Training Program (培训方案管理) ====================
/** 方案列表（含费用汇总） */
export function pageSolutionList(params) {
    return unwrap(apiClient.get(`${BASE}/solutions/list`, { params }));
}
/** 方案详情（含经费明细） */
export function getSolutionDetail(id) {
    return unwrap(apiClient.get(`${BASE}/solutions/${id}/detail`));
}
/** 保存方案（创建/更新 + 经费明细） */
export function saveSolution(payload) {
    return unwrap(apiClient.post(`${BASE}/solutions/save`, payload));
}
/** 删除方案 */
export function deleteSolutionById(id) {
    return unwrap(apiClient.delete(`${BASE}/solutions/${id}`));
}
/** 批量删除企业需求 */
export function batchDeleteDemandTrains(ids) {
    return unwrap(apiClient.post(`${BASE}/demand-trains/batch-delete`, ids));
}
/** 根据需求ID获取报名学员列表 */
export function getDemandStudents(id) {
    return unwrap(apiClient.get(`${BASE}/demand-trains/${id}/students`));
}
/** 根据需求ID获取方案详情 */
export function getSolutionByDemandId(traindemandId) {
    return unwrap(apiClient.get(`${BASE}/solutions/by-demand-id`, { params: { traindemandId } }));
}
/** 企业确认方案 */
export function companyConfirmInfoList(id) {
    return unwrap(apiClient.post(`${BASE}/info-lists/${id}/company-confirm`));
}
/** 企业驳回方案 */
export function companyRejectInfoList(id, reason) {
    return unwrap(apiClient.post(`${BASE}/info-lists/${id}/company-reject`, null, { params: { reason } }));
}
// ==================== Info Classes (班级管理 - 新版) ====================
/** 班级列表（含企业名、工种名、培训标题） */
export function pageInfoClassList(params) {
    return unwrap(apiClient.get(`${BASE}/info-classes`, { params }));
}
export function companyConfirmInfoClass(id) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${id}/company-confirm`));
}
export function companyRejectInfoClass(id, reason) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${id}/company-reject`, null, { params: { reason } }));
}
export function ensureClassCollection(classId) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/ensure-collection`));
}
export function getClassPracticeScope(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/practice-scope`));
}
export function listPracticeScopeCourses(params) {
    return unwrap(apiClient.get(`${BASE}/practice-scope/options/courses`, { params }));
}
export function listPracticeScopeKnowledgeTree(courseId) {
    return unwrap(apiClient.get(`${BASE}/practice-scope/options/knowledge-tree`, { params: { courseId } }));
}
export function suggestClassPracticeCourses(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/practice-scope/suggest-courses`));
}
export function saveClassPracticeScope(classId, payload) {
    return unwrap(apiClient.put(`${BASE}/info-classes/${classId}/practice-scope`, payload));
}
export function getClassReviewSyncConfig(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/review-sync-config`));
}
export function saveClassReviewSyncConfig(classId, payload) {
    return unwrap(apiClient.put(`${BASE}/info-classes/${classId}/review-sync-config`, {
        ...payload,
        enabled: payload.enabled === false || payload.enabled === 0 ? 0 : 1,
    }));
}
export function listClassReviewSyncExamOptions(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/review-sync-config/exam-options`));
}
export function enrollClassReviewSyncRoster(classId) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/review-sync-config/enroll-roster`));
}
export function ensureClassReviewSyncRoster(classId) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/review-sync-config/ensure-roster`));
}
export function ensureClassReviewSyncGray(classId) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/review-sync-config/ensure-gray`));
}
function practiceAnalyticsParams(params) {
    return {
        from: params?.from,
        to: params?.to,
        inScopeOnly: params?.inScopeOnly ?? true,
        keyword: params?.keyword,
        page: params?.page,
        pageSize: params?.pageSize,
        limit: params?.limit,
    };
}
export function getClassPracticeAnalyticsSummary(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/summary`, {
        params: practiceAnalyticsParams(params),
    }));
}
export function getClassPracticeAnalyticsStudents(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/students`, {
        params: practiceAnalyticsParams(params),
    }));
}
export function getClassPracticeAnalyticsKnowledges(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/knowledges`, {
        params: practiceAnalyticsParams(params),
    }));
}
export function getClassPracticeAnalyticsDaily(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/daily`, {
        params: practiceAnalyticsParams(params),
    }));
}
/** 导出班级刷题学情 Excel：指定日期区间，或 allTime=true 导出全部历史 */
export async function exportClassPracticeAnalytics(classId, params) {
    const response = await apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/export`, {
        params: {
            from: params?.allTime ? undefined : params?.from,
            to: params?.allTime ? undefined : params?.to,
            inScopeOnly: params?.inScopeOnly ?? true,
            allTime: params?.allTime ?? false,
        },
        responseType: 'blob',
    });
    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response) || `刷题学情_${classId}.xlsx`;
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}
/** 新增/编辑班级 */
export function saveInfoClass(payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes/save`, payload));
}
/** 批量删除班级 */
export function batchDeleteInfoClasses(ids) {
    return unwrap(apiClient.post(`${BASE}/info-classes/batch-delete`, { ids }));
}
// ==================== 班级学员 ====================
/** 班级学员列表 */
export function listClassStudents(classId, currentPage = 1, limit = 20) {
    return unwrap(apiClient.get(`${BASE}/class-student-list`, { params: { classId, currentPage, limit } }));
}
/** 编辑学员 */
export function saveClassStudent(params) {
    return unwrap(apiClient.post(`${BASE}/class-student-save`, params));
}
/** 批量删除学员 */
export function deleteClassStudents(studentIds) {
    return unwrap(apiClient.post(`${BASE}/class-student-del`, { studentIds }));
}
/** 可导入报名人员 */
export function listSignUpSelf(traininfoId, keyword, classId) {
    return unwrap(apiClient.get(`${BASE}/sign-up-self-list`, { params: { traininfoId, keyword, classId } }));
}
/** 从报名导入学员 */
export function importStudents(params) {
    return unwrap(apiClient.post(`${BASE}/class-student-import`, params));
}
/** 按身份证号导入学员 */
export function importStudentsByIdCard(params) {
    return unwrap(apiClient.post(`${BASE}/class-student-import-by-idcard`, params));
}
/** 班级花名册补开 / 对齐 App 账号（PHP 双写） */
export function syncAppAccountsForClass(classId) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/people/sync-app-accounts`, undefined, { timeout: 180000 }));
}
// ==================== 教务/负责人 ====================
/** 教务/负责人列表 */
export function listClassOthers(train_info_id, id, tag) {
    return unwrap(apiClient.get(`${BASE}/class-other`, { params: { train_info_id, id, tag } }));
}
/** 新增/编辑教务/负责人 */
export function saveClassOther(params) {
    return unwrap(apiClient.post(`${BASE}/class-other-save`, params));
}
/** 批量删除教务/负责人 */
export function deleteClassOthers(ids) {
    return unwrap(apiClient.post(`${BASE}/class-other-del`, { ids }));
}
/** 可导入教务数据源（教师库） */
export function listOtherImportData(tag) {
    return unwrap(apiClient.get(`${BASE}/class-other-import-data`, { params: { tag } }));
}
/** 从教师库导入教务 */
export function importOthers(params) {
    return unwrap(apiClient.post(`${BASE}/class-other-import`, params));
}
/** 班级角色列表 */
export function listClassRoles(classId) {
    return unwrap(apiClient.get(`${BASE}/classes/${classId}/roles`));
}
/** 班级角色摘要（跟班） */
export function getClassRoleSummary(classId) {
    return unwrap(apiClient.get(`${BASE}/classes/${classId}/role-summary`));
}
/** 指派班级角色 */
export function assignClassRole(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/classes/${classId}/roles/assign`, payload));
}
/** 卸任班级角色 */
export function endClassRole(classId, roleId) {
    return unwrap(apiClient.post(`${BASE}/classes/${classId}/roles/${roleId}/end`));
}
// ==================== 培训任务 ====================
/** 班级培训任务列表 */
export function listClassTasks(classId) {
    return unwrap(apiClient.get(`${BASE}/class-task`, { params: { classId } }));
}
/** 新增/编辑培训任务 */
export function saveClassTask(params) {
    return unwrap(apiClient.post(`${BASE}/class-task-save`, params));
}
/** 材料模板列表 type=1 培训 */
export function listFileTemps(params) {
    const ids = params.ids?.length ? params.ids.join(',') : undefined;
    return unwrap(apiClient.get(`${BASE}/file-temps/list`, {
        params: { type: params.type ?? 1, ids },
    }));
}
export function saveFileTemp(payload) {
    return unwrap(apiClient.post(`${BASE}/file-temps/save`, payload));
}
export function batchDeleteFileTemps(ids) {
    return unwrap(apiClient.post(`${BASE}/file-temps/batch-delete`, { ids }));
}
// ==================== 班级文件模板挂载 ====================
/** 挂载文件模板到班级 */
export function attachFileTemps(classId, ids) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/file-temps`, { ids }));
}
/** 移除班级文件模板 */
export function detachFileTemp(classId, templateId) {
    return unwrap(apiClient.delete(`${BASE}/info-classes/${classId}/file-temps/${templateId}`));
}
// ==================== 班级材料生成 ====================
/** 生成班级申报材料 */
export function generateMaterials(classId) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/materials/generate`));
}
// ==================== 毕业管理 / 毕业典礼（O3） ====================
/** 可毕业班级列表 */
export function listGraduationCandidates(params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/graduation-candidates`, {
        params,
        paramsSerializer: {
            // Spring List 绑定：trainTitle=a&trainTitle=b（不要 trainTitle[]=）
            indexes: null,
        },
    }));
}
/** 班级可毕业学员列表 */
export function listGraduationStudents(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/graduation-students`));
}
export function getGraduationStudentDetail(classId, peopleId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/graduation-students/${peopleId}`));
}
export function updateGraduationStudent(classId, peopleId, payload) {
    return unwrap(apiClient.patch(`${BASE}/info-classes/${classId}/graduation-students/${peopleId}`, payload));
}
/** 证书文件夹导入 · 预览匹配（按已选班身份证） */
export function previewGraduationCertImport(classIds, files) {
    const form = new FormData();
    classIds.forEach((id) => form.append('classIds', String(id)));
    files.forEach((file) => form.append('file', file));
    return unwrap(apiClient.post(`${BASE}/graduation/certificates/import/preview`, form, { timeout: 300000 }));
}
/** 异步预览任务状态 */
export function getGraduationCertImportPreviewJob(jobId) {
    return unwrap(apiClient.get(`${BASE}/graduation/certificates/import/preview-jobs/${encodeURIComponent(jobId)}`));
}
/** 证书文件夹导入 · 确认回填证号+文件 */
export function commitGraduationCertImport(previewId) {
    return unwrap(apiClient.post(`${BASE}/graduation/certificates/import/commit`, { previewId }, { timeout: 300000 }));
}
export async function exportCertifiedRoster(classId, classTitle, scope = 'certified') {
    const response = await apiClient.get(`${BASE}/info-classes/${classId}/certified-roster/export`, {
        params: {
            ...(classTitle ? { classTitle } : {}),
            scope,
        },
        responseType: 'blob',
    });
    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response) ||
        `职业技能等级认定获证人员名册_${classId}.xlsx`;
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}
/** 导出获证人员名册（多班为 ZIP；单班为 xlsx） */
export async function exportCertifiedRosters(classIds, scope = 'certified') {
    const ids = classIds.map(String).filter(Boolean);
    if (ids.length === 0) {
        throw new Error('请选择班级');
    }
    const response = await apiClient.get(`${BASE}/graduation/certified-roster/export`, {
        params: { classIds: ids, scope },
        paramsSerializer: { indexes: null },
        responseType: 'blob',
    });
    const isZip = ids.length > 1;
    const blob = new Blob([response.data], {
        type: isZip
            ? 'application/zip'
            : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response) ||
        (isZip ? `获证人员名册_已选${ids.length}班.zip` : `职业技能等级认定获证人员名册.xlsx`);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}
// ==================== 课表配置 ====================
/** 获取班级课表（空且有开班课表文件时后端会自动解析导入） */
export function getClassSchedule(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/schedule`));
}
/** 批量保存课表（跟班调课；不回写开班上传文件） */
export function saveClassSchedule(classId, items) {
    return unwrap(apiClient.put(`${BASE}/info-classes/${classId}/schedule`, { items }));
}
/** 从开班准备上传的 Excel 导入/重导结构化课表 */
export function importClassScheduleFromKcbiao(classId, force = false, confirmOverwriteManual = false) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/schedule/import-from-kcbiao`, null, {
        params: { force, confirmOverwriteManual },
    }));
}
/** 课表双源状态 */
export function getClassScheduleStatus(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/schedule/status`));
}
/** 导出课表 */
export function exportClassSchedule(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/schedule/export`));
}
export function listScheduleChangeRequests(params) {
    return unwrap(apiClient.get(`${BASE}/schedule-change-requests`, { params }));
}
export function getScheduleChangeRequest(id) {
    return unwrap(apiClient.get(`${BASE}/schedule-change-requests/${id}`));
}
export function approveScheduleChangeRequest(id, force = false) {
    return unwrap(apiClient.post(`${BASE}/schedule-change-requests/${id}/approve`, null, {
        params: { force },
    }));
}
export function rejectScheduleChangeRequest(id, body) {
    return unwrap(apiClient.post(`${BASE}/schedule-change-requests/${id}/reject`, body ?? {}));
}
export function listFollowTasks(classId, taskType = 'assignment') {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/tasks`, { params: { taskType } }));
}
export function createFollowTask(payload) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks`, payload));
}
export function updateFollowTask(id, payload) {
    return unwrap(apiClient.put(`${BASE}/follow-tasks/${id}`, payload));
}
export function publishFollowTask(id) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks/${id}/publish`));
}
export function closeFollowTask(id) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks/${id}/close`));
}
export function reopenFollowTask(id) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks/${id}/reopen`));
}
export function getMockUsedQuestionIds(classId, excludeTaskId) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/mock-used-question-ids`, {
        params: excludeTaskId != null ? { excludeTaskId } : undefined,
    }));
}
export function listMockPlanOptions(classId) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/mock-plan-options`));
}
export function createMockWithPlan(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks/classes/${classId}/mock-with-plan`, payload));
}
export function createMockFromPlan(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks/classes/${classId}/mock-from-plan`, payload));
}
export function getFollowBankScope(classId) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/bank-scope`));
}
export function getFollowBankTypeCounts(classId, params) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/bank-type-counts`, { params }));
}
export function drawFollowBankQuestions(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks/classes/${classId}/bank-draw`, payload));
}
export function getFollowTask(id) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/${id}`));
}
export function listFollowTaskItems(id) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/${id}/items`));
}
export function getFollowTaskItemAnswers(taskId, itemId) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/${taskId}/items/${itemId}/answers`));
}
export function getFollowTaskStats(id) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/${id}/stats`));
}
export async function exportFollowTaskAnswers(id) {
    const response = await apiClient.get(`${BASE}/follow-tasks/${id}/export-answers`, {
        responseType: 'blob',
    });
    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response) || `作业答卷_${id}.xlsx`;
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}
export function getHomeworkMetrics(classId) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/homework-metrics`));
}
export function getMockMetrics(classId) {
    return unwrap(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/mock-metrics`));
}
export function syncFollowTaskRoster(id) {
    return unwrap(apiClient.post(`${BASE}/follow-tasks/${id}/sync-roster`));
}
export function listFollowEvidences(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/evidences`, { params }));
}
export function getFollowEvidenceCoverage(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/evidence-coverage`));
}
export async function uploadFollowEvidence(classId, file, fields) {
    const form = new FormData();
    form.append('file', file);
    if (fields.scheduleId != null)
        form.append('scheduleId', String(fields.scheduleId));
    if (fields.bizDate)
        form.append('bizDate', fields.bizDate);
    if (fields.teachFormOverride)
        form.append('teachFormOverride', fields.teachFormOverride);
    if (fields.classHoursOverride)
        form.append('classHoursOverride', fields.classHoursOverride);
    if (fields.caption)
        form.append('caption', fields.caption);
    if (fields.mediaType)
        form.append('mediaType', fields.mediaType);
    if (fields.durationSeconds != null)
        form.append('durationSeconds', String(fields.durationSeconds));
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/follow/evidences/upload`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 180000,
    }));
}
export function reviewFollowEvidence(id, payload) {
    return unwrap(apiClient.post(`${BASE}/follow-evidences/${id}/review`, payload));
}
export function deleteFollowEvidence(id) {
    return unwrap(apiClient.delete(`${BASE}/follow-evidences/${id}`));
}
export function getFollowPhotoPackDefaults(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/photo-pack/defaults`));
}
export function generateFollowPhotoPack(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/follow/photo-pack/generate`, payload ?? {}, { timeout: 180000 }));
}
export function getLatestFollowPhotoPack(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/photo-pack/latest`));
}
export function listFollowOpLogs(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/op-logs`, { params }));
}
export function listFollowWorkLogs(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/logs`, { params }));
}
export function createFollowWorkLog(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/follow/logs`, payload));
}
export function updateFollowWorkLog(id, payload) {
    return unwrap(apiClient.put(`${BASE}/follow-logs/${id}`, payload));
}
export function deleteFollowWorkLog(id) {
    return unwrap(apiClient.delete(`${BASE}/follow-logs/${id}`));
}
export function draftFollowWorkLogFromOps(classId, bizDate) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/follow/logs/draft-from-ops`, { bizDate }));
}
export function getFollowClassHours(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/class-hours`));
}
export function getFollowTeacherDays(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/teacher-days`));
}
export function saveFollowTeacherPay(classId, items) {
    return unwrap(apiClient.put(`${BASE}/info-classes/${classId}/follow/teacher-pay`, { items }));
}
export async function exportFollowTeacherDays(classId) {
    const response = await apiClient.get(`${BASE}/info-classes/${classId}/follow/teacher-days/export`, {
        responseType: 'blob',
    });
    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response) || `教师课酬_${classId}.xlsx`;
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}
export function listFollowSupervisions(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/supervisions`, { params }));
}
export function getFollowSupervision(id) {
    return unwrap(apiClient.get(`${BASE}/follow-supervisions/${id}`));
}
export function createFollowSupervision(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/follow/supervisions`, payload));
}
export function patchFollowSupervisionItem(itemId, payload) {
    return unwrap(apiClient.patch(`${BASE}/follow-supervision-items/${itemId}`, payload));
}
export function submitFollowSupervision(id, payload) {
    return unwrap(apiClient.post(`${BASE}/follow-supervisions/${id}/submit`, payload));
}
export function closeFollowSupervision(id) {
    return unwrap(apiClient.post(`${BASE}/follow-supervisions/${id}/close`));
}
export function listFollowIssues(classId, params) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/issues`, { params }));
}
export function createFollowIssue(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/follow/issues`, payload));
}
export function transitionFollowIssue(id, payload) {
    return unwrap(apiClient.post(`${BASE}/follow-issues/${id}/transition`, payload));
}
export function listFollowSurveys(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/surveys`));
}
export function createFollowSurvey(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/follow/surveys`, payload));
}
export function getFollowQualityMetrics(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/follow/quality-metrics`));
}
export function pageSubsidies(params) {
    return unwrap(apiClient.get(`${BASE}/subsidies`, { params }));
}
export function getSubsidy(id) {
    return unwrap(apiClient.get(`${BASE}/subsidies/${id}`));
}
/** 本班进行中补贴单；无单时返回 null */
export function getClassSubsidy(classId) {
    return unwrap(apiClient.get(`${BASE}/info-classes/${classId}/subsidy`));
}
/** 按班幂等：有进行中单则返回，否则创建草稿 */
export function ensureClassSubsidy(classId, payload) {
    return unwrap(apiClient.post(`${BASE}/info-classes/${classId}/subsidy/ensure`, payload ?? { trainMode: 'skill_upgrade' }));
}
/** 跳转申报补贴页时写入焦点（班级 / 单号）；页面挂载或已打开时消费并清除 */
export const SUBSIDY_DECLARE_FOCUS_KEY = 'train_subsidy_declare_focus';
/** 同页已挂载时通知消费焦点（sessionStorage 同标签页不触发 storage 事件） */
export const SUBSIDY_DECLARE_FOCUS_EVENT = 'train_subsidy_declare_focus';
export function stashSubsidyDeclareFocus(focus) {
    try {
        sessionStorage.setItem(SUBSIDY_DECLARE_FOCUS_KEY, JSON.stringify(focus));
    }
    catch {
        /* ignore quota */
    }
    try {
        window.dispatchEvent(new CustomEvent(SUBSIDY_DECLARE_FOCUS_EVENT, { detail: focus }));
    }
    catch {
        /* ignore non-browser */
    }
}
export function consumeSubsidyDeclareFocus() {
    try {
        const raw = sessionStorage.getItem(SUBSIDY_DECLARE_FOCUS_KEY);
        if (!raw)
            return null;
        sessionStorage.removeItem(SUBSIDY_DECLARE_FOCUS_KEY);
        return JSON.parse(raw);
    }
    catch {
        return null;
    }
}
export function createSubsidy(payload) {
    return unwrap(apiClient.post(`${BASE}/subsidies`, payload));
}
export function updateSubsidy(id, payload) {
    return unwrap(apiClient.put(`${BASE}/subsidies/${id}`, payload));
}
export function deleteSubsidy(id) {
    return unwrap(apiClient.delete(`${BASE}/subsidies/${id}`));
}
export function transitionSubsidy(id, toPhase, remark) {
    return unwrap(apiClient.post(`${BASE}/subsidies/${id}/transition`, { toPhase, remark }));
}
export function listSubsidyEvents(id) {
    return unwrap(apiClient.get(`${BASE}/subsidies/${id}/events`));
}
export function listSubsidySlotFiles(id) {
    return unwrap(apiClient.get(`${BASE}/subsidies/${id}/slot-files`));
}
export function deleteSubsidySlotFile(id, slotFileId) {
    return unwrap(apiClient.delete(`${BASE}/subsidies/${id}/slot-files/${slotFileId}`));
}
export function getSubsidyClassMaterials(id) {
    return unwrap(apiClient.get(`${BASE}/subsidies/${id}/class-materials`));
}
export function syncSubsidyClassMaterials(id) {
    return unwrap(apiClient.post(`${BASE}/subsidies/${id}/class-materials/sync`));
}
export function generateSubsidyClaimForms(id) {
    return unwrap(apiClient.post(`${BASE}/subsidies/${id}/class-materials/generate-claim-forms`));
}
export function listSubsidyMaterialInstances(subsidyId) {
    return unwrap(apiClient.get(`${BASE}/subsidies/${subsidyId}/material-instances`));
}
export async function replaceSubsidyMaterialFile(instanceId, file) {
    const form = new FormData();
    form.append('file', file);
    return unwrap(apiClient.post(`${BASE}/material-instances/${instanceId}/replace-file`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000,
    }));
}
/** 在线编辑：按单元格地址写回（后端 POI 保留边框格式） */
export function patchSubsidyMaterialCells(instanceId, cells) {
    return unwrap(apiClient.post(`${BASE}/material-instances/${instanceId}/patch-cells`, { cells }, { timeout: 120000 }));
}
/** Word 轻量编辑：可编辑段落摘要（标题 / 元信息 / 图注） */
export function getSubsidyMaterialDocxMeta(instanceId) {
    return unwrap(apiClient.get(`${BASE}/material-instances/${instanceId}/docx-meta`));
}
/** Word 轻量编辑保存：按段落索引写回整段文本（后端 POI 保留图片与样式） */
export function patchSubsidyMaterialDocx(instanceId, paragraphs) {
    return unwrap(apiClient.post(`${BASE}/material-instances/${instanceId}/patch-docx`, { paragraphs }, { timeout: 120000 }));
}
export function confirmSubsidyMaterialInstance(instanceId) {
    return unwrap(apiClient.post(`${BASE}/material-instances/${instanceId}/confirm`));
}
export function deleteSubsidyMaterialInstance(instanceId) {
    return unwrap(apiClient.post(`${BASE}/material-instances/${instanceId}/delete`));
}
/** OnlyOffice 编辑器配置（含 JWT token） */
export function getSubsidyMaterialOfficeConfig(instanceId) {
    return unwrap(apiClient.get(`${BASE}/material-instances/${instanceId}/office-config`));
}
export function exportSubsidyMaterials(subsidyId, instanceIds) {
    return unwrap(apiClient.post(`${BASE}/subsidies/${subsidyId}/materials/export`, { instanceIds }, { timeout: 180000 }));
}
export function listSubsidyMaterialTemplates(params) {
    return unwrap(apiClient.get(`${BASE}/subsidy-material-templates`, { params }));
}
export async function uploadSubsidyMaterialTemplate(params) {
    const form = new FormData();
    form.append('file', params.file);
    if (params.title)
        form.append('title', params.title);
    if (params.materialType)
        form.append('materialType', params.materialType);
    if (params.slotCode)
        form.append('slotCode', params.slotCode);
    form.append('scope', params.scope || 'tenant');
    if (params.classId != null)
        form.append('classId', String(params.classId));
    if (params.regionCode)
        form.append('regionCode', params.regionCode);
    if (params.trainMode)
        form.append('trainMode', params.trainMode);
    if (params.policyYear != null)
        form.append('policyYear', String(params.policyYear));
    if (params.remark)
        form.append('remark', params.remark);
    return unwrap(apiClient.post(`${BASE}/subsidy-material-templates`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000,
    }));
}
export function updateSubsidyMaterialTemplate(id, body) {
    return unwrap(apiClient.put(`${BASE}/subsidy-material-templates/${id}`, body));
}
export function deleteSubsidyMaterialTemplate(id) {
    return unwrap(apiClient.delete(`${BASE}/subsidy-material-templates/${id}`));
}
export function reparseSubsidyMaterialTemplate(id) {
    return unwrap(apiClient.post(`${BASE}/subsidy-material-templates/${id}/reparse`));
}
/** 选模板 → 自动填本班字段 → 生成材料 */
export function fillSubsidyFromTemplate(subsidyId, body) {
    return unwrap(apiClient.post(`${BASE}/subsidies/${subsidyId}/materials/fill-from-template`, body, {
        timeout: 120000,
    }));
}
/** 按材料清单逐项一键填数 */
export function fillAllSubsidyMaterials(subsidyId) {
    return unwrap(apiClient.post(`${BASE}/subsidies/${subsidyId}/materials/fill-all`, undefined, {
        timeout: 300000,
    }));
}
/** 对已有材料实例选模板填数 */
export function fillSubsidyMaterialInstance(instanceId, body) {
    const payload = typeof body === 'object' && body != null
        ? body
        : { templateId: body };
    return unwrap(apiClient.post(`${BASE}/material-instances/${instanceId}/fill`, payload, {
        timeout: 120000,
    }));
}
export async function uploadSubsidySlotFile(id, slotCode, file) {
    const form = new FormData();
    form.append('file', file);
    form.append('slotCode', slotCode);
    return unwrap(apiClient.post(`${BASE}/subsidies/${id}/slot-files`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000,
    }));
}
export function generateSubsidyMaterials(id) {
    return unwrap(apiClient.post(`${BASE}/subsidies/${id}/materials/generate`, undefined, { timeout: 180000 }));
}
export function listSubsidyPackProfiles(params) {
    return unwrap(apiClient.get(`${BASE}/subsidy-pack-profiles`, { params }));
}
export function getSubsidyPackProfile(id) {
    return unwrap(apiClient.get(`${BASE}/subsidy-pack-profiles/${id}`));
}
// ==================== 信息收集 (sign-up collection) ====================
export function pageSignUpCollection(params) {
    return unwrap(apiClient.get(`${BASE}/sign-ups/collection`, { params }));
}
export function pageSignUps(params) {
    return unwrap(apiClient.get(`${BASE}/sign-ups`, { params }));
}
export function createSignUp(payload) {
    return unwrap(apiClient.post(`${BASE}/sign-ups`, payload));
}
export function updateSignUp(id, payload) {
    return unwrap(apiClient.put(`${BASE}/sign-ups/${id}`, payload));
}
export function deleteSignUp(id) {
    return unwrap(apiClient.delete(`${BASE}/sign-ups/${id}`));
}
export function getSignUpQrcode(id) {
    return unwrap(apiClient.get(`${BASE}/sign-ups/${id}/qrcode`));
}
export function generateSignUpQrcode(id) {
    return unwrap(apiClient.get(`${BASE}/sign-ups/${id}/qrcode`, { params: { generate: true } }));
}
export function pageSignUpPeople(params) {
    return unwrap(apiClient.get(`${BASE}/sign-up-people`, { params }));
}
export function batchUpdateSignUpPeople(payload) {
    return unwrap(apiClient.put(`${BASE}/sign-up-people/batch`, payload));
}
export async function exportSignUpPeople(signUpId, ids) {
    const response = await apiClient.get(`${BASE}/sign-up-people/export`, {
        params: { signUpId, ids: ids?.length ? ids.join(',') : undefined },
        responseType: 'blob',
    });
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = parseContentDispositionFilename(response) || `报名人员汇总表-${signUpId}.xlsx`;
    a.click();
    window.URL.revokeObjectURL(url);
}
export function getSignUpPerson(id) {
    return unwrap(apiClient.get(`${BASE}/sign-up-people/${id}`));
}
export function updateSignUpPerson(id, payload) {
    return unwrap(apiClient.put(`${BASE}/sign-up-people/${id}`, payload));
}
export function importSignUpPeople(signUpId, file) {
    const formData = new FormData();
    formData.append('signUpId', String(signUpId));
    formData.append('file', file);
    return unwrap(apiClient.post(`${BASE}/sign-up-people/import`, formData));
}
export function importClassPrepRoster(signUpId, file) {
    const formData = new FormData();
    formData.append('signUpId', String(signUpId));
    formData.append('file', file);
    return unwrap(apiClient.post(`${BASE}/sign-up-people/import-class-roster`, formData, { timeout: 120000 }));
}
export function downloadSignUpPeopleImportTemplate() {
    return fetchFileBlob(`${BASE}/sign-up-people/import/template`);
}
export function importSignUpStudents(signUpId, ids) {
    return unwrap(apiClient.post(`${BASE}/sign-up-people/import/students`, { signUpId, ids }));
}
/** 按身份证号从学员库导入报名/资料收集人员（一行一个） */
export function importSignUpStudentsByIdCard(signUpId, idCards) {
    return unwrap(apiClient.post(`${BASE}/sign-up-people/import/students-by-idcard`, { signUpId, idCards }));
}
/** 按身份证从历史信息收集/扫码填报补全当前计划空字段 */
export function syncSignUpPeopleHistory(signUpId, ids) {
    return unwrap(apiClient.post(`${BASE}/sign-up-people/sync-history`, {
        signUpId,
        ids: ids?.length ? ids : undefined,
    }, { timeout: 120000 }));
}
export function pageSignUpImportStudents(params) {
    return unwrap(apiClient.get(`${BASE}/sign-up-people/import/students`, { params }));
}
export function startDefaultTemplateExport(payload) {
    return unwrap(apiClient.post(`${BASE}/sign-up-people/export/default-template`, payload));
}
export function checkDefaultTemplateExport(payload) {
    return unwrap(apiClient.post(`${BASE}/sign-up-people/export/default-template/check`, payload));
}
export function cancelDefaultTemplateExport(payload) {
    return unwrap(apiClient.post(`${BASE}/sign-up-people/export/default-template/cancel`, payload));
}
export function pageSignUpOrcs(params) {
    return unwrap(apiClient.get(`${BASE}/sign-up-orcs`, { params }));
}
export function savePlatformUser(payload) {
    return unwrap(apiClient.post(`${BASE}/platform-users`, payload));
}
// ==================== Train Research Org (培训调研企业) ====================
export function pageResearchOrgs(params) {
    return unwrap(apiClient.get(`${BASE}/research-orgs`, { params }));
}
export function getResearchOrg(id) {
    return unwrap(apiClient.get(`${BASE}/research-orgs/${id}`));
}
export function createResearchOrg(payload) {
    return unwrap(apiClient.post(`${BASE}/research-orgs`, payload));
}
export function updateResearchOrg(id, payload) {
    return unwrap(apiClient.put(`${BASE}/research-orgs/${id}`, payload));
}
export function deleteResearchOrg(id) {
    return unwrap(apiClient.delete(`${BASE}/research-orgs/${id}`));
}
/** 手动输入企业名：查找或创建系统企业（company），返回后写入关联下拉 */
export function ensureSystemCompany(name) {
    return unwrap(apiClient.post(`${BASE}/research-orgs/system-companies`, { name }));
}
// ==================== System Company (培训企业管理) ====================
export function listSystemCompanies(keyword) {
    return unwrap(apiClient.get(`${BASE}/system-companies`, { params: keyword ? { keyword } : undefined }));
}
export function createSystemCompany(payload) {
    return unwrap(apiClient.post(`${BASE}/system-companies`, payload));
}
export function updateSystemCompany(id, payload) {
    return unwrap(apiClient.put(`${BASE}/system-companies/${id}`, payload));
}
export function deleteSystemCompany(id) {
    return unwrap(apiClient.delete(`${BASE}/system-companies/${id}`));
}
// ==================== Research Collection (企业调研人员采集) ====================
export function getResearchCollection(researchOrgId) {
    return unwrap(apiClient.get(`${BASE}/research-orgs/${researchOrgId}/collection`));
}
export function startResearchCollection(researchOrgId) {
    return unwrap(apiClient.post(`${BASE}/research-orgs/${researchOrgId}/collection/start`));
}
export function pageResearchPeople(researchOrgId, params) {
    return unwrap(apiClient.get(`${BASE}/research-orgs/${researchOrgId}/collection/people`, { params }));
}
export function createResearchPerson(researchOrgId, payload) {
    return unwrap(apiClient.post(`${BASE}/research-orgs/${researchOrgId}/collection/people`, payload));
}
export function updateResearchPerson(researchOrgId, peopleId, payload) {
    return unwrap(apiClient.put(`${BASE}/research-orgs/${researchOrgId}/collection/people/${peopleId}`, payload));
}
export function deleteResearchPerson(researchOrgId, peopleId) {
    return unwrap(apiClient.delete(`${BASE}/research-orgs/${researchOrgId}/collection/people/${peopleId}`));
}
export async function downloadResearchCollectionTemplate(researchOrgId, filename) {
    const blob = await fetchFileBlob(`${BASE}/research-orgs/${researchOrgId}/collection/export-template`);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename.endsWith('.xlsx') ? filename : `${filename}-认定采集精简版.xlsx`;
    a.click();
    window.URL.revokeObjectURL(url);
}
export function createThreeYearPlanFromResearch(researchOrgId, payload) {
    return unwrap(apiClient.post(`${BASE}/research-orgs/${researchOrgId}/collection/create-three-year-plan`, payload ?? {}, {
        timeout: 120000,
    }));
}
/** 证书查证：将选中人员的姓名+身份证上传到外部查证接口，回填证书信息 */
export function verifyResearchPeopleCertificate(researchOrgId, peopleIds) {
    return unwrap(apiClient.post(`${BASE}/research-orgs/${researchOrgId}/collection/people/verify-cert`, { peopleIds }, { timeout: 120000 }));
}
/** 方案规划人员认定：按姓名+身份证查证（不落库，前端写回 Excel） */
export function verifyPersonnelCertLookup(people) {
    return unwrap(apiClient.post(`${BASE}/personnel-cert/verify-lookup`, { people }, { timeout: 120000 }));
}
// ==================== Three Year Plan (三年规划) ====================
export function pageThreeYearPlans(params) {
    return unwrap(apiClient.get(`${BASE}/three-year-plans`, { params }));
}
export function getThreeYearPlan(id) {
    return unwrap(apiClient.get(`${BASE}/three-year-plans/${id}`));
}
export function getThreeYearPlanDetailBundle(planId, params = {}) {
    return unwrap(apiClient.get(`${BASE}/three-year-plans/${planId}/detail-bundle`, { params, timeout: 120000 }));
}
export function createThreeYearPlan(payload) {
    const formData = new FormData();
    formData.append('title', payload.title);
    formData.append('targetOrgId', String(payload.targetOrgId));
    if (payload.researchOrgId) {
        formData.append('researchOrgId', String(payload.researchOrgId));
    }
    formData.append('file', payload.file);
    return unwrap(apiClient.post(`${BASE}/three-year-plans`, formData, { timeout: 120000 }));
}
export function refreshThreeYearPlanAnalysis(id) {
    return unwrap(apiClient.get(`${BASE}/three-year-plans/${id}/analysis-status`));
}
export function syncThreeYearPlanFromRencai(id) {
    return unwrap(apiClient.post(`${BASE}/three-year-plans/${id}/sync-from-rencai`));
}
export function prepareThreeYearPlanReview(id) {
    return unwrap(apiClient.post(`${BASE}/three-year-plans/${id}/prepare-review`));
}
export function updateThreeYearPlanItem(planId, itemId, payload) {
    return unwrap(apiClient.put(`${BASE}/three-year-plans/${planId}/items/${itemId}`, payload));
}
export function submitThreeYearPlan(id, payload) {
    const formData = new FormData();
    formData.append('personnelFile', payload.personnelFile);
    formData.append('schemeFile', payload.schemeFile);
    return unwrap(apiClient.post(`${BASE}/three-year-plans/${id}/submit`, formData, { timeout: 120000 }));
}
export function companyConfirmThreeYearPlan(id) {
    return unwrap(apiClient.post(`${BASE}/three-year-plans/${id}/company-confirm`));
}
export function uploadThreeYearPlanEnterpriseReturn(id, payload) {
    const formData = new FormData();
    if (payload.rosterFile)
        formData.append('rosterFile', payload.rosterFile);
    if (payload.shiftFile)
        formData.append('shiftFile', payload.shiftFile);
    return unwrap(apiClient.post(`${BASE}/three-year-plans/${id}/enterprise-return`, formData, { timeout: 120000 }));
}
export function companyRejectThreeYearPlan(id, reason) {
    return unwrap(apiClient.post(`${BASE}/three-year-plans/${id}/company-reject`, null, { params: { reason } }));
}
export function pageThreeYearPlanItems(planId, params) {
    return unwrap(apiClient.get(`${BASE}/three-year-plans/${planId}/items`, { params }));
}
export function listThreeYearPlanMatrix(planId) {
    return unwrap(apiClient.get(`${BASE}/three-year-plans/${planId}/matrix`));
}
export function listThreeYearPlanReviewDistribution(planId) {
    return unwrap(apiClient.get(`${BASE}/three-year-plans/${planId}/review-distribution`));
}
export function deleteThreeYearPlan(id) {
    return unwrap(apiClient.delete(`${BASE}/three-year-plans/${id}`));
}
export async function downloadThreeYearPlanFile(planId, kind) {
    const path = threeYearPlanFileDownloadPath(planId, kind);
    const blob = await fetchFileBlob(path);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = threeYearPlanFileDefaultName(planId, kind);
    a.click();
    window.URL.revokeObjectURL(url);
}
export function threeYearPlanFileDownloadPath(planId, kind) {
    if (kind === 'plan') {
        return `${BASE}/three-year-plans/${planId}/download-plan`;
    }
    if (kind === 'reviewed') {
        return `${BASE}/three-year-plans/${planId}/download-reviewed-plan`;
    }
    if (kind === 'submit-personnel') {
        return `${BASE}/three-year-plans/${planId}/download-submit-personnel`;
    }
    if (kind === 'submit-scheme') {
        return `${BASE}/three-year-plans/${planId}/download-submit-scheme`;
    }
    if (kind === 'enterprise-roster') {
        return `${BASE}/three-year-plans/${planId}/download-enterprise-roster`;
    }
    if (kind === 'enterprise-shift') {
        return `${BASE}/three-year-plans/${planId}/download-enterprise-shift`;
    }
    return `${BASE}/three-year-plans/${planId}/download-result`;
}
export function threeYearPlanMaterialPreviewUrl(planId, kind) {
    return threeYearPlanFileDownloadPath(planId, kind);
}
function threeYearPlanFileDefaultName(planId, kind) {
    if (kind === 'plan')
        return `原始三年规划-${planId}.xlsx`;
    if (kind === 'reviewed')
        return `已审核三年规划-${planId}.xlsx`;
    if (kind === 'submit-personnel')
        return `企业审核人员名单-${planId}.xlsx`;
    if (kind === 'submit-scheme')
        return `方案文档-${planId}`;
    if (kind === 'enterprise-roster')
        return `企业筛选培训名单-${planId}.xlsx`;
    if (kind === 'enterprise-shift')
        return `企业排班表-${planId}.xlsx`;
    return `原始认定结果-${planId}.zip`;
}
// ==================== Division (分班) ====================
export function pageDivisionPlans(params) {
    return unwrap(apiClient.get(`${BASE}/division/plans`, { params }));
}
export function getDivisionReady(planId) {
    return unwrap(apiClient.get(`${BASE}/division/plans/${planId}/ready`));
}
export function ensureDivisionTrainInfo(planId, payload = {}) {
    return unwrap(apiClient.post(`${BASE}/division/plans/${planId}/ensure-train-info`, payload));
}
export function parseDivisionRoster(payload) {
    return unwrap(apiClient.post(`${BASE}/division/roster/parse`, payload, { timeout: 120000 }));
}
export function pageDivisionRoster(params) {
    return unwrap(apiClient.get(`${BASE}/division/roster`, { params }));
}
export function submitDivisionOrgSplit(payload) {
    return unwrap(apiClient.post(`${BASE}/division/org-split`, payload));
}
export function autoPackDivision(payload) {
    return unwrap(apiClient.post(`${BASE}/division/auto-pack`, payload, { timeout: 180000 }));
}
export function manualPackDivision(payload) {
    return unwrap(apiClient.post(`${BASE}/division/manual-pack`, payload));
}
export function addPersonToDivisionPackClass(taskId, tempId, payload) {
    return unwrap(apiClient.post(`${BASE}/division/pack-tasks/${taskId}/classes/${encodeURIComponent(tempId)}/add-person`, payload));
}
export async function importMembersToDivisionPackClass(taskId, tempId, file) {
    const form = new FormData();
    form.append('file', file);
    return unwrap(apiClient.post(`${BASE}/division/pack-tasks/${taskId}/classes/${encodeURIComponent(tempId)}/import-members`, form, { timeout: 120000, headers: { 'Content-Type': 'multipart/form-data' } }));
}
export function downloadDivisionMemberImportTemplate() {
    return apiClient.get(`${BASE}/division/member-import-template`, {
        responseType: 'blob',
    });
}
export function pageDivisionPackTasks(params) {
    return unwrap(apiClient.get(`${BASE}/division/pack-tasks`, { params }));
}
export function getDivisionPackTask(taskId) {
    return unwrap(apiClient.get(`${BASE}/division/pack-tasks/${taskId}`));
}
export function patchDivisionPackTask(taskId, payload) {
    return unwrap(apiClient.post(`${BASE}/division/pack-tasks/${taskId}/patch`, payload));
}
export function submitDivisionPackTask(taskId) {
    return unwrap(apiClient.post(`${BASE}/division/pack-tasks/${taskId}/submit`));
}
export function companyConfirmDivisionPackTask(taskId) {
    return unwrap(apiClient.post(`${BASE}/division/pack-tasks/${taskId}/company-confirm`, undefined, { timeout: 180000 }));
}
export function syncDivisionPackPlatformUsers(taskId) {
    return unwrap(apiClient.post(`${BASE}/division/pack-tasks/${taskId}/sync-platform-users`, undefined, { timeout: 180000 }));
}
export function companyRejectDivisionPackTask(taskId, reason) {
    return unwrap(apiClient.post(`${BASE}/division/pack-tasks/${taskId}/company-reject`, null, {
        params: { reason },
    }));
}
export function getDivisionTrainInfoSummary(trainInfoId) {
    return unwrap(apiClient.get(`${BASE}/division/train-info/${trainInfoId}/summary`));
}
// ==================== Shift Assign (排课分班 L2) ====================
export function previewShiftAssignAdapter(payload) {
    return unwrap(apiClient.post(`${BASE}/shift-assign/adapters/preview`, payload, { timeout: 120000 }));
}
export function precheckShiftAssign(payload) {
    return unwrap(apiClient.post(`${BASE}/shift-assign/precheck`, payload, { timeout: 180000 }));
}
export function generateShiftAssign(payload) {
    return unwrap(apiClient.post(`${BASE}/shift-assign/generate`, payload, { timeout: 180000 }));
}
export function getShiftAssignTask(taskId) {
    return unwrap(apiClient.get(`${BASE}/shift-assign/tasks/${taskId}`));
}
export function patchShiftAssignTask(taskId, payload) {
    return unwrap(apiClient.post(`${BASE}/shift-assign/tasks/${taskId}/patch`, payload));
}
export function bindShiftAssignClass(taskId, payload) {
    return unwrap(apiClient.post(`${BASE}/shift-assign/tasks/${taskId}/bind-class`, payload));
}
export function materializeShiftAssign(taskId, payload) {
    return unwrap(apiClient.post(`${BASE}/shift-assign/tasks/${taskId}/materialize`, payload, { timeout: 180000 }));
}
export function generateAndMaterializeShiftAssign(payload) {
    return unwrap(apiClient.post(`${BASE}/shift-assign/generate-and-materialize`, payload, { timeout: 180000 }));
}
export function pageAttendances(params) {
    return unwrap(apiClient.get(`${BASE}/attendances`, { params }));
}
export function createAttendance(payload) {
    return unwrap(apiClient.post(`${BASE}/attendances`, payload));
}
export function updateAttendance(id, payload) {
    return unwrap(apiClient.put(`${BASE}/attendances/${id}`, payload));
}
export function signInAttendance(payload) {
    return unwrap(apiClient.post(`${BASE}/attendances/sign-in`, payload));
}
/** 导入人社/培训平台考勤信息列表 Excel（按身份证匹配本班学员，同日多课次合并） */
export async function importClassAttendances(classId, file) {
    const form = new FormData();
    form.append('file', file);
    return unwrap(apiClient.post(`${BASE}/classes/${classId}/attendances/import`, form, {
        timeout: 120000,
        headers: { 'Content-Type': 'multipart/form-data' },
    }));
}
export function listExamScoreImportClasses(params) {
    return unwrap(apiClient.get(`${BASE}/exam-score-imports/classes`, {
        params: {
            ...params,
            // 雪花企业 ID 必须以字符串传，避免 Number 精度丢失
            companyId: params?.companyId != null ? String(params.companyId) : undefined,
            trainInfoId: params?.trainInfoId != null ? String(params.trainInfoId) : undefined,
        },
    }));
}
export function pageExamScoreImportClassPeople(classId, params) {
    return unwrap(apiClient.get(`${BASE}/exam-score-imports/classes/${classId}/people`, { params }));
}
export function downloadExamScoreImportTemplate() {
    return fetchFileBlob(`${BASE}/exam-score-imports/template`);
}
export function previewExamScoreImport(classIds, files) {
    const form = new FormData();
    classIds.forEach((id) => form.append('classIds', String(id)));
    const list = Array.isArray(files) ? files : [files];
    list.forEach((file) => form.append('file', file));
    return unwrap(apiClient.post(`${BASE}/exam-score-imports/preview`, form, { timeout: 180000 }));
}
export function commitExamScoreImport(previewId, peopleIds) {
    return unwrap(apiClient.post(`${BASE}/exam-score-imports/commit`, { previewId, peopleIds }));
}
export function pageExamScoreImportBatches(params) {
    return unwrap(apiClient.get(`${BASE}/exam-score-imports/batches`, { params }));
}
export function pageExamScoreImportBatchItems(batchId, params) {
    return unwrap(apiClient.get(`${BASE}/exam-score-imports/batches/${batchId}/items`, { params }));
}
export function downloadExamScoreImportFailures(batchId) {
    return fetchFileBlob(`${BASE}/exam-score-imports/batches/${batchId}/failures.xlsx`);
}

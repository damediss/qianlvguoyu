import { apiClient, unwrap } from './client';
import { fetchFileBlob } from './files';
import { parseContentDispositionFilename } from '../utils/download';
import type { PageResponse } from '../types/api';
import type {
  TrainNeed, TrainNeedQuery, TrainNeedCreateReq, TrainNeedUpdateReq,
  TrainSolution, TrainSolutionQuery, TrainSolutionCreateReq, TrainSolutionUpdateReq,
  TrainRequest, TrainRequestQuery, TrainRequestCreateReq, TrainRequestUpdateReq,
  TrainDeclare, TrainDeclareQuery, TrainDeclareCreateReq, TrainDeclareUpdateReq,
  TrainPeople, TrainPeopleQuery, TrainPeopleCreateReq, TrainPeopleUpdateReq, SignupAuditReq,
  TrainClass, TrainClassQuery, TrainClassCreateReq, TrainClassUpdateReq, AssignClassReq,
  TrainCourse, TrainCourseQuery, TrainCourseCreateReq, TrainCourseUpdateReq,
  CourseKejian, CourseKejianQuery, CourseKejianCreateReq, CourseKejianUpdateReq,
  TrainResource, TrainResourceQuery, TrainResourceCreateReq, TrainResourceUpdateReq,
  TrainTask, TrainTaskQuery, TrainTaskCreateReq, TrainTaskUpdateReq,
  TrainTaskInfo, TrainTaskInfoQuery, TrainTaskInfoCreateReq, TrainTaskInfoUpdateReq,
  TrainExam, TrainExamQuery, TrainExamCreateReq, TrainExamUpdateReq,
  ExamResult, ExamResultQuery, ExamResultCreateReq, ExamResultUpdateReq,
  TrainProtocol, TrainProtocolQuery, TrainProtocolCreateReq, TrainProtocolUpdateReq,
  TrainProject, TrainProjectQuery, TrainProjectCreateReq, TrainProjectUpdateReq,
  TrainProjectTable, TrainProjectTableQuery, TrainProjectTableCreateReq, TrainProjectTableUpdateReq,
  TrainDashboard,
  DashboardMilestoneItem,
  DashboardStageDistribution,
  TrainTalentNeed, TrainTalentNeedQuery, TrainTalentNeedCreateReq, TrainTalentNeedUpdateReq,
  PlatformUser, PlatformUserQuery, PlatformUserCreateReq, PlatformUserUpdateReq,
  TrainTeacherBank, TrainTeacherBankQuery, TrainTeacherBankCreateReq, TrainTeacherBankUpdateReq,
  TrainUserInfo, TrainUserInfoQuery, TrainUserInfoCreateReq, TrainUserInfoUpdateReq,
  TrainBudget, TrainBudgetQuery, TrainBudgetCreateReq, TrainBudgetUpdateReq,
  TrainFileList, TrainFileListQuery, TrainFileListCreateReq, TrainFileListUpdateReq,
  TrainFileDetail, TrainFileDetailQuery, TrainFileDetailCreateReq, TrainFileDetailUpdateReq,
  TrainInfoList, TrainInfoListQuery, TrainInfoListCreateReq, TrainInfoListUpdateReq, TrainInfoListEditReq, TrainInfoListPageResponse, TrainInfoListItem,
  TrainInfoClass, TrainInfoClassQuery, TrainInfoClassCreateReq, TrainInfoClassUpdateReq,
  TrainTaskInfoDetail, TrainTaskInfoDetailQuery, TrainTaskInfoDetailCreateReq, TrainTaskInfoDetailUpdateReq,
  TrainTaskLog, TrainTaskLogQuery, TrainTaskLogCreateReq, TrainTaskLogUpdateReq,
  TrainUserLearnLog, TrainUserLearnLogQuery, TrainUserLearnLogCreateReq, TrainUserLearnLogUpdateReq,
  TrainUserCoursePlan, TrainUserCoursePlanQuery, TrainUserCoursePlanCreateReq, TrainUserCoursePlanUpdateReq,
  TrainUserCoursePaper, TrainUserCoursePaperQuery, TrainUserCoursePaperCreateReq, TrainUserCoursePaperUpdateReq,
  TrainUserPaper, TrainUserPaperQuery, TrainUserPaperCreateReq, TrainUserPaperUpdateReq,
  TrainUserPatent, TrainUserPatentQuery, TrainUserPatentCreateReq, TrainUserPatentUpdateReq,
  TrainUserExamLog, TrainUserExamLogQuery, TrainUserExamLogCreateReq, TrainUserExamLogUpdateReq,
  TrainUserAbility, TrainUserAbilityQuery, TrainUserAbilityCreateReq, TrainUserAbilityUpdateReq,
  DemandTrain, DemandPending, DemandTrainQuery, DemandConvertReq, DemandPublicData,
  DemandEmployee, DemandEmployeeQuery, DemandEmployeeCreateReq, DemandEmployeeUpdateReq,
  TrainSolutionList, TrainSolutionDetail, TrainSolutionBadge, TrainSolutionSaveReq,
  TrainStudent, SolutionByDemand,
  TrainInfoClassListItem, TrainInfoClassPageResponse, ClassStudent, ClassStudentPageResponse, ClassOther, ClassTask, SignUpPerson, TeacherBankItem,
  TrainClassRole, TrainClassRoleSummary,
  TrainSummary, TrainSummaryQuery, TrainSummaryCreateReq, TrainSummaryUpdateReq,
  TrainCertificate, TrainCertificateQuery, TrainCertificateCreateReq, TrainCertificateUpdateReq,
  TrainResearchOrg, TrainResearchOrgQuery, TrainResearchOrgCreateReq, TrainResearchOrgUpdateReq,
  TrainResearchCollection, TrainResearchPerson, TrainResearchPersonQuery,
  TrainResearchPersonCreateReq, TrainResearchPersonUpdateReq,
  CertVerifyBatchResultVO,
  PersonnelCertLookupPerson,
  PersonnelCertLookupResultVO,
  ThreeYearPlan, ThreeYearPlanQuery, ThreeYearPlanItem, ThreeYearPlanItemQuery,
  ThreeYearPlanMatrix, ThreeYearPlanAnalysisStatus, ThreeYearPlanDistribution, ThreeYearPlanDetailBundle,
  TrainSignUpSelf, TrainSignUpSelfQuery, TrainSignUpSelfCreateReq, TrainSignUpSelfUpdateReq,
  TrainSignUpPerson, TrainSignUpPeopleQuery, TrainSignUpOrcSelf, PlatformUserSaveReq,
  SignUpPeopleImportResult,
  SignUpDefaultTemplateExportStart, SignUpDefaultTemplateExportCheck,
  DivisionPlanItem, DivisionReadyVO, DivisionEnsureTrainInfoReq, DivisionEnsureTrainInfoResult,
  DivisionRosterParseReq, DivisionRosterParseResult, DivisionRosterItem,
  DivisionOrgSplitReq, DivisionTrainInfoSummary,
  DivisionAutoPackReq, DivisionAutoPackResult, DivisionManualPackReq, DivisionAddPersonReq,
  DivisionPackPatchReq, DivisionPackTaskResult,
  DivisionPackTaskStatus, PlatformUserSyncResult,
  ShiftAssignGenerateReq, ShiftAssignPreviewResult, ShiftAssignPrecheckResult,
  ShiftAssignTaskResult, ShiftAssignBindClassReq, ShiftAssignPatchReq,
  ShiftAssignMaterializeReq, ShiftAssignGenerateMaterializeReq, ShiftAssignMaterializeResult,
} from '../types/training';

const BASE = '/training';

// ==================== Needs (培训需求) ====================
export function pageNeeds(params: TrainNeedQuery) {
  return unwrap<PageResponse<TrainNeed>>(apiClient.get(`${BASE}/needs`, { params }));
}
export function getNeed(id: number) {
  return unwrap<TrainNeed>(apiClient.get(`${BASE}/needs/${id}`));
}
export function createNeed(payload: TrainNeedCreateReq) {
  return unwrap<TrainNeed>(apiClient.post(`${BASE}/needs`, payload));
}
export function updateNeed(id: number, payload: TrainNeedUpdateReq) {
  return unwrap<TrainNeed>(apiClient.put(`${BASE}/needs/${id}`, payload));
}
export function deleteNeed(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/needs/${id}`));
}

// ==================== Solutions (培训方案) ====================
export function pageSolutions(params: TrainSolutionQuery) {
  return unwrap<PageResponse<TrainSolution>>(apiClient.get(`${BASE}/solutions`, { params }));
}
export function getSolution(id: number) {
  return unwrap<TrainSolution>(apiClient.get(`${BASE}/solutions/${id}`));
}
export function createSolution(payload: TrainSolutionCreateReq) {
  return unwrap<TrainSolution>(apiClient.post(`${BASE}/solutions`, payload));
}
export function updateSolution(id: number, payload: TrainSolutionUpdateReq) {
  return unwrap<TrainSolution>(apiClient.put(`${BASE}/solutions/${id}`, payload));
}
export function deleteSolution(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/solutions/${id}`));
}

// ==================== Requests (培训请求) ====================
export function pageRequests(params: TrainRequestQuery) {
  return unwrap<PageResponse<TrainRequest>>(apiClient.get(`${BASE}/requests`, { params }));
}
export function getRequest(id: number) {
  return unwrap<TrainRequest>(apiClient.get(`${BASE}/requests/${id}`));
}
export function createRequest(payload: TrainRequestCreateReq) {
  return unwrap<TrainRequest>(apiClient.post(`${BASE}/requests`, payload));
}
export function updateRequest(id: number, payload: TrainRequestUpdateReq) {
  return unwrap<TrainRequest>(apiClient.put(`${BASE}/requests/${id}`, payload));
}
export function deleteRequest(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/requests/${id}`));
}

// ==================== Declares (培训申报) ====================
export function pageDeclares(params: TrainDeclareQuery) {
  return unwrap<PageResponse<TrainDeclare>>(apiClient.get(`${BASE}/declares`, { params }));
}
export function getDeclare(id: number) {
  return unwrap<TrainDeclare>(apiClient.get(`${BASE}/declares/${id}`));
}
export function createDeclare(payload: TrainDeclareCreateReq) {
  return unwrap<TrainDeclare>(apiClient.post(`${BASE}/declares`, payload));
}
export function updateDeclare(id: number, payload: TrainDeclareUpdateReq) {
  return unwrap<TrainDeclare>(apiClient.put(`${BASE}/declares/${id}`, payload));
}
export function deleteDeclare(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/declares/${id}`));
}

// ==================== People List (培训人员) ====================
export function pagePeople(params: TrainPeopleQuery) {
  return unwrap<PageResponse<TrainPeople>>(apiClient.get(`${BASE}/people-lists`, { params }));
}
export function getPeople(id: number) {
  return unwrap<TrainPeople>(apiClient.get(`${BASE}/people-lists/${id}`));
}
export function createPeople(payload: TrainPeopleCreateReq) {
  return unwrap<TrainPeople>(apiClient.post(`${BASE}/people-lists`, payload));
}
export function updatePeople(id: number, payload: TrainPeopleUpdateReq) {
  return unwrap<TrainPeople>(apiClient.put(`${BASE}/people-lists/${id}`, payload));
}
export function deletePeople(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/people-lists/${id}`));
}
export function batchAuditPeople(payload: SignupAuditReq) {
  return unwrap<void>(apiClient.put(`${BASE}/people-lists/batch-audit`, payload));
}

// ==================== Class List (班级管理) ====================
export function pageClasses(params: TrainClassQuery) {
  return unwrap<PageResponse<TrainClass>>(apiClient.get(`${BASE}/class-lists`, { params }));
}
export function getClass(id: number) {
  return unwrap<TrainClass>(apiClient.get(`${BASE}/class-lists/${id}`));
}
export function createClass(payload: TrainClassCreateReq) {
  return unwrap<TrainClass>(apiClient.post(`${BASE}/class-lists`, payload));
}
export function updateClass(id: number, payload: TrainClassUpdateReq) {
  return unwrap<TrainClass>(apiClient.put(`${BASE}/class-lists/${id}`, payload));
}
export function deleteClass(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/class-lists/${id}`));
}
export function batchAssignClass(payload: AssignClassReq) {
  return unwrap<void>(apiClient.post(`${BASE}/class-lists/batch-assign`, payload));
}

// ==================== Courses (课程/课时) ====================
export function pageCourses(params: TrainCourseQuery) {
  return unwrap<PageResponse<TrainCourse>>(apiClient.get(`${BASE}/courses`, { params }));
}
export function getCourse(id: number) {
  return unwrap<TrainCourse>(apiClient.get(`${BASE}/courses/${id}`));
}
export function createCourse(payload: TrainCourseCreateReq) {
  return unwrap<TrainCourse>(apiClient.post(`${BASE}/courses`, payload));
}
export function updateCourse(id: number, payload: TrainCourseUpdateReq) {
  return unwrap<TrainCourse>(apiClient.put(`${BASE}/courses/${id}`, payload));
}
export function deleteCourse(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/courses/${id}`));
}

// Kejians (课件)
export function pageKejians(params: CourseKejianQuery) {
  return unwrap<PageResponse<CourseKejian>>(apiClient.get(`${BASE}/course-kejians`, { params }));
}
export function getKejian(id: number) {
  return unwrap<CourseKejian>(apiClient.get(`${BASE}/course-kejians/${id}`));
}
export function createKejian(payload: CourseKejianCreateReq) {
  return unwrap<CourseKejian>(apiClient.post(`${BASE}/course-kejians`, payload));
}
export function updateKejian(id: number, payload: CourseKejianUpdateReq) {
  return unwrap<CourseKejian>(apiClient.put(`${BASE}/course-kejians/${id}`, payload));
}
export function deleteKejian(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/course-kejians/${id}`));
}

// ==================== Resources (资源管理) ====================
export function pageResources(params: TrainResourceQuery) {
  return unwrap<PageResponse<TrainResource>>(apiClient.get(`${BASE}/resources`, { params }));
}
export function getResource(id: number) {
  return unwrap<TrainResource>(apiClient.get(`${BASE}/resources/${id}`));
}
export function createResource(payload: TrainResourceCreateReq) {
  return unwrap<TrainResource>(apiClient.post(`${BASE}/resources`, payload));
}
export function updateResource(id: number, payload: TrainResourceUpdateReq) {
  return unwrap<TrainResource>(apiClient.put(`${BASE}/resources/${id}`, payload));
}
export function deleteResource(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/resources/${id}`));
}

// ==================== Task List (任务管理) ====================
export function pageTasks(params: TrainTaskQuery) {
  return unwrap<PageResponse<TrainTask>>(apiClient.get(`${BASE}/task-lists`, { params }));
}
export function getTask(id: number) {
  return unwrap<TrainTask>(apiClient.get(`${BASE}/task-lists/${id}`));
}
export function createTask(payload: TrainTaskCreateReq) {
  return unwrap<TrainTask>(apiClient.post(`${BASE}/task-lists`, payload));
}
export function updateTask(id: number, payload: TrainTaskUpdateReq) {
  return unwrap<TrainTask>(apiClient.put(`${BASE}/task-lists/${id}`, payload));
}
export function deleteTask(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/task-lists/${id}`));
}

// Task Infos (培训配置)
export function pageTaskInfos(params: TrainTaskInfoQuery) {
  return unwrap<PageResponse<TrainTaskInfo>>(apiClient.get(`${BASE}/task-infos`, { params }));
}
export function getTaskInfo(id: number) {
  return unwrap<TrainTaskInfo>(apiClient.get(`${BASE}/task-infos/${id}`));
}
export function createTaskInfo(payload: TrainTaskInfoCreateReq) {
  return unwrap<TrainTaskInfo>(apiClient.post(`${BASE}/task-infos`, payload));
}
export function updateTaskInfo(id: number, payload: TrainTaskInfoUpdateReq) {
  return unwrap<TrainTaskInfo>(apiClient.put(`${BASE}/task-infos/${id}`, payload));
}
export function deleteTaskInfo(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/task-infos/${id}`));
}

// ==================== Exam List (考试管理) ====================
import type { TrainExamPeople, TrainExamPeopleQuery } from '../types/training';

export function pageExams(params: TrainExamQuery) {
  return unwrap<PageResponse<TrainExam>>(apiClient.get(`${BASE}/exam-lists`, { params }));
}
export function getExam(id: number) {
  return unwrap<TrainExam>(apiClient.get(`${BASE}/exam-lists/${id}`));
}
export function createExam(payload: TrainExamCreateReq) {
  return unwrap<TrainExam>(apiClient.post(`${BASE}/exam-lists`, payload));
}
export function updateExam(id: number, payload: TrainExamUpdateReq) {
  return unwrap<TrainExam>(apiClient.put(`${BASE}/exam-lists/${id}`, payload));
}
export function deleteExam(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/exam-lists/${id}`));
}

// ----- 考生管理 -----
export function pageExamPeople(examId: number, params: Omit<TrainExamPeopleQuery, 'examId'>) {
  return unwrap<PageResponse<TrainExamPeople>>(
    apiClient.get(`${BASE}/exam-lists/${examId}/people-grades`, { params }),
  );
}
export function updateExamPeopleGrade(
  id: number,
  body: { inWhiteList?: number; normalScore?: number; score?: number; totalScore?: number },
) {
  return unwrap<void>(apiClient.put(`${BASE}/exam-lists/people-grades/${id}`, body));
}
export function importExamPeopleByIdCard(examId: number, idCardNumbers: string) {
  return unwrap<{ count: number }>(
    apiClient.post(`${BASE}/exam-lists/${examId}/import-people`, { idCardNumbers }),
  );
}
export function importExamPeopleFromTrain(examId: number, trainId: number) {
  return unwrap<{ count: number }>(
    apiClient.post(`${BASE}/exam-lists/${examId}/import-from-train/${trainId}`),
  );
}
export function batchDeleteExamPeople(ids: number[]) {
  return unwrap<void>(apiClient.post(`${BASE}/exam-lists/people/batch-delete`, { ids }));
}

// Exam Results (考试结果)
export function pageExamResults(params: ExamResultQuery) {
  return unwrap<PageResponse<ExamResult>>(apiClient.get(`${BASE}/exam-results`, { params }));
}
export function getExamResult(id: number) {
  return unwrap<ExamResult>(apiClient.get(`${BASE}/exam-results/${id}`));
}
export function createExamResult(payload: ExamResultCreateReq) {
  return unwrap<ExamResult>(apiClient.post(`${BASE}/exam-results`, payload));
}
export function updateExamResult(id: number, payload: ExamResultUpdateReq) {
  return unwrap<ExamResult>(apiClient.put(`${BASE}/exam-results/${id}`, payload));
}
export function deleteExamResult(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/exam-results/${id}`));
}

// ==================== Protocols (协议管理) ====================
export function pageProtocols(params: TrainProtocolQuery) {
  return unwrap<PageResponse<TrainProtocol>>(apiClient.get(`${BASE}/protocols`, { params }));
}
export function getProtocol(id: number) {
  return unwrap<TrainProtocol>(apiClient.get(`${BASE}/protocols/${id}`));
}
export function createProtocol(payload: TrainProtocolCreateReq) {
  return unwrap<TrainProtocol>(apiClient.post(`${BASE}/protocols`, payload));
}
export function updateProtocol(id: number, payload: TrainProtocolUpdateReq) {
  return unwrap<TrainProtocol>(apiClient.put(`${BASE}/protocols/${id}`, payload));
}
export function deleteProtocol(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/protocols/${id}`));
}

// ==================== Projects (项目管理) ====================
export function pageProjects(params: TrainProjectQuery) {
  return unwrap<PageResponse<TrainProject>>(apiClient.get(`${BASE}/projects`, { params }));
}
export function getProject(id: number) {
  return unwrap<TrainProject>(apiClient.get(`${BASE}/projects/${id}`));
}
export function createProject(payload: TrainProjectCreateReq) {
  return unwrap<TrainProject>(apiClient.post(`${BASE}/projects`, payload));
}
export function updateProject(id: number, payload: TrainProjectUpdateReq) {
  return unwrap<TrainProject>(apiClient.put(`${BASE}/projects/${id}`, payload));
}
export function deleteProject(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/projects/${id}`));
}

// ==================== Project Tables (十大管理表) ====================
export function pageProjectTables(params: TrainProjectTableQuery) {
  return unwrap<PageResponse<TrainProjectTable>>(apiClient.get(`${BASE}/project-tables`, { params }));
}
export function getProjectTable(id: number) {
  return unwrap<TrainProjectTable>(apiClient.get(`${BASE}/project-tables/${id}`));
}
export function createProjectTable(payload: TrainProjectTableCreateReq) {
  return unwrap<TrainProjectTable>(apiClient.post(`${BASE}/project-tables`, payload));
}
export function updateProjectTable(id: number, payload: TrainProjectTableUpdateReq) {
  return unwrap<TrainProjectTable>(apiClient.put(`${BASE}/project-tables/${id}`, payload));
}
export function deleteProjectTable(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/project-tables/${id}`));
}

// ==================== Dashboard (仪表盘) ====================
export function getDashboard() {
  return unwrap<TrainDashboard>(apiClient.get(`${BASE}/dashboard`));
}

export function getDashboardMilestones() {
  return unwrap<DashboardMilestoneItem[]>(apiClient.get(`${BASE}/dashboard/milestones`));
}

export function getDashboardStageDistribution() {
  return unwrap<DashboardStageDistribution>(apiClient.get(`${BASE}/dashboard/stage-distribution`));
}

// ==================== Talent Needs (人才数据管理) ====================
export function pageTalentNeeds(params: TrainTalentNeedQuery) {
  return unwrap<PageResponse<TrainTalentNeed>>(apiClient.get(`${BASE}/talent-needs`, { params }));
}
export function getTalentNeed(id: number) {
  return unwrap<TrainTalentNeed>(apiClient.get(`${BASE}/talent-needs/${id}`));
}
export function createTalentNeed(payload: TrainTalentNeedCreateReq) {
  return unwrap<TrainTalentNeed>(apiClient.post(`${BASE}/talent-needs`, payload));
}
export function updateTalentNeed(id: number, payload: TrainTalentNeedUpdateReq) {
  return unwrap<TrainTalentNeed>(apiClient.put(`${BASE}/talent-needs/${id}`, payload));
}
export function deleteTalentNeed(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/talent-needs/${id}`));
}

// ==================== Platform Users (平台用户) ====================
export function pagePlatformUsers(params: PlatformUserQuery) {
  return unwrap<PageResponse<PlatformUser>>(apiClient.get(`${BASE}/platform-users`, { params }));
}
export function getPlatformUser(id: number) {
  return unwrap<PlatformUser>(apiClient.get(`${BASE}/platform-users/${id}`));
}
export function createPlatformUser(payload: PlatformUserCreateReq) {
  return unwrap<PlatformUser>(apiClient.post(`${BASE}/platform-users`, payload));
}
export function updatePlatformUser(id: number, payload: PlatformUserUpdateReq) {
  return unwrap<PlatformUser>(apiClient.put(`${BASE}/platform-users/${id}`, payload));
}
export function deletePlatformUser(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/platform-users/${id}`));
}

// ==================== Teacher Bank (师资管理) ====================
export function pageTeacherBanks(params: TrainTeacherBankQuery) {
  return unwrap<PageResponse<TrainTeacherBank>>(apiClient.get(`${BASE}/teacher-banks`, { params }));
}
export function getTeacherBank(id: number) {
  return unwrap<TrainTeacherBank>(apiClient.get(`${BASE}/teacher-banks/${id}`));
}
export function createTeacherBank(payload: TrainTeacherBankCreateReq) {
  return unwrap<TrainTeacherBank>(apiClient.post(`${BASE}/teacher-banks`, payload));
}
export function updateTeacherBank(id: number, payload: TrainTeacherBankUpdateReq) {
  return unwrap<TrainTeacherBank>(apiClient.put(`${BASE}/teacher-banks/${id}`, payload));
}
export function deleteTeacherBank(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/teacher-banks/${id}`));
}

// ==================== User Info (用户信息) ====================
export function pageUserInfos(params: TrainUserInfoQuery) {
  return unwrap<PageResponse<TrainUserInfo>>(apiClient.get(`${BASE}/user-infos`, { params }));
}
export function getUserInfo(id: number) {
  return unwrap<TrainUserInfo>(apiClient.get(`${BASE}/user-infos/${id}`));
}
export function createUserInfo(payload: TrainUserInfoCreateReq) {
  return unwrap<TrainUserInfo>(apiClient.post(`${BASE}/user-infos`, payload));
}
export function updateUserInfo(id: number, payload: TrainUserInfoUpdateReq) {
  return unwrap<TrainUserInfo>(apiClient.put(`${BASE}/user-infos/${id}`, payload));
}
export function deleteUserInfo(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-infos/${id}`));
}

// ==================== Budgets (预算费用管理) ====================
export function pageBudgets(params: TrainBudgetQuery) {
  return unwrap<PageResponse<TrainBudget>>(apiClient.get(`${BASE}/budgets`, { params }));
}
export function getBudget(id: number) {
  return unwrap<TrainBudget>(apiClient.get(`${BASE}/budgets/${id}`));
}
export function createBudget(payload: TrainBudgetCreateReq) {
  return unwrap<TrainBudget>(apiClient.post(`${BASE}/budgets`, payload));
}
export function updateBudget(id: number, payload: TrainBudgetUpdateReq) {
  return unwrap<TrainBudget>(apiClient.put(`${BASE}/budgets/${id}`, payload));
}
export function deleteBudget(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/budgets/${id}`));
}

// ==================== File Lists (文件管理) ====================
export function pageFileLists(params: TrainFileListQuery) {
  return unwrap<PageResponse<TrainFileList>>(apiClient.get(`${BASE}/file-lists`, { params }));
}
export function getFileList(id: number) {
  return unwrap<TrainFileList>(apiClient.get(`${BASE}/file-lists/${id}`));
}
export function createFileList(payload: TrainFileListCreateReq) {
  return unwrap<TrainFileList>(apiClient.post(`${BASE}/file-lists`, payload));
}
export function updateFileList(id: number, payload: TrainFileListUpdateReq) {
  return unwrap<TrainFileList>(apiClient.put(`${BASE}/file-lists/${id}`, payload));
}
export function deleteFileList(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/file-lists/${id}`));
}

// ==================== File Details (文件明细) ====================
export function pageFileDetails(params: TrainFileDetailQuery) {
  return unwrap<PageResponse<TrainFileDetail>>(apiClient.get(`${BASE}/file-details`, { params }));
}
export function getFileDetail(id: number) {
  return unwrap<TrainFileDetail>(apiClient.get(`${BASE}/file-details/${id}`));
}
export function createFileDetail(payload: TrainFileDetailCreateReq) {
  return unwrap<TrainFileDetail>(apiClient.post(`${BASE}/file-details`, payload));
}
export function updateFileDetail(id: number, payload: TrainFileDetailUpdateReq) {
  return unwrap<TrainFileDetail>(apiClient.put(`${BASE}/file-details/${id}`, payload));
}
export function deleteFileDetail(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/file-details/${id}`));
}

// ==================== Info Lists (培训信息) ====================
export function pageInfoLists(params: TrainInfoListQuery) {
  const { limit, pageSize, page, ...rest } = params;
  return unwrap<TrainInfoListPageResponse>(
    apiClient.get(`${BASE}/info-lists`, {
      params: {
        ...rest,
        page,
        pageSize: pageSize ?? limit ?? 10,
      },
    }),
  );
}
export function getInfoList(id: number) {
  return unwrap<TrainInfoList>(apiClient.get(`${BASE}/info-lists/${id}`));
}
export function createInfoList(payload: TrainInfoListCreateReq) {
  return unwrap<TrainInfoList>(apiClient.post(`${BASE}/info-lists`, payload));
}
export function updateInfoList(id: number, payload: TrainInfoListUpdateReq) {
  return unwrap<TrainInfoList>(apiClient.put(`${BASE}/info-lists/${id}`, payload));
}
export function deleteInfoList(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/info-lists/${id}`));
}

// ==================== Info Classes (培训班级信息) ====================
export function pageInfoClasses(params: TrainInfoClassQuery) {
  return unwrap<PageResponse<TrainInfoClass>>(apiClient.get(`${BASE}/info-classes`, { params }));
}
export function getInfoClass(id: number) {
  return unwrap<TrainInfoClass>(apiClient.get(`${BASE}/info-classes/${id}`));
}
export function createInfoClass(payload: TrainInfoClassCreateReq) {
  return unwrap<TrainInfoClass>(apiClient.post(`${BASE}/info-classes`, payload));
}
export function updateInfoClass(id: number, payload: TrainInfoClassUpdateReq) {
  return unwrap<TrainInfoClass>(apiClient.put(`${BASE}/info-classes/${id}`, payload));
}
export function deleteInfoClass(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/info-classes/${id}`));
}

// ==================== Task Info Details (任务培训配置详情) ====================
export function pageTaskInfoDetails(params: TrainTaskInfoDetailQuery) {
  return unwrap<PageResponse<TrainTaskInfoDetail>>(apiClient.get(`${BASE}/task-info-details`, { params }));
}
export function getTaskInfoDetail(id: number) {
  return unwrap<TrainTaskInfoDetail>(apiClient.get(`${BASE}/task-info-details/${id}`));
}
export function createTaskInfoDetail(payload: TrainTaskInfoDetailCreateReq) {
  return unwrap<TrainTaskInfoDetail>(apiClient.post(`${BASE}/task-info-details`, payload));
}
export function updateTaskInfoDetail(id: number, payload: TrainTaskInfoDetailUpdateReq) {
  return unwrap<TrainTaskInfoDetail>(apiClient.put(`${BASE}/task-info-details/${id}`, payload));
}
export function deleteTaskInfoDetail(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/task-info-details/${id}`));
}

// ==================== Task Logs (任务日志) ====================
export function pageTaskLogs(params: TrainTaskLogQuery) {
  return unwrap<PageResponse<TrainTaskLog>>(apiClient.get(`${BASE}/task-logs`, { params }));
}
export function getTaskLog(id: number) {
  return unwrap<TrainTaskLog>(apiClient.get(`${BASE}/task-logs/${id}`));
}
export function createTaskLog(payload: TrainTaskLogCreateReq) {
  return unwrap<TrainTaskLog>(apiClient.post(`${BASE}/task-logs`, payload));
}
export function updateTaskLog(id: number, payload: TrainTaskLogUpdateReq) {
  return unwrap<TrainTaskLog>(apiClient.put(`${BASE}/task-logs/${id}`, payload));
}
export function deleteTaskLog(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/task-logs/${id}`));
}

// ==================== User Learn Logs (用户学习记录) ====================
export function pageUserLearnLogs(params: TrainUserLearnLogQuery) {
  return unwrap<PageResponse<TrainUserLearnLog>>(apiClient.get(`${BASE}/user-learn-logs`, { params }));
}
export function getUserLearnLog(id: number) {
  return unwrap<TrainUserLearnLog>(apiClient.get(`${BASE}/user-learn-logs/${id}`));
}
export function createUserLearnLog(payload: TrainUserLearnLogCreateReq) {
  return unwrap<TrainUserLearnLog>(apiClient.post(`${BASE}/user-learn-logs`, payload));
}
export function updateUserLearnLog(id: number, payload: TrainUserLearnLogUpdateReq) {
  return unwrap<TrainUserLearnLog>(apiClient.put(`${BASE}/user-learn-logs/${id}`, payload));
}
export function deleteUserLearnLog(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-learn-logs/${id}`));
}

// ==================== User Course Plans (用户选课计划) ====================
export function pageUserCoursePlans(params: TrainUserCoursePlanQuery) {
  return unwrap<PageResponse<TrainUserCoursePlan>>(apiClient.get(`${BASE}/user-course-plans`, { params }));
}
export function getUserCoursePlan(id: number) {
  return unwrap<TrainUserCoursePlan>(apiClient.get(`${BASE}/user-course-plans/${id}`));
}
export function createUserCoursePlan(payload: TrainUserCoursePlanCreateReq) {
  return unwrap<TrainUserCoursePlan>(apiClient.post(`${BASE}/user-course-plans`, payload));
}
export function updateUserCoursePlan(id: number, payload: TrainUserCoursePlanUpdateReq) {
  return unwrap<TrainUserCoursePlan>(apiClient.put(`${BASE}/user-course-plans/${id}`, payload));
}
export function deleteUserCoursePlan(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-course-plans/${id}`));
}

// ==================== User Course Papers (用户课程证书) ====================
export function pageUserCoursePapers(params: TrainUserCoursePaperQuery) {
  return unwrap<PageResponse<TrainUserCoursePaper>>(apiClient.get(`${BASE}/user-course-papers`, { params }));
}
export function getUserCoursePaper(id: number) {
  return unwrap<TrainUserCoursePaper>(apiClient.get(`${BASE}/user-course-papers/${id}`));
}
export function createUserCoursePaper(payload: TrainUserCoursePaperCreateReq) {
  return unwrap<TrainUserCoursePaper>(apiClient.post(`${BASE}/user-course-papers`, payload));
}
export function updateUserCoursePaper(id: number, payload: TrainUserCoursePaperUpdateReq) {
  return unwrap<TrainUserCoursePaper>(apiClient.put(`${BASE}/user-course-papers/${id}`, payload));
}
export function deleteUserCoursePaper(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-course-papers/${id}`));
}

// ==================== User Papers (用户证书) ====================
export function pageUserPapers(params: TrainUserPaperQuery) {
  return unwrap<PageResponse<TrainUserPaper>>(apiClient.get(`${BASE}/user-papers`, { params }));
}
export function getUserPaper(id: number) {
  return unwrap<TrainUserPaper>(apiClient.get(`${BASE}/user-papers/${id}`));
}
export function createUserPaper(payload: TrainUserPaperCreateReq) {
  return unwrap<TrainUserPaper>(apiClient.post(`${BASE}/user-papers`, payload));
}
export function updateUserPaper(id: number, payload: TrainUserPaperUpdateReq) {
  return unwrap<TrainUserPaper>(apiClient.put(`${BASE}/user-papers/${id}`, payload));
}
export function deleteUserPaper(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-papers/${id}`));
}

// ==================== User Patents (用户专利) ====================
export function pageUserPatents(params: TrainUserPatentQuery) {
  return unwrap<PageResponse<TrainUserPatent>>(apiClient.get(`${BASE}/user-patents`, { params }));
}
export function getUserPatent(id: number) {
  return unwrap<TrainUserPatent>(apiClient.get(`${BASE}/user-patents/${id}`));
}
export function createUserPatent(payload: TrainUserPatentCreateReq) {
  return unwrap<TrainUserPatent>(apiClient.post(`${BASE}/user-patents`, payload));
}
export function updateUserPatent(id: number, payload: TrainUserPatentUpdateReq) {
  return unwrap<TrainUserPatent>(apiClient.put(`${BASE}/user-patents/${id}`, payload));
}
export function deleteUserPatent(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-patents/${id}`));
}

// ==================== User Exam Logs (用户考试日志) ====================
export function pageUserExamLogs(params: TrainUserExamLogQuery) {
  return unwrap<PageResponse<TrainUserExamLog>>(apiClient.get(`${BASE}/user-exam-logs`, { params }));
}
export function getUserExamLog(id: number) {
  return unwrap<TrainUserExamLog>(apiClient.get(`${BASE}/user-exam-logs/${id}`));
}
export function createUserExamLog(payload: TrainUserExamLogCreateReq) {
  return unwrap<TrainUserExamLog>(apiClient.post(`${BASE}/user-exam-logs`, payload));
}
export function updateUserExamLog(id: number, payload: TrainUserExamLogUpdateReq) {
  return unwrap<TrainUserExamLog>(apiClient.put(`${BASE}/user-exam-logs/${id}`, payload));
}
export function deleteUserExamLog(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-exam-logs/${id}`));
}

// ==================== User Abilities (用户能力) ====================
export function pageUserAbilities(params: TrainUserAbilityQuery) {
  return unwrap<PageResponse<TrainUserAbility>>(apiClient.get(`${BASE}/user-abilities`, { params }));
}
export function getUserAbility(id: number) {
  return unwrap<TrainUserAbility>(apiClient.get(`${BASE}/user-abilities/${id}`));
}
export function createUserAbility(payload: TrainUserAbilityCreateReq) {
  return unwrap<TrainUserAbility>(apiClient.post(`${BASE}/user-abilities`, payload));
}
export function updateUserAbility(id: number, payload: TrainUserAbilityUpdateReq) {
  return unwrap<TrainUserAbility>(apiClient.put(`${BASE}/user-abilities/${id}`, payload));
}
export function deleteUserAbility(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/user-abilities/${id}`));
}

// ==================== Summaries (培训总结/经验萃取) ====================
export function pageSummaries(params: TrainSummaryQuery) {
  return unwrap<PageResponse<TrainSummary>>(apiClient.get(`${BASE}/summaries`, { params }));
}
export function getSummary(id: number) {
  return unwrap<TrainSummary>(apiClient.get(`${BASE}/summaries/${id}`));
}
export function createSummary(payload: TrainSummaryCreateReq) {
  return unwrap<TrainSummary>(apiClient.post(`${BASE}/summaries`, payload));
}
export function updateSummary(id: number, payload: TrainSummaryUpdateReq) {
  return unwrap<TrainSummary>(apiClient.put(`${BASE}/summaries/${id}`, payload));
}
export function deleteSummary(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/summaries/${id}`));
}

// ==================== Certificates (证书管理) ====================
export function pageCertificates(params: TrainCertificateQuery) {
  return unwrap<PageResponse<TrainCertificate>>(apiClient.get(`${BASE}/certificates`, { params }));
}
export function getCertificate(id: number) {
  return unwrap<TrainCertificate>(apiClient.get(`${BASE}/certificates/${id}`));
}
export function createCertificate(payload: TrainCertificateCreateReq) {
  return unwrap<TrainCertificate>(apiClient.post(`${BASE}/certificates`, payload));
}
export function updateCertificate(id: number, payload: TrainCertificateUpdateReq) {
  return unwrap<TrainCertificate>(apiClient.put(`${BASE}/certificates/${id}`, payload));
}
export function deleteCertificate(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/certificates/${id}`));
}

// ==================== Demand Train (企业培训需求) ====================
export function pageDemandTrains(params: DemandTrainQuery) {
  return unwrap<PageResponse<DemandTrain>>(apiClient.get(`${BASE}/demand-trains`, { params }));
}
export function getDemandTrain(id: number) {
  return unwrap<DemandTrain>(apiClient.get(`${BASE}/demand-trains/${id}`));
}
export function createDemandTrain(payload: DemandTrain) {
  return unwrap<DemandTrain>(apiClient.post(`${BASE}/demand-trains`, payload));
}
export function updateDemandTrain(id: number, payload: DemandTrain) {
  return unwrap<DemandTrain>(apiClient.put(`${BASE}/demand-trains/${id}`, payload));
}
export function deleteDemandTrain(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/demand-trains/${id}`));
}

// ==================== Demand Employee (企业人才需求) ====================
export function pageDemandEmployees(params: DemandEmployeeQuery) {
  return unwrap<PageResponse<DemandEmployee>>(apiClient.get(`${BASE}/demand-employees`, { params }));
}
export function getDemandEmployee(id: number) {
  return unwrap<DemandEmployee>(apiClient.get(`${BASE}/demand-employees/${id}`));
}
export function createDemandEmployee(payload: DemandEmployeeCreateReq) {
  return unwrap<DemandEmployee>(apiClient.post(`${BASE}/demand-employees`, payload));
}
export function updateDemandEmployee(id: number, payload: DemandEmployeeUpdateReq) {
  return unwrap<DemandEmployee>(apiClient.put(`${BASE}/demand-employees/${id}`, payload));
}
export function deleteDemandEmployee(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/demand-employees/${id}`));
}
export function batchDeleteDemandEmployees(ids: number[]) {
  return unwrap<number[]>(apiClient.post(`${BASE}/demand-employees/batch-delete`, ids));
}

/** 待处理需求列表，del=0 正常数据，del=1 回收站 */
export function pageDemandPendings(params: { page?: number; pageSize?: number; keyword?: string; del?: number }) {
  return unwrap<PageResponse<DemandPending>>(apiClient.get(`${BASE}/demand-trains/pendings`, { params }));
}

/** 公共数据（企业/工种/申报条件等），tags 可选 ['company','worktype','traininfo','kejian','condition'] */
export function getDemandPublicData(tags?: string[]) {
  return unwrap<DemandPublicData>(apiClient.get(`${BASE}/demand-trains/public-data`, { params: tags ? { tags } : {} }));
}

/** 需求转培训开班 */
export function convertDemandToTraining(payload: DemandConvertReq) {
  return unwrap<{ id: number; title: string }>(apiClient.post(`${BASE}/demand-trains/convert`, payload));
}

// ==================== Training Program (培训方案管理) ====================

/** 方案列表（含费用汇总） */
export function pageSolutionList(params: { page?: number; pageSize?: number }) {
  return unwrap<PageResponse<TrainSolutionList>>(apiClient.get(`${BASE}/solutions/list`, { params }));
}

/** 方案详情（含经费明细） */
export function getSolutionDetail(id: number) {
  return unwrap<TrainSolutionDetail>(apiClient.get(`${BASE}/solutions/${id}/detail`));
}

/** 保存方案（创建/更新 + 经费明细） */
export function saveSolution(payload: TrainSolutionSaveReq) {
  return unwrap<{ info: { id: number; title: string } }>(apiClient.post(`${BASE}/solutions/save`, payload));
}

/** 删除方案 */
export function deleteSolutionById(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/solutions/${id}`));
}

/** 批量删除企业需求 */
export function batchDeleteDemandTrains(ids: number[]) {
  return unwrap<number[]>(apiClient.post(`${BASE}/demand-trains/batch-delete`, ids));
}

/** 根据需求ID获取报名学员列表 */
export function getDemandStudents(id: number) {
  return unwrap<TrainStudent[]>(apiClient.get(`${BASE}/demand-trains/${id}/students`));
}

/** 根据需求ID获取方案详情 */
export function getSolutionByDemandId(traindemandId: number) {
  return unwrap<SolutionByDemand>(apiClient.get(`${BASE}/solutions/by-demand-id`, { params: { traindemandId } }));
}

/** 企业确认方案 */
export function companyConfirmInfoList(id: number) {
  return unwrap<void>(apiClient.post(`${BASE}/info-lists/${id}/company-confirm`));
}

/** 企业驳回方案 */
export function companyRejectInfoList(id: number, reason: string) {
  return unwrap<void>(apiClient.post(`${BASE}/info-lists/${id}/company-reject`, null, { params: { reason } }));
}

// ==================== Info Classes (班级管理 - 新版) ====================

/** 班级列表（含企业名、工种名、培训标题） */
export function pageInfoClassList(params: {
  page?: number;
  pageSize?: number;
  companyId?: number;
  workTypeId?: number;
  trainInfoId?: number;
  companyConfirmStatus?: number;
  keyword?: string;
}) {
  return unwrap<TrainInfoClassPageResponse>(apiClient.get(`${BASE}/info-classes`, { params }));
}

export function companyConfirmInfoClass(id: number) {
  return unwrap<{ id: number; company_confirm_status: number }>(
    apiClient.post(`${BASE}/info-classes/${id}/company-confirm`),
  );
}

export function companyRejectInfoClass(id: number, reason: string) {
  return unwrap<{ id: number; company_confirm_status: number }>(
    apiClient.post(`${BASE}/info-classes/${id}/company-reject`, null, { params: { reason } }),
  );
}

export function ensureClassCollection(classId: number | string) {
  return unwrap<{
    signUpId: number;
    title?: string;
    trainClassId?: number;
    trainInfoId?: number;
    collectionSource?: string;
    seeded?: number;
    profile_total?: number;
    profile_approved?: number;
    profile_pending?: number;
    profile_ready?: boolean;
    roster_count?: number;
  }>(apiClient.post(`${BASE}/info-classes/${classId}/ensure-collection`));
}

export interface ClassPracticeScope {
  classId: number;
  classTitle?: string;
  workTypeId?: number;
  workTypeLevel?: number;
  configured: boolean;
  enabled: number;
  remark?: string;
  /** 刷题开放开始日 yyyy-MM-dd，空=不限 */
  practiceStartTime?: string | null;
  /** 刷题截止日 yyyy-MM-dd（含当天），空=不过期 */
  practiceEndTime?: string | null;
  courseIds: number[];
  knowledgeIds: number[];
  courses: { courseId: number; title: string }[];
  knowledges: { knowledgeId: number; courseId: number; title: string }[];
  appBound?: boolean;
  hint?: string;
}

export function getClassPracticeScope(classId: number | string) {
  return unwrap<ClassPracticeScope>(apiClient.get(`${BASE}/info-classes/${classId}/practice-scope`));
}

export function listPracticeScopeCourses(params?: { keyword?: string; workTypeId?: number }) {
  return unwrap<{ items: { id: number; title: string; level?: number; workTypeId?: number }[] }>(
    apiClient.get(`${BASE}/practice-scope/options/courses`, { params }),
  );
}

export function listPracticeScopeKnowledgeTree(courseId: number) {
  return unwrap<{ id: number; title: string; courseId?: number; parentId?: number; children?: unknown[] }[]>(
    apiClient.get(`${BASE}/practice-scope/options/knowledge-tree`, { params: { courseId } }),
  );
}

export function suggestClassPracticeCourses(classId: number | string) {
  return unwrap<{
    classId: number;
    workTypeId?: number;
    workTypeLevel?: number;
    courses: { courseId: number; title: string; level?: number }[];
  }>(apiClient.get(`${BASE}/info-classes/${classId}/practice-scope/suggest-courses`));
}

export function saveClassPracticeScope(
  classId: number | string,
  payload: {
    enabled?: number;
    remark?: string;
    practiceStartTime?: string | null;
    practiceEndTime?: string | null;
    courseIds?: number[];
    knowledgeIds?: number[];
  },
) {
  return unwrap<ClassPracticeScope>(
    apiClient.put(`${BASE}/info-classes/${classId}/practice-scope`, payload),
  );
}

export interface PracticeAnalyticsQuery {
  from?: string;
  to?: string;
  inScopeOnly?: boolean;
  keyword?: string;
  page?: number;
  pageSize?: number;
  limit?: number;
}

export interface PracticeAnalyticsSummary {
  classId: number;
  classTitle?: string;
  from: string;
  to: string;
  inScopeOnly: boolean;
  scopeKnowledgeCount?: number | null;
  rosterCount: number;
  withAccountCount: number;
  activeUserCount: number;
  participationRate: number | null;
  answerCount: number;
  rightCount: number;
  accuracyRate: number | null;
  avgAnswersPerActive: number;
  activeUserCount7d: number;
  hint?: string | null;
}

export interface PracticeAnalyticsStudentRow {
  peopleId: number;
  realname?: string;
  phone?: string;
  idCard?: string;
  platformUserId?: number;
  hasAccount: boolean;
  answerCount: number;
  rightCount: number;
  accuracyRate: number | null;
  paperCount: number;
  practiceDays: number;
  lastPracticeAt?: string | null;
  idleDays?: number | null;
}

export interface PracticeAnalyticsKnowledgeRow {
  knowledgeId: number;
  title: string;
  courseId?: number | null;
  answerCount: number;
  wrongCount: number;
  rightCount: number;
  wrongRate: number | null;
  userCount: number;
}

export interface PracticeAnalyticsDailyRow {
  bizDate: string;
  answerCount: number;
  rightCount: number;
  accuracyRate: number | null;
  userCount: number;
  paperCount: number;
}

function practiceAnalyticsParams(params?: PracticeAnalyticsQuery) {
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

export function getClassPracticeAnalyticsSummary(classId: number | string, params?: PracticeAnalyticsQuery) {
  return unwrap<PracticeAnalyticsSummary>(
    apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/summary`, {
      params: practiceAnalyticsParams(params),
    }),
  );
}

export function getClassPracticeAnalyticsStudents(classId: number | string, params?: PracticeAnalyticsQuery) {
  return unwrap<{
    classId: number;
    from: string;
    to: string;
    inScopeOnly: boolean;
    page: number;
    pageSize: number;
    total: number;
    items: PracticeAnalyticsStudentRow[];
  }>(
    apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/students`, {
      params: practiceAnalyticsParams(params),
    }),
  );
}

export function getClassPracticeAnalyticsKnowledges(classId: number | string, params?: PracticeAnalyticsQuery) {
  return unwrap<{
    classId: number;
    from: string;
    to: string;
    inScopeOnly: boolean;
    items: PracticeAnalyticsKnowledgeRow[];
  }>(
    apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/knowledges`, {
      params: practiceAnalyticsParams(params),
    }),
  );
}

export function getClassPracticeAnalyticsDaily(classId: number | string, params?: PracticeAnalyticsQuery) {
  return unwrap<{
    classId: number;
    from: string;
    to: string;
    inScopeOnly: boolean;
    items: PracticeAnalyticsDailyRow[];
  }>(
    apiClient.get(`${BASE}/info-classes/${classId}/practice-analytics/daily`, {
      params: practiceAnalyticsParams(params),
    }),
  );
}

/** 导出班级刷题学情 Excel：指定日期区间，或 allTime=true 导出全部历史 */
export async function exportClassPracticeAnalytics(
  classId: number | string,
  params?: PracticeAnalyticsQuery & { allTime?: boolean },
) {
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
  const filename =
    parseContentDispositionFilename(response) || `刷题学情_${classId}.xlsx`;
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
}

/** 新增/编辑班级 */
export function saveInfoClass(payload: TrainInfoClassCreateReq) {
  return unwrap<{ id: number; title: string; nums: number }>(apiClient.post(`${BASE}/info-classes/save`, payload));
}

/** 批量删除班级 */
export function batchDeleteInfoClasses(ids: Array<number | string>) {
  return unwrap<{ count: number }>(apiClient.post(`${BASE}/info-classes/batch-delete`, { ids }));
}

// ==================== 班级学员 ====================

/** 班级学员列表 */
export function listClassStudents(classId: number | string, currentPage = 1, limit = 20) {
  return unwrap<ClassStudentPageResponse>(apiClient.get(`${BASE}/class-student-list`, { params: { classId, currentPage, limit } }));
}

/** 编辑学员 */
export function saveClassStudent(params: Record<string, unknown>) {
  return unwrap<{ id: number; realname: string }>(apiClient.post(`${BASE}/class-student-save`, params));
}

/** 批量删除学员 */
export function deleteClassStudents(studentIds: Array<number | string>) {
  return unwrap<{ count: number }>(apiClient.post(`${BASE}/class-student-del`, { studentIds }));
}

/** 可导入报名人员 */
export function listSignUpSelf(traininfoId: number | string, keyword?: string, classId?: number | string) {
  return unwrap<{ lists: SignUpPerson[]; signUpId?: number; collectionSource?: string }>(
    apiClient.get(`${BASE}/sign-up-self-list`, { params: { traininfoId, keyword, classId } }),
  );
}

/** 从报名导入学员 */
export function importStudents(params: { traininfoId: number | string; classId: number | string; studentIds: Array<number | string> }) {
  return unwrap<{
    count: number;
    failed: { id?: number; idCard?: string; name?: string; reason: string }[];
    platformUserSync?: {
      created: number;
      reused: number;
      updated: number;
      skipped: number;
      warnings: { name: string; idCard: string; reason: string }[];
    };
  }>(apiClient.post(`${BASE}/class-student-import`, params));
}

/** 按身份证号导入学员 */
export function importStudentsByIdCard(params: { traininfoId: number | string; classId: number | string; studentIds: string }) {
  return unwrap<{
    count: number;
    failed: { id?: number; idCard?: string; name?: string; reason: string }[];
    platformUserSync?: {
      created: number;
      reused: number;
      updated: number;
      skipped: number;
      warnings: { name: string; idCard: string; reason: string }[];
    };
  }>(apiClient.post(`${BASE}/class-student-import-by-idcard`, params));
}

// ==================== 教务/负责人 ====================

/** 教务/负责人列表 */
export function listClassOthers(train_info_id: number | string, id: number | string, tag: string) {
  return unwrap<{ list: ClassOther[] }>(apiClient.get(`${BASE}/class-other`, { params: { train_info_id, id, tag } }));
}

/** 新增/编辑教务/负责人 */
export function saveClassOther(params: Record<string, unknown>) {
  return unwrap<{ id: number }>(apiClient.post(`${BASE}/class-other-save`, params));
}

/** 批量删除教务/负责人 */
export function deleteClassOthers(ids: Array<number | string>) {
  return unwrap<{ count: number }>(apiClient.post(`${BASE}/class-other-del`, { ids }));
}

/** 可导入教务数据源（教师库） */
export function listOtherImportData(tag: string) {
  return unwrap<{ list: TeacherBankItem[] }>(apiClient.get(`${BASE}/class-other-import-data`, { params: { tag } }));
}

/** 从教师库导入教务 */
export function importOthers(params: { traininfoId: number | string; classId: number | string; tag: string; ids: Array<number | string> }) {
  return unwrap<{ count: number; failed: { id: number; reason: string }[] }>(apiClient.post(`${BASE}/class-other-import`, params));
}

/** 班级角色列表 */
export function listClassRoles(classId: number | string) {
  return unwrap<TrainClassRole[]>(apiClient.get(`${BASE}/classes/${classId}/roles`));
}

/** 班级角色摘要（跟班） */
export function getClassRoleSummary(classId: number | string) {
  return unwrap<TrainClassRoleSummary>(apiClient.get(`${BASE}/classes/${classId}/role-summary`));
}

/** 指派班级角色 */
export function assignClassRole(
  classId: number | string,
  payload: {
    roleCode: string;
    subjectType?: string;
    subjectId: number | string;
    peopleId?: number | string;
    displayName?: string;
    phone?: string;
    idCard?: string;
  },
) {
  return unwrap<TrainClassRole>(apiClient.post(`${BASE}/classes/${classId}/roles/assign`, payload));
}

/** 卸任班级角色 */
export function endClassRole(classId: number | string, roleId: number | string) {
  return unwrap<void>(apiClient.post(`${BASE}/classes/${classId}/roles/${roleId}/end`));
}

// ==================== 培训任务 ====================

/** 班级培训任务列表 */
export function listClassTasks(classId: number | string) {
  return unwrap<{ list: ClassTask[] }>(apiClient.get(`${BASE}/class-task`, { params: { classId } }));
}

/** 新增/编辑培训任务 */
export function saveClassTask(params: { id?: number | string; classId?: number | string; ck_id?: number; date?: string }) {
  return unwrap<{ id: number; ck_id: number; date: string; train_class_id: number }>(apiClient.post(`${BASE}/class-task-save`, params));
}

// ==================== 材料模板（开班申报） ====================

export interface TrainFileTempItem {
  id: number;
  type?: string | number;
  city?: string;
  title: string;
  path_source?: string;
  path_used?: string;
  ext?: string;
  urlSource?: string;
  urlUsed?: string;
}

export interface TrainFileTempSavePayload {
  id?: number;
  type?: number;
  city?: string;
  title: string;
  pathSource: string;
  pathUsed?: string;
  ext?: string;
}

/** 材料模板列表 type=1 培训 */
export function listFileTemps(params: { type?: number; ids?: number[] }) {
  const ids = params.ids?.length ? params.ids.join(',') : undefined;
  return unwrap<{ list: TrainFileTempItem[] }>(apiClient.get(`${BASE}/file-temps/list`, {
    params: { type: params.type ?? 1, ids },
  }));
}

export function saveFileTemp(payload: TrainFileTempSavePayload) {
  return unwrap<{ info: Record<string, unknown> }>(apiClient.post(`${BASE}/file-temps/save`, payload));
}

export function batchDeleteFileTemps(ids: number[]) {
  return unwrap<{ count: number }>(apiClient.post(`${BASE}/file-temps/batch-delete`, { ids }));
}

// ==================== 班级文件模板挂载 ====================

/** 挂载文件模板到班级 */
export function attachFileTemps(classId: number, ids: number[]) {
  return unwrap<{ classId: number; count?: number; ids?: number[]; fileTemps?: number[] }>(
    apiClient.post(`${BASE}/info-classes/${classId}/file-temps`, { ids }),
  );
}

/** 移除班级文件模板 */
export function detachFileTemp(classId: number, templateId: number) {
  return unwrap<{ classId: number; fileTemps: number[] }>(apiClient.delete(`${BASE}/info-classes/${classId}/file-temps/${templateId}`));
}

// ==================== 班级材料生成 ====================

/** 生成班级申报材料 */
export function generateMaterials(classId: number) {
  return unwrap<{ downUrl: string; fileId: number }>(apiClient.post(`${BASE}/info-classes/${classId}/materials/generate`));
}

// ==================== 毕业管理 ====================

/** 可毕业班级列表 */
export function listGraduationCandidates(params: { page?: number; pageSize?: number }) {
  return unwrap<{ list: Record<string, unknown>[]; total: number }>(apiClient.get(`${BASE}/info-classes/graduation-candidates`, { params }));
}

/** 班级可毕业学员列表 */
export function listGraduationStudents(classId: number) {
  return unwrap<{ classTitle: string; list: Record<string, unknown>[]; total: number }>(apiClient.get(`${BASE}/info-classes/${classId}/graduation-students`));
}

/** 执行毕业 */
export function graduateClass(classId: number) {
  return unwrap<{ classId: number; trainInfoId: number; graduateCount: number; status: string }>(apiClient.post(`${BASE}/info-classes/${classId}/graduate`));
}

/** 发送毕业通知 */
export function notifyGraduation(classId: number) {
  return unwrap<{ classId: number; notifiedCount: number; message: string }>(apiClient.post(`${BASE}/info-classes/${classId}/graduation-notify`));
}

// ==================== 课表配置 ====================

/** 获取班级课表（空且有开班课表文件时后端会自动解析导入） */
export function getClassSchedule(classId: number) {
  return unwrap<Record<string, unknown>[]>(apiClient.get(`${BASE}/info-classes/${classId}/schedule`));
}

/** 批量保存课表（跟班调课；不回写开班上传文件） */
export function saveClassSchedule(classId: number, items: Record<string, unknown>[]) {
  return unwrap<{ classId: number; count: number }>(apiClient.put(`${BASE}/info-classes/${classId}/schedule`, { items }));
}

/** 从开班准备上传的 Excel 导入/重导结构化课表 */
export function importClassScheduleFromKcbiao(
  classId: number,
  force = false,
  confirmOverwriteManual = false,
) {
  return unwrap<{
    classId: number;
    count?: number;
    imported?: number;
    skipped?: boolean;
    reason?: string;
    manualEdited?: boolean;
  }>(
    apiClient.post(`${BASE}/info-classes/${classId}/schedule/import-from-kcbiao`, null, {
      params: { force, confirmOverwriteManual },
    }),
  );
}

/** 课表双源状态 */
export function getClassScheduleStatus(classId: number) {
  return unwrap<{
    classId: number;
    hasKcbiaoFile: boolean;
    kcbiaoFile?: string;
    manualEdited: boolean;
    importedAt?: string | null;
    sessionCount: number;
  }>(apiClient.get(`${BASE}/info-classes/${classId}/schedule/status`));
}

/** 导出课表 */
export function exportClassSchedule(classId: number) {
  return unwrap<{ fileId: number; downUrl: string; fileName: string }>(apiClient.get(`${BASE}/info-classes/${classId}/schedule/export`));
}

// ==================== 调课申请审批（S2） ====================

export type ScheduleChangeRequest = {
  id: string | number;
  classId: number;
  scheduleId?: number | null;
  changeType: string;
  changeTypeName?: string;
  beforeSnapshot?: string | null;
  afterPayload?: string | null;
  reason?: string | null;
  status: string;
  statusName?: string;
  applicantPlatformUserId?: number;
  applicantRoleCode?: string | null;
  reviewerUserId?: number | null;
  reviewedAt?: string | null;
  rejectReason?: string | null;
  createdAt?: string | null;
};

export function listScheduleChangeRequests(params?: { classId?: number; status?: string }) {
  return unwrap<ScheduleChangeRequest[]>(
    apiClient.get(`${BASE}/schedule-change-requests`, { params }),
  );
}

export function getScheduleChangeRequest(id: number | string) {
  return unwrap<ScheduleChangeRequest>(apiClient.get(`${BASE}/schedule-change-requests/${id}`));
}

export function approveScheduleChangeRequest(id: number | string, force = false) {
  return unwrap<ScheduleChangeRequest>(
    apiClient.post(`${BASE}/schedule-change-requests/${id}/approve`, null, {
      params: { force },
    }),
  );
}

export function rejectScheduleChangeRequest(
  id: number | string,
  body?: { rejectReason?: string },
) {
  return unwrap<ScheduleChangeRequest>(
    apiClient.post(`${BASE}/schedule-change-requests/${id}/reject`, body ?? {}),
  );
}

// ==================== 跟班作业 / 随堂练（M3） ====================

export type FollowTask = {
  id: string | number;
  classId: number;
  trainInfoId?: number | null;
  scheduleId?: number | null;
  bizDate?: string | null;
  taskType: string;
  taskTypeName?: string;
  title: string;
  description?: string | null;
  deadlineAt?: string | null;
  status: string;
  statusName?: string;
  passScore?: number | null;
  passRule?: string | null;
  showAnalysisAfterSubmit?: boolean;
  publishedAt?: string | null;
  createdAt?: string | null;
  questionCount?: number;
  totalCount?: number | null;
  submittedCount?: number | null;
  gradedCount?: number | null;
  completionRate?: number | null;
};

export type FollowTaskItem = {
  id: string | number;
  taskId: string | number;
  peopleId: number;
  userName?: string | null;
  status: string;
  statusName?: string;
  score?: number | null;
  passed?: boolean | null;
  submittedAt?: string | null;
  gradedAt?: string | null;
  comment?: string | null;
  correctCount?: number | null;
  questionCount?: number | null;
  accuracyRate?: number | null;
  startedAt?: string | null;
  durationSeconds?: number | null;
};

export type FollowTaskAnswerResult = {
  questionId: string;
  sortNumber?: number;
  questionType?: string;
  stem?: string;
  optionsJson?: string | null;
  userAnswerJson?: string | null;
  correctAnswerJson?: string | null;
  analysis?: string | null;
  correct?: boolean | null;
  score?: number | null;
  fullScore?: number | null;
};

export type FollowTaskResult = {
  taskId: string;
  itemId: string;
  status: string;
  score?: number | null;
  passed?: boolean | null;
  showAnalysis?: boolean;
  correctCount?: number | null;
  questionCount?: number | null;
  accuracyRate?: number | null;
  durationSeconds?: number | null;
  answers: FollowTaskAnswerResult[];
};

export type FollowTaskQuestionStat = {
  questionId: string;
  sortNumber?: number;
  questionType?: string;
  stemPreview?: string;
  answeredCount?: number;
  correctCount?: number;
  accuracyRate?: number | null;
};

export type FollowTaskStats = {
  taskId: string;
  questionCount?: number;
  totalCount?: number;
  submittedCount?: number;
  avgScore?: number | null;
  passRate?: number | null;
  avgAccuracyRate?: number | null;
  avgDurationSeconds?: number | null;
  questionStats?: FollowTaskQuestionStat[];
};

export type HomeworkWarning = {
  peopleId?: number;
  userName?: string;
  type?: string;
  message?: string;
  consecutiveMiss?: number;
  avgAccuracyRate?: number;
};

export function listFollowTasks(classId: number) {
  return unwrap<FollowTask[]>(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/tasks`));
}

export function getFollowTask(id: number | string) {
  return unwrap<FollowTask>(apiClient.get(`${BASE}/follow-tasks/${id}`));
}

export function listFollowTaskItems(id: number | string) {
  return unwrap<FollowTaskItem[]>(apiClient.get(`${BASE}/follow-tasks/${id}/items`));
}

export function getFollowTaskItemAnswers(taskId: number | string, itemId: number | string) {
  return unwrap<FollowTaskResult>(
    apiClient.get(`${BASE}/follow-tasks/${taskId}/items/${itemId}/answers`),
  );
}

export function getFollowTaskStats(id: number | string) {
  return unwrap<FollowTaskStats>(apiClient.get(`${BASE}/follow-tasks/${id}/stats`));
}

export async function exportFollowTaskAnswers(id: number | string) {
  const response = await apiClient.get(`${BASE}/follow-tasks/${id}/export-answers`, {
    responseType: 'blob',
  });
  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const filename =
    parseContentDispositionFilename(response) || `作业答卷_${id}.xlsx`;
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
}

export function getHomeworkMetrics(classId: number) {
  return unwrap<{
    classId: number;
    taskCount: number;
    totalItems: number;
    gradedItems: number;
    passedItems: number;
    completionRate: number;
    avgAccuracyRate?: number | null;
    warnings?: HomeworkWarning[];
  }>(apiClient.get(`${BASE}/follow-tasks/classes/${classId}/homework-metrics`));
}

export function syncFollowTaskRoster(id: number | string) {
  return unwrap<{ taskId: string; added: number }>(
    apiClient.post(`${BASE}/follow-tasks/${id}/sync-roster`),
  );
}

// ==================== 信息收集 (sign-up collection) ====================

export function pageSignUpCollection(params: TrainSignUpSelfQuery) {
  return unwrap<PageResponse<TrainSignUpSelf>>(apiClient.get(`${BASE}/sign-ups/collection`, { params }));
}

export function pageSignUps(params: TrainSignUpSelfQuery) {
  return unwrap<PageResponse<TrainSignUpSelf>>(apiClient.get(`${BASE}/sign-ups`, { params }));
}

export function createSignUp(payload: TrainSignUpSelfCreateReq) {
  return unwrap<TrainSignUpSelf>(apiClient.post(`${BASE}/sign-ups`, payload));
}

export function updateSignUp(id: number, payload: TrainSignUpSelfUpdateReq) {
  return unwrap<TrainSignUpSelf>(apiClient.put(`${BASE}/sign-ups/${id}`, payload));
}

export function deleteSignUp(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/sign-ups/${id}`));
}

export interface SignUpQrcodeResult {
  signUpId: number;
  title: string;
  qrcode: string;
  qrcodeDataUrl?: string;
  appQrcode?: string;
  appQrcodeDataUrl?: string;
  registrationPath: string;
  generated: boolean;
}

export function getSignUpQrcode(id: number | string) {
  return unwrap<SignUpQrcodeResult>(
    apiClient.get(`${BASE}/sign-ups/${id}/qrcode`)
  );
}

export function generateSignUpQrcode(id: number | string) {
  return unwrap<SignUpQrcodeResult>(
    apiClient.get(`${BASE}/sign-ups/${id}/qrcode`, { params: { generate: true } })
  );
}

export function pageSignUpPeople(params: TrainSignUpPeopleQuery) {
  return unwrap<PageResponse<TrainSignUpPerson>>(apiClient.get(`${BASE}/sign-up-people`, { params }));
}

export function batchUpdateSignUpPeople(payload: { ids: number[]; field: string; value?: number }) {
  return unwrap<number>(apiClient.put(`${BASE}/sign-up-people/batch`, payload));
}

export async function exportSignUpPeople(signUpId: number | string, ids?: number[]) {
  const response = await apiClient.get(`${BASE}/sign-up-people/export`, {
    params: { signUpId, ids: ids?.length ? ids.join(',') : undefined },
    responseType: 'blob',
  });
  const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `sign-up-people-${signUpId}.xlsx`;
  a.click();
  window.URL.revokeObjectURL(url);
}

export function getSignUpPerson(id: number) {
  return unwrap<TrainSignUpPerson>(apiClient.get(`${BASE}/sign-up-people/${id}`));
}

export function updateSignUpPerson(id: number, payload: Partial<TrainSignUpPerson>) {
  return unwrap<TrainSignUpPerson>(apiClient.put(`${BASE}/sign-up-people/${id}`, payload));
}

export function importSignUpPeople(signUpId: number | string, file: File) {
  const formData = new FormData();
  formData.append('signUpId', String(signUpId));
  formData.append('file', file);
  return unwrap<SignUpPeopleImportResult>(apiClient.post(`${BASE}/sign-up-people/import`, formData));
}

export function downloadSignUpPeopleImportTemplate() {
  return fetchFileBlob(`${BASE}/sign-up-people/import/template`);
}

export function importSignUpStudents(signUpId: number | string, ids: number[]) {
  return unwrap<SignUpPeopleImportResult>(
    apiClient.post(`${BASE}/sign-up-people/import/students`, { signUpId, ids }),
  );
}

export function pageSignUpImportStudents(params: PlatformUserQuery) {
  return unwrap<PageResponse<PlatformUser>>(
    apiClient.get(`${BASE}/sign-up-people/import/students`, { params }),
  );
}

export function startDefaultTemplateExport(payload: {
  signUpId: number;
  ids?: number[];
  status?: number;
}) {
  return unwrap<SignUpDefaultTemplateExportStart>(
    apiClient.post(`${BASE}/sign-up-people/export/default-template`, payload),
  );
}

export function checkDefaultTemplateExport(payload: {
  signUpId: number;
  taskId: number;
  signUpTitle?: string;
}) {
  return unwrap<SignUpDefaultTemplateExportCheck>(
    apiClient.post(`${BASE}/sign-up-people/export/default-template/check`, payload),
  );
}

export function pageSignUpOrcs(params: { signUpId?: number; page?: number; pageSize?: number }) {
  return unwrap<PageResponse<TrainSignUpOrcSelf>>(apiClient.get(`${BASE}/sign-up-orcs`, { params }));
}

export function savePlatformUser(payload: PlatformUserSaveReq) {
  return unwrap<PlatformUser>(apiClient.post(`${BASE}/platform-users`, payload));
}

// ==================== Train Research Org (培训调研企业) ====================
export function pageResearchOrgs(params: TrainResearchOrgQuery) {
  return unwrap<PageResponse<TrainResearchOrg>>(apiClient.get(`${BASE}/research-orgs`, { params }));
}

export function getResearchOrg(id: number) {
  return unwrap<TrainResearchOrg>(apiClient.get(`${BASE}/research-orgs/${id}`));
}

export function createResearchOrg(payload: TrainResearchOrgCreateReq) {
  return unwrap<TrainResearchOrg>(apiClient.post(`${BASE}/research-orgs`, payload));
}

export function updateResearchOrg(id: number, payload: TrainResearchOrgUpdateReq) {
  return unwrap<TrainResearchOrg>(apiClient.put(`${BASE}/research-orgs/${id}`, payload));
}

export function deleteResearchOrg(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/research-orgs/${id}`));
}

// ==================== Research Collection (企业调研人员采集) ====================

export function getResearchCollection(researchOrgId: number) {
  return unwrap<TrainResearchCollection>(
    apiClient.get(`${BASE}/research-orgs/${researchOrgId}/collection`),
  );
}

export function startResearchCollection(researchOrgId: number) {
  return unwrap<TrainResearchCollection>(
    apiClient.post(`${BASE}/research-orgs/${researchOrgId}/collection/start`),
  );
}

export function pageResearchPeople(researchOrgId: number, params: TrainResearchPersonQuery) {
  return unwrap<PageResponse<TrainResearchPerson>>(
    apiClient.get(`${BASE}/research-orgs/${researchOrgId}/collection/people`, { params }),
  );
}

export function createResearchPerson(researchOrgId: number, payload: TrainResearchPersonCreateReq) {
  return unwrap<TrainResearchPerson>(
    apiClient.post(`${BASE}/research-orgs/${researchOrgId}/collection/people`, payload),
  );
}

export function updateResearchPerson(
  researchOrgId: number,
  peopleId: number,
  payload: TrainResearchPersonUpdateReq,
) {
  return unwrap<TrainResearchPerson>(
    apiClient.put(`${BASE}/research-orgs/${researchOrgId}/collection/people/${peopleId}`, payload),
  );
}

export function deleteResearchPerson(researchOrgId: number, peopleId: number) {
  return unwrap<void>(
    apiClient.delete(`${BASE}/research-orgs/${researchOrgId}/collection/people/${peopleId}`),
  );
}

export async function downloadResearchCollectionTemplate(researchOrgId: number, filename: string) {
  const blob = await fetchFileBlob(`${BASE}/research-orgs/${researchOrgId}/collection/export-template`);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename.endsWith('.xlsx') ? filename : `${filename}-认定采集精简版.xlsx`;
  a.click();
  window.URL.revokeObjectURL(url);
}

export function createThreeYearPlanFromResearch(
  researchOrgId: number,
  payload?: { title?: string },
) {
  return unwrap<ThreeYearPlan>(
    apiClient.post(`${BASE}/research-orgs/${researchOrgId}/collection/create-three-year-plan`, payload ?? {}, {
      timeout: 120000,
    }),
  );
}

/** 证书查证：将选中人员的姓名+身份证上传到外部查证接口，回填证书信息 */
export function verifyResearchPeopleCertificate(
  researchOrgId: number,
  peopleIds: number[],
) {
  return unwrap<CertVerifyBatchResultVO>(
    apiClient.post(
      `${BASE}/research-orgs/${researchOrgId}/collection/people/verify-cert`,
      { peopleIds },
      { timeout: 120000 },
    ),
  );
}

/** 方案规划人员认定：按姓名+身份证查证（不落库，前端写回 Excel） */
export function verifyPersonnelCertLookup(people: PersonnelCertLookupPerson[]) {
  return unwrap<PersonnelCertLookupResultVO>(
    apiClient.post(
      `${BASE}/personnel-cert/verify-lookup`,
      { people },
      { timeout: 120000 },
    ),
  );
}

// ==================== Three Year Plan (三年规划) ====================

export function pageThreeYearPlans(params: ThreeYearPlanQuery) {
  return unwrap<PageResponse<ThreeYearPlan>>(apiClient.get(`${BASE}/three-year-plans`, { params }));
}

export function getThreeYearPlan(id: number) {
  return unwrap<ThreeYearPlan>(apiClient.get(`${BASE}/three-year-plans/${id}`));
}

export function getThreeYearPlanDetailBundle(planId: number, params: ThreeYearPlanItemQuery = {}) {
  return unwrap<ThreeYearPlanDetailBundle>(
    apiClient.get(`${BASE}/three-year-plans/${planId}/detail-bundle`, { params, timeout: 120000 }),
  );
}

export function createThreeYearPlan(payload: {
  title: string;
  targetOrgId: number;
  researchOrgId?: number;
  file: File;
}) {
  const formData = new FormData();
  formData.append('title', payload.title);
  formData.append('targetOrgId', String(payload.targetOrgId));
  if (payload.researchOrgId) {
    formData.append('researchOrgId', String(payload.researchOrgId));
  }
  formData.append('file', payload.file);
  return unwrap<ThreeYearPlan>(
    apiClient.post(`${BASE}/three-year-plans`, formData, { timeout: 120000 }),
  );
}

export function refreshThreeYearPlanAnalysis(id: number) {
  return unwrap<ThreeYearPlanAnalysisStatus>(
    apiClient.get(`${BASE}/three-year-plans/${id}/analysis-status`),
  );
}

export function syncThreeYearPlanFromRencai(id: number) {
  return unwrap<ThreeYearPlan>(apiClient.post(`${BASE}/three-year-plans/${id}/sync-from-rencai`));
}

export function prepareThreeYearPlanReview(id: number) {
  return unwrap<ThreeYearPlan>(apiClient.post(`${BASE}/three-year-plans/${id}/prepare-review`));
}

export function updateThreeYearPlanItem(
  planId: number,
  itemId: number,
  payload: Partial<Pick<ThreeYearPlanItem, 'personName' | 'jobName' | 'levelName' | 'eligibleDate' | 'planYear'>>,
) {
  return unwrap<ThreeYearPlanItem>(
    apiClient.put(`${BASE}/three-year-plans/${planId}/items/${itemId}`, payload),
  );
}

export function submitThreeYearPlan(
  id: number,
  payload: { personnelFile: File; schemeFile: File },
) {
  const formData = new FormData();
  formData.append('personnelFile', payload.personnelFile);
  formData.append('schemeFile', payload.schemeFile);
  return unwrap<ThreeYearPlan>(
    apiClient.post(`${BASE}/three-year-plans/${id}/submit`, formData, { timeout: 120000 }),
  );
}

export function companyConfirmThreeYearPlan(id: number) {
  return unwrap<ThreeYearPlan>(apiClient.post(`${BASE}/three-year-plans/${id}/company-confirm`));
}

export function uploadThreeYearPlanEnterpriseReturn(
  id: number,
  payload: { rosterFile?: File; shiftFile?: File },
) {
  const formData = new FormData();
  if (payload.rosterFile) formData.append('rosterFile', payload.rosterFile);
  if (payload.shiftFile) formData.append('shiftFile', payload.shiftFile);
  return unwrap<ThreeYearPlan>(
    apiClient.post(`${BASE}/three-year-plans/${id}/enterprise-return`, formData, { timeout: 120000 }),
  );
}

export function companyRejectThreeYearPlan(id: number, reason: string) {
  return unwrap<ThreeYearPlan>(
    apiClient.post(`${BASE}/three-year-plans/${id}/company-reject`, null, { params: { reason } }),
  );
}

export function pageThreeYearPlanItems(planId: number, params: ThreeYearPlanItemQuery) {
  return unwrap<PageResponse<ThreeYearPlanItem>>(
    apiClient.get(`${BASE}/three-year-plans/${planId}/items`, { params }),
  );
}

export function listThreeYearPlanMatrix(planId: number) {
  return unwrap<ThreeYearPlanMatrix[]>(apiClient.get(`${BASE}/three-year-plans/${planId}/matrix`));
}

export function listThreeYearPlanReviewDistribution(planId: number) {
  return unwrap<ThreeYearPlanDistribution[]>(
    apiClient.get(`${BASE}/three-year-plans/${planId}/review-distribution`),
  );
}

export function deleteThreeYearPlan(id: number) {
  return unwrap<void>(apiClient.delete(`${BASE}/three-year-plans/${id}`));
}

export async function downloadThreeYearPlanFile(
  planId: number,
  kind: 'plan' | 'result' | 'reviewed' | ThreeYearPlanMaterialKind,
) {
  const path = threeYearPlanFileDownloadPath(planId, kind);
  const blob = await fetchFileBlob(path);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = threeYearPlanFileDefaultName(planId, kind);
  a.click();
  window.URL.revokeObjectURL(url);
}

export type ThreeYearPlanMaterialKind =
  | 'submit-personnel'
  | 'submit-scheme'
  | 'enterprise-roster'
  | 'enterprise-shift';

export function threeYearPlanFileDownloadPath(
  planId: number,
  kind: 'plan' | 'result' | 'reviewed' | ThreeYearPlanMaterialKind,
): string {
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

export function threeYearPlanMaterialPreviewUrl(planId: number, kind: ThreeYearPlanMaterialKind): string {
  return threeYearPlanFileDownloadPath(planId, kind);
}

function threeYearPlanFileDefaultName(
  planId: number,
  kind: 'plan' | 'result' | 'reviewed' | ThreeYearPlanMaterialKind,
): string {
  if (kind === 'plan') return `原始三年规划-${planId}.xlsx`;
  if (kind === 'reviewed') return `已审核三年规划-${planId}.xlsx`;
  if (kind === 'submit-personnel') return `企业审核人员名单-${planId}.xlsx`;
  if (kind === 'submit-scheme') return `方案文档-${planId}`;
  if (kind === 'enterprise-roster') return `企业筛选培训名单-${planId}.xlsx`;
  if (kind === 'enterprise-shift') return `企业排班表-${planId}.xlsx`;
  return `原始认定结果-${planId}.zip`;
}

// ==================== Division (分班) ====================

export function pageDivisionPlans(params: {
  page?: number;
  pageSize?: number;
  keyword?: string;
  readyOnly?: boolean;
}) {
  return unwrap<PageResponse<DivisionPlanItem>>(apiClient.get(`${BASE}/division/plans`, { params }));
}

export function getDivisionReady(planId: number) {
  return unwrap<DivisionReadyVO>(apiClient.get(`${BASE}/division/plans/${planId}/ready`));
}

export function ensureDivisionTrainInfo(planId: number, payload: DivisionEnsureTrainInfoReq = {}) {
  return unwrap<DivisionEnsureTrainInfoResult>(
    apiClient.post(`${BASE}/division/plans/${planId}/ensure-train-info`, payload),
  );
}

export function parseDivisionRoster(payload: DivisionRosterParseReq) {
  return unwrap<DivisionRosterParseResult>(
    apiClient.post(`${BASE}/division/roster/parse`, payload, { timeout: 120000 }),
  );
}

export function pageDivisionRoster(params: {
  planId: number;
  trainInfoId?: number;
  assignStatus?: number;
  keyword?: string;
  jobName?: string;
  page?: number;
  pageSize?: number;
}) {
  return unwrap<PageResponse<DivisionRosterItem>>(apiClient.get(`${BASE}/division/roster`, { params }));
}

export function submitDivisionOrgSplit(payload: DivisionOrgSplitReq) {
  return unwrap<{
    trainInfoId: number;
    classCount: number;
    classes: { classId: number; title: string; nums: number }[];
  }>(apiClient.post(`${BASE}/division/org-split`, payload));
}

export function autoPackDivision(payload: DivisionAutoPackReq) {
  return unwrap<DivisionPackTaskResult>(
    apiClient.post(`${BASE}/division/auto-pack`, payload, { timeout: 180000 }),
  );
}

export function manualPackDivision(payload: DivisionManualPackReq) {
  return unwrap<DivisionPackTaskResult>(
    apiClient.post(`${BASE}/division/manual-pack`, payload),
  );
}

export function addPersonToDivisionPackClass(
  taskId: number,
  tempId: string,
  payload: DivisionAddPersonReq,
) {
  return unwrap<DivisionPackTaskResult & { rosterId?: number; createdRoster?: boolean }>(
    apiClient.post(`${BASE}/division/pack-tasks/${taskId}/classes/${encodeURIComponent(tempId)}/add-person`, payload),
  );
}

export async function importMembersToDivisionPackClass(
  taskId: number,
  tempId: string,
  file: File,
) {
  const form = new FormData();
  form.append('file', file);
  return unwrap<{
    added: number;
    skipped: { personName?: string; idCard?: string; reason?: string }[];
    unmatched: { personName?: string; idCard?: string; reason?: string }[];
    task: DivisionPackTaskResult;
  }>(
    apiClient.post(
      `${BASE}/division/pack-tasks/${taskId}/classes/${encodeURIComponent(tempId)}/import-members`,
      form,
      { timeout: 120000, headers: { 'Content-Type': 'multipart/form-data' } },
    ),
  );
}

export function downloadDivisionMemberImportTemplate() {
  return apiClient.get(`${BASE}/division/member-import-template`, {
    responseType: 'blob',
  });
}

export function pageDivisionPackTasks(params: {
  page?: number;
  pageSize?: number;
  status?: DivisionPackTaskStatus | string;
  planId?: number;
}) {
  return unwrap<PageResponse<DivisionPackTaskResult>>(
    apiClient.get(`${BASE}/division/pack-tasks`, { params }),
  );
}

export function getDivisionPackTask(taskId: number) {
  return unwrap<DivisionPackTaskResult>(apiClient.get(`${BASE}/division/pack-tasks/${taskId}`));
}

export function patchDivisionPackTask(taskId: number, payload: DivisionPackPatchReq) {
  return unwrap<DivisionPackTaskResult>(
    apiClient.post(`${BASE}/division/pack-tasks/${taskId}/patch`, payload),
  );
}

export function submitDivisionPackTask(taskId: number) {
  return unwrap<DivisionPackTaskResult>(
    apiClient.post(`${BASE}/division/pack-tasks/${taskId}/submit`),
  );
}

export function companyConfirmDivisionPackTask(taskId: number) {
  return unwrap<DivisionPackTaskResult>(
    apiClient.post(`${BASE}/division/pack-tasks/${taskId}/company-confirm`, undefined, { timeout: 180000 }),
  );
}

export function syncDivisionPackPlatformUsers(taskId: number) {
  return unwrap<{
    taskId: number;
    trainInfoId: number;
    candidates: number;
    linked: number;
    platformUserSync: PlatformUserSyncResult;
  }>(
    apiClient.post(`${BASE}/division/pack-tasks/${taskId}/sync-platform-users`, undefined, { timeout: 180000 }),
  );
}

export function companyRejectDivisionPackTask(taskId: number, reason: string) {
  return unwrap<DivisionPackTaskResult>(
    apiClient.post(`${BASE}/division/pack-tasks/${taskId}/company-reject`, null, {
      params: { reason },
    }),
  );
}

export function getDivisionTrainInfoSummary(trainInfoId: number) {
  return unwrap<DivisionTrainInfoSummary>(
    apiClient.get(`${BASE}/division/train-info/${trainInfoId}/summary`),
  );
}

// ==================== Shift Assign (排课分班 L2) ====================

export function previewShiftAssignAdapter(payload: ShiftAssignGenerateReq) {
  return unwrap<ShiftAssignPreviewResult>(
    apiClient.post(`${BASE}/shift-assign/adapters/preview`, payload, { timeout: 120000 }),
  );
}

export function precheckShiftAssign(payload: ShiftAssignGenerateReq) {
  return unwrap<ShiftAssignPrecheckResult>(
    apiClient.post(`${BASE}/shift-assign/precheck`, payload, { timeout: 180000 }),
  );
}

export function generateShiftAssign(payload: ShiftAssignGenerateReq) {
  return unwrap<ShiftAssignTaskResult>(
    apiClient.post(`${BASE}/shift-assign/generate`, payload, { timeout: 180000 }),
  );
}

export function getShiftAssignTask(taskId: number) {
  return unwrap<ShiftAssignTaskResult>(apiClient.get(`${BASE}/shift-assign/tasks/${taskId}`));
}

export function patchShiftAssignTask(taskId: number, payload: ShiftAssignPatchReq) {
  return unwrap<ShiftAssignTaskResult>(
    apiClient.post(`${BASE}/shift-assign/tasks/${taskId}/patch`, payload),
  );
}

export function bindShiftAssignClass(taskId: number, payload: ShiftAssignBindClassReq) {
  return unwrap<ShiftAssignTaskResult>(
    apiClient.post(`${BASE}/shift-assign/tasks/${taskId}/bind-class`, payload),
  );
}

export function materializeShiftAssign(taskId: number, payload: ShiftAssignMaterializeReq) {
  return unwrap<ShiftAssignMaterializeResult>(
    apiClient.post(`${BASE}/shift-assign/tasks/${taskId}/materialize`, payload, { timeout: 180000 }),
  );
}

export function generateAndMaterializeShiftAssign(payload: ShiftAssignGenerateMaterializeReq) {
  return unwrap<ShiftAssignMaterializeResult & ShiftAssignTaskResult>(
    apiClient.post(`${BASE}/shift-assign/generate-and-materialize`, payload, { timeout: 180000 }),
  );
}

// ==================== 跟班考勤 ====================

export interface TrainAttendanceItem {
  id: number;
  projectId?: number;
  classId?: number;
  userId?: number;
  userName?: string;
  attendanceDate?: string;
  status?: number;
  checkInTime?: string;
  checkOutTime?: string;
  photoPath?: string;
  videoPath?: string;
  remark?: string;
}

export function pageAttendances(params: {
  page?: number;
  pageSize?: number;
  classId?: number;
  userId?: number;
  attendanceDate?: string;
  status?: number;
  keyword?: string;
}) {
  return unwrap<PageResponse<TrainAttendanceItem>>(apiClient.get(`${BASE}/attendances`, { params }));
}

export function createAttendance(payload: {
  projectId?: number;
  classId?: number;
  userId: number;
  userName?: string;
  attendanceDate: string;
  status?: number;
  checkInTime?: string;
  remark?: string;
}) {
  return unwrap<TrainAttendanceItem>(apiClient.post(`${BASE}/attendances`, payload));
}

export function updateAttendance(
  id: number,
  payload: {
    status?: number;
    checkInTime?: string;
    checkOutTime?: string;
    remark?: string;
    photoPath?: string;
    videoPath?: string;
  },
) {
  return unwrap<TrainAttendanceItem>(apiClient.put(`${BASE}/attendances/${id}`, payload));
}

export function signInAttendance(payload: {
  projectId?: number;
  classId?: number;
  userId: number;
  userName?: string;
  attendanceDate: string;
  status?: number;
  checkInTime?: string;
  remark?: string;
}) {
  return unwrap<TrainAttendanceItem>(apiClient.post(`${BASE}/attendances/sign-in`, payload));
}

export interface TrainAttendanceImportFailure {
  rowNum: number;
  name?: string;
  idCard?: string;
  attendanceDate?: string;
  reason: string;
}

export interface TrainAttendanceImportResult {
  totalRows: number;
  parsedRows: number;
  created: number;
  updated: number;
  skipped: number;
  failures: TrainAttendanceImportFailure[];
}

/** 导入人社/培训平台考勤信息列表 Excel（按身份证匹配本班学员，同日多课次合并） */
export async function importClassAttendances(classId: number, file: File) {
  const form = new FormData();
  form.append('file', file);
  return unwrap<TrainAttendanceImportResult>(
    apiClient.post(`${BASE}/classes/${classId}/attendances/import`, form, {
      timeout: 120000,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  );
}

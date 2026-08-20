// ==================== Training Needs (培训需求) ====================
import type { PageResponse } from './api';
export interface TrainNeed {
  id: number;
  orgId?: number;
  orgName?: string;
  trainObject?: string;
  position?: string;
  num?: number;
  workType?: string;
  level?: string;
  target?: string;
  method?: string;
  address?: string;
  trainDate?: string;
  budget?: string;
  teacherRequire?: string;
  needExam?: number;
  needMatch?: number;
  needPaper?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  jsonData?: string;
}

export interface TrainNeedQuery {
  page?: number;
  pageSize?: number;
  orgId?: number;
  keyword?: string;
}

export interface TrainNeedCreateReq {
  orgId?: number;
  orgName?: string;
  trainObject?: string;
  position?: string;
  num?: number;
  workType?: string;
  level?: string;
  target?: string;
  method?: string;
  address?: string;
  trainDate?: string;
  budget?: string;
  teacherRequire?: string;
  needExam?: number;
  needMatch?: number;
  needPaper?: number;
  sortNumber?: number;
  jsonData?: string;
}

export type TrainNeedUpdateReq = Partial<TrainNeedCreateReq>;

// ==================== Training Solutions (培训方案) ====================
export interface TrainSolution {
  id: number;
  trainInfoId?: number;
  companyId?: number;
  companyName?: string;
  title?: string;
  nationPolicy?: string;
  industrySituationAnalysis?: string;
  companyDevelopmentAnalysis?: string;
  researchDataAnalysis?: string;
  projectDevelopmentIdeas?: string;
  mainObjectives?: string;
  mainInitiatives?: string;
  developmentPlan?: string;
  division?: string;
  risk?: string;
  communication?: string;
  checkpoint?: string;
  results?: string;
  teacherIntro?: string;
  requestAnalysis?: string;
  trainContent?: string;
  trainMethod?: string;
  course?: string;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  userId?: number;
  content?: string;
  filePath?: string;
  rejectReason?: string;
}

export interface TrainSolutionQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainSolutionCreateReq {
  trainInfoId?: number;
  companyId?: number;
  companyName?: string;
  title?: string;
  nationPolicy?: string;
  industrySituationAnalysis?: string;
  companyDevelopmentAnalysis?: string;
  researchDataAnalysis?: string;
  projectDevelopmentIdeas?: string;
  mainObjectives?: string;
  mainInitiatives?: string;
  developmentPlan?: string;
  division?: string;
  risk?: string;
  communication?: string;
  checkpoint?: string;
  results?: string;
  teacherIntro?: string;
  requestAnalysis?: string;
  trainContent?: string;
  trainMethod?: string;
  course?: string;
  status?: number;
  sortNumber?: number;
  content?: string;
  filePath?: string;
  rejectReason?: string;
}

export type TrainSolutionUpdateReq = Partial<TrainSolutionCreateReq>;

// ==================== Training Requests (培训请求) ====================
export interface TrainRequest {
  id: number;
  companyName?: string;
  companyId?: number;
  position?: string;
  num?: number;
  type?: string;
  trainDate?: string;
  contact?: string;
  contactPhone?: string;
  isOnPosition?: number;
  studentForm?: string;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainRequestQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainRequestCreateReq {
  companyName?: string;
  companyId?: number;
  position?: string;
  num?: number;
  type?: string;
  trainDate?: string;
  contact?: string;
  contactPhone?: string;
  isOnPosition?: number;
  studentForm?: string;
  sortNumber?: number;
}

export type TrainRequestUpdateReq = Partial<TrainRequestCreateReq>;

// ==================== Training Declares (培训申报) ====================
export interface TrainDeclare {
  id: number;
  trainId?: number;
  title?: string;
  cityId?: number;
  cityName?: string;
  userId?: number;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainDeclareQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainDeclareCreateReq {
  trainId?: number;
  title?: string;
  cityId?: number;
  cityName?: string;
  status?: number;
  sortNumber?: number;
}

export type TrainDeclareUpdateReq = Partial<TrainDeclareCreateReq>;

// ==================== Training People List (培训人员) ====================
export interface TrainPeople {
  id: number;
  trainId?: number;
  trainClassId?: number;
  type?: string;
  userId?: number;
  platformUserId?: number;
  realname?: string;
  phone?: string;
  isLeader?: number;
  isCommittee?: number;
  examScore?: number;
  realScore?: number;
  paper?: string;
  normalScore?: number;
  nationalPaper?: string;
  currentLevel?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  idCard?: string;
  status?: number;
}

export interface TrainPeopleQuery {
  page?: number;
  pageSize?: number;
  trainId?: number;
  trainClassId?: number;
  type?: string;
  keyword?: string;
  /** 认证任务导入时按报名申报工种筛选 */
  applyWorkTypeName?: string;
  /** 认证任务导入时按报名申报等级筛选（0=初级，4=高级技师） */
  applyLevel?: number;
}

export interface TrainPeopleCreateReq {
  trainId?: number;
  trainClassId?: number;
  type?: string;
  userId?: number;
  platformUserId?: number;
  realname?: string;
  phone?: string;
  isLeader?: number;
  isCommittee?: number;
  examScore?: number;
  realScore?: number;
  paper?: string;
  normalScore?: number;
  nationalPaper?: string;
  currentLevel?: number;
  sortNumber?: number;
  idCard?: string;
}

export type TrainPeopleUpdateReq = Partial<TrainPeopleCreateReq>;

export interface SignupAuditReq {
  ids: number[];
  status: number; // 1=通过, 2=拒绝
}

// ==================== Training Class List (班级管理) ====================
export interface TrainClass {
  id: number;
  trainId?: number;
  userId?: number;
  isLeader?: number;
  isCommittee?: number;
  className?: string;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainClassQuery {
  page?: number;
  pageSize?: number;
  trainId?: number;
  keyword?: string;
}

export interface TrainClassCreateReq {
  trainId?: number;
  userId?: number;
  isLeader?: number;
  isCommittee?: number;
  className?: string;
  sortNumber?: number;
}

export type TrainClassUpdateReq = Partial<TrainClassCreateReq>;

export interface AssignClassReq {
  classId: number;
  userIds: number[];
}

// ==================== Training Courses (课程管理) ====================
export interface TrainCourse {
  id: number;
  trainId?: number;
  teacherIdCard?: string;
  teacherName?: string;
  teacherUserId?: number;
  type?: number;
  courseDate?: string;
  title?: string;
  startTime?: string;
  endTime?: string;
  theoryTime?: number;
  operationTime?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainCourseQuery {
  page?: number;
  pageSize?: number;
  trainId?: number;
  type?: number;
  keyword?: string;
}

export interface TrainCourseCreateReq {
  trainId?: number;
  teacherIdCard?: string;
  teacherName?: string;
  teacherUserId?: number;
  type?: number;
  courseDate?: string;
  title?: string;
  startTime?: string;
  endTime?: string;
  theoryTime?: number;
  operationTime?: number;
  sortNumber?: number;
}

export type TrainCourseUpdateReq = Partial<TrainCourseCreateReq>;

// ==================== Course Kejian (课件管理) ====================
export interface CourseKejian {
  id: number;
  sno: string;
  title: string;
  keshi?: number;
  cover?: string;
  type?: number;
  price?: number;
  description?: string;
  content?: string;
  filePath?: string;
  workTypeId?: number;
  tag?: number;
  jigouId?: number;
  ownerId?: number;
  addtime?: number;
}

export interface CourseKejianQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface CourseKejianCreateReq {
  sno: string;
  title: string;
  keshi?: number;
  cover?: string;
  type?: number;
  price?: number;
  description?: string;
  content?: string;
  filePath?: string;
  workTypeId?: number;
  tag?: number;
  jigouId?: number;
  ownerId?: number;
}

export type CourseKejianUpdateReq = Partial<CourseKejianCreateReq>;

// ==================== Training Resources (资源管理) ====================
export interface TrainResource {
  id: number;
  workTypeId?: number;
  title?: string;
  type?: string;
  path?: string;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainResourceQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainResourceCreateReq {
  workTypeId?: number;
  title?: string;
  type?: string;
  path?: string;
  userId?: number;
  sortNumber?: number;
}

export type TrainResourceUpdateReq = Partial<TrainResourceCreateReq>;

// ==================== Training Task List (任务知识管理) ====================
export interface TrainTask {
  id: number;
  workTypeId?: number;
  courseId?: number;
  knowledgeId?: number;
  title: string;
  type?: string;
  content?: string;
  getLearnTime?: number;
  minMinute?: number;
  isRequired?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  score?: number;
  getPoint?: number;
  checkStatus?: number;
  finalCheckStatus?: number;
  checkTime?: string;
  checkUserId?: number;
  checkUsername?: string;
  finalCheckTime?: string;
  finalCheckUserId?: number;
  finalCheckUsername?: string;
}

export interface TrainTaskQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  workTypeId?: number;
  courseId?: number;
  knowledgeId?: number;
  type?: string;
  checkStatus?: number;
}

export interface TrainTaskCreateReq {
  workTypeId?: number;
  courseId?: number;
  knowledgeId?: number;
  title: string;
  type?: string;
  content?: string;
  getLearnTime?: number;
  minMinute?: number;
  isRequired?: number;
  userId?: number;
  sortNumber?: number;
  score?: number;
  getPoint?: number;
  checkStatus?: number;
  finalCheckStatus?: number;
}

export type TrainTaskUpdateReq = Partial<TrainTaskCreateReq>;

// ==================== Training Task Info (培训配置任务) ====================
export interface TrainTaskInfo {
  id: number;
  trainId?: number;
  cover?: string;
  courseId?: number;
  trainTaskId?: number;
  knowledgeId?: number;
  intro?: string;
  isRequired?: number;
  score?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainTaskInfoQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainTaskInfoCreateReq {
  trainId?: number;
  cover?: string;
  courseId?: number;
  trainTaskId?: number;
  knowledgeId?: number;
  intro?: string;
  isRequired?: number;
  score?: number;
  userId?: number;
  sortNumber?: number;
}

export type TrainTaskInfoUpdateReq = Partial<TrainTaskInfoCreateReq>;

// ==================== Training Exam List (考试管理) ====================
export interface TrainExam {
  id: number;
  title?: string;
  level?: string;
  trainId?: number;
  orgId?: number;
  orgName?: string;
  type?: number;
  source?: number;
  invigilateUserId?: number;
  invigilateUsername?: string;
  evaluateUserId?: number;
  evaluateUsername?: string;
  watcherUserId?: number;
  watcherUsername?: string;
  staffUserId?: number;
  staffUsername?: string;
  propositionUserId?: number;
  propositionUsername?: string;
  markingUserId?: number;
  markingUsername?: string;
  examId?: number;
  examDate?: string;
  examDateMode?: number;
  examDateList?: string;
  examTime?: number;
  needFinishAdventure?: number;
  needFinishNormal?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  jigouId?: number;
  createTime?: string;
  /** 关联工种（含 workTypeId / level） */
  workTypeLists?: TrainExamWorkTypeItem[];
  /** 关联课程 ID 列表 */
  courseIdList?: number[];
}

export interface TrainExamQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  trainId?: number;
  type?: number;
  source?: number;
  orgId?: number;
}

/** 考试计划关联工种 */
export interface TrainExamWorkTypeItem {
  id: number;
  examId?: number;
  workTypeId?: number;
  courseId?: number;
  level?: string;
}

/** 考试计划考生 */
export interface TrainExamPeople {
  id: number;
  examId?: number;
  platformUserId?: number;
  workTypeId?: number;
  workTypeLevel?: string;
  realname?: string;
  idCardNum?: string;
  phone?: string;
  education?: string;
  roomId?: number;
  inWhiteList?: number;
  normalScore?: number;
  score?: number;
  totalScore?: number;
  learnTime?: number;
  learnUseTime?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainExamPeopleQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  examId?: number;
}

export interface TrainExamCreateReq {
  title?: string;
  level?: string;
  trainId?: number;
  orgId?: number;
  orgName?: string;
  type?: number;
  source?: number;
  invigilateUserId?: number;
  invigilateUsername?: string;
  evaluateUserId?: number;
  evaluateUsername?: string;
  watcherUserId?: number;
  watcherUsername?: string;
  staffUserId?: number;
  staffUsername?: string;
  propositionUserId?: number;
  propositionUsername?: string;
  markingUserId?: number;
  markingUsername?: string;
  examId?: number;
  examDate?: string;
  examDateMode?: number;
  examDateList?: string;
  examTime?: number;
  needFinishAdventure?: number;
  needFinishNormal?: number;
  jigouId?: number;
  sortNumber?: number;
  courseIdList?: number[];
}

export type TrainExamUpdateReq = Partial<TrainExamCreateReq>;

// ==================== Exam Results (考试结果) ====================
export interface ExamResult {
  id: number;
  type: string;
  examPlanId?: number;
  paperId?: number;
  detailId?: number;
  getScore?: number;
  examType?: number;
  examId?: number;
  userId?: number;
  platformUserId?: number;
  userAnswer: string;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface ExamResultQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface ExamResultCreateReq {
  type: string;
  examPlanId?: number;
  paperId?: number;
  detailId?: number;
  getScore?: number;
  examType?: number;
  examId?: number;
  userId?: number;
  platformUserId?: number;
  userAnswer: string;
  sortNumber?: number;
}

export type ExamResultUpdateReq = Partial<ExamResultCreateReq>;

// ==================== Training Protocol (协议管理) ====================
export interface TrainProtocol {
  id: number;
  projectId?: number;
  title?: string;
  type?: string;
  content?: string;
  version?: number;
  status?: number;
  signedAt?: string;
  signerId?: number;
  approverId?: number;
  rejectReason?: string;
  sortNumber?: number;
  tenantId?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainProtocolQuery {
  page?: number;
  pageSize?: number;
  projectId?: number;
  type?: string;
  status?: number;
  keyword?: string;
}

export interface TrainProtocolCreateReq {
  projectId?: number;
  title?: string;
  type?: string;
  content?: string;
  version?: number;
  status?: number;
  signedAt?: string;
  signerId?: number;
  approverId?: number;
  rejectReason?: string;
  sortNumber?: number;
}

export type TrainProtocolUpdateReq = Partial<TrainProtocolCreateReq>;

// ==================== Training Project (项目管理) ====================
export interface TrainProject {
  id: number;
  projectName?: string;
  companyId?: number;
  companyName?: string;
  managerId?: number;
  managerName?: string;
  planStart?: string;
  planEnd?: string;
  currentStage?: string;
  progress?: number;
  status?: number;
  description?: string;
  sortNumber?: number;
  tenantId?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainProjectQuery {
  page?: number;
  pageSize?: number;
  companyId?: number;
  status?: number;
  keyword?: string;
}

export interface TrainProjectCreateReq {
  projectName?: string;
  companyId?: number;
  companyName?: string;
  managerId?: number;
  managerName?: string;
  planStart?: string;
  planEnd?: string;
  currentStage?: string;
  progress?: number;
  status?: number;
  description?: string;
  sortNumber?: number;
}

export type TrainProjectUpdateReq = Partial<TrainProjectCreateReq>;

// ==================== Training Project Table (十大管理表) ====================
export interface TrainProjectTable {
  id: number;
  projectId?: number;
  tableType?: number;
  tableName?: string;
  content?: string;
  version?: number;
  status?: number;
  ownerId?: number;
  ownerName?: string;
  sortNumber?: number;
  tenantId?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainProjectTableQuery {
  page?: number;
  pageSize?: number;
  projectId?: number;
  tableType?: number;
  status?: number;
  keyword?: string;
}

export interface TrainProjectTableCreateReq {
  projectId?: number;
  tableType?: number;
  tableName?: string;
  content?: string;
  version?: number;
  status?: number;
  ownerId?: number;
  ownerName?: string;
  sortNumber?: number;
}

export type TrainProjectTableUpdateReq = Partial<TrainProjectTableCreateReq>;

// ==================== Dashboard (仪表盘) ====================
export interface DashboardSummaryKpi {
  planExecutionRate: number;
  materialSubmissionRate: number;
  certificationRate: number;
  subsidyApprovalRate: number;
  trainingCompletionRate: number;
  totalProjects: number;
  totalStudents: number;
  totalCertificates: number;
  totalBudgets: number;
}

export interface DashboardMonthlyTrend {
  month: string;
  count: number;
}

export interface DashboardAlertItem {
  indicator: string;
  currentValue: string;
  threshold: string;
  trend: string;
  status: string;
  suggestion: string;
}

export interface TrainDashboard {
  summary: DashboardSummaryKpi;
  monthlyTrends: DashboardMonthlyTrend[];
  alerts: DashboardAlertItem[];
}

export interface DashboardMilestoneItem {
  projectId: number;
  projectName: string;
  currentStage: string;
  progress: number;
  companyName: string;
}

export type DashboardStageDistribution = Record<string, number>;

// ==================== Train Talent Needs (人才数据管理) ====================
export interface TrainTalentNeed {
  id: number;
  title?: string;
  orgId?: number;
  orgName?: string;
  job?: string;
  workType?: string;
  major?: string;
  edu?: string;
  gender?: number;
  num?: number;
  address?: string;
  workYear?: string;
  inPositionDate?: string;
  paperRequire?: string;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  jsonData?: string;
  duty?: string;
  schoolNeed?: string;
}

export interface TrainTalentNeedQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainTalentNeedCreateReq {
  title?: string;
  orgId?: number;
  orgName?: string;
  job?: string;
  workType?: string;
  major?: string;
  edu?: string;
  gender?: number;
  num?: number;
  address?: string;
  workYear?: string;
  inPositionDate?: string;
  paperRequire?: string;
  sortNumber?: number;
  jsonData?: string;
  duty?: string;
  schoolNeed?: string;
}

export type TrainTalentNeedUpdateReq = Partial<TrainTalentNeedCreateReq>;

// ==================== Platform User (平台用户) ====================
export interface PlatformUser {
  id: number;
  realname?: string;
  idcard?: string;
  phone?: string;
  password?: string;
  stat?: string;
  addtime?: number;
  addtimeCn?: string;
  logintime?: number;
  orgId?: number;
  companyId?: number;
  companyName?: string;
  wxopenid?: string;
  expertId?: number;
  task?: number;
}

export interface PlatformUserQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface PlatformUserCreateReq {
  realname?: string;
  idcard?: string;
  phone?: string;
  password?: string;
  stat?: string;
  orgId?: number;
  companyId?: number;
  wxopenid?: string;
  expertId?: number;
  task?: number;
}

export type PlatformUserUpdateReq = Partial<PlatformUserCreateReq>;

// ==================== Sign Up Collection (信息收集) ====================
export interface TrainSignUpSelfFile {
  fileUrl: string;
  fileName?: string;
  fileType?: string;
}

export interface TrainSignUpSelf {
  id: number;
  title?: string;
  cover?: string;
  trainTime?: string;
  examTime?: string;
  orgId?: number;
  orgName?: string;
  trainInfoId?: number;
  trainClassId?: number;
  collectionSource?: string;
  sortNumber?: number;
  peopleCount?: number;
  wxMiniQrcode?: string;
  files?: TrainSignUpSelfFile[];
  createTime?: string;
  updateTime?: string;
}

export interface TrainSignUpSelfQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  orgId?: number;
  trainInfoId?: number;
  classId?: number;
  collectionSource?: string;
}

export interface TrainSignUpSelfCreateReq {
  title: string;
  cover?: string;
  trainTime: string;
  examTime: string;
  orgId: number;
  trainInfoId?: number;
  sortNumber?: number;
}

export type TrainSignUpSelfUpdateReq = Partial<TrainSignUpSelfCreateReq>;

export interface SignUpPeopleImportResult {
  inserted: number;
  skipped: number;
  invalid: number;
  filePath?: string;
  errors?: string[];
  platformUserSync?: PlatformUserSyncResult;
}

export interface SignUpDefaultTemplateExportStart {
  taskId: number;
  jindu: string;
}

export interface SignUpDefaultTemplateExportCheck {
  url?: string;
  jindu?: string;
  error?: number;
  msg?: string;
}

export interface TrainSignUpPerson {
  id: number;
  signUpId?: number;
  platformUserId?: number;
  userId?: number;
  name?: string;
  phone?: string;
  idCardNumber?: string;
  age?: number;
  gender?: string;
  nation?: string;
  political?: string;
  houseType?: string;
  address?: string;
  education?: string;
  photo?: string;
  idCardFront?: string;
  idCardBack?: string;
  idCardHeader?: string;
  idCardHeader2?: string;
  idcardFront?: string;
  idcardBack?: string;
  idcardHeader?: string;
  school?: string;
  major?: string;
  schoolPaperHas?: number;
  schoolNo?: string;
  inSchoolDate?: string;
  graduateDate?: string;
  schoolPaper?: string;
  workTime?: string;
  titleType?: string;
  titlePaper?: string;
  companyId?: number;
  workDateStart?: string;
  workDateEnd?: string;
  workTypePaperHas?: number;
  workTypePaper?: string;
  workTypeId?: number;
  workTypeName?: string;
  level?: number;
  paperNo?: string;
  reqWorkTypeId?: number;
  reqWorkTypeName?: string;
  reqLevel?: number;
  declarationCondition?: string;
  workExp?: string;
  workExpList?: Record<string, unknown>[];
  jog?: string;
  contactFirstPage?: string;
  contactExpirePage?: string;
  contactSignPage?: string;
  contractFirst?: string;
  contractExpire?: string;
  contractSign?: string;
  contract?: string;
  certificate?: string;
  workTypePaperName?: string;
  workTypePaperLevel?: number;
  workTypePaperNo?: string;
  status?: number;
  statusCn?: string;
  companyStatus?: number;
  companyStatusCn?: string;
  hasPay?: number;
  payCn?: string;
  createTime?: string;
  updateTime?: string;
}

export interface TrainSignUpOrcSelf {
  id: number;
  signUpId?: number;
  platformUserId?: number;
  orcData?: string;
  status?: number;
  failReason?: string;
  createTime?: string;
}

export interface TrainSignUpPeopleQuery {
  page?: number;
  pageSize?: number;
  signUpId?: number | string;
  keyword?: string;
  status?: number;
}

export interface PlatformUserSaveReq extends PlatformUserCreateReq {
  id?: number;
}

// ==================== Teacher Bank (师资管理) ====================
export interface TrainTeacherBank {
  id: number;
  name?: string;
  phone?: string;
  idCard?: string;
  teacherLicense?: string;
  engineerLicense?: string;
  levelLicense?: string;
  eduLicense?: string;
  graduatePaper?: string;
  company?: string;
  address?: string;
  idCardNumber?: string;
  startDate?: string;
  endDate?: string;
  birthday?: string;
  gender?: number;
  nation?: string;
  education?: string;
  major?: string;
  skillName?: string;
  qualificationLevel?: string;
  professionalTitle?: string;
  certText?: string;
  teacherQualNo?: string;
  titleCertNo?: string;
  skillCertNo?: string;
  idCardFrontFileId?: number | null;
  idCardBackFileId?: number | null;
  graduateFileId?: number | null;
  educationFileId?: number | null;
  teacherQualFileId?: number | null;
  titleCertFileId?: number | null;
  skillCertFileId?: number | null;
  legacyId?: number | null;
  source?: string | null;
  status?: number;
  tenantId?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
  sortNumber?: number;
  provisional?: boolean;
  fileReadyCount?: number;
  files?: TrainTeacherBankFile[];
}

export interface TrainTeacherBankFile {
  id: number;
  fileType: string;
  fileId: number;
  legacyUrl?: string | null;
  certNo?: string | null;
  sortNo?: number;
  remark?: string | null;
}

export interface TrainTeacherBankImportResult {
  total: number;
  success: number;
  created: number;
  updated: number;
  provisional: number;
  failed?: Array<{ row?: number; name?: string; reason?: string }>;
  warnings?: Array<{ row?: number; name?: string; reason?: string }>;
}

export interface TrainTeacherBankQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainTeacherBankCreateReq {
  name?: string;
  phone?: string;
  idCard?: string;
  teacherLicense?: string;
  engineerLicense?: string;
  levelLicense?: string;
  eduLicense?: string;
  graduatePaper?: string;
  company?: string;
  address?: string;
  idCardNumber?: string;
  startDate?: string;
  endDate?: string;
  birthday?: string;
  gender?: number;
  nation?: string;
  education?: string;
  major?: string;
  skillName?: string;
  qualificationLevel?: string;
  professionalTitle?: string;
  certText?: string;
  teacherQualNo?: string;
  titleCertNo?: string;
  skillCertNo?: string;
  status?: number;
  sortNumber?: number;
}

export type TrainTeacherBankUpdateReq = Partial<TrainTeacherBankCreateReq>;

// ==================== User Info (用户信息) ====================
export interface TrainUserInfo {
  id: number;
  userId?: number;
  companyId?: number;
  personType?: string;
  personSupplementary?: string;
  houseType?: string;
  nationality?: string;
  education?: string;
  jobIntention?: string;
  isPartyMember?: number;
  address?: string;
  idCardFront?: string;
  idCardBack?: string;
  photo?: string;
  eduPhoto?: string;
  workContract?: string;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainUserInfoQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainUserInfoCreateReq {
  userId?: number;
  companyId?: number;
  personType?: string;
  personSupplementary?: string;
  houseType?: string;
  nationality?: string;
  education?: string;
  jobIntention?: string;
  isPartyMember?: number;
  address?: string;
  idCardFront?: string;
  idCardBack?: string;
  photo?: string;
  eduPhoto?: string;
  workContract?: string;
  sortNumber?: number;
}

export type TrainUserInfoUpdateReq = Partial<TrainUserInfoCreateReq>;

// ==================== Budget (预算费用管理) ====================
export interface TrainBudget {
  id: number;
  trainId?: number;
  province?: string;
  city?: string;
  region?: string;
  orgId?: number;
  orgName?: string;
  trainDate?: string;
  trainWorkTypeId?: number;
  trainWorkTypeName?: string;
  trainLevel?: string;
  bonusStandard?: string;
  expectIncome?: number;
  actualIncome?: number;
  schoolManageFee?: number;
  manageFee?: number;
  researchFee?: number;
  companyRequireCommission?: number;
  agentRequireCommission?: number;
  teacherCourseFee?: number;
  teacherFee?: number;
  staffFee?: number;
  staffTravelFee?: number;
  trainFee?: number;
  pointRewards?: number;
  businessFee?: number;
  peopleNum?: number;
  chargeFee?: string;
  serviceFee?: number;
  reviewFee?: number;
  maintainFee?: number;
  makeupPeopleNum?: number;
  theoryMakeupPeopleNum?: number;
  operationMakeupPeopleNum?: number;
  staffMakeupTrainFee?: number;
  reviewMakeupTrainFee?: number;
  profit?: number;
  comments?: string;
  status?: number;
  userId?: number;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
  sortNumber?: number;
}

export interface TrainBudgetQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}

export interface TrainBudgetCreateReq {
  trainId?: number;
  province?: string;
  city?: string;
  region?: string;
  orgId?: number;
  orgName?: string;
  trainDate?: string;
  trainWorkTypeId?: number;
  trainWorkTypeName?: string;
  trainLevel?: string;
  bonusStandard?: string;
  expectIncome?: number;
  actualIncome?: number;
  schoolManageFee?: number;
  manageFee?: number;
  researchFee?: number;
  companyRequireCommission?: number;
  agentRequireCommission?: number;
  teacherCourseFee?: number;
  teacherFee?: number;
  staffFee?: number;
  staffTravelFee?: number;
  trainFee?: number;
  pointRewards?: number;
  businessFee?: number;
  peopleNum?: number;
  chargeFee?: string;
  serviceFee?: number;
  reviewFee?: number;
  maintainFee?: number;
  makeupPeopleNum?: number;
  theoryMakeupPeopleNum?: number;
  operationMakeupPeopleNum?: number;
  staffMakeupTrainFee?: number;
  reviewMakeupTrainFee?: number;
  profit?: number;
  comments?: string;
  status?: number;
  sortNumber?: number;
}

export type TrainBudgetUpdateReq = Partial<TrainBudgetCreateReq>;

// ==================== File List (文件管理) ====================
export interface TrainFileList {
  id: number;
  tableName?: string;
  pk?: string;
  title?: string;
  userId?: number;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainFileListQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainFileListCreateReq {
  tableName?: string;
  pk?: string;
  title?: string;
  userId?: number;
  status?: number;
  sortNumber?: number;
}

export type TrainFileListUpdateReq = Partial<TrainFileListCreateReq>;

// ==================== File Detail (文件明细) ====================
export interface TrainFileDetail {
  id: number;
  fileId?: number;
  title?: string;
  path?: string;
  userId?: number;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainFileDetailQuery {
  page?: number;
  pageSize?: number;
  fileId?: number;
  keyword?: string;
}

export interface TrainFileDetailCreateReq {
  fileId?: number;
  title?: string;
  path?: string;
  userId?: number;
  status?: number;
  sortNumber?: number;
}

export type TrainFileDetailUpdateReq = Partial<TrainFileDetailCreateReq>;

// ==================== Info List (培训信息) ====================
export interface TrainInfoList {
  id: number;
  title?: string;
  cover?: string;
  intro?: string;
  groupType?: number;
  masterType?: number;
  type?: number;
  cate?: string;
  companyId?: number;
  companyName?: string;
  companyAddress?: string;
  workTypeId?: number;
  workTypeTitle?: string;
  workTypeLevel?: number;
  courseId?: number;
  num?: number;
  position?: string;
  courseNum?: number;
  headConfirm?: number;
  userId?: number;
  companyHasConfirm?: number;
  companyConfirmTime?: string;
  companyConfirmUserId?: number;
  companyConfirmUsername?: string;
  passStatus?: number;
  checkUserId?: number;
  checkUsername?: string;
  isSortedLearn?: number;
  maxNormalScore?: number;
  courseFile?: string;
  startDate?: string;
  endDate?: string;
  trainCompanyName?: string;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  point?: number;
  album?: string;
  summary?: string;
  provinceId?: number;
  provinceName?: string;
  cityId?: number;
  cityName?: string;
  abilityScore?: number;
  regionId?: number;
  regionName?: string;
  inChargeUserId?: number;
  inChargeUsername?: string;
  files?: string;
  traindemandId?: number;
}

export interface TrainInfoListQuery {
  page?: number;
  limit?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
  companyId?: number;
  workTypeId?: number;
  groupType?: number;
  groupTypeNe?: number;
  cate?: string;
  passStatus?: number;
  headConfirm?: number;
  /** 对齐 PHP /yundemand/train-info-list：不按 headConfirm/passStatus 过滤 */
  yundemandList?: boolean;
}

export interface TrainInfoListPageResponse {
  list: TrainInfoListItem[];
  total: number;
  limit: number;
}

/** 培训信息列表项（page 接口返回的 snake_case 字段） */
export interface TrainInfoListItem {
  id: number;
  title: string;
  cate?: string;
  group_type?: number;
  company_id?: number;
  company_name?: string;
  train_company_name?: string;
  work_type_title?: string;
  num?: number;
  position?: string;
  course_num?: number;
  head_confirm?: number;
  company_has_confirm?: number;
  status?: number;
  pass_status?: number;
  create_time?: string;
  work_type_level?: number;
}

export interface TrainInfoListEditReq {
  courseNum?: number;
  position?: string;
  status?: number;
}

export interface TrainInfoListCreateReq {
  title?: string;
  cover?: string;
  intro?: string;
  groupType?: number;
  masterType?: number;
  type?: number;
  cate?: string;
  companyId?: number;
  companyName?: string;
  companyAddress?: string;
  workTypeId?: number;
  workTypeLevel?: number;
  courseId?: number;
  num?: number;
  position?: string;
  courseNum?: number;
  headConfirm?: number;
  companyHasConfirm?: number;
  passStatus?: number;
  isSortedLearn?: number;
  maxNormalScore?: number;
  courseFile?: string;
  startDate?: string;
  endDate?: string;
  trainCompanyName?: string;
  status?: number;
  sortNumber?: number;
  point?: number;
  album?: string;
  summary?: string;
  provinceId?: number;
  provinceName?: string;
  cityId?: number;
  cityName?: string;
  abilityScore?: number;
  regionId?: number;
  regionName?: string;
  inChargeUserId?: number;
  inChargeUsername?: string;
  files?: string;
  traindemandId?: number;
}

export type TrainInfoListUpdateReq = Partial<TrainInfoListCreateReq>;

// ==================== Info Class (培训班级信息) ====================
export interface TrainInfoClass {
  id: number;
  trainInfoId?: number;
  title?: string;
  nums?: number;
  companyId?: number;
  workTypeId?: number;
  dateStart?: string;
  dateEnd?: string;
  addtime?: number;
  fileTemps?: string;
  kcbiaoFile?: string;
}

export interface TrainInfoClassQuery {
  page?: number;
  pageSize?: number;
  trainInfoId?: number;
  keyword?: string;
}

export interface TrainInfoClassCreateReq {
  id?: number | string;
  trainInfoId?: number | string;
  title?: string;
  nums?: number;
  companyId?: number;
  workTypeId?: number;
  workTypeLevel?: number;
  dateStart?: string;
  dateEnd?: string;
  fileTemps?: string;
  kcbiaoFile?: string;
}

export type TrainInfoClassUpdateReq = Partial<TrainInfoClassCreateReq>;

// ==================== Task Info Detail (任务培训配置详情) ====================
export interface TrainTaskInfoDetail {
  id: number;
  trainId?: number;
  taskListId?: number;
  workTypeId?: number;
  courseId?: number;
  knowledgeId?: number;
  title?: string;
  type?: string;
  content?: string;
  getLearnTime?: number;
  minMinute?: number;
  isRequired?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  score?: number;
}

export interface TrainTaskInfoDetailQuery {
  page?: number;
  pageSize?: number;
  trainId?: number;
  keyword?: string;
}

export interface TrainTaskInfoDetailCreateReq {
  trainId?: number;
  taskListId?: number;
  workTypeId?: number;
  courseId?: number;
  knowledgeId?: number;
  title?: string;
  type?: string;
  content?: string;
  getLearnTime?: number;
  minMinute?: number;
  isRequired?: number;
  sortNumber?: number;
  score?: number;
}

export type TrainTaskInfoDetailUpdateReq = Partial<TrainTaskInfoDetailCreateReq>;

// ==================== Task Log (任务日志) ====================
export interface TrainTaskLog {
  id: number;
  trainClassId?: number;
  ckId?: number;
  date?: string;
  addtime?: number;
  status?: number;
  times?: string;
}

export interface TrainTaskLogQuery {
  page?: number;
  pageSize?: number;
  trainClassId?: number;
}

export interface TrainTaskLogCreateReq {
  trainClassId?: number;
  ckId?: number;
  date?: string;
  status?: number;
  times?: string;
}

export type TrainTaskLogUpdateReq = Partial<TrainTaskLogCreateReq>;

// ==================== User Learn Log (用户学习记录) ====================
export interface TrainUserLearnLog {
  id: number;
  trainId?: number;
  matchId?: number;
  trainTaskId?: number;
  learnTime?: number;
  score?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainUserLearnLogQuery {
  page?: number;
  pageSize?: number;
  userId?: number;
  trainId?: number;
}

export interface TrainUserLearnLogCreateReq {
  trainId?: number;
  matchId?: number;
  trainTaskId?: number;
  learnTime?: number;
  score?: number;
  userId?: number;
  sortNumber?: number;
}

export type TrainUserLearnLogUpdateReq = Partial<TrainUserLearnLogCreateReq>;

// ==================== User Course Plan (用户选课计划) ====================
export interface TrainUserCoursePlan {
  id: number;
  userId?: number;
  platformUserId?: number;
  realname?: string;
  addFrom?: number;
  courseId?: number;
  date?: string;
  type?: number;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainUserCoursePlanQuery {
  page?: number;
  pageSize?: number;
  userId?: number;
  courseId?: number;
}

export interface TrainUserCoursePlanCreateReq {
  userId?: number;
  platformUserId?: number;
  realname?: string;
  addFrom?: number;
  courseId?: number;
  date?: string;
  type?: number;
  status?: number;
  sortNumber?: number;
}

export type TrainUserCoursePlanUpdateReq = Partial<TrainUserCoursePlanCreateReq>;

// ==================== User Course Paper (用户课程证书) ====================
export interface TrainUserCoursePaper {
  id: number;
  userId?: number;
  platformUserId?: number;
  courseId?: number;
  workTypeId?: number;
  getYear?: string;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainUserCoursePaperQuery {
  page?: number;
  pageSize?: number;
  userId?: number;
  courseId?: number;
}

export interface TrainUserCoursePaperCreateReq {
  userId?: number;
  platformUserId?: number;
  courseId?: number;
  workTypeId?: number;
  getYear?: string;
  status?: number;
  sortNumber?: number;
}

export type TrainUserCoursePaperUpdateReq = Partial<TrainUserCoursePaperCreateReq>;

// ==================== User Paper (用户证书) ====================
export interface TrainUserPaper {
  id: number;
  url?: string;
  title?: string;
  type?: string;
  userId?: number;
  platformUserId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  pk?: number;
}

export interface TrainUserPaperQuery {
  page?: number;
  pageSize?: number;
  userId?: number;
  keyword?: string;
}

export interface TrainUserPaperCreateReq {
  url?: string;
  title?: string;
  type?: string;
  userId?: number;
  platformUserId?: number;
  sortNumber?: number;
  pk?: number;
}

export type TrainUserPaperUpdateReq = Partial<TrainUserPaperCreateReq>;

// ==================== User Patent (用户专利) ====================
export interface TrainUserPatent {
  id: number;
  title?: string;
  paper?: string;
  userId?: number;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainUserPatentQuery {
  page?: number;
  pageSize?: number;
  userId?: number;
  keyword?: string;
}

export interface TrainUserPatentCreateReq {
  title?: string;
  paper?: string;
  userId?: number;
}

export type TrainUserPatentUpdateReq = Partial<TrainUserPatentCreateReq>;

// ==================== User Exam Log (用户考试日志) ====================
export interface TrainUserExamLog {
  id: number;
  trainId?: number;
  knowledgeId?: number;
  userId?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainUserExamLogQuery {
  page?: number;
  pageSize?: number;
  userId?: number;
  trainId?: number;
}

export interface TrainUserExamLogCreateReq {
  trainId?: number;
  knowledgeId?: number;
  userId?: number;
  sortNumber?: number;
}

export type TrainUserExamLogUpdateReq = Partial<TrainUserExamLogCreateReq>;

// ==================== User Ability (用户能力) ====================
export interface TrainUserAbility {
  id: number;
  trainId?: number;
  courseId?: number;
  knowledgeId?: number;
  result?: string;
  configId?: number;
  score?: number;
  currentScore?: number;
  userId?: number;
  platformUserId?: number;
  comments?: string;
  status?: number;
  sortNumber?: number;
  tenantId?: number;
  createTime?: string;
}

export interface TrainUserAbilityQuery {
  page?: number;
  pageSize?: number;
  userId?: number;
  trainId?: number;
}

export interface TrainUserAbilityCreateReq {
  trainId?: number;
  courseId?: number;
  knowledgeId?: number;
  result?: string;
  configId?: number;
  score?: number;
  currentScore?: number;
  userId?: number;
  platformUserId?: number;
  comments?: string;
  status?: number;
  sortNumber?: number;
}

export type TrainUserAbilityUpdateReq = Partial<TrainUserAbilityCreateReq>;

// ==================== Demand Train (企业培训需求) ====================
export interface DemandTrain {
  id: number;
  orgId?: number;
  content?: string;
  nums?: number;
  status?: number;
  addtime?: number;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
  solution?: number;
  solutionCn?: string;
  publisher?: string;
}

export interface DemandPending {
  id: number;
  orgId?: number;
  content?: string;
  nums?: number;
  status?: number;
  addtime?: number;
  addtimeCn?: string;
  company?: string;
  companyType?: string;
  publisher?: string;
}

export interface DemandTrainQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  orgId?: number;
}

// ==================== Demand Employee (企业人才需求) ====================
export interface DemandEmployee {
  id: number;
  orgId?: number;
  work?: string;
  education?: string;
  major?: string;
  addtime?: number;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface DemandEmployeeQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  orgId?: number;
}

export interface DemandEmployeeCreateReq {
  orgId: number;
  work: string;
  education?: string;
  major?: string;
  addtime?: number;
}

export type DemandEmployeeUpdateReq = Partial<DemandEmployeeCreateReq>;

export interface DemandConvertReq {
  title: string;
  companyId: number;
  trainCompanyName?: string;
  workTypeId: number;
  workTypeLevel: number;
  num: number;
  traindemandId?: number;
}

export interface TrainInfoOption extends SimpleItem {
  company_id?: number;
  company_name?: string;
  work_type_id?: number;
}

export interface DemandPublicData {
  company_list: CompanyOption[];
  worktype_list: WorkTypeGroup[];
  traininfo_list?: TrainInfoOption[];
  kejian_list?: SimpleItem[];
  /** 申报条件，对齐 PHP conditionList */
  condition_list?: SimpleItem[];
}

export interface SimpleItem {
  id: number;
  title: string;
}

export interface CompanyOption {
  id: number;
  title: string;
  tag: string;
}

export interface WorkTypeGroup {
  id: number;
  title: string;
  options: WorkTypeOption[];
}

export interface WorkTypeOption {
  id: number;
  title: string;
}

// ==================== Training Solution List (培训方案列表) ====================
export interface TrainSolutionList {
  id: number;
  title: string;
  companyId?: number;
  companyName?: string;
  trainTitle?: string;
  badge?: number;
  expend?: number;
  createTime?: string;
  rejectReason?: string;
}

export interface TrainSolutionDetail {
  id: number;
  title: string;
  company_id?: number;
  company_name?: string;
  train_info_id?: number;
  train_title?: string;
  file_path?: string;
  file?: string;
  file_name?: string;
  fileType?: string;
  content?: string;
  reject_reason?: string;
  badgeList: TrainSolutionBadge[];
}

export interface TrainSolutionBadge {
  id?: number;
  solutionId?: number;
  title: string;
  badge: number;
  expend: number;
}

export interface TrainSolutionSaveReq {
  id?: number;
  title: string;
  companyId: number;
  companyName?: string;
  traininfoId: number;
  filePath?: string;
  badgeList: TrainSolutionBadge[];
}

// ==================== Company Demand (企业端培训需求) ====================
export interface TrainStudent {
  id: number;
  name: string;
  phone: string;
  idCardNumber: string;
  gender: string;
}

export interface SolutionByDemand {
  id: number;
  title: string;
  train_info_id?: number;
  train_info_status?: number;
  file?: string;
  file_path?: string;
  file_name?: string;
  fileType?: string;
  badgeList: TrainSolutionBadge[];
  reject_reason?: string;
  /** ready=方案已就绪；pending=方案正在制作中 */
  status?: 'ready' | 'pending';
}

// ==================== Class People (班级人员 - snake_case 响应) ====================

/** 班级列表项（page 接口返回的 snake_case 字段） */
export interface TrainInfoClassListItem {
  id: number | string;
  title: string;
  nums?: number;
  addtime?: number;
  addtime_cn?: string;
  train_info_id?: number | string;
  company_id?: number | string;
  work_type_id?: number | string;
  work_type_level?: number;
  company_name?: string;
  work_type_title?: string;
  train_title?: string;
  position?: string;
  course_num?: number;
  date_start?: string;
  date_end?: string;
  file_temps?: string;
  kcbiao_file?: string;
  kcbiao_src?: string;
  schedule_manual_edited?: number;
  schedule_imported_at?: string | null;
  company_confirm_status?: number;
  company_confirm_time?: string;
  company_confirm_username?: string;
  company_reject_reason?: string;
  collection_sign_up_id?: number | null;
  profile_total?: number;
  profile_approved?: number;
  profile_pending?: number;
  profile_ready?: boolean;
}

export interface TrainInfoClassPageResponse {
  list: TrainInfoClassListItem[];
  total: number;
  limit: number;
}

/** 学员列表项 */
export interface ClassStudent {
  id: number | string;
  realname: string;
  phone?: string;
  train_id?: number | string;
  train_class_id?: number | string;
  id_card?: string;
  is_leader?: number;
  is_leader_cn?: string;
  is_committee?: number;
  is_committee_cn?: string;
  exam_score?: number;
  real_score?: number;
  normal_score?: number;
  header?: string;
}

export interface ClassStudentPageResponse {
  list: ClassStudent[];
  total: number;
  limit: number;
}

/** 教务/负责人列表项 */
export interface ClassOther {
  id: number | string;
  train_id?: number | string;
  train_class_id?: number | string;
  realname: string;
  phone?: string;
  id_card?: string;
  type?: string;
}

/** 班级岗位角色（train_class_role） */
export interface TrainClassRole {
  id: string;
  classId: number;
  roleCode: string;
  roleName: string;
  subjectType: string;
  subjectId: number;
  platformUserId?: number | null;
  peopleId?: number | null;
  displayName?: string;
  phone?: string;
  status: string;
  unboundWarning?: boolean;
}

export interface TrainClassRoleSummary {
  classId: number;
  roles: TrainClassRole[];
  unboundCount: number;
}

/** 培训任务列表项 */
export interface ClassTask {
  id: number;
  ck_id?: number;
  title?: string;
  date?: string;
  status?: number;
  status_cn?: string;
  times?: string;
  times_cn?: number;
  addtime?: number;
  train_class_id?: number;
}

/** 可导入报名人员 */
export interface SignUpPerson {
  id: number;
  name: string;
  phone?: string;
}

/** 教师库项 */
export interface TeacherBankItem {
  id: number;
  name: string;
  phone?: string;
}

// ==================== Train Summary (培训总结/经验萃取) ====================
export interface TrainSummary {
  id: number;
  title: string;
  author?: string;
  tags?: string;
  content?: string;
  status?: number;
  trainInfoId?: number;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainSummaryQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}

export interface TrainSummaryCreateReq {
  title: string;
  author?: string;
  tags?: string;
  content?: string;
  status?: number;
  trainInfoId?: number;
}

export type TrainSummaryUpdateReq = Partial<TrainSummaryCreateReq>;

// ==================== Train Certificate (证书管理) ====================
export interface TrainCertificate {
  id: number;
  name: string;
  type?: string;
  description?: string;
  templateFile?: string;
  status?: number;
  trainInfoId?: number;
  studentId?: number;
  issueDate?: string;
  tenantId?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainCertificateQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  type?: string;
}

export interface TrainCertificateCreateReq {
  name: string;
  type?: string;
  description?: string;
  templateFile?: string;
  status?: number;
  trainInfoId?: number;
  studentId?: number;
}

export type TrainCertificateUpdateReq = Partial<TrainCertificateCreateReq>;

export interface ResearchCollectionFieldConfig {
  inputType?: 'select' | 'text' | 'date';
  label?: string;
  required?: boolean;
  visible?: boolean;
  allowCustom?: boolean;
  order?: number;
  group?: string;
  options?: string[];
}

export interface ResearchCollectionGroupConfig {
  key: string;
  label: string;
  order: number;
}

export interface ResearchCollectionFieldOptions {
  version?: number;
  groups?: ResearchCollectionGroupConfig[];
  fields?: Record<string, ResearchCollectionFieldConfig>;
}

// ==================== Train Research Org (培训调研企业) ====================
export interface TrainResearchOrg {
  id: number;
  title: string;
  targetOrgId?: number;
  targetOrgName?: string;
  activeSignUpId?: number;
  personnelCount?: number;
  collectionFieldOptions?: ResearchCollectionFieldOptions;
  type?: string;
  provinceId?: number;
  provinceName?: string;
  cityId?: number;
  cityName?: string;
  areaId?: number;
  areaName?: string;
  userId?: number;
  sortNumber?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TrainResearchOrgQuery {
  page?: number;
  pageSize?: number;
  title?: string;
  type?: string;
  status?: number;
}

export interface TrainResearchOrgCreateReq {
  title: string;
  targetOrgId?: number;
  collectionFieldOptions?: ResearchCollectionFieldOptions;
  type?: string;
  provinceId?: number;
  provinceName?: string;
  cityId?: number;
  cityName?: string;
  areaId?: number;
  areaName?: string;
  sortNumber?: number;
  status?: number;
}

export type TrainResearchOrgUpdateReq = Partial<TrainResearchOrgCreateReq>;

export interface TrainResearchCollection {
  researchOrgId: number;
  researchOrgTitle: string;
  targetOrgId?: number;
  targetOrgName?: string;
  signUpId?: number;
  collectionStatus: 'not_started' | 'active';
  personnelCount: number;
  qrcodeToken?: string;
  qrcodeReady: boolean;
  collectionPageUrl?: string;
  collectionStartTime?: string;
  fieldOptions?: ResearchCollectionFieldOptions;
}

export interface TrainResearchPersonCertificate {
  level?: string;
  certNo?: string;
  issueDate?: string;
}

export interface TrainResearchPerson {
  id: number;
  signUpId: number;
  name: string;
  idCardNumber?: string;
  phone?: string;
  companyName?: string;
  major?: string;
  education?: string;
  workTime?: string;
  workTypeName?: string;
  department?: string;
  job?: string;
  workDateStart?: string;
  titleType?: string;
  titleMajor?: string;
  titleCertDate?: string;
  level?: number;
  paperNo?: string;
  certIssueDates?: string;
  certificates?: TrainResearchPersonCertificate[];
  /** 证书查证状态：null未查证 0查证中 1已查证 2查证无结果 3查证失败 */
  certVerified?: number;
  dataSource?: string;
  createTime?: string;
  updateTime?: string;
}

export interface TrainResearchPersonQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface TrainResearchPersonCreateReq {
  name: string;
  idCardNumber?: string;
  phone?: string;
  companyName?: string;
  major?: string;
  education?: string;
  workTime?: string;
  workTypeName?: string;
  department?: string;
  job?: string;
  workDateStart?: string;
  titleType?: string;
  titleMajor?: string;
  titleCertDate?: string;
  level?: number;
  paperNo?: string;
  certIssueDates?: string;
  certificates?: TrainResearchPersonCertificate[];
}

export type TrainResearchPersonUpdateReq = Partial<TrainResearchPersonCreateReq>;

// ==================== Three Year Plan (三年规划) ====================

export interface ThreeYearPlan {
  id: number;
  planNo: string;
  title: string;
  targetOrgId: number;
  targetOrgName?: string;
  researchOrgId?: number;
  researchOrgTitle?: string;
  rencaiTaskId?: number;
  rencaiInputFileId?: number;
  planYears?: string;
  status: number;
  statusLabel?: string;
  analysisStatus?: string;
  rejectReason?: string;
  /** 驳回历史（按时间顺序，越早越靠前） */
  rejectHistory?: ThreeYearPlanRejectHistoryItem[];
  /** 提交历史（按时间顺序，越早越靠前） */
  submitHistory?: ThreeYearPlanSubmitHistoryItem[];
  companyConfirmTime?: string;
  companyConfirmUsername?: string;
  createdBy?: number;
  createTime?: string;
  updateTime?: string;
  totalRows?: number;
  recommendedRows?: number;
  progressPct?: number;
  rencaiTaskStatus?: string;
  submitPersonnelFileId?: number;
  submitSchemeFileId?: number;
  submitPersonnelFileName?: string;
  submitSchemeFileName?: string;
  enterpriseRosterFileId?: number;
  enterpriseShiftFileId?: number;
  enterpriseRosterFileName?: string;
  enterpriseShiftFileName?: string;
  enterpriseReturnTime?: string;
  enterpriseReturnUsername?: string;
  enterpriseReturnReady?: boolean;
  enterpriseReturnHistory?: ThreeYearPlanEnterpriseReturnHistoryItem[];
}

/** 驳回历史条目 */
export interface ThreeYearPlanRejectHistoryItem {
  reason?: string;
  rejectedBy?: string;
  rejectedByUsername?: string;
  /** ISO 字符串 */
  rejectedAt?: string;
}

/** 提交历史条目 */
export interface ThreeYearPlanSubmitHistoryItem {
  submittedBy?: string;
  submittedByUsername?: string;
  submittedAt?: string;
  personnelFileName?: string;
  schemeFileName?: string;
}

/** 企业回传历史条目 */
export interface ThreeYearPlanEnterpriseReturnHistoryItem {
  rosterFileId?: number;
  rosterFileName?: string;
  shiftFileId?: number;
  shiftFileName?: string;
  uploadedBy?: string;
  uploadedByUsername?: string;
  uploadedAt?: string;
  remark?: string;
}

export interface ThreeYearPlanQuery {
  page?: number;
  pageSize?: number;
  title?: string;
  targetOrgId?: number;
  status?: number;
}

export interface ThreeYearPlanItem {
  id: number;
  planId: number;
  personName?: string;
  idCard?: string;
  companyName?: string;
  jobName?: string;
  levelName?: string;
  trainMode?: string;
  planYear?: number;
  trainYear?: number;
  certYear?: number;
  eligibleDate?: string;
  rowIndex?: number;
  source?: string;
}

export interface ThreeYearPlanItemQuery {
  page?: number;
  pageSize?: number;
  planYear?: number;
  jobName?: string;
}

export interface ThreeYearPlanMatrix {
  id: number;
  jobName: string;
  planYear: number;
  trainMode?: string;
  levelName: string;
  headcount: number;
}

export interface ThreeYearPlanAnalysisStatus {
  planId: number;
  rencaiTaskId?: number;
  analysisStatus?: string;
  status: number;
  rencaiTaskStatus?: string;
  totalRows?: number;
  processedRows?: number;
  progressPct?: number;
  recommendedRows?: number;
  notRecommendedRows?: number;
  pendingManualRows?: number;
  errorMessage?: string;
}

export interface ThreeYearPlanDistribution {
  jobName: string;
  levelName: string;
  employeeCount: number;
  thresholdMin: number;
  meetsThreshold: boolean;
  warning?: string;
}

export interface ThreeYearPlanDetailBundle {
  plan: ThreeYearPlan;
  items: PageResponse<ThreeYearPlanItem>;
  matrix: ThreeYearPlanMatrix[];
  distribution: ThreeYearPlanDistribution[];
}

// ==================== Certificate Verification (证书查证) ====================

/** 查证明细 */
export interface CertVerifyDetail {
  peopleId: number;
  name?: string;
  idCardNumber?: string;
  /** 是否查到证书 */
  found: boolean;
  /** 已有证书等级（文本：如"三级"） */
  certLevel?: string;
  /** 证书编号 */
  certNo?: string;
  /** 发证日期 */
  issueDate?: string;
  /** 错误信息（失败/无结果时） */
  errorMessage?: string;
}

/** 批量查证结果 */
export interface CertVerifyBatchResultVO {
  /** 总数 */
  total: number;
  /** 成功查到证书的人数 */
  verified: number;
  /** 查证无结果的人数 */
  notFound: number;
  /** 查证失败的人数 */
  failed: number;
  /** 提示信息 */
  message: string;
  /** 每条明细 */
  details: CertVerifyDetail[];
}

// ==================== 方案规划 · 人员认定（Excel 查证回填，不落库） ====================

export type PersonnelCertLookupStatus =
  | 'pending'
  | 'verified'
  | 'not_found'
  | 'invalid'
  | 'failed'
  | 'skipped';

export interface PersonnelCertLookupPerson {
  rowIndex: number;
  name: string;
  idCard: string;
}

export interface PersonnelCertLookupDetail {
  rowIndex: number;
  name?: string;
  idCard?: string;
  status: PersonnelCertLookupStatus;
  certLevel?: string;
  certNo?: string;
  issueDate?: string;
  message?: string;
}

export interface PersonnelCertLookupResultVO {
  total: number;
  verified: number;
  notFound: number;
  invalid: number;
  failed: number;
  details: PersonnelCertLookupDetail[];
}

/** 分班（组织分班）一期 */
export interface DivisionPlanItem {
  planId: number;
  planNo: string;
  title: string;
  targetOrgId: number;
  orgName?: string;
  status: number;
  statusLabel?: string;
  enterpriseReturnReady?: boolean;
  rosterFileName?: string;
  shiftFileName?: string;
  rosterFileId?: number;
  shiftFileId?: number;
  linkedTrainInfoCount?: number;
  pendingRosterCount?: number;
}

export interface DivisionReadyVO {
  planId: number;
  status: number;
  enterpriseReturnReady: boolean;
  shiftReady?: boolean;
  rosterReady?: boolean;
  requireRoster?: boolean;
  /** 排班表驱动主路径是否可进入 */
  divisionPathReady?: boolean;
  rosterFileId?: number;
  shiftFileId?: number;
  rosterFileName?: string;
  shiftFileName?: string;
  missing: string[];
}

export interface DivisionEnsureTrainInfoReq {
  planYear?: number;
  workTypeId?: number;
  workTypeLevel?: number;
  title?: string;
  /** 培训机构名称 → train_company_name / ${pxdw} */
  trainCompanyName?: string;
  reuseExisting?: boolean;
}

export interface DivisionEnsureTrainInfoResult {
  trainInfoId: number;
  created: boolean;
  title: string;
  trainCompanyName?: string;
}

export interface DivisionRosterParseReq {
  planId: number;
  trainInfoId?: number;
  mode?: 'enterprise_roster' | 'plan_item';
  fallbackToPlanItem?: boolean;
  planYear?: number;
  jobName?: string;
}

export interface DivisionRosterParseResult {
  total: number;
  withIdCard: number;
  withoutIdCard: number;
  source: string;
}

export interface DivisionRosterItem {
  id: number;
  planId: number;
  trainInfoId?: number;
  source: string;
  personName: string;
  idCard?: string;
  phone?: string;
  jobName?: string;
  levelName?: string;
  workshop?: string;
  shiftTeam?: string;
  planYear?: number;
  trainMode?: string;
  assignStatus: number;
  trainClassId?: number;
}

export interface DivisionOrgSplitClassItem {
  classId?: number;
  title: string;
  workTypeId?: number;
  workTypeLevel?: number;
  dateStart?: string;
  dateEnd?: string;
  rosterIds: number[];
}

export interface DivisionOrgSplitReq {
  planId: number;
  trainInfoId: number;
  classes: DivisionOrgSplitClassItem[];
  submitForCompanyConfirm?: boolean;
}

export interface DivisionTrainInfoSummary {
  trainInfoId: number;
  title: string;
  planId?: number;
  companyId?: number;
  companyName?: string;
  classCount: number;
  peopleCount: number;
  pendingRosterCount: number;
}

export interface DivisionAutoPackReq {
  planId: number;
  trainInfoId: number;
  strategy?: 'by_job_level' | 'by_job_level_slot' | 'by_shift_team' | 'by_roster_order';
  classMax?: number;
  classNamePrefix?: string;
  submitForCompanyConfirm?: boolean;
  excludeNoIdCard?: boolean;
  useShiftFilter?: boolean;
  requireJobAndLevel?: boolean;
  year?: number;
  month?: number;
  autoParseRoster?: boolean;
}

export interface DivisionManualPackReq {
  planId: number;
  trainInfoId: number;
  classMax?: number;
  classNamePrefix?: string;
  classes?: {
    title?: string;
    rosterIds?: number[];
  }[];
}

export interface DivisionAddPersonReq {
  personName: string;
  idCard?: string;
  phone?: string;
  jobName?: string;
  levelName?: string;
  workshop?: string;
  shiftTeam?: string;
}

export interface DivisionAutoPackClassMember {
  rosterId: number;
  personName?: string;
  idCard?: string;
  /** 单位/车间/部门 */
  workshop?: string;
  jobName?: string;
  levelName?: string;
  shiftTeam?: string;
  phone?: string;
}

export interface DivisionAutoPackClass {
  tempId?: string;
  classId?: number;
  title: string;
  group?: string;
  /** 班级工种 ID（exam_work_type.id，与开班准备一致） */
  workTypeId?: number;
  /** 班级工种名称（展示/兼容；以 workTypeId 为准） */
  jobName?: string;
  /** 班级等级（可多班不同等级；对应 ${level}） */
  levelName?: string;
  /** 1~5，与名单/认定一致：初级工=1 … 高级技师=5 */
  workTypeLevel?: number;
  nums: number;
  members?: DivisionAutoPackClassMember[];
}

export interface DivisionAutoPackUnassigned {
  rosterId?: number;
  personName?: string;
  idCard?: string;
  workshop?: string;
  jobName?: string;
  levelName?: string;
  shiftTeam?: string;
  reason: string;
}

export type DivisionPackTaskStatus =
  | 'draft'
  | 'pending'
  | 'rejected'
  | 'materialized'
  | 'superseded';

export interface DivisionPackTaskResult {
  taskId: number;
  planId: number;
  trainInfoId: number;
  companyId?: number;
  strategy: string;
  classMax: number;
  year?: number;
  month?: number;
  classNamePrefix?: string;
  status: DivisionPackTaskStatus;
  statusLabel?: string;
  classCount: number;
  assignedCount: number;
  unassignedCount: number;
  classes: DivisionAutoPackClass[];
  unassigned: DivisionAutoPackUnassigned[];
  warnings?: { level?: string; code?: string; message: string }[];
  rejectReason?: string;
  submitUsername?: string;
  confirmUsername?: string;
  confirmTime?: string;
  createTime?: string;
  updateTime?: string;
  materialized?: {
    classCount: number;
    peopleCount: number;
    classes: { classId: number; title: string; nums: number }[];
    platformUserSync?: PlatformUserSyncResult;
  };
  platformUserSync?: PlatformUserSyncResult;
}

export interface PlatformUserSyncResult {
  created: number;
  reused: number;
  updated: number;
  skipped: number;
  phpWritten?: number;
  loginReady?: number;
  warnings?: { name?: string; idCard?: string; reason: string }[];
}

/** @deprecated 使用 DivisionPackTaskResult */
export type DivisionAutoPackResult = DivisionPackTaskResult;

export interface DivisionPackPatchReq {
  classes: {
    tempId?: string;
    title: string;
    group?: string;
    workTypeId?: number;
    jobName?: string;
    levelName?: string;
    members: DivisionAutoPackClassMember[];
  }[];
  unassigned?: DivisionAutoPackUnassigned[];
}

/** 二期：排课分班（L2） */
export interface ShiftAssignGenerateReq {
  planId: number;
  trainInfoId?: number;
  year: number;
  month: number;
  adapterType?: string;
  classMin?: number;
  classMax?: number;
}

export interface ShiftAssignWarning {
  level?: string;
  code?: string;
  message: string;
}

export interface ShiftAssignBinding {
  algoClassName: string;
  recordCount: number;
  trainClassId?: number;
}

export interface ShiftAssignRecordItem {
  id: number;
  personName: string;
  idCard?: string;
  employeeNo?: string;
  dept?: string;
  shiftClass?: string;
  assignDate: string;
  slot: string;
  algoClassName: string;
  trainClassId?: number;
}

export interface ShiftAssignTaskResult {
  taskId: number;
  planId: number;
  trainInfoId?: number;
  year: number;
  month: number;
  adapterType?: string;
  status?: string;
  version?: number;
  sourceShiftFileId?: number;
  warnings?: ShiftAssignWarning[];
  config?: Record<string, unknown>;
  unassignedPersons?: { personKey?: string; personName?: string; reason?: string }[];
  theoryDays?: string[];
  practiceDays?: string[];
  recordCount?: number;
  algoClassNames?: string[];
  bindings?: ShiftAssignBinding[];
  records?: ShiftAssignRecordItem[];
  dailyStats?: { date: string; slot?: string; algoClassName?: string; count: number }[];
  created?: boolean;
  boundAlgoClassName?: string;
  boundTrainClassId?: number;
  boundRecordCount?: number;
}

export interface ShiftAssignPrecheckResult {
  adapterType?: string;
  year: number;
  month: number;
  warnings?: ShiftAssignWarning[];
  algoClassNames?: string[];
  dailyStats?: { date: string; slot?: string; algoClassName?: string; count: number }[];
  unassignedPersons?: { personKey?: string; personName?: string; reason?: string }[];
  recordCount?: number;
  theoryDays?: string[];
  practiceDays?: string[];
}

export interface ShiftAssignPreviewResult {
  adapterType?: string;
  year: number;
  month: number;
  personCount?: number;
  slotCount?: number;
  warnings?: ShiftAssignWarning[];
  unrecognizedShifts?: string[];
  failedExtractRows?: string[];
}

export interface ShiftAssignBindClassReq {
  algoClassName: string;
  trainClassId: number;
}

export interface ShiftAssignPatchReq {
  version: number;
  changes: {
    personName: string;
    employeeNo?: string;
    assignDate: string;
    slot?: string;
    algoClassName?: string | null;
  }[];
}

export interface ShiftAssignMaterializeReq {
  trainInfoId: number;
  mode?: 'algo_class_as_train_class' | 'bind_existing';
  submitForCompanyConfirm?: boolean;
  classNamePrefix?: string;
  onlyUnbound?: boolean;
  matchRoster?: boolean;
}

export interface ShiftAssignGenerateMaterializeReq extends ShiftAssignGenerateReq {
  trainInfoId: number;
  mode?: 'algo_class_as_train_class' | 'bind_existing';
  submitForCompanyConfirm?: boolean;
  classNamePrefix?: string;
  onlyUnbound?: boolean;
  matchRoster?: boolean;
}

export interface ShiftAssignMaterializeResult {
  taskId: number;
  trainInfoId: number;
  status?: string;
  classCount: number;
  classes: { algoClassName: string; classId: number; title?: string; nums: number }[];
  warnings?: ShiftAssignWarning[];
  submitForCompanyConfirm?: boolean;
  generatedAndMaterialized?: boolean;
  recordCount?: number;
  algoClassNames?: string[];
}

// ==================== Exam score import (人社成绩花名册) ====================

export interface ExamScoreImportClassOption {
  id: number | string;
  title: string;
  companyId?: number | string;
  companyName?: string;
  trainInfoId?: number | string;
  trainTitle?: string;
  workTypeId?: number | string;
  workTypeTitle?: string;
  workTypeLevel?: number;
}

export interface ExamScoreImportTarget {
  peopleId: number | string;
  classId: number | string;
  classTitle?: string;
  rosterName?: string;
  oldExamScore?: number | null;
  oldRealScore?: number | null;
  nameMismatch?: boolean;
  jobMismatch?: boolean;
}

export interface ExamScoreImportItem {
  rowNo: number;
  idCard?: string;
  name?: string;
  ticketNo?: string;
  jobName?: string;
  levelName?: string;
  theoryScore?: number | null;
  skillScore?: number | null;
  examScore?: number | null;
  theoryFallback?: boolean;
  status: string;
  targets: ExamScoreImportTarget[];
}

export interface ExamScoreImportRosterGap {
  peopleId?: number | string;
  classId?: number | string;
  classTitle?: string;
  rosterName?: string;
  idCard?: string;
}

export interface ExamScoreImportPreview {
  previewId: string;
  planName?: string;
  planCode?: string;
  examTimeText?: string;
  layout?: string;
  classIds: Array<number | string>;
  totalRows: number;
  matched: number;
  nameWarn: number;
  jobWarn: number;
  unmatched: number;
  emptyScore: number;
  sheetDup: number;
  classMissingOnSheet: number;
  classMissingIdCard: number;
  items: ExamScoreImportItem[];
  classMissingPeople: ExamScoreImportRosterGap[];
  missingIdCardPeople: ExamScoreImportRosterGap[];
}

export interface ExamScoreImportCommitResult {
  batchId: number | string;
  written: number;
  overwritten: number;
  skipped: number;
  unmatched: number;
}

export interface ExamScoreImportBatch {
  id: number | string;
  fileName?: string;
  planName?: string;
  planCode?: string;
  examTimeText?: string;
  layout?: string;
  classIds?: Array<number | string>;
  totalRows: number;
  matchedCount: number;
  writtenCount: number;
  overwrittenCount: number;
  skippedCount: number;
  unmatchedCount: number;
  operatorUsername?: string;
  createdAt?: string;
}

export interface ExamScoreImportBatchItem {
  id: number | string;
  rowNo?: number;
  idCard?: string;
  name?: string;
  ticketNo?: string;
  theoryScore?: number | null;
  skillScore?: number | null;
  comprehensiveScore?: number | null;
  peopleId?: number | string;
  classId?: number | string;
  resultCode: string;
  oldExamScore?: number | null;
  newExamScore?: number | null;
  oldRealScore?: number | null;
  newRealScore?: number | null;
  theoryFallback?: boolean;
}

// ==================== Training Needs (培训需求) ====================
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
  keyword?: string;
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
}

export interface TrainExamQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
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
  signUpId?: number;
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
  status?: number;
  tenantId?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
  sortNumber?: number;
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
  id?: number;
  trainInfoId?: number;
  title?: string;
  nums?: number;
  companyId?: number;
  workTypeId?: number;
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
}

// ==================== Class People (班级人员 - snake_case 响应) ====================

/** 班级列表项（page 接口返回的 snake_case 字段） */
export interface TrainInfoClassListItem {
  id: number;
  title: string;
  nums?: number;
  addtime?: number;
  addtime_cn?: string;
  train_info_id?: number;
  company_id?: number;
  work_type_id?: number;
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
}

export interface TrainInfoClassPageResponse {
  list: TrainInfoClassListItem[];
  total: number;
  limit: number;
}

/** 学员列表项 */
export interface ClassStudent {
  id: number;
  realname: string;
  phone?: string;
  train_id?: number;
  train_class_id?: number;
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
  id: number;
  train_id?: number;
  train_class_id?: number;
  realname: string;
  phone?: string;
  id_card?: string;
  type?: string;
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

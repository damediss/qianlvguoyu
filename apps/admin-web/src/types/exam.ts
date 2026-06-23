export interface ExamWorkType {
  id: number;
  title: string;
  seriesNo: string;
  levelsNo: string;
  parentId: number | null;
  userId: number | null;
  status: number;
  sortNumber: number;
  tenantId: number;
  createTime: string;
  children?: ExamWorkType[];
}

export interface ExamWorkTypeCreateReq {
  title: string;
  seriesNo?: string;
  levelsNo?: string;
  parentId?: number;
  status?: number;
  sortNumber?: number;
}

export interface ExamWorkTypeUpdateReq {
  title: string;
  seriesNo?: string;
  levelsNo?: string;
  parentId?: number;
  status?: number;
  sortNumber?: number;
}

export interface ExamWorkTypeQuery {
  page?: number;
  pageSize?: number;
  title?: string;
}

export interface ExamCourse {
  id: number;
  workTypeId: number | null;
  title: string;
  level: number;
  seriesNoGroup: string;
  seriesNo: string;
  userInCharge: number | null;
  userInChargeName: string;
  lastLevelGetYear: number;
  workYearLimit: number | null;
  minLevel: number | null;
  minLevelYear: number | null;
  startAge: number;
  isGeneral: number;
  examCondition: string;
  version: string;
  comments: string;
  userId: number | null;
  status: number;
  sortNumber: number;
  tenantId: number;
  createTime: string;
}

export interface ExamCourseCreateReq {
  title: string;
  workTypeId?: number;
  level?: number;
  seriesNoGroup?: string;
  seriesNo?: string;
  userInChargeName?: string;
  lastLevelGetYear?: number;
  workYearLimit?: number;
  minLevel?: number;
  minLevelYear?: number;
  startAge?: number;
  isGeneral?: number;
  examCondition?: string;
  comments?: string;
  status?: number;
  sortNumber?: number;
}

export interface ExamCourseUpdateReq {
  title: string;
  workTypeId?: number;
  level?: number;
  seriesNoGroup?: string;
  seriesNo?: string;
  userInChargeName?: string;
  lastLevelGetYear?: number;
  workYearLimit?: number;
  minLevel?: number;
  minLevelYear?: number;
  startAge?: number;
  isGeneral?: number;
  examCondition?: string;
  comments?: string;
  status?: number;
  sortNumber?: number;
}

export interface ExamCourseQuery {
  page?: number;
  pageSize?: number;
  title?: string;
  workTypeId?: number;
}
export interface ExamUserAuthCourse {
  realname: string;
  idCard: string;
  courses: ExamUserAuthCourseItem[];
  status: number;
  sortNumber: number;
  createTime: string;
}

export interface ExamUserAuthCourseItem {
  courseId: number;
  courseTitle: string;
  type: number;
}

export interface ExamUserAuthCourseCreateReq {
  realname: string;
  idCard: string;
  items: ExamUserAuthCourseItem[];
  status?: number;
  sortNumber?: number;
}

export interface ExamUserAuthCourseQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// ---- 细目表任务 ----
export interface ExamTaskXimu {
  id: number;
  workTypeId: number;
  level: number;
  types: number;
  nums: number;
  status: number;
  expertStart: number | null;
  expertEnd: number | null;
  filePath: string;
  fileUrl: string;
  fileType: string;
  addtime: number;
  workTypeCn: string;
  levelCn: string;
  typesCn: string;
  statusCn: string;
  expertStartName: string;
  expertEndName: string;
  addtimeCn: string;
}

export interface ExamTaskXimuQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

export interface ExamTaskXimuDetailChild {
  id: string;
  snoItem: string;
  snoTitle: string;
}

export interface ExamTaskXimuDetail {
  id: number;
  taskId: number;
  sno: string;
  sno0: string;
  sno1: string;
  title: string;
  snoItem: string;
  snoTitle: string;
  rowColumn: string;
  children: ExamTaskXimuDetailChild[];
}

export interface ExamTaskXimuDaily {
  ymd: string;
  nums: number;
}

export interface ExamTaskXimuAssignExpertReq {
  taskId: number;
  expertId: number;
  role: number;
}

export interface PublicDataExpert {
  id: number;
  nickname: string;
}

export interface ExamWorkTypeTree {
  id: number;
  title: string;
  children?: ExamWorkTypeTree[];
}

export interface PublicData {
  workTypeList: ExamWorkTypeTree[];
  expertList: PublicDataExpert[];
}

// ---- 题库审核指派(理论/实操/主观 三子菜单) ----
// 题型枚举:理论 exam / 实操 examshicao / 主观 examsub
export type TaskAuditType = 'exam' | 'examshicao' | 'examsub';

// 审核角色:0 初审 / 1 终审
export type TaskAuditRole = 0 | 1;

// 任务状态:0 待审核 1 初审中 2 终审中 3 完成
export type TaskAuditStatusCode = 0 | 1 | 2 | 3;

export interface TaskAuditVO {
  id: number;
  taskType: TaskAuditType;
  workTypeId: number;
  workTypeCn: string;
  level: number;
  levelCn: string;
  status: TaskAuditStatusCode;
  statusCn: string;
  expertStart: number | null;
  expertStartName: string;
  expertEnd: number | null;
  expertEndName: string;
  nums: number;
  filePath: string;
  fileUrl: string;
  fileType: string;
  addtime: number;
  addtimeCn: string;
}

export interface TaskAuditQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// 详情:每道题的"主体"(来自 task_*_detail),children 为初审/终审两条审核记录
export interface TaskAuditDetailItem {
  id: number;
  taskId: number;
  taskType: TaskAuditType;
  // 理论题字段
  xingwei?: string;
  kaohe1?: string;
  kaohe2?: string;
  kaohe3?: string;
  kaohe4?: string;
  jianding?: string;
  level?: string;
  nanyicd?: string;
  examType?: string;
  examContent?: string;
  examA?: string;
  examB?: string;
  examC?: string;
  examD?: string;
  examE?: string;
  examF?: string;
  examG?: string;
  examH?: string;
  examAnswer?: string;
  isblod?: number;
  // 实操字段
  content?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  // 主观字段
  zhishidian?: string;
  answer?: string;
  isJisuan?: number;
  pic?: string;
  // 初审/终审 child 审核记录
  children?: TaskAuditDetailChild[];
}

export interface TaskAuditDetailChild {
  id: string; // "初审" or "终审"
  examId?: number;
  expertId?: number;
  expertName?: string;
  auditStatus?: number;
  // 理论题审核字段
  examContent?: string;
  examAnswer?: string;
  auditIsblod?: number;
  // 实操题审核字段
  content?: string;
  answer?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  // 主观题审核字段
  pic?: string;
  // 通用
  addtimeCn?: string;
  [key: string]: unknown;
}

export interface TaskAuditDaily {
  ymd: string;
  nums: number;
}

export interface TaskAuditAssignExpertReq {
  taskId: number;
  expertId: number;
  role: TaskAuditRole;
}

// 题库管理端 3 子菜单的 page component 配置(传 props 给共享表格组件)
export interface TaskAuditTableConfig {
  taskType: TaskAuditType;
  permissionPrefix: string; // e.g. "task:exam-audit"
  title: string;             // 页面标题
  detailColumnLabels: TaskAuditDetailColumnLabels;
  // 用于详情 Drawer 展示列的标题
  contentFieldName: string;  // 题干字段名(examContent / content)
}

// 每个题型详情 Drawer 用到的列标签
export interface TaskAuditDetailColumnLabels {
  // 理论
  examType?: string;
  examContent?: string;
  examA?: string;
  examB?: string;
  examC?: string;
  examD?: string;
  examE?: string;
  examF?: string;
  examG?: string;
  examH?: string;
  examAnswer?: string;
  isblod?: string;
  // 实操
  jianding?: string;
  content?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  // 主观
  zhishidian?: string;
  answer?: string;
  isJisuan?: string;
  nanyicd?: string;
}

// 实操题"评分"列的子表 item schema
// 对应后端 exam_task_examshicao_detail.pingfen(JSON 数组)
export interface PingfenItem {
  yaosu?: string;       // 要素
  peifen?: string;      // 配分
  biaozhun?: string;    // 评分标准
  koufen?: string;      // 最大扣分
  buchang?: string;     // 步长
}

// ---- 题库分类导航聚合树(单接口一次性返回工种→课程→知识点 L0~L3) ----
export type QuestionBankTreeNodeType = 'workType' | 'course' | 'knowledge';

export interface QuestionBankTreeNodeVO {
  key: string;
  id: number;
  nodeType: QuestionBankTreeNodeType;
  title: string;
  sortNumber?: number;
  /** workType/course 不返回;knowledge 节点业务状态 0/1 */
  status?: number;
  /** 知识点节点 0/1/2/3 */
  level?: number;
  /** 仅 course 节点 */
  workTypeId?: number;
  /** 仅 knowledge 节点 */
  courseId?: number;
  /** 子树递归题数(L3 挂载题 + 所有后代题) */
  questionCount: number;
  /** 永远是数组(空叶节点是 []),不会是 null */
  children: QuestionBankTreeNodeVO[];
}

// ---- 知识点(试题管理) ----
export interface ExamKnowledgeStructure {
  id: number;
  type: number;
  useType: number | null;
  courseId: number | null;
  parentId: number;
  cover: string;
  intro: string;
  title: string;
  seriesNo: string;
  level: number;
  important: number;
  userId: number | null;
  status: number;
  sortNumber: number;
  tenantId: number;
  amount: number;
  createTime: string;
  children?: ExamKnowledgeStructure[];
}

export interface ExamKnowledgeStructureCreateReq {
  title: string;
  type?: number;
  courseId?: number;
  parentId?: number;
  seriesNo?: string;
  important?: number;
  status?: number;
  sortNumber?: number;
  cover?: string;
  intro?: string;
  amount?: number;
}

export interface ExamKnowledgeStructureUpdateReq {
  title?: string;
  type?: number;
  courseId?: number;
  parentId?: number;
  seriesNo?: string;
  important?: number;
  status?: number;
  sortNumber?: number;
  cover?: string;
  intro?: string;
  amount?: number;
}

export interface ExamKnowledgeStructureQuery {
  page?: number;
  pageSize?: number;
  courseId?: number;
  type?: number;
  title?: string;
  level?: number;
}

// ---- 试题详情(试题管理) ----
export interface ExamExamDetail {
  id: number;
  examId: number | null;
  taskDetailId: number | null;
  source: number;
  orgId: number | null;
  orgName: string;
  knowledgeId: number | null;
  knowledgeTitle: string;
  isPrivate: number;
  difficulty: number;
  userId: number | null;
  creatorName: string;
  checkUserId: number | null;
  finalCheckUserId: number | null;
  type: string;
  keyword: string;
  audio: string;
  video: string;
  content: string;
  answer: string;
  score: number | null;
  resolve: string;
  checkStatus: number;
  finalCheckStatus: number;
  status: number;
  sortNumber: number;
  tenantId: number;
  createTime: string;
  /** 后端 BaseDO.updateTime,某些接口可能不返回(此时回退到 createTime) */
  updateTime?: string;
  checkTime: string;
  finalCheckTime: string;
}

export interface ExamExamDetailCreateReq {
  type: string;
  content?: string;
  answer?: string;
  knowledgeId?: number;
  difficulty?: number;
  score?: number;
  resolve?: string;
  keyword?: string;
  source?: number;
  status?: number;
  sortNumber?: number;
}

export interface ExamExamDetailUpdateReq {
  type?: string;
  content?: string;
  answer?: string;
  knowledgeId?: number;
  difficulty?: number;
  score?: number;
  resolve?: string;
  keyword?: string;
  source?: number;
  status?: number;
  sortNumber?: number;
  checkStatus?: number;
  finalCheckStatus?: number;
}

export interface ExamExamDetailQuery {
  page?: number;
  pageSize?: number;
  courseId?: number;
  knowledgeId?: number;
  type?: string;
  source?: number;
  difficulty?: number;
  status?: number;
  keyword?: string;
}

// 来源映射:0=内部,1=外部
export const EXAM_SOURCE_MAP: Record<number, string> = {
  0: '内部',
  1: '外部',
};

// 题型映射
export const EXAM_TYPE_MAP: Record<string, string> = {
  single: '单选题',
  multi: '多选题',
  judge: '判断题',
  fill: '填空题',
  short: '简答题',
  essay: '论述题',
  case: '案例分析',
  composite: '综合题',
  match: '匹配题',
  read: '阅读理解',
  practice: '实操题',
  drawing: '制图题',
  compute: '计算题',
  name: '名词解释',
  error_correction: '改错题',
  program: '编程题',
  oral: '口试题',
  drawing2: '画图题',
  cloze: '完形填空',
  translation: '翻译题',
  writing: '写作题',
};

// 难度映射
export const DIFFICULTY_MAP: Record<number, string> = {
  0: '易',
  1: '较易',
  2: '中',
  3: '较难',
  4: '难',
};

// 难度颜色
export const DIFFICULTY_COLOR_MAP: Record<number, string> = {
  0: 'green',
  1: 'blue',
  2: 'orange',
  3: 'red',
  4: '#8B0000',
};

// 审核状态映射
export const CHECK_STATUS_MAP: Record<number, string> = {
  0: '待审',
  1: '通过',
  2: '不通过',
};

// ==================== 组卷计划 ====================

export interface ExamMakeExamPlan {
  id: number;
  courseId: number;
  rateId: number | null;
  type: number | null;
  examType: number;
  title: string;
  time: number | null;
  isComputer: number | null;
  isPrint: number | null;
  openRate: number | null;
  totalPoint: number | null;
  passPoint: number | null;
  comments: string | null;
  userId: number | null;
  status: number;
  isAll: number | null;
  sortNumber: number | null;
  tenantId: number;
  createTime: string;
  planDetailList?: ExamMakeExamPlanDetail[];
}

export interface ExamMakeExamPlanDetail {
  id: number;
  makeId: number;
  courseId: number;
  type: string;
  num: number;
  point: number | null;
  startPoint: number | null;
  endPoint: number | null;
  difficultyStart: number | null;
  difficultyEnd: number | null;
  totalPoint: number | null;
  userId: number | null;
  status: number;
  sortNumber: number | null;
  tenantId: number;
  createTime: string;
}

export interface ExamMakeExamPlanDetailReq {
  type: string;
  num: number;
  point?: number;
  startPoint?: number;
  endPoint?: number;
  difficultyStart?: number;
  difficultyEnd?: number;
}

export interface ExamMakeExamPlanCreateReq {
  title: string;
  courseId: number;
  rateId?: number;
  type?: number;
  examType: number;
  time?: number;
  isComputer?: number;
  isPrint?: number;
  openRate?: number;
  totalPoint?: number;
  passPoint?: number;
  isAll?: number;
  comments?: string;
  detailList?: ExamMakeExamPlanDetailReq[];
}

export interface ExamMakeExamPlanQuery {
  page?: number;
  pageSize?: number;
  courseId?: number;
  examType?: number;
  keyword?: string;
}

// ==================== 知识比重 ====================

export interface ExamKnowledgeRate {
  id: number;
  courseId: number;
  type: number | null;
  isStandard: number | null;
  title: string;
  sortNumber: number | null;
  comments: string | null;
  status: number;
  userId: number | null;
  tenantId: number;
  createTime: string;
  detailList?: ExamKnowledgeRateDetail[];
}

export interface ExamKnowledgeRateDetail {
  id: number;
  knowledgeRateId: number;
  knowledgeId: number;
  rate: number;
  userId: number | null;
  sortNumber: number | null;
  comments: string | null;
  status: number;
  tenantId: number;
  createTime: string;
}

export interface ExamKnowledgeRateDetailReq {
  knowledgeId: number;
  rate: number;
}

export interface ExamKnowledgeRateCreateReq {
  title: string;
  courseId: number;
  type?: number;
  isStandard?: number;
  comments?: string;
  detailList?: ExamKnowledgeRateDetailReq[];
}

export interface ExamKnowledgeRateQuery {
  page?: number;
  pageSize?: number;
  courseId?: number;
  keyword?: string;
}

// ==================== 试卷 ====================

export interface ExamPaper {
  id: number;
  makeId: number | null;
  source: number | null;
  type: number | null;
  code: string;
  title: string;
  time: number | null;
  totalPoint: number | null;
  passPoint: number | null;
  isComputer: number | null;
  isPrint: number | null;
  comments: string | null;
  userId: number | null;
  status: number;
  sortNumber: number | null;
  tenantId: number;
  createTime: string;
}

export interface ExamPaperDetail {
  id: number;
  paperId: number;
  examId: number;
  type: string;
  diff: number | null;
  content: string;
  answer: string;
  score: number;
  resolve: string;
  status: number;
  sortNumber: number | null;
  tenantId: number;
  createTime: string;
}

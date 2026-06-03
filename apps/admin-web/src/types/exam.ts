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
  level?: number;
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
  // 初审/终审 child 审核记录
  children?: TaskAuditDetailChild[];
}

export interface TaskAuditDetailChild {
  id: string; // "初审" or "终审"
  expertId?: number;
  expertName?: string;
  auditStatus?: number;
  auditContent?: string;
  auditAnswer?: string;
  auditIsblod?: number;
  auditPingfen?: string;
  auditKaochang?: string;
  auditKaosheng?: string;
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
  content?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  // 主观
  zhishidian?: string;
  answer?: string;
  isJisuan?: string;
}

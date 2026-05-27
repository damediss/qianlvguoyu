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

/** 历年真题（管理端 + App 契约）类型。id 与 examId 一律字符串（雪花 id，防 JS 精度丢失）。 */

export interface ZhentiItem {
  id: string;
  examId: string;
  title: string;
  year: string | null;
  fileSize: number;
  sort: number;
  /** 1 上架 / 0 下架 */
  status: number;
  /** 1 待完善（缺年份或工种等级） */
  metaPending: number;
  createdBy: string | null;
  createdById: string | null;
  createdAt: string;
  /** 共享组展示（「工种 · 四级/中级工」）；私有真题为「本计划私有」 */
  scopeLabel?: string | null;
}

/** 共享组信息（提示条 / 上传弹窗选择项）。 */
export interface ZhentiScopeItem {
  workTypeId: number;
  level: number;
  label: string;
  planCount: number;
}

export interface ZhentiScopeInfo {
  scopeEnabled: boolean;
  /** 无有效共享组：真题仅本计划可见，上传走私有口径 */
  unscoped: boolean;
  scopes: ZhentiScopeItem[];
}

/** 「真题共享范围」页：一条工种绑定。 */
export interface ZhentiScopeBinding {
  workTypeId: number | null;
  workTypeTitle: string | null;
  level: number | null;
  levelLabel: string | null;
  courseId: number | null;
  courseTitle: string | null;
  /** false = 工种不存在或已删除，需改绑课程修复 */
  valid: boolean;
}

export interface ZhentiScopePlanItem {
  examId: string;
  title: string;
  bindings: ZhentiScopeBinding[];
  noBinding: boolean;
  multiBinding: boolean;
  hasInvalid: boolean;
}

export interface ZhentiScopeCourseOption {
  id: number;
  title: string;
  workTypeId: number | null;
  level: number | null;
}

export interface ZhentiScopePlans {
  records: ZhentiScopePlanItem[];
  total: number;
  page: number;
  size: number;
  courses: ZhentiScopeCourseOption[];
}

export interface ZhentiScopeBatchItem {
  examId: string;
  courseIdList: number[];
}

export interface ZhentiPage {
  records: ZhentiItem[];
  total: number;
  page: number;
  size: number;
  years: string[];
}

export interface ZhentiUploadResult {
  id: string;
  title: string;
  year: string | null;
  metaPending: number;
}

export interface ZhentiImportFileResult {
  fileName: string;
  ok: boolean;
  id?: string;
  title?: string;
  year?: string | null;
  reason?: string;
  /** 文件名解析不出年份/工种等级：已入库，可原地补录 */
  pending?: boolean;
}

export interface ZhentiImportResult {
  mode: 'sync' | 'async';
  importId?: string;
  results?: ZhentiImportFileResult[];
}

export interface ZhentiImportLog {
  importId: string;
  status: 'running' | 'done' | 'failed' | string;
  total: number;
  finished: number;
  results: ZhentiImportFileResult[];
}

export interface ZhentiPreview {
  viewerUrl: string;
  title: string;
}

export interface ZhentiQuery {
  examId: string;
  page: number;
  pageSize: number;
  keyword?: string;
  year?: string;
  status?: number;
  metaPending?: number;
}

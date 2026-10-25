/**
 * 教材生成类型 —— 《职业标准驱动教材生成·一期实施设计》v1.0 §2.3/§3.4
 * 雪花 ID 经 json-bigint 处理后以 string 到达前端。
 */

/**
 * 工程状态：parsed（骨架未建兜底态）→ outlined → reviewed（大纲已评审）
 * 二期新增：writing（编写中，首个有效章分配后）→ done（编写完成，教研手动确认）
 */
export type TextbookProjectStatus = 'parsed' | 'outlined' | 'reviewed' | 'writing' | 'done';

/** 大纲节点维度：仅点级（nodeLevel=4）有值 */
export type OutlineDimension = 'theory' | 'skill';

/** parse-preview 同步解析预览（不落库），供新建向导回填 */
export interface ParsePreviewVO {
  workTypeTitle: string;
  seriesNo: string | null;
  standardVersion: string | null;
  /** 方向列表（单方向工种为 ["默认"]） */
  directions: string[];
  /** 方向 → 可用 levelCode 列表（docx 1.4 节解析结果） */
  directionLevels: Record<string, number[]>;
  /** levelCode → 培训参考学时（解析 1.8 节） */
  trainingHoursByLevel: Record<string, number>;
  /** 方向→职业功能序号猜测值（解析失败为空，需人工勾选） */
  guessedFunctionNos: number[];
  /** levelCode → 各等级解析出的 方向→序号（契约外附加字段） */
  guessedFunctionNosByLevel?: Record<string, Record<string, number[]>>;
  /** levelCode → 职业功能序号→名称（勾选项显示真实功能名；契约外附加字段） */
  functionNamesByLevel?: Record<string, Record<string, string>>;
  warnings: string[];
}

export interface ProjectCreateReq {
  /** 已有 file 模块文件 ID 时直传（JSON 分支） */
  fileId?: string;
  /** 与 fileId 二选一：parse-preview 不落库，创建时随 multipart 重新上传原始 docx */
  file?: File;
  direction: string;
  levelCode: number;
  functionNos: number[];
  trainingHours?: number;
  wordsPerHour?: number;
}

export interface ProjectUpdateReq {
  workTypeTitle?: string;
  trainingHours?: number;
  wordsPerHour?: number;
  functionNos?: number[];
  /** 评审通过：outlined → reviewed（契约外推定字段，见设计 §7 待确认项 2） */
  status?: TextbookProjectStatus;
}

export interface TextbookProjectQuery {
  page: number;
  pageSize: number;
  workTypeTitle?: string;
  direction?: string;
  levelCode?: number;
  status?: TextbookProjectStatus;
}

/** 详情接口携带的大纲合计校验（前端只做展示） */
export interface OutlineSummary {
  chapterSum: number;
  totalWords: number;
  balanced: boolean;
  unbalancedChapters: string[];
}

export interface TextbookProjectVO {
  id: string;
  workTypeTitle: string;
  seriesNo: string | null;
  standardVersion: string | null;
  fileId: string;
  direction: string;
  levelCode: number;
  functionNos: number[];
  trainingHours: number;
  wordsPerHour: number;
  totalWords: number;
  status: TextbookProjectStatus;
  /** JSON 数组字符串（解析告警） */
  parseWarnings: string | null;
  updatedAt: string | null;
  /** 仅 GET /{id} 详情返回 */
  summary?: OutlineSummary;
  // ---- 二期编写协同批量补充字段（《二期编写协同实施设计》§10.2，列表接口批量返回，禁止逐行 N+1）----
  chapterCount?: number;
  assignedChapterCount?: number;
  sectionCount?: number;
  approvedSectionCount?: number;
  /** 工程下已有任何正文记录（驱动删除文案） */
  hasContent?: boolean;
  /** 结构锁：存在正文或有效分配后增删/重建被后端拦截 */
  structureLocked?: boolean;
}

/** 大纲节点（扁平下发，前端自组树） */
export interface OutlineNodeVO {
  id: string;
  parentId: string;
  /** 1=篇 2=章 3=节 4=点 */
  nodeLevel: number;
  dimension: OutlineDimension | null;
  /** 标准原始编号（人工新增节点为空） */
  sourceNo: string | null;
  titleRaw: string;
  title: string;
  /** 权重%（章级） */
  weight: number | null;
  /** 目标字数（章/节级） */
  targetWords: number | null;
  /** 字数被人工调整=1 */
  wordsManual: number;
  sortNo: number;
}

export interface OutlineNodeUpdateReq {
  title?: string;
  targetWords?: number;
  sortNo?: number;
}

export interface OutlineNodeCreateReq {
  projectId: string;
  parentId: string;
  title: string;
  /** 仅父级为节/点时有效（人工补充点可指定理论/技能维度） */
  dimension?: OutlineDimension | null;
}

export const TEXTBOOK_LEVEL_OPTIONS: { value: number; label: string }[] = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

export function textbookLevelLabel(levelCode: number | null | undefined): string {
  if (levelCode == null) return '-';
  return TEXTBOOK_LEVEL_OPTIONS.find((o) => o.value === levelCode)?.label ?? String(levelCode);
}

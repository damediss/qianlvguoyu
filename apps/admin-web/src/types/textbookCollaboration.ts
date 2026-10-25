/**
 * 教材编写协同（二期）类型 —— 《职业标准驱动教材生成·二期编写协同实施设计》v1.1 §5/§6
 * 雪花 ID 一律 string；日期 yyyy-MM-dd，时间 yyyy-MM-dd HH:mm:ss；
 * 分页统一 PageResponse（items/page/pageSize/total）。
 */

/** 章任务状态（textbook_chapter_assignment.task_status，正文状态才是事实源） */
export type TextbookTaskStatus = 'assigned' | 'writing' | 'reviewing' | 'returned' | 'approved';

/** 节正文状态（textbook_section_content.content_status） */
export type TextbookSectionContentStatus = 'draft' | 'submitted' | 'approved' | 'returned';

/** 审定动作（W5） */
export type TextbookReviewAction = 'approve' | 'return' | 'reopen';

/** 完成条件阻塞码（C2 completion.blockers.code，§6.1） */
export type TextbookCompletionBlockerCode =
  | 'NO_CHAPTER'
  | 'NO_SECTION'
  | 'CHAPTER_UNASSIGNED'
  | 'EMPTY_CHAPTER'
  | 'SECTION_NOT_APPROVED'
  | 'PLACEHOLDER_PRESENT';

/** 我的编写任务列表筛选（T1，§6.2） */
export type WritingTaskStatusFilter =
  | 'assigned'
  | 'writing'
  | 'returned'
  | 'reviewing'
  | 'approved'
  | 'overdue';

/** 我的审定任务列表筛选（T2，§6.2） */
export type ReviewTaskStatusFilter = 'pending' | 'returned' | 'approved' | 'all';

// ------------------------------------------------------------------
// C1 候选人
// ------------------------------------------------------------------

export interface CollaboratorQuery {
  role: 'writer' | 'reviewer';
  keyword?: string;
  page: number;
  pageSize: number;
}

/** 候选人（不返回手机号；显示 displayName（username）/组织） */
export interface CollaboratorVO {
  id: string;
  displayName: string;
  username: string;
  orgName: string | null;
}

// ------------------------------------------------------------------
// C2/C3/C4 分配
// ------------------------------------------------------------------

/** 章当前分配 + 进度缓存（AssignmentVO，§6.1） */
export interface TextbookAssignmentVO {
  chapterId: string;
  writerId: string;
  writerName: string;
  reviewerId: string;
  reviewerName: string;
  dueDate: string | null;
  instruction: string | null;
  active: boolean;
  taskStatus: TextbookTaskStatus;
  /** 仅改派/取消递增；null 表示该章从未分配过 */
  assignmentVersion: number | null;
  sectionCount: number;
  approvedCount: number;
  submittedCount: number;
  returnedCount: number;
  draftCount: number;
  untouchedCount: number;
  assignedByName: string;
  assignedAt: string;
}

export interface ChapterAssignmentItem {
  chapterId: string;
  writerId: string;
  reviewerId: string;
  dueDate?: string;
  instruction?: string;
  /** null = 数据库从未有该章分配；已取消（active=0）重新分配也必须传当前版本 */
  assignmentVersion: number | null;
}

export interface CompletionBlocker {
  code: TextbookCompletionBlockerCode;
  chapterId?: string;
  sectionId?: string;
  message: string;
}

export interface TextbookCompletion {
  canComplete: boolean;
  blockers: CompletionBlocker[];
}

export interface CollaborationProgress {
  chapterCount: number;
  assignedChapterCount: number;
  sectionCount: number;
  approvedSectionCount: number;
  actualWords: number;
  targetWords: number;
}

export interface CollaborationSection {
  sectionId: string;
  title: string;
  sourceNo: string | null;
  sortNo: number;
  contentExists: boolean;
  contentStatus: TextbookSectionContentStatus | null;
  wordCount: number;
  targetWords: number | null;
  version: number;
  submittedAt: string | null;
  latestReviewRecord: TextbookReviewRecordVO | null;
}

export interface CollaborationChapter {
  chapterId: string;
  title: string;
  sourceNo: string | null;
  targetWords: number | null;
  assignment: TextbookAssignmentVO | null;
  sections: CollaborationSection[];
}

/** C2 工程编写管理一次返回（project 为 TextbookProjectVO 子集 + 编写协同字段） */
export interface TextbookCollaborationVO {
  project: {
    id: string;
    status: string;
    workTypeTitle: string;
    direction?: string;
    levelCode?: number;
  };
  outlineSummary: { chapterSum: number; totalWords: number; balanced: boolean } | null;
  progress: CollaborationProgress;
  completion: TextbookCompletion;
  chapters: CollaborationChapter[];
}

// ------------------------------------------------------------------
// T1~T4 我的任务
// ------------------------------------------------------------------

export interface MyTaskQuery {
  status?: string;
  keyword?: string;
  overdueOnly?: boolean;
  page: number;
  pageSize: number;
}

/** TaskVO = AssignmentVO + 任务列表扩展（§6.2） */
export interface TextbookTaskVO extends TextbookAssignmentVO {
  projectId: string;
  workTypeTitle: string;
  direction: string;
  levelCode: number;
  chapterTitle: string;
  chapterSourceNo: string | null;
  projectStatus: string;
  latestSectionUpdatedAt: string | null;
  actualWords: number;
  targetWords: number | null;
  overdue: boolean;
}

/** T3 编写统计卡（各口径可能相交，不要求相加等于总数） */
export interface WritingTaskSummaryVO {
  assignedCount: number;
  writingCount: number;
  pendingReviewCount: number;
  returnedCount: number;
  approvedCount: number;
}

/** T4 审定统计卡 */
export interface ReviewTaskSummaryVO {
  pendingCount: number;
  returnedCount: number;
  partialApprovedCount: number;
  approvedCount: number;
}

// ------------------------------------------------------------------
// W1 章工作台
// ------------------------------------------------------------------

export interface OutlinePointVO {
  pointId: string;
  titleRaw: string;
  dimension: 'theory' | 'skill' | null;
  sortNo: number;
}

export interface WorkspaceSection {
  sectionId: string;
  title: string;
  sourceNo: string | null;
  sortNo: number;
  targetWords: number | null;
  contentExists: boolean;
  contentStatus: TextbookSectionContentStatus | null;
  wordCount: number;
  version: number;
  submittedAt: string | null;
  submittedByName: string | null;
  submitRevision: number | null;
  currentSnapshotId: string | null;
  latestReviewRecord: TextbookReviewRecordVO | null;
  points: OutlinePointVO[];
}

/** W1 ChapterWorkspaceVO（assignment 为完整 AssignmentVO） */
export interface ChapterWorkspaceVO {
  projectId: string;
  projectStatus: string;
  workTypeTitle: string;
  direction: string;
  levelCode: number;
  chapterId: string;
  chapterTitle: string;
  chapterSourceNo: string | null;
  assignment: TextbookAssignmentVO | null;
  relations: string[];
  canWrite: boolean;
  canReview: boolean;
  readOnlyReason: string | null;
  sections: WorkspaceSection[];
}

// ------------------------------------------------------------------
// W2 正文
// ------------------------------------------------------------------

/** 检查报告“可能未覆盖”项（§8.3：字段为 title，与 W1 标准点 titleRaw 契约不同） */
export interface UncoveredPointVO {
  pointId: string | null;
  title: string;
  dimension: 'theory' | 'skill' | null;
}

export interface SectionCheckReport {
  wordCount: number;
  targetWords: number | null;
  minSuggestedWords: number | null;
  maxSuggestedWords: number | null;
  withinTargetRange: boolean | null;
  uncoveredPoints: UncoveredPointVO[];
  placeholderCount: number;
  checkedAt: string | null;
}

export interface SectionAssetVO {
  assetId: string;
  sourceUri: string;
}

/** W6 审定时间线单条（也内嵌在 W1/W2 latestReviewRecord） */
export interface TextbookReviewRecordVO {
  id?: string;
  action: 'submit' | 'approve' | 'return' | 'reopen';
  submitRevision: number;
  comment: string | null;
  actorName: string;
  createdAt: string;
}

/** W2 SectionContentVO（空正文契约：contentExists=false/contentStatus=null/contentMd=''/version=0） */
export interface SectionContentVO {
  sectionId: string;
  chapterId: string;
  projectId: string;
  sectionTitle: string;
  chapterTitle: string;
  targetWords: number | null;
  contentExists: boolean;
  contentMd: string;
  contentStatus: TextbookSectionContentStatus | null;
  wordCount: number;
  version: number;
  relations: string[];
  canWrite: boolean;
  canReview: boolean;
  readOnly: boolean;
  readOnlyReason: string | null;
  checkReport: SectionCheckReport | null;
  assets: SectionAssetVO[];
  submittedBy: string | null;
  submittedByName: string | null;
  submittedAt: string | null;
  submitRevision: number | null;
  currentSnapshotId: string | null;
  reviewedByName: string | null;
  reviewedAt: string | null;
  latestReviewRecord: TextbookReviewRecordVO | null;
}

// ------------------------------------------------------------------
// W5/W9 请求
// ------------------------------------------------------------------

export interface SaveSectionContentReq {
  contentMd: string;
  baseVersion: number;
}

export interface SubmitSectionContentReq {
  baseVersion: number;
}

export interface ReviewSectionContentReq {
  action: TextbookReviewAction;
  /** return/reopen 必填（1~1000 字），approve 可选 */
  comment?: string;
  baseVersion: number;
}

export interface RestoreSectionVersionReq {
  snapshotId: string;
  baseVersion: number;
}

// ------------------------------------------------------------------
// W7/W8 版本快照
// ------------------------------------------------------------------

export interface SectionVersionVO {
  id: string;
  snapshotNo: number;
  snapshotType: 'submit' | 'before_restore';
  submitRevision: number | null;
  wordCount: number;
  createdAt: string;
  createdByName: string;
}

export interface SectionVersionDetailVO extends SectionVersionVO {
  contentMd: string;
}

// ------------------------------------------------------------------
// A1~A3 图片
// ------------------------------------------------------------------

export interface SectionAssetUploadVO {
  assetId: string;
  sourceUri: string;
}

// ------------------------------------------------------------------
// 409 冲突 data（§6 通用约定）
// ------------------------------------------------------------------

/** 内容版本冲突（W3/W4/W5/W9/A3 返回 409） */
export interface VersionConflictData {
  reason: 'VERSION_CONFLICT';
  currentVersion: number;
  currentContentMd: string;
  currentUpdatedAt: string | null;
  currentUpdatedByName: string | null;
}

/** 分配版本冲突（C3/C4 返回 409） */
export interface AssignmentConflictData {
  reason: 'ASSIGNMENT_VERSION_CONFLICT';
  currentAssignment: TextbookAssignmentVO;
}

/** 整册导出模式（E3/E4） */
export type TextbookBookExportMode = 'review' | 'final';

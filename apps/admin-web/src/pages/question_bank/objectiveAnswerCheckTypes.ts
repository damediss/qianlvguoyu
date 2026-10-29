/**
 * 客观题答案 AI 检查类型（方案 §5 / §13）。
 * 雪花 ID（batchId/itemId）一律字符串，禁止 Number() 转换。
 */
export type ObjectiveAnswerResultCode =
  | 'consistent_high'
  | 'mismatch_high'
  | 'low_confidence'
  | 'invalid'
  | 'failed';

export type ObjectiveAnswerContentQuality = 'clean' | 'suspect' | 'invalid';
export type ObjectiveAnswerBatchStatus =
  | 'pending'
  | 'running'
  | 'pausing'
  | 'paused'
  | 'done'
  | 'failed';
export type ObjectiveAnswerReviewStatus =
  | 'unreviewed'
  | 'confirmed_correct'
  | 'needs_edit'
  | 'deferred';

export interface ObjectiveAnswerScope {
  workTypeId?: number | null;
  levelCode?: number | null;
  source?: number | null;
  types?: string[];
}

export interface ObjectiveAnswerSelectionReq {
  selectionMode: 'ids' | 'filter';
  detailIds?: number[] | null;
  scope?: ObjectiveAnswerScope | null;
}

export interface ObjectiveAnswerFilterOptions {
  workTypes: { id: string; title: string; path: string }[];
  levels: { code: number; label: string }[];
  sources: { value: number; label: string }[];
  types: { value: string; label: string }[];
}

export interface ObjectiveAnswerQuestionRow {
  detailId: number;
  questionType: string;
  questionTypeLabel: string;
  stemPreview: string;
  originalAnswer: string;
  source: number | null;
  sourceLabel: string;
  workTypeId: string | null;
  workTypeTitle: string | null;
  levelCode: number | null;
  levelLabel: string | null;
  courseId: number | null;
  courseTitle: string | null;
  invalidReason: string | null;
  contentQuality: ObjectiveAnswerContentQuality;
}

export interface ObjectiveAnswerQuestionPage {
  items: ObjectiveAnswerQuestionRow[];
  page: number;
  pageSize: number;
  total: number;
}

export interface ObjectiveAnswerPreview {
  selectionCount: number;
  overLimit: boolean;
  maxBatchSize: number;
  validCount?: number | null;
  invalidCount?: number | null;
  suspectContentCount?: number | null;
  invalidContentCount?: number | null;
  expectedCalls?: number | null;
  typeCounts?: Record<string, number> | null;
  invalidReasonCounts?: { code: string; count: number }[] | null;
  modelId: string;
  confidenceThreshold?: number | null;
  marginThreshold?: number | null;
}

export interface ObjectiveAnswerBatch {
  id: string;
  status: ObjectiveAnswerBatchStatus;
  selectionMode: 'ids' | 'filter';
  filterSummary: string;
  total: number;
  done: number;
  pending: number;
  processing: number;
  consistentHigh: number;
  mismatchHigh: number;
  lowConfidence: number;
  invalid: number;
  failed: number;
  contentSuspect: number;
  contentInvalid: number;
  expectedCalls: number;
  callCount: number;
  inputTokensExact: number;
  inputTokensEstimated: number;
  usageEstimatedCalls: number;
  outputTokens: number;
  modelId: string;
  jevBaseUrl?: string | null;
  jevConcurrency?: number | null;
  jevReadTimeoutSeconds?: number | null;
  promptVersion: string;
  confidenceThreshold: number;
  marginThreshold: number;
  createdBy: string | null;
  createdByName: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  failReason: string | null;
  canPause: boolean;
  canResume: boolean;
  canRetryFailed: boolean;
}

export interface ObjectiveAnswerContentWarning {
  ruleCode: string;
  severity: 'suspect' | 'invalid';
  location: 'stem' | 'option';
  optionId: string | null;
  matchedText: string | null;
  suggestedText: string | null;
  message: string;
}

export interface ObjectiveAnswerItem {
  id: string;
  batchId: string;
  detailId: number;
  questionType: string;
  questionTypeLabel: string;
  stemPreview: string;
  originalAnswer: string;
  aiAnswer: string;
  resultCode: ObjectiveAnswerResultCode | null;
  contentQuality: ObjectiveAnswerContentQuality;
  contentWarnings: ObjectiveAnswerContentWarning[];
  needsReview: boolean;
  consistent: boolean | null;
  referenceConfidence: number | null;
  aiConfidence: number | null;
  minMargin: number | null;
  decisionWarning: string | null;
  retryable: boolean;
  attempts: number;
  version: number;
  reviewStatus: ObjectiveAnswerReviewStatus;
  reviewNote: string | null;
  checkedAt: string | null;
  updatedAt: string | null;
}

export interface ObjectiveAnswerOptionLine {
  optionId: string;
  label: string;
  text: string;
  originalCorrect: boolean;
  aiCorrect: boolean | null;
  choice: string | null;
  confidence: number | null;
  referenceProbability: number | null;
  top1Probability: number | null;
  top2Probability: number | null;
  margin: number | null;
}

export interface ObjectiveAnswerCallLog {
  callId: string;
  chunkIndex: number;
  chunkCount: number;
  status: string;
  requestedModel: string | null;
  responseModel: string | null;
  confidenceDefinition: string | null;
  inputTokens: number | null;
  outputTokens: number | null;
  usageEstimated: boolean;
  durationMs: number | null;
  errorCode: string | null;
  errorMessage: string | null;
}

export interface ObjectiveAnswerItemDetail {
  item: ObjectiveAnswerItem;
  sourceLabel: string;
  workTypeTitle: string | null;
  courseTitle: string | null;
  levelCode: number | null;
  knowledgePath: string | null;
  contentSnapshot: string | null;
  stemText: string;
  resolveSnapshot: string | null;
  options: ObjectiveAnswerOptionLine[];
  calls: ObjectiveAnswerCallLog[];
  sourceChanged: boolean;
  modelId: string;
  promptVersion: string;
  requestedModel: string | null;
  responseModel: string | null;
  confidenceDefinition: string | null;
  failReason: string | null;
}

export interface ObjectiveAnswerItemFilter {
  resultCode?: string;
  contentQuality?: string;
  needsReview?: boolean;
  reviewStatus?: string;
  questionType?: string;
  keyword?: string;
}

export const RESULT_LABELS: Record<string, string> = {
  consistent_high: '高置信一致',
  mismatch_high: '高置信不一致',
  low_confidence: '低置信复核',
  invalid: '答案结构异常',
  failed: '检查失败',
};

export const CONTENT_QUALITY_LABELS: Record<string, string> = {
  clean: '正常',
  suspect: '格式疑似异常',
  invalid: '格式确定异常',
};

export const REVIEW_LABELS: Record<string, string> = {
  unreviewed: '未复核',
  confirmed_correct: '确认原答案无误',
  needs_edit: '需要修改',
  deferred: '暂缓处理',
};

export const BATCH_STATUS_LABELS: Record<string, string> = {
  pending: '待开始',
  running: '检查中',
  pausing: '暂停中',
  paused: '已暂停',
  done: '已完成',
  failed: '已失败',
};

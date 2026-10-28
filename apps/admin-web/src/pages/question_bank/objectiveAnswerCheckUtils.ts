import {
  BATCH_STATUS_LABELS,
  CONTENT_QUALITY_LABELS,
  RESULT_LABELS,
  REVIEW_LABELS,
} from './objectiveAnswerCheckTypes';
import type {
  ObjectiveAnswerBatchStatus,
  ObjectiveAnswerContentQuality,
  ObjectiveAnswerItem,
  ObjectiveAnswerResultCode,
} from './objectiveAnswerCheckTypes';

export { BATCH_STATUS_LABELS, CONTENT_QUALITY_LABELS, RESULT_LABELS, REVIEW_LABELS };

/** 五类结果的展示顺序与颜色（不能只靠颜色区分，必须同时有文字）。 */
export const RESULT_ORDER: ObjectiveAnswerResultCode[] = [
  'mismatch_high',
  'low_confidence',
  'consistent_high',
  'invalid',
  'failed',
];

export const RESULT_COLORS: Record<string, string> = {
  mismatch_high: 'red',
  low_confidence: 'orange',
  consistent_high: 'green',
  invalid: 'default',
  failed: 'volcano',
};

export const CONTENT_QUALITY_COLORS: Record<string, string> = {
  clean: 'default',
  suspect: 'gold',
  invalid: 'red',
};

export const BATCH_STATUS_COLORS: Record<string, string> = {
  pending: 'default',
  running: 'processing',
  pausing: 'warning',
  paused: 'warning',
  done: 'success',
  failed: 'error',
};

export function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return `${(Number(value) * 100).toFixed(1)}%`;
}

export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return Number(value).toLocaleString('zh-CN');
}

export function isPollingStatus(status: ObjectiveAnswerBatchStatus | undefined): boolean {
  return status === 'pending' || status === 'running' || status === 'pausing';
}

export function isTerminalStatus(status: ObjectiveAnswerBatchStatus | undefined): boolean {
  return status === 'paused' || status === 'done' || status === 'failed';
}

/** 默认“需人工复核”队列：结果已落定、未复核，且属于需复核结果或内容质量非 clean。 */
export function needsReviewOf(item: ObjectiveAnswerItem): boolean {
  if (!item.resultCode || item.reviewStatus !== 'unreviewed') return false;
  return (
    ['mismatch_high', 'low_confidence', 'invalid', 'failed'].includes(item.resultCode)
    || (item.contentQuality as ObjectiveAnswerContentQuality) !== 'clean'
  );
}

export function progressPercent(done: number, total: number): number {
  if (!total) return 0;
  return Math.min(100, Math.round((done / total) * 100));
}

/**
 * 结果 + 内容质量组合标签：suspect 必须独立可见，不能藏进置信度提示。
 */
export function itemBadges(item: ObjectiveAnswerItem): string[] {
  const badges: string[] = [];
  if (item.resultCode) badges.push(item.resultCode);
  if (item.contentQuality !== 'clean') badges.push(`content:${item.contentQuality}`);
  if (item.decisionWarning === 'AI_COMBINATION_INVALID') badges.push('组合无效');
  return badges;
}

export function extractErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message;
  const maybe = error as { response?: { data?: { message?: string } } };
  return maybe?.response?.data?.message || fallback;
}

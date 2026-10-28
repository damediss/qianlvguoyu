import { describe, expect, it } from 'vitest';
import {
  itemBadges,
  needsReviewOf,
  progressPercent,
  formatPercent,
  isPollingStatus,
  isTerminalStatus,
} from '../src/pages/question_bank/objectiveAnswerCheckUtils';
import type { ObjectiveAnswerItem } from '../src/pages/question_bank/objectiveAnswerCheckTypes';

function item(overrides: Partial<ObjectiveAnswerItem> = {}): ObjectiveAnswerItem {
  return {
    id: '2100000000000000002',
    batchId: '2100000000000000001',
    detailId: 12345,
    questionType: 'single',
    questionTypeLabel: '单选题',
    stemPreview: '题干',
    originalAnswer: 'B. 蓝色',
    aiAnswer: 'C. 黑色',
    resultCode: 'mismatch_high',
    contentQuality: 'clean',
    contentWarnings: [],
    needsReview: true,
    consistent: false,
    referenceConfidence: 0.06,
    aiConfidence: 0.91,
    minMargin: 0.78,
    decisionWarning: null,
    retryable: false,
    attempts: 1,
    version: 3,
    reviewStatus: 'unreviewed',
    reviewNote: null,
    checkedAt: '2026-10-26T10:01:00',
    updatedAt: '2026-10-26T10:01:00',
    ...overrides,
  };
}

describe('objectiveAnswerCheckUtils', () => {
  it('百分比与空值格式化，不把 null 显示成 0%', () => {
    expect(formatPercent(0.913)).toBe('91.3%');
    expect(formatPercent(0)).toBe('0.0%');
    expect(formatPercent(null)).toBe('-');
    expect(formatPercent(undefined)).toBe('-');
  });

  it('轮询只在 pending/running/pausing 开启，paused/failed 属终态刷新', () => {
    expect(isPollingStatus('pending')).toBe(true);
    expect(isPollingStatus('running')).toBe(true);
    expect(isPollingStatus('pausing')).toBe(true);
    expect(isPollingStatus('paused')).toBe(false);
    expect(isPollingStatus('failed')).toBe(false);
    expect(isTerminalStatus('paused')).toBe(true);
    expect(isTerminalStatus('failed')).toBe(true);
    expect(isTerminalStatus('done')).toBe(true);
    expect(isTerminalStatus('running')).toBe(false);
  });

  it('默认复核队列排除 pending/processing 与已复核行', () => {
    expect(needsReviewOf(item())).toBe(true);
    expect(needsReviewOf(item({ resultCode: null }))).toBe(false);
    expect(needsReviewOf(item({ reviewStatus: 'confirmed_correct' }))).toBe(false);
    expect(needsReviewOf(item({ resultCode: 'consistent_high' }))).toBe(false);
    expect(needsReviewOf(item({ resultCode: 'consistent_high', contentQuality: 'suspect' }))).toBe(
      true,
    );
    expect(needsReviewOf(item({ contentQuality: 'invalid' }))).toBe(true);
  });

  it('进度与组合标签：内容质量独立可见，不藏进置信度', () => {
    expect(progressPercent(300, 1200)).toBe(25);
    expect(progressPercent(0, 0)).toBe(0);
    expect(itemBadges(item({ contentQuality: 'suspect' }))).toEqual([
      'mismatch_high',
      'content:suspect',
    ]);
    expect(
      itemBadges(item({ contentQuality: 'clean', decisionWarning: 'AI_COMBINATION_INVALID' })),
    ).toContain('组合无效');
  });
});

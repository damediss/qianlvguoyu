import { isAnsweredLocal, type FormalExamLocalAnswer } from './formalExamAnswerState';
import { isRetryableLocal, type LocalFormalExamAnswer } from './idb/formalExamLocalAnswerTypes';

export type FormalExamSubmitBlockReason = 'conflict' | 'exam-time-expired' | 'unsynced' | null;

export interface FormalExamSubmitCounts {
  SYNCED: number;
  PENDING: number;
  SYNCING: number;
  ERROR: number;
  CONFLICT: number;
}

export interface FormalExamSubmitReadiness {
  block: FormalExamSubmitBlockReason;
  counts: FormalExamSubmitCounts;
  expiredCount: number;
  retryableCount: number;
  unsyncedCount: number;
}

export function emptySubmitCounts(): FormalExamSubmitCounts {
  return { SYNCED: 0, PENDING: 0, SYNCING: 0, ERROR: 0, CONFLICT: 0 };
}

export function inspectSubmitReadiness(records: LocalFormalExamAnswer[]): FormalExamSubmitReadiness {
  const counts = emptySubmitCounts();
  let expiredCount = 0;
  let retryableCount = 0;
  for (const record of records) {
    counts[record.syncStatus] = (counts[record.syncStatus] || 0) + 1;
    if (record.lastError === 'EXAM_TIME_EXPIRED') expiredCount += 1;
    if (isRetryableLocal(record)) retryableCount += 1;
  }
  const unsyncedCount = counts.PENDING + counts.SYNCING + retryableCount;
  if (counts.CONFLICT > 0) {
    return { block: 'conflict', counts, expiredCount, retryableCount, unsyncedCount };
  }
  if (expiredCount > 0) {
    return { block: 'exam-time-expired', counts, expiredCount, retryableCount, unsyncedCount };
  }
  if (retryableCount > 0 || counts.SYNCING > 0) {
    return { block: 'unsynced', counts, expiredCount, retryableCount, unsyncedCount };
  }
  return { block: null, counts, expiredCount, retryableCount, unsyncedCount };
}

export function buildAnswerVersions(
  questionIds: string[],
  records: LocalFormalExamAnswer[],
) {
  const byId = new Map(records.map((item) => [String(item.questionSnapshotId), item]));
  return questionIds.map((questionSnapshotId) => {
    const record = byId.get(String(questionSnapshotId));
    return {
      questionSnapshotId: String(questionSnapshotId),
      version: record?.version || 0,
    };
  });
}

export function countPaperAnswers(
  questions: Array<{ questionSnapshotId: string; type: string }>,
  states: Record<string, { answer: FormalExamLocalAnswer } | undefined>,
) {
  let answered = 0;
  for (const question of questions) {
    const state = states[question.questionSnapshotId];
    if (state && isAnsweredLocal(question.type, state.answer)) answered += 1;
  }
  return {
    total: questions.length,
    answered,
    unanswered: Math.max(0, questions.length - answered),
  };
}

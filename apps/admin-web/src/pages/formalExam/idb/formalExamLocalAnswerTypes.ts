import type { FormalExamLocalAnswer } from '../formalExamAnswerState';

export const FORMAL_EXAM_IDB_NAME = 'formal-exam-runtime';
export const FORMAL_EXAM_IDB_VERSION = 1;
export const FORMAL_EXAM_ANSWER_STORE = 'answers';

export type FormalExamSyncStatus = 'SYNCED' | 'PENDING' | 'SYNCING' | 'ERROR' | 'CONFLICT';

export interface LocalFormalExamAnswer {
  sessionId: string;
  candidateExamId: string;
  questionSnapshotId: string;
  answer: FormalExamLocalAnswer;
  marked: boolean;
  version: number;
  serverVersion: number;
  syncStatus: FormalExamSyncStatus;
  updatedAt: string;
  lastSyncAt?: string;
  lastError?: string;
}

export function localAnswerKey(
  sessionId: string,
  candidateExamId: string,
  questionSnapshotId: string,
): [string, string, string] {
  return [String(sessionId), String(candidateExamId), String(questionSnapshotId)];
}

export function isUnsyncedLocal(record: LocalFormalExamAnswer) {
  return record.syncStatus !== 'SYNCED';
}

export function isRetryableLocal(record: LocalFormalExamAnswer) {
  if (record.syncStatus === 'CONFLICT') return false;
  if (record.lastError === 'EXAM_TIME_EXPIRED') return false;
  return record.syncStatus === 'PENDING'
    || record.syncStatus === 'ERROR'
    || record.syncStatus === 'SYNCING';
}

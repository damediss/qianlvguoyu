export const FORMAL_EXAM_IDB_NAME = 'formal-exam-runtime';
export const FORMAL_EXAM_IDB_VERSION = 1;
export const FORMAL_EXAM_ANSWER_STORE = 'answers';
export function localAnswerKey(sessionId, candidateExamId, questionSnapshotId) {
    return [String(sessionId), String(candidateExamId), String(questionSnapshotId)];
}
export function isUnsyncedLocal(record) {
    return record.syncStatus !== 'SYNCED';
}
export function isRetryableLocal(record) {
    if (record.syncStatus === 'CONFLICT')
        return false;
    if (record.lastError === 'EXAM_TIME_EXPIRED')
        return false;
    return record.syncStatus === 'PENDING'
        || record.syncStatus === 'ERROR'
        || record.syncStatus === 'SYNCING';
}

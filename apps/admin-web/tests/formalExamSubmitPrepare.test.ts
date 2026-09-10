import { describe, expect, it } from 'vitest';
import { inspectSubmitReadiness, buildAnswerVersions, countPaperAnswers } from '../src/pages/formalExam/formalExamSubmitPrepare';
import type { LocalFormalExamAnswer } from '../src/pages/formalExam/idb/formalExamLocalAnswerTypes';

function record(partial: Partial<LocalFormalExamAnswer>): LocalFormalExamAnswer {
  return {
    sessionId: '900000000000001',
    candidateExamId: '900000000000101',
    questionSnapshotId: '9301',
    answer: { selectedOptionIds: ['9301:A'] },
    marked: false,
    version: 1,
    serverVersion: 1,
    syncStatus: 'SYNCED',
    updatedAt: '2026-09-08T09:00:00.000Z',
    ...partial,
  };
}

describe('formalExamSubmitPrepare', () => {
  it('blocks conflict and expired records before unsynced counts', () => {
    expect(inspectSubmitReadiness([record({ syncStatus: 'CONFLICT' })]).block).toBe('conflict');
    expect(inspectSubmitReadiness([
      record({ syncStatus: 'ERROR', lastError: 'EXAM_TIME_EXPIRED' }),
    ]).block).toBe('exam-time-expired');
    expect(inspectSubmitReadiness([record({ syncStatus: 'PENDING', version: 2 })]).block).toBe('unsynced');
    expect(inspectSubmitReadiness([record({ syncStatus: 'SYNCED' })]).block).toBeNull();
  });

  it('builds a version for every paper question including unanswered zeros', () => {
    expect(buildAnswerVersions(['9301', '9302', '9303'], [
      record({ questionSnapshotId: '9301', version: 5 }),
    ])).toEqual([
      { questionSnapshotId: '9301', version: 5 },
      { questionSnapshotId: '9302', version: 0 },
      { questionSnapshotId: '9303', version: 0 },
    ]);
  });

  it('counts unanswered separately from unsynced answers', () => {
    const counts = countPaperAnswers(
      [
        { questionSnapshotId: '9301', type: 'single' },
        { questionSnapshotId: '9302', type: 'multi' },
        { questionSnapshotId: '9303', type: 'fill' },
      ],
      {
        '9301': { answer: { selectedOptionIds: ['9301:A'] } },
        '9302': { answer: { selectedOptionIds: [] } },
      },
    );
    expect(counts).toEqual({ total: 3, answered: 1, unanswered: 2 });
  });
});

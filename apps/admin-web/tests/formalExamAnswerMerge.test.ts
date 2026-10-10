import { describe, expect, it } from 'vitest';
import { mergeServerAndLocal } from '../src/pages/formalExam/formalExamAnswerMerge';
import type { LocalFormalExamAnswer } from '../src/pages/formalExam/idb/formalExamLocalAnswerTypes';
import type { FormalExamPlayerSavedAnswer } from '../src/api/formalExamPlayer';

function server(partial: Partial<FormalExamPlayerSavedAnswer> & { questionSnapshotId: string }): FormalExamPlayerSavedAnswer {
  return {
    marked: false,
    version: 1,
    isAnswered: true,
    ...partial,
  };
}

function local(partial: Partial<LocalFormalExamAnswer>): LocalFormalExamAnswer {
  return {
    sessionId: 's1',
    candidateExamId: 'c1',
    questionSnapshotId: '9301',
    answer: { selectedOptionIds: ['9301:A'] },
    marked: false,
    version: 1,
    serverVersion: 0,
    syncStatus: 'PENDING',
    updatedAt: '2026-09-08T09:00:00.000Z',
    ...partial,
  };
}

describe('formal exam server + IndexedDB merge', () => {
  it('uses server-only answers as SYNCED', () => {
    const result = mergeServerAndLocal({
      sessionId: 's1',
      candidateExamId: 'c1',
      questionSnapshotId: '9301',
      server: server({
        questionSnapshotId: '9301',
        answer: { selectedOptionIds: ['9301:A'] },
        version: 2,
      }),
    });
    expect(result.reason).toBe('server-only');
    expect(result.record?.syncStatus).toBe('SYNCED');
    expect(result.record?.version).toBe(2);
    expect(result.record?.answer.selectedOptionIds).toEqual(['9301:A']);
  });

  it('keeps local pending answers when server has none', () => {
    const result = mergeServerAndLocal({
      sessionId: 's1',
      candidateExamId: 'c1',
      questionSnapshotId: '9301',
      local: local({ version: 4, syncStatus: 'PENDING', answer: { selectedOptionIds: ['9301:B'] } }),
    });
    expect(result.reason).toBe('local-only');
    expect(result.record?.syncStatus).toBe('PENDING');
    expect(result.record?.version).toBe(4);
  });

  it('keeps local when local.version is newer', () => {
    const result = mergeServerAndLocal({
      sessionId: 's1',
      candidateExamId: 'c1',
      questionSnapshotId: '9301',
      server: server({ questionSnapshotId: '9301', version: 2, answer: { selectedOptionIds: ['9301:A'] } }),
      local: local({ version: 5, syncStatus: 'ERROR', answer: { selectedOptionIds: ['9301:C'] } }),
    });
    expect(result.reason).toBe('local-newer');
    expect(result.record?.syncStatus).toBe('PENDING');
    expect(result.record?.version).toBe(5);
    expect(result.record?.answer.selectedOptionIds).toEqual(['9301:C']);
  });

  it('adopts server when server.version is newer', () => {
    const result = mergeServerAndLocal({
      sessionId: 's1',
      candidateExamId: 'c1',
      questionSnapshotId: '9301',
      server: server({ questionSnapshotId: '9301', version: 6, answer: { selectedOptionIds: ['9301:D'] } }),
      local: local({ version: 4, syncStatus: 'PENDING', answer: { selectedOptionIds: ['9301:A'] } }),
    });
    expect(result.reason).toBe('server-newer');
    expect(result.record?.syncStatus).toBe('SYNCED');
    expect(result.record?.version).toBe(6);
    expect(result.record?.answer.selectedOptionIds).toEqual(['9301:D']);
  });

  it('marks CONFLICT when versions match but payload differs', () => {
    const result = mergeServerAndLocal({
      sessionId: 's1',
      candidateExamId: 'c1',
      questionSnapshotId: '9301',
      server: server({ questionSnapshotId: '9301', version: 3, answer: { selectedOptionIds: ['9301:A'] } }),
      local: local({ version: 3, answer: { selectedOptionIds: ['9301:B'] } }),
    });
    expect(result.reason).toBe('conflict');
    expect(result.record?.syncStatus).toBe('CONFLICT');
    expect(result.record?.lastError).toBe('VERSION_PAYLOAD_CONFLICT');
  });

  it('marks SYNCED when versions and payload match', () => {
    const result = mergeServerAndLocal({
      sessionId: 's1',
      candidateExamId: 'c1',
      questionSnapshotId: '9301',
      server: server({ questionSnapshotId: '9301', version: 3, answer: { selectedOptionIds: ['9301:A'] }, marked: true }),
      local: local({ version: 3, marked: true, answer: { selectedOptionIds: ['9301:A'] }, syncStatus: 'PENDING' }),
    });
    expect(result.reason).toBe('same-synced');
    expect(result.record?.syncStatus).toBe('SYNCED');
  });
});

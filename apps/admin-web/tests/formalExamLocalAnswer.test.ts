import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { waitFor } from '@testing-library/react';
import { FormalExamLocalAnswerRepository } from '../src/pages/formalExam/idb/FormalExamLocalAnswerRepository';
import { FORMAL_EXAM_IDB_NAME } from '../src/pages/formalExam/idb/formalExamLocalAnswerTypes';
import type { LocalFormalExamAnswer } from '../src/pages/formalExam/idb/formalExamLocalAnswerTypes';
import { FormalExamAnswerSyncService } from '../src/pages/formalExam/FormalExamAnswerSyncService';

function sample(partial: Partial<LocalFormalExamAnswer>): LocalFormalExamAnswer {
  return {
    sessionId: '2097139390480236546',
    candidateExamId: '900000000000101',
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

async function deleteDb(repo: FormalExamLocalAnswerRepository) {
  await repo.close();
  await new Promise<void>((resolve) => {
    const request = indexedDB.deleteDatabase(FORMAL_EXAM_IDB_NAME);
    request.onsuccess = () => resolve();
    request.onerror = () => resolve();
    request.onblocked = () => resolve();
  });
}

describe('FormalExamLocalAnswerRepository', () => {
  const repo = new FormalExamLocalAnswerRepository();

  beforeEach(async () => {
    await deleteDb(repo);
  });

  afterEach(async () => {
    await deleteDb(repo);
  });

  it('stores snowflake ids as strings and isolates candidate exams', async () => {
    await repo.upsert(sample({ version: 3, answer: { selectedOptionIds: ['9301:B'] } }));
    await repo.upsert(sample({
      candidateExamId: '900000000000202',
      version: 8,
      answer: { selectedOptionIds: ['9301:C'] },
    }));

    const own = await repo.get('2097139390480236546', '900000000000101', '9301');
    const other = await repo.getAllByScope('2097139390480236546', '900000000000202');
    const ownScope = await repo.getAllByScope('2097139390480236546', '900000000000101');

    expect(own?.questionSnapshotId).toBe('9301');
    expect(own?.sessionId).toBe('2097139390480236546');
    expect(own?.candidateExamId).toBe('900000000000101');
    expect(own?.answer.selectedOptionIds).toEqual(['9301:B']);
    expect(ownScope).toHaveLength(1);
    expect(other[0].answer.selectedOptionIds).toEqual(['9301:C']);
    expect(ownScope[0].answer.selectedOptionIds).toEqual(['9301:B']);
  });

  it('does not mark a newer local version as SYNCED', async () => {
    await repo.upsert(sample({ version: 7, syncStatus: 'PENDING' }));
    const result = await repo.markSynced(
      '2097139390480236546',
      '900000000000101',
      '9301',
      6,
      { selectedOptionIds: ['9301:A'] },
      false,
    );
    expect(result?.version).toBe(7);
    expect(result?.syncStatus).toBe('PENDING');
  });

  it('recovers stuck SYNCING records to PENDING', async () => {
    await repo.upsert(sample({ syncStatus: 'SYNCING', version: 4 }));
    await repo.recoverStuckSyncing('2097139390480236546', '900000000000101');
    const row = await repo.get('2097139390480236546', '900000000000101', '9301');
    expect(row?.syncStatus).toBe('PENDING');
  });
});

describe('FormalExamAnswerSyncService', () => {
  const repo = new FormalExamLocalAnswerRepository();

  beforeEach(async () => {
    await deleteDb(repo);
  });

  afterEach(async () => {
    await deleteDb(repo);
  });

  it('compresses to the latest version and ignores a stale success', async () => {
    await repo.upsert(sample({ version: 5, answer: { selectedOptionIds: ['9301:A'] } }));
    let resolveFirst: ((value: unknown) => void) | undefined;
    const calls: number[] = [];
    const sync = new FormalExamAnswerSyncService(repo, async (_sessionId, body) => {
      calls.push(body.answers[0].version);
      if (calls.length === 1) {
        await new Promise((resolve) => {
          resolveFirst = resolve;
        });
      }
      return {
        results: body.answers.map((item) => ({
          questionSnapshotId: item.questionSnapshotId,
          version: item.version,
          isAnswered: true,
          marked: false,
          savedAt: '2026-09-08T09:00:10',
          answeredCount: 1,
          serverTime: '2026-09-08T09:00:10',
          accepted: true,
          status: 'SYNCED',
          answer: item.answer,
        })),
        serverTime: '2026-09-08T09:00:10',
      };
    });
    sync.setOnline(true);
    sync.start({ sessionId: '2097139390480236546', candidateExamId: '900000000000101' });
    await waitFor(() => expect(calls.length).toBe(1));
    await repo.upsert(sample({ version: 7, answer: { selectedOptionIds: ['9301:C'] } }));
    sync.schedule(0);
    resolveFirst?.(undefined);
    await waitFor(() => expect(calls).toContain(7));
    await waitFor(async () => {
      const row = await repo.get('2097139390480236546', '900000000000101', '9301');
      expect(row?.version).toBe(7);
      expect(row?.syncStatus).toBe('SYNCED');
      expect(row?.answer.selectedOptionIds).toEqual(['9301:C']);
    });
    expect(calls).toContain(5);
    sync.stop();
  });

  it('stops retrying EXAM_TIME_EXPIRED', async () => {
    await repo.upsert(sample({ version: 2 }));
    const sync = new FormalExamAnswerSyncService(repo, async () => ({
      results: [{
        questionSnapshotId: '9301',
        version: 2,
        isAnswered: true,
        marked: false,
        savedAt: null,
        answeredCount: 0,
        serverTime: '2026-09-08T10:30:00',
        accepted: false,
        status: 'EXAM_TIME_EXPIRED',
        conflictCode: 'EXAM_TIME_EXPIRED',
      }],
      serverTime: '2026-09-08T10:30:00',
    }));
    sync.setOnline(true);
    sync.start({ sessionId: '2097139390480236546', candidateExamId: '900000000000101' });
    await waitFor(async () => {
      const row = await repo.get('2097139390480236546', '900000000000101', '9301');
      expect(row?.lastError).toBe('EXAM_TIME_EXPIRED');
      expect(row?.syncStatus).toBe('ERROR');
    });
    expect(await repo.listRetryable('2097139390480236546', '900000000000101')).toEqual([]);
    sync.stop();
  });

  it('forceSync runs immediately without waiting for the 20s timer', async () => {
    await repo.upsert(sample({ version: 3, syncStatus: 'PENDING' }));
    const sync = new FormalExamAnswerSyncService(repo, async (_sessionId, body) => ({
      results: body.answers.map((item) => ({
        questionSnapshotId: item.questionSnapshotId,
        version: item.version,
        isAnswered: true,
        marked: false,
        savedAt: '2026-09-08T09:00:10',
        answeredCount: 1,
        serverTime: '2026-09-08T09:00:10',
        accepted: true,
        status: 'SYNCED',
        answer: item.answer,
      })),
      serverTime: '2026-09-08T09:00:10',
    }));
    sync.setOnline(true);
    sync.start({ sessionId: '2097139390480236546', candidateExamId: '900000000000101' });
    await sync.forceSync(5000);
    const row = await repo.get('2097139390480236546', '900000000000101', '9301');
    expect(row?.syncStatus).toBe('SYNCED');
    sync.stop();
  });
});

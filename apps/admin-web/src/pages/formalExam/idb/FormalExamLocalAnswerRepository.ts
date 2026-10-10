import type { FormalExamLocalAnswer } from '../formalExamAnswerState';
import {
  FORMAL_EXAM_ANSWER_STORE,
  FORMAL_EXAM_IDB_NAME,
  FORMAL_EXAM_IDB_VERSION,
  isRetryableLocal,
  isUnsyncedLocal,
  localAnswerKey,
  type LocalFormalExamAnswer,
} from './formalExamLocalAnswerTypes';

function requestToPromise<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error('IndexedDB request failed'));
  });
}

function transactionDone(tx: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error || new Error('IndexedDB transaction failed'));
    tx.onabort = () => reject(tx.error || new Error('IndexedDB transaction aborted'));
  });
}

export class FormalExamLocalAnswerRepository {
  private openPromise: Promise<IDBDatabase> | null = null;

  open(): Promise<IDBDatabase> {
    if (this.openPromise) return this.openPromise;
    this.openPromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(FORMAL_EXAM_IDB_NAME, FORMAL_EXAM_IDB_VERSION);
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(FORMAL_EXAM_ANSWER_STORE)) {
          const store = db.createObjectStore(FORMAL_EXAM_ANSWER_STORE, {
            keyPath: ['sessionId', 'candidateExamId', 'questionSnapshotId'],
          });
          store.createIndex('byScope', ['sessionId', 'candidateExamId'], { unique: false });
          store.createIndex('bySession', 'sessionId', { unique: false });
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => {
        this.openPromise = null;
        reject(request.error || new Error('IndexedDB open failed'));
      };
    });
    return this.openPromise;
  }

  async get(
    sessionId: string,
    candidateExamId: string,
    questionSnapshotId: string,
  ): Promise<LocalFormalExamAnswer | undefined> {
    const db = await this.open();
    const tx = db.transaction(FORMAL_EXAM_ANSWER_STORE, 'readonly');
    const value = await requestToPromise(
      tx.objectStore(FORMAL_EXAM_ANSWER_STORE).get(localAnswerKey(sessionId, candidateExamId, questionSnapshotId)),
    );
    await transactionDone(tx);
    return value ? normalizeRecord(value) : undefined;
  }

  async getAllByScope(sessionId: string, candidateExamId: string): Promise<LocalFormalExamAnswer[]> {
    const db = await this.open();
    const tx = db.transaction(FORMAL_EXAM_ANSWER_STORE, 'readonly');
    const index = tx.objectStore(FORMAL_EXAM_ANSWER_STORE).index('byScope');
    const rows = await requestToPromise(index.getAll([String(sessionId), String(candidateExamId)]));
    await transactionDone(tx);
    return (rows || []).map(normalizeRecord);
  }

  async upsert(record: LocalFormalExamAnswer): Promise<LocalFormalExamAnswer> {
    const next = normalizeRecord({
      ...record,
      sessionId: String(record.sessionId),
      candidateExamId: String(record.candidateExamId),
      questionSnapshotId: String(record.questionSnapshotId),
      updatedAt: record.updatedAt || new Date().toISOString(),
    });
    const db = await this.open();
    const tx = db.transaction(FORMAL_EXAM_ANSWER_STORE, 'readwrite');
    tx.objectStore(FORMAL_EXAM_ANSWER_STORE).put(next);
    await transactionDone(tx);
    return next;
  }

  async markSynced(
    sessionId: string,
    candidateExamId: string,
    questionSnapshotId: string,
    version: number,
    answer: FormalExamLocalAnswer,
    marked: boolean,
  ): Promise<LocalFormalExamAnswer | undefined> {
    const current = await this.get(sessionId, candidateExamId, questionSnapshotId);
    if (!current || current.version !== version) {
      return current;
    }
    return this.upsert({
      ...current,
      answer,
      marked,
      version,
      serverVersion: version,
      syncStatus: 'SYNCED',
      lastSyncAt: new Date().toISOString(),
      lastError: undefined,
    });
  }

  async markError(
    sessionId: string,
    candidateExamId: string,
    questionSnapshotId: string,
    version: number,
    lastError: string,
    syncStatus: LocalFormalExamAnswer['syncStatus'] = 'ERROR',
  ): Promise<LocalFormalExamAnswer | undefined> {
    const current = await this.get(sessionId, candidateExamId, questionSnapshotId);
    if (!current || current.version !== version) {
      return current;
    }
    return this.upsert({
      ...current,
      syncStatus,
      lastError,
    });
  }

  async listRetryable(sessionId: string, candidateExamId: string): Promise<LocalFormalExamAnswer[]> {
    const rows = await this.getAllByScope(sessionId, candidateExamId);
    return rows.filter(isRetryableLocal);
  }

  async countUnsynced(sessionId: string, candidateExamId: string): Promise<number> {
    const rows = await this.getAllByScope(sessionId, candidateExamId);
    return rows.filter(isUnsyncedLocal).length;
  }

  async recoverStuckSyncing(sessionId: string, candidateExamId: string): Promise<void> {
    const rows = await this.getAllByScope(sessionId, candidateExamId);
    for (const row of rows) {
      if (row.syncStatus === 'SYNCING') {
        await this.upsert({ ...row, syncStatus: 'PENDING' });
      }
    }
  }

  async close(): Promise<void> {
    if (!this.openPromise) return;
    try {
      const db = await this.openPromise;
      db.close();
    } catch {
      // ignore close errors so tests can recreate the database
    }
    this.openPromise = null;
  }
}

function normalizeRecord(record: LocalFormalExamAnswer): LocalFormalExamAnswer {
  const answer: FormalExamLocalAnswer = {
    selectedOptionIds: [...(record.answer?.selectedOptionIds || [])],
    blanks: [...(record.answer?.blanks || [])],
  };
  return {
    ...record,
    sessionId: String(record.sessionId),
    candidateExamId: String(record.candidateExamId),
    questionSnapshotId: String(record.questionSnapshotId),
    answer,
    marked: Boolean(record.marked),
    version: record.version || 0,
    serverVersion: record.serverVersion || 0,
    syncStatus: record.syncStatus,
  };
}

export const formalExamLocalAnswerRepository = new FormalExamLocalAnswerRepository();

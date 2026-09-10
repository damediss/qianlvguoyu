import {
  FormalExamApiError,
  syncFormalExamAnswers,
  type FormalExamPlayerAnswerSaveResult,
} from '../../api/formalExamPlayer';
import { FormalExamLocalAnswerRepository } from './idb/FormalExamLocalAnswerRepository';
import { isRetryableLocal, type LocalFormalExamAnswer } from './idb/formalExamLocalAnswerTypes';

export const FORMAL_EXAM_SYNC_BATCH_SIZE = 20;

export interface FormalExamSyncScope {
  sessionId: string;
  candidateExamId: string;
}

type SyncListener = (event: { scope: FormalExamSyncScope; records: LocalFormalExamAnswer[] }) => void;

export class FormalExamAnswerSyncService {
  private readonly listeners = new Set<SyncListener>();
  private running = false;
  private queued = false;
  private timer: number | null = null;
  private interval: number | null = null;
  private backoffMs = 3000;
  private scope: FormalExamSyncScope | null = null;
  private online = typeof navigator === 'undefined' ? true : navigator.onLine;

  private started = false;

  constructor(
    private readonly repository: FormalExamLocalAnswerRepository,
    private readonly syncFn: typeof syncFormalExamAnswers = syncFormalExamAnswers,
  ) {}

  start(scope: FormalExamSyncScope) {
    this.scope = {
      sessionId: String(scope.sessionId),
      candidateExamId: String(scope.candidateExamId),
    };
    if (this.started) {
      this.schedule(0);
      return;
    }
    this.started = true;
    if (typeof window !== 'undefined') {
      window.addEventListener('online', this.onOnline);
      window.addEventListener('offline', this.onOffline);
    }
    if (this.interval != null && typeof window !== 'undefined') {
      window.clearInterval(this.interval);
    }
    if (typeof window !== 'undefined') {
      this.interval = window.setInterval(() => {
        if (this.online) this.schedule(0);
      }, 20000);
    }
    this.schedule(0);
  }

  stop() {
    this.started = false;
    this.scope = null;
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', this.onOnline);
      window.removeEventListener('offline', this.onOffline);
    }
    if (this.timer != null && typeof window !== 'undefined') window.clearTimeout(this.timer);
    if (this.interval != null && typeof window !== 'undefined') window.clearInterval(this.interval);
    this.timer = null;
    this.interval = null;
  }

  subscribe(listener: SyncListener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  setOnline(online: boolean) {
    this.online = online;
    if (online && this.started) {
      this.backoffMs = 3000;
      this.schedule(0);
    }
  }

  isOnline() {
    return this.online;
  }

  schedule(delayMs = 0) {
    if (!this.started || !this.scope) return;
    if (typeof window === 'undefined') {
      void this.syncPending();
      return;
    }
    if (this.timer != null) window.clearTimeout(this.timer);
    this.timer = window.setTimeout(() => {
      this.timer = null;
      void this.syncPending();
    }, delayMs);
  }

  async forceSync(timeoutMs = 15000) {
    if (!this.started || !this.scope) return;
    const previous = this.online;
    this.online = true;
    try {
      await Promise.race([
        this.runForcedSync(),
        new Promise<never>((_, reject) => {
          setTimeout(() => reject(new Error('SYNC_TIMEOUT')), timeoutMs);
        }),
      ]);
    } finally {
      this.online = typeof navigator === 'undefined' ? previous : navigator.onLine;
    }
  }

  private async runForcedSync() {
    while (this.running) {
      await new Promise((resolve) => setTimeout(resolve, 40));
    }
    await this.syncPending();
  }

  private onOnline = () => this.setOnline(true);
  private onOffline = () => this.setOnline(false);

  resume() {
    if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return;
    this.schedule(0);
  }

  async syncPending() {
    if (!this.started || !this.scope) return;
    if (this.running) {
      this.queued = true;
      while (this.running) {
        await new Promise((resolve) => setTimeout(resolve, 40));
      }
      return;
    }
    if (!this.online) {
      await this.emit();
      return;
    }
    this.running = true;
    try {
      do {
        this.queued = false;
        const pending = await this.repository.listRetryable(this.scope.sessionId, this.scope.candidateExamId);
        if (!pending.length) {
          this.backoffMs = 3000;
          await this.emit();
          return;
        }
        for (let index = 0; index < pending.length; index += FORMAL_EXAM_SYNC_BATCH_SIZE) {
          await this.syncChunk(pending.slice(index, index + FORMAL_EXAM_SYNC_BATCH_SIZE));
        }
      } while (this.queued);
      this.backoffMs = 3000;
    } catch {
      this.backoffMs = Math.min(this.backoffMs * 2, 30000);
      const jitter = Math.floor(Math.random() * Math.min(1000, this.backoffMs));
      if (this.online) this.schedule(this.backoffMs + jitter);
    } finally {
      this.running = false;
      await this.emit();
    }
  }

  private async syncChunk(chunk: LocalFormalExamAnswer[]) {
    if (!this.scope) return;
    const inFlight: LocalFormalExamAnswer[] = [];
    for (const item of chunk) {
      const latest = await this.repository.get(
        item.sessionId,
        item.candidateExamId,
        item.questionSnapshotId,
      );
      if (!latest || !isRetryableLocal(latest)) continue;
      inFlight.push(await this.repository.upsert({ ...latest, syncStatus: 'SYNCING' }));
    }
    if (!inFlight.length) return;

    let results: FormalExamPlayerAnswerSaveResult[] = [];
    try {
      const response = await this.syncFn(this.scope.sessionId, {
        answers: inFlight.map((item) => ({
          questionSnapshotId: item.questionSnapshotId,
          answer: item.answer,
          marked: item.marked,
          version: item.version,
        })),
      });
      results = response.results || [];
    } catch (error) {
      const apiError = error instanceof FormalExamApiError ? error : null;
      if (apiError?.code === 'CANCELED') {
        for (const item of inFlight) {
          await this.repository.upsert({ ...item, syncStatus: 'PENDING' });
        }
        return;
      }
      const expired = Boolean(apiError?.message.includes('考试时间已到'));
      const status = apiError?.status;
      let code = expired ? 'EXAM_TIME_EXPIRED' : 'NETWORK';
      let retry = !expired;
      if (status === 401) {
        code = 'UNAUTHORIZED';
        retry = false;
      } else if (status === 403) {
        code = 'FORBIDDEN';
        retry = false;
      } else if (status === 409) {
        code = apiError?.code || 'CONFLICT';
        retry = false;
      }
      for (const item of inFlight) {
        await this.repository.markError(
          item.sessionId,
          item.candidateExamId,
          item.questionSnapshotId,
          item.version,
          code,
          'ERROR',
        );
      }
      if (retry) {
        throw error;
      }
      return;
    }

    const byId = new Map(results.map((item) => [String(item.questionSnapshotId), item]));
    for (const item of inFlight) {
      await this.applyResult(item, byId.get(item.questionSnapshotId));
    }
  }

  private async applyResult(local: LocalFormalExamAnswer, result?: FormalExamPlayerAnswerSaveResult) {
    const latest = await this.repository.get(local.sessionId, local.candidateExamId, local.questionSnapshotId);
    if (!latest || latest.version !== local.version) {
      return;
    }
    if (!result) {
      await this.repository.markError(
        local.sessionId,
        local.candidateExamId,
        local.questionSnapshotId,
        local.version,
        'NETWORK',
      );
      return;
    }
    const status = result.status || (result.accepted ? 'SYNCED' : result.conflictCode) || 'ERROR';
    if (result.accepted) {
      if (result.version !== latest.version) return;
      await this.repository.markSynced(
        latest.sessionId,
        latest.candidateExamId,
        latest.questionSnapshotId,
        result.version,
        result.answer || latest.answer,
        result.marked,
      );
      return;
    }
    if (status === 'STALE_VERSION' && result.answer) {
      await this.repository.upsert({
        ...latest,
        answer: {
          selectedOptionIds: result.answer.selectedOptionIds || [],
          blanks: result.answer.blanks || [],
        },
        marked: result.marked,
        version: result.version,
        serverVersion: result.version,
        syncStatus: 'SYNCED',
        lastSyncAt: new Date().toISOString(),
        lastError: undefined,
      });
      return;
    }
    if (status === 'VERSION_PAYLOAD_CONFLICT') {
      await this.repository.markError(
        latest.sessionId,
        latest.candidateExamId,
        latest.questionSnapshotId,
        latest.version,
        'VERSION_PAYLOAD_CONFLICT',
        'CONFLICT',
      );
      return;
    }
    if (status === 'EXAM_TIME_EXPIRED' || result.conflictCode === 'EXAM_TIME_EXPIRED') {
      await this.repository.markError(
        latest.sessionId,
        latest.candidateExamId,
        latest.questionSnapshotId,
        latest.version,
        'EXAM_TIME_EXPIRED',
        'ERROR',
      );
      return;
    }
    await this.repository.markError(
      latest.sessionId,
      latest.candidateExamId,
      latest.questionSnapshotId,
      latest.version,
      status,
    );
  }

  private async emit() {
    if (!this.scope) return;
    const records = await this.repository.getAllByScope(this.scope.sessionId, this.scope.candidateExamId);
    for (const listener of this.listeners) {
      listener({ scope: this.scope, records });
    }
  }
}

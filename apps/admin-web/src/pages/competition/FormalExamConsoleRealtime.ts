import { createFormalExamConsoleRealtimeTicket } from '../../api/competition';
import type { PageResponse } from '../../types/api';
import type {
  CompetitionFormalCandidateExam,
  CompetitionFormalExamSession,
  CompetitionId,
} from '../../types/competition';
import {
  buildFormalExamWsUrl,
  detectFormalExamSeqGap,
  formalExamRealtimeBackoffMs,
  rememberFormalExamEventId,
  shouldOpenFormalExamRealtimeSocket,
  type FormalExamRealtimeEnvelope,
} from '../formalExam/FormalExamRealtimeClient';

export const FORMAL_EXAM_CONSOLE_DISCONNECT_BANNER = '实时连接中断/正在重连';

export const FORMAL_EXAM_CONSOLE_REALTIME = {
  SESSION_STATUS_CHANGED: 'session.status.changed',
  CANDIDATE_STATUS_CHANGED: 'candidate.status.changed',
  CANDIDATE_PROGRESS_CHANGED: 'candidate.progress.changed',
  CANDIDATE_PRESENCE_CHANGED: 'candidate.presence.changed',
  ACCESS_SESSION_KICKED: 'access-session.kicked',
  FORCE_SUBMIT_COMPLETED: 'candidate.force-submit.completed',
  SNAPSHOT_RELOAD_REQUIRED: 'snapshot.reload.required',
  APPLICATION_HEARTBEAT: 'application.heartbeat',
  CONSOLE_SUBSCRIBE_PAGE: 'console.subscribe-page',
} as const;

export const FORMAL_EXAM_CONNECTION_LABELS: Record<string, { label: string; color: string }> = {
  online: { label: '在线', color: 'success' },
  briefly_offline: { label: '短暂离线', color: 'warning' },
  kicked: { label: '被清退', color: 'error' },
  token_expired: { label: 'token 过期', color: 'default' },
  submitted: { label: '已交卷', color: 'success' },
  absent: { label: '缺考', color: 'default' },
};

const COUNT_KEYS: Record<string, keyof CompetitionFormalExamSession> = {
  waiting_enter: 'waitingEnterCount',
  ready: 'readyCount',
  running: 'runningCount',
  submitted: 'submittedCount',
  auto_submitted: 'autoSubmittedCount',
  forced_submitted: 'forcedSubmittedCount',
  absent: 'absentCount',
};

export type FormalExamConsoleRealtimeHandle = {
  stop: () => void;
  reconnect: () => void;
  subscribePage: (candidateExamIds: string[]) => void;
};

export function shouldOpenFormalExamConsoleRealtimeSocket() {
  return shouldOpenFormalExamRealtimeSocket();
}

export function formalExamConsoleSessionQueryKey(sessionId: CompetitionId | undefined) {
  return ['competition-formal-exam-session', sessionId] as const;
}

export function formalExamConsoleCandidatesQueryKey(
  sessionId: CompetitionId | undefined,
  keyword: string,
  paperSnapshotId: string | undefined,
  status: string | undefined,
  page: number,
  pageSize: number,
) {
  return [
    'competition-formal-exam-session-candidates',
    sessionId,
    keyword,
    paperSnapshotId,
    status,
    page,
    pageSize,
  ] as const;
}

export function formalExamConsolePollIntervalMs(
  _realtimeConnected?: boolean,
  options?: { testRuntime?: boolean; random?: () => number },
) {
  // Console is a single operator page. Do not inherit the 60–120s player backoff:
  // a connected-but-silent WebSocket would freeze answeredCount / 交卷 for minutes.
  void _realtimeConnected;
  void options?.random;
  return 5000;
}

export function resolveFormalExamConnectionState(input: {
  candidateStatus?: string | null;
  accessSessionStatus?: string | null;
  lastSeenAt?: string | null;
  nowMs?: number;
  onlineWithinSeconds?: number;
}) {
  if (input.candidateStatus === 'absent') return 'absent';
  if (input.candidateStatus === 'submitted'
    || input.candidateStatus === 'auto_submitted'
    || input.candidateStatus === 'forced_submitted') {
    return 'submitted';
  }
  if (input.accessSessionStatus === 'kicked') return 'kicked';
  if (input.accessSessionStatus === 'expired') return 'token_expired';
  const windowSeconds = Math.max(20, input.onlineWithinSeconds ?? 60);
  const lastSeen = input.lastSeenAt ? Date.parse(input.lastSeenAt) : Number.NaN;
  const now = input.nowMs ?? Date.now();
  if (Number.isFinite(lastSeen) && lastSeen >= now - windowSeconds * 1000) {
    return 'online';
  }
  return 'briefly_offline';
}

export function applyFormalExamSessionCountDelta(
  session: CompetitionFormalExamSession | undefined,
  previousStatus?: string | null,
  nextStatus?: string | null,
): CompetitionFormalExamSession | undefined {
  if (!session || !previousStatus || !nextStatus || previousStatus === nextStatus) {
    return session;
  }
  const next = { ...session };
  const previousKey = COUNT_KEYS[previousStatus];
  const nextKey = COUNT_KEYS[nextStatus];
  if (previousKey) {
    const current = Number(next[previousKey] ?? 0);
    (next as Record<string, unknown>)[previousKey] = Math.max(0, current - 1);
  }
  if (nextKey) {
    const current = Number(next[nextKey] ?? 0);
    (next as Record<string, unknown>)[nextKey] = current + 1;
  }
  next.submittedTotal = (next.submittedCount ?? 0)
    + (next.autoSubmittedCount ?? 0)
    + (next.forcedSubmittedCount ?? 0);
  return next;
}

export function patchFormalExamConsoleCandidatePage(
  page: PageResponse<CompetitionFormalCandidateExam> | undefined,
  candidateExamId: string | null | undefined,
  patch: Partial<CompetitionFormalCandidateExam>,
): { page?: PageResponse<CompetitionFormalCandidateExam>; patched: boolean } {
  if (!page || !candidateExamId) {
    return { page, patched: false };
  }
  const target = String(candidateExamId);
  let patched = false;
  const items = (page.items || []).map((row) => {
    if (String(row.id) !== target) return row;
    patched = true;
    return { ...row, ...patch };
  });
  return { page: patched ? { ...page, items } : page, patched };
}

export function isFormalExamConsoleCurrentPageCandidate(
  pageCandidateIds: string[],
  candidateExamId?: string | null,
) {
  if (!candidateExamId) return false;
  const target = String(candidateExamId);
  return pageCandidateIds.some((id) => String(id) === target);
}

export type FormalExamConsoleApplyResult = {
  lastSeq: number | null;
  duplicate: boolean;
  gap: boolean;
  invalidate: boolean;
  session?: CompetitionFormalExamSession;
  page?: PageResponse<CompetitionFormalCandidateExam>;
};

export function applyFormalExamConsoleEnvelope(input: {
  envelope: FormalExamRealtimeEnvelope;
  seen: Set<string>;
  lastSeq: number | null;
  pageCandidateIds: string[];
  session?: CompetitionFormalExamSession;
  page?: PageResponse<CompetitionFormalCandidateExam>;
  nowMs?: number;
}): FormalExamConsoleApplyResult {
  const duplicate = rememberFormalExamEventId(input.seen, input.envelope.eventId);
  if (duplicate) {
    return {
      lastSeq: input.lastSeq,
      duplicate: true,
      gap: false,
      invalidate: false,
      session: input.session,
      page: input.page,
    };
  }
  const seqState = detectFormalExamSeqGap(input.lastSeq, input.envelope.seq);
  let session = input.session;
  let page = input.page;
  let invalidate = seqState.gap || input.envelope.type === FORMAL_EXAM_CONSOLE_REALTIME.SNAPSHOT_RELOAD_REQUIRED;
  const candidateExamId = input.envelope.candidateExamId == null
    ? undefined
    : String(input.envelope.candidateExamId);
  const onPage = isFormalExamConsoleCurrentPageCandidate(input.pageCandidateIds, candidateExamId)
    || Boolean(candidateExamId && input.page?.items.some((row) => String(row.id) === candidateExamId));
  const payload = input.envelope.payload ?? undefined;
  const type = input.envelope.type;

  if (type === FORMAL_EXAM_CONSOLE_REALTIME.CANDIDATE_STATUS_CHANGED
    || type === FORMAL_EXAM_CONSOLE_REALTIME.FORCE_SUBMIT_COMPLETED) {
    const status = payloadString(payload, 'status');
    const previousStatus = payloadString(payload, 'previousStatus');
    if (!previousStatus) {
      invalidate = true;
    } else {
      session = applyFormalExamSessionCountDelta(session, previousStatus, status);
    }
    if (onPage && status) {
      const current = page?.items.find((row) => String(row.id) === candidateExamId);
      const patched = patchFormalExamConsoleCandidatePage(page, candidateExamId, {
        status,
        connectionState: resolveFormalExamConnectionState({
          candidateStatus: status,
          accessSessionStatus: current?.accessSessionStatus,
          lastSeenAt: current?.lastSeenAt,
          nowMs: input.nowMs,
        }),
      });
      page = patched.page;
    }
  } else if (type === FORMAL_EXAM_CONSOLE_REALTIME.CANDIDATE_PROGRESS_CHANGED) {
    if (onPage) {
      const answeredCount = payloadNumber(payload, 'answeredCount');
      const totalQuestionCount = payloadNumber(payload, 'totalQuestionCount');
      const patch: Partial<CompetitionFormalCandidateExam> = {};
      if (answeredCount != null) patch.answeredCount = answeredCount;
      if (totalQuestionCount != null) patch.totalQuestionCount = totalQuestionCount;
      page = patchFormalExamConsoleCandidatePage(page, candidateExamId, patch).page;
    }
  } else if (type === FORMAL_EXAM_CONSOLE_REALTIME.CANDIDATE_PRESENCE_CHANGED) {
    if (onPage) {
      const current = page?.items.find((row) => String(row.id) === candidateExamId);
      const lastSeenAt = payloadString(payload, 'lastSeenAt') ?? current?.lastSeenAt ?? null;
      const accessSessionStatus = payloadString(payload, 'accessSessionStatus')
        ?? current?.accessSessionStatus
        ?? null;
      page = patchFormalExamConsoleCandidatePage(page, candidateExamId, {
        lastSeenAt,
        accessSessionStatus,
        connectionState: resolveFormalExamConnectionState({
          candidateStatus: current?.status,
          accessSessionStatus,
          lastSeenAt,
          nowMs: input.nowMs,
        }),
      }).page;
    }
  } else if (type === FORMAL_EXAM_CONSOLE_REALTIME.ACCESS_SESSION_KICKED) {
    if (onPage) {
      const current = page?.items.find((row) => String(row.id) === candidateExamId);
      page = patchFormalExamConsoleCandidatePage(page, candidateExamId, {
        accessSessionStatus: 'kicked',
        connectionState: resolveFormalExamConnectionState({
          candidateStatus: current?.status,
          accessSessionStatus: 'kicked',
          lastSeenAt: current?.lastSeenAt,
          nowMs: input.nowMs,
        }),
      }).page;
    }
  } else if (type === FORMAL_EXAM_CONSOLE_REALTIME.SESSION_STATUS_CHANGED) {
    const status = payloadString(payload, 'status');
    if (session && status) {
      session = { ...session, status };
    }
  }

  return {
    lastSeq: seqState.nextSeq,
    duplicate: false,
    gap: seqState.gap,
    invalidate,
    session,
    page,
  };
}

export function startFormalExamConsoleRealtime(options: {
  examSessionId: string;
  isActive: () => boolean;
  getPageCandidateIds: () => string[];
  onConnected?: () => void;
  onDisconnected?: () => void;
  onSeqGap?: () => void;
  onEnvelope?: (envelope: FormalExamRealtimeEnvelope) => void;
}): FormalExamConsoleRealtimeHandle {
  if (!shouldOpenFormalExamConsoleRealtimeSocket()) {
    return {
      stop() {},
      reconnect() {},
      subscribePage() {},
    };
  }

  let stopped = false;
  let socket: WebSocket | null = null;
  let heartbeatTimer: number | undefined;
  let reconnectTimer: number | undefined;
  let attempt = 0;
  let lastSeq: number | null = null;
  const seen = new Set<string>();
  let heartbeatSeconds = 20;
  let connecting = false;
  let pageCandidateIds = options.getPageCandidateIds();

  const clearTimers = () => {
    if (heartbeatTimer) window.clearInterval(heartbeatTimer);
    if (reconnectTimer) window.clearTimeout(reconnectTimer);
    heartbeatTimer = undefined;
    reconnectTimer = undefined;
  };

  const detachSocket = (notify: boolean) => {
    clearTimers();
    if (socket) {
      socket.onopen = null;
      socket.onclose = null;
      socket.onerror = null;
      socket.onmessage = null;
      try {
        socket.close();
      } catch {
        /* ignore */
      }
    }
    socket = null;
    connecting = false;
    if (notify) options.onDisconnected?.();
  };

  const sendSubscribePage = () => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify({
      type: FORMAL_EXAM_CONSOLE_REALTIME.CONSOLE_SUBSCRIBE_PAGE,
      candidateExamIds: pageCandidateIds,
    }));
  };

  const scheduleReconnect = () => {
    if (stopped || !options.isActive() || reconnectTimer) return;
    const delay = formalExamRealtimeBackoffMs(attempt);
    attempt += 1;
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = undefined;
      void connect();
    }, delay);
  };

  const handleEnvelope = (envelope: FormalExamRealtimeEnvelope) => {
    if (rememberFormalExamEventId(seen, envelope.eventId)) return;
    const seqState = detectFormalExamSeqGap(lastSeq, envelope.seq);
    lastSeq = seqState.nextSeq;
    if (seqState.gap) options.onSeqGap?.();
    options.onEnvelope?.(envelope);
  };

  const connect = async () => {
    if (stopped || connecting || !options.isActive()) return;
    connecting = true;
    detachSocket(false);
    try {
      const ticket = await createFormalExamConsoleRealtimeTicket(options.examSessionId);
      if (stopped || !options.isActive()) {
        connecting = false;
        return;
      }
      heartbeatSeconds = Math.max(5, ticket.heartbeatSeconds || 20);
      const ws = new WebSocket(buildFormalExamWsUrl(ticket.wsPath, ticket.ticket));
      socket = ws;
      ws.onopen = () => {
        connecting = false;
        attempt = 0;
        pageCandidateIds = options.getPageCandidateIds();
        sendSubscribePage();
        options.onConnected?.();
        heartbeatTimer = window.setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({
              type: FORMAL_EXAM_CONSOLE_REALTIME.APPLICATION_HEARTBEAT,
            }));
          }
        }, heartbeatSeconds * 1000);
      };
      ws.onmessage = (event) => {
        if (typeof event.data !== 'string') return;
        try {
          handleEnvelope(JSON.parse(event.data) as FormalExamRealtimeEnvelope);
        } catch {
          /* ignore malformed frames */
        }
      };
      ws.onclose = () => {
        connecting = false;
        socket = null;
        clearTimers();
        options.onDisconnected?.();
        if (!stopped && options.isActive()) scheduleReconnect();
      };
      ws.onerror = () => {
        try {
          ws.close();
        } catch {
          /* ignore */
        }
      };
    } catch {
      connecting = false;
      options.onDisconnected?.();
      scheduleReconnect();
    }
  };

  void connect();
  return {
    stop() {
      stopped = true;
      detachSocket(true);
    },
    reconnect() {
      if (stopped || !options.isActive()) return;
      attempt = 0;
      void connect();
    },
    subscribePage(candidateExamIds: string[]) {
      pageCandidateIds = candidateExamIds.map(String);
      sendSubscribePage();
    },
  };
}

function payloadString(payload: Record<string, unknown> | null | undefined, key: string) {
  const value = payload?.[key];
  return typeof value === 'string' && value ? value : undefined;
}

function payloadNumber(payload: Record<string, unknown> | null | undefined, key: string) {
  const value = payload?.[key];
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

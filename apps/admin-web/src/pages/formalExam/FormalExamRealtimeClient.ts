import {
  FormalExamApiError,
  createFormalExamRealtimeTicket,
} from '../../api/formalExamPlayer';

export const FORMAL_EXAM_REALTIME = {
  SESSION_STATUS_CHANGED: 'session.status.changed',
  CANDIDATE_STATUS_CHANGED: 'candidate.status.changed',
  CANDIDATE_PROGRESS_CHANGED: 'candidate.progress.changed',
  ACCESS_SESSION_KICKED: 'access-session.kicked',
  FORCE_SUBMIT_COMPLETED: 'candidate.force-submit.completed',
  SNAPSHOT_RELOAD_REQUIRED: 'snapshot.reload.required',
  COMMAND_ACK: 'command.ack',
  APPLICATION_HEARTBEAT: 'application.heartbeat',
} as const;

export type FormalExamRealtimeEnvelope = {
  eventId?: string;
  commandId?: string | null;
  type?: string;
  aggregateId?: string;
  examSessionId?: string;
  candidateExamId?: string | null;
  accessSessionId?: string | null;
  generation?: number | null;
  seq?: number | null;
  occurredAt?: string;
  expiresAt?: string;
  target?: string;
  payload?: Record<string, unknown> | null;
};

export type FormalExamRealtimeHandle = {
  stop: () => void;
  reconnect: () => void;
};

function isTestRuntime() {
  try {
    return import.meta.env?.MODE === 'test';
  } catch {
    return false;
  }
}

export function shouldOpenFormalExamRealtimeSocket() {
  return !isTestRuntime() && typeof WebSocket !== 'undefined';
}

/** Full jitter: delay = random(0, min(cap, base * 2^attempt)). */
export function formalExamRealtimeBackoffMs(attempt: number, random: () => number = Math.random) {
  const base = 1000;
  const cap = 30000;
  const exp = Math.min(cap, base * 2 ** Math.max(0, attempt));
  return Math.floor(Math.max(0, random()) * exp);
}

export function rememberFormalExamEventId(seen: Set<string>, eventId?: string | null, limit = 512) {
  if (!eventId) return false;
  if (seen.has(eventId)) return true;
  seen.add(eventId);
  if (seen.size > limit) {
    const oldest = seen.values().next().value;
    if (oldest !== undefined) seen.delete(oldest);
  }
  return false;
}

export function detectFormalExamSeqGap(previousSeq: number | null, seq?: number | null) {
  if (seq == null || !Number.isFinite(seq)) {
    return { nextSeq: previousSeq, gap: false };
  }
  if (previousSeq == null) {
    return { nextSeq: seq, gap: false };
  }
  if (seq <= previousSeq) {
    return { nextSeq: previousSeq, gap: false };
  }
  return { nextSeq: seq, gap: seq > previousSeq + 1 };
}

export function buildFormalExamWsUrl(wsPath: string, ticket: string, location = window.location) {
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const path = wsPath.startsWith('/') ? wsPath : `/${wsPath}`;
  return `${proto}//${location.host}${path}?ticket=${encodeURIComponent(ticket)}`;
}

export function payloadStatus(payload?: Record<string, unknown> | null) {
  const value = payload?.status;
  return typeof value === 'string' ? value : undefined;
}

export function sameFormalExamId(
  left?: string | number | null,
  right?: string | number | null,
) {
  if (left == null || right == null || left === '' || right === '') return false;
  return String(left) === String(right);
}

export function formalExamPlayerEnvelopeRequiresReload(type?: string | null) {
  return type === FORMAL_EXAM_REALTIME.SESSION_STATUS_CHANGED
    || type === FORMAL_EXAM_REALTIME.SNAPSHOT_RELOAD_REQUIRED
    || type === FORMAL_EXAM_REALTIME.CANDIDATE_STATUS_CHANGED
    || type === FORMAL_EXAM_REALTIME.FORCE_SUBMIT_COMPLETED;
}

function isUnauthorized(error: unknown) {
  return error instanceof FormalExamApiError
    && (error.status === 401 || error.code === '401');
}

export function startFormalExamRealtime(options: {
  isActive: () => boolean;
  getGeneration: () => number | undefined;
  onConnected?: () => void;
  onDisconnected?: () => void;
  onKick?: () => void;
  onSnapshotReload?: () => void;
  onSeqGap?: () => void;
  onEnvelope?: (envelope: FormalExamRealtimeEnvelope) => void;
}): FormalExamRealtimeHandle {
  if (!shouldOpenFormalExamRealtimeSocket()) {
    return { stop() {}, reconnect() {} };
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

  const scheduleReconnect = () => {
    if (stopped || !options.isActive() || reconnectTimer) return;
    const delay = formalExamRealtimeBackoffMs(attempt);
    attempt += 1;
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = undefined;
      void connect();
    }, delay);
  };

  const ack = (envelope: FormalExamRealtimeEnvelope) => {
    if (!envelope.commandId || !socket || socket.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify({
      type: FORMAL_EXAM_REALTIME.COMMAND_ACK,
      commandId: envelope.commandId,
      generation: options.getGeneration(),
    }));
  };

  const handleEnvelope = (envelope: FormalExamRealtimeEnvelope) => {
    if (rememberFormalExamEventId(seen, envelope.eventId)) return;
    const seqState = detectFormalExamSeqGap(lastSeq, envelope.seq);
    lastSeq = seqState.nextSeq;
    if (seqState.gap) options.onSeqGap?.();
    ack(envelope);
    if (envelope.type === FORMAL_EXAM_REALTIME.ACCESS_SESSION_KICKED) {
      stopped = true;
      detachSocket(false);
      options.onKick?.();
      return;
    }
    if (formalExamPlayerEnvelopeRequiresReload(envelope.type)) {
      options.onSnapshotReload?.();
    }
    options.onEnvelope?.(envelope);
  };

  const connect = async () => {
    if (stopped || connecting || !options.isActive()) return;
    connecting = true;
    detachSocket(false);
    try {
      const ticket = await createFormalExamRealtimeTicket();
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
        options.onConnected?.();
        heartbeatTimer = window.setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({
              type: FORMAL_EXAM_REALTIME.APPLICATION_HEARTBEAT,
              generation: options.getGeneration(),
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
        if (!stopped && options.isActive()) {
          options.onSnapshotReload?.();
          scheduleReconnect();
        }
      };
      ws.onerror = () => {
        try {
          ws.close();
        } catch {
          /* ignore */
        }
      };
    } catch (error) {
      connecting = false;
      options.onDisconnected?.();
      if (isUnauthorized(error)) {
        stopped = true;
        options.onKick?.();
        return;
      }
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
  };
}

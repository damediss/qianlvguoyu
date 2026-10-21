import { FormalExamApiError, createFormalExamRealtimeTicket, } from '../../api/formalExamPlayer';
export const FORMAL_EXAM_REALTIME = {
    SESSION_STATUS_CHANGED: 'session.status.changed',
    CANDIDATE_STATUS_CHANGED: 'candidate.status.changed',
    CANDIDATE_PROGRESS_CHANGED: 'candidate.progress.changed',
    ACCESS_SESSION_KICKED: 'access-session.kicked',
    FORCE_SUBMIT_COMPLETED: 'candidate.force-submit.completed',
    SNAPSHOT_RELOAD_REQUIRED: 'snapshot.reload.required',
    COMMAND_ACK: 'command.ack',
    APPLICATION_HEARTBEAT: 'application.heartbeat',
};
function isTestRuntime() {
    try {
        return import.meta.env?.MODE === 'test';
    }
    catch {
        return false;
    }
}
export function shouldOpenFormalExamRealtimeSocket() {
    return !isTestRuntime() && typeof WebSocket !== 'undefined';
}
/** Full jitter: delay = random(0, min(cap, base * 2^attempt)). */
export function formalExamRealtimeBackoffMs(attempt, random = Math.random) {
    const base = 1000;
    const cap = 30000;
    const exp = Math.min(cap, base * 2 ** Math.max(0, attempt));
    return Math.floor(Math.max(0, random()) * exp);
}
export function rememberFormalExamEventId(seen, eventId, limit = 512) {
    if (!eventId)
        return false;
    if (seen.has(eventId))
        return true;
    seen.add(eventId);
    if (seen.size > limit) {
        const oldest = seen.values().next().value;
        if (oldest !== undefined)
            seen.delete(oldest);
    }
    return false;
}
export function parseFormalExamSeq(seq) {
    if (seq == null || seq === '')
        return null;
    const value = typeof seq === 'number' ? seq : Number(seq);
    if (!Number.isFinite(value) || !Number.isInteger(value) || value < 1)
        return null;
    if (value > Number.MAX_SAFE_INTEGER)
        return null;
    return value;
}
export function classifyFormalExamRealtimeEvent(input) {
    if (rememberFormalExamEventId(input.seen, input.eventId)) {
        return { action: 'duplicate', lastSeq: input.lastSeq };
    }
    const seq = parseFormalExamSeq(input.seq);
    if (seq == null) {
        return { action: 'skip-seq', lastSeq: input.lastSeq };
    }
    if (input.lastSeq == null || seq > input.lastSeq) {
        return {
            action: 'accept',
            lastSeq: seq,
            jump: input.lastSeq != null && seq > input.lastSeq + 1,
        };
    }
    if (seq === input.lastSeq) {
        return { action: 'collision', lastSeq: input.lastSeq };
    }
    return { action: 'out-of-order', lastSeq: input.lastSeq };
}
export function mergeFormalExamRealtimeEvents(catchUp, buffered) {
    return [...catchUp, ...buffered].sort((left, right) => {
        const leftSeq = parseFormalExamSeq(left.seq);
        const rightSeq = parseFormalExamSeq(right.seq);
        if (leftSeq == null && rightSeq == null) {
            return String(left.eventId || '').localeCompare(String(right.eventId || ''));
        }
        if (leftSeq == null)
            return 1;
        if (rightSeq == null)
            return -1;
        if (leftSeq !== rightSeq)
            return leftSeq - rightSeq;
        return String(left.eventId || '').localeCompare(String(right.eventId || ''));
    });
}
export function buildFormalExamWsUrl(wsPath, ticket, location = window.location) {
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const path = wsPath.startsWith('/') ? wsPath : `/${wsPath}`;
    return `${proto}//${location.host}${path}?ticket=${encodeURIComponent(ticket)}`;
}
export function payloadStatus(payload) {
    const value = payload?.status;
    return typeof value === 'string' ? value : undefined;
}
export function sameFormalExamId(left, right) {
    if (left == null || right == null || left === '' || right === '')
        return false;
    return String(left) === String(right);
}
export function formalExamPlayerEnvelopeRequiresReload(type) {
    return type === FORMAL_EXAM_REALTIME.SESSION_STATUS_CHANGED
        || type === FORMAL_EXAM_REALTIME.SNAPSHOT_RELOAD_REQUIRED
        || type === FORMAL_EXAM_REALTIME.CANDIDATE_STATUS_CHANGED
        || type === FORMAL_EXAM_REALTIME.FORCE_SUBMIT_COMPLETED;
}
function isUnauthorized(error) {
    return error instanceof FormalExamApiError
        && (error.status === 401 || error.code === '401');
}
export function startFormalExamRealtime(options) {
    if (!shouldOpenFormalExamRealtimeSocket()) {
        return { stop() { }, reconnect() { } };
    }
    let stopped = false;
    let socket = null;
    let heartbeatTimer;
    let reconnectTimer;
    let attempt = 0;
    let lastSeq = readStoredSeq(options.cursorKey);
    const seen = new Set();
    let heartbeatSeconds = 20;
    let connecting = false;
    let catchingUp = false;
    let buffer = [];
    const persistSeq = (seq) => {
        lastSeq = seq;
        writeStoredSeq(options.cursorKey, seq);
    };
    const clearTimers = () => {
        if (heartbeatTimer)
            window.clearInterval(heartbeatTimer);
        if (reconnectTimer)
            window.clearTimeout(reconnectTimer);
        heartbeatTimer = undefined;
        reconnectTimer = undefined;
    };
    const detachSocket = (notify) => {
        clearTimers();
        if (socket) {
            socket.onopen = null;
            socket.onclose = null;
            socket.onerror = null;
            socket.onmessage = null;
            try {
                socket.close();
            }
            catch {
                /* ignore */
            }
        }
        socket = null;
        connecting = false;
        catchingUp = false;
        if (notify)
            options.onDisconnected?.();
    };
    const scheduleReconnect = () => {
        if (stopped || !options.isActive() || reconnectTimer)
            return;
        const delay = formalExamRealtimeBackoffMs(attempt);
        attempt += 1;
        reconnectTimer = window.setTimeout(() => {
            reconnectTimer = undefined;
            void connect();
        }, delay);
    };
    const ack = (envelope) => {
        if (!envelope.commandId || !socket || socket.readyState !== WebSocket.OPEN)
            return;
        socket.send(JSON.stringify({
            type: FORMAL_EXAM_REALTIME.COMMAND_ACK,
            commandId: envelope.commandId,
            generation: options.getGeneration(),
        }));
    };
    const applyLive = (envelope) => {
        const verdict = classifyFormalExamRealtimeEvent({
            seen,
            lastSeq,
            eventId: envelope.eventId,
            seq: envelope.seq,
        });
        if (verdict.action === 'duplicate')
            return;
        if (verdict.action === 'collision') {
            options.onSequenceCollision?.();
            options.onSnapshotReload?.();
            return;
        }
        if (verdict.action === 'out-of-order') {
            options.onOutOfOrder?.();
            options.onSnapshotReload?.();
            return;
        }
        if (verdict.action === 'accept') {
            persistSeq(verdict.lastSeq);
            if (verdict.jump)
                options.onSeqJump?.();
        }
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
    const runCatchUp = async () => {
        if (!options.fetchCatchUp) {
            options.onSnapshotReload?.();
            return;
        }
        try {
            const collected = [];
            let after = lastSeq == null ? '0' : String(lastSeq);
            let snapshotRequired = false;
            for (let page = 0; page < 32; page += 1) {
                const result = await options.fetchCatchUp(after);
                if (result.snapshotRequired)
                    snapshotRequired = true;
                collected.push(...(result.events || []));
                after = result.nextSeq == null ? after : String(result.nextSeq);
                if (!result.hasMore)
                    break;
            }
            if (snapshotRequired)
                options.onSnapshotReload?.();
            const queued = buffer.splice(0);
            const merged = mergeFormalExamRealtimeEvents(collected, queued);
            for (const envelope of merged) {
                applyLive(envelope);
            }
            const watermark = parseFormalExamSeq(after);
            if (watermark != null && (lastSeq == null || watermark > lastSeq)) {
                persistSeq(watermark);
            }
        }
        catch {
            options.onSnapshotReload?.();
        }
    };
    const connect = async () => {
        if (stopped || connecting || !options.isActive())
            return;
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
            catchingUp = true;
            buffer = [];
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
                void runCatchUp().finally(() => {
                    catchingUp = false;
                    const rest = buffer.splice(0);
                    for (const envelope of rest)
                        applyLive(envelope);
                });
            };
            ws.onmessage = (event) => {
                if (typeof event.data !== 'string')
                    return;
                try {
                    const envelope = JSON.parse(event.data);
                    if (catchingUp) {
                        buffer.push(envelope);
                        return;
                    }
                    applyLive(envelope);
                }
                catch {
                    /* ignore malformed frames */
                }
            };
            ws.onclose = () => {
                connecting = false;
                socket = null;
                catchingUp = false;
                clearTimers();
                options.onDisconnected?.();
                if (!stopped && options.isActive()) {
                    scheduleReconnect();
                }
            };
            ws.onerror = () => {
                try {
                    ws.close();
                }
                catch {
                    /* ignore */
                }
            };
        }
        catch (error) {
            connecting = false;
            catchingUp = false;
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
            if (stopped || !options.isActive())
                return;
            attempt = 0;
            void connect();
        },
    };
}
function readStoredSeq(key) {
    if (!key)
        return null;
    try {
        return parseFormalExamSeq(sessionStorage.getItem(key));
    }
    catch {
        return null;
    }
}
function writeStoredSeq(key, seq) {
    if (!key || seq == null)
        return;
    try {
        sessionStorage.setItem(key, String(seq));
    }
    catch {
        /* ignore quota / private mode */
    }
}

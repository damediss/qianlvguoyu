import { createFormalExamConsoleRealtimeTicket } from '../../api/competition';
import { buildFormalExamWsUrl, classifyFormalExamRealtimeEvent, formalExamRealtimeBackoffMs, mergeFormalExamRealtimeEvents, parseFormalExamSeq, shouldOpenFormalExamRealtimeSocket, } from '../formalExam/FormalExamRealtimeClient';
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
};
export const FORMAL_EXAM_CONNECTION_LABELS = {
    online: { label: '在线', color: 'success' },
    briefly_offline: { label: '短暂离线', color: 'warning' },
    kicked: { label: '被清退', color: 'error' },
    token_expired: { label: 'token 过期', color: 'default' },
    submitted: { label: '已交卷', color: 'success' },
    absent: { label: '缺考', color: 'default' },
};
const COUNT_KEYS = {
    waiting_enter: 'waitingEnterCount',
    ready: 'readyCount',
    running: 'runningCount',
    submitted: 'submittedCount',
    auto_submitted: 'autoSubmittedCount',
    forced_submitted: 'forcedSubmittedCount',
    absent: 'absentCount',
};
export function shouldOpenFormalExamConsoleRealtimeSocket() {
    return shouldOpenFormalExamRealtimeSocket();
}
export function formalExamConsoleSessionQueryKey(sessionId) {
    return ['competition-formal-exam-session', sessionId];
}
export function formalExamConsoleCandidatesQueryKey(sessionId, keyword, paperSnapshotId, status, page, pageSize) {
    return [
        'competition-formal-exam-session-candidates',
        sessionId,
        keyword,
        paperSnapshotId,
        status,
        page,
        pageSize,
    ];
}
export function formalExamConsolePollIntervalMs(_realtimeConnected, options) {
    // Console is a single operator page. Do not inherit the 60–120s player backoff:
    // a connected-but-silent WebSocket would freeze answeredCount / 交卷 for minutes.
    void _realtimeConnected;
    void options?.random;
    return 5000;
}
export function resolveFormalExamConnectionState(input) {
    if (input.candidateStatus === 'absent')
        return 'absent';
    if (input.candidateStatus === 'submitted'
        || input.candidateStatus === 'auto_submitted'
        || input.candidateStatus === 'forced_submitted') {
        return 'submitted';
    }
    if (input.accessSessionStatus === 'kicked')
        return 'kicked';
    if (input.accessSessionStatus === 'expired')
        return 'token_expired';
    const windowSeconds = Math.max(20, input.onlineWithinSeconds ?? 60);
    const lastSeen = input.lastSeenAt ? Date.parse(input.lastSeenAt) : Number.NaN;
    const now = input.nowMs ?? Date.now();
    if (Number.isFinite(lastSeen) && lastSeen >= now - windowSeconds * 1000) {
        return 'online';
    }
    return 'briefly_offline';
}
export function applyFormalExamSessionCountDelta(session, previousStatus, nextStatus) {
    if (!session || !previousStatus || !nextStatus || previousStatus === nextStatus) {
        return session;
    }
    const next = { ...session };
    const previousKey = COUNT_KEYS[previousStatus];
    const nextKey = COUNT_KEYS[nextStatus];
    if (previousKey) {
        const current = Number(next[previousKey] ?? 0);
        next[previousKey] = Math.max(0, current - 1);
    }
    if (nextKey) {
        const current = Number(next[nextKey] ?? 0);
        next[nextKey] = current + 1;
    }
    next.submittedTotal = (next.submittedCount ?? 0)
        + (next.autoSubmittedCount ?? 0)
        + (next.forcedSubmittedCount ?? 0);
    return next;
}
export function patchFormalExamConsoleCandidatePage(page, candidateExamId, patch) {
    if (!page || !candidateExamId) {
        return { page, patched: false };
    }
    const target = String(candidateExamId);
    let patched = false;
    const items = (page.items || []).map((row) => {
        if (String(row.id) !== target)
            return row;
        patched = true;
        return { ...row, ...patch };
    });
    return { page: patched ? { ...page, items } : page, patched };
}
export function isFormalExamConsoleCurrentPageCandidate(pageCandidateIds, candidateExamId) {
    if (!candidateExamId)
        return false;
    const target = String(candidateExamId);
    return pageCandidateIds.some((id) => String(id) === target);
}
export function applyFormalExamConsoleEnvelope(input) {
    const verdict = classifyFormalExamRealtimeEvent({
        seen: input.seen,
        lastSeq: input.lastSeq,
        eventId: input.envelope.eventId,
        seq: input.envelope.seq,
    });
    if (verdict.action === 'duplicate') {
        return {
            lastSeq: input.lastSeq,
            duplicate: true,
            jump: false,
            outOfOrder: false,
            collision: false,
            invalidate: false,
            session: input.session,
            page: input.page,
        };
    }
    if (verdict.action === 'collision' || verdict.action === 'out-of-order') {
        return {
            lastSeq: input.lastSeq,
            duplicate: false,
            jump: false,
            outOfOrder: verdict.action === 'out-of-order',
            collision: verdict.action === 'collision',
            invalidate: true,
            session: input.session,
            page: input.page,
        };
    }
    let lastSeq = verdict.action === 'accept' ? verdict.lastSeq : input.lastSeq;
    const jump = verdict.action === 'accept' && verdict.jump;
    const outOfOrder = false;
    const collision = false;
    let session = input.session;
    let page = input.page;
    let invalidate = outOfOrder
        || collision
        || input.envelope.type === FORMAL_EXAM_CONSOLE_REALTIME.SNAPSHOT_RELOAD_REQUIRED;
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
        }
        else {
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
    }
    else if (type === FORMAL_EXAM_CONSOLE_REALTIME.CANDIDATE_PROGRESS_CHANGED) {
        if (onPage) {
            const answeredCount = payloadNumber(payload, 'answeredCount');
            const totalQuestionCount = payloadNumber(payload, 'totalQuestionCount');
            const patch = {};
            if (answeredCount != null)
                patch.answeredCount = answeredCount;
            if (totalQuestionCount != null)
                patch.totalQuestionCount = totalQuestionCount;
            page = patchFormalExamConsoleCandidatePage(page, candidateExamId, patch).page;
        }
    }
    else if (type === FORMAL_EXAM_CONSOLE_REALTIME.CANDIDATE_PRESENCE_CHANGED) {
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
    }
    else if (type === FORMAL_EXAM_CONSOLE_REALTIME.ACCESS_SESSION_KICKED) {
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
    }
    else if (type === FORMAL_EXAM_CONSOLE_REALTIME.SESSION_STATUS_CHANGED) {
        const status = payloadString(payload, 'status');
        if (session && status) {
            session = { ...session, status };
        }
    }
    return {
        lastSeq,
        duplicate: false,
        jump,
        outOfOrder,
        collision,
        invalidate,
        session,
        page,
    };
}
export function startFormalExamConsoleRealtime(options) {
    if (!shouldOpenFormalExamConsoleRealtimeSocket()) {
        return {
            stop() { },
            reconnect() { },
            subscribePage() { },
        };
    }
    let stopped = false;
    let socket = null;
    let heartbeatTimer;
    let reconnectTimer;
    let attempt = 0;
    let lastSeq = null;
    const seen = new Set();
    let heartbeatSeconds = 20;
    let connecting = false;
    let catchingUp = false;
    let buffer = [];
    let pageCandidateIds = options.getPageCandidateIds();
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
    const sendSubscribePage = () => {
        if (!socket || socket.readyState !== WebSocket.OPEN)
            return;
        socket.send(JSON.stringify({
            type: FORMAL_EXAM_CONSOLE_REALTIME.CONSOLE_SUBSCRIBE_PAGE,
            candidateExamIds: pageCandidateIds,
        }));
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
            options.onPageRefresh?.();
            return;
        }
        if (verdict.action === 'out-of-order') {
            options.onOutOfOrder?.();
            options.onPageRefresh?.();
            return;
        }
        if (verdict.action === 'accept') {
            lastSeq = verdict.lastSeq;
        }
        options.onEnvelope?.(envelope);
    };
    const runCatchUp = async () => {
        if (!options.fetchCatchUp) {
            options.onPageRefresh?.();
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
                options.onPageRefresh?.();
            const queued = buffer.splice(0);
            for (const envelope of mergeFormalExamRealtimeEvents(collected, queued)) {
                applyLive(envelope);
            }
            const watermark = parseFormalExamSeq(after);
            if (watermark != null && (lastSeq == null || watermark > lastSeq)) {
                lastSeq = watermark;
            }
        }
        catch {
            options.onPageRefresh?.();
        }
    };
    const connect = async () => {
        if (stopped || connecting || !options.isActive())
            return;
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
            catchingUp = true;
            buffer = [];
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
                if (!stopped && options.isActive())
                    scheduleReconnect();
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
        catch {
            connecting = false;
            catchingUp = false;
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
            if (stopped || !options.isActive())
                return;
            attempt = 0;
            void connect();
        },
        subscribePage(candidateExamIds) {
            const next = candidateExamIds.map(String);
            const changed = next.join(',') !== pageCandidateIds.map(String).join(',');
            pageCandidateIds = next;
            sendSubscribePage();
            if (changed) {
                lastSeq = null;
                options.onPageRefresh?.();
            }
        },
    };
}
function payloadString(payload, key) {
    const value = payload?.[key];
    return typeof value === 'string' && value ? value : undefined;
}
function payloadNumber(payload, key) {
    const value = payload?.[key];
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

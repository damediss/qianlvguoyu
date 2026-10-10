import { apiJson } from './http.mjs';
import { nextAnswerVersion } from './ops.mjs';
import { fullJitter } from './crypto.mjs';
import { openTicketSocket, parseEnvelope } from './ws.mjs';
import { postAnswerSyncWithRetry } from './answer-sync-retry.mjs';
import {
  classifyFormalExamRealtimeEvent,
  mergeFormalExamRealtimeEvents,
} from './seq-semantics.mjs';

const ANSWER_SEMANTIC_RETRY_CODES = new Set([
  'STALE_VERSION',
  'VERSION_PAYLOAD_CONFLICT',
]);
const ANSWER_SEMANTIC_RETRY_LIMIT = 4;

export function createPadAgent(options) {
  const {
    baseUrl,
    pad,
    sessionId,
    password,
    ops,
    wsStats,
    scenario = 'normal',
    answerEveryMs = 10_000,
    writeAnswers = false,
  } = options;
  const state = {
    token: null,
    tenantId: null,
    generation: null,
    accessSessionId: null,
    candidateExamId: pad.candidateExamId,
    questions: [],
    versions: {},
    lastAcked: {},
    lastConfirmed: {},
    socket: null,
    seqSeen: new Set(),
    lastSeq: null,
    seqJumps: 0,
    sequenceCollisions: 0,
    outOfOrder: 0,
    missedRelevantEvents: 0,
    duplicates: 0,
    commandsAcked: 0,
    heartbeats: 0,
    stopped: false,
    offlineBuffer: [],
    pendingSync: [],
    syncPaused: false,
    syncItemResults: {
      total: 0,
      accepted: 0,
      rejected: 0,
      byConflictCode: {},
      byOutcome: {},
    },
    semanticRetryExhausted: 0,
    lastSyncFailure: null,
    lastBarrier: null,
  };

  async function login() {
    const result = await apiJson(baseUrl, '/competition/formal-exam/player/auth/login', {
      method: 'POST',
      body: {
        account: pad.account,
        password,
        sessionId: String(sessionId),
        deviceId: pad.deviceId,
      },
    });
    ops.record('login', result);
    if (!result.ok || !result.data?.accessToken) {
      return false;
    }
    state.token = result.data.accessToken;
    state.tenantId = result.data.tenantId;
    state.generation = result.data.generation;
    state.accessSessionId = result.data.accessSessionId;
    state.candidateExamId = result.data.candidateExamId || pad.candidateExamId;
    return true;
  }

  async function authed(path, init = {}) {
    return apiJson(baseUrl, path, {
      ...init,
      token: state.token,
      tenantId: state.tenantId,
    });
  }

  async function contextAndEnter() {
    const context = await authed(`/competition/formal-exam/player/sessions/${sessionId}/context`);
    ops.record('context', context);
    const enter = await authed(`/competition/formal-exam/player/sessions/${sessionId}/enter`, { method: 'POST' });
    ops.record('enter', enter);
    return enter.ok;
  }

  async function fetchPaper() {
    const paper = await authed(`/competition/formal-exam/player/sessions/${sessionId}/paper`);
    ops.record('paper', paper);
    state.questions = paper.data?.questions || [];
    return paper.ok;
  }

  async function connectWs() {
    const ticket = await authed('/competition/formal-exam/player/realtime/ticket', { method: 'POST' });
    ops.record('ticket', ticket);
    wsStats.attempted += 1;
    if (!ticket.ok || !ticket.data?.ticket) {
      if (ticket.status === 503) wsStats.redisUnavailable += 1;
      return false;
    }
    const opened = await openTicketSocket({
      baseUrl,
      wsPath: ticket.data.wsPath || '/api/ws/competition/formal-exam/player',
      ticket: ticket.data.ticket,
    });
    ops.record('ws-connect', { ok: opened.opened, status: opened.opened ? 101 : 0, ms: opened.ms });
    if (!opened.opened) {
      wsStats.connectFailed += 1;
      return false;
    }
    wsStats.opened += 1;
    wsStats.active += 1;
    wsStats.connectSamples.push(opened.ms);
    state.socket = opened.socket;
    const pending = [];
    let live = false;
    state.socket.on('message', (raw) => {
      if (!live) {
        pending.push(raw);
        return;
      }
      onMessage(raw);
    });
    state.socket.on('close', () => {
      if (wsStats.active > 0) wsStats.active -= 1;
      wsStats.closed += 1;
      state.socket = null;
    });
    const catchUp = await catchUpFromHttp();
    const buffered = pending.splice(0).map((raw) => parseEnvelope(raw)).filter(Boolean);
    live = true;
    for (const envelope of mergeFormalExamRealtimeEvents(catchUp.events, buffered)) {
      applyEnvelope(envelope, { fromCatchUp: true });
    }
    if (catchUp.watermark != null && (state.lastSeq == null || catchUp.watermark > state.lastSeq)) {
      state.lastSeq = catchUp.watermark;
    }
    return true;
  }

  async function catchUpFromHttp() {
    const events = [];
    let afterSeq = state.lastSeq == null ? '0' : String(state.lastSeq);
    let snapshotRequired = false;
    for (let pageNo = 0; pageNo < 32; pageNo += 1) {
      const result = await authed(
        `/competition/formal-exam/player/realtime/events?afterSeq=${encodeURIComponent(afterSeq)}&limit=200`,
      );
      ops.record('realtime-catch-up', result);
      if (!result.ok) {
        state.missedRelevantEvents += 1;
        wsStats.missedRelevantEvents += 1;
        await pollStatus();
        return { events, watermark: null };
      }
      const page = result.data || {};
      if (page.snapshotRequired) snapshotRequired = true;
      events.push(...(page.events || []));
      afterSeq = page.nextSeq == null ? afterSeq : String(page.nextSeq);
      if (!page.hasMore) break;
    }
    if (snapshotRequired) {
      wsStats.snapshotRequired += 1;
      await pollStatus();
    }
    const watermark = Number(afterSeq);
    return {
      events,
      watermark: Number.isFinite(watermark) && watermark >= 1 ? watermark : null,
    };
  }

  function onMessage(raw) {
    const envelope = parseEnvelope(raw);
    applyEnvelope(envelope);
  }

  function applyEnvelope(envelope, options = {}) {
    if (!envelope || !envelope.type) return;
    if (!options.fromCatchUp) wsStats.messages += 1;
    const verdict = classifyFormalExamRealtimeEvent({
      seen: state.seqSeen,
      lastSeq: state.lastSeq,
      eventId: envelope.eventId,
      seq: envelope.seq,
    });
    if (verdict.action === 'duplicate') {
      state.duplicates += 1;
      wsStats.duplicates += 1;
      return;
    }
    if (verdict.action === 'collision') {
      state.sequenceCollisions += 1;
      wsStats.sequenceCollisions += 1;
      return;
    }
    if (verdict.action === 'out-of-order') {
      state.outOfOrder += 1;
      wsStats.outOfOrder += 1;
      return;
    }
    if (verdict.action === 'accept') {
      if (verdict.jump) {
        state.seqJumps += 1;
        wsStats.seqJumps += 1;
      }
      state.lastSeq = verdict.lastSeq;
    }
    if (envelope.type === 'command.ack' || envelope.commandId) {
      ack(envelope).catch(() => {});
    }
  }

  async function ack(envelope) {
    if (!envelope.commandId) return;
    const wsAck = state.socket && state.socket.readyState === 1;
    if (wsAck) {
      state.socket.send(JSON.stringify({
        type: 'command.ack',
        commandId: envelope.commandId,
        generation: state.generation,
      }));
      state.commandsAcked += 1;
      wsStats.acks += 1;
      return;
    }
    const result = await authed('/competition/formal-exam/player/realtime/ack', {
      method: 'POST',
      body: { commandId: envelope.commandId, generation: state.generation },
    });
    ops.record('ack', result);
    if (result.ok) {
      state.commandsAcked += 1;
      wsStats.acks += 1;
    }
  }

  function heartbeat() {
    if (!state.socket || state.socket.readyState !== 1) return;
    state.socket.send(JSON.stringify({ type: 'application.heartbeat', generation: state.generation }));
    state.heartbeats += 1;
    wsStats.heartbeats += 1;
  }

  function buildAnswerItem(question, version) {
    const optionId = question.options && question.options[0]
      ? question.options[0].optionId || question.options[0].id
      : `${question.questionSnapshotId}:A`;
    return {
      questionSnapshotId: String(question.questionSnapshotId),
      answer: { selectedOptionIds: [optionId] },
      marked: false,
      version,
    };
  }

  async function syncAnswers(items, options = {}) {
    if (!items.length) return true;
    const enqueueOnFailure = options.enqueueOnFailure !== false;
    let requestItems = items;
    for (let semanticAttempt = 0;
      semanticAttempt <= ANSWER_SEMANTIC_RETRY_LIMIT;
      semanticAttempt += 1) {
      state.lastSyncFailure = null;
      const outcome = await postAnswerSyncWithRetry({
        items: requestItems,
        send: (payload) => authed(
          `/competition/formal-exam/player/sessions/${sessionId}/answers/sync`,
          { method: 'POST', body: { answers: payload } },
        ),
        record: (name, result) => ops.record(name, result),
      });
      if (!outcome.ok) {
        state.lastSyncFailure = 'transport';
        if (enqueueOnFailure) state.pendingSync.push(...requestItems);
        return false;
      }

      const rawResults = outcome.result?.data?.results;
      const results = Array.isArray(rawResults) ? rawResults : [];
      const normalizedResults = requestItems.map((item, index) => results[index] || ({
        questionSnapshotId: item.questionSnapshotId,
        version: item.version,
        accepted: false,
        conflictCode: 'MISSING_RESULT',
      }));
      recordSyncItemResults(normalizedResults);

      const retryItems = [];
      let missingResult = !Array.isArray(rawResults);
      for (let index = 0; index < requestItems.length; index += 1) {
        const requested = requestItems[index];
        const result = normalizedResults[index];
        if (!results[index]) missingResult = true;
        const accepted = result?.accepted === true;
        const id = String(result?.questionSnapshotId ?? requested.questionSnapshotId);
        if (accepted) {
          const version = Number.isInteger(Number(result.version))
            ? Number(result.version)
            : Number(requested.version);
          state.lastAcked[id] = version;
          state.lastConfirmed[id] = version;
          state.versions[id] = Math.max(Number(state.versions[id]) || 0, version);
          continue;
        }

        const conflictCode = String(result?.conflictCode || '');
        const serverVersion = Number(result?.version);
        if (ANSWER_SEMANTIC_RETRY_CODES.has(conflictCode)
            && Number.isInteger(serverVersion) && serverVersion >= 1) {
          retryItems.push({ ...requested, version: serverVersion + 1 });
          state.versions[id] = Math.max(Number(state.versions[id]) || 0, serverVersion);
        }
      }

      if (!retryItems.length) {
        if (missingResult) {
          state.lastSyncFailure = 'semantic';
          if (enqueueOnFailure) state.pendingSync.push(...requestItems);
          return false;
        }
        // Non-retryable per-item outcomes (for example EXAM_TIME_EXPIRED) are
        // retained in the semantic counters but do not create a phantom
        // client version. The accepted rows above are the only rows eligible
        // for lastAcked/submit reconciliation.
        return true;
      }

      if (semanticAttempt >= ANSWER_SEMANTIC_RETRY_LIMIT) {
        state.semanticRetryExhausted += retryItems.length;
        state.lastSyncFailure = 'semantic';
        if (enqueueOnFailure) state.pendingSync.push(...retryItems);
        return false;
      }
      requestItems = retryItems;
      const backoff = 200 * (2 ** semanticAttempt) + fullJitter(201);
      await new Promise((resolve) => setTimeout(resolve, backoff));
    }
    return false;
  }

  function recordSyncItemResults(results) {
    if (ops.recordItemResults) ops.recordItemResults('answer-sync', results);
    for (const result of results) {
      const accepted = result?.accepted === true;
      const conflictCode = result?.conflictCode == null || result.conflictCode === ''
        ? 'NONE'
        : String(result.conflictCode);
      const outcome = `${accepted ? 'accepted' : 'rejected'}:${conflictCode}`;
      state.syncItemResults.total += 1;
      if (accepted) state.syncItemResults.accepted += 1;
      else state.syncItemResults.rejected += 1;
      state.syncItemResults.byConflictCode[conflictCode] =
        (state.syncItemResults.byConflictCode[conflictCode] || 0) + 1;
      state.syncItemResults.byOutcome[outcome] =
        (state.syncItemResults.byOutcome[outcome] || 0) + 1;
    }
  }

  async function drainPendingSync() {
    const leftover = [];
    while (state.pendingSync.length) {
      const batch = state.pendingSync.splice(0, 20);
      const ok = await syncAnswers(batch, { enqueueOnFailure: false });
      if (!ok) leftover.push(...batch);
    }
    state.pendingSync.push(...leftover);
    return leftover.length === 0;
  }

  async function changeAnswers(batchSize = 1) {
    if (!state.questions.length) return;
    if (state.pendingSync.length) {
      await drainPendingSync();
    }
    const count = Math.max(1, Math.min(3, batchSize));
    const items = [];
    for (let i = 0; i < count; i += 1) {
      const question = state.questions[(state.questions.length + i) % state.questions.length];
      const id = String(question.questionSnapshotId);
      const version = nextAnswerVersion(state.versions[id]);
      state.versions[id] = version;
      items.push(buildAnswerItem(question, version));
    }
    if (state.syncPaused) {
      state.offlineBuffer.push(...items);
      return;
    }
    await syncAnswers(items);
  }

  async function flushOfflineBurst() {
    const items = state.offlineBuffer.splice(0, 20);
    state.syncPaused = false;
    if (!items.length) return;
    await syncAnswers(items.slice(0, 20));
  }

  async function syncBarrier(options = {}) {
    const timeoutMs = Math.max(1_000, Number(options.timeoutMs) || 120_000);
    const deadline = Date.now() + timeoutMs;
    let rounds = 0;
    state.syncPaused = false;
    while ((state.pendingSync.length || state.offlineBuffer.length) && Date.now() < deadline) {
      rounds += 1;
      if (state.offlineBuffer.length) {
        const batch = state.offlineBuffer.splice(0, 20);
        await syncAnswers(batch);
      }
      if (state.pendingSync.length) await drainPendingSync();
      if (state.pendingSync.length || state.offlineBuffer.length) {
        const remaining = Math.max(1, deadline - Date.now());
        await new Promise((resolve) => setTimeout(resolve, Math.min(1000, remaining)));
      }
    }
    const result = {
      ok: state.pendingSync.length === 0 && state.offlineBuffer.length === 0,
      pendingSync: state.pendingSync.length,
      offlineBuffer: state.offlineBuffer.length,
      rounds,
      semanticRetryExhausted: state.semanticRetryExhausted,
    };
    state.lastBarrier = result;
    return result;
  }

  async function submit(confirmUnanswered = true, options = {}) {
    if (!options.skipBarrier) {
      const barrier = await syncBarrier(options);
      if (!barrier.ok) {
        const result = {
          ok: false,
          status: 409,
          ms: 0,
          body: { code: 'ANSWERS_NOT_SYNCED', message: 'answer sync barrier failed' },
          data: null,
        };
        ops.record('submit', result);
        return result;
      }
    }
    const answerVersions = state.questions.map((question) => {
      const id = String(question.questionSnapshotId);
      return { questionSnapshotId: id, version: state.lastAcked[id] || 0 };
    });
    const result = await authed(`/competition/formal-exam/player/sessions/${sessionId}/submit`, {
      method: 'POST',
      body: { confirmUnanswered, answerVersions },
    });
    ops.record('submit', result);
    return result;
  }

  async function pollStatus() {
    const result = await authed(`/competition/formal-exam/player/sessions/${sessionId}/status`);
    ops.record('status', result);
    return result;
  }

  async function loop(durationMs, scenarioName) {
    const deadline = Date.now() + durationMs;
    let nextAnswer = Date.now() + fullJitter(answerEveryMs);
    let nextHeartbeat = Date.now();
    let nextStatus = Date.now() + (state.socket ? 60_000 + fullJitter(60_000) : 1000);
    const offlineUntil = scenarioName === 'offline-burst' ? Date.now() + 300_000 : 0;
    if (scenarioName === 'offline-burst') state.syncPaused = true;
    let disconnectedForCatchUp = false;
    while (!state.stopped && Date.now() < deadline) {
      const now = Date.now();
      if (now >= nextHeartbeat) {
        heartbeat();
        nextHeartbeat = now + 20_000;
      }
      if (scenarioName === 'ws-gap-catchup' && !disconnectedForCatchUp && now + 35_000 < deadline) {
        disconnectedForCatchUp = true;
        if (state.socket) {
          try { state.socket.close(); } catch { /* ignore */ }
        }
        await new Promise((resolve) => setTimeout(resolve, 30_000));
        const reopened = await connectWs();
        if (reopened) wsStats.reconnects += 1;
        nextHeartbeat = Date.now() + 20_000;
      }
      if (scenarioName === 'offline-burst' && state.syncPaused && now >= offlineUntil) {
        await new Promise((resolve) => setTimeout(resolve, fullJitter(30_000)));
        await flushOfflineBurst();
      }
      if (writeAnswers && now >= nextAnswer) {
        await changeAnswers(1 + Math.floor(Math.random() * 3));
        nextAnswer = now + answerEveryMs;
      }
      if (now >= nextStatus) {
        await pollStatus();
        nextStatus = now + (state.socket ? 60_000 + fullJitter(60_000) : 4000 + fullJitter(2000));
      }
      await new Promise((resolve) => setTimeout(resolve, 200));
    }
  }

  function stop() {
    state.stopped = true;
    if (state.socket) {
      try { state.socket.close(); } catch { /* ignore */ }
    }
  }

  return {
    pad,
    state,
    login,
    contextAndEnter,
    fetchPaper,
    connectWs,
    loop,
    syncBarrier,
    submit,
    pollStatus,
    stop,
    snapshot() {
      return {
        padIndex: pad.index,
        candidateExamId: state.candidateExamId,
        lastAcked: { ...state.lastAcked },
        seqJumps: state.seqJumps,
        sequenceCollisions: state.sequenceCollisions,
        outOfOrder: state.outOfOrder,
        missedRelevantEvents: state.missedRelevantEvents,
        duplicates: state.duplicates,
        pendingSync: state.pendingSync.length,
        offlineBuffer: state.offlineBuffer.length,
        syncItemResults: { ...state.syncItemResults,
          byConflictCode: { ...state.syncItemResults.byConflictCode },
          byOutcome: { ...state.syncItemResults.byOutcome },
        },
        semanticRetryExhausted: state.semanticRetryExhausted,
        syncBarrier: state.lastBarrier,
        wsOpen: Boolean(state.socket && state.socket.readyState === 1),
      };
    },
  };
}

export function createWsStats() {
  return {
    attempted: 0,
    opened: 0,
    active: 0,
    closed: 0,
    connectFailed: 0,
    redisUnavailable: 0,
    reconnects: 0,
    messages: 0,
    heartbeats: 0,
    acks: 0,
    seqJumps: 0,
    sequenceCollisions: 0,
    outOfOrder: 0,
    missedRelevantEvents: 0,
    snapshotRequired: 0,
    duplicates: 0,
    connectSamples: [],
  };
}

#!/usr/bin/env node
/**
 * Formal-exam Phase 6 load planner / runner.
 *
 * Default is dry-run: prints the HTTP+WS mix for every required scenario.
 * Live modes never invent 3000-pad capacity. Shared/dev MySQL must not be
 * hit with large write traffic.
 *
 *   node scripts/formal-exam-load/run.mjs
 *   node scripts/formal-exam-load/run.mjs --scenario 3000 --consoles 10
 *   node scripts/formal-exam-load/run.mjs --live --mode health --pads 8 --duration-sec 10
 *   node scripts/formal-exam-load/run.mjs --live --mode metrics --base-url http://127.0.0.1:8080/api
 *   node scripts/formal-exam-load/run.mjs --live --mode exam --session-id <id> --account <player> --password <pwd> --pads 1
 *
 * Exam live is read-only by default (login/context/status/ticket/WS). Pass
 * --write-answers only against an isolated test session.
 */

const PAD_SCENARIOS = [100, 500, 1000, 3000];
const CONSOLE_SCENARIOS = [1, 5, 10];
const LIVE_PAD_CAP = 8;
const EXAM_PAD_CAP = 5;
const BLOCKED_LARGE_HOST_FRAGMENTS = ['43.138.163.7'];

function arg(name, fallback) {
  const index = process.argv.indexOf(name);
  if (index < 0 || index === process.argv.length - 1) return fallback;
  return process.argv[index + 1];
}

function hasFlag(name) {
  return process.argv.includes(name);
}

function percentile(samples, p) {
  if (!samples.length) return null;
  const sorted = [...samples].sort((a, b) => a - b);
  const index = Math.min(sorted.length - 1, Math.max(0, Math.ceil((p / 100) * sorted.length) - 1));
  return sorted[index];
}

function latencySummary(samples) {
  if (!samples.length) {
    return { n: 0, p50: null, p95: null, p99: null, max: null, errorRate: null };
  }
  return {
    n: samples.length,
    p50: percentile(samples, 50),
    p95: percentile(samples, 95),
    p99: percentile(samples, 99),
    max: Math.max(...samples),
  };
}

function planPadsAndConsoles(pads, consoles) {
  const statusIntervalMs = 4750;
  const connectedPollMs = 90_000;
  const answerEverySec = 10;
  const reconnectItems = 20;
  const autoSubmitChunk = 100;
  const autoSubmitMaxChunksPerTick = 5;
  return {
    pads,
    consoles,
    playerWsConnections: pads,
    consoleWsConnections: consoles,
    playerStatusPerMinDisconnected: Math.round(pads * (60_000 / statusIntervalMs)),
    playerStatusPerMinConnected: Math.round(pads * (60_000 / connectedPollMs)),
    playerContextOnce: pads,
    playerAnswerActionsPerMin: pads * (60 / answerEverySec),
    playerSyncBatchesPerMinApprox: Math.round((pads * (60 / answerEverySec)) / 20),
    reconnectTwentyAnswerBurst: {
      pads,
      itemsPerPad: reconnectItems,
      httpPosts: pads,
      expectedTransactions: pads,
    },
    unifiedDeadline: {
      sessions: 1,
      candidates: pads,
      waitingToAbsentSetBased: true,
      runningChunkSize: autoSubmitChunk,
      maxChunksPerTick: autoSubmitMaxChunksPerTick,
      ticksToDrainRunning: Math.ceil(pads / (autoSubmitChunk * autoSubmitMaxChunksPerTick)),
    },
    adminHttpWhenWsDownPerMin: consoles * 12,
    adminHttpWhenWsUpPerMin: consoles * 0.5,
    notes: [
      'Answers/paper/submit stay on HTTP. WS is status/control/ACK only.',
      'Connected pads should poll GET /status every 60–120s, not every 4s.',
      'Do not broadcast a 3000-row candidate array on WS.',
      'Reconnect burst is one POST /answers/sync of 20 items per pad, one DB TX.',
      'Unified deadline uses claim + chunk auto-submit; scoring is a separate scheduler.',
    ],
  };
}

function phase6Catalog() {
  return {
    '1-pads-and-consoles': PAD_SCENARIOS.flatMap((pads) =>
      CONSOLE_SCENARIOS.map((consoles) => planPadsAndConsoles(pads, consoles))),
    '2-ws-steady-3000': {
      playerSockets: 3000,
      consoleSockets: 10,
      heartbeatSeconds: 20,
      presenceTtlSeconds: 60,
      ticketTtlSeconds: 45,
      measured: false,
    },
    '3-answer-action-every-10s': {
      pads: 3000,
      actionsPerMin: 3000 * 6,
      path: 'POST /competition/formal-exam/player/sessions/{id}/answers/sync',
      measured: false,
    },
    '4-offline-5min-then-20-item-burst': {
      pads: 3000,
      offlineSeconds: 300,
      itemsPerPad: 20,
      httpPosts: 3000,
      expectedTransactions: 3000,
      thunderingHerdMitigation: 'client full-jitter reconnect; server batch sync one TX',
      measured: false,
    },
    '5-unified-deadline-auto-submit': {
      candidates: 3000,
      claimLeaseSeconds: 25,
      chunkSize: 100,
      maxChunksPerTick: 5,
      schedulerDelayMillis: 5000,
      measured: false,
      unitCovered: 'CompetitionFormalExamAutoSubmitSchedulerTest / ExpiredSessionFinalizerTest',
    },
    '6-faults': {
      rollingRestart: {
        expected: 'HTTP exam continues on MySQL; WS clients reconnect with new tickets; Redis Pub/Sub fans out to the new node',
        measured: false,
      },
      redisDown: {
        expected: 'ticket/WS 503 实时通道暂时不可用; login/paper/answers/submit still MySQL',
        measured: false,
        unitCovered: 'FormalExamWsTicketServiceTest.issueWithoutRedisReturnsUnavailable',
      },
      socketCrossNode: {
        expected: 'outbox + Redis channel formal-exam:realtime; local hub only',
        measured: false,
        unitCovered: 'FormalExamOutboxDispatcherTest / FormalExamRealtimeHubTest',
      },
    },
    '7-fencing': {
      duplicateConnectionSameDevice: 'next generation, kick previous device login',
      otherDeviceWhileActive: '409 该选手已有有效考试会话',
      oldTokenOldGeneration: 'WS close stale generation; HTTP commands fenced',
      lateCommandDuplicateAck: 'command issued→acked CAS; duplicate ACK idempotent',
      measured: 'unit tests, not live 3000-pad',
      unitCovered: [
        'CompetitionFormalExamAccessSessionServiceTest',
        'FormalExamPlayerWebSocketHandlerTest',
        'FormalExamPlayerRealtimeApplicationServiceTest',
      ],
    },
    '8-admin-force': {
      forceSubmit: 'HTTP first, then WS FORCE_SUBMIT_COMPLETED / status',
      forceOffline: 'HTTP first, kick AccessSession, WS ACCESS_SESSION_KICKED',
      measured: 'unit tests + optional UI E2E, not 3000-pad',
      unitCovered: [
        'CompetitionFormalExamForceSubmitServiceTest',
        'CompetitionFormalExamForceOfflineServiceTest',
      ],
    },
  };
}

function normalizeBaseUrl(raw) {
  const trimmed = (raw || 'http://127.0.0.1:8080/api').replace(/\/$/, '');
  if (trimmed.endsWith('/api')) return trimmed;
  return `${trimmed}/api`;
}

function hostBlockedForLargeLive(baseUrl) {
  return BLOCKED_LARGE_HOST_FRAGMENTS.some((fragment) => baseUrl.includes(fragment));
}

async function timedFetch(url, options) {
  const started = Date.now();
  try {
    const response = await fetch(url, options);
    return { ok: response.ok, status: response.status, ms: Date.now() - started, response };
  } catch (error) {
    return { ok: false, status: 0, ms: Date.now() - started, error: String(error) };
  }
}

async function liveHealth(baseUrl, pads, durationSec) {
  const samples = [];
  const statuses = {};
  let errors = 0;
  const deadline = Date.now() + durationSec * 1000;
  const concurrency = Math.min(Math.max(1, pads), LIVE_PAD_CAP);
  while (Date.now() < deadline) {
    const batch = [];
    for (let i = 0; i < concurrency; i += 1) {
      batch.push(timedFetch(`${baseUrl}/actuator/health`));
    }
    const results = await Promise.all(batch);
    for (const result of results) {
      samples.push(result.ms);
      const key = String(result.status);
      statuses[key] = (statuses[key] || 0) + 1;
      if (!result.ok) errors += 1;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  return {
    measured: true,
    target: 'health-only',
    concurrency,
    durationSec,
    statuses,
    errors,
    errorRate: samples.length ? errors / samples.length : null,
    latencyMs: latencySummary(samples),
    warning: 'Health probe is not a 100/500/1000/3000-pad exam capacity result.',
  };
}

async function liveMetrics(baseUrl) {
  const names = [
    'jvm.memory.used',
    'hikaricp.connections.active',
    'hikaricp.connections.max',
    'formal_exam.http',
  ];
  const collected = {};
  for (const name of names) {
    const result = await timedFetch(`${baseUrl}/actuator/metrics/${encodeURIComponent(name)}`);
    if (!result.response) {
      collected[name] = { status: result.status, error: result.error || 'fetch failed' };
      continue;
    }
    const body = await result.response.text();
    collected[name] = {
      status: result.status,
      ms: result.ms,
      body: body.length > 1500 ? `${body.slice(0, 1500)}…` : body,
    };
  }
  return {
    measured: true,
    target: 'actuator-metrics',
    collected,
    warning: 'Metric scrape is process-local and not a 3000-pad result.',
  };
}

async function readJson(result) {
  if (!result.response) return null;
  const text = await result.response.text();
  try {
    return JSON.parse(text);
  } catch {
    return { raw: text };
  }
}

async function liveExam(options) {
  const {
    baseUrl, sessionId, account, password, pads, writeAnswers, durationSec,
  } = options;
  if (!sessionId || !account || !password) {
    return {
      measured: false,
      reason: 'exam live requires --session-id --account --password',
    };
  }
  if (pads > EXAM_PAD_CAP && process.env.FORMAL_EXAM_LOAD_ALLOW_LARGE !== '1') {
    return {
      measured: false,
      reason: `exam live refused pads=${pads}; cap is ${EXAM_PAD_CAP} unless FORMAL_EXAM_LOAD_ALLOW_LARGE=1`,
    };
  }
  if (hostBlockedForLargeLive(baseUrl) && pads > 2) {
    return {
      measured: false,
      reason: 'exam live against the shared 43.138.163.7 database is capped; use pads<=2 or an isolated DB',
    };
  }
  if (writeAnswers && hostBlockedForLargeLive(baseUrl)) {
    return {
      measured: false,
      reason: '--write-answers is blocked against 43.138.163.7',
    };
  }

  const deviceId = `load-pad-${Date.now()}`;
  const login = await timedFetch(`${baseUrl}/competition/formal-exam/player/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ account, password, sessionId, deviceId }),
  });
  const loginBody = await readJson(login);
  const data = loginBody && loginBody.data ? loginBody.data : null;
  const token = data && data.accessToken;
  if (!login.ok || !token) {
    return {
      measured: true,
      target: 'exam-login',
      writeAnswers: false,
      loginMs: login.ms,
      status: login.status,
      code: loginBody && loginBody.code,
      message: loginBody && loginBody.message,
      warning: 'Player login failed; no answers were written.',
    };
  }

  const headers = {
    Authorization: `Bearer ${token}`,
    'X-Tenant-Id': String(data.tenantId || ''),
  };
  const context = await timedFetch(
    `${baseUrl}/competition/formal-exam/player/sessions/${sessionId}/context`,
    { headers },
  );
  const statusSamples = [];
  const deadline = Date.now() + Math.max(3, durationSec) * 1000;
  while (Date.now() < deadline) {
    const status = await timedFetch(
      `${baseUrl}/competition/formal-exam/player/sessions/${sessionId}/status`,
      { headers },
    );
    statusSamples.push(status.ms);
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }

  const ticket = await timedFetch(`${baseUrl}/competition/formal-exam/player/realtime/ticket`, {
    method: 'POST',
    headers,
  });
  const ticketBody = await readJson(ticket);
  let ws = { attempted: false };
  const ticketValue = ticketBody && ticketBody.data && ticketBody.data.ticket;
  const wsPath = ticketBody && ticketBody.data && ticketBody.data.wsPath;
  if (ticket.ok && ticketValue && typeof WebSocket === 'function') {
    ws = await openTicketSocket(baseUrl, wsPath || '/api/ws/competition/formal-exam/player', ticketValue);
  } else if (ticket.status === 503) {
    ws = { attempted: false, redisUnavailable: true, message: ticketBody && ticketBody.message };
  }

  return {
    measured: true,
    target: 'exam-read-only',
    pads: 1,
    writeAnswers: false,
    loginMs: login.ms,
    contextMs: context.ms,
    contextStatus: context.status,
    statusLatencyMs: latencySummary(statusSamples),
    ticketStatus: ticket.status,
    ticketMs: ticket.ms,
    ws,
    generation: data.generation,
    accessSessionId: data.accessSessionId,
    warning: writeAnswers
      ? 'write-answers requested but this runner keeps exam live read-only unless isolated tooling is added'
      : 'Exam live did not POST answers or submit. Not a 3000-pad result.',
  };
}

function wsHttpToWs(baseUrl, wsPath) {
  const api = new URL(baseUrl);
  const proto = api.protocol === 'https:' ? 'wss:' : 'ws:';
  const path = wsPath.startsWith('/') ? wsPath : `/${wsPath}`;
  return `${proto}//${api.host}${path}`;
}

function openTicketSocket(baseUrl, wsPath, ticket) {
  return new Promise((resolve) => {
    if (typeof WebSocket !== 'function') {
      resolve({ attempted: false, reason: 'runtime has no WebSocket' });
      return;
    }
    const url = `${wsHttpToWs(baseUrl, wsPath)}?ticket=${encodeURIComponent(ticket)}`;
    const started = Date.now();
    let settled = false;
    const socket = new WebSocket(url);
    const finish = (payload) => {
      if (settled) return;
      settled = true;
      try {
        socket.close();
      } catch {
        /* ignore */
      }
      resolve(payload);
    };
    socket.addEventListener('open', () => {
      socket.send(JSON.stringify({ type: 'application.heartbeat' }));
      finish({ attempted: true, opened: true, ms: Date.now() - started });
    });
    socket.addEventListener('error', () => {
      finish({ attempted: true, opened: false, ms: Date.now() - started, error: 'ws error' });
    });
    socket.addEventListener('close', (event) => {
      finish({
        attempted: true,
        opened: false,
        ms: Date.now() - started,
        code: event.code,
        reason: event.reason,
      });
    });
    setTimeout(() => finish({ attempted: true, opened: false, ms: Date.now() - started, error: 'timeout' }), 5000);
  });
}

async function main() {
  const live = hasFlag('--live');
  const mode = arg('--mode', 'health');
  const pads = Number(arg('--pads', arg('--scenario', '100')));
  const consoles = Number(arg('--consoles', '1'));
  const durationSec = Number(arg('--duration-sec', '8'));
  const baseUrl = normalizeBaseUrl(arg('--base-url', 'http://127.0.0.1:8080/api'));

  const report = {
    dryRun: !live,
    generatedAt: new Date().toISOString(),
    policy: {
      answersOnHttp: true,
      websocketStatusControlAckOnly: true,
      noFullCandidateBroadcast: true,
      doNotInvent3000Capacity: true,
      sharedDbLargeLiveBlocked: BLOCKED_LARGE_HOST_FRAGMENTS,
    },
    catalog: phase6Catalog(),
    selected: planPadsAndConsoles(pads, consoles),
  };

  if (!live) {
    report.live = {
      measured: false,
      reason: 'dry-run default; pass --live --mode health|metrics|exam',
    };
    process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
    return;
  }

  if (mode === 'health') {
    report.live = await liveHealth(baseUrl, pads, durationSec);
  } else if (mode === 'metrics') {
    report.live = await liveMetrics(baseUrl);
  } else if (mode === 'exam') {
    report.live = await liveExam({
      baseUrl,
      sessionId: arg('--session-id', process.env.FORMAL_EXAM_LOAD_SESSION_ID || ''),
      account: arg('--account', process.env.FORMAL_EXAM_LOAD_ACCOUNT || ''),
      password: arg('--password', process.env.FORMAL_EXAM_LOAD_PASSWORD || ''),
      pads,
      writeAnswers: hasFlag('--write-answers'),
      durationSec,
    });
  } else {
    report.live = { measured: false, reason: `unknown --mode ${mode}` };
  }

  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
  process.exit(1);
});

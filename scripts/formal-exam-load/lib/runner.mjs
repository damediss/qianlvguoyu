import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { monitorEventLoopDelay, performance } from 'node:perf_hooks';
import { applyScenario } from './args.mjs';
import { normalizeBaseUrl, evaluateIsolation } from './gate.mjs';
import { createOps } from './ops.mjs';
import { createPadAgent, createWsStats } from './pad-agent.mjs';
import { createConsoleAgent } from './console-agent.mjs';
import { scrapeHealth, scrapeInfo, listMetricNames, pickMetricNames, scrapeMetrics, summarizeWs, nodeBaseUrls, scrapeHttpRequestCounts, nodeRequestShare } from './collector.mjs';
import { openMysql } from './mysql.mjs';
import { reconcile } from './reconcile.mjs';
import { shardSlice } from './shard.mjs';
import { fullJitter } from './crypto.mjs';
import { latencySummary } from './percentile.mjs';

const FINALIZE_SCENARIOS = new Set([
  'submit', 'normal', 'smoke', 'ws-gap-catchup',
  'L1', 'L2', 'L3', 'L4', 'l1', 'l2', 'l3', 'l4',
  'duplicate-submit',
]);

export async function proveIsolation(baseUrl, options) {
  const info = await scrapeInfo(baseUrl);
  const load = info.body?.formalExamLoadtest || info.data?.formalExamLoadtest || {};
  const health = await scrapeHealth(baseUrl);
  const isolatedInfra = Boolean(options.isolatedInfra);
  const gate = evaluateIsolation({
    jdbcUrl: load.jdbcUrlSanitized || '',
    jdbcSchema: load.jdbcSchema,
    jdbcHost: load.jdbcHost,
    profiles: load.profile || '',
    pads: options.pads,
    writeAnswers: options.writeAnswers,
    isolatedInfra,
    env: options.env || process.env,
  });
  return {
    gate,
    infoStatus: info.status,
    healthStatus: health.status,
    load,
    printed: {
      jdbcHost: load.jdbcHost || gate.jdbcHost,
      jdbcSchema: load.jdbcSchema || gate.jdbcSchema,
      apiNode: load.nodeId || '',
      pads: options.pads,
      writeAnswers: Boolean(options.writeAnswers),
    },
  };
}

export async function runLive(rawOptions) {
  const options = applyScenario({ ...rawOptions, baseUrl: normalizeBaseUrl(rawOptions.baseUrl) });
  const outputDir = options.outputDir || defaultOutputDir();
  fs.mkdirSync(outputDir, { recursive: true });
  const isolation = await proveIsolation(options.baseUrl, options);
  fs.writeFileSync(path.join(outputDir, 'gate.json'), `${JSON.stringify(isolation, null, 2)}\n`);
  console.log(JSON.stringify({
    gate: isolation.gate.ok ? 'PASS' : 'FAIL',
    ...isolation.printed,
    reasons: isolation.gate.reasons,
  }, null, 2));
  if (!isolation.gate.ok && (options.writeAnswers || options.pads > 10)) {
    return {
      blocked: true,
      reason: 'isolation gate failed',
      isolation,
      outputDir,
    };
  }

  const fixture = JSON.parse(fs.readFileSync(resolveFixture(options), 'utf8'));
  const pads = shardSlice(fixture.pads, options.shardIndex, options.shardCount);
  const ops = createOps();
  const wsStats = createWsStats();
  const eluStart = performance.eventLoopUtilization();
  const delay = monitorEventLoopDelay({ resolution: 20 });
  delay.enable();
  const cpuStart = process.cpuUsage();
  const agents = pads.map((pad) => createPadAgent({
    baseUrl: options.baseUrl,
    pad,
    sessionId: fixture.sessionId,
    password: fixture.playerPassword,
    ops,
    wsStats,
    scenario: options.scenario,
    writeAnswers: Boolean(options.writeAnswers),
  }));
  const consoles = (fixture.consolesAccounts || []).slice(0, options.consoles).map((account) =>
    createConsoleAgent({
      baseUrl: options.baseUrl,
      tenantCode: fixture.tenantCode,
      account,
      password: fixture.consolePassword,
      sessionId: fixture.sessionId,
      tenantId: fixture.tenantId,
      ops,
      wsStats,
    }));

  const startedAt = new Date().toISOString();
  await ramp(agents, options.rampSec, async (agent) => {
    const logged = await agent.login();
    if (logged) await agent.contextAndEnter();
  });
  for (const consoleAgent of consoles) {
    await consoleAgent.login();
  }
  if (options.startSession && consoles[0]) {
    await consoles[0].startSession(true);
  }
  await ramp(agents, Math.min(options.rampSec, 30), async (agent) => {
    if (agent.state.token) await agent.fetchPaper();
  });
  for (const consoleAgent of consoles) {
    await consoleAgent.connectWs();
  }
  await ramp(agents, Math.min(options.rampSec, 60), async (agent) => {
    if (agent.state.token) await agent.connectWs();
  });

  const metricsToken = consoles[0]?.state?.token;
  const metricNames = await listMetricNames(options.baseUrl, fixture.tenantId, metricsToken);
  const picked = pickMetricNames(metricNames.names || []);
  const midMetrics = await scrapeMetrics(
    options.baseUrl,
    fixture.tenantId,
    [...picked.wanted, ...picked.extra.slice(0, 20)],
    metricsToken,
  );
  const nodeUrls = nodeBaseUrls(options.env || process.env);
  const nodeSplitStart = await scrapeHttpRequestCounts(nodeUrls, fixture.tenantId, metricsToken);

  const loops = [
    ...agents.map((agent) => agent.loop(options.durationSec * 1000, options.scenario)),
    ...consoles.map((agent) => agent.loop(options.durationSec * 1000)),
  ];
  await Promise.all(loops);

  let finalization = null;
  if (options.writeAnswers && shouldFinalizeScenario(options.scenario)) {
    const barrierTimeoutMs = Math.max(
      1_000,
      Number(process.env.FORMAL_EXAM_LOAD_SYNC_BARRIER_TIMEOUT_MS || 120_000),
    );
    const barrierResults = await Promise.all(
      agents.map((agent) => agent.syncBarrier({ timeoutMs: barrierTimeoutMs })),
    );
    const barrierOk = barrierResults.every((result) => result?.ok);
    const barrier = {
      ok: barrierOk,
      pads: barrierResults.length,
      passed: barrierResults.filter((result) => result?.ok).length,
      failed: barrierResults.filter((result) => !result?.ok).length,
      pendingSync: barrierResults.reduce((sum, result) => sum + Number(result?.pendingSync || 0), 0),
      offlineBuffer: barrierResults.reduce((sum, result) => sum + Number(result?.offlineBuffer || 0), 0),
      semanticRetryExhausted: barrierResults.reduce(
        (sum, result) => sum + Number(result?.semanticRetryExhausted || 0), 0,
      ),
      timeoutMs: barrierTimeoutMs,
    };
    const submitResults = [];
    if (barrierOk) {
      const rounds = options.scenario === 'duplicate-submit' ? 2 : 1;
      for (let round = 0; round < rounds; round += 1) {
        const results = await Promise.all(
          agents.map((agent) => agent.submit(true, { skipBarrier: true })),
        );
        submitResults.push(...results);
      }
    }
    finalization = {
      barrier,
      submit: {
        attempted: submitResults.length,
        ok: submitResults.filter((result) => result?.ok).length,
        failed: submitResults.filter((result) => !result?.ok).length,
      },
    };
  }

  const padSnapshots = agents.map((agent) => agent.snapshot());
  agents.forEach((agent) => agent.stop());
  consoles.forEach((agent) => agent.stop());

  let reconcileResult = null;
  if (options.writeAnswers || options.pads <= 10) {
    const connection = await openMysql(options.env);
    try {
      reconcileResult = await reconcile(connection, fixture, padSnapshots);
    } finally {
      await connection.end();
    }
  }

  delay.disable();
  const nodeSplitEnd = await scrapeHttpRequestCounts(nodeUrls, fixture.tenantId, metricsToken);
  const nodeSplit = nodeRequestShare(nodeSplitStart, nodeSplitEnd);
  const elu = performance.eventLoopUtilization(eluStart);
  const cpu = process.cpuUsage(cpuStart);
  const runner = {
    cpuUserMs: cpu.user / 1000,
    cpuSystemMs: cpu.system / 1000,
    rssBytes: process.memoryUsage().rss,
    eventLoopUtilization: elu.utilization,
    eventLoopDelayNs: { mean: delay.mean, max: delay.max, p99: delay.percentile(99) },
    bottleneck: elu.utilization > 0.85 || delay.mean > 250e6,
  };
  const report = {
    measured: true,
    target: 'formal-exam-live',
    startedAt,
    finishedAt: new Date().toISOString(),
    host: os.hostname(),
    options: {
      scenario: options.scenario,
      padsRequested: options.pads,
      padsThisShard: pads.length,
      consoles: consoles.length,
      durationSec: options.durationSec,
      writeAnswers: options.writeAnswers,
      shardIndex: options.shardIndex,
      shardCount: options.shardCount,
      nodes: options.nodes,
    },
    isolation: isolation.printed,
    gate: isolation.gate,
    operations: ops.snapshot().operations,
    errorSamples: ops.snapshot().errorSamples,
    ws: summarizeWs(wsStats),
    metrics: midMetrics,
    metricCatalog: picked,
    nodeSplit,
    reconcile: reconcileResult,
    finalization,
    padSnapshots,
    runner,
  };
  fs.writeFileSync(path.join(outputDir, 'live.json'), `${JSON.stringify(report, null, 2)}\n`);
  fs.writeFileSync(path.join(outputDir, 'ws.csv'), wsCsv(wsStats));
  return report;
}

function shouldFinalizeScenario(name) {
  return FINALIZE_SCENARIOS.has(name);
}

async function ramp(agents, rampSec, worker) {
  const windowMs = Math.max(1, rampSec) * 1000;
  await Promise.all(agents.map((agent, index) => new Promise((resolve) => {
    const delayMs = agents.length <= 1 ? 0 : Math.floor((index / agents.length) * windowMs);
    setTimeout(() => worker(agent).then(resolve).catch(resolve), delayMs + fullJitter(Math.min(250, windowMs)));
  })));
}

function resolveFixture(options) {
  if (options.fixture) return options.fixture;
  const here = path.dirname(fileURLToPath(import.meta.url));
  return path.join(here, '..', '.run', 'fixture.json');
}

function defaultOutputDir() {
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
  return path.join(repoRoot, 'outputs', `formal-exam-load-${stamp}`);
}

function wsCsv(stats) {
  return `attempted,opened,active,closed,failed,messages,heartbeats,acks,seqJumps,sequenceCollisions,outOfOrder,missedRelevantEvents,duplicates\n${
    [stats.attempted, stats.opened, stats.active, stats.closed, stats.connectFailed,
      stats.messages, stats.heartbeats, stats.acks, stats.seqJumps || 0,
      stats.sequenceCollisions || 0, stats.outOfOrder || 0, stats.missedRelevantEvents || 0,
      stats.duplicates].join(',')}\n`;
}

export { latencySummary };

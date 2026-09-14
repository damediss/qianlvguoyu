import { apiJson } from './http.mjs';
import { latencySummary } from './percentile.mjs';

const INTERESTING = [
  'jvm.memory.used',
  'jvm.memory.max',
  'jvm.threads.live',
  'process.cpu.usage',
  'http.server.requests',
  'formal_exam.http',
  'formal_exam.http.count',
  'formal_exam.sql.count',
  'formal_exam.sync.items',
  'formal_exam.sync.accepted',
  'formal_exam.sync.rejected',
  'formal_exam.finalize',
  'formal_exam.finalize.candidates',
  'formal_exam.finalize.claim',
  'formal_exam.ws',
  'formal_exam.ws.connections.active',
  'formal_exam.ws.send.dropped',
  'formal_exam.outbox.pending',
  'formal_exam.outbox.publishing',
  'formal_exam.outbox.dropped',
  'formal_exam.outbox.oldest_pending_age_seconds',
  'jdbc.connections.active',
  'jdbc.connections.idle',
  'jdbc.connections.max',
  'jdbc.connections.pending',
];

export async function listMetricNames(baseUrl, tenantId, token) {
  const result = await apiJson(baseUrl, '/actuator/metrics', { tenantId, token });
  const names = result.data?.names || result.body?.names || [];
  return { result, names };
}

export function pickMetricNames(available) {
  const wanted = [];
  const extra = [];
  for (const name of available) {
    if (INTERESTING.includes(name)) wanted.push(name);
    if (/druid|jdbc\.connections|hikaricp|formal_exam/i.test(name) && !INTERESTING.includes(name)) {
      extra.push(name);
    }
  }
  return { wanted, extra, ignoredHikari: available.filter((name) => name.startsWith('hikaricp.')) };
}

export async function scrapeMetrics(baseUrl, tenantId, names, token) {
  const collected = {};
  for (const name of names) {
    const result = await apiJson(baseUrl, `/actuator/metrics/${encodeURIComponent(name)}`, { tenantId, token });
    collected[name] = {
      status: result.status,
      ms: result.ms,
      measurements: result.body?.measurements || result.data?.measurements || [],
      availableTags: result.body?.availableTags || [],
    };
  }
  return collected;
}

export async function scrapeInfo(baseUrl) {
  return apiJson(baseUrl, '/actuator/info');
}

export async function scrapeHealth(baseUrl) {
  return apiJson(baseUrl, '/actuator/health');
}

export function summarizeWs(stats) {
  return {
    attempted: stats.attempted,
    opened: stats.opened,
    active: stats.active,
    closed: stats.closed,
    connectFailed: stats.connectFailed,
    redisUnavailable: stats.redisUnavailable,
    messages: stats.messages,
    heartbeats: stats.heartbeats,
    acks: stats.acks,
    seqJumps: stats.seqJumps || stats.seqGaps || 0,
    sequenceCollisions: stats.sequenceCollisions || 0,
    outOfOrder: stats.outOfOrder || 0,
    missedRelevantEvents: stats.missedRelevantEvents || 0,
    duplicates: stats.duplicates,
    connectLatencyMs: latencySummary(stats.connectSamples || []),
    openRate: stats.attempted ? stats.opened / stats.attempted : null,
  };
}

export function metricCount(metric) {
  const measurements = metric?.measurements || [];
  const row = measurements.find((item) => String(item.statistic || '').toUpperCase() === 'COUNT');
  return Number(row?.value) || 0;
}

export function nodeBaseUrls(env = process.env) {
  const host = env.API_BIND_HOST || '127.0.0.1';
  return [
    { node: 'api-1', baseUrl: `http://${host}:${Number(env.API_1_PORT || 18081)}/api` },
    { node: 'api-2', baseUrl: `http://${host}:${Number(env.API_2_PORT || 18082)}/api` },
  ];
}

export async function scrapeHttpRequestCounts(nodes, tenantId, token) {
  const rows = [];
  for (const node of nodes) {
    const collected = await scrapeMetrics(node.baseUrl, tenantId, ['http.server.requests'], token);
    const metric = collected['http.server.requests'] || {};
    rows.push({
      node: node.node,
      baseUrl: node.baseUrl,
      status: metric.status,
      count: metricCount(metric),
    });
  }
  return rows;
}

export function nodeRequestShare(startRows, endRows) {
  const byNode = new Map((endRows || []).map((row) => [row.node, row]));
  const nodes = (startRows || []).map((start) => {
    const end = byNode.get(start.node) || {};
    const delta = Math.max(0, (Number(end.count) || 0) - (Number(start.count) || 0));
    return {
      node: start.node,
      start: Number(start.count) || 0,
      end: Number(end.count) || 0,
      delta,
      scrapeOk: start.status === 200 && end.status === 200,
    };
  });
  const total = nodes.reduce((sum, row) => sum + row.delta, 0);
  const withShare = nodes.map((row) => ({
    ...row,
    share: total > 0 ? row.delta / total : null,
  }));
  const shares = withShare.map((row) => row.share).filter((share) => share != null);
  const balanced = total > 0
    && shares.length === 2
    && shares.every((share) => share >= 0.4 && share <= 0.6);
  return { total, balanced, nodes: withShare };
}

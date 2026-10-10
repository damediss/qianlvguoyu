import { latencySummary } from './percentile.mjs';

export function mergeReports(reports) {
  const pads = new Map();
  const operations = {};
  const ws = {
    attempted: 0, opened: 0, active: 0, closed: 0, connectFailed: 0,
    seqJumps: 0, sequenceCollisions: 0, outOfOrder: 0, missedRelevantEvents: 0, duplicates: 0,
    connectSamples: [],
  };
  for (const report of reports) {
    for (const snapshot of report.padSnapshots || []) {
      pads.set(String(snapshot.padIndex), snapshot);
    }
    const ops = report.operations || {};
    for (const [name, summary] of Object.entries(ops)) {
      if (!operations[name]) {
        operations[name] = {
          samples: [],
          errors: 0,
          n: 0,
          itemResults: {
            total: 0,
            accepted: 0,
            rejected: 0,
            byConflictCode: {},
            byOutcome: {},
          },
        };
      }
      operations[name].n += summary.n || 0;
      operations[name].errors += summary.errors || 0;
      const itemResults = summary.itemResults;
      if (itemResults) {
        operations[name].itemResults.total += itemResults.total || 0;
        operations[name].itemResults.accepted += itemResults.accepted || 0;
        operations[name].itemResults.rejected += itemResults.rejected || 0;
        for (const [code, count] of Object.entries(itemResults.byConflictCode || {})) {
          operations[name].itemResults.byConflictCode[code] =
            (operations[name].itemResults.byConflictCode[code] || 0) + count;
        }
        for (const [outcome, count] of Object.entries(itemResults.byOutcome || {})) {
          operations[name].itemResults.byOutcome[outcome] =
            (operations[name].itemResults.byOutcome[outcome] || 0) + count;
        }
      }
    }
    const w = report.ws || {};
    ws.attempted += w.attempted || 0;
    ws.opened += w.opened || 0;
    ws.closed += w.closed || 0;
    ws.connectFailed += w.connectFailed || 0;
    ws.messages += w.messages || 0;
    ws.heartbeats += w.heartbeats || 0;
    ws.acks += w.acks || 0;
    ws.seqJumps += w.seqJumps || w.seqGaps || 0;
    ws.sequenceCollisions += w.sequenceCollisions || 0;
    ws.outOfOrder += w.outOfOrder || 0;
    ws.missedRelevantEvents += w.missedRelevantEvents || 0;
    ws.duplicates += w.duplicates || 0;
    if (w.connectLatencyMs?.n) {
      ws.connectSamples.push(w.connectLatencyMs.p50, w.connectLatencyMs.p95);
    }
  }
  const mergedOps = {};
  for (const [name, item] of Object.entries(operations)) {
    mergedOps[name] = { n: item.n, errors: item.errors };
    if (item.itemResults.total > 0) {
      mergedOps[name].itemResults = item.itemResults;
    }
  }
  return {
    uniquePads: pads.size,
    operations: mergedOps,
    ws: {
      ...ws,
      connectLatencyMs: latencySummary(ws.connectSamples.filter((value) => value != null)),
    },
  };
}

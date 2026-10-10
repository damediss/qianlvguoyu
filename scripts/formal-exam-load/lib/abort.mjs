export function http5xxCount(operations = {}) {
  let total = 0;
  for (const item of Object.values(operations)) {
    const statuses = item.statuses || {};
    for (const [code, count] of Object.entries(statuses)) {
      if (Number(code) >= 500) total += Number(count) || 0;
    }
  }
  return total;
}

export function operationErrorRate(operations = {}, name) {
  const item = operations[name];
  if (!item) return 0;
  return Number(item.errorRate) || 0;
}

export function shouldStopEscalation(report = {}) {
  const reasons = [];
  if (report.blocked) reasons.push(report.reason || 'blocked');
  const operations = report.operations || {};
  const serverErrors = http5xxCount(operations);
  if (serverErrors > 0) reasons.push(`http 5xx count=${serverErrors}`);
  for (const name of ['login', 'enter', 'answer-sync', 'submit', 'ticket', 'ws-connect']) {
    const rate = operationErrorRate(operations, name);
    if (rate >= 0.05) reasons.push(`${name} errorRate=${rate}`);
  }
  const ws = report.ws || {};
  if (ws.attempted > 0 && ws.opened / ws.attempted < 0.8) {
    reasons.push(`ws open rate ${ws.opened}/${ws.attempted}`);
  }
  if (report.runner?.bottleneck) reasons.push('runner event-loop/CPU saturated');
  const reconcileFailures = report.reconcile?.failures || [];
  if (reconcileFailures.length) reasons.push(`reconcile ${reconcileFailures.length} failure(s)`);
  const dropped = report.metrics?.['formal_exam.ws.send.dropped']?.measurements
    ?.find((item) => item.statistic === 'VALUE' || item.statistic === 'COUNT');
  if (dropped && Number(dropped.value) > 0) {
    reasons.push(`ws send dropped=${dropped.value}`);
  }
  return { stop: reasons.length > 0, reasons };
}

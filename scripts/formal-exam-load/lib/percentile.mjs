export function percentile(samples, p) {
  if (!samples.length) return null;
  const sorted = [...samples].sort((a, b) => a - b);
  const index = Math.min(sorted.length - 1, Math.max(0, Math.ceil((p / 100) * sorted.length) - 1));
  return sorted[index];
}

export function latencySummary(samples) {
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

export function errorRate(total, errors) {
  if (!total) return null;
  return errors / total;
}

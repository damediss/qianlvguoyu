import test from 'node:test';
import assert from 'node:assert/strict';
import { http5xxCount, shouldStopEscalation } from '../lib/abort.mjs';

test('http5xxCount sums 5xx statuses only', () => {
  assert.equal(http5xxCount({
    login: { statuses: { 200: 10, 500: 2 } },
    enter: { statuses: { 503: 1, 409: 4 } },
  }), 3);
});

test('shouldStopEscalation on 5xx, reconcile, and runner saturation', () => {
  const stop = shouldStopEscalation({
    operations: { 'answer-sync': { errorRate: 0.2, statuses: { 500: 8 } } },
    ws: { attempted: 10, opened: 10 },
    reconcile: { failures: ['answerVersion mismatches 2'] },
    runner: { bottleneck: true },
  });
  assert.equal(stop.stop, true);
  assert.ok(stop.reasons.some((reason) => reason.includes('5xx')));
  assert.ok(stop.reasons.some((reason) => reason.includes('runner')));
});

test('healthy smoke does not stop escalation', () => {
  const stop = shouldStopEscalation({
    operations: { login: { errorRate: 0, statuses: { 200: 10 } } },
    ws: { attempted: 10, opened: 10 },
    reconcile: { failures: [] },
    runner: { bottleneck: false },
  });
  assert.equal(stop.stop, false);
});

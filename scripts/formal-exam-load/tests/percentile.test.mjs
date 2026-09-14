import test from 'node:test';
import assert from 'node:assert/strict';
import { percentile, latencySummary } from '../lib/percentile.mjs';

test('percentile uses nearest-rank', () => {
  const samples = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  assert.equal(percentile(samples, 50), 5);
  assert.equal(percentile(samples, 95), 10);
  assert.equal(percentile([], 95), null);
});

test('latency summary includes max', () => {
  const summary = latencySummary([10, 20, 30]);
  assert.equal(summary.n, 3);
  assert.equal(summary.max, 30);
  assert.equal(summary.p50, 20);
});

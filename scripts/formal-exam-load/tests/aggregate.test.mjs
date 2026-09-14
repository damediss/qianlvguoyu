import test from 'node:test';
import assert from 'node:assert/strict';
import { mergeReports } from '../lib/aggregate.mjs';

test('aggregate dedupes pads by index', () => {
  const merged = mergeReports([
    {
      padSnapshots: [{ padIndex: 1, candidateExamId: 'a' }, { padIndex: 2, candidateExamId: 'b' }],
      operations: { login: { n: 2, errors: 0 } },
      ws: { attempted: 2, opened: 2, connectLatencyMs: { n: 2, p50: 10, p95: 20 } },
    },
    {
      padSnapshots: [{ padIndex: 2, candidateExamId: 'b' }, { padIndex: 3, candidateExamId: 'c' }],
      operations: { login: { n: 2, errors: 1 } },
      ws: { attempted: 2, opened: 1 },
    },
  ]);
  assert.equal(merged.uniquePads, 3);
  assert.equal(merged.operations.login.n, 4);
  assert.equal(merged.operations.login.errors, 1);
  assert.equal(merged.ws.opened, 3);
});

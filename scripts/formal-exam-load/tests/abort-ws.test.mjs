import test from 'node:test';
import assert from 'node:assert/strict';
import { shouldStopEscalation } from '../lib/abort.mjs';

test('zero WS opens with attempts is an escalation stop', () => {
  const stop = shouldStopEscalation({
    operations: { login: { errorRate: 0, statuses: { 200: 10 } } },
    ws: { attempted: 10, opened: 0 },
    reconcile: { failures: [] },
  });
  assert.equal(stop.stop, true);
});

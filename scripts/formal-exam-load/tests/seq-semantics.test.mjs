import test from 'node:test';
import assert from 'node:assert/strict';
import {
  classifyFormalExamRealtimeEvent,
  mergeFormalExamRealtimeEvents,
  outboxIntegrityFailures,
} from '../lib/seq-semantics.mjs';

test('seq 1,5,12 is a jump not a loss', () => {
  const seen = new Set();
  const a = classifyFormalExamRealtimeEvent({ seen, lastSeq: null, eventId: 'a', seq: 1 });
  const b = classifyFormalExamRealtimeEvent({ seen, lastSeq: a.lastSeq, eventId: 'b', seq: 5 });
  const c = classifyFormalExamRealtimeEvent({ seen, lastSeq: b.lastSeq, eventId: 'c', seq: '12' });
  assert.equal(a.action, 'accept');
  assert.equal(b.action, 'accept');
  assert.equal(b.jump, true);
  assert.equal(c.action, 'accept');
  assert.equal(c.jump, true);
});

test('out-of-order and collision are distinct from duplicates', () => {
  const seen = new Set();
  assert.equal(classifyFormalExamRealtimeEvent({
    seen, lastSeq: 12, eventId: 'late', seq: 11,
  }).action, 'out-of-order');
  assert.equal(classifyFormalExamRealtimeEvent({
    seen, lastSeq: 12, eventId: 'same', seq: 12,
  }).action, 'collision');
  seen.add('dup');
  assert.equal(classifyFormalExamRealtimeEvent({
    seen, lastSeq: 12, eventId: 'dup', seq: 13,
  }).action, 'duplicate');
});

test('catch-up merge applies in seq order and eventId once', () => {
  const merged = mergeFormalExamRealtimeEvents(
    [{ eventId: 'c', seq: 5 }, { eventId: 'a', seq: 1 }],
    [{ eventId: 'b', seq: 12 }, { eventId: 'c', seq: 5 }],
  );
  const seen = new Set();
  const applied = [];
  let lastSeq = null;
  for (const envelope of merged) {
    const verdict = classifyFormalExamRealtimeEvent({
      seen, lastSeq, eventId: envelope.eventId, seq: envelope.seq,
    });
    if (verdict.action === 'duplicate') continue;
    if (verdict.action === 'accept') lastSeq = verdict.lastSeq;
    applied.push(envelope.eventId);
  }
  assert.deepEqual(applied, ['a', 'c', 'b']);
});

test('duplicate outbox seq is a reconcile failure', () => {
  const failures = outboxIntegrityFailures({
    dropped: 0,
    seqDup: [{ seq: 9, c: 2 }],
    eventDup: [],
  });
  assert.equal(failures.length, 1);
  assert.match(failures[0], /seq duplicates/);
});

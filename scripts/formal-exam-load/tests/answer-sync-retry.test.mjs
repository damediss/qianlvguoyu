import test from 'node:test';
import assert from 'node:assert/strict';
import {
  answerSyncBackoffMs,
  isRetryableAnswerSync,
  postAnswerSyncWithRetry,
} from '../lib/answer-sync-retry.mjs';

test('retryable answer sync is 503 or ANSWER_SYNC_RETRYABLE', () => {
  assert.equal(isRetryableAnswerSync({ status: 503, body: { code: '503' } }), true);
  assert.equal(isRetryableAnswerSync({
    status: 503,
    body: { code: 'ANSWER_SYNC_RETRYABLE' },
  }), true);
  assert.equal(isRetryableAnswerSync({ status: 500, body: { code: '500' } }), false);
  assert.equal(isRetryableAnswerSync({ status: 409, body: { code: 'ANSWERS_NOT_SYNCED' } }), false);
});

test('answer sync backoff is 200/400/800 plus jitter', () => {
  assert.equal(answerSyncBackoffMs(0, 0), 200);
  assert.equal(answerSyncBackoffMs(1, 50), 450);
  assert.equal(answerSyncBackoffMs(2, 200), 1000);
});

test('client retries retryable sync three times then keeps the last failure', async () => {
  const sleeps = [];
  const statuses = [];
  let calls = 0;
  const outcome = await postAnswerSyncWithRetry({
    items: [{ questionSnapshotId: '1', version: 1 }],
    send: async () => {
      calls += 1;
      return { ok: false, status: 503, body: { code: 'ANSWER_SYNC_RETRYABLE' }, ms: 3 };
    },
    record: (name, result) => statuses.push({ name, status: result.status }),
    sleepFn: async (ms) => { sleeps.push(ms); },
    random: () => 0,
  });
  assert.equal(outcome.ok, false);
  assert.equal(calls, 4);
  assert.deepEqual(sleeps, [200, 400, 800]);
  assert.equal(statuses.filter((row) => row.name === 'answer-sync-retry').length, 3);
  assert.equal(statuses.at(-1).name, 'answer-sync');
});

test('successful retry is not removed until the server accepts', async () => {
  let calls = 0;
  const outcome = await postAnswerSyncWithRetry({
    items: [{ questionSnapshotId: '1', version: 1 }],
    send: async () => {
      calls += 1;
      if (calls < 3) {
        return { ok: false, status: 503, body: { code: 'ANSWER_SYNC_RETRYABLE' }, ms: 4 };
      }
      return { ok: true, status: 200, data: { results: [{ accepted: true, version: 1 }] }, ms: 5 };
    },
    record: () => {},
    sleepFn: async () => {},
    random: () => 0,
  });
  assert.equal(outcome.ok, true);
  assert.equal(calls, 3);
});

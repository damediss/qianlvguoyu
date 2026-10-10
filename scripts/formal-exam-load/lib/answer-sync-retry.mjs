export const ANSWER_SYNC_CLIENT_RETRIES = 3;
export const ANSWER_SYNC_RETRYABLE_CODE = 'ANSWER_SYNC_RETRYABLE';

export function isRetryableAnswerSync(result) {
  if (!result) return false;
  const code = result.body?.code;
  return Number(result.status) === 503 || code === ANSWER_SYNC_RETRYABLE_CODE;
}

export function answerSyncBackoffMs(retryIndex, jitterMs = Math.floor(Math.random() * 201)) {
  const base = 200 * (2 ** retryIndex);
  return base + Math.max(0, Number(jitterMs) || 0);
}

export async function postAnswerSyncWithRetry(options) {
  const {
    send,
    items,
    record,
    sleepFn = (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
    random = Math.random,
    clientRetries = ANSWER_SYNC_CLIENT_RETRIES,
  } = options;
  if (!items.length) return { ok: true, result: null };
  const maxAttempts = 1 + clientRetries;
  let last = null;
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    last = await send(items);
    if (last.ok) {
      record('answer-sync', last);
      return { ok: true, result: last };
    }
    const canRetry = isRetryableAnswerSync(last) && attempt < maxAttempts - 1;
    if (!canRetry) {
      record('answer-sync', last);
      return { ok: false, result: last };
    }
    record('answer-sync-retry', last);
    await sleepFn(answerSyncBackoffMs(attempt, Math.floor(random() * 201)));
  }
  return { ok: false, result: last };
}

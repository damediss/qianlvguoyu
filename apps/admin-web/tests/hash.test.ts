import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import test from 'node:test';
import { sha256Blob, sha256Bytes } from '../src/utils/hash';

test('sha256Bytes matches node crypto for empty, ascii and binary payloads', () => {
  const samples = [
    new Uint8Array(),
    new TextEncoder().encode('abc'),
    new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  ];
  for (const sample of samples) {
    const expected = createHash('sha256').update(sample).digest('hex');
    assert.equal(sha256Bytes(sample), expected);
  }
});

test('sha256Blob falls back when Web Crypto is unavailable', async () => {
  const original = globalThis.crypto;
  Object.defineProperty(globalThis, 'crypto', { configurable: true, value: undefined });
  try {
    const bytes = new TextEncoder().encode('defense-sign');
    const blob = new Blob([bytes]);
    const expected = createHash('sha256').update(bytes).digest('hex');
    assert.equal(await sha256Blob(blob), expected);
    assert.equal((await sha256Blob(blob)).length, 64);
  } finally {
    Object.defineProperty(globalThis, 'crypto', { configurable: true, value: original });
  }
});

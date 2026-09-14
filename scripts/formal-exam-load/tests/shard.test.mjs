import test from 'node:test';
import assert from 'node:assert/strict';
import { shardSlice, shardRange } from '../lib/shard.mjs';

test('shards partition without overlap', () => {
  const items = ['a', 'b', 'c', 'd', 'e'];
  const zero = shardSlice(items, 0, 2);
  const one = shardSlice(items, 1, 2);
  assert.deepEqual(zero, ['a', 'c', 'e']);
  assert.deepEqual(one, ['b', 'd']);
  assert.equal(zero.length + one.length, items.length);
});

test('shard range covers 0..n-1', () => {
  const all = [...shardRange(5, 0, 2), ...shardRange(5, 1, 2)].sort((a, b) => a - b);
  assert.deepEqual(all, [0, 1, 2, 3, 4]);
});

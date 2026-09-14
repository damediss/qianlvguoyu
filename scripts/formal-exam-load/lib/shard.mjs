export function shardSlice(items, shardIndex, shardCount) {
  const count = Math.max(1, Number(shardCount) || 1);
  const index = Math.max(0, Number(shardIndex) || 0);
  if (index >= count) {
    throw new Error(`shard-index ${index} is out of range for shard-count ${count}`);
  }
  return items.filter((_, itemIndex) => itemIndex % count === index);
}

export function shardRange(total, shardIndex, shardCount) {
  const items = Array.from({ length: total }, (_, index) => index);
  return shardSlice(items, shardIndex, shardCount);
}

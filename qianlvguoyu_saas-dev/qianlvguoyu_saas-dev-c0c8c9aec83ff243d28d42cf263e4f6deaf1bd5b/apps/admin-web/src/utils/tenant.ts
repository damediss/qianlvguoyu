/** Normalize tenant id from API / storage (may be string or number). */
export function resolveTenantId(...sources: unknown[]): number | undefined {
  for (const source of sources) {
    if (source == null || source === '') {
      continue;
    }
    const value = Number(source);
    if (Number.isFinite(value) && value > 0) {
      return value;
    }
  }
  return undefined;
}

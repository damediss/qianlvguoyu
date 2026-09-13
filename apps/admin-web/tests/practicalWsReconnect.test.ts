import { describe, expect, it } from 'vitest';
import { practicalWsBackoffMs } from '../src/pages/assessor/practicalWsReconnect';

describe('practicalWsBackoffMs（实操采集 WS 重连退避，蓝绿方案 §5.6）', () => {
  it('attempt=0 落在 [0, 1000)', () => {
    for (let i = 0; i < 100; i++) {
      const delay = practicalWsBackoffMs(0);
      expect(delay).toBeGreaterThanOrEqual(0);
      expect(delay).toBeLessThan(1000);
    }
  });

  it('attempt=3 指数增长，上界 8000', () => {
    for (let i = 0; i < 100; i++) {
      const delay = practicalWsBackoffMs(3);
      expect(delay).toBeGreaterThanOrEqual(0);
      expect(delay).toBeLessThan(8000);
    }
  });

  it('封顶 30000：超大 attempt 不再增长', () => {
    expect(practicalWsBackoffMs(50, () => 1)).toBe(30000);
    expect(practicalWsBackoffMs(100, () => 0.999)).toBe(Math.floor(0.999 * 30000));
  });

  it('边界：random=0 → 0；负值防护为 0', () => {
    expect(practicalWsBackoffMs(0, () => 0)).toBe(0);
    expect(practicalWsBackoffMs(2, () => -1)).toBe(0);
  });

  it('full jitter：同 attempt 不同 random 产生不同延迟', () => {
    const a = practicalWsBackoffMs(4, () => 0.1);
    const b = practicalWsBackoffMs(4, () => 0.9);
    expect(b).toBeGreaterThan(a);
  });
});

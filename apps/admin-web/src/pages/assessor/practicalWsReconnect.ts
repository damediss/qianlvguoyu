/**
 * 实操采集 WS 断线重连退避（蓝绿方案 §5.6）。
 *
 * 服务端重启/蓝绿停旧色会掐断存量 WebSocket，考评端必须自动重连才能不中断评分现场。
 * full jitter：delay = random(0, min(cap, base·2^attempt))，成功连接后由调用方重置 attempt。
 */
export function practicalWsBackoffMs(attempt: number, random: () => number = Math.random): number {
  const base = 1000;
  const cap = 30000;
  const exp = Math.min(cap, base * 2 ** Math.max(0, attempt));
  return Math.floor(Math.max(0, random()) * exp);
}

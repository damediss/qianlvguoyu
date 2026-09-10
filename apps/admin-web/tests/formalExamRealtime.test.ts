import { describe, expect, it } from 'vitest';
import {
  buildFormalExamWsUrl,
  detectFormalExamSeqGap,
  formalExamPlayerEnvelopeRequiresReload,
  formalExamRealtimeBackoffMs,
  rememberFormalExamEventId,
  sameFormalExamId,
  shouldOpenFormalExamRealtimeSocket,
  startFormalExamRealtime,
} from '../src/pages/formalExam/FormalExamRealtimeClient';
import { formalExamPollIntervalMs } from '../src/pages/formalExam/formalExamStatus';

describe('formal exam realtime helpers', () => {
  it('does not open a real WebSocket in vitest', () => {
    expect(shouldOpenFormalExamRealtimeSocket()).toBe(false);
    const handle = startFormalExamRealtime({
      isActive: () => true,
      getGeneration: () => 1,
    });
    expect(() => handle.reconnect()).not.toThrow();
    handle.stop();
  });

  it('uses full jitter rather than aligned 3/6/12s', () => {
    expect(formalExamRealtimeBackoffMs(0, () => 0)).toBe(0);
    expect(formalExamRealtimeBackoffMs(0, () => 1)).toBe(1000);
    expect(formalExamRealtimeBackoffMs(2, () => 1)).toBe(4000);
    expect(formalExamRealtimeBackoffMs(8, () => 1)).toBe(30000);
  });

  it('dedups event ids and reports seq gaps', () => {
    const seen = new Set<string>();
    expect(rememberFormalExamEventId(seen, 'e1')).toBe(false);
    expect(rememberFormalExamEventId(seen, 'e1')).toBe(true);
    expect(detectFormalExamSeqGap(null, 1)).toEqual({ nextSeq: 1, gap: false });
    expect(detectFormalExamSeqGap(1, 2)).toEqual({ nextSeq: 2, gap: false });
    expect(detectFormalExamSeqGap(1, 4)).toEqual({ nextSeq: 4, gap: true });
    expect(detectFormalExamSeqGap(4, 3)).toEqual({ nextSeq: 4, gap: false });
  });

  it('builds the ticketed ws url without a bearer token', () => {
    const url = buildFormalExamWsUrl(
      '/api/ws/competition/formal-exam/player',
      'ticket-1',
      { protocol: 'https:', host: 'exam.example:5173' } as Location,
    );
    expect(url).toBe('wss://exam.example:5173/api/ws/competition/formal-exam/player?ticket=ticket-1');
    expect(url).not.toMatch(/Bearer|accessToken|token=/i);
  });

  it('keeps the 4s HTTP fallback even if WS would be connected', () => {
    expect(formalExamPollIntervalMs(true)).toBe(4000);
    expect(formalExamPollIntervalMs(false)).toBe(4000);
    expect(formalExamPollIntervalMs(true, { testRuntime: false, random: () => 0 })).toBe(4000);
    expect(formalExamPollIntervalMs(true, { testRuntime: false, random: () => 1 })).toBe(5500);
  });

  it('reloads the HTTP snapshot after session start or force-submit events', () => {
    expect(formalExamPlayerEnvelopeRequiresReload('session.status.changed')).toBe(true);
    expect(formalExamPlayerEnvelopeRequiresReload('snapshot.reload.required')).toBe(true);
    expect(formalExamPlayerEnvelopeRequiresReload('candidate.status.changed')).toBe(true);
    expect(formalExamPlayerEnvelopeRequiresReload('access-session.kicked')).toBe(false);
    expect(sameFormalExamId('101', 101)).toBe(true);
    expect(sameFormalExamId('101', '102')).toBe(false);
  });
});

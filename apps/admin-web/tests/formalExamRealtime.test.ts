import { describe, expect, it } from 'vitest';
import {
  buildFormalExamWsUrl,
  classifyFormalExamRealtimeEvent,
  formalExamPlayerEnvelopeRequiresReload,
  formalExamRealtimeBackoffMs,
  mergeFormalExamRealtimeEvents,
  parseFormalExamSeq,
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

  it('accepts seq 1, 5, 12 as jumps rather than loss', () => {
    const seen = new Set<string>();
    expect(rememberFormalExamEventId(seen, 'e1')).toBe(false);
    expect(rememberFormalExamEventId(seen, 'e1')).toBe(true);
    const first = classifyFormalExamRealtimeEvent({ seen: new Set(), lastSeq: null, eventId: 'a', seq: 1 });
    expect(first).toEqual({ action: 'accept', lastSeq: 1, jump: false });
    const jump = classifyFormalExamRealtimeEvent({ seen: new Set(), lastSeq: 1, eventId: 'b', seq: 5 });
    expect(jump).toEqual({ action: 'accept', lastSeq: 5, jump: true });
    const later = classifyFormalExamRealtimeEvent({ seen: new Set(), lastSeq: 5, eventId: 'c', seq: '12' });
    expect(later).toEqual({ action: 'accept', lastSeq: 12, jump: true });
  });

  it('flags out-of-order, collision and duplicate event ids', () => {
    const seen = new Set<string>();
    expect(classifyFormalExamRealtimeEvent({
      seen, lastSeq: 12, eventId: 'late', seq: 11,
    }).action).toBe('out-of-order');
    expect(classifyFormalExamRealtimeEvent({
      seen, lastSeq: 12, eventId: 'same-seq', seq: 12,
    }).action).toBe('collision');
    seen.add('dup');
    expect(classifyFormalExamRealtimeEvent({
      seen, lastSeq: 12, eventId: 'dup', seq: 13,
    }).action).toBe('duplicate');
  });

  it('merges catch-up and buffered events in seq order and keeps duplicates once', () => {
    const merged = mergeFormalExamRealtimeEvents(
      [{ eventId: 'c', seq: '5' }, { eventId: 'a', seq: 1 }],
      [{ eventId: 'b', seq: 12 }, { eventId: 'c', seq: 5 }],
    );
    expect(merged.map((item) => item.eventId)).toEqual(['a', 'c', 'c', 'b']);
    const seen = new Set<string>();
    const applied: string[] = [];
    let lastSeq: number | null = null;
    for (const envelope of merged) {
      const verdict = classifyFormalExamRealtimeEvent({
        seen, lastSeq, eventId: envelope.eventId, seq: envelope.seq,
      });
      if (verdict.action === 'duplicate') continue;
      if (verdict.action === 'accept') lastSeq = verdict.lastSeq;
      applied.push(String(envelope.eventId));
    }
    expect(applied).toEqual(['a', 'c', 'b']);
    expect(lastSeq).toBe(12);
  });

  it('parses string seq and rejects values above MAX_SAFE_INTEGER', () => {
    expect(parseFormalExamSeq('9')).toBe(9);
    expect(parseFormalExamSeq(Number.MAX_SAFE_INTEGER + 1)).toBeNull();
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

  it('applies reconnect catch-up then buffered live events in seq order', () => {
    const catchUp = [{ eventId: 'e1', seq: '1' }, { eventId: 'e5', seq: 5 }];
    const buffered = [{ eventId: 'e12', seq: 12 }, { eventId: 'e1', seq: 1 }];
    const seen = new Set<string>();
    let lastSeq: number | null = 0;
    const applied: string[] = [];
    for (const envelope of mergeFormalExamRealtimeEvents(catchUp, buffered)) {
      const verdict = classifyFormalExamRealtimeEvent({
        seen, lastSeq, eventId: envelope.eventId, seq: envelope.seq,
      });
      if (verdict.action === 'duplicate') continue;
      expect(verdict.action).toBe('accept');
      if (verdict.action === 'accept') lastSeq = verdict.lastSeq;
      applied.push(String(envelope.eventId));
    }
    expect(applied).toEqual(['e1', 'e5', 'e12']);
    expect(lastSeq).toBe(12);
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

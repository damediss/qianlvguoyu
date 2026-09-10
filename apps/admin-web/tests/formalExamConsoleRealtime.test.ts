import { describe, expect, it } from 'vitest';
import type { PageResponse } from '../src/types/api';
import type { CompetitionFormalCandidateExam, CompetitionFormalExamSession } from '../src/types/competition';
import {
  FORMAL_EXAM_CONSOLE_DISCONNECT_BANNER,
  applyFormalExamConsoleEnvelope,
  applyFormalExamSessionCountDelta,
  formalExamConsoleCandidatesQueryKey,
  formalExamConsolePollIntervalMs,
  isFormalExamConsoleCurrentPageCandidate,
  patchFormalExamConsoleCandidatePage,
  resolveFormalExamConnectionState,
  shouldOpenFormalExamConsoleRealtimeSocket,
  startFormalExamConsoleRealtime,
} from '../src/pages/competition/FormalExamConsoleRealtime';
import type { FormalExamRealtimeEnvelope } from '../src/pages/formalExam/FormalExamRealtimeClient';

const session: CompetitionFormalExamSession = {
  id: '9',
  eventId: '20',
  stageId: '10',
  eventWorkTypeId: '30',
  eventWorkTypeLevelId: '40',
  examType: 'theory',
  status: 'running',
  durationSeconds: 5400,
  candidateTotal: 3,
  submittedTotal: 0,
  waitingEnterCount: 1,
  readyCount: 0,
  runningCount: 2,
  submittedCount: 0,
  autoSubmittedCount: 0,
  forcedSubmittedCount: 0,
  absentCount: 0,
  paperCount: 1,
  papers: [],
};

const page: PageResponse<CompetitionFormalCandidateExam> = {
  items: [
    {
      id: '101',
      examSessionId: '9',
      candidateId: '1',
      contestantProfileId: '1',
      paperSnapshotId: '11',
      sourcePaperId: '500',
      status: 'running',
      answeredCount: 2,
      totalQuestionCount: 10,
      connectionState: 'online',
      accessSessionStatus: 'active',
    },
  ],
  page: 1,
  pageSize: 20,
  total: 3,
};

function envelope(partial: Partial<FormalExamRealtimeEnvelope>): FormalExamRealtimeEnvelope {
  return {
    eventId: 'e1',
    type: 'candidate.status.changed',
    examSessionId: '9',
    candidateExamId: '101',
    seq: 1,
    payload: {},
    ...partial,
  };
}

describe('formal exam console realtime helpers', () => {
  it('does not open a real WebSocket in vitest', () => {
    expect(shouldOpenFormalExamConsoleRealtimeSocket()).toBe(false);
    const handle = startFormalExamConsoleRealtime({
      examSessionId: '9',
      isActive: () => true,
      getPageCandidateIds: () => ['101'],
    });
    expect(() => handle.subscribePage(['101'])).not.toThrow();
    handle.stop();
  });

  it('keeps the 5s HTTP fallback whether or not WS is connected', () => {
    expect(formalExamConsolePollIntervalMs(true)).toBe(5000);
    expect(formalExamConsolePollIntervalMs(false)).toBe(5000);
    expect(formalExamConsolePollIntervalMs(true, { testRuntime: false, random: () => 0 })).toBe(5000);
    expect(formalExamConsolePollIntervalMs(true, { testRuntime: false, random: () => 1 })).toBe(5000);
    expect(formalExamConsolePollIntervalMs(false, { testRuntime: false, random: () => 0 })).toBe(5000);
  });

  it('includes filter and page in the snapshot query key', () => {
    expect(formalExamConsoleCandidatesQueryKey('9', '张', '11', 'running', 2, 20)).toEqual([
      'competition-formal-exam-session-candidates',
      '9',
      '张',
      '11',
      'running',
      2,
      20,
    ]);
  });

  it('patches the current page and applies status count deltas', () => {
    const seen = new Set<string>();
    const result = applyFormalExamConsoleEnvelope({
      envelope: envelope({
        payload: { status: 'submitted', previousStatus: 'running' },
      }),
      seen,
      lastSeq: null,
      pageCandidateIds: ['101'],
      session,
      page,
    });
    expect(result.duplicate).toBe(false);
    expect(result.invalidate).toBe(false);
    expect(result.session?.runningCount).toBe(1);
    expect(result.session?.submittedCount).toBe(1);
    expect(result.session?.submittedTotal).toBe(1);
    expect(result.page?.items[0]?.status).toBe('submitted');
    expect(result.page?.items[0]?.connectionState).toBe('submitted');
  });

  it('patches progress from page items even before subscribe-page ids are ready', () => {
    const result = applyFormalExamConsoleEnvelope({
      envelope: envelope({
        eventId: 'progress-before-subscribe',
        type: 'candidate.progress.changed',
        payload: { answeredCount: 2, totalQuestionCount: 2 },
      }),
      seen: new Set<string>(),
      lastSeq: 1,
      pageCandidateIds: [],
      session,
      page,
    });
    expect(result.page?.items[0]?.answeredCount).toBe(2);
    expect(result.page?.items[0]?.totalQuestionCount).toBe(2);
  });

  it('does not patch off-page candidates but still updates stats', () => {
    const result = applyFormalExamConsoleEnvelope({
      envelope: envelope({
        eventId: 'off-page',
        candidateExamId: '999',
        payload: { status: 'absent', previousStatus: 'waiting_enter' },
      }),
      seen: new Set<string>(),
      lastSeq: 1,
      pageCandidateIds: ['101'],
      session,
      page,
    });
    expect(isFormalExamConsoleCurrentPageCandidate(['101'], '999')).toBe(false);
    expect(result.page?.items[0]?.status).toBe('running');
    expect(result.session?.waitingEnterCount).toBe(0);
    expect(result.session?.absentCount).toBe(1);
  });

  it('ignores duplicate events and invalidates on seq gap or missing previousStatus', () => {
    const seen = new Set<string>();
    const first = applyFormalExamConsoleEnvelope({
      envelope: envelope({ eventId: 'dup', seq: 2, payload: { status: 'submitted', previousStatus: 'running' } }),
      seen,
      lastSeq: 1,
      pageCandidateIds: ['101'],
      session,
      page,
    });
    const duplicate = applyFormalExamConsoleEnvelope({
      envelope: envelope({ eventId: 'dup', seq: 2, payload: { status: 'submitted', previousStatus: 'running' } }),
      seen,
      lastSeq: first.lastSeq,
      pageCandidateIds: ['101'],
      session: first.session,
      page: first.page,
    });
    expect(duplicate.duplicate).toBe(true);
    expect(duplicate.session).toEqual(first.session);

    const gap = applyFormalExamConsoleEnvelope({
      envelope: envelope({ eventId: 'gap', seq: 9, payload: { status: 'submitted', previousStatus: 'running' } }),
      seen: new Set<string>(),
      lastSeq: 2,
      pageCandidateIds: ['101'],
      session,
      page,
    });
    expect(gap.gap).toBe(true);
    expect(gap.invalidate).toBe(true);

    const missingPrevious = applyFormalExamConsoleEnvelope({
      envelope: envelope({ eventId: 'no-prev', seq: 3, payload: { status: 'submitted' } }),
      seen: new Set<string>(),
      lastSeq: 2,
      pageCandidateIds: ['101'],
      session,
      page,
    });
    expect(missingPrevious.invalidate).toBe(true);
  });

  it('does not treat lastActiveTime as online and keeps presence seq-less', () => {
    expect(resolveFormalExamConnectionState({
      candidateStatus: 'running',
      accessSessionStatus: 'active',
      lastSeenAt: '2026-09-10T00:00:00.000Z',
      nowMs: Date.parse('2026-09-10T01:00:00.000Z'),
    })).toBe('briefly_offline');
    const presence = applyFormalExamConsoleEnvelope({
      envelope: envelope({
        eventId: 'presence',
        type: 'candidate.presence.changed',
        seq: null,
        payload: { connectionState: 'online', lastSeenAt: '2026-09-10T01:00:00.000Z' },
      }),
      seen: new Set<string>(),
      lastSeq: 4,
      pageCandidateIds: ['101'],
      session,
      page,
      nowMs: Date.parse('2026-09-10T01:00:10.000Z'),
    });
    expect(presence.gap).toBe(false);
    expect(presence.lastSeq).toBe(4);
    expect(presence.page?.items[0]?.connectionState).toBe('online');
  });

  it('patches kick on the current page without changing counts', () => {
    const result = applyFormalExamConsoleEnvelope({
      envelope: envelope({
        eventId: 'kick',
        type: 'access-session.kicked',
        payload: { reason: 'kicked' },
      }),
      seen: new Set<string>(),
      lastSeq: 1,
      pageCandidateIds: ['101'],
      session,
      page,
    });
    expect(result.session?.runningCount).toBe(2);
    expect(result.page?.items[0]?.connectionState).toBe('kicked');
    expect(result.page?.items[0]?.accessSessionStatus).toBe('kicked');
  });

  it('does not invent a 3000-row patch array', () => {
    const patched = patchFormalExamConsoleCandidatePage(page, '101', { answeredCount: 3 });
    expect(patched.patched).toBe(true);
    expect(patched.page?.items).toHaveLength(1);
    const delta = applyFormalExamSessionCountDelta(session, 'running', 'submitted');
    expect(delta?.submittedCount).toBe(1);
    expect(FORMAL_EXAM_CONSOLE_DISCONNECT_BANNER).toBe('实时连接中断/正在重连');
  });
});

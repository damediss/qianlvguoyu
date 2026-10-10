import { Button, Spin } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  FormalExamApiError,
  getFormalExamPlayerContext,
  getFormalExamPlayerPaper,
  submitFormalExamPlayer,
  type FormalExamPlayerContext,
  type FormalExamPlayerPaper,
} from '../../api/formalExamPlayer';
import { FormalExamAnswerCard } from './FormalExamAnswerCard';
import { FormalExamAnswerSyncService } from './FormalExamAnswerSyncService';
import { FormalExamQuestionView } from './FormalExamQuestionView';
import { mergeServerAndLocal } from './formalExamAnswerMerge';
import {
  type FormalExamLocalAnswer,
  type FormalExamQuestionState,
  type FormalExamSaveStatus,
} from './formalExamAnswerState';
import { formalExamLocalAnswerRepository } from './idb/FormalExamLocalAnswerRepository';
import type { LocalFormalExamAnswer } from './idb/formalExamLocalAnswerTypes';
import { formatRemaining, remainingMs } from './formalExamTime';
import {
  buildAnswerVersions,
  countPaperAnswers,
  inspectSubmitReadiness,
} from './formalExamSubmitPrepare';
import { isFormalExamClosedStatus } from './formalExamStatus';
import { formalExamRecoveryJitterMs, subscribeFormalExamRecovery } from './formalExamRecovery';

const LOCAL_PERSIST_MS = 200;
const NETWORK_SYNC_MS = 600;

interface Props {
  sessionId: string;
  context: FormalExamPlayerContext;
  onSubmitted: (context: FormalExamPlayerContext) => void;
}

type SubmitPhase = 'idle' | 'preparing' | 'confirm' | 'submitting';

function emptyAnswer(): FormalExamLocalAnswer {
  return { selectedOptionIds: [], blanks: [] };
}

function uiStatusOf(record: LocalFormalExamAnswer): FormalExamSaveStatus {
  if (record.syncStatus === 'SYNCED') return 'saved';
  if (record.syncStatus === 'SYNCING') return 'syncing';
  if (record.syncStatus === 'CONFLICT') return 'conflict';
  if (record.syncStatus === 'ERROR') return 'error';
  if (record.syncStatus === 'PENDING') return 'pending';
  return 'idle';
}

function stateFromLocal(record: LocalFormalExamAnswer): FormalExamQuestionState {
  return {
    answer: record.answer,
    marked: record.marked,
    version: record.version,
    requestedVersion: record.version,
    saveStatus: uiStatusOf(record),
    saveError: record.lastError,
  };
}

function emptyState(): FormalExamQuestionState {
  return {
    answer: emptyAnswer(),
    marked: false,
    version: 0,
    requestedVersion: 0,
    saveStatus: 'idle',
  };
}

function questionSaveLabel(state: FormalExamQuestionState | undefined, expired: boolean) {
  if (expired && state?.saveError === 'EXAM_TIME_EXPIRED') {
    return '考试已截止，该答案未能同步，请联系工作人员';
  }
  if (expired) return '考试时间已到，请等待系统处理';
  if (!state) return '';
  if (state.saveStatus === 'saving') return '正在保存…';
  if (state.saveStatus === 'pending') return '已保存到本机，待同步';
  if (state.saveStatus === 'syncing') return '正在同步…';
  if (state.saveStatus === 'saved') return '✓ 已保存';
  if (state.saveStatus === 'conflict') return '该题答案同步出现冲突，请联系工作人员';
  if (state.saveStatus === 'error') {
    if (state.saveError === 'EXAM_TIME_EXPIRED') {
      return '考试已截止，该答案未能同步，请联系工作人员';
    }
    return '同步失败，等待重试';
  }
  return '';
}

function globalBanner(
  records: LocalFormalExamAnswer[],
  online: boolean,
  expired: boolean,
) {
  const conflict = records.some((item) => item.syncStatus === 'CONFLICT');
  const expiredUnsynced = records.some((item) => item.lastError === 'EXAM_TIME_EXPIRED');
  const pending = records.filter((item) =>
    item.syncStatus === 'PENDING' || item.syncStatus === 'SYNCING' || (item.syncStatus === 'ERROR' && item.lastError !== 'EXAM_TIME_EXPIRED'));
  if (!online) return { tone: 'offline', text: '网络异常 · 答案已保存在本机，将在网络恢复后自动同步' };
  if (conflict) return { tone: 'conflict', text: '有答案同步异常，请联系工作人员' };
  if (expired && (expiredUnsynced || pending.length)) {
    return { tone: 'expired', text: '考试已截止，仍有本地答案未被服务器接收' };
  }
  if (pending.length) return { tone: 'pending', text: `${pending.length} 道答案待同步` };
  return { tone: 'synced', text: '✓ 所有答案已同步' };
}

function isNetworkFailure(error: unknown) {
  if (!(error instanceof FormalExamApiError)) return false;
  if (error.status === 401 || error.status === 403) return false;
  return !error.status || error.status >= 500 || /network|timeout|failed/i.test(error.message);
}

export function FormalExamAnsweringScreen({ sessionId, context, onSubmitted }: Props) {
  const queryClient = useQueryClient();
  const candidateExamId = String(context.candidateExam.id);
  const [index, setIndex] = useState(0);
  const [cardOpen, setCardOpen] = useState(false);
  const [states, setStates] = useState<Record<string, FormalExamQuestionState>>({});
  const [hydrated, setHydrated] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const [expired, setExpired] = useState(false);
  const [online, setOnline] = useState(() => (typeof navigator === 'undefined' ? true : navigator.onLine));
  const [localRecords, setLocalRecords] = useState<LocalFormalExamAnswer[]>([]);
  const [submitPhase, setSubmitPhase] = useState<SubmitPhase>('idle');
  const [submitHint, setSubmitHint] = useState('正在检查答案同步状态…');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [confirmStats, setConfirmStats] = useState({ total: 0, answered: 0, unanswered: 0 });
  const fillLocalTimer = useRef<number | null>(null);
  const fillSyncTimer = useRef<number | null>(null);
  const fillDirty = useRef(false);
  const statesRef = useRef(states);
  const versionsRef = useRef<Record<string, number>>({});
  const persistChain = useRef(Promise.resolve());
  const syncRef = useRef<FormalExamAnswerSyncService | null>(null);
  const submitPhaseRef = useRef(submitPhase);
  statesRef.current = states;
  submitPhaseRef.current = submitPhase;

  const paperQuery = useQuery({
    queryKey: ['formal-exam-player-paper', sessionId],
    queryFn: () => getFormalExamPlayerPaper(sessionId),
    enabled: Boolean(sessionId),
    staleTime: 60_000,
    refetchOnWindowFocus: false,
    retry: false,
  });

  const paper: FormalExamPlayerPaper | undefined = paperQuery.data;
  const questions = paper?.questions || [];
  const current = questions[index];
  const currentState = current ? states[current.questionSnapshotId] : undefined;
  const remaining = remainingMs(context.serverTime, context.examSession.deadlineTime, now);
  const timeUp = expired || remaining <= 0;
  const persistAllowed = hydrated
    && context.examSession.status === 'running'
    && context.candidateExam.status === 'running'
    && submitPhase !== 'submitting';
  const canEdit = persistAllowed && !timeUp && submitPhase === 'idle';
  const banner = useMemo(
    () => globalBanner(localRecords, online, timeUp),
    [localRecords, online, timeUp],
  );

  useEffect(() => {
    const sync = new FormalExamAnswerSyncService(formalExamLocalAnswerRepository);
    syncRef.current = sync;
    const unsubscribe = sync.subscribe(({ records }) => {
      setLocalRecords(records);
      setStates((previous) => {
        const next = { ...previous };
        for (const record of records) {
          const live = next[record.questionSnapshotId];
          if (live && live.version > record.version) continue;
          next[record.questionSnapshotId] = stateFromLocal(record);
        }
        return next;
      });
    });
    return () => {
      unsubscribe();
      sync.stop();
      syncRef.current = null;
    };
  }, [candidateExamId, sessionId]);

  useEffect(() => {
    let cancelled = false;
    async function hydrate(nextPaper: FormalExamPlayerPaper) {
      await formalExamLocalAnswerRepository.recoverStuckSyncing(sessionId, candidateExamId);
      const locals = await formalExamLocalAnswerRepository.getAllByScope(sessionId, candidateExamId);
      const localMap = new Map(locals.map((item) => [item.questionSnapshotId, item]));
      const serverMap = new Map((nextPaper.answers || []).map((item) => [String(item.questionSnapshotId), item]));
      const nextStates: Record<string, FormalExamQuestionState> = {};
      const mergedRecords: LocalFormalExamAnswer[] = [];
      for (const question of nextPaper.questions) {
        const questionSnapshotId = String(question.questionSnapshotId);
        const merged = mergeServerAndLocal({
          sessionId,
          candidateExamId,
          questionSnapshotId,
          server: serverMap.get(questionSnapshotId),
          local: localMap.get(questionSnapshotId),
        });
        if (merged.reason === 'server-newer') {
          console.info('[formal-exam] adopting newer server answer', {
            questionSnapshotId,
            localVersion: localMap.get(questionSnapshotId)?.version,
            serverVersion: serverMap.get(questionSnapshotId)?.version,
          });
        }
        if (merged.record) {
          const stored = await formalExamLocalAnswerRepository.upsert(merged.record);
          mergedRecords.push(stored);
          nextStates[questionSnapshotId] = stateFromLocal(stored);
          versionsRef.current[questionSnapshotId] = stored.version;
        } else {
          nextStates[questionSnapshotId] = emptyState();
        }
      }
      if (cancelled) return;
      setLocalRecords(mergedRecords);
      setStates(nextStates);
      setHydrated(true);
      syncRef.current?.start({ sessionId, candidateExamId });
    }
    if (!paper) return undefined;
    void hydrate(paper);
    return () => {
      cancelled = true;
    };
  }, [candidateExamId, paper, sessionId]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    const onOffline = () => setOnline(false);
    window.addEventListener('offline', onOffline);
    const unsubscribe = subscribeFormalExamRecovery((reason) => {
      if (reason === 'online') {
        setOnline(true);
        if (paperQuery.isError) void paperQuery.refetch();
      }
      const delay = formalExamRecoveryJitterMs();
      window.setTimeout(() => syncRef.current?.resume(), delay);
    });
    return () => {
      window.clearInterval(timer);
      window.removeEventListener('offline', onOffline);
      unsubscribe();
    };
  }, [paperQuery]);

  useEffect(() => {
    if (remaining > 0 || expired) return;
    setExpired(true);
    void queryClient.invalidateQueries({ queryKey: ['formal-exam-player-status', sessionId] });
  }, [expired, queryClient, remaining, sessionId]);

  useEffect(() => () => {
    if (fillLocalTimer.current) window.clearTimeout(fillLocalTimer.current);
    if (fillSyncTimer.current) window.clearTimeout(fillSyncTimer.current);
  }, []);

  const persist = useCallback((
    questionSnapshotId: string,
    patch: Partial<FormalExamQuestionState> & { answer?: FormalExamLocalAnswer },
    options?: { syncDelayMs?: number },
  ) => {
    if (!persistAllowed) return persistChain.current;
    persistChain.current = persistChain.current.then(async () => {
      const currentState = statesRef.current[questionSnapshotId] || emptyState();
      const nextAnswer = patch.answer ?? currentState.answer;
      const nextMarked = patch.marked ?? currentState.marked;
      const nextVersion = Math.max(
        currentState.version,
        currentState.requestedVersion,
        versionsRef.current[questionSnapshotId] || 0,
      ) + 1;
      versionsRef.current[questionSnapshotId] = nextVersion;
      setStates((previous) => ({
        ...previous,
        [questionSnapshotId]: {
          ...currentState,
          answer: nextAnswer,
          marked: nextMarked,
          version: nextVersion,
          requestedVersion: nextVersion,
          saveStatus: 'saving',
          saveError: undefined,
        },
      }));
      try {
        const existing = await formalExamLocalAnswerRepository.get(
          sessionId,
          candidateExamId,
          questionSnapshotId,
        );
        if (existing && existing.version > nextVersion) return;
        const stored = await formalExamLocalAnswerRepository.upsert({
          sessionId,
          candidateExamId,
          questionSnapshotId,
          answer: nextAnswer,
          marked: nextMarked,
          version: nextVersion,
          serverVersion: existing?.serverVersion || 0,
          syncStatus: 'PENDING',
          updatedAt: new Date().toISOString(),
          lastError: undefined,
        });
        setLocalRecords((previous) => {
          const others = previous.filter((item) => item.questionSnapshotId !== questionSnapshotId);
          return [...others, stored];
        });
        setStates((latest) => {
          const live = latest[questionSnapshotId];
          if (!live || live.version !== nextVersion) return latest;
          return {
            ...latest,
            [questionSnapshotId]: {
              ...live,
              saveStatus: 'pending',
            },
          };
        });
        syncRef.current?.schedule(options?.syncDelayMs ?? 0);
      } catch {
        setStates((latest) => {
          const live = latest[questionSnapshotId];
          if (!live || live.version !== nextVersion) return latest;
          return {
            ...latest,
            [questionSnapshotId]: {
              ...live,
              saveStatus: 'error',
              saveError: 'LOCAL_SAVE_FAILED',
            },
          };
        });
      }
    });
    return persistChain.current;
  }, [candidateExamId, persistAllowed, sessionId]);

  const flushFill = useCallback(async (questionSnapshotId?: string) => {
    const targetId = questionSnapshotId || current?.questionSnapshotId;
    if (!targetId) return;
    if (fillLocalTimer.current) window.clearTimeout(fillLocalTimer.current);
    if (fillSyncTimer.current) window.clearTimeout(fillSyncTimer.current);
    fillLocalTimer.current = null;
    fillSyncTimer.current = null;
    if (!fillDirty.current) {
      await persistChain.current;
      return;
    }
    fillDirty.current = false;
    const live = statesRef.current[targetId];
    await persist(targetId, {
      answer: live?.answer || emptyAnswer(),
      marked: live?.marked,
    }, { syncDelayMs: 0 });
    await persistChain.current;
  }, [current?.questionSnapshotId, persist]);

  const abortSubmit = (message: string) => {
    setSubmitError(message);
    setSubmitPhase('idle');
    setSubmitHint('正在检查答案同步状态…');
  };

  const recoverSubmittedContext = async () => {
    try {
      const latest = await getFormalExamPlayerContext(sessionId);
        if (isFormalExamClosedStatus(latest.candidateExam.status)) {
          syncRef.current?.stop();
          onSubmitted(latest);
          return true;
        }
    } catch {
      // keep the original submit error
    }
    return false;
  };

  const prepareSubmit = async () => {
    if (submitPhaseRef.current !== 'idle') return;
    setSubmitError(null);
    setSubmitPhase('preparing');
    setSubmitHint('正在检查答案同步状态…');
    try {
      await flushFill();
      await persistChain.current;
      let records = await formalExamLocalAnswerRepository.getAllByScope(sessionId, candidateExamId);
      let readiness = inspectSubmitReadiness(records);
      if (readiness.block === 'conflict') {
        abortSubmit('当前有答案同步冲突，暂时无法交卷。请联系现场工作人员处理。');
        return;
      }
      if (readiness.block === 'exam-time-expired') {
        abortSubmit('考试已截止，存在未能同步的本地答案。请联系现场工作人员处理。');
        return;
      }
      if (readiness.block === 'unsynced') {
        setSubmitHint('正在同步最后的答案…');
        try {
          await syncRef.current?.forceSync(15000);
        } catch {
          // re-read IndexedDB below and block if still unsynced
        }
        records = await formalExamLocalAnswerRepository.getAllByScope(sessionId, candidateExamId);
        readiness = inspectSubmitReadiness(records);
      }
      if (readiness.block === 'conflict') {
        abortSubmit('当前有答案同步冲突，暂时无法交卷。请联系现场工作人员处理。');
        return;
      }
      if (readiness.block === 'exam-time-expired') {
        abortSubmit('考试已截止，存在未能同步的本地答案。请联系现场工作人员处理。');
        return;
      }
      if (readiness.block === 'unsynced') {
        const pending = Math.max(readiness.retryableCount, 1);
        if (!online) {
          abortSubmit('当前网络不可用，仍有答案未同步。答案已经保存在本机。\n请保持设备开启，网络恢复后再交卷。');
          return;
        }
        abortSubmit(`仍有 ${pending} 道答案未同步到考试服务器，暂时无法交卷。\n答案已经保存在本机，请恢复网络后再次交卷。`);
        return;
      }
      const stats = countPaperAnswers(questions, statesRef.current);
      setConfirmStats(stats);
      setSubmitPhase('confirm');
    } catch {
      abortSubmit('当前网络不可用，仍有答案未同步。答案已经保存在本机。\n请保持设备开启，网络恢复后再交卷。');
    }
  };

  const confirmSubmit = async () => {
    if (submitPhaseRef.current !== 'confirm') return;
    setSubmitPhase('submitting');
    setSubmitHint('正在提交试卷…');
    try {
      const records = await formalExamLocalAnswerRepository.getAllByScope(sessionId, candidateExamId);
      const readiness = inspectSubmitReadiness(records);
      if (readiness.block) {
        abortSubmit(readiness.block === 'conflict'
          ? '当前有答案同步冲突，暂时无法交卷。请联系现场工作人员处理。'
          : readiness.block === 'exam-time-expired'
            ? '考试已截止，存在未能同步的本地答案。请联系现场工作人员处理。'
            : `仍有 ${Math.max(readiness.retryableCount, 1)} 道答案未同步到考试服务器，暂时无法交卷。\n答案已经保存在本机，请恢复网络后再次交卷。`);
        return;
      }
      const result = await submitFormalExamPlayer(sessionId, {
        confirmUnanswered: confirmStats.unanswered > 0,
        answerVersions: buildAnswerVersions(
          questions.map((item) => item.questionSnapshotId),
          records,
        ),
      });
      syncRef.current?.stop();
      onSubmitted({
        ...context,
        candidateExam: {
          ...context.candidateExam,
          status: result.candidateExamStatus,
          submitTime: result.submitTime,
        },
        serverTime: result.serverTime,
      });
      try {
        const latest = await getFormalExamPlayerContext(sessionId);
        if (isFormalExamClosedStatus(latest.candidateExam.status)) {
          onSubmitted(latest);
        }
      } catch {
        // submit already succeeded; local patch is enough
      }
    } catch (error) {
      if (await recoverSubmittedContext()) return;
      if (error instanceof FormalExamApiError && error.code === 'ANSWERS_NOT_SYNCED') {
        abortSubmit('部分答案尚未完成服务器同步，请稍后再交卷。');
        void paperQuery.refetch();
        return;
      }
      if (error instanceof FormalExamApiError && error.code === 'UNANSWERED_CONFIRM_REQUIRED') {
        const unanswered = Number((error.data as { unansweredCount?: number } | undefined)?.unansweredCount);
        setConfirmStats((previous) => ({
          ...previous,
          unanswered: Number.isFinite(unanswered) ? unanswered : previous.unanswered,
        }));
        setSubmitPhase('confirm');
        setSubmitError(error.message);
        return;
      }
      if (error instanceof FormalExamApiError && error.message.includes('考试时间已到')) {
        abortSubmit('考试时间已到，请等待系统处理');
        return;
      }
      abortSubmit(error instanceof Error ? error.message : '交卷失败，请稍后重试');
    }
  };

  useEffect(() => {
    const onHide = () => {
      if (document.visibilityState === 'hidden') void flushFill();
    };
    window.addEventListener('pagehide', onHide);
    document.addEventListener('visibilitychange', onHide);
    return () => {
      window.removeEventListener('pagehide', onHide);
      document.removeEventListener('visibilitychange', onHide);
    };
  }, [flushFill]);

  const retryCurrent = () => {
    if (!current) return;
    syncRef.current?.schedule(0);
  };

  const onChoiceChange = (selectedOptionIds: string[]) => {
    if (!current) return;
    void persist(current.questionSnapshotId, { answer: { selectedOptionIds } });
  };

  const onFillChange = (text: string) => {
    if (!current) return;
    const questionSnapshotId = current.questionSnapshotId;
    fillDirty.current = true;
    setStates((previous) => {
      const live = previous[questionSnapshotId] || emptyState();
      return {
        ...previous,
        [questionSnapshotId]: {
          ...live,
          answer: { blanks: [text] },
        },
      };
    });
    if (fillLocalTimer.current) window.clearTimeout(fillLocalTimer.current);
    if (fillSyncTimer.current) window.clearTimeout(fillSyncTimer.current);
    fillLocalTimer.current = window.setTimeout(() => {
      fillDirty.current = false;
      void persist(questionSnapshotId, { answer: { blanks: [text] } }, { syncDelayMs: NETWORK_SYNC_MS });
    }, LOCAL_PERSIST_MS);
  };

  const onFillCommit = () => {
    void flushFill();
  };

  const toggleMark = () => {
    if (!current || !currentState) return;
    void persist(current.questionSnapshotId, {
      answer: currentState.answer,
      marked: !currentState.marked,
    });
  };

  const goTo = (nextIndex: number) => {
    void flushFill().then(() => setIndex(nextIndex));
  };

  const jump = (questionSnapshotId: string) => {
    const nextIndex = questions.findIndex((item) => item.questionSnapshotId === questionSnapshotId);
    if (nextIndex >= 0) goTo(nextIndex);
    setCardOpen(false);
  };

  const headerClock = formatRemaining(remaining);
  const statusText = questionSaveLabel(currentState, timeUp);

  if (paperQuery.isLoading || (paper && !hydrated)) {
    return (
      <div className="formal-exam-player formal-exam-answering">
        <div className="formal-exam-player__card" style={{ alignItems: 'center' }}>
          <Spin size="large" />
          <p className="formal-exam-player__hint">正在加载试卷…</p>
        </div>
      </div>
    );
  }

  if (paperQuery.isError || !paper || !current) {
    const reconnect = isNetworkFailure(paperQuery.error) || paperQuery.fetchStatus === 'paused';
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card">
          <p className="formal-exam-player__error">
            {reconnect
              ? '暂时无法连接考试服务器，请保持应用开启并等待网络恢复'
              : (paperQuery.error instanceof Error ? paperQuery.error.message : '无法加载试卷')}
          </p>
          <Button type="primary" size="large" onClick={() => void paperQuery.refetch()}>
            重新加载
          </Button>
        </div>
      </div>
    );
  }

  const showRetry = currentState?.saveStatus === 'error'
    && currentState.saveError !== 'EXAM_TIME_EXPIRED'
    && canEdit;

  return (
    <div className="formal-exam-player formal-exam-answering">
      <header className="formal-exam-answering__header">
        <div>
          <strong>理论考试</strong>
          <span>{context.candidateExam.candidateName} {context.candidateExam.candidateNo}</span>
        </div>
        <div className="formal-exam-answering__meta">
          <span>剩余 {headerClock}</span>
          <span className={`formal-exam-answering__save is-${currentState?.saveStatus || 'idle'}`}>
            {statusText}
          </span>
        </div>
      </header>

      <p className={`formal-exam-answering__banner is-${banner.tone}`}>{banner.text}</p>

      {timeUp ? (
        <p className="formal-exam-answering__expired">考试时间已到，请等待系统处理</p>
      ) : null}

      <div className="formal-exam-answering__body">
        <FormalExamQuestionView
          question={current}
          answer={currentState?.answer || emptyAnswer()}
          disabled={!canEdit}
          onChoiceChange={onChoiceChange}
          onFillChange={onFillChange}
          onFillCommit={onFillCommit}
        />
        <div className="formal-exam-answering__mark">
          <Button size="large" disabled={!canEdit} onClick={toggleMark}>
            {currentState?.marked ? '取消标记' : '标记本题'}
          </Button>
          {showRetry ? (
            <Button size="large" type="primary" onClick={retryCurrent}>重试同步</Button>
          ) : null}
        </div>
      </div>

      <footer className="formal-exam-answering__footer">
        <Button size="large" disabled={index <= 0} onClick={() => goTo(Math.max(0, index - 1))}>
          上一题
        </Button>
        <span>{current.order} / {questions.length}</span>
        <Button
          size="large"
          disabled={index >= questions.length - 1}
          onClick={() => goTo(Math.min(questions.length - 1, index + 1))}
        >
          下一题
        </Button>
        <Button size="large" onClick={() => { void flushFill(); setCardOpen(true); }}>答题卡</Button>
        <Button
          size="large"
          danger
          className="formal-exam-answering__submit"
          autoInsertSpace={false}
          disabled={submitPhase !== 'idle'}
          onClick={() => void prepareSubmit()}
        >
          交卷
        </Button>
      </footer>

      <FormalExamAnswerCard
        open={cardOpen}
        questions={questions}
        states={states}
        currentId={current.questionSnapshotId}
        onClose={() => setCardOpen(false)}
        onJump={jump}
      />

      {submitPhase === 'preparing' || submitPhase === 'submitting' ? (
        <div className="formal-exam-submit-overlay" role="status">
          <Spin size="large" />
          <p>{submitHint}</p>
        </div>
      ) : null}

      {submitPhase === 'confirm' ? (
        <div className="formal-exam-submit-overlay">
          <div className="formal-exam-submit-dialog">
            <p>总题数：{confirmStats.total}</p>
            <p>已答：{confirmStats.answered}</p>
            <p>未答：{confirmStats.unanswered}</p>
            {confirmStats.unanswered === 0 ? (
              <>
                <p className="formal-exam-submit-dialog__lead">确认提交试卷？</p>
                <p>交卷后将无法继续修改答案。</p>
              </>
            ) : (
              <>
                <p className="formal-exam-submit-dialog__lead">当前还有 {confirmStats.unanswered} 道题未作答。</p>
                <p>交卷后将无法继续修改答案。</p>
                <p>是否确认交卷？</p>
              </>
            )}
            {submitError ? <p className="formal-exam-player__error">{submitError}</p> : null}
            <div className="formal-exam-submit-dialog__actions">
              <Button size="large" autoInsertSpace={false} onClick={() => { setSubmitPhase('idle'); setSubmitError(null); }}>
                继续答题
              </Button>
              <Button size="large" type="primary" danger autoInsertSpace={false} onClick={() => void confirmSubmit()}>
                确认交卷
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {submitPhase === 'idle' && submitError ? (
        <div className="formal-exam-submit-overlay">
          <div className="formal-exam-submit-dialog">
            {submitError.split('\n').map((line) => <p key={line}>{line}</p>)}
            <Button size="large" type="primary" autoInsertSpace={false} onClick={() => setSubmitError(null)}>
              返回答题
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

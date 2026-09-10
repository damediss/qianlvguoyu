import { Button, Form, Input, Spin, Typography } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import {
  FormalExamApiError,
  enterFormalExam,
  getFormalExamPlayerContext,
  getFormalExamPlayerStatus,
  loginFormalExamPlayer,
  type FormalExamPlayerContext,
  type FormalExamPlayerStatus,
} from '../../api/formalExamPlayer';
import { FormalExamAnsweringScreen } from './FormalExamAnsweringScreen';
import { FormalExamAbsentScreen } from './FormalExamAbsentScreen';
import { FormalExamTerminalScreen } from './FormalExamTerminalScreen';
import {
  formalExamInitialDelayMs,
  formalExamPollIntervalMs,
  isFormalExamTerminalStatus,
  shouldStopFormalExamPolling,
} from './formalExamStatus';
import { formalExamRecoveryJitterMs, subscribeFormalExamRecovery } from './formalExamRecovery';
import {
  FORMAL_EXAM_REALTIME,
  payloadStatus,
  sameFormalExamId,
  startFormalExamRealtime,
  type FormalExamRealtimeHandle,
} from './FormalExamRealtimeClient';
import { getOrCreateFormalExamDeviceId, useFormalExamPlayerStore } from '../../stores/formalExamPlayerStore';
import './FormalExamPlayerApp.css';

function readSessionId() {
  const value = new URLSearchParams(window.location.search).get('sessionId');
  return value && value.trim() ? value.trim() : '';
}

function replaceUrlSessionId(sessionId: string) {
  const url = new URL(window.location.href);
  url.searchParams.set('sessionId', sessionId);
  const next = `${url.pathname}${url.search}${url.hash}`;
  const current = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  if (current !== next) {
    window.history.replaceState(window.history.state, '', next);
  }
}

function stripUrlSessionId() {
  const url = new URL(window.location.href);
  if (!url.searchParams.has('sessionId')) return;
  url.searchParams.delete('sessionId');
  const next = `${url.pathname}${url.search}${url.hash}`;
  const current = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  if (current !== next) {
    window.history.replaceState(window.history.state, '', next);
  }
}

function durationLabel(seconds?: number | null) {
  if (!seconds || seconds <= 0) return '-';
  return `${Math.round(seconds / 60)} 分钟`;
}

function examTypeLabel(value?: string | null) {
  if (value === 'theory') return '理论考试';
  if (value === 'practical') return '实操考试';
  return value || '考试';
}

function isLoopbackHost() {
  const host = window.location.hostname;
  return host === 'localhost' || host === '127.0.0.1' || host === '[::1]';
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function isNetworkFailure(error: unknown) {
  if (!(error instanceof FormalExamApiError)) {
    return error instanceof Error && /network|timeout|failed/i.test(error.message);
  }
  if (error.status === 401 || error.status === 403) return false;
  return !error.status || error.status >= 500 || /network|timeout|failed/i.test(error.message);
}

function viewOf(context?: FormalExamPlayerContext | null) {
  const sessionStatus = context?.examSession.status;
  const status = context?.candidateExam.status;
  if (context?.runtimeIssue) return 'anomaly';
  if (status === 'absent') return 'absent';
  if (status === 'waiting_enter' && sessionStatus && sessionStatus !== 'ready') return 'blocked';
  if (status === 'waiting_enter') return 'confirm';
  if (status === 'ready') return 'waiting';
  if (status === 'running') return 'running';
  if (isFormalExamTerminalStatus(status)) return 'submitted';
  return 'confirm';
}

function mergeStatus(
  previous: FormalExamPlayerContext,
  status: FormalExamPlayerStatus,
): FormalExamPlayerContext {
  return {
    ...previous,
    examSession: {
      ...previous.examSession,
      ...status.examSession,
    },
    candidateExam: {
      ...previous.candidateExam,
      id: status.candidateExam.id,
      status: status.candidateExam.status,
      submitTime: status.candidateExam.submitTime ?? previous.candidateExam.submitTime,
    },
    serverTime: status.serverTime,
    runtimeIssue: status.runtimeIssue,
  };
}

export function FormalExamPlayerApp() {
  const urlSessionId = readSessionId();
  const boundSessionId = useFormalExamPlayerStore((state) => state.examSessionId);
  const token = useFormalExamPlayerStore((state) => state.token);
  const sessionId = token ? (boundSessionId || urlSessionId) : urlSessionId;
  const setSession = useFormalExamPlayerStore((state) => state.setSession);
  const queryClient = useQueryClient();
  const [loginError, setLoginError] = useState<string | null>(null);
  const [pollReady, setPollReady] = useState(false);
  const [realtimeConnected, setRealtimeConnected] = useState(false);
  const realtimeRef = useRef<FormalExamRealtimeHandle | null>(null);

  useEffect(() => {
    setPollReady(false);
    if (!token || !sessionId) return;
    const timer = window.setTimeout(() => setPollReady(true), formalExamInitialDelayMs());
    return () => window.clearTimeout(timer);
  }, [token, sessionId]);

  const contextQuery = useQuery({
    queryKey: ['formal-exam-player-context', sessionId, token],
    queryFn: () => getFormalExamPlayerContext(sessionId),
    enabled: Boolean(token && sessionId),
    refetchInterval: false,
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    networkMode: 'always',
    retry: false,
  });
  const stopPolling = shouldStopFormalExamPolling(contextQuery.data);
  const statusQuery = useQuery({
    queryKey: ['formal-exam-player-status', sessionId, token],
    queryFn: () => getFormalExamPlayerStatus(sessionId),
    enabled: Boolean(token && sessionId && pollReady && contextQuery.data) && !stopPolling,
    refetchInterval: (query) => {
      if (shouldStopFormalExamPolling({
        examSession: query.state.data?.examSession,
        candidateExam: query.state.data?.candidateExam,
      })) {
        return false;
      }
      return formalExamPollIntervalMs(realtimeConnected);
    },
    refetchIntervalInBackground: true,
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    networkMode: 'always',
    retry: false,
  });
  const sessionExpired = (contextQuery.error instanceof FormalExamApiError
      && (contextQuery.error.status === 401 || contextQuery.error.code === '401'))
    || (statusQuery.error instanceof FormalExamApiError
      && (statusQuery.error.status === 401 || statusQuery.error.code === '401'));

  useEffect(() => {
    if (!sessionExpired) return;
    setLoginError('考试账号已被工作人员清退，请重新登录');
    useFormalExamPlayerStore.getState().clear();
    stripUrlSessionId();
  }, [sessionExpired]);

  useEffect(() => {
    if (!token || !sessionId) return;
    const handle = startFormalExamRealtime({
      // A submitted candidate still needs the realtime control channel so a
      // later staff force-offline command can return the Pad to login.
      isActive: () => Boolean(useFormalExamPlayerStore.getState().token),
      getGeneration: () => useFormalExamPlayerStore.getState().generation,
      onConnected: () => setRealtimeConnected(true),
      onDisconnected: () => setRealtimeConnected(false),
      onKick: () => {
        setLoginError('考试账号已被工作人员清退，请重新登录');
        useFormalExamPlayerStore.getState().clear();
        stripUrlSessionId();
      },
      onSnapshotReload: () => {
        void queryClient.invalidateQueries({
          queryKey: ['formal-exam-player-status', sessionId, token],
        });
        void queryClient.invalidateQueries({
          queryKey: ['formal-exam-player-context', sessionId, token],
        });
        void queryClient.invalidateQueries({
          queryKey: ['formal-exam-player-paper', sessionId],
        });
      },
      onSeqGap: () => {
        void queryClient.invalidateQueries({
          queryKey: ['formal-exam-player-status', sessionId, token],
        });
        void queryClient.invalidateQueries({
          queryKey: ['formal-exam-player-context', sessionId, token],
        });
      },
      onEnvelope: (envelope) => {
        queryClient.setQueryData(
          ['formal-exam-player-context', sessionId, token],
          (previous: FormalExamPlayerContext | undefined) => {
            if (!previous) return previous;
            if (envelope.type === FORMAL_EXAM_REALTIME.SESSION_STATUS_CHANGED) {
              const status = payloadStatus(envelope.payload);
              return status
                ? { ...previous, examSession: { ...previous.examSession, status } }
                : previous;
            }
            if (envelope.candidateExamId
              && !sameFormalExamId(envelope.candidateExamId, previous.candidateExam.id)) {
              return previous;
            }
            if (envelope.type === FORMAL_EXAM_REALTIME.CANDIDATE_STATUS_CHANGED
              || envelope.type === FORMAL_EXAM_REALTIME.FORCE_SUBMIT_COMPLETED) {
              const status = payloadStatus(envelope.payload);
              return status
                ? { ...previous, candidateExam: { ...previous.candidateExam, status } }
                : previous;
            }
            return previous;
          },
        );
      },
    });
    realtimeRef.current = handle;
    return () => {
      handle.stop();
      realtimeRef.current = null;
      setRealtimeConnected(false);
    };
  }, [queryClient, sessionId, token]);

  useEffect(() => {
    const status = statusQuery.data;
    if (!status || !token) return;
    queryClient.setQueryData(
      ['formal-exam-player-context', sessionId, token],
      (previous: FormalExamPlayerContext | undefined) => (
        previous ? mergeStatus(previous, status) : previous
      ),
    );
  }, [queryClient, sessionId, statusQuery.data, token]);

  useEffect(() => {
    return subscribeFormalExamRecovery(() => {
      if (!token || !sessionId) return;
      const delay = formalExamRecoveryJitterMs();
      window.setTimeout(() => {
        realtimeRef.current?.reconnect();
        if (shouldStopFormalExamPolling(
          queryClient.getQueryData(['formal-exam-player-context', sessionId, token]),
        )) {
          return;
        }
        void queryClient.invalidateQueries({
          queryKey: ['formal-exam-player-status', sessionId, token],
        });
        const hasContext = queryClient.getQueryData(['formal-exam-player-context', sessionId, token]);
        if (!hasContext) {
          void queryClient.invalidateQueries({
            queryKey: ['formal-exam-player-context', sessionId, token],
          });
        }
      }, delay);
    });
  }, [queryClient, sessionId, token]);

  const loginMutation = useMutation({
    mutationFn: (values: { account: string; password: string }) =>
      loginFormalExamPlayer(
        values.account,
        values.password,
        urlSessionId || undefined,
        getOrCreateFormalExamDeviceId(),
      ),
    onSuccess: (data) => {
      setLoginError(null);
      const nextSessionId = data.examSessionId ? String(data.examSessionId) : sessionId;
      if (nextSessionId) {
        replaceUrlSessionId(nextSessionId);
      }
      setSession({
        token: data.accessToken,
        tenantId: String(data.tenantId),
        playerId: String(data.playerId),
        accessSessionId: data.accessSessionId ? String(data.accessSessionId) : undefined,
        generation: data.generation,
        deviceId: data.deviceId,
        examSessionId: nextSessionId || undefined,
      });
    },
    onError: (error) => setLoginError(errorText(error, '登录失败')),
  });

  const enterMutation = useMutation({
    mutationFn: () => enterFormalExam(sessionId),
    onSuccess: (data) => {
      queryClient.setQueryData(['formal-exam-player-context', sessionId, token], data);
    },
  });

  if (!token || sessionExpired) {
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card">
          <h1 className="formal-exam-player__title">理论考试</h1>
          <p className="formal-exam-player__subtitle">请使用选手账号登录</p>
          {isLoopbackHost() ? (
            <p className="formal-exam-player__error">
              当前地址是 localhost，平板连热点后打不开本机服务。请改用电脑热点网关访问，Windows 一般为 http://192.168.137.1:8080/formal-exam
            </p>
          ) : null}
          <Form
            layout="vertical"
            size="large"
            onFinish={(values) => loginMutation.mutate(values)}
          >
            <Form.Item name="account" label="手机号" rules={[{ required: true, message: '请输入手机号' }]}>
              <Input autoComplete="username" inputMode="numeric" placeholder="请输入手机号" />
            </Form.Item>
            <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password autoComplete="current-password" placeholder="请输入密码" />
            </Form.Item>
            {loginError ? <p className="formal-exam-player__error">{loginError}</p> : null}
            <Button type="primary" htmlType="submit" size="large" loading={loginMutation.isPending} autoInsertSpace={false}>
              登录
            </Button>
          </Form>
        </div>
      </div>
    );
  }

  if (!sessionId) {
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card">
          <h1 className="formal-exam-player__title">理论考试</h1>
          <p className="formal-exam-player__error">缺少比赛场次，请联系工作人员</p>
        </div>
      </div>
    );
  }

  if (contextQuery.isLoading) {
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card" style={{ alignItems: 'center' }}>
          <Spin size="large" />
        </div>
      </div>
    );
  }

  if (contextQuery.isError || !contextQuery.data) {
    const reconnect = isNetworkFailure(contextQuery.error) || contextQuery.fetchStatus === 'paused';
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card">
          <h1 className="formal-exam-player__title">理论考试</h1>
          <p className="formal-exam-player__error">
            {reconnect
              ? '暂时无法连接考试服务器，请保持应用开启并等待网络恢复'
              : errorText(contextQuery.error, '无法获取本场比赛信息，请联系工作人员')}
          </p>
        </div>
      </div>
    );
  }

  const context = contextQuery.data;
  const view = viewOf(context);
  const competition = context.competition;
  const candidate = context.candidateExam;

  if (view === 'confirm') {
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card">
          <h1 className="formal-exam-player__title">{competition.eventName || '正式比赛'}</h1>
          <p className="formal-exam-player__subtitle">{examTypeLabel(context.examSession.examType)}</p>
          <div className="formal-exam-player__meta">
            <p>姓名：{candidate.candidateName || '-'}</p>
            <p>选手编号：{candidate.candidateNo || '-'}</p>
            <p>工种：{competition.tradeName || '-'}</p>
            <p>技能等级：{competition.skillLevelName || '-'}</p>
            <p>考试时长：{durationLabel(context.examSession.durationSeconds)}</p>
          </div>
          <p className="formal-exam-player__hint">请确认以上信息无误。</p>
          {enterMutation.isError ? (
            <p className="formal-exam-player__error">{errorText(enterMutation.error, '进入比赛失败')}</p>
          ) : null}
          <div className="formal-exam-player__actions">
            <Button
              type="primary"
              size="large"
              autoInsertSpace={false}
              loading={enterMutation.isPending}
              onClick={() => enterMutation.mutate()}
            >
              确认进入比赛
            </Button>
          </div>
        </div>
      </div>
    );
  }

  if (view === 'blocked') {
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card">
          <h1 className="formal-exam-player__title">{examTypeLabel(context.examSession.examType)}</h1>
          <p className="formal-exam-player__name">{candidate.candidateName || '-'}</p>
          <p className="formal-exam-player__status">本场比赛已经开始。</p>
          <p className="formal-exam-player__hint">您尚未完成赛前候考进入，请联系现场工作人员处理。</p>
        </div>
      </div>
    );
  }

  if (view === 'anomaly') {
    return (
      <div className="formal-exam-player">
        <div className="formal-exam-player__card">
          <h1 className="formal-exam-player__title">{examTypeLabel(context.examSession.examType)}</h1>
          <p className="formal-exam-player__name">{candidate.candidateName || '-'}</p>
          <p className="formal-exam-player__status">运行数据异常</p>
          <p className="formal-exam-player__hint">{context.runtimeIssue || '请联系现场工作人员处理。'}</p>
        </div>
      </div>
    );
  }

  if (view === 'absent') {
    return <FormalExamAbsentScreen context={context} />;
  }

  if (view === 'submitted') {
    return <FormalExamTerminalScreen context={context} />;
  }

  if (view === 'running') {
    return (
      <FormalExamAnsweringScreen
        sessionId={sessionId}
        context={context}
        onSubmitted={(next) => {
          queryClient.setQueryData(['formal-exam-player-context', sessionId, token], next);
        }}
      />
    );
  }

  return (
    <div className="formal-exam-player">
      <div className="formal-exam-player__card">
        <h1 className="formal-exam-player__title">{examTypeLabel(context.examSession.examType)}</h1>
        <p className="formal-exam-player__name">{candidate.candidateName || '-'}</p>
        <p className="formal-exam-player__subtitle">选手编号 {candidate.candidateNo || '-'}</p>
        <div className="formal-exam-player__checks">
          <Typography.Text>✓ 身份确认完成</Typography.Text>
          <Typography.Text>✓ 已进入比赛</Typography.Text>
        </div>
        <p className="formal-exam-player__status">当前状态：等待开考</p>
        <p className="formal-exam-player__hint">请等待工作人员统一开始比赛</p>
      </div>
    </div>
  );
}

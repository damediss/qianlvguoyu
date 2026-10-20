import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Form, Input, Spin, Typography } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { FormalExamApiError, enterFormalExam, getFormalExamPlayerContext, getFormalExamPlayerStatus, loginFormalExamPlayer, fetchFormalExamPlayerRealtimeEvents, } from '../../api/formalExamPlayer';
import { FormalExamAnsweringScreen } from './FormalExamAnsweringScreen';
import { FormalExamAbsentScreen } from './FormalExamAbsentScreen';
import { FormalExamTerminalScreen } from './FormalExamTerminalScreen';
import { formalExamInitialDelayMs, formalExamPollIntervalMs, isFormalExamTerminalStatus, shouldStopFormalExamPolling, } from './formalExamStatus';
import { formalExamRecoveryJitterMs, subscribeFormalExamRecovery } from './formalExamRecovery';
import { FORMAL_EXAM_REALTIME, payloadStatus, sameFormalExamId, startFormalExamRealtime, } from './FormalExamRealtimeClient';
import { getOrCreateFormalExamDeviceId, useFormalExamPlayerStore } from '../../stores/formalExamPlayerStore';
import './FormalExamPlayerApp.css';
function readSessionId() {
    const value = new URLSearchParams(window.location.search).get('sessionId');
    return value && value.trim() ? value.trim() : '';
}
function replaceUrlSessionId(sessionId) {
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
    if (!url.searchParams.has('sessionId'))
        return;
    url.searchParams.delete('sessionId');
    const next = `${url.pathname}${url.search}${url.hash}`;
    const current = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    if (current !== next) {
        window.history.replaceState(window.history.state, '', next);
    }
}
function durationLabel(seconds) {
    if (!seconds || seconds <= 0)
        return '-';
    return `${Math.round(seconds / 60)} 分钟`;
}
function examTypeLabel(value) {
    if (value === 'theory')
        return '理论考试';
    if (value === 'practical')
        return '实操考试';
    return value || '考试';
}
function isLoopbackHost() {
    const host = window.location.hostname;
    return host === 'localhost' || host === '127.0.0.1' || host === '[::1]';
}
function errorText(error, fallback) {
    return error instanceof Error && error.message ? error.message : fallback;
}
function isNetworkFailure(error) {
    if (!(error instanceof FormalExamApiError)) {
        return error instanceof Error && /network|timeout|failed/i.test(error.message);
    }
    if (error.status === 401 || error.status === 403)
        return false;
    return !error.status || error.status >= 500 || /network|timeout|failed/i.test(error.message);
}
function viewOf(context) {
    const status = context?.candidateExam.status;
    if (context?.runtimeIssue)
        return 'anomaly';
    if (status === 'absent')
        return 'absent';
    if (status === 'waiting_enter')
        return 'confirm';
    if (status === 'ready')
        return 'waiting';
    if (status === 'running')
        return 'running';
    if (isFormalExamTerminalStatus(status))
        return 'submitted';
    return 'confirm';
}
function mergeStatus(previous, status) {
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
    const [loginError, setLoginError] = useState(null);
    const [pollReady, setPollReady] = useState(false);
    const [realtimeConnected, setRealtimeConnected] = useState(false);
    const realtimeRef = useRef(null);
    useEffect(() => {
        setPollReady(false);
        if (!token || !sessionId)
            return;
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
        if (!sessionExpired)
            return;
        setLoginError('考试账号已被工作人员清退，请重新登录');
        useFormalExamPlayerStore.getState().clear();
        stripUrlSessionId();
    }, [sessionExpired]);
    useEffect(() => {
        if (!token || !sessionId)
            return;
        const handle = startFormalExamRealtime({
            // A submitted candidate still needs the realtime control channel so a
            // later staff force-offline command can return the Pad to login.
            isActive: () => Boolean(useFormalExamPlayerStore.getState().token),
            getGeneration: () => useFormalExamPlayerStore.getState().generation,
            cursorKey: `formal-exam:player:lastSeq:${sessionId}`,
            fetchCatchUp: (afterSeq) => fetchFormalExamPlayerRealtimeEvents({ afterSeq, limit: 200 }),
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
            onOutOfOrder: () => {
                void queryClient.invalidateQueries({
                    queryKey: ['formal-exam-player-status', sessionId, token],
                });
                void queryClient.invalidateQueries({
                    queryKey: ['formal-exam-player-context', sessionId, token],
                });
            },
            onSequenceCollision: () => {
                void queryClient.invalidateQueries({
                    queryKey: ['formal-exam-player-status', sessionId, token],
                });
                void queryClient.invalidateQueries({
                    queryKey: ['formal-exam-player-context', sessionId, token],
                });
            },
            onEnvelope: (envelope) => {
                queryClient.setQueryData(['formal-exam-player-context', sessionId, token], (previous) => {
                    if (!previous)
                        return previous;
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
                });
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
        if (!status || !token)
            return;
        queryClient.setQueryData(['formal-exam-player-context', sessionId, token], (previous) => (previous ? mergeStatus(previous, status) : previous));
    }, [queryClient, sessionId, statusQuery.data, token]);
    useEffect(() => {
        return subscribeFormalExamRecovery(() => {
            if (!token || !sessionId)
                return;
            const delay = formalExamRecoveryJitterMs();
            window.setTimeout(() => {
                realtimeRef.current?.reconnect();
                if (shouldStopFormalExamPolling(queryClient.getQueryData(['formal-exam-player-context', sessionId, token]))) {
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
        mutationFn: (values) => loginFormalExamPlayer(values.account, values.password, urlSessionId || undefined, getOrCreateFormalExamDeviceId()),
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
        return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card", children: [_jsx("h1", { className: "formal-exam-player__title", children: "\u7406\u8BBA\u8003\u8BD5" }), _jsx("p", { className: "formal-exam-player__subtitle", children: "\u8BF7\u4F7F\u7528\u9009\u624B\u8D26\u53F7\u767B\u5F55" }), isLoopbackHost() ? (_jsx("p", { className: "formal-exam-player__error", children: "\u5F53\u524D\u5730\u5740\u662F localhost\uFF0C\u5E73\u677F\u8FDE\u70ED\u70B9\u540E\u6253\u4E0D\u5F00\u672C\u673A\u670D\u52A1\u3002\u8BF7\u6539\u7528\u7535\u8111\u70ED\u70B9\u7F51\u5173\u8BBF\u95EE\uFF0CWindows \u4E00\u822C\u4E3A http://192.168.137.1:8080/formal-exam" })) : null, _jsxs(Form, { layout: "vertical", size: "large", onFinish: (values) => loginMutation.mutate(values), children: [_jsx(Form.Item, { name: "account", label: "\u624B\u673A\u53F7", rules: [{ required: true, message: '请输入手机号' }], children: _jsx(Input, { autoComplete: "username", inputMode: "numeric", placeholder: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7" }) }), _jsx(Form.Item, { name: "password", label: "\u5BC6\u7801", rules: [{ required: true, message: '请输入密码' }], children: _jsx(Input.Password, { autoComplete: "current-password", placeholder: "\u8BF7\u8F93\u5165\u5BC6\u7801" }) }), loginError ? _jsx("p", { className: "formal-exam-player__error", children: loginError }) : null, _jsx(Button, { type: "primary", htmlType: "submit", size: "large", loading: loginMutation.isPending, autoInsertSpace: false, children: "\u767B\u5F55" })] })] }) }));
    }
    if (!sessionId) {
        return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card", children: [_jsx("h1", { className: "formal-exam-player__title", children: "\u7406\u8BBA\u8003\u8BD5" }), _jsx("p", { className: "formal-exam-player__error", children: "\u7F3A\u5C11\u6BD4\u8D5B\u573A\u6B21\uFF0C\u8BF7\u8054\u7CFB\u5DE5\u4F5C\u4EBA\u5458" })] }) }));
    }
    if (contextQuery.isLoading) {
        return (_jsx("div", { className: "formal-exam-player", children: _jsx("div", { className: "formal-exam-player__card", style: { alignItems: 'center' }, children: _jsx(Spin, { size: "large" }) }) }));
    }
    if (contextQuery.isError || !contextQuery.data) {
        const reconnect = isNetworkFailure(contextQuery.error) || contextQuery.fetchStatus === 'paused';
        return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card", children: [_jsx("h1", { className: "formal-exam-player__title", children: "\u7406\u8BBA\u8003\u8BD5" }), _jsx("p", { className: "formal-exam-player__error", children: reconnect
                            ? '暂时无法连接考试服务器，请保持应用开启并等待网络恢复'
                            : errorText(contextQuery.error, '无法获取本场比赛信息，请联系工作人员') })] }) }));
    }
    const context = contextQuery.data;
    const view = viewOf(context);
    const competition = context.competition;
    const candidate = context.candidateExam;
    if (view === 'confirm') {
        return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card", children: [_jsx("h1", { className: "formal-exam-player__title", children: competition.eventName || '正式比赛' }), _jsx("p", { className: "formal-exam-player__subtitle", children: examTypeLabel(context.examSession.examType) }), _jsxs("div", { className: "formal-exam-player__meta", children: [_jsxs("p", { children: ["\u59D3\u540D\uFF1A", candidate.candidateName || '-'] }), _jsxs("p", { children: ["\u9009\u624B\u7F16\u53F7\uFF1A", candidate.candidateNo || '-'] }), _jsxs("p", { children: ["\u5DE5\u79CD\uFF1A", competition.tradeName || '-'] }), _jsxs("p", { children: ["\u6280\u80FD\u7B49\u7EA7\uFF1A", competition.skillLevelName || '-'] }), _jsxs("p", { children: ["\u8003\u8BD5\u65F6\u957F\uFF1A", durationLabel(context.examSession.durationSeconds)] })] }), _jsx("p", { className: "formal-exam-player__hint", children: "\u8BF7\u786E\u8BA4\u4EE5\u4E0A\u4FE1\u606F\u65E0\u8BEF\u3002" }), enterMutation.isError ? (_jsx("p", { className: "formal-exam-player__error", children: errorText(enterMutation.error, '进入比赛失败') })) : null, _jsx("div", { className: "formal-exam-player__actions", children: _jsx(Button, { type: "primary", size: "large", autoInsertSpace: false, loading: enterMutation.isPending, onClick: () => enterMutation.mutate(), children: "\u786E\u8BA4\u8FDB\u5165\u6BD4\u8D5B" }) })] }) }));
    }
    if (view === 'anomaly') {
        return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card", children: [_jsx("h1", { className: "formal-exam-player__title", children: examTypeLabel(context.examSession.examType) }), _jsx("p", { className: "formal-exam-player__name", children: candidate.candidateName || '-' }), _jsx("p", { className: "formal-exam-player__status", children: "\u8FD0\u884C\u6570\u636E\u5F02\u5E38" }), _jsx("p", { className: "formal-exam-player__hint", children: context.runtimeIssue || '请联系现场工作人员处理。' })] }) }));
    }
    if (view === 'absent') {
        return _jsx(FormalExamAbsentScreen, { context: context });
    }
    if (view === 'submitted') {
        return _jsx(FormalExamTerminalScreen, { context: context });
    }
    if (view === 'running') {
        return (_jsx(FormalExamAnsweringScreen, { sessionId: sessionId, context: context, onSubmitted: (next) => {
                queryClient.setQueryData(['formal-exam-player-context', sessionId, token], next);
            } }));
    }
    return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card", children: [_jsx("h1", { className: "formal-exam-player__title", children: examTypeLabel(context.examSession.examType) }), _jsx("p", { className: "formal-exam-player__name", children: candidate.candidateName || '-' }), _jsxs("p", { className: "formal-exam-player__subtitle", children: ["\u9009\u624B\u7F16\u53F7 ", candidate.candidateNo || '-'] }), _jsxs("div", { className: "formal-exam-player__checks", children: [_jsx(Typography.Text, { children: "\u2713 \u8EAB\u4EFD\u786E\u8BA4\u5B8C\u6210" }), _jsx(Typography.Text, { children: "\u2713 \u5DF2\u8FDB\u5165\u6BD4\u8D5B" })] }), _jsx("p", { className: "formal-exam-player__status", children: "\u5F53\u524D\u72B6\u6001\uFF1A\u7B49\u5F85\u5F00\u8003" }), _jsx("p", { className: "formal-exam-player__hint", children: "\u8BF7\u7B49\u5F85\u5DE5\u4F5C\u4EBA\u5458\u7EDF\u4E00\u5F00\u59CB\u6BD4\u8D5B" })] }) }));
}

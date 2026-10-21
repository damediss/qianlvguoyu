import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Drawer, Empty, Input, Modal, Select, Space, Spin, Table, Tag, Tooltip, Typography, message, } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import dayjs from 'dayjs';
import { getCompetitionFormalCandidateExam, getCompetitionFormalCandidateResult, getCompetitionFormalExamSession, getCompetitionFormalExamSessionPreflight, pageCompetitionFormalCandidateExams, startCompetitionFormalExamSession, forceSubmitCompetitionFormalCandidateExam, forceOfflineCompetitionFormalCandidateExam, forceOfflineCompetitionFormalCandidateExams, scoreCompetitionFormalExamSession, fetchFormalExamConsoleRealtimeEvents, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import { FORMAL_EXAM_CONNECTION_LABELS, FORMAL_EXAM_CONSOLE_DISCONNECT_BANNER, applyFormalExamConsoleEnvelope, formalExamConsoleCandidatesQueryKey, formalExamConsolePollIntervalMs, formalExamConsoleSessionQueryKey, resolveFormalExamConnectionState, startFormalExamConsoleRealtime, } from './FormalExamConsoleRealtime';
import './CompetitionFormalExamSessionPanel.css';
const sessionStatusMeta = {
    preparing: { label: '准备中', color: 'processing' },
    ready: { label: '准备完成', color: 'success' },
    running: { label: '比赛中', color: 'blue' },
    finished: { label: '已结束', color: 'default' },
    cancelled: { label: '已取消', color: 'error' },
};
const candidateStatusMeta = {
    waiting_enter: { label: '待进入', color: 'gold' },
    ready: { label: '已候考', color: 'blue' },
    running: { label: '考试中', color: 'processing' },
    submitted: { label: '已交卷', color: 'success' },
    auto_submitted: { label: '自动交卷', color: 'orange' },
    forced_submitted: { label: '强制交卷', color: 'red' },
    absent: { label: '缺考', color: 'default' },
};
const scoringStatusMeta = {
    not_started: { label: '待评分', color: 'gold' },
    scoring: { label: '评分中', color: 'processing' },
    completed: { label: '评分完成', color: 'success' },
    failed: { label: '评分失败', color: 'error' },
};
const questionTypeLabel = {
    single: '单选',
    multi: '多选',
    judge: '判断',
    fill: '填空',
};
function formatScore(value) {
    if (value == null || value === '')
        return '--';
    const number = Number(value);
    if (Number.isNaN(number))
        return String(value);
    return number.toFixed(2);
}
function candidateScoreText(row) {
    if (row.status === 'absent' || row.resultStatus === 'absent') {
        return '缺考';
    }
    if (row.resultStatus === 'scored' && row.totalScore != null && row.fullScore != null) {
        return `${formatScore(row.totalScore)} / ${formatScore(row.fullScore)}`;
    }
    return '--';
}
const candidateStatusOptions = Object.entries(candidateStatusMeta).map(([value, meta]) => ({
    value,
    label: meta.label,
}));
function durationLabel(seconds) {
    if (!seconds || seconds <= 0)
        return '-';
    return `${Math.round(seconds / 60)} 分钟`;
}
function formatDateTime(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--';
}
function formatClockTime(value) {
    if (!value)
        return '--';
    const normalized = value.includes('T') ? value : value.replace(' ', 'T');
    const parsed = dayjs(normalized);
    return parsed.isValid() ? parsed.format('HH:mm:ss') : '--';
}
function paperLabel(code, name) {
    const codeText = code ? `${code}卷` : '-';
    return name ? `${codeText} · ${name}` : codeText;
}
function sessionBanner(status) {
    if (status === 'preparing') {
        return { type: 'warning', message: '正式比赛数据仍在准备中' };
    }
    if (status === 'cancelled') {
        return { type: 'error', message: '本场正式比赛已取消' };
    }
    if (status === 'finished') {
        return { type: 'info', message: '本场正式比赛已结束' };
    }
    return null;
}
export function CompetitionFormalExamSessionPanel({ open, loading, session, error, onClose, embedded = false, }) {
    const [keyword, setKeyword] = useState('');
    const [paperSnapshotId, setPaperSnapshotId] = useState();
    const [status, setStatus] = useState();
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [detailId, setDetailId] = useState(null);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [forceTarget, setForceTarget] = useState(null);
    const [forceReason, setForceReason] = useState('');
    const [evictTarget, setEvictTarget] = useState(null);
    const [selectedCandidateIds, setSelectedCandidateIds] = useState([]);
    const [batchEvictIds, setBatchEvictIds] = useState([]);
    const [realtimeState, setRealtimeState] = useState('idle');
    const queryClient = useQueryClient();
    const realtimeRef = useRef(null);
    const applyStateRef = useRef({ seen: new Set(), lastSeq: null });
    const pageIdsRef = useRef([]);
    const scopeRef = useRef({
        sessionId: session?.id,
        keyword,
        paperSnapshotId,
        status,
        page,
        pageSize,
    });
    const canForceSubmit = useAuthStore((state) => state.permissionCodes.includes('competition:exam-session:force-submit'));
    const canForceOffline = useAuthStore((state) => state.permissionCodes.includes('competition:exam-session:force-offline'));
    const canScore = useAuthStore((state) => state.permissionCodes.includes('competition:exam-session:score'));
    const pollMs = useMemo(() => formalExamConsolePollIntervalMs(realtimeState === 'connected'), [realtimeState]);
    const liveSessionQuery = useQuery({
        queryKey: formalExamConsoleSessionQueryKey(session?.id),
        queryFn: () => getCompetitionFormalExamSession(session.id),
        enabled: open && Boolean(session?.id),
        initialData: session ?? undefined,
        staleTime: 4000,
        refetchInterval: pollMs,
        refetchIntervalInBackground: false,
    });
    const currentSession = liveSessionQuery.data ?? session;
    const preflightQuery = useQuery({
        queryKey: ['competition-formal-exam-session-preflight', session?.id],
        queryFn: () => getCompetitionFormalExamSessionPreflight(session.id),
        enabled: open && Boolean(session?.id) && (liveSessionQuery.data?.status ?? session?.status) === 'ready',
    });
    const candidatesQuery = useQuery({
        queryKey: formalExamConsoleCandidatesQueryKey(session?.id, keyword, paperSnapshotId, status, page, pageSize),
        queryFn: () => pageCompetitionFormalCandidateExams(session.id, {
            page,
            pageSize,
            ...(keyword ? { keyword } : {}),
            ...(paperSnapshotId ? { paperSnapshotId } : {}),
            ...(status ? { status } : {}),
        }),
        enabled: open && Boolean(session?.id),
        refetchInterval: pollMs,
        refetchIntervalInBackground: false,
    });
    const detailQuery = useQuery({
        queryKey: ['competition-formal-candidate-exam', detailId],
        queryFn: () => getCompetitionFormalCandidateExam(detailId),
        enabled: open && Boolean(detailId),
    });
    const resultQuery = useQuery({
        queryKey: ['competition-formal-candidate-result', detailId],
        queryFn: () => getCompetitionFormalCandidateResult(detailId),
        enabled: open && Boolean(detailId)
            && currentSession?.status === 'finished'
            && (currentSession.scoringStatus === 'completed' || currentSession.scoringStatus === 'failed'),
        retry: false,
    });
    const startMutation = useMutation({
        mutationFn: () => startCompetitionFormalExamSession(String(currentSession.id), {
            confirmWaitingCandidates: (currentSession?.waitingEnterCount ?? 0) > 0,
        }),
        onSuccess: async (data) => {
            setConfirmOpen(false);
            queryClient.setQueryData(['competition-formal-exam-session', session?.id], data);
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session', session?.id],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session-candidates', session?.id],
            });
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '开始比赛失败');
        },
    });
    const forceSubmitMutation = useMutation({
        mutationFn: () => forceSubmitCompetitionFormalCandidateExam(String(currentSession.id), String(forceTarget.id), { reason: forceReason.trim() }),
        onSuccess: async () => {
            message.success('已强制交卷');
            setForceTarget(null);
            setForceReason('');
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session', session?.id],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session-candidates', session?.id],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-candidate-exam'],
            });
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '强制交卷失败');
        },
    });
    const forceOfflineMutation = useMutation({
        mutationFn: () => forceOfflineCompetitionFormalCandidateExam(String(currentSession.id), String(evictTarget.id)),
        onSuccess: async () => {
            message.success('已清退该选手考试账号，平板将回到登录页');
            setEvictTarget(null);
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session', session?.id],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session-candidates', session?.id],
            });
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '清退失败');
        },
    });
    const batchForceOfflineMutation = useMutation({
        mutationFn: (candidateExamIds) => forceOfflineCompetitionFormalCandidateExams(String(currentSession.id), candidateExamIds),
        onSuccess: async (result, candidateExamIds) => {
            const kickedCount = result?.kickedCount ?? candidateExamIds.length;
            if (kickedCount > 0) {
                message.success(`已清退 ${kickedCount} 名选手考试账号，平板将回到登录页`);
            }
            else {
                message.info(`所选 ${candidateExamIds.length} 名选手当前均已离线，无需重复清退`);
            }
            setBatchEvictIds([]);
            setSelectedCandidateIds([]);
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session', session?.id],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session-candidates', session?.id],
            });
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '批量清退失败');
        },
    });
    const scoreMutation = useMutation({
        mutationFn: () => scoreCompetitionFormalExamSession(String(currentSession.id)),
        onSuccess: async () => {
            message.success('评分任务已执行');
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session', session?.id],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-exam-session-candidates', session?.id],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-formal-candidate-result'],
            });
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '评分失败');
        },
    });
    pageIdsRef.current = (candidatesQuery.data?.items || []).map((row) => String(row.id));
    scopeRef.current = {
        sessionId: session?.id,
        keyword,
        paperSnapshotId,
        status,
        page,
        pageSize,
    };
    useEffect(() => {
        if (!open || !session?.id) {
            realtimeRef.current?.stop();
            realtimeRef.current = null;
            setRealtimeState('idle');
            return undefined;
        }
        const sessionId = session.id;
        applyStateRef.current = { seen: new Set(), lastSeq: null };
        const handle = startFormalExamConsoleRealtime({
            examSessionId: String(sessionId),
            isActive: () => true,
            getPageCandidateIds: () => pageIdsRef.current,
            fetchCatchUp: (afterSeq) => fetchFormalExamConsoleRealtimeEvents(sessionId, { afterSeq, limit: 200 }),
            onConnected: () => setRealtimeState('connected'),
            onDisconnected: () => setRealtimeState('disconnected'),
            onPageRefresh: () => {
                applyStateRef.current.lastSeq = null;
                void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
                void queryClient.invalidateQueries({
                    queryKey: ['competition-formal-exam-session-candidates', sessionId],
                });
            },
            onOutOfOrder: () => {
                void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
                void queryClient.invalidateQueries({
                    queryKey: ['competition-formal-exam-session-candidates', sessionId],
                });
            },
            onSequenceCollision: () => {
                void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
                void queryClient.invalidateQueries({
                    queryKey: ['competition-formal-exam-session-candidates', sessionId],
                });
            },
            onEnvelope: (envelope) => {
                const scope = scopeRef.current;
                const sessionKey = formalExamConsoleSessionQueryKey(scope.sessionId);
                const candidatesKey = formalExamConsoleCandidatesQueryKey(scope.sessionId, scope.keyword, scope.paperSnapshotId, scope.status, scope.page, scope.pageSize);
                const result = applyFormalExamConsoleEnvelope({
                    envelope,
                    seen: applyStateRef.current.seen,
                    lastSeq: applyStateRef.current.lastSeq,
                    pageCandidateIds: pageIdsRef.current,
                    session: queryClient.getQueryData(sessionKey),
                    page: queryClient.getQueryData(candidatesKey),
                });
                applyStateRef.current.lastSeq = result.lastSeq;
                if (result.invalidate) {
                    void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-session', sessionId] });
                    void queryClient.invalidateQueries({
                        queryKey: ['competition-formal-exam-session-candidates', sessionId],
                    });
                    return;
                }
                if (result.session) {
                    queryClient.setQueryData(sessionKey, result.session);
                }
                if (result.page) {
                    queryClient.setQueryData(candidatesKey, result.page);
                }
            },
        });
        realtimeRef.current = handle;
        return () => {
            handle.stop();
            realtimeRef.current = null;
        };
    }, [open, queryClient, session?.id]);
    useEffect(() => {
        realtimeRef.current?.subscribePage(pageIdsRef.current);
    }, [candidatesQuery.data]);
    const sessionStatus = sessionStatusMeta[currentSession?.status || ''] || {
        label: currentSession?.status || '-',
        color: 'default',
    };
    const banner = sessionBanner(currentSession?.status);
    const paperOptions = useMemo(() => (currentSession?.papers || []).map((paper) => ({
        value: String(paper.id),
        label: paperLabel(paper.paperCode, paper.paperName),
    })), [currentSession?.papers]);
    const failedChecks = (preflightQuery.data?.checks || []).filter((item) => !item.passed);
    const resetPanelState = () => {
        setKeyword('');
        setPaperSnapshotId(undefined);
        setStatus(undefined);
        setPage(1);
        setDetailId(null);
        setConfirmOpen(false);
        setForceTarget(null);
        setForceReason('');
        setSelectedCandidateIds([]);
        setBatchEvictIds([]);
    };
    useEffect(() => {
        setSelectedCandidateIds([]);
        setBatchEvictIds([]);
    }, [session?.id]);
    const content = (_jsxs(_Fragment, { children: [loading ? _jsx(Spin, {}) : null, error ? _jsx(Alert, { type: "error", showIcon: true, message: error }) : null, !loading && !error && !session ? (_jsx(Empty, { description: "\u5C1A\u672A\u751F\u6210\u6B63\u5F0F\u6BD4\u8D5B\u8FD0\u884C\u5B9E\u4F8B" })) : null, session && currentSession && (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [banner ? _jsx(Alert, { type: banner.type, showIcon: true, message: banner.message }) : null, realtimeState === 'disconnected' ? (_jsx(Alert, { type: "warning", showIcon: true, message: FORMAL_EXAM_CONSOLE_DISCONNECT_BANNER })) : null, currentSession.status === 'running' && (currentSession.finishBlockers?.length ?? 0) > 0 ? (_jsx(Alert, { type: "error", showIcon: true, message: "\u573A\u6B21\u7ED3\u675F\u53D7\u963B", description: _jsx("ul", { className: "competition-formal-exam-session-panel__fail-list", children: currentSession.finishBlockers?.map((item) => (_jsx("li", { children: item.message }, item.code))) }) })) : null, currentSession.status === 'running'
                        && currentSession.finishEligible
                        && !(currentSession.finishBlockers?.length) ? (_jsx(Alert, { type: "info", showIcon: true, message: "\u8003\u8BD5\u5DF2\u622A\u6B62\uFF0C\u7CFB\u7EDF\u6B63\u5728\u5F52\u6863\u5E76\u7ED3\u675F\u672C\u573A\u6BD4\u8D5B" })) : null, currentSession.status === 'ready' && preflightQuery.data && !preflightQuery.data.ready ? (_jsx(Alert, { type: "error", showIcon: true, message: "\u5F00\u8003\u524D\u68C0\u67E5\u672A\u901A\u8FC7", description: _jsx("ul", { className: "competition-formal-exam-session-panel__fail-list", children: failedChecks.map((item) => (_jsx("li", { children: item.message }, item.code))) }) })) : null, _jsxs("div", { className: "competition-formal-exam-session-panel__hero", children: [_jsx("div", { children: currentSession.status === 'running' || currentSession.status === 'finished' ? (_jsxs("div", { className: "competition-formal-exam-session-panel__times", children: [_jsxs("span", { children: ["\u5B9E\u9645\u5F00\u59CB\uFF1A", formatClockTime(currentSession.actualStartTime)] }), currentSession.status === 'finished' ? (_jsxs("span", { children: ["\u5B9E\u9645\u7ED3\u675F\uFF1A", formatClockTime(currentSession.actualEndTime)] })) : (_jsxs("span", { children: ["\u9884\u8BA1\u622A\u6B62\uFF1A", formatClockTime(currentSession.deadlineTime)] }))] })) : null }), _jsxs("div", { className: "competition-formal-exam-session-panel__hero-actions", children: [currentSession.status === 'finished'
                                        && canScore
                                        && (currentSession.scoringStatus === 'failed' || currentSession.scoringStatus === 'not_started') ? (_jsx(Button, { autoInsertSpace: false, loading: scoreMutation.isPending, onClick: () => scoreMutation.mutate(), children: "\u91CD\u65B0\u6267\u884C\u8BC4\u5206" })) : null, currentSession.status === 'ready' && preflightQuery.data?.ready ? (_jsx(Tooltip, { title: (currentSession.readyCount ?? 0) === 0 ? '当前没有已进入候考的选手' : '', children: _jsx("span", { children: _jsx(Button, { type: "primary", autoInsertSpace: false, disabled: (currentSession.readyCount ?? 0) === 0 || startMutation.isPending, loading: startMutation.isPending, onClick: () => setConfirmOpen(true), children: "\u5F00\u59CB\u6BD4\u8D5B" }) }) })) : null] })] }), _jsxs(Descriptions, { size: "small", column: 3, bordered: true, children: [_jsx(Descriptions.Item, { label: "\u6BD4\u8D5B\u72B6\u6001", children: _jsx(Tag, { color: sessionStatus.color, children: sessionStatus.label }) }), currentSession.status === 'finished' ? (_jsx(Descriptions.Item, { label: "\u8BC4\u5206\u72B6\u6001", children: _jsx(Tag, { color: (scoringStatusMeta[currentSession.scoringStatus || 'not_started'] || scoringStatusMeta.not_started).color, children: (scoringStatusMeta[currentSession.scoringStatus || 'not_started'] || scoringStatusMeta.not_started).label }) })) : null, _jsx(Descriptions.Item, { label: "\u8003\u8BD5\u65F6\u957F", children: durationLabel(currentSession.durationSeconds) }), _jsx(Descriptions.Item, { label: "\u8BA1\u5212\u5F00\u59CB\u65F6\u95F4", children: formatDateTime(currentSession.plannedStartTime) }), _jsx(Descriptions.Item, { label: "\u5B9E\u9645\u5F00\u59CB\u65F6\u95F4", children: formatDateTime(currentSession.actualStartTime) }), _jsx(Descriptions.Item, { label: "\u5B9E\u9645\u7ED3\u675F\u65F6\u95F4", children: formatDateTime(currentSession.actualEndTime) }), _jsx(Descriptions.Item, { label: "\u9009\u624B\u603B\u4EBA\u6570", children: currentSession.candidateTotal ?? 0 }), _jsx(Descriptions.Item, { label: "\u8BD5\u5377\u5957\u6570", children: currentSession.paperCount ?? 0 })] }), currentSession.status === 'finished' && currentSession.scoringStatus === 'failed' && currentSession.scoringError ? (_jsx(Alert, { type: "error", showIcon: true, message: currentSession.scoringError })) : null, currentSession.status === 'ready' ? (_jsx(Card, { size: "small", title: "\u5F00\u8003\u524D\u68C0\u67E5", loading: preflightQuery.isFetching, children: preflightQuery.isError ? (_jsx(Alert, { type: "error", showIcon: true, message: "\u5F00\u8003\u524D\u68C0\u67E5\u52A0\u8F7D\u5931\u8D25" })) : (_jsxs(Space, { direction: "vertical", size: 8, style: { width: '100%' }, children: [(preflightQuery.data?.checks || []).map((item) => (_jsxs("div", { className: "competition-formal-exam-session-panel__check", children: [item.passed
                                            ? _jsx(CheckCircleOutlined, { className: "competition-formal-exam-session-panel__check-ok" })
                                            : _jsx(CloseCircleOutlined, { className: "competition-formal-exam-session-panel__check-fail" }), _jsx(Typography.Text, { children: item.message })] }, item.code))), preflightQuery.data?.ready ? (_jsx(Typography.Text, { type: "success", children: "\u5F53\u524D\u72B6\u6001\uFF1A\u53EF\u4EE5\u8FDB\u5165\u9009\u624B\u5019\u8003\u9636\u6BB5" })) : null] })) })) : null, _jsxs(Card, { size: "small", title: "\u9009\u624B", children: [_jsxs("div", { className: "competition-formal-exam-session-panel__toolbar", children: [_jsx(Input.Search, { allowClear: true, "aria-label": "\u641C\u7D22\u7F16\u53F7\u6216\u59D3\u540D", placeholder: "\u641C\u7D22\u7F16\u53F7\u6216\u59D3\u540D", style: { width: 240 }, onSearch: (value) => { setKeyword(value.trim()); setPage(1); } }), _jsx(Select, { allowClear: true, "aria-label": "\u8BD5\u5377\u7B5B\u9009", placeholder: "\u8BD5\u5377\u7B5B\u9009", style: { width: 220 }, value: paperSnapshotId, options: paperOptions, virtual: false, onChange: (value) => { setPaperSnapshotId(value); setPage(1); } }), _jsx(Select, { allowClear: true, "aria-label": "\u8003\u8BD5\u72B6\u6001", placeholder: "\u8003\u8BD5\u72B6\u6001", style: { width: 160 }, value: status, options: candidateStatusOptions, virtual: false, onChange: (value) => { setStatus(value); setPage(1); } }), canForceOffline ? (_jsxs(Button, { danger: true, "aria-label": "\u6279\u91CF\u6E05\u9000", disabled: selectedCandidateIds.length === 0, onClick: () => setBatchEvictIds([...selectedCandidateIds]), children: ["\u6279\u91CF\u6E05\u9000", selectedCandidateIds.length > 0 ? `（${selectedCandidateIds.length}）` : ''] })) : null] }), _jsx(Table, { rowKey: (row) => String(row.id), size: "small", rowSelection: canForceOffline ? {
                                    preserveSelectedRowKeys: true,
                                    selectedRowKeys: selectedCandidateIds,
                                    onChange: (keys) => {
                                        const nextIds = keys.map(String);
                                        if (nextIds.length > 100) {
                                            message.warning('一次最多批量清退100名选手');
                                            setSelectedCandidateIds(nextIds.slice(0, 100));
                                            return;
                                        }
                                        setSelectedCandidateIds(nextIds);
                                    },
                                } : undefined, loading: candidatesQuery.isFetching, dataSource: candidatesQuery.data?.items || [], locale: { emptyText: '尚未生成选手考试实例' }, pagination: {
                                    current: page,
                                    pageSize,
                                    total: candidatesQuery.data?.total || 0,
                                    showSizeChanger: false,
                                    onChange: (nextPage, nextSize) => {
                                        setPage(nextPage);
                                        setPageSize(nextSize);
                                    },
                                }, columns: [
                                    { title: '编号', dataIndex: 'candidateNo', width: 110, render: (value) => value || '-' },
                                    { title: '姓名', dataIndex: 'candidateName', width: 110, render: (value) => value || '-' },
                                    {
                                        title: '试卷',
                                        width: 160,
                                        render: (_, row) => paperLabel(row.paperCode, row.paperName),
                                    },
                                    {
                                        title: '状态',
                                        dataIndex: 'status',
                                        width: 180,
                                        render: (value, row) => {
                                            const meta = candidateStatusMeta[value] || { label: value, color: 'default' };
                                            const state = row.connectionState || resolveFormalExamConnectionState({
                                                candidateStatus: row.status,
                                                accessSessionStatus: row.accessSessionStatus,
                                                lastSeenAt: row.lastSeenAt,
                                            });
                                            const connection = FORMAL_EXAM_CONNECTION_LABELS[state] || { label: state, color: 'default' };
                                            return (_jsxs(Space, { size: 4, wrap: true, children: [_jsx(Tag, { color: meta.color, children: meta.label }), _jsx(Tag, { color: connection.color, children: connection.label })] }));
                                        },
                                    },
                                    { title: '进入时间', dataIndex: 'enterTime', width: 160, render: formatDateTime },
                                    { title: '开始时间', dataIndex: 'startTime', width: 160, render: formatDateTime },
                                    { title: '交卷时间', dataIndex: 'submitTime', width: 160, render: formatDateTime },
                                    {
                                        title: '缺考确认时间',
                                        dataIndex: 'absentTime',
                                        width: 160,
                                        render: formatDateTime,
                                    },
                                    {
                                        title: '答题进度',
                                        width: 110,
                                        render: (_, row) => `${row.answeredCount ?? 0} / ${row.totalQuestionCount ?? 0}`,
                                    },
                                    {
                                        title: '成绩',
                                        width: 130,
                                        render: (_, row) => (currentSession.status === 'finished' ? candidateScoreText(row) : '--'),
                                    },
                                    {
                                        title: '操作',
                                        width: 220,
                                        render: (_, row) => (_jsxs(Space, { size: 0, children: [_jsx(Button, { type: "link", size: "small", onClick: () => setDetailId(row.id), children: "\u67E5\u770B" }), canForceOffline ? (_jsx(Button, { type: "link", size: "small", onClick: () => setEvictTarget(row), children: "\u6E05\u9000" })) : null, canForceSubmit && currentSession.status === 'running' && row.status === 'running' ? (_jsx(Button, { type: "link", size: "small", danger: true, onClick: () => {
                                                        setForceReason('');
                                                        setForceTarget(row);
                                                    }, children: "\u5F3A\u5236\u4EA4\u5377" })) : null] })),
                                    },
                                ] })] })] })), _jsx(Modal, { title: "\u5F00\u59CB\u6BD4\u8D5B", open: confirmOpen, okText: "\u786E\u8BA4\u5F00\u59CB", cancelText: "\u53D6\u6D88", confirmLoading: startMutation.isPending, onCancel: () => setConfirmOpen(false), onOk: () => startMutation.mutate(), destroyOnHidden: true, transitionName: "", maskTransitionName: "", children: (currentSession?.waitingEnterCount ?? 0) > 0 ? (_jsxs(Space, { direction: "vertical", size: 8, children: [_jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u5F53\u524D\u4ECD\u6709 ", currentSession?.waitingEnterCount, " \u540D\u9009\u624B\u672A\u8FDB\u5165\u5019\u8003\u3002"] }), _jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u5DF2\u5019\u8003\uFF1A", currentSession?.readyCount ?? 0, " \u4EBA\uFF1B\u672A\u8FDB\u5165\uFF1A", currentSession?.waitingEnterCount ?? 0, " \u4EBA\u3002"] }), _jsx(Typography.Paragraph, { style: { marginBottom: 0 }, children: "\u5F00\u59CB\u6BD4\u8D5B\u540E\uFF0C\u8FD9\u4E9B\u9009\u624B\u5C06\u65E0\u6CD5\u901A\u8FC7\u666E\u901A\u5019\u8003\u5165\u53E3\u81EA\u884C\u8FDB\u5165\u672C\u573A\u8003\u8BD5\u3002" }), _jsx(Typography.Paragraph, { style: { marginBottom: 0 }, children: "\u662F\u5426\u786E\u8BA4\u5F00\u59CB\u6BD4\u8D5B\uFF1F" })] })) : (_jsxs(Space, { direction: "vertical", size: 8, children: [_jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u672C\u573A\u5171\u6709 ", currentSession?.readyCount ?? 0, " \u540D\u9009\u624B\u5DF2\u8FDB\u5165\u5019\u8003\u3002"] }), _jsx(Typography.Paragraph, { style: { marginBottom: 0 }, children: "\u786E\u8BA4\u5F00\u59CB\u7406\u8BBA\u8003\u8BD5\uFF1F" }), _jsx(Typography.Paragraph, { style: { marginBottom: 0 }, children: "\u5F00\u59CB\u540E\u5C06\u4EE5\u670D\u52A1\u5668\u65F6\u95F4\u7EDF\u4E00\u8BA1\u65F6\u3002" })] })) }), _jsx(Modal, { title: "\u5F3A\u5236\u4EA4\u5377", open: Boolean(forceTarget), okText: "\u786E\u8BA4\u5F3A\u5236\u4EA4\u5377", cancelText: "\u53D6\u6D88", okButtonProps: { disabled: forceReason.trim().length < 1 || forceReason.trim().length > 500 }, confirmLoading: forceSubmitMutation.isPending, onCancel: () => {
                    setForceTarget(null);
                    setForceReason('');
                }, onOk: () => {
                    if (forceReason.trim().length < 1 || forceReason.trim().length > 500)
                        return;
                    forceSubmitMutation.mutate();
                }, destroyOnHidden: true, transitionName: "", maskTransitionName: "", children: _jsxs(Space, { direction: "vertical", size: 8, style: { width: '100%' }, children: [_jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u59D3\u540D\uFF1A", forceTarget?.candidateName || '-'] }), _jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u9009\u624B\u7F16\u53F7\uFF1A", forceTarget?.candidateNo || '-'] }), _jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u5F53\u524D\u7B54\u9898\u8FDB\u5EA6\uFF1A", forceTarget?.answeredCount ?? 0, " / ", forceTarget?.totalQuestionCount ?? 0] }), _jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u8003\u8BD5\u72B6\u6001\uFF1A", candidateStatusMeta[forceTarget?.status || '']?.label || forceTarget?.status || '-'] }), _jsx(Alert, { type: "warning", showIcon: true, message: "\u5F3A\u5236\u4EA4\u5377\u5C06\u7ACB\u5373\u9501\u5B9A\u670D\u52A1\u5668\u5DF2\u7ECF\u63A5\u6536\u7684\u7B54\u6848\u3002\u5982\u679C\u8BE5\u9009\u624B\u8BBE\u5907\u5F53\u524D\u5B58\u5728\u5C1A\u672A\u540C\u6B65\u7684\u672C\u5730\u7B54\u6848\uFF0C\u8FD9\u4E9B\u7B54\u6848\u4E0D\u4F1A\u81EA\u52A8\u8BA1\u5165\u6B63\u5F0F\u8BD5\u5377\u3002\u786E\u8BA4\u7EE7\u7EED\u5417\uFF1F" }), _jsx(Typography.Paragraph, { style: { marginBottom: 0 }, children: "\u5F3A\u5236\u4EA4\u5377\u539F\u56E0\uFF08\u5FC5\u586B\uFF0C1\uFF5E500 \u5B57\uFF09" }), _jsx(Input.TextArea, { "aria-label": "\u5F3A\u5236\u4EA4\u5377\u539F\u56E0", value: forceReason, maxLength: 500, rows: 4, placeholder: "\u4F8B\u5982\uFF1A\u8BBE\u5907\u8FDD\u89C4\u64CD\u4F5C / \u9009\u624B\u7533\u8BF7\u63D0\u524D\u7ED3\u675F / \u73B0\u573A\u5F02\u5E38\u5904\u7406", onChange: (event) => setForceReason(event.target.value) })] }) }), _jsx(Modal, { title: "\u6E05\u9000\u8003\u8BD5\u8D26\u53F7", open: Boolean(evictTarget), okText: "\u786E\u8BA4\u6E05\u9000", cancelText: "\u53D6\u6D88", confirmLoading: forceOfflineMutation.isPending, onCancel: () => setEvictTarget(null), onOk: () => forceOfflineMutation.mutate(), destroyOnHidden: true, transitionName: "", maskTransitionName: "", children: _jsxs(Space, { direction: "vertical", size: 8, style: { width: '100%' }, children: [_jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u59D3\u540D\uFF1A", evictTarget?.candidateName || '-'] }), _jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u9009\u624B\u7F16\u53F7\uFF1A", evictTarget?.candidateNo || '-'] }), _jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u8003\u8BD5\u72B6\u6001\uFF1A", candidateStatusMeta[evictTarget?.status || '']?.label || evictTarget?.status || '-'] }), _jsx(Alert, { type: "info", showIcon: true, message: "\u6E05\u9000\u53EA\u4F1A\u5F3A\u5236\u8BE5\u9009\u624B\u8003\u8BD5\u8D26\u53F7\u4E0B\u7EBF\uFF0C\u5E73\u677F\u5C06\u56DE\u5230\u767B\u5F55\u9875\u3002\u4E0D\u4F1A\u4EA4\u5377\uFF0C\u4E5F\u4E0D\u4F1A\u4F5C\u5E9F\u5DF2\u7B54\u9898\u76EE\u3002\u4E0B\u4E00\u573A\u9009\u624B\u53EF\u5728\u540C\u4E00\u5E73\u677F\u76F4\u63A5\u767B\u5F55\uFF0C\u4E0D\u5FC5\u66F4\u6362\u8003\u8BD5\u5730\u5740\u3002" })] }) }), _jsx(Modal, { title: "\u6279\u91CF\u6E05\u9000\u8003\u8BD5\u8D26\u53F7", open: batchEvictIds.length > 0, okText: "\u786E\u8BA4\u6279\u91CF\u6E05\u9000", cancelText: "\u53D6\u6D88", confirmLoading: batchForceOfflineMutation.isPending, onCancel: () => setBatchEvictIds([]), onOk: () => batchForceOfflineMutation.mutate(batchEvictIds), destroyOnHidden: true, transitionName: "", maskTransitionName: "", children: _jsxs(Space, { direction: "vertical", size: 8, style: { width: '100%' }, children: [_jsxs(Typography.Paragraph, { style: { marginBottom: 0 }, children: ["\u5DF2\u9009\u62E9 ", batchEvictIds.length, " \u540D\u9009\u624B\u3002"] }), _jsx(Alert, { type: "warning", showIcon: true, message: "\u786E\u8BA4\u6279\u91CF\u6E05\u9000\u8FD9\u4E9B\u9009\u624B\u7684\u8003\u8BD5\u8D26\u53F7\uFF1F", description: "\u7CFB\u7EDF\u53EA\u4F1A\u5F3A\u5236\u8003\u8BD5\u8D26\u53F7\u4E0B\u7EBF\uFF0C\u5E73\u677F\u5C06\u56DE\u5230\u767B\u5F55\u9875\uFF1B\u4E0D\u4F1A\u4EA4\u5377\uFF0C\u4E5F\u4E0D\u4F1A\u4F5C\u5E9F\u5DF2\u7B54\u9898\u76EE\u3002\u5DF2\u63D0\u524D\u4E0B\u7EBF\u7684\u9009\u624B\u4F1A\u5B89\u5168\u8DF3\u8FC7\u3002" })] }) }), _jsxs(Drawer, { title: "\u9009\u624B\u6B63\u5F0F\u8003\u8BD5", open: Boolean(detailId), width: 560, onClose: () => setDetailId(null), destroyOnHidden: true, getContainer: false, children: [detailQuery.isFetching ? _jsx(Spin, {}) : null, detailQuery.isError ? _jsx(Alert, { type: "error", showIcon: true, message: "\u9009\u624B\u8003\u8BD5\u5B9E\u4F8B\u52A0\u8F7D\u5931\u8D25" }) : null, detailQuery.data ? (_jsxs(Descriptions, { size: "small", column: 1, bordered: true, children: [_jsx(Descriptions.Item, { label: "\u59D3\u540D", children: detailQuery.data.candidateName || '-' }), _jsx(Descriptions.Item, { label: "\u7F16\u53F7", children: detailQuery.data.candidateNo || '-' }), _jsx(Descriptions.Item, { label: "contestantProfileId", children: _jsx(Typography.Text, { copyable: { text: String(detailQuery.data.contestantProfileId) }, children: String(detailQuery.data.contestantProfileId) }) }), _jsx(Descriptions.Item, { label: "CandidateExam ID", children: _jsx(Typography.Text, { copyable: { text: String(detailQuery.data.id) }, children: String(detailQuery.data.id) }) }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: candidateStatusMeta[detailQuery.data.status]?.label || detailQuery.data.status }), _jsx(Descriptions.Item, { label: "\u6B63\u5F0F\u8BD5\u5377\u540D\u79F0", children: detailQuery.data.paperName || '-' }), _jsx(Descriptions.Item, { label: "\u6B63\u5F0F\u8BD5\u5377\u4EE3\u7801", children: detailQuery.data.paperCode || '-' }), _jsx(Descriptions.Item, { label: "paperSnapshotId", children: _jsx(Typography.Text, { copyable: { text: String(detailQuery.data.paperSnapshotId) }, children: String(detailQuery.data.paperSnapshotId) }) }), _jsx(Descriptions.Item, { label: "sourcePaperId", children: _jsx(Typography.Text, { copyable: { text: String(detailQuery.data.sourcePaperId) }, children: String(detailQuery.data.sourcePaperId) }) }), _jsx(Descriptions.Item, { label: "\u9898\u76EE\u6570\u91CF", children: detailQuery.data.totalQuestionCount ?? 0 }), _jsx(Descriptions.Item, { label: "\u8FDB\u5165\u65F6\u95F4", children: formatDateTime(detailQuery.data.enterTime) }), _jsx(Descriptions.Item, { label: "\u5F00\u59CB\u65F6\u95F4", children: formatDateTime(detailQuery.data.startTime) }), _jsx(Descriptions.Item, { label: "\u4EA4\u5377\u65F6\u95F4", children: formatDateTime(detailQuery.data.submitTime) }), detailQuery.data.status === 'absent' ? (_jsxs(_Fragment, { children: [_jsx(Descriptions.Item, { label: "\u7F3A\u8003\u786E\u8BA4\u65F6\u95F4", children: formatDateTime(detailQuery.data.absentTime) }), _jsx(Descriptions.Item, { label: "\u7F3A\u8003\u539F\u56E0", children: detailQuery.data.absentReason || '-' })] })) : null, _jsx(Descriptions.Item, { label: "\u4EA4\u5377\u7C7B\u578B", children: detailQuery.data.submitType
                                    ? (candidateStatusMeta[detailQuery.data.submitType]?.label || detailQuery.data.submitType)
                                    : '-' }), detailQuery.data.status === 'forced_submitted' ? (_jsxs(_Fragment, { children: [_jsx(Descriptions.Item, { label: "\u5F3A\u5236\u4EA4\u5377\u65F6\u95F4", children: formatDateTime(detailQuery.data.submitTime) }), _jsx(Descriptions.Item, { label: "\u64CD\u4F5C\u4EBA\u5458", children: detailQuery.data.forceSubmittedBy == null
                                            ? '-'
                                            : String(detailQuery.data.forceSubmittedBy) }), _jsx(Descriptions.Item, { label: "\u5F3A\u5236\u4EA4\u5377\u539F\u56E0", children: detailQuery.data.forceSubmitReason || '-' })] })) : null, _jsx(Descriptions.Item, { label: "\u6700\u540E\u6D3B\u52A8\u65F6\u95F4", children: formatDateTime(detailQuery.data.lastActiveTime) })] })) : null, detailQuery.data ? (_jsxs("div", { style: { marginTop: 16 }, children: [_jsx(Typography.Title, { level: 5, children: "\u6B63\u5F0F\u6210\u7EE9" }), detailQuery.data.status === 'absent' || resultQuery.data?.resultStatus === 'absent' ? (_jsxs(Descriptions, { size: "small", column: 1, bordered: true, children: [_jsx(Descriptions.Item, { label: "\u7ED3\u679C", children: "\u7F3A\u8003" }), _jsx(Descriptions.Item, { label: "\u6210\u7EE9", children: "--" })] })) : resultQuery.data?.resultStatus === 'scored' ? (_jsxs(_Fragment, { children: [_jsxs(Descriptions, { size: "small", column: 1, bordered: true, children: [_jsx(Descriptions.Item, { label: "\u603B\u5206", children: formatScore(resultQuery.data.totalScore) }), _jsx(Descriptions.Item, { label: "\u6EE1\u5206", children: formatScore(resultQuery.data.fullScore) }), _jsx(Descriptions.Item, { label: "\u9898\u76EE\u6570", children: resultQuery.data.questionCount ?? '--' }), _jsx(Descriptions.Item, { label: "\u5DF2\u7B54", children: resultQuery.data.answeredCount ?? '--' }), _jsx(Descriptions.Item, { label: "\u672A\u7B54", children: resultQuery.data.unansweredCount ?? '--' }), _jsx(Descriptions.Item, { label: "\u6B63\u786E", children: resultQuery.data.correctCount ?? '--' }), _jsx(Descriptions.Item, { label: "\u9519\u8BEF", children: resultQuery.data.wrongCount ?? '--' }), _jsx(Descriptions.Item, { label: "\u8BC4\u5206\u65F6\u95F4", children: formatDateTime(resultQuery.data.scoredTime) }), _jsx(Descriptions.Item, { label: "\u8BC4\u5206\u7248\u672C", children: resultQuery.data.scoreVersion ?? '--' })] }), (resultQuery.data.questionScores || []).length > 0 ? (_jsx(Table, { style: { marginTop: 12 }, size: "small", rowKey: (row) => String(row.questionSnapshotId), pagination: false, dataSource: resultQuery.data.questionScores, columns: [
                                            { title: '题号', dataIndex: 'questionOrder', width: 60 },
                                            {
                                                title: '题型',
                                                dataIndex: 'questionType',
                                                width: 70,
                                                render: (value) => questionTypeLabel[value] || value,
                                            },
                                            {
                                                title: '作答',
                                                width: 70,
                                                render: (_, row) => (row.answered ? '已答' : '未答'),
                                            },
                                            {
                                                title: '对错',
                                                width: 70,
                                                render: (_, row) => (row.answered ? (row.correct ? '正确' : '错误') : '--'),
                                            },
                                            {
                                                title: '得分',
                                                width: 90,
                                                render: (_, row) => `${formatScore(row.awardedScore)} / ${formatScore(row.maxScore)}`,
                                            },
                                        ] })) : null] })) : (_jsx(Typography.Text, { type: "secondary", children: "\u6B63\u5F0F\u6210\u7EE9\u5C1A\u672A\u751F\u6210" }))] })) : null] })] }));
    if (embedded) {
        return _jsx("div", { className: "competition-formal-exam-session-panel competition-formal-exam-session-panel--embedded", children: content });
    }
    return (_jsx(Modal, { title: "\u7406\u8BBA\u8003\u8BD5 \u00B7 \u6B63\u5F0F\u6BD4\u8D5B\u63A7\u5236\u53F0", open: open, onCancel: () => {
            setDetailId(null);
            onClose();
        }, footer: null, width: 1120, destroyOnHidden: true, transitionName: "", maskTransitionName: "", className: "competition-formal-exam-session-panel", afterOpenChange: (visible) => {
            if (visible)
                return;
            resetPanelState();
        }, children: content }));
}

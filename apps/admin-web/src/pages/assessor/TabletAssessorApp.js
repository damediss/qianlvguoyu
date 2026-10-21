import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircleOutlined, CloudOutlined, CloudSyncOutlined, LeftOutlined, LogoutOutlined, MinusOutlined, PlusOutlined, ReloadOutlined, WifiOutlined, } from '@ant-design/icons';
import { Alert, Button, Empty, Input, List, Modal, Select, Space, Spin, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { completeMyPracticalSession, confirmMyPracticalStart, createPracticalWebSocketTicket, getMyAssessorExam, getMyAssessorWorkbenchContext, listMyAssessorExams, saveMyAssessorScoringDraft, startMyPracticalSession, submitMyAssessorScoringTask, switchMyAssessorWorkbenchTask, } from '../../api/practicalScoring';
import { logout } from '../../api/auth';
import { useAuthStore } from '../../stores/authStore';
import { assessorDraftKey, deleteAssessorDraft, loadAssessorDraft, saveAssessorDraft, } from '../../utils/assessorDraftDb';
import { AssessorCompleteSignModal } from './AssessorCompleteSignModal';
import { practicalWsBackoffMs } from './practicalWsReconnect';
import './TabletAssessorApp.css';
const FULL_SCORE = 100;
export function TabletAssessorApp() {
    const queryClient = useQueryClient();
    const currentUser = useAuthStore((state) => state.currentUser);
    const token = useAuthStore((state) => state.token);
    const tenantId = useAuthStore((state) => state.tenantId);
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const [selectedExamKey, setSelectedExamKey] = useState();
    const [activeTaskId, setActiveTaskId] = useState();
    const [deductions, setDeductions] = useState({});
    const [remarks, setRemarks] = useState({});
    const [note, setNote] = useState('');
    const [serverVersion, setServerVersion] = useState(0);
    const [online, setOnline] = useState(navigator.onLine);
    const [syncState, setSyncState] = useState('saved');
    // 实操采集 WS 断线重连（蓝绿方案 §5.6）：服务端重启/停旧色掐断连接后按退避自动重建。
    // tick 变化驱动两个 WS effect 重跑建连；attempt 在 onopen 成功后归零。
    const [wsReconnectTick, setWsReconnectTick] = useState(0);
    const wsRetryTimerRef = useRef();
    const wsAttemptRef = useRef(0);
    const scheduleWsReconnect = () => {
        if (wsRetryTimerRef.current != null) {
            return; // 已有待触发的重连
        }
        const delay = practicalWsBackoffMs(wsAttemptRef.current);
        wsAttemptRef.current += 1;
        wsRetryTimerRef.current = window.setTimeout(() => {
            wsRetryTimerRef.current = undefined;
            setWsReconnectTick((t) => t + 1);
        }, delay);
    };
    const hydratedTaskRef = useRef();
    const previousWorkbenchTaskRef = useRef();
    const syncTimerRef = useRef();
    const skipNextDraftSyncRef = useRef(false);
    const workbenchContextQuery = useQuery({
        queryKey: ['my-practical-scoring-workbench-context'],
        queryFn: () => getMyAssessorWorkbenchContext(),
        refetchInterval: (query) => {
            if (!online)
                return false;
            return query.state.data?.completeSign?.status === 'pending' ? 3_000 : 10_000;
        },
        staleTime: 0,
    });
    const examsQuery = useQuery({
        queryKey: ['my-practical-scoring-exams', workbenchContextQuery.data?.activeTaskId],
        queryFn: () => listMyAssessorExams(),
        enabled: workbenchContextQuery.isSuccess,
        refetchInterval: online ? 30_000 : false,
    });
    const switchWorkbenchTaskMutation = useMutation({
        mutationFn: (taskId) => switchMyAssessorWorkbenchTask(taskId),
        onSuccess: (context) => {
            queryClient.setQueryData(['my-practical-scoring-workbench-context'], context);
            setSelectedExamKey(undefined);
            setActiveTaskId(undefined);
            hydratedTaskRef.current = undefined;
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exam'] });
            message.success('已切换当前认定任务');
        },
    });
    // A response started before a leader switches tasks can arrive afterwards. Never render that
    // stale task in the tablet list; opening it would fail the server-side context check.
    const visibleExams = useMemo(() => {
        const activeWorkbenchTaskId = workbenchContextQuery.data?.activeTaskId;
        return activeWorkbenchTaskId
            ? (examsQuery.data || []).filter((exam) => exam.taskId === activeWorkbenchTaskId)
            : [];
    }, [examsQuery.data, workbenchContextQuery.data?.activeTaskId]);
    const selectedExam = useMemo(() => visibleExams.find((exam) => examKey(exam) === selectedExamKey), [selectedExamKey, visibleExams]);
    const examQuery = useQuery({
        queryKey: ['my-practical-scoring-exam', selectedExam?.taskId, selectedExam?.candidateId],
        queryFn: () => getMyAssessorExam(selectedExam.taskId, selectedExam.candidateId),
        enabled: Boolean(selectedExam),
        // 评分面板需要及时感知管理端的退回/状态变化，否则考评员会停留在旧的评分快照上
        refetchInterval: online ? 10_000 : false,
        staleTime: 0,
    });
    const activeDetail = useMemo(() => {
        const questions = examQuery.data?.questions || [];
        return questions.find((question) => question.task.id === activeTaskId) || questions[0];
    }, [activeTaskId, examQuery.data]);
    useEffect(() => {
        if (!selectedExamKey && visibleExams.length) {
            setSelectedExamKey(examKey(visibleExams[0]));
        }
    }, [selectedExamKey, visibleExams]);
    useEffect(() => {
        const currentTaskId = workbenchContextQuery.data?.activeTaskId;
        if (!currentTaskId)
            return;
        const previousTaskId = previousWorkbenchTaskRef.current;
        previousWorkbenchTaskRef.current = currentTaskId;
        if (previousTaskId && previousTaskId !== currentTaskId) {
            hydratedTaskRef.current = undefined;
            setSelectedExamKey(undefined);
            setActiveTaskId(undefined);
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exam'] });
            if (!workbenchContextQuery.data?.leader) {
                message.info('组长已切换当前认定任务，评分列表已同步更新');
            }
        }
    }, [queryClient, workbenchContextQuery.data?.activeTaskId, workbenchContextQuery.data?.leader]);
    useEffect(() => {
        const questions = examQuery.data?.questions || [];
        if (!questions.length) {
            setActiveTaskId(undefined);
            return;
        }
        if (!activeTaskId || !questions.some((question) => question.task.id === activeTaskId)) {
            hydratedTaskRef.current = undefined;
            setActiveTaskId(questions[0].task.id);
        }
    }, [activeTaskId, examQuery.data]);
    useEffect(() => {
        const onOnline = () => {
            setOnline(true);
            setSyncState('syncing');
        };
        const onOffline = () => {
            setOnline(false);
            setSyncState('offline');
        };
        window.addEventListener('online', onOnline);
        window.addEventListener('offline', onOffline);
        return () => {
            window.removeEventListener('online', onOnline);
            window.removeEventListener('offline', onOffline);
        };
    }, []);
    useEffect(() => {
        const detail = activeDetail;
        if (!detail || !currentUser)
            return;
        // 退回/重新编辑会让 task.id 不变但 revision、version、status 发生变化，
        // 必须以这三者组合作为水合去重键，否则考评员面板会停留在旧分数 / 旧版本上，
        // 导致退回后无法重新编辑或提交时出现版本冲突。
        const hydrateKey = `${detail.task.id}@${detail.task.revision}@${detail.task.version}@${detail.task.status}`;
        if (hydratedTaskRef.current === hydrateKey)
            return;
        hydratedTaskRef.current = hydrateKey;
        const key = assessorDraftKey(currentUser.id, detail.task.id, detail.task.revision);
        loadAssessorDraft(key).then((local) => {
            const source = local && local.version >= detail.task.version ? local : payloadFromDetail(detail);
            skipNextDraftSyncRef.current = true;
            setDeductions(Object.fromEntries(source.items.map((item) => [item.itemNo, item.deductedScore])));
            setRemarks(Object.fromEntries(source.items.map((item) => [item.itemNo, item.remark || ''])));
            setNote(source.note || '');
            setServerVersion(Math.max(detail.task.version, source.version));
            setSyncState(local && !online ? 'offline' : 'saved');
        }).catch(() => {
            const source = payloadFromDetail(detail);
            skipNextDraftSyncRef.current = true;
            setDeductions(Object.fromEntries(source.items.map((item) => [item.itemNo, item.deductedScore])));
            setRemarks(Object.fromEntries(source.items.map((item) => [item.itemNo, item.remark || ''])));
            setNote(source.note || '');
            setServerVersion(detail.task.version);
        });
    }, [activeDetail, currentUser, online]);
    const payload = useMemo(() => {
        if (!activeDetail)
            return undefined;
        return {
            version: serverVersion,
            items: activeDetail.scoreItems.map((item) => ({
                scoreItemId: item.id,
                itemNo: item.itemNo,
                deductedScore: deductions[item.itemNo] ?? item.deductedScore ?? 0,
                remark: remarks[item.itemNo] || undefined,
            })),
            note: note || undefined,
        };
    }, [activeDetail, deductions, note, remarks, serverVersion]);
    const draftMutation = useMutation({
        mutationFn: ({ taskId, value }) => saveMyAssessorScoringDraft(taskId, value),
        onSuccess: (detail) => {
            setServerVersion(detail.task.version);
            setSyncState('saved');
            patchQuestionCache(queryClient, examQuery.data, detail);
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
        },
        onError: (error) => {
            const conflict = error?.response?.status === 409 || error?.message?.includes('其他设备');
            setSyncState(conflict ? 'conflict' : 'offline');
        },
    });
    const confirmStartMutation = useMutation({
        mutationFn: () => confirmMyPracticalStart(activeDetail.task.id),
        onSuccess: (startSession) => {
            patchStartSessionCache(queryClient, examQuery.data, startSession);
            message.success(startSession.status === 'started' ? '全员已确认，统一开始' : '已确认，正在等待其他人员');
        },
    });
    const startSessionMutation = useMutation({
        mutationFn: () => startMyPracticalSession(activeDetail.task.id),
        onSuccess: (startSession) => {
            patchStartSessionCache(queryClient, examQuery.data, startSession);
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
            message.success('考试已开始，评分界面已同步解锁');
        },
    });
    const completeSessionMutation = useMutation({
        mutationFn: () => completeMyPracticalSession(activeDetail.task.id),
        onSuccess: (startSession) => {
            patchStartSessionCache(queryClient, examQuery.data, startSession);
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
            message.success('考试已完成，评分数据已锁定');
        },
        onError: (error) => {
            message.error(errorMessage(error, '完成考试失败，请检查评分状态'));
        },
    });
    useEffect(() => {
        const exam = examQuery.data?.exam;
        if (!exam || !token || !tenantId || !online || ['started', 'completed'].includes(exam.startSession?.status || '')) {
            return;
        }
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        let socket;
        let disposed = false;
        createPracticalWebSocketTicket().then((ticket) => {
            if (disposed)
                return;
            const params = new URLSearchParams({
                ticket,
                tenantId: String(tenantId),
                taskId: exam.taskId,
                candidateId: exam.candidateId,
            });
            socket = new WebSocket(`${protocol}//${window.location.host}/api/ws/practical-scoring?${params}`);
            socket.onopen = () => {
                wsAttemptRef.current = 0;
            };
            socket.onclose = () => {
                // 非主动关闭（服务端重启/蓝绿停旧色/网络抖动）→ 退避重连；主动 cleanup 已置 disposed
                if (!disposed) {
                    scheduleWsReconnect();
                }
            };
            socket.onmessage = (event) => {
                const data = JSON.parse(event.data);
                if (data?.type === 'complete-sign') {
                    queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-workbench-context'] });
                    return;
                }
                const startSession = data;
                patchStartSessionCache(queryClient, examQuery.data, startSession);
                if (startSession.status === 'started') {
                    message.success('考生与三名考评员均已确认，现在统一开始评分');
                }
            };
        });
        return () => {
            disposed = true;
            if (wsRetryTimerRef.current != null) {
                window.clearTimeout(wsRetryTimerRef.current);
                wsRetryTimerRef.current = undefined;
            }
            socket?.close();
        };
    }, [examQuery.data, online, queryClient, tenantId, token, wsReconnectTick]);
    useEffect(() => {
        const taskId = workbenchContextQuery.data?.activeTaskId;
        if (!taskId || !token || !tenantId || !online) {
            return;
        }
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        let socket;
        let disposed = false;
        createPracticalWebSocketTicket().then((ticket) => {
            if (disposed)
                return;
            const params = new URLSearchParams({
                ticket,
                tenantId: String(tenantId),
                taskId: String(taskId),
            });
            socket = new WebSocket(`${protocol}//${window.location.host}/api/ws/practical-scoring?${params}`);
            socket.onopen = () => {
                wsAttemptRef.current = 0;
            };
            socket.onclose = () => {
                if (!disposed) {
                    scheduleWsReconnect();
                }
            };
            socket.onmessage = (event) => {
                const data = JSON.parse(event.data);
                if (data?.type === 'complete-sign') {
                    queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-workbench-context'] });
                }
            };
        });
        return () => {
            disposed = true;
            if (wsRetryTimerRef.current != null) {
                window.clearTimeout(wsRetryTimerRef.current);
                wsRetryTimerRef.current = undefined;
            }
            socket?.close();
        };
    }, [online, queryClient, tenantId, token, workbenchContextQuery.data?.activeTaskId, wsReconnectTick]);
    useEffect(() => {
        if (!payload || !activeDetail || !currentUser)
            return;
        if (skipNextDraftSyncRef.current) {
            skipNextDraftSyncRef.current = false;
            return;
        }
        if (activeDetail.task.status === 'submitted'
            || activeDetail.startSession?.status !== 'started')
            return;
        const key = assessorDraftKey(currentUser.id, activeDetail.task.id, activeDetail.task.revision);
        saveAssessorDraft({
            ...payload,
            key,
            taskId: activeDetail.task.id,
            revision: activeDetail.task.revision,
            savedAt: Date.now(),
        }).catch(() => message.warning('本机草稿保存失败，请勿关闭页面'));
        if (syncTimerRef.current)
            window.clearTimeout(syncTimerRef.current);
        if (!online || syncState === 'conflict') {
            setSyncState(online ? 'conflict' : 'offline');
            return;
        }
        setSyncState('syncing');
        syncTimerRef.current = window.setTimeout(() => {
            draftMutation.mutate({ taskId: activeDetail.task.id, value: payload });
        }, 900);
        return () => {
            if (syncTimerRef.current)
                window.clearTimeout(syncTimerRef.current);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [deductions, remarks, note, online]);
    useEffect(() => {
        if (online && syncState === 'offline' && payload && activeDetail) {
            setSyncState('syncing');
            draftMutation.mutate({ taskId: activeDetail.task.id, value: payload });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [online]);
    const submitMutation = useMutation({
        mutationFn: () => submitMyAssessorScoringTask(activeDetail.task.id, payload),
        onSuccess: async (detail) => {
            if (currentUser) {
                await deleteAssessorDraft(assessorDraftKey(currentUser.id, detail.task.id, detail.task.revision));
            }
            message.success('本题评分已提交并锁定');
            patchQuestionCache(queryClient, examQuery.data, detail);
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exams'] });
            queryClient.invalidateQueries({ queryKey: ['my-practical-scoring-exam', detail.task.taskId, detail.task.candidateId] });
            setSyncState('saved');
        },
    });
    const scoringStarted = activeDetail?.startSession?.status === 'started';
    const sessionCompleted = activeDetail?.startSession?.status === 'completed';
    // ready 表示可开考；waiting 且考评员已满确认时同样视为可开考，
    // 兼容历史 waiting 会话（后端会在开始时把 waiting 提升为 started）
    const startSession = activeDetail?.startSession?.status === 'ready'
        || (activeDetail?.startSession?.status === 'waiting'
            && activeDetail?.startSession?.confirmedAssessors === activeDetail?.startSession?.requiredAssessors);
    const startSessionReady = Boolean(startSession);
    const isGroupLeader = Boolean(activeDetail?.task.leader);
    const examReviewRequired = Boolean(examQuery.data?.exam.reviewRequired);
    const questionTotal = activeDetail ? questionLiveScore(activeDetail, deductions) : 0;
    const examTotal = examLiveScore(examQuery.data, activeDetail?.task.id, questionTotal);
    const fullScore = FULL_SCORE;
    const ownSubmittedQuestions = examQuery.data?.questions.filter((question) => question.task.status === 'submitted').length ?? 0;
    const allOwnQuestionsSubmitted = Boolean(examQuery.data?.questions.length)
        && ownSubmittedQuestions === examQuery.data?.questions.length;
    function adjust(itemNo, delta, max) {
        setDeductions((current) => {
            const next = Math.min(max, Math.max(0, (current[itemNo] || 0) + delta));
            return { ...current, [itemNo]: Number(next.toFixed(2)) };
        });
    }
    async function handleLogout() {
        try {
            await logout();
        }
        catch {
            // 后端退出失败时仍清理本机登录态
        }
        finally {
            clearAuth();
            window.location.assign('/assessor-scoring');
        }
    }
    function switchWorkbenchTask(taskId) {
        const task = workbenchContextQuery.data?.switchableTasks.find((item) => item.taskId === workbenchContextQuery.data?.activeTaskId);
        const execute = () => switchWorkbenchTaskMutation.mutate(taskId);
        if (task && task.startedSessionCount > 0) {
            Modal.confirm({
                title: '当前任务仍有进行中的考试',
                content: `该任务有 ${task.startedSessionCount} 个已开考未完成场次。切换不会中断评分，确认后可继续在其他任务操作。`,
                okText: '仍要切换',
                cancelText: '留在当前任务',
                onOk: execute,
            });
            return;
        }
        execute();
    }
    return (_jsxs("div", { className: "assessor-tablet-shell", children: [_jsx(AssessorCompleteSignModal, { open: Boolean(workbenchContextQuery.data?.completeSign?.status === 'pending'
                    && workbenchContextQuery.data?.completeSign?.currentAssessorPending), taskId: workbenchContextQuery.data?.activeTaskId, signerName: currentUser?.displayName || '考评员', signedCount: workbenchContextQuery.data?.completeSign?.signedCount || 0, requiredCount: workbenchContextQuery.data?.completeSign?.requiredCount || 3, onSigned: (view) => {
                    patchCompleteSignCache(queryClient, view);
                } }), _jsxs("header", { className: "assessor-tablet-header", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 3, children: "\u5B9E\u64CD\u8003\u8BC4" }), _jsxs(Typography.Text, { children: [currentUser?.displayName || '考评员', "\uFF0C\u8BF7\u72EC\u7ACB\u5B8C\u6210\u672C\u4EBA\u8BC4\u5206"] })] }), _jsxs(Space, { children: [workbenchContextQuery.data?.leader ? (_jsx(Select, { "aria-label": "\u5F53\u524D\u8BA4\u5B9A\u4EFB\u52A1", style: { minWidth: 250 }, value: workbenchContextQuery.data.activeTaskId, loading: workbenchContextQuery.isFetching, disabled: switchWorkbenchTaskMutation.isPending, onChange: switchWorkbenchTask, options: workbenchContextQuery.data.switchableTasks.map((task) => ({
                                    value: task.taskId,
                                    label: [task.taskNo, task.taskName].filter(Boolean).join(' ') || `认定任务 ${task.taskId}`,
                                })) })) : workbenchContextQuery.data?.leaderAssessorName ? (_jsxs(Tag, { color: "blue", children: ["\u8DDF\u968F\u7EC4\u957F\uFF1A", workbenchContextQuery.data.leaderAssessorName] })) : null, _jsx(Tag, { icon: online ? _jsx(WifiOutlined, {}) : _jsx(CloudOutlined, {}), color: online ? 'success' : 'warning', children: online ? '在线' : '短时断网' }), _jsx(Tag, { icon: _jsx(CloudSyncOutlined, {}), color: syncState === 'conflict' ? 'error' : 'processing', children: syncText(syncState) }), _jsx(Button, { icon: _jsx(LogoutOutlined, {}), onClick: handleLogout, children: "\u5207\u6362\u8D26\u53F7" })] })] }), _jsxs("main", { className: `assessor-tablet-main ${selectedExam ? 'has-selection' : ''}`, children: [_jsxs("aside", { className: "assessor-task-list", children: [_jsxs("div", { className: "assessor-task-list-title", children: [_jsx(Typography.Title, { level: 4, children: "\u5F53\u524D\u8BA4\u5B9A\u4EFB\u52A1" }), _jsx(Button, { size: "large", icon: _jsx(ReloadOutlined, {}), onClick: () => examsQuery.refetch(), disabled: !online })] }), _jsx(Spin, { spinning: examsQuery.isFetching, children: _jsx(List, { dataSource: visibleExams, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u8003\u8BD5\u4EFB\u52A1" }) }, renderItem: (exam) => (_jsx(List.Item, { className: examKey(exam) === selectedExamKey ? 'selected' : '', onClick: () => {
                                            hydratedTaskRef.current = undefined;
                                            setActiveTaskId(undefined);
                                            setSelectedExamKey(examKey(exam));
                                        }, children: _jsxs("div", { className: "assessor-task-card", children: [_jsxs(Space, { wrap: true, children: [_jsx(Tag, { color: exam.status === 'submitted' ? 'success' : exam.status === 'returned' ? 'error' : 'blue', children: statusText(exam.status) }), exam.reviewRequired ? _jsx(Tag, { color: "error", children: "\u5206\u5DEE\u5F02\u5E38" }) : null, _jsx("strong", { children: exam.candidateName }), _jsx("span", { children: exam.candidateNo || '' })] }), _jsxs(Typography.Text, { children: [exam.workTypeName || '-', " / ", exam.levelName || '-'] }), _jsxs("small", { children: [exam.submittedQuestions, "/", exam.totalQuestions, " \u9898\u5DF2\u63D0\u4EA4 \u00B7 ", exam.currentScore || 0, "/", FULL_SCORE, " \u5206"] })] }) })) }) })] }), _jsx("section", { className: "assessor-score-workspace", children: !selectedExam ? _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u8003\u8BD5\u4EFB\u52A1" }) : !examQuery.data && examQuery.isFetching ? _jsx(Spin, { size: "large" }) : activeDetail && examQuery.data ? (_jsxs(_Fragment, { children: [_jsx(Button, { className: "assessor-mobile-back", icon: _jsx(LeftOutlined, {}), onClick: () => setSelectedExamKey(undefined), children: "\u8FD4\u56DE\u4EFB\u52A1" }), _jsxs("div", { className: "assessor-candidate-bar", children: [_jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u5F53\u524D\u8003\u751F" }), _jsx(Typography.Title, { level: 3, children: examQuery.data.exam.candidateName }), _jsxs(Typography.Text, { children: [examQuery.data.exam.candidateNo || '-', " / ", examQuery.data.exam.workTypeName || '-', " / ", examQuery.data.exam.levelName || '-'] })] }), _jsxs("div", { className: "assessor-exam-stats", children: [_jsx(Tag, { color: isGroupLeader ? 'gold' : 'blue', children: isGroupLeader ? '组长端' : '组员端' }), _jsxs("div", { className: "assessor-live-score", children: [_jsx("span", { children: "\u6574\u573A\u5B9E\u65F6\u5F97\u5206" }), _jsxs("strong", { children: [formatScore(examTotal), _jsxs("small", { children: ["/", fullScore] })] })] }), _jsxs("div", { children: [_jsx("span", { children: "\u9898\u76EE\u63D0\u4EA4" }), _jsxs("strong", { children: [ownSubmittedQuestions, "/", examQuery.data.questions.length] })] })] })] }), activeDetail.task.status === 'returned' && (_jsx(Alert, { type: "warning", showIcon: true, message: "\u7BA1\u7406\u5458\u5DF2\u9000\u56DE\uFF0C\u8BF7\u91CD\u65B0\u8BC4\u5206", description: activeDetail.returnReason })), isGroupLeader && examReviewRequired && (_jsx(Alert, { type: "error", showIcon: true, message: "\u6210\u7EE9\u5206\u5DEE\u5F02\u5E38\uFF0C\u6682\u4E0D\u80FD\u5B8C\u6210\u8003\u8BD5", description: "\u4E09\u540D\u8003\u8BC4\u5458\u7ED9\u51FA\u7684\u6210\u7EE9\u504F\u5DEE\u8D85\u8FC710\u5206\uFF0C\u8BF7\u5728\u5B9E\u64CD\u7BA1\u7406\u4E2D\u4FEE\u6539\u5F02\u5E38\u5206\u6570\u5E76\u91CD\u65B0\u63D0\u4EA4\u3002\u4FEE\u6B63\u540E\u672C\u9875\u4F1A\u81EA\u52A8\u5237\u65B0\u72B6\u6001\u3002" })), syncState === 'conflict' && (_jsx(Alert, { type: "error", showIcon: true, message: "\u8BE5\u9898\u5DF2\u5728\u5176\u4ED6\u8BBE\u5907\u66F4\u65B0", description: "\u7CFB\u7EDF\u5DF2\u505C\u6B62\u81EA\u52A8\u8986\u76D6\u3002\u8BF7\u91CD\u65B0\u52A0\u8F7D\u670D\u52A1\u7AEF\u7248\u672C\u540E\u7EE7\u7EED\u3002", action: _jsx(Button, { onClick: () => window.location.reload(), children: "\u91CD\u65B0\u52A0\u8F7D" }) })), !scoringStarted && activeDetail.startSession && (_jsxs("div", { className: "assessor-start-gate", children: [_jsx(Typography.Title, { level: 3, children: "\u7EDF\u4E00\u5F00\u8003\u786E\u8BA4" }), _jsxs("div", { className: "assessor-start-steps", children: [_jsxs(Tag, { color: activeDetail.startSession.confirmedAssessors === activeDetail.startSession.requiredAssessors ? 'success' : 'processing', children: ["\u8003\u8BC4\u5458 ", activeDetail.startSession.confirmedAssessors, "/", activeDetail.startSession.requiredAssessors] }), _jsx(Tag, { color: "success", children: "\u8BD5\u5377\u5DF2\u5339\u914D" })] }), _jsx(Button, { type: "primary", size: "large", style: { display: isGroupLeader ? undefined : 'none' }, disabled: !startSessionReady, loading: startSessionMutation.isPending, onClick: () => startSessionMutation.mutate(), children: startSessionReady ? '组长开始考试' : '等待全部确认' }), activeDetail.startSession.currentAssessorConfirmed ? (_jsx(Button, { size: "large", disabled: true, children: "\u5DF2\u786E\u8BA4" })) : null, _jsx(Button, { size: "large", style: { display: activeDetail.startSession.currentAssessorConfirmed ? 'none' : undefined }, disabled: activeDetail.startSession.currentAssessorConfirmed, loading: confirmStartMutation.isPending, onClick: () => confirmStartMutation.mutate(), children: "\u6211\u5DF2\u6838\u5BF9\u8003\u751F\u4E0E\u8BD5\u5377\uFF0C\u786E\u8BA4\u5C31\u7EEA" })] })), _jsx("div", { className: "assessor-question-nav", children: examQuery.data.questions.map((question, index) => {
                                        const active = question.task.id === activeDetail.task.id;
                                        const score = active ? questionTotal : (question.task.totalScore || 0);
                                        return (_jsxs("button", { type: "button", className: active ? 'active' : '', onClick: () => {
                                                hydratedTaskRef.current = undefined;
                                                setActiveTaskId(question.task.id);
                                            }, children: [_jsxs("span", { children: ["\u8BD5\u9898 ", index + 1] }), _jsx("strong", { children: shortTitle(question.task.questionTitle) }), _jsxs("small", { children: [formatScore(score), " / ", formatScore(questionMaxScore(question)), " \u5206"] }), _jsx(Tag, { color: question.task.status === 'submitted' ? 'success' : question.task.status === 'returned' ? 'error' : 'blue', children: statusText(question.task.status) })] }, question.task.id));
                                    }) }), _jsxs("div", { className: "assessor-question-card", children: [_jsxs("div", { className: "assessor-question-title-row", children: [_jsx(Typography.Title, { level: 3, children: activeDetail.task.questionTitle }), _jsxs("div", { className: "assessor-question-score", children: [_jsx("span", { children: "\u672C\u9898\u5F97\u5206" }), _jsxs("strong", { children: [formatScore(questionTotal), " / ", formatScore(questionMaxScore(activeDetail))] })] })] }), _jsx(Typography.Paragraph, { children: activeDetail.questionContent || '无补充题目说明' }), _jsxs("div", { className: "assessor-prepare-grid", children: [_jsxs("div", { children: [_jsx("strong", { children: "\u8003\u751F\u51C6\u5907" }), _jsx("span", { children: activeDetail.studentPrepare || '-' })] }), _jsxs("div", { children: [_jsx("strong", { children: "\u8BBE\u5907\u8BBE\u65BD" }), _jsx("span", { children: activeDetail.equipmentPrepare || '-' })] })] })] }), _jsx("div", { className: "assessor-score-items", children: activeDetail.scoreItems.map((item) => {
                                        const deducted = deductions[item.itemNo] ?? item.deductedScore ?? 0;
                                        const awarded = Math.max(0, item.maxScore - deducted);
                                        return (_jsxs("article", { className: "assessor-score-item", children: [_jsxs("div", { className: "assessor-score-item-head", children: [_jsxs("div", { children: [_jsx("span", { className: "assessor-item-no", children: item.itemNo }), _jsx(Typography.Title, { level: 4, children: item.itemName })] }), _jsxs("strong", { children: [formatScore(awarded), " / ", formatScore(item.maxScore), " \u5206"] })] }), _jsx(Typography.Paragraph, { children: item.criterion }), _jsxs("div", { className: "assessor-deduction-control", children: [_jsxs(Button, { icon: _jsx(MinusOutlined, {}), disabled: !scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted' || deducted <= 0, onClick: () => adjust(item.itemNo, -item.deductionStep, item.maxDeduction), children: ["\u64A4\u9500 ", formatScore(item.deductionStep), " \u5206"] }), _jsxs("div", { children: [_jsx("span", { children: "\u5F53\u524D\u6263\u5206" }), _jsx("strong", { children: formatScore(deducted) })] }), _jsxs(Button, { danger: true, type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted' || deducted >= item.maxDeduction, onClick: () => adjust(item.itemNo, item.deductionStep, item.maxDeduction), children: ["\u6263 ", formatScore(item.deductionStep), " \u5206"] })] }), _jsx(Input, { size: "large", value: remarks[item.itemNo], disabled: !scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted', onChange: (event) => setRemarks((current) => ({ ...current, [item.itemNo]: event.target.value })), placeholder: "\u6263\u5206\u8BF4\u660E\uFF08\u9009\u586B\uFF09" })] }, item.id));
                                    }) }), _jsxs("div", { className: "assessor-note", children: [_jsx(Typography.Text, { strong: true, children: "\u672C\u9898\u8003\u8BC4\u8BF4\u660E" }), _jsx(Input.TextArea, { rows: 3, size: "large", value: note, disabled: !scoringStarted || sessionCompleted || activeDetail.task.status === 'submitted', onChange: (event) => setNote(event.target.value), placeholder: "\u53EF\u8BB0\u5F55\u73B0\u573A\u60C5\u51B5\uFF0C\u4EC5\u7BA1\u7406\u4EBA\u5458\u53EF\u67E5\u770B" })] }), _jsxs("footer", { className: "assessor-submit-bar", children: [_jsxs("div", { children: [_jsx("span", { children: "\u6574\u573A\u5408\u8BA1" }), _jsxs("strong", { children: [formatScore(examTotal), " / ", fullScore, " \u5206"] })] }), _jsx(Button, { type: "primary", size: "large", icon: _jsx(CheckCircleOutlined, {}), disabled: !scoringStarted || sessionCompleted || !online || syncState === 'syncing'
                                                || syncState === 'conflict' || activeDetail.task.status === 'submitted', loading: submitMutation.isPending, onClick: () => Modal.confirm({
                                                title: '确认提交本题评分？',
                                                content: '提交后本题将锁定，只有管理员退回后才能重新评分。',
                                                okText: '确认提交',
                                                cancelText: '继续检查',
                                                onOk: () => submitMutation.mutateAsync(),
                                            }), children: activeDetail.task.status === 'submitted' ? '本题已提交' : online ? '提交本题' : '联网后提交' }), _jsx(Button, { size: "large", disabled: !scoringStarted || !allOwnQuestionsSubmitted || sessionCompleted || !isGroupLeader || examReviewRequired, loading: completeSessionMutation.isPending, onClick: () => Modal.confirm({
                                                title: '确认完成本场考试？',
                                                content: '系统会校验三名考评员是否全部提交，完成后锁定评分并生成汇总记录。',
                                                okText: '完成考试',
                                                cancelText: '再检查一下',
                                                onOk: () => completeSessionMutation.mutateAsync(),
                                            }), children: sessionCompleted
                                                ? '考试已完成'
                                                : examReviewRequired
                                                    ? '等待实操管理修正异常'
                                                    : !isGroupLeader && allOwnQuestionsSubmitted
                                                        ? '等待组长完成考试'
                                                        : '完成考试' })] })] })) : _jsx(Empty, { description: "\u8003\u8BD5\u4EFB\u52A1\u52A0\u8F7D\u5931\u8D25" }) })] })] }));
}
function payloadFromDetail(detail) {
    return {
        version: detail.task.version,
        items: detail.scoreItems.map((item) => ({
            scoreItemId: item.id,
            itemNo: item.itemNo,
            deductedScore: item.deductedScore || 0,
            remark: item.remark,
        })),
        note: detail.progressNote,
    };
}
function patchCompleteSignCache(queryClient, completeSign) {
    if (!completeSign)
        return;
    queryClient.setQueryData(['my-practical-scoring-workbench-context'], (current) => (current ? { ...current, completeSign } : current));
}
function patchQuestionCache(queryClient, exam, detail) {
    if (!exam)
        return;
    queryClient.setQueryData(['my-practical-scoring-exam', detail.task.taskId, detail.task.candidateId], {
        ...exam,
        questions: exam.questions.map((question) => question.task.id === detail.task.id ? detail : question),
    });
}
function patchStartSessionCache(queryClient, exam, startSession) {
    if (!exam || !startSession)
        return;
    queryClient.setQueryData(['my-practical-scoring-exam', exam.exam.taskId, exam.exam.candidateId], {
        ...exam,
        exam: { ...exam.exam, startSession: mergeStartSession(exam.exam.startSession, startSession) },
        questions: exam.questions.map((question) => ({
            ...question,
            startSession: mergeStartSession(question.startSession, startSession),
        })),
    });
}
function mergeStartSession(current, incoming) {
    return current
        ? { ...incoming, currentAssessorConfirmed: current.currentAssessorConfirmed || incoming.currentAssessorConfirmed }
        : incoming;
}
function examKey(exam) {
    return `${exam.taskId}:${exam.candidateId}:${exam.paperId}`;
}
function questionLiveScore(detail, deductions) {
    return detail.scoreItems.reduce((sum, item) => sum + Math.max(0, item.maxScore - (deductions[item.itemNo] ?? item.deductedScore ?? 0)), 0);
}
function examLiveScore(exam, activeTaskId, activeScore) {
    if (!exam)
        return 0;
    return exam.questions.reduce((sum, question) => (sum + (question.task.id === activeTaskId ? activeScore : (question.task.totalScore || 0))), 0);
}
function questionMaxScore(detail) {
    return detail.scoreItems.reduce((sum, item) => sum + item.maxScore, 0);
}
function shortTitle(title) {
    if (!title)
        return '-';
    return title.replace(/^试题\s*\d+[、.．]?\s*/, '').replace(/[（(]\s*\d+(?:\.\d+)?\s*分\s*[）)]/, '').trim();
}
function formatScore(value) {
    if (value == null)
        return '0';
    return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, '');
}
function statusText(status) {
    return { assigned: '待评分', draft: '评分中', submitted: '已提交', returned: '已退回' }[status] || status;
}
function syncText(status) {
    return { saved: '已同步', syncing: '同步中', offline: '已保存到本机', conflict: '版本冲突' }[status] || status;
}
function errorMessage(error, fallback) {
    return error?.response?.data?.message
        || error?.message
        || fallback;
}

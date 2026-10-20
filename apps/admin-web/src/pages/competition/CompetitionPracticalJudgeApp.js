import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircleOutlined, CloudOutlined, CloudSyncOutlined, LeftOutlined, LogoutOutlined, MinusOutlined, PlusOutlined, ReloadOutlined, WifiOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Empty, Input, List, Modal, Select, Space, Spin, Tag, Typography, message } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { logout } from '../../api/auth';
import { completeCompetitionPracticalJudgeSession, confirmCompetitionPracticalJudgeStart, createCompetitionPracticalRealtimeTicket, listCompetitionPracticalJudgeTasks, openCompetitionPracticalJudgeTask, saveCompetitionPracticalJudgeTask, selectCompetitionPracticalJudgeActiveStage, startCompetitionPracticalJudgeSession, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import '../assessor/TabletAssessorApp.css';
function plain(value) {
    return (value ?? '')
        .replace(/<br\s*\/?\s*>/gi, '\n')
        .replace(/<\/p>/gi, '\n')
        .replace(/<[^>]*>/g, '')
        .replace(/&nbsp;/gi, ' ')
        .replace(/&amp;/gi, '&')
        .replace(/&lt;/gi, '<')
        .replace(/&gt;/gi, '>')
        .replace(/\n{3,}/g, '\n\n')
        .trim();
}
function asRecord(value) {
    return value != null && typeof value === 'object' && !Array.isArray(value)
        ? value
        : undefined;
}
function detailText(detail, ...keys) {
    const record = asRecord(detail);
    for (const key of keys) {
        const value = record?.[key];
        if (typeof value === 'string' && plain(value))
            return plain(value);
    }
    return '';
}
function formatScore(value) {
    const score = Number(value ?? 0);
    return Number.isInteger(score) ? String(score) : score.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}
function taskStatusText(task) {
    if (task.startStatus === 'awaiting_leader')
        return task.judgeRole === 'leader' ? '待完成考试' : '等待组长完成';
    if (task.scoreSheetStatus === 'submitted_locked')
        return '已提交';
    if (task.startStatus === 'waiting')
        return task.currentJudgeConfirmed ? '等待其他裁判' : '待确认';
    if (task.startStatus === 'ready')
        return task.judgeRole === 'leader' ? '待组长开考' : '等待组长开考';
    if (task.startStatus === 'completed')
        return '已完成';
    if (task.scoreSheetStatus === 'unlocked_for_rescore')
        return '已退回';
    if (task.scoreSheetStatus === 'draft')
        return '评分中';
    return '待评分';
}
function questionProgressOf(task, paperQuestionId) {
    if (!task || paperQuestionId == null)
        return undefined;
    return task.questionProgress?.find((item) => String(item.paperQuestionId) === String(paperQuestionId));
}
function questionStatusOf(task, paperQuestionId) {
    if (!task)
        return 'scoring';
    if (task.scoreSheetStatus === 'submitted_locked')
        return 'submitted';
    const progress = questionProgressOf(task, paperQuestionId);
    if (progress?.status)
        return progress.status;
    if (task.scoreSheetStatus === 'unlocked_for_rescore')
        return 'returned';
    return 'scoring';
}
function questionStatusText(status) {
    return { submitted: '已提交', scoring: '评分中', returned: '已退回', unopened: '待评分' }[status] || status;
}
function syncText(state) {
    return {
        saved: '已同步',
        syncing: '同步中',
        offline: '已保存到本机',
        error: '同步失败',
    }[state];
}
function contestantLabel(task) {
    return task.contestantName || task.contestantNo || `选手 ${task.contestantMemberId}`;
}
function taskStageId(task) {
    return String(task.stageId || task.assessmentSheet?.stageId || 'unassigned');
}
function taskScopeLabel(task) {
    const parts = [task.eventName, task.workTypeName, task.levelName, task.stageName]
        .map((part) => (part ?? '').trim())
        .filter(Boolean);
    return parts.join(' · ') || '未分赛段';
}
function errorMessage(error, fallback) {
    return error?.response?.data?.message
        || error?.message
        || fallback;
}
function rowDefault(item) {
    const additive = item.scoringMethod === 'additive';
    return {
        score: additive ? 0 : Number(item.maxScore || 0),
        deduction: 0,
        remark: '',
    };
}
function localDraftKey(userId, assignmentId) {
    return `competition-practical-judge-draft:${userId ?? 'anonymous'}:${assignmentId}`;
}
export function CompetitionPracticalJudgeApp() {
    const token = useAuthStore((state) => state.token);
    const tenantId = useAuthStore((state) => state.tenantId);
    const currentUser = useAuthStore((state) => state.currentUser);
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const queryClient = useQueryClient();
    const [selectedId, setSelectedId] = useState();
    const [activeStageId, setActiveStageId] = useState();
    const [activeQuestionId, setActiveQuestionId] = useState();
    const [scores, setScores] = useState({});
    const [online, setOnline] = useState(navigator.onLine);
    const [realtimeConnected, setRealtimeConnected] = useState(false);
    const [syncState, setSyncState] = useState('saved');
    const [dirty, setDirty] = useState(false);
    const hydratedAssignmentRef = useRef();
    const openedAssignmentsRef = useRef(new Set());
    const payloadFingerprintRef = useRef('');
    const tasksQuery = useQuery({
        queryKey: ['competition-practical-judge-tasks'],
        queryFn: listCompetitionPracticalJudgeTasks,
        refetchInterval: import.meta.env.MODE === 'test' || !online
            ? false
            : (realtimeConnected ? 8_000 : 2_000),
        refetchIntervalInBackground: true,
        staleTime: 0,
    });
    const tasks = tasksQuery.data ?? [];
    const isGroupLeaderUser = tasks.some((task) => task.judgeRole === 'leader');
    const stages = useMemo(() => {
        const map = new Map();
        tasks.forEach((task) => {
            const id = taskStageId(task);
            if (!map.has(id))
                map.set(id, { id, name: taskScopeLabel(task) });
        });
        return [...map.values()];
    }, [tasks]);
    const serverActiveStageId = useMemo(() => {
        const fromTask = tasks.find((task) => task.activeStageId != null && String(task.activeStageId) !== '');
        return fromTask ? String(fromTask.activeStageId) : undefined;
    }, [tasks]);
    const resolvedStageId = isGroupLeaderUser
        ? (activeStageId || serverActiveStageId || stages[0]?.id)
        : (serverActiveStageId || stages[0]?.id);
    const resolvedStageName = stages.find((stage) => stage.id === resolvedStageId)?.name
        || '当前赛段';
    const visibleTasks = useMemo(() => {
        if (!resolvedStageId)
            return tasks;
        return tasks.filter((task) => taskStageId(task) === resolvedStageId);
    }, [resolvedStageId, tasks]);
    const selected = useMemo(() => visibleTasks.find((item) => String(item.assignmentId) === selectedId), [selectedId, visibleTasks]);
    const questions = useMemo(() => (selected?.assessmentSheet?.sheet.questions ?? []), [selected?.assessmentSheet?.sheet.questions]);
    const rows = useMemo(() => questions.flatMap((question) => {
        const items = Array.isArray(question.scoreItems) ? question.scoreItems : [];
        return items.map((item) => ({
            question,
            item: { ...item, scoringMethod: item.scoringMethod || question.scoringMethod || 'deductive' },
            key: `${question.paperQuestionId}:${item.itemNo}`,
        }));
    }), [questions]);
    const activeQuestion = useMemo(() => questions.find((question) => String(question.paperQuestionId) === activeQuestionId) || questions[0], [activeQuestionId, questions]);
    const activeRows = useMemo(() => rows.filter((row) => row.question.paperQuestionId === activeQuestion?.paperQuestionId), [activeQuestion?.paperQuestionId, rows]);
    const locked = selected?.scoreSheetStatus === 'submitted_locked';
    const scoringStarted = selected?.startStatus === 'started';
    const sessionCompleted = selected?.startStatus === 'completed';
    const awaitingLeaderCompletion = selected?.startStatus === 'awaiting_leader';
    const scoreDifferenceExceeded = selected?.scoreDifferenceStatus === 'exceeded';
    const startSessionReady = selected?.startStatus === 'ready';
    const isGroupLeader = selected?.judgeRole === 'leader';
    const activeQuestionStatus = questionStatusOf(selected, activeQuestion?.paperQuestionId);
    const questionLocked = activeQuestionStatus === 'submitted';
    useEffect(() => {
        if (selectedId && visibleTasks.some((task) => String(task.assignmentId) === selectedId))
            return;
        if (visibleTasks.length) {
            hydratedAssignmentRef.current = undefined;
            setSelectedId(String(visibleTasks[0].assignmentId));
            return;
        }
        setSelectedId(undefined);
    }, [selectedId, visibleTasks]);
    useEffect(() => {
        if (!selected)
            return;
        const assignmentId = String(selected.assignmentId);
        if (hydratedAssignmentRef.current === assignmentId)
            return;
        hydratedAssignmentRef.current = assignmentId;
        const initial = {};
        rows.forEach(({ key, item, question }) => {
            const progress = questionProgressOf(selected, question.paperQuestionId);
            const savedEntry = progress?.entries?.find((entry) => Number(entry.paperItemNo) === Number(item.itemNo));
            if (savedEntry && Number.isFinite(Number(savedEntry.score)) && Number.isFinite(Number(savedEntry.deduction))) {
                initial[key] = {
                    score: Number(savedEntry.score),
                    deduction: Number(savedEntry.deduction),
                    remark: savedEntry.remark || '',
                };
                return;
            }
            initial[key] = rowDefault(item);
        });
        try {
            const local = JSON.parse(localStorage.getItem(localDraftKey(currentUser?.id, assignmentId)) || '{}');
            rows.forEach(({ key, item, question }) => {
                if (questionStatusOf(selected, question.paperQuestionId) === 'submitted')
                    return;
                const saved = local[key];
                if (saved && Number.isFinite(Number(saved.score)) && Number.isFinite(Number(saved.deduction))) {
                    initial[key] = { ...rowDefault(item), ...saved };
                }
            });
        }
        catch {
            localStorage.removeItem(localDraftKey(currentUser?.id, assignmentId));
        }
        setScores(initial);
        setActiveQuestionId(questions[0] ? String(questions[0].paperQuestionId) : undefined);
        setDirty(false);
        setSyncState('saved');
        payloadFingerprintRef.current = '';
    }, [currentUser?.id, questions, rows, selected]);
    useEffect(() => {
        if (!selected || !scoringStarted || selected.scoreSheetId)
            return;
        const assignmentId = String(selected.assignmentId);
        if (openedAssignmentsRef.current.has(assignmentId))
            return;
        openedAssignmentsRef.current.add(assignmentId);
        void Promise.resolve(openCompetitionPracticalJudgeTask(selected.assignmentId))
            .then((result) => {
            if (result)
                patchTaskCache(queryClient, result);
        })
            .catch((error) => {
            openedAssignmentsRef.current.delete(assignmentId);
            message.error(errorMessage(error, '评分任务打开失败'));
        });
    }, [queryClient, scoringStarted, selected]);
    useEffect(() => {
        const onOnline = () => {
            setOnline(true);
            setSyncState((current) => current === 'offline' ? 'syncing' : current);
        };
        const onOffline = () => {
            setOnline(false);
            setRealtimeConnected(false);
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
        if (!token || !tenantId || !online)
            return;
        if (import.meta.env.MODE === 'test')
            return;
        let socket;
        let disposed = false;
        let retryTimer;
        let refetchTimer;
        let attempt = 0;
        function connect() {
            createCompetitionPracticalRealtimeTicket().then((ticket) => {
                if (disposed)
                    return;
                const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                const query = new URLSearchParams({ ticket, tenantId: String(tenantId) });
                socket = new WebSocket(`${protocol}//${window.location.host}/api/ws/competition-practical?${query}`);
                socket.onopen = () => {
                    attempt = 0;
                    setRealtimeConnected(true);
                };
                socket.onerror = () => setRealtimeConnected(false);
                socket.onclose = () => {
                    setRealtimeConnected(false);
                    if (disposed)
                        return;
                    const delay = Math.min(8_000, 400 * 2 ** attempt);
                    attempt += 1;
                    retryTimer = window.setTimeout(connect, delay);
                };
                socket.onmessage = () => {
                    if (refetchTimer != null)
                        window.clearTimeout(refetchTimer);
                    refetchTimer = window.setTimeout(() => {
                        void queryClient.refetchQueries({ queryKey: ['competition-practical-judge-tasks'] });
                    }, 50);
                };
            }).catch(() => {
                setRealtimeConnected(false);
                if (disposed)
                    return;
                retryTimer = window.setTimeout(connect, 2_000);
            });
        }
        connect();
        return () => {
            disposed = true;
            if (retryTimer != null)
                window.clearTimeout(retryTimer);
            if (refetchTimer != null)
                window.clearTimeout(refetchTimer);
            socket?.close();
        };
    }, [online, queryClient, tenantId, token]);
    const entries = useMemo(() => activeRows.map(({ question, item, key }) => {
        const value = scores[key] ?? rowDefault(item);
        return {
            paperQuestionId: question.paperQuestionId,
            paperItemNo: item.itemNo,
            score: value.score,
            deduction: value.deduction,
            remark: value.remark || undefined,
        };
    }), [activeRows, scores]);
    const payloadFingerprint = useMemo(() => JSON.stringify(entries), [entries]);
    payloadFingerprintRef.current = payloadFingerprint;
    const saveMutation = useMutation({
        mutationFn: ({ entries: nextEntries, submit }) => saveCompetitionPracticalJudgeTask(selected.assignmentId, nextEntries, submit),
        onSuccess: (result, variables) => {
            if (variables.submit) {
                setDirty(false);
                setSyncState('saved');
                if (result.scoreSheetStatus === 'submitted_locked') {
                    localStorage.removeItem(localDraftKey(currentUser?.id, result.assignmentId));
                    message.success('本题已提交，本场评分已锁定');
                }
                else {
                    message.success('本题评分已提交');
                }
            }
            else if (variables.fingerprint === payloadFingerprintRef.current) {
                setDirty(false);
                setSyncState('saved');
            }
            queryClient.setQueryData(['competition-practical-judge-tasks'], (current) => (current ?? []).map((task) => (String(task.assignmentId) === String(result.assignmentId) ? result : task)));
        },
        onError: (error, variables) => {
            setSyncState(online ? 'error' : 'offline');
            if (variables.submit)
                message.error(errorMessage(error, '评分提交失败'));
        },
    });
    const confirmStartMutation = useMutation({
        mutationFn: () => confirmCompetitionPracticalJudgeStart(selected.assignmentId),
        onSuccess: (result) => {
            patchTaskCache(queryClient, result);
            message.success(result.startStatus === 'ready'
                ? '三名裁判均已确认，请等待组长开始考试'
                : '已确认选手与试卷，正在等待其他裁判');
        },
        onError: (error) => message.error(errorMessage(error, '开考确认失败，请刷新后重试')),
    });
    const startSessionMutation = useMutation({
        mutationFn: () => startCompetitionPracticalJudgeSession(selected.assignmentId),
        onSuccess: (result) => {
            patchTaskCache(queryClient, result);
            message.success('考试已开始，三台裁判设备已同步解锁评分');
        },
        onError: (error) => message.error(errorMessage(error, '开始考试失败，请检查三名裁判是否均已确认')),
    });
    const completeSessionMutation = useMutation({
        mutationFn: () => completeCompetitionPracticalJudgeSession(selected.assignmentId),
        onSuccess: (result) => {
            patchTaskCache(queryClient, result);
            message.success('实操成绩已由组长确认提交，考试已完成');
        },
        onError: (error) => message.error(errorMessage(error, '完成考试失败，请检查三名裁判评分状态')),
    });
    const selectStageMutation = useMutation({
        mutationFn: (stageId) => selectCompetitionPracticalJudgeActiveStage(stageId),
        onSuccess: (result) => {
            queryClient.setQueryData(['competition-practical-judge-tasks'], result);
        },
        onError: (error, stageId) => {
            if (serverActiveStageId)
                setActiveStageId(serverActiveStageId);
            message.error(errorMessage(error, '切换赛段任务失败，请刷新后重试'));
            const fallback = tasks.find((task) => taskStageId(task) === serverActiveStageId)
                || tasks.find((task) => taskStageId(task) !== stageId);
            if (fallback) {
                hydratedAssignmentRef.current = undefined;
                setSelectedId(String(fallback.assignmentId));
            }
        },
    });
    useEffect(() => {
        if (selectStageMutation.isPending)
            return;
        if (serverActiveStageId && serverActiveStageId !== activeStageId) {
            setActiveStageId(serverActiveStageId);
        }
    }, [activeStageId, selectStageMutation.isPending, serverActiveStageId]);
    useEffect(() => {
        if (!dirty || !selected)
            return;
        localStorage.setItem(localDraftKey(currentUser?.id, selected.assignmentId), JSON.stringify(scores));
    }, [currentUser?.id, dirty, scores, selected]);
    useEffect(() => {
        if (!dirty || !selected || !scoringStarted || questionLocked || locked || !activeRows.length)
            return;
        if (!online) {
            setSyncState('offline');
            return;
        }
        setSyncState('syncing');
        const timer = window.setTimeout(() => {
            saveMutation.mutate({ entries, fingerprint: payloadFingerprint, submit: false });
        }, 900);
        return () => window.clearTimeout(timer);
        // saveMutation is intentionally omitted: its object identity changes across renders.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dirty, entries, locked, online, payloadFingerprint, activeRows.length, questionLocked, scoringStarted, selected?.assignmentId]);
    const activeQuestionScore = activeRows.reduce((sum, row) => sum + (scores[row.key]?.score ?? rowDefault(row.item).score), 0);
    const totalScore = questions.reduce((sum, question) => {
        if (question.paperQuestionId === activeQuestion?.paperQuestionId) {
            return sum + activeQuestionScore;
        }
        const progress = questionProgressOf(selected, question.paperQuestionId);
        return sum + Number(progress?.score ?? 0);
    }, 0);
    const submittedQuestionCount = questions.filter((question) => (questionStatusOf(selected, question.paperQuestionId) === 'submitted')).length;
    const fullScore = Number(selected?.assessmentSheet?.totalScore ?? 0);
    const activeQuestionFullScore = activeRows.reduce((sum, row) => sum + Number(row.item.maxScore || 0), 0);
    function selectQuestion(nextId) {
        if (nextId === String(activeQuestion?.paperQuestionId ?? ''))
            return;
        if (dirty && selected && scoringStarted && !questionLocked && online && entries.length) {
            saveMutation.mutate({
                entries,
                fingerprint: payloadFingerprint,
                submit: false,
            });
        }
        setDirty(false);
        setActiveQuestionId(nextId);
    }
    function updateScore(key, item, next) {
        setScores((current) => ({
            ...current,
            [key]: { ...(current[key] ?? rowDefault(item)), ...next },
        }));
        setDirty(true);
        setSyncState(online ? 'syncing' : 'offline');
    }
    function adjust(row, delta) {
        const current = scores[row.key] ?? rowDefault(row.item);
        const maxScore = Number(row.item.maxScore || 0);
        if (row.item.scoringMethod === 'additive') {
            const score = Math.min(maxScore, Math.max(0, current.score + delta));
            updateScore(row.key, row.item, { score: Number(score.toFixed(2)), deduction: 0 });
            return;
        }
        const maxDeduction = Number(row.item.maxDeduction ?? maxScore);
        const deduction = Math.min(maxDeduction, Math.max(0, current.deduction + delta));
        updateScore(row.key, row.item, {
            deduction: Number(deduction.toFixed(2)),
            score: Number(Math.max(0, maxScore - deduction).toFixed(2)),
        });
    }
    async function handleLogout() {
        try {
            await logout();
        }
        catch {
            // 后端退出失败时仍清理本机登录态。
        }
        finally {
            clearAuth();
            window.location.assign('/competition-practical-judge');
        }
    }
    function switchStage(nextId) {
        const stageId = String(nextId);
        if (stageId === resolvedStageId || selectStageMutation.isPending)
            return;
        setActiveStageId(stageId);
        const nextTask = tasks.find((task) => taskStageId(task) === stageId);
        if (nextTask && String(nextTask.assignmentId) !== selectedId) {
            hydratedAssignmentRef.current = undefined;
            setSelectedId(String(nextTask.assignmentId));
        }
        selectStageMutation.mutate(stageId);
    }
    function renderTaskItem(task) {
        const status = taskStatusText(task);
        return (_jsx(List.Item, { className: String(task.assignmentId) === selectedId ? 'selected' : '', onClick: () => {
                hydratedAssignmentRef.current = undefined;
                setSelectedId(String(task.assignmentId));
            }, children: _jsxs("div", { className: "assessor-task-card", children: [_jsxs(Space, { wrap: true, children: [_jsx(Tag, { color: status === '已提交' ? 'success' : status === '已退回' ? 'error' : 'blue', children: status }), _jsx(Tag, { color: task.judgeRole === 'leader' ? 'gold' : 'default', children: task.judgeRole === 'leader' ? '组长' : '组员' }), _jsx("strong", { children: contestantLabel(task) }), _jsx("span", { children: task.workstationCode ? `工位 ${task.workstationCode}` : '' })] }), _jsx(Typography.Text, { children: task.assessmentSheet?.title || '实操考评表' }), _jsxs("small", { children: [task.assessmentSheet?.questionCount || 0, " \u9898 \u00B7 ", formatScore(Number(task.totalScore ?? 0)), "/", formatScore(Number(task.assessmentSheet?.totalScore ?? 0)), " \u5206"] })] }) }));
    }
    const detail = activeQuestion?.detail;
    const questionContent = detailText(detail, 'assessmentRequirementsHtml', 'operationContent') || '无补充题目说明';
    const candidatePrepare = detailText(detail, 'candidatePrepareHtml', 'studentPrepare') || '-';
    const equipmentPrepare = detailText(detail, 'examRoomPrepareHtml', 'equipmentPrepare') || '-';
    return (_jsxs("div", { className: "assessor-tablet-shell", children: [_jsxs("header", { className: "assessor-tablet-header", children: [_jsxs("div", { className: "assessor-tablet-header-main", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 3, children: "\u7ADE\u8D5B\u5B9E\u64CD\u88C1\u5224" }), _jsxs(Typography.Text, { children: [currentUser?.displayName || '裁判员', "\uFF0C\u8BF7\u72EC\u7ACB\u5B8C\u6210\u672C\u4EBA\u8BC4\u5206"] })] }), _jsxs(Space, { wrap: true, children: [_jsx(Tag, { icon: online ? _jsx(WifiOutlined, {}) : _jsx(CloudOutlined, {}), color: online ? 'success' : 'warning', children: online ? '在线' : '短时断网' }), _jsx(Tag, { icon: _jsx(CloudSyncOutlined, {}), color: syncState === 'error' ? 'error' : 'processing', children: syncText(syncState) }), _jsx(Tag, { color: realtimeConnected ? 'success' : 'default', children: realtimeConnected ? '实时连接' : '轮询同步' }), _jsx(Button, { icon: _jsx(LogoutOutlined, {}), onClick: handleLogout, children: "\u5207\u6362\u8D26\u53F7" })] })] }), isGroupLeaderUser && stages.length > 0 ? (_jsxs("div", { className: "assessor-stage-switch", "data-testid": "judge-stage-switch", children: [_jsx(Typography.Text, { type: "secondary", children: "\u8D5B\u6BB5\u4EFB\u52A1" }), _jsx(Select, { size: "large", className: "assessor-stage-select", classNames: { popup: { root: 'assessor-stage-dropdown' } }, value: resolvedStageId, optionLabelProp: "label", options: stages.map((stage) => ({
                                    label: stage.name,
                                    value: stage.id,
                                    title: stage.name,
                                })), onChange: switchStage, loading: selectStageMutation.isPending, disabled: !online || selectStageMutation.isPending, placeholder: "\u8BF7\u9009\u62E9\u8D5B\u6BB5\u4EFB\u52A1" })] })) : resolvedStageId ? (_jsxs("div", { className: "assessor-stage-switch", "data-testid": "judge-stage-current", children: [_jsx(Typography.Text, { type: "secondary", children: "\u5F53\u524D\u8D5B\u6BB5\u4EFB\u52A1" }), _jsx(Tag, { color: "blue", children: resolvedStageName })] })) : null] }), _jsxs("main", { className: `assessor-tablet-main ${selected ? 'has-selection' : ''}`, children: [_jsxs("aside", { className: "assessor-task-list", children: [_jsxs("div", { className: "assessor-task-list-title", children: [_jsx(Typography.Title, { level: 4, children: "\u5F53\u524D\u8D5B\u6BB5\u9009\u624B" }), _jsx(Button, { size: "large", "aria-label": "\u5237\u65B0\u8BC4\u5206\u4EFB\u52A1", icon: _jsx(ReloadOutlined, {}), onClick: () => tasksQuery.refetch(), disabled: !online })] }), _jsx(Spin, { spinning: tasksQuery.isFetching, children: _jsx(List, { dataSource: visibleTasks, locale: { emptyText: _jsx(Empty, { description: "\u5F53\u524D\u8D5B\u6BB5\u6682\u65E0\u8BC4\u5206\u4EFB\u52A1" }) }, renderItem: renderTaskItem }) })] }), _jsx("section", { className: "assessor-score-workspace", children: !selected ? _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u8BC4\u5206\u4EFB\u52A1" }) : !questions.length ? (_jsx(Empty, { description: "\u5F53\u524D\u8003\u8BC4\u8868\u6682\u65E0\u8BC4\u5206\u9898\u76EE" })) : activeQuestion ? (_jsxs(_Fragment, { children: [_jsx(Button, { className: "assessor-mobile-back", icon: _jsx(LeftOutlined, {}), onClick: () => setSelectedId(undefined), children: "\u8FD4\u56DE\u4EFB\u52A1" }), _jsxs("div", { className: "assessor-candidate-bar", children: [_jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: taskScopeLabel(selected) !== '未分赛段'
                                                        ? `当前赛段：${taskScopeLabel(selected)}`
                                                        : '当前选手' }), _jsx(Typography.Title, { level: 3, children: contestantLabel(selected) }), _jsxs(Typography.Text, { children: [selected.workstationCode ? `工位 ${selected.workstationCode}` : '工位未设置', " / ", selected.assessmentSheet.title] })] }), _jsxs("div", { className: "assessor-exam-stats", children: [_jsx(Tag, { color: isGroupLeader ? 'gold' : 'blue', children: isGroupLeader ? '裁判组组长' : '裁判组组员' }), _jsxs("div", { className: "assessor-live-score", children: [_jsx("span", { children: "\u6574\u573A\u5B9E\u65F6\u5F97\u5206" }), _jsxs("strong", { children: [formatScore(totalScore), _jsxs("small", { children: ["/", formatScore(fullScore)] })] })] }), _jsxs("div", { children: [_jsx("span", { children: "\u9898\u76EE\u63D0\u4EA4" }), _jsxs("strong", { children: [submittedQuestionCount, "/", questions.length] })] })] })] }), !online && (_jsx(Alert, { type: "warning", showIcon: true, message: "\u5F53\u524D\u7F51\u7EDC\u4E0D\u53EF\u7528", description: "\u8BC4\u5206\u5185\u5BB9\u4F1A\u4FDD\u7559\u5728\u5F53\u524D\u9875\u9762\uFF0C\u6062\u590D\u8054\u7F51\u540E\u81EA\u52A8\u540C\u6B65\uFF1B\u8054\u7F51\u524D\u4E0D\u80FD\u63D0\u4EA4\u3002" })), syncState === 'error' && (_jsx(Alert, { type: "error", showIcon: true, message: "\u8BC4\u5206\u8349\u7A3F\u540C\u6B65\u5931\u8D25", description: "\u8BF7\u68C0\u67E5\u7F51\u7EDC\u540E\u91CD\u8BD5\uFF0C\u6216\u70B9\u51FB\u5237\u65B0\u6309\u94AE\u91CD\u65B0\u52A0\u8F7D\u4EFB\u52A1\u3002", action: _jsx(Button, { onClick: () => setDirty(true), children: "\u91CD\u65B0\u540C\u6B65" }) })), (selected.startStatus === 'waiting' || selected.startStatus === 'ready') && (_jsxs("div", { className: "assessor-start-gate", children: [_jsx(Typography.Title, { level: 3, children: "\u7EDF\u4E00\u5F00\u8003\u786E\u8BA4" }), _jsx(Typography.Paragraph, { children: "\u8BF7\u4E09\u540D\u88C1\u5224\u5206\u522B\u5728\u5404\u81EA\u5E73\u677F\u6838\u5BF9\u5F53\u524D\u9009\u624B\u3001\u5DE5\u4F4D\u548C\u8BD5\u5377\uFF1B\u5168\u90E8\u786E\u8BA4\u540E\uFF0C\u4EC5\u7EC4\u957F\u53EF\u4EE5\u7EDF\u4E00\u5F00\u8003\u3002" }), _jsxs("div", { className: "assessor-start-steps", children: [_jsxs(Tag, { color: startSessionReady ? 'success' : 'processing', children: ["\u88C1\u5224\u786E\u8BA4 ", selected.confirmedJudges, "/", selected.requiredJudges] }), _jsx(Tag, { color: "success", children: "\u8BD5\u5377\u5DF2\u5339\u914D" }), _jsx(Tag, { color: isGroupLeader ? 'gold' : 'blue', children: isGroupLeader ? '当前设备：组长' : '当前设备：组员' })] }), isGroupLeader && (_jsx(Button, { type: "primary", size: "large", disabled: !startSessionReady || !online, loading: startSessionMutation.isPending, onClick: () => startSessionMutation.mutate(), children: startSessionReady ? '组长开始考试' : '等待三台设备确认' })), selected.currentJudgeConfirmed ? (_jsx(Button, { size: "large", disabled: true, children: "\u672C\u8BBE\u5907\u5DF2\u786E\u8BA4" })) : (_jsx(Button, { size: "large", disabled: !online, loading: confirmStartMutation.isPending, onClick: () => confirmStartMutation.mutate(), children: "\u6211\u5DF2\u6838\u5BF9\u9009\u624B\u4E0E\u8BD5\u5377\uFF0C\u786E\u8BA4\u5C31\u7EEA" }))] })), awaitingLeaderCompletion && (_jsx(Alert, { type: scoreDifferenceExceeded ? 'error' : isGroupLeader ? 'warning' : 'info', showIcon: true, message: scoreDifferenceExceeded
                                        ? `裁判评分分差异常：${formatScore(Number(selected?.actualScoreDifference ?? 0))} 分，允许 ${formatScore(Number(selected?.scoreDifferenceLimit ?? 0))} 分`
                                        : isGroupLeader ? '三名裁判均已提交，请完成考试' : '三名裁判均已提交，等待组长完成考试', description: scoreDifferenceExceeded
                                        ? '请在实操比赛控制台核对三名裁判原始分，并将需要复核的裁判评分退回重评。分差异常解除前不能完成考试。'
                                        : isGroupLeader
                                            ? '确认后系统将按题目快照中的评分要求汇总三名裁判原始分并提交实操成绩。'
                                            : '你的逐题评分已经锁定，最终实操成绩由裁判组组长确认提交。', action: isGroupLeader ? (_jsx(Button, { type: "primary", disabled: !online || scoreDifferenceExceeded, loading: completeSessionMutation.isPending, onClick: () => Modal.confirm({
                                            title: '确认完成考试并提交实操成绩？',
                                            content: '请确认三名裁判均已完成独立评分。提交后将生成本场实操汇总成绩。',
                                            okText: '完成考试',
                                            cancelText: '继续检查',
                                            onOk: () => completeSessionMutation.mutateAsync(),
                                        }), children: "\u5B8C\u6210\u8003\u8BD5" })) : undefined })), _jsx("div", { className: "assessor-question-nav", children: questions.map((question, index) => {
                                        const active = question.paperQuestionId === activeQuestion.paperQuestionId;
                                        const status = questionStatusOf(selected, question.paperQuestionId);
                                        const questionRows = rows.filter((row) => row.question.paperQuestionId === question.paperQuestionId);
                                        const questionScore = active
                                            ? activeQuestionScore
                                            : Number(questionProgressOf(selected, question.paperQuestionId)?.score ?? 0);
                                        const questionFullScore = questionRows.reduce((sum, row) => sum + Number(row.item.maxScore || 0), 0);
                                        return (_jsxs("button", { type: "button", className: active ? 'active' : '', onClick: () => selectQuestion(String(question.paperQuestionId)), children: [_jsxs("span", { children: ["\u8BD5\u9898 ", index + 1] }), _jsx("strong", { children: plain(question.title) || `实操题 ${index + 1}` }), _jsxs("small", { children: [formatScore(questionScore), " / ", formatScore(questionFullScore), " \u5206"] }), _jsx(Tag, { color: status === 'submitted' ? 'success' : status === 'returned' ? 'error' : 'blue', children: questionStatusText(status) })] }, String(question.paperQuestionId)));
                                    }) }), _jsxs("div", { className: "assessor-question-card", children: [_jsxs("div", { className: "assessor-question-title-row", children: [_jsx(Typography.Title, { level: 3, children: plain(activeQuestion.title) || '实操题' }), _jsxs("div", { className: "assessor-question-score", children: [_jsx("span", { children: "\u672C\u9898\u5F97\u5206" }), _jsxs("strong", { children: [formatScore(activeQuestionScore), " / ", formatScore(activeQuestionFullScore)] })] })] }), _jsx(Typography.Paragraph, { style: { whiteSpace: 'pre-line' }, children: questionContent }), _jsxs("div", { className: "assessor-prepare-grid", children: [_jsxs("div", { children: [_jsx("strong", { children: "\u8003\u751F\u51C6\u5907" }), _jsx("span", { children: candidatePrepare })] }), _jsxs("div", { children: [_jsx("strong", { children: "\u8BBE\u5907\u8BBE\u65BD" }), _jsx("span", { children: equipmentPrepare })] })] })] }), _jsx("div", { className: "assessor-score-items", children: activeRows.map((row) => {
                                        const value = scores[row.key] ?? rowDefault(row.item);
                                        const step = Number(row.item.deductionStep ?? 0.5);
                                        const additive = row.item.scoringMethod === 'additive';
                                        const maxAdjustment = additive
                                            ? Number(row.item.maxScore || 0)
                                            : Number(row.item.maxDeduction ?? row.item.maxScore ?? 0);
                                        const currentAdjustment = additive ? value.score : value.deduction;
                                        return (_jsxs("article", { className: "assessor-score-item", children: [_jsxs("div", { className: "assessor-score-item-head", children: [_jsxs("div", { children: [_jsx("span", { className: "assessor-item-no", children: row.item.itemNo }), _jsx(Typography.Title, { level: 4, children: plain(row.item.itemName || row.item.elementHtml) || `评分项 ${row.item.itemNo}` })] }), _jsxs("strong", { children: [formatScore(value.score), " / ", formatScore(Number(row.item.maxScore)), " \u5206"] })] }), _jsx(Typography.Paragraph, { style: { whiteSpace: 'pre-line' }, children: plain(row.item.criterion || row.item.standardHtml) || '无补充评分标准' }), _jsxs("div", { className: "assessor-deduction-control", children: [_jsxs(Button, { icon: _jsx(MinusOutlined, {}), disabled: !scoringStarted || sessionCompleted || questionLocked || currentAdjustment <= 0, onClick: () => adjust(row, -step), children: [additive ? '减' : '撤销', " ", formatScore(step), " \u5206"] }), _jsxs("div", { children: [_jsx("span", { children: additive ? '当前得分' : '当前扣分' }), _jsx("strong", { children: formatScore(currentAdjustment) })] }), _jsxs(Button, { danger: !additive, type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !scoringStarted || sessionCompleted || questionLocked || currentAdjustment >= maxAdjustment, onClick: () => adjust(row, step), children: [additive ? '加' : '扣', " ", formatScore(step), " \u5206"] })] }), _jsx(Input, { size: "large", value: value.remark, disabled: !scoringStarted || sessionCompleted || questionLocked, onChange: (event) => updateScore(row.key, row.item, { remark: event.target.value }), placeholder: "\u6263\u5206\u8BF4\u660E\uFF08\u9009\u586B\uFF09" })] }, row.key));
                                    }) }), _jsxs("footer", { className: "assessor-submit-bar", children: [_jsxs("div", { children: [_jsx("span", { children: "\u6574\u573A\u5408\u8BA1" }), _jsxs("strong", { children: [formatScore(totalScore), " / ", formatScore(fullScore), " \u5206"] })] }), _jsx(Button, { type: "primary", size: "large", icon: _jsx(CheckCircleOutlined, {}), disabled: !scoringStarted || sessionCompleted || questionLocked || !online || !activeRows.length || syncState === 'syncing', loading: saveMutation.isPending, onClick: () => Modal.confirm({
                                                title: '确认提交本题评分？',
                                                content: '提交后本题将锁定，只有管理员退回后才能重新评分。',
                                                okText: '确认提交',
                                                cancelText: '继续检查',
                                                onOk: () => saveMutation.mutateAsync({
                                                    entries,
                                                    fingerprint: payloadFingerprint,
                                                    submit: true,
                                                }),
                                            }), children: questionLocked ? '本题已提交' : online ? '提交本题' : '联网后提交' })] })] })) : _jsx(Empty, { description: "\u8BC4\u5206\u4EFB\u52A1\u52A0\u8F7D\u5931\u8D25" }) })] })] }));
}
function patchTaskCache(queryClient, task) {
    queryClient.setQueryData(['competition-practical-judge-tasks'], (current) => (current ?? []).map((item) => (String(item.assignmentId) === String(task.assignmentId) ? task : item)));
}

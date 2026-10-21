import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { PlayCircleOutlined, ReloadOutlined, SettingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Empty, Input, Modal, Progress, Result, Select, Space, Statistic, Table, Tag, Typography, message, } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { getCompetitionPracticalConsole, createCompetitionPracticalRealtimeTicket, initializeCompetitionPracticalConsole, listCompetitionEventWorkTypeLevels, pageCompetitionStages, returnCompetitionPracticalScoreForRescore, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import './practical-console-expand.css';
function idValue(value) {
    return value == null ? '' : String(value);
}
function errorText(error, fallback) {
    if (error && typeof error === 'object' && 'response' in error) {
        const msg = error.response?.data?.message;
        if (typeof msg === 'string' && msg.trim())
            return msg;
    }
    return error instanceof Error && error.message.trim() ? error.message : fallback;
}
function formatScore(value) {
    if (value === undefined || value === null || value === '')
        return undefined;
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric.toFixed(1) : undefined;
}
function sheetStatusMeta(status) {
    if (status === 'submitted_locked' || status === 'submitted')
        return { color: 'success', text: '已提交' };
    if (status === 'draft' || status === 'scoring')
        return { color: 'processing', text: '评分中' };
    if (status === 'unlocked_for_rescore' || status === 'returned')
        return { color: 'error', text: '已退回' };
    if (status === 'unopened' || !status)
        return { color: 'default', text: '未打开' };
    return { color: 'processing', text: status };
}
export function groupConsoleContestants(tasks) {
    const groups = new Map();
    for (const task of tasks) {
        const contestantId = idValue(task.contestantMemberId);
        const current = groups.get(contestantId) ?? [];
        current.push(task);
        groups.set(contestantId, current);
    }
    return Array.from(groups.entries()).map(([contestantMemberId, referees]) => {
        const ordered = [...referees].sort((left, right) => (idValue(left.refereeMemberId).localeCompare(idValue(right.refereeMemberId))));
        const submittedSheets = ordered.filter((item) => item.scoreSheetStatus === 'submitted_locked').length;
        const questionCount = Math.max(0, ...ordered.map((item) => item.questions?.length ?? 0));
        const completedQuestions = questionCount
            ? Array.from({ length: questionCount }, (_, index) => index).filter((index) => (ordered.length > 0 && ordered.every((item) => questionSubmitted(item.questions?.[index], item.scoreSheetStatus)))).length
            : 0;
        const allSubmitted = ordered.length > 0 && submittedSheets === ordered.length;
        const totalScore = allSubmitted && ordered.every((item) => item.totalScore != null && item.totalScore !== '')
            ? ordered.reduce((sum, item) => sum + Number(item.totalScore), 0) / ordered.length
            : null;
        const scoreDifferenceStatus = ordered[0]?.scoreDifferenceStatus ?? 'waiting';
        const practicalScoreCompleted = ordered.some((item) => item.practicalScoreCompleted);
        const status = scoreDifferenceStatus === 'exceeded' ? 'abnormal'
            : practicalScoreCompleted && allSubmitted ? 'completed'
                : allSubmitted ? 'awaiting_leader' : 'scoring';
        return {
            key: contestantMemberId,
            contestantMemberId,
            contestantNo: ordered[0]?.contestantNo,
            contestantName: ordered[0]?.contestantName,
            workstationCode: ordered[0]?.workstationCode,
            submittedSheets,
            totalSheets: ordered.length,
            completedQuestions,
            totalQuestions: questionCount,
            totalScore,
            scoreDifferenceLimit: ordered[0]?.scoreDifferenceLimit == null
                ? null : Number(ordered[0].scoreDifferenceLimit),
            actualScoreDifference: ordered[0]?.actualScoreDifference == null
                ? null : Number(ordered[0].actualScoreDifference),
            scoreDifferenceStatus,
            status,
            referees: ordered,
        };
    });
}
export function CompetitionPracticalConsolePage({ eventId, eventWorkTypeId, eventWorkTypeLevelCode, initialStageId, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const token = useAuthStore((state) => state.token);
    const tenantId = useAuthStore((state) => state.tenantId);
    const canView = permissions.includes('competition:practical-console:view');
    const canManage = permissions.includes('competition:practical-console:manage');
    const queryClient = useQueryClient();
    const [selectedStageId, setSelectedStageId] = useState(initialStageId);
    const [expandedRowKeys, setExpandedRowKeys] = useState([]);
    const [returnTask, setReturnTask] = useState(null);
    const [returnReason, setReturnReason] = useState('');
    const levelsQuery = useQuery({
        queryKey: ['competition-practical-console-levels', eventId, eventWorkTypeId],
        queryFn: () => listCompetitionEventWorkTypeLevels(eventId, eventWorkTypeId),
        enabled: canView,
    });
    const levelId = idValue(levelsQuery.data?.find((item) => item.levelCode === eventWorkTypeLevelCode)?.id);
    const stagesQuery = useQuery({
        queryKey: ['competition-practical-console-stages', eventId, eventWorkTypeId, eventWorkTypeLevelCode],
        queryFn: () => pageCompetitionStages(eventId, {
            page: 1,
            pageSize: 500,
            eventWorkTypeId,
            eventWorkTypeLevelCode,
        }),
        enabled: canView,
    });
    const stages = stagesQuery.data?.items ?? [];
    useEffect(() => {
        if (!stages.length)
            return setSelectedStageId(undefined);
        if (!selectedStageId || !stages.some((item) => idValue(item.id) === selectedStageId)) {
            setSelectedStageId(initialStageId && stages.some((item) => idValue(item.id) === initialStageId)
                ? initialStageId : idValue(stages[0].id));
        }
    }, [initialStageId, selectedStageId, stages]);
    const selectedStage = useMemo(() => stages.find((item) => idValue(item.id) === selectedStageId), [selectedStageId, stages]);
    const consoleQuery = useQuery({
        queryKey: ['competition-practical-console', selectedStageId, levelId],
        queryFn: () => getCompetitionPracticalConsole(selectedStageId, levelId),
        enabled: canView && Boolean(selectedStageId && levelId),
        refetchInterval: import.meta.env.MODE === 'test' || !(canView && selectedStageId && levelId)
            ? false
            : 4_000,
        staleTime: 0,
    });
    const initializeMutation = useMutation({
        mutationFn: () => initializeCompetitionPracticalConsole(selectedStageId, levelId),
        onSuccess: (data) => {
            queryClient.setQueryData(['competition-practical-console', selectedStageId, levelId], data);
            message.success('实操正式比赛已初始化，裁判、选手和考评表已生成评分任务');
        },
        onError: (error) => message.error(errorText(error, '初始化失败，请核对选手、裁判和考评表配置')),
    });
    const returnMutation = useMutation({
        mutationFn: () => returnCompetitionPracticalScoreForRescore(returnTask.assignmentId, returnReason.trim()),
        onSuccess: () => {
            message.success('已退回该裁判重评，分差状态将在重新提交后自动更新');
            setReturnTask(null);
            setReturnReason('');
            void queryClient.invalidateQueries({ queryKey: ['competition-practical-console'] });
        },
        onError: (error) => message.error(errorText(error, '退回重评失败，请刷新后重试')),
    });
    useEffect(() => {
        if (!token || !tenantId)
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
                socket.onmessage = () => {
                    if (refetchTimer != null)
                        window.clearTimeout(refetchTimer);
                    refetchTimer = window.setTimeout(() => {
                        void queryClient.refetchQueries({ queryKey: ['competition-practical-console'] });
                    }, 50);
                };
                socket.onclose = () => {
                    if (disposed)
                        return;
                    const delay = Math.min(8_000, 400 * 2 ** attempt);
                    attempt += 1;
                    retryTimer = window.setTimeout(connect, delay);
                };
            }).catch(() => {
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
    }, [queryClient, tenantId, token]);
    const contestantRows = useMemo(() => groupConsoleContestants(consoleQuery.data?.tasks ?? []), [consoleQuery.data?.tasks]);
    const contestantKeys = contestantRows.map((row) => row.key).join(',');
    useEffect(() => {
        const keys = contestantKeys ? contestantKeys.split(',') : [];
        setExpandedRowKeys((current) => {
            if (keys.length === 1)
                return keys;
            return current.filter((key) => keys.includes(String(key)));
        });
    }, [contestantKeys]);
    const submittedContestantCount = contestantRows.filter((row) => row.totalSheets > 0 && row.submittedSheets === row.totalSheets).length;
    const abnormalContestants = contestantRows.filter((row) => row.status === 'abnormal');
    if (!canView)
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u5B9E\u64CD\u6BD4\u8D5B\u63A7\u5236\u53F0\u67E5\u770B\u6743\u9650" });
    const state = consoleQuery.data;
    const contestantColumns = [
        { title: '工位', dataIndex: 'workstationCode', width: 90, render: (value) => value || '-' },
        { title: '选手编号', dataIndex: 'contestantNo', width: 160, render: (value, row) => value || row.contestantMemberId },
        { title: '选手', dataIndex: 'contestantName', width: 140, render: (value, row) => value || row.contestantMemberId },
        {
            title: '评分提交',
            width: 180,
            render: (_, row) => (_jsx(Progress, { percent: row.totalSheets ? Math.round(row.submittedSheets * 100 / row.totalSheets) : 0, format: () => `${row.submittedSheets}/${row.totalSheets}` })),
        },
        {
            title: '试题完成',
            width: 120,
            render: (_, row) => (row.totalQuestions ? `${row.completedQuestions}/${row.totalQuestions}` : '-'),
        },
        {
            title: '实操总分',
            width: 130,
            render: (_, row) => formatScore(row.totalScore) ?? '待三人齐交',
        },
        {
            title: '分差预警',
            width: 170,
            render: (_, row) => {
                if (row.scoreDifferenceStatus === 'exceeded') {
                    return _jsxs(Tag, { color: "error", children: ["\u5206\u5DEE ", formatScore(row.actualScoreDifference), " \uFF1E ", formatScore(row.scoreDifferenceLimit)] });
                }
                if (row.scoreDifferenceStatus === 'normal') {
                    return _jsxs(Tag, { color: "success", children: ["\u5206\u5DEE ", formatScore(row.actualScoreDifference), " / ", formatScore(row.scoreDifferenceLimit)] });
                }
                if (row.scoreDifferenceStatus === 'unrestricted')
                    return _jsx(Tag, { children: "\u672A\u9650\u5236" });
                return _jsx(Tag, { children: "\u5F85\u81F3\u5C11 2 \u4EBA\u63D0\u4EA4" });
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (value) => (_jsx(Tag, { color: value === 'completed' ? 'success' : value === 'abnormal' ? 'error' : 'processing', children: value === 'completed' ? '已完成'
                    : value === 'abnormal' ? '分差异常'
                        : value === 'awaiting_leader' ? '待组长确认' : '评分中' })),
        },
    ];
    return _jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%', marginTop: 16 }, children: [_jsx(Card, { size: "small", bordered: false, children: _jsxs(Space, { wrap: true, children: [_jsx(SettingOutlined, {}), _jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u8D5B\u6BB5" }), _jsx(Select, { value: selectedStageId, style: { width: 320 }, loading: stagesQuery.isFetching, placeholder: "\u8BF7\u9009\u62E9\u9700\u8981\u63A7\u5236\u7684\u5B9E\u64CD\u8D5B\u6BB5", options: stages.map((stage) => ({ value: idValue(stage.id), label: stage.name })), onChange: setSelectedStageId }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => { void stagesQuery.refetch(); void consoleQuery.refetch(); }, children: "\u5237\u65B0" }), state && _jsx(Tag, { color: state.scoringTaskCount > 0 ? 'success' : 'warning', children: state.scoringTaskCount > 0 ? '已初始化' : '未初始化' })] }) }), !selectedStage ? _jsx(Card, { children: _jsx(Empty, { description: "\u5F53\u524D\u5DE5\u79CD\u7B49\u7EA7\u4E0B\u6682\u65E0\u8D5B\u6BB5" }) }) : null, consoleQuery.isError ? _jsx(Alert, { type: "error", showIcon: true, message: errorText(consoleQuery.error, '控制台状态加载失败') }) : null, selectedStage && state ? _jsxs(Card, { title: _jsxs(Space, { children: [_jsxs("span", { children: [selectedStage.name, " \u00B7 \u5B9E\u64CD\u8BC4\u5206\u4EFB\u52A1"] }), _jsx(Tag, { children: state.status })] }), extra: state.scoringTaskCount === 0 && canManage ? _jsx(Button, { type: "primary", icon: _jsx(PlayCircleOutlined, {}), loading: initializeMutation.isPending, onClick: () => initializeMutation.mutate(), children: "\u521D\u59CB\u5316\u6B63\u5F0F\u6BD4\u8D5B" }) : null, children: [state.status === 'not_initialized' ? _jsx(Alert, { type: "warning", showIcon: true, message: "\u5F53\u524D\u5B9E\u64CD\u6BD4\u8D5B\u5C1A\u672A\u521D\u59CB\u5316", description: "\u70B9\u51FB\u201C\u521D\u59CB\u5316\u6B63\u5F0F\u6BD4\u8D5B\u201D\u540E\uFF0C\u7CFB\u7EDF\u4F1A\u6821\u9A8C\u5DF2\u786E\u8BA4\u7684\u8D5B\u6BB5\u7F16\u6392\u3001\u9009\u624B\u8BD5\u5377\u4E0E\u8003\u8BC4\u8868\u3001\u4E09\u540D\u88C1\u5224\u914D\u7F6E\uFF0C\u5E76\u81EA\u52A8\u51C6\u5907\u6BD4\u8D5B\u8FD0\u884C\u6570\u636E\u3002" }) : null, _jsxs(Space, { size: 36, wrap: true, style: { marginTop: 16 }, children: [_jsx(Statistic, { title: "\u6BD4\u8D5B\u573A\u6B21", value: state.sessionCount }), _jsx(Statistic, { title: "\u53C2\u8D5B\u9009\u624B", value: state.contestantCount }), _jsx(Statistic, { title: "\u8BC4\u5206\u4EFB\u52A1", value: state.scoringTaskCount }), _jsx(Statistic, { title: "\u5DF2\u6253\u5F00\u8BC4\u5206\u5355", value: state.openedSheetCount }), _jsx(Statistic, { title: "\u5DF2\u63D0\u4EA4\u8BC4\u5206\u5355", value: state.submittedSheetCount })] }), state.scoringTaskCount > 0 ? _jsx(Alert, { style: { marginTop: 20 }, type: "success", showIcon: true, message: "\u8BC4\u5206\u4EFB\u52A1\u5DF2\u51BB\u7ED3", description: "\u6BCF\u6761\u4EFB\u52A1\u5747\u4FDD\u5B58\u88C1\u5224\u3001\u9009\u624B\u3001\u8003\u8BC4\u8868\u7684\u660E\u786E\u5173\u8054\u3002\u88C1\u5224\u7AEF\u63D0\u4EA4\u540E\uFF0C\u63A7\u5236\u53F0\u7EDF\u8BA1\u4F1A\u5373\u65F6\u5237\u65B0\u3002" }) : null, abnormalContestants.length > 0 ? _jsx(Alert, { style: { marginTop: 16 }, type: "error", showIcon: true, message: `${abnormalContestants.length} 名选手存在裁判评分分差异常`, description: "\u8BF7\u5C55\u5F00\u5F02\u5E38\u9009\u624B\uFF0C\u6838\u5BF9\u5404\u88C1\u5224\u539F\u59CB\u5206\uFF0C\u5E76\u5C06\u9700\u8981\u590D\u6838\u7684\u88C1\u5224\u8BC4\u5206\u9000\u56DE\u91CD\u8BC4\u3002\u5206\u5DEE\u6062\u590D\u5230\u9898\u76EE\u5141\u8BB8\u8303\u56F4\u5185\u4E4B\u524D\uFF0C\u7EC4\u957F\u65E0\u6CD5\u5B8C\u6210\u8003\u8BD5\u3002" }) : null, state.scoringTaskCount > 0 ? _jsxs(_Fragment, { children: [_jsxs(Space, { size: 24, style: { margin: '20px 0 16px' }, children: [_jsx(Progress, { type: "circle", size: 92, percent: contestantRows.length
                                            ? Math.round(submittedContestantCount * 100 / contestantRows.length)
                                            : 0 }), _jsxs(Typography.Text, { children: ["\u9009\u624B\u5DF2\u63D0\u4EA4 ", submittedContestantCount, " / ", contestantRows.length, " \u4EBA"] })] }), _jsx(Table, { rowKey: "key", size: "middle", pagination: contestantRows.length > 20 ? { pageSize: 20 } : false, dataSource: contestantRows, columns: contestantColumns, scroll: { x: 1090 }, expandable: {
                                    expandedRowKeys,
                                    onExpandedRowsChange: (keys) => setExpandedRowKeys([...keys]),
                                    expandedRowRender: (row) => _jsx(RefereeBreakdown, { referees: row.referees, scoreDifferenceExceeded: row.scoreDifferenceStatus === 'exceeded', canManage: canManage, onReturn: (task) => {
                                            setReturnTask(task);
                                            setReturnReason('');
                                        } }),
                                    rowExpandable: (row) => row.referees.length > 0,
                                    expandRowByClick: true,
                                } })] }) : null] }) : null, _jsxs(Modal, { open: Boolean(returnTask), title: "\u9000\u56DE\u88C1\u5224\u8BC4\u5206\u91CD\u8BC4", okText: "\u786E\u8BA4\u9000\u56DE", cancelText: "\u53D6\u6D88", okButtonProps: { disabled: !returnReason.trim() }, confirmLoading: returnMutation.isPending, onCancel: () => {
                    if (returnMutation.isPending)
                        return;
                    setReturnTask(null);
                    setReturnReason('');
                }, onOk: () => returnMutation.mutate(), children: [_jsxs(Typography.Paragraph, { children: ["\u5C06\u9000\u56DE\u88C1\u5224\u201C", returnTask?.refereeName || idValue(returnTask?.refereeMemberId), "\u201D\u7684\u8BC4\u5206\uFF08\u5F53\u524D\u603B\u5206\uFF1A", formatScore(returnTask?.totalScore) ?? '-', "\uFF09\u3002 \u8BE5\u88C1\u5224\u91CD\u65B0\u63D0\u4EA4\u540E\u7CFB\u7EDF\u4F1A\u81EA\u52A8\u590D\u6838\u5206\u5DEE\u3002"] }), _jsx(Input.TextArea, { rows: 3, maxLength: 200, showCount: true, value: returnReason, placeholder: "\u8BF7\u586B\u5199\u9000\u56DE\u539F\u56E0\uFF0C\u4F8B\u5982\uFF1A\u4E0E\u5176\u4ED6\u88C1\u5224\u5206\u5DEE\u8FC7\u5927\uFF0C\u8BF7\u590D\u6838\u7B2C 2 \u9898\u8BC4\u5206", onChange: (event) => setReturnReason(event.target.value) })] })] });
}
function RefereeBreakdown({ referees, scoreDifferenceExceeded, canManage, onReturn, }) {
    const padded = [...referees];
    while (padded.length < 3)
        padded.push(null);
    const submittedScores = referees
        .filter((item) => item.scoreSheetStatus === 'submitted_locked' && item.totalScore != null)
        .map((item) => Number(item.totalScore));
    const minScore = submittedScores.length ? Math.min(...submittedScores) : undefined;
    const maxScore = submittedScores.length ? Math.max(...submittedScores) : undefined;
    return (_jsx("div", { className: "practical-candidate-assessors", children: padded.map((referee, index) => {
            if (!referee) {
                return (_jsx("div", { className: "practical-candidate-assessor practical-candidate-assessor--empty", children: "\u8BE5\u4F4D\u7F6E\u6682\u65E0\u88C1\u5224" }, `empty-${index}`));
            }
            const questions = [...(referee.questions ?? [])].sort((left, right) => left.sortOrder - right.sortOrder);
            const submittedQuestions = questions.filter((item) => questionSubmitted(item, referee.scoreSheetStatus)).length;
            const totalQuestions = questions.length;
            const sheetMeta = sheetStatusMeta(referee.scoreSheetStatus);
            const edgeScore = scoreDifferenceExceeded
                && referee.totalScore != null
                && [minScore, maxScore].includes(Number(referee.totalScore));
            return (_jsxs("div", { className: `practical-candidate-assessor${edgeScore ? ' practical-candidate-assessor--difference-edge' : ''}`, children: [_jsxs("div", { className: "practical-assessor-head", children: [_jsx("span", { className: "assessor-name", children: referee.refereeName || idValue(referee.refereeMemberId) }), _jsx(Tag, { color: sheetMeta.color, children: sheetMeta.text })] }), _jsxs("div", { className: "practical-assessor-progress", children: [_jsx(Progress, { percent: totalQuestions ? Math.round((submittedQuestions * 100) / totalQuestions) : (referee.scoreSheetStatus === 'submitted_locked' ? 100 : 0), size: "small", style: { flex: 1, margin: 0 } }), _jsx("span", { children: totalQuestions ? `${submittedQuestions}/${totalQuestions} 已提交` : sheetMeta.text })] }), _jsx("div", { className: "practical-assessor-tasks", children: questions.length === 0 ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u8BD5\u9898\u660E\u7EC6" })) : questions.map((question) => (_jsx(QuestionRow, { question: question, sheetStatus: referee.scoreSheetStatus }, `${idValue(referee.assignmentId)}-${question.paperQuestionId ?? question.sortOrder}`))) }), _jsxs("div", { className: "practical-assessor-total", children: [_jsx("span", { children: "\u5408\u8BA1\u5206" }), _jsx("strong", { children: formatScore(referee.totalScore) ?? '-' })] }), scoreDifferenceExceeded && canManage && referee.scoreSheetStatus === 'submitted_locked' ? (_jsx(Button, { danger: true, onClick: () => onReturn(referee), children: "\u9000\u56DE\u91CD\u8BC4" })) : null] }, idValue(referee.assignmentId)));
        }) }));
}
function QuestionRow({ question, sheetStatus, }) {
    const status = sheetStatusMeta(question.status || sheetStatus);
    return (_jsxs("div", { className: "practical-assessor-task", children: [_jsx("span", { className: "task-title", children: questionTitle(question) }), _jsx("span", { className: "task-score", children: formatScore(question.score) ?? '-' }), _jsx(Tag, { color: status.color, children: status.text })] }));
}
function questionSubmitted(question, sheetStatus) {
    const status = question?.status || sheetStatus;
    return status === 'submitted' || status === 'submitted_locked';
}
function questionTitle(question) {
    const prefix = `试题${question.sortOrder}`;
    const title = (question.title || '').trim();
    if (!title)
        return prefix;
    if (title === prefix || title.startsWith(`${prefix}、`) || title.startsWith(`${prefix} `))
        return title;
    return `${prefix}、${title}`;
}

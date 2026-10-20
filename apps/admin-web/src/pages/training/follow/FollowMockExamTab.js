import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Checkbox, Descriptions, Drawer, Empty, Form, Input, InputNumber, Modal, Progress, Select, Space, Statistic, Switch, Table, Tag, Typography, message, } from 'antd';
import { DownloadOutlined, PlusOutlined, SyncOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
import { MakeExamPlanForm, buildMakeExamPlanCreatePayload, } from '../../exam/MakeExamPlanForm';
import { usePaneActive } from '../../../layouts/TabKeepAlive';
/** 模拟考实时刷新间隔（毫秒），对齐竞赛理论监考 */
const LIVE_POLL_MS = 5000;
const STATUS_COLOR = {
    draft: 'default',
    published: 'processing',
    closed: 'default',
    pending: 'gold',
    submitted: 'blue',
    graded: 'green',
};
function formatDuration(sec) {
    if (sec == null || sec < 0)
        return '—';
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    if (m <= 0)
        return `${s}秒`;
    return `${m}分${String(s).padStart(2, '0')}秒`;
}
function stripHtml(raw) {
    if (!raw)
        return '';
    return String(raw)
        .replace(/<br\s*\/?>/gi, ' ')
        .replace(/<\/p\s*>/gi, ' ')
        .replace(/<[^>]+>/g, '')
        .replace(/&nbsp;/gi, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}
function formatAnswerJson(raw) {
    if (!raw)
        return '—';
    try {
        const v = JSON.parse(raw);
        if (Array.isArray(v))
            return v.join('、');
        if (typeof v === 'boolean')
            return v ? '正确' : '错误';
        if (v === 'true')
            return '正确';
        if (v === 'false')
            return '错误';
        return String(v);
    }
    catch {
        return raw;
    }
}
function summarizeImport(stats) {
    if (!stats)
        return '';
    const parts = [`已挂接客观题 ${stats.importedCount ?? 0} 道`];
    if ((stats.subjectiveSkipped ?? 0) > 0) {
        parts.push(`跳过非客观题 ${stats.subjectiveSkipped}`);
    }
    if ((stats.excludedByOtherMock ?? 0) > 0) {
        parts.push(`排除他卷占用 ${stats.excludedByOtherMock}`);
    }
    return parts.join('；');
}
export function FollowMockExamTab({ classId }) {
    const queryClient = useQueryClient();
    const [taskId, setTaskId] = useState(null);
    const [answerItem, setAnswerItem] = useState(null);
    const [addPlanOpen, setAddPlanOpen] = useState(false);
    const [pickPlanOpen, setPickPlanOpen] = useState(false);
    const [addForm] = Form.useForm();
    const [pickForm] = Form.useForm();
    const [exporting, setExporting] = useState(false);
    /** 实时刷新：列表 + 打开的答卷详情/学员答卷 */
    const [liveRefresh, setLiveRefresh] = useState(false);
    const [lastLiveAt, setLastLiveAt] = useState(null);
    const modalBusy = addPlanOpen || pickPlanOpen;
    // 标签页不可见时暂停实时轮询（§3.6，单点门禁覆盖下方全部 4 个 useQuery）
    const paneActive = usePaneActive();
    const pollMs = paneActive && liveRefresh && !modalBusy ? LIVE_POLL_MS : false;
    const { data: usedMeta } = useQuery({
        queryKey: ['mock-used-q', classId],
        queryFn: () => trainApi.getMockUsedQuestionIds(classId),
        enabled: classId > 0,
    });
    const { data: bankScope } = useQuery({
        queryKey: ['follow-bank-scope', classId],
        queryFn: () => trainApi.getFollowBankScope(classId),
        enabled: classId > 0,
    });
    const { data: planOptions, isLoading: planOptionsLoading } = useQuery({
        queryKey: ['mock-plan-options', classId],
        queryFn: () => trainApi.listMockPlanOptions(classId),
        enabled: classId > 0 && pickPlanOpen,
    });
    const { data: tasks, isLoading, isFetching: tasksFetching, dataUpdatedAt: tasksUpdatedAt, refetch, } = useQuery({
        queryKey: ['follow-tasks', classId, 'mock'],
        queryFn: () => trainApi.listFollowTasks(classId, 'mock'),
        enabled: classId > 0,
        refetchInterval: pollMs,
        refetchIntervalInBackground: false,
    });
    const selectedTask = useMemo(() => (tasks || []).find((t) => String(t.id) === String(taskId)) ?? null, [tasks, taskId]);
    const { data: items, isLoading: itemsLoading, isFetching: itemsFetching, dataUpdatedAt: itemsUpdatedAt, } = useQuery({
        queryKey: ['follow-task-items', taskId],
        queryFn: () => trainApi.listFollowTaskItems(taskId),
        enabled: taskId != null,
        refetchInterval: taskId != null ? pollMs : false,
        refetchIntervalInBackground: false,
    });
    const { data: stats, isLoading: statsLoading, isFetching: statsFetching, dataUpdatedAt: statsUpdatedAt, } = useQuery({
        queryKey: ['follow-task-stats', taskId],
        queryFn: () => trainApi.getFollowTaskStats(taskId),
        enabled: taskId != null,
        refetchInterval: taskId != null ? pollMs : false,
        refetchIntervalInBackground: false,
    });
    const weakQuestions = useMemo(() => {
        const list = stats?.questionStats ?? [];
        return [...list]
            .filter((q) => q.accuracyRate != null)
            .sort((a, b) => (a.accuracyRate ?? 100) - (b.accuracyRate ?? 100));
    }, [stats]);
    const { data: answers, isLoading: answersLoading, isFetching: answersFetching, dataUpdatedAt: answersUpdatedAt, } = useQuery({
        queryKey: ['follow-task-answers', taskId, answerItem?.id],
        queryFn: () => trainApi.getFollowTaskItemAnswers(taskId, answerItem.id),
        enabled: taskId != null && answerItem != null,
        refetchInterval: taskId != null && answerItem != null ? pollMs : false,
        refetchIntervalInBackground: false,
    });
    // 明细刷新后同步当前选中学员行（状态/得分变化）
    useEffect(() => {
        if (!answerItem || !items?.length)
            return;
        const latest = items.find((i) => String(i.id) === String(answerItem.id));
        if (!latest)
            return;
        if (latest.status !== answerItem.status ||
            latest.score !== answerItem.score ||
            latest.submittedAt !== answerItem.submittedAt ||
            latest.accuracyRate !== answerItem.accuracyRate ||
            latest.correctCount !== answerItem.correctCount) {
            setAnswerItem(latest);
        }
    }, [items, answerItem]);
    useEffect(() => {
        const stamps = [tasksUpdatedAt, itemsUpdatedAt, statsUpdatedAt, answersUpdatedAt].filter((t) => t != null && t > 0);
        if (!stamps.length)
            return;
        setLastLiveAt(new Date(Math.max(...stamps)));
    }, [tasksUpdatedAt, itemsUpdatedAt, statsUpdatedAt, answersUpdatedAt]);
    const liveFetching = liveRefresh && (tasksFetching || itemsFetching || statsFetching || answersFetching);
    const invalidate = () => {
        void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId, 'mock'] });
        void queryClient.invalidateQueries({ queryKey: ['mock-used-q', classId] });
        void queryClient.invalidateQueries({ queryKey: ['mock-plan-options', classId] });
        if (taskId != null) {
            void queryClient.invalidateQueries({ queryKey: ['follow-task-items', taskId] });
            void queryClient.invalidateQueries({ queryKey: ['follow-task-stats', taskId] });
        }
    };
    const addPlanMu = useMutation({
        mutationFn: async () => {
            const v = await addForm.validateFields();
            const plan = buildMakeExamPlanCreatePayload(v);
            plan.examType = 0;
            return trainApi.createMockWithPlan(classId, {
                plan,
                title: plan.title,
                passScore: plan.passPoint,
                showAnalysisAfterSubmit: false,
                excludeOtherMock: true,
            });
        },
        onSuccess: (res) => {
            message.success(`模拟考草稿已创建（${summarizeImport(res.importStats) || '已挂接'}）`);
            setAddPlanOpen(false);
            addForm.resetFields();
            invalidate();
        },
        onError: (e) => message.error(e.message || '添加组卷失败'),
    });
    const pickPlanMu = useMutation({
        mutationFn: async () => {
            const v = await pickForm.validateFields();
            return trainApi.createMockFromPlan(classId, {
                makeExamPlanId: v.makeExamPlanId,
                title: v.title || undefined,
                description: v.description,
                bizDate: v.bizDate || undefined,
                passScore: v.passScore,
                showAnalysisAfterSubmit: !!v.showAnalysisAfterSubmit,
                excludeOtherMock: !!v.excludeOtherMock,
            });
        },
        onSuccess: (res) => {
            message.success(`模拟考草稿已创建（${summarizeImport(res.importStats) || '已挂接'}）`);
            setPickPlanOpen(false);
            pickForm.resetFields();
            invalidate();
        },
        onError: (e) => message.error(e.message || '选用计划失败'),
    });
    const publishMu = useMutation({
        mutationFn: (id) => trainApi.publishFollowTask(id),
        onSuccess: () => {
            message.success('已发布');
            invalidate();
        },
        onError: (e) => message.error(e.message || '发布失败'),
    });
    const closeMu = useMutation({
        mutationFn: (id) => trainApi.closeFollowTask(id),
        onSuccess: () => {
            message.success('已关闭');
            invalidate();
        },
        onError: (e) => message.error(e.message || '关闭失败'),
    });
    const reopenMu = useMutation({
        mutationFn: (id) => trainApi.reopenFollowTask(id),
        onSuccess: () => {
            message.success('已重新打开');
            invalidate();
        },
        onError: (e) => message.error(e.message || '重新打开失败'),
    });
    const planCourses = useMemo(() => (bankScope?.courses || []).map((c) => ({
        id: c.courseId,
        title: c.title,
    })), [bankScope]);
    const taskColumns = [
        { title: '标题', dataIndex: 'title', ellipsis: true },
        {
            title: '来源计划',
            width: 160,
            ellipsis: true,
            render: (_, r) => r.makeExamPlanTitle || (r.makeExamPlanId != null ? `#${r.makeExamPlanId}` : '—'),
        },
        {
            title: '题量',
            width: 70,
            render: (_, r) => r.questionCount ?? '—',
        },
        {
            title: '状态',
            width: 90,
            render: (_, r) => (_jsx(Tag, { color: STATUS_COLOR[r.status] || 'default', children: r.statusName || r.status })),
        },
        {
            title: '完成率',
            width: 90,
            render: (_, r) => r.completionRate != null ? `${r.completionRate}%` : r.status === 'draft' ? '—' : '0%',
        },
        {
            title: '进度',
            width: 110,
            render: (_, r) => r.totalCount != null ? `${r.submittedCount ?? 0}/${r.totalCount}` : '—',
        },
        {
            title: '操作',
            width: 240,
            render: (_, r) => (_jsxs(Space, { size: "small", wrap: true, children: [_jsx(Button, { type: "link", size: "small", onClick: () => {
                            setTaskId(r.id);
                            setAnswerItem(null);
                        }, children: "\u7B54\u5377\u8BE6\u60C5" }), r.status === 'draft' ? (_jsx(Button, { type: "link", size: "small", loading: publishMu.isPending, onClick: () => publishMu.mutate(r.id), children: "\u53D1\u5E03" })) : null, r.status === 'published' ? (_jsx(Button, { type: "link", size: "small", danger: true, loading: closeMu.isPending, onClick: () => closeMu.mutate(r.id), children: "\u5173\u95ED" })) : null, r.status === 'closed' ? (_jsx(Button, { type: "link", size: "small", loading: reopenMu.isPending, onClick: () => reopenMu.mutate(r.id), children: "\u91CD\u65B0\u6253\u5F00" })) : null] })),
        },
    ];
    const itemColumns = [
        { title: '学员', dataIndex: 'userName', width: 100 },
        {
            title: '状态',
            width: 80,
            render: (_, r) => (_jsx(Tag, { color: STATUS_COLOR[r.status] || 'default', children: r.statusName || r.status })),
        },
        {
            title: '得分',
            width: 70,
            render: (_, r) => (r.score != null ? r.score : '—'),
        },
        {
            title: '正确题数',
            width: 90,
            render: (_, r) => r.status === 'pending'
                ? '—'
                : `${r.correctCount ?? 0}/${r.questionCount ?? '—'}`,
        },
        {
            title: '正确率',
            width: 90,
            render: (_, r) => (r.accuracyRate != null ? `${r.accuracyRate}%` : '—'),
        },
        {
            title: '时长',
            width: 90,
            render: (_, r) => formatDuration(r.durationSeconds),
        },
        {
            title: '达标',
            width: 70,
            render: (_, r) => (r.passed == null ? '—' : r.passed ? '是' : '否'),
        },
        {
            title: '提交时间',
            dataIndex: 'submittedAt',
            width: 150,
            render: (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '—'),
        },
        {
            title: '操作',
            width: 100,
            render: (_, r) => r.status !== 'pending' ? (_jsx(Button, { type: "link", size: "small", onClick: () => setAnswerItem(r), children: "\u7B54\u5377\u8BE6\u60C5" })) : (_jsx(Typography.Text, { type: "secondary", children: "\u672A\u4EA4" })),
        },
    ];
    const qStatColumns = [
        { title: '题号', dataIndex: 'sortNumber', width: 60 },
        {
            title: '题干',
            ellipsis: true,
            render: (_, r) => stripHtml(r.stemPreview) || '—',
        },
        {
            title: '正确率',
            width: 140,
            render: (_, r) => r.accuracyRate == null ? ('—') : (_jsxs(Space, { size: 8, children: [_jsx(Progress, { percent: Number(r.accuracyRate), size: "small", style: { width: 72, margin: 0 }, status: Number(r.accuracyRate) < 60 ? 'exception' : 'normal', showInfo: false }), _jsxs("span", { children: [r.accuracyRate, "%"] })] })),
        },
        {
            title: '作答/正确',
            width: 100,
            render: (_, r) => `${r.correctCount ?? 0}/${r.answeredCount ?? 0}`,
        },
    ];
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u73ED\u7EA7\u6A21\u62DF\u8003\uFF1A\u901A\u8FC7\u9898\u5E93\u300C\u7EC4\u5377\u8BA1\u5212\u300D\u6DFB\u52A0\u7EC4\u5377\u6216\u9009\u7528\u5DF2\u6709\u8BA1\u5212\uFF1B\u5F00\u542F\u5B9E\u65F6\u66F4\u65B0\u540E\u53EF\u81EA\u52A8\u5237\u65B0\u4EA4\u5377\u8FDB\u5EA6\u4E0E\u7B54\u5377\u8BE6\u60C5\u3002", description: usedMeta != null
                    ? `本班其他模拟卷已占用题库题 ${usedMeta.count ?? 0} 道（草稿+已发布）`
                    : undefined }), _jsxs(Space, { wrap: true, align: "center", style: { width: '100%', justifyContent: 'space-between' }, children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => {
                                    addForm.resetFields();
                                    addForm.setFieldsValue({
                                        examType: 0,
                                        isAll: 0,
                                        isComputer: true,
                                        isPrint: false,
                                        courseId: planCourses[0]?.id,
                                    });
                                    setAddPlanOpen(true);
                                }, children: "\u6DFB\u52A0\u7EC4\u5377" }), _jsx(Button, { onClick: () => {
                                    pickForm.setFieldsValue({
                                        excludeOtherMock: true,
                                        showAnalysisAfterSubmit: false,
                                    });
                                    setPickPlanOpen(true);
                                }, children: "\u9009\u7528\u5DF2\u6709\u8BA1\u5212" }), _jsx(Button, { icon: _jsx(SyncOutlined, { spin: tasksFetching }), onClick: () => {
                                    void refetch();
                                    if (taskId != null) {
                                        void queryClient.invalidateQueries({ queryKey: ['follow-task-items', taskId] });
                                        void queryClient.invalidateQueries({ queryKey: ['follow-task-stats', taskId] });
                                    }
                                }, children: "\u5237\u65B0" })] }), _jsxs(Space, { size: "middle", align: "center", children: [liveRefresh && lastLiveAt ? (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: liveFetching ? '同步中…' : `最近更新 ${lastLiveAt.toLocaleTimeString()}` })) : null, _jsxs(Space, { size: 6, align: "center", children: [_jsx(Typography.Text, { children: "\u5B9E\u65F6\u66F4\u65B0" }), _jsx(Switch, { checked: liveRefresh, onChange: setLiveRefresh, checkedChildren: "\u5F00", unCheckedChildren: "\u5173" })] })] })] }), _jsx(Table, { rowKey: (r) => String(r.id), loading: isLoading, columns: taskColumns, dataSource: tasks || [], pagination: false, size: "middle", locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u6A21\u62DF\u5377\uFF0C\u8BF7\u6DFB\u52A0\u7EC4\u5377\u6216\u9009\u7528\u5DF2\u6709\u8BA1\u5212" }) } }), _jsxs(Drawer, { title: _jsxs(Space, { children: [_jsx("span", { children: selectedTask
                                ? `答卷详情 — ${selectedTask.title}`
                                : '答卷详情' }), liveRefresh ? (_jsx(Tag, { color: "processing", icon: _jsx(SyncOutlined, { spin: itemsFetching || statsFetching }), children: "\u5B9E\u65F6" })) : null] }), open: taskId != null, onClose: () => {
                    setTaskId(null);
                    setAnswerItem(null);
                }, width: 900, destroyOnHidden: true, extra: taskId != null ? (_jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exporting, onClick: async () => {
                        setExporting(true);
                        try {
                            await trainApi.exportFollowTaskAnswers(taskId);
                            message.success('已开始下载');
                        }
                        catch (e) {
                            message.error(e instanceof Error ? e.message : '导出失败');
                        }
                        finally {
                            setExporting(false);
                        }
                    }, children: "\u5BFC\u51FA\u7B54\u5377" })) : null, children: [selectedTask?.makeExamPlanTitle || selectedTask?.makeExamPlanId != null ? (_jsx(Alert, { type: "success", showIcon: true, style: { marginBottom: 12 }, message: `来源组卷计划：${selectedTask.makeExamPlanTitle || `#${selectedTask.makeExamPlanId}`}` })) : null, _jsxs(Space, { size: "large", style: { marginBottom: 12 }, wrap: true, children: [_jsx(Statistic, { title: "\u5E94\u4EA4", value: stats?.totalCount ?? 0, loading: statsLoading }), _jsx(Statistic, { title: "\u5DF2\u4EA4", value: stats?.submittedCount ?? 0, loading: statsLoading }), _jsx(Statistic, { title: "\u5E73\u5747\u5206", value: stats?.avgScore ?? '—', loading: statsLoading }), _jsx(Statistic, { title: "\u8FBE\u6807\u7387", value: stats?.passRate ?? '—', suffix: stats?.passRate != null ? '%' : undefined, loading: statsLoading }), _jsx(Statistic, { title: "\u5E73\u5747\u6B63\u786E\u7387", value: stats?.avgAccuracyRate ?? '—', suffix: stats?.avgAccuracyRate != null ? '%' : undefined, loading: statsLoading }), _jsx(Statistic, { title: "\u5E73\u5747\u65F6\u957F", value: formatDuration(stats?.avgDurationSeconds), loading: statsLoading })] }), weakQuestions.length > 0 ? (_jsxs("div", { style: { marginBottom: 12 }, children: [_jsxs(Typography.Text, { type: "secondary", children: ["\u8584\u5F31\u9898\uFF08\u6309\u6B63\u786E\u7387\u4ECE\u4F4E\u5230\u9AD8\uFF0C\u5171 ", weakQuestions.length, " \u9898\uFF09"] }), _jsx(Table, { style: { marginTop: 8 }, rowKey: (r) => String(r.questionId), size: "small", pagination: false, columns: qStatColumns, dataSource: weakQuestions, scroll: { y: 200 } })] })) : null, _jsx(Table, { rowKey: (r) => String(r.id), loading: itemsLoading, columns: itemColumns, dataSource: items || [], pagination: false, size: "small", scroll: { x: 900 }, locale: {
                            emptyText: (_jsx(Empty, { description: selectedTask?.status === 'draft'
                                    ? '草稿尚未发布，暂无学员答卷'
                                    : '暂无学员交卷记录' })),
                        } })] }), _jsx(Drawer, { title: _jsxs(Space, { children: [_jsx("span", { children: `答卷详情 — ${answerItem?.userName || ''}` }), liveRefresh ? (_jsx(Tag, { color: "processing", icon: _jsx(SyncOutlined, { spin: answersFetching }), children: "\u5B9E\u65F6" })) : null] }), open: answerItem != null, onClose: () => setAnswerItem(null), width: 640, destroyOnHidden: true, children: answersLoading ? (_jsx(Typography.Text, { type: "secondary", children: "\u52A0\u8F7D\u4E2D\u2026" })) : answers ? (_jsxs(_Fragment, { children: [selectedTask?.makeExamPlanTitle || selectedTask?.makeExamPlanId != null ? (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: `来源计划：${selectedTask.makeExamPlanTitle || `#${selectedTask.makeExamPlanId}`}` })) : null, _jsxs(Descriptions, { size: "small", column: 2, style: { marginBottom: 12 }, children: [_jsx(Descriptions.Item, { label: "\u5F97\u5206", children: answers.score ?? '—' }), _jsx(Descriptions.Item, { label: "\u8FBE\u6807", children: answers.passed == null ? '—' : answers.passed ? '是' : '否' }), _jsxs(Descriptions.Item, { label: "\u6B63\u786E\u9898\u6570", children: [answers.correctCount ?? 0, "/", answers.questionCount ?? 0] }), _jsx(Descriptions.Item, { label: "\u6B63\u786E\u7387", children: answers.accuracyRate != null
                                        ? `${answers.accuracyRate}%`
                                        : answerItem?.accuracyRate != null
                                            ? `${answerItem.accuracyRate}%`
                                            : '—' }), _jsx(Descriptions.Item, { label: "\u65F6\u957F", children: formatDuration(answers.durationSeconds != null
                                        ? answers.durationSeconds
                                        : answerItem?.durationSeconds) }), _jsx(Descriptions.Item, { label: "\u63D0\u4EA4\u65F6\u95F4", children: answerItem?.submittedAt
                                        ? String(answerItem.submittedAt).replace('T', ' ').slice(0, 16)
                                        : '—' })] }), (answers.answers ?? []).length === 0 ? (_jsx(Empty, { description: "\u6682\u65E0\u7B54\u9898\u660E\u7EC6" })) : ((answers.answers ?? []).map((a) => (_jsxs("div", { style: {
                                marginBottom: 12,
                                padding: 12,
                                background: '#fafafa',
                                borderRadius: 8,
                                border: `1px solid ${a.correct ? '#b7eb8f' : '#ffccc7'}`,
                            }, children: [_jsxs(Typography.Text, { strong: true, children: [a.sortNumber, ". [", a.questionType, "] ", stripHtml(a.stem)] }), _jsxs("div", { style: { marginTop: 8 }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u5B66\u5458\u4F5C\u7B54\uFF1A" }), ' ', _jsx(Typography.Text, { children: formatAnswerJson(a.userAnswerJson) })] }), _jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u6B63\u786E\u7B54\u6848\uFF1A" }), ' ', _jsx(Typography.Text, { type: "success", children: formatAnswerJson(a.correctAnswerJson) })] }), _jsxs("div", { style: { marginTop: 6 }, children: [_jsx(Tag, { color: a.correct ? 'green' : 'red', children: a.correct ? '正确' : '错误' }), _jsxs(Typography.Text, { type: "secondary", children: [' ', a.score ?? 0, "/", a.fullScore ?? 0] })] }), a.analysis ? (_jsx("div", { style: { marginTop: 4 }, children: _jsxs(Typography.Text, { type: "secondary", children: ["\u89E3\u6790\uFF1A", stripHtml(a.analysis)] }) })) : null] }, String(a.questionId)))))] })) : (_jsx(Empty, {})) }), _jsxs(Modal, { title: "\u6DFB\u52A0\u7EC4\u5377\u8BA1\u5212", open: addPlanOpen, onCancel: () => {
                    setAddPlanOpen(false);
                    addForm.resetFields();
                }, onOk: () => addPlanMu.mutate(), confirmLoading: addPlanMu.isPending, width: 900, destroyOnHidden: true, okText: "\u751F\u6210\u8BA1\u5212\u5E76\u6302\u63A5\u6A21\u62DF\u8003", children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u4E0E\u9898\u5E93\u300C\u7EC4\u5377\u8BA1\u5212 \u2192 \u6DFB\u52A0\u300D\u540C\u4E00\u5957\u8868\u5355\u4E0E\u751F\u6210\u903B\u8F91\uFF1B\u63D0\u4EA4\u540E\u5199\u5165\u7EC4\u5377\u8BA1\u5212\u5217\u8868\uFF0C\u5E76\u81EA\u52A8\u6302\u63A5\u4E3A\u672C\u73ED\u6A21\u62DF\u8003\u8349\u7A3F\u3002" }), _jsx(MakeExamPlanForm, { form: addForm, courses: planCourses, examTypeLocked: 0, coursePlaceholder: planCourses.length ? '选择科目' : '暂无科目，请先配置本班练习范围' })] }), _jsx(Modal, { title: "\u9009\u7528\u5DF2\u6709\u7EC4\u5377\u8BA1\u5212", open: pickPlanOpen, onCancel: () => setPickPlanOpen(false), onOk: () => pickPlanMu.mutate(), confirmLoading: pickPlanMu.isPending, width: 640, destroyOnHidden: true, okText: "\u6302\u63A5\u4E3A\u6A21\u62DF\u8003\u8349\u7A3F", children: _jsxs(Form, { form: pickForm, layout: "vertical", children: [_jsx(Form.Item, { name: "makeExamPlanId", label: "\u7EC4\u5377\u8BA1\u5212", rules: [{ required: true, message: '请选择组卷计划' }], children: _jsx(Select, { loading: planOptionsLoading, showSearch: true, optionFilterProp: "label", placeholder: "\u9009\u62E9\u7406\u8BBA\u7EC4\u5377\u8BA1\u5212", options: (planOptions || []).map((p) => ({
                                    value: p.id,
                                    label: `${p.title}（${p.courseTitle || p.courseId}）`,
                                })), onChange: (id) => {
                                    const opt = (planOptions || []).find((p) => p.id === id);
                                    if (opt) {
                                        pickForm.setFieldsValue({
                                            title: opt.title,
                                            passScore: opt.passPoint ?? undefined,
                                        });
                                    }
                                } }) }), _jsx(Form.Item, { name: "title", label: "\u6A21\u62DF\u8003\u6807\u9898", children: _jsx(Input, { maxLength: 200 }) }), _jsx(Form.Item, { name: "bizDate", label: "\u4E1A\u52A1\u65E5\uFF08\u53EF\u9009\uFF09", children: _jsx(Input, { placeholder: "YYYY-MM-DD" }) }), _jsx(Form.Item, { name: "description", label: "\u8BF4\u660E", children: _jsx(Input.TextArea, { rows: 2, maxLength: 1000 }) }), _jsx(Form.Item, { name: "passScore", label: "\u53CA\u683C\u5206\uFF08\u53EF\u9009\uFF09", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "showAnalysisAfterSubmit", label: "\u4EA4\u5377\u540E\u663E\u793A\u89E3\u6790", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Form.Item, { name: "excludeOtherMock", valuePropName: "checked", children: _jsx(Checkbox, { children: "\u6302\u63A5\u65F6\u6392\u9664\u672C\u73ED\u5176\u4ED6\u6A21\u62DF\u5377\u5DF2\u9009\u9898" }) })] }) })] }));
}

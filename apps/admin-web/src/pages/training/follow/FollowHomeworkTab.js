import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Alert, Button, Descriptions, Drawer, Empty, Progress, Space, Statistic, Table, Tag, Typography, message, } from 'antd';
import { DownloadOutlined, ReloadOutlined, TeamOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
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
export function FollowHomeworkTab({ classId }) {
    const queryClient = useQueryClient();
    const [taskId, setTaskId] = useState(null);
    const [answerItem, setAnswerItem] = useState(null);
    const [exporting, setExporting] = useState(false);
    const { data: metrics, isLoading: metricsLoading } = useQuery({
        queryKey: ['homework-metrics', classId],
        queryFn: () => trainApi.getHomeworkMetrics(classId),
        enabled: classId > 0,
    });
    const { data: tasks, isLoading, refetch } = useQuery({
        queryKey: ['follow-tasks', classId, 'assignment'],
        queryFn: () => trainApi.listFollowTasks(classId, 'assignment'),
        enabled: classId > 0,
    });
    const { data: items, isLoading: itemsLoading } = useQuery({
        queryKey: ['follow-task-items', taskId],
        queryFn: () => trainApi.listFollowTaskItems(taskId),
        enabled: taskId != null,
    });
    const { data: stats, isLoading: statsLoading } = useQuery({
        queryKey: ['follow-task-stats', taskId],
        queryFn: () => trainApi.getFollowTaskStats(taskId),
        enabled: taskId != null,
    });
    const { data: answers, isLoading: answersLoading } = useQuery({
        queryKey: ['follow-task-answers', taskId, answerItem?.id],
        queryFn: () => trainApi.getFollowTaskItemAnswers(taskId, answerItem.id),
        enabled: taskId != null && answerItem != null,
    });
    const syncMu = useMutation({
        mutationFn: (id) => trainApi.syncFollowTaskRoster(id),
        onSuccess: (r, id) => {
            message.success(`已补发 ${r.added} 人`);
            void queryClient.invalidateQueries({ queryKey: ['follow-task-items', id] });
            void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId] });
            void queryClient.invalidateQueries({ queryKey: ['homework-metrics', classId] });
            void queryClient.invalidateQueries({ queryKey: ['follow-task-stats', id] });
        },
        onError: (e) => message.error(e.message || '补发失败'),
    });
    const closeMu = useMutation({
        mutationFn: (id) => trainApi.closeFollowTask(id),
        onSuccess: () => {
            message.success('已关闭');
            void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId] });
            void queryClient.invalidateQueries({ queryKey: ['homework-metrics', classId] });
        },
        onError: (e) => message.error(e.message || '关闭失败'),
    });
    const reopenMu = useMutation({
        mutationFn: (id) => trainApi.reopenFollowTask(id),
        onSuccess: () => {
            message.success('已重新打开');
            void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId] });
            void queryClient.invalidateQueries({ queryKey: ['homework-metrics', classId] });
        },
        onError: (e) => message.error(e.message || '重新打开失败'),
    });
    const weakQuestions = useMemo(() => {
        const list = stats?.questionStats ?? [];
        return [...list]
            .filter((q) => q.accuracyRate != null)
            .sort((a, b) => (a.accuracyRate ?? 100) - (b.accuracyRate ?? 100))
            .slice(0, 5);
    }, [stats]);
    const taskColumns = [
        {
            title: '类型',
            width: 90,
            render: (_, r) => r.taskTypeName || r.taskType,
        },
        { title: '标题', dataIndex: 'title', ellipsis: true },
        {
            title: '挂载',
            width: 120,
            render: (_, r) => r.scheduleId != null ? `课次#${r.scheduleId}` : r.bizDate || '—',
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
            width: 220,
            render: (_, r) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", onClick: () => {
                            setTaskId(r.id);
                            setAnswerItem(null);
                        }, children: "\u660E\u7EC6" }), r.status === 'published' ? (_jsxs(_Fragment, { children: [_jsx(Button, { type: "link", size: "small", loading: syncMu.isPending, onClick: () => syncMu.mutate(r.id), children: "\u8865\u53D1\u540D\u5355" }), _jsx(Button, { type: "link", size: "small", danger: true, loading: closeMu.isPending, onClick: () => closeMu.mutate(r.id), children: "\u5173\u95ED" })] })) : null, r.status === 'closed' ? (_jsx(Button, { type: "link", size: "small", loading: reopenMu.isPending, onClick: () => reopenMu.mutate(r.id), children: "\u91CD\u65B0\u6253\u5F00" })) : null] })),
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
            title: '正确率',
            width: 90,
            render: (_, r) => r.accuracyRate != null ? `${r.accuracyRate}%` : r.status === 'pending' ? '—' : '—',
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
            width: 80,
            render: (_, r) => r.status !== 'pending' ? (_jsx(Button, { type: "link", size: "small", onClick: () => setAnswerItem(r), children: "\u7B54\u5377" })) : ('—'),
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
    return (_jsxs("div", { children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u8BFE\u5802\u4F5C\u4E1A / \u968F\u5802\u7EC3", description: "\u8001\u5E08\u5728 App \u7406\u8BBA\u8001\u5E08\u53F0\u5E03\u7F6E\uFF1B\u672C\u9875\u67E5\u770B\u8FDB\u5EA6\u3001\u6B63\u786E\u7387\u3001\u7B54\u5377\u4E0E\u5BFC\u51FA\u3002\u4E0E\u63D0\u5347\u300C\u8003\u524D\u7EC3\u4E60\u300D\u3001\u73ED\u7EA7\u5237\u9898\u5206\u901A\u9053\u3002" }), _jsxs(Space, { size: "large", style: { marginBottom: 16 }, wrap: true, children: [_jsx(Statistic, { title: "\u5DF2\u53D1\u5E03\u4EFB\u52A1", value: metrics?.taskCount ?? 0, loading: metricsLoading, prefix: _jsx(TeamOutlined, {}) }), _jsx(Statistic, { title: "\u5E94\u4EA4\u4EBA\u6B21", value: metrics?.totalItems ?? 0, loading: metricsLoading }), _jsx(Statistic, { title: "\u5DF2\u4EA4\u4EBA\u6B21", value: metrics?.gradedItems ?? 0, loading: metricsLoading }), _jsx(Statistic, { title: "\u5B8C\u6210\u7387", value: metrics?.completionRate ?? 0, suffix: "%", loading: metricsLoading }), _jsx(Statistic, { title: "\u5E73\u5747\u6B63\u786E\u7387", value: metrics?.avgAccuracyRate ?? '—', suffix: metrics?.avgAccuracyRate != null ? '%' : undefined, loading: metricsLoading }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void refetch(), children: "\u5237\u65B0" })] }), (metrics?.warnings?.length ?? 0) > 0 ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u5B66\u5458\u9884\u8B66", description: _jsx("ul", { style: { margin: 0, paddingLeft: 18 }, children: (metrics?.warnings ?? []).slice(0, 8).map((w, i) => (_jsxs("li", { children: [w.userName || w.peopleId, "\uFF1A", w.message] }, `${w.peopleId}-${w.type}-${i}`))) }) })) : null, _jsx(Table, { rowKey: (r) => String(r.id), size: "small", loading: isLoading, columns: taskColumns, dataSource: tasks ?? [], pagination: { pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 条` }, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u8BFE\u5802\u4F5C\u4E1A\uFF1B\u8BF7\u8001\u5E08\u5728 App \u5E03\u7F6E" }) } }), _jsxs(Drawer, { title: "\u4EFB\u52A1\u63D0\u4EA4\u660E\u7EC6", width: 760, open: taskId != null, onClose: () => {
                    setTaskId(null);
                    setAnswerItem(null);
                }, extra: taskId != null ? (_jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exporting, onClick: async () => {
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
                    }, children: "\u5BFC\u51FA\u7B54\u5377" })) : null, children: [_jsxs(Space, { size: "large", style: { marginBottom: 12 }, wrap: true, children: [_jsx(Statistic, { title: "\u5E73\u5747\u5206", value: stats?.avgScore ?? '—', loading: statsLoading }), _jsx(Statistic, { title: "\u8FBE\u6807\u7387", value: stats?.passRate ?? '—', suffix: stats?.passRate != null ? '%' : undefined, loading: statsLoading }), _jsx(Statistic, { title: "\u5E73\u5747\u6B63\u786E\u7387", value: stats?.avgAccuracyRate ?? '—', suffix: stats?.avgAccuracyRate != null ? '%' : undefined, loading: statsLoading }), _jsx(Statistic, { title: "\u5E73\u5747\u65F6\u957F", value: formatDuration(stats?.avgDurationSeconds), loading: statsLoading })] }), weakQuestions.length > 0 ? (_jsxs("div", { style: { marginBottom: 12 }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u8584\u5F31\u9898\uFF08\u6B63\u786E\u7387\u6700\u4F4E\uFF09" }), _jsx(Table, { style: { marginTop: 8 }, rowKey: (r) => String(r.questionId), size: "small", pagination: false, columns: qStatColumns, dataSource: weakQuestions })] })) : null, _jsx(Table, { rowKey: (r) => String(r.id), size: "small", loading: itemsLoading, columns: itemColumns, dataSource: items ?? [], pagination: false })] }), _jsx(Drawer, { title: `答卷 — ${answerItem?.userName || ''}`, width: 560, open: !!answerItem, onClose: () => setAnswerItem(null), children: answersLoading ? (_jsx(Typography.Text, { type: "secondary", children: "\u52A0\u8F7D\u4E2D\u2026" })) : answers ? (_jsxs(_Fragment, { children: [_jsxs(Descriptions, { size: "small", column: 2, style: { marginBottom: 12 }, children: [_jsx(Descriptions.Item, { label: "\u5F97\u5206", children: answers.score ?? '—' }), _jsx(Descriptions.Item, { label: "\u8FBE\u6807", children: answers.passed == null ? '—' : answers.passed ? '是' : '否' }), _jsx(Descriptions.Item, { label: "\u6B63\u786E\u7387", children: answers.accuracyRate != null
                                        ? `${answers.accuracyRate}%`
                                        : answerItem?.accuracyRate != null
                                            ? `${answerItem.accuracyRate}%`
                                            : '—' }), _jsx(Descriptions.Item, { label: "\u65F6\u957F", children: formatDuration(answers.durationSeconds != null
                                        ? answers.durationSeconds
                                        : answerItem?.durationSeconds) })] }), (answers.answers ?? []).map((a) => (_jsxs("div", { style: {
                                marginBottom: 12,
                                padding: 12,
                                background: '#fafafa',
                                borderRadius: 8,
                                border: `1px solid ${a.correct ? '#b7eb8f' : '#ffccc7'}`,
                            }, children: [_jsxs(Typography.Text, { strong: true, children: [a.sortNumber, ". [", a.questionType, "] ", stripHtml(a.stem)] }), _jsxs("div", { style: { marginTop: 8 }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u4F60\u7684\u9009\u62E9\uFF1A" }), ' ', _jsx(Typography.Text, { children: formatAnswerJson(a.userAnswerJson) })] }), _jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u6B63\u786E\u7B54\u6848\uFF1A" }), ' ', _jsx(Typography.Text, { type: "success", children: formatAnswerJson(a.correctAnswerJson) })] }), _jsxs("div", { style: { marginTop: 6 }, children: [_jsx(Tag, { color: a.correct ? 'green' : 'red', children: a.correct ? '正确' : '错误' }), _jsxs(Typography.Text, { type: "secondary", children: [' ', a.score ?? 0, "/", a.fullScore ?? 0] })] }), a.analysis ? (_jsx("div", { style: { marginTop: 4 }, children: _jsxs(Typography.Text, { type: "secondary", children: ["\u89E3\u6790\uFF1A", stripHtml(a.analysis)] }) })) : null] }, a.questionId)))] })) : (_jsx(Empty, {})) })] }));
}

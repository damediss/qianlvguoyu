import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Card, Empty, Modal, Progress, Select, Space, Table, Tag, Tooltip, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { completeDefenseTask, exportDefenseSummary, getDefenseProgress, markDefenseCandidateAbsent } from '../../api/defenseScoring';
import { pagePreparationTasks } from '../../api/certificationPreparation';
import { PermissionGuard } from '../../directives/permission';
import { formatScore, statusLabel } from '../assessor/defenseScoreCatalog';
import { parseContentDispositionFilename } from '../../utils/download';
import { usePaneActive } from '../../layouts/TabKeepAlive';
function overallLabel(status) {
    if (status === 'completed')
        return { color: 'success', text: '答辩已完成' };
    if (status === 'ready_to_complete')
        return { color: 'processing', text: '评分已齐或已标记缺考，待完成答辩' };
    if (status === 'scoring')
        return { color: 'processing', text: '答辩评分中' };
    if (status === 'not_configured')
        return { color: 'default', text: '未配置综合答辩' };
    return { color: 'default', text: '待开始' };
}
function candidateStatusTag(row) {
    if (row.status === 'absent')
        return _jsx(Tag, { color: "warning", children: "\u7F3A\u8003" });
    if (row.rejected)
        return _jsx(Tag, { color: "error", children: "\u4E0D\u5408\u683C" });
    if (row.status === 'completed')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u5B8C\u6210" });
    if (row.status === 'scoring')
        return _jsx(Tag, { color: "processing", children: "\u8BC4\u5206\u4E2D" });
    if (row.status === 'pending_report')
        return _jsx(Tag, { children: "\u5F85\u4E0A\u4F20\u62A5\u544A" });
    return _jsx(Tag, { children: "\u5F85\u8BC4\u5206" });
}
export function DefenseManagementPage() {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6，原为无条件常驻 5s）
    const [selectedTaskId, setSelectedTaskId] = useState();
    const preferredTaskNo = useMemo(() => new URLSearchParams(window.location.search).get('taskNo') || '', []);
    const tasksQuery = useQuery({
        queryKey: ['certification-defense-management-tasks'],
        queryFn: () => pagePreparationTasks({ page: 1, pageSize: 200, currentPhase: 'preparation' }),
    });
    const tasks = tasksQuery.data?.items || [];
    useEffect(() => {
        if (tasks.length === 0)
            return;
        const preferred = preferredTaskNo
            ? tasks.find((task) => task.taskNo === preferredTaskNo)
            : undefined;
        if (!selectedTaskId) {
            setSelectedTaskId(preferred?.id || tasks[0].id);
            return;
        }
        if (!tasks.some((task) => task.id === selectedTaskId)) {
            setSelectedTaskId(preferred?.id || tasks[0].id);
        }
    }, [preferredTaskNo, selectedTaskId, tasks]);
    const selectedTask = useMemo(() => tasks.find((task) => task.id === selectedTaskId), [selectedTaskId, tasks]);
    const progressQuery = useQuery({
        queryKey: ['defense-scoring-progress', selectedTaskId],
        queryFn: () => getDefenseProgress(selectedTaskId),
        enabled: Boolean(selectedTaskId),
        refetchInterval: paneActive ? 5000 : false,
    });
    const progress = progressQuery.data;
    const overall = overallLabel(progress?.overallStatus);
    const completeDisabled = progress?.overallStatus !== 'ready_to_complete';
    const exportDisabled = progress?.overallStatus !== 'ready_to_complete' && progress?.overallStatus !== 'completed';
    const completeDisabledReason = progress?.overallStatus === 'not_configured'
        ? '请先在认定筹备中绑定综合答辩考评组并上传业绩评估报告'
        : progress?.overallStatus === 'completed'
            ? '答辩已完成'
            : '全部考生完成评分或标记缺考后才能完成答辩';
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-defense-management-tasks'] });
        if (selectedTaskId) {
            queryClient.invalidateQueries({ queryKey: ['defense-scoring-progress', selectedTaskId] });
        }
    };
    const completeMutation = useMutation({
        mutationFn: () => completeDefenseTask(selectedTaskId),
        onSuccess: (result) => {
            message.success(`答辩已完成，共 ${result.candidateCount} 名考生`);
            refresh();
        },
    });
    const absentMutation = useMutation({
        mutationFn: (candidateId) => markDefenseCandidateAbsent(selectedTaskId, candidateId),
        onSuccess: () => {
            message.success('已标记缺考，汇总成绩为 0');
            refresh();
        },
    });
    const exportMutation = useMutation({
        mutationFn: () => exportDefenseSummary(selectedTaskId),
        onSuccess: (resp) => {
            const filename = parseContentDispositionFilename({ headers: resp.headers })
                || `技师、高级技师评价综合评定科目评审成绩汇总表_${selectedTaskId}.zip`;
            const url = window.URL.createObjectURL(resp.data);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            message.success('成绩汇总压缩包已生成');
        },
    });
    const taskLabel = (task) => `${task.taskNo} · ${task.taskName} · ${task.workTypeName || '-'} / ${task.levelName || '-'}`;
    const expertNames = progress?.experts || [];
    const expertCount = Math.max(3, expertNames.length);
    const expertIndexes = Array.from({ length: expertCount }, (_, index) => index);
    const columns = [
        { title: '准考证号', dataIndex: 'admissionTicketNo', width: 180, render: (v, row) => v || row.candidateNo || '-' },
        { title: '考生', dataIndex: 'candidateName', width: 110, fixed: 'left' },
        { title: '证件号', dataIndex: 'maskedIdCardNo', width: 170, render: (v) => v || '-' },
        ...expertIndexes.map((index) => ({
            title: `论文·${expertNames[index]?.assessorName || `专家${index + 1}`}`,
            width: 120,
            render: (_, row) => formatScore(row.thesisScores?.[index]),
        })),
        { title: '论文平均', dataIndex: 'thesisAverage', width: 100, render: (v) => formatScore(v) },
        ...expertIndexes.map((index) => ({
            title: `业绩·${expertNames[index]?.assessorName || `专家${index + 1}`}`,
            width: 120,
            render: (_, row) => formatScore(row.performanceScores?.[index]),
        })),
        { title: '业绩平均', dataIndex: 'performanceAverage', width: 100, render: (v) => formatScore(v) },
        {
            title: '最终成绩',
            width: 110,
            render: (_, row) => (row.rejected ? _jsx(Typography.Text, { type: "danger", children: "\u4E0D\u5408\u683C" }) : formatScore(row.finalScore)),
        },
        { title: '状态', width: 110, render: (_, row) => candidateStatusTag(row) },
        {
            title: '操作',
            width: 100,
            fixed: 'right',
            render: (_, row) => {
                const absentDisabled = row.status === 'completed'
                    || row.status === 'absent'
                    || row.status === 'scoring'
                    || progress?.overallStatus === 'completed';
                return (_jsx(PermissionGuard, { codes: ['certification.defense.complete', 'certification.preparation.task.view'], mode: "any", children: _jsx(Button, { size: "small", danger: true, disabled: absentDisabled, loading: absentMutation.isPending && absentMutation.variables === row.candidateId, onClick: () => {
                            Modal.confirm({
                                title: '确认标记缺考？',
                                content: `${row.candidateName} 将不再进入答辩评分；未提交的草稿评分会清除，成绩汇总表记为 0，签名处留空。`,
                                okText: '确认缺考',
                                cancelText: '取消',
                                okButtonProps: { danger: true },
                                onOk: () => absentMutation.mutateAsync(row.candidateId),
                            });
                        }, children: "\u7F3A\u8003" }) }));
            },
        },
    ];
    return (_jsx(PermissionGuard, { codes: ['certification.defense.progress', 'certification.preparation.task.view'], mode: "any", fallback: _jsx(Empty, { description: "\u6682\u65E0\u7B54\u8FA9\u7BA1\u7406\u6743\u9650" }), children: _jsxs("div", { className: "knowledge-page", children: [_jsxs("div", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u7B54\u8FA9\u7BA1\u7406" }), _jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\uFF0C\u67E5\u770B\u7EFC\u5408\u7B54\u8FA9\u8FDB\u5EA6\uFF0C\u5E76\u7BA1\u7406\u8BBA\u6587\u7B54\u8FA9\u4E0E\u4E1A\u7EE9\u8BC4\u4F30\u8BC4\u5206\u6570\u636E" })] }), _jsx(Space, { children: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }) })] }), _jsxs(Card, { title: "\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1", style: { marginBottom: 16 }, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", loading: tasksQuery.isFetching, value: selectedTaskId, placeholder: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1", style: { width: 'min(720px, 100%)' }, options: tasks.map((task) => ({ value: task.id, label: taskLabel(task) })), onChange: setSelectedTaskId }), selectedTask ? (_jsxs(Typography.Paragraph, { type: "secondary", style: { marginTop: 12, marginBottom: 0 }, children: ["\u5F53\u524D\u4EFB\u52A1\uFF1A", taskLabel(selectedTask)] })) : null] }), !selectedTaskId ? (_jsx(Empty, { description: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1" })) : (_jsxs(ProCard, { title: (_jsxs(Space, { children: ["\u7B54\u8FA9\u8BC4\u5206\u8FDB\u5EA6", _jsx(Tag, { color: overall.color, children: overall.text }), progress?.groupName ? _jsx(Tag, { color: "processing", children: progress.groupName }) : null] })), extra: (_jsxs(Space, { children: [_jsx(Tooltip, { title: completeDisabled ? completeDisabledReason : undefined, children: _jsx("span", { children: _jsx(Button, { type: "primary", danger: true, disabled: completeDisabled, loading: completeMutation.isPending, onClick: () => {
                                            Modal.confirm({
                                                title: '确认完成答辩？',
                                                content: '全部考生已完成论文答辩和业绩评估评分或已标记缺考。完成后可导出成绩汇总表及评分表压缩包。',
                                                okText: '完成答辩',
                                                cancelText: '取消',
                                                onOk: () => completeMutation.mutateAsync(),
                                            });
                                        }, children: "\u5B8C\u6210\u7B54\u8FA9" }) }) }), _jsx(Tooltip, { title: exportDisabled ? '全部考生完成答辩评分后才能导出' : undefined, children: _jsx("span", { children: _jsx(Button, { icon: _jsx(DownloadOutlined, {}), disabled: exportDisabled, loading: exportMutation.isPending, onClick: () => exportMutation.mutate(), children: "\u5BFC\u51FA\u6210\u7EE9\u6C47\u603B\u8868" }) }) })] })), children: [progress?.overallStatus === 'not_configured' ? (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u5C1A\u672A\u914D\u7F6E\u7EFC\u5408\u7B54\u8FA9", description: "\u8BF7\u5148\u5230\u8BA4\u5B9A\u7B79\u5907\u300C\u7EFC\u5408\u7B54\u8FA9\u914D\u7F6E\u300D\u7ED1\u5B9A\u8003\u8BC4\u7EC4\uFF0C\u5E76\u6309\u8003\u751F\u4E0A\u4F20\u4E1A\u7EE9\u8BC4\u4F30\u62A5\u544A PDF\u3002\u8003\u8BC4\u5458\u5728\u7EFC\u5408\u7B54\u8FA9\u7AEF\u5B8C\u6210\u8BBA\u6587\u7B54\u8FA9\u548C\u4E1A\u7EE9\u8BC4\u4F30\u540E\uFF0C\u672C\u9875\u4F1A\u6C47\u603B\u8BC4\u5206\u3002" })) : null, _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u4EBA\u5458\u540D\u5355\u4E0E\u7EFC\u5408\u7B54\u8FA9\u914D\u7F6E\u540C\u6B65", description: "\u4EC5\u6280\u5E08\u3001\u9AD8\u7EA7\u6280\u5E08\u8FDB\u5165\u7B54\u8FA9\u540D\u5355\uFF0C\u4E0E\u8BA4\u5B9A\u7B79\u5907\u300C\u7EFC\u5408\u7B54\u8FA9\u914D\u7F6E\u300D\u4E2D\u7684\u4EBA\u5458\u540D\u5355\u4E00\u81F4\u3002" }), progress?.overallStatus === 'ready_to_complete' ? (_jsx(Alert, { type: "success", showIcon: true, style: { marginBottom: 16 }, message: "\u5168\u90E8\u8003\u751F\u8BC4\u5206\u5DF2\u63D0\u4EA4\u6216\u5DF2\u6807\u8BB0\u7F3A\u8003", description: "\u7F3A\u8003\u8003\u751F\u7684\u6C47\u603B\u6210\u7EE9\u8BB0\u4E3A 0\uFF0C\u7B7E\u540D\u5904\u7559\u7A7A\u3002\u70B9\u51FB\u300C\u5B8C\u6210\u7B54\u8FA9\u300D\u540E\u5373\u53EF\u5BFC\u51FA\u6210\u7EE9\u6C47\u603B\u8868\u53CA\u8BC4\u5206\u8868\u538B\u7F29\u5305\u3002" })) : null, _jsxs(Space, { size: 24, style: { marginBottom: 16 }, children: [_jsx(Progress, { type: "circle", size: 92, percent: progress?.totalCandidateCount
                                        ? Math.round(((progress.candidates || []).filter((row) => row.status === 'completed' || row.status === 'absent').length) * 100 / progress.totalCandidateCount)
                                        : 0 }), _jsxs(Space, { direction: "vertical", size: 4, children: [_jsxs(Typography.Text, { children: ["\u8003\u751F\u5DF2\u8BC4\u5206\u6216\u7F3A\u8003 ", progress?.candidates?.filter((row) => row.status === 'completed' || row.status === 'absent').length || 0, " / ", progress?.totalCandidateCount || 0, " \u4EBA"] }), _jsx(Typography.Text, { type: "secondary", children: "\u6700\u7EC8\u6210\u7EE9 = \u8BBA\u6587\u7B54\u8FA9\u5E73\u5747\u5206 \u00D7 60% + \u4E1A\u7EE9\u8BC4\u4F30\u5E73\u5747\u5206 \u00D7 40%" }), progress?.examDate || progress?.examSite ? (_jsx(Typography.Text, { type: "secondary", children: [progress.examDate, progress.examSite].filter(Boolean).join(' · ') })) : null] })] }), _jsx(Table, { rowKey: "candidateId", columns: columns, dataSource: progress?.candidates || [], loading: progressQuery.isFetching, pagination: false, scroll: { x: 1600 }, expandable: {
                                expandedRowRender: (row) => _jsx(AssessorBreakdown, { assessors: row.assessors }),
                                rowExpandable: (row) => (row.assessors || []).length > 0,
                            }, locale: { emptyText: '暂无符合条件的考生（仅技师、高级技师可参加综合答辩）' } })] }))] }) }));
}
function AssessorBreakdown({ assessors }) {
    return (_jsx(Table, { size: "small", rowKey: "assessorId", pagination: false, dataSource: assessors, columns: [
            {
                title: '考评员',
                dataIndex: 'assessorName',
                render: (value, row) => (_jsxs(Space, { children: [value || '-', row.leader ? _jsx(Tag, { color: "gold", children: "\u7EC4\u957F" }) : null] })),
            },
            {
                title: '论文答辩',
                render: (_, row) => (_jsxs(Space, { children: [_jsx(Tag, { color: row.thesisStatus === 'submitted' ? 'green' : row.thesisStatus === 'draft' ? 'gold' : 'default', children: statusLabel(row.thesisStatus) }), _jsx("span", { children: formatScore(row.thesisScore) }), row.thesisRejected ? _jsx(Tag, { color: "error", children: "\u5426\u5B9A" }) : null] })),
            },
            {
                title: '业绩评估',
                render: (_, row) => (_jsxs(Space, { children: [_jsx(Tag, { color: row.performanceStatus === 'submitted' ? 'green' : row.performanceStatus === 'draft' ? 'gold' : 'default', children: statusLabel(row.performanceStatus) }), _jsx("span", { children: formatScore(row.performanceScore) }), row.performanceRejected ? _jsx(Tag, { color: "error", children: "\u5426\u5B9A" }) : null] })),
            },
        ] }));
}

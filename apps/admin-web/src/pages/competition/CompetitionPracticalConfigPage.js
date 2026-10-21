import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Drawer, Result, Space, Table, Tabs, Tag, Typography, message, } from 'antd';
import { useMemo, useState } from 'react';
import { bindCompetitionPracticalPaper, listCompetitionEventWorkTypeLevels, listCompetitionPracticalPaperBindings, listCompetitionPracticalAssessmentSheets, } from '../../api/competition';
import { listCompetitionMakeExamSnapshotsForScope } from '../../api/competitionQuestion';
import { useAuthStore } from '../../stores/authStore';
import { CompetitionRefereeAssignmentPanel } from './CompetitionRefereeAssignmentPanel';
import { CompetitionPracticalExecutionConfigPanel } from './CompetitionPracticalExecutionConfigPanel';
function idValue(value) {
    return value === undefined || value === null ? '' : String(value);
}
function errorText(error, fallback) {
    return error instanceof Error ? error.message : fallback;
}
function plainText(value) {
    return (value ?? '').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
}
/**
 * Practical paper configuration opened from a stage node's context menu.
 *
 * Binding a published practical paper also creates its immutable assessment
 * sheet snapshot. The second tab exposes both the summary and per-question
 * scoring tables used by the console and referee devices.
 */
export function CompetitionPracticalConfigPage({ eventId, stageId, stageName, eventWorkTypeId, eventWorkTypeName, eventWorkTypeLevelCode, eventWorkTypeLevelName, initialTab = 'paper', }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const queryClient = useQueryClient();
    const canViewPaper = permissions.includes('competition:make-plan:view')
        || permissions.includes('competition:theory-paper:view');
    const canViewReferee = permissions.includes('competition:referee-assignment:view');
    const canViewStagePlan = permissions.includes('competition:stage-plan:view');
    const canManageStagePlan = permissions.includes('competition:stage-plan:manage');
    const canViewAssessment = permissions.includes('competition:practical-assessment:view')
        || canViewStagePlan;
    const [previewSheet, setPreviewSheet] = useState(null);
    const levelsQuery = useQuery({
        queryKey: ['competition-practical-config-levels', eventId, eventWorkTypeId],
        queryFn: () => listCompetitionEventWorkTypeLevels(eventId, eventWorkTypeId),
        enabled: (canViewPaper || canViewReferee || canViewStagePlan) && Boolean(eventWorkTypeId),
    });
    const selectedLevel = levelsQuery.data?.find((level) => level.levelCode === eventWorkTypeLevelCode);
    const eventWorkTypeLevelId = idValue(selectedLevel?.id);
    const scopeReady = Boolean(stageId && eventWorkTypeId && eventWorkTypeLevelId);
    const snapshotsQuery = useQuery({
        queryKey: ['competition-practical-config-snapshots', eventId, eventWorkTypeId, eventWorkTypeLevelId],
        queryFn: () => listCompetitionMakeExamSnapshotsForScope(eventId, {
            eventWorkTypeId,
            eventWorkTypeLevelId,
            contentType: 'practical',
        }),
        enabled: canViewPaper && scopeReady,
    });
    const bindingsQuery = useQuery({
        queryKey: ['competition-practical-paper-bindings', stageId, eventWorkTypeId, eventWorkTypeLevelId],
        queryFn: () => listCompetitionPracticalPaperBindings(stageId, {
            eventWorkTypeId,
            eventWorkTypeLevelId,
        }),
        enabled: canViewStagePlan && scopeReady,
    });
    const assessmentSheetsQuery = useQuery({
        queryKey: ['competition-practical-assessment-sheets', stageId, eventWorkTypeId, eventWorkTypeLevelId],
        queryFn: () => listCompetitionPracticalAssessmentSheets(stageId, {
            eventWorkTypeId,
            eventWorkTypeLevelId,
        }),
        enabled: canViewAssessment && scopeReady,
    });
    const practicalSnapshots = useMemo(() => (snapshotsQuery.data ?? []).filter((snapshot) => snapshot.status === 'published' && snapshot.questionCount > 0), [snapshotsQuery.data]);
    const boundPaperIds = useMemo(() => new Set((bindingsQuery.data ?? []).map((binding) => idValue(binding.paperSnapshotId))), [bindingsQuery.data]);
    const bindMutation = useMutation({
        mutationFn: (paperSnapshotId) => bindCompetitionPracticalPaper(stageId, {
            eventWorkTypeId,
            eventWorkTypeLevelId,
            paperSnapshotId,
        }),
        onSuccess: async () => {
            message.success('实操试卷已绑定到当前赛段');
            await queryClient.invalidateQueries({
                queryKey: ['competition-practical-paper-bindings', stageId, eventWorkTypeId, eventWorkTypeLevelId],
            });
            await queryClient.invalidateQueries({
                queryKey: ['competition-practical-assessment-sheets', stageId, eventWorkTypeId, eventWorkTypeLevelId],
            });
        },
        onError: (error) => message.error(errorText(error, '实操试卷绑定失败，请刷新后重试')),
    });
    const bindPaper = (snapshotId) => {
        if (!canManageStagePlan) {
            message.warning('暂无赛段编排管理权限');
            return;
        }
        if (!scopeReady) {
            message.warning('当前工种等级尚未解析，暂时不能绑定实操试卷');
            return;
        }
        if (boundPaperIds.has(idValue(snapshotId))) {
            message.info('该试卷已绑定到当前赛段');
            return;
        }
        bindMutation.mutate(snapshotId);
    };
    if ((!canViewPaper || !canViewStagePlan) && !canViewReferee) {
        return (_jsx(Result, { status: "403", title: "\u6682\u65E0\u5B9E\u64CD\u914D\u7F6E\u67E5\u770B\u6743\u9650", subTitle: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u7533\u8BF7\u8D5B\u6BB5\u7F16\u6392\u3001\u5B9E\u64CD\u8BD5\u5377\u6216\u88C1\u5224\u914D\u7F6E\u6743\u9650\u3002" }));
    }
    return (_jsxs("div", { "data-testid": "competition-practical-config-page", children: [_jsxs(Card, { bordered: false, className: "competition-filter-card competition-filter-card--workspace", children: [_jsxs("div", { className: "competition-filter-card__head", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u5B9E\u64CD\u914D\u7F6E" }), _jsx(Typography.Text, { type: "secondary", children: "\u7ED1\u5B9A\u5DF2\u53D1\u5E03\u5B9E\u64CD\u8BD5\u5377\u65F6\u81EA\u52A8\u751F\u6210\u8003\u8BC4\u8868\uFF1B\u8003\u8BC4\u8868\u5305\u542B\u8BD5\u9898\u603B\u8BC4\u5206\u8868\u53CA\u6BCF\u9053\u9898\u7684\u8BC4\u5206\u660E\u7EC6\u8868\u3002" })] }), _jsx(Tag, { color: "orange", children: "\u5B9E\u64CD\u8003\u8BC4" })] }), _jsxs(Space, { wrap: true, className: "competition-filter-card__body", children: [_jsxs(Tag, { children: ["\u8D5B\u6BB5\uFF1A", stageName || idValue(stageId)] }), _jsxs(Tag, { children: ["\u5DE5\u79CD\uFF1A", eventWorkTypeName || idValue(eventWorkTypeId)] }), _jsxs(Tag, { children: ["\u7B49\u7EA7\uFF1A", eventWorkTypeLevelName
                                        ? `${eventWorkTypeLevelName}（${eventWorkTypeLevelCode}）`
                                        : eventWorkTypeLevelCode || '未选择'] }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    void snapshotsQuery.refetch();
                                    void bindingsQuery.refetch();
                                    void assessmentSheetsQuery.refetch();
                                }, children: "\u5237\u65B0" })] })] }), !eventWorkTypeLevelId && (_jsx(Alert, { style: { marginTop: 16 }, type: "warning", showIcon: true, message: "\u5F53\u524D\u5DE5\u79CD\u7B49\u7EA7\u5C1A\u672A\u89E3\u6790\uFF0C\u6682\u65F6\u4E0D\u80FD\u52A0\u8F7D\u5B9E\u64CD\u8BD5\u5377\u3002" })), _jsx(Tabs, { style: { marginTop: 16 }, defaultActiveKey: initialTab, items: [
                    ...(canViewPaper && canViewStagePlan ? [{
                            key: 'paper',
                            label: '试卷绑定',
                            children: _jsxs(Card, { size: "small", title: "\u5B9E\u64CD\u8BD5\u5377\u7ED1\u5B9A", children: [_jsx(Alert, { style: { marginBottom: 12 }, type: "info", showIcon: true, message: "\u70B9\u51FB\u8BD5\u5377\u5373\u53EF\u7ED1\u5B9A", description: "\u4EC5\u5C55\u793A\u5F53\u524D\u8D5B\u4E8B\u5DE5\u79CD\u7B49\u7EA7\u4E0B\u5DF2\u6709\u9898\u76EE\u7684\u5DF2\u53D1\u5E03\u5B9E\u64CD\u7EC4\u5377\u5FEB\u7167\uFF1B\u7ED1\u5B9A\u53EA\u5EFA\u7ACB\u8D5B\u6BB5\u5173\u8054\uFF0C\u4E0D\u4F9D\u8D56\u6267\u884C\u65B9\u6848\u3001\u7ED1\u5B9A\u573A\u6B21\u6216\u8D5B\u4E8B\u8BC4\u5206\u89C4\u5219\u3002" }), _jsx(Table, { rowKey: (row) => idValue(row.id), size: "small", pagination: false, loading: snapshotsQuery.isLoading || snapshotsQuery.isFetching
                                            || bindingsQuery.isLoading || bindingsQuery.isFetching || bindMutation.isPending, dataSource: practicalSnapshots, onRow: (record) => ({
                                            onClick: () => bindPaper(record.id),
                                            style: canManageStagePlan && !bindMutation.isPending
                                                ? { cursor: 'pointer' }
                                                : undefined,
                                        }), locale: { emptyText: '当前范围暂无已发布实操组卷快照' }, columns: [
                                            {
                                                title: '试卷',
                                                dataIndex: 'paperCode',
                                                width: 90,
                                                render: (value) => _jsxs(Tag, { color: "orange", children: [value, " \u5377"] }),
                                            },
                                            { title: '版本', dataIndex: 'versionNo', width: 80, render: (value) => `v${value}` },
                                            { title: '组卷计划', dataIndex: 'title', ellipsis: true },
                                            { title: '题量', dataIndex: 'questionCount', width: 80 },
                                            { title: '总分', dataIndex: 'totalScore', width: 80 },
                                            {
                                                title: '时长',
                                                dataIndex: 'durationMinutes',
                                                width: 90,
                                                render: (value) => `${value} 分钟`,
                                            },
                                            {
                                                title: '绑定状态',
                                                width: 120,
                                                render: (_, row) => boundPaperIds.has(idValue(row.id))
                                                    ? _jsx(Tag, { color: "success", children: "\u5DF2\u7ED1\u5B9A" })
                                                    : _jsx(Tag, { children: canManageStagePlan ? '点击绑定' : '只读' }),
                                            },
                                        ] })] }),
                        }] : []),
                    ...(canViewAssessment ? [{
                            key: 'assessment',
                            label: `考评表配置（${assessmentSheetsQuery.data?.length ?? 0}）`,
                            children: _jsxs(Card, { size: "small", title: "\u8003\u8BC4\u8868\u914D\u7F6E", children: [_jsx(Alert, { style: { marginBottom: 12 }, type: "success", showIcon: true, message: "\u8003\u8BC4\u8868\u968F\u8BD5\u5377\u7ED1\u5B9A\u81EA\u52A8\u751F\u6210", description: "\u8003\u8BC4\u8868\u4F7F\u7528\u5DF2\u53D1\u5E03\u8BD5\u5377\u9898\u76EE\u5FEB\u7167\uFF0C\u6B63\u5F0F\u6BD4\u8D5B\u521D\u59CB\u5316\u540E\u4ECD\u4FDD\u6301\u7248\u672C\u4E0D\u53D8\u3002\u70B9\u51FB\u67E5\u770B\u53EF\u6838\u5BF9\u603B\u8BC4\u5206\u8868\u548C\u9010\u9898\u8BC4\u5206\u8868\u3002" }), _jsx(Table, { rowKey: (row) => idValue(row.id), size: "small", pagination: false, loading: assessmentSheetsQuery.isLoading || assessmentSheetsQuery.isFetching, dataSource: assessmentSheetsQuery.data ?? [], locale: { emptyText: '尚未生成考评表，请先在“试卷绑定”中绑定实操试卷' }, columns: [
                                            { title: '考评表编号', dataIndex: 'sheetCode', width: 170 },
                                            { title: '考评表名称', dataIndex: 'title', ellipsis: true },
                                            { title: '试卷', dataIndex: 'paperCode', width: 90 },
                                            { title: '版本', dataIndex: 'versionNo', width: 70, render: (value) => `v${value}` },
                                            { title: '题量', dataIndex: 'questionCount', width: 70 },
                                            { title: '总分', dataIndex: 'totalScore', width: 70 },
                                            { title: '状态', dataIndex: 'status', width: 90, render: () => _jsx(Tag, { color: "success", children: "\u5DF2\u53D1\u5E03" }) },
                                            {
                                                title: '操作',
                                                width: 90,
                                                render: (_, row) => _jsx(Button, { type: "link", onClick: () => setPreviewSheet(row), children: "\u67E5\u770B" }),
                                            },
                                        ] })] }),
                        }] : []),
                    ...(canViewStagePlan ? [{
                            key: 'runtime',
                            label: '运行规则',
                            children: _jsxs(Card, { size: "small", title: "\u8D5B\u6BB5\u5B9E\u64CD\u8FD0\u884C\u89C4\u5219", children: [_jsx(Alert, { style: { marginBottom: 12 }, type: "info", showIcon: true, message: "\u8FD9\u91CC\u914D\u7F6E\u672C\u8D5B\u6BB5\u5982\u4F55\u8003\u8BD5\uFF1B\u8BC4\u5206\u5185\u5BB9\u6765\u81EA\u5DF2\u53D1\u5E03\u7684\u5B9E\u64CD\u8BC4\u5206\u65B9\u6848\u3002", description: "\u8FD0\u884C\u89C4\u5219\u4F1A\u968F\u8D5B\u6BB5\u6267\u884C\u8BA1\u5212\u56FA\u5316\uFF0C\u5DF2\u53D1\u5E03\u8BA1\u5212\u4F7F\u7528\u81EA\u5DF1\u7684\u914D\u7F6E\u5FEB\u7167\u3002" }), _jsx(CompetitionPracticalExecutionConfigPanel, { eventId: eventId, stageId: stageId, stageName: stageName, eventWorkTypeId: eventWorkTypeId, eventWorkTypeLevelId: eventWorkTypeLevelId || undefined, editable: canManageStagePlan })] }),
                        }] : []),
                    ...(canViewReferee ? [{
                            key: 'referee',
                            label: '裁判配置',
                            children: _jsx(CompetitionRefereeAssignmentPanel, { stageId: stageId }),
                        }] : []),
                ] }), _jsx(Drawer, { width: 960, open: Boolean(previewSheet), title: previewSheet ? `${previewSheet.title}（${previewSheet.sheetCode}）` : '考评表', onClose: () => setPreviewSheet(null), children: previewSheet && _jsx(AssessmentSheetPreview, { sheet: previewSheet }) })] }));
}
function AssessmentSheetPreview({ sheet }) {
    const summary = sheet.sheet.summary ?? [];
    const questions = sheet.sheet.questions ?? [];
    return _jsxs(Space, { direction: "vertical", size: "large", style: { width: '100%' }, children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 5, children: "\u8BD5\u9898\u603B\u8BC4\u5206\u8868" }), _jsx(Table, { rowKey: (row) => idValue(row.paperQuestionId), size: "small", pagination: false, dataSource: summary, columns: [
                            { title: '序号', dataIndex: 'sortOrder', width: 70 },
                            { title: '试题名称', dataIndex: 'title' },
                            { title: '配分', dataIndex: 'score', width: 80 },
                            { title: '得分', width: 80, render: () => '—' },
                            { title: '权重', dataIndex: 'weight', width: 80, render: (value) => `${value}%` },
                            { title: '最后得分', width: 100, render: () => '—' },
                            { title: '备注', width: 100, render: () => '—' },
                        ] })] }), questions.map((question) => _jsxs(Card, { size: "small", title: `第 ${question.sortOrder} 题 · ${question.title}`, extra: _jsxs(Tag, { color: "orange", children: ["\u914D\u5206 ", String(question.score)] }), children: [_jsx(Typography.Text, { type: "secondary", children: "\u9010\u9898\u8BC4\u5206\u8868\uFF08\u73B0\u573A\u68C0\u6D4B\u3001\u6263\u5206\u3001\u5F97\u5206\u548C\u5907\u6CE8\u5728\u88C1\u5224\u7AEF\u586B\u5199\uFF09" }), _jsx(Table, { style: { marginTop: 12 }, rowKey: (row) => String(row.itemNo), size: "small", pagination: false, scroll: { x: 1050 }, dataSource: Array.isArray(question.scoreItems) ? question.scoreItems : [], columns: [
                            { title: '序号', dataIndex: 'itemNo', width: 65 },
                            { title: '考试内容', width: 150, render: () => question.title },
                            { title: '评分要素', width: 180, render: (_, row) => plainText(row.itemName || row.elementHtml) || '-' },
                            { title: '配分', dataIndex: 'maxScore', width: 70 },
                            { title: '评分标准', width: 240, render: (_, row) => plainText(row.criterion || row.standardHtml) || '-' },
                            { title: '最大扣分', dataIndex: 'maxDeduction', width: 90, render: (value) => value ?? '-' },
                            { title: '步长', dataIndex: 'deductionStep', width: 70, render: (value) => value ?? '-' },
                            { title: '检测结果', width: 90, render: () => '—' },
                            { title: '扣分', width: 70, render: () => '—' },
                            { title: '得分', width: 70, render: () => '—' },
                            { title: '备注', width: 90, render: () => '—' },
                        ] })] }, idValue(question.paperQuestionId)))] });
}

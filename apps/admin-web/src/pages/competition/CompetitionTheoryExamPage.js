import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DownloadOutlined, PlusOutlined, ReloadOutlined, SafetyCertificateOutlined, SendOutlined, StopOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, DatePicker, Descriptions, Form, Input, InputNumber, Modal, Result, Select, Space, Spin, Table, Tabs, Tag, Typography, message, } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { createCompetitionTheorySchedule, deleteCompetitionTheoryRoom, deleteCompetitionTheorySession, downloadCompetitionTheoryRoster, getCompetitionEvent, getCompetitionTheoryPaperPreview, getCompetitionTheorySchedule, getCompetitionTheorySchedulePreflight, importCompetitionTheoryPaperSnapshots, listCompetitionEventWorkTypes, listCompetitionEventWorkTypeLevels, listCompetitionScoreRules, listCompetitionTheoryMonitor, listCompetitionTheoryPaperSnapshots, listCompetitionTheoryResults, listCompetitionTheoryRooms, listCompetitionTheorySchedules, pageCompetitionStages, performCompetitionTheoryMonitorAction, publishCompetitionTheorySchedule, randomAllocateCompetitionTheorySchedule, saveCompetitionTheoryRoom, saveCompetitionTheorySession, withdrawCompetitionTheorySchedule, } from '../../api/competition';
import { listCompetitionMakeExamSnapshotsForScope, } from '../../api/competitionQuestion';
import { useAuthStore } from '../../stores/authStore';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import { CompetitionPlayerPaperAssignmentPage } from './CompetitionPlayerPaperAssignmentPage';
import './CompetitionEventManagementPage.css';
const questionTypeLabels = {
    single: '单选题',
    multi: '多选题',
    multiple: '多选题',
    judge: '判断题',
};
const previewQuestionText = (value) => {
    if (!value)
        return '';
    const source = value
        .replace(/<br\s*\/?>/gi, ' ')
        .replace(/<\/(?:p|div|li|h[1-6])>/gi, ' ');
    const parsed = new DOMParser().parseFromString(source, 'text/html');
    return (parsed.body.textContent ?? '').replace(/\s+/g, ' ').trim();
};
const monitorActionLabel = {
    'force-offline': '强制下线',
    'force-submit': '强制交卷',
    'restore-absence': '恢复暂定缺考',
};
function idValue(value) {
    return value === undefined || value === null ? '' : String(value);
}
function formatDate(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}
function questionTypeText(value) {
    return value ? questionTypeLabels[value] || value : '-';
}
function statusTag(status) {
    const color = status === 'published' || status === 'submitted_manual' || status === 'submitted_auto'
        ? 'success'
        : status === 'draft' || status === 'waiting' || status === 'ready'
            ? 'default'
            : status === 'in_progress' || status === 'open'
                ? 'processing'
                : status === 'provisional_absent'
                    ? 'warning'
                    : 'error';
    return _jsx(Tag, { color: color, children: status || '-' });
}
function downloadBlob(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}
export function CompetitionTheoryExamPage({ eventId, onNavigate, initialStageId, initialEventWorkTypeId, initialEventWorkTypeLevelCode, embedded = false, stagePlanningEditor = false, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canViewEvent = permissions.includes('competition:event:view');
    const canViewPaper = permissions.includes('competition:theory-paper:view');
    const canManagePaper = permissions.includes('competition:theory-paper:manage');
    const canViewPlayerAssignment = permissions.includes('competition:stage-paper-assignment:view');
    const canViewSchedule = permissions.includes('competition:theory-schedule:view');
    const canManageSchedule = permissions.includes('competition:theory-schedule:manage');
    const canPublishSchedule = permissions.includes('competition:theory-schedule:publish');
    const canExportRoster = permissions.includes('competition:theory-roster:export');
    const canViewMonitor = permissions.includes('competition:theory-monitor:view');
    const canForceOffline = permissions.includes('competition:theory-monitor:force-offline');
    const canForceSubmit = permissions.includes('competition:theory-monitor:force-submit');
    const canRestoreAbsence = permissions.includes('competition:theory-monitor:restore-absence');
    const canViewResults = permissions.includes('competition:theory-result:view');
    const canReadDimensions = permissions.includes('competition:stage:view')
        && permissions.includes('competition:event-work-type:view');
    const canReadScoreRules = permissions.includes('competition:score-rule:view');
    const hasWorkspacePermission = canViewPaper || canViewSchedule || canViewMonitor || canViewResults;
    const [activeTab, setActiveTab] = useState('paper');
    const [stageId, setStageId] = useState(initialStageId);
    // 原始工种 state（可能短暂持有跨赛事的旧 id，来自 URL 参数）；下游一律使用下方派生的 effectiveEventWorkTypeId
    const [rawEventWorkTypeId, setEventWorkTypeId] = useState(initialEventWorkTypeId);
    const [eventWorkTypeLevelId, setEventWorkTypeLevelId] = useState();
    const [scoreRuleId, setScoreRuleId] = useState();
    const [selectedVersionId, setSelectedVersionId] = useState();
    const [previewSnapshotId, setPreviewSnapshotId] = useState();
    const [playerAssignmentOpen, setPlayerAssignmentOpen] = useState(false);
    const [playerAssignmentDirty, setPlayerAssignmentDirty] = useState(false);
    const [roomOpen, setRoomOpen] = useState(false);
    const [editingRoom, setEditingRoom] = useState();
    const [sessionOpen, setSessionOpen] = useState(false);
    const [editingSession, setEditingSession] = useState();
    const [monitorAction, setMonitorAction] = useState();
    const [withdrawOpen, setWithdrawOpen] = useState(false);
    const [roomForm] = Form.useForm();
    const [sessionForm] = Form.useForm();
    const [actionForm] = Form.useForm();
    const [withdrawForm] = Form.useForm();
    const queryClient = useQueryClient();
    const eventQuery = useQuery({
        queryKey: ['competition-event', eventId],
        queryFn: () => getCompetitionEvent(eventId),
        enabled: canViewEvent,
    });
    const workTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', eventId],
        queryFn: () => listCompetitionEventWorkTypes(eventId),
        enabled: canViewEvent && canReadDimensions,
    });
    // 渲染期派生有效工种：跨赛事/URL 参数混合时，未在当前赛事工种列表中的 id 当帧钳为 undefined，
    // 全部查询/操作以此值为准，混合帧内不会发出请求（effect 事后纠偏存在一帧竞态）
    const eventWorkTypeId = rawEventWorkTypeId
        && (workTypesQuery.data || []).some((item) => idValue(item.id) === rawEventWorkTypeId)
        ? rawEventWorkTypeId
        : undefined;
    const scopeReady = Boolean(stageId && eventWorkTypeId && scoreRuleId);
    const paperScopeReady = Boolean(stageId && eventWorkTypeId);
    const stagesQuery = useQuery({
        queryKey: ['competition-stages', eventId],
        queryFn: () => pageCompetitionStages(eventId, {
            page: 1,
            pageSize: 500,
            // A theory page opened from a stage canvas must read that same scoped stage.
            // The standalone theory page keeps its historical event-level behavior.
            eventWorkTypeId: initialEventWorkTypeLevelCode ? eventWorkTypeId : undefined,
            eventWorkTypeLevelCode: initialEventWorkTypeLevelCode,
        }),
        enabled: canViewEvent && canReadDimensions,
    });
    const levelsQuery = useQuery({
        queryKey: ['competition-event-work-type-levels', eventId, eventWorkTypeId],
        queryFn: () => listCompetitionEventWorkTypeLevels(eventId, eventWorkTypeId),
        enabled: canViewEvent && canReadDimensions && Boolean(eventWorkTypeId),
    });
    const rulesQuery = useQuery({
        queryKey: ['competition-score-rules', eventId, stageId, eventWorkTypeId],
        queryFn: () => listCompetitionScoreRules(eventId, { stageId, eventWorkTypeId }),
        enabled: canReadScoreRules && Boolean(stageId && eventWorkTypeId),
    });
    useEffect(() => {
        const stages = stagesQuery.data?.items || [];
        if (!stages.length)
            return;
        if (!stageId || !stages.some((stage) => idValue(stage.id) === stageId)) {
            setStageId(idValue(stages[0].id));
        }
    }, [stageId, stagesQuery.data?.items]);
    useEffect(() => {
        const workTypes = workTypesQuery.data || [];
        if (!workTypes.length)
            return;
        if (!rawEventWorkTypeId || !workTypes.some((workType) => idValue(workType.id) === rawEventWorkTypeId)) {
            setEventWorkTypeId(idValue(workTypes[0].id));
        }
    }, [rawEventWorkTypeId, workTypesQuery.data]);
    useEffect(() => {
        const levels = levelsQuery.data || [];
        if (!levels.length) {
            setEventWorkTypeLevelId(undefined);
            return;
        }
        const preferred = initialEventWorkTypeLevelCode
            ? levels.find((level) => level.levelCode === initialEventWorkTypeLevelCode)
            : undefined;
        if (!eventWorkTypeLevelId || !levels.some((level) => idValue(level.id) === eventWorkTypeLevelId)) {
            setEventWorkTypeLevelId(idValue((preferred || levels[0]).id));
        }
    }, [eventWorkTypeLevelId, initialEventWorkTypeLevelCode, levelsQuery.data]);
    useEffect(() => {
        const rules = rulesQuery.data || [];
        if (!rules.some((rule) => idValue(rule.id) === scoreRuleId)) {
            const preferred = rules.find((rule) => rule.status === 'published') || rules[0];
            setScoreRuleId(preferred ? idValue(preferred.id) : undefined);
        }
    }, [rulesQuery.data, scoreRuleId]);
    const competitionSnapshotsQuery = useQuery({
        queryKey: ['competition-make-snapshots-for-theory-scope', eventId, eventWorkTypeId, eventWorkTypeLevelId],
        queryFn: () => listCompetitionMakeExamSnapshotsForScope(eventId, {
            eventWorkTypeId: eventWorkTypeId,
            eventWorkTypeLevelId: eventWorkTypeLevelId,
            contentType: 'theory',
        }),
        enabled: canViewPaper && Boolean(eventWorkTypeId && eventWorkTypeLevelId),
    });
    const snapshotsQuery = useQuery({
        queryKey: ['competition-theory-paper-snapshots', eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId],
        queryFn: () => listCompetitionTheoryPaperSnapshots(eventId, {
            stageId,
            eventWorkTypeId,
            eventWorkTypeLevelId,
        }),
        enabled: canViewPaper && paperScopeReady,
    });
    const previewQuery = useQuery({
        queryKey: ['competition-theory-paper-preview', eventId, previewSnapshotId],
        queryFn: () => getCompetitionTheoryPaperPreview(eventId, previewSnapshotId),
        enabled: canViewPaper && Boolean(previewSnapshotId),
    });
    const roomsQuery = useQuery({
        queryKey: ['competition-theory-rooms', eventId],
        queryFn: () => listCompetitionTheoryRooms(eventId),
        enabled: canViewSchedule,
    });
    const schedulesQuery = useQuery({
        queryKey: ['competition-theory-schedules', eventId, stageId, eventWorkTypeId],
        queryFn: () => listCompetitionTheorySchedules(eventId, { stageId, eventWorkTypeId }),
        enabled: canViewSchedule && Boolean(stageId && eventWorkTypeId),
    });
    useEffect(() => {
        const versions = schedulesQuery.data || [];
        if (!versions.some((version) => idValue(version.id) === selectedVersionId)) {
            setSelectedVersionId(versions[0] ? idValue(versions[0].id) : undefined);
        }
    }, [schedulesQuery.data, selectedVersionId]);
    const scheduleQuery = useQuery({
        queryKey: ['competition-theory-schedule', selectedVersionId],
        queryFn: () => getCompetitionTheorySchedule(selectedVersionId),
        enabled: canViewSchedule && Boolean(selectedVersionId),
    });
    const preflightQuery = useQuery({
        queryKey: ['competition-theory-preflight', selectedVersionId],
        queryFn: () => getCompetitionTheorySchedulePreflight(selectedVersionId),
        enabled: canViewSchedule && Boolean(selectedVersionId),
    });
    const monitorQuery = useQuery({
        queryKey: ['competition-theory-monitor', selectedVersionId],
        queryFn: () => listCompetitionTheoryMonitor(selectedVersionId),
        enabled: canViewMonitor && Boolean(selectedVersionId) && activeTab === 'monitor',
        refetchInterval: 5000,
    });
    const resultsQuery = useQuery({
        queryKey: ['competition-theory-results', selectedVersionId],
        queryFn: () => listCompetitionTheoryResults(selectedVersionId),
        enabled: canViewResults && Boolean(selectedVersionId) && activeTab === 'monitor',
    });
    const refreshSchedule = async () => {
        await Promise.all([
            queryClient.invalidateQueries({ queryKey: ['competition-theory-schedules'] }),
            queryClient.invalidateQueries({ queryKey: ['competition-theory-schedule', selectedVersionId] }),
            queryClient.invalidateQueries({ queryKey: ['competition-theory-preflight', selectedVersionId] }),
        ]);
    };
    const competitionImportMutation = useMutation({
        mutationFn: (paperSnapshotIds) => {
            const selectedIds = paperSnapshotIds.filter((id, index, all) => all.indexOf(id) === index);
            if (selectedIds.length < 1)
                throw new Error('至少选择一套已发布竞赛组卷快照');
            return importCompetitionTheoryPaperSnapshots(eventId, {
                stageId: stageId,
                eventWorkTypeId: eventWorkTypeId,
                eventWorkTypeLevelId: eventWorkTypeLevelId,
                paperSnapshotIds: selectedIds,
            });
        },
        onSuccess: () => {
            message.success('竞赛组卷快照已绑定并生成理论执行快照');
            queryClient.invalidateQueries({ queryKey: ['competition-theory-paper-snapshots'] });
        },
    });
    const roomMutation = useMutation({
        mutationFn: (payload) => saveCompetitionTheoryRoom(eventId, editingRoom?.id, payload),
        onSuccess: () => {
            message.success('考场已保存');
            setRoomOpen(false);
            setEditingRoom(undefined);
            roomForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['competition-theory-rooms', eventId] });
        },
    });
    const createScheduleMutation = useMutation({
        mutationFn: () => createCompetitionTheorySchedule(eventId, {
            stageId: stageId, eventWorkTypeId: eventWorkTypeId, scoreRuleId: scoreRuleId,
        }),
        onSuccess: async (version) => {
            message.success('排考版本已创建');
            setSelectedVersionId(idValue(version.id));
            await refreshSchedule();
        },
    });
    const sessionMutation = useMutation({
        mutationFn: (values) => saveCompetitionTheorySession(selectedVersionId, editingSession?.id, {
            sessionCode: values.sessionCode,
            sessionName: values.sessionName,
            paperSnapshotId: values.paperSnapshotId,
            startAt: values.timeRange[0].format('YYYY-MM-DDTHH:mm:ss'),
            endAt: values.timeRange[1].format('YYYY-MM-DDTHH:mm:ss'),
            roomIds: values.roomIds,
        }),
        onSuccess: async () => {
            message.success('考试场次已保存');
            setSessionOpen(false);
            setEditingSession(undefined);
            sessionForm.resetFields();
            await refreshSchedule();
        },
    });
    const scheduleActionMutation = useMutation({
        mutationFn: (action) => action === 'allocate'
            ? randomAllocateCompetitionTheorySchedule(selectedVersionId)
            : publishCompetitionTheorySchedule(selectedVersionId),
        onSuccess: async (_, action) => {
            message.success(action === 'allocate' ? '随机分配已完成' : '排考已发布');
            await refreshSchedule();
        },
    });
    const withdrawMutation = useMutation({
        mutationFn: ({ reason }) => withdrawCompetitionTheorySchedule(selectedVersionId, reason),
        onSuccess: async () => {
            message.success('排考已撤回，历史版本与原因已保留');
            setWithdrawOpen(false);
            withdrawForm.resetFields();
            await refreshSchedule();
        },
    });
    const monitorActionMutation = useMutation({
        mutationFn: ({ action, attemptId, reason }) => performCompetitionTheoryMonitorAction(attemptId, action, reason),
        onSuccess: (_, variables) => {
            message.success(`${monitorActionLabel[variables.action]}已执行`);
            setMonitorAction(undefined);
            actionForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['competition-theory-monitor', selectedVersionId] });
            queryClient.invalidateQueries({ queryKey: ['competition-theory-results', selectedVersionId] });
        },
    });
    const selectedSchedule = scheduleQuery.data;
    const snapshotOptions = (snapshotsQuery.data || []).filter((snapshot) => snapshot.status === 'published').map((snapshot) => ({
        label: `${snapshot.paperCode}卷 v${snapshot.versionNo} · ${snapshot.sourceTitle}`,
        value: idValue(snapshot.id),
    }));
    const competitionSnapshots = (competitionSnapshotsQuery.data || [])
        .filter((snapshot) => snapshot.status === 'published' && snapshot.questionCount > 0);
    const selectedCompetitionSnapshotIds = new Set((snapshotsQuery.data || [])
        .filter((snapshot) => snapshot.status === 'published' && snapshot.sourceCompetitionPaperSnapshotId != null)
        .map((snapshot) => idValue(snapshot.sourceCompetitionPaperSnapshotId)));
    const bindCompetitionSnapshot = (snapshotId) => {
        if (!canManagePaper || competitionImportMutation.isPending)
            return;
        if (!stageId || !eventWorkTypeId || !eventWorkTypeLevelId) {
            message.warning('请先选择赛段、工种和等级，再绑定竞赛组卷快照');
            return;
        }
        const clickedId = idValue(snapshotId);
        if (!clickedId || selectedCompetitionSnapshotIds.has(clickedId)) {
            if (clickedId && selectedCompetitionSnapshotIds.has(clickedId)) {
                message.info('该试卷已绑定到当前赛段');
            }
            return;
        }
        const existingIds = competitionSnapshots
            .filter((snapshot) => selectedCompetitionSnapshotIds.has(idValue(snapshot.id)))
            .map((snapshot) => idValue(snapshot.id));
        competitionImportMutation.mutate([...existingIds, clickedId]);
    };
    const roomOptions = (roomsQuery.data || []).filter((room) => room.status === 'enabled').map((room) => ({
        label: `${room.name}（${room.capacity}人）`, value: idValue(room.id),
    }));
    const versionOptions = (schedulesQuery.data || []).map((version) => ({
        label: `v${version.versionNo} · ${version.status}`, value: idValue(version.id),
    }));
    const paperContent = (_jsx(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: !canViewPaper ? _jsx(Result, { status: "403", title: "\u6682\u65E0\u8BD5\u5377\u7BA1\u7406\u67E5\u770B\u6743\u9650" }) : (_jsx(_Fragment, { children: _jsxs(Card, { size: "small", title: "\u7ADE\u8D5B\u7EC4\u5377\u5FEB\u7167\uFF08\u65B0\u7ADE\u8D5B\u9898\u5E93\uFF09", extra: _jsx(Space, { children: canViewPlayerAssignment && _jsx(Button, { disabled: !stageId || !eventWorkTypeId || !eventWorkTypeLevelId || snapshotsQuery.data?.filter((snapshot) => snapshot.status === 'published').length === 0, onClick: () => setPlayerAssignmentOpen(true), children: "\u914D\u7F6E\u9009\u624B\u8BD5\u5377" }) }), children: [_jsx(Alert, { style: { marginBottom: 12 }, type: "info", showIcon: true, message: "\u6BCF\u4E2A\u7ADE\u8D5B\u7EC4\u5377\u8BA1\u5212\u53EA\u751F\u6210\u4E00\u5957\u8BD5\u5377\uFF1B\u70B9\u51FB\u4E0B\u65B9\u8BD5\u5377\u884C\u5373\u53EF\u76F4\u63A5\u7ED1\u5B9A\uFF0C\u5DF2\u7ED1\u5B9A\u8BD5\u5377\u4F1A\u4FDD\u7559\u3002A/B \u6216\u66F4\u591A\u5957\u8BD5\u5377\u8BF7\u5206\u522B\u521B\u5EFA\u591A\u4E2A\u8BA1\u5212\u540E\u4F9D\u6B21\u70B9\u51FB\u7ED1\u5B9A\u3002" }), !eventWorkTypeLevelId && _jsx(Alert, { style: { marginBottom: 12 }, type: "warning", showIcon: true, message: "\u5F53\u524D\u5DE5\u79CD\u6682\u65E0\u7B49\u7EA7\uFF0C\u65E0\u6CD5\u7ED1\u5B9A\u7ADE\u8D5B\u7EC4\u5377\u5FEB\u7167\u3002" }), eventWorkTypeLevelId && !competitionSnapshotsQuery.isLoading && competitionSnapshots.length === 0 && _jsx(Alert, { style: { marginBottom: 12 }, type: "warning", showIcon: true, message: "\u5F53\u524D\u8D5B\u4E8B\u3001\u5DE5\u79CD\u3001\u7B49\u7EA7\u6682\u65E0\u5DF2\u53D1\u5E03\u7684\u7ADE\u8D5B\u7406\u8BBA\u7EC4\u5377\u8BD5\u5377\u3002" }), _jsx(Table, { rowKey: (row) => idValue(row.id), size: "small", pagination: false, loading: competitionSnapshotsQuery.isLoading || competitionImportMutation.isPending, dataSource: competitionSnapshots, onRow: (record) => ({
                            onClick: () => bindCompetitionSnapshot(record.id),
                            style: canManagePaper && !competitionImportMutation.isPending ? { cursor: 'pointer' } : undefined,
                        }), columns: [
                            { title: '试卷', dataIndex: 'paperCode', width: 70, render: (value) => _jsxs(Tag, { color: "purple", children: [value, " \u5377"] }) },
                            { title: '版本', dataIndex: 'versionNo', width: 80, render: (value) => `v${value}` },
                            { title: '组卷计划', dataIndex: 'title', ellipsis: true },
                            { title: '题量', dataIndex: 'questionCount', width: 80 },
                            { title: '总分', dataIndex: 'totalScore', width: 80 },
                            { title: '时长', dataIndex: 'durationMinutes', width: 90, render: (value) => `${value} 分钟` },
                            { title: '状态', width: 100, render: (_, row) => selectedCompetitionSnapshotIds.has(idValue(row.id)) ? _jsx(Tag, { color: "success", children: "\u5DF2\u7ED1\u5B9A" }) : _jsx(Tag, { children: "\u70B9\u51FB\u7ED1\u5B9A" }) },
                        ] })] }) })) }));
    const scheduleToolbar = (_jsxs(Space, { wrap: true, children: [_jsx(Select, { style: { width: 190 }, placeholder: "\u9009\u62E9\u6392\u8003\u7248\u672C", value: selectedVersionId, onChange: setSelectedVersionId, options: versionOptions }), canManageSchedule && _jsx(Button, { icon: _jsx(PlusOutlined, {}), disabled: !scopeReady, loading: createScheduleMutation.isPending, onClick: () => createScheduleMutation.mutate(), children: "\u65B0\u5EFA\u7248\u672C" }), canManageSchedule && _jsx(Button, { disabled: !selectedVersionId || selectedSchedule?.status !== 'draft', onClick: () => { setEditingSession(undefined); sessionForm.resetFields(); setSessionOpen(true); }, children: "\u65B0\u589E\u573A\u6B21" }), canManageSchedule && _jsx(Button, { disabled: !selectedVersionId || selectedSchedule?.status !== 'draft', onClick: () => scheduleActionMutation.mutate('allocate'), children: "\u968F\u673A\u5206\u914D" }), canPublishSchedule && _jsx(Button, { type: "primary", icon: _jsx(SendOutlined, {}), disabled: !preflightQuery.data?.readyToPublish || selectedSchedule?.status !== 'draft', onClick: () => Modal.confirm({ title: '确认发布排考？', content: '发布后选手端将读取本版本，既有已发布版本会按服务端规则处理。', okText: '确认发布', onOk: () => scheduleActionMutation.mutateAsync('publish') }), children: "\u53D1\u5E03" }), canPublishSchedule && _jsx(Button, { danger: true, icon: _jsx(StopOutlined, {}), disabled: selectedSchedule?.status !== 'published', onClick: () => setWithdrawOpen(true), children: "\u64A4\u56DE" })] }));
    const scheduleContent = !canViewSchedule ? _jsx(Result, { status: "403", title: "\u6682\u65E0\u6392\u8003\u67E5\u770B\u6743\u9650" }) : (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsxs(Card, { size: "small", title: "\u6392\u8003\u7248\u672C", extra: scheduleToolbar, children: [selectedSchedule ? (_jsxs(Descriptions, { size: "small", column: 5, children: [_jsxs(Descriptions.Item, { label: "\u7248\u672C", children: ["v", selectedSchedule.versionNo] }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: statusTag(selectedSchedule.status) }), _jsxs(Descriptions.Item, { label: "\u5E94\u6392/\u5DF2\u6392", children: [selectedSchedule.eligibleCandidateCount, " / ", selectedSchedule.assignedCandidateCount] }), _jsx(Descriptions.Item, { label: "\u603B\u5BB9\u91CF", children: selectedSchedule.totalCapacity }), _jsx(Descriptions.Item, { label: "\u5BB9\u91CF\u5DEE", children: selectedSchedule.capacityGap })] })) : _jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u8303\u56F4\u5C1A\u65E0\u6392\u8003\u7248\u672C" }), preflightQuery.data && !preflightQuery.data.readyToPublish && (_jsx(Alert, { style: { marginTop: 12 }, type: "warning", showIcon: true, message: "\u53D1\u5E03\u524D\u68C0\u67E5\u672A\u901A\u8FC7", description: preflightQuery.data.issues.join('；') || '请完成场次与人员分配。' }))] }), _jsx(Card, { size: "small", title: "\u8003\u573A", extra: canManageSchedule && _jsx(Button, { onClick: () => { setEditingRoom(undefined); roomForm.setFieldsValue({ status: 'enabled', capacity: 30 }); setRoomOpen(true); }, children: "\u65B0\u589E\u8003\u573A" }), children: _jsx(Table, { rowKey: (row) => idValue(row.id), size: "small", pagination: false, dataSource: roomsQuery.data || [], columns: [
                        { title: '名称', dataIndex: 'name' }, { title: '地址', dataIndex: 'address' }, { title: '容量', dataIndex: 'capacity', width: 90 },
                        { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
                        { title: '备注', dataIndex: 'remark' },
                        { title: '操作', width: 150, render: (_, room) => canManageSchedule && _jsxs(Space, { size: 0, children: [_jsx(Button, { type: "link", size: "small", onClick: () => { setEditingRoom(room); roomForm.setFieldsValue({ name: room.name, address: room.address, capacity: room.capacity, status: room.status, remark: room.remark || undefined }); setRoomOpen(true); }, children: "\u7F16\u8F91" }), _jsx(Button, { danger: true, type: "link", size: "small", onClick: () => Modal.confirm({ title: `删除考场“${room.name}”？`, okButtonProps: { danger: true }, onOk: async () => { await deleteCompetitionTheoryRoom(room.id); message.success('考场已删除'); queryClient.invalidateQueries({ queryKey: ['competition-theory-rooms', eventId] }); } }), children: "\u5220\u9664" })] }) },
                    ] }) }), _jsx(Card, { size: "small", title: "\u8003\u8BD5\u573A\u6B21", children: _jsx(Table, { rowKey: (row) => idValue(row.id), size: "small", pagination: false, dataSource: selectedSchedule?.sessions || [], columns: [
                        { title: '卷别', dataIndex: 'sessionCode', width: 70 }, { title: '场次', dataIndex: 'sessionName' },
                        { title: '开始', dataIndex: 'startAt', width: 175, render: formatDate }, { title: '结束', dataIndex: 'endAt', width: 175, render: formatDate },
                        { title: '考场', dataIndex: 'rooms', render: (rooms) => rooms.map((room) => room.roomName).join('、') || '-' },
                        { title: '容量', dataIndex: 'totalCapacity', width: 80 }, { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
                        { title: '操作', width: 150, render: (_, session) => canManageSchedule && selectedSchedule?.status === 'draft' && _jsxs(Space, { size: 0, children: [_jsx(Button, { type: "link", size: "small", onClick: () => { setEditingSession(session); sessionForm.setFieldsValue({ sessionCode: session.sessionCode, sessionName: session.sessionName, paperSnapshotId: idValue(session.paperSnapshotId), timeRange: [dayjs(session.startAt), dayjs(session.endAt)], roomIds: session.rooms.map((room) => idValue(room.roomId)) }); setSessionOpen(true); }, children: "\u7F16\u8F91" }), _jsx(Button, { danger: true, type: "link", size: "small", onClick: () => Modal.confirm({ title: `删除场次“${session.sessionName}”？`, okButtonProps: { danger: true }, onOk: async () => { await deleteCompetitionTheorySession(selectedVersionId, session.id); message.success('场次已删除'); await refreshSchedule(); } }), children: "\u5220\u9664" })] }) },
                    ] }) })] }));
    const rosterContent = !canViewSchedule ? _jsx(Result, { status: "403", title: "\u6682\u65E0\u540D\u518C\u67E5\u770B\u6743\u9650" }) : (_jsx(Card, { size: "small", title: "\u8003\u8BD5\u540D\u518C", extra: _jsx(Space, { children: canExportRoster && _jsxs(_Fragment, { children: [_jsx(Button, { icon: _jsx(DownloadOutlined, {}), disabled: !selectedVersionId, onClick: async () => downloadBlob(await downloadCompetitionTheoryRoster(selectedVersionId, 'xlsx'), 'competition-theory-roster.xlsx'), children: "Excel" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), disabled: !selectedVersionId, onClick: async () => downloadBlob(await downloadCompetitionTheoryRoster(selectedVersionId, 'pdf'), 'competition-theory-roster.pdf'), children: "PDF" })] }) }), children: _jsx(Table, { rowKey: (row) => idValue(row.assignmentId), size: "small", dataSource: selectedSchedule?.assignments || [], pagination: { pageSize: 20, showSizeChanger: false }, columns: [
                { title: '选手号', dataIndex: 'contestantNo', width: 120 }, { title: '姓名', dataIndex: 'contestantName', width: 110 },
                { title: '单位', dataIndex: 'orgName' }, { title: '工种', dataIndex: 'workTypeName', width: 140 },
                { title: '场次', dataIndex: 'sessionName', width: 140 }, { title: '考场', dataIndex: 'roomName', width: 140 },
                { title: '座位', dataIndex: 'seatNo', width: 90 }, { title: '开始时间', dataIndex: 'startAt', width: 175, render: formatDate },
            ] }) }));
    const actionAllowed = (action) => action === 'force-offline' ? canForceOffline : action === 'force-submit' ? canForceSubmit : canRestoreAbsence;
    const monitorContent = !canViewMonitor && !canViewResults ? _jsx(Result, { status: "403", title: "\u6682\u65E0\u76D1\u8003\u6216\u7ED3\u679C\u67E5\u770B\u6743\u9650" }) : (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Alert, { showIcon: true, type: "info", message: "\u76D1\u8003\u5217\u8868\u6BCF 5 \u79D2\u5237\u65B0\uFF1B\u4EC5\u5C55\u793A\u72B6\u6001\u4E0E\u6700\u540E\u4FDD\u5B58\u65F6\u95F4\uFF0C\u4E0D\u52A0\u8F7D\u7B54\u5377\u3001\u6807\u51C6\u7B54\u6848\u6216\u89E3\u6790\u3002" }), canViewMonitor && _jsx(Card, { size: "small", title: "\u5B9E\u65F6\u76D1\u8003", extra: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => monitorQuery.refetch(), children: "\u5237\u65B0" }), children: _jsx(Table, { rowKey: (row) => idValue(row.attemptId), size: "small", loading: monitorQuery.isFetching, dataSource: monitorQuery.data || [], pagination: { pageSize: 20, showSizeChanger: false }, columns: [
                        { title: '选手号', dataIndex: 'contestantNo', width: 110 }, { title: '姓名', dataIndex: 'contestantName', width: 100 },
                        { title: '单位', dataIndex: 'orgName' }, { title: '场次/座位', render: (_, row) => `${row.sessionName} / ${row.roomName} ${row.seatNo}` },
                        { title: '答题状态', dataIndex: 'status', width: 140, render: statusTag },
                        { title: '会话', dataIndex: 'examSessionOnline', width: 90, render: (online) => _jsx(Tag, { color: online ? 'success' : 'default', children: online ? '在线' : '离线' }) },
                        { title: '最后保存', dataIndex: 'lastSavedAt', width: 175, render: formatDate },
                        { title: '操作', width: 260, render: (_, row) => _jsx(Space, { size: 0, children: ['force-offline', 'force-submit', 'restore-absence'].filter(actionAllowed).map((action) => _jsx(Button, { type: "link", danger: action !== 'restore-absence', size: "small", disabled: action === 'restore-absence' && row.status !== 'provisional_absent', onClick: () => { actionForm.resetFields(); setMonitorAction({ action, record: row }); }, children: monitorActionLabel[action] }, action)) }) },
                    ] }) }), canViewResults && _jsx(Card, { size: "small", title: "\u7ED3\u679C\u6458\u8981\uFF08\u6210\u7EE9\u672A\u53D1\u5E03\uFF09", children: _jsx(Table, { rowKey: (row) => idValue(row.attemptId), size: "small", loading: resultsQuery.isLoading, dataSource: resultsQuery.data || [], pagination: { pageSize: 20, showSizeChanger: false }, columns: [
                        { title: '选手号', dataIndex: 'contestantNo' }, { title: '姓名', dataIndex: 'contestantName' }, { title: '状态', dataIndex: 'status', render: statusTag },
                        { title: '缺考', dataIndex: 'absent', render: (value) => value ? _jsx(Tag, { color: "error", children: "\u662F" }) : '否' },
                        { title: '客观题总分', dataIndex: 'totalScore', render: (value) => value ?? '-' }, { title: '确认状态', dataIndex: 'confirmationStatus', render: statusTag },
                        { title: '发布状态', dataIndex: 'published', render: () => _jsx(Tag, { children: "\u672A\u53D1\u5E03" }) }, { title: '交卷时间', dataIndex: 'submittedAt', render: formatDate },
                    ] }) })] }));
    if (!canViewEvent || !hasWorkspacePermission) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u7406\u8BBA\u8003\u8BD5\u5DE5\u4F5C\u53F0\u6743\u9650" });
    }
    if (eventQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (!eventQuery.data)
        return _jsx(Result, { status: "404", title: "\u8D5B\u4E8B\u4E0D\u5B58\u5728\u6216\u65E0\u6CD5\u8BBF\u95EE" });
    const content = (_jsxs(_Fragment, { children: [_jsxs(Card, { bordered: false, className: "competition-filter-card competition-filter-card--workspace", children: [_jsxs("div", { className: "competition-filter-card__head", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u7406\u8BBA\u8BD5\u5377\u4E0E\u8003\u8BD5\u6267\u884C" }), _jsx(Typography.Text, { type: "secondary", children: "\u6309\u9636\u6BB5\u3001\u5DE5\u79CD\u4E0E\u7B49\u7EA7\u7BA1\u7406\u7ADE\u8D5B\u72EC\u7ACB\u9898\u5E93\u7EC4\u5377\u5FEB\u7167\u548C\u8003\u8BD5\u6267\u884C" })] }), _jsxs(Tag, { color: "blue", children: [_jsx(SafetyCertificateOutlined, {}), " \u4E13\u7528\u8003\u8BD5\u8EAB\u4EFD\u57DF"] })] }), _jsxs(Space, { wrap: true, className: "competition-filter-card__body", children: [_jsx(Select, { style: { width: 220 }, placeholder: "\u9009\u62E9\u9636\u6BB5", value: stageId, onChange: (value) => { setStageId(value); setScoreRuleId(undefined); }, options: (stagesQuery.data?.items || []).map((stage) => ({ label: stage.name, value: idValue(stage.id) })) }), _jsx(Select, { style: { width: 220 }, placeholder: "\u9009\u62E9\u5DE5\u79CD", value: eventWorkTypeId, onChange: (value) => { setEventWorkTypeId(value); setScoreRuleId(undefined); }, options: (workTypesQuery.data || []).map((workType) => ({ label: workType.displayName || workType.workTypeName || idValue(workType.id), value: idValue(workType.id) })) }), _jsx(Select, { style: { width: 190 }, placeholder: "\u9009\u62E9\u7B49\u7EA7", value: eventWorkTypeLevelId, onChange: setEventWorkTypeLevelId, options: (levelsQuery.data || []).map((level) => ({ label: `${level.levelName}（${level.levelCode}）`, value: idValue(level.id) })) }), activeTab === 'schedule' && canReadScoreRules && _jsx(Select, { style: { width: 260 }, placeholder: "\u9009\u62E9\u8BC4\u5206\u89C4\u5219", value: scoreRuleId, onChange: setScoreRuleId, options: (rulesQuery.data || []).map((rule) => ({ label: `v${rule.ruleVersion} · ${rule.status}`, value: idValue(rule.id) })) }), !stagePlanningEditor && _jsx(Select, { style: { width: 190 }, placeholder: "\u9009\u62E9\u6392\u8003\u7248\u672C", allowClear: true, value: selectedVersionId, onChange: setSelectedVersionId, options: versionOptions })] })] }), !canReadDimensions && _jsx(Alert, { type: "warning", showIcon: true, message: "\u7F3A\u5C11\u9636\u6BB5\u6216\u5DE5\u79CD\u67E5\u770B\u6743\u9650\uFF0C\u65E0\u6CD5\u9009\u62E9\u8003\u8BD5\u8303\u56F4\u3002" }), _jsx(Card, { bordered: false, className: "competition-table-card", style: { marginTop: 16 }, children: stagePlanningEditor ? paperContent : (_jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                        { key: 'paper', label: '试卷', children: paperContent },
                        { key: 'schedule', label: '排考', children: scheduleContent },
                        { key: 'roster', label: '名册', children: rosterContent },
                        { key: 'monitor', label: '监考', children: monitorContent },
                    ] })) }), _jsx(Modal, { title: "\u9009\u624B\u914D\u7F6E \u00B7 \u8BD5\u5377\u5206\u914D", open: playerAssignmentOpen, onCancel: () => {
                    const close = () => { setPlayerAssignmentOpen(false); setPlayerAssignmentDirty(false); };
                    if (playerAssignmentDirty)
                        Modal.confirm({ title: '放弃未保存的选手分配调整？', onOk: close });
                    else
                        close();
                }, footer: null, centered: true, width: "min(1500px, calc(100vw - 32px))", className: "stage-player-paper-assignment-modal", styles: { body: { maxHeight: 'calc(100vh - 112px)', overflowY: 'auto', padding: 0 } }, destroyOnClose: true, children: playerAssignmentOpen && stageId && _jsx(CompetitionPlayerPaperAssignmentPage, { stageId: stageId, onDirtyChange: setPlayerAssignmentDirty }) }), _jsxs(Modal, { title: "\u8BD5\u5377\u9884\u89C8\uFF08\u9898\u5E72\u3001\u9898\u578B\u548C\u5206\u503C\uFF09", open: Boolean(previewSnapshotId), onCancel: () => setPreviewSnapshotId(undefined), footer: null, width: 680, destroyOnClose: true, children: [_jsx(Alert, { style: { marginBottom: 16 }, type: "info", showIcon: true, message: "\u9884\u89C8\u663E\u793A\u9898\u53F7\u3001\u9898\u5E72\u3001\u9898\u578B\u548C\u5206\u503C\uFF1B\u957F\u9898\u5E72\u4F1A\u7701\u7565\u5C55\u793A\uFF0C\u60AC\u505C\u53EF\u67E5\u770B\u5B8C\u6574\u5185\u5BB9\uFF0C\u4E0D\u663E\u793A\u9009\u9879\u3001\u6807\u51C6\u7B54\u6848\u6216\u89E3\u6790\u3002" }), previewQuery.isLoading ? _jsx(Spin, {}) : previewQuery.data ? (_jsxs(_Fragment, { children: [_jsxs(Descriptions, { size: "small", column: 3, style: { marginBottom: 16 }, children: [_jsxs(Descriptions.Item, { label: "\u8BD5\u5377", children: [previewQuery.data.paperCode, " \u5377"] }), _jsxs(Descriptions.Item, { label: "\u7248\u672C", children: ["v", previewQuery.data.versionNo] }), _jsx(Descriptions.Item, { label: "\u9898\u91CF", children: previewQuery.data.questionCount }), _jsx(Descriptions.Item, { label: "\u603B\u5206", children: previewQuery.data.totalScore }), _jsxs(Descriptions.Item, { label: "\u65F6\u957F", children: [previewQuery.data.durationMinutes, " \u5206\u949F"] })] }), _jsx(Table, { rowKey: (row) => String(row.sortOrder), size: "small", pagination: { pageSize: 20, showSizeChanger: false }, dataSource: previewQuery.data.questions, scroll: { x: 620 }, columns: [
                                    { title: '题号', dataIndex: 'sortOrder', width: 90 },
                                    {
                                        title: '题干',
                                        dataIndex: 'content',
                                        width: 420,
                                        render: (value) => {
                                            const text = previewQuestionText(value);
                                            return (_jsx(Typography.Text, { ellipsis: { tooltip: true }, style: { display: 'block' }, children: text || '-' }));
                                        },
                                    },
                                    { title: '题型', dataIndex: 'questionType', render: questionTypeText },
                                    { title: '分值', dataIndex: 'score', width: 120 },
                                ] })] })) : _jsx(Alert, { type: "warning", showIcon: true, message: "\u6682\u65F6\u65E0\u6CD5\u52A0\u8F7D\u8BD5\u5377\u9884\u89C8" })] }), _jsx(Modal, { title: editingRoom ? '编辑考场' : '新增考场', open: roomOpen, onCancel: () => setRoomOpen(false), onOk: () => roomForm.submit(), confirmLoading: roomMutation.isPending, destroyOnClose: true, children: _jsxs(Form, { form: roomForm, layout: "vertical", onFinish: (values) => roomMutation.mutate(values), children: [_jsx(Form.Item, { name: "name", label: "\u8003\u573A\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "address", label: "\u5730\u5740", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsxs(Space, { wrap: true, children: [_jsx(Form.Item, { name: "capacity", label: "\u5BB9\u91CF", rules: [{ required: true }], children: _jsx(InputNumber, { min: 1, max: 10000 }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", rules: [{ required: true }], children: _jsx(Select, { style: { width: 140 }, options: [{ label: '启用', value: 'enabled' }, { label: '停用', value: 'disabled' }] }) })] }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 3 }) })] }) }), _jsx(Modal, { title: editingSession ? '编辑考试场次' : '新增考试场次', open: sessionOpen, onCancel: () => setSessionOpen(false), onOk: () => sessionForm.submit(), confirmLoading: sessionMutation.isPending, destroyOnClose: true, children: _jsxs(Form, { form: sessionForm, layout: "vertical", onFinish: (values) => sessionMutation.mutate(values), children: [_jsxs(Space, { wrap: true, children: [_jsx(Form.Item, { name: "sessionCode", label: "\u8BD5\u5377\u7F16\u53F7", rules: [{ required: true }], children: _jsx(Input, { style: { width: 140 }, placeholder: "\u5982 P1" }) }), _jsx(Form.Item, { name: "sessionName", label: "\u573A\u6B21\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, { style: { width: 280 } }) })] }), _jsx(Form.Item, { name: "paperSnapshotId", label: "\u8BD5\u5377\u5FEB\u7167", rules: [{ required: true }], children: _jsx(Select, { options: snapshotOptions }) }), _jsx(Form.Item, { name: "timeRange", label: "\u8003\u8BD5\u65F6\u95F4", rules: [{ required: true }], children: _jsx(DatePicker.RangePicker, { showTime: true, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "roomIds", label: "\u8003\u573A", rules: [{ required: true }], children: _jsx(Select, { mode: "multiple", options: roomOptions }) })] }) }), _jsx(Modal, { title: "\u64A4\u56DE\u6392\u8003\u7248\u672C", open: withdrawOpen, onCancel: () => setWithdrawOpen(false), onOk: () => withdrawForm.submit(), confirmLoading: withdrawMutation.isPending, okButtonProps: { danger: true }, destroyOnClose: true, children: _jsx(Form, { form: withdrawForm, layout: "vertical", onFinish: (values) => withdrawMutation.mutate(values), children: _jsx(Form.Item, { name: "reason", label: "\u64A4\u56DE\u539F\u56E0", rules: [{ required: true, min: 2 }], children: _jsx(Input.TextArea, { rows: 4 }) }) }) }), _jsxs(Modal, { title: monitorAction ? monitorActionLabel[monitorAction.action] : '监考操作', open: Boolean(monitorAction), onCancel: () => setMonitorAction(undefined), onOk: () => actionForm.submit(), confirmLoading: monitorActionMutation.isPending, okButtonProps: { danger: monitorAction?.action !== 'restore-absence' }, destroyOnClose: true, children: [_jsx(Alert, { style: { marginBottom: 16 }, type: "warning", showIcon: true, message: "\u6B64\u64CD\u4F5C\u4F1A\u5199\u5165\u5BA1\u8BA1\u65E5\u5FD7\u3002\u8BF7\u8F93\u5165\u539F\u56E0\uFF0C\u5E76\u952E\u5165\u201C\u786E\u8BA4\u201D\u5B8C\u6210\u4E8C\u6B21\u786E\u8BA4\u3002" }), _jsxs(Form, { form: actionForm, layout: "vertical", onFinish: (values) => monitorAction && monitorActionMutation.mutate({ action: monitorAction.action, attemptId: monitorAction.record.attemptId, reason: values.reason }), children: [_jsx(Form.Item, { name: "reason", label: "\u64CD\u4F5C\u539F\u56E0", rules: [{ required: true, min: 2 }], children: _jsx(Input.TextArea, { rows: 4 }) }), _jsx(Form.Item, { name: "confirmation", label: "\u4E8C\u6B21\u786E\u8BA4", rules: [{ required: true }, { validator: (_, value) => value === '确认' ? Promise.resolve() : Promise.reject(new Error('请输入“确认”')) }], children: _jsx(Input, { placeholder: "\u786E\u8BA4" }) })] })] })] }));
    return embedded ? content : _jsx(CompetitionEventFrame, { event: eventQuery.data, activeSection: "theory-exam", onNavigate: onNavigate, children: content });
}

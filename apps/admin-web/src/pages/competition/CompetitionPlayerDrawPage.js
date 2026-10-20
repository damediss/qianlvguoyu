import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, DownloadOutlined, OrderedListOutlined, ReloadOutlined, } from '@ant-design/icons';
import { Alert, Button, Card, Descriptions, Modal, Result, Select, Space, Spin, Steps, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents, pageCompetitionStages, } from '../../api/competition';
import { createCompetitionPlayerDrawTask, deleteCompetitionPlayerDrawTask, downloadCompetitionPlayerDrawRoster, drawNextCompetitionPlayer, generateCompetitionPlayerDrawOrder, getCompetitionPlayerDrawTask, listCompetitionPlayerDrawCandidates, pageCompetitionPlayerDrawTasks, saveCompetitionPlayerDrawTaskSelection, } from '../../api/competitionPlayerDraw';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
const MAX_DRAW_PLAYER_COUNT = 5000;
function idValue(value) {
    return value == null ? '' : String(value);
}
function errorText(error, fallback) {
    const responseMessage = error?.response?.data?.message;
    if (typeof responseMessage === 'string' && responseMessage.trim())
        return responseMessage;
    return error instanceof Error && error.message.trim() ? error.message : fallback;
}
function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
function taskStatus(status, playerCount) {
    if (status === 'created' && playerCount === 0)
        return _jsx(Tag, { color: "warning", children: "\u5F85\u9009\u62E9\u9009\u624B" });
    if (status === 'created')
        return _jsx(Tag, { color: "processing", children: "\u5F85\u751F\u6210\u62BD\u7B7E\u987A\u5E8F" });
    if (status === 'ordered')
        return _jsx(Tag, { color: "blue", children: "\u5F85\u9010\u4E2A\u62BD\u7B7E" });
    if (status === 'drawing')
        return _jsx(Tag, { color: "gold", children: "\u62BD\u7B7E\u8FDB\u884C\u4E2D" });
    if (status === 'drawn')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u5B8C\u6210" });
    return _jsx(Tag, { children: status || '-' });
}
function slotKey(slot) {
    return `${idValue(slot.contestantProfileId)}-${slot.drawOrder}`;
}
export function CompetitionPlayerDrawPage({ stageId, stageName, showTitle = true, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:player-draw:view');
    const canCreate = permissions.includes('competition:player-draw:create');
    const canDraw = permissions.includes('competition:player-draw:draw');
    const canExport = permissions.includes('competition:player-draw:export');
    const canDelete = permissions.includes('competition:player-draw:delete');
    const [selectedIds, setSelectedIds] = useState([]);
    const [taskPage, setTaskPage] = useState(1);
    const [wizardOpen, setWizardOpen] = useState(false);
    const [wizardStep, setWizardStep] = useState(0);
    const [createdTaskId, setCreatedTaskId] = useState();
    const [savedPlayerCount, setSavedPlayerCount] = useState(0);
    const [activeTaskId, setActiveTaskId] = useState();
    const [deskOpen, setDeskOpen] = useState(false);
    const [lastDrawn, setLastDrawn] = useState();
    const drawnIdsRef = useRef(new Set());
    const queryClient = useQueryClient();
    const candidatesQuery = useQuery({
        queryKey: ['competition-player-draw-candidates', stageId],
        queryFn: () => listCompetitionPlayerDrawCandidates(stageId),
        enabled: canView && Boolean(stageId) && wizardOpen,
    });
    const candidates = candidatesQuery.data || [];
    const tasksQuery = useQuery({
        queryKey: ['competition-player-draw-tasks', stageId, taskPage],
        queryFn: () => pageCompetitionPlayerDrawTasks(stageId, { page: taskPage, pageSize: 10 }),
        enabled: canView && Boolean(stageId),
    });
    const taskDetailQuery = useQuery({
        queryKey: ['competition-player-draw-task', activeTaskId],
        queryFn: () => getCompetitionPlayerDrawTask(activeTaskId),
        enabled: Boolean(activeTaskId && deskOpen),
    });
    const countMatches = selectedIds.length >= 1 && selectedIds.length <= MAX_DRAW_PLAYER_COUNT;
    const detail = taskDetailQuery.data;
    const detailSlots = useMemo(() => {
        const slots = [...(detail?.slots || [])];
        slots.sort((left, right) => (detail?.status === 'drawn'
            ? (left.appearanceOrder || 0) - (right.appearanceOrder || 0)
            : left.drawOrder - right.drawOrder));
        return slots;
    }, [detail?.slots, detail?.status]);
    const nextSlot = detailSlots.find((slot) => slot.appearanceOrder == null);
    useEffect(() => {
        drawnIdsRef.current = new Set((detail?.slots || [])
            .filter((slot) => slot.appearanceOrder != null)
            .map((slot) => idValue(slot.contestantProfileId)));
    }, [detail?.slots]);
    const rememberTask = (task) => {
        const taskId = idValue(task.id);
        queryClient.setQueryData(['competition-player-draw-task', taskId], task);
        void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-tasks', stageId] });
        return taskId;
    };
    const createMutation = useMutation({
        mutationFn: () => {
            if (!stageId)
                throw new Error('请先选择赛事工种、等级和赛段');
            return createCompetitionPlayerDrawTask(stageId);
        },
        onSuccess: (task) => {
            setCreatedTaskId(rememberTask(task));
            setSavedPlayerCount(task.playerCount || 0);
            setSelectedIds([]);
            setWizardStep(1);
            message.success('抽签任务已创建，请选择选手');
        },
        onError: (error) => message.error(errorText(error, '创建抽签任务失败')),
    });
    const selectionMutation = useMutation({
        mutationFn: (contestantProfileIds) => {
            if (!createdTaskId)
                throw new Error('抽签任务尚未创建');
            return saveCompetitionPlayerDrawTaskSelection(createdTaskId, { contestantProfileIds });
        },
        onSuccess: (task) => {
            rememberTask(task);
            setSavedPlayerCount(task.playerCount || 0);
            setWizardStep(2);
            message.success('选手已保存，请确认后生成抽签顺序');
        },
        onError: (error) => message.error(errorText(error, '保存选手失败')),
    });
    const orderMutation = useMutation({
        mutationFn: generateCompetitionPlayerDrawOrder,
        onSuccess: (task) => {
            const taskId = rememberTask(task);
            setActiveTaskId(taskId);
            setLastDrawn(undefined);
            setWizardOpen(false);
            setDeskOpen(true);
            message.success('抽签顺序已生成，请按顺序逐个抽出场顺序');
        },
        onError: (error) => message.error(errorText(error, '生成抽签顺序失败')),
    });
    const drawMutation = useMutation({
        mutationFn: drawNextCompetitionPlayer,
        onSuccess: (task) => {
            const previous = drawnIdsRef.current;
            const drawn = (task.slots || []).find((slot) => (slot.appearanceOrder != null && !previous.has(idValue(slot.contestantProfileId))));
            setLastDrawn(drawn);
            rememberTask(task);
            if (drawn) {
                message.success(`${drawn.name || '选手'} 抽到出场顺序 ${drawn.appearanceOrder}`);
            }
        },
        onError: (error) => message.error(errorText(error, '抽签失败')),
    });
    const downloadMutation = useMutation({
        mutationFn: downloadCompetitionPlayerDrawRoster,
        onSuccess: (blob, taskId) => downloadBlob(blob, `抽签出场名单-${taskId}.xlsx`),
        onError: (error) => message.error(errorText(error, '导出名单失败')),
    });
    const deleteMutation = useMutation({
        mutationFn: deleteCompetitionPlayerDrawTask,
        onSuccess: (_data, taskId) => {
            message.success('抽签任务已删除');
            if (idValue(taskId) === activeTaskId)
                setDeskOpen(false);
            void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-tasks', stageId] });
        },
        onError: (error) => message.error(errorText(error, '删除抽签任务失败')),
    });
    const candidateColumns = useMemo(() => [
        { title: '选手姓名', dataIndex: 'name', width: 140, render: (value) => value || '-' },
        { title: '所属企业', dataIndex: 'orgName', width: 220, render: (value) => value || '-' },
        { title: '赛事工种', dataIndex: 'workTypeName', width: 180, render: (value) => value || '-' },
        {
            title: '技能等级',
            dataIndex: 'levelName',
            width: 150,
            render: (value, record) => value || record.levelCode || record.rosterLevel || '-',
        },
    ], []);
    const slotColumns = useMemo(() => [
        { title: '抽签顺序', dataIndex: 'drawOrder', width: 110, render: (value) => `第 ${value} 位` },
        {
            title: '出场顺序',
            dataIndex: 'appearanceOrder',
            width: 110,
            render: (value) => (value == null ? '-' : `第 ${value} 位`),
        },
        { title: '选手姓名', dataIndex: 'name', width: 140, render: (value) => value || '-' },
        { title: '所属企业', dataIndex: 'orgName', width: 220, render: (value) => value || '-' },
        { title: '手机号', dataIndex: 'mobile', width: 160, render: (value) => value || '-' },
        {
            title: '状态',
            key: 'drawState',
            width: 110,
            render: (_, record) => (record.appearanceOrder == null
                ? _jsx(Tag, { color: nextSlot && slotKey(nextSlot) === slotKey(record) ? 'processing' : 'default', children: nextSlot && slotKey(nextSlot) === slotKey(record) ? '当前抽签' : '等待' })
                : _jsx(Tag, { color: "success", children: "\u5DF2\u62BD\u51FA" })),
        },
    ], [nextSlot]);
    const openDesk = (taskId) => {
        setActiveTaskId(idValue(taskId));
        setLastDrawn(undefined);
        setDeskOpen(true);
    };
    const confirmDelete = (taskId, status) => {
        const content = status === 'drawn'
            ? '该任务已完成抽签。删除后出场名单将从任务列表中移除。确认删除吗？'
            : status === 'ordered'
                ? '抽签顺序已生成，但尚未逐个抽签。删除后需要重新创建任务。确认删除吗？'
                : '删除后，该未执行的抽签任务将从任务列表中移除。确认删除吗？';
        Modal.confirm({
            title: '删除抽签任务',
            content,
            okText: '删除',
            okButtonProps: { danger: true },
            cancelText: '取消',
            onOk: () => deleteMutation.mutateAsync(idValue(taskId)),
        });
    };
    const taskColumns = [
        { title: '选手数', dataIndex: 'playerCount', width: 90 },
        {
            title: '抽签进度',
            key: 'progress',
            width: 120,
            render: (_, record) => (record.status === 'created' ? '-' : `${record.drawnCount || 0}/${record.playerCount || 0}`),
        },
        {
            title: '任务状态',
            dataIndex: 'status',
            width: 150,
            render: (value, record) => taskStatus(value, record.playerCount),
        },
        { title: '创建时间', dataIndex: 'createdAt', width: 180, render: (value) => value ? value.replace('T', ' ').slice(0, 16) : '-' },
        {
            title: '操作',
            key: 'actions',
            width: 400,
            render: (_, record) => (_jsxs(Space, { children: [record.status === 'created' && record.playerCount === 0 && canCreate && (_jsx(Button, { type: "link", onClick: () => resumeTask(record), children: "\u9009\u62E9\u9009\u624B" })), record.status === 'created' && record.playerCount > 0 && canDraw && (_jsx(Button, { type: "link", onClick: () => resumeTask(record), children: "\u751F\u6210\u62BD\u7B7E\u987A\u5E8F" })), (record.status === 'ordered' || record.status === 'drawing') && canDraw && (_jsx(Button, { type: "link", onClick: () => openDesk(record.id), children: "\u7EE7\u7EED\u62BD\u7B7E" })), record.status === 'drawn' && canView && (_jsx(Button, { type: "link", onClick: () => openDesk(record.id), children: "\u67E5\u770B\u540D\u5355" })), record.status === 'drawn' && canExport && (_jsx(Button, { type: "link", icon: _jsx(DownloadOutlined, {}), loading: downloadMutation.isPending && downloadMutation.variables === record.id, onClick: () => downloadMutation.mutate(record.id), children: "\u5BFC\u51FA Excel" })), (record.status === 'created' || record.status === 'ordered' || record.status === 'drawn') && canDelete && (_jsx(Button, { type: "link", danger: true, icon: _jsx(DeleteOutlined, {}), loading: deleteMutation.isPending && deleteMutation.variables === idValue(record.id), onClick: () => confirmDelete(record.id, record.status), children: "\u5220\u9664" }))] })),
        },
    ];
    const selectedCandidates = useMemo(() => {
        const candidateById = new Map(candidates.map((candidate) => [idValue(candidate.contestantProfileId), candidate]));
        return selectedIds
            .map((id) => candidateById.get(id))
            .filter((candidate) => Boolean(candidate));
    }, [candidates, selectedIds]);
    const openWizard = () => {
        setCreatedTaskId(undefined);
        setSavedPlayerCount(0);
        setSelectedIds([]);
        setWizardStep(0);
        setWizardOpen(true);
    };
    const resumeTask = (task) => {
        setCreatedTaskId(idValue(task.id));
        setSavedPlayerCount(task.playerCount);
        setSelectedIds([]);
        setWizardStep(task.playerCount === 0 ? 1 : 2);
        setWizardOpen(true);
    };
    const closeWizard = () => {
        setWizardOpen(false);
        setWizardStep(0);
    };
    if (!canView)
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u62BD\u7B7E\u6392\u5E8F\u67E5\u770B\u6743\u9650" });
    return (_jsxs("div", { className: "competition-page", style: { padding: 16 }, children: [_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsxs("div", { style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }, children: [_jsxs("div", { children: [showTitle && _jsxs(Typography.Title, { level: 4, style: { margin: 0 }, children: [_jsx(OrderedListOutlined, {}), " \u62BD\u7B7E\u6392\u5E8F"] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u8D5B\u6BB5\uFF1A", stageName || stageId, " \u00B7 \u5148\u5B9A\u62BD\u7B7E\u987A\u5E8F\uFF0C\u518D\u9010\u4E2A\u62BD\u51FA\u573A\u987A\u5E8F"] })] }), _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                            void tasksQuery.refetch();
                                            void queryClient.invalidateQueries({ queryKey: ['competition-player-draw-candidates', stageId] });
                                        }, children: "\u5237\u65B0" }), canCreate && _jsx(Button, { type: "primary", onClick: openWizard, children: "\u521B\u5EFA\u62BD\u7B7E\u4EFB\u52A1" })] })] }), tasksQuery.isError && (_jsx(Alert, { type: "error", showIcon: true, message: errorText(tasksQuery.error, '抽签任务加载失败') })), _jsx(Table, { rowKey: (record) => idValue(record.id), loading: tasksQuery.isFetching, dataSource: tasksQuery.data?.items || [], columns: taskColumns, pagination: {
                            current: taskPage,
                            pageSize: 10,
                            total: tasksQuery.data?.total || 0,
                            showSizeChanger: false,
                            onChange: (page) => setTaskPage(page),
                        }, locale: { emptyText: '当前赛段还没有抽签任务' }, scroll: { x: 980 } })] }), _jsxs(Modal, { title: `抽签任务 · ${['创建抽签任务', '选择选手', '生成抽签顺序'][wizardStep]}`, open: wizardOpen, onCancel: closeWizard, width: 920, centered: true, destroyOnClose: true, styles: { body: { maxHeight: 'calc(100vh - 220px)', overflowY: 'auto' } }, footer: (_jsxs(Space, { children: [wizardStep < 2 && _jsx(Button, { onClick: closeWizard, children: "\u53D6\u6D88" }), wizardStep === 0 && (_jsx(Button, { type: "primary", loading: createMutation.isPending, onClick: () => createMutation.mutate(), children: "\u521B\u5EFA\u4EFB\u52A1\u5E76\u9009\u62E9\u9009\u624B" })), wizardStep === 1 && (_jsx(Button, { type: "primary", loading: selectionMutation.isPending, disabled: !countMatches || !canCreate, onClick: () => selectionMutation.mutate(selectedIds), children: "\u4FDD\u5B58\u9009\u624B\u5E76\u786E\u8BA4" })), wizardStep === 2 && (_jsxs(_Fragment, { children: [_jsx(Button, { onClick: closeWizard, children: "\u7A0D\u540E\u5904\u7406" }), canDraw && (_jsx(Button, { type: "primary", loading: orderMutation.isPending, disabled: !createdTaskId, onClick: () => createdTaskId && orderMutation.mutate(createdTaskId), children: "\u751F\u6210\u62BD\u7B7E\u987A\u5E8F" }))] }))] })), children: [_jsx(Steps, { current: wizardStep, items: [
                            { title: '创建抽签任务' },
                            { title: '选择选手' },
                            { title: '生成抽签顺序' },
                        ], style: { marginBottom: 24 } }), wizardStep === 0 && (_jsx(Alert, { type: "info", showIcon: true, message: `当前赛段：${stageName || stageId}`, description: "\u62BD\u7B7E\u5206\u4E24\u6B21\u6267\u884C\u3002\u7B2C\u4E00\u8F6E\u4E3A\u5DF2\u9009\u9009\u624B\u751F\u6210\u968F\u673A\u62BD\u7B7E\u987A\u5E8F\uFF0C\u51B3\u5B9A\u8C01\u5148\u62BD\uFF1B\u7B2C\u4E8C\u8F6E\u6309\u8FD9\u4E2A\u987A\u5E8F\u9010\u4E2A\u9009\u624B\u62BD\u7B7E\uFF0C\u6BCF\u6B21\u62BD\u51FA\u4E00\u4E2A\u5C1A\u672A\u5360\u7528\u7684\u51FA\u573A\u987A\u5E8F\u3002" })), wizardStep === 1 && (_jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [candidatesQuery.isError && (_jsx(Alert, { type: "error", showIcon: true, message: errorText(candidatesQuery.error, '当前赛段选手加载失败') })), _jsx(Alert, { type: countMatches ? 'success' : 'warning', showIcon: true, message: `当前赛段选手 ${candidates.length} 人；已选 ${selectedIds.length} 人；允许选择 1–${MAX_DRAW_PLAYER_COUNT} 人。`, description: countMatches
                                    ? '人数校验通过。确认后将进入第一轮，生成这些选手的随机抽签顺序。'
                                    : candidates.length === 0
                                        ? '当前赛段暂无可抽签选手。'
                                        : `请选择 1 至 ${MAX_DRAW_PLAYER_COUNT} 名选手。` }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { disabled: !candidates.length || candidatesQuery.isFetching, onClick: () => setSelectedIds(candidates.map((item) => idValue(item.contestantProfileId))), children: "\u5168\u9009\u5F53\u524D\u8D5B\u6BB5\u9009\u624B" }), _jsx(Button, { disabled: !selectedIds.length, onClick: () => setSelectedIds([]), children: "\u6E05\u7A7A\u9009\u62E9" })] }), _jsx(Table, { rowKey: (record) => idValue(record.contestantProfileId), loading: candidatesQuery.isFetching, dataSource: candidates, columns: candidateColumns, rowSelection: {
                                    selectedRowKeys: selectedIds,
                                    preserveSelectedRowKeys: true,
                                    onChange: (keys) => setSelectedIds(keys.map(String)),
                                }, pagination: { pageSize: 8, showSizeChanger: true, pageSizeOptions: [8, 20, 50] }, scroll: { x: 900, y: 360 }, locale: { emptyText: '当前赛段暂无可抽签选手' } })] })), wizardStep === 2 && (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Alert, { type: "success", showIcon: true, message: "\u9009\u624B\u5DF2\u4FDD\u5B58\uFF0C\u7B49\u5F85\u7B2C\u4E00\u8F6E\u62BD\u7B7E", description: `任务 ${createdTaskId || ''} 已选择 ${savedPlayerCount} 名选手。确认后只生成抽签顺序，不会同时分配出场顺序。` }), _jsxs(Descriptions, { bordered: true, size: "small", column: 2, children: [_jsx(Descriptions.Item, { label: "\u6240\u5C5E\u8D5B\u6BB5", children: stageName || stageId }), _jsxs(Descriptions.Item, { label: "\u9009\u624B\u4EBA\u6570", children: [savedPlayerCount, " \u4EBA"] }), _jsx(Descriptions.Item, { label: "\u7B2C\u4E00\u8F6E", span: 2, children: "\u968F\u673A\u6253\u4E71\u9009\u624B\uFF0C\u5F97\u5230\u62BD\u7B7E\u987A\u5E8F" }), _jsx(Descriptions.Item, { label: "\u7B2C\u4E8C\u8F6E", span: 2, children: "\u6309\u62BD\u7B7E\u987A\u5E8F\u9010\u4E2A\u62BD\u53D6\u5C1A\u672A\u5360\u7528\u7684\u51FA\u573A\u987A\u5E8F" })] }), selectedCandidates.length > 0 ? (_jsxs(_Fragment, { children: [_jsx(Typography.Text, { strong: true, children: "\u5DF2\u9009\u9009\u624B" }), _jsx(Table, { rowKey: (record) => idValue(record.contestantProfileId), size: "small", dataSource: selectedCandidates, columns: candidateColumns, pagination: { pageSize: 8, showSizeChanger: true, pageSizeOptions: [8, 20, 50] }, scroll: { x: 900, y: 300 } })] })) : (_jsxs(Typography.Text, { type: "secondary", children: ["\u9009\u624B\u540D\u5355\u5DF2\u4FDD\u5B58\u5728\u4EFB\u52A1\u4E2D\uFF08", savedPlayerCount, " \u4EBA\uFF09\uFF0C\u751F\u6210\u62BD\u7B7E\u987A\u5E8F\u65F6\u5C06\u518D\u6B21\u6821\u9A8C\u8D5B\u6BB5\u6210\u5458\u3002"] }))] }))] }), _jsx(Modal, { title: detail?.status === 'drawn'
                    ? `${detail.stageName || stageName || '抽签'} · 出场名单`
                    : `${detail?.stageName || stageName || '抽签'} · 逐个抽签`, open: deskOpen, onCancel: () => setDeskOpen(false), width: 980, centered: true, styles: { body: { maxHeight: 'calc(100vh - 220px)', overflowY: 'auto' } }, footer: (_jsxs(Space, { children: [_jsx(Button, { onClick: () => setDeskOpen(false), children: "\u5173\u95ED" }), detail?.status === 'drawn' && activeTaskId && canDelete && (_jsx(Button, { danger: true, icon: _jsx(DeleteOutlined, {}), loading: deleteMutation.isPending, onClick: () => confirmDelete(activeTaskId, 'drawn'), children: "\u5220\u9664" })), detail?.status === 'drawn' && activeTaskId && canExport && (_jsx(Button, { type: "primary", icon: _jsx(DownloadOutlined, {}), loading: downloadMutation.isPending, onClick: () => downloadMutation.mutate(activeTaskId), children: "\u5BFC\u51FA Excel" })), detail && detail.status !== 'drawn' && nextSlot && canDraw && (_jsxs(Button, { type: "primary", loading: drawMutation.isPending, onClick: () => activeTaskId && drawMutation.mutate(activeTaskId), children: ["\u4E3A", nextSlot.name || '当前选手', "\u62BD\u53D6\u51FA\u573A\u987A\u5E8F"] }))] })), children: taskDetailQuery.isLoading ? _jsx(Spin, {}) : taskDetailQuery.isError
                    ? _jsx(Result, { status: "error", title: "\u62BD\u7B7E\u7ED3\u679C\u52A0\u8F7D\u5931\u8D25", extra: _jsx(Button, { onClick: () => taskDetailQuery.refetch(), children: "\u91CD\u65B0\u52A0\u8F7D" }) })
                    : (_jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsx(Steps, { size: "small", current: detail?.status === 'drawn' ? 2 : 1, items: [
                                    { title: '抽签顺序已生成' },
                                    { title: '逐个抽出场顺序' },
                                    { title: '完成' },
                                ] }), lastDrawn && (_jsx(Alert, { type: "success", showIcon: true, message: `${lastDrawn.name || '选手'} 抽到出场顺序第 ${lastDrawn.appearanceOrder} 位`, description: `该选手的抽签顺序是第 ${lastDrawn.drawOrder} 位。` })), detail && (_jsx(Alert, { type: detail.status === 'drawn' ? 'success' : 'info', showIcon: true, message: detail.status === 'drawn'
                                    ? `全部 ${detail.playerCount} 名选手已完成抽签。名单按出场顺序排列。`
                                    : `已抽 ${detail.drawnCount || 0}/${detail.playerCount} 人。下一位抽签：第 ${nextSlot?.drawOrder || '-'} 位 ${nextSlot?.name || ''}` })), _jsx(Table, { rowKey: slotKey, dataSource: detailSlots, columns: slotColumns, pagination: { pageSize: 8, showSizeChanger: true }, scroll: { x: 900 }, rowClassName: (record) => (nextSlot && slotKey(nextSlot) === slotKey(record) ? 'ant-table-row-selected' : '') })] })) })] }));
}
export function CompetitionPlayerDrawManagementPage() {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:player-draw:view');
    const eventsQuery = useQuery({
        queryKey: ['competition-events', 'player-draw'],
        queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
        enabled: canView,
    });
    const events = eventsQuery.data?.items || [];
    const [selectedEventId, setSelectedEventId] = useState();
    const [selectedWorkTypeId, setSelectedWorkTypeId] = useState();
    const [selectedLevelCode, setSelectedLevelCode] = useState();
    const [selectedStageId, setSelectedStageId] = useState();
    useEffect(() => {
        if (!events.length) {
            setSelectedEventId(undefined);
            return;
        }
        if (!selectedEventId || !events.some((event) => idValue(event.id) === selectedEventId)) {
            setSelectedEventId(idValue(events[0].id));
        }
    }, [events, selectedEventId]);
    const workTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', selectedEventId, 'player-draw'],
        queryFn: () => listCompetitionEventWorkTypes(selectedEventId),
        enabled: canView && Boolean(selectedEventId),
    });
    const workTypeOptions = useMemo(() => ((workTypesQuery.data || [])
        .filter((workType) => workType.status === 'active')
        .map((workType) => ({
        value: idValue(workType.id),
        label: workType.displayName || workType.workTypeName || idValue(workType.workTypeId),
    }))), [workTypesQuery.data]);
    useEffect(() => {
        if (!workTypesQuery.data)
            return;
        if (!workTypeOptions.length) {
            setSelectedWorkTypeId(undefined);
            setSelectedLevelCode(undefined);
            setSelectedStageId(undefined);
            return;
        }
        if (!selectedWorkTypeId || !workTypeOptions.some((item) => item.value === selectedWorkTypeId)) {
            setSelectedWorkTypeId(workTypeOptions[0].value);
            setSelectedLevelCode(undefined);
            setSelectedStageId(undefined);
        }
    }, [selectedWorkTypeId, workTypeOptions, workTypesQuery.data]);
    const levelsQuery = useQuery({
        queryKey: ['competition-event-work-type-levels', selectedEventId, selectedWorkTypeId, 'player-draw'],
        queryFn: () => listCompetitionEventWorkTypeLevels(selectedEventId, selectedWorkTypeId),
        enabled: canView && Boolean(selectedEventId && selectedWorkTypeId),
    });
    const levelOptions = useMemo(() => ((levelsQuery.data || []).map((level) => ({
        value: level.levelCode,
        label: level.levelName ? `${level.levelName}（${level.levelCode}）` : level.levelCode,
    }))), [levelsQuery.data]);
    useEffect(() => {
        if (!levelsQuery.data || !selectedWorkTypeId)
            return;
        if (!levelOptions.length) {
            setSelectedLevelCode(undefined);
            setSelectedStageId(undefined);
            return;
        }
        if (!selectedLevelCode || !levelOptions.some((item) => item.value === selectedLevelCode)) {
            setSelectedLevelCode(levelOptions[0].value);
            setSelectedStageId(undefined);
        }
    }, [levelOptions, levelsQuery.data, selectedLevelCode, selectedWorkTypeId]);
    const stagesQuery = useQuery({
        queryKey: ['competition-stages', 'player-draw', selectedEventId, selectedWorkTypeId, selectedLevelCode],
        queryFn: () => pageCompetitionStages(selectedEventId, {
            page: 1,
            pageSize: 100,
            eventWorkTypeId: selectedWorkTypeId,
            eventWorkTypeLevelCode: selectedLevelCode,
        }),
        enabled: canView && Boolean(selectedEventId && selectedWorkTypeId && selectedLevelCode),
    });
    const stages = stagesQuery.data?.items || [];
    useEffect(() => {
        if (!stagesQuery.data)
            return;
        const stageIds = stages.map((stage) => idValue(stage.id));
        if (!selectedStageId || !stageIds.includes(selectedStageId)) {
            setSelectedStageId(stageIds[0]);
        }
    }, [selectedStageId, stages, stagesQuery.data]);
    const selectedStage = stages.find((stage) => idValue(stage.id) === selectedStageId);
    if (!canView)
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u62BD\u7B7E\u6392\u5E8F\u67E5\u770B\u6743\u9650" });
    if (eventsQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (eventsQuery.isError) {
        return _jsx(Result, { status: "error", title: "\u8D5B\u4E8B\u4EFB\u52A1\u52A0\u8F7D\u5931\u8D25", extra: _jsx(Button, { onClick: () => eventsQuery.refetch(), children: "\u91CD\u65B0\u52A0\u8F7D" }) });
    }
    return (_jsxs("div", { className: "competition-page", children: [_jsxs("section", { className: "competition-page-heading", children: [_jsxs("div", { children: [_jsxs(Typography.Title, { level: 2, children: [_jsx(OrderedListOutlined, {}), " \u62BD\u7B7E\u6392\u5E8F"] }), _jsxs(Typography.Paragraph, { type: "secondary", children: ["\u6309\u8D5B\u4E8B\u5DE5\u79CD\u3001\u6280\u80FD\u7B49\u7EA7\u548C\u8D5B\u6BB5\u9009\u62E9\u9009\u624B\u3002\u7B2C\u4E00\u8F6E\u751F\u6210\u968F\u673A\u62BD\u7B7E\u987A\u5E8F\uFF0C\u7B2C\u4E8C\u8F6E\u6309\u8BE5\u987A\u5E8F\u9010\u4E2A\u62BD\u51FA\u573A\u987A\u5E8F\u3002", selectedStage ? ` 当前赛段：${selectedStage.name}` : ''] })] }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                            void eventsQuery.refetch();
                            if (selectedEventId)
                                void workTypesQuery.refetch();
                            if (selectedEventId && selectedWorkTypeId)
                                void levelsQuery.refetch();
                            if (selectedEventId && selectedWorkTypeId && selectedLevelCode) {
                                void stagesQuery.refetch();
                            }
                        }, children: "\u5237\u65B0" })] }), _jsx(Card, { bordered: false, className: "competition-filter-card competition-filter-card--compact", children: _jsxs(Space, { align: "center", size: 12, wrap: true, style: { width: '100%' }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u8D5B\u4E8B\u4EFB\u52A1" }), _jsx(Select, { showSearch: true, optionFilterProp: "label", value: selectedEventId, loading: eventsQuery.isFetching, disabled: !events.length, options: events.map((event) => ({
                                value: idValue(event.id),
                                label: `${event.code} · ${event.name}`,
                            })), onChange: (value) => {
                                setSelectedEventId(value);
                                setSelectedWorkTypeId(undefined);
                                setSelectedLevelCode(undefined);
                                setSelectedStageId(undefined);
                            }, placeholder: "\u9009\u62E9\u8D5B\u4E8B\u4EFB\u52A1", style: { width: 280, maxWidth: '100%' } }), _jsx(Typography.Text, { type: "secondary", children: "\u5DE5\u79CD" }), _jsx(Select, { showSearch: true, optionFilterProp: "label", value: selectedWorkTypeId, loading: workTypesQuery.isFetching, disabled: !selectedEventId || (!workTypesQuery.isFetching && workTypeOptions.length === 0), options: workTypeOptions, onChange: (value) => {
                                setSelectedWorkTypeId(value);
                                setSelectedLevelCode(undefined);
                                setSelectedStageId(undefined);
                            }, placeholder: "\u9009\u62E9\u5DE5\u79CD", style: { width: 220, maxWidth: '100%' } }), _jsx(Typography.Text, { type: "secondary", children: "\u7B49\u7EA7" }), _jsx(Select, { showSearch: true, optionFilterProp: "label", value: selectedLevelCode, loading: levelsQuery.isFetching, disabled: !selectedWorkTypeId || levelOptions.length === 0, options: levelOptions, onChange: (value) => {
                                setSelectedLevelCode(value);
                                setSelectedStageId(undefined);
                            }, placeholder: "\u9009\u62E9\u7B49\u7EA7", style: { width: 180, maxWidth: '100%' } }), _jsx(Typography.Text, { type: "secondary", children: "\u8D5B\u6BB5" }), _jsx(Select, { showSearch: true, optionFilterProp: "label", value: selectedStageId, loading: stagesQuery.isFetching, disabled: !selectedLevelCode || stages.length === 0, options: stages.map((stage) => ({
                                value: idValue(stage.id),
                                label: `${stage.sortOrder}. ${stage.name}`,
                            })), onChange: setSelectedStageId, placeholder: "\u9009\u62E9\u8D5B\u6BB5", style: { width: 220, maxWidth: '100%' } })] }) }), workTypesQuery.isError && _jsx(Alert, { type: "error", showIcon: true, message: errorText(workTypesQuery.error, '赛事工种加载失败') }), levelsQuery.isError && _jsx(Alert, { type: "error", showIcon: true, message: errorText(levelsQuery.error, '工种等级加载失败') }), stagesQuery.isError && _jsx(Alert, { type: "error", showIcon: true, message: errorText(stagesQuery.error, '赛段加载失败') }), !events.length && _jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u6CA1\u6709\u53EF\u7528\u8D5B\u4E8B\u4EFB\u52A1" }), !workTypesQuery.isFetching && selectedEventId && workTypeOptions.length === 0 && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u8D5B\u4E8B\u6682\u65E0\u53EF\u7528\u5DE5\u79CD" })), !levelsQuery.isFetching && selectedWorkTypeId && levelOptions.length === 0 && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u5DE5\u79CD\u6682\u65E0\u5DF2\u914D\u7F6E\u7B49\u7EA7" })), !stagesQuery.isFetching && selectedLevelCode && stagesQuery.isSuccess && stages.length === 0 && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u5DE5\u79CD\u548C\u7B49\u7EA7\u4E0B\u6682\u65E0\u8D5B\u6BB5\uFF0C\u8BF7\u5148\u5B8C\u6210\u8D5B\u6BB5\u7F16\u6392" })), selectedStage && (_jsx(CompetitionPlayerDrawPage, { stageId: idValue(selectedStage.id), stageName: selectedStage.name, showTitle: false }, idValue(selectedStage.id)))] }));
}

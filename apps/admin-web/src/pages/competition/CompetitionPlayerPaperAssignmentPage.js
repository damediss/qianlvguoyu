import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ArrowLeftOutlined, ArrowRightOutlined, CheckCircleOutlined, ReloadOutlined, SaveOutlined, SearchOutlined, ThunderboltOutlined, UserSwitchOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Badge, Button, Card, Checkbox, Empty, Input, List, Modal, Progress, Result, Select, Space, Spin, Statistic, Table, Tabs, Tag, Typography, message, } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { autoAllocateCompetitionStagePaperAssignments, confirmCompetitionStagePaperAssignments, getCompetitionStagePaperAssignmentSummary, listCompetitionStagePaperAssignmentPlayers, saveCompetitionStagePaperAssignments, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionPlayerPaperAssignmentPage.css';
const UNASSIGNED = '__unassigned__';
function idValue(value) {
    return value == null ? '' : String(value);
}
function errorText(error, fallback) {
    return error instanceof Error && error.message.trim() ? error.message : fallback;
}
function playerKey(player) {
    return idValue(player.contestantProfileId);
}
function paperLabel(paper) {
    if (!paper)
        return '未分配';
    return paper.title ? `${paper.paperCode}卷 · ${paper.title}` : `${paper.paperCode}卷`;
}
function percentValue(value) {
    const parsed = Number(value ?? 0);
    return Number.isFinite(parsed) ? parsed : 0;
}
export function CompetitionPlayerPaperAssignmentPage({ stageId, onDirtyChange, contentType = 'theory', }) {
    const assignmentContentType = contentType;
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:stage-paper-assignment:view');
    const canManage = permissions.includes('competition:stage-paper-assignment:manage');
    const canConfirm = permissions.includes('competition:stage-paper-assignment:confirm');
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState('table');
    const [keyword, setKeyword] = useState('');
    const [orgId, setOrgId] = useState();
    const [paperSnapshotId, setPaperSnapshotId] = useState();
    const [assignmentStatus, setAssignmentStatus] = useState();
    const [page, setPage] = useState(1);
    const [selectedProfileIds, setSelectedProfileIds] = useState([]);
    const [batchPaperId, setBatchPaperId] = useState();
    const [visualPaperId, setVisualPaperId] = useState();
    const [visualSearch, setVisualSearch] = useState('');
    const [visualSelectedIds, setVisualSelectedIds] = useState([]);
    const [draftAssignments, setDraftAssignments] = useState({});
    const [baselineAssignments, setBaselineAssignments] = useState({});
    const summaryQuery = useQuery({
        queryKey: ['competition-stage-paper-assignment-summary', stageId, assignmentContentType],
        queryFn: () => assignmentContentType === 'practical'
            ? getCompetitionStagePaperAssignmentSummary(stageId, assignmentContentType)
            : getCompetitionStagePaperAssignmentSummary(stageId),
        enabled: canView,
    });
    const visualizationQuery = useQuery({
        queryKey: ['competition-stage-paper-assignment-visualization', stageId, assignmentContentType],
        queryFn: () => assignmentContentType === 'practical'
            ? listCompetitionStagePaperAssignmentPlayers(stageId, assignmentContentType)
            : listCompetitionStagePaperAssignmentPlayers(stageId),
        enabled: canView,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
    });
    const locked = Boolean(summaryQuery.data?.formalExamLocked);
    const canManageEffective = canManage && !locked;
    useEffect(() => {
        if (!visualizationQuery.data)
            return;
        const next = {};
        visualizationQuery.data.forEach((player) => {
            next[playerKey(player)] = idValue(player.paperSnapshotId) || null;
        });
        setBaselineAssignments(next);
        setDraftAssignments(next);
        setSelectedProfileIds([]);
        setVisualSelectedIds([]);
    }, [assignmentContentType, stageId, visualizationQuery.data]);
    const papers = summaryQuery.data?.papers || [];
    const paperById = useMemo(() => new Map(papers.map((paper) => [idValue(paper.id), paper])), [papers]);
    const paperOptions = useMemo(() => papers.map((paper) => ({
        label: `${paper.paperCode}卷${paper.title ? ` · ${paper.title}` : ''}`,
        value: idValue(paper.id),
    })), [papers]);
    useEffect(() => {
        if (!papers.length) {
            setVisualPaperId(undefined);
            return;
        }
        if (!visualPaperId || !papers.some((paper) => idValue(paper.id) === visualPaperId)) {
            setVisualPaperId(idValue(papers[0].id));
        }
    }, [papers, visualPaperId]);
    const effectivePlayers = useMemo(() => {
        return (visualizationQuery.data || []).map((player) => {
            const key = playerKey(player);
            const hasDraft = Object.prototype.hasOwnProperty.call(draftAssignments, key);
            const nextPaperId = hasDraft ? draftAssignments[key] : idValue(player.paperSnapshotId) || null;
            const paper = nextPaperId ? paperById.get(nextPaperId) : undefined;
            return {
                ...player,
                paperSnapshotId: nextPaperId,
                paperCode: paper?.paperCode || null,
                paperTitle: paper?.title || null,
                assignmentStatus: nextPaperId ? 'assigned' : 'unassigned',
            };
        });
    }, [draftAssignments, paperById, visualizationQuery.data]);
    const effectivePlayerById = useMemo(() => new Map(effectivePlayers.map((player) => [playerKey(player), player])), [effectivePlayers]);
    const effectiveSummary = useMemo(() => {
        const source = summaryQuery.data;
        if (!source)
            return undefined;
        const counts = new Map();
        let assignedCount = 0;
        effectivePlayers.forEach((player) => {
            const paperId = idValue(player.paperSnapshotId);
            if (paperId) {
                assignedCount += 1;
                counts.set(paperId, (counts.get(paperId) || 0) + 1);
            }
        });
        const eligibleCount = effectivePlayers.length || source.eligibleCount;
        return {
            ...source,
            assignedCount,
            unassignedCount: Math.max(0, eligibleCount - assignedCount),
            confirmed: source.confirmed && assignedCount === eligibleCount,
            papers: source.papers.map((paper) => {
                const assigned = counts.get(idValue(paper.id)) || 0;
                return {
                    ...paper,
                    assignedCount: assigned,
                    percentage: eligibleCount ? Math.round((assigned / eligibleCount) * 10000) / 100 : 0,
                };
            }),
        };
    }, [effectivePlayers, summaryQuery.data]);
    const dirty = useMemo(() => {
        const keys = new Set([...Object.keys(baselineAssignments), ...Object.keys(draftAssignments)]);
        return [...keys].some((key) => (baselineAssignments[key] || null) !== (draftAssignments[key] || null));
    }, [baselineAssignments, draftAssignments]);
    useEffect(() => { onDirtyChange?.(dirty); }, [dirty, onDirtyChange]);
    const organizationOptions = useMemo(() => {
        const byId = new Map();
        effectivePlayers.forEach((player) => {
            const value = idValue(player.orgId);
            if (value)
                byId.set(value, player.orgName || value);
        });
        return [...byId.entries()]
            .sort((left, right) => left[1].localeCompare(right[1], 'zh-CN'))
            .map(([value, label]) => ({ value, label }));
    }, [effectivePlayers]);
    const assignmentItems = () => effectivePlayers
        .filter((player) => (baselineAssignments[playerKey(player)] || null) !== (idValue(player.paperSnapshotId) || null))
        .map((player) => ({
        contestantProfileId: player.contestantProfileId,
        paperSnapshotId: idValue(player.paperSnapshotId) || null,
    }));
    const acceptLocalChanges = (items) => {
        const next = { ...draftAssignments };
        items.forEach((item) => {
            next[idValue(item.contestantProfileId)] = idValue(item.paperSnapshotId) || null;
        });
        setDraftAssignments(next);
    };
    const invalidateAssignmentQueries = async () => {
        await Promise.all([
            queryClient.invalidateQueries({ queryKey: ['competition-stage-paper-assignment-summary', stageId, assignmentContentType] }),
            queryClient.invalidateQueries({ queryKey: ['competition-stage-paper-assignment-players', stageId, assignmentContentType] }),
            queryClient.invalidateQueries({ queryKey: ['competition-stage-paper-assignment-visualization', stageId, assignmentContentType] }),
        ]);
    };
    const autoAllocateMutation = useMutation({
        mutationFn: () => assignmentContentType === 'practical'
            ? autoAllocateCompetitionStagePaperAssignments(stageId, assignmentContentType)
            : autoAllocateCompetitionStagePaperAssignments(stageId),
        onSuccess: async () => {
            await invalidateAssignmentQueries();
            message.success('已按平均随机策略重新分配试卷');
        },
        onError: (error) => message.error(errorText(error, '自动分配失败，请刷新后重试')),
    });
    const saveMutation = useMutation({
        mutationFn: () => assignmentContentType === 'practical'
            ? saveCompetitionStagePaperAssignments(stageId, assignmentItems(), assignmentContentType)
            : saveCompetitionStagePaperAssignments(stageId, assignmentItems()),
        onSuccess: async () => {
            const next = { ...draftAssignments };
            setBaselineAssignments(next);
            await invalidateAssignmentQueries();
            message.success('选手试卷配置已保存');
        },
        onError: (error) => message.error(errorText(error, '保存试卷配置失败，请刷新后重试')),
    });
    const confirmMutation = useMutation({
        mutationFn: () => assignmentContentType === 'practical'
            ? confirmCompetitionStagePaperAssignments(stageId, assignmentItems(), assignmentContentType)
            : confirmCompetitionStagePaperAssignments(stageId, assignmentItems()),
        onSuccess: async () => {
            const next = { ...draftAssignments };
            setBaselineAssignments(next);
            await invalidateAssignmentQueries();
            message.success('选手试卷配置已确认');
        },
        onError: (error) => message.error(errorText(error, '确认试卷配置失败，请先检查未分配选手')),
    });
    const tableRows = effectivePlayers.filter((player) => {
        const search = keyword.trim().toLowerCase();
        return (!search || [player.name, player.contestantNo]
            .some((value) => value?.toLowerCase().includes(search)))
            && (!orgId || idValue(player.orgId) === orgId)
            && (!paperSnapshotId || idValue(player.paperSnapshotId) === paperSnapshotId)
            && (!assignmentStatus || player.assignmentStatus === assignmentStatus);
    });
    const tableColumns = [
        { title: '姓名', dataIndex: 'name', width: 110, render: (value) => value || '-' },
        { title: '选手编号', width: 150, dataIndex: 'contestantNo', render: (value) => value || '-' },
        { title: '所属单位', dataIndex: 'orgName', ellipsis: true, render: (value) => value || '-' },
        { title: '工种', dataIndex: 'workTypeName', width: 140, render: (value) => value || '-' },
        {
            title: '技能等级', width: 135,
            render: (_, row) => row.levelName ? `${row.levelName}（${row.levelCode || '-'}）` : row.rosterLevel || '-',
        },
        {
            title: '当前试卷', width: 180,
            render: (_, row) => row.paperSnapshotId
                ? _jsx(Tag, { color: "purple", children: paperLabel(paperById.get(idValue(row.paperSnapshotId))) })
                : _jsx(Tag, { children: "\u672A\u5206\u914D" }),
        },
        {
            title: '分配状态', width: 100,
            render: (_, row) => row.assignmentStatus === 'assigned'
                ? _jsx(Badge, { status: "success", text: "\u5DF2\u5206\u914D" })
                : _jsx(Badge, { status: "warning", text: "\u672A\u5206\u914D" }),
        },
        ...(assignmentContentType === 'practical' ? [{
                title: '考评表', width: 160,
                render: (_, row) => row.assessmentSheetId
                    ? _jsx(Tag, { color: "orange", children: row.assessmentSheetCode || '已同步绑定' })
                    : _jsx(Tag, { color: "warning", children: "\u5F85\u4FDD\u5B58\u540C\u6B65" }),
            }] : []),
        {
            title: '操作', width: 220,
            render: (_, row) => canManageEffective ? (_jsx(Select, { size: "small", style: { width: 190 }, value: idValue(row.paperSnapshotId) || UNASSIGNED, options: [{ label: '未分配', value: UNASSIGNED }, ...paperOptions], onChange: (value) => acceptLocalChanges([{
                        contestantProfileId: row.contestantProfileId,
                        paperSnapshotId: value === UNASSIGNED ? null : value,
                    }]) })) : _jsx(Typography.Text, { type: "secondary", children: "\u5DF2\u9501\u5B9A" }),
        },
    ];
    const selectBatchPaper = () => {
        if (!batchPaperId) {
            message.warning('请选择目标试卷或取消分配');
            return;
        }
        if (!selectedProfileIds.length) {
            message.warning('请先勾选需要调整的选手');
            return;
        }
        const items = selectedProfileIds
            .map((profileId) => effectivePlayerById.get(profileId))
            .filter((player) => Boolean(player))
            .map((player) => ({
            contestantProfileId: player.contestantProfileId,
            paperSnapshotId: batchPaperId === UNASSIGNED ? null : batchPaperId,
        }));
        if (!items.length)
            return;
        acceptLocalChanges(items);
        setSelectedProfileIds([]);
        setBatchPaperId(undefined);
    };
    const currentVisualPaperPlayers = effectivePlayers.filter((player) => idValue(player.paperSnapshotId) === visualPaperId);
    const currentVisualPaperPlayerIds = new Set(currentVisualPaperPlayers.map(playerKey));
    const visualKeyword = visualSearch.trim().toLowerCase();
    const visualLeftPlayers = effectivePlayers.filter((player) => {
        if (currentVisualPaperPlayerIds.has(playerKey(player)))
            return false;
        if (!visualKeyword)
            return true;
        return [player.name, player.contestantNo, player.orgName]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(visualKeyword));
    });
    const visualRightPlayers = currentVisualPaperPlayers.filter((player) => {
        if (!visualKeyword)
            return true;
        return [player.name, player.contestantNo, player.orgName]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(visualKeyword));
    });
    const toggleVisualPlayer = (profileId) => {
        setVisualSelectedIds((current) => current.includes(profileId)
            ? current.filter((id) => id !== profileId)
            : [...current, profileId]);
    };
    const moveVisualPlayers = (direction) => {
        const source = direction === 'in' ? visualLeftPlayers : visualRightPlayers;
        const sourceIds = new Set(source.map(playerKey));
        const selected = visualSelectedIds.filter((id) => sourceIds.has(id));
        if (!selected.length) {
            message.warning(direction === 'in' ? '请先从左侧选择选手' : '请先从右侧选择选手');
            return;
        }
        acceptLocalChanges(selected.map((profileId) => ({
            contestantProfileId: profileId,
            paperSnapshotId: direction === 'in' ? visualPaperId : null,
        })));
        setVisualSelectedIds([]);
    };
    if (!canView)
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u9009\u624B\u8BD5\u5377\u914D\u7F6E\u67E5\u770B\u6743\u9650" });
    if (summaryQuery.isLoading || visualizationQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (summaryQuery.isError || visualizationQuery.isError) {
        return _jsx(Result, { status: "error", title: "\u9009\u624B\u8BD5\u5377\u914D\u7F6E\u52A0\u8F7D\u5931\u8D25", extra: _jsx(Button, { onClick: () => { summaryQuery.refetch(); visualizationQuery.refetch(); }, children: "\u91CD\u65B0\u52A0\u8F7D" }) });
    }
    const summary = effectiveSummary;
    const autoAllocate = () => Modal.confirm({
        title: '重新自动分配试卷？',
        content: '该操作会覆盖当前赛段的现有分卷结果，未保存的人工调整也会被覆盖。',
        okText: '重新分配',
        cancelText: '取消',
        onOk: () => autoAllocateMutation.mutateAsync(),
    });
    const confirmAssignment = () => Modal.confirm({
        title: '确认选手试卷配置？',
        content: '确认将保存当前分卷结果并标记为已确认；后续调整保存后需重新确认。',
        okText: '确认配置',
        cancelText: '取消',
        onOk: () => confirmMutation.mutateAsync(),
    });
    return (_jsxs("div", { className: "competition-paper-assignment-page", children: [_jsxs("div", { className: "competition-paper-assignment-page__header", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: assignmentContentType === 'practical' ? '实操选手配置 · 试卷分配' : '选手配置 · 试卷分配' }), _jsx(Typography.Text, { type: "secondary", children: assignmentContentType === 'practical'
                                    ? '为当前赛段符合工种和技能等级要求的选手分配已绑定实操试卷；保存时系统同步绑定该试卷的考评表。'
                                    : '为当前赛段符合工种和技能等级要求的选手分配已配置试卷；已导入待激活选手也会出现在列表中。' })] }), _jsxs(Space, { children: [summary?.confirmed && !dirty && _jsx(Tag, { color: "success", icon: _jsx(CheckCircleOutlined, {}), children: "\u5DF2\u786E\u8BA4" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    if (dirty)
                                        Modal.confirm({ title: '放弃未保存的调整并刷新？', onOk: invalidateAssignmentQueries });
                                    else
                                        void invalidateAssignmentQueries();
                                }, children: "\u5237\u65B0" })] })] }), summary && summary.eligibleCount === 0 && (_jsx(Alert, { style: { marginBottom: 16 }, type: "info", showIcon: true, message: "\u5F53\u524D\u8D5B\u6BB5\u6682\u65E0\u9009\u624B", description: "\u9009\u624B\u7BA1\u7406\u4E2D\u5DF2\u5BFC\u5165\u7684\u540D\u5355\u4F1A\u51FA\u73B0\u5728\u8FD9\u91CC\uFF0C\u5305\u62EC\u5F85\u6FC0\u6D3B\u9009\u624B\u3002\u82E5\u4ECD\u4E3A\u7A7A\uFF0C\u8BF7\u786E\u8BA4\u8BE5\u9009\u624B\u7684\u8D5B\u4E8B\u5DE5\u79CD\u3001\u6280\u80FD\u7B49\u7EA7\u4E0E\u5F53\u524D\u8D5B\u6BB5\u4E00\u81F4\uFF0C\u5E76\u5148\u5728\u7ADE\u8D5B\u8BA1\u5212\u4E2D\u53D1\u5E03\u8D5B\u4E8B\u4EE5\u4FBF\u9009\u624B\u6FC0\u6D3B\u3002" })), locked && (_jsx(Alert, { style: { marginBottom: 16 }, type: "warning", showIcon: true, message: "\u6B63\u5F0F\u6BD4\u8D5B\u6570\u636E\u5DF2\u751F\u6210\uFF0C\u5F53\u524D\u8BD5\u5377\u5206\u914D\u5DF2\u9501\u5B9A\u3002", description: assignmentContentType === 'practical'
                    ? '正式实操数据将使用生成时的运行快照。如需核对选手试卷，请从竞赛菜单进入实操比赛控制台。'
                    : '正式比赛将使用生成时的运行快照。如需核对选手试卷，请从竞赛菜单进入理论比赛控制台。' })), summary && (_jsxs(_Fragment, { children: [_jsx(Card, { size: "small", className: "competition-paper-assignment-page__scope", children: _jsxs(Space, { wrap: true, split: _jsx(Typography.Text, { type: "secondary", children: "/" }), children: [_jsx(Typography.Text, { strong: true, children: summary.stageName }), _jsx(Typography.Text, { children: summary.workTypeName }), _jsxs(Typography.Text, { children: [summary.levelName, "\uFF08", summary.levelCode, "\uFF09"] })] }) }), _jsxs("div", { className: "competition-paper-assignment-page__stats", children: [_jsx(Card, { size: "small", children: _jsx(Statistic, { title: "\u7B26\u5408\u6761\u4EF6\u9009\u624B", value: summary.eligibleCount, suffix: "\u4EBA" }) }), _jsx(Card, { size: "small", children: _jsx(Statistic, { title: "\u5F53\u524D\u914D\u7F6E\u8BD5\u5377", value: summary.paperCount, suffix: "\u5957" }) }), _jsx(Card, { size: "small", children: _jsx(Statistic, { title: "\u5DF2\u5206\u914D", value: summary.assignedCount, suffix: "\u4EBA", valueStyle: { color: '#389e0d' } }) }), _jsx(Card, { size: "small", children: _jsx(Statistic, { title: "\u672A\u5206\u914D", value: summary.unassignedCount, suffix: "\u4EBA", valueStyle: { color: summary.unassignedCount ? '#d46b08' : '#389e0d' } }) })] }), _jsx(Card, { size: "small", title: "\u5206\u914D\u7B56\u7565", extra: canManageEffective && _jsx(Button, { type: "primary", icon: _jsx(ThunderboltOutlined, {}), loading: autoAllocateMutation.isPending, disabled: !summary.paperCount, onClick: autoAllocate, children: "\u91CD\u65B0\u81EA\u52A8\u5206\u914D" }), children: _jsxs("div", { className: "competition-paper-assignment-page__strategy", children: [_jsx(RadioStrategy, {}), _jsx(Typography.Text, { type: "secondary", children: "\u6309\u5F53\u524D\u8BD5\u5377\u6570\u91CF\u5E73\u5747\u5206\u914D\uFF0C\u5148\u968F\u673A\u6253\u6563\u9009\u624B\uFF0C\u4EBA\u6570\u4E0D\u80FD\u6574\u9664\u65F6\u76F8\u90BB\u8BD5\u5377\u6700\u591A\u76F8\u5DEE 1 \u4EBA\u3002" })] }) }), _jsx(Card, { size: "small", title: "\u5206\u914D\u6982\u89C8", className: "competition-paper-assignment-page__overview", children: _jsxs("div", { className: "competition-paper-assignment-page__paper-grid", children: [(summary.papers || []).map((paper) => (_jsxs(Card, { size: "small", className: "competition-paper-assignment-page__paper-card", children: [_jsxs("div", { className: "competition-paper-assignment-page__paper-card-title", children: [_jsxs(Typography.Text, { strong: true, children: [paper.paperCode, "\u5377"] }), _jsxs(Tag, { color: "purple", children: ["v", paper.versionNo] })] }), _jsx(Typography.Text, { type: "secondary", ellipsis: true, children: paper.title || '未命名试卷' }), _jsxs("div", { className: "competition-paper-assignment-page__paper-card-count", children: [paper.assignedCount, " ", _jsx("small", { children: "\u4EBA" })] }), _jsx(Progress, { percent: percentValue(paper.percentage), size: "small", format: (value) => `${value}%` })] }, idValue(paper.id)))), !summary.papers.length && _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u5F53\u524D\u8D5B\u6BB5\u5C1A\u672A\u914D\u7F6E\u8BD5\u5377" })] }) })] })), _jsx(Card, { size: "small", className: "competition-paper-assignment-page__workspace", children: _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                        {
                            key: 'table',
                            label: _jsxs("span", { children: [_jsx(UserSwitchOutlined, {}), " \u9009\u624B\u5217\u8868"] }),
                            children: (_jsxs(_Fragment, { children: [_jsxs("div", { className: "competition-paper-assignment-page__toolbar", children: [_jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u59D3\u540D\u6216\u9009\u624B\u7F16\u53F7", value: keyword, style: { width: 250 }, onChange: (event) => { setKeyword(event.target.value); setPage(1); } }), _jsx(Select, { allowClear: true, placeholder: "\u6240\u5C5E\u5355\u4F4D", style: { width: 180 }, value: orgId, options: organizationOptions, onChange: (value) => { setOrgId(value); setPage(1); } }), _jsx(Select, { allowClear: true, placeholder: "\u5F53\u524D\u8BD5\u5377", style: { width: 180 }, value: paperSnapshotId, options: paperOptions, onChange: (value) => { setPaperSnapshotId(value); setPage(1); } }), _jsx(Select, { allowClear: true, placeholder: "\u5206\u914D\u72B6\u6001", style: { width: 140 }, value: assignmentStatus, options: [{ value: 'assigned', label: '已分配' }, { value: 'unassigned', label: '未分配' }], onChange: (value) => { setAssignmentStatus(value); setPage(1); } }), canManageEffective && (_jsxs(Space.Compact, { children: [_jsx(Select, { placeholder: "\u6279\u91CF\u5206\u914D\u5230", style: { width: 180 }, value: batchPaperId, options: [{ label: '取消分配', value: UNASSIGNED }, ...paperOptions], onChange: setBatchPaperId }), _jsx(Button, { disabled: !selectedProfileIds.length, onClick: selectBatchPaper, children: "\u6279\u91CF\u8C03\u6574" })] }))] }), _jsx(Table, { rowKey: (row) => playerKey(row), size: "small", scroll: { x: 1250 }, loading: visualizationQuery.isFetching, dataSource: tableRows, columns: tableColumns, rowSelection: canManageEffective ? {
                                            selectedRowKeys: selectedProfileIds,
                                            preserveSelectedRowKeys: true,
                                            onChange: (keys) => setSelectedProfileIds(keys.map(String)),
                                        } : undefined, pagination: {
                                            current: page,
                                            pageSize: 20,
                                            total: tableRows.length,
                                            showSizeChanger: false,
                                            onChange: setPage,
                                        }, locale: { emptyText: '暂无选手。已导入待激活选手应显示在此，请核对工种、等级，并确认赛事已发布。' } })] })),
                        },
                        {
                            key: 'visual',
                            label: _jsxs("span", { children: [_jsx(UserSwitchOutlined, {}), " \u53EF\u89C6\u5316\u8C03\u6574"] }),
                            children: (_jsxs("div", { className: "competition-paper-assignment-page__visual", children: [_jsxs("div", { className: "competition-paper-assignment-page__visual-tabs", children: [_jsx(Typography.Text, { type: "secondary", children: "\u5F53\u524D\u8C03\u6574\u8BD5\u5377\uFF1A" }), papers.map((paper) => (_jsxs(Button, { size: "small", type: visualPaperId === idValue(paper.id) ? 'primary' : 'default', onClick: () => { setVisualPaperId(idValue(paper.id)); setVisualSelectedIds([]); }, children: [paper.paperCode, "\u5377"] }, idValue(paper.id))))] }), !papers.length ? _jsx(Empty, { description: "\u8BF7\u5148\u5B8C\u6210\u5F53\u524D\u8D5B\u6BB5\u8BD5\u5377\u914D\u7F6E" }) : (_jsxs("div", { className: "competition-paper-assignment-page__visual-grid", children: [_jsx(VisualPlayerList, { title: "\u9009\u624B\u5217\u8868", description: "\u9009\u62E9\u540E\u79FB\u5165\u5F53\u524D\u8BD5\u5377", players: visualLeftPlayers, selectedIds: visualSelectedIds, onToggle: toggleVisualPlayer, search: visualSearch, onSearch: setVisualSearch, disabled: !canManageEffective }), canManageEffective && (_jsxs("div", { className: "competition-paper-assignment-page__visual-actions", children: [_jsx(Button, { type: "primary", icon: _jsx(ArrowRightOutlined, {}), onClick: () => moveVisualPlayers('in'), children: "\u79FB\u5165" }), _jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: () => moveVisualPlayers('out'), children: "\u79FB\u51FA" })] })), _jsx(VisualPlayerList, { title: paperLabel(visualPaperId ? paperById.get(visualPaperId) : undefined), description: `当前 ${currentVisualPaperPlayers.length} 人`, players: visualRightPlayers, selectedIds: visualSelectedIds, onToggle: toggleVisualPlayer, emptyDescription: "\u5F53\u524D\u8BD5\u5377\u6682\u65E0\u9009\u624B", disabled: !canManageEffective })] }))] })),
                        },
                    ] }) }), _jsxs("div", { className: "competition-paper-assignment-page__footer", children: [_jsx(Typography.Text, { type: dirty ? 'warning' : 'secondary', children: dirty ? '有未保存的人工调整' : '当前配置已保存' }), _jsx(Space, { children: !locked && (_jsxs(_Fragment, { children: [_jsx(Button, { icon: _jsx(SaveOutlined, {}), disabled: !canManageEffective || !dirty, loading: saveMutation.isPending, onClick: () => saveMutation.mutate(), children: "\u4FDD\u5B58" }), _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), disabled: !canManageEffective || !canConfirm || !summary || !summary.eligibleCount || summary.unassignedCount > 0, loading: confirmMutation.isPending, onClick: confirmAssignment, children: "\u786E\u8BA4\u914D\u7F6E" })] })) })] })] }));
}
function RadioStrategy() {
    return (_jsxs("div", { className: "competition-paper-assignment-page__radio-strategy", children: [_jsx("span", { className: "competition-paper-assignment-page__radio-dot" }), _jsx(Typography.Text, { strong: true, children: "\u5E73\u5747\u968F\u673A\u5206\u914D" })] }));
}
function VisualPlayerList({ title, description, players, selectedIds, onToggle, search, onSearch, emptyDescription = '暂无选手', disabled = false, }) {
    return (_jsxs(Card, { size: "small", className: "competition-paper-assignment-page__visual-card", title: title, extra: _jsx(Typography.Text, { type: "secondary", children: description }), children: [onSearch && _jsx(Input, { allowClear: true, size: "small", prefix: _jsx(SearchOutlined, {}), placeholder: "\u7B5B\u9009\u9009\u624B", value: search, onChange: (event) => onSearch(event.target.value), style: { marginBottom: 8 } }), _jsx(List, { size: "small", bordered: true, className: "competition-paper-assignment-page__visual-list", dataSource: players, locale: { emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: emptyDescription }) }, renderItem: (player) => {
                    const key = playerKey(player);
                    return (_jsxs(List.Item, { onClick: () => { if (!disabled)
                            onToggle(key); }, className: "competition-paper-assignment-page__visual-item", children: [!disabled && (_jsx(Checkbox, { checked: selectedIds.includes(key), onChange: () => onToggle(key), onClick: (event) => event.stopPropagation() })), _jsxs("div", { className: "competition-paper-assignment-page__visual-item-main", children: [_jsx(Typography.Text, { ellipsis: true, children: player.name || '-' }), _jsxs(Typography.Text, { type: "secondary", ellipsis: true, children: [player.contestantNo || '-', " \u00B7 ", player.orgName || '-'] })] }), player.paperCode && _jsxs(Tag, { color: "purple", children: [player.paperCode, "\u5377"] })] }));
                } })] }));
}

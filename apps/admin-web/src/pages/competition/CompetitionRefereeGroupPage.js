import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, Modal, Popconfirm, Result, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { useMemo, useState } from 'react';
import { confirmCompetitionRefereeGroup, createCompetitionRefereeGroup, deleteCompetitionRefereeGroup, listCompetitionRefereeGroups, pageCompetitionReferees, releaseCompetitionRefereeGroup, updateCompetitionRefereeGroup, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
function idValue(value) {
    return value === undefined || value === null ? '' : String(value);
}
function errorText(error, fallback) {
    return error instanceof Error ? error.message : fallback;
}
function groupStatusTag(status) {
    if (status === 'confirmed')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u786E\u5B9A" });
    if (status === 'closed')
        return _jsx(Tag, { color: "default", children: "\u5DF2\u89E3\u6563" });
    return _jsx(Tag, { color: "warning", children: "\u5F85\u786E\u5B9A" });
}
function taskStatusTag(status) {
    if (status === 'completed')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u5B8C\u6210" });
    if (status === 'in_progress')
        return _jsx(Tag, { color: "processing", children: "\u8FDB\u884C\u4E2D" });
    return _jsx(Tag, { color: "warning", children: "\u5F85\u5F00\u8D5B" });
}
function boundTaskColumns() {
    return [
        { title: '赛事', dataIndex: 'eventName', render: (value) => value || '-' },
        { title: '赛段', dataIndex: 'stageName', render: (value) => value || '-' },
        { title: '工种', dataIndex: 'workTypeName', width: 140, render: (value) => value || '-' },
        { title: '等级', dataIndex: 'levelName', width: 100, render: (value) => value || '-' },
        {
            title: '选手',
            width: 110,
            render: (_, task) => `${task.completedCount ?? 0}/${task.contestantCount ?? 0}`,
        },
        {
            title: '任务状态',
            dataIndex: 'status',
            width: 110,
            render: (status) => taskStatusTag(status),
        },
    ];
}
export function CompetitionRefereeGroupPage() {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:referee-group:view')
        || permissions.includes('competition:referee:view');
    const canManage = permissions.includes('competition:referee-group:manage')
        || permissions.includes('competition:referee:create')
        || permissions.includes('competition:referee:update');
    const canDelete = permissions.includes('competition:referee-group:manage')
        || permissions.includes('competition:referee:delete');
    const queryClient = useQueryClient();
    const [groupModalOpen, setGroupModalOpen] = useState(false);
    const [editingGroup, setEditingGroup] = useState(null);
    const [groupName, setGroupName] = useState('');
    const [memberProfileIds, setMemberProfileIds] = useState([]);
    const [leaderProfileId, setLeaderProfileId] = useState();
    const groupsQuery = useQuery({
        queryKey: ['competition-referee-groups'],
        queryFn: () => listCompetitionRefereeGroups(),
        enabled: canView,
    });
    const refereesQuery = useQuery({
        queryKey: ['competition-referees', 'referee-groups'],
        queryFn: () => pageCompetitionReferees({ page: 1, pageSize: 200, status: 'active' }),
        enabled: canView,
    });
    const invalidate = async () => {
        await queryClient.invalidateQueries({ queryKey: ['competition-referee-groups'] });
    };
    const groupMutation = useMutation({
        mutationFn: (request) => editingGroup
            ? updateCompetitionRefereeGroup(editingGroup.id, request)
            : createCompetitionRefereeGroup(request),
        onSuccess: async () => {
            message.success(editingGroup ? '裁判小组已更新' : '裁判小组已创建，请确定后再绑定赛段');
            setGroupModalOpen(false);
            setEditingGroup(null);
            await invalidate();
        },
        onError: (error) => message.error(errorText(error, '裁判小组保存失败，请刷新后重试')),
    });
    const confirmMutation = useMutation({
        mutationFn: (groupId) => confirmCompetitionRefereeGroup(groupId),
        onSuccess: async () => {
            message.success('裁判小组已确定，可在赛段实操配置中绑定');
            await invalidate();
        },
        onError: (error) => message.error(errorText(error, '裁判小组确定失败，请检查是否配置了3名裁判和1名组长')),
    });
    const releaseMutation = useMutation({
        mutationFn: (groupId) => releaseCompetitionRefereeGroup(groupId),
        onSuccess: async () => {
            message.success('裁判小组已解散，组内裁判可重新编组');
            await invalidate();
        },
        onError: (error) => message.error(errorText(error, '裁判小组解散失败，请确认全部赛段任务已完成')),
    });
    const deleteMutation = useMutation({
        mutationFn: (groupId) => deleteCompetitionRefereeGroup(groupId),
        onSuccess: async () => {
            message.success('裁判小组已删除');
            await invalidate();
        },
        onError: (error) => message.error(errorText(error, '裁判小组删除失败，请先解除赛段绑定或解散小组')),
    });
    const groups = groupsQuery.data ?? [];
    const referees = refereesQuery.data?.items ?? [];
    const occupiedProfileIds = useMemo(() => {
        const occupied = new Map();
        groups.forEach((group) => {
            if (group.status === 'closed')
                return;
            if (editingGroup && idValue(group.id) === idValue(editingGroup.id))
                return;
            group.members.forEach((member) => {
                const profileId = idValue(member.refereeProfileId);
                if (profileId)
                    occupied.set(profileId, group.groupName);
            });
        });
        return occupied;
    }, [editingGroup, groups]);
    const refereeOptions = useMemo(() => referees.map((referee) => {
        const value = idValue(referee.id);
        const occupiedBy = occupiedProfileIds.get(value);
        return {
            label: `${referee.name}${occupiedBy ? `（已在「${occupiedBy}」）` : referee.qualifications?.length
                ? `（${referee.qualifications.map((item) => item.workTypeName || idValue(item.workTypeId)).join('、')}）`
                : ''}`,
            value,
            disabled: Boolean(occupiedBy),
        };
    }), [occupiedProfileIds, referees]);
    const leaderOptions = useMemo(() => refereeOptions.filter((option) => memberProfileIds.includes(String(option.value))), [memberProfileIds, refereeOptions]);
    const openCreateModal = () => {
        setEditingGroup(null);
        setGroupName('');
        setMemberProfileIds([]);
        setLeaderProfileId(undefined);
        setGroupModalOpen(true);
    };
    const openEditModal = (group) => {
        setEditingGroup(group);
        setGroupName(group.groupName);
        setMemberProfileIds(group.members
            .map((member) => idValue(member.refereeProfileId))
            .filter(Boolean));
        setLeaderProfileId(group.leaderRefereeProfileId == null
            ? undefined : idValue(group.leaderRefereeProfileId));
        setGroupModalOpen(true);
    };
    const submitGroup = () => {
        if (!groupName.trim()) {
            message.warning('请输入裁判小组名称');
            return;
        }
        if (memberProfileIds.length !== 3) {
            message.warning('每个裁判小组必须配置3名裁判');
            return;
        }
        if (!leaderProfileId || !memberProfileIds.includes(leaderProfileId)) {
            message.warning('请选择一名已配置的裁判作为组长');
            return;
        }
        groupMutation.mutate({
            groupName: groupName.trim(),
            refereeProfileIds: memberProfileIds,
            leaderRefereeProfileId: leaderProfileId,
        });
    };
    const columns = useMemo(() => [
        { title: '组号', dataIndex: 'groupNo', width: 80 },
        { title: '裁判小组', dataIndex: 'groupName', width: 180 },
        {
            title: '组内裁判',
            render: (_, group) => (_jsx(Space, { wrap: true, size: [4, 4], children: group.members.map((member) => (_jsxs(Tag, { color: member.memberRole === 'leader' ? 'blue' : undefined, children: [member.refereeName || '未命名裁判', member.memberRole === 'leader' ? '（组长）' : ''] }, idValue(member.id)))) })),
        },
        {
            title: '已绑定任务',
            width: 220,
            render: (_, group) => {
                const tasks = group.boundTasks ?? [];
                if (!tasks.length)
                    return _jsx(Typography.Text, { type: "secondary", children: "\u672A\u7ED1\u5B9A\u8D5B\u6BB5" });
                return (_jsx(Space, { direction: "vertical", size: 0, children: tasks.map((task) => (_jsxs(Typography.Text, { children: [task.eventName || '赛事', " / ", task.stageName || '赛段', " ", taskStatusTag(task.status)] }, `${idValue(task.eventId)}-${idValue(task.stageId)}`))) }));
            },
        },
        { title: '状态', dataIndex: 'status', width: 100, render: (status) => groupStatusTag(status) },
        {
            title: '操作',
            width: 280,
            render: (_, group) => (_jsxs(Space, { wrap: true, children: [canManage && group.status !== 'closed' && (_jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEditModal(group), children: "\u7F16\u8F91" })), canManage && group.status === 'draft' && (_jsx(Popconfirm, { title: "\u786E\u5B9A\u8BE5\u88C1\u5224\u5C0F\u7EC4\uFF1F", description: "\u786E\u5B9A\u540E\u53EF\u5728\u8D5B\u6BB5\u5B9E\u64CD\u914D\u7F6E\u4E2D\u7ED1\u5B9A\u3002\u540C\u4E00\u5C0F\u7EC4\u4E0D\u80FD\u540C\u65F6\u627F\u62C5\u4E0D\u540C\u8D5B\u4E8B\uFF0C\u4F46\u53EF\u53C2\u52A0\u540C\u4E00\u8D5B\u4E8B\u7684\u591A\u4E2A\u8D5B\u6BB5\u3002", onConfirm: () => confirmMutation.mutate(group.id), okText: "\u786E\u5B9A", cancelText: "\u53D6\u6D88", children: _jsx(Button, { size: "small", type: "primary", loading: confirmMutation.isPending, children: "\u786E\u5B9A\u5C0F\u7EC4" }) })), canManage && group.releasable && group.status === 'confirmed' && (_jsx(Popconfirm, { title: "\u786E\u8BA4\u89E3\u6563\u8BE5\u88C1\u5224\u5C0F\u7EC4\uFF1F", description: "\u89E3\u6563\u540E\u7EC4\u5185\u88C1\u5224\u53EF\u91CD\u65B0\u7F16\u5165\u5176\u4ED6\u5C0F\u7EC4\u3002\u5386\u53F2\u7ED1\u5B9A\u4EFB\u52A1\u4ECD\u53EF\u67E5\u770B\u3002", onConfirm: () => releaseMutation.mutate(group.id), okText: "\u89E3\u6563", cancelText: "\u53D6\u6D88", children: _jsx(Button, { size: "small", loading: releaseMutation.isPending, children: "\u89E3\u6563\u5C0F\u7EC4" }) })), canDelete && (_jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u88C1\u5224\u5C0F\u7EC4\uFF1F", description: "\u5DF2\u7ED1\u5B9A\u672A\u5B8C\u6210\u8D5B\u6BB5\u7684\u5C0F\u7EC4\u4E0D\u80FD\u5220\u9664\uFF1B\u4EFB\u52A1\u5168\u90E8\u5B8C\u6210\u540E\u8BF7\u5148\u89E3\u6563\u3002", onConfirm: () => deleteMutation.mutate(group.id), okText: "\u5220\u9664", cancelText: "\u53D6\u6D88", children: _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), loading: deleteMutation.isPending, children: "\u5220\u9664" }) }))] })),
        },
    ], [canDelete, canManage, confirmMutation, deleteMutation, releaseMutation]);
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u88C1\u5224\u5C0F\u7EC4\u67E5\u770B\u6743\u9650", subTitle: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u7533\u8BF7\u7ADE\u8D5B\u88C1\u5224\u7BA1\u7406\u6743\u9650\u3002" });
    }
    return (_jsxs("div", { "data-testid": "competition-referee-group-page", children: [_jsx(Card, { bordered: false, className: "competition-filter-card", children: _jsxs("div", { className: "competition-filter-card__head", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u88C1\u5224\u5C0F\u7EC4" }), _jsx(Typography.Text, { type: "secondary", children: "\u4E00\u540D\u88C1\u5224\u53EA\u80FD\u8FDB\u5165\u4E00\u4E2A\u5C0F\u7EC4\u3002\u5C0F\u7EC4\u540C\u4E00\u65F6\u95F4\u53EA\u80FD\u627F\u62C5\u4E00\u573A\u8D5B\u4E8B\uFF0C\u4F46\u53EF\u53C2\u52A0\u8BE5\u8D5B\u4E8B\u7684\u591A\u4E2A\u8D5B\u6BB5\uFF1B\u5168\u90E8\u8D5B\u6BB5\u4EFB\u52A1\u5B8C\u6210\u540E\u53EF\u5728\u6B64\u89E3\u6563\u5C0F\u7EC4\u3002" })] }), _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void groupsQuery.refetch(), children: "\u5237\u65B0" }), canManage && _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreateModal, children: "\u65B0\u5EFA\u88C1\u5224\u5C0F\u7EC4" })] })] }) }), _jsx(Card, { size: "small", style: { marginTop: 16 }, children: _jsx(Table, { rowKey: (row) => idValue(row.id), loading: groupsQuery.isLoading || groupsQuery.isFetching || groupMutation.isPending, dataSource: groups, columns: columns, pagination: false, locale: { emptyText: '尚未创建裁判小组' }, expandable: {
                        rowExpandable: (group) => (group.boundTasks?.length ?? 0) > 0,
                        expandedRowRender: (group) => (_jsx(Table, { rowKey: (task) => `${idValue(task.eventId)}-${idValue(task.stageId)}-${idValue(task.eventWorkTypeLevelId)}`, size: "small", pagination: false, columns: boundTaskColumns(), dataSource: group.boundTasks ?? [] })),
                    } }) }), _jsx(Modal, { title: editingGroup ? '编辑裁判小组' : '新建裁判小组', open: groupModalOpen, onCancel: () => setGroupModalOpen(false), onOk: submitGroup, okText: "\u4FDD\u5B58\u88C1\u5224\u5C0F\u7EC4", cancelText: "\u53D6\u6D88", confirmLoading: groupMutation.isPending, width: 760, destroyOnClose: true, children: _jsxs(Form, { layout: "vertical", children: [_jsx(Form.Item, { label: "\u88C1\u5224\u5C0F\u7EC4\u540D\u79F0", required: true, children: _jsx(Input, { maxLength: 128, placeholder: "\u4F8B\u5982\uFF1A\u7B2C\u4E00\u88C1\u5224\u5C0F\u7EC4", value: groupName, onChange: (event) => setGroupName(event.target.value) }) }), _jsx(Form.Item, { label: "\u7EC4\u5185\u88C1\u5224\uFF08\u56FA\u5B9A3\u540D\uFF09", required: true, children: _jsx(Select, { mode: "multiple", style: { width: '100%' }, placeholder: "\u4ECE\u88C1\u5224\u4FE1\u606F\u4E2D\u9009\u62E93\u540D\u672A\u7F16\u7EC4\u88C1\u5224", value: memberProfileIds, options: refereeOptions, loading: refereesQuery.isLoading, onChange: (values) => {
                                    setMemberProfileIds(values);
                                    if (leaderProfileId && !values.includes(leaderProfileId))
                                        setLeaderProfileId(undefined);
                                } }) }), _jsx(Form.Item, { label: "\u7EC4\u957F", required: true, children: _jsx(Select, { style: { width: '100%' }, placeholder: "\u4ECE\u7EC4\u5185\u88C1\u5224\u4E2D\u9009\u62E9\u4E00\u540D\u7EC4\u957F", value: leaderProfileId, options: leaderOptions, onChange: setLeaderProfileId, disabled: memberProfileIds.length === 0 }) }), _jsx(Form.Item, { label: "\u5DF2\u7ED1\u5B9A\u4EFB\u52A1", children: (editingGroup?.boundTasks?.length ?? 0) > 0 ? (_jsx(Table, { rowKey: (task) => `${idValue(task.eventId)}-${idValue(task.stageId)}-${idValue(task.eventWorkTypeLevelId)}`, size: "small", pagination: false, columns: boundTaskColumns(), dataSource: editingGroup?.boundTasks ?? [] })) : (_jsx(Typography.Text, { type: "secondary", children: "\u5F53\u524D\u5C0F\u7EC4\u5C1A\u672A\u7ED1\u5B9A\u8D5B\u6BB5\u4EFB\u52A1" })) })] }) })] }));
}

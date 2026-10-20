import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Result, Select, Space, Table, Tag, message, } from 'antd';
import { useMemo } from 'react';
import { bindCompetitionRefereeGroup, getCompetitionRefereeAssignmentWorkspace, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
function idValue(value) {
    return value === undefined || value === null ? '' : String(value);
}
function errorText(error, fallback) {
    return error instanceof Error ? error.message : fallback;
}
export function CompetitionRefereeAssignmentPanel({ stageId }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:referee-assignment:view');
    const canManage = permissions.includes('competition:referee-assignment:manage');
    const queryClient = useQueryClient();
    const workspaceQuery = useQuery({
        queryKey: ['competition-referee-assignment-workspace', stageId],
        queryFn: () => getCompetitionRefereeAssignmentWorkspace(stageId),
        enabled: canView && Boolean(stageId),
    });
    const bindMutation = useMutation({
        mutationFn: ({ contestantMemberId, refereeGroupId }) => bindCompetitionRefereeGroup(stageId, contestantMemberId, refereeGroupId),
        onSuccess: async () => {
            message.success('选手裁判小组绑定已更新');
            await queryClient.invalidateQueries({
                queryKey: ['competition-referee-assignment-workspace', stageId],
            });
        },
        onError: (error) => message.error(errorText(error, '选手裁判小组绑定失败，请刷新后重试')),
    });
    const workspace = workspaceQuery.data;
    const groups = workspace?.groups ?? [];
    const bindableGroups = useMemo(() => groups.filter((group) => group.status === 'confirmed' && group.bindable !== false), [groups]);
    const occupiedGroups = useMemo(() => groups.filter((group) => group.status === 'confirmed' && group.bindable === false), [groups]);
    const groupOptions = (row) => {
        const selectedId = idValue(row.refereeGroupId);
        const options = bindableGroups.map((group) => ({
            label: `${group.groupName}（${group.members.length}名裁判）`,
            value: idValue(group.id),
            disabled: false,
        }));
        const selected = groups.find((group) => idValue(group.id) === selectedId);
        if (selected && !options.some((option) => option.value === selectedId)) {
            options.unshift({
                label: `${selected.groupName}${selected.unavailableReason ? `（${selected.unavailableReason}）` : ''}`,
                value: selectedId,
                disabled: true,
            });
        }
        return options;
    };
    const candidateColumns = useMemo(() => [
        { title: '选手编号', dataIndex: 'contestantNo', width: 120 },
        { title: '选手', dataIndex: 'name', width: 120 },
        { title: '单位', dataIndex: 'orgName', width: 180, ellipsis: true },
        {
            title: '实操试卷',
            width: 230,
            render: (_, row) => row.paperSnapshotId
                ? _jsxs(Space, { size: 4, children: [_jsx(Tag, { color: "success", children: "\u5DF2\u7ED1\u5B9A" }), _jsx("span", { children: row.paperCode || row.paperTitle || '已绑定试卷' })] })
                : _jsx(Tag, { color: "warning", children: "\u672A\u7ED1\u5B9A\u5B9E\u64CD\u8BD5\u5377" }),
        },
        {
            title: '裁判小组',
            width: 280,
            render: (_, row) => (_jsx(Select, { allowClear: true, style: { width: '100%' }, placeholder: row.paperSnapshotId ? '选择已确定的裁判小组' : '请先绑定实操试卷', disabled: !canManage || !row.paperSnapshotId || bindMutation.isPending
                    || (bindableGroups.length === 0 && row.refereeGroupId == null), value: row.refereeGroupId == null ? undefined : idValue(row.refereeGroupId), options: groupOptions(row), onChange: (value) => bindMutation.mutate({
                    contestantMemberId: row.contestantMemberId,
                    refereeGroupId: value || null,
                }) })),
        },
        {
            title: '绑定状态',
            width: 140,
            render: (_, row) => row.refereeGroupId
                ? _jsx(Tag, { color: "success", children: row.refereeGroupName || '已绑定裁判小组' })
                : _jsx(Tag, { color: "warning", children: "\u672A\u7ED1\u5B9A\u88C1\u5224\u5C0F\u7EC4" }),
        },
    ], [bindMutation, bindableGroups, canManage, groups]);
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u8D5B\u6BB5\u88C1\u5224\u914D\u7F6E\u67E5\u770B\u6743\u9650" });
    }
    return (_jsxs(Card, { size: "small", title: "\u88C1\u5224\u914D\u7F6E", extra: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void workspaceQuery.refetch(), children: "\u5237\u65B0" }), children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: `赛段：${workspace?.stageName || idValue(stageId)}${workspace?.levelName ? ` · ${workspace.levelName}` : ''}`, description: canManage
                    ? '请在「裁判管理 / 裁判小组」中创建并确定小组后，在此为选手选择绑定。同一裁判小组不能同时承担不同赛事，但可以参加同一赛事的多个赛段。'
                    : '当前为只读模式；选手只能查看已经绑定的裁判小组。' }), bindableGroups.length === 0 && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u6682\u65E0\u53EF\u7ED1\u5B9A\u7684\u88C1\u5224\u5C0F\u7EC4", description: "\u8BF7\u5148\u5728\u88C1\u5224\u7BA1\u7406\u4E0B\u7684\u300C\u88C1\u5224\u5C0F\u7EC4\u300D\u4E2D\u521B\u5EFA\u5E76\u786E\u5B9A\u5C0F\u7EC4\u3002\u82E5\u5C0F\u7EC4\u5DF2\u5B58\u5728\u4F46\u4ECD\u4E0D\u53EF\u9009\uFF0C\u53EF\u80FD\u662F\u7EC4\u5185\u88C1\u5224\u4E0D\u5177\u5907\u5F53\u524D\u5DE5\u79CD\u7B49\u7EA7\uFF0C\u6216\u5DF2\u627F\u62C5\u5176\u4ED6\u8D5B\u4E8B\u7684\u88C1\u5224\u4EFB\u52A1\u3002" })), occupiedGroups.length > 0 && (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u90E8\u5206\u88C1\u5224\u5C0F\u7EC4\u5F53\u524D\u4E0D\u53EF\u7528", description: occupiedGroups.map((group) => `${group.groupName}：${group.unavailableReason || '不可绑定'}`).join('；') })), _jsx(Card, { size: "small", title: "\u9009\u624B\u7ED1\u5B9A\u88C1\u5224\u5C0F\u7EC4", children: _jsx(Table, { rowKey: (row) => idValue(row.contestantMemberId), size: "small", scroll: { x: 1050 }, loading: workspaceQuery.isLoading || workspaceQuery.isFetching || bindMutation.isPending, dataSource: workspace?.candidates ?? [], columns: candidateColumns, pagination: false, locale: { emptyText: '当前赛段暂无符合条件的选手' } }) })] }));
}

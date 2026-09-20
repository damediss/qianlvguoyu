import { ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Result,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo } from 'react';
import {
  bindCompetitionRefereeGroup,
  getCompetitionRefereeAssignmentWorkspace,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionId,
  CompetitionRefereeAssignmentWorkspace,
  CompetitionRefereeCandidate,
} from '../../types/competition';

function idValue(value?: CompetitionId | null) {
  return value === undefined || value === null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

export function CompetitionRefereeAssignmentPanel({ stageId }: { stageId: CompetitionId }) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:referee-assignment:view');
  const canManage = permissions.includes('competition:referee-assignment:manage');
  const queryClient = useQueryClient();

  const workspaceQuery = useQuery<CompetitionRefereeAssignmentWorkspace>({
    queryKey: ['competition-referee-assignment-workspace', stageId],
    queryFn: () => getCompetitionRefereeAssignmentWorkspace(stageId),
    enabled: canView && Boolean(stageId),
  });

  const bindMutation = useMutation({
    mutationFn: ({ contestantMemberId, refereeGroupId }: {
      contestantMemberId: CompetitionId;
      refereeGroupId?: CompetitionId | null;
    }) => bindCompetitionRefereeGroup(stageId, contestantMemberId, refereeGroupId),
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
  const bindableGroups = useMemo(
    () => groups.filter((group) => group.status === 'confirmed' && group.bindable !== false),
    [groups],
  );
  const occupiedGroups = useMemo(
    () => groups.filter((group) => group.status === 'confirmed' && group.bindable === false),
    [groups],
  );

  const groupOptions = (row: CompetitionRefereeCandidate) => {
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

  const candidateColumns = useMemo<ColumnsType<CompetitionRefereeCandidate>>(() => [
    { title: '选手编号', dataIndex: 'contestantNo', width: 120 },
    { title: '选手', dataIndex: 'name', width: 120 },
    { title: '单位', dataIndex: 'orgName', width: 180, ellipsis: true },
    {
      title: '实操试卷',
      width: 230,
      render: (_, row) => row.paperSnapshotId
        ? <Space size={4}><Tag color="success">已绑定</Tag><span>{row.paperCode || row.paperTitle || '已绑定试卷'}</span></Space>
        : <Tag color="warning">未绑定实操试卷</Tag>,
    },
    {
      title: '裁判小组',
      width: 280,
      render: (_, row) => (
        <Select
          allowClear
          style={{ width: '100%' }}
          placeholder={row.paperSnapshotId ? '选择已确定的裁判小组' : '请先绑定实操试卷'}
          disabled={!canManage || !row.paperSnapshotId || bindMutation.isPending
            || (bindableGroups.length === 0 && row.refereeGroupId == null)}
          value={row.refereeGroupId == null ? undefined : idValue(row.refereeGroupId)}
          options={groupOptions(row)}
          onChange={(value) => bindMutation.mutate({
            contestantMemberId: row.contestantMemberId,
            refereeGroupId: value || null,
          })}
        />
      ),
    },
    {
      title: '绑定状态',
      width: 140,
      render: (_, row) => row.refereeGroupId
        ? <Tag color="success">{row.refereeGroupName || '已绑定裁判小组'}</Tag>
        : <Tag color="warning">未绑定裁判小组</Tag>,
    },
  ], [bindMutation, bindableGroups, canManage, groups]);

  if (!canView) {
    return <Result status="403" title="暂无赛段裁判配置查看权限" />;
  }

  return (
    <Card
      size="small"
      title="裁判配置"
      extra={<Button icon={<ReloadOutlined />} onClick={() => void workspaceQuery.refetch()}>刷新</Button>}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={`赛段：${workspace?.stageName || idValue(stageId)}${workspace?.levelName ? ` · ${workspace.levelName}` : ''}`}
        description={canManage
          ? '请在「裁判管理 / 裁判小组」中创建并确定小组后，在此为选手选择绑定。时间重叠的赛段不能调用同一裁判小组。'
          : '当前为只读模式；选手只能查看已经绑定的裁判小组。'}
      />

      {bindableGroups.length === 0 && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="暂无可绑定的裁判小组"
          description="请先在裁判管理下的「裁判小组」中创建并确定小组。若小组已存在但仍不可选，可能是组内裁判不具备当前工种等级，或已被同时段赛段占用。"
        />
      )}
      {occupiedGroups.length > 0 && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="部分裁判小组当前不可用"
          description={occupiedGroups.map((group) => `${group.groupName}：${group.unavailableReason || '不可绑定'}`).join('；')}
        />
      )}

      <Card size="small" title="选手绑定裁判小组">
        <Table<CompetitionRefereeCandidate>
          rowKey={(row) => idValue(row.contestantMemberId)}
          size="small"
          scroll={{ x: 1050 }}
          loading={workspaceQuery.isLoading || workspaceQuery.isFetching || bindMutation.isPending}
          dataSource={workspace?.candidates ?? []}
          columns={candidateColumns}
          pagination={false}
          locale={{ emptyText: '当前赛段暂无符合条件的选手' }}
        />
      </Card>
    </Card>
  );
}

import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Result,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import {
  confirmCompetitionRefereeGroup,
  createCompetitionRefereeGroup,
  deleteCompetitionRefereeGroup,
  listCompetitionRefereeGroups,
  pageCompetitionReferees,
  releaseCompetitionRefereeGroup,
  updateCompetitionRefereeGroup,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionId,
  CompetitionReferee,
  CompetitionRefereeGroup,
  CompetitionRefereeGroupBoundTask,
  CompetitionRefereeGroupUpsertRequest,
} from '../../types/competition';
import './CompetitionEventManagementPage.css';

function idValue(value?: CompetitionId | null) {
  return value === undefined || value === null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

function groupStatusTag(status: string) {
  if (status === 'confirmed') return <Tag color="success">已确定</Tag>;
  if (status === 'closed') return <Tag color="default">已解散</Tag>;
  return <Tag color="warning">待确定</Tag>;
}

function taskStatusTag(status: string) {
  if (status === 'completed') return <Tag color="success">已完成</Tag>;
  if (status === 'in_progress') return <Tag color="processing">进行中</Tag>;
  return <Tag color="warning">待开赛</Tag>;
}

function boundTaskColumns(): ColumnsType<CompetitionRefereeGroupBoundTask> {
  return [
    { title: '赛事', dataIndex: 'eventName', render: (value: string) => value || '-' },
    { title: '赛段', dataIndex: 'stageName', render: (value: string) => value || '-' },
    { title: '工种', dataIndex: 'workTypeName', width: 140, render: (value: string) => value || '-' },
    { title: '等级', dataIndex: 'levelName', width: 100, render: (value: string) => value || '-' },
    {
      title: '选手',
      width: 110,
      render: (_, task) => `${task.completedCount ?? 0}/${task.contestantCount ?? 0}`,
    },
    {
      title: '任务状态',
      dataIndex: 'status',
      width: 110,
      render: (status: string) => taskStatusTag(status),
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
  const [editingGroup, setEditingGroup] = useState<CompetitionRefereeGroup | null>(null);
  const [groupName, setGroupName] = useState('');
  const [memberProfileIds, setMemberProfileIds] = useState<string[]>([]);
  const [leaderProfileId, setLeaderProfileId] = useState<string>();

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
    mutationFn: (request: CompetitionRefereeGroupUpsertRequest) => editingGroup
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
    mutationFn: (groupId: CompetitionId) => confirmCompetitionRefereeGroup(groupId),
    onSuccess: async () => {
      message.success('裁判小组已确定，可在赛段实操配置中绑定');
      await invalidate();
    },
    onError: (error) => message.error(errorText(error, '裁判小组确定失败，请检查是否配置了3名裁判和1名组长')),
  });

  const releaseMutation = useMutation({
    mutationFn: (groupId: CompetitionId) => releaseCompetitionRefereeGroup(groupId),
    onSuccess: async () => {
      message.success('裁判小组已解散，组内裁判可重新编组');
      await invalidate();
    },
    onError: (error) => message.error(errorText(error, '裁判小组解散失败，请确认全部赛段任务已完成')),
  });

  const deleteMutation = useMutation({
    mutationFn: (groupId: CompetitionId) => deleteCompetitionRefereeGroup(groupId),
    onSuccess: async () => {
      message.success('裁判小组已删除');
      await invalidate();
    },
    onError: (error) => message.error(errorText(error, '裁判小组删除失败，请先解除赛段绑定或解散小组')),
  });

  const groups = groupsQuery.data ?? [];
  const referees = refereesQuery.data?.items ?? [];
  const occupiedProfileIds = useMemo(() => {
    const occupied = new Map<string, string>();
    groups.forEach((group) => {
      if (group.status === 'closed') return;
      if (editingGroup && idValue(group.id) === idValue(editingGroup.id)) return;
      group.members.forEach((member) => {
        const profileId = idValue(member.refereeProfileId);
        if (profileId) occupied.set(profileId, group.groupName);
      });
    });
    return occupied;
  }, [editingGroup, groups]);
  const refereeOptions = useMemo(
    () => referees.map((referee: CompetitionReferee) => {
      const value = idValue(referee.id);
      const occupiedBy = occupiedProfileIds.get(value);
      return {
        label: `${referee.name}${occupiedBy ? `（已在「${occupiedBy}」）` : referee.qualifications?.length
          ? `（${referee.qualifications.map((item) => item.workTypeName || idValue(item.workTypeId)).join('、')}）`
          : ''}`,
        value,
        disabled: Boolean(occupiedBy),
      };
    }),
    [occupiedProfileIds, referees],
  );
  const leaderOptions = useMemo(
    () => refereeOptions.filter((option) => memberProfileIds.includes(String(option.value))),
    [memberProfileIds, refereeOptions],
  );

  const openCreateModal = () => {
    setEditingGroup(null);
    setGroupName('');
    setMemberProfileIds([]);
    setLeaderProfileId(undefined);
    setGroupModalOpen(true);
  };

  const openEditModal = (group: CompetitionRefereeGroup) => {
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

  const columns = useMemo<ColumnsType<CompetitionRefereeGroup>>(() => [
    { title: '组号', dataIndex: 'groupNo', width: 80 },
    { title: '裁判小组', dataIndex: 'groupName', width: 180 },
    {
      title: '组内裁判',
      render: (_, group) => (
        <Space wrap size={[4, 4]}>
          {group.members.map((member) => (
            <Tag key={idValue(member.id)} color={member.memberRole === 'leader' ? 'blue' : undefined}>
              {member.refereeName || '未命名裁判'}{member.memberRole === 'leader' ? '（组长）' : ''}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '已绑定任务',
      width: 220,
      render: (_, group) => {
        const tasks = group.boundTasks ?? [];
        if (!tasks.length) return <Typography.Text type="secondary">未绑定赛段</Typography.Text>;
        return (
          <Space direction="vertical" size={0}>
            {tasks.map((task) => (
              <Typography.Text key={`${idValue(task.eventId)}-${idValue(task.stageId)}`}>
                {task.eventName || '赛事'} / {task.stageName || '赛段'} {taskStatusTag(task.status)}
              </Typography.Text>
            ))}
          </Space>
        );
      },
    },
    { title: '状态', dataIndex: 'status', width: 100, render: (status: string) => groupStatusTag(status) },
    {
      title: '操作',
      width: 280,
      render: (_, group) => (
        <Space wrap>
          {canManage && group.status !== 'closed' && (
            <Button size="small" icon={<EditOutlined />} onClick={() => openEditModal(group)}>编辑</Button>
          )}
          {canManage && group.status === 'draft' && (
            <Popconfirm
              title="确定该裁判小组？"
              description="确定后可在赛段实操配置中绑定。同一小组不能同时承担不同赛事，但可参加同一赛事的多个赛段。"
              onConfirm={() => confirmMutation.mutate(group.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button size="small" type="primary" loading={confirmMutation.isPending}>确定小组</Button>
            </Popconfirm>
          )}
          {canManage && group.releasable && group.status === 'confirmed' && (
            <Popconfirm
              title="确认解散该裁判小组？"
              description="解散后组内裁判可重新编入其他小组。历史绑定任务仍可查看。"
              onConfirm={() => releaseMutation.mutate(group.id)}
              okText="解散"
              cancelText="取消"
            >
              <Button size="small" loading={releaseMutation.isPending}>解散小组</Button>
            </Popconfirm>
          )}
          {canDelete && (
            <Popconfirm
              title="确认删除该裁判小组？"
              description="已绑定未完成赛段的小组不能删除；任务全部完成后请先解散。"
              onConfirm={() => deleteMutation.mutate(group.id)}
              okText="删除"
              cancelText="取消"
            >
              <Button size="small" danger icon={<DeleteOutlined />} loading={deleteMutation.isPending}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ], [canDelete, canManage, confirmMutation, deleteMutation, releaseMutation]);

  if (!canView) {
    return <Result status="403" title="暂无裁判小组查看权限" subTitle="请联系管理员申请竞赛裁判管理权限。" />;
  }

  return (
    <div data-testid="competition-referee-group-page">
      <Card bordered={false} className="competition-filter-card">
        <div className="competition-filter-card__head">
          <div>
            <Typography.Title level={4}>裁判小组</Typography.Title>
            <Typography.Text type="secondary">
              一名裁判只能进入一个小组。小组同一时间只能承担一场赛事，但可参加该赛事的多个赛段；全部赛段任务完成后可在此解散小组。
            </Typography.Text>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => void groupsQuery.refetch()}>刷新</Button>
            {canManage && <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>新建裁判小组</Button>}
          </Space>
        </div>
      </Card>

      <Card size="small" style={{ marginTop: 16 }}>
        <Table<CompetitionRefereeGroup>
          rowKey={(row) => idValue(row.id)}
          loading={groupsQuery.isLoading || groupsQuery.isFetching || groupMutation.isPending}
          dataSource={groups}
          columns={columns}
          pagination={false}
          locale={{ emptyText: '尚未创建裁判小组' }}
          expandable={{
            rowExpandable: (group) => (group.boundTasks?.length ?? 0) > 0,
            expandedRowRender: (group) => (
              <Table<CompetitionRefereeGroupBoundTask>
                rowKey={(task) => `${idValue(task.eventId)}-${idValue(task.stageId)}-${idValue(task.eventWorkTypeLevelId)}`}
                size="small"
                pagination={false}
                columns={boundTaskColumns()}
                dataSource={group.boundTasks ?? []}
              />
            ),
          }}
        />
      </Card>

      <Modal
        title={editingGroup ? '编辑裁判小组' : '新建裁判小组'}
        open={groupModalOpen}
        onCancel={() => setGroupModalOpen(false)}
        onOk={submitGroup}
        okText="保存裁判小组"
        cancelText="取消"
        confirmLoading={groupMutation.isPending}
        width={760}
        destroyOnClose
      >
        <Form layout="vertical">
          <Form.Item label="裁判小组名称" required>
            <Input
              maxLength={128}
              placeholder="例如：第一裁判小组"
              value={groupName}
              onChange={(event) => setGroupName(event.target.value)}
            />
          </Form.Item>
          <Form.Item label="组内裁判（固定3名）" required>
            <Select
              mode="multiple"
              style={{ width: '100%' }}
              placeholder="从裁判信息中选择3名未编组裁判"
              value={memberProfileIds}
              options={refereeOptions}
              loading={refereesQuery.isLoading}
              onChange={(values: string[]) => {
                setMemberProfileIds(values);
                if (leaderProfileId && !values.includes(leaderProfileId)) setLeaderProfileId(undefined);
              }}
            />
          </Form.Item>
          <Form.Item label="组长" required>
            <Select
              style={{ width: '100%' }}
              placeholder="从组内裁判中选择一名组长"
              value={leaderProfileId}
              options={leaderOptions}
              onChange={setLeaderProfileId}
              disabled={memberProfileIds.length === 0}
            />
          </Form.Item>
          <Form.Item label="已绑定任务">
            {(editingGroup?.boundTasks?.length ?? 0) > 0 ? (
              <Table<CompetitionRefereeGroupBoundTask>
                rowKey={(task) => `${idValue(task.eventId)}-${idValue(task.stageId)}-${idValue(task.eventWorkTypeLevelId)}`}
                size="small"
                pagination={false}
                columns={boundTaskColumns()}
                dataSource={editingGroup?.boundTasks ?? []}
              />
            ) : (
              <Typography.Text type="secondary">当前小组尚未绑定赛段任务</Typography.Text>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

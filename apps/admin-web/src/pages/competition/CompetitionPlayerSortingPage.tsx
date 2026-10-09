import {
  DeleteOutlined,
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Card,
  Alert,
  Button,
  Input,
  Modal,
  Pagination,
  Result,
  Select,
  Space,
  Spin,
  Steps,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
  pageCompetitionStages,
} from '../../api/competition';
import {
  createCompetitionPlayerSortingTask,
  deleteCompetitionPlayerSortingTask,
  downloadCompetitionPlayerSortingRoster,
  drawCompetitionPlayerSortingWorkstations,
  getCompetitionPlayerSortingTask,
  listCompetitionPlayerSortingCandidates,
  pageCompetitionPlayerSortingTasks,
  randomizeCompetitionPlayerSortingTask,
  saveCompetitionPlayerSortingGroups,
  saveCompetitionPlayerSortingRoles,
  saveCompetitionPlayerSortingWorkstations,
} from '../../api/competitionPlayerSorting';
import type { CompetitionEvent, CompetitionId, CompetitionStage } from '../../types/competition';
import type {
  CompetitionPlayerSortingAssignment,
  CompetitionPlayerSortingCandidate,
  CompetitionPlayerSortingMember,
  CompetitionPlayerSortingTask,
} from '../../types/competitionPlayerSorting';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';

interface ManualGroup {
  groupName: string;
  orgName: string;
  memberIds: string[];
}

interface CompetitionPlayerSortingPageProps {
  stageId: string;
  stageName?: string;
  showTitle?: boolean;
}

const MISSING_ORG = '未填写参赛公司';

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  const responseMessage = (error as {
    response?: { data?: { message?: unknown } };
  })?.response?.data?.message;
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage;
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function companyName(value?: string | null) {
  const name = value?.trim();
  return name || MISSING_ORG;
}

function splitCodes(value: string) {
  return value.split(/[\s,，、;；]+/).map((item) => item.trim()).filter(Boolean);
}

function taskStatus(task: CompetitionPlayerSortingTask) {
  if (task.groupingMode === 'company' && task.status === 'created') {
    return task.groupCount > 0
      ? <Tag color="processing">待完成排序</Tag>
      : <Tag color="warning">待成组</Tag>;
  }
  if (task.status === 'ordered') return <Tag color="processing">待工位抽签</Tag>;
  if (task.status === 'drawn') return <Tag color="success">已完成工位抽签</Tag>;
  if (task.status === 'sorted') return <Tag color="success">已随机排序</Tag>;
  if (task.status === 'created' && task.playerCount === 0) return <Tag color="warning">待选择选手</Tag>;
  return <Tag color="processing">待随机排序</Tag>;
}

function groupAssignments(assignments: CompetitionPlayerSortingAssignment[]) {
  const groups: Array<{
    groupNo: number;
    groupName: string;
    orgName: string;
    workstation?: string | null;
    members: CompetitionPlayerSortingAssignment[];
  }> = [];
  for (const assignment of assignments) {
    const current = groups.find((group) => group.groupNo === assignment.groupNo);
    if (current) {
      current.members.push(assignment);
      if (!current.workstation && assignment.workstation) current.workstation = assignment.workstation;
      continue;
    }
    groups.push({
      groupNo: assignment.groupNo,
      groupName: assignment.groupName || `第${assignment.groupNo}组`,
      orgName: companyName(assignment.orgName),
      workstation: assignment.workstation,
      members: [assignment],
    });
  }
  for (const group of groups) {
    group.members.sort((left, right) => left.groupOrder - right.groupOrder);
  }
  return groups.sort((left, right) => left.groupNo - right.groupNo);
}

function leaderTag(groupLeader?: boolean | null) {
  if (groupLeader == null) return '-';
  return groupLeader ? <Tag color="gold">组长</Tag> : <Tag>组员</Tag>;
}

export function CompetitionPlayerSortingPage({
  stageId,
  stageName,
  showTitle = true,
}: CompetitionPlayerSortingPageProps) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-sorting:view');
  const canCreate = permissions.includes('competition:player-sorting:create');
  const canRandomize = permissions.includes('competition:player-sorting:randomize');
  const canExport = permissions.includes('competition:player-sorting:export');
  const canDelete = permissions.includes('competition:player-sorting:delete');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [taskPage, setTaskPage] = useState(1);
  const [wizardOpen, setWizardOpen] = useState(false);
  const [wizardStep, setWizardStep] = useState(0);
  const [createdTaskId, setCreatedTaskId] = useState<string>();
  const [roleOptions, setRoleOptions] = useState<string[]>(['组员']);
  const [roleById, setRoleById] = useState<Record<string, string>>({});
  const [members, setMembers] = useState<CompetitionPlayerSortingMember[]>([]);
  const [roleDraft, setRoleDraft] = useState('');
  const [manualGroups, setManualGroups] = useState<ManualGroup[]>([]);
  const [poolSelection, setPoolSelection] = useState<string[]>([]);
  const [groupNameDraft, setGroupNameDraft] = useState('');
  const [workstationLabel, setWorkstationLabel] = useState('');
  const [workstationCodes, setWorkstationCodes] = useState<string[]>([]);
  const [codeDraft, setCodeDraft] = useState('');
  const [previewTask, setPreviewTask] = useState<CompetitionPlayerSortingTask>();
  const [groupPage, setGroupPage] = useState(1);
  const [resultGroupPage, setResultGroupPage] = useState(1);
  const [activeTaskId, setActiveTaskId] = useState<string>();
  const [resultOpen, setResultOpen] = useState(false);
  const queryClient = useQueryClient();

  const candidatesQuery = useQuery({
    queryKey: ['competition-player-sorting-candidates', stageId],
    queryFn: () => listCompetitionPlayerSortingCandidates(stageId!),
    enabled: canView && Boolean(stageId) && wizardOpen,
  });
  const candidates = candidatesQuery.data || [];
  const tasksQuery = useQuery({
    queryKey: ['competition-player-sorting-tasks', stageId, taskPage],
    queryFn: () => pageCompetitionPlayerSortingTasks(stageId!, { page: taskPage, pageSize: 10 }),
    enabled: canView && Boolean(stageId),
  });
  const taskDetailQuery = useQuery({
    queryKey: ['competition-player-sorting-task', activeTaskId],
    queryFn: () => getCompetitionPlayerSortingTask(activeTaskId!),
    enabled: Boolean(activeTaskId && resultOpen),
  });

  const applyDetail = (task: CompetitionPlayerSortingTask) => {
    const options = task.roleOptions?.filter(Boolean).length ? task.roleOptions!.filter(Boolean) : ['组员'];
    const nextRoles: Record<string, string> = {};
    for (const member of task.members || []) {
      nextRoles[idValue(member.contestantProfileId)] = member.role || options[0];
    }
    setCreatedTaskId(idValue(task.id));
    setRoleOptions(options);
    setRoleById(nextRoles);
    setMembers(task.members || []);
    const formed = new Map<number, ManualGroup>();
    for (const member of task.members || []) {
      if (member.groupNo == null || !member.groupName) continue;
      const current = formed.get(member.groupNo) || {
        groupName: member.groupName,
        orgName: companyName(member.orgName),
        memberIds: [],
      };
      current.memberIds.push(idValue(member.contestantProfileId));
      formed.set(member.groupNo, current);
    }
    setManualGroups([...formed.entries()].sort((left, right) => left[0] - right[0]).map((entry) => entry[1]));
    setPoolSelection([]);
    setGroupNameDraft('');
    setWorkstationLabel(task.workstationLabel || '');
    setWorkstationCodes([...(task.workstationCodes || [])]);
    setPreviewTask(task);
  };

  const invalidateTasks = () => {
    void queryClient.invalidateQueries({ queryKey: ['competition-player-sorting-tasks', stageId] });
  };

  const createMutation = useMutation({
    mutationFn: (contestantProfileIds: string[]) => {
      if (!stageId) throw new Error('请先在选手管理中选择赛事工种、等级和赛段');
      return createCompetitionPlayerSortingTask(stageId, { contestantProfileIds });
    },
    onSuccess: (task) => {
      applyDetail(task);
      setWizardStep(1);
      message.success('选手已保存，请按公司选择成员并成组');
      invalidateTasks();
    },
    onError: (error) => message.error(errorText(error, '创建排序任务失败')),
  });

  const groupsMutation = useMutation({
    mutationFn: (groups: ManualGroup[]) => {
      if (!createdTaskId) throw new Error('请先保存参赛选手');
      return saveCompetitionPlayerSortingGroups(createdTaskId, {
        groups: groups.map((group) => ({
          groupName: group.groupName,
          contestantProfileIds: group.memberIds,
        })),
      });
    },
    onSuccess: (task) => {
      applyDetail(task);
      setWizardStep(2);
      message.success('分组已保存，请选择组内角色');
      invalidateTasks();
    },
    onError: (error) => message.error(errorText(error, '保存分组失败')),
  });

  const rolesMutation = useMutation({
    mutationFn: () => {
      if (!createdTaskId) throw new Error('请先创建排序任务');
      return saveCompetitionPlayerSortingRoles(createdTaskId, {
        roleOptions,
        members: members.map((member) => ({
          contestantProfileId: member.contestantProfileId,
          role: roleById[idValue(member.contestantProfileId)] || roleOptions[0],
        })),
      });
    },
    onSuccess: (task) => {
      applyDetail(task);
      setWizardStep(3);
      message.success('组内角色已保存，可以随机排出场顺序并选取组长');
      invalidateTasks();
    },
    onError: (error) => message.error(errorText(error, '保存组内角色失败')),
  });

  const randomizeMutation = useMutation({
    mutationFn: randomizeCompetitionPlayerSortingTask,
    onSuccess: (task) => {
      applyDetail(task);
      setGroupPage(1);
      setWizardStep(3);
      message.success('已随机排出场顺序，并在每组指定一名组长');
      queryClient.setQueryData(['competition-player-sorting-task', idValue(task.id)], task);
      invalidateTasks();
    },
    onError: (error) => message.error(errorText(error, '随机排序失败')),
  });

  const workstationsMutation = useMutation({
    mutationFn: async () => {
      if (!createdTaskId) throw new Error('请先完成随机排序');
      await saveCompetitionPlayerSortingWorkstations(createdTaskId, {
        label: workstationLabel.trim(),
        codes: workstationCodes,
      });
      return drawCompetitionPlayerSortingWorkstations(createdTaskId);
    },
    onSuccess: (task) => {
      const taskId = idValue(task.id);
      message.success('工位已随机分配到各组选手');
      queryClient.setQueryData(['competition-player-sorting-task', taskId], task);
      invalidateTasks();
      setWizardOpen(false);
      setActiveTaskId(taskId);
      setResultOpen(true);
    },
    onError: (error) => message.error(errorText(error, '工位抽签失败')),
  });

  const downloadMutation = useMutation({
    mutationFn: downloadCompetitionPlayerSortingRoster,
    onSuccess: (blob) => {
      downloadBlob(blob, `${stageName || '赛段'}-实操出场名单.xlsx`);
      message.success('实操出场名单已下载');
    },
    onError: (error) => message.error(errorText(error, '下载名单失败')),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCompetitionPlayerSortingTask,
    onSuccess: (_result, taskId) => {
      message.success('排序任务已删除');
      if (idValue(taskId) === activeTaskId) {
        setResultOpen(false);
        setActiveTaskId(undefined);
      }
      invalidateTasks();
    },
    onError: (error) => message.error(errorText(error, '删除排序任务失败')),
  });

  const candidateColumns = useMemo(() => [
    { title: '选手姓名', dataIndex: 'name', width: 140, render: (value: string) => value || '-' },
    { title: '参赛公司', dataIndex: 'orgName', width: 220, render: (value?: string) => companyName(value) },
    { title: '赛事工种', dataIndex: 'workTypeName', width: 180, render: (value: string) => value || '-' },
    {
      title: '技能等级',
      dataIndex: 'levelName',
      width: 150,
      render: (value: string, record: CompetitionPlayerSortingCandidate) => value || record.levelCode || record.rosterLevel || '-',
    },
  ], []);

  const assignmentColumns = useMemo(() => [
    {
      title: '组名',
      dataIndex: 'groupName',
      width: 160,
      render: (value: string | null | undefined, record: CompetitionPlayerSortingAssignment) => value || `第${record.groupNo}组`,
    },
    {
      title: '参赛公司',
      dataIndex: 'orgName',
      width: 220,
      render: (value?: string | null) => companyName(value),
    },
    { title: '出场顺序', dataIndex: 'groupOrder', width: 100 },
    { title: '组内角色', dataIndex: 'role', width: 110, render: (value?: string | null) => value || '-' },
    {
      title: '组长',
      dataIndex: 'groupLeader',
      width: 90,
      render: (value?: boolean | null) => leaderTag(value),
    },
    {
      title: taskDetailQuery.data?.workstationLabel || previewTask?.workstationLabel || '工位',
      dataIndex: 'workstation',
      width: 140,
      render: (value?: string | null) => value || '-',
    },
    { title: '手机号', dataIndex: 'mobile', width: 150, render: (value?: string | null) => value || '-' },
    { title: '考生姓名', dataIndex: 'name', width: 120, render: (value: string) => value || '-' },
  ], [previewTask?.workstationLabel, taskDetailQuery.data?.workstationLabel]);

  const selectedCandidates = useMemo(() => {
    const candidateById = new Map(candidates.map((candidate) => [idValue(candidate.contestantProfileId), candidate]));
    return selectedIds
      .map((id) => candidateById.get(id))
      .filter((candidate): candidate is CompetitionPlayerSortingCandidate => Boolean(candidate));
  }, [candidates, selectedIds]);

  const companyGroups = useMemo(() => {
    const groups: Array<{ orgName: string; players: CompetitionPlayerSortingCandidate[] }> = [];
    for (const candidate of selectedCandidates) {
      const orgName = companyName(candidate.orgName);
      const current = groups.find((group) => group.orgName === orgName);
      if (current) current.players.push(candidate);
      else groups.push({ orgName, players: [candidate] });
    }
    return groups;
  }, [selectedCandidates]);

  const roleGroups = useMemo(() => {
    const groups: Array<{ groupNo: number; groupName: string; orgName: string; members: CompetitionPlayerSortingMember[] }> = [];
    for (const member of members) {
      if (member.groupNo == null) continue;
      const current = groups.find((group) => group.groupNo === member.groupNo);
      if (current) current.members.push(member);
      else groups.push({
        groupNo: member.groupNo,
        groupName: member.groupName || `第${member.groupNo}组`,
        orgName: companyName(member.orgName),
        members: [member],
      });
    }
    return groups.sort((left, right) => left.groupNo - right.groupNo);
  }, [members]);

  const groupedMemberIds = useMemo(
    () => new Set(manualGroups.flatMap((group) => group.memberIds)),
    [manualGroups],
  );
  const poolByCompany = useMemo(() => {
    const groups: Array<{ orgName: string; members: CompetitionPlayerSortingMember[] }> = [];
    for (const member of members) {
      if (groupedMemberIds.has(idValue(member.contestantProfileId))) continue;
      const orgName = companyName(member.orgName);
      const current = groups.find((group) => group.orgName === orgName);
      if (current) current.members.push(member);
      else groups.push({ orgName, members: [member] });
    }
    return groups;
  }, [groupedMemberIds, members]);

  const candidateById = useMemo(
    () => new Map(candidates.map((candidate) => [idValue(candidate.contestantProfileId), candidate])),
    [candidates],
  );
  const openWizard = () => {
    setCreatedTaskId(undefined);
    setPreviewTask(undefined);
    setMembers([]);
    setRoleOptions(['组员']);
    setRoleById({});
    setRoleDraft('');
    setManualGroups([]);
    setPoolSelection([]);
    setGroupNameDraft('');
    setWorkstationLabel('');
    setWorkstationCodes([]);
    setCodeDraft('');
    setSelectedIds([]);
    setWizardStep(0);
    setWizardOpen(true);
  };

  const resumeTask = async (task: CompetitionPlayerSortingTask) => {
    try {
      const detail = await queryClient.fetchQuery({
        queryKey: ['competition-player-sorting-task', idValue(task.id)],
        queryFn: () => getCompetitionPlayerSortingTask(task.id),
      });
      applyDetail(detail);
      const ungrouped = (detail.members || []).some((member) => member.groupNo == null || !member.groupName);
      const missingRole = (detail.members || []).some((member) => !member.role);
      if (detail.status === 'drawn') setWizardStep(4);
      else if (detail.status === 'ordered') {
        setGroupPage(1);
        setWizardStep(3);
      }
      else if (ungrouped) setWizardStep(1);
      else setWizardStep(missingRole ? 2 : 3);
      setWizardOpen(true);
    } catch (error) {
      message.error(errorText(error, '排序任务加载失败'));
    }
  };

  const formGroup = () => {
    const selected = poolSelection.filter((id) => !groupedMemberIds.has(id));
    if (!selected.length) {
      message.warning('请先勾选要成组的选手');
      return;
    }
    const orgs = new Set(selected.map((id) => companyName(members.find((member) => idValue(member.contestantProfileId) === id)?.orgName)));
    if (orgs.size !== 1) {
      message.warning('同一组只能选择同一参赛公司的选手');
      return;
    }
    const groupName = groupNameDraft.trim();
    if (!groupName) {
      message.warning('请填写组名');
      return;
    }
    if (manualGroups.some((group) => group.groupName === groupName)) {
      message.warning('组名已存在');
      return;
    }
    setManualGroups((current) => [...current, { groupName, orgName: [...orgs][0], memberIds: selected }]);
    setPoolSelection([]);
    setGroupNameDraft('');
  };

  const addRole = () => {
    const role = roleDraft.trim();
    if (!role) return;
    if (roleOptions.includes(role)) {
      message.warning('该角色已存在');
      return;
    }
    if (roleOptions.length >= 20) {
      message.warning('组内角色最多 20 个');
      return;
    }
    setRoleOptions((current) => [...current, role]);
    setRoleDraft('');
  };

  const removeRole = (role: string) => {
    if (roleOptions.length <= 1) return;
    const nextOptions = roleOptions.filter((item) => item !== role);
    setRoleOptions(nextOptions);
    setRoleById((current) => {
      const next = { ...current };
      for (const [profileId, value] of Object.entries(next)) {
        if (value === role) next[profileId] = nextOptions[0];
      }
      return next;
    });
  };

  const addCodes = (value: string) => {
    const incoming = splitCodes(value);
    if (!incoming.length) return;
    setWorkstationCodes((current) => {
      const next = [...current];
      let duplicated = 0;
      for (const code of incoming) {
        if (next.includes(code)) duplicated += 1;
        else if (next.length < 500) next.push(code);
      }
      if (duplicated) message.warning('重复的工位编号已跳过');
      return next;
    });
    setCodeDraft('');
  };

  const taskColumns = [
    {
      title: '分组',
      key: 'groups',
      width: 140,
      render: (_: unknown, record: CompetitionPlayerSortingTask) => (
        record.groupingMode === 'company'
          ? (record.groupCount > 0 ? `已成 ${record.groupCount} 组` : '待成组')
          : `${record.groupCount} 组`
      ),
    },
    { title: '选手数', dataIndex: 'playerCount', width: 80 },
    {
      title: '工位',
      key: 'stations',
      width: 160,
      render: (_: unknown, record: CompetitionPlayerSortingTask) => (
        record.groupingMode === 'company'
          ? (record.workstationLabel
            ? `${record.workstationLabel} ${record.workstationCodes?.length || record.workstationCount || 0} 个`
            : '待创建')
          : `${record.workstationCount} 个`
      ),
    },
    {
      title: '任务状态',
      dataIndex: 'status',
      width: 140,
      render: (_: string, record: CompetitionPlayerSortingTask) => taskStatus(record),
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 170, render: (value: string) => value ? value.replace('T', ' ').slice(0, 16) : '-' },
    {
      title: '操作',
      key: 'actions',
      width: 420,
      render: (_: unknown, record: CompetitionPlayerSortingTask) => (
        <Space wrap>
          {record.groupingMode === 'company' && record.status === 'created' && canCreate && (
            <Button type="link" onClick={() => void resumeTask(record)}>继续</Button>
          )}
          {record.groupingMode === 'company' && (record.status === 'ordered' || record.status === 'drawn') && canCreate && (
            <Button type="link" onClick={() => void resumeTask(record)}>
              {record.status === 'drawn' ? '调整工位' : '工位抽签'}
            </Button>
          )}
          {(record.status === 'sorted' || record.status === 'drawn' || record.status === 'ordered') && canView && (
            <Button type="link" onClick={() => { setResultGroupPage(1); setActiveTaskId(idValue(record.id)); setResultOpen(true); }}>
              查看名单
            </Button>
          )}
          {(record.status === 'sorted' || record.status === 'drawn' || record.status === 'ordered') && canExport && (
            <Button
              type="link"
              icon={<DownloadOutlined />}
              loading={downloadMutation.isPending && downloadMutation.variables === record.id}
              onClick={() => downloadMutation.mutate(record.id)}
            >导出 Excel</Button>
          )}
          {canDelete && (
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              loading={deleteMutation.isPending && deleteMutation.variables === idValue(record.id)}
              onClick={() => Modal.confirm({
                title: '删除排序任务',
                content: record.status === 'created'
                  ? '删除后，该未完成的排序任务将从任务列表中移除。确认删除吗？'
                  : '删除后，该任务的分组、出场顺序和工位抽签结果将一并移除。确认删除吗？',
                okText: '删除',
                okButtonProps: { danger: true },
                cancelText: '取消',
                onOk: () => deleteMutation.mutateAsync(idValue(record.id)),
              })}
            >删除</Button>
          )}
        </Space>
      ),
    },
  ];

  const orderedAssignments = previewTask?.assignments || [];
  const assignmentGroups = useMemo(
    () => groupAssignments(previewTask?.assignments || []),
    [previewTask?.assignments],
  );
  const resultGroups = useMemo(
    () => groupAssignments(taskDetailQuery.data?.assignments || []),
    [taskDetailQuery.data?.assignments],
  );
  const assignmentGroupPage = Math.min(groupPage, Math.max(assignmentGroups.length, 1));
  const currentAssignmentGroup = assignmentGroups[assignmentGroupPage - 1];
  const resultGroupIndex = Math.min(resultGroupPage, Math.max(resultGroups.length, 1));
  const currentResultGroup = resultGroups[resultGroupIndex - 1];

  if (!canView) return <Result status="403" title="暂无排序分组查看权限" />;
  return (
    <div className="competition-page" style={{ padding: 16 }}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
          <div>
            {showTitle && <Typography.Title level={4} style={{ margin: 0 }}><TeamOutlined /> 排序分组</Typography.Title>}
            <Typography.Text type="secondary">赛段：{stageName || stageId} · 先按公司选人成组，再抽签排工位</Typography.Text>
          </div>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                void tasksQuery.refetch();
                void queryClient.invalidateQueries({ queryKey: ['competition-player-sorting-candidates', stageId] });
              }}
            >刷新</Button>
            {canCreate && <Button type="primary" onClick={openWizard}>创建排序任务</Button>}
          </Space>
        </div>

        {tasksQuery.isError && (
          <Alert type="error" showIcon message={errorText(tasksQuery.error, '排序任务加载失败')} />
        )}
        <Table<CompetitionPlayerSortingTask>
          rowKey={(record) => idValue(record.id)}
          loading={tasksQuery.isFetching}
          dataSource={tasksQuery.data?.items || []}
          columns={taskColumns}
          pagination={{
            current: taskPage,
            pageSize: 10,
            total: tasksQuery.data?.total || 0,
            showSizeChanger: false,
            onChange: (page) => setTaskPage(page),
          }}
          locale={{ emptyText: '当前赛段还没有排序任务' }}
          scroll={{ x: 1100 }}
        />
      </Space>

      <Modal
        title={`排序任务 · ${['选择选手', '手动成组', '选择组内角色', '随机出场与组长', '工位抽签'][wizardStep]}`}
        open={wizardOpen}
        onCancel={() => setWizardOpen(false)}
        width={980}
        centered
        destroyOnClose
        styles={{ body: { maxHeight: 'calc(100vh - 220px)', overflowY: 'auto' } }}
        footer={(
          <Space>
            {wizardStep < 3 && <Button onClick={() => setWizardOpen(false)}>取消</Button>}
            {wizardStep === 0 && (
              <Button
                type="primary"
                loading={createMutation.isPending}
                disabled={!selectedIds.length}
                onClick={() => createMutation.mutate(selectedIds)}
              >保存选手并进入成组</Button>
            )}
            {wizardStep === 1 && (
              <Button
                type="primary"
                loading={groupsMutation.isPending}
                disabled={!canCreate || groupedMemberIds.size !== members.length || !members.length || manualGroups.some((group) => !group.groupName.trim())}
                onClick={() => groupsMutation.mutate(manualGroups)}
              >保存分组</Button>
            )}
            {wizardStep === 2 && (
              <Button
                type="primary"
                loading={rolesMutation.isPending}
                disabled={!canCreate || !members.length || !roleOptions.length}
                onClick={() => rolesMutation.mutate()}
              >保存角色</Button>
            )}
            {wizardStep === 3 && (
              <>
                <Button onClick={() => setWizardOpen(false)}>稍后处理</Button>
                {canRandomize && (
                  <Button
                    type="primary"
                    loading={randomizeMutation.isPending}
                    disabled={!createdTaskId}
                    onClick={() => createdTaskId && randomizeMutation.mutate(createdTaskId)}
                  >随机排序并选取组长</Button>
                )}
                {orderedAssignments.length > 0 && (
                  <Button onClick={() => setWizardStep(4)}>进入工位抽签</Button>
                )}
              </>
            )}
            {wizardStep === 4 && (
              <>
                <Button onClick={() => setWizardStep(3)}>返回出场顺序</Button>
                <Button
                  type="primary"
                  loading={workstationsMutation.isPending}
                  disabled={!workstationLabel.trim() || assignmentGroups.length < 1 || workstationCodes.length < assignmentGroups.length}
                  onClick={() => workstationsMutation.mutate()}
                >执行工位抽签</Button>
              </>
            )}
          </Space>
        )}
      >
        <Steps
          current={wizardStep}
          items={[
            { title: '选择选手' },
            { title: '手动成组' },
            { title: '组内角色' },
            { title: '出场与组长' },
            { title: '工位抽签' },
          ]}
          style={{ marginBottom: 24 }}
        />

        {wizardStep === 0 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            {candidatesQuery.isError && (
              <Alert type="error" showIcon message={errorText(candidatesQuery.error, '当前赛段选手加载失败')} />
            )}
            <Alert
              type="info"
              showIcon
              message={`已选 ${selectedIds.length} 人，涉及 ${companyGroups.length || 0} 家参赛公司。`}
              description="先确定参加排序的选手。下一步会按公司列出成员，由你勾选成员、填写组名后再成组。同一公司可以分成多组。"
            />
            <Space wrap>
              <Button
                disabled={!candidates.length || candidatesQuery.isFetching}
                onClick={() => setSelectedIds(candidates.map((item) => idValue(item.contestantProfileId)))}
              >全选当前赛段选手</Button>
              <Button disabled={!selectedIds.length} onClick={() => setSelectedIds([])}>清空选择</Button>
            </Space>
            <Table<CompetitionPlayerSortingCandidate>
              rowKey={(record) => idValue(record.contestantProfileId)}
              loading={candidatesQuery.isFetching}
              dataSource={candidates}
              columns={candidateColumns}
              rowSelection={{
                selectedRowKeys: selectedIds,
                preserveSelectedRowKeys: true,
                onChange: (keys) => setSelectedIds(keys.map(String)),
              }}
              pagination={{ pageSize: 8, showSizeChanger: true, pageSizeOptions: [8, 20, 50] }}
              scroll={{ x: 900, y: 360 }}
              locale={{ emptyText: '当前赛段暂无可排序选手' }}
            />
          </Space>
        )}

        {wizardStep === 1 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type={groupedMemberIds.size === members.length && members.length > 0 ? 'success' : 'info'}
              showIcon
              message={`已成组 ${groupedMemberIds.size} / ${members.length} 人，共 ${manualGroups.length} 组。`}
              description="在同一家公司中勾选成员，填写组名后成组。同一公司可以分成多组，例如 8 人分成“银海一组”和“银海二组”。全部选手成组后才能保存。"
            />
            <Space wrap>
              <Input
                value={groupNameDraft}
                maxLength={32}
                placeholder="组名，如银海一组"
                style={{ width: 240 }}
                onChange={(event) => setGroupNameDraft(event.target.value)}
                onPressEnter={formGroup}
              />
              <Button type="primary" icon={<PlusOutlined />} onClick={formGroup}>成组</Button>
            </Space>
            {poolByCompany.map((company) => (
              <Card key={company.orgName} size="small" title={`${company.orgName} · 未成组 ${company.members.length} 人`}>
                <Table<CompetitionPlayerSortingMember>
                  rowKey={(record) => idValue(record.contestantProfileId)}
                  size="small"
                  pagination={false}
                  dataSource={company.members}
                  rowSelection={{
                    selectedRowKeys: poolSelection.filter((id) => company.members.some((member) => idValue(member.contestantProfileId) === id)),
                    onChange: (keys) => setPoolSelection(keys.map(String)),
                  }}
                  columns={[{
                    title: '考生姓名',
                    key: 'name',
                    render: (_, record) => candidateById.get(idValue(record.contestantProfileId))?.name || '-',
                  }]}
                />
              </Card>
            ))}
            {manualGroups.map((group, index) => (
              <Card
                key={`${group.orgName}-${index}`}
                size="small"
                title={`${group.groupName || '未命名'} · ${group.orgName} · ${group.memberIds.length}人`}
                extra={<Button type="link" danger onClick={() => {
                  setManualGroups((current) => current.filter((_, groupIndex) => groupIndex !== index));
                }}
                >解散</Button>}
              >
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  <Input
                    value={group.groupName}
                    maxLength={32}
                    onChange={(event) => {
                      const groupName = event.target.value;
                      setManualGroups((current) => current.map((item, groupIndex) => (
                        groupIndex === index ? { ...item, groupName } : item
                      )));
                    }}
                  />
                  <Typography.Text>
                    {group.memberIds.map((id) => candidateById.get(id)?.name || id).join('、')}
                  </Typography.Text>
                </Space>
              </Card>
            ))}
          </Space>
        )}

        {wizardStep === 2 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              message="为每名选手选择组内角色。组长不在这里指定，下一步会在每组随机选取。"
            />
            <Space wrap>
              {roleOptions.map((role) => (
                <Tag key={role} closable={roleOptions.length > 1} onClose={(event) => { event.preventDefault(); removeRole(role); }}>
                  {role}
                </Tag>
              ))}
              <Input
                value={roleDraft}
                maxLength={16}
                placeholder="添加角色，如主操"
                style={{ width: 180 }}
                onChange={(event) => setRoleDraft(event.target.value)}
                onPressEnter={addRole}
              />
              <Button icon={<PlusOutlined />} onClick={addRole}>添加角色</Button>
            </Space>
            {roleGroups.map((group) => (
              <Card key={group.groupNo} size="small" title={`${group.groupName} · ${group.orgName} · ${group.members.length}人`}>
                <Table<CompetitionPlayerSortingMember>
                  rowKey={(record) => idValue(record.contestantProfileId)}
                  size="small"
                  pagination={false}
                  dataSource={group.members}
                  columns={[
                    {
                      title: '考生姓名',
                      key: 'name',
                      render: (_, record) => candidateById.get(idValue(record.contestantProfileId))?.name || '-',
                    },
                    {
                      title: '组内角色',
                      key: 'role',
                      width: 220,
                      render: (_, record) => (
                        <Select
                          value={roleById[idValue(record.contestantProfileId)] || roleOptions[0]}
                          options={roleOptions.map((role) => ({ value: role, label: role }))}
                          style={{ width: 180 }}
                          onChange={(value) => setRoleById((current) => ({
                            ...current,
                            [idValue(record.contestantProfileId)]: value,
                          }))}
                        />
                      ),
                    },
                  ]}
                />
              </Card>
            ))}
          </Space>
        )}

        {wizardStep === 3 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type={orderedAssignments.length ? 'success' : 'info'}
              showIcon
              message={orderedAssignments.length ? '出场顺序和组长已生成。每次只查看一个组，确认后进入工位抽签。' : '确认角色后，系统会在每个已命名的组内打乱出场顺序，并随机指定 1 名组长。'}
            />
            {currentAssignmentGroup && (
              <Card
                size="small"
                title={`${currentAssignmentGroup.groupName} · ${currentAssignmentGroup.orgName} · ${currentAssignmentGroup.members.length}人`}
              >
                <Table<CompetitionPlayerSortingAssignment>
                  rowKey={(record) => `${record.groupNo}-${record.groupOrder}-${record.contestantProfileId}`}
                  size="small"
                  dataSource={currentAssignmentGroup.members}
                  columns={assignmentColumns}
                  pagination={false}
                  scroll={{ x: 980 }}
                />
              </Card>
            )}
            {assignmentGroups.length > 1 && (
              <Pagination
                current={assignmentGroupPage}
                pageSize={1}
                total={assignmentGroups.length}
                showSizeChanger={false}
                onChange={setGroupPage}
                showTotal={(total, range) => `第 ${range[0]} / ${total} 组`}
              />
            )}
          </Space>
        )}

        {wizardStep === 4 && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type={assignmentGroups.length > 0 && workstationCodes.length >= assignmentGroups.length ? 'success' : 'warning'}
              showIcon
              message={`共 ${assignmentGroups.length} 个组，当前已创建 ${workstationCodes.length} 个工位编号。`}
              description="先自行填写工位描述，再创建对应编号，如 703、704、715 或 B6、A4。抽签以组为单位，每个组抽到一个工位，组内成员共用该结果。编号数量不能少于组数。"
            />
            <Input
              value={workstationLabel}
              maxLength={16}
              placeholder="填写工位描述"
              style={{ width: 240 }}
              onChange={(event) => setWorkstationLabel(event.target.value)}
            />
            <Space wrap>
              <Input
                value={codeDraft}
                maxLength={32}
                placeholder="输入编号，多个可用逗号或空格分隔"
                style={{ width: 320 }}
                onChange={(event) => setCodeDraft(event.target.value)}
                onPressEnter={() => addCodes(codeDraft)}
              />
              <Button icon={<PlusOutlined />} onClick={() => addCodes(codeDraft)}>添加编号</Button>
              <Button disabled={!workstationCodes.length} onClick={() => setWorkstationCodes([])}>清空编号</Button>
            </Space>
            <Space wrap>
              {workstationCodes.map((code) => (
                <Tag key={code} closable onClose={(event) => {
                  event.preventDefault();
                  setWorkstationCodes((current) => current.filter((item) => item !== code));
                }}
                >{code}</Tag>
              ))}
              {!workstationCodes.length && <Typography.Text type="secondary">还没有工位编号</Typography.Text>}
            </Space>
          </Space>
        )}
      </Modal>

      <Modal
        title={taskDetailQuery.data?.stageName ? `${taskDetailQuery.data.stageName} · 实操出场名单` : '实操出场名单'}
        open={resultOpen}
        onCancel={() => setResultOpen(false)}
        width={980}
        footer={(
          <Space>
            <Button onClick={() => setResultOpen(false)}>关闭</Button>
            {taskDetailQuery.data?.groupingMode === 'company' && taskDetailQuery.data.status === 'drawn' && canCreate && (
              <Button onClick={() => {
                if (!taskDetailQuery.data) return;
                applyDetail(taskDetailQuery.data);
                setResultOpen(false);
                setWizardStep(4);
                setWizardOpen(true);
              }}
              >重新抽签工位</Button>
            )}
            {activeTaskId && canExport && (
              <Button
                type="primary"
                icon={<DownloadOutlined />}
                loading={downloadMutation.isPending}
                onClick={() => downloadMutation.mutate(activeTaskId)}
              >导出 Excel</Button>
            )}
          </Space>
        )}
      >
        {taskDetailQuery.isLoading ? <Spin /> : taskDetailQuery.isError
          ? <Result status="error" title="排序结果加载失败" extra={<Button onClick={() => taskDetailQuery.refetch()}>重新加载</Button>} />
          : (
            <>
              {taskDetailQuery.data && (
                <Alert
                  style={{ marginBottom: 12 }}
                  type="success"
                  showIcon
                  message={taskDetailQuery.data.groupingMode === 'company'
                    ? `共 ${taskDetailQuery.data.playerCount} 人，分为 ${taskDetailQuery.data.groupCount} 组；每组随机指定 1 名组长${taskDetailQuery.data.workstationLabel ? `；工位名称 ${taskDetailQuery.data.workstationLabel}` : ''}。`
                    : `共 ${taskDetailQuery.data.playerCount} 人，${taskDetailQuery.data.groupCount} 组；每组工位 ${taskDetailQuery.data.workstationCount} 个。`}
                />
              )}
              {currentResultGroup && (
                <Card
                  size="small"
                  title={`${currentResultGroup.groupName} · ${currentResultGroup.orgName} · ${currentResultGroup.members.length}人${currentResultGroup.workstation ? ` · ${taskDetailQuery.data?.workstationLabel || '工位'} ${currentResultGroup.workstation}` : ''}`}
                >
                  <Table<CompetitionPlayerSortingAssignment>
                    rowKey={(record) => `${record.groupNo}-${record.groupOrder}-${record.contestantProfileId}`}
                    dataSource={currentResultGroup.members}
                    columns={assignmentColumns}
                    pagination={false}
                    scroll={{ x: 1000 }}
                  />
                </Card>
              )}
              {resultGroups.length > 1 && (
                <Pagination
                  style={{ marginTop: 12 }}
                  current={resultGroupIndex}
                  pageSize={1}
                  total={resultGroups.length}
                  showSizeChanger={false}
                  onChange={setResultGroupPage}
                  showTotal={(total, range) => `第 ${range[0]} / ${total} 组`}
                />
              )}
            </>
          )}
      </Modal>
    </div>
  );
}

export function CompetitionPlayerSortingManagementPage() {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:player-sorting:view');
  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'player-sorting'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];
  const [selectedEventId, setSelectedEventId] = useState<string>();
  // 原始工种 state（可能短暂持有跨赛事的旧 id）；下游一律使用下方派生的 selectedWorkTypeId
  const [rawSelectedWorkTypeId, setSelectedWorkTypeId] = useState<string>();
  const [selectedLevelCode, setSelectedLevelCode] = useState<string>();
  const [selectedStageId, setSelectedStageId] = useState<string>();

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
    queryKey: ['competition-event-work-types', selectedEventId, 'player-sorting'],
    queryFn: () => listCompetitionEventWorkTypes(selectedEventId!),
    enabled: canView && Boolean(selectedEventId),
  });
  const workTypeOptions = useMemo(() => (
    (workTypesQuery.data || [])
      .filter((workType) => workType.status === 'active')
      .map((workType) => ({
        value: idValue(workType.id),
        label: workType.displayName || workType.workTypeName || idValue(workType.workTypeId),
      }))
  ), [workTypesQuery.data]);
  // 渲染期派生有效工种：跨赛事切换时，未在当前赛事工种列表中的 id 当帧钳为 undefined，
  // 查询不会发出（effect 事后纠偏存在一帧竞态，会打出 404/422 控制台噪音）
  const selectedWorkTypeId = rawSelectedWorkTypeId
    && workTypeOptions.some((item) => item.value === rawSelectedWorkTypeId)
    ? rawSelectedWorkTypeId
    : undefined;

  useEffect(() => {
    if (!workTypesQuery.data) return;
    if (!workTypeOptions.length) {
      setSelectedWorkTypeId(undefined);
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
      return;
    }
    if (!rawSelectedWorkTypeId || !workTypeOptions.some((item) => item.value === rawSelectedWorkTypeId)) {
      setSelectedWorkTypeId(workTypeOptions[0].value);
      setSelectedLevelCode(undefined);
      setSelectedStageId(undefined);
    }
  }, [rawSelectedWorkTypeId, workTypeOptions, workTypesQuery.data]);

  const levelsQuery = useQuery({
    queryKey: ['competition-event-work-type-levels', selectedEventId, selectedWorkTypeId, 'player-sorting'],
    queryFn: () => listCompetitionEventWorkTypeLevels(selectedEventId!, selectedWorkTypeId!),
    enabled: canView && Boolean(selectedEventId && selectedWorkTypeId),
  });
  const levelOptions = useMemo(() => (
    (levelsQuery.data || []).map((level) => ({
      value: level.levelCode,
      label: level.levelName ? `${level.levelName}（${level.levelCode}）` : level.levelCode,
    }))
  ), [levelsQuery.data]);

  useEffect(() => {
    if (!levelsQuery.data || !selectedWorkTypeId) return;
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
    queryKey: ['competition-stages', 'player-sorting', selectedEventId, selectedWorkTypeId, selectedLevelCode],
    queryFn: () => pageCompetitionStages(selectedEventId!, {
      page: 1,
      pageSize: 100,
      eventWorkTypeId: selectedWorkTypeId,
      eventWorkTypeLevelCode: selectedLevelCode,
    }),
    enabled: canView && Boolean(selectedEventId && selectedWorkTypeId && selectedLevelCode),
  });
  const stages = stagesQuery.data?.items || [];

  useEffect(() => {
    if (!stagesQuery.data) return;
    const stageIds = stages.map((stage) => idValue(stage.id));
    if (!selectedStageId || !stageIds.includes(selectedStageId)) {
      setSelectedStageId(stageIds[0]);
    }
  }, [selectedStageId, stages, stagesQuery.data]);

  const selectedStage = stages.find((stage) => idValue(stage.id) === selectedStageId);

  if (!canView) return <Result status="403" title="暂无排序分组查看权限" />;
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventsQuery.isError) {
    return <Result status="error" title="赛事任务加载失败" extra={<Button onClick={() => eventsQuery.refetch()}>重新加载</Button>} />;
  }

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}><TeamOutlined /> 排序分组</Typography.Title>
          <Typography.Paragraph type="secondary">
            按赛事工种、技能等级和赛段查看并维护实操出场排序任务。
            {selectedStage ? ` 当前赛段：${selectedStage.name}` : ''}
          </Typography.Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            void eventsQuery.refetch();
            if (selectedEventId) void workTypesQuery.refetch();
            if (selectedEventId && selectedWorkTypeId) void levelsQuery.refetch();
            if (selectedEventId && selectedWorkTypeId && selectedLevelCode) {
              void stagesQuery.refetch();
            }
          }}
        >刷新</Button>
      </section>

      <Card bordered={false} className="competition-filter-card competition-filter-card--compact">
        <Space align="center" size={12} wrap style={{ width: '100%' }}>
          <Typography.Text type="secondary">赛事任务</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedEventId}
            loading={eventsQuery.isFetching}
            disabled={!events.length}
            options={events.map((event: CompetitionEvent) => ({
              value: idValue(event.id),
              label: `${event.code} · ${event.name}`,
            }))}
            onChange={(value) => {
              setSelectedEventId(value);
              setSelectedWorkTypeId(undefined);
              setSelectedLevelCode(undefined);
              setSelectedStageId(undefined);
            }}
            placeholder="选择赛事任务"
            style={{ width: 280, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">工种</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedWorkTypeId}
            loading={workTypesQuery.isFetching}
            disabled={!selectedEventId || (!workTypesQuery.isFetching && workTypeOptions.length === 0)}
            options={workTypeOptions}
            onChange={(value) => {
              setSelectedWorkTypeId(value);
              setSelectedLevelCode(undefined);
              setSelectedStageId(undefined);
            }}
            placeholder="选择工种"
            style={{ width: 220, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">等级</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedLevelCode}
            loading={levelsQuery.isFetching}
            disabled={!selectedWorkTypeId || levelOptions.length === 0}
            options={levelOptions}
            onChange={(value) => {
              setSelectedLevelCode(value);
              setSelectedStageId(undefined);
            }}
            placeholder="选择等级"
            style={{ width: 180, maxWidth: '100%' }}
          />
          <Typography.Text type="secondary">赛段</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedStageId}
            loading={stagesQuery.isFetching}
            disabled={!selectedLevelCode || stages.length === 0}
            options={stages.map((stage: CompetitionStage) => ({
              value: idValue(stage.id),
              label: `${stage.sortOrder}. ${stage.name}`,
            }))}
            onChange={setSelectedStageId}
            placeholder="选择赛段"
            style={{ width: 220, maxWidth: '100%' }}
          />
        </Space>
      </Card>

      {workTypesQuery.isError && <Alert type="error" showIcon message={errorText(workTypesQuery.error, '赛事工种加载失败')} />}
      {levelsQuery.isError && <Alert type="error" showIcon message={errorText(levelsQuery.error, '工种等级加载失败')} />}
      {stagesQuery.isError && <Alert type="error" showIcon message={errorText(stagesQuery.error, '赛段加载失败')} />}
      {!events.length && <Alert type="info" showIcon message="当前没有可用赛事任务" />}
      {!workTypesQuery.isFetching && selectedEventId && workTypeOptions.length === 0 && (
        <Alert type="info" showIcon message="当前赛事暂无可用工种" />
      )}
      {!levelsQuery.isFetching && selectedWorkTypeId && levelOptions.length === 0 && (
        <Alert type="info" showIcon message="当前工种暂无已配置等级" />
      )}
      {!stagesQuery.isFetching && selectedLevelCode && stagesQuery.isSuccess && stages.length === 0 && (
        <Alert type="info" showIcon message="当前工种和等级下暂无赛段，请先完成赛段编排" />
      )}
      {selectedStage && (
        <CompetitionPlayerSortingPage
          key={idValue(selectedStage.id)}
          stageId={idValue(selectedStage.id)}
          stageName={selectedStage.name}
          showTitle={false}
        />
      )}
    </div>
  );
}

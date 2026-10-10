import { DeleteOutlined, EditOutlined, ReloadOutlined, TeamOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import {
  addPreparationGroupMember,
  createPreparationAssessorGroup,
  deletePreparationAssessorGroup,
  listPreparationAssessorGroupWorktypes,
  listPreparationAssessorGroups,
  listPreparationGroupMembers,
  removePreparationGroupMember,
  updatePreparationAssessorGroup,
} from '../../../api/certificationPreparation';
import { listAvailableAssessors } from '../../../api/certificationInfo';
import { PermissionGate } from '../../../directives/permission';
import type { AssessorInfo } from '../../../types/certificationInfo';
import type {
  PreparationAssessorGroup,
  PreparationAssessorGroupPayload,
  PreparationAssessorGroupWorktype,
  PreparationTaskAssessor,
  PreparationTaskOverview,
} from '../../../types/certificationPreparation';
import { dash, dateText, statusTag } from './shared';

interface AssessorConfigProps {
  taskId?: string;
  task?: PreparationTaskOverview;
  onChanged?: () => void;
}

export function AssessorConfig({ taskId, task, onChanged }: AssessorConfigProps) {
  const queryClient = useQueryClient();
  const [selectedGroupId, setSelectedGroupId] = useState<string>();
  const [groupModalOpen, setGroupModalOpen] = useState(false);
  const [memberWizardOpen, setMemberWizardOpen] = useState(false);
  const [candidateKeyword, setCandidateKeyword] = useState('');
  const [selectedCandidateIds, setSelectedCandidateIds] = useState<string[]>([]);
  const [editingGroup, setEditingGroup] = useState<PreparationAssessorGroup | null>(null);
  const [groupForm] = Form.useForm<PreparationAssessorGroupPayload & { workTypeScopeIds?: string[] }>();

  const groupsQuery = useQuery({
    queryKey: ['certification-preparation-assessor-groups', taskId],
    queryFn: () => listPreparationAssessorGroups(taskId!),
    enabled: Boolean(taskId),
  });

  const membersQuery = useQuery({
    queryKey: ['certification-preparation-assessor-members', taskId, selectedGroupId],
    queryFn: () => listPreparationGroupMembers(taskId!, selectedGroupId!),
    enabled: Boolean(
      taskId
      && selectedGroupId
      && (groupsQuery.data || []).some((group) => group.id === selectedGroupId),
    ),
  });

  const editingGroupMembersQuery = useQuery({
    queryKey: ['certification-preparation-assessor-members-for-edit', taskId, editingGroup?.id],
    queryFn: () => listPreparationGroupMembers(taskId!, editingGroup!.id),
    enabled: Boolean(taskId && editingGroup?.id),
  });

  const taskWorktypesQuery = useQuery({
    queryKey: ['certification-preparation-task-worktypes', taskId],
    queryFn: () => listPreparationAssessorGroupWorktypes(taskId!),
    enabled: Boolean(taskId),
  });

  useEffect(() => {
    const groups = groupsQuery.data || [];
    if (!selectedGroupId && groups.length > 0) {
      setSelectedGroupId(groups[0].id);
    }
    if (selectedGroupId && groups.length > 0 && !groups.some((group) => group.id === selectedGroupId)) {
      setSelectedGroupId(groups[0].id);
    }
    if (selectedGroupId && !groupsQuery.isFetching && groups.length === 0) {
      setSelectedGroupId(undefined);
    }
  }, [groupsQuery.data, selectedGroupId]);

  const selectedGroup = (groupsQuery.data || []).find((group) => group.id === selectedGroupId);
  const requiredWorkTypeScopes = selectedGroup?.workTypeScopes?.length
    ? selectedGroup.workTypeScopes
    : taskWorktypesQuery.data || [];
  const allRequiredWorkTypesHaveIds = requiredWorkTypeScopes.length > 0
    && requiredWorkTypeScopes.every((scope) => Boolean(scope.workTypeId));
  const requiredWorkTypeIds = allRequiredWorkTypesHaveIds
    ? [...new Set(requiredWorkTypeScopes.map((scope) => String(scope.workTypeId)))]
    : [];
  const requiredWorkTypeNames = allRequiredWorkTypesHaveIds
    ? []
    : [...new Set(requiredWorkTypeScopes.map((scope) => scope.workTypeName || '').filter(Boolean))];

  const availableCandidatesQuery = useQuery({
    queryKey: [
      'certification-info-assessors-available', candidateKeyword, requiredWorkTypeIds, requiredWorkTypeNames,
    ],
    queryFn: () => listAvailableAssessors(
      candidateKeyword || undefined,
      true,
      requiredWorkTypeIds,
      requiredWorkTypeNames,
      true,
    ),
    enabled: memberWizardOpen,
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-groups', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-members', taskId, selectedGroupId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-info-assessors-available'] });
    queryClient.invalidateQueries({ queryKey: ['practical-scoring-setup', taskId] });
    onChanged?.();
  };

  const createGroupMutation = useMutation({
    mutationFn: (values: PreparationAssessorGroupPayload) => createPreparationAssessorGroup(taskId!, values),
    onSuccess: (group) => {
      message.success('考评组已创建');
      setSelectedGroupId(group.id);
      setGroupModalOpen(false);
      groupForm.resetFields();
      refresh();
    },
  });

  const updateGroupMutation = useMutation({
    mutationFn: ({ id, values }: { id: string; values: PreparationAssessorGroupPayload }) =>
      updatePreparationAssessorGroup(taskId!, id, values),
    onSuccess: () => {
      message.success('考评组已更新');
      setGroupModalOpen(false);
      setEditingGroup(null);
      groupForm.resetFields();
      refresh();
    },
  });

  const deleteGroupMutation = useMutation({
    mutationFn: (groupId: string) => deletePreparationAssessorGroup(taskId!, groupId),
    onSuccess: () => {
      message.success('考评组已删除');
      refresh();
    },
  });

  const addMembersMutation = useMutation({
    mutationFn: async () => {
      if (!taskId || !selectedGroupId) throw new Error('请先选择考评组');
      if (selectedCandidateIds.length === 0) {
        throw new Error('请至少选择 1 名成员');
      }
      const members = [];
      for (const assessorId of selectedCandidateIds) {
        members.push(await addPreparationGroupMember(taskId, selectedGroupId, {
          assessorId,
          taskRole: 'member',
          roleName: '成员',
        }));
      }
      return members;
    },
    onSuccess: (members) => {
      message.success(`已添加 ${members.length} 名考评成员`);
      closeMemberWizard();
      refresh();
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: (memberId: string) => removePreparationGroupMember(taskId!, selectedGroupId!, memberId),
    onSuccess: () => {
      message.success('考评组成员已移除');
      refresh();
    },
  });

  const groupColumns: ColumnsType<PreparationAssessorGroup> = [
    { title: '考评组', dataIndex: 'groupName', minWidth: 180, render: dash },
    {
      title: '工种/科目',
      dataIndex: 'workTypeScopes',
      width: 240,
      render: (scopes?: PreparationAssessorGroupWorktype[]) => {
        if (!scopes || scopes.length === 0) return '-';
        return scopes.map((s) => [s.workTypeName, s.levelName].filter(Boolean).join('-')).join('、');
      },
    },
    { title: '组长', dataIndex: 'leaderAssessorName', width: 140, render: dash },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '操作',
      width: 210,
      render: (_, record) => (
        <Space size={4}>
          <PermissionGate code="certification.preparation.assessor.assign">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditGroup(record)}>编辑</Button>
            <Popconfirm
              title="确认删除该考评组？"
              description="删除后不可恢复；若存在成员或已发放任务将无法删除。"
              okText="删除"
              cancelText="取消"
              onConfirm={() => deleteGroupMutation.mutate(record.id)}
            >
              <Button type="link" danger size="small" icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          </PermissionGate>
        </Space>
      ),
    },
  ];

  const memberColumns: ColumnsType<PreparationTaskAssessor> = [
    { title: '姓名', dataIndex: 'assessorName', width: 120 },
    { title: '证号', dataIndex: 'certificateNo', width: 160, render: dash },
    { title: '任务角色', dataIndex: 'roleName', width: 120, render: (_, row) => row.roleName || roleName(row.taskRole) },
    { title: '工种范围', dataIndex: 'workTypeScope', minWidth: 180, render: dash },
    { title: '有效期至', dataIndex: 'validUntil', width: 120, render: dateText },
    { title: '工作状态', dataIndex: 'workStatus', width: 110, render: statusTag },
    {
      title: '操作',
      width: 110,
      render: (_, record) => (
        <PermissionGate code="certification.preparation.assessor.assign">
          <Popconfirm title="确认移除该成员？" okText="移除" cancelText="取消" onConfirm={() => removeMemberMutation.mutate(record.id)}>
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>移除</Button>
          </Popconfirm>
        </PermissionGate>
      ),
    },
  ];

  function openEditGroup(record: PreparationAssessorGroup) {
    setEditingGroup(record);
    groupForm.setFieldsValue({
      groupName: record.groupName,
      leaderAssessorId: record.leaderAssessorId,
      workTypeScopeIds: (record.workTypeScopes || []).map((s) => String(s.scopeId)),
      remark: record.remark,
    });
    setGroupModalOpen(true);
  }

  function submitGroup() {
    groupForm.validateFields().then((values) => {
      const payload: PreparationAssessorGroupPayload = {
        groupName: values.groupName,
        leaderAssessorId: values.leaderAssessorId,
        workTypeScopeIds: values.workTypeScopeIds,
        remark: values.remark,
      };
      if (editingGroup) {
        updateGroupMutation.mutate({ id: editingGroup.id, values: payload });
        return;
      }
      createGroupMutation.mutate(payload);
    });
  }

  function openMemberWizard() {
    if (!taskId || !selectedGroupId) {
      message.warning('请先选择考评组');
      return;
    }
    setCandidateKeyword('');
    setSelectedCandidateIds([]);
    setMemberWizardOpen(true);
  }

  function closeMemberWizard() {
    setMemberWizardOpen(false);
    setCandidateKeyword('');
    setSelectedCandidateIds([]);
  }

  const candidateColumns: ColumnsType<AssessorInfo> = [
    { title: '姓名', dataIndex: 'assessorName', width: 120, render: dash },
    { title: '证号', dataIndex: 'certificateNo', width: 160, render: dash },
    {
      title: '工种范围',
      dataIndex: 'workTypeScopes',
      minWidth: 180,
      render: (value: string[] | undefined) => (value && value.length > 0 ? value.join('、') : '-'),
    },
    { title: '等级范围', dataIndex: 'levelScope', width: 150, render: dash },
    { title: '有效期至', dataIndex: 'validUntil', width: 120, render: dateText },
    { title: '当前状态', dataIndex: 'workStatus', width: 110, render: statusTag },
  ];

  const worktypeOptions = (taskWorktypesQuery.data || []).map((w) => {
    const label = [w.workTypeName, w.levelName].filter(Boolean).join('-');
    return { label, value: String(w.scopeId) };
  });

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="任务考评组"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
            <PermissionGate code="certification.preparation.assessor.assign">
              <Button
                icon={<TeamOutlined />}
                disabled={!taskId}
                onClick={() => {
                  setEditingGroup(null);
                  groupForm.resetFields();
                  groupForm.setFieldsValue({ workTypeScopeIds: [] });
                  setGroupModalOpen(true);
                }}
              >
                新建考评组
              </Button>
            </PermissionGate>
          </Space>
        }
      >
        {!taskId ? (
          <Typography.Text type="secondary">请选择认定任务后配置考评组。</Typography.Text>
        ) : (
          <Table
            rowKey="id"
            columns={groupColumns}
            dataSource={groupsQuery.data || []}
            loading={groupsQuery.isFetching}
            pagination={false}
            rowClassName={(record) => record.id === selectedGroupId ? 'selected-row' : ''}
            onRow={(record) => ({ onClick: () => setSelectedGroupId(record.id) })}
          />
        )}
      </ProCard>

      <ProCard
        title={selectedGroup ? `成员：${selectedGroup.groupName || selectedGroup.id}` : '考评组成员'}
        extra={
          <PermissionGate code="certification.preparation.assessor.assign">
            <Button disabled={!taskId || !selectedGroupId} icon={<TeamOutlined />} onClick={openMemberWizard}>
              添加成员
            </Button>
          </PermissionGate>
        }
      >
        <Table
          rowKey="id"
          columns={memberColumns}
          dataSource={membersQuery.data || []}
          loading={membersQuery.isFetching}
          pagination={false}
          scroll={{ x: 920 }}
        />
      </ProCard>

      <Modal
        title={editingGroup ? '编辑考评组' : '创建考评组'}
        open={groupModalOpen}
        onOk={submitGroup}
        onCancel={() => setGroupModalOpen(false)}
        confirmLoading={createGroupMutation.isPending || updateGroupMutation.isPending}
        width={680}
      >
        <Form form={groupForm} layout="vertical">
          <Form.Item name="groupName" label="考评组名称" rules={[{ required: true, message: '请输入考评组名称' }]}>
            <Input />
          </Form.Item>
          {editingGroup ? (
            <Form.Item name="leaderAssessorId" label="组长" rules={[{ required: true, message: '请选择组长' }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={(editingGroupMembersQuery.data || []).map((m) => ({
                  label: `${m.assessorName || '-'} ${m.certificateNo || ''}`,
                  value: m.assessorId,
                }))}
                loading={editingGroupMembersQuery.isFetching}
                placeholder="从本组成员中选择组长"
              />
            </Form.Item>
          ) : (
            <Typography.Paragraph type="secondary">
              先创建空考评组，再在下方"考评组成员"区域添加成员，最后编辑考评组指定组长。
            </Typography.Paragraph>
          )}
          <Form.Item
            name="workTypeScopeIds"
            label="工种/科目"
            rules={[{ required: true, message: '请选择至少一个工种' }]}
            extra={worktypeOptions.length === 0 ? '当前任务尚未配置工种范围，请先在认定任务中维护' : '可多选，范围严格限定为当前认定任务已配置的工种'}
          >
            <Select
              mode="multiple"
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="请选择工种"
              loading={taskWorktypesQuery.isFetching}
              options={worktypeOptions}
              disabled={worktypeOptions.length === 0}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`添加成员${selectedGroup ? ` - ${selectedGroup.groupName}` : ''}`}
        open={memberWizardOpen}
        onCancel={closeMemberWizard}
        width={920}
        okText="添加到当前考评组"
        confirmLoading={addMembersMutation.isPending}
        onOk={() => {
          if (selectedCandidateIds.length === 0) {
            message.warning('请至少选择 1 名成员');
            return;
          }
          addMembersMutation.mutate();
        }}
      >
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          {availableCandidatesQuery.isError ? (
            <Alert
              type="error"
              showIcon
              message="可用考评员筛选失败"
              description={candidateQueryErrorMessage(availableCandidatesQuery.error)}
            />
          ) : null}
          <Input.Search
            allowClear
            placeholder="搜索姓名、证号"
            onSearch={(value) => setCandidateKeyword(value || '')}
          />
          <Table
            rowKey="id"
            columns={candidateColumns}
            dataSource={availableCandidatesQuery.data || []}
            loading={availableCandidatesQuery.isFetching}
            pagination={false}
            scroll={{ x: 860, y: 360 }}
            rowSelection={{
              selectedRowKeys: selectedCandidateIds,
              onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
            }}
          />
          <Typography.Text type="secondary">
            已选 {selectedCandidateIds.length} 人（候选为已入库、证书未过期的考评员；已绑定其他任务仍可选择）
          </Typography.Text>
        </Space>
      </Modal>
    </Space>
  );
}

function roleName(role?: string) {
  return role === 'leader' ? '组长' : role === 'member' ? '成员' : dash(role);
}

function candidateQueryErrorMessage(error: unknown) {
  if (error && typeof error === 'object') {
    const responseMessage = (error as { response?: { data?: { message?: unknown } } })
      .response?.data?.message;
    if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage;
    if (error instanceof Error && error.message) return error.message;
  }
  return '请检查认定任务工种范围和考评员历史授权，并修正后重试。';
}

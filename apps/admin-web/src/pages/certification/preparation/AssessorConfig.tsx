import { DeleteOutlined, EditOutlined, ReloadOutlined, SendOutlined, TeamOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Typography, Upload, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type Dayjs } from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import {
  addPreparationGroupMember,
  createPreparationAssessorGroup,
  deletePreparationAssessorGroup,
  dispatchPreparationAssessorGroup,
  listPreparationAssessorGroups,
  listPreparationGroupMembers,
  removePreparationGroupMember,
  updatePreparationAssessorGroup,
} from '../../../api/certificationPreparation';
import { listAvailableAssessors } from '../../../api/certificationInfo';
import { PermissionGate } from '../../../directives/permission';
import type { AssessorInfo } from '../../../types/certificationInfo';
import type {
  PreparationAssessorDispatchPayload,
  PreparationAssessorGroup,
  PreparationAssessorGroupPayload,
  PreparationTaskAssessor,
  PreparationTaskOverview,
} from '../../../types/certificationPreparation';
import { dash, dateText, formatDate, statusTag } from './shared';
import { uploadPlatformFile } from '../../../api/files';

interface AssessorConfigProps {
  taskId?: string;
  task?: PreparationTaskOverview;
  onChanged?: () => void;
}

interface DispatchFormValues extends Omit<PreparationAssessorDispatchPayload, 'dueDate'> {
  dueDate?: Dayjs;
}

export function AssessorConfig({ taskId, task, onChanged }: AssessorConfigProps) {
  const queryClient = useQueryClient();
  const [selectedGroupId, setSelectedGroupId] = useState<string>();
  const [groupModalOpen, setGroupModalOpen] = useState(false);
  const [memberWizardOpen, setMemberWizardOpen] = useState(false);
  const [memberWizardStep, setMemberWizardStep] = useState<'pick' | 'group'>('pick');
  const [candidateKeyword, setCandidateKeyword] = useState('');
  const [selectedCandidateIds, setSelectedCandidateIds] = useState<string[]>([]);
  const [leaderCandidateId, setLeaderCandidateId] = useState<string>();
  const [dispatchModalOpen, setDispatchModalOpen] = useState(false);
  const [dispatchAssessmentUploading, setDispatchAssessmentUploading] = useState(false);
  const [editingGroup, setEditingGroup] = useState<PreparationAssessorGroup | null>(null);
  const [groupForm] = Form.useForm<PreparationAssessorGroupPayload>();
  const [dispatchForm] = Form.useForm<DispatchFormValues>();

  const availableCandidatesQuery = useQuery({
    queryKey: ['certification-info-assessors-available', candidateKeyword],
    queryFn: () => listAvailableAssessors(candidateKeyword || undefined),
    enabled: memberWizardOpen,
  });

  const groupsQuery = useQuery({
    queryKey: ['certification-preparation-assessor-groups', taskId],
    queryFn: () => listPreparationAssessorGroups(taskId!),
    enabled: Boolean(taskId),
  });

  const membersQuery = useQuery({
    queryKey: ['certification-preparation-assessor-members', taskId, selectedGroupId],
    queryFn: () => listPreparationGroupMembers(taskId!, selectedGroupId!),
    enabled: Boolean(taskId && selectedGroupId),
  });

  const editingGroupMembersQuery = useQuery({
    queryKey: ['certification-preparation-assessor-members-for-edit', taskId, editingGroup?.id],
    queryFn: () => listPreparationGroupMembers(taskId!, editingGroup!.id),
    enabled: Boolean(taskId && editingGroup?.id),
  });

  useEffect(() => {
    const groups = groupsQuery.data || [];
    if (!selectedGroupId && groups.length > 0) {
      setSelectedGroupId(groups[0].id);
    }
    if (selectedGroupId && groups.length > 0 && !groups.some((group) => group.id === selectedGroupId)) {
      setSelectedGroupId(groups[0].id);
    }
  }, [groupsQuery.data, selectedGroupId]);

  const selectedGroup = (groupsQuery.data || []).find((group) => group.id === selectedGroupId);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-groups', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-members', taskId, selectedGroupId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-info-assessors-available'] });
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

  const memberWizardSaveMutation = useMutation({
    mutationFn: async () => {
      const values = await groupForm.validateFields();
      if (!taskId) {
        throw new Error('请先选择认定任务');
      }
      if (selectedCandidateIds.length === 0) {
        throw new Error('请至少选择 1 名成员');
      }
      const leaderId = leaderCandidateId || values.leaderAssessorId;
      if (!leaderId) {
        throw new Error('请选择组长');
      }
      if (!selectedCandidateIds.includes(String(leaderId))) {
        throw new Error('组长必须从已选成员中选择');
      }

      const group = await createPreparationAssessorGroup(taskId, {
        ...values,
        leaderAssessorId: leaderId,
        status: 'preparing',
      });

      for (const assessorId of selectedCandidateIds) {
        await addPreparationGroupMember(taskId, group.id, {
          assessorId,
          taskRole: String(assessorId) === String(leaderId) ? 'leader' : 'member',
          roleName: String(assessorId) === String(leaderId) ? '组长' : '成员',
        });
      }
      return group;
    },
    onSuccess: (group) => {
      message.success('配置成功');
      setSelectedGroupId(group.id);
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

  const dispatchMutation = useMutation({
    mutationFn: (values: DispatchFormValues) => dispatchPreparationAssessorGroup(taskId!, selectedGroupId!, {
      subject: values.subject,
      title: values.title,
      assessmentFileId: values.assessmentFileId,
      assessmentFileName: values.assessmentFileName,
      durationMin: values.durationMin,
      dueDate: formatDate(values.dueDate),
      remark: values.remark,
    }),
    onSuccess: (tasks) => {
      message.success(`已发放 ${tasks.length} 个考评任务`);
      setDispatchModalOpen(false);
      dispatchForm.resetFields();
      refresh();
    },
  });

  const groupColumns: ColumnsType<PreparationAssessorGroup> = [
    { title: '考评组', dataIndex: 'groupName', minWidth: 180, render: dash },
    { title: '工种/科目', dataIndex: 'workTypeName', width: 140, render: dash },
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
      workTypeName: record.workTypeName,
      status: record.status,
      remark: record.remark,
    });
    setGroupModalOpen(true);
  }

  function submitGroup() {
    groupForm.validateFields().then((values) => {
      if (editingGroup) {
        updateGroupMutation.mutate({ id: editingGroup.id, values });
        return;
      }
      createGroupMutation.mutate(values);
    });
  }

  function openMemberWizard() {
    if (!taskId) {
      message.warning('请先选择认定任务');
      return;
    }
    setMemberWizardStep('pick');
    setCandidateKeyword('');
    setSelectedCandidateIds([]);
    setLeaderCandidateId(undefined);
    groupForm.resetFields();
    groupForm.setFieldsValue({ status: 'preparing', workTypeName: task?.workTypeName });
    setMemberWizardOpen(true);
  }

  function closeMemberWizard() {
    setMemberWizardOpen(false);
    setMemberWizardStep('pick');
    setCandidateKeyword('');
    setSelectedCandidateIds([]);
    setLeaderCandidateId(undefined);
    groupForm.resetFields();
  }

  const candidateColumns: ColumnsType<AssessorInfo> = [
    { title: '姓名', dataIndex: 'assessorName', width: 120, render: dash },
    { title: '证号', dataIndex: 'certificateNo', width: 160, render: dash },
    { title: '工种范围', dataIndex: 'workTypeScope', minWidth: 180, render: dash },
    { title: '等级范围', dataIndex: 'levelScope', width: 150, render: dash },
    { title: '有效期至', dataIndex: 'validUntil', width: 120, render: dateText },
  ];

  const selectedCandidateOptions = useMemo(() => {
    const all = availableCandidatesQuery.data || [];
    return all
      .filter((x) => selectedCandidateIds.includes(String(x.id)))
      .map((x) => ({ label: `${x.assessorName || '-'} ${x.certificateNo || ''}`, value: String(x.id) }));
  }, [availableCandidatesQuery.data, selectedCandidateIds]);

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="任务考评组"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
            <PermissionGate code="certification.preparation.assessor.assign">
              <Button
                disabled={!selectedGroupId || (groupsQuery.data || []).length === 0}
                type="primary"
                icon={<SendOutlined />}
                onClick={() => setDispatchModalOpen(true)}
              >
                发放任务
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
            <Button disabled={!taskId} icon={<TeamOutlined />} onClick={openMemberWizard}>
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
          <Form.Item name="leaderAssessorId" label="组长" rules={[{ required: true, message: '请选择组长' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={
                (editingGroup ? (editingGroupMembersQuery.data || []) : (membersQuery.data || []))
                  .map((m) => ({
                    label: `${m.assessorName || '-'} ${m.certificateNo || ''}`,
                    value: m.assessorId,
                  }))
              }
              loading={editingGroup ? editingGroupMembersQuery.isFetching : membersQuery.isFetching}
              placeholder="从本组成员中选择组长"
            />
          </Form.Item>
          <Form.Item name="workTypeName" label="工种/科目">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ label: '筹备中', value: 'preparing' }, { label: '已发放', value: 'dispatched' }, { label: '已完成', value: 'completed' }]} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={memberWizardStep === 'pick' ? '添加成员' : '创建考评组'}
        open={memberWizardOpen}
        onCancel={closeMemberWizard}
        width={memberWizardStep === 'pick' ? 920 : 680}
        okText={memberWizardStep === 'pick' ? '创建考评组' : '保存'}
        confirmLoading={memberWizardSaveMutation.isPending}
        onOk={() => {
          if (memberWizardStep === 'pick') {
            if (selectedCandidateIds.length === 0) {
              message.warning('请至少选择 1 名成员');
              return;
            }
            setMemberWizardStep('group');
            return;
          }
          memberWizardSaveMutation.mutate();
        }}
      >
        {memberWizardStep === 'pick' ? (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
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
              已选 {selectedCandidateIds.length} 人（候选来自“考评员信息-已入库且空闲且未过期”）
            </Typography.Text>
          </Space>
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Typography.Text type="secondary">已选 {selectedCandidateIds.length} 名成员，请从已选成员中选择组长。</Typography.Text>
            <Form form={groupForm} layout="vertical">
              <Form.Item name="groupName" label="考评组名称" rules={[{ required: true, message: '请输入考评组名称' }]}>
                <Input />
              </Form.Item>
              <Form.Item label="组长" required>
                <Select
                  showSearch
                  optionFilterProp="label"
                  options={selectedCandidateOptions}
                  value={leaderCandidateId}
                  onChange={(value) => {
                    setLeaderCandidateId(String(value));
                    groupForm.setFieldValue('leaderAssessorId', String(value));
                  }}
                  placeholder="从已选成员中选择组长"
                />
              </Form.Item>
              {/* 仍保留字段在 form 中，便于复用后端入参 */}
              <Form.Item name="leaderAssessorId" hidden rules={[{ required: true, message: '请选择组长' }]}>
                <Input />
              </Form.Item>
              <Form.Item name="workTypeName" label="工种/科目">
                <Input />
              </Form.Item>
              <Form.Item name="remark" label="备注">
                <Input.TextArea rows={2} />
              </Form.Item>
            </Form>
            <Button onClick={() => setMemberWizardStep('pick')}>上一步</Button>
          </Space>
        )}
      </Modal>

      <Modal
        title="发放考评任务"
        open={dispatchModalOpen}
        onOk={() => dispatchForm.validateFields().then((values) => dispatchMutation.mutate(values))}
        onCancel={() => setDispatchModalOpen(false)}
        confirmLoading={dispatchMutation.isPending}
      >
        <Form form={dispatchForm} layout="vertical">
          <Form.Item name="subject" label="科目" rules={[{ required: true, message: '请输入科目' }]}>
            <Input placeholder={task?.workTypeName || '实操考评'} />
          </Form.Item>
          <Form.Item
            label="考评表"
            required
            tooltip="导入考评表文件后，将以文件名作为任务标题"
          >
            <Space direction="vertical" style={{ width: '100%' }} size={8}>
              <Upload
                accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg"
                maxCount={1}
                showUploadList={false}
                beforeUpload={async (file) => {
                  setDispatchAssessmentUploading(true);
                  try {
                    const saved = await uploadPlatformFile(file as File);
                    dispatchForm.setFieldsValue({
                      assessmentFileId: String(saved.id),
                      assessmentFileName: saved.originalName,
                      // 兼容后端现有 title 字段（DB 非空）
                      title: saved.originalName,
                    });
                    message.success('考评表已导入');
                  } catch (e) {
                    message.error((e as Error).message || '导入失败');
                  } finally {
                    setDispatchAssessmentUploading(false);
                  }
                  return false;
                }}
              >
                <Button loading={dispatchAssessmentUploading}>导入考评表文件</Button>
              </Upload>
              <Form.Item
                noStyle
                shouldUpdate={(prev, next) => prev.assessmentFileName !== next.assessmentFileName}
              >
                {({ getFieldValue }) => (
                  <Typography.Text type="secondary">
                    {getFieldValue('assessmentFileName') ? `已选择：${getFieldValue('assessmentFileName')}` : '未选择文件'}
                  </Typography.Text>
                )}
              </Form.Item>
            </Space>
          </Form.Item>
          <Form.Item name="assessmentFileId" hidden rules={[{ required: true, message: '请导入考评表文件' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="assessmentFileName" hidden rules={[{ required: true, message: '请导入考评表文件' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="durationMin"
            label="时长（分钟）"
            rules={[
              { required: true, message: '请输入时长' },
              { type: 'number', min: 1, message: '请输入有效时长' },
            ]}
          >
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="dueDate" label="截止日期">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

function roleName(role?: string) {
  return role === 'leader' ? '组长' : role === 'member' ? '成员' : dash(role);
}

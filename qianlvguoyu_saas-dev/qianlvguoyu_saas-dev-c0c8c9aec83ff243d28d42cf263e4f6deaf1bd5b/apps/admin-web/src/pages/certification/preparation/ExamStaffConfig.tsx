import { DeleteOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  assignPreparationExamStaff,
  listPreparationTaskExamStaff,
  removePreparationTaskExamStaff,
  updatePreparationTaskExamStaffRole,
} from '../../../api/certificationPreparation';
import { listAvailableExamStaff } from '../../../api/certificationInfo';
import { PermissionGate } from '../../../directives/permission';
import type { PreparationTaskExamStaff } from '../../../types/certificationPreparation';
import { dash, statusTag } from './shared';

interface ExamStaffConfigProps {
  taskId?: string;
  onChanged?: () => void;
}

interface AssignFormValues {
  staffId: string;
  taskRole: string;
  remark?: string;
}

const EXAM_STAFF_ROLE_OPTIONS = [
  { value: 'internal_supervisor', label: '内督', color: 'gold' },
  { value: 'exam_staff', label: '考务员', color: 'blue' },
  { value: 'invigilator', label: '监考员', color: 'green' },
  { value: 'worker', label: '工作人员', color: 'default' },
] as const;

function roleOption(role?: string) {
  return EXAM_STAFF_ROLE_OPTIONS.find((item) => item.value === role) ?? EXAM_STAFF_ROLE_OPTIONS[1];
}

function roleLabel(role?: string, roleName?: string) {
  return roleName || roleOption(role).label;
}

export function ExamStaffConfig({ taskId, onChanged }: ExamStaffConfigProps) {
  const queryClient = useQueryClient();
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [assignForm] = Form.useForm<AssignFormValues>();

  const availableStaffQuery = useQuery({
    queryKey: ['certification-info-exam-staff-available'],
    queryFn: () => listAvailableExamStaff(),
    enabled: assignModalOpen,
  });

  const taskStaffQuery = useQuery({
    queryKey: ['certification-preparation-task-exam-staff', taskId],
    queryFn: () => listPreparationTaskExamStaff(taskId!),
    enabled: Boolean(taskId),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-exam-staff', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-info-exam-staff-available'] });
    queryClient.invalidateQueries({ queryKey: ['certification-info-exam-staff'] });
    onChanged?.();
  };

  const assignMutation = useMutation({
    mutationFn: (values: AssignFormValues) => {
      const option = roleOption(values.taskRole);
      return assignPreparationExamStaff(taskId!, {
        staffId: values.staffId,
        taskRole: option.value,
        roleName: option.label,
        remark: values.remark,
      });
    },
    onSuccess: () => {
      message.success('考务人员已绑定到任务');
      setAssignModalOpen(false);
      assignForm.resetFields();
      refresh();
    },
  });

  const updateRoleMutation = useMutation({
    mutationFn: ({ taskStaffId, taskRole }: { taskStaffId: string; taskRole: string }) => {
      const option = roleOption(taskRole);
      return updatePreparationTaskExamStaffRole(taskId!, taskStaffId, {
        taskRole: option.value,
        roleName: option.label,
      });
    },
    onSuccess: () => {
      message.success('角色已更新');
      refresh();
    },
  });

  const removeMutation = useMutation({
    mutationFn: (taskStaffId: string) => removePreparationTaskExamStaff(taskId!, taskStaffId),
    onSuccess: () => {
      message.success('任务考务人员已删除');
      refresh();
    },
  });

  const taskStaffColumns: ColumnsType<PreparationTaskExamStaff> = [
    { title: '姓名', dataIndex: 'staffName', width: 140 },
    { title: '联系电话', dataIndex: 'maskedPhone', width: 160, render: dash },
    {
      title: '角色',
      dataIndex: 'taskRole',
      width: 160,
      render: (_, record) => {
        const option = roleOption(record.taskRole);
        return <Tag color={option.color}>{roleLabel(record.taskRole, record.roleName)}</Tag>;
      },
    },
    { title: '备注', dataIndex: 'remark', ellipsis: true, render: dash },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '操作',
      width: 240,
      render: (_, record) => (
        <PermissionGate code="certification.preparation.staff.assign">
          <Space size={8}>
            <Select
              size="small"
              value={record.taskRole || (record.isLeader ? 'internal_supervisor' : 'exam_staff')}
              options={EXAM_STAFF_ROLE_OPTIONS.map(({ value, label }) => ({ value, label }))}
              style={{ width: 116 }}
              loading={updateRoleMutation.isPending}
              onChange={(taskRole) => updateRoleMutation.mutate({ taskStaffId: record.id, taskRole })}
            />
            <Popconfirm title="确认删除该考务人员？" okText="删除" cancelText="取消" onConfirm={() => removeMutation.mutate(record.id)}>
              <Button type="link" danger size="small" icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          </Space>
        </PermissionGate>
      ),
    },
  ];

  const availableStaffOptions = (availableStaffQuery.data || []).map((item) => ({
    label: `${item.staffName}${item.maskedPhone ? `（${item.maskedPhone}）` : ''}`,
    value: item.id,
  }));

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="任务考务人员"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
            <PermissionGate code="certification.preparation.staff.assign">
              <Button
                type="primary"
                icon={<PlusOutlined />}
                disabled={!taskId}
                onClick={() => {
                  assignForm.resetFields();
                  assignForm.setFieldsValue({ taskRole: 'exam_staff' });
                  setAssignModalOpen(true);
                }}
              >
                绑定考务人员
              </Button>
            </PermissionGate>
          </Space>
        }
      >
        {!taskId ? (
          <Typography.Text type="secondary">请选择认定任务后绑定考务人员。</Typography.Text>
        ) : (
          <>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
              考务人员基础信息请在“信息管理 → 考务信息”中维护并审核通过后，再绑定到本任务。
            </Typography.Paragraph>
            <Table
              rowKey="id"
              columns={taskStaffColumns}
              dataSource={taskStaffQuery.data || []}
              loading={taskStaffQuery.isFetching}
              pagination={false}
              scroll={{ x: 900 }}
            />
          </>
        )}
      </ProCard>

      <Modal
        title="绑定考务人员到认定任务"
        open={assignModalOpen}
        onOk={() => assignForm.validateFields().then((values) => assignMutation.mutate(values))}
        onCancel={() => setAssignModalOpen(false)}
        confirmLoading={assignMutation.isPending}
        width={520}
      >
        <Form form={assignForm} layout="vertical" initialValues={{ taskRole: 'exam_staff' }}>
          <Form.Item name="staffId" label="考务人员" rules={[{ required: true, message: '请选择考务人员' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              loading={availableStaffQuery.isFetching}
              placeholder="请选择已审核通过的考务人员"
              options={availableStaffOptions}
              notFoundContent={
                availableStaffQuery.isFetching
                  ? '加载中...'
                  : '暂无可用考务人员，请先在信息管理中添加并审核通过'
              }
            />
          </Form.Item>
          <Form.Item name="taskRole" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select options={EXAM_STAFF_ROLE_OPTIONS.map(({ value, label }) => ({ value, label }))} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="可选，如岗位职责说明" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

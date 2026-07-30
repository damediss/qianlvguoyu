import { CheckCircleOutlined, DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, UserAddOutlined } from '@ant-design/icons';
import { Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  assignCertificationExamSessionCandidates,
  completeCertificationExamSessionCollectionConfig,
  createCertificationExamSession,
  deleteCertificationExamSession,
  listCertificationExamSessionCandidates,
  listCertificationExamSessions,
  updateCertificationExamSession,
} from '../../../api/certificationPreparation';
import { PermissionGate } from '../../../directives/permission';
import type {
  CertificationExamSession,
  CertificationExamSessionCandidate,
  CertificationExamSessionPayload,
  CertificationExamType,
} from '../../../types/certificationPreparation';

interface Props {
  taskId?: string;
  onChanged?: () => void;
}

interface SessionFormValues {
  examType: CertificationExamType;
  sessionName: string;
  examDate?: Dayjs;
  startTime?: Dayjs;
  endTime?: Dayjs;
  status?: CertificationExamSession['status'];
  remark?: string;
}

const statusLabels: Record<CertificationExamSession['status'], string> = {
  DRAFT: '草稿',
  CONFIGURED: '已配置',
  IN_PROGRESS: '进行中',
  ENDED: '已结束',
};

const participationStatusLabels: Record<string, string> = {
  UNASSIGNED: '未分配',
  ASSIGNED: '参加',
  CANCELLED: '取消',
  EXEMPT: '免考',
};

export function ExamSessionConfig({ taskId, onChanged }: Props) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<CertificationExamSession>();
  const [candidateSession, setCandidateSession] = useState<CertificationExamSession>();
  const [selectedCandidateIds, setSelectedCandidateIds] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<SessionFormValues>();
  const sessionsQuery = useQuery({
    queryKey: ['certification-exam-sessions', taskId],
    queryFn: () => listCertificationExamSessions(taskId!),
    enabled: Boolean(taskId),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-exam-sessions', taskId] });
    onChanged?.();
  };
  const saveMutation = useMutation({
    mutationFn: (payload: CertificationExamSessionPayload) =>
      editing ? updateCertificationExamSession(editing.id, payload) : createCertificationExamSession(taskId!, payload),
    onSuccess: () => {
      message.success(editing ? '考试场次已更新' : '考试场次已创建');
      setOpen(false);
      invalidate();
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (sessionId: string) => deleteCertificationExamSession(sessionId),
    onSuccess: () => {
      message.success('考试场次已删除');
      invalidate();
    },
  });
  const candidatesQuery = useQuery({
    queryKey: ['certification-exam-session-candidates', candidateSession?.id],
    queryFn: () => listCertificationExamSessionCandidates(candidateSession!.id),
    enabled: Boolean(candidateSession),
  });
  const assignCandidatesMutation = useMutation({
    mutationFn: () => assignCertificationExamSessionCandidates(candidateSession!.id, {
      candidateIds: selectedCandidateIds,
    }),
    onSuccess: () => {
      message.success('场次考生名单已保存');
      queryClient.invalidateQueries({ queryKey: ['certification-exam-session-candidates', candidateSession?.id] });
      setCandidateSession(undefined);
      invalidate();
    },
  });
  const completeConfigMutation = useMutation({
    mutationFn: (sessionId: string) => completeCertificationExamSessionCollectionConfig(sessionId),
    onSuccess: () => {
      message.success('采集配置已完成，信息采集任务已生成');
      invalidate();
    },
  });

  useEffect(() => {
    const assigned = (candidatesQuery.data || [])
      .filter((candidate) => candidate.participationStatus === 'ASSIGNED')
      .map((candidate) => candidate.candidateId);
    setSelectedCandidateIds(assigned);
  }, [candidatesQuery.data]);

  const openCreate = () => {
    setEditing(undefined);
    form.resetFields();
    form.setFieldsValue({
      examType: 'THEORY',
      status: 'DRAFT',
    });
    setOpen(true);
  };
  const openEdit = (record: CertificationExamSession) => {
    setEditing(record);
    form.setFieldsValue({
      examType: record.examType,
      sessionName: record.sessionName,
      examDate: record.examDate ? dayjs(record.examDate) : undefined,
      startTime: record.startTime ? dayjs(record.startTime) : undefined,
      endTime: record.endTime ? dayjs(record.endTime) : undefined,
      status: record.status,
      remark: record.remark,
    });
    setOpen(true);
  };
  const submit = async () => {
    const values = await form.validateFields();
    saveMutation.mutate({
      ...values,
      examDate: values.examDate?.format('YYYY-MM-DD'),
      startTime: values.startTime?.format('YYYY-MM-DDTHH:mm:ss'),
      endTime: values.endTime?.format('YYYY-MM-DDTHH:mm:ss'),
      status: values.status || 'DRAFT',
      identityCheckRequired: true,
      fingerprintRequired: true,
      signatureRequired: true,
    });
  };

  const columns: ColumnsType<CertificationExamSession> = [
    { title: '场次名称', dataIndex: 'sessionName', width: 220 },
    {
      title: '考试类型',
      dataIndex: 'examType',
      width: 100,
      render: (value: CertificationExamType) => value === 'THEORY' ? <Tag color="blue">理论</Tag> : <Tag color="orange">实操</Tag>,
    },
    { title: '考试日期', dataIndex: 'examDate', width: 120, render: (value?: string) => value || '-' },
    { title: '开始时间', dataIndex: 'startTime', width: 170, render: (value?: string) => value || '-' },
    { title: '结束时间', dataIndex: 'endTime', width: 170, render: (value?: string) => value || '-' },
    {
      title: '业务环节',
      width: 220,
      render: (_, record) => record.examType === 'PRACTICE' ? '信息采集 + 实操评分' : '信息采集',
    },
    { title: '状态', dataIndex: 'status', width: 100, render: (value: CertificationExamSession['status']) => statusLabels[value] || value },
    {
      title: '操作',
      width: 156,
      align: 'center',
      render: (_, record) => (
        <Space size={2}>
          <PermissionGate code="certification.preparation.candidate.exam-session.edit">
            <Button
              type="link"
              size="small"
              title="设置考生"
              aria-label="设置考生"
              icon={<UserAddOutlined />}
              onClick={() => {
                setSelectedCandidateIds([]);
                setCandidateSession(record);
              }}
              style={{ paddingInline: 5 }}
            />
          </PermissionGate>
          <PermissionGate code="certification.preparation.exam-session.edit">
            <Popconfirm
              title="完成采集配置后，该场次将出现在信息采集任务列表中，确认继续？"
              onConfirm={() => completeConfigMutation.mutate(record.id)}
              disabled={record.status === 'CONFIGURED'}
            >
              <Button
                type="link"
                size="small"
                title={record.status === 'CONFIGURED' ? '已完成配置' : '完成采集配置'}
                aria-label={record.status === 'CONFIGURED' ? '已完成配置' : '完成采集配置'}
                icon={<CheckCircleOutlined />}
                disabled={record.status === 'CONFIGURED'}
                loading={completeConfigMutation.isPending && completeConfigMutation.variables === record.id}
                style={{ paddingInline: 5 }}
              />
            </Popconfirm>
          </PermissionGate>
          <PermissionGate code="certification.preparation.exam-session.edit">
            <Button
              type="link"
              size="small"
              title="编辑"
              aria-label="编辑"
              icon={<EditOutlined />}
              onClick={() => openEdit(record)}
              style={{ paddingInline: 5 }}
            />
          </PermissionGate>
          <PermissionGate code="certification.preparation.exam-session.delete">
            <Popconfirm title="确定删除该考试场次？" onConfirm={() => deleteMutation.mutate(record.id)}>
              <Button
                type="link"
                danger
                size="small"
                title="删除"
                aria-label="删除"
                icon={<DeleteOutlined />}
                style={{ paddingInline: 5 }}
              />
            </Popconfirm>
          </PermissionGate>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="考试场次"
      extra={(
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => sessionsQuery.refetch()}>刷新</Button>
          <PermissionGate code="certification.preparation.exam-session.edit">
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate} disabled={!taskId}>新增场次</Button>
          </PermissionGate>
        </Space>
      )}
    >
      <Table<CertificationExamSession>
        rowKey="id"
        loading={sessionsQuery.isLoading}
        dataSource={sessionsQuery.data || []}
        columns={columns}
        pagination={false}
        scroll={{ x: 1100 }}
        locale={{ emptyText: taskId ? '暂无考试场次，请先新增理论或实操场次' : '请先选择认定任务' }}
      />
      <Modal
        title={editing ? '编辑考试场次' : '新增考试场次'}
        open={open}
        onOk={submit}
        onCancel={() => setOpen(false)}
        confirmLoading={saveMutation.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="examType" label="考试类型" rules={[{ required: true }]}>
            <Select options={[{ label: '理论', value: 'THEORY' }, { label: '实操', value: 'PRACTICE' }]} />
          </Form.Item>
          <Form.Item name="sessionName" label="场次名称" rules={[{ required: true, message: '请输入场次名称' }]}>
            <Input placeholder="如：理论考试第一场" />
          </Form.Item>
          <Form.Item name="examDate" label="考试日期">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="startTime" label="开始时间"><DatePicker showTime={{ format: 'HH:mm:ss' }} format="YYYY-MM-DD HH:mm:ss" style={{ width: '100%' }} /></Form.Item>
          <Form.Item
            name="endTime"
            label="结束时间"
            dependencies={['startTime']}
            rules={[{
              validator: async (_, value?: Dayjs) => {
                const startTime = form.getFieldValue('startTime') as Dayjs | undefined;
                if (!value || !startTime || value.isAfter(startTime)) {
                  return;
                }
                throw new Error('结束时间必须晚于开始时间');
              },
            }]}
          >
            <DatePicker
              showTime={{ format: 'HH:mm:ss' }}
              format="YYYY-MM-DD HH:mm:ss"
              style={{ width: '100%' }}
              disabledDate={(current) => {
                const startTime = form.getFieldValue('startTime') as Dayjs | undefined;
                return Boolean(startTime && current && current.isBefore(startTime, 'day'));
              }}
            />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={Object.entries(statusLabels).map(([value, label]) => ({ value, label }))} />
          </Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
      <Modal
        title={candidateSession ? `设置考生名单：${candidateSession.sessionName}` : '设置考生名单'}
        open={Boolean(candidateSession)}
        onOk={() => assignCandidatesMutation.mutate()}
        onCancel={() => setCandidateSession(undefined)}
        confirmLoading={assignCandidatesMutation.isPending}
        width={780}
        destroyOnClose
      >
        <Table<CertificationExamSessionCandidate>
          rowKey="candidateId"
          size="small"
          loading={candidatesQuery.isLoading}
          dataSource={candidatesQuery.data || []}
          rowSelection={{
            selectedRowKeys: selectedCandidateIds,
            onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
          }}
          pagination={false}
          scroll={{ y: 420 }}
          columns={[
            { title: '姓名', dataIndex: 'candidateName', width: 140 },
            { title: '身份证号', dataIndex: 'idCardNo', width: 220 },
            { title: '准考证号', dataIndex: 'admissionTicketNo', width: 150, render: (value?: string) => value || '-' },
            {
              title: '参加状态',
              dataIndex: 'participationStatus',
              render: (value: string) => participationStatusLabels[value] || value || '-',
            },
          ]}
          locale={{ emptyText: '当前场次暂无可配置考生，请先导入考生名单' }}
        />
        <Typography.Text type="secondary">已选择 {selectedCandidateIds.length} 名考生</Typography.Text>
      </Modal>
    </Card>
  );
}

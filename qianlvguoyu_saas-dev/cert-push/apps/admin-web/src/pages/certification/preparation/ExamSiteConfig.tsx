import { DeleteOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, TimePicker, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { useState } from 'react';
import {
  assignPreparationExamSite,
  listPreparationTaskExamStaff,
  listPreparationTaskExamSites,
  removePreparationTaskExamSite,
} from '../../../api/certificationPreparation';
import { listAvailableExamSites } from '../../../api/certificationInfo';
import { PermissionGate } from '../../../directives/permission';
import type { ExamSiteInfo } from '../../../types/certificationInfo';
import type { PreparationTaskExamSite } from '../../../types/certificationPreparation';
import { dash, dateText, formatDate, formGridStyle, statusTag } from './shared';

interface ExamSiteConfigProps {
  taskId?: string;
  onChanged?: () => void;
}

interface AssignFormValues {
  siteId: string;
  examDate?: Dayjs;
  timeRange?: [Dayjs, Dayjs];
  examType?: string;
  arrangedCount?: number;
  taskStaffId?: string;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
}

const examTypeOptions = [
  { label: '理论', value: 'theory' },
  { label: '实操', value: 'practical' },
];

function examTypeTag(type?: string) {
  if (type === 'practical') {
    return <Tag color="green">实操</Tag>;
  }
  if (type === 'theory') {
    return <Tag color="blue">理论</Tag>;
  }
  return dash(type);
}

export function ExamSiteConfig({ taskId, onChanged }: ExamSiteConfigProps) {
  const queryClient = useQueryClient();
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [assignForm] = Form.useForm<AssignFormValues>();

  const availableSitesQuery = useQuery({
    queryKey: ['certification-info-sites-available'],
    queryFn: () => listAvailableExamSites(),
    enabled: assignModalOpen,
  });

  const taskSitesQuery = useQuery({
    queryKey: ['certification-preparation-task-sites', taskId],
    queryFn: () => listPreparationTaskExamSites(taskId!),
    enabled: Boolean(taskId),
  });

  const taskStaffQuery = useQuery({
    queryKey: ['certification-preparation-task-exam-staff', taskId],
    queryFn: () => listPreparationTaskExamStaff(taskId!),
    enabled: Boolean(taskId && assignModalOpen),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-sites', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-exam-staff', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-info-sites-available'] });
    onChanged?.();
  };

  const assignMutation = useMutation({
    mutationFn: (values: AssignFormValues) => assignPreparationExamSite(taskId!, {
      siteId: values.siteId,
      examDate: formatDate(values.examDate),
      startTime: formatTime(values.timeRange?.[0]),
      endTime: formatTime(values.timeRange?.[1]),
      examType: values.examType,
      arrangedCount: values.arrangedCount,
      contactName: values.contactName,
      contactPhone: values.contactPhone,
      remark: values.remark,
    }),
    onSuccess: () => {
      message.success('考场已绑定到任务');
      setAssignModalOpen(false);
      assignForm.resetFields();
      refresh();
    },
  });

  const removeMutation = useMutation({
    mutationFn: (taskSiteId: string) => removePreparationTaskExamSite(taskId!, taskSiteId),
    onSuccess: () => {
      message.success('任务考场已移除');
      refresh();
    },
  });

  const taskSiteColumns: ColumnsType<PreparationTaskExamSite> = [
    { title: '考场', dataIndex: 'siteName', width: 180 },
    { title: '考试日期', dataIndex: 'examDate', width: 120, render: dateText },
    { title: '时间', width: 140, render: (_, row) => `${row.startTime || '-'} - ${row.endTime || '-'}` },
    { title: '考试类型', dataIndex: 'examType', width: 100, render: examTypeTag },
    { title: '安排人数', dataIndex: 'arrangedCount', width: 100, render: dash },
    { title: '考位', dataIndex: 'seatCount', width: 90, render: dash },
    { title: '负责人', dataIndex: 'contactName', width: 110, render: dash },
    { title: '联系电话', dataIndex: 'contactPhone', width: 140, render: dash },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '操作',
      width: 110,
      render: (_, record) => (
        <PermissionGate code="certification.preparation.site.assign">
          <Popconfirm title="确认移除该任务考场？" okText="移除" cancelText="取消" onConfirm={() => removeMutation.mutate(record.id)}>
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>移除</Button>
          </Popconfirm>
        </PermissionGate>
      ),
    },
  ];

  function openAssign(site?: ExamSiteInfo) {
    assignForm.resetFields();
    assignForm.setFieldsValue({
      siteId: site?.id,
      arrangedCount: site?.seatCount,
      examType: site?.siteType === 'practical' ? 'practical' : 'theory',
    });
    setAssignModalOpen(true);
  }

  function onTaskStaffChange(taskStaffId: string) {
    const staff = (taskStaffQuery.data || []).find((item) => item.id === taskStaffId);
    assignForm.setFieldsValue({
      contactName: staff?.staffName,
      contactPhone: staff?.phone || staff?.maskedPhone,
    });
  }

  function submitAssign() {
    assignForm.validateFields().then((values) => {
      const staff = (taskStaffQuery.data || []).find((item) => item.id === values.taskStaffId);
      if (!staff) {
        assignForm.setFields([{ name: 'taskStaffId', errors: ['请从本任务已配置的考务人员中选择'] }]);
        return;
      }
      values.contactName = staff.staffName;
      values.contactPhone = staff.phone || staff.maskedPhone;
      const site = (availableSitesQuery.data || []).find((item) => item.id === values.siteId);
      if (site?.seatCount !== undefined && values.arrangedCount !== undefined && values.arrangedCount > site.seatCount) {
        assignForm.setFields([{ name: 'arrangedCount', errors: ['安排人数不得超过考场考位数量'] }]);
        return;
      }
      const [startTime, endTime] = values.timeRange || [];
      if (startTime && endTime && !endTime.isAfter(startTime)) {
        assignForm.setFields([{ name: 'timeRange', errors: ['结束时间须晚于开始时间'] }]);
        return;
      }
      assignMutation.mutate(values);
    });
  }

  const availableSiteOptions = (availableSitesQuery.data || []).map((item) => ({
    label: `${item.siteName}（${item.seatCount || 0} 位）`,
    value: item.id,
  }));

  const taskStaffOptions = (taskStaffQuery.data || []).map((item) => ({
    label: `${item.staffName}${item.maskedPhone ? `（${item.maskedPhone}）` : ''}`,
    value: item.id,
  }));

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="任务考场"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
            <PermissionGate code="certification.preparation.site.assign">
              <Button type="primary" icon={<PlusOutlined />} disabled={!taskId} onClick={() => openAssign()}>
                绑定考场
              </Button>
            </PermissionGate>
          </Space>
        }
      >
        {!taskId ? (
          <Typography.Text type="secondary">请选择认定任务后绑定考场。</Typography.Text>
        ) : (
          <>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
              考场基础信息请在“信息管理 → 考场信息”中维护并审核通过后，再绑定到本任务。
            </Typography.Paragraph>
            <Table
              rowKey="id"
              columns={taskSiteColumns}
              dataSource={taskSitesQuery.data || []}
              loading={taskSitesQuery.isFetching}
              pagination={false}
              scroll={{ x: 1080 }}
            />
          </>
        )}
      </ProCard>

      <Modal
        title="绑定考场到认定任务"
        open={assignModalOpen}
        onOk={submitAssign}
        onCancel={() => setAssignModalOpen(false)}
        confirmLoading={assignMutation.isPending}
        width={680}
      >
        <Form form={assignForm} layout="vertical" initialValues={{ examType: 'theory' }}>
          <Form.Item name="siteId" label="考场" rules={[{ required: true, message: '请选择考场' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              loading={availableSitesQuery.isFetching}
              placeholder="请选择已审核通过的考场"
              options={availableSiteOptions}
              notFoundContent={
                availableSitesQuery.isFetching
                  ? '加载中...'
                  : '暂无可用考场，请先在信息管理中添加并审核通过'
              }
            />
          </Form.Item>
          <div style={formGridStyle}>
            <Form.Item name="examDate" label="考试日期">
              <DatePicker style={{ width: '100%' }} disabledDate={(current) => current && current < dayjs().startOf('day')} />
            </Form.Item>
            <Form.Item name="arrangedCount" label="安排人数" rules={[{ type: 'number', min: 1, message: '请输入有效人数' }]}>
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="timeRange" label="考试时间">
              <TimePicker.RangePicker
                format="HH:mm"
                minuteStep={5}
                style={{ width: '100%' }}
                placeholder={['开始时间', '结束时间']}
              />
            </Form.Item>
            <Space.Compact style={{ width: '100%' }}>
              <Form.Item
                name="taskStaffId"
                label="考试负责人"
                rules={[{ required: true, message: '请从本任务考务人员中选择' }]}
                style={{ width: '50%', marginBottom: 0 }}
              >
                <Select
                  showSearch
                  optionFilterProp="label"
                  loading={taskStaffQuery.isFetching}
                  placeholder="请选择负责人"
                  options={taskStaffOptions}
                  onChange={onTaskStaffChange}
                  notFoundContent={
                    taskStaffQuery.isFetching
                      ? '加载中...'
                      : '请先在“考务配置”中绑定考务人员'
                  }
                />
              </Form.Item>
              <Form.Item
                name="examType"
                label="考试类型"
                rules={[{ required: true, message: '请选择考试类型' }]}
                style={{ width: '50%', marginBottom: 0 }}
              >
                <Select options={examTypeOptions} />
              </Form.Item>
            </Space.Compact>
            <Form.Item name="contactName" hidden>
              <Input />
            </Form.Item>
            <Form.Item name="contactPhone" hidden>
              <Input />
            </Form.Item>
          </div>
          <Form.Item name="remark" label="备注">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

function formatTime(value?: Dayjs) {
  return value?.format('HH:mm');
}

import { CheckCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Empty, Form, Progress, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import type { Key } from 'react';
import {
  getPreparationChecklist,
  listPreparationTaskExamStaff,
  updatePreparationChecklist,
} from '../../../api/certificationPreparation';
import { PermissionGuard } from '../../../directives/permission';
import type { PreparationChecklistItem } from '../../../types/certificationPreparation';
import { dateTimeText } from './shared';

interface ChecklistReviewProps {
  taskId?: string;
  onChanged?: () => void;
}

interface ConfirmFormValues {
  confirmerTaskStaffId?: string;
}

export function ChecklistReview({ taskId, onChanged }: ChecklistReviewProps) {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<ConfirmFormValues>();
  const [checkedItemIds, setCheckedItemIds] = useState<Key[]>([]);

  const checklistQuery = useQuery({
    queryKey: ['certification-preparation-checklist', taskId],
    queryFn: () => getPreparationChecklist(taskId!),
    enabled: Boolean(taskId),
  });

  const staffQuery = useQuery({
    queryKey: ['certification-preparation-task-exam-staff', taskId],
    queryFn: () => listPreparationTaskExamStaff(taskId!),
    enabled: Boolean(taskId),
  });

  const checklist = checklistQuery.data;
  const items = checklist?.items || [];
  const allChecked = items.length > 0 && checkedItemIds.length === items.length;

  useEffect(() => {
    if (!checklist) {
      setCheckedItemIds([]);
      form.resetFields();
      return;
    }
    setCheckedItemIds(checklist.items.filter((item) => item.checked).map((item) => item.id));
    form.setFieldsValue({ confirmerTaskStaffId: checklist.confirmerTaskStaffId });
  }, [checklist, form]);

  const staffOptions = useMemo(
    () => (staffQuery.data || []).map((staff) => ({
      label: `${staff.staffName}${staff.roleName ? ` / ${staff.roleName}` : ''}`,
      value: staff.id,
    })),
    [staffQuery.data],
  );

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-checklist', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-exam-staff', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    onChanged?.();
  };

  const confirmMutation = useMutation({
    mutationFn: (values: ConfirmFormValues) => updatePreparationChecklist(taskId!, {
      checkedItemIds: checkedItemIds.map(String),
      confirmerTaskStaffId: values.confirmerTaskStaffId,
    }),
    onSuccess: () => {
      message.success('清单核查已确认');
      refresh();
    },
  });

  const columns: ColumnsType<PreparationChecklistItem> = [
    { title: '序号', dataIndex: 'itemNo', width: 80 },
    { title: '名称', dataIndex: 'itemName', minWidth: 260, ellipsis: true },
    { title: '数量', dataIndex: 'quantityText', width: 120, render: (value) => value || '-' },
    {
      title: '核查状态',
      dataIndex: 'checked',
      width: 120,
      render: (value: boolean) => value ? <Tag color="success">已核</Tag> : <Tag>待核</Tag>,
    },
  ];

  if (!taskId) {
    return <Empty description="请选择认定任务后进行清单核查" />;
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="清单核查"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
            <Button disabled={!items.length} onClick={() => setCheckedItemIds(items.map((item) => item.id))}>
              全部勾选
            </Button>
          </Space>
        }
      >
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <div className="knowledge-overview">
            <ProCard className="overview-card">
              <Typography.Text type="secondary">材料总数</Typography.Text>
              <Typography.Title level={3} style={{ margin: 0 }}>{checklist?.totalCount || 0}</Typography.Title>
            </ProCard>
            <ProCard className="overview-card">
              <Typography.Text type="secondary">已核材料</Typography.Text>
              <Typography.Title level={3} style={{ margin: 0 }}>{checkedItemIds.length}</Typography.Title>
            </ProCard>
            <ProCard className="overview-card">
              <Typography.Text type="secondary">确认人</Typography.Text>
              <Typography.Title level={5} style={{ margin: '6px 0 0' }}>{checklist?.confirmerName || '-'}</Typography.Title>
            </ProCard>
            <ProCard className="overview-card">
              <Typography.Text type="secondary">确认时间</Typography.Text>
              <Typography.Title level={5} style={{ margin: '6px 0 0' }}>{dateTimeText(checklist?.confirmedAt)}</Typography.Title>
            </ProCard>
          </div>

          <Progress percent={items.length ? Math.round((checkedItemIds.length * 100) / items.length) : 0} />
          {!allChecked ? (
            <Alert
              type="warning"
              showIcon
              message="全部材料勾选完毕后，才可以选择确认人并完成清单核查。"
            />
          ) : null}

          <Table
            rowKey="id"
            columns={columns}
            dataSource={items}
            loading={checklistQuery.isFetching}
            pagination={false}
            rowSelection={{
              selectedRowKeys: checkedItemIds,
              onChange: setCheckedItemIds,
            }}
            scroll={{ x: 720, y: 520 }}
          />
        </Space>
      </ProCard>

      <ProCard title="核查确认">
        <Form form={form} layout="inline" onFinish={(values) => confirmMutation.mutate(values)}>
          <Form.Item
            name="confirmerTaskStaffId"
            label="确认人"
            rules={[{ required: true, message: '请选择确认人' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="请选择任务考务人员"
              style={{ width: 260 }}
              loading={staffQuery.isFetching}
              options={staffOptions}
              disabled={!allChecked}
            />
          </Form.Item>
          <PermissionGuard codes={['certification.preparation.checklist.confirm']}>
            <Button
              type="primary"
              icon={<CheckCircleOutlined />}
              htmlType="submit"
              loading={confirmMutation.isPending}
              disabled={!allChecked}
            >
              确认清单
            </Button>
          </PermissionGuard>
        </Form>
      </ProCard>
    </Space>
  );
}

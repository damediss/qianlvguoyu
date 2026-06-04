import { CheckOutlined, CloseCircleOutlined, EditOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Drawer, Form, Input, InputNumber, Select, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import {
  getPreparationAssessorTask,
  pagePreparationAssessorTasks,
  reviewPreparationAssessorTask,
} from '../../../api/certificationPreparation';
import { FilePreviewModal, type FilePreviewTarget } from '../../../components/FilePreviewModal';
import { PermissionGate } from '../../../directives/permission';
import { hasPermission } from '../../../directives/permission';
import type {
  PreparationAssessorTask,
  PreparationAssessorTaskReviewPayload,
  PreparationAssessorWorkbenchQuery,
} from '../../../types/certificationPreparation';
import { dash, dateText, dateTimeText, statusTag } from './shared';

export function AssessorWorkbench() {
  const queryClient = useQueryClient();
  const canReview = hasPermission('certification.preparation.assessor.review');
  const [query, setQuery] = useState<PreparationAssessorWorkbenchQuery>({
    page: 1,
    pageSize: 10,
    view: canReview ? 'leader' : 'assessor',
  });
  const [detailTaskId, setDetailTaskId] = useState<string>();
  const [reviewForm] = Form.useForm<PreparationAssessorTaskReviewPayload>();
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);

  const tasksQuery = useQuery({
    queryKey: ['certification-preparation-assessor-workbench', query],
    queryFn: () => pagePreparationAssessorTasks(query),
  });

  const detailQuery = useQuery({
    queryKey: ['certification-preparation-assessor-workbench-detail', detailTaskId],
    queryFn: () => getPreparationAssessorTask(detailTaskId!),
    enabled: Boolean(detailTaskId),
  });

  useEffect(() => {
    const detail = detailQuery.data;
    if (!detail) {
      return;
    }
    reviewForm.setFieldsValue({
      reviewerAssessorId: detail.leaderAssessorId,
      reviewNote: detail.reviewNote,
    });
  }, [detailQuery.data, reviewForm]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-workbench'] });
    if (detailTaskId) {
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-workbench-detail', detailTaskId] });
    }
  };

  const reviewMutation = useMutation({
    mutationFn: (payload: PreparationAssessorTaskReviewPayload) => reviewPreparationAssessorTask(detailTaskId!, payload),
    onSuccess: () => {
      message.success('审批结果已保存');
      refresh();
    },
  });

  const columns: ColumnsType<PreparationAssessorTask> = [
    { title: '科目', dataIndex: 'subject', width: 130, fixed: 'left', render: dash },
    { title: '认定任务', dataIndex: 'taskName', width: 220, render: (_, row) => row.taskName || row.taskId || '-' },
    { title: '考评组', dataIndex: 'groupName', width: 160, render: (_, row) => row.groupName || row.groupId || '-' },
    { title: '考评员', dataIndex: 'assessorName', width: 120, render: (_, row) => row.assessorName || row.assessorId || '-' },
    { title: '时长', dataIndex: 'durationMin', width: 90, render: (value) => value ? `${value} 分钟` : '-' },
    { title: '截止日期', dataIndex: 'dueDate', width: 120, render: dateText },
    { title: '状态', dataIndex: 'status', width: 110, render: statusTag },
    { title: '总分', dataIndex: 'totalScore', width: 90, render: dash },
    { title: '提交时间', dataIndex: 'submittedAt', width: 170, render: dateTimeText },
    {
      title: '操作',
      width: 110,
      fixed: 'right',
      render: (_, record) => (
        <Button type="link" size="small" icon={<EditOutlined />} onClick={() => setDetailTaskId(record.id)}>
          {query.view === 'leader' ? '审批' : '查看'}
        </Button>
      ),
    },
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="考评员工作台"
        extra={<Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>}
      >
        <Form layout="inline" style={{ marginBottom: 16 }} onFinish={(values) => setQuery({ ...query, ...values, page: 1 })}>
          <Form.Item name="assessorId">
            <InputNumber min={1} placeholder="考评员ID" style={{ width: 150 }} />
          </Form.Item>
          {canReview ? (
            <Form.Item name="view" initialValue={query.view}>
              <Select
                style={{ width: 150 }}
                options={[
                  { label: '我的任务', value: 'assessor' },
                  { label: '组长审批', value: 'leader' },
                ]}
              />
            </Form.Item>
          ) : null}
          <Form.Item name="status">
            <Select
              allowClear
              placeholder="状态"
              style={{ width: 150 }}
              options={[
                { label: '已发放', value: 'dispatched' },
                { label: '填写中', value: 'scoring' },
                { label: '已提交', value: 'submitted' },
                { label: '已复核', value: 'reviewed' },
                { label: '需整改', value: 'returned' },
              ]}
            />
          </Form.Item>
          <Button htmlType="submit">查询</Button>
        </Form>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={tasksQuery.data?.items || []}
          loading={tasksQuery.isFetching}
          scroll={{ x: 1280 }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: tasksQuery.data?.total || 0,
            showSizeChanger: true,
            onChange: (page, pageSize) => setQuery({ ...query, page, pageSize }),
          }}
        />
      </ProCard>

      <Drawer
        title={query.view === 'leader' ? '任务审批' : '任务详情'}
        width={820}
        open={Boolean(detailTaskId)}
        onClose={() => setDetailTaskId(undefined)}
        extra={detailQuery.data ? statusTag(detailQuery.data.status) : null}
      >
        {detailQuery.data ? (
          <Space direction="vertical" style={{ width: '100%' }} size={18}>
            <ProCard title="任务信息">
              <Typography.Paragraph>
                <Typography.Text strong>{detailQuery.data.subject || '-'}</Typography.Text>
                <Typography.Text style={{ marginLeft: 8 }}>
                  {displayAssessmentName(detailQuery.data.assessmentFileName || detailQuery.data.title) || '-'}
                </Typography.Text>
              </Typography.Paragraph>
              <Space wrap>
                <Typography.Text type="secondary">认定任务：{detailQuery.data.taskName || detailQuery.data.taskId}</Typography.Text>
                <Typography.Text type="secondary">考评组：{detailQuery.data.groupName || detailQuery.data.groupId}</Typography.Text>
                <Typography.Text type="secondary">考评员：{detailQuery.data.assessorName || detailQuery.data.assessorId}</Typography.Text>
                <Typography.Text type="secondary">截止：{dateText(detailQuery.data.dueDate)}</Typography.Text>
                <Typography.Text type="secondary">总分：{detailQuery.data.totalScore ?? '-'}</Typography.Text>
              </Space>
            </ProCard>

            <ProCard
              title="考评表"
            >
              <Typography.Paragraph style={{ marginBottom: 0 }}>
                {detailQuery.data.assessmentFileId ? (
                  <Button
                    type="link"
                    size="small"
                    style={{ paddingInline: 0 }}
                    onClick={() => setPreviewTarget({
                      fileId: detailQuery.data!.assessmentFileId,
                      fileName: detailQuery.data!.assessmentFileName || detailQuery.data!.title,
                    })}
                  >
                    详情
                  </Button>
                ) : (
                  <Typography.Text type="secondary">-</Typography.Text>
                )}
              </Typography.Paragraph>
            </ProCard>

            {query.view === 'leader' ? (
              <ProCard
                title="审批"
                extra={
                  <PermissionGate code="certification.preparation.assessor.review">
                    <Space>
                      <Button
                        danger
                        icon={<CloseCircleOutlined />}
                        loading={reviewMutation.isPending}
                        onClick={() => {
                          reviewForm.validateFields().then((values) => {
                            if (!values.reviewNote?.trim()) {
                              message.warning('驳回请填写审批意见');
                              return;
                            }
                            reviewMutation.mutate({ ...values, reviewStatus: 'returned' });
                          });
                        }}
                      >
                        驳回
                      </Button>
                      <Button
                        type="primary"
                        icon={<CheckOutlined />}
                        loading={reviewMutation.isPending}
                        onClick={() => reviewForm.validateFields().then((values) => reviewMutation.mutate({ ...values, reviewStatus: 'reviewed' }))}
                      >
                        审批合格
                      </Button>
                    </Space>
                  </PermissionGate>
                }
              >
                <Form form={reviewForm} layout="vertical">
                  <Form.Item name="reviewerAssessorId" hidden>
                    <InputNumber />
                  </Form.Item>
                  <Form.Item name="reviewNote" label="审批意见">
                    <Input.TextArea rows={3} placeholder="可填写审批意见；驳回时必填" />
                  </Form.Item>
                </Form>
              </ProCard>
            ) : null}
          </Space>
        ) : (
          <Typography.Text type="secondary">正在加载任务详情...</Typography.Text>
        )}
      </Drawer>

      <FilePreviewModal open={Boolean(previewTarget)} target={previewTarget} onClose={() => setPreviewTarget(null)} />
    </Space>
  );
}

function displayAssessmentName(name?: string) {
  if (!name) {
    return '';
  }
  // 仅优化展示：不显示 .pdf/.docx 等后缀
  return name.replace(/\.(pdf|docx?|xlsx?)$/i, '');
}

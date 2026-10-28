import React, { useState } from 'react';
import {
  Badge,
  Button,
  Card,
  Empty,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import {
  pageObjectiveAnswerBatches,
  pauseObjectiveAnswerBatch,
  resumeObjectiveAnswerBatch,
  retryFailedObjectiveAnswerItems,
} from './objectiveAnswerCheckApi';
import type { ObjectiveAnswerBatch } from './objectiveAnswerCheckTypes';
import {
  BATCH_STATUS_COLORS,
  BATCH_STATUS_LABELS,
  extractErrorMessage,
  formatNumber,
  progressPercent,
} from './objectiveAnswerCheckUtils';

/** 检查批次历史（方案 §5.7）：不提供删除，历史永久保留。 */
const ObjectiveAnswerCheckBatches: React.FC<{ onOpenResults: (batchId: string) => void }> = ({
  onOpenResults,
}) => {
  const paneActive = usePaneActive();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const batches = useQuery({
    queryKey: ['oac', 'batches', page, pageSize],
    queryFn: () => pageObjectiveAnswerBatches(page, pageSize),
    placeholderData: keepPreviousData,
    refetchInterval: paneActive ? 5000 : false,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['oac', 'batches'] });
  };

  const pauseMutation = useMutation({
    mutationFn: pauseObjectiveAnswerBatch,
    onSuccess: () => {
      message.success('已请求暂停');
      invalidate();
    },
    onError: (error) => message.error(extractErrorMessage(error, '暂停失败')),
  });

  const resumeMutation = useMutation({
    mutationFn: resumeObjectiveAnswerBatch,
    onSuccess: () => {
      message.success('已继续检查');
      invalidate();
    },
    onError: (error) => message.error(extractErrorMessage(error, '继续失败')),
  });

  const retryMutation = useMutation({
    mutationFn: retryFailedObjectiveAnswerItems,
    onSuccess: (data) => {
      message.success(`已复位 ${data.resetCount} 道失败试题`);
      invalidate();
    },
    onError: (error) => message.error(extractErrorMessage(error, '重试失败项失败')),
  });

  const columns: ColumnsType<ObjectiveAnswerBatch> = [
    { title: '批次 ID', dataIndex: 'id', width: 200, ellipsis: true },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 160,
      render: (value: string) => value?.replace('T', ' '),
    },
    { title: '创建人', dataIndex: 'createdByName', width: 100 },
    { title: '筛选摘要', dataIndex: 'filterSummary', ellipsis: true },
    { title: '模型', dataIndex: 'modelId', width: 130 },
    {
      title: '总数/进度',
      width: 130,
      render: (_, batch) => `${formatNumber(batch.done)}/${formatNumber(batch.total)}（${progressPercent(batch.done, batch.total)}%）`,
    },
    {
      title: '结果分布',
      width: 220,
      render: (_, batch) => (
        <Space size={4} wrap>
          <Tag color="red">不一致 {batch.mismatchHigh}</Tag>
          <Tag color="orange">低置信 {batch.lowConfidence}</Tag>
          <Tag color="green">一致 {batch.consistentHigh}</Tag>
          <Tag>异常 {batch.invalid}</Tag>
          <Tag color="volcano">失败 {batch.failed}</Tag>
        </Space>
      ),
    },
    {
      title: '内容格式',
      width: 130,
      render: (_, batch) => `疑似 ${batch.contentSuspect} / 异常 ${batch.contentInvalid}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => (
        <Badge
          status={BATCH_STATUS_COLORS[status] as never}
          text={BATCH_STATUS_LABELS[status] ?? status}
        />
      ),
    },
    {
      title: '调用/Token',
      width: 170,
      render: (_, batch) => (
        <Typography.Text type="secondary">
          {formatNumber(batch.callCount)} 次 / 精确 {formatNumber(batch.inputTokensExact)}
          {batch.inputTokensEstimated > 0
            ? ` + 估算 ${formatNumber(batch.inputTokensEstimated)}`
            : ''}
        </Typography.Text>
      ),
    },
    {
      title: '操作',
      width: 260,
      fixed: 'right',
      render: (_, batch) => (
        <Space size={4} wrap>
          <Button type="link" size="small" onClick={() => onOpenResults(batch.id)}>
            查看结果
          </Button>
          {batch.canPause && (
            <Button
              type="link"
              size="small"
              onClick={() => pauseMutation.mutate(batch.id)}
            >
              暂停
            </Button>
          )}
          {batch.canResume && (
            <Button
              type="link"
              size="small"
              onClick={() => resumeMutation.mutate(batch.id)}
            >
              继续
            </Button>
          )}
          {batch.canRetryFailed && (
            <Button
              type="link"
              size="small"
              onClick={() =>
                Modal.confirm({
                  title: '重试失败项',
                  content: '将重新检查失败试题，并清除这些行的旧复核标记。继续？',
                  okText: '重试',
                  onOk: () => retryMutation.mutate(batch.id),
                })
              }
            >
              重试失败项
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card size="small">
      <Table<ObjectiveAnswerBatch>
        rowKey="id"
        size="small"
        loading={batches.isFetching}
        columns={columns}
        dataSource={batches.data?.items ?? []}
        scroll={{ x: 1600 }}
        pagination={{
          current: page,
          pageSize,
          total: batches.data?.total ?? 0,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50],
          showTotal: (total) => `共 ${formatNumber(total)} 个批次`,
          onChange: (nextPage, nextSize) => {
            setPage(nextPage);
            setPageSize(nextSize);
          },
        }}
        locale={{ emptyText: <Empty description="还没有检查批次" /> }}
      />
    </Card>
  );
};

export default ObjectiveAnswerCheckBatches;

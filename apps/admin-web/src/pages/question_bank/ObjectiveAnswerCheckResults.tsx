import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Progress,
  Radio,
  Row,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { parseContentDispositionFilename } from '../../utils/download';
import {
  exportObjectiveAnswerBatch,
  getCurrentObjectiveAnswerBatch,
  getObjectiveAnswerBatch,
  getObjectiveAnswerItem,
  pageObjectiveAnswerItems,
  pauseObjectiveAnswerBatch,
  resumeObjectiveAnswerBatch,
  retryFailedObjectiveAnswerItems,
  reviewObjectiveAnswerItem,
} from './objectiveAnswerCheckApi';
import type {
  ObjectiveAnswerBatch,
  ObjectiveAnswerBatchStatus,
  ObjectiveAnswerContentWarning,
  ObjectiveAnswerItem,
  ObjectiveAnswerItemDetail,
  ObjectiveAnswerItemFilter,
  ObjectiveAnswerReviewStatus,
} from './objectiveAnswerCheckTypes';
import {
  BATCH_STATUS_COLORS,
  BATCH_STATUS_LABELS,
  CONTENT_QUALITY_COLORS,
  CONTENT_QUALITY_LABELS,
  RESULT_COLORS,
  RESULT_LABELS,
  RESULT_ORDER,
  REVIEW_LABELS,
  extractErrorMessage,
  formatNumber,
  formatPercent,
  isPollingStatus,
  isTerminalStatus,
  progressPercent,
} from './objectiveAnswerCheckUtils';

const DEFAULT_FILTER: ObjectiveAnswerItemFilter = { needsReview: true };

interface Props {
  batchId?: string;
  onBatchChange: (batchId: string) => void;
}

/** 检查结果页（方案 §5.5 / §5.6）：统计卡、复核队列、详情、人工复核、导出。 */
const ObjectiveAnswerCheckResults: React.FC<Props> = ({ batchId, onBatchChange }) => {
  const paneActive = usePaneActive();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<ObjectiveAnswerItemFilter>(DEFAULT_FILTER);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [detailId, setDetailId] = useState<string>();
  const [reviewTarget, setReviewTarget] = useState<ObjectiveAnswerItem>();
  const [keyword, setKeyword] = useState('');

  const currentQuery = useQuery({
    queryKey: ['oac', 'batches', 'current'],
    queryFn: getCurrentObjectiveAnswerBatch,
    enabled: paneActive && !batchId,
    refetchInterval: paneActive && !batchId ? 5000 : false,
  });

  useEffect(() => {
    if (!batchId && currentQuery.data?.id) {
      onBatchChange(currentQuery.data.id);
    }
  }, [batchId, currentQuery.data?.id, onBatchChange]);

  const batchQuery = useQuery({
    queryKey: ['oac', 'batch', batchId],
    queryFn: () => getObjectiveAnswerBatch(batchId as string),
    enabled: !!batchId,
    refetchInterval:
      paneActive && isPollingStatus(batchQueryStatus(batchId)) ? 3000 : false,
    placeholderData: keepPreviousData,
  });

  function batchQueryStatus(id?: string): ObjectiveAnswerBatchStatus | undefined {
    if (!id) return undefined;
    return queryClient.getQueryData<ObjectiveAnswerBatch>(['oac', 'batch', id])?.status;
  }

  const batch = batchQuery.data;
  const status = batch?.status;
  const previousStatus = useRef<ObjectiveAnswerBatchStatus>();

  useEffect(() => {
    if (previousStatus.current && isPollingStatus(previousStatus.current)
      && isTerminalStatus(status) && batchId) {
      // 离开 pending/running/pausing 后必须主动刷新尾部数据，不能只停轮询
      queryClient.invalidateQueries({ queryKey: ['oac', 'batch', batchId] });
      queryClient.invalidateQueries({ queryKey: ['oac', 'items', batchId] });
      queryClient.invalidateQueries({ queryKey: ['oac', 'batches'] });
    }
    previousStatus.current = status;
  }, [status, batchId, queryClient]);

  const itemsQuery = useQuery({
    queryKey: ['oac', 'items', batchId, filter, page, pageSize],
    queryFn: () => pageObjectiveAnswerItems(batchId as string, filter, page, pageSize),
    enabled: !!batchId,
    placeholderData: keepPreviousData,
  });

  const detailQuery = useQuery({
    queryKey: ['oac', 'item', batchId, detailId],
    queryFn: () => getObjectiveAnswerItem(batchId as string, detailId as string),
    enabled: !!batchId && !!detailId,
  });

  const pauseMutation = useMutation({
    mutationFn: () => pauseObjectiveAnswerBatch(batchId as string),
    onSuccess: () => {
      message.success('已请求暂停，正在等待进行中的试题结束');
      queryClient.invalidateQueries({ queryKey: ['oac', 'batch', batchId] });
    },
    onError: (error) => message.error(extractErrorMessage(error, '暂停失败')),
  });

  const resumeMutation = useMutation({
    mutationFn: () => resumeObjectiveAnswerBatch(batchId as string),
    onSuccess: () => {
      message.success('已继续检查');
      queryClient.invalidateQueries({ queryKey: ['oac', 'batch', batchId] });
      queryClient.invalidateQueries({ queryKey: ['oac', 'items', batchId] });
    },
    onError: (error) => message.error(extractErrorMessage(error, '继续失败')),
  });

  const retryMutation = useMutation({
    mutationFn: () => retryFailedObjectiveAnswerItems(batchId as string),
    onSuccess: (data) => {
      message.success(`已复位 ${data.resetCount} 道失败试题，重新检查中`);
      queryClient.invalidateQueries({ queryKey: ['oac', 'batch', batchId] });
      queryClient.invalidateQueries({ queryKey: ['oac', 'items', batchId] });
    },
    onError: (error) => message.error(extractErrorMessage(error, '重试失败项失败')),
  });

  const reviewMutation = useMutation({
    mutationFn: (payload: {
      itemId: string;
      reviewStatus: ObjectiveAnswerReviewStatus;
      note?: string;
      expectedVersion: number;
    }) =>
      reviewObjectiveAnswerItem(batchId as string, payload.itemId, {
        reviewStatus: payload.reviewStatus,
        note: payload.note,
        expectedVersion: payload.expectedVersion,
      }),
    onSuccess: () => {
      message.success('复核意见已保存');
      setReviewTarget(undefined);
      queryClient.invalidateQueries({ queryKey: ['oac', 'items', batchId] });
      queryClient.invalidateQueries({ queryKey: ['oac', 'batch', batchId] });
      if (detailId) {
        queryClient.invalidateQueries({ queryKey: ['oac', 'item', batchId, detailId] });
      }
    },
    onError: (error) => message.error(extractErrorMessage(error, '复核失败')),
  });

  const download = async () => {
    if (!batchId) return;
    try {
      const response = await exportObjectiveAnswerBatch(batchId, filter);
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const filename = parseContentDispositionFilename(response) || '客观题答案AI检查.xlsx';
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (error) {
      message.error(extractErrorMessage(error, '导出失败'));
    }
  };

  const refresh = () => {
    if (!batchId) return;
    batchQuery.refetch();
    itemsQuery.refetch();
  };

  const columns: ColumnsType<ObjectiveAnswerItem> = useMemo(
    () => [
      {
        title: '题干',
        dataIndex: 'stemPreview',
        ellipsis: true,
        render: (text: string) => <Typography.Text ellipsis>{text}</Typography.Text>,
      },
      { title: '题型', dataIndex: 'questionTypeLabel', width: 80 },
      {
        title: '原答案',
        dataIndex: 'originalAnswer',
        width: 160,
        ellipsis: true,
      },
      { title: 'AI 答案', dataIndex: 'aiAnswer', width: 160, ellipsis: true },
      {
        title: '原答案支持度',
        width: 110,
        render: (_, item) => (
          <Tooltip title="模型对原答案标记的支持程度，不是正确率">
            <span>{formatPercent(item.referenceConfidence)}</span>
          </Tooltip>
        ),
      },
      {
        title: 'AI 置信度',
        width: 130,
        render: (_, item) => (
          <span>
            {formatPercent(item.aiConfidence)}
            {item.minMargin !== null && item.minMargin !== undefined && (
              <Typography.Text type="secondary" style={{ marginLeft: 4 }}>
                (差 {formatPercent(item.minMargin)})
              </Typography.Text>
            )}
          </span>
        ),
      },
      {
        title: '检查结果',
        width: 130,
        render: (_, item) =>
          item.resultCode ? (
            <Space size={4} wrap>
              <Tag color={RESULT_COLORS[item.resultCode]}>{RESULT_LABELS[item.resultCode]}</Tag>
              {item.decisionWarning === 'AI_COMBINATION_INVALID' && (
                <Tag color="orange">组合无效</Tag>
              )}
            </Space>
          ) : (
            <Tag>等待检查</Tag>
          ),
      },
      {
        title: '内容质量',
        width: 120,
        render: (_, item) => (
          <Tag color={CONTENT_QUALITY_COLORS[item.contentQuality]}>
            {CONTENT_QUALITY_LABELS[item.contentQuality]}
          </Tag>
        ),
      },
      {
        title: '人工复核',
        width: 130,
        render: (_, item) => REVIEW_LABELS[item.reviewStatus] ?? item.reviewStatus,
      },
      {
        title: '操作',
        width: 130,
        fixed: 'right',
        render: (_, item) => (
          <Space size={4}>
            <Button type="link" size="small" onClick={() => setDetailId(item.id)}>
              详情
            </Button>
            <Button
              type="link"
              size="small"
              disabled={!item.resultCode}
              onClick={() => setReviewTarget(item)}
            >
              复核
            </Button>
          </Space>
        ),
      },
    ],
    [],
  );

  if (!batchId) {
    return (
      <Empty
        description={
          currentQuery.data
            ? '正在载入当前批次…'
            : '暂无正在进行的检查批次，请先到“选择试题”发起检查'
        }
      />
    );
  }

  return (
    <div>
      {batch && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Row gutter={16} align="middle">
            <Col flex="auto">
              <Space wrap size={16}>
                <Badge
                  status={BATCH_STATUS_COLORS[status ?? 'pending'] as never}
                  text={BATCH_STATUS_LABELS[status ?? 'pending']}
                />
                <Typography.Text type="secondary">批次 {batch.id}</Typography.Text>
                <Typography.Text>筛选：{batch.filterSummary}</Typography.Text>
                <Typography.Text type="secondary">
                  {batch.createdByName ?? '-'} · {batch.createdAt?.replace('T', ' ')}
                </Typography.Text>
                {batch.modelId && <Tag>{batch.modelId}</Tag>}
              </Space>
              <div style={{ marginTop: 8 }}>
                <Progress
                  percent={progressPercent(batch.done, batch.total)}
                  size="small"
                  status={status === 'failed' ? 'exception' : undefined}
                />
                <Typography.Text type="secondary">
                  共 {formatNumber(batch.total)} 道，已完成 {formatNumber(batch.done)} 道
                  {batch.failed > 0 ? `（含 ${batch.failed} 道失败）` : ''}
                  ，进行中 {formatNumber(batch.processing)} 道，待检查 {formatNumber(batch.pending)} 道
                </Typography.Text>
              </div>
            </Col>
            <Col>
              <Space wrap>
                <Button onClick={refresh}>刷新</Button>
                {batch.canPause && (
                  <Button onClick={() => pauseMutation.mutate()} loading={pauseMutation.isPending}>
                    暂停
                  </Button>
                )}
                {batch.canResume && (
                  <Button
                    type="primary"
                    onClick={() => resumeMutation.mutate()}
                    loading={resumeMutation.isPending}
                  >
                    继续
                  </Button>
                )}
                {batch.canRetryFailed && (
                  <Button
                    onClick={() =>
                      Modal.confirm({
                        title: '重试失败项',
                        content:
                          '将重新检查失败试题，并清除这些行的旧复核标记（内容格式快照保留）。继续？',
                        okText: '重试',
                        onOk: () => retryMutation.mutate(),
                      })
                    }
                    loading={retryMutation.isPending}
                  >
                    重试失败项
                  </Button>
                )}
                <Button onClick={download}>导出</Button>
              </Space>
            </Col>
          </Row>
          {batch.failReason && (
            <Alert style={{ marginTop: 8 }} type="error" showIcon message={batch.failReason} />
          )}
        </Card>
      )}

      {batch && (
        <Row gutter={8} style={{ marginBottom: 12 }}>
          <StatCard
            title="高置信不一致"
            value={batch.mismatchHigh}
            color={RESULT_COLORS.mismatch_high}
            active={filter.resultCode === 'mismatch_high'}
            onClick={() => {
              setFilter({ resultCode: 'mismatch_high' });
              setPage(1);
            }}
          />
          <StatCard
            title="低置信复核"
            value={batch.lowConfidence}
            color={RESULT_COLORS.low_confidence}
            active={filter.resultCode === 'low_confidence'}
            onClick={() => {
              setFilter({ resultCode: 'low_confidence' });
              setPage(1);
            }}
          />
          <StatCard
            title="高置信一致"
            value={batch.consistentHigh}
            color={RESULT_COLORS.consistent_high}
            active={filter.resultCode === 'consistent_high'}
            onClick={() => {
              setFilter({ resultCode: 'consistent_high' });
              setPage(1);
            }}
          />
          <StatCard
            title="答案结构异常"
            value={batch.invalid}
            color="#8c8c8c"
            active={filter.resultCode === 'invalid'}
            onClick={() => {
              setFilter({ resultCode: 'invalid' });
              setPage(1);
            }}
          />
          <StatCard
            title="检查失败"
            value={batch.failed}
            color={RESULT_COLORS.failed}
            active={filter.resultCode === 'failed'}
            onClick={() => {
              setFilter({ resultCode: 'failed' });
              setPage(1);
            }}
          />
          <StatCard
            title="内容格式待核"
            value={batch.contentSuspect + batch.contentInvalid}
            color="#d48806"
            active={filter.contentQuality === 'not_clean'}
            onClick={() => {
              setFilter({ contentQuality: 'not_clean' });
              setPage(1);
            }}
          />
          <StatCard
            title="全部"
            value={batch.total}
            color="#1677ff"
            active={!filter.resultCode && !filter.contentQuality && !filter.needsReview}
            onClick={() => {
              setFilter({});
              setPage(1);
            }}
          />
        </Row>
      )}

      <Space style={{ marginBottom: 8 }} wrap>
        <Tag
          color={filter.needsReview ? 'blue' : undefined}
          style={{ cursor: 'pointer' }}
          onClick={() => {
            setFilter(DEFAULT_FILTER);
            setPage(1);
          }}
        >
          需人工复核（默认）
        </Tag>
        <Input.Search
          allowClear
          placeholder="搜索题干"
          style={{ width: 240 }}
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onSearch={(value) => {
            setFilter({ ...filter, keyword: value || undefined });
            setPage(1);
          }}
        />
        <SelectReviewStatus
          value={filter.reviewStatus}
          onChange={(value) => {
            setFilter({ ...filter, reviewStatus: value });
            setPage(1);
          }}
        />
      </Space>

      <Table<ObjectiveAnswerItem>
        rowKey="id"
        size="small"
        loading={itemsQuery.isFetching}
        columns={columns}
        dataSource={itemsQuery.data?.items ?? []}
        scroll={{ x: 1300 }}
        pagination={{
          current: page,
          pageSize,
          total: itemsQuery.data?.total ?? 0,
          showSizeChanger: true,
          pageSizeOptions: [20, 50, 100],
          showTotal: (total) => `共 ${formatNumber(total)} 条`,
          onChange: (nextPage, nextSize) => {
            setPage(nextPage);
            setPageSize(nextSize);
          },
        }}
        locale={{ emptyText: <Empty description="当前筛选下没有需要复核的记录" /> }}
      />

      <Drawer
        title="检查详情"
        width={760}
        open={!!detailId}
        onClose={() => setDetailId(undefined)}
        destroyOnHidden
      >
        {detailQuery.data && (
          <ItemDetail
            detail={detailQuery.data}
            onReview={() => {
              if (detailQuery.data) {
                setReviewTarget(detailQuery.data.item);
              }
            }}
          />
        )}
      </Drawer>

      <ReviewModal
        item={reviewTarget}
        onCancel={() => setReviewTarget(undefined)}
        onSubmit={(values) => {
          if (!reviewTarget) return;
          reviewMutation.mutate({
            itemId: reviewTarget.id,
            reviewStatus: values.reviewStatus,
            note: values.note,
            expectedVersion: reviewTarget.version,
          });
        }}
        loading={reviewMutation.isPending}
      />
    </div>
  );
};

const StatCard: React.FC<{
  title: string;
  value: number;
  color: string;
  active?: boolean;
  onClick: () => void;
}> = ({ title, value, color, active, onClick }) => (
  <Col flex="1 1 130px" style={{ minWidth: 130, marginBottom: 8 }}>
    <Card
      size="small"
      hoverable
      onClick={onClick}
      style={{ borderColor: active ? color : undefined, cursor: 'pointer' }}
    >
      <Typography.Text type="secondary">{title}</Typography.Text>
      <div style={{ fontSize: 22, fontWeight: 600, color }}>{formatNumber(value)}</div>
    </Card>
  </Col>
);

const SelectReviewStatus: React.FC<{
  value?: string;
  onChange: (value?: string) => void;
}> = ({ value, onChange }) => (
  <Radio.Group
    value={value}
    onChange={(event) => onChange(event.target.value || undefined)}
    options={[
      { value: '', label: '全部复核状态' },
      { value: 'unreviewed', label: '未复核' },
      { value: 'confirmed_correct', label: '确认无误' },
      { value: 'needs_edit', label: '需修改' },
      { value: 'deferred', label: '暂缓' },
    ]}
    optionType="button"
    size="small"
  />
);

const ItemDetail: React.FC<{
  detail: ObjectiveAnswerItemDetail;
  onReview: () => void;
}> = ({ detail, onReview }) => {
  const item = detail.item;
  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {detail.sourceChanged && (
        <Alert
          type="warning"
          showIcon
          message="题库内容已变更或已删除"
          description="本次结论仍针对创建批次时的快照；如需检查最新内容，请新建批次。"
        />
      )}
      <Descriptions column={2} size="small" bordered>
        <Descriptions.Item label="题型" span={1}>
          {item.questionTypeLabel}
        </Descriptions.Item>
        <Descriptions.Item label="来源">{detail.sourceLabel}</Descriptions.Item>
        <Descriptions.Item label="工种">{detail.workTypeTitle ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="等级">
          {detail.levelCode ? `${detail.levelCode} 级` : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="课程">{detail.courseTitle ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="知识点">{detail.knowledgePath ?? '-'}</Descriptions.Item>
      </Descriptions>
      <Card size="small" title="题干">
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
          {detail.stemText}
        </Typography.Paragraph>
      </Card>
      <Card size="small" title="原解析（仅供人工查看，未发送给 AI）">
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
          {detail.resolveSnapshot || '无'}
        </Typography.Paragraph>
      </Card>

      <Table
        rowKey="optionId"
        size="small"
        pagination={false}
        dataSource={detail.options}
        columns={[
          { title: '选项', dataIndex: 'label', width: 60 },
          { title: '选项内容', dataIndex: 'text', ellipsis: true },
          {
            title: '原答案',
            width: 80,
            render: (_, row) => (row.originalCorrect ? '正确项' : ''),
          },
          {
            title: 'AI 判断',
            width: 90,
            render: (_, row) => (row.aiCorrect === null ? '-' : row.aiCorrect ? '应选' : '不应选'),
          },
          {
            title: 'AI 置信',
            width: 90,
            render: (_, row) => formatPercent(row.confidence),
          },
          {
            title: '原标记支持概率',
            width: 130,
            render: (_, row) => formatPercent(row.referenceProbability),
          },
          {
            title: '最优/次优',
            width: 120,
            render: (_, row) =>
              row.top1Probability === null
                ? '-'
                : `${formatPercent(row.top1Probability)} / ${formatPercent(row.top2Probability)}`,
          },
          {
            title: '概率差',
            width: 90,
            render: (_, row) => formatPercent(row.margin),
          },
        ]}
      />

      {item.contentWarnings.length > 0 && (
        <Card size="small" title="内容质量明细">
          <Space direction="vertical" style={{ width: '100%' }}>
            {item.contentWarnings.map((warning: ObjectiveAnswerContentWarning, index) => (
              <Alert
                key={`${warning.ruleCode}-${index}`}
                type={warning.severity === 'invalid' ? 'error' : 'warning'}
                showIcon
                message={`${warning.location === 'stem' ? '题干' : '选项'}：${warning.message}`}
                description={
                  <span>
                    命中文本：<Typography.Text code>{warning.matchedText || '-'}</Typography.Text>
                    {warning.suggestedText && (
                      <>
                        ；转换候选：
                        <Typography.Text code>{warning.suggestedText}</Typography.Text>
                        （仅供人工核对，未替换原值、未发送给 AI）
                      </>
                    )}
                  </span>
                }
              />
            ))}
          </Space>
        </Card>
      )}

      <Descriptions column={2} size="small" bordered title="检查结论">
        <Descriptions.Item label="原答案">{item.originalAnswer}</Descriptions.Item>
        <Descriptions.Item label="AI 答案">{item.aiAnswer}</Descriptions.Item>
        <Descriptions.Item label="检查结果">
          {item.resultCode ? (
            <Tag color={RESULT_COLORS[item.resultCode]}>{RESULT_LABELS[item.resultCode]}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="内容质量">
          <Tag color={CONTENT_QUALITY_COLORS[item.contentQuality]}>
            {CONTENT_QUALITY_LABELS[item.contentQuality]}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="原答案支持度">
          {formatPercent(item.referenceConfidence)}
        </Descriptions.Item>
        <Descriptions.Item label="AI 置信度">{formatPercent(item.aiConfidence)}</Descriptions.Item>
        <Descriptions.Item label="最小概率差">{formatPercent(item.minMargin)}</Descriptions.Item>
        <Descriptions.Item label="组合告警">{item.decisionWarning ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="模型" span={2}>
          {detail.requestedModel ?? detail.modelId}
          {detail.responseModel ? ` → ${detail.responseModel}` : ''}
        </Descriptions.Item>
        <Descriptions.Item label="提示词版本">{detail.promptVersion}</Descriptions.Item>
        <Descriptions.Item label="检查时间">
          {item.checkedAt?.replace('T', ' ') ?? '-'}
        </Descriptions.Item>
      </Descriptions>

      <Card size="small" title="模型调用审计">
        {detail.calls.length === 0 ? (
          <Typography.Text type="secondary">本次未调用模型</Typography.Text>
        ) : (
          <Table
            rowKey="callId"
            size="small"
            pagination={false}
            dataSource={detail.calls}
            columns={[
              { title: '分块', render: (_, row) => `${row.chunkIndex}/${row.chunkCount}`, width: 70 },
              { title: '状态', dataIndex: 'status', width: 80 },
              { title: '请求模型', dataIndex: 'requestedModel', width: 140 },
              { title: '响应模型', dataIndex: 'responseModel', width: 140 },
              { title: '输入 token', dataIndex: 'inputTokens', width: 100 },
              {
                title: '估算',
                dataIndex: 'usageEstimated',
                width: 70,
                render: (value: boolean) => (value ? '是' : '否'),
              },
              { title: '耗时(ms)', dataIndex: 'durationMs', width: 90 },
              { title: '错误', dataIndex: 'errorMessage', ellipsis: true },
            ]}
          />
        )}
      </Card>

      <Space>
        <Button type="primary" onClick={onReview} disabled={!item.resultCode}>
          人工复核
        </Button>
        <Typography.Text type="secondary">
          复核只记录处理意见，不会修改题库答案。
        </Typography.Text>
      </Space>
    </Space>
  );
};

const ReviewModal: React.FC<{
  item?: ObjectiveAnswerItem;
  loading: boolean;
  onCancel: () => void;
  onSubmit: (values: { reviewStatus: ObjectiveAnswerReviewStatus; note?: string }) => void;
}> = ({ item, loading, onCancel, onSubmit }) => {
  const [form] = Form.useForm();
  useEffect(() => {
    if (item) {
      form.setFieldsValue({ reviewStatus: item.reviewStatus, note: item.reviewNote ?? '' });
    }
  }, [item, form]);
  return (
    <Modal
      title="人工复核"
      open={!!item}
      onCancel={onCancel}
      confirmLoading={loading}
      onOk={() => form.validateFields().then((values) => onSubmit(values))}
      okText="保存意见"
      destroyOnHidden
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="这里只记录处理意见，不会修改题库；如需改题请在题库编辑页操作。"
      />
      <Form form={form} layout="vertical">
        <Form.Item name="reviewStatus" label="复核结论" rules={[{ required: true }]}>
          <Radio.Group
            options={[
              { value: 'confirmed_correct', label: '确认原答案无误' },
              { value: 'needs_edit', label: '需要修改' },
              { value: 'deferred', label: '暂缓处理' },
              { value: 'unreviewed', label: '撤销复核' },
            ]}
          />
        </Form.Item>
        <Form.Item name="note" label="备注（最多 500 字）">
          <Input.TextArea rows={3} maxLength={500} showCount />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ObjectiveAnswerCheckResults;

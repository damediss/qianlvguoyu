import React, { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { useAuthStore } from '../../stores/authStore';
import ObjectiveAnswerCheckResults from './ObjectiveAnswerCheckResults';
import ObjectiveAnswerCheckBatches from './ObjectiveAnswerCheckBatches';
import {
  createObjectiveAnswerBatch,
  getFilterOptions,
  pageObjectiveAnswerQuestions,
  previewObjectiveAnswerSelection,
} from './objectiveAnswerCheckApi';
import type {
  ObjectiveAnswerPreview,
  ObjectiveAnswerQuestionRow,
  ObjectiveAnswerScope,
} from './objectiveAnswerCheckTypes';
import { extractErrorMessage, formatNumber } from './objectiveAnswerCheckUtils';

const DEFAULT_TYPES = ['single', 'multi', 'judge'];

/**
 * 客观题答案 AI 检查（方案 §5）：一个页面、三个页签。
 * 预览与创建共用同一 SelectionReq，禁止前端拼两套筛选逻辑。
 */
const ObjectiveAnswerCheckPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('select');
  const [activeBatchId, setActiveBatchId] = useState<string | undefined>();

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="AI 检查用于快速筛查，不能替代专家审定；检查不会修改题库答案。"
      />
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'select',
            label: '选择试题',
            children: (
              <SelectionTab
                onCreated={(batchId) => {
                  setActiveBatchId(batchId);
                  setActiveTab('results');
                }}
              />
            ),
          },
          {
            key: 'results',
            label: '检查结果',
            children: (
              <ObjectiveAnswerCheckResults
                batchId={activeBatchId}
                onBatchChange={setActiveBatchId}
              />
            ),
          },
          {
            key: 'batches',
            label: '检查批次',
            children: (
              <ObjectiveAnswerCheckBatches
                onOpenResults={(batchId) => {
                  setActiveBatchId(batchId);
                  setActiveTab('results');
                }}
              />
            ),
          },
        ]}
      />
    </div>
  );
};

const SelectionTab: React.FC<{ onCreated: (batchId: string) => void }> = ({ onCreated }) => {
  const paneActive = usePaneActive();
  const queryClient = useQueryClient();
  const canSubmit = useAuthStore((state) =>
    state.hasPermission('exam:objective-answer-check:submit'),
  );
  const [filters, setFilters] = useState<ObjectiveAnswerScope>({
    types: DEFAULT_TYPES,
    workTypeId: null,
    levelCode: null,
    source: null,
  });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [selectedKeys, setSelectedKeys] = useState<number[]>([]);
  const [filterMode, setFilterMode] = useState(false);
  const [filterCount, setFilterCount] = useState(0);
  const [preview, setPreview] = useState<ObjectiveAnswerPreview | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);

  const filterOptions = useQuery({
    queryKey: ['oac', 'filter-options', filters.workTypeId],
    queryFn: () => getFilterOptions(filters.workTypeId),
  });

  const questions = useQuery({
    queryKey: ['oac', 'questions', filters, page, pageSize],
    queryFn: () => pageObjectiveAnswerQuestions({ scope: filters, page, pageSize }),
    placeholderData: keepPreviousData,
    enabled: paneActive,
  });

  const previewMutation = useMutation({
    mutationFn: previewObjectiveAnswerSelection,
    onSuccess: (data) => {
      if (data.overLimit) {
        message.warning(
          `本次最多检查 ${data.maxBatchSize} 道，请缩小筛选范围或改为逐题勾选。`,
        );
        setPreview(null);
        setPreviewOpen(false);
        return;
      }
      setPreview(data);
      setPreviewOpen(true);
    },
    onError: (error) => message.error(extractErrorMessage(error, '预览失败，请重试')),
  });

  const createMutation = useMutation({
    mutationFn: createObjectiveAnswerBatch,
    onSuccess: (data) => {
      message.success('已创建检查批次，正在后台检查');
      setPreviewOpen(false);
      setPreview(null);
      setSelectedKeys([]);
      setFilterMode(false);
      setFilterCount(0);
      queryClient.invalidateQueries({ queryKey: ['oac', 'batches'] });
      onCreated(data.batchId);
    },
    onError: (error) => message.error(extractErrorMessage(error, '创建批次失败，请重试')),
  });

  const selectAllMutation = useMutation({
    mutationFn: () => previewObjectiveAnswerSelection({ selectionMode: 'filter', scope: filters }),
    onSuccess: (data) => {
      if (data.overLimit) {
        message.warning(
          `当前筛选共 ${formatNumber(data.selectionCount)} 道，超过单批上限 `
          + `${formatNumber(data.maxBatchSize)} 道，请缩小范围或改为逐题勾选。`,
        );
        return;
      }
      setFilterMode(true);
      setFilterCount(data.selectionCount);
      setSelectedKeys([]);
      message.success(`已按当前筛选选择 ${formatNumber(data.selectionCount)} 道`);
    },
    onError: (error) => message.error(extractErrorMessage(error, '选择失败，请重试')),
  });

  const hasSelection = selectedKeys.length > 0 || filterMode;

  const applyFilters = (next: ObjectiveAnswerScope) => {
    setFilters(next);
    setPage(1);
    setSelectedKeys([]);
    setFilterMode(false);
    setFilterCount(0);
  };

  const changeFilters = (next: ObjectiveAnswerScope) => {
    if (!hasSelection) {
      applyFilters(next);
      return;
    }
    Modal.confirm({
      title: '筛选条件变化将清空已选试题',
      content: '是否继续？',
      okText: '继续',
      cancelText: '取消',
      onOk: () => applyFilters(next),
    });
  };

  const buildSelection = () => {
    if (filterMode) {
      return { selectionMode: 'filter' as const, scope: filters };
    }
    return {
      selectionMode: 'ids' as const,
      detailIds: selectedKeys,
      scope: filters,
    };
  };

  const startPreview = () => {
    if (!hasSelection) {
      message.warning('请先勾选试题，或按当前筛选选择全部');
      return;
    }
    previewMutation.mutate(buildSelection());
  };

  const columns: ColumnsType<ObjectiveAnswerQuestionRow> = useMemo(
    () => [
      {
        title: '题干',
        dataIndex: 'stemPreview',
        ellipsis: true,
        render: (text: string) => <Typography.Text ellipsis>{text}</Typography.Text>,
      },
      { title: '题型', dataIndex: 'questionTypeLabel', width: 90 },
      { title: '来源', dataIndex: 'sourceLabel', width: 90 },
      {
        title: '工种/等级',
        width: 200,
        render: (_, row) =>
          row.workTypeTitle
            ? `${row.workTypeTitle}${row.levelLabel ? ` / ${row.levelLabel}` : ''}`
            : '-',
      },
      { title: '原答案', dataIndex: 'originalAnswer', width: 160, ellipsis: true },
      {
        title: '预检',
        width: 150,
        render: (_, row) => {
          if (row.invalidReason) {
            return <Tag color="default">结构异常</Tag>;
          }
          if (row.contentQuality === 'invalid') {
            return <Tag color="red">格式确定异常</Tag>;
          }
          if (row.contentQuality === 'suspect') {
            return <Tag color="gold">格式疑似异常</Tag>;
          }
          return <Tag color="green">可检查</Tag>;
        },
      },
    ],
    [],
  );

  const totalSelected = filterMode ? filterCount : selectedKeys.length;

  return (
    <div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Space wrap>
          <Select
            allowClear
            showSearch
            style={{ width: 300 }}
            placeholder="工种（可搜索）"
            value={filters.workTypeId ?? undefined}
            optionFilterProp="label"
            loading={filterOptions.isLoading}
            options={(filterOptions.data?.workTypes ?? []).map((item) => ({
              value: Number(item.id),
              label: item.path || item.title,
            }))}
            onChange={(value) =>
              changeFilters({ ...filters, workTypeId: value ?? null, levelCode: null })
            }
          />
          <Select
            allowClear
            style={{ width: 170 }}
            placeholder="等级"
            value={filters.levelCode ?? undefined}
            options={(filterOptions.data?.levels ?? []).map((item) => ({
              value: item.code,
              label: item.label,
            }))}
            onChange={(value) => changeFilters({ ...filters, levelCode: value ?? null })}
          />
          <Select
            allowClear
            style={{ width: 140 }}
            placeholder="来源"
            value={filters.source ?? undefined}
            options={(filterOptions.data?.sources ?? []).map((item) => ({
              value: item.value,
              label: item.label,
            }))}
            onChange={(value) => changeFilters({ ...filters, source: value ?? null })}
          />
          <Select
            mode="multiple"
            style={{ minWidth: 240 }}
            placeholder="题型"
            value={filters.types}
            options={(filterOptions.data?.types ?? []).map((item) => ({
              value: item.value,
              label: item.label,
            }))}
            onChange={(values) =>
              changeFilters({ ...filters, types: values.length ? values : DEFAULT_TYPES })
            }
          />
          <Button onClick={() => applyFilters({ ...filters })}>查询</Button>
          <Button
            onClick={() =>
              applyFilters({ types: DEFAULT_TYPES, workTypeId: null, levelCode: null, source: null })
            }
          >
            重置
          </Button>
        </Space>
      </Card>

      <Space style={{ marginBottom: 8 }} wrap>
        <Typography.Text>
          已逐题勾选 {selectedKeys.length} 道
          {filterMode ? ` / 已按筛选选择 ${formatNumber(filterCount)} 道` : ''}
        </Typography.Text>
        <Button
          onClick={() => selectAllMutation.mutate()}
          loading={selectAllMutation.isPending}
          disabled={filterMode}
        >
          选择筛选全部
        </Button>
        <Button
          onClick={() => {
            setSelectedKeys([]);
            setFilterMode(false);
            setFilterCount(0);
          }}
          disabled={!hasSelection}
        >
          清空
        </Button>
        {filterMode && (
          <Button type="link" onClick={() => setFilterMode(false)}>
            改为逐题勾选
          </Button>
        )}
      </Space>

      <Table<ObjectiveAnswerQuestionRow>
        rowKey="detailId"
        size="small"
        loading={questions.isFetching}
        columns={columns}
        dataSource={questions.data?.items ?? []}
        rowSelection={
          filterMode
            ? undefined
            : {
                selectedRowKeys: selectedKeys,
                preserveSelectedRowKeys: true,
                onChange: (keys) => setSelectedKeys(keys as number[]),
              }
        }
        pagination={{
          current: page,
          pageSize,
          total: questions.data?.total ?? 0,
          showSizeChanger: true,
          pageSizeOptions: [20, 50, 100],
          showTotal: (total) => `共 ${formatNumber(total)} 道`,
          onChange: (nextPage, nextSize) => {
            setPage(nextPage);
            setPageSize(nextSize);
          },
        }}
        locale={{ emptyText: <Empty description="没有符合条件的试题" /> }}
      />

      <div style={{ marginTop: 12 }}>
        {canSubmit && (
          <Button
            type="primary"
            onClick={startPreview}
            loading={previewMutation.isPending}
            disabled={!hasSelection}
          >
            预览并开始 AI 检查
          </Button>
        )}
        {totalSelected > 0 && (
          <Typography.Text type="secondary" style={{ marginLeft: 12 }}>
            本次将检查 {formatNumber(totalSelected)} 道
          </Typography.Text>
        )}
      </div>

      <Modal
        title="确认开始 AI 检查"
        open={previewOpen}
        onCancel={() => setPreviewOpen(false)}
        onOk={() => preview && createMutation.mutate(buildSelection())}
        okText="开始检查"
        confirmLoading={createMutation.isPending}
        width={640}
        destroyOnHidden
      >
        {preview && (
          <div>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="本次选择">
                {formatNumber(preview.selectionCount)} 道
                {preview.typeCounts
                  ? `（单选 ${preview.typeCounts.single ?? 0} / 多选 `
                    + `${preview.typeCounts.multi ?? 0} / 判断 ${preview.typeCounts.judge ?? 0}）`
                  : ''}
              </Descriptions.Item>
              <Descriptions.Item label="可直接检查">
                {formatNumber(preview.validCount ?? 0)} 道；结构异常{' '}
                {formatNumber(preview.invalidCount ?? 0)} 道（仍会留在结果中）
              </Descriptions.Item>
              <Descriptions.Item label="内容格式提醒">
                疑似 {formatNumber(preview.suspectContentCount ?? 0)} 道（仍参与 AI 但必复核）；
                确定异常 {formatNumber(preview.invalidContentCount ?? 0)} 道（不调 AI）
              </Descriptions.Item>
              <Descriptions.Item label="预计逻辑调用">
                {formatNumber(preview.expectedCalls ?? 0)} 次（多选按每 5 个选项分块，失败重试另计）
              </Descriptions.Item>
              <Descriptions.Item label="模型">{preview.modelId}</Descriptions.Item>
              <Descriptions.Item label="高置信门槛">
                AI 置信度 ≥ {Math.round((preview.confidenceThreshold ?? 0) * 100)}%，概率差 ≥{' '}
                {Math.round((preview.marginThreshold ?? 0) * 100)}%
              </Descriptions.Item>
            </Descriptions>
            <Alert
              style={{ marginTop: 12 }}
              type="warning"
              showIcon
              message="AI 不读取原答案和解析；结果不会自动修改题库。"
            />
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ObjectiveAnswerCheckPage;

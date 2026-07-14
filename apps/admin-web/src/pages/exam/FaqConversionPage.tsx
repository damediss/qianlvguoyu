import { useCallback, useEffect, useState, useRef } from 'react';
import {
  Card, Form, Select, Checkbox, Button, Table, Tag, Progress, Space, Modal,
  Drawer, Descriptions, Spin, message, Tooltip, Popconfirm,
} from 'antd';
import {
  PlayCircleOutlined, PauseCircleOutlined, ReloadOutlined, EyeOutlined,
  SettingOutlined, RobotOutlined, CheckCircleOutlined, CloseCircleOutlined,
  ClockCircleOutlined, MinusCircleOutlined, LoadingOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  submitFaqConversion, listFaqBatches, getFaqBatchProgress, pageFaqRecords,
  getFaqRecordDetail, pauseFaqBatch, resumeFaqBatch, retryFaqBatch,
  listEnabledFaqModels,
} from '../../api/exam';
import { treeWorkTypes, pageCourses } from '../../api/exam';
import type { FaqBatchVO, FaqConversionVO, FaqModelConfigVO, ExamCourse } from '../../types/exam';
import { FaqConfigDialog } from './FaqConfigDialog';

// 题型映射
const TYPE_LABELS: Record<string, string> = { single: '单选题', multi: '多选题', judge: '判断题' };
const STATUS_CONFIG: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
  pending: { color: 'default', icon: <ClockCircleOutlined />, label: '待转换' },
  processing: { color: 'processing', icon: <LoadingOutlined spin />, label: '转换中' },
  success: { color: 'success', icon: <CheckCircleOutlined />, label: '成功' },
  failed: { color: 'error', icon: <CloseCircleOutlined />, label: '失败' },
  skipped: { color: 'warning', icon: <MinusCircleOutlined />, label: '跳过' },
};

/** 工种题库结构化问答管理主页面 */
export function FaqConversionPage() {
  const [form] = Form.useForm();
  const [batches, setBatches] = useState<FaqBatchVO[]>([]);
  const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);
  const [records, setRecords] = useState<FaqConversionVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [models, setModels] = useState<FaqModelConfigVO[]>([]);
  const [configOpen, setConfigOpen] = useState(false);
  const [detailRecord, setDetailRecord] = useState<Record<string, unknown> | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [workTypeTree, setWorkTypeTree] = useState<{ label: string; value: number }[]>([]);
  const [courseOptions, setCourseOptions] = useState<{ label: string; value: number }[]>([]);

  // 轮询状态
  const [pollingBatchId, setPollingBatchId] = useState<string | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 加载工种树
  useEffect(() => {
    treeWorkTypes().then((res) => {
      const opts: { label: string; value: number }[] = [];
      const walk = (nodes: any[]) => {
        for (const n of nodes) {
          opts.push({ label: n.title, value: n.id });
          if (n.children) walk(n.children);
        }
      };
      walk(res);
      setWorkTypeTree(opts);
    }).catch(() => {});
  }, []);

  // 工种联动加载科目列表
  const workTypeId = Form.useWatch('workTypeId', form);
  useEffect(() => {
    if (!workTypeId) {
      setCourseOptions([]);
      form.setFieldsValue({ courseIds: [] });
      return;
    }
    pageCourses({ workTypeId, page: 1, pageSize: 200 }).then((res) => {
      setCourseOptions(res.items.map((c) => ({ label: c.title, value: c.id })));
    }).catch(() => {
      setCourseOptions([]);
    });
    form.setFieldsValue({ courseIds: [] });
  }, [workTypeId]);

  // 加载模型列表
  useEffect(() => {
    listEnabledFaqModels().then(setModels).catch(() => {});
  }, []);

  // 加载批次列表
  const loadBatches = useCallback(async () => {
    try {
      const res = await listFaqBatches({ page: 1, pageSize: 20 });
      setBatches(res.items);
    } catch {
      // silent
    }
  }, []);

  useEffect(() => { loadBatches(); }, [loadBatches]);

  // 轮询进度
  useEffect(() => {
    if (pollRef.current) clearInterval(pollRef.current);
    if (!pollingBatchId) return;

    pollRef.current = setInterval(async () => {
      try {
        const progress = await getFaqBatchProgress(pollingBatchId);
        setBatches((prev) => prev.map((b) => b.id === pollingBatchId ? progress : b));
        if (progress.status === 'completed' || progress.status === 'canceled') {
          setPollingBatchId(null);
          message.success('批次转换完成');
          loadBatches();
        }
      } catch {
        // silent
      }
    }, 3000);

    return () => { if (pollRef.current) clearInterval(pollRef.current); };
  }, [pollingBatchId, loadBatches]);

  // 加载记录
  const loadRecords = useCallback(async () => {
    if (!selectedBatchId) return;
    setLoading(true);
    try {
      const res = await pageFaqRecords({ batchId: selectedBatchId, page, pageSize });
      setRecords(res.items);
      setTotal(res.total);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  }, [selectedBatchId, page, pageSize]);

  useEffect(() => { loadRecords(); }, [loadRecords]);

  // 发起转换
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const batch = await submitFaqConversion({
        workTypeId: values.workTypeId,
        courseIds: values.courseIds,
        questionTypes: values.questionTypes,
        modelCode: values.modelCode,
        concurrencyLimit: values.concurrencyLimit || 5,
      });
      message.success(`已创建批次，共 ${batch.totalExams} 道试题`);
      setSelectedBatchId(batch.id);
      setPollingBatchId(batch.id);
      loadBatches();
      form.resetFields();
    } catch (e: any) {
      message.error(e.message || '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  // 查看明细
  const handleViewDetail = async (id: string) => {
    try {
      const detail = await getFaqRecordDetail(id);
      setDetailRecord(detail);
      setDetailOpen(true);
    } catch {
      message.error('加载详情失败');
    }
  };

  // 记录表格列
  const recordColumns: ColumnsType<FaqConversionVO> = [
    { title: 'ID', dataIndex: 'id', width: 100, ellipsis: true },
    {
      title: '题型', dataIndex: 'sourceType', width: 80,
      render: (t: string) => TYPE_LABELS[t] || t,
    },
    {
      title: '标准问', dataIndex: 'standardQuestion', ellipsis: true,
    },
    {
      title: '标签', dataIndex: 'tagName', width: 200, ellipsis: true,
    },
    {
      title: '状态', dataIndex: 'conversionStatus', width: 100,
      render: (s: string) => {
        const cfg = STATUS_CONFIG[s] || { color: 'default', label: s };
        return <Tag color={cfg.color} icon={cfg.icon}>{cfg.label}</Tag>;
      },
    },
    {
      title: '操作', key: 'actions', width: 80,
      render: (_: unknown, row: FaqConversionVO) => (
        <Tooltip title="查看详情">
          <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => handleViewDetail(row.id)} />
        </Tooltip>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card title={<><RobotOutlined /> 工种题库结构化问答管理</>} extra={
        <Button icon={<SettingOutlined />} onClick={() => setConfigOpen(true)}>WeKnora & LLM 配置</Button>
      }>
        {/* 筛选条件 */}
        <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="workTypeId" label="工种" rules={[{ required: true, message: '请选择工种' }]}>
            <Select options={workTypeTree} placeholder="选择工种" style={{ width: 200 }} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="courseIds" label="科目">
            <Select
              mode="multiple"
              options={courseOptions}
              placeholder={workTypeId ? '选择科目（留空=全部）' : '请先选择工种'}
              disabled={!workTypeId}
              allowClear
              showSearch
              optionFilterProp="label"
              maxTagCount={3}
              style={{ width: 200 }}
            />
          </Form.Item>
          <Form.Item name="questionTypes" label="题型" initialValue={['single', 'multi', 'judge']}>
            <Checkbox.Group options={[{ label: '单选', value: 'single' }, { label: '多选', value: 'multi' }, { label: '判断', value: 'judge' }]} />
          </Form.Item>
          <Form.Item name="modelCode" label="LLM 模型" rules={[{ required: true, message: '请选择模型' }]}>
            <Select placeholder="选择模型" style={{ width: 200 }}>
              {models.map((m) => (<Select.Option key={m.modelCode} value={m.modelCode}>{m.modelName}</Select.Option>))}
            </Select>
          </Form.Item>
          <Form.Item>
            <Button type="primary" icon={<PlayCircleOutlined />} loading={submitting} onClick={handleSubmit}>开始转换</Button>
          </Form.Item>
        </Form>

        {/* 批次列表 */}
        <Card size="small" title="转换批次" style={{ marginBottom: 16 }}>
          <Table
            size="small"
            rowKey="id"
            dataSource={batches}
            pagination={{ pageSize: 5 }}
            onRow={(record) => ({ onClick: () => setSelectedBatchId(record.id), style: { cursor: 'pointer' } })}
            rowClassName={(record) => record.id === selectedBatchId ? 'ant-table-row-selected' : ''}
            columns={[
              { title: '批次ID', dataIndex: 'id', width: 120, ellipsis: true },
              { title: '工种', dataIndex: 'workTypeName', width: 120 },
              { title: '模型', dataIndex: 'modelName', width: 120 },
              { title: '总数', dataIndex: 'totalExams', width: 60 },
              { title: '成功', dataIndex: 'successCount', width: 60 },
              { title: '失败', dataIndex: 'failedCount', width: 60 },
              { title: '跳过', dataIndex: 'skippedCount', width: 60 },
              {
                title: '状态', dataIndex: 'status', width: 80,
                render: (s: string) => {
                  const colors: Record<string, string> = { running: 'processing', paused: 'warning', completed: 'success', canceled: 'default' };
                  const labels: Record<string, string> = { running: '运行中', paused: '暂停', completed: '完成', canceled: '取消' };
                  return <Tag color={colors[s] || 'default'}>{labels[s] || s}</Tag>;
                },
              },
              {
                title: '操作', key: 'batchActions', width: 180,
                render: (_: unknown, row: FaqBatchVO) => (
                  <Space size="small">
                    {row.status === 'running' && (
                      <Button size="small" icon={<PauseCircleOutlined />} onClick={(e) => { e.stopPropagation(); pauseFaqBatch(row.id).then(loadBatches); }}>暂停</Button>
                    )}
                    {row.status === 'paused' && (
                      <Button size="small" type="primary" icon={<PlayCircleOutlined />} onClick={(e) => { e.stopPropagation(); resumeFaqBatch(row.id).then(() => { setPollingBatchId(row.id); loadBatches(); }); }}>继续</Button>
                    )}
                    <Popconfirm title="确定重试失败记录？" onConfirm={(e) => { e?.stopPropagation(); retryFaqBatch(row.id).then(loadRecords); }}>
                      <Button size="small" icon={<ReloadOutlined />} onClick={(e) => e.stopPropagation()}>重试失败</Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
          />
        </Card>

        {/* 转换明细 */}
        {selectedBatchId && (
          <Card size="small" title={`转换明细（批次 ${selectedBatchId}）`}>
            <Table
              size="small"
              rowKey="id"
              loading={loading}
              dataSource={records}
              columns={recordColumns}
              pagination={{
                current: page, pageSize, total, showSizeChanger: true,
                onChange: (p, ps) => { setPage(p); setPageSize(ps); },
              }}
            />
          </Card>
        )}
      </Card>

      {/* 详情抽屉 */}
      <Drawer title="转换记录详情" width={600} open={detailOpen} onClose={() => setDetailOpen(false)}>
        {detailRecord && (
          <div style={{ marginBottom: 16 }}>
            <h3>标准问</h3>
            <p>{detailRecord.standardQuestion as string}</p>
            <h3>答案</h3>
            {Array.isArray(detailRecord.answers) && (
              <ul>{(detailRecord.answers as string[]).map((a, i) => <li key={i}>{a}</li>)}</ul>
            )}
            <h3>相似问</h3>
            {Array.isArray(detailRecord.similarQuestions) && (
              <ul>{(detailRecord.similarQuestions as string[]).map((a, i) => <li key={i}>{a}</li>)}</ul>
            )}
            <h3>反例</h3>
            {Array.isArray(detailRecord.negativeQuestions) && (
              <ul>{(detailRecord.negativeQuestions as string[]).map((a, i) => <li key={i}>{a}</li>)}</ul>
            )}
            {Boolean(detailRecord.errorMessage) && (
              <>
                <h3>错误信息</h3>
                <Tag color="error">{String(detailRecord.errorMessage)}</Tag>
              </>
            )}
          </div>
        )}
      </Drawer>

      {/* 配置弹层 */}
      <FaqConfigDialog open={configOpen} onClose={() => { setConfigOpen(false); listEnabledFaqModels().then(setModels).catch(() => {}); }} />
    </div>
  );
}

export default FaqConversionPage;

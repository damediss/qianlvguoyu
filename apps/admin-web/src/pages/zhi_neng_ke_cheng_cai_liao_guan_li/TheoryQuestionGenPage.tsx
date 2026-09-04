/**
 * 理论知识试题生成管理页面 —— 方案 v1.2 §T5
 * 四页签：细目表导入 / 生成任务 / 试题草稿 / 入库与导出
 */
import React, { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button, Descriptions, Drawer, Form, Input, InputNumber, message, Modal, Popconfirm,
  Select, Space, Statistic, Table, Tabs, Tag, Tree, Upload,
} from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { treeWorkTypes } from '../../api/exam';
import type { ExamWorkType } from '../../types/exam';
import type { PageResponse } from '../../types/api';
import {
  deleteBlueprint, editDraft, exportDrafts, getBlueprintTree, getGenProgress, importBlueprint,
  listBlueprints, listDrafts, listGenBatches, listTheoryModelConfigs, pauseGenBatch,
  publishDrafts, resumeGenBatch, reviewDrafts, retryGenBatch, submitGenBatch,
} from './theoryGenApi';
import type { BlueprintNode, TheoryBlueprint, TheoryDraft, TheoryGenBatch } from './theoryGenApi';
import './XiMuBiaoGuanLiPage.css';

const LEVEL_OPTIONS = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

const LEVEL_NAME: Record<number, string> = {
  1: '高级技师', 2: '技师', 3: '高级工', 4: '中级工', 5: '初级工',
};

/** 指引下限推荐题量（每考评点）：五/四级 单选4+判断2；三/二/一级 单选3+多选2+判断2 */
const QUOTA_DEFAULTS: Record<number, { single: number; judge: number; multi?: number }> = {
  5: { single: 4, judge: 2 },
  4: { single: 4, judge: 2 },
  3: { single: 3, judge: 2, multi: 2 },
  2: { single: 3, judge: 2, multi: 2 },
  1: { single: 3, judge: 2, multi: 2 },
};

const TYPE_LABEL: Record<string, string> = {
  single: '单选题', multi: '多选题', judge: '判断题', essay: '简答题',
  cal: '计算题', draw: '绘图题', discuss: '论述题', case_analysis: '案例分析题',
};

const STATUS_TAG: Record<string, { color: string; text: string }> = {
  pending: { color: 'default', text: '等待中' },
  running: { color: 'processing', text: '生成中' },
  paused: { color: 'warning', text: '已暂停' },
  done: { color: 'success', text: '已完成' },
  failed: { color: 'error', text: '失败' },
};

/** 摊平节点树供 Select/树面板使用 */
function flattenNodes(nodes: BlueprintNode[], depth = 0): Array<{ node: BlueprintNode; depth: number }> {
  const out: Array<{ node: BlueprintNode; depth: number }> = [];
  for (const n of nodes) {
    out.push({ node: n, depth });
    if (n.children?.length) {
      out.push(...flattenNodes(n.children, depth + 1));
    }
  }
  return out;
}

const TheoryQuestionGenPage: React.FC = () => {
  const [tab, setTab] = useState('import');

  // ---------------- 页签1：细目表导入 ----------------
  const [workTypeId, setWorkTypeId] = useState<number | undefined>();
  const [level, setLevel] = useState<number | undefined>(5);
  const qc = useQueryClient();

  const wtTree = useQuery({
    queryKey: ['theorygen-worktypes'],
    queryFn: treeWorkTypes,
  });

  const bpQuery = useQuery({
    queryKey: ['theorygen-blueprints'],
    queryFn: () => listBlueprints({ page: 1, pageSize: 50 }),
  });

  const workTypeOptions = useMemo(() => {
    const flat: ExamWorkType[] = [];
    const walk = (list: ExamWorkType[]) => {
      for (const w of list ?? []) {
        flat.push(w);
        if (w.children?.length) walk(w.children);
      }
    };
    walk(wtTree.data ?? []);
    return flat.map((w) => ({ value: w.id, label: w.title }));
  }, [wtTree.data]);

  const doImport = async (file: File) => {
    if (!workTypeId || !level) {
      message.warning('请先选择工种与等级');
      return false;
    }
    try {
      const bp = await importBlueprint(file, workTypeId, level);
      message.success(`导入成功：考评点 ${bp.pointCount} 个，X:Y:Z=${bp.xyzRatio ?? '-'}`);
      qc.invalidateQueries({ queryKey: ['theorygen-blueprints'] });
    } catch {
      // 错误提示由拦截器统一处理
    }
    return false;
  };

  const bpColumns = [
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '等级', dataIndex: 'level', key: 'level', width: 90,
      render: (v: number) => LEVEL_NAME[v] ?? v },
    { title: '考评点', dataIndex: 'pointCount', key: 'pointCount', width: 80 },
    { title: 'X:Y:Z', dataIndex: 'xyzRatio', key: 'xyzRatio', width: 110 },
    { title: '权重校验', dataIndex: 'weightCheck', key: 'weightCheck', width: 90,
      render: (v: number) => (v === 1 ? <Tag color="success">通过</Tag> : <Tag color="warning">未通过</Tag>) },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100,
      render: (v: string) => (v === 'done' ? <Tag color="success">已完成</Tag>
        : v === 'generating' ? <Tag color="processing">生成中</Tag> : <Tag>已导入</Tag>) },
    { title: '操作', key: 'op', width: 160, render: (_: unknown, r: TheoryBlueprint) => (
      <Space>
        <Button size="small" onClick={() => { setSelectedBlueprintId(r.id); setTab('gen'); }}>生成</Button>
        <Popconfirm title="确认删除该批次？" onConfirm={async () => {
          await deleteBlueprint(r.id);
          message.success('已删除');
          qc.invalidateQueries({ queryKey: ['theorygen-blueprints'] });
        }}>
          <Button size="small" danger>删除</Button>
        </Popconfirm>
      </Space>
    ) },
  ];

  // ---------------- 页签2：生成任务 ----------------
  const [selectedBlueprintId, setSelectedBlueprintId] = useState<string | undefined>();
  const [modelConfigId, setModelConfigId] = useState<string | undefined>();
  const [quotaSingle, setQuotaSingle] = useState<number | null>(null);
  const [quotaJudge, setQuotaJudge] = useState<number | null>(null);
  const [quotaMulti, setQuotaMulti] = useState<number | null>(null);
  const [activeBatchId, setActiveBatchId] = useState<string | undefined>();

  const mcQuery = useQuery({
    queryKey: ['theorygen-model-configs'],
    queryFn: listTheoryModelConfigs,
  });

  // 模型默认选中 theory-gen-glm（部署方未配置时取第一个启用项）
  React.useEffect(() => {
    if (!modelConfigId && mcQuery.data?.length) {
      const preferred = mcQuery.data.find((c) => c.modelCode === 'theory-gen-glm');
      setModelConfigId((preferred ?? mcQuery.data[0]).id);
    }
  }, [mcQuery.data, modelConfigId]);

  /** 指引下限推荐题量（方案 §7 defaultQuota）：五/四级 单选4+判断2；三/二/一级 单选3+多选2+判断2 */
  const applyQuotaDefaults = React.useCallback((blueprintId?: string) => {
    const bp = bpQuery.data?.items.find((b) => b.id === blueprintId);
    const d = bp ? QUOTA_DEFAULTS[bp.level] : undefined;
    setQuotaSingle(d?.single ?? null);
    setQuotaJudge(d?.judge ?? null);
    setQuotaMulti(d?.multi ?? null);
  }, [bpQuery.data]);

  React.useEffect(() => {
    applyQuotaDefaults(selectedBlueprintId);
  }, [selectedBlueprintId, applyQuotaDefaults]);

  const selectedBp = bpQuery.data?.items.find((b) => b.id === selectedBlueprintId);
  const multiDisabled = selectedBp ? selectedBp.level === 5 || selectedBp.level === 4 : false;

  const batchQuery = useQuery({
    queryKey: ['theorygen-batches', selectedBlueprintId],
    queryFn: () => listGenBatches({ blueprintId: selectedBlueprintId, page: 1, pageSize: 20 }),
    enabled: !!selectedBlueprintId,
  });

  // 进行中批次进度轮询（终态停止）
  const progressQuery = useQuery({
    queryKey: ['theorygen-progress', activeBatchId],
    queryFn: () => getGenProgress(activeBatchId!),
    enabled: !!activeBatchId,
    refetchInterval: (q) => {
      const s = q.state.data?.status;
      return s === 'running' || s === 'pending' ? 3000 : false;
    },
  });

  const submitBatch = async () => {
    if (!selectedBlueprintId || !modelConfigId) {
      message.warning('请选择细目表批次与模型');
      return;
    }
    const quota: Record<string, number> = {};
    if (quotaSingle != null) quota.single = quotaSingle;
    if (quotaJudge != null) quota.judge = quotaJudge;
    if (quotaMulti != null) quota.multi = quotaMulti;
    try {
      const b = await submitGenBatch(selectedBlueprintId, modelConfigId,
        Object.keys(quota).length ? quota : undefined);
      message.success('批次已提交');
      setActiveBatchId(b.id);
      qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
    } catch { /* 拦截器提示 */ }
  };

  const batchColumns = [
    { title: '批次', dataIndex: 'id', key: 'id', width: 200, ellipsis: true },
    { title: '模型', dataIndex: 'modelId', key: 'modelId', width: 140 },
    { title: '思考', dataIndex: 'thinking', key: 'thinking', width: 80,
      render: (v: string) => (v === 'enabled' ? <Tag color="purple">开</Tag> : <Tag>关</Tag>) },
    { title: '进度', key: 'progress', width: 160,
      render: (_: unknown, r: TheoryGenBatch) => `${r.donePoints}/${r.totalPoints} 点 · ${r.totalQuestions} 题` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90,
      render: (v: string) => {
        const t = STATUS_TAG[v] ?? { color: 'default', text: v };
        return <Tag color={t.color}>{t.text}</Tag>;
      } },
    { title: '失败原因', dataIndex: 'failReason', key: 'failReason', ellipsis: true },
    { title: '操作', key: 'op', width: 220, render: (_: unknown, r: TheoryGenBatch) => (
      <Space>
        {r.status === 'running' && (
          <Button size="small" onClick={async () => { await pauseGenBatch(r.id); qc.invalidateQueries({ queryKey: ['theorygen-batches'] }); }}>暂停</Button>
        )}
        {r.status === 'paused' && (
          <Button size="small" type="primary" onClick={async () => { await resumeGenBatch(r.id); qc.invalidateQueries({ queryKey: ['theorygen-batches'] }); }}>继续</Button>
        )}
        {r.status === 'failed' && (
          <Button size="small" onClick={async () => { await retryGenBatch(r.id); qc.invalidateQueries({ queryKey: ['theorygen-batches'] }); }}>重试失败</Button>
        )}
        <Button size="small" onClick={() => { setActiveBatchId(r.id); setTab('draft'); }}>草稿</Button>
      </Space>
    ) },
  ];

  // ---------------- 页签3：试题草稿 ----------------
  const [draftQuery, setDraftQuery] = useState<{ batchId?: string; reviewStatus?: string; questionType?: string }>({});
  const [editingDraft, setEditingDraft] = useState<TheoryDraft | null>(null);
  const [editForm] = Form.useForm();
  const [selectedDraftKeys, setSelectedDraftKeys] = useState<React.Key[]>([]);
  const [draftPage, setDraftPage] = useState(1);

  const draftsQuery = useQuery({
    queryKey: ['theorygen-drafts', draftQuery, draftPage],
    queryFn: () => listDrafts({ ...draftQuery, batchId: draftQuery.batchId!, page: draftPage, pageSize: 20 }),
    enabled: !!draftQuery.batchId,
  });

  const draftColumns = [
    { title: '题干', dataIndex: 'content', key: 'content', ellipsis: true },
    { title: '题型', dataIndex: 'questionType', key: 'questionType', width: 90,
      render: (v: string) => TYPE_LABEL[v] ?? v },
    { title: '答案', dataIndex: 'answer', key: 'answer', width: 80 },
    { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 60 },
    { title: '审核', dataIndex: 'reviewStatus', key: 'reviewStatus', width: 80,
      render: (v: string) => (v === 'approved' ? <Tag color="success">已通过</Tag>
        : v === 'rejected' ? <Tag color="error">已驳回</Tag> : <Tag>待审</Tag>) },
    { title: '入库', dataIndex: 'publishStatus', key: 'publishStatus', width: 70,
      render: (v: number) => (v === 1 ? <Tag color="success">已入库</Tag> : <Tag>未入库</Tag>) },
    { title: '操作', key: 'op', width: 70, render: (_: unknown, r: TheoryDraft) => (
      <Button size="small" onClick={() => { setEditingDraft(r); editForm.setFieldsValue(r); }}>编辑</Button>
    ) },
  ];

  const saveDraftEdit = async () => {
    if (!editingDraft) return;
    const values = await editForm.validateFields();
    await editDraft(editingDraft.id, values);
    message.success('已保存');
    setEditingDraft(null);
    qc.invalidateQueries({ queryKey: ['theorygen-drafts'] });
  };

  const bulkReview = async (approve: boolean) => {
    if (!selectedDraftKeys.length) {
      message.warning('请先勾选草稿');
      return;
    }
    const n = await reviewDrafts(selectedDraftKeys as string[], approve);
    message.success(`已${approve ? '通过' : '驳回'} ${n} 条`);
    setSelectedDraftKeys([]);
    qc.invalidateQueries({ queryKey: ['theorygen-drafts'] });
  };

  // ---------------- 页签4：入库与导出 ----------------
  const [taskMode, setTaskMode] = useState<'create' | 'append'>('create');
  const [publishBatchId, setPublishBatchId] = useState<string | undefined>();

  const allBatches = useQuery({
    queryKey: ['theorygen-batches-all'],
    queryFn: () => listGenBatches({ page: 1, pageSize: 50 }),
  });

  const doPublish = async (confirmedBelowQuota?: boolean) => {
    if (!publishBatchId) {
      message.warning('请选择批次');
      return;
    }
    try {
      const r = await publishDrafts(publishBatchId, taskMode, undefined, undefined, confirmedBelowQuota);
      message.success(`入库成功：客观 ${r.objectiveCount} 题、主观 ${r.subjectiveCount} 题`);
      qc.invalidateQueries({ queryKey: ['theorygen-drafts'] });
    } catch (e) {
      const msg = e instanceof Error ? e.message : '';
      if (msg.includes('confirmedBelowQuota')) {
        Modal.confirm({
          title: '低于指引题量下限',
          content: '审核通过题量低于批次配额，确认仍要入库放行吗？（将留痕）',
          onOk: () => doPublish(true),
        });
      }
    }
  };

  const doExport = async () => {
    if (!publishBatchId) {
      message.warning('请选择批次');
      return;
    }
    const blob = await exportDrafts(publishBatchId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `theory-questions-${publishBatchId}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // ---------------- 渲染 ----------------
  const renderTree = (nodes: BlueprintNode[], depth = 0): React.ReactNode[] =>
    nodes.map((n) => (
      <Tree.TreeNode
        key={n.id}
        title={`${n.fullCode} ${n.name}${n.importance ? `（${n.importance}）` : ''}`}
      >
        {n.children?.length ? renderTree(n.children, depth + 1) : null}
      </Tree.TreeNode>
    ));

  return (
    <div className="ximu-page">
      <Tabs activeKey={tab} onChange={setTab} items={[
        {
          key: 'import', label: '细目表导入',
          children: (
            <>
              <Space style={{ marginBottom: 16 }} wrap>
                <Select style={{ width: 220 }} placeholder="选择工种"
                  options={workTypeOptions}
                  value={workTypeId}
                  onChange={(v) => setWorkTypeId(v)} />
                <Select style={{ width: 160 }} placeholder="等级" options={LEVEL_OPTIONS}
                  value={level} onChange={(v) => setLevel(v)} />
                <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={doImport}>
                  <Button type="primary" icon={<UploadOutlined />}>导入细目表 xlsx</Button>
                </Upload>
              </Space>
              <Table rowKey="id" size="small"
                loading={bpQuery.isLoading}
                dataSource={bpQuery.data?.items ?? []}
                columns={bpColumns}
                pagination={{ pageSize: 10, total: bpQuery.data?.total }} />
            </>
          ),
        },
        {
          key: 'gen', label: '生成任务',
          children: (
            <>
              <Space style={{ marginBottom: 8 }} wrap>
                <Select style={{ width: 320 }} placeholder="选择细目表批次"
                  value={selectedBlueprintId}
                  onChange={(v) => setSelectedBlueprintId(v)}
                  options={(bpQuery.data?.items ?? []).map((b) => ({
                    value: b.id, label: `${b.title}（${b.pointCount}点）` }))} />
                <Select style={{ width: 300 }} placeholder="选择模型配置"
                  value={modelConfigId}
                  onChange={(v) => setModelConfigId(v)}
                  loading={mcQuery.isLoading}
                  options={(mcQuery.data ?? []).map((c) => ({
                    value: c.id,
                    label: `${c.modelName}（${c.modelId}${c.thinking === 'enabled' ? ' · 思考开' : ''}）`,
                  }))} />
                <InputNumber min={0} addonBefore="单选" addonAfter="道/点" style={{ width: 170 }}
                  value={quotaSingle ?? undefined} onChange={(v) => setQuotaSingle(v)} />
                <InputNumber min={0} addonBefore="判断" addonAfter="道/点" style={{ width: 170 }}
                  value={quotaJudge ?? undefined} onChange={(v) => setQuotaJudge(v)} />
                <InputNumber min={0} addonBefore="多选" addonAfter="道/点" style={{ width: 170 }}
                  disabled={multiDisabled} value={quotaMulti ?? undefined}
                  onChange={(v) => setQuotaMulti(v)} />
                <Button type="primary" onClick={submitBatch}>提交生成</Button>
              </Space>
              <div style={{ marginBottom: 12, color: '#888', fontSize: 12 }}>
                「道/点」= 每个考评点生成的题数。已按《指引》下限自动填入推荐值：
                五级/四级 单选4+判断2（无多选题型）；三级~一级 单选3+多选2+判断2，均可上调。
                例：200 个考评点 ×（4+2）= 1200 题，达到五级总量 ≥1200 题要求。
              </div>
              <Table rowKey="id" size="small"
                loading={batchQuery.isLoading}
                dataSource={batchQuery.data?.items ?? []}
                columns={batchColumns}
                pagination={{ pageSize: 10 }} />
              {progressQuery.data && (progressQuery.data.status === 'running'
                || progressQuery.data.status === 'pending') && (
                <Descriptions size="small" style={{ marginTop: 12 }} column={4}
                  title={`批次 ${progressQuery.data.id.slice(-8)} 进行中`}>
                  <Descriptions.Item label="已完成点">{progressQuery.data.donePoints}</Descriptions.Item>
                  <Descriptions.Item label="失败点">{progressQuery.data.failedPoints}</Descriptions.Item>
                  <Descriptions.Item label="已产题">{progressQuery.data.totalQuestions}</Descriptions.Item>
                  <Descriptions.Item label="总点数">{progressQuery.data.totalPoints}</Descriptions.Item>
                </Descriptions>
              )}
            </>
          ),
        },
        {
          key: 'draft', label: '试题草稿',
          children: (
            <>
              <Space style={{ marginBottom: 16 }} wrap>
                <Select style={{ width: 300 }} placeholder="选择生成批次"
                  value={draftQuery.batchId}
                  onChange={(v) => { setDraftQuery({ ...draftQuery, batchId: v }); setDraftPage(1); }}
                  options={(allBatches.data?.items ?? []).map((b) => ({
                    value: b.id, label: `${b.id.slice(-8)}（${b.status}）` }))} />
                <Select style={{ width: 120 }} allowClear placeholder="审核状态"
                  value={draftQuery.reviewStatus}
                  onChange={(v) => setDraftQuery({ ...draftQuery, reviewStatus: v })}
                  options={[
                    { value: 'draft', label: '待审' },
                    { value: 'approved', label: '已通过' },
                    { value: 'rejected', label: '已驳回' },
                  ]} />
                <Select style={{ width: 120 }} allowClear placeholder="题型"
                  value={draftQuery.questionType}
                  onChange={(v) => setDraftQuery({ ...draftQuery, questionType: v })}
                  options={Object.entries(TYPE_LABEL).map(([v, l]) => ({ value: v, label: l }))} />
                <Button onClick={() => bulkReview(true)}>批量通过</Button>
                <Button danger onClick={() => bulkReview(false)}>批量驳回</Button>
              </Space>
              <Table rowKey="id" size="small"
                rowSelection={{ selectedRowKeys: selectedDraftKeys,
                  onChange: (keys) => setSelectedDraftKeys(keys) }}
                loading={draftsQuery.isLoading}
                dataSource={draftsQuery.data?.items ?? []}
                columns={draftColumns}
                pagination={{ current: draftPage, pageSize: 20,
                  total: draftsQuery.data?.total ?? 0,
                  onChange: (p) => setDraftPage(p) }} />
              <Drawer open={!!editingDraft} width={560}
                title="编辑草稿"
                onClose={() => setEditingDraft(null)}
                extra={<Button type="primary" onClick={saveDraftEdit}>保存</Button>}>
                {editingDraft && (
                  <Form form={editForm} layout="vertical">
                    <Form.Item name="content" label="题干">
                      <Input.TextArea rows={3} />
                    </Form.Item>
                    <Form.Item name="answer" label="答案">
                      <Input />
                    </Form.Item>
                    <Form.Item name="difficulty" label="难度（1~5）">
                      <InputNumber min={1} max={5} />
                    </Form.Item>
                    <Form.Item name="analysis" label="解析">
                      <Input.TextArea rows={2} />
                    </Form.Item>
                    <Form.Item name="referenceAnswer" label="参考答案（主观题）">
                      <Input.TextArea rows={3} />
                    </Form.Item>
                    <Form.Item name="scorePoints" label="评分要点（主观题）">
                      <Input.TextArea rows={2} />
                    </Form.Item>
                    <Form.Item name="scoringStandard" label="评分标准（主观题）">
                      <Input.TextArea rows={2} />
                    </Form.Item>
                  </Form>
                )}
              </Drawer>
            </>
          ),
        },
        {
          key: 'publish', label: '入库与导出',
          children: (
            <>
              <Space style={{ marginBottom: 16 }} wrap>
                <Select style={{ width: 300 }} placeholder="选择生成批次"
                  value={publishBatchId} onChange={setPublishBatchId}
                  options={(allBatches.data?.items ?? []).map((b) => ({
                    value: b.id, label: `${b.id.slice(-8)}（${b.status}）` }))} />
                <Select style={{ width: 160 }} value={taskMode} onChange={setTaskMode}
                  options={[
                    { value: 'create', label: '新建命题任务' },
                    { value: 'append', label: '追加现有任务' },
                  ]} />
                <Button type="primary"
                  onClick={() => doPublish(false)}>入库（写入题库开发任务）</Button>
                <Button onClick={doExport}>导出试题表 xlsx</Button>
              </Space>
              <Space size={40}>
                <Statistic title="说明" valueRender={() => (
                  <span style={{ color: '#888' }}>
                    入库仅写入「审核通过」的草稿；低于批次配额时会二次确认并留痕。
                  </span>
                )} />
              </Space>
            </>
          ),
        },
      ]} />
    </div>
  );
};

export default TheoryQuestionGenPage;

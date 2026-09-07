/**
 * 理论知识试题生成管理页面 —— 方案 v1.2 §T5
 * 四页签：细目表导入 / 生成任务 / 试题草稿 / 入库与导出
 */
import React, { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  Button, Descriptions, Drawer, Form, Input, InputNumber, message, Modal, Popconfirm,
  Progress, Select, Space, Statistic, Table, Tabs, Tag, Tree, Upload,
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

/** 指引难度档位（方案 §2.3）：1易 2较易 3中 4较难 5难 */
const DIFFICULTY_LABEL: Record<number, string> = {
  1: '易', 2: '较易', 3: '中', 4: '较难', 5: '难',
};

/** 草稿 options 为 JSON 字符串（[{"label":"A","content":"..."}]），零信任解析：失败返回空 */
const parseDraftOptions = (raw?: string | null): Array<{ label: string; content: string }> => {
  if (!raw) return [];
  try {
    const arr = JSON.parse(raw);
    return Array.isArray(arr)
      ? arr.filter((o: { label?: string; content?: string }) => o && o.label && o.content)
      : [];
  } catch {
    return [];
  }
};

/** 批次下拉选项 label：导入时间前置——同工种同等级重复导入时批次标题完全相同，导入时间是最可靠的区分信息；
 * 时间放首位保证标题超长被截断时时间仍可见 */
const bpOptionLabel = (b: TheoryBlueprint): string => {
  const time = b.createdAt ? dayjs(b.createdAt).format('MM-DD HH:mm') : '时间未知';
  return `${time} · ${b.title}（${b.pointCount}点）`;
};

/** 生成批次下拉选项 label：开始时间 + 批次短号 + 中文状态（原「id（done）」纯英文状态令用户困惑）；
 * 时间放首位保证超长截断时仍可区分 */
const genBatchOptionLabel = (b: TheoryGenBatch): string => {
  const time = b.startedAt ?? b.finishedAt;
  const timeText = time ? dayjs(time).format('MM-DD HH:mm') : '时间未知';
  const statusText = STATUS_TAG[b.status]?.text ?? b.status;
  return `${timeText} · 批次${b.id.slice(-8)}（${statusText}）`;
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
  const [bpPage, setBpPage] = useState(1);
  const [bpPageSize, setBpPageSize] = useState(20);
  // 导入进度（同步接口无服务端进度回调，模拟进度渐近 95%、完成跳 100%，参照上传标准 docx 模式）
  const [importing, setImporting] = useState(false);
  const [importPercent, setImportPercent] = useState(0);
  const importTimerRef = React.useRef<number | null>(null);
  const qc = useQueryClient();

  const wtTree = useQuery({
    queryKey: ['theorygen-worktypes'],
    queryFn: treeWorkTypes,
  });

  const bpQuery = useQuery({
    queryKey: ['theorygen-blueprints', bpPage, bpPageSize],
    queryFn: () => listBlueprints({ page: bpPage, pageSize: bpPageSize }),
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
    setImporting(true);
    setImportPercent(0);
    if (importTimerRef.current !== null) window.clearInterval(importTimerRef.current);
    importTimerRef.current = window.setInterval(() => {
      setImportPercent((p) =>
        p >= 95 ? p : Math.min(95, p + Math.max(1, Math.round((95 - p) / 10))),
      );
    }, 300);
    try {
      const bp = await importBlueprint(file, workTypeId, level);
      message.success(`导入成功：考评点 ${bp.pointCount} 个，X:Y:Z=${bp.xyzRatio ?? '-'}`);
      qc.invalidateQueries({ queryKey: ['theorygen-blueprints'] });
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      if (importTimerRef.current !== null) {
        window.clearInterval(importTimerRef.current);
        importTimerRef.current = null;
      }
      setImportPercent(100);
      window.setTimeout(() => {
        setImporting(false);
        setImportPercent(0);
      }, 500);
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
    { title: '导入时间', dataIndex: 'createdAt', key: 'createdAt', width: 150,
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-') },
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

  // 批次下拉用全量查询：页签1的 bpQuery 是分页数据（20/页），翻页后下拉会缺批次甚至丢失已选项。
  // 键共用 'theorygen-blueprints' 前缀，导入/删除后的 invalidateQueries(['theorygen-blueprints']) 会同时刷新两处
  const bpAllQuery = useQuery({
    queryKey: ['theorygen-blueprints', 'all'],
    queryFn: () => listBlueprints({ page: 1, pageSize: 200 }),
  });

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
    const bp = bpAllQuery.data?.items.find((b) => b.id === blueprintId);
    const d = bp ? QUOTA_DEFAULTS[bp.level] : undefined;
    setQuotaSingle(d?.single ?? null);
    setQuotaJudge(d?.judge ?? null);
    setQuotaMulti(d?.multi ?? null);
  }, [bpAllQuery.data]);

  React.useEffect(() => {
    applyQuotaDefaults(selectedBlueprintId);
  }, [selectedBlueprintId, applyQuotaDefaults]);

  const selectedBp = bpAllQuery.data?.items.find((b) => b.id === selectedBlueprintId);
  const multiDisabled = selectedBp ? selectedBp.level === 5 || selectedBp.level === 4 : false;

  const batchQuery = useQuery({
    queryKey: ['theorygen-batches', selectedBlueprintId],
    queryFn: () => listGenBatches({ blueprintId: selectedBlueprintId, page: 1, pageSize: 20 }),
    enabled: !!selectedBlueprintId,
    // 存在进行中批次时每 3s 轮询：进度数字跳动、完成后状态自动翻转（列表接口实时返回计数）
    refetchInterval: (q) => {
      const items = q.state.data?.items ?? [];
      return items.some((b) => b.status === 'running' || b.status === 'pending') ? 3000 : false;
    },
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

  // 页面刷新/重进后自动接管进行中批次：activeBatchId 原本只在提交批次时设置，刷新后丢失，
  // 导致底部进度面板与完成/失败自动联动全部失效。已指向终态批次时切换到新的进行中批次。
  React.useEffect(() => {
    const items = batchQuery.data?.items ?? [];
    const running = items.find((b) => b.status === 'running' || b.status === 'pending');
    if (!running) {
      return;
    }
    const current = activeBatchId ? items.find((b) => b.id === activeBatchId) : undefined;
    if (!activeBatchId || (current && current.status !== 'running' && current.status !== 'pending')) {
      setActiveBatchId(running.id);
    }
  }, [activeBatchId, batchQuery.data]);

  // 批次终态联动：生成中 → 已完成时自动跳"试题草稿"页签并按批次加载题目列表；
  // → 失败时提示失败原因（留在本页签，可"重试失败"）。仅对跃迁触发，手动查看历史批次不跳。
  const prevBatchStatusRef = React.useRef<string | undefined>();
  React.useEffect(() => {
    const status = progressQuery.data?.status;
    if (!status || !activeBatchId) {
      return;
    }
    const prev = prevBatchStatusRef.current;
    prevBatchStatusRef.current = status;
    if ((prev === 'running' || prev === 'pending') && status === 'done') {
      message.success('生成批次已完成，已为您加载试题草稿');
      setDraftQuery({ batchId: activeBatchId });
      setDraftPage(1);
      setSelectedDraftKeys([]);
      setTab('draft');
      qc.invalidateQueries({ queryKey: ['theorygen-drafts'] });
      qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
    } else if ((prev === 'running' || prev === 'pending') && status === 'failed') {
      qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
      message.warning({
        content: `生成批次失败：${progressQuery.data?.failReason ?? '未知原因'}`,
        duration: 6,
      });
    }
  }, [progressQuery.data?.status, progressQuery.data?.failReason, activeBatchId, qc]);

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
    { title: '操作', key: 'op', width: 250, render: (_: unknown, r: TheoryGenBatch) => (
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
        {/* 自动补跑轮次耗尽后仍缺口的完成批次：人工兜底补跑（后端仅跑缺口点，已完成点自动跳过） */}
        {r.status === 'done' && r.donePoints < r.totalPoints && (
          <Button size="small" onClick={async () => { await retryGenBatch(r.id); qc.invalidateQueries({ queryKey: ['theorygen-batches'] }); }}>补跑缺口</Button>
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
  const [draftPageSize, setDraftPageSize] = useState(20);

  const draftsQuery = useQuery({
    queryKey: ['theorygen-drafts', draftQuery, draftPage, draftPageSize],
    queryFn: () => listDrafts({ ...draftQuery, batchId: draftQuery.batchId!, page: draftPage, pageSize: draftPageSize }),
    enabled: !!draftQuery.batchId,
  });

  const draftColumns = [
    { title: '题干', dataIndex: 'content', key: 'content', ellipsis: true },
    { title: '题型', dataIndex: 'questionType', key: 'questionType', width: 90,
      render: (v: string) => TYPE_LABEL[v] ?? v },
    { title: '答案', dataIndex: 'answer', key: 'answer', width: 80 },
    { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 70,
      render: (v: number) => DIFFICULTY_LABEL[v] ?? v },
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
  const [exporting, setExporting] = useState(false);

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
      if (msg.includes('需二次确认')) {
        Modal.confirm({
          title: '题目数量未达本批次计划',
          content: `${msg.replace('【需二次确认】', '')}。确认要继续入库吗？继续后本次操作会被记录备查。`,
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
    setExporting(true);
    try {
      const blob = await exportDrafts(publishBatchId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `theory-questions-${publishBatchId}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
      message.success('试题表已导出');
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      setExporting(false);
    }
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
                <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={doImport}
                  disabled={importing}>
                  <Button type="primary" icon={<UploadOutlined />} loading={importing}>
                    {importing ? '导入中…' : '导入细目表 xlsx'}
                  </Button>
                </Upload>
                {importing && (
                  <>
                    <Progress percent={importPercent} size="small" status="active"
                      style={{ width: 160, margin: 0 }} />
                    <span style={{ color: '#888', fontSize: 12 }}>正在解析细目表，请勿关闭页面…</span>
                  </>
                )}
              </Space>
              <Table rowKey="id" size="small"
                loading={bpQuery.isLoading}
                dataSource={bpQuery.data?.items ?? []}
                columns={bpColumns}
                pagination={{
                  current: bpPage,
                  pageSize: bpPageSize,
                  total: bpQuery.data?.total,
                  showSizeChanger: true,
                  pageSizeOptions: [20, 30, 40, 50],
                  showTotal: (t: number) => `共 ${t} 条`,
                  onChange: (p, s) => {
                    setBpPage(s !== bpPageSize ? 1 : p);
                    setBpPageSize(s);
                  },
                }} />
            </>
          ),
        },
        {
          key: 'gen', label: '生成任务',
          children: (
            <>
              <Space style={{ marginBottom: 8 }} wrap>
                <Select style={{ width: 320 }} placeholder="选择细目表批次" showSearch
                  optionFilterProp="label"
                  value={selectedBlueprintId}
                  onChange={(v) => setSelectedBlueprintId(v)}
                  options={(bpAllQuery.data?.items ?? []).map((b) => ({
                    value: b.id, label: bpOptionLabel(b) }))} />
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
                「道/点」是指细目表中每个考评点要生成的题目数量。系统已按题库开发的通用规范自动填入推荐值：
                五级、四级每个考评点生成 4 道单选题 + 2 道判断题（这两个等级不设多选题）；
                三级~一级每个考评点生成 3 道单选题 + 2 道多选题 + 2 道判断题。
                数值可手动调大，不建议调低——题量偏少会使该等级的题目总数达不到鉴定中心的最低要求
                （如五级、四级全库不少于 1200 题：200 个考评点 × 6 题 = 1200 题，按推荐值正好达标）。
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
                <Select style={{ width: 360 }} placeholder="选择生成批次"
                  value={draftQuery.batchId}
                  onChange={(v) => { setDraftQuery({ ...draftQuery, batchId: v }); setDraftPage(1); }}
                  options={(allBatches.data?.items ?? []).map((b) => ({
                    value: b.id, label: genBatchOptionLabel(b) }))} />
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
                expandable={{
                  // 选项是重要审核内容：行展开显示全部选项，正确答案高亮（客观题才有选项）
                  expandedRowRender: (r: TheoryDraft) => {
                    const opts = parseDraftOptions(r.options);
                    return (
                      <div style={{ display: 'grid', gap: 4, padding: '4px 0' }}>
                        {opts.map((o) => (
                          <div key={o.label}
                            style={{ color: r.answer?.includes(o.label) ? '#389e0d' : undefined }}>
                            <strong>{o.label}.</strong> {o.content}
                            {r.answer?.includes(o.label) && (
                              <Tag color="success" style={{ marginLeft: 8 }}>正确答案</Tag>
                            )}
                          </div>
                        ))}
                      </div>
                    );
                  },
                  rowExpandable: (r: TheoryDraft) => parseDraftOptions(r.options).length > 0,
                }}
                loading={draftsQuery.isLoading}
                dataSource={draftsQuery.data?.items ?? []}
                columns={draftColumns}
                pagination={{
                  current: draftPage,
                  pageSize: draftPageSize,
                  total: draftsQuery.data?.total ?? 0,
                  showSizeChanger: true,
                  pageSizeOptions: [20, 30, 40, 50],
                  showTotal: (t: number) => `共 ${t} 条`,
                  onChange: (p, s) => {
                    setDraftPage(s !== draftPageSize ? 1 : p);
                    setDraftPageSize(s);
                  },
                }} />
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
                <Select style={{ width: 360 }} placeholder="选择生成批次"
                  value={publishBatchId} onChange={setPublishBatchId}
                  options={(allBatches.data?.items ?? []).map((b) => ({
                    value: b.id, label: genBatchOptionLabel(b) }))} />
                <Select style={{ width: 160 }} value={taskMode} onChange={setTaskMode}
                  options={[
                    { value: 'create', label: '新建命题任务' },
                    { value: 'append', label: '追加现有任务' },
                  ]} />
                <Button type="primary"
                  onClick={() => doPublish(false)}>入库（写入题库开发任务）</Button>
                <Button loading={exporting} onClick={doExport}>导出试题表 xlsx</Button>
              </Space>
              <Space size={40}>
                <Statistic title="说明" valueRender={() => (
                  <span style={{ color: '#888' }}>
                    点击入库后，只有「已通过审核」的题目会写入命题任务（驳回和未审核的不会写入）。
                    如果本批次计划生成的题目没有全部审核通过（例如部分题目被驳回），入库时会先弹出提示与你确认，
                    你确认继续后系统会记录这次操作备查。想补足数量，可先到「试题草稿」处理未通过的题目。
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

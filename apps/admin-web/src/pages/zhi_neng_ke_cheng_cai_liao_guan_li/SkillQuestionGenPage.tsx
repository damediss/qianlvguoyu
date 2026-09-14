/**
 * 操作技能试题生成管理页面 —— 方案 v1.1 §T5
 * 两页签：细目表导入（13 列表格上传/校验报告/源表下载/行内"生成"）+ 生成工作台
 * （发起区固定 5 题/点 + 批次总览 + 草稿审核三件套/评分表编辑 + 入库闸门 UI）。
 */
import React, { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  Alert, Button, Drawer, Input, InputNumber, message, Modal, Popconfirm, Popover, Progress,
  Radio, Select, Space, Table, Tabs, Tag, Tooltip, Upload,
} from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { treeWorkTypes } from '../../api/exam';
import type { ExamWorkType } from '../../types/exam';
import {
  deleteSkillBlueprint, downloadSkillBlueprintSource, editSkillDraft, exportSkillDrafts,
  getSkillGenProgress, getSkillPublishPreview, importSkillBlueprint, listSkillBlueprints,
  listSkillDrafts, listSkillGenBatches, listSkillModelConfigs, pauseSkillGenBatch,
  publishSkillDrafts, resumeSkillGenBatch, retrySkillGenBatch, reviewSkillDrafts,
  submitSkillGenBatch,
} from './skillGenApi';
import type { SkillBlueprint, SkillDraft, SkillGenBatch, SkillScoringItem } from './skillGenApi';
import { LEVEL_NAME, STATUS_TAG, parseCheckReport } from './workbench/constants';
import './workbench/GenWorkbench.css';

const LEVEL_OPTIONS = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

/** 每考评点固定题量（与后端 SkillBlueprintApplicationService.PER_POINT_QUOTA 一致） */
const PER_POINT_QUOTA = 5;
/** 等级 → 题库总量下限（后端 MIN_QUESTIONS 同口径） */
const MIN_QUESTIONS: Record<number, number> = { 1: 30, 2: 30, 3: 50, 4: 50, 5: 50 };

const parseScoring = (raw?: string | null): SkillScoringItem[] => {
  if (!raw) return [];
  try {
    const arr = JSON.parse(raw);
    return Array.isArray(arr)
      ? arr.map((o: Partial<SkillScoringItem>) => ({
        item: o?.item ?? '', score: String(o?.score ?? ''), scoreDesc: o?.scoreDesc ?? '',
        maxDeduct: String(o?.maxDeduct ?? ''), step: String(o?.step ?? ''),
      }))
      : [];
  } catch {
    return [];
  }
};

const scoringSum = (rows: SkillScoringItem[]) =>
  rows.reduce((acc, r) => acc + (Number(String(r.score).replace('分', '')) || 0), 0);

const REVIEW_TAG: Record<string, { color: string; text: string }> = {
  draft: { color: 'default', text: '待审' },
  approved: { color: 'success', text: '已通过' },
  rejected: { color: 'error', text: '已驳回' },
};

/**
 * "失败 N" 悬停说明（2026-09-10 用户确认）：
 * 该计数是块级生成失败的累计次数（只增不减，重试成功也不回退），是否已自愈取决于是否仍有缺口。
 */
const failedHint = (b: SkillGenBatch) => {
  const gap = Math.max(0, (b.totalPoints ?? 0) - (b.donePoints ?? 0));
  return gap > 0
    ? `生成失败累计 ${b.failedPoints} 次，仍有 ${gap} 个考评点未生成——可点行内「补跑缺口」补跑`
    : `生成失败累计 ${b.failedPoints} 次（均已自动补跑成功，当前无缺口）`;
};

const SkillQuestionGenPage: React.FC = () => {
  const [tab, setTab] = useState('import');
  const qc = useQueryClient();

  // ---------------- 页签1：细目表导入 ----------------
  const [workTypeId, setWorkTypeId] = useState<number | undefined>();
  const [level, setLevel] = useState<number | undefined>(2);
  const [importing, setImporting] = useState(false);
  const [importPercent, setImportPercent] = useState(0);
  const importTimerRef = React.useRef<number | null>(null);
  const [downloadingBpId, setDownloadingBpId] = useState<string | undefined>();
  const [presetBpId, setPresetBpId] = useState<string | undefined>();

  const wtTree = useQuery({ queryKey: ['skillgen-worktypes'], queryFn: treeWorkTypes });
  const bpQuery = useQuery({
    queryKey: ['skillgen-blueprints'],
    queryFn: () => listSkillBlueprints({ page: 1, pageSize: 100 }),
  });

  const workTypeOptions = useMemo(() => {
    const rows: Array<{ id: number; title: string; path: string }> = [];
    const walk = (list: ExamWorkType[], prefix: string) => {
      for (const w of list ?? []) {
        const path = prefix ? `${prefix}/${w.title}` : w.title;
        rows.push({ id: w.id, title: w.title, path });
        if (w.children?.length) walk(w.children, path);
      }
    };
    walk(wtTree.data ?? [], '');
    const titleCount = new Map<string, number>();
    for (const r of rows) titleCount.set(r.title, (titleCount.get(r.title) ?? 0) + 1);
    return rows.map((r) => ({
      value: r.id,
      label: (titleCount.get(r.title) ?? 0) > 1 ? r.path : r.title,
    }));
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
      setImportPercent((p) => (p >= 95 ? p : Math.min(95, p + Math.max(1, Math.round((95 - p) / 10)))));
    }, 300);
    try {
      const bp = await importSkillBlueprint(file, workTypeId, level);
      const errs = parseCheckReport(bp.checkReport);
      if (errs.length) {
        Modal.warning({
          title: `导入完成，有 ${errs.length} 条提示`,
          width: 560,
          content: (
            <div>
              <div style={{ marginBottom: 8, color: '#888' }}>以下内容请对照源文件核对，修正后可重新导入：</div>
              <ul style={{ margin: 0, paddingLeft: 18 }}>
                {errs.map((e, i) => <li key={i}>{e}</li>)}
              </ul>
            </div>
          ),
        });
      } else {
        message.success(`导入成功：考评点 ${bp.pointCount} 个，X:Y:Z=${bp.xyzRatio ?? '-'}`);
      }
      qc.invalidateQueries({ queryKey: ['skillgen-blueprints'] });
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      if (importTimerRef.current !== null) window.clearInterval(importTimerRef.current);
      setImportPercent(100);
      setImporting(false);
      window.setTimeout(() => setImportPercent(0), 600);
    }
    return false;
  };

  const toWorkbench = (bp: SkillBlueprint) => {
    setPresetBpId(bp.id);
    setTab('workbench');
  };

  return (
    <div style={{ padding: 16 }}>
      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          {
            key: 'import',
            label: '细目表导入',
            children: (
              <div>
                <Space wrap style={{ marginBottom: 12 }}>
                  <Select
                    showSearch
                    optionFilterProp="label"
                    style={{ width: 280 }}
                    placeholder="选择工种"
                    options={workTypeOptions}
                    value={workTypeId}
                    onChange={(v) => setWorkTypeId(v)}
                  />
                  <Select
                    style={{ width: 160 }}
                    placeholder="选择等级"
                    options={LEVEL_OPTIONS}
                    value={level}
                    onChange={(v) => setLevel(v)}
                  />
                  <Upload
                    accept=".xlsx,.xls"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      void doImport(file as unknown as File);
                      return false;
                    }}
                    disabled={importing}
                  >
                    <Button type="primary" icon={<UploadOutlined />} loading={importing}>
                      {importing ? '解析中…' : '上传操作技能细目表'}
                    </Button>
                  </Upload>
                  {importing && (
                    <span style={{ width: 240 }}>
                      <Progress percent={importPercent} size="small" status="active" />
                      <span style={{ color: '#888' }}>正在解析标准文档，请勿关闭页面…</span>
                    </span>
                  )}
                </Space>
                <Table<SkillBlueprint>
                  rowKey="id"
                  size="small"
                  loading={bpQuery.isLoading}
                  dataSource={bpQuery.data?.items ?? []}
                  pagination={false}
                  columns={[
                    { title: '细目表批次', dataIndex: 'title', ellipsis: true },
                    {
                      title: '工种/等级', key: 'wl', width: 200,
                      render: (_: unknown, b: SkillBlueprint) =>
                        `${workTypeOptions.find((w) => w.value === b.workTypeId)?.label ?? b.workTypeId} · ${LEVEL_NAME[b.level] ?? b.level}`,
                    },
                    { title: '考评点', dataIndex: 'pointCount', width: 80 },
                    {
                      title: '预计产出', key: 'plan', width: 110,
                      render: (_: unknown, b: SkillBlueprint) => `${b.pointCount * PER_POINT_QUOTA} 题`,
                    },
                    { title: 'X:Y:Z', dataIndex: 'xyzRatio', width: 100 },
                    {
                      title: '校验', key: 'check', width: 110,
                      render: (_: unknown, b: SkillBlueprint) => {
                        const errs = parseCheckReport(b.checkReport);
                        if (!errs.length) return <Tag color="success">通过</Tag>;
                        return (
                          <Popover
                            title="校验提示"
                            content={(
                              <ul style={{ margin: 0, paddingLeft: 18, maxWidth: 420 }}>
                                {errs.map((e, i) => <li key={i}>{e}</li>)}
                              </ul>
                            )}
                          >
                            <Tag color={b.weightCheck === 1 ? 'warning' : 'error'}>
                              {b.weightCheck === 1 ? `${errs.length} 条提示` : `未通过（${errs.length}）`}
                            </Tag>
                          </Popover>
                        );
                      },
                    },
                    {
                      title: '导入时间', key: 'time', width: 140,
                      render: (_: unknown, b: SkillBlueprint) =>
                        b.createdAt ? dayjs(b.createdAt).format('MM-DD HH:mm') : '-',
                    },
                    {
                      title: '操作', key: 'op', width: 260,
                      render: (_: unknown, b: SkillBlueprint) => (
                        <Space size={4}>
                          <Button size="small" type="primary" onClick={() => toWorkbench(b)}>生成</Button>
                          <Button
                            size="small"
                            loading={downloadingBpId === b.id}
                            onClick={async () => {
                              setDownloadingBpId(b.id);
                              try {
                                const blob = await downloadSkillBlueprintSource(b.id);
                                const url = URL.createObjectURL(blob);
                                const a = document.createElement('a');
                                a.href = url;
                                a.download = b.sourceFileName ?? '细目表.xlsx';
                                a.click();
                                URL.revokeObjectURL(url);
                              } finally {
                                setDownloadingBpId(undefined);
                              }
                            }}
                          >
                            源表下载
                          </Button>
                          <Popconfirm
                            title="删除该细目表批次？"
                            onConfirm={async () => {
                              await deleteSkillBlueprint(b.id);
                              message.success('已删除');
                              qc.invalidateQueries({ queryKey: ['skillgen-blueprints'] });
                            }}
                          >
                            <Button size="small" danger>删除</Button>
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
              </div>
            ),
          },
          {
            key: 'workbench',
            label: '生成工作台',
            // forceRender：停在其他页签时也保持挂载，进行中批次轮询不中断
            forceRender: true,
            children: (
              <SkillWorkbench
                workTypeOptions={workTypeOptions}
                blueprints={bpQuery.data?.items ?? []}
                presetBpId={presetBpId}
              />
            ),
          },
        ]}
      />
    </div>
  );
};

// ---------------------------------------------------------------------------
// 生成工作台
// ---------------------------------------------------------------------------

interface WorkbenchProps {
  workTypeOptions: Array<{ value: number; label: string }>;
  blueprints: SkillBlueprint[];
  presetBpId?: string;
}

const SkillWorkbench: React.FC<WorkbenchProps> = ({ workTypeOptions, blueprints, presetBpId }) => {
  const qc = useQueryClient();
  const [blueprintId, setBlueprintId] = useState<string | undefined>(presetBpId);
  const [modelConfigId, setModelConfigId] = useState<string | undefined>();
  const [submitting, setSubmitting] = useState(false);
  const [activeBatchId, setActiveBatchId] = useState<string | undefined>();
  const [allBatches, setAllBatches] = useState(false);
  const [reviewFilter, setReviewFilter] = useState<string | undefined>('draft');
  const [selectedDraftIds, setSelectedDraftIds] = useState<string[]>([]);
  const [editing, setEditing] = useState<SkillDraft | undefined>();
  const [publishTarget, setPublishTarget] = useState<SkillGenBatch | undefined>();

  React.useEffect(() => {
    if (presetBpId) setBlueprintId(presetBpId);
  }, [presetBpId]);

  const modelsQuery = useQuery({
    queryKey: ['skillgen-models'],
    queryFn: listSkillModelConfigs,
  });
  const modelOptions = useMemo(
    () => (modelsQuery.data ?? []).map((m) => ({
      value: m.id,
      label: `${m.modelName}（${m.modelId}${m.thinking ? ` · thinking=${m.thinking}` : ''}${m.reasoningEffort ? ` · effort=${m.reasoningEffort}` : ''}）`,
    })),
    [modelsQuery.data],
  );
  React.useEffect(() => {
    if (!modelConfigId && modelOptions.length) {
      const preferred = modelOptions.find((o) => o.label.includes('skill-gen')
        || o.label.includes('GLM-5.3'));
      setModelConfigId((preferred ?? modelOptions[0]).value);
    }
  }, [modelOptions, modelConfigId]);

  const bp = blueprints.find((b) => b.id === blueprintId);

  // 批次总览：进行中区轮询
  const activeQuery = useQuery({
    queryKey: ['skillgen-batches-active'],
    queryFn: () => listSkillGenBatches({ status: 'pending,running', page: 1, pageSize: 50 }),
    refetchInterval: (q) => {
      const rows = (q.state.data as { items?: SkillGenBatch[] } | undefined)?.items ?? [];
      return rows.some((b) => b.status === 'pending' || b.status === 'running') ? 3000 : false;
    },
  });
  const historyQuery = useQuery({
    queryKey: ['skillgen-batches-history'],
    // 终态批次服务端过滤（对齐 theorygen 工作台）：与进行中区互斥，
    // 避免提交时把"生成中"快照写进历史缓存、批次完成后永远显示旧状态
    queryFn: () => listSkillGenBatches({ status: 'done,failed,paused', page: 1, pageSize: 20 }),
  });
  const batchRows = useMemo(() => {
    const active = activeQuery.data?.items ?? [];
    const activeIds = new Set(active.map((b) => b.id));
    const history = (historyQuery.data?.items ?? []).filter((b) => !activeIds.has(b.id));
    return [...active, ...history];
  }, [activeQuery.data, historyQuery.data]);

  // 终态联动：批次从进行中区消失（完成/失败/暂停）时刷新历史区与草稿计数，
  // 否则全局 refetchOnWindowFocus=false + 历史区无轮询会让旧行永远停在"生成中"
  const prevActiveIdsRef = React.useRef<Set<string>>(new Set());
  React.useEffect(() => {
    const current = new Set((activeQuery.data?.items ?? []).map((b) => b.id));
    const prev = prevActiveIdsRef.current;
    prevActiveIdsRef.current = current;
    if (prev.size === 0) {
      return; // 首次载入不触发
    }
    const left = [...prev].filter((id) => !current.has(id));
    if (left.length === 0) {
      return;
    }
    qc.invalidateQueries({ queryKey: ['skillgen-batches-history'] });
    qc.invalidateQueries({ queryKey: ['skillgen-drafts'] });
    left.forEach(async (id) => {
      try {
        const b = await getSkillGenProgress(id);
        if (b.status === 'failed') {
          message.warning({ content: `生成批次已结束（失败）：${b.failReason ?? '未知原因'}`, duration: 6 });
        } else if (b.status === 'done') {
          message.success(`生成批次已完成：已产出 ${b.totalQuestions ?? 0} 题`);
        }
      } catch {
        // 进度查询失败不打扰用户，列表刷新已足够
      }
    });
  }, [activeQuery.data, qc]);

  const draftsQuery = useQuery({
    queryKey: ['skillgen-drafts', allBatches ? 'all' : activeBatchId, reviewFilter],
    queryFn: () => listSkillDrafts({
      batchId: allBatches ? undefined : activeBatchId,
      reviewStatus: reviewFilter,
      page: 1,
      pageSize: 50,
    }),
    enabled: allBatches || !!activeBatchId,
  });

  const refreshAll = () => {
    qc.invalidateQueries({ queryKey: ['skillgen-batches-active'] });
    qc.invalidateQueries({ queryKey: ['skillgen-batches-history'] });
    qc.invalidateQueries({ queryKey: ['skillgen-drafts'] });
  };

  const doSubmit = async () => {
    if (!blueprintId || !modelConfigId) {
      message.warning('请选择细目表批次与模型');
      return;
    }
    setSubmitting(true);
    try {
      const batch = await submitSkillGenBatch(blueprintId, modelConfigId);
      message.success('生成任务已提交，可在下方批次表查看进度');
      setActiveBatchId(batch.id);
      refreshAll();
    } catch {
      // 预检失败（点数×5+已入库 < 等级下限）等错误提示由拦截器/后端消息展示
    } finally {
      setSubmitting(false);
    }
  };

  const openDrafts = (b: SkillGenBatch) => {
    setActiveBatchId(b.id);
    setAllBatches(false);
    setSelectedDraftIds([]);
  };

  return (
    <div>
      {/* 发起区 */}
      <div style={{ padding: 12, border: '1px solid #f0f0f0', borderRadius: 8, marginBottom: 12 }}>
        <Space wrap>
          <Select
            showSearch
            optionFilterProp="label"
            style={{ width: 420 }}
            placeholder="选择细目表批次"
            value={blueprintId}
            onChange={setBlueprintId}
            options={blueprints.map((b) => ({
              value: b.id,
              label: `${b.createdAt ? dayjs(b.createdAt).format('MM-DD HH:mm') : '时间未知'} · ${b.title}（${b.pointCount}点）`,
            }))}
          />
          <Select
            style={{ width: 340 }}
            placeholder="选择模型"
            value={modelConfigId}
            onChange={setModelConfigId}
            options={modelOptions}
          />
          <span style={{ color: '#555' }}>
            每考评点固定 <b>{PER_POINT_QUOTA}</b> 题
            {bp ? <>，本批预计产出 <b>{bp.pointCount * PER_POINT_QUOTA}</b> 题（等级下限 {MIN_QUESTIONS[bp.level] ?? '-'} 题）</> : null}
          </span>
          <Button type="primary" loading={submitting} onClick={doSubmit} disabled={!bp}>提交生成</Button>
        </Space>
      </div>

      {/* 批次总览 */}
      <Table<SkillGenBatch>
        rowKey="id"
        size="small"
        dataSource={batchRows}
        pagination={false}
        style={{ marginBottom: 12 }}
        columns={[
          {
            title: '批次', key: 'batch', width: 150,
            render: (_: unknown, b: SkillGenBatch) => {
              const time = b.startedAt ?? b.createdAt ?? b.finishedAt;
              return (
                <span>
                  {time ? dayjs(time).format('MM-DD HH:mm') : '-'}
                  <br />
                  <span style={{ color: '#999' }}>批次…{b.id.slice(-8)}</span>
                </span>
              );
            },
          },
          {
            title: '工种/等级', key: 'wl', width: 190,
            render: (_: unknown, b: SkillGenBatch) => {
              const bpRow = blueprints.find((x) => x.id === b.blueprintId);
              return `${workTypeOptions.find((w) => w.value === (b.workTypeId ?? bpRow?.workTypeId))?.label ?? '-'} · ${LEVEL_NAME[b.level ?? bpRow?.level ?? 0] ?? '-'}`;
            },
          },
          {
            title: '模型', key: 'model', width: 150, ellipsis: true,
            render: (_: unknown, b: SkillGenBatch) =>
              `${b.modelId}${b.thinking ? `（思考 ${b.thinking === 'enabled' ? '开' : '关'}）` : ''}`,
          },
          {
            title: '状态', key: 'status', width: 110,
            render: (_: unknown, b: SkillGenBatch) => {
              const tag = STATUS_TAG[b.status] ?? { color: 'default', text: b.status };
              const cls = b.status === 'pending' ? 'xmb-status-waiting'
                : b.status === 'running' ? 'xmb-status-running' : '';
              const node = <Tag color={tag.color} className={cls}>{tag.text}</Tag>;
              return b.status === 'failed' && b.failReason
                ? <Tooltip title={b.failReason}>{node}</Tooltip> : node;
            },
          },
          {
            title: '生成进度', key: 'progress', width: 230,
            render: (_: unknown, b: SkillGenBatch) => {
              const pct = b.totalPoints ? Math.round(((b.donePoints ?? 0) * 100) / b.totalPoints) : 0;
              return (
                <span>
                  <Progress
                    percent={pct}
                    size="small"
                    status={b.status === 'running' ? 'active' : undefined}
                    format={() => `${b.donePoints ?? 0}/${b.totalPoints ?? 0} 点`}
                  />
                  <span style={{ color: '#888' }}>已产题 {b.totalQuestions ?? 0} 题</span>
                  {b.failedPoints ? (
                    <Tooltip title={failedHint(b)}>
                      <Tag color="error" style={{ marginLeft: 4 }}>失败 {b.failedPoints}</Tag>
                    </Tooltip>
                  ) : null}
                </span>
              );
            },
          },
          {
            title: '审核进度', key: 'review', width: 170,
            render: (_: unknown, b: SkillGenBatch) =>
              `通过 ${b.approvedCount ?? 0} · 待审 ${b.pendingReviewCount ?? 0} · 驳回 ${b.rejectedCount ?? 0}`,
          },
          {
            title: '入库', key: 'publish', width: 100,
            render: (_: unknown, b: SkillGenBatch) => ((b.publishedCount ?? 0) > 0
              ? <Tag color="success">已入库 {b.publishedCount}</Tag>
              : <Tag>未入库</Tag>),
          },
          {
            title: '操作', key: 'op', width: 300, fixed: 'right' as const,
            render: (_: unknown, b: SkillGenBatch) => (
              <Space size={4} wrap>
                <Button size="small" type="primary" onClick={() => openDrafts(b)}>草稿</Button>
                {(b.status === 'running' || b.status === 'pending') && (
                  <Button size="small" onClick={async () => {
                    await pauseSkillGenBatch(b.id); message.success('已暂停'); refreshAll();
                  }}>暂停</Button>
                )}
                {b.status === 'paused' && (
                  <Button size="small" onClick={async () => {
                    await resumeSkillGenBatch(b.id); message.success('已继续'); refreshAll();
                  }}>继续</Button>
                )}
                {(b.status === 'failed' || (b.status === 'done' && (b.donePoints ?? 0) < (b.totalPoints ?? 0))) && (
                  <Button size="small" onClick={async () => {
                    await retrySkillGenBatch(b.id); message.success('已提交补跑'); refreshAll();
                  }}>补跑缺口</Button>
                )}
                <Button size="small" onClick={() => setPublishTarget(b)}>入库</Button>
                <Button size="small" loading={false} onClick={async () => {
                  // 文件名由服务端统一下发（工种(等级)_操作技能试题_批次{短号}.xlsx），前端不再自行拼装
                  const { blob, filename } = await exportSkillDrafts(b.id);
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = filename || `操作技能试题_批次${b.id.slice(-8)}.xlsx`;
                  a.click();
                  URL.revokeObjectURL(url);
                }}>导出</Button>
              </Space>
            ),
          },
        ]}
      />

      {/* 草稿审核面板 */}
      {(allBatches || activeBatchId) ? (
        <div style={{ border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
          <Space wrap style={{ marginBottom: 8 }}>
            <b>试题草稿</b>
            <Radio.Group
              value={allBatches}
              onChange={(e) => { setAllBatches(e.target.value); setSelectedDraftIds([]); }}
              optionType="button"
              options={[{ label: '当前批次', value: false }, { label: '全部批次', value: true }]}
            />
            <Select
              style={{ width: 140 }}
              value={reviewFilter}
              onChange={(v) => { setReviewFilter(v); setSelectedDraftIds([]); }}
              options={[
                { value: undefined, label: '全部状态' },
                { value: 'draft', label: '待审' },
                { value: 'approved', label: '已通过' },
                { value: 'rejected', label: '已驳回' },
              ]}
            />
            <Button size="small" type="primary" disabled={!selectedDraftIds.length} onClick={async () => {
              await reviewSkillDrafts(selectedDraftIds, true);
              message.success('已通过'); setSelectedDraftIds([]); refreshAll();
            }}>批量通过</Button>
            <Button size="small" danger disabled={!selectedDraftIds.length} onClick={async () => {
              await reviewSkillDrafts(selectedDraftIds, false);
              message.success('已驳回'); setSelectedDraftIds([]); refreshAll();
            }}>批量驳回</Button>
          </Space>
          <Table<SkillDraft>
            rowKey="id"
            size="small"
            loading={draftsQuery.isLoading}
            dataSource={draftsQuery.data?.items ?? []}
            pagination={{ pageSize: 20, showSizeChanger: true }}
            rowSelection={{
              selectedRowKeys: selectedDraftIds,
              onChange: (keys) => setSelectedDraftIds(keys as string[]),
              getCheckboxProps: (r) => ({ disabled: r.publishStatus === 1 }),
            }}
            expandable={{
              expandedRowRender: (d) => <DraftDetail draft={d} />,
            }}
            columns={[
              { title: '试题名称', dataIndex: 'questionName', ellipsis: true },
              {
                title: '分值/权重', key: 'score', width: 110,
                render: (_: unknown, d: SkillDraft) =>
                  `${d.score ?? 100} 分${d.weight != null ? ` / ${d.weight}%` : ''}`,
              },
              { title: '考核时间', key: 'duration', width: 90, render: (_: unknown, d: SkillDraft) => `${d.durationMinutes ?? '-'} min` },
              { title: '形式', dataIndex: 'examForm', width: 80 },
              {
                title: '难度', key: 'diff', width: 70,
                render: (_: unknown, d: SkillDraft) => d.difficulty ?? 3,
              },
              {
                title: '评分合计', key: 'sum', width: 90,
                render: (_: unknown, d: SkillDraft) => {
                  const sum = scoringSum(parseScoring(d.scoringItems));
                  return <Tag color={Math.abs(sum - 100) < 0.01 ? 'success' : 'error'}>{sum}</Tag>;
                },
              },
              {
                title: '状态', key: 'review', width: 90,
                render: (_: unknown, d: SkillDraft) => {
                  const tag = REVIEW_TAG[d.reviewStatus] ?? { color: 'default', text: d.reviewStatus };
                  return <Tag color={tag.color}>{tag.text}</Tag>;
                },
              },
              {
                title: '入库', key: 'pub', width: 80,
                render: (_: unknown, d: SkillDraft) => (d.publishStatus === 1 ? <Tag color="success">已入库</Tag> : <Tag>未入库</Tag>),
              },
              {
                title: '操作', key: 'op', width: 90,
                render: (_: unknown, d: SkillDraft) => (
                  <Button size="small" disabled={d.publishStatus === 1} onClick={() => setEditing(d)}>编辑</Button>
                ),
              },
            ]}
          />
        </div>
      ) : (
        <Alert type="info" showIcon message="点击批次表行内「草稿」进入审核面板" />
      )}

      {editing && (
        <DraftEditDrawer
          draft={editing}
          onClose={() => setEditing(undefined)}
          onSaved={() => { setEditing(undefined); refreshAll(); }}
        />
      )}

      {publishTarget && (
        <PublishModal
          batch={publishTarget}
          onClose={() => setPublishTarget(undefined)}
          onPublished={() => { setPublishTarget(undefined); refreshAll(); }}
        />
      )}
    </div>
  );
};

// ---------------------------------------------------------------------------
// 草稿展开行：三件套 + 评分表
// ---------------------------------------------------------------------------

const DraftDetail: React.FC<{ draft: SkillDraft }> = ({ draft }) => {
  const scoring = parseScoring(draft.scoringItems);
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
      <div>
        <b>具体考核要求</b>
        <div style={{ whiteSpace: 'pre-wrap' }}>{draft.requirements}</div>
        <b>否定项</b>
        <div style={{ whiteSpace: 'pre-wrap' }}>{draft.negatives}</div>
        <b>考生准备</b>
        <div style={{ whiteSpace: 'pre-wrap' }}>{draft.examineePrepare}</div>
      </div>
      <div>
        <b>考场准备（含设备设施）</b>
        <div style={{ whiteSpace: 'pre-wrap' }}>{draft.sitePreparation}</div>
      </div>
      <div style={{ gridColumn: '1 / span 2' }}>
        <b>评分记录表（合计 {scoringSum(scoring)} 分）</b>
        <Table<SkillScoringItem>
          rowKey={(_, i) => String(i)}
          size="small"
          pagination={false}
          dataSource={scoring}
          columns={[
            { title: '评分要素', dataIndex: 'item' },
            { title: '配分', dataIndex: 'score', width: 70 },
            { title: '评分标准', dataIndex: 'scoreDesc' },
            { title: '最大扣分', dataIndex: 'maxDeduct', width: 90 },
            { title: '步长', dataIndex: 'step', width: 70 },
          ]}
        />
      </div>
    </div>
  );
};

// ---------------------------------------------------------------------------
// 编辑抽屉（含评分行编辑器 + 配分合计校验；保存后自动退回待审）
// ---------------------------------------------------------------------------

const DraftEditDrawer: React.FC<{ draft: SkillDraft; onClose: () => void; onSaved: () => void }> = (
  { draft, onClose, onSaved },
) => {
  const [questionName, setQuestionName] = useState(draft.questionName);
  const [examForm, setExamForm] = useState(draft.examForm ?? '实操');
  const [durationMinutes, setDurationMinutes] = useState<number | null>(draft.durationMinutes ?? 20);
  const [difficulty, setDifficulty] = useState<number | null>(draft.difficulty ?? 3);
  const [requirements, setRequirements] = useState(draft.requirements ?? '');
  const [negatives, setNegatives] = useState(draft.negatives ?? '');
  const [examineePrepare, setExamineePrepare] = useState(draft.examineePrepare ?? '');
  const [sitePreparation, setSitePreparation] = useState(draft.sitePreparation ?? '');
  const [scoring, setScoring] = useState<SkillScoringItem[]>(parseScoring(draft.scoringItems));
  const [saving, setSaving] = useState(false);
  const sum = scoringSum(scoring);

  const save = async () => {
    if (Math.abs(sum - 100) > 0.01) {
      message.error(`评分表配分合计 ${sum}，必须等于 100`);
      return;
    }
    setSaving(true);
    try {
      await editSkillDraft(draft.id, {
        questionName, examForm, durationMinutes, difficulty, requirements, negatives,
        examineePrepare, sitePreparation, scoringItems: JSON.stringify(scoring),
      });
      message.success('已保存，状态已退回待审核，请重新审核');
      onSaved();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Drawer
      title={`编辑试题草稿（${draft.questionName}）`}
      width={760}
      open
      onClose={onClose}
      extra={<Button type="primary" loading={saving} onClick={save}>保存</Button>}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <div>
          <div>试题名称</div>
          <Input value={questionName} onChange={(e) => setQuestionName(e.target.value)} />
        </div>
        <Space>
          <span>考核形式</span>
          <Input style={{ width: 120 }} value={examForm} onChange={(e) => setExamForm(e.target.value)} />
          <span>考核时间（分钟）</span>
          <InputNumber min={5} max={240} value={durationMinutes} onChange={setDurationMinutes} />
          <span>难度</span>
          <InputNumber min={1} max={5} value={difficulty} onChange={setDifficulty} />
        </Space>
        <div>
          <div>具体考核要求</div>
          <Input.TextArea rows={3} value={requirements} onChange={(e) => setRequirements(e.target.value)} />
        </div>
        <div>
          <div>否定项</div>
          <Input.TextArea rows={3} value={negatives} onChange={(e) => setNegatives(e.target.value)} />
        </div>
        <div>
          <div>考生准备</div>
          <Input.TextArea rows={2} value={examineePrepare} onChange={(e) => setExamineePrepare(e.target.value)} />
        </div>
        <div>
          <div>考场准备（JSON 结构，含设备设施表）</div>
          <Input.TextArea rows={3} value={sitePreparation} onChange={(e) => setSitePreparation(e.target.value)} />
        </div>
        <div>
          <Space>
            <b>评分记录表</b>
            <Tag color={Math.abs(sum - 100) < 0.01 ? 'success' : 'error'}>配分合计 {sum}</Tag>
            <Button size="small" onClick={() => setScoring([...scoring, { item: '', score: '', scoreDesc: '', maxDeduct: '', step: '' }])}>
              增加行
            </Button>
          </Space>
          {scoring.map((row, idx) => (
            <Space key={idx} style={{ width: '100%', marginTop: 6 }} align="start" wrap>
              <Input
                style={{ width: 150 }} placeholder="评分要素" value={row.item}
                onChange={(e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, item: e.target.value } : r)))}
              />
              <Input
                style={{ width: 70 }} placeholder="配分" value={row.score}
                onChange={(e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, score: e.target.value } : r)))}
              />
              <Input
                style={{ width: 260 }} placeholder="评分标准" value={row.scoreDesc}
                onChange={(e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, scoreDesc: e.target.value } : r)))}
              />
              <Input
                style={{ width: 80 }} placeholder="最大扣分" value={row.maxDeduct}
                onChange={(e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, maxDeduct: e.target.value } : r)))}
              />
              <Input
                style={{ width: 60 }} placeholder="步长" value={row.step}
                onChange={(e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, step: e.target.value } : r)))}
              />
              <Button size="small" danger onClick={() => setScoring(scoring.filter((_, i) => i !== idx))}>删除</Button>
            </Space>
          ))}
        </div>
      </Space>
    </Drawer>
  );
};

// ---------------------------------------------------------------------------
// 入库弹窗（硬闸门：累计进度 + 缺口展示，不足置灰）
// ---------------------------------------------------------------------------

const PublishModal: React.FC<{ batch: SkillGenBatch; onClose: () => void; onPublished: () => void }> = (
  { batch, onClose, onPublished },
) => {
  const [taskMode, setTaskMode] = useState<'create' | 'append'>('create');
  const [taskId, setTaskId] = useState<string | undefined>();
  const [publishing, setPublishing] = useState(false);
  const preview = useQuery({
    queryKey: ['skillgen-publish-preview', batch.id],
    queryFn: () => getSkillPublishPreview(batch.id),
  });
  const p = preview.data;

  return (
    <Modal
      title="试题入库（写入实操命题任务）"
      open
      onCancel={onClose}
      okText="确认入库"
      okButtonProps={{ disabled: !p?.canPublish, loading: publishing }}
      onOk={async () => {
        setPublishing(true);
        try {
          const result = await publishSkillDrafts(batch.id, taskMode, taskId);
          message.success(`已入库 ${result.published} 题（累计 ${result.cumulativeTotal}，其中 AI 生成 ${result.cumulativeAiTotal}）`);
          onPublished();
        } finally {
          setPublishing(false);
        }
      }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <div>
          本次可入库（已审核通过）：<b>{p?.approvedCount ?? 0}</b> 题
        </div>
        <div>
          该工种等级累计进度：
          <b>{p?.afterTotal ?? 0}</b> / 下限 {p?.floor ?? '-'} 题（入库前 {p?.cumulativeTotal ?? 0} 题，
          其中 AI 生成 {p?.cumulativeAiTotal ?? 0} 题）
        </div>
        {p && p.gap > 0 && (
          <Alert
            type="warning"
            showIcon
            message={`还差 ${p.gap} 题才达到本等级题库总量下限，暂不可入库：请继续生成并审核通过后再入库。`}
          />
        )}
        {p && p.approvedCount === 0 && (
          <Alert type="info" showIcon message="当前没有审核通过的试题，请先在草稿面板审核通过后再入库。" />
        )}
        <div>
          <span>任务模式：</span>
          <Radio.Group
            value={taskMode}
            onChange={(e) => setTaskMode(e.target.value)}
            options={[
              { label: '新建命题任务', value: 'create' },
              { label: '追加到现有任务', value: 'append' },
            ]}
          />
          {taskMode === 'append' && (
            <Input
              style={{ width: 220, marginLeft: 8 }}
              placeholder="填写任务 ID"
              value={taskId}
              onChange={(e) => setTaskId(e.target.value)}
            />
          )}
        </div>
      </Space>
    </Modal>
  );
};

export default SkillQuestionGenPage;

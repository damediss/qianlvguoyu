/**
 * 细目表管理页 —— 《职业技能标准细目表生成功能实现方案》v1.2 §10.2
 * 菜单：智能课程材料管理 > 细目表管理（code xi_mu_biao_guan_li）
 * 两态：任务列表态 / 树编辑态（选中任务进入）
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Progress,
  Radio,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  LoadingOutlined,
  PlusOutlined,
  ReloadOutlined,
  RobotOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { hasPermission } from '../../directives/permission';
import {
  createNode,
  deleteNode,
  deleteTask,
  downloadExport,
  fetchTask,
  fetchTaskPage,
  fetchTree,
  moveNode,
  resummarizeNode,
  reviewDone,
  triggerSummarize,
  updateNode,
  uploadStandardDocx,
  type ExportKind,
  type StandardNode,
  type StandardTask,
  type SummarizeReq,
} from './api';
import './XiMuBiaoGuanLiPage.css';

const STATUS_TEXT: Record<StandardTask['status'], { text: string; color: string }> = {
  uploaded: { text: '已上传', color: 'default' },
  parsed: { text: '已解析', color: 'blue' },
  llm_running: { text: 'AI概括中', color: 'processing' },
  reviewing: { text: '待审核', color: 'orange' },
  confirmed: { text: '已确认', color: 'green' },
  exported: { text: '已导出', color: 'purple' },
};

const SOURCE_TEXT: Record<string, string> = {
  extract: '规则抽取',
  llm: 'AI概括',
  manual: '人工新增',
};

const REVIEW_TEXT: Record<string, { text: string; color: string }> = {
  pending: { text: '待审核', color: 'orange' },
  approved: { text: '已审核', color: 'green' },
  modified: { text: '已修改', color: 'blue' },
};

interface TreeRow extends StandardNode {
  children?: TreeRow[];
}

function buildTreeRows(flat: StandardNode[]): TreeRow[] {
  const byParent = new Map<string, StandardNode[]>();
  for (const n of flat) {
    const key = n.parentId || '0';
    if (!byParent.has(key)) byParent.set(key, []);
    byParent.get(key)!.push(n);
  }
  const sort = (list: StandardNode[]) =>
    [...list].sort((a, b) => a.sortNumber - b.sortNumber || a.nodeKey.localeCompare(b.nodeKey));
  const attach = (parentId: string): TreeRow[] =>
    sort(byParent.get(parentId) || []).map((n) => {
      const children = attach(n.id);
      return children.length > 0 ? { ...n, children } : { ...n };
    });
  return attach('0');
}

export default function XiMuBiaoGuanLiPage() {
  const [activeTaskId, setActiveTaskId] = useState<string | null>(null);
  return activeTaskId == null ? (
    <TaskListView onEnter={setActiveTaskId} />
  ) : (
    <TreeEditView taskId={activeTaskId} onBack={() => setActiveTaskId(null)} />
  );
}

// ==================================================================
// 任务列表态
// ==================================================================

function TaskListView({ onEnter }: { onEnter: (taskId: string) => void }) {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const canGenerate = hasPermission('ximubiaoguanli:generate');
  const canExport = hasPermission('ximubiaoguanli:export');
  const [uploading, setUploading] = useState(false);
  const [uploadPercent, setUploadPercent] = useState(0);
  const uploadTimerRef = useRef<number | null>(null);
  // AI 概括感知：上一轮 leafTotal 快照、增量提示（+N）、触发中的任务 id
  const prevLeafRef = useRef<Map<string, number>>(new Map());
  const [leafDeltas, setLeafDeltas] = useState<Record<string, number>>({});
  const [summarizingId, setSummarizingId] = useState<string | null>(null);

  // 组件卸载时清理模拟进度定时器
  useEffect(
    () => () => {
      if (uploadTimerRef.current !== null) window.clearInterval(uploadTimerRef.current);
    },
    [],
  );

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['standard-tasks', page, keyword],
    queryFn: () => fetchTaskPage({ page, pageSize: 20, keyword: keyword || undefined }),
    // 每次回到列表页强制拉最新（上传后跳预览树再返回、或他处触发概括后返回，均需看到新状态）
    refetchOnMount: 'always',
  });

  // LLM 概括中的任务轮询状态
  useEffect(() => {
    const hasRunning = (data?.items || []).some((t) => t.status === 'llm_running');
    if (!hasRunning) return;
    const timer = setInterval(() => refetch(), 5000);
    return () => clearInterval(timer);
  }, [data, refetch]);

  // AI 概括感知：对比上一轮 leafTotal 快照，计算增量触发 +N 渐隐动画
  useEffect(() => {
    const items = data?.items || [];
    const deltas: Record<string, number> = {};
    for (const t of items) {
      const prev = prevLeafRef.current.get(t.id);
      if (t.status === 'llm_running' && prev !== undefined && t.leafTotal > prev) {
        deltas[t.id] = t.leafTotal - prev;
      }
      prevLeafRef.current.set(t.id, t.leafTotal);
    }
    if (Object.keys(deltas).length > 0) {
      setLeafDeltas(deltas);
      const timer = window.setTimeout(() => setLeafDeltas({}), 2600);
      return () => window.clearTimeout(timer);
    }
  }, [data]);

  // 触发概括后：按钮 loading 保持到任务状态变为 llm_running
  useEffect(() => {
    if (!summarizingId) return;
    const t = (data?.items || []).find((x) => x.id === summarizingId);
    if (t && t.status === 'llm_running') setSummarizingId(null);
  }, [data, summarizingId]);

  const handleUpload = async (file: File) => {
    // 解析为同步接口、服务端无进度回调：用模拟进度条提示「处理中」，上限 95%，完成后跳 100%
    setUploading(true);
    setUploadPercent(0);
    if (uploadTimerRef.current !== null) window.clearInterval(uploadTimerRef.current);
    uploadTimerRef.current = window.setInterval(() => {
      setUploadPercent((p) =>
        p >= 95 ? p : Math.min(95, p + Math.max(1, Math.round((95 - p) / 10))),
      );
    }, 300);
    try {
      const task = await uploadStandardDocx(file);
      message.success(`解析完成：${task.workTypeTitle}（${task.levels.length} 个等级）`);
      // 等列表缓存刷新完成再进预览树，保证返回列表时能看到新任务
      await queryClient.invalidateQueries({ queryKey: ['standard-tasks'] });
      onEnter(task.id);
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '上传解析失败');
    } finally {
      if (uploadTimerRef.current !== null) {
        window.clearInterval(uploadTimerRef.current);
        uploadTimerRef.current = null;
      }
      setUploadPercent(100);
      window.setTimeout(() => {
        setUploading(false);
        setUploadPercent(0);
      }, 500);
    }
  };

  const handleSummarize = async (task: StandardTask, req: SummarizeReq) => {
    setSummarizingId(task.id);
    try {
      await triggerSummarize(task.id, req);
      message.success('LLM 概括已触发（异步执行，状态自动刷新）');
      refetch();
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '触发失败');
      setSummarizingId(null);
      throw e;
    }
  };

  const handleExport = async (task: StandardTask, kind: ExportKind) => {
    try {
      await downloadExport(task.id, kind);
      message.success('导出成功');
    } catch (e: any) {
      message.error(e?.message || '导出失败');
    }
  };

  const [sqlTask, setSqlTask] = useState<StandardTask | null>(null);
  const [summarizeTask, setSummarizeTask] = useState<StandardTask | null>(null);

  const columns: ColumnsType<StandardTask> = [
    { title: '职业名称', dataIndex: 'workTypeTitle', width: 140 },
    { title: '编码', dataIndex: 'seriesNo', width: 110 },
    { title: '版本', dataIndex: 'version', width: 70, render: (v) => (v ? `${v}年版` : '-') },
    { title: '等级数', width: 70, render: (_, t) => t.levels.length },
    {
      title: '工种',
      dataIndex: 'directions',
      width: 180,
      render: (dirs: string[]) =>
        dirs.length === 1 && dirs[0] === '默认' ? '（不分工种）' : dirs.join('、'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (s: StandardTask['status']) => (
        <Tag
          icon={s === 'llm_running' ? <LoadingOutlined spin /> : undefined}
          color={STATUS_TEXT[s]?.color}
        >
          {STATUS_TEXT[s]?.text || s}
        </Tag>
      ),
    },
    {
      title: '待审/总数',
      width: 130,
      render: (_, t) => {
        const running = t.status === 'llm_running';
        const delta = leafDeltas[t.id];
        return (
          <Space size={4}>
            <span className={running ? 'xmb-leaf-pulse' : undefined}>
              {t.leafPending}/{t.leafTotal}
            </span>
            {running && delta !== undefined && delta > 0 && (
              <span key={`${t.id}-${t.leafTotal}`} className="xmb-leaf-delta">
                +{delta}
              </span>
            )}
          </Space>
        );
      },
    },
    {
      title: '解析警告',
      dataIndex: 'parseWarnings',
      ellipsis: true,
      render: (w: string | null) =>
        w ? (
          <Tooltip title={<pre style={{ maxWidth: 480, whiteSpace: 'pre-wrap' }}>{w}</pre>}>
            <Typography.Text type="warning">{w.split('\n').length} 条</Typography.Text>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: '操作',
      width: 330,
      fixed: 'right',
      render: (_, t) => (
        <Space size={4} wrap>
          <Button size="small" type="link" onClick={() => onEnter(t.id)}>
            预览/审核树
          </Button>
          {canGenerate && (
            <Button
              size="small"
              type="link"
              icon={<RobotOutlined />}
              disabled={t.status === 'llm_running' || summarizingId === t.id}
              loading={summarizingId === t.id}
              onClick={() => setSummarizeTask(t)}
            >
              AI概括
            </Button>
          )}
          {canExport && (
            <>
              <Button size="small" type="link" onClick={() => handleExport(t, 'excel')}>
                理论Excel
              </Button>
              <Button size="small" type="link" onClick={() => handleExport(t, 'skill-excel')}>
                技能Excel
              </Button>
              <Button size="small" type="link" onClick={() => setSqlTask(t)}>
                入库SQL
              </Button>
            </>
          )}
          {canGenerate && (
            <Popconfirm
              title="删除该解析任务及其全部节点？"
              onConfirm={async () => {
                await deleteTask(t.id);
                message.success('已删除');
                refetch();
              }}
            >
              <Button size="small" type="link" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="细目表管理（国家职业技能标准 → 考评要素细目表）"
      extra={
        <Space>
          <Input.Search
            placeholder="职业名称"
            allowClear
            style={{ width: 200 }}
            onSearch={(v) => {
              setKeyword(v);
              setPage(1);
            }}
          />
          {canGenerate && (
            <Space size={8}>
              <Upload
                accept=".docx"
                showUploadList={false}
                disabled={uploading}
                customRequest={({ file }) => handleUpload(file as File)}
              >
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  loading={uploading}
                  disabled={uploading}
                >
                  {uploading ? '解析中…' : '上传标准 docx'}
                </Button>
              </Upload>
              {uploading && (
                <>
                  <Progress
                    percent={uploadPercent}
                    size="small"
                    status="active"
                    style={{ width: 160, margin: 0 }}
                  />
                  <span className="xmb-uploading-hint">正在解析标准文档，请勿关闭页面…</span>
                </>
              )}
            </Space>
          )}
        </Space>
      }
    >
      {(data?.items || []).some((t) => t.status === 'llm_running') && (
        <Alert
          type="info"
          showIcon
          icon={<LoadingOutlined spin />}
          message="AI 概括进行中：每 5 秒自动刷新，全量概括约需数分钟，可离开本页稍后回来查看。"
          style={{ marginBottom: 12 }}
        />
      )}
      <Table<StandardTask>
        rowKey="id"
        loading={isLoading}
        dataSource={data?.items || []}
        columns={columns}
        scroll={{ x: 1200 }}
        pagination={{
          current: page,
          pageSize: 20,
          total: data?.total || 0,
          onChange: setPage,
          showTotal: (t: number) => `共 ${t} 条`,
        }}
      />
      <SqlExportModal
        open={!!sqlTask}
        taskId={sqlTask?.id || ''}
        onClose={() => setSqlTask(null)}
      />
      <SummarizeModal
        task={summarizeTask}
        onClose={() => setSummarizeTask(null)}
        onStart={(req) => handleSummarize(summarizeTask!, req)}
      />
    </Card>
  );
}

// ==================================================================
// 树编辑态
// ==================================================================
// 入库 SQL 导出模式选择（create=新建课程 / reuse=复用已有课程）
// ==================================================================

function SqlExportModal({ open, taskId, scope, onClose }: {
  open: boolean;
  taskId: string;
  scope?: { levelCode?: number; direction?: string };
  onClose: () => void;
}) {
  const [mode, setMode] = useState<'create' | 'reuse'>('create');
  const [loading, setLoading] = useState(false);

  const doExport = async () => {
    setLoading(true);
    try {
      await downloadExport(taskId, 'sql', { ...scope, mode });
      message.success('导出成功');
      onClose();
    } catch (e: any) {
      message.error(e?.message || '导出失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      onOk={doExport}
      confirmLoading={loading}
      title="导出入库SQL"
      okText="导出"
    >
      <Radio.Group value={mode} onChange={(e) => setMode(e.target.value)}>
        <Space direction="vertical">
          <Radio value="create">新建课程（生成 exam_work_type + exam_course）</Radio>
          <Radio value="reuse">复用已有课程（按工种/等级匹配已有 course_id）</Radio>
        </Space>
      </Radio.Group>
      {mode === 'reuse' && (
        <Alert
          type="warning"
          showIcon
          style={{ marginTop: 10 }}
          message="复用模式执行前请先备份目标库：将删除匹配课程下全部旧知识树，再写入新树。"
        />
      )}
    </Modal>
  );
}

// ==================================================================
// AI 概括范围选择弹窗：工种×等级矩阵 + 重要程度档位（默认 XY）
// ==================================================================

function SummarizeModal({ task, onClose, onStart }: {
  task: StandardTask | null;
  onClose: () => void;
  onStart: (req: SummarizeReq) => Promise<void>;
}) {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [importants, setImportants] = useState<string[]>(['X', 'Y']);
  const [loading, setLoading] = useState(false);

  // 有效组合集合：directionLevels 为空（旧任务）时全部等级×工种有效
  const validKeys = useMemo(() => {
    const set = new Set<string>();
    if (!task) return set;
    const matrix =
      task.directionLevels && Object.keys(task.directionLevels).length > 0
        ? task.directionLevels
        : null;
    for (const level of task.levels) {
      for (const dir of task.directions) {
        if (matrix && !matrix[dir]?.includes(level.levelCode)) continue;
        set.add(`${level.levelCode}|${dir}`);
      }
    }
    return set;
  }, [task]);

  // 打开弹窗时重置默认：全部有效组合 + XY
  useEffect(() => {
    if (task) {
      setSelected(new Set(validKeys));
      setImportants(['X', 'Y']);
    }
  }, [task, validKeys]);

  if (!task) return null;

  const toggle = (key: string, checked: boolean) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (checked) next.add(key);
      else next.delete(key);
      return next;
    });
  };

  const doStart = async () => {
    if (selected.size === 0) {
      message.warning('请至少选择一个工种×等级组合');
      return;
    }
    if (importants.length === 0) {
      message.warning('请至少选择一个重要程度');
      return;
    }
    setLoading(true);
    try {
      await onStart({
        scopes: [...selected].map((k) => {
          const [lc, dir] = k.split('|');
          return { levelCode: Number(lc), direction: dir };
        }),
        importants,
      });
      onClose();
    } catch {
      // onStart 内部已提示错误，弹窗保持打开便于调整
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open
      title={`AI 概括范围选择（${task.workTypeTitle}）`}
      okText="开始概括"
      confirmLoading={loading}
      onCancel={onClose}
      onOk={doStart}
      width={640}
    >
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        <div>
          <Space style={{ marginBottom: 4 }}>
            <Typography.Text strong>工种 × 等级</Typography.Text>
            <Button type="link" size="small" onClick={() => setSelected(new Set(validKeys))}>
              全选
            </Button>
            <Button type="link" size="small" onClick={() => setSelected(new Set())}>
              清空
            </Button>
          </Space>
          <table className="xmb-scope-matrix">
            <thead>
              <tr>
                <th>工种 \ 等级</th>
                {task.levels.map((l) => (
                  <th key={l.levelCode}>{l.levelName}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {task.directions.map((dir) => (
                <tr key={dir}>
                  <td>{dir === '默认' ? '（不分工种）' : dir}</td>
                  {task.levels.map((l) => {
                    const key = `${l.levelCode}|${dir}`;
                    return (
                      <td key={key}>
                        {validKeys.has(key) ? (
                          <Checkbox
                            checked={selected.has(key)}
                            onChange={(e) => toggle(key, e.target.checked)}
                          />
                        ) : (
                          <span className="xmb-scope-na">—</span>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div>
          <Typography.Text strong>重要程度（仅作用于理论考评点）</Typography.Text>
          <div style={{ marginTop: 4 }}>
            <Checkbox.Group
              value={importants}
              onChange={(v) => setImportants(v as string[])}
              options={[
                { value: 'X', label: 'X（核心必考）' },
                { value: 'Y', label: 'Y（重要）' },
                { value: 'Z', label: 'Z（一般了解）' },
              ]}
            />
          </div>
        </div>
        <Alert
          type="info"
          showIcon
          message="理论考评点按小节考评权重分配（每张细目表约 200 个，权重越大的小节生成越多）；技能考评点按原文逐条生成。"
        />
      </Space>
    </Modal>
  );
}

// ==================================================================

function TreeEditView({ taskId, onBack }: { taskId: string; onBack: () => void }) {
  const queryClient = useQueryClient();
  const canGenerate = hasPermission('ximubiaoguanli:generate');
  const canExport = hasPermission('ximubiaoguanli:export');

  const [scope, setScope] = useState<{ levelCode: number; direction: string } | null>(null);
  const [dimension, setDimension] = useState<'theory' | 'skill'>('theory');
  const [editing, setEditing] = useState<StandardNode | null>(null);
  const [addingUnder, setAddingUnder] = useState<StandardNode | null>(null);
  const [resummarizing, setResummarizing] = useState<string | null>(null);
  const [sqlOpen, setSqlOpen] = useState(false);
  const [summarizeOpen, setSummarizeOpen] = useState(false);
  const [summarizing, setSummarizing] = useState(false);
  const warningsRef = useRef<HTMLDivElement>(null);

  const { data: task, refetch: refetchTask } = useQuery({
    queryKey: ['standard-task', taskId],
    queryFn: () => fetchTask(taskId),
    refetchInterval: (q) => (q.state.data?.status === 'llm_running' ? 5000 : false),
  });

  // 可选范围 = levels × directions，按 1.4 节工种可用等级矩阵过滤，旧任务无矩阵则保留全组合
  const scopes = useMemo(() => {
    if (!task) return [];
    const out: Array<{ levelCode: number; direction: string; label: string }> = [];
    const constraints =
      task.directionLevels && Object.keys(task.directionLevels).length > 0
        ? task.directionLevels
        : null;
    for (const level of task.levels) {
      for (const direction of task.directions) {
        if (constraints && !constraints[direction]?.includes(level.levelCode)) {
          continue;
        }
        out.push({
          levelCode: level.levelCode,
          direction,
          label: task.directions.length > 1 ? `${level.levelName}-${direction}` : level.levelName,
        });
      }
    }
    return out;
  }, [task]);

  const activeScope = useMemo(() => {
    if (!scopes.length) return null;
    const match = scopes.find(
      (s) => s.levelCode === scope?.levelCode && s.direction === scope?.direction,
    );
    return match ?? scopes[0];
  }, [scopes, scope]);

  const { data: flatNodes, isLoading, refetch } = useQuery({
    queryKey: ['standard-tree', taskId, activeScope?.levelCode, activeScope?.direction, dimension],
    queryFn: () =>
      fetchTree(taskId, {
        levelCode: activeScope!.levelCode,
        direction: activeScope!.direction,
        dimension,
      }),
    enabled: !!activeScope,
  });

  // LLM 概括完成后自动刷新树
  useEffect(() => {
    if (task?.status === 'reviewing') {
      refetch();
    }
  }, [task?.status, refetch]);

  // 技能视图对齐「技能要求权重表」：技能细目表从职业功能（L1）起，
  // 不出现「基本要求 A 支」和「工作要求 B 根」结构行（技能导出同样从 L1 起）
  const treeData = useMemo(() => {
    const flat = flatNodes || [];
    if (dimension !== 'skill') {
      return buildTreeRows(flat);
    }
    const hidden = new Set(
      flat
        .filter((n) => n.nodeKey === 'A' || n.nodeKey.startsWith('A-') || n.nodeKey === 'B')
        .map((n) => n.id),
    );
    const rerooted = flat
      .filter((n) => !hidden.has(n.id))
      // 父节点被隐藏的 L1 职业功能提升为根节点
      .map((n) => (n.parentId && hidden.has(n.parentId) ? { ...n, parentId: '' } : n));
    return buildTreeRows(rerooted);
  }, [flatNodes, dimension]);
  const warnings = useMemo(
    () => (task?.parseWarnings || '').split('\n').filter(Boolean),
    [task],
  );

  const refreshAll = useCallback(() => {
    refetch();
    refetchTask();
    queryClient.invalidateQueries({ queryKey: ['standard-tasks'] });
  }, [refetch, refetchTask, queryClient]);

  // 触发概括后：按钮 loading 保持到任务状态变为 llm_running（任务查询 5s 轮询会自动翻转状态）
  useEffect(() => {
    if (summarizing && task?.status === 'llm_running') setSummarizing(false);
  }, [summarizing, task?.status]);

  const handleSummarize = async (req: SummarizeReq) => {
    if (!task) return;
    setSummarizing(true);
    try {
      await triggerSummarize(task.id, req);
      message.success('LLM 概括已触发（异步执行，状态自动刷新）');
      refetchTask();
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '触发失败');
      setSummarizing(false);
      throw e;
    }
  };

  const handleExport = async (kind: ExportKind) => {
    if (!task) return;
    try {
      await downloadExport(task.id, kind, activeScope
        ? { levelCode: activeScope.levelCode, direction: activeScope.direction }
        : undefined);
      message.success('导出成功');
      refreshAll();
    } catch (e: any) {
      message.error(e?.message || '导出失败');
    }
  };

  const handleResummarize = async (node: StandardNode) => {
    setResummarizing(node.id);
    try {
      await resummarizeNode(node.id, dimension);
      message.success('重新生成完成');
      refreshAll();
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '重新生成失败');
    } finally {
      setResummarizing(null);
    }
  };

  const columns: ColumnsType<TreeRow> = [
    {
      title: '标题',
      dataIndex: 'title',
      width: 340,
      render: (v: string, n) => (
        <Space size={4}>
          <span>{v}</span>
          {n.needLlm === 1 && n.nodeLevel === 2 && (
            <Tooltip title="空小节：docx 无列举项，需 LLM 概括或人工补叶">
              <Tag color="red">空小节</Tag>
            </Tooltip>
          )}
        </Space>
      ),
    },
    { title: '编码', dataIndex: 'nodeKey', width: 110 },
    {
      title: '层级',
      dataIndex: 'nodeLevel',
      width: 60,
      render: (l: number) => `L${l}`,
    },
    {
      title: '维度',
      dataIndex: 'dimension',
      width: 70,
      render: (d: string, n) =>
        n.nodeLevel === 3 ? (d === 'skill' ? <Tag color="geekblue">技能</Tag> : <Tag>理论</Tag>) : '-',
    },
    {
      title: '重要程度',
      dataIndex: 'important',
      width: 90,
      render: (v: string | null, n) =>
        n.nodeLevel === 3 ? (
          canGenerate ? (
            <Select
              size="small"
              value={v || 'X'}
              style={{ width: 64 }}
              options={['X', 'Y', 'Z'].map((x) => ({ value: x, label: x }))}
              onChange={async (val) => {
                await updateNode(n.id, { important: val });
                refetch();
              }}
            />
          ) : (
            v || 'X'
          )
        ) : (
          '-'
        ),
    },
    {
      title: '权重',
      dataIndex: 'weight',
      width: 100,
      render: (v: number | null, n) =>
        n.nodeLevel >= 1 && n.nodeLevel <= 2 ? (
          canGenerate ? (
            <InputNumber
              size="small"
              value={v}
              min={0}
              max={1}
              step={0.01}
              style={{ width: 84 }}
              placeholder="0.00"
              onBlur={async (e) => {
                const val = e.target.value === '' ? null : Number(e.target.value);
                if (val !== v) {
                  await updateNode(n.id, { weight: val ?? undefined });
                  refetch();
                }
              }}
            />
          ) : (
            v ?? '-'
          )
        ) : (
          '-'
        ),
    },
    {
      title: '来源',
      dataIndex: 'source',
      width: 90,
      render: (s: string) => SOURCE_TEXT[s] || s,
    },
    {
      title: '审核状态',
      dataIndex: 'reviewStatus',
      width: 90,
      render: (s: string) => (
        <Tag color={REVIEW_TEXT[s]?.color}>{REVIEW_TEXT[s]?.text || s}</Tag>
      ),
    },
    {
      title: '操作',
      width: 240,
      fixed: 'right',
      render: (_, n) => (
        <Space size={0}>
          {canGenerate && (
            <Button size="small" type="link" icon={<EditOutlined />}
              onClick={() => setEditing(n)} />
          )}
          {canGenerate && n.nodeLevel === 2 && (
            <>
              <Button size="small" type="link" icon={<PlusOutlined />}
                onClick={() => setAddingUnder(n)}>
                加叶
              </Button>
              <Button size="small" type="link" icon={<ReloadOutlined />}
                loading={resummarizing === n.id}
                onClick={() => handleResummarize(n)}>
                重新生成
              </Button>
            </>
          )}
          {canGenerate && (
            <>
              <Button size="small" type="link" icon={<ArrowUpOutlined />}
                onClick={async () => { await moveNode(n.id, 'up'); refetch(); }} />
              <Button size="small" type="link" icon={<ArrowDownOutlined />}
                onClick={async () => { await moveNode(n.id, 'down'); refetch(); }} />
              <Popconfirm title="删除该节点（含子树）？" onConfirm={async () => {
                await deleteNode(n.id);
                message.success('已删除');
                refreshAll();
              }}>
                <Button size="small" type="link" danger icon={<DeleteOutlined />} />
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ];

  if (!task) {
    return <Card loading />;
  }

  return (
    <Card
      title={
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
            返回列表
          </Button>
          <span>
            {task.workTypeTitle}（{task.seriesNo}，{task.version}年版）
          </span>
          <Tag color={STATUS_TEXT[task.status]?.color}>{STATUS_TEXT[task.status]?.text}</Tag>
          <span className={task.status === 'llm_running' ? 'xmb-leaf-pulse' : undefined}>
            待审 {task.leafPending}/{task.leafTotal}
          </span>
        </Space>
      }
      extra={
        <Space>
          {canGenerate && (
            <Button
              type="primary"
              icon={<RobotOutlined />}
              disabled={task.status === 'llm_running'}
              loading={summarizing}
              onClick={() => setSummarizeOpen(true)}
            >
              AI概括
            </Button>
          )}
          {canGenerate && (
            <Popconfirm
              title="将全部待审叶节点标为已审核？"
              onConfirm={async () => {
                try {
                  await reviewDone(task.id);
                  message.success('已全部标为已审核');
                  refreshAll();
                } catch (e: any) {
                  message.error(e?.response?.data?.message || e?.message || '操作失败');
                }
              }}
            >
              <Button>全部标已审</Button>
            </Popconfirm>
          )}
          {canExport && (
            <>
              <Button icon={<DownloadOutlined />} onClick={() => handleExport('excel')}>
                导出理论细目表
              </Button>
              <Button icon={<DownloadOutlined />} onClick={() => handleExport('skill-excel')}>
                导出技能细目表
              </Button>
              <Button icon={<DownloadOutlined />} onClick={() => setSqlOpen(true)}>
                导出入库SQL
              </Button>
            </>
          )}
        </Space>
      }
    >
      {task.status === 'llm_running' && (
        <Alert type="info" showIcon message="LLM 概括进行中，状态每 5 秒自动刷新…"
          style={{ marginBottom: 12 }} />
      )}
      {warnings.length > 0 && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message={`解析警告 ${warnings.length} 条`}
          description={
            <div ref={warningsRef} style={{ maxHeight: 120, overflow: 'auto' }}>
              {warnings.map((w, i) => (
                <div key={i}>{w}</div>
              ))}
            </div>
          }
        />
      )}
      <Space style={{ marginBottom: 12 }} wrap>
        <span>理论/技能叶：</span>
        <Select
          value={dimension}
          style={{ width: 140 }}
          onChange={setDimension}
          options={[
            { value: 'theory', label: '理论考评点' },
            { value: 'skill', label: '技能考评点（技）' },
          ]}
        />
      </Space>
      <Tabs
        activeKey={activeScope ? `${activeScope.levelCode}|${activeScope.direction}` : undefined}
        onChange={(key) => {
          const [levelCode, direction] = key.split('|');
          setScope({ levelCode: Number(levelCode), direction });
        }}
        items={scopes.map((s) => ({
          key: `${s.levelCode}|${s.direction}`,
          label: s.label,
        }))}
      />
      <Table<TreeRow>
        rowKey="id"
        loading={isLoading}
        dataSource={treeData}
        columns={columns}
        expandable={{ defaultExpandAllRows: true }}
        pagination={false}
        scroll={{ x: 1300 }}
        size="small"
      />

      <NodeEditModal
        node={editing}
        onClose={() => setEditing(null)}
        onSaved={() => {
          setEditing(null);
          refetch();
        }}
      />
      <NodeCreateModal
        taskId={taskId}
        parent={addingUnder}
        dimension={dimension}
        onClose={() => setAddingUnder(null)}
        onSaved={() => {
          setAddingUnder(null);
          refreshAll();
        }}
      />
      <SqlExportModal
        open={sqlOpen}
        taskId={taskId}
        scope={activeScope
          ? { levelCode: activeScope.levelCode, direction: activeScope.direction }
          : undefined}
        onClose={() => setSqlOpen(false)}
      />
      <SummarizeModal
        task={summarizeOpen ? task : null}
        onClose={() => setSummarizeOpen(false)}
        onStart={handleSummarize}
      />
    </Card>
  );
}

// ==================================================================
// 编辑 / 新增弹窗
// ==================================================================

function NodeEditModal({ node, onClose, onSaved }: {
  node: StandardNode | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [title, setTitle] = useState('');
  useEffect(() => {
    setTitle(node?.title || '');
  }, [node]);
  if (!node) return null;
  return (
    <Modal
      open
      title={`编辑节点 ${node.nodeKey}`}
      onCancel={onClose}
      onOk={async () => {
        await updateNode(node.id, { title });
        message.success('已保存');
        onSaved();
      }}
    >
      <Typography.Text type="secondary">标题（含编码后缀，可直接修改）</Typography.Text>
      <Input.TextArea rows={3} value={title} onChange={(e) => setTitle(e.target.value)} />
    </Modal>
  );
}

function NodeCreateModal({ taskId, parent, dimension, onClose, onSaved }: {
  taskId: string;
  parent: StandardNode | null;
  dimension: 'theory' | 'skill';
  onClose: () => void;
  onSaved: () => void;
}) {
  const [title, setTitle] = useState('');
  const [important, setImportant] = useState('X');
  useEffect(() => {
    setTitle('');
    setImportant('X');
  }, [parent]);
  if (!parent) return null;
  return (
    <Modal
      open
      title={`在「${parent.title}」下新增${dimension === 'skill' ? '技能' : '理论'}考评点`}
      onCancel={onClose}
      onOk={async () => {
        if (!title.trim()) {
          message.warning('请填写考评点名称');
          return;
        }
        await createNode({
          taskId,
          parentId: parent.id,
          title: title.trim(),
          important,
          dimension,
        });
        message.success('已新增');
        onSaved();
      }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <div>
          <Typography.Text type="secondary">考评点名称</Typography.Text>
          <Input.TextArea rows={2} value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div>
          <Typography.Text type="secondary">重要程度</Typography.Text>
          <Select
            value={important}
            style={{ width: 120, marginLeft: 8 }}
            onChange={setImportant}
            options={['X', 'Y', 'Z'].map((x) => ({ value: x, label: x }))}
          />
        </div>
      </Space>
    </Modal>
  );
}

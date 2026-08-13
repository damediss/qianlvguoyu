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
  Input,
  InputNumber,
  Modal,
  Popconfirm,
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
} from './api';

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

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['standard-tasks', page, keyword],
    queryFn: () => fetchTaskPage({ page, pageSize: 20, keyword: keyword || undefined }),
  });

  // LLM 概括中的任务轮询状态
  useEffect(() => {
    const hasRunning = (data?.items || []).some((t) => t.status === 'llm_running');
    if (!hasRunning) return;
    const timer = setInterval(() => refetch(), 5000);
    return () => clearInterval(timer);
  }, [data, refetch]);

  const handleUpload = async (file: File) => {
    try {
      const task = await uploadStandardDocx(file);
      message.success(`解析完成：${task.workTypeTitle}（${task.levels.length} 个等级）`);
      queryClient.invalidateQueries({ queryKey: ['standard-tasks'] });
      onEnter(task.id);
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '上传解析失败');
    }
  };

  const handleSummarize = async (task: StandardTask) => {
    try {
      await triggerSummarize(task.id);
      message.success('LLM 概括已触发（异步执行，状态自动刷新）');
      refetch();
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '触发失败');
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
      width: 100,
      render: (s: StandardTask['status']) => (
        <Tag color={STATUS_TEXT[s]?.color}>{STATUS_TEXT[s]?.text || s}</Tag>
      ),
    },
    {
      title: '待审/总数',
      width: 90,
      render: (_, t) => `${t.leafPending}/${t.leafTotal}`,
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
              disabled={t.status === 'llm_running'}
              onClick={() => handleSummarize(t)}
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
            <Upload
              accept=".docx"
              showUploadList={false}
              customRequest={({ file }) => handleUpload(file as File)}
            >
              <Button type="primary" icon={<UploadOutlined />}>
                上传标准 docx
              </Button>
            </Upload>
          )}
        </Space>
      }
    >
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
  const warningsRef = useRef<HTMLDivElement>(null);

  const { data: task, refetch: refetchTask } = useQuery({
    queryKey: ['standard-task', taskId],
    queryFn: () => fetchTask(taskId),
    refetchInterval: (q) => (q.state.data?.status === 'llm_running' ? 5000 : false),
  });

  // 可选范围 = levels × directions（剔除解析期跳过的组合：查询后无节点时由后端返空）
  const scopes = useMemo(() => {
    if (!task) return [];
    const out: Array<{ levelCode: number; direction: string; label: string }> = [];
    for (const level of task.levels) {
      for (const direction of task.directions) {
        out.push({
          levelCode: level.levelCode,
          direction,
          label: task.directions.length > 1 ? `${level.levelName}-${direction}` : level.levelName,
        });
      }
    }
    return out;
  }, [task]);

  const activeScope = scope || scopes[0] || null;

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

  const treeData = useMemo(() => buildTreeRows(flatNodes || []), [flatNodes]);
  const warnings = useMemo(
    () => (task?.parseWarnings || '').split('\n').filter(Boolean),
    [task],
  );

  const refreshAll = useCallback(() => {
    refetch();
    refetchTask();
    queryClient.invalidateQueries({ queryKey: ['standard-tasks'] });
  }, [refetch, refetchTask, queryClient]);

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
        n.nodeLevel === 3 ? (d === 'skill' ? <Tag color="geekblue">技</Tag> : <Tag>理</Tag>) : '-',
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
          <span>
            待审 {task.leafPending}/{task.leafTotal}
          </span>
        </Space>
      }
      extra={
        <Space>
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

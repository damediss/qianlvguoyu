/**
 * 教材大纲编辑页（隐藏路由 /textbook/outline/:projectId，不进菜单）
 * —— 《职业标准驱动教材生成·一期实施设计》v1.0 §3.3
 * 树形 Table：扁平 OutlineNodeVO 自组树（参照 XiMuBiaoGuanLiPage 的 buildTreeRows）
 */
import { useEffect, useMemo, useState } from 'react';
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  CheckCircleOutlined,
  DownOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Dropdown,
  Empty,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createOutlineNode,
  deleteOutlineNode,
  exportProject,
  getOutline,
  getProject,
  rebuildOutline,
  updateOutlineNode,
  updateProject,
} from '../../api/textbook';
import { useAuthStore } from '../../stores/authStore';
import { parseContentDispositionFilename } from '../../utils/download';
import {
  textbookLevelLabel,
  type OutlineDimension,
  type OutlineNodeUpdateReq,
  type OutlineNodeVO,
  type OutlineSummary,
  type TextbookProjectStatus,
} from '../../types/textbook';
import { TEXTBOOK_PROJECT_STATUS_META } from './TextbookTaskMeta';

const STATUS_META: Record<TextbookProjectStatus, { text: string; color: string }> = TEXTBOOK_PROJECT_STATUS_META;

const DIMENSION_META: Record<OutlineDimension, { text: string; color: string }> = {
  theory: { text: '理论', color: 'blue' },
  skill: { text: '技能', color: 'geekblue' },
};

interface TreeRow extends OutlineNodeVO {
  children?: TreeRow[];
}

function buildTreeRows(flat: OutlineNodeVO[]): TreeRow[] {
  const byParent = new Map<string, OutlineNodeVO[]>();
  for (const n of flat) {
    const key = n.parentId || '0';
    if (!byParent.has(key)) byParent.set(key, []);
    byParent.get(key)!.push(n);
  }
  const attach = (parentId: string): TreeRow[] =>
    [...(byParent.get(parentId) || [])]
      .sort((a, b) => a.sortNo - b.sortNo || String(a.id).localeCompare(String(b.id)))
      .map((n) => {
        const children = attach(n.id);
        return children.length > 0 ? { ...n, children } : { ...n };
      });
  return attach('0');
}

function errorText(e: unknown, fallback: string): string {
  const err = e as { response?: { data?: { message?: string } }; message?: string };
  return err?.response?.data?.message || err?.message || fallback;
}

/** 隐藏路由跳转（与 TextbookProjectPage 相同约定：pushState + popstate） */
function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

/** 结构冻结提示（§5.7：存在正文或有效分配后，身份变化操作一律 409，前端同步禁用并给出原因） */
const STRUCTURE_LOCK_TOOLTIP = '工程已进入编写（存在章分配或正文），大纲结构已冻结；如需重建请先取消全部分配并确认无正文。';
const DONE_READONLY_TOOLTIP = '工程已完成编写（done），全页只读；如需修改请先在编写管理中重新打开。';

export function TextbookOutlinePage({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient();
  const hasPermission = useAuthStore((s) => s.hasPermission);
  const canManage = hasPermission('textbook:project:manage');
  const canExport = hasPermission('textbook:project:export');

  const [summary, setSummary] = useState<OutlineSummary | null>(null);
  const [addingUnder, setAddingUnder] = useState<TreeRow | null>(null);
  const [exporting, setExporting] = useState(false);

  const projectQuery = useQuery({
    queryKey: ['textbook-project', projectId],
    queryFn: () => getProject(projectId),
  });
  const outlineQuery = useQuery({
    queryKey: ['textbook-outline', projectId],
    queryFn: () => getOutline(projectId),
  });

  const project = projectQuery.data;
  const flat = useMemo(() => outlineQuery.data || [], [outlineQuery.data]);
  const treeData = useMemo(() => buildTreeRows(flat), [flat]);

  // 二期结构冻结与只读口径（§5.7/§10.2）
  const structureLocked = project?.structureLocked ?? false;
  const isDone = project?.status === 'done';
  const structureFrozen = structureLocked || isDone;
  const frozenReason = isDone ? DONE_READONLY_TOOLTIP : STRUCTURE_LOCK_TOOLTIP;

  // 合计校验：详情接口与写接口（改字数/重建）都会携带，写接口响应优先
  useEffect(() => {
    if (project?.summary) setSummary(project.summary);
  }, [project]);

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: ['textbook-outline', projectId] });
    queryClient.invalidateQueries({ queryKey: ['textbook-project', projectId] });
    queryClient.invalidateQueries({ queryKey: ['textbook-projects'] });
  };

  const rebuildMutation = useMutation({
    mutationFn: () => rebuildOutline(projectId),
    onSuccess: (s) => {
      setSummary(s);
      message.success('已重新配比');
      invalidateAll();
    },
  });

  const reviewMutation = useMutation({
    mutationFn: () => updateProject(projectId, { status: 'reviewed' }),
    onSuccess: () => {
      message.success('已评审通过');
      invalidateAll();
    },
  });

  const nodeUpdateMutation = useMutation({
    mutationFn: ({ id, req }: { id: string; req: OutlineNodeUpdateReq }) =>
      updateOutlineNode(id, req),
    onSuccess: (s) => {
      setSummary(s);
      queryClient.invalidateQueries({ queryKey: ['textbook-outline', projectId] });
    },
    onError: (e) => message.error(errorText(e, '保存失败')),
  });

  const nodeDeleteMutation = useMutation({
    mutationFn: deleteOutlineNode,
    onSuccess: () => {
      message.success('已删除（含子孙节点）');
      invalidateAll();
    },
    onError: (e) => message.error(errorText(e, '删除失败')),
  });

  // 上移/下移：同级交换 sortNo（先改自己、再改兄弟，最后刷新）
  const handleMove = async (node: TreeRow, dir: -1 | 1) => {
    const pid = node.parentId || '0';
    const siblings = flat
      .filter((n) => (n.parentId || '0') === pid)
      .sort((a, b) => a.sortNo - b.sortNo);
    const idx = siblings.findIndex((s) => s.id === node.id);
    const other = siblings[idx + dir];
    if (!other) return;
    try {
      await updateOutlineNode(node.id, { sortNo: other.sortNo });
      await updateOutlineNode(other.id, { sortNo: node.sortNo });
      await queryClient.invalidateQueries({ queryKey: ['textbook-outline', projectId] });
    } catch (e) {
      message.error(errorText(e, '移动失败'));
    }
  };

  const handleExport = async (format: 'md' | 'xlsx') => {
    if (!project) return;
    setExporting(true);
    try {
      const resp = await exportProject(projectId, format);
      const blob = resp.data as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download =
        parseContentDispositionFilename(resp) ??
        `教材目录_${project.workTypeTitle}_${project.direction}_${textbookLevelLabel(project.levelCode)}.${format === 'md' ? 'md' : 'xlsx'}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (e) {
      message.error(errorText(e, '导出失败'));
    } finally {
      setExporting(false);
    }
  };

  const warnings = useMemo(() => {
    const raw = project?.parseWarnings;
    if (!raw) return [] as string[];
    try {
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? (arr as string[]).map(String) : [String(arr)];
    } catch {
      return raw.split('\n').filter(Boolean);
    }
  }, [project]);

  const columns: ColumnsType<TreeRow> = [
    {
      title: '标题',
      dataIndex: 'title',
      render: (v: string, n) => (
        <Space size={4}>
          <span>{v}</span>
          {n.sourceNo && (
            <span style={{ color: '#999', fontSize: 12 }}>{n.sourceNo}</span>
          )}
          {!n.sourceNo && n.nodeLevel > 1 && <Tag color="purple">自增</Tag>}
        </Space>
      ),
    },
    {
      title: '维度',
      dataIndex: 'dimension',
      width: 80,
      render: (d: OutlineDimension | null, n) =>
        n.nodeLevel === 4 && d ? (
          <Tag color={DIMENSION_META[d]?.color}>{DIMENSION_META[d]?.text}</Tag>
        ) : (
          '-'
        ),
    },
    {
      title: '权重%',
      dataIndex: 'weight',
      width: 90,
      render: (v: number | null, n) => (n.nodeLevel === 2 && v != null ? `${v}%` : '-'),
    },
    {
      title: '目标字数',
      dataIndex: 'targetWords',
      width: 190,
      render: (v: number | null, n) =>
        n.nodeLevel === 2 || n.nodeLevel === 3 ? (
          <Space size={4}>
            {canManage ? (
              <InputNumber
                size="small"
                min={0}
                max={10000000}
                style={{ width: 110 }}
                value={v ?? undefined}
                disabled={isDone}
                onBlur={(e) => {
                  const raw = (e.target as HTMLInputElement).value;
                  const val = raw === '' ? null : Number(raw);
                  if (val !== v) {
                    nodeUpdateMutation.mutate({
                      id: n.id,
                      req: val == null ? {} : { targetWords: val },
                    });
                  }
                }}
              />
            ) : (
              <span>{v ?? '-'}</span>
            )}
            {n.wordsManual === 1 && <Tag color="orange">已手调</Tag>}
          </Space>
        ) : (
          '-'
        ),
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      render: (_, n) =>
        canManage ? (
          <Space size={0}>
            <Tooltip title={isDone ? DONE_READONLY_TOOLTIP : '上移'}>
              <Button
                size="small"
                type="link"
                aria-label="上移节点"
                icon={<ArrowUpOutlined />}
                disabled={isDone}
                onClick={() => handleMove(n, -1)}
              />
            </Tooltip>
            <Tooltip title={isDone ? DONE_READONLY_TOOLTIP : '下移'}>
              <Button
                size="small"
                type="link"
                aria-label="下移节点"
                icon={<ArrowDownOutlined />}
                disabled={isDone}
                onClick={() => handleMove(n, 1)}
              />
            </Tooltip>
            {n.nodeLevel < 4 && (
              <Tooltip title={structureFrozen ? frozenReason : undefined}>
                <Button
                  size="small"
                  type="link"
                  aria-label="新增子节点"
                  icon={<PlusOutlined />}
                  disabled={structureFrozen}
                  onClick={() => setAddingUnder(n)}
                >
                  新增子
                </Button>
              </Tooltip>
            )}
            <Tooltip title={structureFrozen ? frozenReason : undefined}>
              <Popconfirm
                title="删除该节点？"
                description="将同时删除其全部子孙节点。"
                onConfirm={() => nodeDeleteMutation.mutate(n.id)}
                disabled={structureFrozen}
              >
                <Button size="small" type="link" danger aria-label="删除节点" disabled={structureFrozen}>
                  删除
                </Button>
              </Popconfirm>
            </Tooltip>
          </Space>
        ) : (
          '-'
        ),
    },
  ];

  if (projectQuery.isLoading) {
    return <Card loading />;
  }
  if (!project) {
    return (
      <Card>
        <Empty description="教材工程不存在或无权访问" />
      </Card>
    );
  }

  return (
    <Card
      title={
        <Space wrap>
          <span>{`${project.workTypeTitle}（${project.direction} · ${textbookLevelLabel(project.levelCode)}）`}</span>
          <Tag color={STATUS_META[project.status]?.color}>{STATUS_META[project.status]?.text}</Tag>
          <span>总学时 {project.trainingHours}</span>
          <span>总字数 {project.totalWords}</span>
        </Space>
      }
      extra={
        <Space wrap>
          {canManage && (
            <Tooltip title={structureFrozen ? frozenReason : undefined}>
              <Popconfirm
                title="重新生成骨架并配比？"
                description="未人工调整的字数将被覆盖；已手调的章/节字数保留。"
                onConfirm={() => rebuildMutation.mutate()}
                disabled={structureFrozen}
              >
                <Button icon={<ReloadOutlined />} loading={rebuildMutation.isPending} disabled={structureFrozen}>
                  重新配比
                </Button>
              </Popconfirm>
            </Tooltip>
          )}
          {/* 二期 §10.2：「评审通过」仅 outlined 显示（原 status !== reviewed 写法会在 writing/done 错显示） */}
          {canManage && project.status === 'outlined' && (
            <Popconfirm
              title="确认评审通过？"
              description="状态将变为「大纲已评审」，之后可在编写管理中分配章节任务。"
              onConfirm={() => reviewMutation.mutate()}
            >
              <Button type="primary" icon={<CheckCircleOutlined />}>
                评审通过
              </Button>
            </Popconfirm>
          )}
          {(project.status === 'reviewed' || project.status === 'writing') && (
            <Button type="primary" onClick={() => navigateTo(`/textbook/collaboration/${projectId}`)}>
              编写管理
            </Button>
          )}
          {canExport && (
            <Dropdown
              menu={{
                items: [
                  { key: 'md', label: 'Markdown' },
                  { key: 'xlsx', label: 'Excel' },
                ],
                onClick: ({ key }) => handleExport(key as 'md' | 'xlsx'),
              }}
            >
              <Button loading={exporting}>
                导出 <DownOutlined />
              </Button>
            </Dropdown>
          )}
        </Space>
      }
    >
      {summary && (
        <Alert
          type={summary.balanced ? 'success' : 'error'}
          showIcon
          style={{ marginBottom: 12 }}
          message={
            summary.balanced
              ? `合计校验通过：章合计 ${summary.chapterSum} = 总字数 ${summary.totalWords}`
              : `合计不平衡：章合计 ${summary.chapterSum} ≠ 总字数 ${summary.totalWords}`
          }
          description={
            !summary.balanced && summary.unbalancedChapters?.length
              ? `不平衡章节：${summary.unbalancedChapters.join('、')}`
              : undefined
          }
        />
      )}
      {warnings.length > 0 && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message={`解析警告 ${warnings.length} 条`}
          description={warnings.map((w, i) => (
            <div key={i}>{w}</div>
          ))}
        />
      )}
      {structureLocked && !isDone && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="大纲结构已冻结（存在章分配或正文）"
          description="标题、目标字数与同级排序仍可调整；新增/删除节点与重建已禁用。如需重建，请先在编写管理中取消全部分配并确认无正文。"
        />
      )}
      {isDone && (
        <Alert
          type="success"
          showIcon
          style={{ marginBottom: 12 }}
          message="工程已完成编写，本页只读"
          description="如需继续修改，请先在教材工程列表进入编写管理并重新打开工程。"
        />
      )}

      {flat.length === 0 ? (
        <Empty
          description={
            project.status === 'parsed'
              ? '骨架未生成，可点击下方按钮重新生成'
              : '暂无大纲节点'
          }
        >
          {canManage && (
            <Button type="primary" onClick={() => rebuildMutation.mutate()}>
              重新生成骨架
            </Button>
          )}
        </Empty>
      ) : (
        <Table<TreeRow>
          rowKey="id"
          size="small"
          loading={outlineQuery.isLoading}
          dataSource={treeData}
          columns={columns}
          expandable={{ defaultExpandAllRows: true }}
          pagination={false}
          scroll={{ x: 1100 }}
        />
      )}

      <AddChildModal
        projectId={projectId}
        parent={addingUnder}
        onClose={() => setAddingUnder(null)}
        onSaved={() => {
          setAddingUnder(null);
          invalidateAll();
        }}
      />
    </Card>
  );
}

// ==================================================================
// 新增子节点弹窗（人工补充节/点，source_no 为空）
// ==================================================================

function AddChildModal({
  projectId,
  parent,
  onClose,
  onSaved,
}: {
  projectId: string;
  parent: TreeRow | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [title, setTitle] = useState('');
  const [dimension, setDimension] = useState<OutlineDimension>('theory');

  useEffect(() => {
    setTitle('');
    setDimension('theory');
  }, [parent]);

  if (!parent) return null;

  const leaf = parent.nodeLevel >= 3;

  return (
    <Modal
      open
      title={`在「${parent.title}」下新增${leaf ? '点' : '子节点'}`}
      onCancel={onClose}
      onOk={async () => {
        if (!title.trim()) {
          message.warning('请填写节点标题');
          return;
        }
        try {
          await createOutlineNode({
            projectId,
            parentId: parent.id,
            title: title.trim(),
            dimension: leaf ? dimension : null,
          });
          message.success('已新增');
          onSaved();
        } catch (e) {
          message.error(errorText(e, '新增失败'));
        }
      }}
      destroyOnHidden
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <div>
          <div style={{ marginBottom: 4 }}>节点标题</div>
          <Input.TextArea rows={2} value={title} onChange={(e) => setTitle(e.target.value)} maxLength={255} />
        </div>
        {leaf && (
          <div>
            <div style={{ marginBottom: 4 }}>维度</div>
            <Select
              value={dimension}
              style={{ width: 160 }}
              onChange={setDimension}
              options={[
                { value: 'theory', label: '理论' },
                { value: 'skill', label: '技能' },
              ]}
            />
          </div>
        )}
      </Space>
    </Modal>
  );
}

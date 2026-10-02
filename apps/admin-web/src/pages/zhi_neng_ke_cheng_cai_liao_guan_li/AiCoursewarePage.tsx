/**
 * AI 课件生成管理 —— 方案 v1.2 §14
 * 六个视图合一(操作平铺,不用路由跳转):项目列表 / 创建课件弹窗 / 大纲确认(按章分组) /
 * 生成进度 / 页明细抽屉 / 导出问题清单。
 * 权限码:courseware:project:list/create/confirm/generate/retry/export
 */
import React, { useEffect, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Button, Drawer, Input, Modal, Popconfirm, Progress, Select, Space, Table, Tag,
  Tooltip, Typography, Upload, message,
} from 'antd';
import {
  ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, DownloadOutlined,
  FilePptOutlined, PlusOutlined, ReloadOutlined, UploadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { hasPermission } from '../../directives/permission';
import { fetchPlatformFileBlob, uploadPlatformFile } from '../../api/files';
import {
  addOutlinePage, cancelCoursewareProject, confirmCoursewareOutline, createCoursewareProject,
  deleteCoursewareProject, deleteOutlinePage, exportCoursewareProject, getCoursewareOutline,
  getCoursewareProject, getCoursewareProgress, getCoursewareSlide, listCoursewareProjects,
  reorderOutline, retryCoursewareSlide, updateOutlinePage,
} from './coursewareApi';
import type {
  CoursewareIssue, CoursewareOutlinePage, CoursewareProject, CoursewareSlideBrief,
} from './coursewareApi';

// ------------------------------------------------------------------
// 常量与工具
// ------------------------------------------------------------------

const STATUS_TEXT: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  parsing: { text: '正在解析文档', color: 'processing' },
  digesting: { text: '正在整理素材', color: 'processing' },
  outline_generating: { text: '正在生成大纲', color: 'processing' },
  outline_ready: { text: '大纲待确认', color: 'gold' },
  generating: { text: '正在生成页面', color: 'processing' },
  ready: { text: '已完成', color: 'success' },
  partial: { text: '部分失败', color: 'warning' },
  failed: { text: '失败', color: 'error' },
  cancelled: { text: '已取消', color: 'default' },
};

const DENSITY_OPTIONS = [
  { value: 'concise', label: '精简(要点更少更短)' },
  { value: 'medium', label: '适中(默认)' },
  { value: 'detailed', label: '详细(要点更多更全)' },
];

const CONTENT_LAYOUT_OPTIONS = [
  { value: 'content_bullets', label: '要点页' },
  { value: 'content_table', label: '表格页' },
  { value: 'content_kpi', label: '数据页' },
];

const TERMINAL = new Set(['ready', 'partial', 'failed', 'cancelled']);

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function issueTag(issue: CoursewareIssue) {
  return issue.severity === 'error'
    ? <Tag color="error">需处理</Tag>
    : <Tag color="warning">建议检查</Tag>;
}

// ------------------------------------------------------------------
// 主页面
// ------------------------------------------------------------------

export default function AiCoursewarePage() {
  const queryClient = useQueryClient();
  const [view, setView] = useState<{ kind: 'list' } | { kind: 'outline'; projectId: string }
    | { kind: 'progress'; projectId: string }>({ kind: 'list' });
  const [createOpen, setCreateOpen] = useState(false);

  return (
    <div style={{ padding: 16 }}>
      {view.kind === 'list' && (
        <ProjectListView
          onCreate={() => setCreateOpen(true)}
          onOpen={(project) => setView(project.status === 'outline_ready'
            || ['parsing', 'digesting', 'outline_generating'].includes(project.status)
            ? { kind: 'outline', projectId: project.id }
            : { kind: 'progress', projectId: project.id })}
          onProgress={(id) => setView({ kind: 'progress', projectId: id })}
        />
      )}
      {view.kind === 'outline' && (
        <OutlineView
          projectId={view.projectId}
          onBack={() => setView({ kind: 'list' })}
          onConfirmed={(id) => setView({ kind: 'progress', projectId: id })}
        />
      )}
      {view.kind === 'progress' && (
        <ProgressView
          projectId={view.projectId}
          onBack={() => setView({ kind: 'list' })}
        />
      )}
      <CreateProjectModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(project) => {
          setCreateOpen(false);
          queryClient.invalidateQueries({ queryKey: ['courseware-projects'] });
          setView({ kind: 'outline', projectId: project.id });
        }}
      />
    </div>
  );
}

// ------------------------------------------------------------------
// 视图 1:项目列表
// ------------------------------------------------------------------

function ProjectListView({ onCreate, onOpen, onProgress }: {
  onCreate: () => void;
  onOpen: (project: CoursewareProject) => void;
  onProgress: (id: string) => void;
}) {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['courseware-projects', page, pageSize],
    queryFn: () => listCoursewareProjects({ page, pageSize }),
  });

  const columns: ColumnsType<CoursewareProject> = [
    { title: '课件标题', dataIndex: 'title', ellipsis: true },
    { title: '受众', dataIndex: 'audience', width: 110 },
    {
      title: '状态', dataIndex: 'status', width: 130,
      render: (s: string, row) => (
        <Space size={4}>
          <Tag color={STATUS_TEXT[s]?.color}>{STATUS_TEXT[s]?.text ?? s}</Tag>
          {(s === 'generating') && <span>{row.readyPages + row.failedPages}/{row.totalPages}</span>}
        </Space>
      ),
    },
    {
      title: '进度', width: 120,
      render: (_, row) => row.totalPages > 0
        ? `${row.readyPages} 成 / ${row.failedPages} 败 / 共 ${row.totalPages} 页`
        : '—',
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 170 },
    {
      title: '操作', width: 210,
      render: (_, row) => (
        <Space size={8} wrap>
          {row.status === 'outline_ready' && (
            <Button size="small" type="link" onClick={() => onOpen(row)}>确认大纲</Button>
          )}
          {['parsing', 'digesting', 'outline_generating'].includes(row.status) && (
            <Button size="small" type="link" onClick={() => onOpen(row)}>查看进度</Button>
          )}
          {['generating', 'ready', 'partial', 'failed', 'cancelled'].includes(row.status) && (
            <Button size="small" type="link" onClick={() => onProgress(row.id)}>
              {row.status === 'generating' ? '生成进度' : '页面与导出'}
            </Button>
          )}
          {hasPermission('courseware:project:create') && row.status !== 'generating' && (
            <Popconfirm
              title="删除该课件项目?"
              onConfirm={async () => {
                await deleteCoursewareProject(row.id);
                message.success('已删除');
                queryClient.invalidateQueries({ queryKey: ['courseware-projects'] });
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
    <>
      <Space style={{ marginBottom: 12, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={5} style={{ margin: 0 }}>AI 课件生成</Typography.Title>
        {hasPermission('courseware:project:create') && (
          <Button type="primary" icon={<FilePptOutlined />} onClick={onCreate}
            data-testid="courseware-create-btn">
            上传标准文档生成课件
          </Button>
        )}
      </Space>
      <Table<CoursewareProject>
        rowKey="id"
        size="middle"
        loading={isLoading}
        dataSource={data?.items ?? []}
        columns={columns}
        pagination={{
          current: page, pageSize, total: data?.total ?? 0,
          showSizeChanger: true,
          onChange: (p, ps) => {
            if (ps !== pageSize) {
              setPageSize(ps);
              setPage(1);
            } else {
              setPage(p);
            }
          },
        }}
      />
    </>
  );
}

// ------------------------------------------------------------------
// 视图 2:创建课件弹窗
// ------------------------------------------------------------------

function CreateProjectModal({ open, onClose, onCreated }: {
  open: boolean;
  onClose: () => void;
  onCreated: (project: CoursewareProject) => void;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [audience, setAudience] = useState('学员自学');
  const [density, setDensity] = useState('medium');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    if (!file) {
      message.warning('请先选择标准文档(.docx)');
      return;
    }
    setSubmitting(true);
    try {
      const uploaded = await uploadPlatformFile(file, 'private');
      const project = await createCoursewareProject({
        sourceFileId: String(uploaded.id),
        title: title.trim() || undefined,
        audience: audience.trim() || undefined,
        contentDensity: density,
      });
      message.success('已创建,正在解析文档');
      onCreated(project);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="上传标准文档生成课件"
      open={open}
      onCancel={onClose}
      onOk={submit}
      confirmLoading={submitting}
      okText="开始生成大纲"
      destroyOnHidden
    >
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        <Upload.Dragger
          accept=".docx"
          maxCount={1}
          beforeUpload={(f) => {
            setFile(f);
            if (!title.trim()) {
              setTitle(f.name.replace(/\.docx$/i, ''));
            }
            return false;
          }}
          onRemove={() => setFile(null)}
        >
          <p className="ant-upload-drag-icon"><UploadOutlined /></p>
          <p className="ant-upload-text">点击或拖拽标准文档(.docx)到此处</p>
          <p className="ant-upload-hint">只支持文字版 .docx;上传后先整理素材和大纲,你确认后才开始生成页面</p>
        </Upload.Dragger>
        <Input
          addonBefore="课件标题"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={120}
        />
        <Input
          addonBefore="受众"
          value={audience}
          onChange={(e) => setAudience(e.target.value)}
          maxLength={100}
          placeholder="默认:学员自学"
        />
        <div>
          <span style={{ marginRight: 8 }}>文字量</span>
          <Select
            value={density}
            onChange={setDensity}
            options={DENSITY_OPTIONS}
            style={{ width: 240 }}
          />
        </div>
      </Space>
    </Modal>
  );
}

// ------------------------------------------------------------------
// 视图 3:大纲确认(按章分组,平铺编辑)
// ------------------------------------------------------------------

function OutlineView({ projectId, onBack, onConfirmed }: {
  projectId: string;
  onBack: () => void;
  onConfirmed: (id: string) => void;
}) {
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  const [pages, setPages] = useState<CoursewareOutlinePage[]>([]);

  // 项目状态轮询:大纲未就绪时每 3s 刷新,就绪后停止
  const { data: project } = useQuery({
    queryKey: ['courseware-project', projectId],
    queryFn: () => getCoursewareProject(projectId),
    refetchInterval: (q) =>
      q.state.data && ['outline_ready', ...TERMINAL].includes(q.state.data.status) ? false : 3000,
  });

  const outlineQuery = useQuery({
    queryKey: ['courseware-outline', projectId],
    queryFn: () => getCoursewareOutline(projectId),
    enabled: project?.status === 'outline_ready',
  });

  useEffect(() => {
    if (outlineQuery.data) {
      setPages(outlineQuery.data);
    }
  }, [outlineQuery.data]);

  const busy = project && ['parsing', 'digesting', 'outline_generating'].includes(project.status);
  const ready = project?.status === 'outline_ready';

  const chapters = useMemo(() => {
    const groups = new Map<number, CoursewareOutlinePage[]>();
    for (const p of pages) {
      const key = p.chapterNo || 0;
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key)!.push(p);
    }
    return [...groups.entries()].sort((a, b) => a[0] - b[0]);
  }, [pages]);

  const patchLocal = (id: string, patch: Partial<CoursewareOutlinePage>) => {
    setPages((prev) => prev.map((p) => (p.id === id ? { ...p, ...patch } : p)));
  };

  const saveField = async (id: string, patch: Parameters<typeof updateOutlinePage>[1]) => {
    await updateOutlinePage(id, patch);
    message.success('已保存');
  };

  const move = async (index: number, dir: -1 | 1) => {
    const target = index + dir;
    if (target < 0 || target >= pages.length) {
      return;
    }
    const next = [...pages];
    [next[index], next[target]] = [next[target], next[index]];
    setPages(next);
    await reorderOutline(projectId, next.map((p) => p.id));
    queryClient.invalidateQueries({ queryKey: ['courseware-outline', projectId] });
  };

  const confirm = async () => {
    setConfirming(true);
    try {
      await confirmCoursewareOutline(projectId);
      message.success('大纲已确认,开始生成页面');
      onConfirmed(projectId);
    } finally {
      setConfirming(false);
    }
  };

  if (busy) {
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <Button onClick={onBack}>返回列表</Button>
        <Alert
          type="info"
          showIcon
          message={STATUS_TEXT[project?.status ?? '']?.text ?? '处理中'}
          description="正在解析文档并整理素材、生成大纲,通常需要 1~2 分钟,本页会自动刷新。"
        />
        {project?.status === 'failed' && (
          <Alert type="error" message="处理失败" description={project.error} />
        )}
      </Space>
    );
  }

  if (project?.status === 'failed') {
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <Button onClick={onBack}>返回列表</Button>
        <Alert type="error" message="大纲生成失败" description={project.error} />
      </Space>
    );
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      <Space style={{ justifyContent: 'space-between', width: '100%' }}>
        <Space>
          <Button onClick={onBack}>返回列表</Button>
          <Typography.Title level={5} style={{ margin: 0 }}>
            确认课件大纲(共 {pages.length} 页)
          </Typography.Title>
        </Space>
        {hasPermission('courseware:project:confirm') && ready && (
          <Button type="primary" loading={confirming} onClick={confirm}
            data-testid="outline-confirm-btn">
            确认大纲,开始生成
          </Button>
        )}
      </Space>
      <Alert
        type="info"
        showIcon
        message="标题、要点都可以直接改;可以增删页、调整顺序。确认后才开始生成页面(生成后才消耗大模型调用)。"
      />
      {chapters.map(([chapterNo, chapterPages]) => (
        <div key={chapterNo}>
          <ChapterHeader
            chapterNo={chapterNo}
            chapterTitle={chapterPages[0]?.chapterTitle ?? ''}
            pages={chapterPages}
            editable={ready && hasPermission('courseware:project:confirm')}
            onRename={async (name) => {
              for (const p of chapterPages) {
                patchLocal(p.id, { chapterTitle: name });
              }
              await Promise.all(chapterPages.map((p) =>
                updateOutlinePage(p.id, { chapterTitle: name })));
              message.success('章标题已保存');
            }}
            onAdd={async () => {
              const created = await addOutlinePage(projectId, {
                chapterNo,
                title: '新页面(请编辑标题)',
                keyPoints: [],
              });
              message.success('已新增页面');
              setPages((prev) => [...prev, created]
                .sort((a, b) => a.position - b.position));
              queryClient.invalidateQueries({ queryKey: ['courseware-outline', projectId] });
            }}
          />
          {chapterPages.map((p) => {
            const globalIndex = pages.findIndex((x) => x.id === p.id);
            return (
              <OutlinePageCard
                key={p.id}
                page={p}
                editable={ready && hasPermission('courseware:project:confirm')}
                onPatch={patchLocal}
                onSave={saveField}
                onMove={(dir) => move(globalIndex, dir)}
                onDelete={async () => {
                  await deleteOutlinePage(p.id);
                  message.success('已删除');
                  setPages((prev) => prev.filter((x) => x.id !== p.id));
                  queryClient.invalidateQueries({ queryKey: ['courseware-outline', projectId] });
                }}
              />
            );
          })}
        </div>
      ))}
    </Space>
  );
}

function ChapterHeader({ chapterNo, chapterTitle, pages, editable, onRename, onAdd }: {
  chapterNo: number;
  chapterTitle: string;
  pages: CoursewareOutlinePage[];
  editable: boolean;
  onRename: (name: string) => Promise<void>;
  onAdd: () => Promise<void>;
}) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(chapterTitle);
  useEffect(() => setName(chapterTitle), [chapterTitle]);
  if (chapterNo === 0) {
    return <Typography.Title level={5}>封面与目录</Typography.Title>;
  }
  return (
    <Space style={{ marginTop: 8 }}>
      {editing ? (
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          onBlur={async () => {
            setEditing(false);
            if (name.trim() && name !== chapterTitle) {
              await onRename(name.trim());
            }
          }}
          onPressEnter={() => document.activeElement instanceof HTMLElement
            && document.activeElement.blur()}
          style={{ width: 320 }}
        />
      ) : (
        <Typography.Title
          level={5}
          style={{ margin: 0, cursor: editable ? 'pointer' : 'default' }}
          onClick={() => editable && setEditing(true)}
        >
          第 {chapterNo} 章:{chapterTitle || '(未命名)'}
        </Typography.Title>
      )}
      {editable && (
        <Button size="small" icon={<PlusOutlined />} onClick={onAdd}
          data-testid={`chapter-add-${chapterNo}`}>本章加页</Button>
      )}
    </Space>
  );
}

function OutlinePageCard({ page, editable, onPatch, onSave, onMove, onDelete }: {
  page: CoursewareOutlinePage;
  editable: boolean;
  onPatch: (id: string, patch: Partial<CoursewareOutlinePage>) => void;
  onSave: (id: string, patch: Parameters<typeof updateOutlinePage>[1]) => Promise<void>;
  onMove: (dir: -1 | 1) => Promise<void>;
  onDelete: () => Promise<void>;
}) {
  const roleText: Record<string, string> = {
    cover: '封面', toc: '目录', section: '章节页', content: '内容页', review: '复习思考题',
  };
  return (
    <div
      data-testid={`outline-card-${page.id}`}
      style={{
        border: '1px solid #f0f0f0', borderRadius: 8, padding: 12, marginTop: 8,
        background: '#fff',
      }}
    >
      <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
        <Space direction="vertical" style={{ flex: 1, width: '100%' }} size={4}>
          <Space size={8}>
            <Tag>{page.position}</Tag>
            <Tag color="blue">{roleText[page.pageRole] ?? page.pageRole}</Tag>
            {page.fixed && <Tag>固定页</Tag>}
            {editable ? (
              <Input
                value={page.title}
                onChange={(e) => onPatch(page.id, { title: e.target.value })}
                onBlur={() => onSave(page.id, { title: page.title })}
                style={{ width: 480 }}
                maxLength={120}
                data-testid={`outline-title-${page.id}`}
              />
            ) : (
              <Typography.Text strong>{page.title}</Typography.Text>
            )}
          </Space>
          {page.pageRole === 'content' && (
            <Space size={8} wrap>
              <span style={{ color: '#888' }}>要点(每行一条):</span>
              {editable ? (
                <Input.TextArea
                  value={(page.keyPoints ?? []).join('\n')}
                  onChange={(e) => onPatch(page.id, {
                    keyPoints: e.target.value.split('\n').filter((s) => s.trim()),
                  })}
                  onBlur={() => onSave(page.id, { keyPoints: page.keyPoints })}
                  autoSize={{ minRows: 2, maxRows: 6 }}
                  style={{ width: 560 }}
                />
              ) : (
                <ul style={{ margin: 0, paddingInlineStart: 18 }}>
                  {(page.keyPoints ?? []).map((k, i) => <li key={i}>{k}</li>)}
                </ul>
              )}
              {editable && (
                <Select
                  value={page.layoutId}
                  onChange={(v) => {
                    onPatch(page.id, { layoutId: v });
                    onSave(page.id, { layoutId: v });
                  }}
                  options={CONTENT_LAYOUT_OPTIONS}
                  style={{ width: 120 }}
                />
              )}
            </Space>
          )}
          {page.pageRole !== 'content' && (page.keyPoints?.length ?? 0) > 0 && (
            <ul style={{ margin: 0, paddingInlineStart: 18 }}>
              {page.keyPoints.map((k, i) => <li key={i}>{k}</li>)}
            </ul>
          )}
        </Space>
        {editable && (
          <Space size={4}>
            <Button size="small" icon={<ArrowUpOutlined />} onClick={() => onMove(-1)} />
            <Button size="small" icon={<ArrowDownOutlined />} onClick={() => onMove(1)} />
            {!page.fixed && (
              <Popconfirm title="删除该页?" onConfirm={onDelete}>
                <Button size="small" danger icon={<DeleteOutlined />}
                  data-testid={`outline-delete-${page.id}`} />
              </Popconfirm>
            )}
          </Space>
        )}
      </Space>
    </div>
  );
}

// ------------------------------------------------------------------
// 视图 4:生成进度 + 页列表 + 导出
// ------------------------------------------------------------------

function ProgressView({ projectId, onBack }: { projectId: string; onBack: () => void }) {
  const queryClient = useQueryClient();
  const [detailId, setDetailId] = useState<string | null>(null);
  const [exportIssues, setExportIssues] = useState<CoursewareIssue[] | null>(null);
  const [exporting, setExporting] = useState(false);

  const { data: progress } = useQuery({
    queryKey: ['courseware-progress', projectId],
    queryFn: () => getCoursewareProgress(projectId),
    // 终态停止轮询(方案 §14)
    refetchInterval: (q) => (q.state.data && TERMINAL.has(q.state.data.status) ? false : 3000),
  });

  const { data: project } = useQuery({
    queryKey: ['courseware-project', projectId],
    queryFn: () => getCoursewareProject(projectId),
  });

  const exportPptx = async () => {
    setExporting(true);
    try {
      const result = await exportCoursewareProject(projectId);
      if (result.blocked) {
        setExportIssues(result.issues);
        return;
      }
      if (result.warnings?.length) {
        Modal.info({
          title: '导出成功,以下建议可在 PowerPoint 中检查后调整',
          content: (
            <ul style={{ paddingInlineStart: 18 }}>
              {result.warnings.slice(0, 10).map((w, i) => <li key={i}>{w.message}</li>)}
            </ul>
          ),
        });
      }
      if (result.fileId) {
        const blob = await fetchPlatformFileBlob(String(result.fileId));
        downloadBlob(blob, `${project?.title ?? '课件'}.pptx`);
        message.success('PPTX 已开始下载');
      }
    } finally {
      setExporting(false);
    }
  };

  const slideStatusTag = (s: CoursewareSlideBrief['status']) => {
    const map: Record<string, { text: string; color: string }> = {
      pending: { text: '排队中', color: 'default' },
      generating: { text: '生成中', color: 'processing' },
      ready: { text: '已完成', color: 'success' },
      failed: { text: '失败', color: 'error' },
      cancelled: { text: '已取消', color: 'default' },
    };
    return <Tag color={map[s]?.color}>{map[s]?.text ?? s}</Tag>;
  };

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Space>
          <Button onClick={onBack}>返回列表</Button>
          <Typography.Title level={5} style={{ margin: 0 }}>
            {project?.title ?? '课件'} — 页面与导出
          </Typography.Title>
        </Space>
        <Space>
          {progress?.status === 'generating'
            && hasPermission('courseware:project:generate') && (
            <Popconfirm
              title="取消生成?已完成的页面会保留"
              onConfirm={async () => {
                await cancelCoursewareProject(projectId);
                message.success('已取消');
                queryClient.invalidateQueries({ queryKey: ['courseware-progress', projectId] });
              }}
            >
              <Button danger>取消生成</Button>
            </Popconfirm>
          )}
          {['ready', 'partial', 'cancelled'].includes(progress?.status ?? '')
            && hasPermission('courseware:project:export') && (
            <Button type="primary" icon={<DownloadOutlined />} loading={exporting}
              onClick={exportPptx} data-testid="courseware-export-btn">
              导出 PPTX
            </Button>
          )}
        </Space>
      </Space>

      {progress && (
        <Progress
          percent={progress.totalPages > 0
            ? Math.round(((progress.readyPages + progress.failedPages) / progress.totalPages) * 100)
            : 0}
          status={progress.status === 'failed' ? 'exception' : undefined}
          format={() => `${progress.readyPages + progress.failedPages}/${progress.totalPages} 页`}
        />
      )}
      {progress?.error && <Alert type="warning" showIcon message={progress.error} />}

      <Table<CoursewareSlideBrief>
        rowKey="id"
        size="small"
        dataSource={progress?.slides ?? []}
        pagination={false}
        columns={[
          { title: '页序', dataIndex: 'position', width: 70 },
          { title: '标题', dataIndex: 'title', ellipsis: true },
          {
            title: '状态', dataIndex: 'status', width: 100,
            render: (s: CoursewareSlideBrief['status']) => slideStatusTag(s),
          },
          { title: '说明', dataIndex: 'error', ellipsis: true,
            render: (e: string | null | undefined) => e ?? '—' },
          {
            title: '操作', width: 160,
            render: (_, row) => (
              <Space size={4}>
                {row.status === 'ready' && (
                  <Button size="small" type="link" onClick={() => setDetailId(row.id)}
                    data-testid={`slide-view-${row.id}`}>
                    查看内容
                  </Button>
                )}
                {row.status === 'failed' && hasPermission('courseware:project:retry') && (
                  <Button
                    size="small" type="link" icon={<ReloadOutlined />}
                    data-testid={`slide-retry-${row.id}`}
                    onClick={async () => {
                      await retryCoursewareSlide(row.id);
                      message.success('已重新生成该页,正在生成中');
                      // 重试异步执行(约 10 秒),项目已是终态轮询已停:
                      // 先立即刷新看到"生成中",再轮询到该页终态自动更新列表(用户口径 2026-09-22)
                      await queryClient.refetchQueries({
                        queryKey: ['courseware-progress', projectId],
                      });
                      const deadline = Date.now() + 120_000;
                      while (Date.now() < deadline) {
                        await new Promise((r) => setTimeout(r, 3000));
                        const fresh = await queryClient.fetchQuery({
                          queryKey: ['courseware-progress', projectId],
                          queryFn: () => getCoursewareProgress(projectId),
                          staleTime: 0,
                        });
                        const target = fresh.slides.find((x) => x.id === row.id);
                        if (!target || target.status === 'ready' || target.status === 'failed'
                          || target.status === 'cancelled') {
                          await queryClient.refetchQueries({
                            queryKey: ['courseware-progress', projectId],
                          });
                          break;
                        }
                      }
                    }}
                  >
                    重试
                  </Button>
                )}
              </Space>
            ),
          },
        ]}
      />

      <SlideDetailDrawer slideId={detailId} onClose={() => setDetailId(null)} />

      <Modal
        title="有页面存在问题,暂不能导出"
        open={exportIssues !== null}
        onCancel={() => setExportIssues(null)}
        footer={<Button type="primary" onClick={() => setExportIssues(null)}>知道了</Button>}
      >
        <Alert
          type="error" showIcon style={{ marginBottom: 8 }}
          message="请先处理下列页面的问题(可在页列表重试该页)"
        />
        <ul style={{ paddingInlineStart: 18 }}>
          {(exportIssues ?? []).map((issue, i) => (
            <li key={i}><Space size={4}>{issueTag(issue)}<span>{issue.message}</span></Space></li>
          ))}
        </ul>
      </Modal>
    </Space>
  );
}

// ------------------------------------------------------------------
// 视图 5/6:页明细抽屉(blocks 文本化呈现 + issues + 学习提示)
// ------------------------------------------------------------------

function SlideDetailDrawer({ slideId, onClose }: { slideId: string | null; onClose: () => void }) {
  const { data: slide } = useQuery({
    queryKey: ['courseware-slide', slideId],
    queryFn: () => getCoursewareSlide(slideId!),
    enabled: slideId !== null,
  });

  return (
    <Drawer
      title={slide ? `第 ${slide.position} 页:${slide.title ?? ''}` : '页面内容'}
      open={slideId !== null}
      onClose={onClose}
      width={640}
    >
      {slide?.issues && slide.issues.length > 0 && (
        <Alert
          type="warning" showIcon style={{ marginBottom: 12 }}
          message="本页有建议检查的地方"
          description={(
            <ul style={{ paddingInlineStart: 18, marginBottom: 0 }}>
              {slide.issues.map((issue, i) => (
                <li key={i}>{issue.message}</li>
              ))}
            </ul>
          )}
        />
      )}
      {(slide?.blocks ?? []).map((block, i) => (
        <div key={i} style={{ marginBottom: 12 }}>
          {block.type === 'text' && (
            <Typography.Title level={5}>{block.text}</Typography.Title>
          )}
          {block.type === 'bullets' && (
            <ul style={{ paddingInlineStart: 18 }}>
              {(block.items ?? []).map((item, j) => <li key={j}>{item}</li>)}
            </ul>
          )}
          {block.type === 'table' && (
            <Table
              size="small"
              pagination={false}
              rowKey={(r) => Object.values(r).join('|')}
              columns={(block.header ?? []).map((h, j) => ({
                title: h, dataIndex: String(j),
              }))}
              dataSource={(block.rows ?? []).map((row) =>
                Object.fromEntries(row.map((cell, j) => [String(j), cell])))}
            />
          )}
          {block.type === 'kpi' && (
            <Tag color="blue" style={{ fontSize: 14, padding: '6px 12px' }}>
              {block.value} · {block.label}{block.note ? `(${block.note})` : ''}
            </Tag>
          )}
        </div>
      ))}
      {slide?.speakerNotes && (
        <Alert type="info" showIcon message="学习提示" description={slide.speakerNotes} />
      )}
    </Drawer>
  );
}

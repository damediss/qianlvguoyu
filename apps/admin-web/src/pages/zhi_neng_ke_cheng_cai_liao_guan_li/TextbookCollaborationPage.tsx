/**
 * 教研分配与全册进度页 —— 《二期编写协同实施设计》§10.3（TextbookCollaborationPage.tsx）
 * 隐藏路由 /textbook/collaboration/:projectId（载体菜单 textbook_project）。
 *
 * 主操作平铺（不藏下拉）：批量分配 / 导出审阅稿 / 完成编写（条件不足禁用+tooltip）/ 正式导出（done）/ 重新打开（done）。
 * 分配：候选人远程搜索（300ms 防抖）；批量分配单事务；取消分配必须 Popconfirm。
 */
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ExportOutlined,
  FileAddOutlined,
  TeamOutlined,
  UndoOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { MdPreview } from 'md-editor-rt';
import 'md-editor-rt/lib/style.css';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import dayjs from 'dayjs';
import {
  cancelAssignment,
  completeProject,
  exportBookDocx,
  exportChapterDocx,
  getCollaboration,
  getSectionAssetContent,
  getSectionContent,
  pageCollaborators,
  reopenProject,
  saveAssignments,
} from '../../api/textbookCollaboration';
import { useAuthStore } from '../../stores/authStore';
import { useNavStore } from '../../stores/navStore';
import { parseContentDispositionFilename } from '../../utils/download';
import { apiErrorMessage, extractAssignmentConflict, extractCompletionBlockers } from '../../utils/apiError';
import { resolveTextbookAssetUris, revokeObjectUrls, sanitizeTextbookHtml } from '../../utils/textbookMarkdown';
import { textbookLevelLabel } from '../../types/textbook';
import type {
  ChapterAssignmentItem,
  CollaborationChapter,
  CollaboratorVO,
  SectionContentVO,
  TextbookCollaborationVO,
} from '../../types/textbookCollaboration';
import { ContentStatusTag, ProjectStatusTag, SectionProgress, TaskStatusTag } from './TextbookTaskMeta';

type ChapterStatusFilter = 'all' | 'unassigned' | 'writing' | 'reviewing' | 'returned' | 'approved';

const CHAPTER_STATUS_OPTIONS: { value: ChapterStatusFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'unassigned', label: '未分配' },
  { value: 'writing', label: '编写中' },
  { value: 'reviewing', label: '待审定' },
  { value: 'returned', label: '有打回' },
  { value: 'approved', label: '已完成' },
];

function matchChapterFilter(ch: CollaborationChapter, filter: ChapterStatusFilter): boolean {
  if (filter === 'all') return true;
  if (filter === 'unassigned') return !ch.assignment?.active;
  const status = ch.assignment?.active ? ch.assignment.taskStatus : undefined;
  if (filter === 'writing') return status === 'assigned' || status === 'writing';
  return status === filter;
}

function downloadBlob(resp: { data: unknown; headers?: unknown }, fallbackName: string) {
  const blob = resp.data as Blob;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = parseContentDispositionFilename(resp) ?? fallbackName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function TextbookCollaborationPage({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient();
  const setSelectedKey = useNavStore((s) => s.setSelectedKey);
  const hasPermission = useAuthStore((s) => s.hasPermission);
  const canManage = hasPermission('textbook:project:manage');
  const canExport = hasPermission('textbook:project:export');

  const [statusFilter, setStatusFilter] = useState<ChapterStatusFilter>('all');
  const [writerFilter, setWriterFilter] = useState<string | undefined>();
  const [reviewerFilter, setReviewerFilter] = useState<string | undefined>();
  const [keyword, setKeyword] = useState('');
  const [selectedChapterIds, setSelectedChapterIds] = useState<React.Key[]>([]);
  const [assignTarget, setAssignTarget] = useState<{ chapters: CollaborationChapter[] } | null>(null);
  const [viewSectionId, setViewSectionId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const collabQuery = useQuery({
    queryKey: ['textbook-collaboration', projectId],
    queryFn: () => getCollaboration(projectId),
  });
  const data: TextbookCollaborationVO | undefined = collabQuery.data;

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['textbook-collaboration', projectId] });
    queryClient.invalidateQueries({ queryKey: ['textbook-projects'] });
    queryClient.invalidateQueries({ queryKey: ['textbook-project', projectId] });
  };

  const writerOptions = useMemo(() => {
    const names = new Set<string>();
    data?.chapters.forEach((c) => {
      if (c.assignment?.active && c.assignment.writerName) names.add(c.assignment.writerName);
    });
    return [...names].sort().map((n) => ({ value: n, label: n }));
  }, [data]);

  const reviewerOptions = useMemo(() => {
    const names = new Set<string>();
    data?.chapters.forEach((c) => {
      if (c.assignment?.active && c.assignment.reviewerName) names.add(c.assignment.reviewerName);
    });
    return [...names].sort().map((n) => ({ value: n, label: n }));
  }, [data]);

  const chapters = useMemo(() => {
    let list = data?.chapters || [];
    if (statusFilter !== 'all') list = list.filter((c) => matchChapterFilter(c, statusFilter));
    if (writerFilter) {
      list = list.filter((c) => c.assignment?.active && c.assignment.writerName === writerFilter);
    }
    if (reviewerFilter) {
      list = list.filter((c) => c.assignment?.active && c.assignment.reviewerName === reviewerFilter);
    }
    if (keyword.trim()) {
      const kw = keyword.trim();
      list = list.filter(
        (c) =>
          c.title.includes(kw) ||
          (c.sourceNo || '').includes(kw) ||
          (c.assignment?.writerName || '').includes(kw) ||
          (c.assignment?.reviewerName || '').includes(kw),
      );
    }
    return list;
  }, [data, statusFilter, writerFilter, reviewerFilter, keyword]);

  // ---- 分配 / 取消 / 完成 / 重开 ----
  const handleCancelAssignment = async (chapter: CollaborationChapter) => {
    if (!chapter.assignment) return;
    setBusy(true);
    try {
      await cancelAssignment(chapter.chapterId, chapter.assignment.assignmentVersion ?? 1);
      message.success('已取消分配（正文与历史保留）');
      invalidate();
    } catch (e) {
      const conflict = extractAssignmentConflict(e);
      if (conflict) {
        message.error('分配已被他人更新，请刷新后重试');
        invalidate();
      } else {
        message.error(apiErrorMessage(e, '取消分配失败'));
      }
    } finally {
      setBusy(false);
    }
  };

  const handleComplete = async () => {
    setBusy(true);
    try {
      await completeProject(projectId);
      message.success('已完成编写，可导出正式 docx');
      invalidate();
    } catch (e) {
      const blockers = extractCompletionBlockers(e);
      if (blockers) {
        Modal.error({
          title: '尚不满足完成条件',
          content: (
            <ul style={{ paddingLeft: 18, margin: 0 }}>
              {blockers.map((b, i) => (
                <li key={i}>{b.message}</li>
              ))}
            </ul>
          ),
        });
      } else {
        message.error(apiErrorMessage(e, '完成编写失败'));
      }
    } finally {
      setBusy(false);
    }
  };

  const handleReopen = async () => {
    setBusy(true);
    try {
      await reopenProject(projectId);
      message.success('已重新打开为编写中');
      invalidate();
    } catch (e) {
      message.error(apiErrorMessage(e, '重新打开失败'));
    } finally {
      setBusy(false);
    }
  };

  const handleExportBook = async (mode: 'review' | 'final') => {
    try {
      const resp = await exportBookDocx(projectId, mode);
      downloadBlob(resp, `教材正文_${data?.project.workTypeTitle ?? ''}_${mode === 'review' ? '审阅稿' : '正式稿'}.docx`);
      message.success('导出成功');
    } catch (e) {
      message.error(apiErrorMessage(e, '导出失败'));
    }
  };

  const handleExportChapter = async (chapter: CollaborationChapter) => {
    try {
      const resp = await exportChapterDocx(chapter.chapterId);
      downloadBlob(resp, `教材章_${chapter.title}.docx`);
    } catch (e) {
      message.error(apiErrorMessage(e, '导出失败'));
    }
  };

  // ---- 章表格 ----
  const columns: ColumnsType<CollaborationChapter> = [
    {
      title: '章',
      dataIndex: 'title',
      render: (v: string, c) => (
        <Space size={4}>
          <span>{c.sourceNo ? `${c.sourceNo} ` : ''}{v}</span>
        </Space>
      ),
    },
    { title: '目标字数', dataIndex: 'targetWords', width: 100, render: (v: number | null) => v ?? '-' },
    {
      title: '主笔',
      width: 110,
      render: (_, c) => (c.assignment?.active ? c.assignment.writerName : <Tag>未分配</Tag>),
    },
    {
      title: '审定人',
      width: 110,
      render: (_, c) => (c.assignment?.active ? c.assignment.reviewerName : '-'),
    },
    {
      title: '截止日期',
      width: 110,
      render: (_, c) => c.assignment?.dueDate ?? '不限',
    },
    {
      title: '进度',
      width: 150,
      render: (_, c) => {
        const a = c.assignment;
        if (!a?.active || a.sectionCount === 0) return <Tag>未分配</Tag>;
        return <SectionProgress approved={a.approvedCount} total={a.sectionCount} />;
      },
    },
    {
      title: '状态',
      width: 90,
      render: (_, c) => <TaskStatusTag status={c.assignment?.active ? c.assignment.taskStatus : null} />,
    },
    {
      title: '最近分配',
      width: 150,
      render: (_, c) =>
        c.assignment ? `${c.assignment.assignedByName} · ${c.assignment.assignedAt}` : '-',
    },
    {
      title: '操作',
      width: 240,
      fixed: 'right',
      render: (_, c) => (
        <Space size={0}>
          {canManage && (
            <Button size="small" type="link" onClick={() => setAssignTarget({ chapters: [c] })}>
              {c.assignment?.active ? '改派' : '分配'}
            </Button>
          )}
          {canManage && c.assignment?.active && (
            <Popconfirm
              title={`取消「${c.title}」的分配？`}
              description="不删除正文与审定记录；主笔/审定人立即失去入口。"
              okText="取消分配"
              okButtonProps={{ danger: true }}
              onConfirm={() => void handleCancelAssignment(c)}
            >
              <Button size="small" type="link" danger disabled={busy}>
                取消分配
              </Button>
            </Popconfirm>
          )}
          <Button size="small" type="link" onClick={() => void handleExportChapter(c)}>
            导出本章
          </Button>
        </Space>
      ),
    },
  ];

  if (collabQuery.isLoading) {
    return (
      <Card>
        <Spin />
      </Card>
    );
  }
  if (collabQuery.isError || !data) {
    return (
      <Card>
        <Empty description="工程不存在或无权访问">
          <Button onClick={() => setSelectedKey('textbook_project')}>返回教材工程列表</Button>
        </Empty>
      </Card>
    );
  }

  const progress = data.progress;
  const returnedTotal = data.chapters.reduce((sum, c) => sum + (c.assignment?.active ? c.assignment.returnedCount : 0), 0);
  const blockersText = data.completion.blockers.map((b) => b.message).join('；');
  const status = data.project.status;

  return (
    <Card
      title={
        <Space wrap>
          <Button
            size="small"
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => setSelectedKey('textbook_project')}
          >
            返回工程列表
          </Button>
          <span>
            {data.project.workTypeTitle}
            {data.project.direction ? `（${data.project.direction} · ${textbookLevelLabel(data.project.levelCode)}）` : ''}
          </span>
          <ProjectStatusTag status={status} />
        </Space>
      }
      extra={
        <Space wrap>
          {canManage && (status === 'reviewed' || status === 'writing') && (
            <Button
              icon={<TeamOutlined />}
              onClick={() => {
                const targets = chapters.filter((c) => selectedChapterIds.includes(c.chapterId));
                if (targets.length === 0) {
                  message.warning('请先在表格中勾选要分配的章');
                  return;
                }
                setAssignTarget({ chapters: targets });
              }}
            >
              批量分配（已选 {selectedChapterIds.length}）
            </Button>
          )}
          {canExport && (status === 'writing' || status === 'done') && (
            <Button icon={<ExportOutlined />} onClick={() => void handleExportBook('review')}>
              导出审阅稿
            </Button>
          )}
          {canManage && status === 'writing' && (
            <Tooltip title={data.completion.canComplete ? '校验全部节已通过后流转为编写完成' : blockersText || '尚不满足完成条件'}>
              <Popconfirm
                title="确认完成编写？"
                description="完成后工程转为只读，可导出正式 docx；如需继续修改可重新打开。"
                okText="完成编写"
                onConfirm={() => void handleComplete()}
                disabled={!data.completion.canComplete}
              >
                <Button
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  disabled={!data.completion.canComplete || busy}
                  aria-label="完成编写"
                >
                  完成编写
                </Button>
              </Popconfirm>
            </Tooltip>
          )}
          {canExport && status === 'done' && (
            <Button type="primary" icon={<FileAddOutlined />} onClick={() => void handleExportBook('final')}>
              正式导出
            </Button>
          )}
          {canManage && status === 'done' && (
            <Popconfirm
              title="重新打开为「编写中」？"
              description="正文状态不变，审定人可继续撤销通过，主笔暂不可编辑已通过内容。"
              okText="重新打开"
              onConfirm={() => void handleReopen()}
            >
              <Button icon={<UndoOutlined />} disabled={busy}>
                重新打开
              </Button>
            </Popconfirm>
          )}
        </Space>
      }
    >
      {/* 顶部进度概览 */}
      <Descriptions
        size="small"
        bordered
        column={5}
        style={{ marginBottom: 12 }}
        items={[
          {
            key: 'progress',
            label: '总进度（节通过率）',
            children: `${progress.approvedSectionCount}/${progress.sectionCount}`,
          },
          { key: 'assigned', label: '章分配率', children: `${progress.assignedChapterCount}/${progress.chapterCount}` },
          { key: 'words', label: '已写字数/目标', children: `${progress.actualWords}/${progress.targetWords}` },
          { key: 'returned', label: '打回节数', children: returnedTotal },
          {
            key: 'completion',
            label: '完成条件',
            children: data.completion.canComplete ? <Tag color="green">满足</Tag> : <Tag color="orange">{data.completion.blockers.length} 项未满足</Tag>,
          },
        ]}
      />
      {!data.completion.canComplete && data.completion.blockers.length > 0 && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="完成编写前需逐项解决以下阻塞："
          description={
            <ul style={{ paddingLeft: 18, margin: 0 }}>
              {data.completion.blockers.slice(0, 8).map((b, i) => (
                <li key={i}>{b.message}</li>
              ))}
            </ul>
          }
        />
      )}

      {/* 筛选（前端过滤 C2 一次性返回的全章数据） */}
      <Space style={{ marginBottom: 12 }} wrap>
        <Select
          style={{ width: 120 }}
          value={statusFilter}
          options={CHAPTER_STATUS_OPTIONS}
          onChange={setStatusFilter}
        />
        <Select
          style={{ width: 140 }}
          placeholder="主笔"
          allowClear
          showSearch
          value={writerFilter}
          options={writerOptions}
          onChange={setWriterFilter}
        />
        <Select
          style={{ width: 140 }}
          placeholder="审定人"
          allowClear
          showSearch
          value={reviewerFilter}
          options={reviewerOptions}
          onChange={setReviewerFilter}
        />
        <Input.Search
          style={{ width: 220 }}
          placeholder="章名称/编号/人员"
          allowClear
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
      </Space>

      <Table<CollaborationChapter>
        rowKey="chapterId"
        size="middle"
        loading={collabQuery.isFetching}
        dataSource={chapters}
        columns={columns}
        rowSelection={
          canManage && (status === 'reviewed' || status === 'writing')
            ? {
                selectedRowKeys: selectedChapterIds,
                onChange: setSelectedChapterIds,
              }
            : undefined
        }
        expandable={{
          expandedRowRender: (c) => <SectionTable chapter={c} onView={(sectionId) => setViewSectionId(sectionId)} />,
          rowExpandable: (c) => c.sections.length > 0,
        }}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 章` }}
        scroll={{ x: 1200 }}
      />

      {assignTarget && (
        <AssignModal
          projectId={projectId}
          chapters={assignTarget.chapters}
          onClose={() => setAssignTarget(null)}
          onSaved={() => {
            setAssignTarget(null);
            setSelectedChapterIds([]);
            invalidate();
          }}
        />
      )}

      <SectionViewDrawer
        sectionId={viewSectionId}
        onClose={() => setViewSectionId(null)}
      />
    </Card>
  );
}

// ==================================================================
// 展开节表格：节标题、目标/实际字数、状态、提交时间、最近审定意见、查看正文
// ==================================================================

function SectionTable({ chapter, onView }: { chapter: CollaborationChapter; onView: (sectionId: string) => void }) {
  const columns: ColumnsType<CollaborationChapter['sections'][number]> = [
    {
      title: '节',
      render: (_, s) => `${s.sourceNo ? `${s.sourceNo} ` : ''}${s.title}`,
    },
    { title: '目标字数', dataIndex: 'targetWords', width: 90, render: (v: number | null) => v ?? '-' },
    { title: '实际字数', dataIndex: 'wordCount', width: 90 },
    {
      title: '状态',
      width: 90,
      render: (_, s) => <ContentStatusTag status={s.contentStatus} />,
    },
    { title: '提交时间', dataIndex: 'submittedAt', width: 150, render: (v: string | null) => v ?? '-' },
    {
      title: '最近审定意见',
      render: (_, s) =>
        s.latestReviewRecord
          ? `${s.latestReviewRecord.actorName}：${s.latestReviewRecord.comment || '无意见'}`
          : '-',
    },
    {
      title: '操作',
      width: 100,
      render: (_, s) => (
        <Button size="small" type="link" onClick={() => onView(s.sectionId)}>
          查看正文
        </Button>
      ),
    },
  ];
  return (
    <Table
      rowKey="sectionId"
      size="small"
      dataSource={[...chapter.sections].sort((a, b) => a.sortNo - b.sortNo)}
      columns={columns}
      pagination={false}
    />
  );
}

// ==================================================================
// 只读正文抽屉（教研可看全部章节正文，§2.1）
// ==================================================================

function SectionViewDrawer({ sectionId, onClose }: { sectionId: string | null; onClose: () => void }) {
  const [markdown, setMarkdown] = useState('');
  const objectUrlsRef = useRef(new Map<string, string>());

  const contentQuery = useQuery({
    queryKey: ['textbook-section-content', sectionId],
    queryFn: () => getSectionContent(sectionId!),
    enabled: !!sectionId,
  });
  const section: SectionContentVO | undefined = contentQuery.data;

  useEffect(() => {
    let cancelled = false;
    if (!section) return undefined;
    void resolveTextbookAssetUris(
      section.contentMd,
      (assetId) => getSectionAssetContent(section.sectionId, assetId),
      objectUrlsRef.current,
    ).then((resolved) => {
      if (!cancelled) setMarkdown(resolved);
    });
    return () => {
      cancelled = true;
    };
  }, [section]);

  useEffect(() => {
    if (sectionId) return undefined;
    revokeObjectUrls(objectUrlsRef.current);
    return undefined;
  }, [sectionId]);

  return (
    <Drawer
      title={section ? `${section.sectionTitle}（${section.chapterTitle}）` : '查看正文'}
      open={!!sectionId}
      width="min(960px, 94vw)"
      onClose={onClose}
      destroyOnClose
    >
      {contentQuery.isLoading && <Spin />}
      {contentQuery.isError && <Alert type="error" showIcon message="正文加载失败" />}
      {section && (
        <>
          <Space wrap style={{ marginBottom: 8 }}>
            <ContentStatusTag status={section.contentStatus} />
            <span>{section.wordCount} 字</span>
            {section.submittedByName && <span>提交人 {section.submittedByName}</span>}
            {section.submittedAt && <span>{section.submittedAt}</span>}
          </Space>
          {!section.contentExists && <Alert type="warning" showIcon message="本节尚未编写" />}
          <MdPreview value={markdown} sanitize={sanitizeTextbookHtml} noMermaid noKatex />
        </>
      )}
    </Drawer>
  );
}

// ==================================================================
// 分配/改派/批量分配 Modal（§10.3 分配交互）
// ==================================================================

function useCollaboratorOptions(role: 'writer' | 'reviewer') {
  const [options, setOptions] = useState<CollaboratorVO[]>([]);
  const [fetching, setFetching] = useState(false);
  const [keyword, setKeyword] = useState('');
  const debounceRef = useRef<number | null>(null);

  const search = (kw: string) => {
    setKeyword(kw);
    if (debounceRef.current != null) window.clearTimeout(debounceRef.current);
    debounceRef.current = window.setTimeout(() => {
      setFetching(true);
      pageCollaborators({ role, keyword: kw || undefined, page: 1, pageSize: 20 })
        .then((resp) => setOptions(resp.items))
        .catch(() => setOptions([]))
        .finally(() => setFetching(false));
    }, 300);
  };

  useEffect(() => {
    // 打开即预载第一页
    setFetching(true);
    pageCollaborators({ role, page: 1, pageSize: 20 })
      .then((resp) => setOptions(resp.items))
      .catch(() => setOptions([]))
      .finally(() => setFetching(false));
    return () => {
      if (debounceRef.current != null) window.clearTimeout(debounceRef.current);
    };
  }, [role]);

  return { options, fetching, keyword, search };
}

function AssignModal({
  projectId,
  chapters,
  onClose,
  onSaved,
}: {
  projectId: string;
  chapters: CollaborationChapter[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<{
    writerId?: string;
    reviewerId?: string;
    dueDate?: dayjs.Dayjs | null;
    instruction?: string;
  }>();
  const writerSel = useCollaboratorOptions('writer');
  const reviewerSel = useCollaboratorOptions('reviewer');

  const isBatch = chapters.length > 1;
  const hasExisting = chapters.some((c) => c.assignment?.active);
  const hasOngoing = chapters.some(
    (c) => c.assignment?.active && (c.assignment.submittedCount > 0 || c.assignment.approvedCount > 0),
  );
  const chapterIds = chapters.map((c) => c.chapterId);

  const buildItems = (): ChapterAssignmentItem[] => {
    const values = form.getFieldsValue(true);
    const dueDate = values.dueDate ? values.dueDate.format('YYYY-MM-DD') : undefined;
    return chapters.map((c) => ({
      chapterId: c.chapterId,
      writerId: values.writerId!,
      reviewerId: values.reviewerId!,
      dueDate,
      instruction: values.instruction?.trim() || undefined,
      assignmentVersion: c.assignment?.assignmentVersion ?? null,
    }));
  };

  const submit = async () => {
    // 确认页阶段 Form 已卸载：getFieldsValue(true) 读完整 store（无参版只返回已注册字段，卸载后为空）
    const values = form.getFieldsValue(true);
    if (!values.writerId || !values.reviewerId) {
      message.error('请先选择主笔与审定人');
      return;
    }
    if (values.writerId === values.reviewerId) {
      message.error('主笔与审定人不能是同一人');
      return;
    }
    setSubmitting(true);
    try {
      await saveAssignments(projectId, buildItems());
      message.success(isBatch ? `已为 ${chapters.length} 章保存分配` : '已保存分配');
      onSaved();
    } catch (e) {
      const conflict = extractAssignmentConflict(e);
      if (conflict) {
        message.error('分配已被他人更新，请关闭后重试');
      } else {
        message.error(apiErrorMessage(e, '保存分配失败'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const writerLabel = (u: CollaboratorVO) =>
    `${u.displayName}（${u.username}）${u.orgName ? ` / ${u.orgName}` : ''}`;

  return (
    <Modal
      open
      width={640}
      title={isBatch ? `批量分配 ${chapters.length} 章` : `分配「${chapters[0]?.title ?? ''}」`}
      confirmLoading={submitting}
      onCancel={onClose}
      onOk={() => {
        if (confirming) {
          void submit();
          return;
        }
        form.validateFields().then((values) => {
          if (values.writerId === values.reviewerId) {
            message.error('主笔与审定人不能是同一人');
            return;
          }
          setConfirming(true);
        });
      }}
      okText={confirming ? `确认提交（${chapters.length} 章）` : '下一步'}
      cancelText={confirming ? '返回修改' : '取消'}
      destroyOnHidden
    >
      {hasExisting && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message={hasOngoing ? '改派不改变已提交/已通过状态' : '将更新当前分配（改派）'}
          description={hasOngoing ? '已提交待审定、已通过的节不会因改派被重置，仍由原审定流程处理。' : undefined}
        />
      )}
      {confirming ? (
        <Descriptions
          bordered
          size="small"
          column={1}
          items={[
            { key: 'chapters', label: '影响章数', children: `${chapters.length} 章` },
            {
              key: 'names',
              label: '章节',
              children: chapters.map((c) => c.title).join('、'),
            },
            {
              key: 'writer',
              label: '主笔',
              children: writerLabel(writerSel.options.find((o) => o.id === form.getFieldValue('writerId')) ?? { displayName: '-', username: '-', orgName: null } as CollaboratorVO),
            },
            {
              key: 'reviewer',
              label: '审定人',
              children: writerLabel(reviewerSel.options.find((o) => o.id === form.getFieldValue('reviewerId')) ?? { displayName: '-', username: '-', orgName: null } as CollaboratorVO),
            },
            {
              key: 'due',
              label: '截止日期',
              children: form.getFieldValue('dueDate') ? form.getFieldValue('dueDate').format('YYYY-MM-DD') : '不限',
            },
            {
              key: 'instruction',
              label: '编写说明',
              children: form.getFieldValue('instruction') || '-',
            },
          ]}
        />
      ) : (
        <Form form={form} layout="vertical">
          <Form.Item
            name="writerId"
            label="主笔"
            rules={[{ required: true, message: '请选择主笔' }]}
            extra="候选人需为启用账号且具备「教材编写专家」角色权限"
          >
            <Select
              showSearch
              filterOption={false}
              placeholder="输入姓名/用户名搜索"
              loading={writerSel.fetching}
              onSearch={writerSel.search}
              notFoundContent={
                writerSel.fetching ? <Spin size="small" /> : '无合适人选：请先在用户管理中为该账号绑定教材编写专家角色'
              }
              options={writerSel.options.map((u) => ({ value: u.id, label: writerLabel(u) }))}
            />
          </Form.Item>
          <Form.Item
            name="reviewerId"
            label="审定人"
            rules={[{ required: true, message: '请选择审定人' }]}
            extra="候选人需为启用账号且具备「教材审定专家」角色权限；不能与主笔为同一人"
          >
            <Select
              showSearch
              filterOption={false}
              placeholder="输入姓名/用户名搜索"
              loading={reviewerSel.fetching}
              onSearch={reviewerSel.search}
              notFoundContent={
                reviewerSel.fetching ? <Spin size="small" /> : '无合适人选：请先在用户管理中为该账号绑定教材审定专家角色'
              }
              options={reviewerSel.options.map((u) => ({ value: u.id, label: writerLabel(u) }))}
            />
          </Form.Item>
          <Form.Item name="dueDate" label="截止日期（可不填，表示不限）">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="instruction" label="编写说明（可不填）">
            <Input.TextArea rows={3} maxLength={500} showCount placeholder="如：结合现场操作写清步骤" />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}

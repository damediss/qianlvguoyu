/**
 * 审定三栏工作台 —— 《二期编写协同实施设计》§10.6（TextbookReviewWorkbenchPage.tsx）
 * 隐藏路由 /textbook/review/:chapterId（载体菜单 textbook_my_review）。
 *
 * 中间只读渲染（审定人不能修改 Markdown）；主操作平铺：通过 / 打回 / 导出本节；已 approved 显示撤销通过。
 * 打回/撤销意见必填（1~1000 字）；通过后显示「下一待审定节」按钮。
 */
import { ArrowLeftOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Empty, Form, Input, Modal, Space, Spin, Tag, message } from 'antd';
import { MdPreview } from 'md-editor-rt';
import 'md-editor-rt/lib/style.css';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  exportSectionDocx,
  getChapterWorkspace,
  getSectionAssetContent,
  getSectionContent,
  pageSectionReviews,
  reviewSectionContent,
} from '../../api/textbookCollaboration';
import { useNavStore } from '../../stores/navStore';
import { parseContentDispositionFilename } from '../../utils/download';
import { apiErrorMessage, extractVersionConflict } from '../../utils/apiError';
import { resolveTextbookAssetUris, revokeObjectUrls, sanitizeTextbookHtml } from '../../utils/textbookMarkdown';
import { textbookLevelLabel } from '../../types/textbook';
import type { SectionContentVO, TextbookReviewRecordVO } from '../../types/textbookCollaboration';
import { ContentStatusTag, ProjectStatusTag, reviewActionText } from './TextbookTaskMeta';

export function TextbookReviewWorkbenchPage({ chapterId }: { chapterId: string }) {
  const queryClient = useQueryClient();
  const setSelectedKey = useNavStore((s) => s.setSelectedKey);
  const [sectionId, setSectionId] = useState<string | null>(null);
  const [previewMarkdown, setPreviewMarkdown] = useState('');
  const [reviewModal, setReviewModal] = useState<{ action: 'approve' | 'return' | 'reopen' } | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [reviewRecords, setReviewRecords] = useState<TextbookReviewRecordVO[]>([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [form] = Form.useForm<{ comment: string }>();
  const objectUrlsRef = useRef(new Map<string, string>());

  const workspaceQuery = useQuery({
    queryKey: ['textbook-workspace', chapterId],
    queryFn: () => getChapterWorkspace(chapterId),
  });
  const workspace = workspaceQuery.data;

  const sections = useMemo(
    () => [...(workspace?.sections || [])].sort((a, b) => a.sortNo - b.sortNo),
    [workspace],
  );

  // 默认选中第一个待审定节
  useEffect(() => {
    if (sections.length === 0) {
      setSectionId(null);
      return;
    }
    if (sectionId && sections.some((s) => s.sectionId === sectionId)) return;
    const firstSubmitted = sections.find((s) => s.contentStatus === 'submitted');
    setSectionId((firstSubmitted || sections[0]).sectionId);
  }, [sections, sectionId]);

  const contentQuery = useQuery({
    queryKey: ['textbook-section-content', sectionId],
    queryFn: () => getSectionContent(sectionId!),
    enabled: !!sectionId,
  });
  const section: SectionContentVO | undefined = contentQuery.data;

  // 历史意见（W6 首页；完整时间线可后续在详情中扩展分页）
  const loadReviews = async () => {
    if (!sectionId) return;
    setReviewsLoading(true);
    try {
      const resp = await pageSectionReviews(sectionId, { page: 1, pageSize: 20 });
      setReviewRecords(resp.items);
    } catch {
      // 时间线加载失败不阻塞审定
    } finally {
      setReviewsLoading(false);
    }
  };

  useEffect(() => {
    setReviewRecords([]);
    void loadReviews();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sectionId]);

  // 只读正文渲染：内部资产 URI → blob URL
  useEffect(() => {
    let cancelled = false;
    if (!section) return undefined;
    void resolveTextbookAssetUris(
      section.contentMd,
      (assetId) => getSectionAssetContent(section.sectionId, assetId),
      objectUrlsRef.current,
    ).then((resolved) => {
      if (!cancelled) setPreviewMarkdown(resolved);
    });
    return () => {
      cancelled = true;
    };
  }, [section]);

  useEffect(() => () => revokeObjectUrls(objectUrlsRef.current), []);

  const setSectionCache = (vo: SectionContentVO) => {
    queryClient.setQueryData(['textbook-section-content', vo.sectionId], vo);
  };

  const doReview = async (action: 'approve' | 'return' | 'reopen', comment: string | undefined) => {
    if (!section) return;
    setSubmitting(true);
    try {
      const vo = await reviewSectionContent(section.sectionId, {
        action,
        comment: comment?.trim() || undefined,
        baseVersion: section.version,
      });
      setSectionCache(vo);
      queryClient.invalidateQueries({ queryKey: ['textbook-workspace', chapterId] });
      void loadReviews();
      message.success(
        action === 'approve' ? '已通过' : action === 'return' ? '已打回，等待主笔修改后重新提交' : '已撤销通过',
      );
    } catch (e) {
      const conflict = extractVersionConflict(e);
      if (conflict) {
        message.error('版本冲突：内容已被更新，请刷新后重试');
        queryClient.invalidateQueries({ queryKey: ['textbook-section-content', section.sectionId] });
      } else {
        message.error(apiErrorMessage(e, '操作失败'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const openReviewModal = (action: 'approve' | 'return' | 'reopen') => {
    form.resetFields();
    setReviewModal({ action });
  };

  const submitReviewModal = async () => {
    if (!reviewModal) return;
    try {
      const values = await form.validateFields();
      await doReview(reviewModal.action, values.comment);
      setReviewModal(null);
    } catch {
      // 表单校验失败：错误由 Form.Item 展示；doReview 自行处理接口错误
    }
  };

  const handleExportSection = async () => {
    if (!section) return;
    try {
      const resp = await exportSectionDocx(section.sectionId);
      const blob = resp.data as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = parseContentDispositionFilename(resp) ?? `教材节_${section.sectionTitle}.docx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch {
      message.error('导出失败，请稍后重试');
    }
  };

  const nextSubmitted = sections.find((s) => s.contentStatus === 'submitted' && s.sectionId !== sectionId);

  if (workspaceQuery.isLoading) {
    return (
      <Card>
        <Spin />
      </Card>
    );
  }
  if (workspaceQuery.isError || !workspace) {
    return (
      <Card>
        <Empty description="章节不存在或无权访问">
          <Button onClick={() => setSelectedKey('textbook_my_review')}>返回我的审定任务</Button>
        </Empty>
      </Card>
    );
  }

  const assignment = workspace.assignment;
  const currentMeta = sections.find((s) => s.sectionId === sectionId) || null;
  const canOperate =
    !!section &&
    section.canReview &&
    (section.contentStatus === 'submitted' || section.contentStatus === 'approved') &&
    workspace.projectStatus === 'writing';

  return (
    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
      {/* 左栏：节状态 */}
      <Card size="small" style={{ width: 260, flexShrink: 0, alignSelf: 'flex-start' }} title={workspace.chapterTitle}>
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          <span>
            待审定 {sections.filter((s) => s.contentStatus === 'submitted').length} 节 · 已通过{' '}
            {sections.filter((s) => s.contentStatus === 'approved').length} 节
          </span>
          {assignment && (
            <span style={{ color: '#666', fontSize: 12 }}>
              主笔 {assignment.writerName}
              {assignment.dueDate ? ` · 截止 ${assignment.dueDate}` : ''}
            </span>
          )}
        </Space>
        <div style={{ marginTop: 8 }}>
          {sections.map((s) => (
            <div
              key={s.sectionId}
              role="button"
              tabIndex={0}
              aria-label={`节 ${s.title}`}
              onClick={() => setSectionId(s.sectionId)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') setSectionId(s.sectionId);
              }}
              style={{
                padding: '6px 8px',
                borderRadius: 4,
                cursor: 'pointer',
                background: s.sectionId === sectionId ? '#f9f0ff' : undefined,
                marginBottom: 2,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {s.sourceNo ? `${s.sourceNo} ` : ''}{s.title}
              </span>
              <ContentStatusTag status={s.contentStatus} />
            </div>
          ))}
        </div>
      </Card>

      {/* 中栏：只读正文 + 主操作平铺 */}
      <div style={{ flex: 1, minWidth: 420 }}>
        <Card
          size="small"
          title={
            <Space wrap>
              <Button
                size="small"
                type="text"
                icon={<ArrowLeftOutlined />}
                onClick={() => setSelectedKey('textbook_my_review')}
              >
                返回任务列表
              </Button>
              <span>
                {workspace.workTypeTitle}
                {workspace.direction ? `（${workspace.direction} · ${textbookLevelLabel(workspace.levelCode)}）` : ''}
              </span>
              <ProjectStatusTag status={workspace.projectStatus} />
            </Space>
          }
        >
          {contentQuery.isLoading && <Spin />}
          {contentQuery.isError && <Alert type="error" showIcon message="正文加载失败" />}
          {section && currentMeta && (
            <div>
              <h3 style={{ marginTop: 0 }}>
                {currentMeta.sourceNo ? `${currentMeta.sourceNo} ` : ''}{section.sectionTitle}
              </h3>
              <Space wrap style={{ marginBottom: 8 }}>
                <ContentStatusTag status={section.contentStatus} />
                {section.submitRevision != null && <Tag>第 {section.submitRevision} 次提交</Tag>}
                {section.wordCount > 0 && <span>{section.wordCount} 字</span>}
                {section.submittedByName && <span>主笔提交人 {section.submittedByName}</span>}
                {section.submittedAt && <span>{section.submittedAt}</span>}
              </Space>

              {/* 主操作平铺（§10.6） */}
              <Space wrap style={{ marginBottom: 12 }}>
                {canOperate && section.contentStatus === 'submitted' && (
                  <>
                    <Button type="primary" loading={submitting} onClick={() => openReviewModal('approve')}>
                      通过
                    </Button>
                    <Button danger loading={submitting} onClick={() => openReviewModal('return')}>
                      打回
                    </Button>
                  </>
                )}
                {canOperate && section.contentStatus === 'approved' && (
                  <Button danger loading={submitting} onClick={() => openReviewModal('reopen')}>
                    撤销通过
                  </Button>
                )}
                <Button onClick={() => void handleExportSection()}>导出本节</Button>
                {nextSubmitted && (
                  <Button onClick={() => setSectionId(nextSubmitted.sectionId)}>
                    下一待审定节：{nextSubmitted.title}
                  </Button>
                )}
              </Space>

              {!canOperate && section.contentStatus !== 'submitted' && section.contentStatus !== 'approved' && (
                <Alert
                  type="info"
                  showIcon
                  style={{ marginBottom: 8 }}
                  message={section.readOnlyReason || '当前节不在待审定/已通过状态，仅供查看'}
                />
              )}
              {workspace.projectStatus === 'done' && (
                <Alert type="warning" showIcon style={{ marginBottom: 8 }} message="工程已完成编写；如需撤销审定，请先由教研重新打开工程" />
              )}

              {!section.contentExists && <Alert type="warning" showIcon message="本节尚未编写" />}
              <div style={{ border: '1px solid #f0f0f0', borderRadius: 4, padding: 12 }}>
                <MdPreview value={previewMarkdown} sanitize={sanitizeTextbookHtml} noMermaid noKatex />
              </div>
            </div>
          )}
        </Card>
      </div>

      {/* 右栏：编写要求 / 当前提交版本 / 历史意见 */}
      <div style={{ width: 320, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Card size="small" title="编写要求">
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <span>本节目标字数：{currentMeta?.targetWords ?? '-'}</span>
            {assignment?.instruction && <span>分配说明：{assignment.instruction}</span>}
            {currentMeta && currentMeta.points.length > 0 && (
              <div style={{ marginTop: 4 }}>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>标准理论点/技能点</div>
                {currentMeta.points.map((p) => (
                  <div key={p.pointId} style={{ fontSize: 12, color: '#555' }}>
                    <Tag color={p.dimension === 'skill' ? 'geekblue' : 'blue'} style={{ marginInlineEnd: 4 }}>
                      {p.dimension === 'skill' ? '技能' : p.dimension === 'theory' ? '理论' : '点'}
                    </Tag>
                    {p.titleRaw}
                  </div>
                ))}
              </div>
            )}
          </Space>
        </Card>
        <Card size="small" title="当前提交版本">
          <Space direction="vertical" size={2}>
            <span>{section?.submitRevision != null ? `第 ${section.submitRevision} 次提交` : '未提交'}</span>
            <span>主笔：{assignment?.writerName ?? '-'}</span>
            <span>提交时间：{section?.submittedAt ?? '-'}</span>
          </Space>
        </Card>
        <Card size="small" title="历史意见" extra={reviewsLoading ? <Spin size="small" /> : undefined}>
          {reviewRecords.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无记录" />}
          {reviewRecords.map((r, i) => (
            <div key={r.id ?? i} style={{ marginBottom: 8, fontSize: 12 }}>
              <div>
                <b>{reviewActionText(r.action)}</b>（第 {r.submitRevision} 次提交） · {r.actorName}
              </div>
              <div style={{ color: '#555' }}>{r.comment || '无意见'}</div>
              <div style={{ color: '#999' }}>{r.createdAt}</div>
            </div>
          ))}
        </Card>
      </div>

      {/* 通过（意见选填）/ 打回、撤销通过（意见必填 1~1000 字） */}
      <Modal
        open={!!reviewModal}
        title={reviewModal?.action === 'approve' ? '通过本节' : reviewModal?.action === 'return' ? '打回本节' : '撤销通过'}
        confirmLoading={submitting}
        onCancel={() => setReviewModal(null)}
        onOk={() => void submitReviewModal()}
        okText={reviewModal?.action === 'approve' ? '确认通过' : reviewModal?.action === 'return' ? '确认打回' : '确认撤销'}
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={form} layout="vertical" style={{ marginTop: 12 }}>
          <Form.Item
            name="comment"
            label="审定意见"
            rules={
              reviewModal?.action === 'approve'
                ? [{ max: 1000, message: '意见最多 1000 字' }]
                : [
                    { required: true, whitespace: true, message: reviewModal?.action === 'return' ? '打回必须填写意见' : '撤销通过必须填写原因' },
                    { min: 1, max: 1000, message: '意见长度 1~1000 字' },
                  ]
            }
          >
            <Input.TextArea rows={4} maxLength={1000} showCount placeholder="请输入审定意见" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

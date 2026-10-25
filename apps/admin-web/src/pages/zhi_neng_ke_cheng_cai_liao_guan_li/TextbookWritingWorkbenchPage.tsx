/**
 * 主笔三栏编写工作台 —— 《二期编写协同实施设计》§10.5（TextbookWritingWorkbenchPage.tsx）
 * 隐藏路由 /textbook/write/:chapterId（载体菜单 textbook_my_writing）。
 *
 * 桌面三栏：左 260px 节导航（按 sortNo，不按状态重排）、中间自适应编辑器、右 320px 编写要求与检查；
 * 窄屏右栏移到编辑器下方。主操作平铺（保存草稿/提交审定/历史版本/导出本节/上传图片）。
 */
import { ArrowLeftOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Empty, Radio, Space, Spin, Tag, message } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  exportSectionDocx,
  getChapterWorkspace,
  getSectionContent,
} from '../../api/textbookCollaboration';
import { useNavStore } from '../../stores/navStore';
import { parseContentDispositionFilename } from '../../utils/download';
import { textbookLevelLabel } from '../../types/textbook';
import type { SectionContentVO, WorkspaceSection } from '../../types/textbookCollaboration';
import {
  ContentStatusTag,
  ProjectStatusTag,
  reviewActionText,
} from './TextbookTaskMeta';
import { TextbookSectionEditor, type TextbookSectionEditorHandle } from './TextbookSectionEditor';
import { TextbookVersionDrawer } from './TextbookVersionDrawer';

type SectionFilter = 'all' | 'unfinished' | 'submitted' | 'approved';

const FILTER_OPTIONS: { value: SectionFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'unfinished', label: '未完成' },
  { value: 'submitted', label: '待审定' },
  { value: 'approved', label: '已通过' },
];

function filterSections(sections: WorkspaceSection[], filter: SectionFilter): WorkspaceSection[] {
  switch (filter) {
    case 'unfinished':
      return sections.filter((s) => s.contentStatus !== 'approved');
    case 'submitted':
      return sections.filter((s) => s.contentStatus === 'submitted');
    case 'approved':
      return sections.filter((s) => s.contentStatus === 'approved');
    default:
      return sections;
  }
}

export function TextbookWritingWorkbenchPage({ chapterId }: { chapterId: string }) {
  const queryClient = useQueryClient();
  const setSelectedKey = useNavStore((s) => s.setSelectedKey);
  const [sectionId, setSectionId] = useState<string | null>(null);
  const [filter, setFilter] = useState<SectionFilter>('all');
  const [dirty, setDirty] = useState(false);
  const [versionsOpen, setVersionsOpen] = useState(false);
  const editorRef = useRef<TextbookSectionEditorHandle | null>(null);

  const workspaceQuery = useQuery({
    queryKey: ['textbook-workspace', chapterId],
    queryFn: () => getChapterWorkspace(chapterId),
  });
  const workspace = workspaceQuery.data;

  const sections = useMemo(
    () => [...(workspace?.sections || [])].sort((a, b) => a.sortNo - b.sortNo),
    [workspace],
  );

  // 默认选中第一个未通过节，其次第一节；选中节在列表内保持稳定
  useEffect(() => {
    if (sections.length === 0) {
      setSectionId(null);
      return;
    }
    if (sectionId && sections.some((s) => s.sectionId === sectionId)) return;
    const firstUnfinished = sections.find((s) => s.contentStatus !== 'approved');
    setSectionId((firstUnfinished || sections[0]).sectionId);
  }, [sections, sectionId]);

  const contentQuery = useQuery({
    queryKey: ['textbook-section-content', sectionId],
    queryFn: () => getSectionContent(sectionId!),
    enabled: !!sectionId,
  });
  const section: SectionContentVO | undefined = contentQuery.data;

  const setSectionCache = (vo: SectionContentVO) => {
    queryClient.setQueryData(['textbook-section-content', vo.sectionId], vo);
  };

  const handleSectionSwitch = async (nextId: string) => {
    if (nextId === sectionId) return;
    // 离开保护：dirty 先尝试保存；失败弹「继续留下 / 放弃未保存内容离开」（§10.5）
    const mayLeave = editorRef.current ? await editorRef.current.confirmLeave() : true;
    if (!mayLeave) return;
    setDirty(false);
    setSectionId(nextId);
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
    } catch (e) {
      message.error('导出失败，请稍后重试');
    }
  };

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
          <Button onClick={() => setSelectedKey('textbook_my_writing')}>返回我的编写任务</Button>
        </Empty>
      </Card>
    );
  }

  const assignment = workspace.assignment;
  const approvedCount = sections.filter((s) => s.contentStatus === 'approved').length;
  const visibleSections = filterSections(sections, filter);
  const currentMeta = sections.find((s) => s.sectionId === sectionId) || null;

  return (
    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
      {/* 左栏：节导航 */}
      <Card size="small" style={{ width: 260, flexShrink: 0, alignSelf: 'flex-start' }} title={
        <Space direction="vertical" size={2}>
          <span>
            {workspace.chapterSourceNo ? `${workspace.chapterSourceNo} ` : ''}{workspace.chapterTitle}
          </span>
        </Space>
      }>
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          <span>
            总进度：{approvedCount}/{sections.length} 节已通过
          </span>
          {assignment && (
            <span style={{ color: '#666', fontSize: 12 }}>
              审定人 {assignment.reviewerName}
              {assignment.dueDate ? ` · 截止 ${assignment.dueDate}` : ''}
            </span>
          )}
          <Radio.Group
            size="small"
            options={FILTER_OPTIONS}
            value={filter}
            onChange={(e) => setFilter(e.target.value as SectionFilter)}
            optionType="button"
            style={{ marginTop: 4 }}
          />
        </Space>
        <div style={{ marginTop: 8 }}>
          {visibleSections.map((s) => (
            <div
              key={s.sectionId}
              role="button"
              tabIndex={0}
              aria-label={`节 ${s.title}`}
              onClick={() => void handleSectionSwitch(s.sectionId)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') void handleSectionSwitch(s.sectionId);
              }}
              style={{
                padding: '6px 8px',
                borderRadius: 4,
                cursor: 'pointer',
                background: s.sectionId === sectionId ? '#e6f4ff' : undefined,
                marginBottom: 2,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {s.sourceNo ? `${s.sourceNo} ` : ''}{s.title}
              </span>
              {s.latestReviewRecord?.action === 'return' && (
                <Tag color="red" style={{ marginInlineEnd: 0 }}>
                  打回
                </Tag>
              )}
              <ContentStatusTag status={s.contentStatus} />
              <span style={{ color: '#999', fontSize: 12 }}>
                {s.wordCount}/{s.targetWords ?? '-'}
              </span>
            </div>
          ))}
          {visibleSections.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该筛选下无节" />}
        </div>
      </Card>

      {/* 中栏：编辑器 */}
      <div style={{ flex: 1, minWidth: 420 }}>
        <Card size="small" title={
          <Space wrap>
            <Button
              size="small"
              type="text"
              icon={<ArrowLeftOutlined />}
              onClick={() => setSelectedKey('textbook_my_writing')}
            >
              返回任务列表
            </Button>
            <span>
              {workspace.workTypeTitle}
              {workspace.direction ? `（${workspace.direction} · ${textbookLevelLabel(workspace.levelCode)}）` : ''}
            </span>
            <ProjectStatusTag status={workspace.projectStatus} />
          </Space>
        }>
          {contentQuery.isLoading && <Spin />}
          {contentQuery.isError && <Alert type="error" showIcon message="正文加载失败，请刷新重试" />}
          {section && currentMeta && (
            <div>
              <h3 style={{ marginTop: 0 }}>
                {currentMeta.sourceNo ? `${currentMeta.sourceNo} ` : ''}{section.sectionTitle}
              </h3>
              {section.contentStatus === 'returned' && section.latestReviewRecord && (
                <Alert
                  type="error"
                  showIcon
                  style={{ marginBottom: 8 }}
                  message={`已打回（${reviewActionText(section.latestReviewRecord.action)}，第 ${section.latestReviewRecord.submitRevision} 次提交）：${section.latestReviewRecord.comment || '无意见'}`}
                  description={`审定人 ${section.latestReviewRecord.actorName} · ${section.latestReviewRecord.createdAt}`}
                />
              )}
              <TextbookSectionEditor
                key={section.sectionId}
                ref={editorRef}
                section={section}
                onSaved={setSectionCache}
                onSubmitted={(vo) => {
                  setSectionCache(vo);
                  queryClient.invalidateQueries({ queryKey: ['textbook-workspace', chapterId] });
                }}
                onExternalAdopted={(data) => {
                  setSectionCache({
                    ...section,
                    contentMd: data.currentContentMd,
                    version: data.currentVersion,
                  });
                }}
                onDirtyChange={setDirty}
                onOpenVersions={() => setVersionsOpen(true)}
                onExportSection={() => void handleExportSection()}
              />
            </div>
          )}
          {!section && !contentQuery.isLoading && !contentQuery.isError && (
            <Empty description="请选择左侧节开始编写" />
          )}
        </Card>
      </div>

      {/* 右栏：编写要求 / 检查结果 / 最近审定意见 */}
      <div style={{ width: 320, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Card size="small" title="编写要求">
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <span>章进度：{approvedCount}/{sections.length} 节已通过</span>
            <span>本节目标字数：{currentMeta?.targetWords ?? '-'}</span>
            {assignment?.instruction && <span>分配说明：{assignment.instruction}</span>}
            {assignment && <span>审定人：{assignment.reviewerName}</span>}
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
        {section?.checkReport && (
          <Card size="small" title="检查结果" extra={<span style={{ color: '#999', fontSize: 12 }}>仅供检查，不代替专家判断</span>}>
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              {section.checkReport.minSuggestedWords != null && section.checkReport.maxSuggestedWords != null && (
                <span>
                  建议字数区间：{section.checkReport.minSuggestedWords}~{section.checkReport.maxSuggestedWords}
                  （当前 {section.checkReport.wordCount} 字）
                </span>
              )}
              {section.checkReport.uncoveredPoints.length > 0 ? (
                <span>
                  可能未覆盖（{section.checkReport.uncoveredPoints.length}）：
                  {section.checkReport.uncoveredPoints.map((p) => p.title).join('、')}
                </span>
              ) : (
                <span>覆盖检查：未见遗漏</span>
              )}
              <span>待补充占位：{section.checkReport.placeholderCount} 处</span>
            </Space>
          </Card>
        )}
        {section?.latestReviewRecord && (
          <Card size="small" title="最近审定意见">
            <Space direction="vertical" size={2}>
              <span>
                {reviewActionText(section.latestReviewRecord.action)}（第 {section.latestReviewRecord.submitRevision} 次提交）
              </span>
              <span style={{ color: '#555' }}>{section.latestReviewRecord.comment || '无意见'}</span>
              <span style={{ color: '#999', fontSize: 12 }}>
                {section.latestReviewRecord.actorName} · {section.latestReviewRecord.createdAt}
              </span>
            </Space>
          </Card>
        )}
      </div>

      {section && (
        <TextbookVersionDrawer
          open={versionsOpen}
          section={section}
          dirty={dirty}
          onClose={() => setVersionsOpen(false)}
          onRestored={(vo) => {
            setSectionCache(vo);
            queryClient.invalidateQueries({ queryKey: ['textbook-workspace', chapterId] });
          }}
        />
      )}
    </div>
  );
}

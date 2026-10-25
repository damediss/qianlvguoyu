/**
 * 教材节历史版本抽屉 —— 《二期编写协同实施设计》§10.7（TextbookVersionDrawer.tsx）
 *
 * - 列表：W7 分页（snapshotNo 倒序），抽屉底部「加载更多」，不一次拉取全部历史
 * - 查看：选中快照与当前内容并排渲染（二期不做字符级 diff）
 * - 恢复：仅当前主笔且 draft/returned 可操作；当前 dirty 时提示先保存；后端先存 before_restore 再恢复
 */
import { Button, Drawer, Empty, List, Popconfirm, Space, Tag, Tooltip, message } from 'antd';
import { MdPreview } from 'md-editor-rt';
import 'md-editor-rt/lib/style.css';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getSectionAssetContent,
  getSectionVersion,
  pageSectionVersions,
  restoreSectionVersion,
} from '../../api/textbookCollaboration';
import { apiErrorMessage, extractVersionConflict } from '../../utils/apiError';
import {
  resolveTextbookAssetUris,
  revokeObjectUrls,
  sanitizeTextbookHtml,
} from '../../utils/textbookMarkdown';
import type {
  SectionContentVO,
  SectionVersionDetailVO,
  SectionVersionVO,
} from '../../types/textbookCollaboration';

const PAGE_SIZE = 20;

function snapshotLabel(v: SectionVersionVO): string {
  return v.snapshotType === 'submit' ? `提交第 ${v.submitRevision ?? '?'} 版` : '恢复前快照';
}

export function TextbookVersionDrawer({
  open,
  section,
  dirty,
  onClose,
  onRestored,
}: {
  open: boolean;
  /** 当前节最新 W2（含 version/contentStatus/contentMd） */
  section: SectionContentVO;
  /** 编辑器当前 dirty：恢复前提示先保存 */
  dirty: boolean;
  onClose: () => void;
  onRestored: (vo: SectionContentVO) => void;
}) {
  const [items, setItems] = useState<SectionVersionVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<SectionVersionDetailVO | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [snapshotMarkdown, setSnapshotMarkdown] = useState('');
  const [currentMarkdown, setCurrentMarkdown] = useState('');
  const objectUrlsRef = useRef(new Map<string, string>());

  const canRestore =
    section.canWrite && (section.contentStatus === 'draft' || section.contentStatus === 'returned');
  const hasMore = items.length < total;

  const load = useCallback(
    async (nextPage: number, replace: boolean) => {
      setLoading(true);
      try {
        const resp = await pageSectionVersions(section.sectionId, { page: nextPage, pageSize: PAGE_SIZE });
        setItems((prev) => (replace ? resp.items : [...prev, ...resp.items]));
        setTotal(resp.total);
        setPage(nextPage);
      } catch (e) {
        message.error(apiErrorMessage(e, '加载历史版本失败'));
      } finally {
        setLoading(false);
      }
    },
    [section.sectionId],
  );

  useEffect(() => {
    if (!open) {
      setItems([]);
      setDetail(null);
      setTotal(0);
      revokeObjectUrls(objectUrlsRef.current);
      return undefined;
    }
    void load(1, true);
    return undefined;
  }, [open, load]);

  // 并排查看：快照与当前内容都做内部资产 URI → blob URL（§8.2），统一 revoke
  useEffect(() => {
    let cancelled = false;
    if (!detail) return undefined;
    void resolveTextbookAssetUris(
      detail.contentMd,
      (assetId) => getSectionAssetContent(section.sectionId, assetId),
      objectUrlsRef.current,
    ).then((resolved) => {
      if (!cancelled) setSnapshotMarkdown(resolved);
    });
    void resolveTextbookAssetUris(
      section.contentMd,
      (assetId) => getSectionAssetContent(section.sectionId, assetId),
      objectUrlsRef.current,
    ).then((resolved) => {
      if (!cancelled) setCurrentMarkdown(resolved);
    });
    return () => {
      cancelled = true;
    };
  }, [detail, section.contentMd, section.sectionId]);

  const openDetail = async (v: SectionVersionVO) => {
    setDetailLoading(true);
    setDetail(null);
    try {
      const d = await getSectionVersion(v.id);
      setDetail(d);
    } catch (e) {
      message.error(apiErrorMessage(e, '加载快照内容失败'));
    } finally {
      setDetailLoading(false);
    }
  };

  const handleRestore = async (v: SectionVersionVO) => {
    setRestoring(true);
    try {
      const vo = await restoreSectionVersion(section.sectionId, {
        snapshotId: v.id,
        baseVersion: section.version,
      });
      message.success('已恢复该版本（保持当前状态不变，恢复前内容已自动存为快照）');
      onRestored(vo);
      onClose();
    } catch (e) {
      const conflict = extractVersionConflict(e);
      if (conflict) {
        message.error('版本冲突：内容已被他人更新，请刷新后重试');
      } else {
        message.error(apiErrorMessage(e, '恢复失败'));
      }
    } finally {
      setRestoring(false);
    }
  };

  return (
    <Drawer
      title={`历史版本 —— ${section.sectionTitle}`}
      open={open}
      width="min(1080px, 94vw)"
      onClose={onClose}
      destroyOnClose
    >
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <div style={{ width: 320, flexShrink: 0 }}>
          <List
            size="small"
            loading={loading && items.length === 0}
            dataSource={items}
            rowKey="id"
            renderItem={(v) => (
              <List.Item
                style={{ cursor: 'pointer' }}
                onClick={() => void openDetail(v)}
                actions={[
                  canRestore ? (
                    <Popconfirm
                      title={`恢复为「${snapshotLabel(v)}」？`}
                      description="当前内容会先自动保存为「恢复前快照」，本节状态保持不变。"
                      okText="恢复"
                      cancelText="取消"
                      onConfirm={(e) => {
                        e?.stopPropagation();
                        void handleRestore(v);
                      }}
                      onCancel={(e) => e?.stopPropagation()}
                      disabled={dirty || restoring}
                    >
                      <Button
                        size="small"
                        disabled={dirty || restoring}
                        loading={restoring}
                        onClick={(e) => e.stopPropagation()}
                      >
                        恢复
                      </Button>
                    </Popconfirm>
                  ) : null,
                ].filter(Boolean)}
              >
                <List.Item.Meta
                  title={
                    <Space size={6}>
                      <span>{snapshotLabel(v)}</span>
                      {detail?.id === v.id && <Tag color="blue">查看中</Tag>}
                    </Space>
                  }
                  description={`${v.createdByName} · ${v.createdAt} · ${v.wordCount} 字`}
                />
              </List.Item>
            )}
          />
          {items.length === 0 && !loading && <Empty description="暂无历史版本（提交审定后自动生成快照）" />}
          {hasMore && (
            <div style={{ textAlign: 'center', padding: 12 }}>
              <Tooltip title={dirty ? '当前有未保存修改，请先保存再恢复' : undefined}>
                <Button loading={loading} onClick={() => void load(page + 1, false)}>
                  加载更多（{items.length}/{total}）
                </Button>
              </Tooltip>
            </div>
          )}
        </div>
        <div style={{ flex: 1, minWidth: 320 }}>
          {detailLoading && <p>加载快照内容…</p>}
          {!detail && !detailLoading && <Empty description="点击左侧版本查看快照与当前内容并排对比" />}
          {detail && (
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <div style={{ flex: 1, minWidth: 300, borderRight: '1px solid #f0f0f0', paddingRight: 8 }}>
                <h4>
                  {snapshotLabel(detail)}（{detail.createdAt}，{detail.wordCount} 字）
                </h4>
                <div style={{ maxHeight: '60vh', overflow: 'auto' }}>
                  <MdPreview value={snapshotMarkdown} sanitize={sanitizeTextbookHtml} noMermaid noKatex />
                </div>
              </div>
              <div style={{ flex: 1, minWidth: 300 }}>
                <h4>当前内容（v{section.version}）</h4>
                <div style={{ maxHeight: '60vh', overflow: 'auto' }}>
                  <MdPreview value={currentMarkdown} sanitize={sanitizeTextbookHtml} noMermaid noKatex />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </Drawer>
  );
}

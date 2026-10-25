/**
 * 教材节 Markdown 编辑器 —— 《二期编写协同实施设计》§10.5（TextbookSectionEditor.tsx）
 *
 * 职责：编辑/预览切换、图片上传与内部 URI 替换、保存状态、自动保存引擎、本地备份、409 冲突处理、提交流程。
 * 自动保存规则（§10.5）：
 * - 停止输入 3 秒触发一次；连续输入时最长 30 秒兜底一次；Ctrl+S 手动保存
 * - 自动保存串行：上一次未返回不发起下一次；失败指数退避最多 2 次后暂停，提示手动重试
 * - 本地备份键含 username+sectionId+baseVersion；保存成功清除；加载时仅在服务端版本匹配时提示恢复
 * - 409 冲突弹窗展示服务端版本/更新人/时间，提供「加载服务端最新」「下载我的草稿」，不自动覆盖
 */
import { PictureOutlined } from '@ant-design/icons';
import { Alert, Button, Modal, Popconfirm, Space, message } from 'antd';
import { MdEditor, MdPreview } from 'md-editor-rt';
import 'md-editor-rt/lib/style.css';
import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import type { ExposeParam, ToolbarNames } from 'md-editor-rt';
import { useAuthStore } from '../../stores/authStore';
import { useNavigationBlocker } from '../../hooks/useUnsavedChangesBlocker';
import {
  saveSectionContent,
  submitSectionContent,
  uploadSectionAsset,
  getSectionAssetContent,
} from '../../api/textbookCollaboration';
import { apiErrorMessage, extractVersionConflict } from '../../utils/apiError';
import {
  clearTextbookBackup,
  loadTextbookBackup,
  resolveTextbookAssetUris,
  revokeObjectUrls,
  saveTextbookBackup,
  sanitizeTextbookHtml,
  textbookBackupKey,
} from '../../utils/textbookMarkdown';
import type { SectionContentVO, VersionConflictData } from '../../types/textbookCollaboration';
import { ContentStatusTag } from './TextbookTaskMeta';

/** 自动保存参数（§10.5：3 秒防抖 / 30 秒兜底 / 退避 2 次后暂停） */
const AUTOSAVE_IDLE_MS = 3000;
const AUTOSAVE_MAX_WAIT_MS = 30000;
const AUTOSAVE_MAX_RETRIES = 2;

/** 工具栏不出现外链图片入口（image 工具含「添加链接」页签）；图片仅经 A1 上传 */
const EDITOR_TOOLBARS_EXCLUDE: ToolbarNames[] = [
  'image',
  'mermaid',
  'katex',
  'github',
  'prettier',
  'save',
  'htmlPreview',
];

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const IMAGE_MAX_SIZE = 10 * 1024 * 1024;

export type SectionSaveResult = 'saved' | 'skipped' | 'queued' | 'conflict' | 'error' | 'paused' | 'blocked';

export interface TextbookSectionEditorHandle {
  /** dirty 时先尝试保存；失败弹「继续留下 / 放弃未保存内容离开」；返回 true 才允许离开 */
  confirmLeave: () => Promise<boolean>;
  /** 手动保存（保存草稿按钮同款逻辑） */
  saveNow: () => Promise<SectionSaveResult>;
}

interface TextbookSectionEditorProps {
  section: SectionContentVO;
  onSaved: (vo: SectionContentVO) => void;
  onSubmitted: (vo: SectionContentVO) => void;
  /** 409 后选择「加载服务端最新」时通知父组件同步 W2 缓存 */
  onExternalAdopted: (data: VersionConflictData) => void;
  onDirtyChange: (dirty: boolean) => void;
  onOpenVersions: () => void;
  onExportSection: () => void;
}

type SaveStatus =
  | { kind: 'clean' }
  | { kind: 'dirty' }
  | { kind: 'saving' }
  | { kind: 'saved'; at: Date }
  | { kind: 'retrying'; attempt: number }
  | { kind: 'failed'; message: string };

function formatTime(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

/** 提交前检查警告（§7.3：检查有警告仍可提交，由前端二次确认，不拦截专家判断） */
export function collectSubmitWarnings(vo: SectionContentVO): string[] {
  const warnings: string[] = [];
  const report = vo.checkReport;
  if (report) {
    if (report.withinTargetRange === false && report.minSuggestedWords != null && report.maxSuggestedWords != null) {
      warnings.push(`字数偏离建议区间（建议 ${report.minSuggestedWords}~${report.maxSuggestedWords} 字，当前 ${report.wordCount} 字）`);
    }
    if (report.uncoveredPoints?.length > 0) {
      warnings.push(`疑似未覆盖 ${report.uncoveredPoints.length} 个标准点：${report.uncoveredPoints.map((p) => p.title).join('、')}`);
    }
    if (report.placeholderCount > 0) {
      warnings.push(`存在 ${report.placeholderCount} 处「待专家补充」占位`);
    }
  }
  return warnings;
}

export const TextbookSectionEditor = forwardRef<TextbookSectionEditorHandle, TextbookSectionEditorProps>(
  function TextbookSectionEditor(
    { section, onSaved, onSubmitted, onExternalAdopted, onDirtyChange, onOpenVersions, onExportSection },
    ref,
  ) {
    const username = useAuthStore((s) => s.currentUser?.username) || 'anonymous';
    const { registerGuard } = useNavigationBlocker();

    // ---- 本地状态（以 prop 初始化一次；prop 版本前进且本地干净时同步，§10.5 恢复/审定后刷新） ----
    const [content, setContent] = useState(section.contentMd);
    const [baseVersion, setBaseVersion] = useState(section.version);
    const [saveStatus, setSaveStatus] = useState<SaveStatus>({ kind: 'clean' });
    const [conflict, setConflict] = useState<VersionConflictData | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [uploadingImage, setUploadingImage] = useState(false);
    const [readOnlyMarkdown, setReadOnlyMarkdown] = useState(section.contentMd);

    const contentRef = useRef(content);
    const savedContentRef = useRef(section.contentMd);
    const baseVersionRef = useRef(section.version);
    const savingRef = useRef(false);
    const pendingAgainRef = useRef(false);
    const pausedRef = useRef(false);
    const failCountRef = useRef(0);
    const idleTimerRef = useRef<number | null>(null);
    const maxWaitTimerRef = useRef<number | null>(null);
    const backoffTimerRef = useRef<number | null>(null);
    const lastSyncedVersionRef = useRef(section.version);
    const objectUrlsRef = useRef(new Map<string, string>());
    const editorRef = useRef<ExposeParam | null>(null);
    const fileInputRef = useRef<HTMLInputElement | null>(null);

    const canEdit =
      section.canWrite && (section.contentStatus == null || section.contentStatus === 'draft' || section.contentStatus === 'returned');
    const canEditRef = useRef(canEdit);
    canEditRef.current = canEdit;

    const dirty = content !== savedContentRef.current;

    useEffect(() => {
      onDirtyChange(dirty);
    }, [dirty, onDirtyChange]);

    // ---- prop（W2 缓存）前进同步：恢复历史/审定打回后父组件 setQueryData ----
    useEffect(() => {
      if (section.version !== lastSyncedVersionRef.current) {
        lastSyncedVersionRef.current = section.version;
        if (contentRef.current === savedContentRef.current) {
          // 本地干净：直接采纳服务端内容（恢复版本、打回后编辑前刷新等）
          contentRef.current = section.contentMd;
          savedContentRef.current = section.contentMd;
          baseVersionRef.current = section.version;
          setContent(section.contentMd);
          setBaseVersion(section.version);
          setSaveStatus({ kind: 'clean' });
          pausedRef.current = false;
          failCountRef.current = 0;
          setConflict(null);
        }
        // 本地 dirty：保留本地内容，下次保存自然 409 走冲突弹窗
      }
    }, [section]);

    // ---- 只读渲染：内部资产 URI → blob URL（§8.2） ----
    useEffect(() => {
      let cancelled = false;
      if (canEdit) return undefined;
      void resolveTextbookAssetUris(
        content,
        (assetId) => getSectionAssetContent(section.sectionId, assetId),
        objectUrlsRef.current,
      ).then((resolved) => {
        if (!cancelled) setReadOnlyMarkdown(resolved);
      });
      return () => {
        cancelled = true;
      };
    }, [canEdit, content, section.sectionId]);

    useEffect(() => () => revokeObjectUrls(objectUrlsRef.current), []);

    // ---- 本地备份 ----
    const backupKey = textbookBackupKey(username, section.sectionId, baseVersion);

    useEffect(() => {
      // 加载时仅在服务端版本仍匹配（即本次 baseVersion）时提示恢复本地备份
      const backup = loadTextbookBackup(backupKey);
      if (backup != null && backup !== section.contentMd) {
        Modal.confirm({
          title: '检测到本地未保存的草稿',
          content: `上次编辑「${section.sectionTitle}」时存在未成功保存的内容，是否恢复本地草稿？`,
          okText: '恢复本地草稿',
          cancelText: '丢弃',
          onOk: () => {
            contentRef.current = backup;
            setContent(backup);
            setSaveStatus({ kind: 'dirty' });
          },
          onCancel: () => clearTextbookBackup(backupKey),
        });
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [backupKey]);

    // ---- 定时器管理 ----
    const clearTimers = useCallback(() => {
      if (idleTimerRef.current != null) window.clearTimeout(idleTimerRef.current);
      if (maxWaitTimerRef.current != null) window.clearTimeout(maxWaitTimerRef.current);
      idleTimerRef.current = null;
      maxWaitTimerRef.current = null;
    }, []);

    useEffect(() => () => {
      clearTimers();
      if (backoffTimerRef.current != null) window.clearTimeout(backoffTimerRef.current);
    }, [clearTimers]);

    // ---- 保存引擎（串行；接受版本更高的响应；退避后暂停） ----
    const runSave = useCallback(async (): Promise<SectionSaveResult> => {
      if (savingRef.current) {
        pendingAgainRef.current = true;
        return 'queued';
      }
      const contentNow = contentRef.current;
      if (contentNow === savedContentRef.current) return 'skipped';
      if (pausedRef.current) return 'paused';
      if (!canEditRef.current) return 'blocked';

      savingRef.current = true;
      setSaveStatus({ kind: 'saving' });
      try {
        const vo = await saveSectionContent(section.sectionId, {
          contentMd: contentNow,
          baseVersion: baseVersionRef.current,
        });
        // 返回乱序时只接受版本更高的响应（§7.6）
        if (vo.version > baseVersionRef.current) {
          savedContentRef.current = vo.contentMd;
          baseVersionRef.current = vo.version;
          lastSyncedVersionRef.current = vo.version;
          setBaseVersion(vo.version);
          if (contentRef.current === contentNow) {
            contentRef.current = vo.contentMd;
            setContent(vo.contentMd);
          }
          setSaveStatus({ kind: 'saved', at: new Date() });
          failCountRef.current = 0;
          pausedRef.current = false;
          clearTimers();
          clearTextbookBackup(textbookBackupKey(username, section.sectionId, baseVersionRef.current - 1));
          clearTextbookBackup(textbookBackupKey(username, section.sectionId, baseVersionRef.current));
          onSaved(vo);
        }
        return 'saved';
      } catch (e) {
        const conflictData = extractVersionConflict(e);
        if (conflictData) {
          pausedRef.current = true;
          setSaveStatus({ kind: 'failed', message: '保存冲突：服务端已有更新版本' });
          setConflict(conflictData);
          clearTimers();
          return 'conflict';
        }
        failCountRef.current += 1;
        if (failCountRef.current <= AUTOSAVE_MAX_RETRIES) {
          setSaveStatus({ kind: 'retrying', attempt: failCountRef.current });
          const delay = 2000 * failCountRef.current;
          if (backoffTimerRef.current != null) window.clearTimeout(backoffTimerRef.current);
          backoffTimerRef.current = window.setTimeout(() => {
            backoffTimerRef.current = null;
            void runSave();
          }, delay);
          return 'error';
        }
        pausedRef.current = true;
        setSaveStatus({ kind: 'failed', message: apiErrorMessage(e, '自动保存失败，已暂停，请手动重试') });
        clearTimers();
        return 'error';
      } finally {
        savingRef.current = false;
        if (pendingAgainRef.current) {
          pendingAgainRef.current = false;
          void runSave();
        }
      }
    }, [clearTimers, onSaved, section.sectionId, username]);

    const runSaveRef = useRef(runSave);
    runSaveRef.current = runSave;

    // ---- 变更 → 防抖 3s + 30s 兜底 + 本地备份 ----
    const scheduleAutosave = useCallback(() => {
      if (backoffTimerRef.current != null) {
        window.clearTimeout(backoffTimerRef.current);
        backoffTimerRef.current = null;
        failCountRef.current = 0;
      }
      if (idleTimerRef.current != null) window.clearTimeout(idleTimerRef.current);
      idleTimerRef.current = window.setTimeout(() => {
        idleTimerRef.current = null;
        void runSaveRef.current();
      }, AUTOSAVE_IDLE_MS);
      if (maxWaitTimerRef.current == null) {
        maxWaitTimerRef.current = window.setTimeout(() => {
          maxWaitTimerRef.current = null;
          void runSaveRef.current();
        }, AUTOSAVE_MAX_WAIT_MS);
      }
    }, []);

    const handleChange = useCallback(
      (value: string) => {
        contentRef.current = value;
        setContent(value);
        saveTextbookBackup(backupKey, value);
        scheduleAutosave();
      },
      [backupKey, scheduleAutosave],
    );

    // ---- Ctrl+S 手动保存 ----
    useEffect(() => {
      const handler = (e: KeyboardEvent) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
          e.preventDefault();
          void runSaveRef.current().then((r) => {
            if (r === 'saved') message.success('已保存');
          });
        }
      };
      window.addEventListener('keydown', handler);
      return () => window.removeEventListener('keydown', handler);
    }, []);

    // ---- 离开保护（切换节 / 返回 / 关闭页面前 dirty 先尝试保存，§10.5） ----
    const askLeaveAnyway = useCallback(() => {
      return new Promise<boolean>((resolve) => {
        Modal.confirm({
          title: '当前内容尚未保存成功',
          content: '离开将丢失未保存的修改（本地备份仍保留，可回到本节恢复）。确定放弃未保存内容离开？',
          okText: '放弃未保存内容离开',
          okButtonProps: { danger: true },
          cancelText: '继续留下',
          onOk: () => resolve(true),
          onCancel: () => resolve(false),
        });
      });
    }, []);

    const confirmLeave = useCallback(async () => {
      if (contentRef.current === savedContentRef.current) return true;
      const result = await runSaveRef.current();
      if (result === 'saved' || result === 'skipped') return true;
      return askLeaveAnyway();
    }, [askLeaveAnyway]);

    useImperativeHandle(ref, () => ({ confirmLeave, saveNow: runSave }), [confirmLeave, runSave]);

    useEffect(() => {
      const guard = () => confirmLeave();
      return registerGuard(guard);
    }, [confirmLeave, registerGuard]);

    // ---- 409 冲突处理 ----
    const downloadLocalDraft = useCallback(() => {
      const blob = new Blob([contentRef.current], { type: 'text/markdown;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${section.sectionTitle || '教材节'}_草稿.md`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, [section.sectionTitle]);

    const adoptServer = useCallback(() => {
      if (!conflict) return;
      contentRef.current = conflict.currentContentMd;
      savedContentRef.current = conflict.currentContentMd;
      baseVersionRef.current = conflict.currentVersion;
      lastSyncedVersionRef.current = conflict.currentVersion;
      setContent(conflict.currentContentMd);
      setBaseVersion(conflict.currentVersion);
      setSaveStatus({ kind: 'clean' });
      pausedRef.current = false;
      failCountRef.current = 0;
      clearTimers();
      onExternalAdopted(conflict);
      setConflict(null);
      message.info('已加载服务端最新内容');
    }, [clearTimers, conflict, onExternalAdopted]);

    // ---- 图片上传（A1，工具栏无外链图片入口） ----
    const handleImageUpload = useCallback(
      async (files: FileList | null) => {
        if (!files || files.length === 0) return;
        const invalid = Array.from(files).find((f) => !IMAGE_TYPES.includes(f.type) || f.size > IMAGE_MAX_SIZE);
        if (invalid) {
          message.error('仅支持不超过 10MB 的 JPG、PNG、WebP 图片');
          return;
        }
        setUploadingImage(true);
        try {
          for (const file of Array.from(files)) {
            const uploaded = await uploadSectionAsset(section.sectionId, file);
            const markdown = `\n\n![图片](${uploaded.sourceUri})\n`;
            if (editorRef.current?.insert) {
              editorRef.current.insert(() => ({ targetValue: markdown, select: false, focusNext: false }));
            } else {
              handleChange(`${contentRef.current}${markdown}`);
            }
          }
          message.success('图片已上传');
        } catch (e) {
          message.error(apiErrorMessage(e, '图片上传失败'));
        } finally {
          setUploadingImage(false);
          if (fileInputRef.current) fileInputRef.current.value = '';
        }
      },
      [handleChange, section.sectionId],
    );

    /** 编辑器预览内部资产 URI → blob URL（transformImgUrl，仅本节 textbook-asset://） */
    const transformImgUrl = useCallback(async (src: string) => {
      if (!src.startsWith('textbook-asset://')) return src;
      const assetId = src.slice('textbook-asset://'.length);
      const cached = objectUrlsRef.current.get(assetId);
      if (cached) return cached;
      try {
        const blob = await getSectionAssetContent(section.sectionId, assetId);
        const url = URL.createObjectURL(blob);
        objectUrlsRef.current.set(assetId, url);
        return url;
      } catch {
        return src;
      }
    }, [section.sectionId]);

    // ---- 提交审定（§10.5：先保存成功，再确认警告，不自动跳下一节） ----
    const handleSubmit = useCallback(async () => {
      if (contentRef.current !== savedContentRef.current) {
        const result = await runSaveRef.current();
        if (result !== 'saved' && result !== 'skipped') {
          message.error('请先保存成功后再提交审定');
          return;
        }
      }
      const warnings = collectSubmitWarnings({
        ...section,
        contentMd: contentRef.current,
      } as SectionContentVO);
      const doSubmit = async () => {
        setSubmitting(true);
        try {
          const vo = await submitSectionContent(section.sectionId, baseVersionRef.current);
          message.success('已提交审定，本节切换为只读');
          onSubmitted(vo);
        } catch (e) {
          const conflictData = extractVersionConflict(e);
          if (conflictData) {
            setConflict(conflictData);
          } else {
            message.error(apiErrorMessage(e, '提交失败'));
          }
        } finally {
          setSubmitting(false);
        }
      };
      if (warnings.length > 0) {
        Modal.confirm({
          title: '检查存在以下提示，确认仍要提交？',
          content: (
            <ul style={{ paddingLeft: 18, margin: 0 }}>
              {warnings.map((w, i) => (
                <li key={i}>{w}</li>
              ))}
            </ul>
          ),
          okText: '仍要提交',
          cancelText: '返回修改',
          onOk: doSubmit,
        });
      } else {
        await doSubmit();
      }
    }, [onSubmitted, section, section.sectionId]);

    // 保存再次变更后，「已保存/干净」态应回落为「未保存」
    const effectiveSaveStatus: SaveStatus =
      dirty && (saveStatus.kind === 'clean' || saveStatus.kind === 'saved')
        ? { kind: 'dirty' }
        : saveStatus;

    const saveStatusText = (() => {
      switch (effectiveSaveStatus.kind) {
        case 'clean':
          return '已保存';
        case 'dirty':
          return '未保存';
        case 'saving':
          return '保存中…';
        case 'saved':
          return `已于 ${formatTime(effectiveSaveStatus.at)} 保存`;
        case 'retrying':
          return `保存失败，自动重试中（第 ${effectiveSaveStatus.attempt} 次）`;
        case 'failed':
          return effectiveSaveStatus.message;
      }
    })();

    const manualRetry = () => {
      pausedRef.current = false;
      failCountRef.current = 0;
      void runSave();
    };

    return (
      <div>
        <div style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <ContentStatusTag status={section.contentStatus} />
          <span>目标字数 {section.targetWords ?? '-'}</span>
          <span style={{ color: '#888' }}>当前 {section.wordCount} 字</span>
          <span
            aria-label="保存状态"
            style={{
              color:
                effectiveSaveStatus.kind === 'failed'
                  ? '#cf1322'
                  : effectiveSaveStatus.kind === 'dirty' || effectiveSaveStatus.kind === 'retrying'
                    ? '#d46b08'
                    : '#888',
            }}
          >
            {saveStatusText}
          </span>
          {effectiveSaveStatus.kind === 'failed' && (
            <Button size="small" onClick={manualRetry}>
              手动重试
            </Button>
          )}
        </div>

        {!canEdit && (
          <Alert type="info" showIcon style={{ marginBottom: 8 }} message={section.readOnlyReason || '当前状态只读'} />
        )}

        {/* 主操作平铺（§10.5：保存草稿/提交审定/历史版本/导出本节 + 上传图片） */}
        {canEdit && (
          <Space wrap style={{ marginBottom: 8 }}>
            <Button onClick={() => void runSave()}>保存草稿</Button>
            <Popconfirm
              title="确认提交审定？"
              description="提交后本节将转为只读，等待审定人处理。"
              okText="提交审定"
              cancelText="取消"
              onConfirm={() => void handleSubmit()}
            >
              <Button type="primary" loading={submitting} disabled={submitting}>
                提交审定
              </Button>
            </Popconfirm>
            <Button onClick={onOpenVersions}>历史版本</Button>
            <Button onClick={onExportSection}>导出本节</Button>
            <Button icon={<PictureOutlined />} loading={uploadingImage} onClick={() => fileInputRef.current?.click()}>
              上传图片
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              style={{ display: 'none' }}
              onChange={(e) => void handleImageUpload(e.target.files)}
            />
          </Space>
        )}
        {!canEdit && section.contentExists && (
          <Space wrap style={{ marginBottom: 8 }}>
            <Button onClick={onOpenVersions}>历史版本</Button>
            <Button onClick={onExportSection}>导出本节</Button>
          </Space>
        )}

        {canEdit ? (
          <MdEditor
            ref={editorRef as never}
            language="zh-CN"
            value={content}
            onChange={handleChange}
            onSave={() => void runSave()}
            transformImgUrl={transformImgUrl}
            sanitize={sanitizeTextbookHtml}
            toolbarsExclude={EDITOR_TOOLBARS_EXCLUDE}
            noMermaid
            noKatex
            noPrettier
            placeholder="请输入本节正文（支持 Markdown；图片请点击「上传图片」，不支持外链图片）"
            style={{ height: 520 }}
          />
        ) : (
          <div>
            {!section.contentExists && <Alert type="warning" showIcon message="本节尚未编写" />}
            <MdPreview value={readOnlyMarkdown} sanitize={sanitizeTextbookHtml} noMermaid noKatex />
          </div>
        )}

        {/* 409 冲突弹窗：展示服务端版本/更新人/时间，不自动覆盖（§10.5） */}
        <Modal
          open={!!conflict}
          title="保存冲突：服务端已有更新版本"
          okText="加载服务端最新"
          cancelText="继续编辑（保留我的内容）"
          onOk={adoptServer}
          onCancel={() => setConflict(null)}
          footer={[
            <Button key="download" onClick={downloadLocalDraft}>
              下载我的草稿为 .md
            </Button>,
            <Button key="stay" onClick={() => setConflict(null)}>
              继续编辑（保留我的内容）
            </Button>,
            <Button key="reload" type="primary" danger onClick={adoptServer}>
              加载服务端最新
            </Button>,
          ]}
        >
          {conflict && (
            <div>
              <p>检测到他人或另一窗口已保存更新的内容，当前编辑内容<b>不会被自动覆盖</b>。</p>
              <p>
                服务端版本 v{conflict.currentVersion}
                {conflict.currentUpdatedByName ? `，更新人：${conflict.currentUpdatedByName}` : ''}
                {conflict.currentUpdatedAt ? `，更新时间：${conflict.currentUpdatedAt}` : ''}
              </p>
              <p style={{ color: '#888' }}>可先「下载我的草稿为 .md」留底，再决定是否加载服务端最新内容。</p>
            </div>
          )}
        </Modal>
      </div>
    );
  },
);

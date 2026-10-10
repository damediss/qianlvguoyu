import { useEffect, useMemo, useState } from 'react';
import { Button, Empty, List, Space, Typography, Upload, message } from 'antd';
import { ArrowLeftOutlined, EditOutlined, FileOutlined } from '@ant-design/icons';
import type { SubsidyMaterialInstance, SubsidyMaterialSlotFile } from '../../../../api/training';
import * as trainApi from '../../../../api/training';
import {
  downloadUrlFile,
  fetchPlatformFileBlob,
  fetchUrlFileBlob,
  resolveFilePreviewKind,
  type FilePreviewKind,
} from '../../../../api/files';
import { MaterialStatusTag } from './MaterialStatusTag';
import { MaterialFilePreview } from './MaterialFilePreview';
import { MaterialSpreadsheetEditor } from './MaterialSpreadsheetEditor';
import { DocxMetaEditor } from './DocxMetaEditor';
import { DocxOfficeEditor } from './DocxOfficeEditor';

type Props = {
  item: SubsidyMaterialInstance | null;
  onConfirm?: () => void;
  confirmLoading?: boolean;
  onReplaceFile?: (file: File) => void | Promise<void>;
  replaceLoading?: boolean;
  onFillFromTemplate?: () => void;
  /** OnlyOffice / 字段编辑保存后刷新清单与预览（可 async） */
  onOfficeSaved?: () => void | Promise<void>;
};

type PreviewTarget = {
  key: string;
  fileId?: string | number | null;
  filePath?: string | null;
  fileName?: string | null;
};

function isSpreadsheet(name?: string | null, path?: string | null) {
  const s = `${name || ''} ${path || ''}`.toLowerCase();
  return /\.(xlsx?|csv)(\?|$)/i.test(s);
}

function isWordDoc(name?: string | null, path?: string | null) {
  const s = `${name || ''} ${path || ''}`.toLowerCase();
  return /\.docx?(\?|$)/i.test(s);
}

/** 申请表/花名册/汇总等默认可进 Office 在线编辑（文件名常无扩展名） */
function isOfficeEditableMaterialType(materialType?: string | null) {
  return (
    materialType === 'application_form'
    || materialType === 'roster'
    || materialType === 'summary'
    || materialType === 'other'
  );
}

/** 证书 PDF 等多文件并列；申请表/花名册等即使 slotFiles 有历史记录也按当前版本单文件处理 */
function isMultiFileMaterial(item: SubsidyMaterialInstance) {
  if (item.materialType === 'certificate') return true;
  const slots = item.slotFiles || [];
  if (slots.length <= 1) return false;
  const mainName = item.fileName || '';
  if (isSpreadsheet(mainName) || isWordDoc(mainName)) return false;
  if (isOfficeEditableMaterialType(item.materialType)) return false;
  return slots.length > 1;
}

function buildSlotTargets(item: SubsidyMaterialInstance): PreviewTarget[] {
  const slots = item.slotFiles || [];

  if (isMultiFileMaterial(item) && slots.length > 0) {
    return slots.map((f: SubsidyMaterialSlotFile, idx) => ({
      key: String(f.id ?? `slot-${idx}`),
      fileId: f.fileId,
      filePath: f.filePath,
      fileName: f.fileName || `文件${idx + 1}`,
    }));
  }

  if (item.fileId || item.filePath || item.photoPackPath) {
    return [
      {
        key: 'main',
        fileId: item.fileId,
        filePath: item.filePath || item.photoPackPath,
        fileName: item.fileName || item.photoPackTitle || item.title,
      },
    ];
  }

  if (slots.length === 1) {
    const f = slots[0];
    return [
      {
        key: String(f.id ?? 'slot-0'),
        fileId: f.fileId,
        filePath: f.filePath,
        fileName: f.fileName || item.title,
      },
    ];
  }

  return [];
}

export function MaterialWorkspacePanel({
  item,
  onConfirm,
  confirmLoading,
  onReplaceFile,
  replaceLoading,
  onFillFromTemplate,
  onOfficeSaved,
}: Props) {
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [savingCells, setSavingCells] = useState(false);
  /** Word 默认进 OnlyOffice；服务不可用时再回退字段编辑 */
  const [wordEditorMode, setWordEditorMode] = useState<'meta' | 'office'>('office');
  /** 文件名无扩展名时，用 MIME 嗅探结果决定编辑器 */
  const [sniffedKind, setSniffedKind] = useState<FilePreviewKind | null>(null);
  /** OnlyOffice 是否可用（探测完成后才为最终值） */
  const [onlyOfficeAvailable, setOnlyOfficeAvailable] = useState(false);
  const [onlyOfficeChecked, setOnlyOfficeChecked] = useState(false);

  const targets = useMemo(() => (item ? buildSlotTargets(item) : []), [item]);

  useEffect(() => {
    setSelectedKey(null);
    setEditing(false);
    setWordEditorMode('office');
    setSniffedKind(null);
    setOnlyOfficeAvailable(false);
    setOnlyOfficeChecked(false);
  }, [item?.id]);

  useEffect(() => {
    if (!item?.hasFile || !item?.id) {
      setOnlyOfficeAvailable(false);
      setOnlyOfficeChecked(true);
      return;
    }
    let cancelled = false;
    setOnlyOfficeChecked(false);
    void trainApi
      .getSubsidyMaterialOfficeConfig(item.id)
      .then((cfg) => {
        if (!cancelled) {
          const ok = !!cfg.enabled && !!cfg.config && !!cfg.apiScriptUrl;
          setOnlyOfficeAvailable(ok);
          setOnlyOfficeChecked(true);
          if (!ok) setWordEditorMode('meta');
        }
      })
      .catch(() => {
        if (!cancelled) {
          setOnlyOfficeAvailable(false);
          setOnlyOfficeChecked(true);
          setWordEditorMode('meta');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [item?.id, item?.hasFile]);

  const multiFile = !!item && isMultiFileMaterial(item) && targets.length > 1;

  const active =
    item == null
      ? null
      : selectedKey != null
        ? targets.find((t) => t.key === selectedKey) ?? null
        : multiFile
          ? null
          : targets[0] ?? null;

  useEffect(() => {
    if (!item?.hasFile || !active || multiFile) {
      setSniffedKind(null);
      return;
    }
    const nameHint = `${active.fileName || ''} ${item.fileName || ''} ${item.uploadTemplateFileName || ''}`;
    if (isWordDoc(nameHint) || isSpreadsheet(nameHint)) {
      setSniffedKind(isWordDoc(nameHint) ? 'docx' : 'spreadsheet');
      return;
    }

    let cancelled = false;
    const sniff = async () => {
      try {
        const blob = active.filePath
          ? await fetchUrlFileBlob(active.filePath)
          : active.fileId != null
            ? await fetchPlatformFileBlob(String(active.fileId))
            : null;
        if (cancelled || !blob) return;
        const kind = resolveFilePreviewKind(
          active.fileName || item.fileName || item.title || '',
          undefined,
          blob.type,
        );
        if (!cancelled && (kind === 'docx' || kind === 'spreadsheet')) {
          setSniffedKind(kind);
        } else if (!cancelled && isOfficeEditableMaterialType(item.materialType)) {
          setSniffedKind(
            item.materialType === 'roster' || item.materialType === 'summary' ? 'spreadsheet' : 'docx',
          );
        }
      } catch {
        if (!cancelled && isOfficeEditableMaterialType(item.materialType)) {
          setSniffedKind(
            item.materialType === 'roster' || item.materialType === 'summary' ? 'spreadsheet' : 'docx',
          );
        }
      }
    };
    void sniff();
    return () => {
      cancelled = true;
    };
  }, [
    item?.id,
    item?.hasFile,
    item?.fileName,
    item?.title,
    item?.materialType,
    item?.uploadTemplateFileName,
    active?.fileId,
    active?.filePath,
    active?.fileName,
    multiFile,
  ]);

  if (!item) {
    return (
      <div className="material-workspace-empty">
        <Empty description="请从左侧选择材料查看或编辑" />
      </div>
    );
  }

  const nameBundle = `${active?.fileName || ''} ${item.fileName || ''} ${item.uploadTemplateFileName || ''} ${active?.filePath || ''}`;
  const spreadsheet =
    sniffedKind === 'spreadsheet'
    || (sniffedKind == null
      && (isSpreadsheet(active?.fileName, active?.filePath)
        || isSpreadsheet(item.fileName, item.filePath)
        || /\.xlsx?/i.test(nameBundle)));
  // 申请表：扩展名缺失时也先按 Word 露出编辑（预览已能用 MIME 渲 docx）
  const wordDoc =
    sniffedKind === 'docx'
    || (sniffedKind == null
      && (isWordDoc(active?.fileName, active?.filePath)
        || isWordDoc(item.fileName, item.filePath)
        || /\.docx?/i.test(nameBundle)))
    || (sniffedKind == null
      && item.hasFile
      && item.materialType === 'application_form'
      && !isSpreadsheet(nameBundle));

  const canEditSpreadsheet = !!active && !multiFile && spreadsheet && item.hasFile && !!onReplaceFile;
  const canEditWord = !!active && !multiFile && wordDoc && item.hasFile;
  const canEdit =
    item.hasFile
    && !multiFile
    && !!active
    && (canEditSpreadsheet
      || canEditWord
      || (isOfficeEditableMaterialType(item.materialType) && item.hasFile));

  const downloadTarget = active || (!multiFile ? targets[0] : null);

  const handleSaveSpreadsheetCells = async (cells: Record<string, string>) => {
    setSavingCells(true);
    try {
      await trainApi.patchSubsidyMaterialCells(item.id, cells);
      message.success('已保存（保留原表边框格式）');
      setEditing(false);
      onOfficeSaved?.();
    } catch (e) {
      message.error((e as Error).message || '保存失败');
      throw e;
    } finally {
      setSavingCells(false);
    }
  };

  const refreshAfterSave = async () => {
    try {
      await onOfficeSaved?.();
    } catch {
      /* 上层已提示 */
    }
  };

  const enterEditing = () => {
    if (editing) {
      // 退出编辑前拉一次最新版本，避免预览仍显示打开编辑前的旧文件
      setWordEditorMode('office');
      setEditing(false);
      void refreshAfterSave();
      return;
    }
    if (sniffedKind == null && item.materialType === 'application_form' && !spreadsheet) {
      setSniffedKind('docx');
    }
    const isWord =
      !spreadsheet
      && (wordDoc || sniffedKind === 'docx' || item.materialType === 'application_form');
    if (isWord) {
      // 已确认 OnlyOffice 不可用 → 字段编辑；否则优先进在线排版（失败再回退）
      setWordEditorMode(onlyOfficeChecked && !onlyOfficeAvailable ? 'meta' : 'office');
    }
    setEditing(true);
  };

  const showWordEditor = editing && !!active && (canEditWord || (editing && sniffedKind === 'docx'));
  const showSheetEditor = editing && !!active && canEditSpreadsheet;

  return (
    <div className="material-workspace-panel">
      <div className="material-workspace-header">
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>
            {item.title}
          </Typography.Title>
          <Space size={8} wrap style={{ marginTop: 6 }}>
            <MaterialStatusTag status={item.status} />
            {item.materialTypeLabel ? (
              <Typography.Text type="secondary">{item.materialTypeLabel}</Typography.Text>
            ) : null}
            {item.versionNo != null ? (
              <Typography.Text type="secondary">v{item.versionNo}</Typography.Text>
            ) : null}
            {multiFile ? (
              <Typography.Text type="secondary">{targets.length} 个文件</Typography.Text>
            ) : null}
          </Space>
        </div>
        <Space wrap size={[8, 8]} className="material-workspace-actions">
          {multiFile && active ? (
            <Button icon={<ArrowLeftOutlined />} onClick={() => setSelectedKey(null)}>
              返回列表
            </Button>
          ) : null}
          {onFillFromTemplate && !multiFile ? (
            <Button onClick={onFillFromTemplate} disabled={editing}>
              从模板填数
            </Button>
          ) : null}
          {onReplaceFile && !multiFile ? (
            <Upload
              showUploadList={false}
              disabled={editing}
              beforeUpload={(file) => {
                void onReplaceFile(file as File);
                return false;
              }}
            >
              <Button loading={replaceLoading} disabled={editing}>
                替换上传
              </Button>
            </Upload>
          ) : null}
          {canEdit ? (
            <Button
              type={editing ? 'primary' : 'default'}
              icon={<EditOutlined />}
              onClick={enterEditing}
            >
              {editing ? '退出编辑' : '编辑'}
            </Button>
          ) : null}
          {item.status !== 'confirmed' && item.hasFile && onConfirm && !multiFile ? (
            <Button type="primary" loading={confirmLoading} onClick={onConfirm} disabled={editing}>
              确认本材料
            </Button>
          ) : null}
          {downloadTarget?.filePath ? (
            <Button
              disabled={editing}
              onClick={() =>
                void downloadUrlFile(
                  downloadTarget.filePath!,
                  downloadTarget.fileName || item.fileName || 'material-file',
                ).catch((e: Error) => {
                  message.error(e?.message || '下载失败');
                })
              }
            >
              下载
            </Button>
          ) : null}
        </Space>
      </div>

      {!item.hasFile && targets.length === 0 ? (
        <Empty description="暂无文件：可用「从模板填数」、「替换上传」，或先「从本班刷新材料」" />
      ) : multiFile && !active ? (
        <div className="material-file-list">
          <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
            本材料含多个文件，请点击查看：
          </Typography.Paragraph>
          <List
            size="small"
            bordered
            dataSource={targets}
            renderItem={(f, idx) => (
              <List.Item
                className="material-file-list-item"
                onClick={() => setSelectedKey(f.key)}
                actions={[
                  <Button key="view" type="link" size="small" onClick={() => setSelectedKey(f.key)}>
                    查看
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  avatar={<FileOutlined />}
                  title={f.fileName || `文件 ${idx + 1}`}
                  description={f.filePath || (f.fileId != null ? `fileId=${f.fileId}` : undefined)}
                />
              </List.Item>
            )}
          />
        </div>
      ) : active && showSheetEditor ? (
        <MaterialSpreadsheetEditor
          fileId={active.fileId}
          filePath={active.filePath}
          fileName={active.fileName || item.fileName}
          onSaveCells={handleSaveSpreadsheetCells}
          saveLoading={savingCells || !!replaceLoading}
          editable
        />
      ) : active && showWordEditor ? (
        wordEditorMode === 'office' ? (
          <div>
            <div style={{ padding: '8px 12px' }}>
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setWordEditorMode('meta');
                  void refreshAfterSave();
                }}
              >
                ← 改用字段编辑
              </Button>
            </div>
            <DocxOfficeEditor
              instanceId={item.id}
              versionKey={item.currentVersionId ?? item.versionNo}
              onUnavailable={(reason) => {
                message.info(reason || 'OnlyOffice 不可用，已切换到字段编辑');
                setWordEditorMode('meta');
                setOnlyOfficeAvailable(false);
                setOnlyOfficeChecked(true);
              }}
              onSaved={() => refreshAfterSave()}
            />
          </div>
        ) : (
          <DocxMetaEditor
            instanceId={item.id}
            onlyOfficeAvailable={onlyOfficeAvailable}
            onCancel={() => setEditing(false)}
            onOpenFullEditor={
              onlyOfficeAvailable
                ? () => setWordEditorMode('office')
                : undefined
            }
            onSaved={() => {
              void refreshAfterSave().then(() => setEditing(false));
            }}
          />
        )
      ) : active && editing ? (
        <Empty description="正在识别文件类型…若仍无法编辑，请确认文件为 docx/xlsx" />
      ) : active ? (
        <MaterialFilePreview
          fileId={active.fileId}
          filePath={active.filePath}
          fileName={active.fileName || item.fileName}
          versionKey={item.currentVersionId ?? item.versionNo}
        />
      ) : (
        <Empty description="暂无文件可预览" />
      )}
    </div>
  );
}

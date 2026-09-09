import { useEffect, useMemo, useState } from 'react';
import { Button, Empty, List, Space, Typography, Upload, message } from 'antd';
import { ArrowLeftOutlined, EditOutlined, FileOutlined } from '@ant-design/icons';
import type { SubsidyMaterialInstance, SubsidyMaterialSlotFile } from '../../../../api/training';
import * as trainApi from '../../../../api/training';
import { downloadUrlFile } from '../../../../api/files';
import { MaterialStatusTag } from './MaterialStatusTag';
import { MaterialFilePreview } from './MaterialFilePreview';
import { MaterialSpreadsheetEditor } from './MaterialSpreadsheetEditor';
import { DocxOfficeEditor } from './DocxOfficeEditor';

type Props = {
  item: SubsidyMaterialInstance | null;
  onConfirm?: () => void;
  confirmLoading?: boolean;
  onReplaceFile?: (file: File) => void | Promise<void>;
  replaceLoading?: boolean;
  onFillFromTemplate?: () => void;
  onOfficeSaved?: () => void;
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

/** 证书 PDF 等多文件并列；申请表/花名册等即使 slotFiles 有历史记录也按当前版本单文件处理 */
function isMultiFileMaterial(item: SubsidyMaterialInstance) {
  if (item.materialType === 'certificate') return true;
  const slots = item.slotFiles || [];
  if (slots.length <= 1) return false;
  // 多个 slot 文件且当前主文件不像 office 文档 → 按多文件列表
  const mainName = item.fileName || '';
  if (isSpreadsheet(mainName) || isWordDoc(mainName)) return false;
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

  const targets = useMemo(() => (item ? buildSlotTargets(item) : []), [item]);

  useEffect(() => {
    setSelectedKey(null);
    setEditing(false);
  }, [item?.id]);

  if (!item) {
    return (
      <div className="material-workspace-empty">
        <Empty description="请从左侧选择材料查看或编辑" />
      </div>
    );
  }

  const multiFile = isMultiFileMaterial(item) && targets.length > 1;

  const active =
    selectedKey != null
      ? targets.find((t) => t.key === selectedKey) ?? null
      : multiFile
        ? null
        : targets[0] ?? null;

  const fileLabel = `${active?.fileName || ''} ${item.fileName || ''} ${active?.filePath || ''}`;
  const spreadsheet =
    isSpreadsheet(active?.fileName, active?.filePath)
    || isSpreadsheet(item.fileName, item.filePath)
    || /\.xlsx?/i.test(fileLabel);
  const wordDoc =
    isWordDoc(active?.fileName, active?.filePath)
    || isWordDoc(item.fileName, item.filePath);

  const canEditSpreadsheet = !!active && !multiFile && spreadsheet && item.hasFile && !!onReplaceFile;
  const canEditWord = !!active && !multiFile && wordDoc && item.hasFile;
  const canEdit = item.hasFile && !multiFile && (!!onReplaceFile || canEditWord) && (spreadsheet || wordDoc);

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
        <Space wrap size={[8, 8]}>
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
              onClick={() => setEditing((v) => !v)}
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
                ).catch(() => undefined)
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
      ) : active && editing && canEditSpreadsheet ? (
        <MaterialSpreadsheetEditor
          fileId={active.fileId}
          filePath={active.filePath}
          fileName={active.fileName || item.fileName}
          onSaveCells={handleSaveSpreadsheetCells}
          saveLoading={savingCells || !!replaceLoading}
          editable
        />
      ) : active && editing && canEditWord ? (
        <DocxOfficeEditor
          instanceId={item.id}
          versionKey={item.currentVersionId ?? item.versionNo}
          onSaved={() => {
            onOfficeSaved?.();
            setEditing(false);
          }}
        />
      ) : active ? (
        <MaterialFilePreview
          fileId={active.fileId}
          filePath={active.filePath}
          fileName={active.fileName || item.fileName}
        />
      ) : (
        <Empty description="暂无文件可预览" />
      )}
    </div>
  );
}

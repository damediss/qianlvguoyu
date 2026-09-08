import { useEffect, useMemo, useState } from 'react';
import { Button, Empty, List, Space, Typography, Upload } from 'antd';
import { ArrowLeftOutlined, FileOutlined } from '@ant-design/icons';
import type { SubsidyMaterialInstance, SubsidyMaterialSlotFile } from '../../../../api/training';
import { downloadUrlFile } from '../../../../api/files';
import { MaterialStatusTag } from './MaterialStatusTag';
import { MaterialFilePreview } from './MaterialFilePreview';
import { DocxOfficeEditor } from './DocxOfficeEditor';

type Props = {
  item: SubsidyMaterialInstance | null;
  onConfirm?: () => void;
  confirmLoading?: boolean;
  onReplaceFile?: (file: File) => void;
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

function isOfficeDoc(name?: string | null, path?: string | null) {
  const s = `${name || ''} ${path || ''}`.toLowerCase();
  return /\.(docx?|xlsx?|pptx?|odt|ods|odp|rtf|csv)(\?|$)/i.test(s);
}

function buildSlotTargets(item: SubsidyMaterialInstance): PreviewTarget[] {
  const slots = item.slotFiles || [];
  if (slots.length > 0) {
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
  const [officeEdit, setOfficeEdit] = useState(false);

  const targets = useMemo(() => (item ? buildSlotTargets(item) : []), [item]);
  const multiFile = targets.length > 1;

  useEffect(() => {
    setSelectedKey(null);
    setOfficeEdit(false);
  }, [item?.id]);

  if (!item) {
    return (
      <div className="material-workspace-empty">
        <Empty description="请从左侧选择材料查看或编辑" />
      </div>
    );
  }

  const active =
    selectedKey != null
      ? targets.find((t) => t.key === selectedKey) ?? null
      : multiFile
        ? null
        : targets[0] ?? null;

  const canOfficeEdit =
    !!active
    && !multiFile
    && isOfficeDoc(active.fileName, active.filePath)
    && item.hasFile;

  const downloadTarget = active || (!multiFile ? targets[0] : null);

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
        <Space wrap>
          {multiFile && active ? (
            <Button icon={<ArrowLeftOutlined />} onClick={() => setSelectedKey(null)}>
              返回列表
            </Button>
          ) : null}
          {onFillFromTemplate && !multiFile ? (
            <Button onClick={onFillFromTemplate}>从模板填数</Button>
          ) : null}
          {onReplaceFile && !multiFile ? (
            <Upload
              showUploadList={false}
              beforeUpload={(file) => {
                onReplaceFile(file as File);
                return false;
              }}
            >
              <Button loading={replaceLoading}>替换上传</Button>
            </Upload>
          ) : null}
          {canOfficeEdit ? (
            <Button type={officeEdit ? 'primary' : 'default'} onClick={() => setOfficeEdit((v) => !v)}>
              {officeEdit ? '退出在线编辑' : 'OnlyOffice 编辑'}
            </Button>
          ) : null}
          {item.status !== 'confirmed' && item.hasFile && onConfirm && !multiFile ? (
            <Button type="primary" loading={confirmLoading} onClick={onConfirm}>
              确认本材料
            </Button>
          ) : null}
          {downloadTarget?.filePath ? (
            <Button
              onClick={() =>
                void downloadUrlFile(
                  downloadTarget.filePath!,
                  downloadTarget.fileName || 'material-file',
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
      ) : active && officeEdit && canOfficeEdit ? (
        <DocxOfficeEditor
          instanceId={item.id}
          versionKey={item.currentVersionId ?? item.versionNo}
          onSaved={onOfficeSaved}
        />
      ) : active ? (
        <MaterialFilePreview
          fileId={active.fileId}
          filePath={active.filePath}
          fileName={active.fileName}
        />
      ) : (
        <Empty description="暂无文件可预览" />
      )}
    </div>
  );
}

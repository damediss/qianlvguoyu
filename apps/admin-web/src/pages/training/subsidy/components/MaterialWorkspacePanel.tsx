import { Button, Empty, Space, Typography, Upload } from 'antd';
import type { SubsidyMaterialInstance } from '../../../../api/training';
import { downloadUrlFile } from '../../../../api/files';
import { MaterialStatusTag } from './MaterialStatusTag';
import { DocxOfficeEditor } from './DocxOfficeEditor';

type Props = {
  item: SubsidyMaterialInstance | null;
  onConfirm?: () => void;
  confirmLoading?: boolean;
  onReplaceFile?: (file: File) => void;
  replaceLoading?: boolean;
  onOfficeSaved?: () => void;
};

function isPdf(path?: string | null, name?: string | null) {
  const s = `${path || ''} ${name || ''}`.toLowerCase();
  return s.includes('.pdf');
}

function isImage(path?: string | null, name?: string | null) {
  const s = `${path || ''} ${name || ''}`.toLowerCase();
  return /\.(png|jpe?g|gif|webp)(\?|$)/i.test(s);
}

function isOfficeDoc(name?: string | null, path?: string | null) {
  const s = `${name || ''} ${path || ''}`.toLowerCase();
  return /\.(docx?|xlsx?|pptx?|odt|ods|odp|rtf|csv)(\?|$)/i.test(s);
}

export function MaterialWorkspacePanel({
  item,
  onConfirm,
  confirmLoading,
  onReplaceFile,
  replaceLoading,
  onOfficeSaved,
}: Props) {
  if (!item) {
    return (
      <div className="material-workspace-empty">
        <Empty description="请从左侧选择材料查看或编辑" />
      </div>
    );
  }

  const previewPath = item.filePath || item.photoPackPath;
  const previewName = item.fileName || item.photoPackTitle || item.title;
  const officeEditable = item.hasFile && isOfficeDoc(previewName, previewPath);

  return (
    <div className="material-workspace-panel">
      <div className="material-workspace-header">
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>
            {item.title}
          </Typography.Title>
          <Space size={8} wrap style={{ marginTop: 6 }}>
            <MaterialStatusTag status={item.status} />
            {item.materialTypeLabel ? <Typography.Text type="secondary">{item.materialTypeLabel}</Typography.Text> : null}
            {item.versionNo != null ? <Typography.Text type="secondary">v{item.versionNo}</Typography.Text> : null}
            {officeEditable ? <Typography.Text type="success" style={{ fontSize: 12 }}>OnlyOffice 可编辑</Typography.Text> : null}
          </Space>
        </div>
        <Space wrap>
          {onReplaceFile ? (
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
          {item.status !== 'confirmed' && item.hasFile && onConfirm ? (
            <Button type="primary" loading={confirmLoading} onClick={onConfirm}>
              确认本材料
            </Button>
          ) : null}
          {previewPath ? (
            <Button
              onClick={() =>
                void downloadUrlFile(previewPath, previewName || 'material-file').catch(() => undefined)
              }
            >
              下载
            </Button>
          ) : null}
        </Space>
      </div>

      {!item.hasFile ? (
        <Empty description="暂无文件，请「替换上传」或先执行「从本班刷新材料」后再在线编辑" />
      ) : officeEditable ? (
        <DocxOfficeEditor
          instanceId={item.id}
          versionKey={item.currentVersionId ?? item.versionNo}
          onSaved={onOfficeSaved}
        />
      ) : previewPath && isPdf(previewPath, previewName) ? (
        <iframe title="pdf-preview" className="material-preview-frame" src={previewPath} />
      ) : previewPath && isImage(previewPath, previewName) ? (
        <img className="material-preview-image" src={previewPath} alt={previewName || 'preview'} />
      ) : (
        <div className="material-preview-fallback">
          <Typography.Paragraph>
            当前文件：<Typography.Text code>{previewName}</Typography.Text>
          </Typography.Paragraph>
          <Typography.Paragraph type="secondary">
            该格式暂不支持内嵌预览/在线编辑，请下载后本地查看，或使用「替换上传」。
          </Typography.Paragraph>
        </div>
      )}
    </div>
  );
}

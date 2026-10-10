import { useEffect, useRef, useState } from 'react';
import { Alert, Empty, Image, Spin, Typography } from 'antd';
import {
  fetchPlatformFileBlob,
  fetchUrlFileBlob,
  resolveFilePreviewKind,
  type FilePreviewKind,
} from '../../../../api/files';
import { renderSpreadsheetPreview } from '../../../../utils/spreadsheetPreview';

type Props = {
  fileId?: string | number | null;
  filePath?: string | null;
  fileName?: string | null;
  /** 版本变化时强制重新拉文件（避免仅 filePath 字符串相同或缓存导致旧预览） */
  versionKey?: string | number | null;
};

/**
 * 补贴材料工作台内嵌预览：docx（docx-preview）/ xlsx（SheetJS）/ pdf / 图片。
 * 不依赖 OnlyOffice，私有 COS 走鉴权下载。
 */
export function MaterialFilePreview({ fileId, filePath, fileName, versionKey }: Props) {
  const docxRef = useRef<HTMLDivElement>(null);
  const sheetRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [kind, setKind] = useState<FilePreviewKind>('unsupported');
  const [objectUrl, setObjectUrl] = useState<string>();

  useEffect(() => {
    if (!fileId && !filePath) {
      setKind('unsupported');
      setError(null);
      return;
    }

    let cancelled = false;
    let localUrl: string | undefined;

    async function load() {
      setLoading(true);
      setError(null);
      setKind('unsupported');
      setObjectUrl(undefined);
      if (docxRef.current) docxRef.current.innerHTML = '';
      if (sheetRef.current) sheetRef.current.innerHTML = '';

      try {
        const blob = filePath
          ? await fetchUrlFileBlob(filePath)
          : await fetchPlatformFileBlob(String(fileId));
        if (cancelled) return;

        const resolved = resolveFilePreviewKind(fileName || '', undefined, blob.type);

        if (resolved === 'spreadsheet') {
          setKind('spreadsheet');
          await new Promise<void>((r) => requestAnimationFrame(() => r()));
          if (cancelled || !sheetRef.current) return;
          await renderSpreadsheetPreview(blob, sheetRef.current);
          return;
        }

        if (resolved === 'docx') {
          setKind('docx');
          await new Promise<void>((r) => requestAnimationFrame(() => r()));
          if (cancelled || !docxRef.current) return;
          docxRef.current.innerHTML = '';
          const { renderAsync } = await import('docx-preview');
          await renderAsync(blob, docxRef.current, undefined, {
            className: 'docx-preview-container',
            inWrapper: true,
            ignoreWidth: false,
            ignoreHeight: false,
          });
          return;
        }

        if (resolved === 'pdf' || resolved === 'image') {
          localUrl = URL.createObjectURL(blob);
          setObjectUrl(localUrl);
          setKind(resolved);
          return;
        }

        // doc 等旧格式：尝试按 office 提示下载
        setKind('unsupported');
      } catch (e) {
        if (!cancelled) {
          setError((e as Error).message || '预览失败');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
      if (localUrl) URL.revokeObjectURL(localUrl);
    };
  }, [fileId, filePath, fileName, versionKey]);

  if (!fileId && !filePath) {
    return <Empty description="暂无文件可预览" />;
  }

  if (error) {
    return <Alert type="warning" showIcon message="预览失败" description={error} />;
  }

  return (
    <div className="material-file-preview">
      <Spin spinning={loading} tip="加载预览…">
        <div className="material-file-preview-body">
          {kind === 'image' && objectUrl ? (
            <Image src={objectUrl} alt={fileName || 'preview'} className="material-preview-image" />
          ) : null}
          {kind === 'pdf' && objectUrl ? (
            <iframe title="pdf-preview" className="material-preview-frame" src={objectUrl} />
          ) : null}
          <div
            ref={sheetRef}
            className="material-spreadsheet-preview"
            style={{ display: kind === 'spreadsheet' ? 'block' : 'none' }}
          />
          <div
            ref={docxRef}
            className="material-docx-preview"
            style={{ display: kind === 'docx' ? 'block' : 'none' }}
          />
          {!loading && kind === 'unsupported' && !error ? (
            <div className="material-preview-fallback">
              <Typography.Paragraph>
                当前文件：<Typography.Text code>{fileName || '未知'}</Typography.Text>
              </Typography.Paragraph>
              <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                该格式暂不支持内嵌预览，请使用「下载」本地查看。
              </Typography.Paragraph>
            </div>
          ) : null}
        </div>
      </Spin>
    </div>
  );
}

import { Empty, Spin, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import {
  fetchUrlFileBlob,
  fileNameFromUrl,
  resolveFilePreviewKind,
  type FilePreviewKind,
} from '../api/files';
import { renderSpreadsheetPreview } from '../utils/spreadsheetPreview';

interface SolutionDocumentPreviewProps {
  fileUrl?: string;
  fileName?: string;
  fileType?: string;
}

/** 培训方案查看弹窗内嵌文档预览，对齐 yun_shang VueOfficeDocx/Pdf */
export function SolutionDocumentPreview({ fileUrl, fileName, fileType }: SolutionDocumentPreviewProps) {
  const docxContainerRef = useRef<HTMLDivElement>(null);
  const spreadsheetContainerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(false);
  const [previewKind, setPreviewKind] = useState<FilePreviewKind>('unsupported');
  const [objectUrl, setObjectUrl] = useState<string>();

  useEffect(() => {
    if (!fileUrl) {
      return;
    }

    let cancelled = false;
    let localObjectUrl: string | undefined;
    const resolvedName = fileName || fileNameFromUrl(fileUrl);

    async function renderDocx(blob: Blob) {
      await new Promise<void>((resolve) => {
        requestAnimationFrame(() => resolve());
      });
      const container = docxContainerRef.current;
      if (!container) {
        throw new Error('预览容器未就绪');
      }
      container.innerHTML = '';
      const { renderAsync } = await import('docx-preview');
      await renderAsync(blob, container, undefined, {
        className: 'docx-preview-container',
        inWrapper: true,
        ignoreWidth: false,
        ignoreHeight: false,
      });
    }

    async function loadPreview() {
      setLoading(true);
      setObjectUrl(undefined);
      setPreviewKind('unsupported');
      if (docxContainerRef.current) {
        docxContainerRef.current.innerHTML = '';
      }
      if (spreadsheetContainerRef.current) {
        spreadsheetContainerRef.current.innerHTML = '';
      }

      try {
        const blob = await fetchUrlFileBlob(fileUrl!);
        if (cancelled) {
          return;
        }

        let kind = resolveFilePreviewKind(resolvedName, fileType, blob.type);
        if (kind === 'office') {
          kind = 'docx';
        }
        setPreviewKind(kind);

        if (kind === 'unsupported') {
          return;
        }

        if (kind === 'pdf') {
          localObjectUrl = URL.createObjectURL(blob);
          setObjectUrl(localObjectUrl);
          return;
        }

        if (kind === 'spreadsheet') {
          await new Promise<void>((resolve) => {
            requestAnimationFrame(() => resolve());
          });
          const container = spreadsheetContainerRef.current;
          if (!container) {
            throw new Error('预览容器未就绪');
          }
          await renderSpreadsheetPreview(blob, container);
          return;
        }

        if (kind === 'docx') {
          await renderDocx(blob);
        }
      } catch (error) {
        if (!cancelled) {
          message.error(error instanceof Error ? error.message : '文档预览失败');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadPreview();

    return () => {
      cancelled = true;
      if (localObjectUrl) {
        URL.revokeObjectURL(localObjectUrl);
      }
    };
  }, [fileUrl, fileName, fileType]);

  if (!fileUrl) {
    return null;
  }

  return (
    <div className="solution-doc-preview">
      <Spin spinning={loading}>
        {previewKind === 'pdf' && objectUrl ? (
          <iframe title="PDF预览" src={objectUrl} className="solution-doc-preview__iframe" />
        ) : null}
        <div
          ref={spreadsheetContainerRef}
          className="solution-doc-preview__spreadsheet"
          style={{ display: previewKind === 'spreadsheet' ? 'block' : 'none' }}
        />
        <div
          ref={docxContainerRef}
          className="solution-doc-preview__docx"
          style={{ display: previewKind === 'docx' ? 'block' : 'none' }}
        />
        {!loading && previewKind === 'unsupported' ? (
          <Empty description="暂不支持在线预览该文件类型，请点击上方链接下载查看" />
        ) : null}
      </Spin>
    </div>
  );
}

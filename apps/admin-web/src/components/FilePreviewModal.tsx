import { DownloadOutlined } from '@ant-design/icons';
import { Button, Empty, Image, Modal, Spin, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import {
  buildOfficeOnlinePreviewUrl,
  fetchCandidateDocumentBlob,
  fetchPlatformFileBlob,
  fetchUrlFileBlob,
  fileNameFromUrl,
  resolveFilePreviewKind,
  shouldUseOfficeOnlinePreview,
  type FilePreviewKind,
} from '../api/files';
import { renderSpreadsheetPreview } from '../utils/spreadsheetPreview';

export interface FilePreviewTarget {
  fileId?: string;
  /** CDN / 静态文件绝对或相对 URL（自主报名方案附件等） */
  fileUrl?: string;
  fileName?: string;
  fileType?: string;
  taskId?: string;
  documentId?: string;
}

interface FilePreviewModalProps {
  open: boolean;
  target: FilePreviewTarget | null;
  onClose: () => void;
}

export function FilePreviewModal({ open, target, onClose }: FilePreviewModalProps) {
  const docxContainerRef = useRef<HTMLDivElement>(null);
  const spreadsheetContainerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(false);
  const [previewKind, setPreviewKind] = useState<FilePreviewKind>('unsupported');
  const [objectUrl, setObjectUrl] = useState<string>();
  const [officePreviewUrl, setOfficePreviewUrl] = useState<string>();
  const [downloadBlob, setDownloadBlob] = useState<Blob>();

  useEffect(() => {
    const previewTarget = target;
    if (
      !open
      || !previewTarget
      || (
        !previewTarget.fileId
        && !previewTarget.fileUrl
        && !(previewTarget.taskId && previewTarget.documentId)
      )
    ) {
      return;
    }
    const fileName = previewTarget.fileName || fileNameFromUrl(previewTarget.fileUrl);
    const fileType = previewTarget.fileType;
    const taskId = previewTarget.taskId;
    const documentId = previewTarget.documentId;
    const fileId = previewTarget.fileId;
    const fileUrl = previewTarget.fileUrl;

    let cancelled = false;
    let localObjectUrl: string | undefined;

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
      setOfficePreviewUrl(undefined);
      setDownloadBlob(undefined);
      setPreviewKind('unsupported');
      if (docxContainerRef.current) {
        docxContainerRef.current.innerHTML = '';
      }
      if (spreadsheetContainerRef.current) {
        spreadsheetContainerRef.current.innerHTML = '';
      }

      try {
        const blob = fileUrl
          ? await fetchUrlFileBlob(fileUrl)
          : taskId && documentId
            ? await fetchCandidateDocumentBlob(taskId, documentId)
            : await fetchPlatformFileBlob(fileId!);
        if (cancelled) {
          return;
        }
        setDownloadBlob(blob);

        const kind = resolveFilePreviewKind(fileName, fileType, blob.type);

        if (kind === 'spreadsheet') {
          setPreviewKind('spreadsheet');
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
          setPreviewKind('docx');
          await renderDocx(blob);
          return;
        }

        if (kind === 'image' || kind === 'pdf') {
          localObjectUrl = URL.createObjectURL(blob);
          setObjectUrl(localObjectUrl);
          setPreviewKind(kind);
          return;
        }

        if (
          kind === 'office'
          && fileUrl
          && shouldUseOfficeOnlinePreview(fileUrl, fileName, fileType, blob.type)
        ) {
          setPreviewKind('office');
          setOfficePreviewUrl(buildOfficeOnlinePreviewUrl(fileUrl));
          return;
        }

        if (kind === 'unsupported') {
          return;
        }
      } catch (error) {
        if (!cancelled) {
          message.error(error instanceof Error ? error.message : '文件预览失败');
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
  }, [open, target?.fileId, target?.fileUrl, target?.fileName, target?.fileType, target?.taskId, target?.documentId]);

  useEffect(() => {
    if (open) {
      return;
    }
    if (objectUrl) {
      URL.revokeObjectURL(objectUrl);
    }
    setObjectUrl(undefined);
    setOfficePreviewUrl(undefined);
    setDownloadBlob(undefined);
    if (docxContainerRef.current) {
      docxContainerRef.current.innerHTML = '';
    }
    if (spreadsheetContainerRef.current) {
      spreadsheetContainerRef.current.innerHTML = '';
    }
  }, [open, objectUrl]);

  function handleDownload() {
    if (!downloadBlob || !target) {
      return;
    }
    const url = URL.createObjectURL(downloadBlob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = target.fileName || 'download';
    anchor.click();
    URL.revokeObjectURL(url);
  }

  const title = target?.fileName ? `预览 — ${target.fileName}` : '文件预览';
  const modalWidth = previewKind === 'image' ? 720 : previewKind === 'office' ? '80%' : 960;

  return (
    <Modal
      title={title}
      open={open}
      onCancel={onClose}
      footer={
        downloadBlob ? (
          <Button icon={<DownloadOutlined />} onClick={handleDownload}>
            下载
          </Button>
        ) : null
      }
      width={modalWidth}
      destroyOnClose
    >
      <Spin spinning={loading}>
        <div style={{ minHeight: 420, maxHeight: '70vh', overflow: 'auto' }}>
          {previewKind === 'office' && officePreviewUrl ? (
            <iframe
              title="Office 在线预览"
              src={officePreviewUrl}
              style={{ width: '100%', height: 600, border: 0 }}
            />
          ) : null}
          {previewKind === 'image' && objectUrl ? (
            <Image src={objectUrl} alt={target?.fileName || '预览'} style={{ maxWidth: '100%' }} />
          ) : null}
          {previewKind === 'pdf' && objectUrl ? (
            <iframe title="PDF 预览" src={objectUrl} style={{ width: '100%', height: '68vh', border: 0 }} />
          ) : null}
          <div
            ref={spreadsheetContainerRef}
            className="spreadsheet-preview-host"
            style={{ display: previewKind === 'spreadsheet' ? 'block' : 'none' }}
          />
          <div
            ref={docxContainerRef}
            className="docx-preview-host"
            style={{ display: previewKind === 'docx' ? 'block' : 'none' }}
          />
          {!loading && previewKind === 'unsupported' ? (
            <Empty description="暂不支持在线预览该文件类型，请下载后查看">
              {downloadBlob ? (
                <Button type="primary" icon={<DownloadOutlined />} onClick={handleDownload}>
                  下载文件
                </Button>
              ) : null}
            </Empty>
          ) : null}
          {!loading
            && previewKind !== 'unsupported'
            && previewKind !== 'office'
            && previewKind !== 'spreadsheet'
            && previewKind !== 'docx'
            && !objectUrl ? (
              <Typography.Text type="secondary">暂无预览内容</Typography.Text>
            ) : null}
        </div>
      </Spin>
    </Modal>
  );
}

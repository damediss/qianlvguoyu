import { DownloadOutlined } from '@ant-design/icons';
import { Button, Empty, Image, Modal, Spin, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import {
  fetchCandidateDocumentBlob,
  fetchPlatformFileBlob,
  fetchUrlFileBlob,
  fileNameFromUrl,
  resolveFilePreviewKind,
  type FilePreviewKind,
} from '../api/files';

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
  const [loading, setLoading] = useState(false);
  const [previewKind, setPreviewKind] = useState<FilePreviewKind>('unsupported');
  const [objectUrl, setObjectUrl] = useState<string>();
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

    let cancelled = false;
    let localObjectUrl: string | undefined;

    async function loadPreview() {
      setLoading(true);
      setObjectUrl(undefined);
      setDownloadBlob(undefined);
      if (docxContainerRef.current) {
        docxContainerRef.current.innerHTML = '';
      }

      const kind = resolveFilePreviewKind(fileName, fileType);
      setPreviewKind(kind);

      try {
        const blob = previewTarget.fileUrl
          ? await fetchUrlFileBlob(previewTarget.fileUrl)
          : taskId && documentId
            ? await fetchCandidateDocumentBlob(taskId, documentId)
            : await fetchPlatformFileBlob(fileId!);
        if (cancelled) {
          return;
        }
        setDownloadBlob(blob);

        if (kind === 'image' || kind === 'pdf') {
          localObjectUrl = URL.createObjectURL(blob);
          setObjectUrl(localObjectUrl);
          return;
        }

        if (kind === 'docx') {
          await new Promise<void>((resolve) => {
            requestAnimationFrame(() => resolve());
          });
          const container = docxContainerRef.current;
          if (!container) {
            throw new Error('预览容器未就绪');
          }
          const { renderAsync } = await import('docx-preview');
          await renderAsync(blob, container, undefined, {
            className: 'docx-preview-container',
            inWrapper: true,
            ignoreWidth: false,
            ignoreHeight: false,
          });
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
    setDownloadBlob(undefined);
    if (docxContainerRef.current) {
      docxContainerRef.current.innerHTML = '';
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
      width={previewKind === 'image' ? 720 : 960}
      destroyOnClose
    >
      <Spin spinning={loading}>
        <div style={{ minHeight: 420, maxHeight: '70vh', overflow: 'auto' }}>
          {previewKind === 'image' && objectUrl ? (
            <Image src={objectUrl} alt={target?.fileName || '预览'} style={{ maxWidth: '100%' }} />
          ) : null}
          {previewKind === 'pdf' && objectUrl ? (
            <iframe title="PDF 预览" src={objectUrl} style={{ width: '100%', height: '68vh', border: 0 }} />
          ) : null}
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
          {!loading && previewKind !== 'unsupported' && !objectUrl && previewKind !== 'docx' ? (
            <Typography.Text type="secondary">暂无预览内容</Typography.Text>
          ) : null}
        </div>
      </Spin>
    </Modal>
  );
}

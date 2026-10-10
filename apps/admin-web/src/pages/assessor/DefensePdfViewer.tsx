import { Empty, Spin } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import type { PDFDocumentProxy } from 'pdfjs-dist';
import { fetchPlatformFileBlob } from '../../api/files';
import { installLegacyBrowserPolyfills } from '../../utils/legacyBrowserPolyfills';

interface DefensePdfViewerProps {
  fileId?: string;
}

type PdfjsWorkerGlobal = {
  pdfjsWorker?: { WorkerMessageHandler?: unknown };
};

const MAX_CANVAS_EDGE = 4096;

function toPdfBlob(blob: Blob) {
  if (blob.type === 'application/pdf') {
    return blob;
  }
  return new Blob([blob], { type: 'application/pdf' });
}

function clearNode(node: HTMLElement) {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
}

function measurePdfWidth(host: HTMLElement) {
  const candidates = [
    host.clientWidth,
    host.getBoundingClientRect().width,
    host.parentElement?.clientWidth,
    host.parentElement?.getBoundingClientRect().width,
    Math.min(window.innerWidth - 24, 720),
  ];
  for (const value of candidates) {
    const width = Math.floor(Number(value) || 0);
    if (width >= 24) {
      return width;
    }
  }
  return 360;
}

async function loadPdfjs() {
  installLegacyBrowserPolyfills();
  const [pdfjs, workerModule, workerUrl] = await Promise.all([
    import('pdfjs-dist/legacy/build/pdf.min.js'),
    import('pdfjs-dist/legacy/build/pdf.worker.min.js'),
    import('./pdfjs.worker?worker&url'),
  ]);
  const handler = (workerModule as { WorkerMessageHandler?: unknown }).WorkerMessageHandler
    ?? (globalThis as PdfjsWorkerGlobal).pdfjsWorker?.WorkerMessageHandler;
  if (handler) {
    (globalThis as PdfjsWorkerGlobal).pdfjsWorker = { WorkerMessageHandler: handler };
  }
  pdfjs.GlobalWorkerOptions.workerSrc = workerUrl.default;
  return pdfjs;
}

/** Android / 微信内置浏览器无法在 iframe 中渲染 PDF，改为交给系统阅读器或分享面板。 */
export async function openPdfExternally(blob: Blob, fileName?: string) {
  const pdfBlob = toPdfBlob(blob);
  const name = fileName?.toLowerCase().endsWith('.pdf')
    ? fileName
    : `${fileName || '业绩评估报告'}.pdf`;
  const file = new File([pdfBlob], name, { type: 'application/pdf' });
  const shareData: ShareData = { files: [file], title: name };
  try {
    if (typeof navigator.canShare === 'function' && navigator.canShare(shareData) && navigator.share) {
      await navigator.share(shareData);
      return;
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      return;
    }
  }

  const url = URL.createObjectURL(pdfBlob);
  const opened = window.open(url, '_blank', 'noopener,noreferrer');
  if (!opened) {
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = name;
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  }
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export function DefensePdfViewer({ fileId }: DefensePdfViewerProps) {
  const query = useQuery({
    queryKey: ['defense-pdf-blob', fileId],
    queryFn: () => fetchPlatformFileBlob(fileId!),
    enabled: Boolean(fileId),
  });

  if (!fileId || query.isLoading) {
    return <Spin className="defense-phone-spin" />;
  }
  if (!query.data) {
    return <Empty description={query.error instanceof Error ? query.error.message : '报告加载失败'} />;
  }

  return (
    <div className="defense-pdf-wrap">
      <PdfJsPages blob={query.data} />
    </div>
  );
}

function PdfJsPages({ blob }: { blob: Blob }) {
  const hostRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    const host = hostRef.current;
    if (!host) {
      return;
    }

    let cancelled = false;
    let pdf: PDFDocumentProxy | undefined;
    let timer = 0;
    let lastWidth = 0;
    let generation = 0;
    let widthTries = 0;

    async function renderPages(width: number, token: number) {
      if (!host || cancelled) {
        return;
      }
      setLoading(true);
      setError(undefined);
      const pdfjs = await loadPdfjs();
      if (!pdf) {
        const data = new Uint8Array(await blob.arrayBuffer());
        pdf = await pdfjs.getDocument({ data }).promise;
      }
      const documentProxy = pdf;
      if (!documentProxy || cancelled || token !== generation) {
        return;
      }

      clearNode(host);
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      for (let pageNumber = 1; pageNumber <= documentProxy.numPages; pageNumber += 1) {
        if (cancelled || token !== generation) {
          return;
        }
        const page = await documentProxy.getPage(pageNumber);
        const base = page.getViewport({ scale: 1 });
        let cssScale = width / base.width;
        let viewport = page.getViewport({ scale: cssScale });
        const pixelWidth = viewport.width * dpr;
        const pixelHeight = viewport.height * dpr;
        const edge = Math.max(pixelWidth, pixelHeight);
        if (edge > MAX_CANVAS_EDGE) {
          cssScale *= MAX_CANVAS_EDGE / edge;
          viewport = page.getViewport({ scale: cssScale });
        }
        const drawViewport = page.getViewport({ scale: cssScale * dpr });
        const canvas = document.createElement('canvas');
        canvas.className = 'defense-pdf-page';
        canvas.width = Math.max(1, Math.floor(drawViewport.width));
        canvas.height = Math.max(1, Math.floor(drawViewport.height));
        canvas.style.width = '100%';
        canvas.style.height = `${Math.max(1, Math.floor(viewport.height))}px`;
        const context = canvas.getContext('2d', { alpha: false });
        if (!context) {
          continue;
        }
        host.appendChild(canvas);
        try {
          await page.render({
            canvasContext: context,
            viewport: drawViewport,
          }).promise;
        } catch (cause) {
          canvas.remove();
          if (!host.firstChild) {
            throw cause;
          }
        }
        if (pageNumber === 1) {
          setLoading(false);
        }
      }
      if (cancelled || token !== generation) {
        return;
      }
      if (!host.firstChild) {
        setError('PDF 页面渲染为空，请改用外部阅读器');
      }
      setLoading(false);
    }

    function schedule(width: number) {
      if (width < 24) {
        if (widthTries < 20 && !cancelled) {
          widthTries += 1;
          window.clearTimeout(timer);
          timer = window.setTimeout(() => schedule(measurePdfWidth(host!)), 50);
          return;
        }
        width = 360;
      }
      if (Math.abs(width - lastWidth) < 8 && lastWidth >= 24) {
        return;
      }
      lastWidth = width;
      generation += 1;
      const token = generation;
      window.clearTimeout(timer);
      timer = window.setTimeout(() => {
        void renderPages(width, token).catch((cause) => {
          if (cancelled || token !== generation) {
            return;
          }
          setLoading(false);
          setError(cause instanceof Error ? cause.message : 'PDF 渲染失败');
        });
      }, 80);
    }

    const observer = new ResizeObserver((entries) => {
      schedule(Math.floor(entries[0]?.contentRect.width || measurePdfWidth(host)));
    });
    observer.observe(host);
    schedule(measurePdfWidth(host));

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      observer.disconnect();
      void pdf?.destroy();
      clearNode(host);
    };
  }, [blob]);

  return (
    <div className="defense-pdf-pages-wrap">
      {loading ? <Spin className="defense-pdf-loading" /> : null}
      {error ? <Empty description={error} /> : null}
      <div ref={hostRef} className="defense-pdf-pages" />
    </div>
  );
}

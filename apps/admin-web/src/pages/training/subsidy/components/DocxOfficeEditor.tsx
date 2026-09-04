import { useEffect, useRef, useState } from 'react';
import { Alert, Spin, Typography } from 'antd';
import type { SubsidyMaterialOfficeConfig } from '../../../../api/training';
import * as trainApi from '../../../../api/training';

type Props = {
  instanceId: string | number;
  /** 版本变化时强制重建编辑器（document key 会变） */
  versionKey?: string | number | null;
  onSaved?: () => void;
};

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (id: string, config: Record<string, unknown>) => { destroyEditor?: () => void };
    };
  }
}

const SCRIPT_ATTR = 'data-onlyoffice-api';

function loadApiScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[${SCRIPT_ATTR}="1"]`);
    if (existing) {
      if (window.DocsAPI?.DocEditor) {
        resolve();
        return;
      }
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', () => reject(new Error('OnlyOffice api.js 加载失败')));
      return;
    }
    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.setAttribute(SCRIPT_ATTR, '1');
    script.onload = () => resolve();
    script.onerror = () => reject(new Error(`无法加载 OnlyOffice 脚本：${src}`));
    document.head.appendChild(script);
  });
}

export function DocxOfficeEditor({ instanceId, versionKey, onSaved }: Props) {
  const placeholderId = `oo-editor-${String(instanceId).replace(/\W/g, '')}`;
  const editorRef = useRef<{ destroyEditor?: () => void } | null>(null);
  const onSavedRef = useRef(onSaved);
  onSavedRef.current = onSaved;
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [meta, setMeta] = useState<Pick<SubsidyMaterialOfficeConfig, 'documentServerUrl'> | null>(null);

  useEffect(() => {
    let cancelled = false;

    const boot = async () => {
      setLoading(true);
      setError(null);
      try {
        const cfg = await trainApi.getSubsidyMaterialOfficeConfig(instanceId);
        if (cancelled) return;
        if (!cfg.enabled || !cfg.config || !cfg.apiScriptUrl) {
          setError(cfg.message || 'OnlyOffice 未启用或未配置');
          setLoading(false);
          return;
        }
        setMeta({ documentServerUrl: cfg.documentServerUrl });
        await loadApiScript(cfg.apiScriptUrl);
        if (cancelled) return;
        if (!window.DocsAPI?.DocEditor) {
          throw new Error('DocsAPI.DocEditor 未就绪，请检查 Document Server 是否可访问');
        }

        if (editorRef.current?.destroyEditor) {
          try {
            editorRef.current.destroyEditor();
          } catch {
            /* ignore */
          }
          editorRef.current = null;
        }

        const host = document.getElementById(placeholderId);
        if (host) {
          host.innerHTML = '';
        }

        const editorConfig: Record<string, unknown> = {
          ...cfg.config,
          events: {
            onDocumentStateChange: (event: { data?: boolean }) => {
              if (event?.data === false) {
                onSavedRef.current?.();
              }
            },
            onError: (event: { data?: string }) => {
              setError(event?.data || 'OnlyOffice 编辑器错误');
            },
          },
        };

        editorRef.current = new window.DocsAPI.DocEditor(placeholderId, editorConfig);
        setLoading(false);
      } catch (e) {
        if (!cancelled) {
          setError((e as Error).message || '打开在线编辑失败');
          setLoading(false);
        }
      }
    };

    void boot();

    return () => {
      cancelled = true;
      if (editorRef.current?.destroyEditor) {
        try {
          editorRef.current.destroyEditor();
        } catch {
          /* ignore */
        }
        editorRef.current = null;
      }
    };
  }, [instanceId, versionKey, placeholderId]);

  if (error) {
    return (
      <Alert
        type="warning"
        showIcon
        message="无法打开在线编辑"
        description={
          <div>
            <div>{error}</div>
            <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
              请确认已启动 OnlyOffice Document Server，并在后端配置
              <Typography.Text code>ONLYOFFICE_ENABLED=true</Typography.Text>、
              <Typography.Text code>ONLYOFFICE_DS_URL</Typography.Text>、
              <Typography.Text code>ONLYOFFICE_PUBLIC_BASE_URL</Typography.Text>、
              <Typography.Text code>ONLYOFFICE_JWT_SECRET</Typography.Text>（与 DS 一致）。
              {meta?.documentServerUrl ? (
                <>
                  {' '}
                  当前 DS：<Typography.Text code>{meta.documentServerUrl}</Typography.Text>
                </>
              ) : null}
            </Typography.Paragraph>
          </div>
        }
      />
    );
  }

  return (
    <div className="material-office-editor-wrap">
      {loading ? (
        <div className="material-office-editor-loading">
          <Spin tip="正在加载 OnlyOffice…" />
        </div>
      ) : null}
      <div id={placeholderId} className="material-office-editor-host" />
    </div>
  );
}

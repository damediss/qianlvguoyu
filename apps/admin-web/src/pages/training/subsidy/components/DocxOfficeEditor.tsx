import { useEffect, useRef, useState } from 'react';
import { Alert, Spin, Typography, message } from 'antd';
import type { SubsidyMaterialOfficeConfig } from '../../../../api/training';
import * as trainApi from '../../../../api/training';

type Props = {
  instanceId: string | number;
  /** 打开时的版本标识；仅用于首次挂载，保存后不因父级刷新而重建编辑器 */
  versionKey?: string | number | null;
  /** 服务端已落库新版本后回调（可 async，用于刷新列表/预览） */
  onSaved?: () => void | Promise<void>;
  /** OnlyOffice 未启用/不可用时回调，由上层回退到字段编辑 */
  onUnavailable?: (reason: string) => void;
};

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function documentKeyOf(cfg: SubsidyMaterialOfficeConfig): string {
  const doc = cfg.config?.document as { key?: string } | undefined;
  return doc?.key ? String(doc.key) : '';
}

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

export function DocxOfficeEditor({ instanceId, versionKey: _versionKey, onSaved, onUnavailable }: Props) {
  const placeholderId = `oo-editor-${String(instanceId).replace(/\W/g, '')}`;
  const editorRef = useRef<{ destroyEditor?: () => void } | null>(null);
  const onSavedRef = useRef(onSaved);
  onSavedRef.current = onSaved;
  const onUnavailableRef = useRef(onUnavailable);
  onUnavailableRef.current = onUnavailable;
  /** 本次打开时的 document.key；服务端回调落库后 key 会变 */
  const openedKeyRef = useRef<string>('');
  const everDirtyRef = useRef(false);
  const waitingSaveRef = useRef(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [meta, setMeta] = useState<Pick<SubsidyMaterialOfficeConfig, 'documentServerUrl'> | null>(null);

  useEffect(() => {
    let cancelled = false;
    everDirtyRef.current = false;
    waitingSaveRef.current = false;
    openedKeyRef.current = '';

    const notifySaved = async () => {
      try {
        await onSavedRef.current?.();
      } catch {
        /* 刷新失败不打断编辑 */
      }
    };

    /** Document Server 本地已干净 ≠ 平台回调已写完；轮询到新 version key 再通知上层 */
    const waitForPersistedVersion = async () => {
      if (waitingSaveRef.current || cancelled) return;
      waitingSaveRef.current = true;
      const baseline = openedKeyRef.current;
      try {
        for (let i = 0; i < 24; i++) {
          await sleep(500);
          if (cancelled) return;
          try {
            const latest = await trainApi.getSubsidyMaterialOfficeConfig(instanceId);
            const key = documentKeyOf(latest);
            if (key && baseline && key !== baseline) {
              openedKeyRef.current = key;
              message.success('已保存到服务器');
              await notifySaved();
              return;
            }
          } catch {
            /* 轮询中偶发失败则继续 */
          }
        }
        // 超时仍刷新一次，避免回调慢时界面一直旧数据
        await notifySaved();
      } finally {
        waitingSaveRef.current = false;
      }
    };

    const boot = async () => {
      setLoading(true);
      setError(null);
      try {
        const cfg = await trainApi.getSubsidyMaterialOfficeConfig(instanceId);
        if (cancelled) return;
        if (!cfg.enabled || !cfg.config || !cfg.apiScriptUrl) {
          const reason = cfg.message || 'OnlyOffice 未启用或未配置';
          onUnavailableRef.current?.(reason);
          setError(reason);
          setLoading(false);
          return;
        }
        openedKeyRef.current = documentKeyOf(cfg);
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

        // DocsAPI 会把占位节点替换为 iframe；height 用像素，避免父级仅 min-height 时 100% 塌成 ~150px
        const hostEl = document.getElementById(placeholderId);
        const hostHeight = Math.max(hostEl?.clientHeight || 0, 560);
        const editorConfig: Record<string, unknown> = {
          ...cfg.config,
          width: '100%',
          height: `${hostHeight}px`,
          events: {
            onDocumentStateChange: (event: { data?: boolean }) => {
              if (event?.data === true) {
                everDirtyRef.current = true;
                return;
              }
              // 仅在「曾经脏过 → 变干净」时等待服务端落库；忽略首次打开的 false
              if (event?.data === false && everDirtyRef.current) {
                void waitForPersistedVersion();
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
          const reason = (e as Error).message || '打开在线编辑失败';
          onUnavailableRef.current?.(reason);
          setError(reason);
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
    // 刻意不依赖 versionKey：保存后父级刷新 currentVersionId 不应拆掉正在编辑的 iframe。
    // 换材料时 instanceId 会变；同一材料重新进入会整组件卸载重挂。
  }, [instanceId, placeholderId]);

  if (error) {
    return (
      <Alert
        type="info"
        showIcon
        message="完整排版编辑不可用"
        description={
          <div>
            <div>{error}</div>
            <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
              本环境未部署 OnlyOffice 时，请使用「字段编辑」改申请表内容，或下载后替换上传。
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

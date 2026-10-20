import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useRef, useState } from 'react';
import { Alert, Spin, Typography } from 'antd';
import * as trainApi from '../../../../api/training';
const SCRIPT_ATTR = 'data-onlyoffice-api';
function loadApiScript(src) {
    return new Promise((resolve, reject) => {
        const existing = document.querySelector(`script[${SCRIPT_ATTR}="1"]`);
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
export function DocxOfficeEditor({ instanceId, versionKey, onSaved, onUnavailable }) {
    const placeholderId = `oo-editor-${String(instanceId).replace(/\W/g, '')}`;
    const editorRef = useRef(null);
    const onSavedRef = useRef(onSaved);
    onSavedRef.current = onSaved;
    const onUnavailableRef = useRef(onUnavailable);
    onUnavailableRef.current = onUnavailable;
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [meta, setMeta] = useState(null);
    useEffect(() => {
        let cancelled = false;
        const boot = async () => {
            setLoading(true);
            setError(null);
            try {
                const cfg = await trainApi.getSubsidyMaterialOfficeConfig(instanceId);
                if (cancelled)
                    return;
                if (!cfg.enabled || !cfg.config || !cfg.apiScriptUrl) {
                    const reason = cfg.message || 'OnlyOffice 未启用或未配置';
                    onUnavailableRef.current?.(reason);
                    setError(reason);
                    setLoading(false);
                    return;
                }
                setMeta({ documentServerUrl: cfg.documentServerUrl });
                await loadApiScript(cfg.apiScriptUrl);
                if (cancelled)
                    return;
                if (!window.DocsAPI?.DocEditor) {
                    throw new Error('DocsAPI.DocEditor 未就绪，请检查 Document Server 是否可访问');
                }
                if (editorRef.current?.destroyEditor) {
                    try {
                        editorRef.current.destroyEditor();
                    }
                    catch {
                        /* ignore */
                    }
                    editorRef.current = null;
                }
                const host = document.getElementById(placeholderId);
                if (host) {
                    host.innerHTML = '';
                }
                const editorConfig = {
                    ...cfg.config,
                    events: {
                        onDocumentStateChange: (event) => {
                            if (event?.data === false) {
                                onSavedRef.current?.();
                            }
                        },
                        onError: (event) => {
                            setError(event?.data || 'OnlyOffice 编辑器错误');
                        },
                    },
                };
                editorRef.current = new window.DocsAPI.DocEditor(placeholderId, editorConfig);
                setLoading(false);
            }
            catch (e) {
                if (!cancelled) {
                    const reason = e.message || '打开在线编辑失败';
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
                }
                catch {
                    /* ignore */
                }
                editorRef.current = null;
            }
        };
    }, [instanceId, versionKey, placeholderId]);
    if (error) {
        return (_jsx(Alert, { type: "info", showIcon: true, message: "\u5B8C\u6574\u6392\u7248\u7F16\u8F91\u4E0D\u53EF\u7528", description: _jsxs("div", { children: [_jsx("div", { children: error }), _jsxs(Typography.Paragraph, { type: "secondary", style: { marginTop: 8, marginBottom: 0, fontSize: 12 }, children: ["\u672C\u73AF\u5883\u672A\u90E8\u7F72 OnlyOffice \u65F6\uFF0C\u8BF7\u4F7F\u7528\u300C\u5B57\u6BB5\u7F16\u8F91\u300D\u6539\u7533\u8BF7\u8868\u5185\u5BB9\uFF0C\u6216\u4E0B\u8F7D\u540E\u66FF\u6362\u4E0A\u4F20\u3002", meta?.documentServerUrl ? (_jsxs(_Fragment, { children: [' ', "\u5F53\u524D DS\uFF1A", _jsx(Typography.Text, { code: true, children: meta.documentServerUrl })] })) : null] })] }) }));
    }
    return (_jsxs("div", { className: "material-office-editor-wrap", children: [loading ? (_jsx("div", { className: "material-office-editor-loading", children: _jsx(Spin, { tip: "\u6B63\u5728\u52A0\u8F7D OnlyOffice\u2026" }) })) : null, _jsx("div", { id: placeholderId, className: "material-office-editor-host" })] }));
}

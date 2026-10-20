import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Empty, Spin, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { fetchUrlFileBlob, fileNameFromUrl, resolveFilePreviewKind, } from '../api/files';
import { renderSpreadsheetPreview } from '../utils/spreadsheetPreview';
/** 培训方案查看弹窗内嵌文档预览，对齐 yun_shang VueOfficeDocx/Pdf */
export function SolutionDocumentPreview({ fileUrl, fileName, fileType }) {
    const docxContainerRef = useRef(null);
    const spreadsheetContainerRef = useRef(null);
    const [loading, setLoading] = useState(false);
    const [previewKind, setPreviewKind] = useState('unsupported');
    const [objectUrl, setObjectUrl] = useState();
    useEffect(() => {
        if (!fileUrl) {
            return;
        }
        let cancelled = false;
        let localObjectUrl;
        const resolvedName = fileName || fileNameFromUrl(fileUrl);
        async function renderDocx(blob) {
            await new Promise((resolve) => {
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
                const blob = await fetchUrlFileBlob(fileUrl);
                if (cancelled) {
                    return;
                }
                let kind = resolveFilePreviewKind(resolvedName, fileType, blob.type);
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
                    await new Promise((resolve) => {
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
            }
            catch (error) {
                if (!cancelled) {
                    message.error(error instanceof Error ? error.message : '文档预览失败');
                }
            }
            finally {
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
    return (_jsx("div", { className: "solution-doc-preview", children: _jsxs(Spin, { spinning: loading, children: [previewKind === 'pdf' && objectUrl ? (_jsx("iframe", { title: "PDF\u9884\u89C8", src: objectUrl, className: "solution-doc-preview__iframe" })) : null, _jsx("div", { ref: spreadsheetContainerRef, className: "solution-doc-preview__spreadsheet", style: { display: previewKind === 'spreadsheet' ? 'block' : 'none' } }), _jsx("div", { ref: docxContainerRef, className: "solution-doc-preview__docx", style: { display: previewKind === 'docx' ? 'block' : 'none' } }), !loading && previewKind === 'unsupported' ? (_jsx(Empty, { description: "\u6682\u4E0D\u652F\u6301\u5728\u7EBF\u9884\u89C8\u8BE5\u6587\u4EF6\u7C7B\u578B\uFF0C\u8BF7\u70B9\u51FB\u4E0A\u65B9\u94FE\u63A5\u4E0B\u8F7D\u67E5\u770B" })) : null] }) }));
}

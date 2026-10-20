import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useRef, useState } from 'react';
import { Alert, Empty, Image, Spin, Typography } from 'antd';
import { fetchPlatformFileBlob, fetchUrlFileBlob, resolveFilePreviewKind, } from '../../../../api/files';
import { renderSpreadsheetPreview } from '../../../../utils/spreadsheetPreview';
/**
 * 补贴材料工作台内嵌预览：docx（docx-preview）/ xlsx（SheetJS）/ pdf / 图片。
 * 不依赖 OnlyOffice，私有 COS 走鉴权下载。
 */
export function MaterialFilePreview({ fileId, filePath, fileName }) {
    const docxRef = useRef(null);
    const sheetRef = useRef(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [kind, setKind] = useState('unsupported');
    const [objectUrl, setObjectUrl] = useState();
    useEffect(() => {
        if (!fileId && !filePath) {
            setKind('unsupported');
            setError(null);
            return;
        }
        let cancelled = false;
        let localUrl;
        async function load() {
            setLoading(true);
            setError(null);
            setKind('unsupported');
            setObjectUrl(undefined);
            if (docxRef.current)
                docxRef.current.innerHTML = '';
            if (sheetRef.current)
                sheetRef.current.innerHTML = '';
            try {
                const blob = filePath
                    ? await fetchUrlFileBlob(filePath)
                    : await fetchPlatformFileBlob(String(fileId));
                if (cancelled)
                    return;
                const resolved = resolveFilePreviewKind(fileName || '', undefined, blob.type);
                if (resolved === 'spreadsheet') {
                    setKind('spreadsheet');
                    await new Promise((r) => requestAnimationFrame(() => r()));
                    if (cancelled || !sheetRef.current)
                        return;
                    await renderSpreadsheetPreview(blob, sheetRef.current);
                    return;
                }
                if (resolved === 'docx') {
                    setKind('docx');
                    await new Promise((r) => requestAnimationFrame(() => r()));
                    if (cancelled || !docxRef.current)
                        return;
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
            }
            catch (e) {
                if (!cancelled) {
                    setError(e.message || '预览失败');
                }
            }
            finally {
                if (!cancelled)
                    setLoading(false);
            }
        }
        void load();
        return () => {
            cancelled = true;
            if (localUrl)
                URL.revokeObjectURL(localUrl);
        };
    }, [fileId, filePath, fileName]);
    if (!fileId && !filePath) {
        return _jsx(Empty, { description: "\u6682\u65E0\u6587\u4EF6\u53EF\u9884\u89C8" });
    }
    if (error) {
        return _jsx(Alert, { type: "warning", showIcon: true, message: "\u9884\u89C8\u5931\u8D25", description: error });
    }
    return (_jsx("div", { className: "material-file-preview", children: _jsx(Spin, { spinning: loading, tip: "\u52A0\u8F7D\u9884\u89C8\u2026", children: _jsxs("div", { className: "material-file-preview-body", children: [kind === 'image' && objectUrl ? (_jsx(Image, { src: objectUrl, alt: fileName || 'preview', className: "material-preview-image" })) : null, kind === 'pdf' && objectUrl ? (_jsx("iframe", { title: "pdf-preview", className: "material-preview-frame", src: objectUrl })) : null, _jsx("div", { ref: sheetRef, className: "material-spreadsheet-preview", style: { display: kind === 'spreadsheet' ? 'block' : 'none' } }), _jsx("div", { ref: docxRef, className: "material-docx-preview", style: { display: kind === 'docx' ? 'block' : 'none' } }), !loading && kind === 'unsupported' && !error ? (_jsxs("div", { className: "material-preview-fallback", children: [_jsxs(Typography.Paragraph, { children: ["\u5F53\u524D\u6587\u4EF6\uFF1A", _jsx(Typography.Text, { code: true, children: fileName || '未知' })] }), _jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 0 }, children: "\u8BE5\u683C\u5F0F\u6682\u4E0D\u652F\u6301\u5185\u5D4C\u9884\u89C8\uFF0C\u8BF7\u4F7F\u7528\u300C\u4E0B\u8F7D\u300D\u672C\u5730\u67E5\u770B\u3002" })] })) : null] }) }) }));
}

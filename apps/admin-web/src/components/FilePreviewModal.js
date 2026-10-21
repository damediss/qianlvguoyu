import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DownloadOutlined } from '@ant-design/icons';
import { Button, Empty, Image, Modal, Spin, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { buildOfficeOnlinePreviewUrl, fetchCandidateDocumentBlob, fetchPlatformFileBlob, fetchUrlFileBlob, fileNameFromUrl, resolveFilePreviewKind, shouldUseOfficeOnlinePreview, } from '../api/files';
import { renderSpreadsheetPreview } from '../utils/spreadsheetPreview';
export function FilePreviewModal({ open, target, onClose }) {
    const docxContainerRef = useRef(null);
    const spreadsheetContainerRef = useRef(null);
    const [loading, setLoading] = useState(false);
    const [previewKind, setPreviewKind] = useState('unsupported');
    const [objectUrl, setObjectUrl] = useState();
    const [officePreviewUrl, setOfficePreviewUrl] = useState();
    const [downloadBlob, setDownloadBlob] = useState();
    useEffect(() => {
        const previewTarget = target;
        if (!open
            || !previewTarget
            || (!previewTarget.fileId
                && !previewTarget.fileUrl
                && !(previewTarget.taskId && previewTarget.documentId))) {
            return;
        }
        const fileName = previewTarget.fileName || fileNameFromUrl(previewTarget.fileUrl);
        const fileType = previewTarget.fileType;
        const taskId = previewTarget.taskId;
        const documentId = previewTarget.documentId;
        const fileId = previewTarget.fileId;
        const fileUrl = previewTarget.fileUrl;
        let cancelled = false;
        let localObjectUrl;
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
                let blob;
                if (fileUrl) {
                    blob = await fetchUrlFileBlob(fileUrl);
                }
                else if (taskId && documentId) {
                    blob = await fetchCandidateDocumentBlob(taskId, documentId);
                }
                else if (fileId) {
                    blob = await fetchPlatformFileBlob(fileId);
                }
                else {
                    throw new Error('缺少文件标识');
                }
                if (cancelled) {
                    return;
                }
                setDownloadBlob(blob);
                const kind = resolveFilePreviewKind(fileName, fileType, blob.type);
                if (kind === 'spreadsheet') {
                    setPreviewKind('spreadsheet');
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
                if (kind === 'office'
                    && fileUrl
                    && shouldUseOfficeOnlinePreview(fileUrl, fileName, fileType, blob.type)) {
                    setPreviewKind('office');
                    setOfficePreviewUrl(buildOfficeOnlinePreviewUrl(fileUrl));
                    return;
                }
                if (kind === 'unsupported') {
                    return;
                }
            }
            catch (error) {
                if (!cancelled) {
                    message.error(error instanceof Error ? error.message : '文件预览失败');
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
    return (_jsx(Modal, { title: title, open: open, onCancel: onClose, footer: downloadBlob ? (_jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: handleDownload, children: "\u4E0B\u8F7D" })) : null, width: modalWidth, destroyOnHidden: true, children: _jsx(Spin, { spinning: loading, children: _jsxs("div", { style: { minHeight: 420, maxHeight: '70vh', overflow: 'auto' }, children: [previewKind === 'office' && officePreviewUrl ? (_jsx("iframe", { title: "Office \u5728\u7EBF\u9884\u89C8", src: officePreviewUrl, style: { width: '100%', height: 600, border: 0 } })) : null, previewKind === 'image' && objectUrl ? (_jsx(Image, { src: objectUrl, alt: target?.fileName || '预览', style: { maxWidth: '100%' } })) : null, previewKind === 'pdf' && objectUrl ? (_jsx("iframe", { title: "PDF \u9884\u89C8", src: objectUrl, style: { width: '100%', height: '68vh', border: 0 } })) : null, _jsx("div", { ref: spreadsheetContainerRef, className: "spreadsheet-preview-host", style: { display: previewKind === 'spreadsheet' ? 'block' : 'none' } }), _jsx("div", { ref: docxContainerRef, className: "docx-preview-host", style: { display: previewKind === 'docx' ? 'block' : 'none' } }), !loading && previewKind === 'unsupported' ? (_jsx(Empty, { description: "\u6682\u4E0D\u652F\u6301\u5728\u7EBF\u9884\u89C8\u8BE5\u6587\u4EF6\u7C7B\u578B\uFF0C\u8BF7\u4E0B\u8F7D\u540E\u67E5\u770B", children: downloadBlob ? (_jsx(Button, { type: "primary", icon: _jsx(DownloadOutlined, {}), onClick: handleDownload, children: "\u4E0B\u8F7D\u6587\u4EF6" })) : null })) : null, !loading
                        && previewKind !== 'unsupported'
                        && previewKind !== 'office'
                        && previewKind !== 'spreadsheet'
                        && previewKind !== 'docx'
                        && !objectUrl ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u9884\u89C8\u5185\u5BB9" })) : null] }) }) }));
}

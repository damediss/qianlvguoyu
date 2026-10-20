import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, DownloadOutlined, EyeOutlined, FilePdfOutlined, InboxOutlined, } from '@ant-design/icons';
import { Alert, Button, Descriptions, Drawer, Form, Input, InputNumber, Modal, Radio, Space, Spin, Tag, Upload, message } from 'antd';
import { MdEditor, MdPreview } from 'md-editor-rt';
import 'md-editor-rt/lib/style.css';
import MarkdownIt from 'markdown-it';
import sanitizeHtml from 'sanitize-html';
import { useCallback, useEffect, useRef, useState } from 'react';
import { createCompetitionNotice, deleteCompetitionNoticeAsset, getCompetitionNotice, getCompetitionNoticeAsset, updateCompetitionNotice, uploadCompetitionNoticeAsset, } from '../../api/competition';
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const IMAGE_MAX_SIZE = 10 * 1024 * 1024;
const PDF_MAX_SIZE = 50 * 1024 * 1024;
const markdownParser = new MarkdownIt({ html: true });
markdownParser.validateLink = () => true;
const contentTypeMeta = {
    markdown: { label: '图文公告', color: 'blue' },
    pdf: { label: 'PDF 赛事文件', color: 'purple' },
};
function sanitizeNoticeHtml(html) {
    return sanitizeHtml(html, {
        allowedTags: [
            'p', 'br', 'strong', 'em', 's', 'blockquote', 'ul', 'ol', 'li', 'h1', 'h2', 'h3',
            'h4', 'h5', 'h6', 'pre', 'code', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'a', 'img',
        ],
        allowedAttributes: {
            a: ['href', 'title', 'target', 'rel'],
            img: ['src', 'alt', 'title'],
            code: ['class'],
        },
        allowedSchemes: ['http', 'https', 'mailto', 'blob'],
        allowProtocolRelative: false,
        transformTags: {
            img: (tagName, attribs) => ({
                tagName,
                attribs: {
                    ...attribs,
                    src: attribs.src?.startsWith('blob:') ? attribs.src : '',
                },
            }),
            a: (tagName, attribs) => ({
                tagName,
                attribs: {
                    ...attribs,
                    href: /^(https?:|mailto:)/i.test(attribs.href || '') ? attribs.href : '',
                    target: '_blank',
                    rel: 'noopener noreferrer',
                },
            }),
        },
    });
}
function fileSize(size) {
    if (size >= 1024 * 1024)
        return `${(size / 1024 / 1024).toFixed(2)} MB`;
    return `${Math.max(1, Math.ceil(size / 1024))} KB`;
}
function isPdf(file) {
    return file.type === 'application/pdf' && file.name.toLowerCase().endsWith('.pdf');
}
function validateMarkdownSource(value) {
    if (!value)
        return Promise.resolve();
    const collectImageSources = (tokens) => tokens.flatMap((token) => {
        const ownSource = token.type === 'image' ? token.attrGet('src') : null;
        return [...(ownSource ? [ownSource] : []), ...(token.children ? collectImageSources(token.children) : [])];
    });
    const containsRawHtml = (tokens) => tokens.some((token) => token.type === 'html_inline'
        || token.type === 'html_block'
        || Boolean(token.children && containsRawHtml(token.children)));
    const tokens = markdownParser.parse(value, {});
    if (containsRawHtml(tokens)) {
        return Promise.reject(new Error('Markdown 正文不允许使用原始 HTML'));
    }
    const imageSources = collectImageSources(tokens);
    if (imageSources.some((source) => !/^notice-asset:\/\/\d+$/.test(source))) {
        return Promise.reject(new Error('图片必须通过编辑器上传，不允许使用外链、data: 或 javascript: 地址'));
    }
    const containsUnsafeLink = (items) => items.some((token) => {
        const href = token.type === 'link_open' ? token.attrGet('href') : null;
        const normalizedHref = href?.replace(/[\u0000-\u0020\u007f]+/g, '').toLowerCase();
        return normalizedHref?.startsWith('javascript:')
            || normalizedHref?.startsWith('data:')
            || Boolean(token.children && containsUnsafeLink(token.children));
    });
    if (containsUnsafeLink(tokens)) {
        return Promise.reject(new Error('链接不允许使用 javascript: 或 data: 地址'));
    }
    return Promise.resolve();
}
export function CompetitionNoticeEditorDrawer({ eventId, mode, noticeId, onClose, onSaved, }) {
    const [form] = Form.useForm();
    const [draftId, setDraftId] = useState(noticeId);
    const [assets, setAssets] = useState([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [previewing, setPreviewing] = useState(false);
    const [contentType, setContentType] = useState('markdown');
    const [readOnlyMarkdown, setReadOnlyMarkdown] = useState('');
    const objectUrls = useRef(new Map());
    const readOnly = mode === 'view';
    const open = Boolean(mode);
    const sessionGeneration = useRef(0);
    const sessionKeyRef = useRef('');
    const sessionKey = `${open}:${mode || ''}:${noticeId == null ? '' : String(noticeId)}`;
    if (sessionKeyRef.current !== sessionKey) {
        sessionKeyRef.current = sessionKey;
        sessionGeneration.current += 1;
    }
    const revokeObjectUrls = useCallback(() => {
        objectUrls.current.forEach((url) => URL.revokeObjectURL(url));
        objectUrls.current.clear();
    }, []);
    useEffect(() => {
        let cancelled = false;
        const generation = sessionGeneration.current;
        const isStale = () => cancelled || sessionGeneration.current !== generation;
        if (!open) {
            revokeObjectUrls();
            setDraftId(undefined);
            setAssets([]);
            setReadOnlyMarkdown('');
            setLoading(false);
            form.resetFields();
            return () => {
                cancelled = true;
            };
        }
        revokeObjectUrls();
        if (!noticeId) {
            setDraftId(undefined);
            setAssets([]);
            setReadOnlyMarkdown('');
            setContentType('markdown');
            form.resetFields();
            form.setFieldsValue({ contentType: 'markdown', content: '', version: 1 });
            return () => {
                cancelled = true;
            };
        }
        setLoading(true);
        void (async () => {
            const notice = await getCompetitionNotice(noticeId);
            if (isStale())
                return;
            const type = notice.contentType || 'markdown';
            setDraftId(notice.id);
            setAssets(notice.assets || []);
            setContentType(type);
            form.setFieldsValue({
                title: notice.title,
                content: notice.content || '',
                contentType: type,
                version: notice.version,
            });
            if (mode === 'view' && type === 'markdown') {
                const replacements = await Promise.all((notice.assets || [])
                    .filter((asset) => asset.assetType === 'inline_image')
                    .map(async (asset) => {
                    const blob = await getCompetitionNoticeAsset(notice.id, asset.assetId);
                    if (isStale())
                        return null;
                    const url = URL.createObjectURL(blob);
                    objectUrls.current.set(String(asset.assetId), url);
                    return [`notice-asset://${asset.assetId}`, url];
                }));
                if (!isStale()) {
                    setReadOnlyMarkdown(replacements.filter((item) => item !== null).reduce((content, [source, url]) => content.replaceAll(source, url), notice.content || ''));
                }
            }
            else {
                setReadOnlyMarkdown(notice.content || '');
            }
        })().finally(() => {
            if (!isStale())
                setLoading(false);
        });
        return () => {
            cancelled = true;
        };
    }, [form, mode, noticeId, open, revokeObjectUrls]);
    useEffect(() => () => revokeObjectUrls(), [revokeObjectUrls]);
    const payloadFromForm = () => {
        const values = form.getFieldsValue();
        return {
            title: values.title?.trim() || '',
            content: values.content?.trim() || '',
            contentType: values.contentType || contentType,
            version: values.version || 1,
        };
    };
    const ensureDraft = async () => {
        if (draftId)
            return draftId;
        const title = form.getFieldValue('title')?.trim();
        if (!title) {
            await form.validateFields(['title']);
            throw new Error('请先填写公告标题');
        }
        const created = await createCompetitionNotice(eventId, payloadFromForm());
        setDraftId(created.id);
        setAssets(created.assets || []);
        form.setFieldValue('version', created.version);
        message.success('已自动保存草稿');
        onSaved();
        return created.id;
    };
    const uploadImages = async (files, callback) => {
        const invalid = files.find((file) => !IMAGE_TYPES.includes(file.type) || file.size > IMAGE_MAX_SIZE);
        if (invalid) {
            message.error('仅支持不超过 10MB 的 JPG、PNG、WebP 图片');
            return;
        }
        setUploading(true);
        try {
            const id = await ensureDraft();
            const uploaded = await Promise.all(files.map((file) => uploadCompetitionNoticeAsset(id, file, 'inline_image')));
            setAssets((current) => [...current, ...uploaded]);
            callback(uploaded.map((asset) => `notice-asset://${asset.assetId}`));
        }
        finally {
            setUploading(false);
        }
    };
    const resolveAssetUrl = useCallback(async (source) => {
        if (!source.startsWith('notice-asset://') || !draftId)
            return '';
        const generation = sessionGeneration.current;
        const assetId = source.slice('notice-asset://'.length);
        const cached = objectUrls.current.get(assetId);
        if (cached)
            return cached;
        const blob = await getCompetitionNoticeAsset(draftId, assetId);
        if (sessionGeneration.current !== generation)
            return '';
        const url = URL.createObjectURL(blob);
        objectUrls.current.set(assetId, url);
        return url;
    }, [draftId]);
    const removeAsset = async (asset) => {
        if (!draftId)
            return;
        await deleteCompetitionNoticeAsset(draftId, asset.assetId);
        const key = String(asset.assetId);
        const url = objectUrls.current.get(key);
        if (url)
            URL.revokeObjectURL(url);
        objectUrls.current.delete(key);
        setAssets((current) => current.filter((item) => String(item.assetId) !== key));
        if (asset.assetType === 'inline_image') {
            const content = form.getFieldValue('content') || '';
            form.setFieldValue('content', content.replaceAll(`notice-asset://${asset.assetId}`, ''));
        }
        message.success('文件已删除');
    };
    const changeContentType = async (next) => {
        if (next === contentType)
            return;
        const incompatible = assets.filter((asset) => next === 'markdown' ? asset.assetType === 'pdf' : asset.assetType === 'inline_image');
        const currentContent = form.getFieldValue('content') || '';
        const confirmed = await new Promise((resolve) => {
            Modal.confirm({
                title: '切换公告类型？',
                content: incompatible.length > 0
                    ? `切换后将清空当前正文，并由服务端事务删除 ${incompatible.length} 个不兼容文件，且无法恢复。`
                    : currentContent
                        ? '切换后将清空当前正文，且无法恢复。'
                        : '确认切换公告发布类型？',
                okText: '切换并清理',
                cancelText: '取消',
                onOk: () => resolve(true),
                onCancel: () => resolve(false),
            });
        });
        if (!confirmed) {
            form.setFieldValue('contentType', contentType);
            return;
        }
        if (draftId) {
            try {
                const updated = await updateCompetitionNotice(draftId, {
                    ...payloadFromForm(),
                    content: '',
                    contentType: next,
                });
                const retainedIds = new Set((updated.assets || []).map((asset) => String(asset.assetId)));
                assets.forEach((asset) => {
                    const key = String(asset.assetId);
                    if (!retainedIds.has(key)) {
                        const url = objectUrls.current.get(key);
                        if (url)
                            URL.revokeObjectURL(url);
                        objectUrls.current.delete(key);
                    }
                });
                setAssets(updated.assets || []);
                setContentType(updated.contentType || next);
                form.setFieldsValue({
                    title: updated.title,
                    content: updated.content || '',
                    contentType: updated.contentType || next,
                    version: updated.version,
                });
                onSaved();
            }
            catch {
                form.setFieldsValue({ content: currentContent, contentType });
            }
            return;
        }
        form.setFieldValue('content', '');
        setContentType(next);
    };
    const uploadPdf = async (file) => {
        if (!isPdf(file) || file.size > PDF_MAX_SIZE) {
            message.error('仅支持不超过 50MB 的 PDF 文件');
            return Upload.LIST_IGNORE;
        }
        setUploading(true);
        try {
            const id = await ensureDraft();
            const oldPdf = assets.find((asset) => asset.assetType === 'pdf');
            const uploaded = await uploadCompetitionNoticeAsset(id, file, 'pdf');
            setAssets((current) => [...current.filter((asset) => asset.assetType !== 'pdf'), uploaded]);
            message.success(oldPdf ? 'PDF 已替换' : 'PDF 上传成功');
        }
        finally {
            setUploading(false);
        }
        return false;
    };
    const openPdf = async (download) => {
        const pdf = assets.find((asset) => asset.assetType === 'pdf');
        if (!draftId || !pdf)
            return;
        setPreviewing(true);
        const generation = sessionGeneration.current;
        try {
            const blob = await getCompetitionNoticeAsset(draftId, pdf.assetId);
            if (sessionGeneration.current !== generation)
                return;
            const url = URL.createObjectURL(blob);
            const urlKey = `pdf-preview-${Date.now()}`;
            objectUrls.current.set(urlKey, url);
            const revoke = () => {
                if (objectUrls.current.get(urlKey) === url) {
                    URL.revokeObjectURL(url);
                    objectUrls.current.delete(urlKey);
                }
            };
            if (download) {
                const anchor = document.createElement('a');
                anchor.href = url;
                anchor.download = pdf.originalName || 'competition-notice.pdf';
                anchor.click();
                setTimeout(revoke, 1000);
            }
            else {
                window.open(url, '_blank', 'noopener,noreferrer');
                setTimeout(revoke, 60000);
            }
        }
        finally {
            setPreviewing(false);
        }
    };
    const submit = async () => {
        const values = await form.validateFields();
        if (values.contentType === 'pdf' && !assets.some((asset) => asset.assetType === 'pdf')) {
            message.error('请上传 PDF 赛事文件');
            return;
        }
        setSaving(true);
        try {
            const payload = {
                title: values.title.trim(),
                content: values.content?.trim() || '',
                contentType: values.contentType,
                version: values.version,
            };
            if (draftId)
                await updateCompetitionNotice(draftId, payload);
            else
                await createCompetitionNotice(eventId, payload);
            message.success(draftId ? '公告已保存' : '公告创建成功');
            onSaved();
            onClose();
        }
        finally {
            setSaving(false);
        }
    };
    const pdfAsset = assets.find((asset) => asset.assetType === 'pdf');
    const drawerTitle = mode === 'create' ? '创建公告' : mode === 'edit' ? '编辑公告' : '查看公告';
    return (_jsx(Drawer, { title: _jsxs(Space, { children: [drawerTitle, _jsx(Tag, { color: contentTypeMeta[contentType].color, children: contentTypeMeta[contentType].label })] }), open: open, width: "min(1080px, 94vw)", destroyOnClose: true, onClose: onClose, extra: readOnly ? _jsx(Button, { onClick: onClose, children: "\u5173\u95ED" }) : _jsx(Button, { type: "primary", loading: saving, onClick: submit, children: "\u4FDD\u5B58\u8349\u7A3F" }), children: _jsx(Spin, { spinning: loading || uploading, children: _jsxs(Form, { form: form, layout: "vertical", requiredMark: "optional", children: [_jsxs("div", { className: "competition-notice-form-grid", children: [_jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true, whitespace: true, message: '请输入公告标题' }], children: _jsx(Input, { disabled: readOnly, maxLength: 256, showCount: true, placeholder: "\u8BF7\u8F93\u5165\u516C\u544A\u6807\u9898" }) }), _jsx(Form.Item, { name: "version", label: "\u7248\u672C", rules: [{ required: true, type: 'number', min: 1 }], children: _jsx(InputNumber, { disabled: readOnly, min: 1, precision: 0, style: { width: '100%' } }) })] }), _jsx(Form.Item, { name: "contentType", label: "\u53D1\u5E03\u7C7B\u578B", children: _jsx(Radio.Group, { disabled: readOnly, optionType: "button", buttonStyle: "solid", options: [
                                { label: '图文公告（Markdown）', value: 'markdown' },
                                { label: 'PDF 赛事文件', value: 'pdf' },
                            ], onChange: (event) => void changeContentType(event.target.value) }) }), contentType === 'markdown' ? (_jsx(Form.Item, { name: "content", label: "\u516C\u544A\u6B63\u6587", rules: [
                            { required: true, whitespace: true, message: '请输入公告正文' },
                            { max: 100000, message: 'Markdown 正文最多 100,000 字符' },
                            { validator: (_, value) => validateMarkdownSource(value) },
                        ], children: readOnly ? (_jsx(MdPreview, { value: readOnlyMarkdown, sanitize: sanitizeNoticeHtml })) : (_jsx(MdEditor, { language: "zh-CN", value: form.getFieldValue('content') || '', onChange: (value) => form.setFieldValue('content', value), onUploadImg: uploadImages, transformImgUrl: resolveAssetUrl, sanitize: sanitizeNoticeHtml, noMermaid: true, noKatex: true, style: { height: 560 } })) })) : (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "content", label: "\u6587\u4EF6\u7B80\u4ECB\uFF08\u9009\u586B\uFF09", rules: [{ max: 2000, message: 'PDF 简介最多 2,000 字符' }], children: _jsx(Input.TextArea, { disabled: readOnly, rows: 5, maxLength: 2000, showCount: true, placeholder: "\u8BF7\u8F93\u5165\u8D5B\u4E8B\u6587\u4EF6\u7B80\u4ECB" }) }), _jsxs("div", { className: "competition-notice-pdf", children: [pdfAsset ? (_jsx(Descriptions, { bordered: true, size: "small", column: 1, children: _jsx(Descriptions.Item, { label: "PDF \u6587\u4EF6", children: _jsxs(Space, { wrap: true, children: [_jsx(FilePdfOutlined, { className: "competition-notice-pdf__icon" }), _jsx("span", { children: pdfAsset.originalName }), _jsx(Tag, { children: fileSize(pdfAsset.sizeBytes) }), _jsx(Button, { size: "small", icon: _jsx(EyeOutlined, {}), loading: previewing, onClick: () => void openPdf(false), children: "\u9884\u89C8" }), _jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), loading: previewing, onClick: () => void openPdf(true), children: "\u4E0B\u8F7D" }), !readOnly && _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => void removeAsset(pdfAsset), children: "\u5220\u9664" })] }) }) })) : readOnly ? (_jsx(Alert, { type: "warning", showIcon: true, message: "\u5F53\u524D\u516C\u544A\u672A\u7ED1\u5B9A PDF \u6587\u4EF6" })) : (_jsxs(Upload.Dragger, { accept: ".pdf,application/pdf", multiple: false, showUploadList: false, beforeUpload: uploadPdf, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(InboxOutlined, {}) }), _jsx("p", { className: "ant-upload-text", children: "\u70B9\u51FB\u6216\u62D6\u62FD PDF \u5230\u6B64\u5904\u4E0A\u4F20" }), _jsx("p", { className: "ant-upload-hint", children: "\u4EC5\u652F\u6301 1 \u4E2A PDF\uFF0C\u6700\u5927 50MB\uFF1B\u518D\u6B21\u4E0A\u4F20\u5C06\u66FF\u6362\u539F\u6587\u4EF6" })] })), pdfAsset && !readOnly && (_jsx(Upload, { accept: ".pdf,application/pdf", showUploadList: false, beforeUpload: uploadPdf, children: _jsx(Button, { className: "competition-notice-pdf__replace", loading: uploading, children: "\u66FF\u6362 PDF" }) }))] })] }))] }) }) }));
}
export { contentTypeMeta };

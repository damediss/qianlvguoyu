import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Col, Descriptions, Form, Input, Modal, Row, Space, Spin, Tag, Typography, message, } from 'antd';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { AuthImage } from '../../components/ApiAuthImage';
import { fetchUrlFileBlob, fileNameFromUrl, resolveFilePreviewKind, } from '../../api/files';
import { FilePreviewModal } from '../../components/FilePreviewModal';
function resolveUploadedUrl(result) {
    return `/api/files/${result.id}/download?inline=true`;
}
function pickMediaUrl(...candidates) {
    for (const raw of candidates) {
        if (!raw?.trim())
            continue;
        return raw.trim();
    }
    return '';
}
function studentStatusTag(status) {
    if (status === 1)
        return _jsx(Tag, { color: "success", children: "\u5DF2\u7ED3\u4E1A" });
    if (status === 2)
        return _jsx(Tag, { color: "error", children: "\u4E0D\u5408\u683C\u7ED3\u4E1A" });
    return _jsx(Tag, { children: "\u672A\u7ED3\u4E1A" });
}
function scoreText(v) {
    if (v == null || v === '')
        return '—';
    return String(v);
}
const PHOTO_W = 90;
const PHOTO_H = 120;
/** 证书缩略图：与证件照同高，横向比例 */
const CERT_W = 170;
const CERT_H = 120;
function PhotoBlock({ title, url }) {
    return (_jsxs("div", { style: { textAlign: 'center', width: PHOTO_W }, children: [_jsx(Typography.Text, { type: "secondary", style: { display: 'block', marginBottom: 6, fontSize: 12 }, children: title }), url ? (_jsx(AuthImage, { url: url, alt: title, style: {
                    width: PHOTO_W,
                    height: PHOTO_H,
                    objectFit: 'cover',
                    borderRadius: 4,
                    border: '1px solid #f0f0f0',
                } })) : (_jsx("div", { style: {
                    width: PHOTO_W,
                    height: PHOTO_H,
                    margin: '0 auto',
                    border: '1px dashed #d9d9d9',
                    borderRadius: 4,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#999',
                    fontSize: 12,
                }, children: "\u6682\u65E0" }))] }));
}
/** 可上传且保留缩略图预览的证件/证书框 */
function EditableMediaBlock({ title, url, wide, accept, uploading, onUpload, onPreview, }) {
    const inputRef = useRef(null);
    const w = wide ? CERT_W : PHOTO_W;
    const h = wide ? CERT_H : PHOTO_H;
    const [loading, setLoading] = useState(false);
    const [objectUrl, setObjectUrl] = useState();
    const [previewKind, setPreviewKind] = useState('unsupported');
    useEffect(() => {
        if (!url) {
            setObjectUrl(undefined);
            setPreviewKind('unsupported');
            setLoading(false);
            return;
        }
        if (url.startsWith('blob:')) {
            setObjectUrl(url);
            setPreviewKind('blob-image');
            setLoading(false);
            return;
        }
        let cancelled = false;
        let localUrl;
        setLoading(true);
        setObjectUrl(undefined);
        setPreviewKind('unsupported');
        const fetchUrl = url.includes('?') ? `${url}&inline=true` : `${url}?inline=true`;
        fetchUrlFileBlob(fetchUrl, { silentError: true })
            .then((blob) => {
            if (cancelled)
                return;
            const kind = resolveFilePreviewKind(fileNameFromUrl(url), undefined, blob.type);
            if (kind === 'image' || kind === 'pdf') {
                localUrl = URL.createObjectURL(blob);
                setObjectUrl(localUrl);
                setPreviewKind(kind);
                return;
            }
            if (blob.type.startsWith('image/')) {
                localUrl = URL.createObjectURL(blob);
                setObjectUrl(localUrl);
                setPreviewKind('image');
                return;
            }
            if (blob.type === 'application/pdf') {
                localUrl = URL.createObjectURL(blob);
                setObjectUrl(localUrl);
                setPreviewKind('pdf');
                return;
            }
            setPreviewKind('unsupported');
        })
            .catch(() => {
            if (!cancelled)
                setPreviewKind('unsupported');
        })
            .finally(() => {
            if (!cancelled)
                setLoading(false);
        });
        return () => {
            cancelled = true;
            if (localUrl)
                URL.revokeObjectURL(localUrl);
        };
    }, [url]);
    const openPicker = () => {
        if (uploading)
            return;
        inputRef.current?.click();
    };
    const onFileChange = async (event) => {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file)
            return;
        try {
            await onUpload(file);
        }
        catch {
            // 上层已提示
        }
    };
    const frameStyle = {
        width: w,
        height: h,
        margin: '0 auto',
        border: url ? '1px solid #f0f0f0' : '1px dashed #bfbfbf',
        borderRadius: 6,
        background: '#fafafa',
        overflow: 'hidden',
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    };
    const openLargePreview = () => {
        if (url && onPreview)
            onPreview(url);
    };
    const renderThumb = () => {
        if (uploading || loading) {
            return _jsx(LoadingOutlined, { style: { fontSize: 22, color: '#1a7fc1' } });
        }
        if (!url) {
            return (_jsxs("div", { style: { color: '#999', fontSize: 12, textAlign: 'center' }, children: [_jsx(PlusOutlined, { style: { fontSize: 18, marginBottom: 4 } }), _jsx("div", { children: "\u4E0A\u4F20" })] }));
        }
        if ((previewKind === 'image' || previewKind === 'blob-image') && objectUrl) {
            return (_jsx("img", { src: objectUrl, alt: title, style: { width: '100%', height: '100%', objectFit: wide ? 'contain' : 'cover', display: 'block' } }));
        }
        if (previewKind === 'pdf' && objectUrl) {
            return (_jsx("iframe", { title: `${title}预览`, src: `${objectUrl}#toolbar=0&navpanes=0&scrollbar=0&view=Fit`, style: {
                    width: '100%',
                    height: '100%',
                    display: 'block',
                    pointerEvents: 'none',
                    background: '#fff',
                    border: 'none',
                } }));
        }
        return (_jsx(AuthImage, { url: url, alt: title, style: { width: '100%', height: '100%', objectFit: wide ? 'contain' : 'cover', display: 'block' } }));
    };
    return (_jsxs("div", { style: { textAlign: 'center', width: w }, children: [_jsx(Typography.Text, { type: "secondary", style: { display: 'block', marginBottom: 6, fontSize: 12 }, children: title }), _jsx("input", { ref: inputRef, type: "file", accept: accept, style: { display: 'none' }, onChange: (e) => void onFileChange(e) }), _jsx("div", { role: "button", tabIndex: 0, title: url ? (onPreview ? '点击预览' : '点击更换') : '点击上传', onClick: () => {
                    if (!url) {
                        openPicker();
                        return;
                    }
                    if (onPreview) {
                        openLargePreview();
                        return;
                    }
                    openPicker();
                }, onKeyDown: (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        if (!url)
                            openPicker();
                        else if (onPreview)
                            openLargePreview();
                        else
                            openPicker();
                    }
                }, style: {
                    ...frameStyle,
                    cursor: uploading ? 'wait' : 'pointer',
                }, children: renderThumb() }), _jsx("div", { style: { marginTop: 4, minHeight: 22 }, children: url ? (_jsxs(Space, { size: 8, children: [onPreview ? (_jsx(Button, { type: "link", size: "small", style: { padding: 0, height: 'auto', fontSize: 12 }, onClick: (e) => {
                                e.stopPropagation();
                                openLargePreview();
                            }, children: "\u9884\u89C8" })) : null, _jsx(Button, { type: "link", size: "small", style: { padding: 0, height: 'auto', fontSize: 12 }, disabled: uploading, onClick: (e) => {
                                e.stopPropagation();
                                openPicker();
                            }, children: "\u66F4\u6362" })] })) : null })] }));
}
function CertificatePreviewBlock({ url, title = '证书', onOpen, }) {
    const [loading, setLoading] = useState(false);
    const [objectUrl, setObjectUrl] = useState();
    const [previewKind, setPreviewKind] = useState('unsupported');
    useEffect(() => {
        if (!url) {
            setObjectUrl(undefined);
            setPreviewKind('unsupported');
            setLoading(false);
            return;
        }
        let cancelled = false;
        let localUrl;
        setLoading(true);
        setObjectUrl(undefined);
        setPreviewKind('unsupported');
        const fetchUrl = url.includes('?') ? `${url}&inline=true` : `${url}?inline=true`;
        fetchUrlFileBlob(fetchUrl, { silentError: true })
            .then((blob) => {
            if (cancelled)
                return;
            const kind = resolveFilePreviewKind(fileNameFromUrl(url), undefined, blob.type);
            if (kind === 'image' || kind === 'pdf') {
                localUrl = URL.createObjectURL(blob);
                setObjectUrl(localUrl);
                setPreviewKind(kind);
                return;
            }
            setPreviewKind('unsupported');
        })
            .catch(() => {
            if (!cancelled)
                setPreviewKind('unsupported');
        })
            .finally(() => {
            if (!cancelled)
                setLoading(false);
        });
        return () => {
            cancelled = true;
            if (localUrl)
                URL.revokeObjectURL(localUrl);
        };
    }, [url]);
    if (!url)
        return null;
    const frameStyle = {
        width: CERT_W,
        height: CERT_H,
        border: '1px solid #f0f0f0',
        borderRadius: 4,
        background: '#fafafa',
        overflow: 'hidden',
        cursor: 'pointer',
    };
    const openPreview = () => onOpen?.(url);
    return (_jsxs("div", { style: { textAlign: 'center', width: CERT_W }, children: [_jsx(Typography.Text, { type: "secondary", style: { display: 'block', marginBottom: 6, fontSize: 12 }, children: title }), loading ? (_jsx("div", { style: { ...frameStyle, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'default' }, children: _jsx(Spin, { size: "small" }) })) : previewKind === 'image' && objectUrl ? (_jsx("div", { role: "button", tabIndex: 0, title: "\u70B9\u51FB\u653E\u5927\u9884\u89C8", onClick: openPreview, onKeyDown: (e) => {
                    if (e.key === 'Enter' || e.key === ' ')
                        openPreview();
                }, children: _jsx(AuthImage, { url: objectUrl, alt: title, style: { ...frameStyle, objectFit: 'contain', display: 'block' } }) })) : previewKind === 'pdf' && objectUrl ? (_jsx("div", { role: "button", tabIndex: 0, title: "\u70B9\u51FB\u653E\u5927\u9884\u89C8", onClick: openPreview, onKeyDown: (e) => {
                    if (e.key === 'Enter' || e.key === ' ')
                        openPreview();
                }, style: { display: 'block' }, children: _jsx("iframe", { title: "\u8BC1\u4E66\u9884\u89C8", src: `${objectUrl}#toolbar=0&navpanes=0&scrollbar=0&view=Fit`, style: { ...frameStyle, display: 'block', pointerEvents: 'none', background: '#fff' } }) })) : (_jsx("div", { style: {
                    ...frameStyle,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    borderStyle: 'dashed',
                    color: '#999',
                    fontSize: 12,
                    cursor: 'default',
                }, children: "\u6682\u65E0" }))] }));
}
export function useGraduationStudentActions(classId, onChanged) {
    const queryClient = useQueryClient();
    const [detailOpen, setDetailOpen] = useState(false);
    const [editOpen, setEditOpen] = useState(false);
    const [peopleId, setPeopleId] = useState(null);
    const [editContext, setEditContext] = useState(null);
    const [mediaDrafts, setMediaDrafts] = useState({});
    const [uploadingField, setUploadingField] = useState(null);
    const [certPreview, setCertPreview] = useState(null);
    const [editForm] = Form.useForm();
    const openCertFile = (fileUrl, fileName) => {
        setCertPreview({
            fileUrl,
            fileName: fileName || fileNameFromUrl(fileUrl) || '证书.pdf',
        });
    };
    const { data: detail, isLoading: detailLoading } = useQuery({
        queryKey: ['graduation-student-detail', classId, peopleId],
        queryFn: () => trainApi.getGraduationStudentDetail(classId, peopleId),
        enabled: classId > 0 && !!peopleId && (detailOpen || editOpen),
    });
    const saveMu = useMutation({
        mutationFn: (payload) => trainApi.updateGraduationStudent(classId, peopleId, payload),
        onSuccess: () => {
            message.success('学员信息已保存');
            setEditOpen(false);
            setEditContext(null);
            setMediaDrafts({});
            void queryClient.invalidateQueries({ queryKey: ['graduation-students', classId] });
            void queryClient.invalidateQueries({ queryKey: ['graduation-student-detail', classId, peopleId] });
            onChanged?.();
        },
        onError: (e) => message.error(e.message || '保存失败'),
    });
    const closeEdit = () => {
        setEditOpen(false);
        setEditContext(null);
        setMediaDrafts({});
        setUploadingField(null);
        if (!detailOpen)
            setPeopleId(null);
    };
    const openDetail = (id) => {
        setPeopleId(id);
        setDetailOpen(true);
    };
    const openEdit = (row) => {
        const id = Number(row.id);
        if (!Number.isFinite(id) || id <= 0)
            return;
        setPeopleId(id);
        setMediaDrafts({});
        setEditContext({
            realname: row.realname != null ? String(row.realname) : undefined,
            theoryScore: row.theoryScore,
            skillScore: row.skillScore ?? row.realScore,
            examScore: row.examScore,
            performanceScore: row.performanceScore,
            graduateStatus: typeof row.graduateStatus === 'number' ? row.graduateStatus : undefined,
            qualified: typeof row.qualified === 'boolean' ? row.qualified : undefined,
            qualifyHint: row.qualifyHint != null ? String(row.qualifyHint) : undefined,
            certificateNo: row.certificateNo != null ? String(row.certificateNo) : undefined,
        });
        editForm.setFieldsValue({
            realname: row.realname ?? '',
            phone: row.phone ?? '',
            idCard: row.idCard ?? '',
            ticketNo: row.ticketNo ?? '',
        });
        setEditOpen(true);
    };
    const handleMediaUpload = async (field, file, kind) => {
        if (kind === 'image') {
            const lower = file.name.toLowerCase();
            const okExt = /\.(jpe?g|jfif|png|webp|gif|bmp|heic|heif)$/i.test(lower);
            const okType = !file.type || file.type.startsWith('image/');
            if (!okExt && !okType) {
                message.error('请选择 JPG/PNG/WEBP 等图片文件');
                return;
            }
        }
        else {
            const lower = file.name.toLowerCase();
            const okExt = /\.(pdf|jpe?g|png|webp)$/i.test(lower);
            if (!okExt) {
                message.error('证书请上传 PDF 或图片');
                return;
            }
        }
        setUploadingField(field);
        const localPreview = URL.createObjectURL(file);
        setMediaDrafts((prev) => {
            const old = prev[field];
            if (old?.startsWith('blob:'))
                URL.revokeObjectURL(old);
            return { ...prev, [field]: localPreview };
        });
        try {
            const result = await uploadFile(file, 'public');
            const url = resolveUploadedUrl(result);
            setMediaDrafts((prev) => {
                const old = prev[field];
                if (old?.startsWith('blob:'))
                    URL.revokeObjectURL(old);
                return { ...prev, [field]: url };
            });
            message.success('已上传，请点击保存生效');
        }
        catch (err) {
            setMediaDrafts((prev) => {
                const old = prev[field];
                if (old?.startsWith('blob:'))
                    URL.revokeObjectURL(old);
                const next = { ...prev };
                delete next[field];
                return next;
            });
            message.error(err instanceof Error && err.message ? err.message : '上传失败');
        }
        finally {
            setUploadingField(null);
        }
    };
    const submitEdit = async () => {
        try {
            const values = await editForm.validateFields();
            saveMu.mutate({
                ...values,
                idCardHeader: mediaDrafts.idCardHeader,
                idCardFront: mediaDrafts.idCardFront,
                idCardBack: mediaDrafts.idCardBack,
                certificateFilePath: mediaDrafts.certificateFilePath,
            });
        }
        catch {
            // 表单校验
        }
    };
    const editDisplayName = detail?.realname || editContext?.realname || '';
    const summaryQualified = detail?.qualified ?? editContext?.qualified;
    const summaryQualifyHint = detail?.qualifyHint || editContext?.qualifyHint;
    const summaryStatus = detail?.graduateStatus ?? editContext?.graduateStatus;
    const summaryCert = detail?.certificateNo || editContext?.certificateNo;
    const actionColumn = {
        title: '操作',
        key: 'studentAct',
        width: 120,
        fixed: 'right',
        render: (_, row) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", onClick: () => openDetail(Number(row.id)), children: "\u8BE6\u60C5" }), _jsx(Button, { type: "link", size: "small", onClick: () => openEdit(row), children: "\u4FEE\u6539" })] })),
    };
    const modals = (_jsxs(_Fragment, { children: [_jsx(Modal, { title: detail?.realname ? `学员详情 · ${detail.realname}` : '学员详情', open: detailOpen, onCancel: () => {
                    setDetailOpen(false);
                    setPeopleId(null);
                }, footer: [
                    _jsx(Button, { type: "primary", onClick: () => {
                            if (!detail)
                                return;
                            setMediaDrafts({});
                            setEditContext({
                                realname: detail.realname,
                                theoryScore: detail.theoryScore,
                                skillScore: detail.skillScore,
                                examScore: detail.examScore,
                                performanceScore: detail.performanceScore,
                                graduateStatus: detail.graduateStatus,
                                qualified: detail.qualified,
                                qualifyHint: detail.qualifyHint,
                                certificateNo: detail.certificateNo || undefined,
                            });
                            editForm.setFieldsValue({
                                realname: detail.realname ?? '',
                                phone: detail.phone ?? '',
                                idCard: detail.idCard ?? '',
                                ticketNo: detail.ticketNo ?? '',
                            });
                            setDetailOpen(false);
                            setEditOpen(true);
                        }, children: "\u4FEE\u6539" }, "edit"),
                    _jsx(Button, { onClick: () => {
                            setDetailOpen(false);
                            setPeopleId(null);
                        }, children: "\u5173\u95ED" }, "close"),
                ], width: 920, destroyOnHidden: true, children: detailLoading ? (_jsx("div", { style: { padding: 48, textAlign: 'center' }, children: _jsx(Spin, {}) })) : detail ? (_jsxs(Space, { direction: "vertical", size: "middle", style: { width: '100%' }, children: [_jsxs(Descriptions, { bordered: true, size: "small", column: 2, children: [_jsx(Descriptions.Item, { label: "\u59D3\u540D", children: detail.realname || '—' }), _jsx(Descriptions.Item, { label: "\u624B\u673A", children: detail.phone || '—' }), _jsx(Descriptions.Item, { label: "\u8EAB\u4EFD\u8BC1", children: detail.idCard || '—' }), _jsx(Descriptions.Item, { label: "\u51C6\u8003\u8BC1\u53F7", children: detail.ticketNo || '—' }), _jsx(Descriptions.Item, { label: "\u6027\u522B", children: detail.gender || '—' }), _jsx(Descriptions.Item, { label: "\u6C11\u65CF", children: detail.nation || '—' }), _jsx(Descriptions.Item, { label: "\u5B66\u5386", children: detail.education || '—' }), _jsx(Descriptions.Item, { label: "\u653F\u6CBB\u9762\u8C8C", children: detail.political || '—' }), _jsx(Descriptions.Item, { label: "\u5730\u5740", span: 2, children: detail.address || '—' }), _jsx(Descriptions.Item, { label: "\u7406\u8BBA", children: scoreText(detail.theoryScore) }), _jsx(Descriptions.Item, { label: "\u6280\u80FD", children: scoreText(detail.skillScore) }), _jsx(Descriptions.Item, { label: "\u7EFC\u5408", children: scoreText(detail.examScore) }), _jsx(Descriptions.Item, { label: "\u5DE5\u4F5C\u4E1A\u7EE9", children: scoreText(detail.performanceScore) }), _jsx(Descriptions.Item, { label: "\u8FBE\u6807", children: detail.qualified ? _jsx(Tag, { color: "success", children: "\u8FBE\u6807" }) : _jsx(Tag, { color: "warning", children: detail.qualifyHint || '未达标' }) }), _jsx(Descriptions.Item, { label: "\u7ED3\u4E1A\u72B6\u6001", children: studentStatusTag(detail.graduateStatus) }), _jsx(Descriptions.Item, { label: "\u8BC1\u4E66\u7F16\u53F7", span: 2, children: detail.certificateNo ? (detail.certificateFilePath ? (_jsx(Button, { type: "link", size: "small", style: { padding: 0, height: 'auto' }, onClick: () => openCertFile(detail.certificateFilePath, `${detail.certificateNo}.pdf`), children: detail.certificateNo })) : (detail.certificateNo)) : ('—') }), _jsx(Descriptions.Item, { label: "\u804C\u4E1A", children: detail.certificateOccupation || '—' }), _jsx(Descriptions.Item, { label: "\u5DE5\u79CD", children: detail.certificateWorkType || '—' }), _jsx(Descriptions.Item, { label: "\u7B49\u7EA7", children: detail.certificateLevel || '—' }), _jsx(Descriptions.Item, { label: "\u6838\u53D1\u65E5\u671F", children: detail.certificateIssueDate || '—' }), _jsx(Descriptions.Item, { label: "\u53D1\u8BC1\u673A\u6784", span: 2, children: detail.certificateIssuingAuthority || '—' }), _jsx(Descriptions.Item, { label: "\u8BC4\u4EF7\u673A\u6784", span: 2, children: detail.certificateEvaluationOrg || '—' })] }), _jsxs("div", { children: [_jsx(Typography.Text, { strong: true, style: { display: 'block', marginBottom: 12 }, children: "\u8BC1\u4EF6\u4E0E\u8BC1\u4E66\u7167\u7247" }), _jsxs(Space, { wrap: true, size: "middle", align: "start", children: [_jsx(PhotoBlock, { title: "\u8BC1\u4EF6\u7167", url: detail.idCardHeader }), _jsx(PhotoBlock, { title: "\u8EAB\u4EFD\u8BC1\u6B63\u9762", url: detail.idCardFront }), _jsx(PhotoBlock, { title: "\u8EAB\u4EFD\u8BC1\u53CD\u9762", url: detail.idCardBack }), _jsx(CertificatePreviewBlock, { url: detail.certificateFilePath || undefined, title: "\u8BC1\u4E66", onOpen: (fileUrl) => openCertFile(fileUrl, detail.certificateNo ? `${detail.certificateNo}.pdf` : undefined) })] })] })] })) : (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u5B66\u5458\u8BE6\u60C5" })) }), _jsx(FilePreviewModal, { open: Boolean(certPreview), target: certPreview, onClose: () => setCertPreview(null) }), _jsxs(Modal, { title: _jsxs("div", { children: [_jsx("div", { style: { fontSize: 16, fontWeight: 600, color: '#12263a' }, children: editDisplayName ? `修改学员 · ${editDisplayName}` : '修改学员' }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12, fontWeight: 400 }, children: "\u7ED3\u4E1A\u540D\u518C\u57FA\u7840\u4FE1\u606F\u6821\u6B63" })] }), open: editOpen, onCancel: closeEdit, onOk: () => void submitEdit(), okText: "\u4FDD\u5B58", cancelText: "\u53D6\u6D88", confirmLoading: saveMu.isPending || uploadingField != null, destroyOnHidden: true, width: 720, styles: { body: { paddingTop: 12 } }, children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u53EF\u4FEE\u6539\u59D3\u540D\u3001\u624B\u673A\u3001\u8EAB\u4EFD\u8BC1\u3001\u51C6\u8003\u8BC1\u53F7\uFF0C\u4EE5\u53CA\u8BC1\u4EF6\u7167/\u8EAB\u4EFD\u8BC1\u6B63\u53CD\u9762/\u8BC1\u4E66\u6587\u4EF6\uFF1B\u6210\u7EE9\u4E0E\u7ED3\u4E1A\u72B6\u6001\u8BF7\u8D70\u7ED3\u4E1A\u6D41\u7A0B\u3002" }), _jsxs("div", { style: {
                            marginBottom: 16,
                            padding: '12px 14px',
                            borderRadius: 8,
                            background: 'linear-gradient(135deg, #f4f8fb 0%, #eef5fa 100%)',
                            border: '1px solid #e2ebf2',
                        }, children: [_jsxs(Space, { wrap: true, size: [8, 8], style: { marginBottom: 10 }, children: [studentStatusTag(summaryStatus), summaryQualified === true ? (_jsx(Tag, { color: "success", children: "\u8FBE\u6807" })) : summaryQualified === false ? (_jsx(Tag, { color: "warning", children: summaryQualifyHint || '未达标' })) : null, summaryCert ? _jsxs(Tag, { color: "blue", children: ["\u8BC1\u4E66 ", summaryCert] }) : _jsx(Tag, { children: "\u6682\u65E0\u8BC1\u4E66\u7F16\u53F7" })] }), _jsxs(Row, { gutter: [12, 8], children: [_jsxs(Col, { span: 6, children: [_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u7406\u8BBA" }), _jsx("div", { style: { fontWeight: 600, color: '#1f2937' }, children: scoreText(detail?.theoryScore ?? editContext?.theoryScore) })] }), _jsxs(Col, { span: 6, children: [_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u6280\u80FD" }), _jsx("div", { style: { fontWeight: 600, color: '#1f2937' }, children: scoreText(detail?.skillScore ?? editContext?.skillScore) })] }), _jsxs(Col, { span: 6, children: [_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u7EFC\u5408" }), _jsx("div", { style: { fontWeight: 600, color: '#1f2937' }, children: scoreText(detail?.examScore ?? editContext?.examScore) })] }), _jsxs(Col, { span: 6, children: [_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u5DE5\u4F5C\u4E1A\u7EE9" }), _jsx("div", { style: { fontWeight: 600, color: '#1f2937' }, children: scoreText(detail?.performanceScore ?? editContext?.performanceScore) })] })] })] }), _jsxs(Form, { form: editForm, layout: "vertical", requiredMark: "optional", onFinish: (values) => saveMu.mutate({
                            ...values,
                            idCardHeader: mediaDrafts.idCardHeader,
                            idCardFront: mediaDrafts.idCardFront,
                            idCardBack: mediaDrafts.idCardBack,
                            certificateFilePath: mediaDrafts.certificateFilePath,
                        }), children: [_jsx(Typography.Text, { strong: true, style: { display: 'block', marginBottom: 12 }, children: "\u53EF\u4FEE\u6539\u5B57\u6BB5" }), _jsxs(Row, { gutter: 16, children: [_jsx(Col, { xs: 24, sm: 12, children: _jsx(Form.Item, { name: "realname", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, { maxLength: 32, placeholder: "\u5B66\u5458\u771F\u5B9E\u59D3\u540D", allowClear: true }) }) }), _jsx(Col, { xs: 24, sm: 12, children: _jsx(Form.Item, { name: "phone", label: "\u624B\u673A", children: _jsx(Input, { maxLength: 20, placeholder: "\u8054\u7CFB\u624B\u673A\u53F7", allowClear: true }) }) }), _jsx(Col, { xs: 24, sm: 12, children: _jsx(Form.Item, { name: "idCard", label: "\u8EAB\u4EFD\u8BC1\u53F7", children: _jsx(Input, { maxLength: 32, placeholder: "15 \u6216 18 \u4F4D\u8BC1\u4EF6\u53F7", allowClear: true }) }) }), _jsx(Col, { xs: 24, sm: 12, children: _jsx(Form.Item, { name: "ticketNo", label: "\u51C6\u8003\u8BC1\u53F7", children: _jsx(Input, { maxLength: 64, placeholder: "\u8003\u8BD5\u51C6\u8003\u8BC1\u53F7", allowClear: true }) }) })] })] }), _jsxs("div", { style: { marginTop: 4, paddingTop: 12, borderTop: '1px solid #f0f0f0' }, children: [_jsx(Typography.Text, { strong: true, style: { display: 'block', marginBottom: 4 }, children: "\u8BC1\u4EF6\u4E0E\u8BC1\u4E66" }), _jsx(Typography.Text, { type: "secondary", style: { display: 'block', marginBottom: 12, fontSize: 12 }, children: "\u65B9\u6846\u5185\u5C55\u793A\u7F29\u7565\u56FE\uFF1B\u70B9\u51FB\u53EF\u9884\u89C8\u3002\u4E0B\u65B9\u300C\u66F4\u6362\u300D\u53EF\u91CD\u65B0\u4E0A\u4F20\uFF0C\u4FDD\u5B58\u540E\u751F\u6548\u3002\u4E0A\u4F20\u8BC1\u4EF6\u7167\u524D\u8BF7\u786E\u4FDD\u5DF2\u586B\u5199\u8EAB\u4EFD\u8BC1\u53F7\u3002" }), _jsxs(Space, { wrap: true, size: "middle", align: "start", children: [_jsx(EditableMediaBlock, { title: "\u8BC1\u4EF6\u7167", url: pickMediaUrl(mediaDrafts.idCardHeader, detail?.idCardHeader), accept: "image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp", uploading: uploadingField === 'idCardHeader', onUpload: (file) => handleMediaUpload('idCardHeader', file, 'image'), onPreview: (fileUrl) => openCertFile(fileUrl, '证件照') }), _jsx(EditableMediaBlock, { title: "\u8EAB\u4EFD\u8BC1\u6B63\u9762", url: pickMediaUrl(mediaDrafts.idCardFront, detail?.idCardFront), accept: "image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp", uploading: uploadingField === 'idCardFront', onUpload: (file) => handleMediaUpload('idCardFront', file, 'image'), onPreview: (fileUrl) => openCertFile(fileUrl, '身份证正面') }), _jsx(EditableMediaBlock, { title: "\u8EAB\u4EFD\u8BC1\u53CD\u9762", url: pickMediaUrl(mediaDrafts.idCardBack, detail?.idCardBack), accept: "image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp", uploading: uploadingField === 'idCardBack', onUpload: (file) => handleMediaUpload('idCardBack', file, 'image'), onPreview: (fileUrl) => openCertFile(fileUrl, '身份证反面') }), _jsx(EditableMediaBlock, { title: "\u8BC1\u4E66", wide: true, url: pickMediaUrl(mediaDrafts.certificateFilePath, detail?.certificateFilePath), accept: ".pdf,application/pdf,image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp", uploading: uploadingField === 'certificateFilePath', onUpload: (file) => handleMediaUpload('certificateFilePath', file, 'file'), onPreview: (fileUrl) => openCertFile(fileUrl, detail?.certificateNo ? `${detail.certificateNo}.pdf` : undefined) })] })] })] })] }));
    return { actionColumn, modals };
}

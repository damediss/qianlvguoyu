import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckOutlined, DeleteOutlined, EyeOutlined, FileSyncOutlined, RollbackOutlined, UploadOutlined, } from '@ant-design/icons';
import { Button, Descriptions, Form, Image, Input, Modal, Popconfirm, Select, Space, Table, Typography, Upload, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { uploadFile } from '../../../api/knowledge';
import { generatePreparationCandidateDocument, pagePreparationCandidateDocuments, deletePreparationCandidateDocument, reviewPreparationCandidateDocument, uploadPreparationCandidateDocument, } from '../../../api/certificationPreparation';
import { FilePreviewModal } from '../../../components/FilePreviewModal';
import { PermissionGate } from '../../../directives/permission';
import { boolTag, candidateDocumentRatioTag, dash, dateTimeText, statusTag } from './shared';
const AUTO_APPROVED_IMAGE_DOC_CODES = new Set(['DOC002', 'DOC003', 'DOC004']);
export function CandidateDocumentDrawer({ taskId, candidate, open, onClose, onChanged }) {
    const queryClient = useQueryClient();
    const [detailStatus, setDetailStatus] = useState();
    const [uploadModalOpen, setUploadModalOpen] = useState(false);
    const [uploadingDocument, setUploadingDocument] = useState(null);
    const [reviewingDocument, setReviewingDocument] = useState(null);
    const [previewTarget, setPreviewTarget] = useState(null);
    const [uploadingFile, setUploadingFile] = useState(false);
    const [localPreviewUrl, setLocalPreviewUrl] = useState();
    const [uploadForm] = Form.useForm();
    const [reviewForm] = Form.useForm();
    useEffect(() => {
        return () => {
            if (localPreviewUrl) {
                URL.revokeObjectURL(localPreviewUrl);
            }
        };
    }, [localPreviewUrl]);
    const documentsQuery = useQuery({
        queryKey: ['certification-preparation-documents', taskId, candidate?.id, detailStatus],
        queryFn: () => pagePreparationCandidateDocuments(taskId, {
            candidateId: candidate.id,
            status: detailStatus,
            page: 1,
            pageSize: 50,
        }),
        enabled: open && Boolean(candidate?.id),
    });
    useEffect(() => {
        if (!open) {
            setDetailStatus(undefined);
            setUploadModalOpen(false);
            setUploadingDocument(null);
            setReviewingDocument(null);
            setPreviewTarget(null);
            setLocalPreviewUrl((prev) => {
                if (prev)
                    URL.revokeObjectURL(prev);
                return undefined;
            });
        }
    }, [open]);
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-documents', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
        onChanged?.();
    };
    const saveMutation = useMutation({
        mutationFn: (payload) => uploadPreparationCandidateDocument(taskId, payload),
        onSuccess: () => {
            if (uploadingDocument?.fileId && AUTO_APPROVED_IMAGE_DOC_CODES.has(uploadingDocument.docCode)) {
                message.warning('照片已修改，请重新生成申请表');
            }
            else {
                message.success('资料已保存');
            }
            setUploadModalOpen(false);
            setUploadingDocument(null);
            uploadForm.resetFields();
            refresh();
        },
    });
    const reviewMutation = useMutation({
        mutationFn: ({ id, payload }) => reviewPreparationCandidateDocument(taskId, id, payload),
        onSuccess: () => {
            message.success('审核结果已保存');
            setReviewingDocument(null);
            reviewForm.resetFields();
            refresh();
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (documentId) => deletePreparationCandidateDocument(taskId, documentId),
        onSuccess: () => {
            message.success('资料已删除，可重新上传');
            refresh();
        },
    });
    const generateMutation = useMutation({
        mutationFn: (docCode) => generatePreparationCandidateDocument(taskId, candidate.id, { docCode }),
        onSuccess: () => {
            message.success('报名信息表已生成');
            refresh();
        },
        onError: (error) => message.error(error instanceof Error ? error.message : '生成失败'),
    });
    const requiredImagesReady = useMemo(() => ['DOC002', 'DOC003', 'DOC004'].every((docCode) => (documentsQuery.data?.items || []).some((item) => item.docCode === docCode && Boolean(item.fileId))), [documentsQuery.data?.items]);
    const uploadProps = {
        maxCount: 1,
        beforeUpload: async (file) => {
            const docCode = uploadForm.getFieldValue('docCode') || uploadingDocument?.docCode;
            const validationError = validateUploadFileType(docCode, file);
            if (validationError) {
                message.warning(validationError);
                return Upload.LIST_IGNORE;
            }
            setUploadingFile(true);
            try {
                const saved = await uploadFile(file, 'private');
                const fileId = typeof saved.id === 'string' ? saved.id : String(saved.id);
                uploadForm.setFieldsValue({
                    fileId,
                    fileName: saved.originalName,
                    fileType: saved.contentType,
                });
                setLocalPreviewUrl((prev) => {
                    if (prev)
                        URL.revokeObjectURL(prev);
                    return URL.createObjectURL(file);
                });
                message.success('文件已上传到统一文件服务');
            }
            catch (error) {
                message.error(error instanceof Error ? error.message : '文件上传失败');
                return Upload.LIST_IGNORE;
            }
            finally {
                setUploadingFile(false);
            }
            return false;
        },
    };
    const documentColumns = [
        { title: '资料编码', dataIndex: 'docCode', width: 100 },
        { title: '资料名称', dataIndex: 'docName', minWidth: 200 },
        { title: '必传', dataIndex: 'required', width: 72, render: boolTag },
        { title: '文件', dataIndex: 'fileName', minWidth: 140, render: dash },
        {
            title: '预览',
            width: 80,
            align: 'center',
            render: (_, record) => record.fileId && record.status !== 'missing' ? (_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => setPreviewTarget({
                    taskId,
                    documentId: record.id,
                    fileId: record.fileId,
                    fileName: record.fileName,
                    fileType: record.fileType,
                }), children: "\u9884\u89C8" })) : (_jsx(Typography.Text, { type: "secondary", children: "-" })),
        },
        { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
        { title: '上传时间', dataIndex: 'uploadedAt', width: 150, render: dateTimeText },
        {
            title: '操作',
            width: 140,
            fixed: 'right',
            render: (_, record) => {
                if (record.docCode === 'DOC001') {
                    if (record.status === 'approved') {
                        return (_jsx(PermissionGate, { code: "certification.preparation.document.generate", children: _jsx(Button, { type: "link", size: "small", icon: _jsx(FileSyncOutlined, {}), loading: generateMutation.isPending, onClick: () => generateMutation.mutate('DOC001'), children: "\u91CD\u65B0\u751F\u6210" }) }));
                    }
                    return (_jsx(PermissionGate, { code: "certification.preparation.document.generate", children: _jsx(Button, { type: "link", size: "small", icon: _jsx(FileSyncOutlined, {}), loading: generateMutation.isPending, disabled: !requiredImagesReady, onClick: () => generateMutation.mutate('DOC001'), children: record.status === 'missing' ? '生成认定申请表' : '重新生成' }) }));
                }
                if (AUTO_APPROVED_IMAGE_DOC_CODES.has(record.docCode)) {
                    return (_jsx(PermissionGate, { code: "certification.preparation.document.upload", children: _jsx(Button, { type: "link", size: "small", icon: _jsx(UploadOutlined, {}), onClick: () => openUploadModal(record), children: record.fileId ? '修改' : '上传' }) }));
                }
                const reviewed = record.status === 'approved';
                if (record.status === 'missing') {
                    return (_jsx(PermissionGate, { code: "certification.preparation.document.upload", children: _jsx(Button, { type: "link", size: "small", icon: _jsx(UploadOutlined, {}), onClick: () => openUploadModal(record), children: "\u5F85\u4E0A\u4F20" }) }));
                }
                if (record.status === 'rejected') {
                    return (_jsx(PermissionGate, { code: "certification.preparation.document.upload", children: _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u9A73\u56DE\u8D44\u6599\uFF1F", okText: "\u5220\u9664", cancelText: "\u53D6\u6D88", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), loading: deleteMutation.isPending, children: "\u5220\u9664" }) }) }));
                }
                return (_jsx(PermissionGate, { code: "certification.preparation.document.review", children: _jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(CheckOutlined, {}), disabled: reviewed, onClick: () => openReview(record, 'approved'), children: "\u901A\u8FC7" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(RollbackOutlined, {}), disabled: reviewed, onClick: () => openReview(record, 'rejected'), children: "\u9A73\u56DE" })] }) }));
            },
        },
    ];
    function openUploadModal(record) {
        if (!candidate || record.docCode === 'DOC001') {
            return;
        }
        uploadForm.resetFields();
        setLocalPreviewUrl((prev) => {
            if (prev)
                URL.revokeObjectURL(prev);
            return undefined;
        });
        setUploadingDocument(record);
        uploadForm.setFieldsValue({
            candidateId: candidate.id,
            docCode: record.docCode,
            docName: record.docName,
            required: record.required,
        });
        setUploadModalOpen(true);
    }
    function openReview(record, status) {
        setReviewingDocument(record);
        reviewForm.setFieldsValue({ status, reviewComment: status === 'approved' ? '资料审核通过' : undefined });
    }
    function submitUpload() {
        uploadForm.validateFields().then((values) => saveMutation.mutate(values));
    }
    function submitReview() {
        if (!reviewingDocument) {
            return;
        }
        reviewForm.validateFields().then((values) => {
            reviewMutation.mutate({ id: reviewingDocument.id, payload: values });
        });
    }
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { title: candidate ? `${candidate.candidateName} — 学员资料` : '学员资料', open: open, onCancel: onClose, width: 1080, footer: null, destroyOnHidden: true, children: candidate ? (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsxs(Descriptions, { bordered: true, size: "small", column: 3, children: [_jsx(Descriptions.Item, { label: "\u59D3\u540D", children: candidate.candidateName }), _jsx(Descriptions.Item, { label: "\u8EAB\u4EFD\u8BC1\u53F7", children: candidate.maskedIdCardNo || '-' }), _jsx(Descriptions.Item, { label: "\u8054\u7CFB\u7535\u8BDD", children: candidate.maskedPhone || '-' }), _jsx(Descriptions.Item, { label: "\u62A5\u8003\u7B49\u7EA7", children: candidate.levelName || '-' }), _jsx(Descriptions.Item, { label: "\u8D44\u6599\u8FDB\u5EA6", children: candidateDocumentRatioTag(candidate) })] }), _jsx(Space, { style: { justifyContent: 'flex-start', width: '100%' }, children: _jsx(Select, { allowClear: true, placeholder: "\u6309\u8D44\u6599\u72B6\u6001\u7B5B\u9009", style: { width: 180 }, value: detailStatus, onChange: setDetailStatus, options: [
                                    { label: '未导入', value: 'missing' },
                                    { label: '待审核', value: 'pending_review' },
                                    { label: '已上传', value: 'uploaded' },
                                    { label: '已通过', value: 'approved' },
                                    { label: '已驳回', value: 'rejected' },
                                ] }) }), _jsx(Table, { rowKey: "id", size: "small", columns: documentColumns, dataSource: documentsQuery.data?.items || [], loading: documentsQuery.isFetching, scroll: { x: 1000 }, pagination: false })] })) : null }), _jsx(Modal, { title: uploadingDocument?.fileId ? '修改学员资料' : '上传学员资料', open: uploadModalOpen, onOk: submitUpload, onCancel: () => {
                    setUploadModalOpen(false);
                    setUploadingDocument(null);
                }, confirmLoading: saveMutation.isPending || uploadingFile, width: 720, destroyOnHidden: true, children: _jsxs(Form, { form: uploadForm, layout: "vertical", children: [_jsx(Form.Item, { name: "candidateId", hidden: true, rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "docCode", label: "\u8D44\u6599\u7C7B\u578B", rules: [{ required: true, message: '请选择资料类型' }], children: _jsx(Input, { disabled: true }) }), _jsx(Form.Item, { name: "docName", label: "\u8D44\u6599\u540D\u79F0", rules: [{ required: true, message: '请输入资料名称' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "required", label: "\u662F\u5426\u5FC5\u4F20", children: _jsx(Select, { options: [{ label: '是', value: true }, { label: '否', value: false }] }) }), _jsxs(Form.Item, { label: "\u6587\u4EF6", children: [_jsx(Upload, { ...uploadProps, accept: acceptByDocCode(uploadingDocument?.docCode), children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), loading: uploadingFile, children: uploadingDocument?.fileId ? '修改' : '上传' }) }), localPreviewUrl ? (_jsx("div", { style: { marginTop: 12 }, children: _jsx(Image, { src: localPreviewUrl, alt: "\u4E0A\u4F20\u9884\u89C8", style: { maxWidth: 240, maxHeight: 320 } }) })) : null] }), _jsx(Form.Item, { name: "fileId", label: "\u6587\u4EF6ID", rules: [{ required: true, message: '请先上传文件' }], children: _jsx(Input, { disabled: true }) }), _jsx(Form.Item, { name: "fileName", label: "\u6587\u4EF6\u540D", rules: [{ required: true, message: '请先上传文件' }], children: _jsx(Input, { disabled: true }) }), _jsx(Form.Item, { name: "fileType", label: "\u6587\u4EF6\u7C7B\u578B", children: _jsx(Input, { disabled: true }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input, {}) })] }) }), _jsx(FilePreviewModal, { open: Boolean(previewTarget), target: previewTarget, onClose: () => setPreviewTarget(null) }), _jsx(Modal, { title: "\u8D44\u6599\u5BA1\u6838", open: Boolean(reviewingDocument), onOk: submitReview, onCancel: () => setReviewingDocument(null), confirmLoading: reviewMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: reviewForm, layout: "vertical", children: [_jsx(Form.Item, { name: "status", label: "\u5BA1\u6838\u7ED3\u679C", rules: [{ required: true, message: '请选择审核结果' }], children: _jsx(Select, { options: [{ label: '通过', value: 'approved' }, { label: '驳回', value: 'rejected' }] }) }), _jsx(Form.Item, { name: "reviewComment", label: "\u5BA1\u6838\u610F\u89C1", children: _jsx(Input.TextArea, { rows: 4 }) })] }) })] }));
}
function acceptByDocCode(docCode) {
    if (['DOC002', 'DOC003', 'DOC004', 'DOC005', 'DOC006'].includes(docCode || '')) {
        return 'image/*';
    }
    return undefined;
}
function validateUploadFileType(docCode, file) {
    const extension = file.name.includes('.') ? file.name.substring(file.name.lastIndexOf('.')).toLowerCase() : '';
    if (['DOC002', 'DOC003', 'DOC004', 'DOC005', 'DOC006'].includes(docCode || '')) {
        const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp'];
        if (!file.type.startsWith('image/') && !imageExtensions.includes(extension)) {
            return '图片资料仅支持图片格式';
        }
    }
    return undefined;
}

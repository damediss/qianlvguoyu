import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Button, Card, Col, Descriptions, Drawer, Form, Input, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, Upload, message, } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined, DownloadOutlined, UploadOutlined, EyeOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadPlatformFile } from '../../api/files';
import { ApiAuthImage } from '../../components/ApiAuthImage';
import { FilePreviewModal } from '../../components/FilePreviewModal';
const STATUS_OPTIONS = [
    { value: 1, label: '启用' },
    { value: 0, label: '停用' },
];
const GENDER_OPTIONS = [
    { value: 1, label: '男' },
    { value: 0, label: '女' },
];
const SOURCE_LABEL = {
    legacy: '老系统迁移',
    excel: 'Excel 导入',
    manual: '手工录入',
};
/** 仅展示合法身份证号；占位文案显示为 — */
function displayIdCardNumber(value) {
    const v = value?.trim();
    if (!v)
        return '—';
    if (/^[0-9Xx]{15}$/.test(v) || /^[0-9Xx]{18}$/.test(v))
        return v;
    return '—';
}
function genderLabel(v) {
    if (v === 1)
        return '男';
    if (v === 0)
        return '女';
    return '—';
}
function filePreviewUrl(fileId) {
    if (fileId == null)
        return undefined;
    return `/api/files/${fileId}/download?inline=true`;
}
const FILE_SLOTS = [
    { type: 'ID_FRONT', label: '身份证正面', field: 'idCardFrontFileId' },
    { type: 'ID_BACK', label: '身份证反面', field: 'idCardBackFileId' },
    { type: 'GRADUATE', label: '毕业证', field: 'graduateFileId' },
    { type: 'EDUCATION', label: '学历证', field: 'educationFileId' },
    { type: 'TEACHER_QUAL', label: '教师资格证', field: 'teacherQualFileId' },
    { type: 'TITLE', label: '职称证', field: 'titleCertFileId' },
    { type: 'SKILL', label: '技能等级证书', field: 'skillCertFileId' },
];
function fillTeacherForm(form, row) {
    form.setFieldsValue({
        name: row.name,
        phone: row.phone,
        idCardNumber: displayIdCardNumber(row.idCardNumber) === '—' ? undefined : row.idCardNumber,
        company: row.company,
        gender: row.gender,
        nation: row.nation,
        birthday: row.birthday,
        startDate: row.startDate,
        endDate: row.endDate,
        education: row.education,
        major: row.major,
        skillName: row.skillName,
        qualificationLevel: row.qualificationLevel,
        professionalTitle: row.professionalTitle,
        certText: row.certText,
        teacherQualNo: row.teacherQualNo,
        titleCertNo: row.titleCertNo,
        skillCertNo: row.skillCertNo,
        status: row.status ?? 1,
        address: row.address,
    });
}
function LicensePhotos({ teacher, teacherId, onPreview, onUploaded }) {
    const bindMu = useMutation({
        mutationFn: ({ type, fileId }) => trainApi.bindTeacherBankFile(teacherId, type, fileId),
        onSuccess: () => {
            message.success('证照已更新');
            onUploaded();
        },
        onError: (e) => message.error(e.message || '绑定失败'),
    });
    const clearMu = useMutation({
        mutationFn: (type) => trainApi.clearTeacherBankFile(teacherId, type),
        onSuccess: () => {
            message.success('已清除');
            onUploaded();
        },
        onError: (e) => message.error(e.message || '清除失败'),
    });
    return (_jsx(Row, { gutter: [12, 12], children: FILE_SLOTS.map((slot) => {
            const fileId = teacher[slot.field];
            const url = filePreviewUrl(fileId);
            return (_jsx(Col, { xs: 24, sm: 12, md: 8, children: _jsxs(Card, { size: "small", title: slot.label, extra: fileId ? (_jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["#", fileId] })) : null, styles: { body: { padding: 12 } }, children: [_jsx("div", { style: {
                                height: 140,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                background: '#fafafa',
                                borderRadius: 6,
                                overflow: 'hidden',
                                cursor: fileId ? 'pointer' : 'default',
                                marginBottom: 8,
                            }, onClick: () => {
                                if (!fileId)
                                    return;
                                onPreview({ fileId: String(fileId), fileName: slot.label });
                            }, children: url ? (_jsx(ApiAuthImage, { url: url, alt: slot.label, style: { maxWidth: '100%', maxHeight: 140, objectFit: 'contain' }, preview: false })) : (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u5F71\u50CF" })) }), _jsxs(Space, { size: "small", wrap: true, children: [_jsx(Upload, { accept: "image/*,.pdf", showUploadList: false, beforeUpload: async (file) => {
                                        try {
                                            const uploaded = await uploadPlatformFile(file, 'private');
                                            bindMu.mutate({ type: slot.type, fileId: uploaded.id });
                                        }
                                        catch (e) {
                                            message.error(e instanceof Error ? e.message : '上传失败');
                                        }
                                        return false;
                                    }, children: _jsx(Button, { size: "small", loading: bindMu.isPending, children: fileId ? '更换' : '上传' }) }), fileId ? (_jsxs(_Fragment, { children: [_jsx(Button, { size: "small", onClick: () => onPreview({ fileId: String(fileId), fileName: slot.label }), children: "\u67E5\u770B" }), _jsx(Button, { size: "small", danger: true, loading: clearMu.isPending, onClick: () => clearMu.mutate(slot.type), children: "\u6E05\u9664" })] })) : null] })] }) }, slot.type));
        }) }));
}
export function TrainingAcademicManagePage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [detailId, setDetailId] = useState(null);
    const [previewTarget, setPreviewTarget] = useState(null);
    const { data, isLoading, refetch } = useQuery({
        queryKey: ['teacher-banks', page, pageSize, keyword],
        queryFn: () => trainApi.pageTeacherBanks({ page, pageSize, keyword: keyword || undefined }),
    });
    const { data: detail, isLoading: detailLoading } = useQuery({
        queryKey: ['teacher-bank-detail', detailId],
        queryFn: () => trainApi.getTeacherBank(detailId),
        enabled: detailId != null,
    });
    const { data: editingDetail, isLoading: editingLoading } = useQuery({
        queryKey: ['teacher-bank-detail', editingId],
        queryFn: () => trainApi.getTeacherBank(editingId),
        enabled: editingId != null && modalOpen,
    });
    const invalidateTeacher = (id) => {
        queryClient.invalidateQueries({ queryKey: ['teacher-banks'] });
        if (id != null) {
            queryClient.invalidateQueries({ queryKey: ['teacher-bank-detail', id] });
        }
        else {
            queryClient.invalidateQueries({ queryKey: ['teacher-bank-detail'] });
        }
    };
    const createMu = useMutation({
        mutationFn: (payload) => trainApi.createTeacherBank(payload),
        onSuccess: () => {
            message.success('添加成功');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            invalidateTeacher();
        },
        onError: (e) => message.error(e.message || '添加失败'),
    });
    const updateMu = useMutation({
        mutationFn: ({ id, payload }) => trainApi.updateTeacherBank(id, payload),
        onSuccess: (_res, vars) => {
            message.success('保存成功');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            invalidateTeacher(vars.id);
        },
        onError: (e) => message.error(e.message || '保存失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteTeacherBank(id),
        onSuccess: () => {
            message.success('删除成功');
            invalidateTeacher();
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    const importMu = useMutation({
        mutationFn: (file) => trainApi.importTeacherBanks(file),
        onSuccess: (res) => {
            message.success(`导入完成：成功 ${res.success}（新建 ${res.created} / 更新 ${res.updated} / 临时 ${res.provisional}），失败 ${res.failed?.length ?? 0}`);
            invalidateTeacher();
        },
        onError: (e) => message.error(e.message || '导入失败'),
    });
    const migrateMu = useMutation({
        mutationFn: (file) => trainApi.migrateTeacherBanksFromLegacy(file, true),
        onSuccess: (res) => {
            message.success(`迁移完成：成功 ${res.success}（新建 ${res.created} / 更新 ${res.updated}），失败 ${res.failed?.length ?? 0}，警告 ${res.warnings?.length ?? 0}`);
            invalidateTeacher();
        },
        onError: (e) => message.error(e.message || '迁移失败'),
    });
    const openCreate = () => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: 1 });
        setModalOpen(true);
    };
    const openEdit = async (id) => {
        setEditingId(id);
        setModalOpen(true);
        try {
            const row = await trainApi.getTeacherBank(id);
            fillTeacherForm(form, row);
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '加载失败');
            setModalOpen(false);
            setEditingId(null);
        }
    };
    const submit = async () => {
        const v = await form.validateFields();
        if (editingId != null) {
            updateMu.mutate({ id: editingId, payload: v });
        }
        else {
            createMu.mutate(v);
        }
    };
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 72 },
        {
            title: '姓名',
            dataIndex: 'name',
            width: 100,
            render: (v, row) => (_jsxs(Space, { size: 4, children: [_jsx("span", { children: v }), row.provisional ? _jsx(Tag, { color: "orange", children: "\u5F85\u8865\u8EAB\u4EFD\u8BC1" }) : null] })),
        },
        { title: '手机号', dataIndex: 'phone', width: 120, render: (v) => v || '—' },
        {
            title: '身份证号',
            width: 170,
            render: (_, row) => displayIdCardNumber(row.idCardNumber),
        },
        { title: '学历', dataIndex: 'education', width: 100, render: (v) => v || '—' },
        { title: '专业', dataIndex: 'major', ellipsis: true, render: (v) => v || '—' },
        {
            title: '职务',
            dataIndex: 'professionalTitle',
            width: 110,
            ellipsis: true,
            render: (v) => v || '—',
        },
        {
            title: '证照',
            dataIndex: 'fileReadyCount',
            width: 72,
            render: (v) => `${v ?? 0}/7`,
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 72,
            render: (v) => (v === 0 ? '停用' : '启用'),
        },
        {
            title: '操作',
            key: 'act',
            width: 220,
            fixed: 'right',
            render: (_, row) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => setDetailId(row.id), children: "\u8BE6\u60C5" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => void openEdit(row.id), children: "\u4FEE\u6539" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => deleteMu.mutate(row.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    const teacherFormFields = (_jsx(Form, { form: form, layout: "vertical", children: _jsxs(Row, { gutter: 16, children: [_jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "name", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, { maxLength: 20 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", children: _jsx(Input, { maxLength: 11 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "idCardNumber", label: "\u8EAB\u4EFD\u8BC1\u53F7", children: _jsx(Input, { maxLength: 18, placeholder: "15 \u6216 18 \u4F4D" }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "gender", label: "\u6027\u522B", children: _jsx(Select, { allowClear: true, options: GENDER_OPTIONS, placeholder: "\u8BF7\u9009\u62E9" }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "nation", label: "\u6C11\u65CF", children: _jsx(Input, { maxLength: 32 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "birthday", label: "\u51FA\u751F\u65E5\u671F", children: _jsx(Input, { maxLength: 32, placeholder: "\u5982 1990-01-01" }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "startDate", label: "\u8EAB\u4EFD\u8BC1\u6709\u6548\u671F\u8D77", children: _jsx(Input, { maxLength: 32 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "endDate", label: "\u8EAB\u4EFD\u8BC1\u6709\u6548\u671F\u6B62", children: _jsx(Input, { maxLength: 32 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", initialValue: 1, children: _jsx(Select, { options: STATUS_OPTIONS }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "company", label: "\u5DE5\u4F5C\u5355\u4F4D", children: _jsx(Input, { maxLength: 255 }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "address", label: "\u5730\u5740", children: _jsx(Input, { maxLength: 500 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "education", label: "\u5B66\u5386", children: _jsx(Input, { maxLength: 50 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "major", label: "\u4E13\u4E1A", children: _jsx(Input, { maxLength: 50 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "professionalTitle", label: "\u4E13\u4E1A\u6280\u672F\u804C\u52A1", children: _jsx(Input, { maxLength: 255 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "qualificationLevel", label: "\u804C\u4E1A\u8D44\u683C\u7B49\u7EA7", children: _jsx(Input, { maxLength: 255 }) }) }), _jsx(Col, { span: 16, children: _jsx(Form.Item, { name: "skillName", label: "\u804C\u4E1A\u6280\u80FD\u540D\u79F0", children: _jsx(Input, { maxLength: 512 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "teacherQualNo", label: "\u6559\u5E08\u8D44\u683C\u8BC1\u53F7", children: _jsx(Input, { maxLength: 128 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "titleCertNo", label: "\u804C\u79F0\u8BC1\u53F7", children: _jsx(Input, { maxLength: 128 }) }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "skillCertNo", label: "\u6280\u80FD\u7B49\u7EA7\u8BC1\u53F7", children: _jsx(Input, { maxLength: 255 }) }) }), _jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "certText", label: "\u8D44\u683C\u8BC1\u4E66\u539F\u6587", children: _jsx(Input.TextArea, { rows: 3, placeholder: "\u6301\u6709\u4F55\u79CD\u804C\u4E1A\u8D44\u683C\u8BC1\u4E66\u7B49\u539F\u6587" }) }) })] }) }));
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u6559\u52A1\u7BA1\u7406" }), _jsx(Typography.Text, { type: "secondary", children: "\u5E08\u8D44\u5E93\uFF1AExcel \u5BFC\u5165\u3001\u8001\u7CFB\u7EDF JSON \u8FC1\u79FB\u3001\u8BC1\u7167\u5F71\u50CF\u7BA1\u7406\uFF08train_teacher_bank\uFF09" })] }) }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Space, { wrap: true, children: [_jsx(Input, { placeholder: "\u59D3\u540D/\u624B\u673A/\u8EAB\u4EFD\u8BC1/\u5B66\u5386/\u4E13\u4E1A", prefix: _jsx(SearchOutlined, {}), value: keyword, onChange: (e) => setKeyword(e.target.value), onPressEnter: () => {
                                setPage(1);
                                void refetch();
                            }, style: { width: 260 }, allowClear: true }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => {
                                setPage(1);
                                void refetch();
                            }, children: "\u67E5\u8BE2" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u6DFB\u52A0" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: () => void trainApi.downloadTeacherBankImportTemplate().catch((e) => message.error(e.message || '下载失败')), children: "\u4E0B\u8F7D\u6A21\u677F" }), _jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                importMu.mutate(file);
                                return false;
                            }, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), loading: importMu.isPending, children: "\u5BFC\u5165 Excel" }) }), _jsx(Upload, { accept: ".json,application/json", showUploadList: false, beforeUpload: (file) => {
                                Modal.confirm({
                                    title: '从老系统 JSON 迁移？',
                                    content: '将按身份证 upsert；图片会尝试从 URL 转存。耗时可能较长。',
                                    onOk: () => migrateMu.mutateAsync(file),
                                });
                                return false;
                            }, children: _jsx(Button, { loading: migrateMu.isPending, children: "\u8FC1\u79FB legacy JSON" }) }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void refetch(), children: "\u5237\u65B0" })] }) }), _jsx(Card, { size: "small", children: _jsx(Table, { rowKey: "id", loading: isLoading, columns: columns, dataSource: data?.items ?? [], scroll: { x: 1200 }, pagination: {
                        current: page,
                        pageSize,
                        total: data?.total ?? 0,
                        showSizeChanger: true,
                        pageSizeOptions: ['10', '20', '50', '100'],
                        onChange: (p, size) => {
                            if (typeof size === 'number' && size > 0 && size !== pageSize) {
                                setPageSize(size);
                                setPage(1);
                            }
                            else {
                                setPage(p);
                            }
                        },
                        showTotal: (t) => `共 ${t} 人`,
                    } }) }), _jsxs(Modal, { title: editingId != null ? '修改师资信息' : '添加师资', open: modalOpen, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                }, onOk: () => void submit(), confirmLoading: createMu.isPending || updateMu.isPending, destroyOnHidden: true, width: 920, styles: { body: { maxHeight: '72vh', overflowY: 'auto' } }, children: [teacherFormFields, editingId != null ? (_jsx(Card, { size: "small", title: `证照影像（${editingDetail?.fileReadyCount ?? 0}/7）`, style: { marginTop: 8 }, loading: editingLoading && !editingDetail, children: editingDetail ? (_jsx(LicensePhotos, { teacher: editingDetail, teacherId: editingId, onPreview: setPreviewTarget, onUploaded: () => invalidateTeacher(editingId) })) : (_jsx(Typography.Text, { type: "secondary", children: "\u52A0\u8F7D\u8BC1\u7167\u4E2D\u2026" })) })) : (_jsx(Typography.Paragraph, { type: "secondary", style: { marginTop: 8, marginBottom: 0 }, children: "\u65B0\u5EFA\u4FDD\u5B58\u540E\uFF0C\u53EF\u5728\u8BE6\u60C5\u6216\u4FEE\u6539\u9875\u4E0A\u4F20\u8BC1\u7167\u7167\u7247\u3002" }))] }), _jsx(Drawer, { title: detail?.name ? `师资详情 · ${detail.name}` : '师资详情', width: 960, open: detailId != null, onClose: () => setDetailId(null), destroyOnHidden: true, extra: detailId != null ? (_jsx(Button, { type: "primary", icon: _jsx(EditOutlined, {}), onClick: () => {
                        const id = detailId;
                        setDetailId(null);
                        void openEdit(id);
                    }, children: "\u4FEE\u6539" })) : null, children: detailLoading || !detail ? (_jsx(Typography.Text, { type: "secondary", children: "\u52A0\u8F7D\u4E2D\u2026" })) : (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsxs(Card, { size: "small", title: "\u57FA\u672C\u4FE1\u606F", children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [detail.provisional ? _jsx(Tag, { color: "orange", children: "\u5F85\u8865\u8EAB\u4EFD\u8BC1" }) : null, _jsx(Tag, { children: detail.status === 0 ? '停用' : '启用' }), detail.source ? (_jsx(Tag, { color: "blue", children: SOURCE_LABEL[detail.source] || detail.source })) : null, detail.legacyId != null ? _jsxs(Tag, { children: ["legacy_id=", detail.legacyId] }) : null, _jsxs(Tag, { children: ["\u8BC1\u7167 ", detail.fileReadyCount ?? 0, "/7"] })] }), _jsxs(Descriptions, { column: { xs: 1, sm: 2, md: 3 }, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u59D3\u540D", children: detail.name || '—' }), _jsx(Descriptions.Item, { label: "\u624B\u673A", children: detail.phone || '—' }), _jsx(Descriptions.Item, { label: "\u8EAB\u4EFD\u8BC1\u53F7", children: displayIdCardNumber(detail.idCardNumber) }), _jsx(Descriptions.Item, { label: "\u6027\u522B", children: genderLabel(detail.gender) }), _jsx(Descriptions.Item, { label: "\u6C11\u65CF", children: detail.nation || '—' }), _jsx(Descriptions.Item, { label: "\u51FA\u751F\u65E5\u671F", children: detail.birthday || '—' }), _jsx(Descriptions.Item, { label: "\u8EAB\u4EFD\u8BC1\u6709\u6548\u671F\u8D77", children: detail.startDate || '—' }), _jsx(Descriptions.Item, { label: "\u8EAB\u4EFD\u8BC1\u6709\u6548\u671F\u6B62", children: detail.endDate || '—' }), _jsx(Descriptions.Item, { label: "\u5DE5\u4F5C\u5355\u4F4D", span: 2, children: detail.company || '—' }), _jsx(Descriptions.Item, { label: "\u5730\u5740", span: 3, children: detail.address || '—' }), _jsx(Descriptions.Item, { label: "\u5B66\u5386", children: detail.education || '—' }), _jsx(Descriptions.Item, { label: "\u4E13\u4E1A", children: detail.major || '—' }), _jsx(Descriptions.Item, { label: "\u4E13\u4E1A\u6280\u672F\u804C\u52A1", children: detail.professionalTitle || '—' }), _jsx(Descriptions.Item, { label: "\u804C\u4E1A\u8D44\u683C\u7B49\u7EA7", children: detail.qualificationLevel || '—' }), _jsx(Descriptions.Item, { label: "\u804C\u4E1A\u6280\u80FD\u540D\u79F0", span: 2, children: detail.skillName || '—' }), _jsx(Descriptions.Item, { label: "\u6559\u5E08\u8D44\u683C\u8BC1\u53F7", children: detail.teacherQualNo || '—' }), _jsx(Descriptions.Item, { label: "\u804C\u79F0\u8BC1\u53F7", children: detail.titleCertNo || '—' }), _jsx(Descriptions.Item, { label: "\u6280\u80FD\u7B49\u7EA7\u8BC1\u53F7", children: detail.skillCertNo || '—' }), _jsx(Descriptions.Item, { label: "\u8D44\u683C\u8BC1\u4E66\u539F\u6587", span: 3, children: _jsx(Typography.Paragraph, { style: { marginBottom: 0, whiteSpace: 'pre-wrap' }, type: detail.certText ? undefined : 'secondary', children: detail.certText || '—' }) })] })] }), _jsx(Card, { size: "small", title: `证照影像（${detail.fileReadyCount ?? 0}/7）`, children: _jsx(LicensePhotos, { teacher: detail, teacherId: detail.id, onPreview: setPreviewTarget, onUploaded: () => invalidateTeacher(detail.id) }) })] })) }), _jsx(FilePreviewModal, { open: previewTarget != null, target: previewTarget, onClose: () => setPreviewTarget(null) })] }));
}

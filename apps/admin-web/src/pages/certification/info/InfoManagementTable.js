import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, ExportOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, Typography, Upload, message } from 'antd';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { uploadPlatformFile } from '../../../api/files';
import { treeWorkTypes } from '../../../api/exam';
import { FilePreviewModal } from '../../../components/FilePreviewModal';
import { LevelScopeRangeSelect } from '../../../components/LevelScopeRangeSelect';
import { PermissionGate } from '../../../directives/permission';
import { flattenAssessorWorkTypeOptions } from '../../../utils/workTypeOptions';
const auditOptions = [
    { label: '待审核', value: 'draft' },
    { label: '待审核', value: 'pending' },
    { label: '已入库', value: 'approved' },
    { label: '已驳回', value: 'rejected' },
    { label: '已停用', value: 'disabled' },
];
const auditColor = {
    draft: 'default',
    pending: 'processing',
    approved: 'success',
    rejected: 'error',
    disabled: 'warning',
};
const deletableAuditStatuses = ['draft', 'rejected', 'disabled'];
function isRecordDeletable(record) {
    if (deletableAuditStatuses.includes(record.auditStatus)) {
        return true;
    }
    return record.workStatus === 'disabled'
        || record.status === 'disabled'
        || record.equipmentStatus === 'disabled';
}
export function InfoManagementTable({ title, queryKey, permissionPrefix, rowKey, statusField, columns, auditColumnIndex, fields, statusOptions, editableAuditStatuses = ['draft', 'rejected'], showStatusFilter = true, pageApi, createApi, updateApi, submitApi, reviewApi, disableApi, enableApi, deleteApi, createAccountApi, toolbarExtra, }) {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [reviewForm] = Form.useForm();
    const [query, setQuery] = useState({ page: 1, pageSize: 10 });
    const [modalMode, setModalMode] = useState(null);
    const [current, setCurrent] = useState(null);
    const [reviewing, setReviewing] = useState(null);
    const [accountRecord, setAccountRecord] = useState(null);
    const [accountForm] = Form.useForm();
    const dataQuery = useQuery({
        queryKey: [queryKey, query],
        queryFn: () => pageApi(query),
    });
    const hasWorkTypeField = fields.some((field) => field.type === 'work-type-multiple');
    const workTypeQuery = useQuery({
        queryKey: ['exam-work-types-tree'],
        queryFn: treeWorkTypes,
        enabled: hasWorkTypeField,
        staleTime: 5 * 60 * 1000,
    });
    const workTypeOptions = useMemo(() => flattenAssessorWorkTypeOptions(workTypeQuery.data ?? []), [workTypeQuery.data]);
    const invalidate = () => queryClient.invalidateQueries({ queryKey: [queryKey] });
    const saveMutation = useMutation({
        mutationFn: async (values) => {
            const normalized = normalizePayload(values, fields, workTypeOptions);
            return current ? updateApi(current.id, normalized) : createApi(normalized);
        },
        onSuccess: () => {
            message.success('保存成功');
            setModalMode(null);
            setCurrent(null);
            invalidate();
        },
    });
    const actionMutation = useMutation({
        mutationFn: async (task) => {
            if (task.action === 'submit')
                return submitApi(task.record.id);
            if (task.action === 'disable')
                return disableApi(task.record.id, { reason: task.reason });
            if (task.action === 'enable')
                return enableApi(task.record.id);
            return deleteApi(task.record.id);
        },
        onSuccess: () => {
            message.success('操作成功');
            invalidate();
        },
    });
    const reviewMutation = useMutation({
        mutationFn: (payload) => reviewApi(reviewing.id, payload),
        onSuccess: () => {
            message.success('审核完成');
            setReviewing(null);
            reviewForm.resetFields();
            invalidate();
        },
    });
    const accountMutation = useMutation({
        mutationFn: (payload) => createAccountApi(accountRecord.id, payload),
        onSuccess: () => {
            message.success('平板登录账号已创建');
            setAccountRecord(null);
            accountForm.resetFields();
            invalidate();
        },
    });
    const finalColumns = useMemo(() => {
        const auditColumn = {
            title: '审核状态',
            dataIndex: 'auditStatus',
            width: 120,
            render: (value) => _jsx(Tag, { color: auditColor[value] || 'default', children: labelOf(auditOptions, value) }),
        };
        const actionColumn = {
            title: '操作',
            width: 260,
            fixed: 'right',
            render: (_, record) => (_jsxs(Space, { size: 4, wrap: true, children: [_jsx(Button, { size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openModal('view', record), children: "\u67E5\u770B" }), createAccountApi && !record.userId ? (_jsx(PermissionGate, { code: `${permissionPrefix}.update`, children: _jsx(Button, { size: "small", onClick: () => setAccountRecord(record), children: "\u521B\u5EFA\u8D26\u53F7" }) })) : null, _jsx(PermissionGate, { code: `${permissionPrefix}.update`, children: _jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), disabled: !editableAuditStatuses.includes(record.auditStatus), onClick: () => openModal('edit', record), children: "\u7F16\u8F91" }) }), _jsx(PermissionGate, { code: `${permissionPrefix}.submit`, children: _jsx(Button, { size: "small", disabled: !['draft', 'rejected'].includes(record.auditStatus), onClick: () => actionMutation.mutate({ action: 'submit', record }), children: "\u63D0\u4EA4" }) }), _jsx(PermissionGate, { code: `${permissionPrefix}.review`, children: _jsx(Button, { size: "small", disabled: record.auditStatus !== 'pending', onClick: () => setReviewing(record), children: "\u5BA1\u6838" }) }), _jsx(PermissionGate, { code: `${permissionPrefix}.update`, children: record.auditStatus === 'disabled' ? (_jsx(Popconfirm, { title: "\u786E\u8BA4\u542F\u7528\u8BE5\u8BB0\u5F55\uFF1F", onConfirm: () => actionMutation.mutate({ action: 'enable', record }), children: _jsx(Button, { size: "small", type: "primary", children: "\u542F\u7528" }) })) : (_jsx(Popconfirm, { title: "\u786E\u8BA4\u505C\u7528\u8BE5\u8BB0\u5F55\uFF1F", onConfirm: () => actionMutation.mutate({ action: 'disable', record, reason: '主管停用' }), children: _jsx(Button, { size: "small", disabled: record.auditStatus !== 'approved', children: "\u505C\u7528" }) })) }), _jsx(PermissionGate, { code: `${permissionPrefix}.delete`, children: _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u8BB0\u5F55\uFF1F", onConfirm: () => actionMutation.mutate({ action: 'delete', record }), children: _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), disabled: !isRecordDeletable(record) }) }) })] })),
        };
        const base = [...columns];
        const index = typeof auditColumnIndex === 'number' && Number.isFinite(auditColumnIndex)
            ? Math.max(0, Math.min(auditColumnIndex, base.length))
            : base.length;
        base.splice(index, 0, auditColumn);
        base.push(actionColumn);
        return base;
    }, [actionMutation, auditColumnIndex, columns, createAccountApi, editableAuditStatuses, permissionPrefix]);
    const openModal = (mode, record) => {
        setModalMode(mode);
        setCurrent(record || null);
        form.resetFields();
        if (record) {
            const values = { ...record };
            for (const field of fields) {
                if (field.type === 'date' && typeof values[field.name] === 'string') {
                    values[field.name] = dayjs(values[field.name]);
                }
                if (field.name === 'phone' && !values.phone && values.maskedPhone) {
                    values.phone = values.maskedPhone;
                }
            }
            form.setFieldsValue(values);
        }
    };
    return (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsxs(Space, { align: "center", style: { justifyContent: 'space-between', width: '100%' }, children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: title }), _jsx(Typography.Text, { type: "secondary", children: "\u57FA\u7840\u6863\u6848\u5EFA\u6863\u3001\u63D0\u4EA4\u5BA1\u6838\u3001\u4E3B\u7BA1\u5BA1\u6838\u548C\u72B6\u6001\u7EF4\u62A4" })] }), _jsxs(Space, { children: [toolbarExtra, _jsx(PermissionGate, { code: `${permissionPrefix}.export`, children: _jsx(Button, { icon: _jsx(ExportOutlined, {}), onClick: () => message.info('导出任务已提交'), children: "\u5BFC\u51FA" }) }), _jsx(PermissionGate, { code: `${permissionPrefix}.create`, children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => openModal('create'), children: "\u65B0\u589E" }) })] })] }), _jsxs(Space, { wrap: true, children: [_jsx(Input.Search, { allowClear: true, placeholder: "\u5173\u952E\u8BCD\u641C\u7D22", style: { width: 260 }, onSearch: (keyword) => setQuery((old) => ({ ...old, keyword, page: 1 })) }), _jsx(Select, { allowClear: true, placeholder: "\u5BA1\u6838\u72B6\u6001", options: auditOptions, style: { width: 160 }, onChange: (auditStatus) => setQuery((old) => ({ ...old, auditStatus, page: 1 })) }), showStatusFilter && statusOptions.length > 0 ? (_jsx(Select, { allowClear: true, placeholder: "\u8FD0\u884C\u72B6\u6001", options: statusOptions, style: { width: 180 }, onChange: (status) => setQuery((old) => ({ ...old, status, page: 1 })) })) : null] }), _jsx(Table, { rowKey: rowKey, columns: finalColumns, dataSource: dataQuery.data?.items || [], loading: dataQuery.isLoading, scroll: { x: 1180 }, pagination: {
                    current: query.page,
                    pageSize: query.pageSize,
                    total: dataQuery.data?.total || 0,
                    showSizeChanger: true,
                    onChange: (page, pageSize) => setQuery((old) => ({ ...old, page, pageSize })),
                } }), _jsx(Modal, { title: modalTitle(title, modalMode), open: !!modalMode, onCancel: () => setModalMode(null), onOk: () => form.submit(), okButtonProps: { disabled: modalMode === 'view', loading: saveMutation.isPending }, width: 760, children: _jsxs(Form, { form: form, layout: "vertical", disabled: modalMode === 'view', onFinish: (values) => saveMutation.mutate(values), children: [current?.workTypeMappingWarning ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u5386\u53F2\u5DE5\u79CD\u6388\u6743\u9700\u8981\u4FEE\u6B63", description: current.workTypeMappingWarning })) : null, fields.filter((field) => !(modalMode === 'view' && field.hideInView)
                            && !(modalMode === 'edit' && field.hideInEdit)).map((field) => (_jsx(Form.Item, { name: field.name, label: field.label, hidden: field.hidden, rules: [{
                                    required: field.required || (modalMode === 'create' && field.createRequired),
                                    message: `请填写${field.label}`,
                                }], children: renderField(field, form, modalMode === 'view', workTypeOptions, workTypeQuery.isLoading, workTypeQuery.isError) }, field.name)))] }) }), _jsx(Modal, { title: `主管审核 - ${reviewing ? String(reviewing[statusField]) : ''}`, open: !!reviewing, onCancel: () => setReviewing(null), footer: [
                    _jsx(Button, { danger: true, loading: reviewMutation.isPending, onClick: () => reviewForm.validateFields().then((values) => reviewMutation.mutate({ reviewAction: 'reject', comment: values.comment })), children: "\u9A73\u56DE" }, "reject"),
                    _jsx(Button, { type: "primary", loading: reviewMutation.isPending, onClick: () => reviewMutation.mutate({ reviewAction: 'pass' }), children: "\u901A\u8FC7" }, "pass"),
                ], children: _jsx(Form, { form: reviewForm, layout: "vertical", children: _jsx(Form.Item, { name: "comment", label: "\u9A73\u56DE\u539F\u56E0", rules: [{ min: 6, message: '驳回原因不少于 6 个字符' }], children: _jsx(Input.TextArea, { rows: 4, placeholder: "\u9A73\u56DE\u65F6\u5FC5\u586B" }) }) }) }), _jsx(Modal, { title: `创建平板账号 - ${accountRecord ? String(accountRecord[statusField]) : ''}`, open: Boolean(accountRecord), confirmLoading: accountMutation.isPending, onCancel: () => {
                    setAccountRecord(null);
                    accountForm.resetFields();
                }, onOk: () => accountForm.validateFields().then((values) => accountMutation.mutate(values)), children: _jsxs(Form, { form: accountForm, layout: "vertical", children: [_jsx(Form.Item, { name: "username", label: "\u5E73\u677F\u767B\u5F55\u8D26\u53F7", rules: [{ required: true, message: '请输入平板登录账号' }], children: _jsx(Input, { autoComplete: "off" }) }), _jsx(Form.Item, { name: "password", label: "\u5E73\u677F\u767B\u5F55\u5BC6\u7801", rules: [
                                { required: true, message: '请输入平板登录密码' },
                                { min: 8, message: '密码至少 8 位' },
                            ], children: _jsx(Input.Password, { autoComplete: "new-password" }) })] }) })] }));
}
function renderField(field, form, readOnly = false, workTypeOptions = [], workTypeLoading = false, workTypeError = false) {
    if (field.type === 'password')
        return readOnly ? _jsx(SecretText, {}) : _jsx(Input.Password, { autoComplete: "new-password" });
    if (field.type === 'textarea')
        return _jsx(Input.TextArea, { rows: 3 });
    if (field.type === 'date')
        return _jsx(DatePicker, { style: { width: '100%' } });
    if (field.type === 'number')
        return _jsx(InputNumber, { min: 1, style: { width: '100%' } });
    if (field.type === 'select')
        return _jsx(Select, { options: field.options || [] });
    if (field.type === 'select-multiple')
        return (_jsx(Select, { mode: "tags", allowClear: true, showSearch: true, maxTagCount: "responsive", placeholder: field.placeholder, tokenSeparators: [',', '，', '、'] }));
    if (field.type === 'work-type-multiple')
        return (_jsx(Select, { mode: "multiple", allowClear: true, showSearch: true, optionFilterProp: "label", maxTagCount: "responsive", placeholder: field.placeholder, options: workTypeOptions, loading: workTypeLoading, notFoundContent: workTypeError ? '工种数据加载失败' : undefined }));
    if (field.type === 'levelRange')
        return _jsx(LevelScopeRangeSelect, {});
    if (field.type === 'imageUpload')
        return _jsx(ImageUploadField, { field: field, form: form, readOnly: readOnly });
    return _jsx(Input, {});
}
function SecretText({ value }) {
    if (!value) {
        return _jsx(Typography.Text, { type: "secondary", children: "\u672A\u8BB0\u5F55" });
    }
    return _jsx(Typography.Text, { copyable: true, code: true, children: value });
}
function ImageUploadField({ field, form, value, onChange, readOnly }) {
    const [previewTarget, setPreviewTarget] = useState(null);
    const fileName = field.fileNameField ? form.getFieldValue(field.fileNameField) : undefined;
    const fileList = value
        ? [{ uid: String(value), name: fileName || '设备照片', status: 'done' }]
        : [];
    const openPreview = () => {
        const fileId = String(value || '');
        if (!fileId) {
            return;
        }
        setPreviewTarget({
            fileId,
            fileName: fileName || '设备照片',
            fileType: 'image/*',
        });
    };
    if (readOnly) {
        return (_jsxs(_Fragment, { children: [value ? (_jsx(Button, { type: "link", size: "small", style: { paddingInline: 0 }, disabled: false, onClick: openPreview, children: "\u67E5\u770B\u7167\u7247" })) : (_jsx(Typography.Text, { type: "secondary", children: "-" })), _jsx(FilePreviewModal, { open: Boolean(previewTarget), target: previewTarget, onClose: () => setPreviewTarget(null) })] }));
    }
    return (_jsxs(_Fragment, { children: [_jsx(Upload, { accept: "image/*", disabled: false, maxCount: 1, fileList: fileList, beforeUpload: async (file) => {
                    if (readOnly) {
                        return Upload.LIST_IGNORE;
                    }
                    if (!file.type.startsWith('image/')) {
                        message.warning('请上传图片文件');
                        return Upload.LIST_IGNORE;
                    }
                    const uploaded = await uploadPlatformFile(file, 'private');
                    onChange?.(String(uploaded.id));
                    if (field.fileNameField) {
                        form.setFieldValue(field.fileNameField, uploaded.originalName);
                    }
                    message.success('设备照片已上传');
                    return Upload.LIST_IGNORE;
                }, onPreview: (file) => {
                    const fileId = String(file.uid || value || '');
                    if (!fileId) {
                        return;
                    }
                    setPreviewTarget({
                        fileId,
                        fileName: file.name || fileName || '设备照片',
                        fileType: 'image/*',
                    });
                }, onRemove: () => {
                    if (readOnly) {
                        return false;
                    }
                    onChange?.(undefined);
                    if (field.fileNameField) {
                        form.setFieldValue(field.fileNameField, undefined);
                    }
                }, showUploadList: {
                    showPreviewIcon: true,
                    showRemoveIcon: !readOnly,
                }, listType: "picture", children: _jsx(Button, { children: "\u4E0A\u4F20\u7167\u7247" }) }), _jsx(FilePreviewModal, { open: Boolean(previewTarget), target: previewTarget, onClose: () => setPreviewTarget(null) })] }));
}
function normalizePayload(values, fields, workTypeOptions) {
    const next = { ...values };
    for (const field of fields) {
        if (field.type === 'date' && next[field.name] && dayjs.isDayjs(next[field.name])) {
            next[field.name] = next[field.name].format('YYYY-MM-DD');
        }
    }
    if (fields.some((field) => field.type === 'work-type-multiple')) {
        const selectedIds = Array.isArray(next.workTypeIds)
            ? next.workTypeIds.map(String).filter((id) => workTypeOptions.some((option) => option.value === id))
            : [];
        const selectedPaths = selectedIds
            .map((id) => workTypeOptions.find((option) => option.value === id)?.label)
            .filter((path) => Boolean(path));
        next.workTypeIds = selectedIds;
        next.workTypeScopes = selectedPaths;
        next.workTypeScope = selectedPaths.join('、');
    }
    return next;
}
function labelOf(options, value) {
    return options.find((item) => item.value === value)?.label || value || '-';
}
function modalTitle(title, mode) {
    if (mode === 'create')
        return `新增${title}`;
    if (mode === 'edit')
        return `编辑${title}`;
    if (mode === 'view')
        return `查看${title}`;
    return title;
}

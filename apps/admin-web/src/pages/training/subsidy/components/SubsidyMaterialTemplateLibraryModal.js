import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, Upload, message, } from 'antd';
import { DeleteOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../../api/training';
import { downloadUrlFile } from '../../../../api/files';
const MATERIAL_TYPE_OPTS = [
    { value: 'application_form', label: '申请表' },
    { value: 'roster', label: '花名册' },
    { value: 'summary', label: '汇总表' },
    { value: 'other', label: '其他' },
];
const SCOPE_OPTS = [
    { value: 'tenant', label: '机构库' },
    { value: 'class', label: '本班覆盖' },
    { value: 'platform', label: '平台' },
];
export function SubsidyMaterialTemplateLibraryModal({ open, onClose, classId, regionCode, trainMode, policyYear, }) {
    const qc = useQueryClient();
    const [keyword, setKeyword] = useState('');
    const [uploadOpen, setUploadOpen] = useState(false);
    const [form] = Form.useForm();
    const listQ = useQuery({
        queryKey: ['subsidy-material-templates', classId, keyword],
        queryFn: () => trainApi.listSubsidyMaterialTemplates({
            classId: classId ?? undefined,
            keyword: keyword || undefined,
            status: 'active',
        }),
        enabled: open,
    });
    const invalidate = () => {
        void qc.invalidateQueries({ queryKey: ['subsidy-material-templates'] });
    };
    const uploadMu = useMutation({
        mutationFn: (values) => trainApi.uploadSubsidyMaterialTemplate({
            file: values.file,
            title: values.title,
            materialType: values.materialType,
            scope: values.scope || 'tenant',
            classId: values.scope === 'class' ? classId ?? undefined : undefined,
            regionCode: regionCode || undefined,
            trainMode: trainMode || undefined,
            policyYear: policyYear ?? undefined,
            remark: values.remark,
        }),
        onSuccess: () => {
            message.success('模板已上传');
            setUploadOpen(false);
            form.resetFields();
            invalidate();
        },
        onError: (e) => message.error(e.message || '上传失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteSubsidyMaterialTemplate(id),
        onSuccess: () => {
            message.success('已删除');
            invalidate();
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    const reparseMu = useMutation({
        mutationFn: (id) => trainApi.reparseSubsidyMaterialTemplate(id),
        onSuccess: () => {
            message.success('已重新解析占位符');
            invalidate();
        },
        onError: (e) => message.error(e.message || '解析失败'),
    });
    const rows = useMemo(() => listQ.data ?? [], [listQ.data]);
    return (_jsxs(_Fragment, { children: [_jsxs(Modal, { title: "\u8865\u8D34\u6750\u6599\u6A21\u677F\u5E93", open: open, onCancel: onClose, width: 900, footer: _jsx(Button, { onClick: onClose, children: "\u5173\u95ED" }), destroyOnClose: true, children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u6A21\u677F\u6807\u9898", style: { width: 220 }, onSearch: setKeyword }), _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), onClick: () => setUploadOpen(true), children: "\u4E0A\u4F20\u6A21\u677F" }), _jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["\u652F\u6301 docx / xlsx \u81EA\u52A8\u586B\u6570\u3002\u82B1\u540D\u518C\u8BF7\u9009\u6750\u6599\u7C7B\u578B\u300C\u82B1\u540D\u518C\u300D\uFF0C\u8868\u5934\u542B\u300C\u59D3\u540D\u300D\u6216\u539F\u578B\u884C\u5199 ", '${name}', " / ", '${idCard}', "\u3002"] })] }), _jsx(Table, { rowKey: (r) => String(r.id), size: "small", loading: listQ.isLoading, dataSource: rows, pagination: { pageSize: 8 }, columns: [
                            {
                                title: '标题',
                                dataIndex: 'title',
                                ellipsis: true,
                            },
                            {
                                title: '范围',
                                dataIndex: 'scope',
                                width: 88,
                                render: (v) => SCOPE_OPTS.find((o) => o.value === v)?.label || v,
                            },
                            {
                                title: '类型',
                                dataIndex: 'materialTypeLabel',
                                width: 88,
                            },
                            {
                                title: '格式',
                                dataIndex: 'fileFormat',
                                width: 64,
                                render: (v) => _jsx(Tag, { children: v }),
                            },
                            {
                                title: '占位符',
                                dataIndex: 'fieldKeys',
                                ellipsis: true,
                                render: (keys) => keys?.length ? (_jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: [keys.slice(0, 6).join(', '), keys.length > 6 ? ` +${keys.length - 6}` : ''] })) : (_jsx(Typography.Text, { type: "secondary", children: "\u2014" })),
                            },
                            {
                                title: '操作',
                                width: 200,
                                render: (_, row) => (_jsxs(Space, { size: 4, wrap: true, children: [row.templateFilePath ? (_jsx(Button, { type: "link", size: "small", onClick: () => void downloadUrlFile(row.templateFilePath, `${row.title}.${row.fileFormat}`).catch(() => message.error('下载失败')), children: "\u4E0B\u8F7D" })) : null, row.fileFormat === 'docx' || row.fileFormat === 'xlsx' || row.fileFormat === 'xls' ? (_jsx(Button, { type: "link", size: "small", icon: _jsx(ReloadOutlined, {}), loading: reparseMu.isPending, onClick: () => reparseMu.mutate(row.id), children: "\u91CD\u89E3\u6790" })) : null, _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u6A21\u677F\uFF1F", onConfirm: () => deleteMu.mutate(row.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
                            },
                        ] })] }), _jsx(Modal, { title: "\u4E0A\u4F20\u586B\u6570\u6A21\u677F", open: uploadOpen, onCancel: () => {
                    setUploadOpen(false);
                    form.resetFields();
                }, onOk: () => form.submit(), confirmLoading: uploadMu.isPending, destroyOnClose: true, children: _jsxs(Form, { form: form, layout: "vertical", initialValues: { scope: 'tenant', materialType: 'application_form' }, onFinish: (values) => {
                        const file = values.file?.[0]?.originFileObj;
                        if (!file) {
                            message.warning('请选择文件');
                            return;
                        }
                        if (values.scope === 'class' && classId == null) {
                            message.warning('当前无班级，无法上传本班覆盖模板');
                            return;
                        }
                        uploadMu.mutate({
                            file,
                            title: values.title,
                            materialType: values.materialType,
                            scope: values.scope,
                            remark: values.remark,
                        });
                    }, children: [_jsx(Form.Item, { name: "file", label: "\u6A21\u677F\u6587\u4EF6", rules: [{ required: true, message: '请上传文件' }], valuePropName: "fileList", getValueFromEvent: (e) => e?.fileList, children: _jsx(Upload, { beforeUpload: () => false, maxCount: 1, accept: ".docx,.xlsx,.xls", children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), children: "\u9009\u62E9 docx / xlsx" }) }) }), _jsx(Form.Item, { name: "title", label: "\u6807\u9898", children: _jsx(Input, { placeholder: "\u9ED8\u8BA4\u53D6\u6587\u4EF6\u540D" }) }), _jsx(Form.Item, { name: "materialType", label: "\u6750\u6599\u7C7B\u578B", rules: [{ required: true }], children: _jsx(Select, { options: MATERIAL_TYPE_OPTS }) }), _jsx(Form.Item, { name: "scope", label: "\u5165\u5E93\u8303\u56F4", rules: [{ required: true }], children: _jsx(Select, { options: SCOPE_OPTS }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u53EF\u9009" }) })] }) })] }));
}

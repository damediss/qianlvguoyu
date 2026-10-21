import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Button, Card, Col, Form, Input, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, Upload, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, UploadOutlined, SearchOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
const STATUS_MAP = {
    0: { label: '未颁发', color: 'default' },
    1: { label: '已颁发', color: 'success' },
    2: { label: '已作废', color: 'error' },
};
export function TrainingCertificateTemplatePage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [searchForm] = Form.useForm();
    const [query, setQuery] = useState({ page: 1, pageSize: 20 });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const { data, isLoading } = useQuery({
        queryKey: ['training-certificates', query],
        queryFn: () => trainApi.pageCertificates(query),
    });
    const createMu = useMutation({
        mutationFn: (p) => trainApi.createCertificate(p),
        onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-certificates'] }); },
        onError: () => message.error('创建失败'),
    });
    const updateMu = useMutation({
        mutationFn: ({ id, p }) => trainApi.updateCertificate(id, p),
        onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-certificates'] }); },
        onError: () => message.error('更新失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteCertificate(id),
        onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-certificates'] }); },
    });
    const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
    const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
    const openEdit = (r) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
    const handleSubmit = async () => {
        const v = await form.validateFields();
        editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
    };
    const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 70 },
        { title: '模板名称', dataIndex: 'name', width: 200, ellipsis: true },
        { title: '类型', dataIndex: 'type', width: 100 },
        { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
        { title: '模板文件', dataIndex: 'templateFile', width: 150, ellipsis: true,
            render: (v) => v ? _jsx("a", { href: v, target: "_blank", rel: "noopener noreferrer", children: "\u67E5\u770B\u6587\u4EF6" }) : _jsx(Tag, { children: "\u65E0" }) },
        {
            title: '状态', dataIndex: 'status', width: 100,
            render: (v) => { const s = STATUS_MAP[v] ?? { label: '未知', color: 'default' }; return _jsx(Tag, { color: s.color, children: s.label }); },
        },
        { title: '创建时间', dataIndex: 'createTime', width: 160 },
        {
            title: '操作', key: 'act', width: 120, fixed: 'right',
            render: (_, r) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(r), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => deleteMu.mutate(r.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u8BC1\u4E66\u6A21\u677F" }), _jsx(Typography.Text, { type: "secondary", children: "\u7BA1\u7406\u57F9\u8BAD\u8BC1\u4E66\u6A21\u677F\uFF0C\u652F\u6301\u6A21\u677F\u521B\u5EFA\u3001\u7F16\u8F91\u548C\u6587\u4EF6\u4E0A\u4F20" })] }) }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Form, { form: searchForm, layout: "inline", children: [_jsx(Form.Item, { name: "keyword", children: _jsx(Input, { placeholder: "\u641C\u7D22\u6A21\u677F\u540D\u79F0...", prefix: _jsx(SearchOutlined, {}), allowClear: true, onPressEnter: handleSearch, style: { width: 240 } }) }), _jsx(Form.Item, { name: "type", children: _jsx(Select, { allowClear: true, placeholder: "\u6A21\u677F\u7C7B\u578B", style: { width: 120 }, options: [{ value: '结业', label: '结业证书' }, { value: '资格', label: '资格证书' }, { value: '培训', label: '培训证明' }] }) }), _jsx(Form.Item, { children: _jsx(Button, { type: "primary", onClick: handleSearch, children: "\u67E5\u8BE2" }) })] }) }), _jsxs(Card, { size: "small", children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['training-certificates'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u6A21\u677F" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, columns: columns, dataSource: data?.items ?? [], scroll: { x: 900 }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total: data?.total ?? 0,
                            showSizeChanger: true,
                            pageSizeOptions: ['10', '20', '50', '100'],
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (p, ps) => {
                                setQuery((prev) => ({
                                    ...prev,
                                    page: typeof ps === 'number' && ps !== prev.pageSize ? 1 : p,
                                    pageSize: typeof ps === 'number' && ps > 0 ? ps : prev.pageSize,
                                }));
                            },
                        } })] }), _jsx(Modal, { title: editingId ? '编辑证书模板' : '新增证书模板', open: modalOpen, onOk: handleSubmit, onCancel: closeModal, confirmLoading: createMu.isPending || updateMu.isPending, destroyOnHidden: true, width: 600, children: _jsx(Form, { form: form, layout: "vertical", children: _jsxs(Row, { gutter: 24, children: [_jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "name", label: "\u6A21\u677F\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u6A21\u677F\u540D\u79F0" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "type", label: "\u6A21\u677F\u7C7B\u578B", rules: [{ required: true }], children: _jsx(Select, { allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: [{ value: '结业', label: '结业证书' }, { value: '资格', label: '资格证书' }, { value: '培训', label: '培训证明' }] }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: [{ value: 0, label: '未颁发' }, { value: 1, label: '已颁发' }] }) }) }), _jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "description", label: "\u63CF\u8FF0", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u8BF7\u8F93\u5165\u6A21\u677F\u63CF\u8FF0" }) }) }), _jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "templateFile", label: "\u6A21\u677F\u6587\u4EF6", children: _jsx(Upload, { maxCount: 1, beforeUpload: () => false, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), children: "\u4E0A\u4F20\u6A21\u677F\u6587\u4EF6" }) }) }) })] }) }) })] }));
}

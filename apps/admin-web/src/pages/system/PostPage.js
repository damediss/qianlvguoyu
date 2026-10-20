import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, IdcardOutlined, } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pagePosts, createPost, updatePost, deletePost, } from '../../api/system';
export function PostPage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', status: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['system-posts', query],
        queryFn: () => pagePosts(query),
    });
    const createMutation = useMutation({
        mutationFn: createPost,
        onSuccess: () => {
            message.success('岗位已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-posts'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updatePost(id, payload),
        onSuccess: () => {
            message.success('岗位已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-posts'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deletePost,
        onSuccess: () => {
            message.success('岗位已删除');
            queryClient.invalidateQueries({ queryKey: ['system-posts'] });
        },
    });
    const openCreate = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: 'enabled', sortOrder: 0 });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            code: record.code,
            name: record.name,
            sortOrder: record.sortOrder,
            status: record.status,
            remark: record.remark,
        });
        setModalOpen(true);
    }, [form]);
    const handleSubmit = useCallback(() => {
        form.validateFields().then((values) => {
            if (editingId) {
                updateMutation.mutate({ id: editingId, payload: values });
            }
            else {
                createMutation.mutate(values);
            }
        });
    }, [form, editingId, createMutation, updateMutation]);
    const handleDelete = useCallback((record) => {
        Modal.confirm({
            title: `确认删除岗位「${record.name}」？`,
            content: '删除后不可恢复，请确认操作。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate(record.id),
        });
    }, [deleteMutation]);
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const columns = [
        {
            title: '序号',
            width: 80,
            render: (_, __, index) => index + 1,
        },
        {
            title: '岗位编码',
            dataIndex: 'code',
            width: 160,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '岗位名称',
            dataIndex: 'name',
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '排序',
            dataIndex: 'sortOrder',
            width: 80,
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        {
            title: '备注',
            dataIndex: 'remark',
            ellipsis: true,
        },
        {
            title: '操作',
            width: 140,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(IdcardOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u5C97\u4F4D\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7EF4\u62A4\u7EC4\u7EC7\u5185\u7684\u5C97\u4F4D\u4FE1\u606F\uFF0C\u652F\u6301\u7F16\u7801\u3001\u6392\u5E8F\u548C\u72B6\u6001\u7BA1\u7406\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-posts'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u5C97\u4F4D" })] })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u5C97\u4F4D\u540D\u79F0\u6216\u7F16\u7801", allowClear: true, style: { width: 280 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['system-posts'] }) }), _jsx(Select, { placeholder: "\u72B6\u6001", allowClear: true, style: { width: 120 }, value: query.status || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 })), options: [
                                    { label: '启用', value: 'enabled' },
                                    { label: '停用', value: 'disabled' },
                                ] }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-posts'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: editingId ? '编辑岗位' : '新增岗位', open: modalOpen, width: 520, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsx(Form.Item, { name: "code", label: "\u5C97\u4F4D\u7F16\u7801", rules: [{ required: true, message: '请输入岗位编码' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u7F16\u7801\uFF08\u5982 GENERAL_MANAGER\uFF09", maxLength: 64, disabled: !!editingId }) }), _jsx(Form.Item, { name: "name", label: "\u5C97\u4F4D\u540D\u79F0", rules: [{ required: true, message: '请输入岗位名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u540D\u79F0", maxLength: 128 }) }), _jsx(Form.Item, { name: "sortOrder", label: "\u6392\u5E8F\u53F7", children: _jsx(InputNumber, { min: 0, max: 9999, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: [
                                    { label: '启用', value: 'enabled' },
                                    { label: '停用', value: 'disabled' },
                                ] }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { placeholder: "\u5907\u6CE8\u4FE1\u606F\uFF08\u53EF\u9009\uFF09", maxLength: 512, rows: 3 }) })] }) })] }));
}

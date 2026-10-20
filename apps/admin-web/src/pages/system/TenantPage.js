import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, TeamOutlined, } from '@ant-design/icons';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageTenants, createTenant, updateTenant, deleteTenant, } from '../../api/system';
export function TenantPage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', status: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['system-tenants', query],
        queryFn: () => pageTenants(query),
    });
    const createMutation = useMutation({
        mutationFn: createTenant,
        onSuccess: () => {
            message.success('租户已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-tenants'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateTenant(id, payload),
        onSuccess: () => {
            message.success('租户已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-tenants'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteTenant,
        onSuccess: () => {
            message.success('租户已删除');
            queryClient.invalidateQueries({ queryKey: ['system-tenants'] });
        },
    });
    const openCreate = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: 'active' });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            code: record.code,
            name: record.name,
            status: record.status,
            contactName: record.contactName,
            contactPhone: record.contactPhone,
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
            title: `确认删除租户「${record.name}」？`,
            content: '删除租户将影响该租户下的所有数据和用户，此操作不可恢复。',
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
            title: '租户编码',
            dataIndex: 'code',
            width: 150,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '租户名称',
            dataIndex: 'name',
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '联系人',
            dataIndex: 'contactName',
            width: 120,
            render: (v) => v || '-',
        },
        {
            title: '联系电话',
            dataIndex: 'contactPhone',
            width: 140,
            render: (v) => v || '-',
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (v) => {
                const map = {
                    active: { color: 'success', text: '正常' },
                    suspended: { color: 'warning', text: '挂起' },
                    disabled: { color: 'error', text: '停用' },
                };
                const item = map[v] || { color: 'default', text: v };
                return _jsx(Tag, { color: item.color, children: item.text });
            },
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            width: 170,
            render: (v) => v ? new Date(v).toLocaleString('zh-CN') : '-',
        },
        {
            title: '操作',
            width: 140,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(TeamOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u79DF\u6237\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7BA1\u7406\u5E73\u53F0\u4E2D\u7684\u6240\u6709\u79DF\u6237\uFF0C\u5305\u62EC\u521B\u5EFA\u3001\u72B6\u6001\u63A7\u5236\u548C\u8054\u7CFB\u4FE1\u606F\u7EF4\u62A4\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-tenants'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u79DF\u6237" })] })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u79DF\u6237\u540D\u79F0\u6216\u7F16\u7801", allowClear: true, style: { width: 280 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['system-tenants'] }) }), _jsx(Select, { placeholder: "\u72B6\u6001", allowClear: true, style: { width: 120 }, value: query.status || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 })), options: [
                                    { label: '正常', value: 'active' },
                                    { label: '挂起', value: 'suspended' },
                                    { label: '停用', value: 'disabled' },
                                ] }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-tenants'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: editingId ? '编辑租户' : '新增租户', open: modalOpen, width: 560, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsx(Form.Item, { name: "code", label: "\u79DF\u6237\u7F16\u7801", rules: [{ required: true, message: '请输入租户编码' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u552F\u4E00\u79DF\u6237\u7F16\u7801\uFF08\u5982 acme_corp\uFF09", maxLength: 64, disabled: !!editingId }) }), _jsx(Form.Item, { name: "name", label: "\u79DF\u6237\u540D\u79F0", rules: [{ required: true, message: '请输入租户名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u79DF\u6237\u540D\u79F0", maxLength: 128 }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: [
                                    { label: '正常', value: 'active' },
                                    { label: '挂起', value: 'suspended' },
                                    { label: '停用', value: 'disabled' },
                                ] }) }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "contactName", label: "\u8054\u7CFB\u4EBA", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u8054\u7CFB\u4EBA\u59D3\u540D", maxLength: 64 }) }), _jsx(Form.Item, { name: "contactPhone", label: "\u8054\u7CFB\u7535\u8BDD", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u8054\u7CFB\u4EBA\u7535\u8BDD", maxLength: 32 }) })] }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { placeholder: "\u5907\u6CE8\u4FE1\u606F\uFF08\u53EF\u9009\uFF09", maxLength: 512, rows: 3 }) })] }) })] }));
}

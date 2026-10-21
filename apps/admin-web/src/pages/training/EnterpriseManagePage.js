import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, BankOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
const STATUS_OPTIONS = [
    { value: 'enabled', label: '启用' },
    { value: 'disabled', label: '禁用' },
];
export function EnterpriseManagePage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [keyword, setKeyword] = useState('');
    const [searchKeyword, setSearchKeyword] = useState('');
    const [modalOpen, setModalOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const { data = [], isLoading, refetch } = useQuery({
        queryKey: ['system-companies', searchKeyword],
        queryFn: () => trainApi.listSystemCompanies(searchKeyword || undefined),
    });
    const createMu = useMutation({
        mutationFn: (payload) => trainApi.createSystemCompany(payload),
        onSuccess: () => {
            message.success('创建成功');
            closeModal();
            void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
            void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
        },
        onError: (e) => message.error(e.message || '创建失败'),
    });
    const updateMu = useMutation({
        mutationFn: ({ id, payload }) => trainApi.updateSystemCompany(id, payload),
        onSuccess: () => {
            message.success('更新成功');
            closeModal();
            void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
            void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
        },
        onError: (e) => message.error(e.message || '更新失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteSystemCompany(id),
        onSuccess: () => {
            message.success('删除成功');
            void queryClient.invalidateQueries({ queryKey: ['system-companies'] });
            void queryClient.invalidateQueries({ queryKey: ['orgs-simple'] });
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    const closeModal = () => {
        setModalOpen(false);
        setEditing(null);
        form.resetFields();
    };
    const openCreate = () => {
        setEditing(null);
        form.resetFields();
        form.setFieldsValue({ sortOrder: 0, status: 'enabled' });
        setModalOpen(true);
    };
    const openEdit = (row) => {
        setEditing(row);
        form.setFieldsValue({
            name: row.name,
            sortOrder: row.sortOrder ?? 0,
            status: row.status || 'enabled',
        });
        setModalOpen(true);
    };
    const columns = useMemo(() => [
        { title: 'ID', dataIndex: 'id', width: 90 },
        { title: '编码', dataIndex: 'code', width: 180 },
        { title: '企业名称', dataIndex: 'name', ellipsis: true },
        { title: '排序', dataIndex: 'sortOrder', width: 80, align: 'center' },
        {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            align: 'center',
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u7981\u7528" }),
        },
        {
            title: '操作',
            key: 'act',
            width: 140,
            fixed: 'right',
            render: (_, row) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(row), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664\u8BE5\u4F01\u4E1A\uFF1F", onConfirm: () => deleteMu.mutate(row.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ], [deleteMu]);
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsxs(Typography.Title, { level: 4, children: [_jsx(BankOutlined, { style: { marginRight: 8 } }), "\u4F01\u4E1A\u7BA1\u7406"] }), _jsx(Typography.Text, { type: "secondary", children: "\u7EF4\u62A4\u7CFB\u7EDF\u4F01\u4E1A\uFF08\u57F9\u8BAD\u5173\u8054\u4F01\u4E1A\uFF09\uFF0C\u4F9B\u8C03\u7814\u5173\u8054\u3001\u5F00\u73ED\u3001\u5BA1\u6838\u7B49\u573A\u666F\u9009\u7528" })] }) }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Space, { wrap: true, children: [_jsx(Input, { placeholder: "\u6309\u540D\u79F0\u6216\u7F16\u7801\u641C\u7D22", prefix: _jsx(SearchOutlined, {}), value: keyword, allowClear: true, style: { width: 240 }, onChange: (e) => setKeyword(e.target.value), onPressEnter: () => setSearchKeyword(keyword.trim()) }), _jsx(Button, { type: "primary", icon: _jsx(SearchOutlined, {}), onClick: () => setSearchKeyword(keyword.trim()), children: "\u641C\u7D22" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u4F01\u4E1A" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => refetch(), children: "\u5237\u65B0" })] }) }), _jsx(Card, { size: "small", children: _jsx(Table, { rowKey: "id", loading: isLoading, columns: columns, dataSource: data, scroll: { x: 800 }, pagination: { pageSize: 20, showTotal: (t) => `共 ${t} 条` } }) }), _jsx(Modal, { title: editing ? '编辑企业' : '新增企业', open: modalOpen, onCancel: closeModal, onOk: () => form.submit(), confirmLoading: createMu.isPending || updateMu.isPending, destroyOnClose: true, children: _jsxs(Form, { form: form, layout: "vertical", onFinish: (values) => {
                        if (editing) {
                            updateMu.mutate({ id: editing.id, payload: values });
                        }
                        else {
                            createMu.mutate(values);
                        }
                    }, children: [_jsx(Form.Item, { name: "name", label: "\u4F01\u4E1A\u540D\u79F0", rules: [{ required: true, message: '请输入企业名称' }], children: _jsx(Input, { maxLength: 128, placeholder: "\u8BF7\u8F93\u5165\u4F01\u4E1A\u540D\u79F0" }) }), _jsx(Form.Item, { name: "sortOrder", label: "\u6392\u5E8F", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", rules: [{ required: true, message: '请选择状态' }], children: _jsx(Select, { options: STATUS_OPTIONS }) })] }) })] }));
}

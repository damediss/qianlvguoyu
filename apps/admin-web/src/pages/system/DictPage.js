import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, BookOutlined, } from '@ant-design/icons';
import { Button, Drawer, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageDicts, createDict, updateDict, getDictItems, createDictItem, updateDictItem, } from '../../api/system';
export function DictPage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', status: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [drawerDict, setDrawerDict] = useState(null);
    const [itemModalOpen, setItemModalOpen] = useState(false);
    const [editingItemId, setEditingItemId] = useState(null);
    const [form] = Form.useForm();
    const [itemForm] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['system-dicts', query],
        queryFn: () => pageDicts(query),
    });
    const itemsQuery = useQuery({
        queryKey: ['system-dict-items', drawerDict?.code],
        queryFn: () => getDictItems(drawerDict.code),
        enabled: !!drawerDict,
    });
    const createMutation = useMutation({
        mutationFn: createDict,
        onSuccess: () => {
            message.success('字典已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-dicts'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateDict(id, payload),
        onSuccess: () => {
            message.success('字典已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-dicts'] });
        },
    });
    const createItemMutation = useMutation({
        mutationFn: (payload) => createDictItem(drawerDict.id, payload),
        onSuccess: () => {
            message.success('字典项已创建');
            setItemModalOpen(false);
            itemForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-dict-items'] });
        },
    });
    const updateItemMutation = useMutation({
        mutationFn: ({ itemId, payload }) => updateDictItem(itemId, payload),
        onSuccess: () => {
            message.success('字典项已更新');
            setItemModalOpen(false);
            setEditingItemId(null);
            itemForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-dict-items'] });
        },
    });
    const openCreate = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: 'enabled' });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            code: record.code,
            name: record.name,
            value: record.value,
            status: record.status,
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
    const openItemCreate = useCallback(() => {
        setEditingItemId(null);
        itemForm.resetFields();
        itemForm.setFieldsValue({ status: 'enabled', sortOrder: 0 });
        setItemModalOpen(true);
    }, [itemForm]);
    const openItemEdit = useCallback((record) => {
        setEditingItemId(record.id);
        itemForm.setFieldsValue({
            itemValue: record.itemValue,
            itemLabel: record.itemLabel,
            sortOrder: record.sortOrder,
            status: record.status,
        });
        setItemModalOpen(true);
    }, [itemForm]);
    const handleItemSubmit = useCallback(() => {
        itemForm.validateFields().then((values) => {
            if (editingItemId) {
                updateItemMutation.mutate({ itemId: editingItemId, payload: values });
            }
            else {
                createItemMutation.mutate(values);
            }
        });
    }, [itemForm, editingItemId, createItemMutation, updateItemMutation]);
    const dictItems = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const detailItems = itemsQuery.data || [];
    const columns = [
        {
            title: '序号',
            width: 80,
            render: (_, __, index) => index + 1,
        },
        {
            title: '字典编码',
            dataIndex: 'code',
            width: 180,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '字典名称',
            dataIndex: 'name',
            width: 150,
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '字典值',
            dataIndex: 'value',
            ellipsis: true,
            render: (text) => text ? _jsx(Typography.Text, { children: text }) : '-',
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            width: 170,
            render: (v) => v ? new Date(v).toLocaleString('zh-CN') : '-',
        },
        {
            title: '操作',
            width: 200,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(BookOutlined, {}), onClick: () => setDrawerDict(record), children: "\u5B57\u5178\u9879" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" })] })),
        },
    ];
    const itemColumns = [
        {
            title: '排序',
            dataIndex: 'sortOrder',
            width: 70,
        },
        {
            title: '值',
            dataIndex: 'itemValue',
            ellipsis: { showTitle: false },
            render: (text) => (_jsx(Typography.Text, { code: true, ellipsis: { tooltip: text }, children: text })),
        },
        {
            title: '标签',
            dataIndex: 'itemLabel',
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        {
            title: '操作',
            width: 80,
            render: (_, record) => (_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openItemEdit(record), children: "\u7F16\u8F91" })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(BookOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u5B57\u5178\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7EF4\u62A4\u7CFB\u7EDF\u6570\u636E\u5B57\u5178\u53CA\u5176\u9009\u9879\u503C\uFF0C\u7528\u4E8E\u4E0B\u62C9\u9009\u62E9\u3001\u679A\u4E3E\u503C\u7B49\u573A\u666F\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-dicts'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u5B57\u5178" })] })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u5B57\u5178\u540D\u79F0\u6216\u7F16\u7801", allowClear: true, style: { width: 280 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['system-dicts'] }) }), _jsx(Select, { placeholder: "\u72B6\u6001", allowClear: true, style: { width: 120 }, value: query.status || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 })), options: [
                                    { label: '启用', value: 'enabled' },
                                    { label: '停用', value: 'disabled' },
                                ] }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-dicts'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: dictItems, columns: columns, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: editingId ? '编辑字典' : '新增字典', open: modalOpen, width: 480, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsx(Form.Item, { name: "code", label: "\u5B57\u5178\u7F16\u7801", rules: [{ required: true, message: '请输入字典编码' }], children: _jsx(Input, { placeholder: "\u5982 gender, education", maxLength: 64, disabled: !!editingId }) }), _jsx(Form.Item, { name: "name", label: "\u5B57\u5178\u540D\u79F0", rules: [{ required: true, message: '请输入字典名称' }], children: _jsx(Input, { placeholder: "\u5982 \u6027\u522B, \u5B66\u5386", maxLength: 64 }) }), _jsx(Form.Item, { name: "value", label: "\u5B57\u5178\u503C", children: _jsx(Input, { placeholder: "\u7F16\u7801\u5BF9\u5E94\u7684\u503C", maxLength: 1000, allowClear: true }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: [
                                    { label: '启用', value: 'enabled' },
                                    { label: '停用', value: 'disabled' },
                                ] }) })] }) }), _jsx(Drawer, { title: drawerDict ? `字典项 - ${drawerDict.name}` : '字典项', open: !!drawerDict, width: 560, onClose: () => setDrawerDict(null), extra: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openItemCreate, children: "\u65B0\u589E\u5B57\u5178\u9879" }), children: _jsx(Table, { rowKey: "id", size: "small", loading: itemsQuery.isLoading, dataSource: detailItems, columns: itemColumns, pagination: false }) }), _jsx(Modal, { title: editingItemId ? '编辑字典项' : '新增字典项', open: itemModalOpen, width: 440, onOk: handleItemSubmit, onCancel: () => {
                    setItemModalOpen(false);
                    setEditingItemId(null);
                    itemForm.resetFields();
                }, confirmLoading: createItemMutation.isPending || updateItemMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: itemForm, layout: "vertical", style: { marginTop: 16 }, children: [_jsx(Form.Item, { name: "itemValue", label: "\u503C", rules: [{ required: true, message: '请输入字典项值' }], children: _jsx(Input.TextArea, { placeholder: "\u5B58\u50A8\u503C\uFF08\u5982 male, female\uFF09", showCount: true, rows: 5, disabled: !!editingItemId }) }), _jsx(Form.Item, { name: "itemLabel", label: "\u6807\u7B7E", rules: [{ required: true, message: '请输入显示标签' }, { max: 512, message: '最多 512 字' }], children: _jsx(Input.TextArea, { placeholder: "\u663E\u793A\u6587\u672C\uFF08\u5982 \u7537, \u5973\uFF1B\u7533\u62A5\u6761\u4EF6\u8BF7\u586B\u5168\u6587\uFF09", maxLength: 512, showCount: true, rows: 4 }) }), _jsx(Form.Item, { name: "sortOrder", label: "\u6392\u5E8F", children: _jsx(InputNumber, { min: 0, max: 9999, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: [
                                    { label: '启用', value: 'enabled' },
                                    { label: '停用', value: 'disabled' },
                                ] }) })] }) })] }));
}

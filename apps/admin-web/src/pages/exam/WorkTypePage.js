import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, ApartmentOutlined, } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, TreeSelect, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageWorkTypes, treeWorkTypes, createWorkType, updateWorkType, deleteWorkType, } from '../../api/exam';
function buildTreeSelectData(nodes) {
    return nodes.map((node) => ({
        title: node.title,
        value: node.id,
        children: node.children ? buildTreeSelectData(node.children) : undefined,
    }));
}
function filterSelfAndDescendants(nodes, excludeId) {
    return nodes
        .filter((n) => n.id !== excludeId)
        .map((n) => ({
        ...n,
        children: n.children ? filterSelfAndDescendants(n.children, excludeId) : undefined,
    }));
}
export function WorkTypePage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, title: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [viewMode, setViewMode] = useState('tree');
    const [form] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['exam-work-types', query],
        queryFn: () => pageWorkTypes(query),
    });
    const treeQuery = useQuery({
        queryKey: ['exam-work-types-tree'],
        queryFn: treeWorkTypes,
    });
    const createMutation = useMutation({
        mutationFn: createWorkType,
        onSuccess: () => {
            message.success('工种已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
            queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateWorkType(id, payload),
        onSuccess: () => {
            message.success('工种已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
            queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteWorkType,
        onSuccess: () => {
            message.success('工种已删除（含子级）');
            queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
            queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
        },
    });
    const openCreate = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: true, sortNumber: 1 });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            title: record.title,
            seriesNo: record.seriesNo || '',
            levelsNo: record.levelsNo || '',
            parentId: record.parentId || undefined,
            status: record.status === 1,
            sortNumber: record.sortNumber,
        });
        setModalOpen(true);
    }, [form]);
    const handleSubmit = useCallback(() => {
        form.validateFields().then((values) => {
            const payload = {
                title: values.title,
                seriesNo: values.seriesNo || undefined,
                levelsNo: values.levelsNo || undefined,
                parentId: values.parentId || undefined,
                status: values.status ? 1 : 0,
                sortNumber: values.sortNumber ?? 1,
            };
            if (editingId) {
                updateMutation.mutate({ id: editingId, payload });
            }
            else {
                createMutation.mutate(payload);
            }
        });
    }, [form, editingId, createMutation, updateMutation]);
    const handleDelete = useCallback((record) => {
        Modal.confirm({
            title: `确认删除「${record.title}」？`,
            content: '将同时删除该工种下的所有子级工种。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate(record.id),
        });
    }, [deleteMutation]);
    const treeData = treeQuery.data || [];
    const allTreeData = buildTreeSelectData(treeData);
    const parentTreeData = editingId
        ? buildTreeSelectData(filterSelfAndDescendants(JSON.parse(JSON.stringify(treeData)), editingId))
        : allTreeData;
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const columns = [
        {
            title: '序号',
            width: 105,
            render: (_, __, index) => index + 1,
        },
        {
            title: '工种名称',
            dataIndex: 'title',
            render: (text) => (_jsx(Typography.Text, { strong: true, children: text })),
        },
        { title: '职业编码', dataIndex: 'seriesNo', width: 140 },
        { title: '级别', dataIndex: 'levelsNo', width: 120 },
        {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (v) => v === 1 ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        { title: '排序号', dataIndex: 'sortNumber', width: 90 },
        {
            title: '操作',
            width: 120,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(ApartmentOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u5DE5\u79CD\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7EF4\u62A4\u5DE5\u79CD\u7684\u6811\u5F62\u7ED3\u6784\uFF0C\u652F\u6301\u591A\u7EA7\u5206\u7C7B\u3001\u804C\u4E1A\u7F16\u7801\u548C\u7EA7\u522B\u7BA1\u7406\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    queryClient.invalidateQueries({ queryKey: ['exam-work-types'] });
                                    queryClient.invalidateQueries({ queryKey: ['exam-work-types-tree'] });
                                }, children: "\u5237\u65B0" }), _jsx(Select, { value: viewMode, onChange: setViewMode, style: { width: 110 }, options: [
                                    { label: '树形视图', value: 'tree' },
                                    { label: '列表视图', value: 'table' },
                                ] }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u5DE5\u79CD" })] })] }), _jsx("div", { style: { padding: '0 24px' }, children: viewMode === 'table' ? (_jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u5DE5\u79CD\u540D\u79F0\u3001\u7F16\u7801\u3001\u7EA7\u522B", allowClear: true, style: { width: 300 }, value: query.title, onChange: (e) => setQuery((prev) => ({ ...prev, title: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['exam-work-types'] }) }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['exam-work-types'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, pagination: {
                                current: query.page,
                                pageSize: query.pageSize,
                                total,
                                showSizeChanger: true,
                                showTotal: (t) => `共 ${t} 条`,
                                onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                            } })] })) : (_jsx(Table, { rowKey: "id", size: "middle", loading: treeQuery.isLoading, dataSource: treeData, columns: columns, pagination: false, expandable: { defaultExpandAllRows: true } })) }), _jsx(Modal, { title: editingId ? '编辑工种' : '新增工种', open: modalOpen, width: 520, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsx(Form.Item, { name: "title", label: "\u5DE5\u79CD\u540D\u79F0", rules: [{ required: true, message: '请输入工种名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u5DE5\u79CD\u540D\u79F0", maxLength: 255 }) }), _jsx(Form.Item, { name: "seriesNo", label: "\u804C\u4E1A\u7F16\u7801", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u804C\u4E1A\u7F16\u7801", maxLength: 200 }) }), _jsx(Form.Item, { name: "levelsNo", label: "\u7EA7\u522B", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u7EA7\u522B", maxLength: 200 }) }), _jsx(Form.Item, { name: "parentId", label: "\u4E0A\u7EA7\u5DE5\u79CD", children: _jsx(TreeSelect, { allowClear: true, showSearch: true, treeNodeFilterProp: "title", placeholder: "\u4E0D\u9009\u5219\u4E3A\u9876\u7EA7\u5DE5\u79CD", treeData: parentTreeData, treeDefaultExpandAll: true }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", valuePropName: "checked", children: _jsx(Switch, { checkedChildren: "\u542F\u7528", unCheckedChildren: "\u505C\u7528" }) }), _jsx(Form.Item, { name: "sortNumber", label: "\u6392\u5E8F\u53F7", children: _jsx(InputNumber, { min: 1, max: 9999, style: { width: '100%' } }) })] }) })] }));
}

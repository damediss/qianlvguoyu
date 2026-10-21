import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, BankOutlined, } from '@ant-design/icons';
import { Button, Empty, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Tree, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { getOrgTree, createOrg, updateOrg, deleteOrg, } from '../../api/system';
function buildTreeData(nodes) {
    return nodes.map((node) => ({
        key: node.id,
        title: node.name,
        code: node.code,
        orgType: node.orgType,
        sortOrder: node.sortOrder,
        status: node.status,
        parentId: node.parentId,
        children: node.children?.length ? buildTreeData(node.children) : undefined,
    }));
}
function buildTreeSelectData(nodes, excludeId) {
    return nodes
        .filter((n) => !excludeId || n.id !== excludeId)
        .map((node) => ({
        title: node.name,
        value: node.id,
        children: node.children?.length
            ? buildTreeSelectData(node.children, excludeId)
            : undefined,
    }));
}
function flattenTreeForSelect(nodes) {
    const result = [];
    function walk(list) {
        for (const n of list) {
            result.push(n);
            if (n.children?.length)
                walk(n.children);
        }
    }
    walk(nodes);
    return result;
}
export function OrgPage() {
    const queryClient = useQueryClient();
    const [selectedOrg, setSelectedOrg] = useState(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const treeQuery = useQuery({
        queryKey: ['system-orgs-tree'],
        queryFn: getOrgTree,
    });
    const createMutation = useMutation({
        mutationFn: createOrg,
        onSuccess: () => {
            message.success('组织已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateOrg(id, payload),
        onSuccess: () => {
            message.success('组织已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            setSelectedOrg(null);
            queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteOrg,
        onSuccess: () => {
            message.success('组织已删除');
            setSelectedOrg(null);
            queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] });
        },
    });
    const openCreate = useCallback((parentId) => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({
            parentId: parentId ?? 0,
            orgType: 'department',
            status: 'enabled',
            sortOrder: 0,
        });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            parentId: record.parentId,
            orgType: record.orgType,
            code: record.code,
            name: record.name,
            sortOrder: record.sortOrder,
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
    const handleDelete = useCallback((record) => {
        Modal.confirm({
            title: `确认删除组织「${record.name}」？`,
            content: '将同时删除该组织下的所有子级组织。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate(record.id),
        });
    }, [deleteMutation]);
    const treeData = treeQuery.data || [];
    const antTreeData = buildTreeData(treeData);
    const tableColumns = [
        {
            title: '组织名称',
            dataIndex: 'name',
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '编码',
            dataIndex: 'code',
            width: 140,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '类型',
            dataIndex: 'orgType',
            width: 100,
            render: (v) => {
                const map = {
                    company: { color: 'blue', text: '公司' },
                    department: { color: 'green', text: '部门' },
                    team: { color: 'orange', text: '小组' },
                };
                const item = map[v] || { color: 'default', text: v };
                return _jsx(Tag, { color: item.color, children: item.text });
            },
        },
        {
            title: '排序',
            dataIndex: 'sortOrder',
            width: 70,
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        {
            title: '操作',
            width: 180,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => openCreate(record.id), children: "\u5B50\u7EA7" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(BankOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u7EC4\u7EC7\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7EF4\u62A4\u516C\u53F8\u7EC4\u7EC7\u67B6\u6784\u7684\u6811\u5F62\u7ED3\u6784\uFF0C\u5305\u62EC\u516C\u53F8\u3001\u90E8\u95E8\u548C\u5C0F\u7EC4\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-orgs-tree'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => openCreate(0), children: "\u65B0\u589E\u7EC4\u7EC7" })] })] }), _jsxs("div", { style: { display: 'grid', gridTemplateColumns: '280px 1fr', gap: 16, padding: '0 24px' }, children: [_jsxs("div", { style: {
                            border: '1px solid #e2e8f0',
                            borderRadius: 10,
                            padding: 16,
                            background: '#fff',
                            minHeight: 400,
                        }, children: [_jsx(Typography.Text, { strong: true, style: { display: 'block', marginBottom: 12, fontSize: 14 }, children: "\u7EC4\u7EC7\u67B6\u6784" }), antTreeData.length > 0 ? (_jsx(Tree, { treeData: antTreeData, defaultExpandAll: true, selectedKeys: selectedOrg ? [selectedOrg.id] : [], onSelect: (keys, info) => {
                                    if (keys.length > 0) {
                                        setSelectedOrg(info.node);
                                    }
                                } })) : (_jsx(Empty, { description: "\u6682\u65E0\u7EC4\u7EC7\u6570\u636E", image: Empty.PRESENTED_IMAGE_SIMPLE }))] }), _jsx("div", { style: {
                            border: '1px solid #e2e8f0',
                            borderRadius: 10,
                            padding: 16,
                            background: '#fff',
                            minHeight: 400,
                        }, children: selectedOrg ? (_jsxs(_Fragment, { children: [_jsxs("div", { style: { marginBottom: 16 }, children: [_jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: selectedOrg.name }), _jsxs(Typography.Text, { type: "secondary", children: ["\u7F16\u7801: ", selectedOrg.code, " | \u7C7B\u578B: ", selectedOrg.orgType] })] }), _jsx(Table, { rowKey: "id", size: "small", dataSource: selectedOrg.children || [], columns: tableColumns, pagination: false, expandable: { defaultExpandAllRows: true } })] })) : (_jsx("div", { style: { display: 'grid', placeItems: 'center', minHeight: 300 }, children: _jsx(Empty, { description: "\u8BF7\u5728\u5DE6\u4FA7\u9009\u62E9\u4E00\u4E2A\u7EC4\u7EC7\u8282\u70B9", image: Empty.PRESENTED_IMAGE_SIMPLE }) })) })] }), _jsx(Modal, { title: editingId ? '编辑组织' : '新增组织', open: modalOpen, width: 520, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsxs(Form.Item, { name: "parentId", label: "\u4E0A\u7EA7\u7EC4\u7EC7", children: [_jsx(Input, { type: "hidden" }), _jsx(Select, { placeholder: "\u9009\u62E9\u4E0A\u7EA7\u7EC4\u7EC7\uFF08\u4E0D\u9009\u5219\u4E3A\u9876\u7EA7\uFF09", allowClear: true, options: [
                                        { label: '顶级组织', value: 0 },
                                        ...buildTreeSelectData(treeData, editingId || undefined),
                                    ], showSearch: true, filterOption: (input, option) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase()) })] }), _jsx(Form.Item, { name: "orgType", label: "\u7EC4\u7EC7\u7C7B\u578B", rules: [{ required: true, message: '请选择组织类型' }], children: _jsx(Select, { options: [
                                    { label: '公司', value: 'company' },
                                    { label: '部门', value: 'department' },
                                    { label: '小组', value: 'team' },
                                ] }) }), _jsx(Form.Item, { name: "code", label: "\u7EC4\u7EC7\u7F16\u7801", rules: [{ required: true, message: '请输入组织编码' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u552F\u4E00\u7F16\u7801", maxLength: 64, disabled: !!editingId }) }), _jsx(Form.Item, { name: "name", label: "\u7EC4\u7EC7\u540D\u79F0", rules: [{ required: true, message: '请输入组织名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u7EC4\u7EC7\u540D\u79F0", maxLength: 128 }) }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "sortOrder", label: "\u6392\u5E8F", style: { flex: 1 }, children: _jsx(InputNumber, { min: 0, max: 9999, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", style: { flex: 1 }, children: _jsx(Select, { options: [
                                            { label: '启用', value: 'enabled' },
                                            { label: '停用', value: 'disabled' },
                                        ] }) })] })] }) })] }));
}

import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UserOutlined, KeyOutlined, } from '@ant-design/icons';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, TreeSelect, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageUsers, createUser, updateUser, deleteUser, resetPassword, getOrgTree, listAllPosts, pageRoles, } from '../../api/system';
function buildOrgTreeSelect(nodes) {
    return nodes.map((node) => ({
        title: node.name,
        value: node.id,
        children: node.children?.length ? buildOrgTreeSelect(node.children) : undefined,
    }));
}
export function UserPage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', status: '', userType: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [pwdModalOpen, setPwdModalOpen] = useState(false);
    const [pwdUserId, setPwdUserId] = useState(null);
    const [form] = Form.useForm();
    const [pwdForm] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['system-users', query],
        queryFn: () => pageUsers(query),
    });
    const orgQuery = useQuery({
        queryKey: ['system-orgs-tree'],
        queryFn: getOrgTree,
    });
    const postQuery = useQuery({
        queryKey: ['system-posts-all'],
        queryFn: listAllPosts,
    });
    const roleQuery = useQuery({
        queryKey: ['system-roles-all'],
        queryFn: () => pageRoles({ page: 1, pageSize: 200 }),
    });
    const createMutation = useMutation({
        mutationFn: createUser,
        onSuccess: () => {
            message.success('用户已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-users'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateUser(id, payload),
        onSuccess: () => {
            message.success('用户已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-users'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteUser,
        onSuccess: () => {
            message.success('用户已删除');
            queryClient.invalidateQueries({ queryKey: ['system-users'] });
        },
    });
    const resetPwdMutation = useMutation({
        mutationFn: ({ id, password }) => resetPassword(id, { newPassword: password }),
        onSuccess: () => {
            message.success('密码已重置');
            setPwdModalOpen(false);
            setPwdUserId(null);
            pwdForm.resetFields();
        },
    });
    const openCreate = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: 'enabled', userType: 'normal', roleIds: [] });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            orgId: record.orgId || undefined,
            postId: record.postId || undefined,
            username: record.username,
            displayName: record.displayName,
            userType: record.userType,
            phone: record.phone,
            email: record.email,
            status: record.status,
            roleIds: record.roleIds || [],
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
            title: `确认删除用户「${record.displayName}」？`,
            content: '删除后不可恢复。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate(record.id),
        });
    }, [deleteMutation]);
    const handleResetPassword = useCallback(() => {
        pwdForm.validateFields().then((values) => {
            if (pwdUserId) {
                resetPwdMutation.mutate({ id: pwdUserId, password: values.newPassword });
            }
        });
    }, [pwdForm, pwdUserId, resetPwdMutation]);
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const orgTreeData = buildOrgTreeSelect(orgQuery.data || []);
    const posts = postQuery.data || [];
    const roles = roleQuery.data?.items || [];
    const columns = [
        {
            title: '序号',
            width: 70,
            render: (_, __, index) => index + 1,
        },
        {
            title: '用户名',
            dataIndex: 'username',
            width: 130,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '姓名',
            dataIndex: 'displayName',
            width: 120,
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '用户类型',
            dataIndex: 'userType',
            width: 100,
            render: (v) => {
                const map = {
                    platform_admin: '平台管理员',
                    tenant_admin: '租户管理员',
                    certification_admin: '认定管理员',
                    normal: '普通用户',
                };
                return map[v] || v;
            },
        },
        {
            title: '岗位',
            dataIndex: 'postName',
            width: 120,
            render: (v) => v || '-',
        },
        {
            title: '角色',
            dataIndex: 'roleCodes',
            width: 180,
            render: (codes) => codes?.length ? (_jsx(Space, { size: 4, wrap: true, children: codes.map((c) => (_jsx(Tag, { color: "blue", children: c }, c))) })) : ('-'),
        },
        {
            title: '手机号',
            dataIndex: 'phone',
            width: 130,
            render: (v) => v || '-',
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => {
                const map = {
                    enabled: { color: 'success', text: '正常' },
                    locked: { color: 'warning', text: '锁定' },
                    disabled: { color: 'error', text: '停用' },
                };
                const item = map[v] || { color: 'default', text: v };
                return _jsx(Tag, { color: item.color, children: item.text });
            },
        },
        {
            title: '最后登录',
            dataIndex: 'lastLoginAt',
            width: 160,
            render: (v) => v ? new Date(v).toLocaleString('zh-CN') : '-',
        },
        {
            title: '操作',
            width: 200,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(KeyOutlined, {}), onClick: () => {
                            setPwdUserId(record.id);
                            pwdForm.resetFields();
                            setPwdModalOpen(true);
                        }, children: "\u91CD\u7F6E" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(UserOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u7528\u6237\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7BA1\u7406\u5E73\u53F0\u7528\u6237\u8D26\u53F7\uFF0C\u5305\u62EC\u521B\u5EFA\u3001\u89D2\u8272\u5206\u914D\u3001\u5C97\u4F4D\u5173\u8054\u548C\u5BC6\u7801\u7BA1\u7406\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-users'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u7528\u6237" })] })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u7528\u6237\u540D\u3001\u59D3\u540D\u3001\u624B\u673A\u53F7", allowClear: true, style: { width: 280 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['system-users'] }) }), _jsx(Select, { placeholder: "\u7528\u6237\u7C7B\u578B", allowClear: true, style: { width: 140 }, value: query.userType || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, userType: v || '', page: 1 })), options: [
                                    { label: '平台管理员', value: 'platform_admin' },
                                    { label: '租户管理员', value: 'tenant_admin' },
                                    { label: '认定管理员', value: 'certification_admin' },
                                    { label: '普通用户', value: 'normal' },
                                ] }), _jsx(Select, { placeholder: "\u72B6\u6001", allowClear: true, style: { width: 120 }, value: query.status || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 })), options: [
                                    { label: '正常', value: 'enabled' },
                                    { label: '锁定', value: 'locked' },
                                    { label: '停用', value: 'disabled' },
                                ] }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-users'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, scroll: { x: 1300 }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: editingId ? '编辑用户' : '新增用户', open: modalOpen, width: 600, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "username", label: "\u7528\u6237\u540D", rules: [{ required: true, message: '请输入用户名' }], style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u767B\u5F55\u7528\u6237\u540D", maxLength: 64, disabled: !!editingId }) }), _jsx(Form.Item, { name: "displayName", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u663E\u793A\u59D3\u540D", maxLength: 64 }) })] }), !editingId && (_jsx(Form.Item, { name: "password", label: "\u5BC6\u7801", rules: [
                                { required: true, message: '请输入密码' },
                                { min: 8, message: '密码至少8位' },
                            ], children: _jsx(Input.Password, { placeholder: "\u8BF7\u8F93\u5165\u5BC6\u7801\uFF08\u81F3\u5C118\u4F4D\uFF09", maxLength: 128 }) })), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "userType", label: "\u7528\u6237\u7C7B\u578B", rules: [{ required: true, message: '请选择用户类型' }], style: { flex: 1 }, children: _jsx(Select, { options: [
                                            { label: '平台管理员', value: 'platform_admin' },
                                            { label: '租户管理员', value: 'tenant_admin' },
                                            { label: '认定管理员', value: 'certification_admin' },
                                            { label: '普通用户', value: 'normal' },
                                        ] }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", style: { flex: 1 }, children: _jsx(Select, { options: [
                                            { label: '正常', value: 'enabled' },
                                            { label: '锁定', value: 'locked' },
                                            { label: '停用', value: 'disabled' },
                                        ] }) })] }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "orgId", label: "\u6240\u5C5E\u7EC4\u7EC7", style: { flex: 1 }, children: _jsx(TreeSelect, { placeholder: "\u9009\u62E9\u6240\u5C5E\u7EC4\u7EC7", allowClear: true, treeDefaultExpandAll: true, treeData: orgTreeData }) }), _jsx(Form.Item, { name: "postId", label: "\u5C97\u4F4D", style: { flex: 1 }, children: _jsx(Select, { placeholder: "\u9009\u62E9\u5C97\u4F4D", allowClear: true, options: posts.map((p) => ({ label: p.name, value: p.id })) }) })] }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u624B\u673A\u53F7", maxLength: 32 }) }), _jsx(Form.Item, { name: "email", label: "\u90AE\u7BB1", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u90AE\u7BB1\u5730\u5740", maxLength: 128 }) })] }), _jsx(Form.Item, { name: "roleIds", label: "\u89D2\u8272", children: _jsx(Select, { mode: "multiple", placeholder: "\u9009\u62E9\u89D2\u8272\uFF08\u53EF\u591A\u9009\uFF09", options: roles.map((r) => ({ label: `${r.name} (${r.code})`, value: r.id })) }) })] }) }), _jsx(Modal, { title: "\u91CD\u7F6E\u5BC6\u7801", open: pwdModalOpen, width: 400, onOk: handleResetPassword, onCancel: () => {
                    setPwdModalOpen(false);
                    setPwdUserId(null);
                    pwdForm.resetFields();
                }, confirmLoading: resetPwdMutation.isPending, destroyOnHidden: true, children: _jsx(Form, { form: pwdForm, layout: "vertical", style: { marginTop: 16 }, children: _jsx(Form.Item, { name: "newPassword", label: "\u65B0\u5BC6\u7801", rules: [
                            { required: true, message: '请输入新密码' },
                            { min: 8, message: '密码至少8位' },
                        ], children: _jsx(Input.Password, { placeholder: "\u8BF7\u8F93\u5165\u65B0\u5BC6\u7801\uFF08\u81F3\u5C118\u4F4D\uFF09", maxLength: 128 }) }) }) })] }));
}

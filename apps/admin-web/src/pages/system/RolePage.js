import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SafetyOutlined, } from '@ant-design/icons';
import { Button, Checkbox, Drawer, Form, Input, Modal, Select, Space, Table, Tag, Tree, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo } from 'react';
import { pageRoles, createRole, updateRole, deleteRole, getRole, bindRolePermissions, getMenuTree, getPermissionMatrix, } from '../../api/system';
// 操作类型分组顺序（业务侧常见顺序：先看再写后改删导）
const ACTION_ORDER = [
    { key: 'view', label: '查看', color: 'blue' },
    { key: 'query', label: '查询', color: 'blue' },
    { key: 'list', label: '列表', color: 'blue' },
    { key: 'create', label: '新增', color: 'green' },
    { key: 'add', label: '新增', color: 'green' },
    { key: 'update', label: '编辑', color: 'orange' },
    { key: 'edit', label: '编辑', color: 'orange' },
    { key: 'delete', label: '删除', color: 'red' },
    { key: 'export', label: '导出', color: 'purple' },
    { key: 'import', label: '导入', color: 'cyan' },
    { key: 'audit', label: '审核', color: 'gold' },
    { key: 'other', label: '其他', color: 'default' },
];
/** 雪花 ID 经 json-bigint 可能是 string，禁止 Number() 以免丢精度 */
function toIdKey(value) {
    if (value == null || value === '')
        return '';
    return String(value);
}
function normalizeIdList(values) {
    if (!Array.isArray(values))
        return [];
    return values.map(toIdKey).filter((id) => id !== '');
}
function extractTreeCheckedKeys(keys) {
    if (Array.isArray(keys))
        return normalizeIdList(keys);
    if (keys && typeof keys === 'object' && Array.isArray(keys.checked)) {
        return normalizeIdList(keys.checked);
    }
    return [];
}
function resolveActionGroup(action) {
    const key = (action || '').toLowerCase().trim();
    if (!key)
        return ACTION_ORDER[ACTION_ORDER.length - 1];
    return ACTION_ORDER.find((g) => g.key === key) || ACTION_ORDER[ACTION_ORDER.length - 1];
}
function buildMenuCheckTree(menus, checkedIds) {
    const checkedKeys = [];
    function walk(nodes) {
        return nodes.map((node) => {
            const key = toIdKey(node.id);
            if (checkedIds.has(key)) {
                checkedKeys.push(key);
            }
            return {
                key,
                title: node.name,
                children: node.children?.length ? walk(node.children) : undefined,
            };
        });
    }
    const data = walk(menus);
    return { data, checkedKeys };
}
export function RolePage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '', roleType: '', status: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [permDrawerOpen, setPermDrawerOpen] = useState(false);
    const [permRoleId, setPermRoleId] = useState(null);
    const [permRoleDetail, setPermRoleDetail] = useState(null);
    const [checkedMenuIds, setCheckedMenuIds] = useState([]);
    const [checkedPermIds, setCheckedPermIds] = useState(new Set());
    const [form] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['system-roles', query],
        queryFn: () => pageRoles(query),
    });
    const menuTreeQuery = useQuery({
        queryKey: ['system-menus-tree'],
        queryFn: getMenuTree,
    });
    const permMatrixQuery = useQuery({
        queryKey: ['system-perm-matrix'],
        queryFn: getPermissionMatrix,
    });
    const createMutation = useMutation({
        mutationFn: createRole,
        onSuccess: () => {
            message.success('角色已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-roles'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateRole(id, payload),
        onSuccess: () => {
            message.success('角色已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-roles'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteRole,
        onSuccess: () => {
            message.success('角色已删除');
            queryClient.invalidateQueries({ queryKey: ['system-roles'] });
        },
    });
    const bindMutation = useMutation({
        mutationFn: () => bindRolePermissions(permRoleId, {
            menuIds: checkedMenuIds,
            permissionIds: Array.from(checkedPermIds),
        }),
        onSuccess: () => {
            message.success('权限绑定已保存');
            setPermDrawerOpen(false);
            setPermRoleId(null);
            setPermRoleDetail(null);
            queryClient.invalidateQueries({ queryKey: ['system-roles'] });
        },
    });
    const openCreate = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: 'enabled', roleType: 'custom' });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            code: record.code,
            name: record.name,
            roleType: record.roleType,
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
            title: `确认删除角色「${record.name}」？`,
            content: '删除后不可恢复，已绑定该角色的用户将失去对应权限。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate(record.id),
        });
    }, [deleteMutation]);
    const openPermDrawer = useCallback(async (record) => {
        setPermRoleId(record.id);
        try {
            const detail = await getRole(record.id);
            setPermRoleDetail(detail);
            setCheckedMenuIds(normalizeIdList(detail.menuIds));
            setCheckedPermIds(new Set(normalizeIdList(detail.permissionIds)));
        }
        catch {
            setCheckedMenuIds([]);
            setCheckedPermIds(new Set());
        }
        setPermDrawerOpen(true);
    }, []);
    const handleMenuCheck = useCallback((keys) => {
        const nextMenuIds = extractTreeCheckedKeys(keys);
        setCheckedMenuIds(nextMenuIds);
        // 联动：把"当前所有已勾菜单"下的 perm id 全部加进 checkedPermIds
        // 反向不联动（取消菜单时不取消对应权限），由用户手动细调
        const allMatrix = (permMatrixQuery.data || []);
        const permIdByMenuId = new Map();
        for (const group of allMatrix) {
            permIdByMenuId.set(toIdKey(group.menuId), group.permissions.map((p) => toIdKey(p.id)).filter(Boolean));
        }
        setCheckedPermIds((prev) => {
            const next = new Set(prev);
            const menuIdSet = new Set(nextMenuIds);
            for (const [menuId, permIds] of permIdByMenuId.entries()) {
                if (menuIdSet.has(menuId)) {
                    for (const pid of permIds)
                        next.add(pid);
                }
            }
            return next;
        });
    }, [permMatrixQuery.data]);
    const handlePermToggle = useCallback((permId, checked) => {
        const key = toIdKey(permId);
        if (!key)
            return;
        setCheckedPermIds((prev) => {
            const next = new Set(prev);
            if (checked) {
                next.add(key);
            }
            else {
                next.delete(key);
            }
            return next;
        });
    }, []);
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const menuTree = menuTreeQuery.data || [];
    const permMatrix = permMatrixQuery.data || [];
    const { data: menuCheckTreeData } = useMemo(() => buildMenuCheckTree(menuTree, new Set(checkedMenuIds)), [menuTree, checkedMenuIds]);
    const columns = [
        {
            title: '序号',
            width: 70,
            render: (_, __, index) => index + 1,
        },
        {
            title: '角色编码',
            dataIndex: 'code',
            width: 160,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '角色名称',
            dataIndex: 'name',
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '角色类型',
            dataIndex: 'roleType',
            width: 110,
            render: (v) => {
                const map = {
                    platform: { color: 'purple', text: '平台角色' },
                    tenant: { color: 'blue', text: '租户角色' },
                    custom: { color: 'green', text: '自定义' },
                };
                const item = map[v] || { color: 'default', text: v };
                return _jsx(Tag, { color: item.color, children: item.text });
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        {
            title: '备注',
            dataIndex: 'remark',
            ellipsis: true,
            render: (v) => v || '-',
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            width: 170,
            render: (v) => v ? new Date(v).toLocaleString('zh-CN') : '-',
        },
        {
            title: '操作',
            width: 220,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(SafetyOutlined, {}), onClick: () => openPermDrawer(record), children: "\u6743\u9650" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(SafetyOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u89D2\u8272\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7BA1\u7406\u7CFB\u7EDF\u89D2\u8272\u53CA\u5176\u6743\u9650\u7ED1\u5B9A\uFF0C\u63A7\u5236\u4E0D\u540C\u89D2\u8272\u7684\u83DC\u5355\u548C\u529F\u80FD\u8BBF\u95EE\u6743\u9650\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-roles'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u89D2\u8272" })] })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u89D2\u8272\u540D\u79F0\u6216\u7F16\u7801", allowClear: true, style: { width: 260 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['system-roles'] }) }), _jsx(Select, { placeholder: "\u89D2\u8272\u7C7B\u578B", allowClear: true, style: { width: 130 }, value: query.roleType || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, roleType: v || '', page: 1 })), options: [
                                    { label: '平台角色', value: 'platform' },
                                    { label: '租户角色', value: 'tenant' },
                                    { label: '自定义', value: 'custom' },
                                ] }), _jsx(Select, { placeholder: "\u72B6\u6001", allowClear: true, style: { width: 120 }, value: query.status || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, status: v || '', page: 1 })), options: [
                                    { label: '启用', value: 'enabled' },
                                    { label: '停用', value: 'disabled' },
                                ] }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-roles'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: editingId ? '编辑角色' : '新增角色', open: modalOpen, width: 520, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "code", label: "\u89D2\u8272\u7F16\u7801", rules: [{ required: true, message: '请输入角色编码' }], style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u5982 PLATFORM_ADMIN", maxLength: 64, disabled: !!editingId }) }), _jsx(Form.Item, { name: "name", label: "\u89D2\u8272\u540D\u79F0", rules: [{ required: true, message: '请输入角色名称' }], style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u5982 \u5E73\u53F0\u7BA1\u7406\u5458", maxLength: 64 }) })] }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "roleType", label: "\u89D2\u8272\u7C7B\u578B", rules: [{ required: true, message: '请选择角色类型' }], style: { flex: 1 }, children: _jsx(Select, { options: [
                                            { label: '平台角色', value: 'platform' },
                                            { label: '租户角色', value: 'tenant' },
                                            { label: '自定义', value: 'custom' },
                                        ] }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", style: { flex: 1 }, children: _jsx(Select, { options: [
                                            { label: '启用', value: 'enabled' },
                                            { label: '停用', value: 'disabled' },
                                        ] }) })] }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { placeholder: "\u5907\u6CE8\u4FE1\u606F\uFF08\u53EF\u9009\uFF09", maxLength: 512, rows: 3 }) })] }) }), _jsxs(Drawer, { title: permRoleDetail ? `权限配置 - ${permRoleDetail.name}` : '权限配置', open: permDrawerOpen, width: 640, onClose: () => {
                    setPermDrawerOpen(false);
                    setPermRoleId(null);
                    setPermRoleDetail(null);
                }, extra: _jsx(Button, { type: "primary", loading: bindMutation.isPending, onClick: () => bindMutation.mutate(), children: "\u4FDD\u5B58\u6743\u9650" }), children: [_jsxs("div", { style: { marginBottom: 24 }, children: [_jsx(Typography.Title, { level: 5, style: { marginBottom: 12 }, children: "\u83DC\u5355\u6743\u9650" }), _jsx("div", { style: {
                                    border: '1px solid #e2e8f0',
                                    borderRadius: 8,
                                    padding: 12,
                                    maxHeight: 280,
                                    overflow: 'auto',
                                }, children: menuCheckTreeData.length > 0 ? (_jsx(Tree, { checkable: true, defaultExpandAll: true, checkedKeys: checkedMenuIds, onCheck: handleMenuCheck, treeData: menuCheckTreeData })) : (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u83DC\u5355\u6570\u636E" })) })] }), _jsxs("div", { children: [_jsxs("div", { style: { display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 12 }, children: [_jsx(Typography.Title, { level: 5, style: { margin: 0 }, children: "\u529F\u80FD\u6743\u9650" }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u52FE\u9009\u83DC\u5355\u65F6\u4F1A\u81EA\u52A8\u5E26\u51FA\u8BE5\u83DC\u5355\u4E0B\u7684\u5168\u90E8\u6743\u9650\uFF1B\u53D6\u6D88\u83DC\u5355\u4E0D\u4F1A\u7EA7\u8054\u53D6\u6D88\uFF0C\u53EF\u5355\u72EC\u8C03\u6574" })] }), permMatrix.length > 0 ? (permMatrix.map((group) => {
                                const menuIsChecked = checkedMenuIds.includes(toIdKey(group.menuId));
                                const grouped = group.permissions.reduce((acc, p) => {
                                    const g = resolveActionGroup(p.action);
                                    if (!acc[g.key])
                                        acc[g.key] = [];
                                    acc[g.key].push(p);
                                    return acc;
                                }, {});
                                return (_jsxs("div", { style: {
                                        marginBottom: 16,
                                        border: menuIsChecked ? '1px solid #1677ff' : '1px solid #e2e8f0',
                                        borderRadius: 8,
                                        overflow: 'hidden',
                                        background: menuIsChecked ? '#f0f7ff' : '#fff',
                                        transition: 'all 0.2s',
                                    }, children: [_jsxs("div", { style: {
                                                padding: '8px 14px',
                                                background: menuIsChecked ? '#e6f4ff' : '#f8fafc',
                                                borderBottom: '1px solid #e2e8f0',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'space-between',
                                            }, children: [_jsxs(Space, { size: 8, wrap: true, children: [_jsx(Typography.Text, { strong: true, style: { fontSize: 13, color: '#1f2937' }, children: group.menuName }), group.menuPath && group.menuPath.includes(' / ') && (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: group.menuPath })), _jsxs(Tag, { color: "default", children: [group.permissions.length, " \u4E2A\u6743\u9650"] })] }), _jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", onClick: () => {
                                                                // 一键全选该菜单下所有权限
                                                                setCheckedPermIds((prev) => {
                                                                    const next = new Set(prev);
                                                                    for (const p of group.permissions) {
                                                                        const pid = toIdKey(p.id);
                                                                        if (pid)
                                                                            next.add(pid);
                                                                    }
                                                                    return next;
                                                                });
                                                            }, children: "\u5168\u9009" }), _jsx(Button, { type: "link", size: "small", onClick: () => {
                                                                // 一键清空该菜单下所有权限
                                                                setCheckedPermIds((prev) => {
                                                                    const next = new Set(prev);
                                                                    for (const p of group.permissions) {
                                                                        const pid = toIdKey(p.id);
                                                                        if (pid)
                                                                            next.delete(pid);
                                                                    }
                                                                    return next;
                                                                });
                                                            }, children: "\u6E05\u7A7A" })] })] }), _jsx("div", { style: { padding: '10px 14px' }, children: ACTION_ORDER.filter((g) => grouped[g.key]?.length).map((g) => (_jsxs("div", { style: {
                                                    display: 'flex',
                                                    alignItems: 'flex-start',
                                                    gap: 12,
                                                    padding: '4px 0',
                                                }, children: [_jsx(Tag, { color: g.color, style: { minWidth: 48, textAlign: 'center', marginTop: 4 }, children: g.label }), _jsx(Space, { wrap: true, size: [12, 8], style: { flex: 1 }, children: grouped[g.key].map((perm) => {
                                                            const permKey = toIdKey(perm.id);
                                                            return (_jsx(Checkbox, { checked: checkedPermIds.has(permKey), onChange: (e) => handlePermToggle(perm.id, e.target.checked), children: _jsxs(Space, { size: 4, children: [_jsx("span", { children: perm.name }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 11 }, code: true, children: perm.code })] }) }, permKey));
                                                        }) })] }, g.key))) })] }, toIdKey(group.menuId)));
                            })) : (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u6743\u9650\u6570\u636E" }))] })] })] }));
}

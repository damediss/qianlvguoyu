import { jsxs as _jsxs, jsx as _jsx } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, AppstoreOutlined, } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, TreeSelect, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useRef } from 'react';
import { getMenuTree, createMenu, updateMenu, deleteMenu, batchUpdateMenuVisible, } from '../../api/system';
import { IconPicker } from '../../components/IconPicker';
import { usePinyinAutoGen } from '../../hooks/usePinyinAutoGen';
import { iconFromName } from '../../utils/icons';
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
/** Collect all descendant IDs from a tree node. */
function collectDescendantIds(nodes, targetId) {
    const findNode = (list) => {
        for (const node of list) {
            if (node.id === targetId)
                return node;
            if (node.children?.length) {
                const found = findNode(node.children);
                if (found)
                    return found;
            }
        }
        return null;
    };
    const collectIds = (node) => {
        const ids = [];
        if (node.children?.length) {
            for (const child of node.children) {
                ids.push(child.id);
                ids.push(...collectIds(child));
            }
        }
        return ids;
    };
    const targetNode = findNode(nodes);
    if (!targetNode)
        return [];
    return collectIds(targetNode);
}
const MENU_TYPE_OPTIONS = [
    { label: '目录', value: 'directory' },
    { label: '菜单', value: 'menu' },
    { label: '按钮', value: 'button' },
];
const MENU_TYPE_TAG = {
    directory: { color: 'blue', text: '目录' },
    menu: { color: 'green', text: '菜单' },
    button: { color: 'orange', text: '按钮' },
};
export function MenuPage() {
    const queryClient = useQueryClient();
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const menuType = Form.useWatch('menuType', form);
    const originalVisibleRef = useRef(null);
    const treeQuery = useQuery({
        queryKey: ['system-menus-tree'],
        queryFn: getMenuTree,
    });
    const treeData = treeQuery.data || [];
    const { onNameChange } = usePinyinAutoGen(form, treeData);
    const createMutation = useMutation({
        mutationFn: createMenu,
        onSuccess: () => {
            message.success('菜单已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateMenu(id, payload),
        onSuccess: () => {
            message.success('菜单已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
        },
    });
    const batchVisibleMutation = useMutation({
        mutationFn: batchUpdateMenuVisible,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (input) => {
            if (typeof input === 'object' && 'cascade' in input) {
                return deleteMenu(input.id, { cascade: input.cascade });
            }
            return deleteMenu(input);
        },
        onSuccess: (data, variables) => {
            const isCascade = typeof variables === 'object' && 'cascade' in variables && variables.cascade;
            if (isCascade) {
                const count = typeof data === 'number' ? data : 1;
                message.success(`已级联删除 ${count} 个菜单`);
            }
            else {
                message.success('菜单已删除');
            }
            queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
        },
    });
    const openCreate = useCallback((parentId) => {
        setEditingId(null);
        originalVisibleRef.current = null;
        form.resetFields();
        form.setFieldsValue({
            parentId: parentId ?? 0,
            menuType: 'menu',
            sortOrder: 0,
            visible: 1,
            expanded: 1,
            status: 'enabled',
        });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        originalVisibleRef.current = record.visible;
        form.setFieldsValue({
            parentId: record.parentId,
            menuType: record.menuType,
            code: record.code,
            name: record.name,
            path: record.path,
            component: record.component,
            permissionCode: record.permissionCode,
            icon: record.icon,
            sortOrder: record.sortOrder,
            visible: record.visible,
            expanded: record.expanded ?? 1,
            status: record.status,
        });
        setModalOpen(true);
    }, [form]);
    const handleSubmit = useCallback(() => {
        form.validateFields().then((values) => {
            // Switch produces boolean; backend expects Integer (0/1)
            const newVisible = typeof values.visible === 'boolean' ? (values.visible ? 1 : 0) : values.visible;
            const newExpanded = typeof values.expanded === 'boolean' ? (values.expanded ? 1 : 0) : values.expanded;
            const payload = { ...values, visible: newVisible, expanded: newExpanded };
            // Check if we need to cascade visibility for directory type
            if (editingId &&
                values.menuType === 'directory' &&
                originalVisibleRef.current !== null &&
                newVisible !== originalVisibleRef.current) {
                const descendantIds = collectDescendantIds(treeData, editingId);
                if (descendantIds.length > 0) {
                    Modal.confirm({
                        title: '级联修改可见性',
                        content: `此操作将同时修改 ${descendantIds.length} 个子菜单的可见性，是否继续？`,
                        okText: '确认',
                        cancelText: '取消',
                        onOk: () => {
                            Promise.all([
                                updateMutation.mutateAsync({ id: editingId, payload }),
                                batchVisibleMutation.mutateAsync({ ids: descendantIds, visible: newVisible }),
                            ]).then(() => {
                                message.success(`已更新菜单及 ${descendantIds.length} 个子菜单的可见性`);
                                setModalOpen(false);
                                setEditingId(null);
                                form.resetFields();
                                queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] });
                            });
                        },
                    });
                    return;
                }
            }
            if (editingId) {
                updateMutation.mutate({ id: editingId, payload });
            }
            else {
                createMutation.mutate(payload);
            }
        });
    }, [form, editingId, treeData, createMutation, updateMutation, batchVisibleMutation, queryClient]);
    const handleDelete = useCallback((record) => {
        const descendantIds = collectDescendantIds(treeData, record.id);
        const hasChildren = descendantIds.length > 0;
        if (!hasChildren) {
            // 叶子菜单：直接走默认 delete
            Modal.confirm({
                title: `确认删除菜单「${record.name}」？`,
                content: '该菜单为叶子节点，无子菜单。',
                okText: '确认删除',
                okType: 'danger',
                cancelText: '取消',
                onOk: () => deleteMutation.mutate(record.id),
            });
            return;
        }
        // 父菜单：弹级联确认，传 cascade=true
        const names = descendantIds
            .slice(0, 5)
            .map((id) => {
            const findName = (list) => {
                for (const n of list) {
                    if (n.id === id)
                        return n.name;
                    if (n.children?.length) {
                        const r = findName(n.children);
                        if (r)
                            return r;
                    }
                }
                return null;
            };
            return findName(treeData) || `#${id}`;
        });
        const moreCount = descendantIds.length - names.length;
        const namePreview = names.join('、') + (moreCount > 0 ? ` 等 ${moreCount} 个` : '');
        Modal.confirm({
            title: `级联删除菜单「${record.name}」？`,
            content: (_jsxs("div", { children: [_jsxs("p", { style: { marginBottom: 8, color: '#dc2626', fontWeight: 500 }, children: ["\u8BE5\u83DC\u5355\u4E0B\u8FD8\u6709 ", descendantIds.length, " \u4E2A\u5B50\u83DC\u5355\uFF0C\u5220\u9664\u540E\u5C06\u7EA7\u8054\u6E05\u7A7A\u3002"] }), _jsxs("p", { style: { marginBottom: 8, color: '#475569' }, children: ["\u5B50\u83DC\u5355\uFF1A", namePreview] }), _jsx("p", { style: { marginBottom: 0, color: '#94a3b8', fontSize: 12 }, children: "\u540C\u65F6\u4F1A\u89E3\u9664\u6240\u6709\u89D2\u8272\u5BF9\u8BE5\u83DC\u5355\u53CA\u5176\u5B50\u83DC\u5355\u7684\u7ED1\u5B9A\uFF0C\u64CD\u4F5C\u4E0D\u53EF\u6062\u590D\u3002" })] })),
            okText: '级联删除',
            okType: 'danger',
            cancelText: '取消',
            width: 480,
            onOk: () => deleteMutation.mutate({ id: record.id, cascade: true }),
        });
    }, [deleteMutation, treeData]);
    const treeSelectData = buildTreeSelectData(treeData, editingId || undefined);
    const columns = [
        {
            title: '菜单名称',
            dataIndex: 'name',
            width: 220,
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '类型',
            dataIndex: 'menuType',
            width: 80,
            render: (v) => {
                const item = MENU_TYPE_TAG[v] || { color: 'default', text: v };
                return _jsx(Tag, { color: item.color, children: item.text });
            },
        },
        {
            title: '编码',
            dataIndex: 'code',
            width: 160,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '路径',
            dataIndex: 'path',
            width: 160,
            ellipsis: true,
            render: (v) => v || '-',
        },
        {
            title: '权限码',
            dataIndex: 'permissionCode',
            width: 140,
            ellipsis: true,
            render: (v) => v ? _jsx(Typography.Text, { code: true, children: v }) : '-',
        },
        {
            title: '图标',
            dataIndex: 'icon',
            width: 80,
            render: (v) => v ? _jsx("span", { title: v, children: iconFromName(v) || v }) : '-',
        },
        {
            title: '排序',
            dataIndex: 'sortOrder',
            width: 70,
        },
        {
            title: '可见',
            dataIndex: 'visible',
            width: 70,
            render: (v) => (v === 1 ? _jsx(Tag, { color: "success", children: "\u662F" }) : _jsx(Tag, { children: "\u5426" })),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        {
            title: '操作',
            width: 200,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => openCreate(record.id), children: "\u5B50\u7EA7" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(AppstoreOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u83DC\u5355\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7EF4\u62A4\u7CFB\u7EDF\u83DC\u5355\u7684\u6811\u5F62\u7ED3\u6784\uFF0C\u5305\u62EC\u76EE\u5F55\u3001\u9875\u9762\u83DC\u5355\u548C\u64CD\u4F5C\u6309\u94AE\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-menus-tree'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => openCreate(0), children: "\u65B0\u589E\u83DC\u5355" })] })] }), _jsx("div", { style: { padding: '0 24px' }, children: _jsx(Table, { rowKey: "id", size: "middle", loading: treeQuery.isLoading, dataSource: treeData, columns: columns, pagination: false, expandable: { defaultExpandAllRows: true } }) }), _jsx(Modal, { title: editingId ? '编辑菜单' : '新增菜单', open: modalOpen, width: 580, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    originalVisibleRef.current = null;
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsx(Form.Item, { name: "parentId", label: "\u4E0A\u7EA7\u83DC\u5355", children: _jsx(TreeSelect, { placeholder: "\u9009\u62E9\u4E0A\u7EA7\u83DC\u5355\uFF08\u4E0D\u9009\u5219\u4E3A\u9876\u7EA7\uFF09", allowClear: true, treeDefaultExpandAll: true, treeData: [
                                    { title: '顶级菜单', value: 0, children: treeSelectData },
                                ] }) }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "menuType", label: "\u83DC\u5355\u7C7B\u578B", rules: [{ required: true, message: '请选择菜单类型' }], style: { flex: 1 }, children: _jsx(Select, { options: MENU_TYPE_OPTIONS }) }), _jsx(Form.Item, { name: "name", label: "\u83DC\u5355\u540D\u79F0", rules: [{ required: true, message: '请输入菜单名称' }], style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u83DC\u5355\u540D\u79F0", maxLength: 64, onChange: (e) => {
                                            // Auto-generate fields only in create mode
                                            if (!editingId) {
                                                onNameChange(e.target.value);
                                            }
                                        } }) })] }), _jsx(Form.Item, { name: "code", label: "\u83DC\u5355\u7F16\u7801", rules: [{ required: true, message: '请输入菜单编码' }], children: _jsx(Input, { placeholder: "\u5982 system_user", maxLength: 64, disabled: !!editingId }) }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "path", label: "\u8DEF\u7531\u8DEF\u5F84", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u5982 /system/users", maxLength: 255 }) }), _jsx(Form.Item, { name: "component", label: "\u7EC4\u4EF6\u8DEF\u5F84", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u5982 system/UserPage", maxLength: 255 }) })] }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "permissionCode", label: "\u6743\u9650\u7801", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u5982 user:view", maxLength: 128 }) }), _jsx(Form.Item, { name: "icon", label: "\u56FE\u6807", style: { flex: 1 }, children: _jsx(IconPicker, { placeholder: "\u9009\u62E9\u56FE\u6807" }) })] }), _jsxs(Space, { size: 16, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "sortOrder", label: "\u6392\u5E8F", style: { flex: 1 }, children: _jsx(InputNumber, { min: 0, max: 9999, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "visible", label: "\u662F\u5426\u53EF\u89C1", valuePropName: "checked", style: { flex: 1 }, children: _jsx(Switch, { checkedChildren: "\u53EF\u89C1", unCheckedChildren: "\u9690\u85CF" }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", style: { flex: 1 }, children: _jsx(Select, { options: [
                                            { label: '启用', value: 'enabled' },
                                            { label: '停用', value: 'disabled' },
                                        ] }) }), menuType === 'directory' && (_jsx(Form.Item, { name: "expanded", label: "\u9ED8\u8BA4\u5C55\u5F00", valuePropName: "checked", style: { flex: 1 }, children: _jsx(Switch, { checkedChildren: "\u662F", unCheckedChildren: "\u5426" }) }))] })] }) })] }));
}

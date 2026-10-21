import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined, SafetyCertificateOutlined, UnorderedListOutlined, TableOutlined, } from '@ant-design/icons';
import { Button, Collapse, Select, Space, Table, Tag, Typography, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { getPermissionMatrix, getPermissionList } from '../../api/system';
export function PermissionPage() {
    const queryClient = useQueryClient();
    const [viewMode, setViewMode] = useState('matrix');
    const matrixQuery = useQuery({
        queryKey: ['system-perm-matrix'],
        queryFn: getPermissionMatrix,
    });
    const listQuery = useQuery({
        queryKey: ['system-perm-list'],
        queryFn: getPermissionList,
    });
    const matrixData = matrixQuery.data || [];
    const listData = listQuery.data || [];
    // Matrix view columns
    const matrixPermColumns = [
        {
            title: '权限名称',
            dataIndex: 'name',
            width: 140,
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '权限码',
            dataIndex: 'code',
            width: 180,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '操作',
            dataIndex: 'action',
            width: 100,
            render: (v) => {
                const map = {
                    view: { color: 'blue', text: '查看' },
                    create: { color: 'green', text: '创建' },
                    update: { color: 'orange', text: '更新' },
                    delete: { color: 'red', text: '删除' },
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
            title: '绑定角色',
            dataIndex: 'boundRoles',
            render: (roles) => roles?.length ? (_jsx(Space, { size: 4, wrap: true, children: roles.map((r) => (_jsx(Tag, { color: "processing", children: r.name }, r.id))) })) : (_jsx(Typography.Text, { type: "secondary", children: "\u672A\u7ED1\u5B9A" })),
        },
    ];
    // List view columns
    const listColumns = [
        {
            title: '序号',
            width: 70,
            render: (_, __, index) => index + 1,
        },
        {
            title: '权限名称',
            dataIndex: 'name',
            width: 140,
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        {
            title: '权限码',
            dataIndex: 'code',
            width: 180,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '操作',
            dataIndex: 'action',
            width: 80,
            render: (v) => {
                const map = {
                    view: { color: 'blue', text: '查看' },
                    create: { color: 'green', text: '创建' },
                    update: { color: 'orange', text: '更新' },
                    delete: { color: 'red', text: '删除' },
                };
                const item = map[v] || { color: 'default', text: v };
                return _jsx(Tag, { color: item.color, children: item.text });
            },
        },
        {
            title: '所属模块',
            dataIndex: 'menuName',
            width: 120,
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => v === 'enabled' ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        {
            title: '绑定角色',
            dataIndex: 'boundRoles',
            render: (roles) => roles?.length ? (_jsx(Space, { size: 4, wrap: true, children: roles.map((r) => (_jsx(Tag, { color: "processing", children: r.name }, r.id))) })) : (_jsx(Typography.Text, { type: "secondary", children: "\u672A\u7ED1\u5B9A" })),
        },
    ];
    // Stats
    const totalPermissions = matrixData.reduce((acc, g) => acc + g.permissions.length, 0);
    const totalModules = matrixData.length;
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(SafetyCertificateOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u6743\u9650\u7BA1\u7406" })] }), _jsxs(Typography.Text, { children: ["\u67E5\u770B\u7CFB\u7EDF\u4E2D\u6240\u6709\u6743\u9650\u70B9\u7684\u5206\u5E03\u548C\u89D2\u8272\u7ED1\u5B9A\u60C5\u51B5\u3002\u5171 ", totalModules, " \u4E2A\u6A21\u5757\uFF0C", totalPermissions, " \u4E2A\u6743\u9650\u70B9\u3002"] })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    queryClient.invalidateQueries({ queryKey: ['system-perm-matrix'] });
                                    queryClient.invalidateQueries({ queryKey: ['system-perm-list'] });
                                }, children: "\u5237\u65B0" }), _jsx(Select, { value: viewMode, onChange: setViewMode, style: { width: 130 }, options: [
                                    {
                                        label: (_jsxs(Space, { size: 4, children: [_jsx(TableOutlined, {}), " \u77E9\u9635\u89C6\u56FE"] })),
                                        value: 'matrix',
                                    },
                                    {
                                        label: (_jsxs(Space, { size: 4, children: [_jsx(UnorderedListOutlined, {}), " \u5217\u8868\u89C6\u56FE"] })),
                                        value: 'list',
                                    },
                                ] })] })] }), _jsx("div", { style: { padding: '0 24px' }, children: viewMode === 'matrix' ? (_jsx(Collapse, { defaultActiveKey: matrixData.map((g) => String(g.menuId)), items: matrixData.map((group) => ({
                        key: String(group.menuId),
                        label: (_jsxs(Space, { wrap: true, children: [_jsx(Typography.Text, { strong: true, children: group.menuName }), group.menuPath && group.menuPath.includes(' / ') && (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: group.menuPath })), _jsxs(Tag, { children: [group.permissions.length, " \u4E2A\u6743\u9650"] }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: group.menuCode })] })),
                        children: (_jsx(Table, { rowKey: "id", size: "small", dataSource: group.permissions, columns: matrixPermColumns, pagination: false })),
                    })) })) : (_jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: listData, columns: listColumns, pagination: {
                        pageSize: 50,
                        showSizeChanger: true,
                        showTotal: (t) => `共 ${t} 条`,
                    } })) })] }));
}

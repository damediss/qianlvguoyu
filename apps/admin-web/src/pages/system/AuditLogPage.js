import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined, SearchOutlined, FileTextOutlined, } from '@ant-design/icons';
import { Button, Input, Select, Space, Table, Tag, Typography, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pageAuditLogs } from '../../api/system';
const ACTION_COLORS = {
    CREATE: 'green',
    UPDATE: 'blue',
    DELETE: 'red',
    LOGIN: 'cyan',
    LOGOUT: 'orange',
};
export function AuditLogPage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({
        page: 1,
        pageSize: 20,
        module: '',
        action: '',
        targetType: '',
    });
    const listQuery = useQuery({
        queryKey: ['system-audit-logs', query],
        queryFn: () => pageAuditLogs(query),
    });
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const columns = [
        {
            title: '时间',
            dataIndex: 'createdAt',
            width: 180,
            render: (v) => v ? new Date(v).toLocaleString('zh-CN') : '-',
        },
        {
            title: '模块',
            dataIndex: 'module',
            width: 120,
            render: (text) => _jsx(Typography.Text, { code: true, children: text }),
        },
        {
            title: '操作',
            dataIndex: 'action',
            width: 200,
            render: (v) => (_jsx(Tag, { color: ACTION_COLORS[v] || 'default', children: v })),
        },
        {
            title: '目标类型',
            dataIndex: 'targetType',
            width: 120,
        },
        {
            title: '目标ID',
            dataIndex: 'targetId',
            width: 140,
            render: (v) => v ? _jsx(Typography.Text, { code: true, children: v }) : '-',
        },
        {
            title: '用户ID',
            dataIndex: 'userId',
            width: 120,
        },
        {
            title: 'IP 地址',
            dataIndex: 'ip',
            width: 140,
        },
        {
            title: '请求ID',
            dataIndex: 'requestId',
            ellipsis: true,
            render: (text) => (_jsx(Typography.Text, { copyable: { text }, style: { fontSize: 12, color: '#64748b' }, children: text ? text.slice(0, 20) + '...' : '-' })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(FileTextOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u5BA1\u8BA1\u65E5\u5FD7" })] }), _jsx(Typography.Text, { children: "\u8BB0\u5F55\u7CFB\u7EDF\u4E2D\u6240\u6709\u5173\u952E\u64CD\u4F5C\u7684\u5BA1\u8BA1\u8F68\u8FF9\uFF0C\u7528\u4E8E\u5B89\u5168\u5BA1\u8BA1\u548C\u95EE\u9898\u6392\u67E5\u3002" })] }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-audit-logs'] }), children: "\u5237\u65B0" })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u6A21\u5757\u540D\u79F0", allowClear: true, style: { width: 180 }, value: query.module, onChange: (e) => setQuery((prev) => ({ ...prev, module: e.target.value, page: 1 })) }), _jsx(Select, { placeholder: "\u64CD\u4F5C\u7C7B\u578B", allowClear: true, style: { width: 140 }, value: query.action || undefined, onChange: (v) => setQuery((prev) => ({ ...prev, action: v || '', page: 1 })), options: [
                                    { label: 'CREATE', value: 'CREATE' },
                                    { label: 'UPDATE', value: 'UPDATE' },
                                    { label: 'DELETE', value: 'DELETE' },
                                    { label: 'LOGIN', value: 'LOGIN' },
                                    { label: 'LOGOUT', value: 'LOGOUT' },
                                ] }), _jsx(Input, { placeholder: "\u76EE\u6807\u7C7B\u578B", allowClear: true, style: { width: 160 }, value: query.targetType, onChange: (e) => setQuery((prev) => ({ ...prev, targetType: e.target.value, page: 1 })) }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['system-audit-logs'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] })] }));
}

import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EyeOutlined, FileDoneOutlined, ReloadOutlined, SearchOutlined, } from '@ant-design/icons';
import { Button, Card, Empty, Input, Modal, Space, Table, Tag, Typography, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pageArchiveRecords } from '../../api/certificationArchive';
import { PermissionGate } from '../../directives/permission';
import { ArchiveDetail } from './archive/ArchiveDetail';
import { archiveStatusTag, dateTimeText, } from './archive/shared';
function archiveFlagTag(value) {
    return value
        ? _jsx(Tag, { color: "success", children: "\u5DF2\u5B8C\u6210" })
        : _jsx(Tag, { children: "\u672A\u5B8C\u6210" });
}
/**
 * 任务档案：只展示已经完成归档的记录，并提供归档内容查看、下载入口。
 */
export function TaskArchivePage() {
    const queryClient = useQueryClient();
    const [keywordInput, setKeywordInput] = useState('');
    const [query, setQuery] = useState({
        page: 1,
        pageSize: 10,
        archiveStatus: 'ARCHIVED',
    });
    const [selectedArchiveId, setSelectedArchiveId] = useState();
    const recordsQuery = useQuery({
        queryKey: ['certification-task-archive-records', query],
        queryFn: () => pageArchiveRecords(query),
    });
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-task-archive-records'] });
    };
    const search = (keyword) => {
        setQuery((previous) => ({
            ...previous,
            keyword: keyword.trim() || undefined,
            page: 1,
        }));
    };
    const columns = [
        { title: '任务编号', dataIndex: 'taskNo', width: 160, fixed: 'left' },
        { title: '任务名称', dataIndex: 'taskName', width: 240, ellipsis: true },
        { title: '认定批次', dataIndex: 'batchName', width: 160, render: (value) => value || '-' },
        { title: '工种/职业', dataIndex: 'workTypeName', width: 150, render: (value) => value || '-' },
        { title: '等级', dataIndex: 'levelName', width: 120, render: (value) => value || '-' },
        { title: '归档人数', dataIndex: 'candidateCount', width: 100 },
        {
            title: '基础信息',
            dataIndex: 'basicArchived',
            width: 110,
            render: archiveFlagTag,
        },
        {
            title: '归档材料',
            dataIndex: 'materialArchived',
            width: 110,
            render: archiveFlagTag,
        },
        {
            title: '归档状态',
            dataIndex: 'archiveStatus',
            width: 160,
            render: archiveStatusTag,
        },
        {
            title: '归档时间',
            dataIndex: 'archivedAt',
            width: 180,
            render: dateTimeText,
        },
        {
            title: '操作人',
            dataIndex: 'lastOperatorName',
            width: 110,
            render: (value) => value || '-',
        },
        {
            title: '操作',
            width: 110,
            fixed: 'right',
            render: (_, record) => (_jsx(Button, { size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => setSelectedArchiveId(record.id), children: "\u67E5\u770B" })),
        },
    ];
    return (_jsx(PermissionGate, { code: "certification.archive.record.view", fallback: _jsx(Empty, { description: "\u6682\u65E0\u4EFB\u52A1\u6863\u6848\u8BBF\u95EE\u6743\u9650" }), children: _jsxs("div", { className: "knowledge-page", children: [_jsxs("div", { className: "page-heading knowledge-heading", children: [_jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(FileDoneOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u4EFB\u52A1\u6863\u6848" }), _jsx(Typography.Text, { type: "secondary", children: "\u96C6\u4E2D\u67E5\u770B\u5DF2\u5B8C\u6210\u5F52\u6863\u7684\u8BA4\u5B9A\u4EFB\u52A1\u3001\u4EFB\u52A1\u6750\u6599\u548C\u8003\u751F\u6750\u6599" })] })] }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" })] }), _jsxs(Card, { title: "\u5DF2\u5F52\u6863\u4EFB\u52A1", size: "small", children: [_jsx(Space, { wrap: true, style: { marginBottom: 12 }, children: _jsx(Input.Search, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u4EFB\u52A1\u7F16\u53F7\u6216\u4EFB\u52A1\u540D\u79F0", style: { width: 320 }, value: keywordInput, onChange: (event) => setKeywordInput(event.target.value), onSearch: search }) }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: recordsQuery.data?.items || [], loading: recordsQuery.isFetching, scroll: { x: 1650 }, pagination: {
                                current: query.page,
                                pageSize: query.pageSize,
                                total: recordsQuery.data?.total || 0,
                                showSizeChanger: true,
                                showTotal: (total) => `共 ${total} 条已归档任务`,
                                onChange: (page, pageSize) => setQuery((previous) => ({
                                    ...previous,
                                    page,
                                    pageSize,
                                })),
                            } })] }), _jsx(Modal, { title: "\u4EFB\u52A1\u6863\u6848\u8BE6\u60C5", open: !!selectedArchiveId, width: 1120, footer: null, destroyOnHidden: true, onCancel: () => setSelectedArchiveId(undefined), children: _jsx(ArchiveDetail, { archiveId: selectedArchiveId }) })] }) }));
}

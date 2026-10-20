import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CheckCircleOutlined, CloseCircleOutlined, DownloadOutlined } from '@ant-design/icons';
import { Alert, Button, Descriptions, Empty, List, message, Space, Table, Tabs, Typography } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { confirmArchiveRecord, downloadArchiveFile, getArchiveCompleteCheck, getArchiveDetail, pageArchiveCandidates, } from '../../../api/certificationArchive';
import { archiveStatusTag, dateTimeText, fileSizeText } from './shared';
export function ArchiveDetail({ archiveId, onCompleted }) {
    const queryClient = useQueryClient();
    const detailQuery = useQuery({
        queryKey: ['certification-archive-detail', archiveId],
        queryFn: () => getArchiveDetail(archiveId),
        enabled: !!archiveId,
    });
    const candidatesQuery = useQuery({
        queryKey: ['certification-archive-candidates', archiveId],
        queryFn: () => pageArchiveCandidates(archiveId, { page: 1, pageSize: 50 }),
        enabled: !!archiveId,
    });
    const checkQuery = useQuery({
        queryKey: ['certification-archive-complete-check', archiveId],
        queryFn: () => getArchiveCompleteCheck(archiveId),
        enabled: !!archiveId,
    });
    const confirmMutation = useMutation({
        mutationFn: () => confirmArchiveRecord(archiveId),
        onSuccess: (result) => {
            if (!result.passed) {
                message.warning(result.items.find((item) => !item.passed)?.message || '归档结束验核未通过');
                return;
            }
            message.success('认定归档已完成，任务资源已释放');
            queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
            queryClient.invalidateQueries({ queryKey: ['certification-archive-detail', archiveId] });
            onCompleted?.();
        },
    });
    if (!archiveId) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u5F52\u6863\u8BB0\u5F55" });
    }
    const detail = detailQuery.data;
    if (!detail) {
        return _jsx(Empty, { description: detailQuery.isLoading ? '加载中' : '暂无归档详情' });
    }
    return (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsxs(Descriptions, { bordered: true, size: "small", column: 3, children: [_jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7F16\u53F7", children: detail.record.taskNo }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u540D\u79F0", children: detail.record.taskName }), _jsx(Descriptions.Item, { label: "\u5F52\u6863\u72B6\u6001", children: archiveStatusTag(detail.record.archiveStatus) }), _jsx(Descriptions.Item, { label: "\u5DE5\u79CD", children: detail.record.workTypeName || '-' }), _jsx(Descriptions.Item, { label: "\u7B49\u7EA7", children: detail.record.levelName || '-' }), _jsx(Descriptions.Item, { label: "\u8BA4\u5B9A\u4EBA\u6570", children: detail.record.candidateCount }), _jsx(Descriptions.Item, { label: "\u5B8C\u6210\u65F6\u95F4", children: dateTimeText(detail.record.completedAt) }), _jsx(Descriptions.Item, { label: "\u6700\u8FD1\u64CD\u4F5C\u4EBA", children: detail.record.lastOperatorName || '-' }), _jsx(Descriptions.Item, { label: "\u6700\u8FD1\u5F52\u6863", children: dateTimeText(detail.record.archivedAt) })] }), _jsx(Tabs, { items: [
                    {
                        key: 'materials',
                        label: '认定材料',
                        children: _jsx(MaterialTable, { data: detail.materials }),
                    },
                    {
                        key: 'candidates',
                        label: '考生档案',
                        children: _jsx(CandidateTable, { data: candidatesQuery.data?.items || [], loading: candidatesQuery.isLoading }),
                    },
                ] }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 5, children: "\u5F52\u6863\u7ED3\u675F\u9A8C\u6838" }), _jsx(List, { bordered: true, loading: checkQuery.isLoading, dataSource: checkQuery.data?.items || [], renderItem: (item) => (_jsx(List.Item, { children: _jsxs(Space, { children: [item.passed
                                        ? _jsx(CheckCircleOutlined, { style: { color: '#16a34a' } })
                                        : _jsx(CloseCircleOutlined, { style: { color: '#dc2626' } }), _jsx(Typography.Text, { strong: true, children: item.name }), _jsx(Typography.Text, { type: item.passed ? 'secondary' : 'danger', children: item.message })] }) })) })] }), detail.record.archiveStatus === 'ARCHIVED' ? (_jsx(Alert, { type: "success", showIcon: true, message: "\u8BE5\u4EFB\u52A1\u5DF2\u5B8C\u6210\u8BA4\u5B9A\u5F52\u6863" })) : (_jsx(Space, { style: { width: '100%', justifyContent: 'flex-end' }, children: _jsx(Button, { type: "primary", disabled: !checkQuery.data?.passed, loading: confirmMutation.isPending, onClick: () => confirmMutation.mutate(), children: "\u5B8C\u6210\u5F52\u6863" }) }))] }));
}
function MaterialTable({ data }) {
    const columns = [
        { title: '材料名称', dataIndex: 'materialName', width: 260 },
        { title: '文件名', dataIndex: 'fileName', width: 260, ellipsis: true },
        { title: '文件大小', dataIndex: 'fileSize', width: 120, render: fileSizeText },
        { title: '上传时间', dataIndex: 'uploadedAt', width: 180, render: dateTimeText },
        {
            title: '操作',
            width: 100,
            render: (_, row) => _jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), onClick: () => downloadArchiveFile(row.id, row.fileName), children: "\u4E0B\u8F7D" }),
        },
    ];
    return _jsx(Table, { rowKey: "id", columns: columns, dataSource: data, pagination: false, scroll: { x: 900 } });
}
function CandidateTable({ data, loading }) {
    const columns = [
        { title: '考生编号', dataIndex: 'candidateNo', width: 130 },
        { title: '姓名', dataIndex: 'name', width: 110 },
        { title: '身份证号', dataIndex: 'maskedIdCard', width: 180 },
        { title: '文件夹', dataIndex: 'folderName', width: 220, ellipsis: true },
    ];
    return (_jsx(Table, { rowKey: "id", loading: loading, columns: columns, dataSource: data, expandable: { expandedRowRender: (row) => _jsx(CandidateMaterialTable, { data: row.materials || [] }) }, pagination: false, scroll: { x: 800 } }));
}
function CandidateMaterialTable({ data }) {
    const columns = [
        { title: '材料类型', dataIndex: 'materialType', width: 180 },
        { title: '材料分组', dataIndex: 'materialGroup', width: 160 },
        { title: '文件名', dataIndex: 'fileName', width: 260, ellipsis: true },
        { title: '大小', dataIndex: 'fileSize', width: 120, render: fileSizeText },
        {
            title: '操作',
            width: 100,
            render: (_, row) => _jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), onClick: () => downloadArchiveFile(row.id, row.fileName), children: "\u4E0B\u8F7D" }),
        },
    ];
    return _jsx(Table, { rowKey: "id", size: "small", columns: columns, dataSource: data, pagination: false });
}

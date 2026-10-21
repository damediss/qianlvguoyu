import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { Button, Modal, Space, Table } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cancelArchiveImportBatch, confirmArchiveImportBatch, pageArchiveImportBatches, pageArchiveImportErrors, } from '../../../api/certificationArchive';
import { batchStatusTag, dateTimeText } from './shared';
export function ArchiveImportBatch({ archiveId }) {
    const queryClient = useQueryClient();
    const batchesQuery = useQuery({
        queryKey: ['certification-archive-batches', archiveId],
        queryFn: () => pageArchiveImportBatches({ page: 1, pageSize: 20, archiveId }),
        enabled: !!archiveId,
    });
    const confirmMutation = useMutation({
        mutationFn: confirmArchiveImportBatch,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['certification-archive-batches', archiveId] });
            queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
        },
    });
    const cancelMutation = useMutation({
        mutationFn: cancelArchiveImportBatch,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['certification-archive-batches', archiveId] }),
    });
    const showErrors = (batch) => {
        Modal.info({
            title: `异常列表 - ${batch.batchNo}`,
            width: 860,
            content: _jsx(BatchErrors, { batchId: batch.id }),
        });
    };
    const columns = [
        { title: '批次号', dataIndex: 'batchNo', width: 210 },
        { title: 'ZIP 文件', dataIndex: 'zipFileName', width: 220, ellipsis: true },
        { title: '状态', dataIndex: 'status', width: 130, render: batchStatusTag },
        { title: '考生数', dataIndex: 'totalCandidates', width: 90 },
        { title: '成功数', dataIndex: 'successCount', width: 90 },
        { title: '异常数', dataIndex: 'errorCount', width: 90 },
        { title: '创建时间', dataIndex: 'createdAt', width: 170, render: dateTimeText },
        {
            title: '操作',
            width: 240,
            render: (_, row) => (_jsxs(Space, { children: [_jsx(Button, { size: "small", disabled: !row.errorCount, onClick: () => showErrors(row), children: "\u5F02\u5E38" }), _jsx(Button, { size: "small", icon: _jsx(CheckOutlined, {}), disabled: row.status !== 'PARSE_SUCCESS', onClick: () => confirmMutation.mutate(row.id), children: "\u786E\u8BA4" }), _jsx(Button, { size: "small", icon: _jsx(CloseOutlined, {}), disabled: row.status === 'CONFIRMED', onClick: () => cancelMutation.mutate(row.id), children: "\u53D6\u6D88" })] })),
        },
    ];
    return (_jsx(Table, { rowKey: "id", columns: columns, dataSource: batchesQuery.data?.items || [], loading: batchesQuery.isLoading, pagination: false, scroll: { x: 1100 } }));
}
function BatchErrors({ batchId }) {
    const query = useQuery({
        queryKey: ['certification-archive-batch-errors', batchId],
        queryFn: () => pageArchiveImportErrors(batchId, { page: 1, pageSize: 100 }),
    });
    const columns = [
        { title: '考生编号', dataIndex: 'candidateNo', width: 120 },
        { title: '姓名', dataIndex: 'name', width: 100 },
        { title: '身份证号', dataIndex: 'maskedIdCard', width: 180 },
        { title: '异常类型', dataIndex: 'errorType', width: 180 },
        { title: '异常说明', dataIndex: 'errorMessage' },
    ];
    return _jsx(Table, { rowKey: "id", size: "small", columns: columns, dataSource: query.data?.items || [], loading: query.isLoading, pagination: false });
}

import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { Button, Modal, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cancelArchiveImportBatch,
  confirmArchiveImportBatch,
  pageArchiveImportBatches,
  pageArchiveImportErrors,
} from '../../../api/certificationArchive';
import type { CertificationArchiveImportBatch, CertificationArchiveImportError } from '../../../types/certificationArchive';
import { batchStatusTag, dateTimeText } from './shared';

export function ArchiveImportBatch({ archiveId }: { archiveId?: string }) {
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

  const showErrors = (batch: CertificationArchiveImportBatch) => {
    Modal.info({
      title: `异常列表 - ${batch.batchNo}`,
      width: 860,
      content: <BatchErrors batchId={batch.id} />,
    });
  };

  const columns: ColumnsType<CertificationArchiveImportBatch> = [
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
      render: (_, row) => (
        <Space>
          <Button size="small" disabled={!row.errorCount} onClick={() => showErrors(row)}>异常</Button>
          <Button size="small" icon={<CheckOutlined />} disabled={row.status !== 'PARSE_SUCCESS'} onClick={() => confirmMutation.mutate(row.id)}>确认</Button>
          <Button size="small" icon={<CloseOutlined />} disabled={row.status === 'CONFIRMED'} onClick={() => cancelMutation.mutate(row.id)}>取消</Button>
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={batchesQuery.data?.items || []}
      loading={batchesQuery.isLoading}
      pagination={false}
      scroll={{ x: 1100 }}
    />
  );
}

function BatchErrors({ batchId }: { batchId: string }) {
  const query = useQuery({
    queryKey: ['certification-archive-batch-errors', batchId],
    queryFn: () => pageArchiveImportErrors(batchId, { page: 1, pageSize: 100 }),
  });
  const columns: ColumnsType<CertificationArchiveImportError> = [
    { title: '考生编号', dataIndex: 'candidateNo', width: 120 },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '身份证号', dataIndex: 'maskedIdCard', width: 180 },
    { title: '异常类型', dataIndex: 'errorType', width: 180 },
    { title: '异常说明', dataIndex: 'errorMessage' },
  ];
  return <Table rowKey="id" size="small" columns={columns} dataSource={query.data?.items || []} loading={query.isLoading} pagination={false} />;
}

import { DownloadOutlined } from '@ant-design/icons';
import { Button, Descriptions, Empty, Space, Table, Tabs } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { downloadArchiveFile, getArchiveDetail, pageArchiveCandidates } from '../../../api/certificationArchive';
import type { CertificationArchiveCandidate, CertificationArchiveCandidateMaterial, CertificationArchiveMaterial } from '../../../types/certificationArchive';
import { archiveStatusTag, dateTimeText, fileSizeText } from './shared';

export function ArchiveDetail({ archiveId }: { archiveId?: string }) {
  const detailQuery = useQuery({
    queryKey: ['certification-archive-detail', archiveId],
    queryFn: () => getArchiveDetail(archiveId!),
    enabled: !!archiveId,
  });
  const candidatesQuery = useQuery({
    queryKey: ['certification-archive-candidates', archiveId],
    queryFn: () => pageArchiveCandidates(archiveId!, { page: 1, pageSize: 50 }),
    enabled: !!archiveId,
  });

  if (!archiveId) {
    return <Empty description="请选择归档记录" />;
  }
  const detail = detailQuery.data;
  if (!detail) {
    return <Empty description={detailQuery.isLoading ? '加载中' : '暂无归档详情'} />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Descriptions bordered size="small" column={3}>
        <Descriptions.Item label="任务编号">{detail.record.taskNo}</Descriptions.Item>
        <Descriptions.Item label="任务名称">{detail.record.taskName}</Descriptions.Item>
        <Descriptions.Item label="归档状态">{archiveStatusTag(detail.record.archiveStatus)}</Descriptions.Item>
        <Descriptions.Item label="工种">{detail.record.workTypeName || '-'}</Descriptions.Item>
        <Descriptions.Item label="等级">{detail.record.levelName || '-'}</Descriptions.Item>
        <Descriptions.Item label="认定人数">{detail.record.candidateCount}</Descriptions.Item>
        <Descriptions.Item label="完成时间">{dateTimeText(detail.record.completedAt)}</Descriptions.Item>
        <Descriptions.Item label="最近操作人">{detail.record.lastOperatorName || '-'}</Descriptions.Item>
        <Descriptions.Item label="最近归档">{dateTimeText(detail.record.archivedAt)}</Descriptions.Item>
      </Descriptions>
      <Tabs
        items={[
          {
            key: 'materials',
            label: '认定材料',
            children: <MaterialTable data={detail.materials} />,
          },
          {
            key: 'candidates',
            label: '考生档案',
            children: <CandidateTable data={candidatesQuery.data?.items || []} loading={candidatesQuery.isLoading} />,
          },
        ]}
      />
    </Space>
  );
}

function MaterialTable({ data }: { data: CertificationArchiveMaterial[] }) {
  const columns: ColumnsType<CertificationArchiveMaterial> = [
    { title: '材料名称', dataIndex: 'materialName', width: 260 },
    { title: '文件名', dataIndex: 'fileName', width: 260, ellipsis: true },
    { title: '文件大小', dataIndex: 'fileSize', width: 120, render: fileSizeText },
    { title: '上传时间', dataIndex: 'uploadedAt', width: 180, render: dateTimeText },
    {
      title: '操作',
      width: 100,
      render: (_, row) => <Button size="small" icon={<DownloadOutlined />} onClick={() => downloadArchiveFile(row.id, row.fileName)}>下载</Button>,
    },
  ];
  return <Table rowKey="id" columns={columns} dataSource={data} pagination={false} scroll={{ x: 900 }} />;
}

function CandidateTable({ data, loading }: { data: CertificationArchiveCandidate[]; loading?: boolean }) {
  const columns: ColumnsType<CertificationArchiveCandidate> = [
    { title: '考生编号', dataIndex: 'candidateNo', width: 130 },
    { title: '姓名', dataIndex: 'name', width: 110 },
    { title: '身份证号', dataIndex: 'maskedIdCard', width: 180 },
    { title: '文件夹', dataIndex: 'folderName', width: 220, ellipsis: true },
  ];
  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={data}
      expandable={{ expandedRowRender: (row) => <CandidateMaterialTable data={row.materials || []} /> }}
      pagination={false}
      scroll={{ x: 800 }}
    />
  );
}

function CandidateMaterialTable({ data }: { data: CertificationArchiveCandidateMaterial[] }) {
  const columns: ColumnsType<CertificationArchiveCandidateMaterial> = [
    { title: '材料类型', dataIndex: 'materialType', width: 180 },
    { title: '材料分组', dataIndex: 'materialGroup', width: 160 },
    { title: '文件名', dataIndex: 'fileName', width: 260, ellipsis: true },
    { title: '大小', dataIndex: 'fileSize', width: 120, render: fileSizeText },
    {
      title: '操作',
      width: 100,
      render: (_, row) => <Button size="small" icon={<DownloadOutlined />} onClick={() => downloadArchiveFile(row.id, row.fileName)}>下载</Button>,
    },
  ];
  return <Table rowKey="id" size="small" columns={columns} dataSource={data} pagination={false} />;
}

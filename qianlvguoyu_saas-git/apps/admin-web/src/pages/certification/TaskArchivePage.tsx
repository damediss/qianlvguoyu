import {
  EyeOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Empty,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pageArchiveRecords } from '../../api/certificationArchive';
import { PermissionGate } from '../../directives/permission';
import type {
  ArchivePageQuery,
  CertificationArchiveRecord,
} from '../../types/certificationArchive';
import { ArchiveDetail } from './archive/ArchiveDetail';
import {
  archiveStatusTag,
  dateTimeText,
} from './archive/shared';

function archiveFlagTag(value?: boolean) {
  return value
    ? <Tag color="success">已完成</Tag>
    : <Tag>未完成</Tag>;
}

/**
 * 任务档案：只展示已经完成归档的记录，并提供归档内容查看、下载入口。
 */
export function TaskArchivePage() {
  const queryClient = useQueryClient();
  const [keywordInput, setKeywordInput] = useState('');
  const [query, setQuery] = useState<ArchivePageQuery>({
    page: 1,
    pageSize: 10,
    archiveStatus: 'ARCHIVED',
  });
  const [selectedArchiveId, setSelectedArchiveId] = useState<string>();

  const recordsQuery = useQuery({
    queryKey: ['certification-task-archive-records', query],
    queryFn: () => pageArchiveRecords(query),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-task-archive-records'] });
  };

  const search = (keyword: string) => {
    setQuery((previous) => ({
      ...previous,
      keyword: keyword.trim() || undefined,
      page: 1,
    }));
  };

  const columns: ColumnsType<CertificationArchiveRecord> = [
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
      render: (_, record) => (
        <Button
          size="small"
          icon={<EyeOutlined />}
          onClick={() => setSelectedArchiveId(record.id)}
        >
          查看
        </Button>
      ),
    },
  ];

  return (
    <PermissionGate
      code="certification.archive.record.view"
      fallback={<Empty description="暂无任务档案访问权限" />}
    >
      <div className="knowledge-page">
        <div className="page-heading knowledge-heading">
          <div className="page-title-line">
            <div className="page-title-icon"><FileDoneOutlined /></div>
            <div>
              <Typography.Title level={1}>任务档案</Typography.Title>
              <Typography.Text type="secondary">
                集中查看已完成归档的认定任务、任务材料和考生材料
              </Typography.Text>
            </div>
          </div>
          <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
        </div>

        <Card title="已归档任务" size="small">
          <Space wrap style={{ marginBottom: 12 }}>
            <Input.Search
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索任务编号或任务名称"
              style={{ width: 320 }}
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              onSearch={search}
            />
          </Space>
          <Table
            rowKey="id"
            columns={columns}
            dataSource={recordsQuery.data?.items || []}
            loading={recordsQuery.isFetching}
            scroll={{ x: 1650 }}
            pagination={{
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
            }}
          />
        </Card>

        <Modal
          title="任务档案详情"
          open={!!selectedArchiveId}
          width={1120}
          footer={null}
          destroyOnHidden
          onCancel={() => setSelectedArchiveId(undefined)}
        >
          <ArchiveDetail archiveId={selectedArchiveId} />
        </Modal>
      </div>
    </PermissionGate>
  );
}

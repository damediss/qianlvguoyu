import { FolderOpenOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Input, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { pageAvailableArchiveTasks } from '../../../api/certificationArchive';
import type { CertificationArchiveTask } from '../../../types/certificationArchive';
import { archiveStatusTag, dateTimeText } from './shared';

interface Props {
  keyword?: string;
  onKeywordChange: (keyword?: string) => void;
  onArchiveBasic: (task: CertificationArchiveTask) => void;
  onSelectArchive: (archiveId: string) => void;
}

export function ArchiveWorkbench({ keyword, onKeywordChange, onArchiveBasic, onSelectArchive }: Props) {
  const query = useQuery({
    queryKey: ['certification-archive-tasks', keyword],
    queryFn: () => pageAvailableArchiveTasks({ page: 1, pageSize: 20, keyword, status: 'preparation_completed' }),
  });

  const columns: ColumnsType<CertificationArchiveTask> = [
    { title: '任务编号', dataIndex: 'taskNo', width: 150, fixed: 'left' },
    { title: '任务名称', dataIndex: 'taskName', width: 230, ellipsis: true },
    { title: '认定批次', dataIndex: 'batchName', width: 150, render: (v) => v || '-' },
    { title: '工种/职业', dataIndex: 'workTypeName', width: 140, render: (v) => v || '-' },
    { title: '等级', dataIndex: 'levelName', width: 110, render: (v) => v || '-' },
    { title: '完成时间', dataIndex: 'completedAt', width: 170, render: dateTimeText },
    { title: '人数', dataIndex: 'candidateCount', width: 80 },
    { title: '任务状态', dataIndex: 'taskStatus', width: 140, render: (v) => v === 'preparation_completed' ? '认定筹备已完成' : v || '-' },
    { title: '基础归档', dataIndex: 'basicArchived', width: 110, render: (v) => v ? '已归档' : '未归档' },
    { title: '资料归档', dataIndex: 'materialArchived', width: 110, render: (v) => v ? '已归档' : '未归档' },
    { title: '归档状态', dataIndex: 'archiveStatus', width: 140, render: archiveStatusTag },
    { title: '最近归档', dataIndex: 'archivedAt', width: 170, render: dateTimeText },
    { title: '操作人', dataIndex: 'lastOperatorName', width: 110, render: (v) => v || '-' },
    {
      title: '操作',
      width: 190,
      fixed: 'right',
      render: (_, row) => (
        <Space>
          <Button size="small" onClick={() => onArchiveBasic(row)}>基础归档</Button>
          <Button
            size="small"
            icon={<FolderOpenOutlined />}
            disabled={!row.archiveId}
            onClick={() => row.archiveId && onSelectArchive(row.archiveId)}
          >
            详情
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Space wrap>
        <Input.Search
          allowClear
          placeholder="搜索任务编号或名称"
          style={{ width: 280 }}
          defaultValue={keyword}
          onSearch={(value) => onKeywordChange(value || undefined)}
        />
        <Button icon={<ReloadOutlined />} onClick={() => query.refetch()}>刷新</Button>
      </Space>
      <Table
        rowKey="taskId"
        loading={query.isLoading}
        columns={columns}
        dataSource={query.data?.items || []}
        pagination={false}
        scroll={{ x: 1600 }}
      />
    </Space>
  );
}

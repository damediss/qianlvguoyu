import { FileDoneOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Card, Empty, Input, Progress, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pageCertificationTasks } from '../../api/certification';
import { PermissionGate } from '../../directives/permission';
import type { CertificationTask, CertificationTaskQuery } from '../../types/certification';

const phaseLabelMap: Record<string, string> = {
  task_creation: '任务创建阶段',
  preparation: '认定筹备阶段',
  archive: '认定归档阶段',
  certification_completed: '认定完成',
};

const statusLabelMap: Record<string, string> = {
  in_progress: '进行中',
  pending_review: '待审核',
  rejected: '已驳回',
  completed: '已完成',
};

function phaseTag(value?: string) {
  const label = value ? phaseLabelMap[value] || value : '-';
  const color = value === 'certification_completed' ? 'success' : 'default';
  return <Tag color={color}>{label}</Tag>;
}

function statusTag(value?: string) {
  const label = value ? statusLabelMap[value] || value : '-';
  const colorMap: Record<string, string> = {
    in_progress: 'processing',
    pending_review: 'warning',
    rejected: 'error',
    completed: 'success',
  };
  const color = value ? colorMap[value] || 'default' : 'default';
  return <Tag color={color}>{label}</Tag>;
}

function dash(value?: unknown) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function dateTimeText(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

/**
 * 任务档案：展示 current_phase = certification_completed 的任务清单。
 */
export function TaskArchivePage() {
  const queryClient = useQueryClient();
  // 任务档案模块：固定按任务阶段 = certification_completed 过滤
  const [query, setQuery] = useState<CertificationTaskQuery>({
    page: 1,
    pageSize: 10,
    currentPhase: 'certification_completed',
  });

  const tasksQuery = useQuery({
    queryKey: ['certification-task-archive', query],
    queryFn: () => pageCertificationTasks(query),
  });

  const tasks = tasksQuery.data?.items || [];

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-task-archive'] });
  };

  const columns: ColumnsType<CertificationTask> = [
    { title: '任务编号', dataIndex: 'taskNo', width: 160, fixed: 'left' },
    { title: '任务名称', dataIndex: 'taskName', width: 230, ellipsis: true },
    { title: '任务经理', dataIndex: 'taskManagerName', width: 110 },
    { title: '工种', dataIndex: 'workTypeName', width: 130, render: dash },
    { title: '等级', dataIndex: 'levelName', width: 110, render: dash },
    { title: '计划人数', dataIndex: 'plannedPeopleCount', width: 100, render: dash },
    { title: '考试日期', dataIndex: 'examDate', width: 120, render: dash },
    {
      title: '当前阶段',
      dataIndex: 'currentPhase',
      width: 140,
      render: (value: string) => phaseTag(value),
    },
    {
      title: '任务状态',
      dataIndex: 'status',
      width: 110,
      render: (value: string) => statusTag(value),
    },
    {
      title: '阶段进度',
      width: 140,
      render: (_, record) => {
        const total = record.phaseTotalCount || 4;
        const done = record.phaseCompletedCount || 0;
        const percent = total ? Math.round((done / total) * 100) : 0;
        return <Progress percent={percent} size="small" />;
      },
    },
    {
      title: '完成时间',
      dataIndex: 'lastBusinessUpdatedAt',
      width: 170,
      render: dateTimeText,
    },
  ];

  return (
    <PermissionGate code="certification.archive.task.view" fallback={<Empty description="暂无任务档案访问权限" />}>
      <div className="knowledge-page">
        <div className="page-heading knowledge-heading">
          <div>
            <div className="page-title-line">
              <div className="page-title-icon"><FileDoneOutlined /></div>
              <div>
                <Typography.Title level={1}>任务档案</Typography.Title>
                <Typography.Text type="secondary">已经走完“认定完成”阶段的认定任务归档清单</Typography.Text>
              </div>
            </div>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
          </Space>
        </div>

        <Card title="任务列表" size="small">
          <Space wrap style={{ marginBottom: 12 }}>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索任务编号、名称、经理"
              style={{ width: 280 }}
              value={query.keyword}
              onChange={(event) => setQuery((prev) => ({
                ...prev,
                keyword: event.target.value,
                page: 1,
              }))}
            />
          </Space>
          <Table
            rowKey="id"
            columns={columns}
            dataSource={tasks}
            loading={tasksQuery.isFetching}
            scroll={{ x: 1600 }}
            pagination={{
              current: query.page,
              pageSize: query.pageSize,
              total: tasksQuery.data?.total || 0,
              showSizeChanger: true,
              onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
            }}
          />
        </Card>
      </div>
    </PermissionGate>
  );
}

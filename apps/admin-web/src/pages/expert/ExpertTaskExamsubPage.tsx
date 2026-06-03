// 专家端:主观题审核
// 菜单 code: expert_task_examsub (per plan §3.2)

import { useState } from 'react';
import { Button, Form, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import { FormOutlined, ReloadOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  pageMyTaskExamsubs,
  getExpertTaskExamsubDetail,
  submitExpertTaskExamsub,
} from '../../api/expert';
import { useAuthStore } from '../../stores/authStore';
import type { ExpertMyTaskVO, ExpertMyTaskQuery, ExpertTaskSubmitReq } from '../../types/expert';
import { ExpertAuditForm } from './components/ExpertAuditForm';

const STATUS_TAG: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'default' },
  1: { label: '初审中', color: 'processing' },
  2: { label: '终审中', color: 'warning' },
  3: { label: '完成', color: 'success' },
};

const ROLE_TAG: Record<number, { label: string; color: string }> = {
  0: { label: '初审', color: 'green' },
  1: { label: '终审', color: 'red' },
};

export function ExpertTaskExamsubPage() {
  const qc = useQueryClient();
  const hasPerm = useAuthStore((s) => s.hasPermission);
  const [query, setQuery] = useState<ExpertMyTaskQuery>({ page: 1, pageSize: 20 });
  const [auditOpen, setAuditOpen] = useState(false);
  const [currentTask, setCurrentTask] = useState<ExpertMyTaskVO | null>(null);

  const listQuery = useQuery({
    queryKey: ['expert-task-examsubs', query],
    queryFn: () => pageMyTaskExamsubs(query),
  });

  const detailQuery = useQuery({
    queryKey: ['expert-task-examsub-detail', currentTask?.taskId],
    queryFn: () => getExpertTaskExamsubDetail(currentTask!.taskId),
    enabled: auditOpen && Boolean(currentTask),
  });

  const handleAudit = (row: ExpertMyTaskVO) => {
    if (row.status === 3) {
      message.warning('该任务已完成,无需审核');
      return;
    }
    setCurrentTask(row);
    setAuditOpen(true);
  };

  const handleClose = () => {
    setAuditOpen(false);
    setCurrentTask(null);
    qc.invalidateQueries({ queryKey: ['expert-task-examsubs'] });
  };

  const handleSubmit = async (item: any, values: Record<string, unknown>) => {
    if (!currentTask) return { count: 0, total: 0, isdone: 0 };
    const payload: ExpertTaskSubmitReq = {
      taskId: currentTask.taskId,
      role: currentTask.role,
      items: [values as any],
    };
    return await submitExpertTaskExamsub(currentTask.taskId, payload);
  };

  const columns = [
    { title: '任务ID', dataIndex: 'taskId', width: 100 },
    {
      title: '工种/级别',
      key: 'workType',
      width: 200,
      render: (_: any, r: ExpertMyTaskVO) => <Tag>{r.workTypeCn}({r.levelCn})</Tag>,
    },
    { title: '题量', dataIndex: 'nums', width: 80 },
    {
      title: '进度',
      key: 'progress',
      width: 100,
      render: (_: any, r: ExpertMyTaskVO) => `${r.finishedNums}/${r.nums}`,
    },
    {
      title: '审核角色',
      dataIndex: 'role',
      width: 100,
      render: (v: number) => {
        const r = ROLE_TAG[v];
        return r ? <Tag color={r.color}>{r.label}</Tag> : <Tag>{v}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v: number) => {
        const s = STATUS_TAG[v];
        return s ? <Tag color={s.color}>{s.label}</Tag> : <Tag>{v}</Tag>;
      },
    },
    { title: '指派时间', dataIndex: 'addtimeCn', width: 160 },
    { title: '完成时间', dataIndex: 'endtimeCn', width: 160 },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      fixed: 'right' as const,
      render: (_: any, r: ExpertMyTaskVO) =>
        r.status !== 3 && hasPerm('expert:task-examsub:submit') ? (
          <Button type="primary" size="small" icon={<FormOutlined />} onClick={() => handleAudit(r)}>
            审核
          </Button>
        ) : null,
    },
  ];

  const records: ExpertMyTaskVO[] = listQuery.data?.items ?? [];
  const total = listQuery.data?.total ?? 0;
  const items = detailQuery.data?.items ?? [];

  return (
    <div style={{ padding: 16 }}>
      <Typography.Title level={4} style={{ marginBottom: 12 }}>
        <FormOutlined /> 主观题审核
      </Typography.Title>

      <Space style={{ marginBottom: 12 }}>
        <Form.Item label="角色" style={{ marginBottom: 0 }}>
          <Select
            value={query.role}
            style={{ width: 120 }}
            allowClear
            placeholder="全部"
            onChange={(v) => setQuery({ ...query, role: v, page: 1 })}
            options={[
              { label: '初审', value: 0 },
              { label: '终审', value: 1 },
            ]}
          />
        </Form.Item>
        <Form.Item label="状态" style={{ marginBottom: 0 }}>
          <Select
            value={query.status}
            style={{ width: 120 }}
            allowClear
            placeholder="全部"
            onChange={(v) => setQuery({ ...query, status: v, page: 1 })}
            options={[
              { label: '待审核', value: 0 },
              { label: '初审中', value: 1 },
              { label: '终审中', value: 2 },
              { label: '完成', value: 3 },
            ]}
          />
        </Form.Item>
        <Button icon={<ReloadOutlined />} onClick={() => qc.invalidateQueries({ queryKey: ['expert-task-examsubs'] })}>
          刷新
        </Button>
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={records}
        loading={listQuery.isFetching}
        scroll={{ x: 1100 }}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }),
        }}
      />

      <Modal
        title={currentTask ? `${currentTask.workTypeCn} - 主观题审核` : '审核'}
        open={auditOpen}
        onCancel={handleClose}
        footer={null}
        width={700}
        destroyOnClose
      >
        {detailQuery.isLoading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
        ) : (
          <ExpertAuditForm
            taskType="examsub"
            items={items}
            onSubmit={handleSubmit}
            onComplete={handleClose}
          />
        )}
      </Modal>
    </div>
  );
}

export default ExpertTaskExamsubPage;

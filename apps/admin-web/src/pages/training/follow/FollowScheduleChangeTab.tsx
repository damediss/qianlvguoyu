import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Descriptions,
  Drawer,
  Empty,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { CheckOutlined, CloseOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';

type Props = {
  classId: number;
};

function parseJson(raw?: string | null): Record<string, unknown> | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function fieldRows(before: Record<string, unknown> | null, after: Record<string, unknown> | null) {
  const keys = [
    'scheduleDate',
    'startTime',
    'endTime',
    'roomName',
    'teacherName',
    'courseName',
    'dayOfWeek',
  ];
  return keys
    .map((k) => ({
      key: k,
      before: before?.[k] == null || before[k] === '' ? '—' : String(before[k]),
      after: after?.[k] == null || after[k] === '' ? '—' : String(after[k]),
      changed: before && after ? String(before[k] ?? '') !== String(after[k] ?? '') : false,
    }))
    .filter((r) => r.before !== '—' || r.after !== '—');
}

const STATUS_COLOR: Record<string, string> = {
  pending: 'gold',
  approved: 'green',
  rejected: 'red',
  cancelled: 'default',
};

export function FollowScheduleChangeTab({ classId }: Props) {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string | undefined>('pending');
  const [detail, setDetail] = useState<trainApi.ScheduleChangeRequest | null>(null);
  const [force, setForce] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['schedule-change-requests', classId, statusFilter],
    queryFn: () => trainApi.listScheduleChangeRequests({ classId, status: statusFilter }),
    enabled: classId > 0,
  });

  const list = data ?? [];

  const approveMu = useMutation({
    mutationFn: (id: string | number) => trainApi.approveScheduleChangeRequest(id, force),
    onSuccess: () => {
      message.success('已通过并更新课表');
      setDetail(null);
      setForce(false);
      void queryClient.invalidateQueries({ queryKey: ['schedule-change-requests'] });
      void queryClient.invalidateQueries({ queryKey: ['follow-schedule', classId] });
      void queryClient.invalidateQueries({ queryKey: ['follow-schedule-status', classId] });
      void queryClient.invalidateQueries({ queryKey: ['follow-teacher-days', classId] });
    },
    onError: (e: Error) => message.error(e.message || '审批失败'),
  });

  const rejectMu = useMutation({
    mutationFn: (id: string | number) =>
      trainApi.rejectScheduleChangeRequest(id, { rejectReason: rejectReason || undefined }),
    onSuccess: () => {
      message.success('已驳回');
      setRejectOpen(false);
      setRejectReason('');
      setDetail(null);
      void queryClient.invalidateQueries({ queryKey: ['schedule-change-requests'] });
    },
    onError: (e: Error) => message.error(e.message || '驳回失败'),
  });

  const before = useMemo(() => parseJson(detail?.beforeSnapshot), [detail]);
  const after = useMemo(() => parseJson(detail?.afterPayload), [detail]);
  const diffs = useMemo(() => fieldRows(before, after), [before, after]);

  const columns: ColumnsType<trainApi.ScheduleChangeRequest> = [
    {
      title: '类型',
      dataIndex: 'changeTypeName',
      width: 110,
      render: (v, r) => v || r.changeType,
    },
    {
      title: '课次',
      dataIndex: 'scheduleId',
      width: 90,
      render: (v) => (v != null ? `#${v}` : '加课'),
    },
    {
      title: '原因',
      dataIndex: 'reason',
      ellipsis: true,
      render: (v?: string) => v || '—',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (s: string, r) => <Tag color={STATUS_COLOR[s] || 'default'}>{r.statusName || s}</Tag>,
    },
    {
      title: '申请时间',
      dataIndex: 'createdAt',
      width: 160,
      render: (v?: string) => (v ? String(v).replace('T', ' ').slice(0, 16) : '—'),
    },
    {
      title: '操作',
      width: 160,
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => setDetail(r)}>
            详情
          </Button>
          {r.status === 'pending' ? (
            <>
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setDetail(r);
                  setForce(false);
                }}
              >
                审批
              </Button>
            </>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="App 老师调课申请审批"
        description="通过后即时写入结构化课表并标记「已调课」。紧急直接改课仍在「课程表」Tab。"
      />
      <Space wrap style={{ marginBottom: 12 }}>
        <Button
          type={statusFilter === 'pending' ? 'primary' : 'default'}
          size="small"
          onClick={() => setStatusFilter('pending')}
        >
          待审批
        </Button>
        <Button
          type={statusFilter === undefined ? 'primary' : 'default'}
          size="small"
          onClick={() => setStatusFilter(undefined)}
        >
          全部
        </Button>
        <Button
          type={statusFilter === 'approved' ? 'primary' : 'default'}
          size="small"
          onClick={() => setStatusFilter('approved')}
        >
          已通过
        </Button>
        <Button
          type={statusFilter === 'rejected' ? 'primary' : 'default'}
          size="small"
          onClick={() => setStatusFilter('rejected')}
        >
          已驳回
        </Button>
        <Button size="small" icon={<ReloadOutlined />} onClick={() => void refetch()}>
          刷新
        </Button>
      </Space>

      <Table
        rowKey={(r) => String(r.id)}
        loading={isLoading}
        columns={columns}
        dataSource={list}
        pagination={{ pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 条` }}
        locale={{ emptyText: <Empty description="暂无调课申请" /> }}
        size="small"
      />

      <Drawer
        title="调课申请详情"
        width={520}
        open={!!detail}
        onClose={() => {
          setDetail(null);
          setForce(false);
        }}
        extra={
          detail?.status === 'pending' ? (
            <Space>
              <Button
                danger
                icon={<CloseOutlined />}
                onClick={() => {
                  setRejectReason('');
                  setRejectOpen(true);
                }}
              >
                驳回
              </Button>
              <Button
                type="primary"
                icon={<CheckOutlined />}
                loading={approveMu.isPending}
                onClick={() => detail && approveMu.mutate(detail.id)}
              >
                通过
              </Button>
            </Space>
          ) : null
        }
      >
        {detail ? (
          <>
            <Descriptions column={1} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="类型">
                {detail.changeTypeName || detail.changeType}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_COLOR[detail.status] || 'default'}>
                  {detail.statusName || detail.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="课次 ID">{detail.scheduleId ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="申请人角色">{detail.applicantRoleCode || '—'}</Descriptions.Item>
              <Descriptions.Item label="原因">{detail.reason || '—'}</Descriptions.Item>
              {detail.rejectReason ? (
                <Descriptions.Item label="驳回原因">{detail.rejectReason}</Descriptions.Item>
              ) : null}
            </Descriptions>

            {detail.changeType === 'cancel' ? (
              <Alert type="warning" showIcon message="停课：通过后将删除该课次" />
            ) : (
              <Table
                size="small"
                pagination={false}
                rowKey="key"
                dataSource={diffs}
                columns={[
                  { title: '字段', dataIndex: 'key', width: 120 },
                  { title: '变更前', dataIndex: 'before' },
                  {
                    title: '变更后',
                    dataIndex: 'after',
                    render: (v: string, r) =>
                      r.changed ? <Typography.Text type="danger">{v}</Typography.Text> : v,
                  },
                ]}
              />
            )}

            {detail.status === 'pending' ? (
              <div style={{ marginTop: 16 }}>
                <Checkbox checked={force} onChange={(e) => setForce(e.target.checked)}>
                  强制通过（忽略教师/教室冲突）
                </Checkbox>
              </div>
            ) : null}
          </>
        ) : null}
      </Drawer>

      <Modal
        title="驳回调课申请"
        open={rejectOpen}
        onCancel={() => setRejectOpen(false)}
        onOk={() => detail && rejectMu.mutate(detail.id)}
        confirmLoading={rejectMu.isPending}
        okText="确认驳回"
        okButtonProps={{ danger: true }}
      >
        <Input.TextArea
          rows={3}
          maxLength={500}
          placeholder="驳回原因（选填）"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>
    </div>
  );
}

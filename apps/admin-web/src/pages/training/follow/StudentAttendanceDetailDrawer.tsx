import { useMemo } from 'react';
import { Alert, Col, Drawer, Empty, Progress, Row, Space, Statistic, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
import type { ClassStudent } from '../../../types/training';

const ATTENDANCE_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '缺勤', color: 'error' },
  1: { text: '出勤', color: 'success' },
  2: { text: '迟到', color: 'warning' },
  3: { text: '早退', color: 'orange' },
};

function formatClassHours(value?: number | string | null): string {
  if (value == null || value === '') return '—';
  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n)) return '—';
  return n.toFixed(1);
}

type Props = {
  open: boolean;
  classId: number | null;
  student: ClassStudent | null;
  hoursRow?: trainApi.FollowStudentClassHours;
  onClose: () => void;
};

function toNumId(id: number | string): number | null {
  const n = typeof id === 'number' ? id : Number(id);
  return Number.isFinite(n) ? n : null;
}

export function StudentAttendanceDetailDrawer({ open, classId, student, hoursRow, onClose }: Props) {
  const userId = student ? toNumId(student.id) : null;

  const { data, isLoading, isError } = useQuery({
    queryKey: ['follow-student-attendance-detail', classId, userId],
    queryFn: () =>
      trainApi.pageAttendances({
        page: 1,
        pageSize: 500,
        classId: classId!,
        userId: userId!,
      }),
    enabled: open && classId != null && classId > 0 && userId != null,
    retry: false,
  });

  const rows = useMemo(() => {
    const list = [...(data?.items ?? [])];
    list.sort((a, b) => String(b.attendanceDate ?? '').localeCompare(String(a.attendanceDate ?? '')));
    return list;
  }, [data?.items]);

  const summary = useMemo(() => {
    let present = 0;
    let absent = 0;
    let late = 0;
    let early = 0;
    for (const row of rows) {
      const s = row.status;
      if (s === 1) present += 1;
      else if (s === 0) absent += 1;
      else if (s === 2) late += 1;
      else if (s === 3) early += 1;
    }
    const recorded = rows.length;
    const effectivePresent = present + late + early;
    const rate = recorded > 0 ? Math.round((effectivePresent / recorded) * 1000) / 10 : 0;
    return { present, absent, late, early, recorded, effectivePresent, rate };
  }, [rows]);

  const columns: ColumnsType<trainApi.TrainAttendanceItem> = [
    {
      title: '日期',
      dataIndex: 'attendanceDate',
      width: 120,
      render: (v?: string) => v || '—',
    },
    {
      title: '课次',
      dataIndex: 'scheduleId',
      width: 100,
      render: (v?: number | null) => (v != null ? `课次#${v}` : '按日'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status?: number) => {
        if (status == null) return <Tag>未知</Tag>;
        const meta = ATTENDANCE_STATUS[status] ?? { text: String(status), color: 'default' };
        return <Tag color={meta.color}>{meta.text}</Tag>;
      },
    },
    {
      title: '签到时间',
      dataIndex: 'checkInTime',
      width: 110,
      render: (v?: string) => v || '—',
    },
    {
      title: '签退时间',
      dataIndex: 'checkOutTime',
      width: 110,
      render: (v?: string) => v || '—',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
      render: (v?: string) => v || '—',
    },
  ];

  return (
    <Drawer
      title={student ? `${student.realname} · 考勤明细` : '考勤明细'}
      width={800}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      {student && (
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            {[student.phone, student.id_card].filter(Boolean).join(' · ') || '暂无联系方式'}
          </Typography.Text>

          {isError && (
            <Alert
              type="warning"
              showIcon
              message="考勤明细加载失败"
              description="请确认账号具备 train:attendance:view 权限。"
            />
          )}

          <Row gutter={[12, 12]}>
            <Col span={6}>
              <Statistic title="登记条数" value={summary.recorded} />
            </Col>
            <Col span={6}>
              <Statistic title="有效出勤" value={summary.effectivePresent} suffix={`/ ${summary.recorded || 0}`} />
            </Col>
            <Col span={6}>
              <Statistic
                title="已上课时"
                value={formatClassHours(hoursRow?.attendedHours)}
                suffix={hoursRow?.attendedHours != null ? '课时' : undefined}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="计划学时"
                value={formatClassHours(hoursRow?.plannedHours)}
                suffix={hoursRow?.plannedHours != null ? '课时' : undefined}
              />
            </Col>
          </Row>
          {hoursRow && hoursRow.completionRate != null ? (
            <div>
              <Typography.Text type="secondary">
                课时完成率 {Number(hoursRow.completionRate).toFixed(1)}%
                {hoursRow.hoursStatus === 'unrecorded' ? '（无考勤记录，按未旷课计）' : ''}
              </Typography.Text>
              <Progress percent={Number(hoursRow.completionRate)} size="small" showInfo={false} style={{ marginTop: 4 }} />
            </div>
          ) : null}
          <Row gutter={[12, 12]}>
            <Col span={8}>
              <Statistic title="出勤率" value={summary.rate} suffix="%" />
              <Progress percent={summary.rate} size="small" showInfo={false} style={{ marginTop: 4 }} />
            </Col>
          </Row>

          <Space wrap>
            <Tag color="success">出勤 {summary.present}</Tag>
            <Tag color="warning">迟到 {summary.late}</Tag>
            <Tag color="orange">早退 {summary.early}</Tag>
            <Tag color="error">缺勤 {summary.absent}</Tag>
          </Space>

          <Table
            rowKey={(r) => String(r.id)}
            size="small"
            loading={isLoading}
            columns={columns}
            dataSource={rows}
            pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无历史考勤记录" /> }}
            scroll={{ y: 420 }}
          />
        </Space>
      )}
    </Drawer>
  );
}

import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Form,
  Input,
  Modal,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  EditOutlined,
  HistoryOutlined,
  LineChartOutlined,
  PlusOutlined,
  StarOutlined,
  TeamOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../../api/training';
import type { ClassOther, ClassStudent, TrainInfoClassListItem } from '../../../types/training';
import { StudentAttendanceDetailDrawer } from './StudentAttendanceDetailDrawer';
import { FollowPracticeTab } from './FollowPracticeTab';

type ApiId = number | string;

const ATTENDANCE_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '缺勤', color: 'error' },
  1: { text: '出勤', color: 'success' },
  2: { text: '迟到', color: 'warning' },
  3: { text: '早退', color: 'orange' },
};

function toNumId(id: ApiId): number | null {
  const n = typeof id === 'number' ? id : Number(id);
  return Number.isFinite(n) ? n : null;
}

function todayStr() {
  return dayjs().format('YYYY-MM-DD');
}

type FollowLog = {
  id: string;
  date: string;
  author: string;
  content: string;
  createdAt: string;
};

function loadLogs(classId: ApiId): FollowLog[] {
  try {
    const raw = localStorage.getItem(`follow_logs_${classId}`);
    return raw ? (JSON.parse(raw) as FollowLog[]) : [];
  } catch {
    return [];
  }
}

function saveLogs(classId: ApiId, logs: FollowLog[]) {
  localStorage.setItem(`follow_logs_${classId}`, JSON.stringify(logs));
}

type Props = {
  classItem: TrainInfoClassListItem;
  onBack: () => void;
};

export function FollowClassWorkbench({ classItem, onBack }: Props) {
  const queryClient = useQueryClient();
  const classId = classItem.id;
  const trainInfoId = classItem.train_info_id;
  const classIdNum = toNumId(classId);

  const [activeTab, setActiveTab] = useState('overview');
  const [bizDate, setBizDate] = useState<Dayjs>(dayjs());
  const [sessionKey, setSessionKey] = useState<string>('day');
  const [logModalOpen, setLogModalOpen] = useState(false);
  const [logForm] = Form.useForm();
  const [logs, setLogs] = useState<FollowLog[]>(() => loadLogs(classId));
  const [studentPage, setStudentPage] = useState(1);
  const [detailStudent, setDetailStudent] = useState<ClassStudent | null>(null);
  const [importResult, setImportResult] = useState<trainApi.TrainAttendanceImportResult | null>(null);

  const dateStr = bizDate.format('YYYY-MM-DD');

  const { data: scheduleData } = useQuery({
    queryKey: ['follow-schedule', classIdNum],
    queryFn: () => trainApi.getClassSchedule(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
  });

  const scheduleItems = useMemo(() => {
    const list = (scheduleData ?? []) as Record<string, unknown>[];
    return list.filter((s) => String(s.scheduleDate ?? '') === dateStr);
  }, [scheduleData, dateStr]);

  const { data: studentsData, isLoading: studentsLoading } = useQuery({
    queryKey: ['follow-students', classId, studentPage],
    queryFn: () => trainApi.listClassStudents(classId, studentPage, 50),
    enabled: !!classId,
  });

  const students = studentsData?.list ?? [];
  const studentTotal = studentsData?.total ?? 0;

  /** 概览统计用全量花名册（避免分页导致当日实到不准） */
  const { data: allStudentsData } = useQuery({
    queryKey: ['follow-students-all', classId],
    queryFn: () => trainApi.listClassStudents(classId, 1, 500),
    enabled: !!classId,
  });
  const allStudents = allStudentsData?.list ?? students;
  const rosterTotal = allStudentsData?.total ?? studentTotal;

  const { data: staffData } = useQuery({
    queryKey: ['follow-staff', trainInfoId, classId],
    queryFn: () => trainApi.listClassOthers(trainInfoId!, classId, 'teacher'),
    enabled: trainInfoId != null && !!classId,
  });

  const { data: chargeData } = useQuery({
    queryKey: ['follow-charge', trainInfoId, classId],
    queryFn: () => trainApi.listClassOthers(trainInfoId!, classId, 'fuzhe'),
    enabled: trainInfoId != null && !!classId,
  });

  const staffList = staffData?.list ?? [];
  const chargeList = chargeData?.list ?? [];
  const leaders = allStudents.filter((s) => Number(s.is_leader) === 1);

  const { data: attendancePage, isLoading: attendanceLoading, isError: attendanceError } = useQuery({
    queryKey: ['follow-attendance', classIdNum, dateStr],
    queryFn: () =>
      trainApi.pageAttendances({
        page: 1,
        pageSize: 500,
        classId: classIdNum!,
        attendanceDate: dateStr,
      }),
    enabled: classIdNum != null && classIdNum > 0,
    retry: false,
  });

  /** 班级全部考勤（累计统计 / 近日出勤） */
  const { data: allAttendancePage, isLoading: allAttendanceLoading } = useQuery({
    queryKey: ['follow-attendance-all', classIdNum],
    queryFn: () =>
      trainApi.pageAttendances({
        page: 1,
        pageSize: 5000,
        classId: classIdNum!,
      }),
    enabled: classIdNum != null && classIdNum > 0,
    retry: false,
  });

  const attendanceMap = useMemo(() => {
    const map = new Map<string, trainApi.TrainAttendanceItem>();
    for (const row of attendancePage?.items ?? []) {
      if (row.userId != null) map.set(String(row.userId), row);
    }
    return map;
  }, [attendancePage?.items]);

  const dayStats = useMemo(() => {
    let onTime = 0;
    let late = 0;
    let early = 0;
    let absent = 0;
    let registered = 0;
    for (const s of allStudents) {
      const att = attendanceMap.get(String(s.id));
      if (!att || att.status == null) continue;
      registered += 1;
      if (att.status === 1) onTime += 1;
      else if (att.status === 2) late += 1;
      else if (att.status === 3) early += 1;
      else if (att.status === 0) absent += 1;
    }
    const present = onTime + late + early;
    const base = rosterTotal > 0 ? rosterTotal : allStudents.length;
    const unregistered = Math.max(base - registered, 0);
    const rate = base > 0 ? Math.round((present / base) * 1000) / 10 : 0;
    return { onTime, late, early, absent, present, registered, unregistered, rate, base };
  }, [allStudents, attendanceMap, rosterTotal]);

  const presentCount = dayStats.present;
  const attendanceRate = dayStats.rate;

  const classStats = useMemo(() => {
    const rows = allAttendancePage?.items ?? [];
    const byDate = new Map<string, { present: number; absent: number; total: number }>();
    const byUser = new Map<
      string,
      { name: string; present: number; absent: number; late: number; early: number; total: number }
    >();

    const nameById = new Map(allStudents.map((s) => [String(s.id), s.realname]));

    for (const row of rows) {
      const date = String(row.attendanceDate ?? '').slice(0, 10);
      if (!date) continue;
      const uid = row.userId != null ? String(row.userId) : '';
      const status = row.status;
      const day = byDate.get(date) ?? { present: 0, absent: 0, total: 0 };
      day.total += 1;
      if (status != null && status > 0) day.present += 1;
      else if (status === 0) day.absent += 1;
      byDate.set(date, day);

      if (!uid) continue;
      const u = byUser.get(uid) ?? {
        name: row.userName || nameById.get(uid) || uid,
        present: 0,
        absent: 0,
        late: 0,
        early: 0,
        total: 0,
      };
      u.total += 1;
      if (status === 0) u.absent += 1;
      else if (status === 1) u.present += 1;
      else if (status === 2) {
        u.late += 1;
        u.present += 1;
      } else if (status === 3) {
        u.early += 1;
        u.present += 1;
      }
      if (row.userName) u.name = row.userName;
      byUser.set(uid, u);
    }

    let sumPresent = 0;
    let sumTotal = 0;
    for (const d of byDate.values()) {
      sumPresent += d.present;
      sumTotal += d.total;
    }
    const cumulativeRate = sumTotal > 0 ? Math.round((sumPresent / sumTotal) * 1000) / 10 : 0;

    const dailyTrend = [...byDate.entries()]
      .map(([date, v]) => ({
        date,
        present: v.present,
        absent: v.absent,
        total: v.total,
        rate: v.total > 0 ? Math.round((v.present / v.total) * 1000) / 10 : 0,
      }))
      .sort((a, b) => b.date.localeCompare(a.date))
      .slice(0, 14);

    const warnings = [...byUser.entries()]
      .map(([userId, u]) => {
        const rate = u.total > 0 ? Math.round((u.present / u.total) * 1000) / 10 : 100;
        return { userId, ...u, rate };
      })
      .filter((u) => u.absent >= 2 || u.rate < 80)
      .sort((a, b) => b.absent - a.absent || a.rate - b.rate)
      .slice(0, 20);

    return {
      recordDays: byDate.size,
      recordCount: rows.length,
      cumulativeRate,
      dailyTrend,
      warnings,
      loading: allAttendanceLoading,
    };
  }, [allAttendancePage?.items, allStudents, allAttendanceLoading]);

  const signInMu = useMutation({
    mutationFn: (student: ClassStudent) => {
      const uid = toNumId(student.id);
      if (uid == null) return Promise.reject(new Error('无效学员ID'));
      const existing = attendanceMap.get(String(student.id));
      if (existing?.id) {
        return trainApi.updateAttendance(existing.id, {
          status: 1,
          checkInTime: dayjs().format('HH:mm:ss'),
        });
      }
      return trainApi.signInAttendance({
        classId: classIdNum ?? undefined,
        projectId: toNumId(trainInfoId ?? 0) ?? undefined,
        userId: uid,
        userName: student.realname,
        attendanceDate: dateStr,
        status: 1,
        checkInTime: dayjs().format('HH:mm:ss'),
      });
    },
    onSuccess: () => {
      message.success('已标记出勤');
      queryClient.invalidateQueries({ queryKey: ['follow-attendance', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-attendance-all', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-student-attendance-detail'] });
    },
    onError: () => message.error('考勤操作失败（请确认账号具备 train:attendance 权限）'),
  });

  const markAbsentMu = useMutation({
    mutationFn: (student: ClassStudent) => {
      const uid = toNumId(student.id);
      if (uid == null) return Promise.reject(new Error('无效学员ID'));
      const existing = attendanceMap.get(String(student.id));
      if (existing?.id) {
        return trainApi.updateAttendance(existing.id, { status: 0 });
      }
      return trainApi.createAttendance({
        classId: classIdNum ?? undefined,
        projectId: toNumId(trainInfoId ?? 0) ?? undefined,
        userId: uid,
        userName: student.realname,
        attendanceDate: dateStr,
        status: 0,
      });
    },
    onSuccess: () => {
      message.success('已标记缺勤');
      queryClient.invalidateQueries({ queryKey: ['follow-attendance', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-attendance-all', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-student-attendance-detail'] });
    },
    onError: () => message.error('考勤操作失败'),
  });

  const importMu = useMutation({
    mutationFn: (file: File) => {
      if (classIdNum == null) return Promise.reject(new Error('无效班级ID'));
      return trainApi.importClassAttendances(classIdNum, file);
    },
    onSuccess: (result) => {
      setImportResult(result);
      queryClient.invalidateQueries({ queryKey: ['follow-attendance', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-attendance-all', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-student-attendance-detail'] });
      const ok = (result.created ?? 0) + (result.updated ?? 0);
      if (ok > 0) {
        message.success(`导入完成：新增 ${result.created}，更新 ${result.updated}`);
      } else {
        message.warning('未写入有效考勤，请查看失败明细');
      }
    },
    onError: (err: unknown) => {
      const msg =
        err && typeof err === 'object' && 'message' in err
          ? String((err as { message?: string }).message)
          : '导入失败';
      message.error(msg || '导入失败（请确认文件格式与 train:attendance:create 权限）');
    },
  });

  const attendanceColumns: ColumnsType<ClassStudent> = [
    { title: '姓名', dataIndex: 'realname', width: 120 },
    { title: '手机', dataIndex: 'phone', width: 130, render: (v?: string) => v || '—' },
    {
      title: '班长',
      width: 70,
      render: (_, row) => (Number(row.is_leader) === 1 ? <Tag color="blue">是</Tag> : '—'),
    },
    {
      title: '考勤状态',
      width: 100,
      render: (_, row) => {
        const att = attendanceMap.get(String(row.id));
        if (!att || att.status == null) return <Tag>未登记</Tag>;
        const meta = ATTENDANCE_STATUS[att.status] ?? { text: String(att.status), color: 'default' };
        return <Tag color={meta.color}>{meta.text}</Tag>;
      },
    },
    {
      title: '签到时间',
      width: 110,
      render: (_, row) => attendanceMap.get(String(row.id))?.checkInTime || '—',
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      render: (_, row) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => setDetailStudent(row)}>
            明细
          </Button>
          <Button type="link" size="small" loading={signInMu.isPending} onClick={() => signInMu.mutate(row)}>
            出勤
          </Button>
          <Button type="link" size="small" danger loading={markAbsentMu.isPending} onClick={() => markAbsentMu.mutate(row)}>
            缺勤
          </Button>
        </Space>
      ),
    },
  ];

  const roleRows = useMemo(() => {
    const rows: { key: string; role: string; name: string; phone?: string; source: string }[] = [];
    for (const s of leaders) {
      rows.push({
        key: `leader-${s.id}`,
        role: '班长',
        name: s.realname,
        phone: s.phone,
        source: '开班准备 is_leader',
      });
    }
    for (const s of staffList) {
      rows.push({
        key: `staff-${s.id}`,
        role: '跟班教务（教务）',
        name: s.realname,
        phone: s.phone,
        source: '班级教务 staff',
      });
    }
    for (const s of chargeList) {
      rows.push({
        key: `charge-${s.id}`,
        role: '负责人 / 班主任候选',
        name: s.realname,
        phone: s.phone,
        source: '班级负责人 inCharge',
      });
    }
    return rows;
  }, [leaders, staffList, chargeList]);

  const handleSaveLog = async () => {
    const values = await logForm.validateFields();
    const next: FollowLog = {
      id: `${Date.now()}`,
      date: values.date ? dayjs(values.date).format('YYYY-MM-DD') : todayStr(),
      author: values.author || '班主任',
      content: values.content,
      createdAt: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    };
    const merged = [next, ...logs];
    setLogs(merged);
    saveLogs(classId, merged);
    setLogModalOpen(false);
    logForm.resetFields();
    message.success('日志已保存（本地暂存，待跟班日志 API 落库）');
  };

  const comingSoon = (title: string, desc: string) => (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      description={
        <div>
          <Typography.Text strong>{title}</Typography.Text>
          <br />
          <Typography.Text type="secondary">{desc}</Typography.Text>
        </div>
      }
    />
  );

  return (
    <div className="follow-workbench">
      <div className="follow-workbench-header">
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
            返回班级列表
          </Button>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {classItem.title || '未命名班级'}
          </Typography.Title>
          <Tag color="processing">跟班工作台</Tag>
        </Space>
        <Typography.Text type="secondary">
          {[classItem.company_name, classItem.work_type_title, classItem.train_title].filter(Boolean).join(' · ') || '—'}
          {(classItem.date_start || classItem.date_end) &&
            ` ｜ ${classItem.date_start || '—'} ~ ${classItem.date_end || '—'}`}
        </Typography.Text>
      </div>

      <Card size="small" style={{ marginBottom: 12 }}>
        <Space wrap size="middle">
          <span>
            业务日期：
            <DatePicker value={bizDate} onChange={(d) => d && setBizDate(d)} allowClear={false} />
          </span>
          <span>
            课次：
            <Select
              style={{ width: 280 }}
              value={sessionKey}
              onChange={setSessionKey}
              options={[
                { value: 'day', label: scheduleItems.length ? '按日（无指定课次）' : '按日跟班（当日无排课）' },
                ...scheduleItems.map((s, idx) => ({
                  value: String(s.id ?? `s-${idx}`),
                  label: `${String(s.courseName || '课程')} ${String(s.startTime || '')}-${String(s.endTime || '')}`,
                })),
              ]}
            />
          </span>
          <Tag>{scheduleItems.length ? `当日课次 ${scheduleItems.length}` : '无排课 · 按日模式'}</Tag>
        </Space>
      </Card>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'overview',
            label: '概览',
            children: (
              <div>
                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                  <Col xs={12} sm={8} md={4}>
                    <Card size="small"><Statistic title="学员数" value={rosterTotal} prefix={<TeamOutlined />} /></Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card size="small">
                      <Statistic title="当日实到" value={presentCount} suffix={`/ ${dayStats.base || '—'}`} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card size="small">
                      <Statistic title="当日出勤率" value={attendanceRate} suffix="%" />
                      <Progress
                        percent={attendanceRate}
                        showInfo={false}
                        size="small"
                        status={attendanceRate < 80 ? 'exception' : 'normal'}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card size="small" loading={classStats.loading}>
                      <Statistic title="累计出勤率" value={classStats.cumulativeRate} suffix="%" prefix={<CheckCircleOutlined />} />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {classStats.recordDays} 个有考勤日 · {classStats.recordCount} 条记录
                      </Typography.Text>
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card size="small">
                      <Statistic title="当日未登记" value={dayStats.unregistered} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card size="small"><Statistic title="跟班日志" value={logs.length} prefix={<HistoryOutlined />} /></Card>
                  </Col>
                </Row>

                <Card size="small" title={`当日考勤构成（${dateStr}）`} style={{ marginBottom: 16 }}>
                  <Row gutter={[12, 12]}>
                    <Col xs={12} sm={8} md={4}><Statistic title="出勤" value={dayStats.onTime} valueStyle={{ color: '#389e0d' }} /></Col>
                    <Col xs={12} sm={8} md={4}><Statistic title="迟到" value={dayStats.late} valueStyle={{ color: '#d48806' }} /></Col>
                    <Col xs={12} sm={8} md={4}><Statistic title="早退" value={dayStats.early} valueStyle={{ color: '#d46b08' }} /></Col>
                    <Col xs={12} sm={8} md={4}><Statistic title="缺勤" value={dayStats.absent} valueStyle={{ color: '#cf1322' }} /></Col>
                    <Col xs={12} sm={8} md={4}><Statistic title="已登记" value={dayStats.registered} /></Col>
                    <Col xs={12} sm={8} md={4}>
                      <Statistic title="待登记" value={dayStats.unregistered} />
                      <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('attendance')}>去考勤 →</Button>
                    </Col>
                  </Row>
                </Card>

                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                  <Col xs={24} lg={12}>
                    <Card size="small" title="近日出勤率" loading={classStats.loading}>
                      {classStats.dailyTrend.length === 0 ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无考勤记录，可导入考勤表或当日登记" />
                      ) : (
                        <Table
                          size="small"
                          rowKey="date"
                          pagination={false}
                          dataSource={classStats.dailyTrend}
                          columns={[
                            { title: '日期', dataIndex: 'date', width: 120 },
                            {
                              title: '实到/登记',
                              width: 110,
                              render: (_, r) => `${r.present}/${r.total}`,
                            },
                            {
                              title: '缺勤',
                              dataIndex: 'absent',
                              width: 70,
                            },
                            {
                              title: '出勤率',
                              dataIndex: 'rate',
                              render: (v: number) => (
                                <Space>
                                  <Progress
                                    percent={v}
                                    size="small"
                                    style={{ width: 80, margin: 0 }}
                                    status={v < 80 ? 'exception' : 'normal'}
                                  />
                                  <span>{v}%</span>
                                </Space>
                              ),
                            },
                            {
                              title: '',
                              width: 70,
                              render: (_, r) => (
                                <Button
                                  type="link"
                                  size="small"
                                  style={{ padding: 0 }}
                                  onClick={() => {
                                    setBizDate(dayjs(r.date));
                                    setActiveTab('attendance');
                                  }}
                                >
                                  查看
                                </Button>
                              ),
                            },
                          ]}
                        />
                      )}
                    </Card>
                  </Col>
                  <Col xs={24} lg={12}>
                    <Card
                      size="small"
                      title="学员预警"
                      extra={<Typography.Text type="secondary">缺勤≥2 或个人出勤率&lt;80%</Typography.Text>}
                      loading={classStats.loading}
                    >
                      {classStats.warnings.length === 0 ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无预警学员" />
                      ) : (
                        <Table
                          size="small"
                          rowKey="userId"
                          pagination={false}
                          dataSource={classStats.warnings}
                          columns={[
                            { title: '姓名', dataIndex: 'name', width: 100 },
                            { title: '缺勤', dataIndex: 'absent', width: 60 },
                            { title: '迟到', dataIndex: 'late', width: 60 },
                            {
                              title: '个人出勤率',
                              dataIndex: 'rate',
                              render: (v: number) => (
                                <Tag color={v < 80 ? 'error' : 'warning'}>{v}%</Tag>
                              ),
                            },
                            {
                              title: '',
                              width: 70,
                              render: (_, r) => {
                                const stu = allStudents.find((s) => String(s.id) === r.userId);
                                return (
                                  <Button
                                    type="link"
                                    size="small"
                                    style={{ padding: 0 }}
                                    disabled={!stu}
                                    onClick={() => stu && setDetailStudent(stu)}
                                  >
                                    明细
                                  </Button>
                                );
                              },
                            },
                          ]}
                        />
                      )}
                    </Card>
                  </Col>
                </Row>

                <Row gutter={[12, 12]}>
                  <Col xs={24} md={12}>
                    <Card size="small" title="角色摘要">
                      <Space direction="vertical" style={{ width: '100%' }}>
                        <div>班长：{leaders.map((s) => s.realname).join('、') || '未设置'}</div>
                        <div>教务：{staffList.map((s: ClassOther) => s.realname).join('、') || '未设置'}</div>
                        <div>负责人：{chargeList.map((s: ClassOther) => s.realname).join('、') || '未设置'}</div>
                        <Typography.Text type="secondary">正式班主任/主讲角色指派将在角色 Tab 完善。</Typography.Text>
                      </Space>
                    </Card>
                  </Col>
                  <Col xs={24} md={12}>
                    <Card size="small" title="今日待办">
                      <Space direction="vertical">
                        <div>待登记考勤：{dayStats.unregistered} 人</div>
                        <div>预警学员：{classStats.warnings.length} 人</div>
                        <div>过程留痕、作业批改：能力建设中</div>
                        <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('attendance')}>去考勤 →</Button>
                        <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('roles')}>去角色 →</Button>
                      </Space>
                    </Card>
                  </Col>
                </Row>
              </div>
            ),
          },
          {
            key: 'attendance',
            label: '考勤',
            children: (
              <div>
                {attendanceError && (
                  <Alert
                    type="warning"
                    showIcon
                    style={{ marginBottom: 12 }}
                    message="考勤接口暂不可用或无权限"
                    description="请确认角色具备 train:attendance:view / create。仍可查看学员名单。"
                  />
                )}
                <Space wrap style={{ marginBottom: 12 }}>
                  <Upload
                    accept=".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      const name = file.name.toLowerCase();
                      if (!name.endsWith('.xls') && !name.endsWith('.xlsx')) {
                        message.error('请上传 .xls 或 .xlsx 考勤文件');
                        return Upload.LIST_IGNORE;
                      }
                      importMu.mutate(file);
                      return false;
                    }}
                  >
                    <Button
                      type="primary"
                      icon={<UploadOutlined />}
                      loading={importMu.isPending}
                      disabled={classIdNum == null}
                    >
                      导入考勤表
                    </Button>
                  </Upload>
                  <Typography.Text type="secondary">
                    支持人社/培训平台「考勤信息列表」.xls/.xlsx；按身份证匹配本班学员，同日多课次合并写入
                  </Typography.Text>
                </Space>
                <Table
                  rowKey={(r) => String(r.id)}
                  loading={studentsLoading || attendanceLoading || importMu.isPending}
                  columns={attendanceColumns}
                  dataSource={students}
                  pagination={{
                    current: studentPage,
                    pageSize: 50,
                    total: studentTotal,
                    onChange: setStudentPage,
                    showTotal: (t) => `共 ${t} 人`,
                  }}
                  scroll={{ x: 720 }}
                  locale={{ emptyText: '暂无学员，请先在开班准备维护名单' }}
                />
              </div>
            ),
          },
          {
            key: 'evidence',
            label: '留痕',
            children: comingSoon('过程留痕', '照片/视频证据将挂载课次或按日；上传通道复用现有文件服务（M2）'),
          },
          {
            key: 'homework',
            label: '作业',
            children: comingSoon('作业与随堂练', 'SaaS 独立作业布置/批改将在本 Tab 落地（M3），不影响 App promote'),
          },
          {
            key: 'practice',
            label: '刷题',
            children: (
              <FollowPracticeTab
                classId={classId}
                classTitle={classItem.title}
                active={activeTab === 'practice'}
              />
            ),
          },
          {
            key: 'students',
            label: '学员',
            children: (
              <Table
                rowKey={(r) => String(r.id)}
                loading={studentsLoading}
                dataSource={students}
                pagination={{
                  current: studentPage,
                  pageSize: 50,
                  total: studentTotal,
                  onChange: setStudentPage,
                }}
                columns={[
                  { title: '姓名', dataIndex: 'realname', width: 120 },
                  { title: '手机', dataIndex: 'phone', width: 130, render: (v?: string) => v || '—' },
                  {
                    title: '班长',
                    width: 80,
                    render: (_, row) => (Number(row.is_leader) === 1 ? <Tag color="blue">是</Tag> : '否'),
                  },
                  {
                    title: '班委',
                    width: 80,
                    render: (_, row) => (Number(row.is_committee) === 1 ? '是' : '否'),
                  },
                  {
                    title: '当日考勤',
                    width: 100,
                    render: (_, row) => {
                      const att = attendanceMap.get(String(row.id));
                      if (!att || att.status == null) return '未登记';
                      return ATTENDANCE_STATUS[att.status]?.text ?? String(att.status);
                    },
                  },
                  {
                    title: '预警',
                    width: 80,
                    render: () => <Tag>—</Tag>,
                  },
                  {
                    title: '操作',
                    width: 90,
                    fixed: 'right',
                    render: (_, row) => (
                      <Button type="link" size="small" onClick={() => setDetailStudent(row)}>
                        考勤明细
                      </Button>
                    ),
                  },
                ]}
              />
            ),
          },
          {
            key: 'roles',
            label: '角色',
            children: (
              <div>
                <Alert
                  type="info"
                  showIcon
                  style={{ marginBottom: 12 }}
                  message="当前展示开班准备已有人员映射"
                  description="班长 ← is_leader；跟班教务 ← 教务 staff；负责人 ← inCharge（班主任双轨正式表 train_class_role 待后端）。人员维护请前往「开班准备」。"
                />
                <Table
                  rowKey="key"
                  dataSource={roleRows}
                  pagination={false}
                  locale={{ emptyText: '暂无角色人员，请先在开班准备设置班长/教务/负责人' }}
                  columns={[
                    { title: '角色', dataIndex: 'role', width: 180 },
                    { title: '姓名', dataIndex: 'name', width: 140 },
                    { title: '手机', dataIndex: 'phone', width: 140, render: (v?: string) => v || '—' },
                    { title: '来源', dataIndex: 'source' },
                  ]}
                />
              </div>
            ),
          },
          {
            key: 'logs',
            label: '日志·督导',
            children: (
              <div>
                <Space style={{ marginBottom: 12 }}>
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => {
                      logForm.setFieldsValue({ date: bizDate, author: '班主任', content: '' });
                      setLogModalOpen(true);
                    }}
                  >
                    新增跟班日志
                  </Button>
                  <Typography.Text type="secondary">督导检查 / 异常闭环将在 M4 接入</Typography.Text>
                </Space>
                <Table
                  rowKey="id"
                  dataSource={logs}
                  pagination={false}
                  locale={{ emptyText: '暂无日志' }}
                  columns={[
                    { title: '日期', dataIndex: 'date', width: 120 },
                    { title: '记录人', dataIndex: 'author', width: 120 },
                    { title: '内容', dataIndex: 'content' },
                    { title: '写入时间', dataIndex: 'createdAt', width: 170 },
                  ]}
                />
              </div>
            ),
          },
          {
            key: 'metrics',
            label: '指标',
            children: (
              <Row gutter={[12, 12]}>
                <Col xs={12} md={6}>
                  <Card size="small">
                    <Statistic title="出勤率（当日）" value={attendanceRate} suffix="%" prefix={<CheckCircleOutlined />} />
                  </Card>
                </Col>
                <Col xs={12} md={6}>
                  <Card size="small" loading={classStats.loading}>
                    <Statistic title="出勤率（累计）" value={classStats.cumulativeRate} suffix="%" prefix={<LineChartOutlined />} />
                  </Card>
                </Col>
                <Col xs={12} md={6}>
                  <Card size="small"><Statistic title="作业完成率" value="—" prefix={<EditOutlined />} /></Card>
                </Col>
                <Col xs={12} md={6}>
                  <Card size="small"><Statistic title="满意度" value="—" prefix={<StarOutlined />} /></Card>
                </Col>
                <Col span={24}>
                  <Alert
                    type="info"
                    showIcon
                    message="出勤率已接入考勤数据；作业/通过率/满意度将在后续里程碑对接"
                    description={`口径：当日实到÷学员数；累计=有效出勤记录（出勤/迟到/早退）÷全部考勤记录。当前累计覆盖 ${classStats.recordDays} 个有考勤日。`}
                  />
                </Col>
              </Row>
            ),
          },
        ]}
      />

      <StudentAttendanceDetailDrawer
        open={!!detailStudent}
        classId={classIdNum}
        student={detailStudent}
        onClose={() => setDetailStudent(null)}
      />

      <Modal
        title="新增跟班日志"
        open={logModalOpen}
        onCancel={() => setLogModalOpen(false)}
        onOk={handleSaveLog}
        destroyOnHidden
      >
        <Form form={logForm} layout="vertical">
          <Form.Item name="date" label="日期" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="author" label="记录人" rules={[{ required: true, message: '请输入记录人' }]}>
            <Input placeholder="班主任 / 跟班教务" />
          </Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true, message: '请输入日志内容' }]}>
            <Input.TextArea rows={4} placeholder="出勤概况、异常学员、沟通与处理…" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="考勤导入结果"
        open={!!importResult}
        onCancel={() => setImportResult(null)}
        onOk={() => setImportResult(null)}
        width={720}
        destroyOnHidden
      >
        {importResult && (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Alert
              type={(importResult.created + importResult.updated) > 0 ? 'success' : 'warning'}
              showIcon
              message={`文件行 ${importResult.totalRows} · 合并人日 ${importResult.parsedRows} · 新增 ${importResult.created} · 更新 ${importResult.updated} · 跳过行 ${importResult.skipped}`}
              description="同日多课次已按最差状态合并；切换业务日期可查看对应天的考勤。"
            />
            {(importResult.failures?.length ?? 0) > 0 && (
              <Table
                size="small"
                rowKey={(r) => `${r.rowNum}-${r.idCard}-${r.attendanceDate}`}
                dataSource={importResult.failures}
                pagination={{ pageSize: 8 }}
                columns={[
                  { title: '行号', dataIndex: 'rowNum', width: 70 },
                  { title: '姓名', dataIndex: 'name', width: 100, render: (v?: string) => v || '—' },
                  { title: '身份证', dataIndex: 'idCard', width: 180, render: (v?: string) => v || '—' },
                  { title: '上课日期', dataIndex: 'attendanceDate', width: 110, render: (v?: string) => v || '—' },
                  { title: '原因', dataIndex: 'reason' },
                ]}
              />
            )}
          </Space>
        )}
      </Modal>
    </div>
  );
}
